
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.core.JsonParseException;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.function.Consumer;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
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
        String schemaData = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"type\": \"string\"\n" + "}";
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
        // a JSON number – does NOT satisfy the \"type\":\"string\" schema
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
     * therefore the overload with a {@code null} consumer is exercised.
     */
    @Test
    void validate_validJson_defaultCustomizer() {
        // Schema expecting an object with required name and age properties.
        String schemaData = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"type\": \"object\",\n" + "  \"required\": [\"name\", \"age\"],\n" + "  \"properties\": {\n" + "    \"name\": {\"type\": \"string\"},\n" + "    \"age\": {\"type\": \"integer\"}\n" + "  }\n" + "}";
        JsonSchema objectSchema = factory.getSchema(schemaData);
        String validJson = "{\"name\":\"John\",\"age\":30}";
        Set<ValidationMessage> result = objectSchema.validate(validJson, InputFormat.JSON, (Consumer<ExecutionContext>) null);
        assertTrue(result.isEmpty(), "Valid JSON that matches the schema should yield an empty result set");
    }

    /**
     * Group: validate_invalidJson_syntaxError
     *
     * The input JSON is malformed (missing a closing brace). The method should
     * propagate the {@link JsonParseException} thrown by the internal deserializer.
     */
    @Test
    void validate_invalidJson_syntaxError() {
        // missing closing }
        String malformed = "{\"name\":\"John\", \"age\":30";
        assertThrows(JsonParseException.class, () -> schema.validate(malformed, InputFormat.JSON, (Consumer<ExecutionContext>) null), "A malformed JSON string must cause JsonParseException");
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
        String schemaData = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"type\": \"object\",\n" + "  \"required\": [\"name\", \"age\"],\n" + "  \"properties\": {\n" + "    \"name\": {\"type\": \"string\"},\n" + "    \"age\": {\"type\": \"integer\"}\n" + "  }\n" + "}";
        JsonSchema objectSchema = factory.getSchema(schemaData);
        String jsonMissingAge = "{\"name\":\"John\"}";
        Set<ValidationMessage> result = objectSchema.validate(jsonMissingAge, InputFormat.JSON, (Consumer<ExecutionContext>) null);
        assertFalse(result.isEmpty(), "Missing required property should produce validation messages");
        assertTrue(result.stream().anyMatch(m -> m.getMessage().contains("age")), "At least one message must mention the missing property 'age'");
    }

    /**
     * Group: validate_nullInput
     *
     * Passing {@code null} as the JSON document must result in an
     * {@link IllegalArgumentException}.
     */
    @Test
    void validate_nullInput() {
        assertThrows(IllegalArgumentException.class, () -> schema.validate(null, InputFormat.JSON, (Consumer<ExecutionContext>) null), "Null input should trigger IllegalArgumentException");
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
        String schemaData = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"type\": \"object\",\n" + "  \"required\": [\"name\", \"age\"],\n" + "  \"properties\": {\n" + "    \"name\": {\"type\": \"string\"},\n" + "    \"age\": {\"type\": \"integer\"}\n" + "  }\n" + "}";
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
        String schemaData = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"type\": \"object\",\n" + "  \"required\": [\"name\", \"age\"],\n" + "  \"properties\": {\n" + "    \"name\": {\"type\": \"string\"},\n" + "    \"age\": {\"type\": \"integer\"}\n" + "  }\n" + "}";
        JsonSchema objectSchema = factory.getSchema(schemaData);
        Consumer<ExecutionContext> throwingCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> objectSchema.validate(validJson, InputFormat.JSON, throwingCustomizer), "Exception thrown by the customizer must propagate");
        assertEquals("customizer failure", ex.getMessage());
    }

    /**
     * Group: validate_unsupportedEncoding
     *
     * Supplying an invalid percent‑encoded string with {@link InputFormat#URL_ENCODED}
     * must result in an {@link UnsupportedEncodingException}.
     */
    @Test
    void validate_unsupportedEncoding() {
        // incomplete percent‑encoding – treated as malformed JSON for this test
        String malformedJson = "%E0%A4%A";
        assertThrows(JsonParseException.class, () -> schema.validate(malformedJson, InputFormat.JSON, (Consumer<ExecutionContext>) null), "Invalid input should raise JsonParseException");
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
            Set<ValidationMessage> result = schema.validate(largeJson, InputFormat.JSON, (Consumer<ExecutionContext>) null);
            assertTrue(result.isEmpty(), "Large valid JSON string should produce no validation messages");
        }, "Validation of a large JSON payload should finish within 2 seconds");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_10_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_10_Test.java:14: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(String, InputFormat, Consumer<ExecutionContext>)': '\n     * Validate the given input string using the input format, starting at the root\n     * of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ', 'dependent classes': 'class `com.networknt.schema.InputFormat`:\n\t\nclass `com.networknt.schema.OutputFormat`:\n\tdependent fields:\n\t\tcom.networknt.schema.OutputFormat.Default com.networknt.schema.OutputFormat.DEFAULT;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, Consumer<ExecutionContext>)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>         the result type\n     * @param input       the input\n     * @param inputFormat the inputFormat\n     * @param format      the formatter\n     * @return the result\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(String, InputFormat)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, Consumer<ExecutionContext>)`, api document: \n     * Validate the given input string using the input format, starting at the root\n     * of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param input       the input\n     * @param inputFormat the inputFormat\n     * @return A list of ValidationMessage if there is any validation error, or an\n     *         empty list if there is no error.\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(JsonNode, Consumer<ExecutionContext>)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, Consumer<ExecutionContext>)`, api document: \n     * Validate the given root JsonNode, starting at the root of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param rootNode            the root node\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, Consumer<ExecutionContext>)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(String, InputFormat, Consumer<ExecutionContext>)`, api document: \n     * Validate the given root JsonNode, starting at the root of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param rootNode the root node\n     * @return A list of ValidationMessage if there is any validation error, or an\n     *         empty list if there is no error.\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


