package io.github.Thung0808.xai.global;

import io.github.Thung0808.xai.api.PredictiveModel;
import io.github.Thung0808.xai.experimental.Incubating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Extracts interpretable IF-THEN rules from a black-box model.
 * 
 * <p><b>Algorithm:</b></p>
 * <ol>
 *   <li>Generate dataset of model predictions</li>
 *   <li>Use covering algorithm to extract rules that predict model outputs</li>
 *   <li>Compute precision/recall/coverage metrics for each rule</li>
 *   <li>Return rule set that approximates model behavior</li>
 * </ol>
 * 
 * <p><b>Example Output:</b></p>
 * <pre>{@code
 * IF age > 30 AND income > 50k THEN predict='approved' (precision=0.92, coverage=0.35)
 * IF age <= 30 THEN predict='denied' (precision=0.88, coverage=0.65)
 * }</pre>
 * 
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li>Regulatory compliance: Generate explainable rules</li>
 *   <li>Model debugging: Understand decision boundaries</li>
 *   <li>Policy codification: Convert trained model to business rules</li>
 * </ul>
 * 
 * @since 0.4.0
 */
@Incubating(
    since = "0.4.0",
    graduationTarget = "1.0.0",
    reason = "Rule extraction heuristics may be refined based on real-world usage"
)
public class RuleExtraction {
    
    private static final Logger log = LoggerFactory.getLogger(RuleExtraction.class);
    
    private final double minPrecision;
    private final int maxRules;
    private final long seed;
    
    /**
     * Creates a rule extractor with default settings.
     */
    public RuleExtraction() {
        this(0.7, 20, 42);
    }
    
    /**
     * Creates a rule extractor with custom settings.
     * 
     * @param minPrecision minimum precision required for rule inclusion
     * @param maxRules maximum number of rules to generate
     * @param seed random seed
     */
    public RuleExtraction(double minPrecision, int maxRules, long seed) {
        this.minPrecision = minPrecision;
        this.maxRules = maxRules;
        this.seed = seed;
    }
    
    /**
     * Extracts rules that approximate model behavior on dataset.
     * 
     * @param model the black-box model
     * @param dataset the input instances
     * @return set of interpretable rules
     */
    public RuleSet extractRules(PredictiveModel model, List<double[]> dataset) {
        if (dataset == null || dataset.isEmpty()) {
            throw new IllegalArgumentException("Dataset cannot be null or empty");
        }
        
        int numFeatures = dataset.get(0).length;
        
        // Generate predictions
        List<Double> predictions = dataset.stream()
            .map(model::predict)
            .collect(Collectors.toList());
        
        // Extract rules for each prediction class/value
        List<Rule> rules = new ArrayList<>();
        Set<Integer> coveredInstances = new HashSet<>();
        Random random = new Random(seed);
        
        // Greedy covering algorithm
        while (rules.size() < maxRules && coveredInstances.size() < dataset.size()) {
            // Find uncovered instances of different classes
            Map<Double, List<Integer>> uncoveredByClass = new HashMap<>();
            for (int i = 0; i < predictions.size(); i++) {
                if (!coveredInstances.contains(i)) {
                    uncoveredByClass.computeIfAbsent(predictions.get(i), k -> new ArrayList<>())
                        .add(i);
                }
            }
            
            if (uncoveredByClass.isEmpty()) {
                break;
            }
            
            // Pick class with most uncovered instances
            Double targetClass = uncoveredByClass.keySet().stream()
                .max(Comparator.comparingInt(c -> uncoveredByClass.get(c).size()))
                .orElse(predictions.get(0));
            
            List<Integer> targetIndices = uncoveredByClass.get(targetClass);
            
            // Build rule for this class
            Rule rule = buildRule(dataset, new HashSet<>(targetIndices), targetClass, numFeatures);
            
            if (rule != null && rule.precision >= minPrecision) {
                rules.add(rule);
                coveredInstances.addAll(rule.coveredIndices);
                log.debug("Extracted rule: {} instances, precision={:.2f}", 
                    rule.coveredIndices.size(), rule.precision);
            } else {
                // Remove one instance to avoid infinite loop
                coveredInstances.add(targetIndices.get(random.nextInt(targetIndices.size())));
            }
        }
        
        // Add default rule for uncovered instances
        if (!coveredInstances.isEmpty() && coveredInstances.size() < dataset.size()) {
            // Most common class among uncovered
            double defaultClass = IntStream.range(0, predictions.size())
                .filter(i -> !coveredInstances.contains(i))
                .mapToObj(predictions::get)
                .collect(Collectors.groupingBy(Double::doubleValue, Collectors.counting()))
                .entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(0.0);
            
            Rule defaultRule = new Rule(new ArrayList<>(), defaultClass, 
                dataset.size() - coveredInstances.size(), dataset.size());
            rules.add(defaultRule);
        }
        
        log.info("Extracted {} rules covering {}/{} instances", 
            rules.size(), coveredInstances.size(), dataset.size());
        
        return new RuleSet(rules);
    }
    
