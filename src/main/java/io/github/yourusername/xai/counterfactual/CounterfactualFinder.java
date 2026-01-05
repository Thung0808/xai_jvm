package io.github.Thung0808.xai.counterfactual;

import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.experimental.Incubating;

/**
 * Finds minimal changes to input that flip prediction to desired target.
 * 
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li>Loan rejection → "Change income from $50k to $55k to get approved"</li>
 *   <li>Credit score → "Pay down $2000 debt to improve score"</li>
 *   <li>Medical diagnosis → "Reduce blood pressure to X to avoid risk"</li>
 * </ul>
 * 
 * <p><b>Algorithm:</b> Growing Spheres (DiCE-inspired)</p>
 * <ol>
 *   <li>Start with original input</li>
 *   <li>Perturb mutable features in small increments</li>
 *   <li>Stop when prediction crosses threshold</li>
 *   <li>Return minimal change set</li>
 * </ol>
 * 
 * <p><b>Constraints:</b></p>
 * <ul>
 *   <li>Feature bounds (min/max values)</li>
 *   <li>Immutable features (e.g., age, gender)</li>
 *   <li>Cost weights (changing some features is harder/more expensive)</li>
 * </ul>
 *
 * @since 0.3.0
 */
@Incubating(
    since = "0.3.0",
    graduationTarget = "1.0.0",
    reason = "Algorithm implementation still being validated in production scenarios"
)
public interface CounterfactualFinder {
    
    /**
     * Finds minimal changes to reach target prediction.
     * 
     * @param model the predictive model
     * @param input current input
     * @param targetPrediction desired prediction value
     * @param config search configuration
     * @return counterfactual result
     */
    CounterfactualResult findCounterfactual(
        PredictiveModel model,
        double[] input,
        double targetPrediction,
        CounterfactualConfig config
    );
    
    /**
     * Finds multiple diverse counterfactuals.
     * 
     * @param model the predictive model
     * @param input current input
     * @param targetPrediction desired prediction
     * @param config search configuration
     * @param numCounterfactuals number of alternatives to find
     * @return list of diverse counterfactuals
     */
    java.util.List<CounterfactualResult> findDiverseCounterfactuals(
        PredictiveModel model,
        double[] input,
        double targetPrediction,
        CounterfactualConfig config,
        int numCounterfactuals
    );
}
