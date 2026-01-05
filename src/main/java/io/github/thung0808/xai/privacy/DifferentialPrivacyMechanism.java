package io.github.thung0808.xai.privacy;

import io.github.thung0808.xai.api.Experimental;
import java.util.*;

/**
 * Differential Privacy mechanisms for XAI.
 * Protects privacy while generating explanations.
 * 
 * <p><b>Experimental:</b> This API is experimental and may change in future releases.
 * 
 * @since 1.1.0
 */
@Experimental(since = "1.1.0")
public class DifferentialPrivacyMechanism {
    private double epsilon;  // Privacy budget
    private double delta;    // Failure probability
    private PrivacyBudgetTracker budgetTracker;  // Budget enforcement
    
    public DifferentialPrivacyMechanism(double epsilon, double delta) {
        this.epsilon = epsilon;
        this.delta = delta;
        this.budgetTracker = new PrivacyBudgetTracker(epsilon, delta);
    }
    
    public DifferentialPrivacyMechanism(double epsilon, double delta, PrivacyBudgetTracker tracker) {
        this.epsilon = epsilon;
        this.delta = delta;
        this.budgetTracker = tracker;
    }
    
    /**
     * Laplace Mechanism: Add Laplace noise to feature attribution
     * Y = f(X) + Lap(0, Δf/ε) where Δf is sensitivity
     * 
     * @return noisy attributions, or null if budget exhausted
     */
    public double[] applyLaplaceNoise(double[] attributions) {
        // Check privacy budget
        if (!budgetTracker.requestBudget(epsilon * 0.1, delta * 0.1)) {
            return null;  // Budget exhausted - block explanation
        }
        
        double[] noisy = attributions.clone();
        double sensitivity = computeSensitivity(attributions);
        double scale = sensitivity / epsilon;
        
        Random rand = new Random();
        for (int i = 0; i < noisy.length; i++) {
            // Generate Laplace noise: -scale * ln(u) * sign(u-0.5)
            double u = rand.nextDouble();
            double laplacNoise = -scale * Math.log(u) * Math.signum(u - 0.5);
            noisy[i] += laplacNoise;
        }
        
        return noisy;
    }
    
    /**
     * Gaussian Mechanism: Add Gaussian noise
     * Better for composition and concentrated DP
     */
    public double[] applyGaussianNoise(double[] attributions) {
        // Check privacy budget
        if (!budgetTracker.requestBudget(epsilon * 0.15, delta * 0.05)) {
            return null;  // Budget exhausted
        }
        
        double[] noisy = attributions.clone();
        double sensitivity = computeSensitivity(attributions);
        double variance = (2 * sensitivity * sensitivity * Math.log(1.25 / delta)) / (epsilon * epsilon);
        double stdDev = Math.sqrt(variance);
        
        Random rand = new Random();
        for (int i = 0; i < noisy.length; i++) {
            noisy[i] += rand.nextGaussian() * stdDev;
        }
        
        return noisy;
    }
    
    /**
     * Compute sensitivity of attributions (max change with one data point change)
     */
    private double computeSensitivity(double[] attributions) {
        double max = 0;
        for (double attr : attributions) {
            max = Math.max(max, Math.abs(attr));
        }
        return max > 0 ? max : 1.0;
    }
    
    /**
     * Get privacy parameters (epsilon-delta)
     */
    public String getPrivacyGuarantee() {
        return String.format("(ε=%.4f, δ=%.2e)-differential privacy", epsilon, delta);
    }
    
    /**
     * Get remaining privacy budget
     */
    public double getRemainingEpsilon() {
        return budgetTracker.getRemainingEpsilon();
    }
    
    /**
     * Generate privacy audit report
     */
    public String generateAuditReport() {
        return budgetTracker.generateAuditReport();
    }
    
    @Override
    public String toString() {
        return "DifferentialPrivacy{" + getPrivacyGuarantee() + '}';
    }
}
