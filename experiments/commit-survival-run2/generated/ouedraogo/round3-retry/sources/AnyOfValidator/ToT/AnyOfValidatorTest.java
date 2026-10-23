/**
 * Tests for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The tests are written with JUnit‑4 and Mockito and cover the three public
 * methods of the class:
 *
 * 1. {@code validate(...)} – normal validation, type‑mismatch handling,
 *    discriminator‑related branches and required‑property filtering.
 * 2. {@code walk(...)} – delegation to {@code validate(...)} when the flag is
 *    {@code true} and a no‑op walk when the flag is {@code false}.
 * 3. {@code preloadJsonSchema()} – verification that all child schemas are
 *    pre‑loaded.
 *
 * The test suite creates a minimal but realistic environment by mocking the
 * surrounding validation infrastructure ( {@code ValidationContext},
 * {@code ExecutionContext}, {@code CollectorContext}, {@code ValidatorState},
 * {@code JsonSchema}, etc.).  Helper methods are used to build dummy
 * {@code JsonNode} instances and {@code ValidationMessage}s.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.CollectorContext.Scope;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    /* --------------------------------------------------------------------- *
     *  Common test fixtures
     * --------------------------------------------------------------------- */

    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private Scope parentScope;          // used for dynamic scopes
    @Mock private Scope grandParentScope;
    @Mock private JsonSchema subSchema1;
    @Mock private JsonSchema subSchema2;
    @Mock private JsonNode schemaNode;        // the "anyOf" array node
    @Mock private JsonNode subNode1;           // first element of the anyOf array
    @Mock private JsonNode subNode2;           // second element of the anyOf array
    @Mock private JsonNode instanceNode;      // node that will be validated
    @Mock private JsonNode rootNode;          // root of the document (unused in tests)

    private AnyOfValidator validator;

    /**
     * Helper that creates a {@link ValidationMessage} with the supplied type
     * and message.  The real library uses a builder – the test only needs the
     * {@code getType()} and {@code getMessage()} methods.
     */
    private static ValidationMessage buildMessage(String type, String message) {
        ValidationMessage vm = mock(ValidationMessage.class);
        when(vm.getType()).thenReturn(type);
        when(vm.getMessage()).thenReturn(message);
        return vm;
    }

    @Before
    public void setUp() throws Exception {
        // -----------------------------------------------------------------
        // 1. Basic configuration of the ValidationContext
        // -----------------------------------------------------------------
        when(validationContext.getConfig()).thenReturn(config);
        // By default discriminators are disabled – individual tests will enable
        // them when needed.
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // -----------------------------------------------------------------
        // 2. ValidationContext creates the child schemas from the "anyOf" node
        // -----------------------------------------------------------------
        // The AnyOfValidator constructor iterates over schemaNode elements.
        // We mimic an array of size 2.
        when(schemaNode.size()).thenReturn(2);
        when(schemaNode.get(0)).thenReturn(subNode1);
        when(schemaNode.get(1)).thenReturn(subNode2);
        when(validationContext.newSchema(anyString(), eq(subNode1), any(JsonSchema.class)))
                .thenReturn(subSchema1);
        when(validationContext.newSchema(anyString(), eq(subNode2), any(JsonSchema.class)))
                .thenReturn(subSchema2);

        // -----------------------------------------------------------------
        // 3. CollectorContext and its dynamic scopes
        // -----------------------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(parentScope, grandParentScope);
        // exitDynamicScope returns the same mock – the validator only merges
        // scopes when there are no errors.
        when(collectorContext.exitDynamicScope()).thenReturn(parentScope);

        // -----------------------------------------------------------------
        // 4. Store a ValidatorState inside the collector context
        // -----------------------------------------------------------------
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // Default state: no matched node yet, walk disabled.
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // -----------------------------------------------------------------
        // 5. Build the AnyOfValidator under test
        // -----------------------------------------------------------------
        validator = new AnyOfValidator("anyOf", schemaNode, mock(JsonSchema.class), validationContext);
    }

    /* --------------------------------------------------------------------- *
     *  validate(...) – happy path (one subschema matches)
     * --------------------------------------------------------------------- */
    @Test
    public void validate_WhenOneSubschemaMatches_ReturnsEmptySet() {
        // Sub‑schema 1 matches (no errors), sub‑schema 2 is ignored.
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema1.validate(executionContext, instanceNode, rootNode, "/test"))
                .thenReturn(Collections.emptySet());

        when(subSchema2.hasTypeValidator()).thenReturn(false);
        when(subSchema2.validate(executionContext, instanceNode, rootNode, "/test"))
                .thenReturn(Collections.singleton(buildMessage("type", "should not be called")));

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "/test");

        assertTrue("Expected no validation errors when a subschema matches", result.isEmpty());

        // The validator must have marked the node as matched in the state.
        verify(validatorState).setMatchedNode(true);
        // The grand‑parent scope should have merged the successful child scope.
        verify(grandParentScope).mergeWith(parentScope);
    }

    /* --------------------------------------------------------------------- *
     *  validate(...) – all subschemas fail (type mismatch)
     * --------------------------------------------------------------------- */
    @Test
    public void validate_WhenAllSubschemasFail_ReturnsCombinedErrors() {
        // Both subschemas report a simple “type” error.
        ValidationMessage err1 = buildMessage("type", "expected string");
        ValidationMessage err2 = buildMessage("type", "expected integer");

        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema1.validate(executionContext, instanceNode, rootNode, "/foo"))
                .thenReturn(Collections.singleton(err1));

        when(subSchema2.hasTypeValidator()).thenReturn(false);
        when(subSchema2.validate(executionContext, instanceNode, rootNode, "/foo"))
                .thenReturn(Collections.singleton(err2));

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "/foo");

        assertEquals("Two errors should be aggregated", 2, result.size());
        assertTrue(result.contains(err1));
        assertTrue(result.contains(err2));

        // No match – the state must stay false.
        verify(validatorState, never()).setMatchedNode(true);
    }

    /* --------------------------------------------------------------------- *
     *  validate(...) – type validator short‑circuits non‑matching types
     * --------------------------------------------------------------------- */
    @Test
    public void validate_WithTypeValidatorAndMismatchingNode_SkipsSchema() {
        // Schema 1 declares type "string", node is an integer → should be skipped.
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        when(typeValidator.equalsToSchemaType(instanceNode)).thenReturn(false);
        when(subSchema1.hasTypeValidator()).thenReturn(true);
        when(subSchema1.getTypeValidator()).thenReturn(typeValidator);

        // Schema 2 declares type "integer" and matches.
        TypeValidator intValidator = mock(TypeValidator.class);
        when(intValidator.getSchemaType()).thenReturn(JsonType.INTEGER);
        when(intValidator.equalsToSchemaType(instanceNode)).thenReturn(true);
        when(subSchema2.hasTypeValidator()).thenReturn(true);
        when(subSchema2.getTypeValidator()).thenReturn(intValidator);
        when(subSchema2.validate(executionContext, instanceNode, rootNode, "/num"))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "/num");

        // Only the matching integer schema should succeed → empty error set.
        assertTrue(result.isEmpty());

        // Verify that the first schema never received a validate call.
        verify(subSchema1, never()).validate(any(), any(), any(), any());
    }

    /* --------------------------------------------------------------------- *
     *  validate(...) – discriminator enabled, match found, no errors
     * --------------------------------------------------------------------- */
    @Test
    public void validate_WithOpenAPIDiscriminator_MatchFoundAndValid_ReturnsEmptySet() {
        // Enable discriminator handling.
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        // Re‑create validator to pick up the new config flag.
        validator = new AnyOfValidator("anyOf", schemaNode, mock(JsonSchema.class), validationContext);

        // Pretend the discriminator context has already identified a matching
        // subschema (the validator sets this flag internally, we simulate it).
        validator.discriminatorContext.setDiscriminatorMatchFound(true);

        // Sub‑schema validates without errors.
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema1.validate(executionContext, instanceNode, rootNode, "/disc"))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "/disc");

        assertTrue("When discriminator matches and validation passes, no errors should be returned", result.isEmpty());

        // No additional “discriminator‑selected candidate …” message must appear.
        verify(collectorContext, never()).leaveDiscriminatorContextImmediately(anyString());
    }

    /* --------------------------------------------------------------------- *
     *  validate(...) – discriminator active but no match found
     * --------------------------------------------------------------------- */
    @Test
    public void validate_WithOpenAPIDiscriminator_NoMatchFound_ReturnsDiscriminatorError() {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        validator = new AnyOfValidator("anyOf", schemaNode, mock(JsonSchema.class), validationContext);

        // No match is recorded in the discriminator context.
        // All subschemas return some error → the validator should fall back to
        // the generic discriminator‑failure message.
        ValidationMessage err = buildMessage("type", "any error");
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema1.validate(executionContext, instanceNode, rootNode, "/disc"))
                .thenReturn(Collections.singleton(err));
        when(subSchema2.hasTypeValidator()).thenReturn(false);
        when(subSchema2.validate(executionContext, instanceNode, rootNode, "/disc"))
                .thenReturn(Collections.singleton(err));

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "/disc");

        assertEquals("Exactly one error (the discriminator‑failure message) should be returned", 1, result.size());
        ValidationMessage vm = result.iterator().next();
        assertEquals("based on the provided discriminator. No alternative could be chosen based on the discriminator property",
                vm.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  walk(...) – should delegate to validate when flag is true
     * --------------------------------------------------------------------- */
    @Test
    public void walk_WithValidateFlagTrue_DelegatesToValidate() {
        // Spy on the validator to intercept the internal call.
        AnyOfValidator spyValidator = Mockito.spy(validator);
        doReturn(Collections.emptySet()).when(spyValidator).validate(executionContext, instanceNode, rootNode, "/walk");

        Set<ValidationMessage> result = spyValidator.walk(executionContext, instanceNode, rootNode, "/walk", true);

        assertTrue(result.isEmpty());
        verify(spyValidator, times(1)).validate(executionContext, instanceNode, rootNode, "/walk");
    }

    /* --------------------------------------------------------------------- *
     *  walk(...) – should not validate when flag is false
     * --------------------------------------------------------------------- */
    @Test
    public void walk_WithValidateFlagFalse_WalksChildSchemasWithoutValidation() {
        // Child schemas' walk method should be invoked with shouldValidateSchema=false.
        when(subSchema1.walk(executionContext, instanceNode, rootNode, "/walk", false)).thenReturn(new LinkedHashSet<>());
        when(subSchema2.walk(executionContext, instanceNode, rootNode, "/walk", false)).thenReturn(new LinkedHashSet<>());

        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, "/walk", false);

        assertTrue("walk with flag false must return an empty set", result.isEmpty());
        verify(subSchema1).walk(executionContext, instanceNode, rootNode, "/walk", false);
        verify(subSchema2).walk(executionContext, instanceNode, rootNode, "/walk", false);
    }

    /* --------------------------------------------------------------------- *
     *  preloadJsonSchema() – all child schemas must be pre‑loaded
     * --------------------------------------------------------------------- */
    @Test
    public void preloadJsonSchema_InvokesPreloadOnAllSubschemas() {
        // The method under test is final in BaseJsonValidator; we cannot spy on
        // it directly.  Instead we verify that each child schema receives the
        // preloadJsonSchema() call.
        validator.preloadJsonSchema();

        verify(subSchema1).preloadJsonSchema();
        verify(subSchema2).preloadJsonSchema();
    }
}
