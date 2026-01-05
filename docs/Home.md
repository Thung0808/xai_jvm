# XAI Core Wiki

Welcome to the **XAI Core** documentation hub! This wiki provides comprehensive guides, tutorials, and mathematical explanations for all features.

---

## 📖 Getting Started

- [Installation Guide](Installation-Guide.md)
- [Quick Start Tutorial](Quick-Start.md)
- [API Reference](API-Reference.md)
- [Examples Gallery](Examples.md)

---

## 🧮 Core Concepts

### Explainability Methods

- **[SHAP (SHapley Additive exPlanations)](SHAP-Guide.md)** - Game theory-based feature attribution
- **[LIME (Local Interpretable Model-agnostic Explanations)](LIME-Guide.md)** - Linear approximations
- **[Permutation Importance](Permutation-Guide.md)** - Feature shuffle testing
- **[Tree-based Explanations](Tree-Guide.md)** - Native decision tree interpretation

---

## 🚀 Advanced Features (v1.1.0)

### [Causal AI](Causal-AI.md) ⚠️ Experimental

Learn how to perform causal inference with Pearl's do-calculus:

- **Directed Acyclic Graphs (DAGs)** - Causal structure modeling
- **do-calculus** - Interventional queries
- **Backdoor Adjustment** - Confounding control
- **Frontdoor Adjustment** - Mediation analysis
- **Counterfactual Reasoning** - "What-if" scenarios

**Use Cases**: Medical treatment effects, policy impact analysis, fraud detection

---

### [Privacy-Preserving XAI](Privacy-XAI.md) ⚠️ Experimental

Protect sensitive data while generating explanations:

- **Differential Privacy (DP)** - ε-δ privacy guarantees
- **Privacy Budget Tracking** - Query monitoring and alerts
- **Federated Explanations** - Distributed data aggregation
- **Compliance** - GDPR Article 22, CCPA readiness

**Mathematical Foundation**: Laplace mechanism, Gaussian mechanism, privacy loss accounting

---

### [LLM Explainability](LLM-XAI.md) ⚠️ Experimental

Explain transformer-based language models:

- **Attention Map Extraction** - Visualize BERT/GPT attention weights
- **Token Saliency** - Word-level importance scoring
- **ONNX Integration** - Portable model format support
- **Use Cases** - Sentiment analysis, chatbot debugging, bias detection

---

### [Interactive What-If Analysis](What-If-Guide.md) ⚠️ Experimental

Real-time counterfactual predictions:

- **WebSocket Integration** - Live dashboard updates
- **Feature Perturbation** - User-defined constraints
- **Surrogate Models** - Fast approximation
- **Use Cases** - Credit approval, medical treatment planning

---

## 🎯 Production Guides

- **[Performance Tuning](Performance-Tuning.md)** - SIMD optimization, Virtual Threads
- **[Deployment Strategies](Deployment.md)** - Spring Boot, Micronaut, Quarkus
- **[Monitoring & Observability](Monitoring.md)** - Metrics, logging, tracing
- **[Security Best Practices](Security.md)** - Input validation, privacy controls

---

## 📊 Use Case Deep Dives

- **[Financial Services](Use-Case-Finance.md)** - Credit scoring, fraud detection
- **[Healthcare](Use-Case-Healthcare.md)** - Diagnosis support, treatment recommendations
- **[E-commerce](Use-Case-Ecommerce.md)** - Product recommendations, churn prediction
- **[Manufacturing](Use-Case-Manufacturing.md)** - Predictive maintenance, quality control

---

## 🔬 Mathematical Foundations

### SHAP Theory

SHAP values are based on **Shapley values** from cooperative game theory:

$$
\phi_i = \sum_{S \subseteq N \setminus \{i\}} \frac{|S|!(n-|S|-1)!}{n!} [f(S \cup \{i\}) - f(S)]
$$

Where:
- $\phi_i$ = Shapley value for feature $i$
- $N$ = Set of all features
- $S$ = Subset of features excluding $i$
- $f(S)$ = Model prediction using feature subset $S$

