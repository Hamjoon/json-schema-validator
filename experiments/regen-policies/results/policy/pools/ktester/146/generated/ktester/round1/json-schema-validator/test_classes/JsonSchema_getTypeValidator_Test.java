package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    /**
     * Simple schema that defines a primitive type – should yield a {@link TypeValidator}.
     */
    private static final String SCHEMA_WITH_TYPE = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "string"
        }
        """;

    /**
     * Schema that does **not** define a {@code type} keyword – should yield {@code null}.
     */
    private static final String SCHEMA_WITHOUT_TYPE = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "properties": {
            "name": { "type": "string" }
          }
        }
        """;

    /**
     * The factory used to create {@link JsonSchema} instances for the tests.
     */
    private JsonSchemaFactory factory;

    /**
     * Dummy validator used when we need a non‑empty validators list.
     */
    @Mock
    private JsonValidator dummyJsonValidator;

    /**
     * Dummy {@link TypeValidator} that we can inject into the schema.
     */
    @Mock
    private TypeValidator dummyTypeValidator;

    private TestableJsonSchema schema;

    /**
     * Flag that the overridden {@code getValidators()} sets when it is called.
     */
    private static class TestableJsonSchema extends JsonSchema {

        boolean getValidatorsCalled = false;

        /**
         * What the overridden method should return.
         */
        List<JsonValidator> validatorsToReturn = new ArrayList<>();

        /**
         * Whether the overridden method should also set {@code typeValidator}.
         */
        boolean shouldSetTypeValidator = false;

        /**
         * Exception to be thrown from the overridden method (null = no exception).
         */
        RuntimeException exceptionToThrow = null;

        @Override
        public List<JsonValidator> getValidators() {
            getValidatorsCalled = true;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            // Populate the private fields via reflection.
            try {
                Field validatorsField = JsonSchema.class.getDeclaredField("validators");
                validatorsField.setAccessible(true);
                validatorsField.set(this, validatorsToReturn);
                if (shouldSetTypeValidator) {
                    Field typeField = JsonSchema.class.getDeclaredField("typeValidator");
                    typeField.setAccessible(true);
                    // The test will set the dummy instance directly on the subclass field.
                    typeField.set(this, ((TestableJsonSchema) this).typeValidator);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return validatorsToReturn;
        }

        /**
         * Helper to inject a {@link TypeValidator} directly (bypassing the overridden method).
         */
        void injectTypeValidator(TypeValidator tv) {
            try {
                Field typeField = JsonSchema.class.getDeclaredField("typeValidator");
                typeField.setAccessible(true);
                typeField.set(this, tv);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Helper to inject a validators list directly (bypassing the overridden method).
         */
        void injectValidators(List<JsonValidator> list) {
            try {
                Field validatorsField = JsonSchema.class.getDeclaredField("validators");
                validatorsField.setAccessible(true);
                validatorsField.set(this, list);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh factory for each test – the factory is cheap and
        // guarantees a clean validation context.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // The base class has a protected no‑arg constructor, so we can instantiate it directly.
        schema = new TestableJsonSchema();
        // Ensure a clean state before each test.
        schema.getValidatorsCalled = false;
        schema.validatorsToReturn = new ArrayList<>();
        schema.shouldSetTypeValidator = false;
        schema.exceptionToThrow = null;
        schema.injectValidators(null);
        schema.injectTypeValidator(null);
    }

    @Test
    void getTypeValidator_returnsNonNullWhenSchemaHasType() {
        // Arrange: obtain a JsonSchema that contains a top‑level "type" keyword.
        JsonSchema schema = factory.getSchema(SCHEMA_WITH_TYPE);
        // Act: invoke the method under test.
        TypeValidator typeValidator = schema.getTypeValidator();
        // Assert: a TypeValidator must be present and correctly initialised.
        assertNotNull(typeValidator, "Expected a non‑null TypeValidator for a schema with a 'type' keyword");
        // The validator should be of the concrete class TypeValidator (not a subclass).
        assertEquals(TypeValidator.class, typeValidator.getClass(), "Expected the validator to be an instance of TypeValidator");
        // Subsequent calls must return the same instance (lazy loading caches the result).
        assertSame(typeValidator, schema.getTypeValidator(), "Subsequent calls to getTypeValidator() should return the cached instance");
    }

    @Test
    void getTypeValidator_returnsNullWhenSchemaHasNoType() {
        // Arrange: obtain a JsonSchema that does NOT contain a top‑level "type" keyword.
        JsonSchema schema = factory.getSchema(SCHEMA_WITHOUT_TYPE);
        // Act: invoke the method under test.
        TypeValidator typeValidator = schema.getTypeValidator();
        // Assert: the method should return null because no type validator was created.
        assertNull(typeValidator, "Expected null TypeValidator for a schema without a top‑level 'type' keyword");
    }

    /* -------------------------------------------------------------
     *  1. validators is null – lazy loading triggers getValidators()
     * ------------------------------------------------------------- */
    @Test
    void validatorsIsNull_lazyLoadingInvokesGetValidators() {
        // No pre‑initialisation – validators field is null.
        // Configure the overridden method to set a dummy TypeValidator.
        schema.shouldSetTypeValidator = true;
        // field in subclass, later copied via reflection.
        schema.typeValidator = dummyTypeValidator;
        // Act
        TypeValidator result = schema.getTypeValidator();
        // Assert
        assertTrue(schema.getValidatorsCalled, "getValidators() must have been called because validators was initially null");
        assertSame(dummyTypeValidator, result, "The returned TypeValidator should be the dummy instance set by the overridden getValidators()");
    }

    /* -------------------------------------------------------------
     *  2. validators already initialized – getValidators() not called
     * ------------------------------------------------------------- */
    @Test
    void validatorsAlreadyInitialized_noLazyLoading() throws Exception {
        // Pre‑populate both private fields.
        schema.injectValidators(Collections.singletonList(dummyJsonValidator));
        schema.injectTypeValidator(dummyTypeValidator);
        // Act
        TypeValidator result = schema.getTypeValidator();
        // Assert
        assertFalse(schema.getValidatorsCalled, "getValidators() must NOT be called when validators is already non‑null");
        assertSame(dummyTypeValidator, result, "The method should return the pre‑populated TypeValidator");
    }

    /* -------------------------------------------------------------
     *  3. validators null – lazy loading with getValidators NOT setting typeValidator
     * ------------------------------------------------------------- */
    @Test
    void validatorsNull_lazyLoadingWithoutSettingTypeValidator_returnsNull() {
        // Configure overridden getValidators to *only* initialise validators list.
        schema.shouldSetTypeValidator = false;
        schema.validatorsToReturn = Collections.singletonList(dummyJsonValidator);
        // Act
        TypeValidator result = schema.getTypeValidator();
        // Assert
        assertTrue(schema.getValidatorsCalled, "getValidators() must have been called because validators was initially null");
        assertNull(result, "When getValidators() does not set typeValidator, the method should return null");
        // Verify that the validators list is now non‑null.
        try {
            Field f = JsonSchema.class.getDeclaredField("validators");
            f.setAccessible(true);
            assertNotNull(f.get(schema), "validators list should have been initialised by getValidators()");
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    /* -------------------------------------------------------------
     *  4. validators already initialized but typeValidator null – no lazy loading
     * ------------------------------------------------------------- */
    @Test
    void validatorsInitialized_typeValidatorNull_noLazyLoading() {
        // Populate validators but leave typeValidator null.
        schema.injectValidators(Collections.singletonList(dummyJsonValidator));
        // Act
        TypeValidator result = schema.getTypeValidator();
        // Assert
        assertFalse(schema.getValidatorsCalled, "getValidators() must NOT be called when validators is already initialised");
        assertNull(result, "With a non‑null validators list but no typeValidator, the method should return null");
    }

    /* -------------------------------------------------------------
     *  5. multiple getTypeValidator calls after lazy loading – getValidators called only once
     * ------------------------------------------------------------- */
    @Test
    void multipleCalls_lazyLoadingInvokesGetValidatorsOnlyOnce() {
        // Configure overridden method to set a dummy TypeValidator.
        schema.shouldSetTypeValidator = true;
        schema.typeValidator = dummyTypeValidator;
        // First call – triggers lazy loading.
        TypeValidator first = schema.getTypeValidator();
        // Second call – should use cached value.
        TypeValidator second = schema.getTypeValidator();
        // Assert
        assertTrue(schema.getValidatorsCalled, "getValidators() should have been called during the first invocation");
        assertSame(dummyTypeValidator, first, "First call must return the dummy TypeValidator");
        assertSame(dummyTypeValidator, second, "Second call must return the same cached TypeValidator");
    }

    /* -------------------------------------------------------------
     *  6. validators null – getValidators throws RuntimeException
     * ------------------------------------------------------------- */
    @Test
    void validatorsNull_getValidatorsThrowsRuntimeException_propagates() {
        schema.exceptionToThrow = new RuntimeException("boom");
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> schema.getTypeValidator(), "RuntimeException from getValidators() must propagate");
        assertEquals("boom", thrown.getMessage());
    }

    /* -------------------------------------------------------------
     *  7. validators null – getValidators throws IllegalArgumentException
     * ------------------------------------------------------------- */
    @Test
    void validatorsNull_getValidatorsThrowsIllegalArgumentException_propagates() {
        schema.exceptionToThrow = new IllegalArgumentException("bad argument");
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> schema.getTypeValidator(), "IllegalArgumentException from getValidators() must propagate");
        assertEquals("bad argument", thrown.getMessage());
    }

    /* -------------------------------------------------------------
     *  8. validators null – getValidators throws IllegalStateException
     * ------------------------------------------------------------- */
    @Test
    void validatorsNull_getValidatorsThrowsIllegalStateException_propagates() {
        schema.exceptionToThrow = new IllegalStateException("illegal state");
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> schema.getTypeValidator(), "IllegalStateException from getValidators() must propagate");
        assertEquals("illegal state", thrown.getMessage());
    }

    /* -------------------------------------------------------------
     *  9. validators null – getValidators throws AssertionError (Error subclass)
     * ------------------------------------------------------------- */
    @Test
    void validatorsNull_getValidatorsThrowsAssertionError_propagates() {
        schema.exceptionToThrow = new AssertionError("assertion failed");
        AssertionError thrown = assertThrows(AssertionError.class, () -> schema.getTypeValidator(), "AssertionError from getValidators() must propagate");
        assertEquals("assertion failed", thrown.getMessage());
    }
}
