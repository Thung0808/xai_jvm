package io.github.Thung0808.xai.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FeatureAttribution record.
 */
class FeatureAttributionTest {
    
    @Test
    void testBasicConstruction() {
        FeatureAttribution attr = new FeatureAttribution("age", 0.5);
        
        assertEquals("age", attr.feature());
        assertEquals(0.5, attr.importance());
        assertEquals(0.0, attr.confidenceInterval());
    }
    
    @Test
    void testConstructionWithConfidence() {
        FeatureAttribution attr = new FeatureAttribution("income", 0.8, 0.1);
        
        assertEquals("income", attr.feature());
        assertEquals(0.8, attr.importance());
        assertEquals(0.1, attr.confidenceInterval());
        assertTrue(attr.hasUncertainty());
    }
    
    @Test
    void testValidation() {
        assertThrows(IllegalArgumentException.class, 
            () -> new FeatureAttribution("", 0.5));
        
        assertThrows(IllegalArgumentException.class, 
            () -> new FeatureAttribution("feature", Double.NaN));
        
        assertThrows(IllegalArgumentException.class, 
            () -> new FeatureAttribution("feature", 0.5, -0.1));
    }
    
    @Test
    void testStabilityScore() {
        // Perfect stability (no confidence interval)
        FeatureAttribution stable = new FeatureAttribution("f1", 0.5, 0.0);
        assertEquals(1.0, stable.stabilityScore(), 0.001);
        
        // Some uncertainty
        FeatureAttribution uncertain = new FeatureAttribution("f2", 0.5, 0.1);
        assertTrue(uncertain.stabilityScore() < 1.0);
        assertTrue(uncertain.stabilityScore() > 0.5);
    }
}
