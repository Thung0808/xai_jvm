package io.github.thung0808.xai.privacy;

import io.github.thung0808.xai.api.Experimental;
import java.time.*;
import java.util.*;

/**
 * Privacy Budget Tracker - Monitor epsilon consumption and enforce privacy guarantees.
 * Tracks cumulative epsilon consumption across all explanations and blocks queries
 * when privacy budget is exhausted to prevent reconstruction attacks.
 * 
 * <p><b>Experimental:</b> This API is experimental and may change in future releases.
 * 
 * <p>Reference: Dwork et al., "Differential Privacy: A Survey of Results"
 * 
 * @since 1.1.0
 */
@Experimental(since = "1.1.0")
public class PrivacyBudgetTracker {
    
    private final double totalEpsilonBudget;
    private final double totalDeltaBudget;
    private double consumedEpsilon;
    private double consumedDelta;
    private final List<BudgetConsumption> consumptionHistory;
    private final Map<String, Double> clientBudgets;
    private boolean budgetExhausted;
    
    /**
     * Initialize privacy budget tracker with epsilon and delta parameters.
     * @param totalEpsilonBudget Total epsilon budget (typically 0.5 - 2.0)
     * @param totalDeltaBudget Total delta budget (typically 1e-6)
     */
    public PrivacyBudgetTracker(double totalEpsilonBudget, double totalDeltaBudget) {
        if (totalEpsilonBudget <= 0 || totalDeltaBudget <= 0) {
            throw new IllegalArgumentException("Privacy budgets must be positive");
        }
        this.totalEpsilonBudget = totalEpsilonBudget;
        this.totalDeltaBudget = totalDeltaBudget;
        this.consumedEpsilon = 0.0;
        this.consumedDelta = 0.0;
        this.consumptionHistory = new ArrayList<>();
        this.clientBudgets = new HashMap<>();
        this.budgetExhausted = false;
    }
    
    /**
     * Request epsilon budget for an explanation query.
     * @param epsilon Epsilon cost for this query
     * @return true if budget available, false if budget exhausted
     */
    public synchronized boolean requestBudget(double epsilon, double delta) {
        if (budgetExhausted) {
            return false;
        }
        
        // Check if request exceeds remaining budget
        double remainingEpsilon = totalEpsilonBudget - consumedEpsilon;
        double remainingDelta = totalDeltaBudget - consumedDelta;
        
        if (epsilon > remainingEpsilon || delta > remainingDelta) {
            budgetExhausted = true;
            return false;
        }
        
        // Record consumption
        consumedEpsilon += epsilon;
        consumedDelta += delta;
        
        BudgetConsumption consumption = new BudgetConsumption(
            epsilon, delta, consumedEpsilon, consumedDelta, LocalDateTime.now()
        );
        consumptionHistory.add(consumption);
        
        return true;
    }
    
    /**
     * Register federated client with individual privacy budget.
     * @param clientId Client identifier
     * @param epsilonBudget Epsilon budget for this client
     */
    public synchronized void registerClient(String clientId, double epsilonBudget) {
        clientBudgets.put(clientId, epsilonBudget);
    }
    
    /**
     * Check if explanation can proceed for client with federated privacy.
     * @param clientId Client identifier
     * @param epsilonCost Epsilon cost for this explanation
     * @return true if client has remaining budget
     */
    public synchronized boolean checkClientBudget(String clientId, double epsilonCost) {
        if (!clientBudgets.containsKey(clientId)) {
            return false;
        }
        
        double remaining = clientBudgets.get(clientId);
        if (epsilonCost > remaining) {
            return false;
        }
        
        clientBudgets.put(clientId, remaining - epsilonCost);
        return true;
    }
    
    /**
     * Get remaining epsilon budget.
     * @return Remaining epsilon
     */
    public synchronized double getRemainingEpsilon() {
        return Math.max(0, totalEpsilonBudget - consumedEpsilon);
    }
    
    /**
     * Get remaining delta budget.
     * @return Remaining delta
     */
    public synchronized double getRemainingDelta() {
        return Math.max(0, totalDeltaBudget - consumedDelta);
    }
    
    /**
     * Get privacy consumption percentage.
     * @return Percentage (0-100) of budget consumed
     */
    public synchronized double getConsumptionPercentage() {
        return (consumedEpsilon / totalEpsilonBudget) * 100;
    }
    
    /**
     * Check if budget is exhausted.
     * @return true if no more queries allowed
     */
    public synchronized boolean isBudgetExhausted() {
        return budgetExhausted || (consumedEpsilon >= totalEpsilonBudget);
    }
    
