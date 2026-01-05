package io.github.Thung0808.xai.spring.boot.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Spring Boot configuration properties for XAI Core.
 * 
 * <p>Usage in application.yml:
 * <pre>{@code
 * xai:
 *   enabled: true
 *   default-explainer: permutation  # permutation, tree, linear, causal
 *   metrics:
 *     enabled: true
 *     percentiles: [0.5, 0.95, 0.99]
 *   annotation:
 *     enabled: true
 *     log-to-console: true
 *     log-to-database: false
 * }</pre>
 */
@Component
@ConfigurationProperties(prefix = "xai")
public class XaiProperties {
    
    /**
     * Enable/disable XAI auto-configuration.
     */
    private boolean enabled = true;
    
    /**
     * Default explainer type: permutation, tree, linear, causal.
     */
    private String defaultExplainer = "permutation";
    
    /**
     * Default number of samples for explainer.
     */
    private int defaultSamples = 100;
    
    /**
     * Metrics configuration.
     */
    private MetricsConfig metrics = new MetricsConfig();
    
    /**
     * @Explainable annotation configuration.
     */
    private AnnotationConfig annotation = new AnnotationConfig();
    
    // Getters & Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getDefaultExplainer() {
        return defaultExplainer;
    }
    
    public void setDefaultExplainer(String defaultExplainer) {
        this.defaultExplainer = defaultExplainer;
    }
    
    public int getDefaultSamples() {
        return defaultSamples;
    }
    
    public void setDefaultSamples(int defaultSamples) {
        this.defaultSamples = defaultSamples;
    }
    
    public MetricsConfig getMetrics() {
        return metrics;
    }
    
    public void setMetrics(MetricsConfig metrics) {
        this.metrics = metrics;
    }
    
    public AnnotationConfig getAnnotation() {
        return annotation;
    }
    
    public void setAnnotation(AnnotationConfig annotation) {
        this.annotation = annotation;
    }
    
    /**
     * Metrics configuration nested properties.
     */
    public static class MetricsConfig {
        private boolean enabled = true;
        private double[] percentiles = {0.5, 0.95, 0.99};
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public double[] getPercentiles() {
            return percentiles;
        }
        
        public void setPercentiles(double[] percentiles) {
            this.percentiles = percentiles;
        }
    }
    
    /**
     * @Explainable annotation configuration.
     */
    public static class AnnotationConfig {
        private boolean enabled = true;
        private boolean logToConsole = true;
        private boolean logToDatabase = false;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public boolean isLogToConsole() {
            return logToConsole;
        }
        
        public void setLogToConsole(boolean logToConsole) {
            this.logToConsole = logToConsole;
        }
        
        public boolean isLogToDatabase() {
            return logToDatabase;
        }
        
        public void setLogToDatabase(boolean logToDatabase) {
            this.logToDatabase = logToDatabase;
        }
    }
}
