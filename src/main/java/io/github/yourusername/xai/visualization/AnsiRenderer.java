package io.github.Thung0808.xai.visualization;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.FeatureAttribution;

import java.util.Comparator;
import java.util.List;

/**
 * Renders explanations with ANSI color codes for terminal output.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>🟢 Green for positive impact</li>
 *   <li>🔴 Red for negative impact</li>
 *   <li>Bold for high importance</li>
 *   <li>Horizontal bars for visual comparison</li>
 * </ul>
 *
 * @since 0.2.0
 */
public class AnsiRenderer implements ExplanationRenderer {
    
    // ANSI escape codes
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    
    private final boolean useColors;
    private final boolean sortByImportance;
    
    public AnsiRenderer(boolean useColors, boolean sortByImportance) {
        this.useColors = useColors;
        this.sortByImportance = sortByImportance;
    }
    
    public AnsiRenderer() {
        this(true, true);
    }
    
    @Override
    public String render(Explanation explanation) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append(color(BOLD + CYAN, "═══════════════════════════════════════════════════════════")).append("\n");
        sb.append(color(BOLD + CYAN, "                    EXPLANATION REPORT                        ")).append("\n");
        sb.append(color(BOLD + CYAN, "═══════════════════════════════════════════════════════════")).append("\n\n");
        
        // Prediction info
        sb.append(color(BOLD, "Prediction: "))
            .append(color(YELLOW, String.format("%.4f", explanation.getPrediction())))
            .append("  |  ")
            .append(color(BOLD, "Baseline: "))
            .append(color(YELLOW, String.format("%.4f", explanation.getBaseline())))
            .append("\n\n");
        
        // Attributions
        sb.append(color(BOLD, "Feature Attributions:")).append("\n\n");
        
        List<FeatureAttribution> attrs = explanation.getAttributions();
        
        if (sortByImportance) {
            attrs = attrs.stream()
                .sorted(Comparator.comparingDouble(
                    (FeatureAttribution a) -> Math.abs(a.importance())).reversed())
                .toList();
        }
        
        // Find max importance for scaling
        double maxImportance = attrs.stream()
            .mapToDouble(a -> Math.abs(a.importance()))
            .max()
            .orElse(1.0);
        
        for (FeatureAttribution attr : attrs) {
            renderAttribution(sb, attr, maxImportance);
        }
        
        // Metadata
        var meta = explanation.getMetadata();
        if (meta != null) {
            sb.append("\n");
            sb.append(color(BOLD + BLUE, "───────────────────────────────────────────────────────────")).append("\n");
            sb.append(color(BOLD, "Metadata: "))
                .append(meta.explainerName())
                .append(" | Trials: ").append(meta.trials())
                .append(" | Seed: ").append(meta.seed())
                .append("\n");
        }
        
        sb.append(color(BOLD + CYAN, "═══════════════════════════════════════════════════════════")).append("\n");
        
        return sb.toString();
    }
    
    @Override
    public String mimeType() {
        return "text/plain";
    }
    
    private void renderAttribution(StringBuilder sb, FeatureAttribution attr, double maxImportance) {
        double importance = attr.importance();
        double absImportance = Math.abs(importance);
        
        // Feature name (fixed width)
        String featureName = String.format("%-20s", attr.feature());
        sb.append(color(BOLD, featureName)).append(" ");
        
        // Importance value
        String importanceStr = String.format("%+.4f", importance);
        String valueColor = importance >= 0 ? GREEN : RED;
        sb.append(color(valueColor, importanceStr)).append(" ");
        
        // Bar chart
        int barLength = (int) Math.round((absImportance / maxImportance) * 30);
        barLength = Math.max(1, barLength);
        
        String bar = "█".repeat(barLength);
        sb.append(color(valueColor, bar));
        
        // Confidence interval
        sb.append(color(RESET, String.format(" (±%.4f)", attr.confidenceInterval())));
        
        sb.append("\n");
    }
    
    private String color(String code, String text) {
        if (!useColors) {
            return text;
        }
        return code + text + RESET;
    }
}
