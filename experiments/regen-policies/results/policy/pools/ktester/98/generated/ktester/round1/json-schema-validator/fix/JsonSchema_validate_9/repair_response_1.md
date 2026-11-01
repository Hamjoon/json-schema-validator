```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test suite for {@link JsonSchema#validate(JsonNode, OutputFormat, ExecutionContextCustomizer)}.
 *
 * The original version relied on Mockito which is not available in this
 * environment, so all mock‑related code has been removed.  The tests now use
 * simple lambda implementations of {@link ExecutionContextCustomizer}.
 */
class JsonSchema_validate_9_Test {

    /** Factory used to create {@link JsonSchema} instances. */
    private JsonSchemaFactory factory;

    /** The schema under test. */
    private JsonSchema schema;

    /** Jackson mapper for creating {@link JsonNode} instances. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple object schema that requires a string property {@code name}. */
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

    /** Input JSON that violates the schema (numeric {@code name}). */
    private static final String INVALID_INPUT = """
        { "name": 123 }
        """;

    /** No‑op customizer used where a customizer is required but no behaviour is needed. */
    private static final ExecutionContextCustomizer NOOP_CUSTOMIZER = ctx -> ctx;

    /** Simple {@link OutputFormat} that returns {@code "OK"} when there are no
     * validation messages and {@code "ERROR: …"} when there are.
     */
    private static class SimpleStringFormat implements OutputFormat<String> {

        @Override
        public String format(JsonSchema schema,
                             Set<ValidationMessage> messages,
                             ExecutionContext executionContext,
                             ValidationContext validationContext) {
            if (messages == null || messages.isEmpty()) {
                return "OK";
            }
            java.util.List<String> msgs = new ArrayList<>();
            messages.stream()
                    .sorted(Comparator.comparing(ValidationMessage::getPath))
                    .forEach(m -> msgs.add(m.getPath() + ": " + m.getMessage()));
            return "ERROR: " + String.join("; ", msgs);
        }
    }

    /**
     * {@link OutputFormat} that collects the textual representation of each
     * {@link ValidationMessage} into a {@link java.util.List<String>}.
     */
    private static class CollectingListFormat implements OutputFormat<java.util.List<String>> {

        @Override
        public java.util.List<String> format(JsonSchema schema,
                                             Set<ValidationMessage> messages,
                                             ExecutionContext executionContext,
                                             ValidationContext validationContext) {
            if (messages == null || messages.isEmpty()) {
                return Collections.emptyList();
            }
            java.util.List<String> list = new ArrayList<>();
            messages.stream()
                    .sorted(Comparator.comparing(ValidationMessage::getPath))
                    .forEach(m -> list.add(
                            m.getPath()
                                    .replaceFirst("^#", "")
                                    .replaceFirst("^\\.", "")
                                    + ": " + m.getMessage()));
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

    /* --------------------------------------------------------------------- */
    /*                     Parameter providers for the tests                */
    /* --------------------------------------------------------------------- */

    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(
                // rootNode == null → NPE
                Arguments.of(null,
                        new SimpleStringFormat(),
                        NOOP_CUSTOMIZER,
                        NullPointerException.class,
                        "When the JSON document (rootNode) is null the method should throw a NullPointerException because validation cannot be performed on a missing node."),
                // format == null → IllegalArgumentException
                Arguments.of(MAPPER.createObjectNode(),
                        null,
                        NOOP_CUSTOMIZER,
                        IllegalArgumentException.class,
                        "If the OutputFormat argument is null the method must reject the call with an IllegalArgumentException (the format is required to produce a result).")
        );
    }

    static Stream<Arguments> successfulWithoutCustomizerProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """;
        return Stream.of(Arguments.of(schema,
                """
                { "age": 30 }""",
                new SimpleStringFormat(),
                null,
                "OK"));
    }

    static Stream<Arguments> validationErrorNoCustomizerProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """;
        return Stream.of(Arguments.of(schema,
                """
                { "age": "thirty" }""",
                new SimpleStringFormat(),
                null,
                "ERROR: age: integer expected"));
    }

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
        return Stream.of(
                // valid document → OK
                Arguments.of(schema,
                        """
                        { "age": 30, "name": "John" }""",
                        new SimpleStringFormat(),
                        failFast,
                        "OK"),
                // invalid document → single error because fail‑fast stops after the first
                Arguments.of(schema,
                        """
                        { "age": "thirty", "name": 123 }""",
                        new SimpleStringFormat(),
                        failFast,
                        "ERROR: age: integer expected"));
    }

    static Stream<Arguments> collectingListFormatProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } }
            }
            """;
        return Stream.of(
                // valid document → empty list
                Arguments.of(schema,
                        """
                        { "age": 30 }""",
                        new CollectingListFormat(),
                        null,
                        Collections.emptyList()),
                // invalid document → list with one message
                Arguments.of(schema,
                        """
                        { "age": "thirty" }""",
                        new CollectingListFormat(),
                        null,
                        List.of("age: integer expected")));
    }

    static Stream<Arguments> missingRequiredProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "age": { "type": "integer" } },
              "required": [ "age" ]
            }
            """;
        return Stream.of(
                // SimpleStringFormat
                Arguments.of(schema,
                        """
                        { }""",
                        new SimpleStringFormat(),
                        null,
                        "ERROR: age: is required"),
                // CollectingListFormat
                Arguments.of(schema,
                        """
                        { }""",
                        new CollectingListFormat(),
                        null,
                        List.of("age: is required")));
    }

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
        return Stream.of(
                // SimpleStringFormat
                Arguments.of(schema,
                        json,
                        new SimpleStringFormat(),
                        null,
                        "ERROR: #: extraneous property \"extra\" is not allowed"),
                // CollectingListFormat
                Arguments.of(schema,
                        json,
                        new CollectingListFormat(),
                        null,
                        List.of("#: extraneous property \"extra\" is not allowed")));
    }

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
        return Stream.of(
                // SimpleStringFormat
                Arguments.of(schema,
                        json,
                        new SimpleStringFormat(),
                        null,
                        "ERROR: [2]: integer expected"),
                // CollectingListFormat
                Arguments.of(schema,
                        json,
                        new CollectingListFormat(),
                        null,
                        List.of("[2]: integer expected")));
    }

    static Stream<Arguments> rootNodeTypeMismatchProvider() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": { "name": { "type": "string" } }
            }
            """;
        String json = "\"just a string\"";
        return Stream.of(
                // SimpleStringFormat
                Arguments.of(schema,
                        json,
                        new SimpleStringFormat(),
                        null,
                        "ERROR: #: object expected"),
                // CollectingListFormat
                Arguments.of(schema,
                        json,
                        new CollectingListFormat(),
                        null,
                        List.of("#: object expected")));
    }

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
        return Stream.of(
                // CollectingListFormat – both errors collected
                Arguments.of(schema,
                        """
                        { "age": "thirty", "name": 123 }""",
                        new CollectingListFormat(),
                        disable,
                        List.of("age: integer expected", "name: string expected")),
                // SimpleStringFormat – concatenated message
                Arguments.of(schema,
                        """
                        { "age": "thirty", "name": 123 }""",
                        new SimpleStringFormat(),
                        disable,
                        "ERROR: age: integer expected; name: string expected"));
    }

    static Stream<Arguments> refUnsupportedEncodingProvider() {
        // The $ref contains an illegal percent‑encoding sequence.
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "#/components/schemas/Invalid%ZZ"
            }
            """;
        return Stream.of(Arguments.of(schema,
                """
                { "any": "value" }""",
                new SimpleStringFormat(),
                null,
                UnsupportedEncodingException.class));
    }

    static Stream<Arguments> refIOExceptionProvider() {
        // Reference a non‑existent file.
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "file:/nonexistent/schema.json"
            }
            """;
        return Stream.of(Arguments.of(schema,
                """
                { "any": "value" }""",
                new SimpleStringFormat(),
                null,
                IOException.class));
    }

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
        return Stream.of(Arguments.of(schema,
                """
                { "age": 30 }""",
                new SimpleStringFormat(),
                bad,
                IllegalStateException.class));
    }

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
            public String format(JsonSchema schema,
                                 Set<ValidationMessage> messages,
                                 ExecutionContext executionContext,
                                 ValidationContext validationContext) {
                throw new RuntimeException("format failure");
            }
        };
        return Stream.of(Arguments.of(schema,
                """
                { "age": 30 }""",
                faulty,
                null,
                RuntimeException.class));
    }

    static Stream<Arguments> malformedSchemaProvider() {
        // Unsupported type value "unknown".
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "unknown"
            }
            """;
        return Stream.of(Arguments.of(schema,
                """
                { "age": 30 }""",
                new SimpleStringFormat(),
                null,
                IllegalArgumentException.class));
    }

    /* --------------------------------------------------------------------- */
    /*                               Test methods                             */
    /* --------------------------------------------------------------------- */

    @Test
    void validateJsonNodeWithCustomizer_returnsExpectedMessages() throws Exception {
        JsonNode rootNode = MAPPER.readTree(INVALID_INPUT);
        Set<ValidationMessage> messages = schema.validate(rootNode, OutputFormat.DEFAULT, NOOP_CUSTOMIZER);
        assertNotNull(messages, "The returned message set must not be null");
        assertEquals(1, messages.size(),
                () -> "Expected exactly one validation error, but got: " + messages);
    }

    @Test
    void validateJsonNodeWithoutCustomizer_behavesIdentically() throws Exception {
        JsonNode rootNode = MAPPER.readTree(INVALID_INPUT);
        Set<ValidationMessage> messages = schema.validate(rootNode, OutputFormat.DEFAULT, (ExecutionContextCustomizer) null);
        assertNotNull(messages);
        assertEquals(1, messages.size());
    }

    @Test
    void validateJsonNodeWithCollectorOutputFormat() throws Exception {
        JsonNode rootNode = MAPPER.readTree(INVALID_INPUT);
        // Use the built‑in DEFAULT format which returns a Set<ValidationMessage>
        Set<ValidationMessage> set = schema.validate(rootNode, OutputFormat.DEFAULT, NOOP_CUSTOMIZER);
        assertNotNull(set);
        assertEquals(1, set.size());
        ValidationMessage vm = set.iterator().next();
        assertTrue(vm.getPath().contains("name"),
                "The validation message should be about the 'name' property");
    }

    @ParameterizedTest(name = "[{index}] Null handling – {4}")
    @MethodSource("nullHandlingProvider")
    void nullHandling(JsonNode rootNode,
                      OutputFormat<?> format,
                      ExecutionContextCustomizer customizer,
                      Class<? extends Throwable> expectedException,
                      String description) {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        if (expectedException != null) {
            assertThrows(expectedException,
                    () -> schema.validate(rootNode, format, customizer),
                    description);
        } else {
            fail("Expected an exception but none was thrown");
        }
    }

    @ParameterizedTest(name = "[{index}] Successful validation without customizer")
    @MethodSource("successfulWithoutCustomizerProvider")
    void successfulWithoutCustomizer(String schemaJson,
                                    String inputJson,
                                    OutputFormat<?> format,
                                    ExecutionContextCustomizer customizer,
                                    String expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Validation error (no customizer)")
    @MethodSource("validationErrorNoCustomizerProvider")
    void validationErrorNoCustomizer(String schemaJson,
                                    String inputJson,
                                    OutputFormat<?> format,
                                    ExecutionContextCustomizer customizer,
                                    String expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Fail‑fast customizer")
    @MethodSource("failFastCustomizerProvider")
    void failFastCustomizer(String schemaJson,
                           String inputJson,
                           OutputFormat<?> format,
                           ExecutionContextCustomizer customizer,
                           String expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] CollectingListFormat")
    @MethodSource("collectingListFormatProvider")
    void collectingListFormat(String schemaJson,
                              String inputJson,
                              OutputFormat<?> format,
                              ExecutionContextCustomizer customizer,
                              List<String> expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Missing required property")
    @MethodSource("missingRequiredProvider")
    void missingRequired(String schemaJson,
                         String inputJson,
                         OutputFormat<?> format,
                         ExecutionContextCustomizer customizer,
                         Object expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Additional properties not allowed")
    @MethodSource("additionalPropertiesFalseProvider")
    void additionalPropertiesFalse(String schemaJson,
                                   String inputJson,
                                   OutputFormat<?> format,
                                   ExecutionContextCustomizer customizer,
                                   Object expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Array element type validation")
    @MethodSource("arrayElementProvider")
    void arrayElementValidation(String schemaJson,
                                String inputJson,
                                OutputFormat<?> format,
                                ExecutionContextCustomizer customizer,
                                Object expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Root node type mismatch")
    @MethodSource("rootNodeTypeMismatchProvider")
    void rootNodeTypeMismatch(String schemaJson,
                              String inputJson,
                              OutputFormat<?> format,
                              ExecutionContextCustomizer customizer,
                              Object expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Disable fail‑fast")
    @MethodSource("disableFailFastProvider")
    void disableFailFast(String schemaJson,
                         String inputJson,
                         OutputFormat<?> format,
                         ExecutionContextCustomizer customizer,
                         Object expected) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        Object result = schema.validate(node, format, customizer);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] $ref – UnsupportedEncodingException")
    @MethodSource("refUnsupportedEncodingProvider")
    void refUnsupportedEncoding(String schemaJson,
                                String inputJson,
                                OutputFormat<?> format,
                                ExecutionContextCustomizer customizer,
                                Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> {
            JsonSchema schema = factory.getSchema(schemaJson);
            JsonNode node = MAPPER.readTree(inputJson);
            schema.validate(node, format, customizer);
        });
    }

    @ParameterizedTest(name = "[{index}] $ref – IOException")
    @MethodSource("refIOExceptionProvider")
    void refIOException(String schemaJson,
                        String inputJson,
                        OutputFormat<?> format,
                        ExecutionContextCustomizer customizer,
                        Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> {
            JsonSchema schema = factory.getSchema(schemaJson);
            JsonNode node = MAPPER.readTree(inputJson);
            schema.validate(node, format, customizer);
        });
    }

    @ParameterizedTest(name = "[{index}] ExecutionContextCustomizer exception propagation")
    @MethodSource("customizerExceptionProvider")
    void customizerExceptionPropagation(String schemaJson,
                                        String inputJson,
                                        OutputFormat<?> format,
                                        ExecutionContextCustomizer customizer,
                                        Class<? extends Throwable> expectedException) {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        assertThrows(expectedException, () -> schema.validate(node, format, customizer));
    }

    @ParameterizedTest(name = "[{index}] OutputFormat exception propagation")
    @MethodSource("formatExceptionProvider")
    void formatExceptionPropagation(String schemaJson,
                                    String inputJson,
                                    OutputFormat<?> format,
                                    ExecutionContextCustomizer customizer,
                                    Class<? extends Throwable> expectedException) {
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = MAPPER.readTree(inputJson);
        assertThrows(expectedException, () -> schema.validate(node, format, customizer));
    }

    @ParameterizedTest(name = "[{index}] Malformed schema – IllegalArgumentException")
    @MethodSource("malformedSchemaProvider")
    void malformedSchema(String schemaJson,
                         String inputJson,
                         OutputFormat<?> format,
                         ExecutionContextCustomizer customizer,
                         Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> {
            JsonSchema schema = factory.getSchema(schemaJson);
            JsonNode node = MAPPER.readTree(inputJson);
            schema.validate(node, format, customizer);
        });
    }
}
```