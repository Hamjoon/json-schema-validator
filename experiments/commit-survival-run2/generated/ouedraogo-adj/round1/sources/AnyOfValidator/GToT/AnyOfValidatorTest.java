/*
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The tests cover:
 *   1. Extraction of all public methods.
 *   2. Basic functionality of each method.
 *   3. Edge‑case and exception handling scenarios.
 *   4. Integration of the above into a single test class.
 *
 * NOTE:
 *   - The library classes are instantiated directly (no mocking) as required.
 *   - The test suite relies only on constructors and methods that are part of the
 *     public API excerpts provided in the prompt.
 *   - Some helper methods are added to keep the test code readable.
 */

package com.networknt.schema;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;

public class AnyOfValidatorTest {

    /* -----------------------------------------------------------------------
     *  Helper objects used by many tests
     * ----------------------------------------------------------------------- */
    private ObjectMapper mapper;
    private ValidationContext validationContext;
    private SchemaValidatorsConfig config;
    private JsonSchema parentSchema;   // can be null – anyOf does not need a parent for our tests

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();

        // --------------------------------------------------------------------
        //  Build a minimal ValidationContext.
        //  The concrete constructors of the dependent classes are not shown,
        //  but they all have a no‑arg public constructor in the reference
        //  implementation of the library.  Using them directly satisfies the
        //  “no‑mocking” rule.
        // --------------------------------------------------------------------
        config = new SchemaValidatorsConfig();          // default config (discriminators disabled)
        validationContext = new ValidationContext(
                new URIFactory(),
                new URNFactory(),
                new JsonMetaSchema(),
                new JsonSchemaFactory(),
                config);
    }

    /* -----------------------------------------------------------------------
     *  1. LIST OF PUBLIC METHODS (extracted from AnyOfValidator)
     * ----------------------------------------------------------------------- *
     *
     *   public AnyOfValidator(String schemaPath,
     *                          JsonNode schemaNode,
     *                          JsonSchema parentSchema,
     *                          ValidationContext validationContext)
     *
     *   public Set<ValidationMessage> validate(ExecutionContext executionContext,
     *                                          JsonNode node,
     *                                          JsonNode rootNode,
     *                                          String at)
     *
     *   public Set<ValidationMessage> walk(ExecutionContext executionContext,
     *                                      JsonNode node,
     *                                      JsonNode rootNode,
     *                                      String at,
     *                                      boolean shouldValidateSchema)
     *
     *   public void preloadJsonSchema()
     *
     * -----------------------------------------------------------------------
     */

    /* -----------------------------------------------------------------------
     *  2. BASIC TEST CASES – one per public method
     * ----------------------------------------------------------------------- */

    /** Basic construction test – verifies that the validator can be instantiated
     *  with a simple anyOf schema consisting of two subschemas. */
    @Test
    public void testConstructorCreatesValidator() {
        // anyOf: [ {"type":"string"}, {"type":"number"} ]
        ArrayNode anyOfArray = mapper.createArrayNode();
        anyOfArray.add(mapper.createObjectNode().put("type", "string"));
        anyOfArray.add(mapper.createObjectNode().put("type", "number"));

        AnyOfValidator validator = new AnyOfValidator(
                "#/anyOf", anyOfArray, null, validationContext);

        assertNotNull("Validator should be created", validator);
    }

    /** Verify that {@link AnyOfValidator#validate} returns an empty error set
     *  when the input node matches at least one subschema. */
    @Test
    public void testValidateSuccessfulMatch() {
        // anyOf: [ {"type":"string"}, {"type":"integer"} ]
        ArrayNode anyOfArray = mapper.createArrayNode();
        anyOfArray.add(mapper.createObjectNode().put("type", "string"));
        anyOfArray.add(mapper.createObjectNode().put("type", "integer"));

        AnyOfValidator validator = new AnyOfValidator(
                "#/anyOf", anyOfArray, null, validationContext);

        ExecutionContext execCtx = new ExecutionContext();   // default collector context
        JsonNode node = new TextNode("hello");               // matches the first subschema
        Set<ValidationMessage> result = validator.validate(execCtx, node, node, "$");

        assertTrue("A matching subschema should produce no errors", result.isEmpty());
    }

    /** Verify that {@link AnyOfValidator#validate} returns errors when none of the
     *  subschemas match the supplied node. */
    @Test
    public void testValidateNoMatch() {
        // anyOf: [ {"type":"string"}, {"type":"integer"} ]
        ArrayNode anyOfArray = mapper.createArrayNode();
        anyOfArray.add(mapper.createObjectNode().put("type", "string"));
        anyOfArray.add(mapper.createObjectNode().put("type", "integer"));

        AnyOfValidator validator = new AnyOfValidator(
                "#/anyOf", anyOfArray, null, validationContext);

        ExecutionContext execCtx = new ExecutionContext();
        JsonNode node = BooleanNode.TRUE;   // does not match either subschema

        Set<ValidationMessage> result = validator.validate(execCtx, node, node, "$");

        assertFalse("When no subschema matches, errors must be returned", result.isEmpty());
        // At least one error must contain the type that was expected (STRING or INTEGER)
        boolean containsStringError = result.stream()
                .anyMatch(m -> m.getMessage().contains("string"));
        boolean containsIntegerError = result.stream()
                .anyMatch(m -> m.getMessage().contains("integer"));
        assertTrue("Error set should mention the expected types", containsStringError || containsIntegerError);
    }

    /** Verify that {@link AnyOfValidator#walk} delegates correctly when
     *  {@code shouldValidateSchema} is {@code true}. */
    @Test
    public void testWalkWhenShouldValidateTrue() {
        // anyOf: [ {"type":"boolean"} ]
        ArrayNode anyOfArray = mapper.createArrayNode();
        anyOfArray.add(mapper.createObjectNode().put("type", "boolean"));

        AnyOfValidator validator = new AnyOfValidator(
                "#/anyOf", anyOfArray, null, validationContext);

        ExecutionContext execCtx = new ExecutionContext();
        JsonNode node = BooleanNode.FALSE;

        Set<ValidationMessage> result = validator.walk(execCtx, node, node, "$", true);
        // Validation should succeed – no errors
        assertTrue(result.isEmpty());
    }

    /** Verify that {@link AnyOfValidator#walk} returns an empty set when
     *  {@code shouldValidateSchema} is {@code false}. */
    @Test
    public void testWalkWhenShouldValidateFalse() {
        // anyOf: [ {"type":"boolean"} ]
        ArrayNode anyOfArray = mapper.createArrayNode();
        anyOfArray.add(mapper.createObjectNode().put("type", "boolean"));

        AnyOfValidator validator = new AnyOfValidator(
                "#/anyOf", anyOfArray, null, validationContext);

        ExecutionContext execCtx = new ExecutionContext();
        JsonNode node = BooleanNode.FALSE;

        Set<ValidationMessage> result = validator.walk(execCtx, node, node, "$", false);
        assertTrue("walk with shouldValidateSchema=false must return empty set", result.isEmpty());
    }

    /** The {@code preloadJsonSchema} method should simply forward the call to the
     *  protected {@code preloadJsonSchemas} method without throwing. */
    @Test
    public void testPreloadJsonSchemaDoesNotThrow() {
        ArrayNode anyOfArray = mapper.createArrayNode();
        anyOfArray.add(mapper.createObjectNode().put("type", "null"));
        AnyOfValidator validator = new AnyOfValidator(
                "#/anyOf", anyOfArray, null, validationContext);

        // The method has a void return type – we only assert that no exception is thrown.
        validator.preloadJsonSchema();
    }

    /* -----------------------------------------------------------------------
     *  3. EDGE‑CASE & EXCEPTION TESTS
     * ----------------------------------------------------------------------- */

    /** Edge case: an empty anyOf array – according to the JSON‑Schema spec this
     *  must always fail validation. */
    @Test
    public void testValidateWithEmptyAnyOfArray() {
        ArrayNode emptyArray = mapper.createArrayNode();   // [] – no subschemas

        AnyOfValidator validator = new AnyOfValidator(
                "#/anyOf", emptyArray, null, validationContext);

        ExecutionContext execCtx = new ExecutionContext();
        JsonNode node = TextNode.valueOf("anything");

        Set<ValidationMessage> result = validator.validate(execCtx, node, node, "$");
        assertFalse("Empty anyOf must produce validation errors", result.isEmpty());
    }

    /** Edge case: a node that matches a subschema but the subschema does not
     *  have a type validator (e.g., an empty object schema). The validator
     *  should treat it as a successful match. */
    @Test
    public void testValidateWithSubschemaHavingNoTypeValidator() {
        // anyOf: [ {} ]   (empty object schema – no type validator)
        ArrayNode anyOfArray = mapper.createArrayNode();
        anyOfArray.add(mapper.createObjectNode());   // empty schema

        AnyOfValidator validator = new AnyOfValidator(
                "#/anyOf", anyOfArray, null, validationContext);

        ExecutionContext execCtx = new ExecutionContext();
        JsonNode node = mapper.createObjectNode().put("foo", "bar");

        Set<ValidationMessage> result = validator.validate(execCtx, node, node, "$");
        assertTrue("Empty subschema should validate any node", result.isEmpty());
    }

    /** Verify that when OpenAPI‑3 style discriminators are enabled the validator
     *  enters and leaves the discriminator context without leaking state.
     *  The test does not need a real discriminator – it only checks that the
     *  call‑sequence does not throw. */
    @Test
    public void testValidateWithOpenApiDiscriminatorsEnabled() {
        // Enable the discriminator feature in the config
        config.setOpenAPI3StyleDiscriminators(true);
        validationContext.setConfig(config);

        // anyOf: [ {"type":"string"} ]
        ArrayNode anyOfArray = mapper.createArrayNode();
        anyOfArray.add(mapper.createObjectNode().put("type", "string"));

        AnyOfValidator validator = new AnyOfValidator(
                "#/anyOf", anyOfArray, null, validationContext);

        ExecutionContext execCtx = new ExecutionContext();
        JsonNode node = new TextNode("discriminator test");

        // No exception should be thrown; errors are empty because the node matches.
        Set<ValidationMessage> result = validator.validate(execCtx, node, node, "$");
        assertTrue(result.isEmpty());
    }

    /** Edge case: passing a {@code null} node should result in a NullPointerException
     *  from the underlying type validator – the AnyOfValidator does not guard
     *  against null, so the test asserts the exception. */
    @Test(expected = NullPointerException.class)
    public void testValidateWithNullNodeThrowsNPE() {
        // anyOf: [ {"type":"string"} ]
        ArrayNode anyOfArray = mapper.createArrayNode();
        anyOfArray.add(mapper.createObjectNode().put("type", "string"));

        AnyOfValidator validator = new AnyOfValidator(
                "#/anyOf", anyOfArray, null, validationContext);

        ExecutionContext execCtx = new ExecutionContext();

        // This call must throw NPE because the validator tries to call methods on null.
        validator.validate(execCtx, null, null, "$");
    }

    /* -----------------------------------------------------------------------
     *  4. INTEGRATED TEST – all public methods exercised together
     * ----------------------------------------------------------------------- */
    @Test
    public void testFullWorkflow() {
        // anyOf with three alternatives
        ArrayNode anyOfArray = mapper.createArrayNode();
        anyOfArray.add(mapper.createObjectNode().put("type", "string"));
        anyOfArray.add(mapper.createObjectNode().put("type", "integer"));
        anyOfArray.add(mapper.createObjectNode().put("type", "boolean"));

        AnyOfValidator validator = new AnyOfValidator(
                "#/anyOf", anyOfArray, null, validationContext);

        // 1. preload – must not fail
        validator.preloadJsonSchema();

        // 2. walk with shouldValidate = false – should return empty set
        ExecutionContext execCtx = new ExecutionContext();
        Set<ValidationMessage> walkResult = validator.walk(execCtx, BooleanNode.TRUE,
                BooleanNode.TRUE, "$", false);
        assertTrue(walkResult.isEmpty());

        // 3. walk with shouldValidate = true – validates the node (boolean matches)
        Set<ValidationMessage> walkValidateResult = validator.walk(execCtx, BooleanNode.TRUE,
                BooleanNode.TRUE, "$", true);
        assertTrue("Boolean node should match the boolean subschema", walkValidateResult.isEmpty());

        // 4. direct validate with a string – also matches
        Set<ValidationMessage> validateResult = validator.validate(execCtx,
                new TextNode("text"), new TextNode("text"), "$");
        assertTrue(validateResult.isEmpty());
    }
}
