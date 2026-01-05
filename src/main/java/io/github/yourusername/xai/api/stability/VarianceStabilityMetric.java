package io.github.Thung0808.xai.api.stability;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.Explainer;
import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.api.PredictiveModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Measures stability by computing variance of feature importances across multiple runs.
 * 
 * <p>Algorithm:</p>
 * <ol>
 *   <li>Run explainer N times on the same input</li>
 *   <li>For each feature, calculate variance of importance scores</li>
 *   <li>Stability = 1 / (1 + average_variance)</li>
 * </ol>
 * 
 * <p>Score interpretation:</p>
 * <ul>
 *   <li>0.9-1.0: Very stable (excellent)</li>
 *   <li>0.7-0.9: Stable (good)</li>
 *   <li>0.5-0.7: Moderately stable (acceptable)</li>
 *   <li>< 0.5: Unstable (warning!)</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class VarianceStabilityMetric implements StabilityMetric {
    
    private static final Logger log = LoggerFactory.getLogger(VarianceStabilityMetric.class);
    
    @Override
    public double compute(
        PredictiveModel model,
        Explainer<PredictiveModel> explainer,
        double[] input,
        int trials
    ) {
        if (model == null || explainer == null || input == null) {
            throw new IllegalArgumentException("Model, explainer, and input cannot be null");
        }
        if (trials < 2) {
            throw new IllegalArgumentException("Trials must be at least 2");
        }
        
        log.debug("Computing stability metric with {} trials", trials);
        
        // Collect feature importances across trials
        Map<String, double[]> featureImportances = new HashMap<>();
        
        for (int i = 0; i < trials; i++) {
            try {
                Explanation exp = explainer.explain(model, input);
                
                for (FeatureAttribution attr : exp.getAttributions()) {
                    featureImportances
                        .computeIfAbsent(attr.feature(), k -> new double[trials])
                        [i] = attr.importance();
                }
            } catch (Exception e) {
                log.warn("Trial {} failed: {}", i, e.getMessage());
            }
        }
        
        if (featureImportances.isEmpty()) {
            log.warn("No valid explanations generated");
            return 0.0;
        }
        
        // Calculate variance for each feature
        double totalVariance = 0.0;
        int featureCount = 0;
        
        for (Map.Entry<String, double[]> entry : featureImportances.entrySet()) {
            double variance = calculateVariance(entry.getValue());
            totalVariance += variance;
            featureCount++;
            
            log.trace("Feature '{}': variance = {}", entry.getKey(), variance);
        }
        
        double avgVariance = totalVariance / featureCount;
        double stability = 1.0 / (1.0 + avgVariance);
        
        log.debug("Average variance: {}, Stability score: {}", avgVariance, stability);
        
        return stability;
    }
    
    /**
     * Calculates variance of an array of values.
     */
    private double calculateVariance(double[] values) {
        if (values.length < 2) {
            return 0.0;
        }
        
        // Calculate mean
        double sum = 0.0;
        int count = 0;
        for (double v : values) {
            if (Double.isFinite(v)) {
                sum += v;
                count++;
            }
        }
        
        if (count < 2) {
            return 0.0;
        }
        
        double mean = sum / count;
        
        // Calculate variance
        double varianceSum = 0.0;
        for (double v : values) {
            if (Double.isFinite(v)) {
                double diff = v - mean;
                varianceSum += diff * diff;
            }
        }
        
        return varianceSum / (count - 1); // Sample variance
    }
}
