package io.github.Thung0808.xai.api;

import java.util.Map;

/**
 * Comprehensive trust score for explanation quality and reliability.
 * 
 * <p><b>Purpose:</b> Aggregates multiple quality metrics into a single
 * actionable trust score that guides automation decisions.</p>
 * 
 * <p><b>Components:</b></p>
 * <ul>
 *   <li><b>Stability:</b> Consistency across repeated runs</li>
 *   <li><b>Variance:</b> Spread in feature importance estimates</li>
 *   <li><b>Confidence:</b> Statistical confidence interval width</li>
 *   <li><b>Bias Risk:</b> Potential for discriminatory patterns</li>
 *   <li><b>Coverage:</b> How well explanation covers prediction</li>
 * </ul>
 * 
 * <p><b>Score Ranges:</b></p>
 * <ul>
 *   <li>0.9 - 1.0: HIGH - Safe for automation</li>
 *   <li>0.7 - 0.9: MEDIUM - Human review recommended</li>
 *   <li>0.5 - 0.7: LOW - Requires investigation</li>
 *   <li>0.0 - 0.5: CRITICAL - Do not use</li>
 * </ul>
 * 
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * TrustScore trust = TrustScore.builder()
 *     .stability(0.95)
 *     .variance(0.02)
 *     .confidenceWidth(0.05)
 *     .biasRisk(BiasRisk.LOW)
 *     .coverage(0.98)
 *     .build();
 * 
 * if (trust.getOverallScore() >= 0.9) {
 *     // Safe for automated decision
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
@Stable(since = "0.3.0")
public class TrustScore {
    
    private final double stability;
    private final double variance;
    private final double confidenceWidth;
    private final BiasRisk biasRisk;
    private final double coverage;
    private final double overallScore;
    private final TrustLevel level;
    private final String recommendation;
    
    private TrustScore(Builder builder) {
        this.stability = builder.stability;
        this.variance = builder.variance;
        this.confidenceWidth = builder.confidenceWidth;
        this.biasRisk = builder.biasRisk;
        this.coverage = builder.coverage;
        
        // Compute overall score (weighted average)
        this.overallScore = computeOverallScore();
        this.level = determineTrustLevel();
        this.recommendation = generateRecommendation();
    }
    
    /**
     * Computes weighted aggregate trust score.
     * 
     * Formula: 0.3*stability + 0.2*(1-variance) + 0.2*(1-confWidth) 
     *          + 0.2*biasScore + 0.1*coverage
     */
    private double computeOverallScore() {
        double biasScore = switch (biasRisk) {
            case LOW -> 1.0;
            case MEDIUM -> 0.7;
            case HIGH -> 0.4;
            case CRITICAL -> 0.0;
        };
        
        double varianceScore = Math.max(0.0, 1.0 - variance * 10); // Scale variance
        double confidenceScore = Math.max(0.0, 1.0 - confidenceWidth * 5); // Scale CI width
        
        return 0.3 * stability
             + 0.2 * varianceScore
             + 0.2 * confidenceScore
             + 0.2 * biasScore
             + 0.1 * coverage;
    }
    
    private TrustLevel determineTrustLevel() {
        if (overallScore >= 0.9) return TrustLevel.HIGH;
        if (overallScore >= 0.7) return TrustLevel.MEDIUM;
        if (overallScore >= 0.5) return TrustLevel.LOW;
        return TrustLevel.CRITICAL;
    }
    
    private String generateRecommendation() {
        return switch (level) {
            case HIGH -> "SAFE_FOR_AUTOMATION - Explanation is reliable and can be used for automated decisions";
            case MEDIUM -> "HUMAN_REVIEW_RECOMMENDED - Explanation is mostly reliable but requires human oversight";
            case LOW -> "INVESTIGATION_REQUIRED - Low trust score indicates potential issues";
            case CRITICAL -> "DO_NOT_USE - Critical issues detected, explanation is unreliable";
        };
    }
    
    public double getStability() { return stability; }
    public double getVariance() { return variance; }
    public double getConfidenceWidth() { return confidenceWidth; }
    public BiasRisk getBiasRisk() { return biasRisk; }
    public double getCoverage() { return coverage; }
    public double getOverallScore() { return overallScore; }
    public TrustLevel getLevel() { return level; }
    public String getRecommendation() { return recommendation; }
    
    /**
     * Exports as structured map for JSON serialization.
     */
    public Map<String, Object> toMap() {
        return Map.of(
            "trustScore", overallScore,
            "level", level.name(),
            "recommendation", recommendation,
            "components", Map.of(
                "stability", stability,
                "variance", variance,
                "confidenceWidth", confidenceWidth,
                "biasRisk", biasRisk.name(),
                "coverage", coverage
            )
        );
    }
    
    @Override
    public String toString() {
        return String.format("TrustScore{score=%.3f, level=%s, stability=%.3f, variance=%.3f, biasRisk=%s}",
            overallScore, level, stability, variance, biasRisk);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private double stability = 1.0;
        private double variance = 0.0;
        private double confidenceWidth = 0.0;
        private BiasRisk biasRisk = BiasRisk.LOW;
        private double coverage = 1.0;
        
        public Builder stability(double stability) {
            this.stability = Math.max(0.0, Math.min(1.0, stability));
            return this;
        }
        
        public Builder variance(double variance) {
            this.variance = Math.max(0.0, variance);
            return this;
        }
        
        public Builder confidenceWidth(double width) {
            this.confidenceWidth = Math.max(0.0, width);
            return this;
        }
        
        public Builder biasRisk(BiasRisk risk) {
            this.biasRisk = risk;
            return this;
        }
        
        public Builder coverage(double coverage) {
            this.coverage = Math.max(0.0, Math.min(1.0, coverage));
            return this;
        }
        
        public TrustScore build() {
            return new TrustScore(this);
        }
    }
    
    /**
     * Overall trust level classification.
     */
    public enum TrustLevel {
        HIGH,
        MEDIUM,
        LOW,
        CRITICAL
    }
    
    /**
     * Bias risk assessment.
     */
    public enum BiasRisk {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
