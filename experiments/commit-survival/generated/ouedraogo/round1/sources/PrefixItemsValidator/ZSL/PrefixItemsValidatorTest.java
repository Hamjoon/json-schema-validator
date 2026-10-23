/*
 * License: Apache-2.0
 * This test class provides comprehensive JUnit‑4 tests for
 * com.networknt.schema.PrefixItemsValidator.
 *
 * The tests cover:
 *  1. Constructor validation (valid & invalid schemaNode).
 *  2. The {@code validate} method – array handling, evaluated‑items collection,
 *     and error aggregation.
 *  3. The {@code walk} method – default value injection and listener invocation.
 *
 * Mockito is used to stub all collaborators (JsonSchema, ValidationContext,
 * ExecutionContext, CollectorContext, Config, ApplyDefaultsStrategy, etc.).
 */

package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * JUnit‑4 test suite for {@link PrefixItemsValidator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PrefixItemsValidatorTest {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private Config config;

    @Mock
    private ApplyDefaultsStrategy applyDefaultsStrategy;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private JsonSchema parentSchema;

    @Before
    public void setUp() {
        // ValidationContext returns a Config and can create JsonSchema instances.
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
        when(validationContext.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));

        // ExecutionContext provides a CollectorContext.
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.getEvaluatedItems()).thenReturn(new LinkedHashSet<>());

        // Default strategy – will be overridden in specific tests when needed.
        when(validationContext.getApplyDefaultsStrategy()).thenReturn(applyDefaultsStrategy);
    }

    /**
     * Helper to build a simple JsonNode array: [ {"type":"string"}, {"type":"integer"} ]
     */
    private ArrayNode buildTupleSchemaArray() {
        ObjectNode stringSchema = JsonNodeFactory.instance.objectNode().put("type", "string");
        ObjectNode integerSchema = JsonNodeFactory.instance.objectNode().put("type", "integer");
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        array.add(stringSchema);
        array.add(integerSchema);
        return array;
    }

    /**
     * Helper to create a mock JsonSchema that returns the supplied validation result.
     */
    private JsonSchema mockJsonSchema(Set<ValidationMessage> result) {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.validate(any(), any(), any(), anyString())).thenReturn(result);
        when(schema.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());
        when(schema.getSchemaNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        return schema;
    }

    @Test
    public void testConstructorWithValidNonEmptyArrayNode() {
        // Arrange
        ArrayNode schemaNode = buildTupleSchemaArray();
        // ValidationContext must create a JsonSchema for each tuple entry.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenAnswer(invocation -> mockJsonSchema(Collections.emptySet()));

        // Act
        PrefixItemsValidator validator = new PrefixItemsValidator(
                "#/prefixItems", schemaNode, parentSchema, validationContext);

        // Assert
        assertNotNull(validator);
        List<JsonSchema> tupleSchemas = validator.getTupleSchema();
        assertEquals(2, tupleSchemas.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWhenSchemaNodeIsNotArray() {
        // Arrange: a simple object node instead of an array.
        ObjectNode notArrayNode = JsonNodeFactory.instance.objectNode().put("type", "string");

        // Act
        new PrefixItemsValidator("#/prefixItems", notArrayNode, parentSchema, validationContext);
        // Expect IllegalArgumentException
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorThrowsWhenArrayNodeIsEmpty() {
        // Arrange: empty array node.
        ArrayNode emptyArray = JsonNodeFactory.instance.arrayNode();

        // Act
        new PrefixItemsValidator("#/prefixItems", emptyArray, parentSchema, validationContext);
        // Expect IllegalArgumentException
    }

    @Test
    public void testValidateAggregatesErrorsAndTracksEvaluatedItems() {
        // Arrange
        ArrayNode schemaNode = buildTupleSchemaArray();

        // First tuple schema validates successfully, second returns an error.
        JsonSchema firstSchema = mockJsonSchema(Collections.emptySet());

        ValidationMessage errorMsg = ValidationMessage.builder()
                .code("type")
                .path("#/0")
                .message("type mismatch")
                .build();
        JsonSchema secondSchema = mockJsonSchema(Collections.singleton(errorMsg));

        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(firstSchema)
                .thenReturn(secondSchema);

        PrefixItemsValidator validator = new PrefixItemsValidator(
                "#/prefixItems", schemaNode, parentSchema, validationContext);

        // Input JSON array: ["hello", 123]
        ArrayNode instance = JsonNodeFactory.instance.arrayNode()
                .add("hello")
                .add(123);

        // Act
        Set<ValidationMessage> errors = validator.validate(executionContext, instance,
                instance, "#");

        // Assert
        // One error from second schema
        assertEquals(1, errors.size());
        assertTrue(errors.contains(errorMsg));

        // Evaluated items should contain the successful first index only.
        Set<String> evaluated = collectorContext.getEvaluatedItems();
        assertEquals(1, evaluated.size());
        assertTrue(evaluated.contains("#/0"));
    }

    @Test
    public void testValidateIgnoresNonArrayNode() {
        // Arrange
        ArrayNode schemaNode = buildTupleSchemaArray();
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(mockJsonSchema(Collections.emptySet()))
                .thenReturn(mockJsonSchema(Collections.emptySet()));

        PrefixItemsValidator validator = new PrefixItemsValidator(
                "#/prefixItems", schemaNode, parentSchema, validationContext);

        // Non‑array node (object)
        ObjectNode nonArray = JsonNodeFactory.instance.objectNode().put("foo", "bar");

        // Act
        Set<ValidationMessage> errors = validator.validate(executionContext, nonArray,
                nonArray, "#");

        // Assert
        assertTrue(errors.isEmpty());
        // No evaluated items should be added.
        assertTrue(collectorContext.getEvaluatedItems().isEmpty());
    }

    @Test
    public void testWalkAppliesDefaultValuesWhenNodeIsNullAndStrategyAllows() throws Exception {
        // Arrange
        // Tuple schema where the first element has a default value "defaultString".
        ObjectNode firstSchemaNode = JsonNodeFactory.instance.objectNode()
                .put("type", "string")
                .set("default", JsonNodeFactory.instance.textNode("defaultString"));
        ObjectNode secondSchemaNode = JsonNodeFactory.instance.objectNode()
                .put("type", "integer"); // no default

        ArrayNode tupleSchemaArray = JsonNodeFactory.instance.arrayNode()
                .add(firstSchemaNode)
                .add(secondSchemaNode);

        // Mock JsonSchema instances; they need to expose getSchemaNode().
        JsonSchema firstSchema = mock(JsonSchema.class);
        JsonSchema secondSchema = mock(JsonSchema.class);
        when(firstSchema.getSchemaNode()).thenReturn(firstSchemaNode);
        when(secondSchema.getSchemaNode()).thenReturn(secondSchemaNode);
        // Walk should return empty set (no validation errors).
        when(firstSchema.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());
        when(secondSchema.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        // ValidationContext creates the above schemas.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(firstSchema)
                .thenReturn(secondSchema);

        PrefixItemsValidator validator = new PrefixItemsValidator(
                "#/prefixItems", tupleSchemaArray, parentSchema, validationContext);

        // Force the defaults strategy to return true.
        when(applyDefaultsStrategy.shouldApplyArrayDefaults()).thenReturn(true);
        // Inject the strategy via reflection (field is defined in BaseJsonValidator).
        java.lang.reflect.Field strategyField = BaseJsonValidator.class
                .getDeclaredField("applyDefaultsStrategy");
        strategyField.setAccessible(true);
        strategyField.set(validator, applyDefaultsStrategy);

        // Instance array: [null, 42]
        ArrayNode instance = JsonNodeFactory.instance.arrayNode()
                .addNull()
                .add(42);

        // Act
        Set<ValidationMessage> messages = validator.walk(executionContext, instance,
                instance, "#", false);

        // Assert
        // No validation messages expected.
        assertTrue(messages.isEmpty());

        // The first element should have been replaced by the default value.
        assertEquals("defaultString", instance.get(0).asText());

        // Verify that walk was invoked on both tuple schemas.
        verify(firstSchema).walk(eq(executionContext), eq(instance.get(0)),
                eq(instance), eq("#/0"), eq(false));
        verify(secondSchema).walk(eq(executionContext), eq(instance.get(1)),
                eq(instance), eq("#/1"), eq(false));
    }
}
