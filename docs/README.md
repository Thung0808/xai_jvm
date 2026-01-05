# XAI Core Documentation

Welcome to the **XAI Core Wiki**! This comprehensive documentation provides guides, tutorials, and mathematical foundations for explainable AI on the JVM.

---

## 📖 Table of Contents

### Getting Started
- [Installation Guide](Installation-Guide.md)
- [Quick Start Tutorial](Quick-Start.md)
- [Examples Gallery](Examples.md)

### Core Explainability Methods
- [SHAP Guide](SHAP-Guide.md) - Shapley Additive Explanations
- [LIME Guide](LIME-Guide.md) - Local Interpretable Model-agnostic Explanations
- [Permutation Importance](Permutation-Guide.md)
- [Tree Explainer](Tree-Guide.md) - Native decision tree interpretation

### Advanced Features (v1.1.0)
- [Causal AI](Causal-AI.md) ⚠️ - Pearl's do-calculus and counterfactuals
- [Privacy-Preserving XAI](Privacy-XAI.md) ⚠️ - Differential privacy
- [LLM Explainability](LLM-XAI.md) ⚠️ - Transformer attention maps
- [Interactive What-If](What-If-Guide.md) ⚠️ - Real-time counterfactuals

### Production Guides
- [Performance Tuning](Performance-Tuning.md)
- [Deployment Strategies](Deployment.md)
- [Monitoring & Observability](Monitoring.md)
- [Security Best Practices](Security.md)

### Project Information
- [API Stability Guarantees](../STABILITY.md)
- [Roadmap](../ROADMAP.md) - v1.2.0 to v2.0.0
- [Contributing Guidelines](Contributing.md)
- [Code of Conduct](Code-of-Conduct.md)

---

## 🚀 Quick Links

