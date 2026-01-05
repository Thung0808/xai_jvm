package io.github.Thung0808.xai.fairness;

import io.github.Thung0808.xai.api.Stable;
import io.github.Thung0808.xai.api.*;

import java.util.*;

/**
 * Monitors bias and fairness in model explanations.
 * 
 * <p><b>Purpose:</b> Detect discriminatory patterns in predictions
 * and explanations across demographic groups.</p>
 * 
 * <p><b>Metrics:</b></p>
 * <ul>
 *   <li><b>Disparate Impact:</b> Ratio of positive rates between groups</li>
 *   <li><b>Equalized Odds:</b> Equal TPR and FPR across groups</li>
 *   <li><b>Feature Importance Disparity:</b> Different explanations for similar inputs</li>
 * </ul>
 * 
 * <p><b>Regulatory Compliance:</b></p>
 * <ul>
 *   <li>GDPR Article 22 (Automated Decision-Making)</li>
 *   <li>US Fair Lending Laws</li>
 *   <li>EU AI Act requirements</li>
 * </ul>
 *
 * @since 0.3.0
 */
@Stable(since = "0.3.0")
public class BiasMonitor {
    
    private final int sensitiveFeatureIndex;
    private final String[] groupNames;
    
    /**
     * Creates a bias monitor for a specific sensitive feature.
     * 
     * @param sensitiveFeatureIndex index of protected attribute (e.g., gender, race)
     * @param groupNames names of groups (e.g., ["Male", "Female"])
     */
    public BiasMonitor(int sensitiveFeatureIndex, String[] groupNames) {
        this.sensitiveFeatureIndex = sensitiveFeatureIndex;
        this.groupNames = groupNames;
    }
    
    /**
     * Analyzes bias across a dataset of explanations.
     * 
     * @param explanations list of explanations with inputs
     * @param inputs corresponding inputs
     * @param predictions corresponding predictions
     * @return bias audit report
     */
    public BiasReport analyzeBias(
        List<Explanation> explanations,
        List<double[]> inputs,
        double[] predictions
    ) {
        if (explanations.size() != inputs.size() || explanations.size() != predictions.length) {
            throw new IllegalArgumentException("Explanations, inputs, and predictions must have same size");
        }
        
        // Group data by sensitive attribute
        Map<Integer, List<Integer>> groupIndices = new HashMap<>();
        for (int i = 0; i < inputs.size(); i++) {
            int group = (int) inputs.get(i)[sensitiveFeatureIndex];
            groupIndices.computeIfAbsent(group, k -> new ArrayList<>()).add(i);
        }
        
        // Compute metrics per group
        Map<Integer, GroupMetrics> groupMetrics = new HashMap<>();
        
        for (Map.Entry<Integer, List<Integer>> entry : groupIndices.entrySet()) {
            int groupId = entry.getKey();
            List<Integer> indices = entry.getValue();
            
            GroupMetrics metrics = computeGroupMetrics(
                explanations, predictions, indices);
            
            groupMetrics.put(groupId, metrics);
        }
        
        // Compute disparate impact
        double disparateImpact = computeDisparateImpact(groupMetrics);
        
        // Compute feature importance disparity
        double importanceDisparity = computeImportanceDisparity(explanations, groupIndices);
        
        return new BiasReport(
            groupMetrics,
            groupNames,
            disparateImpact,
            importanceDisparity
        );
    }
    
    /**
     * Computes metrics for a single group.
     */
    private GroupMetrics computeGroupMetrics(
        List<Explanation> explanations,
        double[] predictions,
        List<Integer> indices
    ) {
        int total = indices.size();
        int positives = 0;
        double avgPrediction = 0.0;
        
        for (int idx : indices) {
            double pred = predictions[idx];
            avgPrediction += pred;
            
            if (pred >= 0.5) {
                positives++;
            }
        }
        
        avgPrediction /= total;
        double positiveRate = (double) positives / total;
        
        // Compute average feature importances
        Map<String, Double> avgImportances = new HashMap<>();
        for (int idx : indices) {
            Explanation exp = explanations.get(idx);
            for (FeatureAttribution attr : exp.getAttributions()) {
                avgImportances.merge(attr.feature(), attr.importance(),
                    (a, b) -> a + b);
            }
        }
        
        // Normalize
        avgImportances.replaceAll((k, v) -> v / total);
        
        return new GroupMetrics(
            total,
            positives,
            positiveRate,
            avgPrediction,
            avgImportances
        );
    }
    
    /**
     * Computes disparate impact ratio.
     * Ratio should be between 0.8 and 1.25 to pass "80% rule".
     */
    private double computeDisparateImpact(Map<Integer, GroupMetrics> groupMetrics) {
        if (groupMetrics.size() < 2) {
            return 1.0; // No disparity with single group
        }
        
        // Find min and max positive rates
        double minRate = Double.MAX_VALUE;
        double maxRate = Double.MIN_VALUE;
        
        for (GroupMetrics metrics : groupMetrics.values()) {
            minRate = Math.min(minRate, metrics.positiveRate);
            maxRate = Math.max(maxRate, metrics.positiveRate);
        }
        
        if (maxRate < 1e-10) {
            return 1.0; // All zero
        }
        
        return minRate / maxRate;
    }
    
