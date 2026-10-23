```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Set;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tests for {@link BaseJsonValidator#atRoot()}.
 *
 * The original test suite used Mockito and tried to create custom {@link PathType}
 * implementations by subclassing the enum.  Since {@code PathType} is a final enum
 * this is not possible.  The tests have therefore been simplified to work with
 * the real {@code PathType} enum values and without any Mockito usage.
 */
class BaseJsonValidator_atRoot_Test {

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}
     * required for the tests.
     */
    private static class DummyValidator extends BaseJsonValidator {

        DummyValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema,
                       ValidatorTypeCode validatorType, ValidationContext validationContext,
                       boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext,
                  suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                               JsonNode rootNode, String at) {
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
        @Override public JsonSchema getParentSchema() { throw new UnsupportedOperationException(); }
        @Override public JsonSchema getRootSchema() { throw new UnsupportedOperationException(); }
        @Override public JsonNode getRootNode() { throw new UnsupportedOperationException(); }
        @Override public JsonNode getCurrentNode() { throw new UnsupportedOperationException(); }
        @Override public String getCurrentPath() { throw new UnsupportedOperationException(); }
        @Override public String getPath() { throw new UnsupportedOperationException(); }
        @Override public void setCurrentPath(String path) { throw new UnsupportedOperationException(); }
        @Override public void setCurrentNode(JsonNode node) { throw new UnsupportedOperationException(); }
        @Override public void setRootNode(JsonNode node) { throw new UnsupportedOperationException(); }
        @Override public void setParentSchema(JsonSchema schema) { throw new UnsupportedOperationException(); }
        @Override public void setRootSchema(JsonSchema schema) { throw new UnsupportedOperationException(); }
        @Override public void setCurrentSchema(JsonSchema schema) { throw new UnsupportedOperationException(); }
        @Override public JsonSchema getCurrentSchema() { throw new UnsupportedOperationException(); }
        @Override public JsonSchema getRootSchemaOrSelf() { throw new UnsupportedOperationException(); }
        @Override public JsonSchema getCurrentSchemaOrSelf() { throw new UnsupportedOperationException(); }
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
        Assertions.assertEquals("$", root,
                "When ValidationContext is null the validator should use PathType.DEFAULT and return \"$\" as the root path.");
    }

    @Test
    void atRoot_returnsJsonPointerRoot_whenPathTypeIsProvided() throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        setPathType(validator, PathType.JSON_POINTER);
        String root = validator.atRoot();
        Assertions.assertEquals("/", root,
                "The atRoot() method should delegate to the supplied PathType.");
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

    private static Stream<RootProvider> normalRootProvider() {
        return Stream.of(
                new RootProvider(PathType.JSON_PATH, "$", "PathType.JSON_PATH (JSONPath root \"$\")"),
                new RootProvider(PathType.JSON_POINTER, "/", "PathType.JSON_POINTER (JSON Pointer root \"/\")")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalRootProvider")
    void atRoot_returnsProvidedRoot_forVariousPathTypes(RootProvider provider) throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        setPathType(validator, provider.pathType);
        String root = validator.atRoot();
        Assertions.assertEquals(provider.expectedRoot, root,
                "atRoot() should return the root supplied by PathType.getRoot()");
    }

    @Test
    void atRoot_returnsRootFromValidationContext() {
        // Provide a ValidationContext that supplies PathType.JSON_POINTER (root "/")
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setPathType(PathType.JSON_POINTER);
        StubContext ctx = new StubContext(cfg);
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, ctx, false);
        String root = validator.atRoot();
        Assertions.assertEquals("/", root,
                "When a ValidationContext supplies a PathType, atRoot() should return that PathType's root.");
    }
}
```