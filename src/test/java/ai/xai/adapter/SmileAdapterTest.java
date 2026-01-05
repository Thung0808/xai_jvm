package ai.xai.adapter;

import ai.xai.core.Predictable;
import org.junit.jupiter.api.Test;
import smile.classification.LogisticRegression;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SmileAdapter.
 */
class SmileAdapterTest {

    @Test
    void testSmileAdapterPredict() {
        // Create simple training data
        double[][] X = {
            {1.0, 2.0},
            {2.0, 3.0},
            {3.0, 4.0},
            {4.0, 5.0}
        };
        
        int[] y = {0, 0, 1, 1};

        // Train a Smile model
        LogisticRegression model = LogisticRegression.fit(X, y);
        
        // Wrap it with adapter
        Predictable predictable = new SmileAdapter(model);

        // Test prediction
        double pred1 = predictable.predict(new double[]{1.0, 2.0});
        double pred2 = predictable.predict(new double[]{4.0, 5.0});

        // Check predictions are either 0.0 or 1.0
        assertTrue(pred1 == 0.0 || pred1 == 1.0);
        assertTrue(pred2 == 0.0 || pred2 == 1.0);
        
        // Model should predict class 0 for low values and class 1 for high values
        assertTrue(pred1 <= pred2, "Higher input should have higher/equal prediction");
    }

    @Test
    void testSmileAdapterNotNull() {
        double[][] X = {{1.0}, {2.0}};
        int[] y = {0, 1};
        
        LogisticRegression model = LogisticRegression.fit(X, y);
        SmileAdapter adapter = new SmileAdapter(model);

        assertNotNull(adapter);
        assertNotNull(adapter.predict(new double[]{1.5}));
    }
}
