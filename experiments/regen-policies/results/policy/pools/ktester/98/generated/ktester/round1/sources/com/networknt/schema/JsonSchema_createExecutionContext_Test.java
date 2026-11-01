package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_createExecutionContext_Test {

    /**
     * Mocks and test fixtures shared by all test methods.
     */
//     @Mock
    ValidationContext // injected into JsonSchema
    validationContext;

//     @Mock
    JsonMetaSchema // required by ValidationContext
    metaSchema;

//     @Mock
    SchemaLocation // passed to JsonSchema.from(...)
    schemaLocation;

//     @Mock
    JsonNodePath // passed to JsonSchema.from(...)
    evaluationPath;

    // real config – we set values on it
    SchemaValidatorsConfig config;

    // a minimal schema node (empty object)
    ObjectNode schemaNode;

    /**
     * Instance of {@link JsonSchema} under test.
     */
    JsonSchema jsonSchema;

    /**
     * Minimal stub for {@link JsonMetaSchema}.
     */
//     private static class StubJsonMetaSchema extends JsonMetaSchema {
        // No additional behaviour required.
    }

    /**
     * Minimal stub for {@link SchemaLocation}.
     */
//     private static class StubSchemaLocation extends SchemaLocation {
        // No additional behaviour required.
    }

    /**
     * Minimal stub for {@link JsonNodePath}.
     */
//     private static class StubJsonNodePath extends JsonNodePath {
        // No additional behaviour required.
    }

    /**
     * Very small stub for {@link ValidationContext}. It forwards the required
     * configuration values and supplies empty maps for resources/anchors.
     */
    private static class StubValidationContext extends ValidationContext {

        private final SchemaValidatorsConfig cfg;

        StubValidationContext(SchemaValidatorsConfig cfg, JsonMetaSchema meta) {
            // The library constructor expects (JsonMetaSchema, JsonSchemaFactory, SchemaValidatorsConfig)
            super(meta, null, cfg);
            this.cfg = cfg;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return cfg;
        }

        @Override
        public ConcurrentMap<String, JsonSchema> getSchemaResources() {
            return new ConcurrentHashMap<>();
        }

        @Override
        public ConcurrentMap<String, JsonSchema> getDynamicAnchors() {
            return new ConcurrentHashMap<>();
        }

//         @Override
//         public String resolveSchemaId(Object id) {
//             return null;
//         }
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
        // real collaborators
//         metaSchema = JsonMetaSchema.getInstance(VersionFlag.V201909);
        // not required for the execution‑context tests
        schemaLocation = null;
        // not required for the execution‑context tests
        evaluationPath = null;
        // stub validation context that returns the config above
        validationContext = new StubValidationContext(config, metaSchema);
        // create the JsonSchema instance using the package‑private factory
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
    }

    /**
     * Verify that the ExecutionConfig values are copied from the ValidationContext.
     */
    @Test
    void createExecutionContext_copiesConfigValues() {
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        ExecutionConfig execConfig = executionContext.getExecutionConfig();
        assertNotNull(execConfig, "ExecutionConfig should not be null");
        assertEquals(config.getLocale(), execConfig.getLocale(), "Locale should be copied from the config");
        // ExecutionConfig uses the getter name {@code getFormatAssertionsEnabled}
        assertEquals(config.getFormatAssertionsEnabled(), execConfig.getFormatAssertionsEnabled(), "formatAssertionsEnabled should be copied from the config");
        assertEquals(config.isFailFast(), execConfig.isFailFast(), "failFast flag should be copied from the config");
    }

    /**
     * Verify that a configured ExecutionContextCustomizer is invoked.
     */
    @Test
    void createExecutionContext_invokesCustomizer_whenPresent() {
        final boolean[] called = { false };
        ExecutionContextCustomizer customizer = (execCtx, vCtx) -> called[0] = true;
        config.setExecutionContextCustomizer(customizer);
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        assertTrue(called[0], "Customizer should have been invoked exactly once");
        // suppress unused‑variable warning
        assertNotNull(executionContext);
    }

    /**
     * Customizer mutates the ExecutionConfig (toggles failFast).
     */
    @Test
    void createExecutionContext_customizerCanMutateConfig() {
        ExecutionContextCustomizer customizer = (execCtx, vCtx) -> {
            ExecutionConfig ec = execCtx.getExecutionConfig();
            ec.setFailFast(!ec.isFailFast());
        };
        config.setExecutionContextCustomizer(customizer);
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        // original config value was true
        boolean expected = !config.isFailFast();
        assertEquals(expected, executionContext.getExecutionConfig().isFailFast(), "Customizer should have toggled the failFast flag");
    }

    /**
     * If the customizer throws, the exception propagates out of createExecutionContext().
     */
    @Test
    void createExecutionContext_customizerThrows_exceptionPropagates() {
        ExecutionContextCustomizer customizer = (execCtx, vCtx) -> {
            throw new RuntimeException("boom");
        };
        config.setExecutionContextCustomizer(customizer);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.createExecutionContext(), "Exception from customizer should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Null ValidationContext inside JsonSchema leads to NPE.
     */
    @Test
    void createExecutionContext_nullValidationContext_throwsNPE() throws Exception {
        Field field = BaseJsonValidator.class.getDeclaredField("validationContext");
        field.setAccessible(true);
        field.set(jsonSchema, null);
        assertThrows(NullPointerException.class, () -> jsonSchema.createExecutionContext(), "When validationContext is null, createExecutionContext should throw NPE");
    }

    /**
     * Null SchemaValidatorsConfig returned from ValidationContext leads to NPE.
     */
    @Test
    void createExecutionContext_nullConfig_throwsNPE() {
        validationContext = new StubValidationContext(null, metaSchema);
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        assertThrows(NullPointerException.class, () -> jsonSchema.createExecutionContext(), "When validationContext.getConfig() returns null, createExecutionContext should throw NPE");
    }

    /**
     * Verify that a non‑default locale is correctly transferred to the ExecutionContext.
     */
    @Test
    void createExecutionContext_localePropagation() {
        config.setLocale(Locale.FRENCH);
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        assertEquals(Locale.FRENCH, executionContext.getExecutionConfig().getLocale(), "Locale set in SchemaValidatorsConfig must be propagated to ExecutionContext");
    }
}