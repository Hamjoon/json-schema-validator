package com.networknt.schema;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link AnyOfValidator}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All public methods of {@code AnyOfValidator}.</li>
 *   <li>Normal operation (matching one subschema, matching several, no match).</li>
 *   <li>Edge cases such as an empty {@code anyOf} array and type‑mismatch handling.</li>
 *   <li>Invocation of {@code walk} with {@code shouldValidateSchema} set to {@code false}.</li>
 *   <li>Execution of {@code preloadJsonSchema} (must not throw).</li>
 * </ul>
 *
 * <p>Only the public API that is visible in the supplied source code is used – no mocking or
 * subclassing of library classes is performed.
 */
public class AnyOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private SchemaValidatorsConfig config;

    @Before
    public void setUp() {
        // The concrete implementations of the factories and meta‑schema are part of the
        // networknt‑json‑schema library.  They all provide a no‑arg constructor, therefore we
        // can instantiate them directly.
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();               // default dialect
        JsonSchemaFactory schemaFactory = new JsonSchemaFactory();

        config = new SchemaValidatorsConfig();   // default configuration (no discriminator)
        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema,
                schemaFactory, config);
    }

    /**
     * Helper that creates an {@link AnyOfValidator} from a JSON array representing the
     * {@code anyOf} keyword.
     */
    private AnyOfValidator createValidator(ArrayNode anyOfArray) {
        SchemaLocation schemaLocation = SchemaLocation.of("http://example.com/schema");
        JsonNodePath evaluationPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        // parent schema is not required for the tests – we pass {@code null}
        return new AnyOfValidator(schemaLocation, evaluationPath, anyOfArray, null, validationContext);
    }

    /**
     * Basic test for the {@code validate} method – the instance matches the first subschema
     * (type = string).  The validator must return an empty error set.
     */
    @Test
    public void testValidateMatchesFirstSubschema() throws Exception {
        ArrayNode anyOf = MAPPER.createArrayNode()
                .add(MAPPER.readTree("{\"type\":\"string\"}"))
                .add(MAPPER.readTree("{\"type\":\"integer\"}"));
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = MAPPER.readTree("\"hello\"");          // a JSON string
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> errors = validator.validate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertTrue("Expected no validation errors when the node matches the first subschema", errors.isEmpty());
    }

    /**
     * Instance matches the second subschema (type = integer).  The validator must also
     * return an empty error set.
     */
    @Test
    public void testValidateMatchesSecondSubschema() throws Exception {
        ArrayNode anyOf = MAPPER.createArrayNode()
                .add(MAPPER.readTree("{\"type\":\"string\"}"))
                .add(MAPPER.readTree("{\"type\":\"integer\"}"));
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = MAPPER.readTree("5");                 // a JSON integer
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> errors = validator.validate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertTrue("Expected no validation errors when the node matches the second subschema", errors.isEmpty());
    }

    /**
     * Instance does NOT match any subschema (type = boolean).  The validator must return
     * at least one {@link ValidationMessage}.
     */
    @Test
    public void testValidateMatchesNone() throws Exception {
        ArrayNode anyOf = MAPPER.createArrayNode()
                .add(MAPPER.readTree("{\"type\":\"string\"}"))
                .add(MAPPER.readTree("{\"type\":\"integer\"}"));
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = MAPPER.readTree("true");              // a JSON boolean
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> errors = validator.validate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertFalse("Expected validation errors when the node matches none of the subschemas", errors.isEmpty());
    }

    /**
     * When both subschemas are satisfied (identical schemas), {@code anyOf} should still
     * succeed – the validator must return an empty error set.
     */
    @Test
    public void testValidateMatchesMultipleSubschemas() throws Exception {
        // Two identical string schemas – a string value satisfies both.
        ArrayNode anyOf = MAPPER.createArrayNode()
                .add(MAPPER.readTree("{\"type\":\"string\"}"))
                .add(MAPPER.readTree("{\"type\":\"string\"}"));
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = MAPPER.readTree("\"duplicate\"");     // a JSON string
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> errors = validator.validate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertTrue("Expected no validation errors when the node matches multiple subschemas", errors.isEmpty());
    }

    /**
     * Edge case – an empty {@code anyOf} array.  According to the specification the
     * instance can never be valid, therefore the validator must return a non‑empty set.
     */
    @Test
    public void testValidateEmptyAnyOfArray() throws Exception {
        ArrayNode anyOf = MAPPER.createArrayNode();            // empty array
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = MAPPER.readTree("\"anything\"");      // any JSON value
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> errors = validator.validate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertFalse("An empty anyOf array must produce validation errors", errors.isEmpty());
    }

    /**
     * Verify that {@code walk} with {@code shouldValidateSchema = false} delegates to the
     * subschemas but returns an empty {@link Set}.
     */
    @Test
    public void testWalkWithoutValidation() throws Exception {
        ArrayNode anyOf = MAPPER.createArrayNode()
                .add(MAPPER.readTree("{\"type\":\"string\"}"))
                .add(MAPPER.readTree("{\"type\":\"integer\"}"));
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = MAPPER.readTree("\"ignored\"");       // value does not matter
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> result = validator.walk(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT), false);

        // The contract of the method is to return an empty set when validation is disabled.
        assertTrue("walk(..., false) must return an empty set", result.isEmpty());
    }

    /**
     * Verify that {@code walk} with {@code shouldValidateSchema = true} simply forwards
     * to {@code validate}.  The behaviour is therefore identical to the corresponding
     * {@code validate} test.
     */
    @Test
    public void testWalkWithValidation() throws Exception {
        ArrayNode anyOf = MAPPER.createArrayNode()
                .add(MAPPER.readTree("{\"type\":\"string\"}"))
                .add(MAPPER.readTree("{\"type\":\"integer\"}"));
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = MAPPER.readTree("42");               // an integer
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> result = validator.walk(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT), true);

        assertTrue("walk(..., true) must validate and return no errors for a matching node", result.isEmpty());
    }

    /**
     * The {@code preloadJsonSchema} method should complete without throwing any
     * exception.  It is a void method that simply pre‑loads the subschemas.
     */
    @Test
    public void testPreloadJsonSchemaDoesNotThrow() throws Exception {
        ArrayNode anyOf = MAPPER.createArrayNode()
                .add(MAPPER.readTree("{\"type\":\"string\"}"))
                .add(MAPPER.readTree("{\"type\":\"integer\"}"));
        AnyOfValidator validator = createValidator(anyOf);

        // The method does not return a value – the test succeeds if no exception is raised.
        validator.preloadJsonSchema();
    }

    /**
     * Edge case – the validation configuration enables OpenAPI‑3 style discriminators.
     * The validator must still run without throwing, even though the supplied schema
     * does not contain a discriminator definition.
     */
    @Test
    public void testValidateWithOpenAPI3DiscriminatorFlagEnabled() throws Exception {
        // Enable the discriminator flag in the configuration.
        config.setOpenAPI3StyleDiscriminators(true);
        // Re‑create the ValidationContext so that it picks up the updated config.
        validationContext = new ValidationContext(
                new URIFactory(),
                new URNFactory(),
                JsonMetaSchema.getV7(),
                new JsonSchemaFactory(),
                config);

        ArrayNode anyOf = MAPPER.createArrayNode()
                .add(MAPPER.readTree("{\"type\":\"string\"}"))
                .add(MAPPER.readTree("{\"type\":\"integer\"}"));
        AnyOfValidator validator = createValidator(anyOf);

        JsonNode node = MAPPER.readTree("\"no‑discriminator\""); // a string value
        ExecutionContext ctx = new ExecutionContext();

        // The validator must still produce the correct result (no errors for a matching string)
        Set<ValidationMessage> errors = validator.validate(ctx, node, node,
                new JsonNodePath(JsonNodePath.PathType.ROOT));

        assertTrue("Validator must succeed even when discriminator handling is enabled but not used", errors.isEmpty());
    }

    // -----------------------------------------------------------------------
    // List of public methods (extracted from AnyOfValidator)
    // -----------------------------------------------------------------------
    // 1. public Set<ValidationMessage> validate(ExecutionContext executionContext,
    //                                          JsonNode node,
    //                                          JsonNode rootNode,
    //                                          JsonNodePath instanceLocation)
    // 2. public Set<ValidationMessage> walk(ExecutionContext executionContext,
    //                                      JsonNode node,
    //                                      JsonNode rootNode,
    //                                      JsonNodePath instanceLocation,
    //                                      boolean shouldValidateSchema)
    // 3. public void preloadJsonSchema()
}
