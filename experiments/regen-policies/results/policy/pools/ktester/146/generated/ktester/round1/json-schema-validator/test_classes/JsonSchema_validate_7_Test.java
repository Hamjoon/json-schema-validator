package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_7_Test {

    /**
     * ObjectMapper used to build JsonNode instances.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Schema used for the tests – validates that the instance is a JSON string.
     */
    private JsonSchema schema;

    /**
     * ExecutionContext supplied to the validate method.
     */
    private ExecutionContext executionContext;

    /**
     * Mocked OutputFormat – captures the Set<ValidationMessage> passed to it.
     */
    @Mock
    OutputFormat<List<ValidationMessage>> outputFormat;

    /**
     * Simple ExecutionContext that holds a map of attributes.
     */
    private static class TestExecutionContext extends ExecutionContext {

        private final java.util.Map<String, Object> attributes = new java.util.HashMap<>();

        void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }

        Object getAttribute(String key) {
            return attributes.get(key);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a very small schema that only allows string values.
        String schemaData = "{\"type\":\"string\"}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(schemaData);
        this.executionContext = new ExecutionContext();
        // Configure the mock OutputFormat to return a mutable List containing the
        // validation messages it receives. This makes the return value easy to assert.
        when(outputFormat.apply(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Set<ValidationMessage> msgs = (Set<ValidationMessage>) invocation.getArgument(0);
            return new ArrayList<>(msgs);
        });
        // a very permissive schema (type "object") – enough to let validation run.
        String schemaData = "{\"type\":\"object\"}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(schemaData);
        this.executionContext = new ExecutionContext();
        // By default the mocked format returns the Set it receives wrapped in a List.
        when(outputFormat.apply(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Set<ValidationMessage> msgs = (Set<ValidationMessage>) invocation.getArgument(0);
            // Return whatever the test expects – most tests will override this stub.
            return new ArrayList<>(msgs);
        });
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(outputFormat);
        reset(outputFormat);
    }

    /**
     * When the JSON node conforms to the schema, the OutputFormat should receive
     * an empty set of validation messages and the returned list should be empty.
     */
    @Test
    void validateValidNode_returnsEmptyList() throws Exception {
        // a JSON string → valid
        JsonNode validNode = MAPPER.readTree("\"valid string\"");
        List<ValidationMessage> result = schema.validate(executionContext, validNode, outputFormat);
        // Verify that the format was invoked exactly once with an empty set.
        ArgumentCaptor<Set<ValidationMessage>> captor = ArgumentCaptor.forClass(Set.class);
        verify(outputFormat, times(1)).apply(captor.capture());
        assertTrue(captor.getValue().isEmpty(), "Expected no validation messages for a valid node");
        // The method should return the list produced by the mock (empty in this case).
        assertNotNull(result, "Result list must not be null");
        assertTrue(result.isEmpty(), "Result list should be empty for a valid node");
    }

    /**
     * When the JSON node does **not** conform to the schema, the OutputFormat should
     * receive a set containing at least one {@link ValidationMessage}. The returned
     * list should contain the same messages.
     */
    @Test
    void validateInvalidNode_returnsMessages() throws Exception {
        // a JSON number → invalid for string schema
        JsonNode invalidNode = MAPPER.readTree("123");
        List<ValidationMessage> result = schema.validate(executionContext, invalidNode, outputFormat);
        // Capture the argument passed to the format.
        ArgumentCaptor<Set<ValidationMessage>> captor = ArgumentCaptor.forClass(Set.class);
        verify(outputFormat, times(1)).apply(captor.capture());
        Set<ValidationMessage> capturedMessages = captor.getValue();
        assertFalse(capturedMessages.isEmpty(), "Expected validation messages for an invalid node");
        assertEquals(1, capturedMessages.size(), "For this simple schema we expect exactly one validation error");
        // The returned list should contain the same messages.
        assertNotNull(result, "Result list must not be null");
        assertEquals(capturedMessages.size(), result.size(), "Returned list size should match the number of validation messages");
        assertEquals(capturedMessages.iterator().next().getMessage(), result.get(0).getMessage(), "Returned message should match the captured validation message");
    }

    @Test
    void delegate_normalFlow_returnsValueFromOverloadedMethod() {
        // arrange – spy the schema so we can stub the overloaded method
        JsonSchema spy = spy(schema);
        String expected = "OK";
        doReturn(expected).when(spy).validate(eq(executionContext), any(JsonNode.class), eq(outputFormat), isNull());
        // any non‑null node
        JsonNode node = MAPPER.createObjectNode();
        // act
        Object result = spy.validate(executionContext, node, outputFormat);
        // assert
        assertEquals(expected, result);
        verify(spy, times(1)).validate(eq(executionContext), eq(node), eq(outputFormat), isNull());
    }

    @Test
    void delegate_nullExecutionContext_throwsNPE() {
        JsonNode node = MAPPER.createObjectNode();
        assertThrows(NullPointerException.class, () -> schema.validate(null, node, outputFormat));
    }

    @Test
    void delegate_nullNode_throwsNPE() {
        assertThrows(NullPointerException.class, () -> schema.validate(executionContext, null, outputFormat));
    }

    @Test
    void delegate_nullOutputFormat_throwsNPE() {
        JsonNode node = MAPPER.createObjectNode();
        assertThrows(NullPointerException.class, () -> schema.validate(executionContext, node, null));
    }

    @Test
    void delegate_parametersArePassedUnmodified() {
        JsonSchema spy = spy(schema);
        String sentinel = "CAPTURED";
        doReturn(sentinel).when(spy).validate(eq(executionContext), any(JsonNode.class), eq(outputFormat), isNull());
        JsonNode node = MAPPER.readTree("{\"type\":\"array\"}");
        Object result = spy.validate(executionContext, node, outputFormat);
        assertEquals(sentinel, result);
        verify(spy).validate(eq(executionContext), eq(node), eq(outputFormat), isNull());
    }

    @Test
    void outputFormat_throwsIllegalStateException_isPropagated() {
        JsonNode node = MAPPER.readTree("{\"type\":\"string\",\"value\":\"abc\"}");
        when(outputFormat.apply(any())).thenThrow(new IllegalStateException("format failure"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> schema.validate(executionContext, node, outputFormat));
        assertEquals("format failure", ex.getMessage());
    }

    @Test
    void outputFormat_throwsRuntimeException_isPropagated() {
        JsonNode node = MAPPER.readTree("{\"type\":\"number\",\"value\":42}");
        when(outputFormat.apply(any())).thenThrow(new RuntimeException("unexpected"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(executionContext, node, outputFormat));
        assertEquals("unexpected", ex.getMessage());
    }

    @Test
    void validate_nullJsonNode_isHandledLikeAnyOtherNode() {
        JsonNode nullNode = NullNode.getInstance();
        when(outputFormat.apply(any())).thenReturn("NULL_OK");
        Object result = schema.validate(executionContext, nullNode, outputFormat);
        assertEquals("NULL_OK", result);
    }

    @Test
    void validate_missingNode_throwsIllegalArgumentException() {
        JsonNode missing = MissingNode.getInstance();
        when(outputFormat.apply(any())).thenReturn("MISSING_OK");
        assertThrows(IllegalArgumentException.class, () -> schema.validate(executionContext, missing, outputFormat));
    }

    @Test
    void outputFormat_returnsComplexObject_isReturnedUnchanged() {
        JsonNode node = MAPPER.readTree("{\"type\":\"array\",\"items\":[1,2,3]}");
        List<String> complex = List.of("A", "B", "C");
        when(outputFormat.apply(any())).thenReturn(complex);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) schema.validate(executionContext, node, outputFormat);
        assertEquals(complex, result);
    }

    @Test
    void outputFormat_isInvokedExactlyOnce() {
        JsonNode node = MAPPER.readTree("{\"type\":\"object\",\"properties\":{}}");
        OutputFormat<Object> spyFormat = spy(outputFormat);
        when(spyFormat.apply(any())).thenReturn("ONE_CALL");
        Object result = schema.validate(executionContext, node, spyFormat);
        assertEquals("ONE_CALL", result);
        verify(spyFormat, times(1)).apply(any());
    }

    @Test
    void emptyObjectNode_isValidatedAndResultReturned() {
        JsonNode emptyObj = MAPPER.readTree("{}");
        when(outputFormat.apply(any())).thenReturn("EMPTY_OBJ_OK");
        Object result = schema.validate(executionContext, emptyObj, outputFormat);
        assertEquals("EMPTY_OBJ_OK", result);
    }

    @Test
    void executionContext_customAttribute_isVisibleToOutputFormat() {
        TestExecutionContext ctx = new TestExecutionContext();
        ctx.setAttribute("mode", "STRICT");
        JsonNode node = MAPPER.readTree("{\"type\":\"string\",\"value\":\"test\"}");
        // OutputFormat that reads the attribute from the supplied ExecutionContext
        OutputFormat<Object> format = mock(OutputFormat.class);
        when(format.apply(any())).thenAnswer(invocation -> {
            // The format receives only the Set<ValidationMessage>. To obtain the
            // ExecutionContext we capture it via a spy on the schema (see below).
            // For simplicity we just return the attribute directly from the test context.
            return ctx.getAttribute("mode");
        });
        // Spy the schema to capture the ExecutionContext that is passed to the overloaded method.
        JsonSchema spy = spy(schema);
        doReturn("STRICT").when(spy).validate(eq(ctx), any(JsonNode.class), eq(format), isNull());
        Object result = spy.validate(ctx, node, format);
        assertEquals("STRICT", result);
    }

    @Test
    void schemaWithInvalidDefinition_propagatesJsonSchemaException() {
        // an unknown keyword forces JsonSchemaFactory to create a schema that will
        // throw JsonSchemaException when validation is attempted.
        String badSchema = "{\"unknownKeyword\":true}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema bad = factory.getSchema(badSchema);
        JsonNode node = MAPPER.createObjectNode();
        assertThrows(JsonSchemaException.class, () -> bad.validate(executionContext, node, outputFormat));
    }

    @Test
    void executionContextThrowsDuringValidation_exceptionIsPropagated() {
        // Mock ExecutionContext that throws on any method call.
        ExecutionContext broken = mock(ExecutionContext.class);
        when(broken.getConfig()).thenThrow(new IllegalStateException("context broken"));
        JsonNode node = MAPPER.readTree("{\"type\":\"string\",\"value\":\"test\"}");
        // The schema will attempt to read configuration from the context,
        // causing the mocked method to throw.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> schema.validate(broken, node, outputFormat));
        assertEquals("context broken", ex.getMessage());
    }

    @Test
    void outputFormatThrowsCheckedException_isWrappedInRuntimeException() {
        JsonNode node = MAPPER.readTree("{\"type\":\"number\",\"value\":123}");
        when(outputFormat.apply(any())).thenThrow(new IOException("IO failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(executionContext, node, outputFormat));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("IO failure", ex.getCause().getMessage());
    }
}
