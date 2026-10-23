/**
 * JUnit‑4 test suite for {@link com.networknt.schema.PrefixItemsValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Constructor validation (valid array, non‑array & empty array handling)</li>
 *   <li>{@link PrefixItemsValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}</li>
 *   <li>{@link PrefixItemsValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}</li>
 *   <li>Behaviour of the internal {@link WalkListenerRunner} during walk()</li>
 *   <li>Accessor {@link PrefixItemsValidator#getTupleSchema()}</li>
 * </ul>
 *
 * Mockito is used to mock the heavy collaborators (ValidationContext, ExecutionContext,
 * JsonSchema, WalkListenerRunner, etc.) so the tests stay fast and focused on the
 * logic inside {@code PrefixItemsValidator}.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@link PrefixItemsValidator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PrefixItemsValidatorTest {

    // ----------------------------------------------------------------------
    // Mocks & shared objects
    // ----------------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private JsonSchema parentSchema;
    @Mock private JsonSchema tupleSchema0;
    @Mock private JsonSchema tupleSchema1;
    @Mock private WalkListenerRunner mockListenerRunner;

    private ObjectMapper mapper = new ObjectMapper();

    private ArrayNode validSchemaArray; // the JSON schema node passed to the constructor
    private PrefixItemsValidator validator; // instance under test

    @Before
    public void setUp() throws Exception {
        // Build a simple array node that will act as the "prefixItems" schema definition.
        validSchemaArray = mapper.createArrayNode();
        validSchemaArray.addObject(); // element 0
        validSchemaArray.addObject(); // element 1

        // ValidationContext#newSchema(...) must return our mocked tuple schemas.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(tupleSchema0)   // first call -> tupleSchema0
                .thenReturn(tupleSchema1);  // second call -> tupleSchema1

        // WalkListenerRunner is created inside the constructor; we will replace it later via reflection.
        // For now we let the real DefaultItemWalkListenerRunner be instantiated (it does nothing).
        validator = new PrefixItemsValidator("#/prefixItems", validSchemaArray, parentSchema, validationContext);
    }

    // ----------------------------------------------------------------------
    // Constructor tests
    // ----------------------------------------------------------------------
    @Test
    public void constructor_shouldCreateTupleSchemas_whenArrayNodeIsNonEmpty() {
        List<JsonSchema> tupleSchemas = validator.getTupleSchema();
        assertEquals("Two tuple schemas should be created", 2, tupleSchemas.size());
        assertSame(tupleSchema0, tupleSchemas.get(0));
        assertSame(tupleSchema1, tupleSchemas.get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_shouldThrow_whenSchemaNodeIsNotArray() {
        JsonNode notArray = mapper.textNode("not-an-array");
        new PrefixItemsValidator("#/prefixItems", notArray, parentSchema, validationContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_shouldThrow_whenArrayNodeIsEmpty() {
        ArrayNode emptyArray = mapper.createArrayNode(); // size == 0
        new PrefixItemsValidator("#/prefixItems", emptyArray, parentSchema, validationContext);
    }

    // ----------------------------------------------------------------------
    // validate() tests
    // ----------------------------------------------------------------------
    @Test
    public void validate_shouldIgnoreNonArrayNode_andReturnNoErrors() {
        JsonNode notArray = mapper.textNode("string");
        Set<ValidationMessage> result = validator.validate(executionContext, notArray, notArray, "$");
        assertTrue("Non‑array node must produce no errors", result.isEmpty());
    }

    @Test
    public void validate_shouldValidateEachItem_upToTupleSize_andCollectErrors() {
        // Build an array node with three items, but only two tuple schemas exist.
        ArrayNode data = mapper.createArrayNode();
        data.add(1); // index 0
        data.add(2); // index 1
        data.add(3); // index 2 – should be ignored by PrefixItemsValidator

        // Mock validation results:
        Set<ValidationMessage> emptySet = Collections.emptySet();
        ValidationMessage vm = ValidationMessage.builder().path("$[0]").message("error0").type("type0").build();
        Set<ValidationMessage> errorSet = new LinkedHashSet<>(Collections.singletonList(vm));

        when(tupleSchema0.validate(executionContext, data.get(0), data, "$[0]")).thenReturn(errorSet);
        when(tupleSchema1.validate(executionContext, data.get(1), data, "$[1]")).thenReturn(emptySet);

        // ExecutionContext must provide a collector for evaluated items.
        CollectorContext collector = mock(CollectorContext.class);
        when(executionContext.getCollectorContext()).thenReturn(collector);
        Collection<String> evaluated = new ArrayList<>();
        when(collector.getEvaluatedItems()).thenReturn(evaluated);

        Set<ValidationMessage> result = validator.validate(executionContext, data, data, "$");

        // Verify errors
        assertEquals("One error expected from first tuple schema", 1, result.size());
        assertTrue(result.contains(vm));

        // Verify that only the successful validation added its path to evaluated items.
        assertEquals("Only index 1 should be recorded as evaluated", Collections.singleton("$[1]"), new HashSet<>(evaluated));
    }

    // ----------------------------------------------------------------------
    // walk() tests – default handling & listener interaction
    // ----------------------------------------------------------------------
    @Test
    public void walk_shouldApplyDefaults_whenItemIsNull_andDefaultExists() {
        // Prepare a tuple schema whose underlying schema node contains a "default" value.
        ObjectNode schemaNode0 = mapper.createObjectNode();
        schemaNode0.set("default", mapper.textNode("default0"));
        when(tupleSchema0.getSchemaNode()).thenReturn(schemaNode0);
        when(tupleSchema0.getSchemaPath()).thenReturn("#/prefixItems/0");
        when(tupleSchema0.getParentSchema()).thenReturn(parentSchema);
        // second tuple schema has no default
        when(tupleSchema1.getSchemaNode()).thenReturn(mapper.createObjectNode());
        when(tupleSchema1.getSchemaPath()).thenReturn("#/prefixItems/1");
        when(tupleSchema1.getParentSchema()).thenReturn(parentSchema);

        // Build data array: first element is null, second is a concrete value.
        ArrayNode data = mapper.createArrayNode();
        data.add(NullNode.getInstance()); // index 0 – should be replaced by default
        data.add(mapper.numberNode(42));  // index 1 – untouched

        // Mock the walk call of each tuple schema – simply return empty set.
        when(tupleSchema0.walk(any(), any(), any(), anyString(), anyBoolean())).thenReturn(Collections.emptySet());
        when(tupleSchema1.walk(any(), any(), any(), anyString(), anyBoolean())).thenReturn(Collections.emptySet());

        // Replace the internal listener runner with a mock that always returns true for pre‑walk.
        injectMockListenerRunner();

        Set<ValidationMessage> msgs = validator.walk(executionContext, data, data, "$", false);
        assertTrue("walk should return no validation messages", msgs.isEmpty());

        // Verify that the default value was injected.
        assertEquals("default0", data.get(0).asText());

        // Verify that walk() was invoked on both tuple schemas.
        verify(tupleSchema0).walk(eq(executionContext), eq(data.get(0)), eq(data), eq("$[0]"), eq(false));
        verify(tupleSchema1).walk(eq(executionContext), eq(data.get(1)), eq(data), eq("$[1]"), eq(false));
    }

    @Test
    public void walk_shouldRespectPreAndPostListeners() {
        // No defaults needed for this test – focus on listener execution.
        when(tupleSchema0.getSchemaNode()).thenReturn(mapper.createObjectNode());
        when(tupleSchema0.getSchemaPath()).thenReturn("#/prefixItems/0");
        when(tupleSchema0.getParentSchema()).thenReturn(parentSchema);
        when(tupleSchema1.getSchemaNode()).thenReturn(mapper.createObjectNode());
        when(tupleSchema1.getSchemaPath()).thenReturn("#/prefixItems/1");
        when(tupleSchema1.getParentSchema()).thenReturn(parentSchema);

        // Stub walk() to return a non‑empty set for the first schema.
        ValidationMessage vm = ValidationMessage.builder().path("$[0]").message("msg").type("type").build();
        when(tupleSchema0.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.singleton(vm));
        when(tupleSchema1.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        // Configure mock listener runner:
        // pre‑walk returns true for first item, false for second (so second schema walk is skipped).
        when(mockListenerRunner.runPreWalkListeners(
                eq(executionContext),
                eq(ValidatorTypeCode.PREFIX_ITEMS.getValue()),
                any(JsonNode.class), any(JsonNode.class), anyString(),
                anyString(), any(JsonNode.class), any(JsonSchema.class),
                eq(validationContext), any()))
                .thenAnswer(invocation -> {
                    String at = invocation.getArgument(4);
                    // only allow walk for index 0
                    return at.endsWith("[0]");
                });

        // post‑walk should be called exactly once (for index 0).
        doNothing().when(mockListenerRunner).runPostWalkListeners(
                eq(executionContext),
                eq(ValidatorTypeCode.PREFIX_ITEMS.getValue()),
                any(JsonNode.class), any(JsonNode.class), anyString(),
                anyString(), any(JsonNode.class), any(JsonSchema.class),
                eq(validationContext), any(), anySet());

        // Inject the mocked listener runner.
        injectMockListenerRunner();

        // Data array with two items.
        ArrayNode data = mapper.createArrayNode();
        data.add(mapper.textNode("a"));
        data.add(mapper.textNode("b"));

        Set<ValidationMessage> msgs = validator.walk(executionContext, data, data, "$", true);
        assertEquals("Only the first tuple schema should have contributed messages", 1, msgs.size());
        assertTrue(msgs.contains(vm));

        // Verify listener interactions.
        verify(mockListenerRunner, times(2)).runPreWalkListeners(
                eq(executionContext),
                eq(ValidatorTypeCode.PREFIX_ITEMS.getValue()),
                any(JsonNode.class), any(JsonNode.class), anyString(),
                anyString(), any(JsonNode.class), any(JsonSchema.class),
                eq(validationContext), any());

        verify(mockListenerRunner, times(1)).runPostWalkListeners(
                eq(executionContext),
                eq(ValidatorTypeCode.PREFIX_ITEMS.getValue()),
                any(JsonNode.class), any(JsonNode.class), anyString(),
                anyString(), any(JsonNode.class), any(JsonSchema.class),
                eq(validationContext), any(), anySet());
    }

    // ----------------------------------------------------------------------
    // Helper – inject mocked WalkListenerRunner via reflection
    // ----------------------------------------------------------------------
    private void injectMockListenerRunner() throws Exception {
        java.lang.reflect.Field field = PrefixItemsValidator.class.getDeclaredField("arrayItemWalkListenerRunner");
        field.setAccessible(true);
        field.set(validator, mockListenerRunner);
    }
}
