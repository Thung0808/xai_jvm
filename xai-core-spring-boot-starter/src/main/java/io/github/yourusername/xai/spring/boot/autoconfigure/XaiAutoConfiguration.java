package io.github.Thung0808.xai.spring.boot.autoconfigure;

import io.github.Thung0808.xai.api.Explainer;
import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.explainer.PermutationExplainer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Boot auto-configuration for XAI Core.
 * 
 * <p>This configuration automatically:
 * <ul>
 *   <li>Creates explainer beans for models in the Spring context</li>
 *   <li>Configures metrics collection via Micrometer</li>
 *   <li>Registers REST endpoints for explanation queries</li>
 *   <li>Enables @Explainable annotation processing</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>{@code
 * // application.yml
 * xai:
 *   enabled: true
 *   default-explainer: permutation
 *   metrics:
 *     enabled: true
 *     percentiles: [0.5, 0.95, 0.99]
 * 
 * // Your Spring Bean
 * @Service
 * public class CreditScoringService {
 *   @Explainable(model = "creditModel", importance = true)
 *   public double scoreLoan(LoanApplication app) { ... }
 * }
 * }</pre>
 * 
 * @since 1.1.0-alpha
 */
@AutoConfiguration
@EnableConfigurationProperties(XaiProperties.class)
public class XaiAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(XaiAutoConfiguration.class);
    
    private final XaiProperties properties;
    
    public XaiAutoConfiguration(XaiProperties properties) {
        this.properties = properties;
    }
    
    /**
     * Auto-create Permutation Explainer bean if no model-specific explainer exists.
     */
    @Bean
    @Conditional(ExplainerMissingCondition.class)
    public Explainer<PredictiveModel> defaultExplainer() {
        log.info("Configuring default XAI explainer: {}", properties.getDefaultExplainer());
        return new PermutationExplainer()
            .withNumSamples(properties.getDefaultExplainer().equalsIgnoreCase("permutation") ? 100 : 50);
    }
    
    /**
     * Configure metrics collection for explanations.
     */
    @Bean
    public ExplainerMetrics explainerMetrics(MeterRegistry meterRegistry) {
        ExplainerMetrics metrics = new ExplainerMetrics(meterRegistry);
        log.info("XAI metrics enabled. Exposing: trust_score, robustness_score, drift_magnitude");
        return metrics;
    }
    
    /**
     * Register the @Explainable annotation processor.
     */
    @Bean
    public ExplainableMethodInterceptor explainableInterceptor(ExplainerMetrics metrics) {
        return new ExplainableMethodInterceptor(metrics);
    }
    
    /**
     * Expose explanation controller for REST queries.
     */
    @Bean
    public ExplanationController explanationController(
            MeterRegistry meterRegistry,
            ExplainerMetrics metrics) {
        return new ExplanationController(meterRegistry, metrics);
    }
    
    @Override
    public String toString() {
        return String.format(
            "XaiAutoConfiguration{enabled=%s, defaultExplainer=%s, metricsEnabled=%s}",
            properties.isEnabled(),
            properties.getDefaultExplainer(),
            properties.isMetricsEnabled()
        );
    }
}
