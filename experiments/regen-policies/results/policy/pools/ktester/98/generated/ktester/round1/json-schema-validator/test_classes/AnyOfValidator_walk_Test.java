package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_walk_Test {

    /**
     * Common collaborators used by every test.
     */
    @Mock
    ExecutionContext executionContext;

    @Mock
    JsonNode node;

    @Mock
    JsonNode rootNode;

    @Mock
    JsonNodePath instanceLocation;

    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    // can be {@code null} but a mock keeps the constructor happy
    @Mock
    JsonSchema parentSchema;

    /**
     * Two subschema mocks that the {@link AnyOfValidator} will hold.
     */
    @Mock
    JsonSchema subSchema1;

    @Mock
    JsonSchema subSchema2;

    /**
     * The validator under test – created in {@link #setupBeforeEach()}.
     */
    AnyOfValidator validator;

    @BeforeEach
    void setupBeforeEach() {
        // Stub the ValidationContext so that it returns our two mocked subschemas.
        when(validationContext.newSchema(any(), any(), any(), any())).thenReturn(// first call
        subSchema1).thenReturn(// second call
        subSchema2);
        // Build the AnyOfValidator instance with the mocked collaborators.
        validator = new AnyOfValidator(schemaLocation, // evaluationPath – can be a simple instance
        new JsonNodePath(JsonNodePath.PathType.JSON_POINTER), anyOfSchemaNode(), parentSchema, validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid cross‑test interference.
        reset(executionContext, node, rootNode, instanceLocation, validationContext, schemaLocation, parentSchema, subSchema1, subSchema2);
    }

    /**
     * A minimal JSON array node required by the {@code AnyOfValidator} constructor.
     */
    private static ArrayNode anyOfSchemaNode() {
        // [{}, {}] – two empty object schemas
        return JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode()).add(JsonNodeFactory.instance.objectNode());
    }

    /**
     * Helper to create an empty array node (no subschemas).
     */
    private static ArrayNode emptySchemaNode() {
        // [] – no elements
        return JsonNodeFactory.instance.arrayNode();
    }

    @Test
    void walkWhenShouldValidateSchemaTrue_ShouldDelegateToValidate() {
        // Arrange – we spy the validator so that we can stub the protected validate method.
        AnyOfValidator spyValidator = Mockito.spy(validator);
        // Prepare a dummy ValidationMessage set that the stub will return.
        ValidationMessage dummyMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> expected = new LinkedHashSet<>(Collections.singleton(dummyMessage));
        // Stub the protected validate method (the overload with the extra boolean flag).
        doReturn(expected).when(spyValidator).validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        // Act
        Set<ValidationMessage> actual = spyValidator.walk(executionContext, node, rootNode, instanceLocation, true);
        // Assert – the result must be exactly the stubbed set.
        assertSame(expected, actual, "walk(true) should return the set produced by validate(..., true)");
        // Verify that validate was called once with the correct arguments.
        verify(spyValidator, times(1)).validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        // No subschema walk should have happened.
        verifyNoInteractions(subSchema1, subSchema2);
    }

    @Test
    void walkWhenShouldValidateSchemaFalse_ShouldWalkAllSubSchemasAndReturnEmptySet() {
        // Arrange – nothing special; the subschemas are already mocked.
        // Act
        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode, instanceLocation, false);
        // Assert – the method must return an empty LinkedHashSet.
        assertTrue(result instanceof LinkedHashSet, "Result should be a LinkedHashSet");
        assertTrue(result.isEmpty(), "Result should be empty when shouldValidateSchema is false");
        // Verify that each subschema's walk method was invoked exactly once with shouldValidateSchema = false.
        verify(subSchema1, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        verify(subSchema2, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
    }

    @Test
    void walkWhenShouldValidateSchemaFalse_WithTwoSubSchemas_ShouldWalkAllAndReturnEmptySet() {
        // Act
        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode, instanceLocation, false);
        // Assert – result must be an empty LinkedHashSet.
        assertTrue(result instanceof LinkedHashSet, "Result should be a LinkedHashSet");
        assertTrue(result.isEmpty(), "Result should be empty when shouldValidateSchema is false");
        // Verify each subschema's walk was invoked exactly once with shouldValidateSchema = false.
        verify(subSchema1, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        verify(subSchema2, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
    }

    @Test
    void walkWhenShouldValidateSchemaFalse_WithEmptySchemas_ShouldReturnEmptySetAndNoWalks() {
        // Build a validator that holds an empty schema list.
        AnyOfValidator emptyValidator = new AnyOfValidator(schemaLocation, new JsonNodePath(JsonNodePath.PathType.JSON_POINTER), emptySchemaNode(), parentSchema, validationContext);
        // Act
        Set<ValidationMessage> result = emptyValidator.walk(executionContext, node, rootNode, instanceLocation, false);
        // Assert
        assertTrue(result instanceof LinkedHashSet, "Result should be a LinkedHashSet");
        assertTrue(result.isEmpty(), "Result should be empty when there are no subschemas");
        // No interaction with the previously mocked subschemas because they were never added.
        verifyNoInteractions(subSchema1, subSchema2);
    }

    @Test
    void walkWhenShouldValidateSchemaTrue_NullExecutionContext_ShouldThrowNPE() {
        AnyOfValidator spy = Mockito.spy(validator);
        // Force the protected validate method to throw NPE when the executionContext is null.
        doThrow(new NullPointerException("executionContext is null")).when(spy).validate(isNull(), any(), any(), any(), eq(true));
        assertThrows(NullPointerException.class, () -> spy.walk(null, node, rootNode, instanceLocation, true), "walk(true) with null executionContext should propagate NullPointerException");
    }

    @Test
    void walkWhenShouldValidateSchemaTrue_NullNode_ShouldThrowNPE() {
        AnyOfValidator spy = Mockito.spy(validator);
        doThrow(new NullPointerException("node is null")).when(spy).validate(eq(executionContext), isNull(), any(), any(), eq(true));
        assertThrows(NullPointerException.class, () -> spy.walk(executionContext, null, rootNode, instanceLocation, true), "walk(true) with null node should propagate NullPointerException");
    }

    @Test
    void walkWhenShouldValidateSchemaTrue_NullRootNode_ShouldThrowNPE() {
        AnyOfValidator spy = Mockito.spy(validator);
        doThrow(new NullPointerException("rootNode is null")).when(spy).validate(eq(executionContext), any(), isNull(), any(), eq(true));
        assertThrows(NullPointerException.class, () -> spy.walk(executionContext, node, null, instanceLocation, true), "walk(true) with null rootNode should propagate NullPointerException");
    }

    @Test
    void walkWhenShouldValidateSchemaTrue_NullInstanceLocation_ShouldThrowNPE() {
        AnyOfValidator spy = Mockito.spy(validator);
        doThrow(new NullPointerException("instanceLocation is null")).when(spy).validate(eq(executionContext), any(), any(), isNull(), eq(true));
        assertThrows(NullPointerException.class, () -> spy.walk(executionContext, node, rootNode, null, true), "walk(true) with null instanceLocation should propagate NullPointerException");
    }

    @Test
    void walkWhenShouldValidateSchemaFalse_NullSchemasField_ShouldThrowNPE() throws Exception {
        // Use reflection to set the private final field `schemas` to null.
        Field schemasField = AnyOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(validator, null);
        assertThrows(NullPointerException.class, () -> validator.walk(executionContext, node, rootNode, instanceLocation, false), "walk(false) with internal schemas == null should throw NullPointerException");
    }

    @Test
    void walkWhenShouldValidateSchemaFalse_NullElementInSchemas_ShouldThrowNPE() throws Exception {
        // Replace the schemas list with a list that contains a single null entry.
        Field schemasField = AnyOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(validator, List.of((JsonSchema) null));
        assertThrows(NullPointerException.class, () -> validator.walk(executionContext, node, rootNode, instanceLocation, false), "walk(false) with a null element in schemas list should throw NullPointerException");
    }

    @Test
    void walkWhenShouldValidateSchemaFalse_ChildSchemaThrowsRuntimeException_ShouldPropagate() {
        // Make the first subschema throw a RuntimeException when its walk method is called.
        doThrow(new RuntimeException("child schema failure")).when(subSchema1).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> validator.walk(executionContext, node, rootNode, instanceLocation, false), "Exception from child schema.walk should propagate");
        assertEquals("child schema failure", thrown.getMessage());
        // Verify that the second subschema was never invoked because the first already threw.
        verify(subSchema2, never()).walk(any(), any(), any(), any(), any());
    }
}
