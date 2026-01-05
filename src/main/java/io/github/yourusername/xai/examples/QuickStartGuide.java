package io.github.Thung0808.xai.examples;

/**
 * Quick start guide for XAI Core.
 * 
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // 1. Create model adapter
 * PredictiveModel model = ... ; // Your trained model
 * 
 * // 2. Prepare reference data
 * double[][] referenceData = ...; // Training or validation set
 * double[] referenceLabels = ...; // Corresponding labels
 * 
 * // 3. Create explainer
 * Explainer explainer = new PermutationExplainer(
 *     model,
 *     referenceData,
 *     referenceLabels
 * );
 * 
 * // 4. Explain a prediction
 * double[] instance = {45, 80000, 720, 0.35};
 * String[] featureNames = {"age", "income", "credit_score", "debt_ratio"};
 * 
 * Explanation explanation = explainer.explain(instance, featureNames);
 * 
 * // 5. Use the explanation
 * System.out.printf("Prediction: %.2f%n", explanation.getPrediction());
 * explanation.getAttributions().forEach(attr ->
 *     System.out.printf("  %s: %.3f%n", attr.feature(), attr.importance())
 * );
 * }</pre>
 * 
 * <h2>JSON Export</h2>
 * <pre>{@code
 * // Convert to OpenAPI-friendly JSON
 * ExplanationJson json = ExplanationJson.from(explanation);
 * String jsonString = json.toJsonString();
 * 
 * // Send to frontend or store in database
 * }</pre>
 * 
 * <h2>Visualization</h2>
 * <pre>{@code
 * // Generate force plot specification
 * ForcePlotSpec forcePlot = ForcePlotSpec.from(explanation, 0.5);
 * String vizData = forcePlot.toJsonString();
 * 
 * // Render with React, D3, Plotly, etc.
 * }</pre>
 * 
 * @since 0.7.0
 */
public final class QuickStartGuide {
    private QuickStartGuide() {
        throw new UnsupportedOperationException("Documentation class");
    }
}
