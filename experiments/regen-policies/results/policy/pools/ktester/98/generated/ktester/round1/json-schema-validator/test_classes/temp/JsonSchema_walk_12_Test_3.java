package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

class JsonSchema_walk_12_Test {

    private static JsonSchemaFactory factory;

    private static ObjectMapper mapper;

    //     @Mock
    private OutputFormat<String> // mocked OutputFormat used in all tests
    outputFormatMock;

    private SimpleOutputFormat outputFormat;

    /**
     * Simple test double for {@link OutputFormat}.
     */
    private static class SimpleOutputFormat implements OutputFormat<Object> {

        // By default just return the set of validation messages.
        private Function<Set<ValidationMessage>, Object> function = msgs -> msgs;

        private int callCount = 0;

        void setFunction(Function<Set<ValidationMessage>, Object> f) {
            this.function = f;
        }

        void reset() {
            callCount = 0;
            function = msgs -> msgs;
        }

        int getCallCount() {
            return callCount;
        }

        @Override
        public Object format(JsonSchema schema, Set<ValidationMessage> messages, ExecutionContext executionContext, ValidationContext validationContext) {
            callCount++;
            return function.apply(messages);
        }
    }

    /**
     * Simple test double for {@link ExecutionContextCustomizer}.
     */
    private static class TestCustomizer implements ExecutionContextCustomizer {

        private int invokeCount = 0;

        @Override
        public void customize(ExecutionContext ctx, ValidationContext vc) {
            invokeCount++;
        }

        int getInvokeCount() {
            return invokeCount;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use draft‑07 – it is sufficient for the tests.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        outputFormat = new SimpleOutputFormat();
        outputFormat.reset();
    }

    /**
     * Verifies that {@code walk(..., validate = false, customizer = null)} returns the value
     * produced by the supplied {@link OutputFormat}.
     */
    @Test
    void walkWithoutValidationAndWithoutCustomizer_returnsOutputFormatResult() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        ExecutionContext executionContext = new ExecutionContext();
        outputFormat.setFunction(vr -> "formatted-result");
        Object result = schema.walk(executionContext, node, outputFormat, false, (ExecutionContextCustomizer) null);
        assertEquals("formatted-result", result);
        assertEquals(1, outputFormat.getCallCount(), "OutputFormat should be invoked exactly once");
    }

    /**
     * Verifies that {@code walk(..., validate = true, customizer = null)} also returns the value
     * from the {@link OutputFormat} when the schema validates successfully.
     */
    @Test
    void walkWithValidationAndWithoutCustomizer_successfulValidation_returnsOutputFormatResult() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}");
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        ExecutionContext executionContext = new ExecutionContext();
        outputFormat.setFunction(vr -> "ok");
        Object result = schema.walk(executionContext, node, outputFormat, true, (ExecutionContextCustomizer) null);
        assertEquals("ok", result);
        assertEquals(1, outputFormat.getCallCount());
    }

    /**
     * Verifies that a non‑null {@link ExecutionContextCustomizer} is accepted and does not interfere
     * with the normal flow of {@code walk}.
     */
    @Test
    void walkWithCustomizer_isInvokedAndReturnsOutputFormatResult() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"age\":30}");
        ExecutionContext executionContext = new ExecutionContext();
        TestCustomizer customizer = new TestCustomizer();
        outputFormat.setFunction(vr -> "customized");
        Object result = schema.walk(executionContext, node, outputFormat, false, (ExecutionContextCustomizer) customizer);
        assertEquals("customized", result);
        assertEquals(1, outputFormat.getCallCount());
        assertEquals(1, customizer.getInvokeCount(), "Customizer should be invoked exactly once");
    }

    /* --------------------------------------------------------------
     *  Test group: walk_validStringNode_validatesSuccessfully
     * -------------------------------------------------------------- */
    @Test
    void walk_validStringNode_validatesSuccessfully() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("\"hello\"");
        ExecutionContext executionContext = new ExecutionContext();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, (ExecutionContextCustomizer) null);
        assertTrue(result.isEmpty(), "When the node matches the schema, walk should return an empty set of ValidationMessage");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_invalidStringNode_validationFails
     * -------------------------------------------------------------- */
    @Test
    void walk_invalidStringNode_validationFails() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("123");
        ExecutionContext executionContext = new ExecutionContext();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, (ExecutionContextCustomizer) null);
        assertEquals(1, result.size(), "A type‑mismatch should produce a single ValidationMessage");
        ValidationMessage message = result.iterator().next();
        assertTrue(message.getMessage().toLowerCase().contains("string"), "The message should mention the expected type String");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_validateFlagFalse_skipsValidation
     * -------------------------------------------------------------- */
    @Test
    void walk_validateFlagFalse_skipsValidation() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("123");
        ExecutionContext executionContext = new ExecutionContext();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, false, (ExecutionContextCustomizer) null);
        assertTrue(result.isEmpty(), "When validation flag is false, walk should skip validation and return an empty set");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_withExecutionCustomizerAddsCustomMessage
     * -------------------------------------------------------------- */
    @Test
    void walk_withExecutionCustomizerAddsCustomMessage() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("\"test\"");
        ExecutionContext executionContext = new ExecutionContext();
        TestCustomizer customizer = new TestCustomizer();
        Set<ValidationMessage> customSet = new HashSet<>();
        ValidationMessage customMessage = ValidationMessage.builder().message("custom: added by ExecutionContextCustomizer").build();
        customSet.add(customMessage);
        outputFormat.setFunction(vr -> customSet);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, (ExecutionContextCustomizer) customizer);
        assertEquals(1, customizer.getInvokeCount(), "Customizer must have been called exactly once");
        assertEquals(1, result.size(), "Result set should contain exactly the custom message injected by the test setup");
        assertTrue(result.contains(customMessage), "Result set must contain the expected custom ValidationMessage");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_nullNode_throwsNullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walk_nullNode_throwsNullPointerException() {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext executionContext = new ExecutionContext();
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, null, outputFormat, true, (ExecutionContextCustomizer) null));
    }

    /* --------------------------------------------------------------
     *  Test group: walk_nullExecutionContext_throwsNullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walk_nullExecutionContext_throwsNullPointerException() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"valid\":\"yes\"}");
        assertThrows(NullPointerException.class, () -> schema.walk(null, node, outputFormat, true, (ExecutionContextCustomizer) null));
    }

    /* --------------------------------------------------------------
     *  Test group: walk_nullNode_returnsEmptySet
     * -------------------------------------------------------------- */
    @Test
    void walk_nullNode_returnsEmptySet() throws Exception {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext executionContext = new ExecutionContext();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, null, outputFormat, true, (ExecutionContextCustomizer) null);
        assertTrue(result.isEmpty(), "When the node is null, walk should return an empty set of ValidationMessage");
        assertEquals(1, outputFormat.getCallCount(), "OutputFormat should still be invoked once even for a null node");
    }
}
