
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_preloadJsonSchema_Test {

    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaValidatorsConfig config;

    @Mock
    JsonSchema mockSchema1;

    @Mock
    JsonSchema mockSchema2;

    AnyOfValidator anyOfValidator;

    /**
     * Simple {@link JsonSchema} implementation that records calls to {@code preloadJsonSchema}.
     */
    private static class SimpleJsonSchema extends JsonSchema {

        private int preloadCount = 0;

        SimpleJsonSchema() {
            super(null, null, null, null);
        }

        @Override
        public void preloadJsonSchema() {
            preloadCount++;
        }

        int getPreloadCount() {
            return preloadCount;
        }

        /* --------------------------------------------------------------------- */
        /* The remaining abstract methods are not needed for the current tests. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at) {
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at, boolean walkAll) {
            return Collections.emptySet();
        }

        @Override
        public JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public JsonValidator getValidator() {
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

        @Override
        public String getRef() {
            return null;
        }

        @Override
        public String getSchemaLocation() {
            return null;
        }
    }

    /**
     * Minimal {@link SchemaValidatorsConfig} implementation used by the tests.
     */
    private static class StubSchemaValidatorsConfig implements SchemaValidatorsConfig {

        @Override
        public boolean isOpenAPI3StyleDiscriminators() {
            return false;
        }
        /* All other methods use the default behaviour (return false / null). */
    }

    /**
     * Minimal {@link ValidationContext} implementation that returns pre‑programmed schemas.
     */
    private static class StubValidationContext implements ValidationContext {

        private final SchemaValidatorsConfig config;

        private final Queue<JsonSchema> queue;

        StubValidationContext(SchemaValidatorsConfig config, Queue<JsonSchema> queue) {
            this.config = config;
            this.queue = queue;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return config;
        }

        @Override
        public JsonSchema newSchema(String path, JsonNode schemaNode, JsonSchema parent) {
            // return the next pre‑programmed mock
            return queue.poll();
        }

        /* The remaining methods are not required for these tests – provide dummy
         * implementations so the class can be instantiated. */
        @Override
        public String getCurrentUri() {
            return null;
        }

        @Override
        public JsonSchema getParentSchema() {
            return null;
        }

        @Override
        public JsonNode resolve(JsonNode node) {
            return null;
        }

        @Override
        public JsonNode resolve(JsonNode node, JsonNode rootNode) {
            return null;
        }

        @Override
        public JsonNode resolve(JsonNode node, JsonNode rootNode, String at) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, String at) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, String at, boolean isReference) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, String at) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, String at, boolean isReference) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, String at, boolean isReference, boolean isRecursive) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, String at, boolean isReference) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, boolean isReference) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive, boolean isRecursiveRef) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, String at, boolean isReference, boolean isRecursive, boolean isRecursiveRef) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, String at, boolean isReference, boolean isRecursive) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, String at, boolean isReference) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, String at) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveParent) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, String at, boolean isReference, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveParent) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, String at, boolean isReference, boolean isRecursive) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, String at, boolean isReference, boolean isRecursive) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive, boolean isRecursiveRef) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, String at, boolean isReference, boolean isRecursive, boolean isRecursiveRef) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveParent) {
            return null;
        }

        @Override
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, String at, boolean isReference, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveParent) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, boolean isReference) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, boolean isReference, boolean isRecursive) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, boolean isReference, boolean isRecursive, boolean isRecursiveRef) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, boolean isReference, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveParent) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, boolean isReference) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, boolean isReference, boolean isRecursive) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, boolean isReference, boolean isRecursive, boolean isRecursiveRef) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, boolean isReference, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveParent) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive, boolean isRecursiveRef) {
            return null;
        }

        @Override
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveParent) {
            return null;
        }
    }

    /**
     * Subclass of {@link AnyOfValidator} that delegates the preload behaviour to a supplied {@link Consumer}.
     */
    private static class TestAnyOfValidator extends AnyOfValidator {

        private final Consumer<List<JsonSchema>> behaviour;

        TestAnyOfValidator(String path, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext, Consumer<List<JsonSchema>> behaviour) {
            super(path, schemaNode, parentSchema, validationContext);
            this.behaviour = behaviour;
        }

        @Override
        protected void preloadJsonSchemas(List<JsonSchema> schemas) {
            behaviour.accept(schemas);
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----- stubbed behaviour for ValidationContext -----
        config = new StubSchemaValidatorsConfig();
        mockSchema1 = new SimpleJsonSchema();
        mockSchema2 = new SimpleJsonSchema();
        Queue<JsonSchema> queue = new ArrayDeque<>();
        queue.add(mockSchema1);
        queue.add(mockSchema2);
        validationContext = new StubValidationContext(config, queue);
        // ----- build a simple schema node that contains two subschemas -----
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());
        schemaArray.add(mapper.createObjectNode());
        anyOfValidator = new AnyOfValidator("testPath", schemaArray, null, validationContext);
    }

    @Test
    void testPreloadJsonSchemaInvokesPreloadOnAllContainedSchemas() {
        anyOfValidator.preloadJsonSchema();
        assertEquals(1, mockSchema1.getPreloadCount(), "First schema should have its preloadJsonSchema() called exactly once");
        assertEquals(1, mockSchema2.getPreloadCount(), "Second schema should have its preloadJsonSchema() called exactly once");
    }

    /**
     * Reflectively obtains the private {@code schemas} field from the validator.
     */
    @SuppressWarnings("unchecked")
    private List<JsonSchema> getSchemas(AnyOfValidator validator) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        return (List<JsonSchema>) f.get(validator);
    }

    @Test
    void preloadWithEmptySchemasList() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode emptyArray = mapper.createArrayNode();
        AnyOfValidator validator = new AnyOfValidator("emptyPath", emptyArray, null, validationContext);
        assertTrue(getSchemas(validator).isEmpty(), "pre‑condition: list must be empty");
        validator.preloadJsonSchema();
        assertTrue(getSchemas(validator).isEmpty(), "list should remain empty");
        assertEquals(0, mockSchema1.getPreloadCount());
        assertEquals(0, mockSchema2.getPreloadCount());
    }

    @Test
    void preloadWithPrePopulatedSchemasList() throws Exception {
        Consumer<List<JsonSchema>> addOneMore = list -> list.add(mockSchema1);
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, addOneMore);
        List<JsonSchema> internal = getSchemas(validator);
        internal.add(mockSchema1);
        internal.add(mockSchema2);
        assertEquals(2, internal.size());
        validator.preloadJsonSchema();
        // the overridden behaviour adds one more element
        assertEquals(3, internal.size());
        assertTrue(internal.contains(mockSchema1));
        assertTrue(internal.contains(mockSchema2));
    }

    @Test
    void exceptionPropagationFromPreloadJsonSchemas() {
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, list -> {
            throw new RuntimeException("boom");
        });
        RuntimeException ex = assertThrows(RuntimeException.class, validator::preloadJsonSchema);
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void multipleConsecutivePreloadCalls() throws Exception {
        Consumer<List<JsonSchema>> addTwo = list -> {
            SimpleJsonSchema s1 = new SimpleJsonSchema();
            SimpleJsonSchema s2 = new SimpleJsonSchema();
            list.add(s1);
            list.add(s2);
        };
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, addTwo);
        validator.preloadJsonSchema();
        assertEquals(2, getSchemas(validator).size());
        validator.preloadJsonSchema();
        assertEquals(4, getSchemas(validator).size());
    }

    @Test
    void preloadClearsExistingSchemas() throws Exception {
        Consumer<List<JsonSchema>> clear = List::clear;
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, clear);
        List<JsonSchema> internal = getSchemas(validator);
        internal.add(new SimpleJsonSchema());
        internal.add(new SimpleJsonSchema());
        internal.add(new SimpleJsonSchema());
        assertEquals(3, internal.size());
        validator.preloadJsonSchema();
        assertTrue(internal.isEmpty(), "list should be empty after clear");
    }

    @Test
    void preloadAddsNullSchema() throws Exception {
        Consumer<List<JsonSchema>> addNull = list -> list.add(null);
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, addNull);
        int before = getSchemas(validator).size();
        validator.preloadJsonSchema();
        List<JsonSchema> after = getSchemas(validator);
        assertEquals(before + 1, after.size());
        assertNull(after.get(after.size() - 1));
    }

    @Test
    void preloadWithLargeNumberOfSchemas() throws Exception {
        Consumer<List<JsonSchema>> addHundred = list -> {
            for (int i = 0; i < 100; i++) {
                list.add(new SimpleJsonSchema());
            }
        };
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, addHundred);
        validator.preloadJsonSchema();
        assertEquals(100, getSchemas(validator).size());
    }

    @Test
    void preloadWithNoModifications() throws Exception {
        Consumer<List<JsonSchema>> noop = list -> {
            /* no‑op */
        };
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, noop);
        List<JsonSchema> internal = getSchemas(validator);
        for (int i = 0; i < 5; i++) internal.add(new SimpleJsonSchema());
        assertEquals(5, internal.size());
        validator.preloadJsonSchema();
        assertEquals(5, internal.size(), "size must stay unchanged");
    }

    @Test
    void nullInternalSchemasList() throws Exception {
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, list -> {
            /* no‑op */
        });
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, null);
        assertThrows(NullPointerException.class, validator::preloadJsonSchema);
    }

    @Test
    void unmodifiableSchemasList() throws Exception {
        Consumer<List<JsonSchema>> tryAdd = list -> list.add(new SimpleJsonSchema());
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, tryAdd);
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, Collections.unmodifiableList(new ArrayList<>()));
        assertThrows(UnsupportedOperationException.class, validator::preloadJsonSchema);
    }

    @Test
    void specificUncheckedExceptionPropagation() {
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, list -> {
            throw new IllegalArgumentException("bad arg");
        });
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, validator::preloadJsonSchema);
        assertEquals("bad arg", ex.getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:12: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:98: error: interface expected here
    private static class StubSchemaValidatorsConfig implements SchemaValidatorsConfig {
                                                               ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:110: error: interface expected here
    private static class StubValidationContext implements ValidationContext {
                                                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:220: error: method getSchema(JsonNode,JsonNode,String,boolean) is already defined in class StubValidationContext
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, String at, boolean isReference) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:245: error: method getSchema(String,JsonNode,JsonNode,String,boolean,boolean) is already defined in class StubValidationContext
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, String at, boolean isReference, boolean isRecursive) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:250: error: method getSchema(String,JsonNode,JsonNode,boolean,boolean) is already defined in class StubValidationContext
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:255: error: method getSchema(String,JsonNode,JsonNode,String,boolean) is already defined in class StubValidationContext
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, String at, boolean isReference) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:260: error: method getSchema(String,JsonNode,JsonNode,boolean) is already defined in class StubValidationContext
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:265: error: method getSchema(String,JsonNode,JsonNode,String) is already defined in class StubValidationContext
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, String at) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:270: error: method getSchema(String,JsonNode) is already defined in class StubValidationContext
        public JsonSchema getSchema(String path, JsonNode node) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:290: error: method getSchema(JsonNode,JsonNode,boolean,boolean) is already defined in class StubValidationContext
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:295: error: method getSchema(JsonNode,JsonNode,String,boolean,boolean) is already defined in class StubValidationContext
        public JsonSchema getSchema(JsonNode node, JsonNode rootNode, String at, boolean isReference, boolean isRecursive) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:320: error: method getSchema(String) is already defined in class StubValidationContext
        public JsonSchema getSchema(String path) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:365: error: method getSchema(String,JsonNode,JsonNode,boolean) is already defined in class StubValidationContext
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:370: error: method getSchema(String,JsonNode,JsonNode,boolean,boolean) is already defined in class StubValidationContext
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:375: error: method getSchema(String,JsonNode,JsonNode,boolean,boolean,boolean) is already defined in class StubValidationContext
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive, boolean isRecursiveRef) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:380: error: method getSchema(String,JsonNode,JsonNode,boolean,boolean,boolean,boolean) is already defined in class StubValidationContext
        public JsonSchema getSchema(String path, JsonNode node, JsonNode rootNode, boolean isReference, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveParent) {
                          ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:15: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:18: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class AnyOfValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:21: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class AnyOfValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:24: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class AnyOfValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class AnyOfValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:40: error: incompatible types: <null> cannot be converted to boolean
            super(null, null, null, null);
                                    ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:69: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:84: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:89: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:100: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:121: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:126: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:134: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:139: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:144: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:149: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:154: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:159: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:164: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:169: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:174: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:179: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:184: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:189: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:194: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:199: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:204: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:209: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:214: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:219: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:224: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:229: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:234: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:239: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:244: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:249: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:254: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:259: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:264: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:269: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:274: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:279: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:284: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:289: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:294: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:299: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:304: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:309: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:314: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:319: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:324: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:329: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:334: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:339: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:344: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:349: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:354: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:359: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:364: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:369: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:374: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:379: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:397: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:406: error: incompatible types: StubSchemaValidatorsConfig cannot be converted to SchemaValidatorsConfig
        config = new StubSchemaValidatorsConfig();
                 ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:412: error: incompatible types: StubValidationContext cannot be converted to ValidationContext
        validationContext = new StubValidationContext(config, queue);
                            ^
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:424: error: cannot find symbol
        assertEquals(1, mockSchema1.getPreloadCount(), "First schema should have its preloadJsonSchema() called exactly once");
                                   ^
  symbol:   method getPreloadCount()
  location: variable mockSchema1 of type JsonSchema
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:425: error: cannot find symbol
        assertEquals(1, mockSchema2.getPreloadCount(), "Second schema should have its preloadJsonSchema() called exactly once");
                                   ^
  symbol:   method getPreloadCount()
  location: variable mockSchema2 of type JsonSchema
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:446: error: cannot find symbol
        assertEquals(0, mockSchema1.getPreloadCount());
                                   ^
  symbol:   method getPreloadCount()
  location: variable mockSchema1 of type JsonSchema
src/test/java/com/networknt/schema/AnyOfValidator_preloadJsonSchema_Test.java:447: error: cannot find symbol
        assertEquals(0, mockSchema2.getPreloadCount());
                                   ^
  symbol:   method getPreloadCount()
  location: variable mockSchema2 of type JsonSchema
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
87 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'dependent classes': 'class `com.networknt.schema.AnyOfValidator`:\n\tdependent fields:\n\t\tjava.util.List<com.networknt.schema.JsonSchema> com.networknt.schema.AnyOfValidator.schemas;\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.AnyOfValidator.preloadJsonSchema()`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.AnyOfValidator.preloadJsonSchema()`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `public void preloadJsonSchema()` returns `void`, related with `com.networknt.schema.AnyOfValidator.preloadJsonSchema()`;\nclass `com.networknt.schema.PatternPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public void preloadJsonSchema()` returns `void`, related with `com.networknt.schema.AnyOfValidator.preloadJsonSchema()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


