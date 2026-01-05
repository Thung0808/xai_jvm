package io.github.Thung0808.xai.api;

import java.lang.annotation.*;

/**
 * Marks an API as stable and backward-compatible.
 * 
 * <p><b>Contract:</b></p>
 * <ul>
 *   <li>No breaking changes within same MAJOR version</li>
 *   <li>Deprecated methods remain for at least one MAJOR version</li>
 *   <li>Safe to use in production</li>
 * </ul>
 * 
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @Stable(since = "1.0.0")
 * public interface Explainer<M> {
 *     Explanation explain(M model, double[] input);
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface Stable {
    
    /**
     * Version when this API became stable.
     */
    String since();
    
    /**
     * Additional documentation about stability guarantees.
     */
    String value() default "";
}
