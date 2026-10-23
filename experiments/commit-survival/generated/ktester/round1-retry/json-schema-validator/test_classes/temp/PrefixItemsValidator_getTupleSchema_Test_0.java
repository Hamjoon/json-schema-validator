package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class PrefixItemsValidator_getTupleSchema_Test {

    /**
     * Mocks and test data **
     */
    @Mock
    ValidationContext // mocked ValidationContext
    validationContext;

    @Mock
    SchemaValidatorsConfig // config returned by validationContext.getConfig()
    config;

    // instance under test
    PrefixItemsValidator validator;

    // the JSON schema node passed to the constructor
    ArrayNode schemaArrayNode;

    // schemas that the validator should store
    List<JsonSchema> expectedTupleSchemas;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // 1. Prepare a non‑empty ArrayNode that mimics a valid "prefixItems"
        //    declaration – e.g. [{}, {}].
        // -----------------------------------------------------------------
        ObjectMapper mapper = new ObjectMapper();
        schemaArrayNode = mapper.createArrayNode();
        // empty schema object
        ObjectNode first = mapper.createObjectNode();
        // another empty schema object
        ObjectNode second = mapper.createObjectNode();
        schemaArrayNode.add(first);
        schemaArrayNode.add(second);
        // -----------------------------------------------------------------
        // 2. Configure the ValidationContext mock:
        //    - getConfig() must return a config whose getArrayItemWalkListeners()
        //      returns an empty list (the constructor reads this value).
        //    - newSchema(...) should create a fresh mock JsonSchema for each call
        //      and collect them so we can assert against the validator's state.
        // -----------------------------------------------------------------
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
        expectedTupleSchemas = new ArrayList<>();
        // each invocation of newSchema returns a new mock and records it
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(invocation -> {
            JsonSchema mockSchema = mock(JsonSchema.class);
            expectedTupleSchemas.add(mockSchema);
            return mockSchema;
        });
        // -----------------------------------------------------------------
        // 3. Build the PrefixItemsValidator instance.
        //    The parentSchema argument can be null for this unit test.
        // -----------------------------------------------------------------
        validator = new PrefixItemsValidator("testSchemaPath", schemaArrayNode, // parentSchema
        null, // mocked context
        validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(validationContext, config);
    }

    /**
     * Helper to set the private field `tupleSchema` via reflection **
     */
    private void setTupleSchema(List<JsonSchema> list) throws Exception {
        Field f = PrefixItemsValidator.class.getDeclaredField("tupleSchema");
        f.setAccessible(true);
        f.set(validator, list);
    }

    @Test
    void getTupleSchema_returnsListPopulatedFromSchemaNode() {
        // Act: retrieve the tuple schema list from the validator.
        List<JsonSchema> actual = validator.getTupleSchema();
        // Assert: the list size matches the number of elements in the input array.
        assertEquals(2, actual.size(), "Tuple schema list should contain two entries.");
        // Assert: the list returned is exactly the mocks produced by ValidationContext.newSchema().
        assertEquals(expectedTupleSchemas, actual, "The tuple schema list should contain the JsonSchema instances created by ValidationContext.");
    }

    @Test
    void constructor_throwsExceptionWhenSchemaNodeIsEmptyArray() {
        // Arrange: an empty ArrayNode
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode emptyArray = mapper.createArrayNode();
        // Act & Assert: IllegalArgumentException is expected.
        assertThrows(IllegalArgumentException.class, () -> {
            new PrefixItemsValidator("emptyPath", emptyArray, null, validationContext);
        });
    }

    @Test
    @DisplayName("Empty tuple schema – getTupleSchema() returns an empty, non‑null list")
    void emptyTupleSchema_returnsEmptyList() throws Exception {
        // Build a validator with an empty array node.
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode emptyArray = mapper.createArrayNode();
        // No schemas will be created, so we don't need to stub newSchema for this case.
        PrefixItemsValidator emptyValidator = new PrefixItemsValidator("emptyPath", emptyArray, null, validationContext);
        List<JsonSchema> result = emptyValidator.getTupleSchema();
        assertNotNull(result, "Result should never be null.");
        assertTrue(result.isEmpty(), "Result list should be empty for an empty tuple schema.");
    }

    @Test
    @DisplayName("Single element tuple schema – list contains exactly the provided JsonSchema mock")
    void singleElementTupleSchema_returnsSingleMock() throws Exception {
        // Prepare a schema array with a single element.
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode singleArray = mapper.createArrayNode();
        singleArray.add(mapper.createObjectNode());
        // Stub newSchema to return a specific mock.
        JsonSchema mockSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockSchema);
        PrefixItemsValidator singleValidator = new PrefixItemsValidator("singlePath", singleArray, null, validationContext);
        List<JsonSchema> result = singleValidator.getTupleSchema();
        assertNotNull(result);
        assertEquals(1, result.size(), "List should contain exactly one element.");
        assertSame(mockSchema, result.get(0), "The element should be the mock supplied by ValidationContext.");
    }

    @Test
    @DisplayName("Multiple elements tuple schema – list preserves order of three supplied mocks")
    void multipleElementsTupleSchema_preservesOrder() throws Exception {
        // Build an array node with three entries.
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode threeArray = mapper.createArrayNode();
        threeArray.add(mapper.createObjectNode());
        threeArray.add(mapper.createObjectNode());
        threeArray.add(mapper.createObjectNode());
        // Prepare three distinct mocks.
        JsonSchema mock1 = mock(JsonSchema.class);
        JsonSchema mock2 = mock(JsonSchema.class);
        JsonSchema mock3 = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mock1, mock2, mock3);
        PrefixItemsValidator threeValidator = new PrefixItemsValidator("threePath", threeArray, null, validationContext);
        List<JsonSchema> result = threeValidator.getTupleSchema();
        assertEquals(3, result.size(), "List should contain three elements.");
        assertSame(mock1, result.get(0));
        assertSame(mock2, result.get(1));
        assertSame(mock3, result.get(2));
    }

    @Test
    @DisplayName("Mutable reference test – adding to returned list mutates internal state")
    void mutableReference_addElementReflectsInternalState() {
        List<JsonSchema> list = validator.getTupleSchema();
        int originalSize = list.size();
        JsonSchema extra = mock(JsonSchema.class);
        // mutate the returned list
        list.add(extra);
        // The internal list should have grown as well.
        assertEquals(originalSize + 1, validator.getTupleSchema().size(), "Internal tupleSchema should reflect the added element.");
        assertTrue(validator.getTupleSchema().contains(extra), "Added element must be present internally.");
    }

    @Test
    @DisplayName("Null tuple schema handling – getTupleSchema() returns null when field is null")
    void nullTupleSchema_viaReflection_returnsNull() throws Exception {
        // Force the private field to null.
        setTupleSchema(null);
        assertNull(validator.getTupleSchema(), "When the internal tupleSchema field is null, getTupleSchema() should return null.");
    }

    @Test
    @DisplayName("Original list mutation effect – validator holds a live reference to supplied list")
    void originalListMutationEffect_reflectedInGetter() throws Exception {
        // Create a mutable list and inject it via reflection.
        List<JsonSchema> externalList = new ArrayList<>();
        JsonSchema first = mock(JsonSchema.class);
        externalList.add(first);
        setTupleSchema(externalList);
        // Verify initial state.
        assertEquals(1, validator.getTupleSchema().size());
        // Mutate the original list after construction.
        JsonSchema second = mock(JsonSchema.class);
        externalList.add(second);
        // The validator should see the change.
        List<JsonSchema> after = validator.getTupleSchema();
        assertEquals(2, after.size(), "Validator should reflect mutation of the original list.");
        assertSame(second, after.get(1), "The newly added element must be present in the returned list.");
    }

    @Test
    @DisplayName("Reference equality across calls – same List instance returned")
    void referenceEquality_multipleCallsReturnSameInstance() {
        List<JsonSchema> firstCall = validator.getTupleSchema();
        List<JsonSchema> secondCall = validator.getTupleSchema();
        assertSame(firstCall, secondCall, "Two successive calls must return the exact same List instance.");
    }

    @Test
    @DisplayName("Immutable list behavior – modifying returned list throws UnsupportedOperationException")
    void immutableList_behaviorThrowsException() throws Exception {
        // Inject an unmodifiable list.
        List<JsonSchema> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        setTupleSchema(unmodifiable);
        List<JsonSchema> returned = validator.getTupleSchema();
        assertThrows(UnsupportedOperationException.class, () -> returned.add(mock(JsonSchema.class)), "Attempting to modify an unmodifiable list should throw UnsupportedOperationException.");
    }

    @Test
    @DisplayName("List containing null element – getTupleSchema() returns list with null entry")
    void listContainingNullElement_returnsListWithNull() throws Exception {
        List<JsonSchema> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        setTupleSchema(listWithNull);
        List<JsonSchema> result = validator.getTupleSchema();
        assertEquals(1, result.size(), "List size should be 1.");
        assertNull(result.get(0), "The sole element should be null.");
    }

    @Test
    @DisplayName("List with duplicate schemas – duplicates are preserved in order")
    void listWithDuplicateSchemas_preservesDuplicates() throws Exception {
        JsonSchema dup = mock(JsonSchema.class);
        List<JsonSchema> dupList = new ArrayList<>(Arrays.asList(dup, dup, dup));
        setTupleSchema(dupList);
        List<JsonSchema> result = validator.getTupleSchema();
        assertEquals(3, result.size(), "List size should reflect all duplicates.");
        assertSame(dup, result.get(0));
        assertSame(dup, result.get(1));
        assertSame(dup, result.get(2));
    }
}
