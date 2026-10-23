```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchema_createExecutionContext_Test {

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------
    private ValidationContext validationContext;
    private SchemaValidatorsConfig config;
    private JsonSchema jsonSchema;

    /**
     * Simple stub for {@link SchemaValidatorsConfig}.
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
    }

    /**
     * Stub for {@link ValidationContext}. It only overrides the methods that are
     * consulted by {@link JsonSchema#createExecutionContext()}.
     */
    private static class StubValidationContext extends ValidationContext {

        private final StubConfig stubConfig;

        StubValidationContext(StubConfig cfg) {
            // The real ValidationContext constructor expects many arguments.
            // For the purpose of these tests we can invoke the protected
            // constructor with nulls – the overridden methods below will supply
            // the needed behaviour.
            super(null, null, null, null, null);
            this.stubConfig = cfg;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return stubConfig;
        }

        @Override
        public MetaSchema getMetaSchema() {
            // The JsonSchema constructor needs a non‑null meta‑schema.
            // Any built‑in draft works for the tests.
            return MetaSchema.DRAFTV7;
        }
    }

    // -----------------------------------------------------------------------
    // Test lifecycle
    // -----------------------------------------------------------------------
    @BeforeEach
    void setupBeforeEach() throws Exception {
        config = new StubConfig();

        // a normal ValidationContext (with a non‑null config) is required for
        // the majority of the tests.
        validationContext = new StubValidationContext(config);

        jsonSchema = createJsonSchema(validationContext);
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------
    /**
     * Creates a {@link JsonSchema} instance via the private constructor.
     */
    private JsonSchema createJsonSchema(ValidationContext vc) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class, String.class, URI.class, JsonNode.class,
                JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // The remaining parameters are irrelevant for execution‑context tests.
        return ctor.newInstance(vc, null, null, null, null, false);
    }

    /**
     * Extracts a private field value via reflection.
     */
    private static <T> T getPrivateField(Object target, String fieldName, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------
    /**
     * Scenario 1 – a supplier is supplied in the config.
     */
    @Test
    void createExecutionContext_usesSuppliedExecutionContext() {
        ExecutionContext supplied = new ExecutionContext();
        Supplier<ExecutionContext> supplier = () -> supplied;
        config.setExecutionContextSupplier(supplier);

        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertSame(supplied, ctx,
                "The ExecutionContext returned should be the one supplied by the config");
    }

    /**
     * Scenario – supplier returns {@code null}.
     */
    @Test
    void createExecutionContext_supplierReturnsNull() {
        config.setExecutionContextSupplier(() -> null);
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertNull(ctx, "When the supplier returns null the method should return null");
    }

    /**
     * Scenario – supplier throws a {@link RuntimeException}.
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
     * Scenario – {@link ValidationContext} returns {@code null} config (NPE expected).
     */
    @Test
    void createExecutionContext_validationContextReturnsNullConfig() {
        // ValidationContext that returns a null config.
        validationContext = new StubValidationContext(null);
        JsonSchema schema = assertDoesNotThrow(() -> createJsonSchema(validationContext));

        NullPointerException npe = assertThrows(NullPointerException.class,
                schema::createExecutionContext,
                "A null config should cause a NullPointerException");
        assertNotNull(npe.getMessage());
    }

    /**
     * Scenario – {@link JsonSchema} constructed with a {@code null}
     * {@link ValidationContext} (NPE expected when {@code createExecutionContext}
     * is invoked).
     */
    @Test
    void createExecutionContext_nullValidationContext() throws Exception {
        // Build a JsonSchema with a regular (non‑null) ValidationContext first.
        JsonSchema schema = createJsonSchema(validationContext);

        // Then force the private field to {@code null} via reflection.
        Field vcField = JsonSchema.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        vcField.set(schema, null);

        NullPointerException npe = assertThrows(NullPointerException.class,
                schema::createExecutionContext,
                "When validationContext is null, a NullPointerException should be thrown");
        assertNotNull(npe.getMessage());
    }
}
```