
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
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

    class MutatingValidator extends PropertiesValidator {
        MutatingValidator(String path, JsonNode node, JsonSchema parent, ValidationContext ctx) {
            super(path, node, parent, ctx);
        }
    }

    /**
     * Simple stub for {@link ValidationContext}.
     */
    private static class StubValidationContext implements ValidationContext {
        final List<StubJsonSchema> created = new ArrayList<>();

        @Override
        public com.networknt.schema.JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
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

        // The remaining methods of the JsonSchema interface are not required for these tests.
        // They can be left with default (empty) implementations.
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
        public com.networknt.schema.JsonSchema getItems() {
            return null;
        }

        @Override
        public void setAdditionalItems(JsonSchema additionalItems) {
        }

        @Override
        public com.networknt.schema.JsonSchema getAdditionalItems() {
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
        public com.networknt.schema.JsonSchema getAdditionalProperties() {
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
        public com.networknt.schema.JsonSchema getNot() {
            return null;
        }

        @Override
        public void setIf(JsonSchema ifSchema) {
        }

        @Override
        public com.networknt.schema.JsonSchema getIf() {
            return null;
        }

        @Override
        public void setThen(JsonSchema thenSchema) {
        }

        @Override
        public com.networknt.schema.JsonSchema getThen() {
            return null;
        }

        @Override
        public void setElse(JsonSchema elseSchema) {
        }

        @Override
        public com.networknt.schema.JsonSchema getElse() {
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
        public com.networknt.schema.JsonSchema getRefSchema() {
            return null;
        }

        @Override
        public void setSchemaType(SchemaType schemaType) {
        }

        @Override
        public com.networknt.schema.SchemaType getSchemaType() {
            return null;
        }

        @Override
        public void setFormatValidator(FormatValidator formatValidator) {
        }

        @Override
        public com.networknt.schema.FormatValidator getFormatValidator() {
            return null;
        }

        @Override
        public void setValidatorType(ValidatorTypeCode validatorType) {
        }

        @Override
        public com.networknt.schema.ValidatorTypeCode getValidatorType() {
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

        // when true the overridden method does nothing
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
        schemasField = com.networknt.schema.PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub ValidationContext#newSchema to return a fresh mock JsonSchema for each call
        // and keep a reference to the mock for later verification.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(com.networknt.schema.JsonSchema.class))).thenAnswer(invocation -> {
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
    private com.networknt.schema.PropertiesValidator createValidatorWithMap(Map<String, JsonSchema> map) throws Exception {
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
        assertEquals(2, validator.getSchemas().size(), "Validator should have created a JsonSchema stub for each property");
        // AND – invoking preloadJsonSchema()
        validator.preloadJsonSchema();
        // THEN – each created stub must have received a single call to preloadJsonSchema()
        for (StubJsonSchema stub : ctx.created) {
            assertTrue(stub.preloadCalled, "Each created JsonSchema stub should have its preloadJsonSchema() called");
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
        // we only want to capture the argument
        spy.doNothing = true;
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
        // remove B
        map.remove("B");
        // re‑insert under same key
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
                // Capture the collection before mutating the map
                capturedCollection = schemas;
                // Mutate the underlying map
                map.clear();
                // Verify snapshot still contains the original element
                assertTrue(capturedCollection.contains(only), "Snapshot collection must still contain the original schema");
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
        Field validatorField = PropertiesValidator_preloadJsonSchema_Test.StubJsonSchema.class.getDeclaredField("this$0");
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

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:1: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
 ^
  symbol: class ExtendWith
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:2: error: cannot find symbol
@DisplayName("PropertiesValidator.preloadJsonSchema() tests")
 ^
  symbol: class DisplayName
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:7: error: cannot find symbol
    private static final ObjectMapper MAPPER = new ObjectMapper();
                         ^
  symbol:   class ObjectMapper
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:13: error: cannot find symbol
    ValidationContext validationContext;
    ^
  symbol:   class ValidationContext
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:18: error: cannot find symbol
    private final List<JsonSchema> createdSchemas = new ArrayList<>();
                  ^
  symbol:   class List
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:18: error: cannot find symbol
    private final List<JsonSchema> createdSchemas = new ArrayList<>();
                       ^
  symbol:   class JsonSchema
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:23: error: cannot find symbol
    private static Field schemasField;
                   ^
  symbol:   class Field
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:463: error: cannot find symbol
    private com.networknt.schema.PropertiesValidator createValidatorWithMap(Map<String, JsonSchema> map) throws Exception {
                                                                            ^
  symbol:   class Map
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:463: error: cannot find symbol
    private com.networknt.schema.PropertiesValidator createValidatorWithMap(Map<String, JsonSchema> map) throws Exception {
                                                                                        ^
  symbol:   class JsonSchema
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:472: error: cannot find symbol
    private Collection<JsonSchema> capturePreloadArgument(PropertiesValidator spy) {
                                                          ^
  symbol:   class PropertiesValidator
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:472: error: cannot find symbol
    private Collection<JsonSchema> capturePreloadArgument(PropertiesValidator spy) {
            ^
  symbol:   class Collection
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:472: error: cannot find symbol
    private Collection<JsonSchema> capturePreloadArgument(PropertiesValidator spy) {
                       ^
  symbol:   class JsonSchema
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:482: error: cannot find symbol
    private static com.networknt.schema.PropertiesValidator replaceSchemasMap(PropertiesValidator validator, Map<String, JsonSchema> map) throws Exception {
                                                                              ^
  symbol:   class PropertiesValidator
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:482: error: cannot find symbol
    private static com.networknt.schema.PropertiesValidator replaceSchemasMap(PropertiesValidator validator, Map<String, JsonSchema> map) throws Exception {
                                                                                                             ^
  symbol:   class Map
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:482: error: cannot find symbol
    private static com.networknt.schema.PropertiesValidator replaceSchemasMap(PropertiesValidator validator, Map<String, JsonSchema> map) throws Exception {
                                                                                                                         ^
  symbol:   class JsonSchema
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:25: error: cannot find symbol
    class MutatingValidator extends PropertiesValidator {
                                    ^
  symbol:   class PropertiesValidator
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:26: error: cannot find symbol
        MutatingValidator(String path, JsonNode node, JsonSchema parent, ValidationContext ctx) {
                                       ^
  symbol:   class JsonNode
  location: class PropertiesValidator_preloadJsonSchema_Test.MutatingValidator
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:26: error: cannot find symbol
        MutatingValidator(String path, JsonNode node, JsonSchema parent, ValidationContext ctx) {
                                                      ^
  symbol:   class JsonSchema
  location: class PropertiesValidator_preloadJsonSchema_Test.MutatingValidator
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:26: error: cannot find symbol
        MutatingValidator(String path, JsonNode node, JsonSchema parent, ValidationContext ctx) {
                                                                         ^
  symbol:   class ValidationContext
  location: class PropertiesValidator_preloadJsonSchema_Test.MutatingValidator
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:34: error: cannot find symbol
    private static class StubValidationContext implements ValidationContext {
                                                          ^
  symbol:   class ValidationContext
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:35: error: cannot find symbol
        final List<StubJsonSchema> created = new ArrayList<>();
              ^
  symbol:   class List
  location: class StubValidationContext
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:48: error: cannot find symbol
    private static class StubJsonSchema implements JsonSchema {
                                                   ^
  symbol:   class JsonSchema
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:38: error: cannot find symbol
        public com.networknt.schema.JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
                                                                      ^
  symbol:   class JsonNode
  location: class StubValidationContext
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:38: error: cannot find symbol
        public com.networknt.schema.JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
                                                                                     ^
  symbol:   class JsonSchema
  location: class StubValidationContext
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:70: error: cannot find symbol
        public void accept(JsonSchemaVisitor visitor) {
                           ^
  symbol:   class JsonSchemaVisitor
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:74: error: cannot find symbol
        public void validate(JsonNode node, JsonNode rootNode, String at) {
                             ^
  symbol:   class JsonNode
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:74: error: cannot find symbol
        public void validate(JsonNode node, JsonNode rootNode, String at) {
                                            ^
  symbol:   class JsonNode
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:114: error: cannot find symbol
        public void setEnums(List<String> enums) {
                             ^
  symbol:   class List
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:118: error: cannot find symbol
        public List<String> getEnums() {
               ^
  symbol:   class List
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:195: error: cannot find symbol
        public void setItems(JsonSchema items) {
                             ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:204: error: cannot find symbol
        public void setAdditionalItems(JsonSchema additionalItems) {
                                       ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:213: error: cannot find symbol
        public void setRequired(List<String> required) {
                                ^
  symbol:   class List
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:217: error: cannot find symbol
        public List<String> getRequired() {
               ^
  symbol:   class List
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:222: error: cannot find symbol
        public void setProperties(Map<String, JsonSchema> properties) {
                                  ^
  symbol:   class Map
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:222: error: cannot find symbol
        public void setProperties(Map<String, JsonSchema> properties) {
                                              ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:226: error: cannot find symbol
        public Map<String, JsonSchema> getProperties() {
               ^
  symbol:   class Map
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:226: error: cannot find symbol
        public Map<String, JsonSchema> getProperties() {
                           ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:231: error: cannot find symbol
        public void setPatternProperties(Map<String, JsonSchema> patternProperties) {
                                         ^
  symbol:   class Map
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:231: error: cannot find symbol
        public void setPatternProperties(Map<String, JsonSchema> patternProperties) {
                                                     ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:235: error: cannot find symbol
        public Map<String, JsonSchema> getPatternProperties() {
               ^
  symbol:   class Map
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:235: error: cannot find symbol
        public Map<String, JsonSchema> getPatternProperties() {
                           ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:240: error: cannot find symbol
        public void setAdditionalProperties(JsonSchema additionalProperties) {
                                            ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:249: error: cannot find symbol
        public void setDependencies(Map<String, JsonSchema> dependencies) {
                                    ^
  symbol:   class Map
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:249: error: cannot find symbol
        public void setDependencies(Map<String, JsonSchema> dependencies) {
                                                ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:253: error: cannot find symbol
        public Map<String, JsonSchema> getDependencies() {
               ^
  symbol:   class Map
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:253: error: cannot find symbol
        public Map<String, JsonSchema> getDependencies() {
                           ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:258: error: cannot find symbol
        public void setAllOf(List<JsonSchema> allOf) {
                             ^
  symbol:   class List
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:258: error: cannot find symbol
        public void setAllOf(List<JsonSchema> allOf) {
                                  ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:262: error: cannot find symbol
        public List<JsonSchema> getAllOf() {
               ^
  symbol:   class List
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:262: error: cannot find symbol
        public List<JsonSchema> getAllOf() {
                    ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:267: error: cannot find symbol
        public void setAnyOf(List<JsonSchema> anyOf) {
                             ^
  symbol:   class List
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:267: error: cannot find symbol
        public void setAnyOf(List<JsonSchema> anyOf) {
                                  ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:271: error: cannot find symbol
        public List<JsonSchema> getAnyOf() {
               ^
  symbol:   class List
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:271: error: cannot find symbol
        public List<JsonSchema> getAnyOf() {
                    ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:276: error: cannot find symbol
        public void setOneOf(List<JsonSchema> oneOf) {
                             ^
  symbol:   class List
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:276: error: cannot find symbol
        public void setOneOf(List<JsonSchema> oneOf) {
                                  ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:280: error: cannot find symbol
        public List<JsonSchema> getOneOf() {
               ^
  symbol:   class List
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:280: error: cannot find symbol
        public List<JsonSchema> getOneOf() {
                    ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:285: error: cannot find symbol
        public void setNot(JsonSchema not) {
                           ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:294: error: cannot find symbol
        public void setIf(JsonSchema ifSchema) {
                          ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:303: error: cannot find symbol
        public void setThen(JsonSchema thenSchema) {
                            ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:312: error: cannot find symbol
        public void setElse(JsonSchema elseSchema) {
                            ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:321: error: cannot find symbol
        public void setDefinitions(Map<String, JsonSchema> definitions) {
                                   ^
  symbol:   class Map
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:321: error: cannot find symbol
        public void setDefinitions(Map<String, JsonSchema> definitions) {
                                               ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:325: error: cannot find symbol
        public Map<String, JsonSchema> getDefinitions() {
               ^
  symbol:   class Map
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:325: error: cannot find symbol
        public Map<String, JsonSchema> getDefinitions() {
                           ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:330: error: cannot find symbol
        public void setRefSchema(JsonSchema refSchema) {
                                 ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:339: error: cannot find symbol
        public void setSchemaType(SchemaType schemaType) {
                                  ^
  symbol:   class SchemaType
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:343: error: cannot find symbol
        public com.networknt.schema.SchemaType getSchemaType() {
                                   ^
  symbol:   class SchemaType
  location: package com.networknt.schema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:348: error: cannot find symbol
        public void setFormatValidator(FormatValidator formatValidator) {
                                       ^
  symbol:   class FormatValidator
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:357: error: cannot find symbol
        public void setValidatorType(ValidatorTypeCode validatorType) {
                                     ^
  symbol:   class ValidatorTypeCode
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:366: error: cannot find symbol
        public void setSchemaNode(JsonNode schemaNode) {
                                  ^
  symbol:   class JsonNode
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:370: error: cannot find symbol
        public JsonNode getSchemaNode() {
               ^
  symbol:   class JsonNode
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:375: error: cannot find symbol
        public void setValidationContext(ValidationContext validationContext) {
                                         ^
  symbol:   class ValidationContext
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:379: error: cannot find symbol
        public ValidationContext getValidationContext() {
               ^
  symbol:   class ValidationContext
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:384: error: cannot find symbol
        public void setParentSchema(JsonSchema parentSchema) {
                                    ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:388: error: cannot find symbol
        public JsonSchema getParentSchema() {
               ^
  symbol:   class JsonSchema
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:402: error: package SpecVersion does not exist
        public void setSchemaVersion(SpecVersion.VersionFlag version) {
                                                ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:406: error: package SpecVersion does not exist
        public SpecVersion.VersionFlag getSchemaVersion() {
                          ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:414: error: cannot find symbol
    private static class SpyPropertiesValidator extends PropertiesValidator {
                                                        ^
  symbol:   class PropertiesValidator
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:415: error: cannot find symbol
        Collection<JsonSchema> capturedCollection = null;
        ^
  symbol:   class Collection
  location: class SpyPropertiesValidator
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:415: error: cannot find symbol
        Collection<JsonSchema> capturedCollection = null;
                   ^
  symbol:   class JsonSchema
  location: class SpyPropertiesValidator
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:420: error: cannot find symbol
        SpyPropertiesValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
                                                  ^
  symbol:   class JsonNode
  location: class SpyPropertiesValidator
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:420: error: cannot find symbol
        SpyPropertiesValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
                                                                       ^
  symbol:   class JsonSchema
  location: class SpyPropertiesValidator
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:420: error: cannot find symbol
        SpyPropertiesValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
                                                                                                ^
  symbol:   class ValidationContext
  location: class SpyPropertiesValidator
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:425: error: cannot find symbol
        protected void preloadJsonSchemas(Collection<JsonSchema> schemas) {
                                          ^
  symbol:   class Collection
  location: class SpyPropertiesValidator
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:425: error: cannot find symbol
        protected void preloadJsonSchemas(Collection<JsonSchema> schemas) {
                                                     ^
  symbol:   class JsonSchema
  location: class SpyPropertiesValidator
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:1: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:12: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:433: error: cannot find symbol
    @BeforeAll
     ^
  symbol:   class BeforeAll
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:439: error: cannot find symbol
    @BeforeEach
     ^
  symbol:   class BeforeEach
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:450: error: cannot find symbol
    @AfterEach
     ^
  symbol:   class AfterEach
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:487: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:513: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:514: error: cannot find symbol
    @DisplayName("Empty schemas map – should invoke preloadJsonSchemas with empty collection")
     ^
  symbol:   class DisplayName
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:532: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:533: error: cannot find symbol
    @DisplayName("Single schema entry – collection contains exactly that schema")
     ^
  symbol:   class DisplayName
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:547: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:548: error: cannot find symbol
    @DisplayName("Multiple schema entries – preserves insertion order")
     ^
  symbol:   class DisplayName
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:564: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class PropertiesValidator_preloadJsonSchema_Test
100 errors
only showing the first 100 errors, of 300 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'dependent classes': 'class `com.networknt.schema.PropertiesValidator`:\n\tdependent fields:\n\t\tjava.util.Map<java.lang.String, com.networknt.schema.JsonSchema> com.networknt.schema.PropertiesValidator.schemas;\n\trelated functions:\n\t\tmethod `public Map<String, JsonSchema> getSchemas()` returns `java.util.Map<java.lang.String, com.networknt.schema.JsonSchema>`, related with `com.networknt.schema.PropertiesValidator.preloadJsonSchema()`;\nclass `com.networknt.schema.PatternPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public void preloadJsonSchema()` returns `void`, related with `com.networknt.schema.PropertiesValidator.preloadJsonSchema()`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `public void preloadJsonSchema()` returns `void`, related with `com.networknt.schema.PropertiesValidator.preloadJsonSchema()`;\nclass `com.networknt.schema.FormatKeyword`:\n\trelated functions:\n\t\tmethod ` Collection<Format> getFormats()` returns `java.util.Collection<com.networknt.schema.Format>`, related with `com.networknt.schema.PropertiesValidator.preloadJsonSchema()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


