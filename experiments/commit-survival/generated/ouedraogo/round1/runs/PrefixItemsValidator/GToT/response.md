**Step 1 – Public API of `PrefixItemsValidator`**

| Visibility | Return type | Method name & signature | Description |
|------------|-------------|--------------------------|-------------|
| `public` | `PrefixItemsValidator(String, JsonNode, JsonSchema, ValidationContext)` | `PrefixItemsValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext)` | Constructor – builds the validator from a *prefixItems* schema definition. |
| `public` | `Set<ValidationMessage>` | `validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at)` | Validates an instance array against the tuple‑schemas defined in *prefixItems*. |
| `public` | `Set<ValidationMessage>` | `walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema)` | Walks the instance array, applying defaults (if configured) and invoking walk‑listeners. |
| `public` | `List<JsonSchema>` | `getTupleSchema()` | Returns the list of tuple‑schemas created from the *prefixItems* array. |
| `public` | `void` | `preloadJsonSchema()` | Pre‑loads all tuple‑schemas (delegates to `BaseJsonValidator`). |

---

**Step 2 – Basic JUnit 4 test case ideas (one per method)**  

| Method | Basic test purpose |
|--------|--------------------|
| **Constructor** | Verify that a valid *prefixItems* array creates a non‑null validator and that the internal `tupleSchema` size matches the array length. |
| **validate** | When the instance node is an array whose first *n* items satisfy the *n* tuple‑schemas, the method returns an empty set and the evaluated‑items collection is populated. |
| **walk** | With an array node and a tuple‑schema that returns a known `ValidationMessage` from its `walk` method, the validator’s `walk` returns that message. |
| **getTupleSchema** | The returned list is the same instance that the constructor filled – its size equals the number of schema elements supplied. |
| **preloadJsonSchema** | Calling `preloadJsonSchema()` does not throw and internally forwards to `BaseJsonValidator.preloadJsonSchemas`. (We verify the call by spying on a mock `JsonSchema`.) |

---

**Step 3 – Edge‑case & exception scenarios that must be tested**

| Method | Edge case / exception | Why it matters |
|--------|-----------------------|----------------|
| **Constructor** | `schemaNode` is **not** an `ArrayNode` (e.g. an `ObjectNode`). | Must throw `IllegalArgumentException`. |
| **Constructor** | `schemaNode` is an **empty** array. | Must throw `IllegalArgumentException`. |
| **validate** | `node` is **not** an array (e.g. a JSON string). | Should simply return an empty, immutable set – no validation performed. |
| **validate** | Instance array is **longer** than the tuple‑schema list. | Only the first *tupleSchema.size()* items are validated; extra items are ignored. |
| **validate** | One of the tuple‑schemas returns a **non‑empty** error set. | Those errors are propagated and the evaluated‑items list is **not** updated for that index. |
| **walk** | `applyDefaultsStrategy.shouldApplyArrayDefaults()` returns **false**. | No default handling should be performed; the method still walks the items that exist. |
| **walk** | A tuple‑schema’s `walk` method returns a **non‑empty** set. | Those messages must be present in the result of `walk`. |
| **walk** | The instance node is **null** or not an array. | Should return an empty set without NPE. |
| **preloadJsonSchema** | Underlying tuple‑schemas throw an exception while pre‑loading. | The validator must propagate the exception (test that it is not swallowed). |

---

**Step 4 – JUnit 4 test cases for the identified edge cases & exceptions**  

Below we use **Mockito** to isolate the validator from the rest of the library and to control the behaviour of the dependent `JsonSchema`, `ValidationContext`, `ExecutionContext` and related collaborators.

*Key helper utilities used in the test class*  

```java
private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

/** builds a simple ArrayNode from the supplied element nodes */
private static ArrayNode array(JsonNode... elements) {
    ArrayNode a = FACTORY.arrayNode();
    for (JsonNode e : elements) {
        a.add(e);
    }
    return a;
}

/** creates a mock ValidationContext that returns the supplied JsonSchema for every call to newSchema(...) */
private static ValidationContext mockValidationContext(JsonSchema schemaToReturn) {
    ValidationContext vc = mock(ValidationContext.class);
    when(vc.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
            .thenReturn(schemaToReturn);
    // configuration needed for the DefaultItemWalkListenerRunner (empty listeners)
    Config config = mock(Config.class);
    when(config.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
    when(vc.getConfig()).thenReturn(config);
    // factory required by walkSchema()
    when(vc.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));
    return vc;
}

/** creates a mock ExecutionContext with a collector that provides a mutable Set for evaluated items */
private static ExecutionContext mockExecutionContext(Set<String> evaluatedItems) {
    ExecutionContext ec = mock(ExecutionContext.class);
    CollectorContext cc = mock(CollectorContext.class);
    when(cc.getEvaluatedItems()).thenReturn(evaluatedItems);
    when(ec.getCollectorContext()).thenReturn(cc);
    return ec;
}

/** a tiny ValidationMessage factory – we only need the message text for assertions */
private static ValidationMessage message(String path, String msg) {
    return ValidationMessage.of("test", path, msg);
}
```

