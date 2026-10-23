```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PrefixItemsValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}.
 *
 * The tests cover:
 * <ul>
 *   <li>behaviour when the {@link ApplyDefaultsStrategy} is disabled/enabled</li>
 *   <li>different sizes of the input array vs the {@code tupleSchema}</li>
 *   <li>default‑value handling for {@code null} items</li>
 *   <li>exceptional situations (null fields, wrong node types, etc.)</li>
 * </ul>
 */
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

    /** Helper to set a private or protected field via reflection. */
    private static void setField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Helper to create a mock {@link ApplyDefaultsStrategy}. */
    private static ApplyDefaultsStrategy mockApplyDefaultsStrategy(boolean apply) {
        ApplyDefaultsStrategy strategy = mock(ApplyDefaultsStrategy.class);
        when(strategy.shouldApplyArrayDefaults()).thenReturn(apply);
        return strategy;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Basic stubbing required by the PrefixItemsValidator constructor
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(parentSchema);

        // Minimal schema node – a single empty schema object in the "prefixItems" array
        ArrayNode schemaNode = MAPPER.createArrayNode();
        schemaNode.add(MAPPER.createObjectNode());

        validator = new PrefixItemsValidator("testPath", schemaNode, parentSchema, validationContext);
    }

    /* --------------------------------------------------------------
     *  Simple “no‑array” path
     * -------------------------------------------------------------- */
    @Test
    void walk_returnsEmptySetWhenNodeIsNotArray() {
        JsonNode nonArrayNode = mock(JsonNode.class);
        when(nonArrayNode.isArray()).thenReturn(false);
        ExecutionContext executionContext = new ExecutionContext();

        Set<ValidationMessage> result = validator.walk(executionContext, nonArrayNode,
                mock(JsonNode.class), "/", true);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(nonArrayNode, times(1)).isArray();
        verifyNoMoreInteractions(nonArrayNode);
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
        assertEquals("[1,2,3]", array.toString(),
                "Array content must remain unchanged when defaults are not applied");
    }

    /* --------------------------------------------------------------
     *  Tuple schema empty – walk should iterate zero times
     * -------------------------------------------------------------- */
    @Test
    void walk_whenTupleSchemaIsEmpty_shouldIterateZeroTimes() {
        setField(validator, "applyDefaultsStrategy", mockApplyDefaultsStrategy(true));
        setField(validator, "tupleSchema", Collections.emptyList());

        ArrayNode array = MAPPER.createArrayNode().add("a").add("b");
        Set<ValidationMessage> messages = validator.walk(new ExecutionContext(),
                array, array, "/", true);

        assertTrue(messages.isEmpty());
        assertEquals("[\"a\",\"b\"]", array.toString(),
                "Array must stay unchanged when there is no tuple schema");
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
        Set<ValidationMessage> messages = validator.walk(new ExecutionContext(),
                array, array, "/", true);

        assertTrue(messages.isEmpty(), "No validation messages expected");
        assertEquals("[1,2]", array.toString(),
                "Array content must remain unchanged when defaults are not defined");
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
        Set<ValidationMessage> messages = validator.walk(new ExecutionContext(),
                array, array, "/", true);

        assertTrue(messages.isEmpty());
        assertEquals("[\"x\",\"y\",\"z\"]", array.toString(),
                "Array must stay unchanged – walk only limits iteration, it does not trim the array");
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
        Set<ValidationMessage> messages = validator.walk(new ExecutionContext(),
                array, array, "/", true);

        assertTrue(messages.isEmpty());
        assertEquals("[\"defaultValue\",5]", array.toString(),
                "The null element must be replaced by its default value");
    }

    /* --------------------------------------------------------------
     *  Null element without a default – array must stay unchanged
     * -------------------------------------------------------------- */
    @Test
    void walk_whenElementIsNullAndNoDefault_shouldPassNullToDoWalkWithoutModification() {
        setField(validator, "applyDefaultsStrategy", mockApplyDefaultsStrategy(true));

        JsonSchema schema0 = mock(JsonSchema.class);
        when(schema0.getSchemaNode()).thenReturn(MAPPER.createObjectNode()); // no "default"
        JsonSchema schema1 = mock(JsonSchema.class);
        when(schema1.getSchemaNode()).thenReturn(MAPPER.createObjectNode());

        setField(validator, "tupleSchema", Arrays.asList(schema0, schema1));

        ArrayNode array = MAPPER.createArrayNode().addNull().add("b");
        Set<ValidationMessage> messages = validator.walk(new ExecutionContext(),
                array, array, "/", true);

        assertTrue(messages.isEmpty());
        assertEquals("[null,\"b\"]", array.toString(),
                "Array must remain unchanged when no default is defined");
    }

    /* --------------------------------------------------------------
     *  Non‑null element – should be forwarded unchanged
     * -------------------------------------------------------------- */
    @Test
    void walk_whenElementIsNonNull_shouldPassOriginalNodeToDoWalk() {
        setField(validator, "applyDefaultsStrategy", mockApplyDefaultsStrategy(true));

        // two trivial schemas
        JsonSchema s0 = mock(JsonSchema.class);
        JsonSchema s1 = mock(JsonSchema.class);
        when(s0.getSchemaNode()).thenReturn(MAPPER.createObjectNode());
        when(s1.getSchemaNode()).thenReturn(MAPPER.createObjectNode());
        setField(validator, "tupleSchema", Arrays.asList(s0, s1));

        ArrayNode array = MAPPER.createArrayNode().add(10).addNull();
        Set<ValidationMessage> messages = validator.walk(new ExecutionContext(),
                array, array, "/", true);

        assertTrue(messages.isEmpty());
        assertEquals("[10,null]", array.toString(),
                "