# XAI Core Release Notes

## Version 1.1.0-alpha (2026-01-05)

### 🚀 Major Features

#### 1. Natural Language Explanations (`nlp` package)
- **Convert technical XAI to human-readable narratives**
- Target audiences: Business stakeholders, customers, compliance officers
- Multi-language support (English, Vietnamese, extensible)
- Multiple narrative styles:
  - `TECHNICAL`: For data scientists ("Attribution φ = +0.35")
  - `BUSINESS`: For managers ("Age contributes 35% to the decision")
  - `REGULATORY`: For compliance ("Article 15 compliance documentation")
  - `CUSTOMER`: For end users ("Your age is the main factor")
- LLM-ready structured output (integration with GPT-4/Claude)

**Example:**
```java
NaturalLanguageExplainer nlg = new NaturalLanguageExplainer(Language.ENGLISH);
String report = nlg.toHumanReadable(explanation, NarrativeStyle.BUSINESS);
// "This customer has HIGH default risk (87%). Main factors: Age (35%), Debt ratio (25%)"
```

#### 2. Causal Inference (`causal` package)
- **Beyond correlation — true causal effects using Do-calculus**
- Pearl's causal hierarchy: Observational → Interventional → Counterfactual
- Features:
  - Direct vs total effects (mediator analysis)
  - Confounder identification (backdoor criterion)
  - Counterfactual reasoning ("What if...?")
  - Causal fairness analysis (legitimate vs discriminatory paths)
- **Critical for policy decisions** where interventions matter

**Example:**
```java
CausalExplainer causal = new CausalExplainer(model, trainingData, labels);
CausalEffect effect = causal.interventionalEffect(customer, "income", newValue);

// Correlation (SHAP): -0.18
// True causal effect: -0.12
// Confounding bias: -0.06 (33% of effect is spurious!)
```

#### 3. Interactive HTML Dashboard (`visualization` package)
- **Single-file, zero-build HTML dashboard**
- Tech stack: Tailwind CSS 3.4 + Vue 3 + Chart.js 4.4
- Features:
  - Force plots (SHAP-style gradient visualization)
  - Feature contributions bar chart
  - Interactive details table
  - Responsive design (mobile-friendly)
- Zero npm dependencies (all CDN-loaded)
- `HtmlDashboard.generateAndOpen(explanation)` — instant browser preview

#### 4. Model-Specific Optimizers (`explainer` package)
**TreeExplainer** (TreeSHAP algorithm)
- 10x faster for tree-based models
- Exact Shapley values (no sampling)
- Support: RandomForest, GradientTreeBoost
- Latency: ~10μs (vs ~100μs permutation)

**LinearExplainer** (Coefficient-based)
- 1000x faster for linear models
- Instant explanations (~1μs)
- Support: Linear/Logistic Regression, Ridge, LASSO
- Exact Shapley values: φᵢ = wᵢ × xᵢ

#### 5. Robustness Testing (`api.stability` package)
- **EU AI Act Article 15 compliance**
- Perturbation-based stability analysis
- Metrics:
  - Overall robustness score (0-1)
  - Per-feature stability (mean, std, max drift)
  - Auto-interpretation ("Highly robust" / "Unstable")
- Perturbation types: Gaussian, Uniform, Adversarial (planned)

**Example:**
```java
RobustnessScore robustness = new RobustnessScore();
RobustnessReport report = robustness.evaluate(model, instance, 100);

if (report.score() < 0.70) {
    throw new RobustnessException("EU AI Act compliance failure");
}
```

### ⚡ Performance Improvements

- **TreeExplainer:** 10x faster for tree models (TreeSHAP)
- **LinearExplainer:** 1000x faster for linear models (exact)
- **Confidence interval clamping:** Fixed bug in ParallelExplainer where CV > 1.0 caused negative intervals

### 🐛 Bug Fixes

- **ParallelExplainer:** Fixed confidence interval calculation
  - Issue: `1.0 - stability` could be negative when CV > 1.0
  - Fix: Clamped to `[0, 1]` range
  - Test: `ConvergentExplainerTest.testNoConvergence` now passes

### 📚 Documentation

- Enhanced package-info.java with LaTeX formulas
- Created comprehensive examples:
  - `NLPExample.java` — Human-readable narratives
  - `CausalExample.java` — Do-calculus interventions
- Updated README.md with new features

### ⚠️ Breaking Changes

**None** — All changes are additive (new packages/classes)

### 🔄 API Stability

- **@Stable:** Core API unchanged (Explanation, FeatureAttribution, Explainer)
- **@Incubating (since 1.1.0-alpha):**
  - `NaturalLanguageExplainer`
  - `CausalExplainer`
  - `TreeExplainer`
  - `LinearExplainer`
  - `RobustnessScore`
  - `HtmlDashboard`

### 📦 Dependencies

- **Zero runtime dependencies** (core library)
- Optional: Smile 3.1.1 (examples/tests only)
- JDK 21+ required (Vector API, Virtual Threads)

### 🧪 Testing

- **96/96 tests passing** ✅
- Code coverage: ~85% (excluding examples)
- Performance: 0.273μs pooled latency (183x faster than SHAP)

### 🚧 Planned for 1.2.0

1. **Spring Boot Starter** (`xai-core-spring-boot-starter`)
   - Auto-configuration for Explainer beans
   - REST endpoint wrappers
   - Actuator metrics integration

2. **Streaming Wrappers** (`xai-core-streaming`)
   - Kafka: `ExplanationProducer`, `ExplanationConsumer`
   - Flink: `ExplanationFunction` (MapFunction)
   - Real-time drift monitoring

3. **Benchmarking Whitepaper**
   - XAI Core vs SHAP performance comparison
   - Latency, throughput, memory benchmarks
   - Accuracy validation (identical Shapley values)

4. **Security Audit**
   - OWASP Dependency-Check
   - Snyk vulnerability scan
   - SBOM generation

### 📋 Migration Guide

No migration needed — all changes are backward compatible.

To use new features:
```java
// Natural Language
import io.github.Thung0808.xai.nlp.NaturalLanguageExplainer;

// Causal Inference
import io.github.Thung0808.xai.causal.CausalExplainer;

// HTML Dashboard
import io.github.Thung0808.xai.visualization.HtmlDashboard;

// Model-specific optimizers
import io.github.Thung0808.xai.explainer.TreeExplainer;
import io.github.Thung0808.xai.explainer.LinearExplainer;

// Robustness testing
import io.github.Thung0808.xai.api.stability.RobustnessScore;
```

### 🙏 Acknowledgments

- TreeSHAP algorithm: Lundberg et al. (2020)
- Causal inference: Pearl's "Causality" (2000)
- EU AI Act compliance requirements: Article 15

### 📞 Support

- GitHub Issues: https://github.com/Thung0808/xai-core/issues
- Documentation: https://github.com/Thung0808/xai-core/wiki
- Email: your.email@example.com

---

**Full Changelog:** v0.7.0...v1.1.0-alpha
