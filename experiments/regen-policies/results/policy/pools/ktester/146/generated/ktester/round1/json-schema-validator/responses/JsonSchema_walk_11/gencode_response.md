```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Extended unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, OutputFormat, boolean, Consumer)}.
 *
 * <p>
 * The test suite covers the behaviour described in the JSON test‑case specification:
 * <ul>
 *   <li>validation flag true/false,</li>
 *   <li>null arguments,</li>
 *   <li>customiser invocation,</li>
 *   <li>exception propagation from the consumer or the {@link OutputFormat},</li>
 *   <li>handling of special node types (MissingNode),</li>
 *   <li>schema‑specific scenarios (type mismatch, nested objects, $ref decoding errors).</li>
 * </ul>
 * </p>
 *
 * <p>
 * Where a real {@link JsonSchema} instance is required, it is created via {@link JsonSchemaFactory}
 * with a minimal schema that matches the test scenario.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_11_Test {

    private static JsonSchemaFactory factory;
    private ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // factory is reused for all tests – the version flag is the latest stable one.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        mapper = new ObjectMapper();
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods to build reusable mocks / output formats.
     * --------------------------------------------------------------------- */

    /** Simple {@link OutputFormat} that returns a constant value. */
    private static <T> OutputFormat<T> constantOutput(T value) {
        return messages -> value;
    }

    /** {@link OutputFormat} that returns the number of validation messages. */
    private static OutputFormat<Integer> countMessagesOutput() {
        return messages -> messages.size();
    }

    /** {@link OutputFormat} that collects the supplied messages into a list and returns it. */
    private static OutputFormat<List<ValidationMessage>> collectingOutput() {
        return messages -> new ArrayList<>(messages);
    }

    /** {@link OutputFormat} that throws a supplied exception when invoked. */
    private static <T> OutputFormat<T> throwingOutput(RuntimeException ex) {
        return messages -> {
            throw ex;
        };
    }

    /** {@link Consumer} that sets a flag on a mutable boolean array. */
    private static Consumer<ExecutionContext> flagSettingConsumer(boolean[] flag) {
        return ctx -> flag[0] = true;
    }

    /** {@link Consumer} that throws the given exception (wrapped if checked). */
    private static Consumer<ExecutionContext> throwingConsumer(Exception ex) {
        return ctx -> {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            // checked exception – wrap into RuntimeException as the Consumer API does not allow checked throws
            throw new RuntimeException(ex);
        };
    }

    /* --------------------------------------------------------------------- *
     *  Existing simple tests (kept for completeness)
     * --------------------------------------------------------------------- */

    @Test
    void walk_withConsumer_returnsOutputAndInvokesConsumer() throws Exception {
        JsonNode node = mapper.readTree("{\"foo\":\"bar\"}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");

        OutputFormat<String> outputFormat = constantOutput("formatted-output");
        final boolean[] consumerCalled = {false};
        Consumer<ExecutionContext> customizer = flagSettingConsumer(consumerCalled);

        ExecutionContext ctx = new ExecutionContext();

        String result = schema.walk(ctx, node, outputFormat, false, customizer);
        assertEquals("formatted-output", result);
        assertTrue(consumerCalled[0], "Consumer must have been invoked");
    }

    @Test
    void walk_withValidationEnabled_returnsOutput() throws Exception {
        JsonNode node = mapper.readTree("{\"a\":1}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");

        OutputFormat<Integer> outputFormat = countMessagesOutput();
        ExecutionContext ctx = new ExecutionContext();

        Integer result = schema.walk(ctx, node, outputFormat, true, ctx -> {
        });
        assertEquals(0, result.intValue(), "No validation messages expected for a valid node");
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised tests for the remaining groups.
     * --------------------------------------------------------------------- */

    /** Data provider for the *Validate flag true – normal flow* case. */
    static List<Arguments> provideValidateTrueNormalFlow() {
        JsonNode node = new ObjectMapper().createObjectNode().put("name", "John");
        return Collections.singletonList(Arguments.of(node));
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
        JsonNode node = mapper.readTree("[1,2,3]"); // an array node
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

        assertThrows(IllegalArgumentException.class,
                () -> schema.walk(ctx, null, outputFormat, true, null),
                "Null node should result in IllegalArgumentException");
    }

    @Test
    void nullOutputFormat_throwsNullPointerException() throws Exception {
        JsonNode node = mapper.readTree("{\"id\":\"123\"}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = new ExecutionContext();

        assertThrows(NullPointerException.class,
                () -> schema.walk(ctx, node, null, true, null),
                "Null OutputFormat must cause NullPointerException");
    }

    @Test
    void nullExecutionContext_throwsNullPointerException() throws Exception {
        JsonNode node = mapper.readTree("{\"foo\":\"bar\"}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        OutputFormat<String> outputFormat = constantOutput("won't be used");

        assertThrows(NullPointerException.class,
                () -> schema.walk(null, node, outputFormat, true, null),
                "Null ExecutionContext must cause NullPointerException");
    }

    @Test
    void executionCustomizerThrows_propagatesRuntimeException() throws Exception {
        JsonNode node = mapper.readTree("{\"valid\":true}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> outputFormat = constantOutput("won't be used");
        Consumer<ExecutionContext> throwingCustomizer = throwingConsumer(new RuntimeException("customizer failure"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(ctx, node, outputFormat, true, throwingCustomizer));
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
        boolean containsAgeError = messages.stream()
                .anyMatch(m -> m.getPath().equals("$.age"));
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
        JsonNode node = mapper.createObjectNode(); // empty object node
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
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(ctx, node, throwing, true, null));
        assertEquals("format failure", ex.getMessage());
    }

    @Test
    void outputFormatThrowsCheckedException_wrappedAsRuntimeException() throws Exception {
        JsonNode node = mapper.readTree("{\"key\":\"value\"}");
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = new ExecutionContext();

        // Simulate a checked exception by throwing IOException wrapped in RuntimeException
        OutputFormat<String> throwing = throwingOutput(new IOException("IO error"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(ctx, node, throwing, false, null));
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
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(ctx, node, outputFormat, true, throwingCustomizer));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("IO in customizer", ex.getCause().getMessage());
    }

    @Test
    void nodeIsMissingNode_throwsIllegalArgumentException() {
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> outputFormat = constantOutput("won't be used");

        assertThrows(IllegalArgumentException.class,
                () -> schema.walk(ctx, MissingNode.getInstance(), outputFormat, true, null),
                "MissingNode should be rejected with IllegalArgumentException");
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

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> schema.walk(ctx, node, outputFormat, true, null));
        assertTrue(ex.getMessage().toLowerCase().contains("unsupportedencoding") ||
                   ex.getCause() instanceof UnsupportedEncodingException,
                "Expected IllegalArgumentException caused by UnsupportedEncodingException");
    }
}
```