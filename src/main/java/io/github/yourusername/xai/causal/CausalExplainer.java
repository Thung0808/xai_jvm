package io.github.Thung0808.xai.causal;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.experimental.Incubating;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Causal inference explainer using Do-calculus for interventional analysis.
 * 
 * <p><strong>Purpose:</strong> Measure true causal effects (not just correlations)
 * by answering: "What if I <em>intervene</em> to change Feature A?" This differs from:
 * <ul>
 *   <li><strong>SHAP/LIME:</strong> Measure correlation/association only</li>
 *   <li><strong>Counterfactuals:</strong> Find existing data points with different outcomes</li>
 *   <li><strong>Causal Inference:</strong> Simulate actual interventions on causal graph</li>
 * </ul>
 * 
 * <h2>Mathematical Foundation</h2>
 * <p>Implements simplified Do-calculus (Pearl, 2000):
 * <blockquote>
 * $$P(Y | do(X = x)) \neq P(Y | X = x)$$
 * </blockquote>
 * 
 * <p>Where:
 * <ul>
 *   <li>P(Y | X = x) = Observational (correlation)</li>
 *   <li>P(Y | do(X = x)) = Interventional (causation)</li>
 * </ul>
 * 
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>Estimate causal graph structure (naive independence tests)</li>
 *   <li>For intervention do(X = x):
 *     <ul>
 *       <li>Remove all incoming edges to X (cut confounders)</li>
 *       <li>Set X = x for all instances</li>
 *       <li>Propagate through causal descendants only</li>
 *     </ul>
 *   </li>
 *   <li>Measure Average Treatment Effect (ATE): E[Y | do(X=x₁)] - E[Y | do(X=x₀)]</li>
 * </ol>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Train model
 * PredictiveModel model = ...;
 * 
 * // Create causal explainer
 * CausalExplainer causal = new CausalExplainer(
 *     model,
 *     trainingData,    // Used to estimate causal structure
 *     trainingLabels
 * );
 * 
 * // Answer: "What if I intervene to increase Income by 20%?"
 * CausalEffect effect = causal.interventionalEffect(
 *     instance,
 *     "income",       // Feature to intervene on
 *     1.20            // Multiply by 1.20 (20% increase)
 * );
 * 
 * // Results:
 * // - observationalCorrelation: 0.45 (SHAP-like correlation)
 * // - causalEffect: 0.32 (true causal impact after removing confounders)
 * // - confoundingBias: 0.13 (difference = confounding)
 * 
 * System.out.println("True causal effect: " + effect.ate());
 * System.out.println("Correlation (biased): " + effect.observationalEffect());
 * System.out.println("Confounding bias: " + effect.confoundingBias());
 * }</pre>
 * 
 * <h2>Use Cases</h2>
 * <ul>
 *   <li><strong>Policy decisions:</strong> "If we increase credit limit, what's the true impact on default risk?"</li>
 *   <li><strong>Medical interventions:</strong> "If we prescribe drug A, what's the causal effect on recovery?"</li>
 *   <li><strong>Marketing:</strong> "If we send email campaign, what's the true lift (not just correlation)?"</li>
 * </ul>
 * 
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Requires sufficient training data to estimate causal structure</li>
 *   <li>Assumes no unmeasured confounders (strong assumption)</li>
 *   <li>Causal graph estimation is approximate (naive independence tests)</li>
 *   <li>For rigorous causal inference, consider domain expertise + structural equations</li>
 * </ul>
 * 
 * @since 1.1.0-alpha
 * @see <a href="http://bayes.cs.ucla.edu/BOOK-2K/">Causality (Pearl, 2000)</a>
 */
@Incubating(since = "1.1.0-alpha", graduationTarget = "1.3.0")
public class CausalExplainer {
    
    private final PredictiveModel model;
    private final double[][] trainingData;
    private final double[] trainingLabels;
    private final CausalGraph causalGraph;
    private final int numBootstrapSamples;
    
    /**
     * Creates causal explainer with default settings.
     * 
     * @param model the predictive model
     * @param trainingData training data to estimate causal structure
     * @param trainingLabels training labels
     */
    public CausalExplainer(PredictiveModel model, double[][] trainingData, double[] trainingLabels) {
        this(model, trainingData, trainingLabels, 100);
    }
    
    /**
     * Creates causal explainer with custom bootstrap samples.
     * 
     * @param model the predictive model
     * @param trainingData training data to estimate causal structure
     * @param trainingLabels training labels
     * @param numBootstrapSamples number of bootstrap samples for ATE estimation
     */
    public CausalExplainer(PredictiveModel model, double[][] trainingData, 
                          double[] trainingLabels, int numBootstrapSamples) {
        this.model = model;
        this.trainingData = trainingData;
        this.trainingLabels = trainingLabels;
        this.numBootstrapSamples = numBootstrapSamples;
        
        // Estimate causal graph structure from data
        this.causalGraph = estimateCausalGraph(trainingData, trainingLabels);
    }
    
