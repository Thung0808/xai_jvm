package io.github.Thung0808.xai.api.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for context-aware features.
 */
class ContextTest {
    
    @Test
    void testFeatureConditionEvaluation() {
        FeatureCondition cond = FeatureCondition.greaterThan("age", 30.0);
        
        assertTrue(cond.evaluate(35.0));
        assertFalse(cond.evaluate(25.0));
        assertFalse(cond.evaluate(30.0));
    }
    
    @Test
    void testConditionOperators() {
        assertEquals(ConditionOperator.GREATER_THAN, 
            FeatureCondition.greaterThan("f", 5.0).operator());
        
        assertEquals(ConditionOperator.LESS_THAN, 
            FeatureCondition.lessThan("f", 5.0).operator());
        
        assertEquals(ConditionOperator.EQUAL, 
            FeatureCondition.equal("f", 5.0).operator());
    }
    
    @Test
    void testConditionalExplanation() {
        ConditionalExplanation exp = ConditionalExplanation.builder()
            .withPrediction(0.9)
            .withBaseline(0.5)
            .addAttribution("income", 0.7)
            .addCondition(FeatureCondition.greaterThan("age", 30.0))
            .withMetadata("ConditionalExplainer")
            .build();
        
        assertEquals(1, exp.getConditions().size());
        assertEquals("age", exp.getConditions().get(0).feature());
    }
    
    @Test
    void testConditionsSatisfied() {
        ConditionalExplanation exp = ConditionalExplanation.builder()
            .withPrediction(1.0)
            .addAttribution("f1", 0.5)
            .addCondition(FeatureCondition.greaterThan("age", 30.0))
            .addCondition(FeatureCondition.lessThan("income", 50000.0))
            .withMetadata("Test")
            .build();
        
        java.util.Map<String, Double> satisfied = java.util.Map.of(
            "age", 35.0,
            "income", 40000.0
        );
        
        assertTrue(exp.conditionsSatisfied(satisfied));
        
        java.util.Map<String, Double> notSatisfied = java.util.Map.of(
            "age", 25.0,
            "income", 40000.0
        );
        
        assertFalse(exp.conditionsSatisfied(notSatisfied));
    }
}
