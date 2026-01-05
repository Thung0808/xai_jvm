package ai.xai.explainer;

import ai.xai.core.Explanation;
import ai.xai.core.Predictable;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PermutationExplainer.
 */
class PermutationExplainerTest {

    /**
     * Simple mock model: predicts 1 if sum of features > 3, else 0.
     */
    static class MockModel implements Predictable {
        @Override
        public double predict(double[] input) {
            double sum = 0;
            for (double val : input) {
                sum += val;
            }
            return sum > 3.0 ? 1.0 : 0.0;
        }
    }

    @Test
    void testPermutationExplainerBasic() {
        // Create simple dataset where feature_0 is more important
        double[][] X = {
            {1.0, 0.0},  // sum=1 → 0
            {2.0, 0.0},  // sum=2 → 0
            {3.0, 0.0},  // sum=3 → 0
            {4.0, 0.0},  // sum=4 → 1
            {5.0, 0.0},  // sum=5 → 1
        };
        
        double[] y = {0.0, 0.0, 0.0, 1.0, 1.0};

        MockModel model = new MockModel();
        PermutationExplainer explainer = new PermutationExplainer(model, X, y);
        
        Explanation explanation = explainer.explain();

        assertNotNull(explanation);
        Map<String, Double> importance = explanation.getFeatureImportance();
        
        // Should have 2 features
        assertEquals(2, importance.size());
        assertTrue(importance.containsKey("feature_0"));
        assertTrue(importance.containsKey("feature_1"));
        
        // feature_0 should be more important than feature_1
        // (since feature_1 is always 0, permuting it has no effect)
        double imp0 = importance.get("feature_0");
        double imp1 = importance.get("feature_1");
        
        assertTrue(imp0 > imp1, 
            String.format("feature_0 (%.3f) should be more important than feature_1 (%.3f)", 
                imp0, imp1));
    }

    @Test
    void testPermutationExplainerWithBalancedFeatures() {
        // Dataset where both features matter equally
        double[][] X = {
            {2.0, 2.0},  // sum=4 → 1
            {1.0, 1.0},  // sum=2 → 0
            {2.5, 2.5},  // sum=5 → 1
            {1.5, 1.5},  // sum=3 → 0
        };
        
        double[] y = {1.0, 0.0, 1.0, 0.0};

        MockModel model = new MockModel();
        PermutationExplainer explainer = new PermutationExplainer(model, X, y);
        
        Explanation explanation = explainer.explain();

        assertNotNull(explanation);
        Map<String, Double> importance = explanation.getFeatureImportance();
        
        // Both features should have similar importance
        double imp0 = importance.get("feature_0");
        double imp1 = importance.get("feature_1");
        
        // Allow some variance due to randomness in permutation
        assertEquals(imp0, imp1, 0.3, 
            "Both features should have similar importance");
    }
}
