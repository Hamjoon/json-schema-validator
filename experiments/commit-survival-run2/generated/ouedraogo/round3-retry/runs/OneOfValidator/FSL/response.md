###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.CollectorContext.Scope;

/**
 * Unit tests for {@link OneOfValidator}.
 *
 * The tests use Mockito to stub the collaborators required by {@code OneOfValidator}
 * ( {@link ValidationContext}, {@link ExecutionContext}, {@link CollectorContext},
 * {@link JsonSchema}, {@link ValidatorState} etc.).  The goal is to verify the
 * contract of the public methods {@code validate(...)} and {@code walk(...)} under
 * different combinations of child‑schema validation results.
 */
public class OneOfValidatorTest {

    /** Mocks used by every test */
    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private Scope dummyScope;
    @Mock private ValidatorState validatorState;
    @Mock private JsonSchema parentSchema;

    /** Two child schemas that will be returned by the {@code ValidationContext} */
    @Mock private JsonSchema childSchema1;
    @Mock private JsonSchema childSchema2;

    /** A dummy node representing the instance being validated */
    @Mock private JsonNode instanceNode;
    @Mock private JsonNode rootNode;

    /** The validator under test */
    private OneOfValidator validator;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        /* -------------------------------------------------
         * Common stubbing for the collector context
         * ------------------------------------------------- */
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // the evaluated‑item/property sets are spied so we can verify that they are cleared
        Set<String> evaluatedItems = Mockito.spy(new HashSet<String>());
        Set<String> evaluatedProperties = Mockito.spy(new HashSet<String>());
        when(collectorContext.getEvaluatedItems()).thenReturn(evaluatedItems);
        when(collectorContext.getEvaluatedProperties()).thenReturn(evaluatedProperties);

        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        /* -------------------------------------------------
         * Stub the ValidationContext to create the two child
         * schemas that OneOfValidator will hold.
         * ------------------------------------------------- */
        // The constructor of OneOfValidator iterates over the elements of the
        // supplied schema node and calls validationContext.newSchema(...) for each.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(childSchema1)   // first child
                .thenReturn(childSchema2); // second child

        /* -------------------------------------------------
         * Build a minimal “oneOf” schema node (an empty array with
         * two objects) – the actual content of the child nodes is not
         * important because the child schemas are mocked.
         * ------------------------------------------------- */
        ArrayNode oneOfNode = JsonNodeFactory.instance.arrayNode();
        oneOfNode.addObject(); // element 0
        oneOfNode.addObject(); // element 1

        // Instantiate the validator (the constructor will fill the private “schemas” list)
        validator = new OneOfValidator("testPath", oneOfNode, parentSchema, validationContext);
    }

    /**
     * Helper that creates a {@code Set<ValidationMessage>} containing a single
     * dummy message.  The concrete content of the message is irrelevant for
     * the logic we test – we only need a non‑empty set to represent a failure.
     */
    private Set<ValidationMessage> errorSet() {
        ValidationMessage msg = mock(ValidationMessage.class);
        return Collections.singleton(msg);
    }

    /**
     * Scenario: exactly one child schema validates successfully.
     * Expected result: the validator returns an empty error set and marks the
     * node as matched.
     */
    @Test
    public void testValidate_ExactlyOneSchemaValid() {
        // child 0 succeeds, child 1 fails
        when(childSchema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(childSchema2.validate(any(), any(), any(), any())).thenReturn(errorSet());

        // default state values
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isMatchedNode()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "#");

        assertTrue("There should be no validation errors when exactly one schema matches", result.isEmpty());
        // The validator must have set matchedNode to true on the state object.
        verify(validatorState).setMatchedNode(true);
    }

    /**
     * Scenario: none of the child schemas validate (all return errors).
     * Expected result: a “oneOf” error is produced and the child errors are
     * included in the returned set.
     */
    @Test
    public void testValidate_NoSchemaValid() {
        when(childSchema1.validate(any(), any(), any(), any())).thenReturn(errorSet());
        when(childSchema2.validate(any(), any(), any(), any())).thenReturn(errorSet());

        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isMatchedNode()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "#");

        // At least one message (the “oneOf” error) must be present
        assertFalse("Validation should fail when no schema matches", result.isEmpty());
        // The result must contain the two child errors plus the oneOf error
        // (the exact number is not important – we only assert that it is > 1)
        assertTrue("Result should contain more than one message (oneOf + child errors)",
                result.size() > 1);

        // Verify that the evaluated‑item/property sets have been cleared
        verify(collectorContext.getEvaluatedItems()).clear();
        verify(collectorContext.getEvaluatedProperties()).clear();
    }

    /**
     * Scenario: more than one child schema validates successfully.
     * Expected result: the validator reports a “oneOf” error (because the
     * number of valid schemas is > 1) and does not report child errors
     * from the schemas that succeeded.
     */
    @Test
    public void testValidate_MultipleSchemasValid() {
        // both children succeed
        when(childSchema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(childSchema2.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isMatchedNode()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "#");

        // The validator must generate a single “oneOf” error (no child errors)
        assertEquals("When more than one schema matches, only the oneOf error should be present",
                1, result.size());

        // Verify that the evaluated‑item/property sets have been cleared
        verify(collectorContext.getEvaluatedItems()).clear();
        verify(collectorContext.getEvaluatedProperties()).clear();
    }

    /**
     * Walk with {@code shouldValidateSchema = true} must delegate to {@code validate(...)}.
     * The test uses a spy on the validator to confirm that the delegation occurs.
     */
    @Test
    public void testWalk_DelegatesToValidateWhenShouldValidateTrue() {
        // Spy the validator so we can stub the validate method
        OneOfValidator spyValidator = Mockito.spy(validator);

        Set<ValidationMessage> fakeResult = Collections.singleton(mock(ValidationMessage.class));
        doReturn(fakeResult).when(spyValidator).validate(any(), any(), any(), any());

        Set<ValidationMessage> walkResult = spyValidator.walk(executionContext, instanceNode, rootNode, "#", true);

        assertEquals("walk should return exactly the set produced by validate()", fakeResult, walkResult);
        verify(spyValidator, times(1)).validate(executionContext, instanceNode, rootNode, "#");
    }

    /**
     * Walk with {@code shouldValidateSchema = false} must invoke {@code walk(...)} on each
     * child schema but must not collect any {@link ValidationMessage}s.
     */
    @Test
    public void testWalk_InvokesChildWalkWhenShouldValidateFalse() {
        // child schemas should receive a walk call
        when(childSchema1.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());
        when(childSchema2.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, "#", false);

        // walk must return an empty set because shouldValidateSchema is false
        assertTrue("walk should return an empty set when shouldValidateSchema is false", result.isEmpty());

        // verify that each child schema's walk method was called exactly once
        verify(childSchema1, times(1)).walk(executionContext, instanceNode, rootNode, "#", false);
        verify(childSchema2, times(1)).walk(executionContext, instanceNode, rootNode, "#", false);
    }

    /**
     * Verify that {@code preloadJsonSchema()} triggers the initialization of each
     * child schema.
     */
    @Test
    public void testPreloadJsonSchema_InitializesAllChildren() {
        // Child schemas are mocked; we only need to verify that initializeValidators() is invoked.
        validator.preloadJsonSchema();

        verify(childSchema1, times(1)).initializeValidators();
        verify(childSchema2, times(1)).initializeValidators();
    }
}
###Test END##