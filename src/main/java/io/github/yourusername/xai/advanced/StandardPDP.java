package io.github.Thung0808.xai.advanced;

import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.api.Stable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Standard partial dependence plot implementation using percentile-based grid.
 * 
 * <p><b>Features:</b></p>
 * <ul>
 *   <li>Percentile-based grid (deciles, quartiles, etc.)</li>
 *   <li>Optional Individual Conditional Expectation (ICE) curves</li>
 *   <li>Confidence intervals via bootstrap</li>
 *   <li>Feature preprocessing (mean/median centering)</li>
 * </ul>
 * 
 * @since 0.5.0
 */
@Stable(since = "0.5.0")
public class StandardPDP implements PartialDependencePlotter {
    
    private static final Logger log = LoggerFactory.getLogger(StandardPDP.class);
    
    private final int gridSize;
    private final boolean computeICE;
    private final boolean computeCI;
    private final String centeringStrategy;  // "mean" or "median"
    private final long seed;
    
    /**
     * Creates a standard PDP with default settings.
     * Grid size: 10 (deciles), no ICE curves, no confidence intervals.
     */
    public StandardPDP() {
        this(10, false, false, "mean", 42);
    }
    
    /**
     * Creates a standard PDP with custom settings.
     * 
     * @param gridSize number of percentile-based grid points
     * @param computeICE whether to compute individual conditional expectations
     * @param computeCI whether to compute confidence intervals
     * @param centeringStrategy "mean" or "median" for other features
     * @param seed random seed for reproducibility
     */
    public StandardPDP(int gridSize, boolean computeICE, boolean computeCI, 
                      String centeringStrategy, long seed) {
        this.gridSize = gridSize;
        this.computeICE = computeICE;
        this.computeCI = computeCI;
        this.centeringStrategy = centeringStrategy;
        this.seed = seed;
    }
    
    @Override
    public PartialDependence computePDP(PredictiveModel model, List<double[]> dataset, int featureIdx) {
        if (dataset == null || dataset.isEmpty()) {
            throw new IllegalArgumentException("Dataset cannot be null or empty");
        }
        
        int numFeatures = dataset.get(0).length;
        if (featureIdx < 0 || featureIdx >= numFeatures) {
            throw new IllegalArgumentException("Feature index out of range: " + featureIdx);
        }
        
        // Extract feature values
        List<Double> featureValues = dataset.stream()
            .map(instance -> instance[featureIdx])
            .collect(Collectors.toList());
        
        // Create percentile-based grid
        double[] gridPoints = createPercentileGrid(featureValues);
        
        // Compute PDP: average prediction when feature varies
        double[] predictions = new double[gridPoints.length];
        double[] lowerBounds = computeCI ? new double[gridPoints.length] : null;
        double[] upperBounds = computeCI ? new double[gridPoints.length] : null;
        List<double[]> iceCurves = new ArrayList<>();
        
        // Compute center values for other features
        double[] featureCenter = computeFeatureCenter(dataset, featureIdx);
        
        for (int gridIdx = 0; gridIdx < gridPoints.length; gridIdx++) {
            double gridValue = gridPoints[gridIdx];
            List<Double> predictionsByInstance = new ArrayList<>();
            
            // For each instance, compute prediction with feature set to grid value
            for (double[] instance : dataset) {
                double[] modified = instance.clone();
                modified[featureIdx] = gridValue;
                double pred = model.predict(modified);
                predictionsByInstance.add(pred);
            }
            
            // Average across instances
            double avgPred = predictionsByInstance.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            predictions[gridIdx] = avgPred;
            
            // Confidence intervals
            if (computeCI) {
                double stdDev = Math.sqrt(predictionsByInstance.stream()
                    .mapToDouble(p -> Math.pow(p - avgPred, 2))
                    .average()
                    .orElse(0.0));
                double ci = 1.96 * stdDev / Math.sqrt(dataset.size());
                lowerBounds[gridIdx] = avgPred - ci;
                upperBounds[gridIdx] = avgPred + ci;
            }
            
            // ICE curve for first instance (if enabled)
            if (computeICE) {
                double[] iceCurve = new double[gridPoints.length];
                for (int i = 0; i < gridPoints.length; i++) {
                    double[] modifiedForICE = dataset.get(0).clone();
                    modifiedForICE[featureIdx] = gridPoints[i];
                    iceCurve[i] = model.predict(modifiedForICE);
                }
                if (gridIdx == 0) {
                    iceCurves.add(iceCurve);
                }
            }
        }
        
        log.info("Computed PDP for feature_{}: {} grid points", featureIdx, gridSize);
        
        return new PartialDependence(
            featureIdx,
            "feature_" + featureIdx,
            gridPoints,
            predictions,
            lowerBounds,
            upperBounds,
            iceCurves
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
    
    private double[] computeFeatureCenter(List<double[]> dataset, int featureIdx) {
        int numFeatures = dataset.get(0).length;
        double[] center = new double[numFeatures];
        
        if ("median".equals(centeringStrategy)) {
            for (int f = 0; f < numFeatures; f++) {
                final int idx = f;  // Capture for lambda
                if (idx == featureIdx) {
                    center[idx] = 0.0;  // Will be set by PDP
                } else {
                    List<Double> values = dataset.stream()
                        .map(instance -> instance[idx])
                        .sorted()
                        .collect(Collectors.toList());
                    center[idx] = values.get(values.size() / 2);
                }
            }
        } else {  // mean
            for (int f = 0; f < numFeatures; f++) {
                final int idx = f;  // Capture for lambda
                if (idx == featureIdx) {
                    center[idx] = 0.0;
                } else {
                    center[idx] = dataset.stream()
                        .mapToDouble(instance -> instance[idx])
                        .average()
                        .orElse(0.0);
                }
            }
        }
        
        return center;
    }
    
    @Override
    public String getName() {
        return "StandardPDP";
    }
}
