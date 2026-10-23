###Test START##
```java
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.ValidationContext.Config;
import com.networknt.schema.ValidatorState;
import com.networknt.schema.CollectorContext.Scope;
import com.networknt.schema.ValidatorTypeCode;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

/**
 * Unit tests for {@link AnyOfValidator}.
 *
 * <p>The tests use Mockito to mock the heavy weight infrastructure that
 * {@code AnyOfValidator} depends on (validation context, execution context,
 * collector context, {@link JsonSchema} instances, etc.).  The goal is to
 * exercise every logical branch inside {@code validate()}, {@code walk()}
 * and {@code preloadJsonSchema()} without needing the full schema engine.</p>
 */
public class AnyOfValidatorTest {

    /** Common objects used by many test cases. */
    private JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
    private JsonNode dummyArrayNode;           // the "anyOf" schema node (an array)
    private JsonSchema parentSchema;           // parent schema – not used directly
    private ValidationContext validationContext;
    private Config config;
    private ExecutionContext executionContext;
    private CollectorContext collectorContext;
    private ValidatorState validatorState;
    private Scope grandParentScope;
    private Scope parentScope;
    private Scope childScope;

    @Before
    public void setUp() {
        // a simple empty array node – the validator will replace each element with a mocked schema
        dummyArrayNode = nodeFactory.arrayNode();

        // parent schema can be a simple mock – it is never inspected by the tests
        parentSchema = mock(JsonSchema.class);

        // ----- ValidationContext & its Config -----
        validationContext = mock(ValidationContext.class);
        config = mock(Config.class);
        when(validationContext.getConfig()).thenReturn(config);

        // ----- ExecutionContext & CollectorContext -----
        executionContext = mock(ExecutionContext.class);
        collectorContext = mock(CollectorContext.class);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // ----- Scopes (dynamic collection scopes) -----
        grandParentScope = mock(Scope.class);
        parentScope = mock(Scope.class);
        childScope = mock(Scope.class);
        when(collectorContext.enterDynamicScope())
                .thenReturn(grandParentScope)   // first call – grand‑parent
                .thenReturn(parentScope)       // second call – per‑schema parent
                .thenReturn(childScope);       // third call – child (won’t be used but kept for completeness)

        when(collectorContext.exitDynamicScope())
                .thenReturn(parentScope)       // exit child → returns its parent
                .thenReturn(grandParentScope); // exit parent → returns grand‑parent

        // ----- ValidatorState -----
        validatorState = mock(ValidatorState.class);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(false);
    }

    /**
     * Helper to build an {@link AnyOfValidator} with a given number of child schemas.
     * The returned validator will have {@code config.isOpenAPI3StyleDiscriminators()}
     * set to the supplied value.
     *
     * @param schemaCount      number of child schemas to create
     * @param discriminatorOn true to enable OpenAPI‑3 style discriminators
     * @return a fully‑initialised {@code AnyOfValidator}
     */
    private AnyOfValidator createValidator(int schemaCount, boolean discriminatorOn) {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(discriminatorOn);

        // Build a JSON array node with the requested number of dummy elements.
        dummyArrayNode = nodeFactory.arrayNode();
        for (int i = 0; i < schemaCount; i++) {
            dummyArrayNode = dummyArrayNode.add(nodeFactory.objectNode());
        }

        // Mock the creation of child schemas – each call to newSchema returns a fresh mock.
        List<JsonSchema> mockedSchemas = new ArrayList<>();
        for (int i = 0; i < schemaCount; i++) {
            JsonSchema mocked = mock(JsonSchema.class);
            mockedSchemas.add(mocked);
            when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                    .thenReturn(mocked);
        }

        // Finally create the validator.
        AnyOfValidator validator = new AnyOfValidator(
                "anyOfPath",
                dummyArrayNode,
                parentSchema,
                validationContext);

        // Replace the internal list with the mocks we created (the constructor already added them,
        // but we want to guarantee order and content).
        // The field is private; we use reflection to set it.
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            f.set(validator, mockedSchemas);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return validator;
    }

    /**
     * Test the branch where a child schema has a {@link TypeValidator}
     * whose type does not match the instance node.  The validator must add a
     * type‑mismatch {@link ValidationMessage} to the error set and then
     * continue with the next subschema.
     */
    @Test
    public void testValidate_TypeMismatchAddsErrorAndContinues() {
        // Build a validator with two child schemas.
        AnyOfValidator validator = createValidator(2, false);

        // ---- First schema – type mismatch ----
        JsonSchema schema1 = validator.schemas.get(0);
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(schema1.hasTypeValidator()).thenReturn(true);
        when(schema1.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING); // not UNION
        when(typeValidator.equalsToSchemaType(any(JsonNode.class))).thenReturn(false);
        when(typeValidator.getSchemaType().toString()).thenReturn("string");

        // The validator will never call validate()/walk() on this schema because we
        // short‑circuit on the type mismatch, but we stub them just in case.
        when(schema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        // ---- Second schema – passes validation ----
        JsonSchema schema2 = validator.schemas.get(1);
        when(schema2.hasTypeValidator()).thenReturn(false);
        when(schema2.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        // The node to validate – any non‑null node works.
        JsonNode instanceNode = nodeFactory.textNode("any value");

        // Execute validation.
        Set<ValidationMessage> result = validator.validate(
                executionContext,
                instanceNode,
                instanceNode,          // root node (same as instance for this test)
                "$");                  // location

        // Because the second schema succeeded, the validator should return an empty set.
        assertTrue("Result should be empty when a later subschema validates successfully", result.isEmpty());

        // Verify that the type‑mismatch message was *added* and then cleared.
        // We do this by checking that the second schema's validate() was invoked,
        // which only happens after the type‑check branch was taken.
        verify(schema2).validate(any(), eq(instanceNode), eq(instanceNode), eq("$"));
    }

    /**
     * Test that when a subschema validates without errors and discriminators are
     * disabled, the validator returns an empty set and marks the node as matched.
     */
    @Test
    public void testValidate_ValidSubschemaWithoutDiscriminators() {
        AnyOfValidator validator = createValidator(1, false);

        JsonSchema schema = validator.schemas.get(0);
        when(schema.hasTypeValidator()).thenReturn(false);
        when(schema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        JsonNode instanceNode = nodeFactory.objectNode();

        Set<ValidationMessage> result = validator.validate(
                executionContext,
                instanceNode,
                instanceNode,
                "$");

        // Expect no errors.
        assertTrue("Validator should return no errors when a subschema validates cleanly", result.isEmpty());

        // The finally‑block should have called state.setMatchedNode(true).
        verify(validatorState).setMatchedNode(true);
    }

    /**
     * Verify that {@link AnyOfValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}
     * delegates to {@code validate()} when the {@code shouldValidateSchema} flag is {@code true}.
     */
    @Test
    public void testWalk_DelegatesToValidateWhenFlagTrue() {
        AnyOfValidator realValidator = createValidator(0, false);
        AnyOfValidator validatorSpy = Mockito.spy(realValidator);

        JsonNode node = nodeFactory.objectNode();

        // When walk is called with shouldValidateSchema=true, validate() must be invoked.
        validatorSpy.walk(executionContext, node, node, "$", true);
        verify(validatorSpy).validate(executionContext, node, node, "$");
    }

    /**
     * Verify that {@link AnyOfValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}
     * walks each child schema with {@code shouldValidateSchema=false} when the flag is {@code false}.
     */
    @Test
    public void testWalk_WalksChildrenWhenFlagFalse() {
        AnyOfValidator validator = createValidator(2, false);

        JsonSchema schema1 = validator.schemas.get(0);
        JsonSchema schema2 = validator.schemas.get(1);

        when(schema1.walk(any(), any(), any(), any(), eq(false)))
                .thenReturn(Collections.emptySet());
        when(schema2.walk(any(), any(), any(), any(), eq(false)))
                .thenReturn(Collections.emptySet());

        JsonNode node = nodeFactory.objectNode();

        Set<ValidationMessage> result = validator.walk(executionContext, node, node, "$", false);

        // walk() should return an empty set when shouldValidateSchema is false.
        assertTrue(result.isEmpty());

        // Verify that each child schema's walk() was called with shouldValidateSchema = false.
        verify(schema1).walk(executionContext, node, node, "$", false);
        verify(schema2).walk(executionContext, node, node, "$", false);
    }

    /**
     * Ensure that {@link AnyOfValidator#preloadJsonSchema()} forwards the call to
     * {@code BaseJsonValidator#preloadJsonSchemas(List)}.  Because {@code preloadJsonSchemas}
     * is protected, we create a concrete subclass that overrides it and records the invocation.
     */
    @Test
    public void testPreloadJsonSchema_InvokesPreloadJsonSchemas() {
        // Subclass that records whether preloadJsonSchemas(List) was called.
        class TestableAnyOfValidator extends AnyOfValidator {
            boolean called = false;
            TestableAnyOfValidator(String schemaPath, JsonNode schemaNode,
                                   JsonSchema parentSchema, ValidationContext validationContext) {
                super(schemaPath, schemaNode, parentSchema, validationContext);
            }
            @Override
            protected void preloadJsonSchemas(List<JsonSchema> schemas) {
                called = true; // record the call
                // delegate to super just to keep behaviour identical (no‑op for our test).
                super.preloadJsonSchemas(schemas);
            }
        }

        // Build a validator with a single child schema.
        JsonNode arrayNode = nodeFactory.arrayNode().add(nodeFactory.objectNode());
        TestableAnyOfValidator validator = new TestableAnyOfValidator(
                "anyOfPath",
                arrayNode,
                parentSchema,
                validationContext);

        // Invoke the method under test.
        validator.preloadJsonSchema();

        // Verify that the overridden method was hit.
        assertTrue("preloadJsonSchemas should have been called from preloadJsonSchema()", validator.called);
    }
}
```
###Test END##