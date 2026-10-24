package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.Rule;

import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic successful validation (exactly one subschema matches)</li>
 *   <li>validation when no subschema matches</li>
 *   <li>validation when more than one subschema matches</li>
 *   <li>the {@code failFast} behaviour (exception is thrown)</li>
 *   <li>the {@code walk} method with {@code shouldValidateSchema} true/false</li>
 *   <li>the {@code preloadJsonSchema} method (smoke test)</li>
 *   <li>edge cases such as an empty <code>oneOf</code> array and a {@code null} instance node</li>
 * </ul>
 *
 * The test class builds real {@link ValidationContext}, {@link ExecutionContext}
 * and {@link JsonSchema} objects using the public library API – no mocking or
 * subclassing of library interfaces is performed, as required by the task.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Helper to create a minimal ValidationContext – the library allows nulls for
    // the factories/config when they are not used by the particular test.
    private ValidationContext createValidationContext() {
        return new ValidationContext(
                null,               // URIFactory
                null,               // URNFactory
                null,               // JsonMetaSchema
                null,               // JsonSchemaFactory
                null                // SchemaValidatorsConfig
        );
    }

    /**
     * Creates a {@link OneOfValidator} from a JSON string that represents the
     * <code>oneOf</code> array (e.g. "[{\"type\":\"string\"},{\"type\":\"integer\"}]").
     *
     * @param oneOfArrayJson the JSON array definition of the oneOf keyword
     * @return a fully initialised {@code OneOfValidator}
     */
    private OneOfValidator createValidator(String oneOfArrayJson) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(oneOfArrayJson);
        ValidationContext vc = createValidationContext();
        // parent schema is not needed for the tests – pass {@code null}
        return new OneOfValidator("testSchema", schemaNode, null, vc);
    }

    /**
     * Executes validation with a fresh {@link ExecutionContext}.
     *
     * @param validator the {@code OneOfValidator} under test
     * @param node      the instance node to validate
     * @return the set of {@link ValidationMessage}s produced by {@code validate}
     */
    private Set<ValidationMessage> validate(OneOfValidator validator, JsonNode node) {
        ExecutionContext execCtx = new ExecutionContext(new CollectorContext());
        return validator.validate(execCtx, node, node, "$");
    }

    /**
     * Executes {@code walk} with a fresh {@link ExecutionContext}.
     *
     * @param validator            the {@code OneOfValidator} under test
     * @param node                 the instance node to walk
     * @param shouldValidateSchema if {@code true} the walk delegates to {@code validate}
     * @return the set of {@link ValidationMessage}s produced by {@code walk}
     */
    private Set<ValidationMessage> walk(OneOfValidator validator, JsonNode node, boolean shouldValidateSchema) {
        ExecutionContext execCtx = new ExecutionContext(new CollectorContext());
        return validator.walk(execCtx, node, node, "$", shouldValidateSchema);
    }

    /** --------------------------------------------------------------
     *  BASIC FUNCTIONALITY
     *  -------------------------------------------------------------- */

    @Test
    public void testValidateExactlyOneSchemaMatches() throws Exception {
        // oneOf = [string, integer]
        OneOfValidator validator = createValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        // a JSON string matches the first subschema only
        JsonNode instance = new TextNode("hello");

        Set<ValidationMessage> messages = validate(validator, instance);
        assertTrue("Exactly one subschema matches – no validation errors expected", messages.isEmpty());
    }

    @Test
    public void testValidateNoSchemaMatches() throws Exception {
        // oneOf = [string, integer]
        OneOfValidator validator = createValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        // a boolean does not match any subschema
        JsonNode instance = BooleanNode.TRUE;

        Set<ValidationMessage> messages = validate(validator, instance);
        assertFalse("No subschema matches – validation errors expected", messages.isEmpty());

        // At least one message must be the OneOf error (code defined by the validator type)
        boolean containsOneOf = messages.stream()
                .anyMatch(m -> m.getCode() != null && m.getCode().toLowerCase().contains("oneof"));
        assertTrue("OneOf error message must be present", containsOneOf);
    }

    @Test
    public void testValidateMoreThanOneSchemaMatches() throws Exception {
        // oneOf = [number, integer] – an integer satisfies both
        OneOfValidator validator = createValidator("[{\"type\":\"number\"},{\"type\":\"integer\"}]");

        JsonNode instance = new IntNode(42);

        Set<ValidationMessage> messages = validate(validator, instance);
        assertFalse("More than one subschema matches – validation errors expected", messages.isEmpty());

        // The error set should contain only the OneOf error (no child errors because both subschemas succeeded)
        boolean containsOneOf = messages.stream()
                .anyMatch(m -> m.getCode() != null && m.getCode().toLowerCase().contains("oneof"));
        assertTrue("OneOf error message must be present", containsOneOf);
        // Ensure there is no type‑error message from the child schemas
        long childErrors = messages.stream()
                .filter(m -> m.getCode() != null && !m.getCode().toLowerCase().contains("oneof"))
                .count();
        assertEquals("When more than one subschema matches, child errors should be absent", 0, childErrors);
    }

    /** --------------------------------------------------------------
     *  EDGE CASES
     *  -------------------------------------------------------------- */

    @Test
    public void testValidateWithEmptyOneOfArray() throws Exception {
        // oneOf = [] – by definition no schema can match
        OneOfValidator validator = createValidator("[]");

        JsonNode instance = new TextNode("anything");

        Set<ValidationMessage> messages = validate(validator, instance);
        assertFalse("Empty oneOf array must produce a validation error", messages.isEmpty());

        boolean containsOneOf = messages.stream()
                .anyMatch(m -> m.getCode() != null && m.getCode().toLowerCase().contains("oneof"));
        assertTrue("OneOf error message must be present for empty array", containsOneOf);
    }

    @Test
    public void testValidateWithNullInstanceNode() throws Exception {
        // oneOf = [string]
        OneOfValidator validator = createValidator("[{\"type\":\"string\"}]");

        JsonNode instance = NullNode.instance;

        Set<ValidationMessage> messages = validate(validator, instance);
        assertFalse("Null instance should not match a string schema – errors expected", messages.isEmpty());

        boolean containsOneOf = messages.stream()
                .anyMatch(m -> m.getCode() != null && m.getCode().toLowerCase().contains("oneof"));
        assertTrue("OneOf error must be present when null does not match any subschema", containsOneOf);
    }

    /** --------------------------------------------------------------
     *  FAIL‑FAST BEHAVIOUR
     *  -------------------------------------------------------------- */

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testFailFastThrowsJsonSchemaException() throws Exception {
        // oneOf = [string, integer] – instance matches both, causing failFast to trigger
        OneOfValidator validator = createValidator("[{\"type\":\"number\"},{\"type\":\"integer\"}]");

        // Force failFast to true via reflection (field is defined in BaseJsonValidator)
        java.lang.reflect.Field failFastField = BaseJsonValidator.class.getDeclaredField("failFast");
        failFastField.setAccessible(true);
        failFastField.setBoolean(validator, true);

        JsonNode instance = new IntNode(7);

        thrown.expect(JsonSchemaException.class);
        // The exception is thrown from within validate()
        validator.validate(new ExecutionContext(new CollectorContext()), instance, instance, "$");
    }

    /** --------------------------------------------------------------
     *  WALK METHOD
     *  -------------------------------------------------------------- */

    @Test
    public void testWalkWhenShouldValidateSchemaTrue() throws Exception {
        // oneOf = [string, integer]
        OneOfValidator validator = createValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        JsonNode instance = new TextNode("walk-test");

        Set<ValidationMessage> messages = walk(validator, instance, true);
        // Behaviour must be identical to validate()
        assertTrue("When shouldValidateSchema=true, walk should return same result as validate (no errors)",
                messages.isEmpty());
    }

    @Test
    public void testWalkWhenShouldValidateSchemaFalse() throws Exception {
        // oneOf = [string, integer]
        OneOfValidator validator = createValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        JsonNode instance = BooleanNode.FALSE; // matches none

        // shouldValidateSchema = false – walk delegates to each subschema's walk,
        // but does not collect validation messages.
        Set<ValidationMessage> messages = walk(validator, instance, false);
        assertTrue("When shouldValidateSchema=false, walk should return an empty set", messages.isEmpty());
    }

    /** --------------------------------------------------------------
     *  PRELOADJSONSCHEMA METHOD
     *  -------------------------------------------------------------- */

    @Test
    public void testPreloadJsonSchemaExecutesWithoutException() throws Exception {
        // oneOf = [string, integer]
        OneOfValidator validator = createValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        // The method does not return a value – we only verify that it does not throw.
        validator.preloadJsonSchema();
    }

    /** --------------------------------------------------------------
     *  INTERNAL STATE RESET (indirectly verified)
     *  -------------------------------------------------------------- */

    @Test
    public void testValidatorStateIsResetAfterValidation() throws Exception {
        // oneOf = [string]
        OneOfValidator validator = createValidator("[{\"type\":\"string\"}]");

        JsonNode instance = new TextNode("state-test");

        // First validation – should succeed
        Set<ValidationMessage> first = validate(validator, instance);
        assertTrue("First validation must succeed", first.isEmpty());

        // Second validation with a non‑matching node – should now produce errors,
        // proving that internal state was correctly reset between calls.
        JsonNode badInstance = BooleanNode.TRUE;
        Set<ValidationMessage> second = validate(validator, badInstance);
        assertFalse("Second validation must fail (state reset)", second.isEmpty());
    }
}
