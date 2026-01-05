package io.github.Thung0808.xai.advanced;

import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.experimental.Incubating;

import java.util.List;

/**
 * SPI for computing Partial Dependence Plots (PDPs).
 * 
 * <p>Partial dependence shows the marginal effect of a feature on model predictions,
 * averaging over the distribution of other features.</p>
 * 
 * <p><b>Algorithm:</b></p>
 * <ol>
 *   <li>Select a feature of interest</li>
 *   <li>Create a grid of feature values (e.g., percentiles or fixed intervals)</li>
 *   <li>For each grid value:
 *     <ul>
 *       <li>Set feature to grid value in all dataset instances</li>
 *       <li>Keep other features unchanged (at mean/median/actual values)</li>
 *       <li>Compute predictions for all instances</li>
 *       <li>Average predictions across dataset</li>
 *     </ul>
 *   </li>
 *   <li>Return grid points and averaged predictions</li>
 * </ol>
 * 
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li><b>Feature effects:</b> How does age affect loan approval probability?</li>
 *   <li><b>Non-linearity detection:</b> Is the relationship linear or curved?</li>
 *   <li><b>Saturating behavior:</b> Do diminishing returns apply?</li>
 *   <li><b>Fairness analysis:</b> Ensure protected attributes don't cause unfair outcomes</li>
 * </ul>
 * 
 * <p><b>Example Output:</b></p>
 * <pre>{@code
 * Feature: age
 * Grid points: [20, 30, 40, 50, 60, 70]
 * Predictions: [0.35, 0.52, 0.68, 0.72, 0.75, 0.76]
 * 
 * Interpretation: Loan approval increases with age, with diminishing returns after 50
 * }</pre>
 * 
 * <p><b>Advantages:</b></p>
 * <ul>
 *   <li>Model-agnostic (any black box)</li>
 *   <li>Easily visualized (2D plot)</li>
 *   <li>Accounts for feature correlation via averaging</li>
 * </ul>
 * 
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Assumes feature independence (may extrapolate to unrealistic regions)</li>
 *   <li>Hides feature interactions (use 2D PDP for interactions)</li>
 *   <li>Averaging masks heterogeneous effects</li>
 * </ul>
 * 
 * @since 0.5.0
 * @see InteractionPlotter
 */
@Incubating(
    since = "0.5.0",
    graduationTarget = "1.0.0",
    reason = "Grid strategy and feature preprocessing may be refined based on feedback"
)
public interface PartialDependencePlotter {
    
    /**
     * Computes partial dependence for a single feature.
     * 
     * <p>Creates a grid of feature values and computes the average prediction
     * when the feature is set to each grid value.</p>
     * 
     * @param model the predictive model
     * @param dataset the full dataset (for averaging over other features)
     * @param featureIdx the feature index to analyze
     * @return partial dependence result with grid points and predictions
     * @throws IllegalArgumentException if featureIdx out of range or dataset empty
     */
    PartialDependence computePDP(PredictiveModel model, List<double[]> dataset, int featureIdx);
    
    /**
     * Computes partial dependence with custom grid points.
     * 
     * @param model the predictive model
     * @param dataset the full dataset
     * @param featureIdx the feature index
     * @param gridPoints custom grid values (must be sorted)
     * @return partial dependence result
     */
    default PartialDependence computePDP(
            PredictiveModel model,
            List<double[]> dataset,
            int featureIdx,
            double[] gridPoints) {
        return computePDP(model, dataset, featureIdx);
    }
    
    /**
     * Returns the name of this PDP implementation.
     * 
     * @return implementation name (e.g., "StandardPDP", "KernelPDP")
     */
    String getName();
}
