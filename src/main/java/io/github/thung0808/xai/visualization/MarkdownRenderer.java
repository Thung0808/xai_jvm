package io.github.thung0808.xai.visualization;

import io.github.thung0808.xai.api.Explanation;
import io.github.thung0808.xai.api.FeatureAttribution;

import java.util.Comparator;
import java.util.List;

/**
 * Renders explanations as Markdown tables.
 * 
 * <p>Perfect for documentation, Jupyter notebooks, and GitHub README files.</p>
 *
 * @since 0.2.0
 */
public class MarkdownRenderer implements ExplanationRenderer {
    
    private final boolean sortByImportance;
    
    public MarkdownRenderer(boolean sortByImportance) {
        this.sortByImportance = sortByImportance;
    }
    
    public MarkdownRenderer() {
        this(true);
    }
    
    @Override
    public String render(Explanation explanation) {
        StringBuilder md = new StringBuilder();
        
        // Header
        md.append("# Explanation\n\n");
        
        // Summary
        md.append("**Prediction:** ").append(String.format("%.4f", explanation.getPrediction())).append("\n\n");
        md.append("**Baseline:** ").append(String.format("%.4f", explanation.getBaseline())).append("\n\n");
        
        // Attributions table
        md.append("## Feature Attributions\n\n");
        md.append("| Feature | Importance | Confidence Interval | Impact |\n");
        md.append("|---------|------------|---------------------|--------|\n");
        
        List<FeatureAttribution> attrs = explanation.getAttributions();
        
        if (sortByImportance) {
            attrs = attrs.stream()
                .sorted(Comparator.comparingDouble(
                    (FeatureAttribution a) -> Math.abs(a.importance())).reversed())
                .toList();
        }
        
        for (FeatureAttribution attr : attrs) {
            String impact = renderImpactBar(attr.importance());
            
            md.append("| ")
                .append(attr.feature())
                .append(" | ")
                .append(String.format("%.4f", attr.importance()))
                .append(" | ")
                .append(String.format("Â±%.4f", attr.confidenceInterval()))
                .append(" | ")
                .append(impact)
                .append(" |\n");
        }
        
        // Metadata
        var meta = explanation.getMetadata();
        if (meta != null) {
            md.append("\n## Metadata\n\n");
            md.append("- **Explainer:** ").append(meta.explainerName()).append("\n");
            md.append("- **Timestamp:** ").append(meta.timestamp()).append("\n");
            md.append("- **Seed:** ").append(meta.seed()).append("\n");
            md.append("- **Trials:** ").append(meta.trials()).append("\n");
        }
        
        return md.toString();
    }
    
    @Override
    public String mimeType() {
        return "text/markdown";
    }
    
    /**
     * Creates a simple ASCII bar chart for impact visualization.
     */
    private String renderImpactBar(double importance) {
        // Scale to [-10, +10]
        int bars = (int) Math.round(importance * 10);
        bars = Math.max(-10, Math.min(10, bars));
        
        if (bars > 0) {
            return "+" + "â–ˆ".repeat(bars);
        } else if (bars < 0) {
            return "-" + "â–ˆ".repeat(-bars);
        } else {
            return "Â·";
        }
    }
}


