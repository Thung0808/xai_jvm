package io.github.thung0808.xai.api.stability;

import io.github.thung0808.xai.api.Explainer;
import io.github.thung0808.xai.api.PredictiveModel;

/**
 * Interface for measuring the stability/reliability of explanations.
 * 
 * <p>Stability metrics answer the question: "If I run the explainer multiple times,
 * or with slightly perturbed inputs, do I get consistent explanations?"</p>
 * 
 * <p>This is critical for:</p>
 * <ul>
 *   <li>Production systems where explanations must be trustworthy</li>
 *   <li>Identifying when an explainer is unreliable</li>
 *   <li>Comparing different explainability algorithms</li>
 * </ul>
 * 
 * <p>Higher scores (closer to 1.0) indicate more stable explanations.</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface StabilityMetric {
    
    /**
     * Computes a stability score for explanations of the given input.
     * 
     * @param model the model being explained
     * @param explainer the explainer to test
     * @param input the input instance to explain
     * @param trials number of times to run the explainer (higher = more accurate but slower)
     * @return stability score from 0.0 (unstable) to 1.0 (perfectly stable)
     * @throws IllegalArgumentException if parameters are invalid
     */
    double compute(
        PredictiveModel model,
        Explainer<PredictiveModel> explainer,
        double[] input,
        int trials
    );
    
    /**
     * Computes stability with default number of trials (30).
     */
    default double compute(
        PredictiveModel model,
        Explainer<PredictiveModel> explainer,
        double[] input
    ) {
        return compute(model, explainer, input, 30);
    }
}


