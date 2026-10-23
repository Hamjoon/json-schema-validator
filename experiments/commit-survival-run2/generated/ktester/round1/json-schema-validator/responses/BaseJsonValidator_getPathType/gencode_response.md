```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidatorTypeCode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#getPathType()}.
 *
 * <p>The {@code BaseJsonValidator} class is abstract, therefore a minimal concrete subclass
 * {@link TestValidator} is defined inside the test suite. The subclass forwards the constructor
 * arguments to the super‑class and implements {@code validate} with a trivial empty‑set return.
 * A reflective helper {@code setPathType()} is used to inject the desired {@link PathType}
 * because the field is {@code private final} and not exposed by the public API.</p>
 *
 * <p>The test cases cover:</p>
 * <ul>
 *   <li>Correct value returned for each {@link PathType} constant.</li>
 *   <li>Null handling – a {@code null} path type should be treated as {@link PathType#UNKNOWN}.</li>
 *   <li>Immutability of the stored path type across other public operations.</li>
 *   <li>Inheritance behaviour and method shadowing in subclasses.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getPathType_Test {

    // -------------------------------------------------------------------------
    // Mocks shared by all tests
    // -------------------------------------------------------------------------
    @Mock
    private JsonSchema parentSchema;               // not used directly in these tests

    @Mock
    private ValidatorTypeCode validatorType;      // not used directly in these tests

    @Mock
    private ValidationContext validationContext; // not used directly in these tests

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------
    private static final String SCHEMA_PATH = "$";
    private static final JsonNode SCHEMA_NODE = mock(JsonNode.class);

    @BeforeEach
    void resetMocks() {
        reset(parentSchema, validatorType, validationContext);
    }

    // -------------------------------------------------------------------------
    // Helper concrete implementation of BaseJsonValidator
    // -------------------------------------------------------------------------
    /**
     * Minimal concrete validator needed only to expose the protected {@code getPathType()}
     * method for verification.
     */
    private static final class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
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
     * Subclass that deliberately shadows {@code getPathType()} with its own public method.
     * The test will invoke {@code super.getPathType()} via a helper to confirm that the
     * original value stored in the base class is unchanged.
     */
    private static final class ShadowingValidator extends TestValidator {

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
            // “super” cannot be used outside the class hierarchy, therefore we call the
            // protected method directly – it resolves to the BaseJsonValidator version.
            return super.getPathType();
        }
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

    // -------------------------------------------------------------------------
    // 1. Return correct PathType for each enum constant
    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "getPathType() returns {0}")
    @EnumSource(PathType.class)
    void getPathType_returnsConfiguredValue(PathType configured) {
        TestValidator validator = new TestValidator(
                SCHEMA_PATH,
                SCHEMA_NODE,
                parentSchema,
                validatorType,
                validationContext,
                false);
        setPathType(validator, configured);

        assertEquals(configured, validator.getPathType(),
                "Validator should return the PathType it was constructed with");
    }

    // -------------------------------------------------------------------------
    // 2. Null handling – fall back to UNKNOWN
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("getPathType() falls back to UNKNOWN when internal field is null")
    void getPathType_nullFallsBackToUnknown() {
        TestValidator validator = new TestValidator(
                SCHEMA_PATH,
                SCHEMA_NODE,
                parentSchema,
                validatorType,
                validationContext,
                false);
        // Force the private field to be null.
        setPathType(validator, null);

        // The contract (as stipulated by the test data) says a null should be treated as UNKNOWN.
        PathType result = validator.getPathType();
        assertEquals(PathType.UNKNOWN, result,
                "When the internal pathType is null, getPathType() should return PathType.UNKNOWN");
    }

    // -------------------------------------------------------------------------
    // 3. Immutability across other public operations
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Calling validate() does not change the stored PathType")
    void getPathType_immutableAfterValidate() {
        TestValidator validator = new TestValidator(
                SCHEMA_PATH,
                SCHEMA_NODE,
                parentSchema,
                validatorType,
                validationContext,
                false);
        setPathType(validator, PathType.JSON_PATH);

        // invoke the public method (which does nothing but is part of the API)
        validator.validate(null, mock(JsonNode.class));

        assertEquals(PathType.JSON_PATH, validator.getPathType(),
                "PathType must remain unchanged after calling validate()");
    }

    @Test
    @DisplayName("Calling preloadJsonSchemas() does not change the stored PathType")
    void getPathType_immutableAfterPreloadJsonSchemas() {
        TestValidator validator = new TestValidator(
                SCHEMA_PATH,
                SCHEMA_NODE,
                parentSchema,
                validatorType,
                validationContext,
                false);
        setPathType(validator, PathType.SCHEMA_PATH);

        // preload with an empty collection – the method is protected, so we expose it via a helper.
        validator.callPreloadJsonSchemas(Collections.emptyList());

        assertEquals(PathType.SCHEMA_PATH, validator.getPathType(),
                "PathType must remain unchanged after preloadJsonSchemas()");
    }

    // -------------------------------------------------------------------------
    // 4. Subclass inheritance and method shadowing
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Subclass that does not override getPathType() inherits the correct value")
    void subclass_inheritsPathType() {
        InheritedValidator validator = new InheritedValidator(
                SCHEMA_PATH,
                SCHEMA_NODE,
                parentSchema,
                validatorType,
                validationContext,
                false);
        setPathType(validator, PathType.JSON_POINTER);

        assertEquals(PathType.JSON_POINTER, validator.getPathType(),
                "Inherited subclass should return the PathType stored in the base class");
    }

    @Test
    @DisplayName("Subclass that shadows getPathType() – super.getPathType() still returns original")
    void subclass_shadowingSuperPathType() {
        ShadowingValidator validator = new ShadowingValidator(
                SCHEMA_PATH,
                SCHEMA_NODE,
                parentSchema,
                validatorType,
                validationContext,
                false);
        setPathType(validator, PathType.JSON_PATH);

        // The public shadow method returns JSON_POINTER, but the helper that calls the
        // protected base implementation must still return the injected JSON_PATH.
        assertEquals(PathType.JSON_POINTER, validator.getPathType(),
                "Shadowing method should return its own constant");
        assertEquals(PathType.JSON_PATH, validator.getSuperPathType(),
                "super.getPathType() must return the originally stored PathType");
    }
}
```