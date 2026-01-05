package io.github.Thung0808.xai.advanced;

import io.github.Thung0808.xai.api.Stable;

import java.util.*;

/**
 * Immutable result container for partial dependence computation.
 * 
 * <p>Stores feature grid points and corresponding average model predictions,
 * along with optional Individual Conditional Expectation (ICE) curves.</p>
 * 
 * @since 0.5.0
 */
@Stable(since = "0.5.0")
public class PartialDependence {
    
    private final int featureIdx;
    private final String featureName;
    private final double[] gridPoints;
    private final double[] predictions;
    private final double[] lowerBound;
    private final double[] upperBound;
    private final List<double[]> iceCurves;
    
    /**
     * Creates a partial dependence result.
     * 
     * @param featureIdx the feature index
     * @param featureName the feature name (optional)
     * @param gridPoints sorted array of feature values
     * @param predictions average predictions at each grid point
     */
    public PartialDependence(
            int featureIdx,
            String featureName,
            double[] gridPoints,
            double[] predictions) {
        this(featureIdx, featureName, gridPoints, predictions, null, null, new ArrayList<>());
    }
    
    /**
     * Creates a partial dependence with confidence intervals.
     * 
     * @param featureIdx the feature index
     * @param featureName the feature name
     * @param gridPoints sorted array of feature values
     * @param predictions average predictions
     * @param lowerBound lower 95% confidence bound
     * @param upperBound upper 95% confidence bound
     */
    public PartialDependence(
            int featureIdx,
            String featureName,
            double[] gridPoints,
            double[] predictions,
            double[] lowerBound,
            double[] upperBound) {
        this(featureIdx, featureName, gridPoints, predictions, lowerBound, upperBound, new ArrayList<>());
    }
    
    /**
     * Creates a partial dependence with ICE curves.
     * 
     * @param featureIdx the feature index
     * @param featureName the feature name
     * @param gridPoints sorted array of feature values
     * @param predictions average predictions
     * @param lowerBound lower confidence bound (nullable)
     * @param upperBound upper confidence bound (nullable)
     * @param iceCurves individual conditional expectation curves (one per dataset instance)
     */
    public PartialDependence(
            int featureIdx,
            String featureName,
            double[] gridPoints,
            double[] predictions,
            double[] lowerBound,
            double[] upperBound,
            List<double[]> iceCurves) {
        if (gridPoints.length != predictions.length) {
            throw new IllegalArgumentException("Grid points and predictions must have same length");
        }
        if (lowerBound != null && lowerBound.length != predictions.length) {
            throw new IllegalArgumentException("Lower bound must match predictions length");
        }
        if (upperBound != null && upperBound.length != predictions.length) {
            throw new IllegalArgumentException("Upper bound must match predictions length");
        }
        
        this.featureIdx = featureIdx;
        this.featureName = featureName != null ? featureName : ("feature_" + featureIdx);
        this.gridPoints = gridPoints.clone();
        this.predictions = predictions.clone();
        this.lowerBound = lowerBound != null ? lowerBound.clone() : null;
        this.upperBound = upperBound != null ? upperBound.clone() : null;
        this.iceCurves = List.copyOf(iceCurves);
    }
    
    /**
     * Returns the feature index.
     */
    public int getFeatureIdx() {
        return featureIdx;
    }
    
    /**
     * Returns the feature name.
     */
    public String getFeatureName() {
        return featureName;
    }
    
    /**
     * Returns the grid points (feature values).
     */
    public double[] getGridPoints() {
        return gridPoints.clone();
    }
    
    /**
     * Returns the average predictions at each grid point.
     */
    public double[] getPredictions() {
        return predictions.clone();
    }
    
    /**
     * Returns the lower confidence bound (if available).
     */
    public Optional<double[]> getLowerBound() {
        return Optional.ofNullable(lowerBound != null ? lowerBound.clone() : null);
    }
    
    /**
     * Returns the upper confidence bound (if available).
     */
    public Optional<double[]> getUpperBound() {
        return Optional.ofNullable(upperBound != null ? upperBound.clone() : null);
    }
    
    /**
     * Returns individual conditional expectation curves.
     * 
     * @return list of curves, one per dataset instance
     */
    public List<double[]> getICECurves() {
        return new ArrayList<>(iceCurves);
    }
    
    /**
     * Returns whether confidence intervals are available.
     */
    public boolean hasConfidenceBounds() {
        return lowerBound != null && upperBound != null;
    }
    
    /**
     * Returns the range of grid points.
     */
    public double[] getGridRange() {
        return new double[]{gridPoints[0], gridPoints[gridPoints.length - 1]};
    }
    
    /**
     * Returns the range of predictions.
     */
    public double[] getPredictionRange() {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double p : predictions) {
            min = Math.min(min, p);
            max = Math.max(max, p);
        }
        return new double[]{min, max};
    }
    
    /**
     * Computes the slope (rate of change) across the PDP.
     * 
     * @return slope in prediction per feature unit
     */
    public double getSlope() {
        double[] range = getPredictionRange();
        double[] gridRange = getGridRange();
        return (range[1] - range[0]) / (gridRange[1] - gridRange[0]);
    }
    
    /**
     * Detects monotonicity of the partial dependence.
     * 
     * @return 1 if increasing, -1 if decreasing, 0 if non-monotonic
     */
    public int getMonotonicity() {
        int increasing = 0;
        int decreasing = 0;
        
        for (int i = 1; i < predictions.length; i++) {
            if (predictions[i] > predictions[i - 1]) {
                increasing++;
            } else if (predictions[i] < predictions[i - 1]) {
                decreasing++;
            }
        }
        
        if (increasing > decreasing * 2) {
            return 1;
        } else if (decreasing > increasing * 2) {
            return -1;
        }
        return 0;
    }
    
    /**
     * Computes heterogeneity (variance across ICE curves).
     * 
     * @return standard deviation of curve variance
     */
    public double getHeterogeneity() {
        if (iceCurves.isEmpty()) {
            return 0.0;
        }
        
        List<Double> variances = new ArrayList<>();
        
        for (double[] curve : iceCurves) {
            double mean = 0.0;
            for (double val : curve) {
                mean += val;
            }
            mean /= curve.length;
            
            double variance = 0.0;
            for (double val : curve) {
                variance += Math.pow(val - mean, 2);
            }
            variance /= curve.length;
            
            variances.add(variance);
        }
        
        double meanVar = variances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double sumSqDiff = variances.stream()
            .mapToDouble(v -> Math.pow(v - meanVar, 2))
            .sum();
        
        return Math.sqrt(sumSqDiff / variances.size());
    }
    
    @Override
    public String toString() {
        return String.format(
            "PartialDependence{feature=%s, gridPoints=%d, slope=%.4f, monotonic=%d}",
            featureName, gridPoints.length, getSlope(), getMonotonicity()
        );
    }
}
