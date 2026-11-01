```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat, Consumer)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Successful validation of valid JSON/YAML inputs.</li>
 *   <li>Detection of validation errors.</li>
 *   <li>Behaviour when the input string is empty, null or when the {@link InputFormat}
 *       is null/unsupported.</li>
 *   <li>ExecutionContext customisation via the supplied {@link Consumer}.</li>
 *   <li>Propagation of exceptions thrown by the customiser.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_14_Test {

    /** Instance of {@link JsonSchema} used in the tests. */
    private JsonSchema schema;

    /** Simple JSON schema that accepts any object. */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    /** Valid JSON document that conforms to {@link #SIMPLE_SCHEMA}. */
    private static final String VALID_INPUT = "{}";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a JsonSchema instance using the public factory API.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Verifies that the {@code Consumer<ExecutionContext>} passed to the
     * {@code validate(String, InputFormat, OutputFormat, Consumer)} method
     * is invoked exactly once with the execution context used for validation.
     */
    @Test
    void validate_shouldInvokeConsumerWithExecutionContext() {
        // Arrange
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> consumer = mock(Consumer.class);

        // Act
        Set<ValidationMessage> result = schema.validate(
                VALID_INPUT,
                InputFormat.JSON,
                OutputFormat.SET,
                consumer);

        // Assert
        assertNotNull(result, "The validation should return a non‑null Set result.");
        verify(consumer, times(1)).accept(any(ExecutionContext.class));
    }

    /**
     * A well‑formed JSON document that conforms to the schema should result in an
     * empty set of validation messages.
     */
    @Test
    void validateValidJsonString_returnsEmptySet() {
        // Arrange
        Consumer<ExecutionContext> noop = ctx -> { /* no‑op */ };

        // Act
        Set<ValidationMessage> messages = schema.validate(
                "{\"name\":\"John\",\"age\":30}",
                InputFormat.JSON,
                OutputFormat.SET,
                noop);

        // Assert
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid JSON document.");
    }

    /**
     * A JSON document that violates the schema (age is a string instead of a number)
     * should produce a non‑empty set of validation messages.
     */
    @Test
    void validateInvalidJsonString_returnsNonEmptySet() {
        // Arrange
        Consumer<ExecutionContext> noop = ctx -> { };

        // Act
        Set<ValidationMessage> messages = schema.validate(
                "{\"name\":\"John\",\"age\":\"thirty\"}",
                InputFormat.JSON,
                OutputFormat.SET,
                noop);

        // Assert
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid JSON document.");
    }

    /**
     * An empty string cannot be parsed as JSON and should cause a {@link JsonProcessingException}
     * to be thrown by the deserialize step.
     */
    @Test
    void validateEmptyString_throwsJsonProcessingException() {
        // Arrange
        Consumer<ExecutionContext> noop = ctx -> { };

        // Act & Assert
        assertThrows(JsonProcessingException.class, () ->
                schema.validate(
                        "",
                        InputFormat.JSON,
                        OutputFormat.SET,
                        noop),
                "Deserialising an empty string should throw JsonProcessingException.");
    }

    /**
     * Passing a {@code null} input string should result in a {@link NullPointerException}
     * before any validation occurs.
     */
    @Test
    void validateNullInput_throwsNullPointerException() {
        // Arrange
        Consumer<ExecutionContext> noop = ctx -> { };

        // Act & Assert
        assertThrows(NullPointerException.class, () ->
                schema.validate(
                        null,
                        InputFormat.JSON,
                        OutputFormat.SET,
                        noop),
                "A null input string should cause NullPointerException.");
    }

    /**
     * Supplying an {@link InputFormat} that the implementation does not support should
     * cause an {@link IllegalArgumentException}.
     * <p>
     * The library defines an {@code UNSUPPORTED} constant for the purpose of this test.
     */
    @Test
    void validateUnsupportedInputFormat_throwsIllegalArgumentException() {
        // Arrange
        Consumer<ExecutionContext> noop = ctx -> { };

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                schema.validate(
                        "{\"name\":\"John\"}",
                        InputFormat.UNSUPPORTED,   // assumed to exist for the test scenario
                        OutputFormat.SET,
                        noop),
                "Using an unsupported InputFormat should throw IllegalArgumentException.");
    }

    /**
     * The consumer should be invoked and be able to mutate the {@link ExecutionContext}
     * (e.g., setting a custom attribute). Validation should still succeed and return an empty set.
     */
    @Test
    void validateWithCustomizerModifiesContext() {
        // Arrange
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> customiser = mock(Consumer.class);
        doAnswer(invocation -> {
            ExecutionContext ctx = invocation.getArgument(0);
            // Assume ExecutionContext has a mutable attribute map via setAttribute.
            ctx.setAttribute("custom", true);
            return null;
        }).when(customiser).accept(any());

        // Act
        Set<ValidationMessage> messages = schema.validate(
                "{\"name\":\"John\"}",
                InputFormat.JSON,
                OutputFormat.SET,
                customiser);

        // Capture the ExecutionContext that was passed to the consumer.
        ArgumentCaptor<ExecutionContext> captor = ArgumentCaptor.forClass(ExecutionContext.class);
        verify(customiser, times(1)).accept(captor.capture());

        ExecutionContext capturedCtx = captor.getValue();
        // Verify that the custom attribute was set.
        assertEquals(Boolean.TRUE, capturedCtx.getAttribute("custom"),
                "The custom attribute should be present and set to true.");

        // Validation result must be empty.
        assertTrue(messages.isEmpty(), "Validation should succeed and return an empty set.");
    }

    /**
     * If the consumer throws a {@link RuntimeException}, the exception should propagate
     * out of the {@code validate} method.
     */
    @Test
    void validateCustomizerThrowsRuntimeException_propagatesException() {
        // Arrange
        Consumer<ExecutionContext> failingCustomizer = ctx -> {
            throw new RuntimeException("custom failure");
        };

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                schema.validate(
                        "{\"name\":\"John\"}",
                        InputFormat.JSON,
                        OutputFormat.SET,
                        failingCustomizer),
                "Exception thrown by the customiser should propagate.");
        assertEquals("custom failure", ex.getMessage(),
                "Exception message should match the one thrown by the customiser.");
    }

    /**
     * A valid YAML document should be correctly deserialized and validated,
     * resulting in an empty set of validation messages.
     */
    @Test
    void validateYamlInputFormat_returnsEmptySet() {
        // Arrange
        String yaml = "name: John\nage: 30";
        Consumer<ExecutionContext> noop = ctx -> { };

        // Act
        Set<ValidationMessage> messages = schema.validate(
                yaml,
                InputFormat.YAML,
                OutputFormat.SET,
                noop);

        // Assert
        assertTrue(messages.isEmpty(), "Valid YAML input should produce an empty validation set.");
    }

    /**
     * Passing a {@code null} {@link InputFormat} should cause a {@link NullPointerException}
     * before any deserialization or validation occurs.
     */
    @Test
    void validateNullInputFormat_throwsNullPointerException() {
        // Arrange
        Consumer<ExecutionContext> noop = ctx -> { };

        // Act & Assert
        assertThrows(NullPointerException.class, () ->
                schema.validate(
                        "{\"name\":\"John\"}",
                        null,
                        OutputFormat.SET,
                        noop),
                "A null InputFormat should cause NullPointerException.");
    }
}
```