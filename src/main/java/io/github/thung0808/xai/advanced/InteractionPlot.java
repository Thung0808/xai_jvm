package io.github.thung0808.xai.advanced;

import io.github.thung0808.xai.api.Stable;

import java.util.*;

/**
 * Immutable result container for 2-way partial dependence (interaction analysis).
 * 
 * <p>Stores a 2D grid of predictions showing how two features interact to affect model output.</p>
 * 
 * @since 0.5.0
 */
@Stable(since = "0.5.0")
public class InteractionPlot {
    
    private final int feature1Idx;
    private final int feature2Idx;
    private final String feature1Name;
    private final String feature2Name;
    private final double[] grid1Points;
    private final double[] grid2Points;
    private final double[][] predictions;  // [grid1_idx][grid2_idx]
    private final PartialDependence pdp1;
    private final PartialDependence pdp2;
    
    /**
     * Creates an interaction plot result.
     * 
     * @param feature1Idx first feature index
     * @param feature2Idx second feature index
     * @param feature1Name first feature name
     * @param feature2Name second feature name
     * @param grid1Points grid values for feature 1
     * @param grid2Points grid values for feature 2
     * @param predictions 2D array of predictions [f1][f2]
     */
    public InteractionPlot(
            int feature1Idx,
            int feature2Idx,
            String feature1Name,
            String feature2Name,
            double[] grid1Points,
            double[] grid2Points,
            double[][] predictions) {
        this(feature1Idx, feature2Idx, feature1Name, feature2Name,
            grid1Points, grid2Points, predictions, null, null);
    }
    
    /**
     * Creates an interaction plot with marginal PDPs.
     * 
     * @param feature1Idx first feature index
     * @param feature2Idx second feature index
     * @param feature1Name first feature name
     * @param feature2Name second feature name
     * @param grid1Points grid values for feature 1
     * @param grid2Points grid values for feature 2
     * @param predictions 2D array of predictions
     * @param pdp1 marginal PDP for feature 1 (optional)
     * @param pdp2 marginal PDP for feature 2 (optional)
     */
    public InteractionPlot(
            int feature1Idx,
            int feature2Idx,
            String feature1Name,
            String feature2Name,
            double[] grid1Points,
            double[] grid2Points,
            double[][] predictions,
            PartialDependence pdp1,
            PartialDependence pdp2) {
        
        if (predictions.length != grid1Points.length) {
            throw new IllegalArgumentException("Grid1 must match predictions first dimension");
        }
        for (double[] row : predictions) {
            if (row.length != grid2Points.length) {
                throw new IllegalArgumentException("Grid2 must match predictions second dimension");
            }
        }
        
        this.feature1Idx = feature1Idx;
        this.feature2Idx = feature2Idx;
        this.feature1Name = feature1Name != null ? feature1Name : ("feature_" + feature1Idx);
        this.feature2Name = feature2Name != null ? feature2Name : ("feature_" + feature2Idx);
        this.grid1Points = grid1Points.clone();
        this.grid2Points = grid2Points.clone();
        this.predictions = new double[predictions.length][];
        for (int i = 0; i < predictions.length; i++) {
            this.predictions[i] = predictions[i].clone();
        }
        this.pdp1 = pdp1;
        this.pdp2 = pdp2;
    }
    
    /**
     * Returns the first feature index.
     */
    public int getFeature1Idx() {
        return feature1Idx;
    }
    
    /**
     * Returns the second feature index.
     */
    public int getFeature2Idx() {
        return feature2Idx;
    }
    
    /**
     * Returns the first feature name.
     */
    public String getFeature1Name() {
        return feature1Name;
    }
    
    /**
     * Returns the second feature name.
     */
    public String getFeature2Name() {
        return feature2Name;
    }
    
    /**
     * Returns grid points for feature 1.
     */
    public double[] getGrid1Points() {
        return grid1Points.clone();
    }
    
