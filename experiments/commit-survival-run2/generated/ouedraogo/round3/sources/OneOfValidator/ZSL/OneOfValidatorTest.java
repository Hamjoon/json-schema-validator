/****************************************************************************************
 *  OneOfValidatorTest.java
 *
 *  Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.OneOfValidator}.
 *
 *  The tests use Mockito to mock the complex collaborators required by the validator:
 *   – {@link com.networknt.schema.ValidationContext}
 *   – {@link com.networknt.schema.ExecutionContext}
 *   – {@link com.networknt.schema.CollectorContext}
 *   – {@link com.networknt.schema.ValidatorState}
 *   – {@link com.networknt.schema.JsonSchema}
 *
 *  The test cases cover:
 *   1. Exactly one child schema matches (no errors).
 *   2. No child schema matches (error aggregation).
 *   3. More than one child schema matches (failure because “one‑of” is violated).
 *   4. Fail‑fast mode – a {@link com.networknt.schema.JsonSchemaException} is thrown.
 *   5. {@code walk(...)} when validation is requested.
 *   6. {@code walk(...)} when validation is disabled (delegates to child schemas).
 *   7. {@code preloadJsonSchema()} – ensures each child schema is initialized.
 *
 *  The file is deliberately self‑contained; only JUnit‑4 and Mockito are required.
 ****************************************************************************************/

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

