package io.github.Thung0808.xai.adapter;

import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.api.Stable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry for ML framework adapters using Java ServiceLoader.
 * 
 * <p>This class manages automatic and manual adapter selection for
 * converting native ML framework models to {@link PredictiveModel}.</p>
 * 
 * <p><b>Auto-Detection Example:</b></p>
 * <pre>{@code
 * // Automatically detects available adapters and selects appropriate one
 * LogisticRegression smileModel = new LogisticRegression(...);
 * PredictiveModel model = ModelAdapterRegistry.adapt(smileModel);
 * // → SmileModelAdapter is auto-detected and used
 * }</pre>
 * 
 * <p><b>Manual Selection Example:</b></p>
 * <pre>{@code
 * PredictiveModel model = ModelAdapterRegistry.adapt(smileModel, "smile");
 * }</pre>
 * 
 * <p><b>List Available Adapters:</b></p>
 * <pre>{@code
 * List<ModelAdapterInfo> adapters = ModelAdapterRegistry.getAvailableAdapters();
 * for (ModelAdapterInfo info : adapters) {
 *     System.out.println(info.getFramework() + ": " + info.getDescription());
 * }
 * // Output:
 * // smile: Smile ML library adapter for logistic regression, random forest
 * // djl: Deep Java Library adapter for neural networks
 * }</pre>
 *
 * @since 0.4.0
 */
