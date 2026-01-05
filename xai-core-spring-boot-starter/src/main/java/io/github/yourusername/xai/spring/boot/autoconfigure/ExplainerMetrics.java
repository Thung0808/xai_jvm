package io.github.Thung0808.xai.spring.boot.autoconfigure;

import io.github.Thung0808.xai.api.Explanation;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Micrometer integration for XAI metrics.
 * 
 * <p>Collects and exposes:
 * <ul>
 *   <li>xai.explanation.latency — Time to generate explanation (μs)</li>
 *   <li>xai.trust.score — Overall trust score of explanation (0-1)</li>
 *   <li>xai.robustness.score — Model robustness metric (0-1)</li>
 *   <li>xai.drift.magnitude — Drift detection magnitude (0-1)</li>
 *   <li>xai.explanations.total — Total explanations generated</li>
 *   <li>xai.manipulations.detected — Suspicious explanations flagged</li>
 * </ul>
 * 
 * <p>Automatically exported to Prometheus via Spring Boot Actuator.
 * 
 * <p>Usage:
 * <pre>{@code
 * metrics.recordExplanation(explanation, 123);  // latency in μs
 * // In Prometheus: 
 * // xai_explanation_latency_seconds_max{...} = 0.000123
 * }</pre>
 * 
 * @since 1.1.0-alpha
 */
public class ExplainerMetrics {
    
    private static final Logger log = LoggerFactory.getLogger(ExplainerMetrics.class);
    
    private final MeterRegistry meterRegistry;
    private final Timer explanationLatency;
    private final Counter explanationsTotal;
    private final Counter manipulationsDetected;
    private final AtomicReference<Double> currentTrustScore;
    private final AtomicReference<Double> currentRobustnessScore;
    private final AtomicReference<Double> currentDriftMagnitude;
    
    public ExplainerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Timer for explanation generation latency
        this.explanationLatency = Timer.builder("xai.explanation.latency")
            .description("Time to generate explanation (microseconds)")
            .publishPercentiles(0.5, 0.95, 0.99)
            .baseTimeUnit(TimeUnit.MICROSECONDS)
            .register(meterRegistry);
        
        // Counter for total explanations
        this.explanationsTotal = Counter.builder("xai.explanations.total")
            .description("Total number of explanations generated")
            .register(meterRegistry);
        
        // Counter for detected manipulations
        this.manipulationsDetected = Counter.builder("xai.manipulations.detected")
            .description("Number of suspicious explanations flagged")
            .register(meterRegistry);
        
        // Gauge for current trust score
        this.currentTrustScore = new AtomicReference<>(0.0);
        meterRegistry.gauge("xai.trust.score", currentTrustScore, AtomicReference::get);
        
        // Gauge for current robustness score
        this.currentRobustnessScore = new AtomicReference<>(0.0);
        meterRegistry.gauge("xai.robustness.score", currentRobustnessScore, AtomicReference::get);
        
        // Gauge for current drift magnitude
        this.currentDriftMagnitude = new AtomicReference<>(0.0);
        meterRegistry.gauge("xai.drift.magnitude", currentDriftMagnitude, AtomicReference::get);
        
        log.info("ExplainerMetrics initialized with Prometheus export");
    }
    
    /**
     * Record explanation generation with latency measurement.
     * 
     * @param explanation the generated explanation
     * @param latencyMicros latency in microseconds
     */
    public void recordExplanation(Explanation explanation, long latencyMicros) {
        explanationLatency.record(latencyMicros, TimeUnit.MICROSECONDS);
        explanationsTotal.increment();
        
        // Update current metrics from explanation
        double stability = explanation.getStabilityScore();
        currentTrustScore.set(stability);
        
        log.debug("Recorded explanation: latency={}μs, stability={:.3f}", latencyMicros, stability);
    }
    
    /**
     * Record robustness score from RobustnessScore analysis.
     */
    public void recordRobustnessScore(double score) {
        if (score >= 0.0 && score <= 1.0) {
            currentRobustnessScore.set(score);
            log.debug("Recorded robustness score: {:.3f}", score);
        }
    }
    
    /**
     * Record drift detection magnitude.
     */
    public void recordDriftMagnitude(double magnitude) {
        if (magnitude >= 0.0 && magnitude <= 1.0) {
            currentDriftMagnitude.set(magnitude);
            log.debug("Recorded drift magnitude: {:.3f}", magnitude);
        }
    }
    
    /**
     * Increment counter for detected manipulation.
     */
    public void flagManipulation() {
        manipulationsDetected.increment();
        log.warn("Suspicious explanation detected and flagged");
    }
    
    /**
     * Get current trust score gauge value.
     */
    public double getTrustScore() {
        return currentTrustScore.get();
    }
    
    /**
     * Get current robustness score gauge value.
     */
    public double getRobustnessScore() {
        return currentRobustnessScore.get();
    }
    
    /**
     * Get current drift magnitude gauge value.
     */
    public double getDriftMagnitude() {
        return currentDriftMagnitude.get();
    }
    
    /**
     * Record timing operation (e.g., for @Timed annotation).
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }
    
    @Override
    public String toString() {
        return String.format(
            "ExplainerMetrics{trust=%.3f, robustness=%.3f, drift=%.3f}",
            getTrustScore(),
            getRobustnessScore(),
            getDriftMagnitude()
        );
    }
}
