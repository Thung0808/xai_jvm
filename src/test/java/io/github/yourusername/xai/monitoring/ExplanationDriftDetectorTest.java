package io.github.Thung0808.xai.monitoring;

import io.github.Thung0808.xai.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExplanationDriftDetector.
 */
class ExplanationDriftDetectorTest {
    
    private ExplanationDriftDetector detector;
    
    @BeforeEach
    void setUp() {
        detector = new ExplanationDriftDetector();
    }
    
    @Test
    void testNoDrift() {
        List<Explanation> baseline = createExplanations(
            new double[]{0.5, 0.3, 0.2},
            new double[]{0.5, 0.3, 0.2},
            new double[]{0.5, 0.3, 0.2}
        );
        
        detector.setBaseline(baseline);
        
        List<Explanation> current = createExplanations(
            new double[]{0.51, 0.29, 0.20},
            new double[]{0.49, 0.31, 0.20},
            new double[]{0.50, 0.30, 0.20}
        );
        
        var report = detector.detect(current);
        
        assertFalse(report.hasDrift());
        assertEquals(ExplanationDriftDetector.DriftLevel.NONE, report.getLevel());
        assertTrue(report.getOverallDriftScore() < 0.1);
    }
    
    @Test
    void testHighDrift() {
        List<Explanation> baseline = createExplanations(
            new double[]{0.8, 0.1, 0.1},
            new double[]{0.8, 0.1, 0.1},
            new double[]{0.8, 0.1, 0.1}
        );
        
        detector.setBaseline(baseline);
        
        // Complete flip in feature importance
        List<Explanation> current = createExplanations(
            new double[]{0.1, 0.1, 0.8},
            new double[]{0.1, 0.1, 0.8},
            new double[]{0.1, 0.1, 0.8}
        );
        
        var report = detector.detect(current);
        
        assertTrue(report.hasDrift());
        assertEquals(ExplanationDriftDetector.DriftLevel.HIGH, report.getLevel());
        assertTrue(report.getOverallDriftScore() > 0.4);
    }
    
    @Test
    void testRankCorrelation() {
        // Baseline: feature1 > feature2 > feature3
        List<Explanation> baseline = createExplanations(
            new double[]{0.6, 0.3, 0.1}
        );
        
        detector.setBaseline(baseline);
        
        // Current: feature3 > feature2 > feature1 (complete reversal)
        List<Explanation> current = createExplanations(
            new double[]{0.1, 0.3, 0.6}
        );
        
        var report = detector.detect(current);
        
        // Should detect rank change
        assertTrue(report.getRankCorrelation() < 0.5);
        assertTrue(report.hasDrift());
    }
    
    @Test
    void testEmptyBaselineThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            detector.setBaseline(List.of());
        });
    }
    
    @Test
    void testDetectWithoutBaselineThrows() {
        assertThrows(IllegalStateException.class, () -> {
            detector.detect(createExplanations(new double[]{0.5, 0.5}));
        });
    }
    
    private List<Explanation> createExplanations(double[]... importanceSets) {
        List<Explanation> explanations = new ArrayList<>();
        
        for (double[] importances : importanceSets) {
            var builder = Explanation.builder()
                .withPrediction(0.7)
                .withBaseline(0.5)
                .withMetadata(ExplanationMetadata.builder("Test").build());
            
            for (int i = 0; i < importances.length; i++) {
                builder.addAttribution(new FeatureAttribution(
                    "feature" + (i + 1),
                    importances[i],
                    0.01
                ));
            }
            
            explanations.add(builder.build());
        }
        
        return explanations;
    }
}
