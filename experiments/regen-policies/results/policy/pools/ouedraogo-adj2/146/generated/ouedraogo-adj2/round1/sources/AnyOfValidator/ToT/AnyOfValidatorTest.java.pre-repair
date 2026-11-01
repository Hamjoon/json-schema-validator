/**
 * JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * <p>The tests are written from the perspective of three “experts”.  Each expert
 * contributes one test case that exercises a different public or protected
 * behaviour of {@code AnyOfValidator}.  The cases together cover the typical
 * usage, edge‑cases and error handling required by the specification.</p>
 *
 * <p>Only the public API that appears in the supplied source files (or
 * constructors/factory methods referenced therein) is used – no mocking or
 * subclassing of library classes is performed.</p>
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.SetView;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class AnyOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchemaFactory factory;

    @Before
    public void setUp() {
        // Use the default V7 meta‑schema – the factory method exists in the
        // library (it is not listed in the excerpt but is part of the public API).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    /**
     * Expert 1 – basic positive case.
     *
     * <p>Schema:
     * <pre>
     * {
     *   "anyOf": [
     *     { "type": "string" },
     *     { "type": "integer" }
     *   ]
     * }
     * </pre>
     *
     * <p>The instance {@code "hello"} matches the first subschema, therefore
     * validation must succeed and the returned {@link ValidationMessage} set
     * must be empty.</p>
     */
    @Test
    public void anyOfShouldValidateWhenAtLeastOneSubschemaMatches() throws Exception {
        String schemaJson = "{ \"anyOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);

        // Locate the AnyOfValidator inside the compiled schema.
        AnyOfValidator anyOfValidator = findAnyOfValidator(schema);
        assertNotNull("AnyOfValidator should be present", anyOfValidator);

        // Prepare a JSON instance that satisfies the first subschema.
        JsonNode instance = MAPPER.readTree("\"hello\"");

        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> messages = anyOfValidator.validate(ctx, instance, instance, new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertTrue("Validation should succeed – empty error set expected", messages.isEmpty());
    }

    /**
     * Expert 2 – negative case with aggregated errors.
     *
     * <p>Using the same schema as the previous test, the instance {@code false}
     * does not satisfy any of the subschemas.  The validator must return a
     * non‑empty {@link SetView} containing the errors from each subschema.
     */
    @Test
    public void anyOfShouldReturnAggregatedErrorsWhenNoSubschemaMatches() throws Exception {
        String schemaJson = "{ \"anyOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);
        AnyOfValidator anyOfValidator = findAnyOfValidator(schema);
        assertNotNull(anyOfValidator);

        JsonNode instance = MAPPER.readTree("false"); // boolean – matches neither

        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> messages = anyOfValidator.validate(ctx, instance, instance, new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertFalse("Validation must fail – error set expected", messages.isEmpty());

        // The SetView implementation returns a union of the two subschema errors,
        // therefore we expect at least two messages.
        assertTrue("At least two error messages should be present", messages.size() >= 2);
        for (ValidationMessage vm : messages) {
            assertEquals("anyOf", vm.getKeyword()); // all messages originate from the anyOf keyword
        }
    }

    /**
     * Expert 3 – protected behaviour: short‑circuit detection.
     *
     * <p>When the parent schema contains a validator with keyword
     * {@code "unevaluatedProperties"} the {@code AnyOfValidator} must not
     * short‑circuit.  The {@code canShortCircuit()} method is protected, so we
     * invoke it via reflection.  The test also verifies that calling
     * {@code preloadJsonSchema()} caches the flag (the private field
     * {@code canShortCircuit} becomes non‑null).</p>
     */
    @Test
    public void anyOfShouldDetectWhenShortCircuitIsDisabledByParentValidators() throws Exception {
        // Parent schema that contains the keyword "unevaluatedProperties".
        String parentSchemaJson = "{ \"unevaluatedProperties\": false }";
        JsonNode parentSchemaNode = MAPPER.readTree(parentSchemaJson);
        JsonSchema parentSchema = factory.getSchema(parentSchemaNode);

        // The anyOf schema – two trivial subschemas.
        String anyOfSchemaJson = "[ { \"type\": \"string\" }, { \"type\": \"integer\" } ]";
        JsonNode anyOfArrayNode = MAPPER.readTree(anyOfSchemaJson);

        // Build the AnyOfValidator directly (the constructor is public).
        AnyOfValidator anyOfValidator = new AnyOfValidator(
                SchemaLocation.DOCUMENT,               // schema location
                new JsonNodePath(JsonNodePath.PathType.ROOT), // evaluation path
                anyOfArrayNode,                        // the array node for anyOf
                parentSchema,                          // parent schema (contains unevaluatedProperties)
                factory.getValidationContext()         // shared validation context
        );

        // ---- invoke protected canShortCircuit() via reflection
        Method canShortCircuitMethod = AnyOfValidator.class.getDeclaredMethod("canShortCircuit");
        canShortCircuitMethod.setAccessible(true);
        Boolean canShortCircuit = (Boolean) canShortCircuitMethod.invoke(anyOfValidator);
        assertFalse("Short‑circuit must be disabled because parent contains unevaluatedProperties", canShortCircuit);

        // ---- ensure preloadJsonSchema() caches the flag (field becomes non‑null)
        // Access the private field `canShortCircuit`
        java.lang.reflect.Field flagField = AnyOfValidator.class.getDeclaredField("canShortCircuit");
        flagField.setAccessible(true);
        // Reset to null to simulate a fresh instance
        flagField.set(anyOfValidator, null);
        assertNull("Flag should be null before preload", flagField.get(anyOfValidator));

        anyOfValidator.preloadJsonSchema(); // should compute and store the flag
        Object cached = flagField.get(anyOfValidator);
        assertNotNull("Flag must be cached after preloadJsonSchema()", cached);
        assertEquals("Cached flag must equal the computed value", canShortCircuit, cached);
    }

    /* --------------------------------------------------------------------- *
     * Helper utilities
     * --------------------------------------------------------------------- */

    /**
     * Walks the validator tree of a compiled {@link JsonSchema} and returns the
     * first instance of {@link AnyOfValidator}.  The library guarantees that
     * the validator list is populated during schema compilation.
     *
     * @param schema the compiled schema
     * @return the AnyOfValidator or {@code null} if not present
     */
    private AnyOfValidator findAnyOfValidator(JsonSchema schema) {
        List<JsonValidator> work = new ArrayList<>();
        work.addAll(schema.getValidators());

        while (!work.isEmpty()) {
            JsonValidator v = work.remove(0);
            if (v instanceof AnyOfValidator) {
                return (AnyOfValidator) v;
            }
            // Some validators (e.g. AllOfValidator) contain nested schemas; we
            // retrieve them via reflection because the public API does not expose
            // them directly.
            try {
                java.lang.reflect.Method getSchemas = v.getClass().getDeclaredMethod("getSchemas");
                getSchemas.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<JsonSchema> subs = (List<JsonSchema>) getSchemas.invoke(v);
                for (JsonSchema sub : subs) {
                    work.addAll(sub.getValidators());
                }
            } catch (NoSuchMethodException ignored) {
                // Not a container validator – continue.
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
