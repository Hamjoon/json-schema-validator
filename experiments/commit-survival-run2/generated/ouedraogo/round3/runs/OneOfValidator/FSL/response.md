###Test START##
```java
/**
 * Unit tests for {@link com.networknt.schema.OneOfValidator}.
 *
 * The tests use Mockito to isolate the validator from the rest of the
 * library and focus on the logic inside {@code OneOfValidator}.
 *
 * The test suite covers:
 * <ul>
 *   <li>Construction – that the validator creates the expected number of child schemas.</li>
 *   <li>Successful validation when exactly one subschema matches.</li>
 *   <li>Failure when no subschema matches.</li>
 *   <li>Failure when more than one subschema matches (including fail‑fast mode).</li>
 *   <li>Behaviour when {@code ValidatorState.isWalkEnabled()} is {@code true}.</li>
 *   <li>The {@code walk} method delegating correctly to {@code validate} or to each child schema.</li>
 *   <li>The {@code preloadJsonSchema} method invoking {@code initializeValidators} on each child.</li>
 * </ul>
 *
 * The code is written for JUnit‑4 and Mockito‑2+.
 */

package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@link OneOfValidator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    // -----------------------------------------------------------------
    // Mocks shared by all test methods
    // -----------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private JsonNode schemaNode;          // the "oneOf" array node
    @Mock private JsonNode childNode0;
    @Mock private JsonNode childNode1;
    @Mock private JsonNode instanceNode;        // node being validated
    @Mock private JsonNode rootNode;           // root of the document
    @Mock private Scope grandParentScope;
    @Mock private Scope parentScope0;
    @Mock private Scope parentScope1;
    @Mock private Scope exitScope0;
    @Mock private Scope exitScope1;
    @Mock private Set<ValidationMessage> emptyMessageSet;
    @Mock private Set<ValidationMessage> child0Errors;
    @Mock private Set<ValidationMessage> child1Errors;

    // child schemas created by the validator
    @Mock private JsonSchema childSchema0;
    @Mock private JsonSchema childSchema1;

    // dummy evaluated collections – we only need to verify that {@code clear()} is called
    @Mock private Set<Object> evaluatedItems;
    @Mock private Set<Object> evaluatedProperties;

    private OneOfValidator validator;

    /**
     * Common test fixture.
     */
    @Before
    public void setUp() throws Exception {
        // -----------------------------------------------------------------
        // Build a fake "oneOf" array node containing two children.
        // -----------------------------------------------------------------
        when(schemaNode.size()).thenReturn(2);
        when(schemaNode.get(0)).thenReturn(childNode0);
        when(schemaNode.get(1)).thenReturn(childNode1);

        // -----------------------------------------------------------------
        // ValidationContext creates a new JsonSchema for each child node.
        // -----------------------------------------------------------------
        when(validationContext.newSchema(anyString(), eq(childNode0), any(JsonSchema.class)))
                .thenReturn(childSchema0);
        when(validationContext.newSchema(anyString(), eq(childNode1), any(JsonSchema.class)))
                .thenReturn(childSchema1);

        // -----------------------------------------------------------------
        // ExecutionContext → CollectorContext → dynamic scopes.
        // -----------------------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // enterDynamicScope() is called once for the grand‑parent and once per child.
        // We return distinct mock Scope objects so that the code can call mergeWith()
        // without NPEs.
        when(collectorContext.enterDynamicScope())
                .thenReturn(grandParentScope)   // first call – grand‑parent
                .thenReturn(parentScope0)      // second call – child #0
                .thenReturn(parentScope1);     // third call – child #1

        // exitDynamicScope() is called the same number of times.
        when(collectorContext.exitDynamicScope())
                .thenReturn(exitScope0)        // after child #0
                .thenReturn(exitScope1);       // after child #1

        // -----------------------------------------------------------------
        // Provide a dummy ValidatorState object.
        // -----------------------------------------------------------------
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // Default behaviour – walk disabled, validation enabled, node considered matched.
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        // The state mutators are void; we just allow them to be called.
        doNothing().when(validatorState).setComplexValidator(anyBoolean());
        doNothing().when(validatorState).setMatchedNode(anyBoolean());

        // -----------------------------------------------------------------
        // Collections that the validator clears on error.
        // -----------------------------------------------------------------
        when(collectorContext.getEvaluatedItems()).thenReturn(evaluatedItems);
        when(collectorContext.getEvaluatedProperties()).thenReturn(evaluatedProperties);

        // -----------------------------------------------------------------
        // Create the validator under test.
        // -----------------------------------------------------------------
        validator = new OneOfValidator("#/oneOf", schemaNode, null, validationContext);
        // The constructor adds the two child schemas – we keep a reference for later stubbing.
    }

    // -----------------------------------------------------------------
    // Helper to build a dummy ValidationMessage set.
    // -----------------------------------------------------------------
    private Set<ValidationMessage> messages(String... texts) {
        Set<ValidationMessage> set = new LinkedHashSet<>();
        for (String txt : texts) {
            ValidationMessage vm = mock(ValidationMessage.class);
            when(vm.getMessage()).thenReturn(txt);
            set.add(vm);
        }
        return set;
    }

    // -----------------------------------------------------------------
    // 1. Exactly one subschema matches – validation succeeds.
    // -----------------------------------------------------------------
    @Test
    public void testValidateExactlyOneValidSchema() {
        // child 0 validates successfully
        when(childSchema0.validate(executionContext, instanceNode, rootNode, "#/foo"))
                .thenReturn(Collections.emptySet());

        // child 1 returns a validation error
        Set<ValidationMessage> errorSet = messages("error from schema 1");
        when(childSchema1.validate(executionContext, instanceNode, rootNode, "#/foo"))
                .thenReturn(errorSet);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "#/foo");

        // No errors because exactly one child succeeded.
        assertTrue("Result should be empty when one schema matches", result.isEmpty());

        // Verify that the state was marked as a complex validator and then reset.
        InOrder inOrder = inOrder(validatorState);
        inOrder.verify(validatorState).setComplexValidator(true);
        inOrder.verify(validatorState).setComplexValidator(false);
        inOrder.verify(validatorState).setMatchedNode(true);
    }

    // -----------------------------------------------------------------
    // 2. No subschema matches – a OneOf error plus child errors are returned.
    // -----------------------------------------------------------------
    @Test
    public void testValidateNoValidSchema() {
        Set<ValidationMessage> errors0 = messages("schema0 error");
        Set<ValidationMessage> errors1 = messages("schema1 error");

        when(childSchema0.validate(executionContext, instanceNode, rootNode, "#/foo"))
                .thenReturn(errors0);
        when(childSchema1.validate(executionContext, instanceNode, rootNode, "#/foo"))
                .thenReturn(errors1);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "#/foo");

        // The result must contain the OneOf error (generated by the validator)
        // plus the two child errors.
        assertEquals("Expected three validation messages (OneOf + 2 child errors)",
                3, result.size());

        // Verify that the collector cleared its evaluated items/properties.
        verify(evaluatedItems).clear();
        verify(evaluatedProperties).clear();
    }

    // -----------------------------------------------------------------
    // 3. More than one subschema matches – error reported, fail‑fast mode throws.
    // -----------------------------------------------------------------
    @Test
    public void testValidateMultipleValidSchemas_FailFast() throws Exception {
        // Both children succeed.
        when(childSchema0.validate(executionContext, instanceNode, rootNode, "#/foo"))
                .thenReturn(Collections.emptySet());
        when(childSchema1.validate(executionContext, instanceNode, rootNode, "#/foo"))
                .thenReturn(Collections.emptySet());

        // Enable fail‑fast via reflection (the field lives in BaseJsonValidator).
        Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.set(validator, Boolean.TRUE);

        try {
            validator.validate(executionContext, instanceNode, rootNode, "#/foo");
            fail("Expected JsonSchemaException because more than one schema matched and failFast is true");
        } catch (JsonSchemaException ex) {
            // Expected – the exception message contains the number of valid schemas (2).
            assertTrue(ex.getMessage().contains("2"));
        }
    }

    // -----------------------------------------------------------------
    // 4. Walk enabled – validator should call `schema.walk` instead of `schema.validate`.
    // -----------------------------------------------------------------
    @Test
    public void testValidateWhenWalkEnabled() {
        // Switch the state so that walk is enabled but validation within walk is disabled.
        when(validatorState.isWalkEnabled()).thenReturn(true);
        when(validatorState.isValidationEnabled()).thenReturn(false);

        // Child 0 walks successfully (no errors)
        when(childSchema0.walk(executionContext, instanceNode, rootNode, "#/foo", false))
                .thenReturn(Collections.emptySet());

        // Child 1 walks and returns an error.
        Set<ValidationMessage> walkErrors = messages("walk error");
        when(childSchema1.walk(executionContext, instanceNode, rootNode, "#/foo", false))
                .thenReturn(walkErrors);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "#/foo");

        // No successful schema (child0 walked with no errors but hasMatchedNode is true,
        // however we still have a second schema with errors – the validator will
        // treat it as “multiple matches” and therefore report an error).
        // The exact number is not the focus; we only check that the correct API was used.
        verify(childSchema0, never()).validate(any(), any(), any(), anyString());
        verify(childSchema1, never()).validate(any(), any(), any(), anyString());

        verify(childSchema0).walk(executionContext, instanceNode, rootNode, "#/foo", false);
        verify(childSchema1).walk(executionContext, instanceNode, rootNode, "#/foo", false);
    }

    // -----------------------------------------------------------------
    // 5. walk(...) – delegating to validate when shouldValidateSchema == true.
    // -----------------------------------------------------------------
    @Test
    public void testWalkDelegatesToValidateWhenShouldValidateSchemaTrue() {
        // Spy on the validator so we can verify that validate() is called.
        OneOfValidator spyValidator = spy(validator);

        // The underlying validate call will return an empty set.
        doReturn(Collections.emptySet())
                .when(spyValidator)
                .validate(executionContext, instanceNode, rootNode, "#/foo");

        Set<ValidationMessage> result = spyValidator.walk(executionContext, instanceNode, rootNode, "#/foo", true);

        // The walk method should have delegated to validate.
        verify(spyValidator, times(1))
                .validate(executionContext, instanceNode, rootNode, "#/foo");

        // And it should return whatever validate returned (empty set).
        assertTrue(result.isEmpty());
    }

    // -----------------------------------------------------------------
    // 6. walk(...) – traversing children when shouldValidateSchema == false.
    // -----------------------------------------------------------------
    @Test
    public void testWalkCallsChildWalkWhenShouldValidateSchemaFalse() {
        // Child walk calls – we do not care about the returned set.
        when(childSchema0.walk(executionContext, instanceNode, rootNode, "#/foo", false))
                .thenReturn(Collections.emptySet());
        when(childSchema1.walk(executionContext, instanceNode, rootNode, "#/foo", false))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, "#/foo", false);

        // No validation messages should be produced.
        assertTrue(result.isEmpty());

        // Verify that each child schema's walk method was invoked.
        verify(childSchema0).walk(executionContext, instanceNode, rootNode, "#/foo", false);
        verify(childSchema1).walk(executionContext, instanceNode, rootNode, "#/foo", false);
    }

    // -----------------------------------------------------------------
    // 7. preloadJsonSchema – each child schema must be initialized.
    // -----------------------------------------------------------------
    @Test
    public void testPreloadJsonSchemaInitializesAllChildren() {
        validator.preloadJsonSchema();

        verify(childSchema0).initializeValidators();
        verify(childSchema1).initializeValidators();
    }

    // -----------------------------------------------------------------
    // 8. Constructor builds the expected number of child schemas.
    // -----------------------------------------------------------------
    @Test
    public void testConstructorCreatesCorrectNumberOfSchemas() throws Exception {
        // The validator's internal list is private; we use reflection to inspect it.
        java.lang.reflect.Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonSchema> schemaList = (List<JsonSchema>) f.get(validator);
        assertEquals("Validator should contain two child schemas", 2, schemaList.size());
        assertSame("First child schema should be the mock returned by ValidationContext",
                childSchema0, schemaList.get(0));
        assertSame("Second child schema should be the mock returned by ValidationContext",
                childSchema1, schemaList.get(1));
    }
}
```
###Test END##