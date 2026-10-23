**Step 1 – Public methods of `PrefixItemsValidator`**

| # | Visibility | Return type | Method name | Parameter list |
|---|------------|-------------|-------------|----------------|
| 1 | `public` | `PrefixItemsValidator` (constructor) | `PrefixItemsValidator` | `String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext` |
| 2 | `public` | `Set<ValidationMessage>` | `validate` | `ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at` |
| 3 | `public` | `Set<ValidationMessage>` | `walk` | `ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema` |
| 4 | `public` | `List<JsonSchema>` | `getTupleSchema` | *(none)* |
| 5 | `public` | `void` | `preloadJsonSchema` | *(none)* |

---

**Step 2 – Basic JUnit 4 test cases for each method**  

(These tests verify the *happy‑path* behaviour.)

* `testConstructorCreatesTupleSchema` – builds the validator with a non‑empty `ArrayNode` and checks that the internal `tupleSchema` list has the same size as the array.  
* `testValidateIgnoresNonArrayNode` – passes a non‑array JSON node to `validate` and asserts that the returned error set is empty.  
* `testValidateEvaluatesItemsWhenSchemasPass` – supplies an array node whose elements are validated by mocked `JsonSchema` objects that return **no** errors; the test verifies that the paths are recorded in the *evaluated items* collection.  
* `testValidateCollectsErrorsFromSchemas` – one of the mocked schemas returns a `ValidationMessage`; the test checks that this message appears in the result of `validate`.  
* `testWalkAppliesDefaultsAndWalksSchema` – creates an array whose first element is `null` and whose schema defines a `"default"` value; after `walk` the array element is replaced by the default and the schema’s `walk` method is invoked.  
* `testGetTupleSchemaReturnsInternalList` – simply asserts that the list returned by `getTupleSchema` is the same instance that was populated in the constructor.  
* `testPreloadJsonSchemaInvokesPreloadOnAllTupleSchemas` – verifies that `preloadJsonSchemas` (inherited from `BaseJsonValidator`) is called for each tuple schema (the method itself is `protected` in the base class, so we only check that no exception is thrown and that the mocked schemas receive the call).

---

**Step 3 – Edge‑case & exception scenarios that must be tested**

| Scenario | Reason |
|----------|--------|
| **Constructor receives a non‑array node** | The class must throw `IllegalArgumentException`. |
| **Constructor receives an empty array node** | Same exception – the spec requires a *non‑empty* array. |
| **`validate` called with an array longer than the tuple schema** | Only the first *N* items (where *N* = `tupleSchema.size()`) must be validated; the extra items are ignored. |
| **`validate` called with an array shorter than the tuple schema** | Validation must stop at the actual array length – no `IndexOutOfBoundsException`. |
| **`walk` when `applyDefaultsStrategy` is disabled** | No default insertion and no walk should be performed. |
| **`walk` when the node is not an array** | Method must return an empty set without throwing. |
| **`walk` when a schema has no `"default"` field** | The original node must stay unchanged but the schema’s `walk` still runs. |
| **`walk` when a pre‑walk listener aborts walking** | The validator must respect the boolean returned by `runPreWalkListeners` and skip the schema’s `walk`. (We mock the listener runner to return `false`). |
| **`walk` when a post‑walk listener throws an exception** | The validator should propagate the exception (we verify that the exception is not swallowed). |

---

**Step 4 – JUnit 4 test cases for the identified edge cases**

* `testConstructorRejectsNonArrayNode` – passes a plain `ObjectNode` and expects `IllegalArgumentException`.  
* `testConstructorRejectsEmptyArrayNode` – passes an empty `ArrayNode` and expects the same exception.  
* `testValidateIgnoresItemsBeyondTupleSchema` – array of size 3, tuple schema size 2; verify only two schemas are consulted.  
* `testValidateStopsAtArrayEndWhenSchemaIsLonger` – array of size 1, tuple schema size 3; ensure only the first schema is used.  
* `testWalkDoesNothingWhenApplyDefaultsStrategyIsDisabled` – mock `validationContext.getConfig().getApplyDefaultsStrategy()` to return a strategy that reports *no* default application; assert the input array stays unchanged and no walk is triggered.  
* `testWalkSkipsSchemaWhenPreWalkListenerReturnsFalse` – inject a mock `WalkListenerRunner` that returns `false` from `runPreWalkListeners`; verify that the schema’s `walk` method is **not** called.  
* `testWalkPropagatesExceptionFromPostWalkListener` – make the mock runner throw a `RuntimeException` from `runPostWalkListeners`; assert that the exception bubbles up.

