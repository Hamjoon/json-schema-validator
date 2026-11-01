###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public constructors and method signatures.</li>
 *   <li>Basic successful validation (exactly one subschema matches).</li>
 *   <li>Edge cases:
 *       <ul>
 *         <li>no subschema matches</li>
 *         <li>more than one subschema matches</li>
 *         <li>empty {@code oneOf} array</li>
 *       </ul>
 *   </li>
 *   <li>{@code walk} behaviour with {@code shouldValidateSchema} true and false.</li>
 *   <li>{@code preloadJsonSchema} invokes {@code initializeValidators()} on each child schema.</li>
 * </ul>
 *
 * The test creates real {@link ValidationContext}, {@link JsonSchema},
 * {@link ExecutionContext}, {@link JsonNode} and related objects – no mocking
 * or subclassing of library classes is performed.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private JsonNodePath rootPath;
    private SchemaLocation rootLocation;

    @Before
    public void setUp() {
        // Minimal, but fully functional, ValidationContext components.
        // All of these classes have public no‑arg constructors in the library.
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();               // latest draft‑07 meta‑schema
        JsonSchemaFactory schemaFactory = new JsonSchemaFactory();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema, schemaFactory, config);
        executionContext = new ExecutionContext();                       // uses a fresh CollectorContext internally
        rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        rootLocation = SchemaLocation.DOCUMENT;
    }

    /**
     * Helper that builds a {@link OneOfValidator} from a JSON array node.
     */
    private OneOfValidator buildValidator(ArrayNode oneOfArray) {
        return new OneOfValidator(
                rootLocation,
                rootPath,
                oneOfArray,
                null,               // parent schema – not needed for these tests
                validationContext);
    }

    /** ----------------------------------------------------------------------
     *  PUBLIC METHOD LISTING (extracted from the source)
     * ---------------------------------------------------------------------- */
    // public OneOfValidator(SchemaLocation schemaLocation,
    //                         JsonNodePath evaluationPath,
    //                         JsonNode schemaNode,
    //                         JsonSchema parentSchema,
    //                         ValidationContext validationContext)
    //
    // public Set<ValidationMessage> validate(ExecutionContext executionContext,
    //                                        JsonNode node,
    //                                        JsonNode rootNode,
    //                                        JsonNodePath instanceLocation)
    //
    // public Set<ValidationMessage> walk(ExecutionContext executionContext,
    //                                    JsonNode node,
    //                                    JsonNode rootNode,
    //                                    JsonNodePath instanceLocation,
    //                                    boolean shouldValidateSchema)
    //
    // public void preloadJsonSchema()

    /** ----------------------------------------------------------------------
     *  BASIC FUNCTIONALITY TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testValidate_ExactlyOneMatch_ShouldPass() {
        // oneOf: [ { "type":"string" }, { "type":"integer" } ]
        ArrayNode oneOf = MAPPER.createArrayNode();
        ObjectNode schemaString = MAPPER.createObjectNode().put("type", "string");
        ObjectNode schemaInteger = MAPPER.createObjectNode().put("type", "integer");
        oneOf.add(schemaString);
        oneOf.add(schemaInteger);

        OneOfValidator validator = buildValidator(oneOf);

        JsonNode instance = MAPPER.readTree("\"hello\""); // a JSON string
        Set<ValidationMessage> errors = validator.validate(executionContext, instance, instance, rootPath);

        assertTrue("No errors expected when exactly one subschema matches", errors.isEmpty());

        // state should be marked as matched – we can infer this because no errors were added.
    }

    @Test
    public void testValidate_NoMatch_ShouldReturnOneOfError() {
        // oneOf: [ { "type":"integer" }, { "type":"number" } ]
        ArrayNode oneOf = MAPPER.createArrayNode();
        oneOf.add(MAPPER.createObjectNode().put("type", "integer"));
        oneOf.add(MAPPER.createObjectNode().put("type", "number"));

        OneOfValidator validator = buildValidator(oneOf);

        JsonNode instance = MAPPER.readTree("\"text\""); // a JSON string – matches neither
        Set<ValidationMessage> errors = validator.validate(executionContext, instance, instance, rootPath);

        assertFalse("Errors expected when no subschema matches", errors.isEmpty());

        // The first error must be the OneOf aggregate error; its arguments contain the count (0)
        ValidationMessage first = errors.iterator().next();
        assertEquals("0", first.getArguments()[0]);
    }

    @Test
    public void testValidate_MultipleMatches_ShouldReturnOneOfError() {
        // oneOf: [ {}, {} ] – empty schemas accept everything, so both match.
        ArrayNode oneOf = MAPPER.createArrayNode();
        oneOf.add(MAPPER.createObjectNode()); // empty schema
        oneOf.add(MAPPER.createObjectNode()); // empty schema

        OneOfValidator validator = buildValidator(oneOf);

        JsonNode instance = MAPPER.readTree("123"); // any JSON value
        Set<ValidationMessage> errors = validator.validate(executionContext, instance, instance, rootPath);

        assertFalse("Errors expected when more than one subschema matches", errors.isEmpty());

        ValidationMessage first = errors.iterator().next();
        // arguments[0] should be "2" because two schemas were valid
        assertEquals("2", first.getArguments()[0]);
    }

    @Test
    public void testValidate_EmptyOneOfArray_ShouldReturnOneOfError() {
        // oneOf: [] – nothing can match.
        ArrayNode oneOf = MAPPER.createArrayNode(); // empty array

        OneOfValidator validator = buildValidator(oneOf);

        JsonNode instance = MAPPER.readTree("true"); // any JSON value
        Set<ValidationMessage> errors = validator.validate(executionContext, instance, instance, rootPath);

        assertFalse("Errors expected for empty oneOf array", errors.isEmpty());

        ValidationMessage first = errors.iterator().next();
        assertEquals("0", first.getArguments()[0]); // zero valid schemas
    }

    /** ----------------------------------------------------------------------
     *  WALK METHOD TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testWalk_ShouldDelegateToValidateWhenFlagTrue() {
        // oneOf: [ { "type":"string" }, { "type":"integer" } ]
        ArrayNode oneOf = MAPPER.createArrayNode();
        oneOf.add(MAPPER.createObjectNode().put("type", "string"));
        oneOf.add(MAPPER.createObjectNode().put("type", "integer"));

        OneOfValidator validator = buildValidator(oneOf);

        JsonNode instance = MAPPER.readTree("\"world\"");
        Set<ValidationMessage> errorsFromWalk = validator.walk(executionContext, instance, instance, rootPath, true);

        // With shouldValidateSchema == true the walk method simply calls validate()
        assertTrue("Walk with shouldValidateSchema=true should behave like validate()", errorsFromWalk.isEmpty());
    }

    @Test
    public void testWalk_ShouldNotValidateWhenFlagFalse() {
        // oneOf: [ { "type":"string" }, { "type":"integer" } ]
        ArrayNode oneOf = MAPPER.createArrayNode();
        oneOf.add(MAPPER.createObjectNode().put("type", "string"));
        oneOf.add(MAPPER.createObjectNode().put("type", "integer"));

        OneOfValidator validator = buildValidator(oneOf);

        JsonNode instance = MAPPER.readTree("\"ignore\"");

        // shouldValidateSchema = false – walk on each child schema is performed,
        // but the method does not collect any messages.
        Set<ValidationMessage> result = validator.walk(executionContext, instance, instance, rootPath, false);

        assertTrue("When shouldValidateSchema is false, walk must return an empty set", result.isEmpty());
    }

    /** ----------------------------------------------------------------------
     *  PRELOAD METHOD TEST
     * ---------------------------------------------------------------------- */

    @Test
    public void testPreloadJsonSchema_InvokesInitializeOnChildren() {
        // oneOf with two simple schemas
        ArrayNode oneOf = MAPPER.createArrayNode();
        oneOf.add(MAPPER.createObjectNode().put("type", "string"));
        oneOf.add(MAPPER.createObjectNode().put("type", "integer"));

        OneOfValidator validator = buildValidator(oneOf);

        // The validator stores the child JsonSchema objects in its private list.
        // Calling preloadJsonSchema() must invoke initializeValidators() on each child.
        // Since initializeValidators() does not return a value, we verify that no
        // exception is thrown and that subsequent validation works (which would fail
        // if the child schemas were not initialized).
        validator.preloadJsonSchema();

        // After pre‑loading, a valid instance should still validate correctly.
        JsonNode instance = MAPPER.readTree("42"); // integer matches the second schema only
        Set<ValidationMessage> errors = validator.validate(executionContext, instance, instance, rootPath);
        assertTrue("Validation must still succeed after preloadJsonSchema()", errors.isEmpty());
    }
}
```
###Test END##