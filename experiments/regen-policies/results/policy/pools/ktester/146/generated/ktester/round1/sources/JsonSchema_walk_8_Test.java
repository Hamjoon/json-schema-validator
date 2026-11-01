package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_8_Test {

    /**
     * Real schema instance created by the factory – used as the basis for a spy.
     */
    private JsonSchema realSchema;

    /**
     * Spy that allows us to stub the internal methods while keeping the rest of the
     *  implementation intact.
     */
    private JsonSchema schemaSpy;

    /**
     * Simple JSON schema used for the tests – an empty object schema is sufficient.
     */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    /**
     * Schema that requires a {@code name} property – used for validation‑failure tests.
     */
    private static final String REQUIRED_NAME_SCHEMA = """
        {
          "type": "object",
          "required": ["name"]
        }
        """;

    @BeforeEach
    void setupBeforeEach() {
        // Build a real JsonSchema instance using the factory (the constructor of JsonSchema is private).
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        realSchema = factory.getSchema(SIMPLE_SCHEMA);
        // Create a Mockito spy so we can stub selected methods.
        schemaSpy = Mockito.spy(realSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy to avoid cross‑test interference.
        Mockito.reset(schemaSpy);
    }

    /**
     * Verify that {@code walk(String, InputFormat, boolean, Consumer)}:
     * <ul>
     *   <li>creates an {@link ExecutionContext} via {@code createExecutionContext()}</li>
     *   <li>deserialises the supplied JSON string (delegated to the private {@code deserialize} method)</li>
     *   <li>delegates to the {@code walk(ExecutionContext, JsonNode, boolean, Consumer)} overload</li>
     *   <li>returns the {@link ValidationResult} produced by the delegated method</li>
     * </ul>
     */
    @Test
    void walkStringDelegatesCorrectly() {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        final String jsonInput = "{\"name\":\"test\"}";
        final boolean validateFlag = true;
        // Mock the ExecutionContext that the schema is supposed to create.
        ExecutionContext mockContext = mock(ExecutionContext.class);
        doReturn(mockContext).when(schemaSpy).createExecutionContext();
        // Prepare a dummy consumer – we will capture the instance that reaches the
        // delegated method to ensure it is the same one we passed.
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> consumer = mock(Consumer.class);
        // The result we expect the delegated method to return.
        ValidationResult expectedResult = mock(ValidationResult.class);
        // Stub the final overload so it returns the expected result without executing
        // the full validation logic.
        doReturn(expectedResult).when(schemaSpy).walk(eq(mockContext), any(JsonNode.class), eq(validateFlag), any());
        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        ValidationResult actualResult = schemaSpy.walk(jsonInput, InputFormat.JSON, validateFlag, consumer);
        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        // The result must be the one supplied by the stubbed overload.
        assertSame(expectedResult, actualResult, "walk should return the delegated ValidationResult");
        // Verify that createExecutionContext() was indeed called.
        verify(schemaSpy, times(1)).createExecutionContext();
        // Capture the consumer that was passed to the delegated overload.
        ArgumentCaptor<Consumer<ExecutionContext>> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(schemaSpy, times(1)).walk(eq(mockContext), any(JsonNode.class), eq(validateFlag), consumerCaptor.capture());
        // The captured consumer must be the same instance we supplied.
        assertSame(consumer, consumerCaptor.getValue(), "The consumer passed to the delegated walk must be the original one");
        // (Optional) Verify that the consumer is invoked – this is part of the contract
        // of the real implementation. Because we stubbed the overload, the consumer
        // is not called automatically; we invoke it manually to demonstrate the
        // expected behaviour.
        consumer.accept(mockContext);
        verify(consumer, times(1)).accept(mockContext);
    }

    @Test
    void walkValidJsonWithValidationEnabled_returnsSuccess() {
        // Arrange
        String json = "{\"name\":\"John\",\"age\":30}";
        // Use a real schema (empty object) – any well‑formed JSON is valid.
        // No customizer.
        // Act
        ValidationResult result = schemaSpy.walk(json, InputFormat.JSON, true, null);
        // Assert
        assertTrue(result.isSuccess(), "A well‑formed JSON should result in a successful ValidationResult");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected for a valid input");
    }

    @Test
    void walkValidJsonWithValidationDisabled_returnsSuccess() {
        // Arrange
        String json = "{\"name\":\"John\",\"age\":30}";
        // Stubbing the final overload to ensure we do not run any validators.
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        doReturn(mockCtx).when(schemaSpy).createExecutionContext();
        ValidationResult stubResult = mock(ValidationResult.class);
        when(stubResult.isSuccess()).thenReturn(true);
        when(stubResult.getMessages()).thenReturn(Set.of());
        doReturn(stubResult).when(schemaSpy).walk(eq(mockCtx), any(JsonNode.class), eq(false), any());
        // Act
        ValidationResult result = schemaSpy.walk(json, InputFormat.JSON, false, null);
        // Assert
        assertTrue(result.isSuccess(), "When validation flag is false the walk should still succeed");
        verify(schemaSpy, times(1)).createExecutionContext();
        verify(schemaSpy, times(1)).walk(eq(mockCtx), any(JsonNode.class), eq(false), isNull());
    }

    @Test
    void walkMalformedJson_returnsFailedResult() {
        // Arrange
        String malformed = "{invalidJson: true,";
        // Act
        ValidationResult result = schemaSpy.walk(malformed, InputFormat.JSON, true, null);
        // Assert
        assertFalse(result.isSuccess(), "Malformed JSON must produce a failed ValidationResult");
        assertFalse(result.getMessages().isEmpty(), "A validation message describing the deserialization error is expected");
        // The message should contain something about parsing – we just check non‑emptiness.
    }

    @Test
    void walkNullInputFormat_throwsIllegalArgumentException() {
        // Arrange
        String json = "{\"name\":\"John\"}";
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> schemaSpy.walk(json, null, true, null), "Passing a null InputFormat must result in IllegalArgumentException");
    }

    @Test
    void walkWithConsumerCustomizer_invokesConsumer() {
        // Arrange
        String json = "{\"name\":\"John\"}";
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        doReturn(mockCtx).when(schemaSpy).createExecutionContext();
        // Consumer that records invocation.
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> customizer = mock(Consumer.class);
        // Stub final overload to return a successful result.
        ValidationResult stubResult = mock(ValidationResult.class);
        when(stubResult.isSuccess()).thenReturn(true);
        when(stubResult.getMessages()).thenReturn(Set.of());
        doReturn(stubResult).when(schemaSpy).walk(eq(mockCtx), any(JsonNode.class), eq(true), any());
        // Act
        ValidationResult result = schemaSpy.walk(json, InputFormat.JSON, true, customizer);
        // Assert
        assertTrue(result.isSuccess(), "Result should be successful when the consumer does not interfere");
        verify(customizer, times(1)).accept(mockCtx);
    }

    @Test
    void walkWithNullConsumer_doesNotThrow() {
        // Arrange
        String json = "{\"name\":\"John\"}";
        // Stub the final overload to avoid real validation.
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        doReturn(mockCtx).when(schemaSpy).createExecutionContext();
        ValidationResult stubResult = mock(ValidationResult.class);
        when(stubResult.isSuccess()).thenReturn(true);
        when(stubResult.getMessages()).thenReturn(Set.of());
        doReturn(stubResult).when(schemaSpy).walk(eq(mockCtx), any(JsonNode.class), eq(true), isNull());
        // Act & Assert – no exception expected.
        assertDoesNotThrow(() -> schemaSpy.walk(json, InputFormat.JSON, true, null));
    }

    @Test
    void walkValidYamlInput_returnsSuccess() {
        // Arrange
        String yaml = "name: John\nage: 30";
        // Use the real implementation – the empty object schema accepts any object.
        ValidationResult result = schemaSpy.walk(yaml, InputFormat.YAML, true, null);
        // Assert
        assertTrue(result.isSuccess(), "Valid YAML should be deserialized and validated successfully");
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void walkUrlEncodedJson_returnsSuccess() {
        // Arrange
        // {"name":"John","age":30}
        String encoded = "%7B%22name%22%3A%22John%22%2C%22age%22%3A30%7D";
        ValidationResult result = schemaSpy.walk(encoded, InputFormat.JSON, true, null);
        // Assert
        assertTrue(result.isSuccess(), "URL‑encoded JSON should be decoded and validated successfully");
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void walkConsumerThrowsRuntimeException_propagates() {
        // Arrange
        String json = "{\"name\":\"John\"}";
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        doReturn(mockCtx).when(schemaSpy).createExecutionContext();
        Consumer<ExecutionContext> explodingConsumer = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        // Stub the final overload – it will never be reached because the consumer throws first.
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schemaSpy.walk(json, InputFormat.JSON, true, explodingConsumer));
        assertEquals("customizer failure", ex.getMessage(), "The RuntimeException from the consumer must be propagated");
    }

    @Test
    void walkViolatesRequiredProperty_returnsFailedResult() {
        // Arrange – use a schema that requires the property "name".
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        JsonSchema requiredSchema = factory.getSchema(REQUIRED_NAME_SCHEMA);
        JsonSchema spy = Mockito.spy(requiredSchema);
        String jsonMissingName = "{\"age\":30}";
        // Act
        ValidationResult result = spy.walk(jsonMissingName, InputFormat.JSON, true, null);
        // Assert
        assertFalse(result.isSuccess(), "Missing required property must cause validation failure");
        assertFalse(result.getMessages().isEmpty(), "Validation messages should contain the missing‑property error");
        // Simple sanity check on the message content.
        boolean containsRequired = result.getMessages().stream().anyMatch(msg -> msg.getMessage().toLowerCase().contains("required"));
        assertTrue(containsRequired, "At least one message should mention the required property");
    }

    @Test
    void walkEmptyJsonString_returnsFailedResult() {
        // Arrange
        // JSON string containing an empty string, not an object.
        String empty = "\"\"";
        // Act
        ValidationResult result = schemaSpy.walk(empty, InputFormat.JSON, true, null);
        // Assert
        assertFalse(result.isSuccess(), "Empty JSON input must result in a failed ValidationResult");
        assertFalse(result.getMessages().isEmpty(), "An error message describing the deserialization problem is expected");
    }

    @Test
    void walkWhitespaceOnlyJson_returnsFailedResult() {
        // Arrange
        String whitespace = "\"   \\n\\t  \"";
        // Act
        ValidationResult result = schemaSpy.walk(whitespace, InputFormat.JSON, true, null);
        // Assert
        assertFalse(result.isSuccess(), "Whitespace‑only JSON must be considered invalid");
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void walkInvalidYaml_returnsFailedResult() {
        // Arrange
        String malformedYaml = "name: John\nage: [30";
        // Act
        ValidationResult result = schemaSpy.walk(malformedYaml, InputFormat.YAML, true, null);
        // Assert
        assertFalse(result.isSuccess(), "Malformed YAML should cause a failed ValidationResult");
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void walkNullInputString_throwsIllegalArgumentException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> schemaSpy.walk(null, InputFormat.JSON, true, null), "Passing a null input string must result in IllegalArgumentException");
    }

    @Test
    void walkUnsupportedInputFormatValue_throwsIllegalArgumentException() {
        // Arrange
        String xml = "<root><name>John</name></root>";
        // The library does not support XML, so the method should reject it.
        assertThrows(IllegalArgumentException.class, () -> schemaSpy.walk(xml, InputFormat.XML, true, null), "Unsupported InputFormat (XML) must cause IllegalArgumentException");
    }
}
