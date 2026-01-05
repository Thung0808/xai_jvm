package io.github.Thung0808.xai.advanced;

import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.experimental.Incubating;

import java.util.List;

/**
 * SPI for computing 2-way Partial Dependence (interaction effects).
 * 
 * <p>Analyzes how two features interact to affect model predictions.
 * Unlike marginal PDPs, 2-way PDPs show non-additive effects.</p>
 * 
 * <p><b>Algorithm:</b></p>
 * <ol>
 *   <li>Select two features (f1, f2)</li>
 *   <li>Create 2D grid: f1 ∈ {v1, v2, ..., vn}, f2 ∈ {u1, u2, ..., um}</li>
 *   <li>For each (f1=vi, f2=uj):
 *     <ul>
 *       <li>Set both features to grid values in all instances</li>
 *       <li>Keep other features at mean/median</li>
 *       <li>Compute predictions</li>
 *       <li>Average across dataset</li>
 *     </ul>
 *   </li>
 *   <li>Return n×m heatmap of averaged predictions</li>
 * </ol>
 * 
 * <p><b>Interaction Detection:</b></p>
 * <p>If PDP(f1=v, f2=u) ≠ PDP(f1=v) + PDP(f2=u) - baseline, then interaction exists.</p>
 * 
 * <p><b>Example:</b></p>
 * <p>Age × Income interaction in loan approval:
 * - Young + Low Income: 5% approval
 * - Young + High Income: 40% approval
 * - Old + Low Income: 25% approval
 * - Old + High Income: 80% approval
 * 
 * Non-additive effect: Young people benefit more from high income than old people.
 * </p>
 * 
 * @since 0.5.0
 * @see PartialDependencePlotter
 */
@Incubating(
    since = "0.5.0",
    graduationTarget = "1.0.0",
    reason = "Interaction metrics may be refined based on use cases"
)
public interface InteractionPlotter {
    
    /**
     * Computes 2-way partial dependence (interaction plot).
     * 
     * @param model the predictive model
     * @param dataset the full dataset
     * @param featureIdx1 first feature index
     * @param featureIdx2 second feature index
     * @return interaction result with 2D grid of predictions
     * @throws IllegalArgumentException if feature indices invalid or out of range
     */
    InteractionPlot computeInteraction(
        PredictiveModel model,
        List<double[]> dataset,
        int featureIdx1,
        int featureIdx2);
    
    /**
     * Computes interaction with custom grid sizes.
     * 
     * @param model the predictive model
     * @param dataset the full dataset
     * @param featureIdx1 first feature index
     * @param featureIdx2 second feature index
     * @param gridSize1 number of grid points for feature 1
     * @param gridSize2 number of grid points for feature 2
     * @return interaction result
     */
    default InteractionPlot computeInteraction(
        PredictiveModel model,
        List<double[]> dataset,
        int featureIdx1,
        int featureIdx2,
        int gridSize1,
        int gridSize2) {
        return computeInteraction(model, dataset, featureIdx1, featureIdx2);
    }
    
    /**
     * Returns the name of this interaction plotter.
     */
    String getName();
}
