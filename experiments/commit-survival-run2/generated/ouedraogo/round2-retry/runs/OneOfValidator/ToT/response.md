###Test START##
```java
/*
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.OneOfValidator}.
 *
 * The tests cover:
 *   1. Normal behaviour – exactly one subschema matches.
 *   2. No subschema matches.
 *   3. More than one subschema matches.
 *   4. Edge case – empty {@code oneOf} array.
 *   5. Fail‑fast mode – {@link com.networknt.schema.JsonSchemaException} is thrown.
 *
 * The tests use the public API of the NetworkNT JSON‑Schema library
 * (JsonSchemaFactory, JsonSchema, ValidationContext, etc.) so the
 * {@code OneOfValidator} is exercised in the same way it is used at
 * runtime.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link OneOfValidator}.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Helper that builds a {@link JsonSchema} from a JSON string. */
    private JsonSchema getSchema(String schemaJson) {
        return getSchema(schemaJson, false);
    }

    /** Helper that builds a {@link JsonSchema} with an optional fail‑fast flag. */
    private JsonSchema getSchema(String schemaJson, boolean failFast) {
        try {
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            ValidationContext validationContext = ValidationContext.newBuilder()
                    .setFailFast(failFast)
                    .build();
            JsonSchemaFactory factory = new JsonSchemaFactory(validationContext);
            return factory.getSchema(schemaNode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Parses a JSON instance (the data to be validated). */
    private JsonNode json(String instanceJson) {
        try {
            return MAPPER.readTree(instanceJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- *
     *  1. Exactly one subschema matches – valid cases
     * --------------------------------------------------------------------- */

    @Test
    public void whenStringMatchesOnlyFirstSchema_thenNoErrors() {
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"number\" } ] }";
        JsonSchema jsonSchema = getSchema(schema);

        Set<ValidationMessage> errors = jsonSchema.validate(json("\"hello\""));
        assertTrue("A matching string should produce no validation errors", errors.isEmpty());
    }

    @Test
    public void whenNumberMatchesOnlySecondSchema_thenNoErrors() {
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"number\" } ] }";
        JsonSchema jsonSchema = getSchema(schema);

        Set<ValidationMessage> errors = jsonSchema.validate(json("42"));
        assertTrue("A matching number should produce no validation errors", errors.isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  2. No subschema matches – invalid case
     * --------------------------------------------------------------------- */

    @Test
    public void whenNoSchemaMatches_thenOneOfErrorIsReturned() {
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"number\" } ] }";
        JsonSchema jsonSchema = getSchema(schema);

        Set<ValidationMessage> errors = jsonSchema.validate(json("true"));
        assertFalse("Validation must fail when nothing matches", errors.isEmpty());

        // The message key for ONE_OF is defined in ValidatorTypeCode.ONE_OF
        ValidationMessage first = errors.iterator().next();
        assertEquals("oneOf", first.getType()); // type should be "oneOf"
        assertTrue("Error message must contain the number of valid schemas (0)",
                first.getMessage().contains("0"));
    }

    /* --------------------------------------------------------------------- *
     *  3. More than one subschema matches – invalid case
     * --------------------------------------------------------------------- */

    @Test
    public void whenMultipleSchemasMatch_thenOneOfErrorIsReturned() {
        // Both subschemas accept any positive integer:
        //   – { "type": "integer" }
        //   – { "minimum": 0 }
        String schema = "{ \"oneOf\": [ { \"type\": \"integer\" }, { \"minimum\": 0 } ] }";
        JsonSchema jsonSchema = getSchema(schema);

        Set<ValidationMessage> errors = jsonSchema.validate(json("5"));
        assertFalse("Validation must fail when more than one schema matches", errors.isEmpty());

        ValidationMessage first = errors.iterator().next();
        assertEquals("oneOf", first.getType());
        assertTrue("Error message must contain the number of valid schemas (2)",
                first.getMessage().contains("2"));
    }

    /* --------------------------------------------------------------------- *
     *  4. Edge case – empty oneOf array
     * --------------------------------------------------------------------- */

    @Test
    public void whenOneOfArrayIsEmpty_thenAlwaysFails() {
        String schema = "{ \"oneOf\": [] }";
        JsonSchema jsonSchema = getSchema(schema);

        Set<ValidationMessage> errors = jsonSchema.validate(json("\"anything\""));
        assertFalse("Empty oneOf should always produce a validation error", errors.isEmpty());

        ValidationMessage first = errors.iterator().next();
        assertEquals("oneOf", first.getType());
        assertTrue("Error message must indicate 0 valid schemas", first.getMessage().contains("0"));
    }

    /* --------------------------------------------------------------------- *
     *  5. Fail‑fast mode – exception is thrown on first error
     * --------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void whenFailFastIsEnabled_andValidationFails_thenExceptionIsThrown() {
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"number\" } ] }";
        JsonSchema jsonSchema = getSchema(schema, true); // failFast = true

        // This instance does not match any subschema, so validation should abort
        // with a JsonSchemaException rather than returning a Set.
        jsonSchema.validate(json("false"));
    }

    /* --------------------------------------------------------------------- *
     *  6. Verify that child errors are propagated when validation fails
     * --------------------------------------------------------------------- */

    @Test
    public void whenValidationFails_childErrorsAreReturnedAlongsideOneOfError() {
        // Sub‑schema 1: a required property "name" (string)
        // Sub‑schema 2: a required property "age" (integer)
        String schema = "{ \"oneOf\": [ " +
                "  { \"type\": \"object\", \"required\": [\"name\"], \"properties\": { \"name\": {\"type\":\"string\"} } }," +
                "  { \"type\": \"object\", \"required\": [\"age\"],  \"properties\": { \"age\":  {\"type\":\"integer\"} } }" +
                "] }";

        JsonSchema jsonSchema = getSchema(schema);

        // Instance has none of the required properties – both subschemas will emit errors.
        Set<ValidationMessage> errors = jsonSchema.validate(json("{\"foo\":\"bar\"}"));
        assertFalse("Overall validation must fail", errors.isEmpty());

        // There must be at least one ONE_OF message and also the child messages (missing required).
        boolean foundOneOf = false;
        boolean foundRequired = false;
        for (ValidationMessage vm : errors) {
            if ("oneOf".equals(vm.getType())) {
                foundOneOf = true;
            }
            if (vm.getMessage().contains("required")) {
                foundRequired = true;
            }
        }
        assertTrue("OneOf error should be present", foundOneOf);
        assertTrue("Child required‑property errors should also be present", foundRequired);
    }
}
```
###Test END##