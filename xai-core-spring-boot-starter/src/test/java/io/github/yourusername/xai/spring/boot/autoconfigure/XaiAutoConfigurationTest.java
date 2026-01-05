package io.github.Thung0808.xai.spring.boot.autoconfigure;

import io.github.Thung0808.xai.api.Explainer;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for XAI Spring Boot auto-configuration.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class XaiAutoConfigurationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private XaiProperties xaiProperties;
    
    @Autowired
    private ExplainerMetrics explainerMetrics;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired(required = false)
    private Explainer<?> defaultExplainer;
    
    @Test
    void testAutoConfigurationLoaded() {
        assertThat(xaiProperties).isNotNull();
        assertThat(xaiProperties.isEnabled()).isTrue();
    }
    
    @Test
    void testExplainerMetricsBean() {
        assertThat(explainerMetrics).isNotNull();
        assertThat(explainerMetrics.getTrustScore()).isNotNull();
    }
    
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/xai/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.explainer").value("available"));
    }
    
    @Test
    void testMetricsEndpoint() throws Exception {
        mockMvc.perform(get("/xai/metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trust_score").isNotEmpty())
            .andExpect(jsonPath("$.robustness_score").isNotEmpty());
    }
    
    @Test
    void testPropertiesLoading() {
        assertThat(xaiProperties.getDefaultExplainer()).isEqualTo("permutation");
        assertThat(xaiProperties.getDefaultSamples()).isGreaterThan(0);
        assertThat(xaiProperties.getMetrics().isEnabled()).isTrue();
    }
}
