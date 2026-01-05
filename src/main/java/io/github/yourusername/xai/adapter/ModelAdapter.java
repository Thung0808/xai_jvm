package io.github.Thung0808.xai.adapter;

import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.api.Stable;

/**
 * Service Provider Interface (SPI) for ML framework adapters.
 * 
 * <p>Implement this interface to integrate XAI with new ML frameworks.
 * The adapter pattern allows automatic framework detection via ServiceLoader.</p>
 * 
 * <p><b>Example Implementation (Smile):</b></p>
 * <pre>{@code
 * public class SmileModelAdapter implements ModelAdapter {
 *     @Override
 *     public String getFramework() {
 *         return "smile";
 *     }
 *     
 *     @Override
 *     public boolean canAdapt(Object model) {
 *         return model instanceof LogisticRegression 
 *             || model instanceof RandomForest;
 *     }
 *     
 *     @Override
 *     public PredictiveModel adapt(Object model, Object... config) {
 *         if (model instanceof LogisticRegression) {
 *             return new SmileLogisticRegressionAdapter((LogisticRegression) model);
 *         }
 *         throw new IllegalArgumentException("Unsupported model type");
 *     }
 * }
 * }</pre>
 * 
 * <p><b>ServiceLoader Registration:</b></p>
 * <p>Create file: {@code META-INF/services/io.github.Thung0808.xai.adapter.ModelAdapter}</p>
 * <p>Content: {@code io.github.Thung0808.xai.adapter.smile.SmileModelAdapter}</p>
 * 
 * <p><b>Auto-Detection Usage:</b></p>
 * <pre>{@code
 * // Automatic: Smile classes available → SmileAdapter loaded
 * PredictiveModel model = ModelAdapterRegistry.adapt(myLogisticRegression);
 * 
 * // Manual: Specify adapter explicitly
 * PredictiveModel model = ModelAdapterRegistry.adapt(myDJLModel, "djl");
 * }</pre>
 *
 * @since 0.4.0
 */
@Stable(since = "0.4.0")
public interface ModelAdapter {
    
    /**
     * Returns the name of the ML framework this adapter supports.
     * 
     * @return framework name (e.g., "smile", "djl", "onnx")
     */
    String getFramework();
    
    /**
     * Checks if this adapter can adapt the given model.
     * 
     * <p>This method is called by {@link ModelAdapterRegistry} to determine
     * if this adapter can handle the model. Return true only if you can
     * successfully adapt the model.</p>
     * 
     * @param model the object to check (may be null)
     * @return true if this adapter can adapt the model
     */
    boolean canAdapt(Object model);
    
    /**
     * Adapts an ML framework model to a {@link PredictiveModel}.
     * 
     * @param model the native framework model
     * @param config optional configuration (framework-specific)
     * @return adapted model as PredictiveModel
     * @throws IllegalArgumentException if model cannot be adapted
     * @throws IllegalStateException if required dependencies not available
     */
    PredictiveModel adapt(Object model, Object... config);
    
    /**
     * Returns the priority of this adapter for auto-detection.
     * 
     * <p>When multiple adapters can handle a model, the one with highest
     * priority is used. Default: 0 (normal priority).</p>
     * 
     * <p>Use higher values to prioritize:</p>
     * <ul>
     *   <li>100: Default/preferred adapter for a framework
     *   <li>50: Alternative adapter
     *   <li>0: Fallback adapter
     *   <li>-50: Low-priority adapter (use only if others fail)
     * </ul>
     * 
     * @return priority value (higher wins)
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * Checks if this adapter is available (dependencies present).
     * 
     * <p>This method should check if required framework classes are on
     * the classpath. Return false if dependencies are missing.</p>
     * 
     * @return true if adapter can be used
     */
    default boolean isAvailable() {
        return true;
    }
    
    /**
     * Returns a human-readable description of this adapter.
     * 
     * @return description (e.g., "Smile ML library adapter for logistic regression, random forest")
     */
    default String getDescription() {
        return "Adapter for " + getFramework() + " framework";
    }
}
