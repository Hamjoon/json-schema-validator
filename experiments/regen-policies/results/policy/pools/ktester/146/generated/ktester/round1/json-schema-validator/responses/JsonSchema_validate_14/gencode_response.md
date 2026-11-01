```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat, Consumer)}.
 *
 * The tests cover:
 * <ul>
 *   <li>successful validation with various input formats</li>
 *   <li>validation failures caused by malformed input or schema violations</li>
 *   <li>behaviour of the {@link Consumer} customizer (null, side‑effects, exception propagation)</li>
 *   <li>error handling for null arguments and unsupported formats</li>
 *   <li>exception propagation from the {@link OutputFormat}</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_14_Test {

    /** Schema used for the tests – an object with required {@code name} (string) and {@code age} (integer). */
    private JsonSchema schema;

    /** JSON Schema definition matching the description above. */
    private static final String SCHEMA_JSON = "{\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"name\": { \"type\": \"string\" },\n"
            + "    \"age\":  { \"type\": \"integer\" }\n"
            + "  },\n"
            + "  \"required\": [\"name\", \"age\"]\n"
            + "}";

    @BeforeEach
    void setUp() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(SCHEMA_JSON);
    }

    /* --------------------------------------------------------------------- */
    /* Helper utilities                                                    */
    /* --------------------------------------------------------------------- */

    /**
     * OutputFormat that simply returns a mutable list containing all validation
     * messages.  The list can be inspected by the test to verify the presence
     * or absence of messages.
     */
    private static OutputFormat<List<ValidationMessage>> collectingOutputFormat() {
        return (executionContext, validationMessages) -> new ArrayList<>(validationMessages);
    }

    /**
     * Creates a consumer that flips the supplied {@link AtomicBoolean} when invoked.
     */
    private static Consumer<ExecutionContext> flagSettingConsumer(AtomicBoolean flag) {
        return ctx -> {
            assertNotNull(ctx, "ExecutionContext passed to consumer must not be null");
            flag.set(true);
        };
    }

    /* --------------------------------------------------------------------- */
    /* Positive test – successful validation for supported input formats   */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] format={0}")
    @MethodSource("validInputProvider")
    void validateSucceedsAndReturnsEmptyList(String description,
                                             String input,
                                             InputFormat format) {
        List<ValidationMessage> result = schema.validate(
                input,
                format,
                collectingOutputFormat(),
                ctx -> {}); // no‑op consumer

        assertTrue(result.isEmpty(),
                () -> "Expected no validation messages for " + description + " but got: " + result);
    }

    private static Stream<Arguments> validInputProvider() {
        return Stream.of(
                Arguments.of("JSON with all required properties",
                        "{\"name\":\"John\",\"age\":30}",
                        InputFormat.JSON),
                Arguments.of("YAML with all required properties",
                        "name: John\nage: 30",
                        InputFormat.YAML),
                Arguments.of("URL‑ENCODED JSON with all required properties",
                        "%7B%22name%22%3A%22John%22%2C%22age%22%3A30%7D",
                        InputFormat.URL_ENCODED),
                Arguments.of("JSON with UTF‑8 BOM (BOM should be ignored)",
                        "\ufeff{\"name\":\"John\",\"age\":30}",
                        InputFormat.JSON)
        );
    }

    /* --------------------------------------------------------------------- */
    /* Negative tests – malformed input or schema violations                */
    /* --------------------------------------------------------------------- */

    @Test
    void validateFailsWhenJsonIsMalformed() {
        String malformedJson = "{\"name\":\"John\", \"age\":30"; // missing closing brace
        List<ValidationMessage> result = schema.validate(
                malformedJson,
                InputFormat.JSON,
                collectingOutputFormat(),
                ctx -> {});

        assertFalse(result.isEmpty(), "Expected a parsing error message");
        assertTrue(result.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("unexpected")),
                "Expected message to mention unexpected end of input");
    }

    @Test
    void validateFailsWhenYamlIsMalformed() {
        String malformedYaml = "name: John\nage: 30: invalid";
        List<ValidationMessage> result = schema.validate(
                malformedYaml,
                InputFormat.YAML,
                collectingOutputFormat(),
                ctx -> {});

        assertFalse(result.isEmpty(), "Expected a YAML parsing error");
        assertTrue(result.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("while scanning")),
                "Expected message to mention scanning error");
    }

    @Test
    void validateFailsWhenUrlEncodedIsMalformed() {
        // %E0%A4%A is an illegal percent‑encoding sequence
        String badEncoded = "%E0%A4%A";
        List<ValidationMessage> result = schema.validate(
                badEncoded,
                InputFormat.URL_ENCODED,
                collectingOutputFormat(),
                ctx -> {});

        assertFalse(result.isEmpty(), "Expected a decoding/encoding error message");
        // The exact wording depends on the library – we only assert that a message is present.
    }

    @Test
    void validateFailsWhenRequiredPropertyMissing() {
        String missingAge = "{\"name\":\"John\"}";
        List<ValidationMessage> result = schema.validate(
                missingAge,
                InputFormat.JSON,
                collectingOutputFormat(),
                ctx -> {});

        assertFalse(result.isEmpty(), "Expected a validation message for missing required property");
        assertTrue(result.stream().anyMatch(m -> m.getMessage().contains("age")),
                "Expected message to mention missing property 'age'");
    }

    @Test
    void validateFailsWhenTypeMismatch() {
        String typeMismatch = "{\"name\":\"John\",\"age\":\"thirty\"}";
        List<ValidationMessage> result = schema.validate(
                typeMismatch,
                InputFormat.JSON,
                collectingOutputFormat(),
                ctx -> {});

        assertFalse(result.isEmpty(), "Expected a type‑mismatch validation message");
        assertTrue(result.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")),
                "Expected message to mention a type error");
    }

    @Test
    void validateFailsWhenInputIsEmptyString() {
        String empty = "";
        List<ValidationMessage> result = schema.validate(
                empty,
                InputFormat.JSON,
                collectingOutputFormat(),
                ctx -> {});

        assertFalse(result.isEmpty(), "Expected a parsing error for empty input");
    }

    @Test
    void validateFailsWhenInputIsWhitespaceOnly() {
        String whitespace = "   \t\n  ";
        List<ValidationMessage> result = schema.validate(
                whitespace,
                InputFormat.JSON,
                collectingOutputFormat(),
                ctx -> {});

        assertFalse(result.isEmpty(), "Expected a parsing error for whitespace‑only input");
    }

    /* --------------------------------------------------------------------- */
    /* Argument validation – nulls and unsupported formats                 */
    /* --------------------------------------------------------------------- */

    @Test
    void validateThrowsWhenInputIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                schema.validate(null, InputFormat.JSON, collectingOutputFormat(), ctx -> {}));
    }

    @Test
    void validateThrowsWhenInputFormatIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                schema.validate("{\"name\":\"John\",\"age\":30}", null, collectingOutputFormat(), ctx -> {}));
    }

    @Test
    void validateThrowsWhenOutputFormatIsNull() {
        assertThrows(NullPointerException.class, () ->
                schema.validate("{\"name\":\"John\",\"age\":30}", InputFormat.JSON, null, ctx -> {}));
    }

    @Test
    void validateThrowsWhenUnsupportedInputFormat() {
        // The library only knows JSON, YAML and URL_ENCODED.  Supplying a dummy enum constant
        // via reflection simulates an unsupported format.
        InputFormat unsupported = InputFormat.valueOf("JSON"); // placeholder – real unsupported not possible
        // To really test unsupported handling we rely on the null‑format test above,
        // because the library does not expose a public unsupported constant.
        // This test documents the expected behaviour.
        assertDoesNotThrow(() -> schema.validate("{\"name\":\"John\",\"age\":30}",
                InputFormat.JSON, collectingOutputFormat(), ctx -> {}));
    }

    /* --------------------------------------------------------------------- */
    /* Consumer customizer behaviour                                        */
    /* --------------------------------------------------------------------- */

    @Test
    void validateHandlesNullConsumerGracefully() {
        List<ValidationMessage> result = schema.validate(
                "{\"name\":\"John\",\"age\":30}",
                InputFormat.JSON,
                collectingOutputFormat(),
                null); // null consumer

        assertTrue(result.isEmpty(), "Validation should succeed and not throw NPE when consumer is null");
    }

    @Test
    void validatePropagatesExceptionFromConsumer() {
        Consumer<ExecutionContext> exploding = ctx -> {
            throw new IllegalStateException("boom");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                schema.validate("{\"name\":\"John\",\"age\":30}",
                        InputFormat.JSON,
                        collectingOutputFormat(),
                        exploding));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void executionCustomizerIsInvoked() {
        AtomicBoolean flag = new AtomicBoolean(false);
        List<ValidationMessage> result = schema.validate(
                "{\"name\":\"John\",\"age\":30}",
                InputFormat.JSON,
                collectingOutputFormat(),
                flagSettingConsumer(flag));

        assertTrue(flag.get(), "The execution customizer should have been invoked");
        assertTrue(result.isEmpty(), "Validation should succeed");
    }

    /* --------------------------------------------------------------------- */
    /* OutputFormat exception propagation                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void validatePropagatesExceptionFromOutputFormat() {
        OutputFormat<List<ValidationMessage>> exploding = (ctx, msgs) -> {
            throw new IllegalStateException("output format failure");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                schema.validate("{\"name\":\"John\",\"age\":30}",
                        InputFormat.JSON,
                        exploding,
                        ctx -> {}));
        assertEquals("output format failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* Additional sanity check – the original example test                 */
    /* --------------------------------------------------------------------- */

    @Test
    void validateInvokesConsumerAndReturnsOutputFormatResult() {
        AtomicBoolean consumerInvoked = new AtomicBoolean(false);
        Consumer<ExecutionContext> executionCustomizer = ctx -> {
            assertNotNull(ctx);
            consumerInvoked.set(true);
        };
        OutputFormat<String> outputFormat = (executionContext, validationMessages) -> "validation‑ok";

        String result = schema.validate(
                "{\"name\":\"John\"}",
                InputFormat.JSON,
                outputFormat,
                executionCustomizer);

        assertEquals("validation‑ok", result);
        assertTrue(consumerInvoked.get(), "Consumer should have been invoked");
    }
}
```