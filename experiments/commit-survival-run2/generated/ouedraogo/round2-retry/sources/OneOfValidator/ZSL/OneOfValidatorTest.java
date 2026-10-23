package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Unit tests for {@link OneOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction of the validator (schema extraction).</li>
 *   <li>Successful validation when exactly one subschema matches.</li>
 *   <li>Validation failures when zero or more than one subschema matches.</li>
 *   <li>Fail‑fast behaviour.</li>
 *   <li>walk() delegation (both with and without validation).</li>
 *   <li>preloadJsonSchema() invoking initialise on child schemas.</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    private static final String SCHEMA_PATH = "/oneOf";
    private static final String AT = "#/test";

    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private JsonNode testNode;
    @Mock private JsonNode rootNode;

    @Captor private ArgumentCaptor<JsonSchema> schemaCaptor;

    private JsonSchema schema0;
    private JsonSchema schema1;
    private OneOfValidator validator;

    @Before
    public void setUp() throws Exception {
        // --------------------------------------------------------------------
        // Build a simple JSON array node that represents two subschemas.
        // --------------------------------------------------------------------
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaArrayNode = mapper.readTree("[{}, {}]"); // two empty subschemas

        // --------------------------------------------------------------------
        // Mock the ValidationContext to return two distinct JsonSchema mocks.
        // --------------------------------------------------------------------
        schema0 = mock(JsonSchema.class);
        schema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schema0, schema1);

        // --------------------------------------------------------------------
        // Construct the validator under test.
        // --------------------------------------------------------------------
        validator = new OneOfValidator(SCHEMA_PATH, schemaArrayNode, null, validationContext);

        // --------------------------------------------------------------------
        // Common ExecutionContext / CollectorContext wiring.
        // --------------------------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // Dynamic scopes – we just return fresh mocks; they are never inspected.
        Scope dummyScope = mock(Scope.class);
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);

        // Provide a real mutable set for the evaluated‑items/properties clearing logic.
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        // ValidatorState used inside the validator.
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        // setMatchedNode / setComplexValidator are void – we just let them do nothing.
        doNothing().when(validatorState).setMatchedNode(anyBoolean());
        doNothing().when(validatorState).setComplexValidator(anyBoolean());
    }

    // ------------------------------------------------------------------------
    // Helper method to create a ValidationMessage mock set.
    // ------------------------------------------------------------------------
    private Set<ValidationMessage> createMessageSet(int count) {
        Set<ValidationMessage> set = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            set.add(mock(ValidationMessage.class));
        }
        return set;
    }

    // ------------------------------------------------------------------------
    // 1. Exactly one subschema validates – expect no errors.
    // ------------------------------------------------------------------------
    @Test
    public void testValidateExactlyOneMatches() {
        // schema0 matches (empty error set)
        when(schema0.validate(any(ExecutionContext.class), eq(testNode), eq(rootNode), eq(AT)))
                .thenReturn(Collections.emptySet());

        // schema1 does not match (returns a non‑empty set)
        Set<ValidationMessage> schema1Errors = createMessageSet(1);
        when(schema1.validate(any(ExecutionContext.class), eq(testNode), eq(rootNode), eq(AT)))
                .thenReturn(schema1Errors);

        Set<ValidationMessage> result = validator.validate(executionContext, testNode, rootNode, AT);

        // No OneOf error should be reported.
        assertTrue("Result should be empty when exactly one subschema matches", result.isEmpty());

        // Verify that the validator marked itself as a complex validator and then reset it.
        InOrder inOrder = inOrder(validatorState);
        inOrder.verify(validatorState).setComplexValidator(true);
        inOrder.verify(validatorState).setMatchedNode(true); // after successful validation
        inOrder.verify(validatorState).setComplexValidator(false);
        inOrder.verify(validatorState).setMatchedNode(true);
    }

    // ------------------------------------------------------------------------
    // 2. Zero subschemas match – expect OneOf error + child errors.
    // ------------------------------------------------------------------------
    @Test
    public void testValidateNoMatches() {
        Set<ValidationMessage> errors0 = createMessageSet(2);
        Set<ValidationMessage> errors1 = createMessageSet(1);

        when(schema0.validate(any(), eq(testNode), eq(rootNode), eq(AT))).thenReturn(errors0);
        when(schema1.validate(any(), eq(testNode), eq(rootNode), eq(AT))).thenReturn(errors1);

        Set<ValidationMessage> result = validator.validate(executionContext, testNode, rootNode, AT);

        // OneOf error + all child errors (2 + 1 = 3 child errors)
        int expected = 1 + 2 + 1;
        assertEquals("Result should contain OneOf error plus all child errors", expected, result.size());

        // Ensure evaluated items/properties were cleared.
        assertTrue("Evaluated items set should be cleared", collectorContext.getEvaluatedItems().isEmpty());
        assertTrue("Evaluated properties set should be cleared", collectorContext.getEvaluatedProperties().isEmpty());
    }

    // ------------------------------------------------------------------------
    // 3. More than one subschema matches – short‑circuit and OneOf error.
    // ------------------------------------------------------------------------
    @Test
    public void testValidateMultipleMatches() {
        // Both schemas match (empty error sets)
        when(schema0.validate(any(), eq(testNode), eq(rootNode), eq(AT))).thenReturn(Collections.emptySet());
        when(schema1.validate(any(), eq(testNode), eq(rootNode), eq(AT))).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, testNode, rootNode, AT);

        // Only the OneOf error should be present (no child errors as both were empty).
        assertEquals("Only OneOf error should be reported when >1 schemas match", 1, result.size());
    }

    // ------------------------------------------------------------------------
    // 4. Fail‑fast behaviour – JsonSchemaException should be thrown.
    // ------------------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void testValidateFailFast() throws Exception {
        // Enable failFast via reflection (the field is defined in BaseJsonValidator).
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        // Both schemas match – this will trigger the OneOf error path.
        when(schema0.validate(any(), eq(testNode), eq(rootNode), eq(AT))).thenReturn(Collections.emptySet());
        when(schema1.validate(any(), eq(testNode), eq(rootNode), eq(AT))).thenReturn(Collections.emptySet());

        // Expect JsonSchemaException.
        validator.validate(executionContext, testNode, rootNode, AT);
    }

    // ------------------------------------------------------------------------
    // 5. walk() with validation (should delegate to validate()).
    // ------------------------------------------------------------------------
    @Test
    public void testWalkWithValidation() {
        // Setup a simple scenario where exactly one schema matches.
        when(schema0.validate(any(), eq(testNode), eq(rootNode), eq(AT))).thenReturn(Collections.emptySet());
        when(schema1.validate(any(), eq(testNode), eq(rootNode), eq(AT))).thenReturn(createMessageSet(1));

        Set<ValidationMessage> validateResult = validator.validate(executionContext, testNode, rootNode, AT);
        Set<ValidationMessage> walkResult = validator.walk(executionContext, testNode, rootNode, AT, true);

        assertEquals("walk(true) should return the same result as validate()", validateResult, walkResult);
    }

    // ------------------------------------------------------------------------
    // 6. walk() without validation – should invoke each subschema's walk().
    // ------------------------------------------------------------------------
    @Test
    public void testWalkWithoutValidation() {
        // Both subschemas are asked to walk; we verify the calls.
        Set<ValidationMessage> dummy = Collections.emptySet();
        when(schema0.walk(any(), eq(testNode), eq(rootNode), eq(AT), eq(false))).thenReturn(dummy);
        when(schema1.walk(any(), eq(testNode), eq(rootNode), eq(AT), eq(false))).thenReturn(dummy);

        Set<ValidationMessage> walkResult = validator.walk(executionContext, testNode, rootNode, AT, false);

        // No validation – walk should return an empty set.
        assertTrue("walk(false) should return empty set", walkResult.isEmpty());

        verify(schema0).walk(any(), eq(testNode), eq(rootNode), eq(AT), eq(false));
        verify(schema1).walk(any(), eq(testNode), eq(rootNode), eq(AT), eq(false));
    }

    // ------------------------------------------------------------------------
    // 7. preloadJsonSchema() – each child schema must be initialised.
    // ------------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchema() {
        // No behaviour needed; we just verify that initialiseValidators() is called.
        validator.preloadJsonSchema();

        verify(schema0).initializeValidators();
        verify(schema1).initializeValidators();
    }
}
