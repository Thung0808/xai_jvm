package io.github.thung0808.xai.schema;

import io.github.thung0808.xai.api.*;
import io.github.thung0808.xai.api.Stable;
import io.github.thung0808.xai.counterfactual.CounterfactualResult;
import io.github.thung0808.xai.fairness.BiasMonitor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Standardized JSON output for all XAI explanations.
 * 
 * <p>Provides OpenAPI-friendly, frontend-consumable JSON structure:</p>
 * <pre>{@code
 * {
 *   "schemaVersion": "1.0",
 *   "prediction": 0.87,
 *   "attributions": [
 *     {"feature": "age", "importance": 0.35, "confidence": [0.32, 0.38]},
 *     {"feature": "income", "importance": 0.28, "confidence": [0.25, 0.31]}
 *   ],
 *   "counterfactuals": [...],
 *   "trustScore": {"score": 0.91, "level": "HIGH"},
 *   "fairness": {"disparateImpact": 1.0, "level": "FAIR"},
 *   "metadata": {...}
 * }
 * }</pre>
 * 
 * <p><b>Design Principles:</b></p>
 * <ul>
 *   <li>Framework-agnostic (works with React, Angular, Vue)</li>
 *   <li>BI tool friendly (Tableau, PowerBI can consume)</li>
 *   <li>Schema versioning for backward compatibility</li>
 *   <li>No rendering logic (pure data)</li>
 * </ul>
 * 
 * @since 0.6.0
 */
@Stable(since = "0.6.0")
public class ExplanationJson {
    
    public static final String SCHEMA_VERSION = "1.0";
    
    private final String schemaVersion;
    private final double prediction;
    private final List<AttributionJson> attributions;
    private final List<CounterfactualJson> counterfactuals;
    private final TrustScoreJson trustScore;
    private final FairnessJson fairness;
    private final Map<String, Object> metadata;
    
    /**
     * Creates a standardized JSON explanation.
     */
    public ExplanationJson(
            double prediction,
            List<AttributionJson> attributions,
            List<CounterfactualJson> counterfactuals,
            TrustScoreJson trustScore,
            FairnessJson fairness,
            Map<String, Object> metadata) {
        this.schemaVersion = SCHEMA_VERSION;
        this.prediction = prediction;
        this.attributions = attributions != null ? List.copyOf(attributions) : List.of();
        this.counterfactuals = counterfactuals != null ? List.copyOf(counterfactuals) : List.of();
        this.trustScore = trustScore;
        this.fairness = fairness;
        this.metadata = metadata != null ? new LinkedHashMap<>(metadata) : new LinkedHashMap<>();
    }
    
    /**
     * Converts Explanation to JSON format.
     */
    public static ExplanationJson from(Explanation explanation) {
        List<AttributionJson> attributions = explanation.getAttributions().stream()
            .map(AttributionJson::from)
            .collect(Collectors.toList());
        
        // TrustScore not available in current API - placeholder
        TrustScoreJson trustScore = null;
        
        return new ExplanationJson(
            explanation.getPrediction(),
            attributions,
            List.of(),  // Counterfactuals if available
            trustScore,
            null,  // Fairness if available
            extractMetadata(explanation)
        );
    }
    
    private static Map<String, Object> extractMetadata(Explanation explanation) {
        Map<String, Object> meta = new LinkedHashMap<>();
        ExplanationMetadata metadata = explanation.getMetadata();
        
        meta.put("explainerName", metadata.explainerName());
        meta.put("seed", metadata.seed());
        meta.put("trials", metadata.trials());
        meta.put("timestamp", metadata.timestamp().toString());
        meta.put("libraryVersion", metadata.libraryVersion());
        
        if (metadata.featureSchemaHash() != null) {
            meta.put("featureSchemaHash", metadata.featureSchemaHash());
        }
        
        return meta;
    }
    