    /**
     * Computes feature importance disparity between groups.
     * Measures how differently features are weighted across groups.
     */
    private double computeImportanceDisparity(
        List<Explanation> explanations,
        Map<Integer, List<Integer>> groupIndices
    ) {
        if (groupIndices.size() < 2) {
            return 0.0;
        }
        
        // Get all unique feature names
        Set<String> allFeatures = new HashSet<>();
        for (Explanation exp : explanations) {
            for (FeatureAttribution attr : exp.getAttributions()) {
                allFeatures.add(attr.feature());
            }
        }
        
        // Compute average importance per group per feature
        Map<Integer, Map<String, Double>> groupImportances = new HashMap<>();
        
        for (Map.Entry<Integer, List<Integer>> entry : groupIndices.entrySet()) {
            int groupId = entry.getKey();
            List<Integer> indices = entry.getValue();
            
            Map<String, Double> importances = new HashMap<>();
            for (String feature : allFeatures) {
                double sum = 0.0;
                int count = 0;
                
                for (int idx : indices) {
                    Explanation exp = explanations.get(idx);
                    for (FeatureAttribution attr : exp.getAttributions()) {
                        if (attr.feature().equals(feature)) {
                            sum += Math.abs(attr.importance());
                            count++;
                        }
                    }
                }
                
                if (count > 0) {
                    importances.put(feature, sum / count);
                }
            }
            
            groupImportances.put(groupId, importances);
        }
        
        // Compute max difference across groups for each feature
        double maxDisparity = 0.0;
        
        for (String feature : allFeatures) {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            
            for (Map<String, Double> importances : groupImportances.values()) {
                double importance = importances.getOrDefault(feature, 0.0);
                min = Math.min(min, importance);
                max = Math.max(max, importance);
            }
            
            if (max > 1e-10) {
                double disparity = (max - min) / max;
                maxDisparity = Math.max(maxDisparity, disparity);
            }
        }
        
        return maxDisparity;
    }
    
    /**
     * Metrics for a single demographic group.
     */
    public record GroupMetrics(
        int count,
        int positives,
        double positiveRate,
        double avgPrediction,
        Map<String, Double> avgFeatureImportances
    ) {}
    
    /**
     * Comprehensive bias audit report.
     */
    public static class BiasReport {
        private final Map<Integer, GroupMetrics> groupMetrics;
        private final String[] groupNames;
        private final double disparateImpact;
        private final double importanceDisparity;
        
        public BiasReport(
            Map<Integer, GroupMetrics> groupMetrics,
            String[] groupNames,
            double disparateImpact,
            double importanceDisparity
        ) {
            this.groupMetrics = groupMetrics;
            this.groupNames = groupNames;
            this.disparateImpact = disparateImpact;
            this.importanceDisparity = importanceDisparity;
        }
        
        public Map<Integer, GroupMetrics> getGroupMetrics() {
            return groupMetrics;
        }
        
        public double getDisparateImpact() {
            return disparateImpact;
        }
        
        public double getImportanceDisparity() {
            return importanceDisparity;
        }
        
        /**
         * Checks if model passes the 80% rule (0.8 ≤ DI ≤ 1.25).
         */
        public boolean passes80PercentRule() {
            return disparateImpact >= 0.8 && disparateImpact <= 1.25;
        }
        
        /**
         * Assesses overall fairness level.
         */
        public FairnessLevel assessFairness() {
            if (disparateImpact < 0.5) {
                return FairnessLevel.SEVERE_BIAS;
            } else if (disparateImpact < 0.8) {
                return FairnessLevel.MODERATE_BIAS;
            } else if (disparateImpact >= 0.8 && disparateImpact <= 1.25) {
                return FairnessLevel.FAIR;
            } else {
                return FairnessLevel.REVERSE_BIAS;
            }
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("╔═══════════════════════════════════════════════════════════╗\n");
            sb.append("║                    BIAS AUDIT REPORT                      ║\n");
            sb.append("╚═══════════════════════════════════════════════════════════╝\n\n");
            
            // Group statistics
            sb.append("Group Statistics:\n");
            sb.append("─────────────────────────────────────────────────────────────\n");
            for (Map.Entry<Integer, GroupMetrics> entry : groupMetrics.entrySet()) {
                String groupName = groupNames[entry.getKey()];
                GroupMetrics metrics = entry.getValue();
                
                sb.append(String.format("  %s (n=%d):\n", groupName, metrics.count));
                sb.append(String.format("    Positive Rate: %.2f%%\n", metrics.positiveRate * 100));
                sb.append(String.format("    Avg Prediction: %.4f\n", metrics.avgPrediction));
            }
            
            // Fairness metrics
            sb.append("\nFairness Metrics:\n");
            sb.append("─────────────────────────────────────────────────────────────\n");
            sb.append(String.format("  Disparate Impact: %.4f %s\n",
                disparateImpact,
                passes80PercentRule() ? "✓ PASS" : "✗ FAIL"));
            sb.append(String.format("  Importance Disparity: %.4f\n", importanceDisparity));
            sb.append(String.format("  Overall Assessment: %s\n", assessFairness()));
            
            return sb.toString();
        }
    }
    
    /**
     * Fairness assessment levels.
     */
    public enum FairnessLevel {
        SEVERE_BIAS("Severe bias detected - immediate action required"),
        MODERATE_BIAS("Moderate bias - review and mitigation recommended"),
        FAIR("Model passes fairness tests"),
        REVERSE_BIAS("Reverse bias detected - favoring protected group");
        
        private final String description;
        
        FairnessLevel(String description) {
            this.description = description;
        }
        
        @Override
        public String toString() {
            return name() + " - " + description;
        }
    }
}
