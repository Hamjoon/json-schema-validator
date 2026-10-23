package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class PropertiesValidator_preloadJsonSchema_Test {

    /**
     * Shared Jackson mapper for creating {@link JsonNode}s from JSON strings.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Mocked validation context supplied to the {@link PropertiesValidator} constructor.
     */
    @Mock
    ValidationContext validationContext;

    /**
     * Holds the {@link JsonSchema} mocks that are created by the {@code newSchema} stub.
     */
    private final List<JsonSchema> createdSchemas = new ArrayList<>();

    /**
     * The field reference for the private {@code schemas} map.
     */
    private static Field schemasField;

    @BeforeAll
    static void initReflection() throws Exception {
        schemasField = PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub ValidationContext#newSchema to return a fresh mock JsonSchema for each call
        // and keep a reference to the mock for later verification.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(invocation -> {
            JsonSchema mockSchema = mock(JsonSchema.class);
            createdSchemas.add(mockSchema);
            return mockSchema;
        });
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the list of created schema mocks for the next test method.
        createdSchemas.clear();
        // Clear interactions on the mocked ValidationContext.
        clearInvocations(validationContext);
    }

    /**
     * Helper to create a fresh {@link PropertiesValidator} instance with a minimal
     * schema node (empty object) and then replace its internal {@code schemas}
     * map with the supplied one.
     */
    private PropertiesValidator createValidatorWithMap(Map<String, JsonSchema> map) throws Exception {
        PropertiesValidator validator = new PropertiesValidator("testSchema", MAPPER.createObjectNode(), null, validationContext);
        schemasField.set(validator, map);
        return validator;
    }

    /**
     * Captures the argument passed to {@code preloadJsonSchemas} on a spy.
     */
    private Collection<JsonSchema> capturePreloadArgument(PropertiesValidator spy) {
        ArgumentCaptor<Collection<JsonSchema>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(spy, times(1)).preloadJsonSchemas(captor.capture());
        return captor.getValue();
    }

    @Test
    void testPreloadJsonSchemaInvokesPreloadOnEachSchema() throws Exception {
        // GIVEN – a simple schema node containing two properties
        String schemaJson = """
            {
              "type": "object",
              "properties": {
                "first": { "type": "string" },
                "second": { "type": "integer" }
              }
            }
            """;
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        // WHEN – constructing the validator (the constructor populates the internal map)
        PropertiesValidator validator = new PropertiesValidator(// schemaPath
        "testSchema", // schemaNode
        schemaNode, // parentSchema
        null, // mocked ValidationContext
        validationContext);
        // Sanity check – the internal map should contain exactly the two mocks we created.
        assertEquals(2, validator.getSchemas().size(), "Validator should have created a JsonSchema mock for each property");
        // AND – invoking preloadJsonSchema()
        validator.preloadJsonSchema();
        // THEN – each created JsonSchema mock must have received a single call to preloadJsonSchema()
        for (JsonSchema mockSchema : createdSchemas) {
            verify(mockSchema, times(1)).preloadJsonSchema();
        }
        // Also verify that no extra interactions occurred on the mocks.
        verifyNoMoreInteractions(createdSchemas.toArray());
    }

    @Test
    @DisplayName("Empty schemas map – should invoke preloadJsonSchemas with empty collection")
    void emptySchemasMap() throws Exception {
        // GIVEN – an empty LinkedHashMap as the internal schemas map
        PropertiesValidator validator = createValidatorWithMap(new LinkedHashMap<>());
        // Spy the validator to intercept the protected method
        PropertiesValidator spy = Mockito.spy(validator);
        doNothing().when(spy).preloadJsonSchemas(anyCollection());
        // WHEN
        spy.preloadJsonSchema();
        // THEN – the captured collection must be empty and no exception is thrown
        Collection<JsonSchema> captured = capturePreloadArgument(spy);
        assertTrue(captured.isEmpty(), "Expected an empty collection to be passed");
    }

    @Test
    @DisplayName("Single schema entry – collection contains exactly that schema")
    void singleSchemaEntry() throws Exception {
        JsonSchema schema = mock(JsonSchema.class);
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("only", schema);
        PropertiesValidator validator = createValidatorWithMap(map);
        PropertiesValidator spy = Mockito.spy(validator);
        doNothing().when(spy).preloadJsonSchemas(anyCollection());
        spy.preloadJsonSchema();
        Collection<JsonSchema> captured = capturePreloadArgument(spy);
        assertEquals(1, captured.size(), "Exactly one schema should be forwarded");
        assertTrue(captured.contains(schema), "The forwarded collection must contain the inserted schema");
    }

    @Test
    @DisplayName("Multiple schema entries – preserves insertion order")
    void multipleSchemaEntriesPreserveOrder() throws Exception {
        JsonSchema a = mock(JsonSchema.class);
        JsonSchema b = mock(JsonSchema.class);
        JsonSchema c = mock(JsonSchema.class);
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("a", a);
        map.put("b", b);
        map.put("c", c);
        PropertiesValidator validator = createValidatorWithMap(map);
        PropertiesValidator spy = Mockito.spy(validator);
        doNothing().when(spy).preloadJsonSchemas(anyCollection());
        spy.preloadJsonSchema();
        Collection<JsonSchema> captured = capturePreloadArgument(spy);
        assertEquals(Arrays.asList(a, b, c), new ArrayList<>(captured), "The order of schemas must match the LinkedHashMap insertion order");
    }

    @Test
    @DisplayName("Null schema value in map – propagates NullPointerException")
    void nullSchemaValuePropagatesNPE() throws Exception {
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("nullKey", null);
        PropertiesValidator validator = createValidatorWithMap(map);
        // Do NOT spy – we want the real implementation to run and hit the NPE
        assertThrows(NullPointerException.class, validator::preloadJsonSchema, "Expected NullPointerException when a null schema is present");
    }

    @Test
    @DisplayName("Exception propagation from a schema during preload")
    void exceptionFromSchemaPropagates() throws Exception {
        JsonSchema badSchema = mock(JsonSchema.class);
        doThrow(new RuntimeException("boom")).when(badSchema).preloadJsonSchema();
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("bad", badSchema);
        PropertiesValidator validator = createValidatorWithMap(map);
        // Real method should propagate the RuntimeException
        RuntimeException ex = assertThrows(RuntimeException.class, validator::preloadJsonSchema);
        assertEquals("boom", ex.getMessage());
    }

    @Test
    @DisplayName("Repeated invocation does not alter internal state")
    void repeatedInvocationKeepsState() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("one", s1);
        map.put("two", s2);
        PropertiesValidator validator = createValidatorWithMap(map);
        PropertiesValidator spy = Mockito.spy(validator);
        doNothing().when(spy).preloadJsonSchemas(anyCollection());
        // First call
        spy.preloadJsonSchema();
        // Second call
        spy.preloadJsonSchema();
        // Capture both invocations
        ArgumentCaptor<Collection<JsonSchema>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(spy, times(2)).preloadJsonSchemas(captor.capture());
        List<Collection<JsonSchema>> allArgs = captor.getAllValues();
        assertEquals(2, allArgs.size());
        for (Collection<JsonSchema> arg : allArgs) {
            assertEquals(Arrays.asList(s1, s2), new ArrayList<>(arg), "Each call must receive the same collection in the same order");
        }
        // Verify map unchanged
        assertEquals(2, validator.getSchemas().size(), "Internal map size must remain unchanged after repeated calls");
    }

    @Test
    @DisplayName("Order preservation with mixed insertion patterns")
    void mixedInsertionOrderPreserved() throws Exception {
        JsonSchema a = mock(JsonSchema.class);
        JsonSchema b = mock(JsonSchema.class);
        JsonSchema c = mock(JsonSchema.class);
        JsonSchema d = mock(JsonSchema.class);
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("A", a);
        map.put("B", b);
        map.put("C", c);
        // remove B
        map.remove("B");
        // re‑insert under same key
        map.put("B", d);
        PropertiesValidator validator = createValidatorWithMap(map);
        PropertiesValidator spy = Mockito.spy(validator);
        doNothing().when(spy).preloadJsonSchemas(anyCollection());
        spy.preloadJsonSchema();
        Collection<JsonSchema> captured = capturePreloadArgument(spy);
        // Expected order: A, C, B (where B now refers to d)
        assertEquals(Arrays.asList(a, c, d), new ArrayList<>(captured), "Insertion order after removal/re‑insertion must be A, C, D");
    }

    @Test
    @DisplayName("Null collection handling after map mutation – snapshot semantics")
    void collectionSnapshotAfterMutation() throws Exception {
        JsonSchema only = mock(JsonSchema.class);
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("only", only);
        PropertiesValidator validator = createValidatorWithMap(map);
        PropertiesValidator spy = Mockito.spy(validator);
        // Capture the collection passed to preloadJsonSchemas
        doAnswer(invocation -> {
            Collection<JsonSchema> passed = invocation.getArgument(0);
            // Mutate the map after the method receives the collection
            map.clear();
            // Verify that the passed collection still contains the original element
            assertTrue(passed.contains(only), "Snapshot collection must still contain the original schema");
            return null;
        }).when(spy).preloadJsonSchemas(anyCollection());
        spy.preloadJsonSchema();
    }

    @Test
    @DisplayName("Concurrent modification during preload – propagates CME")
    void concurrentModificationDuringPreload() throws Exception {
        // Schema that mutates the validator's map when its preloadJsonSchema is called
        JsonSchema mutating = mock(JsonSchema.class);
        // We'll need a reference to the validator inside the answer
        PropertiesValidator[] holder = new PropertiesValidator[1];
        doAnswer(invocation -> {
            // Mutate the map (add a new entry) while iteration is in progress
            holder[0].getSchemas().put("new", mock(JsonSchema.class));
            return null;
        }).when(mutating).preloadJsonSchema();
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("mut", mutating);
        PropertiesValidator validator = createValidatorWithMap(map);
        // give the answer access to the validator
        holder[0] = validator;
        // Expect ConcurrentModificationException from the real implementation
        assertThrows(ConcurrentModificationException.class, validator::preloadJsonSchema, "Modifying the map during iteration should cause CME");
    }

    @Test
    @DisplayName("Error propagation from schema – AssertionError bubbles up")
    void assertionErrorFromSchemaPropagates() throws Exception {
        JsonSchema bad = mock(JsonSchema.class);
        doThrow(new AssertionError("assertion failed")).when(bad).preloadJsonSchema();
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("bad", bad);
        PropertiesValidator validator = createValidatorWithMap(map);
        AssertionError err = assertThrows(AssertionError.class, validator::preloadJsonSchema);
        assertEquals("assertion failed", err.getMessage());
    }

    @Test
    @DisplayName("Null internal schemas map – NullPointerException")
    void nullInternalSchemasMap() throws Exception {
        PropertiesValidator validator = new PropertiesValidator("id", MAPPER.createObjectNode(), null, validationContext);
        // Force the private field to null
        schemasField.set(validator, null);
        assertThrows(NullPointerException.class, validator::preloadJsonSchema, "Calling preloadJsonSchema with a null internal map must throw NPE");
    }
}
