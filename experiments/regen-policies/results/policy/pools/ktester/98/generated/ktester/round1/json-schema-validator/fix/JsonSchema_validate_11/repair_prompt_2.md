
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import com.fasterxml.jackson.core.JsonParseException;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_11_Test {

    /**
     * Shared factory used to create {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    /**
     * Configuration for the schema (no pre‑loading).
     */
    private static SchemaValidatorsConfig config;

    /**
     * The schema under test.
     */
    private JsonSchema schema;

    /**
     * Simple customizer that does nothing – used to exercise the overload.
     */
    private ExecutionContextCustomizer customizer;

    /**
     * Simple no‑op customizer used by a few tests.
     */
    private ExecutionContextCustomizer noopCustomizer;

    @BeforeAll
    static void setupBeforeAll() {
        // Build a factory that can resolve the external reference used in the schema.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.singletonMap("http://example.org/ref.json", // reference schema – a simple object with a required string property
        "{\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" + "  \"$id\": \"http://example.org/ref.json\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": { \"type\": \"string\" }\n" + "  },\n" + "  \"required\": [\"name\"]\n" + "}"))));
        // Do not preload the root schema – we want the factory to parse it lazily.
        config = SchemaValidatorsConfig.builder().preloadJsonSchema(false).build();
    }

    @BeforeEach
    void setupBeforeEach() {
        // A no‑op customizer – the method under test accepts any implementation.
        noopCustomizer = (ctx, vc) -> {
            // intentionally left blank
        };
        // Another no‑op customizer used by a few tests.
        customizer = (ctx, vc) -> {
            // intentionally left blank
        };
    }

    @AfterEach
    void teardownAfterEach() {
        schema = null;
        customizer = null;
        noopCustomizer = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
        config = null;
    }

    /* --------------------------------------------------------------
     * Helper to create a JsonSchema from a raw schema definition.
     * -------------------------------------------------------------- */
    private JsonSchema createSchema(String schemaData) {
        return factory.getSchema(schemaData, config);
    }

    /**
     * Validates that the overload accepting an {@link ExecutionContextCustomizer}
     * correctly delegates to the core validation logic and returns the expected
     * {@link ValidationMessage}s.
     */
    @Test
    void validate_WithExecutionContextCustomizer_ReturnsExpectedMessages() {
        String schemaData = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"$id\": \"http://example.org/schema.json\",\n" + "  \"type\": \"object\",\n" + "  \"$ref\": \"http://example.org/ref.json\"\n" + "}";
        schema = createSchema(schemaData);
        // Input that violates the referenced schema (name must be a string).
        String inputData = "{ \"name\": 1 }";
        Set<ValidationMessage> messages = schema.validate(inputData, InputFormat.JSON, customizer);
        assertEquals(1, messages.size(), "Expected a single validation error");
        ValidationMessage message = messages.iterator().next();
        assertTrue(message.getMessage().toLowerCase().contains("type"), "Expected the validation message to mention a type error");
    }

    /**
     * A concurrency sanity check that also uses the customizer overload.
     * This mirrors the existing concurrency test but exercises the three‑argument
     * method.
     */
    @Test
    void concurrency_WithCustomizer() throws Exception {
        String schemaData = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" + "  \"$id\": \"http://example.org/schema.json\",\n" + "  \"type\": \"object\",\n" + "  \"$ref\": \"http://example.org/ref.json\"\n" + "}";
        schema = createSchema(schemaData);
        String inputData = "{ \"name\": 1 }";
        CountDownLatch latch = new CountDownLatch(1);
        Exception[] captured = new Exception[1];
        Runnable task = () -> {
            try {
                latch.await();
                Set<ValidationMessage> msgs = schema.validate(inputData, InputFormat.JSON, customizer);
                assertEquals(1, msgs.size());
            } catch (Exception e) {
                synchronized (captured) {
                    captured[0] = e;
                }
            }
        };
        Thread[] threads = new Thread[30];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(task, "worker-" + i);
            threads[i].start();
        }
        // release all workers simultaneously
        latch.countDown();
        for (Thread t : threads) {
            t.join();
        }
        if (captured[0] != null) {
            throw captured[0];
        }
    }

    /**
     * 1. Valid JSON input – should return an empty set.
     */
    @Test
    void validJsonInput_ReturnsEmptySet() {
        String schemaData = "{\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": {\"type\":\"string\"},\n" + "    \"age\": {\"type\":\"integer\"}\n" + "  },\n" + "  \"required\": [\"name\",\"age\"],\n" + "  \"additionalProperties\": false\n" + "}";
        schema = createSchema(schemaData);
        String input = "{\"name\":\"John\",\"age\":30}";
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, noopCustomizer);
        assertTrue(msgs.isEmpty(), "Valid JSON should produce no validation messages");
    }

    /**
     * 2. Valid YAML input – should return an empty set.
     */
    @Test
    void validYamlInput_ReturnsEmptySet() {
        String schemaData = "{\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": {\"type\":\"string\"},\n" + "    \"age\": {\"type\":\"integer\"}\n" + "  },\n" + "  \"required\": [\"name\",\"age\"],\n" + "  \"additionalProperties\": false\n" + "}";
        schema = createSchema(schemaData);
        String yaml = "name: John\nage: 30";
        Set<ValidationMessage> msgs = schema.validate(yaml, InputFormat.YAML, noopCustomizer);
        assertTrue(msgs.isEmpty(), "Valid YAML should produce no validation messages");
    }

    /**
     * 3. Invalid type for property 'age' – expect a type‑mismatch message.
     */
    @Test
    void invalidAgeType_ReturnsTypeMismatchMessage() {
        String schemaData = "{\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\"age\": {\"type\":\"integer\"}},\n" + "  \"required\": [\"age\"]\n" + "}";
        schema = createSchema(schemaData);
        String input = "{\"age\":\"thirty\"}";
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, noopCustomizer);
        assertEquals(1, msgs.size(), "Expected exactly one validation error");
        ValidationMessage msg = msgs.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("type"), "Message should mention a type error");
    }

    /**
     * 4. Malformed JSON – should throw a {@link JsonParseException}.
     */
    @Test
    void malformedJson_ThrowsJsonParseException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        // missing closing brace
        String malformed = "{\"name\":\"John\"";
        assertThrows(JsonParseException.class, () -> schema.validate(malformed, InputFormat.JSON, noopCustomizer), "Malformed JSON must result in a JsonParseException");
    }

    /**
     * 5. Null input string – should throw {@link IllegalArgumentException}.
     */
    @Test
    void nullInput_ThrowsIllegalArgumentException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        assertThrows(IllegalArgumentException.class, () -> schema.validate(null, InputFormat.JSON, noopCustomizer), "Passing null as input must raise IllegalArgumentException");
    }

    /**
     * 6. Null {@link InputFormat} – should throw {@link IllegalArgumentException}.
     */
    @Test
    void nullInputFormat_ThrowsIllegalArgumentException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        assertThrows(IllegalArgumentException.class, () -> schema.validate("{}", null, noopCustomizer), "Null InputFormat must raise IllegalArgumentException");
    }

    /**
     * 7. Valid URL‑encoded JSON – should return an empty set.
     */
    @Test
    void urlEncodedValidJson_ReturnsEmptySet() {
        String schemaData = "{\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\"name\": {\"type\":\"string\"}, \"age\": {\"type\":\"integer\"}},\n" + "  \"required\": [\"name\",\"age\"]\n" + "}";
        schema = createSchema(schemaData);
        String urlEncoded = "%7B%22name%22%3A%22John%22%2C%22age%22%3A30%7D";
        String decoded = URLDecoder.decode(urlEncoded, StandardCharsets.UTF_8.name());
        Set<ValidationMessage> msgs = schema.validate(decoded, InputFormat.JSON, noopCustomizer);
        assertTrue(msgs.isEmpty(), "Decoded JSON should be valid");
    }

    /**
     * 8. URL‑encoded string that decodes to non‑JSON – expect a parsing exception.
     */
    @Test
    void urlEncodedInvalidContent_ThrowsParsingException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        // "你好" – not JSON/YAML
        String urlEncoded = "%E4%BD%A0%E5%A5%BD";
        String decoded = URLDecoder.decode(urlEncoded, StandardCharsets.UTF_8.name());
        assertThrows(JsonParseException.class, () -> schema.validate(decoded, InputFormat.JSON, noopCustomizer), "Decoding to non‑JSON should result in a parsing exception");
    }

    /**
     * 9. Empty string with JSON format – parsing exception expected.
     */
    @Test
    void emptyStringJson_ThrowsJsonParseException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        assertThrows(JsonParseException.class, () -> schema.validate("", InputFormat.JSON, noopCustomizer), "Empty string for JSON input must raise JsonParseException");
    }

    /**
     * 10. Whitespace‑only JSON – parsing exception expected.
     */
    @Test
    void whitespaceOnlyJson_ThrowsJsonParseException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        assertThrows(JsonParseException.class, () -> schema.validate("   \n   ", InputFormat.JSON, noopCustomizer), "Whitespace‑only JSON must raise JsonParseException");
    }

    /**
     * 11. Additional property not allowed – expect a specific validation message.
     */
    @Test
    void additionalProperty_NotAllowed_ReturnsMessage() {
        String schemaData = "{\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\"name\": {\"type\":\"string\"}, \"age\": {\"type\":\"integer\"}},\n" + "  \"required\": [\"name\",\"age\"],\n" + "  \"additionalProperties\": false\n" + "}";
        schema = createSchema(schemaData);
        String input = "{\"name\":\"John\",\"age\":30,\"extra\":\"value\"}";
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, noopCustomizer);
        assertEquals(1, msgs.size(), "One error expected for additional property");
        ValidationMessage msg = msgs.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("additional"), "Message should mention additional property");
    }

    /**
     * 12. Missing required property – expect a required‑property message.
     */
    @Test
    void missingRequiredProperty_ReturnsMessage() {
        String schemaData = "{\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\"name\": {\"type\":\"string\"}, \"age\": {\"type\":\"integer\"}},\n" + "  \"required\": [\"name\",\"age\"]\n" + "}";
        schema = createSchema(schemaData);
        String input = "{\"name\":\"John\"}";
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, noopCustomizer);
        assertEquals(1, msgs.size(), "One error expected for missing required property");
        ValidationMessage msg = msgs.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("required"), "Message should mention required property");
    }

    /**
     * 13. Null value for a non‑nullable property – expect a type‑mismatch message.
     */
    @Test
    void nullValueForNonNullableProperty_ReturnsMessage() {
        String schemaData = "{\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\"name\": {\"type\":\"string\"}, \"age\": {\"type\":\"integer\"}},\n" + "  \"required\": [\"name\",\"age\"]\n" + "}";
        schema = createSchema(schemaData);
        String input = "{\"name\":null,\"age\":30}";
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, noopCustomizer);
        assertEquals(1, msgs.size(), "One error expected for null 'name'");
        ValidationMessage msg = msgs.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("type"), "Message should mention a type error for null value");
    }

    /**
     * 14. Fail‑fast customizer – validation should stop after the first error.
     */
    @Test
    void failFastCustomizer_StopsAfterFirstError() {
        String schemaData = "{\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": {\"type\":\"string\"},\n" + "    \"age\": {\"type\":\"integer\"},\n" + "    \"city\": {\"type\":\"string\"}\n" + "  },\n" + "  \"required\": [\"name\",\"age\"],\n" + "  \"additionalProperties\": false\n" + "}";
        schema = createSchema(schemaData);
        String input = "{\"name\":\"John\",\"age\":\"thirty\",\"city\":\"NY\"}";
        ExecutionContextCustomizer failFast = (ctx, vc) -> ctx.setFailFast(true);
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, failFast);
        // With fail‑fast we expect exactly one error.
        assertEquals(1, msgs.size(), "Fail‑fast should limit the result to a single message");
    }

    /**
     * 15. Report‑all customizer – both errors should be reported.
     */
    @Test
    void reportAllCustomizer_CollectsAllErrors() {
        String schemaData = "{\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": {\"type\":\"string\"},\n" + "    \"age\": {\"type\":\"integer\"},\n" + "    \"city\": {\"type\":\"string\"}\n" + "  },\n" + "  \"required\": [\"name\",\"age\"],\n" + "  \"additionalProperties\": false\n" + "}";
        schema = createSchema(schemaData);
        String input = "{\"name\":\"John\",\"age\":\"thirty\",\"city\":\"NY\"}";
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, noopCustomizer);
        assertTrue(msgs.size() >= 2, "When reportAll is true (default) we expect at least two messages (type + additional)");
    }

    /**
     * 16. Customizer that throws – the exception must propagate unchanged.
     */
    @Test
    void customizerThrows_PropagatesRuntimeException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        ExecutionContextCustomizer exploding = (ctx, vc) -> {
            throw new RuntimeException("Customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate("{\"a\":1}", InputFormat.JSON, exploding));
        assertEquals("Customizer failure", ex.getMessage(), "Exception message from customizer should be preserved");
    }

    /**
     * 17. Customizer sets a user‑defined attribute – verify it is stored in the context.
     */
    @Test
    void customizerSetsCustomMessage_AttributeIsStored() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        final ExecutionContext[] captured = new ExecutionContext[1];
        ExecutionContextCustomizer customizer = ctx -> {
            ctx.setCustomMessage("myCustomMessage");
            captured[0] = ctx;
        };
        // Validation itself should succeed (empty object is valid)
        Set<ValidationMessage> msgs = schema.validate("{}", InputFormat.JSON, customizer);
        assertTrue(msgs.isEmpty(), "Valid input should produce no messages");
        assertNotNull(captured[0], "ExecutionContext should have been captured by the customizer");
        assertEquals("myCustomMessage", captured[0].getCustomMessage(), "Custom message set by the customizer must be retrievable");
    }

    /**
     * 18. Malformed YAML – should result in a parsing exception.
     */
    @Test
    void malformedYaml_ThrowsJsonParseException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        String malformedYaml = "name: John\nage: 30: extra";
        assertThrows(JsonParseException.class, () -> schema.validate(malformedYaml, InputFormat.YAML, noopCustomizer), "Malformed YAML must raise JsonParseException");
    }

    /**
     * 19. Invalid percent‑encoding – expect an {@link IllegalArgumentException} or {@link UnsupportedEncodingException}.
     */
    @Test
    void invalidPercentEncoding_ThrowsException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        // Incomplete percent sequence
        String badEncoding = "%E4%BD%A0%E5%A5";
        Exception ex = assertThrows(RuntimeException.class, () -> schema.validate(badEncoding, InputFormat.JSON, noopCustomizer));
        assertTrue(ex instanceof IllegalArgumentException || ex instanceof UnsupportedEncodingException, "Expected IllegalArgumentException or UnsupportedEncodingException for bad percent‑encoding");
    }

    /**
     * 20. Empty URL‑encoded input – after decoding we get an empty string, which cannot be parsed.
     */
    @Test
    void emptyUrlEncodedInput_ThrowsJsonParseException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        // decoding an empty string yields empty string
        String decoded = "";
        assertThrows(JsonParseException.class, () -> schema.validate(decoded, InputFormat.JSON, noopCustomizer), "Empty URL‑encoded input should lead to a parsing exception");
    }

    /**
     * 21. Whitespace‑only URL‑encoded input – decoding yields whitespace, which is invalid JSON/YAML.
     */
    @Test
    void whitespaceUrlEncodedInput_ThrowsJsonParseException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        String whitespaceEncoded = "%20%20%20%0A%20%20";
        String decoded = URLDecoder.decode(whitespaceEncoded, StandardCharsets.UTF_8.name());
        assertThrows(JsonParseException.class, () -> schema.validate(decoded, InputFormat.JSON, noopCustomizer), "Whitespace after URL decoding must raise a parsing exception");
    }

    /**
     * 22. Customizer sets an illegal configuration (negative max depth) – validation should reject it.
     */
    @Test
    void customizerInvalidConfiguration_ThrowsIllegalArgumentException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        ExecutionContextCustomizer badConfig = ctx -> ctx.setMaxDepth(-1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> schema.validate("{}", InputFormat.JSON, badConfig));
        assertTrue(ex.getMessage().toLowerCase().contains("maxdepth"), "Exception message should indicate the illegal maxDepth value");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_11_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_11_Test.java:15: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_validate_11_Test.java:346: error: cannot find symbol
            ctx.setCustomMessage("myCustomMessage");
               ^
  symbol:   method setCustomMessage(String)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_11_Test.java:345: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer customizer = ctx -> {
                                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_11_Test.java:353: error: cannot find symbol
        assertEquals("myCustomMessage", captured[0].getCustomMessage(), "Custom message set by the customizer must be retrievable");
                                                   ^
  symbol:   method getCustomMessage()
  location: class ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_11_Test.java:411: error: cannot find symbol
        ExecutionContextCustomizer badConfig = ctx -> ctx.setMaxDepth(-1);
                                                         ^
  symbol:   method setMaxDepth(int)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_11_Test.java:411: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer badConfig = ctx -> ctx.setMaxDepth(-1);
                                               ^
7 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(String, InputFormat, ExecutionContextCustomizer)': '\n     * Validate the given input string using the input format, starting at the root\n     * of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     *\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ', 'dependent classes': 'class `com.networknt.schema.InputFormat`:\n\t\nclass `com.networknt.schema.ExecutionContextCustomizer`:\n\t\nclass `com.networknt.schema.OutputFormat`:\n\tdependent fields:\n\t\tcom.networknt.schema.OutputFormat.Default com.networknt.schema.OutputFormat.DEFAULT;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(JsonNode, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, ExecutionContextCustomizer)`, api document: \n     * Validate the given root JsonNode, starting at the root of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     *\n     * @param rootNode            the root node\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(String, InputFormat)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, ExecutionContextCustomizer)`, api document: \n     * Validate the given input string using the input format, starting at the root\n     * of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param input       the input\n     * @param inputFormat the inputFormat\n     * @return A list of ValidationMessage if there is any validation error, or an\n     *         empty list if there is no error.\n     ;\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, ExecutionContextCustomizer)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>      the result type\n     * @param rootNode the root node\n     * @param format   the formatter\n     * @return the result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, ExecutionContextCustomizer)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, ExecutionContextCustomizer)`, api document: \n     * Validate the given root JsonNode, starting at the root of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param rootNode the root node\n     * @return A list of ValidationMessage if there is any validation error, or an\n     *         empty list if there is no error.\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


