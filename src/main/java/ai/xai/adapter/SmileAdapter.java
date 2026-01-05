package ai.xai.adapter;

import ai.xai.core.Predictable;
import smile.classification.LogisticRegression;

/**
 * Adapter for Smile ML LogisticRegression model.
 * Converts Smile's model interface to our Predictable interface.
 */
public class SmileAdapter implements Predictable {

    private final LogisticRegression model;

    /**
     * Creates an adapter for a Smile LogisticRegression model.
     * 
     * @param model the Smile model to wrap
     */
    public SmileAdapter(LogisticRegression model) {
        this.model = model;
    }

    /**
     * Makes a prediction using the wrapped Smile model.
     * 
     * @param input feature vector
     * @return predicted probability (0.0-1.0)
     */
    @Override
    public double predict(double[] input) {
        int prediction = model.predict(input);
        // For binary classification, return 0.0 or 1.0
        return (double) prediction;
    }
}
