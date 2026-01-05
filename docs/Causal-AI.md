# Causal AI Guide

**⚠️ Experimental Feature** (since v1.1.0) - API may change based on feedback

---

## Overview

The **Causal AI module** enables you to perform **causal inference** using Pearl's do-calculus framework. Unlike correlation-based explanations (SHAP, LIME), causal methods answer **interventional** and **counterfactual** questions:

- **Correlation**: "Patients who take drug X have better outcomes"
- **Causation**: "If we **force** a patient to take drug X, will they improve?"

---

## Key Concepts

### 1. Directed Acyclic Graph (DAG)

A **DAG** represents causal relationships between variables:

- **Nodes**: Variables (e.g., "smoking", "cancer", "genetics")
- **Edges**: Direct causal effects (e.g., "smoking → cancer")
- **Edge Weights**: Strength of causal effect (0.0 to 1.0)

**Example DAG**:
```
Genetics → Cancer
Smoking → Cancer
Smoking → Tar Deposits → Cancer
```

### 2. Interventions (do-operator)

The **do-operator** represents interventions:

- **Observational**: $P(\text{cancer} | \text{smoking})$ - "What's the probability of cancer **given** we observe smoking?"
- **Interventional**: $P(\text{cancer} | \text{do}(\text{smoking}))$ - "What's the probability of cancer **if we force** smoking?"

**Key Insight**: Observational ≠ Interventional (due to confounding)

### 3. Confounding

A **confounder** is a variable that affects both treatment and outcome:

```
Genetics → Smoking
Genetics → Cancer
```

Here, "genetics" confounds the smoking→cancer relationship. Naive correlation will overestimate smoking's effect.

---

## Mathematical Foundation

### Pearl's do-calculus

**Three Rules for Causal Inference**:

**Rule 1: Insertion/Deletion of Observations**
$$
P(y | \text{do}(x), z, w) = P(y | \text{do}(x), w) \text{ if } (Y \perp Z | X, W)_{G_{\overline{X}}}
$$

**Rule 2: Action/Observation Exchange**
$$
P(y | \text{do}(x), \text{do}(z), w) = P(y | \text{do}(x), z, w) \text{ if } (Y \perp Z | X, W)_{G_{\overline{X}\underline{Z}}}
$$

**Rule 3: Insertion/Deletion of Actions**
$$
P(y | \text{do}(x), \text{do}(z), w) = P(y | \text{do}(x), w) \text{ if } (Y \perp Z | X, W)_{G_{\overline{X}\overline{Z(W)}}}
$$

Where:
- $G_{\overline{X}}$ = Graph with incoming edges to $X$ deleted
- $G_{\underline{Z}}$ = Graph with outgoing edges from $Z$ deleted
- $(Y \perp Z | X, W)_G$ = $Y$ and $Z$ are independent given $X, W$ in graph $G$

---

### Backdoor Criterion

A set $Z$ **satisfies the backdoor criterion** for $(X, Y)$ if:

1. **No descendant**: No node in $Z$ is a descendant of $X$
2. **Blocks backdoor paths**: $Z$ blocks all paths from $X$ to $Y$ that have an arrow into $X$

**If backdoor criterion is satisfied**:
$$
P(y | \text{do}(x)) = \sum_z P(y | x, z) P(z)
$$

**Example**:
```
Z → X → Y
Z → Y
```
Here, $Z$ is a confounder. Adjusting for $Z$ satisfies the backdoor criterion.

---

### Frontdoor Criterion

A set $M$ **satisfies the frontdoor criterion** for $(X, Y)$ if:

1. **Intercepts all paths**: $M$ intercepts all directed paths from $X$ to $Y$
2. **No backdoor paths**: No backdoor path from $X$ to $M$
3. **X blocks backdoors**: $X$ blocks all backdoor paths from $M$ to $Y$

