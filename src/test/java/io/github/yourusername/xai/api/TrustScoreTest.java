package io.github.Thung0808.xai.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TrustScore calculation and classification.
 */
class TrustScoreTest {
    
    @Test
    void testHighTrustScore() {
        TrustScore trust = TrustScore.builder()
            .stability(0.98)
            .variance(0.01)
            .confidenceWidth(0.02)
            .biasRisk(TrustScore.BiasRisk.LOW)
            .coverage(0.99)
            .build();
        
        assertTrue(trust.getOverallScore() >= 0.9, 
            "Expected score >= 0.9 but got " + trust.getOverallScore());
        assertEquals(TrustScore.TrustLevel.HIGH, trust.getLevel());
        assertTrue(trust.getRecommendation().contains("SAFE_FOR_AUTOMATION"));
    }
    
    @Test
    void testLowTrustScore() {
        TrustScore trust = TrustScore.builder()
            .stability(0.5)
            .variance(0.3)
            .confidenceWidth(0.4)
            .biasRisk(TrustScore.BiasRisk.HIGH)
            .coverage(0.6)
            .build();
        
        assertTrue(trust.getOverallScore() < 0.7);
        assertNotEquals(TrustScore.TrustLevel.HIGH, trust.getLevel());
    }
    
    @Test
    void testCriticalBiasRisk() {
        TrustScore trust = TrustScore.builder()
            .stability(1.0)
            .variance(0.0)
            .confidenceWidth(0.0)
            .biasRisk(TrustScore.BiasRisk.CRITICAL)
            .coverage(1.0)
            .build();
        
        // Critical bias should drag down overall score
        assertTrue(trust.getOverallScore() < 0.9);
    }
    
    @Test
    void testToMap() {
        TrustScore trust = TrustScore.builder()
            .stability(0.9)
            .variance(0.05)
            .confidenceWidth(0.1)
            .biasRisk(TrustScore.BiasRisk.MEDIUM)
            .coverage(0.95)
            .build();
        
        var map = trust.toMap();
        
        assertTrue(map.containsKey("trustScore"));
        assertTrue(map.containsKey("level"));
        assertTrue(map.containsKey("recommendation"));
        assertTrue(map.containsKey("components"));
    }
    
    @Test
    void testBoundaryConditions() {
        // Test edge cases
        TrustScore perfectTrust = TrustScore.builder()
            .stability(1.0)
            .variance(0.0)
            .confidenceWidth(0.0)
            .biasRisk(TrustScore.BiasRisk.LOW)
            .coverage(1.0)
            .build();
        
        assertEquals(1.0, perfectTrust.getOverallScore(), 0.01);
        
        TrustScore worstTrust = TrustScore.builder()
            .stability(0.0)
            .variance(1.0)
            .confidenceWidth(1.0)
            .biasRisk(TrustScore.BiasRisk.CRITICAL)
            .coverage(0.0)
            .build();
        
        assertTrue(worstTrust.getOverallScore() < 0.5);
        assertEquals(TrustScore.TrustLevel.CRITICAL, worstTrust.getLevel());
    }
}
