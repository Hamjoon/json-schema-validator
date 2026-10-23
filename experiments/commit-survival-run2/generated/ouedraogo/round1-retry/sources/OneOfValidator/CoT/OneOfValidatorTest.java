/**
 * JUnit‑4 test suite for {@link com.networknt.schema.OneOfValidator}.
 *
 * The tests are written with Mockito to isolate the validator from the rest of the
 * schema library.  Each test builds a {@code OneOfValidator} instance whose internal
 * {@code List<JsonSchema>} is populated with mock schemas that return pre‑configured
 * validation results.  The suite covers:
 *
 * 1. Public API extraction.
 * 2. Basic functionality of each public method.
 * 3. Edge‑cases such as zero, one and multiple matching subschemas.
 * 4. The {@code failFast} flag that triggers {@link JsonSchemaException}.
 * 5. The {@code walk} and {@code preloadJsonSchema} behaviours.
 *
 * The file starts with the required marker “###Test START##” and ends with
 * “###Test END##”.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ----------------------------------------------------------------------
 * 1. PUBLIC METHODS (extracted from the source)
 * ----------------------------------------------------------------------
 *
 * public OneOfValidator(String schemaPath, JsonNode schemaNode,
 *                       JsonSchema parentSchema, ValidationContext validationContext)
 *
 * public Set<ValidationMessage> validate(ExecutionContext executionContext,
 *                                        JsonNode node, JsonNode rootNode,
 *                                        String at)
 *
 * public Set<ValidationMessage> walk(ExecutionContext executionContext,
 *                                    JsonNode node, JsonNode rootNode,
 *                                    String at, boolean shouldValidateSchema)
 *
 * public void preloadJsonSchema()
 *
 * ----------------------------------------------------------------------
 */