@Stable(since = "0.4.0")
public final class ModelAdapterRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(ModelAdapterRegistry.class);
    private static final Map<String, ModelAdapter> ADAPTERS_BY_FRAMEWORK = new HashMap<>();
    private static final List<ModelAdapter> ADAPTERS_BY_PRIORITY = new ArrayList<>();
    private static boolean initialized = false;
    
    static {
        initialize();
    }
    
    private ModelAdapterRegistry() {
        // Utility class
    }
    
    /**
     * Initializes the registry by loading adapters via ServiceLoader.
     */
    private static synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        ServiceLoader<ModelAdapter> loader = ServiceLoader.load(ModelAdapter.class);
        List<ModelAdapter> discovered = new ArrayList<>();
        
        for (ModelAdapter adapter : loader) {
            if (!adapter.isAvailable()) {
                log.debug("Adapter {} is not available (dependencies missing)", adapter.getFramework());
                continue;
            }
            
            discovered.add(adapter);
            ADAPTERS_BY_FRAMEWORK.put(adapter.getFramework(), adapter);
            log.info("Loaded adapter: {} ({})", adapter.getFramework(), adapter.getDescription());
        }
        
        // Sort by priority (highest first)
        ADAPTERS_BY_PRIORITY.addAll(discovered);
        ADAPTERS_BY_PRIORITY.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        
        initialized = true;
        log.info("ModelAdapterRegistry initialized with {} adapters", discovered.size());
    }
    
    /**
     * Adapts a native ML model to PredictiveModel with automatic adapter selection.
     * 
     * <p>Iterates through available adapters in priority order and returns
     * the first one that can adapt the model.</p>
     * 
     * @param model the native ML framework model
     * @return adapted PredictiveModel
     * @throws IllegalArgumentException if no adapter can handle the model
     * @throws IllegalStateException if no adapters available
     */
    public static PredictiveModel adapt(Object model) {
        return adapt(model, (Object[]) null);
    }
    
    /**
     * Adapts a model with optional configuration.
     * 
     * @param model the native model
     * @param config optional framework-specific configuration
     * @return adapted PredictiveModel
     * @throws IllegalArgumentException if no adapter can handle the model
     */
    public static PredictiveModel adapt(Object model, Object... config) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        
        if (ADAPTERS_BY_PRIORITY.isEmpty()) {
            throw new IllegalStateException("No adapters available. Check your dependencies.");
        }
        
        // Try each adapter in priority order
        for (ModelAdapter adapter : ADAPTERS_BY_PRIORITY) {
            if (adapter.canAdapt(model)) {
                log.debug("Using adapter {} for model type {}", 
                    adapter.getFramework(), model.getClass().getName());
                return adapter.adapt(model, config);
            }
        }
        
        throw new IllegalArgumentException(
            "No adapter found for model type: " + model.getClass().getName() + 
            ". Available adapters: " + getAvailableFrameworks());
    }
    
    /**
     * Adapts a model using a specific framework adapter.
     * 
     * @param model the native model
     * @param framework the framework name (e.g., "smile", "djl")
     * @return adapted PredictiveModel
     * @throws IllegalArgumentException if adapter not found or cannot handle model
     */
    public static PredictiveModel adapt(Object model, String framework) {
        return adapt(model, framework, null);
    }
    
    /**
     * Adapts a model using a specific framework adapter with configuration.
     * 
     * @param model the native model
     * @param framework the framework name
     * @param config optional configuration
     * @return adapted PredictiveModel
     * @throws IllegalArgumentException if adapter not found or cannot handle model
     */
    public static PredictiveModel adapt(Object model, String framework, Object... config) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }
        
        if (framework == null) {
            return adapt(model, config);
        }
        
        ModelAdapter adapter = ADAPTERS_BY_FRAMEWORK.get(framework);
        if (adapter == null) {
            throw new IllegalArgumentException(
                "Adapter not found for framework: " + framework + 
                ". Available: " + getAvailableFrameworks());
        }
        
        if (!adapter.canAdapt(model)) {
            throw new IllegalArgumentException(
                "Adapter for " + framework + " cannot handle model type: " + 
                model.getClass().getName());
        }
        
        return adapter.adapt(model, config);
    }
    
    /**
     * Returns list of available adapters with their metadata.
     * 
     * @return list of adapter information
     */
    public static List<ModelAdapterInfo> getAvailableAdapters() {
        return ADAPTERS_BY_PRIORITY.stream()
            .map(adapter -> new ModelAdapterInfo(
                adapter.getFramework(),
                adapter.getDescription(),
                adapter.getPriority(),
                adapter.isAvailable()
            ))
            .collect(Collectors.toUnmodifiableList());
    }
    
    /**
     * Returns list of available framework names.
     * 
     * @return comma-separated framework names
     */
    public static String getAvailableFrameworks() {
        return ADAPTERS_BY_FRAMEWORK.keySet().stream()
            .sorted()
            .collect(Collectors.joining(", "));
    }
    
    /**
     * Checks if a specific framework adapter is available.
     * 
     * @param framework the framework name
     * @return true if adapter is loaded and available
     */
    public static boolean isAvailable(String framework) {
        ModelAdapter adapter = ADAPTERS_BY_FRAMEWORK.get(framework);
        return adapter != null && adapter.isAvailable();
    }
    
    /**
     * Registers a custom adapter (for testing or manual registration).
     * 
     * @param adapter the adapter to register
     */
    public static void register(ModelAdapter adapter) {
        Objects.requireNonNull(adapter, "Adapter cannot be null");
        ADAPTERS_BY_FRAMEWORK.put(adapter.getFramework(), adapter);
        ADAPTERS_BY_PRIORITY.add(adapter);
        ADAPTERS_BY_PRIORITY.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        log.info("Registered custom adapter: {}", adapter.getFramework());
    }
    
    /**
     * Unregisters an adapter (primarily for testing).
     * 
     * @param framework the framework name
     */
    public static void unregister(String framework) {
        ModelAdapter adapter = ADAPTERS_BY_FRAMEWORK.remove(framework);
        if (adapter != null) {
            ADAPTERS_BY_PRIORITY.remove(adapter);
            log.info("Unregistered adapter: {}", framework);
        }
    }
    
    /**
     * Clears all registered adapters and reinitializes from ServiceLoader.
     */
    public static void reset() {
        ADAPTERS_BY_FRAMEWORK.clear();
        ADAPTERS_BY_PRIORITY.clear();
        initialized = false;
        initialize();
    }
    
    /**
     * Metadata about a registered adapter.
     */
    public static class ModelAdapterInfo {
        private final String framework;
        private final String description;
        private final int priority;
        private final boolean available;
        
        public ModelAdapterInfo(String framework, String description, int priority, boolean available) {
            this.framework = framework;
            this.description = description;
            this.priority = priority;
            this.available = available;
        }
        
        public String getFramework() { return framework; }
        public String getDescription() { return description; }
        public int getPriority() { return priority; }
        public boolean isAvailable() { return available; }
        
        @Override
        public String toString() {
            return String.format("%s (priority=%d): %s %s", 
                framework, priority, description, available ? "[AVAILABLE]" : "[UNAVAILABLE]");
        }
    }
}
