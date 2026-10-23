```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PropertiesValidator#preloadJsonSchema()}.
 *
 * <p>These tests do not rely on external mocking libraries.  Simple stub
 * implementations of {@link ValidationContext} and {@link JsonSchema} are used
 * to record interactions and to simulate the behaviours required by the test
 * scenarios.</p>
 */
@DisplayName("PropertiesValidator.preloadJsonSchema() tests")
class PropertiesValidator_preloadJsonSchema_Test {

    /** Shared Jackson mapper for creating {@link JsonNode}s from JSON strings. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Holds a reference to the private {@code schemas} field of {@link PropertiesValidator}. */
    private static Field schemasField;

    /** Simple stub for {@link ValidationContext}. */
    private static class StubValidationContext implements ValidationContext {
        final List<StubJsonSchema> created = new ArrayList<>();

        @Override
        public JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
            StubJsonSchema schema = new StubJsonSchema();
            created.add(schema);
            return schema;
        }
    }

    /** Simple stub for {@link JsonSchema}. */
    private static class StubJsonSchema implements JsonSchema {
        /** Indicates whether {@link #preloadJsonSchema()} was invoked. */
        boolean preloadCalled = false;

        /** Optional exception to be thrown when {@code preloadJsonSchema()} is called. */
        RuntimeException toThrow = null;

        @Override
        public void preloadJsonSchema() {
            preloadCalled = true;
            if (toThrow != null) {
                throw toThrow;
            }
        }

        // The remaining methods of the JsonSchema interface are not required for these tests.
        // They can be left with default (empty) implementations.
        @Override public void accept(JsonSchemaVisitor visitor) {}
        @Override public void validate(JsonNode node, JsonNode rootNode, String at) {}
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public String getSchemaPath() { return null; }
        @Override public void setSchemaPath(String path) {}
        @Override public JsonNode getSchemaNode() { return null; }
        @Override public void setSchemaNode(JsonNode node) {}
        @Override public ValidationContext getValidationContext() { return null; }
        @Override public void setValidationContext(ValidationContext ctx) {}
        @Override public void setParentSchema(JsonSchema parent) {}
        @Override public String getRef() { return null; }
        @Override public void setRef(String ref) {}
        @Override public void setId(String id) {}
        @Override public String getId() { return null; }
        @Override public void setType(String type) {}
        @Override public String getType() { return null; }
        @Override public void setFormat(String format) {}
        @Override public String getFormat() { return null; }
        @Override public void setEnums(List<String> enums) {}
        @Override public List<String> getEnums() { return null; }
        @Override public void setPattern(String pattern) {}
        @Override public String getPattern() { return null; }
        @Override public void setMaximum(Number maximum) {}
        @Override public Number getMaximum() { return null; }
        @Override public void setMinimum(Number minimum) {}
        @Override public Number getMinimum() { return null; }
        @Override public void setExclusiveMaximum(boolean exclusive) {}
        @Override public boolean isExclusiveMaximum() { return false; }
        @Override public void setExclusiveMinimum(boolean exclusive) {}
        @Override public boolean isExclusiveMinimum() { return false; }
        @Override public void setMultipleOf(Number multipleOf) {}
        @Override public Number getMultipleOf() { return null; }
        @Override public void setMaxLength(Integer maxLength) {}
        @Override public Integer getMaxLength() { return null; }
        @Override public void setMinLength(Integer minLength) {}
        @Override public Integer getMinLength() { return null; }
        @Override public void setItems(JsonSchema items) {}
        @Override public JsonSchema getItems() { return null; }
        @Override public void setAdditionalItems(JsonSchema additionalItems) {}
        @Override public JsonSchema getAdditionalItems() { return null; }
        @Override public void setRequired(List<String> required) {}
        @Override public List<String> getRequired() { return null; }
        @Override public void setProperties(Map<String, JsonSchema> properties) {}
        @Override public Map<String, JsonSchema> getProperties() { return null; }
        @Override public void setPatternProperties(Map<String, JsonSchema> patternProperties) {}
        @Override public Map<String, JsonSchema> getPatternProperties() { return null; }
        @Override public void setAdditionalProperties(JsonSchema additionalProperties) {}
        @Override public JsonSchema getAdditionalProperties() { return null; }
        @Override public void setDependencies(Map<String, JsonSchema> dependencies) {}
        @Override public Map<String, JsonSchema> getDependencies() { return null; }
        @Override public void setAllOf(List<JsonSchema> allOf) {}
        @Override public List<JsonSchema> getAllOf() { return null; }
        @Override public void setAnyOf(List<JsonSchema> anyOf) {}
        @Override public List<JsonSchema> getAnyOf() { return null; }
        @Override public void setOneOf(List<JsonSchema> oneOf) {}
        @Override public List<JsonSchema> getOneOf() { return null; }
        @Override public void setNot(JsonSchema not) {}
        @Override public JsonSchema getNot() { return null; }
        @Override public void setIf(JsonSchema ifSchema) {}
        @Override public JsonSchema getIf() { return null; }
        @Override public void setThen(JsonSchema thenSchema) {}
        @Override public JsonSchema getThen() { return null; }
        @Override public void setElse(JsonSchema elseSchema) {}
        @Override public JsonSchema getElse() { return null; }
        @Override public void setDefinitions(Map<String, JsonSchema> definitions) {}
        @Override public Map<String, JsonSchema> getDefinitions() { return null; }
        @Override public void setRefSchema(JsonSchema refSchema) {}
        @Override public JsonSchema getRefSchema() { return null; }
        @Override public void setSchemaVersion(SpecVersion.VersionFlag version) {}
        @Override public SpecVersion.VersionFlag getSchemaVersion() { return null; }
        @Override public void setSchemaType(SchemaType schemaType) {}
        @Override public SchemaType getSchemaType() { return null; }
        @Override public void setFormatValidator(FormatValidator formatValidator) {}
        @Override public FormatValidator getFormatValidator() { return null; }
        @Override public void setValidatorType(ValidatorTypeCode validatorType) {}
        @Override public ValidatorTypeCode getValidatorType() { return null; }
        @Override public void setSchemaNode(JsonNode schemaNode) {}
        @Override public JsonNode getSchemaNode() { return null; }
        @Override public void setValidationContext(ValidationContext validationContext) {}
        @Override public ValidationContext getValidationContext() { return null; }
        @Override public void setParentSchema(JsonSchema parentSchema) {}
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public void setSchemaPath(String schemaPath) {}
        @Override public String getSchemaPath() { return null; }
        @Override public void setSchemaVersion(SpecVersion.VersionFlag version) {}
        @Override public SpecVersion.VersionFlag getSchemaVersion() { return null; }
    }

    /** Helper subclass that records the collection passed to {@code preloadJsonSchemas}. */
    private static class SpyPropertiesValidator extends PropertiesValidator {
        Collection<JsonSchema> capturedCollection = null;
        boolean doNothing = false; // when true the overridden method does nothing

        SpyPropertiesValidator(String schemaPath,
                               JsonNode schemaNode,
                               JsonSchema parentSchema,
                               ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validationContext);
        }

        @Override
        protected void preloadJsonSchemas(Collection<JsonSchema> schemas) {
            capturedCollection = schemas;
            if (!doNothing) {
                super.preloadJsonSchemas(schemas);
            }
        }
    }

    @BeforeAll
    static void initReflection() throws Exception {
        schemasField = PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
    }

    /**
     * Replaces the private {@code schemas} map of a {@link PropertiesValidator}
     * instance with the supplied map.
     */
    private static PropertiesValidator replaceSchemasMap(PropertiesValidator validator,
                                                          Map<String, JsonSchema> map) throws Exception {
        schemasField.set(validator, map);
        return validator;
    }

    @Test
    @DisplayName("preloadJsonSchema invokes preload on each schema")
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
        StubValidationContext ctx = new StubValidationContext();

        // WHEN – constructing the validator (the constructor creates the internal map)
        PropertiesValidator validator = new PropertiesValidator("testSchema", schemaNode, null, ctx);

        // Sanity check – the internal map should contain exactly two schema objects
        assertEquals(2, validator.getSchemas().size(),
                "Validator should have created a JsonSchema stub for each property");

        // AND – invoking preloadJsonSchema()
        validator.preloadJsonSchema();

        // THEN – each created stub must have received a single call to preloadJsonSchema()
        for (StubJsonSchema stub : ctx.created) {
            assertTrue(stub.preloadCalled,
                    "Each created JsonSchema stub should have its preloadJsonSchema() called");
        }
    }

    @Test
    @DisplayName("Empty schemas map – should invoke preloadJsonSchemas with empty collection")
    void emptySchemasMap() throws Exception {
        // GIVEN – an empty map
        Map<String, JsonSchema> emptyMap = new LinkedHashMap<>();
        PropertiesValidator base = new PropertiesValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(base, emptyMap);

        // Spy via subclass
        SpyPropertiesValidator spy = new SpyPropertiesValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(spy, emptyMap);
        spy.doNothing = true; // we only want to capture the argument

        // WHEN
        spy.preloadJsonSchema();

        // THEN – captured collection must be empty
        assertNotNull(spy.capturedCollection, "The collection passed to preloadJsonSchemas must be captured");
        assertTrue(spy.capturedCollection.isEmpty(), "Expected an empty collection to be passed");
    }

    @Test
    @DisplayName("Single schema entry – collection contains exactly that schema")
    void singleSchemaEntry() throws Exception {
        JsonSchema schema = new StubJsonSchema();
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("only", schema);

        SpyPropertiesValidator spy = new SpyPropertiesValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(spy, map);
        spy.doNothing = true;

        spy.preloadJsonSchema();

        assertNotNull(spy.capturedCollection);
        assertEquals(1, spy.capturedCollection.size());
        assertTrue(spy.capturedCollection.contains(schema));
    }

    @Test
    @DisplayName("Multiple schema entries – preserves insertion order")
    void multipleSchemaEntriesPreserveOrder() throws Exception {
        JsonSchema a = new StubJsonSchema();
        JsonSchema b = new StubJsonSchema();
        JsonSchema c = new StubJsonSchema();

        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("a", a);
        map.put("b", b);
        map.put("c", c);

        SpyPropertiesValidator spy = new SpyPropertiesValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(spy, map);
        spy.doNothing = true;

        spy.preloadJsonSchema();

        assertEquals(Arrays.asList(a, b, c), new ArrayList<>(spy.capturedCollection));
    }

    @Test
    @DisplayName("Null schema value in map – propagates NullPointerException")
    void nullSchemaValuePropagatesNPE() throws Exception {
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("nullKey", null);

        PropertiesValidator validator = new PropertiesValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(validator, map);

        assertThrows(NullPointerException.class, validator::preloadJsonSchema);
    }

    @Test
    @DisplayName("Exception propagation from a schema during preload")
    void exceptionFromSchemaPropagates() throws Exception {
        StubJsonSchema badSchema = new StubJsonSchema();
        badSchema.toThrow = new RuntimeException("boom");

        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("bad", badSchema);

        PropertiesValidator validator = new PropertiesValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(validator, map);

        RuntimeException ex = assertThrows(RuntimeException.class, validator::preloadJsonSchema);
        assertEquals("boom", ex.getMessage());
    }

    @Test
    @DisplayName("Repeated invocation does not alter internal state")
    void repeatedInvocationKeepsState() throws Exception {
        JsonSchema s1 = new StubJsonSchema();
        JsonSchema s2 = new StubJsonSchema();

        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("one", s1);
        map.put("two", s2);

        SpyPropertiesValidator spy = new SpyPropertiesValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(spy, map);
        spy.doNothing = true;

        // First call
        spy.preloadJsonSchema();
        Collection<JsonSchema> first = spy.capturedCollection;

        // Second call
        spy.preloadJsonSchema();
        Collection<JsonSchema> second = spy.capturedCollection;

        assertEquals(Arrays.asList(s1, s2), new ArrayList<>(first));
        assertEquals(Arrays.asList(s1, s2), new ArrayList<>(second));

        // Verify map unchanged
        assertEquals(2, spy.getSchemas().size());
    }

    @Test
    @DisplayName("Order preservation with mixed insertion patterns")
    void mixedInsertionOrderPreserved() throws Exception {
        JsonSchema a = new StubJsonSchema();
        JsonSchema b = new StubJsonSchema();
        JsonSchema c = new StubJsonSchema();
        JsonSchema d = new StubJsonSchema();

        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("A", a);
        map.put("B", b);
        map.put("C", c);
        map.remove("B");          // remove B
        map.put("B", d);          // re‑insert under same key

        SpyPropertiesValidator spy = new SpyPropertiesValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(spy, map);
        spy.doNothing = true;

        spy.preloadJsonSchema();

        assertEquals(Arrays.asList(a, c, d), new ArrayList<>(spy.capturedCollection));
    }

    @Test
    @DisplayName("Null collection handling after map mutation – snapshot semantics")
    void collectionSnapshotAfterMutation() throws Exception {
        JsonSchema only = new StubJsonSchema();
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("only", only);

        SpyPropertiesValidator spy = new SpyPropertiesValidator("id", MAPPER.createObjectNode(), null, null) {
            @Override
            protected void preloadJsonSchemas(Collection<JsonSchema> schemas) {
                // Capture the collection before mutating the map
                capturedCollection = schemas;
                // Mutate the underlying map
                map.clear();
                // Verify snapshot still contains the original element
                assertTrue(capturedCollection.contains(only),
                        "Snapshot collection must still contain the original schema");
                // No further action needed
            }
        };
        replaceSchemasMap(spy, map);

        spy.preloadJsonSchema();
    }

    @Test
    @DisplayName("Concurrent modification during preload – propagates CME")
    void concurrentModificationDuringPreload() throws Exception {
        // Schema that mutates the validator's map while its preloadJsonSchema is called
        StubJsonSchema mutating = new StubJsonSchema() {
            @Override
            public void preloadJsonSchema() {
                // add a new entry during iteration
                getValidator().getSchemas().put("new", new StubJsonSchema());
            }
        };

        // Helper to expose the validator to the stub
        class MutatingValidator extends PropertiesValidator {
            MutatingValidator(String path, JsonNode node, JsonSchema parent, ValidationContext ctx) {
                super(path, node, parent, ctx);
            }
        }

        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("mut", mutating);
        MutatingValidator validator = new MutatingValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(validator, map);

        // The stub needs a reference back to the validator; we set it via reflection
        Field validatorField = StubJsonSchema.class.getDeclaredField("this$0");
        validatorField.setAccessible(true);
        validatorField.set(mutating, validator);

        assertThrows(ConcurrentModificationException.class, validator::preloadJsonSchema);
    }

    @Test
    @DisplayName("Error propagation from schema – AssertionError bubbles up")
    void assertionErrorFromSchemaPropagates() throws Exception {
        StubJsonSchema bad = new StubJsonSchema();
        bad.toThrow = new AssertionError("assertion failed");

        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("bad", bad);

        PropertiesValidator validator = new PropertiesValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(validator, map);

        AssertionError err = assertThrows(AssertionError.class, validator::preloadJsonSchema);
        assertEquals("assertion failed", err.getMessage());
    }

    @Test
    @DisplayName("Null internal schemas map – NullPointerException")
    void nullInternalSchemasMap() throws Exception {
        PropertiesValidator validator = new PropertiesValidator("id", MAPPER.createObjectNode(), null, null);
        schemasField.set(validator, null);
        assertThrows(NullPointerException.class, validator::preloadJsonSchema);
    }
}
```