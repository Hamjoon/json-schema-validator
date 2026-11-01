```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JsonSchema#createExecutionContext()}.
 *
 * <p>The original test used Mockito for mocks, but the project does not depend on
 * Mockito.  To keep the test self‑contained we replace the mocked objects with
 * very small concrete stubs that extend the real types.  The stubs provide only
 * the behaviour required by the tests.</p>
 */
class JsonSchema_createExecutionContext_Test {

    /* ----------------------------------------------------------------------
     * Test fixtures – real objects (or very small stubs) used by all tests.
     * ---------------------------------------------------------------------- */
    private ValidationContext validationContext;          // injected into JsonSchema
    private JsonMetaSchema metaSchema;                    // required by ValidationContext
    private SchemaLocation schemaLocation;                // passed to JsonSchema.from(...)
    private JsonNodePath evaluationPath;                  // passed to JsonSchema.from(...)

    private SchemaValidatorsConfig config;                // real config – we set values on it
    private ObjectNode schemaNode;                        // a minimal schema node (empty object)

    private JsonSchema jsonSchema;                        // instance under test

    /* ----------------------------------------------------------------------
     * Very small concrete stubs – they extend the library classes (which are
     * abstract or concrete) rather than implement them as interfaces.
     * ---------------------------------------------------------------------- */
    private static class StubValidationContext extends ValidationContext {
        private final SchemaValidatorsConfig cfg;
        private final JsonMetaSchema meta;

        StubValidationContext(SchemaValidatorsConfig cfg, JsonMetaSchema meta) {
            this.cfg = cfg;
            this.meta = meta;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return cfg;
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            return meta;
        }

        @Override
        public ConcurrentMap<String, JsonSchema> getSchemaResources() {
            return new ConcurrentHashMap<>();
        }

        @Override
        public ConcurrentMap<String, JsonSchema> getDynamicAnchors() {
            return new ConcurrentHashMap<>();
        }

        @Override
        public String resolveSchemaId(Object id) {
            return null;
        }
    }

    /** Minimal stub for {@link JsonMetaSchema}. */
    private static class StubJsonMetaSchema extends JsonMetaSchema {
        // No additional behaviour required.
    }

    /** Minimal stub for {@link SchemaLocation}. */
    private static class StubSchemaLocation extends SchemaLocation {
        // No additional behaviour required.
    }

    /** Minimal stub for {@link JsonNodePath}. */
    private static class StubJsonNodePath extends JsonNodePath {
        // No additional behaviour required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // a minimal schema node (empty object)
        schemaNode = new ObjectMapper().createObjectNode();

        // real configuration – we set values on it
        config = new SchemaValidatorsConfig();
        config.setLocale(Locale.GERMAN);
        config.setFormatAssertionsEnabled(true);
        config.setFailFast(true);

        // create stub collaborators
        metaSchema = new StubJsonMetaSchema();
        schemaLocation = new StubSchemaLocation();
        evaluationPath = new StubJsonNodePath();
        validationContext = new StubValidationContext(config, metaSchema);

        // create the JsonSchema instance using the package‑private factory
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                schemaNode, null, false);
    }

    /** Verify that the ExecutionConfig values are copied from the ValidationContext. */
    @Test
    void createExecutionContext_copiesConfigValues() {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        ExecutionConfig execConfig = executionContext.getExecutionConfig();

        assertNotNull(execConfig, "ExecutionConfig should not be null");
        assertEquals(config.getLocale(), execConfig.getLocale(),
                "Locale should be copied from the config");
        // ExecutionConfig uses the getter name {@code getFormatAssertionsEnabled}
        assertEquals(config.getFormatAssertionsEnabled(),
                execConfig.getFormatAssertionsEnabled(),
                "formatAssertionsEnabled should be copied from the config");
        assertEquals(config.isFailFast(),
                execConfig.isFailFast(),
                "failFast flag should be copied from the config");
    }

    /** Verify that a configured ExecutionContextCustomizer is invoked. */
    @Test
    void createExecutionContext_invokesCustomizer_whenPresent() {
        final boolean[] called = { false };
        ExecutionContextCustomizer customizer = (execCtx, vCtx) -> called[0] = true;
        config.setExecutionContextCustomizer(customizer);

        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        assertTrue(called[0], "Customizer should have been invoked exactly once");
    }

    /** Customizer mutates the ExecutionConfig (toggles failFast). */
    @Test
    void createExecutionContext_customizerCanMutateConfig() {
        ExecutionContextCustomizer customizer = (execCtx, vCtx) -> {
            ExecutionConfig ec = execCtx.getExecutionConfig();
            ec.setFailFast(!ec.isFailFast());
        };
        config.setExecutionContextCustomizer(customizer);

        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        boolean expected = !config.isFailFast(); // original config value was true
        assertEquals(expected,
                executionContext.getExecutionConfig().isFailFast(),
                "Customizer should have toggled the failFast flag");
    }

    /** If the customizer throws, the exception propagates out of createExecutionContext(). */
    @Test
    void createExecutionContext_customizerThrows_exceptionPropagates() {
        ExecutionContextCustomizer customizer = (execCtx, vCtx) -> {
            throw new RuntimeException("boom");
        };
        config.setExecutionContextCustomizer(customizer);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.createExecutionContext(),
                "Exception from customizer should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /** Null ValidationContext inside JsonSchema leads to NPE. */
    @Test
    void createExecutionContext_nullValidationContext_throwsNPE() throws Exception {
        Field field = BaseJsonValidator.class.getDeclaredField("validationContext");
        field.setAccessible(true);
        field.set(jsonSchema, null);
        assertThrows(NullPointerException.class,
                () -> jsonSchema.createExecutionContext(),
                "When validationContext is null, createExecutionContext should throw NPE");
    }

    /** Null SchemaValidatorsConfig returned from ValidationContext leads to NPE. */
    @Test
    void createExecutionContext_nullConfig_throwsNPE() {
        validationContext = new StubValidationContext(null, metaSchema);
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                schemaNode, null, false);
        assertThrows(NullPointerException.class,
                () -> jsonSchema.createExecutionContext(),
                "When validationContext.getConfig() returns null, createExecutionContext should throw NPE");
    }

    /** Verify that a non‑default locale is correctly transferred to the ExecutionContext. */
    @Test
    void createExecutionContext_localePropagation() {
        config.setLocale(Locale.FRENCH);
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        assertEquals(Locale.FRENCH,
                executionContext.getExecutionConfig().getLocale(),
                "Locale set in SchemaValidatorsConfig must be propagated to ExecutionContext");
    }
}
```