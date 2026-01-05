/**
 * Core XAI (Explainable AI) framework for enterprise JVM applications.
 * 
 * <h2>Overview</h2>
 * <p>XAI Core provides production-grade model explainability with sub-millisecond latency,
 * designed for regulated industries and real-time systems.
 * 
 * <h2>Mathematical Foundation</h2>
 * <p>XAI Core implements Shapley values from cooperative game theory to attribute
 * predictions fairly across features:
 * 
 * <p><strong>Shapley Value Formula:</strong>
 * <blockquote>
 * $$\phi_i = \sum_{S \subseteq N \setminus \{i\}} \frac{|S|!(|N|-|S|-1)!}{|N|!} [f_{S \cup \{i\}}(x) - f_S(x)]$$
 * </blockquote>
 * 
 * <p>Where:
 * <ul>
 *   <li>φᵢ = attribution for feature i</li>
 *   <li>N = set of all features</li>
 *   <li>S = subset of features (coalition)</li>
 *   <li>f<sub>S</sub>(x) = model prediction using only features in S</li>
 * </ul>
 * 
 * <h2>Core Concepts</h2>
 * 
 * <h3>1. Local Explanations (Instance-level)</h3>
 * <p>Explain individual predictions:
 * <pre>{@code
 * // Prediction decomposition
 * prediction = baseValue + Σ φᵢ
 * 
 * // Example: Credit scoring
 * 0.87 = 0.50 + 0.25(age) + 0.18(income) - 0.08(debt) + ...
 * }</pre>
 * 
 * <h3>2. Global Explanations (Model-level)</h3>
 * <p>Understand model behavior across all instances:
 * <ul>
 *   <li><strong>Feature Importance:</strong> Σ|φᵢ| / N over all instances</li>
 *   <li><strong>Partial Dependence:</strong> ∂E[f(x)] / ∂xⱼ</li>
 *   <li><strong>Interactions:</strong> H(i,j) = Var[φᵢⱼ] / Var[f(x)]</li>
 * </ul>
 * 
 * <h3>3. Counterfactuals</h3>
 * <p>Minimal changes for different outcome:
 * <blockquote>
 * $$x' = \arg\min_{x'} \|x' - x\|_2 \text{ s.t. } f(x') = y'$$
 * </blockquote>
 * 
 * <h2>Architecture</h2>
 * <pre>
 * ┌─────────────────────────────────────────────┐
 * │         Application Layer                   │
 * │  Spring Boot / Micronaut / Standalone       │
 * └─────────────────┬───────────────────────────┘
 *                   │
 * ┌─────────────────▼───────────────────────────┐
 * │         XAI Core API                        │
 * │  ┌─────────┐  ┌──────────┐  ┌──────────┐  │
 * │  │Explainer│  │Explanation│ │TrustScore│  │
 * │  └─────────┘  └──────────┘  └──────────┘  │
 * └─────────────────┬───────────────────────────┘
 *                   │
 * ┌─────────────────▼───────────────────────────┐
 * │      Implementation Layer                   │
 * │  ┌──────────────┐  ┌────────────────────┐  │
 * │  │ Permutation  │  │ Tree/Linear (1.1)  │  │
 * │  │  Explainer   │  │    Explainers      │  │
 * │  └──────────────┘  └────────────────────┘  │
 * └─────────────────┬───────────────────────────┘
 *                   │
 * ┌─────────────────▼───────────────────────────┐
 * │       Performance Layer                     │
 * │  ┌────────┐  ┌───────────┐  ┌──────────┐  │
 * │  │ SIMD   │  │  Virtual  │  │  Object  │  │
 * │  │Vector  │  │  Threads  │  │  Pooling │  │
 * │  └────────┘  └───────────┘  └──────────┘  │
 * └─────────────────────────────────────────────┘
 * </pre>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * // 1. Create explainer
 * PermutationExplainer explainer = new PermutationExplainer(1000);
 * 
 * // 2. Explain prediction
 * Explanation explanation = explainer.explain(model, instance);
 * 
 * // 3. Get attributions
 * for (FeatureAttribution attr : explanation.getAttributions()) {
 *     System.out.printf("%s: %.4f\n", 
 *         attr.featureName(), attr.attribution());
 * }
 * }</pre>
 * 
 * <h3>High-Performance Mode</h3>
 * <pre>{@code
 * // Use SIMD acceleration (4x speedup)
 * VectorizedExplainer explainer = new VectorizedExplainer(500);
 * 
 * // Or use object pooling (zero allocation)
 * try (ExplanationPool pool = new ExplanationPool(100)) {
 *     ReusableExplanation explanation = pool.acquire();
 *     explainer.explain(model, instance, explanation);
 *     // ... use explanation ...
 * }
 * }</pre>
 * 
 * <h3>Model-Specific Optimizations (1.1.0-alpha)</h3>
 * <pre>{@code
 * // For tree models (10x faster)
 * TreeExplainer treeExplainer = new TreeExplainer();
 * Explanation exp1 = treeExplainer.explain(randomForest, instance);
 * 
 * // For linear models (1000x faster)
 * LinearExplainer linearExplainer = new LinearExplainer();
 * Explanation exp2 = linearExplainer.explain(linearRegression, instance);
 * }</pre>
 * 
 * <h3>Interactive Dashboard (0.7.0+)</h3>
 * <pre>{@code
 * // Generate and open HTML dashboard
 * HtmlDashboard.generateAndOpen(explanation);
 * 
 * // User sees professional dashboard in browser
 * // - Force plots with feature contributions
 * // - Interactive feature table
 * // - Trust score and metadata
 * }</pre>
 * 
 * <h2>Performance Characteristics</h2>
 * <table border="1">
 * <tr>
 *   <th>Explainer</th>
 *   <th>Latency</th>
 *   <th>Accuracy</th>
 *   <th>Use Case</th>
 * </tr>
 * <tr>
 *   <td>LinearExplainer</td>
 *   <td>~1μs</td>
 *   <td>Exact</td>
 *   <td>Linear models</td>
 * </tr>
 * <tr>
 *   <td>TreeExplainer</td>
 *   <td>~10μs</td>
 *   <td>Exact</td>
 *   <td>Tree ensembles</td>
 * </tr>
 * <tr>
 *   <td>VectorizedExplainer</td>
 *   <td>~1μs</td>
 *   <td>Approximate</td>
 *   <td>Any model (SIMD)</td>
 * </tr>
 * <tr>
 *   <td>PermutationExplainer</td>
 *   <td>~50μs</td>
 *   <td>Approximate</td>
 *   <td>Any model (baseline)</td>
 * </tr>
 * </table>
 * 
 * <h2>Regulatory Compliance</h2>
 * <p>XAI Core supports compliance with:
 * <ul>
 *   <li><strong>FCRA §615(a)</strong> — Adverse action notices with reasons</li>
 *   <li><strong>ECOA §1691(d)</strong> — Credit decision explanations</li>
 *   <li><strong>EU AI Act Art. 13</strong> — High-risk AI transparency</li>
 *   <li><strong>GDPR Art. 22</strong> — Right to explanation</li>
 * </ul>
 * 
 * <h2>Module Structure</h2>
 * <ul>
 *   <li>{@link io.github.Thung0808.xai.api} — Core interfaces</li>
 *   <li>{@link io.github.Thung0808.xai.explainer} — Explanation algorithms</li>
 *   <li>{@link io.github.Thung0808.xai.performance} — High-performance variants</li>
 *   <li>{@link io.github.Thung0808.xai.counterfactual} — What-if analysis</li>
 *   <li>{@link io.github.Thung0808.xai.fairness} — Bias detection</li>
 *   <li>{@link io.github.Thung0808.xai.global} — Model-level explanations</li>
 *   <li>{@link io.github.Thung0808.xai.advanced} — PDP, interactions</li>
 *   <li>{@link io.github.Thung0808.xai.visualization} — Rendering & dashboards</li>
 * </ul>
 * 
 * <h2>References</h2>
 * <ul>
 *   <li>Lundberg & Lee (2017). "A Unified Approach to Interpreting Model Predictions" (SHAP)</li>
 *   <li>Lundberg et al. (2020). "From local explanations to global understanding with explainable AI for trees" (TreeSHAP)</li>
 *   <li>Molnar (2022). "Interpretable Machine Learning"</li>
 * </ul>
 * 
 * @since 0.1.0
 * @version 0.7.0
 */
package io.github.Thung0808.xai;
