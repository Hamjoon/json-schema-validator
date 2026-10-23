###Test START##
```java
/**
 * Unit tests for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The tests use Mockito to mock the complex collaborators
 * ( {@link com.networknt.schema.ValidationContext},
 *   {@link com.networknt.schema.ExecutionContext},
 *   {@link com.networknt.schema.CollectorContext},
 *   {@link com.networknt.schema.JsonSchema},
 *   {@link com.networknt.schema.TypeValidator},
 *   etc.).
 *
 * The goal is to exercise every logical branch inside
 * {@code AnyOfValidator.validate(..)} as well as the constructor,
 * {@code walk(..)} and {@code preloadJsonSchema(..)}.
 *
 * NOTE: The production code depends on many classes from the
 *       *networknt* JSON‑Schema validator library.  For the purpose of
 *       these unit tests only the behaviour that is relevant for the
 *       {@code AnyOfValidator} logic is mocked.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test suite for {@link AnyOfValidator}.
 */
public class AnyOfValidatorTest {

    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private JsonSchema parentSchema;
    @Mock private JsonNode schemaNode;           // the "anyOf" array node
    @Mock private JsonNode instanceNode;         // the node being validated
    @Mock private JsonNode rootNode;             // root of the document
    @Mock private ValidationMessage typeErrorMessage;
    @Mock private ValidationMessage customErrorMessage;
    @Mock private Scope dynamicScope;            // used for CollectorContext scopes
    @Mock private Scope parentScope;             // used for merging scopes
    @Mock private ValidatorState validatorState;

    private static final String SCHEMA_PATH = "$.anyOf";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // common stubbing for collector context
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.exitDynamicScope()).thenReturn(parentScope);
    }

    /**
     * Helper method to create a validator instance with a configurable
     * discriminator flag.
     */
    private AnyOfValidator createValidator(boolean openApiDiscriminator) {
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(openApiDiscriminator);

        // The anyOf node is an array – we simulate two child schema nodes.
        JsonNode child0 = mock(JsonNode.class);
        JsonNode child1 = mock(JsonNode.class);
        when(schemaNode.size()).thenReturn(2);
        when(schemaNode.get(0)).thenReturn(child0);
        when(schemaNode.get(1)).thenReturn(child1);

        // ValidationContext creates a JsonSchema for each child.
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/0"), eq(child0), eq(parentSchema)))
                .thenReturn(schema0);
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/1"), eq(child1), eq(parentSchema)))
                .thenReturn(schema1);

        // Finally build the validator.
        return new AnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);
    }

    /**
     * Verify that the constructor builds the internal schema list correctly
     * and creates (or does not create) a discriminator context depending on
     * the configuration flag.
     */
    @Test
    public void testConstructorDiscriminatorContextCreation() throws Exception {
        // ----- discriminator disabled -----
        AnyOfValidator validatorNoDisc = createValidator(false);
        // Use reflection to read the private field
        java.lang.reflect.Field discField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        discField.setAccessible(true);
        Object discContext = discField.get(validatorNoDisc);
        assertNull("Discriminator context must be null when disabled", discContext);

        // ----- discriminator enabled -----
        AnyOfValidator validatorDisc = createValidator(true);
        discContext = discField.get(validatorDisc);
        assertNotNull("Discriminator context must be created when enabled", discContext);
    }

    /**
     * Scenario:
     *  - OpenAPI discriminator is disabled.
     *  - The single subschema has a {@link TypeValidator} whose schema type
     *    does NOT match the instance node.
     *  - The validator should add a type‑mismatch error (produced via
     *    {@code buildValidationMessage}) and must NOT invoke the subschema's
     *    {@code validate} method.
     */
    @Test
    public void testValidate_TypeMismatchAddsErrorAndSkipsSchemaValidation() {
        AnyOfValidator validator = createValidator(false);

        // Grab the first (and only) subschema created by the constructor.
        JsonSchema subschema = validator.schemas.get(0);
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(subschema.hasTypeValidator()).thenReturn(true);
        when(subschema.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        // Simulate node type mismatch.
        when(typeValidator.equalsToSchemaType(instanceNode)).thenReturn(false);

        // The subschema's validate method must never be called – we verify later.
        when(subschema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        // Prepare validator state – the initial flag does not matter for this test.
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // Execute validation.
        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        // Expect exactly one error (the type‑mismatch message).
        assertEquals("One type‑mismatch error expected", 1, result.size());

        // Verify that the subschema's validate was never invoked.
        verify(subschema, never()).validate(any(), any(), any(), any());

        // Verify that the collector entered/exited a dynamic scope exactly once.
        verify(collectorContext, times(1)).enterDynamicScope();
        verify(collectorContext, times(1)).exitDynamicScope();

        // No discriminator related calls should happen.
        verify(validationContext, never()).enterDiscriminatorContext(any(), anyString());
        verify(validationContext, never()).leaveDiscriminatorContextImmediately(anyString());
    }

    /**
     * Scenario:
     *  - OpenAPI discriminator disabled.
     *  - The subschema validates successfully (returns an empty error set)
     *    and the validator state reports a matched node.
     *  - The validator should return an empty set and set the state to
     *    matched.
     */
    @Test
    public void testValidate_ValidSubschemaReturnsEmptyAndSetsMatchedNode() {
        AnyOfValidator validator = createValidator(false);

        // Use the first subschema.
        JsonSchema subschema = validator.schemas.get(0);
        when(subschema.hasTypeValidator()).thenReturn(false);
        when(subschema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        // State: initial hasMatchedNode = false, but after validation we want
        // state.hasMatchedNode() to be true so that the validator counts the
        // subschema as valid.
        when(validatorState.hasMatchedNode()).thenReturn(false, true);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // Execute validation.
        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        // Expect no errors.
        assertTrue("Result should be empty when a subschema validates successfully", result.isEmpty());

        // Verify that the validator called state.setMatchedNode(true) at the end.
        verify(validatorState, atLeastOnce()).setMatchedNode(true);

        // Verify that the subschema's validate method was called exactly once.
        verify(subschema, times(1)).validate(executionContext, instanceNode, rootNode, "$");
    }

    /**
     * Scenario:
     *  - OpenAPI discriminator enabled.
     *  - Ensure that the validator interacts with the discriminator context
     *    (enter/leave) and that the walk/validate delegation works.
     */
    @Test
    public void testWalk_WithDiscriminatorEnabled_DelegatesToValidateWhenFlagTrue() {
        AnyOfValidator validator = createValidator(true);

        // Mock a subschema – its walk/validate will not be exercised because
        // the validator will return early via the discriminator path.
        JsonSchema subschema = validator.schemas.get(0);
        when(subschema.hasTypeValidator()).thenReturn(false);
        when(subschema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        // State stub – not used for this particular flow.
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // Execute walk with shouldValidateSchema = true – this should invoke
        // validate() internally.
        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, "$", true);

        // Since discriminator is enabled the validator will call
        // enterDiscriminatorContext and leaveDiscriminatorContextImmediately.
        verify(validationContext, times(1)).enterDiscriminatorContext(any(ValidationContext.DiscriminatorContext.class), eq("$"));
        verify(validationContext, times(1)).leaveDiscriminatorContextImmediately(eq("$"));

        // The result should be whatever validate() returned – in our mock it is empty.
        assertTrue(result.isEmpty());

        // When shouldValidateSchema = false the validator must walk each subschema
        // without performing validation.
        reset(collectorContext, validatorState);
        Set<ValidationMessage> walkResult = validator.walk(executionContext, instanceNode, rootNode, "$", false);
        assertTrue("walk with shouldValidateSchema=false must return empty set", walkResult.isEmpty());

        // Verify that each subschema's walk method was invoked once with shouldValidateSchema = false.
        for (JsonSchema schema : validator.schemas) {
            verify(schema, times(1)).walk(executionContext, instanceNode, rootNode, "$", false);
        }
    }

    /**
     * Verify that {@code preloadJsonSchema()} forwards the call to the helper
     * method defined in {@link BaseJsonValidator}.
     */
    @Test
    public void testPreloadJsonSchema_ForwardsToBaseClass() throws Exception {
        AnyOfValidator validator = createValidator(false);
        // Spy on the validator to intercept the protected call.
        AnyOfValidator spyValidator = spy(validator);
        // Invoke the method under test.
        spyValidator.preloadJsonSchema();
        // Verify that the protected helper was called with the internal schema list.
        verify(spyValidator, times(1)).preloadJsonSchemas(eq(spyValidator.schemas));
    }
}
```
###Test END##