```java
package com.networknt.schema;

import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

/**
 * Unit tests for {@link BaseJsonValidator#atRoot()}.
 *
 * <p>This test class avoids external mocking frameworks and instead uses simple
 * stub implementations for the required interfaces. The original tests that
 * attempted to create custom {@code PathType} implementations have been
 * removed because {@code PathType} is an {@code enum} and cannot be subclassed.
 * The remaining tests verify the default behaviour, the handling of a supplied
 * {@code PathType} via {@code ValidationContext}, and the null‑pointer cases.</p>
 */
class BaseJsonValidatorAtRootTest {

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

    /** Simple stub for {@link SchemaValidatorsConfig}. */
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

    /** Simple stub for {@link ValidationContext}. */
    private static class StubContext implements ValidationContext {
        private final SchemaValidatorsConfig config;

        StubContext(SchemaValidatorsConfig config) {
            this.config = config;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return config;
        }

        // All other methods are left with default (no‑op) implementations.
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
    void atRoot_returnsRootFromValidationContext() {
        // Provide a ValidationContext that supplies PathType.JSON_POINTER (root "/")
        StubContext ctx = new StubContext(new StubConfig(PathType.JSON_POINTER));
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, ctx, false);
        String root = validator.atRoot();
        Assertions.assertEquals("/", root,
                "When a ValidationContext supplies a PathType, atRoot() should return that PathType's root.");
    }

    @Test
    void atRoot_throwsNullPointerException_whenPathTypeIsNull() throws Exception {
        DummyValidator validator = new DummyValidator("schemaPath", null, null, null, null, false);
        // Force the private 'pathType' field to null via reflection.
        setPathType(validator, null);
        Assertions.assertThrows(NullPointerException.class,
                validator::atRoot,
                "Calling atRoot() with a null PathType should result in NullPointerException.");
    }
}
```