package io.github.thung0808.xai.causal;

import io.github.thung0808.xai.api.Experimental;
import java.util.*;

/**
 * Directed Acyclic Graph (DAG) for causal inference.
 * Represents causal relationships between variables.
 * 
 * <p><b>Experimental:</b> This API is experimental and may change in future releases.
 * Use {@link #addEdge(String, String, double)} to build causal graphs.
 * 
 * @since 1.1.0
 */
@Experimental(since = "1.1.0")
public class CausalGraph {
    private final Map<String, Set<String>> adjacencyList;  // variable -> set of children
    private final Map<String, Double> edgeWeights;         // edge -> weight
    
    public CausalGraph() {
        this.adjacencyList = new HashMap<>();
        this.edgeWeights = new HashMap<>();
    }
    
    /**
     * Add a causal edge from source to target.
     * Meaning: source causally affects target
     */
    public void addEdge(String source, String target, double weight) {
        adjacencyList.computeIfAbsent(source, k -> new HashSet<>()).add(target);
        adjacencyList.computeIfAbsent(target, k -> new HashSet<>());
        edgeWeights.put(source + "->" + target, weight);
    }
    
    /**
     * Get all causal parents (confounders) of a variable
     */
    public Set<String> getParents(String variable) {
        Set<String> parents = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : adjacencyList.entrySet()) {
            if (entry.getValue().contains(variable)) {
                parents.add(entry.getKey());
            }
        }
        return parents;
    }
    
    /**
     * Get all causal children (effects) of a variable
     */
    public Set<String> getChildren(String variable) {
        return adjacencyList.getOrDefault(variable, new HashSet<>());
    }
    
    /**
     * Check if path exists from source to target
     */
    public boolean pathExists(String source, String target) {
        Set<String> visited = new HashSet<>();
        return dfsPath(source, target, visited);
    }
    
    private boolean dfsPath(String current, String target, Set<String> visited) {
        if (current.equals(target)) return true;
        visited.add(current);
        
        for (String child : getChildren(current)) {
            if (!visited.contains(child) && dfsPath(child, target, visited)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Compute backdoor criterion adjustment set (for confounding control)
     * Returns variables that should be conditioned on to block confounding paths
     */
    public Set<String> getAdjustmentSet(String treatment, String outcome) {
        Set<String> confounders = new HashSet<>();
        
        // Find common ancestors (confounders)
        Set<String> treatmentAncestors = getAncestors(treatment);
        Set<String> outcomeAncestors = getAncestors(outcome);
        treatmentAncestors.retainAll(outcomeAncestors);
        confounders.addAll(treatmentAncestors);
        
        return confounders;
    }
    
    /**
     * Get all ancestors of a variable
     */
    public Set<String> getAncestors(String variable) {
        Set<String> ancestors = new HashSet<>();
        Set<String> visited = new HashSet<>();
        dfsAncestors(variable, ancestors, visited);
        return ancestors;
    }
    
    private void dfsAncestors(String variable, Set<String> ancestors, Set<String> visited) {
        for (String parent : getParents(variable)) {
            if (!visited.contains(parent)) {
                ancestors.add(parent);
                visited.add(parent);
                dfsAncestors(parent, ancestors, visited);
            }
        }
    }
    
    /**
     * Export to JSON for visualization
     */
    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\"nodes\": [");
        
        // Add nodes
        List<String> nodes = new ArrayList<>(adjacencyList.keySet());
        for (int i = 0; i < nodes.size(); i++) {
            json.append("{\"id\": \"").append(nodes.get(i)).append("\"}");
            if (i < nodes.size() - 1) json.append(", ");
        }
        
        json.append("], \"edges\": [");
        
        // Add edges
        List<String> edgeKeys = new ArrayList<>(edgeWeights.keySet());
        for (int i = 0; i < edgeKeys.size(); i++) {
            String edge = edgeKeys.get(i);
            String[] parts = edge.split("->");
            json.append("{\"source\": \"").append(parts[0])
                .append("\", \"target\": \"").append(parts[1])
                .append("\", \"weight\": ").append(edgeWeights.get(edge))
                .append("}");
            if (i < edgeKeys.size() - 1) json.append(", ");
        }
        
        json.append("]}");
        return json.toString();
    }
    
    @Override
    public String toString() {
        return "CausalGraph{" +
                "nodes=" + adjacencyList.keySet() +
                ", edges=" + edgeWeights.size() +
                '}';
    }
}