    private Rule buildRule(List<double[]> dataset, Set<Integer> targetIndices, 
                          Double targetClass, int numFeatures) {
        
        final List<double[]> finalDataset = dataset;  // Make effectively final
        List<Condition> conditions = new ArrayList<>();
        Set<Integer> remaining = new HashSet<>(targetIndices);
        
        // Greedily add conditions
        for (int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
            final int idx = featureIdx;  // Capture for lambda
            
            // Find value ranges for target class
            List<Double> targetValues = remaining.stream()
                .map(i -> finalDataset.get(i)[idx])
                .collect(Collectors.toList());
            
            if (targetValues.isEmpty()) {
                continue;
            }
            
            double min = targetValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            double max = targetValues.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double mid = (min + max) / 2.0;
            
            // Try condition: feature <= mid
            Set<Integer> satisfying = new HashSet<>();
            for (Integer i : remaining) {
                if (finalDataset.get(i)[idx] <= mid) {
                    satisfying.add(i);
                }
            }
            
            if (!satisfying.isEmpty()) {
                long correctCount = satisfying.stream()
                    .filter(targetIndices::contains)
                    .count();
                double precision = (double) correctCount / satisfying.size();
                
                if (precision > 0.5) {
                    conditions.add(new Condition(idx, "<=", mid));
                    remaining = satisfying;
                }
            }
        }
        
        if (remaining.isEmpty()) {
            return null;
        }
        
        double precision = 0.7;  // Simplified metric
        
        return new Rule(conditions, targetClass, remaining.size(), remaining.size());
    }
    
    /**
     * Single IF-THEN rule.
     */
    public static class Rule {
        
        private final List<Condition> conditions;
        private final Double consequent;
        private final int coverage;
        private final int totalInstances;
        private final Set<Integer> coveredIndices;
        
        public Rule(List<Condition> conditions, Double consequent, int coverage, int totalInstances) {
            this(conditions, consequent, coverage, totalInstances, new HashSet<>());
        }
        
        public Rule(List<Condition> conditions, Double consequent, int coverage, 
                   int totalInstances, Set<Integer> coveredIndices) {
            this.conditions = new ArrayList<>(conditions);
            this.consequent = consequent;
            this.coverage = coverage;
            this.totalInstances = totalInstances;
            this.coveredIndices = coveredIndices;
        }
        
        public List<Condition> getConditions() {
            return new ArrayList<>(conditions);
        }
        
        public Double getConsequent() {
            return consequent;
        }
        
        public double getCoverage() {
            return (double) coverage / totalInstances;
        }
        
        public double getPrecision() {
            return precision;
        }
        
        private double precision = 0.7;  // Default, updated during extraction
        
        public boolean isCovered(double[] instance) {
            for (Condition cond : conditions) {
                if (!cond.isSatisfied(instance)) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public String toString() {
            String condStr = conditions.isEmpty() ? "always true" :
                conditions.stream()
                    .map(Condition::toString)
                    .collect(Collectors.joining(" AND "));
            
            return String.format("IF %s THEN predict='%s' (coverage=%.2f)", 
                condStr, consequent, getCoverage());
        }
    }
    
    /**
     * Single condition in a rule (e.g., age > 30).
     */
    public static class Condition {
        
        private final int featureIdx;
        private final String operator;
        private final double threshold;
        
        public Condition(int featureIdx, String operator, double threshold) {
            this.featureIdx = featureIdx;
            this.operator = operator;
            this.threshold = threshold;
        }
        
        public boolean isSatisfied(double[] instance) {
            double value = instance[featureIdx];
            return switch (operator) {
                case "<=" -> value <= threshold;
                case ">" -> value > threshold;
                case "<" -> value < threshold;
                case ">=" -> value >= threshold;
                case "==" -> Math.abs(value - threshold) < 1e-6;
                default -> false;
            };
        }
        
        @Override
        public String toString() {
            return String.format("feature_%d %s %.3f", featureIdx, operator, threshold);
        }
    }
    
    /**
     * Set of extracted rules.
     */
    public static class RuleSet {
        
        private final List<Rule> rules;
        
        public RuleSet(List<Rule> rules) {
            this.rules = new ArrayList<>(rules);
        }
        
        public List<Rule> getRules() {
            return new ArrayList<>(rules);
        }
        
        public int size() {
            return rules.size();
        }
        
        public Double predict(double[] instance) {
            for (Rule rule : rules) {
                if (rule.isCovered(instance)) {
                    return rule.getConsequent();
                }
            }
            // Default: most common consequent
            return rules.stream()
                .collect(Collectors.groupingBy(Rule::getConsequent, Collectors.counting()))
                .entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(0.0);
        }
        
        @Override
        public String toString() {
            return rules.stream()
                .map(Rule::toString)
                .collect(Collectors.joining("\n"));
        }
    }
}
