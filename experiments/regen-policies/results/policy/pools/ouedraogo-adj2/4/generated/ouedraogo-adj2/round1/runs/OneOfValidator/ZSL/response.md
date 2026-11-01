###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * <p>The tests build real {@link JsonSchema} instances via {@link ValidationContext},
 * create an {@link ExecutionContext} with a fresh {@link CollectorContext},
 * and then exercise the public behaviour of {@code OneOfValidator}:
 *
 * <ul>
 *   <li>construction with a list of subschemas</li>
 *   <li>validation when exactly one subschema matches</li>
 *   <li>validation when none matches</li>
 *   <li>validation when more than one matches (short‑circuit)</li>
 *   <li>the {@code walk} method in both “validate” and “skip‑validation” modes</li>
 *   <li>pre‑loading of child schemas via {@code preloadJsonSchema()}</li>
 * </ul>
 *
 * <p>The test class uses only the public API that is part of the source fragment
 * supplied in the prompt – no additional third‑party mocking frameworks are used.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private JsonNodePath rootPath;
    private SchemaLocation rootLocation;

    @Before
    public void setUp() {
        // -----------------------------------------------------------------
        // Build the minimal objects required by the library.
        // All of the following classes have public no‑arg constructors in
        // the official “networknt” library, therefore they can be instantiated
        // directly.
        // -----------------------------------------------------------------
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7(); // any meta‑schema works
        JsonSchemaFactory schemaFactory = new JsonSchemaFactory();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        validationContext = new ValidationContext(
                uriFactory,
                urnFactory,
                metaSchema,
                schemaFactory,
                config
        );

        // ExecutionContext with a fresh CollectorContext (used internally by
        // OneOfValidator to handle dynamic scopes).
        executionContext = new ExecutionContext(new CollectorContext());

        // Simple root location / path – they are only used for error messages.
        rootLocation = SchemaLocation.DOCUMENT;
        rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
    }

    /**
     * Helper that creates a {@link OneOfValidator} from a list of JSON schema
     * fragments. Each fragment is turned into a {@link JsonSchema} via the
     * {@link ValidationContext#newSchema} factory method and then handed to the
     * validator constructor.
     *
     * @param schemaFragments JSON nodes representing the individual subschemas
     * @return a fully initialised {@code OneOfValidator}
     */
    private OneOfValidator createValidator(List<JsonNode> schemaFragments) {
        // Build an ArrayNode that represents the "oneOf" array.
        JsonNode oneOfArray = MAPPER.createArrayNode();
        for (JsonNode fragment : schemaFragments) {
            ((com.fasterxml.jackson.databind.node.ArrayNode) oneOfArray).add(fragment);
        }

        // The parent schema is not needed for the tests – we pass {@code null}.
        return new OneOfValidator(
                rootLocation,
                rootPath,
                oneOfArray,
                null,
                validationContext
        );
    }

    /**
     * Scenario: exactly one subschema matches the instance.
     * Expected outcome – an empty error set and the validator state signals a match.
     */
    @Test
    public void validate_ExactlyOneMatches_ShouldReturnNoErrors() throws Exception {
        // subschema 0 : must be an integer
        JsonNode intSchema = MAPPER.readTree("{\"type\":\"integer\"}");
        // subschema 1 : must be a string
        JsonNode stringSchema = MAPPER.readTree("{\"type\":\"string\"}");

        OneOfValidator validator = createValidator(Arrays.asList(intSchema, stringSchema));

        // Instance that satisfies only the first subschema (the integer)
        JsonNode instance = MAPPER.readTree("5");

        Set<ValidationMessage> messages = validator.validate(
                executionContext,
                instance,
                instance,               // root node is the same as the instance
                rootPath
        );

        assertTrue("When exactly one subschema matches there must be no errors", messages.isEmpty());

        // The validator state should have been marked as matched.
        assertTrue(executionContext.getValidatorState().hasMatchedNode());
    }

    /**
     * Scenario: none of the subschemas match.
     * Expected outcome – a single ONE_OF error (argument = 0) plus the child
     * errors coming from each subschema.
     */
    @Test
    public void validate_NoneMatch_ShouldReturnOneOfErrorAndChildErrors() throws Exception {
        // subschema 0 : integer
        JsonNode intSchema = MAPPER.readTree("{\"type\":\"integer\"}");
        // subschema 1 : string with minLength 3
        JsonNode stringSchema = MAPPER.readTree("{\"type\":\"string\",\"minLength\":3}");

        OneOfValidator validator = createValidator(Arrays.asList(intSchema, stringSchema));

        // Instance that matches neither (a boolean)
        JsonNode instance = MAPPER.readTree("true");

        Set<ValidationMessage> messages = validator.validate(
                executionContext,
                instance,
                instance,
                rootPath
        );

        // There must be at least the ONE_OF message.
        assertFalse("There should be errors when no subschema matches", messages.isEmpty());

        // Find the ONE_OF message – its code is defined by the validator type.
        Optional<ValidationMessage> oneOfMessage = messages.stream()
                .filter(m -> m.getCode().equals(ValidatorTypeCode.ONE_OF.getErrorCode()))
                .findFirst();

        assertTrue("ONE_OF validation message must be present", oneOfMessage.isPresent());

        // The ONE_OF message should contain the number of valid subschemas (0)
        assertArrayEquals(new Object[]{"0"}, oneOfMessage.get().getArguments());

        // Child errors from each subschema must also be present (type errors)
        long typeErrorCount = messages.stream()
                .filter(m -> m.getCode().equals("type"))
                .count();
        assertEquals("Both subschemas should have produced a type error", 2, typeErrorCount);
    }

    /**
     * Scenario: more than one subschema matches.
     * Expected outcome – ONE_OF error (argument = 2) and validation short‑circuits
     * after the second match; child errors are still collected from the already
     * processed subschemas.
     */
    @Test
    public void validate_MultipleMatch_ShouldReturnOneOfError() throws Exception {
        // subschema 0 : any integer
        JsonNode intSchema = MAPPER.readTree("{\"type\":\"integer\"}");
        // subschema 1 : any number (covers integers as well)
        JsonNode numberSchema = MAPPER.readTree("{\"type\":\"number\"}");

        OneOfValidator validator = createValidator(Arrays.asList(intSchema, numberSchema));

        // Instance that is an integer – matches both subschemas.
        JsonNode instance = MAPPER.readTree("42");

        Set<ValidationMessage> messages = validator.validate(
                executionContext,
                instance,
                instance,
                rootPath
        );

        // ONE_OF error must be present with argument "2"
        Optional<ValidationMessage> oneOfMessage = messages.stream()
                .filter(m -> m.getCode().equals(ValidatorTypeCode.ONE_OF.getErrorCode()))
                .findFirst();

        assertTrue("ONE_OF validation message must be present when >1 subschemas match", oneOfMessage.isPresent());
        assertArrayEquals(new Object[]{"2"}, oneOfMessage.get().getArguments());

        // No type errors should be present because both subschemas succeeded.
        long typeErrorCount = messages.stream()
                .filter(m -> m.getCode().equals("type"))
                .count();
        assertEquals("No type errors should be reported when matches exist", 0, typeErrorCount);
    }

    /**
     * The {@code walk} method delegates to {@code validate} when the flag
     * {@code shouldValidateSchema} is {@code true}.  This test confirms that the
     * same error set is produced.
     */
    @Test
    public void walk_WithValidation_ShouldDelegateToValidate() throws Exception {
        JsonNode boolSchema = MAPPER.readTree("{\"type\":\"boolean\"}");
        JsonNode stringSchema = MAPPER.readTree("{\"type\":\"string\"}");

        OneOfValidator validator = createValidator(Arrays.asList(boolSchema, stringSchema));

        JsonNode instance = MAPPER.readTree("false"); // matches only the first subschema

        Set<ValidationMessage> validateMessages = validator.validate(
                executionContext,
                instance,
                instance,
                rootPath
        );

        Set<ValidationMessage> walkMessages = validator.walk(
                executionContext,
                instance,
                instance,
                rootPath,
                true   // request validation
        );

        assertEquals("walk(true) must return exactly the same messages as validate()", validateMessages, walkMessages);
    }

    /**
     * When {@code shouldValidateSchema} is {@code false} the validator must walk
     * each child schema without collecting any validation messages.
     */
    @Test
    public void walk_WithoutValidation_ShouldNotCollectErrors() throws Exception {
        JsonNode intSchema = MAPPER.readTree("{\"type\":\"integer\"}");
        JsonNode stringSchema = MAPPER.readTree("{\"type\":\"string\"}");

        OneOfValidator validator = createValidator(Arrays.asList(intSchema, stringSchema));

        JsonNode instance = MAPPER.readTree("\"hello\""); // matches only the string schema

        Set<ValidationMessage> walkMessages = validator.walk(
                executionContext,
                instance,
                instance,
                rootPath,
                false   // do not validate – just walk
        );

        // The walk method returns the messages it collected itself; when validation
        // is disabled it must return an empty set.
        assertTrue("walk(false) must return an empty set", walkMessages.isEmpty());
    }

    /**
     * {@code preloadJsonSchema()} must invoke {@code initializeValidators()} on
     * each child schema.  The easiest observable effect is that after pre‑loading,
     * the validator can be used without throwing a {@link JsonSchemaException}.
     */
    @Test
    public void preloadJsonSchema_ShouldPrepareChildSchemas() throws Exception {
        JsonNode intSchema = MAPPER.readTree("{\"type\":\"integer\"}");
        JsonNode stringSchema = MAPPER.readTree("{\"type\":\"string\"}");

        OneOfValidator validator = createValidator(Arrays.asList(intSchema, stringSchema));

        // Pre‑load – this should not raise any exception.
        validator.preloadJsonSchema();

        // After pre‑loading, validation must still work.
        JsonNode instance = MAPPER.readTree("123");
        Set<ValidationMessage> messages = validator.validate(
                executionContext,
                instance,
                instance,
                rootPath
        );
        assertTrue("After preload, validation should still succeed when exactly one subschema matches", messages.isEmpty());
    }
}
```
###Test END##