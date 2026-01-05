package io.github.Thung0808.xai.adapter;

import io.github.Thung0808.xai.api.PredictiveModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ModelAdapterRegistry SPI pattern.
 */
class ModelAdapterRegistryTest {
    
    @BeforeEach
    void setUp() {
        // Reset registry before each test
        ModelAdapterRegistry.reset();
    }
    
    @Test
    void testRegistryInitialization() {
        // Should load Smile adapter via ServiceLoader
        assertTrue(ModelAdapterRegistry.isAvailable("smile"),
            "Smile adapter should be available when smile-core is on classpath");
    }
    
    @Test
    void testGetAvailableFrameworks() {
        String frameworks = ModelAdapterRegistry.getAvailableFrameworks();
        assertNotNull(frameworks);
        assertFalse(frameworks.isEmpty());
        assertTrue(frameworks.contains("smile"),
            "Available frameworks should include 'smile'");
    }
    
    @Test
    void testGetAvailableAdapters() {
        var adapters = ModelAdapterRegistry.getAvailableAdapters();
        assertNotNull(adapters);
        assertFalse(adapters.isEmpty());
        
        assertTrue(adapters.stream()
            .anyMatch(a -> a.getFramework().equals("smile")),
            "Should have Smile adapter");
    }
    
    @Test
    void testAdapterInfo() {
        var adapters = ModelAdapterRegistry.getAvailableAdapters();
        var smileInfo = adapters.stream()
            .filter(a -> a.getFramework().equals("smile"))
            .findFirst()
            .orElseThrow();
        
        assertEquals("smile", smileInfo.getFramework());
        assertTrue(smileInfo.getDescription().contains("Smile"));
        assertTrue(smileInfo.isAvailable());
        assertEquals(100, smileInfo.getPriority());
    }
    
    @Test
    void testRegisterCustomAdapter() {
        MockAdapter mockAdapter = new MockAdapter();
        ModelAdapterRegistry.register(mockAdapter);
        
        assertTrue(ModelAdapterRegistry.isAvailable("mock"));
        assertTrue(ModelAdapterRegistry.getAvailableFrameworks().contains("mock"));
    }
    
    @Test
    void testUnregisterAdapter() {
        MockAdapter mockAdapter = new MockAdapter();
        ModelAdapterRegistry.register(mockAdapter);
        assertTrue(ModelAdapterRegistry.isAvailable("mock"));
        
        ModelAdapterRegistry.unregister("mock");
        assertFalse(ModelAdapterRegistry.isAvailable("mock"));
    }
    
    @Test
    void testAdaptWithCustomAdapter() {
        MockAdapter mockAdapter = new MockAdapter();
        ModelAdapterRegistry.register(mockAdapter);
        
        Object mockModel = new Object();
        PredictiveModel adapted = ModelAdapterRegistry.adapt(mockModel);
        
        assertNotNull(adapted);
        assertEquals(0.5, adapted.predict(new double[]{1.0, 2.0}));
    }
    
    @Test
    void testAdaptWithSpecificFramework() {
        MockAdapter mockAdapter = new MockAdapter();
        ModelAdapterRegistry.register(mockAdapter);
        
        Object mockModel = new Object();
        PredictiveModel adapted = ModelAdapterRegistry.adapt(mockModel, "mock");
        
        assertNotNull(adapted);
    }
    
    @Test
    void testAdaptNullModelThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ModelAdapterRegistry.adapt(null);
        });
    }
    
    @Test
    void testAdaptUnknownFrameworkThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            ModelAdapterRegistry.adapt(new Object(), "nonexistent");
        });
    }
    
    @Test
    void testAdaptNoApplicableAdapterThrows() {
        // Create a unique object that no adapter can handle
        class UnknownModel {}
        
        assertThrows(IllegalArgumentException.class, () -> {
            ModelAdapterRegistry.adapt(new UnknownModel());
        });
    }
    
    /**
     * Mock adapter for testing.
     */
    private static class MockAdapter implements ModelAdapter {
        
        @Override
        public String getFramework() {
            return "mock";
        }
        
        @Override
        public boolean canAdapt(Object model) {
            return true; // Accept anything
        }
        
        @Override
        public PredictiveModel adapt(Object model, Object... config) {
            return input -> 0.5; // Return constant prediction
        }
        
        @Override
        public String getDescription() {
            return "Mock adapter for testing";
        }
    }
}
