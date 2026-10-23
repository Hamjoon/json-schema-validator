package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Set;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atRoot_Test {

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    @Mock
    private ValidationContext mockValidationContext;

    @Mock
    private SchemaValidatorsConfig mockConfig;

    @Mock
    private PathType mockPathType;

    @Mock
    private JsonNode mockSchemaNode;

    /**
     * Simple {@link PathType} implementation that can return a configurable
     * root value or throw an exception.
     */
    private static class SimplePathType extends PathType {

        private final String root;

        private final boolean throwOnGetRoot;

        SimplePathType(String root) {
            this(root, false);
        }

        private SimplePathType(String root, boolean throwOnGetRoot) {
            this.root = root;
            this.throwOnGetRoot = throwOnGetRoot;
        }

        /**
         * Creates an instance that throws an {@link IllegalStateException} when {@code getRoot()} is called.
         */
        static SimplePathType throwing() {
            return new SimplePathType(null, true);
        }

        @Override
        public String getRoot() {
            if (throwOnGetRoot) {
                throw new IllegalStateException("boom");
            }
            return root;
        }

        /**
         * The separator is not used by the tests – return a harmless default.
         */
        @Override
        public String getSeparator() {
            return ".";
        }
    }

    /**
     * Simple stub for {@link SchemaValidatorsConfig}.
     */
    private static class StubConfig implements SchemaValidatorsConfig {

        private final PathType pathType;

        StubConfig(PathType pathType) {
            this.pathType = pathType;
        }

        @Override
        public PathType getPathType() {
            return pathType;
        }
        // All other methods are left with default (no‑op) implementations.
    }

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}
     * required for the tests.
     */
    private static class DummyValidator extends BaseJsonValidator {

        DummyValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            // No validation logic needed for the atRoot() tests.
            return Collections.emptySet();
        }
    }

    /**
     * Simple {@link ValidationContext} implementation that returns a configurable
     * {@link SchemaValidatorsConfig}.
     */
    private static class StubContext implements ValidationContext {

        private final SchemaValidatorsConfig config;

        StubContext(SchemaValidatorsConfig config) {
            this.config = config;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return config;
        }

        // The remaining methods of the interface are not needed for these tests.
        @Override
        public JsonSchema getParentSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchema getRootSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonNode getRootNode() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonNode getCurrentNode() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getCurrentPath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCurrentPath(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCurrentNode(JsonNode node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRootNode(JsonNode node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setParentSchema(JsonSchema schema) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setRootSchema(JsonSchema schema) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCurrentSchema(JsonSchema schema) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchema getCurrentSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchema getRootSchemaOrSelf() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchema getCurrentSchemaOrSelf() {
            throw new UnsupportedOperationException();
        }
    }

    private static class RootProvider {

        final String expectedRoot;

        final PathType pathType;

        final String description;

        RootProvider(PathType pathType, String expectedRoot, String description) {
            this.pathType = pathType;
            this.expectedRoot = expectedRoot;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default stubbing for the "custom PathType" tests.
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.getPathType()).thenReturn(mockPathType);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks between tests to avoid cross‑test contamination.
        Mockito.reset(mockParentSchema, mockValidatorType, mockValidationContext, mockConfig, mockPathType, mockSchemaNode);
    }

    /**
     * --------------------------------------------------------------
     *  2️⃣  Normal behaviour with several PathType implementations
     *  --------------------------------------------------------------
     */
    private static Stream<RootProvider> normalRootProvider() {
        return Stream.of(new RootProvider(PathType.JSON_PATH, "$", "PathType.JSON_PATH (JSONPath root \"$\")"), new RootProvider(PathType.JSON_POINTER, "/", "PathType.JSON_POINTER (JSON Pointer root \"/\")"));
    }

    /**
     * Helper that injects a {@link PathType} instance into a {@link BaseJsonValidator}
     * via reflection.
     */
    private static void setPathType(BaseJsonValidator validator, PathType pathType) throws Exception {
        Field field = BaseJsonValidator.class.getDeclaredField("pathType");
        field.setAccessible(true);
        field.set(validator, pathType);
    }

    @Test
    void atRoot_returnsDefaultRoot_whenValidationContextIsNull() {
        // ValidationContext is null → BaseJsonValidator falls back to PathType.DEFAULT
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        String root = validator.atRoot();
        Assertions.assertEquals("$", root, "When ValidationContext is null the validator should use PathType.DEFAULT and return \"$\" as the root path.");
    }

    @Test
    void atRoot_returnsCustomRoot_whenPathTypeIsProvided() throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        setPathType(validator, new SimplePathType("customRoot"));
        String root = validator.atRoot();
        Assertions.assertEquals("customRoot", root, "The atRoot() method should delegate to the supplied PathType.");
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("normalRootProvider")
    void atRoot_returnsProvidedRoot_forVariousPathTypes(RootProvider provider) throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        setPathType(validator, provider.pathType);
        String root = validator.atRoot();
        Assertions.assertEquals(provider.expectedRoot, root, "atRoot() should return the root supplied by PathType.getRoot()");
    }

    /**
     * --------------------------------------------------------------
     *  3️⃣  Custom PathType that returns null
     *  --------------------------------------------------------------
     */
    @Test
    void atRoot_propagatesNull_whenPathTypeReturnsNull() throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        setPathType(validator, new SimplePathType(null));
        Assertions.assertNull(validator.atRoot(), "If PathType.getRoot() returns null, atRoot() should return null without throwing.");
    }

    /**
     * --------------------------------------------------------------
     *  4️⃣  Null PathType → NullPointerException
     *  --------------------------------------------------------------
     */
    @Test
    void atRoot_throwsNullPointerException_whenPathTypeIsNull() throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        // Force the private 'pathType' field to null via reflection.
        setPathType(validator, null);
        Assertions.assertThrows(NullPointerException.class, validator::atRoot, "Calling atRoot() with a null PathType should result in NullPointerException.");
    }

    /**
     * --------------------------------------------------------------
     *  5️⃣  PathType#getRoot() throws an unchecked exception → propagation
     *  --------------------------------------------------------------
     */
    @Test
    void atRoot_propagatesRuntimeException_whenPathTypeThrows() throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        setPathType(validator, SimplePathType.throwing());
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, validator::atRoot, "The exception from PathType.getRoot() should be propagated unchanged.");
        Assertions.assertEquals("boom", ex.getMessage(), "Propagated exception should retain the original message.");
    }

    @Test
    void atRoot_returnsRootFromValidationContext() {
        // Provide a ValidationContext that supplies PathType.JSON_POINTER (root "/")
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setPathType(PathType.JSON_POINTER);
        StubContext ctx = new StubContext(cfg);
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, ctx, false);
        String root = validator.atRoot();
        Assertions.assertEquals("/", root, "When a ValidationContext supplies a PathType, atRoot() should return that PathType's root.");
    }

    @Test
    void atRoot_returnsJsonPointerRoot_whenPathTypeIsProvided() throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        setPathType(validator, PathType.JSON_POINTER);
        String root = validator.atRoot();
        Assertions.assertEquals("/", root, "The atRoot() method should delegate to the supplied PathType.");
    }
}
