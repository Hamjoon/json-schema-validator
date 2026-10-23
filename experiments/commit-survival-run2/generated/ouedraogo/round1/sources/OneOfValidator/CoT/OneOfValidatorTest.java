package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OneOfValidator}.  The tests use Mockito to mock the heavy
 * dependencies ( {@link JsonSchema}, {@link ExecutionContext}, {@link CollectorContext},
 * {@link ValidatorState}, etc.) so that we can focus on the logic inside
 * {@code OneOfValidator#validate}, {@code #walk} and {@code #preloadJsonSchema}.
 */
@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private ValidatorState validatorState;

    @Mock
    private Scope dummyScope;               // used for both enter/exit scopes

    @Mock
    private Set<JsonNode> evaluatedItems;   // cleared after a validation error

    @Mock
    private Set<String> evaluatedProperties; // cleared after a validation error

    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Common mock set‑up executed before each test.
     */
    @Before
    public void setUp() {
        // ExecutionContext → CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // CollectorContext → dynamic scopes
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);

        // CollectorContext → state object
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // CollectorContext → evaluated collections (used only for clear() verification)
        when(collectorContext.getEvaluatedItems()).thenReturn(evaluatedItems);
        when(collectorContext.getEvaluatedProperties()).thenReturn(evaluatedProperties);

        // Default behaviour of the validator state
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
    }

    /**
     * Helper that creates a {@link OneOfValidator} for a given {@link ArrayNode}
     * containing *n* child schema nodes.  The {@code validationContext} mock is
     * configured to return a distinct {@link JsonSchema} mock for each child.
     */
    private OneOfValidator createValidator(ArrayNode schemaArray,
                                            JsonSchema... schemaMocks) {
        // the constructor iterates over the array and calls validationContext.newSchema(...)
        // for each element.  We set up the mock to return the supplied schema mocks in order.
        OngoingStubbing<JsonSchema> stub = when(validationContext.newSchema(anyString(),
                any(JsonNode.class), any(JsonSchema.class)));
        for (JsonSchema mock : schemaMocks) {
            stub = stub.thenReturn(mock);
        }

        // the path argument does not matter for the test – we use a dummy string.
        return new OneOfValidator("dummyPath", schemaArray, null, validationContext);
    }

    /**
     * ------------------------------------------------------------
     *  PUBLIC METHODS OF ONEOFVALIDATOR (extracted from the source)
     * ------------------------------------------------------------
     *
     * {@code public OneOfValidator(String schemaPath, JsonNode schemaNode,
     *          JsonSchema parentSchema, ValidationContext validationContext)}
     *
     * {@code public Set<ValidationMessage> validate(ExecutionContext executionContext,
     *          JsonNode node, JsonNode rootNode, String at)}
     *
     * {@code public Set<ValidationMessage> walk(ExecutionContext executionContext,
     *          JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema)}
     *
     * {@code public void preloadJsonSchema()}
     *
     * ------------------------------------------------------------
     */

    /* --------------------------------------------------------------------- *
     *  BASIC FUNCTIONALITY TESTS
     * --------------------------------------------------------------------- */

    /**
     * When exactly ONE subschema validates successfully the validator must
     * return an empty error set and mark the node as matched.
     */
    @Test
    public void testValidateExactlyOneSchemaMatches() {
        // two subschemas – first fails, second succeeds
        JsonSchema failingSchema = mock(JsonSchema.class);
        JsonSchema succeedingSchema = mock(JsonSchema.class);

        // failing schema returns a non‑empty error set
        Set<ValidationMessage> failErrors = Collections.singleton(
                ValidationMessage.builder().type("type1").path("$").message("fail").build()
        );
        when(failingSchema.validate(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString())).thenReturn(failErrors);

        // succeeding schema returns an empty set (i.e. validation passed)
        when(succeedingSchema.validate(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());

        // build an array node with two dummy child objects
        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());
        schemaArray.add(mapper.createObjectNode());

        OneOfValidator validator = createValidator(schemaArray, failingSchema, succeedingSchema);

        // invoke validation
        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");

        // Expect no errors because exactly one schema matched
        assertTrue("Result should be empty when a single schema matches", result.isEmpty());

        // Verify that the validator state was marked as matched
        verify(validatorState).setMatchedNode(true);
    }

    /**
     * When NO subschema matches the validator must return an error whose
     * message contains the number of valid schemas (zero) and must also
     * include the child errors from each subschema.
     */
    @Test
    public void testValidateNoSchemaMatches() {
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);

        Set<ValidationMessage> errorsA = Collections.singleton(
                ValidationMessage.builder().type("a").path("$").message("a error").build()
        );
        Set<ValidationMessage> errorsB = Collections.singleton(
                ValidationMessage.builder().type("b").path("$").message("b error").build()
        );

        when(schemaA.validate(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString())).thenReturn(errorsA);
        when(schemaB.validate(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString())).thenReturn(errorsB);

        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());
        schemaArray.add(mapper.createObjectNode());

        OneOfValidator validator = createValidator(schemaArray, schemaA, schemaB);

        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");

        // Should contain the aggregated child errors (2) plus the OneOf error (1)
        assertEquals("Expected three validation messages (OneOf + two child errors)",
                3, result.size());

        // Verify that the collector cleared evaluated items/properties
        verify(evaluatedItems).clear();
        verify(evaluatedProperties).clear();
    }

    /**
     * When MORE THAN ONE subschema validates successfully the validator must
     * report an error (or throw if fail‑fast is enabled).  This test checks the
     * non‑fail‑fast path.
     */
    @Test
    public void testValidateMultipleSchemasMatch() {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        // both schemas succeed (empty error set)
        when(schema1.validate(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        when(schema2.validate(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());

        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());
        schemaArray.add(mapper.createObjectNode());

        OneOfValidator validator = createValidator(schemaArray, schema1, schema2);

        // Ensure failFast is FALSE (default) – we will also test the true case later
        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");

        // Because two schemas matched, we must get at least the OneOf error
        assertFalse("Result should contain errors when more than one schema matches", result.isEmpty());

        // The child error set is empty, so size should be exactly 1 (the OneOf error)
        assertEquals("Only the OneOf error should be present", 1, result.size());

        // The validator must have cleared the evaluated collections
        verify(evaluatedItems).clear();
        verify(evaluatedProperties).clear();
    }

    /**
     * Same scenario as {@link #testValidateMultipleSchemasMatch()} but with
     * {@code failFast} enabled – the validator must throw {@link JsonSchemaException}
     * immediately.
     */
    @Test(expected = JsonSchemaException.class)
    public void testValidateMultipleSchemasMatch_failFast() throws Exception {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        when(schema1.validate(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        when(schema2.validate(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());

        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());
        schemaArray.add(mapper.createObjectNode());

        OneOfValidator validator = createValidator(schemaArray, schema1, schema2);

        // Turn on the protected failFast flag via reflection
        Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        // This call must throw JsonSchemaException
        validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");
    }

    /**
     * Edge case: the validator is constructed with an empty schema array.
     * In this situation the validator should behave as if zero subschemas
     * matched – i.e. produce a OneOf error with count “0”.
     */
    @Test
    public void testValidateWithEmptySchemaArray() {
        ArrayNode emptyArray = mapper.createArrayNode(); // size == 0

        // No subschema mocks are needed because the loop will never be entered
        OneOfValidator validator = createValidator(emptyArray);

        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");

        assertFalse("Result must contain an error when there are no subschemas", result.isEmpty());
        // The only error should be the OneOf error (count = 0)
        assertEquals(1, result.size());

        // Ensure the collector cleanup happened
        verify(evaluatedItems).clear();
        verify(evaluatedProperties).clear();
    }

    /* --------------------------------------------------------------------- *
     *  WALK METHOD TESTS
     * --------------------------------------------------------------------- */

    /**
     * When {@code shouldValidateSchema} is true the {@code walk} method must
     * delegate to {@code validate} and return its result.
     */
    @Test
    public void testWalkShouldValidate() {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.validate(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());

        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());

        OneOfValidator validator = createValidator(schemaArray, schema);

        Set<ValidationMessage> walkResult = validator.walk(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$", true);

        // With a single matching subschema we expect an empty set
        assertTrue(walkResult.isEmpty());

        // verify that validate() was indeed called (indirectly via walk)
        verify(schema).validate(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString());
    }

    /**
     * When {@code shouldValidateSchema} is false the validator must invoke
     * {@code walk} on each subschema but must not aggregate any errors.
     */
    @Test
    public void testWalkShouldNotValidate() {
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);

        // walk on subschemas returns a non‑empty set – the validator itself should
        // return an empty set because it does not collect those errors.
        when(schemaA.walk(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString(), anyBoolean()))
                .thenReturn(Collections.singleton(
                        ValidationMessage.builder().type("a").path("$").message("a").build()));
        when(schemaB.walk(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString(), anyBoolean()))
                .thenReturn(Collections.singleton(
                        ValidationMessage.builder().type("b").path("$").message("b").build()));

        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());
        schemaArray.add(mapper.createObjectNode());

        OneOfValidator validator = createValidator(schemaArray, schemaA, schemaB);

        Set<ValidationMessage> walkResult = validator.walk(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$", false);

        // The validator itself does not return any messages in this mode
        assertTrue("walk should return an empty set when shouldValidateSchema is false",
                walkResult.isEmpty());

        // Verify that each subschema's walk method was invoked exactly once
        verify(schemaA).walk(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString(), eq(false));
        verify(schemaB).walk(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString(), eq(false));
    }

    /* --------------------------------------------------------------------- *
     *  PRELOAD METHOD TEST
     * --------------------------------------------------------------------- */

    /**
     * {@code preloadJsonSchema()} must call {@code initializeValidators()} on
     * every subschema.
     */
    @Test
    public void testPreloadJsonSchemaCallsInitializeValidators() {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());
        schemaArray.add(mapper.createObjectNode());

        OneOfValidator validator = createValidator(schemaArray, schema1, schema2);

        validator.preloadJsonSchema();

        verify(schema1).initializeValidators();
        verify(schema2).initializeValidators();
    }

    /* --------------------------------------------------------------------- *
     *  EDGE CASES FOR WALK ENABLED / VALIDATION ENABLED FLAGS
     * --------------------------------------------------------------------- */

    /**
     * When the validator state signals that walk‑mode is enabled, the validator
     * must call {@code schema.walk(...)} instead of {@code schema.validate(...)}.
     */
    @Test
    public void testValidateWhenWalkEnabled() {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.walk(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        // Enable walk mode in the state
        when(validatorState.isWalkEnabled()).thenReturn(true);
        when(validatorState.isValidationEnabled()).thenReturn(true);

        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());

        OneOfValidator validator = createValidator(schemaArray, schema);

        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");

        // No errors because the subschema walked successfully
        assertTrue(result.isEmpty());

        // Verify that walk was invoked (not validate)
        verify(schema).walk(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString(), eq(true));
        verify(schema, never()).validate(any(), any(), any(), anyString());
    }

    /**
     * When {@code state.isWalkEnabled()} is true but the subschema returns
     * errors, those errors must be propagated as child errors.
     */
    @Test
    public void testValidateWhenWalkEnabledAndSchemaFails() {
        JsonSchema schema = mock(JsonSchema.class);
        Set<ValidationMessage> childErr = Collections.singleton(
                ValidationMessage.builder().type("c").path("$").message("c error").build()
        );
        when(schema.walk(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString(), anyBoolean()))
                .thenReturn(childErr);

        when(validatorState.isWalkEnabled()).thenReturn(true);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        // hasMatchedNode is irrelevant because there are errors – the validator
        // will not count this as a matching schema.

        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());

        OneOfValidator validator = createValidator(schemaArray, schema);

        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");

        // Since the only subschema failed, numberOfValidSchema == 0 → OneOf error + child error
        assertEquals(2, result.size());
        assertTrue(result.containsAll(childErr));

        verify(schema).walk(executionContext, any(JsonNode.class),
                any(JsonNode.class), anyString(), eq(true));
    }
}
