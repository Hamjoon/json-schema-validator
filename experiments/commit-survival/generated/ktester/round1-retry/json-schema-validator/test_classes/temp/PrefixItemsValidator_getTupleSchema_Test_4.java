package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.net.URI;

class PrefixItemsValidator_getTupleSchema_Test {

    //     @Mock
    ValidationContext // mocked ValidationContext
    validationContext;

    //     @Mock
    SchemaValidatorsConfig // config returned by validationContext.getConfig()
    config;

    // instance under test
    PrefixItemsValidator validator;

    // the JSON schema node passed to the constructor
    ArrayNode schemaArrayNode;

    // schemas that the validator should store
    List<JsonSchema> expectedTupleSchemas;

    /**
     * Minimal {@link URIFactory} implementation that simply creates a {@link URI}.
     */
    static class SimpleURIFactory implements URIFactory {

        @Override
        public URI create(String uri) {
            return URI.create(uri);
        }
    }

    /**
     * Minimal visitor interface required by {@link JsonSchema}.
     */
    interface JsonSchemaVisitor {
        // No methods needed for the tests.
    }

    /**
     * Stub {@link JsonSchema} implementation used throughout the tests.
     */
    static class SimpleJsonSchema extends JsonSchema {

        SimpleJsonSchema() {
            // The real JsonSchema constructor expects (ValidationContext, URI, JsonNode)
            // All three arguments are irrelevant for the behaviour we test, so we pass null.
            super(null, null, null);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            return Collections.emptySet();
        }

        @Override
        public JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public String getSchemaPath() {
            return null;
        }

        @Override
        public JsonSchema getParentSchema() {
            return null;
        }
    }

    /**
     * A {@link ValidationContext} stub that records every schema created via
     * {@link #newSchema(String, JsonNode, JsonSchema)}.
     */
    static class DummyValidationContext extends ValidationContext {

        private final SchemaValidatorsConfig config;

        private final List<JsonSchema> created = new ArrayList<>();

        DummyValidationContext(SchemaValidatorsConfig config) {
            // The real ValidationContext constructor takes several factories.
            // We only need a non‑null URIFactory; the rest can stay null.
            super(new SimpleURIFactory(), null, null, null, config);
            this.config = config;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return config;
        }

        @Override
        public JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
            JsonSchema schema = new SimpleJsonSchema();
            created.add(schema);
            return schema;
        }

