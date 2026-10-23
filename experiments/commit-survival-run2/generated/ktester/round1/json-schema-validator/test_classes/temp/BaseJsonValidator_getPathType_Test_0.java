package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidationContext.DiscriminatorContext;
import com.networknt.schema.ValidatorTypeCode;
import com.networknt.schema.PathType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.lang.reflect.Field;
import java.util.Collection;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getPathType_Test {

    // -------------------------------------------------------------------------
    @Mock
    private JsonSchema // parent schema – not used in the test
    parentSchema;

    @Mock
    private ValidatorTypeCode // validator type – not used in the test
    validatorType;

    @Mock
    private ValidationContext // context injected into the validator
    validationContext;

    // -------------------------------------------------------------------------
    private static final String SCHEMA_PATH = "$";

    private static final JsonNode SCHEMA_NODE = mock(JsonNode.class);

    /**
     * Minimal concrete validator needed only to expose the protected {@code getPathType()}
     * method for verification.
     */
    private static final class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        // Required by the JsonValidator contract – return an empty set for simplicity.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
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
     * Subclass that does **not** override {@code getPathType()} – used to verify that the
     * inheritance chain works as expected.
     */
    private static final class InheritedValidator extends TestValidator {

        InheritedValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }
    }

    /**
     * Subclass that deliberately shadows {@code getPathType()} with its own public method.
     * The test will invoke {@code super.getPathType()} via a helper to confirm that the
     * original value stored in the base class is unchanged.
     */
    private static final class ShadowingValidator extends TestValidator {

        ShadowingValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        /**
         * A public method that *shadows* the base implementation.
         */
        public PathType getPathType() {
            // Return a different constant deliberately.
            return PathType.JSON_POINTER;
        }

        /**
         * Helper that forwards to the protected method defined in BaseJsonValidator.
         */
        public PathType getSuperPathType() {
            // “super” cannot be used outside the class hierarchy, therefore we call the
            // protected method directly – it resolves to the BaseJsonValidator version.
            return super.getPathType();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test to avoid cross‑test interference.
        reset(parentSchema, validatorType, validationContext);
        reset(parentSchema, validatorType, validationContext);
    }

    /**
     * Reflectively sets the {@code pathType} field of a {@link BaseJsonValidator}
     * instance. This is required because the field is {@code private final} and there
     * is no public setter.
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
            fail("Unable to set pathType reflectively: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("getPathType() returns the default PathType when no config is supplied")
    void getPathType_returnsDefaultWhenConfigIsNull() {
        // ValidationContext#getConfig() returns null → BaseJsonValidator falls back to PathType.DEFAULT
        when(validationContext.getConfig()).thenReturn(null);
        TestValidator validator = new TestValidator(SCHEMA_PATH, SCHEMA_NODE, parentSchema, validatorType, validationContext, false);
        PathType actual = validator.getPathType();
        // The library defines DEFAULT as the fallback; we assert that the returned value
        // equals the constant used by the implementation.
        assertEquals(PathType.DEFAULT, actual, "When no configuration is provided, getPathType() should return PathType.DEFAULT");
    }

    @Test
    @DisplayName("getPathType() returns the PathType defined in the supplied config")
    void getPathType_returnsConfiguredValue() {
        // Arrange a custom configuration that specifies a non‑default PathType.
        SchemaValidatorsConfig config = mock(SchemaValidatorsConfig.class);
        when(config.getPathType()).thenReturn(PathType.JSON_POINTER);
        when(validationContext.getConfig()).thenReturn(config);
        TestValidator validator = new TestValidator(SCHEMA_PATH, SCHEMA_NODE, parentSchema, validatorType, validationContext, false);
        PathType actual = validator.getPathType();
        assertEquals(PathType.JSON_POINTER, actual, "getPathType() should return the PathType supplied by the configuration");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("getPathType() falls back to UNKNOWN when internal field is null")
    void getPathType_nullFallsBackToUnknown() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, SCHEMA_NODE, parentSchema, validatorType, validationContext, false);
        // Force the private field to be null.
        setPathType(validator, null);
        // The contract (as stipulated by the test data) says a null should be treated as UNKNOWN.
        PathType result = validator.getPathType();
        assertEquals(PathType.UNKNOWN, result, "When the internal pathType is null, getPathType() should return PathType.UNKNOWN");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Calling validate() does not change the stored PathType")
    void getPathType_immutableAfterValidate() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, SCHEMA_NODE, parentSchema, validatorType, validationContext, false);
        setPathType(validator, PathType.JSON_PATH);
        // invoke the public method (which does nothing but is part of the API)
        validator.validate(null, mock(JsonNode.class));
        assertEquals(PathType.JSON_PATH, validator.getPathType(), "PathType must remain unchanged after calling validate()");
    }

    @Test
    @DisplayName("Calling preloadJsonSchemas() does not change the stored PathType")
    void getPathType_immutableAfterPreloadJsonSchemas() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, SCHEMA_NODE, parentSchema, validatorType, validationContext, false);
        setPathType(validator, PathType.SCHEMA_PATH);
        // preload with an empty collection – the method is protected, so we expose it via a helper.
        validator.callPreloadJsonSchemas(Collections.emptyList());
        assertEquals(PathType.SCHEMA_PATH, validator.getPathType(), "PathType must remain unchanged after preloadJsonSchemas()");
    }

    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Subclass that does not override getPathType() inherits the correct value")
    void subclass_inheritsPathType() {
        InheritedValidator validator = new InheritedValidator(SCHEMA_PATH, SCHEMA_NODE, parentSchema, validatorType, validationContext, false);
        setPathType(validator, PathType.JSON_POINTER);
        assertEquals(PathType.JSON_POINTER, validator.getPathType(), "Inherited subclass should return the PathType stored in the base class");
    }

    @Test
    @DisplayName("Subclass that shadows getPathType() – super.getPathType() still returns original")
    void subclass_shadowingSuperPathType() {
        ShadowingValidator validator = new ShadowingValidator(SCHEMA_PATH, SCHEMA_NODE, parentSchema, validatorType, validationContext, false);
        setPathType(validator, PathType.JSON_PATH);
        // The public shadow method returns JSON_POINTER, but the helper that calls the
        // protected base implementation must still return the injected JSON_PATH.
        assertEquals(PathType.JSON_POINTER, validator.getPathType(), "Shadowing method should return its own constant");
        assertEquals(PathType.JSON_PATH, validator.getSuperPathType(), "super.getPathType() must return the originally stored PathType");
    }
}
