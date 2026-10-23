/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.OneOfValidator}.
 *
 * The tests are written from the perspective of three “experts” collaborating on the
 * validation logic.  Each test covers a distinct execution path of the public API:
 *
 * 1. {@link OneOfValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}
 *    – exactly one schema matches,
 *    – no schema matches,
 *    – more than one schema matches,
 *    – fail‑fast mode.
 *
 * 2. {@link OneOfValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}
 *    – delegating to {@code validate} when validation is required,
 *    – delegating to each child schema’s {@code walk} when validation is disabled.
 *
 * 3. {@link OneOfValidator#preloadJsonSchema()}
 *    – ensures each child schema is pre‑loaded.
 *
 * The heavy‑weight collaborators (ExecutionContext, CollectorContext, ValidationContext,
 * ValidatorState, JsonSchema, JsonNode, …) are mocked with Mockito so that the
 * behaviour of {@code OneOfValidator} can be exercised in isolation.
 *
 * The test suite can be executed with any JUnit‑4 runner (e.g. Maven Surefire,
 * IDE JUnit runner, etc.).
 */

package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

/**
 * Test class for {@link OneOfValidator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    // -------------------------------------------------------------------------
    // Mocks shared by all tests
    // -------------------------------------------------------------------------
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidationContext validationContext;
    @Mock private ValidatorState validatorState;
    @Mock private JsonNode schemaNode;   // the "oneOf" array node
    @Mock private JsonNode instanceNode; // node being validated
    @Mock private JsonNode rootNode;
    @Mock private Scope dummyScope;      // placeholder for dynamic scopes

    // -------------------------------------------------------------------------
    // Helper to create a OneOfValidator with a given list of child schemas
    // -------------------------------------------------------------------------
    private OneOfValidator createValidator(List<JsonSchema> childSchemas) {
        // Mock the schemaNode so that it pretends to contain the given number of children
        when(schemaNode.size()).thenReturn(childSchemas.size());
        for (int i = 0; i < childSchemas.size(); i++) {
            when(schemaNode.get(i)).thenReturn(mock(JsonNode.class));
        }

        // ValidationContext creates a new JsonSchema for each child node.  We simply
        // return the pre‑created mocks.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenAnswer(invocation -> {
                    int idx = Integer.parseInt(((String) invocation.getArgument(0)).split("/")[2]);
                    return childSchemas.get(idx);
                });

        // Build the validator (the constructor will fill the internal "schemas" list)
        OneOfValidator validator = new OneOfValidator(
                "#/oneOf", schemaNode, mock(JsonSchema.class), validationContext);

        // Wire the execution context to return our mocked collector
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // Dynamic scopes – just return a dummy scope each time
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
        // Provide the validator state via the collector context
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // Default state behaviour
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        // The validator internally calls state.setMatchedNode(true) – we ignore the argument
        doNothing().when(validatorState).setMatchedNode(anyBoolean());
        doNothing().when(validatorState).setComplexValidator(anyBoolean());

        return validator;
    }

    // -------------------------------------------------------------------------
    // Utility to build a simple ValidationMessage set for a child schema
    // -------------------------------------------------------------------------
    private Set<ValidationMessage> singleErrorMessage(String path, String message) {
        ValidationMessage vm = ValidationMessage.of("oneOf", path, message);
        return Collections.singleton(vm);
    }

    // -------------------------------------------------------------------------
    // 1. Tests for the validate() method
    // -------------------------------------------------------------------------

    @Test
    public void validate_exactlyOneSchemaMatches_returnsNoErrors() {
        // -------------------------------------------------------------
        // Arrange – two child schemas: first matches (empty error set),
        // second does not match (non‑empty error set)
        // -------------------------------------------------------------
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        when(schema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet()); // matches
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(singleErrorMessage("#/oneOf/1", "invalid"));

        OneOfValidator validator = createValidator(Arrays.asList(schema0, schema1));

        // -------------------------------------------------------------
        // Act
        // -------------------------------------------------------------
        Set<ValidationMessage> errors = validator.validate(
                executionContext, instanceNode, rootNode, "#");

        // -------------------------------------------------------------
        // Assert – exactly one matching schema → no errors and state set
        // -------------------------------------------------------------
        assertTrue("No validation errors expected when exactly one schema matches", errors.isEmpty());

        // Verify state flag handling
        InOrder inOrder = inOrder(validatorState);
        inOrder.verify(validatorState).setComplexValidator(true);
        // resetValidatorState is a private static method – we verify its side‑effects
        inOrder.verify(validatorState).setComplexValidator(false);
        inOrder.verify(validatorState).setMatchedNode(true);
    }

    @Test
    public void validate_noSchemaMatches_returnsOneOfErrorAndChildErrors() {
        // -------------------------------------------------------------
        // Arrange – both child schemas return errors
        // -------------------------------------------------------------
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        Set<ValidationMessage> err0 = singleErrorMessage("#/oneOf/0", "error0");
        Set<ValidationMessage> err1 = singleErrorMessage("#/oneOf/1", "error1");

        when(schema0.validate(any(), any(), any(), anyString())).thenReturn(err0);
        when(schema1.validate(any(), any(), any(), anyString())).thenReturn(err1);

        OneOfValidator validator = createValidator(Arrays.asList(schema0, schema1));

        // -------------------------------------------------------------
        // Act
        // -------------------------------------------------------------
        Set<ValidationMessage> errors = validator.validate(
                executionContext, instanceNode, rootNode, "#");

        // -------------------------------------------------------------
        // Assert – OneOf error + both child errors present
        // -------------------------------------------------------------
        assertEquals("Expected exactly two child errors plus the OneOf error", 3, errors.size());

        // The OneOf error is identified by the validator type code (ONE_OF)
        boolean hasOneOfError = errors.stream()
                .anyMatch(m -> m.getType().equals(ValidatorTypeCode.ONE_OF.getValue()));
        assertTrue("Result set must contain the OneOf error", hasOneOfError);
        assertTrue("Result set must contain child error from schema0", errors.containsAll(err0));
        assertTrue("Result set must contain child error from schema1", errors.containsAll(err1));

        // Ensure the collector's evaluated collections are cleared
        verify(collectorContext).getEvaluatedItems();
        verify(collectorContext).getEvaluatedProperties();
    }

    @Test
    public void validate_multipleSchemasMatch_returnsOneOfError() {
        // -------------------------------------------------------------
        // Arrange – both schemas match (empty error sets)
        // -------------------------------------------------------------
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        when(schema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = createValidator(Arrays.asList(schema0, schema1));

        // -------------------------------------------------------------
        // Act
        // -------------------------------------------------------------
        Set<ValidationMessage> errors = validator.validate(
                executionContext, instanceNode, rootNode, "#");

        // -------------------------------------------------------------
        // Assert – more than one matching schema → OneOf error only
        // -------------------------------------------------------------
        assertEquals("Only the OneOf error should be present when >1 schemas match", 1, errors.size());

        ValidationMessage oneOfMsg = errors.iterator().next();
        assertEquals("Validator type code must be ONE_OF", ValidatorTypeCode.ONE_OF.getValue(),
                oneOfMsg.getType());
        assertEquals("The message should contain the number of valid schemas (2)",
                "2", oneOfMsg.getMessage());
    }

    @Test(expected = JsonSchemaException.class)
    public void validate_failFastEnabled_throwsJsonSchemaException() throws Exception {
        // -------------------------------------------------------------
        // Arrange – a validator that will fail fast
        // -------------------------------------------------------------
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        when(schema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet()); // matches
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet()); // also matches → triggers fail‑fast

        OneOfValidator validator = createValidator(Arrays.asList(schema0, schema1));

        // Enable fail‑fast via reflection (the field lives in BaseJsonValidator)
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        // -------------------------------------------------------------
        // Act – expect JsonSchemaException
        // -------------------------------------------------------------
        validator.validate(executionContext, instanceNode, rootNode, "#");
    }

    // -------------------------------------------------------------------------
    // 2. Tests for the walk() method
    // -------------------------------------------------------------------------

    @Test
    public void walk_shouldValidateSchema_delegatesToValidate() {
        // -------------------------------------------------------------
        // Arrange – one child schema that returns a single error
        // -------------------------------------------------------------
        JsonSchema schema = mock(JsonSchema.class);
        Set<ValidationMessage> childError = singleErrorMessage("#/oneOf/0", "invalid");
        when(schema.validate(any(), any(), any(), anyString())).thenReturn(childError);

        OneOfValidator validator = createValidator(Collections.singletonList(schema));

        // -------------------------------------------------------------
        // Act – walk with shouldValidateSchema = true
        // -------------------------------------------------------------
        Set<ValidationMessage> msgs = validator.walk(
                executionContext, instanceNode, rootNode, "#", true);

        // -------------------------------------------------------------
        // Assert – walk should have called validate() internally and
        // returned exactly the same messages.
        // -------------------------------------------------------------
        assertEquals(childError, msgs);
        verify(schema).validate(any(), any(), any(), anyString());
    }

    @Test
    public void walk_shouldNotValidateSchema_delegatesToChildrenWalk() {
        // -------------------------------------------------------------
        // Arrange – two child schemas; we only verify that walk() is
        // invoked on each child when validation is disabled.
        // -------------------------------------------------------------
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);

        // Child walks return empty sets (the actual return value is ignored)
        when(schema0.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());
        when(schema1.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = createValidator(Arrays.asList(schema0, schema1));

        // -------------------------------------------------------------
        // Act – walk with shouldValidateSchema = false
        // -------------------------------------------------------------
        Set<ValidationMessage> msgs = validator.walk(
                executionContext, instanceNode, rootNode, "#", false);

        // -------------------------------------------------------------
        // Assert – no messages are returned, but each child’s walk was called
        // -------------------------------------------------------------
        assertTrue("When validation is disabled, walk should return an empty set", msgs.isEmpty());
        verify(schema0).walk(any(), any(), any(), anyString(), eq(false));
        verify(schema1).walk(any(), any(), any(), anyString(), eq(false));
    }

    // -------------------------------------------------------------------------
    // 3. Test for preloadJsonSchema()
    // -------------------------------------------------------------------------

    @Test
    public void preloadJsonSchema_initializesAllChildSchemas() {
        // -------------------------------------------------------------
        // Arrange – three child schemas
        // -------------------------------------------------------------
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        OneOfValidator validator = createValidator(Arrays.asList(schema0, schema1, schema2));

        // -------------------------------------------------------------
        // Act
        // -------------------------------------------------------------
        validator.preloadJsonSchema();

        // -------------------------------------------------------------
        // Assert – each child schema had its initializeValidators() method invoked
        // -------------------------------------------------------------
        verify(schema0).initializeValidators();
        verify(schema1).initializeValidators();
        verify(schema2).initializeValidators();
    }

    // -------------------------------------------------------------------------
    // Additional edge‑case: empty oneOf array
    // -------------------------------------------------------------------------

    @Test
    public void validate_emptyOneOfArray_returnsOneOfError() {
        // -------------------------------------------------------------
        // Arrange – an empty schemaNode (size == 0)
        // -------------------------------------------------------------
        when(schemaNode.size()).thenReturn(0);
        OneOfValidator validator = new OneOfValidator(
                "#/oneOf", schemaNode, mock(JsonSchema.class), validationContext);

        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        doNothing().when(validatorState).setMatchedNode(anyBoolean());
        doNothing().when(validatorState).setComplexValidator(anyBoolean());

        // -------------------------------------------------------------
        // Act
        // -------------------------------------------------------------
        Set<ValidationMessage> errors = validator.validate(
                executionContext, instanceNode, rootNode, "#");

        // -------------------------------------------------------------
        // Assert – OneOf error with count = 0
        // -------------------------------------------------------------
        assertEquals("Exactly one OneOf error expected for empty array", 1, errors.size());
        ValidationMessage msg = errors.iterator().next();
        assertEquals("Validator type must be ONE_OF", ValidatorTypeCode.ONE_OF.getValue(), msg.getType());
        assertEquals("Message should indicate 0 valid schemas", "0", msg.getMessage());
    }
}
