package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JsonSchema}.  The tests focus on the public API
 * and cover normal behaviour as well as a few edge‑case / error‑handling
 * situations.
 *
 * <p>Only the classes, constructors and methods that appear in the supplied
 * source code (or in the minimal public API that was listed) are used – no
 * external mocking frameworks are employed.</p>
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private SchemaValidatorsConfig config;

    @Before
    public void setUp() {
        /*
         * The ValidationContext constructor requires several collaborators.
         * For the purpose of these unit tests we can safely pass {@code null}
         * for the factories because the code paths exercised never dereference
         * them (they are only used when an “id” keyword is present).  The
         * meta‑schema is needed for fragment resolution; we use the Draft‑7
         * meta‑schema which is readily available via the static factory method.
         */
        this.config = new SchemaValidatorsConfig();
        this.validationContext = new ValidationContext(
                null,                // URIFactory – not used in our tests
                null,                // URNFactory – not used in our tests
                JsonMetaSchema.getV7(),
                null,                // JsonSchemaFactory – not used directly
                this.config);
    }

    /**
     * Helper that creates a {@link JsonSchema} from a JSON schema string.
     */
    private JsonSchema createSchema(String schemaJson) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        // Use the static factory method – it bypasses the deprecated constructors.
        return JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
    }

    @Test
    public void testValidateValidString() throws Exception {
        String schema = "{\"type\":\"string\"}";
        JsonSchema jsonSchema = createSchema(schema);

        JsonNode validNode = MAPPER.readTree("\"hello world\"");
        Set<ValidationMessage> errors = jsonSchema.validate(validNode);

        assertTrue("A valid string should produce no validation errors", errors.isEmpty());
    }

    @Test
    public void testValidateInvalidString() throws Exception {
        String schema = "{\"type\":\"string\"}";
        JsonSchema jsonSchema = createSchema(schema);

        JsonNode invalidNode = MAPPER.readTree("123");
        Set<ValidationMessage> errors = jsonSchema.validate(invalidNode);

        assertFalse("A non‑string value must produce validation errors", errors.isEmpty());
        // The error should be produced by the TypeValidator
        assertTrue(errors.iterator().next().getType().contains("type"));
    }

    @Test
    public void testGetCurrentUriWhenNoId() throws Exception {
        String schema = "{\"type\":\"integer\"}";
        JsonSchema jsonSchema = createSchema(schema);

        // No $id keyword – currentUri should be null
        assertNull("currentUri must be null when schema does not contain an id", jsonSchema.getCurrentUri());
    }

    @Test
    public void testGetRefSchemaNodeSimplePath() throws Exception {
        /*
         * The schema contains a property definition.  We ask the JsonSchema
         * instance to resolve the JSON‑Pointer "#/properties/name" and verify
         * that the returned node is the definition of the property.
         */
        String schema = "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"name\": {\"type\": \"string\"},\n" +
                "    \"age\":  {\"type\": \"integer\"}\n" +
                "  }\n" +
                "}";
        JsonSchema jsonSchema = createSchema(schema);

        JsonNode nameNode = jsonSchema.getRefSchemaNode("#/properties/name");
        assertNotNull("Resolved node for '#/properties/name' must not be null", nameNode);
        assertEquals("type", "string", nameNode.get("type").asText());
    }

    @Test
    public void testRecursiveAnchorSetsDynamicFlag() throws Exception {
        /*
         * The "$recursiveAnchor" keyword must be a boolean.  When true the
         * internal flag {@code dynamicAnchor} should become true.
         */
        String schema = "{ \"$recursiveAnchor\": true }";
        JsonSchema jsonSchema = createSchema(schema);

        // Trigger reading of validators (the constructor does not evaluate the keyword)
        jsonSchema.initializeValidators();

        assertTrue("isDynamicAnchor() must be true when $recursiveAnchor is true", jsonSchema.isDynamicAnchor());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRecursiveAnchorInvalidTypeThrowsException() throws Exception {
        /*
         * Providing a non‑boolean value for "$recursiveAnchor" must cause a
         * JsonSchemaException to be thrown during validator creation.
         */
        String schema = "{ \"$recursiveAnchor\": \"yes\" }";
        JsonSchema jsonSchema = createSchema(schema);

        // The exception is raised while reading validators (called lazily)
        jsonSchema.validate(MAPPER.readTree("\"any\""));
    }

    @Test
    public void testInitializeValidatorsPreloads() throws Exception {
        /*
         * The initializeValidators() method should mark the schema as having
         * loaded its validators.  We verify that a subsequent call does not
         * attempt to preload again (no exception is thrown and the internal
         * flag is set).
         */
        String schema = "{ \"type\": \"boolean\" }";
        JsonSchema jsonSchema = createSchema(schema);

        // First call – should preload
        jsonSchema.initializeValidators();

        // Second call – should be a no‑op and must not throw
        jsonSchema.initializeValidators();

        // The flag is private; we can indirectly verify by calling a method that
        // depends on validators being present (e.g., validate a correct value)
        Set<ValidationMessage> errors = jsonSchema.validate(MAPPER.readTree("true"));
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testHasRequiredAndTypeValidators() throws Exception {
        /*
         * A schema that contains both “required” and “type” keywords should
         * expose the corresponding validator objects via the public accessors.
         */
        String schema = "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"required\": [\"id\"],\n" +
                "  \"properties\": { \"id\": {\"type\":\"integer\"} }\n" +
                "}";
        JsonSchema jsonSchema = createSchema(schema);

        // Force validator creation
        jsonSchema.validate(MAPPER.readTree("{\"id\":1}"));

        assertTrue("Schema should report having a required validator", jsonSchema.hasRequiredValidator());
        assertNotNull("getRequiredValidator() must return a non‑null instance", jsonSchema.getRequiredValidator());

        assertTrue("Schema should report having a type validator", jsonSchema.hasTypeValidator());
        assertNotNull("getTypeValidator() must return a non‑null instance", jsonSchema.getTypeValidator());
    }

    @Test
    public void testCreateChildSchemaPathPropagation() throws Exception {
        /*
         * The createChildSchema method must return a new JsonSchema instance
         * whose schemaPath reflects the supplied child path.
         */
        String parentSchema = "{ \"type\": \"object\" }";
        JsonSchema parent = createSchema(parentSchema);

        String childSchema = "{ \"type\": \"string\" }";
        JsonSchema child = parent.createChildSchema("#/properties/name", MAPPER.readTree(childSchema));

        assertEquals("Child schema path must match the supplied path",
                "#/properties/name", child.getSchemaPath());
    }

    @Test
    public void testWalkDelegatesToValidators() throws Exception {
        /*
         * The walk(...) method ultimately iterates over the registered
         * JsonValidator (which also implements JsonSchemaWalker).  Using a
         * simple schema we can verify that the method returns an empty set
         * when the data matches the schema.
         */
        String schema = "{ \"type\": \"integer\" }";
        JsonSchema jsonSchema = createSchema(schema);

        JsonNode node = MAPPER.readTree("42");
        ValidationResult result = jsonSchema.walk(node, true);

        assertNotNull("walk() must return a ValidationResult", result);
        assertTrue("walk() on a valid integer must produce no messages", result.getValidationMessages().isEmpty());
    }

    @Test
    public void testToStringContainsSchemaPathAndNode() throws Exception {
        String schema = "{ \"type\": \"null\" }";
        JsonSchema jsonSchema = createSchema(schema);

        String str = jsonSchema.toString();

        assertTrue("toString() should contain the schema path", str.contains(jsonSchema.getSchemaPath()));
        assertTrue("toString() should contain the JSON representation of the schema node", str.contains("\"type\""));
    }

    @Test
    public void testFindAncestorRootIsSelfWhenNoParent() throws Exception {
        String schema = "{ \"type\": \"boolean\" }";
        JsonSchema jsonSchema = createSchema(schema);

        JsonSchema ancestor = jsonSchema.findAncestor();

        assertSame("When there is no parent, findAncestor() must return the instance itself",
                jsonSchema, ancestor);
    }

    @Test
    public void testCreateExecutionContextProvidesCollectorContext() {
        String schema = "{ \"type\": \"boolean\" }";
        JsonSchema jsonSchema = null;
        try {
            jsonSchema = createSchema(schema);
        } catch (Exception e) {
            fail("Schema creation failed: " + e.getMessage());
        }

        ExecutionContext ec = jsonSchema.createExecutionContext();

        assertNotNull("ExecutionContext must not be null", ec);
        assertNotNull("CollectorContext inside ExecutionContext must not be null",
                ec.getCollectorContext());
    }
}
