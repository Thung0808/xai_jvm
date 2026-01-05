# Privacy-Preserving XAI Guide

**⚠️ Experimental Feature** (since v1.1.0) - API may change based on feedback

---

## Overview

The **Privacy-Preserving XAI module** enables you to generate explanations while protecting sensitive data using **Differential Privacy (DP)**. This is critical for:

- **GDPR Article 22**: Right to explanation without exposing training data
- **HIPAA Compliance**: Medical ML models
- **Financial Privacy**: Credit scoring explanations
- **Federated Learning**: Explanations across distributed data

---

## Key Concepts

### 1. Differential Privacy (DP)

**Formal Definition**: A mechanism $M$ is $\varepsilon$-differentially private if for all datasets $D_1, D_2$ differing by **one row**:

$$
\frac{P(M(D_1) \in S)}{P(M(D_2) \in S)} \leq e^\varepsilon
$$

**Intuition**: An adversary **cannot tell** whether your data was in the dataset, even after seeing the explanation.

**Privacy Parameters**:
- $\varepsilon$ (epsilon): **Privacy budget** (smaller = more private)
  - $\varepsilon < 1.0$: Strong privacy
  - $1.0 \leq \varepsilon < 3.0$: Moderate privacy
  - $\varepsilon > 3.0$: Weak privacy
- $\delta$ (delta): **Failure probability** (typically $10^{-5}$ to $10^{-6}$)

---

### 2. Privacy Budget Tracking

Each query **consumes** privacy budget. Once budget is exhausted, no more queries allowed.

**Example**:
```
Initial budget: ε = 10.0
Query 1: ε = 1.0 consumed → Remaining: 9.0
Query 2: ε = 2.0 consumed → Remaining: 7.0
Query 3: ε = 7.5 consumed → Remaining: 2.5
Query 4: ε = 3.0 requested → DENIED (exceeds budget)
```

**Privacy Composition**: Total privacy loss = $\sum_{i=1}^{k} \varepsilon_i$ (basic composition)

---

### 3. Noise Mechanisms

To achieve DP, we **add noise** to explanations:

#### Laplace Mechanism

For $\varepsilon$-DP:
$$
M(D) = f(D) + \text{Lap}\left(\frac{\Delta f}{\varepsilon}\right)
$$

Where:
- $\Delta f$ = **Global sensitivity** = $\max_{D_1, D_2} |f(D_1) - f(D_2)|$
- $\text{Lap}(b)$ = Laplace distribution with scale $b$, density: $\frac{1}{2b} e^{-|x|/b}$

**When to use**: Counting queries, histograms, feature importance (unbounded sensitivity)

#### Gaussian Mechanism

For $(\varepsilon, \delta)$-DP:
$$
M(D) = f(D) + \mathcal{N}\left(0, \sigma^2\right)
$$

Where:
$$
\sigma = \frac{\Delta f}{\varepsilon} \sqrt{2 \ln(1.25/\delta)}
$$

**When to use**: Bounded sensitivity, machine learning (gradient clipping)

---

## Mathematical Foundations

### Global Sensitivity

The **global sensitivity** of a function $f$ is:
$$
\Delta f = \max_{D_1, D_2 \text{ differ by 1 row}} |f(D_1) - f(D_2)|
$$

**Examples**:

1. **Count query** (e.g., "How many fraud cases?"): $\Delta f = 1$ (adding/removing 1 row changes count by 1)

2. **Mean query** (e.g., "Average transaction amount"):
   $$
   \Delta f = \frac{\max(\text{value})}{n}
   $$
   For bounded values $[0, 100]$ and $n=1000$: $\Delta f = 0.1$

3. **Feature importance** (SHAP values): Typically $\Delta f \approx 1.0$ (normalized)

---

### Privacy Loss Accounting

**Basic Composition**: $k$ queries with $\varepsilon_i$ → Total privacy loss:
$$
\varepsilon_{\text{total}} = \sum_{i=1}^{k} \varepsilon_i
$$

**Advanced Composition** (tighter bound):
$$
\varepsilon_{\text{total}} = \varepsilon \sqrt{2k \ln(1/\delta)} + k\varepsilon(e^\varepsilon - 1)
$$

**Rényi Differential Privacy** (even tighter): Uses Rényi divergence instead of max divergence.

---

### Reconstruction Attack Detection

**Threat**: Adversary makes many queries to **reconstruct** original data.

**Defense**: Track query history and detect suspicious patterns:

1. **Query overlap**: Multiple queries on same records → High risk
2. **Budget exhaustion rate**: Rapid consumption → Warning
3. **Statistical inference**: Correlations between queries

