package ai.xai.core;

import java.util.Map;

/**
 * Represents an explanation of a model's predictions.
 * Contains feature importance scores.
 */
public class Explanation {
    private final Map<String, Double> featureImportance;

    /**
     * Creates a new explanation with the given feature importance scores.
     * 
     * @param featureImportance map of feature names to importance scores
     */
    public Explanation(Map<String, Double> featureImportance) {
        this.featureImportance = featureImportance;
    }

    /**
     * Returns the feature importance scores.
     * 
     * @return map of feature names to importance scores
     */
    public Map<String, Double> getFeatureImportance() {
        return featureImportance;
    }

    @Override
    public String toString() {
        return "Explanation{featureImportance=" + featureImportance + "}";
    }
}
