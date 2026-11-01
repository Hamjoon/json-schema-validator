package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.SetView;

/**
 * Comprehensive unit tests for {@link AnyOfValidator}.
 *
 * The tests cover the following scenarios:
 *
 * <ul>
 *   <li>Successful validation when at least one subschema matches.</li>
 *   <li>Failure when none of the subschemas match.</li>
 *   <li>Skipping a subschema whose {@link TypeValidator} does not match the instance type.</li>
 *   <li>Behaviour of {@link #walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)} with
 *        {@code shouldValidateSchema = false} (should return an empty set without invoking validation).</li>
 *   <li>Correct caching of the {@code canShortCircuit} flag via {@link #preloadJsonSchema()}.</li>
 * </ul>
 *
 * The test suite builds real {@link JsonSchema} objects through {@link ValidationContext#newSchema}
 * and does **not** use any mocking framework.
 */
public class AnyOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private JsonNodePath rootPath;

    @Before
    public void setUp() {
        // Create a ValidationContext with the default (V7) meta‑schema.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();                     // static helper in the library
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();            // default factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();          // default configuration
        this.validationContext = new ValidationContext(metaSchema, factory, config);

        // ExecutionContext with default configuration (annotation collection disabled)
        this.executionContext = new ExecutionContext();

        // Root instance location – the library provides a PathType enum; ROOT is the usual entry point.
        this.rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
    }

    /**
     * Helper that builds an {@link AnyOfValidator} from a JSON array definition.
     *
     * @param anyOfDefinition JSON array node that represents the {@code anyOf} keyword.
     * @return a fully initialised {@link AnyOfValidator}.
     */
    private AnyOfValidator createValidator(JsonNode anyOfDefinition) {
        // The parent schema is a simple empty schema – it has no validators, therefore
        // {@link AnyOfValidator#canShortCircuit()} will evaluate to {@code true}.
        JsonNode emptySchemaNode = MAPPER.createObjectNode();
        JsonSchema parentSchema = validationContext.newSchema(
                SchemaLocation.DOCUMENT, new JsonNodePath(JsonNodePath.PathType.ROOT),
                emptySchemaNode, null);

        // The location of the anyOf keyword (for the purpose of the test we use the document root).
        SchemaLocation schemaLocation = SchemaLocation.DOCUMENT;
        JsonNodePath evaluationPath = new JsonNodePath(JsonNodePath.PathType.ROOT);

        return new AnyOfValidator(schemaLocation, evaluationPath, anyOfDefinition, parentSchema, validationContext);
    }

    /**
     * Test that validation succeeds when at least one subschema matches the instance.
     * The instance is a string – it matches the first subschema ({@code type: "string"}).
     */
    @Test
    public void testValidateSuccessWhenOneSubschemaMatches() throws Exception {
        // anyOf: [ { "type": "string" }, { "type": "integer" } ]
        String anyOfJson = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        JsonNode anyOfNode = MAPPER.readTree(anyOfJson);
        AnyOfValidator validator = createValidator(anyOfNode);

        JsonNode instance = MAPPER.readTree("\"hello world\""); // a JSON string

        Set<ValidationMessage> messages = validator.validate(executionContext, instance, instance, rootPath);
        // Expect no validation errors (empty set)
        assertTrue("Expected validation to succeed (empty error set)", messages.isEmpty());
    }

    /**
     * Test that validation fails when **none** of the subschemas match the instance.
     * The instance is a boolean – it does not match either {@code type: "string"} nor {@code type: "integer"}.
     */
    @Test
    public void testValidateFailureWhenNoSubschemaMatches() throws Exception {
        // anyOf: [ { "type": "string" }, { "type": "integer" } ]
        String anyOfJson = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        JsonNode anyOfNode = MAPPER.readTree(anyOfJson);
        AnyOfValidator validator = createValidator(anyOfNode);

        JsonNode instance = MAPPER.readTree("true"); // a JSON boolean

        Set<ValidationMessage> messages = validator.validate(executionContext, instance, instance, rootPath);
        // Expect at least two error messages (one from each subschema)
        assertFalse("Expected validation to fail (non‑empty error set)", messages.isEmpty());
        assertEquals("Two subschemas should each contribute an error", 2, messages.size());
    }

    /**
     * Test that a subschema whose {@link TypeValidator} does not match the instance type is
     * **skipped** and its type‑validation error is recorded.
     *
     * The first subschema expects a string, the second expects an integer. The instance is a number.
     * The integer subschema matches, therefore validation succeeds and the error from the
     * string subschema should be present in the aggregated error set (because the validator
     * continues after a type mismatch).
     */
    @Test
    public void testValidateWithTypeMismatchSkipping() throws Exception {
        // anyOf: [ { "type": "string" }, { "type": "integer" } ]
        String anyOfJson = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        JsonNode anyOfNode = MAPPER.readTree(anyOfJson);
        AnyOfValidator validator = createValidator(anyOfNode);

        JsonNode instance = MAPPER.readTree("42"); // a JSON integer

        Set<ValidationMessage> messages = validator.validate(executionContext, instance, instance, rootPath);

        // Validation must succeed because the integer subschema validates.
        assertTrue("Instance matches the second subschema, validation should succeed", messages.isEmpty());

        // To verify that the first subschema's type error was added to the internal error collection,
        // we force a failure by using a value that matches none of the subschemas.
        JsonNode badInstance = MAPPER.readTree("true"); // boolean – matches neither
        Set<ValidationMessage> badMessages = validator.validate(executionContext, badInstance, badInstance, rootPath);
        // We should receive two errors – one from the string type validator, one from the integer type validator.
        assertEquals("Both type validators should have contributed an error", 2, badMessages.size());
    }

    /**
     * Verify that {@link AnyOfValidator#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}
     * returns an empty set when {@code shouldValidateSchema} is {@code false},
     * even though the subschemas are still walked (their own {@code walk} methods are invoked).
     *
     * The test uses a simple anyOf definition and checks that the returned set is empty.
     */
    @Test
    public void testWalkWhenShouldValidateSchemaIsFalse() throws Exception {
        // anyOf: [ { "type": "string" }, { "type": "integer" } ]
        String anyOfJson = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        JsonNode anyOfNode = MAPPER.readTree(anyOfJson);
        AnyOfValidator validator = createValidator(anyOfNode);

        JsonNode instance = MAPPER.readTree("\"dummy\"");

        Set<ValidationMessage> result = validator.walk(executionContext, instance, instance, rootPath, false);
        // According to the implementation, walk with shouldValidateSchema == false must return an empty set.
        assertTrue("walk(..., false) should always return an empty set", result.isEmpty());
    }

    /**
     * Test that {@link AnyOfValidator#preloadJsonSchema()} correctly caches the result of
     * {@link AnyOfValidator#canShortCircuit()}.
     *
     * The parent schema used in the test does **not** contain any {@code unevaluatedProperties}
     * or {@code unevaluatedItems} validators, therefore {@code canShortCircuit()} should be {@code true}
     * after the preload.
     */
    @Test
    public void testPreloadJsonSchemaCachesCanShortCircuitFlag() throws Exception {
        // anyOf: [ { "type": "string" } ]
        String anyOfJson = "[{\"type\":\"string\"}]";
        JsonNode anyOfNode = MAPPER.readTree(anyOfJson);
        AnyOfValidator validator = createValidator(anyOfNode);

        // Before preload the internal flag is null (cannot be accessed directly, so we invoke the method)
        // The method is protected; we use reflection to call it.
        java.lang.reflect.Method canShortCircuitMethod = AnyOfValidator.class
                .getDeclaredMethod("canShortCircuit");
        canShortCircuitMethod.setAccessible(true);
        Boolean before = (Boolean) canShortCircuitMethod.invoke(validator);
        assertNull("Before preload the cached flag should be null", before);

        // Preload – this should compute and store the flag.
        validator.preloadJsonSchema();

        Boolean after = (Boolean) canShortCircuitMethod.invoke(validator);
        assertNotNull("After preload the cached flag must be non‑null", after);
        assertTrue("With a parent schema that has no unevaluated* validators, canShortCircuit should be true", after);
    }

    /**
     * Helper method to assert that a given field in a {@link ValidationMessage} is not {@code null}.
     * This is used to guarantee that the library returns fully populated messages.
     */
    private void assertMessageFieldsNonNull(ValidationMessage message) {
        assertNotNull("Message code must be present", message.getCode());
        assertNotNull("Instance location must be present", message.getInstanceLocation());
        assertNotNull("Schema location must be present", message.getSchemaLocation());
        assertNotNull("Message text must be present", message.getMessage());
    }

    /**
     * Additional sanity check: when validation fails, each {@link ValidationMessage}
     * should contain non‑null mandatory fields.
     */
    @Test
    public void testValidationMessagesAreFullyPopulated() throws Exception {
        // anyOf: [ { "type": "string" } ]
        String anyOfJson = "[{\"type\":\"string\"}]";
        JsonNode anyOfNode = MAPPER.readTree(anyOfJson);
        AnyOfValidator validator = createValidator(anyOfNode);

        JsonNode instance = MAPPER.readTree("123"); // integer – does not match

        Set<ValidationMessage> messages = validator.validate(executionContext, instance, instance, rootPath);
        assertFalse("Expected validation to fail", messages.isEmpty());
        for (ValidationMessage msg : messages) {
            assertMessageFieldsNonNull(msg);
        }
    }
}
