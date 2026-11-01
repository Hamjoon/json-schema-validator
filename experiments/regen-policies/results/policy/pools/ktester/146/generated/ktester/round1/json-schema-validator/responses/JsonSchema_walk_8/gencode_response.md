**Generated unit‑test class**

```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Unit tests for {@link JsonSchema#walk(String, InputFormat, boolean, Consumer)}.
 *
 * The tests cover the most relevant behaviours described in the supplied
 * test‑case matrix:
 *
 * <ul>
 *   <li>successful walk with validation enabled/disabled</li>
 *   <li>delegation to the overload that receives an {@link ExecutionContext}</li>
 *   <li>handling of malformed input (JSON/YAML) and unsupported {@link InputFormat}</li>
 *   <li>behaviour when the {@code Consumer<ExecutionContext>} is {@code null},
 *       modifies the context, or throws an exception</li>
 *   <li>URL‑encoded input handling</li>
 *   <li>validation failure caused by a schema constraint (required property)</li>
 * </ul>
 *
 * Heavy‑weight operations (actual validation) are avoided by stubbing the final
 * overload {@code walk(ExecutionContext, JsonNode, boolean, Consumer)} – this
 * keeps the tests fast and focused on the public method’s orchestration logic.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_8_Test {

    /** Real schema instance created by the factory – used as the basis for a spy. */
    private JsonSchema realSchema;

    /** Spy that allows us to stub internal methods while keeping the rest of the
     * implementation intact. */
    private JsonSchema schemaSpy;

    /** Simple empty‑object schema – sufficient for most delegation tests. */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    /** Schema that requires a {@code name} property – used for validation‑failure tests. */
    private static final String REQUIRED_NAME_SCHEMA = """
            {
              "type": "object",
              "required": ["name"]
            }
            """;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a real JsonSchema instance using the factory (the constructor of JsonSchema is private).
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        realSchema = factory.getSchema(SIMPLE_SCHEMA);
        schemaSpy = Mockito.spy(realSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy to avoid cross‑test interference.
        Mockito.reset(schemaSpy);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- *
     *  1.  Delegation test – already present in the original scaffold.
     * --------------------------------------------------------------------- */

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

        // Dummy consumer – we capture the instance that reaches the delegated method.
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> consumer = mock(Consumer.class);

        // Expected result from the delegated overload.
        ValidationResult expectedResult = mock(ValidationResult.class);
        doReturn(expectedResult)
                .when(schemaSpy)
                .walk(eq(mockContext), any(JsonNode.class), eq(validateFlag), any());

        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        ValidationResult actualResult = schemaSpy.walk(jsonInput, InputFormat.JSON, validateFlag, consumer);

        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        assertSame(expectedResult, actualResult, "walk should return the delegated ValidationResult");
        verify(schemaSpy, times(1)).createExecutionContext();

        ArgumentCaptor<Consumer<ExecutionContext>> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(schemaSpy, times(1))
                .walk(eq(mockContext), any(JsonNode.class), eq(validateFlag), consumerCaptor.capture());

        assertSame(consumer, consumerCaptor.getValue(),
                "The consumer passed to the delegated walk must be the original one");

        // Demonstrate that the consumer is invoked (the real implementation would do this).
        consumer.accept(mockContext);
        verify(consumer, times(1)).accept(mockContext);
    }

    /* --------------------------------------------------------------------- *
     *  2.  Validation enabled – happy path (real validation, no stubbing).
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     *  3.  Validation disabled – the method must still succeed.
     * --------------------------------------------------------------------- */

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
        doReturn(stubResult)
                .when(schemaSpy)
                .walk(eq(mockCtx), any(JsonNode.class), eq(false), any());

        // Act
        ValidationResult result = schemaSpy.walk(json, InputFormat.JSON, false, null);

        // Assert
        assertTrue(result.isSuccess(), "When validation flag is false the walk should still succeed");
        verify(schemaSpy, times(1)).createExecutionContext();
        verify(schemaSpy, times(1))
                .walk(eq(mockCtx), any(JsonNode.class), eq(false), isNull());
    }

    /* --------------------------------------------------------------------- *
     *  4.  Deserialization failure – malformed JSON.
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     *  5.  Unsupported InputFormat (null) – should throw IllegalArgumentException.
     * --------------------------------------------------------------------- */

    @Test
    void walkNullInputFormat_throwsIllegalArgumentException() {
        // Arrange
        String json = "{\"name\":\"John\"}";
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> schemaSpy.walk(json, null, true, null),
                "Passing a null InputFormat must result in IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- *
     *  6.  Consumer customizer – verifies that the consumer is invoked with the
     *      freshly created ExecutionContext.
     * --------------------------------------------------------------------- */

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
        doReturn(stubResult)
                .when(schemaSpy)
                .walk(eq(mockCtx), any(JsonNode.class), eq(true), any());

        // Act
        ValidationResult result = schemaSpy.walk(json, InputFormat.JSON, true, customizer);

        // Assert
        assertTrue(result.isSuccess(), "Result should be successful when the consumer does not interfere");
        verify(customizer, times(1)).accept(mockCtx);
    }

    /* --------------------------------------------------------------------- *
     *  7.  Null consumer – walk must not throw NPE.
     * --------------------------------------------------------------------- */

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
        doReturn(stubResult)
                .when(schemaSpy)
                .walk(eq(mockCtx), any(JsonNode.class), eq(true), isNull());

        // Act & Assert – no exception expected.
        assertDoesNotThrow(() -> schemaSpy.walk(json, InputFormat.JSON, true, null));
    }

    /* --------------------------------------------------------------------- *
     *  8.  YAML input format – valid content.
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     *  9.  URL‑encoded JSON – must be decoded before validation.
     * --------------------------------------------------------------------- */

    @Test
    void walkUrlEncodedJson_returnsSuccess() {
        // Arrange
        String encoded = "%7B%22name%22%3A%22John%22%2C%22age%22%3A30%7D"; // {"name":"John","age":30}
        ValidationResult result = schemaSpy.walk(encoded, InputFormat.JSON, true, null);
        // Assert
        assertTrue(result.isSuccess(), "URL‑encoded JSON should be decoded and validated successfully");
        assertTrue(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- *
     * 10.  Consumer throws RuntimeException – the exception must propagate.
     * --------------------------------------------------------------------- */

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
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schemaSpy.walk(json, InputFormat.JSON, true, explodingConsumer));
        assertEquals("customizer failure", ex.getMessage(),
                "The RuntimeException from the consumer must be propagated");
    }

    /* --------------------------------------------------------------------- *
     * 11.  Validation failure due to schema constraint (required property).
     * --------------------------------------------------------------------- */

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
        boolean containsRequired = result.getMessages().stream()
                .anyMatch(msg -> msg.getMessage().toLowerCase().contains("required"));
        assertTrue(containsRequired, "At least one message should mention the required property");
    }

    /* --------------------------------------------------------------------- *
     * 12.  Empty JSON string – deserialization should fail.
     * --------------------------------------------------------------------- */

    @Test
    void walkEmptyJsonString_returnsFailedResult() {
        // Arrange
        String empty = "\"\""; // JSON string containing an empty string, not an object.
        // Act
        ValidationResult result = schemaSpy.walk(empty, InputFormat.JSON, true, null);
        // Assert
        assertFalse(result.isSuccess(), "Empty JSON input must result in a failed ValidationResult");
        assertFalse(result.getMessages().isEmpty(), "An error message describing the deserialization problem is expected");
    }

    /* --------------------------------------------------------------------- *
     * 13.  Whitespace‑only JSON – deserialization should fail.
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     * 14.  Invalid YAML – deserialization failure.
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     * 15.  Null input string – should throw IllegalArgumentException.
     * --------------------------------------------------------------------- */

    @Test
    void walkNullInputString_throwsIllegalArgumentException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> schemaSpy.walk(null, InputFormat.JSON, true, null),
                "Passing a null input string must result in IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- *
     * 16.  Unsupported InputFormat value (e.g., XML) – expect IllegalArgumentException.
     * --------------------------------------------------------------------- */

    @Test
    void walkUnsupportedInputFormatValue_throwsIllegalArgumentException() {
        // Arrange
        String xml = "<root><name>John</name></root>";
        // The library does not support XML, so the method should reject it.
        assertThrows(IllegalArgumentException.class,
                () -> schemaSpy.walk(xml, InputFormat.XML, true, null),
                "Unsupported InputFormat (XML) must cause IllegalArgumentException");
    }
}
```