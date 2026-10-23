import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
/* Additional imports required for the JsonSchema stub */
import com.networknt.schema.JsonSchemaVisitor;
import com.networknt.schema.SchemaType;
import com.networknt.schema.FormatValidator;
import com.networknt.schema.SpecVersion;

@ExtendWith(MockitoExtension.class)
@DisplayName("PropertiesValidator.preloadJsonSchema() tests")
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

    /**
     * Helper static reference used by the concurrent‑modification test.
     */
    private static PropertiesValidator concurrentValidator;

    /**
     * Subclass that allows mutation of the validator's internal map while
     * preloading.
     */
    class MutatingValidator extends PropertiesValidator {

        MutatingValidator(String path, JsonNode node, JsonSchema parent, ValidationContext ctx) {
            super(path, node, parent, ctx);
        }
    }

    /**
     * Simple stub for {@link ValidationContext}.  It creates a fresh
     * {@link JsonSchema} stub for each call to {@code newSchema}.
     */
    private static class StubValidationContext extends ValidationContext {

        final List<StubJsonSchema> created = new ArrayList<>();

        @Override
        public JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
            StubJsonSchema schema = new StubJsonSchema();
            created.add(schema);
            return schema;
        }
    }

    /**
     * Simple stub for {@link JsonSchema}.
     */
    private static class StubJsonSchema implements JsonSchema {

        /**
         * Indicates whether {@link #preloadJsonSchema()} was invoked.
         */
        boolean preloadCalled = false;

        /**
         * Optional exception to be thrown when {@code preloadJsonSchema()} is called.
         */
        RuntimeException toThrow = null;

        @Override
        public void preloadJsonSchema() {
            preloadCalled = true;
            if (toThrow != null) {
                throw toThrow;
            }
        }

        // -----------------------------------------------------------------
        // Empty implementations of the remaining JsonSchema methods.
        // -----------------------------------------------------------------
        @Override
        public void accept(JsonSchemaVisitor visitor) {
        }

        @Override
        public void validate(JsonNode node, JsonNode rootNode, String at) {
        }

        @Override
        public String getRef() {
            return null;
        }

        @Override
        public void setRef(String ref) {
        }

        @Override
        public void setId(String id) {
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public void setType(String type) {
        }

        @Override
        public String getType() {
            return null;
        }

        @Override
        public void setFormat(String format) {
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public void setEnums(List<String> enums) {
        }

        @Override
        public List<String> getEnums() {
            return null;
        }

        @Override
        public void setPattern(String pattern) {
        }

        @Override
        public String getPattern() {
            return null;
        }

        @Override
        public void setMaximum(Number maximum) {
        }

        @Override
        public Number getMaximum() {
            return null;
        }

        @Override
        public void setMinimum(Number minimum) {
        }

        @Override
        public Number getMinimum() {
            return null;
        }

        @Override
        public void setExclusiveMaximum(boolean exclusive) {
        }

        @Override
        public boolean isExclusiveMaximum() {
            return false;
        }

        @Override
        public void setExclusiveMinimum(boolean exclusive) {
        }

        @Override
        public boolean isExclusiveMinimum() {
            return false;
        }

        @Override
        public void setMultipleOf(Number multipleOf) {
        }

        @Override
        public Number getMultipleOf() {
            return null;
        }

        @Override
        public void setMaxLength(Integer maxLength) {
        }

        @Override
        public Integer getMaxLength() {
            return null;
        }

        @Override
        public void setMinLength(Integer minLength) {
        }

        @Override
        public Integer getMinLength() {
            return null;
        }

        @Override
        public void setItems(JsonSchema items) {
        }

        @Override
        public JsonSchema getItems() {
            return null;
        }

        @Override
        public void setAdditionalItems(JsonSchema additionalItems) {
        }

        @Override
        public JsonSchema getAdditionalItems() {
            return null;
        }

        @Override
        public void setRequired(List<String> required) {
        }

        @Override
        public List<String> getRequired() {
            return null;
        }

        @Override
        public void setProperties(Map<String, JsonSchema> properties) {
        }

        @Override
        public Map<String, JsonSchema> getProperties() {
            return null;
        }

        @Override
        public void setPatternProperties(Map<String, JsonSchema> patternProperties) {
        }

        @Override
        public Map<String, JsonSchema> getPatternProperties() {
            return null;
        }

        @Override
        public void setAdditionalProperties(JsonSchema additionalProperties) {
        }

        @Override
        public JsonSchema getAdditionalProperties() {
            return null;
        }

        @Override
        public void setDependencies(Map<String, JsonSchema> dependencies) {
        }

        @Override
        public Map<String, JsonSchema> getDependencies() {
            return null;
        }

        @Override
        public void setAllOf(List<JsonSchema> allOf) {
        }

        @Override
        public List<JsonSchema> getAllOf() {
            return null;
        }

        @Override
        public void setAnyOf(List<JsonSchema> anyOf) {
        }

        @Override
        public List<JsonSchema> getAnyOf() {
            return null;
        }

        @Override
        public void setOneOf(List<JsonSchema> oneOf) {
        }

        @Override
        public List<JsonSchema> getOneOf() {
            return null;
        }

        @Override
        public void setNot(JsonSchema not) {
        }

        @Override
        public JsonSchema getNot() {
            return null;
        }

        @Override
        public void setIf(JsonSchema ifSchema) {
        }

        @Override
        public JsonSchema getIf() {
            return null;
        }

        @Override
        public void setThen(JsonSchema thenSchema) {
        }

        @Override
        public JsonSchema getThen() {
            return null;
        }

        @Override
        public void setElse(JsonSchema elseSchema) {
        }

        @Override
        public JsonSchema getElse() {
            return null;
        }

        @Override
        public void setDefinitions(Map<String, JsonSchema> definitions) {
        }

        @Override
        public Map<String, JsonSchema> getDefinitions() {
            return null;
        }

        @Override
        public void setRefSchema(JsonSchema refSchema) {
        }

        @Override
        public JsonSchema getRefSchema() {
            return null;
        }

        @Override
        public void setSchemaType(SchemaType schemaType) {
        }

        @Override
        public SchemaType getSchemaType() {
            return null;
        }

        @Override
        public void setFormatValidator(FormatValidator formatValidator) {
        }

        @Override
        public FormatValidator getFormatValidator() {
            return null;
        }

        @Override
        public void setValidatorType(ValidatorTypeCode validatorType) {
        }

        @Override
        public ValidatorTypeCode getValidatorType() {
            return null;
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode) {
        }

        @Override
        public JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public void setValidationContext(ValidationContext validationContext) {
        }

        @Override
        public ValidationContext getValidationContext() {
            return null;
        }

        @Override
        public void setParentSchema(JsonSchema parentSchema) {
        }

        @Override
        public JsonSchema getParentSchema() {
            return null;
        }

        @Override
        public void setSchemaPath(String schemaPath) {
        }

        @Override
        public String getSchemaPath() {
            return null;
        }

        @Override
        public void setSchemaVersion(SpecVersion.VersionFlag version) {
        }

        @Override
        public SpecVersion.VersionFlag getSchemaVersion() {
            return null;
        }
    }

    /**
     * Helper subclass that records the collection passed to {@code preloadJsonSchemas}.
     */
    private static class SpyPropertiesValidator extends PropertiesValidator {

        Collection<JsonSchema> capturedCollection = null;

        boolean doNothing = false;

        SpyPropertiesValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
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

    @AfterEach
    void teardownAfterEach() {
        createdSchemas.clear();
    }

    /**
     * Helper to create a fresh {@link PropertiesValidator} instance with a minimal
     * schema node (empty object) and then replace its internal {@code schemas}
     * map with the supplied one.
     */
    private com.networknt.schema.PropertiesValidator createValidatorWithMap(Map<String, JsonSchema> map) throws Exception {
        PropertiesValidator validator = new PropertiesValidator("testSchema", MAPPER.createObjectNode(), null, null);
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

    /**
     * Replaces the private {@code schemas} map of a {@link PropertiesValidator}
     * instance with the supplied map.
     */
    private static com.networknt.schema.PropertiesValidator replaceSchemasMap(PropertiesValidator validator, Map<String, JsonSchema> map) throws Exception {
        schemasField.set(validator, map);
        return validator;
    }

    @Test
    void testPreloadJsonSchemaInvokesPreloadOnEachSchema() throws Exception {
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
        PropertiesValidator validator = new PropertiesValidator("testSchema", schemaNode, null, ctx);
        assertEquals(2, validator.getSchemas().size(), "Validator should have created a JsonSchema stub for each property");
        validator.preloadJsonSchema();
        for (StubJsonSchema stub : ctx.created) {
            assertTrue(stub.preloadCalled, "Each created JsonSchema stub should have its preloadJsonSchema() called");
        }
    }

    @Test
    @DisplayName("Empty schemas map – should invoke preloadJsonSchemas with empty collection")
    void emptySchemasMap() throws Exception {
        Map<String, JsonSchema> emptyMap = new LinkedHashMap<>();
        PropertiesValidator base = new PropertiesValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(base, emptyMap);
        SpyPropertiesValidator spy = new SpyPropertiesValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(spy, emptyMap);
        spy.doNothing = true;
        spy.preloadJsonSchema();
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
        spy.preloadJsonSchema();
        Collection<JsonSchema> first = spy.capturedCollection;
        spy.preloadJsonSchema();
        Collection<JsonSchema> second = spy.capturedCollection;
        assertEquals(Arrays.asList(s1, s2), new ArrayList<>(first));
        assertEquals(Arrays.asList(s1, s2), new ArrayList<>(second));
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
        map.remove("B");
        map.put("B", d);
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
                capturedCollection = schemas;
                map.clear();
                assertTrue(capturedCollection.contains(only), "Snapshot collection must still contain the original schema");
            }
        };
        replaceSchemasMap(spy, map);
        spy.preloadJsonSchema();
    }

    @Test
    @DisplayName("Concurrent modification during preload – propagates CME")
    void concurrentModificationDuringPreload() throws Exception {
        StubJsonSchema mutating = new StubJsonSchema() {

            @Override
            public void preloadJsonSchema() {
                // mutate the validator's map while iterating
                concurrentValidator.getSchemas().put("new", new StubJsonSchema());
            }
        };
        Map<String, JsonSchema> map = new LinkedHashMap<>();
        map.put("mut", mutating);
        MutatingValidator validator = new MutatingValidator("id", MAPPER.createObjectNode(), null, null);
        replaceSchemasMap(validator, map);
        // make it visible to the stub
        concurrentValidator = validator;
        assertThrows(ConcurrentModificationException.class, validator::preloadJsonSchema);
        concurrentValidator = null;
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
