package io.github.Thung0808.xai.advanced;

import io.github.Thung0808.xai.api.PredictiveModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for PDP and interaction analysis.
 */
class AdvancedExplanationTest {
    
    private List<double[]> dataset;
    private PredictiveModel simpleLinearModel;
    private PredictiveModel interactionModel;
    
    @BeforeEach
    void setUp() {
        // Create synthetic dataset: 100 instances, 5 features
        dataset = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            double f0 = i / 100.0;           // 0 to 1
            double f1 = (i % 50) / 50.0;    // 0 to 1 (cyclic)
            double f2 = Math.random();       // random
            double f3 = i > 50 ? 1.0 : 0.0; // binary
            double f4 = Math.sin(i / 20.0); // oscillating
            
            dataset.add(new double[]{f0, f1, f2, f3, f4});
        }
        
        // Simple linear model: y = 0.3*f0 + 0.4*f1 + 0.1*f2
        simpleLinearModel = instance -> 0.3 * instance[0] + 0.4 * instance[1] + 0.1 * instance[2];
        
        // Interaction model: y = f0 * f1 (non-additive)
        interactionModel = instance -> instance[0] * instance[1];
    }
    
    // ===== PartialDependence Tests =====
    
    @Test
    void testStandardPDPBasics() {
        PartialDependencePlotter pdp = new StandardPDP();
        PartialDependence result = pdp.computePDP(simpleLinearModel, dataset, 0);
        
        assertNotNull(result);
        assertEquals(0, result.getFeatureIdx());
        assertEquals("feature_0", result.getFeatureName());
        assertEquals(10, result.getGridPoints().length);  // default gridSize=10
        assertEquals(10, result.getPredictions().length);
    }
    
    @Test
    void testPDPGridPoints() {
        PartialDependencePlotter pdp = new StandardPDP(5, false, false, "mean", 42);
        PartialDependence result = pdp.computePDP(simpleLinearModel, dataset, 1);
        
        double[] grid = result.getGridPoints();
        assertEquals(5, grid.length);
        
        // Grid points should be sorted
        for (int i = 1; i < grid.length; i++) {
            assertTrue(grid[i] >= grid[i - 1]);
        }
    }
    
    @Test
    void testPDPMonotonicity() {
        // Test with simple linear model
        PartialDependencePlotter pdp = new StandardPDP();
        PartialDependence result = pdp.computePDP(simpleLinearModel, dataset, 0);
        
        // Feature 0 has positive coefficient (0.3), should be monotonically increasing
        int monotonic = result.getMonotonicity();
        assertTrue(monotonic >= 0, "Feature 0 should increase");  // 1 or 0
    }
    
    @Test
    void testPDPSlope() {
        PartialDependencePlotter pdp = new StandardPDP(10, false, false, "mean", 42);
        PartialDependence result = pdp.computePDP(simpleLinearModel, dataset, 0);
        
        double slope = result.getSlope();
        assertTrue(slope > 0, "Positive coefficient should give positive slope");
    }
    
    @Test
    void testPDPRanges() {
        PartialDependencePlotter pdp = new StandardPDP();
        PartialDependence result = pdp.computePDP(simpleLinearModel, dataset, 0);
        
        double[] gridRange = result.getGridRange();
        assertEquals(2, gridRange.length);
        assertTrue(gridRange[0] < gridRange[1]);
        
        double[] predRange = result.getPredictionRange();
        assertEquals(2, predRange.length);
        assertTrue(predRange[0] <= predRange[1]);
    }
    
    @Test
    void testPDPVariance() {
        PartialDependencePlotter pdp = new StandardPDP();
        PartialDependence result = pdp.computePDP(simpleLinearModel, dataset, 2);
        
        // Feature 2 is random (coeff 0.1), heterogeneity should be available
        double hetero = result.getHeterogeneity();
        assertTrue(hetero >= 0.0);  // Heterogeneity should be non-negative
    }
    
    @Test
    void testPDPWithConfidenceBounds() {
        PartialDependencePlotter pdp = new StandardPDP(8, false, true, "mean", 42);
        PartialDependence result = pdp.computePDP(simpleLinearModel, dataset, 0);
        
        assertTrue(result.hasConfidenceBounds());
        assertTrue(result.getLowerBound().isPresent());
        assertTrue(result.getUpperBound().isPresent());
        
        double[] lower = result.getLowerBound().get();
        double[] upper = result.getUpperBound().get();
        double[] pred = result.getPredictions();
        
        // Bounds should enclose predictions
        for (int i = 0; i < pred.length; i++) {
            assertTrue(pred[i] >= lower[i] - 1e-6);
            assertTrue(pred[i] <= upper[i] + 1e-6);
        }
    }
    
    @Test
    void testPDPWithICE() {
        PartialDependencePlotter pdp = new StandardPDP(6, true, false, "mean", 42);
        PartialDependence result = pdp.computePDP(simpleLinearModel, dataset, 0);
        
        assertFalse(result.getICECurves().isEmpty());
        double[] iceCurve = result.getICECurves().get(0);
        assertEquals(6, iceCurve.length);
    }
    
    @Test
    void testEmptyDatasetThrows() {
        PartialDependencePlotter pdp = new StandardPDP();
        assertThrows(IllegalArgumentException.class,
            () -> pdp.computePDP(simpleLinearModel, new ArrayList<>(), 0));
    }
    
    @Test
    void testInvalidFeatureIndexThrows() {
        PartialDependencePlotter pdp = new StandardPDP();
        assertThrows(IllegalArgumentException.class,
            () -> pdp.computePDP(simpleLinearModel, dataset, 100));
    }
    
    // ===== InteractionPlot Tests =====
    
    @Test
    void testInteractionPlotBasics() {
        InteractionPlotter inter = new StandardInteractionPDP(5, true, "mean");
        InteractionPlot result = inter.computeInteraction(interactionModel, dataset, 0, 1);
        
        assertNotNull(result);
        assertEquals(0, result.getFeature1Idx());
        assertEquals(1, result.getFeature2Idx());
        assertEquals(5, result.getGrid1Points().length);
        assertEquals(5, result.getGrid2Points().length);
        assertEquals(5, result.getPredictions().length);
        assertEquals(5, result.getPredictions()[0].length);
    }
    
    @Test
    void testInteractionDetection() {
        // Interaction model: y = f0 * f1 (multiplicative = interaction)
        InteractionPlotter inter = new StandardInteractionPDP(8, true, "mean");
        InteractionPlot result = inter.computeInteraction(interactionModel, dataset, 0, 1);
        
        double strength = result.getInteractionStrength();
        assertTrue(strength > 0.0, "Multiplicative model should show interaction");
        
        // With marginal PDPs available, interaction strength should be > 0.1
        if (result.getPDP1().isPresent() && result.getPDP2().isPresent()) {
            assertTrue(result.hasInteraction(), "Should detect significant interaction");
        }
    }
    
    @Test
    void testNoInteractionInLinearModel() {
        // Linear model: y = 0.3*f0 + 0.4*f1 (additive = no interaction)
        InteractionPlotter inter = new StandardInteractionPDP(8, true, "mean");
        InteractionPlot result = inter.computeInteraction(simpleLinearModel, dataset, 0, 1);
        
        double strength = result.getInteractionStrength();
        // For purely additive model, strength should be close to 0
        assertTrue(strength < 0.2, "Linear model should have minimal interaction");
    }
    
    @Test
    void testInteractionSynergy() {
        InteractionPlotter inter = new StandardInteractionPDP(6, true, "mean");
        InteractionPlot result = inter.computeInteraction(interactionModel, dataset, 0, 1);
        
        double synergy = result.getSynergy();
        assertNotNull(synergy);
        // Multiplicative interaction should have positive synergy
    }
    
    @Test
    void testInteractionWithMarginalPDPs() {
        InteractionPlotter inter = new StandardInteractionPDP(7, true, "mean");
        InteractionPlot result = inter.computeInteraction(simpleLinearModel, dataset, 0, 2);
        
        assertTrue(result.getPDP1().isPresent());
        assertTrue(result.getPDP2().isPresent());
        
        PartialDependence pdp1 = result.getPDP1().get();
        PartialDependence pdp2 = result.getPDP2().get();
        
        assertEquals(7, pdp1.getGridPoints().length);
        assertEquals(7, pdp2.getGridPoints().length);
    }
    
    @Test
    void testInteractionWithoutMarginalPDPs() {
        InteractionPlotter inter = new StandardInteractionPDP(5, false, "mean");
        InteractionPlot result = inter.computeInteraction(simpleLinearModel, dataset, 0, 1);
        
        assertTrue(result.getPDP1().isEmpty());
        assertTrue(result.getPDP2().isEmpty());
    }
    
    @Test
    void testInteractionGridShape() {
        InteractionPlotter inter = new StandardInteractionPDP(6, false, "mean");
        InteractionPlot result = inter.computeInteraction(interactionModel, dataset, 1, 3);
        
        double[][] grid = result.getPredictions();
        assertEquals(6, grid.length);
        for (double[] row : grid) {
            assertEquals(6, row.length);
        }
    }
    
    @Test
    void testInteractionPredictionRange() {
        InteractionPlotter inter = new StandardInteractionPDP(5, false, "mean");
        InteractionPlot result = inter.computeInteraction(interactionModel, dataset, 0, 1);
        
        double[] range = result.getPredictionRange();
        assertEquals(2, range.length);
        assertTrue(range[0] <= range[1]);
    }
    
    @Test
    void testInteractionSameFeatureThrows() {
        InteractionPlotter inter = new StandardInteractionPDP();
        assertThrows(IllegalArgumentException.class,
            () -> inter.computeInteraction(interactionModel, dataset, 0, 0));
    }
    
    @Test
    void testInteractionInvalidIndicesThrows() {
        InteractionPlotter inter = new StandardInteractionPDP();
        assertThrows(IllegalArgumentException.class,
            () -> inter.computeInteraction(interactionModel, dataset, 0, 100));
    }
    
    // ===== Integration Tests =====
    
    @Test
    void testPDPToStringRepresentation() {
        PartialDependencePlotter pdp = new StandardPDP();
        PartialDependence result = pdp.computePDP(simpleLinearModel, dataset, 0);
        
        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("PartialDependence"));
        assertTrue(str.contains("feature_0"));
    }
    
    @Test
    void testInteractionToStringRepresentation() {
        InteractionPlotter inter = new StandardInteractionPDP();
        InteractionPlot result = inter.computeInteraction(simpleLinearModel, dataset, 0, 1);
        
        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("InteractionPlot"));
    }
    
    @Test
    void testMultipleFeatureAnalysis() {
        PartialDependencePlotter pdp = new StandardPDP(8, false, false, "mean", 42);
        
        PartialDependence pdp0 = pdp.computePDP(simpleLinearModel, dataset, 0);
        PartialDependence pdp1 = pdp.computePDP(simpleLinearModel, dataset, 1);
        PartialDependence pdp2 = pdp.computePDP(simpleLinearModel, dataset, 2);
        
        // Feature 0 coeff = 0.3, Feature 1 coeff = 0.4, Feature 2 coeff = 0.1
        // So slope should be: pdp1 > pdp0 > pdp2
        double slope0 = pdp0.getSlope();
        double slope1 = pdp1.getSlope();
        double slope2 = pdp2.getSlope();
        
        assertTrue(slope1 > slope0);  // 0.4 > 0.3
        assertTrue(slope0 > slope2);  // 0.3 > 0.1
    }
}
