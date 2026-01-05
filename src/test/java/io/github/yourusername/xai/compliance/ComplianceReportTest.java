package io.github.Thung0808.xai.compliance;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.api.ExplanationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ComplianceReport.
 */
public class ComplianceReportTest {
    
    private Explanation explanation;
    
    @BeforeEach
    void setUp() {
        // Create a simple explanation for testing
        explanation = Explanation.builder()
            .withPrediction(0.85)
            .withBaseline(0.5)
            .addAttribution("feature_1", 0.25)
            .addAttribution("feature_2", 0.10)
            .withMetadata("testExplainer")
            .build();
    }
    
    @Test
    void testComplianceReportCreation() {
        ComplianceReport report = new ComplianceReport(explanation, "testModel", "1.0.0");
        
        assertNotNull(report);
        assertNotNull(report.getReportId());
        assertNotNull(report.getTimestamp());
        assertEquals("testModel", report.getModelName());
        assertEquals("1.0.0", report.getModelVersion());
    }
    
    @Test
    void testComplianceReportMetadata() {
        ComplianceReport report = new ComplianceReport(explanation, "creditModel", "2.1.0");
        var metadata = report.getMetadata();
        
        assertTrue(metadata.containsKey("prediction"));
        assertTrue(metadata.containsKey("stabilityScore"));
        assertTrue(metadata.containsKey("attributions"));
        assertTrue(metadata.containsKey("modelName"));
        assertTrue(metadata.containsKey("modelVersion"));
        
        assertEquals(0.85, (double) metadata.get("prediction"), 0.001);
        assertEquals("creditModel", metadata.get("modelName"));
    }
    
    @Test
    void testRegulatoryReferences() {
        ComplianceReport report = new ComplianceReport(explanation, "testModel", "1.0.0");
        var refs = report.getRegulatoryReferences();
        
        assertTrue(!refs.isEmpty());
        assertTrue(refs.stream().anyMatch(r -> r.contains("GDPR")));
        assertTrue(refs.stream().anyMatch(r -> r.contains("EU AI Act")));
    }
    
    @Test
    void testDigitalSignature() {
        ComplianceReport report = new ComplianceReport(explanation, "testModel", "1.0.0");
        
        assertNull(report.getDigitalSignature());
        
        String sig = "test-signature-123";
        report.setDigitalSignature(sig);
        assertEquals(sig, report.getDigitalSignature());
    }
    
    @Test
    void testToString() {
        ComplianceReport report = new ComplianceReport(explanation, "testModel", "1.0.0");
        String str = report.toString();
        
        assertTrue(str.contains("testModel"));
        assertTrue(str.contains("1.0.0"));
    }
}