    /**
     * Returns grid points for feature 2.
     */
    public double[] getGrid2Points() {
        return grid2Points.clone();
    }
    
    /**
     * Returns the 2D prediction grid.
     * 
     * @return predictions[feature1_index][feature2_index]
     */
    public double[][] getPredictions() {
        double[][] copy = new double[predictions.length][];
        for (int i = 0; i < predictions.length; i++) {
            copy[i] = predictions[i].clone();
        }
        return copy;
    }
    
    /**
     * Returns marginal PDP for feature 1 (if available).
     */
    public Optional<PartialDependence> getPDP1() {
        return Optional.ofNullable(pdp1);
    }
    
    /**
     * Returns marginal PDP for feature 2 (if available).
     */
    public Optional<PartialDependence> getPDP2() {
        return Optional.ofNullable(pdp2);
    }
    
    /**
     * Computes the strength of interaction (Friedman H-statistic approximation).
     * 
     * <p>H = sqrt(sum((prediction[i][j] - predicted_additive[i][j])^2) / sum((prediction[i][j])^2))</p>
     * 
     * <p>Range: [0, 1] where 1 = complete interaction, 0 = additive</p>
     * 
     * @return interaction strength score
     */
    public double getInteractionStrength() {
        if (pdp1 == null || pdp2 == null) {
            return 0.0;  // Cannot compute without marginal PDPs
        }
        
        double[] pred1 = pdp1.getPredictions();
        double[] pred2 = pdp2.getPredictions();
        double baseline = Arrays.stream(pred1).average().orElse(0.0);
        
        double sumAdditiveError = 0.0;
        double sumTotal = 0.0;
        
        for (int i = 0; i < predictions.length; i++) {
            for (int j = 0; j < predictions[i].length; j++) {
                double additive = pred1[i] + pred2[j] - baseline;
                double interaction = predictions[i][j] - additive;
                
                sumAdditiveError += interaction * interaction;
                sumTotal += predictions[i][j] * predictions[i][j];
            }
        }
        
        if (sumTotal == 0.0) {
            return 0.0;
        }
        
        return Math.sqrt(sumAdditiveError / sumTotal);
    }
    
    /**
     * Detects whether true interaction exists (beyond random variation).
     * 
     * <p>Uses threshold: H > 0.1 indicates meaningful interaction</p>
     * 
     * @return true if interaction is significant
     */
    public boolean hasInteraction() {
        return getInteractionStrength() > 0.1;
    }
    
    /**
     * Returns the prediction range of the interaction grid.
     */
    public double[] getPredictionRange() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        
        for (double[] row : predictions) {
            for (double pred : row) {
                min = Math.min(min, pred);
                max = Math.max(max, pred);
            }
        }
        
        return new double[]{min, max};
    }
    
    /**
     * Computes synergy index: how much do the features amplify each other?
     * 
     * @return synergy score
     */
    public double getSynergy() {
        if (pdp1 == null || pdp2 == null) {
            return 0.0;
        }
        
        double[] pred1 = pdp1.getPredictions();
        double[] pred2 = pdp2.getPredictions();
        
        double sum1 = Arrays.stream(pred1).sum();
        double sum2 = Arrays.stream(pred2).sum();
        
        double sumInteraction = 0.0;
        for (double[] row : predictions) {
            for (double pred : row) {
                sumInteraction += pred;
            }
        }
        
        double expectedAdditiveSum = (sum1 / pred1.length) * (sum2 / pred2.length) * 
                                     predictions.length * predictions[0].length;
        
        if (expectedAdditiveSum == 0.0) {
            return 0.0;
        }
        
        return (sumInteraction - expectedAdditiveSum) / expectedAdditiveSum;
    }
    
    @Override
    public String toString() {
        return String.format(
            "InteractionPlot{feature1=%s, feature2=%s, grid1=%d, grid2=%d, interaction=%.3f}",
            feature1Name, feature2Name, grid1Points.length, grid2Points.length,
            getInteractionStrength()
        );
    }
}


