package io.github.Thung0808.xai.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Explanation builder and functionality.
 */
class ExplanationTest {
    
    @Test
    void testBuilderPattern() {
        Explanation exp = Explanation.builder()
            .withPrediction(0.85)
            .withBaseline(0.5)
            .addAttribution("feature_0", 0.35)
            .addAttribution("feature_1", -0.15)
            .withMetadata("TestExplainer")
            .build();
        
        assertEquals(0.85, exp.getPrediction());
        assertEquals(0.5, exp.getBaseline());
        assertEquals(2, exp.getAttributions().size());
        assertNotNull(exp.getMetadata());
    }
    
    @Test
    void testTopAttributions() {
        Explanation exp = Explanation.builder()
            .withPrediction(1.0)
            .withBaseline(0.0)
            .addAttribution("low", 0.1)
            .addAttribution("high", 0.9)
            .addAttribution("medium", 0.5)
            .withMetadata("TestExplainer")
            .build();
        
        var top = exp.getTopAttributions();
        assertEquals("high", top.get(0).feature());
        assertEquals("medium", top.get(1).feature());
        assertEquals("low", top.get(2).feature());
    }
    
    @Test
    void testTopNAttributions() {
        Explanation exp = Explanation.builder()
            .withPrediction(1.0)
            .withBaseline(0.0)
            .addAttribution("f1", 0.1)
            .addAttribution("f2", 0.9)
            .addAttribution("f3", 0.5)
            .withMetadata("TestExplainer")
            .build();
        
        var top2 = exp.getTopAttributions(2);
        assertEquals(2, top2.size());
        assertEquals("f2", top2.get(0).feature());
    }
    
    @Test
    void testImmutability() {
        Explanation exp = Explanation.builder()
            .withPrediction(1.0)
            .addAttribution("f1", 0.5)
            .withMetadata("TestExplainer")
            .build();
        
        assertThrows(UnsupportedOperationException.class, 
            () -> exp.getAttributions().add(new FeatureAttribution("f2", 0.3)));
    }
    
    @Test
    void testMetadataRequired() {
        assertThrows(IllegalStateException.class, () -> 
            Explanation.builder()
                .withPrediction(1.0)
                .build()
        );
    }
}
