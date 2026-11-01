package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_preloadJsonSchema_Test {

    /**
     * Mocks used in the tests.
     */
    @Mock
    ValidationContext validationContext;

    /**
     * Two mock schemas that will be returned by the mocked {@link ValidationContext}.
     */
    @Mock
    JsonSchema childSchema1;

    @Mock
    JsonSchema childSchema2;

    /**
     * The validator under test.
     */
    OneOfValidator validator;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // The actual content of the child objects is irrelevant for this test.
        ObjectMapper mapper = new ObjectMapper();
        // two empty subschemas
        JsonNode schemaNode = mapper.readTree("[{}, {}]");
        // AbsoluteIri and PathType are part of the library; use simple values.
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com");
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri, new JsonNodePath(PathType.JSON_POINTER));
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        // validationContext.newSchema(...) for each element.
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenReturn(childSchema1, childSchema2);
        // ---- 4. Instantiate the validator ----------------------------------------
        validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, // parent schema – not needed here
        null, validationContext);
        // ---- 1. Prepare a minimal JSON schema node that represents a "oneOf" array ----
        // an empty list internally.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode emptyArray = mapper.readTree("[]");
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com");
        // ---- 2. Build the required constructor arguments -------------------------
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri, new JsonNodePath(PathType.JSON_POINTER));
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        // The constructor may call validationContext.newSchema – we stub it just in case.
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mock(JsonSchema.class));
        // ---- 3. Configure the ValidationContext mock to return our child schemas ----
        // The constructor of OneOfValidator iterates over the array and calls
        validator = new OneOfValidator(schemaLocation, evaluationPath, emptyArray, null, validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(validationContext, childSchema1, childSchema2);
        reset(validationContext);
    }

    /**
     * Helper to inject a custom {@code schemas} list via reflection.
     */
    private static void setSchemas(OneOfValidator v, List<JsonSchema> schemas) throws Exception {
        Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(v, schemas);
    }

    @Test
    void preloadJsonSchema_initializesAllChildSchemas_andCallsCanShortCircuit() {
        // Spy the validator so we can verify the protected method call.
        OneOfValidator spyValidator = spy(validator);
        // ---- 5. Execute the method under test ------------------------------------
        spyValidator.preloadJsonSchema();
        // ---- 6. Verify interactions -----------------------------------------------
        // Each child schema must have its initialise method invoked exactly once.
        verify(childSchema1, times(1)).initializeValidators();
        verify(childSchema2, times(1)).initializeValidators();
        // The protected helper canShortCircuit() must be called once.
        verify(spyValidator, times(1)).canShortCircuit();
        // No other unexpected interactions.
        verifyNoMoreInteractions(childSchema1, childSchema2, validationContext);
    }

    /* -------------------------------------------------------------
     *  1. Empty schemas list
     * ------------------------------------------------------------- */
    @Test
    void emptySchemasList() throws Exception {
        // inject an explicit empty list
        setSchemas(validator, Collections.emptyList());
        OneOfValidator spy = spy(validator);
        spy.preloadJsonSchema();
        // No child schema, therefore no initialise calls
        verify(spy, times(1)).canShortCircuit();
        // verifyNoMoreInteractions would also ensure no unexpected calls on validationContext
        verifyNoMoreInteractions(validationContext);
    }

    /* -------------------------------------------------------------
     *  2. Single schema
     * ------------------------------------------------------------- */
    @Test
    void singleSchema() throws Exception {
        JsonSchema child = mock(JsonSchema.class);
        setSchemas(validator, Collections.singletonList(child));
        OneOfValidator spy = spy(validator);
        spy.preloadJsonSchema();
        verify(child, times(1)).initializeValidators();
        verify(spy, times(1)).canShortCircuit();
    }

    /* -------------------------------------------------------------
     *  3. Multiple schemas
     * ------------------------------------------------------------- */
    @Test
    void multipleSchemas() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        JsonSchema s3 = mock(JsonSchema.class);
        setSchemas(validator, new ArrayList<>(List.of(s1, s2, s3)));
        OneOfValidator spy = spy(validator);
        spy.preloadJsonSchema();
        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
        verify(s3, times(1)).initializeValidators();
        verify(spy, times(1)).canShortCircuit();
    }

    /* -------------------------------------------------------------
     *  4. Schema initialization exception
     * ------------------------------------------------------------- */
    @Test
    void schemaInitializationException() throws Exception {
        JsonSchema good = mock(JsonSchema.class);
        JsonSchema bad = mock(JsonSchema.class);
        doThrow(new RuntimeException("boom")).when(bad).initializeValidators();
        setSchemas(validator, new ArrayList<>(List.of(good, bad)));
        OneOfValidator spy = spy(validator);
        RuntimeException ex = assertThrows(RuntimeException.class, spy::preloadJsonSchema);
        assertEquals("boom", ex.getMessage());
        // good schema should have been called, bad throws, canShortCircuit never reached
        verify(good, times(1)).initializeValidators();
        verify(bad, times(1)).initializeValidators();
        verify(spy, never()).canShortCircuit();
    }

    /* -------------------------------------------------------------
     *  5. Null schema element in list
     * ------------------------------------------------------------- */
    @Test
    void nullSchemaElementInList() throws Exception {
        JsonSchema good = mock(JsonSchema.class);
        setSchemas(validator, new ArrayList<>(List.of(good, null, good)));
        OneOfValidator spy = spy(validator);
        assertThrows(NullPointerException.class, spy::preloadJsonSchema);
        // first good may have been called before NPE; we don't assert order, just that canShortCircuit not called
        verify(spy, never()).canShortCircuit();
    }

    /* -------------------------------------------------------------
     *  6. canShortCircuit throws RuntimeException
     * ------------------------------------------------------------- */
    @Test
    void canShortCircuitThrowsRuntimeException() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        setSchemas(validator, new ArrayList<>(List.of(s1, s2)));
        OneOfValidator spy = spy(validator);
        doThrow(new RuntimeException("short-circuit failure")).when(spy).canShortCircuit();
        RuntimeException ex = assertThrows(RuntimeException.class, spy::preloadJsonSchema);
        assertEquals("short-circuit failure", ex.getMessage());
        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
        verify(spy, times(1)).canShortCircuit();
    }

    /* -------------------------------------------------------------
     *  7. Idempotent preloadJsonSchema calls
     * ------------------------------------------------------------- */
    @Test
    void idempotentPreloadCalls() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        setSchemas(validator, new ArrayList<>(List.of(s1, s2)));
        OneOfValidator spy = spy(validator);
        // first call
        spy.preloadJsonSchema();
        // second call
        spy.preloadJsonSchema();
        verify(s1, times(2)).initializeValidators();
        verify(s2, times(2)).initializeValidators();
        verify(spy, times(2)).canShortCircuit();
    }

    /* -------------------------------------------------------------
     *  8. Partial initialization when a middle schema throws
     * ------------------------------------------------------------- */
    @Test
    void partialInitializationMiddleThrows() throws Exception {
        JsonSchema first = mock(JsonSchema.class);
        JsonSchema second = mock(JsonSchema.class);
        JsonSchema third = mock(JsonSchema.class);
        doThrow(new RuntimeException("middle")).when(second).initializeValidators();
        setSchemas(validator, new ArrayList<>(List.of(first, second, third)));
        OneOfValidator spy = spy(validator);
        RuntimeException ex = assertThrows(RuntimeException.class, spy::preloadJsonSchema);
        assertEquals("middle", ex.getMessage());
        verify(first, times(1)).initializeValidators();
        verify(second, times(1)).initializeValidators();
        verify(third, never()).initializeValidators();
        verify(spy, never()).canShortCircuit();
    }

    /* -------------------------------------------------------------
     *  9. Null schemas list
     * ------------------------------------------------------------- */
    @Test
    void nullSchemasList() throws Exception {
        // force null reference
        setSchemas(validator, null);
        OneOfValidator spy = spy(validator);
        assertThrows(NullPointerException.class, spy::preloadJsonSchema);
        verify(spy, never()).canShortCircuit();
    }

    /* -------------------------------------------------------------
     * 10. Concurrent modification during iteration
     * ------------------------------------------------------------- */
    @Test
    void concurrentModificationDuringIteration() throws Exception {
        JsonSchema modifier = mock(JsonSchema.class);
        JsonSchema other = mock(JsonSchema.class);
        // The modifier will remove an element from the list while iterating
        doAnswer(invocation -> {
            List<JsonSchema> list = (List<JsonSchema>) OneOfValidator.class.getDeclaredField("schemas").get(validator);
            // trigger concurrent modification
            list.remove(other);
            return null;
        }).when(modifier).initializeValidators();
        List<JsonSchema> list = new ArrayList<>(List.of(modifier, other));
        setSchemas(validator, list);
        OneOfValidator spy = spy(validator);
        assertThrows(ConcurrentModificationException.class, spy::preloadJsonSchema);
        verify(spy, never()).canShortCircuit();
    }

    /* -------------------------------------------------------------
     * 11. canShortCircuit throws NullPointerException
     * ------------------------------------------------------------- */
    @Test
    void canShortCircuitThrowsNullPointerException() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        setSchemas(validator, new ArrayList<>(List.of(s1, s2)));
        OneOfValidator spy = spy(validator);
        doThrow(new NullPointerException("NPE in canShortCircuit")).when(spy).canShortCircuit();
        NullPointerException ex = assertThrows(NullPointerException.class, spy::preloadJsonSchema);
        assertEquals("NPE in canShortCircuit", ex.getMessage());
        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
        verify(spy, times(1)).canShortCircuit();
    }
}
