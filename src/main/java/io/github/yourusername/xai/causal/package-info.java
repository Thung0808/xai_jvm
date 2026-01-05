/**
 * Causal inference for XAI using Do-calculus.
 * 
 * <p>This package implements interventional causal analysis to distinguish
 * true causation from mere correlation in model explanations.
 * 
 * <h2>Key Concepts</h2>
 * <ul>
 *   <li><strong>Observational:</strong> P(Y | X = x) - What we observe (correlation)</li>
 *   <li><strong>Interventional:</strong> P(Y | do(X = x)) - What happens if we intervene (causation)</li>
 * </ul>
 * 
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Policy decisions: "If we change credit limit, what's the true impact?"</li>
 *   <li>Medical interventions: "If we prescribe drug A, what's the causal effect?"</li>
 *   <li>Marketing: "If we send campaign, what's the true lift?"</li>
 * </ul>
 * 
 * <h2>Example</h2>
 * <pre>{@code
 * // Create causal explainer
 * CausalExplainer causal = new CausalExplainer(model, trainingData, labels);
 * 
 * // Answer: "What if I intervene to increase Income by 20%?"
 * CausalEffect effect = causal.interventionalEffect(instance, "income", 1.20);
 * 
 * System.out.println("True causal effect: " + effect.ate());
 * System.out.println("Confounding bias: " + effect.confoundingBias());
 * }</pre>
 * 
 * @since 1.1.0-alpha
 * @see <a href="http://bayes.cs.ucla.edu/BOOK-2K/">Causality (Pearl, 2000)</a>
 */
package io.github.Thung0808.xai.causal;
