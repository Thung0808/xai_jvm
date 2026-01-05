package io.github.thung0808.xai.llm;

import io.github.thung0808.xai.api.Experimental;
import java.util.*;

/**
 * Attention Map Extractor for Transformer models.
 * Retrieves attention weights to explain token relationships.
 * 
 * <p><b>Experimental:</b> This API is experimental and may change in future releases.
 * 
 * @since 1.1.0
 */
@Experimental(since = "1.1.0")
public class AttentionMapExtractor {
    
    /**
     * Represents attention between tokens
     */
    public static class AttentionHead {
        public String[] tokens;
        public double[][] attentionMatrix;  // N x N matrix
        public int layerId;
        public int headId;
        
        public AttentionHead(String[] tokens, double[][] matrix, int layer, int head) {
            this.tokens = tokens;
            this.attentionMatrix = matrix;
            this.layerId = layer;
            this.headId = head;
        }
    }
    
    /**
     * Extract attention weights from model output
     * In production, this would connect to ONNX Runtime or similar
     */
    public static AttentionHead extractAttentionHead(String[] tokens, 
                                                      double[][] rawAttention,
                                                      int layer, int head) {
        return new AttentionHead(tokens, normalizeAttention(rawAttention), layer, head);
    }
    
    /**
     * Normalize attention matrix (softmax rows)
     */
    private static double[][] normalizeAttention(double[][] matrix) {
        int n = matrix.length;
        double[][] normalized = new double[n][n];
        
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < n; j++) {
                sum += Math.exp(matrix[i][j]);
            }
            for (int j = 0; j < n; j++) {
                normalized[i][j] = Math.exp(matrix[i][j]) / sum;
            }
        }
        
        return normalized;
    }
    
    /**
     * Get most attended tokens for a query token
     */
    public static List<TokenAttention> getMostAttendedTokens(AttentionHead head, 
                                                             int queryTokenIdx, 
                                                             int topK) {
        List<TokenAttention> attended = new ArrayList<>();
        double[] queryAttention = head.attentionMatrix[queryTokenIdx];
        
        for (int i = 0; i < queryAttention.length; i++) {
            attended.add(new TokenAttention(head.tokens[i], i, queryAttention[i]));
        }
        
        attended.sort((a, b) -> Double.compare(b.weight, a.weight));
        return attended.subList(0, Math.min(topK, attended.size()));
    }
    
    /**
     * Token attention pair
     */
    public static class TokenAttention {
        public String token;
        public int tokenIdx;
        public double weight;
        
        public TokenAttention(String token, int idx, double weight) {
            this.token = token;
            this.tokenIdx = idx;
            this.weight = weight;
        }
        
        @Override
        public String toString() {
            return String.format("%s(%.3f)", token, weight);
        }
    }
    
    /**
     * Visualize attention matrix as ASCII
     */
    public static String visualizeAttention(AttentionHead head, int maxTokens) {
        StringBuilder sb = new StringBuilder();
        int n = Math.min(maxTokens, head.tokens.length);
        
        sb.append("Attention Head Layer ").append(head.layerId)
          .append(" Head ").append(head.headId).append(":\n");
        
        // Print tokens
        sb.append("       ");
        for (int j = 0; j < n; j++) {
            sb.append(String.format("%6s", head.tokens[j]));
        }
        sb.append("\n");
        
        // Print attention weights
        for (int i = 0; i < n; i++) {
            sb.append(String.format("%-6s", head.tokens[i]));
            for (int j = 0; j < n; j++) {
                double weight = head.attentionMatrix[i][j];
                char bar = getBarChar(weight);
                sb.append(String.format("%6c", bar));
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    private static char getBarChar(double weight) {
        if (weight < 0.1) return ' ';
        if (weight < 0.3) return '░';
        if (weight < 0.6) return '▒';
        return '█';
    }
}
