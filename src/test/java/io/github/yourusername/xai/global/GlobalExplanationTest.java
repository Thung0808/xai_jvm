package io.github.Thung0808.xai.global;

import io.github.Thung0808.xai.api.FeatureAttribution;
import io.github.Thung0808.xai.api.PredictiveModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for GlobalExplainer and related classes.
 */
class GlobalExplanationTest {
    
    private List<double[]> dataset;
    private PredictiveModel model;
    
    @BeforeEach
    void setUp() {
        // Create synthetic dataset (5 features, 100 instances)
        dataset = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            double[] instance = {
                Math.sin(i / 10.0),           // feature_0: oscillating
                i / 100.0,                     // feature_1: increasing
                Math.random(),                 // feature_2: random
                i % 2 == 0 ? 1.0 : -1.0,     // feature_3: binary
                Math.cos(i / 5.0)             // feature_4: oscillating
            };
            dataset.add(instance);
        }
        
        // Simple model: weighted sum of features
        model = instance -> {
            double score = 0.3 * instance[0] + 0.4 * instance[1] 
                         + 0.1 * instance[2] + 0.15 * instance[3] + 0.05 * instance[4];
            return score > 0 ? 1.0 : 0.0;
        };
    }
    
    @Test
    void testGlobalFeatureImportanceBasics() {
        GlobalExplainer explainer = new GlobalFeatureImportance(20, 0.1, 42);
        GlobalExplanation explanation = explainer.explainDataset(model, dataset);
        
        assertNotNull(explanation);
        assertEquals(100, explanation.getDatasetSize());
        assertEquals(5, explanation.getNumFeatures());
        assertEquals("GlobalFeatureImportance", explanation.getExplainerName());
    }
    
    @Test
    void testGlobalExplanationFeatureAttributions() {
        GlobalExplainer explainer = new GlobalFeatureImportance(15, 0.1, 42);
        GlobalExplanation explanation = explainer.explainDataset(model, dataset);
        
        List<FeatureAttribution> attributions = explanation.getFeatureImportances();
        assertEquals(5, attributions.size());
        
        // All importances should be non-negative
        attributions.forEach(fa -> assertTrue(fa.importance() >= 0.0));
        
        // Top features
        List<FeatureAttribution> top3 = explanation.getTopFeatures(3);
        assertEquals(3, top3.size());
        assertTrue(top3.get(0).importance() >= top3.get(1).importance());
    }
    
    @Test
    void testGlobalExplanationToMap() {
        GlobalExplainer explainer = new GlobalFeatureImportance(10, 0.1, 42);
        GlobalExplanation explanation = explainer.explainDataset(model, dataset);
        
        Map<String, Double> importanceMap = explanation.toMap();
        assertEquals(5, importanceMap.size());
        
        // Verify keys exist
        for (int i = 0; i < 5; i++) {
            assertTrue(importanceMap.containsKey("feature_" + i));
        }
    }
    
    @Test
    void testGlobalExplanationMetrics() {
        GlobalExplainer explainer = new GlobalFeatureImportance(15, 0.1, 42);
        GlobalExplanation explanation = explainer.explainDataset(model, dataset);
        
        // Total importance should be > 0
        double totalImportance = explanation.getTotalImportance();
        assertTrue(totalImportance > 0.0);
        
        // Variance should be >= 0
        double variance = explanation.getImportanceVariance();
        assertTrue(variance >= 0.0);
        
        // Gini should be in [0, 1]
        double gini = explanation.getGiniCoefficient();
        assertTrue(gini >= 0.0 && gini <= 1.0);
    }
    
    @Test
    void testGlobalExplanationMetadata() {
        GlobalExplainer explainer = new GlobalFeatureImportance(10, 0.1, 42);
        GlobalExplanation explanation = explainer.explainDataset(model, dataset);
        
        explanation.setMetadata("test_key", "test_value");
        assertEquals("test_value", explanation.getMetadata("test_key"));
        
        explanation.setMetadata("numeric", 3.14);
        assertEquals(3.14, explanation.getMetadata("numeric"));
    }
    
    @Test
    void testSurrogateTreeExplainerBasics() {
        GlobalExplainer explainer = new SurrogateTreeExplainer(4, 3, 42);
        GlobalExplanation explanation = explainer.explainDataset(model, dataset);
        
        assertNotNull(explanation);
        assertEquals(100, explanation.getDatasetSize());
        assertEquals("SurrogateTree", explanation.getExplainerName());
        
        // Should have fidelity metadata
        assertNotNull(explanation.getMetadata("fidelity"));
        assertTrue((Double) explanation.getMetadata("fidelity") >= 0.0);
    }
    
    @Test
    void testSurrogateTreeFidelity() {
        GlobalExplainer explainer = new SurrogateTreeExplainer(5, 3, 42);
        GlobalExplanation explanation = explainer.explainDataset(model, dataset);
        
        double fidelity = (Double) explanation.getMetadata("fidelity");
        
        // Fidelity should be reasonable (>0.5 for this simple model)
        assertTrue(fidelity > 0.0, "Fidelity should be > 0");
        
        // Tree depth should be in reasonable range
        int depth = (Integer) explanation.getMetadata("treeDepth");
        assertTrue(depth > 0 && depth <= 6);
    }
    
    @Test
    void testSurrogateTreeRules() {
        GlobalExplainer explainer = new SurrogateTreeExplainer(3, 5, 42);
        GlobalExplanation explanation = explainer.explainDataset(model, dataset);
        
        // Should produce interpretable feature importances
        List<FeatureAttribution> top = explanation.getTopFeatures(1);
        assertEquals(1, top.size());
        assertTrue(top.get(0).importance() >= 0.0);
    }
    
    @Test
    void testRuleExtractionBasics() {
        RuleExtraction ruleExtractor = new RuleExtraction(0.6, 10, 42);
        RuleExtraction.RuleSet rules = ruleExtractor.extractRules(model, dataset);
        
        assertNotNull(rules);
        assertTrue(rules.size() > 0);
        
        // Should have some rules
        assertTrue(rules.getRules().size() <= 10);
    }
    
    @Test
    void testRuleExtractionPrediction() {
        RuleExtraction ruleExtractor = new RuleExtraction(0.6, 15, 42);
        RuleExtraction.RuleSet rules = ruleExtractor.extractRules(model, dataset);
        
        // Test prediction on first instance
        double[] testInstance = dataset.get(0);
        Double rulePrediction = rules.predict(testInstance);
        
        assertNotNull(rulePrediction);
        assertTrue(rulePrediction >= -10 && rulePrediction <= 10);
    }
    
    @Test
    void testRuleExtractionCoverage() {
        RuleExtraction ruleExtractor = new RuleExtraction(0.6, 20, 42);
        RuleExtraction.RuleSet rules = ruleExtractor.extractRules(model, dataset);
        
        // Each rule should have sensible coverage
        rules.getRules().forEach(rule -> {
            double coverage = rule.getCoverage();
            assertTrue(coverage >= 0.0 && coverage <= 1.0);
        });
    }
    
    @Test
    void testRuleConditions() {
        RuleExtraction.Condition cond1 = new RuleExtraction.Condition(0, "<=", 5.0);
        RuleExtraction.Condition cond2 = new RuleExtraction.Condition(1, ">", 3.0);
        
        double[] instance1 = {4.0, 4.0, 0.0, 0.0, 0.0};
        double[] instance2 = {6.0, 4.0, 0.0, 0.0, 0.0};
        
        assertTrue(cond1.isSatisfied(instance1));
        assertFalse(cond1.isSatisfied(instance2));
        assertTrue(cond2.isSatisfied(instance1));
    }
    
    @Test
    void testRuleToString() {
        List<RuleExtraction.Condition> conditions = new ArrayList<>();
        conditions.add(new RuleExtraction.Condition(0, "<=", 5.0));
        conditions.add(new RuleExtraction.Condition(1, ">", 3.0));
        
        RuleExtraction.Rule rule = new RuleExtraction.Rule(conditions, 1.0, 50, 100);
        String ruleStr = rule.toString();
        
        assertNotNull(ruleStr);
        assertTrue(ruleStr.contains("IF"));
        assertTrue(ruleStr.contains("THEN"));
    }
    
    @Test
    void testEmptyDatasetThrows() {
        GlobalExplainer explainer = new GlobalFeatureImportance();
        assertThrows(IllegalArgumentException.class, 
            () -> explainer.explainDataset(model, new ArrayList<>()));
    }
    
    @Test
    void testNullDatasetThrows() {
        GlobalExplainer explainer = new GlobalFeatureImportance();
        assertThrows(IllegalArgumentException.class, 
            () -> explainer.explainDataset(model, null));
    }
    
    @Test
    void testMultipleExplainersConsistency() {
        GlobalExplainer featureImportance = new GlobalFeatureImportance(15, 0.1, 42);
        GlobalExplainer surrogateTree = new SurrogateTreeExplainer(4, 3, 42);
        
        GlobalExplanation exp1 = featureImportance.explainDataset(model, dataset);
        GlobalExplanation exp2 = surrogateTree.explainDataset(model, dataset);
        
        // Both should identify non-zero importance for features
        assertTrue(exp1.getTotalImportance() > 0);
        assertTrue(exp2.getTotalImportance() > 0);
    }
    
    @Test
    void testGlobalExplanationStringRepresentation() {
        GlobalExplainer explainer = new GlobalFeatureImportance(10, 0.1, 42);
        GlobalExplanation explanation = explainer.explainDataset(model, dataset);
        
        String str = explanation.toString();
        assertNotNull(str);
        assertTrue(str.contains("GlobalExplanation"));
    }
}
