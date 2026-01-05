/**
 * Compliance and audit reporting for XAI explanations.
 * 
 * <p>Provides GDPR and EU AI Act compliance features:
 * <ul>
 *   <li><strong>ComplianceReport:</strong> Structured audit trail</li>
 *   <li><strong>DigitalSignature:</strong> Integrity verification (HMAC-SHA256)</li>
 *   <li><strong>ExplanationPdfExporter:</strong> PDF-ready HTML reports</li>
 * </ul>
 * 
 * <h2>Regulatory Requirements Met</h2>
 * <ul>
 *   <li><strong>GDPR Article 22:</strong> Right to meaningful explanation</li>
 *   <li><strong>EU AI Act Article 15:</strong> Robustness, accuracy, cybersecurity</li>
 *   <li><strong>FCRA Section 615:</strong> Adverse action notices with reasons</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Export to compliance-ready HTML
 * ExplanationPdfExporter exporter = new ExplanationPdfExporter("loanModel", "v2.1.0")
 *     .withOrganization("MyBank Corp")
 *     .withSignatureKey("private-key-2026");
 * 
 * String html = exporter.generateComplianceHtml(explanation);
 * Files.write(Path.of("loan_decision_123.html"), html.getBytes());
 * 
 * // Convert HTML → PDF using wkhtmltopdf or Chromium
 * // Result is audit-ready PDF with:
 * // - Timestamp
 * // - Digital signature
 * // - Compliance statements
 * // - GDPR right-to-explanation footer
 * }</pre>
 * 
 * @since 1.1.0-alpha
 */
package io.github.Thung0808.xai.compliance;
