###Test START##
```java
/**
 * JUnit4 tests for {@link com.networknt.schema.OneOfValidator}.
 *
 * These tests cover the main validation logic, the walk delegation,
 * and the preloadJsonSchema method.  All external collaborators are
 * mocked with Mockito so the tests focus purely on the behaviour of
 * OneOfValidator.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    /** Mocks used by every test */
    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private Scope grandParentScope;
    @Mock private Scope childScope;
    @Mock private Scope parentScope;
    @Mock private ValidatorState validatorState;
    @Mock private JsonNode schemaArrayNode;   // the "oneOf" array node
    @Mock private JsonNode rootNode;
    @Mock private JsonNode instanceNode;      // node being validated
    @Mock private JsonSchema parentSchema;    // not used directly

    /** Two child schemas that will be returned by ValidationContext#newSchema */
    @Mock private JsonSchema childSchema0;
    @Mock private JsonSchema childSchema1;

    /** The validator under test */
    private OneOfValidator validator;

    /** Path used when constructing the validator */
    private static final String SCHEMA_PATH = "#/properties/foo/oneOf";

    @Before
    public void setUp() throws Exception {
        // ----- prepare the "oneOf" array node -----
        when(schemaArrayNode.size()).thenReturn(2);
        when(schemaArrayNode.get(0)).thenReturn(mock(JsonNode.class));
        when(schemaArrayNode.get(1)).thenReturn(mock(JsonNode.class));

        // ----- ValidationContext creates a JsonSchema for each child node -----
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/0"), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema0);
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/1"), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema1);

        // ----- ExecutionContext provides the CollectorContext -----
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // ----- CollectorContext dynamic scope handling -----
        when(collectorContext.enterDynamicScope()).thenReturn(grandParentScope, parentScope, childScope);
        when(collectorContext.exitDynamicScope()).thenReturn(childScope, parentScope, grandParentScope);

        // ----- The shared ValidatorState object -----
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // By default we are not walking, but validation is enabled.
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        // Assume a match has been found unless a test overrides this.
        when(validatorState.hasMatchedNode()).thenReturn(true);

        // ----- The evaluated collections – we only need to verify clear() is called -----
        when(collectorContext.getEvaluatedItems()).thenReturn(mock(Collection.class));
        when(collectorContext.getEvaluatedProperties()).thenReturn(mock(Collection.class));

        // ----- finally create the validator instance -----
        validator = new OneOfValidator(SCHEMA_PATH, schemaArrayNode, parentSchema, validationContext);
    }

    /**
     * Helper that builds a ValidationMessage set containing a single dummy message.
     */
    private Set<ValidationMessage> dummyMessageSet(String msg) {
        ValidationMessage vm = ValidationMessage.builder()
                .code("dummy")
                .path("#")
                .message(msg)
                .build();
        return Collections.singleton(vm);
    }

    /** --------------------------------------------------------------
     *  1. Exactly one child schema validates successfully.
     *  -------------------------------------------------------------- */
    @Test
    public void testValidate_ExactlyOneValidSchema() {
        // childSchema0 matches (empty error set), childSchema1 fails
        when(childSchema0.validate(executionContext, instanceNode, rootNode, "#"))
                .thenReturn(Collections.emptySet());
        when(childSchema1.validate(executionContext, instanceNode, rootNode, "#"))
                .thenReturn(dummyMessageSet("error from schema 1"));

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "#");

        // No errors should be reported because exactly one schema matched.
        assertTrue("Result should be empty when one schema matches", result.isEmpty());

        // Verify that the validator marked the state as a complex validator and later reset it.
        InOrder inOrder = inOrder(validatorState);
        inOrder.verify(validatorState).setComplexValidator(true);
        inOrder.verify(validatorState).setMatchedNode(true); // after successful validation
        // resetValidatorState is private – we verify its effects via subsequent calls:
        // after validation the state should be non‑complex and matched.
        verify(validatorState).setComplexValidator(false);
        verify(validatorState, atLeastOnce()).setMatchedNode(true);

        // Because there were no errors, the grand‑parent scope should be merged.
        verify(grandParentScope).mergeWith(grandParentScope);
    }

    /** --------------------------------------------------------------
     *  2. No child schema validates – expect a OneOf error plus child errors.
     *  -------------------------------------------------------------- */
    @Test
    public void testValidate_NoValidSchema() {
        // Both schemas return errors.
        Set<ValidationMessage> err0 = dummyMessageSet("schema0 error");
        Set<ValidationMessage> err1 = dummyMessageSet("schema1 error");
        when(childSchema0.validate(executionContext, instanceNode, rootNode, "#")).thenReturn(err0);
        when(childSchema1.validate(executionContext, instanceNode, rootNode, "#")).thenReturn(err1);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "#");

        // Result must contain the OneOf message and both child errors.
        assertEquals("Result should contain 3 messages (OneOf + 2 child errors)", 3, result.size());

        // The OneOf message contains the number of valid schemas ("0").
        Optional<ValidationMessage> oneOfMessage = result.stream()
                .filter(m -> m.getCode().equals(ValidatorTypeCode.ONE_OF.getErrorCodeKey()))
                .findFirst();
        assertTrue("OneOf validation message must be present", oneOfMessage.isPresent());
        assertTrue("OneOf message must contain the number of valid schemas",
                oneOfMessage.get().getMessage().contains("0"));

        // Verify that evaluated collections are cleared.
        verify(collectorContext.getEvaluatedItems()).clear();
        verify(collectorContext.getEvaluatedProperties()).clear();

        // No merge should happen because there were errors.
        verify(grandParentScope, never()).mergeWith(any(Scope.class));
    }

    /** --------------------------------------------------------------
     *  3. More than one schema matches – short‑circuit after the second match.
     *  -------------------------------------------------------------- */
    @Test
    public void testValidate_MultipleValidSchemas() {
        // Both schemas succeed (empty error sets)
        when(childSchema0.validate(executionContext, instanceNode, rootNode, "#"))
                .thenReturn(Collections.emptySet());
        when(childSchema1.validate(executionContext, instanceNode, rootNode, "#"))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "#");

        // OneOne error message + no child errors (both matched)
        assertEquals("Only the OneOf error should be present", 1, result.size());
        ValidationMessage vm = result.iterator().next();
        assertEquals("Error code must match ONE_OF", ValidatorTypeCode.ONE_OF.getErrorCodeKey(), vm.getCode());
        assertTrue("Message must contain the number of valid schemas (2)", vm.getMessage().contains("2"));
    }

    /** --------------------------------------------------------------
     *  4. failFast == true – a JsonSchemaException should be thrown.
     *  -------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void testValidate_FailFastThrowsException() throws Exception {
        // Force failFast via reflection
        java.lang.reflect.Field ff = OneOfValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        // No schema matches – this will trigger the OneOf error.
        when(childSchema0.validate(executionContext, instanceNode, rootNode, "#"))
                .thenReturn(dummyMessageSet("error0"));
        when(childSchema1.validate(executionContext, instanceNode, rootNode, "#"))
                .thenReturn(dummyMessageSet("error1"));

        // Expect JsonSchemaException
        validator.validate(executionContext, instanceNode, rootNode, "#");
    }

    /** --------------------------------------------------------------
     *  5. walk(..., shouldValidateSchema = true) delegates to validate().
     *  -------------------------------------------------------------- */
    @Test
    public void testWalk_ShouldValidateSchemaDelegatesToValidate() {
        // Spy on the validator to intercept the validate call.
        OneOfValidator spyValidator = Mockito.spy(validator);

        // Make validate return a known set.
        Set<ValidationMessage> fakeResult = dummyMessageSet("walk‑delegated");
        doReturn(fakeResult).when(spyValidator).validate(executionContext, instanceNode, rootNode, "#");

        Set<ValidationMessage> result = spyValidator.walk(executionContext, instanceNode, rootNode, "#", true);

        // Verify delegation and result.
        verify(spyValidator, times(1)).validate(executionContext, instanceNode, rootNode, "#");
        assertEquals("walk should return the same set as validate", fakeResult, result);
    }

    /** --------------------------------------------------------------
     *  6. walk(..., shouldValidateSchema = false) invokes each child schema's walk().
     *  -------------------------------------------------------------- */
    @Test
    public void testWalk_ShouldNotValidateSchemaCallsChildWalks() {
        // child walk should be called with shouldValidateSchema = false
        when(childSchema0.walk(executionContext, instanceNode, rootNode, "#", false)).thenReturn(Collections.emptySet());
        when(childSchema1.walk(executionContext, instanceNode, rootNode, "#", false)).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, "#", false);

        // No messages are expected because child walks return empty sets.
        assertTrue("Result should be empty when child walks return no messages", result.isEmpty());

        // Verify that each child's walk method was invoked.
        verify(childSchema0, times(1)).walk(executionContext, instanceNode, rootNode, "#", false);
        verify(childSchema1, times(1)).walk(executionContext, instanceNode, rootNode, "#", false);
    }

    /** --------------------------------------------------------------
     *  7. preloadJsonSchema() should call initializeValidators() on each child.
     *  -------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemaCallsInitializeValidators() {
        validator.preloadJsonSchema();

        verify(childSchema0, times(1)).initializeValidators();
        verify(childSchema1, times(1)).initializeValidators();
    }
}
```
###Test END##