    /**
     * Reset budget (use with caution - should only happen at well-defined periods).
     */
    public synchronized void resetBudget() {
        consumedEpsilon = 0.0;
        consumedDelta = 0.0;
        budgetExhausted = false;
        consumptionHistory.clear();
    }
    
    /**
     * Generate privacy audit report.
     * @return Formatted report string
     */
    public synchronized String generateAuditReport() {
        StringBuilder report = new StringBuilder();
        report.append("╔════════════════════════════════════════════════════════╗\n");
        report.append("║         PRIVACY BUDGET AUDIT REPORT                    ║\n");
        report.append("╚════════════════════════════════════════════════════════╝\n\n");
        
        report.append(String.format("Total Epsilon Budget: %.4f%n", totalEpsilonBudget));
        report.append(String.format("Consumed Epsilon:    %.4f%n", consumedEpsilon));
        report.append(String.format("Remaining Epsilon:   %.4f%n", getRemainingEpsilon()));
        report.append(String.format("Consumption:         %.1f%%%n\n", getConsumptionPercentage()));
        
        report.append(String.format("Total Delta Budget:  %.2e%n", totalDeltaBudget));
        report.append(String.format("Consumed Delta:      %.2e%n", consumedDelta));
        report.append(String.format("Remaining Delta:     %.2e%n\n", getRemainingDelta()));
        
        report.append(String.format("Status:              %s%n\n", 
            budgetExhausted ? "⚠️  BUDGET EXHAUSTED" : "✓ ACTIVE"));
        
        // Consumption history
        report.append("Consumption History:\n");
        report.append("─".repeat(54)).append("\n");
        
        for (int i = 0; i < consumptionHistory.size(); i++) {
            BudgetConsumption c = consumptionHistory.get(i);
            report.append(String.format("%2d. ε=%.4f, δ=%.2e | Cumulative: ε=%.4f, δ=%.2e | %s%n",
                i + 1, c.epsilon, c.delta, c.cumulativeEpsilon, c.cumulativeDelta, c.timestamp));
        }
        
        // Reconstruction attack risk assessment
        report.append("\n").append("─".repeat(54)).append("\n");
        report.append("Reconstruction Attack Risk:\n");
        double reconstructionRisk = computeReconstructionRisk();
        report.append(String.format("  Risk Score: %.2f%%%n", reconstructionRisk));
        
        if (reconstructionRisk > 80) {
            report.append("  ⚠️  CRITICAL: High risk of membership inference attack\n");
        } else if (reconstructionRisk > 50) {
            report.append("  ⚠️  WARNING: Moderate risk of data leakage\n");
        } else {
            report.append("  ✓ LOW: Privacy protections effective\n");
        }
        
        return report.toString();
    }
    
    /**
     * Estimate reconstruction attack risk based on epsilon consumption.
     * Higher epsilon = easier to reconstruct individual records.
     * Reference: Kairouz et al., "The Secret Sharer: Measuring Unintended 
     * Neural Network Memorization & Extracting Secrets"
     */
    private double computeReconstructionRisk() {
        if (consumedEpsilon == 0) return 0;
        
        // Heuristic: risk increases with epsilon consumption
        // Risk = min(100, epsilon * 100 / (1 + epsilon))
        return Math.min(100, (consumedEpsilon * 100) / (1 + consumedEpsilon));
    }
    
    /**
     * Record of individual budget consumption event.
     */
    public static class BudgetConsumption {
        public final double epsilon;
        public final double delta;
        public final double cumulativeEpsilon;
        public final double cumulativeDelta;
        public final LocalDateTime timestamp;
        
        public BudgetConsumption(double epsilon, double delta, 
                                double cumulativeEpsilon, double cumulativeDelta,
                                LocalDateTime timestamp) {
            this.epsilon = epsilon;
            this.delta = delta;
            this.cumulativeEpsilon = cumulativeEpsilon;
            this.cumulativeDelta = cumulativeDelta;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Composition theorem: Estimate epsilon after multiple queries.
     * Basic composition: ε_total = ε₁ + ε₂ + ... + εₙ
     * @param queryCount Number of queries made
     * @return Total epsilon consumed
     */
    public static double basicComposition(double epsilonPerQuery, int queryCount) {
        return epsilonPerQuery * queryCount;
    }
    
    /**
     * Advanced composition (Dwork, Rothblum, Vadhan).
     * More efficient: ε_total ≈ ε₀ + √(2 * queryCount * ln(1/δ) * ε₀)
     * @param epsilon0 Base epsilon per query
     * @param delta Privacy parameter
     * @param queryCount Number of queries
     * @return Total epsilon with advanced composition
     */
    public static double advancedComposition(double epsilon0, double delta, int queryCount) {
        return epsilon0 + Math.sqrt(2 * queryCount * Math.log(1.0 / delta)) * epsilon0;
    }
}
