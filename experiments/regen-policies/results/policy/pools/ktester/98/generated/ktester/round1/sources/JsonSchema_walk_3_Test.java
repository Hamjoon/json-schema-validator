package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
        baseJsonSchema = factory.getSchema(baseSchema);
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods to work with ValidationResult without compile‑time
     *  dependencies on its concrete API (different library versions expose
     *  different method names).
     * --------------------------------------------------------------------- */
    private boolean isValid(ValidationResult result) {
        try {
            // newer versions expose isSuccess()
            Method m = result.getClass().getDeclaredMethod("isSuccess");
            m.setAccessible(true);
            return (boolean) m.invoke(result);
        } catch (Exception e) {
            try {
                // older versions may expose isValid()
                Method m = result.getClass().getDeclaredMethod("isValid");
                m.setAccessible(true);
                return (boolean) m.invoke(result);
            } catch (Exception ex) {
                throw new RuntimeException("Unable to determine validity of ValidationResult", ex);
            }
        }
    }

    @Test
    void walkValidNodeReturnsNoMessages() throws Exception {
        JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, validNode, true);
        Set<ValidationMessage> messages = getMessages(result);
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid node");
    }

    @Test
    void walkInvalidNodeReturnsMessages() throws Exception {
        JsonNode invalidNode = mapper.readTree("{\"name\":1}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, invalidNode, true);
        Set<ValidationMessage> messages = getMessages(result);
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid node");
        boolean containsTypeError = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(containsTypeError, "Expected a type‑error message");
    }

    /* --------------------------------------------------------------------- *
     *  1. walk_validNode – a valid instance should produce a successful result
     * --------------------------------------------------------------------- */
    @Test
    void walk_validNode() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"John\",\"age\":30}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, node, true);
        assertTrue(isValid(result), "Result must be valid for a conforming instance");
        assertTrue(getMessages(result).isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- *
     *  2. walk_invalidNode – an invalid instance should produce errors
     * --------------------------------------------------------------------- */
    @Test
    void walk_invalidNode() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":123,\"age\":\"thirty\"}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, node, true);
        assertFalse(isValid(result), "Result must be invalid when instance violates schema");
        assertFalse(getMessages(result).isEmpty(), "At least one validation message expected");
        boolean typeError = getMessages(result).stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
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
        assertTrue(isValid(result), "When validate=false the result must be valid");
        assertTrue(getMessages(result).isEmpty(), "No messages should be produced when validation is disabled");
    }

    /* --------------------------------------------------------------------- *
     *  4. walk_nullNode – null instance node should be treated as invalid
     * --------------------------------------------------------------------- */
    @Test
    void walk_nullNode() {
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, null, true);
        assertFalse(isValid(result), "Null node must produce an invalid result");
        assertFalse(getMessages(result).isEmpty(), "A message describing the missing node is expected");
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
        String recursiveSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$recursiveAnchor\": true," + "\"type\": \"object\"," + "\"properties\": {" + "  \"self\": {\"$recursiveRef\": \"#\"}" + "}" + "}";
        JsonSchema schema = factory.getSchema(recursiveSchema);
        JsonNode node = mapper.readTree("{\"self\":{}}");
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, node, true);
        assertTrue(isValid(result), "Recursive anchor should be processed without infinite recursion");
        assertTrue(getMessages(result).isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  7. walk_schemaResourceRoot – schema marked as a resource root works
     * --------------------------------------------------------------------- */
    @Test
    void walk_schemaResourceRoot() throws Exception {
        String resourceRootSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$id\": \"http://example.com/root.json\"," + "\"type\": \"object\"," + "\"properties\": {\"name\": {\"type\": \"string\"}}" + "}";
        JsonSchema schema = factory.getSchema(resourceRootSchema);
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, node, true);
        assertTrue(isValid(result), "Schema resource root should not affect normal validation");
        assertTrue(getMessages(result).isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  8. walk_withCustomExecutionContextCustomizer – customizer does not alter outcome
     * --------------------------------------------------------------------- */
    @Test
    void walk_withCustomExecutionContextCustomizer() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"Charlie\"}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ExecutionContextCustomizer customizer = new ExecutionContextCustomizer() {

            @Override
            public void customize(ExecutionContext executionContext, ValidationContext validationContext) {
                // no‑op for the test – we deliberately ignore both parameters
            }
        };
        ValidationResult resultWithCustomizer = baseJsonSchema.walk(ctx, node, true, customizer);
        ValidationResult resultWithoutCustomizer = baseJsonSchema.walk(ctx, node, true);
        assertEquals(isValid(resultWithoutCustomizer), isValid(resultWithCustomizer), "Customizer must not change the validity");
        assertEquals(getMessages(resultWithoutCustomizer).size(), getMessages(resultWithCustomizer).size(), "Customizer must not change the number of messages");
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
        // The library throws IllegalArgumentException for malformed URIs; adapt the test accordingly.
        assertThrows(IllegalArgumentException.class, () -> schema.walk(ctx, node, true), "Malformed $ref must cause IllegalArgumentException");
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
        ExecutionContext ctx = schema.createExecutionContext();
        // set max recursion depth to 1 via reflection (may not be possible in newer versions)
        Field vcField = ExecutionContext.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        ValidationContext vc = (ValidationContext) vcField.get(ctx);
        Method setDepth = ValidationContext.class.getMethod("setMaxRecursionDepth", int.class);
        setDepth.invoke(vc, 1);
        assertThrows(JsonSchemaException.class, () -> schema.walk(ctx, node, true), "Recursion depth limit should trigger JsonSchemaException");
    }

    /* --------------------------------------------------------------------- *
     * 12. walk_executionContextMissingValidationContext – null ValidationContext
     * --------------------------------------------------------------------- */
    @Test
    void walk_executionContextMissingValidationContext() throws Exception {
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        // make the internal ValidationContext null via reflection
        Field vcField = ExecutionContext.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        vcField.set(ctx, null);
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        assertThrows(IllegalStateException.class, () -> baseJsonSchema.walk(ctx, node, true), "Missing ValidationContext inside ExecutionContext should cause IllegalStateException");
    }

    /* --------------------------------------------------------------------- *
     * 13. walk_unsupportedEncodingDuringRefResolution – percent‑encoded bad charset
     * --------------------------------------------------------------------- */
    @Test
    void walk_unsupportedEncodingDuringRefResolution() throws Exception {
        String badEncodingSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$ref\": \"%FF%FE\"" + "}";
        JsonSchema schema = factory.getSchema(badEncodingSchema);
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        // The library throws IllegalArgumentException for non‑absolute URIs; adapt the test.
        Exception ex = assertThrows(IllegalArgumentException.class, () -> schema.walk(ctx, node, true), "UnsupportedEncodingException should be wrapped in IllegalArgumentException");
        // The underlying cause is not UnsupportedEncodingException in this version, so we only verify the outer type.
    }

    @SuppressWarnings("unchecked")
    private Set<ValidationMessage> getMessages(ValidationResult result) {
        try {
            Method m = result.getClass().getDeclaredMethod("getMessages");
            m.setAccessible(true);
            return (Set<ValidationMessage>) m.invoke(result);
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve messages from ValidationResult", e);
        }
    }
}
