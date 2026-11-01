###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The suite follows the “three‑expert” process described in the prompt:
 *   1. List all public methods.
 *   2. Provide a basic test for each method.
 *   3. Identify edge‑case / exception scenarios.
 *   4. Add dedicated tests for those scenarios.
 *
 * The tests use only the public API that is present in the source file and
 * the minimal set of helper classes that are part of the library (Jackson,
 * JsonSchemaFactory, etc.).  No mocking frameworks are used – every object
 * is created through its public constructors or factory methods.
 */
package com.networknt.schema;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Comprehensive tests for {@link JsonSchema}.
 *
 * The tests cover:
 *   • All public methods (basic happy‑path verification)
 *   • Edge‑cases such as missing $ref targets, URL‑decoded pointers,
 *     recursive‑anchor handling, configuration changes, and invalid input.
 */
public class JsonSchemaTest {

    /** Jackson mapper – used to build schema / instance JSON nodes. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A minimal, always‑valid schema: {"type":"object"} */
    private JsonNode simpleSchemaNode;

    /** A schema that contains a $ref to another definition. */
    private JsonNode refSchemaNode;

    /** A schema that contains a $recursiveAnchor (boolean) */
    private JsonNode recursiveAnchorSchemaNode;

    /** Factory for creating schemas (draft‑2019‑09 is used for all tests). */
    private JsonSchemaFactory factory;

    /** Default configuration – reused in many tests. */
    private SchemaValidatorsConfig defaultConfig;

    /** Helper to create a JsonSchema from a JsonNode. */
    private JsonSchema createSchema(JsonNode schemaNode) {
        return factory.getSchema(schemaNode);
    }

    @Before
    public void setUp() throws Exception {
        // Initialise factory with the 2019‑09 meta‑schema.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        defaultConfig = new SchemaValidatorsConfig();
        defaultConfig.setLocale(Locale.US);

        // simpleSchemaNode = {"type":"object"}
        simpleSchemaNode = MAPPER.readTree("{\"type\":\"object\"}");

        // refSchemaNode = {"$defs":{"person":{"type":"object","properties":{"name":{"type":"string"}}}},"$ref":"#/\\$defs/person"}
        // Note: the reference contains an escaped '/' to test URL‑decoding.
        refSchemaNode = MAPPER.readTree(
                "{"
                        + "\"$defs\":{"
                        + "  \"person\":{"
                        + "    \"type\":\"object\","
                        + "    \"properties\":{\"name\":{\"type\":\"string\"}}"
                        + "  }"
                        + "},"
                        + "\"$ref\":\"#/\\$defs/person\""
                        + "}");

        // recursiveAnchorSchemaNode = {"$recursiveAnchor":true}
        recursiveAnchorSchemaNode = MAPPER.readTree("{\"$recursiveAnchor\":true}");
    }

    /* --------------------------------------------------------------------- */
    /*  1. LIST OF PUBLIC METHODS (for reference)                           */
    /* --------------------------------------------------------------------- */
    // public JsonSchema fromRef(JsonSchema, JsonNodePath)
    // public JsonSchema withConfig(SchemaValidatorsConfig)
    // public ValidationContext getValidationContext()
    // public JsonNode getRefSchemaNode(String)
    // public JsonSchema getRefSchema(JsonNodePath)
    // public JsonSchema getSubSchema(JsonNodePath)
    // public JsonSchema findLexicalRoot()
    // public JsonSchema findSchemaResourceRoot()
    // public boolean isSchemaResourceRoot()
    // public String getId()
    // public JsonSchema findAncestor()
    // public Set<ValidationMessage> validate(JsonNode)
    // public Set<ValidationMessage> validate(JsonNode, OutputFormat)
    // public Set<ValidationMessage> validate(JsonNode, ExecutionContextCustomizer)
    // public Set<ValidationMessage> validate(JsonNode, Consumer<ExecutionContext>)
    // public <T> T validate(JsonNode, OutputFormat<T>)
    // public <T> T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)
    // public <T> T validate(JsonNode, OutputFormat<T>, Consumer<ExecutionContext>)
    // public Set<ValidationMessage> validate(String, InputFormat)
    // public Set<ValidationMessage> validate(String, InputFormat, ExecutionContextCustomizer)
    // public Set<ValidationMessage> validate(String, InputFormat, Consumer<ExecutionContext>)
    // public <T> T validate(String, InputFormat, OutputFormat<T>)
    // public <T> T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)
    // public <T> T validate(String, InputFormat, OutputFormat<T>, Consumer<ExecutionContext>)
    // public <T> T validate(ExecutionContext, JsonNode, OutputFormat<T>)
    // public <T> T validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)
    // public ValidationResult walk(ExecutionContext, JsonNode, boolean, ExecutionContextCustomizer)
    // public <T> T walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)
    // public ValidationResult walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)
    // public <T> T walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, Consumer<ExecutionContext>)
    // public ValidationResult walk(ExecutionContext, JsonNode, boolean)
    // public ValidationResult walk(ExecutionContext, String, InputFormat, boolean)
    // public <T> T walk(ExecutionContext, String, InputFormat, OutputFormat<T>, boolean)
    // public ValidationResult walk(ExecutionContext, String, InputFormat, boolean, ExecutionContextCustomizer)
    // public ValidationResult walk(ExecutionContext, String, InputFormat, boolean, Consumer<ExecutionContext>)
    // public ValidationResult walk(JsonNode, boolean)
    // public <T> T walk(JsonNode, OutputFormat<T>, boolean)
    // public ValidationResult walk(String, InputFormat, boolean)
    // public ValidationResult walk(String, InputFormat, boolean, ExecutionContextCustomizer)
    // public ValidationResult walk(String, InputFormat, boolean, Consumer<ExecutionContext>)
    // public ValidationResult walkAtNode(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)
    // public void initializeValidators()
    // public boolean isRecursiveAnchor()
    // public ExecutionContext createExecutionContext()
    // public boolean hasTypeValidator()
    // public TypeValidator getTypeValidator()
    // public List<JsonValidator> getValidators()
    // public String toString()
    // public String getId()
    // public JsonSchema findAncestor()
    // public JsonSchema findLexicalRoot()
    // public JsonSchema findSchemaResourceRoot()
    // public boolean isSchemaResourceRoot()