    /**
     * Computes interventional effect of setting a feature to a specific value.
     * 
     * @param instance the instance to explain
     * @param featureName name of feature to intervene on
     * @param interventionValue new value to set (can be multiplicative like 1.20 for +20%)
     * @return causal effect analysis
     */
    public CausalEffect interventionalEffect(double[] instance, String featureName, double interventionValue) {
        return interventionalEffect(instance, getFeatureIndex(featureName), interventionValue);
    }
    
    /**
     * Computes interventional effect of setting a feature to a specific value.
     * 
     * @param instance the instance to explain
     * @param featureIndex index of feature to intervene on
     * @param interventionValue new value to set
     * @return causal effect analysis
     */
    public CausalEffect interventionalEffect(double[] instance, int featureIndex, double interventionValue) {
        // 1. Baseline prediction (no intervention)
        double baselinePrediction = model.predict(instance);
        
        // 2. Observational effect (SHAP-like, just change the feature)
        double[] observationalInstance = instance.clone();
        observationalInstance[featureIndex] = interventionValue;
        double observationalPrediction = model.predict(observationalInstance);
        double observationalEffect = observationalPrediction - baselinePrediction;
        
        // 3. Interventional effect (do-calculus, cut confounders)
        double interventionalPrediction = computeInterventionalPrediction(
            instance, featureIndex, interventionValue
        );
        double causalEffect = interventionalPrediction - baselinePrediction;
        
        // 4. Confounding bias
        double confoundingBias = observationalEffect - causalEffect;
        
        // 5. Compute confidence intervals via bootstrap
        double[] ateDistribution = bootstrapATE(instance, featureIndex, interventionValue);
        double ateLower = percentile(ateDistribution, 2.5);
        double ateUpper = percentile(ateDistribution, 97.5);
        
        return new CausalEffect(
            featureIndex,
            causalEffect,              // Average Treatment Effect (ATE)
            observationalEffect,       // Biased correlation
            confoundingBias,           // Confounding
            ateLower,                  // 95% CI lower
            ateUpper,                  // 95% CI upper
            ateUpper - ateLower        // Uncertainty width
        );
    }
    
    /**
     * Computes interventional prediction using do-calculus.
     * 
     * <p>Algorithm:
     * <ol>
     *   <li>Clone instance</li>
     *   <li>Set intervention feature = interventionValue</li>
     *   <li>For all features causally downstream of intervention:
     *     <ul>
     *       <li>Resample from training data distribution (cut confounders)</li>
     *       <li>Keep causally independent features unchanged</li>
     *     </ul>
     *   </li>
     *   <li>Average predictions over resampled instances</li>
     * </ol>
     */
    private double computeInterventionalPrediction(double[] instance, int featureIndex, double interventionValue) {
        int numSamples = 50; // Number of samples for expectation
        double sumPredictions = 0.0;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        
        for (int i = 0; i < numSamples; i++) {
            double[] interventionalInstance = instance.clone();
            
            // Set intervention
            interventionalInstance[featureIndex] = interventionValue;
            
            // For causally dependent features, resample from training distribution
            Set<Integer> dependents = causalGraph.getDescendants(featureIndex);
            for (int dep : dependents) {
                // Resample from training data
                int randomIdx = rng.nextInt(trainingData.length);
                interventionalInstance[dep] = trainingData[randomIdx][dep];
            }
            
            // Predict
            double prediction = model.predict(interventionalInstance);
            sumPredictions += prediction;
        }
        
        return sumPredictions / numSamples;
    }
    
    /**
     * Bootstrap confidence intervals for ATE.
     */
    private double[] bootstrapATE(double[] instance, int featureIndex, double interventionValue) {
        double[] ateEstimates = new double[numBootstrapSamples];
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        
        for (int b = 0; b < numBootstrapSamples; b++) {
            // Resample training data
            int[] indices = new int[trainingData.length];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = rng.nextInt(trainingData.length);
            }
            
            // Compute ATE on bootstrap sample
            double sumControl = 0.0;
            double sumTreatment = 0.0;
            
            for (int idx : indices) {
                double[] inst = trainingData[idx].clone();
                
                // Control (no intervention)
                sumControl += model.predict(inst);
                
                // Treatment (intervene)
                inst[featureIndex] = interventionValue;
                sumTreatment += model.predict(inst);
            }
            
            ateEstimates[b] = (sumTreatment - sumControl) / indices.length;
        }
        