**Risk Levels**:
- **LOW** (<50% budget): Safe
- **MEDIUM** (50-75% budget): Monitor
- **HIGH** (>75% budget): Alert/Block

---

## Usage Examples

### Example 1: Basic Differential Privacy

```java
import io.github.thung0808.xai.privacy.DifferentialPrivacyMechanism;

// Initialize DP mechanism
double epsilon = 1.0;   // Privacy budget for this query
double delta = 1e-5;    // Failure probability
DifferentialPrivacyMechanism dp = new DifferentialPrivacyMechanism(epsilon, delta);

// Original SHAP values (sensitive!)
double[] shapValues = {0.3, -0.2, 0.5, 0.1};

// Add Laplace noise
double[] privateSHAP = dp.addNoise(shapValues, "Laplace");

System.out.println("Original SHAP: " + Arrays.toString(shapValues));
System.out.println("Private SHAP:  " + Arrays.toString(privateSHAP));
```

**Output**:
```
Original SHAP: [0.3, -0.2, 0.5, 0.1]
Private SHAP:  [0.32, -0.18, 0.48, 0.13]  // Noisy, but preserves privacy
```

---

### Example 2: Privacy Budget Tracking

```java
import io.github.thung0808.xai.privacy.PrivacyBudgetTracker;

// Initialize tracker with total budget
double totalBudget = 10.0;
PrivacyBudgetTracker tracker = new PrivacyBudgetTracker(totalBudget);

// Query 1: SHAP explanation
double query1Budget = 2.0;
int datasetSize1 = 1000;
tracker.recordQuery(query1Budget, datasetSize1);
System.out.println("Budget consumed: " + tracker.getBudgetConsumed() + "%");
// Output: "Budget consumed: 20%"

// Query 2: LIME explanation
double query2Budget = 1.5;
int datasetSize2 = 1000;
tracker.recordQuery(query2Budget, datasetSize2);
System.out.println("Budget consumed: " + tracker.getBudgetConsumed() + "%");
// Output: "Budget consumed: 35%"

// Query 3: Feature importance
double query3Budget = 3.0;
int datasetSize3 = 1000;
tracker.recordQuery(query3Budget, datasetSize3);
System.out.println("Budget consumed: " + tracker.getBudgetConsumed() + "%");
// Output: "Budget consumed: 65%"

// Check if next query is allowed
double query4Budget = 5.0;
if (tracker.canQuery(query4Budget)) {
    System.out.println("Query allowed");
} else {
    System.err.println("Budget exceeded! Remaining: " + tracker.getRemainingBudget());
}
// Output: "Budget exceeded! Remaining: 3.5"
```

---

### Example 3: Automated Alerts

```java
// Configure alerts at thresholds
tracker.setAlertThreshold(0.5);  // Alert at 50%
tracker.setAlertThreshold(0.75); // Alert at 75%
tracker.setAlertThreshold(0.9);  // Alert at 90%

// Make queries
tracker.recordQuery(3.0, 1000); // 30%
tracker.recordQuery(2.5, 1000); // 55% → Alert triggered!
tracker.recordQuery(2.0, 1000); // 75% → Alert triggered!
tracker.recordQuery(1.5, 1000); // 90% → Alert triggered!

// Get alert history
List<String> alerts = tracker.getAlerts();
for (String alert : alerts) {
    System.out.println(alert);
}
```

**Output**:
```
[WARNING] Privacy budget 50% consumed (5.0/10.0)
[WARNING] Privacy budget 75% consumed (7.5/10.0)
[CRITICAL] Privacy budget 90% consumed (9.0/10.0)
```

---

### Example 4: Audit Report Generation

```java
// Generate comprehensive audit report
AuditReport report = tracker.generateAuditReport();

System.out.println("=== Privacy Audit Report ===");
System.out.println("Total Queries: " + report.getQueryCount());
System.out.println("Budget Consumed: " + report.getBudgetConsumed() + "%");
System.out.println("Remaining Budget: " + report.getRemainingBudget());
System.out.println("Risk Level: " + report.getRisk());
System.out.println("Reconstruction Attack Risk: " + report.getReconstructionRisk() + "%");
System.out.println("\nRecommendations:");
for (String rec : report.getRecommendations()) {
    System.out.println("- " + rec);
}

// Export to JSON (for compliance teams)
String jsonReport = report.toJSON();
Files.writeString(Path.of("privacy-audit.json"), jsonReport);
```

**Output**:
```
=== Privacy Audit Report ===
Total Queries: 8
Budget Consumed: 87%
Remaining Budget: 1.3
Risk Level: HIGH
Reconstruction Attack Risk: 34.7%

Recommendations:
- Reduce query frequency
- Increase epsilon for remaining queries
- Consider resetting budget after cooldown period
- Review query patterns for potential attacks
```

