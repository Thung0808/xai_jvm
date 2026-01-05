package io.github.Thung0808.xai.api.context;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.api.ExplanationMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A context-aware explanation that includes conditions under which feature attributions apply.
 * 
 * <p>This is a major differentiator for this XAI library. While most explainers say
 * "Feature X has importance 0.5", a conditional explanation says:</p>
 * 
 * <pre>
 * "Feature 'income' has importance 0.8 WHEN age > 30"
 * "Feature 'education' has importance 0.3 WHEN income < 50000"
 * </pre>
 * 
 * <p>This is crucial for:</p>
 * <ul>
 *   <li>Understanding feature interactions</li>
 *   <li>Identifying subpopulation-specific patterns</li>
 *   <li>Building more trustworthy AI systems</li>
 * </ul>
 *
 * @since 0.1.0
 */
public final class ConditionalExplanation extends Explanation {
    
    private final List<FeatureCondition> conditions;
    
    private ConditionalExplanation(Builder builder) {
        super(builder);
        this.conditions = List.copyOf(builder.conditions);
    }
    
    /**
     * Returns the conditions under which this explanation is valid.
     */
    public List<FeatureCondition> getConditions() {
        return conditions;
    }
    
    /**
     * Checks if all conditions are satisfied by the given feature values.
     * 
     * @param featureValues map from feature name to value
     * @return true if all conditions are met
     */
    public boolean conditionsSatisfied(java.util.Map<String, Double> featureValues) {
        return conditions.stream()
            .allMatch(condition -> {
                Double value = featureValues.get(condition.feature());
                return value != null && condition.evaluate(value);
            });
    }
    
    @Override
    public String toString() {
        String baseStr = super.toString();
        String condStr = conditions.stream()
            .map(FeatureCondition::toString)
            .collect(Collectors.joining(" AND "));
        return baseStr + " | Conditions: [" + condStr + "]";
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for ConditionalExplanation.
     */
    public static class Builder extends Explanation.Builder {
        private final List<FeatureCondition> conditions = new java.util.ArrayList<>();
        
        public Builder addCondition(FeatureCondition condition) {
            this.conditions.add(Objects.requireNonNull(condition));
            return this;
        }
        
        public Builder addCondition(String feature, ConditionOperator operator, double value) {
            return addCondition(new FeatureCondition(feature, operator, value));
        }
        
        @Override
        public Builder withPrediction(double prediction) {
            super.withPrediction(prediction);
            return this;
        }
        
        @Override
        public Builder withBaseline(double baseline) {
            super.withBaseline(baseline);
            return this;
        }
        
        @Override
        public Builder addAttribution(String feature, double importance) {
            super.addAttribution(feature, importance);
            return this;
        }
        
        @Override
        public Builder addAttribution(String feature, double importance, double confidence) {
            super.addAttribution(feature, importance, confidence);
            return this;
        }
        
        @Override
        public Builder addAttribution(FeatureAttribution attribution) {
            super.addAttribution(attribution);
            return this;
        }
        
        @Override
        public Builder withMetadata(ExplanationMetadata metadata) {
            super.withMetadata(metadata);
            return this;
        }
        
        @Override
        public Builder withMetadata(String explainerName) {
            super.withMetadata(explainerName);
            return this;
        }
        
        @Override
        public ConditionalExplanation build() {
            return new ConditionalExplanation(this);
        }
    }
}
