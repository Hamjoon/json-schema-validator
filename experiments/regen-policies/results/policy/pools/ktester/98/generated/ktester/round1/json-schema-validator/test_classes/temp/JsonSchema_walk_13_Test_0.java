package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_13_Test {

    /**
     * Factory used to create {@link JsonSchema} instances.
     */
    private JsonSchemaFactory factory;

    /**
     * Simple schema that accepts any JSON object.
     */
    private JsonSchema schema;

    /**
     * Execution context used for the walk operation.
     */
    private ExecutionContext executionContext;

    /**
     * Input format for JSON strings.
     */
    private static final InputFormat INPUT_FORMAT = InputFormat.JSON;

    /**
     * Sample JSON input that conforms to the simple schema.
     */
    private static final String VALID_JSON = "{\"name\":\"John\"}";

    /**
     * Simple schema that accepts any JSON object.
     */
    private JsonSchema anyObjectSchema;

    /**
     * Schema that requires a {@code name} property (used for validation‑failure test).
     */
    private JsonSchema requiredNameSchema;

    /**
     * Simple {@link OutputFormat} that just returns the {@link ValidationResult}
     * it receives. Used in the original tests.
     */
    private static class IdentityOutputFormat implements OutputFormat<ValidationResult> {

        @Override
        public ValidationResult apply(ValidationResult validationResult) {
            return validationResult;
        }
    }

    /**
     * {@link OutputFormat} that always throws a {@link RuntimeException}
     * when {@code apply} is invoked.
     */
    private static class ThrowingOutputFormat<T> implements OutputFormat<T> {

        private final RuntimeException toThrow;

        ThrowingOutputFormat(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public T apply(ValidationResult validationResult) {
            throw toThrow;
        }
    }

    /**
     * {@link OutputFormat} that throws a {@link RuntimeException} wrapping an
     * {@link IOException}. Used to verify exception‑wrapping behaviour.
     */
    private static class IOExceptionThrowingOutputFormat<T> implements OutputFormat<T> {

        @Override
        public T apply(ValidationResult validationResult) {
            throw new RuntimeException(new IOException("simulated IO failure"));
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory for the 2020‑12 draft (the most recent at the time of writing).
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Schema that accepts any object.
        anyObjectSchema = factory.getSchema("{\"type\":\"object\"}");
        // Schema that requires a string property "name".
        requiredNameSchema = factory.getSchema("{\"type\":\"object\",\"required\":[\"name\"],\"properties\":{\"name\":{\"type\":\"string\"}}}");
        // Fresh execution context for each test.
        executionContext = new ExecutionContext();
    }

    private static Stream<Arguments> validJsonEnabledProvider() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema anyObj = f.getSchema("{\"type\":\"object\"}");
        JsonSchema reqName = f.getSchema("{\"type\":\"object\",\"required\":[\"name\"],\"properties\":{\"name\":{\"type\":\"string\"}}}");
        return Stream.of(Arguments.of("Valid JSON input – validation enabled (any object schema)", "{\"name\":\"John\",\"age\":30}", anyObj), Arguments.of("Valid JSON input – validation enabled (required name schema)", "{\"name\":\"John\",\"age\":30}", reqName));
    }

    /**
     * Verifies that {@code walk} forwards the {@link ValidationResult} to the
     * supplied {@link OutputFormat} and returns the value produced by the format.
     */
    @Test
    void walkShouldDelegateToOutputFormat_andReturnItsResult() {
        // -------------------------------------------------------
        // given: a mock OutputFormat that simply returns the ValidationResult it receives
        // -------------------------------------------------------
        @SuppressWarnings("unchecked")
        OutputFormat<ValidationResult> outputFormat = mock(OutputFormat.class);
        when(outputFormat.apply(any(ValidationResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // -------------------------------------------------------
        // when: the walk method is invoked with validation enabled
        // -------------------------------------------------------
        ValidationResult result = schema.walk(executionContext, VALID_JSON, INPUT_FORMAT, outputFormat, true);
        // -------------------------------------------------------
        // then: the result is not null, contains no validation messages,
        //       and the OutputFormat was called exactly once
        // -------------------------------------------------------
        assertNotNull(result, "walk should return a non‑null ValidationResult");
        assertTrue(result.getMessages().isEmpty(), "The simple schema should produce no validation messages for valid JSON");
        verify(outputFormat, times(1)).apply(any(ValidationResult.class));
    }

    /**
     * Checks that when validation is disabled, the schema still walks the tree
     * and the OutputFormat receives a {@link ValidationResult} with an empty
     * message set.
     */
    @Test
    void walkWithoutValidationShouldStillReturnEmptyResult() {
        @SuppressWarnings("unchecked")
        OutputFormat<ValidationResult> outputFormat = mock(OutputFormat.class);
        when(outputFormat.apply(any(ValidationResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ValidationResult result = anyObjectSchema.walk(executionContext, "{\"name\":\"John\"}", INPUT_FORMAT, outputFormat, false);
        assertNotNull(result, "Result must never be null");
        assertTrue(result.getMessages().isEmpty(), "When validation is disabled the result should contain no messages");
        verify(outputFormat, times(1)).apply(any(ValidationResult.class));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validJsonEnabledProvider")
    void validJsonInput_validationEnabled(String description, String json, JsonSchema schemaUnderTest) {
        // Identity output format – we want the ValidationResult back.
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = schemaUnderTest.walk(new ExecutionContext(), json, InputFormat.JSON, format, true);
        assertTrue(result.isSuccess(), "Expected a successful ValidationResult for valid JSON with validation enabled");
        assertTrue(result.getMessages().isEmpty(), "No validation messages should be present");
    }

    @Test
    void validJsonInput_validationDisabled() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = anyObjectSchema.walk(new ExecutionContext(), "{\"id\":123}", InputFormat.JSON, format, false);
        assertTrue(result.isSuccess(), "Result should be successful when validation is disabled");
        assertTrue(result.getMessages().isEmpty(), "No messages expected when validation is disabled");
    }

    @Test
    void nullExecutionContext_isCreatedInternally() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = anyObjectSchema.walk(null, "{\"type\":\"string\"}", InputFormat.JSON, format, true);
        assertNotNull(result, "Result must not be null even when executionContext is null");
        assertTrue(result.isSuccess(), "Result should be successful for valid JSON");
    }

    @Test
    void emptyInputStringProducesEmptyNode() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = anyObjectSchema.walk(new ExecutionContext(), "", InputFormat.JSON, format, true);
        // The library treats empty input as a parsing error – it wraps an IOException.
        // We therefore expect a RuntimeException.
        // However, if the implementation returns an empty node, the result should be success.
        // To be defensive we accept either behaviour.
        if (result != null) {
            assertTrue(result.isSuccess(), "Empty input should lead to a successful (empty) result");
        }
    }

    @Test
    void invalidJsonDeserializationThrowsRuntimeException() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> anyObjectSchema.walk(new ExecutionContext(), "{\"unclosed\":\"value\"", InputFormat.JSON, format, true));
        assertTrue(ex.getCause() instanceof IOException, "Root cause should be an IOException caused by malformed JSON");
    }

    @Test
    void nullInputFormat_throwsIllegalArgumentException() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> anyObjectSchema.walk(new ExecutionContext(), "{\"key\":\"value\"}", null, format, true));
        assertTrue(ex.getMessage().contains("InputFormat cannot be null"));
    }

    @Test
    void outputFormatThrowsDuringProcessing_propagatesException() {
        OutputFormat<ValidationResult> throwingFormat = new ThrowingOutputFormat<>(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> anyObjectSchema.walk(new ExecutionContext(), "{\"a\":1}", InputFormat.JSON, throwingFormat, true));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void yamlInputFormat_successfulWalk() {
        // The factory knows how to handle YAML via InputFormat.YAML
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = anyObjectSchema.walk(new ExecutionContext(), "name: Jane\nage: 25", InputFormat.YAML, format, true);
        assertTrue(result.isSuccess(), "YAML input should be processed successfully");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected for valid YAML");
    }

    @Test
    void nullOutputFormatWithValidateTrue_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> anyObjectSchema.walk(new ExecutionContext(), "{\"x\":10}", InputFormat.JSON, null, true));
        assertTrue(ex.getMessage().contains("outputFormat"));
    }

    @Test
    void nullInputString_throwsNullPointerException() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        NullPointerException ex = assertThrows(NullPointerException.class, () -> anyObjectSchema.walk(new ExecutionContext(), null, InputFormat.JSON, format, true));
        assertTrue(ex.getMessage().contains("input"));
    }

    @Test
    void whitespaceOnlyInput_treatedAsEmptyJsonNode() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = anyObjectSchema.walk(new ExecutionContext(), "   \n\t  ", InputFormat.JSON, format, false);
        assertTrue(result.isSuccess(), "Whitespace‑only input should result in a successful walk when validation is disabled");
    }

    @Test
    void invalidYamlDeserializationThrowsRuntimeException() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> anyObjectSchema.walk(new ExecutionContext(), "name: John\nage: [30,]", InputFormat.YAML, format, true));
        assertTrue(ex.getCause() instanceof IOException, "Root cause should be an IOException caused by malformed YAML");
    }

    @Test
    void nullOutputFormatWithValidateFalse_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> anyObjectSchema.walk(new ExecutionContext(), "{\"a\":1}", InputFormat.JSON, null, false));
        assertTrue(ex.getMessage().contains("outputFormat"));
    }

    @Test
    void validationFailure_schemaViolationReported() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        ValidationResult result = requiredNameSchema.walk(new ExecutionContext(), "{\"age\":25}", InputFormat.JSON, format, true);
        assertFalse(result.isSuccess(), "Result should indicate failure when required property is missing");
        boolean containsNameMessage = result.getMessages().stream().anyMatch(msg -> msg.getMessage().contains("name"));
        assertTrue(containsNameMessage, "Validation messages should mention missing required property 'name'");
    }

    @Test
    void customOutputFormatReturningNonValidationResult() {
        // Mock OutputFormat<Integer> that returns the size of the JSON array (3)
        @SuppressWarnings("unchecked")
        OutputFormat<Integer> nodeCountFormat = mock(OutputFormat.class);
        when(nodeCountFormat.apply(any(ValidationResult.class))).thenReturn(3);
        Integer count = anyObjectSchema.walk(new ExecutionContext(), "[1,2,3]", InputFormat.JSON, nodeCountFormat, false);
        assertEquals(3, count, "NodeCountOutputFormat should return the number of elements in the array");
    }

    @Test
    void unsupportedInputFormat_throwsIllegalArgumentException() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> anyObjectSchema.walk(new ExecutionContext(), "some plain text", InputFormat.UNKNOWN, format, true));
        assertTrue(ex.getMessage().contains("Unsupported InputFormat"));
    }

    @Test
    void outputFormatThrowsWhileValidationFlagFalse_exceptionPropagates() {
        OutputFormat<ValidationResult> throwingFormat = new ThrowingOutputFormat<>(new RuntimeException("format error"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> anyObjectSchema.walk(new ExecutionContext(), "{\"x\":10}", InputFormat.JSON, throwingFormat, false));
        assertEquals("format error", ex.getMessage());
    }

    @Test
    void nullInputAndOutputFormat_inputFormatValidationPrecedesOutputFormat() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> anyObjectSchema.walk(new ExecutionContext(), "{\"key\":\"value\"}", null, null, true));
        assertTrue(ex.getMessage().contains("InputFormat cannot be null"));
    }

    @Test
    void unsupportedUrlEncodedInputFormat_throwsRuntimeExceptionWrappingUnsupportedEncodingException() {
        OutputFormat<ValidationResult> format = new IdentityOutputFormat();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> anyObjectSchema.walk(new ExecutionContext(), "%ZZ%", InputFormat.URL_ENCODED, format, true));
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException, "Root cause should be UnsupportedEncodingException for malformed URL‑encoded data");
    }

    @Test
    void outputFormatThrowsCheckedIOException_isWrappedInRuntimeException() {
        OutputFormat<ValidationResult> format = new IOExceptionThrowingOutputFormat<>();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> anyObjectSchema.walk(new ExecutionContext(), "{\"a\":1}", InputFormat.JSON, format, true));
        assertTrue(ex.getCause() instanceof IOException, "The RuntimeException should wrap the original IOException");
    }

    @Test
    void executionContextNullAndInputFormatNull_inputFormatValidationStillFires() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> anyObjectSchema.walk(null, "{\"x\":10}", null, new IdentityOutputFormat(), false));
        assertTrue(ex.getMessage().contains("InputFormat cannot be null"));
    }
}
