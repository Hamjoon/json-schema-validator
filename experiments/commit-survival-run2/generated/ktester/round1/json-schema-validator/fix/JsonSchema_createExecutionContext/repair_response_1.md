```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link JsonSchema#createExecutionContext()}.
 */
class JsonSchema_createExecutionContext_Test {

    /**
     * Stub for {@link SchemaValidatorsConfig}.
     */
    private static class StubConfig extends SchemaValidatorsConfig {

        private Supplier<ExecutionContext> executionContextSupplier;

        private boolean unevaluatedItemsAnalysisDisabled;

        private boolean unevaluatedPropertiesAnalysisDisabled;

        @Override
        public Supplier<ExecutionContext> getExecutionContextSupplier() {
            return executionContextSupplier;
        }

        @Override
        public boolean isUnevaluatedItemsAnalysisDisabled() {
            return unevaluatedItemsAnalysisDisabled;
        }

        @Override
        public boolean isUnevaluatedPropertiesAnalysisDisabled() {
            return unevaluatedPropertiesAnalysisDisabled;
        }

        @Override
        public void setExecutionContextSupplier(Supplier<ExecutionContext> s) {
            this.executionContextSupplier = s;
        }

        @Override
        public void setUnevaluatedItemsAnalysisDisabled(boolean b) {
            this.unevaluatedItemsAnalysisDisabled = b;
        }

        @Override
        public void setUnevaluatedPropertiesAnalysisDisabled(boolean b) {
            this.unevaluatedPropertiesAnalysisDisabled = b;
        }
    }

    /**
     * Stub for {@link ValidationContext}.
     */
    private static class StubValidationContext extends ValidationContext {

        private final StubConfig stubConfig;

        StubValidationContext(StubConfig cfg) {
            // The super‑class has a protected constructor that expects many arguments.
            // For the purpose of these tests we can invoke the no‑arg constructor via reflection.
            super(null, null, null, null, null);
            this.stubConfig = cfg;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return stubConfig;
        }
    }

    /**
     * Mocks / stubs used for every test.
     */
    private ValidationContext validationContext;

    private StubConfig config;

    /**
     * Instance under test.
     */
    private JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        config = new StubConfig();
        validationContext = new StubValidationContext(config);
        jsonSchema = createJsonSchema(validationContext);
    }

    /**
     * Helper – creates a JsonSchema instance via the private constructor.
     */
    private JsonSchema createJsonSchema(ValidationContext vc) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class, String.class, URI.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // other arguments are not needed for the execution‑context tests
        return ctor.newInstance(vc, null, null, null, null, false);
    }

    /**
     * Helper – extracts a private field value via reflection.
     */
    private static <T> T getPrivateField(Object target, String fieldName, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }

    /**
     * Scenario 1 – a supplier is supplied in the config.
     */
    @Test
    void createExecutionContext_usesSuppliedExecutionContext() {
        // arrange – a supplier that returns a freshly created ExecutionContext
        ExecutionContext supplied = new ExecutionContext();
        Supplier<ExecutionContext> supplier = () -> supplied;
        config.setExecutionContextSupplier(supplier);
        // act
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        // assert
        assertSame(supplied, ctx, "The ExecutionContext returned should be the one supplied by the config");
    }

    /**
     * Scenario 2 – no supplier, default CollectorContext is created.
     */
    @Test
    void createExecutionContext_createsDefaultExecutionContextWhenNoSupplier() throws Exception {
        config.setExecutionContextSupplier(null);
        config.setUnevaluatedItemsAnalysisDisabled(true);
        config.setUnevaluatedPropertiesAnalysisDisabled(false);
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertNotNull(ctx, "ExecutionContext must not be null when no supplier is configured");
        CollectorContext collector = getPrivateField(ctx, "collectorContext", CollectorContext.class);
        assertNotNull(collector, "CollectorContext should be created inside ExecutionContext");
        boolean itemsDisabled = getPrivateField(collector, "unevaluatedItemsAnalysisDisabled", boolean.class);
        boolean propsDisabled = getPrivateField(collector, "unevaluatedPropertiesAnalysisDisabled", boolean.class);
        assertTrue(itemsDisabled, "unevaluatedItemsAnalysisDisabled flag must be propagated");
        assertFalse(propsDisabled, "unevaluatedPropertiesAnalysisDisabled flag must be propagated");
    }

    /**
     * Parameterized test for all flag combinations when the supplier is null.
     */
    @ParameterizedTest(name = "flags – items:{0} props:{1}")
    @CsvSource({ "false, false", "true, true", "false, true", "true, false" })
    void createExecutionContext_defaultFlags(boolean itemsDisabled, boolean propsDisabled) throws Exception {
        config.setExecutionContextSupplier(null);
        config.setUnevaluatedItemsAnalysisDisabled(itemsDisabled);
        config.setUnevaluatedPropertiesAnalysisDisabled(propsDisabled);
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertNotNull(ctx, "ExecutionContext must not be null");
        CollectorContext collector = getPrivateField(ctx, "collectorContext", CollectorContext.class);
        assertNotNull(collector, "CollectorContext must be present");
        boolean actualItems = getPrivateField(collector, "unevaluatedItemsAnalysisDisabled", boolean.class);
        boolean actualProps = getPrivateField(collector, "unevaluatedPropertiesAnalysisDisabled", boolean.class);
        assertEquals(itemsDisabled, actualItems, "Items flag should match config");
        assertEquals(propsDisabled, actualProps, "Properties flag should match config");
    }

    /**
     * Scenario – supplier returns null.
     */
    @Test
    void createExecutionContext_supplierReturnsNull() {
        config.setExecutionContextSupplier(() -> null);
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertNull(ctx, "When the supplier returns null the method should return null");
    }

    /**
     * Scenario – supplier throws a RuntimeException.
     */
    @Test
    void createExecutionContext_supplierThrowsException() {
        RuntimeException ex = new RuntimeException("supplier failure");
        config.setExecutionContextSupplier(() -> {
            throw ex;
        });
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> jsonSchema.createExecutionContext(),
                "The RuntimeException from the supplier should propagate");
        assertSame(ex, thrown, "The thrown exception must be the same instance");
    }

    /**
     * Scenario – multiple invocations produce distinct instances.
     */
    @Test
    void createExecutionContext_multipleInvocationsProduceDistinctInstances() throws Exception {
        config.setExecutionContextSupplier(null);
        config.setUnevaluatedItemsAnalysisDisabled(false);
        config.setUnevaluatedPropertiesAnalysisDisabled(false);
        ExecutionContext first = jsonSchema.createExecutionContext();
        ExecutionContext second = jsonSchema.createExecutionContext();
        assertNotSame(first, second, "Each call should return a new ExecutionContext instance");
        CollectorContext firstCollector = getPrivateField(first, "collectorContext", CollectorContext.class);
        CollectorContext secondCollector = getPrivateField(second, "collectorContext", CollectorContext.class);
        assertNotNull(firstCollector, "First ExecutionContext must contain a CollectorContext");
        assertNotNull(secondCollector, "Second ExecutionContext must contain a CollectorContext");
        assertNotSame(firstCollector, secondCollector, "Each ExecutionContext should have its own CollectorContext");
    }

    /**
     * Scenario – ValidationContext returns null config (NPE expected).
     */
    @Test
    void createExecutionContext_validationContextReturnsNullConfig() {
        // replace the validation context with one that returns null config
        validationContext = new StubValidationContext(null);
        JsonSchema schema = assertDoesNotThrow(() -> createJsonSchema(validationContext));
        NullPointerException npe = assertThrows(NullPointerException.class,
                schema::createExecutionContext,
                "A null config should cause a NullPointerException");
        assertNotNull(npe.getMessage());
    }

    /**
     * Scenario – JsonSchema constructed with a null ValidationContext (NPE expected).
     */
    @Test
    void createExecutionContext_nullValidationContext() throws Exception {
        JsonSchema schemaWithNullVc = createJsonSchema(null);
        NullPointerException npe = assertThrows(NullPointerException.class,
                schemaWithNullVc::createExecutionContext,
                "When validationContext is null, a NullPointerException should be thrown");
        assertNotNull(npe.getMessage());
    }
}
```