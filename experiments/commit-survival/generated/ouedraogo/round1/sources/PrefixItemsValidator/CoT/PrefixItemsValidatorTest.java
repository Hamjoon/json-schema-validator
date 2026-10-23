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
