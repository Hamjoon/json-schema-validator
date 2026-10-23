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
 * Unit‑tests that focus on the {@link JsonSchema#createExecutionContext()} method.
 *
 * <p>Only a very small part of the whole JsonSchema implementation is needed for these
 * tests, therefore a number of lightweight stubs are used instead of the full library
 * classes.</p>
 */
class JsonSchema_createExecutionContext_Test {

    /* --------------------------------------------------------------------- */
    /*  Stubs / test doubles                                                */
    /* --------------------------------------------------------------------- */

    /** Simple stub for {@link SchemaValidatorsConfig}. */
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

        // the two “set” flags are not required for the current tests – the
        // default values are sufficient.
    }

    /** Stub for {@link ValidationContext}. It only overrides the methods that are
     *  consulted by {@link JsonSchema#createExecutionContext()}. */
    private static class StubValidationContext extends ValidationContext {
        private final StubConfig stubConfig;

        StubValidationContext(StubConfig cfg) {
            // The real ValidationContext constructor expects a lot of arguments.
            // For the purpose of these tests we can invoke the protected
            // constructor with nulls – the overridden methods below will supply
            // the needed behavior.
            super(null, null, null, null, null);
            this.stubConfig = cfg;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return stubConfig;
        }

        @Override
        public MetaSchema getMetaSchema() {
            // A concrete MetaSchema instance is required by JsonSchema’s
            // constructor but it is not used inside createExecutionContext().
            // Using one of the built‑in drafts avoids the need for a custom
            // implementation.
            return MetaSchema.DRAFTV7;
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Test fields                                                          */
    /* --------------------------------------------------------------------- */

    private ValidationContext validationContext;   // may be null in some tests
    private StubConfig config;
    private JsonSchema jsonSchema;                 // instance under test

    /* --------------------------------------------------------------------- */
    /*  Test life‑cycle                                                    */
    /* --------------------------------------------------------------------- */

    @BeforeEach
    void setupBeforeEach() throws Exception {
        config = new StubConfig();
        // a normal ValidationContext (with a non‑null config) is required for
        // the majority of the tests.
        validationContext = new StubValidationContext(config);
        jsonSchema = createJsonSchema(validationContext);
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Instantiates {@link JsonSchema} via its private constructor. */
    private JsonSchema createJsonSchema(ValidationContext vc) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class
                .getDeclaredConstructor(ValidationContext.class, String.class, URI.class,
                        JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // The remaining parameters are irrelevant for execution‑context tests.
        return ctor.newInstance(vc, null, null, null, null, false);
    }

    /** Reads a private field value via reflection. */
    private static <T> T getPrivateField(Object target, String fieldName, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }

    /* --------------------------------------------------------------------- */
    /*  Test cases                                                          */
    /* --------------------------------------------------------------------- */

    /** Scenario 1 – a supplier is supplied in the config. */
    @Test
    void createExecutionContext_usesSuppliedExecutionContext() {
        ExecutionContext supplied = new ExecutionContext();
        Supplier<ExecutionContext> supplier = () -> supplied;
        config.setExecutionContextSupplier(supplier);

        ExecutionContext ctx = jsonSchema.createExecutionContext();

        assertSame(supplied, ctx, "The ExecutionContext returned should be the one supplied by the config");
    }

    /** Scenario – supplier returns null. */
    @Test
    void createExecutionContext_supplierReturnsNull() {
        config.setExecutionContextSupplier(() -> null);
        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertNull(ctx, "When the supplier returns null the method should return null");
    }

    /** Scenario – supplier throws a RuntimeException. */
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

    /** Scenario – ValidationContext returns null config (NPE expected). */
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

    /** Scenario – JsonSchema constructed with a null ValidationContext (NPE expected). */
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