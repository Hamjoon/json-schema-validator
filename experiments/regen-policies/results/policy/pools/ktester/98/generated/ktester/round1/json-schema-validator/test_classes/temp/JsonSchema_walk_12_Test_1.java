package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_12_Test {

    private static JsonSchemaFactory factory;

    private static ObjectMapper mapper;

    @Mock
    private OutputFormat<String> // mocked OutputFormat used in all tests
    outputFormatMock;

    private SimpleOutputFormat outputFormat;

    /**
     * Simple test double for {@link OutputFormat}.
     */
    private static class SimpleOutputFormat implements OutputFormat<Object> {

        // default
        private Function<ValidationResult, Object> function = vr -> vr.getMessages();

        private int callCount = 0;

        void setFunction(Function<ValidationResult, Object> f) {
            this.function = f;
        }

        void reset() {
            callCount = 0;
            function = vr -> vr.getMessages();
        }

        int getCallCount() {
            return callCount;
        }

        @Override
        public Object apply(ValidationResult validationResult) {
            callCount++;
            return function.apply(validationResult);
        }
    }

    /**
     * Simple test double for {@link ExecutionContextCustomizer}.
     */
    private static class TestCustomizer implements ExecutionContextCustomizer {

        private int invokeCount = 0;

        @Override
        public void customize(ExecutionContext ctx) {
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
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        ExecutionContext executionContext = new ExecutionContext();
        outputFormat.setFunction(vr -> "formatted-result");
        // WHEN
        Object result = schema.walk(executionContext, node, outputFormat, false, null);
        // THEN
        assertEquals("formatted-result", result);
        assertEquals(1, outputFormat.getCallCount(), "OutputFormat should be invoked exactly once");
    }

    /**
     * Verifies that {@code walk(..., validate = true, customizer = null)} also returns the value
     * from the {@link OutputFormat} when the schema validates successfully.
     */
    @Test
    void walkWithValidationAndWithoutCustomizer_successfulValidation_returnsOutputFormatResult() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}");
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        ExecutionContext executionContext = new ExecutionContext();
        outputFormat.setFunction(vr -> "ok");
        // WHEN
        Object result = schema.walk(executionContext, node, outputFormat, true, null);
        // THEN
        assertEquals("ok", result);
        assertEquals(1, outputFormat.getCallCount());
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
        TestCustomizer customizer = new TestCustomizer();
        outputFormat.setFunction(vr -> "customized");
        // WHEN
        Object result = schema.walk(executionContext, node, outputFormat, false, customizer);
        // THEN
        assertEquals("customized", result);
        assertEquals(1, outputFormat.getCallCount());
        assertEquals(1, customizer.getInvokeCount(), "Customizer should be invoked exactly once");
    }

    /* --------------------------------------------------------------
     *  Test group: walk_validStringNode_validatesSuccessfully
     * -------------------------------------------------------------- */
    @Test
    void walk_validStringNode_validatesSuccessfully() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("\"hello\"");
        ExecutionContext executionContext = new ExecutionContext();
        // WHEN
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, null);
        // THEN
        assertTrue(result.isEmpty(), "When the node matches the schema, walk should return an empty set of ValidationMessage");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_invalidStringNode_validationFails
     * -------------------------------------------------------------- */
    @Test
    void walk_invalidStringNode_validationFails() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("123");
        ExecutionContext executionContext = new ExecutionContext();
        // WHEN
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, null);
        // THEN
        assertEquals(1, result.size(), "A type‑mismatch should produce a single ValidationMessage");
        ValidationMessage message = result.iterator().next();
        assertTrue(message.getMessage().contains("expected type: String"), "The message should mention the expected type String");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_validateFlagFalse_skipsValidation
     * -------------------------------------------------------------- */
    @Test
    void walk_validateFlagFalse_skipsValidation() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("123");
        ExecutionContext executionContext = new ExecutionContext();
        // WHEN
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, false, null);
        // THEN
        assertTrue(result.isEmpty(), "When validation flag is false, walk should skip validation and return an empty set");
        assertEquals(1, outputFormat.getCallCount());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_withExecutionCustomizerAddsCustomMessage
     * -------------------------------------------------------------- */
    @Test
    void walk_withExecutionCustomizerAddsCustomMessage() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("\"test\"");
        ExecutionContext executionContext = new ExecutionContext();
        TestCustomizer customizer = new TestCustomizer();
        // The OutputFormat will return a synthetic set containing a custom ValidationMessage.
        Set<ValidationMessage> customSet = new HashSet<>();
        ValidationMessage customMessage = ValidationMessage.of("custom: added by ExecutionContextCustomizer");
        customSet.add(customMessage);
        outputFormat.setFunction(vr -> customSet);
        // WHEN
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) schema.walk(executionContext, node, outputFormat, true, customizer);
        // THEN
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
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext executionContext = new ExecutionContext();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, null, outputFormat, true, null));
    }

    /* --------------------------------------------------------------
     *  Test group: walk_nullExecutionContext_throwsNullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walk_nullExecutionContext_throwsNullPointerException() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"valid\":\"yes\"}");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> schema.walk(null, node, outputFormat, true, null));
    }
}
