package io.github.Thung0808.xai.adapter.smile;

import io.github.Thung0808.xai.adapter.ModelAdapter;
import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.api.Stable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for Smile ML library models.
 * 
 * <p>Universal adapter that wraps Smile classifier models using reflection.</p>
 * 
 * <p>Auto-discovered via ServiceLoader when xai-adapter-smile is on classpath.</p>
 *
 * @since 0.4.0
 */
@Stable(since = "0.4.0")
public class SmileModelAdapter implements ModelAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(SmileModelAdapter.class);
    
    @Override
    public String getFramework() {
        return "smile";
    }
    
    @Override
    public boolean canAdapt(Object model) {
        if (model == null) {
            return false;
        }
        
        String className = model.getClass().getName();
        return className.startsWith("smile.classification.") 
            || className.startsWith("smile.regression.");
    }
    
    @Override
    public PredictiveModel adapt(Object model, Object... config) {
        return new SmileWrapper(model);
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            Class.forName("smile.classification.Classifier");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public String getDescription() {
        return "Universal adapter for Smile ML classifiers and regressors";
    }
    
    /**
     * Generic wrapper for Smile models using reflection.
     */
    @Stable(since = "0.4.0")
    private static class SmileWrapper implements PredictiveModel {
        private final Object smileModel;
        
        SmileWrapper(Object model) {
            this.smileModel = model;
        }
        
        @Override
        public double predict(double[] input) {
            if (input == null) {
                throw new IllegalArgumentException("Input cannot be null");
            }
            
            try {
                // Try to call predict(double[])
                var method = smileModel.getClass().getMethod("predict", double[].class);
                Object result = method.invoke(smileModel, (Object) input);
                
                // Handle different return types
                if (result instanceof Double) {
                    return (Double) result;
                } else if (result instanceof Integer) {
                    return ((Integer) result).doubleValue();
                } else if (result instanceof int[]) {
                    return ((int[]) result)[0];
                } else if (result instanceof double[]) {
                    double[] probs = (double[]) result;
                    return probs.length > 0 ? probs[0] : 0.5;
                }
                
                // Fallback
                return 0.5;
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                log.error("Cannot invoke predict on {}: {}", smileModel.getClass().getName(), e.getMessage());
                throw new IllegalStateException("Cannot invoke predict method", e);
            }
        }
        
        @Override
        public int getFeatureCount() {
            try {
                // Try to get number of features using reflection
                var method = smileModel.getClass().getMethod("numFeatures");
                Object result = method.invoke(smileModel);
                if (result instanceof Integer) {
                    return (Integer) result;
                }
            } catch (Exception e) {
                // Ignore, return -1
            }
            return -1;
        }
    }
}
