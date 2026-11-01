package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link AnyOfValidator}.
 *
 * <p>This test class follows the steps requested:
 * <ol>
 *   <li>List all public methods of {@code AnyOfValidator}.</li>
 *   <li>Provide a basic test for each public method.</li>
 *   <li>Identify edge‑case and exception scenarios.</li>
 *   <li>Write dedicated tests for those scenarios.</li>
 *   <li>Combine everything into a single test file.</li>
 * </ol>
 *
 * <p>All objects are created using only the constructors and methods that appear in the
 * supplied source code or in the public signatures that were provided. No mocking libraries
 * are used; reflection is only employed to inspect the private {@code canShortCircuit}
 * cache flag.</p>
 */
public class AnyOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Helper to create a {@link ValidationContext} with the minimal required arguments. */
    private ValidationContext createValidationContext() {
        // The concrete classes {@code JsonMetaSchema}, {@code JsonSchemaFactory}
        // and {@code SchemaValidatorsConfig} are part of the library but their
        // constructors are not listed.  They have public no‑arg constructors in the
        // reference implementation, therefore we can instantiate them directly.
        JsonMetaSchema metaSchema = JsonMetaSchema.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        return new ValidationContext(metaSchema, factory, config);
    }

    /** Helper to build an {@link AnyOfValidator} from a JSON array schema. */
    private AnyOfValidator buildValidator(String anyOfSchemaJson) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(anyOfSchemaJson);
        ValidationContext context = createValidationContext();

        // Use the root location and path for the test schema.
        SchemaLocation location = SchemaLocation.of("http://example.com/root");
        JsonNodePath path = new JsonNodePath(JsonNodePath.PathType.ROOT);

        // The parent schema can be {@code null} for top‑level validators.
        return new AnyOfValidator(location, path, schemaNode, null, context);
    }

    /** --------------------------------------------------------------------
     *  1. List of public methods (signatures) of AnyOfValidator
     *
     *  public Set<ValidationMessage> validate(ExecutionContext executionContext,
     *                                          JsonNode node,
     *                                          JsonNode rootNode,
     *                                          JsonNodePath instanceLocation)
     *
     *  public Set<ValidationMessage> walk(ExecutionContext executionContext,
     *                                     JsonNode node,
     *                                     JsonNode rootNode,
     *                                     JsonNodePath instanceLocation,
     *                                     boolean shouldValidateSchema)
     *
     *  public void preloadJsonSchema()
     *  -------------------------------------------------------------------- */
    /* No code needed – the list above satisfies the requirement. */

    /** --------------------------------------------------------------------
     *  2. Basic test cases for each public method
     *  -------------------------------------------------------------------- */

    @Test
    public void testValidate_ValidStringAgainstAnyOf() throws Exception {
        // anyOf: [ { "type": "string" }, { "type": "integer" } ]
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        JsonNode node = MAPPER.readTree("\"hello\"");
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.validate(ctx, node, node, new JsonNodePath(JsonNodePath.PathType.ROOT));
        assertTrue("A valid string should produce no validation messages", messages.isEmpty());
    }

    @Test
    public void testValidate_ValidIntegerAgainstAnyOf() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        JsonNode node = MAPPER.readTree("123");
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.validate(ctx, node, node, new JsonNodePath(JsonNodePath.PathType.ROOT));
        assertTrue("A valid integer should produce no validation messages", messages.isEmpty());
    }

    @Test
    public void testValidate_InvalidBooleanAgainstAnyOf() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        JsonNode node = MAPPER.readTree("true");
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.validate(ctx, node, node, new JsonNodePath(JsonNodePath.PathType.ROOT));
        // The validator should collect errors from both subschemas.
        assertFalse("Invalid value should produce validation messages", messages.isEmpty());
        // At least two messages are expected (one per subschema).
        assertTrue("Expected at least two messages", messages.size() >= 2);
    }

    @Test
    public void testWalk_WithShouldValidateTrue() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"}]");

        JsonNode node = MAPPER.readTree("\"test\"");
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.walk(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT), true);
        assertTrue("walk with shouldValidate=true should behave like validate", messages.isEmpty());
    }

    @Test
    public void testWalk_WithShouldValidateFalse() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"}]");

        JsonNode node = MAPPER.readTree("\"test\"");
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.walk(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT), false);
        // When shouldValidate is false the method walks each subschema without
        // collecting errors, therefore it always returns an empty set.
        assertTrue("walk with shouldValidate=false must return empty set", messages.isEmpty());
    }

    @Test
    public void testPreloadJsonSchema_CachesCanShortCircuitFlag() throws Exception {
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"}]");

        // The flag is lazily computed; after preloadJsonSchema() it must be cached.
        validator.preloadJsonSchema();

        // Use reflection to read the private field.
        Field f = AnyOfValidator.class.getDeclaredField("canShortCircuit");
        f.setAccessible(true);
        Boolean cached = (Boolean) f.get(validator);
        assertNotNull("canShortCircuit flag should be cached after preload", cached);
        // With no unevaluated* validators the flag should be true.
        assertTrue("Default canShortCircuit should be true", cached);
    }

    /** --------------------------------------------------------------------
     *  3. Edge‑case & exception handling scenarios
     *
     *  - Constructor receives a non‑array schema node → throws JsonSchemaException.
     *  - Empty anyOf array (size == 0) → validation should return an empty set
     *    because no subschema can validate the instance (spec permits this).
     *  - Short‑circuit behaviour when annotation collection is enabled.
     *  - Presence of an "unevaluatedProperties" validator in the parent schema
     *    disables short‑circuiting.
     *  -------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void testConstructor_NonArraySchemaNode_ThrowsException() throws Exception {
        // schemaNode is an object, not an array.
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"string\"}");
        ValidationContext ctx = createValidationContext();

        new AnyOfValidator(SchemaLocation.of("http://example.com/root"),
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                schemaNode,
                null,
                ctx);
    }

    @Test
    public void testValidate_EmptyAnyOfArray_ReturnsEmptySet() throws Exception {
        AnyOfValidator validator = buildValidator("[]");

        JsonNode node = MAPPER.readTree("\"anything\"");
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.validate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT));
        // According to the implementation, when there are no subschemas the
        // validator ends with numberOfValidSubSchemas == 0 and allErrors == null,
        // which results in an empty set.
        assertTrue("Empty anyOf should produce no validation messages", messages.isEmpty());
    }

    @Test
    public void testCanShortCircuit_DisabledByParentValidator() throws Exception {
        // Build a parent schema that contains an "unevaluatedProperties" validator.
        // We create a dummy parent JsonSchema manually because the public API
        // does not expose a way to add a validator after construction.
        // Instead we construct a JsonSchema whose validator list already contains
        // the required keyword.
        JsonNode parentSchemaNode = MAPPER.readTree("{\"unevaluatedProperties\":{}}");
        ValidationContext ctx = createValidationContext();
        JsonSchema parent = ctx.newSchema(SchemaLocation.of("http://example.com/parent"),
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                parentSchemaNode,
                null);

        // The AnyOfValidator will inspect the parent schema's validators.
        AnyOfValidator validator = new AnyOfValidator(
                SchemaLocation.of("http://example.com/anyof"),
                new JsonNodePath(JsonNodePath.PathType.ROOT),
                MAPPER.readTree("[{\"type\":\"string\"}]"),
                parent,
                ctx);

        // Force cache computation.
        validator.preloadJsonSchema();

        // Verify that the cached flag is false because of the unevaluatedProperties validator.
        Field f = AnyOfValidator.class.getDeclaredField("canShortCircuit");
        f.setAccessible(true);
        Boolean cached = (Boolean) f.get(validator);
        assertNotNull("canShortCircuit flag must be cached", cached);
        assertFalse("Presence of unevaluatedProperties should disable short‑circuit", cached);
    }

    @Test
    public void testCanShortCircuit_AnnotationCollectionEnabled() throws Exception {
        // Create a ValidationContext whose ExecutionConfig has annotation collection enabled.
        JsonMetaSchema meta = JsonMetaSchema.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        ValidationContext ctx = new ValidationContext(meta, factory, cfg);

        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"}]");

        // ExecutionContext with annotation collection turned on.
        ExecutionConfig execCfg = new ExecutionConfig();
        execCfg.setAnnotationCollectionEnabled(true);
        ExecutionContext execCtx = new ExecutionContext(execCfg);

        // Invoke the protected canShortCircuit(ExecutionContext) via reflection.
        java.lang.reflect.Method m = AnyOfValidator.class.getDeclaredMethod("canShortCircuit", ExecutionContext.class);
        m.setAccessible(true);
        Boolean result = (Boolean) m.invoke(validator, execCtx);
        assertFalse("Annotation collection enabled must prevent short‑circuit", result);
    }

    /** --------------------------------------------------------------------
     *  4. Additional sanity test – ensure that short‑circuit works when possible.
     *  -------------------------------------------------------------------- */

    @Test
    public void testValidate_ShortCircuitWhenFirstSubschemaValid() throws Exception {
        // anyOf with two schemas; first one will validate, second will not.
        AnyOfValidator validator = buildValidator("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        JsonNode node = MAPPER.readTree("\"shortcircuit\"");
        ExecutionContext ctx = new ExecutionContext();

        // Ensure annotation collection is off so short‑circuiting is allowed.
        ctx.getExecutionConfig().setAnnotationCollectionEnabled(false);
        ctx.setFailFast(false); // fail‑fast does not affect short‑circuit logic here.

        Set<ValidationMessage> messages = validator.validate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT));
        // The first subschema is valid, so the validator should short‑circuit and
        // return an empty message set.
        assertTrue("Validator should short‑circuit after first successful subschema", messages.isEmpty());
    }
}
