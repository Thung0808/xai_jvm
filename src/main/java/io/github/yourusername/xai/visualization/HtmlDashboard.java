package io.github.Thung0808.xai.visualization;

import io.github.Thung0808.xai.api.Explanation;
import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.schema.ExplanationJson;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Generates an interactive single-file HTML dashboard for XAI explanations.
 * 
 * <p>This low-code visualization tool allows Java developers to instantly view
 * explanations in a professional web dashboard without writing any frontend code.
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Single-file HTML with embedded Tailwind CSS and Vue.js (CDN)</li>
 *   <li>Force plots with feature attribution visualization</li>
 *   <li>Interactive feature details table</li>
 *   <li>Counterfactual analysis (if available)</li>
 *   <li>Trust score and metadata display</li>
 *   <li>Auto-opens in default browser</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Generate explanation
 * Explanation explanation = explainer.explain(model, instance);
 * 
 * // Generate and open HTML dashboard
 * Path report = HtmlDashboard.generate(explanation);
 * HtmlDashboard.open(report);
 * 
 * // Or use convenience method
 * HtmlDashboard.generateAndOpen(explanation);
 * }</pre>
 * 
 * @since 0.7.0
 * @see ExplanationJson
 */
public class HtmlDashboard {
    
    private static final String TEMPLATE_RESOURCE = "/xai-dashboard.html";
    private static final String DEFAULT_OUTPUT_DIR = "xai-reports";
    
    /**
     * Generates an HTML dashboard from an explanation.
     * 
     * @param explanation the explanation to visualize
     * @return path to the generated HTML file
     * @throws IOException if file generation fails
     */
    public static Path generate(Explanation explanation) throws IOException {
        return generate(explanation, null);
    }
    
    /**
     * Generates an HTML dashboard from an explanation with custom output path.
     * 
     * @param explanation the explanation to visualize
     * @param outputPath custom output path (null for default)
     * @return path to the generated HTML file
     * @throws IOException if file generation fails
     */
    public static Path generate(Explanation explanation, Path outputPath) throws IOException {
        // Load template
        String template = loadTemplate();
        
        // Generate JSON data
        String jsonData = buildJsonData(explanation);
        
        // Inject data into template
        String html = template.replace(
            "window.XAI_DATA || {",
            "window.XAI_DATA = " + jsonData + " || {"
        );
        
        // Determine output path
        Path output = outputPath;
        if (output == null) {
            Path reportDir = Paths.get(DEFAULT_OUTPUT_DIR);
            Files.createDirectories(reportDir);
            String filename = String.format("xai-report-%d.html", System.currentTimeMillis());
            output = reportDir.resolve(filename);
        }
        
        // Write HTML file
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write(html);
        }
        
        return output;
    }
    
    /**
     * Generates an HTML dashboard and opens it in the default browser.
     * 
     * @param explanation the explanation to visualize
     * @return path to the generated HTML file
     * @throws IOException if file generation or browser opening fails
     */
    public static Path generateAndOpen(Explanation explanation) throws IOException {
        Path report = generate(explanation);
        open(report);
        return report;
    }
    
    /**
     * Opens an HTML file in the default system browser.
     * 
     * @param htmlPath path to HTML file
     * @throws IOException if browser cannot be opened
     */
    public static void open(Path htmlPath) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(htmlPath.toUri());
                return;
            }
        }
        
        // Fallback: print URL
        System.out.println("Open in browser: " + htmlPath.toUri());
    }
    
    /**
     * Loads the HTML template from resources.
     */
    private static String loadTemplate() throws IOException {
        try (InputStream is = HtmlDashboard.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (is == null) {
                throw new IOException("Template not found: " + TEMPLATE_RESOURCE);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Builds JSON data object from explanation.
     */
    private static String buildJsonData(Explanation explanation) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // Schema version
        json.append("  \"schemaVersion\": \"1.0\",\n");
        
        // Metadata
        var metadata = explanation.getMetadata();
        json.append("  \"explainer\": \"").append(escape(metadata.explainerName())).append("\",\n");
        json.append("  \"timestamp\": ").append(metadata.timestamp()).append(",\n");
        json.append("  \"libraryVersion\": \"").append(escape(metadata.libraryVersion())).append("\",\n");
        json.append("  \"samples\": ").append(metadata.trials()).append(",\n");
        
        // Prediction
        double prediction = explanation.getPrediction();
        json.append("  \"prediction\": ").append(prediction).append(",\n");
        
        // Base value (average prediction)
        double baseValue = explanation.getBaseline();
        json.append("  \"baseValue\": ").append(baseValue).append(",\n");
        
        // Trust score (placeholder - not yet in Explanation API)
        double trustScore = 0.90;
        json.append("  \"trustScore\": ").append(trustScore).append(",\n");
        
        // Features
        json.append("  \"features\": [\n");
        List<FeatureAttribution> attributions = explanation.getAttributions();
        for (int i = 0; i < attributions.size(); i++) {
            FeatureAttribution attr = attributions.get(i);
            json.append("    {\n");
            json.append("      \"name\": \"").append(escape(attr.feature())).append("\",\n");
            json.append("      \"value\": ").append(0.0).append(",\n"); // Value not stored in FeatureAttribution
            json.append("      \"attribution\": ").append(attr.importance()).append(",\n");
            json.append("      \"confidence\": ").append(attr.confidenceInterval()).append("\n");
            json.append("    }");
            if (i < attributions.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("  ],\n");
        
        // Counterfactual (if available)
        json.append("  \"counterfactual\": null\n");
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * Escapes special characters for JSON strings.
     */
    private static String escape(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private HtmlDashboard() {
        throw new UnsupportedOperationException("Utility class");
    }
}
