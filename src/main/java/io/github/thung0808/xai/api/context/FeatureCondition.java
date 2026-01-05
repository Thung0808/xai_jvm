package io.github.thung0808.xai.api.context;

/**
 * Represents a condition on a feature value.
 * 
 * <p>Used in context-aware explanations to express:</p>
 * <pre>
 * "Feature X is important when Feature Y > 10"
 * "Feature income matters when age < 30"
 * </pre>
 *
 * @param feature the feature name
 * @param operator the comparison operator
 * @param value the threshold value
 * @since 0.1.0
 */
public record FeatureCondition(
    String feature,
    ConditionOperator operator,
    double value
) {
    
    public FeatureCondition {
        if (feature == null || feature.isBlank()) {
            throw new IllegalArgumentException("Feature name cannot be null or blank");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Operator cannot be null");
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Value must be finite");
        }
    }
    
    /**
     * Evaluates this condition against a feature value.
     * 
     * @param actualValue the actual feature value
     * @return true if the condition is satisfied
     */
    public boolean evaluate(double actualValue) {
        return operator.evaluate(actualValue, value);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s %.3f", feature, operator.symbol(), value);
    }
    
    /**
     * Factory method for "greater than" condition.
     */
    public static FeatureCondition greaterThan(String feature, double value) {
        return new FeatureCondition(feature, ConditionOperator.GREATER_THAN, value);
    }
    
    /**
     * Factory method for "less than" condition.
     */
    public static FeatureCondition lessThan(String feature, double value) {
        return new FeatureCondition(feature, ConditionOperator.LESS_THAN, value);
    }
    
    /**
     * Factory method for "equal" condition.
     */
    public static FeatureCondition equal(String feature, double value) {
        return new FeatureCondition(feature, ConditionOperator.EQUAL, value);
    }
}


