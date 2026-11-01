package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_preloadJsonSchema_Test {

    /**
     * Mocks a {@link ValidationContext} that creates mock {@link JsonSchema}s.
     */
    @Mock
    ValidationContext validationContext;

    /**
     * Two mock schemas that will be returned by the mocked {@code ValidationContext}.
     */
    @Mock
    JsonSchema mockSchema1;

    @Mock
    JsonSchema mockSchema2;

    /**
     * The validator under test (a Mockito spy so we can verify protected calls).
     */
    OneOfValidator validatorSpy;

    /**
     * Common objects used to build the {@link OneOfValidator} instance.
     */
    SchemaLocation schemaLocation;

    JsonNodePath evaluationPath;

    // an array node containing two empty objects
    JsonNode schemaNode;

    /**
     * Two generic mock schemas – reused in many tests.
     */
    @Mock
    JsonSchema mockSchemaA;

    @Mock
    JsonSchema mockSchemaB;

    // not used for the logic we test
    JsonNode dummySchemaNode;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----- 1. Prepare the JSON array node that represents the "oneOf" value -----
        ObjectMapper mapper = new ObjectMapper();
        // "[{}, {}]" → an array with two empty objects
        schemaNode = mapper.readTree("[{}, {}]");
        // ----- 2. Build the required location/path objects (they are simple value objects) -----
        // AbsoluteIri can be null for the purpose of this test
        schemaLocation = new SchemaLocation(null, new JsonNodePath(JsonNodePath.PathType.JSON_POINTER));
        evaluationPath = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
        // validationContext.newSchema(...). We make it return our mocks.
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenReturn(// for the first element
        mockSchema1).thenReturn(// for the second element
        mockSchema2);
        // ----- 4. Create a real OneOfValidator instance and then spy it -----
        OneOfValidator realValidator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, // parentSchema – not needed for this test
        null, validationContext);
        validatorSpy = spy(realValidator);
        // ----- 3. Stub the ValidationContext to return the two mock schemas in order -----
        // The constructor of OneOfValidator iterates over the array and calls
        // Build a minimal JSON node – the content is irrelevant for preloadJsonSchema().
        ObjectMapper mapper = new ObjectMapper();
        dummySchemaNode = mapper.readTree("[{}, {}]");
        // Simple location / path objects (they are value objects).
        schemaLocation = new SchemaLocation(null, new JsonNodePath(JsonNodePath.PathType.JSON_POINTER));
        evaluationPath = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
        // We will replace that list later with the exact collection we want to test.
        OneOfValidator realValidator = new OneOfValidator(schemaLocation, evaluationPath, dummySchemaNode, null, validationContext);
        validatorSpy = spy(realValidator);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        reset(validationContext, mockSchema1, mockSchema2, validatorSpy);
        // Reset all mocks to keep tests isolated.
        reset(validationContext, mockSchemaA, mockSchemaB, validatorSpy);
    }

    /**
     * Helper to set the private {@code schemas} field via reflection.
     */
    private void setSchemasField(List<JsonSchema> schemas) throws Exception {
        Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validatorSpy, schemas);
    }

    @Test
    void preloadJsonSchema_initializesAllSchemasAndCallsCanShortCircuit() {
        // WHEN: the method under test is invoked
        validatorSpy.preloadJsonSchema();
        // THEN: each mock schema must have its initializeValidators() method called exactly once
        verify(mockSchema1, times(1)).initializeValidators();
        verify(mockSchema2, times(1)).initializeValidators();
        // AND: the protected canShortCircuit() method must be invoked.
        // Because we are using a spy, Mockito can verify protected method calls.
        verify(validatorSpy, times(1)).canShortCircuit();
    }

    @Test
    @DisplayName("preloadJsonSchema – normal operation with two schemas")
    void preloadJsonSchema_twoSchemas() throws Exception {
        // GIVEN: a list containing two distinct mock schemas
        setSchemasField(Arrays.asList(mockSchemaA, mockSchemaB));
        // WHEN
        validatorSpy.preloadJsonSchema();
        // THEN: each schema must be initialized exactly once and canShortCircuit() called once
        verify(mockSchemaA, times(1)).initializeValidators();
        verify(mockSchemaB, times(1)).initializeValidators();
        verify(validatorSpy, times(1)).canShortCircuit();
    }

    @Test
    @DisplayName("preloadJsonSchema – normal operation with three schemas")
    void preloadJsonSchema_threeSchemas() throws Exception {
        // GIVEN: three mock schemas
        JsonSchema mockSchemaC = mock(JsonSchema.class);
        setSchemasField(Arrays.asList(mockSchemaA, mockSchemaB, mockSchemaC));
        // WHEN
        validatorSpy.preloadJsonSchema();
        // THEN: each of the three schemas is initialized once; canShortCircuit() once
        verify(mockSchemaA, times(1)).initializeValidators();
        verify(mockSchemaB, times(1)).initializeValidators();
        verify(mockSchemaC, times(1)).initializeValidators();
        verify(validatorSpy, times(1)).canShortCircuit();
    }

    @Test
    @DisplayName("preloadJsonSchema – empty schemas list")
    void preloadJsonSchema_emptyList() throws Exception {
        setSchemasField(Collections.emptyList());
        // WHEN / THEN: method completes without exception and only canShortCircuit() runs
        assertDoesNotThrow(() -> validatorSpy.preloadJsonSchema());
        verify(validatorSpy, times(1)).canShortCircuit();
        // No interaction with any schema (there are none)
        verifyNoInteractions(mockSchemaA, mockSchemaB);
    }

    @Test
    @DisplayName("preloadJsonSchema – null schemas list")
    void preloadJsonSchema_nullList() throws Exception {
        // Directly set the field to null
        setSchemasField(null);
        // WHEN / THEN: a NullPointerException is expected because the for‑each loop dereferences null
        assertThrows(NullPointerException.class, () -> validatorSpy.preloadJsonSchema());
        // canShortCircuit() must never be invoked
        verify(validatorSpy, never()).canShortCircuit();
    }

    @Test
    @DisplayName("preloadJsonSchema – initializeValidators throws RuntimeException")
    void preloadJsonSchema_schemaInitializeThrows() throws Exception {
        // First schema throws, second should never be reached
        doThrow(new RuntimeException("init failure")).when(mockSchemaA).initializeValidators();
        setSchemasField(Arrays.asList(mockSchemaA, mockSchemaB));
        // WHEN / THEN: the exception propagates and canShortCircuit() is not called
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validatorSpy.preloadJsonSchema());
        assertEquals("init failure", ex.getMessage());
        verify(mockSchemaA, times(1)).initializeValidators();
        // loop stops after exception
        verify(mockSchemaB, never()).initializeValidators();
        verify(validatorSpy, never()).canShortCircuit();
    }

    @Test
    @DisplayName("preloadJsonSchema – duplicate schema instances")
    void preloadJsonSchema_duplicateSchemas() throws Exception {
        // Same mock appears twice
        setSchemasField(Arrays.asList(mockSchemaA, mockSchemaA));
        validatorSpy.preloadJsonSchema();
        // initializeValidators should be called twice (once per list entry)
        verify(mockSchemaA, times(2)).initializeValidators();
        verify(validatorSpy, times(1)).canShortCircuit();
    }

    @Test
    @DisplayName("preloadJsonSchema – list contains a null element")
    void preloadJsonSchema_nullElementInList() throws Exception {
        setSchemasField(Arrays.asList(mockSchemaA, null, mockSchemaB));
        // Expect NPE when the loop reaches the null entry
        NullPointerException npe = assertThrows(NullPointerException.class, () -> validatorSpy.preloadJsonSchema());
        // The first schema should have been initialized before the failure
        verify(mockSchemaA, times(1)).initializeValidators();
        // The second (null) causes the NPE, so the third is never touched
        verify(mockSchemaB, never()).initializeValidators();
        verify(validatorSpy, never()).canShortCircuit();
    }

    @Test
    @DisplayName("preloadJsonSchema – canShortCircuit throws RuntimeException")
    void preloadJsonSchema_canShortCircuitThrows() throws Exception {
        setSchemasField(Arrays.asList(mockSchemaA, mockSchemaB));
        // Force the protected method to throw
        doThrow(new RuntimeException("short‑circuit failure")).when(validatorSpy).canShortCircuit();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validatorSpy.preloadJsonSchema());
        assertEquals("short‑circuit failure", ex.getMessage());
        // Schemas must have been initialized before the exception
        verify(mockSchemaA, times(1)).initializeValidators();
        verify(mockSchemaB, times(1)).initializeValidators();
        // canShortCircuit was invoked and threw
        verify(validatorSpy, times(1)).canShortCircuit();
    }

    @Test
    @DisplayName("preloadJsonSchema – ConcurrentModificationException during iteration")
    void preloadJsonSchema_concurrentModification() throws Exception {
        // Custom list whose iterator throws CME on second call to next()
        List<JsonSchema> problematicList = new AbstractList<JsonSchema>() {

            private final List<JsonSchema> backing = new ArrayList<>(Arrays.asList(mockSchemaA, mockSchemaB));

            @Override
            public JsonSchema get(int index) {
                return backing.get(index);
            }

            @Override
            public int size() {
                return backing.size();
            }

            @Override
            public Iterator<JsonSchema> iterator() {
                Iterator<JsonSchema> it = backing.iterator();
                return new Iterator<JsonSchema>() {

                    int count = 0;

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public JsonSchema next() {
                        if (count++ == 1) {
                            throw new ConcurrentModificationException("simulated");
                        }
                        return it.next();
                    }
                };
            }
        };
        setSchemasField(problematicList);
        // The CME should propagate; canShortCircuit must not be called
        assertThrows(ConcurrentModificationException.class, () -> validatorSpy.preloadJsonSchema());
        // First schema initialized, second triggers the exception
        verify(mockSchemaA, times(1)).initializeValidators();
        verify(mockSchemaB, never()).initializeValidators();
        verify(validatorSpy, never()).canShortCircuit();
    }

    @Test
    @DisplayName("preloadJsonSchema – iterator() throws IllegalStateException")
    void preloadJsonSchema_iteratorThrowsIllegalState() throws Exception {
        // Mock a List whose iterator() throws immediately
        @SuppressWarnings("unchecked")
        List<JsonSchema> badList = mock(List.class);
        when(badList.iterator()).thenThrow(new IllegalStateException("iterator failure"));
        setSchemasField(badList);
        IllegalStateException ise = assertThrows(IllegalStateException.class, () -> validatorSpy.preloadJsonSchema());
        assertEquals("iterator failure", ise.getMessage());
        // No schema interaction and canShortCircuit() never called
        verifyNoInteractions(mockSchemaA, mockSchemaB);
        verify(validatorSpy, never()).canShortCircuit();
    }
}
