package io.github.thung0808.xai.monitoring;

import io.github.thung0808.xai.api.Explanation;
import io.github.thung0808.xai.api.FeatureAttribution;
import io.github.thung0808.xai.api.Stable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Detects drift in explanations over time.
 * 
 * <p><b>Purpose:</b> Monitor explanation stability across model versions,
 * data distributions, or time windows. Drift detection prevents silent
 * degradation and catches model behavior changes.</p>
 * 
 * <p><b>Metrics Tracked:</b></p>
 * <ul>
 *   <li><b>Feature Importance Shift:</b> Changes in attribution magnitudes</li>
 *   <li><b>Rank Correlation:</b> Changes in feature importance ordering</li>
 *   <li><b>Jensen-Shannon Divergence:</b> Distribution distance</li>
 *   <li><b>Entropy Change:</b> Information content variation</li>
 * </ul>
 * 
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li>Model monitoring in production</li>
 *   <li>A/B testing of model versions</li>
 *   <li>Detecting data distribution shifts</li>
 *   <li>Regulatory compliance audits</li>
 * </ul>
 * 
 * <p><b>Example:</b></p>
 * <pre>{@code
 * ExplanationDriftDetector detector = new ExplanationDriftDetector();
 * 
 * // Establish baseline
 * detector.setBaseline(baselineExplanations);
 * 
 * // Monitor new explanations
 * DriftReport report = detector.detect(currentExplanations);
 * 
 * if (report.hasDrift()) {
 *     logger.warn("Explanation drift detected: {}", report);
 *     // Alert, rollback, or investigate
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
@Stable(since = "0.3.0")
public class ExplanationDriftDetector {
    
    private List<Explanation> baselineExplanations;
    private Map<String, Double> baselineImportances;
    private double baselineEntropy;
    
    /**
     * Sets baseline explanations for comparison.
     * 
     * @param baseline reference explanations (typically from initial deployment)
     */
    public void setBaseline(List<Explanation> baseline) {
        if (baseline == null || baseline.isEmpty()) {
            throw new IllegalArgumentException("Baseline cannot be empty");
        }
        
        this.baselineExplanations = new ArrayList<>(baseline);
        this.baselineImportances = aggregateImportances(baseline);
        this.baselineEntropy = computeEntropy(baselineImportances);
    }
    
    /**
     * Detects drift between baseline and current explanations.
     * 
     * @param current new explanations to compare against baseline
     * @return drift report with metrics and recommendations
     */
    public DriftReport detect(List<Explanation> current) {
        if (baselineExplanations == null) {
            throw new IllegalStateException("Baseline not set. Call setBaseline() first.");
        }
        
        if (current == null || current.isEmpty()) {
            throw new IllegalArgumentException("Current explanations cannot be empty");
        }
        
        Map<String, Double> currentImportances = aggregateImportances(current);
        double currentEntropy = computeEntropy(currentImportances);
        
        // Compute drift metrics
        double jsDivergence = computeJSDivergence(baselineImportances, currentImportances);
        double rankCorrelation = computeRankCorrelation(baselineImportances, currentImportances);
        double entropyChange = Math.abs(currentEntropy - baselineEntropy) / baselineEntropy;
        double maxShift = computeMaxFeatureShift(baselineImportances, currentImportances);
        
        // Overall drift score (weighted combination)
        double driftScore = 0.4 * jsDivergence
                          + 0.3 * (1.0 - rankCorrelation)
                          + 0.2 * entropyChange
                          + 0.1 * maxShift;
        
        return new DriftReport(
            jsDivergence,
            rankCorrelation,
            entropyChange,
            maxShift,
            driftScore,
            determineDriftLevel(driftScore),
            generateRecommendation(driftScore)
        );
    }
    
    /**
     * Aggregates feature importances across explanations.
     */
    private Map<String, Double> aggregateImportances(List<Explanation> explanations) {
        Map<String, Double> aggregated = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        
        for (Explanation exp : explanations) {
            for (FeatureAttribution attr : exp.getAttributions()) {
                String feature = attr.feature();
                double importance = Math.abs(attr.importance());
                
                aggregated.merge(feature, importance, Double::sum);
                counts.merge(feature, 1, Integer::sum);
            }
        }
        
        // Normalize to averages
        aggregated.replaceAll((k, v) -> v / counts.get(k));
        
        // Normalize to distribution (sum to 1.0)
        double total = aggregated.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total > 0) {
            aggregated.replaceAll((k, v) -> v / total);
        }
        
        return aggregated;
    }
    
    /**
     * Computes Jensen-Shannon divergence between two distributions.
     */
    private double computeJSDivergence(Map<String, Double> p, Map<String, Double> q) {
        Set<String> allFeatures = new HashSet<>();
        allFeatures.addAll(p.keySet());
        allFeatures.addAll(q.keySet());
        
        // Compute average distribution M = (P + Q) / 2
        Map<String, Double> m = new HashMap<>();
        for (String feature : allFeatures) {
            double pVal = p.getOrDefault(feature, 0.0);
            double qVal = q.getOrDefault(feature, 0.0);
            m.put(feature, (pVal + qVal) / 2.0);
        }
        
        // JS = (KL(P||M) + KL(Q||M)) / 2
        double klPM = computeKLDivergence(p, m, allFeatures);
        double klQM = computeKLDivergence(q, m, allFeatures);
        
        return (klPM + klQM) / 2.0;
    }
    
    /**
     * Computes KL divergence.
     */
    private double computeKLDivergence(
        Map<String, Double> p,
        Map<String, Double> q,
        Set<String> features
    ) {
        double kl = 0.0;
        for (String feature : features) {
            double pVal = p.getOrDefault(feature, 1e-10);
            double qVal = q.getOrDefault(feature, 1e-10);
            
            if (pVal > 0) {
                kl += pVal * Math.log(pVal / qVal);
            }
        }
        return kl;
    }
    
    /**
     * Computes Spearman rank correlation.
     */
    private double computeRankCorrelation(Map<String, Double> p, Map<String, Double> q) {
        Set<String> commonFeatures = new HashSet<>(p.keySet());
        commonFeatures.retainAll(q.keySet());
        
        if (commonFeatures.size() < 2) {
            return 1.0; // Perfect correlation by default
        }
        
        List<String> sortedP = commonFeatures.stream()
            .sorted(Comparator.comparingDouble(p::get).reversed())
            .collect(Collectors.toList());
        
        List<String> sortedQ = commonFeatures.stream()
            .sorted(Comparator.comparingDouble(q::get).reversed())
            .collect(Collectors.toList());
        
        // Compute rank differences
        double sumD2 = 0.0;
        for (String feature : commonFeatures) {
            int rankP = sortedP.indexOf(feature);
            int rankQ = sortedQ.indexOf(feature);
            double d = rankP - rankQ;
            sumD2 += d * d;
        }
        
        int n = commonFeatures.size();
        return 1.0 - (6.0 * sumD2) / (n * (n * n - 1));
    }
    
    /**
     * Computes entropy of importance distribution.
     */
    private double computeEntropy(Map<String, Double> importances) {
        double entropy = 0.0;
        for (double p : importances.values()) {
            if (p > 0) {
                entropy -= p * Math.log(p);
            }
        }
        return entropy;
    }
    
    /**
     * Finds maximum importance shift for any single feature.
     */
    private double computeMaxFeatureShift(Map<String, Double> baseline, Map<String, Double> current) {
        double maxShift = 0.0;
        
        Set<String> allFeatures = new HashSet<>();
        allFeatures.addAll(baseline.keySet());
        allFeatures.addAll(current.keySet());
        
        for (String feature : allFeatures) {
            double baseVal = baseline.getOrDefault(feature, 0.0);
            double currVal = current.getOrDefault(feature, 0.0);
            double shift = Math.abs(currVal - baseVal);
            maxShift = Math.max(maxShift, shift);
        }
        
        return maxShift;
    }
    
    private DriftLevel determineDriftLevel(double score) {
        if (score < 0.1) return DriftLevel.NONE;
        if (score < 0.2) return DriftLevel.LOW;
        if (score < 0.4) return DriftLevel.MODERATE;
        return DriftLevel.HIGH;
    }
    
    private String generateRecommendation(double score) {
        return switch (determineDriftLevel(score)) {
            case NONE -> "No significant drift detected. Explanations remain stable.";
            case LOW -> "Minor drift detected. Continue monitoring but no immediate action required.";
            case MODERATE -> "Moderate drift detected. Investigate potential causes and consider retraining.";
            case HIGH -> "High drift detected. Urgent investigation required. Model behavior may have changed significantly.";
        };
    }
    
    /**
     * Report of detected drift with metrics and recommendations.
     */
    public static class DriftReport {
        private final double jsDivergence;
        private final double rankCorrelation;
        private final double entropyChange;
        private final double maxFeatureShift;
        private final double overallDriftScore;
        private final DriftLevel level;
        private final String recommendation;
        
        public DriftReport(
            double jsDivergence,
            double rankCorrelation,
            double entropyChange,
            double maxFeatureShift,
            double overallDriftScore,
            DriftLevel level,
            String recommendation
        ) {
            this.jsDivergence = jsDivergence;
            this.rankCorrelation = rankCorrelation;
            this.entropyChange = entropyChange;
            this.maxFeatureShift = maxFeatureShift;
            this.overallDriftScore = overallDriftScore;
            this.level = level;
            this.recommendation = recommendation;
        }
        
        public boolean hasDrift() {
            return level != DriftLevel.NONE;
        }
        
        public double getJsDivergence() { return jsDivergence; }
        public double getRankCorrelation() { return rankCorrelation; }
        public double getEntropyChange() { return entropyChange; }
        public double getMaxFeatureShift() { return maxFeatureShift; }
        public double getOverallDriftScore() { return overallDriftScore; }
        public DriftLevel getLevel() { return level; }
        public String getRecommendation() { return recommendation; }
        
        @Override
        public String toString() {
            return String.format(
                "DriftReport{score=%.3f, level=%s, JS=%.3f, rankCorr=%.3f, entropy=%.3f, maxShift=%.3f}",
                overallDriftScore, level, jsDivergence, rankCorrelation, entropyChange, maxFeatureShift
            );
        }
    }
    
    /**
     * Drift severity level.
     */
    public enum DriftLevel {
        NONE,
        LOW,
        MODERATE,
        HIGH
    }
}


