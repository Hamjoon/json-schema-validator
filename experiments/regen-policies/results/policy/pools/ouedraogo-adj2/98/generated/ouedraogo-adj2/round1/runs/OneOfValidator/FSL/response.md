###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.SetView;

/**
 * JUnit‑4 test suite that exercises all logical paths of {@link OneOfValidator}.
 *
 * <p>The tests create real {@link JsonSchema} objects using the public
 * {@link JsonSchemaFactory} API – no mocking or subclassing of library classes
 * is performed.  Each test extracts the {@code OneOfValidator} from the compiled
 * schema and invokes its {@code validate(...)} method with a concrete
 * {@link ExecutionContext}, instance node and {@link JsonNodePath}.</p>
 *
 * <p>Coverage goals:</p>
 * <ul>
 *   <li>Zero matching subschemas → error with message key {@code "oneOf"}</li>
 *   <li>Exactly one matching subschema → no errors</li>
 *   <li>More than one matching subschema → error with message key {@code "oneOf.indexes"}
 *       and the list of matching indexes</li>
 *   <li>Short‑circuit behaviour (default {@code canShortCircuit}=true)</li>
 *   <li>Child‑error aggregation when {@code failFast}=false</li>
 *   <li>Suppression of child errors when {@code failFast}=true</li>
 * </ul>
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Helper that builds a {@link JsonSchema} from a JSON string and returns the
     * {@link OneOfValidator} contained in that schema.
     */
    private OneOfValidator getOneOfValidator(String schemaJson) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        // Use Draft‑7 (V7) – any version works for the simple schemas used here
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                .getSchema(schemaNode);
        List<JsonValidator> validators = schema.getValidators();
        for (JsonValidator v : validators) {
            if (v instanceof OneOfValidator) {
                return (OneOfValidator) v;
            }
        }
        throw new IllegalStateException("OneOfValidator not present in schema");
    }

    /**
     * Returns a fresh {@link ExecutionContext} with the default configuration.
     */
    private ExecutionContext newExecutionContext() {
        return new ExecutionContext();
    }

    /**
     * Returns a {@link JsonNodePath} representing the root location (\"$\" in JSON‑Pointer).
     */
    private JsonNodePath rootPath() {
        // The root path is created with the ROOT path type.
        return new JsonNodePath(JsonNodePath.PathType.ROOT);
    }

    @Test
    public void testZeroMatchingSchemas() throws Exception {
        // Schema: oneOf string OR integer
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        OneOfValidator validator = getOneOfValidator(schemaJson);

        // Instance that matches neither (boolean)
        JsonNode instance = MAPPER.readTree("true");

        ExecutionContext ctx = newExecutionContext();
        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, rootPath());

        assertEquals("Exactly one validation message expected when no schema matches", 1, messages.size());
        ValidationMessage msg = messages.iterator().next();
        assertEquals("Message key must be 'oneOf' when zero matches", "oneOf", msg.getMessageKey());
        assertEquals("Number of matching schemas argument should be 0", "0", msg.getArguments()[0]);
    }

    @Test
    public void testExactlyOneMatchingSchema() throws Exception {
        // Schema: oneOf string OR integer
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        OneOfValidator validator = getOneOfValidator(schemaJson);

        // Instance that matches only the string schema
        JsonNode instance = MAPPER.readTree("\"hello\"");

        ExecutionContext ctx = newExecutionContext();
        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, rootPath());

        assertTrue("No validation messages expected when exactly one schema matches", messages.isEmpty());
    }

    @Test
    public void testMultipleMatchingSchemas() throws Exception {
        // Both subschemas accept any number ≥ 0 → instance 5 matches both
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"number\"}, {\"minimum\":0} ] }";
        OneOfValidator validator = getOneOfValidator(schemaJson);

        JsonNode instance = MAPPER.readTree("5");

        ExecutionContext ctx = newExecutionContext();
        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, rootPath());

        assertEquals("One validation message expected when more than one schema matches", 1, messages.size());
        ValidationMessage msg = messages.iterator().next();
        assertEquals("Message key must be 'oneOf.indexes' when multiple matches",
                "oneOf.indexes", msg.getMessageKey());

        // The first argument is the number of matching schemas
        assertEquals("Number of matching schemas argument should be 2", "2", msg.getArguments()[0]);

        // The second argument is the comma‑separated list of matching indexes
        String indexes = (String) msg.getArguments()[1];
        assertTrue("Indexes should contain '0'", indexes.contains("0"));
        assertTrue("Indexes should contain '1'", indexes.contains("1"));
    }

    @Test
    public void testShortCircuitBehaviour() throws Exception {
        // Three subschemas – the first two are always valid for a string instance
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        OneOfValidator validator = getOneOfValidator(schemaJson);

        // The instance matches the first two subschemas → short‑circuit after the second
        JsonNode instance = MAPPER.readTree("\"any\"");

        ExecutionContext ctx = newExecutionContext();
        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, rootPath());

        // Still only one top‑level message (multiple matches) – the loop stopped after 2 matches
        assertEquals(1, messages.size());
        ValidationMessage msg = messages.iterator().next();
        assertEquals("oneOf.indexes", msg.getMessageKey());
        assertEquals("2", msg.getArguments()[0]); // only two schemas matched before short‑circuit
        assertEquals("0, 1", msg.getArguments()[1]); // indexes 0 and 1
    }

    @Test
    public void testChildErrorsAggregatedWhenFailFastFalse() throws Exception {
        // Both subschemas are strings but impose contradictory length constraints
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\",\"maxLength\":1}, {\"type\":\"string\",\"minLength\":5} ] }";
        OneOfValidator validator = getOneOfValidator(schemaJson);

        JsonNode instance = MAPPER.readTree("\"abc\""); // length 3 → violates both

        ExecutionContext ctx = newExecutionContext(); // default failFast = false
        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, rootPath());

        // Expect the top‑level oneOf message + the two child error messages
        assertEquals("Should contain top‑level message and two child errors", 3, messages.size());

        // Verify that a child message with key 'maxLength' is present
        boolean maxLengthFound = messages.stream()
                .anyMatch(m -> "maxLength".equals(m.getMessageKey()));
        boolean minLengthFound = messages.stream()
                .anyMatch(m -> "minLength".equals(m.getMessageKey()));
        assertTrue("Child error for maxLength must be reported", maxLengthFound);
        assertTrue("Child error for minLength must be reported", minLengthFound);
    }

    @Test
    public void testChildErrorsSuppressedWhenFailFastTrue() throws Exception {
        // Same schema as previous test
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\",\"maxLength\":1}, {\"type\":\"string\",\"minLength\":5} ] }";
        OneOfValidator validator = getOneOfValidator(schemaJson);

        JsonNode instance = MAPPER.readTree("\"abc\""); // length 3 → violates both

        ExecutionContext ctx = newExecutionContext();
        // Enable fast‑fail – child errors must not be aggregated
        ctx.setFailFast(true);

        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, rootPath());

        // Only the top‑level oneOf message should be present
        assertEquals("Only the top‑level oneOf message should be reported when failFast is true", 1, messages.size());
        ValidationMessage msg = messages.iterator().next();
        assertEquals("oneOf", msg.getMessageKey());
    }
}
###Test END##