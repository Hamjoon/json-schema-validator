package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

@ExtendWith(MockitoExtension.class)
class JsonSchema_createExecutionContext_Test {

    /**
     * Mocks and test fixtures shared by all test methods.
     */
    @Mock
    ValidationContext // injected into JsonSchema
    validationContext;

    @Mock
    JsonMetaSchema // required by ValidationContext
    metaSchema;

    @Mock
    SchemaLocation // passed to JsonSchema.from(...)
    schemaLocation;

    @Mock
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
     * Simple stub for {@link ValidationContext}.
     */
    private static class StubValidationContext implements ValidationContext {

        private SchemaValidatorsConfig config;

        private JsonMetaSchema metaSchema;

        StubValidationContext(SchemaValidatorsConfig config, JsonMetaSchema metaSchema) {
            this.config = config;
            this.metaSchema = metaSchema;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return config;
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            return metaSchema;
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
            // not needed for the tests
            return null;
        }

        // ------------------------------------------------------------------
        // The remaining methods of ValidationContext are not used by the
        // tests; they can be left with default (null) implementations.
        // ------------------------------------------------------------------
        @Override
        public String getCurrentUri() {
            return null;
        }

        @Override
        public String getCurrentUriFragment() {
            return null;
        }

        @Override
        public String getCurrentBaseUri() {
            return null;
        }

        @Override
        public String getCurrentBaseUriFragment() {
            return null;
        }

        @Override
        public String getCurrentDocumentUri() {
            return null;
        }

        @Override
        public String getCurrentDocumentUriFragment() {
            return null;
        }

        @Override
        public String getCurrentDocumentBaseUri() {
            return null;
        }

        @Override
        public String getCurrentDocumentBaseUriFragment() {
            return null;
        }

        @Override
        public String getCurrentPointer() {
            return null;
        }

        @Override
        public String getCurrentSchemaPath() {
            return null;
        }

        @Override
        public String getCurrentSchema() {
            return null;
        }

        @Override
        public String getCurrentSchemaDocument() {
            return null;
        }

        @Override
        public String getCurrentSchemaDocumentFragment() {
            return null;
        }

        @Override
        public String getCurrentSchemaDocumentBaseUri() {
            return null;
        }

        @Override
        public String getCurrentSchemaDocumentBaseUriFragment() {
            return null;
        }

        @Override
        public String getCurrentSchemaDocumentPath() {
            return null;
        }

        @Override
        public String getCurrentSchemaDocumentFragmentPath() {
            return null;
        }

        @Override
        public String getCurrentSchemaId() {
            return null;
        }

        @Override
        public String getCurrentSchemaUri() {
            return null;
        }

        @Override
        public String getCurrentSchemaUriFragment() {
            return null;
        }

        @Override
        public String getCurrentSchemaBaseUri() {
            return null;
        }

        @Override
        public String getCurrentSchemaBaseUriFragment() {
            return null;
        }

        @Override
        public String getCurrentSchemaPathFragment() {
            return null;
        }

        @Override
        public String getCurrentSchemaPathBaseUri() {
            return null;
        }

        @Override
        public String getCurrentSchemaPathBaseUriFragment() {
            return null;
        }

        @Override
        public String getCurrentSchemaPathFragmentBaseUri() {
            return null;
        }

        @Override
        public String getCurrentSchemaPathFragmentBaseUriFragment() {
            return null;
        }

        @Override
        public String getCurrentSchemaPathFragmentBaseUriFragmentPath() {
            return null;
        }

        @Override
        public String getCurrentSchemaPathFragmentBaseUriFragmentPathFragment() {
            return null;
        }

        @Override
        public void setCurrentUri(String uri) {
        }

        @Override
        public void setCurrentUriFragment(String fragment) {
        }

        @Override
        public void setCurrentBaseUri(String baseUri) {
        }

        @Override
        public void setCurrentBaseUriFragment(String fragment) {
        }

        @Override
        public void setCurrentDocumentUri(String uri) {
        }

