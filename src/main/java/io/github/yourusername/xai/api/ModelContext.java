package io.github.Thung0808.xai.api;

import java.util.List;
import java.util.Objects;

/**
 * Context information about a model to enable meaningful explanations.
 * 
 * <p>This includes feature names and baseline values used for perturbation.</p>
 *
 * @param featureNames names of input features
 * @param baselines baseline values for each feature (used for permutation)
 * @since 0.2.0
 */
public record ModelContext(
    List<String> featureNames,
    double[] baselines
) {
    
    public ModelContext {
        Objects.requireNonNull(featureNames, "Feature names cannot be null");
        Objects.requireNonNull(baselines, "Baselines cannot be null");
        
        if (featureNames.size() != baselines.length) {
            throw new IllegalArgumentException(
                "Feature names and baselines must have same length");
        }
        
        // Defensive copy
        featureNames = List.copyOf(featureNames);
        baselines = baselines.clone();
    }
    
    /**
     * Creates a context with default baselines (all zeros).
     */
    public static ModelContext withDefaults(List<String> featureNames) {
        return new ModelContext(featureNames, new double[featureNames.size()]);
    }
    
    /**
     * Creates a context from sample data (uses mean as baseline).
     */
    public static ModelContext fromData(List<String> featureNames, double[][] data) {
        double[] baselines = new double[featureNames.size()];
        
        for (int i = 0; i < featureNames.size(); i++) {
            double sum = 0.0;
            for (double[] row : data) {
                sum += row[i];
            }
            baselines[i] = sum / data.length;
        }
        
        return new ModelContext(featureNames, baselines);
    }
    
    public int featureCount() {
        return featureNames.size();
    }
}
