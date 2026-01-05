package io.github.Thung0808.xai.api;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Comprehensive metadata for explanation reproducibility and auditability.
 * 
 * <p><b>Purpose:</b> Enables 100% reproducibility of explanations and
 * supports regulatory audit requirements (GDPR Article 22, EU AI Act).</p>
 * 
 * <p><b>Reproducibility Contract:</b> Given identical metadata, the explanation
 * can be regenerated with identical results.</p>
 * 
 * <p><b>Enhanced Fields (v0.3.0):</b></p>
 * <ul>
 *   <li>featureSchemaHash: Detects schema changes</li>
 *   <li>modelSignature: Identifies model version</li>
 *   <li>hardwareInfo: Tracks compute environment</li>
 *   <li>algorithmVersion: Tracks explainer algorithm version</li>
 * </ul>
 *
 * @since 0.1.0
 */
@Stable(since = "0.3.0")
public final class ExplanationMetadata {
    
    // Core fields (v0.1.0)
    private final String explainerName;
    private final Instant timestamp;
    private final long seed;
    private final String libraryVersion;
    private final int trials;
    
    // Enhanced fields (v0.3.0) - optional for backward compatibility
    private final String featureSchemaHash;
    private final String modelSignature;
    private final String hardwareInfo;
    private final String algorithmVersion;
    private final Map<String, Object> customMetadata;
    
    private ExplanationMetadata(Builder builder) {
        this.explainerName = Objects.requireNonNull(builder.explainerName, "Explainer name is required");
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.seed = builder.seed;
        this.libraryVersion = builder.libraryVersion != null ? builder.libraryVersion : "0.3.0";
        this.trials = builder.trials;
        
        // Enhanced fields
        this.featureSchemaHash = builder.featureSchemaHash;
        this.modelSignature = builder.modelSignature;
        this.hardwareInfo = builder.hardwareInfo;
        this.algorithmVersion = builder.algorithmVersion;
        this.customMetadata = builder.customMetadata != null 
            ? Map.copyOf(builder.customMetadata) 
            : Map.of();
    }
    
    public String explainerName() {
        return explainerName;
    }
    
    public Instant timestamp() {
        return timestamp;
    }
    
    public long seed() {
        return seed;
    }
    
    public String libraryVersion() {
        return libraryVersion;
    }
    
    public int trials() {
        return trials;
    }
    
    public String featureSchemaHash() {
        return featureSchemaHash;
    }
    
    public String modelSignature() {
        return modelSignature;
    }
    
    public String hardwareInfo() {
        return hardwareInfo;
    }
    
    public String algorithmVersion() {
        return algorithmVersion;
    }
    
    public Map<String, Object> customMetadata() {
        return customMetadata;
    }
    
    /**
     * Exports complete metadata as structured map for serialization.
     */
    public Map<String, Object> toMap() {
        var map = new java.util.HashMap<String, Object>();
        map.put("explainerName", explainerName);
        map.put("timestamp", timestamp.toString());
        map.put("seed", seed);
        map.put("libraryVersion", libraryVersion);
        map.put("trials", trials);
        
        if (featureSchemaHash != null) map.put("featureSchemaHash", featureSchemaHash);
        if (modelSignature != null) map.put("modelSignature", modelSignature);
        if (hardwareInfo != null) map.put("hardwareInfo", hardwareInfo);
        if (algorithmVersion != null) map.put("algorithmVersion", algorithmVersion);
        if (!customMetadata.isEmpty()) map.put("custom", customMetadata);
        
        return map;
    }
    
    public static Builder builder(String explainerName) {
        return new Builder(explainerName);
    }
    
    @Override
    public String toString() {
        return String.format("ExplanationMetadata{explainer='%s', version='%s', timestamp=%s, seed=%d, trials=%d}",
            explainerName, libraryVersion, timestamp, seed, trials);
    }
    
    /**
     * Builder for ExplanationMetadata.
     */
    public static final class Builder {
        private final String explainerName;
        private Instant timestamp;
        private long seed = 42;
        private String libraryVersion;
        private int trials = 1;
        
        // Enhanced fields (v0.3.0)
        private String featureSchemaHash;
        private String modelSignature;
        private String hardwareInfo;
        private String algorithmVersion;
        private Map<String, Object> customMetadata;
        
        private Builder(String explainerName) {
            this.explainerName = explainerName;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }
        
        public Builder libraryVersion(String version) {
            this.libraryVersion = version;
            return this;
        }
        
        public Builder trials(int trials) {
            if (trials < 1) {
                throw new IllegalArgumentException("Trials must be positive");
            }
            this.trials = trials;
            return this;
        }
        
        /**
         * Sets feature schema hash for detecting schema changes.
         * Compute using feature names and types.
         * 
         * @since 0.3.0
         */
        public Builder featureSchemaHash(String hash) {
            this.featureSchemaHash = hash;
            return this;
        }
        
        /**
         * Sets model signature/version identifier.
         * Use model checksum, version tag, or training timestamp.
         * 
         * @since 0.3.0
         */
        public Builder modelSignature(String signature) {
            this.modelSignature = signature;
            return this;
        }
        
        /**
         * Sets hardware/compute environment info.
         * Example: "AWS c5.xlarge, Java 21.0.1, Linux x86_64"
         * 
         * @since 0.3.0
         */
        public Builder hardwareInfo(String info) {
            this.hardwareInfo = info;
            return this;
        }
        
        /**
         * Sets explainer algorithm version.
         * Use semantic versioning for algorithm changes.
         * 
         * @since 0.3.0
         */
        public Builder algorithmVersion(String version) {
            this.algorithmVersion = version;
            return this;
        }
        
        /**
         * Sets custom metadata for application-specific fields.
         * 
         * @since 0.3.0
         */
        public Builder customMetadata(Map<String, Object> metadata) {
            this.customMetadata = metadata;
            return this;
        }
        
        public ExplanationMetadata build() {
            return new ExplanationMetadata(this);
        }
    }
}
