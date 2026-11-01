###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.SetView;

/**
 * Unit tests for {@link OneOfValidator}. The tests exercise the core
 * behaviour of the validator without relying on any mocking framework.
 * Real {@link JsonSchema} objects are created via {@link JsonSchemaFactory}
 * and the validator is invoked through the normal schema validation API.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchemaFactory factory;

    @Before
    public void setUp() {
        // Use the 2019‑09 draft – it is the most common version and provides
        // all keywords used in the tests (type, minimum, maxLength, minLength, etc.).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
    }

    /**
     * Helper that validates a JSON instance against a given schema string
     * and returns the raw {@link ValidationMessage}s.
     */
    private Set<ValidationMessage> validate(String schemaJson, String instanceJson, boolean failFast) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        JsonNode instanceNode = MAPPER.readTree(instanceJson);

        JsonSchema schema = factory.getSchema(schemaNode);

        ExecutionContext ctx = new ExecutionContext();
        ctx.setFailFast(failFast); // controls {@link OneOfValidator#reportChildErrors}
        // the root node for validation is the instance itself
        return schema.validate(ctx, instanceNode, instanceNode, JsonNodePath.of(""));
    }

    /**
     * When exactly one subschema matches, no validation messages are produced.
     */
    @Test
    public void testExactlyOneMatch() throws Exception {
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";

        // a string matches the first subschema only
        Set<ValidationMessage> msgs = validate(schema, "\"hello\"", false);
        assertTrue("Expected no validation messages for a matching string", msgs.isEmpty());

        // an integer matches the second subschema only
        msgs = validate(schema, "42", false);
        assertTrue("Expected no validation messages for a matching integer", msgs.isEmpty());
    }

    /**
     * When no subschema matches, a single {@code oneOf} error is reported.
     */
    @Test
    public void testNoMatch() throws Exception {
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";

        // a boolean matches neither subschema
        Set<ValidationMessage> msgs = validate(schema, "true", false);
        assertEquals("Exactly one validation message expected when no subschema matches", 1, msgs.size());

        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf", vm.getMessageKey());

        // the message arguments contain the number of valid schemas (0) and an empty string for indexes
        Object[] args = vm.getArguments();
        assertEquals("0", args[0]);                 // number of matching schemas
        assertEquals("", args[1]);                  // indexes list (empty)
    }

    /**
     * When more than one subschema matches, a {@code oneOf.indexes} message is emitted
     * listing the indexes of the matching schemas.
     */
    @Test
    public void testMultipleMatches() throws Exception {
        // Both subschemas accept any number >= 0
        String schema = "{ \"oneOf\": [ { \"type\": \"number\" }, { \"minimum\": 0 } ] }";

        // The instance 5 satisfies both subschemas
        Set<ValidationMessage> msgs = validate(schema, "5", false);
        assertEquals("Exactly one validation message expected when multiple subschemas match", 1, msgs.size());

        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf.indexes", vm.getMessageKey());

        Object[] args = vm.getArguments();
        assertEquals("2", args[0]); // two schemas matched
        // the second argument should be a comma‑separated list of the matching indexes
        String indexes = (String) args[1];
        // order of indexes is deterministic (0,1)
        assertEquals("0, 1", indexes);
    }

    /**
     * Verify that child errors are only reported when {@code failFast} is disabled.
     * The schema contains two subschemas that both reject the instance – each produces
     * a distinct child error (maxLength / minLength). With {@code failFast = false}
     * the validator aggregates those child errors; with {@code failFast = true}
     * only the top‑level {@code oneOf} message is returned.
     */
    @Test
    public void testReportChildErrorsDependsOnFailFast() throws Exception {
        String schema = "{ \"oneOf\": [ "
                + "  { \"type\": \"string\", \"maxLength\": 2 }, "
                + "  { \"type\": \"string\", \"minLength\": 5 }"
                + "] }";

        // The instance "abcd" (length 4) violates both subschemas
        String instance = "\"abcd\"";

        // ---- failFast = false : child errors should be present
        Set<ValidationMessage> msgs = validate(schema, instance, false);
        // Expect one top‑level oneOf message + two child messages
        assertEquals("Expected three messages (one top‑level + two child errors)", 3, msgs.size());

        // Verify that at least one message has the key "maxLength" and another "minLength"
        Set<String> keys = msgs.stream()
                .map(ValidationMessage::getMessageKey)
                .collect(Collectors.toSet());

        assertTrue("Child error for maxLength expected", keys.contains("maxLength"));
        assertTrue("Child error for minLength expected", keys.contains("minLength"));

        // ---- failFast = true : only the top‑level oneOf message should be returned
        msgs = validate(schema, instance, true);
        assertEquals("Only the top‑level oneOf message should be present when failFast is true", 1, msgs.size());

        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf", vm.getMessageKey());
    }

    /**
     * The {@code canShortCircuit()} optimisation is disabled when a schema
     * contains {@code unevaluatedProperties} or {@code unevaluatedItems}. This test
     * builds a schema that mixes {@code oneOf} with {@code unevaluatedProperties}
     * and asserts that the validator does **not** short‑circuit after the second
     * matching subschema.
     */
    @Test
    public void testCanShortCircuitDisabledByUnevaluatedProperties() throws Exception {
        // The outer schema contains an unevaluatedProperties keyword, which forces
        // {@code canShortCircuit()} to return false.
        String schema = "{"
                + "  \"unevaluatedProperties\": false,"
                + "  \"oneOf\": ["
                + "    { \"type\": \"number\" },"
                + "    { \"minimum\": 0 },"
                + "    { \"maximum\": 10 }"
                + "  ]"
                + "}";

        // The instance 5 satisfies **all three** subschemas.
        Set<ValidationMessage> msgs = validate(schema, "5", false);
        // Because short‑circuiting is disabled, the validator will detect more than
        // one matching subschema and therefore emit a oneOf.indexes message.
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf.indexes", vm.getMessageKey());

        Object[] args = vm.getArguments();
        assertEquals("3", args[0]);               // three matching schemas
        assertEquals("0, 1, 2", args[1]);          // indexes of the matching schemas
    }

    /**
     * Helper to create a {@link JsonNodePath} from a simple JSON‑Pointer string.
     */
    private static JsonNodePath JsonNodePathOf(String pointer) {
        // The library provides a static factory method via the constructor that
        // accepts a string representation; however the public API only exposes
        // the constructor with a {@code PathType}.  For the purpose of the test
        // we can use the empty path (the root) because the validator does not
        // depend on the concrete instance location for the scenarios covered.
        return new JsonNodePath(com.networknt.schema.JsonNodePath.PathType.ROOT);
    }
}
###Test END##