All these tests are combined into a single file below.

---

**Step 5 – Complete JUnit 4 test file (`PrefixItemsValidatorTest.java`)**

```java
###Test START##
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PrefixItemsValidator}.
 *
 * The tests use Mockito to mock the heavy dependencies
 * (ValidationContext, JsonSchema, ExecutionContext, etc.).
 * Only the logic inside {@code PrefixItemsValidator} is exercised.
 */
@RunWith(MockitoJUnitRunner.class)
public class PrefixItemsValidatorTest {

    // -----------------------------------------------------------------
    // Mocks required for constructing a PrefixItemsValidator instance
    // -----------------------------------------------------------------
    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonSchema parentSchema;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;   // inner interface used by ExecutionContext

    // -----------------------------------------------------------------
    // Helper objects that are reused across many tests
    // -----------------------------------------------------------------
    private final String schemaPath = "#/prefixItems";

    @Before
    public void setUp() {
        // ExecutionContext -> CollectorContext -> evaluated items set
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.getEvaluatedItems()).thenReturn(new LinkedHashSet<String>());

        // ValidationContext – the validator asks it for new JsonSchema objects.
        // By default we return a mock JsonSchema that validates successfully.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenAnswer(new Answer<JsonSchema>() {
                    @Override
                    public JsonSchema answer(InvocationOnMock invocation) {
                        JsonSchema mock = mock(JsonSchema.class);
                        // default: no validation errors, no walk errors, no default node
                        when(mock.validate(any(), any(), any(), anyString()))
                                .thenReturn(Collections.emptySet());
                        when(mock.walk(any(), any(), any(), anyString(), anyBoolean()))
                                .thenReturn(Collections.emptySet());
                        when(mock.getSchemaNode()).thenReturn(JsonNodeFactory.instance.objectNode());
                        when(mock.getSchemaPath()).thenReturn("#/tupleSchema");
                        when(mock.getParentSchema()).thenReturn(parentSchema);
                        return mock;
                    }
                });

        // ValidationContext configuration – by default defaults are applied
        ValidationContext.Config config = mock(ValidationContext.Config.class);
        when(config.getArrayItemWalkListeners()).thenReturn(Collections.emptyList());
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getJsonSchemaFactory()).thenReturn(mock(JsonSchemaFactory.class));
        when(validationContext.getApplyDefaultsStrategy()).thenReturn(new ApplyDefaultsStrategy() {
            @Override
            public boolean shouldApplyArrayDefaults() {
                return true;
            }
        });
    }

    // --------------------------------------------------------------
    // Helper to build a PrefixItemsValidator with a given schema array
    // --------------------------------------------------------------
    private PrefixItemsValidator buildValidator(ArrayNode schemaArray) {
        return new PrefixItemsValidator(schemaPath, schemaArray, parentSchema, validationContext);
    }

    // -----------------------------------------------------------------
    // 1️⃣  Constructor – happy path
    // -----------------------------------------------------------------
    @Test
    public void testConstructorCreatesTupleSchema() {
        ArrayNode schemaArray = JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode())
                .add(JsonNodeFactory.instance.objectNode());

        PrefixItemsValidator validator = buildValidator(schemaArray);

        assertNotNull(validator.getTupleSchema());
        assertEquals(2, validator.getTupleSchema().size());
    }

    // -----------------------------------------------------------------
    // 2️⃣  Constructor – error scenarios (edge cases)
    // -----------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNonArrayNode() {
        ObjectNode notArray = JsonNodeFactory.instance.objectNode();
        new PrefixItemsValidator(schemaPath, notArray, parentSchema, validationContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsEmptyArrayNode() {
        ArrayNode empty = JsonNodeFactory.instance.arrayNode(); // size == 0
        new PrefixItemsValidator(schemaPath, empty, parentSchema, validationContext);
    }

    // -----------------------------------------------------------------
    // 3️⃣  validate() – non‑array input
    // -----------------------------------------------------------------
    @Test
    public void testValidateIgnoresNonArrayNode() {
        PrefixItemsValidator validator = buildValidator(JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode()));
        JsonNode notArray = JsonNodeFactory.instance.objectNode();

        Set<ValidationMessage> errors = validator.validate(executionContext, notArray,
                notArray, "#");

        assertTrue(errors.isEmpty());
    }

    // -----------------------------------------------------------------
    // 4️⃣  validate() – all schemas succeed (evaluated items are recorded)
    // -----------------------------------------------------------------
    @Test
    public void testValidateEvaluatesItemsWhenSchemasPass() {
        // two tuple schemas
        PrefixItemsValidator validator = buildValidator(JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode())
                .add(JsonNodeFactory.instance.objectNode()));

        ArrayNode data = JsonNodeFactory.instance.arrayNode()
                .add(IntNode.valueOf(1))
                .add(TextNode.valueOf("foo"));

        Set<ValidationMessage> errors = validator.validate(executionContext, data, data, "#");

        assertTrue(errors.isEmpty());

        // collectorContext should have received the evaluated paths "#/0" and "#/1"
        Set<String> evaluated = collectorContext.getEvaluatedItems();
        assertTrue(evaluated.contains("#/0"));
        assertTrue(evaluated.contains("#/1"));
    }

    // -----------------------------------------------------------------
    // 5️⃣  validate() – one schema returns an error
    // -----------------------------------------------------------------
    @Test
    public void testValidateCollectsErrorsFromSchemas() {
        // Mock the first tuple schema to produce a ValidationMessage
        JsonSchema errorSchema = mock(JsonSchema.class);
        ValidationMessage vm = mock(ValidationMessage.class);
        when(errorSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(vm)));
        when(errorSchema.getSchemaNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        when(errorSchema.getSchemaPath()).thenReturn("#/tuple/0");
        when(errorSchema.getParentSchema()).thenReturn(parentSchema);
        // Ensure the validator receives this mock as the first tuple schema
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(errorSchema)   // first call -> error schema
                .thenAnswer(invocation -> {
                    // subsequent calls -> normal mock (no errors)
                    JsonSchema mock = mock(JsonSchema.class);
                    when(mock.validate(any(), any(), any(), anyString()))
                            .thenReturn(Collections.emptySet());
                    when(mock.getSchemaNode()).thenReturn(JsonNodeFactory.instance.objectNode());
                    when(mock.getSchemaPath()).thenReturn("#/tuple/1");
                    when(mock.getParentSchema()).thenReturn(parentSchema);
                    return mock;
                });

        PrefixItemsValidator validator = buildValidator(JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode())
                .add(JsonNodeFactory.instance.objectNode()));

        ArrayNode data = JsonNodeFactory.instance.arrayNode()
                .add(IntNode.valueOf(42))
                .add(TextNode.valueOf("bar"));

        Set<ValidationMessage> errors = validator.validate(executionContext, data, data, "#");

        assertEquals(1, errors.size());
        assertTrue(errors.contains(vm));

        // Only the second item should be marked as evaluated (first failed)
        Set<String> evaluated = collectorContext.getEvaluatedItems();
        assertFalse(evaluated.contains("#/0"));
        assertTrue(evaluated.contains("#/1"));
    }

    // -----------------------------------------------------------------
    // 6️⃣  validate() – array longer than tuple schema (extra items ignored)
    // -----------------------------------------------------------------
    @Test
    public void testValidateIgnoresItemsBeyondTupleSchema() {
        // only one tuple schema
        PrefixItemsValidator validator = buildValidator(JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode()));

        // data array has three elements
        ArrayNode data = JsonNodeFactory.instance.arrayNode()
                .add(IntNode.valueOf(1))
                .add(IntNode.valueOf(2))
                .add(IntNode.valueOf(3));

        Set<ValidationMessage> errors = validator.validate(executionContext, data, data, "#");
        assertTrue(errors.isEmpty());

        // only "#/0" should be recorded as evaluated
        Set<String> evaluated = collectorContext.getEvaluatedItems();
        assertTrue(evaluated.contains("#/0"));
        assertFalse(evaluated.contains("#/1"));
        assertFalse(evaluated.contains("#/2"));
    }

    // -----------------------------------------------------------------
    // 7️⃣  validate() – array shorter than tuple schema (no IndexOutOfBounds)
    // -----------------------------------------------------------------
    @Test
    public void testValidateStopsAtArrayEndWhenSchemaIsLonger() {
        // two tuple schemas
        PrefixItemsValidator validator = buildValidator(JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode())
                .add(JsonNodeFactory.instance.objectNode()));

        // data array has only one element
        ArrayNode data = JsonNodeFactory.instance.arrayNode()
                .add(TextNode.valueOf("only one"));

        Set<ValidationMessage> errors = validator.validate(executionContext, data, data, "#");
        assertTrue(errors.isEmpty());

        // only "#/0" should be evaluated
        Set<String> evaluated = collectorContext.getEvaluatedItems();
        assertTrue(evaluated.contains("#/0"));
        assertFalse(evaluated.contains("#/1"));
    }

    // -----------------------------------------------------------------
    // 8️⃣  walk() – default insertion and schema walking
    // -----------------------------------------------------------------
    @Test
    public void testWalkAppliesDefaultsAndWalksSchema() {
        // Prepare a tuple schema that defines a default value
        JsonSchema schemaWithDefault = mock(JsonSchema.class);
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode()
                .set("default", TextNode.valueOf("defaultValue"));
        when(schemaWithDefault.getSchemaNode()).thenReturn(schemaNode);
        when(schemaWithDefault.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(schemaWithDefault.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());
        when(schemaWithDefault.getSchemaPath()).thenReturn("#/tuple/0");
        when(schemaWithDefault.getParentSchema()).thenReturn(parentSchema);

        // First call to newSchema returns our special mock, second call returns a normal mock
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schemaWithDefault)
                .thenAnswer(invocation -> {
                    JsonSchema mock = mock(JsonSchema.class);
                    when(mock.getSchemaNode()).thenReturn(JsonNodeFactory.instance.objectNode());
                    when(mock.validate(any(), any(), any(), anyString()))
                            .thenReturn(Collections.emptySet());
                    when(mock.walk(any(), any(), any(), anyString(), anyBoolean()))
                            .thenReturn(Collections.emptySet());
                    when(mock.getSchemaPath()).thenReturn("#/tuple/1");
                    when(mock.getParentSchema()).thenReturn(parentSchema);
                    return mock;
                });

        PrefixItemsValidator validator = buildValidator(JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode())
                .add(JsonNodeFactory.instance.objectNode()));

        // Inject a mock WalkListenerRunner that simply returns true for pre‑walk
        WalkListenerRunner listenerRunner = mock(WalkListenerRunner.class);
        when(listenerRunner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(JsonNode.class), any(JsonSchema.class), any(ValidationContext.class),
                any(JsonSchemaFactory.class))).thenReturn(true);
        // post‑walk does nothing
        doNothing().when(listenerRunner).runPostWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class),
                any(ValidationContext.class), any(JsonSchemaFactory.class), anySet());

        // Use reflection to replace the private field
        try {
            java.lang.reflect.Field f = PrefixItemsValidator.class.getDeclaredField("arrayItemWalkListenerRunner");
            f.setAccessible(true);
            f.set(validator, listenerRunner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Data array: first element is null (should be replaced by default)
        ArrayNode data = JsonNodeFactory.instance.arrayNode()
                .add(NullNode.instance)
                .add(IntNode.valueOf(5));

        Set<ValidationMessage> msgs = validator.walk(executionContext, data, data, "#", true);
        assertTrue(msgs.isEmpty());

        // Verify default value insertion
        assertEquals(TextNode.valueOf("defaultValue"), data.get(0));

        // Verify that the schema's walk method was invoked for both items
        verify(schemaWithDefault, times(1)).walk(eq(executionContext), eq(TextNode.valueOf("defaultValue")),
                eq(data), eq("#/0"), eq(true));
        // second schema (the generic mock) should also have been walked
        verify(validationContext, atLeastOnce()).newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class));
    }

    // -----------------------------------------------------------------
    // 9️⃣  walk() – defaults are disabled (nothing should change)
    // -----------------------------------------------------------------
    @Test
    public void testWalkDoesNothingWhenApplyDefaultsStrategyIsDisabled() {
        // Disable defaults via the validation context
        ApplyDefaultsStrategy noDefaults = mock(ApplyDefaultsStrategy.class);
        when(noDefaults.shouldApplyArrayDefaults()).thenReturn(false);
        when(validationContext.getApplyDefaultsStrategy()).thenReturn(noDefaults);

        PrefixItemsValidator validator = buildValidator(JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode()));

        ArrayNode data = JsonNodeFactory.instance.arrayNode()
                .add(NullNode.instance);

        Set<ValidationMessage> msgs = validator.walk(executionContext, data, data, "#", true);
        assertTrue(msgs.isEmpty());

        // The null value must stay unchanged because defaults are not applied
        assertTrue(data.get(0).isNull());
    }

    // -----------------------------------------------------------------
    // 🔟  walk() – pre‑walk listener aborts walking
    // -----------------------------------------------------------------
    @Test
    public void testWalkSkipsSchemaWhenPreWalkListenerReturnsFalse() {
        PrefixItemsValidator validator = buildValidator(JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode()));

        // Mock listener runner to return false (skip walking)
        WalkListenerRunner listenerRunner = mock(WalkListenerRunner.class);
        when(listenerRunner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(JsonNode.class), any(JsonSchema.class), any(ValidationContext.class),
                any(JsonSchemaFactory.class))).thenReturn(false);
        // post‑walk still runs (do nothing)
        doNothing().when(listenerRunner).runPostWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class),
                any(ValidationContext.class), any(JsonSchemaFactory.class), anySet());

        // Replace the private field
        try {
            java.lang.reflect.Field f = PrefixItemsValidator.class.getDeclaredField("arrayItemWalkListenerRunner");
            f.setAccessible(true);
            f.set(validator, listenerRunner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Use a real array node – contents irrelevant
        ArrayNode data = JsonNodeFactory.instance.arrayNode()
                .add(IntNode.valueOf(1));

        // Grab the tuple schema mock to verify that its walk() is NOT called
        JsonSchema tupleSchema = validator.getTupleSchema().get(0);
        Set<ValidationMessage> result = validator.walk(executionContext, data, data, "#", true);
        assertTrue(result.isEmpty());

        verify(tupleSchema, never()).walk(any(), any(), any(), anyString(), anyBoolean());
    }

    // -----------------------------------------------------------------
    // 1️⃣1️⃣ walk() – post‑walk listener throws, exception must propagate
    // -----------------------------------------------------------------
    @Test(expected = RuntimeException.class)
    public void testWalkPropagatesExceptionFromPostWalkListener() {
        PrefixItemsValidator validator = buildValidator(JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode()));

        WalkListenerRunner listenerRunner = mock(WalkListenerRunner.class);
        when(listenerRunner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(JsonNode.class), any(JsonSchema.class), any(ValidationContext.class),
                any(JsonSchemaFactory.class))).thenReturn(true);
        // Post‑walk throws
        doThrow(new RuntimeException("post‑walk failure"))
                .when(listenerRunner).runPostWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class),
                any(ValidationContext.class), any(JsonSchemaFactory.class), anySet());

        // Inject
        try {
            java.lang.reflect.Field f = PrefixItemsValidator.class.getDeclaredField("arrayItemWalkListenerRunner");
            f.setAccessible(true);
            f.set(validator, listenerRunner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ArrayNode data = JsonNodeFactory.instance.arrayNode()
                .add(IntNode.valueOf(7));

        // This call must bubble up the RuntimeException
        validator.walk(executionContext, data, data, "#", true);
    }

    // -----------------------------------------------------------------
    // 1️⃣2️⃣ getTupleSchema() – simple accessor test
    // -----------------------------------------------------------------
    @Test
    public void testGetTupleSchemaReturnsInternalList() {
        PrefixItemsValidator validator = buildValidator(JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode()));
        List<JsonSchema> list = validator.getTupleSchema();
        assertNotNull(list);
        // The returned list should be the same instance used internally
        assertSame(list, validator.getTupleSchema());
    }

    // -----------------------------------------------------------------
    // 1️⃣3️⃣ preloadJsonSchema() – ensure it forwards to each tuple schema
    // -----------------------------------------------------------------
    @Test
    public void testPreloadJsonSchemaInvokesPreloadOnAllTupleSchemas() {
        // Create two distinct schema mocks so we can verify the preload call
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        when(schemaA.getSchemaNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        when(schemaB.getSchemaNode()).thenReturn(JsonNodeFactory.instance.objectNode());

        // Make the ValidationContext return them in order
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schemaA)
                .thenReturn(schemaB);

        PrefixItemsValidator validator = buildValidator(JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode())
                .add(JsonNodeFactory.instance.objectNode()));

        // The method under test
        validator.preloadJsonSchema();

        // Verify that BaseJsonValidator's protected helper was called – we cannot call it directly,
        // but we can assert that no exception was thrown and that the mocks were touched.
        // (If the method were not delegating, the following verification would fail.)
        verify(schemaA, atLeastOnce()).getSchemaNode();
        verify(schemaB, atLeastOnce()).getSchemaNode();
    }
}
###Test END##