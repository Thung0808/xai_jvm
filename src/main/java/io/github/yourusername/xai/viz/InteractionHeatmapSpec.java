package io.github.Thung0808.xai.viz;

import io.github.Thung0808.xai.advanced.InteractionPlot;
import io.github.Thung0808.xai.api.Stable;

import java.util.*;

/**
 * Interaction heatmap visualization specification.
 * 
 * <p><b>Framework-Agnostic Design:</b></p>
 * <p>Outputs pure data structure for rendering by:</p>
 * <ul>
 *   <li>Plotly: <code>plotly.graph_objects.Heatmap(z=matrix, x=x, y=y)</code></li>
 *   <li>Seaborn: <code>sns.heatmap(spec.matrix)</code></li>
 *   <li>D3.js: <code>d3.heatmap(spec)</code></li>
 *   <li>PowerBI: Direct matrix import</li>
 * </ul>
 * 
 * <p><b>Example Output:</b></p>
 * <pre>{@code
 * {
 *   "type": "interaction_heatmap",
 *   "feature1": "age",
 *   "feature2": "income",
 *   "matrix": [
 *     [0.45, 0.52, 0.61],
 *     [0.48, 0.58, 0.67],
 *     [0.51, 0.64, 0.75]
 *   ],
 *   "xValues": [20, 40, 60],
 *   "yValues": [30000, 60000, 90000],
 *   "interactionStrength": 0.24,
 *   "synergy": "positive"
 * }
 * }</pre>
 * 
 * @since 0.6.0
 */
@Stable(since = "0.6.0")
public class InteractionHeatmapSpec {
    
    private final String type = "interaction_heatmap";
    private final String feature1;
    private final String feature2;
    private final double[][] matrix;
    private final double[] xValues;
    private final double[] yValues;
    private final Double interactionStrength;
    private final String synergy;
    private final Map<String, Object> options;
    
    /**
     * Creates an interaction heatmap specification.
     * 
     * @param feature1 name of first feature (x-axis)
     * @param feature2 name of second feature (y-axis)
     * @param matrix 2D prediction matrix [y][x]
     * @param xValues x-axis values (feature1)
     * @param yValues y-axis values (feature2)
     * @param interactionStrength H-statistic (0-1)
     * @param synergy "positive", "negative", or "none"
     */
    public InteractionHeatmapSpec(
            String feature1,
            String feature2,
            double[][] matrix,
            double[] xValues,
            double[] yValues,
            Double interactionStrength,
            String synergy) {
        this.feature1 = feature1;
        this.feature2 = feature2;
        this.matrix = deepCopy(matrix);
        this.xValues = xValues.clone();
        this.yValues = yValues.clone();
        this.interactionStrength = interactionStrength;
        this.synergy = synergy;
        this.options = new LinkedHashMap<>();
    }
    
    /**
     * Converts InteractionPlot to visualization spec.
     */
    public static InteractionHeatmapSpec from(
            InteractionPlot plot,
            String feature1,
            String feature2) {
        
        double interactionStrength = plot.getInteractionStrength();
        String synergy;
        if (interactionStrength < 0.05) {
            synergy = "none";
        } else {
            // Determine synergy by comparing diagonal predictions
            double[][] grid = plot.getPredictions();
            double diagSum = 0.0;
            int diagCount = Math.min(grid.length, grid[0].length);
            for (int i = 0; i < diagCount; i++) {
                diagSum += grid[i][i];
            }
            double diagMean = diagSum / diagCount;
            
            double totalMean = Arrays.stream(grid)
                .flatMapToDouble(Arrays::stream)
                .average()
                .orElse(0.0);
            
            synergy = diagMean > totalMean ? "positive" : "negative";
        }
        
        return new InteractionHeatmapSpec(
            feature1,
            feature2,
            plot.getPredictions(),
            plot.getGrid1Points(),
            plot.getGrid2Points(),
            interactionStrength,
            synergy
        );
    }
    
    /**
     * Converts to JSON string for frontend consumption.
     */
    public String toJsonString() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"type\": \"").append(type).append("\",\n");
        json.append("  \"feature1\": \"").append(feature1).append("\",\n");
        json.append("  \"feature2\": \"").append(feature2).append("\",\n");
        
        // Matrix
        json.append("  \"matrix\": [\n");
        for (int i = 0; i < matrix.length; i++) {
            json.append("    [");
            for (int j = 0; j < matrix[i].length; j++) {
                json.append(String.format("%.6f", matrix[i][j]));
                if (j < matrix[i].length - 1) json.append(", ");
            }
            json.append("]");
            if (i < matrix.length - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");
        
        // X values
        json.append("  \"xValues\": [");
        for (int i = 0; i < xValues.length; i++) {
            json.append(String.format("%.4f", xValues[i]));
            if (i < xValues.length - 1) json.append(", ");
        }
        json.append("],\n");
        
        // Y values
        json.append("  \"yValues\": [");
        for (int i = 0; i < yValues.length; i++) {
            json.append(String.format("%.4f", yValues[i]));
            if (i < yValues.length - 1) json.append(", ");
        }
        json.append("],\n");
        
        json.append("  \"interactionStrength\": ").append(interactionStrength).append(",\n");
        json.append("  \"synergy\": \"").append(synergy).append("\"\n");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Adds custom rendering option.
     */
    public InteractionHeatmapSpec withOption(String key, Object value) {
        options.put(key, value);
        return this;
    }
    
    /**
     * Returns contour lines for given thresholds.
     */
    public List<ContourLine> getContourLines(double... thresholds) {
        List<ContourLine> contours = new ArrayList<>();
        for (double threshold : thresholds) {
            List<Point> points = new ArrayList<>();
            // Simple contour extraction (can be enhanced with marching squares)
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    if (Math.abs(matrix[i][j] - threshold) < 0.01) {
                        points.add(new Point(xValues[j], yValues[i]));
                    }
                }
            }
            if (!points.isEmpty()) {
                contours.add(new ContourLine(threshold, points));
            }
        }
        return contours;
    }
    
    // Getters
    public String getType() { return type; }
    public String getFeature1() { return feature1; }
    public String getFeature2() { return feature2; }
    public double[][] getMatrix() { return deepCopy(matrix); }
    public double[] getXValues() { return xValues.clone(); }
    public double[] getYValues() { return yValues.clone(); }
    public Double getInteractionStrength() { return interactionStrength; }
    public String getSynergy() { return synergy; }
    public Map<String, Object> getOptions() { return new LinkedHashMap<>(options); }
    
    // Helpers
    private static double[][] deepCopy(double[][] original) {
        double[][] copy = new double[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }
    
    /**
     * Point in 2D space.
     */
    public static class Point {
        public final double x;
        public final double y;
        
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
    
    /**
     * Contour line at specific threshold.
     */
    public static class ContourLine {
        public final double threshold;
        public final List<Point> points;
        
        public ContourLine(double threshold, List<Point> points) {
            this.threshold = threshold;
            this.points = List.copyOf(points);
        }
    }
}
