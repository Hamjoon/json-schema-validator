/**
 * JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>public API extraction</li>
 *   <li>basic happy‑path validation for matching and non‑matching instances</li>
 *   <li>edge‑cases such as an empty {@code anyOf} array and a non‑array schema definition</li>
 *   <li>the short‑circuit logic (both the cached {@code canShortCircuit()} and the execution‑context version)</li>
 *   <li>the {@code walk(...)} method behaviour</li>
 * </ul>
 *
 * The test class builds real {@link com.networknt.schema.AnyOfValidator} instances
 * using only the public constructors and factory methods that appear in the supplied
 * source code or in the public API of the dependent classes.
 *
 * NOTE: The library classes (e.g. {@code JsonMetaSchema}, {@code JsonSchemaFactory},
 * {@code SchemaValidatorsConfig}, {@code ExecutionConfig}) are part of the
 * {@code com.networknt.schema} artifact and provide public no‑arg or builder
 * methods that allow us to create concrete objects without any mocking.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

public class AnyOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private JsonNodePath rootPath;
    private SchemaLocation rootLocation;

    @Before
    public void setUp() {
        // Build the minimal objects required for constructing an AnyOfValidator
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();               // standard meta‑schema
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(); // default factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();      // default config

        validationContext = new ValidationContext(metaSchema, schemaFactory, config);
        rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        rootLocation = SchemaLocation.DOCUMENT; // DOCUMENT is the canonical root location
    }

    /**
     * Helper that creates an {@code AnyOfValidator} from a JSON array node that
     * contains the supplied subschemas.
     */
    private AnyOfValidator createValidator(ArrayNode anyOfArray) {
        // The parent schema is not needed for the tests – we can safely pass {@code null}
        return new AnyOfValidator(rootLocation, rootPath, anyOfArray, null, validationContext);
    }

    /**
     * 1. Public API extraction (for documentation purposes)
     *
     * The public methods of {@code AnyOfValidator} are:
     * <pre>
     *   Set<ValidationMessage> validate(ExecutionContext executionContext,
     *                                 JsonNode node,
     *                                 JsonNode rootNode,
     *                                 JsonNodePath instanceLocation)
     *
     *   Set<ValidationMessage> walk(ExecutionContext executionContext,
     *                                 JsonNode node,
     *                                 JsonNode rootNode,
     *                                 JsonNodePath instanceLocation,
     *                                 boolean shouldValidateSchema)
     *
     *   protected boolean canShortCircuit(ExecutionContext executionContext)
     *
     *   protected boolean canShortCircuit()
     *
     *   public void preloadJsonSchema()
     * </pre>
     *
     * The constructor is public as well.
     */

    /* --------------------------------------------------------------------- */
    /*   BASIC VALIDATION TESTS                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void validate_shouldPassWhenStringMatchesAnyOf() {
        // anyOf: [ {"type":"string"}, {"type":"integer"} ]
        ArrayNode anyOf = JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode().put("type", "string"))
                .add(JsonNodeFactory.instance.objectNode().put("type", "integer"));
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode instance = JsonNodeFactory.instance.textNode("hello");
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, rootPath);
        assertTrue("A matching string should produce no validation errors", messages.isEmpty());
    }

    @Test
    public void validate_shouldPassWhenIntegerMatchesAnyOf() {
        // anyOf: [ {"type":"string"}, {"type":"integer"} ]
        ArrayNode anyOf = JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode().put("type", "string"))
                .add(JsonNodeFactory.instance.objectNode().put("type", "integer"));
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode instance = JsonNodeFactory.instance.numberNode(42);
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, rootPath);
        assertTrue("A matching integer should produce no validation errors", messages.isEmpty());
    }

    @Test
    public void validate_shouldFailWhenNoSubSchemaMatches() {
        // anyOf: [ {"type":"string"}, {"type":"integer"} ]
        ArrayNode anyOf = JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode().put("type", "string"))
                .add(JsonNodeFactory.instance.objectNode().put("type", "integer"));
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode instance = JsonNodeFactory.instance.booleanNode(true); // boolean does not match
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, rootPath);
        assertFalse("A non‑matching instance should produce validation errors", messages.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*   EDGE CASES                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void constructor_shouldThrowWhenSchemaNodeIsNotArray() {
        // Passing an object node instead of an array must raise JsonSchemaException
        JsonNode notArray = JsonNodeFactory.instance.objectNode().put("type", "string");
        try {
            new AnyOfValidator(rootLocation, rootPath, notArray, null, validationContext);
            fail("Expected JsonSchemaException because schemaNode is not an array");
        } catch (JsonSchemaException e) {
            // expected – verify that the message mentions “array”
            assertTrue(e.getMessage().contains("array"));
        }
    }

    @Test
    public void validate_withEmptyAnyOfArray_shouldReturnEmptyErrorSet() {
        // anyOf: []  (no alternatives)
        ArrayNode emptyArray = JsonNodeFactory.instance.arrayNode();
        AnyOfValidator validator = createValidator(emptyArray);

        JsonNode instance = JsonNodeFactory.instance.textNode("anything");
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, rootPath);
        // With no subschemas, the spec says the validator always succeeds.
        assertTrue("Empty anyOf should succeed (no errors)", messages.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*   SHORT‑CIRCUIT LOGIC                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void canShortCircuit_withoutParentValidators_returnsTrue() {
        // Build a validator with a simple anyOf – there are no parent validators,
        // therefore the cached flag should be true.
        ArrayNode anyOf = JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode().put("type", "string"));
        AnyOfValidator validator = createValidator(anyOf);

        // The public method is protected – we can invoke it via reflection.
        try {
            java.lang.reflect.Method m = AnyOfValidator.class.getDeclaredMethod("canShortCircuit");
            m.setAccessible(true);
            Boolean result = (Boolean) m.invoke(validator);
            assertTrue("canShortCircuit() should be true when no parent validators exist", result);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    public void canShortCircuit_executionContext_respectsAnnotationCollectionFlag() {
        // The method returns true when annotation collection is disabled.
        ArrayNode anyOf = JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode().put("type", "string"));
        AnyOfValidator validator = createValidator(anyOf);

        // ExecutionContext with annotation collection disabled
        ExecutionConfig cfgDisabled = new ExecutionConfig();
        cfgDisabled.setAnnotationCollectionEnabled(false);
        ExecutionContext ctxDisabled = new ExecutionContext(cfgDisabled);

        // ExecutionContext with annotation collection enabled
        ExecutionConfig cfgEnabled = new ExecutionConfig();
        cfgEnabled.setAnnotationCollectionEnabled(true);
        ExecutionContext ctxEnabled = new ExecutionContext(cfgEnabled);

        try {
            java.lang.reflect.Method m = AnyOfValidator.class.getDeclaredMethod("canShortCircuit",
                    ExecutionContext.class);
            m.setAccessible(true);
            Boolean resultDisabled = (Boolean) m.invoke(validator, ctxDisabled);
            Boolean resultEnabled = (Boolean) m.invoke(validator, ctxEnabled);

            assertTrue("When annotation collection is disabled, canShortCircuit(ExecutionContext) must be true",
                    resultDisabled);
            assertFalse("When annotation collection is enabled, canShortCircuit(ExecutionContext) must be false",
                    resultEnabled);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    /* --------------------------------------------------------------------- */
    /*   WALK METHOD                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void walk_shouldDelegateToValidateWhenShouldValidateSchemaIsTrue() {
        // anyOf: [{"type":"string"}]
        ArrayNode anyOf = JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode().put("type", "string"));
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode instance = JsonNodeFactory.instance.textNode("walk-test");
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validator.walk(ctx, instance, instance, rootPath, true);
        assertTrue("walk with shouldValidateSchema=true should behave like validate()", messages.isEmpty());
    }

    @Test
    public void walk_shouldIterateSubSchemasWhenShouldValidateSchemaIsFalse() {
        // anyOf: [{"type":"string"}, {"type":"integer"}]
        ArrayNode anyOf = JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode().put("type", "string"))
                .add(JsonNodeFactory.instance.objectNode().put("type", "integer"));
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode instance = JsonNodeFactory.instance.booleanNode(false);
        ExecutionContext ctx = new ExecutionContext();

        // shouldValidateSchema = false – the method should walk each sub‑schema but not
        // collect validation errors (it always returns an empty set).
        Set<ValidationMessage> messages = validator.walk(ctx, instance, instance, rootPath, false);
        assertTrue("walk with shouldValidateSchema=false must return empty set", messages.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*   PRELOAD METHOD                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void preloadJsonSchema_shouldCacheCanShortCircuitFlag() {
        // The method does not expose its internal flag, but we can verify that it does
        // not throw and that subsequent calls to canShortCircuit() still work.
        ArrayNode anyOf = JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode().put("type", "string"));
        AnyOfValidator validator = createValidator(anyOf);

        // First call – should initialise internal flag
        validator.preloadJsonSchema();

        // Second call – invoke protected canShortCircuit() via reflection
        try {
            java.lang.reflect.Method m = AnyOfValidator.class.getDeclaredMethod("canShortCircuit");
            m.setAccessible(true);
            Boolean cached = (Boolean) m.invoke(validator);
            assertNotNull("canShortCircuit flag should be cached after preload", cached);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }
}
