package io.github.Thung0808.xai.global;

import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.experimental.Incubating;

import java.util.List;

/**
 * SPI for dataset-level (global) explanations.
 * 
 * <p>While local explanations explain individual predictions, global explanations
 * summarize patterns across the entire dataset or model behavior.</p>
 * 
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li>Dataset auditing: What features drive most predictions?</li>
 *   <li>Fairness analysis: Are certain groups treated differently?</li>
 *   <li>Model debugging: Which features is the model relying on?</li>
 *   <li>Regulatory compliance: Document key decision drivers</li>
 * </ul>
 * 
 * <p><b>Example:</b></p>
 * <pre>{@code
 * GlobalExplainer explainer = new GlobalFeatureImportance();
 * GlobalExplanation explanation = explainer.explainDataset(model, dataset);
 * 
 * // Top 5 most important features
 * List<FeatureAttribution> top5 = explanation.getTopFeatures(5);
 * // [age: 0.35, income: 0.28, education: 0.21, ...]
 * 
 * // Feature importance distribution
 * Map<String, Double> importances = explanation.getFeatureImportances();
 * }</pre>
 *
 * @since 0.4.0
 */
@Incubating(
    since = "0.4.0",
    graduationTarget = "1.0.0",
    reason = "API still evolving; gathering feedback from production usage"
)
public interface GlobalExplainer {
    
    /**
     * Explains model behavior across an entire dataset.
     * 
     * @param model the predictive model
     * @param dataset list of input instances (features as double arrays)
     * @return global explanation summarizing dataset patterns
     * @throws IllegalArgumentException if dataset is empty or null
     * @throws IllegalStateException if required state not configured
     */
    GlobalExplanation explainDataset(PredictiveModel model, List<double[]> dataset);
    
    /**
     * Explains model behavior across a dataset with optional labels.
     * 
     * <p>Labels enable additional analysis:</p>
     * <ul>
     *   <li>Correct vs incorrect predictions</li>
     *   <li>Per-class feature importance</li>
     *   <li>Fairness metrics by outcome</li>
     * </ul>
     * 
     * @param model the predictive model
     * @param dataset list of input instances
     * @param labels corresponding predictions or ground truth
     * @return global explanation with label-aware insights
     */
    default GlobalExplanation explainDataset(PredictiveModel model, List<double[]> dataset, double[] labels) {
        return explainDataset(model, dataset);
    }
    
    /**
     * Returns the name of this global explainer.
     * 
     * @return explainer name (e.g., "GlobalFeatureImportance", "SurrogateTree")
     */
    String getName();
}
