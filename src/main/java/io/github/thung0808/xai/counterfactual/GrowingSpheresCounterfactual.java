package io.github.thung0808.xai.counterfactual;

import io.github.thung0808.xai.api.PredictiveModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gradient-based counterfactual finder using Growing Spheres algorithm.
 * 
 * <p><b>Algorithm:</b> Iteratively perturb features in direction that
 * moves prediction toward target, respecting constraints and costs.</p>
 * 
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Respects feature bounds and immutability</li>
 *   <li>Minimizes weighted cost of changes</li>
 *   <li>Supports diverse counterfactual generation</li>
 * </ul>
 *
 * @since 0.3.0
 */
public class GrowingSpheresCounterfactual implements CounterfactualFinder {
    
    private static final Logger log = LoggerFactory.getLogger(GrowingSpheresCounterfactual.class);
    
    private final double stepSize;
    private final Random random;
    
    public GrowingSpheresCounterfactual(double stepSize, long seed) {
        this.stepSize = stepSize;
        this.random = new Random(seed);
    }
    
    public GrowingSpheresCounterfactual() {
        this(0.1, 42);
    }
    
    @Override
    public CounterfactualResult findCounterfactual(
        PredictiveModel model,
        double[] input,
        double targetPrediction,
        CounterfactualConfig config
    ) {
        double[] current = input.clone();
        double currentPrediction = model.predict(current);
        
        log.debug("Starting counterfactual search: current={}, target={}",
            currentPrediction, targetPrediction);
        
        List<CounterfactualResult.FeatureChange> changes = new ArrayList<>();
        double totalCost = 0.0;
        
        for (int iter = 0; iter < config.maxIterations(); iter++) {
            // Check convergence
            if (Math.abs(currentPrediction - targetPrediction) < config.tolerance()) {
                log.info("Counterfactual found in {} iterations", iter);
                return buildResult(input, current, model.predict(input), currentPrediction,
                    changes, totalCost, true, iter);
            }
            
            // Compute gradient (finite difference approximation)
            double[] gradient = computeGradient(model, current, config);
            
            // Update features toward target
            boolean changed = false;
            for (int i = 0; i < current.length; i++) {
                if (!config.isMutable(i)) {
                    continue; // Skip immutable features
                }
                
                double[] bounds = config.getBounds(i);
                double minVal = bounds[0];
                double maxVal = bounds[1];
                
                // Gradient direction (toward target)
                double direction = targetPrediction > currentPrediction ? 1.0 : -1.0;
                double delta = direction * gradient[i] * stepSize;
                
                double newValue = current[i] + delta;
                
                // Clip to bounds
                newValue = Math.max(minVal, Math.min(maxVal, newValue));
                
                if (Math.abs(newValue - current[i]) > 1e-6) {
                    double actualDelta = newValue - current[i];
                    double cost = Math.abs(actualDelta) * config.getCost(i);
                    
                    totalCost += cost;
                    current[i] = newValue;
                    changed = true;
                }
            }
            
            if (!changed) {
                log.warn("No mutable features can be changed further");
                break;
            }
            
            currentPrediction = model.predict(current);
        }
        
        // Didn't converge
        log.warn("Counterfactual search did not converge within {} iterations",
            config.maxIterations());
        
        // Compute final changes
        changes = computeChanges(input, current, config);
        
        return buildResult(input, current, model.predict(input), currentPrediction,
            changes, totalCost, false, config.maxIterations());
    }
    
    @Override
    public List<CounterfactualResult> findDiverseCounterfactuals(
        PredictiveModel model,
        double[] input,
        double targetPrediction,
        CounterfactualConfig config,
        int numCounterfactuals
    ) {
        List<CounterfactualResult> results = new ArrayList<>();
        
        // Generate diverse starting points with random perturbations
        for (int i = 0; i < numCounterfactuals; i++) {
            double[] perturbedInput = input.clone();
            
            // Add small random noise to encourage diversity
            for (int j = 0; j < perturbedInput.length; j++) {
                if (config.isMutable(j)) {
                    double[] bounds = config.getBounds(j);
                    double range = bounds[1] - bounds[0];
                    double noise = (random.nextDouble() - 0.5) * range * 0.1; // 10% noise
                    
                    perturbedInput[j] = Math.max(bounds[0],
                        Math.min(bounds[1], perturbedInput[j] + noise));
                }
            }
            
            CounterfactualResult result = findCounterfactual(
                model, perturbedInput, targetPrediction, config);
            
            if (result.isSuccess()) {
                results.add(result);
            }
        }
        
        log.info("Found {} diverse counterfactuals out of {} attempts",
            results.size(), numCounterfactuals);
        
        return results;
    }
    
    /**
     * Computes gradient using finite differences.
     */
    private double[] computeGradient(
        PredictiveModel model,
        double[] input,
        CounterfactualConfig config
    ) {
        double[] gradient = new double[input.length];
        double eps = 1e-4;
        
        double basePrediction = model.predict(input);
        
        for (int i = 0; i < input.length; i++) {
            if (!config.isMutable(i)) {
                gradient[i] = 0.0;
                continue;
            }
            
            double[] perturbed = input.clone();
            perturbed[i] += eps;
            
            // Clip to bounds
            double[] bounds = config.getBounds(i);
            perturbed[i] = Math.max(bounds[0], Math.min(bounds[1], perturbed[i]));
            
            double newPrediction = model.predict(perturbed);
            gradient[i] = Math.abs(newPrediction - basePrediction) / eps;
        }
        
        return gradient;
    }
    
    /**
     * Computes list of changes between original and counterfactual.
     */
    private List<CounterfactualResult.FeatureChange> computeChanges(
        double[] original,
        double[] counterfactual,
        CounterfactualConfig config
    ) {
        List<CounterfactualResult.FeatureChange> changes = new ArrayList<>();
        
        for (int i = 0; i < original.length; i++) {
            if (Math.abs(original[i] - counterfactual[i]) > 1e-6) {
                double delta = counterfactual[i] - original[i];
                double cost = Math.abs(delta) * config.getCost(i);
                
                changes.add(new CounterfactualResult.FeatureChange(
                    i, original[i], counterfactual[i], delta, cost
                ));
            }
        }
        
        return changes;
    }
    
    /**
     * Builds result object.
     */
    private CounterfactualResult buildResult(
        double[] original,
        double[] counterfactual,
        double originalPred,
        double counterfactualPred,
        List<CounterfactualResult.FeatureChange> changes,
        double totalCost,
        boolean success,
        int iterations
    ) {
        return CounterfactualResult.builder()
            .originalInput(original)
            .counterfactualInput(counterfactual)
            .originalPrediction(originalPred)
            .counterfactualPrediction(counterfactualPred)
            .totalCost(totalCost)
            .success(success)
            .iterations(iterations)
            .build();
    }
}


