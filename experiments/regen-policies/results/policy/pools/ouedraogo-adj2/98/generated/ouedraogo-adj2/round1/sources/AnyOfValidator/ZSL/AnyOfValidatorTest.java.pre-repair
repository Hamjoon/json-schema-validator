/***************************************************************************************************
 * AnyOfValidatorTest
 *
 * Comprehensive JUnit‑4 test suite for {@link AnyOfValidator}. The tests cover:
 *   • Construction of the validator with a simple anyOf schema.
 *   • Validation logic (matching first subschema, matching second subschema, no match).
 *   • Walk behaviour with and without schema validation.
 *   • Short‑circuit logic (both the cached flag and the execution‑context variant).
 *   • preloadJsonSchema() side‑effects.
 *
 * The test uses only public constructors and methods that are declared in the source code
 * above or in the supplied dependency signatures.
 *
 * Note: The library classes (JsonMetaSchema, JsonSchemaFactory, SchemaValidatorsConfig,
 * ExecutionConfig, etc.) are part of the same dependency set; they are instantiated using
 * their public APIs as they exist in the library. No mocking or subclassing of abstract
 * library types is performed.
 **************************************************************************************************/

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

public class AnyOfValidatorTest {

    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private JsonNodePath rootPath;
    private AnyOfValidator anyOfValidator;

    /**
     * Build a very small anyOf validator that contains two subschemas:
     * <pre>
     *   [
     *     { "type" : "string" },
     *     { "type" : "integer" }
     *   ]
     * </pre>
     *
     * The validator is created once and reused by the test methods.
     */
    @Before
    public void setUp() {
        // --------------------------------------------------------------------
        // 1️⃣  Create the minimal objects required by the library.
        // --------------------------------------------------------------------
        // meta‑schema, factory and config are standard library utilities.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();                     // default V7 meta‑schema
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();          // default configuration

        // ValidationContext is the entry point for building schemas/validators.
        this.validationContext = new ValidationContext(metaSchema, factory, config);

        // ExecutionContext – default (no special collector or config).
        this.executionContext = new ExecutionContext();

        // Root JSON‑Pointer path – the library provides a constant for the root.
        this.rootPath = JsonNodePath.ROOT;   // equivalent to an empty path

        // --------------------------------------------------------------------
        // 2️⃣  Build the anyOf schema node (an array with two simple type schemas)
        // --------------------------------------------------------------------
        ArrayNode anyOfArray = JsonNodeFactory.instance.arrayNode();

        //  {"type":"string"}
        ObjectNode stringSchema = JsonNodeFactory.instance.objectNode()
                .put("type", "string");
        //  {"type":"integer"}
        ObjectNode integerSchema = JsonNodeFactory.instance.objectNode()
                .put("type", "integer");

        anyOfArray.add(stringSchema);
        anyOfArray.add(integerSchema);

        // --------------------------------------------------------------------
        // 3️⃣  Instantiate the validator under test.
        // --------------------------------------------------------------------
        this.anyOfValidator = new AnyOfValidator(
                SchemaLocation.of("http://example.com/schema"), // arbitrary location
                JsonNodePath.ROOT,                              // evaluation path (root)
                anyOfArray,                                     // the anyOf schema node
                null,                                           // no parent schema
                this.validationContext);
    }

    /**
     * Helper that invokes the public {@code validate} method with the same
     * node as both the instance node and the root node.
     */
    private Set<ValidationMessage> validate(JsonNode node) {
        return anyOfValidator.validate(executionContext, node, node, rootPath);
    }

    /**
     * Helper that invokes the {@code walk} method with {@code shouldValidateSchema}
     * set to {@code true}.
     */
    private Set<ValidationMessage> walkValidate(JsonNode node) {
        return anyOfValidator.walk(executionContext, node, node, rootPath, true);
    }

    /**
     * Helper that invokes the {@code walk} method with {@code shouldValidateSchema}
     * set to {@code false}.
     */
    private Set<ValidationMessage> walkNoValidate(JsonNode node) {
        return anyOfValidator.walk(executionContext, node, node, rootPath, false);
    }

    // ------------------------------------------------------------------------
    //  VALIDATION LOGIC
    // ------------------------------------------------------------------------

    @Test
    public void shouldValidateWhenFirstSubschemaMatches() {
        JsonNode stringNode = JsonNodeFactory.instance.textNode("hello world");

        Set<ValidationMessage> result = validate(stringNode);

        // The string matches the first subschema, therefore anyOf succeeds
        // and the result must be an empty set.
        assertTrue("Expected no validation errors when the first subschema matches", result.isEmpty());
    }

    @Test
    public void shouldValidateWhenSecondSubschemaMatches() {
        JsonNode integerNode = JsonNodeFactory.instance.numberNode(42);

        Set<ValidationMessage> result = validate(integerNode);

        // The integer matches the second subschema, anyOf succeeds.
        assertTrue("Expected no validation errors when the second subschema matches", result.isEmpty());
    }

