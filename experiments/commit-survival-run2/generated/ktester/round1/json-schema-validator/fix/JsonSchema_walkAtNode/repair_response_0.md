```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Unit tests for {@link JsonSchema#walkAtNode(ExecutionContext, JsonNode, JsonNode, String, boolean)}.
 *
 * <p>This test class has been rewritten to avoid external mocking frameworks
 * (e.g. Mockito) which are not available in the compilation environment.
 * Simple concrete or anonymous implementations are used instead.</p>
 */
class JsonSchema_walkAtNode_Test {

    // ---------- Shared test data ----------
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonSchema jsonSchema;          // schema used in the first two tests
    private ExecutionContext executionContext; // simple execution context

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Build a minimal (empty) schema node
        JsonNode emptySchemaNode = mapper.readTree("{}");

        // Create the JsonSchema instance.  The ValidationContext parameter can be null
        // because the validator does not need it for the features exercised in these tests.
        jsonSchema = JsonSchema.from(
                null,                     // ValidationContext – not needed for these tests
                "$",                      // schemaPath (root)
                new URI("http://example.com/schema"),
                emptySchemaNode,
                null,                     // parent schema
                false);                   // do not suppress sub‑schema retrieval

        // Simple execution context – no keyword‑walk listener runner is required.
        executionContext = new ExecutionContext();
    }

    /** Helper that builds a {@link JsonSchema} from a JSON schema node. */
    private JsonSchema buildSchema(JsonNode schemaNode) throws Exception {
        return JsonSchema.from(
                null,
                "$",
                new URI("http://example.com/schema"),
                schemaNode,
                null,
                false);
    }

    /** Helper that creates a plain {@link ExecutionContext}. */
    private ExecutionContext simpleExecutionContext() {
        return new ExecutionContext();
    }

    /** Helper that creates an {@link ExecutionContext} with a supplied {@link WalkListenerRunner}. */
    private ExecutionContext executionContextWithRunner(WalkListenerRunner runner) {
        return new ExecutionContext() {
            @Override
            public WalkListenerRunner getKeywordWalkListenerRunner() {
                return runner;
            }
        };
    }

    /** Returns a simple object schema with a required property. */
    private JsonSchema requiredSchema(String... props) throws Exception {
        ArrayNode required = mapper.createArrayNode();
        for (String p : props) {
            required.add(p);
        }
        ObjectNode schema = mapper.createObjectNode()
                .put("type", "object")
                .set("required", required);
        return buildSchema(schema);
    }

    /** Returns a schema that only defines a type. */
    private JsonSchema typeSchema(String type) throws Exception {
        ObjectNode schema = mapper.createObjectNode().put("type", type);
        return buildSchema(schema);
    }

    /** Returns a schema with a dynamic anchor. */
    private JsonSchema dynamicAnchorSchema() throws Exception {
        ObjectNode schema = mapper.createObjectNode().put("$dynamicAnchor", "myAnchor");
        return buildSchema(schema);
    }

    /** Returns a boolean schema (true or false). */
    private JsonSchema booleanSchema(boolean value) throws Exception {
        JsonNode schema = BooleanNode.valueOf(value);
        return buildSchema(schema);
    }

    /** Returns a schema with an unknown keyword – used to trigger {@link JsonSchemaException}. */
    private JsonSchema unknownKeywordSchema() throws Exception {
        ObjectNode schema = mapper.createObjectNode().put("unknownKeyword", "value");
        return buildSchema(schema);
    }

    /** Returns a simple empty object schema. */
    private JsonSchema simpleSchema() throws Exception {
        return buildSchema(mapper.createObjectNode());
    }

    /** Checks whether any validation message contains the given text (case‑insensitive). */
    private static boolean containsMessageContaining(ValidationResult result, String fragment) {
        Set<String> msgs = result.getMessages();
        for (String m : msgs) {
            if (m.toLowerCase().contains(fragment.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // ---------- Tests ----------
    @Test
    void walkAtNode_WithEmptySchemaAndSimpleNode_ReturnsEmptyValidationResult() throws Exception {
        JsonNode instanceNode = mapper.readTree("{\"name\":\"John\",\"age\":30}");
        JsonNode rootNode = instanceNode;
        ValidationResult result = jsonSchema.walkAtNode(executionContext, instanceNode, rootNode, "", false);
        Assertions.assertNotNull(result, "walkAtNode should never return null");
        Assertions.assertTrue(result.getMessages().isEmpty(),
                "Expected no validation messages for an empty schema, but got: " + result.getMessages());
    }

    @Test
    void walkAtNode_WithEmptySchemaAndValidationEnabled_ReturnsEmptyValidationResult() throws Exception {
        JsonNode instanceNode = mapper.readTree("[1,2,3]");
        JsonNode rootNode = instanceNode;
        ValidationResult result = jsonSchema.walkAtNode(executionContext, instanceNode, rootNode, "", true);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getMessages().isEmpty(),
                "Even when validation is enabled, an empty schema should produce no messages.");
    }

    @Test
    void walkAtNode_WithNullNode_ReturnsEmptyValidationResult() {
        JsonNode nullNode = null;
        JsonNode rootNode = mapper.createObjectNode();
        ValidationResult result = jsonSchema.walkAtNode(executionContext, nullNode, rootNode, "", false);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getMessages().isEmpty(),
                "walkAtNode should handle null node gracefully and return an empty result.");
    }

    @Test
    void walkAtNode_AllParametersNull_ReturnsInvalidResult() {
        JsonSchema schema = new JsonSchema(null, null, null, null, false);
        ValidationResult result = schema.walkAtNode(null, null, null, "", false);
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isValid(), "Result must be invalid when everything is null");
        Assertions.assertFalse(result.getMessages().isEmpty(),
                "A validation message should explain the problem");
    }

    @Test
    void walkAtNode_NullInstanceNodeWithValidationEnabled_ReturnsMissingInstanceMessage() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = simpleExecutionContext();
        ValidationResult result = schema.walkAtNode(ctx, null, mapper.createObjectNode(), "/", true);
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isValid(), "Missing instance should produce an invalid result");
        Assertions.assertTrue(containsMessageContaining(result, "instance is missing"));
    }

    @Test
    void walkAtNode_ValidateFlagFalse_SkipsValidation() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, node, node, "/data", false);
        Assertions.assertTrue(result.isValid(), "When validation flag is false the walk should be successful");
        Assertions.assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void walkAtNode_ValidateFlagTrue_WithEmptySchema_ReturnsValidResult() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, node, node, "/data", true);
        Assertions.assertTrue(result.isValid(), "Empty schema should always validate successfully");
    }

    @Test
    void walkAtNode_RequiredPropertyMissing_ReturnsInvalidResult() throws Exception {
        JsonSchema schema = requiredSchema("name");
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode instance = JsonNodeFactory.instance.objectNode(); // empty object
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/person", true);
        Assertions.assertFalse(result.isValid(), "Missing required property must cause validation failure");
        Assertions.assertTrue(containsMessageContaining(result, "required"));
    }

    @Test
    void walkAtNode_RequiredPropertyPresent_ReturnsValidResult() throws Exception {
        JsonSchema schema = requiredSchema("name");
        ExecutionContext ctx = simpleExecutionContext();
        ObjectNode instance = JsonNodeFactory.instance.objectNode().put("name", "John");
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/person", true);
        Assertions.assertTrue(result.isValid());
        Assertions.assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void walkAtNode_TypeMismatch_ReturnsInvalidResult() throws Exception {
        JsonSchema schema = typeSchema("string");
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode instance = JsonNodeFactory.instance.numberNode(10);
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/age", true);
        Assertions.assertFalse(result.isValid(), "Type mismatch should be reported");
        Assertions.assertTrue(containsMessageContaining(result, "type"));
    }

    @Test
    void walkAtNode_TypeMatch_ReturnsValidResult() throws Exception {
        JsonSchema schema = typeSchema("integer");
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode instance = JsonNodeFactory.instance.numberNode(5);
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/count", true);
        Assertions.assertTrue(result.isValid());
        Assertions.assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void walkAtNode_DynamicAnchor_DoesNotFailValidation() throws Exception {
        JsonSchema schema = dynamicAnchorSchema();
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode instance = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/", true);
        Assertions.assertTrue(result.isValid(), "Dynamic anchors should not cause validation errors");
    }

    @Test
    void walkAtNode_SchemaCreationThrowsUnsupportedEncodingException_ReturnsInvalidResult() throws Exception {
        // Simulate an ExecutionContext that throws when getSchema() is called.
        ExecutionContext ctx = new ExecutionContext() {
            @Override
            public JsonSchema getSchema() {
                throw new UnsupportedEncodingException("UTF‑8 not supported");
            }
        };
        JsonSchema dummy = buildSchema(mapper.createObjectNode());
        ValidationResult result = dummy.walkAtNode(ctx,
                JsonNodeFactory.instance.objectNode(),
                JsonNodeFactory.instance.objectNode(),
                "/",
                true);
        Assertions.assertFalse(result.isValid(),
                "Exception should be wrapped in an invalid ValidationResult");
        Assertions.assertTrue(containsMessageContaining(result, "UnsupportedEncodingException"));
    }

    @Test
    void walkAtNode_WithKeywordWalkListenerRunner_InvokesCallbacks() throws Exception {
        // Create a simple listener that records calls.
        WalkListenerRunner listener = new WalkListenerRunner() {
            boolean beforeCalled = false;
            boolean afterCalled = false;

            @Override
            public void beforeWalk(JsonSchema schema, JsonNode node, JsonNode rootNode, String path) {
                beforeCalled = true;
            }

            @Override
            public void afterWalk(JsonSchema schema, JsonNode node, JsonNode rootNode, String path,
                                  ValidationResult result) {
                afterCalled = true;
            }

            // expose the flags for assertions
            boolean wasBeforeCalled() { return beforeCalled; }
            boolean wasAfterCalled() { return afterCalled; }
        };

        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = executionContextWithRunner(listener);
        JsonNode instance = JsonNodeFactory.instance.objectNode();

        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/", true);
        Assertions.assertTrue(result.isValid());
        Assertions.assertTrue(((WalkListenerRunner) listener).wasBeforeCalled(),
                "beforeWalk should have been invoked");
        Assertions.assertTrue(((WalkListenerRunner) listener).wasAfterCalled(),
                "afterWalk should have been invoked");
    }

    @Test
    void walkAtNode_NoKeywordWalkListenerRunner_DoesNotThrow() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = executionContextWithRunner(null);
        JsonNode instance = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/", true);
        Assertions.assertTrue(result.isValid(),
                "Walk should succeed even when no listener runner is supplied");
    }

    @Test
    void walkAtNode_MalformedRefUri_ReturnsInvalidResult() throws Exception {
        ObjectNode schemaNode = mapper.createObjectNode().put("$ref", "http://[invalid-uri]");
        JsonSchema schema = buildSchema(schemaNode);
        ExecutionContext ctx = simpleExecutionContext();
        JsonNode instance = JsonNodeFactory.instance.textNode("test");
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/field", true);
        Assertions.assertFalse(result.isValid(),
                "Malformed $ref URI should result in validation failure");
        Assertions.assertTrue(containsMessageContaining(result, "URISyntaxException"));
    }

    @Test
    void walkAtNode_NullExecutionContext_ReturnsInvalidResult() {
        JsonSchema schema = JsonSchema.from(null,
                "$",
                URI.create("http://example.com"),
                mapper.createObjectNode(),
                null,
                false);
        ValidationResult result = schema.walkAtNode(null,
                JsonNodeFactory.instance.objectNode(),
                JsonNodeFactory.instance.objectNode(),
                "/",
                true);
        Assertions.assertFalse(result.isValid(),
                "Null ExecutionContext must be reported as an error");
        Assertions.assertTrue(containsMessageContaining(result, "ExecutionContext"));
    }
}
```