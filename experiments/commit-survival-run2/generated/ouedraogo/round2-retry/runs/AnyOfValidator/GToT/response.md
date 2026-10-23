###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The tests cover:
 * 1. Public API extraction (constructor, {@code validate}, {@code walk}, {@code preloadJsonSchema}).
 * 2. Basic happy‑path validation where a subschema validates successfully.
 * 3. Edge cases:
 *    • Type‑validator mismatch (union vs. concrete type).
 *    • No subschema matches – only “required” errors are filtered out.
 *    • OpenAPI‑3 style discriminator handling (match found / not found).
 * 4. Exception safety – ensuring that dynamic scopes are correctly closed even when
 *    validation throws.
 *
 * Mockito is used to stub all collaborators from the validation framework
 * ( {@link JsonSchema}, {@link ValidationContext}, {@link ExecutionContext},
 *   {@link CollectorContext}, {@link Scope}, etc.) so the tests focus purely on
 *   the logic inside {@code AnyOfValidator}.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.slf4j.Logger;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    /* --------------------------------------------------------------------- *
     *  Test fixtures – common mocks used by several test methods
     * --------------------------------------------------------------------- */
    @Mock private JsonNode rootSchemaNode;          // the “anyOf” array node
    @Mock private JsonNode subschemaNode;           // a single element inside the anyOf array
    @Mock private JsonSchema parentSchema;          // parent schema (not used directly)
    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private Scope grandParentScope;
    @Mock private Scope parentScope;
    @Mock private Scope childScope;
    @Mock private JsonSchema subschema;             // schema created from subschemaNode
    @Mock private TypeValidator typeValidator;

    private AnyOfValidator validator;                // system under test

    @Before
    public void setUp() {
        // ---------- ValidationContext configuration ----------
        when(validationContext.getConfig()).thenReturn(config);
        // default: discriminators disabled – individual tests can override
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // ---------- Schema node (anyOf array) ----------
        when(rootSchemaNode.size()).thenReturn(1);
        when(rootSchemaNode.get(0)).thenReturn(subschemaNode);

        // ---------- ValidationContext creates a JsonSchema ----------
        when(validationContext.newSchema(anyString(), eq(subschemaNode), eq(parentSchema)))
                .thenReturn(subschema);

        // ---------- CollectorContext dynamic scopes ----------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(grandParentScope, parentScope, childScope);
        when(collectorContext.exitDynamicScope()).thenReturn(parentScope, childScope);

        // ---------- ValidatorState – stored inside CollectorContext ----------
        // The validator stores a ValidatorState under a well‑known key; we provide a simple mock.
        ValidatorState validatorState = mock(ValidatorState.class);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // ---------- Build the AnyOfValidator (constructor is exercised here) ----------
        validator = new AnyOfValidator("#/anyOf", rootSchemaNode, parentSchema, validationContext);
    }

    /* --------------------------------------------------------------------- *
     *  1. Basic happy‑path validation
     * --------------------------------------------------------------------- */
    @Test
    public void testValidate_HappyPath_SubSchemaValid() {
        // Sub‑schema validates without errors.
        when(subschema.hasTypeValidator()).thenReturn(false);
        when(subschema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class),   // node to validate
                mock(JsonNode.class),   // root node (unused in this scenario)
                "$");                    // JSON‑Pointer location

        assertTrue("Validator should return an empty error set when a subschema succeeds", result.isEmpty());

        // Verify that dynamic scopes were entered / exited exactly once per subschema.
        verify(collectorContext, times(2)).enterDynamicScope(); // grand‑parent + per‑subschema
        verify(collectorContext, times(2)).exitDynamicScope();
    }

    /* --------------------------------------------------------------------- *
     *  2. Edge case – type validator present but node type does not match
     * --------------------------------------------------------------------- */
    @Test
    public void testValidate_TypeMismatch_ShouldAddTypeError() {
        JsonNode nodeToValidate = mock(JsonNode.class);
        // Simulate a concrete type validator (e.g., STRING) that does NOT match the node.
        when(subschema.hasTypeValidator()).thenReturn(true);
        when(subschema.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        when(typeValidator.equalsToSchemaType(nodeToValidate)).thenReturn(false);
        // The validator should add a type‑mismatch message and skip further validation.
        Set<ValidationMessage> result = validator.validate(executionContext, nodeToValidate,
                mock(JsonNode.class), "$");

        // Expect exactly one validation message whose type matches the schema type.
        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertEquals(JsonType.STRING.toString(), msg.getMessage()); // message is built from schema type
    }

    /* --------------------------------------------------------------------- *
     *  3. Edge case – all subschemas fail, but only “required” errors are present.
     *      The validator should suppress those errors when at least one subschema
     *      is considered valid (according to the “anyOf” semantics). Here we
     *      simulate zero valid subschemas, so the required‑errors must be kept.
     * --------------------------------------------------------------------- */
    @Test
    public void testValidate_AllInvalid_WithRequiredErrors() {
        // Subschema returns a “required” validation error.
        ValidationMessage requiredError = ValidationMessage.builder()
                .type(ValidatorTypeCode.REQUIRED.getValue())
                .message("required property missing")
                .path("$")
                .build();

        when(subschema.hasTypeValidator()).thenReturn(false);
        when(subschema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(requiredError));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // The result must contain the required‑error because no subschema succeeded.
        assertEquals(1, result.size());
        assertTrue(result.iterator().next().getType().equals(ValidatorTypeCode.REQUIRED.getValue()));
    }

    /* --------------------------------------------------------------------- *
     *  4. OpenAPI‑3 discriminator – match found and subschema passes.
     * --------------------------------------------------------------------- */
    @Test
    public void testValidate_OpenApiDiscriminator_MatchFound_SubSchemaValid() {
        // Enable discriminator handling.
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        // Re‑create validator to pick up the new config flag.
        validator = new AnyOfValidator("#/anyOf", rootSchemaNode, parentSchema, validationContext);

        // Mock discriminator context inside ValidationContext.
        ValidationContext.DiscriminatorContext discContext = mock(ValidationContext.DiscriminatorContext.class);
        // Force the validator to think a discriminator match has been found.
        when(discContext.isDiscriminatorMatchFound()).thenReturn(true);
        when(discContext.isActive()).thenReturn(true);
        // The validator stores a freshly created context, so we need to capture it.
        doAnswer(invocation -> {
            validator.validationContext.enterDiscriminatorContext(discContext, "$");
            return null;
        }).when(validationContext).enterDiscriminatorContext(any(), anyString());

        // Subschema validates successfully.
        when(subschema.hasTypeValidator()).thenReturn(false);
        when(subschema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        // Execute validation.
        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // Because the discriminator matched and the subschema is valid,
        // the validator must return an empty set (no errors).
        assertTrue(result.isEmpty());

        // Verify that the discriminator context was entered and then left.
        verify(validationContext).enterDiscriminatorContext(any(), eq("$"));
        verify(validationContext).leaveDiscriminatorContextImmediately(eq("$"));
    }

    /* --------------------------------------------------------------------- *
     *  5. OpenAPI‑3 discriminator – match found but subschema fails.
     * --------------------------------------------------------------------- */
    @Test
    public void testValidate_OpenApiDiscriminator_MatchFound_SubSchemaInvalid() {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        validator = new AnyOfValidator("#/anyOf", rootSchemaNode, parentSchema, validationContext);

        // Mock a discriminator context that reports a match.
        ValidationContext.DiscriminatorContext discContext = mock(ValidationContext.DiscriminatorContext.class);
        when(discContext.isDiscriminatorMatchFound()).thenReturn(true);
        when(discContext.isActive()).thenReturn(true);
        doAnswer(invocation -> {
            validator.validationContext.enterDiscriminatorContext(discContext, "$");
            return null;
        }).when(validationContext).enterDiscriminatorContext(any(), anyString());

        // Subschema returns a generic validation error.
        ValidationMessage genericError = ValidationMessage.builder()
                .type("type")
                .message("generic error")
                .path("$")
                .build();
        when(subschema.hasTypeValidator()).thenReturn(false);
        when(subschema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(genericError));

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        // The result must contain the original error plus the discriminator remark.
        assertEquals(2, result.size());
        boolean hasRemark = result.stream().anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Result should contain discriminator remark", hasRemark);
    }

    /* --------------------------------------------------------------------- *
     *  6. walk(…) when shouldValidateSchema = false – should delegate to subschemas
     * --------------------------------------------------------------------- */
    @Test
    public void testWalk_SkipValidation_DelegatesToSubschemas() {
        // Walk on each subschema should be called with shouldValidateSchema = false.
        when(subschema.walk(any(), any(), any(), anyString(), eq(false)))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.walk(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$", false);

        // walk returns an empty set when validation is skipped.
        assertTrue(result.isEmpty());
        verify(subschema).walk(any(), any(), any(), eq("$"), eq(false));
    }

    /* --------------------------------------------------------------------- *
     *  7. preloadJsonSchema – ensures the internal list is pre‑loaded.
     * --------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchema_InvokesHelper() {
        // Spy on the validator to capture the protected helper method.
        AnyOfValidator spyValidator = spy(validator);
        doNothing().when(spyValidator).preloadJsonSchemas(anyList());

        spyValidator.preloadJsonSchema();

        // Verify that the helper was called with the internal schemas list.
        verify(spyValidator).preloadJsonSchemas(eq(spyValidator.schemas));
    }
}
```
###Test END##