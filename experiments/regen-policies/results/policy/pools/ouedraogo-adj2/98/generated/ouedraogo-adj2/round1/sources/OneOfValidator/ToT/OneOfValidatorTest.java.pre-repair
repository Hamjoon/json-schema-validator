package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * JUnit‑4 test suite for {@link OneOfValidator}.  The tests exercise the
 * constructor (including the error path), the {@code validate} method for the
 * three logical outcomes (exactly one matching schema, none matching, more than
 * one matching) and the {@code walk} and {@code preloadJsonSchema} helper
 * methods.
 *
 * <p>Only public APIs that appear in the source of {@code OneOfValidator} or
 * in the dependency signatures supplied in the prompt are used – no mocks,
 * no external factories and no subclassing of library classes.</p>
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** a very small {@link ValidationContext} – the constructor tolerates null
     *  arguments for the purpose of these unit tests. */
    private ValidationContext validationContext;

    /** a dummy {@link JsonNodePath}.  The {@code ROOT} constant exists in the
     *  library’s {@code PathType} enum. */
    private JsonNodePath rootPath;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // The ValidationContext ctor signature is
        // ValidationContext(JsonMetaSchema, JsonSchemaFactory, SchemaValidatorsConfig)
        // Passing nulls is sufficient because the tests never ask the context
        // for a meta‑schema, a factory or a config.
        validationContext = new ValidationContext(null, null, null);
        rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
    }

    /**
     * Helper that creates a {@link OneOfValidator} from a JSON array describing
     * the subschemas.
     */
    private OneOfValidator createValidator(String jsonArray) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(jsonArray);
        return new OneOfValidator(
                SchemaLocation.DOCUMENT,            // any location works for the test
                rootPath,
                schemaNode,
                null,                               // no parent schema required
                validationContext);
    }

    /**
     * The constructor must reject a non‑array schema node.
     */
    @Test
    public void constructorRejectsNonArrayNode() throws Exception {
        thrown.expect(JsonSchemaException.class);
        JsonNode notArray = MAPPER.readTree("{\"type\":\"string\"}");
        new OneOfValidator(SchemaLocation.DOCUMENT, rootPath, notArray, null, validationContext);
    }

    /**
     * When exactly one subschema validates the instance, {@code validate}
     * returns an empty set (no errors).
     */
    @Test
    public void validateExactlyOneSchemaMatches() throws Exception {
        // oneOf: [{type:"string"}, {type:"number"}]
        OneOfValidator validator = createValidator("[{\"type\":\"string\"},{\"type\":\"number\"}]");

        JsonNode instance = MAPPER.readTree("\"hello\""); // a string
        ExecutionContext exec = new ExecutionContext();

        Set<ValidationMessage> result = validator.validate(exec, instance, instance, rootPath);
        assertTrue("Expected no validation errors", result.isEmpty());
    }

    /**
     * When none of the subschemas validates the instance the validator must
     * return a single {@code oneOf} error (messageKey = "oneOf").
     */
    @Test
    public void validateNoSchemaMatches() throws Exception {
        // oneOf: [{type:"string"}, {type:"number"}]
        OneOfValidator validator = createValidator("[{\"type\":\"string\"},{\"type\":\"number\"}]");

        JsonNode instance = MAPPER.readTree("true"); // a boolean – matches neither
        ExecutionContext exec = new ExecutionContext();

        Set<ValidationMessage> result = validator.validate(exec, instance, instance, rootPath);
        assertEquals("Exactly one error expected", 1, result.size());

        ValidationMessage msg = result.iterator().next();
        assertEquals("Message key must be 'oneOf'", "oneOf", msg.getMessageKey());

        // the arguments array contains the number of valid schemas (0) and an empty
        // string for the indexes list.
        Object[] args = msg.getArguments();
        assertEquals("First argument should be the count of valid schemas", "0", args[0]);
        assertEquals("Second argument should be empty when none match", "", args[1]);
    }

    /**
     * When more than one subschema validates the instance the validator must
     * report a {@code oneOf.indexes} error that lists the matching indexes.
     * The short‑circuit optimisation stops after the second match, but the
     * overall result must still be a single error message.
     */
    @Test
    public void validateMultipleSchemasMatchShortCircuit() throws Exception {
        // both subschemas accept a string: type and enum
        OneOfValidator validator = createValidator("[{\"type\":\"string\"},{\"enum\":[\"hello\",\"world\"]}]");

        JsonNode instance = MAPPER.readTree("\"hello\"");
        ExecutionContext exec = new ExecutionContext();

        Set<ValidationMessage> result = validator.validate(exec, instance, instance, rootPath);
        assertEquals("Exactly one aggregated error expected", 1, result.size());

        ValidationMessage msg = result.iterator().next();
        assertEquals("Message key must be 'oneOf.indexes'", "oneOf.indexes", msg.getMessageKey());

        // arguments[0] = number of valid schemas (2)
        // arguments[1] = comma‑separated list of matching indexes ("0, 1")
        Object[] args = msg.getArguments();
        assertEquals("Two schemas should match", "2", args[0]);
        assertEquals("Indexes list must contain both matches", "0, 1", args[1]);
    }

    /**
     * The {@code walk} method should delegate to {@code validate} when the
     * {@code shouldValidateSchema} flag is {@code true}.  The result sets must
     * be identical.
     */
    @Test
    public void walkDelegatesToValidateWhenFlagTrue() throws Exception {
        OneOfValidator validator = createValidator("[{\"type\":\"string\"},{\"type\":\"number\"}]");

        JsonNode instance = MAPPER.readTree("\"test\"");
        ExecutionContext exec = new ExecutionContext();

        Set<ValidationMessage> viaValidate = validator.validate(exec, instance, instance, rootPath);
        Set<ValidationMessage> viaWalk = validator.walk(exec, instance, instance, rootPath, true);

        assertEquals("walk(true) must return the same set as validate()", viaValidate, viaWalk);
    }

    /**
     * The {@code preloadJsonSchema} method must initialise child schemas and
     * cache the short‑circuit flag.  After calling it, a subsequent call to
     * {@code canShortCircuit()} must return the cached value without
     * recomputation.
     */
    @Test
    public void preloadInitialisesChildrenAndCachesShortCircuit() throws Exception {
        // Simple oneOf – no unevaluated* validators, so short‑circuit is true.
        OneOfValidator validator = createValidator("[{\"type\":\"string\"}]");

        // before preload the internal cache is null (reflection used only for
        // verification – not part of the public API)
        java.lang.reflect.Field cacheField = OneOfValidator.class.getDeclaredField("canShortCircuit");
        cacheField.setAccessible(true);
        assertNull("Cache should be null before preload", cacheField.get(validator));

        validator.preloadJsonSchema();

        // after preload the cache must be non‑null and true
        Boolean cached = (Boolean) cacheField.get(validator);
        assertNotNull("Cache must be populated after preload", cached);
        assertTrue("Short‑circuit should be enabled for this schema", cached);
    }
}
