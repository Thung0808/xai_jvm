# XAI Core 1.1.0 Release Notes

**Release Date**: January 5, 2026  
**Maven Central**: `io.github.thung0808:xai-core:1.1.0`  
**Deployment ID**: `3fe0f913-5710-4bad-8a28-c677d7530f6c`  
**Git Tag**: `v1.1.0`

---

## 🚀 Major Features

### 1. **Causal AI Module** (⚠️ Experimental)
- **CausalGraph**: Directed Acyclic Graph (DAG) for causal relationships
- **DoCalculusOperator**: Pearl's do-calculus operations (do, backdoor, frontdoor)
- **Counterfactual Reasoning**: "What would happen if X were different?"
- **Use Cases**: Medical diagnosis, policy impact analysis, fraud detection

```java
CausalGraph graph = new CausalGraph();
graph.addVariable("smoking");
graph.addVariable("cancer");
graph.addEdge("smoking", "cancer", 0.7);

DoCalculusOperator doCalc = new DoCalculusOperator(graph);
double effect = doCalc.computeATE("smoking", "cancer", dataset);
System.out.println("Causal effect: " + effect);
```

---

### 2. **LLM Explainability Module** (⚠️ Experimental)
- **AttentionMapExtractor**: Extract attention weights from transformer models (BERT, GPT)
- **TextSaliencyMap**: Token-level importance for text classification
- **Supported Formats**: ONNX Runtime, native transformer models
- **Use Cases**: Sentiment analysis, chatbot debugging, bias detection

```java
AttentionMapExtractor extractor = new AttentionMapExtractor(onnxModelPath);
double[][] attention = extractor.extractAttention("The movie was amazing!");

TextSaliencyMap saliency = new TextSaliencyMap(model);
Map<String, Double> importance = saliency.explain("This product is terrible", "NEGATIVE");
```

---

### 3. **Privacy-Preserving XAI Module** (⚠️ Experimental)
- **Differential Privacy**: Laplace/Gaussian noise mechanisms (ε-δ privacy)
- **PrivacyBudgetTracker**: Privacy budget monitoring with reconstruction attack detection
- **FederatedExplanationAggregator**: Aggregate explanations across distributed data
- **Compliance**: GDPR Article 22, CCPA compliance-ready

```java
PrivacyBudgetTracker tracker = new PrivacyBudgetTracker(10.0);
DifferentialPrivacyMechanism dp = new DifferentialPrivacyMechanism(1.0, 1e-5);

double[] privateExplanation = dp.addNoise(rawImportance, "Laplace");
tracker.recordQuery(1.0, 1000); // epsilon=1.0, dataset_size=1000

AuditReport report = tracker.generateAuditReport();
System.out.println("Privacy Risk: " + report.getRisk()); // LOW/MEDIUM/HIGH
```

---

### 4. **Interactive What-If Module** (⚠️ Experimental)
- **WhatIfEngine**: Real-time counterfactual predictions
- **WebSocket Integration**: Spring Boot-based interactive dashboard
- **Feature Engineering**: User-defined constraints and ranges
- **Use Cases**: Credit approval "what-if", medical treatment planning

```java
WhatIfEngine engine = new WhatIfEngine(model, featureNames);

Map<String, Double> original = Map.of(
    "income", 50000.0,
    "credit_score", 650.0
);

Map<String, Double> modified = Map.of(
    "income", 70000.0,      // Increased income
    "credit_score", 700.0   // Improved credit score
);

WhatIfResult result = engine.evaluate(original, modified);
System.out.println("Original: " + result.getOriginalPrediction());
System.out.println("Modified: " + result.getModifiedPrediction());
System.out.println("Change: " + (result.getModifiedPrediction() - result.getOriginalPrediction()));
```

---

## 🛠️ Strategic Improvements

### 5. **Maven Build Profiles**
- **`minimal`**: Core XAI only (~500 KB, no ONNX/Spring Boot)
- **`full`**: All features (~1.2 MB, includes ONNX Runtime)

```bash
# Minimal build (production environments)
mvn clean package -P minimal

# Full build (development/research)
mvn clean package -P full
```

---

### 6. **Privacy Budget Tracker Enhancements**
- **Reconstruction Attack Detection**: Detect privacy leakage risks
- **Query History Auditing**: Timestamp, epsilon, dataset size tracking
- **Automated Alerts**: Thresholds at 50%, 75%, 90% budget consumption
- **Audit Report Generation**: JSON export for compliance teams

```java
tracker.recordQuery(2.0, 1000);   // 20% budget used
tracker.recordQuery(3.0, 1000);   // 50% budget used → Warning!

AuditReport report = tracker.generateAuditReport();
System.out.println("Total Queries: " + report.getQueryCount());
System.out.println("Budget Consumed: " + report.getBudgetConsumed() + "%");
System.out.println("Risk Level: " + report.getRisk());
System.out.println("Recommendation: " + report.getRecommendation());
```

