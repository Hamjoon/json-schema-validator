package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_walk_Test {

    // ----- Mocks shared by all tests -------------------------------------------------
    @Mock
    ValidationContext validationContext;

    @Mock
    ExecutionContext executionContext;

    @Mock
    JsonNode rootNode;

    @Mock
    JsonNodePath instanceLocation;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonSchema parentSchema;

    // represents the schemaNode passed to the validator
    @Mock
    JsonNode anyOfArrayNode;

    // individual subschema nodes inside the array
    @Mock
    JsonNode subschemaNode0;

    @Mock
    JsonNode subschemaNode1;

    // the JsonSchema objects created by ValidationContext
    @Mock
    JsonSchema subschemaValidator0;

    @Mock
    JsonSchema subschemaValidator1;

    // object under test
    AnyOfValidator anyOfValidator;

    @BeforeEach
    void setupBeforeEach() {
        // ----- Prepare the "anyOf" array node ---------------------------------------
        when(anyOfArrayNode.size()).thenReturn(2);
        when(anyOfArrayNode.get(0)).thenReturn(subschemaNode0);
        when(anyOfArrayNode.get(1)).thenReturn(subschemaNode1);
        // ----- Stub ValidationContext#newSchema to return our mock subschema validators
        // The real method signature is:
        //   JsonSchema newSchema(SchemaLocation location, JsonNodePath path, JsonNode node,
        //                       JsonSchema parent)
        // We only need to match the call arguments loosely.
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), eq(subschemaNode0), any(JsonSchema.class))).thenReturn(subschemaValidator0);
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), eq(subschemaNode1), any(JsonSchema.class))).thenReturn(subschemaValidator1);
        // ----- Build the validator under test ---------------------------------------
        anyOfValidator = new AnyOfValidator(schemaLocation, evaluationPath, anyOfArrayNode, parentSchema, validationContext);
    }

    @Test
    void walk_ShouldValidateSchema_True_InvokesValidateAndReturnsItsResult() {
        // Arrange: spy the validator so we can stub validate()
        AnyOfValidator spyValidator = spy(anyOfValidator);
        Set<ValidationMessage> expected = new LinkedHashSet<>();
        expected.add(mock(ValidationMessage.class));
        doReturn(expected).when(spyValidator).validate(executionContext, mock(JsonNode.class), rootNode, instanceLocation);
        // Act
        Set<ValidationMessage> actual = spyValidator.walk(executionContext, mock(JsonNode.class), rootNode, instanceLocation, true);
        // Assert
        assertSame(expected, actual, "When shouldValidateSchema is true, walk should return the result of validate()");
        verify(spyValidator, times(1)).validate(executionContext, any(JsonNode.class), eq(rootNode), eq(instanceLocation));
        // No schema.walk calls should happen when validation is performed directly
        verifyNoInteractions(subschemaValidator0, subschemaValidator1);
    }

    @Test
    void walk_ShouldValidateSchema_False_InvokesEachSubschemaWalkAndReturnsEmptySet() {
        // Arrange: stub each subschema's walk method (they return arbitrary non‑empty sets)
        Set<ValidationMessage> dummySet0 = new LinkedHashSet<>(Collections.singleton(mock(ValidationMessage.class)));
        Set<ValidationMessage> dummySet1 = new LinkedHashSet<>(Collections.singleton(mock(ValidationMessage.class)));
        when(subschemaValidator0.walk(executionContext, mock(JsonNode.class), rootNode, instanceLocation, false)).thenReturn(dummySet0);
        when(subschemaValidator1.walk(executionContext, mock(JsonNode.class), rootNode, instanceLocation, false)).thenReturn(dummySet1);
        // the node being validated
        JsonNode testNode = mock(JsonNode.class);
        // Act
        Set<ValidationMessage> result = anyOfValidator.walk(executionContext, testNode, rootNode, instanceLocation, false);
        // Assert
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "When shouldValidateSchema is false, AnyOfValidator should return an empty set");
        // Verify each subschema's walk was invoked exactly once with shouldValidateSchema = false
        verify(subschemaValidator0, times(1)).walk(executionContext, testNode, rootNode, instanceLocation, false);
        verify(subschemaValidator1, times(1)).walk(executionContext, testNode, rootNode, instanceLocation, false);
    }

    @Test
    void walk_ShouldValidateSchema_True_ValidateReturnsNonEmptySet_IsPropagated() {
        // Spy to return a non‑empty set from validate()
        AnyOfValidator spy = spy(anyOfValidator);
        Set<ValidationMessage> nonEmpty = new LinkedHashSet<>();
        nonEmpty.add(mock(ValidationMessage.class));
        doReturn(nonEmpty).when(spy).validate(executionContext, mock(JsonNode.class), rootNode, instanceLocation);
        Set<ValidationMessage> actual = spy.walk(executionContext, mock(JsonNode.class), rootNode, instanceLocation, true);
        assertEquals(nonEmpty, actual, "The exact set returned by validate() must be returned unchanged");
    }

    @Test
    void walk_ShouldValidateSchema_True_ValidateReturnsEmptySet_ReturnsEmptySet() {
        AnyOfValidator spy = spy(anyOfValidator);
        Set<ValidationMessage> empty = Collections.emptySet();
        doReturn(empty).when(spy).validate(executionContext, mock(JsonNode.class), rootNode, instanceLocation);
        Set<ValidationMessage> actual = spy.walk(executionContext, mock(JsonNode.class), rootNode, instanceLocation, true);
        assertEquals(empty, actual, "Empty set from validate() must be returned unchanged");
    }

    @Test
    void walk_ShouldValidateSchema_True_ValidateReturnsNull_ReturnsEmptySetDefensively() {
        AnyOfValidator spy = spy(anyOfValidator);
        doReturn(null).when(spy).validate(executionContext, mock(JsonNode.class), rootNode, instanceLocation);
        Set<ValidationMessage> actual = spy.walk(executionContext, mock(JsonNode.class), rootNode, instanceLocation, true);
        assertNotNull(actual, "walk should never return null even if validate() returns null");
        assertTrue(actual.isEmpty(), "When validate() returns null, walk must treat it as an empty set");
    }

    @Test
    void walk_ShouldValidateSchema_True_ValidateThrowsRuntimeException_Propagates() {
        AnyOfValidator spy = spy(anyOfValidator);
        RuntimeException boom = new RuntimeException("boom");
        doThrow(boom).when(spy).validate(executionContext, mock(JsonNode.class), rootNode, instanceLocation);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> spy.walk(executionContext, mock(JsonNode.class), rootNode, instanceLocation, true));
        assertSame(boom, thrown, "Exception from validate() must propagate unchanged");
    }

    @Test
    void walk_ShouldValidateSchema_False_EmptySchemasList_ReturnsEmptySet() throws Exception {
        // Create a validator whose anyOf array node is empty
        JsonNode emptyArrayNode = mock(JsonNode.class);
        when(emptyArrayNode.size()).thenReturn(0);
        AnyOfValidator validator = new AnyOfValidator(schemaLocation, evaluationPath, emptyArrayNode, parentSchema, validationContext);
        Set<ValidationMessage> result = validator.walk(executionContext, mock(JsonNode.class), rootNode, instanceLocation, false);
        assertNotNull(result);
        assertTrue(result.isEmpty(), "With no subschemas the result must be an empty set");
    }

    @Test
    void walk_ShouldValidateSchema_False_DuplicateSchemas_InvokesEachOccurrence() throws Exception {
        // Force the internal list to contain the same validator twice
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonSchema> list = (List<JsonSchema>) f.get(anyOfValidator);
        list.clear();
        list.add(subschemaValidator0);
        // duplicate
        list.add(subschemaValidator0);
        JsonNode node = mock(JsonNode.class);
        when(subschemaValidator0.walk(executionContext, node, rootNode, instanceLocation, false)).thenReturn(new LinkedHashSet<>());
        Set<ValidationMessage> result = anyOfValidator.walk(executionContext, node, rootNode, instanceLocation, false);
        assertTrue(result.isEmpty(), "Result must be empty regardless of inner walk results");
        // Verify walk called twice (once per occurrence)
        verify(subschemaValidator0, times(2)).walk(executionContext, node, rootNode, instanceLocation, false);
    }

    @Test
    void walk_ShouldValidateSchema_False_SchemasListContainsNull_ThrowsNullPointerException() throws Exception {
        // Insert a null entry into the internal schemas list
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonSchema> list = (List<JsonSchema>) f.get(anyOfValidator);
        list.clear();
        // null element
        list.add(null);
        JsonNode node = mock(JsonNode.class);
        assertThrows(NullPointerException.class, () -> anyOfValidator.walk(executionContext, node, rootNode, instanceLocation, false));
    }

    @Test
    void walk_ShouldValidateSchema_False_SubSchemaWalkThrowsRuntimeException_Propagates() {
        JsonNode node = mock(JsonNode.class);
        RuntimeException boom = new RuntimeException("boom");
        when(subschemaValidator0.walk(executionContext, node, rootNode, instanceLocation, false)).thenThrow(boom);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> anyOfValidator.walk(executionContext, node, rootNode, instanceLocation, false));
        assertSame(boom, thrown, "RuntimeException from inner schema.walk must propagate");
    }

    @Test
    void walk_NullNodeParameter_ShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> anyOfValidator.walk(executionContext, null, rootNode, instanceLocation, false));
    }

    @Test
    void walk_NullExecutionContext_ShouldValidateSchemaTrue_ThrowsNPE() {
        AnyOfValidator spy = spy(anyOfValidator);
        doCallRealMethod().when(spy).validate(isNull(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class));
        // The real validate implementation is unknown; we just expect NPE when it dereferences the context
        assertThrows(NullPointerException.class, () -> spy.walk(null, mock(JsonNode.class), rootNode, instanceLocation, true));
    }

    @Test
    void walk_NullExecutionContext_ShouldValidateSchemaFalse_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> anyOfValidator.walk(null, mock(JsonNode.class), rootNode, instanceLocation, false));
    }

    @Test
    void walk_NullRootNode_ShouldValidateSchemaTrue_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> anyOfValidator.walk(executionContext, mock(JsonNode.class), null, instanceLocation, true));
    }

    @Test
    void walk_NullRootNode_ShouldValidateSchemaFalse_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> anyOfValidator.walk(executionContext, mock(JsonNode.class), null, instanceLocation, false));
    }

    @Test
    void walk_NullInstanceLocation_ShouldValidateSchemaTrue_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> anyOfValidator.walk(executionContext, mock(JsonNode.class), rootNode, null, true));
    }

    @Test
    void walk_NullInstanceLocation_ShouldValidateSchemaFalse_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> anyOfValidator.walk(executionContext, mock(JsonNode.class), rootNode, null, false));
    }
}
