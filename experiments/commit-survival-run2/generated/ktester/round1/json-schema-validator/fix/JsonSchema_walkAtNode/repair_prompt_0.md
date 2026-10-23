
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Stream;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import com.networknt.schema.walk.WalkListenerRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

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

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----- Configure the ValidationContext mock -----
        when(mockValidationContext.getMetaSchema()).thenReturn(mockMetaSchema);
        // Return null for config – this disables the keywordWalkListenerRunner.
        when(mockValidationContext.getConfig()).thenReturn(null);
        // ----- Build a minimal schema node (empty object) -----
        JsonNode emptySchemaNode = mapper.readTree("{}");
        // ----- Create the JsonSchema instance via the public factory method -----
        // Parameters: validationContext, schemaPath, currentUri, schemaNode, parent, suppressSubSchemaRetrieval
        jsonSchema = JsonSchema.from(mockValidationContext, // schemaPath (root)
        "$", // currentUri – any valid URI works
        new URI("http://example.com/schema"), emptySchemaNode, // no parent schema
        null, // do not suppress sub‑schema retrieval
        false);
        // ----- Execution context for validation runs -----
        executionContext = new ExecutionContext();
        // All ValidationContext interactions needed for JsonSchema.from()
        when(mockValidationContext.getMetaSchema()).thenReturn(mockMetaSchema);
        // No keyword‑walk listener by default
        when(mockValidationContext.getConfig()).thenReturn(null);
    }

    /**
     * Build a {@link JsonSchema} instance from a JSON schema node.
     */
    private JsonSchema buildSchema(JsonNode schemaNode) throws Exception {
        return JsonSchema.from(mockValidationContext, // schemaPath (root)
        "$", new URI("http://example.com/schema"), schemaNode, null, false);
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

    // ---------- Tests ----------
    @Test
    void walkAtNode_WithEmptySchemaAndSimpleNode_ReturnsEmptyValidationResult() throws Exception {
        // Given: a simple JSON instance node
        JsonNode instanceNode = mapper.readTree("{\"name\":\"John\",\"age\":30}");
        // for this test the root is the same node
        JsonNode rootNode = instanceNode;
        // When: invoking walkAtNode (validation disabled)
        ValidationResult result = jsonSchema.walkAtNode(executionContext, instanceNode, rootNode, "", false);
        // Then: result should be non‑null and contain no validation messages
        assertNotNull(result, "walkAtNode should never return null");
        assertTrue(result.getMessages().isEmpty(), "Expected no validation messages for an empty schema, but got: " + result.getMessages());
    }

    @Test
    void walkAtNode_WithEmptySchemaAndValidationEnabled_ReturnsEmptyValidationResult() throws Exception {
        // Given: a simple JSON instance node
        JsonNode instanceNode = mapper.readTree("[1,2,3]");
        JsonNode rootNode = instanceNode;
        // When: invoking walkAtNode with shouldValidateSchema = true
        ValidationResult result = jsonSchema.walkAtNode(executionContext, instanceNode, rootNode, "", true);
        // Then: still no messages because the schema does not define any constraints
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "Even when validation is enabled, an empty schema should produce no messages.");
    }

    @Test
    void walkAtNode_WithNullNode_ReturnsEmptyValidationResult() {
        // Given: a null instance node (simulating missing data)
        JsonNode nullNode = null;
        // empty root
        JsonNode rootNode = mapper.createObjectNode();
        // When: invoking walkAtNode
        ValidationResult result = jsonSchema.walkAtNode(executionContext, nullNode, rootNode, "", false);
        // Then: result should be non‑null and contain no messages (the method guards against nulls internally)
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "walkAtNode should handle null node gracefully and return an empty result.");
    }

    /**
     * Null handling **
     */
    @Test
    void walkAtNode_AllParametersNull_ReturnsInvalidResult() {
        ValidationResult result = new JsonSchema(null, null, null, null, false).walkAtNode(null, null, null, "", false);
        assertNotNull(result);
        assertFalse(result.isValid(), "Result must be invalid when everything is null");
        assertFalse(result.getMessages().isEmpty(), "A validation message should explain the problem");
    }

    @Test
    void walkAtNode_NullInstanceNodeWithValidationEnabled_ReturnsMissingInstanceMessage() throws Exception {
        // empty schema
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = mockExecutionContext(schema);
        ValidationResult result = schema.walkAtNode(ctx, null, mapper.createObjectNode(), "/", true);
        assertNotNull(result);
        assertFalse(result.isValid(), "Missing instance should produce an invalid result");
        assertTrue(containsMessageContaining(result, "instance is missing"));
    }

    /**
     * Validation flag handling **
     */
    @Test
    void walkAtNode_ValidateFlagFalse_SkipsValidation() throws Exception {
        // no constraints
        JsonSchema schema = buildSchema(mapper.createObjectNode());
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

    /**
     * Required validator branch **
     */
    @Test
    void walkAtNode_RequiredPropertyMissing_ReturnsInvalidResult() throws Exception {
        JsonSchema schema = requiredSchema("name");
        ExecutionContext ctx = mockExecutionContext(schema);
        // empty object
        JsonNode instance = JsonNodeFactory.instance.objectNode();
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

    /**
     * Type validator branch **
     */
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

    /**
     * Dynamic anchor handling **
     */
    @Test
    void walkAtNode_DynamicAnchor_DoesNotFailValidation() throws Exception {
        JsonSchema schema = dynamicAnchorSchema();
        ExecutionContext ctx = mockExecutionContext(schema);
        JsonNode instance = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/", true);
        assertTrue(result.isValid(), "Dynamic anchors should not cause validation errors");
    }

    /**
     * Exception handling – UnsupportedEncodingException **
     */
    @Test
    void walkAtNode_SchemaCreationThrowsUnsupportedEncodingException_ReturnsInvalidResult() throws Exception {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getSchema()).thenThrow(new UnsupportedEncodingException("UTF‑8 not supported"));
        // schema not used, just to call method
        JsonSchema dummy = buildSchema(mapper.createObjectNode());
        ValidationResult result = dummy.walkAtNode(ctx, JsonNodeFactory.instance.objectNode(), JsonNodeFactory.instance.objectNode(), "/", true);
        assertFalse(result.isValid(), "Exception should be wrapped in an invalid ValidationResult");
        assertTrue(containsMessageContaining(result, "UnsupportedEncodingException"));
    }

    /**
     * KeywordWalkListenerRunner – normal flow **
     */
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

    /**
     * KeywordWalkListenerRunner – null fallback **
     */
    @Test
    void walkAtNode_NoKeywordWalkListenerRunner_DoesNotThrow() throws Exception {
        JsonSchema schema = buildSchema(mapper.createObjectNode());
        ExecutionContext ctx = mockExecutionContext(schema, null);
        JsonNode instance = JsonNodeFactory.instance.objectNode();
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/", true);
        assertTrue(result.isValid(), "Walk should succeed even when no listener runner is supplied");
    }

    /**
     * $ref resolution – malformed URI **
     */
    @Test
    void walkAtNode_MalformedRefUri_ReturnsInvalidResult() throws Exception {
        ObjectNode schemaNode = mapper.createObjectNode().put("$ref", "http://[invalid-uri]");
        JsonSchema schema = buildSchema(schemaNode);
        ExecutionContext ctx = mockExecutionContext(schema);
        JsonNode instance = JsonNodeFactory.instance.textNode("test");
        ValidationResult result = schema.walkAtNode(ctx, instance, instance, "/field", true);
        assertFalse(result.isValid(), "Malformed $ref URI should result in validation failure");
        assertTrue(containsMessageContaining(result, "URISyntaxException"));
    }

    /**
     * ExecutionContext null handling **
     */
    @Test
    void walkAtNode_NullExecutionContext_ReturnsInvalidResult() {
        JsonSchema schema = JsonSchema.from(mockValidationContext, "$", URI.create("http://example.com"), mapper.createObjectNode(), null, false);
        ValidationResult result = schema.walkAtNode(null, JsonNodeFactory.instance.objectNode(), JsonNodeFactory.instance.objectNode(), "/", true);
        assertFalse(result.isValid(), "Null ExecutionContext must be reported as an error");
        assertTrue(containsMessageContaining(result, "ExecutionContext"));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:7: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:11: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:21: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:22: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:24: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:28: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:31: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:34: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:37: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:50: error: cannot find symbol
        when(mockValidationContext.getMetaSchema()).thenReturn(mockMetaSchema);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:52: error: cannot find symbol
        when(mockValidationContext.getConfig()).thenReturn(null);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:65: error: cannot find symbol
        when(mockValidationContext.getMetaSchema()).thenReturn(mockMetaSchema);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:67: error: cannot find symbol
        when(mockValidationContext.getConfig()).thenReturn(null);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:82: error: cannot find symbol
        ExecutionContext ctx = mock(ExecutionContext.class);
                               ^
  symbol:   method mock(Class<ExecutionContext>)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:83: error: cannot find symbol
        when(ctx.getSchema()).thenReturn(schema);
                ^
  symbol:   method getSchema()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:85: error: cannot find symbol
        when(ctx.getKeywordWalkListenerRunner()).thenReturn(null);
                ^
  symbol:   method getKeywordWalkListenerRunner()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:93: error: cannot find symbol
        ExecutionContext ctx = mock(ExecutionContext.class);
                               ^
  symbol:   method mock(Class<ExecutionContext>)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:94: error: cannot find symbol
        when(ctx.getSchema()).thenReturn(schema);
                ^
  symbol:   method getSchema()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:95: error: cannot find symbol
        when(ctx.getKeywordWalkListenerRunner()).thenReturn(runner);
                ^
  symbol:   method getKeywordWalkListenerRunner()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:161: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "Expected no validation messages for an empty schema, but got: " + result.getMessages());
                                                                                                                            ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:161: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "Expected no validation messages for an empty schema, but got: " + result.getMessages());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:173: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "Even when validation is enabled, an empty schema should produce no messages.");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:186: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty(), "walkAtNode should handle null node gracefully and return an empty result.");
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:194: error: incompatible types: boolean cannot be converted to JsonSchema
        ValidationResult result = new JsonSchema(null, null, null, null, false).walkAtNode(null, null, null, "", false);
                                                                         ^
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:196: error: cannot find symbol
        assertFalse(result.isValid(), "Result must be invalid when everything is null");
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:197: error: cannot find symbol
        assertFalse(result.getMessages().isEmpty(), "A validation message should explain the problem");
                          ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:207: error: cannot find symbol
        assertFalse(result.isValid(), "Missing instance should produce an invalid result");
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:208: error: cannot find symbol
        assertTrue(containsMessageContaining(result, "instance is missing"));
                   ^
  symbol:   method containsMessageContaining(ValidationResult,String)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:221: error: cannot find symbol
        assertTrue(result.isValid(), "When validation flag is false the walk should be successful");
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:222: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:231: error: cannot find symbol
        assertTrue(result.isValid(), "Empty schema should always validate successfully");
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:244: error: cannot find symbol
        assertFalse(result.isValid(), "Missing required property must cause validation failure");
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:245: error: cannot find symbol
        assertTrue(containsMessageContaining(result, "required"));
                   ^
  symbol:   method containsMessageContaining(ValidationResult,String)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:254: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:255: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:267: error: cannot find symbol
        assertFalse(result.isValid(), "Type mismatch should be reported");
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:268: error: cannot find symbol
        assertTrue(containsMessageContaining(result, "type"));
                   ^
  symbol:   method containsMessageContaining(ValidationResult,String)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:277: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:278: error: cannot find symbol
        assertTrue(result.getMessages().isEmpty());
                         ^
  symbol:   method getMessages()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:290: error: cannot find symbol
        assertTrue(result.isValid(), "Dynamic anchors should not cause validation errors");
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:298: error: cannot find symbol
        ExecutionContext ctx = mock(ExecutionContext.class);
                               ^
  symbol:   method mock(Class<ExecutionContext>)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:299: error: cannot find symbol
        when(ctx.getSchema()).thenThrow(new UnsupportedEncodingException("UTF‑8 not supported"));
                ^
  symbol:   method getSchema()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:303: error: cannot find symbol
        assertFalse(result.isValid(), "Exception should be wrapped in an invalid ValidationResult");
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:304: error: cannot find symbol
        assertTrue(containsMessageContaining(result, "UnsupportedEncodingException"));
                   ^
  symbol:   method containsMessageContaining(ValidationResult,String)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:313: error: constructor DefaultKeywordWalkListenerRunner in class DefaultKeywordWalkListenerRunner cannot be applied to given types;
        WalkListenerRunner listener = spy(new DefaultKeywordWalkListenerRunner());
                                          ^
  required: Map<String,List<JsonSchemaWalkListener>>
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:318: error: cannot find symbol
        assertTrue(result.isValid());
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:319: error: cannot find symbol
        verify(listener, times(1)).beforeWalk(eq(schema), eq(instance), eq(instance), eq("/"));
                         ^
  symbol:   method times(int)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:319: error: cannot find symbol
        verify(listener, times(1)).beforeWalk(eq(schema), eq(instance), eq(instance), eq("/"));
                                              ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:319: error: cannot find symbol
        verify(listener, times(1)).beforeWalk(eq(schema), eq(instance), eq(instance), eq("/"));
                                                          ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:319: error: cannot find symbol
        verify(listener, times(1)).beforeWalk(eq(schema), eq(instance), eq(instance), eq("/"));
                                                                        ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:319: error: cannot find symbol
        verify(listener, times(1)).beforeWalk(eq(schema), eq(instance), eq(instance), eq("/"));
                                                                                      ^
  symbol:   method eq(String)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:320: error: cannot find symbol
        verify(listener, times(1)).afterWalk(eq(schema), eq(instance), eq(instance), eq("/"), any());
                         ^
  symbol:   method times(int)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:320: error: cannot find symbol
        verify(listener, times(1)).afterWalk(eq(schema), eq(instance), eq(instance), eq("/"), any());
                                             ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:320: error: cannot find symbol
        verify(listener, times(1)).afterWalk(eq(schema), eq(instance), eq(instance), eq("/"), any());
                                                         ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:320: error: cannot find symbol
        verify(listener, times(1)).afterWalk(eq(schema), eq(instance), eq(instance), eq("/"), any());
                                                                       ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:320: error: cannot find symbol
        verify(listener, times(1)).afterWalk(eq(schema), eq(instance), eq(instance), eq("/"), any());
                                                                                     ^
  symbol:   method eq(String)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:320: error: cannot find symbol
        verify(listener, times(1)).afterWalk(eq(schema), eq(instance), eq(instance), eq("/"), any());
                                                                                              ^
  symbol:   method any()
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:332: error: cannot find symbol
        assertTrue(result.isValid(), "Walk should succeed even when no listener runner is supplied");
                         ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:345: error: cannot find symbol
        assertFalse(result.isValid(), "Malformed $ref URI should result in validation failure");
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:346: error: cannot find symbol
        assertTrue(containsMessageContaining(result, "URISyntaxException"));
                   ^
  symbol:   method containsMessageContaining(ValidationResult,String)
  location: class JsonSchema_walkAtNode_Test
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:356: error: cannot find symbol
        assertFalse(result.isValid(), "Null ExecutionContext must be reported as an error");
                          ^
  symbol:   method isValid()
  location: variable result of type ValidationResult
src/test/java/com/networknt/schema/JsonSchema_walkAtNode_Test.java:357: error: cannot find symbol
        assertTrue(containsMessageContaining(result, "ExecutionContext"));
                   ^
  symbol:   method containsMessageContaining(ValidationResult,String)
  location: class JsonSchema_walkAtNode_Test
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
63 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'return type': 'com.networknt.schema.ValidationResult', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public ValidationResult walk(ExecutionContext, JsonNode, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walkAtNode(ExecutionContext, JsonNode, JsonNode, String, boolean)`, api document: \n     * Walk the JSON node\n     * @param executionContext     ExecutionContext\n     * @param node                 JsonNode\n     * @param shouldValidateSchema indicator on validation\n     *\n     * @return result of ValidationResult\n     ;\n\t\tmethod `public ValidationResult walkAtNode(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walkAtNode(ExecutionContext, JsonNode, JsonNode, String, boolean)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.walkAtNode(ExecutionContext, JsonNode, JsonNode, String, boolean)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