---

### Example 5: Federated Explanation Aggregation

Aggregate explanations from **multiple data silos** without sharing raw data:

```java
import io.github.thung0808.xai.privacy.FederatedExplanationAggregator;

// 3 hospitals with patient data (cannot share due to HIPAA)
double[][] hospital1SHAP = {{0.3, -0.2}, {0.5, 0.1}};
double[][] hospital2SHAP = {{0.4, -0.1}, {0.6, 0.0}};
double[][] hospital3SHAP = {{0.2, -0.3}, {0.4, 0.2}};

FederatedExplanationAggregator aggregator = new FederatedExplanationAggregator();

// Add explanations from each hospital (with DP noise)
aggregator.addClientExplanations(hospital1SHAP, 1.0, 1e-5); // epsilon=1.0
aggregator.addClientExplanations(hospital2SHAP, 1.0, 1e-5);
aggregator.addClientExplanations(hospital3SHAP, 1.0, 1e-5);

// Aggregate using secure averaging
double[][] globalSHAP = aggregator.aggregateSecurely();

System.out.println("Global SHAP (privacy-preserving): " + Arrays.deepToString(globalSHAP));
```

**Output**:
```
Global SHAP (privacy-preserving): [[0.31, -0.19], [0.52, 0.11]]
```

**Privacy Guarantee**: Each hospital's contribution is $\varepsilon$-DP protected. Total budget = $3\varepsilon$ (basic composition).

---

## Real-World Use Cases

### 1. GDPR Article 22 Compliance

**Scenario**: Credit scoring model must provide explanations without revealing training data.

```java
// Credit scoring model
Predictor creditModel = loadModel("credit-scorer.onnx");

// Generate SHAP explanation
ShapExplainer explainer = new ShapExplainer(creditModel);
double[] shapValues = explainer.explain(applicantData);

// Apply differential privacy
DifferentialPrivacyMechanism dp = new DifferentialPrivacyMechanism(1.0, 1e-5);
double[] privateSHAP = dp.addNoise(shapValues, "Laplace");

// Track budget
PrivacyBudgetTracker tracker = new PrivacyBudgetTracker(100.0); // 100 explanations per day
tracker.recordQuery(1.0, 10000); // Dataset size: 10k customers

// Return explanation to customer
return new Explanation(privateSHAP, featureNames);
```

**Legal Justification**:
- **GDPR Article 22**: Right to explanation ✓
- **GDPR Article 5**: Data minimization ✓ (no raw data exposed)
- **GDPR Recital 71**: Privacy-preserving explanations ✓

---

### 2. Healthcare Model Explanations (HIPAA)

**Scenario**: Hospital uses ML for diagnosis, must explain without exposing patient data.

```java
// Diagnosis model
Predictor diagnosisModel = loadModel("diagnosis-model.onnx");

// Patient data (PHI - Protected Health Information)
double[] patientFeatures = {age, bmi, bloodPressure, cholesterol};

// Generate explanation
ShapExplainer explainer = new ShapExplainer(diagnosisModel);
double[] shapValues = explainer.explain(patientFeatures);

// Apply DP (HIPAA Safe Harbor: de-identification)
DifferentialPrivacyMechanism dp = new DifferentialPrivacyMechanism(0.5, 1e-6);
double[] privateSHAP = dp.addNoise(shapValues, "Gaussian");

// Audit trail (HIPAA requirement)
PrivacyBudgetTracker tracker = new PrivacyBudgetTracker(50.0);
tracker.recordQuery(0.5, 5000, "Patient ID: " + patientId);

// Log to SIEM
logger.info("Private explanation generated for patient {}", patientId);
```

---

### 3. Financial Fraud Detection

**Scenario**: Bank explains fraud predictions while protecting customer privacy.

```java
// Fraud model
Predictor fraudModel = loadModel("fraud-detector.onnx");

// Transaction features
double[] transaction = {amount, velocity, geolocation, merchantCategory};

// Generate explanation
LimeExplainer explainer = new LimeExplainer(fraudModel);
double[] importance = explainer.explain(transaction);

// Apply DP
DifferentialPrivacyMechanism dp = new DifferentialPrivacyMechanism(2.0, 1e-5);
double[] privateImportance = dp.addNoise(importance, "Laplace");

// Track budget (per customer per month)
PrivacyBudgetTracker tracker = getTrackerForCustomer(customerId);
tracker.recordQuery(2.0, 100000); // 100k transactions dataset

// Check for abuse
AuditReport report = tracker.generateAuditReport();
if (report.getRisk().equals("HIGH")) {
    alertSecurityTeam(customerId, report);
}
```

