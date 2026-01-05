# Test XAI Library

Simple demo project to test `io.github.thung0808:xai-core:1.0.0` library.

## Quick Start

**Build:**
```bash
cd test-xai-library
mvn clean compile
```

**Run demo:**
```bash
mvn exec:java -Dexec.mainClass="io.github.thung0808.test.DemoXaiLibrary"
```

## What It Does

- Creates a simple mock binary classifier
- Uses `PermutationExplainer` to generate feature attributions
- Displays results showing feature importance

## Expected Output

```
=== XAI Core Library Demo ===

✓ Model created (mock binary classifier)
  Input: 2 features [feature_0, feature_1]
  Output: probability of class 1

✓ Explainer created: PermutationExplainer

Test instance: [0.7, 0.3]

Generating explanation...

=== Explanation Results ===

Model prediction: 0.3200
Explainer: PermutationExplainer
Timestamp: 2026-01-05T...

=== Feature Attributions ===
Feature 0: +0.3000 (importance: 75.00%)
Feature 1: -0.2000 (importance: 25.00%)

=== Interpretation ===
• Positive attribution → feature increases prediction
• Negative attribution → feature decreases prediction
• Higher absolute value → stronger influence

✓ Demo completed successfully!
```

## Dependencies

- `io.github.thung0808:xai-core:1.0.0` — XAI Core library
- Java 21+
- Maven 3.6+
