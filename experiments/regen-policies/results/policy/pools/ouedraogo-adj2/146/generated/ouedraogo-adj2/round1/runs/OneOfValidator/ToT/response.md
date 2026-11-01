###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Comprehensive JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * <p>The tests cover the most important execution paths:
 *
 * <ul>
 *   <li>Exactly one subschema validates – no error is reported.</li>
 *   <li>No subschema validates – a generic {@code oneOf} error is reported.</li>
 *   <li>More than one subschema validates – an {@code oneOf.indexes} error is reported
 *       containing the matching schema indexes.</li>
 *   <li>Short‑circuit optimisation is disabled when the parent schema contains
 *       {@code unevaluatedProperties} or {@code unevaluatedItems} – the validator
 *       continues to evaluate all alternatives.</li>
 * </ul>
 *
 * <p>The test class builds real {@link JsonSchema} objects through the public
 * factory API – no mocking or subclassing of library classes is performed.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext ctx;

    @Before
    public void setUp() {
        // Build a minimal ValidationContext.  All arguments have public constructors
        // in the library, therefore they can be instantiated directly.
        JsonMetaSchema meta = JsonMetaSchema.getV7();                         // draft‑07 meta‑schema
        JsonSchemaFactory factory = new JsonSchemaFactory();                  // default factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();        // default config
        this.ctx = new ValidationContext(meta, factory, config);
    }

    /**
     * Helper that creates a {@link JsonSchema} from a JSON string.
     */
    private JsonSchema loadSchema(String json) throws IOException {
        JsonNode schemaNode = MAPPER.readTree(json);
        // The root location for a top‑level schema is {@link SchemaLocation#DOCUMENT}
        return ctx.newSchema(SchemaLocation.DOCUMENT, new JsonNodePath(JsonNodePath.PathType.ROOT), schemaNode, null);
    }

    /**
     * Extracts the {@link OneOfValidator} from a compiled {@link JsonSchema}.
     */
    private OneOfValidator getOneOfValidator(JsonSchema schema) {
        Optional<JsonValidator> opt = schema.getValidators().stream()
                .filter(v -> v instanceof OneOfValidator)
                .findFirst();
        assertTrue("Schema must contain a OneOfValidator", opt.isPresent());
        return (OneOfValidator) opt.get();
    }

    /**
     * Executes validation using the supplied {@link OneOfValidator} and returns the
     * set of {@link ValidationMessage}s.
     */
    private Set<ValidationMessage> validate(OneOfValidator validator, JsonNode instance) {
        ExecutionContext exec = new ExecutionContext(); // default execution context
        // The root node and the instance node are the same for these unit tests.
        return validator.validate(exec, instance, instance, new JsonNodePath(JsonNodePath.PathType.ROOT));
    }

    /** --------------------------------------------------------------------- **/
    /** 1. Exactly one subschema matches – no error should be reported.          **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testSingleMatch() throws Exception {
        String schema = "{"
                + "  \"oneOf\": ["
                + "    { \"type\": \"string\" },"
                + "    { \"type\": \"integer\" }"
                + "  ]"
                + "}";
        JsonSchema jsonSchema = loadSchema(schema);
        OneOfValidator validator = getOneOfValidator(jsonSchema);

        JsonNode instance = MAPPER.readTree("\"Hello world\"");
        Set<ValidationMessage> messages = validate(validator, instance);
        assertTrue("When exactly one subschema matches, the result must be empty", messages.isEmpty());

        // Also test the integer alternative
        JsonNode intInstance = MAPPER.readTree("42");
        messages = validate(validator, intInstance);
        assertTrue(messages.isEmpty());
    }

    /** --------------------------------------------------------------------- **/
    /** 2. No subschema matches – a generic oneOf error must be returned.      **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testNoMatch() throws Exception {
        String schema = "{"
                + "  \"oneOf\": ["
                + "    { \"type\": \"string\" },"
                + "    { \"type\": \"integer\" }"
                + "  ]"
                + "}";
        JsonSchema jsonSchema = loadSchema(schema);
        OneOfValidator validator = getOneOfValidator(jsonSchema);

        JsonNode instance = MAPPER.readTree("true"); // boolean – matches neither
        Set<ValidationMessage> messages = validate(validator, instance);
        assertEquals(1, messages.size());

        ValidationMessage msg = messages.iterator().next();
        assertEquals("oneOf", msg.getMessageKey()); // generic message key
        // the message arguments contain the number of valid schemas (0)
        assertArrayEquals(new Object[] { "0", "" }, msg.getArguments());
    }

    /** --------------------------------------------------------------------- **/
    /** 3. More than one subschema matches – an indexed error must be reported. **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testMultipleMatches() throws Exception {
        // Two empty subschemas always succeed → two matches.
        String schema = "{"
                + "  \"oneOf\": [ {}, {} ]"
                + "}";
        JsonSchema jsonSchema = loadSchema(schema);
        OneOfValidator validator = getOneOfValidator(jsonSchema);

        JsonNode instance = MAPPER.readTree("\"any value\"");
        Set<ValidationMessage> messages = validate(validator, instance);
        assertEquals(1, messages.size());

        ValidationMessage msg = messages.iterator().next();
        // When more than one schema matches the validator uses the "oneOf.indexes"
        // message key and lists the matching indexes.
        assertEquals("oneOf.indexes", msg.getMessageKey());

        // Verify that the indexes "0, 1" are present in the arguments.
        Object[] args = msg.getArguments();
        assertEquals("2", args[0]);                 // number of valid schemas
        assertEquals("0, 1", args[1]);               // comma‑separated indexes
    }

    /** --------------------------------------------------------------------- **/
    /** 4. Short‑circuit optimisation must be disabled when the parent schema   **/
    /**    contains an unevaluated keyword.                                    **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testCanShortCircuitDisabledByParentKeyword() throws Exception {
        // Parent schema contains "unevaluatedProperties": {} which forces
        // canShortCircuit() to return false.
        String schema = "{"
                + "  \"unevaluatedProperties\": {},"
                + "  \"oneOf\": [ {}, {} ]"
                + "}";
        JsonSchema jsonSchema = loadSchema(schema);
        OneOfValidator validator = getOneOfValidator(jsonSchema);

        // Verify via reflection that the cached flag is false.
        java.lang.reflect.Field flagField = OneOfValidator.class.getDeclaredField("canShortCircuit");
        flagField.setAccessible(true);
        // The flag is lazily computed; trigger it by calling the protected method.
        java.lang.reflect.Method shortCircuitMethod = OneOfValidator.class.getDeclaredMethod("canShortCircuit");
        shortCircuitMethod.setAccessible(true);
        boolean canShortCircuit = (Boolean) shortCircuitMethod.invoke(validator);
        assertFalse("canShortCircuit must be false because parent contains unevaluatedProperties", canShortCircuit);

        // Even though both subschemas match, the validator must still evaluate all
        // alternatives (the result should still be the indexed error).
        JsonNode instance = MAPPER.readTree("null");
        Set<ValidationMessage> messages = validate(validator, instance);
        assertEquals(1, messages.size());
        ValidationMessage msg = messages.iterator().next();
        assertEquals("oneOf.indexes", msg.getMessageKey());
        Object[] args = msg.getArguments();
        assertEquals("2", args[0]);                 // two matching schemas
        assertEquals("0, 1", args[1]);               // both indexes reported
    }

    /** --------------------------------------------------------------------- **/
    /** 5. The {@code walk} method must delegate correctly depending on the      **/
    /**    {@code shouldValidateSchema} flag.                                   **/
    /** --------------------------------------------------------------------- **/
    @Test
    public void testWalkDelegation() throws Exception {
        String schema = "{"
                + "  \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ]"
                + "}";
        JsonSchema jsonSchema = loadSchema(schema);
        OneOfValidator validator = getOneOfValidator(jsonSchema);

        ExecutionContext exec = new ExecutionContext();
        JsonNode instance = MAPPER.readTree("\"abc\"");

        // When shouldValidateSchema = true the validator must evaluate its
        // subschemas (i.e. behave like {@link #validate}).
        Set<ValidationMessage> walkResult = validator.walk(exec, instance, instance,
                new JsonNodePath(JsonNodePath.PathType.ROOT), true);
        assertTrue("walk with validation should succeed for a matching instance", walkResult.isEmpty());

        // When shouldValidateSchema = false the validator must simply walk
        // each child schema without collecting errors.  Because the child
        // validators also return empty sets for the given instance, the walk
        // result stays empty.
        Set<ValidationMessage> walkNoValidate = validator.walk(exec, instance, instance,
                new JsonNodePath(JsonNodePath.PathType.ROOT), false);
        assertTrue("walk without validation must also return an empty set", walkNoValidate.isEmpty());
    }
}
```
###Test END##