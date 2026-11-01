
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidationContext_Test {

    @Mock
    private ValidationContext mockValidationContext;

    @Mock
    private JsonMetaSchema mockMetaSchema;

    @Mock
    private JsonSchemaFactory mockFactory;

    @Mock
    private JsonNode mockSchemaNode;

    @Mock
    private JsonNodePath mockEvaluationPath;

    private JsonSchema jsonSchema;

    @Mock
    private SchemaValidatorsConfig mockConfig;

    private SimpleValidationContext validationContext;

    private SimpleMetaSchema metaSchema;

    private SimpleFactory factory;

    private SimpleNodePath evaluationPath;

    /**
     * Simple stub for {@link ValidationContext}.
     */
    static class SimpleValidationContext implements ValidationContext {

        private JsonMetaSchema metaSchema;

        private JsonSchemaFactory factory;

        private final ConcurrentMap<String, JsonSchema> schemaResources = new ConcurrentHashMap<>();

        private final ConcurrentMap<String, JsonSchema> dynamicAnchors = new ConcurrentHashMap<>();

        SimpleValidationContext(JsonMetaSchema metaSchema, JsonSchemaFactory factory) {
            this.metaSchema = metaSchema;
            this.factory = factory;
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            return metaSchema;
        }

        void setMetaSchema(JsonMetaSchema metaSchema) {
            this.metaSchema = metaSchema;
        }

        @Override
        public JsonSchemaFactory getJsonSchemaFactory() {
            return factory;
        }

        @Override
        public ConcurrentMap<String, JsonSchema> getSchemaResources() {
            return schemaResources;
        }

        @Override
        public ConcurrentMap<String, JsonSchema> getDynamicAnchors() {
            return dynamicAnchors;
        }

        @Override
        public JsonSchema resolveSchemaId(String id) {
            return null;
        }
    }

    /**
     * Minimal stub for {@link JsonMetaSchema}.
     */
    static class SimpleMetaSchema implements JsonMetaSchema {

        @Override
        public JsonNode readAnchor(String anchor) {
            return null;
        }

        @Override
        public JsonNode readDynamicAnchor(String anchor) {
            return null;
        }
    }

    /**
     * Minimal stub for {@link JsonSchemaFactory}.
     */
    static class SimpleFactory implements JsonSchemaFactory {
        // No behaviour required for the tests.
    }

    /**
     * Minimal stub for {@link JsonNodePath}.
     */
    static class SimpleNodePath implements JsonNodePath {
        // No behaviour required for the tests.
    }

    /**
     * Minimal stub for {@link SchemaValidatorsConfig}.
     */
    static class SimpleConfig implements SchemaValidatorsConfig {
        // No behaviour required for the tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        metaSchema = new SimpleMetaSchema();
        factory = new SimpleFactory();
        validationContext = new SimpleValidationContext(metaSchema, factory);
        evaluationPath = new SimpleNodePath();
        // Build the JsonSchema instance using the package‑private factory method.
        SchemaLocation schemaLocation = new SchemaLocation(null, null);
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, // schema node – not needed for these tests
        null, // parent schema
        null, false);
    }

    @Test
    void getValidationContext_returnsSameInstance() {
        ValidationContext returned = jsonSchema.getValidationContext();
        assertSame(validationContext, returned, "JsonSchema#getValidationContext should return the ValidationContext supplied at construction");
    }

    @Test
    void getValidationContext_multipleCallsReturnSameInstance() {
        ValidationContext first = jsonSchema.getValidationContext();
        ValidationContext second = jsonSchema.getValidationContext();
        assertSame(first, second, "Multiple invocations of getValidationContext should return the identical instance");
    }

    @Test
    void getValidationContext_returnsNullWhenFieldIsNull() throws Exception {
        // Create a fresh JsonSchema instance.
        JsonSchema schema = JsonSchema.from(validationContext, new SchemaLocation(null, null), evaluationPath, null, null, false);
        // Force the private field in BaseJsonValidator to null via reflection.
        Field field = BaseJsonValidator.class.getDeclaredField("validationContext");
        field.setAccessible(true);
        field.set(schema, null);
        // The getter should now return null.
        assertNull(schema.getValidationContext(), "When the internal validationContext field is null, getValidationContext() must return null");
    }

    @Test
    void getValidationContext_doesNotCreateCopy() {
        // Retrieve the stored context and mutate it.
        SimpleValidationContext ctx = (SimpleValidationContext) jsonSchema.getValidationContext();
        SimpleMetaSchema newMeta = new SimpleMetaSchema();
        ctx.setMetaSchema(newMeta);
        // Retrieve again and verify we still have the same reference (hence the mutation is visible).
        ValidationContext again = jsonSchema.getValidationContext();
        assertSame(ctx, again, "getValidationContext should return the stored reference, not a copy");
        assertSame(newMeta, again.getMetaSchema(), "Mutations on the returned ValidationContext must be reflected on subsequent calls");
    }

    @Test
    void getValidationContext_inheritedSchemaViaFromRefSharesContext() {
        // Simulate a child schema creation via fromRef.
        JsonSchema child = jsonSchema.fromRef(jsonSchema, evaluationPath);
        assertNotNull(child, "fromRef should produce a non‑null child schema");
        assertSame(validationContext, child.getValidationContext(), "Child schema created via fromRef must share the same ValidationContext as the parent");
    }

    @Test
    void getValidationContext_unchangedAfterWithConfig() {
        // Apply a custom configuration; the method returns a new JsonSchema instance.
        SimpleConfig config = new SimpleConfig();
        JsonSchema configured = jsonSchema.withConfig(config);
        assertNotNull(configured, "withConfig should return a new JsonSchema instance");
        assertSame(validationContext, configured.getValidationContext(), "withConfig must preserve the original ValidationContext instance");
    }

    @Test
    void getValidationContext_unchangedAfterValidationExecution() {
        // Build a schema with an empty object node – no validators will be generated.
        ObjectNode emptySchemaNode = JsonNodeFactory.instance.objectNode();
        JsonSchema simpleSchema = JsonSchema.from(validationContext, new SchemaLocation(null, null), evaluationPath, emptySchemaNode, null, false);
        // Perform a validation on a trivial JSON instance (empty object).
        ObjectNode instance = JsonNodeFactory.instance.objectNode();
        Set<ValidationMessage> messages = simpleSchema.validate(instance);
        // Expect no validation messages.
        assertTrue(messages.isEmpty(), "Validation of an empty schema against an empty instance should succeed");
        // Ensure the ValidationContext is still the same instance.
        assertSame(validationContext, simpleSchema.getValidationContext(), "Validation execution must not replace or modify the stored ValidationContext");
    }

    @Test
    void getValidationContext_threadSafetyOfConcurrentReads() throws Exception {
        final int THREAD_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<ValidationContext>> futures = new ArrayList<>();
        // Submit concurrent tasks that invoke getValidationContext().
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> jsonSchema.getValidationContext()));
        }
        // Collect results.
        List<ValidationContext> results = new ArrayList<>();
        for (Future<ValidationContext> f : futures) {
            results.add(f.get(5, TimeUnit.SECONDS));
        }
        executor.shutdownNow();
        // All returned references must be identical to the original mock.
        for (ValidationContext ctx : results) {
            assertSame(validationContext, ctx, "All concurrent calls to getValidationContext must return the identical ValidationContext instance");
        }
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:13: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:49: error: interface expected here
    static class SimpleValidationContext implements ValidationContext {
                                                    ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:97: error: interface expected here
    static class SimpleMetaSchema implements JsonMetaSchema {
                                             ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:113: error: interface expected here
    static class SimpleFactory implements JsonSchemaFactory {
                                          ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:120: error: interface expected here
    static class SimpleNodePath implements JsonNodePath {
                                           ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:12: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:127: error: interface expected here
    static class SimpleConfig implements SchemaValidatorsConfig {
                                         ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:15: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:18: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:21: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:24: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:35: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:64: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:73: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:78: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:83: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:88: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:99: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:104: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:135: error: incompatible types: SimpleMetaSchema cannot be converted to JsonMetaSchema
        validationContext = new SimpleValidationContext(metaSchema, factory);
                                                        ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:139: error: incompatible types: SimpleValidationContext cannot be converted to ValidationContext
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, // schema node – not needed for these tests
                                     ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:160: error: incompatible types: SimpleValidationContext cannot be converted to ValidationContext
        JsonSchema schema = JsonSchema.from(validationContext, new SchemaLocation(null, null), evaluationPath, null, null, false);
                                            ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:172: error: incompatible types: ValidationContext cannot be converted to SimpleValidationContext
        SimpleValidationContext ctx = (SimpleValidationContext) jsonSchema.getValidationContext();
                                                                                               ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:174: error: incompatible types: SimpleMetaSchema cannot be converted to JsonMetaSchema
        ctx.setMetaSchema(newMeta);
                          ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:184: error: incompatible types: SimpleNodePath cannot be converted to JsonNodePath
        JsonSchema child = jsonSchema.fromRef(jsonSchema, evaluationPath);
                                                          ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:193: error: incompatible types: SimpleConfig cannot be converted to SchemaValidatorsConfig
        JsonSchema configured = jsonSchema.withConfig(config);
                                                      ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:202: error: incompatible types: SimpleValidationContext cannot be converted to ValidationContext
        JsonSchema simpleSchema = JsonSchema.from(validationContext, new SchemaLocation(null, null), evaluationPath, emptySchemaNode, null, false);
                                                  ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
29 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.ValidationContext', 'dependent classes': 'class `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.getValidationContext()`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;\nclass `com.networknt.schema.FormatValidator`:\n\trelated functions:\n\t\tmethod `protected boolean isStrict(ExecutionContext)` returns `boolean`, related with `com.networknt.schema.JsonSchema.getValidationContext()`, api document: \r\n     * Determines if strict handling.\r\n     * <p>\r\n     * Note that this defaults to false.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @return whether to perform strict handling\r\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


