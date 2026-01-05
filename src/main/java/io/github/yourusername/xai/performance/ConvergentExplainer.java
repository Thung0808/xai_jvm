package io.github.Thung0808.xai.performance;

import io.github.Thung0808.xai.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Stability-aware explainer that automatically determines sample size.
 * 
 * <p>Unlike SHAP which requires manual sample specification, this explainer
 * runs until explanations converge within a specified tolerance (epsilon).</p>
 * 
 * <p><b>Algorithm:</b></p>
 * <ol>
 *   <li>Start with minimum samples (e.g., 10)</li>
 *   <li>Compute explanation</li>
 *   <li>Increase samples and recompute</li>
 *   <li>If change < epsilon, stop (converged)</li>
 *   <li>Otherwise, continue up to max samples</li>
 * </ol>
 * 
 * <p>This ensures explanations are both fast AND reliable.</p>
 *
 * @since 0.2.0
 */
@Stable(since = "0.3.0")
public class ConvergentExplainer implements Explainer<PredictiveModel> {
    
    private static final Logger log = LoggerFactory.getLogger(ConvergentExplainer.class);
    
    private final ModelContext context;
    private final double epsilon; // Convergence threshold
    private final int minSamples;
    private final int maxSamples;
    private final int stepSize;
    
    /**
     * Creates a convergent explainer with custom configuration.
     * 
     * @param context model context with feature names
     * @param epsilon convergence threshold (0.01 = 1% change)
     * @param minSamples minimum samples to try
     * @param maxSamples maximum samples before giving up
     * @param stepSize samples to add each iteration
     */
    public ConvergentExplainer(
        ModelContext context,
        double epsilon,
        int minSamples,
        int maxSamples,
        int stepSize
    ) {
        this.context = context;
        this.epsilon = epsilon;
        this.minSamples = minSamples;
        this.maxSamples = maxSamples;
        this.stepSize = stepSize;
    }
    
    /**
     * Creates a convergent explainer with default settings.
     */
    public ConvergentExplainer(ModelContext context) {
        this(context, 0.01, 10, 100, 10);
    }
    
    @Override
    public Explanation explain(PredictiveModel model, double[] input) {
        log.debug("Starting convergent explanation (epsilon={})", epsilon);
        
        List<FeatureAttribution> previousAttrs = null;
        int currentSamples = minSamples;
        int iterations = 0;
        
        while (currentSamples <= maxSamples) {
            iterations++;
            
            // Use parallel explainer for this iteration
            ParallelExplainer explainer = new ParallelExplainer(
                context, 42, currentSamples, 0.01);
            
            Explanation current = explainer.explain(model, input);
            List<FeatureAttribution> currentAttrs = current.getAttributions();
            
            // Check convergence
            if (previousAttrs != null) {
                double maxChange = computeMaxChange(previousAttrs, currentAttrs);
                
                log.debug("Iteration {}: samples={}, maxChange={:.4f}", 
                    iterations, currentSamples, maxChange);
                
                if (maxChange < epsilon) {
                    log.info("Converged after {} iterations with {} samples", 
                        iterations, currentSamples);
                    
                    // Add convergence info to metadata
                    ExplanationMetadata meta = ExplanationMetadata.builder("ConvergentExplainer")
                        .seed(42)
                        .trials(currentSamples)
                        .build();
                    
                    return Explanation.builder()
                        .withPrediction(current.getPrediction())
                        .withBaseline(current.getBaseline())
                        .addAllAttributions(currentAttrs)
                        .withMetadata(meta)
                        .build();
                }
            }
            
            previousAttrs = currentAttrs;
            currentSamples += stepSize;
        }
        
        log.warn("Did not converge within {} samples (epsilon={})", maxSamples, epsilon);
        
        // Return last result even if not converged
        ParallelExplainer finalExplainer = new ParallelExplainer(
            context, 42, maxSamples, 0.01);
        return finalExplainer.explain(model, input);
    }
    
    /**
     * Computes maximum relative change between two sets of attributions.
     */
    private double computeMaxChange(
        List<FeatureAttribution> prev,
        List<FeatureAttribution> current
    ) {
        double maxChange = 0.0;
        
        for (int i = 0; i < prev.size(); i++) {
            double prevImportance = prev.get(i).importance();
            double currImportance = current.get(i).importance();
            
            // Relative change (avoid division by zero)
            double denominator = Math.max(Math.abs(prevImportance), 1e-10);
            double relativeChange = Math.abs(currImportance - prevImportance) / denominator;
            
            maxChange = Math.max(maxChange, relativeChange);
        }
        
        return maxChange;
    }
    
    /**
     * Computes a convergence report for diagnostic purposes.
     */
    public ConvergenceReport analyzeConvergence(PredictiveModel model, double[] input) {
        List<Integer> sampleCounts = new ArrayList<>();
        List<Double> maxChanges = new ArrayList<>();
        
        List<FeatureAttribution> previousAttrs = null;
        int currentSamples = minSamples;
        
        while (currentSamples <= maxSamples) {
            ParallelExplainer explainer = new ParallelExplainer(
                context, 42, currentSamples, 0.01);
            
            Explanation current = explainer.explain(model, input);
            List<FeatureAttribution> currentAttrs = current.getAttributions();
            
            if (previousAttrs != null) {
                double maxChange = computeMaxChange(previousAttrs, currentAttrs);
                sampleCounts.add(currentSamples);
                maxChanges.add(maxChange);
            }
            
            previousAttrs = currentAttrs;
            currentSamples += stepSize;
        }
        
        return new ConvergenceReport(sampleCounts, maxChanges, epsilon);
    }
    
    /**
     * Report showing convergence behavior.
     */
    public record ConvergenceReport(
        List<Integer> sampleCounts,
        List<Double> maxChanges,
        double epsilon
    ) {
        
        public boolean hasConverged() {
            return maxChanges.stream().anyMatch(change -> change < epsilon);
        }
        
        public int convergenceSamples() {
            for (int i = 0; i < maxChanges.size(); i++) {
                if (maxChanges.get(i) < epsilon) {
                    return sampleCounts.get(i);
                }
            }
            return -1; // Not converged
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Convergence Analysis:\n");
            sb.append(String.format("Epsilon threshold: %.4f\n", epsilon));
            sb.append(String.format("Converged: %s\n", hasConverged()));
            
            if (hasConverged()) {
                sb.append(String.format("Samples required: %d\n", convergenceSamples()));
            }
            
            sb.append("\nIteration History:\n");
            for (int i = 0; i < sampleCounts.size(); i++) {
                sb.append(String.format("  Samples=%d: maxChange=%.4f %s\n",
                    sampleCounts.get(i),
                    maxChanges.get(i),
                    maxChanges.get(i) < epsilon ? "✓ Converged" : ""));
            }
            
            return sb.toString();
        }
    }
}
