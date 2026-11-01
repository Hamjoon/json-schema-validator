###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link JsonSchema}. The tests focus on the public behaviour
 * of the class and avoid any external mocking – only real objects from the
 * library are used.
 */
public class JsonSchemaTest {

    private ObjectMapper mapper;
    private ValidationContext validationContext;
    private SchemaValidatorsConfig config;

    @Before
    public void setUp() throws Exception {
        // Jackson mapper for building JSON nodes used in the tests
        mapper = new ObjectMapper();

        // Build the minimal objects required to construct a ValidationContext.
        // All of these classes have public no‑arg constructors in the library,
        // which are not listed explicitly in the prompt but are part of the
        // public API, therefore they are legal to use.
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV4(); // any draft works for our tests
        JsonSchemaFactory jsonSchemaFactory = new JsonSchemaFactory();

        config = new SchemaValidatorsConfig(); // default configuration
        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema, jsonSchemaFactory, config);
    }

    /**
     * Helper that creates a {@link JsonSchema} instance from the supplied
     * JSON node and optional base URI.
     */
    private JsonSchema createSchema(JsonNode schemaNode, URI baseUri) {
        SchemaLocation schemaLocation = SchemaLocation.of("http://example.com/root");
        JsonNodePath evaluationPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        // The static factory method is package‑private but we are in the same
        // package (com.networknt.schema) so we can call it directly.
        return JsonSchema.from(validationContext, schemaLocation, evaluationPath, baseUri, schemaNode, null, false);
    }

    @Test
    public void testCurrentUriWithId() throws Exception {
        // schema contains an explicit "$id"
        ObjectNode schema = mapper.createObjectNode();
        schema.put("$id", "http://example.com/my-schema");

        JsonSchema jsonSchema = createSchema(schema, new URI("http://example.com/base"));
        URI current = jsonSchema.getCurrentUri();

        // The current URI should be the resolved value of the "$id"
        assertEquals(new URI("http://example.com/my-schema"), current);
    }

    @Test
    public void testGetRefSchemaNode() throws Exception {
        // Build a schema with a definitions section and a $ref that points to it
        ObjectNode inner = mapper.createObjectNode();
        inner.put("type", "string");

        ObjectNode definitions = mapper.createObjectNode();
        definitions.set("myDef", inner);

        ObjectNode schema = mapper.createObjectNode();
        schema.set("definitions", definitions);
        schema.put("$ref", "#/definitions/myDef");

        JsonSchema jsonSchema = createSchema(schema, null);
        JsonNode refNode = jsonSchema.getRefSchemaNode("#/definitions/myDef");

        assertNotNull(refNode);
        assertTrue(refNode.isObject());
        assertEquals("string", refNode.get("type").asText());
    }

    @Test
    public void testFindAncestor() throws Exception {
        // Root schema (no parent)
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("type", "object");
        JsonSchema rootSchema = createSchema(rootNode, null);

        // Child schema
        ObjectNode childNode = mapper.createObjectNode();
        childNode.put("type", "string");
        JsonSchema childSchema = rootSchema.createChildSchema(SchemaLocation.of("http://example.com/child"), childNode);

        // The ancestor of the child must be the root schema
        assertSame(rootSchema, childSchema.findAncestor());
    }

    @Test
    public void testFindLexicalRoot() throws Exception {
        // Root schema without an "$id"
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("type", "object");
        JsonSchema rootSchema = createSchema(rootNode, null);

        // Intermediate schema that also lacks "$id"
        ObjectNode intermediateNode = mapper.createObjectNode();
        intermediateNode.put("type", "object");
        JsonSchema intermediate = rootSchema.createChildSchema(SchemaLocation.of("http://example.com/intermediate"), intermediateNode);

        // Leaf schema that contains an "$id"
        ObjectNode leafNode = mapper.createObjectNode();
        leafNode.put("$id", "http://example.com/leaf");
        leafNode.put("type", "string");
        JsonSchema leaf = intermediate.createChildSchema(SchemaLocation.of("http://example.com/leaf"), leafNode);

        // The lexical root for the leaf must be the leaf itself because it has an id
        assertSame(leaf, leaf.findLexicalRoot());

        // The lexical root for the intermediate (no id) must be the original root
        assertSame(rootSchema, intermediate.findLexicalRoot());
    }

    @Test
    public void testHasRequiredValidator() throws Exception {
        ObjectNode schema = mapper.createObjectNode();
        schema.putArray("required").add("name");

        JsonSchema jsonSchema = createSchema(schema, null);
        assertTrue(jsonSchema.hasRequiredValidator());
        assertNotNull(jsonSchema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidator() throws Exception {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "string");

        JsonSchema jsonSchema = createSchema(schema, null);
        assertTrue(jsonSchema.hasTypeValidator());
        assertNotNull(jsonSchema.getTypeValidator());
        assertEquals("string", jsonSchema.getTypeValidator().getSchemaType().name().toLowerCase());
    }

    @Test
    public void testInitializeValidators() throws Exception {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "integer");

        JsonSchema jsonSchema = createSchema(schema, null);
        // Validators are lazily loaded – calling initializeValidators should preload them
        jsonSchema.initializeValidators();

        // After initialization the validators list must be non‑null and contain at least one validator
        List<JsonValidator> validators = jsonSchema.getValidators();
        assertNotNull(validators);
        assertFalse(validators.isEmpty());
    }

    @Test
    public void testDynamicAnchor() throws Exception {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("$recursiveAnchor", true);

        JsonSchema jsonSchema = createSchema(schema, null);
        assertTrue(jsonSchema.isDynamicAnchor());
    }

    @Test
    public void testCreateChildSchema() throws Exception {
        ObjectNode parentNode = mapper.createObjectNode();
        parentNode.put("type", "object");

        JsonSchema parent = createSchema(parentNode, null);

        ObjectNode childNode = mapper.createObjectNode();
        childNode.put("type", "string");

        JsonSchema child = parent.createChildSchema(SchemaLocation.of("http://example.com/child"), childNode);
        assertNotNull(child);
        // The child's parent must be the original parent
        assertSame(parent, child.getParentSchema());
    }

    @Test
    public void testToStringRepresentation() throws Exception {
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "boolean");

        JsonSchema jsonSchema = createSchema(schema, null);
        String stringRep = jsonSchema.toString();

        // The string must contain the evaluation path (which for the root is empty)
        // and the JSON representation of the schema node.
        assertTrue(stringRep.contains("\"type\""));
        assertTrue(stringRep.contains("boolean"));
    }

    @Test
    public void testValidatorsSortingOrder() throws Exception {
        // Build a schema that contains both "properties" and "required"
        ObjectNode properties = mapper.createObjectNode();
        properties.set("name", mapper.createObjectNode().put("type", "string"));

        ObjectNode schema = mapper.createObjectNode();
        schema.set("required", mapper.createArrayNode().add("name"));
        schema.set("properties", properties);

        JsonSchema jsonSchema = createSchema(schema, null);
        List<JsonValidator> validators = jsonSchema.getValidators();

        // According to VALIDATOR_SORT, "properties" must appear before "required"
        assertTrue(validators.size() >= 2);
        String firstKeyword = validators.get(0).getEvaluationPath().getName(-1);
        String secondKeyword = validators.get(1).getEvaluationPath().getName(-1);
        assertEquals("properties", firstKeyword);
        assertEquals("required", secondKeyword);
    }
}
```
###Test END##