package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.networknt.schema.utils.SetView;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit 4 test cases for {@link OneOfValidator}.
 *
 * <p>These tests cover:
 * <ul>
 *   <li>Public method signatures</li>
 *   <li>Normal operation of {@code validate}</li>
 *   <li>Edge‑cases such as empty {@code oneOf}, non‑array schema node, and short‑circuit behaviour</li>
 *   <li>Helper methods {@code reportChildErrors} and {@code canShortCircuit}</li>
 *   <li>The {@code walk} and {@code preloadJsonSchema} methods</li>
 * </ul>
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private ExecutionContext executionContext;

    @Before
    public void setUp() {
        // The library provides convenient static factories for these objects.
        // They are used here as‑is – no mocking or subclassing of library classes.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();                     // typical default meta‑schema
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();           // default factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();         // default config
        validationContext = new ValidationContext(metaSchema, factory, config);
        executionContext = new ExecutionContext();                            // default execution context
    }

    /**
     * ----------------------------------------------------------------------
     *  Public methods of OneOfValidator (extracted from the source)
     * ----------------------------------------------------------------------
     *
     * <pre>
     * public OneOfValidator(SchemaLocation schemaLocation,
     *                       JsonNodePath evaluationPath,
     *                       JsonNode schemaNode,
     *                       JsonSchema parentSchema,
     *                       ValidationContext validationContext)
     *
     * public Set<ValidationMessage> validate(ExecutionContext executionContext,
     *                                         JsonNode node,
     *                                         JsonNode rootNode,
     *                                         JsonNodePath instanceLocation)
     *
     * protected Set<ValidationMessage> validate(ExecutionContext executionContext,
     *                                            JsonNode node,
     *                                            JsonNode rootNode,
     *                                            JsonNodePath instanceLocation,
     *                                            boolean walk)
     *
     * protected boolean reportChildErrors(ExecutionContext executionContext)
     *
     * protected boolean canShortCircuit()
     *
     * public Set<ValidationMessage> walk(ExecutionContext executionContext,
     *                                     JsonNode node,
     *                                     JsonNode rootNode,
     *                                     JsonNodePath instanceLocation,
     *                                     boolean shouldValidateSchema)
     *
     * public void preloadJsonSchema()
     * </pre>
     */

    /* ----------------------------------------------------------------------
     *  1. Constructor – non‑array schema node must throw JsonSchemaException
     * ---------------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void constructorShouldThrowWhenSchemaNodeIsNotArray() {
        JsonNode notArrayNode = MAPPER.createObjectNode().put("type", "string"); // an object, not an array
        new OneOfValidator(SchemaLocation.DOCUMENT,
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                notArrayNode,
                null,
                validationContext);
    }

    /* ----------------------------------------------------------------------
     *  2. Basic validation – exactly one subschema matches
     * ---------------------------------------------------------------------- */
    @Test
    public void validateShouldPassWhenExactlyOneSchemaMatches() throws Exception {
        // oneOf: [ {"type":"string"}, {"type":"integer"} ]
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonNode schemaNode = MAPPER.readTree(schemaJson).get("oneOf");

        OneOfValidator validator = new OneOfValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                schemaNode,
                null,
                validationContext);

        JsonNode stringNode = MAPPER.readTree("\"hello\"");
        Set<ValidationMessage> errors = validator.validate(executionContext, stringNode, stringNode, new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertTrue("String value should satisfy exactly one subschema → no errors", errors.isEmpty());
    }

    @Test
    public void validateShouldPassWhenSecondSchemaMatches() throws Exception {
        // same schema as above
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonNode schemaNode = MAPPER.readTree(schemaJson).get("oneOf");

        OneOfValidator validator = new OneOfValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                schemaNode,
                null,
                validationContext);

        JsonNode intNode = MAPPER.readTree("42");
        Set<ValidationMessage> errors = validator.validate(executionContext, intNode, intNode, new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertTrue("Integer value should satisfy exactly one subschema → no errors", errors.isEmpty());
    }

    /* ----------------------------------------------------------------------
     *  3. Validation – no subschema matches → oneOf error
     * ---------------------------------------------------------------------- */
    @Test
    public void validateShouldReturnOneOfErrorWhenNoSchemaMatches() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonNode schemaNode = MAPPER.readTree(schemaJson).get("oneOf");

        OneOfValidator validator = new OneOfValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                schemaNode,
                null,
                validationContext);

        JsonNode booleanNode = MAPPER.readTree("true");
        Set<ValidationMessage> errors = validator.validate(executionContext, booleanNode, booleanNode,
                new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertEquals("Exactly one validation message expected", 1, errors.size());
        ValidationMessage msg = errors.iterator().next();
        assertEquals("Message key must be 'oneOf' when zero schemas validate", "oneOf", msg.getMessageKey());
        assertEquals("Argument[0] – number of valid schemas", "0", msg.getArguments()[0]);
    }

    /* ----------------------------------------------------------------------
     *  4. Validation – more than one subschema matches → oneOf.indexes error
     * ---------------------------------------------------------------------- */
    @Test
    public void validateShouldReturnOneOfIndexesErrorWhenMultipleSchemasMatch() throws Exception {
        // two identical subschemas – both accept a string
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"string\"} ] }";
        JsonNode schemaNode = MAPPER.readTree(schemaJson).get("oneOf");

        OneOfValidator validator = new OneOfValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                schemaNode,
                null,
                validationContext);

        JsonNode stringNode = MAPPER.readTree("\"duplicate\"");
        Set<ValidationMessage> errors = validator.validate(executionContext, stringNode, stringNode,
                new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertEquals("Exactly one validation message expected", 1, errors.size());
        ValidationMessage msg = errors.iterator().next();
        assertEquals("Message key must be 'oneOf.indexes' when >1 schemas validate", "oneOf.indexes", msg.getMessageKey());
        assertEquals("First argument – number of valid schemas", "2", msg.getArguments()[0]);
        assertTrue("Second argument should contain the matching indexes (0,1)",
                ((String) msg.getArguments()[1]).contains("0") && ((String) msg.getArguments()[1]).contains("1"));
    }

    /* ----------------------------------------------------------------------
     *  5. Edge case – empty oneOf array
     * ---------------------------------------------------------------------- */
    @Test
    public void validateShouldFailWhenOneOfArrayIsEmpty() throws Exception {
        String schemaJson = "{ \"oneOf\": [] }";
        JsonNode schemaNode = MAPPER.readTree(schemaJson).get("oneOf");

        OneOfValidator validator = new OneOfValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                schemaNode,
                null,
                validationContext);

        JsonNode anyNode = MAPPER.readTree("\"anything\"");
        Set<ValidationMessage> errors = validator.validate(executionContext, anyNode, anyNode,
                new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertEquals("Empty oneOf must always produce an error", 1, errors.size());
        ValidationMessage msg = errors.iterator().next();
        assertEquals("Message key must be 'oneOf' when zero schemas validate", "oneOf", msg.getMessageKey());
        assertEquals("Argument[0] – number of valid schemas", "0", msg.getArguments()[0]);
    }

    /* ----------------------------------------------------------------------
     *  6. reportChildErrors – respects ExecutionConfig.failFast flag
     * ---------------------------------------------------------------------- */
    @Test
    public void reportChildErrorsRespectsFailFastFlag() throws Exception {
        // Build a minimal validator – we only need the method under test.
        OneOfValidator validator = new OneOfValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                MAPPER.createArrayNode(), // empty oneOf – constructor will succeed
                null,
                validationContext);

        // 6a – failFast = false  → should report child errors
        executionContext.getExecutionConfig().setFailFast(false);
        assertTrue("When failFast is false, child errors should be reported",
                validator.reportChildErrors(executionContext));

        // 6b – failFast = true → should NOT report child errors
        executionContext.getExecutionConfig().setFailFast(true);
        assertFalse("When failFast is true, child errors should NOT be reported",
                validator.reportChildErrors(executionContext));
    }

    /* ----------------------------------------------------------------------
     *  7. canShortCircuit – false when parent schema contains unevaluated* validators
     * ---------------------------------------------------------------------- */
    @Test
    public void canShortCircuitReturnsFalseIfParentHasUnevaluatedValidators() throws Exception {
        // Create a dummy JsonValidator that reports the keyword "unevaluatedProperties"
        JsonValidator dummy = new JsonValidator() {
            @Override public Set<ValidationMessage> validate(ExecutionContext ec, JsonNode n, JsonNode r, JsonNodePath p) { return Collections.emptySet(); }
            @Override public Set<ValidationMessage> walk(ExecutionContext ec, JsonNode n, JsonNode r, JsonNodePath p, boolean v) { return Collections.emptySet(); }
            @Override public SchemaLocation getSchemaLocation() { return SchemaLocation.DOCUMENT; }
            @Override public JsonNodePath getEvaluationPath() { return new JsonNodePath(JsonNodePath.PathType.ROOT); }
            @Override public String getKeyword() { return "unevaluatedProperties"; }
        };

        // Minimal parent schema that returns the dummy validator in its list
        JsonSchema parent = new JsonSchema(
                Collections.singletonList(dummy), // validators
                true,                             // validatorsLoaded
                false,                            // recursiveAnchor
                null,                             // typeValidator (not needed for this test)
                null,                             // id
                false,                            // suppressSubSchemaRetrieval
                MAPPER.createObjectNode(),        // schemaNode
                validationContext,
                null, null, null, null, null, null,
                Collections.emptyMap()
        ) {
            // keep default behaviour – nothing else required
        };

        // oneOf with a single trivial subschema
        JsonNode oneOfNode = MAPPER.createArrayNode().add(MAPPER.createObjectNode().put("type", "string"));
        OneOfValidator validator = new OneOfValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                oneOfNode,
                parent,
                validationContext);

        // Invoke the protected method via reflection
        boolean result = (Boolean) java.lang.reflect.Method.class
                .cast(OneOfValidator.class.getDeclaredMethod("canShortCircuit"))
                .invoke(validator);

        assertFalse("canShortCircuit must be false when parent contains unevaluated* validator", result);
    }

    @Test
    public void canShortCircuitReturnsTrueWhenNoUnevaluatedValidators() throws Exception {
        // Parent schema without any special validators
        JsonSchema parent = new JsonSchema(
                Collections.emptyList(),
                true,
                false,
                null,
                null,
                false,
                MAPPER.createObjectNode(),
                validationContext,
                null, null, null, null, null, null,
                Collections.emptyMap()
        ) {
            // nothing extra
        };

        JsonNode oneOfNode = MAPPER.createArrayNode().add(MAPPER.createObjectNode().put("type", "string"));
        OneOfValidator validator = new OneOfValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                oneOfNode,
                parent,
                validationContext);

        boolean result = (Boolean) java.lang.reflect.Method.class
                .cast(OneOfValidator.class.getDeclaredMethod("canShortCircuit"))
                .invoke(validator);

        assertTrue("canShortCircuit must be true when parent has no unevaluated* validators", result);
    }

    /* ----------------------------------------------------------------------
     *  8. walk – when shouldValidateSchema is true it should delegate to validate
     * ---------------------------------------------------------------------- */
    @Test
    public void walkShouldDelegateToValidateWhenShouldValidateSchemaIsTrue() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"} ] }";
        JsonNode schemaNode = MAPPER.readTree(schemaJson).get("oneOf");

        OneOfValidator validator = new OneOfValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                schemaNode,
                null,
                validationContext);

        JsonNode node = MAPPER.readTree("\"hello\"");
        Set<ValidationMessage> result = validator.walk(executionContext, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT), true);

        // The walk method should return the same result as validate – here no errors.
        assertTrue("walk with shouldValidateSchema=true must return empty set for a valid instance", result.isEmpty());
    }

    @Test
    public void walkShouldIterateSchemasWhenShouldValidateSchemaIsFalse() throws Exception {
        // oneOf with two simple schemas – we only care that the method does not throw.
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonNode schemaNode = MAPPER.readTree(schemaJson).get("oneOf");

        OneOfValidator validator = new OneOfValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                schemaNode,
                null,
                validationContext);

        JsonNode node = MAPPER.readTree("42");
        Set<ValidationMessage> result = validator.walk(executionContext, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT), false);

        // When shouldValidateSchema is false the method returns an empty set (it only walks).
        assertTrue("walk with shouldValidateSchema=false should return empty set", result.isEmpty());
    }

    /* ----------------------------------------------------------------------
     *  9. preloadJsonSchema – ensures internal short‑circuit flag is cached
     * ---------------------------------------------------------------------- */
    @Test
    public void preloadJsonSchemaShouldInitializeChildSchemasAndCacheCanShortCircuit() throws Exception {
        // oneOf with a single trivial subschema
        JsonNode oneOfNode = MAPPER.createArrayNode().add(MAPPER.createObjectNode().put("type", "string"));

        OneOfValidator validator = new OneOfValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                oneOfNode,
                null,
                validationContext);

        // Call preload – this should not throw and subsequent canShortCircuit call must be cached.
        validator.preloadJsonSchema();

        // After preload, invoking canShortCircuit must return the cached value (true in this simple case).
        boolean cached = (Boolean) java.lang.reflect.Method.class
                .cast(OneOfValidator.class.getDeclaredMethod("canShortCircuit"))
                .invoke(validator);
        assertTrue("After preload, canShortCircuit should be true for a schema without unevaluated* validators", cached);
    }
}
