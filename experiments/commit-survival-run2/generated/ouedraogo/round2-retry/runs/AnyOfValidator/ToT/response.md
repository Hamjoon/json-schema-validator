###Test START##
```java
/*
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The tests focus on the three public behaviours of the class:
 *
 * 1. {@code validate(...)} – the core validation algorithm.
 * 2. {@code walk(...)}      – delegation to {@code validate} when {@code shouldValidateSchema}
 *                            is {@code true} and a no‑op otherwise.
 * 3. {@code preloadJsonSchema()} – ensures that the internal schemas are pre‑loaded.
 *
 * Because {@code AnyOfValidator} collaborates with many heavy‑weight
 * components from the *networknt* JSON‑Schema library, we mock those collaborators
 * with Mockito.  This isolates the validator logic and makes the tests fast,
 * deterministic and easy to understand.
 *
 * The test class covers:
 *
 * • Normal operation (a matching subschema, a non‑matching subschema, multiple subschemas).
 * • Type‑validator short‑circuiting (union vs concrete types).
 * • OpenAPI‑3 discriminator handling (both the “match found” and “no match” paths).
 * • Walk‑mode delegation.
 * • Pre‑loading of child schemas.
 *
 * All tests run under JUnit‑4 and use the standard `assert*` methods.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.CollectorContext.Scope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    // -------------------------------------------------------------------------
    // Mocks shared by all tests
    // -------------------------------------------------------------------------

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ValidationContext.Config config;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private ValidatorState validatorState;

    @Mock
    private ValidationContext.DiscriminatorContext discriminatorContext;

    @Mock
    private JsonSchema schema0;

    @Mock
    private JsonSchema schema1;

    @Mock
    private JsonSchema schema2;

    @Mock
    private Scope grandParentScope;

    @Mock
    private Scope parentScope;

    @Mock
    private Scope childScope;

    private JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

    private JsonNode dummyNode;
    private JsonNode dummyRoot;

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /**
     * Builds a minimal {@link AnyOfValidator} instance that contains the supplied
     * mock schemas.  The constructor of {@code AnyOfValidator} expects a {@code JsonNode}
     * that represents the "anyOf" array; we therefore build a synthetic array node.
     */
    private AnyOfValidator buildValidator(JsonSchema... schemas) {
        // Build a synthetic anyOf array node – the content is irrelevant because we
        // stub ValidationContext#newSchema() to return the supplied mocks.
        JsonNode anyOfArray = nodeFactory.arrayNode();
        for (int i = 0; i < schemas.length; i++) {
            anyOfArray = anyOfArray.add(nodeFactory.objectNode());
        }

        // Stub the ValidationContext to return our mock schemas in order.
        for (int i = 0; i < schemas.length; i++) {
            when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                    .thenReturn(schemas[i]);
        }

        // Stub the configuration for discriminator handling.
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // Build the validator.
        AnyOfValidator validator = new AnyOfValidator("schemaPath", anyOfArray, mock(JsonSchema.class), validationContext);

        // Injector for the internal discriminator context (null when feature disabled).
        // No need to set it manually – the constructor already did it.

        // Wire the execution context to return our mocked collector.
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(parentScope, childScope);
        when(collectorContext.exitDynamicScope()).thenReturn(parentScope, childScope);
        when(collectorContext.enterDynamicScope()).thenReturn(grandParentScope);
        when(collectorContext.exitDynamicScope()).thenReturn(grandParentScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        return validator;
    }

    // -------------------------------------------------------------------------
    // Common test data
    // -------------------------------------------------------------------------

    @Before
    public void setUp() {
        // A simple JSON node used as the "instance" being validated.
        dummyNode = nodeFactory.objectNode().put("foo", "bar");
        dummyRoot = nodeFactory.objectNode();
    }

    // -------------------------------------------------------------------------
    // 1. Tests for the `validate` method
    // -------------------------------------------------------------------------

    @Test
    public void validate_whenAllSubschemasFail_returnsAllErrors() {
        // Arrange – two schemas that each return a single error.
        ValidationMessage err0 = ValidationMessage.of("anyOf", "0", "type", "type error 0");
        ValidationMessage err1 = ValidationMessage.of("anyOf", "1", "type", "type error 1");

        when(schema0.validate(executionContext, dummyNode, dummyRoot, "at")).thenReturn(Collections.singleton(err0));
        when(schema1.validate(executionContext, dummyNode, dummyRoot, "at")).thenReturn(Collections.singleton(err1));

        AnyOfValidator validator = buildValidator(schema0, schema1);

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyRoot, "at");

        // Assert – both errors should be present.
        assertEquals(2, result.size());
        assertTrue(result.contains(err0));
        assertTrue(result.contains(err1));

        // Verify that the validator merged the dynamic scopes correctly.
        verify(collectorContext, times(2)).enterDynamicScope();
        verify(collectorContext, times(2)).exitDynamicScope();
    }

    @Test
    public void validate_whenOneSubschemaPasses_clearsOtherErrorsAndReturnsEmptySet() {
        // Arrange – first schema fails, second schema succeeds (no errors).
        ValidationMessage err0 = ValidationMessage.of("anyOf", "0", "type", "type error 0");
        when(schema0.validate(executionContext, dummyNode, dummyRoot, "at")).thenReturn(Collections.singleton(err0));
        when(schema1.validate(executionContext, dummyNode, dummyRoot, "at")).thenReturn(Collections.emptySet());

        // The validator must report success because a subschema matched.
        AnyOfValidator validator = buildValidator(schema0, schema1);

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyRoot, "at");

        // Assert – empty result (successful validation) and no lingering errors.
        assertTrue(result.isEmpty());

        // The internal counter `numberOfValidSubSchemas` should be >= 1,
        // which is verified indirectly by the empty result.
        verify(schema0).validate(executionContext, dummyNode, dummyRoot, "at");
        verify(schema1).validate(executionContext, dummyNode, dummyRoot, "at");
    }

    @Test
    public void validate_respectsTypeValidator_andSkipsMismatchingTypes() {
        // Arrange – schema0 has a type validator of STRING, schema1 is a fallback.
        TypeValidator stringTypeValidator = mock(TypeValidator.class);
        when(stringTypeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        when(stringTypeValidator.equalsToSchemaType(dummyNode)).thenReturn(false); // node is OBJECT

        when(schema0.hasTypeValidator()).thenReturn(true);
        when(schema0.getTypeValidator()).thenReturn(stringTypeValidator);
        // When type mismatch occurs, validator adds a type‑error message and continues.
        ValidationMessage typeError = ValidationMessage.of("anyOf", "0", "type", "type mismatch");
        when(schema0.validate(executionContext, dummyNode, dummyRoot, "at")).thenReturn(Collections.singleton(typeError));

        // schema1 has no type validator and succeeds.
        when(schema1.hasTypeValidator()).thenReturn(false);
        when(schema1.validate(executionContext, dummyNode, dummyRoot, "at")).thenReturn(Collections.emptySet());

        AnyOfValidator validator = buildValidator(schema0, schema1);

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyRoot, "at");

        // Assert – validation succeeds because schema1 matches.  The type‑error from schema0
        // must not be present in the final result.
        assertTrue(result.isEmpty());

        // Verify that the type‑validator was consulted.
        verify(stringTypeValidator).equalsToSchemaType(dummyNode);
        verify(schema0, never()).validate(executionContext, dummyNode, dummyRoot, "at"); // short‑circuit
    }

    @Test
    public void validate_whenOpenAPI3DiscriminatorMatchFound_andSubschemaHasErrors_returnsErrorsPlusRemark() {
        // Enable discriminator handling.
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        when(validationContext.enterDiscriminatorContext(any(), anyString())).thenReturn(null);
        when(validationContext.leaveDiscriminatorContextImmediately(anyString())).thenReturn(null);
        when(validationContext.getConfig()).thenReturn(config);

        // The discriminator context reports a match.
        when(discriminatorContext.isDiscriminatorMatchFound()).thenReturn(true);
        // The validator creates a new DiscriminatorContext internally; we need to
        // replace it with our mock.
        // Unfortunately the field is private, so we resort to reflection.
        AnyOfValidator validator = buildValidator(schema0);
        try {
            java.lang.reflect.Field field = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            field.setAccessible(true);
            field.set(validator, discriminatorContext);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // Schema returns a single error.
        ValidationMessage subError = ValidationMessage.of("anyOf", "0", "type", "subschema error");
        when(schema0.validate(executionContext, dummyNode, dummyRoot, "at")).thenReturn(Collections.singleton(subError));

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyRoot, "at");

        // Assert – result must contain the original sub‑error and the discriminator remark.
        assertEquals(2, result.size());
        assertTrue(result.contains(subError));
        boolean hasRemark = result.stream().anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Result should contain discriminator remark", hasRemark);
    }

    @Test
    public void validate_whenOpenAPI3DiscriminatorActiveButNoMatch_returnsSpecificError() {
        // Enable discriminator handling.
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        when(validationContext.enterDiscriminatorContext(any(), anyString())).thenReturn(null);
        when(validationContext.leaveDiscriminatorContextImmediately(anyString())).thenReturn(null);
        when(validationContext.getConfig()).thenReturn(config);

        // No discriminator match is found.
        when(discriminatorContext.isActive()).thenReturn(true);
        when(discriminatorContext.isDiscriminatorMatchFound()).thenReturn(false);

        AnyOfValidator validator = buildValidator(schema0);
        try {
            java.lang.reflect.Field field = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            field.setAccessible(true);
            field.set(validator, discriminatorContext);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // The subschema fails (any error is fine).
        ValidationMessage subError = ValidationMessage.of("anyOf", "0", "type", "subschema error");
        when(schema0.validate(executionContext, dummyNode, dummyRoot, "at")).thenReturn(Collections.singleton(subError));

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode, dummyRoot, "at");

        // Assert – we should get the generic discriminator‑failure message.
        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue(msg.getMessage().contains("based on the provided discriminator"));
    }

    // -------------------------------------------------------------------------
    // 2. Tests for the `walk` method
    // -------------------------------------------------------------------------

    @Test
    public void walk_whenShouldValidateSchemaTrue_delegatesToValidate() {
        // Arrange – validator with a single mock schema that succeeds.
        when(schema0.validate(executionContext, dummyNode, dummyRoot, "at")).thenReturn(Collections.emptySet());

        AnyOfValidator validator = buildValidator(schema0);

        // Spy on the validator to verify delegation.
        AnyOfValidator spyValidator = spy(validator);

        // Act
        Set<ValidationMessage> result = spyValidator.walk(executionContext, dummyNode, dummyRoot, "at", true);

        // Assert – the result is the same as validate() and validate() was invoked.
        assertTrue(result.isEmpty());
        verify(spyValidator).validate(executionContext, dummyNode, dummyRoot, "at");
    }

    @Test
    public void walk_whenShouldValidateSchemaFalse_callsWalkOnChildSchemasAndReturnsEmpty() {
        // Arrange – schemas that would return errors if called, but we expect them not to.
        when(schema0.walk(executionContext, dummyNode, dummyRoot, "at", false)).thenReturn(Collections.singleton(ValidationMessage.of("anyOf", "0", "type", "error")));
        when(schema1.walk(executionContext, dummyNode, dummyRoot, "at", false)).thenReturn(Collections.emptySet());

        AnyOfValidator validator = buildValidator(schema0, schema1);

        // Act
        Set<ValidationMessage> result = validator.walk(executionContext, dummyNode, dummyRoot, "at", false);

        // Assert – walk returns an empty set regardless of child errors.
        assertTrue(result.isEmpty());

        // Verify that each child schema's walk(.., false) was invoked.
        verify(schema0).walk(executionContext, dummyNode, dummyRoot, "at", false);
        verify(schema1).walk(executionContext, dummyNode, dummyRoot, "at", false);
    }

    // -------------------------------------------------------------------------
    // 3. Tests for the `preloadJsonSchema` method
    // -------------------------------------------------------------------------

    @Test
    public void preloadJsonSchema_invokesPreloadOnAllChildSchemas() {
        // Arrange – the child schemas are mocks; we only need to verify the call.
        AnyOfValidator validator = buildValidator(schema0, schema1, schema2);

        // Act
        validator.preloadJsonSchema();

        // Assert – each child schema's preloadJsonSchemas method should be called.
        // The helper method is defined in BaseJsonValidator; we verify via the
        // protected method `preloadJsonSchemas(List<JsonSchema>)` using a spy.
        AnyOfValidator spy = spy(validator);
        doNothing().when(spy).preloadJsonSchemas(anyList());

        spy.preloadJsonSchema();

        // Capture the list passed to preloadJsonSchemas.
        ArgumentCaptor<List<JsonSchema>> captor = ArgumentCaptor.forClass(List.class);
        verify(spy).preloadJsonSchemas(captor.capture());

        List<JsonSchema> passed = captor.getValue();
        assertEquals(3, passed.size());
        assertTrue(passed.containsAll(Arrays.asList(schema0, schema1, schema2)));
    }
}
```
###Test END##