###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * JUnit‑4 test suite for {@link AnyOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public method signatures</li>
 *   <li>Normal execution paths</li>
 *   <li>Edge‑case handling (empty schema list, type‑mismatch, discriminator mode, etc.)</li>
 *   <li>Exception / error handling paths</li>
 * </ul>
 *
 * All tests use Mockito to stub the heavy‑weight collaborators
 * ({@link ValidationContext}, {@link ExecutionContext}, {@link CollectorContext},
 * {@link JsonSchema}, {@link TypeValidator}, {@link ValidationMessage}, …).
 */
public class AnyOfValidatorTest {

    /** The object under test – created anew for each test method. */
    private AnyOfValidator validator;

    /** Mocks shared by many tests. */
    private ValidationContext validationContext;
    private ValidationContext.Config config;
    private ExecutionContext executionContext;
    private CollectorContext collectorContext;
    private CollectorContext.Scope grandParentScope;
    private CollectorContext.Scope parentScope;
    private ValidatorState validatorState;
    private JsonNode schemaNode;          // the "anyOf" array node
    private JsonNode dummyNode;           // node that will be validated
    private JsonNode rootNode;            // root of the document (unused in these tests)
    private String at = "$";

    @Before
    public void setUp() {
        // ----- generic mocks -------------------------------------------------
        validationContext = mock(ValidationContext.class);
        config = mock(ValidationContext.Config.class);
        when(validationContext.getConfig()).thenReturn(config);
        // By default, OpenAPI discriminator handling is turned off
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        executionContext = mock(ExecutionContext.class);
        collectorContext = mock(CollectorContext.class);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // ----- collector‑scope mocks (dynamic scopes are just placeholders) ----
        grandParentScope = mock(CollectorContext.Scope.class);
        parentScope = mock(CollectorContext.Scope.class);
        when(collectorContext.enterDynamicScope()).thenReturn(grandParentScope, parentScope);
        when(collectorContext.exitDynamicScope()).thenReturn(parentScope, grandParentScope);

        // ----- validator state ------------------------------------------------
        validatorState = mock(ValidatorState.class);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // ----- JSON nodes ----------------------------------------------------
        schemaNode = mock(JsonNode.class);   // the "anyOf" array node
        dummyNode = mock(JsonNode.class);
        rootNode = mock(JsonNode.class);
    }

    /**
     * Helper that creates an {@link AnyOfValidator} with a supplied list of
     * {@link JsonSchema} mocks.  The {@code schemas} argument is the list that
     * the validator will iterate over during {@code validate()}.
     */
    private AnyOfValidator createValidatorWithSchemas(List<JsonSchema> schemas) {
        // Mock the behaviour of ValidationContext#newSchema for each array element.
        // The validator will call newSchema(schemaPath + "/" + i, schemaNode.get(i), parentSchema)
        // – we simply return the pre‑constructed mocks irrespective of the arguments.
        for (JsonSchema s : schemas) {
            when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                    .thenReturn(s);
        }

        // The array node must report its size and return a dummy child for each index.
        when(schemaNode.size()).thenReturn(schemas.size());
        for (int i = 0; i < schemas.size(); i++) {
            when(schemaNode.get(i)).thenReturn(mock(JsonNode.class));
        }

        // Finally instantiate the validator.
        return new AnyOfValidator("anyOf", schemaNode, mock(JsonSchema.class), validationContext);
    }

    // -----------------------------------------------------------------------
    // 1.  Public method signatures (extracted for documentation purposes)
    // -----------------------------------------------------------------------
    //    - AnyOfValidator(String schemaPath, JsonNode schemaNode,
    //                     JsonSchema parentSchema, ValidationContext validationContext)
    //    - Set<ValidationMessage> validate(ExecutionContext executionContext,
    //                                      JsonNode node, JsonNode rootNode, String at)
    //    - Set<ValidationMessage> walk(ExecutionContext executionContext,
    //                                 JsonNode node, JsonNode rootNode,
    //                                 String at, boolean shouldValidateSchema)
    //    - void preloadJsonSchema()
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // 2.  Basic functional tests
    // -----------------------------------------------------------------------

