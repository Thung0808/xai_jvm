package io.github.Thung0808.xai.explainer;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.ExplanationMetadata;
import io.github.Thung0808.xai.api.Explainer;
import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.api.PredictiveModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Permutation Feature Importance explainer - modern implementation.
 * 
 * <p>Algorithm:</p>
 * <ol>
 *   <li>Make prediction on original input (baseline)</li>
 *   <li>For each feature:
 *     <ul>
 *       <li>Shuffle that feature's value</li>
 *       <li>Make new prediction</li>
 *       <li>Importance = |baseline - new_prediction|</li>
 *     </ul>
 *   </li>
 * </ol>
 * 
 * <p>This is a model-agnostic method that works with any {@link PredictiveModel}.</p>
 *
 * @since 0.1.0
 */
public class PermutationExplainer implements Explainer<PredictiveModel> {
    
    private static final Logger log = LoggerFactory.getLogger(PermutationExplainer.class);
    
    private final Random random;
    private final int numSamples;
    private final double noiseLevel;
    
    /**
     * Creates a permutation explainer with custom configuration.
     * 
     * @param seed random seed for reproducibility
     * @param numSamples number of permutation samples per feature (higher = more stable but slower)
     * @param noiseLevel standard deviation of Gaussian noise to add (0 = no noise)
     */
    public PermutationExplainer(long seed, int numSamples, double noiseLevel) {
        if (numSamples < 1) {
            throw new IllegalArgumentException("Number of samples must be positive");
        }
        if (noiseLevel < 0) {
            throw new IllegalArgumentException("Noise level must be non-negative");
        }
        
        this.random = new Random(seed);
        this.numSamples = numSamples;
        this.noiseLevel = noiseLevel;
        
        log.debug("PermutationExplainer initialized: seed={}, samples={}, noise={}", 
            seed, numSamples, noiseLevel);
    }
    
    /**
     * Creates a permutation explainer with default settings.
     */
    public PermutationExplainer() {
        this(42, 10, 0.0);
    }
    
    @Override
    public Explanation explain(PredictiveModel model, double[] input) {
        if (model == null || input == null) {
            throw new IllegalArgumentException("Model and input cannot be null");
        }
        if (input.length == 0) {
            throw new IllegalArgumentException("Input cannot be empty");
        }
        
        log.debug("Explaining input with {} features", input.length);
        
        long startTime = System.currentTimeMillis();
        
        // Get baseline prediction
        double baseline = model.predict(input);
        log.trace("Baseline prediction: {}", baseline);
        
        // Build explanation
        Explanation.Builder builder = Explanation.builder()
            .withPrediction(baseline)
            .withBaseline(baseline);
        
        // Compute importance for each feature
        for (int featureIdx = 0; featureIdx < input.length; featureIdx++) {
            double importance = computeFeatureImportance(model, input, featureIdx, baseline);
            String featureName = "feature_" + featureIdx;
            
            builder.addAttribution(featureName, importance);
            log.trace("Feature {}: importance = {}", featureName, importance);
        }
        
        // Add metadata
        ExplanationMetadata metadata = ExplanationMetadata.builder("PermutationExplainer")
            .seed(random.nextLong())
            .trials(numSamples)
            .build();
        
        builder.withMetadata(metadata);
        
        long elapsedMs = System.currentTimeMillis() - startTime;
        log.debug("Explanation completed in {} ms", elapsedMs);
        
        return builder.build();
    }
    
    /**
     * Computes importance of a single feature by permutation.
     */
    private double computeFeatureImportance(
        PredictiveModel model,
        double[] input,
        int featureIdx,
        double baseline
    ) {
        double totalDifference = 0.0;
        
        for (int sample = 0; sample < numSamples; sample++) {
            double[] permuted = input.clone();
            
            // Permute the feature value (add noise or replace with random value)
            if (noiseLevel > 0) {
                permuted[featureIdx] += random.nextGaussian() * noiseLevel;
            } else {
                // Simple permutation: multiply by random factor
                permuted[featureIdx] *= (0.5 + random.nextDouble());
            }
            
            double newPrediction = model.predict(permuted);
            totalDifference += Math.abs(baseline - newPrediction);
        }
        
        return totalDifference / numSamples;
    }
}