---

### 7. **XAI Dashboard 2.0**
- **Multi-Module UI**: 4 tabs (Causal AI, LLM XAI, What-If, Privacy)
- **Causal Graph Visualization**: Cytoscape.js DAG rendering
- **Attention Heatmaps**: Transformer attention weight visualization
- **Interactive What-If Scenarios**: Real-time prediction updates
- **Privacy Budget Gauges**: Visual privacy consumption tracking

**Location**: `d:\xai-jvm\examples\xai-dashboard-2.0.html`  
**Dependencies**: Cytoscape.js 3.28.1, Chart.js 4.4.1, Bootstrap 5.3.2

---

## 📚 Enterprise Documentation

### 8. **API Stability Guarantees** (NEW)
- **File**: `STABILITY.md`
- **Semantic Versioning**: Major.Minor.Patch (1.1.0)
- **Stability Levels**:
  - ✅ **Stable**: No breaking changes within major version (1.x.x)
  - ⚠️ **Experimental**: 6-month experimental period, may change
  - 🔒 **Internal**: Private APIs, no guarantees
- **Deprecation Policy**: 6-month grace period + migration guides
- **Experimental Packages**: `causal`, `llm`, `privacy`, `interactive`

---

### 9. **Public Roadmap** (NEW)
- **File**: `ROADMAP.md`
- **v1.2.0** (Q2 2026): Streaming XAI, temporal explanations, anomaly detection
- **v1.3.0** (Q4 2026): GPU acceleration (CUDA/Metal), SIMD optimization
- **v2.0.0** (2027): API modernization, async/reactive programming, breaking changes
- **Community Input**: GitHub Issues voting for feature prioritization

---

### 10. **Experimental API Marking**
All new features annotated with `@Experimental(since = "1.1.0")`:
- Warns users about API stability
- Auto-documented in Javadocs
- Prevents accidental production usage
- Clear migration path to stable APIs

```java
@Experimental(since = "1.1.0")
public class CausalGraph {
    // Implementation subject to change
}
```

---

## 📦 Deployment Information

**Artifacts**:
- `xai-core-1.1.0.jar` (252 KB) - Compiled bytecode
- `xai-core-1.1.0-sources.jar` (143 KB) - Source code
- `xai-core-1.1.0-javadoc.jar` (4.8 MB) - API documentation
- All artifacts GPG-signed with key `4486441525DAC6928AA003A4D296A77E5555942B`

**Maven Central Deployment**:
- Status: **Uploaded (requires manual publishing)**
- Deployment ID: `3fe0f913-5710-4bad-8a28-c677d7530f6c`
- Publish URL: https://central.sonatype.com/publishing/deployments
- Expected Sync Time: ~2 hours after manual publish

**Usage**:
```xml
<dependency>
    <groupId>io.github.thung0808</groupId>
    <artifactId>xai-core</artifactId>
    <version>1.1.0</version>
</dependency>
```

---

## 🔧 Breaking Changes

**None** - Full backward compatibility with v1.0.0

All existing APIs (LIME, SHAP, PermutationExplainer, TreeExplainer, etc.) remain unchanged.

---

## 🐛 Bug Fixes

- Fixed class visibility issues in causal module
- Resolved differential privacy package structure
- Corrected WhatIfSimulationEngine file naming

---

## 📊 Statistics

- **Total Classes**: 71 Java classes
- **Lines of Code**: ~15,000 LOC
- **Test Coverage**: Core APIs (LIME, SHAP) tested
- **Documentation**: 100% Javadoc coverage
- **Dependencies**: 3 optional (ONNX, GSON, Spring Boot)

---

## 🔐 Security

- **GPG Signing**: All artifacts signed with verified GPG key
- **Privacy Compliance**: GDPR/CCPA-ready differential privacy
- **Vulnerability Scanning**: No known CVEs in dependencies

---

## 🙏 Acknowledgments

Special thanks to the open-source community:
- **ONNX Runtime**: LLM model inference
- **Cytoscape.js**: Causal graph visualization
- **Smile**: Machine learning utilities

---

## 📞 Support

- **GitHub Issues**: https://github.com/Thung0808/xai_jvm/issues
- **Documentation**: https://github.com/Thung0808/xai_jvm/wiki
- **Email**: thung0808@github.com

---

## 🎯 Next Steps

1. **Manual Publishing**: Visit https://central.sonatype.com/publishing/deployments
2. **Verify Publication**: Check Maven Central after ~2 hours
3. **Community Feedback**: Gather input on experimental APIs (6-month window)
4. **v1.2.0 Planning**: Begin work on streaming XAI features

---

**Released by**: thung0808  
**Release Type**: Official (Stable + Experimental features)  
**License**: MIT License