@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    private static final String SCHEMA_PATH = "#/properties/test";

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private ValidatorState validatorState;

    @Mock
    private CollectorContext.Scope grandParentScope;

    @Mock
    private CollectorContext.Scope parentScope;

    @Mock
    private CollectorContext.Scope childScope;

    @Mock
    private JsonSchema parentSchema; // passed to the validator constructor – not used directly

    private ObjectMapper mapper = new ObjectMapper();

    private JsonNode rootNode = mapper.createObjectNode(); // dummy root node
    private JsonNode node = mapper.createObjectNode();     // dummy instance node

    @Before
    public void setUp() {
        // ----- CollectorContext behaviour -------------------------------------------------
        // enterDynamicScope() is called twice (grand‑parent and per‑schema parent scopes)
        when(collectorContext.enterDynamicScope())
                .thenReturn(grandParentScope)   // first call – outermost
                .thenReturn(parentScope)        // second call – per‑schema parent
                .thenReturn(parentScope)        // subsequent calls – reuse same mock
                .thenReturn(parentScope);

        // exitDynamicScope() returns a child scope that will be merged into the parent scope
        when(collectorContext.exitDynamicScope()).thenReturn(childScope);

        // get() must return the validator state when queried with the special key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // evaluated items / properties – we use mutable sets so we can verify they are cleared
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        // ----- ExecutionContext -----------------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
    }

    /**
     * Helper that builds a {@link OneOfValidator} with an {@code ArrayNode}
     * containing {@code schemaCount} child schema nodes.
     *
     * The {@link ValidationContext#newSchema(String, JsonNode, JsonSchema)} method
     * is stubbed to return the supplied {@code schemaMocks} in order.
     */
    private OneOfValidator buildValidator(int schemaCount,
                                           List<JsonSchema> schemaMocks) throws Exception {

        // Create an array node with the requested number of empty child objects
        ArrayNode arrayNode = mapper.createArrayNode();
        for (int i = 0; i < schemaCount; i++) {
            arrayNode.addObject(); // each child is an empty object node
        }

        // Stub ValidationContext#newSchema to return the mocks in order
        OngoingStubbing<JsonSchema> stub = when(validationContext.newSchema(anyString(),
                any(JsonNode.class), eq(parentSchema)));
        for (JsonSchema mock : schemaMocks) {
            stub = stub.thenReturn(mock);
        }

        // Build the validator
        OneOfValidator validator = new OneOfValidator(SCHEMA_PATH, arrayNode, parentSchema,
                validationContext);
        return validator;
    }

    /**
     * Utility to flip the private {@code failFast} flag of {@link BaseJsonValidator}
     * via reflection.
     */
    private void setFailFast(OneOfValidator validator, boolean failFast) throws Exception {
        Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, failFast);
    }

    /* -------------------------------------------------------------------------
     *  Test 1 – exactly one schema validates successfully.
     * ----------------------------------------------------------------------- */
    @Test
    public void testValidateExactlyOneValidSchema() throws Exception {
        // Mocks for the two child schemas
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        // schema0 is valid (returns empty set), schema1 is invalid (returns a message)
        when(schema0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        ValidationMessage err1 = mock(ValidationMessage.class);
        when(schema1.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(err1));

        // State configuration
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true); // child matched

        OneOfValidator validator = buildValidator(2, Arrays.asList(schema0, schema1));

        Set<ValidationMessage> errors = validator.validate(executionContext, node, rootNode, "$");

        // Exactly one schema matched → no OneOf error and no child errors should be returned
        assertTrue("Expected no validation errors", errors.isEmpty());

        // Verify that the validator marked the node as matched
        verify(validatorState).setMatchedNode(true);
    }

    /* -------------------------------------------------------------------------
     *  Test 2 – no schema validates (zero matches). All child errors must be
     *           aggregated together with the OneOf error.
     * ----------------------------------------------------------------------- */
    @Test
    public void testValidateNoValidSchema() throws Exception {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        ValidationMessage err0 = mock(ValidationMessage.class);
        ValidationMessage err1 = mock(ValidationMessage.class);

        when(schema0.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(err0));
        when(schema1.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(err1));

        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        OneOfValidator validator = buildValidator(2, Arrays.asList(schema0, schema1));

        Set<ValidationMessage> errors = validator.validate(executionContext, node, rootNode, "$");

        // We must have at least three messages: the OneOf message + the two child messages
        assertEquals("Expected three validation messages (OneOf + 2 child errors)",
                3, errors.size());
        assertTrue("Child error 0 should be present", errors.contains(err0));
        assertTrue("Child error 1 should be present", errors.contains(err1));

        // The evaluated‑items / evaluated‑properties collections must be cleared because
        // the OneOf condition failed.
        verify(collectorContext.getEvaluatedItems()).clear();
        verify(collectorContext.getEvaluatedProperties()).clear();
    }

    /* -------------------------------------------------------------------------
     *  Test 3 – more than one schema validates (two matches). The validator must
     *           report a OneOf error and must NOT report any child errors.
     * ----------------------------------------------------------------------- */
    @Test
    public void testValidateMultipleValidSchemas() throws Exception {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        // Both schemas are valid (empty error sets)
        when(schema0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(schema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        OneOfValidator validator = buildValidator(2, Arrays.asList(schema0, schema1));

        Set<ValidationMessage> errors = validator.validate(executionContext, node, rootNode, "$");

        // Exactly one OneOf error should be present and no child errors (because child errors are empty)
        assertEquals("Expected a single OneOf validation error", 1, errors.size());

        // Verify that the evaluated collections were cleared (failure path)
        verify(collectorContext.getEvaluatedItems()).clear();
        verify(collectorContext.getEvaluatedProperties()).clear();
    }

    /* -------------------------------------------------------------------------
     *  Test 4 – fail‑fast mode should throw JsonSchemaException when the OneOf
     *           condition is not satisfied.
     * ----------------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void testValidateFailFastThrowsException() throws Exception {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        // Both schemas are invalid → zero valid schemas
        ValidationMessage err0 = mock(ValidationMessage.class);
        when(schema0.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(err0));
        when(schema1.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet()); // make one valid to test fail‑fast after detection

        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        OneOfValidator validator = buildValidator(2, Arrays.asList(schema0, schema1));
        setFailFast(validator, true); // enable fail‑fast

        // The call must throw JsonSchemaException
        validator.validate(executionContext, node, rootNode, "$");
    }

    /* -------------------------------------------------------------------------
     *  Test 5 – walk(..., shouldValidateSchema = true) must delegate to validate().
     * ----------------------------------------------------------------------- */
    @Test
    public void testWalkShouldValidateTrueDelegatesToValidate() throws Exception {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        OneOfValidator validator = buildValidator(1, Collections.singletonList(schema));

        // Spy on the validator to capture the call to validate()
        OneOfValidator spyValidator = spy(validator);
        Set<ValidationMessage> dummySet = Collections.singleton(mock(ValidationMessage.class));
        doReturn(dummySet).when(spyValidator).validate(any(), any(), any(), any());

        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, rootNode, "$", true);

        // The result must be exactly the dummy set returned from validate()
        assertSame("walk(true) should return the set produced by validate()", dummySet, result);
        verify(spyValidator, times(1)).validate(executionContext, node, rootNode, "$");
    }

    /* -------------------------------------------------------------------------
     *  Test 6 – walk(..., shouldValidateSchema = false) must invoke walk() on
     *           each child schema but must not collect any messages itself.
     * ----------------------------------------------------------------------- */
    @Test
    public void testWalkShouldValidateFalseInvokesChildWalk() throws Exception {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        // Child schemas' walk() method returns a set – we don't care about its content.
        when(schema0.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());
        when(schema1.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());

        when(validatorState.isWalkEnabled()).thenReturn(false); // not used for this path
        OneOfValidator validator = buildValidator(2, Arrays.asList(schema0, schema1));

        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode, "$", false);

        // walk(false) should return an empty set (the validator does not add messages itself)
        assertTrue("walk(false) must return an empty set", result.isEmpty());

        // Verify that each child schema's walk method was called exactly once
        verify(schema0, times(1)).walk(executionContext, node, rootNode, "$", false);
        verify(schema1, times(1)).walk(executionContext, node, rootNode, "$", false);
    }

    /* -------------------------------------------------------------------------
     *  Test 7 – preloadJsonSchema() must call initializeValidators() on each child.
     * ----------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemaInitializesAllChildren() throws Exception {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        OneOfValidator validator = buildValidator(3, Arrays.asList(schema0, schema1, schema2));

        validator.preloadJsonSchema();

        // Verify that each child schema received the initialize call
        verify(schema0, times(1)).initializeValidators();
        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
    }
}
