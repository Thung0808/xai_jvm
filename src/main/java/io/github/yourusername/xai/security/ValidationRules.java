package io.github.Thung0808.xai.security;

import io.github.Thung0808.xai.api.*;

/**
 * Built-in validation rules.
 */
public class ValidationRules {
    
    /**
     * Rule: Stability score must be above threshold.
     */
    public static ValidationRule stabilityScoreThreshold(final double minScore) {
        return new ValidationRule() {
            @Override
            public boolean validate(Explanation expl, double[] instance) {
                return expl.getStabilityScore() >= minScore;
            }
            
            @Override
            public String getErrorMessage() {
                return "Stability score below threshold: " + minScore;
            }
        };
    }
    
    /**
     * Rule: All attributions must be finite (no NaN, Infinity).
     */
    public static ValidationRule finiteAttributions() {
        return new ValidationRule() {
            @Override
            public boolean validate(Explanation expl, double[] instance) {
                return expl.getAttributions().stream()
                    .allMatch(attr -> Double.isFinite(attr.importance()));
            }
            
            @Override
            public String getErrorMessage() {
                return "Non-finite attribution values detected (NaN, Infinity)";
            }
        };
    }
    
    /**
     * Rule: Sum of attributions should approximately equal (prediction - baseline).
     */
    public static ValidationRule sumApproximation(final double tolerance) {
        return new ValidationRule() {
            @Override
            public boolean validate(Explanation expl, double[] instance) {
                double attributionSum = expl.getAttributions().stream()
                    .mapToDouble(FeatureAttribution::importance)
                    .sum();
                
                double expectedChange = expl.getPrediction() - expl.getBaseline();
                return Math.abs(attributionSum - expectedChange) <= tolerance;
            }
            
            @Override
            public String getErrorMessage() {
                return "Attribution sum inconsistency: sum does not match prediction change";
            }
        };
    }
}
