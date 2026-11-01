package com.networknt.schema;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite that exercises the public behaviour of {@link JsonSchema}.
 * <p>
 * The tests focus on the most important public methods that can be exercised
 * without resorting to mocking or reflection:
 *
 * <ul>
 *   <li>{@link JsonSchema#isSchemaResourceRoot()}</li>
 *   <li>{@link JsonSchema#getId()}</li>
 *   <li>{@link JsonSchema#isRecursiveAnchor()}</li>
 *   <li>{@link JsonSchema#getRefSchemaNode(String)}</li>
 *   <li>{@link JsonSchema#initializeValidators()}</li>
 *   <li>{@link JsonSchema#createExecutionContext()}</li>
 *   <li>{@link JsonSchema#toString()}</li>
 * </ul>
 *
 * The tests build real {@link JsonSchema} instances via the public
 * {@link JsonSchemaFactory} API, using Jackson's {@link ObjectMapper} to create
 * {@link JsonNode} objects from JSON strings.
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchemaFactory factory;

    @Before
    public void setUp() {
        // Use the 2019‑09 dialect – it is the most recent stable version supported
        // by the library and provides all keywords used in the tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
    }

    /**
     * Helper that creates a {@link JsonSchema} from a JSON string.
     */
    private JsonSchema schemaFromJson(String json) throws IOException {
        JsonNode node = MAPPER.readTree(json);
        return factory.getSchema(node);
    }

    @Test
    public void testSchemaIdAndRootDetection() throws Exception {
        String json = "{ \"$id\": \"http://example.com/my-schema\", \"type\": \"object\" }";
        JsonSchema schema = schemaFromJson(json);

        // The $id should be stored and the schema must be recognised as a
        // resource root (because it has an explicit id).
        assertEquals("http://example.com/my-schema", schema.getId());
        assertTrue("Schema with $id should be a resource root", schema.isSchemaResourceRoot());

        // The lexical root of a schema that has an id is itself.
        assertSame("Lexical root should be the schema itself when id is present",
                schema, schema.findLexicalRoot());
    }

    @Test
    public void testRecursiveAnchorFlag() throws Exception {
        String json = "{ \"$recursiveAnchor\": true }";
        JsonSchema schema = schemaFromJson(json);

        assertTrue("The $recursiveAnchor keyword should set the flag",
                schema.isRecursiveAnchor());
    }

    @Test
    public void testGetRefSchemaNode_WithJsonPointer() throws Exception {
        String json = "{"
                + "  \"definitions\": {"
                + "    \"myDef\": { \"type\": \"string\" }"
                + "  },"
                + "  \"$ref\": \"#/definitions/myDef\""
                + "}";
        JsonSchema schema = schemaFromJson(json);

        // Resolve the reference manually using the public API.
        JsonNode refNode = schema.getRefSchemaNode("#/definitions/myDef");
        assertNotNull("Reference node must not be null", refNode);
        assertTrue("Reference node should be an object node", refNode.isObject());
        assertEquals("string", refNode.get("type").asText());
    }

    @Test
    public void testInitializeValidators_PreloadsSchemas() throws Exception {
        // A schema that contains a $ref to another subschema.
        String json = "{"
                + "  \"definitions\": {"
                + "    \"inner\": { \"type\": \"integer\" }"
                + "  },"
                + "  \"$ref\": \"#/definitions/inner\""
                + "}";
        JsonSchema schema = schemaFromJson(json);

        // Before initialization the validators are lazily loaded – we cannot
        // observe the internal flag directly, but we can ensure that calling
        // initializeValidators does not throw and that validation works afterwards.
        schema.initializeValidators();

        // Validate a correct instance – should produce no errors.
        Set<ValidationMessage> errors = schema.validate(MAPPER.readTree("5"));
        assertTrue("Valid integer should produce no errors after preload", errors.isEmpty());

        // Validate an incorrect instance – should produce a type error.
        errors = schema.validate(MAPPER.readTree("\"string\""));
        assertFalse("Invalid type should produce errors after preload", errors.isEmpty());
        ValidationMessage vm = errors.iterator().next();
        assertEquals("type", vm.getKeyword()); // the keyword that failed
    }

    @Test
    public void testCreateExecutionContext_InheritsConfig() throws Exception {
        // Build a schema with a custom configuration (e.g., fail‑fast disabled).
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setFailFast(false);
        config.setLocale(java.util.Locale.US);
        JsonSchemaFactory customFactory = JsonSchemaFactory.builder(factory.getMetaSchema())
                .setConfig(config)
                .build();
        JsonSchema schema = customFactory.getSchema("{\"type\":\"null\"}");

        ExecutionContext ctx = schema.createExecutionContext();

        // The execution context must reflect the configuration values.
        assertEquals("Locale should be propagated", java.util.Locale.US, ctx.getExecutionConfig().getLocale());
        assertFalse("Fail‑fast flag should be false", ctx.isFailFast());
    }

    @Test
    public void testToString_IncludesEvaluationPathAndSchemaNode() throws Exception {
        String json = "{ \"type\": \"boolean\" }";
        JsonSchema schema = schemaFromJson(json);

        String str = schema.toString();
        // The string representation should start with the evaluation path (which is
        // the root path) and contain the raw schema JSON.
        assertTrue("toString should start with a quotation mark and the root path",
                str.startsWith("\"\""));
        assertTrue("toString should contain the original schema JSON",
                str.contains("\"type\":\"boolean\""));
    }

    @Test
    public void testHasTypeValidator_AndGetTypeValidator() throws Exception {
        // Schema with an explicit type keyword.
        String json = "{ \"type\": \"array\" }";
        JsonSchema schema = schemaFromJson(json);

        assertTrue("Schema with a type keyword must report having a TypeValidator",
                schema.hasTypeValidator());

        TypeValidator tv = schema.getTypeValidator();
        assertNotNull("getTypeValidator should return a non‑null instance", tv);
        assertEquals("The TypeValidator should report the correct schema type",
                JsonType.ARRAY, tv.getSchemaType());
    }

    @Test
    public void testFindAncestor_AndFindSchemaResourceRoot() throws Exception {
        // A nested schema: root → child → grandchild.
        String json = "{"
                + "  \"$id\": \"http://example.com/root\","
                + "  \"properties\": {"
                + "    \"child\": {"
                + "      \"$id\": \"http://example.com/child\","
                + "      \"properties\": {"
                + "        \"grand\": { \"type\": \"string\" }"
                + "      }"
                + "    }"
                + "  }"
                + "}";
        JsonSchema root = schemaFromJson(json);
        JsonSchema child = root.getSubSchema(JsonNodePath.of("properties").append("child"));
        JsonSchema grand = child.getSubSchema(JsonNodePath.of("properties").append("grand"));

        // The ancestor of the grand schema (the top‑most parent) must be the root.
        assertSame("findAncestor should climb to the root", root, grand.findAncestor());

        // The resource root of the grand schema must be the child schema because it
        // carries its own $id.
        assertSame("resource root should be the schema that defines its own $id",
                child, grand.findSchemaResourceRoot());
    }
}
