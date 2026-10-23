###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;

/**
 * Comprehensive unit tests for {@link AnyOfValidator}.
 *
 * The tests use Mockito to isolate {@code AnyOfValidator} from the rest of the
 * validation framework and focus on the internal decision‑making logic:
 *
 * <ul>
 *   <li>type‑validator handling (type mismatch vs. match)</li>
 *   <li>successful subschema validation (including the early‑return optimisation)</li>
 *   <li>aggregation of errors when no subschema matches</li>
 *   <li>OpenAPI 3 discriminator handling (match found with/without errors)</li>
 *   <li>walk() delegation behaviour</li>
 * </ul>
 *
 * Because many collaborators are package‑private in the original library,
 * the tests rely on the public contracts of those collaborators and use
 * Mockito stubs to provide the required behaviour.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    /** Helper to create a dummy {@link JsonNode} that behaves like an array of given size. */
    private JsonNode arrayNode(int size) {
        JsonNode node = mock(JsonNode.class);
        when(node.size()).thenReturn(size);
        for (int i = 0; i < size; i++) {
            JsonNode child = mock(JsonNode.class);
            when(node.get(i)).thenReturn(child);
        }
        return node;
    }

    /** Minimal {@link ValidationContext.Config} stub. */
    private static class ConfigStub implements ValidationContext.Config {
        private final boolean openApi3Discriminators;

        ConfigStub(boolean openApi3Discriminators) {
            this.openApi3Discriminators = openApi3Discriminators;
        }

        @Override public boolean isOpenAPI3StyleDiscriminators() { return openApi3Discriminators; }

        // All other methods are irrelevant for the current tests; they return defaults.
        @Override public boolean isFailFast() { return false; }
        @Override public boolean isFailFastDeeper() { return false; }
        @Override public boolean isNullableField() { return false; }
        @Override public boolean isHandleNullableField() { return false; }
        @Override public boolean isPolymorphic() { return false; }
        @Override public boolean isFullMessage() { return false; }
        @Override public boolean isMessageOnlyIfInvalid() { return false; }
        @Override public boolean isUseDeprecatedSchemaVersion() { return false; }
        @Override public boolean isOpenAPI3StyleDiscriminator() { return false; }
        @Override public boolean isOpenAPI3StyleDiscriminators() { return openApi3Discriminators; }
        @Override public boolean isUseVerboseTypes() { return false; }
        @Override public boolean isDisallowAdditionalPropertiesIfNotSpecified() { return false; }
        @Override public boolean isFailFastNever() { return false; }
        @Override public boolean isFailFastIfInvalid() { return false; }
        @Override public boolean isSimpleTypeCheck() { return false; }
        @Override public boolean isAllowMultipleOfZero() { return false; }
        @Override public boolean isUseUniqueItems() { return false; }
        @Override public boolean isValidateFormats() { return false; }
        @Override public boolean isValidateEnums() { return false; }
        @Override public boolean isValidatePatternProperties() { return false; }
        @Override public boolean isValidateAdditionalProperties() { return false; }
        @Override public boolean isValidateDependencies() { return false; }
        @Override public boolean isValidateReadOnly() { return false; }
        @Override public boolean isValidateWriteOnly() { return false; }
        @Override public boolean isValidateWriteOnlyOnlyIfHasWriteOnlyFields() { return false; }
        @Override public boolean isValidateIfThenElse() { return false; }
        @Override public boolean isValidateMultipleOf() { return false; }
        @Override public boolean isValidateMinimumMaximum() { return false; }
        @Override public boolean isValidateItems() { return false; }
        @Override public boolean isValidateAdditionalItems() { return false; }
        @Override public boolean isValidateUniqueItems() { return false; }
        @Override public boolean isValidatePattern() { return false; }
        @Override public boolean isValidateMinLength() { return false; }
        @Override public boolean isValidateMaxLength() { return false; }
        @Override public boolean isValidateMinItems() { return false; }
        @Override public boolean isValidateMaxItems() { return false; }
        @Override public boolean isValidateAdditionalPropertiesBaseOnVersion() { return false; }
        @Override public boolean isValidateObjectAdditionalProperties() { return false; }
        @Override public boolean isValidateObjectAdditionalPropertiesBaseOnVersion() { return false; }
    }

    /** Minimal {@link ValidationContext} stub that returns the supplied {@link ConfigStub}. */
    private ValidationContext buildValidationContext(boolean openApi3Discriminators,
                                                      List<JsonSchema> schemas) {
        ValidationContext ctx = mock(ValidationContext.class);
        ValidationContext.Config cfg = new ConfigStub(openApi3Discriminators);
        when(ctx.getConfig()).thenReturn(cfg);
        // newSchema(..) must return the pre‑created JsonSchema mocks.
        Answer<JsonSchema> newSchemaAnswer = invocation -> {
            // The caller passes the child schema node; we simply return the next mock
            // from the list in order of appearance.
            int index = (int) invocation.getArgument(0, String.class).chars()
                    .filter(ch -> ch == '/')
                    .count(); // crude way to map "/0", "/1", …
            return schemas.get(index);
        };
        when(ctx.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenAnswer(newSchemaAnswer);
        // Discriminator‑related methods are no‑ops for the tests that do not enable it.
        doNothing().when(ctx).enterDiscriminatorContext(any(), anyString());
        doNothing().when(ctx).leaveDiscriminatorContextImmediately(anyString());
        return ctx;
    }

    /** Helper to create a {@link ValidatorState} mock with configurable walkEnabled flag. */
    private ValidatorState buildValidatorState(boolean walkEnabled, boolean initiallyMatched) {
        ValidatorState state = mock(ValidatorState.class);
        final boolean[] matched = { initiallyMatched };
        when(state.hasMatchedNode()).thenAnswer(inv -> matched[0]);
        doAnswer(inv -> { matched[0] = inv.getArgument(0, Boolean.class); return null; })
                .when(state).setMatchedNode(anyBoolean());
        when(state.isWalkEnabled()).thenReturn(walkEnabled);
        return state;
    }

    @Test
    public void testValidateWithTypeMismatchAddsErrorAndContinues() {
        // -------------------------------------------------
        // Setup: one subschema with a TypeValidator that does NOT match the node
        // -------------------------------------------------
        JsonNode schemaArray = arrayNode(1);
        JsonSchema subSchema = mock(JsonSchema.class);
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(subSchema.hasTypeValidator()).thenReturn(true);
        when(subSchema.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        when(typeValidator.equalsToSchemaType(any(JsonNode.class))).thenReturn(false);
        // When validation is invoked we return a non‑empty error set (simulating type error)
        ValidationMessage typeError = ValidationMessage.of("anyOf", "type mismatch");
        when(subSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(typeError));

        ValidationContext vc = buildValidationContext(false,
                Collections.singletonList(subSchema));

        // Execution context / collector context
        ExecutionContext execCtx = mock(ExecutionContext.class);
        CollectorContext collector = mock(CollectorContext.class);
        when(execCtx.getCollectorContext()).thenReturn(collector);
        // Dynamic scopes – they are just dummies that support mergeWith
        Scope dynScope = mock(Scope.class);
        when(collector.enterDynamicScope()).thenReturn(dynScope);
        when(collector.exitDynamicScope()).thenReturn(dynScope);
        // ValidatorState stored under the well‑known key
        ValidatorState state = buildValidatorState(false, false);
        when(collector.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);

        AnyOfValidator validator = new AnyOfValidator("path", schemaArray, null, vc);
        JsonNode node = mock(JsonNode.class); // the node to be validated

        Set<ValidationMessage> result = validator.validate(execCtx, node, node, "#");

        // -------------------------------------------------
        // Expectations
        // -------------------------------------------------
        // Because the type does not match, the validator should add a single
        // ValidationMessage (the one produced by the subschema) and then continue.
        assertEquals(1, result.size());
        assertTrue(result.contains(typeError));
        // No subschema succeeded, therefore the validator state must still be false.
        verify(state, never()).setMatchedNode(true);
    }

    @Test
    public void testValidateSuccessfulSubschemaReturnsEmptyWhenNoDiscriminator() {
        // -------------------------------------------------
        // Setup: one subschema that validates without errors
        // -------------------------------------------------
        JsonNode schemaArray = arrayNode(1);
        JsonSchema subSchema = mock(JsonSchema.class);
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(subSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        ValidationContext vc = buildValidationContext(false,
                Collections.singletonList(subSchema));

        ExecutionContext execCtx = mock(ExecutionContext.class);
        CollectorContext collector = mock(CollectorContext.class);
        when(execCtx.getCollectorContext()).thenReturn(collector);
        Scope dynScope = mock(Scope.class);
        when(collector.enterDynamicScope()).thenReturn(dynScope);
        when(collector.exitDynamicScope()).thenReturn(dynScope);
        // ValidatorState that reports “matched” after a successful validation
        ValidatorState state = buildValidatorState(false, false);
        when(collector.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);
        // After the empty‑error validation the validator sets matchedNode = true
        doAnswer(inv -> {
            // simulate that the subschema set the flag
            when(state.hasMatchedNode()).thenReturn(true);
            return null;
        }).when(state).setMatchedNode(anyBoolean());

        AnyOfValidator validator = new AnyOfValidator("path", schemaArray, null, vc);
        JsonNode node = mock(JsonNode.class);

        Set<ValidationMessage> result = validator.validate(execCtx, node, node, "#");

        // -------------------------------------------------
        // Expectations
        // -------------------------------------------------
        // No discriminator -> early return with empty set
        assertTrue(result.isEmpty());
        // The validator must have cleared any previously collected errors.
        verify(state).setMatchedNode(true);
    }

    @Test
    public void testValidateAggregatesErrorsWhenNoSubschemaMatches() {
        // -------------------------------------------------
        // Setup: two subschemas, both produce errors (one required, one other)
        // -------------------------------------------------
        JsonNode schemaArray = arrayNode(2);
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        ValidationMessage requiredError = ValidationMessage.of(ValidatorTypeCode.REQUIRED.getValue(),
                "missing required property");
        ValidationMessage otherError = ValidationMessage.of("anyOf", "some other failure");
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(requiredError));
        when(schema2.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(otherError));

        ValidationContext vc = buildValidationContext(false,
                Arrays.asList(schema1, schema2));

        ExecutionContext execCtx = mock(ExecutionContext.class);
        CollectorContext collector = mock(CollectorContext.class);
        when(execCtx.getCollectorContext()).thenReturn(collector);
        Scope dynScope = mock(Scope.class);
        when(collector.enterDynamicScope()).thenReturn(dynScope);
        when(collector.exitDynamicScope()).thenReturn(dynScope);
        // state starts with no matched node and walk disabled
        ValidatorState state = buildValidatorState(false, false);
        when(collector.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);

        AnyOfValidator validator = new AnyOfValidator("path", schemaArray, null, vc);
        JsonNode node = mock(JsonNode.class);

        Set<ValidationMessage> result = validator.validate(execCtx, node, node, "#");

        // -------------------------------------------------
        // Expectations
        // -------------------------------------------------
        // Both errors should be returned because there is no successful subschema.
        assertEquals(2, result.size());
        assertTrue(result.contains(requiredError));
        assertTrue(result.contains(otherError));
        // The special logic that removes required‑type errors when at least one
        // subschema succeeded must NOT fire (no success), therefore the required
        // error stays in the result.
        assertTrue(result.stream()
                .anyMatch(v -> ValidatorTypeCode.REQUIRED.getValue().equals(v.getType())));
    }

    @Test
    public void testValidateWithOpenAPI3DiscriminatorMatchFoundAndErrorsAddsRemark() {
        // -------------------------------------------------
        // Setup: OpenAPI‑3 discriminator enabled, one subschema matches the discriminator
        //        but still returns errors.
        // -------------------------------------------------
        JsonNode schemaArray = arrayNode(1);
        JsonSchema subSchema = mock(JsonSchema.class);
        when(subSchema.hasTypeValidator()).thenReturn(false);
        ValidationMessage subError = ValidationMessage.of("anyOf", "subschema error");
        when(subSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(subError));

        ValidationContext vc = buildValidationContext(true,
                Collections.singletonList(subSchema));

        // Simulate that the discriminator context reports a match
        ValidationContext.DiscriminatorContext discCtx = new ValidationContext.DiscriminatorContext();
        discCtx.setDiscriminatorMatchFound(true); // package‑private setter via reflection
        // Use reflection to inject the created context (the validator creates its own,
        // so we need to replace it after construction)
        AnyOfValidator validator = new AnyOfValidator("path", schemaArray, null, vc);
        // Replace the internal discriminatorContext with the one we configured
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            f.setAccessible(true);
            f.set(validator, discCtx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ExecutionContext execCtx = mock(ExecutionContext.class);
        CollectorContext collector = mock(CollectorContext.class);
        when(execCtx.getCollectorContext()).thenReturn(collector);
        Scope dynScope = mock(Scope.class);
        when(collector.enterDynamicScope()).thenReturn(dynScope);
        when(collector.exitDynamicScope()).thenReturn(dynScope);
        ValidatorState state = buildValidatorState(false, false);
        when(collector.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);

        JsonNode node = mock(JsonNode.class);

        Set<ValidationMessage> result = validator.validate(execCtx, node, node, "#");

        // -------------------------------------------------
        // Expectations
        // -------------------------------------------------
        // The validator must return the subschema error plus the discriminator remark.
        assertEquals(2, result.size());
        assertTrue(result.contains(subError));
        boolean remarkFound = result.stream()
                .anyMatch(v -> v.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Discriminator remark should be present", remarkFound);
    }

    @Test
    public void testValidateWithOpenAPI3DiscriminatorMatchFoundNoErrorsClearsAll() {
        // -------------------------------------------------
        // Setup: OpenAPI‑3 discriminator enabled, discriminator match found,
        //        subschema validates cleanly.
        // -------------------------------------------------
        JsonNode schemaArray = arrayNode(1);
        JsonSchema subSchema = mock(JsonSchema.class);
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(subSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        ValidationContext vc = buildValidationContext(true,
                Collections.singletonList(subSchema));

        AnyOfValidator validator = new AnyOfValidator("path", schemaArray, null, vc);
        // Force discriminator match flag
        ValidationContext.DiscriminatorContext discCtx = new ValidationContext.DiscriminatorContext();
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            f.setAccessible(true);
            f.set(validator, discCtx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Use reflection to set the internal flag to true (the validator checks this flag)
        try {
            java.lang.reflect.Field active = ValidationContext.DiscriminatorContext.class
                    .getDeclaredField("active");
            active.setAccessible(true);
            active.set(discCtx, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Also indicate that a match was found
        try {
            java.lang.reflect.Field match = ValidationContext.DiscriminatorContext.class
                    .getDeclaredField("discriminatorMatchFound");
            match.setAccessible(true);
            match.setBoolean(discCtx, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ExecutionContext execCtx = mock(ExecutionContext.class);
        CollectorContext collector = mock(CollectorContext.class);
        when(execCtx.getCollectorContext()).thenReturn(collector);
        Scope dynScope = mock(Scope.class);
        when(collector.enterDynamicScope()).thenReturn(dynScope);
        when(collector.exitDynamicScope()).thenReturn(dynScope);
        ValidatorState state = buildValidatorState(false, false);
        when(collector.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);

        JsonNode node = mock(JsonNode.class);

        Set<ValidationMessage> result = validator.validate(execCtx, node, node, "#");

        // -------------------------------------------------
        // Expectations
        // -------------------------------------------------
        // Because the discriminator matched and there were no errors,
        // the validator should return an empty set (all errors cleared).
        assertTrue(result.isEmpty());
    }

    @Test
    public void testWalkDelegatesToValidateWhenShouldValidateTrue() {
        // -------------------------------------------------
        // Setup: AnyOfValidator with a single subschema; we will spy on the validator
        //        to ensure that walk(true) calls validate().
        // -------------------------------------------------
        JsonNode schemaArray = arrayNode(1);
        JsonSchema subSchema = mock(JsonSchema.class);
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(subSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        ValidationContext vc = buildValidationContext(false,
                Collections.singletonList(subSchema));

        AnyOfValidator validator = spy(new AnyOfValidator("path", schemaArray, null, vc));

        ExecutionContext execCtx = mock(ExecutionContext.class);
        CollectorContext collector = mock(CollectorContext.class);
        when(execCtx.getCollectorContext()).thenReturn(collector);
        Scope dynScope = mock(Scope.class);
        when(collector.enterDynamicScope()).thenReturn(dynScope);
        when(collector.exitDynamicScope()).thenReturn(dynScope);
        ValidatorState state = buildValidatorState(false, false);
        when(collector.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);

        JsonNode node = mock(JsonNode.class);
        Set<ValidationMessage> result = validator.walk(execCtx, node, node, "#", true);

        // -------------------------------------------------
        // Expectations
        // -------------------------------------------------
        verify(validator, times(1)).validate(execCtx, node, node, "#");
        // The result should be the same as a direct call to validate()
        assertTrue(result.isEmpty());
    }

    @Test
    public void testWalkSkipsValidateWhenShouldValidateFalse() {
        // -------------------------------------------------
        // Setup: two subschemas; walk(false) must invoke each subschema's walk()
        //        but must not call any validator.validate().
        // -------------------------------------------------
        JsonNode schemaArray = arrayNode(2);
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        // Subschemas' walk methods are stubbed to return a non‑empty set so we can see they were called.
        ValidationMessage dummy = ValidationMessage.of("anyOf", "dummy");
        when(schemaA.walk(any(), any(), any(), anyString(), eq(false)))
                .thenReturn(Collections.singleton(dummy));
        when(schemaB.walk(any(), any(), any(), anyString(), eq(false)))
                .thenReturn(Collections.emptySet());

        ValidationContext vc = buildValidationContext(false,
                Arrays.asList(schemaA, schemaB));

        AnyOfValidator validator = new AnyOfValidator("path", schemaArray, null, vc);

        ExecutionContext execCtx = mock(ExecutionContext.class);
        CollectorContext collector = mock(CollectorContext.class);
        when(execCtx.getCollectorContext()).thenReturn(collector);
        Scope dynScope = mock(Scope.class);
        when(collector.enterDynamicScope()).thenReturn(dynScope);
        when(collector.exitDynamicScope()).thenReturn(dynScope);
        // No validator state needed for the false branch
        JsonNode node = mock(JsonNode.class);

        Set<ValidationMessage> result = validator.walk(execCtx, node, node, "#", false);

        // -------------------------------------------------
        // Expectations
        // -------------------------------------------------
        verify(schemaA, times(1)).walk(execCtx, node, node, "#", false);
        verify(schemaB, times(1)).walk(execCtx, node, node, "#", false);
        // walk(false) always returns an empty set (the validator does not aggregate the
        // subschema results when shouldValidate is false).
        assertTrue(result.isEmpty());
    }
}
###Test END##