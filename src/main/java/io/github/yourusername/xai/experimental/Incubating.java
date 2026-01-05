package io.github.Thung0808.xai.experimental;

import java.lang.annotation.*;

/**
 * Marks an API as experimental/incubating.
 * 
 * <p><b>Warning:</b> APIs marked with this annotation:</p>
 * <ul>
 *   <li>May have breaking changes in MINOR versions</li>
 *   <li>May be removed without deprecation period</li>
 *   <li>Not recommended for production use</li>
 * </ul>
 * 
 * <p><b>Graduation Path:</b> Experimental → {@link io.github.Thung0808.xai.api.Stable}</p>
 * 
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @Incubating(since = "0.3.0", graduationTarget = "1.0.0")
 * public interface CounterfactualFinder {
 *     CounterfactualResult findCounterfactual(...);
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PACKAGE})
public @interface Incubating {
    
    /**
     * Version when this API was introduced.
     */
    String since();
    
    /**
     * Target version for graduation to stable (if known).
     */
    String graduationTarget() default "";
    
    /**
     * Reason for experimental status and known limitations.
     */
    String reason() default "API under active development";
}
