```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.*;

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
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(
                        Collections.singletonMap("http://example.org/ref.json",
                                // reference schema – a simple object with a required string property
                                "{\n" + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                                        + "  \"$id\": \"http://example.org/ref.json\",\n"
                                        + "  \"type\": \"object\",\n"
                                        + "  \"properties\": {\n"
                                        + "    \"name\": { \"type\": \"string\" }\n"
                                        + "  },\n"
                                        + "  \"required\": [\"name\"]\n" + "}"))));
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
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n"
                + "  \"$id\": \"http://example.org/schema.json\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"$ref\": \"http://example.org/ref.json\"\n"
                + "}";
        schema = createSchema(schemaData);
        // Input that violates the referenced schema (name must be a string).
        String inputData = "{ \"name\": 1 }";
        Set<ValidationMessage> messages = schema.validate(inputData, InputFormat.JSON, customizer);
        assertEquals(1, messages.size(), "Expected a single validation error");
        ValidationMessage message = messages.iterator().next();
        assertTrue(message.getMessage().toLowerCase().contains("type"),
                "Expected the validation message to mention a type error");
    }

    /**
     * A concurrency sanity check that also uses the customizer overload.
     * This mirrors the existing concurrency test but exercises the three‑argument
     * method.
     */
    @Test
    void concurrency_WithCustomizer() throws Exception {
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n"
                + "  \"$id\": \"http://example.org/schema.json\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"$ref\": \"http://example.org/ref.json\"\n"
                + "}";
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
        String schemaData = "{\n"
                + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": {\"type\":\"string\"},\n"
                + "    \"age\": {\"type\":\"integer\"}\n"
                + "  },\n"
                + "  \"required\": [\"name\",\"age\"],\n"
                + "  \"additionalProperties\": false\n"
                + "}";
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
        String schemaData = "{\n"
                + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": {\"type\":\"string\"},\n"
                + "    \"age\": {\"type\":\"integer\"}\n"
                + "  },\n"
                + "  \"required\": [\"name\",\"age\"],\n"
                + "  \"additionalProperties\": false\n"
                + "}";
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
        String schemaData = "{\n"
                + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\"age\": {\"type\":\"integer\"}},\n"
                + "  \"required\": [\"age\"]\n"
                + "}";
        schema = createSchema(schemaData);
        String input = "{\"age\":\"thirty\"}";
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, noopCustomizer);
        assertEquals(1, msgs.size(), "Expected exactly one validation error");
        ValidationMessage msg = msgs.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("type"),
                "Message should mention a type error");
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
        assertThrows(JsonParseException.class,
                () -> schema.validate(malformed, InputFormat.JSON, noopCustomizer),
                "Malformed JSON must result in a JsonParseException");
    }

    /**
     * 5. Null input string – should throw {@link IllegalArgumentException}.
     */
    @Test
    void nullInput_ThrowsIllegalArgumentException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(null, InputFormat.JSON, noopCustomizer),
                "Passing null as input must raise IllegalArgumentException");
    }

    /**
     * 6. Null {@link InputFormat} – should throw {@link IllegalArgumentException}.
     */
    @Test
    void nullInputFormat_ThrowsIllegalArgumentException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate("{}", null, noopCustomizer),
                "Null InputFormat must raise IllegalArgumentException");
    }

    /**
     * 7. Valid URL‑encoded JSON – should return an empty set.
     */
    @Test
    void urlEncodedValidJson_ReturnsEmptySet() throws Exception {
        String schemaData = "{\n"
                + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\"name\": {\"type\":\"string\"}, \"age\": {\"type\":\"integer\"}},\n"
                + "  \"required\": [\"name\",\"age\"]\n"
                + "}";
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
    void urlEncodedInvalidContent_ThrowsParsingException() throws Exception {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        // "你好" – not JSON/YAML
        String urlEncoded = "%E4%BD%A0%E5%A5%BD";
        String decoded = URLDecoder.decode(urlEncoded, StandardCharsets.UTF_8.name());
        assertThrows(JsonParseException.class,
                () -> schema.validate(decoded, InputFormat.JSON, noopCustomizer),
                "Decoding to non‑JSON should result in a parsing exception");
    }

    /**
     * 9. Empty string with JSON format – parsing exception expected.
     */
    @Test
    void emptyStringJson_ThrowsJsonParseException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        assertThrows(JsonParseException.class,
                () -> schema.validate("", InputFormat.JSON, noopCustomizer),
                "Empty string for JSON input must raise JsonParseException");
    }

    /**
     * 10. Whitespace‑only JSON – parsing exception expected.
     */
    @Test
    void whitespaceOnlyJson_ThrowsJsonParseException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        assertThrows(JsonParseException.class,
                () -> schema.validate("   \n   ", InputFormat.JSON, noopCustomizer),
                "Whitespace‑only JSON must raise JsonParseException");
    }

    /**
     * 11. Additional property not allowed – expect a specific validation message.
     */
    @Test
    void additionalProperty_NotAllowed_ReturnsMessage() {
        String schemaData = "{\n"
                + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\"name\": {\"type\":\"string\"}, \"age\": {\"type\":\"integer\"}},\n"
                + "  \"required\": [\"name\",\"age\"],\n"
                + "  \"additionalProperties\": false\n"
                + "}";
        schema = createSchema(schemaData);
        String input = "{\"name\":\"John\",\"age\":30,\"extra\":\"value\"}";
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, noopCustomizer);
        assertEquals(1, msgs.size(), "One error expected for additional property");
        ValidationMessage msg = msgs.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("additional"),
                "Message should mention additional property");
    }

    /**
     * 12. Missing required property – expect a required‑property message.
     */
    @Test
    void missingRequiredProperty_ReturnsMessage() {
        String schemaData = "{\n"
                + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\"name\": {\"type\":\"string\"}, \"age\": {\"type\":\"integer\"}},\n"
                + "  \"required\": [\"name\",\"age\"]\n"
                + "}";
        schema = createSchema(schemaData);
        String input = "{\"name\":\"John\"}";
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, noopCustomizer);
        assertEquals(1, msgs.size(), "One error expected for missing required property");
        ValidationMessage msg = msgs.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("required"),
                "Message should mention required property");
    }

    /**
     * 13. Null value for a non‑nullable property – expect a type‑mismatch message.
     */
    @Test
    void nullValueForNonNullableProperty_ReturnsMessage() {
        String schemaData = "{\n"
                + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\"name\": {\"type\":\"string\"}, \"age\": {\"type\":\"integer\"}},\n"
                + "  \"required\": [\"name\",\"age\"]\n"
                + "}";
        schema = createSchema(schemaData);
        String input = "{\"name\":null,\"age\":30}";
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, noopCustomizer);
        assertEquals(1, msgs.size(), "One error expected for null 'name'");
        ValidationMessage msg = msgs.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("type"),
                "Message should mention a type error for null value");
    }

    /**
     * 14. Fail‑fast customizer – validation should stop after the first error.
     */
    @Test
    void failFastCustomizer_StopsAfterFirstError() {
        String schemaData = "{\n"
                + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": {\"type\":\"string\"},\n"
                + "    \"age\": {\"type\":\"integer\"},\n"
                + "    \"city\": {\"type\":\"string\"}\n"
                + "  },\n"
                + "  \"required\": [\"name\",\"age\"],\n"
                + "  \"additionalProperties\": false\n"
                + "}";
        schema = createSchema(schemaData);
        String input = "{\"name\":\"John\",\"age\":\"thirty\",\"city\":\"NY\"}";
        ExecutionContextCustomizer failFast = (ctx, vc) -> ctx.setFailFast(true);
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, failFast);
        // With fail‑fast we expect exactly one error.
        assertEquals(1, msgs.size(), "Fail‑fast should limit the result to a single message");
    }

    /**
     * 15. Report‑all customizer – both errors should be reported.
     * The library reports all errors by default, so we simply reuse the no‑op
     * customizer and verify that more than one message is returned.
     */
    @Test
    void reportAllCustomizer_CollectsAllErrors() {
        String schemaData = "{\n"
                + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": {\"type\":\"string\"},\n"
                + "    \"age\": {\"type\":\"integer\"},\n"
                + "    \"city\": {\"type\":\"string\"}\n"
                + "  },\n"
                + "  \"required\": [\"name\",\"age\"],\n"
                + "  \"additionalProperties\": false\n"
                + "}";
        schema = createSchema(schemaData);
        String input = "{\"name\":\"John\",\"age\":\"thirty\",\"city\":\"NY\"}";
        Set<ValidationMessage> msgs = schema.validate(input, InputFormat.JSON, noopCustomizer);
        assertTrue(msgs.size() >= 2,
                "When reportAll is true (default) we expect at least two messages (type + additional)");
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
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate("{\"a\":1}", InputFormat.JSON, exploding));
        assertEquals("Customizer failure", ex.getMessage(),
                "Exception message from customizer should be preserved");
    }

    /**
     * 18. Malformed YAML – should result in a parsing exception.
     */
    @Test
    void malformedYaml_ThrowsJsonParseException() {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        String malformedYaml = "name: John\nage: 30: extra";
        assertThrows(JsonParseException.class,
                () -> schema.validate(malformedYaml, InputFormat.YAML, noopCustomizer),
                "Malformed YAML must raise JsonParseException");
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
        Exception ex = assertThrows(RuntimeException.class,
                () -> schema.validate(badEncoding, InputFormat.JSON, noopCustomizer));
        assertTrue(ex instanceof IllegalArgumentException || ex instanceof UnsupportedEncodingException,
                "Expected IllegalArgumentException or UnsupportedEncodingException for bad percent‑encoding");
    }

    /**
     * 20. Empty URL‑encoded input – after decoding we get an empty string, which cannot be parsed.
     */
    @Test
    void emptyUrlEncodedInput_ThrowsJsonParseException() throws Exception {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        String decoded = ""; // decoding an empty string yields empty string
        assertThrows(JsonParseException.class,
                () -> schema.validate(decoded, InputFormat.JSON, noopCustomizer),
                "Empty URL‑encoded input should lead to a parsing exception");
    }

    /**
     * 21. Whitespace‑only URL‑encoded input – decoding yields whitespace, which is invalid JSON/YAML.
     */
    @Test
    void whitespaceUrlEncodedInput_ThrowsJsonParseException() throws Exception {
        String schemaData = "{ \"type\": \"object\" }";
        schema = createSchema(schemaData);
        String whitespaceEncoded = "%20%20%20%0A%20%20";
        String decoded = URLDecoder.decode(whitespaceEncoded, StandardCharsets.UTF_8.name());
        assertThrows(JsonParseException.class,
                () -> schema.validate(decoded, InputFormat.JSON, noopCustomizer),
                "Whitespace after URL decoding must raise a parsing exception");
    }
}
```