###Test START##
```java
/**
 * JUnit 4 test suite for {@link com.networknt.schema.OneOfValidator}.
 *
 * The tests cover:
 *  <ul>
 *      <li>normal validation where exactly one schema matches</li>
 *      <li>no schema matches</li>
 *      <li>more than one schema matches (including fail‑fast mode)</li>
 *      <li>walk() behaviour with {@code shouldValidateSchema} true/false</li>
 *      <li>preloadJsonSchema() delegating to the child schemas</li>
 *  </ul>
 *
 *  The heavy‑weight collaborators ({@link com.networknt.schema.ExecutionContext},
 *  {@link com.networknt.schema.CollectorContext}, {@link com.networknt.schema.ValidatorState},
 *  {@link com.networknt.schema.JsonSchema}…) are mocked with Mockito.
 *
 *  The {@code OneOfValidator} instance is created with an empty schema node and the
 *  internal {@code schemas} list is populated via reflection – this avoids the need
 *  for a full {@link com.networknt.schema.ValidationContext} implementation.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    // -----------------------------------------------------------------------
    // Mocks for the execution environment
    // -----------------------------------------------------------------------
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private Scope dummyScope;               // used for both enter/exit
    @Mock private JsonNode node;                  // node under validation
    @Mock private JsonNode rootNode;              // root of the document
    @Mock private JsonNode schemaArrayNode;       // the "oneOf" array node passed to ctor

    // -----------------------------------------------------------------------
    // Mocks for child schemas
    // -----------------------------------------------------------------------
    @Mock private JsonSchema schemaA;
    @Mock private JsonSchema schemaB;

    // -----------------------------------------------------------------------
    // Helper objects
    // -----------------------------------------------------------------------
    private OneOfValidator validator;

    @Before
    public void setUp() throws Exception {
        // -------------------------------------------------------------------
        // Common stubbing for the execution context hierarchy
        // -------------------------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        // -------------------------------------------------------------------
        // The validator expects a JsonNode that behaves like an array.
        // For the purpose of the constructor we give a mock that reports size 0.
        // -------------------------------------------------------------------
        when(schemaArrayNode.size()).thenReturn(0);

        // -------------------------------------------------------------------
        // Build the validator (it will have an empty internal schema list)
        // -------------------------------------------------------------------
        validator = new OneOfValidator("#/testOneOf", schemaArrayNode,
                mock(JsonSchema.class), mock(ValidationContext.class));

        // -------------------------------------------------------------------
        // Inject the two mocked child schemas via reflection.
        // -------------------------------------------------------------------
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonSchema> schemaList = (List<JsonSchema>) schemasField.get(validator);
        schemaList.clear();
        schemaList.add(schemaA);
        schemaList.add(schemaB);
    }

    // -----------------------------------------------------------------------
    // Helper to set a generic ValidationMessage mock
    // -----------------------------------------------------------------------
    private ValidationMessage mockMessage(String msg) {
        ValidationMessage vm = mock(ValidationMessage.class);
        when(vm.getMessage()).thenReturn(msg);
        return vm;
    }

    // -----------------------------------------------------------------------
    // 1. Exactly one schema validates successfully
    // -----------------------------------------------------------------------
    @Test
    public void testValidate_ExactlyOneValidSchema() {
        // schemaA succeeds, schemaB fails
        when(schemaA.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(schemaB.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(mockMessage("B failed")));

        // validatorState defaults
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true); // after a successful walk

        Set<ValidationMessage> result = validator.validate(executionContext, node, rootNode, "$");

        // No errors – exactly one schema matched
        assertTrue("Result must be empty when exactly one schema matches", result.isEmpty());

        // The validator must have set the matched flag on the state
        verify(validatorState, atLeastOnce()).setMatchedNode(true);
    }

    // -----------------------------------------------------------------------
    // 2. No schema validates – we should get a OneOf error plus child errors
    // -----------------------------------------------------------------------
    @Test
    public void testValidate_NoValidSchema() {
        ValidationMessage errA = mockMessage("A error");
        ValidationMessage errB = mockMessage("B error");

        when(schemaA.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(errA));
        when(schemaB.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(errB));

        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true); // irrelevant – we have errors

        Set<ValidationMessage> result = validator.validate(executionContext, node, rootNode, "$.path");

        // Expect exactly one OneOf error + the two child errors = 3 messages
        assertEquals("Three messages expected (OneOf + 2 child errors)", 3, result.size());

        // The OneOf error message contains the number of matching schemas ("0")
        boolean oneOfFound = result.stream()
                .anyMatch(m -> m.getMessage().contains("0"));
        assertTrue("OneOf validation message (with count 0) must be present", oneOfFound);

        // Child errors must be present
        assertTrue(result.contains(errA));
        assertTrue(result.contains(errB));
    }

    // -----------------------------------------------------------------------
    // 3. More than one schema validates – failFast true throws JsonSchemaException
    // -----------------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void testValidate_MultipleValidSchemas_FailFast() throws Exception {
        // Both schemas succeed
        when(schemaA.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(schemaB.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        // Enable fail‑fast via reflection (field defined in BaseJsonValidator)
        Field failFastField = BaseJsonValidator.class.getDeclaredField("failFast");
        failFastField.setAccessible(true);
        failFastField.setBoolean(validator, true);

        // This call must throw JsonSchemaException
        validator.validate(executionContext, node, rootNode, "$");
    }

    // -----------------------------------------------------------------------
    // 4. walk() with shouldValidateSchema = true delegates to validate()
    // -----------------------------------------------------------------------
    @Test
    public void testWalk_ShouldValidate() {
        // Spy the validator to intercept the validate() call
        OneOfValidator spyValidator = Mockito.spy(validator);

        // stub validate() to return an empty set (no need to go through the whole loop)
        doReturn(Collections.emptySet())
                .when(spyValidator).validate(any(), any(), any(), anyString());

        Set<ValidationMessage> msgs = spyValidator.walk(executionContext, node, rootNode, "$", true);

        // validate() must have been invoked exactly once
        verify(spyValidator, times(1)).validate(executionContext, node, rootNode, "$");
        assertTrue(msgs.isEmpty());
    }

    // -----------------------------------------------------------------------
    // 5. walk() with shouldValidateSchema = false calls each child schema.walk()
    // -----------------------------------------------------------------------
    @Test
    public void testWalk_ShouldNotValidate() {
        // Both child schemas should receive walk() calls
        when(schemaA.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(Collections.emptySet());
        when(schemaB.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(Collections.emptySet());

        Set<ValidationMessage> msgs = validator.walk(executionContext, node, rootNode, "$", false);

        // No messages expected because we do not aggregate child walk results
        assertTrue(msgs.isEmpty());

        verify(schemaA, times(1)).walk(executionContext, node, rootNode, "$", false);
        verify(schemaB, times(1)).walk(executionContext, node, rootNode, "$", false);
    }

    // -----------------------------------------------------------------------
    // 6. preloadJsonSchema() must invoke initializeValidators() on each child
    // -----------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchema_DelegatesToChildren() {
        // No behavior needed – just verify the delegation
        validator.preloadJsonSchema();

        verify(schemaA, times(1)).initializeValidators();
        verify(schemaB, times(1)).initializeValidators();
    }
}
```
###Test END##