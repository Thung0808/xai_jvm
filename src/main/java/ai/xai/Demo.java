package ai.xai;

import ai.xai.adapter.SmileAdapter;
import ai.xai.core.Explanation;
import ai.xai.core.Predictable;
import ai.xai.explainer.PermutationExplainer;
import smile.classification.LogisticRegression;

/**
 * Demo application showing how to use the XAI library.
 */
public class Demo {
    public static void main(String[] args) {
        System.out.println("=== XAI-JVM Demo ===\n");

        // Create synthetic training data
        // Features: [feature_0, feature_1]
        // Labels: 0 or 1
        double[][] X = {
            {1.0, 2.0},
            {2.0, 3.0},
            {3.0, 4.0},
            {4.0, 5.0},
            {5.0, 6.0},
            {1.5, 2.5},
            {2.5, 3.5},
            {3.5, 4.5}
        };
        
        int[] y = {0, 0, 1, 1, 1, 0, 1, 1};

        System.out.println("Training data: " + X.length + " samples, " + X[0].length + " features");
        System.out.println("Training Logistic Regression model...\n");

        // Train a Smile LogisticRegression model
        LogisticRegression lr = LogisticRegression.fit(X, y);

        // Wrap the model with our adapter
        Predictable model = new SmileAdapter(lr);

        // Test predictions
        System.out.println("Sample predictions:");
        for (int i = 0; i < 3; i++) {
            double pred = model.predict(X[i]);
            System.out.printf("  Sample %d: features=%s, predicted=%d, actual=%d%n",
                i, formatArray(X[i]), (int)pred, y[i]);
        }
        System.out.println();

        // Create explainer and generate explanation
        System.out.println("Computing feature importance using Permutation method...\n");
        
        // Convert int[] to double[] for explainer
        double[] yDouble = new double[y.length];
        for (int i = 0; i < y.length; i++) {
            yDouble[i] = (double) y[i];
        }
        
        PermutationExplainer explainer = new PermutationExplainer(model, X, yDouble);
        Explanation explanation = explainer.explain();

        // Display results
        System.out.println("Feature Importance Scores:");
        explanation.getFeatureImportance().forEach((feature, importance) -> {
            System.out.printf("  %s: %.4f%n", feature, importance);
        });
        
        System.out.println("\n=== Demo Complete ===");
        System.out.println("Interpretation: Higher scores indicate more important features.");
        System.out.println("A positive score means removing that feature hurts accuracy.");
    }

    private static String formatArray(double[] arr) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(String.format("%.1f", arr[i]));
            if (i < arr.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
