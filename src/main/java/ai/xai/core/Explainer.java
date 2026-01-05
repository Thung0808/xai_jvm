package ai.xai.core;

/**
 * Core interface for explainable AI algorithms.
 * 
 * @param <M> the model type
 * @param <X> the input data type
 */
public interface Explainer<M, X> {
    /**
     * Generates an explanation for the given model and input.
     * 
     * @param model the model to explain
     * @param input the input data
     * @return explanation with feature importance scores
     */
    Explanation explain(M model, X input);
}
