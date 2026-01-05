package io.github.Thung0808.xai.compliance;

import io.github.Thung0808.xai.api.*;
import java.time.Instant;
import java.util.*;

/**
 * Compliance report for regulatory auditing.
 * 
 * <p>Contains all information required by:
 * <ul>
 *   <li>GDPR Article 22 — Right to Explanation</li>
 *   <li>EU AI Act Article 15 — Robustness & Accuracy</li>
 *   <li>FCRA Section 615 — Adverse Action Notices</li>
 * </ul>
 */
public class ComplianceReport {
    
    private final String reportId;
    private final Instant timestamp;
    private final String modelName;
    private final String modelVersion;
    private final String explanation;
    private final Map<String, Object> metadata;
    private final List<String> regulatoryReferences;
    private String digitalSignature;
    
    public ComplianceReport(Explanation explanation, String modelName, String modelVersion) {
        this.reportId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.explanation = String.format(
            "Prediction: %.4f\nStability Score: %.2f\nFeature Attributions: %d items",
            explanation.getPrediction(),
            explanation.getStabilityScore(),
            explanation.getAttributions().size()
        );
        this.metadata = new HashMap<>();
        this.regulatoryReferences = new ArrayList<>();
        this.populateMetadata(explanation);
        this.populateRegulatoryReferences();
    }
    
    private void populateMetadata(Explanation explanation) {
        metadata.put("prediction", explanation.getPrediction());
        metadata.put("stabilityScore", explanation.getStabilityScore());
        metadata.put("baseline", explanation.getBaseline());
        
        Map<String, Double> attributions = new LinkedHashMap<>();
        for (FeatureAttribution attr : explanation.getAttributions()) {
            attributions.put(attr.feature(), attr.importance());
        }
        metadata.put("attributions", attributions);
        
        metadata.put("generatedAt", timestamp.toString());
        metadata.put("modelName", modelName);
        metadata.put("modelVersion", modelVersion);
    }
    
    private void populateRegulatoryReferences() {
        regulatoryReferences.add("GDPR Article 22: Right to meaningful explanation");
        regulatoryReferences.add("EU AI Act Article 15: Robustness, accuracy, cybersecurity");
        regulatoryReferences.add("FCRA Section 615: Adverse action notices");
        regulatoryReferences.add("Fair Credit Reporting Act: Disclosure of adverse information");
    }
    
    // Getters
    public String getReportId() { return reportId; }
    public Instant getTimestamp() { return timestamp; }
    public String getModelName() { return modelName; }
    public String getModelVersion() { return modelVersion; }
    public String getExplanation() { return explanation; }
    public Map<String, Object> getMetadata() { return metadata; }
    public List<String> getRegulatoryReferences() { return regulatoryReferences; }
    public String getDigitalSignature() { return digitalSignature; }
    public void setDigitalSignature(String signature) { this.digitalSignature = signature; }
    
    @Override
    public String toString() {
        return "ComplianceReport{" +
                "reportId='" + reportId + '\'' +
                ", timestamp=" + timestamp +
                ", modelName='" + modelName + '\'' +
                ", modelVersion='" + modelVersion + '\'' +
                '}';
    }
}
