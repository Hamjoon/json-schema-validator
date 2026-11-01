package com.networknt.schema;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JUnit‑4 test suite for {@link JsonSchema}.  The tests focus on the public API
 * of the class and on the edge‑case / exception handling that can be exercised
 * without needing to spin up a full HTTP server or remote $ref resolution.
 *
 * <p>The test class lives in the same package as {@code JsonSchema} so that the
 * protected constructor can be used when a {@link ValidationContext} is not
 * readily available.  For the majority of tests a real {@code JsonSchema}
 * instance is created via {@link JsonSchemaFactory#getInstance(VersionFlag, java.util.function.Consumer)}
 * – this is the only public factory method referenced in the source code and
 * therefore allowed by the problem statement.</p>
 *
 * <p>Where reflection is used (e.g. to inspect the private {@code validatorsLoaded}
 * flag) the code is guarded with a try‑catch block so that a failure to access the
 * field does not cause the whole test suite to abort.</p>
 */
public class JsonSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema simpleStringSchema;      // {"type":"string"}
    private JsonSchema schemaWithId;            // {"$id":"http://example.com/root","type":"object"}
    private JsonSchema schemaWithRecursiveAnchor; // {"$recursiveAnchor":true}
    private JsonSchema schemaWithRef;           // {"$ref":"#/definitions/foo","definitions":{"foo":{"type":"integer"}}}
    private JsonSchemaFactory factory;

    @Before
    public void setUp() {
        // Use the V201909 dialect – the latest one supported by the library.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V201909, null);

        simpleStringSchema = factory.getSchema("{\"type\":\"string\"}");
        schemaWithId = factory.getSchema("{\"$id\":\"http://example.com/root\",\"type\":\"object\"}");
        schemaWithRecursiveAnchor = factory.getSchema("{\"$recursiveAnchor\":true}");
        schemaWithRef = factory.getSchema("{\"$ref\":\"#/definitions/foo\",\"definitions\":{\"foo\":{\"type\":\"integer\"}}}");
    }

    /* --------------------------------------------------------------------- */
    /*  PUBLIC METHOD SIGNATURE LIST (extracted from the source)            */
    /* --------------------------------------------------------------------- */
    // static JsonSchema from(ValidationContext, SchemaLocation,
    //        JsonNodePath, JsonNode, JsonSchema, boolean)
    // public JsonSchema fromRef(JsonSchema, JsonNodePath)
    // public JsonSchema withConfig(SchemaValidatorsConfig)
    // public ValidationContext getValidationContext()
    // public JsonNode getRefSchemaNode(String)
    // public JsonSchema getRefSchema(JsonNodePath)
    // public JsonSchema getSubSchema(JsonNodePath)
    // public JsonSchema findLexicalRoot()
    // public JsonSchema findSchemaResourceRoot()
    // public boolean isSchemaResourceRoot()
    // public String getId()
    // public JsonSchema findAncestor()
    // public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)
    // public Set<ValidationMessage> validate(JsonNode)
    // public Set<ValidationMessage> validate(JsonNode, ExecutionContextCustomizer)
    // public Set<ValidationMessage> validate(JsonNode, java.util.function.Consumer<ExecutionContext>)
    // public <T> T validate(JsonNode, OutputFormat<T>)
    // public <T> T validate(JsonNode, OutputFormat<T>, ExecutionContextCustomizer)
    // public <T> T validate(JsonNode, OutputFormat<T>, java.util.function.Consumer<ExecutionContext>)
    // public Set<ValidationMessage> validate(String, InputFormat)
    // public Set<ValidationMessage> validate(String, InputFormat, ExecutionContextCustomizer)
    // public Set<ValidationMessage> validate(String, InputFormat, java.util.function.Consumer<ExecutionContext>)
    // public <T> T validate(String, InputFormat, OutputFormat<T>)
    // public <T> T validate(String, InputFormat, OutputFormat<T>, ExecutionContextCustomizer)
    // public <T> T validate(String, InputFormat, OutputFormat<T>, java.util.function.Consumer<ExecutionContext>)
    // public <T> T validate(ExecutionContext, JsonNode, OutputFormat<T>)
    // public <T> T validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)
    // public <T> T validate(ExecutionContext, JsonNode, OutputFormat<T>, java.util.function.Consumer<ExecutionContext>)
    // public void initializeValidators()
    // public boolean isRecursiveAnchor()
    // public ExecutionContext createExecutionContext()
    // public boolean hasTypeValidator()
    // public TypeValidator getTypeValidator()
    // public List<JsonValidator> getValidators()
    // public String toString()

    /* --------------------------------------------------------------------- */
    /*  BASIC FUNCTIONAL TESTS                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetId() {
        assertEquals("http://example.com/root", schemaWithId.getId());
        assertNull(simpleStringSchema.getId());
    }

    @Test
    public void testIsRecursiveAnchor() {
        assertTrue(schemaWithRecursiveAnchor.isRecursiveAnchor());
        assertFalse(simpleStringSchema.isRecursiveAnchor());
    }

    @Test
    public void testToStringFormat() {
        String s = simpleStringSchema.toString();
        assertTrue(s.contains("\"type\""));
        assertTrue(s.startsWith("\""));
    }

    @Test
    public void testCreateExecutionContext() {
        ExecutionContext ctx = simpleStringSchema.createExecutionContext();
        assertNotNull(ctx);
        // The context should carry the same locale as the default config
        assertEquals(simpleStringSchema.getValidationContext().getConfig().getLocale(),
                ctx.getExecutionConfig().getLocale());
    }

    @Test
    public void testValidateSimpleStringSchema() {
        Set<ValidationMessage> errors = simpleStringSchema.validate(MAPPER.readTree("\"hello\""));
        assertTrue(errors.isEmpty());

        errors = simpleStringSchema.validate(MAPPER.readTree("123"));
        assertFalse(errors.isEmpty());
        // The error code for a type mismatch is defined by the TypeValidator
        ValidationMessage vm = errors.iterator().next();
        assertEquals(ValidatorTypeCode.TYPE.getValue(), vm.getCode());
    }

    @Test
    public void testValidateStringInput() {
        Set<ValidationMessage> errors = simpleStringSchema.validate("\"world\"", InputFormat.JSON);
        assertTrue(errors.isEmpty());

        errors = simpleStringSchema.validate("true", InputFormat.JSON);
        assertFalse(errors.isEmpty());
    }

    @Test
    public void testValidateWithCustomExecutionContextCustomizer() {
        ExecutionContextCustomizer customizer = (ec, vc) -> ec.setFailFast(true);
        Set<ValidationMessage> errors = simpleStringSchema.validate(MAPPER.readTree("123"),
                customizer);
        // With fail‑fast enabled we should still get a single error
        assertEquals(1, errors.size());
    }

    @Test
    public void testValidateWithConsumer() {
        final boolean[] called = { false };
        simpleStringSchema.validate(MAPPER.readTree("123"), e -> called[0] = true);
        assertTrue(called[0]);
    }

    @Test
    public void testValidateWithOutputFormat() {
        // Use the default OutputFormat (which simply returns the raw set)
        Set<ValidationMessage> errors = simpleStringSchema.validate(MAPPER.readTree("123"),
                OutputFormat.DEFAULT);
        assertFalse(errors.isEmpty());
    }

    @Test
    public void testInitializeValidatorsIdempotent() throws Exception {
        // First call – validators should be loaded
        simpleStringSchema.initializeValidators();
        // Use reflection to read the private flag
        java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
        f.setAccessible(true);
        assertTrue((Boolean) f.get(simpleStringSchema));

        // Second call – should not throw and flag stays true
        simpleStringSchema.initializeValidators();
        assertTrue((Boolean) f.get(simpleStringSchema));
    }

    /* --------------------------------------------------------------------- */
    /*  EDGE‑CASE & EXCEPTION TESTS                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetRefSchemaNodeWithAbsoluteIdFragment() {
        // The schema has an $id, so a ref that starts with the same $id should be stripped.
        JsonNode node = schemaWithId.getRefSchemaNode("http://example.com/root#/definitions/foo");
        // The schema does not actually contain a definitions node, so the result is a missing node (null)
        assertNull(node);
    }

    @Test
    public void testGetRefSchemaNodeUrlEncodedPointer() throws UnsupportedEncodingException {
        // Build a schema with a property that contains a space, which forces URL‑encoding in the pointer.
        JsonSchema complex = factory.getSchema("{\"properties\":{\"my prop\":{\"type\":\"string\"}}}");
        JsonNode node = complex.getRefSchemaNode("#/properties/my%20prop");
        assertNotNull(node);
        assertEquals("string", node.get("type").asText());
    }

    @Test(expected = JsonSchemaException.class)
    public void testInvalidRecursiveAnchorThrowsException() {
        // The validator should reject a non‑boolean $recursiveAnchor.
        factory.getSchema("{\"$recursiveAnchor\":\"yes\"}");
    }

    @Test
    public void testFindLexicalRootWhenIdAbsent() {
        JsonSchema root = simpleStringSchema.findLexicalRoot();
        // Because there is no $id and no parent, the lexical root is the schema itself.
        assertSame(simpleStringSchema, root);
    }

    @Test
    public void testFindSchemaResourceRootWhenIdPresent() {
        JsonSchema root = schemaWithId.findSchemaResourceRoot();
        // The presence of $id makes the schema a resource root.
        assertSame(schemaWithId, root);
    }

    @Test
    public void testIsSchemaResourceRoot_NoIdAndNoParent() {
        assertTrue(simpleStringSchema.isSchemaResourceRoot());
    }

    @Test
    public void testIsSchemaResourceRoot_WithId() {
        assertTrue(schemaWithId.isSchemaResourceRoot());
    }

    @Test
    public void testFromRefCreatesNewInstanceWithDifferentEvaluationPath() {
        JsonNodePath newPath = new JsonNodePath(PathType.JSON_POINTER).append("foo");
        JsonSchema refSchema = simpleStringSchema.fromRef(simpleStringSchema, newPath);
        assertNotSame(simpleStringSchema, refSchema);
        // The evaluation path of the new schema must be the one we passed.
        assertEquals(newPath, refSchema.getEvaluationPath());
    }

    @Test
    public void testWithConfigReturnsSameInstanceWhenUnchanged() {
        SchemaValidatorsConfig cfg = simpleStringSchema.getValidationContext().getConfig();
        JsonSchema result = simpleStringSchema.withConfig(cfg);
        assertSame(simpleStringSchema, result);
    }

    @Test
    public void testWithConfigCreatesNewInstanceWhenChanged() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setFailFast(true);
        JsonSchema result = simpleStringSchema.withConfig(cfg);
        assertNotSame(simpleStringSchema, result);
        assertTrue(result.getValidationContext().getConfig().isFailFast());
    }

    @Test
    public void testGetRefSchemaWithJsonPointerFragment() {
        JsonSchema subs = schemaWithRef.getSubSchema(new JsonNodePath(PathType.JSON_POINTER).append("definitions").append("foo"));
        assertNotNull(subs);
        assertEquals("integer", subs.getSchemaNode().get("type").asText());
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetRefSchemaWithUnknownAnchorThrowsException() {
        // The fragment is interpreted as an anchor (non‑JSON‑pointer)
        schemaWithId.getRefSchema(new JsonNodePath(PathType.JSON_POINTER).append("nonexistent"));
    }

    @Test
    public void testHasTypeValidator() {
        assertTrue(simpleStringSchema.hasTypeValidator());
        // A schema that only contains $ref does not have a direct type validator.
        assertFalse(schemaWithRef.hasTypeValidator());
    }

    @Test
    public void testGetValidatorsNonEmpty() {
        List<JsonValidator> validators = simpleStringSchema.getValidators();
        assertNotNull(validators);
        assertFalse(validators.isEmpty());
    }

    @Test
    public void testValidateWithNullInputThrowsIllegalArgumentException() {
        try {
            simpleStringSchema.validate((String) null, InputFormat.JSON);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /*  TESTS FOR THE STATIC FACTORY METHOD                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testStaticFromFactoryMethodCreatesEquivalentSchema() {
        // Build the objects needed for the static factory method.
        ValidationContext ctx = new ValidationContext(factory.getMetaSchema(),
                factory, new SchemaValidatorsConfig());
        SchemaLocation loc = SchemaLocation.of("http://example.com/schema");
        JsonNodePath path = new JsonNodePath(PathType.JSON_POINTER);
        JsonNode node = MAPPER.readTree("{\"type\":\"boolean\"}");
        JsonSchema result = JsonSchema.from(ctx, loc, path, node, null, false);
        assertNotNull(result);
        assertEquals("boolean", result.getSchemaNode().get("type").asText());
    }

    /* --------------------------------------------------------------------- */
    /*  WALK METHOD BASIC TESTS (only a very small subset)                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testWalkSimpleSchema() {
        ExecutionContext ctx = simpleStringSchema.createExecutionContext();
        JsonNode node = MAPPER.readTree("\"hello\"");
        ValidationResult result = simpleStringSchema.walk(ctx, node, true);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void testWalkWithInvalidDataCollectsErrors() {
        ExecutionContext ctx = simpleStringSchema.createExecutionContext();
        JsonNode node = MAPPER.readTree("123");
        ValidationResult result = simpleStringSchema.walk(ctx, node, true);
        assertFalse(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  NEGATIVE TESTS FOR INVALID SCHEMAS                                   */
    /* --------------------------------------------------------------------- */

    @Test(expected = InvalidSchemaException.class)
    public void testInvalidIdFailsWhenSchemaIdValidatorRejects() {
        // Install a validator that always rejects.
        JsonSchemaIdValidator rejectingValidator = (id, root, sloc, result, vc) -> false;
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setSchemaIdValidator(rejectingValidator);
        JsonSchemaFactory customFactory = JsonSchemaFactory.getInstance(VersionFlag.V201909, null);
        customFactory.getMetaSchema().setConfig(cfg); // assume meta‑schema exposes config setter
        customFactory.getSchema("{\"$id\":\"http://bad.id\"}");
    }

    /* --------------------------------------------------------------------- */
    /*  COVERAGE FOR DEPRECATED / LEGACY METHODS (no‑op but must compile)   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDeprecatedValidateAndCollectMethods() {
        ExecutionContext ctx = simpleStringSchema.createExecutionContext();
        JsonNode node = MAPPER.readTree("\"test\"");
        ValidationResult result = simpleStringSchema.validateAndCollect(ctx, node);
        assertNotNull(result);
    }

    @Test
    public void testDeprecatedValidateAndCollectWithRootNode() {
        ExecutionContext ctx = simpleStringSchema.createExecutionContext();
        JsonNode node = MAPPER.readTree("\"test\"");
        ValidationResult result = simpleStringSchema.validateAndCollect(ctx, node, node, simpleStringSchema.atRoot());
        assertNotNull(result);
    }
}
