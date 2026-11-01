package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import java.io.UnsupportedEncodingException;

class JsonSchema_walk_3_Test {

    private static JsonSchemaFactory factory;

    private static JsonSchema schema;

    private static ObjectMapper mapper;

    // reused in many tests
    private static JsonSchema baseJsonSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // a simple base schema – an object with a required string property "name"
        String baseSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"type\": \"object\"," + "\"properties\": {" + "    \"name\": {\"type\": \"string\"}" + "}," + "\"required\": [\"name\"]" + "}";
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        mapper = new ObjectMapper();
        // the base schema is used by several tests; we keep a reference for convenience
        baseJsonSchema = factory.getSchema(baseSchema);
    }

    @Test
    void walkValidNodeReturnsNoMessages() throws Exception {
        // given: a valid JSON instance {"name":"John"}
        JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");
        // when: walk is invoked with a fresh ExecutionContext and validation enabled
        ExecutionContext ctx = new ExecutionContext();
        ValidationResult result = schema.walk(ctx, validNode, true);
        // then: the result should contain no validation messages
        Set<ValidationMessage> messages = result.getMessages();
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid node");
    }

    @Test
    void walkInvalidNodeReturnsMessages() throws Exception {
        // given: an invalid JSON instance {"name":1}
        JsonNode invalidNode = mapper.readTree("{\"name\":1}");
        // when: walk is invoked with a fresh ExecutionContext and validation enabled
        ExecutionContext ctx = new ExecutionContext();
        ValidationResult result = schema.walk(ctx, invalidNode, true);
        // then: the result should contain at least one validation message
        Set<ValidationMessage> messages = result.getMessages();
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid node");
        // optional: verify the message mentions the type mismatch
        boolean containsTypeError = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(containsTypeError, "Expected a type‑error message");
    }

    /* --------------------------------------------------------------------- *
     *  1. walk_validNode – a valid instance should produce a successful result
     * --------------------------------------------------------------------- */
    @Test
    void walk_validNode() throws Exception {
        // given
        JsonNode node = mapper.readTree("{\"name\":\"John\",\"age\":30}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        // when
        ValidationResult result = baseJsonSchema.walk(ctx, node, true);
        // then
        assertTrue(result.isValid(), "Result must be valid for a conforming instance");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- *
     *  2. walk_invalidNode – an invalid instance should produce errors
     * --------------------------------------------------------------------- */
    @Test
    void walk_invalidNode() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":123,\"age\":\"thirty\"}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, node, true);
        assertFalse(result.isValid(), "Result must be invalid when instance violates schema");
        assertFalse(result.getMessages().isEmpty(), "At least one validation message expected");
        boolean typeError = result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(typeError, "A type‑error message should be present");
    }

    /* --------------------------------------------------------------------- *
     *  3. walk_validateFlagFalse – validation is skipped, always valid
     * --------------------------------------------------------------------- */
    @Test
    void walk_validateFlagFalse() throws Exception {
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, node, false);
        assertTrue(result.isValid(), "When validate=false the result must be valid");
        assertTrue(result.getMessages().isEmpty(), "No messages should be produced when validation is disabled");
    }

    /* --------------------------------------------------------------------- *
     *  4. walk_nullNode – null instance node should be treated as invalid
     * --------------------------------------------------------------------- */
    @Test
    void walk_nullNode() {
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, null, true);
        assertFalse(result.isValid(), "Null node must produce an invalid result");
        assertFalse(result.getMessages().isEmpty(), "A message describing the missing node is expected");
    }

    /* --------------------------------------------------------------------- *
     *  5. walk_nullExecutionContext – passing null should throw NPE
     * --------------------------------------------------------------------- */
    @Test
    void walk_nullExecutionContext() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        assertThrows(NullPointerException.class, () -> baseJsonSchema.walk(null, node, true), "A null ExecutionContext should cause NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  6. walk_recursiveAnchor_true – schema with $recursiveAnchor resolves $recursiveRef
     * --------------------------------------------------------------------- */
    @Test
    void walk_recursiveAnchor_true() throws Exception {
        // schema with a recursive anchor and a $recursiveRef that points to itself
        String recursiveSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$recursiveAnchor\": true," + "\"type\": \"object\"," + "\"properties\": {" + "  \"self\": {\"$recursiveRef\": \"#\"}" + "}" + "}";
        JsonSchema schema = factory.getSchema(recursiveSchema);
        // simple self‑reference
        JsonNode node = mapper.readTree("{\"self\":{}}");
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, node, true);
        assertTrue(result.isValid(), "Recursive anchor should be processed without infinite recursion");
        assertTrue(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  7. walk_schemaResourceRoot – schema marked as a resource root works
     * --------------------------------------------------------------------- */
    @Test
    void walk_schemaResourceRoot() throws Exception {
        // a schema that is a resource root (has an $id)
        String resourceRootSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$id\": \"http://example.com/root.json\"," + "\"type\": \"object\"," + "\"properties\": {\"name\": {\"type\": \"string\"}}" + "}";
        JsonSchema schema = factory.getSchema(resourceRootSchema);
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, node, true);
        assertTrue(result.isValid(), "Schema resource root should not affect normal validation");
        assertTrue(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  8. walk_withCustomExecutionContextCustomizer – customizer does not alter outcome
     * --------------------------------------------------------------------- */
    @Test
    void walk_withCustomExecutionContextCustomizer() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"Charlie\"}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        // customizer that simply adds a dummy attribute – it should not affect validation
        ExecutionContextCustomizer customizer = executionContext -> {
            // no‑op for the test
        };
        ValidationResult resultWithCustomizer = baseJsonSchema.walk(ctx, node, true, customizer);
        ValidationResult resultWithoutCustomizer = baseJsonSchema.walk(ctx, node, true);
        assertEquals(resultWithoutCustomizer.isValid(), resultWithCustomizer.isValid(), "Customizer must not change the validity");
        assertEquals(resultWithoutCustomizer.getMessages().size(), resultWithCustomizer.getMessages().size(), "Customizer must not change the number of messages");
    }

    /* --------------------------------------------------------------------- *
     *  9. walk_invalidRefSyntax – malformed $ref should raise an exception
     * --------------------------------------------------------------------- */
    @Test
    void walk_invalidRefSyntax() throws Exception {
        String badRefSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$ref\": \"http://[invalid-uri]\"" + "}";
        JsonSchema schema = factory.getSchema(badRefSchema);
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(JsonSchemaException.class, () -> schema.walk(ctx, node, true), "Malformed $ref must cause JsonSchemaException");
    }

    /* --------------------------------------------------------------------- *
     * 10. walk_unresolvableRef – $ref to a non‑existent definition should fail
     * --------------------------------------------------------------------- */
    @Test
    void walk_unresolvableRef() throws Exception {
        String unresolvableRefSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$ref\": \"#/components/schemas/Unknown\"" + "}";
        JsonSchema schema = factory.getSchema(unresolvableRefSchema);
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(JsonSchemaException.class, () -> schema.walk(ctx, node, true), "Unresolvable $ref must raise JsonSchemaException");
    }

    /* --------------------------------------------------------------------- *
     * 11. walk_recursiveAnchorDepthExceeded – low recursion depth should be detected
     * --------------------------------------------------------------------- */
    @Test
    void walk_recursiveAnchorDepthExceeded() throws Exception {
        String recursiveSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$recursiveAnchor\": true," + "\"type\": \"object\"," + "\"properties\": {" + "  \"self\": {\"$recursiveRef\": \"#\"}" + "}" + "}";
        JsonSchema schema = factory.getSchema(recursiveSchema);
        JsonNode node = mapper.readTree("{\"self\":{}}");
        // create an ExecutionContext and limit recursion depth to 1
        ExecutionContext ctx = schema.createExecutionContext();
        // the internal ValidationContext is not part of the public API; we use reflection
        // to set the max recursion depth for the purpose of this test.
        java.lang.reflect.Field vcField = ExecutionContext.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        ValidationContext vc = (ValidationContext) vcField.get(ctx);
        java.lang.reflect.Method setDepth = ValidationContext.class.getMethod("setMaxRecursionDepth", int.class);
        setDepth.invoke(vc, 1);
        assertThrows(JsonSchemaException.class, () -> schema.walk(ctx, node, true), "Recursion depth limit should trigger JsonSchemaException");
    }

    /* --------------------------------------------------------------------- *
     * 12. walk_executionContextMissingValidationContext – null ValidationContext
     * --------------------------------------------------------------------- */
    @Test
    void walk_executionContextMissingValidationContext() throws Exception {
        // mock ExecutionContext so that getValidationContext() returns null
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        when(mockCtx.getValidationContext()).thenReturn(null);
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        assertThrows(IllegalStateException.class, () -> baseJsonSchema.walk(mockCtx, node, true), "Missing ValidationContext inside ExecutionContext should cause IllegalStateException");
    }

    /* --------------------------------------------------------------------- *
     * 13. walk_unsupportedEncodingDuringRefResolution – percent‑encoded bad charset
     * --------------------------------------------------------------------- */
    @Test
    void walk_unsupportedEncodingDuringRefResolution() throws Exception {
        // schema with a $ref that contains an illegal percent‑encoding sequence
        String badEncodingSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$ref\": \"%FF%FE\"" + "}";
        JsonSchema schema = factory.getSchema(badEncodingSchema);
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        Exception ex = assertThrows(JsonSchemaException.class, () -> schema.walk(ctx, node, true), "UnsupportedEncodingException should be wrapped in JsonSchemaException");
        // verify that the cause is indeed UnsupportedEncodingException
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException, "Root cause must be UnsupportedEncodingException");
    }
}
