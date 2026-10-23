###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The test class covers:
 *  1. Extraction of public API (constructor, {@code validate}, {@code walk}, {@code preloadJsonSchema}).
 *  2. Basic happy‑path executions.
 *  3. Edge‑case handling:
 *      – empty “anyOf” array,
 *      – type‑mismatch short‑circuit,
 *      – successful sub‑schema validation that clears accumulated errors,
 *      – discriminator‑related branches (OpenAPI‑3 style enabled/disabled),
 *      – walk invoked with {@code shouldValidateSchema = false}.
 *  4. Verification that {@code preloadJsonSchema} delegates to the protected
 *     {@code preloadJsonSchemas} method of {@code BaseJsonValidator}.
 *
 * The validator has a large number of collaborators.  All external
 * dependencies are mocked with Mockito so that the tests focus solely on the
 * logic inside {@code AnyOfValidator}.
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

import java.lang.reflect.Field;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    // ----------------------------------------------------------------------
    // Mocks shared by many tests
    // ----------------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private CollectorContext.Scope grandParentScope;
    @Mock private CollectorContext.Scope parentScope;
    @Mock private CollectorContext.Scope childScope;

    // ----------------------------------------------------------------------
    // Common test fixtures
    // ----------------------------------------------------------------------
    private final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
    private final String schemaPath = "#/anyOf";

    @Before
    public void setUp() {
        // ValidationContext provides a Config and creates JsonSchema instances.
        when(validationContext.getConfig()).thenReturn(config);
        // By default discriminators are disabled – individual tests change this.
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // ExecutionContext supplies the CollectorContext.
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // CollectorContext returns a fresh ValidatorState and dummy Scopes.
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(collectorContext.enterDynamicScope()).thenReturn(grandParentScope, parentScope, childScope);
        when(collectorContext.exitDynamicScope()).thenReturn(parentScope, childScope);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(false);
    }

    /**
     * Helper that creates an {@code AnyOfValidator} whose internal
     * {@code schemas} list consists of the supplied {@code schemaMocks}.
     * The method also stubs {@code ValidationContext#newSchema} so that
     * the constructor picks up the provided mocks.
     */
    private AnyOfValidator createValidatorWithSchemas(JsonNode schemaArrayNode,
                                                       List<JsonSchema> schemaMocks) {
        // Stub the factory method – it is called once per element in the array.
        final Answer<JsonSchema> answer = new Answer<JsonSchema>() {
            private int idx = 0;
            @Override public JsonSchema answer(InvocationOnMock invocation) {
                return schemaMocks.get(idx++);
            }
        };
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenAnswer(answer);

        return new AnyOfValidator(schemaPath, schemaArrayNode, mock(JsonSchema.class), validationContext);
    }

    // ----------------------------------------------------------------------
    // 1. Public API extraction (implicit – the following tests compile only if
    //    the listed public members exist)
    // ----------------------------------------------------------------------
    @Test
    public void testPublicConstructorExists() {
        JsonNode emptyArray = nodeFactory.arrayNode();
        AnyOfValidator validator = new AnyOfValidator(schemaPath, emptyArray,
                mock(JsonSchema.class), validationContext);
        assertNotNull(validator);
    }

    @Test
    public void testPublicValidateExists() throws Exception {
        AnyOfValidator validator = new AnyOfValidator(schemaPath,
                nodeFactory.arrayNode(), mock(JsonSchema.class), validationContext);
        // reflection to ensure the method signature matches
        validator.getClass()
                .getMethod("validate", ExecutionContext.class, JsonNode.class,
                        JsonNode.class, String.class);
    }

    @Test
    public void testPublicWalkExists() throws Exception {
        AnyOfValidator validator = new AnyOfValidator(schemaPath,
                nodeFactory.arrayNode(), mock(JsonSchema.class), validationContext);
        validator.getClass()
                .getMethod("walk", ExecutionContext.class, JsonNode.class,
                        JsonNode.class, String.class, boolean.class);
    }

    @Test
    public void testPublicPreloadJsonSchemaExists() throws Exception {
        AnyOfValidator validator = new AnyOfValidator(schemaPath,
                nodeFactory.arrayNode(), mock(JsonSchema.class), validationContext);
        validator.getClass()
                .getMethod("preloadJsonSchema");
    }

    // ----------------------------------------------------------------------
    // 2. Basic behaviour – empty anyOf array
    // ----------------------------------------------------------------------
    @Test
    public void testValidateWithNoSubSchemasReturnsEmptySet() {
        JsonNode emptyArray = nodeFactory.arrayNode(); // []
        AnyOfValidator validator = createValidatorWithSchemas(emptyArray, Collections.emptyList());

        Set<ValidationMessage> result = validator.validate(executionContext,
                nodeFactory.textNode("any value"), nodeFactory.objectNode(), "$");
        assertTrue("Result must be empty when no sub‑schemas are present", result.isEmpty());
    }

    // ----------------------------------------------------------------------
    // 3. Type‑mismatch short‑circuit
    // ----------------------------------------------------------------------
    @Test
    public void testValidate_TypeMismatchAddsErrorAndSkipsSchemaValidation() {
        // Build a single schema mock that reports a non‑UNION type validator.
        JsonSchema schemaMock = mock(JsonSchema.class);
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(schemaMock.hasTypeValidator()).thenReturn(true);
        when(schemaMock.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        when(typeValidator.equalsToSchemaType(any(JsonNode.class))).thenReturn(false);
        // The validator should *not* be asked to validate the node.
        when(schemaMock.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        JsonNode arrayNode = nodeFactory.arrayNode().add(nodeFactory.objectNode());
        AnyOfValidator validator = createValidatorWithSchemas(arrayNode,
                Collections.singletonList(schemaMock));

        // Node of a different type (number) → mismatch.
        JsonNode testNode = nodeFactory.numberNode(123);

        Set<ValidationMessage> result = validator.validate(executionContext,
                testNode, nodeFactory.objectNode(), "$");

        // Expect exactly one error (the type‑mismatch message) and no interaction
        // with the schema's validate method.
        assertEquals(1, result.size());
        verify(schemaMock, never()).validate(any(), any(), any(), anyString());
    }

    // ----------------------------------------------------------------------
    // 4. Successful sub‑schema validation clears accumulated errors
    // ----------------------------------------------------------------------
    @Test
    public void testValidate_ValidSubSchemaClearsAllErrors() {
        // Schema that validates successfully (returns empty set)
        JsonSchema schemaMock = mock(JsonSchema.class);
        when(schemaMock.hasTypeValidator()).thenReturn(false);
        when(schemaMock.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // Simulate that the validator has already recorded a previous error.
        // The first iteration will add a dummy error; the second (valid) schema
        // should clear it.
        JsonSchema errorSchemaMock = mock(JsonSchema.class);
        when(errorSchemaMock.hasTypeValidator()).thenReturn(false);
        ValidationMessage dummyError = ValidationMessage.of("anyOf", "dummy error");
        when(errorSchemaMock.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(dummyError));

        JsonNode arrayNode = nodeFactory.arrayNode()
                .add(nodeFactory.objectNode())   // error‑producing schema
                .add(nodeFactory.objectNode());  // valid schema

        AnyOfValidator validator = createValidatorWithSchemas(arrayNode,
                Arrays.asList(errorSchemaMock, schemaMock));

        // Force the validator state to think a node has already been matched.
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        Set<ValidationMessage> result = validator.validate(executionContext,
                nodeFactory.textNode("test"), nodeFactory.objectNode(), "$");

        // Because the second schema succeeded, the validator must return an
        // empty set (all previous errors are cleared).
        assertTrue("All errors must be cleared when a sub‑schema validates", result.isEmpty());
        // Verify that the state was finally marked as matched.
        verify(validatorState).setMatchedNode(true);
    }

    // ----------------------------------------------------------------------
    // 5. Discriminator‑related branches
    // ----------------------------------------------------------------------
    @Test
    public void testValidate_DiscriminatorEnabled_NoMatchFoundReturnsDiscriminatorError() throws Exception {
        // Enable OpenAPI‑3 style discriminators.
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        // Create a validator with a single dummy schema.
        JsonSchema schemaMock = mock(JsonSchema.class);
        when(schemaMock.hasTypeValidator()).thenReturn(false);
        when(schemaMock.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        JsonNode arrayNode = nodeFactory.arrayNode().add(nodeFactory.objectNode());
        AnyOfValidator validator = createValidatorWithSchemas(arrayNode,
                Collections.singletonList(schemaMock));

        // Replace the private discriminatorContext with a Mockito spy that
        // reports “no match found”.
        Field dcField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        dcField.setAccessible(true);
        ValidationContext.DiscriminatorContext dcMock = mock(ValidationContext.DiscriminatorContext.class);
        when(dcMock.isDiscriminatorMatchFound()).thenReturn(false);
        when(dcMock.isActive()).thenReturn(true);
        dcField.set(validator, dcMock);

        Set<ValidationMessage> result = validator.validate(executionContext,
                nodeFactory.objectNode(), nodeFactory.objectNode(), "$");

        // The validator should return a single message explaining that no
        // discriminator alternative could be chosen.
        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue(msg.getMessage().contains("based on the provided discriminator"));
    }

    @Test
    public void testValidate_DiscriminatorEnabled_MatchFoundWithErrorsAddsRemark() throws Exception {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        // Schema that produces an error.
        JsonSchema schemaMock = mock(JsonSchema.class);
        when(schemaMock.hasTypeValidator()).thenReturn(false);
        ValidationMessage subError = ValidationMessage.of("type", "type mismatch");
        when(schemaMock.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(subError));

        JsonNode arrayNode = nodeFactory.arrayNode().add(nodeFactory.objectNode());
        AnyOfValidator validator = createValidatorWithSchemas(arrayNode,
                Collections.singletonList(schemaMock));

        // Mock the discriminator context to indicate a match was found.
        Field dcField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        dcField.setAccessible(true);
        ValidationContext.DiscriminatorContext dcMock = mock(ValidationContext.DiscriminatorContext.class);
        when(dcMock.isDiscriminatorMatchFound()).thenReturn(true);
        when(dcMock.isActive()).thenReturn(true);
        dcField.set(validator, dcMock);

        Set<ValidationMessage> result = validator.validate(executionContext,
                nodeFactory.objectNode(), nodeFactory.objectNode(), "$");

        // Expect the original sub‑schema error plus the discriminator remark.
        assertEquals(2, result.size());
        boolean containsRemark = result.stream()
                .anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Result set must contain the discriminator remark", containsRemark);
    }

    // ----------------------------------------------------------------------
    // 6. walk() behaviour when validation is disabled
    // ----------------------------------------------------------------------
    @Test
    public void testWalkWhenShouldValidateSchemaIsFalseInvokesChildWalksAndReturnsEmptySet() {
        // Two child schemas – we only check that their walk() method is called.
        JsonSchema child1 = mock(JsonSchema.class);
        JsonSchema child2 = mock(JsonSchema.class);
        JsonNode arrayNode = nodeFactory.arrayNode()
                .add(nodeFactory.objectNode())
                .add(nodeFactory.objectNode());

        AnyOfValidator validator = createValidatorWithSchemas(arrayNode,
                Arrays.asList(child1, child2));

        Set<ValidationMessage> result = validator.walk(executionContext,
                nodeFactory.objectNode(), nodeFactory.objectNode(), "$", false);

        // walk should return an empty set.
        assertTrue(result.isEmpty());

        // Verify that each child schema's walk method was invoked exactly once.
        verify(child1).walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), eq("$"), eq(false));
        verify(child2).walk(eq(executionContext), any(JsonNode.class), any(JsonNode.class), eq("$"), eq(false));
    }

    // ----------------------------------------------------------------------
    // 7. preloadJsonSchema() delegates to BaseJsonValidator#preloadJsonSchemas
    // ----------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchemaDelegatesToBaseMethod() throws Exception {
        // Build a validator with a single schema mock.
        JsonSchema child = mock(JsonSchema.class);
        JsonNode arrayNode = nodeFactory.arrayNode().add(nodeFactory.objectNode());

        AnyOfValidator realValidator = createValidatorWithSchemas(arrayNode,
                Collections.singletonList(child));

        // Spy on the concrete validator to intercept the protected method.
        AnyOfValidator spyValidator = Mockito.spy(realValidator);
        // Stub the protected method – we just want to capture the argument.
        doNothing().when(spyValidator).preloadJsonSchemas(anyCollection());

        // Call the public method.
        spyValidator.preloadJsonSchema();

        // Verify that the protected helper received the exact list stored inside.
        verify(spyValidator, times(1)).preloadJsonSchemas(eq(realValidator.schemas));
    }

}
```
###Test END##