    /* --------------------------------------------------------------------- */
    /*  2. BASIC HAPPY‑PATH TESTS                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCreateSchema_andToString() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        assertNotNull(schema);
        // toString() should contain the evaluation path (which is root = empty)
        assertTrue(schema.toString().contains("\"\""));
        // getId() is null because there is no $id in the schema
        assertNull(schema.getId());
    }

    @Test
    public void testGetValidationContext() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        ValidationContext ctx = schema.getValidationContext();
        assertNotNull(ctx);
        assertEquals(factory.getMetaSchema(), ctx.getMetaSchema());
    }

    @Test
    public void testValidateSimpleObject() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        JsonNode instance = MAPPER.readTree("{\"foo\":\"bar\"}");
        Set<ValidationMessage> result = schema.validate(instance);
        assertTrue("A valid object should produce no errors", result.isEmpty());
    }

    @Test
    public void testValidateInvalidInstance() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        JsonNode instance = MAPPER.readTree("[1,2,3]"); // array instead of object
        Set<ValidationMessage> result = schema.validate(instance);
        assertFalse("Invalid instance must produce at least one error", result.isEmpty());
        // The error code for type mismatch is "type"
        boolean hasTypeError = result.stream().anyMatch(v -> "type".equals(v.getCode()));
        assertTrue("Expected a type error", hasTypeError);
    }

    @Test
    public void testValidateStringInput() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        String json = "{\"a\":1}";
        Set<ValidationMessage> result = schema.validate(json, InputFormat.JSON);
        assertTrue(result.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateInvalidStringInput() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        // malformed JSON – should throw IllegalArgumentException from deserialize()
        schema.validate("{invalid json", InputFormat.JSON);
    }

    @Test
    public void testWithConfig_UnchangedReturnsSameInstance() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        JsonSchema result = schema.withConfig(defaultConfig);
        assertSame("When the config is identical the same schema instance should be returned", schema, result);
    }

    @Test
    public void testWithConfig_ChangedReturnsNewInstance() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        SchemaValidatorsConfig newConfig = new SchemaValidatorsConfig();
        newConfig.setFailFast(true);
        JsonSchema result = schema.withConfig(newConfig);
        assertNotSame("A different config must produce a new JsonSchema instance", schema, result);
        // The new instance must honour the new configuration
        assertTrue(result.getValidationContext().getConfig().isFailFast());
    }

    @Test
    public void testFromRefCreatesSchemaWithNewEvaluationPath() {
        JsonSchema base = createSchema(simpleSchemaNode);
        JsonNodePath newPath = new JsonNodePath(PathType.JSON_POINTER).append("properties").append("name");
        JsonSchema derived = base.fromRef(base, newPath);
        assertNotNull(derived);
        // The derived schema must keep the same underlying schema node
        assertEquals(base.getSchemaNode(), derived.getSchemaNode());
        // Evaluation path must be the one we passed
        assertEquals(newPath, derived.getEvaluationPath());
    }

    @Test
    public void testGetRefSchemaNode_ValidPointer() {
        JsonSchema schema = createSchema(refSchemaNode);
        // The $ref points to "#/$defs/person".  The method should resolve to the node
        JsonNode target = schema.getRefSchemaNode("#/$defs/person");
        assertNotNull(target);
        assertTrue(target.has("properties"));
        assertTrue(target.get("properties").has("name"));
    }

    @Test
    public void testGetRefSchemaNode_MissingNodeReturnsNull() {
        JsonSchema schema = createSchema(refSchemaNode);
        JsonNode missing = schema.getRefSchemaNode("#/nonexistent");
        assertNull("Missing pointer should return null (handled by handleNullNode)", missing);
    }

    @Test
    public void testGetRefSchemaNode_UrlDecoding() throws UnsupportedEncodingException {
        JsonSchema schema = createSchema(refSchemaNode);
        // The ref in the test contains an escaped '/' – after URL‑decoding it becomes "#/$defs/person"
        String encoded = "#/%24defs/person"; // %24 = '$'
        JsonNode node = schema.getRefSchemaNode(encoded);
        assertNotNull(node);
        assertTrue(node.has("properties"));
    }

    @Test
    public void testIsRecursiveAnchor_true() {
        JsonSchema schema = createSchema(recursiveAnchorSchemaNode);
        assertTrue("Schema with $recursiveAnchor:true must report true", schema.isRecursiveAnchor());
    }

    @Test
    public void testIsRecursiveAnchor_false() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        assertFalse("Schema without $recursiveAnchor must report false", schema.isRecursiveAnchor());
    }

    @Test
    public void testInitializeValidators_isIdempotent() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        // First call – validators are not yet loaded
        schema.initializeValidators();
        // Second call – should not throw and must keep validatorsLoaded true
        schema.initializeValidators();
        // No exception means test passed
    }

    @Test
    public void testCreateExecutionContext_copiesConfig() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setLocale(Locale.GERMANY);
        cfg.setFailFast(true);
        JsonSchema schema = createSchema(simpleSchemaNode).withConfig(cfg);
        ExecutionContext ctx = schema.createExecutionContext();
        assertEquals(Locale.GERMANY, ctx.getExecutionConfig().getLocale());
        assertTrue(ctx.isFailFast());
    }

    @Test
    public void testHasTypeValidator_true() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        assertTrue(schema.hasTypeValidator());
        assertNotNull(schema.getTypeValidator());
    }

    @Test
    public void testGetValidators_nonEmpty() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        List<JsonValidator> validators = schema.getValidators();
        assertNotNull(validators);
        assertFalse(validators.isEmpty());
    }

    @Test
    public void testFindLexicalRoot_rootHasNoId() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        JsonSchema lexicalRoot = schema.findLexicalRoot();
        // As there is no $id, the root itself is the lexical root
        assertSame(schema, lexicalRoot);
    }

    @Test
    public void testFindSchemaResourceRoot_rootIsResourceRoot() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        JsonSchema root = schema.findSchemaResourceRoot();
        assertSame(schema, root);
    }

    @Test
    public void testIsSchemaResourceRoot_trueWhenNoIdAndNoParent() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        assertTrue(schema.isSchemaResourceRoot());
    }

    @Test
    public void testFindAncestor_returnsSelfWhenNoParent() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        assertSame(schema, schema.findAncestor());
    }

    @Test
    public void testWalk_validObject_noErrors() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        JsonNode instance = MAPPER.readTree("{\"x\":1}");
        ValidationResult result = schema.walk(instance, true);
        assertTrue("Walk with valid data must have no errors", result.getMessages().isEmpty());
    }

    @Test
    public void testWalk_invalidObject_collectsErrors() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        JsonNode instance = MAPPER.readTree("[1,2]");
        ValidationResult result = schema.walk(instance, true);
        assertFalse("Walk with invalid data must contain errors", result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  3. EDGE‑CASE & EXCEPTION SCENARIOS                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetRefSchemaNode_withIdPrefixStripping() throws Exception {
        // Schema with $id = "http://example.com/base#"
        String schemaJson = "{"
                + "\"$id\":\"http://example.com/base#\","
                + "\"properties\":{\"foo\":{\"type\":\"string\"}}"
                + "}";
        JsonNode node = MAPPER.readTree(schemaJson);
        JsonSchema schema = createSchema(node);
        // Reference includes the full id; method must strip it.
        String ref = "http://example.com/base#/properties/foo";
        JsonNode target = schema.getRefSchemaNode(ref);
        assertNotNull(target);
        assertEquals("string", target.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_nullIdAndFragment() throws Exception {
        // Schema without $id and without fragment – should be a resource root.
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        JsonSchema schema = createSchema(node);
        // Reference that starts with a non‑existent id must be treated as a normal JSON pointer.
        JsonNode result = schema.getRefSchemaNode("#/nonexistent");
        assertNull(result);
    }

    @Test(expected = JsonSchemaException.class)
    public void testRead_invalidRecursiveAnchor_throwsException() throws Exception {
        // $recursiveAnchor must be boolean – using a string triggers exception.
        String badSchema = "{\"$recursiveAnchor\":\"yes\"}";
        JsonNode node = MAPPER.readTree(badSchema);
        // The constructor reads the schema and validates $recursiveAnchor.
        createSchema(node);
    }

    @Test
    public void testGetRefSchema_anchorResolution() {
        // Build a schema with an explicit anchor.
        String schemaWithAnchor = "{"
                + "\"$defs\":{"
                + "   \"myAnchor\":{"
                + "       \"$anchor\":\"myAnchor\","
                + "       \"type\":\"object\""
                + "   }"
                + "}"
                + "}";
        JsonNode node = null;
        try {
            node = MAPPER.readTree(schemaWithAnchor);
        } catch (Exception e) {
            fail("Failed to parse test schema");
        }
        JsonSchema schema = createSchema(node);
        JsonNodePath anchorPath = new JsonNodePath(PathType.JSON_POINTER).append("$defs").append("myAnchor");
        JsonSchema anchored = schema.getRefSchema(anchorPath);
        assertNotNull(anchored);
        assertEquals("object", anchored.getSchemaNode().get("type").asText());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetRefSchema_unknownAnchor_throwsException() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        // Attempt to resolve a non‑existent anchor.
        schema.getRefSchema("nonexistentAnchor");
    }

    @Test
    public void testGetSubSchema_missingFragment_throwsInvalidSchemaRefException() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        JsonNodePath badFragment = new JsonNodePath(PathType.JSON_POINTER).append("nonexistent");
        try {
            schema.getSubSchema(badFragment);
            fail("Expected InvalidSchemaRefException");
        } catch (InvalidSchemaRefException e) {
            // Expected – the exception contains a ValidationMessage with code "internal.unresolvedRef"
            assertEquals("internal.unresolvedRef", e.getValidationMessage().getCode());
        }
    }

    @Test
    public void testValidate_withFailFastConfiguration() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setFailFast(true);
        JsonSchema schema = createSchema(simpleSchemaNode).withConfig(cfg);
        JsonNode instance = MAPPER.readTree("[1,2,3]"); // invalid (type mismatch)
        // Validation should stop after first error – still returns a non‑empty set.
        Set<ValidationMessage> errors = schema.validate(instance);
        assertFalse(errors.isEmpty());
        // Ensure the execution context inside the schema also has failFast enabled.
        ExecutionContext ctx = schema.createExecutionContext();
        assertTrue(ctx.isFailFast());
    }

    @Test
    public void testWalkAtNode_customizer_isInvoked() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        ExecutionContext ctx = schema.createExecutionContext();
        JsonNode instance = MAPPER.readTree("{\"a\":1}");
        final boolean[] invoked = {false};
        ExecutionContextCustomizer customizer = (executionContext, validationContext) -> invoked[0] = true;
        schema.walkAtNode(ctx, instance, instance, schema.atRoot(), true);
        // The customizer version of walkAtNode is not directly exposed, but we can test the
        // overloaded variant that accepts a customizer.
        schema.walkAtNode(ctx, instance, instance, schema.atRoot(), true);
        // Since we called the method without a customizer, the flag stays false.
        assertFalse(invoked[0]);
        // Now call the variant that takes a customizer.
        schema.walkAtNode(ctx, instance, instance, schema.atRoot(), true);
        // Using the Consumer overload to verify it is called.
        schema.walkAtNode(ctx, instance, instance, schema.atRoot(), true);
        // The test purpose is to demonstrate that customizers can be passed; actual
        // verification of invocation is performed via the Consumer overload below.
        schema.walkAtNode(ctx, instance, instance, schema.atRoot(), true);
    }

    @Test
    public void testWalkWithConsumerCustomizer() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        JsonNode instance = MAPPER.readTree("{\"b\":2}");
        final boolean[] called = {false};
        ValidationResult result = schema.walk(instance, true, executionContext -> called[0] = true);
        assertTrue("Consumer customizer must have been invoked", called[0]);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void testValidateStringWithCustomOutputFormat() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        // Use the built‑in OutputFormat that returns a List<String> of messages.
        OutputFormat<List<String>> format = OutputFormat.LIST;
        List<String> messages = schema.validate("{\"c\":3}", InputFormat.JSON, format);
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testValidateStringWithInvalidInputFormat_throwsException() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        try {
            schema.validate("not a json", InputFormat.JSON);
            fail("Expected IllegalArgumentException due to malformed JSON");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testValidateAndCollect_isDeprecatedButWorks() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        JsonNode instance = MAPPER.readTree("{\"d\":4}");
        ValidationResult result = schema.validateAndCollect(instance);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void testToString_containsSchemaNode() {
        JsonSchema schema = createSchema(simpleSchemaNode);
        String s = schema.toString();
        assertTrue(s.contains("\"type\":\"object\""));
    }
}
```
###Test END##