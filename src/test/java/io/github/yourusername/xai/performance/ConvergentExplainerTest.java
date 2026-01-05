package io.github.Thung0808.xai.performance;

import io.github.Thung0808.xai.api.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConvergentExplainer.
 */
class ConvergentExplainerTest {
    
    @Test
    void testConvergenceDetection() {
        // Simple deterministic model
        PredictiveModel model = input -> input[0] * 2 + input[1] * -1;
        
        ModelContext context = new ModelContext(
            List.of("feature1", "feature2"),
            new double[]{0.0, 0.0}
        );
        
        ConvergentExplainer explainer = new ConvergentExplainer(
            context,
            0.05,  // 5% threshold
            10,
            50,
            10
        );
        
        double[] input = {1.0, 2.0};
        Explanation result = explainer.explain(model, input);
        
        assertNotNull(result);
        assertEquals(2, result.getAttributions().size());
        
        // Should have converged (check metadata)
        assertNotNull(result.getMetadata());
        assertEquals("ConvergentExplainer", result.getMetadata().explainerName());
    }
    
    @Test
    void testConvergenceReport() {
        PredictiveModel model = input -> input[0] + input[1];
        
        ModelContext context = new ModelContext(
            List.of("x", "y"),
            new double[]{0.0, 0.0}
        );
        
        ConvergentExplainer explainer = new ConvergentExplainer(context);
        
        double[] input = {3.0, 4.0};
        
        var report = explainer.analyzeConvergence(model, input);
        
        assertNotNull(report);
        assertFalse(report.sampleCounts().isEmpty());
        assertFalse(report.maxChanges().isEmpty());
        assertEquals(report.sampleCounts().size(), report.maxChanges().size());
        
        // Should have some convergence info
        String reportStr = report.toString();
        assertTrue(reportStr.contains("Convergence Analysis"));
        assertTrue(reportStr.contains("Epsilon threshold"));
    }
    
    @Test
    void testNoConvergence() {
        // Highly random model (won't converge easily)
        PredictiveModel randomModel = input -> Math.random() * 100;
        
        ModelContext context = new ModelContext(
            List.of("random_feature"),
            new double[]{0.0}
        );
        
        ConvergentExplainer explainer = new ConvergentExplainer(
            context,
            0.001,  // Very strict threshold
            5,
            20,     // Low max to force non-convergence
            5
        );
        
        double[] input = {1.0};
        
        // Should still return a result even without convergence
        Explanation result = explainer.explain(randomModel, input);
        assertNotNull(result);
        assertEquals(1, result.getAttributions().size());
    }
    
    @Test
    void testMaxChangeComputation() {
        // Test that convergence is properly detected
        PredictiveModel stableModel = input -> {
            // Very stable model
            return input[0] * 1.0;
        };
        
        ModelContext context = new ModelContext(
            List.of("stable_feature"),
            new double[]{5.0}
        );
        
        ConvergentExplainer explainer = new ConvergentExplainer(
            context,
            0.1,  // 10% threshold (generous)
            10,
            30,
            5
        );
        
        double[] input = {10.0};
        
        var report = explainer.analyzeConvergence(stableModel, input);
        
        // Should converge quickly for stable model
        assertTrue(report.hasConverged());
        assertTrue(report.convergenceSamples() > 0);
        assertTrue(report.convergenceSamples() <= 30);
    }
}
