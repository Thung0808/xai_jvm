# API Stability Guarantee

## Semantic Versioning

XAI Core follows [Semantic Versioning 2.0.0](https://semver.org/):

- **MAJOR** version (x.0.0): Breaking API changes
- **MINOR** version (1.x.0): New features, backward compatible
- **PATCH** version (1.1.x): Bug fixes, backward compatible

**Current Stable Version**: `1.1.0`

---

## Stability Levels

### 1. **Stable APIs** ✓

**Guarantee**: Backward compatible within major version (1.x.x)

**Packages**:
- `io.github.thung0808.xai.api.*` - Core interfaces
- `io.github.thung0808.xai.explainer.*` - Explanation algorithms
- `io.github.thung0808.xai.adapter.*` - ML framework adapters

**Promise**:
- No breaking changes to public methods
- Deprecation warnings 6 months before removal
- Migration guides for all breaking changes

**Example**:
```java
// Stable - will not break in 1.x
PredictiveModel model = ...;
Explainer explainer = new PermutationExplainer(model);
Explanation explanation = explainer.explain(features);
```

---

### 2. **Experimental APIs** ⚠️

**Annotation**: `@Experimental(since = "1.1.0")`

**Guarantee**: API may change without notice in minor versions

**Packages**:
- `io.github.thung0808.xai.causal.*` - Causal AI & DAG
- `io.github.thung0808.xai.llm.*` - LLM Explainability
- `io.github.thung0808.xai.privacy.*` - Differential Privacy
- `io.github.thung0808.xai.interactive.*` - What-If Simulation

**Why Experimental?**:
- New features still gathering user feedback
- API design may evolve based on real-world usage
- Performance optimizations may require signature changes

**Stability Timeline**:
- `1.1.0` → `1.2.0`: Experimental (6 months)
- `1.3.0`: Promoted to Stable after feedback integration

**Example**:
```java
// Experimental - API may change
@Experimental(since = "1.1.0")
CausalGraph dag = new CausalGraph();
dag.addEdge("age", "income", 0.3);
```

---

### 3. **Internal APIs** 🔒

**Annotation**: Package-private or `@Internal`

**Guarantee**: No stability guarantee, may break anytime

**Packages**:
- `io.github.thung0808.xai.internal.*` - Internal utilities
- Package-private classes in any module

**Policy**: Do not use in production code

---

## Deprecation Policy

### Process

1. **Deprecation Notice** (Version N):
   - Mark with `@Deprecated(since = "N", forRemoval = true)`
   - Add Javadoc explaining migration path
   - Log warning on first use

2. **Grace Period** (6 months minimum):
   - Feature remains functional
   - Migration guide published
   - Alternative API provided

3. **Removal** (Version N+2 or later):
   - Deprecated API removed
   - Compile-time error for users

### Example

```java
/**
 * @deprecated Since 1.2.0, replaced by {@link NewExplainer#explain(double[])}
 * Will be removed in 2.0.0
 */
@Deprecated(since = "1.2.0", forRemoval = true)
public Explanation oldExplain(double[] features) {
    // ...
}
```

---

## Breaking Changes Policy

### What is NOT a Breaking Change

✓ Adding new methods to interfaces with default implementations
✓ Adding new classes or packages
✓ Adding optional parameters with defaults
✓ Throwing new unchecked exceptions (with documentation)
✓ Performance improvements
✓ Bug fixes that correct documented behavior

### What IS a Breaking Change

✗ Removing public methods
✗ Changing method signatures
✗ Changing return types
✗ Removing classes or packages
✗ Changing exception types (checked)
✗ Changing documented behavior semantics

---

## Version Compatibility Matrix

| Version | Java | Dependencies | Notes |
|---------|------|--------------|-------|
| 1.0.0   | 21+  | Smile 2.6, SLF4J 2.0 | Initial stable release |
| 1.1.0   | 21+  | + ONNX 1.17, GSON 2.10 | Causal AI, LLM XAI, Privacy |
| 1.2.0   | 21+  | + Panama Vector API | Planned: Streaming XAI |
| 2.0.0   | 21+  | API cleanup | Planned: Remove deprecated APIs |

---

## Testing Strategy

### Before Minor Release (1.x.0)

- ✓ All unit tests passing (>95% coverage)
- ✓ Integration tests with Smile, ONNX Runtime
- ✓ Performance benchmarks (no regression >5%)
- ✓ API compatibility check (japicmp)

### Before Major Release (x.0.0)

- ✓ All of the above
- ✓ Migration guide for breaking changes
- ✓ Beta release for early adopters (2 weeks)

---

## Support Policy

### Active Support

- **Latest MINOR** (e.g., 1.1.x): Bug fixes, security patches, new features
- **Previous MINOR** (e.g., 1.0.x): Critical bug fixes only (3 months)

### End-of-Life

- **Older versions** (< 1.0.0): No support, upgrade recommended

---

## Experimental → Stable Promotion Criteria

An experimental API graduates to stable when:

1. **Usage Maturity**: Used in production by ≥5 organizations
2. **API Stability**: No major design changes in 6 months
3. **Test Coverage**: ≥90% line coverage
4. **Documentation**: Complete Javadoc + user guide
5. **Performance**: Benchmarked and optimized
6. **Feedback Integration**: User feedback incorporated

---

## Reporting Issues

### Security Issues

**Contact**: Open GitHub Security Advisory (private)
**Response Time**: 48 hours
**Patch Timeline**: Critical fixes within 7 days

### API Stability Issues

**Contact**: GitHub Issues with `[API Stability]` label
**Response Time**: 5 business days
**Fix Timeline**: Next minor release or patch

---

## Commitment

We commit to:

- ✓ **Backward compatibility** within major versions
- ✓ **Clear deprecation notices** with migration paths
- ✓ **Semantic versioning** without surprises
- ✓ **Rapid security patches** for critical issues
- ✓ **Transparent roadmap** with version planning

**Last Updated**: January 5, 2026  
**Effective From**: Version 1.1.0
