package io.github.thung0808.xai.viz;

import io.github.thung0808.xai.api.Explanation;
import io.github.thung0808.xai.api.FeatureAttribution;
import io.github.thung0808.xai.api.Stable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Force plot visualization specification (SHAP-style).
 * 
 * <p><b>Framework-Agnostic Design:</b></p>
 * <p>Outputs pure data structure that can be rendered by:</p>
 * <ul>
 *   <li>React: <code>&lt;ForceChart data={spec} /&gt;</code></li>
 *   <li>Vega-Lite: <code>vegaEmbed('#vis', spec)</code></li>
 *   <li>D3.js: <code>d3.forceChart(spec)</code></li>
 *   <li>Python: <code>plotly.graph_objects.ForcePlot(spec)</code></li>
 * </ul>
 * 
 * <p><b>Example Output:</b></p>
 * <pre>{@code
 * {
 *   "type": "force",
 *   "baseValue": 0.5,
 *   "outputValue": 0.87,
 *   "forces": [
 *     {"feature": "age=45", "effect": +0.25, "value": 45},
 *     {"feature": "income=$80k", "effect": +0.18, "value": 80000}
 *   ],
 *   "colorScheme": "redBlue"
 * }
 * }</pre>
 * 
 * @since 0.6.0
 */
@Stable(since = "0.6.0")
public class ForcePlotSpec {
    
    private final String type = "force";
    private final double baseValue;
    private final double outputValue;
    private final List<Force> forces;
    private final String colorScheme;
    private final Map<String, Object> options;
    
    /**
     * Creates a force plot specification.
     * 
     * @param baseValue the baseline prediction (e.g., dataset mean)
     * @param outputValue the actual prediction
     * @param forces list of feature forces (sorted by magnitude)
     * @param colorScheme color scheme name ("redBlue", "viridis", etc.)
     */
    public ForcePlotSpec(
            double baseValue,
            double outputValue,
            List<Force> forces,
            String colorScheme) {
        this.baseValue = baseValue;
        this.outputValue = outputValue;
        this.forces = List.copyOf(forces);
        this.colorScheme = colorScheme;
        this.options = new LinkedHashMap<>();
    }
    
    /**
     * Converts Explanation to force plot spec.
     * 
     * @param explanation the explanation to visualize
     * @param baseValue the baseline value (model's average prediction)
     * @return force plot specification
     */
    public static ForcePlotSpec from(Explanation explanation, double baseValue) {
        List<Force> forces = explanation.getAttributions().stream()
            .map(attr -> new Force(
                attr.feature(),
                attr.importance(),
                0.0,  // Actual feature value (not available in current API)
                attr.importance() > 0 ? "positive" : "negative"
            ))
            .sorted(Comparator.comparingDouble((Force f) -> Math.abs(f.effect)).reversed())
            .collect(Collectors.toList());
        
        return new ForcePlotSpec(
            baseValue,
            explanation.getPrediction(),
            forces,
            "redBlue"
        );
    }
    
    /**
     * Converts to JSON string for frontend consumption.
     */
    public String toJsonString() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"type\": \"").append(type).append("\",\n");
        json.append("  \"baseValue\": ").append(baseValue).append(",\n");
        json.append("  \"outputValue\": ").append(outputValue).append(",\n");
        json.append("  \"forces\": [\n");
        
        for (int i = 0; i < forces.size(); i++) {
            json.append("    ").append(forces.get(i).toJsonString());
            if (i < forces.size() - 1) json.append(",");
            json.append("\n");
        }
        
        json.append("  ],\n");
        json.append("  \"colorScheme\": \"").append(colorScheme).append("\"\n");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Adds custom rendering option.
     */
    public ForcePlotSpec withOption(String key, Object value) {
        options.put(key, value);
        return this;
    }
    
    // Getters
    public String getType() { return type; }
    public double getBaseValue() { return baseValue; }
    public double getOutputValue() { return outputValue; }
    public List<Force> getForces() { return forces; }
    public String getColorScheme() { return colorScheme; }
    public Map<String, Object> getOptions() { return new LinkedHashMap<>(options); }
    
    /**
     * Single force (feature effect).
     */
    public static class Force {
        private final String feature;
        private final double effect;
        private final double value;
        private final String direction;
        
        public Force(String feature, double effect, double value, String direction) {
            this.feature = feature;
            this.effect = effect;
            this.value = value;
            this.direction = direction;
        }
        
        public String toJsonString() {
            return String.format(
                "{\"feature\": \"%s\", \"effect\": %.6f, \"value\": %.2f, \"direction\": \"%s\"}",
                feature, effect, value, direction
            );
        }
        
        public String getFeature() { return feature; }
        public double getEffect() { return effect; }
        public double getValue() { return value; }
        public String getDirection() { return direction; }
    }
}