        @Override
        public void setCurrentDocumentUriFragment(String fragment) {
        }

        @Override
        public void setCurrentDocumentBaseUri(String baseUri) {
        }

        @Override
        public void setCurrentDocumentBaseUriFragment(String fragment) {
        }

        @Override
        public void setCurrentPointer(String pointer) {
        }

        @Override
        public void setCurrentSchemaPath(String path) {
        }

        @Override
        public void setCurrentSchema(String schema) {
        }

        @Override
        public void setCurrentSchemaDocument(String document) {
        }

        @Override
        public void setCurrentSchemaDocumentFragment(String fragment) {
        }

        @Override
        public void setCurrentSchemaDocumentBaseUri(String baseUri) {
        }

        @Override
        public void setCurrentSchemaDocumentBaseUriFragment(String fragment) {
        }

        @Override
        public void setCurrentSchemaDocumentPath(String path) {
        }

        @Override
        public void setCurrentSchemaDocumentFragmentPath(String path) {
        }

        @Override
        public void setCurrentSchemaId(String id) {
        }

        @Override
        public void setCurrentSchemaUri(String uri) {
        }

        @Override
        public void setCurrentSchemaUriFragment(String fragment) {
        }

        @Override
        public void setCurrentSchemaBaseUri(String baseUri) {
        }

        @Override
        public void setCurrentSchemaBaseUriFragment(String fragment) {
        }

        @Override
        public void setCurrentSchemaPathFragment(String fragment) {
        }

        @Override
        public void setCurrentSchemaPathBaseUri(String baseUri) {
        }

        @Override
        public void setCurrentSchemaPathBaseUriFragment(String fragment) {
        }

        @Override
        public void setCurrentSchemaPathFragmentBaseUri(String baseUri) {
        }

        @Override
        public void setCurrentSchemaPathFragmentBaseUriFragment(String fragment) {
        }

        @Override
        public void setCurrentSchemaPathFragmentBaseUriFragmentPath(String path) {
        }

        @Override
        public void setCurrentSchemaPathFragmentBaseUriFragmentPathFragment(String fragment) {
        }
    }

    /**
     * Minimal stub for {@link JsonMetaSchema}.
     */
    private static class StubJsonMetaSchema implements JsonMetaSchema {
        // No behavior needed for the current tests.
    }

    /**
     * Minimal stub for {@link SchemaLocation}.
     */
    private static class StubSchemaLocation implements SchemaLocation {
        // No behavior needed for the current tests.
    }

    /**
     * Minimal stub for {@link JsonNodePath}.
     */
    private static class StubJsonNodePath implements JsonNodePath {
        // No behavior needed for the current tests.
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
        // stub collaborators
        metaSchema = new StubJsonMetaSchema();
        schemaLocation = new StubSchemaLocation();
        evaluationPath = new StubJsonNodePath();
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
        assertEquals(config.getFormatAssertionsEnabled(), execConfig.isFormatAssertionsEnabled(), "formatAssertionsEnabled should be copied from the config");
        assertEquals(config.isFailFast(), execConfig.isFailFast(), "failFast flag should be copied from the config");
    }

    /**
     * Verify that a configured ExecutionContextCustomizer is invoked.
     */
    @Test
    void createExecutionContext_invokesCustomizer_whenPresent() {
        // a customizer that records whether it has been called
        final boolean[] called = { false };
        ExecutionContextCustomizer customizer = (execCtx, vCtx) -> called[0] = true;
        config.setExecutionContextCustomizer(customizer);
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        assertTrue(called[0], "Customizer should have been invoked exactly once");
    }

    /**
     * Customizer mutates the ExecutionConfig (toggles failFast).
     */
    @Test
    void createExecutionContext_customizerCanMutateConfig() {
        // customizer that flips the failFast flag
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
        // set the protected field BaseJsonValidator.validationContext to null via reflection
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
        // make the stub return null for getConfig()
        validationContext = new StubValidationContext(null, metaSchema);
        // recreate the JsonSchema instance with the new context
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
