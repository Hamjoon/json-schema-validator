```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchema#validate(JsonNode, OutputFormat, ExecutionContextCustomizer)}.
 *
 * <p>The test data is driven by the JSON test‑case description supplied with the
 * kata.  Each logical group is covered by a dedicated {@code @ParameterizedTest}
 * that receives the schema, the input JSON, the {@link OutputFormat} implementation,
 * an optional {@link ExecutionContextCustomizer} and the expected result (or
 * expected exception).</p>
 *
 * <p>Custom {@link OutputFormat} implementations are defined at the bottom of the
 * class – {@link SimpleStringFormat} returns a single {@code String} describing
 * the validation outcome and {@link CollectingListFormat} returns a {@code List<String>}
 * with the individual messages.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_9_Test {

    /** Factory used to create {@link JsonSchema} instances. */
    private JsonSchemaFactory factory;

    /** Jackson mapper for creating {@link JsonNode} instances. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A mock customizer – the default behaviour (do‑nothing) is sufficient for many tests. */
    @Mock
    ExecutionContextCustomizer noopCustomizer;

    @BeforeEach
    void setUp() {
        // 2020‑12 draft is used for all schemas in this test suite.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /* --------------------------------------------------------------------- *
     *  Parameter sources – each method returns a stream of arguments that
     *  represent one test case from the JSON description.
     *  --------------------------------------------------------------------- */

    /** Null handling – rootNode null or format null. */
    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(
                // rootNode == null → NPE
                Arguments.of(null,
                        new SimpleStringFormat(),
                        null,
                        NullPointerException.class,
                        "When the JSON document (rootNode) is null the method should throw a NullPointerException because validation cannot be performed on a missing node."),
                // format == null → IllegalArgumentException
                Arguments.of(MAPPER.createObjectNode(),
                        null,
                        null,
                        IllegalArgumentException.class,
                        "If the OutputFormat argument is null the method must reject the call with an IllegalArgumentException (the format is required to produce a result).")
        );
    }

    /** Successful validation without a customizer – schema expects an integer “age”. */
    static Stream<Arguments> successfulWithoutCustomizerProvider() {
        String schema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "age": { "type": "integer" } }
                }
                """;
        return Stream.of(
                Arguments.of(schema,
                        """{ "age": 30 }""",
                        new SimpleStringFormat(),
                        null,
                        "OK")
        );
    }

    /** Validation with errors (no customizer). */
    static Stream<Arguments> validationErrorNoCustomizerProvider() {
        String schema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "age": { "type": "integer" } }
                }
                """;
        return Stream.of(
                Arguments.of(schema,
                        """{ "age": "thirty" }""",
                        new SimpleStringFormat(),
                        null,
                        "ERROR: age: integer expected")
        );
    }

    /** Validation with ExecutionContextCustomizer (fail‑fast true). */
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
        ExecutionContextCustomizer failFast = ctx -> ctx.setConfig(new SchemaValidatorsConfig().setFailFast(true));
        return Stream.of(
                // valid document → OK
                Arguments.of(schema,
                        """{ "age": 30, "name": "John" }""",
                        new SimpleStringFormat(),
                        failFast,
                        "OK"),
                // invalid document → single error because fail‑fast stops after the first
                Arguments.of(schema,
                        """{ "age": "thirty", "name": 123 }""",
                        new SimpleStringFormat(),
                        failFast,
                        "ERROR: age: integer expected")
        );
    }

    /** Custom OutputFormat – collector that returns a List<String>. */
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
                        """{ "age": 30 }""",
                        new CollectingListFormat(),
                        null,
                        Collections.emptyList()),
                // invalid document → list with one message
                Arguments.of(schema,
                        """{ "age": "thirty" }""",
                        new CollectingListFormat(),
                        null,
                        List.of("age: integer expected"))
        );
    }

    /** Missing required property. */
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
                        """{ }""",
                        new SimpleStringFormat(),
                        null,
                        "ERROR: age: is required"),
                // CollectingListFormat
                Arguments.of(schema,
                        """{ }""",
                        new CollectingListFormat(),
                        null,
                        List.of("age: is required"))
        );
    }

    /** Disallowed additional property (additionalProperties:false). */
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
        String json = """{ "age": 30, "extra": "value" }""";
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
                        List.of("#: extraneous property \"extra\" is not allowed"))
        );
    }

    /** Array element type validation. */
    static Stream<Arguments> arrayElementProvider() {
        String schema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "array",
                  "items": { "type": "integer" }
                }
                """;
        String json = """[1,2,"three"]""";
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
                        List.of("[2]: integer expected"))
        );
    }

    /** Root node type mismatch (primitive vs object). */
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
                        List.of("#: object expected"))
        );
    }

    /** ExecutionContextCustomizer that disables fail‑fast. */
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
        ExecutionContextCustomizer disable = ctx -> ctx.setConfig(new SchemaValidatorsConfig().setFailFast(false));
        return Stream.of(
                // CollectingListFormat – both errors collected
                Arguments.of(schema,
                        """{ "age": "thirty", "name": 123 }""",
                        new CollectingListFormat(),
                        disable,
                        List.of("age: integer expected", "name: string expected")),
                // SimpleStringFormat – concatenated message
                Arguments.of(schema,
                        """{ "age": "thirty", "name": 123 }""",
                        new SimpleStringFormat(),
                        disable,
                        "ERROR: age: integer expected; name: string expected")
        );
    }

    /** $ref handling – UnsupportedEncodingException. */
    static Stream<Arguments> refUnsupportedEncodingProvider() {
        // The $ref contains an illegal percent‑encoding sequence.
        String schema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$ref": "#/components/schemas/Invalid%ZZ"
                }
                """;
        return Stream.of(
                Arguments.of(schema,
                        """{ "any": "value" }""",
                        new SimpleStringFormat(),
                        null,
                        UnsupportedEncodingException.class)
        );
    }

    /** $ref handling – IOException on external schema. */
    static Stream<Arguments> refIOExceptionProvider() {
        // Reference a non‑existent file.
        String schema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$ref": "file:/nonexistent/schema.json"
                }
                """;
        return Stream.of(
                Arguments.of(schema,
                        """{ "any": "value" }""",
                        new SimpleStringFormat(),
                        null,
                        IOException.class)
        );
    }

    /** ExecutionContextCustomizer exception propagation. */
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
        return Stream.of(
                Arguments.of(schema,
                        """{ "age": 30 }""",
                        new SimpleStringFormat(),
                        bad,
                        IllegalStateException.class)
        );
    }

    /** OutputFormat exception propagation. */
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
            public String apply(Set<ValidationMessage> messages) {
                throw new RuntimeException("format failure");
            }
        };
        return Stream.of(
                Arguments.of(schema,
                        """{ "age": 30 }""",
                        faulty,
                        null,
                        RuntimeException.class)
        );
    }

    /** Invalid schema definition – IllegalArgumentException during construction. */
    static Stream<Arguments> malformedSchemaProvider() {
        // Unsupported type value "unknown".
        String schema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "unknown"
                }
                """;
        return Stream.of(
                Arguments.of(schema,
                        """{ "age": 30 }""",
                        new SimpleStringFormat(),
                        null,
                        IllegalArgumentException.class)
        );
    }

    /* --------------------------------------------------------------------- *
     *  Test methods – each consumes the arguments from the streams above.
     *  --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] Null handling – {4}")
    @MethodSource("nullHandlingProvider")
    void nullHandling(JsonNode rootNode,
                      OutputFormat<?> format,
                      ExecutionContextCustomizer customizer,
                      Class<? extends Throwable> expectedException,
                      String description) {
        // Build a trivial schema (object without constraints) – the schema itself
        // is irrelevant because the method should fail before any validation.
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
        // Build the schema – the constructor itself may already throw, so we
        // capture the exception from either creation or validation.
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
        assertThrows(expectedException,
                () -> schema.validate(node, format, customizer));
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
        assertThrows(expectedException,
                () -> schema.validate(node, format, customizer));
    }

    @ParameterizedTest(name = "[{index}] Malformed schema – IllegalArgumentException")
    @MethodSource("malformedSchemaProvider")
    void malformedSchema(String schemaJson,
                         String inputJson,
                         OutputFormat<?> format,
                         ExecutionContextCustomizer customizer,
                         Class<? extends Throwable> expectedException) {
        // The exception is thrown during schema creation.
        assertThrows(expectedException, () -> {
            JsonSchema schema = factory.getSchema(schemaJson);
            JsonNode node = MAPPER.readTree(inputJson);
            schema.validate(node, format, customizer);
        });
    }

    /* --------------------------------------------------------------------- *
     *  Helper OutputFormat implementations used by the tests.
     *  --------------------------------------------------------------------- */

    /**
     * Simple {@link OutputFormat} that returns {@code "OK"} when there are no
     * validation messages and {@code "ERROR: …"} when there are.
     */
    private static class SimpleStringFormat implements OutputFormat<String> {
        @Override
        public String apply(Set<ValidationMessage> messages) {
            if (messages == null || messages.isEmpty()) {
                return "OK";
            }
            // Concatenate messages in a deterministic order.
            List<String> msgs = new ArrayList<>();
            messages.stream()
                    .sorted((a, b) -> a.getPath().compareTo(b.getPath()))
                    .forEach(m -> msgs.add(m.getPath() + ": " + m.getMessage()));
            return "ERROR: " + String.join("; ", msgs);
        }
    }

    /**
     * {@link OutputFormat} that collects the textual representation of each
     * {@link ValidationMessage} into a {@link List<String>}.
     */
    private static class CollectingListFormat implements OutputFormat<List<String>> {
        @Override
        public List<String> apply(Set<ValidationMessage> messages) {
            if (messages == null || messages.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> list = new ArrayList<>();
            messages.stream()
                    .sorted((a, b) -> a.getPath().compareTo(b.getPath()))
                    .forEach(m -> list.add(m.getPath().replaceFirst("^#", "").replaceFirst("^\\.", "") + ": " + m.getMessage()));
            return list;
        }
    }
}
```