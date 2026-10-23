###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.CollectorContext.Scope;

/**
 * Comprehensive JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Constructor – building the internal schema list.</li>
 *   <li>validate() – the three possible outcomes (exactly one match, none, more than one).</li>
 *   <li>validate() – fail‑fast behaviour.</li>
 *   <li>walk() – delegating to {@code validate} when validation is requested and delegating to each child schema otherwise.</li>
 *   <li>preloadJsonSchema() – ensuring each child schema is initialised.</li>
 * </ul>
 *
 * All external collaborators are mocked with Mockito so the tests exercise only the logic inside {@code OneOfValidator}.
 */
public class OneOfValidatorTest {

    private static final String SCHEMA_PATH = "/oneOf";

    private ObjectMapper mapper = new ObjectMapper();

    /** Mocks used in most tests */
    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private Scope dynamicScope;
    @Mock private JsonSchema childSchema1;
    @Mock private JsonSchema childSchema2;

    /** Helper collections used by {@link CollectorContext} */
    private Set<String> evaluatedItems = new HashSet<>();
    private Set<String> evaluatedProperties = new HashSet<>();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // CollectorContext behaviour
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(collectorContext.getEvaluatedItems()).thenReturn(evaluatedItems);
        when(collectorContext.getEvaluatedProperties()).thenReturn(evaluatedProperties);

