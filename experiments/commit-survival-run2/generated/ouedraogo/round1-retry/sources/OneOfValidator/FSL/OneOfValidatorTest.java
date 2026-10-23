package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link OneOfValidator}. The tests use Mockito to isolate the
 * validator from the rest of the library and focus on the control flow inside
 * {@code validate()}, {@code walk()} and {@code preloadJsonSchema()}.
 */
@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    // ----- Mocks required for the validator infrastructure -----------------
    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private Scope dynamicScope;
    @Mock private Scope parentScope;
    @Mock private Scope grandParentScope;
    @Mock private ValidatorState validatorState;
    @Mock private JsonNode schemaNode;          // the "oneOf" array node
    @Mock private JsonNode rootNode;
    @Mock private JsonNode instanceNode;       // the JSON document being validated

    // Two child schemas that will be added to the validator
    @Mock private JsonSchema childSchema1;
    @Mock private JsonSchema childSchema2;

    // A dummy ValidationMessage used for assertions
    @Mock private ValidationMessage dummyMessage;

    private OneOfValidator validator;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // ----- Common behaviour for the execution/collector infrastructure -----
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope).thenReturn(parentScope);
        when(collectorContext.exitDynamicScope()).thenReturn(parentScope).thenReturn(grandParentScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // The state initially reports that walk is disabled (the default path exercised by validate())
        when(validatorState.isWalkEnabled()).thenReturn(false);
        // When the validator asks for validationEnabled flag inside walk() we return true
        when(validatorState.isValidationEnabled()).thenReturn(true);
        // The state starts with a matched node; the validator will flip it as needed
        when(validatorState.hasMatchedNode()).thenReturn(true);

        // ----- Build a JSON array node that contains two elements (child schemas) -----
        // The size() method of the array node must return 2 and get(i) must return a dummy node.
        when(schemaNode.size()).thenReturn(2);
        when(schemaNode.get(0)).thenReturn(mock(JsonNode.class));
        when(schemaNode.get(1)).thenReturn(mock(JsonNode.class));

        // ----- ValidationContext creates new JsonSchema objects for each child -----
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(childSchema1)   // first child
                .thenReturn(childSchema2);  // second child

        // ----- Create the validator under test ---------------------------------
        validator = new OneOfValidator("schemaPath", schemaNode, mock(JsonSchema.class), validationContext);

        // ----- Stub the protected buildValidationMessage() method so we can
        //       recognise the error message that the validator creates.
        OneOfValidator spyValidator = spy(validator);
        doReturn(dummyMessage).when(spyValidator).buildValidationMessage(anyString(), anyString());
        validator = spyValidator;
    }

    /**
     * Helper that builds a {@code Set<ValidationMessage>} containing a single dummy
     * message. Used to simulate a failing child schema.
     */
    private Set<ValidationMessage> oneErrorSet() {
        return Collections.singleton(dummyMessage);
    }

    // -------------------------------------------------------------------------
    // 1. Exactly one child schema validates successfully → no errors returned
    // -------------------------------------------------------------------------
    @Test
    public void testValidateExactlyOneSchemaMatches() {
        // child 1 fails, child 2 succeeds
        when(childSchema1.validate(executionContext, instanceNode, rootNode, "at"))
                .thenReturn(oneErrorSet());
        when(childSchema2.validate(executionContext, instanceNode, rootNode, "at"))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "at");

        // No validation messages should be produced
        assertTrue("Result must be empty when exactly one schema matches", result.isEmpty());

        // State must be reset (complex flag cleared, matchedNode true)
        verify(validatorState).setComplexValidator(false);
        verify(validatorState, atLeastOnce()).setMatchedNode(true);

        // The grand‑parent scope should be merged because there are no errors
        verify(grandParentScope).mergeWith(grandParentScope);
    }

    // -------------------------------------------------------------------------
    // 2. No child schema matches → OneOf error + aggregated child errors
    // -------------------------------------------------------------------------
    @Test
    public void testValidateNoSchemaMatches() {
        // Both children return an error set
        when(childSchema1.validate(executionContext, instanceNode, rootNode, "at"))
                .thenReturn(oneErrorSet());
        when(childSchema2.validate(executionContext, instanceNode, rootNode, "at"))
                .thenReturn(oneErrorSet());

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "at");

        // The result must contain the dummyMessage from the validator (OneOf error)
        // plus the two child errors (which are the same dummyMessage in this test)
        assertEquals("Exactly three messages expected (OneOf + two child errors)", 3, result.size());
        assertTrue(result.contains(dummyMessage));

        // The collector context must have cleared its evaluated items/properties
        verify(collectorContext).getEvaluatedItems();
        verify(collectorContext).getEvaluatedProperties();
        // (clear() is a void method – we verify that the clear calls were made)
        verify(collectorContext.getEvaluatedItems(), times(1)).clear();
        verify(collectorContext.getEvaluatedProperties(), times(1)).clear();

        // State must be reset
        verify(validatorState).setComplexValidator(false);
        verify(validatorState, atLeastOnce()).setMatchedNode(true);
    }

    // -------------------------------------------------------------------------
    // 3. More than one child schema matches → OneOf error (failFast = false)
    // -------------------------------------------------------------------------
    @Test
    public void testValidateMultipleSchemasMatch() {
        // Both children succeed
        when(childSchema1.validate(executionContext, instanceNode, rootNode, "at"))
                .thenReturn(Collections.emptySet());
        when(childSchema2.validate(executionContext, instanceNode, rootNode, "at"))
                .thenReturn(Collections.emptySet());

        // Force the validator to think that both schemas matched
        // The loop will break after the second match, leaving numberOfValidSchema == 2
        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "at");

        // Expect a OneOf error (dummyMessage) plus any child errors (none in this scenario)
        assertEquals("Only the OneOf error should be present", 1, result.size());
        assertTrue(result.contains(dummyMessage));
    }

    // -------------------------------------------------------------------------
    // 4. failFast = true → JsonSchemaException is thrown when validation fails
    // -------------------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void testValidateFailFastThrowsException() throws Exception {
        // Make both schemas fail
        when(childSchema1.validate(executionContext, instanceNode, rootNode, "at"))
                .thenReturn(oneErrorSet());
        when(childSchema2.validate(executionContext, instanceNode, rootNode, "at"))
                .thenReturn(oneErrorSet());

        // Enable failFast via reflection (BaseJsonValidator has a protected field)
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.set(validator, true);

        // The call must throw JsonSchemaException
        validator.validate(executionContext, instanceNode, rootNode, "at");
    }

    // -------------------------------------------------------------------------
    // 5. walk() with shouldValidateSchema = true delegates to validate()
    // -------------------------------------------------------------------------
    @Test
    public void testWalkShouldValidateTrue() {
        // Prepare a non‑empty set that validate() will return
        Set<ValidationMessage> validationResult = new HashSet<>();
        validationResult.add(dummyMessage);
        doReturn(validationResult).when(validator).validate(executionContext, instanceNode, rootNode, "at");

        Set<ValidationMessage> walkResult = validator.walk(executionContext, instanceNode, rootNode, "at", true);

        assertEquals("walk should return exactly what validate returns", validationResult, walkResult);
        verify(validator).validate(executionContext, instanceNode, rootNode, "at");
    }

    // -------------------------------------------------------------------------
    // 6. walk() with shouldValidateSchema = false invokes each child schema's walk()
    // -------------------------------------------------------------------------
    @Test
    public void testWalkShouldValidateFalse() {
        // Ensure validate() is NOT called
        doReturn(Collections.emptySet()).when(validator).validate(any(), any(), any(), anyString());

        // Call walk with shouldValidateSchema = false
        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, "at", false);

        // walk() should return an empty set (it does not aggregate child walk results)
        assertTrue(result.isEmpty());

        // Verify that each child schema's walk() method was invoked exactly once
        verify(childSchema1).walk(executionContext, instanceNode, rootNode, "at", false);
        verify(childSchema2).walk(executionContext, instanceNode, rootNode, "at", false);
        // Ensure that validate() was never called
        verify(validator, never()).validate(any(), any(), any(), anyString());
    }

    // -------------------------------------------------------------------------
    // 7. preloadJsonSchema() must initialise validators of all child schemas
    // -------------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchemaInitialisesChildren() {
        validator.preloadJsonSchema();

        verify(childSchema1).initializeValidators();
        verify(childSchema2).initializeValidators();
    }
}
