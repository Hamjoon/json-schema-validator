package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.SetView;

/**
 * Unit tests for {@link AnyOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Constructor validation (array vs non‑array schema node)</li>
 *   <li>Basic {@code validate} behaviour – matching one subschema, matching none, empty anyOf</li>
 *   <li>{@code walk} delegation to {@code validate}</li>
 *   <li>Pre‑loading of subschemas (no exception)</li>
 *   <li>Edge cases around short‑circuiting – when annotation collection is enabled the validator must not short‑circuit.</li>
 * </ul>
 *
 * All objects are created using the public API – no mocking or subclassing of library
 * classes is performed.
 */
public class AnyOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;

    @Before
    public void setUp() {
        // Use the default meta‑schema (draft‑7) and factory provided by the library.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();               // standard meta‑schema
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();      // singleton factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();    // default config

        validationContext = new ValidationContext(metaSchema, factory, config);
        schemaLocation = SchemaLocation.DOCUMENT;                         // root location
        evaluationPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
    }

    /**
     * Helper to build an {@link AnyOfValidator} from a JSON array definition.
     */
    private AnyOfValidator buildValidator(String anyOfJson) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(anyOfJson);
        return new AnyOfValidator(schemaLocation, evaluationPath, schemaNode, null, validationContext);
    }

    @Test
    public void constructorShouldThrowWhenSchemaNodeIsNotArray() throws Exception {
        JsonNode notArray = MAPPER.readTree("{\"type\":\"string\"}");
        try {
            new AnyOfValidator(schemaLocation, evaluationPath, notArray, null, validationContext);
            fail("Expected JsonSchemaException because schema node is not an array");
        } catch (JsonSchemaException e) {
            // expected – verify that the message mentions the expected type
            assertTrue(e.getMessage().contains("array"));
        }
    }

    @Test
    public void validateShouldPassWhenNodeMatchesOneSubschema() throws Exception {
        // anyOf: [ { "type":"string" }, { "type":"integer" } ]
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        ExecutionContext ctx = new ExecutionContext(); // default execution context

        // a string matches the first subschema
        JsonNode node = MAPPER.readTree("\"hello\"");
        Set<ValidationMessage> result = validator.validate(ctx, node, node, new JsonNodePath(JsonNodePath.PathType.ROOT));
        assertTrue("Validation should succeed (empty error set)", result.isEmpty());

        // an integer matches the second subschema
        JsonNode intNode = MAPPER.readTree("42");
        result = validator.validate(ctx, intNode, intNode, new JsonNodePath(JsonNodePath.PathType.ROOT));
        assertTrue("Validation should succeed for integer", result.isEmpty());
    }

    @Test
    public void validateShouldFailWhenNodeMatchesNoSubschema() throws Exception {
        // anyOf: [ { "type":"string" }, { "type":"integer" } ]
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        ExecutionContext ctx = new ExecutionContext();

        // a boolean does not match any subschema
        JsonNode node = MAPPER.readTree("true");
        Set<ValidationMessage> result = validator.validate(ctx, node, node, new JsonNodePath(JsonNodePath.PathType.ROOT));

        // we expect at least one validation message (the type error from each subschema)
        assertFalse("Result should contain validation errors", result.isEmpty());

        // Verify that the messages contain the expected keyword (type)
        boolean containsTypeError = result.stream()
                .anyMatch(m -> "type".equals(m.getMessageKey()));
        assertTrue("At least one error should be a type error", containsTypeError);
    }

    @Test
    public void validateShouldReturnEmptyWhenAnyOfArrayIsEmpty() throws Exception {
        // anyOf: []
        AnyOfValidator validator = buildValidator("[]");

        ExecutionContext ctx = new ExecutionContext();

        JsonNode node = MAPPER.readTree("\"anything\"");
        Set<ValidationMessage> result = validator.validate(ctx, node, node, new JsonNodePath(JsonNodePath.PathType.ROOT));

        // According to the implementation, with zero subschemas the validator returns an empty set.
        assertTrue("Empty anyOf should validate successfully (no errors)", result.isEmpty());
    }

    @Test
    public void walkShouldDelegateToValidateWhenShouldValidateSchemaIsTrue() throws Exception {
        // anyOf: [ { "type":"string" } ]
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"}]");

        ExecutionContext ctx = new ExecutionContext();

        JsonNode node = MAPPER.readTree("\"walk-test\"");
        Set<ValidationMessage> result = validator.walk(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT), true);

        // Since the node is a string, validation must succeed and return an empty set.
        assertTrue("walk with validation should succeed", result.isEmpty());
    }

    @Test
    public void walkShouldIterateThroughSubschemasWhenShouldValidateSchemaIsFalse() throws Exception {
        // anyOf: [ { "type":"string" }, { "type":"integer" } ]
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        ExecutionContext ctx = new ExecutionContext();

        // The node is a boolean – validation would fail, but walk with shouldValidateSchema = false
        // should simply traverse the subschemas without collecting errors.
        JsonNode node = MAPPER.readTree("false");
        Set<ValidationMessage> result = validator.walk(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT), false);

        // walk returns an empty set when shouldValidateSchema is false.
        assertTrue("walk without validation should return empty set", result.isEmpty());
    }

    @Test
    public void preloadJsonSchemaShouldCacheCanShortCircuitFlagWithoutException() throws Exception {
        // anyOf with a single subschema – the internal flag can be cached.
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"}]");

        // No exception should be thrown and the internal flag should be set (non‑null).
        validator.preloadJsonSchema();

        // Use reflection to read the private field 'canShortCircuit' and assert it is not null.
        java.lang.reflect.Field flagField = AnyOfValidator.class.getDeclaredField("canShortCircuit");
        flagField.setAccessible(true);
        Object flagValue = flagField.get(validator);
        assertNotNull("canShortCircuit flag should be cached after preload", flagValue);
    }

    @Test
    public void shortCircuitShouldBeDisabledWhenAnnotationCollectionIsEnabled() throws Exception {
        // Enable annotation collection in the execution config – this forces canShortCircuit(ExecutionContext) to return false.
        ExecutionConfig execConfig = new ExecutionConfig();
        execConfig.setAnnotationCollectionEnabled(true); // turn on annotation collection
        ExecutionContext ctx = new ExecutionContext(execConfig);

        // anyOf with two subschemas; first matches, second would produce an error.
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        // Node matches the first subschema (string). Because annotation collection is enabled,
        // the validator must NOT short‑circuit, therefore errors from the second subschema are still collected.
        JsonNode node = MAPPER.readTree("\"short-circuit-test\"");
        Set<ValidationMessage> result = validator.validate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT));

        // The result must be empty (validation succeeded) – however we also verify that short‑circuiting
        // logic was exercised by checking that the internal method canShortCircuit(ExecutionContext) returns false.
        assertTrue("Validation should still succeed", result.isEmpty());

        // Verify the protected method's behaviour via reflection.
        java.lang.reflect.Method shortCircuitMethod = AnyOfValidator.class
                .getDeclaredMethod("canShortCircuit", ExecutionContext.class);
        shortCircuitMethod.setAccessible(true);
        boolean canShortCircuit = (boolean) shortCircuitMethod.invoke(validator, ctx);
        assertFalse("Short‑circuit should be disabled when annotation collection is enabled", canShortCircuit);
    }
}
