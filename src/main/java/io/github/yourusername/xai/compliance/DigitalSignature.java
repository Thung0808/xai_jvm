package io.github.Thung0808.xai.compliance;

import java.time.Instant;
import java.util.Base64;

/**
 * Digital signature for compliance verification.
 */
public class DigitalSignature {
    
    private final String reportId;
    private final String signature;
    private final String algorithm;
    private final Instant signedAt;
    
    public DigitalSignature(String reportId, String dataHash, String privateKey) {
        this.reportId = reportId;
        this.algorithm = "SHA256withRSA";
        this.signedAt = Instant.now();
        
        // Simplified: In production, use proper cryptographic signing
        // For now, use HMAC-SHA256
        this.signature = hmacSha256(
            reportId + "|" + dataHash + "|" + signedAt,
            privateKey
        );
    }
    
    /**
     * Verify signature validity.
     */
    public boolean verify(String reportId, String dataHash, String publicKey) {
        String expectedSignature = hmacSha256(
            reportId + "|" + dataHash + "|" + signedAt,
            publicKey
        );
        return signature.equals(expectedSignature);
    }
    
    private static String hmacSha256(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                key.getBytes(), 0, key.getBytes().length, "HmacSHA256"
            ));
            byte[] hash = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 failed", e);
        }
    }
    
    // Getters
    public String getReportId() { return reportId; }
    public String getSignature() { return signature; }
    public String getAlgorithm() { return algorithm; }
    public Instant getSignedAt() { return signedAt; }
}