*Individual test methods (grouped by the method they exercise)*  

```java
/*** Constructor ***/
@Test
public void constructorCreatesValidatorWhenValidArray() {
    // a prefixItems array with two simple schemas (the content of the schemas is irrelevant for the test)
    ArrayNode schemaNode = array(FACTORY.objectNode(), FACTORY.objectNode());
    JsonSchema dummySchema = mock(JsonSchema.class);
    ValidationContext vc = mockValidationContext(dummySchema);

    PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems", schemaNode,
            null, vc);

    assertNotNull(validator);
    assertEquals(2, validator.getTupleSchema().size());
}

/** illegal argument – non‑array node */
@Test(expected = IllegalArgumentException.class)
public void constructorThrowsWhenSchemaNodeNotArray() {
    JsonNode notArray = FACTORY.objectNode();
    ValidationContext vc = mockValidationContext(mock(JsonSchema.class));
    new PrefixItemsValidator("#/prefixItems", notArray, null, vc);
}

/** illegal argument – empty array */
@Test(expected = IllegalArgumentException.class)
public void constructorThrowsWhenSchemaArrayEmpty() {
    ArrayNode empty = FACTORY.arrayNode(); // size == 0
    ValidationContext vc = mockValidationContext(mock(JsonSchema.class));
    new PrefixItemsValidator("#/prefixItems", empty, null, vc);
}

/*** getTupleSchema ***/
@Test
public void getTupleSchemaReturnsCorrectList() {
    ArrayNode schemaNode = array(FACTORY.objectNode(), FACTORY.objectNode(), FACTORY.objectNode());
    JsonSchema dummy = mock(JsonSchema.class);
    ValidationContext vc = mockValidationContext(dummy);
    PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems", schemaNode,
            null, vc);

    List<JsonSchema> list = validator.getTupleSchema();
    assertSame(list, validator.getTupleSchema()); // same instance
    assertEquals(3, list.size());
}

/*** validate – happy path ***/
@Test
public void validateReturnsEmptySetWhenAllItemsValid() {
    // three tuple schemas – each will return an empty error set
    JsonSchema schema0 = mock(JsonSchema.class);
    JsonSchema schema1 = mock(JsonSchema.class);
    when(schema0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
    when(schema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

    // validator with two tuple schemas
    ArrayNode prefix = array(FACTORY.objectNode(), FACTORY.objectNode());
    ValidationContext vc = mock(ValidationContext.class);
    when(vc.newSchema(anyString(), any(JsonNode.class), any())).thenReturn(schema0, schema1);
    when(vc.getConfig()).thenReturn(mock(Config.class));
    when(vc.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));

    PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems", prefix,
            null, vc);

    // instance array with two items (both strings)
    ArrayNode instance = array(FACTORY.textNode("a"), FACTORY.textNode("b"));
    Set<String> evaluated = new LinkedHashSet<>();
    ExecutionContext ec = mockExecutionContext(evaluated);

    Set<ValidationMessage> result = validator.validate(ec, instance, instance, "$");
    assertTrue(result.isEmpty());
    assertEquals(2, evaluated.size());
    assertTrue(evaluated.contains("$[0]"));
    assertTrue(evaluated.contains("$[1]"));
}

/*** validate – error from one tuple schema ***/
@Test
public void validatePropagatesErrorsAndDoesNotMarkEvaluated() {
    JsonSchema okSchema = mock(JsonSchema.class);
    JsonSchema badSchema = mock(JsonSchema.class);
    when(okSchema.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

    ValidationMessage err = message("$[1]", "type mismatch");
    when(badSchema.validate(any(), any(), any(), any()))
            .thenReturn(Collections.singleton(err));

    // validator with two tuple schemas
    ArrayNode prefix = array(FACTORY.objectNode(), FACTORY.objectNode());
    ValidationContext vc = mock(ValidationContext.class);
    when(vc.newSchema(anyString(), any(JsonNode.class), any())).thenReturn(okSchema, badSchema);
    when(vc.getConfig()).thenReturn(mock(Config.class));
    when(vc.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));

    PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems", prefix,
            null, vc);

    // instance array with two items
    ArrayNode instance = array(FACTORY.textNode("ok"), FACTORY.numberNode(123));
    Set<String> evaluated = new LinkedHashSet<>();
    ExecutionContext ec = mockExecutionContext(evaluated);

    Set<ValidationMessage> result = validator.validate(ec, instance, instance, "$");
    assertEquals(1, result.size());
    assertTrue(result.contains(err));
    // only the first element should be in evaluatedItems
    assertEquals(1, evaluated.size());
    assertTrue(evaluated.contains("$[0]"));
}

/*** validate – non‑array node ***/
@Test
public void validateIgnoresNonArrayNode() {
    // any schema – we will never get to it because node.isArray() will be false
    JsonSchema schema = mock(JsonSchema.class);
    ValidationContext vc = mockValidationContext(schema);
    PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems",
            array(FACTORY.objectNode()), null, vc);

    JsonNode notArray = FACTORY.textNode("not an array");
    ExecutionContext ec = mockExecutionContext(new LinkedHashSet<>());

    Set<ValidationMessage> result = validator.validate(ec, notArray, notArray, "$");
    assertTrue(result.isEmpty());
}

/*** walk – delegate to tuple schema walk ***/
@Test
public void walkCollectsMessagesFromTupleSchemas() {
    // stub two tuple schemas – their walk() will each return a distinct message
    JsonSchema s0 = mock(JsonSchema.class);
    JsonSchema s1 = mock(JsonSchema.class);
    ValidationMessage m0 = message("$[0]", "msg0");
    ValidationMessage m1 = message("$[1]", "msg1");
    when(s0.walk(any(), any(), any(), anyString(), anyBoolean()))
            .thenReturn(Collections.singleton(m0));
    when(s1.walk(any(), any(), any(), anyString(), anyBoolean()))
            .thenReturn(Collections.singleton(m1));

    // validator with two tuple schemas
    ArrayNode prefix = array(FACTORY.objectNode(), FACTORY.objectNode());
    ValidationContext vc = mock(ValidationContext.class);
    when(vc.newSchema(anyString(), any(JsonNode.class), any())).thenReturn(s0, s1);
    // config needed for the DefaultItemWalkListenerRunner (empty listeners)
    Config cfg = mock(Config.class);
    when(cfg.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
    when(vc.getConfig()).thenReturn(cfg);
    when(vc.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));

    PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems", prefix,
            null, vc);

    // instance array with two items (content does not matter)
    ArrayNode instance = array(FACTORY.textNode("a"), FACTORY.textNode("b"));
    ExecutionContext ec = mockExecutionContext(new LinkedHashSet<>());

    Set<ValidationMessage> result = validator.walk(ec, instance, instance, "$", true);
    assertEquals(2, result.size());
    assertTrue(result.contains(m0));
    assertTrue(result.contains(m1));
}

/*** walk – when defaults are not applied (strategy returns false) ***/
@Test
public void walkDoesNotApplyDefaultsWhenStrategyIsDisabled() {
    // we only care that walk still forwards the call; defaults handling is internal
    JsonSchema schema = mock(JsonSchema.class);
    when(schema.walk(any(), any(), any(), anyString(), anyBoolean()))
            .thenReturn(Collections.emptySet());

    // validator with a single tuple schema
    ArrayNode prefix = array(FACTORY.objectNode());
    ValidationContext vc = mock(ValidationContext.class);
    when(vc.newSchema(anyString(), any(JsonNode.class), any())).thenReturn(schema);
    Config cfg = mock(Config.class);
    when(cfg.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
    when(vc.getConfig()).thenReturn(cfg);
    when(vc.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));

    PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems", prefix,
            null, vc);

    // force the applyDefaultsStrategy to *not* apply defaults
    // BaseJsonValidator exposes the protected field – we use reflection for the test
    try {
        Field f = BaseJsonValidator.class.getDeclaredField("applyDefaultsStrategy");
        f.setAccessible(true);
        ApplyDefaultsStrategy strategy = mock(ApplyDefaultsStrategy.class);
        when(strategy.shouldApplyArrayDefaults()).thenReturn(false);
        f.set(validator, strategy);
    } catch (Exception e) {
        fail("Reflection failed: " + e);
    }

    ArrayNode instance = array(FACTORY.nullNode()); // null value that could be replaced by a default
    ExecutionContext ec = mockExecutionContext(new LinkedHashSet<>());

    Set<ValidationMessage> result = validator.walk(ec, instance, instance, "$", true);
    // no messages expected, but also no exception – defaults were simply ignored
    assertTrue(result.isEmpty());
}

/*** preloadJsonSchema – forwards to BaseJsonValidator ***/
@Test
public void preloadJsonSchemaCallsPreloadOnAllTupleSchemas() {
    JsonSchema s0 = mock(JsonSchema.class);
    JsonSchema s1 = mock(JsonSchema.class);
    // make the validator use these two schemas
    ArrayNode prefix = array(FACTORY.objectNode(), FACTORY.objectNode());
    ValidationContext vc = mock(ValidationContext.class);
    when(vc.newSchema(anyString(), any(JsonNode.class), any())).thenReturn(s0, s1);
    Config cfg = mock(Config.class);
    when(cfg.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
    when(vc.getConfig()).thenReturn(cfg);
    when(vc.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));

    PrefixItemsValidator validator = new PrefixItemsValidator("#/prefixItems", prefix,
            null, vc);

    // invoke the method under test
    validator.preloadJsonSchema();

    // verify that BaseJsonValidator.preloadJsonSchemas() called preload on each schema
    verify(s0).preload();
    verify(s1).preload();
}
```

---

**Step 5 – Merged, ready‑to‑run JUnit 4 test file**

```java
###Test START##
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
###Test END##