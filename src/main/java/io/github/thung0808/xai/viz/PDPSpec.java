package io.github.thung0808.xai.viz;

import io.github.thung0808.xai.advanced.PartialDependence;
import io.github.thung0808.xai.api.Stable;

import java.util.*;

/**
 * Partial Dependence Plot visualization specification.
 * 
 * <p><b>Framework-Agnostic Design:</b></p>
 * <p>Outputs pure data structure for rendering by:</p>
 * <ul>
 *   <li>Plotly: <code>plotly.graph_objects.Scatter(x=values, y=predictions)</code></li>
 *   <li>Recharts: <code>&lt;LineChart data={spec.points} /&gt;</code></li>
 *   <li>Matplotlib: <code>plt.plot(spec.values, spec.predictions)</code></li>
 *   <li>Tableau: Direct data import</li>
 * </ul>
 * 
 * <p><b>Example Output:</b></p>
 * <pre>{@code
 * {
 *   "type": "pdp",
 *   "featureName": "age",
 *   "points": [
 *     {"value": 20, "prediction": 0.45, "lower": 0.42, "upper": 0.48},
 *     {"value": 30, "prediction": 0.58, "lower": 0.54, "upper": 0.62}
 *   ],
 *   "slope": 0.013,
 *   "monotonicity": 0.95,
 *   "heterogeneity": 0.02
 * }
 * }</pre>
 * 
 * @since 0.6.0
 */
@Stable(since = "0.6.0")
public class PDPSpec {
    
    private final String type = "pdp";
    private final String featureName;
    private final List<PDPPoint> points;
    private final Double slope;
    private final Double monotonicity;
    private final Double heterogeneity;
    private final List<ICECurve> iceCurves;
    
    /**
     * Creates a PDP specification.
     * 
     * @param featureName name of the feature being analyzed
     * @param points list of (value, prediction) points
     * @param slope average slope of the PDP curve
     * @param monotonicity monotonicity score (-1 to +1)
     * @param heterogeneity variance across individual curves
     * @param iceCurves optional ICE curves for detailed analysis
     */
    public PDPSpec(
            String featureName,
            List<PDPPoint> points,
            Double slope,
            Double monotonicity,
            Double heterogeneity,
            List<ICECurve> iceCurves) {
        this.featureName = featureName;
        this.points = List.copyOf(points);
        this.slope = slope;
        this.monotonicity = monotonicity;
        this.heterogeneity = heterogeneity;
        this.iceCurves = iceCurves != null ? List.copyOf(iceCurves) : null;
    }
    
    /**
     * Converts PartialDependence to visualization spec.
     */
    public static PDPSpec from(PartialDependence pd, String featureName) {
        List<PDPPoint> points = new ArrayList<>();
        double[] values = pd.getGridPoints();
        double[] predictions = pd.getPredictions();
        double[] lowerBounds = pd.getLowerBound().orElse(null);
        double[] upperBounds = pd.getUpperBound().orElse(null);
        
        for (int i = 0; i < values.length; i++) {
            double lowerVal = lowerBounds != null ? lowerBounds[i] : predictions[i];
            double upperVal = upperBounds != null ? upperBounds[i] : predictions[i];
            points.add(new PDPPoint(values[i], predictions[i], lowerVal, upperVal));
        }
        
        // Convert ICE curves if available
        List<ICECurve> iceCurves = null;
        List<double[]> iceList = pd.getICECurves();
        if (!iceList.isEmpty()) {
            iceCurves = new ArrayList<>();
            for (int j = 0; j < iceList.size(); j++) {
                double[] curve = iceList.get(j);
                List<Double> curvePredictions = new ArrayList<>();
                for (int i = 0; i < values.length && i < curve.length; i++) {
                    curvePredictions.add(curve[i]);
                }
                iceCurves.add(new ICECurve("instance_" + j, values, curvePredictions));
            }
        }
        
        return new PDPSpec(
            featureName,
            points,
            pd.getSlope(),
            (double) pd.getMonotonicity(),
            pd.getHeterogeneity(),
            iceCurves
        );
    }
    
    /**
     * Converts to JSON string for frontend consumption.
     */
    public String toJsonString() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"type\": \"").append(type).append("\",\n");
        json.append("  \"featureName\": \"").append(featureName).append("\",\n");
        json.append("  \"points\": [\n");
        
        for (int i = 0; i < points.size(); i++) {
            json.append("    ").append(points.get(i).toJsonString());
            if (i < points.size() - 1) json.append(",");
            json.append("\n");
        }
        
        json.append("  ],\n");
        json.append("  \"slope\": ").append(slope).append(",\n");
        json.append("  \"monotonicity\": ").append(monotonicity).append(",\n");
        json.append("  \"heterogeneity\": ").append(heterogeneity);
        
        if (iceCurves != null && !iceCurves.isEmpty()) {
            json.append(",\n");
            json.append("  \"iceCurves\": [\n");
            for (int i = 0; i < iceCurves.size(); i++) {
                json.append("    ").append(iceCurves.get(i).toJsonString());
                if (i < iceCurves.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]\n");
        } else {
            json.append("\n");
        }
        
        json.append("}");
        return json.toString();
    }
    
    // Getters
    public String getType() { return type; }
    public String getFeatureName() { return featureName; }
    public List<PDPPoint> getPoints() { return points; }
    public Double getSlope() { return slope; }
    public Double getMonotonicity() { return monotonicity; }
    public Double getHeterogeneity() { return heterogeneity; }
    public Optional<List<ICECurve>> getICECurves() { 
        return Optional.ofNullable(iceCurves); 
    }
    
    /**
     * Single point in PDP curve.
     */
    public static class PDPPoint {
        private final double value;
        private final double prediction;
        private final double lower;
        private final double upper;
        
        public PDPPoint(double value, double prediction, double lower, double upper) {
            this.value = value;
            this.prediction = prediction;
            this.lower = lower;
            this.upper = upper;
        }
        
        public String toJsonString() {
            return String.format(
                "{\"value\": %.4f, \"prediction\": %.6f, \"lower\": %.6f, \"upper\": %.6f}",
                value, prediction, lower, upper
            );
        }
        
        public double getValue() { return value; }
        public double getPrediction() { return prediction; }
        public double getLower() { return lower; }
        public double getUpper() { return upper; }
    }
    
    /**
     * Individual Conditional Expectation curve.
     */
    public static class ICECurve {
        private final String instanceId;
        private final double[] values;
        private final List<Double> predictions;
        
        public ICECurve(String instanceId, double[] values, List<Double> predictions) {
            this.instanceId = instanceId;
            this.values = values.clone();
            this.predictions = List.copyOf(predictions);
        }
        
        public String toJsonString() {
            StringBuilder json = new StringBuilder();
            json.append("{\"instanceId\": \"").append(instanceId).append("\", ");
            json.append("\"points\": [");
            for (int i = 0; i < values.length; i++) {
                json.append(String.format("{\"value\": %.4f, \"prediction\": %.6f}", 
                    values[i], predictions.get(i)));
                if (i < values.length - 1) json.append(", ");
            }
            json.append("]}");
            return json.toString();
        }
        
        public String getInstanceId() { return instanceId; }
        public double[] getValues() { return values.clone(); }
        public List<Double> getPredictions() { return predictions; }
    }
}


