package io.github.Thung0808.xai.compliance;

import io.github.Thung0808.xai.api.Explanation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExplanationPdfExporter.
 */
public class ExplanationPdfExporterTest {
    
    private Explanation explanation;
    
    @BeforeEach
    void setUp() {
        explanation = Explanation.builder()
            .withPrediction(0.75)
            .withBaseline(0.50)
            .addAttribution("age", 0.15)
            .addAttribution("income", -0.10)
            .addAttribution("credit_score", 0.20)
            .withMetadata("testExplainer")
            .build();
    }
    
    @Test
    void testPdfExporterCreation() {
        ExplanationPdfExporter exporter = new ExplanationPdfExporter("creditModel", "v2.1.0");
        
        assertNotNull(exporter);
    }
    
    @Test
    void testPdfExporterWithOrganization() {
        ExplanationPdfExporter exporter = new ExplanationPdfExporter("creditModel", "v2.1.0")
            .withOrganization("Acme Corp");
        
        assertNotNull(exporter);
    }
    
    @Test
    void testPdfExporterWithSignatureKey() {
        ExplanationPdfExporter exporter = new ExplanationPdfExporter("creditModel", "v2.1.0")
            .withSignatureKey("secret-key-123");
        
        assertNotNull(exporter);
    }
    
    @Test
    void testGenerateComplianceHtml() {
        ExplanationPdfExporter exporter = new ExplanationPdfExporter("creditModel", "v2.1.0")
            .withOrganization("Test Bank");
        
        String html = exporter.generateComplianceHtml(explanation);
        
        assertNotNull(html);
        assertTrue(html.length() > 0);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("creditModel"));
        assertTrue(html.contains("v2.1.0"));
        assertTrue(html.contains("Model Output"));
        assertTrue(html.contains("Feature Attributions"));
    }
    
    @Test
    void testHtmlContainsExplanationData() {
        ExplanationPdfExporter exporter = new ExplanationPdfExporter("loanModel", "v1.0.0");
        String html = exporter.generateComplianceHtml(explanation);
        
        // Check that explanation data is in HTML
        assertTrue(html.contains("0.75") || html.contains("0.7500"));  // Prediction
        assertTrue(html.contains("age") || html.contains("income"));   // Feature names
        assertTrue(html.contains("Stability Score"));
    }
    
    @Test
    void testHtmlContainsRegulatoryInfo() {
        ExplanationPdfExporter exporter = new ExplanationPdfExporter("testModel", "1.0.0");
        String html = exporter.generateComplianceHtml(explanation);
        
        // Check for regulatory compliance section
        assertTrue(html.contains("Regulatory Compliance") || html.contains("GDPR"));
        assertTrue(html.contains("compliance"));
    }
    
    @Test
    void testExporterChaining() {
        ExplanationPdfExporter exporter = new ExplanationPdfExporter("model", "1.0")
            .withOrganization("OrgName")
            .withSignatureKey("key123");
        
        assertNotNull(exporter);
        String html = exporter.generateComplianceHtml(explanation);
        assertNotNull(html);
    }
    
    @Test
    void testHtmlStructure() {
        ExplanationPdfExporter exporter = new ExplanationPdfExporter("model", "1.0");
        String html = exporter.generateComplianceHtml(explanation);
        
        // Check basic HTML structure
        assertTrue(html.contains("<html"));
        assertTrue(html.contains("</html>"));
        assertTrue(html.contains("<head>"));
        assertTrue(html.contains("</head>"));
        assertTrue(html.contains("<body>"));
        assertTrue(html.contains("</body>"));
        assertTrue(html.contains("<table"));
    }
}
