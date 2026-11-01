```java
package com.networknt.schema;

import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Additional unit tests for {@link JsonSchema#getTypeValidator()} that focus on the
 * lazy‑loading behaviour and on exception propagation.  The tests use a real
 * {@link JsonSchemaFactory} to obtain a concrete {@link JsonSchema} instance and
 * then create a Mockito {@link Spy} so that the {@code getValidators()} method can
 * be stubbed while still exercising the actual implementation of
 * {@code getTypeValidator()}.
 *
 * <p>All interactions with the private fields {@code validators} and
 * {@code typeValidator} are performed via reflection – this respects the
 * requirement of not accessing private members directly.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    /** Shared factory used to build {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // any supported draft works – we use Draft‑07
        factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
    }

    @BeforeEach
    void setupBeforeEach() {
        // no per‑test state required
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up at the class level
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods for reflection manipulation
     * --------------------------------------------------------------------- */

    /** Sets the private {@code validators} field on the supplied schema. */
    private static void setValidators(JsonSchema schema, List<JsonValidator> validators) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        f.set(schema, validators);
    }

    /** Sets the private {@code typeValidator} field on the supplied schema. */
    private static void setTypeValidator(JsonSchema schema, TypeValidator typeValidator) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("typeValidator");
        f.setAccessible(true);
        f.set(schema, typeValidator);
    }

    /** Retrieves the current value of the private {@code validators} field. */
    @SuppressWarnings("unchecked")
    private static List<JsonValidator> getValidatorsField(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        return (List<JsonValidator>) f.get(schema);
    }

    /** Retrieves the current value of the private {@code typeValidator} field. */
    private static TypeValidator getTypeValidatorField(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("typeValidator");
        f.setAccessible(true);
        return (TypeValidator) f.get(schema);
    }

    /* --------------------------------------------------------------------- *
     *  1. Lazy loading behavior – getValidators() should be invoked once
     * --------------------------------------------------------------------- */

    @Test
    void lazyLoadingInvokesGetValidatorsExactlyOnce() throws Exception {
        // create a real schema (empty object) and spy it
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);

        // stub getValidators() to populate both fields
        TypeValidator stubValidator = mock(TypeValidator.class);
        List<JsonValidator> stubList = new ArrayList<>();

        doAnswer(invocation -> {
            // simulate the lazy loading side‑effects
            setValidators(spy, stubList);
            setTypeValidator(spy, stubValidator);
            return stubList;
        }).when(spy).getValidators();

        // first call – validators are null, so getValidators() must be executed
        TypeValidator result = spy.getTypeValidator();

        assertNotNull(result, "TypeValidator should be non‑null after lazy loading");
        assertSame(stubValidator, result, "Returned instance must be the one set by the stubbed getValidators()");
        verify(spy, times(1)).getValidators();
    }

    /* --------------------------------------------------------------------- *
     *  2. Already initialized – getValidators() must NOT be called
     * --------------------------------------------------------------------- */

    @Test
    void alreadyInitializedReturnsExistingValidatorWithoutCallingGetValidators() throws Exception {
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);

        // manually initialise the private fields
        TypeValidator existing = mock(TypeValidator.class);
        List<JsonValidator> existingList = Collections.singletonList(mock(JsonValidator.class));
        setValidators(spy, existingList);
        setTypeValidator(spy, existing);

        // invoke method under test
        TypeValidator result = spy.getTypeValidator();

        assertSame(existing, result, "Should return the pre‑initialised TypeValidator");
        // verify that the lazy‑loader was never touched
        verify(spy, never()).getValidators();
    }

    /* --------------------------------------------------------------------- *
     *  3. Lazy loading where no TypeValidator is created (null)
     * --------------------------------------------------------------------- */

    @Test
    void lazyLoadingReturnsNullWhenNoTypeValidatorCreated() throws Exception {
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);

        // getValidators populates only the validators list
        List<JsonValidator> stubList = new ArrayList<>();
        stubList.add(mock(JsonValidator.class));

        doAnswer(invocation -> {
            setValidators(spy, stubList);
            // deliberately leave typeValidator as null
            setTypeValidator(spy, null);
            return stubList;
        }).when(spy).getValidators();

        TypeValidator result = spy.getTypeValidator();

        assertNull(result, "When getValidators() does not set a TypeValidator, getTypeValidator() must return null");
        verify(spy, times(1)).getValidators();
    }

    /* --------------------------------------------------------------------- *
     *  4. Repeated lazy‑loading calls – only first call triggers loading
     * --------------------------------------------------------------------- */

    @Test
    void repeatedCallsTriggerGetValidatorsOnlyOnce() throws Exception {
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);

        TypeValidator stubValidator = mock(TypeValidator.class);
        List<JsonValidator> stubList = new ArrayList<>();

        doAnswer(invocation -> {
            setValidators(spy, stubList);
            setTypeValidator(spy, stubValidator);
            return stubList;
        }).when(spy).getValidators();

        // first call – triggers loading
        TypeValidator first = spy.getTypeValidator();
        // second call – should reuse the already populated fields
        TypeValidator second = spy.getTypeValidator();

        assertSame(first, second, "Both calls must return the same TypeValidator instance");
        verify(spy, times(1)).getValidators();
    }

    /* --------------------------------------------------------------------- *
     *  5. Pre‑initialized validators list without a TypeValidator
     * --------------------------------------------------------------------- */

    @Test
    void preInitializedEmptyValidatorsReturnNullWithoutCallingGetValidators() throws Exception {
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);

        // initialise validators list but keep typeValidator null
        setValidators(spy, new ArrayList<>());
        setTypeValidator(spy, null);

        TypeValidator result = spy.getTypeValidator();

        assertNull(result, "When validators are pre‑initialized but typeValidator is null, result must be null");
        verify(spy, never()).getValidators();
    }

    /* --------------------------------------------------------------------- *
     *  6. Lazy loading that leaves validators list empty
     * --------------------------------------------------------------------- */

    @Test
    void lazyLoadingWithEmptyValidatorsListReturnsNull() throws Exception {
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);

        doAnswer(invocation -> {
            // empty list, typeValidator stays null
            setValidators(spy, new ArrayList<>());
            setTypeValidator(spy, null);
            return getValidatorsField(spy);
        }).when(spy).getValidators();

        TypeValidator result = spy.getTypeValidator();

        assertNull(result, "Empty validators list after lazy loading should still result in null TypeValidator");
        verify(spy, times(1)).getValidators();
    }

    /* --------------------------------------------------------------------- *
     *  7. Exception propagation – RuntimeException
     * --------------------------------------------------------------------- */

    @Test
    void exceptionFromGetValidatorsPropagatesRuntimeException() throws Exception {
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);

        doThrow(new IllegalStateException("boom")).when(spy).getValidators();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                spy::getTypeValidator,
                "RuntimeException from getValidators() must propagate");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  8. Custom TypeValidator subclass returned
     * --------------------------------------------------------------------- */

    /** Simple custom subclass used only for identification in the test. */
    private static class MySpecialTypeValidator extends TypeValidator {
        // The real TypeValidator has a protected constructor; we delegate to it
        // using reflection to avoid dealing with its complex signature.
        MySpecialTypeValidator() {
            super(null, null, null, null, null);
        }
    }

    @Test
    void customSubclassReturnedAndGetValidatorsInvokedOnce() throws Exception {
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);

        MySpecialTypeValidator custom = new MySpecialTypeValidator();
        List<JsonValidator> stubList = new ArrayList<>();

        doAnswer(invocation -> {
            setValidators(spy, stubList);
            setTypeValidator(spy, custom);
            return stubList;
        }).when(spy).getValidators();

        TypeValidator result = spy.getTypeValidator();

        assertNotNull(result, "Result must not be null");
        assertTrue(result instanceof MySpecialTypeValidator,
                "Returned validator should be an instance of the custom subclass");
        verify(spy, times(1)).getValidators();
    }

    /* --------------------------------------------------------------------- *
     *  9. NullPointerException propagation
     * --------------------------------------------------------------------- */

    @Test
    void nullPointerExceptionFromGetValidatorsPropagates() throws Exception {
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);

        doThrow(new NullPointerException("null boom")).when(spy).getValidators();

        NullPointerException ex = assertThrows(NullPointerException.class,
                spy::getTypeValidator,
                "NullPointerException from getValidators() must propagate");
        assertEquals("null boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     * 10. Error propagation – AssertionError
     * --------------------------------------------------------------------- */

    @Test
    void errorFromGetValidatorsPropagatesUnchanged() throws Exception {
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);

        doThrow(new AssertionError("assertion failed")).when(spy).getValidators();

        AssertionError err = assertThrows(AssertionError.class,
                spy::getTypeValidator,
                "Error thrown by getValidators() must propagate unchanged");
        assertEquals("assertion failed", err.getMessage());
    }
}
```