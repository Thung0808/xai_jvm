/**
 * Spring Boot auto-configuration for XAI Core.
 * 
 * <p><strong>Quick Start (30 seconds):</strong></p>
 * 
 * <h3>Step 1: Add Dependency</h3>
 * <pre>{@code
 * <dependency>
 *     <groupId>io.github.Thung0808</groupId>
 *     <artifactId>xai-core-spring-boot-starter</artifactId>
 *     <version>1.1.0-alpha</version>
 * </dependency>
 * }</pre>
 * 
 * <h3>Step 2: Configure in application.yml</h3>
 * <pre>{@code
 * xai:
 *   enabled: true
 *   default-explainer: permutation
 *   metrics:
 *     enabled: true
 * }</pre>
 * 
 * <h3>Step 3: Use @Explainable Annotation</h3>
 * <pre>{@code
 * @Service
 * public class CreditScoringService {
 *     @Explainable(
 *         model = "creditModel",
 *         importance = true,
 *         logOutput = true,
 *         sanitize = true
 *     )
 *     public double scoreLoan(LoanApplication app) {
 *         return creditModel.predict(featureVector);
 *     }
 * }
 * }</pre>
 * 
 * <h3>Step 4: Query Explanations via REST</h3>
 * <pre>{@code
 * # Generate explanation
 * curl -X POST http://localhost:8080/xai/explain \
 *   -H "Content-Type: application/json" \
 *   -d '{"features": [1.0, 2.0, 3.0]}'
 * 
 * # View metrics
 * curl http://localhost:8080/xai/metrics
 * 
 * # Get Prometheus metrics
 * curl http://localhost:8080/actuator/prometheus
 * }</pre>
 * 
 * <h3>Features</h3>
 * <ul>
 *   <li><strong>Auto-Configuration:</strong> Just add the dependency, everything is configured automatically</li>
 *   <li><strong>@Explainable Annotation:</strong> Mark methods for automatic explanation generation</li>
 *   <li><strong>REST API:</strong> Query explanations via HTTP endpoints</li>
 *   <li><strong>Micrometer Integration:</strong> Export metrics to Prometheus/Grafana</li>
 *   <li><strong>Manipulation Detection:</strong> Automatic sanitization checks</li>
 *   <li><strong>Latency Tracking:</strong> Monitor explanation generation performance</li>
 * </ul>
 * 
 * <h3>Metrics Exposed</h3>
 * <ul>
 *   <li>{@code xai.explanation.latency} — Time to generate explanation (μs)</li>
 *   <li>{@code xai.trust.score} — Current trust score (0-1)</li>
 *   <li>{@code xai.robustness.score} — Current robustness score (0-1)</li>
 *   <li>{@code xai.drift.magnitude} — Drift detection magnitude (0-1)</li>
 *   <li>{@code xai.explanations.total} — Total explanations generated</li>
 *   <li>{@code xai.manipulations.detected} — Detected suspicious explanations</li>
 * </ul>
 * 
 * <h3>Grafana Dashboard</h3>
 * <p>Use the provided Grafana JSON dashboard to visualize model health in real-time.</p>
 * 
 * @since 1.1.0-alpha
 */
package io.github.Thung0808.xai.spring.boot.autoconfigure;
