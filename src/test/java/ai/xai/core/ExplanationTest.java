package ai.xai.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Explanation class.
 */
class ExplanationTest {

    @Test
    void testExplanationCreation() {
        Map<String, Double> importance = new HashMap<>();
        importance.put("feature_0", 0.5);
        importance.put("feature_1", 0.3);

        Explanation explanation = new Explanation(importance);

        assertNotNull(explanation);
        assertEquals(2, explanation.getFeatureImportance().size());
        assertEquals(0.5, explanation.getFeatureImportance().get("feature_0"));
        assertEquals(0.3, explanation.getFeatureImportance().get("feature_1"));
    }

    @Test
    void testExplanationToString() {
        Map<String, Double> importance = new HashMap<>();
        importance.put("feature_0", 0.5);

        Explanation explanation = new Explanation(importance);
        String result = explanation.toString();

        assertTrue(result.contains("Explanation"));
        assertTrue(result.contains("feature_0"));
    }
}
