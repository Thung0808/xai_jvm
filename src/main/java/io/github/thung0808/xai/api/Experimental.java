package io.github.thung0808.xai.api;

import java.lang.annotation.*;

/**
 * Indicates that an API is experimental and may change in future releases.
 * 
 * <p>Experimental APIs are subject to change without notice in minor versions.
 * They are included to gather feedback from users before stabilizing the API.
 * 
 * <p>Use experimental APIs at your own risk in production environments.
 * 
 * <p><b>Stability Timeline:</b>
 * <ul>
 *   <li>Experimental for at least 6 months</li>
 *   <li>Promoted to stable after user feedback integration</li>
 *   <li>Or removed if deemed not useful</li>
 * </ul>
 * 
 * <p><b>Example:</b>
 * <pre>{@code
 * @Experimental(since = "1.1.0")
 * public class CausalGraph {
 *     // API may change in 1.2.0, 1.3.0, etc.
 * }
 * }</pre>
 * 
 * @see <a href="https://github.com/Thung0808/xai-core/blob/main/STABILITY.md">Stability Guarantees</a>
 * @since 1.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.PACKAGE})
public @interface Experimental {
    /**
     * Version when this API was introduced as experimental.
     * @return version string (e.g., "1.1.0")
     */
    String since();
    
    /**
     * Optional reason why this API is experimental.
     * @return reason description
     */
    String reason() default "API is under active development and may change based on user feedback";
}
