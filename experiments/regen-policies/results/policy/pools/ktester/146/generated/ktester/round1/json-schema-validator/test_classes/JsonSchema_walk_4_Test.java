package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_4_Test {

    /**
     * Shared Jackson mapper used to build {@link JsonNode} instances.
     */
    private static ObjectMapper mapper;

    /**
     * The schema under test – an empty schema that always validates successfully.
     */
    private JsonSchema jsonSchema;

    /**
     * An {@link OutputFormat} that extracts the validation messages from the
     * {@link ValidationResult} and returns them as a {@link List} of strings.
     */
    private static class CollectingOutputFormat implements OutputFormat<List<String>> {

        @Override
        public List<String> format(ValidationResult validationResult) {
            Set<ValidationMessage> msgs = validationResult.getMessages();
            List<String> list = new ArrayList<>(msgs.size());
            for (ValidationMessage vm : msgs) {
                list.add(vm.getMessage());
            }
            return list;
        }
    }

    /**
     * An {@link OutputFormat} that always throws a {@link RuntimeException}
     * when its {@code format} method is invoked.
     */
    private static class ExceptionThrowingOutputFormat implements OutputFormat<Object> {

        @Override
        public Object format(ValidationResult validationResult) {
            throw new RuntimeException("deliberate exception from OutputFormat");
        }
    }

    /**
     * An {@link OutputFormat} that returns {@code true} if validation succeeded,
     * {@code false} otherwise.
     */
    private static class BooleanResultOutputFormat implements OutputFormat<Boolean> {

        @Override
        public Boolean format(ValidationResult validationResult) {
            return validationResult.getMessages().isEmpty();
        }
    }

    /**
     * An {@link OutputFormat} that throws a checked {@link IOException}. The
     * {@code walk} method is expected to wrap it into an unchecked exception.
     */
    private static class IOExceptionThrowingOutputFormat implements OutputFormat<Object> {

        @Override
        public Object format(ValidationResult validationResult) throws IOException {
            throw new IOException("checked exception from OutputFormat");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // An empty schema (“{}”) accepts any JSON value – perfect for a lightweight test.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        jsonSchema = factory.getSchema("{}");
    }

    /**
     * Helper that builds a {@link JsonSchema} from a raw JSON schema string.
     */
    private JsonSchema buildSchema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }

    /**
     * Verifies that {@code walk(JsonNode, OutputFormat, boolean)} delegates to the
     * provided {@link OutputFormat} and returns its result.
     *
     * @throws IOException if the test JSON cannot be parsed (should never happen)
     */
    @Test
    void walkShouldDelegateToOutputFormat() throws IOException {
        // GIVEN – a simple JSON node and a mocked OutputFormat
        JsonNode node = mapper.readTree("{\"key\":\"value\"}");
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        when(format.format(any())).thenReturn("formatted-result");
        // WHEN – invoking the method under test
        String result = jsonSchema.walk(node, format, false);
        // THEN – the result comes from the OutputFormat and the format method was called once
        assertEquals("formatted-result", result);
        verify(format, times(1)).format(any());
    }

    /**
     * When validation is requested (the {@code validate} flag is {@code true}),
     * the {@link OutputFormat} should receive a {@link ValidationResult} that
     * contains any validation messages. This test uses a schema that expects a
     * string but supplies a numeric value, causing a validation error.
     *
     * @throws IOException if the test JSON cannot be parsed
     */
    @Test
    void walkWithValidationShouldPassValidationResultToOutputFormat() throws IOException {
        // GIVEN – a schema that requires a string value
        String schemaJson = "{ \"type\": \"string\" }";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema stringSchema = factory.getSchema(schemaJson);
        // Node that violates the schema (number instead of string)
        JsonNode invalidNode = mapper.readTree("123");
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        when(format.format(any())).thenReturn("invalid");
        // WHEN – walk with validation enabled
        String result = stringSchema.walk(invalidNode, format, true);
        // THEN – the OutputFormat is called and its return value is propagated
        assertEquals("invalid", result);
        verify(format, times(1)).format(any());
    }

    @Test
    void successfulValidationValidateTrue() throws Exception {
        // schema expects an object with a string property "name"
        String schema = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } }, \"required\": [\"name\"] }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("{\"name\":\"John\"}");
        CollectingOutputFormat format = new CollectingOutputFormat();
        List<String> result = jsonSchema.walk(instance, format, true);
        assertTrue(result.isEmpty(), "No validation messages expected for a valid instance");
    }

    @Test
    void validationDisabledReturnsEmptyResult() throws Exception {
        // schema expects an integer, but we will feed a string – validation is disabled
        String schema = "{ \"type\": \"integer\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("\"not-an-integer\"");
        CollectingOutputFormat format = new CollectingOutputFormat();
        List<String> result = jsonSchema.walk(instance, format, false);
        assertTrue(result.isEmpty(), "When validation is disabled, no messages must be produced");
    }

    @Test
    void invalidInstanceProducesValidationMessage() throws Exception {
        String schema = "{ \"type\": \"integer\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("\"not-an-integer\"");
        CollectingOutputFormat format = new CollectingOutputFormat();
        List<String> result = jsonSchema.walk(instance, format, true);
        assertFalse(result.isEmpty(), "A type‑mismatch should yield at least one validation message");
        assertTrue(result.stream().anyMatch(msg -> msg.toLowerCase().contains("type")), "Message should mention a type error");
    }

    @Test
    void nullNodeThrowsException() {
        CollectingOutputFormat format = new CollectingOutputFormat();
        assertThrows(NullPointerException.class, () -> jsonSchema.walk(null, format, true), "Passing a null JsonNode should result in NPE");
    }

    @Test
    void nullOutputFormatThrowsException() throws Exception {
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        assertThrows(NullPointerException.class, () -> jsonSchema.walk(node, null, true), "Passing a null OutputFormat should result in NPE");
    }

    @Test
    void missingRequiredPropertyProducesMessage() throws Exception {
        String schema = "{ \"type\": \"object\", \"required\": [\"id\"], \"properties\": { \"id\": { \"type\": \"integer\" } } }";
        jsonSchema = buildSchema(schema);
        // empty object
        JsonNode instance = mapper.readTree("{}");
        CollectingOutputFormat format = new CollectingOutputFormat();
        List<String> result = jsonSchema.walk(instance, format, true);
        assertFalse(result.isEmpty(), "Missing required property must produce a validation message");
        assertTrue(result.stream().anyMatch(m -> m.contains("required")), "Message should mention 'required'");
    }

    @Test
    void enumConstraintViolationProducesMessage() throws Exception {
        String schema = "{ \"type\": \"string\", \"enum\": [\"red\", \"green\", \"blue\"] }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("\"yellow\"");
        CollectingOutputFormat format = new CollectingOutputFormat();
        List<String> result = jsonSchema.walk(instance, format, true);
        assertFalse(result.isEmpty(), "Enum violation must be reported");
        assertTrue(result.stream().anyMatch(m -> m.toLowerCase().contains("enum")), "Message should reference enum");
    }

    @Test
    void patternConstraintViolationProducesMessage() throws Exception {
        String schema = "{ \"type\": \"string\", \"pattern\": \"^[A-Z]{3}[0-9]{4}$\" }";
        jsonSchema = buildSchema(schema);
        // lower‑case, does not match
        JsonNode instance = mapper.readTree("\"abc1234\"");
        CollectingOutputFormat format = new CollectingOutputFormat();
        List<String> result = jsonSchema.walk(instance, format, true);
        assertFalse(result.isEmpty(), "Pattern mismatch must be reported");
        assertTrue(result.stream().anyMatch(m -> m.toLowerCase().contains("pattern")), "Message should reference pattern");
    }

    @Test
    void arrayItemsTypeMismatchProducesMessage() throws Exception {
        String schema = "{ \"type\": \"array\", \"items\": { \"type\": \"integer\" } }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("[1, \"two\", 3]");
        CollectingOutputFormat format = new CollectingOutputFormat();
        List<String> result = jsonSchema.walk(instance, format, true);
        assertFalse(result.isEmpty(), "Array element type mismatch must be reported");
        assertTrue(result.stream().anyMatch(m -> m.toLowerCase().contains("type")), "Message should mention type error");
    }

    @Test
    void oneOfMatchingSubschemaProducesNoMessages() throws Exception {
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("\"a string\"");
        CollectingOutputFormat format = new CollectingOutputFormat();
        List<String> result = jsonSchema.walk(instance, format, true);
        assertTrue(result.isEmpty(), "When oneOf matches, no validation messages are expected");
    }

    @Test
    void oneOfNoMatchingSubschemaProducesMessage() throws Exception {
        String schema = "{ \"oneOf\": [ { \"type\": \"boolean\" }, { \"type\": \"null\" } ] }";
        jsonSchema = buildSchema(schema);
        // number
        JsonNode instance = mapper.readTree("123");
        CollectingOutputFormat format = new CollectingOutputFormat();
        List<String> result = jsonSchema.walk(instance, format, true);
        assertFalse(result.isEmpty(), "When none of the oneOf alternatives match, a message must be produced");
        assertTrue(result.stream().anyMatch(m -> m.toLowerCase().contains("oneof")), "Message should reference oneOf");
    }

    @Test
    void outputFormatRuntimeExceptionIsPropagated() throws Exception {
        String schema = "{ \"type\": \"integer\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("42");
        OutputFormat<Object> format = new ExceptionThrowingOutputFormat();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.walk(instance, format, true), "RuntimeException from OutputFormat must be propagated");
        assertEquals("deliberate exception from OutputFormat", ex.getMessage());
    }

    @Test
    void outputFormatCheckedIOExceptionIsWrapped() throws Exception {
        String schema = "{ \"type\": \"integer\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("42");
        OutputFormat<Object> format = new IOExceptionThrowingOutputFormat();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.walk(instance, format, true), "Checked IOException should be wrapped in a RuntimeException");
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("checked exception from OutputFormat", ex.getCause().getMessage());
    }

    @Test
    void schemaAllowsNullNodeProducesNoMessages() throws Exception {
        String schema = "{ \"type\": [\"null\", \"string\"] }";
        jsonSchema = buildSchema(schema);
        // literal null
        JsonNode instance = mapper.readTree("null");
        CollectingOutputFormat format = new CollectingOutputFormat();
        List<String> result = jsonSchema.walk(instance, format, true);
        assertTrue(result.isEmpty(), "Null value is allowed – no validation messages expected");
    }

    @Test
    void booleanResultOutputFormatReturnsTrueOnSuccess() throws Exception {
        String schema = "{ \"type\": \"boolean\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("true");
        BooleanResultOutputFormat format = new BooleanResultOutputFormat();
        Boolean result = jsonSchema.walk(instance, format, true);
        assertTrue(result, "OutputFormat should return true when validation succeeds");
    }

    @Test
    void unresolvedRefThrowsException() {
        String schema = "{ \"$ref\": \"#/nonexistent\" }";
        jsonSchema = buildSchema(schema);
        // any node
        JsonNode instance = mapper.createObjectNode();
        CollectingOutputFormat format = new CollectingOutputFormat();
        // The library throws a JsonSchemaException (runtime) when $ref cannot be resolved.
        assertThrows(RuntimeException.class, () -> jsonSchema.walk(instance, format, true), "Unresolvable $ref must result in an exception");
    }

    @Test
    void recursiveAnchorDoesNotOverflowStack() throws Exception {
        // Draft‑2020‑12 supports "$recursiveAnchor": true
        String schema = "{ \"$id\": \"#\", \"$recursiveAnchor\": true, \"type\": \"object\", \"properties\": { \"self\": { \"$ref\": \"#\" } } }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("{\"self\":{}}");
        CollectingOutputFormat format = new CollectingOutputFormat();
        // The walk should finish and return (no stack overflow). Validation passes.
        List<String> result = jsonSchema.walk(instance, format, true);
        assertTrue(result.isEmpty(), "Recursive anchor should be handled without error");
    }

    @Test
    void missingNodeArgumentThrowsException() {
        JsonNode missing = MissingNode.getInstance();
        CollectingOutputFormat format = new CollectingOutputFormat();
        assertThrows(IllegalArgumentException.class, () -> jsonSchema.walk(missing, format, true), "MissingNode should be rejected with IllegalArgumentException");
    }

    @Test
    void executionContextCreationFailurePropagatesException() throws Exception {
        // Create a schema instance but force createExecutionContext() to throw.
        // We achieve this by creating a subclass that overrides the method.
        JsonSchema broken = new JsonSchema() {

            @Override
            public ExecutionContext createExecutionContext() {
                throw new IllegalStateException("forced failure");
            }
        };
        // Use a trivial schema that would otherwise be valid.
        JsonNode dummyNode = mapper.readTree("{}");
        CollectingOutputFormat format = new CollectingOutputFormat();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> broken.walk(dummyNode, format, true), "Failure in createExecutionContext must be propagated");
        assertEquals("forced failure", ex.getMessage());
    }

    @Test
    void invalidIdUriThrowsUnsupportedEncodingException() {
        // The schema contains an invalid percent‑encoding sequence.
        String schema = "{ \"$id\": \"http://example.com/schema%ZZ\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.createObjectNode();
        CollectingOutputFormat format = new CollectingOutputFormat();
        // The library wraps the UnsupportedEncodingException into a RuntimeException.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.walk(instance, format, true), "Invalid $id should result in an exception");
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException);
    }

    @Test
    void validateFalseWithInvalidInstanceProducesNoMessages() throws Exception {
        String schema = "{ \"type\": \"integer\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("\"not-an-integer\"");
        CollectingOutputFormat format = new CollectingOutputFormat();
        List<String> result = jsonSchema.walk(instance, format, false);
        assertTrue(result.isEmpty(), "When validation is disabled, even an invalid instance yields no messages");
    }

    @Test
    void schemaWithRecursiveAnchorHandledGracefully() throws Exception {
        // Same as test 17 but focusing on the isRecursiveAnchor() flag.
        String schema = "{ \"$recursiveAnchor\": true, \"type\": \"object\", \"properties\": { \"self\": { \"$ref\": \"#\" } } }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("{\"self\":{}}");
        CollectingOutputFormat format = new CollectingOutputFormat();
        List<String> result = jsonSchema.walk(instance, format, true);
        assertTrue(result.isEmpty(), "Recursive anchor should not cause infinite recursion");
    }

    @Test
    void customBooleanOutputFormatWorks() throws Exception {
        String schema = "{ \"type\": \"boolean\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("false");
        BooleanResultOutputFormat format = new BooleanResultOutputFormat();
        Boolean result = jsonSchema.walk(instance, format, true);
        assertFalse(result, "Boolean result should reflect validation outcome (false when messages exist)");
    }

    @Test
    @Disabled("Running this test would cause a StackOverflowError and halt the test suite. " + "It is kept as documentation of the expected behaviour.")
    void circularRefWithoutRecursiveAnchorCausesStackOverflow() {
        String schema = "{ \"$ref\": \"#/definitions/self\", \"definitions\": { \"self\": { \"$ref\": \"#/definitions/self\" } } }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.createObjectNode();
        CollectingOutputFormat format = new CollectingOutputFormat();
        assertThrows(StackOverflowError.class, () -> jsonSchema.walk(instance, format, true), "Circular $ref without recursive anchor should overflow the stack");
    }
}
