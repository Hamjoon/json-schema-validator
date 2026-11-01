package com.networknt.schema;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;
import java.util.Set;

/**
 * Comprehensive JUnit‑4 tests for {@link OneOfValidator}.
 *
 * <p>The tests exercise the public contract of {@code OneOfValidator}
 * by using real {@link JsonSchema} instances created from JSON schema
 * definitions that contain a {@code oneOf} keyword.  The {@code JsonSchema}
 * class internally builds a {@code OneOfValidator} for the keyword, so
 * validating JSON instances against the schema triggers the validator
 * logic we need to verify.</p>
 *
 * <p>Four distinct scenarios are covered:</p>
 *
 * <ul>
 *   <li>Exactly one subschema matches – validation succeeds.</li>
 *   <li>No subschema matches – a “oneOf” error with argument {@code 0}
 *       is produced.</li>
 *   <li>More than one subschema matches – a “oneOf” error with argument
 *       {@code 2} (or the actual count) is produced.</li>
 *   <li>The {@code walk} method is exercised with {@code shouldValidateSchema}
 *       set to {@code false} to ensure child schemas are visited without
 *       producing validation messages.</li>
 * </ul>
 *
 * <p>All objects are created through their public constructors or factory
 * methods; no mocking or subclassing of library classes is performed.</p>
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ExecutionContext executionContext;

    @Before
    public void setUp() {
        // The default ExecutionContext contains a fresh CollectorContext and
        // ExecutionConfig with the default locale.
        this.executionContext = new ExecutionContext();
    }

    /**
     * Helper method that builds a {@link JsonSchema} from a JSON string
     * using the V7 meta‑schema (the version used by the library for the
     * test suite).
     */
    private JsonSchema buildSchema(String jsonSchema) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(jsonSchema);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        return factory.getSchema(schemaNode);
    }

    /**
     * Scenario 1 – exactly one subschema matches.
     *
     * Schema: oneOf [ {"type":"string"}, {"type":"integer"} ]
     * Instance: a JSON string ("hello").
     *
     * Expected: no validation messages, i.e. the OneOfValidator reports
     * a single valid subschema.
     */
    @Test
    public void testOneOfExactlyOneMatches() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema schema = buildSchema(schemaJson);

        JsonNode instance = MAPPER.readTree("\"hello\""); // a JSON string

        Set<ValidationMessage> messages = schema.validate(instance);
        assertTrue("Validation should succeed when exactly one subschema matches", messages.isEmpty());
    }

    /**
     * Scenario 2 – no subschema matches.
     *
     * Schema: oneOf [ {"type":"string"}, {"type":"integer"} ]
     * Instance: a JSON boolean (true).
     *
     * Expected: a single OneOf validation message whose argument is
     * the number of matching subschemas (0).  The message code is
     * {@code oneOf} (derived from the validator type).
     */
    @Test
    public void testOneOfNoMatches() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema schema = buildSchema(schemaJson);

        JsonNode instance = MAPPER.readTree("true"); // a JSON boolean

        Set<ValidationMessage> messages = schema.validate(instance);
        assertEquals("Exactly one validation message should be produced when no subschemas match", 1, messages.size());

        ValidationMessage msg = messages.iterator().next();
        assertEquals("Validator type code should be 'oneOf'", "oneOf", msg.getType());

        // The message arguments contain the number of matching subschemas.
        Object[] args = msg.getArguments();
        assertNotNull("Arguments should not be null", args);
        assertEquals("There should be a single argument", 1, args.length);
        assertEquals("Argument should be \"0\" when no subschemas match", "0", args[0].toString());
    }

    /**
     * Scenario 3 – more than one subschema matches.
     *
     * Schema: oneOf [ {"type":"integer"}, {"type":"number"} ]
     * Instance: an integer value (5) which satisfies both subschemas.
     *
     * Expected: a OneOf validation message with argument {@code 2}
     * (two matching subschemas) and the child error collections
     * (which are empty because each matching subschema reports no errors).
     */
    @Test
    public void testOneOfMultipleMatches() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"integer\"}, {\"type\":\"number\"} ] }";
        JsonSchema schema = buildSchema(schemaJson);

        JsonNode instance = MAPPER.readTree("5"); // integer value

        Set<ValidationMessage> messages = schema.validate(instance);
        assertEquals("One validation message expected when multiple subschemas match", 1, messages.size());

        ValidationMessage msg = messages.iterator().next();
        assertEquals("Validator type code should be 'oneOf'", "oneOf", msg.getType());

        Object[] args = msg.getArguments();
        assertNotNull("Arguments should not be null", args);
        assertEquals("Exactly one argument expected", 1, args.length);
        assertEquals("Argument should be \"2\" when two subschemas match", "2", args[0].toString());
    }

    /**
     * Scenario 4 – exercising the {@code walk} method with
     * {@code shouldValidateSchema = false}.
     *
     * The walk call should traverse each child schema but must not
     * return any {@link ValidationMessage}s because validation is
     * disabled.  We verify that the returned set is empty.
     */
    @Test
    public void testWalkWithoutValidation() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema schema = buildSchema(schemaJson);

        JsonNode instance = MAPPER.readTree("\"ignored\""); // content does not matter

        // Use the JsonSchemaWalker API directly.
        JsonNodePath instanceLocation = new JsonNodePath(JsonNodePath.PathType.ROOT);
        Set<ValidationMessage> walkMessages = schema.walk(
                executionContext,
                instance,
                instance,                 // root node is the same as the instance for this test
                instanceLocation,
                false                     // shouldValidateSchema = false
        );

        assertTrue("walk with shouldValidateSchema=false must not produce messages", walkMessages.isEmpty());
    }

    /**
     * Verify that {@code preloadJsonSchema} invokes {@code initializeValidators}
     * on each child schema.  The method does not return a value, so we
     * simply assert that calling it does not throw any exception.
     */
    @Test
    public void testPreloadJsonSchemaDoesNotFail() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema schema = buildSchema(schemaJson);

        // The cast to OneOfValidator is safe because the schema contains a
        // oneOf keyword and the library creates a OneOfValidator for it.
        JsonValidator validator = schema.getValidators()
                .stream()
                .filter(v -> v instanceof OneOfValidator)
                .findFirst()
                .orElseThrow(() -> new AssertionError("OneOfValidator not found"));

        // Preconditions: validator must be a OneOfValidator.
        assertTrue(validator instanceof OneOfValidator);
        OneOfValidator oneOfValidator = (OneOfValidator) validator;

        // The method should complete without throwing.
        oneOfValidator.preloadJsonSchema();
    }
}
