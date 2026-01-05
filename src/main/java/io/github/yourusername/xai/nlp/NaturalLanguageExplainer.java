package io.github.Thung0808.xai.nlp;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.experimental.Incubating;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates human-readable natural language descriptions from model explanations.
 * 
 * <p><strong>Purpose:</strong> Transform technical feature attributions into narratives
 * that non-technical stakeholders (managers, compliance officers, customers) can understand.
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Multiple language support (English, Vietnamese)</li>
 *   <li>Customizable narrative templates</li>
 *   <li>Contextual thresholds for "high", "medium", "low" impact</li>
 *   <li>Regulatory compliance narratives (FCRA, GDPR)</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Generate explanation
 * Explanation explanation = explainer.explain(model, instance);
 * 
 * // Convert to human-readable text
 * NaturalLanguageExplainer nlg = new NaturalLanguageExplainer(Language.ENGLISH);
 * String narrative = nlg.toHumanReadable(explanation);
 * 
 * // Output example:
 * // "This decision was primarily based on Age (contributing 45%), 
 * //  which exceeded the system's risk threshold. Secondary factors 
 * //  include Income (18%) and Credit Score (-8%)."
 * 
 * // Vietnamese version
 * NaturalLanguageExplainer nlgVi = new NaturalLanguageExplainer(Language.VIETNAMESE);
 * String vietnameseText = nlgVi.toHumanReadable(explanation);
 * // "Quyết định này chủ yếu dựa trên Độ tuổi (đóng góp 45%), 
 * //  vượt ngưỡng rủi ro của hệ thống..."
 * }</pre>
 * 
 * <h2>LLM Integration</h2>
 * <p>For advanced use cases, export structured data for LLM processing:
 * <pre>{@code
 * Map<String, Object> structuredData = nlg.toStructuredPrompt(explanation);
 * 
 * // Send to GPT-4/Gemini:
 * String llmPrompt = String.format(
 *     "Generate a detailed regulatory compliance report: %s",
 *     structuredData
 * );
 * }</pre>
 * 
 * @since 1.1.0-alpha
 * @see Explanation
 */
@Incubating(since = "1.1.0-alpha", graduationTarget = "1.2.0")
public class NaturalLanguageExplainer {
    
    /**
     * Supported natural languages.
     */
    public enum Language {
        ENGLISH,
        VIETNAMESE
    }
    
    /**
     * Narrative style for generated text.
     */
    public enum NarrativeStyle {
        /** Technical style for data scientists */
        TECHNICAL,
        
        /** Business style for managers */
        BUSINESS,
        
        /** Regulatory compliance style */
        REGULATORY,
        
        /** Customer-facing style (simple language) */
        CUSTOMER
    }
    
    private final Language language;
    private final NarrativeStyle style;
    private final double highImpactThreshold;
    private final double mediumImpactThreshold;
    
    /**
     * Creates NLG explainer with default settings (English, Business style).
     */
    public NaturalLanguageExplainer() {
        this(Language.ENGLISH, NarrativeStyle.BUSINESS);
    }
    
    /**
     * Creates NLG explainer with specified language.
     */
    public NaturalLanguageExplainer(Language language) {
        this(language, NarrativeStyle.BUSINESS);
    }
    
    /**
     * Creates NLG explainer with full customization.
     * 
     * @param language target language
     * @param style narrative style
     */
    public NaturalLanguageExplainer(Language language, NarrativeStyle style) {
        this(language, style, 0.20, 0.10);
    }
    
    /**
     * Creates NLG explainer with custom thresholds.
     * 
     * @param language target language
     * @param style narrative style
     * @param highImpactThreshold absolute attribution > this = "high impact" (default 0.20)
     * @param mediumImpactThreshold absolute attribution > this = "medium impact" (default 0.10)
     */
    public NaturalLanguageExplainer(Language language, NarrativeStyle style,
                                    double highImpactThreshold, double mediumImpactThreshold) {
        this.language = language;
        this.style = style;
        this.highImpactThreshold = highImpactThreshold;
        this.mediumImpactThreshold = mediumImpactThreshold;
    }
    
