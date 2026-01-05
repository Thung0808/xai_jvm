package io.github.Thung0808.xai.spring.boot.autoconfigure;

import io.github.Thung0808.xai.api.Explainer;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that checks if no Explainer bean is already registered.
 * If true, auto-configuration will create a default Explainer bean.
 */
public class ExplainerMissingCondition implements Condition {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        try {
            context.getBeanFactory().getBean(Explainer.class);
            return false;  // Explainer exists, don't create default
        } catch (Exception e) {
            return true;   // No Explainer found, create default
        }
    }
}
