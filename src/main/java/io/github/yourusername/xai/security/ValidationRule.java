package io.github.Thung0808.xai.security;

import io.github.Thung0808.xai.api.*;

/**
 * Validation rule for custom explanation checks.
 */
public interface ValidationRule {
    /**
     * Validate explanation against custom rule.
     * @return true if explanation passes validation
     */
    boolean validate(Explanation explanation, double[] instance);
    
    /**
     * Get human-readable error message if validation fails.
     */
    String getErrorMessage();
}
