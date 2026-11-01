package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Typical successful validation when exactly one subschema matches.</li>
 *   <li>Failure when no subschema matches.</li>
 *   <li>Failure when more than one subschema matches (the “one‑of” rule is violated).</li>
 *   <li>Edge case of an empty {@code oneOf} array.</li>
 *   <li>Verification that {@code preloadJsonSchema()} does not throw and correctly
 *       initialises child schemas.</li>
 * </ul>
 *
 * <p>All objects are created through the public constructors / factory methods
 * that are part of the library (no mocking or subclassing of library types).
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Helper that builds a {@link JsonSchema} from a JSON schema string using the
     * default {@link JsonSchemaFactory}. The returned schema contains a {@link OneOfValidator}
     * when the supplied schema defines a {@code oneOf} keyword.
     */
    private JsonSchema compileSchema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        return factory.getSchema(schemaJson);
    }

    /**
     * Helper that parses a JSON instance (as a string) into a {@link JsonNode}.
     */
    private JsonNode parseInstance(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    /** -----------------------------------------------------------------------
     *  1️⃣  Exactly one subschema matches – string value.
     * ----------------------------------------------------------------------- */
    @Test
    public void testValidate_StringMatchesExactlyOneSchema() throws Exception {
        // oneOf: string OR integer
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";
        JsonSchema jsonSchema = compileSchema(schema);

        JsonNode instance = parseInstance("\"hello world\""); // a JSON string

        Set<ValidationMessage> messages = jsonSchema.validate(instance);
        // No validation errors – exactly one subschema (the string schema) matches.
        assertTrue("Expected no validation errors", messages.isEmpty());
    }

    /** -----------------------------------------------------------------------
     *  2️⃣  Exactly one subschema matches – integer value.
     * ----------------------------------------------------------------------- */
    @Test
    public void testValidate_IntegerMatchesExactlyOneSchema() throws Exception {
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";
        JsonSchema jsonSchema = compileSchema(schema);

        JsonNode instance = parseInstance("42"); // a JSON integer

        Set<ValidationMessage> messages = jsonSchema.validate(instance);
        assertTrue("Expected no validation errors", messages.isEmpty());
    }

    /** -----------------------------------------------------------------------
     *  3️⃣  No subschema matches – boolean value.
     * ----------------------------------------------------------------------- */
    @Test
    public void testValidate_NoMatchingSchema() throws Exception {
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";
        JsonSchema jsonSchema = compileSchema(schema);

        JsonNode instance = parseInstance("true"); // a JSON boolean

        Set<ValidationMessage> messages = jsonSchema.validate(instance);
        assertFalse("Expected validation errors because no subschema matches", messages.isEmpty());

        // The OneOfValidator reports the number of matching schemas (0) as an argument.
        ValidationMessage message = messages.iterator().next();
        assertEquals("0", message.getArguments()[0]);
    }

    /** -----------------------------------------------------------------------
     *  4️⃣  More than one subschema matches – violation of the “one‑of” rule.
     * ----------------------------------------------------------------------- */
    @Test
    public void testValidate_MultipleMatchingSchemas() throws Exception {
        // Both subschemas accept the value "a".
        String schema = "{ \"oneOf\": [ " +
                "{ \"enum\": [\"a\", \"b\"] }, " +
                "{ \"enum\": [\"a\"] } " +
                "] }";
        JsonSchema jsonSchema = compileSchema(schema);

        JsonNode instance = parseInstance("\"a\""); // matches both enums

        Set<ValidationMessage> messages = jsonSchema.validate(instance);
        assertFalse("Expected validation errors because more than one subschema matches", messages.isEmpty());

        ValidationMessage message = messages.iterator().next();
        // The validator should report that 2 schemas matched.
        assertEquals("2", message.getArguments()[0]);
    }

    /** -----------------------------------------------------------------------
     *  5️⃣  Edge case – empty {@code oneOf} array (always invalid).
     * ----------------------------------------------------------------------- */
    @Test
    public void testValidate_EmptyOneOfArray() throws Exception {
        String schema = "{ \"oneOf\": [] }";
        JsonSchema jsonSchema = compileSchema(schema);

        JsonNode instance = parseInstance("null"); // any value

        Set<ValidationMessage> messages = jsonSchema.validate(instance);
        assertFalse("Empty oneOf must always produce an error", messages.isEmpty());

        ValidationMessage message = messages.iterator().next();
        // Zero matching schemas is reported.
        assertEquals("0", message.getArguments()[0]);
    }

    /** -----------------------------------------------------------------------
     *  6️⃣  preloadJsonSchema() – should initialise child schemas without throwing.
     * ----------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchema_DoesNotThrow() throws Exception {
        // The schema contains two simple subschemas. Pre‑loading must initialise them.
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";
        JsonSchema jsonSchema = compileSchema(schema);

        // Retrieve the underlying OneOfValidator via reflection (the library does not
        // expose it directly, but it is guaranteed to be present when the schema has a
        // oneOf keyword).
        OneOfValidator validator = null;
        for (JsonValidator v : jsonSchema.getValidators()) {
            if (v instanceof OneOfValidator) {
                validator = (OneOfValidator) v;
                break;
            }
        }
        assertNotNull("OneOfValidator should be present in the compiled schema", validator);

        // The method should run without throwing any exception.
        validator.preloadJsonSchema();
    }

    /** -----------------------------------------------------------------------
     *  7️⃣  walk() with shouldValidateSchema = false – should delegate to child
     *      schemas without collecting messages.
     * ----------------------------------------------------------------------- */
    @Test
    public void testWalk_DelegatesWhenShouldValidateSchemaIsFalse() throws Exception {
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";
        JsonSchema jsonSchema = compileSchema(schema);
        JsonNode instance = parseInstance("\"text\"");

        // Obtain the OneOfValidator as in the previous test.
        OneOfValidator validator = null;
        for (JsonValidator v : jsonSchema.getValidators()) {
            if (v instanceof OneOfValidator) {
                validator = (OneOfValidator) v;
                break;
            }
        }
        assertNotNull(validator);

        ExecutionContext ctx = new ExecutionContext(); // default context
        JsonNodePath path = new JsonNodePath(JsonNodePath.PathType.ROOT);

        // Walk with validation disabled – we expect an empty result set.
        Set<ValidationMessage> walkResult = validator.walk(ctx, instance, instance, path, false);
        assertTrue("walk with shouldValidateSchema=false must return an empty set", walkResult.isEmpty());
    }

    /** -----------------------------------------------------------------------
     *  8️⃣  walk() with shouldValidateSchema = true – should behave like validate().
     * ----------------------------------------------------------------------- */
    @Test
    public void testWalk_ValidatesWhenShouldValidateSchemaIsTrue() throws Exception {
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";
        JsonSchema jsonSchema = compileSchema(schema);
        JsonNode instance = parseInstance("true"); // boolean – matches none

        // Obtain the validator.
        OneOfValidator validator = null;
        for (JsonValidator v : jsonSchema.getValidators()) {
            if (v instanceof OneOfValidator) {
                validator = (OneOfValidator) v;
                break;
            }
        }
        assertNotNull(validator);

        ExecutionContext ctx = new ExecutionContext();
        JsonNodePath path = new JsonNodePath(JsonNodePath.PathType.ROOT);

        Set<ValidationMessage> walkResult = validator.walk(ctx, instance, instance, path, true);
        assertFalse("walk with shouldValidateSchema=true must return validation errors", walkResult.isEmpty());

        // The error must indicate that zero schemas matched.
        ValidationMessage message = walkResult.iterator().next();
        assertEquals("0", message.getArguments()[0]);
    }
}
