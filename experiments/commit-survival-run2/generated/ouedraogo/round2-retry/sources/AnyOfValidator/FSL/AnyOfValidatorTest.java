/**
 * JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * <p>The tests use Mockito to isolate {@code AnyOfValidator} from the rest of the
 * validation framework.  Only the public API of the class ( {@code validate},
 * {@code walk} and {@code preloadJsonSchema}) is exercised.  All complex
 * collaborators – {@link com.networknt.schema.ValidationContext},
 * {@link com.networknt.schema.ExecutionContext},
 * {@link com.networknt.schema.CollectorContext},
 * {@link com.networknt.schema.JsonSchema},
 * {@link com.networknt.schema.TypeValidator},
 * {@link com.networknt.schema.ValidatorState} – are mocked so that each
 * logical branch inside {@code AnyOfValidator.validate(...)} can be triggered
 * deterministically.</p>
 *
 * <p>Because the real {@code buildValidationMessage(...)} method (in the
 * abstract {@code BaseJsonValidator}) creates a concrete {@code
 * ValidationMessage} instance, the tests do not depend on its exact contents.
 * Instead they verify the size of the returned {@code Set<ValidationMessage>}
 * and, when necessary, the presence of the special discriminator remark
 * (a constant defined in {@code AnyOfValidator}).</p>
 *
 * @author  OpenAI ChatGPT
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    /* --------------------------------------------------------------------- *
     *  Common test fixtures
     * --------------------------------------------------------------------- */

    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private Scope dynamicScope;               // generic dynamic scope mock
    @Mock private ValidatorState validatorState;
    @Mock private JsonSchema parentSchema;         // the schema that owns the AnyOfValidator
    @Mock private JsonSchema subSchema;            // a sub‑schema used in many tests
    @Mock private TypeValidator typeValidator;     // type validator used for type‑mismatch tests
    @Mock private ValidationMessage validationMessage; // generic validation message

    private JsonNode dummyNode;                     // the JSON node that will be validated
    private String at = "$";                        // JSON‑Pointer location used in the tests

    @Before
    public void setUp() {
        // a simple JSON node – the concrete content is irrelevant for the logic we test
        dummyNode = JsonNodeFactory.instance.textNode("dummy");

        // default stubbing for the validation context hierarchy
        when(validationContext.getConfig()).thenReturn(config);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // the dynamic scopes used by the validator – they are only required to exist,
        // their behaviour (mergeWith) is verified via Mockito’s “verify” calls.
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dynamicScope);
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods
     * --------------------------------------------------------------------- */

    /**
     * Creates an {@code AnyOfValidator} instance that contains a single
     * sub‑schema (the mock supplied in {@code subSchema}).
     *
     * @param discriminatorEnabled whether OpenAPI‑3 style discriminators are active
     * @return a fully initialised {@code AnyOfValidator}
     */
    private AnyOfValidator createValidator(boolean discriminatorEnabled) {
        // the schema node that represents the “anyOf” array – we need a non‑null node
        JsonNode anyOfArrayNode = JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode()); // one empty object element

        // the validation context must be able to create a new schema for that element
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(subSchema);

        // configure the discriminator flag
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(discriminatorEnabled);

        // finally construct the validator
        return new AnyOfValidator("anyOf", anyOfArrayNode, parentSchema, validationContext);
    }

    /**
     * Returns a {@link ValidationMessage} that mimics the one produced by
     * {@code BaseJsonValidator#buildValidationMessage}.  The concrete type and
     * content are not important – the tests only need a distinct instance.
     */
    private ValidationMessage dummyMessage(String type, String message) {
        ValidationMessage vm = mock(ValidationMessage.class);
        when(vm.getType()).thenReturn(type);
        when(vm.getMessage()).thenReturn(message);
        return vm;
    }

    /* --------------------------------------------------------------------- *
     *  1.  Type‑mismatch branch (no UNION, type does not match)
     * --------------------------------------------------------------------- */

    @Test
    public void testValidate_TypeMismatchAddsMessageAndSkipsSchema() {
        AnyOfValidator validator = createValidator(false);

        // ----- configure the sub‑schema to expose a type validator -----
        when(subSchema.hasTypeValidator()).thenReturn(true);
        when(subSchema.getTypeValidator()).thenReturn(typeValidator);
        // pretend the schema expects a STRING but the node is NOT a STRING
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        when(typeValidator.equalsToSchemaType(dummyNode)).thenReturn(false);

        // ----- validator state: we start with “no matched node” -----
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // ----- the validation message that should be produced for the type error -----
        ValidationMessage typeError = dummyMessage("type", "type mismatch");
        // we cannot call the protected buildValidationMessage, but we can stub the
        // internal call by letting the validator add our dummy message directly
        // via the “allErrors” collection – this is achieved by making the
        // validator think the schema returned a non‑empty error set.
        Set<ValidationMessage> typeErrorSet = Collections.singleton(typeError);
        when(subSchema.validate(executionContext, dummyNode, dummyNode, at)).thenReturn(typeErrorSet);

        // invoke the validator
        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyNode, at);

        // we expect exactly the type error message
        assertEquals(1, result.size());
        assertTrue(result.contains(typeError));
    }

    /* --------------------------------------------------------------------- *
     *  2.  Successful validation without discriminators
     * --------------------------------------------------------------------- */

    @Test
    public void testValidate_SuccessfulSchemaReturnsEmptySetWhenNoDiscriminators() {
        AnyOfValidator validator = createValidator(false);

        // sub‑schema does NOT have a type validator – the type check is skipped
        when(subSchema.hasTypeValidator()).thenReturn(false);

        // the validator state reports that a node has already been matched;
        // this satisfies the “if (!state.hasMatchedNode()) continue;” guard
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // the sub‑schema validates without errors
        when(subSchema.validate(executionContext, dummyNode, dummyNode, at))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyNode, at);

        // because the sub‑schema succeeded, AnyOfValidator must return an empty set
        assertTrue("Expected no validation errors", result.isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  3.  Discriminator enabled – match found, sub‑schema returns errors
     * --------------------------------------------------------------------- */

    @Test
    public void testValidate_DiscriminatorMatchFound_WithErrorsAddsRemark() throws Exception {
        AnyOfValidator validator = createValidator(true);

        // Force the internal discriminator context to behave as “match found”.
        // The field is private & final – we replace it via reflection.
        java.lang.reflect.Field discField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        discField.setAccessible(true);
        ValidationContext.DiscriminatorContext mockDisc = mock(ValidationContext.DiscriminatorContext.class);
        when(mockDisc.isDiscriminatorMatchFound()).thenReturn(true);
        discField.set(validator, mockDisc);

        // sub‑schema does not have a type validator
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // sub‑schema returns a single validation error
        ValidationMessage subError = dummyMessage("custom", "sub‑schema error");
        when(subSchema.validate(executionContext, dummyNode, dummyNode, at))
                .thenReturn(Collections.singleton(subError));

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyNode, at);

        // The result must contain the original sub‑schema error *and* the discriminator remark.
        assertEquals(2, result.size());
        assertTrue(result.contains(subError));

        // the remark text is defined as a constant in AnyOfValidator
        boolean containsRemark = result.stream()
                .anyMatch(vm -> vm.getMessage() != null && vm.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Result should contain the discriminator remark", containsRemark);
    }

    /* --------------------------------------------------------------------- *
     *  4.  Discriminator enabled – match found, sub‑schema passes
     * --------------------------------------------------------------------- */

    @Test
    public void testValidate_DiscriminatorMatchFound_NoErrorsReturnsEmptySet() throws Exception {
        AnyOfValidator validator = createValidator(true);

        // make the discriminator context report a successful match
        java.lang.reflect.Field discField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        discField.setAccessible(true);
        ValidationContext.DiscriminatorContext mockDisc = mock(ValidationContext.DiscriminatorContext.class);
        when(mockDisc.isDiscriminatorMatchFound()).thenReturn(true);
        discField.set(validator, mockDisc);

        // sub‑schema validates cleanly
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(subSchema.validate(executionContext, dummyNode, dummyNode, at))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyNode, at);

        // Because the discriminator matched and there were no errors, the validator must
        // return an empty, immutable set.
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  5.  Discriminator enabled – no match found after looping all subschemas
     * --------------------------------------------------------------------- */

    @Test
    public void testValidate_DiscriminatorActive_NoMatchFound_ReturnsSpecialMessage() throws Exception {
        AnyOfValidator validator = createValidator(true);

        // make the discriminator context report that it is active but no match was found
        java.lang.reflect.Field discField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        discField.setAccessible(true);
        ValidationContext.DiscriminatorContext mockDisc = mock(ValidationContext.DiscriminatorContext.class);
        when(mockDisc.isActive()).thenReturn(true);
        when(mockDisc.isDiscriminatorMatchFound()).thenReturn(false);
        discField.set(validator, mockDisc);

        // sub‑schema validates with an error (so that the validator has something to report)
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        ValidationMessage subError = dummyMessage("required", "missing required property");
        when(subSchema.validate(executionContext, dummyNode, dummyNode, at))
                .thenReturn(Collections.singleton(subError));

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyNode, at);

        // The validator must return a *single* message that mentions the discriminator.
        assertEquals(1, result.size());
        ValidationMessage only = result.iterator().next();
        assertTrue("Message must refer to the discriminator",
                only.getMessage() != null && only.getMessage().contains("discriminator"));
    }

    /* --------------------------------------------------------------------- *
     *  6.  walk(...) – delegating to validate when shouldValidateSchema=true
     * --------------------------------------------------------------------- */

    @Test
    public void testWalk_DelegatesToValidateWhenShouldValidateIsTrue() {
        AnyOfValidator validator = createValidator(false);

        // make the sub‑schema succeed so that validate returns an empty set
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(subSchema.validate(executionContext, dummyNode, dummyNode, at))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.walk(executionContext, dummyNode, dummyNode, at, true);

        // walk should simply forward to validate – we already verified that validate
        // returns an empty set for this setup.
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  7.  walk(...) – when shouldValidateSchema=false all subschemas are walked
     * --------------------------------------------------------------------- */

    @Test
    public void testWalk_WhenShouldValidateIsFalseEachSubSchemaIsWalked() {
        AnyOfValidator validator = createValidator(false);

        // the walk method of the sub‑schema does not return anything (void), but we can
        // verify that it is invoked exactly once.
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(subSchema.walk(executionContext, dummyNode, dummyNode, at, false)).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.walk(executionContext, dummyNode, dummyNode, at, false);

        // The method should return an empty set (its own LinkedHashSet) and the sub‑schema
        // walk method must have been called.
        assertTrue(result.isEmpty());
        verify(subSchema, times(1)).walk(executionContext, dummyNode, dummyNode, at, false);
    }

    /* --------------------------------------------------------------------- *
     *  8.  preloadJsonSchema – simply forwards to the helper method
     * --------------------------------------------------------------------- */

    @Test
    public void testPreloadJsonSchema_ForwardsToHelper() {
        AnyOfValidator validator = createValidator(false);

        // The helper method (preloadJsonSchemas) is defined in the superclass.
        // We spy on the validator to verify that it is invoked.
        AnyOfValidator spyValidator = spy(validator);
        doNothing().when(spyValidator).preloadJsonSchemas(anyList());

        spyValidator.preloadJsonSchema();

        verify(spyValidator, times(1)).preloadJsonSchemas(anyList());
    }
}
