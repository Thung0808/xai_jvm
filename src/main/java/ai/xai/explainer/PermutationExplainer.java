package ai.xai.explainer;

import ai.xai.core.Explanation;
import ai.xai.core.Predictable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Permutation Feature Importance explainer.
 * 
 * Algorithm:
 * 1. Calculate baseline accuracy with original data
 * 2. For each feature:
 *    a. Shuffle (permute) that feature's values
 *    b. Calculate new accuracy
 *    c. Importance = baseline - new_accuracy
 * 3. Higher drop in accuracy = more important feature
 */
public class PermutationExplainer {

    private final Predictable model;
    private final double[][] X;
    private final double[] y;
    private final Random random;

    /**
     * Creates a permutation explainer.
     * 
     * @param model the predictable model
     * @param X training/validation data (samples x features)
     * @param y true labels
     */
    public PermutationExplainer(Predictable model, double[][] X, double[] y) {
        this.model = model;
        this.X = X;
        this.y = y;
        this.random = new Random(42);
    }

    /**
     * Generates an explanation by computing permutation importance.
     * 
     * @return explanation with feature importance scores
     */
    public Explanation explain() {
        double baseline = accuracy(X, y);
        Map<String, Double> importance = new HashMap<>();

        int features = X[0].length;

        for (int f = 0; f < features; f++) {
            double[][] permuted = permuteFeature(X, f);
            double score = accuracy(permuted, y);
            double importanceScore = baseline - score;
            importance.put("feature_" + f, importanceScore);
        }

        return new Explanation(importance);
    }

    /**
     * Calculates accuracy of the model on given data.
     * 
     * @param X input data
     * @param y true labels
     * @return accuracy score (0.0 to 1.0)
     */
    private double accuracy(double[][] X, double[] y) {
        int correct = 0;
        for (int i = 0; i < X.length; i++) {
            double pred = model.predict(X[i]);
            // Binary classification: pred >= 0.5 → class 1, else class 0
            if ((pred >= 0.5 && y[i] == 1.0) ||
                (pred < 0.5 && y[i] == 0.0)) {
                correct++;
            }
        }
        return (double) correct / X.length;
    }

    /**
     * Creates a copy of X with one feature permuted (shuffled).
     * 
     * @param X original data
     * @param featureIndex index of feature to permute
     * @return new array with permuted feature
     */
    private double[][] permuteFeature(double[][] X, int featureIndex) {
        int rows = X.length;
        int cols = X[0].length;
        
        // Create a deep copy of X
        double[][] permuted = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(X[i], 0, permuted[i], 0, cols);
        }

        // Extract the feature column
        double[] featureValues = new double[rows];
        for (int i = 0; i < rows; i++) {
            featureValues[i] = X[i][featureIndex];
        }

        // Shuffle the feature values using Fisher-Yates algorithm
        for (int i = rows - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            double temp = featureValues[i];
            featureValues[i] = featureValues[j];
            featureValues[j] = temp;
        }

        // Put shuffled values back
        for (int i = 0; i < rows; i++) {
            permuted[i][featureIndex] = featureValues[i];
        }

        return permuted;
    }
}
