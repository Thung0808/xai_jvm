package io.github.Thung0808.xai.api;

/**
 * Core interface for explainability algorithms.
 * 
 * <p>An explainer generates human-interpretable explanations for predictions
 * made by machine learning models. Different explainers use different algorithms
 * (e.g., LIME, SHAP, Permutation Importance).</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * Explainer<PredictiveModel> explainer = new PermutationExplainer();
 * Explanation exp = explainer.explain(model, new double[]{1.0, 2.0, 3.0});
 * }</pre>
 *
 * @param <M> the type of model this explainer can explain (must extend PredictiveModel)
 * @since 0.1.0
 */
@FunctionalInterface
@Stable(since = "0.3.0")
public interface Explainer<M extends PredictiveModel> {
    
    /**
     * Generates an explanation for a single prediction.
     * 
     * @param model the model to explain
     * @param input the input instance to explain
     * @return an explanation containing feature attributions and metadata
     * @throws IllegalArgumentException if model or input is null
     * @throws IllegalStateException if explanation cannot be generated
     */
    Explanation explain(M model, double[] input);
}
