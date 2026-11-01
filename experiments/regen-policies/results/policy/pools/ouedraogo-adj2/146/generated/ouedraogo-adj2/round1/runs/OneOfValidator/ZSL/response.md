###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.utils.SetView;

/**
 * JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * The tests construct real {@link JsonSchema} objects via the public
 * {@link JsonSchemaFactory} API (the library’s normal way of creating
 * schemas) and then obtain the {@code OneOfValidator} instance from the
 * schema’s validator list.  All public and protected behaviour of the
 * class is exercised:
 *
 * <ul>
 *   <li>validation when exactly one subschema matches</li>
 *   <li>validation when none matches (error code “oneOf”)</li>
 *   <li>validation when more than one subschema matches (error code “oneOf.indexes”)</li>
 *   <li>the {@code canShortCircuit()} caching logic</li>
 *   <li>the {@code reportChildErrors(...)} flag handling</li>
 *   <li>the {@code walk(...)} overloads</li>
 *   <li>pre‑loading of subschemas via {@code preloadJsonSchema()}</li>
 * </ul>
 *
 * The test class deliberately avoids any mocking; every object is built
 * through the library’s public constructors or factory methods.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext context;
    private ExecutionContext execContext;
    private JsonNodePath rootPath;

    @Before
    public void setUp() {
        // Build a ValidationContext using the default meta‑schema (V7) and a default configuration.
        JsonMetaSchema meta = JsonMetaSchema.getV7();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        this.context = new ValidationContext(meta, factory, new SchemaValidatorsConfig());

        // ExecutionContext with default ExecutionConfig (failFast = false)
        this.execContext = new ExecutionContext();

        // Root path – the library defines a ROOT constant in PathType.
        this.rootPath = new JsonNodePath(com.networknt.schema.PathType.ROOT);
    }

    /**
     * Helper that creates a {@link JsonSchema} from a JSON string.
     */
    private JsonSchema getSchema(String json) {
        JsonNode schemaNode = MAPPER.readTree(json);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        return factory.getSchema(schemaNode);
    }

    /**
     * Retrieves the {@link OneOfValidator} from a schema that contains a
     * top‑level {@code oneOf} keyword.
     */
    private OneOfValidator getOneOfValidator(JsonSchema schema) {
        return (OneOfValidator) schema.getValidators().stream()
                .filter(v -> v instanceof OneOfValidator)
                .findFirst()
                .orElseThrow(() -> new AssertionError("OneOfValidator not found"));
    }

    @Test
    public void testValidateExactlyOneMatch_String() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema schema = getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);

        JsonNode instance = new TextNode("hello");
        Set<ValidationMessage> messages = validator.validate(execContext, instance, instance, rootPath);

        // No validation errors – exactly one subschema matches.
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testValidateExactlyOneMatch_Integer() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema schema = getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);

        JsonNode instance = new IntNode(42);
        Set<ValidationMessage> messages = validator.validate(execContext, instance, instance, rootPath);

        // No validation errors – exactly one subschema matches.
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testValidateNoMatch_Boolean() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema schema = getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);

        JsonNode instance = BooleanNode.TRUE;
        Set<ValidationMessage> messages = validator.validate(execContext, instance, instance, rootPath);

        // OneOf should fail because zero subschemas are valid.
        assertEquals(1, messages.size());
        ValidationMessage msg = messages.iterator().next();
        assertEquals("oneOf", msg.getMessageKey());
        // the first argument is the number of valid subschemas ("0")
        assertEquals("0", msg.getArguments()[0]);
    }

    @Test
    public void testValidateMultipleMatches_Number() throws Exception {
        // Both subschemas accept any number >= 0, so the instance matches both.
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"number\"}, {\"minimum\":0} ] }";
        JsonSchema schema = getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);

        JsonNode instance = new IntNode(5);
        Set<ValidationMessage> messages = validator.validate(execContext, instance, instance, rootPath);

        // Expect a “oneOf.indexes” error because two subschemas are valid.
        assertEquals(1, messages.size());
        ValidationMessage msg = messages.iterator().next();
        assertEquals("oneOf.indexes", msg.getMessageKey());

        // arguments[0] = number of valid schemas, arguments[1] = comma‑separated list of indexes
        assertEquals("2", msg.getArguments()[0]);
        assertEquals("0, 1", msg.getArguments()[1]);
    }

    @Test
    public void testCanShortCircuitCaching() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema schema = getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);

        // First call – computes the flag.
        Method canShortCircuit = OneOfValidator.class.getDeclaredMethod("canShortCircuit");
        canShortCircuit.setAccessible(true);
        Boolean first = (Boolean) canShortCircuit.invoke(validator);
        assertTrue(first);

        // Manipulate the internal field to prove caching is used.
        java.lang.reflect.Field field = OneOfValidator.class.getDeclaredField("canShortCircuit");
        field.setAccessible(true);
        field.set(validator, Boolean.FALSE);

        // Second call should return the cached value (still true because it was computed earlier).
        Boolean second = (Boolean) canShortCircuit.invoke(validator);
        assertTrue(second);
    }

    @Test
    public void testReportChildErrorsWhenFailFast() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema schema = getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);

        // Turn on fail‑fast in the execution config.
        execContext.setFailFast(true);

        Method reportChildErrors = OneOfValidator.class.getDeclaredMethod("reportChildErrors", ExecutionContext.class);
        reportChildErrors.setAccessible(true);
        Boolean result = (Boolean) reportChildErrors.invoke(validator, execContext);
        assertFalse("When failFast is true, child errors should not be reported", result);
    }

    @Test
    public void testWalkValidateTrueDelegatesToValidate() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema schema = getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);

        JsonNode instance = new TextNode("world");
        Set<ValidationMessage> viaValidate = validator.validate(execContext, instance, instance, rootPath);
        Set<ValidationMessage> viaWalk = validator.walk(execContext, instance, instance, rootPath, true);

        // Both should produce the same (empty) result set.
        assertEquals(viaValidate, viaWalk);
    }

    @Test
    public void testWalkValidateFalseDoesNotValidateChildren() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema schema = getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);

        // Pass a null node – walk(false) should simply traverse without validation.
        Set<ValidationMessage> result = validator.walk(execContext, null, null, rootPath, false);
        assertTrue("walk with shouldValidateSchema=false should return an empty set", result.isEmpty());
    }

    @Test
    public void testPreloadJsonSchemaCachesCanShortCircuit() throws Exception {
        String schemaJson = "{ \"oneOf\": [ {\"type\":\"string\"}, {\"type\":\"integer\"} ] }";
        JsonSchema schema = getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);

        // Before preload, the internal flag is null.
        java.lang.reflect.Field flagField = OneOfValidator.class.getDeclaredField("canShortCircuit");
        flagField.setAccessible(true);
        assertNull(flagField.get(validator));

        // Pre‑load – should initialise the flag.
        validator.preloadJsonSchema();
        Object flag = flagField.get(validator);
        assertNotNull("preloadJsonSchema should initialise canShortCircuit", flag);
        assertTrue((Boolean) flag);
    }
}
###Test END##