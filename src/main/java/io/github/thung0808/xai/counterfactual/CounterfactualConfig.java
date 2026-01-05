package io.github.thung0808.xai.counterfactual;

import java.util.Arrays;
import java.util.Set;

/**
 * Configuration for counterfactual search.
 * 
 * <p>Defines constraints, bounds, and search parameters.</p>
 *
 * @param featureBounds min/max values for each feature
 * @param immutableFeatures indices of features that cannot be changed
 * @param featureCosts cost of changing each feature (1.0 = normal, 10.0 = expensive)
 * @param maxIterations maximum search iterations
 * @param tolerance acceptable distance from target prediction
 * @since 0.3.0
 */
public record CounterfactualConfig(
    double[][] featureBounds,
    Set<Integer> immutableFeatures,
    double[] featureCosts,
    int maxIterations,
    double tolerance
) {
    
    /**
     * Builder for CounterfactualConfig.
     */
    public static class Builder {
        private double[][] featureBounds;
        private Set<Integer> immutableFeatures = Set.of();
        private double[] featureCosts;
        private int maxIterations = 1000;
        private double tolerance = 0.01;
        
        /**
         * Sets feature bounds [min, max] for each feature.
         */
        public Builder featureBounds(double[][] bounds) {
            this.featureBounds = bounds;
            return this;
        }
        
        /**
         * Sets indices of immutable features (e.g., age, gender).
         */
        public Builder immutableFeatures(Set<Integer> indices) {
            this.immutableFeatures = indices;
            return this;
        }
        
        /**
         * Sets cost multipliers for each feature.
         * Higher cost = less preferred to change.
         */
        public Builder featureCosts(double[] costs) {
            this.featureCosts = costs;
            return this;
        }
        
        /**
         * Sets maximum search iterations.
         */
        public Builder maxIterations(int iterations) {
            this.maxIterations = iterations;
            return this;
        }
        
        /**
         * Sets tolerance for target prediction.
         */
        public Builder tolerance(double tolerance) {
            this.tolerance = tolerance;
            return this;
        }
        
        /**
         * Builds the configuration.
         */
        public CounterfactualConfig build() {
            if (featureBounds == null) {
                throw new IllegalStateException("Feature bounds are required");
            }
            
            // Default costs: all features equally costly
            if (featureCosts == null) {
                featureCosts = new double[featureBounds.length];
                Arrays.fill(featureCosts, 1.0);
            }
            
            return new CounterfactualConfig(
                featureBounds,
                immutableFeatures,
                featureCosts,
                maxIterations,
                tolerance
            );
        }
    }
    
    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Checks if a feature is mutable.
     */
    public boolean isMutable(int featureIndex) {
        return !immutableFeatures.contains(featureIndex);
    }
    
    /**
     * Gets bounds for a feature.
     */
    public double[] getBounds(int featureIndex) {
        return featureBounds[featureIndex];
    }
    
    /**
     * Gets cost for changing a feature.
     */
    public double getCost(int featureIndex) {
        return featureCosts[featureIndex];
    }
}


