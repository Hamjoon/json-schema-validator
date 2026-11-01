
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Comparator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_9_Test {

    /**
     * Factory used to create {@link JsonSchema} instances.
     */
    private JsonSchemaFactory factory;

    /**
     * The schema under test.
     */
    private JsonSchema schema;

    /**
     * Jackson mapper for creating {@link JsonNode} instances.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A simple object schema that requires a string property {@code name}.
     */
    private static final String SIMPLE_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "name": { "type": "string" }
          },
          "required": [ "name" ]
        }
        """;

    /**
     * Input JSON that violates the schema (numeric {@code name}).
     */
    private static final String INVALID_INPUT = """
        { "name": 123 }
        """;

    /**
     * A mock customizer – the default behaviour (do‑nothing) is sufficient for the test.
     */
    @Mock
    ExecutionContextCustomizer executionCustomizer;

    /**
     * A mock customizer – the default behaviour (do‑nothing) is sufficient for many tests.
     */
    @Mock
    ExecutionContextCustomizer noopCustomizer;

    /**
     * No‑op customizer used where a customizer is required but no behaviour is needed.
     */
    private static final ExecutionContextCustomizer NOOP_CUSTOMIZER = ctx -> {
        // intentionally empty
    };

    /**
     * Simple {@link OutputFormat} that returns {@code "OK"} when there are no
     * validation messages and {@code "ERROR: …"} when there are.
     */
    private static class SimpleStringFormat implements OutputFormat<String> {

        @Override
        public String format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
            if (messages == null || messages.isEmpty()) {
                return "OK";
            }
            java.util.List<String> msgs = new ArrayList<>();
            messages.stream().sorted(Comparator.comparing(ValidationMessage::getPath)).forEach(m -> msgs.add(m.getPath() + ": " + m.getMessage()));
            return "ERROR: " + String.join("; ", msgs);
        }
    }

    /**
     * {@link OutputFormat} that collects the textual representation of each
     * {@link ValidationMessage} into a {@link java.util.List<String>}.
     */
    private static class CollectingListFormat implements OutputFormat<java.util.List<String>> {

        @Override
        public java.util.List<String> format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
            if (messages == null || messages.isEmpty()) {
                return Collections.emptyList();
            }
            java.util.List<String> list = new ArrayList<>();
            messages.stream().sorted(Comparator.comparing(ValidationMessage::getPath)).forEach(m -> list.add(m.getPath().replaceFirst("^#", "").replaceFirst("^\\.", "") + ": " + m.getMessage()));
            return list;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory for the 2020‑12 draft (the schema declares this draft).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Build the JsonSchema instance from the literal schema string.
        schema = factory.getSchema(SIMPLE_SCHEMA, SchemaValidatorsConfig.builder().build());
        // Ensure the same draft is used for any other schema created in the tests.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * Null handling – rootNode null or format null.
     */
    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(// rootNode == null → NPE
        Arguments.of(null, new SimpleStringFormat(), NOOP_CUSTOMIZER, NullPointerException.class, "When the JSON document (rootNode) is null the method should throw a NullPointerException because validation cannot be performed on a missing node."), // format == null → IllegalArgumentException
        Arguments.of(MAPPER.createObjectNode(), null, NOOP_CUSTOMIZER, IllegalArgumentException.class, "If the OutputFormat argument is null the method must reject the call with an IllegalArgumentException (the format is required to produce a result)."));
    }

    /**
     * Successful validation without a customizer – schema expects an integer “age”.
     */
    static Stream<Arguments> successfulWithoutCustomizerProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """;
        return Stream.of(Arguments.of(schema, """
            { "age": 30 }""", new SimpleStringFormat(), null, "OK"));
    }

    /**
     * Validation with errors (no customizer).
     */
    static Stream<Arguments> validationErrorNoCustomizerProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """;
        return Stream.of(Arguments.of(schema, """
            { "age": "thirty" }""", new SimpleStringFormat(), null, "ERROR: age: integer expected"));
    }

    /**
     * Validation with ExecutionContextCustomizer (fail‑fast true).
     */
    static Stream<Arguments> failFastCustomizerProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" },
                "name": { "type": "string" }
              }
            }
            """;
        ExecutionContextCustomizer failFast = ctx -> {
            ctx.setConfig(new SchemaValidatorsConfig().setFailFast(true));
            return ctx;
        };
        return Stream.of(// valid document → OK
        Arguments.of(schema, """
            { "age": 30, "name": "John" }""", new SimpleStringFormat(), failFast, "OK"), // invalid document → single error because fail‑fast stops after the first
        Arguments.of(schema, """
            { "age": "thirty", "name": 123 }""", new SimpleStringFormat(), failFast, "ERROR: age: integer expected"));
    }

    /**
     * Custom OutputFormat – collector that returns a List<String>.
     */
    static Stream<Arguments> collectingListFormatProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """;
        return Stream.of(// valid document → empty list
        Arguments.of(schema, """
            { "age": 30 }""", new CollectingListFormat(), null, Collections.emptyList()), // invalid document → list with one message
        Arguments.of(schema, """
            { "age": "thirty" }""", new CollectingListFormat(), null, List.of("age: integer expected")));
    }

    /**
     * Missing required property.
     */
    static Stream<Arguments> missingRequiredProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } },
              "required": [ "age" ]
            }
            """;
        return Stream.of(// SimpleStringFormat
        Arguments.of(schema, """
            { }""", new SimpleStringFormat(), null, "ERROR: age: is required"), // CollectingListFormat
        Arguments.of(schema, """
            { }""", new CollectingListFormat(), null, List.of("age: is required")));
    }

    /**
     * Disallowed additional property (additionalProperties:false).
     */
    static Stream<Arguments> additionalPropertiesFalseProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } },
              "additionalProperties": false,
              "required": [ "age" ]
            }
            """;
        String json = """
            { "age": 30, "extra": "value" }""";
        return Stream.of(// SimpleStringFormat
        Arguments.of(schema, json, new SimpleStringFormat(), null, "ERROR: #: extraneous property \"extra\" is not allowed"), // CollectingListFormat
        Arguments.of(schema, json, new CollectingListFormat(), null, List.of("#: extraneous property \"extra\" is not allowed")));
    }

    /**
     * Array element type validation.
     */
    static Stream<Arguments> arrayElementProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "array",
              "items": { "type": "integer" }
            }
            """;
        String json = """
            [1,2,"three"]""";
        return Stream.of(// SimpleStringFormat
        Arguments.of(schema, json, new SimpleStringFormat(), null, "ERROR: [2]: integer expected"), // CollectingListFormat
        Arguments.of(schema, json, new CollectingListFormat(), null, List.of("[2]: integer expected")));
    }

    /**
     * Root node type mismatch (primitive vs object).
     */
    static Stream<Arguments> rootNodeTypeMismatchProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "name": { "type": "string" } }
            }
            """;
        String json = "\"just a string\"";
        return Stream.of(// SimpleStringFormat
        Arguments.of(schema, json, new SimpleStringFormat(), null, "ERROR: #: object expected"), // CollectingListFormat
        Arguments.of(schema, json, new CollectingListFormat(), null, List.of("#: object expected")));
    }

    /**
     * ExecutionContextCustomizer that disables fail‑fast.
     */
    static Stream<Arguments> disableFailFastProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" },
                "name": { "type": "string" }
              }
            }
            """;
        ExecutionContextCustomizer disable = ctx -> {
            ctx.setConfig(new SchemaValidatorsConfig().setFailFast(false));
            return ctx;
        };
        return Stream.of(// CollectingListFormat – both errors collected
        Arguments.of(schema, """
            { "age": "thirty", "name": 123 }""", new CollectingListFormat(), disable, List.of("age: integer expected", "name: string expected")), // SimpleStringFormat – concatenated message
        Arguments.of(schema, """
            { "age": "thirty", "name": 123 }""", new SimpleStringFormat(), disable, "ERROR: age: integer expected; name: string expected"));
    }

    /**
     * $ref handling – UnsupportedEncodingException.
     */
    static Stream<Arguments> refUnsupportedEncodingProvider() {
        // The $ref contains an illegal percent‑encoding sequence.
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "#/components/schemas/Invalid%ZZ"
            }
            """;
        return Stream.of(Arguments.of(schema, """
            { "any": "value" }""", new SimpleStringFormat(), null, UnsupportedEncodingException.class));
    }

    /**
     * $ref handling – IOException on external schema.
     */
    static Stream<Arguments> refIOExceptionProvider() {
        // Reference a non‑existent file.
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "file:/nonexistent/schema.json"
            }
            """;
        return Stream.of(Arguments.of(schema, """
            { "any": "value" }""", new SimpleStringFormat(), null, IOException.class));
    }

    /**
     * ExecutionContextCustomizer exception propagation.
     */
    static Stream<Arguments> customizerExceptionProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """;
        ExecutionContextCustomizer bad = ctx -> {
            throw new IllegalStateException("customizer failure");
        };
        return Stream.of(Arguments.of(schema, """
            { "age": 30 }""", new SimpleStringFormat(), bad, IllegalStateException.class));
    }

    /**
     * OutputFormat exception propagation.
     */
    static Stream<Arguments> formatExceptionProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """;
        OutputFormat<String> faulty = new OutputFormat<>() {

            @Override
            public String format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
                throw new RuntimeException("format failure");
            }
        };
        return Stream.of(Arguments.of(schema, """
            { "age": 30 }""", faulty, null, RuntimeException.class));
    }

    /**
     * Invalid schema definition – IllegalArgumentException during construction.
     */
    static Stream<Arguments> malformedSchemaProvider() {
        // Unsupported type value "unknown".
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "unknown"
            }
            """;
        return Stream.of(Arguments.of(schema, """
            { "age": 30 }""", new SimpleStringFormat(), null, IllegalArgumentException.class));
    }

    /**
     * Validates a {@link JsonNode} using the {@code validate(JsonNode, OutputFormat, ExecutionContextCustomizer)}
     * overload and verifies that a single validation error is produced.
     */
    @Test
    void validateJsonNodeWithCustomizer_returnsExpectedMessages() throws Exception {
        JsonNode rootNode = MAPPER.readTree(INVALID_INPUT);
        Set<ValidationMessage> messages = schema.validate(rootNode, OutputFormat.DEFAULT, NOOP_CUSTOMIZER);
        assertNotNull(messages, "The returned message set must not be null");
        assertEquals(1, messages.size(), () -> "Expected exactly one validation error, but got: " + messages);
    }

    /**
     * The same validation performed without providing a customizer (null is allowed).
     * This mirrors the overload {@code validate(JsonNode, OutputFormat)}.
     */
    @Test
    void validateJsonNodeWithoutCustomizer_behavesIdentically() throws Exception {
        JsonNode rootNode = MAPPER.readTree(INVALID_INPUT);
        Set<ValidationMessage> messages = schema.validate(rootNode, OutputFormat.DEFAULT, (ExecutionContextCustomizer) null);
        assertNotNull(messages);
        assertEquals(1, messages.size());
    }

    /**
     * Demonstrates usage of a custom {@link OutputFormat}.  Here we use the built‑in
     * {@link OutputFormat#COLLECTOR} which aggregates messages into a {@link java.util.List}.
     */
    @Test
    void validateJsonNodeWithCollectorOutputFormat() throws Exception {
        JsonNode rootNode = MAPPER.readTree(INVALID_INPUT);
        // Use the built‑in DEFAULT format which returns a Set<ValidationMessage>
        Set<ValidationMessage> set = schema.validate(rootNode, OutputFormat.DEFAULT, NOOP_CUSTOMIZER);
        assertNotNull(set);
        assertEquals(1, set.size());
        ValidationMessage vm = set.iterator().next();
        assertTrue(vm.getPath().contains("name"), "The validation message should be about the 'name' property");
    }

    @ParameterizedTest(name = "[{index}] Null handling – {4}")
    @MethodSource("nullHandlingProvider")
    void nullHandling(JsonNode rootNode, OutputFormat<?> format, ExecutionContextCustomizer customizer, Class<? extends Throwable> expectedException, String description) {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        if (expectedException != null) {
            assertThrows(expectedException, () -> schema.validate(rootNode, format, customizer), description);
        } else {
            fail("Expected an exception but none was thrown");
        }
    }

    @ParameterizedTest(name = "[{index}] Successful validation without customizer")
    @MethodSource("successfulWithoutCustomizerProvider")
    void successfulWithoutCustomizer(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, String expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Validation error (no customizer)")
    @MethodSource("validationErrorNoCustomizerProvider")
    void validationErrorNoCustomizer(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, String expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Fail‑fast customizer")
    @MethodSource("failFastCustomizerProvider")
    void failFastCustomizer(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, String expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] CollectingListFormat")
    @MethodSource("collectingListFormatProvider")
    void collectingListFormat(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, List<String> expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Missing required property")
    @MethodSource("missingRequiredProvider")
    void missingRequired(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, Object expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Additional properties not allowed")
    @MethodSource("additionalPropertiesFalseProvider")
    void additionalPropertiesFalse(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, Object expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Array element type validation")
    @MethodSource("arrayElementProvider")
    void arrayElementValidation(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, Object expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Root node type mismatch")
    @MethodSource("rootNodeTypeMismatchProvider")
    void rootNodeTypeMismatch(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, Object expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Disable fail‑fast")
    @MethodSource("disableFailFastProvider")
    void disableFailFast(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, Object expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] $ref – UnsupportedEncodingException")
    @MethodSource("refUnsupportedEncodingProvider")
    void refUnsupportedEncoding(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> {
            JsonSchema schema = factory.getSchema(schemaJson);
            JsonNode node = MAPPER.readTree(inputJson);
            schema.validate(node, format, customizer);
        });
    }

    @ParameterizedTest(name = "[{index}] $ref – IOException")
    @MethodSource("refIOExceptionProvider")
    void refIOException(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> {
            JsonSchema schema = factory.getSchema(schemaJson);
            JsonNode node = MAPPER.readTree(inputJson);
            schema.validate(node, format, customizer);
        });
    }

    @ParameterizedTest(name = "[{index}] ExecutionContextCustomizer exception propagation")
    @MethodSource("customizerExceptionProvider")
    void customizerExceptionPropagation(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, Class<? extends Throwable> expectedException) {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        assertThrows(expectedException, () -> schema.validate(node, format, customizer));
    }

    @ParameterizedTest(name = "[{index}] OutputFormat exception propagation")
    @MethodSource("formatExceptionProvider")
    void formatExceptionPropagation(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, Class<? extends Throwable> expectedException) {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        assertThrows(expectedException, () -> schema.validate(node, format, customizer));
    }

    @ParameterizedTest(name = "[{index}] Malformed schema – IllegalArgumentException")
    @MethodSource("malformedSchemaProvider")
    void malformedSchema(String schemaJson, String inputJson, OutputFormat<?> format, ExecutionContextCustomizer customizer, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> {
            JsonSchema schema = factory.getSchema(schemaJson);
            JsonNode node = MAPPER.readTree(inputJson);
            schema.validate(node, format, customizer);
        });
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:19: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:20: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:23: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:65: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_validate_9_Test
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:71: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_validate_9_Test
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:77: error: incompatible types: incompatible parameter types in lambda expression
    private static final ExecutionContextCustomizer NOOP_CUSTOMIZER = ctx -> {
                                                                      ^
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:93: error: invalid method reference
            messages.stream().sorted(Comparator.comparing(ValidationMessage::getPath)).forEach(m -> msgs.add(m.getPath() + ": " + m.getMessage()));
                                                          ^
  cannot find symbol
    symbol:   method getPath()
    location: class ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:110: error: invalid method reference
            messages.stream().sorted(Comparator.comparing(ValidationMessage::getPath)).forEach(m -> list.add(m.getPath().replaceFirst("^#", "").replaceFirst("^\\.", "") + ": " + m.getMessage()));
                                                          ^
  cannot find symbol
    symbol:   method getPath()
    location: class ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:179: error: 'void' type not allowed here
            ctx.setConfig(new SchemaValidatorsConfig().setFailFast(true));
                                                                  ^
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:180: error: incompatible types: bad return type in lambda expression
            return ctx;
                   ^
    unexpected return value
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:178: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer failFast = ctx -> {
                                              ^
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:296: error: 'void' type not allowed here
            ctx.setConfig(new SchemaValidatorsConfig().setFailFast(false));
                                                                  ^
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:297: error: incompatible types: bad return type in lambda expression
            return ctx;
                   ^
    unexpected return value
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:295: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer disable = ctx -> {
                                             ^
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:347: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer bad = ctx -> {
                                         ^
src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java:427: error: cannot find symbol
        assertTrue(vm.getPath().contains("name"), "The validation message should be about the 'name' property");
                     ^
  symbol:   method getPath()
  location: variable vm of type ValidationMessage
Note: src/test/java/com/networknt/schema/JsonSchema_validate_9_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
16 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)': '\n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.ExecutionContextCustomizer`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public ValidationResult walk(JsonNode, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Walk the JSON node.\n     * \n     * @param node     the input\n     * @param validate true to validate the input against the schema\n     * @return the validation result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;\n\t\tmethod `public ValidationResult walk(String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Walk the input.\n     * \n     * @param input       the input\n     * @param inputFormat the input format\n     * @param validate    true to validate the input against the schema\n     * @return the validation result\n     ;\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>                 the result type\n     * @param rootNode            the root node\n     * @param format              the formatter\n     * @param executionCustomizer the execution customizer\n     * @return the result\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


