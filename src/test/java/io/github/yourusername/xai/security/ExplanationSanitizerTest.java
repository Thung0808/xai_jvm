package io.github.Thung0808.xai.security;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.Explainer;
import io.github.Thung0808.xai.api.PredictiveModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExplanationSanitizer (Defensive XAI).
 */
public class ExplanationSanitizerTest {
    
    private Explanation baseExplanation;
    private Explainer<PredictiveModel> mockExplainer1;
    private Explainer<PredictiveModel> mockExplainer2;
    private PredictiveModel mockModel;
    
    @BeforeEach
    void setUp() {
        // Create base explanation
        baseExplanation = Explanation.builder()
            .withPrediction(0.85)
            .withBaseline(0.5)
            .addAttribution("feature_1", 0.25)
            .addAttribution("feature_2", 0.10)
            .withMetadata("primaryExplainer")
            .build();
        
        // Create consistent mock explainers
        mockExplainer1 = (model, input) -> Explanation.builder()
            .withPrediction(0.85)  // Same prediction
            .withBaseline(0.5)
            .addAttribution("feature_1", 0.24)  // Similar attribution
            .addAttribution("feature_2", 0.11)  // Similar attribution
            .withMetadata("validator1")
            .build();
        
        mockExplainer2 = (model, input) -> Explanation.builder()
            .withPrediction(0.86)  // Slightly different
            .withBaseline(0.5)
            .addAttribution("feature_1", 0.26)
            .addAttribution("feature_2", 0.09)
            .withMetadata("validator2")
            .build();
        
        // Create mock model
        mockModel = new PredictiveModel() {
            @Override
            public double predict(double[] input) {
                return 0.85;
            }
        };
    }
    
    @Test
    void testSanitizerWithConsistentExplanations() {
        ExplanationSanitizer sanitizer = new ExplanationSanitizer(0.30);
        sanitizer.addValidator(mockExplainer1);
        sanitizer.addValidator(mockExplainer2);
        
        SanitizationResult result = sanitizer.validate(baseExplanation, mockModel, new double[]{1.0, 2.0});
        
        // Should be valid or have minor inconsistencies
        assertNotNull(result);
        assertNotNull(result.getMessage());
    }
    
    @Test
    void testSanitizerNoValidators() {
        ExplanationSanitizer sanitizer = new ExplanationSanitizer(0.30);
        
        SanitizationResult result = sanitizer.validate(baseExplanation, mockModel, new double[]{1.0, 2.0});
        
        // Should report no validators configured
        assertNotNull(result);
        assertTrue(result.getMessage().contains("validators"));
    }
    
    @Test
    void testSanitizerThresholdParameter() {
        ExplanationSanitizer sanitizer1 = new ExplanationSanitizer(0.10);  // Strict
        ExplanationSanitizer sanitizer2 = new ExplanationSanitizer(0.50);  // Lenient
        
        sanitizer1.addValidator(mockExplainer1);
        sanitizer2.addValidator(mockExplainer1);
        
        SanitizationResult result1 = sanitizer1.validate(baseExplanation, mockModel, new double[]{1.0, 2.0});
        SanitizationResult result2 = sanitizer2.validate(baseExplanation, mockModel, new double[]{1.0, 2.0});
        
        assertNotNull(result1);
        assertNotNull(result2);
        // Stricter threshold might flag more issues
    }
    
    @Test
    void testSanitizationResultValid() {
        SanitizationResult result = new SanitizationResult(true, "Test message", false);
        
        assertTrue(result.isValid());
        assertFalse(result.isSuspicious());
        assertEquals("Test message", result.getMessage());
    }
    
    @Test
    void testSanitizationResultSuspicious() {
        SanitizationResult result = new SanitizationResult(false, "Manipulation detected", true);
        
        assertFalse(result.isValid());
        assertTrue(result.isSuspicious());
        assertEquals("Manipulation detected", result.getMessage());
    }
    
    @Test
    void testSanitizerChaining() {
        ExplanationSanitizer sanitizer = new ExplanationSanitizer(0.30)
            .addValidator(mockExplainer1)
            .addValidator(mockExplainer2);
        
        // Should be chainable
        assertNotNull(sanitizer);
        
        SanitizationResult result = sanitizer.validate(baseExplanation, mockModel, new double[]{1.0, 2.0});
        assertNotNull(result);
    }
}
