package ai.xai.core;

/**
 * Generic interface for predictable models.
 * Allows different ML libraries to be adapted to a common interface.
 */
public interface Predictable {
    /**
     * Makes a prediction for the given input.
     * 
     * @param input feature vector
     * @return predicted value (typically 0.0-1.0 for classification)
     */
    double predict(double[] input);
}
