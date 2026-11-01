package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_walk_Test {

    /**
     * Objects that are shared between the tests.
     */
    private static ObjectMapper mapper;

    /**
     * Instance under test (spied) and its collaborators.
     */
    private AnyOfValidator anyOfValidator;

    private AnyOfValidator anyOfValidatorSpy;

    private ValidationContext validationContext;

    private JsonSchema schemaMock1;

    private JsonSchema schemaMock2;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        /* ---------- create the JSON array that represents the anyOf keyword ---------- */
        JsonNode anyOfArrayNode;
        try {
            anyOfArrayNode = mapper.readTree("[{\"type\":\"string\"},{\"type\":\"integer\"}]");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /* ---------- mock the constructor arguments ---------- */
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        // not needed for the behaviour we test
        JsonSchema parentSchema = null;
        validationContext = mock(ValidationContext.class);
        schemaMock1 = mock(JsonSchema.class);
        schemaMock2 = mock(JsonSchema.class);
        /* newSchema is called once per element of the anyOf array */
        when(validationContext.newSchema(any(), any(), any(), any())).thenReturn(schemaMock1, schemaMock2);
        /* ---------- create the validator and spy on it ---------- */
        anyOfValidator = new AnyOfValidator(schemaLocation, evaluationPath, anyOfArrayNode, parentSchema, validationContext);
        anyOfValidatorSpy = spy(anyOfValidator);
    }

    @Test
    void walkShouldValidateWhenFlagTrueAndNodeNotNull() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode rootNode = mock(JsonNode.class);
        JsonNodePath instanceLocation = mock(JsonNodePath.class);
        ValidationMessage validationMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> expected = Collections.singleton(validationMessage);
        // Stub the protected validate method (via the spy) to return a known set
        doReturn(expected).when(anyOfValidatorSpy).validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        // Act
        Set<ValidationMessage> actual = anyOfValidatorSpy.walk(executionContext, node, rootNode, instanceLocation, true);
        // Assert
        assertSame(expected, actual, "walk should return the result of validate when shouldValidateSchema is true");
        verify(anyOfValidatorSpy, times(1)).validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        // No schema walk should be triggered
        verifyNoInteractions(schemaMock1, schemaMock2);
    }

    @Test
    void walkShouldDelegateToSchemasWhenFlagFalse() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode rootNode = mock(JsonNode.class);
        JsonNodePath instanceLocation = mock(JsonNodePath.class);
        // The schemas' walk method returns an empty set – the validator itself also returns empty.
        when(schemaMock1.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());
        when(schemaMock2.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());
        // Act
        Set<ValidationMessage> result = anyOfValidatorSpy.walk(executionContext, node, rootNode, instanceLocation, false);
        // Assert
        assertTrue(result.isEmpty(), "walk should return an empty set when shouldValidateSchema is false");
        // Verify that each schema's walk method was invoked with shouldValidateSchema = false
        verify(schemaMock1, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        verify(schemaMock2, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        // The validator's own validate method must NOT be called
        verify(anyOfValidatorSpy, never()).validate(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void walkShouldReturnEmptyWhenSchemasEmptyAndFlagFalse() throws Exception {
        // Build a validator whose anyOf array is empty → schemas list will be empty
        JsonNode emptyArray = mapper.readTree("[]");
        AnyOfValidator validator = new AnyOfValidator(mock(SchemaLocation.class), mock(JsonNodePath.class), emptyArray, null, validationContext);
        AnyOfValidator spy = spy(validator);
        ExecutionContext ctx = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        JsonNodePath loc = mock(JsonNodePath.class);
        // Act
        Set<ValidationMessage> result = spy.walk(ctx, node, root, loc, false);
        // Assert
        assertTrue(result.isEmpty(), "With an empty schemas list and flag false, walk must return empty set");
        // No schema interactions because the list is empty
        verifyNoInteractions(schemaMock1, schemaMock2);
    }

    @Test
    void walkShouldThrowNPEWhenSchemasFieldIsNull() throws Exception {
        // Set the private 'schemas' field to null via reflection
        Field schemasField = AnyOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(anyOfValidatorSpy, null);
        ExecutionContext ctx = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        JsonNodePath loc = mock(JsonNodePath.class);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> anyOfValidatorSpy.walk(ctx, node, root, loc, false), "When internal schemas list is null, a NullPointerException is expected");
    }

    @Test
    void walkShouldThrowNPEWhenSchemasContainsNullElement() throws Exception {
        // Replace the schemas list with a list that contains a null entry
        Field schemasField = AnyOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(anyOfValidatorSpy, Arrays.asList(schemaMock1, null, schemaMock2));
        ExecutionContext ctx = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        JsonNodePath loc = mock(JsonNodePath.class);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> anyOfValidatorSpy.walk(ctx, node, root, loc, false), "Iterating over a schemas list containing null should cause NPE");
    }

    @Test
    void walkShouldSkipValidateWhenNodeIsNullEvenIfFlagTrue() {
        // Arrange
        ExecutionContext ctx = mock(ExecutionContext.class);
        // explicitly null
        JsonNode node = null;
        JsonNode root = null;
        JsonNodePath loc = mock(JsonNodePath.class);
        // Stub schema walks to verify they are called with shouldValidateSchema = false
        when(schemaMock1.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());
        when(schemaMock2.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());
        // Act
        Set<ValidationMessage> result = anyOfValidatorSpy.walk(ctx, node, root, loc, true);
        // Assert
        assertTrue(result.isEmpty(), "When node is null, walk must return empty set even if flag is true");
        verify(anyOfValidatorSpy, never()).validate(any(), any(), any(), any(), anyBoolean());
        verify(schemaMock1, times(1)).walk(eq(ctx), eq(node), eq(root), eq(loc), eq(false));
        verify(schemaMock2, times(1)).walk(eq(ctx), eq(node), eq(root), eq(loc), eq(false));
    }

    @Test
    void walkShouldPropagateNullPointerExceptionWhenExecutionContextIsNull() {
        // Force the protected validate method to throw NPE when executionContext is null
        doThrow(new NullPointerException("executionContext is null")).when(anyOfValidatorSpy).validate(isNull(), any(), any(), any(), eq(true));
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        JsonNodePath loc = mock(JsonNodePath.class);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> anyOfValidatorSpy.walk(null, node, root, loc, true), "Null executionContext should cause NPE from validate");
    }

    @Test
    void walkShouldPropagateNullPointerExceptionWhenInstanceLocationIsNull() {
        // Force validate to NPE when instanceLocation is null
        doThrow(new NullPointerException("instanceLocation is null")).when(anyOfValidatorSpy).validate(any(), any(), any(), isNull(), eq(true));
        ExecutionContext ctx = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> anyOfValidatorSpy.walk(ctx, node, root, null, true), "Null instanceLocation should cause NPE from validate");
    }

    @Test
    void walkShouldPropagateUncheckedExceptionFromValidate() {
        // Mock validate to throw an unchecked exception
        doThrow(new IllegalStateException("boom")).when(anyOfValidatorSpy).validate(any(), any(), any(), any(), eq(true));
        ExecutionContext ctx = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        JsonNodePath loc = mock(JsonNodePath.class);
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> anyOfValidatorSpy.walk(ctx, node, root, loc, true), "Unchecked exception from validate must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void walkShouldReturnValidateResultWhenValidateReturnsNonEmptySet() {
        // Arrange
        ExecutionContext ctx = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        JsonNodePath loc = mock(JsonNodePath.class);
        ValidationMessage msg1 = mock(ValidationMessage.class);
        ValidationMessage msg2 = mock(ValidationMessage.class);
        Set<ValidationMessage> validationResult = new LinkedHashSet<>(Arrays.asList(msg1, msg2));
        // Stub validate to return a non‑empty set
        doReturn(validationResult).when(anyOfValidatorSpy).validate(eq(ctx), eq(node), eq(root), eq(loc), eq(true));
        // Act
        Set<ValidationMessage> actual = anyOfValidatorSpy.walk(ctx, node, root, loc, true);
        // Assert
        assertSame(validationResult, actual, "Non‑empty result from validate must be returned unchanged");
        verify(anyOfValidatorSpy, times(1)).validate(eq(ctx), eq(node), eq(root), eq(loc), eq(true));
        // Ensure schemas are NOT walked
        verifyNoInteractions(schemaMock1, schemaMock2);
    }

    @Test
    void walkShouldValidateWhenFlagTrueAndSchemasNonEmpty() {
        // This mirrors the original behaviour: validate is called and schemas are NOT walked.
        ExecutionContext ctx = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        JsonNodePath loc = mock(JsonNodePath.class);
        Set<ValidationMessage> expected = Collections.emptySet();
        doReturn(expected).when(anyOfValidatorSpy).validate(eq(ctx), eq(node), eq(root), eq(loc), eq(true));
        Set<ValidationMessage> actual = anyOfValidatorSpy.walk(ctx, node, root, loc, true);
        assertSame(expected, actual);
        verify(anyOfValidatorSpy, times(1)).validate(eq(ctx), eq(node), eq(root), eq(loc), eq(true));
        verifyNoInteractions(schemaMock1, schemaMock2);
    }

    @Test
    void walkShouldWalkAllSchemasWhenFlagFalseAndSchemasNonEmpty() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        JsonNodePath loc = mock(JsonNodePath.class);
        // Stub each schema's walk to return empty set (the return value is ignored by AnyOfValidator)
        when(schemaMock1.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());
        when(schemaMock2.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());
        Set<ValidationMessage> result = anyOfValidatorSpy.walk(ctx, node, root, loc, false);
        assertTrue(result.isEmpty(), "Result must be empty when shouldValidateSchema is false");
        verify(schemaMock1, times(1)).walk(eq(ctx), eq(node), eq(root), eq(loc), eq(false));
        verify(schemaMock2, times(1)).walk(eq(ctx), eq(node), eq(root), eq(loc), eq(false));
        verify(anyOfValidatorSpy, never()).validate(any(), any(), any(), any(), anyBoolean());
    }
}
