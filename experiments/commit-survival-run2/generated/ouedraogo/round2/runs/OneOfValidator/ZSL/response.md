###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link OneOfValidator}.
 *
 * The tests use Mockito to stub out all heavy dependencies
 * (JsonSchema, ExecutionContext, CollectorContext, Scope, etc.)
 * and focus on the logical flow inside {@code OneOfValidator}.
 *
 * Scenarios covered:
 * <ul>
 *   <li>Exactly one subschema validates – no errors returned.</li>
 *   <li>No subschema validates – a OneOf error is reported together with child errors.</li>
 *   <li>More than one subschema validates – OneOf error with count > 1.</li>
 *   <li>Fail‑fast mode – {@link JsonSchemaException} is thrown.</li>
 *   <li>walk(..) with validation enabled and disabled.</li>
 *   <li>preloadJsonSchema() invokes initializeValidators on each subschema.</li>
 * </ul>
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
    private Scope grandParentScope;

    @Mock
    private Scope parentScope;

    @Mock
    private Scope childScope;

    @Mock
    private ValidatorState validatorState;

    @Mock
    private JsonSchema parentSchema;

    @Mock
    private JsonNode instanceNode;   // the node being validated

    @Mock
    private JsonNode rootNode;      // the root of the document

    private OneOfValidator validator;

    private JsonSchema schema0;
    private JsonSchema schema1;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        // Build a simple array node that represents the "oneOf" keyword.
        ArrayNode schemaArray = MAPPER.createArrayNode();
        schemaArray.addObject(); // placeholder for subschema 0
        schemaArray.addObject(); // placeholder for subschema 1

        // Mock ValidationContext to return two distinct JsonSchema mocks.
        schema0 = mock(JsonSchema.class);
        schema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(schema0, schema1);

        // Create the validator under test.
        validator = new OneOfValidator(
                "#/properties/test/oneOf",
                schemaArray,
                parentSchema,
                validationContext);

        // Common collector context stubbing.
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope())
                .thenReturn(grandParentScope)   // first call – grand‑parent scope
                .thenReturn(parentScope)      // second call – per‑schema parent scope
                .thenReturn(childScope);       // third call – inner scope for each schema

        when(collectorContext.exitDynamicScope())
                .thenReturn(childScope)       // exit inner scope
                .thenReturn(parentScope)      // exit per‑schema parent scope
                .thenReturn(grandParentScope); // exit grand‑parent

        // State handling – simple behaviours sufficient for the tests.
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        // The methods that change internal flags are no‑ops for the mock.
        doNothing().when(validatorState).setComplexValidator(anyBoolean());
        doNothing().when(validatorState).setMatchedNode(anyBoolean());

        // Evaluated items / properties collections – used only for clearing.
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
    }

    /**
     * Helper to create a dummy ValidationMessage.
     */
    private ValidationMessage dummyMessage(String path, String message) {
        ValidationMessage vm = mock(ValidationMessage.class);
        when(vm.getPath()).thenReturn(path);
        when(vm.getMessage()).thenReturn(message);
        return vm;
    }

    @Test
    public void testValidateExactlyOneValidSchema() {
        // schema0 validates successfully (empty error set)
        when(schema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // schema1 fails validation (non‑empty error set)
        ValidationMessage childError = dummyMessage("#", "child error");
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(childError)));

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "#");

        // No OneOf error because exactly one subschema succeeded.
        assertTrue("Result should be empty when exactly one subschema validates", result.isEmpty());

        // Verify that the state was marked as complex and that matchedNode flag was set.
        verify(validatorState).setComplexValidator(true);
        verify(validatorState, atLeastOnce()).setMatchedNode(true);
    }

    @Test
    public void testValidateNoValidSchema() {
        // Both subschemas return errors.
        ValidationMessage err0 = dummyMessage("#/0", "error 0");
        ValidationMessage err1 = dummyMessage("#/1", "error 1");
        when(schema0.validate(any(), any(), any(), anyString()))
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(err0)));
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(err1)));

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "#");

        // Should contain the OneOf aggregation error plus the two child errors.
        assertEquals("Expected three validation messages (OneOf + 2 child)", 3, result.size());

        // Ensure that at least one of the messages is the OneOf message (contains "0").
        boolean containsOneOf = result.stream()
                .anyMatch(vm -> vm.toString().contains("0")); // the count is 0
        assertTrue("Result must contain OneOf error with count 0", containsOneOf);
    }

    @Test
    public void testValidateMultipleValidSchemas() {
        // Both subschemas validate successfully.
        when(schema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "#");

        // More than one matching schema – should be a OneOf error with count 2.
        assertEquals("Only the OneOf aggregation error should be present", 1, result.size());
        ValidationMessage oneOfMsg = result.iterator().next();
        assertTrue("OneOf message should contain the count '2'",
                oneOfMsg.toString().contains("2"));
    }

    @Test(expected = JsonSchemaException.class)
    public void testValidateFailFast() {
        // Enable fail‑fast via reflection (the field is defined in BaseJsonValidator).
        try {
            java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
            ff.setAccessible(true);
            ff.setBoolean(validator, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Both subschemas validate successfully – this will trigger the fail‑fast path.
        when(schema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // This call must throw JsonSchemaException.
        validator.validate(executionContext, instanceNode, rootNode, "#");
    }

    @Test
    public void testWalkValidateTrue() {
        // walk(..., shouldValidateSchema = true) should delegate to validate().
        when(schema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> walkResult = validator.walk(executionContext, instanceNode, rootNode, "#", true);
        // Both schemas are valid → OneOf error with count 2.
        assertEquals(1, walkResult.size());
        ValidationMessage vm = walkResult.iterator().next();
        assertTrue(vm.toString().contains("2"));
    }

    @Test
    public void testWalkValidateFalse() {
        // walk(..., shouldValidateSchema = false) should invoke each schema's walk()
        // but must not call validate().
        Set<ValidationMessage> empty = Collections.emptySet();
        when(schema0.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(empty);
        when(schema1.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(empty);

        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, "#", false);
        // No validation performed → result should be empty.
        assertTrue(result.isEmpty());

        // Verify that walk() was indeed called on both subschemas.
        verify(schema0).walk(any(), any(), any(), anyString(), eq(false));
        verify(schema1).walk(any(), any(), any(), anyString(), eq(false));
        // Ensure that validate() was never called.
        verify(schema0, never()).validate(any(), any(), any(), anyString());
        verify(schema1, never()).validate(any(), any(), any(), anyString());
    }

    @Test
    public void testPreloadJsonSchema() {
        // preloadJsonSchema() must call initializeValidators() on each subschema.
        validator.preloadJsonSchema();

        verify(schema0).initializeValidators();
        verify(schema1).initializeValidators();
    }
}
```
###Test END##