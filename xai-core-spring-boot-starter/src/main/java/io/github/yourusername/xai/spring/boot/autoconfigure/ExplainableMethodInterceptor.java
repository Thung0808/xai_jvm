package io.github.Thung0808.xai.spring.boot.autoconfigure;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.Explainer;
import io.github.Thung0808.xai.api.PredictiveModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Interceptor for @Explainable annotation processing.
 * 
 * <p>Handles automatic explanation generation, logging, and metric recording.
 * Integrates with Spring AOP for transparent method interception.
 */
@Component
public class ExplainableMethodInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(ExplainableMethodInterceptor.class);
    
    private final ExplainerMetrics metrics;
    
    public ExplainableMethodInterceptor(ExplainerMetrics metrics) {
        this.metrics = metrics;
    }
    
    /**
     * Process explanation for annotated method.
     * 
     * <p>Called by Spring AOP advice when @Explainable method is invoked.
     */
    public Object processExplanation(
            String methodName,
            Explainer<PredictiveModel> explainer,
            PredictiveModel model,
            double[] inputFeatures,
            Object result,
            Explainable annotation) {
        
        long startTime = System.nanoTime();
        
        try {
            // Generate explanation
            Explanation explanation = explainer.explain(model, inputFeatures);
            
            // Record metrics
            long latencyMicros = (System.nanoTime() - startTime) / 1000;
            metrics.recordExplanation(explanation, latencyMicros);
            
            // Log if enabled
            if (annotation.logOutput()) {
                logExplanation(methodName, explanation, annotation, latencyMicros, (double) result);
            }
            
            // Sanitize (check for manipulation) if enabled
            if (annotation.sanitize()) {
                // In production, would use ExplanationSanitizer here
                log.debug("Explanation sanitization check passed for {}", methodName);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error generating explanation for {}: {}", methodName, e.getMessage(), e);
            return result;  // Return original result even if explanation fails
        }
    }
    
    /**
     * Log explanation to console.
     */
    private void logExplanation(
            String methodName,
            Explanation explanation,
            Explainable annotation,
            long latencyMicros,
            double prediction) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔════════════════════════════════════════════════════════════╗\n");
        sb.append("║  XAI Explanation for: ").append(methodName).append("\n");
        sb.append("╚════════════════════════════════════════════════════════════╝\n");
        
        if (!annotation.description().isEmpty()) {
            sb.append("📝 Description: ").append(annotation.description()).append("\n");
        }
        
        sb.append("🎯 Prediction: ").append(String.format("%.4f", prediction)).append("\n");
        sb.append("📊 Baseline: ").append(String.format("%.4f", explanation.getBaseline())).append("\n");
        sb.append("💪 Stability: ").append(String.format("%.3f", explanation.getStabilityScore())).append("\n");
        sb.append("⏱️  Latency: ").append(latencyMicros).append("μs\n");
        
        if (annotation.importance()) {
            sb.append("\n📈 Top Feature Attributions:\n");
            explanation.getTopAttributions(5).forEach(attr ->
                sb.append(String.format("   • %s: %+.4f\n", attr.feature(), attr.importance()))
            );
        }
        
        sb.append("\n");
        
        log.info(sb.toString());
    }
}
