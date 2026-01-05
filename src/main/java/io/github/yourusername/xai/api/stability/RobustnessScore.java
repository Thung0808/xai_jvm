package io.github.Thung0808.xai.api.stability;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.Explainer;
import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.experimental.Incubating;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Measures explanation robustness under adversarial input perturbations.
 * 
 * <p><strong>EU AI Act Compliance:</strong> Article 15 requires high-risk AI systems
 * to be "sufficiently robust" and "resilient against attempts to alter their use
 * or performance by exploiting their vulnerabilities."
 * 
 * <h2>Purpose</h2>
 * <p>Verifies that explanations remain stable when inputs are perturbed slightly.
 * If tiny changes to input features cause dramatic changes in attributions, the
 * explanations may be unreliable or manipulable.
 * 
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>Generate explanation for original instance x</li>
 *   <li>Create n perturbed versions: x' = x + ε·noise</li>
 *   <li>Generate explanations for each x'</li>
 *   <li>Measure attribution stability across perturbations</li>
 * </ol>
 * 
 * <p><strong>Robustness Score:</strong>
 * <blockquote>
 * $$R = 1 - \frac{1}{n} \sum_{i=1}^{n} \frac{\|\phi(x') - \phi(x)\|_2}{\|\phi(x)\|_2}$$
 * </blockquote>
 * 
 * <p>Where:
 * <ul>
 *   <li>R ∈ [0, 1] (higher is more robust)</li>
 *   <li>φ(x) = attribution vector for instance x</li>
 *   <li>ε = perturbation magnitude</li>
 *   <li>n = number of perturbations tested</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create robustness tester
 * RobustnessScore robustness = new RobustnessScore(
 *     explainer,
 *     100,     // Test 100 perturbations
 *     0.01     // ±1% noise
 * );
 * 
 * // Test explanation stability
 * RobustnessReport report = robustness.evaluate(model, instance);
 * 
 * // Check if robust enough
 * if (report.score() > 0.95) {
 *     System.out.println("✅ Explanations are robust");
 * } else {
 *     System.out.println("⚠️ Explanations may be unstable");
 * }
 * 
 * // Get detailed metrics
 * System.out.println("Mean stability: " + report.meanStability());
 * System.out.println("Worst-case drift: " + report.maxDrift());
 * }</pre>
 * 
 * <h2>Interpretation</h2>
 * <table border="1">
 * <tr><th>Score</th><th>Interpretation</th><th>Action</th></tr>
 * <tr><td>&gt; 0.95</td><td>Highly robust</td><td>✅ Safe for production</td></tr>
 * <tr><td>0.85-0.95</td><td>Moderately robust</td><td>⚠️ Monitor in production</td></tr>
 * <tr><td>&lt; 0.85</td><td>Unstable</td><td>❌ Investigate before deployment</td></tr>
 * </table>
 * 
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Computational cost: O(n × explainer_cost)</li>
 *   <li>Gaussian noise may not match real adversarial attacks</li>
 *   <li>Score depends on perturbation magnitude ε</li>
 * </ul>
 * 
 * @since 1.1.0-alpha
 * @see <a href="https://digital-strategy.ec.europa.eu/en/policies/regulatory-framework-ai">EU AI Act</a>
 */
@Incubating(since = "1.1.0-alpha", graduationTarget = "1.2.0")
public class RobustnessScore {
    
    private final Explainer<PredictiveModel> explainer;
    private final int numPerturbations;
    private final double perturbationMagnitude;
    private final PerturbationType perturbationType;
    
    /**
     * Type of perturbation to apply.
     */
    public enum PerturbationType {
        /** Gaussian noise: x' = x + N(0, ε) */
        GAUSSIAN,
        
        /** Uniform noise: x' = x + U(-ε, +ε) */
        UNIFORM,
        
        /** Adversarial (gradient-based, coming soon) */
        ADVERSARIAL
    }
    
    /**
     * Creates a RobustnessScore with default settings.
     * 
     * @param explainer the explainer to test
     */
    public RobustnessScore(Explainer<PredictiveModel> explainer) {
        this(explainer, 100, 0.01, PerturbationType.GAUSSIAN);
    }
    
    /**
     * Creates a RobustnessScore with custom settings.
     * 
     * @param explainer the explainer to test
     * @param numPerturbations number of perturbed instances to test
     * @param perturbationMagnitude noise magnitude (e.g., 0.01 = 1% of feature range)
     */
    public RobustnessScore(Explainer<PredictiveModel> explainer, 
                           int numPerturbations, 
                           double perturbationMagnitude) {
        this(explainer, numPerturbations, perturbationMagnitude, PerturbationType.GAUSSIAN);
    }
    
    /**
     * Creates a RobustnessScore with full customization.
     * 
     * @param explainer the explainer to test
     * @param numPerturbations number of perturbed instances to test
     * @param perturbationMagnitude noise magnitude
     * @param perturbationType type of noise to add
     */
    public RobustnessScore(Explainer<PredictiveModel> explainer,
                           int numPerturbations,
                           double perturbationMagnitude,
                           PerturbationType perturbationType) {
        if (numPerturbations < 10) {
            throw new IllegalArgumentException("numPerturbations must be >= 10");
        }
        if (perturbationMagnitude <= 0.0 || perturbationMagnitude > 0.5) {
            throw new IllegalArgumentException("perturbationMagnitude must be in (0, 0.5]");
        }
        
        this.explainer = explainer;
        this.numPerturbations = numPerturbations;
        this.perturbationMagnitude = perturbationMagnitude;
        this.perturbationType = perturbationType;
    }
    
    /**
     * Evaluates explanation robustness for a given instance.
     * 
     * @param model the predictive model
     * @param instance the instance to test
     * @return robustness report with metrics
     */
    public RobustnessReport evaluate(PredictiveModel model, double[] instance) {
        long startTime = System.nanoTime();
        
        // Get baseline explanation
        Explanation baseline = explainer.explain(model, instance);
        double[] baselineAttrs = extractAttributions(baseline);
        
        // Generate perturbed instances and explanations
        List<Double> stabilities = new ArrayList<>();
        List<Double> drifts = new ArrayList<>();
        double maxDrift = 0.0;
        double minStability = 1.0;
        
        for (int i = 0; i < numPerturbations; i++) {
            // Perturb instance
            double[] perturbed = perturb(instance);
            
            // Get explanation
            Explanation perturbedExpl = explainer.explain(model, perturbed);
            double[] perturbedAttrs = extractAttributions(perturbedExpl);
            
            // Compute stability
            double drift = computeDrift(baselineAttrs, perturbedAttrs);
            double stability = 1.0 - drift;
            
            stabilities.add(stability);
            drifts.add(drift);
            
            if (drift > maxDrift) maxDrift = drift;
            if (stability < minStability) minStability = stability;
        }
        
        // Compute aggregate metrics
        double meanStability = stabilities.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        
        double stdStability = computeStdDev(stabilities, meanStability);
        
        double robustnessScore = meanStability; // Overall score
        
        long endTime = System.nanoTime();
        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        
        return new RobustnessReport(
            robustnessScore,
            meanStability,
            stdStability,
            minStability,
            maxDrift,
            numPerturbations,
            perturbationMagnitude,
            perturbationType,
            elapsedMs
        );
    }
    
    /**
     * Perturbs an instance with noise.
     */
    private double[] perturb(double[] instance) {
        double[] perturbed = new double[instance.length];
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        
        for (int i = 0; i < instance.length; i++) {
            double noise;
            
            switch (perturbationType) {
                case GAUSSIAN:
                    noise = rng.nextGaussian() * perturbationMagnitude;
                    break;
                    
                case UNIFORM:
                    noise = rng.nextDouble(-perturbationMagnitude, perturbationMagnitude);
                    break;
                    
                case ADVERSARIAL:
                    // TODO: Implement gradient-based adversarial perturbation
                    throw new UnsupportedOperationException("Adversarial perturbation not yet implemented");
                    
                default:
                    noise = 0.0;
            }
            
            perturbed[i] = instance[i] + noise;
        }
        
        return perturbed;
    }
    
    /**
     * Extracts attribution values from explanation.
     */
    private double[] extractAttributions(Explanation explanation) {
        List<FeatureAttribution> attrs = explanation.getAttributions();
        double[] values = new double[attrs.size()];
        
        for (int i = 0; i < attrs.size(); i++) {
            values[i] = attrs.get(i).importance();
        }
        
        return values;
    }
    
    /**
     * Computes normalized drift between two attribution vectors.
     * 
     * <p>Drift = ||φ' - φ|| / ||φ||
     */
    private double computeDrift(double[] baseline, double[] perturbed) {
        if (baseline.length != perturbed.length) {
            throw new IllegalArgumentException("Attribution vectors must have same length");
        }
        
        double sumSqDiff = 0.0;
        double sumSqBase = 0.0;
        
        for (int i = 0; i < baseline.length; i++) {
            double diff = perturbed[i] - baseline[i];
            sumSqDiff += diff * diff;
            sumSqBase += baseline[i] * baseline[i];
        }
        
        if (sumSqBase < 1e-10) {
            // Avoid division by zero
            return Math.sqrt(sumSqDiff);
        }
        
        return Math.sqrt(sumSqDiff / sumSqBase);
    }
    
    /**
     * Computes standard deviation.
     */
    private double computeStdDev(List<Double> values, double mean) {
        double sumSqDiff = 0.0;
        
        for (double value : values) {
            double diff = value - mean;
            sumSqDiff += diff * diff;
        }
        
        return Math.sqrt(sumSqDiff / values.size());
    }
    
    /**
     * Report containing robustness metrics.
     * 
     * @param score overall robustness score [0, 1]
     * @param meanStability average stability across perturbations
     * @param stdStability standard deviation of stability
     * @param minStability worst-case stability
     * @param maxDrift worst-case drift
     * @param numPerturbations number of perturbations tested
     * @param perturbationMagnitude noise magnitude used
     * @param perturbationType type of perturbation
     * @param elapsedMs evaluation time in milliseconds
     */
    public record RobustnessReport(
        double score,
        double meanStability,
        double stdStability,
        double minStability,
        double maxDrift,
        int numPerturbations,
        double perturbationMagnitude,
        PerturbationType perturbationType,
        double elapsedMs
    ) {
        /**
         * Returns a human-readable interpretation.
         */
        public String interpretation() {
            if (score > 0.95) {
                return "Highly robust - Safe for production";
            } else if (score > 0.85) {
                return "Moderately robust - Monitor in production";
            } else {
                return "Unstable - Investigate before deployment";
            }
        }
        
        /**
         * Returns true if explanations pass robustness threshold.
         */
        public boolean isRobust() {
            return score > 0.85;
        }
        
        /**
         * Returns formatted summary string.
         */
        public String summary() {
            return String.format(
                "Robustness Score: %.3f (%s)\n" +
                "Mean Stability: %.3f ± %.3f\n" +
                "Worst-case Drift: %.3f\n" +
                "Perturbations: %d × %.1f%% noise (%s)\n" +
                "Evaluation Time: %.1fms",
                score, interpretation(),
                meanStability, stdStability,
                maxDrift,
                numPerturbations, perturbationMagnitude * 100, perturbationType,
                elapsedMs
            );
        }
    }
}