    /**
     * Converts to JSON string (manually constructed for zero deps).
     */
    public String toJsonString() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"schemaVersion\": \"").append(schemaVersion).append("\",\n");
        json.append("  \"prediction\": ").append(prediction).append(",\n");
        
        // Attributions
        json.append("  \"attributions\": [\n");
        for (int i = 0; i < attributions.size(); i++) {
            json.append("    ").append(attributions.get(i).toJsonString());
            if (i < attributions.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");
        
        // Counterfactuals
        json.append("  \"counterfactuals\": [\n");
        for (int i = 0; i < counterfactuals.size(); i++) {
            json.append("    ").append(counterfactuals.get(i).toJsonString());
            if (i < counterfactuals.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");
        
        // Trust score
        if (trustScore != null) {
            json.append("  \"trustScore\": ").append(trustScore.toJsonString()).append(",\n");
        }
        
        // Fairness
        if (fairness != null) {
            json.append("  \"fairness\": ").append(fairness.toJsonString()).append(",\n");
        }
        
        // Metadata
        json.append("  \"metadata\": ").append(metadataToJson()).append("\n");
        json.append("}");
        
        return json.toString();
    }
    
    private String metadataToJson() {
        StringBuilder json = new StringBuilder("{");
        List<String> entries = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                entries.add("\"" + key + "\": \"" + value + "\"");
            } else if (value instanceof Number) {
                entries.add("\"" + key + "\": " + value);
            } else if (value instanceof Boolean) {
                entries.add("\"" + key + "\": " + value);
            }
        }
        
        json.append(String.join(", ", entries));
        json.append("}");
        return json.toString();
    }
    
    // Getters for programmatic access
    public String getSchemaVersion() { return schemaVersion; }
    public double getPrediction() { return prediction; }
    public List<AttributionJson> getAttributions() { return attributions; }
    public List<CounterfactualJson> getCounterfactuals() { return counterfactuals; }
    public TrustScoreJson getTrustScore() { return trustScore; }
    public FairnessJson getFairness() { return fairness; }
    public Map<String, Object> getMetadata() { return new LinkedHashMap<>(metadata); }
    
    /**
     * Feature attribution in JSON format.
     */
    public static class AttributionJson {
        private final String feature;
        private final double importance;
        private final double[] confidenceInterval;
        
        public AttributionJson(String feature, double importance, double[] confidenceInterval) {
            this.feature = feature;
            this.importance = importance;
            this.confidenceInterval = confidenceInterval;
        }
        
        public static AttributionJson from(FeatureAttribution attr) {
            double lower = attr.importance() - attr.confidenceInterval();
            double upper = attr.importance() + attr.confidenceInterval();
            return new AttributionJson(attr.feature(), attr.importance(), new double[]{lower, upper});
        }
        
        public String toJsonString() {
            return String.format(
                "{\"feature\": \"%s\", \"importance\": %.6f, \"confidenceInterval\": [%.6f, %.6f]}",
                feature, importance, confidenceInterval[0], confidenceInterval[1]
            );
        }
        
        public String getFeature() { return feature; }
        public double getImportance() { return importance; }
        public double[] getConfidenceInterval() { return confidenceInterval.clone(); }
    }
    
    /**
     * Counterfactual in JSON format.
     */
    public static class CounterfactualJson {
        private final Map<String, Double> changes;
        private final double prediction;
        private final double distance;
        
        public CounterfactualJson(Map<String, Double> changes, double prediction, double distance) {
            this.changes = new LinkedHashMap<>(changes);
            this.prediction = prediction;
            this.distance = distance;
        }
        
        public String toJsonString() {
            StringBuilder json = new StringBuilder("{");
            json.append("\"changes\": {");
            
            List<String> entries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : changes.entrySet()) {
                entries.add(String.format("\"%s\": %.6f", entry.getKey(), entry.getValue()));
            }
            json.append(String.join(", ", entries));
            json.append("}, ");
            json.append(String.format("\"prediction\": %.6f, ", prediction));
            json.append(String.format("\"distance\": %.6f", distance));
            json.append("}");
            return json.toString();
        }
        
        public Map<String, Double> getChanges() { return new LinkedHashMap<>(changes); }
        public double getPrediction() { return prediction; }
        public double getDistance() { return distance; }
    }
    
    /**
     * Trust score in JSON format.
     */
    public static class TrustScoreJson {
        private final double score;
        private final String level;
        private final Map<String, Double> components;
        
        public TrustScoreJson(double score, String level, Map<String, Double> components) {
            this.score = score;
            this.level = level;
            this.components = new LinkedHashMap<>(components);
        }
        
        public static TrustScoreJson from(TrustScore trust) {
            Map<String, Double> components = new LinkedHashMap<>();
            components.put("stability", trust.getStability());
            components.put("variance", trust.getVariance());
            components.put("confidence", trust.getConfidenceWidth());
            components.put("bias", 1.0);
            components.put("coverage", trust.getCoverage());
            
            return new TrustScoreJson(trust.getOverallScore(), trust.getLevel().name(), components);
        }
        
        public String toJsonString() {
            StringBuilder json = new StringBuilder("{");
            json.append(String.format("\"score\": %.4f, ", score));
            json.append(String.format("\"level\": \"%s\", ", level));
            json.append("\"components\": {");
            
            List<String> entries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : components.entrySet()) {
                entries.add(String.format("\"%s\": %.4f", entry.getKey(), entry.getValue()));
            }
            json.append(String.join(", ", entries));
            json.append("}}");
            return json.toString();
        }
        
        public double getScore() { return score; }
        public String getLevel() { return level; }
        public Map<String, Double> getComponents() { return new LinkedHashMap<>(components); }
    }
    
    /**
     * Fairness metrics in JSON format.
     */
    public static class FairnessJson {
        private final double disparateImpact;
        private final String level;
        private final Map<String, Double> groupMetrics;
        
        public FairnessJson(double disparateImpact, String level, Map<String, Double> groupMetrics) {
            this.disparateImpact = disparateImpact;
            this.level = level;
            this.groupMetrics = new LinkedHashMap<>(groupMetrics);
        }
        
        public String toJsonString() {
            StringBuilder json = new StringBuilder("{");
            json.append(String.format("\"disparateImpact\": %.4f, ", disparateImpact));
            json.append(String.format("\"level\": \"%s\", ", level));
            json.append("\"groupMetrics\": {");
            
            List<String> entries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : groupMetrics.entrySet()) {
                entries.add(String.format("\"%s\": %.4f", entry.getKey(), entry.getValue()));
            }
            json.append(String.join(", ", entries));
            json.append("}}");
            return json.toString();
        }
        
        public double getDisparateImpact() { return disparateImpact; }
        public String getLevel() { return level; }
        public Map<String, Double> getGroupMetrics() { return new LinkedHashMap<>(groupMetrics); }
    }
}


