package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationContext.DiscriminatorContext;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

public class AnyOfValidatorTest {

    private ObjectMapper mapper;
    private ValidationContext validationContext;
    private SchemaValidatorsConfig config;

    @Before
    public void setUp() {
        // simple ObjectMapper for creating JsonNode instances
        mapper = new ObjectMapper();

        // Create a minimal ValidationContext.  All factories that are not needed for the
        // tests are passed as null – the implementation tolerates nulls for these arguments.
        config = new SchemaValidatorsConfig();               // default configuration (discriminators disabled)
        validationContext = new ValidationContext(
                null,               // URIFactory
                null,               // URNFactory
                null,               // JsonMetaSchema
                null,               // JsonSchemaFactory
                config);            // Config
    }

    /**
     * Helper that builds an {@link AnyOfValidator} from a JSON array string that
     * represents the anyOf subschemas.
     *
     * @param anyOfSchemaJson the JSON representation of the anyOf array
     * @return a ready‑to‑use AnyOfValidator instance
     */
    private AnyOfValidator buildValidator(String anyOfSchemaJson) throws Exception {
        JsonNode schemaNode = mapper.readTree(anyOfSchemaJson);
        // the AnyOfValidator constructor needs a parent schema – we can pass null because
        // the validator never uses the parent schema directly in the code under test.
        return new AnyOfValidator(
                "#/anyOf",          // arbitrary schema path
                schemaNode,
                null,               // parent schema
                validationContext);
    }

    /**
     * When at least one subschema validates successfully the validator must return
     * an empty error set, regardless of the errors produced by the other subschemas.
     */
    @Test
    public void testAnyOfValidWhenOneSubschemaMatches() throws Exception {
        // anyOf: string OR integer
        String anyOf = "[ {\"type\":\"string\"}, {\"type\":\"integer\"} ]";
        AnyOfValidator validator = buildValidator(anyOf);

        // Node that matches the first subschema (string)
        JsonNode stringNode = mapper.readTree("\"hello\"");
        ExecutionContext execCtx = new ExecutionContext();

        Set<ValidationMessage> result = validator.validate(execCtx, stringNode, stringNode, "#/value");
        assertTrue("Expected no validation errors when a subschema matches", result.isEmpty());

        // Node that matches the second subschema (integer)
        JsonNode intNode = mapper.readTree("42");
        result = validator.validate(execCtx, intNode, intNode, "#/value");
        assertTrue("Expected no validation errors when a subschema matches", result.isEmpty());
    }

    /**
     * When none of the subschemas match, the validator must return the accumulated
     * errors from each subschema.  In this case the errors are type‑mismatch messages.
     */
    @Test
    public void testAnyOfInvalidWhenNoSubschemaMatches() throws Exception {
        // anyOf: string OR integer
        String anyOf = "[ {\"type\":\"string\"}, {\"type\":\"integer\"} ]";
        AnyOfValidator validator = buildValidator(anyOf);

        // Node that matches neither subschema (boolean)
        JsonNode boolNode = mapper.readTree("true");
        ExecutionContext execCtx = new ExecutionContext();

        Set<ValidationMessage> result = validator.validate(execCtx, boolNode, boolNode, "#/value");
        assertFalse("Expected validation errors when no subschema matches", result.isEmpty());

        // Both subschemas add a type‑error message; we check that at least one message
        // contains the expected schema type name.
        boolean containsStringType = result.stream()
                .anyMatch(m -> m.getMessage().contains("string"));
        boolean containsIntegerType = result.stream()
                .anyMatch(m -> m.getMessage().contains("integer"));
        assertTrue("Expected a type‑error for the string subschema", containsStringType);
        assertTrue("Expected a type‑error for the integer subschema", containsIntegerType);
    }

