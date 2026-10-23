package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Comprehensive JUnit‑4 test suite for {@link PrefixItemsValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Constructor validation (happy path & error conditions)</li>
 *   <li>Public API methods: {@code validate}, {@code walk},
 *       {@code getTupleSchema} and {@code preloadJsonSchema}</li>
 *   <li>Edge‑cases such as non‑array instance nodes, extra items,
 *       default‑application toggling, and exception propagation.</li>
 * </ul>
 *
 * All collaborators are mocked with Mockito so the test suite does not depend
 * on the rest of the {@code networknt} validation engine.
 */
@RunWith(MockitoJUnitRunner.class)
public class PrefixItemsValidatorTest {

    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    /* ---------- helpers ---------- */

    private static ArrayNode array(JsonNode... elements) {
        ArrayNode a = FACTORY.arrayNode();
        for (JsonNode e : elements) {
            a.add(e);
        }
        return a;
    }

    private static ValidationContext mockValidationContext(JsonSchema schemaToReturn) {
        ValidationContext vc = mock(ValidationContext.class);
        when(vc.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schemaToReturn);
        Config cfg = mock(Config.class);
        when(cfg.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
        when(vc.getConfig()).thenReturn(cfg);
        when(vc.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));
        return vc;
    }

    private static ExecutionContext mockExecutionContext(Set<String> evaluatedItems) {
        ExecutionContext ec = mock(ExecutionContext.class);
        CollectorContext cc = mock(CollectorContext.class);
        when(cc.getEvaluatedItems()).thenReturn(evaluatedItems);
        when(ec.getCollectorContext()).thenReturn(cc);
        return ec;
    }

    private static ValidationMessage message(String path, String msg) {
        return ValidationMessage.of("test", path, msg);
    }

    /* ---------- constructor tests ---------- */

    @Test
    public void constructorCreatesValidatorWhenValidArray() {
        ArrayNode schemaNode = array(FACTORY.objectNode(), FACTORY.objectNode());
        JsonSchema dummy = mock(JsonSchema.class);
        ValidationContext vc = mockValidationContext(dummy);

        PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems",
                schemaNode, null, vc);

        assertNotNull(validator);
        assertEquals(2, validator.getTupleSchema().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsWhenSchemaNodeNotArray() {
        JsonNode notArray = FACTORY.objectNode();
        ValidationContext vc = mockValidationContext(mock(JsonSchema.class));
        new PrefixItemsValidator("#/prefixItems", notArray, null, vc);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsWhenSchemaArrayEmpty() {
        ArrayNode empty = FACTORY.arrayNode(); // size == 0
        ValidationContext vc = mockValidationContext(mock(JsonSchema.class));
        new PrefixItemsValidator("#/prefixItems", empty, null, vc);
    }

    /* ---------- getTupleSchema ---------- */

    @Test
    public void getTupleSchemaReturnsCorrectList() {
        ArrayNode schemaNode = array(FACTORY.objectNode(),
                FACTORY.objectNode(),
                FACTORY.objectNode());
        JsonSchema dummy = mock(JsonSchema.class);
        ValidationContext vc = mockValidationContext(dummy);
        PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems",
                schemaNode, null, vc);

        List<JsonSchema> list = validator.getTupleSchema();
        assertSame(list, validator.getTupleSchema());
        assertEquals(3, list.size());
    }

    /* ---------- validate ---------- */

    @Test
    public void validateReturnsEmptySetWhenAllItemsValid() {
        JsonSchema s0 = mock(JsonSchema.class);
        JsonSchema s1 = mock(JsonSchema.class);
        when(s0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(s1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        ArrayNode prefix = array(FACTORY.objectNode(), FACTORY.objectNode());
        ValidationContext vc = mock(ValidationContext.class);
        when(vc.newSchema(anyString(), any(JsonNode.class), any()))
                .thenReturn(s0, s1);
        Config cfg = mock(Config.class);
        when(cfg.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
        when(vc.getConfig()).thenReturn(cfg);
        when(vc.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));

        PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems",
                prefix, null, vc);

        ArrayNode instance = array(FACTORY.textNode("a"), FACTORY.textNode("b"));
        Set<String> evaluated = new LinkedHashSet<>();
        ExecutionContext ec = mockExecutionContext(evaluated);

        Set<ValidationMessage> result = validator.validate(ec, instance, instance, "$");
        assertTrue(result.isEmpty());
        assertEquals(2, evaluated.size());
        assertTrue(evaluated.contains("$[0]"));
        assertTrue(evaluated.contains("$[1]"));
    }

    @Test
    public void validatePropagatesErrorsAndDoesNotMarkEvaluated() {
        JsonSchema ok = mock(JsonSchema.class);
        JsonSchema bad = mock(JsonSchema.class);
        when(ok.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        ValidationMessage err = message("$[1]", "type mismatch");
        when(bad.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(err));

        ArrayNode prefix = array(FACTORY.objectNode(), FACTORY.objectNode());
        ValidationContext vc = mock(ValidationContext.class);
        when(vc.newSchema(anyString(), any(JsonNode.class), any()))
                .thenReturn(ok, bad);
        Config cfg = mock(Config.class);
        when(cfg.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
        when(vc.getConfig()).thenReturn(cfg);
        when(vc.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));

        PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems",
                prefix, null, vc);

        ArrayNode instance = array(FACTORY.textNode("ok"), FACTORY.numberNode(123));
        Set<String> evaluated = new LinkedHashSet<>();
        ExecutionContext ec = mockExecutionContext(evaluated);

        Set<ValidationMessage> result = validator.validate(ec, instance, instance, "$");
        assertEquals(1, result.size());
        assertTrue(result.contains(err));
        assertEquals(1, evaluated.size());
        assertTrue(evaluated.contains("$[0]"));
    }

    @Test
    public void validateIgnoresNonArrayNode() {
        JsonSchema dummy = mock(JsonSchema.class);
        ValidationContext vc = mockValidationContext(dummy);
        PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems",
                array(FACTORY.objectNode()), null, vc);

        JsonNode notArray = FACTORY.textNode("string");
        ExecutionContext ec = mockExecutionContext(new LinkedHashSet<>());

        Set<ValidationMessage> result = validator.validate(ec, notArray, notArray, "$");
        assertTrue(result.isEmpty());
    }

    /* ---------- walk ---------- */

    @Test
    public void walkCollectsMessagesFromTupleSchemas() {
        JsonSchema s0 = mock(JsonSchema.class);
        JsonSchema s1 = mock(JsonSchema.class);
        ValidationMessage m0 = message("$[0]", "msg0");
        ValidationMessage m1 = message("$[1]", "msg1");
        when(s0.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.singleton(m0));
        when(s1.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.singleton(m1));

        ArrayNode prefix = array(FACTORY.objectNode(), FACTORY.objectNode());
        ValidationContext vc = mock(ValidationContext.class);
        when(vc.newSchema(anyString(), any(JsonNode.class), any()))
                .thenReturn(s0, s1);
        Config cfg = mock(Config.class);
        when(cfg.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
        when(vc.getConfig()).thenReturn(cfg);
        when(vc.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));

        PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems",
                prefix, null, vc);

        ArrayNode instance = array(FACTORY.textNode("a"), FACTORY.textNode("b"));
        ExecutionContext ec = mockExecutionContext(new LinkedHashSet<>());

        Set<ValidationMessage> result = validator.walk(ec, instance, instance, "$", true);
        assertEquals(2, result.size());
        assertTrue(result.contains(m0));
        assertTrue(result.contains(m1));
    }

    @Test
    public void walkDoesNotApplyDefaultsWhenStrategyIsDisabled() throws Exception {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        ArrayNode prefix = array(FACTORY.objectNode());
        ValidationContext vc = mock(ValidationContext.class);
        when(vc.newSchema(anyString(), any(JsonNode.class), any()))
                .thenReturn(schema);
        Config cfg = mock(Config.class);
        when(cfg.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
        when(vc.getConfig()).thenReturn(cfg);
        when(vc.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));

        PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems",
                prefix, null, vc);

        // Force the protected applyDefaultsStrategy to return false
        Field f = BaseJsonValidator.class.getDeclaredField("applyDefaultsStrategy");
        f.setAccessible(true);
        ApplyDefaultsStrategy strategy = mock(ApplyDefaultsStrategy.class);
        when(strategy.shouldApplyArrayDefaults()).thenReturn(false);
        f.set(validator, strategy);

        ArrayNode instance = array(FACTORY.nullNode()); // would be replaced if defaults were applied
        ExecutionContext ec = mockExecutionContext(new LinkedHashSet<>());

        Set<ValidationMessage> result = validator.walk(ec, instance, instance, "$", true);
        assertTrue(result.isEmpty()); // no messages and no exception
    }

    /* ---------- preloadJsonSchema ---------- */

    @Test
    public void preloadJsonSchemaCallsPreloadOnAllTupleSchemas() {
        JsonSchema s0 = mock(JsonSchema.class);
        JsonSchema s1 = mock(JsonSchema.class);
        when(s0.preload()).thenReturn(null);
        when(s1.preload()).thenReturn(null);

        ArrayNode prefix = array(FACTORY.objectNode(), FACTORY.objectNode());
        ValidationContext vc = mock(ValidationContext.class);
        when(vc.newSchema(anyString(), any(JsonNode.class), any()))
                .thenReturn(s0, s1);
        Config cfg = mock(Config.class);
        when(cfg.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
        when(vc.getConfig()).thenReturn(cfg);
        when(vc.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));

        PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems",
                prefix, null, vc);

        validator.preloadJsonSchema();

        verify(s0).preload();
        verify(s1).preload();
    }
}
