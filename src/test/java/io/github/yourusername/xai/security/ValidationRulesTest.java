package io.github.Thung0808.xai.security;

import io.github.Thung0808.xai.api.Explanation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationRules.
 */
public class ValidationRulesTest {
    
    private Explanation goodExplanation;
    private Explanation badExplanation;
    
    @BeforeEach
    void setUp() {
        goodExplanation = Explanation.builder()
            .withPrediction(0.85)
            .withBaseline(0.50)
            .addAttribution("feature_1", 0.25)
            .addAttribution("feature_2", 0.10)
            .withMetadata("goodExplainer")
            .build();
    }
    
    @Test
    void testStabilityScoreThreshold() {
        ValidationRule rule = ValidationRules.stabilityScoreThreshold(0.70);
        
        // Good explanation should pass (stability > 0.70)
        boolean valid = rule.validate(goodExplanation, new double[]{1.0, 2.0});
        assertTrue(valid);
        
        // Get error message
        String msg = rule.getErrorMessage();
        assertTrue(msg.contains("Stability"));
    }
    
    @Test
    void testFiniteAttributions() {
        ValidationRule rule = ValidationRules.finiteAttributions();
        
        // Good explanation should pass (all finite)
        boolean valid = rule.validate(goodExplanation, new double[]{1.0, 2.0});
        assertTrue(valid);
        
        // Get error message
        String msg = rule.getErrorMessage();
        assertTrue(msg.contains("Non-finite"));
    }
    
    @Test
    void testSumApproximation() {
        ValidationRule rule = ValidationRules.sumApproximation(0.10);  // Tolerance = 0.10
        
        // Good explanation: sum of attributions ≈ prediction - baseline
        // sum = 0.25 + 0.10 = 0.35
        // prediction - baseline = 0.85 - 0.50 = 0.35 ✓
        boolean valid = rule.validate(goodExplanation, new double[]{1.0, 2.0});
        assertTrue(valid);
        
        // Get error message
        String msg = rule.getErrorMessage();
        assertTrue(msg.contains("sum"));
    }
    
    @Test
    void testMultipleRules() {
        ValidationRule rule1 = ValidationRules.finiteAttributions();
        ValidationRule rule2 = ValidationRules.stabilityScoreThreshold(0.50);
        ValidationRule rule3 = ValidationRules.sumApproximation(0.20);
        
        // All three should pass for good explanation
        assertTrue(rule1.validate(goodExplanation, new double[]{1.0}));
        assertTrue(rule2.validate(goodExplanation, new double[]{1.0}));
        assertTrue(rule3.validate(goodExplanation, new double[]{1.0}));
    }
    
    @Test
    void testRuleErrorMessages() {
        ValidationRule rule1 = ValidationRules.finiteAttributions();
        ValidationRule rule2 = ValidationRules.stabilityScoreThreshold(0.90);
        ValidationRule rule3 = ValidationRules.sumApproximation(0.01);
        
        assertNotNull(rule1.getErrorMessage());
        assertNotNull(rule2.getErrorMessage());
        assertNotNull(rule3.getErrorMessage());
        
        assertTrue(rule1.getErrorMessage().length() > 0);
        assertTrue(rule2.getErrorMessage().length() > 0);
        assertTrue(rule3.getErrorMessage().length() > 0);
    }
}
