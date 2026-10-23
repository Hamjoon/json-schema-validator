
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidatorTypeCode;
import com.networknt.schema.ValidationContext;

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

    @BeforeEach
    void setupBeforeEach() {
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(invocation -> {
            JsonSchema mockSchema = mock(JsonSchema.class);
            createdSchemas.add(mockSchema);
            return mockSchema;
        });
    }

    @AfterEach
    void teardownAfterEach() {
        createdSchemas.clear();
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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:2: error: package org.mockito does not exist
import static org.mockito.ArgumentMatchers.*;
                         ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:3: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:20: error: package org.mockito does not exist
import org.mockito.ArgumentCaptor;
                  ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:21: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:22: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:23: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:57: error: cannot find symbol
    private static PropertiesValidator concurrentValidator;
                   ^
  symbol:   class PropertiesValidator
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:506: error: cannot find symbol
    private Collection<JsonSchema> capturePreloadArgument(PropertiesValidator spy) {
                                                          ^
  symbol:   class PropertiesValidator
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:516: error: cannot find symbol
    private static com.networknt.schema.PropertiesValidator replaceSchemasMap(PropertiesValidator validator, Map<String, JsonSchema> map) throws Exception {
                                                                              ^
  symbol:   class PropertiesValidator
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:24: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:59: error: cannot find symbol
    class MutatingValidator extends PropertiesValidator {
                                    ^
  symbol:   class PropertiesValidator
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:69: error: interface expected here
    private static class StubValidationContext implements ValidationContext {
                                                          ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:84: error: interface expected here
    private static class StubJsonSchema implements JsonSchema {
                                                   ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:108: error: cannot find symbol
        public void accept(JsonSchemaVisitor visitor) {
                           ^
  symbol:   class JsonSchemaVisitor
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:377: error: cannot find symbol
        public void setSchemaType(SchemaType schemaType) {
                                  ^
  symbol:   class SchemaType
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:381: error: cannot find symbol
        public SchemaType getSchemaType() {
               ^
  symbol:   class SchemaType
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:386: error: cannot find symbol
        public void setFormatValidator(FormatValidator formatValidator) {
                                       ^
  symbol:   class FormatValidator
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:390: error: cannot find symbol
        public FormatValidator getFormatValidator() {
               ^
  symbol:   class FormatValidator
  location: class StubJsonSchema
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:440: error: package SpecVersion does not exist
        public void setSchemaVersion(SpecVersion.VersionFlag version) {
                                                ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:444: error: package SpecVersion does not exist
        public SpecVersion.VersionFlag getSchemaVersion() {
                          ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:452: error: cannot find symbol
    private static class SpyPropertiesValidator extends PropertiesValidator {
                                                        ^
  symbol:   class PropertiesValidator
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:29: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:41: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:73: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:77: error: incompatible types: StubJsonSchema cannot be converted to JsonSchema
            return schema;
                   ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:96: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:107: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:111: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:115: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:120: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:124: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:128: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:133: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:137: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:142: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:146: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:151: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:155: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:160: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:164: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:169: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:173: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:178: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:182: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:187: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:191: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:196: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:200: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:205: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:209: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:214: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:218: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:223: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:227: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:232: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:236: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:241: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:245: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:250: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:254: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:259: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:263: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:268: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:272: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:277: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:281: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:286: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:290: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:295: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:299: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:304: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:308: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:313: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:317: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:322: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:326: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:331: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:335: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:340: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:344: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:349: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:353: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:358: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:362: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:367: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:371: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:376: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:380: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:385: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:389: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:394: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:398: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:403: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:407: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:412: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:416: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:421: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:425: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:430: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_preloadJsonSchema_Test.java:434: error: method does not override or implement a method from a supertype
        @Override
        ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
100 errors
only showing the first 100 errors, of 152 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'dependent classes': 'class `com.networknt.schema.PropertiesValidator`:\n\tdependent fields:\n\t\tjava.util.Map<java.lang.String, com.networknt.schema.JsonSchema> com.networknt.schema.PropertiesValidator.schemas;\n\trelated functions:\n\t\tmethod `public Map<String, JsonSchema> getSchemas()` returns `java.util.Map<java.lang.String, com.networknt.schema.JsonSchema>`, related with `com.networknt.schema.PropertiesValidator.preloadJsonSchema()`;\nclass `com.networknt.schema.PatternPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public void preloadJsonSchema()` returns `void`, related with `com.networknt.schema.PropertiesValidator.preloadJsonSchema()`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `public void preloadJsonSchema()` returns `void`, related with `com.networknt.schema.PropertiesValidator.preloadJsonSchema()`;\nclass `com.networknt.schema.FormatKeyword`:\n\trelated functions:\n\t\tmethod ` Collection<Format> getFormats()` returns `java.util.Collection<com.networknt.schema.Format>`, related with `com.networknt.schema.PropertiesValidator.preloadJsonSchema()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


