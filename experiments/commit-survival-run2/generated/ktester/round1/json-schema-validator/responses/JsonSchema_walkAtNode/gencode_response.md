```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link JsonSchema#walkAtNode(ExecutionContext, JsonNode, JsonNode, String, boolean)}.
 *
 * <p>Each test focuses on a particular behaviour described in the supplied JSON test matrix.
 * Helper methods are used to build minimal schema definitions and to create a mock {@link ExecutionContext}
 * that returns those schemas.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walkAtNode_Test {

    // ---------- Shared mocks ----------
    @Mock
    private ValidationContext mockValidationContext;
    @Mock
    private JsonMetaSchema mockMetaSchema;

    private ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void beforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setUp() {
        // All ValidationContext interactions needed for JsonSchema.from()
        when(mockValidationContext.getMetaSchema()).thenReturn(mockMetaSchema);
        // No keyword‑walk listener by default
        when(mockValidationContext.getConfig()).thenReturn(null);
    }

    /* -------------------------------------------------
     *  Helper utilities
     * ------------------------------------------------- */

    /** Build a {@link JsonSchema} instance from a JSON schema node. */
    private JsonSchema buildSchema(JsonNode schemaNode) throws Exception {
        return JsonSchema.from(
                mockValidationContext,
                "$",                               // schemaPath (root)
                new URI("http://example.com/schema"),
                schemaNode,
                null,
                false);
    }

    /** Create a mock {@link ExecutionContext} that returns the given {@link JsonSchema}. */
    private ExecutionContext mockExecutionContext(JsonSchema schema) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getSchema()).thenReturn(schema);
        // By default no WalkListenerRunner
        when(ctx.getKeywordWalkListenerRunner()).thenReturn(null);
        return ctx;
    }

    /** Convenience wrapper that also sets up a mock ExecutionContext when the schema is null */
    private ExecutionContext mockExecutionContext(JsonSchema schema, WalkListenerRunner runner) {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getSchema()).thenReturn(schema);
        when(ctx.getKeywordWalkListenerRunner()).thenReturn(runner);
        return ctx;
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
        ObjectNode schema = mapper.createObjectNode()
                .put("type", type);
        return buildSchema(schema);
    }

    /** Returns a schema with a dynamic anchor. */
    private JsonSchema dynamicAnchorSchema() throws Exception {
        ObjectNode schema = mapper.createObjectNode()
                .put("$dynamicAnchor", "myAnchor");
        return buildSchema(schema);
    }

    /** Returns a boolean schema (true or false). */
    private JsonSchema booleanSchema(boolean value) throws Exception {
        JsonNode schema = BooleanNode.valueOf(value);
        return buildSchema(schema);
    }

    /** Returns a schema with an unknown keyword – used to trigger JsonSchemaException. */
    private JsonSchema unknownKeywordSchema() throws Exception {
        ObjectNode schema = mapper.createObjectNode()
                .put("unknownKeyword", "value");
        return buildSchema(schema);
    }

    /** Returns a schema with an invalid JSON‑pointer path (no leading slash). */
    private JsonSchema simpleSchema() throws Exception {
        return buildSchema(mapper.createObjectNode());
    }

    /* -------------------------------------------------
     *  Test groups
     * ------------------------------------------------- */

    /*** Null handling ***/
    @Test
    void walkAtNode_AllParametersNull_ReturnsInvalidResult() {
        ValidationResult result = new JsonSchema(null, null, null, null, false)
                .walkAtNode(null, null, null, "", false);
        assertNotNull(result);
        assertFalse(result.isValid(), "Result must be invalid when everything is null");
        assertFalse(result.getMessages().isEmpty(), "A validation message should explain the problem");
    }

    @Test
    void walkAtNode_NullInstanceNodeWithValidationEnabled_ReturnsMissingInstanceMessage() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode()); // empty schema
        ExecutionContext ctx = mockExecutionContext(schema);
        ValidationResult result = schema.walkAtNode(ctx, null, mapper.createObjectNode(), "/", true);
        assertNotNull(result);
        assertFalse(result.isValid(), "Missing instance should produce an invalid result");
        assertTrue(containsMessageContaining(result, "instance is missing"));
    }

    /*** Validation flag handling ***/
    @Test
    void walkAtNode_ValidateFlagFalse_SkipsValidation() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode()); // no constraints
        ExecutionContext ctx = mockExecutionContext(schema);
        JsonNode node = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, node, node, "/data", false);
        assertTrue(result.isValid(), "When validation flag is false the walk should be successful");
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void walkAtNode_ValidateFlagTrue_WithEmptySchema_ReturnsValidResult() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = mockExecutionContext(schema);
        JsonNode node = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, node, node, "/data", true);
        assertTrue(result.isValid(), "Empty schema should always validate successfully");
    }

    /*** Required validator branch ***/
    @Test
    void walkAtNode_RequiredPropertyMissing_ReturnsInvalidResult() throws Exception {
        JsonSchema schema = requiredSchema("name");
        ExecutionContext ctx = mockExecutionContext(schema);
        JsonNode instance = JsonNodeFactory.instance.objectNode(); // empty object
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/person", true);
        assertFalse(result.isValid(), "Missing required property must cause validation failure");
        assertTrue(containsMessageContaining(result, "required"));
    }

    @Test
    void walkAtNode_RequiredPropertyPresent_ReturnsValidResult() throws Exception {
        JsonSchema schema = requiredSchema("name");
        ExecutionContext ctx = mockExecutionContext(schema);
        ObjectNode instance = JsonNodeFactory.instance.objectNode().put("name", "John");
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/person", true);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty());
    }

    /*** Type validator branch ***/
    @Test
    void walkAtNode_TypeMismatch_ReturnsInvalidResult() throws Exception {
        JsonSchema schema = typeSchema("string");
        ExecutionContext ctx = mockExecutionContext(schema);
        JsonNode instance = JsonNodeFactory.instance.numberNode(10);
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/age", true);
        assertFalse(result.isValid(), "Type mismatch should be reported");
        assertTrue(containsMessageContaining(result, "type"));
    }

    @Test
    void walkAtNode_TypeMatch_ReturnsValidResult() throws Exception {
        JsonSchema schema = typeSchema("integer");
        ExecutionContext ctx = mockExecutionContext(schema);
        JsonNode instance = JsonNodeFactory.instance.numberNode(5);
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/count", true);
        assertTrue(result.isValid());
        assertTrue(result.getMessages().isEmpty());
    }

    /*** Dynamic anchor handling ***/
    @Test
    void walkAtNode_DynamicAnchor_DoesNotFailValidation() throws Exception {
        JsonSchema schema = dynamicAnchorSchema();
        ExecutionContext ctx = mockExecutionContext(schema);
        JsonNode instance = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/", true);
        assertTrue(result.isValid(), "Dynamic anchors should not cause validation errors");
    }

    /*** Exception handling – UnsupportedEncodingException ***/
    @Test
    void walkAtNode_SchemaCreationThrowsUnsupportedEncodingException_ReturnsInvalidResult() throws Exception {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getSchema()).thenThrow(new UnsupportedEncodingException("UTF‑8 not supported"));
        JsonSchema dummy = buildSchema(mapper.createObjectNode()); // schema not used, just to call method
        ValidationResult result = dummy.walkAtNode(ctx, JsonNodeFactory.instance.objectNode(),
                JsonNodeFactory.instance.objectNode(), "/", true);
        assertFalse(result.isValid(), "Exception should be wrapped in an invalid ValidationResult");
        assertTrue(containsMessageContaining(result, "UnsupportedEncodingException"));
    }

    /*** KeywordWalkListenerRunner – normal flow ***/
    @Test
    void walkAtNode_WithKeywordWalkListenerRunner_InvokesCallbacks() throws Exception {
        // Spy listener to verify calls
        WalkListenerRunner listener = spy(new DefaultKeywordWalkListenerRunner());

        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = mockExecutionContext(schema, listener);
        JsonNode instance = JsonNodeFactory.instance.objectNode();

        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/", true);

        assertTrue(result.isValid());
        verify(listener, times(1)).beforeWalk(eq(schema), eq(instance), eq(instance), eq("/"));
        verify(listener, times(1)).afterWalk(eq(schema), eq(instance), eq(instance), eq("/"), any());
    }

    /*** KeywordWalkListenerRunner – null fallback ***/
    @Test
    void walkAtNode_NoKeywordWalkListenerRunner_DoesNotThrow() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = mockExecutionContext(schema, null);
        JsonNode instance = JsonNodeFactory.instance.objectNode();

        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/", true);
        assertTrue(result.isValid(), "Walk should succeed even when no listener runner is supplied");
    }

    /*** $ref resolution – malformed URI ***/
    @Test
    void walkAtNode_MalformedRefUri_ReturnsInvalidResult() throws Exception {
        ObjectNode schemaNode = mapper.createObjectNode()
                .put("$ref", "http://[invalid-uri]");
        JsonSchema schema = buildSchema(schemaNode);
        ExecutionContext ctx = mockExecutionContext(schema);
        JsonNode instance = JsonNodeFactory.instance.textNode("test");

        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/field", true);
        assertFalse(result.isValid(), "Malformed $ref URI should result in validation failure");
        assertTrue(containsMessageContaining(result, "URISyntaxException"));
    }

    /*** ExecutionContext null handling ***/
    @Test
    void walkAtNode_NullExecutionContext_ReturnsInvalidResult() {
        JsonSchema schema = JsonSchema.from(mockValidationContext, "$", URI.create("http://example.com"), mapper.createObjectNode(), null, false);
        ValidationResult result = schema.walkAtNode(null, JsonNodeFactory.instance.objectNode(),
                JsonNodeFactory.instance.objectNode(), "/", true);
        assertFalse(result.isValid(), "Null ExecutionContext must be reported as an error");
        assertTrue(containsMessageContaining(result, "ExecutionContext"));
    }

    /*** RootNode null handling with non‑null instance ***/
    @Test
    void walkAtNode_NullRootNode_ReturnsInvalidResult() throws Exception {
        JsonSchema schema = buildSchema(map