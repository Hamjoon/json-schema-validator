package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class PrefixItemsValidator_walk_Test {

    private PrefixItemsValidator validator;

    @Mock
    private JsonSchema parentSchema;

    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaValidatorsConfig config;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setupBeforeEach() {
        // Stub configuration that the validator expects.
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        // The constructor calls validationContext.newSchema(...). Return a dummy schema.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(parentSchema);
        // Build a minimal valid "prefixItems" schema node (an array with a single empty schema object).
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode schemaNode = mapper.createArrayNode();
        // {} – a placeholder schema
        schemaNode.add(mapper.createObjectNode());
        // Create the validator instance under test.
        validator = new PrefixItemsValidator("testPath", schemaNode, parentSchema, validationContext);
    }

    /**
     * Helper to set a private or protected field via reflection.
     */
    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper to create a mock {@link ApplyDefaultsStrategy}.
     */
    private static ApplyDefaultsStrategy mockApplyDefaultsStrategy(boolean apply) {
        ApplyDefaultsStrategy strategy = mock(ApplyDefaultsStrategy.class);
        when(strategy.shouldApplyArrayDefaults()).thenReturn(apply);
        return strategy;
    }

    @Test
    void walk_returnsEmptySetWhenNodeIsNotArray() {
        // Arrange
        JsonNode nonArrayNode = mock(JsonNode.class);
        when(nonArrayNode.isArray()).thenReturn(false);
        ExecutionContext executionContext = new ExecutionContext();
        JsonNode rootNode = mock(JsonNode.class);
        String at = "";
        boolean shouldValidateSchema = false;
        // Act
        Set<ValidationMessage> result = validator.walk(executionContext, nonArrayNode, rootNode, at, shouldValidateSchema);
        // Assert
        assertNotNull(result, "Resulting set must not be null");
        assertTrue(result.isEmpty(), "Result should be empty when the node is not an array");
        verify(nonArrayNode, times(1)).isArray();
        verifyNoMoreInteractions(nonArrayNode);
    }

    @Test
    void walk_returnsEmptySetWhenArrayIsEmpty() {
        // Arrange
        ObjectMapper mapper = new ObjectMapper();
        // size == 0
        ArrayNode emptyArray = mapper.createArrayNode();
        ExecutionContext executionContext = new ExecutionContext();
        JsonNode rootNode = mock(JsonNode.class);
        String at = "";
        boolean shouldValidateSchema = false;
        // Act
        Set<ValidationMessage> result = validator.walk(executionContext, emptyArray, rootNode, at, shouldValidateSchema);
        // Assert
        assertNotNull(result, "Resulting set must not be null");
        assertTrue(result.isEmpty(), "Result should be empty when the array has no items");
        // The validator should have inspected the array but performed no further actions.
        assertTrue(emptyArray.isArray(), "The test array must still be recognized as an array");
    }

    /* --------------------------------------------------------------
     *  ApplyDefaultsStrategy disabled – array must stay untouched
     * -------------------------------------------------------------- */
    @Test
    void walk_whenApplyDefaultsStrategyIsDisabled_shouldReturnEmptyMessagesAndNotModifyArray() {
        // Disable defaults
        setField(validator, "applyDefaultsStrategy", mockApplyDefaultsStrategy(false));
        ArrayNode array = MAPPER.createArrayNode().add(1).add(2).add(3);
        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> messages = validator.walk(ctx, array, array, "/", true);
        assertTrue(messages.isEmpty(), "No validation messages expected");
        // array must stay exactly the same
        assertEquals("[1,2,3]", array.toString(), "Array content must remain unchanged when defaults are not applied");
    }

    /* --------------------------------------------------------------
     *  Tuple schema empty – walk should iterate zero times
     * -------------------------------------------------------------- */
    @Test
    void walk_whenTupleSchemaIsEmpty_shouldIterateZeroTimes() {
        setField(validator, "applyDefaultsStrategy", mockApplyDefaultsStrategy(true));
        setField(validator, "tupleSchema", Collections.emptyList());
        ArrayNode array = MAPPER.createArrayNode().add("a").add("b");
        Set<ValidationMessage> messages = validator.walk(new ExecutionContext(), array, array, "/", true);
        assertTrue(messages.isEmpty());
        assertEquals("[\"a\",\"b\"]", array.toString(), "Array must stay unchanged when there is no tuple schema");
    }

    /* --------------------------------------------------------------
     *  Node size smaller than tupleSchema size
     * -------------------------------------------------------------- */
    @Test
    void walk_whenNodeSizeLessThanTupleSchemaSize_shouldIterateNodeSizeTimes() {
        setField(validator, "applyDefaultsStrategy", mockApplyDefaultsStrategy(true));
        // Create a tuple schema with three entries (larger than the array size)
        List<JsonSchema> tuple = Arrays.asList(mock(JsonSchema.class), mock(JsonSchema.class), mock(JsonSchema.class));
        setField(validator, "tupleSchema", tuple);
        // each mocked schema returns an empty object node for getSchemaNode()
        tuple.forEach(js -> when(js.getSchemaNode()).thenReturn(MAPPER.createObjectNode()));
        ArrayNode array = MAPPER.createArrayNode().add(1).add(2);
        Set<ValidationMessage> messages = validator.walk(new ExecutionContext(), array, array, "/", true);
        assertTrue(messages.isEmpty(), "No validation messages expected");
        assertEquals("[1,2]", array.toString(), "Array content must remain unchanged when defaults are not defined");
    }

    /* --------------------------------------------------------------
     *  Node size greater than tupleSchema size
     * -------------------------------------------------------------- */
    @Test
    void walk_whenNodeSizeGreaterThanTupleSchemaSize_shouldIterateTupleSchemaSizeTimes() {
        setField(validator, "applyDefaultsStrategy", mockApplyDefaultsStrategy(true));
        // tuple schema with two entries
        List<JsonSchema> tuple = Arrays.asList(mock(JsonSchema.class), mock(JsonSchema.class));
        setField(validator, "tupleSchema", tuple);
        tuple.forEach(js -> when(js.getSchemaNode()).thenReturn(MAPPER.createObjectNode()));
        ArrayNode array = MAPPER.createArrayNode().add("x").add("y").add("z");
        Set<ValidationMessage> messages = validator.walk(new ExecutionContext(), array, array, "/", true);
        assertTrue(messages.isEmpty());
        assertEquals("[\"x\",\"y\",\"z\"]", array.toString(), "Array must stay unchanged – walk only limits iteration, it does not trim the array");
    }

    /* --------------------------------------------------------------
     *  Null element with a default value defined in the schema
     * -------------------------------------------------------------- */
    @Test
    void walk_whenElementIsNullAndDefaultExists_shouldReplaceWithDefaultAndPassDefaultToDoWalk() {
        setField(validator, "applyDefaultsStrategy", mockApplyDefaultsStrategy(true));
        // schema for index 0 contains a default value "defaultValue"
        JsonSchema schemaWithDefault = mock(JsonSchema.class);
        ObjectNode schemaNode0 = MAPPER.createObjectNode().set("default", new TextNode("defaultValue"));
        when(schemaWithDefault.getSchemaNode()).thenReturn(schemaNode0);
        // schema for index 1 – no default
        JsonSchema schemaNoDefault = mock(JsonSchema.class);
        when(schemaNoDefault.getSchemaNode()).thenReturn(MAPPER.createObjectNode());
        setField(validator, "tupleSchema", Arrays.asList(schemaWithDefault, schemaNoDefault));
        ArrayNode array = MAPPER.createArrayNode().addNull().add(5);
        Set<ValidationMessage> messages = validator.walk(new ExecutionContext(), array, array, "/", true);
        assertTrue(messages.isEmpty());
        assertEquals("[\"defaultValue\",5]", array.toString(), "The null element must be replaced by its default value");
    }

    /* --------------------------------------------------------------
     *  Null element without a default – array must stay unchanged
     * -------------------------------------------------------------- */
    @Test
    void walk_whenElementIsNullAndNoDefault_shouldPassNullToDoWalkWithoutModification() {
        setField(validator, "applyDefaultsStrategy", mockApplyDefaultsStrategy(true));
        JsonSchema schema0 = mock(JsonSchema.class);
        // no "default"
        when(schema0.getSchemaNode()).thenReturn(MAPPER.createObjectNode());
        JsonSchema schema1 = mock(JsonSchema.class);
        when(schema1.getSchemaNode()).thenReturn(MAPPER.createObjectNode());
        setField(validator, "tupleSchema", Arrays.asList(schema0, schema1));
        ArrayNode array = MAPPER.createArrayNode().addNull().add("b");
        Set<ValidationMessage> messages = validator.walk(new ExecutionContext(), array, array, "/", true);
        assertTrue(messages.isEmpty());
        assertEquals("[null,\"b\"]", array.toString(), "Array must remain unchanged when no default is defined");
    }
}
