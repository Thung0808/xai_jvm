package io.github.Thung0808.xai.security;

import io.github.Thung0808.xai.api.*;
import java.util.*;

/**
 * Defensive XAI — Detect explanation manipulation attacks.
 * 
 * <p>Defends against adversarial explanation attacks where attacker:
 * <ol>
 *   <li>Modifies input slightly</li>
 *   <li>Model prediction stays same (robust model)</li>
 *   <li>But explainer gives WRONG attribution (misleading)</li>
 * </ol>
 * 
 * <p>Example attack:
 * <pre>
 * Original: credit_score=720 (attribution: +0.35)
 * Manipulated: credit_score=721 (attribution: -0.15)  ← FLIPS!
 * 
 * Prediction unchanged, but explanation misleads about credit_score importance.
 * </pre>
 * 
 * <p>Solution: Cross-validate explanations from multiple methods.
 * If disagreement > 30%, flag as potential manipulation.
 * 
 * @since 1.1.0-alpha
 */
public class ExplanationSanitizer {
    
    private final double deviationThreshold;  // Default: 0.30 (30%)
    private final List<Explainer<?>> validators;
    
    public ExplanationSanitizer(double deviationThreshold) {
        this.deviationThreshold = deviationThreshold;
        this.validators = new ArrayList<>();
    }
    
    /**
     * Add a validator explainer (e.g., TreeExplainer for cross-validation).
     */
    public ExplanationSanitizer addValidator(Explainer<?> explainer) {
        validators.add(explainer);
        return this;
    }
    
    /**
     * Check if explanation is suspicious (potential manipulation).
     * 
     * @param primaryExplanation the explanation to validate
     * @param model the model instance (needed to recompute validator explanations)
     * @param instance the input instance
     * @return validation result with verdict and message
     */
    public SanitizationResult validate(Explanation primaryExplanation, PredictiveModel model, double[] instance) {
        List<Explanation> validatorExplanations = new ArrayList<>();
        
        // Get explanations from validators
        for (Explainer validator : validators) {
            try {
                @SuppressWarnings("unchecked")
                Explainer<PredictiveModel> typedValidator = (Explainer<PredictiveModel>) validator;
                Explanation valExpl = typedValidator.explain(model, instance);
                validatorExplanations.add(valExpl);
            } catch (Exception e) {
                // Validator failed — flag as suspicious
                return new SanitizationResult(
                    false,
                    "Validator explainer failed: " + e.getMessage(),
                    true  // IS_SUSPICIOUS
                );
            }
        }
        
        if (validatorExplanations.isEmpty()) {
            return new SanitizationResult(
                true,
                "No validators configured",
                false
            );
        }
        
        // Check consistency
        return checkConsistency(primaryExplanation, validatorExplanations);
    }
    
    private SanitizationResult checkConsistency(
            Explanation primary,
            List<Explanation> validators) {
        
        List<String> inconsistencies = new ArrayList<>();
        
        // 1. Check prediction consistency
        double primaryPred = primary.getPrediction();
        for (Explanation validator : validators) {
            double validatorPred = validator.getPrediction();
            double predDiff = Math.abs(primaryPred - validatorPred) / Math.max(Math.abs(primaryPred), 1e-6);
            
            if (predDiff > deviationThreshold) {
                inconsistencies.add(String.format(
                    "Prediction inconsistency: primary=%.4f, validator=%.4f (diff=%.1f%%)",
                    primaryPred, validatorPred, predDiff * 100
                ));
            }
        }
        
        // 2. Check attribution consistency (ranking)
        Map<String, Integer> primaryRanking = getAttributionRanking(primary);
        
        for (Explanation validator : validators) {
            Map<String, Integer> validatorRanking = getAttributionRanking(validator);
            
            // Compute Spearman correlation of rankings
            double correlation = computeRankingCorrelation(primaryRanking, validatorRanking);
            
            if (correlation < (1.0 - deviationThreshold)) {
                inconsistencies.add(String.format(
                    "Attribution ranking inconsistency: correlation=%.2f (threshold=%.2f)",
                    correlation,
                    1.0 - deviationThreshold
                ));
            }
        }
        
        // 3. Check stability score consistency
        double primaryStability = primary.getStabilityScore();
        for (Explanation validator : validators) {
            double validatorStability = validator.getStabilityScore();
            double stabilityDiff = Math.abs(primaryStability - validatorStability);
            
            if (stabilityDiff > deviationThreshold) {
                inconsistencies.add(String.format(
                    "Stability score inconsistency: primary=%.2f, validator=%.2f (diff=%.1f%%)",
                    primaryStability, validatorStability, stabilityDiff * 100
                ));
            }
        }
        
        // Final verdict
        boolean isSuspicious = !inconsistencies.isEmpty();
        String message = isSuspicious ?
            "Potential explanation manipulation detected!\n" + String.join("\n", inconsistencies) :
            "Explanation passed validation checks";
        
        return new SanitizationResult(
            !isSuspicious,  // isValid
            message,
            isSuspicious
        );
    }
    
    /**
     * Get feature ranking by absolute attribution.
     */
    private Map<String, Integer> getAttributionRanking(Explanation explanation) {
        List<FeatureAttribution> attrs = explanation.getAttributions().stream()
            .sorted((a, b) -> Double.compare(
                Math.abs(b.importance()),
                Math.abs(a.importance())
            ))
            .toList();
        
        Map<String, Integer> ranking = new LinkedHashMap<>();
        for (int i = 0; i < attrs.size(); i++) {
            ranking.put(attrs.get(i).feature(), i);
        }
        return ranking;
    }
    
    /**
     * Compute Spearman rank correlation.
     */
    private double computeRankingCorrelation(
            Map<String, Integer> rankingA,
            Map<String, Integer> rankingB) {
        
        Set<String> commonFeatures = new HashSet<>(rankingA.keySet());
        commonFeatures.retainAll(rankingB.keySet());
        
        if (commonFeatures.isEmpty()) {
            return 0.0;
        }
        
        // Compute sum of squared rank differences
        double sumSquaredDiff = 0.0;
        for (String feature : commonFeatures) {
            int rankA = rankingA.get(feature);
            int rankB = rankingB.get(feature);
            double diff = rankA - rankB;
            sumSquaredDiff += diff * diff;
        }
        
        int n = commonFeatures.size();
        double rho = 1.0 - (6.0 * sumSquaredDiff) / (n * (n * n - 1));
        
        return Math.max(0.0, rho);  // Clamp to [0, 1]
    }
}