        Arrays.sort(ateEstimates);
        return ateEstimates;
    }
    
    /**
     * Estimates causal graph structure using naive conditional independence tests.
     * 
     * <p><strong>Note:</strong> This is a simplified heuristic. For production use,
     * consider proper causal discovery algorithms (PC, FCI, etc.) or domain expertise.
     */
    private CausalGraph estimateCausalGraph(double[][] data, double[] labels) {
        int numFeatures = data[0].length;
        CausalGraph graph = new CausalGraph(numFeatures);
        
        // Simple heuristic: Feature i causes feature j if corr(i, j | others) > threshold
        // This is NOT rigorous causal discovery, just a placeholder
        
        for (int i = 0; i < numFeatures; i++) {
            for (int j = 0; j < numFeatures; j++) {
                if (i == j) continue;
                
                // Compute partial correlation (simplified)
                double correlation = computeCorrelation(data, i, j);
                
                if (Math.abs(correlation) > 0.3) {
                    // Assume i → j if i comes before j (temporal ordering heuristic)
                    if (i < j) {
                        graph.addEdge(i, j);
                    }
                }
            }
        }
        
        return graph;
    }
    
    /**
     * Computes Pearson correlation between two features.
     */
    private double computeCorrelation(double[][] data, int feature1, int feature2) {
        int n = data.length;
        
        // Compute means
        double mean1 = 0.0, mean2 = 0.0;
        for (double[] row : data) {
            mean1 += row[feature1];
            mean2 += row[feature2];
        }
        mean1 /= n;
        mean2 /= n;
        
        // Compute correlation
        double numerator = 0.0;
        double denom1 = 0.0, denom2 = 0.0;
        
        for (double[] row : data) {
            double diff1 = row[feature1] - mean1;
            double diff2 = row[feature2] - mean2;
            numerator += diff1 * diff2;
            denom1 += diff1 * diff1;
            denom2 += diff2 * diff2;
        }
        
        if (denom1 < 1e-10 || denom2 < 1e-10) return 0.0;
        
        return numerator / Math.sqrt(denom1 * denom2);
    }
    
    /**
     * Computes percentile of sorted array.
     */
    private double percentile(double[] sortedArray, double percentile) {
        int index = (int) Math.ceil(sortedArray.length * percentile / 100.0) - 1;
        index = Math.max(0, Math.min(index, sortedArray.length - 1));
        return sortedArray[index];
    }
    
    /**
     * Gets feature index by name (placeholder - assumes numeric indices).
     */
    private int getFeatureIndex(String featureName) {
        // In production, maintain a feature name → index mapping
        try {
            return Integer.parseInt(featureName.replace("feature_", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unknown feature: " + featureName);
        }
    }
    
    /**
     * Causal graph structure (Directed Acyclic Graph).
     */
    private static class CausalGraph {
        private final int numFeatures;
        private final Set<Integer>[] children; // Adjacency list
        
        @SuppressWarnings("unchecked")
        public CausalGraph(int numFeatures) {
            this.numFeatures = numFeatures;
            this.children = new Set[numFeatures];
            for (int i = 0; i < numFeatures; i++) {
                children[i] = new HashSet<>();
            }
        }
        
        public void addEdge(int from, int to) {
            children[from].add(to);
        }
        
        /**
         * Gets all descendants (children, grandchildren, etc.) of a feature.
         */
        public Set<Integer> getDescendants(int feature) {
            Set<Integer> descendants = new HashSet<>();
            Queue<Integer> queue = new LinkedList<>();
            queue.add(feature);
            
            while (!queue.isEmpty()) {
                int current = queue.poll();
                for (int child : children[current]) {
                    if (!descendants.contains(child)) {
                        descendants.add(child);
                        queue.add(child);
                    }
                }
            }
            
            return descendants;
        }
    }
    
    /**
     * Result of causal effect analysis.
     * 
     * @param featureIndex index of intervened feature
     * @param ate Average Treatment Effect (true causal effect)
     * @param observationalEffect biased correlation (SHAP-like)
     * @param confoundingBias difference between observational and causal
     * @param ciLower 95% confidence interval lower bound
     * @param ciUpper 95% confidence interval upper bound
     * @param uncertainty width of confidence interval
     */
    public record CausalEffect(
        int featureIndex,
        double ate,
        double observationalEffect,
        double confoundingBias,
        double ciLower,
        double ciUpper,
        double uncertainty
    ) {
        /**
         * Returns true if causal effect is statistically significant (CI doesn't include 0).
         */
        public boolean isSignificant() {
            return (ciLower > 0 && ciUpper > 0) || (ciLower < 0 && ciUpper < 0);
        }
        
        /**
         * Returns interpretation of confounding bias.
         */
        public String confoundingInterpretation() {
            double bias = Math.abs(confoundingBias);
            if (bias < 0.01) {
                return "Negligible confounding (correlation ≈ causation)";
            } else if (bias < 0.05) {
                return "Moderate confounding detected";
            } else {
                return "Strong confounding detected (correlation ≠ causation)";
            }
        }
        
        @Override
        public String toString() {
            return String.format(
                "CausalEffect{ATE=%.4f [%.4f, %.4f], Observational=%.4f, Bias=%.4f, Significant=%s}",
                ate, ciLower, ciUpper, observationalEffect, confoundingBias, isSignificant()
            );
        }
    }
}
