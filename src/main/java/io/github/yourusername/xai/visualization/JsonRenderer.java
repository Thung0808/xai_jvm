package io.github.Thung0808.xai.visualization;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.api.Stable;

/**
 * Renders explanations as JSON.
 * 
 * <p>Output is manually constructed to avoid external dependencies.
 * For production use with complex objects, consider Jackson or Gson.</p>
 *
 * @since 0.2.0
 */
@Stable(since = "0.3.0")
public class JsonRenderer implements ExplanationRenderer {
    
    private final boolean prettyPrint;
    
    public JsonRenderer(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
    
    public JsonRenderer() {
        this(true);
    }
    
    @Override
    public String render(Explanation explanation) {
        StringBuilder json = new StringBuilder();
        String indent = prettyPrint ? "  " : "";
        String indent2 = prettyPrint ? "    " : "";
        String indent3 = prettyPrint ? "      " : "";
        String newline = prettyPrint ? "\n" : "";
        
        json.append("{").append(newline);
        
        // Prediction
        if (prettyPrint) json.append(indent);
        json.append("\"prediction\":")
            .append(prettyPrint ? " " : "")
            .append(explanation.getPrediction())
            .append(",").append(newline);
        
        // Baseline
        if (prettyPrint) json.append(indent);
        json.append("\"baseline\":")
            .append(prettyPrint ? " " : "")
            .append(explanation.getBaseline())
            .append(",").append(newline);
        
        // Attributions
        if (prettyPrint) json.append(indent);
        json.append("\"attributions\":[").append(newline);
        
        var attrs = explanation.getAttributions();
        for (int i = 0; i < attrs.size(); i++) {
            FeatureAttribution attr = attrs.get(i);
            
            if (prettyPrint) json.append(indent2);
            json.append("{").append(newline);
            
            if (prettyPrint) json.append(indent3);
            json.append("\"feature\":\"").append(escape(attr.feature())).append("\",").append(newline);
            
            if (prettyPrint) json.append(indent3);
            json.append("\"importance\":")
                .append(attr.importance()).append(",").append(newline);
            
            if (prettyPrint) json.append(indent3);
            json.append("\"confidenceInterval\":")
                .append(attr.confidenceInterval()).append(newline);
            
            if (prettyPrint) json.append(indent2);
            json.append("}");
            
            if (i < attrs.size() - 1) {
                json.append(",");
            }
            json.append(newline);
        }
        
        if (prettyPrint) json.append(indent);
        json.append("]").append(newline);
        
        // Metadata
        var meta = explanation.getMetadata();
        if (meta != null) {
            if (prettyPrint) json.append(indent);
            json.append(",\"metadata\":{").append(newline);
            
            if (prettyPrint) json.append(indent2);
            json.append("\"explainer\":\"").append(escape(meta.explainerName())).append("\",").append(newline);
            
            if (prettyPrint) json.append(indent2);
            json.append("\"timestamp\":\"").append(meta.timestamp()).append("\",").append(newline);
            
            if (prettyPrint) json.append(indent2);
            json.append("\"seed\":").append(meta.seed()).append(",").append(newline);
            
            if (prettyPrint) json.append(indent2);
            json.append("\"trials\":").append(meta.trials()).append(newline);
            
            if (prettyPrint) json.append(indent);
            json.append("}").append(newline);
        }
        
        json.append("}");
        
        return json.toString();
    }
    
    @Override
    public String mimeType() {
        return "application/json";
    }
    
    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
