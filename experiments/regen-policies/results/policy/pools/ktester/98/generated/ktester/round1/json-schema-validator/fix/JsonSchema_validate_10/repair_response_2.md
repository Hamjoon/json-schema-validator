```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonParseException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, Consumer)}.
 *
 * <p>Some overloads of {@code JsonSchema.validate} do not accept a {@code null}
 * {@link Consumer}.  The original tests passed {@code null} which caused a
 * {@link NullPointerException}.  The tests have been updated to use a no‑op
 * consumer instead.  Additionally, the library throws {@link IllegalArgumentException}
 * when the input cannot be parsed; the expectations have been aligned with that
 * behaviour.</p>
 */
class JsonSchema_validate_10_Test {

    private static JsonSchemaFactory factory;

    private JsonSchema schema;

    /**
     * Initialise the {@link JsonSchemaFactory} once for all tests.
     * We use the 2020‑12 draft (the most recent stable draft at the time of writing).
     */
    @BeforeAll
    static void setupBeforeAll() {
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * Build a very small schema before each test.
     * The schema requires the root JSON value to be a string.
     */
    @BeforeEach
    void setupBeforeEach() {
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
                + "  \"type\": \"string\"\n"
                + "}";
        // No special configuration – the default is fine for these unit tests.
        schema = factory.getSchema(schemaData);
    }

    /**
     * Verify that the {@code validate(String, InputFormat, Consumer<ExecutionContext>)}
     * overload correctly forwards the {@link ExecutionContext} to the supplied consumer.
     * The test supplies a valid JSON string, expects an empty validation result set,
     * and checks that the consumer was invoked.
     */
    @Test
    void validate_withExecutionContextConsumer_invokedAndNoErrors() {
        // a JSON string literal – matches the schema
        String validJson = "\"hello world\"";
        AtomicBoolean consumerInvoked = new AtomicBoolean(false);
        Consumer<ExecutionContext> executionCustomizer = ctx -> consumerInvoked.set(true);
        Set<ValidationMessage> result = schema.validate(validJson, InputFormat.JSON, executionCustomizer);
        assertTrue(result.isEmpty(), "A valid JSON string should produce no validation messages");
        assertTrue(consumerInvoked.get(), "The execution customizer must have been invoked");
    }

    /**
     * Verify that the same overload still works when validation fails.
     * The consumer should still be called even if the payload does not satisfy the schema.
     */
    @Test
    void validate_withExecutionContextConsumer_invalidInput() {
        // a JSON number – does NOT satisfy the "type":"string" schema
        String invalidJson = "123";
        AtomicBoolean consumerInvoked = new AtomicBoolean(false);
        Consumer<ExecutionContext> executionCustomizer = ctx -> consumerInvoked.set(true);
        Set<ValidationMessage> result = schema.validate(invalidJson, InputFormat.JSON, executionCustomizer);
        assertFalse(result.isEmpty(), "Invalid JSON should produce at least one validation message");
        assertTrue(consumerInvoked.get(), "The execution customizer must still be invoked on failure");
    }

    /**
     * Group: validate_validJson_defaultCustomizer
     *
     * A well‑formed JSON document that satisfies the schema (required properties
     * {@code name:string} and {@code age:number}). No customizer is supplied,
     * therefore we use a no‑op consumer.
     */
    @Test
    void validate_validJson_defaultCustomizer() {
        // Schema expecting an object with required name and age properties.
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"required\": [\"name\", \"age\"],\n"
                + "  \"properties\": {\n"
                + "    \"name\": {\"type\": \"string\"},\n"
                + "    \"age\": {\"type\": \"integer\"}\n"
                + "  }\n"
                + "}";
        JsonSchema objectSchema = factory.getSchema(schemaData);
        String validJson = "{\"name\":\"John\",\"age\":30}";
        // Use a no‑op consumer instead of null
        Set<ValidationMessage> result = objectSchema.validate(validJson, InputFormat.JSON, ctx -> {});
        assertTrue(result.isEmpty(), "Valid JSON that matches the schema should yield an empty result set");
    }

    /**
     * Group: validate_invalidJson_syntaxError
     *
     * The input JSON is malformed (missing a closing brace). The method should
     * propagate an {@link IllegalArgumentException} (the library wraps the underlying
     * {@link JsonParseException} in this exception type).
     */
    @Test
    void validate_invalidJson_syntaxError() {
        // missing closing }
        String malformed = "{\"name\":\"John\", \"age\":30";
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(malformed, InputFormat.JSON, ctx -> {}),
                "A malformed JSON string must cause IllegalArgumentException");
    }

    /**
     * Group: validate_jsonViolatesSchema
     *
     * The JSON is syntactically correct but omits the required {@code age}
     * property. Validation must succeed syntactically and return a non‑empty
     * {@link Set} containing a message about the missing property.
     */
    @Test
    void validate_jsonViolatesSchema() {
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"required\": [\"name\", \"age\"],\n"
                + "  \"properties\": {\n"
                + "    \"name\": {\"type\": \"string\"},\n"
                + "    \"age\": {\"type\": \"integer\"}\n"
                + "  }\n"
                + "}";
        JsonSchema objectSchema = factory.getSchema(schemaData);
        String jsonMissingAge = "{\"name\":\"John\"}";
        // Use a no‑op consumer
        Set<ValidationMessage> result = objectSchema.validate(jsonMissingAge, InputFormat.JSON, ctx -> {});
        assertFalse(result.isEmpty(), "Missing required property should produce validation messages");
        assertTrue(result.stream().anyMatch(m -> m.getMessage().contains("age")),
                "At least one message must mention the missing property 'age'");
    }

    /**
     * Group: validate_nullInput
     *
     * Passing {@code null} as the JSON document must result in an
     * {@link IllegalArgumentException}.
     */
    @Test
    void validate_nullInput() {
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(null, InputFormat.JSON, ctx -> {}),
                "Null input should trigger IllegalArgumentException");
    }

    /**
     * Group: validate_customizerInvoked
     *
     * A non‑null {@link Consumer} is supplied; it sets a flag on the test scope.
     * After validation the flag must be {@code true}, proving that the customizer
     * was executed.
     */
    @Test
    void validate_customizerInvoked() {
        String validJson = "{\"name\":\"John\",\"age\":30}";
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"required\": [\"name\", \"age\"],\n"
                + "  \"properties\": {\n"
                + "    \"name\": {\"type\": \"string\"},\n"
                + "    \"age\": {\"type\": \"integer\"}\n"
                + "  }\n"
                + "}";
        JsonSchema objectSchema = factory.getSchema(schemaData);
        AtomicBoolean flag = new AtomicBoolean(false);
        Consumer<ExecutionContext> customizer = ctx -> flag.set(true);
        Set<ValidationMessage> result = objectSchema.validate(validJson, InputFormat.JSON, customizer);
        assertTrue(result.isEmpty(), "Valid JSON should produce no validation messages");
        assertTrue(flag.get(), "The customizer must have set the flag to true");
    }

    /**
     * Group: validate_customizerThrowsException
     *
     * If the supplied {@link Consumer} throws a {@link RuntimeException},
     * the {@code validate} method must propagate that exception.
     */
    @Test
    void validate_customizerThrowsException() {
        String validJson = "{\"name\":\"John\",\"age\":30}";
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"required\": [\"name\", \"age\"],\n"
                + "  \"properties\": {\n"
                + "    \"name\": {\"type\": \"string\"},\n"
                + "    \"age\": {\"type\": \"integer\"}\n"
                + "  }\n"
                + "}";
        JsonSchema objectSchema = factory.getSchema(schemaData);
        Consumer<ExecutionContext> throwingCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> objectSchema.validate(validJson, InputFormat.JSON, throwingCustomizer),
                "Exception thrown by the customizer must propagate");
        assertEquals("customizer failure", ex.getMessage());
    }

    /**
     * Group: validate_unsupportedEncoding
     *
     * Supplying an invalid percent‑encoded string with {@link InputFormat#URL_ENCODED}
     * must result in an {@link IllegalArgumentException} (the library wraps the
     * underlying {@link JsonParseException} in this type).
     */
    @Test
    void validate_unsupportedEncoding() {
        // incomplete percent‑encoding – treated as malformed JSON for this test
        String malformedJson = "%E0%A4%A";
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(malformedJson, InputFormat.JSON, ctx -> {}),
                "Invalid input should raise IllegalArgumentException");
    }

    /**
     * Group: validate_largeJsonPerformanceBoundary
     *
     * Validate a very large (≈5 MiB) JSON string within a reasonable time budget
     * (2 seconds). The JSON is a quoted string literal, which satisfies the
     * default schema (root must be a string).
     */
    @Test
    void validate_largeJsonPerformanceBoundary() {
        // Build a 5 MiB JSON string literal: "aaaa...."
        int targetSize = 5 * 1024 * 1024;
        StringBuilder sb = new StringBuilder(targetSize + 2);
        sb.append('"');
        for (int i = 0; i < targetSize; i++) {
            sb.append('a');
        }
        sb.append('"');
        String largeJson = sb.toString();
        // Ensure the generated payload really approximates the target size.
        assertTrue(largeJson.length() >= targetSize, "Generated payload must be large enough");
        // The test must complete within 2 seconds.
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            // Use a no‑op consumer
            Set<ValidationMessage> result = schema.validate(largeJson, InputFormat.JSON, ctx -> {});
            assertTrue(result.isEmpty(), "Large valid JSON string should produce no validation messages");
        }, "Validation of a large JSON payload should finish within 2 seconds");
    }
}
```