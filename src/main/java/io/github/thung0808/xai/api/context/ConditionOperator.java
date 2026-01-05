package io.github.thung0808.xai.api.context;

/**
 * Operators for feature conditions.
 * Used in context-aware explanations to express when a feature attribution is valid.
 *
 * @since 0.1.0
 */
public enum ConditionOperator {
    /** Feature value is greater than threshold */
    GREATER_THAN(">"),
    
    /** Feature value is less than threshold */
    LESS_THAN("<"),
    
    /** Feature value equals threshold (with epsilon tolerance) */
    EQUAL("="),
    
    /** Feature value is greater than or equal to threshold */
    GREATER_EQUAL(">="),
    
    /** Feature value is less than or equal to threshold */
    LESS_EQUAL("<="),
    
    /** Feature value is not equal to threshold */
    NOT_EQUAL("!=");
    
    private final String symbol;
    
    ConditionOperator(String symbol) {
        this.symbol = symbol;
    }
    
    public String symbol() {
        return symbol;
    }
    
    /**
     * Evaluates the condition for given value and threshold.
     */
    public boolean evaluate(double value, double threshold) {
        return switch (this) {
            case GREATER_THAN -> value > threshold;
            case LESS_THAN -> value < threshold;
            case EQUAL -> Math.abs(value - threshold) < 1e-10;
            case GREATER_EQUAL -> value >= threshold;
            case LESS_EQUAL -> value <= threshold;
            case NOT_EQUAL -> Math.abs(value - threshold) >= 1e-10;
        };
    }
}


