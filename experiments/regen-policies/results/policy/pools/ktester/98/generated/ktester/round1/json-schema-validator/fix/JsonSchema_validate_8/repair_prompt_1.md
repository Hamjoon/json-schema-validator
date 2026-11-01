
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.*;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Set;
import java.util.ArrayList;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_8_Test {

    /**
     * Factory used for creating {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    /**
     * A simple schema that expects an object with a required property {@code name}.
     */
    private static JsonSchema schema;

    /**
     * A simple schema that expects an object with a required property {@code name}.
     */
    private static JsonSchema simpleSchema;

    /**
     * Schema that requires both {@code age} and {@code name}.
     */
    private static JsonSchema requiredAgeAndNameSchema;

    /**
     * Schema that disallows additional properties.
     */
    private static JsonSchema noAdditionalPropsSchema;

    /**
     * Schema with an unresolved $ref (used for the last test case).
     */
    private static JsonSchema unresolvedRefSchema;

    /**
     * Collects the validation messages into a {@link List}.
     */
    private static final OutputFormat<List<ValidationMessage>> COLLECTING_FORMAT = messages -> new ArrayList<>(messages);

    /**
     * Returns the number of validation messages.
     */
    private static final OutputFormat<Integer> COUNTING_FORMAT = Set::size;

    /**
     * Throws an unchecked exception when invoked – used to test propagation.
     */
    private static final OutputFormat<Object> EXCEPTION_THROWING_FORMAT = messages -> {
        throw new IllegalArgumentException("Intentional exception from OutputFormat");
    };

    /**
     * Jackson mapper used throughout the tests.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // 1. simpleSchema – only "name" required
        simpleSchema = factory.getSchema("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "required": [ "name" ]
            }
            """);
        // 2. schema – same as simpleSchema (used by the first two tests)
        schema = simpleSchema;
        // 3. requiredAgeAndNameSchema – both "age" (integer) and "name" (string) required
        requiredAgeAndNameSchema = factory.getSchema("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" },
                "name": { "type": "string" }
              },
              "required": [ "age", "name" ]
            }
            """);
        // 4. noAdditionalPropsSchema – same as requiredAgeAndName but disallow extra fields
        noAdditionalPropsSchema = factory.getSchema("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" },
                "name": { "type": "string" }
              },
              "required": [ "age", "name" ],
              "additionalProperties": false
            }
            """);
        // 5. unresolvedRefSchema – $ref points to a non‑existent definition
        unresolvedRefSchema = factory.getSchema("""
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "#/definitions/NonExistent"
            }
            """);
    }

    /**
     * Verify that a valid JSON node results in an empty {@link Set} of
     * {@link ValidationMessage}s and that the supplied {@code Consumer}
     * is invoked exactly once with the {@link ExecutionContext} created by the
     * {@link JsonSchema}.
     */
    @Test
    void validate_validNode_callsCustomizer_andReturnsEmptySet(@Mock Consumer<ExecutionContext> customizer) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"name\":\"John Doe\"}");
        // a simple consumer that records how many times it is called
        AtomicInteger calls = new AtomicInteger(0);
        Consumer<ExecutionContext> customizer = ctx -> calls.incrementAndGet();
        Set<ValidationMessage> messages = schema.validate(validNode, OutputFormat.DEFAULT, customizer);
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid node");
        assertEquals(1, calls.get(), "Customizer should be called exactly once");
    }

    /**
     * Verify that an invalid JSON node yields validation messages while the
     * customizer is still invoked. The schema requires the property {@code name},
     * so omitting it should produce exactly one message.
     */
    @Test
    void validate_invalidNode_callsCustomizer_andReturnsMessages(@Mock Consumer<ExecutionContext> customizer) throws Exception {
        JsonNode invalidNode = MAPPER.readTree("{\"age\":30}");
        AtomicInteger calls = new AtomicInteger(0);
        Consumer<ExecutionContext> customizer = ctx -> calls.incrementAndGet();
        Set<ValidationMessage> messages = schema.validate(invalidNode, OutputFormat.DEFAULT, customizer);
        assertEquals(1, messages.size(), "Expected exactly one validation message for missing required property");
        ValidationMessage msg = messages.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("required"), "Validation message should mention the required property");
        assertEquals(1, calls.get(), "Customizer should be called exactly once");
    }

    // ---------- Successful validation – schema matches ----------
    @Test
    void successfulValidation_returnsEmptyList() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":30,\"name\":\"Alice\"}");
        List<ValidationMessage> result = simpleSchema.validate(node, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        });
        assertTrue(result.isEmpty(), "A valid instance should produce an empty list of messages");
    }

    // ---------- Validation failures – schema violations ----------
    @Test
    void validationFailures_collectMultipleMessages() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":\"thirty\",\"name\":null}");
        List<ValidationMessage> result = requiredAgeAndNameSchema.validate(node, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        });
        assertTrue(result.size() >= 2, "Expected at least two validation messages");
        boolean ageType = result.stream().anyMatch(m -> m.getPath().endsWith("age"));
        boolean nameRequired = result.stream().anyMatch(m -> m.getPath().endsWith("name"));
        assertTrue(ageType && nameRequired, "Messages should reference both 'age' and 'name'");
    }

    // ---------- Null rootNode handling ----------
    @Test
    void nullRootNode_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> simpleSchema.validate((JsonNode) null, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        }));
    }

    // ---------- Null OutputFormat handling ----------
    @Test
    void nullOutputFormat_throwsNullPointerException() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":25}");
        assertThrows(NullPointerException.class, () -> simpleSchema.validate(node, null, ctx -> {
            /* no‑op */
        }));
    }

    // ---------- Consumer throws exception ----------
    @Test
    void consumerThrows_exceptionPropagates() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":20}");
        Consumer<ExecutionContext> explodingConsumer = ctx -> {
            throw new IllegalStateException("Consumer failure");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> simpleSchema.validate(node, COLLECTING_FORMAT, explodingConsumer));
        assertEquals("Consumer failure", ex.getMessage());
    }

    // ---------- Consumer is invoked (side‑effect verification) ----------
    @Test
    void consumerSideEffect_isObserved() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":40}");
        AtomicBoolean flag = new AtomicBoolean(false);
        Consumer<ExecutionContext> sideEffectConsumer = ctx -> flag.set(true);
        simpleSchema.validate(node, COLLECTING_FORMAT, sideEffectConsumer);
        assertTrue(flag.get(), "Consumer should have set the flag to true");
    }

    // ---------- Non‑object rootNode (array) – type mismatch handling ----------
    @Test
    void arrayRootNode_reportsTypeMismatch() throws Exception {
        JsonNode arrayNode = MAPPER.readTree("[{\"age\":30}]");
        List<ValidationMessage> result = simpleSchema.validate(arrayNode, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        });
        assertFalse(result.isEmpty(), "Array root should produce at least one validation message");
        boolean typeMismatch = result.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(typeMismatch, "Message should indicate a type mismatch (expected object)");
    }

    // ---------- Empty object – required property violations ----------
    @Test
    void emptyObject_reportsAllMissingRequired() throws Exception {
        JsonNode empty = MAPPER.readTree("{}");
        List<ValidationMessage> result = requiredAgeAndNameSchema.validate(empty, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        });
        // Two required properties -> two messages
        assertEquals(2, result.size(), "Empty object should generate two required‑property messages");
    }

    // ---------- Additional properties not allowed – unexpected field detection ----------
    @Test
    void additionalPropertiesDisallowed_reportsUnexpectedField() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":30,\"name\":\"Bob\",\"extra\":\"value\"}");
        List<ValidationMessage> result = noAdditionalPropsSchema.validate(node, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        });
        boolean extraDetected = result.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("additional"));
        assertTrue(extraDetected, "Expected a message about the disallowed 'extra' property");
    }

    // ---------- Null Consumer handling – fail‑fast behavior ----------
    @Test
    void nullConsumer_throwsNullPointerException() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":25,\"name\":\"Eve\"}");
        assertThrows(NullPointerException.class, () -> simpleSchema.validate(node, COLLECTING_FORMAT, (Consumer<ExecutionContext>) null));
    }

    // ---------- Custom OutputFormat – returns transformed result ----------
    @Test
    void countingOutputFormat_returnsZeroForValidInstance() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":30,\"name\":\"Alice\"}");
        Integer count = requiredAgeAndNameSchema.validate(node, COUNTING_FORMAT, ctx -> {
            /* no‑op */
        });
        assertEquals(0, count, "Valid instance should result in count 0");
    }

    @Test
    void countingOutputFormat_countsInvalidMessages() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":\"thirty\",\"name\":null}");
        Integer count = requiredAgeAndNameSchema.validate(node, COUNTING_FORMAT, ctx -> {
            /* no‑op */
        });
        assertTrue(count >= 2, "Invalid instance should produce count >= 2");
    }

    // ---------- OutputFormat throws unchecked exception – propagation test ----------
    @Test
    void outputFormatThrows_exceptionPropagates() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":30,\"name\":\"Alice\"}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> simpleSchema.validate(node, EXCEPTION_THROWING_FORMAT, ctx -> {
            /* no‑op */
        }));
        assertEquals("Intentional exception from OutputFormat", ex.getMessage());
    }

    // ---------- ExecutionContext customization – fail‑fast mode ----------
    @Test
    void failFastMode_stopsAfterFirstError() throws Exception {
        JsonNode node = MAPPER.readTree("{\"age\":\"thirty\",\"name\":\"Bob\"}");
        List<ValidationMessage> result = requiredAgeAndNameSchema.validate(node, COLLECTING_FORMAT, ctx -> ctx.setFailFast(true));
        // With fail‑fast we expect exactly one message
        assertEquals(1, result.size(), "Fail‑fast should stop after the first validation error");
    }

    // ---------- Unresolved $ref handling – exception propagation ----------
    @Test
    void unresolvedRef_throwsIllegalArgumentException() throws Exception {
        JsonNode node = MAPPER.readTree("{\"valid\":true}");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> unresolvedRefSchema.validate(node, COLLECTING_FORMAT, ctx -> {
            /* no‑op */
        }));
        assertNotNull(ex.getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:9: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:11: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:19: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:128: error: cannot find symbol
    void validate_validNode_callsCustomizer_andReturnsEmptySet(@Mock Consumer<ExecutionContext> customizer) throws Exception {
                                                                ^
  symbol:   class Mock
  location: class JsonSchema_validate_8_Test
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:144: error: cannot find symbol
    void validate_invalidNode_callsCustomizer_andReturnsMessages(@Mock Consumer<ExecutionContext> customizer) throws Exception {
                                                                  ^
  symbol:   class Mock
  location: class JsonSchema_validate_8_Test
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:55: error: cannot infer type arguments for ArrayList<>
    private static final OutputFormat<List<ValidationMessage>> COLLECTING_FORMAT = messages -> new ArrayList<>(messages);
                                                                                               ^
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:55: error: incompatible types: incompatible parameter types in lambda expression
    private static final OutputFormat<List<ValidationMessage>> COLLECTING_FORMAT = messages -> new ArrayList<>(messages);
                                                                                   ^
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:60: error: incompatible types: invalid method reference
    private static final OutputFormat<Integer> COUNTING_FORMAT = Set::size;
                                                                 ^
    method size in interface Set<E> cannot be applied to given types
      required: no arguments
      found:    JsonSchema,Set<ValidationMessage>,ExecutionContext,ValidationContext
      reason: actual and formal argument lists differ in length
  where E is a type-variable:
    E extends Object declared in interface Set
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:65: error: incompatible types: incompatible parameter types in lambda expression
    private static final OutputFormat<Object> EXCEPTION_THROWING_FORMAT = messages -> {
                                                                          ^
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:132: error: variable customizer is already defined in method validate_validNode_callsCustomizer_andReturnsEmptySet(Consumer<ExecutionContext>)
        Consumer<ExecutionContext> customizer = ctx -> calls.incrementAndGet();
                                   ^
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:147: error: variable customizer is already defined in method validate_invalidNode_callsCustomizer_andReturnsMessages(Consumer<ExecutionContext>)
        Consumer<ExecutionContext> customizer = ctx -> calls.incrementAndGet();
                                   ^
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:173: error: cannot find symbol
        boolean ageType = result.stream().anyMatch(m -> m.getPath().endsWith("age"));
                                                         ^
  symbol:   method getPath()
  location: variable m of type ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_validate_8_Test.java:174: error: cannot find symbol
        boolean nameRequired = result.stream().anyMatch(m -> m.getPath().endsWith("name"));
                                                              ^
  symbol:   method getPath()
  location: variable m of type ValidationMessage
13 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)': '\n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\nclass `com.networknt.schema.SchemaValidatorsConfig.Builder`:\n\trelated functions:\n\t\tmethod `public Builder itemWalkListeners(Consumer<List<JsonSchemaWalkListener>>)` returns `com.networknt.schema.SchemaValidatorsConfig.Builder`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


