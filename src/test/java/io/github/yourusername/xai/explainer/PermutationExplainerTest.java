package io.github.Thung0808.xai.explainer;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.PredictiveModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the refactored PermutationExplainer.
 */
class PermutationExplainerTest {
    
    @Test
    void testBasicExplanation() {
        // Simple linear model
        PredictiveModel model = input -> input[0] * 0.5 + input[1] * 0.3;
        
        PermutationExplainer explainer = new PermutationExplainer();
        double[] input = {2.0, 3.0};
        
        Explanation exp = explainer.explain(model, input);
        
        assertNotNull(exp);
        assertEquals(2, exp.getAttributions().size());
        assertEquals(2.0 * 0.5 + 3.0 * 0.3, exp.getPrediction(), 0.001);
    }
    
    @Test
    void testFeatureImportanceOrder() {
        // Model where first feature is more important
        PredictiveModel model = input -> input[0] * 0.9 + input[1] * 0.1;
        
        PermutationExplainer explainer = new PermutationExplainer(42, 20, 0.1);
        double[] input = {5.0, 5.0};
        
        Explanation exp = explainer.explain(model, input);
        
        var top = exp.getTopAttributions();
        assertEquals("feature_0", top.get(0).feature());
        assertTrue(top.get(0).importance() > top.get(1).importance());
    }
    
    @Test
    void testReproducibility() {
        PredictiveModel model = input -> input[0] * 0.5;
        
        PermutationExplainer explainer1 = new PermutationExplainer(12345, 10, 0.0);
        PermutationExplainer explainer2 = new PermutationExplainer(12345, 10, 0.0);
        
        double[] input = {10.0};
        
        Explanation exp1 = explainer1.explain(model, input);
        Explanation exp2 = explainer2.explain(model, input);
        
        // Same seed should give similar results
        assertEquals(exp1.getPrediction(), exp2.getPrediction(), 0.001);
    }
    
    @Test
    void testValidation() {
        PermutationExplainer explainer = new PermutationExplainer();
        
        assertThrows(IllegalArgumentException.class, 
            () -> explainer.explain(null, new double[]{1.0}));
        
        PredictiveModel model = input -> 1.0;
        assertThrows(IllegalArgumentException.class, 
            () -> explainer.explain(model, null));
        
        assertThrows(IllegalArgumentException.class, 
            () -> explainer.explain(model, new double[]{}));
    }
}
