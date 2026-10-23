package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PrefixItemsValidator}.
 *
 * <p>These tests cover:
 * <ul>
 *   <li>Constructor validation (non‑empty array, empty array, non‑array)</li>
 *   <li>{@link PrefixItemsValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}</li>
 *   <li>{@link PrefixItemsValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}</li>
 *   <li>{@link PrefixItemsValidator#getTupleSchema()}</li>
 *   <li>{@link PrefixItemsValidator#preloadJsonSchema()}</li>
 * </ul>
 *
 * <p>All external collaborators are mocked with Mockito to keep the tests focused on the
 * behaviour of {@code PrefixItemsValidator} itself.
 */
@RunWith(MockitoJUnitRunner.class)
public class PrefixItemsValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonSchema parentSchema;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private WalkListenerRunner walkListenerRunner;

    @Captor
    private ArgumentCaptor<Set<ValidationMessage>> validationMessageCaptor;

    private PrefixItemsValidator validator;

    @Before
    public void setUp() {
        // Default behaviour for the validation context – needed by the constructor
        when(validationContext.getConfig()).thenReturn(new ValidationConfiguration());
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenAnswer(invocation -> mock(JsonSchema.class));

        // ExecutionContext returns a collector context that holds the evaluated items set
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.getEvaluatedItems()).thenReturn(new LinkedHashSet<>());
    }

    /**
     * Helper that creates a simple {@code PrefixItemsValidator} with the given schema array.
     */
    private PrefixItemsValidator createValidatorWithSchemaArray(ArrayNode schemaArray) {
        // The constructor validates the array content, so we must provide a non‑null, non‑empty array.
        return new PrefixItemsValidator("testPath", schemaArray, parentSchema, validationContext);
    }

    // -------------------------------------------------------------------------
    // Constructor tests
    // -------------------------------------------------------------------------

    @Test
    public void constructor_withNonEmptyArray_initialisesTupleSchema() {
        ArrayNode schemaArray = MAPPER.createArrayNode();
        schemaArray.addObject(); // first tuple schema
        schemaArray.addObject(); // second tuple schema

        PrefixItemsValidator v = createValidatorWithSchemaArray(schemaArray);
        assertEquals(2, v.getTupleSchema().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_withEmptyArray_throwsException() {
        ArrayNode emptyArray = MAPPER.createArrayNode(); // size == 0
        new PrefixItemsValidator("testPath", emptyArray, parentSchema, validationContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_withNonArrayNode_throwsException() {
        JsonNode notArray = MAPPER.createObjectNode(); // an object, not an array
        new PrefixItemsValidator("testPath", notArray, parentSchema, validationContext);
    }

    // -------------------------------------------------------------------------
    // validate() tests
    // -------------------------------------------------------------------------

    @Test
    public void validate_nonArrayNode_returnsEmptySet() {
        // Build a validator with a single tuple schema (mocked)
        ArrayNode schemaArray = MAPPER.createArrayNode();
        schemaArray.addObject();
        validator = createValidatorWithSchemaArray(schemaArray);

        JsonNode nonArrayNode = MAPPER.createObjectNode(); // not an array

        Set<ValidationMessage> result = validator.validate(executionContext, nonArrayNode,
                nonArrayNode, "$");
        assertTrue(result.isEmpty());

        // Ensure that no evaluated items were added
        verify(collectorContext, never()).getEvaluatedItems();
    }

    @Test
    public void validate_arrayAllValid_returnsEmptySetAndMarksEvaluatedItems() {
        // Prepare two tuple schemas that always validate successfully
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        when(schema0.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
        when(schema1.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());

        // Stub validationContext to return the prepared schemas
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schema0, schema1);

        // Build validator
        ArrayNode schemaArray = MAPPER.createArrayNode();
        schemaArray.addObject();
        schemaArray.addObject();
        validator = createValidatorWithSchemaArray(schemaArray);

        // Create an array node with two items (matching the tuple length)
        ArrayNode instance = MAPPER.createArrayNode();
        instance.add("a");
        instance.add("b");

        Set<ValidationMessage> result = validator.validate(executionContext, instance, instance, "$");
        assertTrue(result.isEmpty());

        // Verify that both paths were added to evaluated items
        Set<String> evaluated = collectorContext.getEvaluatedItems();
        assertEquals(new LinkedHashSet<>(Arrays.asList("$.0", "$.1")), evaluated);
    }

    @Test
    public void validate_arrayWithInvalidItem_returnsErrorsAndDoesNotMarkEvaluated() {
        // First schema validates, second schema returns an error
        JsonSchema schema0 = mock(JsonSchema.class);
        JsonSchema schema1 = mock(JsonSchema.class);
        when(schema0.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        ValidationMessage vm = mock(ValidationMessage.class);
        Set<ValidationMessage> errorSet = new LinkedHashSet<>();
        errorSet.add(vm);
        when(schema1.validate(any(), any(), any(), anyString()))
                .thenReturn(errorSet);

        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schema0, schema1);

        ArrayNode schemaArray = MAPPER.createArrayNode();
        schemaArray.addObject();
        schemaArray.addObject();
        validator = createValidatorWithSchemaArray(schemaArray);

        ArrayNode instance = MAPPER.createArrayNode();
        instance.add("valid");
        instance.add("invalid");

        Set<ValidationMessage> result = validator.validate(executionContext, instance, instance, "$");
        assertEquals(1, result.size());
        assertTrue(result.contains(vm));

        // Only the first path should be marked as evaluated
        Set<String> evaluated = collectorContext.getEvaluatedItems();
        assertEquals(Collections.singleton("$.0"), evaluated);
    }

    // -------------------------------------------------------------------------
    // walk() tests – default handling and listener delegation
    // -------------------------------------------------------------------------

    @Test
    public void walk_arrayWithNullAndDefault_appliesDefaultAndWalksSchema() {
        // Mock a tuple schema that has a "default" property and a walk() implementation
        JsonSchema tupleSchema = mock(JsonSchema.class);
        ObjectNode tupleSchemaNode = MAPPER.createObjectNode();
        tupleSchemaNode.set("default", MAPPER.getNodeFactory().textNode("defaultValue"));
        when(tupleSchema.getSchemaNode()).thenReturn(tupleSchemaNode);
        when(tupleSchema.getSchemaPath()).thenReturn("schemaPath");
        when(tupleSchema.getParentSchema()).thenReturn(parentSchema);
        when(tupleSchema.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Collections.emptySet());

        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(tupleSchema);
        when(validationContext.getConfig()).thenReturn(new ValidationConfiguration());

        // Build validator with a single tuple schema
        ArrayNode schemaArray = MAPPER.createArrayNode();
        schemaArray.addObject(); // placeholder – the actual node is supplied by the mock above
        validator = createValidatorWithSchemaArray(schemaArray);

        // Inject a mock WalkListenerRunner that simply returns true for pre‑walk (so walk() is executed)
        WalkListenerRunner mockRunner = mock(WalkListenerRunner.class);
        when(mockRunner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(), any(), any(), any()))
                .thenReturn(true);
        // The post‑walk listener does nothing special for this test
        doNothing().when(mockRunner).runPostWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any(), any());

        // Replace the internal runner via reflection (since the field is package‑private)
        //noinspection unchecked
        validator.arrayItemWalkListenerRunner = mockRunner;

        // Create an array instance with a single null element
        ArrayNode instance = MAPPER.createArrayNode();
        instance.add(NullNode.getInstance());

        // Ensure defaults strategy is enabled – the default strategy in BaseJsonValidator applies defaults
        // (no extra setup required for the test)

        Set<ValidationMessage> walkResult = validator.walk(executionContext, instance, instance, "$", true);
        assertTrue(walkResult.isEmpty());

        // The null element should have been replaced by the default value
        assertEquals("defaultValue", instance.get(0).asText());

        // Verify that the tuple schema's walk method was invoked
        verify(tupleSchema).walk(eq(executionContext), any(JsonNode.class), eq(instance), eq("$.0"), eq(true));
    }

    @Test
    public void walk_nonArrayOrDefaultsNotApplied_doesNothing() {
        // Build a validator with a dummy tuple schema
        JsonSchema dummySchema = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(dummySchema);
        ArrayNode schemaArray = MAPPER.createArrayNode();
        schemaArray.addObject();
        validator = createValidatorWithSchemaArray(schemaArray);

        // Replace the listener runner with a mock that would fail the test if invoked
        WalkListenerRunner mockRunner = mock(WalkListenerRunner.class);
        validator.arrayItemWalkListenerRunner = mockRunner;

        // 1) Pass a non‑array node – walk should return immediately
        JsonNode nonArray = MAPPER.createObjectNode();
        Set<ValidationMessage> result = validator.walk(executionContext, nonArray, nonArray, "$", true);
        assertTrue(result.isEmpty());

        // 2) Pass an empty array – nothing to walk, but still should not invoke listeners
        ArrayNode emptyArray = MAPPER.createArrayNode();
        result = validator.walk(executionContext, emptyArray, emptyArray, "$", true);
        assertTrue(result.isEmpty());

        // Verify that the listener runner was never called
        verifyNoInteractions(mockRunner);
    }

    // -------------------------------------------------------------------------
    // Miscellaneous public methods
    // -------------------------------------------------------------------------

    @Test
    public void getTupleSchema_returnsInternalListReference() {
        ArrayNode schemaArray = MAPPER.createArrayNode();
        schemaArray.addObject();
        schemaArray.addObject();
        validator = createValidatorWithSchemaArray(schemaArray);

        List<JsonSchema> tuple = validator.getTupleSchema();
        assertEquals(2, tuple.size());

        // The returned list should be the same instance used internally (modifications affect the validator)
        tuple.add(mock(JsonSchema.class));
        assertEquals(3, validator.getTupleSchema().size());
    }

    @Test
    public void preloadJsonSchema_invokesBaseMethodWithoutException() {
        // The BaseJsonValidator implementation simply iterates over the list,
        // so we only need to ensure that no exception is thrown.
        ArrayNode schemaArray = MAPPER.createArrayNode();
        schemaArray.addObject();
        validator = createValidatorWithSchemaArray(schemaArray);

        // No explicit verification – the absence of an exception is sufficient
        validator.preloadJsonSchema();
    }
}
