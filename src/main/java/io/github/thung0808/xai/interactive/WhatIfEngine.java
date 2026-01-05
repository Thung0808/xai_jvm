package io.github.thung0808.xai.interactive;

import io.github.thung0808.xai.api.PredictiveModel;
import io.github.thung0808.xai.api.Experimental;
import java.util.*;

/**
 * Interactive What-If Simulation Engine.
 * Provides real-time counterfactual predictions.
 * 
 * <p><b>Experimental:</b> This API is experimental and may change in future releases.
 * 
 * @since 1.1.0
 */
@Experimental(since = "1.1.0")
class WhatIfSimulationEngine {
    private final PredictiveModel model;
    private final double[] baselineFeatures;
    private final String[] featureNames;
    private final double[] featureSensitivity;  // Local gradient estimates
    
    public WhatIfSimulationEngine(PredictiveModel model, double[] baseline, String[] names) {
        this.model = model;
        this.baselineFeatures = baseline.clone();
        this.featureNames = names;
        this.featureSensitivity = new double[baseline.length];
        computeLocalSensitivity();
    }
    
    /**
     * Compute local sensitivity analysis (first-order derivatives)
     * Enables fast what-if predictions
     */
    private void computeLocalSensitivity() {
        double h = 0.0001;  // Small perturbation
        double baselinePred = model.predict(baselineFeatures);
        
        for (int i = 0; i < baselineFeatures.length; i++) {
            double[] perturbed = baselineFeatures.clone();
            perturbed[i] += h;
            double perturbedPred = model.predict(perturbed);
            
            // Gradient approximation: f'(x) ≈ (f(x+h) - f(x)) / h
            featureSensitivity[i] = (perturbedPred - baselinePred) / h;
        }
    }
    
    /**
     * Fast what-if prediction using linear approximation
     * y_new ≈ y_old + f'(x) * Δx
     */
    public double predictAfterChanges(int featureIdx, double newValue) {
        double oldValue = baselineFeatures[featureIdx];
        double delta = newValue - oldValue;
        double baselinePred = model.predict(baselineFeatures);
        
        // Linear approximation
        return baselinePred + featureSensitivity[featureIdx] * delta;
    }
    
    /**
     * Accurate what-if prediction (recompute model)
     */
    public double predictAccurate(int featureIdx, double newValue) {
        double[] modified = baselineFeatures.clone();
        modified[featureIdx] = newValue;
        return model.predict(modified);
    }
    
    /**
     * Batch what-if: explore multiple feature changes
     */
    public WhatIfResult whatIf(Map<Integer, Double> featureChanges) {
        double[] scenario = baselineFeatures.clone();
        
        for (Map.Entry<Integer, Double> change : featureChanges.entrySet()) {
            scenario[change.getKey()] = change.getValue();
        }
        
        double oldPred = model.predict(baselineFeatures);
        double newPred = model.predict(scenario);
        double delta = newPred - oldPred;
        
        return new WhatIfResult(scenario, oldPred, newPred, delta);
    }
    
    /**
     * Find feature values to reach target prediction
     * Uses iterative optimization
     */
    public double[] findValuesForTarget(double targetPred, int iterations) {
        double[] current = baselineFeatures.clone();
        double learningRate = 0.01;
        
        for (int iter = 0; iter < iterations; iter++) {
            double currentPred = model.predict(current);
            double error = targetPred - currentPred;
            
            // Gradient descent step
            for (int i = 0; i < current.length; i++) {
                current[i] += learningRate * featureSensitivity[i] * error;
            }
        }
        
        return current;
    }
    
    /**
     * Export what-if results for streaming to frontend
     */
    public String toStreamingJSON(Map<Integer, Double> changes) {
        WhatIfResult result = whatIf(changes);
        StringBuilder json = new StringBuilder();
        
        json.append("{")
            .append("\"baseline\": ").append(result.baselinePrediction).append(", ")
            .append("\"predicted\": ").append(result.newPrediction).append(", ")
            .append("\"delta\": ").append(result.delta).append(", ")
            .append("\"changes\": {");
        
        boolean first = true;
        for (Map.Entry<Integer, Double> change : changes.entrySet()) {
            if (!first) json.append(", ");
            json.append("\"").append(featureNames[change.getKey()]).append("\": ")
                .append(change.getValue());
            first = false;
        }
        
        json.append("}}");
        return json.toString();
    }
    
    /**
     * What-If Result
     */
    public static class WhatIfResult {
        public double[] scenario;
        public double baselinePrediction;
        public double newPrediction;
        public double delta;
        
        public WhatIfResult(double[] scenario, double baseline, double newPred, double delta) {
            this.scenario = scenario;
            this.baselinePrediction = baseline;
            this.newPrediction = newPred;
            this.delta = delta;
        }
        
        @Override
        public String toString() {
            return String.format("WhatIf{baseline=%.2f → new=%.2f (Δ=%.2f)}", 
                baselinePrediction, newPrediction, delta);
        }
    }
    
    public String[] getFeatureNames() {
        return featureNames;
    }
    
    public double[] getSensitivity() {
        return featureSensitivity.clone();
    }
}

/**
 * Surrogate Model for faster what-if predictions
 * Uses polynomial regression to approximate model locally
 */
class SurrogateModel {
    private final double[] center;
    private final double[] coefficients;
    
    public SurrogateModel(double[] center, double[] coefficients) {
        this.center = center;
        this.coefficients = coefficients;
    }
    
    /**
     * Predict using surrogate (polynomial model)
     */
    public double predict(double[] features) {
        double result = coefficients[0];  // Intercept
        
        // Linear terms
        for (int i = 0; i < features.length && i + 1 < coefficients.length; i++) {
            result += coefficients[i + 1] * (features[i] - center[i]);
        }
        
        return result;
    }
    
    /**
     * Fit surrogate to samples
     */
    public static SurrogateModel fitSurrogate(double[][] samples, double[] targets) {
        // Simple: fit linear model via normal equations
        // In production, use ridge regression or other regularization
        
        int n = samples[0].length;
        double[] center = new double[n];
        for (int i = 0; i < n; i++) {
            for (double[] sample : samples) {
                center[i] += sample[i];
            }
            center[i] /= samples.length;
        }
        
        // Placeholder coefficients
        double[] coeffs = new double[n + 1];
        coeffs[0] = Arrays.stream(targets).average().orElse(0);
        
        return new SurrogateModel(center, coeffs);
    }
}
