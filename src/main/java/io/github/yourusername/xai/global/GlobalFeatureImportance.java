package io.github.Thung0808.xai.global;

import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.api.Stable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes global feature importance by aggregating perturbation-based importance across dataset.
 * 
 * <p><b>Algorithm:</b></p>
 * <ol>
 *   <li>For each feature, measure impact on model predictions when shuffled</li>
 *   <li>Aggregate importance scores across all dataset instances</li>
 *   <li>Return ranked feature list with statistical summaries</li>
 * </ol>
 * 
 * <p><b>Performance:</b> O(n * m * k) where n=dataset size, m=features, k=perturbations</p>
 * 
 * <p><b>Example:</b></p>
 * <pre>{@code
 * GlobalExplainer explainer = new GlobalFeatureImportance();
 * GlobalExplanation explanation = explainer.explainDataset(model, dataset);
 * 
 * // Top 5 features
 * explanation.getTopFeatures(5).forEach(fa ->
 *     System.out.println(fa.feature() + ": " + fa.importance())
 * );
 * }</pre>
 *
 * @since 0.4.0
 */
@Stable(since = "0.4.0")
public class GlobalFeatureImportance implements GlobalExplainer {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalFeatureImportance.class);
    
    private final int numPerturbations;
    private final double noiseLevel;
    private final long seed;
    
    /**
     * Creates a global feature importance explainer with default settings.
     */
    public GlobalFeatureImportance() {
        this(50, 0.1, 42);
    }
    
    /**
     * Creates a global feature importance explainer with custom settings.
     * 
     * @param numPerturbations number of perturbed samples per feature
     * @param noiseLevel standard deviation of Gaussian noise
     * @param seed random seed for reproducibility
     */
    public GlobalFeatureImportance(int numPerturbations, double noiseLevel, long seed) {
        this.numPerturbations = numPerturbations;
        this.noiseLevel = noiseLevel;
        this.seed = seed;
    }
    
    @Override
    public GlobalExplanation explainDataset(PredictiveModel model, List<double[]> dataset) {
        if (dataset == null || dataset.isEmpty()) {
            throw new IllegalArgumentException("Dataset cannot be null or empty");
        }
        
        int numFeatures = dataset.get(0).length;
        Map<Integer, List<Double>> importancesPerFeature = new HashMap<>();
        
        // Initialize
        for (int i = 0; i < numFeatures; i++) {
            importancesPerFeature.put(i, new ArrayList<>());
        }
        
        Random random = new Random(seed);
        
        // For each instance in dataset
        for (double[] instance : dataset) {
            // For each feature
            for (int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
                double baseline = model.predict(instance);
                
                // Compute importance by shuffling this feature
                double totalImpact = 0.0;
                for (int p = 0; p < numPerturbations; p++) {
                    double[] perturbed = instance.clone();
                    // Add noise to feature
                    perturbed[featureIdx] += random.nextGaussian() * noiseLevel;
                    
                    double perturbedPred = model.predict(perturbed);
                    totalImpact += Math.abs(baseline - perturbedPred);
                }
                
                double avgImpact = totalImpact / numPerturbations;
                importancesPerFeature.get(featureIdx).add(avgImpact);
            }
        }
        
        // Aggregate importances
        List<FeatureAttribution> aggregated = new ArrayList<>();
        for (int i = 0; i < numFeatures; i++) {
            List<Double> importances = importancesPerFeature.get(i);
            double mean = importances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double stdDev = calculateStdDev(importances, mean);
            
            aggregated.add(new FeatureAttribution(
                "feature_" + i,
                mean,
                stdDev
            ));
        }
        
        log.info("Computed global feature importances for {} instances, {} features", 
            dataset.size(), numFeatures);
        
        return new GlobalExplanation(getName(), aggregated, dataset.size(), numFeatures);
    }
    
    private double calculateStdDev(List<Double> values, double mean) {
        if (values.size() < 2) {
            return 0.0;
        }
        
        double sumSquaredDiff = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .sum();
        
        return Math.sqrt(sumSquaredDiff / (values.size() - 1));
    }
    
    @Override
    public String getName() {
        return "GlobalFeatureImportance";
    }
}
