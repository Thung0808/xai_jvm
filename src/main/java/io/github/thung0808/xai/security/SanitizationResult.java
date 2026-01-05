package io.github.thung0808.xai.security;

/**
 * Result of explanation sanitization.
 */
public class SanitizationResult {
    private final boolean isValid;
    private final String message;
    private final boolean isSuspicious;
    
    public SanitizationResult(boolean isValid, String message, boolean isSuspicious) {
        this.isValid = isValid;
        this.message = message;
        this.isSuspicious = isSuspicious;
    }
    
    public boolean isValid() { return isValid; }
    public String getMessage() { return message; }
    public boolean isSuspicious() { return isSuspicious; }
    
    @Override
    public String toString() {
        return String.format(
            "SanitizationResult{valid=%s, suspicious=%s, message='%s'}",
            isValid, isSuspicious, message
        );
    }
}


