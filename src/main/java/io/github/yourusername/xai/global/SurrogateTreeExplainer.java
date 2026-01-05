package io.github.Thung0808.xai.global;

import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.experimental.Incubating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Approximates a black-box model with an interpretable decision tree surrogate.
 * 
 * <p><b>Algorithm:</b></p>
 * <ol>
 *   <li>Generate predictions from black-box model on dataset</li>
 *   <li>Train a shallow decision tree to approximate those predictions</li>
 *   <li>Extract feature importance from tree structure</li>
 *   <li>Compute fidelity (R² score) between tree and original model</li>
 * </ol>
 * 
 * <p><b>Benefits:</b></p>
 * <ul>
 *   <li>Global explanation via tree structure (IF-THEN rules)</li>
 *   <li>Feature importance from splits</li>
 *   <li>Fidelity score indicates explanation quality</li>
 * </ul>
 * 
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Limited by tree depth - deeper trees less interpretable</li>
 *   <li>May not capture complex model behavior</li>
 *   <li>Fidelity indicates approximation quality</li>
 * </ul>
 * 
 * @since 0.4.0
 */
@Incubating(
    since = "0.4.0",
    graduationTarget = "1.0.0",
    reason = "Tree building strategy may be refined based on feedback"
)
public class SurrogateTreeExplainer implements GlobalExplainer {
    
    private static final Logger log = LoggerFactory.getLogger(SurrogateTreeExplainer.class);
    
    private final int maxDepth;
    private final int minSamplesLeaf;
    private final long seed;
    
    /**
     * Creates a surrogate tree explainer with default settings.
     */
    public SurrogateTreeExplainer() {
        this(4, 5, 42);
    }
    
    /**
     * Creates a surrogate tree explainer with custom settings.
     * 
     * @param maxDepth maximum tree depth (controls interpretability)
     * @param minSamplesLeaf minimum samples required at leaf node
     * @param seed random seed
     */
    public SurrogateTreeExplainer(int maxDepth, int minSamplesLeaf, long seed) {
        this.maxDepth = maxDepth;
        this.minSamplesLeaf = minSamplesLeaf;
        this.seed = seed;
    }
    
    @Override
    public GlobalExplanation explainDataset(PredictiveModel model, List<double[]> dataset) {
        if (dataset == null || dataset.isEmpty()) {
            throw new IllegalArgumentException("Dataset cannot be null or empty");
        }
        
        int numFeatures = dataset.get(0).length;
        
        // Generate predictions from black-box model
        double[] targets = dataset.stream()
            .mapToDouble(model::predict)
            .toArray();
        
        // Build decision tree on this data
        DecisionTree tree = new DecisionTree(maxDepth, minSamplesLeaf, seed);
        tree.fit(dataset.toArray(new double[0][]), targets);
        
        // Compute fidelity (R² score)
        double[] treePredictions = dataset.stream()
            .mapToDouble(tree::predict)
            .toArray();
        
        double fidelity = computeR2(targets, treePredictions);
        
        // Extract feature importance from splits
        List<FeatureAttribution> featureImportances = new ArrayList<>();
        Map<Integer, Double> importanceMap = tree.getFeatureImportances();
        
        double totalImportance = importanceMap.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        
        for (int i = 0; i < numFeatures; i++) {
            double importance = importanceMap.getOrDefault(i, 0.0);
            double normalized = totalImportance > 0 ? importance / totalImportance : 0.0;
            
            featureImportances.add(new FeatureAttribution(
                "feature_" + i,
                normalized,
                0.0  // No confidence interval available from tree structure
            ));
        }
        
        GlobalExplanation explanation = new GlobalExplanation(
            getName(),
            featureImportances,
            dataset.size(),
            numFeatures
        );
        
        explanation.setMetadata("fidelity", fidelity);
        explanation.setMetadata("treeDepth", tree.getDepth());
        explanation.setMetadata("numLeaves", tree.getNumLeaves());
        
        log.info("Trained surrogate tree: depth={}, fidelity={:.3f}, leaves={}",
            tree.getDepth(), fidelity, tree.getNumLeaves());
        
        return explanation;
    }
    
    private double computeR2(double[] targets, double[] predictions) {
        double mean = Arrays.stream(targets).average().orElse(0.0);
        
        double ssRes = 0.0;
        double ssTot = 0.0;
        
        for (int i = 0; i < targets.length; i++) {
            ssRes += Math.pow(targets[i] - predictions[i], 2);
            ssTot += Math.pow(targets[i] - mean, 2);
        }
        
        if (ssTot == 0.0) {
            return 1.0;
        }
        
        return 1.0 - (ssRes / ssTot);
    }
    
    @Override
    public String getName() {
        return "SurrogateTree";
    }
    
    /**
     * Simple decision tree implementation for surrogate modeling.
     */
    private static class DecisionTree {
        
        private Node root;
        private final int maxDepth;
        private final int minSamplesLeaf;
        private final Random random;
        
        DecisionTree(int maxDepth, int minSamplesLeaf, long seed) {
            this.maxDepth = maxDepth;
            this.minSamplesLeaf = minSamplesLeaf;
            this.random = new Random(seed);
        }
        
