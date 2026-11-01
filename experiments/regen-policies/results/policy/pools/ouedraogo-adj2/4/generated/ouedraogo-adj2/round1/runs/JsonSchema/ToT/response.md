###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchema}.
 *
 * <p>The tests are written in the same package as {@code JsonSchema} so that
 * package‑private members (e.g. {@code JsonSchema.from(...)}) can be accessed
 * without using reflection or mocks.</p>
 *
 * <p>Only the public and package‑private APIs that appear in the supplied
 * source code (or in the dependency signatures) are used.  No external
 * mocking frameworks are employed – real objects are constructed via the
 * constructors/factory methods that are available.</p>
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;

    @Before
    public void setUp() {
        /*
         *  ValidationContext requires a URIFactory, URNFactory, JsonMetaSchema,
         *  JsonSchemaFactory and a SchemaValidatorsConfig.  For the purposes of
         *  these unit tests we do not need URI resolution or schema‐factory
         *  capabilities, therefore the factories can be {@code null}.  The
         *  meta‑schema is required for id handling; we use the Draft‑07 meta‑schema.
         */
        validationContext = new ValidationContext(
                null,                     // URIFactory – not needed for the tests
                null,                     // URNFactory – not needed for the tests
                JsonMetaSchema.getV7(),   // a concrete meta‑schema
                null,                     // JsonSchemaFactory – not needed for the tests
                new SchemaValidatorsConfig()
        );

        schemaLocation = SchemaLocation.of("http://example.com/root-schema");
        // PathType is an enum inside JsonNodePath; the ROOT constant represents the
        // empty evaluation path.
        evaluationPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
    }

    /**
     * Helper that builds a {@link JsonSchema} from a JSON string.
     */
    private JsonSchema createSchema(String json) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(json);
        // No current URI (null) and no parent schema – this is a top‑level schema.
        return JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                null, schemaNode, null, false);
    }

    /* --------------------------------------------------------------------- */
    /*  getCurrentUri()                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void getCurrentUri_withoutId_returnsNull() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"string\" }");
        assertNull("When no $id is present, currentUri must be null", schema.getCurrentUri());
    }

    @Test
    public void getCurrentUri_withId_returnsCombinedUri() throws Exception {
        JsonSchema schema = createSchema("{ \"$id\": \"http://example.com/base\", \"type\": \"string\" }");
        URI expected = new URI("http://example.com/base");
        assertEquals("The currentUri should be the id defined in the schema", expected, schema.getCurrentUri());
    }

    /* --------------------------------------------------------------------- */
    /*  required validator detection                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void hasRequiredValidator_whenPresent_isTrue() throws Exception {
        JsonSchema schema = createSchema("{ \"required\": [\"name\"] }");
        assertTrue("Schema contains a required validator", schema.hasRequiredValidator());
        assertNotNull("requiredValidator must be non‑null", schema.getRequiredValidator());
    }

    @Test
    public void hasRequiredValidator_whenAbsent_isFalse() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"object\" }");
        assertFalse("Schema does not contain a required validator", schema.hasRequiredValidator());
        assertNull("requiredValidator must be null", schema.getRequiredValidator());
    }

    /* --------------------------------------------------------------------- */
    /*  type validator detection                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void hasTypeValidator_whenPresent_isTrue() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"integer\" }");
        assertTrue("Schema contains a type validator", schema.hasTypeValidator());
        assertNotNull("typeValidator must be non‑null", schema.getTypeValidator());
    }

    @Test
    public void hasTypeValidator_whenAbsent_isFalse() throws Exception {
        JsonSchema schema = createSchema("{ \"properties\": { \"id\": { \"type\": \"string\" } } }");
        assertFalse("Schema does not contain a top‑level type validator", schema.hasTypeValidator());
        assertNull("typeValidator must be null", schema.getTypeValidator());
    }

    /* --------------------------------------------------------------------- */
    /*  $ref resolution                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void getRefSchemaNode_resolvesLocalDefinition() throws Exception {
        String json = "{\n" +
                "  \"definitions\": {\n" +
                "    \"positiveInteger\": { \"type\": \"integer\", \"minimum\": 0 }\n" +
                "  },\n" +
                "  \"$ref\": \"#/definitions/positiveInteger\"\n" +
                "}";
        JsonSchema schema = createSchema(json);
        JsonNode refNode = schema.getRefSchemaNode("#/definitions/positiveInteger");
        assertNotNull("Reference node must be resolved", refNode);
        assertEquals("integer", refNode.get("type").asText());
        assertTrue("minimum must be present", refNode.has("minimum"));
    }

    /* --------------------------------------------------------------------- */
    /*  getValidators() – boolean schema handling                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void getValidators_booleanTrue_returnsTrueValidator() throws Exception {
        JsonSchema schema = createSchema("true");
        // The boolean schema is represented as a JsonNode (true) – the validator
        // keyword will be the string "true".
        assertEquals(1, schema.getValidators().size());
        JsonValidator validator = schema.getValidators().get(0);
        assertEquals("true", validator.getKeyword());
    }

    @Test
    public void getValidators_booleanFalse_returnsFalseValidator() throws Exception {
        JsonSchema schema = createSchema("false");
        assertEquals(1, schema.getValidators().size());
        JsonValidator validator = schema.getValidators().get(0);
        assertEquals("false", validator.getKeyword());
    }

    /* --------------------------------------------------------------------- */
    /*  initializeValidators – ensure pre‑loading does not explode            */
    /* --------------------------------------------------------------------- */

    @Test
    public void initializeValidators_executesWithoutException() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"string\" }");
        // The method should simply mark the schema as loaded and invoke
        // preloadJsonSchema() on each validator.  No exception means success.
        schema.initializeValidators();
        // No direct observable state, but a second call must be safe as well.
        schema.initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /*  Validation – happy path and error path                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void validate_validString_returnsNoErrors() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"string\" }");
        JsonNode instance = MAPPER.readTree("\"hello world\"");
        Set<ValidationMessage> errors = schema.validate(instance);
        assertTrue("A valid string must produce an empty error set", errors.isEmpty());
    }

    @Test
    public void validate_invalidType_returnsErrors() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"string\" }");
        JsonNode instance = MAPPER.readTree("123");
        Set<ValidationMessage> errors = schema.validate(instance);
        assertFalse("Invalid type must produce at least one error", errors.isEmpty());

        // Verify that the error refers to the correct validator keyword.
        ValidationMessage first = errors.iterator().next();
        assertEquals("type", first.getKeyword());
    }

    /* --------------------------------------------------------------------- */
    /*  findAncestor – lexical hierarchy                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void findAncestor_returnsRootSchema() throws Exception {
        // Root schema
        JsonSchema root = createSchema("{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }");
        // Child schema (simulates a subschema under a property)
        JsonSchema child = root.createChildSchema(
                SchemaLocation.of("http://example.com/root-schema#/properties/name"),
                MAPPER.readTree("{ \"type\": \"string\" }")
        );

        JsonSchema ancestor = child.findAncestor();
        assertSame("findAncestor should return the top‑most schema (the root)", root, ancestor);
    }

    /* --------------------------------------------------------------------- */
    /*  toString – sanity check                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void toString_containsPathAndSchemaNode() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"boolean\" }");
        String asString = schema.toString();
        assertTrue("toString must contain the evaluation path", asString.contains(schema.getEvaluationPath().toString()));
        assertTrue("toString must contain the raw schema JSON", asString.contains("\"type\":\"boolean\""));
    }

    /* --------------------------------------------------------------------- */
    /*  isDynamicAnchor – default is false                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void isDynamicAnchor_withoutRecursiveAnchor_isFalse() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"object\" }");
        assertFalse("When $recursiveAnchor is absent, dynamicAnchor must be false", schema.isDynamicAnchor());
    }

    @Test
    public void isDynamicAnchor_withRecursiveAnchorTrue_isTrue() throws Exception {
        JsonSchema schema = createSchema("{ \"$recursiveAnchor\": true }");
        assertTrue("When $recursiveAnchor is true, dynamicAnchor must be true", schema.isDynamicAnchor());
    }

    /* --------------------------------------------------------------------- */
    /*  Walk – ensure walk listeners are invoked without error                */
    /* --------------------------------------------------------------------- */

    @Test
    public void walk_validInstance_returnsNoErrors() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"string\" }");
        JsonNode instance = MAPPER.readTree("\"sample\"");
        ValidationResult result = schema.walk(instance, false);
        assertTrue("Walk on a valid instance must produce no validation messages", result.getValidationMessages().isEmpty());
    }

    @Test
    public void walk_invalidInstance_returnsErrors() throws Exception {
        JsonSchema schema = createSchema("{ \"type\": \"string\" }");
        JsonNode instance = MAPPER.readTree("42");
        ValidationResult result = schema.walk(instance, false);
        assertFalse("Walk on an invalid instance must produce validation messages", result.getValidationMessages().isEmpty());
        ValidationMessage msg = result.getValidationMessages().iterator().next();
        assertEquals("type", msg.getKeyword());
    }
}
```
###Test END##