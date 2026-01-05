/**
 * Natural Language Processing for XAI explanations.
 * 
 * <p>This package provides tools to convert technical feature attributions
 * into human-readable narratives for non-technical stakeholders.
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Multi-language support (English, Vietnamese)</li>
 *   <li>Customizable narrative styles (Technical, Business, Regulatory, Customer)</li>
 *   <li>LLM-ready structured output for GPT-4/Gemini integration</li>
 * </ul>
 * 
 * <h2>Example</h2>
 * <pre>{@code
 * // Generate explanation
 * Explanation explanation = explainer.explain(model, instance);
 * 
 * // Convert to human-readable text
 * NaturalLanguageExplainer nlg = new NaturalLanguageExplainer(Language.ENGLISH);
 * String narrative = nlg.toHumanReadable(explanation);
 * 
 * // Send to managers
 * emailService.send(manager, narrative);
 * }</pre>
 * 
 * @since 1.1.0-alpha
 */
package io.github.Thung0808.xai.nlp;
