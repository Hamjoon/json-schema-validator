package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    /**
     * Shared factory used to build {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    /**
     * Simple custom subclass used only for identification in the test.
     */
    private static class MySpecialTypeValidator extends TypeValidator {

        // The real TypeValidator has a protected constructor; we delegate to it
        // using reflection to avoid dealing with its complex signature.
        MySpecialTypeValidator() {
            super(null, null, null, null, null);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use a concrete Draft‑07 factory – any supported version works for the
        // purpose of these tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
    }

    /**
     * Sets the private {@code validators} field on the supplied schema.
     */
    private static void setValidators(JsonSchema schema, List<JsonValidator> validators) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        f.set(schema, validators);
    }

    /**
     * Sets the private {@code typeValidator} field on the supplied schema.
     */
    private static void setTypeValidator(JsonSchema schema, TypeValidator typeValidator) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("typeValidator");
        f.setAccessible(true);
        f.set(schema, typeValidator);
    }

    /**
     * Retrieves the current value of the private {@code typeValidator} field.
     */
    private static TypeValidator getTypeValidatorField(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("typeValidator");
        f.setAccessible(true);
        return (TypeValidator) f.get(schema);
    }

    /**
     * When the schema contains a {@code "type"} keyword the {@link JsonSchema}
     * should create a {@link TypeValidator} and {@code getTypeValidator()}
     * must return a non‑null instance.
     */
    @Test
    void getTypeValidatorReturnsNonNullWhenTypeKeywordPresent() {
        // a simple schema that defines a type
        String schemaJson = "{\"type\":\"string\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        // invoke the method under test
        TypeValidator typeValidator = schema.getTypeValidator();
        // verify that a TypeValidator was created
        assertNotNull(typeValidator, "TypeValidator should be non‑null for schema with a 'type' keyword");
    }

    /**
     * When the schema does **not** contain a {@code "type"} keyword the
     * {@link JsonSchema} must not create a {@link TypeValidator} and
     * {@code getTypeValidator()} should return {@code null}.
     */
    @Test
    void getTypeValidatorReturnsNullWhenNoTypeKeyword() {
        // a schema without a "type" keyword (only a "minimum" constraint)
        String schemaJson = "{\"minimum\":5}";
        JsonSchema schema = factory.getSchema(schemaJson);
        // invoke the method under test
        TypeValidator typeValidator = schema.getTypeValidator();
        // verify that no TypeValidator exists
        assertNull(typeValidator, "TypeValidator should be null for schema without a 'type' keyword");
    }

    /**
     * Retrieves the current value of the private {@code validators} field.
     */
    @SuppressWarnings("unchecked")
    private static List<JsonValidator> getValidatorsField(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        return (List<JsonValidator>) f.get(schema);
    }

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

    @Test
    void exceptionFromGetValidatorsPropagatesRuntimeException() throws Exception {
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);
        doThrow(new IllegalStateException("boom")).when(spy).getValidators();
        IllegalStateException ex = assertThrows(IllegalStateException.class, spy::getTypeValidator, "RuntimeException from getValidators() must propagate");
        assertEquals("boom", ex.getMessage());
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
        assertTrue(result instanceof MySpecialTypeValidator, "Returned validator should be an instance of the custom subclass");
        verify(spy, times(1)).getValidators();
    }

    @Test
    void nullPointerExceptionFromGetValidatorsPropagates() throws Exception {
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);
        doThrow(new NullPointerException("null boom")).when(spy).getValidators();
        NullPointerException ex = assertThrows(NullPointerException.class, spy::getTypeValidator, "NullPointerException from getValidators() must propagate");
        assertEquals("null boom", ex.getMessage());
    }

    @Test
    void errorFromGetValidatorsPropagatesUnchanged() throws Exception {
        JsonSchema real = factory.getSchema("{}");
        JsonSchema spy = Mockito.spy(real);
        doThrow(new AssertionError("assertion failed")).when(spy).getValidators();
        AssertionError err = assertThrows(AssertionError.class, spy::getTypeValidator, "Error thrown by getValidators() must propagate unchanged");
        assertEquals("assertion failed", err.getMessage());
    }
}