**Properties**:
- **Efficiency**: $\sum_{i=1}^{n} \phi_i = f(x) - f(\emptyset)$
- **Symmetry**: If $i$ and $j$ contribute equally, $\phi_i = \phi_j$
- **Dummy**: If $i$ doesn't contribute, $\phi_i = 0$
- **Additivity**: For combined models, $\phi_i = \phi_i^{(1)} + \phi_i^{(2)}$

### Causal Inference

**Pearl's do-calculus** provides rules for causal reasoning:

**Rule 1 (Insertion/Deletion of Observations)**:
$$
P(y | \text{do}(x), z, w) = P(y | \text{do}(x), w) \text{ if } (Y \perp Z | X, W)_{G_{\overline{X}}}
$$

**Rule 2 (Action/Observation Exchange)**:
$$
P(y | \text{do}(x), \text{do}(z), w) = P(y | \text{do}(x), z, w) \text{ if } (Y \perp Z | X, W)_{G_{\overline{X}\underline{Z}}}
$$

**Rule 3 (Insertion/Deletion of Actions)**:
$$
P(y | \text{do}(x), \text{do}(z), w) = P(y | \text{do}(x), w) \text{ if } (Y \perp Z | X, W)_{G_{\overline{X}\overline{Z(W)}}}
$$

**Backdoor Criterion**: A set $Z$ satisfies the backdoor criterion if:
1. No node in $Z$ is a descendant of $X$
2. $Z$ blocks all backdoor paths from $X$ to $Y$

**Average Treatment Effect (ATE)**:
$$
\text{ATE} = E[Y | \text{do}(X=1)] - E[Y | \text{do}(X=0)]
$$

### Differential Privacy

**ε-Differential Privacy**: A mechanism $M$ is $\varepsilon$-differentially private if for all datasets $D_1, D_2$ differing by one row:

$$
\frac{P(M(D_1) \in S)}{P(M(D_2) \in S)} \leq e^\varepsilon
$$

**Laplace Mechanism**:
$$
M(D) = f(D) + \text{Lap}\left(\frac{\Delta f}{\varepsilon}\right)
$$

Where:
- $\Delta f$ = Global sensitivity = $\max_{D_1, D_2} |f(D_1) - f(D_2)|$
- $\text{Lap}(b)$ = Laplace distribution with scale $b$

**Gaussian Mechanism** (for $(\varepsilon, \delta)$-DP):
$$
M(D) = f(D) + \mathcal{N}\left(0, \frac{2\Delta f^2 \ln(1.25/\delta)}{\varepsilon^2}\right)
$$

**Privacy Budget Tracking**:
- **Basic Composition**: $k$ queries with $\varepsilon_i$ → Total privacy loss = $\sum_{i=1}^{k} \varepsilon_i$
- **Advanced Composition**: Tighter bounds using Rényi Differential Privacy

---

## 🛠️ API Stability

- **Stable APIs** (✅): `ShapExplainer`, `LimeExplainer`, `PermutationExplainer`, `TreeExplainer`
- **Experimental APIs** (⚠️): `CausalGraph`, `DoCalculusOperator`, `AttentionMapExtractor`, `PrivacyBudgetTracker`
- **Deprecation Policy**: 6-month grace period with migration guides

See [STABILITY.md](../STABILITY.md) for full details.

---

## 🗺️ Roadmap

- **v1.2.0** (Q2 2026): Streaming XAI, temporal explanations
- **v1.3.0** (Q4 2026): GPU acceleration, SIMD optimizations
- **v2.0.0** (2027): API modernization, reactive programming

See [ROADMAP.md](../ROADMAP.md) for details.

---

## 🤝 Contributing

We welcome contributions! Check our:

- [Contributing Guidelines](Contributing.md)
- [Code of Conduct](Code-of-Conduct.md)
- [Development Setup](Development-Setup.md)

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/Thung0808/xai_jvm/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Thung0808/xai_jvm/discussions)
- **Email**: thung0808@github.com

---

**Last Updated**: January 5, 2026 (v1.1.0)