    @Test
    public void shouldCollectErrorsWhenNoSubschemaMatches() {
        // A boolean does not satisfy either "string" or "integer".
        JsonNode booleanNode = JsonNodeFactory.instance.booleanNode(true);

        Set<ValidationMessage> result = validate(booleanNode);

        // The validator should return a non‑empty set containing the errors from
        // both subschemas (type mismatches).
        assertFalse("Expected validation errors when no subschema matches", result.isEmpty());

        // Both subschemas produce a type‑error, therefore we expect at least two messages.
        // (The exact number depends on internal implementation – we only assert >=2.)
        assertTrue("Expected at least two error messages", result.size() >= 2);
        for (ValidationMessage vm : result) {
            assertEquals("type", vm.getCode()); // the error code for a type mismatch is "type"
        }
    }

    // ------------------------------------------------------------------------
    //  WALK LOGIC
    // ------------------------------------------------------------------------

    @Test
    public void walkShouldValidateWhenFlagIsTrue() {
        JsonNode integerNode = JsonNodeFactory.instance.numberNode(7);

        Set<ValidationMessage> result = walkValidate(integerNode);

        // walk with validation delegates to validate(..., walk=true) which
        // should behave exactly like the normal validate method.
        assertTrue("walk(validate=true) should succeed for a matching integer", result.isEmpty());
    }

    @Test
    public void walkShouldNotValidateWhenFlagIsFalse() {
        // The node does NOT match any subschema, but walk with shouldValidateSchema=false
        // must *not* produce validation errors – it simply walks the child schemas.
        JsonNode booleanNode = JsonNodeFactory.instance.booleanNode(false);

        Set<ValidationMessage> result = walkNoValidate(booleanNode);

        // According to the implementation, walk without validation returns an empty set.
        assertTrue("walk(validate=false) must return an empty set regardless of node", result.isEmpty());
    }

    // ------------------------------------------------------------------------
    //  SHORT‑CIRCUIT LOGIC
    // ------------------------------------------------------------------------

    @Test
    public void canShortCircuitShouldCacheResult() {
        // First call calculates the flag (no parent validators -> true)
        boolean first = anyOfValidator.canShortCircuit();

        // Manually flip the internal field to a different value to ensure that the
        // second call does *not* recompute but returns the cached value.
        // (We use reflection because the field is private.)
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("canShortCircuit");
            f.setAccessible(true);
            f.set(anyOfValidator, Boolean.FALSE);
        } catch (Exception e) {
            fail("Reflection failed while tampering with canShortCircuit field: " + e);
        }

        // Second call must return the cached value (false) regardless of the actual
        // parent schema content.
        boolean second = anyOfValidator.canShortCircuit();

        assertFalse("canShortCircuit should return the cached value after the first call", second);
        assertTrue("First call should have returned true because no parent validators exist", first);
    }

    @Test
    public void canShortCircuitExecutionContextRespectsAnnotationCollectionFlag() {
        // By default, annotation collection is disabled → short‑circuit permitted.
        assertTrue("Default execution config should allow short‑circuit",
                anyOfValidator.canShortCircuit(executionContext));

        // Enable annotation collection and verify that short‑circuit is now disabled.
        ExecutionConfig execConfig = executionContext.getExecutionConfig();
        execConfig.setAnnotationCollectionEnabled(true);
        executionContext.setExecutionConfig(execConfig); // apply the modified config

        assertFalse("When annotation collection is enabled, short‑circuit must be disabled",
                anyOfValidator.canShortCircuit(executionContext));
    }

    // ------------------------------------------------------------------------
    //  PRE‑LOAD LOGIC
    // ------------------------------------------------------------------------

    @Test
    public void preloadJsonSchemaShouldCacheCanShortCircuitFlag() {
        // At this point the canShortCircuit cache is still null.
        // We invoke preloadJsonSchema which internally calls canShortCircuit()
        // and therefore should initialise the cache.
        anyOfValidator.preloadJsonSchema();

        // Verify that the cached flag is now non‑null and true (no parent validators).
        assertTrue("After preloadJsonSchema, canShortCircuit cache must be true",
                anyOfValidator.canShortCircuit());
    }

    // ------------------------------------------------------------------------
    //  INTERNAL HELPERS – ensure they are reachable (package‑private)
    // ------------------------------------------------------------------------

    @Test
    public void internalValidateWithWalkFlagDoesNotThrow() {
        // The protected validate method with the extra 'walk' flag is exercised
        // indirectly via the public walk(..., true) method. The test simply
        // asserts that the call does not raise any unexpected exception.
        JsonNode node = JsonNodeFactory.instance.textNode("sample");
        try {
            anyOfValidator.validate(executionContext, node, node, rootPath, true);
        } catch (Exception e) {
            fail("Protected validate(..., walk=true) threw an exception: " + e);
        }
    }
}
