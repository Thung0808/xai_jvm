package io.github.Thung0808.xai.global;

import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.api.Stable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Container for global explanation results.
 * 
 * <p>Holds aggregated insights across a dataset:</p>
 * <ul>
 *   <li>Feature importance rankings</li>
 *   <li>Importance distributions</li>
 *   <li>Dataset statistics</li>
 *   <li>Model-level metrics</li>
 * </ul>
 *
 * @since 0.4.0
 */
@Stable(since = "0.4.0")
public class GlobalExplanation {
    
    private final String explainerName;
    private final List<FeatureAttribution> featureImportances;
    private final int datasetSize;
    private final int numFeatures;
    private final Map<String, Object> metadata;
    
    /**
     * Creates a global explanation.
     * 
     * @param explainerName the explainer that generated this (e.g., "GlobalFeatureImportance")
     * @param featureImportances list of feature attributions for all features
     * @param datasetSize number of instances in dataset
     * @param numFeatures number of input features
     */
    public GlobalExplanation(
            String explainerName,
            List<FeatureAttribution> featureImportances,
            int datasetSize,
            int numFeatures) {
        this.explainerName = explainerName;
        this.featureImportances = List.copyOf(featureImportances);
        this.datasetSize = datasetSize;
        this.numFeatures = numFeatures;
        this.metadata = new LinkedHashMap<>();
    }
    
    /**
     * Returns the source explainer name.
     */
    public String getExplainerName() {
        return explainerName;
    }
    
    /**
     * Returns all feature importances (sorted by importance descending).
     */
    public List<FeatureAttribution> getFeatureImportances() {
        return featureImportances.stream()
            .sorted(Comparator.comparingDouble((FeatureAttribution a) -> Math.abs(a.importance())).reversed())
            .collect(Collectors.toUnmodifiableList());
    }
    
    /**
     * Returns top-K most important features.
     */
    public List<FeatureAttribution> getTopFeatures(int k) {
        return getFeatureImportances().stream()
            .limit(k)
            .collect(Collectors.toUnmodifiableList());
    }
    
    /**
     * Returns feature importances as a map.
     */
    public Map<String, Double> toMap() {
        return getFeatureImportances().stream()
            .collect(Collectors.toMap(
                FeatureAttribution::feature,
                FeatureAttribution::importance,
                (v1, v2) -> v1,
                LinkedHashMap::new
            ));
    }
    
    /**
     * Returns the dataset size used for this explanation.
     */
    public int getDatasetSize() {
        return datasetSize;
    }
    
    /**
     * Returns the number of features analyzed.
     */
    public int getNumFeatures() {
        return numFeatures;
    }
    
    /**
     * Stores custom metadata (e.g., fidelity scores, timing).
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }
    
    /**
     * Retrieves custom metadata.
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Returns total importance (sum of absolute values).
     */
    public double getTotalImportance() {
        return featureImportances.stream()
            .mapToDouble(a -> Math.abs(a.importance()))
            .sum();
    }
    
    /**
     * Returns variance in feature importances.
     */
    public double getImportanceVariance() {
        List<Double> importances = featureImportances.stream()
            .map(a -> Math.abs(a.importance()))
            .collect(Collectors.toList());
        
        if (importances.isEmpty()) {
            return 0.0;
        }
        
        double mean = importances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = importances.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        
        return variance;
    }
    
    /**
     * Returns Gini coefficient (feature importance concentration).
     * 
     * <p>0 = uniform distribution, 1 = concentrated on single feature</p>
     */
    public double getGiniCoefficient() {
        List<Double> importances = featureImportances.stream()
            .map(a -> Math.abs(a.importance()))
            .sorted()
            .collect(Collectors.toList());
        
        if (importances.isEmpty() || importances.stream().mapToDouble(Double::doubleValue).sum() == 0) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (int i = 0; i < importances.size(); i++) {
            sum += (2 * (i + 1) - importances.size() - 1) * importances.get(i);
        }
        
        double total = importances.stream().mapToDouble(Double::doubleValue).sum();
        return sum / (importances.size() * total);
    }
    
    @Override
    public String toString() {
        return String.format(
            "GlobalExplanation{explainer=%s, features=%d, dataset=%d, topFeature=%s, gini=%.3f}",
            explainerName, numFeatures, datasetSize,
            getTopFeatures(1).stream().map(FeatureAttribution::feature).findFirst().orElse("none"),
            getGiniCoefficient()
        );
    }
}
