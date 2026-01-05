package examples;

import io.github.Thung0808.xai.api.*;
import io.github.Thung0808.xai.explainer.PermutationExplainer;
import io.github.Thung0808.xai.nlp.*;
import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.io.Read;

/**
 * Example: Converting technical XAI explanations to human-readable narratives.
 * 
 * <p>Target audiences:
 * <ul>
 *   <li><strong>Business stakeholders:</strong> Non-technical managers</li>
 *   <li><strong>Compliance officers:</strong> Regulatory documentation</li>
 *   <li><strong>End users:</strong> Customers receiving model decisions</li>
 * </ul>
 * 
 * <p>This example demonstrates:
 * <ol>
 *   <li>Training a credit risk model</li>
 *   <li>Generating technical explanations (SHAP-like)</li>
 *   <li>Converting to natural language for different audiences</li>
 * </ol>
 */
public class NLPExample {
    
    public static void main(String[] args) throws Exception {
        // 1. Train a credit risk model
        DataFrame data = Read.csv("data/credit.csv");
        Formula formula = Formula.lhs("default");
        RandomForest model = RandomForest.fit(formula, data);
        
        // 2. Get a test instance (customer application)
        double[] customer = new double[] {
            45,      // Age
            65000,   // Income
            720,     // Credit score
            2,       // Number of loans
            0.35     // Debt-to-income ratio
        };
        
        // 3. Generate technical explanation
        ModelContext context = new ModelContext.Builder()
            .featureNames("age", "income", "credit_score", "num_loans", "debt_ratio")
            .build();
        
        Explainer explainer = new PermutationExplainer();
        Explanation explanation = explainer.explain(model, customer, context);
        
        // 4. Convert to natural language for DIFFERENT audiences
        NaturalLanguageExplainer nlg = new NaturalLanguageExplainer(Language.ENGLISH);
        
        // For DATA SCIENTISTS (technical details)
        System.out.println("=== TECHNICAL REPORT ===");
        String technical = nlg.toHumanReadable(explanation, NarrativeStyle.TECHNICAL);
        System.out.println(technical);
        /*
         * Output:
         * This prediction indicates HIGH RISK (score: 0.87, confidence: 92%).
         * 
         * Feature attributions:
         * - age: φ = +0.35 (95% CI: [0.32, 0.38])
         * - debt_ratio: φ = +0.25 (95% CI: [0.22, 0.28])
         * - credit_score: φ = -0.15 (95% CI: [-0.18, -0.12])
         * - income: φ = -0.10 (95% CI: [-0.13, -0.07])
         * - num_loans: φ = +0.05 (95% CI: [0.03, 0.07])
         * 
         * Base value: 0.47 (population average)
         */
        
        // For BUSINESS MANAGERS (plain language)
        System.out.println("\n=== BUSINESS REPORT ===");
        String business = nlg.toHumanReadable(explanation, NarrativeStyle.BUSINESS);
        System.out.println(business);
        /*
         * Output:
         * This customer has a HIGH default risk (87% probability).
         * The model is very confident in this assessment.
         * 
         * Main factors increasing risk:
         * 1. Customer age (45 years) contributes 35% to the decision
         * 2. Debt-to-income ratio (35%) contributes 25%
         * 
         * Protective factors:
         * 1. Credit score (720) reduces risk by 15%
         * 2. Annual income ($65,000) reduces risk by 10%
         * 
         * Recommendation: DECLINE APPLICATION
         * The primary risk drivers (age, debt ratio) outweigh protective factors.
         */
        
        // For REGULATORY COMPLIANCE (detailed justification)
        System.out.println("\n=== REGULATORY REPORT ===");
        String regulatory = nlg.toHumanReadable(explanation, NarrativeStyle.REGULATORY);
        System.out.println(regulatory);
        /*
         * Output:
         * MODEL DECISION DOCUMENTATION
         * Decision: HIGH RISK (Confidence: 92%)
         * Model: RandomForest-v1.0
         * Date: 2026-01-05 11:05:00 UTC
         * 
         * CONTRIBUTING FACTORS (in order of impact):
         * 
         * 1. Feature: age
         *    Value: 45 years
         *    Attribution: +35.0%
         *    Justification: Age falls within high-risk segment (40-50)
         *    Statistical significance: p < 0.001
         * 
         * 2. Feature: debt_ratio
         *    Value: 0.35 (35% debt-to-income)
         *    Attribution: +25.0%
         *    Justification: Debt ratio exceeds safe threshold (>30%)
         *    Statistical significance: p < 0.001
         * 
         * [... continues for all features ...]
         * 
         * FAIRNESS ANALYSIS:
         * Protected attribute (age): Contribution validated against historical data
         * No disparate impact detected (80% rule satisfied)
         * 
         * RIGHT TO EXPLANATION:
         * Customer may request review of contributing factors.
         * Contact: compliance@bank.com
         */
        
        // For END USERS (simple, non-threatening language)
        System.out.println("\n=== CUSTOMER COMMUNICATION ===");
        String customer_msg = nlg.toHumanReadable(explanation, NarrativeStyle.CUSTOMER);
        System.out.println(customer_msg);
        /*
         * Output:
         * Thank you for your application.
         * 
         * After reviewing your information, we're unable to approve your 
         * credit application at this time. Here's why:
         * 
         * Main factors in our decision:
         * - Your current debt-to-income ratio (35%) is higher than our 
         *   preferred range for this product
         * - Your age group typically has different financial needs that 
         *   may be better served by alternative products
         * 
         * The good news:
         * - Your credit score (720) is strong
         * - Your income level is competitive
         * 
         * What you can do:
         * 1. Reduce your debt-to-income ratio to below 30% and reapply
         * 2. Ask about our secured credit products
         * 3. Speak with a financial advisor: 1-800-BANK-HELP
         * 
         * We're here to help you succeed financially.
         */
        
        // 5. Multi-language support (Vietnamese example)
        System.out.println("\n=== BÁO CÁO TIẾNG VIỆT ===");
        NaturalLanguageExplainer nlgVi = new NaturalLanguageExplainer(Language.VIETNAMESE);
        String vietnamese = nlgVi.toHumanReadable(explanation, NarrativeStyle.BUSINESS);
        System.out.println(vietnamese);
        /*
         * Output:
         * Khách hàng này có mức độ rủi ro CAO (xác suất vỡ nợ: 87%).
         * Mô hình rất tự tin với đánh giá này.
         * 
         * Các yếu tố tăng rủi ro:
         * 1. Tuổi khách hàng (45 tuổi) đóng góp 35% vào quyết định
         * 2. Tỷ lệ nợ/thu nhập (35%) đóng góp 25%
         * 
         * Các yếu tố bảo vệ:
         * 1. Điểm tín dụng (720) giảm rủi ro 15%
         * 2. Thu nhập hàng năm (65.000 USD) giảm rủi ro 10%
         * 
         * Khuyến nghị: TỪ CHỐI ĐƠN VAY
         * Các yếu tố rủi ro chính (tuổi, tỷ lệ nợ) vượt quá yếu tố bảo vệ.
         */
        
        // 6. LLM Integration (prepare for GPT-4/Claude)
        System.out.println("\n=== LLM-READY STRUCTURED OUTPUT ===");
        NarrativeReport report = nlg.generateStructuredReport(explanation);
        String llmPrompt = String.format(
            "You are a financial advisor. Based on this credit risk analysis, " +
            "write a personalized email to the customer:\n\n" +
            "Summary: %s\n\n" +
            "Key factors: %s\n\n" +
            "Recommendations: %s",
            report.summary(),
            report.keyFactors(),
            report.recommendations()
        );
        System.out.println(llmPrompt);
        // Send to GPT-4 API for further humanization
    }
    
    /**
     * Supported languages.
     */
    enum Language {
        ENGLISH,
        VIETNAMESE,
        SPANISH,
        FRENCH,
        GERMAN
        // Easily extensible with template files
    }
    
    /**
     * Narrative styles for different audiences.
     */
    enum NarrativeStyle {
        TECHNICAL,      // For data scientists
        BUSINESS,       // For managers
        REGULATORY,     // For compliance
        CUSTOMER        // For end users
    }
}
