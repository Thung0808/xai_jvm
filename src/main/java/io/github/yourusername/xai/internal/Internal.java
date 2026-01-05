package io.github.Thung0808.xai.internal;

import java.lang.annotation.*;

/**
 * Marks internal APIs not intended for public use.
 * 
 * <p><b>Restrictions:</b></p>
 * <ul>
 *   <li>No backward compatibility guarantees</li>
 *   <li>Can change or be removed at any time</li>
 *   <li>Not part of public API surface</li>
 *   <li>Should not be used outside this library</li>
 * </ul>
 * 
 * <p><b>Visibility:</b> Classes marked as internal should ideally have
 * package-private visibility. When public visibility is required for
 * technical reasons (e.g., cross-package access), this annotation
 * explicitly marks them as non-public API.</p>
 * 
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @Internal
 * public class VectorMathUtils {
 *     // Implementation details
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PACKAGE})
public @interface Internal {
    
    /**
     * Reason why this is internal and not public API.
     */
    String reason() default "Implementation detail";
}
