package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * The tests are written against the public API of {@code OneOfValidator}
 * (constructor, {@code validate}, {@code walk} and {@code preloadJsonSchema})
 * but they exercise the validator through the high‑level {@link JsonSchema}
 * API because constructing a {@code ValidationContext} manually is
 * cumbersome.  The {@link JsonSchemaFactory} creates the required
 * {@code ValidationContext} internally, so the behaviour of the
 * {@code OneOfValidator} is verified end‑to‑end.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JsonSchemaFactory factory;

    @Before
    public void setUp() {
        // Use the default factory – it creates a ValidationContext with all
        // required collaborators (URIFactory, URNFactory, etc.).
        factory = JsonSchemaFactory.getInstance();
    }

    /**
     * Helper to build a {@link JsonSchema} from a JSON string.
     */
    private JsonSchema getSchema(String schemaJson) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        return factory.getSchema(schemaNode);
    }

    /**
     * Helper to parse a JSON value (string, number, boolean, object, array).
     */
    private JsonNode parseJson(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    /* --------------------------------------------------------------------- */
    /*  PUBLIC METHOD LIST (extracted from OneOfValidator)                  */
    /* --------------------------------------------------------------------- */
    // 1. public OneOfValidator(String schemaPath,
    //                         JsonNode schemaNode,
    //                         JsonSchema parentSchema,
    //                         ValidationContext validationContext)
    //
    // 2. public Set<ValidationMessage> validate(ExecutionContext executionContext,
    //                                          JsonNode node,
    //                                          JsonNode rootNode,
    //                                          String at)
    //
    // 3. public Set<ValidationMessage> walk(ExecutionContext executionContext,
    //                                      JsonNode node,
    //                                      JsonNode rootNode,
    //                                      String at,
    //                                      boolean shouldValidateSchema)
    //
    // 4. public void preloadJsonSchema()

    /* --------------------------------------------------------------------- */
    /*  BASIC FUNCTIONALITY TESTS                                            */
    /* --------------------------------------------------------------------- */

    /**
     * Validate a node that matches exactly ONE subschema.
     * The validator must return an empty error set.
     */
    @Test
    public void testValidateExactlyOneValid_string() throws Exception {
        String schema = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema jsonSchema = getSchema(schema);

        JsonNode node = parseJson("\"hello\""); // a JSON string

        Set<ValidationMessage> messages = jsonSchema.validate(node);
        assertTrue("Expected no validation errors when exactly one subschema matches", messages.isEmpty());
    }

    /**
     * Same as above but with an integer value.
     */
    @Test
    public void testValidateExactlyOneValid_integer() throws Exception {
        String schema = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema jsonSchema = getSchema(schema);

        JsonNode node = parseJson("42"); // a JSON integer

        Set<ValidationMessage> messages = jsonSchema.validate(node);
        assertTrue("Expected no validation errors when exactly one subschema matches", messages.isEmpty());
    }

    /**
     * Node does not satisfy any subschema – error set must contain the
     * OneOf validation message.
     */
    @Test
    public void testValidateZeroValid() throws Exception {
        String schema = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema jsonSchema = getSchema(schema);

        JsonNode node = parseJson("true"); // boolean – matches none

        Set<ValidationMessage> messages = jsonSchema.validate(node);
        assertFalse("Expected validation errors when no subschema matches", messages.isEmpty());

        // The error code for ONE_OF is defined by the library; we only assert
        // that at least one message contains the path "" (root) and that
        // the message mentions that 0 schemas matched.
        boolean containsZeroInfo = messages.stream()
                .anyMatch(m -> m.getMessage().contains("0"));
        assertTrue("Error message should indicate that 0 schemas matched", containsZeroInfo);
    }

    /**
     * Node satisfies MORE THAN ONE subschema – the validator must report a
     * OneOf error (exactly one match required).
     */
    @Test
    public void testValidateMultipleValid() throws Exception {
        // Both subschemas accept any string: type=string and maxLength=5
        String schema = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"maxLength\":5} ] }";
        JsonSchema jsonSchema = getSchema(schema);

        JsonNode node = parseJson("\"abc\""); // length 3, matches both

        Set<ValidationMessage> messages = jsonSchema.validate(node);
        assertFalse("Expected validation errors when more than one subschema matches", messages.isEmpty());

        // The message should indicate that 2 schemas matched
        boolean containsTwoInfo = messages.stream()
                .anyMatch(m -> m.getMessage().contains("2"));
        assertTrue("Error message should indicate that 2 schemas matched", containsTwoInfo);
    }

    /**
     * Empty oneOf array – by spec it never validates successfully.
     */
    @Test
    public void testValidateEmptyOneOfArray() throws Exception {
        String schema = "{ \"oneOf\": [] }";
        JsonSchema jsonSchema = getSchema(schema);

        JsonNode node = parseJson("\"anything\"");

        Set<ValidationMessage> messages = jsonSchema.validate(node);
        assertFalse("Expected validation error for empty oneOf array", messages.isEmpty());

        // The message should mention that 0 schemas matched
        boolean containsZeroInfo = messages.stream()
                .anyMatch(m -> m.getMessage().contains("0"));
        assertTrue("Error message should indicate that 0 schemas matched", containsZeroInfo);
    }

    /**
     * Walk with validation enabled – should behave exactly like {@code validate}.
     */
    @Test
    public void testWalkShouldValidate_true() throws Exception {
        String schema = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema jsonSchema = getSchema(schema);
        ExecutionContext ctx = jsonSchema.createExecutionContext();

        JsonNode node = parseJson("\"test\"");

        // Walk with shouldValidateSchema = true
        Set<ValidationMessage> walkMessages = jsonSchema.walk(node, true);
        Set<ValidationMessage> validateMessages = jsonSchema.validate(node);

        assertEquals("walk(true) must return same messages as validate()", validateMessages, walkMessages);
    }

    /**
     * Walk with validation disabled – the validator walks its children but
     * does not collect any errors, therefore the returned set must be empty.
     */
    @Test
    public void testWalkShouldValidate_false() throws Exception {
        String schema = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema jsonSchema = getSchema(schema);
        ExecutionContext ctx = jsonSchema.createExecutionContext();

        JsonNode node = parseJson("\"test\"");

        // Walk with shouldValidateSchema = false
        Set<ValidationMessage> walkMessages = jsonSchema.walk(node, false);
        assertTrue("walk(false) should return an empty set (no validation performed)", walkMessages.isEmpty());
    }

    /**
     * Ensure that {@code preloadJsonSchema()} does not throw any exception.
     * The method is called indirectly via {@link JsonSchema#initializeValidators()}.
     */
    @Test
    public void testPreloadJsonSchemaDoesNotThrow() throws Exception {
        String schema = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema jsonSchema = getSchema(schema);

        // The call should complete without throwing.
        jsonSchema.initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /*  EDGE‑CASE & EXCEPTION TESTS                                          */
    /* --------------------------------------------------------------------- */

    /**
     * Passing {@code null} as the instance node should result in a
     * {@link NullPointerException} (the validator dereferences the node).
     */
    @Test(expected = NullPointerException.class)
    public void testValidate_NullNode_throwsNPE() throws Exception {
        String schema = "{ \"oneOf\": [ {\"type\":\"string\"} ] }";
        JsonSchema jsonSchema = getSchema(schema);
        jsonSchema.validate((JsonNode) null);
    }

    /**
     * Verify that the internal state flag {@code complexValidator} is reset
     * after validation – this is an indirect test that the private
     * {@code resetValidatorState} method works correctly.
     *
     * The test inspects the {@link ValidatorState} via the collector
     * context after a validation run.
     */
    @Test
    public void testValidatorStateResetAfterValidate() throws Exception {
        String schema = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema jsonSchema = getSchema(schema);
        ExecutionContext ctx = jsonSchema.createExecutionContext();

        // First validation (should set complexValidator = true temporarily)
        JsonNode node = parseJson("\"hello\"");
        jsonSchema.validate(node);

        // After validation the state must be reset
        CollectorContext collector = ctx.getCollectorContext();
        ValidatorState state = (ValidatorState) collector.get(ValidatorState.VALIDATOR_STATE_KEY);
        assertNotNull("ValidatorState must be present in collector", state);
        assertFalse("complexValidator flag must be false after validation", state.isComplexValidator());
        assertTrue("matchedNode flag must be true after successful validation", state.isMatchedNode());
    }

    /**
     * Test that the validator correctly merges dynamic scopes when a child
     * schema validates without errors.  This is exercised by validating a
     * simple object against a oneOf that contains a subschema with a property
     * validator.
     */
    @Test
    public void testScopeMergingOnSuccessfulChild() throws Exception {
        String schema = "{ \"oneOf\": [ " +
                "  { \"type\":\"object\", \"properties\": { \"a\": {\"type\":\"string\"} }, \"required\":[\"a\"] }," +
                "  { \"type\":\"object\", \"properties\": { \"b\": {\"type\":\"integer\"} }, \"required\":[\"b\"] }" +
                "] }";
        JsonSchema jsonSchema = getSchema(schema);
        ExecutionContext ctx = jsonSchema.createExecutionContext();

        JsonNode node = parseJson("{\"a\":\"value\"}");

        Set<ValidationMessage> messages = jsonSchema.validate(node);
        assertTrue("Object matching first subschema should be valid", messages.isEmpty());

        // The collector should now contain the evaluated property "a"
        CollectorContext collector = ctx.getCollectorContext();
        assertTrue("Evaluated properties should contain 'a' after successful validation",
                collector.getEvaluatedProperties().contains("a"));
    }
}
