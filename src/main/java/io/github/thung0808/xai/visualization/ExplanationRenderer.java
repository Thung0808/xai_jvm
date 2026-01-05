package io.github.thung0808.xai.visualization;

import io.github.thung0808.xai.api.Explanation;

/**
 * Renders explanations in various formats.
 * 
 * <p>Unlike Python's matplotlib-dependent SHAP, this provides multiple
 * zero-dependency output formats suitable for different contexts:</p>
 * 
 * <ul>
 *   <li><b>JSON:</b> Machine-readable for APIs</li>
 *   <li><b>Markdown:</b> Documentation and reports</li>
 *   <li><b>ANSI:</b> Colored terminal output</li>
 *   <li><b>HTML:</b> Interactive web dashboards</li>
 * </ul>
 *
 * @since 0.2.0
 */
public interface ExplanationRenderer {
    
    /**
     * Renders an explanation to a string in the format supported by this renderer.
     *
     * @param explanation the explanation to render
     * @return formatted string representation
     */
    String render(Explanation explanation);
    
    /**
     * Returns the MIME type of the output format.
     */
    String mimeType();
}


