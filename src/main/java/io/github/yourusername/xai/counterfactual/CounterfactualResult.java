package io.github.Thung0808.xai.counterfactual;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of counterfactual search.
 * 
 * <p>Contains the modified input that achieves target prediction,
 * along with metadata about which features were changed and by how much.</p>
 *
 * @since 0.3.0
 */
public class CounterfactualResult {
    
    private final double[] originalInput;
    private final double[] counterfactualInput;
    private final double originalPrediction;
    private final double counterfactualPrediction;
    private final List<FeatureChange> changes;
    private final double totalCost;
    private final boolean success;
    private final int iterations;
    
    private CounterfactualResult(Builder builder) {
        this.originalInput = builder.originalInput;
        this.counterfactualInput = builder.counterfactualInput;
        this.originalPrediction = builder.originalPrediction;
        this.counterfactualPrediction = builder.counterfactualPrediction;
        this.changes = List.copyOf(builder.changes);
        this.totalCost = builder.totalCost;
        this.success = builder.success;
        this.iterations = builder.iterations;
    }
    
    /**
     * Gets original input.
     */
    public double[] getOriginalInput() {
        return originalInput.clone();
    }
    
    /**
     * Gets counterfactual input.
     */
    public double[] getCounterfactualInput() {
        return counterfactualInput.clone();
    }
    
    /**
     * Gets original prediction.
     */
    public double getOriginalPrediction() {
        return originalPrediction;
    }
    
    /**
     * Gets counterfactual prediction.
     */
    public double getCounterfactualPrediction() {
        return counterfactualPrediction;
    }
    
    /**
     * Gets list of feature changes.
     */
    public List<FeatureChange> getChanges() {
        return changes;
    }
    
    /**
     * Gets total cost of changes.
     */
    public double getTotalCost() {
        return totalCost;
    }
    
    /**
     * Whether counterfactual was successfully found.
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Number of iterations used.
     */
    public int getIterations() {
        return iterations;
    }
    
    /**
     * Formats as human-readable recommendation.
     */
    public String toRecommendation(String[] featureNames) {
        if (!success) {
            return "No counterfactual found within constraints.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("To change prediction from ")
          .append(String.format("%.3f", originalPrediction))
          .append(" to ")
          .append(String.format("%.3f", counterfactualPrediction))
          .append(":\n\n");
        
        for (FeatureChange change : changes) {
            String name = featureNames[change.featureIndex()];
            sb.append("  • Change ").append(name)
              .append(" from ").append(String.format("%.2f", change.oldValue()))
              .append(" to ").append(String.format("%.2f", change.newValue()))
              .append(" (delta: ").append(String.format("%+.2f", change.delta()))
              .append(", cost: ").append(String.format("%.2f", change.cost()))
              .append(")\n");
        }
        
        sb.append("\nTotal cost: ").append(String.format("%.2f", totalCost));
        sb.append(" | Iterations: ").append(iterations);
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("CounterfactualResult{success=%s, changes=%d, cost=%.2f, iterations=%d}",
            success, changes.size(), totalCost, iterations);
    }
    
    /**
     * Builder for CounterfactualResult.
     */
    public static class Builder {
        private double[] originalInput;
        private double[] counterfactualInput;
        private double originalPrediction;
        private double counterfactualPrediction;
        private List<FeatureChange> changes = new ArrayList<>();
        private double totalCost;
        private boolean success;
        private int iterations;
        
        public Builder originalInput(double[] input) {
            this.originalInput = input;
            return this;
        }
        
        public Builder counterfactualInput(double[] input) {
            this.counterfactualInput = input;
            return this;
        }
        
        public Builder originalPrediction(double prediction) {
            this.originalPrediction = prediction;
            return this;
        }
        
        public Builder counterfactualPrediction(double prediction) {
            this.counterfactualPrediction = prediction;
            return this;
        }
        
        public Builder addChange(FeatureChange change) {
            this.changes.add(change);
            return this;
        }
        
        public Builder totalCost(double cost) {
            this.totalCost = cost;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder iterations(int iterations) {
            this.iterations = iterations;
            return this;
        }
        
        public CounterfactualResult build() {
            return new CounterfactualResult(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Represents a change to a single feature.
     */
    public record FeatureChange(
        int featureIndex,
        double oldValue,
        double newValue,
        double delta,
        double cost
    ) {}
}