**If frontdoor criterion is satisfied**:
$$
P(y | \text{do}(x)) = \sum_m P(m | x) \sum_{x'} P(y | x', m) P(x')
$$

**Example** (unobserved confounder):
```
U → X   U → Y
X → M → Y
```
Here, $U$ is unobserved. Frontdoor adjustment through mediator $M$ still identifies causal effect.

---

## Usage Examples

### Example 1: Basic DAG Construction

```java
import io.github.thung0808.xai.causal.CausalGraph;

CausalGraph graph = new CausalGraph();

// Add variables
graph.addVariable("smoking");
graph.addVariable("tar_deposits");
graph.addVariable("cancer");
graph.addVariable("genetics");

// Add causal edges (with strengths)
graph.addEdge("smoking", "tar_deposits", 0.8);
graph.addEdge("tar_deposits", "cancer", 0.6);
graph.addEdge("genetics", "smoking", 0.3);
graph.addEdge("genetics", "cancer", 0.4);

// Check graph properties
boolean isAcyclic = graph.isAcyclic(); // true if no cycles
List<String> parents = graph.getParents("cancer"); // ["tar_deposits", "genetics"]
```

---

### Example 2: Average Treatment Effect (ATE)

Calculate the **causal effect** of smoking on cancer:

```java
import io.github.thung0808.xai.causal.DoCalculusOperator;

// Prepare dataset (each row is a patient)
double[][] dataset = {
    // smoking, tar_deposits, cancer, genetics
    {1.0, 0.8, 1.0, 0.5},  // Patient 1: smokes, has cancer
    {0.0, 0.1, 0.0, 0.2},  // Patient 2: doesn't smoke, no cancer
    {1.0, 0.7, 1.0, 0.6},  // Patient 3: smokes, has cancer
    // ... more patients
};

DoCalculusOperator doCalc = new DoCalculusOperator(graph);

// Compute ATE: E[cancer | do(smoking=1)] - E[cancer | do(smoking=0)]
double ate = doCalc.computeATE("smoking", "cancer", dataset);

System.out.println("Average Treatment Effect: " + ate);
// Output: "Average Treatment Effect: 0.42"
// Interpretation: Forcing someone to smoke increases cancer risk by 42%
```

---

### Example 3: Backdoor Adjustment

Control for confounding using backdoor criterion:

```java
// Check if backdoor adjustment is valid
List<String> adjustmentSet = doCalc.findBackdoorAdjustmentSet("smoking", "cancer");
System.out.println("Adjust for: " + adjustmentSet);
// Output: "Adjust for: [genetics]"

// Compute adjusted effect
double adjustedEffect = doCalc.backdoorAdjustment("smoking", "cancer", adjustmentSet, dataset);
System.out.println("Adjusted causal effect: " + adjustedEffect);
```

**Implementation**:
$$
P(\text{cancer} | \text{do}(\text{smoking})) = \sum_{\text{genetics}} P(\text{cancer} | \text{smoking}, \text{genetics}) P(\text{genetics})
$$

---

### Example 4: Frontdoor Adjustment

When confounder is **unobserved**:

```java
// DAG with unobserved confounder U
// U → smoking, U → cancer (U is unobserved)
// smoking → tar_deposits → cancer (tar_deposits is mediator)

graph.addVariable("smoking");
graph.addVariable("tar_deposits");
graph.addVariable("cancer");
graph.addEdge("smoking", "tar_deposits", 0.8);
graph.addEdge("tar_deposits", "cancer", 0.6);
// U is unobserved, so not in graph

// Check frontdoor criterion
List<String> mediators = doCalc.findFrontdoorAdjustmentSet("smoking", "cancer");
System.out.println("Mediators: " + mediators);
// Output: "Mediators: [tar_deposits]"

// Compute effect via mediator
double frontdoorEffect = doCalc.frontdoorAdjustment("smoking", "cancer", mediators, dataset);
System.out.println("Frontdoor causal effect: " + frontdoorEffect);
```

---

### Example 5: Counterfactual Reasoning

Answer "What-if" questions:

```java
// Counterfactual: "What would patient X's cancer risk be if they had NOT smoked?"

Map<String, Double> observedValues = Map.of(
    "smoking", 1.0,      // Actually smokes
    "genetics", 0.6,     // High genetic risk
    "tar_deposits", 0.7,
    "cancer", 1.0        // Has cancer
);

Map<String, Double> intervention = Map.of(
    "smoking", 0.0  // Counterfactual: didn't smoke
);

double counterfactualRisk = doCalc.computeCounterfactual(
    "cancer", 
    observedValues, 
    intervention, 
    graph
);

System.out.println("Observed cancer risk: 1.0");
System.out.println("Counterfactual risk (if no smoking): " + counterfactualRisk);
// Output: "Counterfactual risk (if no smoking): 0.58"
// Interpretation: Without smoking, patient's cancer risk would drop to 58%
```

---

## Real-World Use Cases

### 1. Medical Treatment Planning

**Question**: Will prescribing drug X improve patient outcomes?

```java
CausalGraph medicalGraph = new CausalGraph();
medicalGraph.addVariable("treatment");
medicalGraph.addVariable("recovery");
medicalGraph.addVariable("age");
medicalGraph.addVariable("severity");

medicalGraph.addEdge("age", "treatment", 0.3);
medicalGraph.addEdge("age", "recovery", 0.4);
medicalGraph.addEdge("severity", "treatment", 0.5);
medicalGraph.addEdge("severity", "recovery", 0.6);
medicalGraph.addEdge("treatment", "recovery", 0.7);

DoCalculusOperator doCalc = new DoCalculusOperator(medicalGraph);
double treatmentEffect = doCalc.computeATE("treatment", "recovery", patientData);
```

**Interpretation**:
- `treatmentEffect > 0.3` → Strong positive effect, recommend treatment
- `treatmentEffect < 0.1` → Weak effect, consider alternatives
- Adjust for confounders (age, severity) using backdoor criterion

---

### 2. Policy Impact Analysis

**Question**: Will increasing minimum wage reduce poverty?

```java
CausalGraph policyGraph = new CausalGraph();
policyGraph.addVariable("min_wage");
policyGraph.addVariable("poverty_rate");
policyGraph.addVariable("unemployment");
policyGraph.addVariable("gdp_growth");

policyGraph.addEdge("min_wage", "unemployment", -0.2);
policyGraph.addEdge("unemployment", "poverty_rate", 0.6);
policyGraph.addEdge("gdp_growth", "min_wage", 0.3);
policyGraph.addEdge("gdp_growth", "poverty_rate", -0.5);

double policyEffect = doCalc.computeATE("min_wage", "poverty_rate", economicData);
```

**Insights**:
- Direct effect: min_wage → poverty_rate
- Indirect effect: min_wage → unemployment → poverty_rate
- Confounding: gdp_growth affects both variables

---

### 3. Fraud Detection (Causal Patterns)

**Question**: Does transaction X **cause** fraud, or just correlate?

```java
CausalGraph fraudGraph = new CausalGraph();
fraudGraph.addVariable("transaction_velocity");
fraudGraph.addVariable("geolocation_mismatch");
fraudGraph.addVariable("fraud");
fraudGraph.addVariable("account_age");

fraudGraph.addEdge("transaction_velocity", "fraud", 0.7);
fraudGraph.addEdge("geolocation_mismatch", "fraud", 0.8);
fraudGraph.addEdge("account_age", "transaction_velocity", -0.4);
fraudGraph.addEdge("account_age", "fraud", -0.5);

// Identify causal fraud indicators (not just correlated)
double causalImpact = doCalc.computeATE("transaction_velocity", "fraud", transactionData);
```

---

## Performance Characteristics

- **Graph Construction**: O(V + E) where V = variables, E = edges
- **ATE Computation**: O(N × V²) where N = dataset size
- **Backdoor/Frontdoor Search**: O(2^V) worst-case (exponential in confounders)
- **Recommended**: Keep DAGs < 20 variables for interactive use

**Optimization Tips**:
- Use `CausalGraph.pruneIrrelevant(treatment, outcome)` to remove unrelated variables
- Cache `findBackdoorAdjustmentSet()` results (expensive)
- For large datasets, sample N=10,000 rows for ATE estimation

---

## Limitations & Best Practices

### ⚠️ Causal Assumptions Required

Causal inference is **only valid if**:

1. **Correct DAG**: Your causal graph must reflect true causal structure
2. **No unmeasured confounders**: All confounders must be observed (or use frontdoor adjustment)
3. **Positivity**: All covariate combinations must occur in data
4. **Consistency**: Treatment must be well-defined

**Rule of Thumb**: Use domain expertise (doctors, economists) to validate DAG structure.

---

### Debugging Causal Graphs

```java
// Check for cycles (DAGs must be acyclic)
if (!graph.isAcyclic()) {
    throw new IllegalStateException("Causal graph contains cycles!");
}

// Visualize graph
String dotFormat = graph.toDot(); // GraphViz DOT format
System.out.println(dotFormat);

// Check identification
boolean isIdentified = doCalc.isIdentifiable("treatment", "outcome");
if (!isIdentified) {
    System.err.println("Causal effect is NOT identifiable from data!");
}
```

---

### When to Use Causal AI vs. SHAP/LIME

| Use Case | Method | Reason |
|----------|--------|--------|
| Feature importance | SHAP/LIME | No causal assumptions needed |
| Treatment planning | Causal AI | Need interventional effects |
| Policy evaluation | Causal AI | Must distinguish correlation vs. causation |
| Model debugging | SHAP/LIME | Faster, no DAG required |
| Counterfactuals | Causal AI | Need "what-if" scenarios |

**General Rule**: Use SHAP/LIME for **model explanations**, Causal AI for **decision-making under interventions**.

---

## API Stability

⚠️ **Experimental** (since v1.1.0)

This API is experimental and may change in future releases based on community feedback. Expected stabilization: **v1.2.0 (Q2 2026)**.

**Potential changes**:
- Graph serialization format
- do-calculus API naming
- Counterfactual computation algorithm

See [STABILITY.md](../STABILITY.md) for deprecation policy.

---

## Further Reading

- **Book**: *Causality* by Judea Pearl (Cambridge University Press, 2009)
- **Paper**: *The Seven Tools of Causal Inference* (Pearl, 2019)
- **Tutorial**: [Introduction to Causal Inference](https://www.bradyneal.com/causal-inference-course)
- **Software**: [DoWhy (Python)](https://github.com/py-why/dowhy) - Inspiration for this module

---

**Next**: [Privacy-Preserving XAI](Privacy-XAI.md) | [Back to Wiki Home](Home.md)
