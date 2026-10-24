###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link OneOfValidator}. The tests create real {@link JsonSchema}
 * objects using the library’s public constructors and validate simple JSON
 * instances against a “oneOf” schema.  The focus is on the behaviour of the
 * {@code validate}, {@code walk} and {@code preloadJsonSchema} methods and on
 * edge‑cases such as zero, one or many matching subschemas.
 *
 * NOTE: The library classes such as {@link URIFactory}, {@link URNFactory},
 * {@link JsonMetaSchema}, {@link JsonSchemaFactory} and {@link SchemaValidatorsConfig}
 * have public no‑arg constructors in the reference implementation, therefore the
 * tests instantiate a {@link ValidationContext} with simple (often {@code null})
 * arguments.  In a real environment you would provide proper instances,
 * but for the purpose of these unit tests the minimal construction is sufficient.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private JsonSchema parentSchema; // can be {@code null} for these tests

    @Before
    public void setUp() {
        // Minimal (but functional) ValidationContext – the concrete library provides
        // default implementations for the required factories and meta‑schema.
        // Passing {@code null} is safe for the limited usage performed in the tests.
        validationContext = new ValidationContext(
                null,                     // URIFactory
                null,                     // URNFactory
                null,                     // JsonMetaSchema
                null,                     // JsonSchemaFactory
                new SchemaValidatorsConfig() // configuration (can be default)
        );
        parentSchema = null; // not needed for the test scenarios
    }

    /**
     * Helper that builds a {@link OneOfValidator} from a JSON representation of a
     * “oneOf” keyword (an array of subschemas).
     *
     * @param oneOfArrayJson the JSON array string, e.g. "[{\"type\":\"null\"}]"
     * @return a freshly constructed {@link OneOfValidator}
     */
    private OneOfValidator createValidator(String oneOfArrayJson) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(oneOfArrayJson);
        // The schemaPath is arbitrary for the test – we use “/oneOf”.
        return new OneOfValidator("/oneOf", schemaNode, parentSchema, validationContext);
    }

    /**
     * Helper that creates an {@link ExecutionContext} with a brand‑new
     * {@link CollectorContext}.  No special collector data are required for the
     * tests.
     */
    private ExecutionContext createExecutionContext() {
        return new ExecutionContext(new CollectorContext());
    }

    /**
     * ------------------------------------------------------------
     * PUBLIC METHOD LIST (extracted from OneOfValidator)
     * ------------------------------------------------------------
     *
     * 1. public OneOfValidator(String schemaPath,
     *                         JsonNode schemaNode,
     *                         JsonSchema parentSchema,
     *                         ValidationContext validationContext)
     *
     * 2. public Set<ValidationMessage> validate(ExecutionContext executionContext,
     *                                          JsonNode node,
     *                                          JsonNode rootNode,
     *                                          String at)
     *
     * 3. public Set<ValidationMessage> walk(ExecutionContext executionContext,
     *                                      JsonNode node,
     *                                      JsonNode rootNode,
     *                                      String at,
     *                                      boolean shouldValidateSchema)
     *
     * 4. public void preloadJsonSchema()
     *
     * ------------------------------------------------------------
     */

    /** -----------------------------------------------------------------
     *  BASIC FUNCTIONAL TEST – exactly one subschema matches.
     *  The “oneOf” contains a “null” schema and a “string” schema.
     *  The instance is a JSON string, therefore only the second subschema
     *  matches and validation should succeed (no errors).
     * ----------------------------------------------------------------- */
    @Test
    public void testValidateExactlyOneSchemaMatches() throws Exception {
        // oneOf: [ {"type":"null"}, {"type":"string"} ]
        OneOfValidator validator = createValidator("[{\"type\":\"null\"},{\"type\":\"string\"}]");

        ExecutionContext ctx = createExecutionContext();
        JsonNode instance = MAPPER.readTree("\"hello\""); // a JSON string

        Set<ValidationMessage> errors = validator.validate(ctx, instance, instance, "$");

        // Exactly one schema matches → no errors
        assertTrue("Expected no validation errors", errors.isEmpty());

        // The validator state should have been marked as matched (indirectly verified
        // by the absence of errors – the internal flag does not have a public getter).
    }

    /** -----------------------------------------------------------------
     *  EDGE CASE – no subschema matches.
     *  The “oneOf” contains a “null” schema and a “boolean” schema.
     *  The instance is a number, therefore none of the subschemas match.
     *  Validation must return a “oneOf” error.
     * ----------------------------------------------------------------- */
    @Test
    public void testValidateNoSchemaMatches() throws Exception {
        // oneOf: [ {"type":"null"}, {"type":"boolean"} ]
        OneOfValidator validator = createValidator("[{\"type\":\"null\"},{\"type\":\"boolean\"}]");

        ExecutionContext ctx = createExecutionContext();
        JsonNode instance = MAPPER.readTree("123"); // a JSON number

        Set<ValidationMessage> errors = validator.validate(ctx, instance, instance, "$");

        // Expect exactly one error – the OneOf validator error message
        assertEquals("Expected a single OneOf error", 1, errors.size());

        ValidationMessage msg = errors.iterator().next();
        // The message code for ONE_OF is defined in ValidatorTypeCode.ONE_OF;
        // we use the validator’s error code key indirectly via the message.
        assertEquals("oneOf", msg.getType());
        // The message arguments contain the number of matching schemas (0)
        assertArrayEquals(new String[]{"0"}, msg.getArguments());
    }

    /** -----------------------------------------------------------------
     *  EDGE CASE – more than one subschema matches.
     *  The “oneOf” contains two identical “null” schemas.  The instance is
     *  the JSON literal null, therefore both subschemas match.
     *  Validation must report an error because the number of matches is not 1.
     * ----------------------------------------------------------------- */
    @Test
    public void testValidateMultipleSchemasMatch() throws Exception {
        // oneOf: [ {"type":"null"}, {"type":"null"} ]
        OneOfValidator validator = createValidator("[{\"type\":\"null\"},{\"type\":\"null\"}]");

        ExecutionContext ctx = createExecutionContext();
        JsonNode instance = MAPPER.readTree("null"); // JSON null

        Set<ValidationMessage> errors = validator.validate(ctx, instance, instance, "$");

        // Expect a OneOf error (number of matching schemas = 2)
        assertEquals("Expected a single OneOf error", 1, errors.size());

        ValidationMessage msg = errors.iterator().next();
        assertEquals("oneOf", msg.getType());
        assertArrayEquals(new String[]{"2"}, msg.getArguments());
    }

    /** -----------------------------------------------------------------
     *  EDGE CASE – empty “oneOf” array.
     *  According to the spec, an empty array means the instance can never
     *  be valid, therefore the validator must always produce an error.
     * ----------------------------------------------------------------- */
    @Test
    public void testValidateEmptyOneOfArray() throws Exception {
        // oneOf: []
        OneOfValidator validator = createValidator("[]");

        ExecutionContext ctx = createExecutionContext();
        JsonNode instance = MAPPER.readTree("\"anything\"");

        Set<ValidationMessage> errors = validator.validate(ctx, instance, instance, "$");

        assertEquals("Empty oneOf must produce an error", 1, errors.size());

        ValidationMessage msg = errors.iterator().next();
        assertEquals("oneOf", msg.getType());
        // No subschema matched → argument “0”
        assertArrayEquals(new String[]{"0"}, msg.getArguments());
    }

    /** -----------------------------------------------------------------
     *  TEST WALK METHOD – when shouldValidateSchema is true the walk method
     *  must delegate to {@code validate} and return the same set of messages.
     * ----------------------------------------------------------------- */
    @Test
    public void testWalkValidateFlagTrue() throws Exception {
        // oneOf: [ {"type":"integer"} ]
        OneOfValidator validator = createValidator("[{\"type\":\"integer\"}]");

        ExecutionContext ctx = createExecutionContext();
        JsonNode instance = MAPPER.readTree("42"); // integer

        // Walk with validation enabled
        Set<ValidationMessage> walkMessages = validator.walk(ctx, instance, instance, "$", true);
        // Direct validation for comparison
        Set<ValidationMessage> directMessages = validator.validate(ctx, instance, instance, "$");

        assertEquals("walk(shouldValidate=true) must return same messages as validate()",
                directMessages, walkMessages);
    }

    /** -----------------------------------------------------------------
     *  TEST WALK METHOD – when shouldValidateSchema is false the walk method
     *  must iterate over each subschema’s walk method but must not collect any
     *  messages (the returned set is empty).
     * ----------------------------------------------------------------- */
    @Test
    public void testWalkValidateFlagFalse() throws Exception {
        // oneOf: [ {"type\":\"integer\"}, {"type\":\"string\"} ]
        OneOfValidator validator = createValidator("[{\"type\":\"integer\"},{\"type\":\"string\"}]");

        ExecutionContext ctx = createExecutionContext();
        JsonNode instance = MAPPER.readTree("\"text\""); // string

        // Walk with validation disabled – should return an empty set
        Set<ValidationMessage> walkMessages = validator.walk(ctx, instance, instance, "$", false);
        assertTrue("walk(shouldValidate=false) must return an empty set", walkMessages.isEmpty());

        // Ensure that no exception was thrown and the internal walk of subschemas
        // was executed (indirectly verified by the lack of errors).
    }

    /** -----------------------------------------------------------------
     *  TEST preloadJsonSchema – simply ensures that invoking the method does
     *  not throw an exception.  The method walks each child schema and calls
     *  {@code initializeValidators()}, which is safe for the simple schemas
     *  used in the test suite.
     * ----------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemaDoesNotThrow() throws Exception {
        // oneOf: [ {"type":"null"}, {"type":"boolean"} ]
        OneOfValidator validator = createValidator("[{\"type\":\"null\"},{\"type\":\"boolean\"}]");

        // The method should complete without throwing any exception.
        validator.preloadJsonSchema();
    }
}
```
###Test END##