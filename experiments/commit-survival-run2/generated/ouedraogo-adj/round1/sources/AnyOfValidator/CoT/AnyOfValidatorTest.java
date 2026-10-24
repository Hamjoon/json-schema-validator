package com.networknt.schema;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

public class AnyOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private JsonSchema parentSchema; // can be null – not used by AnyOfValidator itself

    /** --------------------------------------------------------------------
     *  Helper to create a ValidationContext with the discriminator flag set as
     *  requested.
     *  -------------------------------------------------------------------- */
    private ValidationContext buildValidationContext(boolean openApiDiscriminators) {
        // The library supplies default implementations for all arguments.
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();               // draft‑07
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setOpenAPI3StyleDiscriminators(openApiDiscriminators);
        return new ValidationContext(uriFactory, urnFactory, metaSchema, schemaFactory, config);
    }

    @Before
    public void setUp() {
        // default context – discriminators disabled for the majority of tests
        this.validationContext = buildValidationContext(false);
        this.executionContext = new ExecutionContext(); // uses a fresh CollectorContext internally
        this.parentSchema = null; // parent schema is not needed for these unit tests
    }

    /** --------------------------  BASIC TESTS  --------------------------- */

    @Test
    public void testValidateAllValidSubschemas() throws Exception {
        // anyOf : [ {"type":"string"}, {"type":"integer"} ]
        String schemaJson = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        AnyOfValidator validator = new AnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);

        JsonNode node = MAPPER.readTree("\"hello\"");   // a JSON string

        Set<ValidationMessage> errors = validator.validate(executionContext, node, node, "$");
        assertTrue("All subschemas are valid – error set must be empty", errors.isEmpty());
    }

    @Test
    public void testValidateNoMatchingSubschema() throws Exception {
        String schemaJson = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        AnyOfValidator validator = new AnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);

        JsonNode node = MAPPER.readTree("true");   // a JSON boolean – matches none

        Set<ValidationMessage> errors = validator.validate(executionContext, node, node, "$");
        assertFalse("No subschema matches – there must be at least one error", errors.isEmpty());

        // each error should contain the type that was expected (STRING or INTEGER)
        boolean containsString = errors.stream()
                .anyMatch(m -> m.getMessage().contains("string"));
        boolean containsInteger = errors.stream()
                .anyMatch(m -> m.getMessage().contains("integer"));
        assertTrue("Error set should contain a string‑type message", containsString);
        assertTrue("Error set should contain an integer‑type message", containsInteger);
    }

    @Test
    public void testValidateEmptyAnyOf() throws Exception {
        // anyOf : []   (empty array)
        JsonNode schemaNode = MAPPER.readTree("[]");
        AnyOfValidator validator = new AnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);

        JsonNode node = MAPPER.readTree("\"anything\"");

        Set<ValidationMessage> errors = validator.validate(executionContext, node, node, "$");
        assertTrue("Empty anyOf must produce an empty error set", errors.isEmpty());
    }

    @Test
    public void testWalkWhenShouldValidateTrue() throws Exception {
        String schemaJson = "[{\"type\":\"string\"}]";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        AnyOfValidator validator = new AnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);

        JsonNode node = MAPPER.readTree("\"text\"");

        Set<ValidationMessage> viaValidate = validator.validate(executionContext, node, node, "$");
        Set<ValidationMessage> viaWalk = validator.walk(executionContext, node, node, "$", true);

        assertEquals("walk(..., true) must delegate to validate()", viaValidate, viaWalk);
    }

    @Test
    public void testWalkWhenShouldValidateFalse() throws Exception {
        String schemaJson = "[{\"type\":\"string\"}]";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        AnyOfValidator validator = new AnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);

        JsonNode node = MAPPER.readTree("123"); // integer – does NOT satisfy the subschema

        // walk with shouldValidate = false must never produce validation errors
        Set<ValidationMessage> result = validator.walk(executionContext, node, node, "$", false);
        assertTrue("walk(..., false) must return an empty set even if node does not match", result.isEmpty());
    }

    @Test
    public void testPreloadJsonSchemaDoesNotThrow() throws Exception {
        String schemaJson = "[{\"type\":\"string\"}]";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        AnyOfValidator validator = new AnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);

        // simply invoke – any exception will cause the test to fail
        validator.preloadJsonSchema();
    }

    /** --------------------------  EDGE CASES  --------------------------- */

    @Test
    public void testValidateWithOpenAPI3DiscriminatorsEnabled() throws Exception {
        // rebuild the context with discriminators turned on
        this.validationContext = buildValidationContext(true);
        this.executionContext = new ExecutionContext(); // fresh collector context

        // simple anyOf – discriminators are not used inside the subschemas, so the
        // validator should behave exactly as in the normal case and must not throw.
        String schemaJson = "[{\"type\":\"string\"}]";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        AnyOfValidator validator = new AnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);

        JsonNode node = MAPPER.readTree("\"abc\"");
        Set<ValidationMessage> errors = validator.validate(executionContext, node, node, "$");
        assertTrue("When discriminators are enabled but not used, validation must still succeed", errors.isEmpty());
    }

    @Test
    public void testValidateWithUnionType() throws Exception {
        // union type: ["string","null"]
        String schemaJson = "[{\"type\":[\"string\",\"null\"]}]";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        AnyOfValidator validator = new AnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);

        // node is a string – should be accepted
        JsonNode node = MAPPER.readTree("\"union\"");
        Set<ValidationMessage> errors = validator.validate(executionContext, node, node, "$");
        assertTrue("Union type should accept a matching string", errors.isEmpty());

        // node is null – also acceptable
        JsonNode nullNode = MAPPER.readTree("null");
        Set<ValidationMessage> errors2 = validator.validate(executionContext, nullNode, nullNode, "$");
        assertTrue("Union type should accept null", errors2.isEmpty());
    }

    @Test
    public void testValidateRequiredErrorsAreSuppressedWhenOneSubschemaValid() throws Exception {
        // subschema 0 : requires property "x"
        // subschema 1 : type string (valid)
        String schemaJson = "[{\"required\":[\"x\"]},{\"type\":\"string\"}]";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        AnyOfValidator validator = new AnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);

        // node is a simple string – matches subschema 1, fails subschema 0 with a required error
        JsonNode node = MAPPER.readTree("\"ok\"");

        Set<ValidationMessage> errors = validator.validate(executionContext, node, node, "$");
        // because subschema 1 is valid, required‑error from subschema 0 must be removed
        assertTrue("When one subschema validates, required‑property errors must be suppressed", errors.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testValidateNullNodeThrowsException() throws Exception {
        String schemaJson = "[{\"type\":\"string\"}]";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        AnyOfValidator validator = new AnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);

        // passing null as the node – library does not guard against it
        validator.validate(executionContext, null, null, "$");
    }
}