    /**
     * Converts explanation to human-readable narrative.
     * 
     * @param explanation the explanation to convert
     * @return natural language description
     */
    public String toHumanReadable(Explanation explanation) {
        StringBuilder narrative = new StringBuilder();
        
        // Get sorted attributions by absolute importance
        List<FeatureAttribution> attributions = explanation.getTopAttributions();
        
        if (attributions.isEmpty()) {
            return getNoFeaturesMessage();
        }
        
        // Introduction
        narrative.append(getIntroduction(explanation));
        narrative.append("\n\n");
        
        // Primary factors
        narrative.append(getPrimaryFactors(attributions));
        narrative.append("\n\n");
        
        // Secondary factors (if any)
        List<FeatureAttribution> secondary = getSecondaryFactors(attributions);
        if (!secondary.isEmpty()) {
            narrative.append(getSecondaryFactorsText(secondary));
            narrative.append("\n\n");
        }
        
        // Confidence/uncertainty
        narrative.append(getConfidenceStatement(explanation));
        
        return narrative.toString();
    }
    
    /**
     * Converts explanation to structured data for LLM prompting.
     * 
     * @param explanation the explanation to convert
     * @return structured data suitable for GPT-4/Gemini API
     */
    public Map<String, Object> toStructuredPrompt(Explanation explanation) {
        Map<String, Object> data = new LinkedHashMap<>();
        
        data.put("prediction", explanation.getPrediction());
        data.put("baseline", explanation.getBaseline());
        data.put("stability", explanation.getStabilityScore());
        
        List<Map<String, Object>> features = new ArrayList<>();
        for (FeatureAttribution attr : explanation.getTopAttributions()) {
            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("name", attr.feature());
            feature.put("importance", attr.importance());
            feature.put("impact_level", getImpactLevel(attr.importance()));
            feature.put("direction", attr.importance() > 0 ? "positive" : "negative");
            features.add(feature);
        }
        data.put("features", features);
        
        // Metadata
        var metadata = explanation.getMetadata();
        data.put("explainer", metadata.explainerName());
        data.put("timestamp", metadata.timestamp());
        
        return data;
    }
    
    /**
     * Generates introduction sentence based on language and style.
     */
    private String getIntroduction(Explanation explanation) {
        double prediction = explanation.getPrediction();
        
        switch (language) {
            case VIETNAMESE:
                return switch (style) {
                    case TECHNICAL -> String.format(
                        "Mô hình dự đoán giá trị %.2f với độ tin cậy %.1f%%.",
                        prediction, explanation.getStabilityScore() * 100
                    );
                    case BUSINESS -> String.format(
                        "Quyết định này có kết quả %.2f, chủ yếu dựa trên các yếu tố sau:",
                        prediction
                    );
                    case REGULATORY -> String.format(
                        "Theo quy định, hệ thống đưa ra kết quả %.2f với các lý do cụ thể:",
                        prediction
                    );
                    case CUSTOMER -> String.format(
                        "Kết quả của bạn là %.2f. Đây là lý do:",
                        prediction
                    );
                };
                
            case ENGLISH:
            default:
                return switch (style) {
                    case TECHNICAL -> String.format(
                        "Model predicts %.2f with %.1f%% confidence.",
                        prediction, explanation.getStabilityScore() * 100
                    );
                    case BUSINESS -> String.format(
                        "This decision yielded a score of %.2f, primarily driven by the following factors:",
                        prediction
                    );
                    case REGULATORY -> String.format(
                        "Per regulatory requirements, the system produced a score of %.2f based on:",
                        prediction
                    );
                    case CUSTOMER -> String.format(
                        "Your score is %.2f. Here's why:",
                        prediction
                    );
                };
        }
    }
    