- **Maven Central**: [io.github.thung0808:xai-core:1.1.0](https://central.sonatype.com/artifact/io.github.thung0808/xai-core/1.1.0)
- **GitHub Repository**: [Thung0808/xai_jvm](https://github.com/Thung0808/xai_jvm)
- **Release Notes**: [v1.1.0](../RELEASE-NOTES-1.1.0.md)
- **Issues**: [Bug Reports & Feature Requests](https://github.com/Thung0808/xai_jvm/issues)

---

## 🌟 What's New in v1.1.0

### Causal AI Module
Perform causal inference with Pearl's do-calculus:
- Directed Acyclic Graphs (DAGs)
- Average Treatment Effect (ATE)
- Backdoor/Frontdoor adjustment
- Counterfactual reasoning

[Read full guide →](Causal-AI.md)

### Privacy-Preserving XAI
Protect sensitive data with differential privacy:
- ε-δ privacy guarantees
- Privacy budget tracking
- Federated explanation aggregation
- GDPR/HIPAA compliance

[Read full guide →](Privacy-XAI.md)

### LLM Explainability
Explain transformer models:
- Attention weight extraction
- Token saliency maps
- ONNX Runtime integration
- Multi-layer analysis

[Read full guide →](LLM-XAI.md)

### Interactive What-If
Real-time counterfactual predictions:
- WebSocket dashboard integration
- Feature perturbation
- Surrogate model approximation
- Credit approval scenarios

[Read full guide →](What-If-Guide.md)

---

## 📚 Mathematical Foundations

### SHAP Theory

Shapley values from cooperative game theory:

$$
\phi_i = \sum_{S \subseteq N \setminus \{i\}} \frac{|S|!(n-|S|-1)!}{n!} [f(S \cup \{i\}) - f(S)]
$$

**Properties**: Efficiency, Symmetry, Dummy, Additivity

### Causal Inference

Pearl's do-calculus for interventions:

**Average Treatment Effect**:
$$
\text{ATE} = E[Y | \text{do}(X=1)] - E[Y | \text{do}(X=0)]
$$

**Backdoor Adjustment**:
$$
P(y | \text{do}(x)) = \sum_z P(y | x, z) P(z)
$$

### Differential Privacy

ε-DP definition:
$$
\frac{P(M(D_1) \in S)}{P(M(D_2) \in S)} \leq e^\varepsilon
$$

**Laplace Mechanism**:
$$
M(D) = f(D) + \text{Lap}\left(\frac{\Delta f}{\varepsilon}\right)
$$

---

## 🎯 Use Case Guides

- [Financial Services](Use-Case-Finance.md) - Credit scoring, fraud detection
- [Healthcare](Use-Case-Healthcare.md) - Diagnosis support, treatment planning
- [E-commerce](Use-Case-Ecommerce.md) - Recommendations, churn prediction
- [Manufacturing](Use-Case-Manufacturing.md) - Predictive maintenance

---

## 🛠️ API Reference

### Core Classes

```java
// SHAP Explainer
ShapExplainer explainer = new ShapExplainer(model, featureNames);
double[] shapValues = explainer.explain(instance);

// Causal Graph
CausalGraph graph = new CausalGraph();
graph.addVariable("treatment");
graph.addEdge("treatment", "outcome", 0.7);

// Differential Privacy
DifferentialPrivacyMechanism dp = new DifferentialPrivacyMechanism(1.0, 1e-5);
double[] privateExplanation = dp.addNoise(shapValues, "Laplace");

// LLM Attention
AttentionMapExtractor extractor = new AttentionMapExtractor(onnxPath);
double[][][][] attention = extractor.extractAttention(text);
```

### Stability Levels

- ✅ **Stable**: `ShapExplainer`, `LimeExplainer`, `PermutationExplainer`
- ⚠️ **Experimental**: `CausalGraph`, `AttentionMapExtractor`, `PrivacyBudgetTracker`
- 🔒 **Internal**: Implementation details (may change)

See [STABILITY.md](../STABILITY.md) for full API guarantees.

---

## 📊 Performance Benchmarks

| Method | Latency (p50) | Throughput | Memory |
|--------|---------------|------------|--------|
| SHAP (pooled) | 0.273 μs | 3.6M ops/sec | 128 MB |
| LIME | 1.2 ms | 830 ops/sec | 64 MB |
| Permutation | 45 ms | 22 ops/sec | 32 MB |
| Tree Explainer | 0.8 μs | 1.2M ops/sec | 16 MB |

*Tested on: Java 21, Ryzen 9 5950X, 64GB RAM*

---

## 🤝 Community

### Get Involved

- 🐛 [Report Bugs](https://github.com/Thung0808/xai_jvm/issues/new?labels=bug)
- 💡 [Request Features](https://github.com/Thung0808/xai_jvm/issues/new?labels=enhancement)
- 🤝 [Submit Pull Requests](https://github.com/Thung0808/xai_jvm/pulls)
- 💬 [Join Discussions](https://github.com/Thung0808/xai_jvm/discussions)

### Share the Project

- [Twitter](https://twitter.com/intent/tweet?text=Check%20out%20XAI%20Core%20-%20Enterprise%20Explainable%20AI%20for%20JVM!&url=https://github.com/Thung0808/xai_jvm)
- [LinkedIn](https://www.linkedin.com/sharing/share-offsite/?url=https://github.com/Thung0808/xai_jvm)
- [Reddit](https://www.reddit.com/submit?url=https://github.com/Thung0808/xai_jvm&title=XAI%20Core)
- [Hacker News](https://news.ycombinator.com/submitlink?u=https://github.com/Thung0808/xai_jvm)

---

## 📞 Support

- **Documentation**: You're here! 📖
- **GitHub Issues**: [Technical support](https://github.com/Thung0808/xai_jvm/issues)
- **Email**: thung0808@github.com

---

## 📄 License

MIT License - See [LICENSE](../LICENSE) for details.

---

**Last Updated**: January 5, 2026 (v1.1.0)  
**Built with ❤️ for the JVM ecosystem**
