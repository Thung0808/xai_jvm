package io.github.thung0808.xai.privacy;

import io.github.thung0808.xai.api.Experimental;
import java.util.*;

/**
 * Federated Explanation Aggregator.
 * Aggregates explanations from multiple devices without seeing raw data.
 * Integrates with PrivacyBudgetTracker for federated privacy-preserving learning.
 * 
 * <p><b>Experimental:</b> This API is experimental and may change in future releases.
 * 
 * @since 1.1.0
 */
@Experimental(since = "1.1.0")
public class FederatedExplanationAggregator {
    private List<double[]> clientExplanations;
    private List<Double> clientWeights;
    private PrivacyBudgetTracker budgetTracker;
    
    public FederatedExplanationAggregator() {
        this.clientExplanations = new ArrayList<>();
        this.clientWeights = new ArrayList<>();
        this.budgetTracker = new PrivacyBudgetTracker(1.0, 1e-6);
    }
    
    public FederatedExplanationAggregator(PrivacyBudgetTracker tracker) {
        this.clientExplanations = new ArrayList<>();
        this.clientWeights = new ArrayList<>();
        this.budgetTracker = tracker;
    }
    
    /**
     * Register client explanation vector with privacy budget check
     * @return true if budget available, false if exhausted
     */
    public synchronized boolean registerClientExplanation(double[] explanation, double weight) {
        // Federated queries consume minimal privacy budget
        if (!budgetTracker.requestBudget(0.05, 1e-8)) {
            return false;  // Budget exhausted - reject explanation
        }
        
        clientExplanations.add(explanation);
        clientWeights.add(weight);
        return true;
    }
    
    /**
     * Aggregate explanations from all clients
     * Weighted averaging: E_agg = Σ(w_i * E_i) / Σ(w_i)
     */
    public double[] aggregateExplanations() {
        if (clientExplanations.isEmpty()) {
            throw new IllegalStateException("No client explanations registered");
        }
        
        int featureDim = clientExplanations.get(0).length;
        double[] aggregated = new double[featureDim];
        double totalWeight = 0;
        
        for (int i = 0; i < clientExplanations.size(); i++) {
            double[] explanation = clientExplanations.get(i);
            double weight = clientWeights.get(i);
            
            for (int j = 0; j < featureDim; j++) {
                aggregated[j] += explanation[j] * weight;
            }
            totalWeight += weight;
        }
        
        // Normalize by total weight
        for (int j = 0; j < featureDim; j++) {
            aggregated[j] /= totalWeight;
        }
        
        return aggregated;
    }
    
    /**
     * Get aggregation statistics
     */
    public Map<String, Object> getAggregationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("num_clients", clientExplanations.size());
        double totalWeight = clientWeights.stream().mapToDouble(Double::doubleValue).sum();
        stats.put("total_weight", totalWeight);
        stats.put("avg_weight", totalWeight / Math.max(1, clientExplanations.size()));
        
        // Variance calculation
        double mean = stats.get("avg_weight") instanceof Double ? (Double) stats.get("avg_weight") : 0;
        double variance = 0;
        for (double w : clientWeights) {
            variance += Math.pow(w - mean, 2);
        }
        variance /= Math.max(1, clientWeights.size());
        stats.put("variance", variance);
        
        return stats;
    }
    
    /**
     * Get privacy budget tracker
     */
    public PrivacyBudgetTracker getBudgetTracker() {
        return budgetTracker;
    }
    
    /**
     * Check if federated aggregation can continue
     */
    public boolean canContinueAggregation() {
        return !budgetTracker.isBudgetExhausted();
    }
}
