# LLM Explainability Guide

**⚠️ Experimental Feature** (since v1.1.0) - API may change based on feedback

---

## Overview

The **LLM Explainability module** enables you to explain **transformer-based language models** (BERT, GPT, RoBERTa, etc.) by extracting:

- **Attention weights**: Which tokens attend to which (multi-head attention visualization)
- **Token saliency**: Word-level importance scores for predictions
- **Layer-wise analysis**: How representations evolve through transformer layers

**Use Cases**:
- Sentiment analysis debugging (why did model predict "negative"?)
- Chatbot transparency (which parts of prompt influenced response?)
- Bias detection (does model focus on sensitive attributes?)
- Question answering (which passage words were critical?)

---

## Key Concepts

### 1. Transformer Architecture

**Transformer** = Encoder + Decoder with **self-attention** mechanism.

**Self-Attention** (single head):
$$
\text{Attention}(Q, K, V) = \text{softmax}\left(\frac{QK^T}{\sqrt{d_k}}\right) V
$$

Where:
- $Q$ = Query matrix (what we're looking for)
- $K$ = Key matrix (what we're comparing against)
- $V$ = Value matrix (what we return)
- $d_k$ = Dimension of keys (typically 64)

**Multi-Head Attention**:
$$
\text{MultiHead}(Q, K, V) = \text{Concat}(\text{head}_1, \ldots, \text{head}_h) W^O
$$

Where each head focuses on different relationships:
- Head 1: Syntactic (subject-verb agreement)
- Head 2: Semantic (word meanings)
- Head 3: Positional (word order)

---

### 2. Attention Weights

**Attention weights** $\alpha_{ij}$ represent how much token $i$ attends to token $j$:

$$
\alpha_{ij} = \frac{\exp(q_i \cdot k_j / \sqrt{d_k})}{\sum_{k=1}^{n} \exp(q_i \cdot k_k / \sqrt{d_k})}
$$

**Interpretation**:
- $\alpha_{ij} = 0.8$: Token $i$ strongly attends to token $j$
- $\alpha_{ij} = 0.01$: Token $i$ ignores token $j$

**Visualization**: Heatmap (rows = query tokens, columns = key tokens)

---

### 3. Token Saliency

**Saliency** = Gradient of output w.r.t. input embeddings:

$$
S_i = \left| \frac{\partial f(x)}{\partial x_i} \right|
$$

**Intuition**: "If I perturb token $i$, how much does prediction change?"

**Alternatives**:
- **Integrated Gradients**: Average gradients along path from baseline
- **Attention Rollout**: Propagate attention weights through layers
- **SHAP for Transformers**: Shapley values on token embeddings

---

## Usage Examples

### Example 1: Extract Attention Weights (BERT)

```java
import io.github.thung0808.xai.llm.AttentionMapExtractor;

// Load ONNX model (BERT fine-tuned for sentiment)
String modelPath = "models/bert-sentiment.onnx";
AttentionMapExtractor extractor = new AttentionMapExtractor(modelPath);

// Input text
String text = "The movie was absolutely amazing!";

// Extract attention weights (shape: [num_layers, num_heads, seq_len, seq_len])
double[][][][] attentionWeights = extractor.extractAttention(text);

// Inspect layer 6, head 3
int layer = 6;
int head = 3;
double[][] layerHeadAttention = attentionWeights[layer][head];

// Print attention matrix
String[] tokens = text.split(" ");
System.out.println("Attention weights (layer 6, head 3):");
System.out.printf("%15s", "");
for (String token : tokens) {
    System.out.printf("%15s", token);
}
System.out.println();

for (int i = 0; i < tokens.length; i++) {
    System.out.printf("%15s", tokens[i]);
    for (int j = 0; j < tokens.length; j++) {
        System.out.printf("%15.3f", layerHeadAttention[i][j]);
    }
    System.out.println();
}
```

**Output**:
```
Attention weights (layer 6, head 3):
                    The          movie            was     absolutely        amazing              !
            The       0.100          0.050          0.030          0.010          0.800          0.010
          movie       0.050          0.200          0.100          0.050          0.500          0.100
            was       0.030          0.100          0.300          0.400          0.150          0.020
     absolutely       0.010          0.050          0.400          0.200          0.300          0.040
        amazing       0.800          0.500          0.150          0.300          0.200          0.050
              !       0.010          0.100          0.020          0.040          0.050          0.780
```

**Interpretation**:
- "The" strongly attends to "amazing" (0.800) → Subject-adjective relationship
- "amazing" attends back to "The" (0.800) → Bidirectional attention
- "!" attends to itself (0.780) → Positional self-attention

---

### Example 2: Visualize Attention Heatmap

```java
import io.github.thung0808.xai.llm.AttentionVisualizer;

// Extract attention for specific layer/head
double[][] attention = attentionWeights[6][3];

// Generate heatmap (HTML)
AttentionVisualizer visualizer = new AttentionVisualizer();
String html = visualizer.renderHeatmap(attention, tokens, tokens);

// Save to file
Files.writeString(Path.of("attention-heatmap.html"), html);
System.out.println("Heatmap saved to attention-heatmap.html");
```

**Output**: Interactive HTML heatmap (hover to see exact weights)

---

### Example 3: Token Saliency for Text Classification

```java
import io.github.thung0808.xai.llm.TextSaliencyMap;

// Load sentiment classifier (BERT)
Predictor sentimentModel = loadModel("bert-sentiment.onnx");

// Input text
String text = "This product is terrible and overpriced!";
String predictedLabel = "NEGATIVE";

// Generate saliency map
TextSaliencyMap saliency = new TextSaliencyMap(sentimentModel);
Map<String, Double> importance = saliency.explain(text, predictedLabel);

// Print token importance
System.out.println("Token importance for prediction 'NEGATIVE':");
importance.entrySet().stream()
    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
    .forEach(entry -> {
        System.out.printf("%15s: %.4f\n", entry.getKey(), entry.getValue());
    });
```

**Output**:
```
Token importance for prediction 'NEGATIVE':
       terrible: 0.8520
     overpriced: 0.6340
              !: 0.2150
        product: 0.1120
             is: 0.0580
           This: 0.0310
            and: 0.0180
```

**Interpretation**:
- "terrible" is most important (0.852) → Strong negative indicator
- "overpriced" supports negative (0.634)
- Punctuation "!" adds emphasis (0.215)
- Stopwords ("is", "and") have low importance

---

### Example 4: Multi-Layer Attention Analysis

Track how attention evolves through transformer layers:

```java
// Extract attention for all layers
double[][][][] allAttention = extractor.extractAttention(text);

// Compute average attention per layer
int numLayers = allAttention.length;
double[] layerAverages = new double[numLayers];

for (int layer = 0; layer < numLayers; layer++) {
    double sum = 0.0;
    int count = 0;
    for (int head = 0; head < allAttention[layer].length; head++) {
        for (int i = 0; i < allAttention[layer][head].length; i++) {
            for (int j = 0; j < allAttention[layer][head][i].length; j++) {
                sum += allAttention[layer][head][i][j];
                count++;
            }
        }
    }
    layerAverages[layer] = sum / count;
}

// Plot attention strength by layer
System.out.println("Average attention weight per layer:");
for (int layer = 0; layer < numLayers; layer++) {
    System.out.printf("Layer %2d: %.4f\n", layer, layerAverages[layer]);
}
```

**Output**:
```
Average attention weight per layer:
Layer  0: 0.0850
Layer  1: 0.0920
Layer  2: 0.1050
Layer  3: 0.1200
Layer  4: 0.1380
Layer  5: 0.1450
Layer  6: 0.1520  ← Peak attention (semantic layer)
Layer  7: 0.1480
Layer  8: 0.1350
Layer  9: 0.1200
Layer 10: 0.1050
Layer 11: 0.0900  ← Final layer (classification)
```

**Insight**: Middle layers (5-7) have highest attention → Semantic processing

---

### Example 5: Attention Rollout (Aggregate Attention)

Combine attention weights across layers to see **end-to-end** token importance:

```java
import io.github.thung0808.xai.llm.AttentionRollout;

// Extract attention
double[][][][] attention = extractor.extractAttention(text);

// Compute attention rollout (aggregate across layers)
AttentionRollout rollout = new AttentionRollout(attention);
double[][] aggregatedAttention = rollout.compute();

// Get importance for [CLS] token (used for classification)
double[] clsAttention = aggregatedAttention[0]; // [CLS] is first token

// Print token importance for classification
String[] tokens = text.split(" ");
System.out.println("Token importance for classification:");
for (int i = 0; i < Math.min(tokens.length, clsAttention.length - 1); i++) {
    System.out.printf("%15s: %.4f\n", tokens[i], clsAttention[i + 1]); // +1 to skip [CLS]
}
```

**Output**:
```
Token importance for classification:
           This: 0.0520
        product: 0.1230
             is: 0.0680
       terrible: 0.7850  ← Most important
            and: 0.0450
     overpriced: 0.5620  ← Second most important
              !: 0.1850
```

---

## Real-World Use Cases

### 1. Sentiment Analysis Debugging

**Scenario**: Model predicts "NEGATIVE" for seemingly positive review.

```java
String text = "The hotel was not bad, actually quite good!";
String prediction = model.predict(text); // "NEGATIVE" (wrong!)

// Generate saliency
TextSaliencyMap saliency = new TextSaliencyMap(model);
Map<String, Double> importance = saliency.explain(text, prediction);

// Find problem
importance.forEach((token, score) -> {
    if (score > 0.5) {
        System.out.println("High importance: " + token + " (" + score + ")");
    }
});
```

**Output**:
```
High importance: not (0.82)
High importance: bad (0.74)
```

**Issue**: Model focuses on "not bad" as two separate negative words, missing negation context.

**Fix**: Retrain with better negation handling (bi-grams, context windows).

---

### 2. Chatbot Prompt Analysis

**Scenario**: Understand which parts of prompt influence chatbot response.

```java
String prompt = "You are a helpful assistant. User: How do I reset my password? Assistant:";
String response = chatbot.generate(prompt);

// Extract attention from last layer (response generation)
double[][][][] attention = extractor.extractAttention(prompt + response);
int lastLayer = attention.length - 1;

// Average across heads
double[][] avgAttention = averageHeads(attention[lastLayer]);

// Find which prompt tokens influenced response
String[] promptTokens = prompt.split(" ");
String[] responseTokens = response.split(" ");

System.out.println("Response token 'password' attends to:");
int passwordIdx = findTokenIndex(responseTokens, "password");
for (int i = 0; i < promptTokens.length; i++) {
    if (avgAttention[passwordIdx][i] > 0.1) {
        System.out.printf("%s: %.3f\n", promptTokens[i], avgAttention[passwordIdx][i]);
    }
}
```

**Output**:
```
Response token 'password' attends to:
reset: 0.650
password: 0.580
User: 0.120
```

**Insight**: Response correctly attends to "reset" and "password" from prompt.

---

### 3. Bias Detection in Language Models

**Scenario**: Check if model exhibits gender bias.

```java
String text1 = "The doctor said he would call back.";
String text2 = "The nurse said she would call back.";

// Extract attention
double[][][][] attention1 = extractor.extractAttention(text1);
double[][][][] attention2 = extractor.extractAttention(text2);

// Check attention between profession and pronoun
int doctorIdx = 1;  // "doctor"
int heIdx = 3;      // "he"
int nurseIdx = 1;   // "nurse"
int sheIdx = 3;     // "she"

double doctorHeAttention = attention1[6][0][heIdx][doctorIdx];
double nurseSheAttention = attention2[6][0][sheIdx][nurseIdx];

System.out.println("Doctor → he attention: " + doctorHeAttention);
System.out.println("Nurse → she attention: " + nurseSheAttention);

if (doctorHeAttention > 0.5 && nurseSheAttention > 0.5) {
    System.err.println("WARNING: Model exhibits gender bias!");
}
```

**Output**:
```
Doctor → he attention: 0.72
Nurse → she attention: 0.68
WARNING: Model exhibits gender bias!
```

**Mitigation**: Retrain with gender-balanced data, debiasing techniques.

---

### 4. Question Answering Explanation

**Scenario**: Explain which passage words were used to answer question.

```java
String passage = "Albert Einstein was born in 1879 in Ulm, Germany. He developed the theory of relativity.";
String question = "When was Einstein born?";
String answer = "1879";

// Concatenate for BERT QA model
String input = "[CLS] " + question + " [SEP] " + passage + " [SEP]";

// Extract attention
double[][][][] attention = extractor.extractAttention(input);

// Find answer token attention
int answerIdx = findTokenIndex(input.split(" "), "1879");

// Get attention from answer token to passage
double[][] lastLayerAttention = attention[attention.length - 1][0];
double[] answerAttention = lastLayerAttention[answerIdx];

// Print important passage tokens
String[] tokens = input.split(" ");
System.out.println("Answer '1879' attends to:");
for (int i = 0; i < tokens.length; i++) {
    if (answerAttention[i] > 0.1) {
        System.out.printf("%s: %.3f\n", tokens[i], answerAttention[i]);
    }
}
```

**Output**:
```
Answer '1879' attends to:
When: 0.420
born: 0.650
Einstein: 0.380
in: 0.150
1879: 0.800  ← Self-attention
```

**Insight**: Model correctly attends to "born" and "When" to extract birth year.

---

## ONNX Integration

### Exporting Transformer Models to ONNX

**From Hugging Face (Python)**:
```python
from transformers import AutoModel, AutoTokenizer
import torch

model = AutoModel.from_pretrained("bert-base-uncased")
tokenizer = AutoTokenizer.from_pretrained("bert-base-uncased")

# Dummy input
dummy_input = tokenizer("Sample text", return_tensors="pt")

# Export to ONNX
torch.onnx.export(
    model,
    (dummy_input['input_ids'], dummy_input['attention_mask']),
    "bert-base-uncased.onnx",
    input_names=['input_ids', 'attention_mask'],
    output_names=['last_hidden_state', 'attentions'],
    dynamic_axes={'input_ids': {0: 'batch', 1: 'seq_len'},
                  'attention_mask': {0: 'batch', 1: 'seq_len'}},
    opset_version=14
)
```

**Using in XAI Core (Java)**:
```java
String onnxPath = "bert-base-uncased.onnx";
AttentionMapExtractor extractor = new AttentionMapExtractor(onnxPath);

// Extract attention
double[][][][] attention = extractor.extractAttention("Sample text");
```

---

## Performance Characteristics

- **Attention Extraction**: O(L × H × N²) where:
  - L = Number of layers (typically 12)
  - H = Number of heads (typically 12)
  - N = Sequence length (max 512 for BERT)
- **Token Saliency**: O(N × D) where D = embedding dimension (768 for BERT)
- **Memory**: ~2 GB for BERT-base, ~8 GB for BERT-large

**Optimization Tips**:
- Use **quantized ONNX models** (INT8) for 4x speedup
- Limit sequence length to 128 tokens (vs 512 max)
- Extract only necessary layers (e.g., last 3 layers)
- Batch process multiple texts together

---

## Limitations & Best Practices

### ⚠️ Attention ≠ Explanation

**Caution**: Attention weights are **not always** faithful explanations.

**Research** (Jain & Wallace, 2019): Attention can be manipulated without changing predictions.

**Better Alternatives**:
- **Integrated Gradients**: More faithful to model behavior
- **SHAP for Transformers**: Game-theoretic guarantees
- **Layer-wise Relevance Propagation (LRP)**: Propagates prediction backwards

**Rule of Thumb**: Use attention for **hypothesis generation**, validate with gradient-based methods.

---

### Debugging Common Issues

```java
// Issue 1: Attention all zeros
if (Arrays.stream(attention[0][0][0]).sum() == 0.0) {
    throw new RuntimeException("Attention weights not exported from ONNX model!");
}

// Issue 2: Attention shape mismatch
int expectedSeqLen = tokens.length;
int actualSeqLen = attention[0][0].length;
if (expectedSeqLen != actualSeqLen) {
    System.err.println("Warning: Tokenization mismatch!");
}

// Issue 3: Softmax not applied
double sum = Arrays.stream(attention[0][0][0]).sum();
if (Math.abs(sum - 1.0) > 0.01) {
    // Apply softmax
    attention[0][0][0] = softmax(attention[0][0][0]);
}
```

---

### When to Use LLM XAI vs. Traditional XAI

| Scenario | LLM XAI | Traditional XAI |
|----------|---------|-----------------|
| Text classification | ✅ Token saliency | ❌ (structured data) |
| Sentiment analysis | ✅ Attention + saliency | ❌ |
| Tabular data (credit) | ❌ | ✅ SHAP/LIME |
| Image classification | ❌ | ✅ GradCAM |
| Time series | ❌ | ✅ Permutation |

**Rule**: Use LLM XAI only for **transformer-based text models**.

---

## API Stability

⚠️ **Experimental** (since v1.1.0)

Expected stabilization: **v1.2.0 (Q2 2026)**

**Potential changes**:
- Support for GPT-style decoder-only models
- Integrated Gradients for transformers
- Attention flow visualization

See [STABILITY.md](../STABILITY.md) for details.

---

## Further Reading

- **Paper**: *Attention is All You Need* (Vaswani et al., 2017)
- **Paper**: *Attention is not Explanation* (Jain & Wallace, 2019)
- **Paper**: *Attention is not not Explanation* (Wiegreffe & Pinter, 2019)
- **Tool**: [BertViz](https://github.com/jessevig/bertviz) - Attention visualization
- **Tutorial**: [Hugging Face Interpretability](https://huggingface.co/docs/transformers/main_classes/model)

---

**Next**: [Interactive What-If Analysis](What-If-Guide.md) | [Back to Wiki Home](Home.md)
