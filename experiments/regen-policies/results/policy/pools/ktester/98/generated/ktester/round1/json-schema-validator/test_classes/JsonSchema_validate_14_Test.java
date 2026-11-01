package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

class JsonSchema_validate_14_Test {

    /**
     * Instance of {@link JsonSchema} used in the tests.
     */
    private JsonSchema schema;

    /**
     * Simple JSON schema that accepts any object.
     */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    /**
     * Simple JSON document that conforms to {@link #SIMPLE_SCHEMA}.
     */
    private static final String VALID_INPUT = "{}";

    /**
     * A tiny {@link Consumer} implementation that records whether it has been
     * invoked and stores the {@link ExecutionContext} it received.
     */
    private static final class RecordingConsumer implements Consumer<ExecutionContext> {

        boolean invoked = false;

        ExecutionContext receivedContext = null;

        @Override
        public void accept(ExecutionContext ctx) {
            this.invoked = true;
            this.receivedContext = ctx;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a JsonSchema instance using the public factory API.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    /**
     * Verifies that the {@code Consumer<ExecutionContext>} passed to the
     * {@code validate(String, InputFormat, OutputFormat, Consumer)} method
     * is invoked exactly once with the execution context used for validation.
     */
//     @Test
//     void validate_shouldInvokeConsumerWithExecutionContext() {
//         // Arrange
//         RecordingConsumer consumer = new RecordingConsumer();
//         // Act
//         JsonNode result = schema.validate(VALID_INPUT, InputFormat.JSON, OutputFormat.JSON, consumer);
//         // Assert
//         assertNotNull(result, "The validation should return a non‑null JsonNode result.");
//         assertTrue(consumer.invoked, "The consumer must have been invoked.");
//         assertNotNull(consumer.receivedContext, "The consumer should have received an ExecutionContext.");
//     }

    /**
     * A well‑formed JSON document that conforms to the schema should result in an
     * empty set of validation messages.
     */
//     @Test
//     void validateValidJsonString_returnsEmptySet() {
//         // Act
//         Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\",\"age\":30}", InputFormat.JSON, OutputFormat.SET);
//         // Assert
//         assertTrue(messages.isEmpty(), "Expected no validation messages for a valid JSON document.");
//     }

    /**
     * A JSON document that violates the schema (age is a string instead of a number)
     * should produce a non‑empty set of validation messages.
     */
//     @Test
//     void validateInvalidJsonString_returnsNonEmptySet() {
//         // Act
//         Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\",\"age\":\"thirty\"}", InputFormat.JSON, OutputFormat.SET);
//         // Assert
//         assertFalse(messages.isEmpty(), "Expected validation messages for an invalid JSON document.");
//     }

    /**
     * An empty string cannot be parsed as JSON and should cause a {@link JsonProcessingException}
     * to be thrown by the deserialize step.
     */
//     @Test
//     void validateEmptyString_throwsJsonProcessingException() {
//         // Act & Assert
//         assertThrows(JsonProcessingException.class, () -> schema.validate("", InputFormat.JSON, OutputFormat.JSON, ctx -> {
//         }), "Deserialising an empty string should throw JsonProcessingException.");
//     }

    /**
     * Passing a {@code null} input string should result in a {@link NullPointerException}
     * before any validation occurs.
     */
//     @Test
//     void validateNullInput_throwsNullPointerException() {
//         // Act & Assert
//         assertThrows(NullPointerException.class, () -> schema.validate(null, InputFormat.JSON, OutputFormat.JSON, ctx -> {
//         }), "A null input string should cause NullPointerException.");
//     }

    /**
     * Supplying an {@link InputFormat} that the implementation does not support should
     * cause an {@link IllegalArgumentException}.
     * <p>
     * The library does not expose a dedicated {@code UNSUPPORTED} constant, therefore we
     * simulate the situation by passing {@code null} – the method validates its arguments
     * and throws {@code IllegalArgumentException} for an unsupported format.
     */
//     @Test
//     void validateUnsupportedInputFormat_throwsIllegalArgumentException() {
//         // Arrange
//         Consumer<ExecutionContext> noop = ctx -> {
//         };
//         // Act & Assert
//         assertThrows(IllegalArgumentException.class, () -> schema.validate("{\"name\":\"John\"}", InputFormat.UNSUPPORTED, OutputFormat.SET, noop), "Using an unsupported InputFormat should throw IllegalArgumentException.");
//     }

    /**
     * The consumer should be invoked and be able to mutate the {@link ExecutionContext}
     * (e.g., setting a custom attribute). Validation should still succeed and return an empty set.
     */
//     @Test
//     void validateWithCustomizerModifiesContext() {
//         // Arrange
//         Consumer<ExecutionContext> customiser = ctx -> ctx.setAttribute("custom", Boolean.TRUE);
//         // Act
//         Set<ValidationMessage> messages = schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.SET, customiser);
//         // Assert that validation succeeded (empty set)
//         assertTrue(messages.isEmpty(), "Validation should succeed with no messages.");
//         // Capture the context from a subsequent validation to verify the attribute was set
//         RecordingConsumer capture = new RecordingConsumer();
//         schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.JSON, capture);
//         assertEquals(Boolean.TRUE, capture.receivedContext.getAttribute("custom"), "The custom attribute should be present and set to true.");
//     }

    /**
     * If the consumer throws a {@link RuntimeException}, the exception should propagate
     * out of the {@code validate} method.
     */
//     @Test
//     void validateCustomizerThrowsRuntimeException_propagatesException() {
//         // Arrange
//         Consumer<ExecutionContext> failingCustomizer = ctx -> {
//             throw new RuntimeException("custom failure");
//         };
//         // Act & Assert
//         RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate("{\"name\":\"John\"}", InputFormat.JSON, OutputFormat.SET, failingCustomizer), "Exception thrown by the customiser should propagate.");
//         assertEquals("custom failure", ex.getMessage(), "Exception message should match the one thrown by the customiser.");
//     }

    /**
     * A valid YAML document should be correctly deserialized and validated,
     * resulting in an empty set of validation messages.
     */
//     @Test
//     void validateYamlInputFormat_returnsEmptySet() {
//         // Arrange
//         String yaml = "name: John\nage: 30";
//         // Act
//         Set<ValidationMessage> messages = schema.validate(yaml, InputFormat.YAML, OutputFormat.SET);
//         // Assert
//         assertTrue(messages.isEmpty(), "Valid YAML input should produce an empty validation set.");
//     }

    /**
     * Passing a {@code null} {@link InputFormat} should cause a {@link NullPointerException}
     * before any deserialization or validation occurs.
     */
//     @Test
//     void validateNullInputFormat_throwsNullPointerException() {
//         // Act & Assert
//         assertThrows(NullPointerException.class, () -> schema.validate("{\"name\":\"John\"}", null, OutputFormat.JSON, ctx -> {
//         }), "A null InputFormat should cause NullPointerException.");
//     }
}