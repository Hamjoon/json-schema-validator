###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Typical usage – exactly one subschema matches.</li>
 *   <li>Edge cases – none matches, more than one matches.</li>
 *   <li>Error handling – fail‑fast mode throws {@link JsonSchemaException}.</li>
 *   <li>State handling – {@link ValidatorState} flags are correctly reset.</li>
 * </ul>
 *
 * <p>All objects are created using the public constructors and factory methods
 * that belong to the library – no mocks or subclassing of abstract types are used.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private JsonSchema parentSchema;   // a dummy parent – required by the constructor
    private ExecutionContext executionContext;

    /**
     * Helper that creates a minimal but functional {@link ValidationContext}.
     *
     * <p>The library provides a default {@link JsonSchemaFactory} that can be
     * used together with the default meta‑schema.  Those factories are created
     * via their public no‑arg constructors (or static factory methods) – the
     * exact signatures are not shown in the excerpt but are part of the public
     * API, therefore they can be used directly.
     *
     * <p>For the purpose of this test we initialise the context with the
     * default factories and an empty {@link SchemaValidatorsConfig}.
     */
    @Before
    public void setUp() {
        // --------------------------------------------------------------------
        // 1. Create the required factories (the library supplies public no‑arg
        //    constructors or static factory methods for each of them).
        // --------------------------------------------------------------------
        URIFactory uriFactory = new URIFactory();                     // public no‑arg ctor
        URNFactory urnFactory = new URNFactory();                     // public no‑arg ctor
        JsonMetaSchema metaSchema = JsonMetaSchema.getV201909();      // static factory
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(); // static factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig(); // public no‑arg ctor

        // --------------------------------------------------------------------
        // 2. Assemble the ValidationContext.
        // --------------------------------------------------------------------
        validationContext = new ValidationContext(uriFactory, urnFactory,
                metaSchema, schemaFactory, config);

        // --------------------------------------------------------------------
        // 3. Create a trivial parent schema – required only to satisfy the
        //    OneOfValidator constructor signature.
        // --------------------------------------------------------------------
        JsonNode emptyNode = MAPPER.createObjectNode(); // "{}"
        parentSchema = new JsonSchema(validationContext, URI.create("urn:parent"), emptyNode);

        // --------------------------------------------------------------------
        // 4. Create an ExecutionContext that carries a fresh CollectorContext.
        // --------------------------------------------------------------------
        executionContext = new ExecutionContext(new CollectorContext());
    }

    /**
     * Utility that builds a {@link OneOfValidator} from a JSON array describing
     * the subschemas.
     *
     * @param oneOfArray JSON array node containing the subschemas
     * @return a ready‑to‑use {@link OneOfValidator}
     */
    private OneOfValidator buildValidator(JsonNode oneOfArray) {
        return new OneOfValidator("#/oneOf", oneOfArray, parentSchema, validationContext);
    }

    /**
     * Test case: exactly one subschema validates successfully.
     *
     * <p>The schema consists of two alternatives:
     * <pre>
     *   - {"type":"string"}
     *   - {"type":"integer"}
     * </pre>
     * The instance {@code "hello"} matches only the first, therefore the
     * validator must return an empty error set and set the {@code matchedNode}
     * flag to {@code true}.
     */
    @Test
    public void testOneOfSingleMatch() throws Exception {
        // --------------------------------------------------------------
        // Build the "oneOf" schema array.
        // --------------------------------------------------------------
        JsonNode schemaArray = MAPPER.readTree("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        OneOfValidator validator = buildValidator(schemaArray);

        // --------------------------------------------------------------
        // Validate a JSON string – matches only the first subschema.
        // --------------------------------------------------------------
        JsonNode instance = MAPPER.readTree("\"hello\"");

        Set<ValidationMessage> errors = validator.validate(executionContext, instance,
                instance, "$");

        // --------------------------------------------------------------
        // Assertions
        // --------------------------------------------------------------
        assertTrue("No validation errors expected when exactly one subschema matches", errors.isEmpty());

        // Verify that the internal state has been reset (matchedNode = true)
        ValidatorState state = (ValidatorState) executionContext.getCollectorContext()
                .get(ValidatorState.VALIDATOR_STATE_KEY);
        assertTrue("ValidatorState.matchedNode must be true after successful validation",
                state.isMatchedNode());
    }

    /**
     * Test case: none of the subschemas match.
     *
     * <p>Using the same schema as the previous test, the instance {@code true}
     * does not satisfy either {@code string} nor {@code integer}.  The validator
     * must report a single "oneOf" error with the argument {@code 0}
     * (the number of matching subschemas) and also include the errors from
     * the individual subschemas.
     */
    @Test
    public void testOneOfNoMatch() throws Exception {
        JsonNode schemaArray = MAPPER.readTree("[{\"type\":\"string\"},{\"type\":\"integer\"}]");
        OneOfValidator validator = buildValidator(schemaArray);

        JsonNode instance = MAPPER.readTree("true"); // boolean

        Set<ValidationMessage> errors = validator.validate(executionContext, instance,
                instance, "$");

        // --------------------------------------------------------------
        // The validator must contain exactly one "oneOf" error.
        // --------------------------------------------------------------
        assertEquals("Exactly one ValidationMessage expected (the oneOf error)", 1, errors.size());

        ValidationMessage message = errors.iterator().next();
        assertEquals("Error code must correspond to ONE_OF", "oneOf", message.getCode());
        assertEquals("Argument must be the number of matching schemas (0)", "0", message.getArguments()[0]);
    }

    /**
     * Test case: more than one subschema matches.
     *
     * <p>The schema contains two overlapping alternatives:
     * <pre>
     *   - {"type":"number"}
     *   - {"minimum":0}
     * </pre>
     * The instance {@code 5} satisfies both, therefore the validator must
     * produce a "oneOf" error with argument {@code 2} and must short‑circuit
     * after detecting the second match.
     */
    @Test
    public void testOneOfMultipleMatches() throws Exception {
        JsonNode schemaArray = MAPPER.readTree("[{\"type\":\"number\"},{\"minimum\":0}]");
        OneOfValidator validator = buildValidator(schemaArray);

        JsonNode instance = MAPPER.readTree("5");

        Set<ValidationMessage> errors = validator.validate(executionContext, instance,
                instance, "$");

        assertEquals("Exactly one ValidationMessage expected (the oneOf error)", 1, errors.size());

        ValidationMessage message = errors.iterator().next();
        assertEquals("Error code must correspond to ONE_OF", "oneOf", message.getCode());
        assertEquals("Argument must be the number of matching schemas (2)", "2", message.getArguments()[0]);
    }

    /**
     * Test case: validator is configured with {@code failFast = true}.
     *
     * <p>When {@code failFast} is enabled the validator must throw a
     * {@link JsonSchemaException} as soon as the number of matching subschemas
     * is not exactly one.
     */
    @Test(expected = JsonSchemaException.class)
    public void testOneOfFailFastThrowsException() throws Exception {
        // --------------------------------------------------------------
        // Build a schema that will not match the instance.
        // --------------------------------------------------------------
        JsonNode schemaArray = MAPPER.readTree("[{\"type\":\"string\"}]");
        OneOfValidator validator = buildValidator(schemaArray);

        // --------------------------------------------------------------
        // Enable fail‑fast on the underlying BaseJsonValidator.
        // --------------------------------------------------------------
        // The protected field 'failFast' is set through the constructor of
        // ValidationMessageHandler (the superclass).  The public API provides a
        // way to enable it via the ValidationMessageHandler constructor that
        // accepts a boolean.  Since OneOfValidator does not expose a setter,
        // we obtain a new instance via reflection for the purpose of the test.
        java.lang.reflect.Field failFastField = BaseJsonValidator.class.getDeclaredField("failFast");
        failFastField.setAccessible(true);
        failFastField.setBoolean(validator, true);

        // --------------------------------------------------------------
        // Validate a non‑matching instance – expect exception.
        // --------------------------------------------------------------
        JsonNode instance = MAPPER.readTree("123"); // number, not a string

        validator.validate(executionContext, instance, instance, "$");
    }

    /**
     * Test case: ensure that after a validation run the {@link ValidatorState}
     * flags are restored to their defaults (complexValidator = false,
     * matchedNode = true).
     */
    @Test
    public void testValidatorStateResetAfterValidation() throws Exception {
        JsonNode schemaArray = MAPPER.readTree("[{\"type\":\"string\"}]");
        OneOfValidator validator = buildValidator(schemaArray);

        JsonNode instance = MAPPER.readTree("\"test\"");

        // Force the state into a non‑default configuration before validation.
        ValidatorState state = (ValidatorState) executionContext.getCollectorContext()
                .get(ValidatorState.VALIDATOR_STATE_KEY);
        state.setComplexValidator(true);
        state.setMatchedNode(false);

        // Execute validation – should succeed (single match).
        Set<ValidationMessage> errors = validator.validate(executionContext, instance,
                instance, "$");
        assertTrue(errors.isEmpty());

        // After validation the state must be reset.
        assertFalse("complexValidator flag must be reset to false", state.isComplexValidator());
        assertTrue("matchedNode flag must be reset to true", state.isMatchedNode());
    }

    /**
     * Test case: the {@code walk} method delegates correctly to {@code validate}
     * when {@code shouldValidateSchema} is {@code true}, and otherwise
     * walks each subschema without collecting messages.
     */
    @Test
    public void testWalkDelegation() throws Exception {
        JsonNode schemaArray = MAPPER.readTree("[{\"type\":\"string\"},{\"type\":\"integer\"}]");
        OneOfValidator validator = buildValidator(schemaArray);

        JsonNode instance = MAPPER.readTree("\"walk-test\"");

        // --------------------------------------------------------------
        // Walk with validation – should behave like validate().
        // --------------------------------------------------------------
        Set<ValidationMessage> walkMessages = validator.walk(executionContext, instance,
                instance, "$", true);
        // The instance matches only the first subschema → no errors.
        assertTrue(walkMessages.isEmpty());

        // --------------------------------------------------------------
        // Walk without validation – should return an empty set regardless
        // of match because messages are not collected.
        // --------------------------------------------------------------
        Set<ValidationMessage> walkNoValidate = validator.walk(executionContext, instance,
                instance, "$", false);
        assertTrue("walk without validation must return an empty set", walkNoValidate.isEmpty());
    }
}
```
###Test END##