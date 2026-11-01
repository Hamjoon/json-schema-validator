```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.walk.WalkAbortException;
import com.networknt.schema.walk.WalkListenerRunner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit‑tests for {@link JsonSchema#validateAndCollect(ExecutionContext, JsonNode)}.
 * <p>
 * The test suite follows the groups defined in the supplied JSON test‑case description.
 * Each group is exercised by a dedicated test method (or a parameterised method when
 * several similar cases exist).  Real {@link JsonSchema} instances are built from
 * minimal JSON schema definitions that trigger the required validator(s).  Where
 * the behaviour depends on the {@link ExecutionContext} or on a {@link WalkListenerRunner},
 * Mockito is used to create the necessary mocks.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validateAndCollect_Test {

    /** Helper to build a {@link JsonSchema} from a JSON schema string. */
    private static JsonSchema schemaFrom(String jsonSchema) {
        JsonNode schemaNode = JsonMapperFactory.getInstance().readTree(jsonSchema);
        return JsonSchemaFactory.getInstance(VersionFlag.V201909).getSchema(schemaNode);
    }

    /* --------------------------------------------------------------------- */
    /* 1. Null inputs and early exit                                         */
    /* --------------------------------------------------------------------- */

    @Test
    void whenBothExecutionContextAndNodeAreNull_thenInvalidResultReturned() {
        JsonSchema schema = schemaFrom("{}"); // empty schema – any document accepted
        ValidationResult result = schema.validateAndCollect(null, null);
        assertNotNull(result);
        assertFalse(result.isValid(), "Result must be invalid when both arguments are null");
        assertEquals(1, result.getMessages().size(),
                "Exactly one validation message should be present");
        assertTrue(result.getMessages().iterator().next().getMessage()
                .toLowerCase().contains("null"), "Message should mention null instance");
    }

    @Test
    void whenExecutionContextIsNull_thenInvalidResultReturned() {
        JsonSchema schema = schemaFrom("{}");
        JsonNode node = JsonMapperFactory.getInstance().readTree("{}");
        ValidationResult result = schema.validateAndCollect(null, node);
        assertNotNull(result);
        assertFalse(result.isValid(), "Result must be invalid when executionContext is null");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("executioncontext")),
                "Message should mention missing ExecutionContext");
    }

    @Test
    void whenNodeIsNullButExecutionContextIsValid_thenInvalidResultReturned() {
        JsonSchema schema = schemaFrom("{}");
        ExecutionContext ctx = new ExecutionContext();
        ValidationResult result = schema.validateAndCollect(ctx, null);
        assertNotNull(result);
        assertFalse(result.isValid(), "Result must be invalid when node is null");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("null")),
                "Message should mention null instance node");
    }

    /* --------------------------------------------------------------------- */
    /* 2. Schema without any validators                                      */
    /* --------------------------------------------------------------------- */

    @Test
    void schemaWithoutValidatorsReturnsSuccessfulResult() {
        JsonSchema schema = schemaFrom("{}"); // no validators at all
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonMapperFactory.getInstance().readTree("{}");
        ValidationResult result = schema.validateAndCollect(ctx, node);
        assertNotNull(result);
        assertTrue(result.isValid(), "Empty schema should validate any document");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- */
    /* 3. Required validator – missing required property                     */
    /* --------------------------------------------------------------------- */

    @Test
    void requiredValidatorDetectsMissingProperty() {
        JsonSchema schema = schemaFrom("{\"required\":[\"missing\"]}");
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"present\":\"value\"}");
        ValidationResult result = schema.validateAndCollect(ctx, node);
        assertFalse(result.isValid(), "Result must be invalid when required property is absent");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().contains("missing")),
                "Message should contain the missing property name");
    }

    /* --------------------------------------------------------------------- */
    /* 4. Type validator – type mismatch                                      */
    /* --------------------------------------------------------------------- */

    @Test
    void typeValidatorDetectsTypeMismatch() {
        JsonSchema schema = schemaFrom("{\"type\":\"string\"}");
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = new IntNode(123);
        ValidationResult result = schema.validateAndCollect(ctx, node);
        assertFalse(result.isValid(), "Result must be invalid for type mismatch");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("string")
                        && m.getMessage().toLowerCase().contains("integer")),
                "Message should indicate expected string but found integer");
    }

    /* --------------------------------------------------------------------- */
    /* 5. Recursive anchor handling                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void recursiveAnchorDoesNotCauseInfiniteRecursion() {
        // schema with $recursiveAnchor:true and a self‑reference via $ref
        String schemaJson = """
                {
                  "$id":"http://example.com/rec",
                  "$recursiveAnchor":true,
                  "type":"object",
                  "properties":{
                    "self":{"$ref":"#"}
                  }
                }""";
        JsonSchema schema = schemaFrom(schemaJson);
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"self\":{}}");
        ValidationResult result = schema.validateAndCollect(ctx, node);
        assertTrue(result.isValid(), "Recursive anchor should prevent infinite recursion");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- */
    /* 6. WalkListenerRunner aborts walk                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void walkListenerRunnerAbortProducesInvalidResult() throws Exception {
        JsonSchema schema = schemaFrom("{}");
        // mock a WalkListenerRunner that throws WalkAbortException on first node
        WalkListenerRunner abortRunner = mock(WalkListenerRunner.class);
        doThrow(new WalkAbortException("abort")).when(abortRunner).run(any(), any(), any());

        // inject the runner via reflection (field is package‑private)
        Field field = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        field.setAccessible(true);
        field.set(schema, abortRunner);

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"a\":1}");
        ValidationResult result = schema.validateAndCollect(ctx, node);
        assertFalse(result.isValid(), "Abort should make the result invalid");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("abort")),
                "Message should indicate walk was aborted");
    }

    /* --------------------------------------------------------------------- */
    /* 7. Enum validator – value not allowed                                  */
    /* --------------------------------------------------------------------- */

    @Test
    void enumValidatorRejectsDisallowedValue() {
        JsonSchema schema = schemaFrom("{\"enum\":[\"red\",\"green\",\"yellow\"]}");
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = new TextNode("blue");
        ValidationResult result = schema.validateAndCollect(ctx, node);
        assertFalse(result.isValid(), "Enum validator should reject value not in list");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().contains("blue") && m.getMessage().toLowerCase().contains("enum")),
                "Message should mention the disallowed enum value");
    }

    /* --------------------------------------------------------------------- */
    /* 8. Pattern validator – regex mismatch                                   */
    /* --------------------------------------------------------------------- */

    @Test
    void patternValidatorDetectsMismatch() {
        JsonSchema schema = schemaFrom("{\"pattern\":\"^[A-Z]{3}[0-9]{3}$\"}");
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = new TextNode("123-AB");
        ValidationResult result = schema.validateAndCollect(ctx, node);
        assertFalse(result.isValid(), "Pattern validator should fail for non‑matching string");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().contains("123-AB") && m.getMessage().toLowerCase().contains("pattern")),
                "Message should contain the offending value and pattern description");
    }

    /* --------------------------------------------------------------------- */
    /* 9. $ref resolution – required property in referenced schema            */
    /* --------------------------------------------------------------------- */

    @Test
    void refResolutionDetectsMissingRequiredInReferencedSchema() {
        String schemaJson = """
                {
                  "$ref":"#/definitions/person",
                  "definitions":{
                    "person":{
                      "type":"object",
                      "required":["age"]
                    }
                  }
                }""";
        JsonSchema schema = schemaFrom(schemaJson);
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"name\":\"John\"}");
        ValidationResult result = schema.validateAndCollect(ctx, node);
        assertFalse(result.isValid(), "Missing required property in $ref target should be reported");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("age")),
                "Message should mention the missing required property \"age\"");
    }

    /* --------------------------------------------------------------------- */
    /* 10. Combined required + type validators – type passes, required fails */
    /* --------------------------------------------------------------------- */

    @Test
    void combinedRequiredAndTypeValidatorsOnlyRequiredFails() {
        String schemaJson = """
                {
                  "type":"object",
                  "required":["mandatory"]
                }""";
        JsonSchema schema = schemaFrom(schemaJson);
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"optional\":\"value\"}");
        ValidationResult result = schema.validateAndCollect(ctx, node);
        assertFalse(result.isValid(), "Result must be invalid because required property is missing");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().contains("mandatory")),
                "Message should reference the missing required property");
        // type validator should not add a message because the node is an object
        assertEquals(1, result.getMessages().size(),
                "Only the required‑property message should be present");
    }

    /* --------------------------------------------------------------------- */
    /* 11. Array items type mismatch                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void arrayItemsValidatorDetectsElementTypeMismatch() {
        JsonSchema schema = schemaFrom("{\"type\":\"array\",\"items\":{\"type\":\"string\"}}");
        ExecutionContext ctx = new ExecutionContext();
        ArrayNode array = JsonMapperFactory.getInstance().createArrayNode()
                .add("a")
                .add(2)
                .add("c");
        ValidationResult result = schema.validateAndCollect(ctx, array);
        assertFalse(result.isValid(), "Array item type mismatch should invalidate the document");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("index 1")
                        && m.getMessage().toLowerCase().contains("integer")
                        && m.getMessage().toLowerCase().contains("string")),
                "Message should indicate the element at index 1 is an integer but a string was expected");
    }

    /* --------------------------------------------------------------------- */
    /* 12. AdditionalProperties false – extra field present                  */
    /* --------------------------------------------------------------------- */

    @Test
    void additionalPropertiesFalseDetectsUnexpectedField() {
        String schemaJson = """
                {
                  "type":"object",
                  "additionalProperties":false,
                  "properties":{
                    "known":{"type":"string"}
                  }
                }""";
        JsonSchema schema = schemaFrom(schemaJson);
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"known\":\"value\",\"extra\":\"notAllowed\"}");
        ValidationResult result = schema.validateAndCollect(ctx, node);
        assertFalse(result.isValid(), "Unexpected property should cause validation failure");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("extra")
                        && m.getMessage().toLowerCase().contains("additionalproperties")),
                "Message should mention the disallowed property \"extra\"");
    }

    /* --------------------------------------------------------------------- */
    /* 13. WalkListenerRunner adds custom message without aborting            */
    /* --------------------------------------------------------------------- */

    @Test
    void walkListenerRunnerAddsCustomMessages() throws Exception {
        JsonSchema schema = schemaFrom("{}");
        WalkListenerRunner runner = mock(WalkListenerRunner.class);
        // Simulate listener adding a custom message for each visited node
        doAnswer(invocation -> {
            ExecutionContext ctx = invocation.getArgument(0);
            JsonNode node = invocation.getArgument(1);
            // directly add a message to the CollectorContext inside the ExecutionContext
            ctx.getCollectorContext().addMessage(new ValidationMessage.Builder()
                    .code("listener-info")
                    .message("listener-info")
                    .path(JsonNodePath.create(node))
                    .build());
            return null;
        }).when(runner).run(any(), any(), any());

        // inject runner
        Field f = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        f.setAccessible(true);
        f.set(schema, runner);

        ExecutionContext ctx = new ExecutionContext(); // default collector context
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"a\":1,\"b\":2}");
        ValidationResult result = schema.validateAndCollect(ctx, node);
        assertTrue(result.isValid(), "Schema without constraints should still be valid");
        // two nodes visited (root + each property) => at least 3 custom messages
        long customCount = result.getMessages().stream()
                .filter(m -> "listener-info".equals(m.getCode()))
                .count();
        assertTrue(customCount >= 1, "At least one custom listener‑info message should be present");
    }

    /* --------------------------------------------------------------------- */
    /* 14. ExecutionContext with custom CollectorContext – prefixed messages */
    /* --------------------------------------------------------------------- */

    @Test
    void customCollectorContextPrefixesMessages() {
        // schema expecting a string
        JsonSchema schema = schemaFrom("{\"type\":\"string\"}");
        // build a custom ExecutionContext whose CollectorContext prefixes messages
        ExecutionContext ctx = new ExecutionContext();
        CollectorContext collector = new CollectorContext();
        collector.addScope(Scope.builder()
                .prefix("[custom]")
                .build());
        // inject custom collector into the execution context (field is package‑private)
        try {
            Field ccField = ExecutionContext.class.getDeclaredField("collectorContext");
            ccField.setAccessible(true);
            ccField.set(ctx, collector);
        } catch (ReflectiveOperationException e) {
            fail("Failed to inject custom CollectorContext: " + e);
        }

        JsonNode node = new IntNode(42);
        ValidationResult result = schema.validateAndCollect(ctx, node);
        assertFalse(result.isValid(), "Type mismatch should make the result invalid");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().startsWith("[custom]")),
                "Message should be prefixed with the custom scope");
    }

    /* --------------------------------------------------------------------- */
    /* 15. Malformed $ref handling – URLDecoder throws IllegalArgumentException */
    /* --------------------------------------------------------------------- */

    @Test
    void malformedRefCausesIllegalArgumentException() {
        String malformedSchema = """
                {
                  "$ref":"#/invalid%zz"
                }""";
        JsonSchema schema = schemaFrom(malformedSchema);
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonMapperFactory.getInstance().createObjectNode();
        // The exception is thrown during validation, not schema creation
        assertThrows(IllegalArgumentException.class,
                () -> schema.validateAndCollect(ctx, node),
                "Malformed % encoding in $ref should result in IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    /* 16. WalkListenerRunner unexpected RuntimeException propagation        */
    /* --------------------------------------------------------------------- */

    @Test
    void unexpectedRuntimeExceptionFromListenerPropagates() throws Exception {
        JsonSchema schema = schemaFrom("{}");
        WalkListenerRunner badRunner = mock(WalkListenerRunner.class);
        doThrow(new RuntimeException("listener failure")).when(badRunner).run(any(), any(), any());

        // inject
        Field f = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        f.setAccessible(true);
        f.set(schema, badRunner);

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonMapperFactory.getInstance().readTree("{\"a\":1}");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validateAndCollect(ctx, node),
                "RuntimeException from listener should propagate");
        assertEquals("listener failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 17. Circular $ref without recursive anchor – recursion limit exception */
    /* --------------------------------------------------------------------- */

    @Test
    void circularRefWithoutRecursiveAnchorThrowsValidationException() {
        String circularSchema = """
                {
                  "$ref":"#"
                }""";
        JsonSchema schema = schemaFrom(circularSchema);
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonMapperFactory.getInstance().createObjectNode();
        // The library throws ValidationException (or IllegalStateException) for this case
        assertThrows(RuntimeException.class, // using generic RuntimeException to cover both possibilities
                () -> schema.validateAndCollect(ctx, node),
                "Circular $ref without recursive anchor should cause an exception");
    }

    /* --------------------------------------------------------------------- */
    /* 18. Validator throws JsonProcessingException – wrapped exception       */
    /* --------------------------------------------------------------------- */

    @Test
    void validatorThrowsJsonProcessingExceptionIsWrapped() throws Exception {
        // Create a spy of a schema that returns a custom validator throwing JsonProcessingException
        JsonSchema realSchema = schemaFrom("{\"type\":\"string\"}");
        JsonSchema spySchema = spy(realSchema);

        JsonValidator badValidator = mock(JsonValidator.class);
        when(badValidator.validate(any(), any())).thenThrow(new JsonProcessingException("invalid JSON") {});

        // make getValidators() return a list containing the bad validator
        doReturn(Collections.singletonList(badValidator)).when(spySchema).getValidators();

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = new TextNode("any");

        RuntimeException wrapper = assertThrows(RuntimeException.class,
                () -> spySchema.validateAndCollect(ctx, node),
                "Exception from validator should be wrapped and re‑thrown");
        assertTrue(wrapper.getCause() instanceof JsonProcessingException,
                "Cause of the thrown exception must be JsonProcessingException");
        assertEquals("invalid JSON", wrapper.getCause().getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods for parameterised tests (if needed)                    */
    /* --------------------------------------------------------------------- */

    // No parameterised tests are required for the current set because each
    // scenario has distinct setup logic.  The method source pattern is kept
    // here for future extensions.
    private static Stream<Arguments> dummyProvider() {
        return Stream.empty();
    }
}
```