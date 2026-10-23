package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.net.URI;
import com.fasterxml.jackson.databind.node.*;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import com.networknt.schema.walk.WalkListenerRunner;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walkAtNode_Test {

    // ---------- Mocks & shared test data ----------
    @Mock
    private ValidationContext mockValidationContext;

    @Mock
    private JsonMetaSchema mockMetaSchema;

    @Mock
    private JsonSchemaFactory mockJsonSchemaFactory;

    @Mock
    private SchemaValidatorsConfig mockConfig;

    // class under test
    private JsonSchema jsonSchema;

    private ExecutionContext executionContext;

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Simple implementation of {@link WalkListenerRunner} used for testing.
     */
    private static class SimpleListener implements WalkListenerRunner {

        private boolean beforeCalled = false;

        private boolean afterCalled = false;

        @Override
        public void runPreWalkListeners(ExecutionContext ctx, String path, JsonNode node, JsonNode rootNode, String parentSchemaPath, JsonNode parentSchemaNode, JsonNode schemaNode, JsonSchema schema, ValidationContext validationContext, JsonSchemaFactory factory, Set<ValidationMessage> messages) {
            beforeCalled = true;
        }

        @Override
        public void runPostWalkListeners(ExecutionContext ctx, String path, JsonNode node, JsonNode rootNode, String parentSchemaPath, JsonNode parentSchemaNode, JsonNode schemaNode, JsonSchema schema, ValidationContext validationContext, JsonSchemaFactory factory, Set<ValidationMessage> messages) {
            afterCalled = true;
        }

        boolean wasBeforeCalled() {
            return beforeCalled;
        }

        boolean wasAfterCalled() {
            return afterCalled;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Build a minimal (empty) schema node
        JsonNode emptySchemaNode = mapper.readTree("{}");
        // Create the JsonSchema instance. ValidationContext is not needed for these tests.
        jsonSchema = JsonSchema.from(null, "$", new URI("http://example.com/schema"), emptySchemaNode, null, false);
        // Simple execution context – no keyword‑walk listener runner is required.
        executionContext = new ExecutionContext();
    }

    /**
     * Build a {@link JsonSchema} instance from a JSON schema node.
     */
    private JsonSchema buildSchema(JsonNode schemaNode) throws Exception {
        return JsonSchema.from(null, "$", new URI("http://example.com/schema"), schemaNode, null, false);
    }

    /**
     * Create a mock {@link ExecutionContext} that returns the given {@link JsonSchema}.
     */
    private ExecutionContext mockExecutionContext(JsonSchema schema) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getSchema()).thenReturn(schema);
        // By default no WalkListenerRunner
        when(ctx.getKeywordWalkListenerRunner()).thenReturn(null);
        return ctx;
    }

    /**
     * Convenience wrapper that also sets up a mock ExecutionContext when the schema is null
     */
    private ExecutionContext mockExecutionContext(JsonSchema schema, WalkListenerRunner runner) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getSchema()).thenReturn(schema);
        when(ctx.getKeywordWalkListenerRunner()).thenReturn(runner);
        return ctx;
    }

    /**
     * Returns a simple object schema with a required property.
     */
    private JsonSchema requiredSchema(String... props) throws Exception {
        ArrayNode required = mapper.createArrayNode();
        for (String p : props) {
            required.add(p);
        }
        ObjectNode schema = mapper.createObjectNode().put("type", "object").set("required", required);
        return buildSchema(schema);
    }

    /**
     * Returns a schema that only defines a type.
     */
    private JsonSchema typeSchema(String type) throws Exception {
        ObjectNode schema = mapper.createObjectNode().put("type", type);
        return buildSchema(schema);
    }

    /**
     * Returns a schema with a dynamic anchor.
     */
    private JsonSchema dynamicAnchorSchema() throws Exception {
        ObjectNode schema = mapper.createObjectNode().put("$dynamicAnchor", "myAnchor");
        return buildSchema(schema);
    }

    /**
     * Returns a boolean schema (true or false).
     */
    private JsonSchema booleanSchema(boolean value) throws Exception {
        JsonNode schema = BooleanNode.valueOf(value);
        return buildSchema(schema);
    }

    /**
     * Returns a schema with an unknown keyword – used to trigger JsonSchemaException.
     */
    private JsonSchema unknownKeywordSchema() throws Exception {
        ObjectNode schema = mapper.createObjectNode().put("unknownKeyword", "value");
        return buildSchema(schema);
    }

    /**
     * Returns a schema with an invalid JSON‑pointer path (no leading slash).
     */
    private JsonSchema simpleSchema() throws Exception {
        return buildSchema(mapper.createObjectNode());
    }

    /**
     * Helper that creates a plain {@link ExecutionContext}.
     */
    private ExecutionContext simpleExecutionContext() {
        return new ExecutionContext();
    }

    /**
     * Helper that creates an {@link ExecutionContext} with a supplied {@link WalkListenerRunner}.
     */
    private ExecutionContext executionContextWithRunner(WalkListenerRunner runner) {
        return new ExecutionContext() {

            @Override
            public WalkListenerRunner getKeywordWalkListenerRunner() {
                return runner;
            }
        };
    }

    /**
     * Checks whether any validation message contains the given text (case‑insensitive).
     */
    private static boolean containsMessageContaining(ValidationResult result, String fragment) {
        // ValidationResult may expose messages as Set<String> or Set<ValidationMessage>.
        // Handle both possibilities.
        Set<?> msgs = result.getMessages();
        for (Object o : msgs) {
            String text;
            if (o instanceof ValidationMessage) {
                text = ((ValidationMessage) o).getMessage();
            } else {
                text = o.toString();
            }
            if (text.toLowerCase().contains(fragment.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a simple {@link ExecutionContext} that returns the supplied schema
     * and (optionally) a {@link WalkListenerRunner}.
     */
    private ExecutionContext createExecutionContext(JsonSchema schema, WalkListenerRunner runner) {
        return new ExecutionContext() {

            @Override
            public JsonSchema getSchema() {
                return schema;
            }

            @Override
            public WalkListenerRunner getKeywordWalkListenerRunner() {
                return runner;
            }
        };
    }

    /**
     * Create a stub {@link ExecutionContext} that returns the given {@link JsonSchema}
     * and (optionally) a {@link WalkListenerRunner}.
     */
    private ExecutionContext stubExecutionContext(JsonSchema schema, WalkListenerRunner runner) {
        return new ExecutionContext() {

            @Override
            public JsonSchema getSchema() {
                return schema;
            }

            @Override
            public WalkListenerRunner getKeywordWalkListenerRunner() {
                return runner;
            }
        };
    }

    // ---------- Tests ----------
    @Test
    void walkAtNode_WithEmptySchemaAndSimpleNode_ReturnsEmptyValidationResult() throws Exception {
        JsonNode instanceNode = mapper.readTree("{\"name\":\"John\",\"age\":30}");
        JsonNode rootNode = instanceNode;
        ValidationResult result = jsonSchema.walkAtNode(executionContext, instanceNode, rootNode, "", false);
        Assertions.assertNotNull(result, "walkAtNode should never return null");
        Assertions.assertTrue(result.getMessages().isEmpty(), "Expected no validation messages for an empty schema, but got: " + result.getMessages());
    }

    @Test
    void walkAtNode_WithEmptySchemaAndValidationEnabled_ReturnsEmptyValidationResult() throws Exception {
        JsonNode instanceNode = mapper.readTree("[1,2,3]");
        JsonNode rootNode = instanceNode;
        ValidationResult result = jsonSchema.walkAtNode(executionContext, instanceNode, rootNode, "", true);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getMessages().isEmpty(), "Even when validation is enabled, an empty schema should produce no messages.");
    }

    @Test
    void walkAtNode_WithNullNode_ReturnsEmptyValidationResult() {
        JsonNode nullNode = null;
        JsonNode rootNode = mapper.createObjectNode();
        ValidationResult result = jsonSchema.walkAtNode(executionContext, nullNode, rootNode, "", false);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getMessages().isEmpty(), "walkAtNode should handle null node gracefully and return an empty result.");
    }

    /**
     * Null handling **
     */
    @Test
    void walkAtNode_AllParametersNull_ReturnsInvalidResult() {
        JsonSchema schema = JsonSchema.from(null, "$", null, null, null, false);
        ValidationResult result = schema.walkAtNode(null, null, null, "", false);
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess(), "Result must be invalid when everything is null");
        Assertions.assertFalse(result.getMessages().isEmpty(), "A validation message should explain the problem");
    }

    @Test
    void walkAtNode_NullInstanceNodeWithValidationEnabled_ReturnsMissingInstanceMessage() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = simpleExecutionContext();
        ValidationResult result = schema.walkAtNode(ctx, null, mapper.createObjectNode(), "/", true);
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess(), "Missing instance should produce an invalid result");
        Assertions.assertTrue(containsMessageContaining(result, "instance is missing"));
    }

    /**
     * Validation flag handling **
     */
    @Test
    void walkAtNode_ValidateFlagFalse_SkipsValidation() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, node, node, "/data", false);
        Assertions.assertTrue(result.isSuccess(), "When validation flag is false the walk should be successful");
        Assertions.assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void walkAtNode_ValidateFlagTrue_WithEmptySchema_ReturnsValidResult() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, node, node, "/data", true);
        Assertions.assertTrue(result.isSuccess(), "Empty schema should always validate successfully");
    }

    /**
     * Required validator branch **
     */
    @Test
    void walkAtNode_RequiredPropertyMissing_ReturnsInvalidResult() throws Exception {
        JsonSchema schema = requiredSchema("name");
        ExecutionContext ctx = simpleExecutionContext();
        // empty object
        JsonNode instance = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/person", true);
        Assertions.assertFalse(result.isSuccess(), "Missing required property must cause validation failure");
        Assertions.assertTrue(containsMessageContaining(result, "required"));
    }

    @Test
    void walkAtNode_RequiredPropertyPresent_ReturnsValidResult() throws Exception {
        JsonSchema schema = requiredSchema("name");
        ExecutionContext ctx = simpleExecutionContext();
        ObjectNode instance = JsonNodeFactory.instance.objectNode().put("name", "John");
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/person", true);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getMessages().isEmpty());
    }

    /**
     * Type validator branch **
     */
    @Test
    void walkAtNode_TypeMismatch_ReturnsInvalidResult() throws Exception {
        JsonSchema schema = typeSchema("string");
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode instance = JsonNodeFactory.instance.numberNode(10);
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/age", true);
        Assertions.assertFalse(result.isSuccess(), "Type mismatch should be reported");
        Assertions.assertTrue(containsMessageContaining(result, "type"));
    }

    @Test
    void walkAtNode_TypeMatch_ReturnsValidResult() throws Exception {
        JsonSchema schema = typeSchema("integer");
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode instance = JsonNodeFactory.instance.numberNode(5);
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/count", true);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getMessages().isEmpty());
    }

    /**
     * Dynamic anchor handling **
     */
    @Test
    void walkAtNode_DynamicAnchor_DoesNotFailValidation() throws Exception {
        JsonSchema schema = dynamicAnchorSchema();
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode instance = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/", true);
        Assertions.assertTrue(result.isSuccess(), "Dynamic anchors should not cause validation errors");
    }

    /**
     * Exception handling – UnsupportedEncodingException **
     */
    @Test
    void walkAtNode_SchemaCreationThrowsUnsupportedEncodingException_ReturnsInvalidResult() throws Exception {
        ExecutionContext ctx = new ExecutionContext() {

            @Override
            public JsonSchema getSchema() {
                try {
                    throw new UnsupportedEncodingException("UTF‑8 not supported");
                } catch (UnsupportedEncodingException e) {
                    // wrap in a RuntimeException to satisfy the method signature
                    throw new RuntimeException(e);
                }
            }
        };
        JsonSchema dummy = buildSchema(mapper.createObjectNode());
        ValidationResult result = dummy.walkAtNode(ctx, JsonNodeFactory.instance.objectNode(), JsonNodeFactory.instance.objectNode(), "/", true);
        Assertions.assertFalse(result.isSuccess(), "Exception should be wrapped in an invalid ValidationResult");
        Assertions.assertTrue(containsMessageContaining(result, "UnsupportedEncodingException"));
    }

    /**
     * KeywordWalkListenerRunner – normal flow **
     */
    @Test
    void walkAtNode_WithKeywordWalkListenerRunner_InvokesCallbacks() throws Exception {
        SimpleListener listener = new SimpleListener();
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = stubExecutionContext(schema, listener);
        JsonNode instance = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/", true);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(listener.wasBeforeCalled(), "beforeWalk should have been invoked");
        Assertions.assertTrue(listener.wasAfterCalled(), "afterWalk should have been invoked");
    }

    /**
     * KeywordWalkListenerRunner – null fallback **
     */
    @Test
    void walkAtNode_NoKeywordWalkListenerRunner_DoesNotThrow() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = stubExecutionContext(schema, null);
        JsonNode instance = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/", true);
        Assertions.assertTrue(result.isSuccess(), "Walk should succeed even when no listener runner is supplied");
    }

    /**
     * $ref resolution – malformed URI **
     */
    @Test
    void walkAtNode_MalformedRefUri_ReturnsInvalidResult() throws Exception {
        ObjectNode schemaNode = mapper.createObjectNode().put("$ref", "http://[invalid-uri]");
        JsonSchema schema = buildSchema(schemaNode);
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode instance = JsonNodeFactory.instance.textNode("test");
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/field", true);
        Assertions.assertFalse(result.isSuccess(), "Malformed $ref URI should result in validation failure");
        Assertions.assertTrue(containsMessageContaining(result, "URISyntaxException"));
    }

    /**
     * ExecutionContext null handling **
     */
    @Test
    void walkAtNode_NullExecutionContext_ReturnsInvalidResult() {
        JsonSchema schema = JsonSchema.from(null, "$", URI.create("http://example.com"), mapper.createObjectNode(), null, false);
        ValidationResult result = schema.walkAtNode(null, JsonNodeFactory.instance.objectNode(), JsonNodeFactory.instance.objectNode(), "/", true);
        Assertions.assertFalse(result.isSuccess(), "Null ExecutionContext must be reported as an error");
        Assertions.assertTrue(containsMessageContaining(result, "ExecutionContext"));
    }
}
