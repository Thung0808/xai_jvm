package io.github.Thung0808.xai.spring.boot.autoconfigure;

import java.lang.annotation.*;

/**
 * Annotation to mark methods for automatic explanation generation and logging.
 * 
 * <p>When applied to a method, XAI Core will automatically:
 * <ol>
 *   <li>Capture input parameters and predicted output</li>
 *   <li>Generate explanation using configured explainer</li>
 *   <li>Log explanation to console or database</li>
 *   <li>Track metrics (latency, trust score, robustness)</li>
 *   <li>Detect potential explanation manipulation</li>
 * </ol>
 * 
 * <p>Example usage:
 * <pre>{@code
 * @Service
 * public class CreditScoringService {
 *     @Autowired
 *     private Explainer<CreditModel> explainer;
 *     
 *     @Explainable(
 *         model = "creditModel",
 *         importance = true,
 *         logOutput = true,
 *         sanitize = true
 *     )
 *     public double scoreLoan(LoanApplication app) {
 *         // Business logic...
 *         return creditModel.predict(featureVector);
 *     }
 * }
 * 
 * // Output:
 * // 2026-01-05T14:23:45.123Z [INFO] Explanation for scoreLoan:
 * // Prediction: 0.78 (Approve with caution)
 * // Top features: credit_score (+0.35), annual_income (+0.25)
 * // Trust: 0.92, Robustness: 0.88
 * }</pre>
 * 
 * @since 1.1.0-alpha
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Explainable {
    
    /**
     * The model bean name or type to use for explanation.
     * If not specified, uses the default configured explainer.
     */
    String model() default "";
    
    /**
     * Whether to log feature importances/attributions.
     */
    boolean importance() default true;
    
    /**
     * Whether to log the explanation to console.
     */
    boolean logOutput() default true;
    
    /**
     * Whether to sanitize (check for manipulation) the explanation.
     */
    boolean sanitize() default true;
    
    /**
     * Threshold for flagging as suspicious (0.0-1.0).
     * Default: 0.30 (30% deviation).
     */
    double manipulationThreshold() default 0.30;
    
    /**
     * Whether to persist explanation to database.
     */
    boolean persist() default false;
    
    /**
     * Custom description for logging.
     */
    String description() default "";
}
