package io.github.Thung0808.xai.spring.boot.autoconfigure;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.Explainer;
import io.github.Thung0808.xai.api.PredictiveModel;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * REST API for XAI explanation queries.
 * 
 * <p>Endpoints:
 * <ul>
 *   <li>GET /xai/explain — Generate explanation for input features</li>
 *   <li>GET /xai/metrics — Get current metric values</li>
 *   <li>GET /actuator/prometheus — Prometheus metrics endpoint</li>
 * </ul>
 * 
 * <p>Example requests:
 * <pre>{@code
 * # Generate explanation
 * curl -X POST http://localhost:8080/xai/explain \
 *   -H "Content-Type: application/json" \
 *   -d '{"features": [1.0, 2.0, 3.0], "model": "creditModel"}'
 * 
 * # Get metrics
 * curl http://localhost:8080/xai/metrics
 * 
 * # Get Prometheus metrics
 * curl http://localhost:8080/actuator/prometheus
 * }</pre>
 * 
 * @since 1.1.0-alpha
 */
@RestController
@RequestMapping("/xai")
public class ExplanationController {
    
    private static final Logger log = LoggerFactory.getLogger(ExplanationController.class);
    
    private final MeterRegistry meterRegistry;
    private final ExplainerMetrics metrics;
    
    @Autowired
    private Explainer<PredictiveModel> defaultExplainer;
    
    public ExplanationController(
            MeterRegistry meterRegistry,
            ExplainerMetrics metrics) {
        this.meterRegistry = meterRegistry;
        this.metrics = metrics;
    }
    
    /**
     * Generate explanation for given input features.
     * 
     * Request body:
     * {@code
     * {
     *   "features": [1.0, 2.0, 3.0],
     *   "model": "creditModel",
     *   "include_metadata": true
     * }
     * }
     */
    @PostMapping("/explain")
    public ResponseEntity<?> generateExplanation(@RequestBody ExplanationRequest request) {
        try {
            long startTime = System.nanoTime();
            
            // Generate explanation (would use specific model if provided)
            Explanation explanation = defaultExplainer.explain(null, request.getFeatures());
            
            long latencyMicros = (System.nanoTime() - startTime) / 1000;
            metrics.recordExplanation(explanation, latencyMicros);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("prediction", explanation.getPrediction());
            response.put("baseline", explanation.getBaseline());
            response.put("stability_score", explanation.getStabilityScore());
            response.put("latency_micros", latencyMicros);
            response.put("attributions", explanation.getTopAttributions(10));
            response.put("num_features", explanation.getAttributions().size());
            
            if (request.isIncludeMetadata()) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("explainer_name", explanation.getMetadata().explainerName());
                metadata.put("timestamp", explanation.getMetadata().timestamp());
                response.put("metadata", metadata);
            }
            
            log.info("Generated explanation: prediction={}, stability={:.3f}, latency={}μs",
                explanation.getPrediction(),
                explanation.getStabilityScore(),
                latencyMicros);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating explanation: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get current metric values.
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        Map<String, Object> metricsMap = new HashMap<>();
        metricsMap.put("trust_score", metrics.getTrustScore());
        metricsMap.put("robustness_score", metrics.getRobustnessScore());
        metricsMap.put("drift_magnitude", metrics.getDriftMagnitude());
        metricsMap.put("meter_registry_size", meterRegistry.find("xai.explanation.latency").timer() != null ? 1 : 0);
        
        return ResponseEntity.ok(metricsMap);
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("explainer", "available");
        status.put("metrics", "enabled");
        status.put("message", "XAI Core is running");
        return ResponseEntity.ok(status);
    }
    
    /**
     * Request body for explanation endpoint.
     */
    public static class ExplanationRequest {
        private double[] features;
        private String model = "default";
        private boolean includeMetadata = false;
        
        // Getters and setters
        public double[] getFeatures() {
            return features;
        }
        
        public void setFeatures(double[] features) {
            this.features = features;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public boolean isIncludeMetadata() {
            return includeMetadata;
        }
        
        public void setIncludeMetadata(boolean includeMetadata) {
            this.includeMetadata = includeMetadata;
        }
    }
}
