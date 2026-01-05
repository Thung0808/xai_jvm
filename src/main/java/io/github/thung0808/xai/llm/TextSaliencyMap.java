package io.github.thung0808.xai.llm;

import io.github.thung0808.xai.api.Experimental;
import java.util.*;

/**
 * Saliency Maps for text explanation.
 * Computes importance of each token in the input.
 * 
 * <p><b>Experimental:</b> This API is experimental and may change in future releases.
 * 
 * @since 1.1.0
 */
@Experimental(since = "1.1.0")
public class TextSaliencyMap {
    private String[] tokens;
    private double[] saliencies;
    private String targetClass;
    
    public TextSaliencyMap(String text, String targetClass) {
        this.tokens = text.split("\\s+");
        this.saliencies = new double[tokens.length];
        this.targetClass = targetClass;
    }
    
    /**
     * Compute token importance using gradient-based method
     * Approximates integrated gradients for text
     */
    public void computeTokenImportance(TokenScorer scorer) {
        double baselineScore = scorer.scoreTokens(new double[tokens.length]);
        
        for (int i = 0; i < tokens.length; i++) {
            // Perturbation importance: importance = impact of removing token
            double[] perturbed = new double[tokens.length];
            Arrays.fill(perturbed, 1.0);
            perturbed[i] = 0;  // Mask this token
            
            double perturbedScore = scorer.scoreTokens(perturbed);
            saliencies[i] = Math.abs(perturbedScore - baselineScore);
        }
        
        // Normalize
        double sum = Arrays.stream(saliencies).sum();
        if (sum > 0) {
            for (int i = 0; i < saliencies.length; i++) {
                saliencies[i] /= sum;
            }
        }
    }
    
    /**
     * Get top K important tokens
     */
    public List<TokenSaliency> getTopTokens(int k) {
        List<TokenSaliency> importances = new ArrayList<>();
        
        for (int i = 0; i < tokens.length; i++) {
            importances.add(new TokenSaliency(tokens[i], i, saliencies[i]));
        }
        
        importances.sort((a, b) -> Double.compare(b.saliency, a.saliency));
        return importances.subList(0, Math.min(k, importances.size()));
    }
    
    /**
     * Highlight tokens with color codes
     */
    public String highlightTokens() {
        StringBuilder sb = new StringBuilder();
        double max = Arrays.stream(saliencies).max().orElse(1.0);
        
        for (int i = 0; i < tokens.length; i++) {
            int intensity = (int)((saliencies[i] / max) * 100);
            String colored = colorizeToken(tokens[i], intensity);
            sb.append(colored).append(" ");
        }
        
        return sb.toString();
    }
    
    private String colorizeToken(String token, int intensity) {
        // ANSI color codes for gradient
        if (intensity < 20) return token;  // Low importance
        if (intensity < 40) return "\u001B[92m" + token + "\u001B[0m";      // Light green
        if (intensity < 60) return "\u001B[33m" + token + "\u001B[0m";      // Yellow
        if (intensity < 80) return "\u001B[38;5;208m" + token + "\u001B[0m"; // Orange
        return "\u001B[91m" + token + "\u001B[0m";                           // Red
    }
    
    /**
     * Export to JSON for web visualization
     */
    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\"tokens\": [");
        
        for (int i = 0; i < tokens.length; i++) {
            json.append("{\"text\": \"").append(tokens[i])
                .append("\", \"saliency\": ").append(String.format("%.3f", saliencies[i]))
                .append("}");
            if (i < tokens.length - 1) json.append(", ");
        }
        
        json.append("], \"targetClass\": \"").append(targetClass).append("\"}");
        return json.toString();
    }
    
    /**
     * Token-saliency pair
     */
    public static class TokenSaliency {
        public String token;
        public int position;
        public double saliency;
        
        public TokenSaliency(String token, int pos, double sal) {
            this.token = token;
            this.position = pos;
            this.saliency = sal;
        }
        
        @Override
        public String toString() {
            return String.format("\"%s\" (%.2f%%)", token, saliency * 100);
        }
    }
    
    /**
     * Interface for scoring tokens
     */
    public interface TokenScorer {
        double scoreTokens(double[] tokenMask);
    }
}
