```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Unit tests for {@link BaseJsonValidator#getPathType()} behaviour.
 * <p>
 * The original test relied on Mockito which is not available in the compilation
 * environment. All uses of Mockito have therefore been removed and replaced with
 * simple stubs or {@code null} values.
 */
@DisplayName("BaseJsonValidator#getPathType() behaviour")
class BaseJsonValidator_getPathType_Test {

    // -------------------------------------------------------------------------
    // These fields were previously annotated with @Mock. They are kept only to
    // hold a reference that can be passed to the validator constructor; a value
    // of {@code null} is sufficient for the tests.
    private static final JsonSchema PARENT_SCHEMA = null;
    private static final ValidatorTypeCode VALIDATOR_TYPE = null;
    private static final ValidationContext VALIDATION_CONTEXT = null;

    // -------------------------------------------------------------------------
    private static final String SCHEMA_PATH = "$";

    private static final JsonNode SCHEMA_NODE = NullNode.getInstance();

    /**
     * Minimal concrete validator needed only to expose the protected
     * {@code getPathType()} method for verification.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        // The library defines two overloads of {@code validate}.  Both are
        // provided here without the {@code @Override} annotation so the code
        // compiles regardless of the exact signature present in the version
        // used during compilation.
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            return Collections.emptySet();
        }

        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node) {
            return Collections.emptySet();
        }

        /**
         * Calls the protected {@code preloadJsonSchemas} method of the base class.
         */
        void callPreloadJsonSchemas(Collection<JsonSchema> schemas) {
            preloadJsonSchemas(schemas);
        }
    }

    /**
     * Subclass that does **not** override {@code getPathType()} – used to verify
     * that the inheritance chain works as expected.
     */
    private static class InheritedValidator extends TestValidator {
        InheritedValidator(String schemaPath,
                           JsonNode schemaNode,
                           JsonSchema parentSchema,
                           ValidatorTypeCode validatorType,
                           ValidationContext validationContext,
                           boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }
    }

    /**
     * Subclass that deliberately shadows {@code getPathType()} with its own
     * public method. The test will invoke {@code super.getPathType()} via a
     * helper to confirm that the original value stored in the base class is
     * unchanged.
     */
    private static class ShadowingValidator extends TestValidator {
        ShadowingValidator(String schemaPath,
                           JsonNode schemaNode,
                           JsonSchema parentSchema,
                           ValidatorTypeCode validatorType,
                           ValidationContext validationContext,
                           boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        /** A public method that *shadows* the base implementation. */
        public PathType getPathType() {
            // Return a different constant deliberately.
            return PathType.JSON_POINTER;
        }

        /** Helper that forwards to the protected method defined in BaseJsonValidator. */
        public PathType getSuperPathType() {
            return super.getPathType();
        }
    }

    /**
     * Reflectively sets the {@code pathType} field of a {@link BaseJsonValidator}
     * instance. This is required because the field is {@code private final} and
     * there is no public setter.
     *
     * @param validator the instance whose field should be modified
     * @param value     the {@link PathType} value (may be {@code null})
     */
    private static void setPathType(BaseJsonValidator validator, PathType value) {
        try {
            Field field = BaseJsonValidator.class.getDeclaredField("pathType");
            field.setAccessible(true);
            field.set(validator, value);
        } catch (Exception e) {
            Assertions.fail("Unable to set pathType reflectively: " + e.getMessage());
        }
    }

    private static ValidationContext createContextWithConfig(SchemaValidatorsConfig config) {
        // Constructor signature: (URIFactory, URNFactory, JsonMetaSchema,
        // JsonSchemaFactory, SchemaValidatorsConfig)
        return new ValidationContext(null, null, null, null, config);
    }

    @Test
    @DisplayName("getPathType() returns the default PathType when no config is supplied")
    void getPathType_returnsDefaultWhenConfigIsNull() {
        ValidationContext context = createContextWithConfig(null);
        TestValidator validator = new TestValidator(SCHEMA_PATH, SCHEMA_NODE, null, null, context, false);
        PathType actual = validator.getPathType();
        Assertions.assertEquals(PathType.DEFAULT, actual,
                "When no configuration is provided, getPathType() should return PathType.DEFAULT");
    }

    @Test
    @DisplayName("getPathType() returns the PathType defined in the supplied config")
    void getPathType_returnsConfiguredValue() {
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setPathType(PathType.JSON_POINTER);
        ValidationContext context = createContextWithConfig(config);
        TestValidator validator = new TestValidator(SCHEMA_PATH, SCHEMA_NODE, null, null, context, false);
        PathType actual = validator.getPathType();
        Assertions.assertEquals(PathType.JSON_POINTER, actual,
                "getPathType() should return the PathType supplied by the configuration");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("getPathType() falls back to DEFAULT when internal field is null")
    void getPathType_nullFallsBackToDefault() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, SCHEMA_NODE, PARENT_SCHEMA, VALIDATOR_TYPE, VALIDATION_CONTEXT, false);
        // Force the private field to be null.
        setPathType(validator, null);
        PathType result = validator.getPathType();
        Assertions.assertEquals(PathType.DEFAULT, result,
                "When the internal pathType is null, getPathType() should return PathType.DEFAULT");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Calling validate() does not change the stored PathType")
    void getPathType_immutableAfterValidate() {
        ValidationContext context = createContextWithConfig(null);
        TestValidator validator = new TestValidator(SCHEMA_PATH, SCHEMA_NODE, null, null, context, false);
        setPathType(validator, PathType.JSON_PATH);
        // invoke the public method (which does nothing but is part of the API)
        validator.validate(null, NullNode.getInstance());
        Assertions.assertEquals(PathType.JSON_PATH, validator.getPathType(),
                "PathType must remain unchanged after calling validate()");
    }

    @Test
    @DisplayName("Calling preloadJsonSchemas() does not change the stored PathType")
    void getPathType_immutableAfterPreloadJsonSchemas() {
        ValidationContext context = createContextWithConfig(null);
        TestValidator validator = new TestValidator(SCHEMA_PATH, SCHEMA_NODE, null, null, context, false);
        setPathType(validator, PathType.JSON_PATH);
        // preload with an empty collection – the method is protected, so we expose it via a helper.
        validator.callPreloadJsonSchemas(Collections.emptyList());
        Assertions.assertEquals(PathType.JSON_PATH, validator.getPathType(),
                "PathType must remain unchanged after preloadJsonSchemas()");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Subclass that does not override getPathType() inherits the correct value")
    void subclass_inheritsPathType() {
        ValidationContext context = createContextWithConfig(null);
        InheritedValidator validator = new InheritedValidator(SCHEMA_PATH, SCHEMA_NODE, null, null, context, false);
        setPathType(validator, PathType.JSON_POINTER);
        Assertions.assertEquals(PathType.JSON_POINTER, validator.getPathType(),
                "Inherited subclass should return the PathType stored in the base class");
    }

    @Test
    @DisplayName("Subclass that shadows getPathType() – super.getPathType() still returns original")
    void subclass_shadowingSuperPathType() {
        ValidationContext context = createContextWithConfig(null);
        ShadowingValidator validator = new ShadowingValidator(SCHEMA_PATH, SCHEMA_NODE, null, null, context, false);
        setPathType(validator, PathType.JSON_PATH);
        // The public shadow method returns JSON_POINTER, but the helper that calls the
        // protected base implementation must still return the injected JSON_PATH.
        Assertions.assertEquals(PathType.JSON_POINTER, validator.getPathType(),
                "Shadowing method should return its own constant");
        Assertions.assertEquals(PathType.JSON_PATH, validator.getSuperPathType(),
                "super.getPathType() must return the originally stored PathType");
    }
}
```