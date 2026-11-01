```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.stream.Stream;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Additional unit tests for {@link JsonSchema#validate(String, InputFormat, Consumer)}.
 *
 * The tests cover a wide range of scenarios described in the JSON test‑case
 * specification, including null/empty inputs, unsupported formats, execution
 * customizers, and schema‑constraint violations.
 *
 * The {@code @BeforeEach} method from the original test class creates a
 * {@link JsonSchema} instance based on a simple schema that requires a
 * {@code name} property of type {@code string}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_10_Test {

    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }
            """;

    private JsonSchema schema;
    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory for the 2020‑12 draft and obtain a JsonSchema instance.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
                builder -> {}); // default builder – no special configuration
        schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema, "Schema should be created successfully");
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /*  Existing tests (kept for completeness)                              */
    /* --------------------------------------------------------------------- */

    @Test
    void validateValidJsonCallsConsumerAndReturnsNoMessages() {
        // GIVEN
        String validJson = """{ "name": "John Doe" }""";
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> consumer = mock(Consumer.class);

        // WHEN
        Set<ValidationMessage> messages = schema.validate(validJson, InputFormat.JSON, consumer);

        // THEN
        assertTrue(messages.isEmpty(), "A valid JSON document should produce no validation messages");
        verify(consumer, times(1)).accept(any(ExecutionContext.class));
    }

    @Test
    void validateInvalidJsonCallsConsumerAndReturnsMessages() {
        // GIVEN
        String invalidJson = """{ "name": 123 }"""; // name should be a string
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> consumer = mock(Consumer.class);

        // WHEN
        Set<ValidationMessage> messages = schema.validate(invalidJson, InputFormat.JSON, consumer);

        // THEN
        assertFalse(messages.isEmpty(), "An invalid JSON document should produce validation messages");
        assertEquals(1, messages.size(), "Exactly one validation message is expected");
        verify(consumer, times(1)).accept(any(ExecutionContext.class));
    }

    /* --------------------------------------------------------------------- */
    /*  New test cases derived from the provided JSON specification           */
    /* --------------------------------------------------------------------- */

    @Test
    void validJsonWithoutCustomizerReturnsEmptySet() {
        // GIVEN
        String json = """{ "name":"John" }""";

        // WHEN
        Set<ValidationMessage> messages = schema.validate(json, InputFormat.JSON, null);

        // THEN
        assertTrue(messages.isEmpty(), "Valid JSON with null customizer must return an empty set");
    }

    @Test
    void invalidJsonSyntaxReturnsParsingErrorMessage() {
        // GIVEN
        String malformed = """{ "name":"John" """;

        // WHEN
        Set<ValidationMessage> messages = schema.validate(malformed, InputFormat.JSON, null);

        // THEN
        assertFalse(messages.isEmpty(), "Malformed JSON should yield at least one validation message");
        // The exact wording may differ between Jackson versions – we only check that it mentions parsing.
        boolean containsParseInfo = messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("parse"));
        assertTrue(containsParseInfo, "Expected a parsing‑related validation message");
    }

    @Test
    void nullInputStringThrowsIllegalArgumentException() {
        // GIVEN
        String input = null;

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(input, InputFormat.JSON, null),
                "A null input string should cause IllegalArgumentException");
    }

    @Test
    void emptyInputStringReturnsParsingError() {
        // GIVEN
        String empty = "";

        // WHEN
        Set<ValidationMessage> messages = schema.validate(empty, InputFormat.JSON, null);

        // THEN
        assertFalse(messages.isEmpty(), "Empty input should produce a parsing error message");
        assertTrue(messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("empty")), "Message should indicate empty content");
    }

    @Test
    void unsupportedInputFormatThrowsUnsupportedOperationException() {
        // GIVEN
        String json = """{ "name":"John" }""";

        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class,
                () -> schema.validate(json, InputFormat.UNSUPPORTED, null),
                "Unsupported InputFormat must trigger UnsupportedOperationException");
    }

    @Test
    void executionCustomizerModifiesContext() {
        // GIVEN
        String json = """{ "age":30 }"""; // valid w.r.t. schema (no required property check here)
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("testFlag", true);
        ArgumentCaptor<ExecutionContext> captor = ArgumentCaptor.forClass(ExecutionContext.class);

        // WHEN
        Set<ValidationMessage> messages = schema.validate(json, InputFormat.JSON, customizer);

        // THEN
        assertTrue(messages.isEmpty(), "The JSON document is valid, so no messages are expected");
        // Verify that the customizer was invoked and that the attribute was set.
        verify(customizer, times(1)).accept(captor.capture());
        ExecutionContext captured = captor.getValue();
        assertEquals(Boolean.TRUE, captured.getAttribute("testFlag"),
                "Custom attribute set by the consumer must be present in the ExecutionContext");
    }

    @Test
    void executionCustomizerThrowsRuntimeExceptionIsPropagated() {
        // GIVEN
        String json = """{ "valid":true }""";
        Consumer<ExecutionContext> failingCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };

        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(json, InputFormat.JSON, failingCustomizer));
        assertEquals("customizer failure", ex.getMessage(),
                "The original RuntimeException message must be preserved");
    }

    @Test
    void yamlValidDocumentReturnsEmptySet() {
        // GIVEN
        String yaml = """
                name: John
                age: 25
                """;

        // WHEN
        Set<ValidationMessage> messages = schema.validate(yaml, InputFormat.YAML, null);

        // THEN
        assertTrue(messages.isEmpty(), "A well‑formed YAML document should produce no validation messages");
    }

    @Test
    void yamlInvalidDocumentReturnsParsingError() {
        // GIVEN
        String malformedYaml = """
                name: John
                age: [unclosed
                """;

        // WHEN
        Set<ValidationMessage> messages = schema.validate(malformedYaml, InputFormat.YAML, null);

        // THEN
        assertFalse(messages.isEmpty(), "Malformed YAML should produce a parsing error message");
        assertTrue(messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("yaml")), "Message should refer to YAML parsing");
    }

    @Test
    void nullInputFormatThrowsException() {
        // GIVEN
        String json = """{ "name":"John" }""";

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(json, null, null),
                "A null InputFormat must cause an IllegalArgumentException");
    }

    @Test
    void whitespaceOnlyInputStringReturnsParsingError() {
        // GIVEN
        String whitespace = "   \t\n";

        // WHEN
        Set<ValidationMessage> messages = schema.validate(whitespace, InputFormat.JSON, null);

        // THEN
        assertFalse(messages.isEmpty(), "Whitespace‑only input should yield a parsing error");
        assertTrue(messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("no content")), "Message should indicate no content");
    }

    @Test
    void missingRequiredPropertyReturnsRequiredMessage() {
        // GIVEN
        String jsonMissingName = """{ "age":30 }""";

        // WHEN
        Set<ValidationMessage> messages = schema.validate(jsonMissingName, InputFormat.JSON, null);

        // THEN
        assertFalse(messages.isEmpty(), "Missing required property must generate a validation message");
        assertTrue(messages.stream()
                .anyMatch(m -> m.getMessage().contains("required") && m.getMessage().contains("name")),
                "Message should mention the missing required property 'name'");
    }

    @Test
    void missingRequiredPropertyWithCustomizer() {
        // GIVEN
        String jsonMissingName = """{ "age":30 }""";
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("customizerInvoked", true);
        ArgumentCaptor<ExecutionContext> captor = ArgumentCaptor.forClass(ExecutionContext.class);

        // WHEN
        Set<ValidationMessage> messages = schema.validate(jsonMissingName, InputFormat.JSON, customizer);

        // THEN
        assertFalse(messages.isEmpty(), "Schema violation must be reported even when a customizer is present");
        assertTrue(messages.stream()
                .anyMatch(m -> m.getMessage().contains("required") && m.getMessage().contains("name")),
                "Message should indicate missing required property 'name'");

        verify(customizer, times(1)).accept(captor.capture());
        ExecutionContext ctx = captor.getValue();
        assertEquals(Boolean.TRUE, ctx.getAttribute("customizerInvoked"),
                "Customizer must have set its attribute");
    }

    @Test
    void jsonWithUtf8BomIsParsedSuccessfully() {
        // GIVEN
        String jsonWithBom = "\uFEFF{\"name\":\"John\"}";

        // WHEN
        Set<ValidationMessage> messages = schema.validate(jsonWithBom, InputFormat.JSON, null);

        // THEN
        assertTrue(messages.isEmpty(), "JSON prefixed with a UTF‑8 BOM should be parsed without errors");
    }

    @Test
    void executionCustomizerThrowsCheckedExceptionWrappedInRuntimeException() {
        // GIVEN
        String json = """{ "valid":true }""";
        Consumer<ExecutionContext> customizer = ctx -> {
            try {
                ctx.setAttribute("x", 1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(json, InputFormat.JSON, customizer));
        // The original cause is not important for this test; we only verify propagation.
        assertNotNull(ex.getMessage(), "RuntimeException should be propagated");
    }

    @Test
    void malformedUrlEncodedInputThrowsIllegalArgumentException() {
        // GIVEN
        String malformed = "%ZZ";

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(malformed, InputFormat.URL_ENCODED, null),
                "Malformed percent‑encoding must result in IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterised helper for future extensions (not used currently)     */
    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> provideInvalidJsonInputs() {
        return Stream.of(
                Arguments.of("{\"name\":\"John\"", "Invalid JSON syntax"),
                Arguments.of("", "Empty input")
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidJsonInputs")
    void parameterisedInvalidJsonTests(String input, String description) {
        Set<ValidationMessage> messages = schema.validate(input, InputFormat.JSON, null);
        assertFalse(messages.isEmpty(), description + " should produce a validation message");
    }
}
```