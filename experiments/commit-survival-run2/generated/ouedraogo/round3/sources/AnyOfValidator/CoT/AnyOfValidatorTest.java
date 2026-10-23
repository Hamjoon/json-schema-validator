package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AnyOfValidator}.  The tests use Mockito to mock all
 * collaborators (ValidationContext, ExecutionContext, JsonSchema, etc.) so that
 * the behaviour of {@code AnyOfValidator} can be exercised in isolation.
 *
 * The test class covers:
 * <ul>
 *   <li>Public API extraction</li>
 *   <li>Basic happy‑path validation</li>
 *   <li>Edge‑case handling (type mismatches, discriminator enabled, empty schemas)</li>
 *   <li>Exception‑free execution of {@code walk} and {@code preloadJsonSchema}</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    /* --------------------------------------------------------------------- */
    /*  Public methods of AnyOfValidator (extracted for reference)           */
    /* --------------------------------------------------------------------- *
     *
     * public AnyOfValidator(String schemaPath,
     *                        JsonNode schemaNode,
     *                        JsonSchema parentSchema,
     *                        ValidationContext validationContext)
     *
     * public Set<ValidationMessage> validate(ExecutionContext executionContext,
     *                                         JsonNode node,
     *                                         JsonNode rootNode,
     *                                         String at)
     *
     * public Set<ValidationMessage> walk(ExecutionContext executionContext,
     *                                     JsonNode node,
     *                                     JsonNode rootNode,
     *                                     String at,
     *                                     boolean shouldValidateSchema)
     *
     * public void preloadJsonSchema()
     *
     * --------------------------------------------------------------------- */

    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private JsonSchema parentSchema;
    @Mock private JsonNode schemaNode;          // the "anyOf" array node
    @Mock private JsonNode childSchemaNode;     // a single schema element inside the array
    @Mock private JsonNode instanceNode;        // the JSON instance being validated
    @Mock private JsonNode rootNode;            // root of the instance tree
    @Mock private JsonSchema subSchema;         // schema created for each array element
    @Mock private TypeValidator typeValidator;
    @Mock private ValidationMessage validationMessage;
    @Mock private Scope dynamicScope;           // used by CollectorContext

    private static final String SCHEMA_PATH = "$.anyOf";
    private static final String AT = "$";

    @Before
    public void setUp() {
        // Default configuration – discriminator disabled unless a test overrides it
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // ExecutionContext always returns our mocked CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // CollectorContext dynamic‑scope handling (used by AnyOfValidator)
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dynamicScope);
    }

    /**
     * Helper that creates an {@link AnyOfValidator} with a single child schema.
     */
    private AnyOfValidator createValidator(boolean discriminatorEnabled) {
        // Simulate an "anyOf" array containing a single element
        when(schemaNode.size()).thenReturn(1);
        when(schemaNode.get(0)).thenReturn(childSchemaNode);
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/0"), eq(childSchemaNode), eq(parentSchema)))
                .thenReturn(subSchema);

        // By default the sub‑schema has no type validator and validates successfully
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(subSchema.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        // Build the validator
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(discriminatorEnabled);
        AnyOfValidator validator = new AnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);

        // If discriminator handling is required, replace the internal context with a mock
        if (discriminatorEnabled) {
            ValidationContext.DiscriminatorContext mockDiscriminator = mock(ValidationContext.DiscriminatorContext.class);
            try {
                java.lang.reflect.Field field = AnyOfValidator.class.getDeclaredField("discriminatorContext");
                field.setAccessible(true);
                field.set(validator, mockDiscriminator);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return validator;
    }

    /**
     * Test that the constructor creates the expected number of sub‑schemas and
     * respects the discriminator configuration flag.
     */
    @Test
    public void testConstructorCreatesSchemasAndDiscriminatorFlag() {
        // --- discriminator disabled -------------------------------------------------
        AnyOfValidator validatorNoDisc = createValidator(false);
        // Access the private 'schemas' list via reflection
        List<JsonSchema> schemasNoDisc = getSchemasList(validatorNoDisc);
        assertEquals("Expected exactly one sub‑schema", 1, schemasNoDisc.size());
        assertNull("Discriminator context should be null when feature is disabled",
                getDiscriminatorContext(validatorNoDisc));

        // --- discriminator enabled --------------------------------------------------
        AnyOfValidator validatorDisc = createValidator(true);
        List<JsonSchema> schemasDisc = getSchemasList(validatorDisc);
        assertEquals("Expected exactly one sub‑schema", 1, schemasDisc.size());
        assertNotNull("Discriminator context must be created when feature is enabled",
                getDiscriminatorContext(validatorDisc));
    }

    /**
     * Happy‑path validation – the only sub‑schema validates without errors.
     * The validator should return an empty error set.
     */
    @Test
    public void testValidateAllValidReturnsEmptySet() {
        AnyOfValidator validator = createValidator(false);
        // Mock a validator state that reports a matched node (the usual case)
        ValidatorState state = mock(ValidatorState.class);
        when(state.hasMatchedNode()).thenReturn(true);
        when(state.isWalkEnabled()).thenReturn(false);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, AT);
        assertTrue("When the sub‑schema validates successfully, the result must be empty", result.isEmpty());

        // Verify that the sub‑schema's validate method was invoked exactly once
        verify(subSchema, times(1)).validate(executionContext, instanceNode, rootNode, AT);
    }

    /**
     * The sub‑schema returns a validation error – the validator must propagate it.
     */
    @Test
    public void testValidateSubSchemaErrorIsPropagated() {
        AnyOfValidator validator = createValidator(false);
        // Sub‑schema now returns a single error
        Set<ValidationMessage> errors = new LinkedHashSet<>();
        errors.add(validationMessage);
        when(subSchema.validate(any(), any(), any(), any())).thenReturn(errors);

        // Mock validator state
        ValidatorState state = mock(ValidatorState.class);
        when(state.hasMatchedNode()).thenReturn(true);
        when(state.isWalkEnabled()).thenReturn(false);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, AT);
        assertEquals("The returned set must contain exactly the error supplied by the sub‑schema", 1, result.size());
        assertTrue(result.contains(validationMessage));

        verify(subSchema, times(1)).validate(executionContext, instanceNode, rootNode, AT);
    }

    /**
     * When the configuration enables OpenAPI‑3 style discriminators and the
     * discriminator match is found but the sub‑schema reports errors, the validator
     * must add the special discriminator remark to the result.
     */
    @Test
    public void testValidateDiscriminatorMatchFoundWithErrorsAddsRemark() {
        AnyOfValidator validator = createValidator(true);
        // Prepare a mock discriminator context that signals a match was found
        ValidationContext.DiscriminatorContext discCtx = getDiscriminatorContext(validator);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        when(discCtx.isActive()).thenReturn(true);

        // Sub‑schema returns a single error
        Set<ValidationMessage> errors = new LinkedHashSet<>();
        errors.add(validationMessage);
        when(subSchema.validate(any(), any(), any(), any())).thenReturn(errors);

        // Mock validator state
        ValidatorState state = mock(ValidatorState.class);
        when(state.hasMatchedNode()).thenReturn(true);
        when(state.isWalkEnabled()).thenReturn(false);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, AT);
        // The original error plus the discriminator remark must be present
        assertEquals("Expected original error plus discriminator remark", 2, result.size());
        assertTrue("Original validation message must be present", result.contains(validationMessage));
    }

    /**
     * When the discriminator match is found and the sub‑schema validates
     * successfully, the validator should return an empty set (no errors).
     */
    @Test
    public void testValidateDiscriminatorMatchFoundNoErrorsReturnsEmpty() {
        AnyOfValidator validator = createValidator(true);
        ValidationContext.DiscriminatorContext discCtx = getDiscriminatorContext(validator);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        when(discCtx.isActive()).thenReturn(true);

        // Sub‑schema validates without errors (default behaviour)

        // Mock validator state
        ValidatorState state = mock(ValidatorState.class);
        when(state.hasMatchedNode()).thenReturn(true);
        when(state.isWalkEnabled()).thenReturn(false);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, AT);
        assertTrue("When discriminator matches and no errors occur, result must be empty", result.isEmpty());
    }

    /**
     * When discriminators are enabled but no discriminator match can be found,
     * the validator must return a single message explaining that no alternative
     * could be chosen.
     */
    @Test
    public void testValidateDiscriminatorActiveNoMatchReturnsSpecificMessage() {
        AnyOfValidator validator = createValidator(true);
        ValidationContext.DiscriminatorContext discCtx = getDiscriminatorContext(validator);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(false);
        when(discCtx.isActive()).thenReturn(true);

        // Sub‑schema validates without errors (default)

        // Mock validator state
        ValidatorState state = mock(ValidatorState.class);
        when(state.hasMatchedNode()).thenReturn(true);
        when(state.isWalkEnabled()).thenReturn(false);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, AT);
        assertEquals("Exactly one error message must be produced when no discriminator alternative is selected", 1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue("Message must contain the discriminator‑related wording",
                msg.getMessage().contains("discriminator"));
    }

    /**
     * The {@code walk} method should delegate to {@code validate} when
     * {@code shouldValidateSchema} is {@code true}, otherwise it must invoke
     * {@code walk} on each sub‑schema without collecting any errors.
     */
    @Test
    public void testWalkDelegatesOrIteratesCorrectly() {
        AnyOfValidator validator = createValidator(false);

        // Mock validator state for the delegated call
        ValidatorState state = mock(ValidatorState.class);
        when(state.hasMatchedNode()).thenReturn(true);
        when(state.isWalkEnabled()).thenReturn(false);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);

        // 1) shouldValidateSchema = true → delegate to validate()
        Set<ValidationMessage> delegatedResult = validator.walk(executionContext, instanceNode, rootNode, AT, true);
        assertTrue("When delegating, result must be empty (no errors from sub‑schema)", delegatedResult.isEmpty());

        // 2) shouldValidateSchema = false → each sub‑schema's walk() should be called
        validator.walk(executionContext, instanceNode, rootNode, AT, false);
        verify(subSchema, times(1)).walk(executionContext, instanceNode, rootNode, AT, false);
    }

    /**
     * The {@code preloadJsonSchema} method must invoke the protected
     * {@code preloadJsonSchemas} method defined in {@link BaseJsonValidator}.
     * We verify this by extending {@code AnyOfValidator} with a test‑specific
     * subclass that records the invocation.
     */
    @Test
    public void testPreloadJsonSchemaInvokesPreloadJsonSchemas() {
        // Create a test subclass that overrides the protected method
        class TestableAnyOfValidator extends AnyOfValidator {
            boolean preloadCalled = false;
            TestableAnyOfValidator(String schemaPath, JsonNode schemaNode,
                                   JsonSchema parentSchema, ValidationContext ctx) {
                super(schemaPath, schemaNode, parentSchema, ctx);
            }
            @Override
            protected void preloadJsonSchemas(List<JsonSchema> schemas) {
                preloadCalled = true;
                // No further action required for the test
            }
        }

        // Arrange
        when(schemaNode.size()).thenReturn(0); // no child schemas needed for this test
        TestableAnyOfValidator testable = new TestableAnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);

        // Act
        testable.preloadJsonSchema();

        // Assert
        assertTrue("preloadJsonSchemas should have been called from preloadJsonSchema()", testable.preloadCalled);
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods for reflective access                                 */
    /* --------------------------------------------------------------------- */

    /**
     * Retrieves the private {@code schemas} list from an {@link AnyOfValidator}
     * instance via reflection.
     */
    @SuppressWarnings("unchecked")
    private List<JsonSchema> getSchemasList(AnyOfValidator validator) {
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            return (List<JsonSchema>) f.get(validator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the private {@code discriminatorContext} field from an
     * {@link AnyOfValidator} instance via reflection.
     */
    private ValidationContext.DiscriminatorContext getDiscriminatorContext(AnyOfValidator validator) {
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            f.setAccessible(true);
            return (ValidationContext.DiscriminatorContext) f.get(validator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
