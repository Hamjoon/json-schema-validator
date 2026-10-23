package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * The tests cover:
 *   • normal behaviour (exactly one schema matches)
 *   • all edge‑cases identified in the analysis
 *   • exception handling (fail‑fast mode)
 *   • auxiliary public methods (walk, preloadJsonSchema)
 *
 * Mockito is used to stub the complex collaborators
 * (ExecutionContext, CollectorContext, ValidatorState, JsonSchema, …).
 */
@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    // -----------------------------------------------------------------
    // Common test fixtures
    // -----------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private Scope dummyScope;               // used for both enter/exit calls
    @Mock private JsonSchema parentSchema;        // not used directly in tests

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        // CollectorContext returns a fresh dummy Scope for every enter/exit call
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
        // The state object is stored under a known key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // Default state behaviour – can be overridden per test
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isMatchedNode()).thenReturn(true);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
    }

    // -----------------------------------------------------------------
    // Helper to build a OneOfValidator with a given list of child schemas
    // -----------------------------------------------------------------
    private OneOfValidator buildValidator(List<JsonSchema> childSchemas) {
        // Build an ArrayNode whose size equals the number of child schemas
        ArrayNode arrayNode = mapper.createArrayNode();
        for (int i = 0; i < childSchemas.size(); i++) {
            arrayNode.add(mapper.createObjectNode()); // dummy child node
        }

        // Mock ValidationContext#newSchema to return the supplied mocks in order
        Answer<JsonSchema> answer = new Answer<JsonSchema>() {
            private int index = 0;
            @Override public JsonSchema answer(InvocationOnMock invocation) {
                return childSchemas.get(index++);
            }
        };
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenAnswer(answer);

        OneOfValidator validator = new OneOfValidator("testPath", arrayNode, parentSchema, validationContext);

        // The BaseJsonValidator has a protected 'failFast' flag – force it via reflection
        try {
            java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
            ff.setAccessible(true);
            ff.set(validator, false); // default: not fail‑fast
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return validator;
    }

    // -----------------------------------------------------------------
    // 1. validate – exactly one schema matches (happy path)
    // -----------------------------------------------------------------
    @Test
    public void testValidate_ExactlyOneMatches() {
        // child0 – returns errors, child1 – returns empty set
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        when(schema0.validate(any(), any(), any(), any())).thenReturn(
                Collections.singleton(mock(ValidationMessage.class)));
        when(schema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");

        // Because exactly one schema succeeded, there must be **no** errors
        assertTrue("No validation errors expected", result.isEmpty());

        // Verify state handling
        verify(validatorState).setComplexValidator(true);
        verify(validatorState).setMatchedNode(true);
        verify(validatorState).setComplexValidator(false); // resetValidatorState
    }

    // -----------------------------------------------------------------
    // 2. validate – zero schemas match → one‑of error + child errors
    // -----------------------------------------------------------------
    @Test
    public void testValidate_NoSchemaMatches() {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        ValidationMessage childMsg0 = mock(ValidationMessage.class);
        ValidationMessage childMsg1 = mock(ValidationMessage.class);

        when(schema0.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(childMsg0));
        when(schema1.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(childMsg1));

        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");

        // Expect one “one‑of” message + the two child messages
        assertEquals("Three messages expected (one‑of + two child)", 3, result.size());

        // The one‑of message must contain the count “0”
        boolean containsCountZero = result.stream()
                .anyMatch(m -> m.getMessage().contains("0"));
        assertTrue("One‑of message should mention 0 matching schemas", containsCountZero);
    }

    // -----------------------------------------------------------------
    // 3. validate – more than one schema matches → error and short‑circuit
    // -----------------------------------------------------------------
    @Test
    public void testValidate_MultipleSchemasMatch() {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        // Both schemas return empty error set → both match
        when(schema0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(schema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");

        // Because two schemas matched, we must get a one‑of error (count = 2)
        assertEquals("Only the one‑of error should be present", 1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue("Message must contain the number of matches (2)",
                msg.getMessage().contains("2"));
    }

    // -----------------------------------------------------------------
    // 4. validate – fail‑fast mode throws JsonSchemaException
    // -----------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void testValidate_FailFastThrowsException() throws Exception {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        when(schema0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(schema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1));

        // Switch failFast on via reflection
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.set(validator, true);

        // This call must throw JsonSchemaException because 2 schemas match
        validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");
    }

    // -----------------------------------------------------------------
    // 5. walk – shouldValidateSchema = true → delegates to validate()
    // -----------------------------------------------------------------
    @Test
    public void testWalk_ValidateDelegate() {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidator(Collections.singletonList(schema));

        Set<ValidationMessage> result = validator.walk(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$", true);

        // With a single matching schema we expect no errors
        assertTrue(result.isEmpty());

        // Verify that validate() was indeed called (indirectly via the walk call)
        verify(schema).validate(any(), any(), any(), any());
    }

    // -----------------------------------------------------------------
    // 6. walk – shouldValidateSchema = false → calls walk() on each child
    // -----------------------------------------------------------------
    @Test
    public void testWalk_NoValidationCallsChildWalk() {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        // Child walks should be invoked but we don't care about their result
        when(schema0.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());
        when(schema1.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());

        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1));

        Set<ValidationMessage> result = validator.walk(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$", false);

        // In “no‑validate” mode the validator never adds its own messages
        assertTrue("Result must be empty when shouldValidateSchema=false", result.isEmpty());

        verify(schema0).walk(any(), any(), any(), any(), eq(false));
        verify(schema1).walk(any(), any(), any(), any(), eq(false));
    }

    // -----------------------------------------------------------------
    // 7. preloadJsonSchema – each child schema must be initialised
    // -----------------------------------------------------------------
    @Test
    public void testPreloadJsonSchema_InitialisesChildren() {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1));

        validator.preloadJsonSchema();

        verify(schema0).initializeValidators();
        verify(schema1).initializeValidators();
    }

    // -----------------------------------------------------------------
    // 8. Edge case – empty schema array (no child schemas)
    // -----------------------------------------------------------------
    @Test
    public void testValidate_EmptySchemaArray() {
        OneOfValidator validator = buildValidator(Collections.emptyList());

        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");

        // Should produce a one‑of error with count 0
        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue(msg.getMessage().contains("0"));
    }

    // -----------------------------------------------------------------
    // 9. State handling – when a child schema validates without errors
    //    but state.hasMatchedNode() == false the match is ignored.
    // -----------------------------------------------------------------
    @Test
    public void testValidate_MatchIgnoredWhenStateHasNoMatchedNode() {
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        ValidationMessage childMsg = mock(ValidationMessage.class);

        // First schema succeeds (empty set) but we force hasMatchedNode = false
        when(schema0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        // Second schema fails
        when(schema1.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(childMsg));

        // Flip the flag after the first schema runs
        doAnswer(invocation -> false).when(validatorState).hasMatchedNode();

        OneOfValidator validator = buildValidator(Arrays.asList(schema0, schema1));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");

        // Because the first match is ignored and the second schema fails,
        // we end up with a one‑of error (count 0) plus the child error.
        assertEquals(2, result.size());
        boolean hasChild = result.stream().anyMatch(m -> m == childMsg);
        assertTrue("Child error must be present", hasChild);
    }

    // -----------------------------------------------------------------
    // 10. Walk mode – when state.isWalkEnabled() = true the validator
    //     must call schema.walk() instead of schema.validate()
    // -----------------------------------------------------------------
    @Test
    public void testValidate_UsesWalkWhenStateWalkEnabled() {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.walk(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        // Enable walk mode in the validator state
        when(validatorState.isWalkEnabled()).thenReturn(true);
        when(validatorState.isValidationEnabled()).thenReturn(true);

        OneOfValidator validator = buildValidator(Collections.singletonList(schema));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mapper.createObjectNode(), mapper.createObjectNode(), "$");

        // No errors expected because the walk returned an empty set
        assertTrue(result.isEmpty());

        verify(schema).walk(any(), any(), any(), any(), eq(true));
        verify(schema, never()).validate(any(), any(), any(), any());
    }
}
