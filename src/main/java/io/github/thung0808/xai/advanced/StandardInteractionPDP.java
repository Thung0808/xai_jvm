package io.github.thung0808.xai.advanced;

import io.github.thung0808.xai.api.PredictiveModel;
import io.github.thung0808.xai.api.Stable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Standard 2-way partial dependence implementation.
 * 
 * <p>Computes interaction effects using percentile-based 2D grids.</p>
 * 
 * @since 0.5.0
 */
@Stable(since = "0.5.0")
public class StandardInteractionPDP implements InteractionPlotter {
    
    private static final Logger log = LoggerFactory.getLogger(StandardInteractionPDP.class);
    
    private final int gridSize;
    private final boolean includeMarginalPDPs;
    private final String centeringStrategy;
    
    /**
     * Creates a standard interaction plotter.
     * 
     * @param gridSize number of grid points per dimension
     * @param includeMarginalPDPs whether to compute and include marginal PDPs
     * @param centeringStrategy "mean" or "median"
     */
    public StandardInteractionPDP(int gridSize, boolean includeMarginalPDPs, String centeringStrategy) {
        this.gridSize = gridSize;
        this.includeMarginalPDPs = includeMarginalPDPs;
        this.centeringStrategy = centeringStrategy;
    }
    
    /**
     * Creates with default settings (grid=10, includes marginal PDPs, mean centering).
     */
    public StandardInteractionPDP() {
        this(10, true, "mean");
    }
    
    @Override
    public InteractionPlot computeInteraction(
            PredictiveModel model,
            List<double[]> dataset,
            int featureIdx1,
            int featureIdx2) {
        
        if (dataset == null || dataset.isEmpty()) {
            throw new IllegalArgumentException("Dataset cannot be null or empty");
        }
        
        int numFeatures = dataset.get(0).length;
        if (featureIdx1 < 0 || featureIdx1 >= numFeatures || 
            featureIdx2 < 0 || featureIdx2 >= numFeatures) {
            throw new IllegalArgumentException("Feature indices out of range");
        }
        
        if (featureIdx1 == featureIdx2) {
            throw new IllegalArgumentException("Feature indices must be different");
        }
        
        // Extract feature values
        List<Double> feature1Values = dataset.stream()
            .map(instance -> instance[featureIdx1])
            .collect(Collectors.toList());
        
        List<Double> feature2Values = dataset.stream()
            .map(instance -> instance[featureIdx2])
            .collect(Collectors.toList());
        
        // Create percentile grids
        double[] grid1 = createPercentileGrid(feature1Values);
        double[] grid2 = createPercentileGrid(feature2Values);
        
        // Compute 2D interaction grid
        double[][] predictions = new double[grid1.length][grid2.length];
        
        for (int i = 0; i < grid1.length; i++) {
            for (int j = 0; j < grid2.length; j++) {
                double avgPred = 0.0;
                
                // Average prediction across dataset with both features set
                for (double[] instance : dataset) {
                    double[] modified = instance.clone();
                    modified[featureIdx1] = grid1[i];
                    modified[featureIdx2] = grid2[j];
                    avgPred += model.predict(modified);
                }
                
                predictions[i][j] = avgPred / dataset.size();
            }
        }
        
        // Optionally compute marginal PDPs
        PartialDependence pdp1 = null;
        PartialDependence pdp2 = null;
        
        if (includeMarginalPDPs) {
            StandardPDP pdpComputer = new StandardPDP(gridSize, false, false, centeringStrategy, 42);
            pdp1 = pdpComputer.computePDP(model, dataset, featureIdx1);
            pdp2 = pdpComputer.computePDP(model, dataset, featureIdx2);
        }
        
        log.info("Computed interaction plot: feature_{} Ã— feature_{}, grid {}Ã—{}",
            featureIdx1, featureIdx2, gridSize, gridSize);
        
        return new InteractionPlot(
            featureIdx1,
            featureIdx2,
            "feature_" + featureIdx1,
            "feature_" + featureIdx2,
            grid1,
            grid2,
            predictions,
            pdp1,
            pdp2
        );
    }
    
    private double[] createPercentileGrid(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        
        double[] grid = new double[gridSize];
        for (int i = 0; i < gridSize; i++) {
            double percentile = (i + 1.0) / (gridSize + 1.0);
            int idx = (int) (percentile * sorted.size());
            idx = Math.max(0, Math.min(idx, sorted.size() - 1));
            grid[i] = sorted.get(idx);
        }
        
        return grid;
    }
    
    @Override
    public String getName() {
        return "StandardInteractionPDP";
    }
}


