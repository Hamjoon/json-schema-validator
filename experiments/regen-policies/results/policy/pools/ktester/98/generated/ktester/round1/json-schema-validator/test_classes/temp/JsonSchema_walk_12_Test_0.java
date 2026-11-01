package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_12_Test {

    private static JsonSchemaFactory factory;

    private static ObjectMapper mapper;

    @Mock
    private OutputFormat<String> // mocked OutputFormat used in all tests
    outputFormatMock;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a simple draft‑07 schema that does not impose any constraints.
        // This keeps the validation path trivial and guarantees that validation succeeds.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test so that previous interactions do not interfere.
        reset(outputFormatMock);
        // By default the mock returns the set of messages contained in the ValidationResult.
        when(outputFormatMock.apply(any())).thenAnswer(invocation -> {
            ValidationResult vr = invocation.getArgument(0);
            return vr.getMessages();
        });
    }

    /**
     * Verifies that {@code walk(..., validate = false, customizer = null)} returns the value
     * produced by the supplied {@link OutputFormat}.
     */
    @Test
    void walkWithoutValidationAndWithoutCustomizer_returnsOutputFormatResult() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        ExecutionContext executionContext = new ExecutionContext();
        when(outputFormatMock.apply(any())).thenReturn("formatted-result");
        // WHEN
        String result = schema.walk(executionContext, node, outputFormatMock, false, (ExecutionContextCustomizer) null);
        // THEN
        assertEquals("formatted-result", result, "walk should return the value from OutputFormat");
        verify(outputFormatMock, times(1)).apply(any());
    }

    /**
     * Verifies that {@code walk(..., validate = true, customizer = null)} also returns the value
     * from the {@link OutputFormat} when the schema validates successfully.
     */
    @Test
    void walkWithValidationAndWithoutCustomizer_successfulValidation_returnsOutputFormatResult() throws Exception {
        // GIVEN
        // A schema that requires a string property "name". The provided node satisfies the schema.
        JsonSchema schema = factory.getSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}");
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        ExecutionContext executionContext = new ExecutionContext();
        when(outputFormatMock.apply(any())).thenReturn("ok");
        // WHEN
        String result = schema.walk(executionContext, node, outputFormatMock, true, (ExecutionContextCustomizer) null);
        // THEN
        assertEquals("ok", result);
        verify(outputFormatMock, times(1)).apply(any());
    }

    /**
     * Verifies that a non‑null {@link ExecutionContextCustomizer} is accepted and does not interfere
     * with the normal flow of {@code walk}.
     */
    @Test
    void walkWithCustomizer_isInvokedAndReturnsOutputFormatResult() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"age\":30}");
        ExecutionContext executionContext = new ExecutionContext();
        // A simple customizer that adds a dummy collector to the context (no‑op for this test)
        ExecutionContextCustomizer customizer = ctx -> {
            // No real customization needed; just ensure the lambda can be called.
        };
        when(outputFormatMock.apply(any())).thenReturn("customized");
        // WHEN
        String result = schema.walk(executionContext, node, outputFormatMock, false, customizer);
        // THEN
        assertEquals("customized", result);
        verify(outputFormatMock, times(1)).apply(any());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_validStringNode_validatesSuccessfully
     * -------------------------------------------------------------- */
    @Test
    void walk_validStringNode_validatesSuccessfully() throws Exception {
        // GIVEN – a schema that only accepts a JSON string
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("\"hello\"");
        ExecutionContext executionContext = new ExecutionContext();
        // WHEN
        Set<ValidationMessage> result = schema.walk(executionContext, node, outputFormatMock, // validation enabled
        true, // no customizer
        (ExecutionContextCustomizer) null);
        // THEN – validation succeeded, therefore the result set must be empty
        assertTrue(result.isEmpty(), "When the node matches the schema, walk should return an empty set of ValidationMessage");
        verify(outputFormatMock, times(1)).apply(any());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_invalidStringNode_validationFails
     * -------------------------------------------------------------- */
    @Test
    void walk_invalidStringNode_validationFails() throws Exception {
        // GIVEN – a schema that expects a string, but the node is a number
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("123");
        ExecutionContext executionContext = new ExecutionContext();
        // WHEN
        Set<ValidationMessage> result = schema.walk(executionContext, node, outputFormatMock, // validation enabled
        true, (ExecutionContextCustomizer) null);
        // THEN – we expect exactly one message describing the type mismatch
        assertEquals(1, result.size(), "A type‑mismatch should produce a single ValidationMessage");
        ValidationMessage message = result.iterator().next();
        assertTrue(message.getMessage().contains("expected type: String"), "The message should mention the expected type String");
        verify(outputFormatMock, times(1)).apply(any());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_validateFlagFalse_skipsValidation
     * -------------------------------------------------------------- */
    @Test
    void walk_validateFlagFalse_skipsValidation() throws Exception {
        // GIVEN – same schema as previous test but validation is disabled
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        // mismatching type
        JsonNode node = mapper.readTree("123");
        ExecutionContext executionContext = new ExecutionContext();
        // WHEN
        Set<ValidationMessage> result = schema.walk(executionContext, node, outputFormatMock, // validation disabled
        false, (ExecutionContextCustomizer) null);
        // THEN – because validation is skipped, the result must be empty
        assertTrue(result.isEmpty(), "When validation flag is false, walk should skip validation and return an empty set");
        verify(outputFormatMock, times(1)).apply(any());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_withExecutionCustomizerAddsCustomMessage
     * -------------------------------------------------------------- */
    @Test
    void walk_withExecutionCustomizerAddsCustomMessage() throws Exception {
        // GIVEN – a simple schema that accepts any string
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("\"test\"");
        ExecutionContext executionContext = new ExecutionContext();
        // A customizer that we can verify was invoked.
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        // The mock OutputFormat will ignore the ValidationResult and return a set
        // containing a synthetic custom message, emulating a customizer‑added message.
        Set<ValidationMessage> customSet = new HashSet<>();
        ValidationMessage customMessage = ValidationMessage.of("custom: added by ExecutionContextCustomizer");
        customSet.add(customMessage);
        when(outputFormatMock.apply(any())).thenReturn(customSet);
        // WHEN
        Set<ValidationMessage> result = schema.walk(executionContext, node, outputFormatMock, true, customizer);
        // THEN – the customizer must have been called and the result must contain the synthetic message
        verify(customizer, times(1)).customize(any());
        assertEquals(1, result.size(), "Result set should contain exactly the custom message injected by the test setup");
        assertTrue(result.contains(customMessage), "Result set must contain the expected custom ValidationMessage");
        verify(outputFormatMock, times(1)).apply(any());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_nullNode_throwsNullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walk_nullNode_throwsNullPointerException() {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext executionContext = new ExecutionContext();
        // WHEN / THEN – a NullPointerException is expected because the node argument is null
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, null, outputFormatMock, true, (ExecutionContextCustomizer) null));
    }

    /* --------------------------------------------------------------
     *  Test group: walk_nullExecutionContext_throwsNullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walk_nullExecutionContext_throwsNullPointerException() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"valid\":\"yes\"}");
        // WHEN / THEN – a NullPointerException is expected because the executionContext argument is null
        assertThrows(NullPointerException.class, () -> schema.walk(null, node, outputFormatMock, true, (ExecutionContextCustomizer) null));
    }
}
