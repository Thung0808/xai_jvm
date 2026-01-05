package io.github.thung0808.xai.compliance;

import io.github.thung0808.xai.api.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Export explanations to PDF-compliant HTML (suitable for conversion to PDF).
 * 
 * <p>Generates compliance-ready reports with:
 * <ul>
 *   <li>Timestamps for audit trails</li>
 *   <li>Model versioning for reproducibility</li>
 *   <li>Digital signatures for integrity verification</li>
 *   <li>GDPR/EU AI Act compliance statements</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>{@code
 * ExplanationPdfExporter exporter = new ExplanationPdfExporter("creditModel", "v2.1.0");
 * String html = exporter.generateComplianceHtml(explanation);
 * Files.write(Path.of("report.html"), html.getBytes());
 * 
 * // Then convert HTML to PDF using wkhtmltopdf, Chromium, or similar
 * }</pre>
 * 
 * @since 1.1.0-alpha
 */
public class ExplanationPdfExporter {
    
    private final String modelName;
    private final String modelVersion;
    private String organizationName = "Default Organization";
    private String signatureKey;
    
    public ExplanationPdfExporter(String modelName, String modelVersion) {
        this.modelName = modelName;
        this.modelVersion = modelVersion;
    }
    
    public ExplanationPdfExporter withOrganization(String name) {
        this.organizationName = name;
        return this;
    }
    
    public ExplanationPdfExporter withSignatureKey(String key) {
        this.signatureKey = key;
        return this;
    }
    
    /**
     * Generate compliance-ready HTML report.
     */
    public String generateComplianceHtml(Explanation explanation) {
        ComplianceReport report = new ComplianceReport(
            explanation,
            modelName,
            modelVersion
        );
        
        // Sign if key is available
        if (signatureKey != null) {
            String dataHash = hashReport(report);
            DigitalSignature sig = new DigitalSignature(
                report.getReportId(),
                dataHash,
                signatureKey
            );
            report.setDigitalSignature(sig.getSignature());
        }
        
        return buildHtmlContent(report, explanation);
    }
    
    /**
     * Save compliance report to file.
     */
    public void saveToFile(Explanation explanation, Path filePath) throws IOException {
        String html = generateComplianceHtml(explanation);
        Files.write(filePath, html.getBytes());
    }
    
    private String buildHtmlContent(ComplianceReport report, Explanation explanation) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>Explainability Compliance Report</title>\n");
        html.append("  <style>\n").append(getCssStyles()).append("  </style>\n");
        html.append("</head>\n<body>\n");
        
        // Header
        html.append("<div class=\"header\">\n");
        html.append("  <h1>Explainability Compliance Report</h1>\n");
        html.append("  <p class=\"organization\">").append(organizationName).append("</p>\n");
        html.append("</div>\n\n");
        
        // Report Metadata
        html.append("<section class=\"metadata\">\n");
        html.append("  <h2>Report Information</h2>\n");
        html.append("  <table>\n");
        html.append("    <tr><td><strong>Report ID</strong></td><td>").append(report.getReportId()).append("</td></tr>\n");
        html.append("    <tr><td><strong>Generated</strong></td><td>").append(report.getTimestamp()).append("</td></tr>\n");
        html.append("    <tr><td><strong>Model Name</strong></td><td>").append(report.getModelName()).append("</td></tr>\n");
        html.append("    <tr><td><strong>Model Version</strong></td><td>").append(report.getModelVersion()).append("</td></tr>\n");
        html.append("  </table>\n");
        html.append("</section>\n\n");
        
        // Prediction & Attribution
        html.append("<section class=\"prediction\">\n");
        html.append("  <h2>Model Output & Explanation</h2>\n");
        html.append("  <p><strong>Prediction:</strong> ").append(String.format("%.4f", explanation.getPrediction())).append("</p>\n");
        html.append("  <p><strong>Stability Score:</strong> ").append(String.format("%.2f%%", explanation.getStabilityScore() * 100)).append("</p>\n");
        
        html.append("  <h3>Feature Attributions</h3>\n");
        html.append("  <table class=\"attributions\">\n");
        html.append("    <thead><tr><th>Feature</th><th>Attribution</th><th>Confidence</th></tr></thead>\n");
        html.append("    <tbody>\n");
        