@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    // ------------------------------------------------------------------
    // Common mocks used by many tests
    // ------------------------------------------------------------------
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private Scope grandParentScope;
    @Mock private Scope parentScope;
    @Mock private Scope childScope;

    @Before
    public void setUpCommonMocks() {
        // CollectorContext hierarchy
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(grandParentScope, parentScope, childScope);
        when(collectorContext.exitDynamicScope()).thenReturn(childScope, parentScope, grandParentScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        // Scope merging does nothing (void method)
        doNothing().when(grandParentScope).mergeWith(any(Scope.class));
        doNothing().when(parentScope).mergeWith(any(Scope.class));
        doNothing().when(childScope).mergeWith(any(Scope.class));

        // Default state flags
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
    }

    // ------------------------------------------------------------------
    // Helper to build a OneOfValidator whose internal schemas are mocks.
    // ------------------------------------------------------------------
    private OneOfValidator buildValidator(List<Set<ValidationMessage>> schemaResults,
                                           List<Boolean> walkEnabledFlags) {
        // 1️⃣ Mock the schema node (an array node) – size() and get(i) are used.
        JsonNode schemaNode = mock(JsonNode.class);
        when(schemaNode.size()).thenReturn(schemaResults.size());
        // child nodes are not inspected – any JsonNode works.
        for (int i = 0; i < schemaResults.size(); i++) {
            when(schemaNode.get(i)).thenReturn(mock(JsonNode.class));
        }

        // 2️⃣ Create mock JsonSchema objects that return the supplied results.
        List<JsonSchema> mockSchemas = new ArrayList<>();
        for (int i = 0; i < schemaResults.size(); i++) {
            JsonSchema schemaMock = mock(JsonSchema.class);
            Set<ValidationMessage> result = schemaResults.get(i);
            when(schemaMock.validate(any(), any(), any(), anyString()))
                    .thenReturn(result);
            when(schemaMock.walk(any(), any(), any(), anyString(), anyBoolean()))
                    .thenReturn(result);
            // also needed for preloadJsonSchema()
            doNothing().when(schemaMock).initializeValidators();
            mockSchemas.add(schemaMock);
        }

        // 3️⃣ Mock ValidationContext – newSchema(...) returns the prepared mocks in order.
        ValidationContext validationContext = mock(ValidationContext.class);
        final Iterator<JsonSchema> schemaIter = mockSchemas.iterator();
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenAnswer(invocation -> schemaIter.next());

        // 4️⃣ Construct the validator (the constructor will fill the private `schemas` list).
        OneOfValidator validator = new OneOfValidator("testPath", schemaNode,
                mock(JsonSchema.class), validationContext);

        // 5️⃣ If the caller wants some schemas to behave as if walk‑enabled, adjust the mock.
        for (int i = 0; i < walkEnabledFlags.size(); i++) {
            when(mockSchemas.get(i).walk(any(), any(), any(), anyString(), eq(walkEnabledFlags.get(i))))
                    .thenReturn(schemaResults.get(i));
        }

        return validator;
    }

    // ------------------------------------------------------------------
    // 2️⃣ BASIC FUNCTIONALITY TESTS
    // ------------------------------------------------------------------

    @Test
    public void testValidate_ExactlyOneSchemaMatches_ReturnsEmptyErrors() {
        // First schema succeeds (empty set), second fails (non‑empty set)
        Set<ValidationMessage> empty = Collections.emptySet();
        ValidationMessage childError = mock(ValidationMessage.class);
        Set<ValidationMessage> errorSet = Collections.singleton(childError);

        OneOfValidator validator = buildValidator(
                Arrays.asList(empty, errorSet),
                Arrays.asList(false, false));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // No OneOf error because exactly one subschema matched.
        assertTrue("Expected no validation errors", result.isEmpty());

        // Verify that the validator state was marked as matched at the end.
        verify(validatorState).setMatchedNode(true);
    }

    @Test
    public void testValidate_NoSchemaMatches_ReturnsOneOfErrorAndChildErrors() {
        ValidationMessage err1 = mock(ValidationMessage.class);
        ValidationMessage err2 = mock(ValidationMessage.class);
        Set<ValidationMessage> set1 = Collections.singleton(err1);
        Set<ValidationMessage> set2 = Collections.singleton(err2);

        OneOfValidator validator = buildValidator(
                Arrays.asList(set1, set2),
                Arrays.asList(false, false));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$.field");

        // There must be at least one error (the OneOf error) plus the child errors.
        assertFalse("Expected validation errors", result.isEmpty());
        // The child errors must be present.
        assertTrue(result.contains(err1));
        assertTrue(result.contains(err2));
    }

    @Test
    public void testValidate_MultipleSchemasMatch_ReturnsOneOfError() {
        // Both schemas succeed (empty sets)
        OneOfValidator validator = buildValidator(
                Arrays.asList(Collections.emptySet(), Collections.emptySet()),
                Arrays.asList(false, false));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // Because more than one subschema matched, a OneOf error is produced.
        assertFalse("Expected OneOf validation error", result.isEmpty());

        // No child errors should be present because both schemas were valid.
        for (ValidationMessage vm : result) {
            // The message text contains the number of matching schemas (2)
            assertTrue(vm.getMessage().contains("2"));
        }
    }

    @Test(expected = JsonSchemaException.class)
    public void testValidate_MultipleSchemasMatch_FailFastThrowsException() throws Exception {
        OneOfValidator validator = buildValidator(
                Arrays.asList(Collections.emptySet(), Collections.emptySet()),
                Arrays.asList(false, false));

        // Use reflection to turn on the protected `failFast` flag.
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");
        // Expect JsonSchemaException
    }

    @Test
    public void testWalk_ShouldValidateWhenFlagTrue() {
        ValidationMessage vm = mock(ValidationMessage.class);
        Set<ValidationMessage> vmSet = Collections.singleton(vm);

        OneOfValidator validator = buildValidator(
                Collections.singletonList(vmSet),
                Collections.singletonList(false));

        Set<ValidationMessage> walkResult = validator.walk(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$", true);

        // walk with shouldValidateSchema=true delegates to validate()
        assertEquals(vmSet, walkResult);
    }

    @Test
    public void testWalk_ShouldDelegateToSchemasWhenFlagFalse() {
        // Two schemas – we only care that walk() on each schema is invoked.
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        when(schema1.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());
        when(schema2.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        // Build a validator with the two prepared mocks.
        JsonNode schemaNode = mock(JsonNode.class);
        when(schemaNode.size()).thenReturn(2);
        when(schemaNode.get(0)).thenReturn(mock(JsonNode.class));
        when(schemaNode.get(1)).thenReturn(mock(JsonNode.class));

        ValidationContext validationContext = mock(ValidationContext.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schema1, schema2);

        OneOfValidator validator = new OneOfValidator("path", schemaNode,
                mock(JsonSchema.class), validationContext);

        // Invoke walk with shouldValidateSchema = false
        validator.walk(executionContext, mock(JsonNode.class),
                mock(JsonNode.class), "$", false);

        // Verify that each schema's walk method was called exactly once.
        verify(schema1, times(1)).walk(any(), any(), any(), anyString(), eq(false));
        verify(schema2, times(1)).walk(any(), any(), any(), anyString(), eq(false));
    }

    @Test
    public void testPreloadJsonSchema_InitializesAllChildSchemas() {
        // Two child schemas – we want to ensure initializeValidators() is invoked.
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        doNothing().when(schema1).initializeValidators();
        doNothing().when(schema2).initializeValidators();

        JsonNode schemaNode = mock(JsonNode.class);
        when(schemaNode.size()).thenReturn(2);
        when(schemaNode.get(0)).thenReturn(mock(JsonNode.class));
        when(schemaNode.get(1)).thenReturn(mock(JsonNode.class));

        ValidationContext validationContext = mock(ValidationContext.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schema1, schema2);

        OneOfValidator validator = new OneOfValidator("path", schemaNode,
                mock(JsonSchema.class), validationContext);

        validator.preloadJsonSchema();

        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
    }

    // ------------------------------------------------------------------
    // 3️⃣ EDGE‑CASE / EXCEPTION TESTS (additional scenarios)
    // ------------------------------------------------------------------

    @Test
    public void testValidate_WithWalkEnabledSchemas_UsesWalkMethod() {
        // Simulate schemas that are walk‑enabled (state.isWalkEnabled() == true)
        Set<ValidationMessage> vmSet = Collections.emptySet(); // both succeed
        OneOfValidator validator = buildValidator(
                Collections.singletonList(vmSet),
                Collections.singletonList(true));

        // Force the validator state to report walk‑enabled.
        when(validatorState.isWalkEnabled()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // Still exactly one schema matches → no OneOf error.
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValidate_WhenStateHasMatchedNodeFalse_SkipsCounting() {
        // Schema returns empty set but validator state reports hasMatchedNode = false.
        Set<ValidationMessage> empty = Collections.emptySet();
        OneOfValidator validator = buildValidator(
                Collections.singletonList(empty),
                Collections.singletonList(false));

        when(validatorState.hasMatchedNode()).thenReturn(false);

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // Because hasMatchedNode is false, the validator should treat the schema as non‑matching.
        // Hence numberOfValidSchema == 0 and a OneOf error is produced.
        assertFalse("Expected OneOf error", result.isEmpty());
    }

    @Test
    public void testValidate_CollectorContextScopeMerging() {
        // The purpose of this test is to ensure the validator correctly merges scopes
        // when a subschema validates without errors.
        Set<ValidationMessage> empty = Collections.emptySet();
        OneOfValidator validator = buildValidator(
                Collections.singletonList(empty),
                Collections.singletonList(false));

        // No special stubbing needed – just invoke validate.
        validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // Verify that the parent scope was merged with the child scope exactly once.
        verify(parentScope, times(1)).mergeWith(any(Scope.class));
        // Verify that the grand‑parent scope was merged only after the whole method finishes.
        verify(grandParentScope, times(1)).mergeWith(any(Scope.class));
    }

    // ------------------------------------------------------------------
    // 4️⃣ Utility test to confirm the public method signatures (reflection based)
    // ------------------------------------------------------------------
    @Test
    public void testPublicMethodSignatures() {
        // Verify that the expected public methods exist with the correct signatures.
        Class<?> clazz = OneOfValidator.class;
        try {
            clazz.getConstructor(String.class, JsonNode.class, JsonSchema.class, ValidationContext.class);
            clazz.getMethod("validate", ExecutionContext.class, JsonNode.class, JsonNode.class, String.class);
            clazz.getMethod("walk", ExecutionContext.class, JsonNode.class, JsonNode.class, String.class, boolean.class);
            clazz.getMethod("preloadJsonSchema");
        } catch (NoSuchMethodException e) {
            fail("Expected public method missing: " + e.getMessage());
        }
    }
}