    /** AnyOf with a single subschema that validates without errors → empty result. */
    @Test
    public void testValidate_AllValidSubschemas_ReturnsEmptySet() {
        JsonSchema subSchema = mock(JsonSchema.class);
        when(subSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        validator = createValidatorWithSchemas(Collections.singletonList(subSchema));

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, rootNode, at);
        assertTrue("Expected no validation messages when the only subschema passes", result.isEmpty());

        // Verify that the validator marked the node as matched.
        verify(validatorState).setMatchedNode(true);
    }

    /** AnyOf with an empty schema array → validation succeeds (no errors). */
    @Test
    public void testValidate_EmptySchemaArray_ReturnsEmptySet() {
        validator = createValidatorWithSchemas(Collections.emptyList());

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, rootNode, at);
        assertTrue("Empty anyOf should produce no errors", result.isEmpty());
    }

    /** Subschema reports a type‑mismatch → a type‑error message is produced. */
    @Test
    public void testValidate_TypeMismatch_AddsTypeErrorMessage() {
        // ----- mock a TypeValidator that does NOT match the node -----
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING); // not UNION
        when(typeValidator.equalsToSchemaType(dummyNode)).thenReturn(false);
        when(typeValidator.toString()).thenReturn("string");

        // ----- mock a JsonSchema that has the above TypeValidator -----
        JsonSchema subSchema = mock(JsonSchema.class);
        when(subSchema.hasTypeValidator()).thenReturn(true);
        when(subSchema.getTypeValidator()).thenReturn(typeValidator);
        when(subSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        validator = createValidatorWithSchemas(Collections.singletonList(subSchema));

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, rootNode, at);
        assertEquals("Exactly one validation message should be produced for type mismatch", 1, result.size());

        // The message should contain the expected schema type name.
        ValidationMessage vm = result.iterator().next();
        assertTrue("Message type should be the schema type (string)", vm.getMessage().contains("string"));
    }

    /** When discriminator handling is ON and a discriminator match is found,
     *  the validator should return either the subschema errors plus the
     *  discriminator remark or an empty set if the matched subschema is valid. */
    @Test
    public void testValidate_WithOpenAPIDiscriminator_MatchFoundWithErrors() {
        // Enable discriminator mode
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        // Mock a discriminator context that signals a match has been found.
        ValidationContext.DiscriminatorContext discContext = new ValidationContext.DiscriminatorContext();
        discContext.setDiscriminatorMatchFound(true);
        // The validator creates its own context, so we need to spy on the validator
        // after construction to inject the mocked context.
        validator = new AnyOfValidator("anyOf", schemaNode, mock(JsonSchema.class), validationContext);
        // Replace the internally created discriminatorContext with the one we prepared.
        // (Reflection is used because the field is private and final.)
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            f.setAccessible(true);
            f.set(validator, discContext);
        } catch (Exception e) {
            fail("Failed to inject discriminator context: " + e);
        }

        // Subschema returns a non‑empty error set.
        ValidationMessage subError = mock(ValidationMessage.class);
        when(subError.getType()).thenReturn("anyOf");
        when(subError.getMessage()).thenReturn("subschema error");
        JsonSchema subSchema = mock(JsonSchema.class);
        when(subSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(subError));

        // Replace the internal schemas list via reflection.
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            f.set(validator, Collections.singletonList(subSchema));
        } catch (Exception e) {
            fail("Failed to inject schemas list: " + e);
        }

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, rootNode, at);
        // Expected: the original subschema error + the discriminator remark
        assertEquals("Two messages expected (original + discriminator remark)", 2, result.size());

        // Verify that one of the messages contains the discriminator remark constant.
        boolean remarkFound = result.stream()
                .anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Discriminator remark should be present", remarkFound);
    }

    // -----------------------------------------------------------------------
    // 3.  walk() tests
    // -----------------------------------------------------------------------

    /** walk(..., shouldValidateSchema = true) must delegate to validate(). */
    @Test
    public void testWalk_ShouldValidateTrue_DelegatesToValidate() {
        JsonSchema subSchema = mock(JsonSchema.class);
        when(subSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        validator = createValidatorWithSchemas(Collections.singletonList(subSchema));

        // Spy on the validator to verify delegation.
        AnyOfValidator spyValidator = spy(validator);
        doReturn(Collections.emptySet()).when(spyValidator).validate(any(), any(), any(), anyString());

        Set<ValidationMessage> result = spyValidator.walk(executionContext, dummyNode, rootNode, at, true);
        verify(spyValidator, times(1)).validate(executionContext, dummyNode, rootNode, at);
        assertTrue(result.isEmpty());
    }

    /** walk(..., shouldValidateSchema = false) must invoke walk on each subschema
     *  but return an empty set. */
    @Test
    public void testWalk_ShouldValidateFalse_ExecutesSubschemaWalks() {
        JsonSchema subSchema1 = mock(JsonSchema.class);
        JsonSchema subSchema2 = mock(JsonSchema.class);
        validator = createValidatorWithSchemas(Arrays.asList(subSchema1, subSchema2));

        Set<ValidationMessage> result = validator.walk(executionContext, dummyNode, rootNode, at, false);
        // Verify that each subschema's walk method was called with shouldValidate = false.
        verify(subSchema1, times(1)).walk(executionContext, dummyNode, rootNode, at, false);
        verify(subSchema2, times(1)).walk(executionContext, dummyNode, rootNode, at, false);
        assertTrue("walk should return an empty set when shouldValidateSchema is false", result.isEmpty());
    }

    // -----------------------------------------------------------------------
    // 4.  preloadJsonSchema() test – simply ensures no exception is thrown.
    // -----------------------------------------------------------------------

    @Test
    public void testPreloadJsonSchema_DoesNotThrow() {
        JsonSchema subSchema = mock(JsonSchema.class);
        validator = createValidatorWithSchemas(Collections.singletonList(subSchema));

        // The method under test has no return value; we just assert that it completes.
        validator.preloadJsonSchema();

        // Internally it should have called preloadJsonSchemas(List) from BaseJsonValidator.
        // That method is protected – we verify by spying on the validator and checking
        // that the protected method was invoked.
        AnyOfValidator spyValidator = spy(validator);
        spyValidator.preloadJsonSchema();
        verify(spyValidator, times(1)).preloadJsonSchemas(anyList());
    }

    // -----------------------------------------------------------------------
    // 5.  Edge‑case: required‑error filtering when at least one subschema is valid.
    // -----------------------------------------------------------------------

    @Test
    public void testValidate_RequiredErrorsFilteredWhenValidSubschemaExists() {
        // Subschema #1 – returns a "required" error.
        ValidationMessage requiredError = mock(ValidationMessage.class);
        when(requiredError.getType()).thenReturn(ValidatorTypeCode.REQUIRED.getValue());
        when(requiredError.getMessage()).thenReturn("missing required");

        JsonSchema schemaWithRequiredError = mock(JsonSchema.class);
        when(schemaWithRequiredError.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(requiredError));

        // Subschema #2 – returns no errors (valid).
        JsonSchema validSchema = mock(JsonSchema.class);
        when(validSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        validator = createValidatorWithSchemas(Arrays.asList(schemaWithRequiredError, validSchema));

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, rootNode, at);
        // Because a valid subschema exists, required‑type errors should be filtered out.
        assertTrue("Result should be empty after filtering required errors", result.isEmpty());
    }
}
```
###Test END##