package io.github.Thung0808.xai.performance;

import io.github.Thung0808.xai.api.FeatureAttribution;

import java.util.ArrayList;
import java.util.List;

/**
 * Zero-allocation explanation object designed for pooling.
 * 
 * <p>Unlike immutable {@link io.github.Thung0808.xai.api.Explanation},
 * this class is mutable and reusable. It maintains fixed-size arrays
 * to avoid heap allocations during hot paths.</p>
 * 
 * <p><b>Performance Characteristics:</b></p>
 * <ul>
 *   <li>Zero heap allocations after warmup</li>
 *   <li>Fixed-size arrays (no ArrayList resizing)</li>
 *   <li>Primitive arrays for numerical data</li>
 * </ul>
 * 
 * <p><b>Important:</b> Not thread-safe. Use with {@link ExplanationPool}
 * to manage lifecycle.</p>
 *
 * @since 0.3.0
 */
public class ReusableExplanation {
    
    private static final int INITIAL_CAPACITY = 16;
    
    // Mutable state
    private double prediction;
    private double baseline;
    
    // Pre-allocated arrays to avoid resizing
    private String[] featureNames;
    private double[] importances;
    private double[] confidenceIntervals;
    private int attributionCount;
    
    // Metadata
    private String explainerName;
    private long seed;
    private int trials;
    
    /**
     * Creates a reusable explanation with default capacity.
     */
    public ReusableExplanation() {
        this.featureNames = new String[INITIAL_CAPACITY];
        this.importances = new double[INITIAL_CAPACITY];
        this.confidenceIntervals = new double[INITIAL_CAPACITY];
        this.attributionCount = 0;
    }
    
    /**
     * Resets all state for reuse (Zero-Copy operation).
     */
    public void reset() {
        this.prediction = 0.0;
        this.baseline = 0.0;
        this.attributionCount = 0;
        this.explainerName = null;
        this.seed = 0;
        this.trials = 0;
    }
    
    /**
     * Sets prediction value.
     */
    public void setPrediction(double prediction) {
        this.prediction = prediction;
    }
    
    /**
     * Sets baseline value.
     */
    public void setBaseline(double baseline) {
        this.baseline = baseline;
    }
    
    /**
     * Adds a feature attribution (zero-allocation).
     * 
     * @param featureName name of the feature
     * @param importance importance score
     * @param confidenceInterval confidence interval
     */
    public void addAttribution(String featureName, double importance, double confidenceInterval) {
        // Grow arrays if needed (rare after warmup)
        if (attributionCount >= featureNames.length) {
            int newCapacity = featureNames.length * 2;
            
            String[] newNames = new String[newCapacity];
            double[] newImportances = new double[newCapacity];
            double[] newIntervals = new double[newCapacity];
            
            System.arraycopy(featureNames, 0, newNames, 0, attributionCount);
            System.arraycopy(importances, 0, newImportances, 0, attributionCount);
            System.arraycopy(confidenceIntervals, 0, newIntervals, 0, attributionCount);
            
            featureNames = newNames;
            importances = newImportances;
            confidenceIntervals = newIntervals;
        }
        
        featureNames[attributionCount] = featureName;
        importances[attributionCount] = importance;
        confidenceIntervals[attributionCount] = confidenceInterval;
        attributionCount++;
    }
    
    /**
     * Sets metadata.
     */
    public void setMetadata(String explainerName, long seed, int trials) {
        this.explainerName = explainerName;
        this.seed = seed;
        this.trials = trials;
    }
    
    /**
     * Gets prediction.
     */
    public double getPrediction() {
        return prediction;
    }
    
    /**
     * Gets baseline.
     */
    public double getBaseline() {
        return baseline;
    }
    
    /**
     * Gets attribution count.
     */
    public int getAttributionCount() {
        return attributionCount;
    }
    
    /**
     * Gets feature name at index.
     */
    public String getFeatureName(int index) {
        if (index < 0 || index >= attributionCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + attributionCount);
        }
        return featureNames[index];
    }
    
    /**
     * Gets importance at index.
     */
    public double getImportance(int index) {
        if (index < 0 || index >= attributionCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + attributionCount);
        }
        return importances[index];
    }
    
    /**
     * Gets confidence interval at index.
     */
    public double getConfidenceInterval(int index) {
        if (index < 0 || index >= attributionCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + attributionCount);
        }
        return confidenceIntervals[index];
    }
    
    /**
     * Converts to immutable Explanation for external use.
     */
    public io.github.Thung0808.xai.api.Explanation toImmutable() {
        var builder = io.github.Thung0808.xai.api.Explanation.builder()
            .withPrediction(prediction)
            .withBaseline(baseline);
        
        for (int i = 0; i < attributionCount; i++) {
            builder.addAttribution(new FeatureAttribution(
                featureNames[i],
                importances[i],
                confidenceIntervals[i]
            ));
        }
        
        if (explainerName != null) {
            builder.withMetadata(
                io.github.Thung0808.xai.api.ExplanationMetadata.builder(explainerName)
                    .seed(seed)
                    .trials(trials)
                    .build()
            );
        }
        
        return builder.build();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ReusableExplanation{");
        sb.append("prediction=").append(prediction);
        sb.append(", baseline=").append(baseline);
        sb.append(", attributions=").append(attributionCount);
        sb.append("}");
        return sb.toString();
    }
}
