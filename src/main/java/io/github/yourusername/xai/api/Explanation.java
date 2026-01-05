package io.github.Thung0808.xai.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents an explanation of a model's prediction with feature attributions.
 * 
 * <p>This is the core output of any explainer. It contains:</p>
 * <ul>
 *   <li>The prediction value</li>
 *   <li>A baseline prediction (for comparison)</li>
 *   <li>Feature attributions (importance scores)</li>
 *   <li>Metadata about how the explanation was generated</li>
 * </ul>
 * 
 * <p>Use the fluent builder to create explanations:</p>
 * <pre>{@code
 * Explanation exp = Explanation.builder()
 *     .withPrediction(0.85)
 *     .withBaseline(0.5)
 *     .addAttribution("age", 0.35)
 *     .addAttribution("income", -0.15)
 *     .withMetadata(meta -> meta.seed(12345).trials(100))
 *     .build();
 * }</pre>
 *
 * @since 0.1.0
 */
@Stable(since = "0.3.0")
public class Explanation {
    
    private final double prediction;
    private final double baseline;
    private final List<FeatureAttribution> attributions;
    private final ExplanationMetadata metadata;
    
    protected Explanation(Builder builder) {
        this.prediction = builder.prediction;
        this.baseline = builder.baseline;
        this.attributions = List.copyOf(builder.attributions); // Immutable
        this.metadata = Objects.requireNonNull(builder.metadata, "Metadata is required");
    }
    
    public double getPrediction() {
        return prediction;
    }
    
    public double getBaseline() {
        return baseline;
    }
    
    public List<FeatureAttribution> getAttributions() {
        return attributions;
    }
    
    public ExplanationMetadata getMetadata() {
        return metadata;
    }
    
    /**
     * Returns attributions sorted by absolute importance (descending).
     */
    public List<FeatureAttribution> getTopAttributions() {
        return attributions.stream()
            .sorted(Comparator.comparingDouble((FeatureAttribution a) -> 
                Math.abs(a.importance())).reversed())
            .collect(Collectors.toUnmodifiableList());
    }
    
    /**
     * Returns the top N most important features.
     */
    public List<FeatureAttribution> getTopAttributions(int n) {
        return getTopAttributions().stream()
            .limit(n)
            .collect(Collectors.toUnmodifiableList());
    }
    
    /**
     * Calculates overall stability score (0-1, higher is more stable).
     */
    public double getStabilityScore() {
        if (attributions.isEmpty()) {
            return 1.0;
        }
        return attributions.stream()
            .mapToDouble(FeatureAttribution::stabilityScore)
            .average()
            .orElse(1.0);
    }
    
    @Override
    public String toString() {
        return String.format("Explanation{prediction=%.4f, baseline=%.4f, features=%d, stability=%.3f}",
            prediction, baseline, attributions.size(), getStabilityScore());
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Fluent builder for creating Explanation instances.
     */
    public static class Builder {
        private double prediction = 0.0;
        private double baseline = 0.0;
        private final List<FeatureAttribution> attributions = new ArrayList<>();
        private ExplanationMetadata metadata;
        
        public Builder withPrediction(double prediction) {
            if (!Double.isFinite(prediction)) {
                throw new IllegalArgumentException("Prediction must be finite");
            }
            this.prediction = prediction;
            return this;
        }
        
        public Builder withBaseline(double baseline) {
            if (!Double.isFinite(baseline)) {
                throw new IllegalArgumentException("Baseline must be finite");
            }
            this.baseline = baseline;
            return this;
        }
        
        public Builder addAttribution(String feature, double importance) {
            this.attributions.add(new FeatureAttribution(feature, importance));
            return this;
        }
        
        public Builder addAttribution(String feature, double importance, double confidence) {
            this.attributions.add(new FeatureAttribution(feature, importance, confidence));
            return this;
        }
        
        public Builder addAttribution(FeatureAttribution attribution) {
            this.attributions.add(Objects.requireNonNull(attribution));
            return this;
        }
        
        public Builder addAllAttributions(List<FeatureAttribution> attributions) {
            this.attributions.addAll(Objects.requireNonNull(attributions));
            return this;
        }
        
        public Builder withMetadata(ExplanationMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder withMetadata(String explainerName) {
            this.metadata = ExplanationMetadata.builder(explainerName).build();
            return this;
        }
        
        public Explanation build() {
            if (metadata == null) {
                throw new IllegalStateException("Metadata is required. Use withMetadata()");
            }
            return new Explanation(this);
        }
    }
}