        // ExecutionContext simply returns the mocked CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // Default ValidatorState behaviour – walk disabled, validation enabled, node considered matched
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        // setMatchedNode / setComplexValidator are void – we just let them be called
    }

    /**
     * Builds a OneOfValidator instance that contains two child schemas.
     *
     * @return a freshly constructed {@link OneOfValidator}
     * @throws Exception if the JSON for the schema array cannot be parsed
     */
    private OneOfValidator createValidator() throws Exception {
        // JSON array representing two (empty) subschemas – the actual content is irrelevant because we mock the subschemas
        JsonNode schemaNode = mapper.readTree("[{}, {}]");

        // ValidationContext#newSchema must return the two mocked child schemas in order
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/0"), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(childSchema1);
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/1"), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(childSchema2);

        return new OneOfValidator(SCHEMA_PATH, schemaNode, mock(JsonSchema.class), validationContext);
    }

    /** --------------------------------------------------------------
     *  VALIDATE – EXACTLY ONE CHILD SCHEMA MATCHES
     *  -------------------------------------------------------------- */
    @Test
    public void validate_ExactlyOneValidSchema_NoErrorsReturned() throws Exception {
        OneOfValidator validator = createValidator();

        // childSchema1 fails (returns a non‑empty error set)
        Set<ValidationMessage> errorsFromFirst = Collections.singleton(
                ValidationMessage.builder().path("a").message("error1").build());
        when(childSchema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errorsFromFirst);

        // childSchema2 succeeds (empty error set)
        when(childSchema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        JsonNode node = mapper.readTree("{\"field\":\"value\"}");

        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // No validation messages should be produced because exactly one schema matches
        assertTrue("Expected no validation errors", result.isEmpty());

        // Verify that the state was marked as a complex validator and that the matched‑node flag is set
        verify(validatorState).setComplexValidator(true);
        verify(validatorState, atLeastOnce()).setMatchedNode(true);
    }

    /** --------------------------------------------------------------
     *  VALIDATE – NO CHILD SCHEMA MATCHES
     *  -------------------------------------------------------------- */
    @Test
    public void validate_NoValidSchema_ReturnsOneOfError() throws Exception {
        OneOfValidator validator = createValidator();

        // Both child schemas return errors
        Set<ValidationMessage> errorSet = Collections.singleton(
                ValidationMessage.builder().path("b").message("errorB").build());
        when(childSchema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errorSet);
        when(childSchema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errorSet);

        JsonNode node = mapper.readTree("{\"field\":\"value\"}");

        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // OneOfValidator must add its own error (numberOfValidSchema == 0) plus the child errors
        assertEquals("Expected exactly two messages (OneOf + child)", 3, result.size());

        // The OneOf error message contains the count "0" as its argument
        boolean oneOfFound = result.stream()
                .anyMatch(m -> m.getMessage().contains("0"));
        assertTrue("OneOf error with count 0 should be present", oneOfFound);
    }

    /** --------------------------------------------------------------
     *  VALIDATE – MORE THAN ONE CHILD SCHEMA MATCHES
     *  -------------------------------------------------------------- */
    @Test
    public void validate_MultipleValidSchemas_ReturnsOneOfError() throws Exception {
        OneOfValidator validator = createValidator();

        // Both child schemas succeed (empty error sets)
        when(childSchema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(childSchema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        JsonNode node = mapper.readTree("{\"field\":\"value\"}");

        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // Expect OneOf error (count == 2) and no child errors because both succeeded
        assertEquals("Expected a single OneOf error", 1, result.size());

        ValidationMessage message = result.iterator().next();
        assertTrue("OneOf error should contain count 2", message.getMessage().contains("2"));
    }

    /** --------------------------------------------------------------
     *  VALIDATE – FAIL‑FAST MODE
     *  -------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void validate_FailFast_ThrowsException() throws Exception {
        OneOfValidator validator = createValidator();

        // Force fail‑fast via reflection (BaseJsonValidator defines the protected field 'failFast')
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        // Both schemas fail – this will trigger the OneOf error
        Set<ValidationMessage> errorSet = Collections.singleton(
                ValidationMessage.builder().path("c").message("errorC").build());
        when(childSchema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errorSet);
        when(childSchema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(errorSet);

        JsonNode node = mapper.readTree("{\"field\":\"value\"}");

        // The call must throw JsonSchemaException because failFast is true
        validator.validate(executionContext, node, node, "$");
    }

    /** --------------------------------------------------------------
     *  WALK – SHOULD VALIDATE (delegates to validate)
     *  -------------------------------------------------------------- */
    @Test
    public void walk_ShouldValidate_DelegatesToValidate() throws Exception {
        OneOfValidator validator = createValidator();

        // Spy on the validator to verify that validate() is invoked
        OneOfValidator spyValidator = spy(validator);

        JsonNode node = mapper.readTree("{\"x\":1}");

        // Walk with shouldValidateSchema = true
        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, node, "$", true);

        // validate() should have been called exactly once
        verify(spyValidator, times(1)).validate(executionContext, node, node, "$");

        // The returned set is whatever validate() produced (empty in this stubbed scenario)
        assertNotNull(result);
    }

    /** --------------------------------------------------------------
     *  WALK – SHOULD NOT VALIDATE (calls child walk for each schema)
     *  -------------------------------------------------------------- */
    @Test
    public void walk_ShouldNotValidate_InvokesChildWalks() throws Exception {
        OneOfValidator validator = createValidator();

        JsonNode node = mapper.readTree("{\"y\":2}");

        // Child schemas' walk method does nothing (returns empty set)
        when(childSchema1.walk(executionContext, node, node, "$", false)).thenReturn(Collections.emptySet());
        when(childSchema2.walk(executionContext, node, node, "$", false)).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.walk(executionContext, node, node, "$", false);

        // Both child walks must have been invoked
        verify(childSchema1, times(1)).walk(executionContext, node, node, "$", false);
        verify(childSchema2, times(1)).walk(executionContext, node, node, "$", false);

        // When validation is disabled, walk returns an empty set
        assertTrue(result.isEmpty());
    }

    /** --------------------------------------------------------------
     *  PRELOADJSONSCHEMA – ensures each child schema is initialised
     *  -------------------------------------------------------------- */
    @Test
    public void preloadJsonSchema_InitialisesAllChildSchemas() throws Exception {
        OneOfValidator validator = createValidator();

        // Call the method under test
        validator.preloadJsonSchema();

        // Verify that each child schema received a call to initializeValidators()
        verify(childSchema1, times(1)).initializeValidators();
        verify(childSchema2, times(1)).initializeValidators();
    }

    /** --------------------------------------------------------------
     *  INTERNAL – resetValidatorState is exercised indirectly via validate()
     *  -------------------------------------------------------------- */
    @Test
    public void validate_ResetsValidatorStateAfterExecution() throws Exception {
        OneOfValidator validator = createValidator();

        // One child succeeds, the other fails – we end up with exactly one valid schema
        when(childSchema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(childSchema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.singleton(
                        ValidationMessage.builder().path("d").message("errorD").build()));

        JsonNode node = mapper.readTree("{\"z\":3}");

        validator.validate(executionContext, node, node, "$");

        // After validation the validatorState must be reset:
        //   - complexValidator flag cleared
        //   - matchedNode flag set to true
        InOrder inOrder = inOrder(validatorState);
        inOrder.verify(validatorState).setComplexValidator(true);
        inOrder.verify(validatorState).setComplexValidator(false);
        inOrder.verify(validatorState).setMatchedNode(true);
    }
}
###Test END##