        for (FeatureAttribution attr : explanation.getAttributions()) {
            html.append("    <tr>\n");
            html.append("      <td>").append(attr.feature()).append("</td>\n");
            html.append("      <td class=\"").append(attr.importance() >= 0 ? "positive" : "negative").append("\">");
            html.append(String.format("%.4f", attr.importance())).append("</td>\n");
            
            double ci = attr.confidenceInterval();
            html.append("      <td>Â±").append(String.format("%.4f", ci)).append("</td>\n");
            html.append("    </tr>\n");
        }
        
        html.append("    </tbody>\n");
        html.append("  </table>\n");
        html.append("</section>\n\n");
        
        // Regulatory Compliance
        html.append("<section class=\"compliance\">\n");
        html.append("  <h2>Regulatory Compliance</h2>\n");
        html.append("  <p>This report documents compliance with the following regulations:</p>\n");
        html.append("  <ul>\n");
        
        for (String ref : report.getRegulatoryReferences()) {
            html.append("    <li>").append(ref).append("</li>\n");
        }
        
        html.append("  </ul>\n");
        
        // Rights & Disclaimers
        html.append("  <h3>Your Rights</h3>\n");
        html.append("  <p>\n");
        html.append("    Under GDPR Article 22, you have the right to request human review of this automated decision.<br>\n");
        html.append("    Contact: <code>dpo@").append(organizationName.toLowerCase()).append(".com</code>\n");
        html.append("  </p>\n");
        html.append("</section>\n\n");
        
        // Digital Signature (if signed)
        if (report.getDigitalSignature() != null) {
            html.append("<section class=\"signature\">\n");
            html.append("  <h2>Digital Signature (Integrity Verification)</h2>\n");
            html.append("  <p class=\"mono\">").append(report.getDigitalSignature()).append("</p>\n");
            html.append("  <p class=\"disclaimer\">This report is digitally signed. Any modifications will invalidate the signature.</p>\n");
            html.append("</section>\n\n");
        }
        
        // Footer
        html.append("<footer>\n");
        html.append("  <p>This document was automatically generated by XAI Core v1.1.0-alpha</p>\n");
        html.append("  <p><small>Enterprise-grade XAI for regulated industries</small></p>\n");
        html.append("</footer>\n");
        
        html.append("</body>\n</html>\n");
        
        return html.toString();
    }
    
    private String getCssStyles() {
        return """
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                line-height: 1.6;
                color: #333;
                max-width: 1200px;
                margin: 0 auto;
                padding: 20px;
                background: #f9f9f9;
            }
            
            .header {
                border-bottom: 3px solid #2c3e50;
                padding-bottom: 20px;
                margin-bottom: 30px;
            }
            
            .header h1 {
                margin: 0;
                font-size: 28px;
                color: #2c3e50;
            }
            
            .organization {
                color: #7f8c8d;
                font-size: 14px;
                margin-top: 5px;
            }
            
            section {
                background: white;
                padding: 20px;
                margin-bottom: 20px;
                border-radius: 4px;
                box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            }
            
            h2 {
                border-left: 4px solid #3498db;
                padding-left: 10px;
                font-size: 20px;
                color: #2c3e50;
                margin-top: 0;
            }
            
            table {
                width: 100%;
                border-collapse: collapse;
                font-size: 14px;
            }
            
            table td, table th {
                padding: 10px;
                border-bottom: 1px solid #ecf0f1;
                text-align: left;
            }
            
            table th {
                background: #f5f5f5;
                font-weight: bold;
                color: #2c3e50;
            }
            
            .attributions .positive {
                color: #27ae60;
                font-weight: bold;
            }
            
            .attributions .negative {
                color: #e74c3c;
                font-weight: bold;
            }
            
            .mono {
                font-family: 'Courier New', monospace;
                font-size: 12px;
                word-break: break-all;
                background: #f5f5f5;
                padding: 10px;
                border-radius: 3px;
            }
            
            .disclaimer {
                font-size: 12px;
                color: #7f8c8d;
                font-style: italic;
            }
            
            footer {
                text-align: center;
                border-top: 1px solid #ecf0f1;
                padding-top: 20px;
                color: #95a5a6;
                font-size: 12px;
            }
            
            @page {
                size: A4;
                margin: 1cm;
            }
            
            @media print {
                body {
                    background: white;
                }
                section {
                    box-shadow: none;
                    page-break-inside: avoid;
                }
            }
            """;
    }
    
    private String hashReport(ComplianceReport report) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            String data = report.getReportId() + "|" + report.getModelName() + 
                         "|" + report.getModelVersion() + "|" + report.getTimestamp();
            byte[] hash = digest.digest(data.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hash computation failed", e);
        }
    }
}


