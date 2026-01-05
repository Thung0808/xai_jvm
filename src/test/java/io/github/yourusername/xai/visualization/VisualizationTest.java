package io.github.Thung0808.xai.visualization;

import io.github.Thung0808.xai.api.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for visualization renderers.
 */
class VisualizationTest {
    
    private Explanation createSampleExplanation() {
        return Explanation.builder()
            .withPrediction(0.75)
            .withBaseline(0.5)
            .addAttribution(new FeatureAttribution("age", 0.15, 0.02))
            .addAttribution(new FeatureAttribution("income", -0.10, 0.03))
            .addAttribution(new FeatureAttribution("education", 0.20, 0.01))
            .withMetadata(ExplanationMetadata.builder("TestExplainer")
                .seed(42)
                .trials(100)
                .build())
            .build();
    }
    
    @Test
    void testJsonRenderer() {
        Explanation explanation = createSampleExplanation();
        JsonRenderer renderer = new JsonRenderer(true);
        
        String json = renderer.render(explanation);
        
        assertNotNull(json);
        assertTrue(json.contains("\"prediction\""));
        assertTrue(json.contains("0.75"));
        assertTrue(json.contains("\"age\""));
        assertTrue(json.contains("\"income\""));
        assertTrue(json.contains("\"education\""));
        assertTrue(json.contains("\"metadata\""));
        assertTrue(json.contains("TestExplainer"));
        
        assertEquals("application/json", renderer.mimeType());
    }
    
    @Test
    void testJsonRendererCompact() {
        Explanation explanation = createSampleExplanation();
        JsonRenderer renderer = new JsonRenderer(false);
        
        String json = renderer.render(explanation);
        
        assertNotNull(json);
        assertFalse(json.contains("\n")); // Compact format
        assertTrue(json.contains("\"prediction\":0.75"));
    }
    
    @Test
    void testMarkdownRenderer() {
        Explanation explanation = createSampleExplanation();
        MarkdownRenderer renderer = new MarkdownRenderer(true);
        
        String markdown = renderer.render(explanation);
        
        assertNotNull(markdown);
        assertTrue(markdown.contains("# Explanation"));
        assertTrue(markdown.contains("**Prediction:**"));
        assertTrue(markdown.contains("## Feature Attributions"));
        assertTrue(markdown.contains("| Feature | Importance"));
        assertTrue(markdown.contains("age"));
        assertTrue(markdown.contains("income"));
        assertTrue(markdown.contains("education"));
        assertTrue(markdown.contains("## Metadata"));
        
        assertEquals("text/markdown", renderer.mimeType());
    }
    
    @Test
    void testMarkdownRendererSorting() {
        Explanation explanation = createSampleExplanation();
        
        // With sorting
        MarkdownRenderer sortedRenderer = new MarkdownRenderer(true);
        String sorted = sortedRenderer.render(explanation);
        
        // Without sorting
        MarkdownRenderer unsortedRenderer = new MarkdownRenderer(false);
        String unsorted = unsortedRenderer.render(explanation);
        
        assertNotNull(sorted);
        assertNotNull(unsorted);
        
        // Both should contain all features
        assertTrue(sorted.contains("education"));
        assertTrue(unsorted.contains("education"));
    }
    
    @Test
    void testAnsiRenderer() {
        Explanation explanation = createSampleExplanation();
        AnsiRenderer renderer = new AnsiRenderer(true, true);
        
        String ansi = renderer.render(explanation);
        
        assertNotNull(ansi);
        assertTrue(ansi.contains("EXPLANATION REPORT"));
        assertTrue(ansi.contains("Prediction:"));
        assertTrue(ansi.contains("Baseline:"));
        assertTrue(ansi.contains("Feature Attributions:"));
        assertTrue(ansi.contains("age"));
        assertTrue(ansi.contains("income"));
        assertTrue(ansi.contains("education"));
        
        // Should contain ANSI codes
        assertTrue(ansi.contains("\u001B["));
        
        assertEquals("text/plain", renderer.mimeType());
    }
    
    @Test
    void testAnsiRendererWithoutColors() {
        Explanation explanation = createSampleExplanation();
        AnsiRenderer renderer = new AnsiRenderer(false, true);
        
        String plain = renderer.render(explanation);
        
        assertNotNull(plain);
        assertTrue(plain.contains("age"));
        
        // Should NOT contain ANSI codes
        assertFalse(plain.contains("\u001B["));
    }
    
    @Test
    void testAllRenderersWithMinimalExplanation() {
        // Test with minimal explanation
        Explanation minimal = Explanation.builder()
            .withPrediction(1.0)
            .withBaseline(0.0)
            .addAttribution(new FeatureAttribution("x", 0.5, 0.1))
            .withMetadata(ExplanationMetadata.builder("TestExplainer").build())
            .build();
        
        JsonRenderer jsonRenderer = new JsonRenderer();
        String json = jsonRenderer.render(minimal);
        assertNotNull(json);
        assertTrue(json.contains("\"x\""));
        
        MarkdownRenderer mdRenderer = new MarkdownRenderer();
        String markdown = mdRenderer.render(minimal);
        assertNotNull(markdown);
        assertTrue(markdown.contains("x"));
        
        AnsiRenderer ansiRenderer = new AnsiRenderer();
        String ansi = ansiRenderer.render(minimal);
        assertNotNull(ansi);
        assertTrue(ansi.contains("x"));
    }
    
    @Test
    void testJsonEscaping() {
        Explanation explanation = Explanation.builder()
            .withPrediction(1.0)
            .withBaseline(0.0)
            .addAttribution(new FeatureAttribution("feature\"with\\quotes\nand\nnewlines", 0.5, 0.1))
            .withMetadata(ExplanationMetadata.builder("TestExplainer").build())
            .build();
        
        JsonRenderer renderer = new JsonRenderer();
        String json = renderer.render(explanation);
        
        assertNotNull(json);
        assertTrue(json.contains("\\\""));  // Escaped quote
        assertTrue(json.contains("\\\\"));  // Escaped backslash
        assertTrue(json.contains("\\n"));   // Escaped newline
    }
}