---

### 4. Federated Learning (Multi-Institution)

**Scenario**: 5 banks want to train fraud model together without sharing data.

```java
// Each bank generates local SHAP explanations
Bank bank1 = new Bank("Bank1", trainingData1);
Bank bank2 = new Bank("Bank2", trainingData2);
// ... 5 banks total

FederatedExplanationAggregator aggregator = new FederatedExplanationAggregator();

// Each bank adds their explanations (with DP)
aggregator.addClientExplanations(bank1.getLocalSHAP(), 1.0, 1e-5);
aggregator.addClientExplanations(bank2.getLocalSHAP(), 1.0, 1e-5);
// ... all banks

// Aggregate globally
double[][] globalExplanations = aggregator.aggregateSecurely();

// Total privacy budget: 5 * 1.0 = 5.0 (basic composition)
System.out.println("Global model explanation (privacy-preserving):");
System.out.println(Arrays.deepToString(globalExplanations));
```

---

## Performance Characteristics

- **Noise Addition**: O(n) where n = number of features
- **Budget Tracking**: O(1) per query
- **Audit Report**: O(k) where k = number of queries
- **Federated Aggregation**: O(m × n) where m = clients, n = features

**Optimization Tips**:
- Cache `DifferentialPrivacyMechanism` instances (expensive RNG initialization)
- Use batch queries to reduce composition overhead
- Pre-allocate `PrivacyBudgetTracker` with realistic total budget

---

## Choosing Privacy Parameters

### Epsilon (ε) Selection Guide

| Use Case | Epsilon | Trade-off |
|----------|---------|-----------|
| Medical records (HIPAA) | 0.1 - 0.5 | Strong privacy, high noise |
| Financial data (PCI-DSS) | 0.5 - 1.0 | Moderate privacy, moderate noise |
| General ML explanations | 1.0 - 3.0 | Practical privacy, low noise |
| Public datasets | 3.0 - 10.0 | Weak privacy, minimal noise |

**Rule of Thumb**: Start with ε = 1.0 and adjust based on:
- **Regulatory requirements** (GDPR, HIPAA, CCPA)
- **Data sensitivity** (PHI, PII, financial)
- **Explanation utility** (lower ε = more noise = less useful)

### Delta (δ) Selection

- **Typical**: $\delta = \frac{1}{n^2}$ where $n$ = dataset size
- **Conservative**: $\delta = 10^{-6}$ to $10^{-8}$
- **Never exceed**: $\delta > 10^{-3}$ (too risky)

---

## Limitations & Best Practices

### ⚠️ Privacy-Utility Trade-off

**More privacy (lower ε) = More noise = Less useful explanations**

Example:
```
ε = 0.1: SHAP values so noisy they're useless
ε = 1.0: Reasonable balance
ε = 10.0: Minimal privacy, explanations accurate
```

**Mitigation**:
- Use **feature grouping** (explain at higher level)
- Apply **post-processing** (consistency constraints)
- Increase **dataset size** (noise scales with 1/√n)

---

### Debugging Privacy Issues

```java
// Test privacy guarantee
double[][] testData = generateAdjacentDatasets(); // D1, D2 differ by 1 row
double[] result1 = dp.addNoise(f(testData[0]), "Laplace");
double[] result2 = dp.addNoise(f(testData[1]), "Laplace");

// Check indistinguishability
double ratio = probability(result1) / probability(result2);
System.out.println("Privacy ratio: " + ratio);
// Should be < e^epsilon
```

---

### Common Pitfalls

1. **Forgetting composition**: Each query consumes budget!
2. **Underestimating sensitivity**: Use worst-case $\Delta f$
3. **Reusing randomness**: Each query needs fresh random noise
4. **Ignoring post-processing**: Non-DP operations break privacy
5. **Exceeding budget**: Monitor tracker continuously

---

## API Stability

⚠️ **Experimental** (since v1.1.0)

Expected stabilization: **v1.2.0 (Q2 2026)**

**Potential changes**:
- Advanced composition (Rényi DP)
- Adaptive budget allocation
- Privacy amplification via sampling

See [STABILITY.md](../STABILITY.md) for details.

---

## Further Reading

- **Book**: *The Algorithmic Foundations of Differential Privacy* (Dwork & Roth, 2014)
- **Paper**: *Deep Learning with Differential Privacy* (Abadi et al., 2016)
- **Tutorial**: [Programming Differential Privacy](https://programming-dp.com/)
- **Tool**: [Google's DP Library](https://github.com/google/differential-privacy)

---

**Next**: [LLM Explainability](LLM-XAI.md) | [Back to Wiki Home](Home.md)
