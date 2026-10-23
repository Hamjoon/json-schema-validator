###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The tests use Mockito to isolate {@code AnyOfValidator} from the rest of the
 * validation framework and to verify the complex control flow inside the
 * {@code validate} and {@code walk} methods.
 *
 * The test cases cover:
 * <ul>
 *   <li>Type‑validator mismatch handling.</li>
 *   <li>Successful validation of a subschema (early return of an empty set).</li>
 *   <li>OpenAPI‑style discriminator handling when a discriminator match is found
 *       (both with and without validation errors).</li>
 *   <li>OpenAPI‑style discriminator handling when no discriminator match can be
 *       chosen (error message about the discriminator).</li>
 *   <li>The {@code walk} method when {@code shouldValidateSchema} is {@code true}
 *       and {@code false}.</li>
 *   <li>The {@code preloadJsonSchema} method delegating to the contained schemas.</li>
 * </ul>
 *
 * The test class is deliberately self‑contained – it creates a minimal
 * {@code JsonNode} structure required by the constructor and uses reflection
 * to inject a mocked {@code DiscriminatorContext} where needed.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for {@link AnyOfValidator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    // -----------------------------------------------------------------------
    // Mocks used by every test case
    // -----------------------------------------------------------------------
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
    private Scope mockScope; // used for both parent and grand‑parent scopes

    // -----------------------------------------------------------------------
    // Helper to build a validator with a given number of subschemas
    // -----------------------------------------------------------------------
    private AnyOfValidator buildValidator(int subschemaCount) {
        // an array node with {@code subschemaCount} empty objects
        ArrayNode schemaNode = JsonNodeFactory.instance.arrayNode();
        for (int i = 0; i < subschemaCount; i++) {
            schemaNode.add(JsonNodeFactory.instance.objectNode());
        }

        // The ValidationContext will create a mock JsonSchema for each subschema
        List<JsonSchema> createdSchemas = new ArrayList<>();
        for (int i = 0; i < subschemaCount; i++) {
            JsonSchema mockSchema = mock(JsonSchema.class);
            createdSchemas.add(mockSchema);
            when(validationContext.newSchema(anyString(), any(JsonNode.class), isNull()))
                    .thenReturn(mockSchema);
        }

        // Build the validator under test
        AnyOfValidator validator = new AnyOfValidator(
                "anyOf", schemaNode, null, validationContext);

        // Wire the execution/collector context used during validation
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY))
                .thenReturn(validatorState);
        when(collectorContext.enterDynamicScope()).thenReturn(mockScope);
        when(collectorContext.exitDynamicScope()).thenReturn(mockScope);

        // The config is obtained from the validation context
        when(validationContext.getConfig()).thenReturn(config);

        // Store the created mock schemas for later verification
        validator.getClass().getDeclaredFields(); // just to keep the compiler happy
        return validator;
    }

    // -----------------------------------------------------------------------
    // Common stubbing for a validator state that reports “walk disabled”
    // -----------------------------------------------------------------------
    private void stubStateForWalkDisabled() {
        when(validatorState.isWalkEnabled()).thenReturn(false);
        // In most tests we want the state to appear as “matched” after a
        // successful validation so that AnyOf counts the subschema as valid.
        when(validatorState.hasMatchedNode()).thenReturn(true);
    }

    // -----------------------------------------------------------------------
    // Test 1 – type validator mismatch adds a type‑error message
    // -----------------------------------------------------------------------
    @Test
    public void testValidate_TypeValidatorMismatch_ShouldAddTypeError() {
        // ---------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------
        AnyOfValidator validator = buildValidator(1);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        JsonSchema schema = validator.getClass()
                .getDeclaredFields()[0] // hack to obtain the first mock schema
                .getDeclaringClass()
                .cast(validator)
                .schemas.get(0);
        // Mock a TypeValidator that does NOT match the node type
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        when(typeValidator.equalsToSchemaType(any(JsonNode.class))).thenReturn(false);
        when(schema.hasTypeValidator()).thenReturn(true);
        when(schema.getTypeValidator()).thenReturn(typeValidator);

        // The validator should never call validate() on the schema because
        // the type mismatch short‑circuits the loop.
        when(schema.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        // The node we validate – the actual content does not matter
        JsonNode node = JsonNodeFactory.instance.textNode("any text");

        stubStateForWalkDisabled();

        // ---------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // ---------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------
        // Exactly one error (the type‑mismatch) should be reported.
        assertEquals(1, result.size());
        ValidationMessage message = result.iterator().next();
        // The message should contain the expected schema type name.
        assertTrue(message.getMessage().toLowerCase().contains("string"));
    }

    // -----------------------------------------------------------------------
    // Test 2 – a valid subschema results in an empty error set (early return)
    // -----------------------------------------------------------------------
    @Test
    public void testValidate_ValidSubschema_ShouldReturnEmptySet() {
        // ---------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------
        AnyOfValidator validator = buildValidator(1);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        JsonSchema schema = validator.getClass()
                .getDeclaredFields()[0]
                .getDeclaringClass()
                .cast(validator)
                .schemas.get(0);

        // No type validator – validation proceeds straight to schema.validate()
        when(schema.hasTypeValidator()).thenReturn(false);
        when(schema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        JsonNode node = JsonNodeFactory.instance.objectNode();

        stubStateForWalkDisabled();

        // ---------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // ---------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------
        // The validator should have returned the empty set received from the
        // subschema, i.e. no validation errors.
        assertTrue(result.isEmpty());

        // Verify that the validator cleared any accumulated errors internally.
        // (We cannot inspect the internal set directly, but we can verify that
        // the schema.validate() method was called exactly once.)
        verify(schema, times(1)).validate(any(), any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // Test 3 – OpenAPI discriminator match found with validation errors
    // -----------------------------------------------------------------------
    @Test
    public void testValidate_DiscriminatorMatchFound_WithErrors() throws Exception {
        // ---------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------
        AnyOfValidator validator = buildValidator(1);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        // Replace the real discriminator context with a mock
        ValidationContext.DiscriminatorContext mockDiscriminatorContext =
                mock(ValidationContext.DiscriminatorContext.class);
        when(mockDiscriminatorContext.isDiscriminatorMatchFound()).thenReturn(true);
        // make the internal final field point to our mock (reflection)
        java.lang.reflect.Field discField = AnyOfValidator.class
                .getDeclaredField("discriminatorContext");
        discField.setAccessible(true);
        discField.set(validator, mockDiscriminatorContext);

        JsonSchema schema = validator.getClass()
                .getDeclaredFields()[0]
                .getDeclaringClass()
                .cast(validator)
                .schemas.get(0);
        when(schema.hasTypeValidator()).thenReturn(false);
        ValidationMessage subError = ValidationMessage.builder()
                .type("anyOf")
                .path("$")
                .message("subschema error")
                .build();
        when(schema.validate(any(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(subError)));

        JsonNode node = JsonNodeFactory.instance.objectNode();

        stubStateForWalkDisabled();

        // ---------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // ---------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------
        // Because a discriminator match was found, the validator returns the
        // original subschema errors (without the extra discriminator remark).
        assertEquals(1, result.size());
        assertTrue(result.contains(subError));

        // Verify that the discriminator context was queried.
        verify(mockDiscriminatorContext, times(1)).isDiscriminatorMatchFound();
    }

    // -----------------------------------------------------------------------
    // Test 4 – OpenAPI discriminator active but no match; final discriminator error
    // -----------------------------------------------------------------------
    @Test
    public void testValidate_DiscriminatorActive_NoMatch_ShouldReturnDiscriminatorError() throws Exception {
        // ---------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------
        AnyOfValidator validator = buildValidator(1);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        // Inject a mock discriminator context that reports “active” but
        // no match has been found.
        ValidationContext.DiscriminatorContext mockDiscriminatorContext =
                mock(ValidationContext.DiscriminatorContext.class);
        when(mockDiscriminatorContext.isActive()).thenReturn(true);
        when(mockDiscriminatorContext.isDiscriminatorMatchFound()).thenReturn(false);
        java.lang.reflect.Field discField = AnyOfValidator.class
                .getDeclaredField("discriminatorContext");
        discField.setAccessible(true);
        discField.set(validator, mockDiscriminatorContext);

        JsonSchema schema = validator.getClass()
                .getDeclaredFields()[0]
                .getDeclaringClass()
                .cast(validator)
                .schemas.get(0);
        when(schema.hasTypeValidator()).thenReturn(false);
        when(schema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        JsonNode node = JsonNodeFactory.instance.objectNode();
        stubStateForWalkDisabled();

        // ---------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // ---------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------
        // The validator should return a single message that mentions the
        // discriminator.
        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue(msg.getMessage()
                .contains("based on the provided discriminator"));
    }

    // -----------------------------------------------------------------------
    // Test 5 – walk() with shouldValidateSchema == true delegates to validate()
    // -----------------------------------------------------------------------
    @Test
    public void testWalk_ShouldValidateWhenFlagTrue() {
        // ---------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------
        AnyOfValidator validator = buildValidator(1);
        AnyOfValidator spyValidator = spy(validator);
        Set<ValidationMessage> fakeResult = new LinkedHashSet<>();
        doReturn(fakeResult).when(spyValidator)
                .validate(any(), any(), any(), anyString());

        JsonNode node = JsonNodeFactory.instance.objectNode();

        // ---------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------
        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, node, "$", true);

        // ---------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------
        assertSame(fakeResult, result);
        verify(spyValidator, times(1))
                .validate(executionContext, node, node, "$");
    }

    // -----------------------------------------------------------------------
    // Test 6 – walk() with shouldValidateSchema == false walks each subschema
    // -----------------------------------------------------------------------
    @Test
    public void testWalk_ShouldNotValidateWhenFlagFalse() {
        // ---------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------
        AnyOfValidator validator = buildValidator(2);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        JsonSchema schema1 = validator.getClass()
                .getDeclaredFields()[0]
                .getDeclaringClass()
                .cast(validator)
                .schemas.get(0);
        JsonSchema schema2 = validator.getClass()
                .getDeclaredFields()[0]
                .getDeclaringClass()
                .cast(validator)
                .schemas.get(1);

        // The walk method of each subschema should be called with shouldValidate = false
        when(schema1.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(new LinkedHashSet<>());
        when(schema2.walk(any(), any(), any(), anyString(), eq(false))).thenReturn(new LinkedHashSet<>());

        JsonNode node = JsonNodeFactory.instance.objectNode();

        // ---------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------
        Set<ValidationMessage> result = validator.walk(executionContext, node, node, "$", false);

        // ---------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------
        // The result of walk() when validation is disabled is always empty.
        assertTrue(result.isEmpty());
        verify(schema1, times(1)).walk(executionContext, node, node, "$", false);
        verify(schema2, times(1)).walk(executionContext, node, node, "$", false);
    }

    // -----------------------------------------------------------------------
    // Test 7 – preloadJsonSchema() should invoke preloadJsonSchema() on each child schema
    // -----------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchema_DelegatesToChildSchemas() {
        // ---------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------
        AnyOfValidator validator = buildValidator(3);
        JsonSchema schema1 = validator.getClass()
                .getDeclaredFields()[0]
                .getDeclaringClass()
                .cast(validator)
                .schemas.get(0);
        JsonSchema schema2 = validator.getClass()
                .getDeclaredFields()[0]
                .getDeclaringClass()
                .cast(validator)
                .schemas.get(1);
        JsonSchema schema3 = validator.getClass()
                .getDeclaredFields()[0]
                .getDeclaringClass()
                .cast(validator)
                .schemas.get(2);

        // ---------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------
        validator.preloadJsonSchema();

        // ---------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------
        verify(schema1, times(1)).preloadJsonSchema();
        verify(schema2, times(1)).preloadJsonSchema();
        verify(schema3, times(1)).preloadJsonSchema();
    }
}
```
###Test END##