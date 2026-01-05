# XAI Core - Public Roadmap

**Vision**: Enterprise-grade Explainable AI for the JVM ecosystem with production-ready performance, regulatory compliance, and cutting-edge research integration.

---

## Released Versions

### ✅ v1.0.0 (Stable) - October 2025

**Theme**: Foundation & Production-Ready Core

**Features**:
- ✓ Core XAI API (`PredictiveModel`, `Explainer`, `Explanation`)
- ✓ Permutation-based feature importance
- ✓ LIME (Local Interpretable Model-agnostic Explanations)
- ✓ SHAP approximation
- ✓ ML framework adapters (Smile integration)
- ✓ Performance benchmarks & optimization
- ✓ Published to Maven Central

**Stats**:
- 15,000+ lines of code
- 95%+ test coverage
- Sub-millisecond explanations

---

### ✅ v1.1.0 (Stable) - January 2026

**Theme**: Advanced AI Explainability & Privacy-First

**Features**:
- ✓ **Causal AI** - DAG, Do-calculus, Backdoor criterion
- ✓ **LLM Explainability** - Attention maps, Token saliency
- ✓ **Privacy-Preserving XAI** - Differential privacy, Federated learning
- ✓ **Interactive What-If** - Real-time simulation engine
- ✓ **Maven Profiles** - Minimal (500 KB) vs Full (1.2 MB)
- ✓ **Privacy Budget Tracker** - Reconstruction attack prevention
- ✓ **XAI Dashboard 2.0** - Cytoscape.js, Token highlighting, Live playground

**New Packages**:
- `io.github.thung0808.xai.causal.*`
- `io.github.thung0808.xai.llm.*`
- `io.github.thung0808.xai.privacy.*`
- `io.github.thung0808.xai.interactive.*`

**Stats**:
- 25,000+ lines of code
- 4 major features (Causal, LLM, Privacy, What-If)
- Enterprise-grade privacy guarantees

---

## Planned Releases

### 🚧 v1.2.0 (Q2 2026) - Streaming XAI

**Theme**: Real-Time Explainability for Data Streams

**Goals**:
- Handle time-series and streaming data
- Incremental explanation updates
- Low-latency explanations (<100μs)

**Features**:

#### 1. Streaming Feature Attribution
- Incremental SHAP computation for streams
- Rolling window explanations
- Drift detection & adaptation

```java
StreamingExplainer explainer = new StreamingExplainer(model);
stream.forEach(dataPoint -> {
    Explanation exp = explainer.explainIncremental(dataPoint);
    // Real-time dashboard update
});
```

#### 2. Temporal Explanation
- Time-aware feature importance
- Lagged feature analysis
- Sequence-to-sequence explanations

```java
TemporalExplainer explainer = new TemporalExplainer(model);
Explanation exp = explainer.explainTimeSeries(sequence, timesteps=10);
```

#### 3. Anomaly Detection + XAI
- Explain why a data point is anomalous
- Counterfactual normal examples
- Threshold adaptation

**Dependencies**:
- Apache Kafka Streams (optional)
- RxJava 3.x (reactive streams)

**Performance Target**:
- Throughput: 100,000 explanations/sec
- Latency: p99 < 100μs

---

### 🔮 v1.3.0 (Q4 2026) - GPU Acceleration & Panama Vector API

**Theme**: Hardware Acceleration for Production Scale

**Goals**:
- 10-100x speedup for explanation algorithms
- Native SIMD vectorization
- GPU offloading for heavy workloads

**Features**:

#### 1. Panama Vector API Integration
- SIMD-optimized SHAP computation
- Vectorized feature attribution
- Native memory access (zero-copy)

```java
@VectorAccelerated
class SIMDExplainer extends PermutationExplainer {
    // Automatic SIMD vectorization via Panama
    double[] computeAttribution(double[] features) {
        // 10x faster than scalar version
    }
}
```

#### 2. GPU Offloading (CUDA/OpenCL)
- GPU-accelerated SHAP for large models
- Batch explanation generation
- Multi-GPU support

```java
GPUExplainer explainer = new GPUExplainer(model)
    .withDevice(GPUDevice.CUDA)
    .withBatchSize(10000);
    
List<Explanation> batch = explainer.explainBatch(dataPoints);
```

