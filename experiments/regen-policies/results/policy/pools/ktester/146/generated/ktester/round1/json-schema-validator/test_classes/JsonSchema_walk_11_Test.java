package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Consumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_11_Test {

    private static JsonSchemaFactory factory;

    private static JsonSchema schema;

    private ExecutionContext executionContext;

    private ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // a very small schema – an empty object schema is sufficient for the walk tests
        String simpleSchema = "{ \"type\": \"object\" }";
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        schema = factory.getSchema(simpleSchema);
    }

    @BeforeEach
    void setupBeforeEach() {
        // default execution context
        executionContext = new ExecutionContext();
        mapper = new ObjectMapper();
    }

    /**
     * Simple {@link OutputFormat} that returns a constant value.
     */
    private static <T> OutputFormat<T> constantOutput(T value) {
        return messages -> value;
    }

    /**
     * {@link OutputFormat} that returns the number of validation messages.
     */
    private static OutputFormat<Integer> countMessagesOutput() {
        return messages -> messages.size();
    }

    /**
     * {@link OutputFormat} that collects the supplied messages into a list and returns it.
     */
    private static OutputFormat<List<ValidationMessage>> collectingOutput() {
        return messages -> new ArrayList<>(messages);
    }

    /**
     * {@link OutputFormat} that throws a supplied exception when invoked.
     */
    private static <T> OutputFormat<T> throwingOutput(RuntimeException ex) {
        return messages -> {
            throw ex;
        };
    }

    /**
     * {@link Consumer} that sets a flag on a mutable boolean array.
     */
    private static Consumer<ExecutionContext> flagSettingConsumer(boolean[] flag) {
        return ctx -> flag[0] = true;
    }

    /**
     * {@link Consumer} that throws the given exception (wrapped if checked).
     */
    private static Consumer<ExecutionContext> throwingConsumer(Exception ex) {
        return ctx -> {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            // checked exception – wrap into RuntimeException as the Consumer API does not allow checked throws
            throw new RuntimeException(ex);
        };
    }

    /**
     * Data provider for the *Validate flag true – normal flow* case.
     */
    static List<Arguments> provideValidateTrueNormalFlow() {
        JsonNode node = new ObjectMapper().createObjectNode().put("name", "John");
        return Collections.singletonList(Arguments.of(node));
    }

    /**
     * Verify that {@code walk} returns the value produced by the supplied {@link OutputFormat}
     * and that the {@link Consumer} customiser is invoked exactly once.
     */
    @Test
    void walk_withConsumer_returnsOutputAndInvokesConsumer() throws Exception {
        // ----- given ---------------------------------------------------------
        JsonNode node = mapper.readTree("{\"foo\":\"bar\"}");
        // simple OutputFormat that returns a constant string
        OutputFormat<String> outputFormat = messages -> "formatted-output";
        // consumer that flips a flag so we can assert it was called
        final boolean[] consumerCalled = { false };
        Consumer<ExecutionContext> customizer = ctx -> consumerCalled[0] = true;
        // ----- when ----------------------------------------------------------
        String result = schema.walk(executionContext, node, outputFormat, false, customizer);
        // ----- then ----------------------------------------------------------
        assertEquals("formatted-output", result, "walk should return the value from OutputFormat");
        assertTrue(consumerCalled[0], "Consumer customizer must have been invoked");
    }

    /**
     * The same flow but with validation enabled.  The schema is an empty object schema,
     * so the node is valid and the walk should still return the OutputFormat result.
     */
    @Test
    void walk_withValidationEnabled_returnsOutput() throws Exception {
        // ----- given ---------------------------------------------------------
        JsonNode node = mapper.readTree("{\"a\":1}");
        // return number of messages
        OutputFormat<Integer> outputFormat = messages -> messages.size();
        Consumer<ExecutionContext> customizer = ctx -> {
            /* no‑op */
        };
        // ----- when ----------------------------------------------------------
        Integer result = schema.walk(executionContext, node, outputFormat, true, customizer);
        // ----- then ----------------------------------------------------------
        // The node conforms to the empty object schema, therefore no validation messages
        assertEquals(0, result.intValue(), "There should be no validation messages for a valid node");
    }

    @ParameterizedTest
    @MethodSource("provideValidateTrueNormalFlow")
    void validateFlagTrue_normalFlow(JsonNode node) {
        // schema that accepts any object – validation will succeed
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        OutputFormat<String> outputFormat = constantOutput("validation passed");
        ExecutionContext ctx = new ExecutionContext();
        String result = schema.walk(ctx, node, outputFormat, true, null);
        assertEquals("validation passed", result);
    }

    @Test
    void validateFlagFalse_skipsValidation() throws Exception {
        // an array node
        JsonNode node = mapper.readTree("[1,2,3]");
        JsonSchema schema = factory.getSchema("{\"type\":\"array\"}");
        OutputFormat<String> outputFormat = constantOutput("validation skipped");
        ExecutionContext ctx = new ExecutionContext();
        String result = schema.walk(ctx, node, outputFormat, false, null);
        assertEquals("validation skipped", result);
    }

    @Test
    void nonNullExecutionCustomizer_isInvokedBeforeValidation() throws Exception {
        JsonNode node = mapper.readTree("{\"age\":30}");
        // schema expects an integer property "age"
        JsonSchema schema = factory.getSchema("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}");
        // consumer adds a custom attribute to the execution context
        ExecutionContext ctx = new ExecutionContext();
        Consumer<ExecutionContext> customizer = executionContext -> executionContext.getExecutionConfig().put("customFlag", true);
        // output format reads that flag and returns a distinct string
        OutputFormat<String> outputFormat = messages -> {
            Object flag = ctx.getExecutionConfig().get("customFlag");
            return (flag != null && (boolean) flag) ? "customizer applied" : "customizer not applied";
        };
        String result = schema.walk(ctx, node, outputFormat, true, customizer);
        assertEquals("customizer applied", result);
    }

    @Test
    void nullNode_throwsIllegalArgumentException() {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> outputFormat = constantOutput("won't be used");
        assertThrows(IllegalArgumentException.class, () -> schema.walk(ctx, null, outputFormat, true, null), "Null node should result in IllegalArgumentException");
    }

    @Test
    void nullOutputFormat_throwsNullPointerException() throws Exception {
        JsonNode node = mapper.readTree("{\"id\":\"123\"}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = new ExecutionContext();
        assertThrows(NullPointerException.class, () -> schema.walk(ctx, node, null, true, null), "Null OutputFormat must cause NullPointerException");
    }

    @Test
    void nullExecutionContext_throwsNullPointerException() throws Exception {
        JsonNode node = mapper.readTree("{\"foo\":\"bar\"}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        OutputFormat<String> outputFormat = constantOutput("won't be used");
        assertThrows(NullPointerException.class, () -> schema.walk(null, node, outputFormat, true, null), "Null ExecutionContext must cause NullPointerException");
    }

    @Test
    void executionCustomizerThrows_propagatesRuntimeException() throws Exception {
        JsonNode node = mapper.readTree("{\"valid\":true}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> outputFormat = constantOutput("won't be used");
        Consumer<ExecutionContext> throwingCustomizer = throwingConsumer(new RuntimeException("customizer failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(ctx, node, outputFormat, true, throwingCustomizer));
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void outputFormatReturnsNull_resultIsNull() throws Exception {
        JsonNode node = mapper.readTree("{\"a\":1}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> nullReturningFormat = messages -> null;
        String result = schema.walk(ctx, node, nullReturningFormat, false, null);
        assertNull(result, "When OutputFormat returns null, walk should return null");
    }

    @Test
    void validateTrue_nodeFailsSchema_validationMessagesCollected() throws Exception {
        // schema: age must be integer
        JsonSchema schema = factory.getSchema("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}");
        JsonNode node = mapper.readTree("{\"age\":\"twenty\"}");
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<List<ValidationMessage>> collecting = collectingOutput();
        List<ValidationMessage> messages = schema.walk(ctx, node, collecting, true, null);
        assertFalse(messages.isEmpty(), "Validation messages should be present");
        boolean containsAgeError = messages.stream().anyMatch(m -> m.getPath().equals("$.age"));
        assertTrue(containsAgeError, "Expected an error message for property 'age'");
    }

    @Test
    void validateTrue_nestedObjectPasses_validationSuccess() throws Exception {
        // schema with nested object definition
        String schemaStr = """
            {
              "type":"object",
              "properties":{
                "person":{
                  "type":"object",
                  "properties":{
                    "name":{"type":"string"},
                    "age":{"type":"integer"}
                  },
                  "required":["name","age"]
                }
              },
              "required":["person"]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaStr);
        JsonNode node = mapper.readTree("{\"person\":{\"name\":\"Alice\",\"age\":30}}");
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> outputFormat = constantOutput("nested validation passed");
        String result = schema.walk(ctx, node, outputFormat, true, null);
        assertEquals("nested validation passed", result);
    }

    @Test
    void validateTrue_schemaWithoutValidators_treatedAsValid() throws Exception {
        // empty schema – no validators at all
        JsonSchema schema = factory.getSchema("{}");
        // empty object node
        JsonNode node = mapper.createObjectNode();
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> outputFormat = constantOutput("no validators – treated as valid");
        String result = schema.walk(ctx, node, outputFormat, true, null);
        assertEquals("no validators – treated as valid", result);
    }

    @Test
    void outputFormatThrowsRuntimeException_propagates() throws Exception {
        JsonNode node = mapper.readTree("{\"key\":\"value\"}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> throwing = throwingOutput(new RuntimeException("format failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(ctx, node, throwing, true, null));
        assertEquals("format failure", ex.getMessage());
    }

    @Test
    void outputFormatThrowsCheckedException_wrappedAsRuntimeException() throws Exception {
        JsonNode node = mapper.readTree("{\"key\":\"value\"}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = new ExecutionContext();
        // Simulate a checked exception by throwing IOException wrapped in RuntimeException
        OutputFormat<String> throwing = throwingOutput(new IOException("IO error"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(ctx, node, throwing, false, null));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("IO error", ex.getCause().getMessage());
    }

    @Test
    void executionCustomizerThrowsCheckedException_wrappedAsRuntimeException() throws Exception {
        JsonNode node = mapper.readTree("{\"key\":123}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> outputFormat = constantOutput("won't be used");
        Consumer<ExecutionContext> throwingCustomizer = throwingConsumer(new IOException("IO in customizer"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(ctx, node, outputFormat, true, throwingCustomizer));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("IO in customizer", ex.getCause().getMessage());
    }

    @Test
    void nodeIsMissingNode_throwsIllegalArgumentException() {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> outputFormat = constantOutput("won't be used");
        assertThrows(IllegalArgumentException.class, () -> schema.walk(ctx, MissingNode.getInstance(), outputFormat, true, null), "MissingNode should be rejected with IllegalArgumentException");
    }

    @Test
    void schemaRefWithUnsupportedEncoding_throwsIllegalArgumentException() {
        // The schema contains a $ref with an illegal percent‑encoding.
        String schemaWithBadRef = """
            {
              "$ref": "http://example.com/schema%ZZ"
            }
            """;
        // The factory will try to resolve the $ref during schema creation.
        // We expect an IllegalArgumentException to be thrown at walk time.
        JsonSchema schema = factory.getSchema(schemaWithBadRef);
        JsonNode node = mapper.createObjectNode().put("$ref", "http://example.com/schema%ZZ");
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> outputFormat = constantOutput("won't be used");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> schema.walk(ctx, node, outputFormat, true, null));
        assertTrue(ex.getMessage().toLowerCase().contains("unsupportedencoding") || ex.getCause() instanceof UnsupportedEncodingException, "Expected IllegalArgumentException caused by UnsupportedEncodingException");
    }
}