        List<JsonSchema> getCreatedSchemas() {
            return created;
        }
    }

    /**
     * ValidationContext that always returns a predefined schema instance.
     */
    static class FixedSchemaValidationContext extends DummyValidationContext {

        private final JsonSchema fixed;

        FixedSchemaValidationContext(SchemaValidatorsConfig config, JsonSchema fixed) {
            super(config);
            this.fixed = fixed;
        }

        @Override
        public JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
            return fixed;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // 1. Prepare a non‑empty ArrayNode that mimics a valid "prefixItems"
        ObjectMapper mapper = new ObjectMapper();
        schemaArrayNode = mapper.createArrayNode();
        schemaArrayNode.add(mapper.createObjectNode());
        schemaArrayNode.add(mapper.createObjectNode());
        // 2. Create a dummy ValidationContext.
        config = new SchemaValidatorsConfig();
        validationContext = new DummyValidationContext(config);
        // 3. Build the PrefixItemsValidator instance.
        validator = new PrefixItemsValidator("testSchemaPath", schemaArrayNode, null, validationContext);
        // 4. Capture the schemas that the validator created via the stub context.
        expectedTupleSchemas = new ArrayList<>(((DummyValidationContext) validationContext).getCreatedSchemas());
    }

    @AfterEach
    void teardownAfterEach() {
        validator = null;
        validationContext = null;
        config = null;
        expectedTupleSchemas = null;
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
        List<JsonSchema> actual = validator.getTupleSchema();
        assertEquals(2, actual.size(), "Tuple schema list should contain two entries.");
        assertEquals(expectedTupleSchemas, actual, "The tuple schema list should contain the JsonSchema instances created by ValidationContext.");
    }

    @Test
    void constructor_throwsExceptionWhenSchemaNodeIsEmptyArray() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode emptyArray = mapper.createArrayNode();
        assertThrows(IllegalArgumentException.class, () -> new PrefixItemsValidator("emptyPath", emptyArray, null, validationContext));
    }

    @Test
    @DisplayName("Empty tuple schema – getTupleSchema() returns an empty, non‑null list")
    void emptyTupleSchema_returnsEmptyList() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode emptyArray = mapper.createArrayNode();
        PrefixItemsValidator emptyValidator = new PrefixItemsValidator("emptyPath", emptyArray, null, validationContext);
        List<JsonSchema> result = emptyValidator.getTupleSchema();
        assertNotNull(result, "Result should never be null.");
        assertTrue(result.isEmpty(), "Result list should be empty for an empty tuple schema.");
    }

    @Test
    @DisplayName("Multiple elements tuple schema – list preserves order of three supplied mocks")
    void multipleElementsTupleSchema_preservesOrder() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode threeArray = mapper.createArrayNode();
        threeArray.add(mapper.createObjectNode());
        threeArray.add(mapper.createObjectNode());
        threeArray.add(mapper.createObjectNode());
        PrefixItemsValidator threeValidator = new PrefixItemsValidator("threePath", threeArray, null, validationContext);
        List<JsonSchema> result = threeValidator.getTupleSchema();
        assertEquals(3, result.size(), "List should contain three elements.");
        for (int i = 0; i < 3; i++) {
            assertTrue(result.get(i) instanceof SimpleJsonSchema, "Element " + i + " should be a SimpleJsonSchema.");
        }
    }

    @Test
    @DisplayName("Mutable reference test – adding to returned list mutates internal state")
    void mutableReference_addElementReflectsInternalState() {
        List<JsonSchema> list = validator.getTupleSchema();
        int originalSize = list.size();
        JsonSchema extra = new SimpleJsonSchema();
        list.add(extra);
        assertEquals(originalSize + 1, validator.getTupleSchema().size(), "Internal tupleSchema should reflect the added element.");
        assertTrue(validator.getTupleSchema().contains(extra), "Added element must be present internally.");
    }

    @Test
    @DisplayName("Null tuple schema handling – getTupleSchema() returns null when field is null")
    void nullTupleSchema_viaReflection_returnsNull() throws Exception {
        setTupleSchema(null);
        assertNull(validator.getTupleSchema(), "When the internal tupleSchema field is null, getTupleSchema() should return null.");
    }

    @Test
    @DisplayName("Original list mutation effect – validator holds a live reference to supplied list")
    void originalListMutationEffect_reflectedInGetter() throws Exception {
        List<JsonSchema> externalList = new ArrayList<>();
        JsonSchema first = new SimpleJsonSchema();
        externalList.add(first);
        setTupleSchema(externalList);
        assertEquals(1, validator.getTupleSchema().size());
        JsonSchema second = new SimpleJsonSchema();
        externalList.add(second);
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
        List<JsonSchema> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        setTupleSchema(unmodifiable);
        List<JsonSchema> returned = validator.getTupleSchema();
        assertThrows(UnsupportedOperationException.class, () -> returned.add(new SimpleJsonSchema()), "Attempting to modify an unmodifiable list should throw UnsupportedOperationException.");
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
        JsonSchema dup = new SimpleJsonSchema();
        List<JsonSchema> dupList = new ArrayList<>(Arrays.asList(dup, dup, dup));
        setTupleSchema(dupList);
        List<JsonSchema> result = validator.getTupleSchema();
        assertEquals(3, result.size(), "List size should reflect all duplicates.");
        assertSame(dup, result.get(0));
        assertSame(dup, result.get(1));
        assertSame(dup, result.get(2));
    }

    @Test
    @DisplayName("Single element tuple schema – list contains exactly one schema")
    void singleElementTupleSchema_returnsSingleSchema() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode singleArray = mapper.createArrayNode();
        singleArray.add(mapper.createObjectNode());
        // Use a fixed‑schema context to guarantee the returned instance.
        JsonSchema fixed = new SimpleJsonSchema();
        ValidationContext fixedContext = new FixedSchemaValidationContext(config, fixed);
        PrefixItemsValidator singleValidator = new PrefixItemsValidator("singlePath", singleArray, null, fixedContext);
        List<JsonSchema> result = singleValidator.getTupleSchema();
        assertNotNull(result);
        assertEquals(1, result.size(), "List should contain exactly one element.");
        assertSame(fixed, result.get(0), "The element should be the fixed schema supplied by the context.");
    }
}