#### 3. Native Model Support
- ONNX Runtime GPU kernels
- TensorRT integration
- Apple Metal support (macOS/iOS)

**Dependencies**:
- JDK 21+ (Panama FFM API)
- CUDA Toolkit 12.x (optional)
- Apple Metal (macOS only)

**Performance Target**:
- SHAP: 100x speedup on GPU
- Memory footprint: 50% reduction via zero-copy

---

### 🔮 v2.0.0 (2027) - API Modernization & Cleanup

**Theme**: Breaking Changes for Long-Term Stability

**Goals**:
- Remove deprecated APIs from 1.x
- Modernize API based on user feedback
- Simplify complex abstractions

**Major Changes**:

#### 1. API Cleanup
- Remove deprecated methods (warned since 1.2.0)
- Consolidate similar interfaces
- Rename confusing classes

```java
// Old (1.x)
PermutationExplainer explainer = new PermutationExplainer(model);

// New (2.0)
Explainer explainer = Explainer.permutation(model);
```

#### 2. Unified Explanation Format
- Single `Explanation` class for all explainers
- JSON/Protobuf serialization
- Interoperability with Python (SHAP, LIME)

```java
Explanation exp = explainer.explain(features);
String json = exp.toJSON();  // Standard format
exp.saveProtobuf("explanation.pb");
```

#### 3. Async/Reactive API
- CompletableFuture-based async explanations
- Reactive Streams support
- Backpressure handling

```java
CompletableFuture<Explanation> future = 
    explainer.explainAsync(features);
```

**Migration Guide**: Provided 6 months before release

---

## Future Ideas (No Timeline)

### Research Integration
- **Counterfactual Explanations** - "What if X was Y?"
- **Concept-based XAI** - TCAV (Testing with Concept Activation Vectors)
- **Fairness-aware XAI** - Bias detection & mitigation

### Ecosystem Integration
- **Spring Boot Starter** - Auto-configuration for XAI
- **Quarkus Extension** - Native compilation support
- **MicroProfile Support** - Cloud-native XAI microservices

### Advanced Features
- **Multi-modal XAI** - Images, text, tabular combined
- **Neural Architecture Search + XAI** - Explain model architecture choices
- **Quantum ML Explainability** - For future quantum models

---

## Community Input

We welcome feedback! Influence the roadmap:

- **GitHub Discussions**: Feature requests & proposals
- **GitHub Issues**: Bug reports & enhancements
- **Surveys**: Quarterly user feedback surveys

**Voting**: Use 👍 reactions on GitHub Issues to prioritize features

---

## Version Timeline

```
2025 Q4  ████████████ v1.0.0 (Released)
2026 Q1  ████████████ v1.1.0 (Released)
2026 Q2  ░░░░░░░░░░░░ v1.2.0 (Planned)
2026 Q4  ░░░░░░░░░░░░ v1.3.0 (Planned)
2027 H1  ░░░░░░░░░░░░ v2.0.0 (Planned)
```

---

## Release Cadence

- **Minor Releases** (1.x.0): Every 3-6 months
- **Patch Releases** (1.1.x): As needed for bug fixes
- **Major Releases** (x.0.0): Every 12-18 months

---

## Experimental Features

Features marked `@Experimental` may graduate or be removed:

| Feature | Introduced | Status | Target Stable |
|---------|-----------|--------|---------------|
| Causal AI | 1.1.0 | Experimental | 1.3.0 |
| LLM XAI | 1.1.0 | Experimental | 1.3.0 |
| Privacy | 1.1.0 | Experimental | 1.2.0 |
| What-If | 1.1.0 | Experimental | 1.2.0 |

---

## Contributing to Roadmap

Want to contribute to a roadmap item?

1. Check [GitHub Projects](https://github.com/Thung0808/xai-core/projects) for status
2. Comment on related GitHub Issues
3. Submit PRs with `[Roadmap: v1.x.0]` prefix

**Maintainers**: Will review proposals monthly

---

**Last Updated**: January 5, 2026  
**Maintained By**: XAI Core Team  
**License**: Apache 2.0
