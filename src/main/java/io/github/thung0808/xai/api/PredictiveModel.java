package io.github.thung0808.xai.api;

/**
 * Core interface for predictive models in the XAI framework.
 * This is a functional interface that represents any model capable of making predictions.
 * 
 * <p>This interface is intentionally simple and framework-agnostic, allowing integration
 * with any ML library (Smile, Weka, DL4J, custom models, etc.).</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Lambda implementation
 * PredictiveModel linear = input -> input[0] * 0.5 + input[1] * 0.3;
 * 
 * // Method reference
 * PredictiveModel smileModel = myLogisticRegression::predict;
 * }</pre>
 *
 * @since 0.1.0
 */
@FunctionalInterface
@Stable(since = "0.3.0")
public interface PredictiveModel {
    
    /**
     * Makes a prediction for the given input.
     * 
     * @param input feature vector as primitive double array (zero-allocation)
     * @return predicted value (typically 0.0-1.0 for classification, any value for regression)
     * @throws IllegalArgumentException if input is null or has incorrect dimensions
     */
    double predict(double[] input);
    
    /**
     * Returns the number of features expected by this model.
     * Default implementation returns -1 (unknown).
     * 
     * @return number of input features, or -1 if not known
     */
    default int getFeatureCount() {
        return -1;
    }
}