    /**
     * Describes primary (high impact) factors.
     */
    private String getPrimaryFactors(List<FeatureAttribution> attributions) {
        List<FeatureAttribution> primary = attributions.stream()
            .filter(a -> Math.abs(a.importance()) >= highImpactThreshold)
            .limit(3)
            .toList();
        
        if (primary.isEmpty()) {
            // No high impact factors, take top 1
            primary = List.of(attributions.get(0));
        }
        
        StringBuilder text = new StringBuilder();
        
        switch (language) {
            case VIETNAMESE:
                text.append("Yếu tố chính: ");
                for (int i = 0; i < primary.size(); i++) {
                    FeatureAttribution attr = primary.get(i);
                    if (i > 0) text.append(", ");
                    
                    text.append(String.format(
                        "**%s** (đóng góp %.1f%%)",
                        formatFeatureName(attr.feature()),
                        Math.abs(attr.importance()) * 100
                    ));
                    
                    if (attr.importance() > 0) {
                        text.append(" - tăng điểm");
                    } else {
                        text.append(" - giảm điểm");
                    }
                }
                break;
                
            case ENGLISH:
            default:
                text.append("Primary factors: ");
                for (int i = 0; i < primary.size(); i++) {
                    FeatureAttribution attr = primary.get(i);
                    if (i > 0) text.append(", ");
                    
                    text.append(String.format(
                        "**%s** (contributing %.1f%%)",
                        formatFeatureName(attr.feature()),
                        Math.abs(attr.importance()) * 100
                    ));
                    
                    if (attr.importance() > 0) {
                        text.append(" - increased score");
                    } else {
                        text.append(" - decreased score");
                    }
                }
                break;
        }
        
        return text.toString();
    }
    
    /**
     * Gets secondary (medium impact) factors.
     */
    private List<FeatureAttribution> getSecondaryFactors(List<FeatureAttribution> attributions) {
        return attributions.stream()
            .filter(a -> Math.abs(a.importance()) >= mediumImpactThreshold &&
                        Math.abs(a.importance()) < highImpactThreshold)
            .limit(3)
            .toList();
    }
    
    /**
     * Describes secondary factors.
     */
    private String getSecondaryFactorsText(List<FeatureAttribution> secondary) {
        StringBuilder text = new StringBuilder();
        
        switch (language) {
            case VIETNAMESE:
                text.append("Yếu tố phụ: ");
                break;
            case ENGLISH:
            default:
                text.append("Secondary factors: ");
                break;
        }
        
        for (int i = 0; i < secondary.size(); i++) {
            FeatureAttribution attr = secondary.get(i);
            if (i > 0) text.append(", ");
            
            text.append(String.format(
                "%s (%.1f%%)",
                formatFeatureName(attr.feature()),
                Math.abs(attr.importance()) * 100
            ));
        }
        
        return text.toString();
    }
    
    /**
     * Generates confidence/uncertainty statement.
     */
    private String getConfidenceStatement(Explanation explanation) {
        double stability = explanation.getStabilityScore();
        
        switch (language) {
            case VIETNAMESE:
                if (stability > 0.9) {
                    return "Độ tin cậy: **Cao** (giải thích ổn định).";
                } else if (stability > 0.7) {
                    return "Độ tin cậy: **Trung bình** (có một số biến động).";
                } else {
                    return "Độ tin cậy: **Thấp** (kết quả có thể không ổn định).";
                }
                
            case ENGLISH:
            default:
                if (stability > 0.9) {
                    return "Confidence: **High** (explanation is stable).";
                } else if (stability > 0.7) {
                    return "Confidence: **Medium** (some variability exists).";
                } else {
                    return "Confidence: **Low** (results may be unstable).";
                }
        }
    }
    
    /**
     * Gets impact level label for an attribution value.
     */
    private String getImpactLevel(double importance) {
        double abs = Math.abs(importance);
        if (abs >= highImpactThreshold) return "high";
        if (abs >= mediumImpactThreshold) return "medium";
        return "low";
    }
    
    /**
     * Formats feature names for display (removes underscores, capitalizes).
     */
    private String formatFeatureName(String featureName) {
        // Convert snake_case or camelCase to Title Case
        return Arrays.stream(featureName.split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }
    
    /**
     * Gets message when no features available.
     */
    private String getNoFeaturesMessage() {
        return switch (language) {
            case VIETNAMESE -> "Không có thông tin giải thích.";
            case ENGLISH -> "No explanation available.";
        };
    }
}
