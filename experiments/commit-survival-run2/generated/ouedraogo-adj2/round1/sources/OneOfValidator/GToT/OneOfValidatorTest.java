package com.networknt.schema;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Extraction of public methods</li>
 *   <li>Basic functionality of {@code validate}, {@code walk} and {@code preloadJsonSchema}</li>
 *   <li>Edge‑case handling – no matching schema, multiple matching schemas and fail‑fast mode</li>
 *   <li>Behaviour of {@code walk} when validation is disabled</li>
 * </ul>
 *
 * All objects are created via the public constructors/factory methods defined in the
 * supplied source code – no mocks or subclassing are used.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private JsonSchema parentSchema; // can be null for root schemas

    @Before
    public void setUp() {
        // Minimal but functional ValidationContext – the concrete implementations of the
        // factories are part of the library and have public no‑arg constructors.
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV201909(); // default meta‑schema
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema, schemaFactory, config);
        parentSchema = null; // root level – no parent needed for these tests
    }

    /**
     * Helper to create a OneOfValidator from a JSON array string that represents the
     * {@code oneOf} keyword value.
     */
    private OneOfValidator createValidator(String schemaPath, String oneOfArrayJson) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(oneOfArrayJson);
        return new OneOfValidator(schemaPath, schemaNode, parentSchema, validationContext);
    }

    /**
     * Basic test for the public constructor – ensures that the internal list of
     * subschemas is populated correctly.
     */
    @Test
    public void testConstructorPopulatesSchemas() throws Exception {
        String oneOf = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        OneOfValidator validator = createValidator("#/oneOf", oneOf);
        // The private field is not directly accessible; we verify via behaviour.
        JsonNode testString = MAPPER.readTree("\"hello\"");
        Set<ValidationMessage> errors = validator.validate(new ExecutionContext(), testString,
                testString, "$");
        // A string matches exactly one subschema → no errors.
        assertTrue("Expected no validation errors for a matching string", errors.isEmpty());
    }

    /**
     * Validates that a node matching exactly one subschema yields an empty error set.
     */
    @Test
    public void testValidateExactlyOneMatch() throws Exception {
        String oneOf = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        OneOfValidator validator = createValidator("#/oneOf", oneOf);

        JsonNode jsonString = MAPPER.readTree("\"a simple string\"");
        Set<ValidationMessage> errors = validator.validate(new ExecutionContext(),
                jsonString, jsonString, "$");

        assertTrue("When exactly one schema matches, there should be no errors", errors.isEmpty());
    }

    /**
     * Validates that a node matching none of the subschemas produces a single
     * {@code oneOf} error with the argument {@code "0"}.
     */
    @Test
    public void testValidateNoMatch() throws Exception {
        String oneOf = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        OneOfValidator validator = createValidator("#/oneOf", oneOf);

        JsonNode jsonBoolean = MAPPER.readTree("true");
        Set<ValidationMessage> errors = validator.validate(new ExecutionContext(),
                jsonBoolean, jsonBoolean, "$");

        assertEquals("Exactly one error (the oneOf error) should be returned", 1, errors.size());
        ValidationMessage message = errors.iterator().next();
        assertEquals("The error message should contain the number of valid schemas (0)",
                "0", message.getArguments()[0]);
    }

    /**
     * Validates that a node matching more than one subschema yields a {@code oneOf}
     * error with the argument {@code "2"} and that child errors from the individual
     * schemas are also collected.
     */
    @Test
    public void testValidateMultipleMatches() throws Exception {
        // Both schemas accept any number → a numeric node matches both.
        String oneOf = "[{\"type\":\"number\"},{\"minimum\":0}]";
        OneOfValidator validator = createValidator("#/oneOf", oneOf);

        JsonNode jsonNumber = MAPPER.readTree("5");
        Set<ValidationMessage> errors = validator.validate(new ExecutionContext(),
                jsonNumber, jsonNumber, "$");

        // Should contain the oneOf error plus any child errors (none in this simple case)
        assertEquals("One error for the oneOf violation should be present", 1, errors.size());

        ValidationMessage oneOfMessage = errors.iterator().next();
        assertEquals("The error argument must indicate two matching schemas",
                "2", oneOfMessage.getArguments()[0]);
    }

    /**
     * Tests the {@code walk} method when {@code shouldValidateSchema} is {@code true}.
     * The method should delegate to {@code validate} and return the same error set.
     */
    @Test
    public void testWalkValidateTrue() throws Exception {
        String oneOf = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        OneOfValidator validator = createValidator("#/oneOf", oneOf);

        JsonNode jsonBoolean = MAPPER.readTree("false");
        Set<ValidationMessage> walkResult = validator.walk(new ExecutionContext(),
                jsonBoolean, jsonBoolean, "$", true);

        Set<ValidationMessage> validateResult = validator.validate(new ExecutionContext(),
                jsonBoolean, jsonBoolean, "$");

        assertEquals("walk(…, true) must return the same set as validate()", validateResult, walkResult);
    }

    /**
     * Tests the {@code walk} method when {@code shouldValidateSchema} is {@code false}.
     * No validation errors should be returned, regardless of the node content.
     */
    @Test
    public void testWalkValidateFalse() throws Exception {
        String oneOf = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        OneOfValidator validator = createValidator("#/oneOf", oneOf);

        JsonNode jsonNumber = MAPPER.readTree("10");
        Set<ValidationMessage> walkResult = validator.walk(new ExecutionContext(),
                jsonNumber, jsonNumber, "$", false);

        assertTrue("walk with shouldValidateSchema=false must return an empty set", walkResult.isEmpty());
    }

    /**
     * Calls {@code preloadJsonSchema} to ensure that it does not throw and that the
     * internal subschemas are initialized. The library does not expose a direct
     * observable state for "initialized", therefore the test simply verifies that
     * the method completes without exception.
     */
    @Test
    public void testPreloadJsonSchema() throws Exception {
        String oneOf = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        OneOfValidator validator = createValidator("#/oneOf", oneOf);

        // No exception → success
        validator.preloadJsonSchema();
    }

    /**
     * Edge‑case test: verify that when the validator is configured in fail‑fast mode
     * (via the {@code failFast} flag in {@link BaseJsonValidator}), a {@link JsonSchemaException}
     * is thrown as soon as the number of matching schemas is not exactly one.
     *
     * The {@code failFast} flag can be set through the {@link ValidationContext} config.
     */
    @Test(expected = JsonSchemaException.class)
    public void testValidateFailFast() throws Exception {
        // Enable fail‑fast in the configuration.
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setFailFast(true);
        validationContext.setConfig(config);

        String oneOf = "[{\"type\":\"string\"},{\"type\":\"integer\"}]";
        OneOfValidator validator = createValidator("#/oneOf", oneOf);

        JsonNode jsonBoolean = MAPPER.readTree("true");
        // This should trigger a JsonSchemaException because no schema matches.
        validator.validate(new ExecutionContext(), jsonBoolean, jsonBoolean, "$");
    }

    /**
     * Utility test that demonstrates the public method signatures collected from the class.
     * This does not perform runtime checks but documents the API for reference.
     */
    @Test
    public void testPublicMethodSignatures() throws Exception {
        // Signatures (for documentation purposes only)
        //   OneOfValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext)
        //   Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at)
        //   Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema)
        //   void preloadJsonSchema()
        // The test simply ensures the class can be instantiated and the methods are callable.
        String oneOf = "[{\"type\":\"string\"}]";
        OneOfValidator validator = createValidator("#/oneOf", oneOf);
        assertNotNull("Validator instance must be created", validator);
    }
}
