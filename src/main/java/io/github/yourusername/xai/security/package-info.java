/**
 * Security features for XAI explanations.
 * 
 * <p>Defends against adversarial attacks on explanations:
 * <ul>
 *   <li><strong>Explanation Manipulation:</strong> Attacker modifies input, prediction stays same but explanation flips</li>
 *   <li><strong>Cross-validation:</strong> Compare multiple explanation methods</li>
 *   <li><strong>Consistency checks:</strong> Prediction, attribution ranking, trust score</li>
 * </ul>
 * 
 * <h2>Attack Example</h2>
 * <pre>
 * Original input: [age=45, income=80000, credit=720]
 * Explainer: credit_score is POSITIVE (φ=+0.35)
 * 
 * Adversary modifies: [age=45, income=80000, credit=721]
 * Model prediction: SAME (still 0.87)
 * Explainer: credit_score is NEGATIVE (φ=-0.15)  ← MANIPULATION!
 * 
 * The model is robust, but the explanation is not.
 * </pre>
 * 
 * <h2>Defense Strategy</h2>
 * <ol>
 *   <li>Get explanation from primary explainer</li>
 *   <li>Get explanations from 2-3 validator explainers (e.g., TreeExplainer, LinearExplainer)</li>
 *   <li>Compare:
 *     <ul>
 *       <li>Prediction agreement (< 30% deviation)</li>
 *       <li>Attribution ranking Spearman correlation (> 0.70)</li>
 *       <li>Trust score agreement (< 30% deviation)</li>
 *     </ul>
 *   </li>
 *   <li>If inconsistency detected: Flag as "Potential Manipulation Detected"</li>
 * </ol>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create sanitizer with cross-validators
 * ExplanationSanitizer sanitizer = new ExplanationSanitizer(0.30);  // 30% threshold
 * sanitizer.addValidator(new TreeExplainer());
 * sanitizer.addValidator(new LinearExplainer());
 * 
 * // Validate explanation
 * Explanation primaryExpl = primaryExplainer.explain(instance, context);
 * SanitizationResult result = sanitizer.validate(primaryExpl, instance, context);
 * 
 * if (result.isSuspicious()) {
 *     System.err.println("⚠️ ALERT: " + result.getMessage());
 *     // Take defensive action:
 *     // - Log the incident
 *     // - Retry with different model
 *     // - Alert security team
 *     // - Require human review
 * }
 * }</pre>
 * 
 * @since 1.1.0-alpha
 */
package io.github.Thung0808.xai.security;
