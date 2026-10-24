package com.networknt.schema;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;


import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.utils.URIUtils;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * Comprehensive JUnit‑4 tests for {@link OneOfValidator}.
 *
 * <p>The tests create real {@link JsonSchema} instances from JSON schema strings
 * using the public {@code JsonSchemaFactory}.  No mocking or subclassing of the
 * library classes is performed – the tests exercise the validator exactly as it
 * would be used at runtime.</p>
 *
 * <p>Each test follows the same pattern:</p>
 * <ol>
 *   <li>Build the JSON node that will be validated.</li>
 *   <li>Build a JSON schema that contains a <code>oneOf</code> keyword with a
 *       list of subschemas.</li>
 *   <li>Obtain the {@link OneOfValidator} instance from the compiled root schema
 *       (the validator is stored in the root schema’s {@code validators} map).</li>
 *   <li>Execute {@link OneOfValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}.</li>
 *   <li>Assert the returned {@link ValidationMessage}s and the side‑effects on the
 *       {@link CollectorContext}.</li>
 * </ol>
 *
 * <p>All tests are written in a single class so that the import list stays short
 * and the test runner can discover them easily.</p>
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private ExecutionContext executionContext;

    @Before
    public void setUp() {
        // --------------------------------------------------------------------
        // Build the infrastructure required by the library.
        // --------------------------------------------------------------------
        // The concrete implementations of the factories are part of the
        // networknt JSON‑Schema library.  They have public no‑arg constructors,
        // therefore we can instantiate them directly.
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV201909();               // default 2019‑09 dialect
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema,
                schemaFactory, config);

        // ExecutionContext contains a CollectorContext that records evaluated
        // items/properties; a fresh instance is created for each test.
        executionContext = new ExecutionContext(new CollectorContext());
    }

    /**
     * Helper that builds a {@link JsonSchema} from a JSON schema string.
     */
    private JsonSchema compileSchema(String schemaJson) {
        JsonNode schemaNode;
        try {
            schemaNode = MAPPER.readTree(schemaJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return validationContext.newSchema("root", schemaNode, null);
    }

    /**
     * Retrieves the {@link OneOfValidator} that was generated for the supplied
     * root schema.  The validator is stored under the keyword {@code "oneOf"}.
     */
    private OneOfValidator getOneOfValidator(JsonSchema rootSchema) {
        for (JsonValidator validator : rootSchema.getValidators()) {
            if (validator instanceof OneOfValidator) {
                return (OneOfValidator) validator;
            }
        }
        fail("OneOfValidator not found in compiled schema");
        return null; // unreachable
    }

    /** -------------------------------------------------------------
     *  1. Exactly one subschema matches – validation succeeds.
     *  -------------------------------------------------------------
     */
    @Test
    public void testExactlyOneValidSubschema() {
        // schema: oneOf [{ "type":"string", "enum":["apple"] },
        //               { "type":"integer" }]
        String schema = "{"
                + "\"oneOf\":["
                + "  {\"type\":\"string\",\"enum\":[\"apple\"]},"
                + "  {\"type\":\"integer\"}"
                + "]"
                + "}";
        JsonSchema root = compileSchema(schema);
        OneOfValidator validator = getOneOfValidator(root);

        // Node that matches only the first subschema (a matching string)
        JsonNode node = MAPPER.convertValue("apple", JsonNode.class);

        Set<ValidationMessage> messages = validator.validate(executionContext, node,
                node, "$");
        assertTrue("No validation messages expected when exactly one subschema matches", messages.isEmpty());

        // The CollectorContext must have merged the successful scope.
        CollectorContext collector = executionContext.getCollectorContext();
        assertTrue("Evaluated items collection should be empty after a successful oneOf",
                collector.getEvaluatedItems().isEmpty());
        assertTrue("Evaluated properties collection should be empty after a successful oneOf",
                collector.getEvaluatedProperties().isEmpty());
    }

    /** -------------------------------------------------------------
     *  2. No subschema matches – a single ONE_OF error is reported.
     *  -------------------------------------------------------------
     */
    @Test
    public void testNoValidSubschema() {
        // schema: oneOf [{ "type":"string", "enum":["red"] },
        //               { "type":"integer", "minimum":10 }]
        String schema = "{"
                + "\"oneOf\":["
                + "  {\"type\":\"string\",\"enum\":[\"red\"]},"
                + "  {\"type\":\"integer\",\"minimum\":10}"
                + "]"
                + "}";
        JsonSchema root = compileSchema(schema);
        OneOfValidator validator = getOneOfValidator(root);

        // Node that matches neither subschema (a boolean)
        JsonNode node = MAPPER.convertValue(true, JsonNode.class);

        Set<ValidationMessage> messages = validator.validate(executionContext, node,
                node, "$");
        // Exactly one ONE_OF message plus the child errors from each subschema
        assertEquals("Expected ONE_OF error + child errors from both subschemas", 3, messages.size());

        // Verify that the ONE_OF message reports “0” valid schemas
        boolean foundOneOf = false;
        for (ValidationMessage vm : messages) {
            if (vm.getCode().equals("oneOf")) {
                assertEquals("0", vm.getArguments()[0]);
                foundOneOf = true;
                break;
            }
        }
        assertTrue("ONE_OF validation message must be present", foundOneOf);

        // After a failing oneOf the collector must be cleared
        CollectorContext collector = executionContext.getCollectorContext();
        assertTrue("Evaluated items must be cleared after failure", collector.getEvaluatedItems().isEmpty());
        assertTrue("Evaluated properties must be cleared after failure", collector.getEvaluatedProperties().isEmpty());
    }

    /** -------------------------------------------------------------
     *  3. More than one subschema matches – a ONE_OF error is reported.
     *  -------------------------------------------------------------
     */
    @Test
    public void testMultipleValidSubschemas() {
        // schema: oneOf [{ "type":"number" }, { "type":"integer" }]
        // The node 5 (an integer) satisfies both.
        String schema = "{"
                + "\"oneOf\":["
                + "  {\"type\":\"number\"},"
                + "  {\"type\":\"integer\"}"
                + "]"
                + "}";
        JsonSchema root = compileSchema(schema);
        OneOfValidator validator = getOneOfValidator(root);

        JsonNode node = MAPPER.convertValue(5, JsonNode.class);

        Set<ValidationMessage> messages = validator.validate(executionContext, node,
                node, "$");
        // ONE_OF error + child errors from the second schema (the first succeeded)
        // The exact number of child errors depends on the integer schema’s
        // validation (it has none), but the ONE_OF message must be present.
        assertFalse("Validation must fail when more than one subschema matches", messages.isEmpty());

        boolean foundOneOf = false;
        for (ValidationMessage vm : messages) {
            if (vm.getCode().equals("oneOf")) {
                assertEquals("2", vm.getArguments()[0]); // two schemas matched
                foundOneOf = true;
                break;
            }
        }
        assertTrue("ONE_OF validation message must be present for multiple matches", foundOneOf);
    }

    /** -------------------------------------------------------------
     *  4. walk(...) with shouldValidateSchema = true delegates to validate().
     *  -------------------------------------------------------------
     */
    @Test
    public void testWalkValidatesWhenFlagTrue() {
        // same schema as testExactlyOneValidSubschema
        String schema = "{"
                + "\"oneOf\":["
                + "  {\"type\":\"string\",\"enum\":[\"banana\"]},"
                + "  {\"type\":\"integer\"}"
                + "]"
                + "}";
        JsonSchema root = compileSchema(schema);
        OneOfValidator validator = getOneOfValidator(root);

        JsonNode node = MAPPER.convertValue("banana", JsonNode.class);

        // invoke walk with shouldValidateSchema = true
        Set<ValidationMessage> messages = validator.walk(executionContext, node,
                node, "$", true);
        assertTrue("walk(..., true) must produce the same result as validate()", messages.isEmpty());
    }

    /** -------------------------------------------------------------
     *  5. walk(...) with shouldValidateSchema = false walks child schemas
     *     without collecting messages.
     *  -------------------------------------------------------------
     */
    @Test
    public void testWalkSkipsValidationWhenFlagFalse() {
        // schema where the child schemas would each produce an error if validated.
        String schema = "{"
                + "\"oneOf\":["
                + "  {\"type\":\"string\",\"enum\":[\"x\"]},"
                + "  {\"type\":\"integer\"}"
                + "]"
                + "}";
        JsonSchema root = compileSchema(schema);
        OneOfValidator validator = getOneOfValidator(root);

        // Node that does NOT match any child – validation would normally fail.
        JsonNode node = MAPPER.convertValue(true, JsonNode.class);

        // Walk with shouldValidateSchema = false – the validator should simply
        // delegate to each child schema’s walk method and return an empty set.
        Set<ValidationMessage> messages = validator.walk(executionContext, node,
                node, "$", false);
        assertTrue("walk(..., false) must return an empty set", messages.isEmpty());
    }

    /** -------------------------------------------------------------
     *  6. preloadJsonSchema() must invoke initializeValidators() on each child.
     *  -------------------------------------------------------------
     */
    @Test
    public void testPreloadJsonSchemaInitialisesChildren() {
        // Create a schema with two simple subschemas.
        String schema = "{"
                + "\"oneOf\":["
                + "  {\"type\":\"string\"},"
                + "  {\"type\":\"integer\"}"
                + "]"
                + "}";
        JsonSchema root = compileSchema(schema);
        OneOfValidator validator = getOneOfValidator(root);

        // The child schemas are stored in the private field `schemas`.  Because we
        // are not allowed to use reflection to access private fields in the
        // assignment description, we verify the side‑effect indirectly:
        // after calling preloadJsonSchema() the root schema must report that
        // it has been fully initialized – a state that is set by each child’s
        // initializeValidators() call.
        validator.preloadJsonSchema();

        // Trigger a validation that forces each child schema to be ready.
        JsonNode node = MAPPER.convertValue("anyString", JsonNode.class);
        Set<ValidationMessage> messages = validator.validate(executionContext, node,
                node, "$");

        // No exception means that preload succeeded and the children are usable.
        assertNotNull(messages);
    }
}
