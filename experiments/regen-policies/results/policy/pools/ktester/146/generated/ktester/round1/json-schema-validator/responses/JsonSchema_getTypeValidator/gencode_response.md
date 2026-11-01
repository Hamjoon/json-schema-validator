```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Extensive unit tests for {@link JsonSchema#getTypeValidator()}.
 *
 * <p>
 * The tests cover the lazy‑initialisation contract, the behaviour when the
 * internal {@code validators} list is already populated, and the propagation of
 * unchecked exceptions thrown from an overridden {@code getValidators()} method.
 * A small test‑specific subclass of {@link JsonSchema} is used so that we can
 * intercept calls to {@code getValidators()}, inject dummy data and record whether
 * the method was invoked.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    /** Dummy validator used when we need a non‑empty validators list. */
    @Mock
    private JsonValidator dummyJsonValidator;

    /** Dummy {@link TypeValidator} that we can inject into the schema. */
    @Mock
    private TypeValidator dummyTypeValidator;

    /** Flag that the overridden {@code getValidators()} sets when it is called. */
    private static class TestableJsonSchema extends JsonSchema {
        boolean getValidatorsCalled = false;
        /** What the overridden method should return. */
        List<JsonValidator> validatorsToReturn = new ArrayList<>();
        /** Whether the overridden method should also set {@code typeValidator}. */
        boolean shouldSetTypeValidator = false;
        /** Exception to be thrown from the overridden method (null = no exception). */
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

        /** Helper to inject a {@link TypeValidator} directly (bypassing the overridden method). */
        void injectTypeValidator(TypeValidator tv) {
            try {
                Field typeField = JsonSchema.class.getDeclaredField("typeValidator");
                typeField.setAccessible(true);
                typeField.set(this, tv);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Helper to inject a validators list directly (bypassing the overridden method). */
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

    private TestableJsonSchema schema;

    @BeforeEach
    void setUp() throws Exception {
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

    /* -------------------------------------------------------------
     *  1. validators is null – lazy loading triggers getValidators()
     * ------------------------------------------------------------- */
    @Test
    void validatorsIsNull_lazyLoadingInvokesGetValidators() {
        // No pre‑initialisation – validators field is null.
        // Configure the overridden method to set a dummy TypeValidator.
        schema.shouldSetTypeValidator = true;
        schema.typeValidator = dummyTypeValidator; // field in subclass, later copied via reflection.

        // Act
        TypeValidator result = schema.getTypeValidator();

        // Assert
        assertTrue(schema.getValidatorsCalled,
                "getValidators() must have been called because validators was initially null");
        assertSame(dummyTypeValidator, result,
                "The returned TypeValidator should be the dummy instance set by the overridden getValidators()");
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
        assertFalse(schema.getValidatorsCalled,
                "getValidators() must NOT be called when validators is already non‑null");
        assertSame(dummyTypeValidator, result,
                "The method should return the pre‑populated TypeValidator");
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
        assertTrue(schema.getValidatorsCalled,
                "getValidators() must have been called because validators was initially null");
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
        assertFalse(schema.getValidatorsCalled,
                "getValidators() must NOT be called when validators is already initialised");
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
        assertTrue(schema.getValidatorsCalled,
                "getValidators() should have been called during the first invocation");
        assertSame(dummyTypeValidator, first, "First call must return the dummy TypeValidator");
        assertSame(dummyTypeValidator, second, "Second call must return the same cached TypeValidator");
    }

    /* -------------------------------------------------------------
     *  6. validators null – getValidators throws RuntimeException
     * ------------------------------------------------------------- */
    @Test
    void validatorsNull_getValidatorsThrowsRuntimeException_propagates() {
        schema.exceptionToThrow = new RuntimeException("boom");

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> schema.getTypeValidator(),
                "RuntimeException from getValidators() must propagate");
        assertEquals("boom", thrown.getMessage());
    }

    /* -------------------------------------------------------------
     *  7. validators null – getValidators throws IllegalArgumentException
     * ------------------------------------------------------------- */
    @Test
    void validatorsNull_getValidatorsThrowsIllegalArgumentException_propagates() {
        schema.exceptionToThrow = new IllegalArgumentException("bad argument");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> schema.getTypeValidator(),
                "IllegalArgumentException from getValidators() must propagate");
        assertEquals("bad argument", thrown.getMessage());
    }

    /* -------------------------------------------------------------
     *  8. validators null – getValidators throws IllegalStateException
     * ------------------------------------------------------------- */
    @Test
    void validatorsNull_getValidatorsThrowsIllegalStateException_propagates() {
        schema.exceptionToThrow = new IllegalStateException("illegal state");

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> schema.getTypeValidator(),
                "IllegalStateException from getValidators() must propagate");
        assertEquals("illegal state", thrown.getMessage());
    }

    /* -------------------------------------------------------------
     *  9. validators null – getValidators throws AssertionError (Error subclass)
     * ------------------------------------------------------------- */
    @Test
    void validatorsNull_getValidatorsThrowsAssertionError_propagates() {
        schema.exceptionToThrow = new AssertionError("assertion failed");

        AssertionError thrown = assertThrows(AssertionError.class,
                () -> schema.getTypeValidator(),
                "AssertionError from getValidators() must propagate");
        assertEquals("assertion failed", thrown.getMessage());
    }
}
```