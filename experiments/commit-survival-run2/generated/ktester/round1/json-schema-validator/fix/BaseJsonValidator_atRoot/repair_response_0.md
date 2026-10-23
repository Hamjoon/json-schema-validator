```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Tests for {@link BaseJsonValidator#atRoot()}.
 * <p>
 * The tests avoid external mocking frameworks and instead use simple
 * stub implementations together with reflection to inject the desired
 * {@link PathType} into the validator.
 */
class BaseJsonValidator_atRoot_Test {

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}
     * required for the tests.
     */
    private static class DummyValidator extends BaseJsonValidator {

        DummyValidator(String schemaPath,
                       JsonNode schemaNode,
                       JsonSchema parentSchema,
                       ValidatorTypeCode validatorType,
                       ValidationContext validationContext,
                       boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            // No validation logic needed for the atRoot() tests.
            return Collections.emptySet();
        }
    }

    /**
     * Simple {@link PathType} implementation that can return a configurable
     * root value or throw an exception.
     */
    private static class SimplePathType implements PathType {
        private final String root;
        private final boolean throwOnGetRoot;

        SimplePathType(String root) {
            this.root = root;
            this.throwOnGetRoot = false;
        }

        /** Creates an instance that throws an {@link IllegalStateException} when {@code getRoot()} is called. */
        static SimplePathType throwing() {
            SimplePathType p = new SimplePathType(null);
            p.throwOnGetRoot = true;
            return p;
        }

        @Override
        public String getRoot() {
            if (throwOnGetRoot) {
                throw new IllegalStateException("boom");
            }
            return root;
        }

        /** The separator is not used by the tests – return a harmless default. */
        @Override
        public String getSeparator() {
            return ".";
        }
    }

    /**
     * Helper class used by the parameterised test.
     */
    private static class RootProvider {
        final String expectedRoot;
        final String description;

        RootProvider(String expectedRoot, String description) {
            this.expectedRoot = expectedRoot;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Provides a few different root strings that a {@link PathType} might return.
     */
    private static Stream<RootProvider> normalRootProvider() {
        return Stream.of(
                new RootProvider("$", "PathType returning JSONPath root \"$\""),
                new RootProvider("", "PathType returning empty string (JSON Pointer)"),
                new RootProvider("/", "PathType returning \"/\" (JSON Pointer root)")
        );
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
    void atRoot_returnsCustomRoot_whenPathTypeIsProvided() throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        setPathType(validator, new SimplePathType("customRoot"));
        String root = validator.atRoot();
        Assertions.assertEquals("customRoot", root,
                "The atRoot() method should delegate to the supplied PathType.");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalRootProvider")
    void atRoot_returnsProvidedRoot_forVariousPathTypes(RootProvider provider) throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        setPathType(validator, new SimplePathType(provider.expectedRoot));
        String root = validator.atRoot();
        Assertions.assertEquals(provider.expectedRoot, root,
                "atRoot() should return the root supplied by PathType.getRoot()");
    }

    @Test
    void atRoot_propagatesNull_whenPathTypeReturnsNull() throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        setPathType(validator, new SimplePathType(null));
        Assertions.assertNull(validator.atRoot(),
                "If PathType.getRoot() returns null, atRoot() should return null without throwing.");
    }

    @Test
    void atRoot_throwsNullPointerException_whenPathTypeIsNull() throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        // Force the private 'pathType' field to null via reflection.
        setPathType(validator, null);
        Assertions.assertThrows(NullPointerException.class, validator::atRoot,
                "Calling atRoot() with a null PathType should result in NullPointerException.");
    }

    @Test
    void atRoot_propagatesRuntimeException_whenPathTypeThrows() throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        setPathType(validator, SimplePathType.throwing());
        IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, validator::atRoot,
                "The exception from PathType.getRoot() should be propagated unchanged.");
        Assertions.assertEquals("boom", ex.getMessage(),
                "Propagated exception should retain the original message.");
    }
}
```