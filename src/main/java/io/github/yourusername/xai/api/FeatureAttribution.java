package io.github.Thung0808.xai.api;

/**
 * Represents the attribution (importance) of a single feature in a prediction.
 * 
 * <p>This is an immutable record that pairs a feature identifier with its
 * computed importance score. Higher absolute values indicate greater importance.</p>
 * 
 * <p>Optionally includes a confidence interval to quantify uncertainty in the attribution.</p>
 *
 * @param feature the feature identifier (name or index as string)
 * @param importance the importance score (can be positive or negative)
 * @param confidenceInterval the standard deviation or confidence range (0 means deterministic)
 * @since 0.1.0
 */
@Stable(since = "0.3.0")
public record FeatureAttribution(
    String feature,
    double importance,
    double confidenceInterval
) {
    
    /**
     * Creates a feature attribution without confidence interval.
     * 
     * @param feature the feature identifier
     * @param importance the importance score
     */
    public FeatureAttribution(String feature, double importance) {
        this(feature, importance, 0.0);
    }
    
    /**
     * Compact constructor with validation.
     */
    public FeatureAttribution {
        if (feature == null || feature.isBlank()) {
            throw new IllegalArgumentException("Feature name cannot be null or blank");
        }
        if (!Double.isFinite(importance)) {
            throw new IllegalArgumentException("Importance must be finite");
        }
        if (confidenceInterval < 0 || !Double.isFinite(confidenceInterval)) {
            throw new IllegalArgumentException("Confidence interval must be non-negative and finite");
        }
    }
    
    /**
     * Returns true if this attribution has a confidence interval (uncertainty quantification).
     */
    public boolean hasUncertainty() {
        return confidenceInterval > 0;
    }
    
    /**
     * Returns a stability score from 0 to 1.
     * 1.0 means perfectly stable (no variance), lower values indicate instability.
     */
    public double stabilityScore() {
        if (Math.abs(importance) < 1e-10) {
            return 1.0; // Near-zero importance is stable by definition
        }
        return 1.0 / (1.0 + confidenceInterval / Math.abs(importance));
    }
}