        void fit(double[][] X, double[] y) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < X.length; i++) {
                indices.add(i);
            }
            this.root = buildTree(X, y, indices, 0);
        }
        
        private Node buildTree(double[][] X, double[] y, List<Integer> indices, int depth) {
            if (indices.isEmpty() || depth >= maxDepth) {
                return new LeafNode(calculateMean(y, indices));
            }
            
            // Find best split
            BestSplit bestSplit = findBestSplit(X, y, indices);
            
            if (bestSplit == null || indices.size() < 2 * minSamplesLeaf) {
                return new LeafNode(calculateMean(y, indices));
            }
            
            // Recursively build left and right subtrees
            Node leftChild = buildTree(X, y, bestSplit.leftIndices, depth + 1);
            Node rightChild = buildTree(X, y, bestSplit.rightIndices, depth + 1);
            
            return new SplitNode(bestSplit.featureIdx, bestSplit.threshold, leftChild, rightChild);
        }
        
        private BestSplit findBestSplit(double[][] X, double[] y, List<Integer> indices) {
            double bestGain = 0.0;
            BestSplit bestSplit = null;
            
            int numFeatures = X[0].length;
            double parentVariance = calculateVariance(y, indices);
            
            for (int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
                for (Integer idx : indices) {
                    double threshold = X[idx][featureIdx];
                    
                    List<Integer> leftIndices = new ArrayList<>();
                    List<Integer> rightIndices = new ArrayList<>();
                    
                    for (Integer i : indices) {
                        if (X[i][featureIdx] <= threshold) {
                            leftIndices.add(i);
                        } else {
                            rightIndices.add(i);
                        }
                    }
                    
                    if (leftIndices.size() < minSamplesLeaf || rightIndices.size() < minSamplesLeaf) {
                        continue;
                    }
                    
                    double leftVar = calculateVariance(y, leftIndices);
                    double rightVar = calculateVariance(y, rightIndices);
                    
                    double leftWeight = (double) leftIndices.size() / indices.size();
                    double rightWeight = (double) rightIndices.size() / indices.size();
                    
                    double gain = parentVariance - (leftWeight * leftVar + rightWeight * rightVar);
                    
                    if (gain > bestGain) {
                        bestGain = gain;
                        bestSplit = new BestSplit(featureIdx, threshold, gain, leftIndices, rightIndices);
                    }
                }
            }
            
            return bestSplit;
        }
        
        private double calculateMean(double[] y, List<Integer> indices) {
            return indices.stream()
                .mapToDouble(i -> y[i])
                .average()
                .orElse(0.0);
        }
        
        private double calculateVariance(double[] y, List<Integer> indices) {
            if (indices.isEmpty()) {
                return 0.0;
            }
            
            double mean = calculateMean(y, indices);
            double variance = indices.stream()
                .mapToDouble(i -> Math.pow(y[i] - mean, 2))
                .average()
                .orElse(0.0);
            
            return variance;
        }
        
        double predict(double[] instance) {
            return root.predict(instance);
        }
        
        int getDepth() {
            return root.getDepth();
        }
        
        int getNumLeaves() {
            return root.getNumLeaves();
        }
        
        Map<Integer, Double> getFeatureImportances() {
            return root.getFeatureImportances();
        }
        
        interface Node {
            double predict(double[] instance);
            int getDepth();
            int getNumLeaves();
            Map<Integer, Double> getFeatureImportances();
        }
        
        static class LeafNode implements Node {
            double value;
            
            LeafNode(double value) {
                this.value = value;
            }
            
            @Override
            public double predict(double[] instance) {
                return value;
            }
            
            @Override
            public int getDepth() {
                return 1;
            }
            
            @Override
            public int getNumLeaves() {
                return 1;
            }
            
            @Override
            public Map<Integer, Double> getFeatureImportances() {
                return new HashMap<>();
            }
        }
        
        static class SplitNode implements Node {
            int featureIdx;
            double threshold;
            Node left;
            Node right;
            
            SplitNode(int featureIdx, double threshold, Node left, Node right) {
                this.featureIdx = featureIdx;
                this.threshold = threshold;
                this.left = left;
                this.right = right;
            }
            
            @Override
            public double predict(double[] instance) {
                if (instance[featureIdx] <= threshold) {
                    return left.predict(instance);
                } else {
                    return right.predict(instance);
                }
            }
            
            @Override
            public int getDepth() {
                return 1 + Math.max(left.getDepth(), right.getDepth());
            }
            
            @Override
            public int getNumLeaves() {
                return left.getNumLeaves() + right.getNumLeaves();
            }
            
            @Override
            public Map<Integer, Double> getFeatureImportances() {
                Map<Integer, Double> importances = new HashMap<>();
                importances.put(featureIdx, 1.0);
                
                // Aggregate child importances
                left.getFeatureImportances().forEach((k, v) ->
                    importances.merge(k, v, Double::sum)
                );
                right.getFeatureImportances().forEach((k, v) ->
                    importances.merge(k, v, Double::sum)
                );
                
                return importances;
            }
        }
        
        static class BestSplit {
            int featureIdx;
            double threshold;
            double gain;
            List<Integer> leftIndices;
            List<Integer> rightIndices;
            
            BestSplit(int featureIdx, double threshold, double gain,
                     List<Integer> leftIndices, List<Integer> rightIndices) {
                this.featureIdx = featureIdx;
                this.threshold = threshold;
                this.gain = gain;
                this.leftIndices = leftIndices;
                this.rightIndices = rightIndices;
            }
        }
    }
}
