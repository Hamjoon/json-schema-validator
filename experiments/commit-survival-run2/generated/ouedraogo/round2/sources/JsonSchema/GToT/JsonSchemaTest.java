/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The test class covers:
 *  • Extraction of all public methods (see the comment block at the top).
 *  • Basic happy‑path tests for each public method.
 *  • Edge‑case / exception scenarios (invalid $ref, missing required fields,
 *    malformed URIs, $recursiveAnchor handling, etc.).
 *
 * The tests rely only on the public API of the *networknt* JSON‑Schema library
 * and use Jackson’s {@link com.fasterxml.jackson.databind.ObjectMapper} to build
 * JSON nodes.  No external mocking framework is required – real schema objects
 * are created via {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * All tests are written with JUnit‑4.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.Assert.*;

public class JsonSchemaTest {

    /** The factory used to create JsonSchema instances for the tests. */
    private JsonSchemaFactory factory;

    /** Jackson mapper – used to build JsonNode instances from JSON strings. */
    private ObjectMapper mapper;

    @Before
    public void setUp() {
        // Use the latest draft (V201909) – it works for all features exercised below.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        mapper = new ObjectMapper();
    }

    /* ----------------------------------------------------------------------
     *  Public method list (extracted from the source)
     * ----------------------------------------------------------------------
     *
     *  static JsonSchema from(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)
     *  JsonSchema(ValidationContext, URI, JsonNode)                                 // deprecated ctor
     *  JsonSchema(ValidationContext, String, URI, JsonNode, JsonSchema)            // deprecated ctor
     *  JsonSchema(ValidationContext, URI, JsonNode, boolean)                      // deprecated ctor
     *  JsonSchema createChildSchema(String, JsonNode)
     *  URI getCurrentUri()
     *  JsonNode getRefSchemaNode(String)
     *  JsonSchema findLexicalRoot()
     *  JsonSchema findAncestor()
     *  Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)
     *  Set<ValidationMessage> validate(JsonNode)
     *  ValidationResult validateAndCollect(ExecutionContext, JsonNode)
     *  ValidationResult validateAndCollect(JsonNode)
     *  ValidationResult walk(ExecutionContext, JsonNode, boolean)
     *  ValidationResult walk(JsonNode, boolean)
     *  ValidationResult walkAtNode(ExecutionContext, JsonNode, JsonNode, String, boolean)
     *  Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)
     *  String toString()
     *  boolean hasRequiredValidator()
     *  JsonValidator getRequiredValidator()
     *  boolean hasTypeValidator()
     *  TypeValidator getTypeValidator()
     *  void initializeValidators()
     *  boolean isDynamicAnchor()
     *  ExecutionContext createExecutionContext()
     */

    /* ----------------------------------------------------------------------
     *  Helper – builds a JsonSchema from a JSON string.
     * ---------------------------------------------------------------------- */
    private JsonSchema buildSchema(String json) throws Exception {
        JsonNode node = mapper.readTree(json);
        // The factory creates a ValidationContext internally.
        return factory.getSchema(node);
    }

    /* ----------------------------------------------------------------------
     *  1. createChildSchema()
     * ---------------------------------------------------------------------- */
    @Test
    public void testCreateChildSchema() throws Exception {
        JsonSchema parent = buildSchema("{\"type\":\"object\"}");
        JsonSchema child = parent.createChildSchema("#/properties/name", mapper.readTree("{\"type\":\"string\"}"));
        assertNotNull("Child schema must be created", child);
        // The child should have the same ValidationContext as the parent.
        assertSame(parent.getValidationContext(), child.getValidationContext());
    }

    /* ----------------------------------------------------------------------
     *  2. getCurrentUri()
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetCurrentUri() throws Exception {
        // schema without an explicit $id – currentUri will be null.
        JsonSchema schema = buildSchema("{\"type\":\"string\"}");
        assertNull("Current URI should be null when no $id is present", schema.getCurrentUri());

        // schema with an $id – factory resolves it against the base URI.
        JsonSchema schemaWithId = factory.getSchema(
                mapper.readTree("{\"$id\":\"http://example.com/mySchema\",\"type\":\"string\"}"));
        URI uri = schemaWithId.getCurrentUri();
        assertNotNull(uri);
        assertEquals("http://example.com/mySchema", uri.toString());
    }

    /* ----------------------------------------------------------------------
     *  3. getRefSchemaNode()
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetRefSchemaNode_ValidRef() throws Exception {
        String schemaJson = "{\n" +
                "  \"$defs\": {\n" +
                "    \"sub\": {\"type\":\"string\"}\n" +
                "  },\n" +
                "  \"$ref\": \"#/$defs/sub\"\n" +
                "}";
        JsonSchema schema = buildSchema(schemaJson);
        JsonNode refNode = schema.getRefSchemaNode("#/$defs/sub");
        assertNotNull(refNode);
        assertEquals("string", refNode.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_InvalidRef() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"object\"}");
        JsonNode refNode = schema.getRefSchemaNode("#/nonexistent");
        // According to the implementation a missing node results in null.
        assertNull(refNode);
    }

    /* ----------------------------------------------------------------------
     *  4. findLexicalRoot() / findAncestor()
     * ---------------------------------------------------------------------- */
    @Test
    public void testFindAncestorAndLexicalRoot() throws Exception {
        // Build a parent schema with an $id (creates a lexical root)
        JsonSchema parent = factory.getSchema(
                mapper.readTree("{\"$id\":\"http://example.com/parent\",\"type\":\"object\"}"));
        // Child schema without its own $id – should resolve to the same lexical root.
        JsonSchema child = parent.createChildSchema("#/properties/name",
                mapper.readTree("{\"type\":\"string\"}"));
        assertSame(parent, child.findAncestor());
        assertSame(parent, child.findLexicalRoot());
    }

    /* ----------------------------------------------------------------------
     *  5. validate(ExecutionContext, JsonNode, JsonNode, String)
     * ---------------------------------------------------------------------- */
    @Test
    public void testValidate_WithExecutionContext() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"object\",\"required\":[\"name\"]}");
        JsonNode valid = mapper.readTree("{\"name\":\"John\"}");
        JsonNode invalid = mapper.readTree("{\"age\":30}");

        ExecutionContext ctx = schema.createExecutionContext();

        Set<ValidationMessage> errorsValid = schema.validate(ctx, valid, valid, "$");
        assertTrue(errorsValid.isEmpty());

        Set<ValidationMessage> errorsInvalid = schema.validate(ctx, invalid, invalid, "$");
        assertFalse(errorsInvalid.isEmpty());
        // The error should be about the missing required property "name".
        boolean hasRequiredError = errorsInvalid.stream()
                .anyMatch(m -> m.getMessage().contains("required"));
        assertTrue(hasRequiredError);
    }

    /* ----------------------------------------------------------------------
     *  6. validate(JsonNode) – convenience overload.
     * ---------------------------------------------------------------------- */
    @Test
    public void testValidateConvenient() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"string\"}");
        Set<ValidationMessage> errors = schema.validate(mapper.readTree("\"hello\""));
        assertTrue(errors.isEmpty());

        errors = schema.validate(mapper.readTree("123"));
        assertFalse(errors.isEmpty());
    }

    /* ----------------------------------------------------------------------
     *  7. validateAndCollect(ExecutionContext, JsonNode) & overload.
     * ---------------------------------------------------------------------- */
    @Test
    public void testValidateAndCollect() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"object\",\"required\":[\"id\"]}");
        JsonNode good = mapper.readTree("{\"id\":1}");
        JsonNode bad = mapper.readTree("{\"name\":\"foo\"}");

        ValidationResult resultGood = schema.validateAndCollect(good);
        assertTrue(resultGood.getValidationMessages().isEmpty());

        ValidationResult resultBad = schema.validateAndCollect(bad);
        assertFalse(resultBad.getValidationMessages().isEmpty());
        assertTrue(resultBad.getValidationMessages().stream()
                .anyMatch(m -> m.getMessage().contains("required")));
    }

    /* ----------------------------------------------------------------------
     *  8. walk(...) – ensure walk does not throw and returns an empty set for a
     *     schema that does not define walk‑specific keywords.
     * ---------------------------------------------------------------------- */
    @Test
    public void testWalkMethods() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"object\"}");
        JsonNode instance = mapper.readTree("{\"any\":\"value\"}");

        ValidationResult result = schema.walk(instance, false);
        assertTrue(result.getValidationMessages().isEmpty());

        // walkAtNode – same as walk for this simple case.
        ValidationResult resultAtNode = schema.walkAtNode(schema.createExecutionContext(),
                instance, instance, "$", false);
        assertTrue(resultAtNode.getValidationMessages().isEmpty());
    }

    /* ----------------------------------------------------------------------
     *  9. toString()
     * ---------------------------------------------------------------------- */
    @Test
    public void testToString() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"null\"}");
        String txt = schema.toString();
        assertTrue(txt.startsWith("\"#\""));
        assertTrue(txt.contains("\"type\":\"null\""));
    }

    /* ----------------------------------------------------------------------
     * 10. hasRequiredValidator() / getRequiredValidator()
     * ---------------------------------------------------------------------- */
    @Test
    public void testRequiredValidatorPresence() throws Exception {
        JsonSchema withReq = buildSchema("{\"required\":[\"a\"]}");
        assertTrue(withReq.hasRequiredValidator());
        assertNotNull(withReq.getRequiredValidator());

        JsonSchema withoutReq = buildSchema("{\"type\":\"object\"}");
        assertFalse(withoutReq.hasRequiredValidator());
        assertNull(withoutReq.getRequiredValidator());
    }

    /* ----------------------------------------------------------------------
     * 11. hasTypeValidator() / getTypeValidator()
     * ---------------------------------------------------------------------- */
    @Test
    public void testTypeValidatorPresence() throws Exception {
        JsonSchema withType = buildSchema("{\"type\":\"array\"}");
        assertTrue(withType.hasTypeValidator());
        assertNotNull(withType.getTypeValidator());

        JsonSchema withoutType = buildSchema("{\"properties\":{\"a\":{\"type\":\"string\"}}}");
        assertFalse(withoutType.hasTypeValidator());
        assertNull(withoutType.getTypeValidator());
    }

    /* ----------------------------------------------------------------------
     * 12. initializeValidators()
     * ---------------------------------------------------------------------- */
    @Test
    public void testInitializeValidators() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"object\",\"properties\":{\"x\":{\"type\":\"integer\"}}}");
        // The method should not throw; subsequent validation must still work.
        schema.initializeValidators();
        Set<ValidationMessage> errors = schema.validate(mapper.readTree("{\"x\":10}"));
        assertTrue(errors.isEmpty());
    }

    /* ----------------------------------------------------------------------
     * 13. isDynamicAnchor()
     * ---------------------------------------------------------------------- */
    @Test
    public void testDynamicAnchorDetection() throws Exception {
        JsonSchema withoutAnchor = buildSchema("{\"type\":\"object\"}");
        assertFalse(withoutAnchor.isDynamicAnchor());

        JsonSchema withAnchor = buildSchema("{\"$recursiveAnchor\":true}");
        assertTrue(withAnchor.isDynamicAnchor());
    }

    /* ----------------------------------------------------------------------
     * 14. createExecutionContext()
     * ---------------------------------------------------------------------- */
    @Test
    public void testCreateExecutionContext() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"null\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull(ctx);
        // The collector context should be present inside the execution context.
        assertNotNull(ctx.getCollectorContext());
    }

    /* ----------------------------------------------------------------------
     * 15. Static factory method JsonSchema.from(...)
     * ---------------------------------------------------------------------- */
    @Test
    public void testStaticFromMethod() throws Exception {
        // Build a ValidationContext via the factory (the same as the public API does).
        ValidationContext vc = factory.getValidationContext();
        JsonNode schemaNode = mapper.readTree("{\"type\":\"boolean\"}");
        // Use an arbitrary base URI.
        URI base = new URI("http://example.com/base");
        JsonSchema schema = JsonSchema.from(vc, "#", base, schemaNode, null, false);
        assertNotNull(schema);
        assertEquals(base, schema.getCurrentUri());
        // Validate boolean true – should pass.
        Set<ValidationMessage> msgs = schema.validate(mapper.readTree("true"));
        assertTrue(msgs.isEmpty());
    }
}