    /**
     * When a subschema validates successfully, any “required” errors produced by the
     * other subschemas must be removed from the final result (the spec says that
     * required errors are ignored once any subschema passes).
     */
    @Test
    public void testRequiredErrorsAreFilteredWhenSubschemaPasses() throws Exception {
        // anyOf: object requiring property “a” OR object requiring property “b”
        String anyOf = "[\n" +
                "  {\n" +
                "    \"type\": \"object\",\n" +
                "    \"required\": [\"a\"],\n" +
                "    \"properties\": {\"a\": {\"type\": \"string\"}}\n" +
                "  },\n" +
                "  {\n" +
                "    \"type\": \"object\",\n" +
                "    \"required\": [\"b\"],\n" +
                "    \"properties\": {\"b\": {\"type\": \"string\"}}\n" +
                "  }\n" +
                "]";
        AnyOfValidator validator = buildValidator(anyOf);

        // Node satisfies the first subschema (has property “a”), but not the second.
        JsonNode node = mapper.readTree("{\"a\":\"value\"}");
        ExecutionContext execCtx = new ExecutionContext();

        Set<ValidationMessage> result = validator.validate(execCtx, node, node, "#/obj");
        // Because the first subschema is valid, the validator must succeed with no errors.
        assertTrue("Expected no validation errors after a successful subschema (required errors filtered)",
                result.isEmpty());
    }

    /**
     * Verify that the validator correctly handles a null node – the subschemas are still
     * evaluated and the result follows the same rules as for non‑null nodes.
     */
    @Test
    public void testValidateWithNullNode() throws Exception {
        // anyOf: string OR integer
        String anyOf = "[ {\"type\":\"string\"}, {\"type\":\"integer\"} ]";
        AnyOfValidator validator = buildValidator(anyOf);

        JsonNode nullNode = mapper.readTree("null");
        ExecutionContext execCtx = new ExecutionContext();

        Set<ValidationMessage> result = validator.validate(execCtx, nullNode, nullNode, "#/null");
        // Both subschemas reject null (type mismatch), so we expect errors.
        assertFalse("Expected errors when null does not satisfy any subschema", result.isEmpty());
    }

    /**
     * When the library is configured for OpenAPI 3 style discriminators, the validator
     * creates a {@link DiscriminatorContext}.  This test checks that the context is
     * created (non‑null) when the flag is enabled and that it is absent otherwise.
     *
     * The test does not exercise the full discriminator matching logic – that would
     * require a considerably larger schema – but it proves that the conditional
     * construction works as intended.
     */
    @Test
    public void testDiscriminatorContextCreatedWhenEnabled() throws Exception {
        // Enable the OpenAPI3 discriminator flag in the configuration
        config.setOpenAPI3StyleDiscriminators(true);
        // anyOf with a single empty subschema (the content is irrelevant for this test)
        String anyOf = "[ {} ]";
        AnyOfValidator validator = buildValidator(anyOf);

        // Use reflection to obtain the private field `discriminatorContext`
        java.lang.reflect.Field field = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        field.setAccessible(true);
        DiscriminatorContext ctx = (DiscriminatorContext) field.get(validator);
        assertNotNull("DiscriminatorContext should be created when the flag is enabled", ctx);
    }

    /**
     * The opposite of the previous test – with the discriminator flag disabled the
     * validator must not create a {@link DiscriminatorContext}.
     */
    @Test
    public void testDiscriminatorContextAbsentWhenDisabled() throws Exception {
        // Ensure the flag is disabled (default)
        config.setOpenAPI3StyleDiscriminators(false);
        String anyOf = "[ {} ]";
        AnyOfValidator validator = buildValidator(anyOf);

        java.lang.reflect.Field field = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        field.setAccessible(true);
        DiscriminatorContext ctx = (DiscriminatorContext) field.get(validator);
        assertNull("DiscriminatorContext should be null when the flag is disabled", ctx);
    }

    /**
     * Verify that the {@link AnyOfValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}
     * method delegates to {@code validate} when the {@code shouldValidateSchema} flag is true,
     * and that it returns an empty set when the flag is false.
     */
    @Test
    public void testWalkDelegatesAccordingToFlag() throws Exception {
        String anyOf = "[ {\"type\":\"string\"} ]";
        AnyOfValidator validator = buildValidator(anyOf);
        ExecutionContext ctx = new ExecutionContext();

        JsonNode node = mapper.readTree("\"text\"");
        // shouldValidateSchema = true → walk should perform validation
        Set<ValidationMessage> result = validator.walk(ctx, node, node, "#/path", true);
        assertTrue("Walk with validation flag should return empty set for a valid node", result.isEmpty());

        // shouldValidateSchema = false → walk should not validate and return empty set
        result = validator.walk(ctx, node, node, "#/path", false);
        assertTrue("Walk without validation flag must return an empty set", result.isEmpty());
    }
}
