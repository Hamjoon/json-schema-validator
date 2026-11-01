package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_10_Test {

    /* --------------------------------------------------------------------- */
    @Mock
    ExecutionContext executionContext;

    @Mock
    JsonNode node;

    @Mock
    JsonNode rootNode;

    @Mock
    JsonNodePath instancePath;

    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaValidatorsConfig config;

    @Mock
    KeywordWalkListenerRunner listenerRunner;

    @Mock
    JsonValidator validator;

    @Mock
    ValidationMessage validationMessage;

    /* --------------------------------------------------------------------- */
    private JsonSchema schemaUnderTest;

    @Mock
    JsonValidator validator2;

    @Mock
    ValidationMessage validationMessage2;

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // Build a minimal concrete subclass of JsonSchema using the protected
        // constructor. All arguments that are not required for the walk()
        // logic are passed as null or empty collections.
        // -----------------------------------------------------------------
        schemaUnderTest = new JsonSchema(// validators (will be stubbed)
        Collections.emptyList(), // validatorsLoaded
        false, // recursiveAnchor
        false, // typeValidator
        null, // id
        null, // suppressSubSchemaRetrieval
        false, // schemaNode
        null, // ValidationContext
        validationContext, // errorMessageType
        null, // errorMessageKeyword
        null, // messageSource
        null, // keyword
        null, // parentSchema
        null, // schemaLocation
        null, // evaluationPath
        null, // evaluationParentSchema
        null, // errorMessage map
        Collections.emptyMap());
        // -----------------------------------------------------------------
        // Stub the validationContext hierarchy used inside walk().
        // -----------------------------------------------------------------
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getKeywordWalkListenerRunner()).thenReturn(listenerRunner);
    }

    /* --------------------------------------------------------------------- */
    private void setValidationContext(JsonSchema schema, ValidationContext ctx) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(schema, ctx);
    }

    /**
     * When the pre‑walk listener returns {@code true}, the validator’s {@code walk}
     * method should be executed and any messages it returns must be part of the
     * final error set. Post‑walk listeners must also be invoked.
     */
    @Test
    void walk_preWalkTrue_validatorExecuted_andListenersInvoked() {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        // The schema should report a single validator.
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        // The validator’s walk() returns a set containing one ValidationMessage.
        Set<ValidationMessage> validatorResult = new HashSet<>(Collections.singleton(validationMessage));
        when(validator.walk(executionContext, node, rootNode, instancePath, true)).thenReturn(validatorResult);
        // Pre‑walk listener permits execution.
        when(listenerRunner.runPreWalkListeners(eq(executionContext), // keyword name – not relevant for this test
        anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator))).thenReturn(true);
        // Post‑walk listener does nothing (void method); we only verify the call.
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true);
        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        assertNotNull(result, "Resulting set must not be null");
        assertEquals(1, result.size(), "Exactly one ValidationMessage should be returned");
        assertTrue(result.contains(validationMessage), "Returned set must contain the validator's message");
        // Verify interactions
        verify(listenerRunner, times(1)).runPreWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator));
        verify(validator, times(1)).walk(executionContext, node, rootNode, instancePath, true);
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), eq(result));
    }

    /**
     * When the pre‑walk listener returns {@code false}, the validator’s {@code walk}
     * must **not** be called, but the post‑walk listener must still be executed.
     * The resulting error set should be empty.
     */
    @Test
    void walk_preWalkFalse_validatorSkipped_andPostListenerStillInvoked() {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        // Pre‑walk listener denies execution.
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator))).thenReturn(false);
        // Post‑walk listener – again just verify the call.
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true);
        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        assertNotNull(result, "Result set must never be null");
        assertTrue(result.isEmpty(), "No messages should be collected when pre‑walk is false");
        // Verify that validator.walk was never called.
        verify(validator, never()).walk(any(), any(), any(), any(), anyBoolean());
        // Verify that listeners were still invoked.
        verify(listenerRunner, times(1)).runPreWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator));
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), eq(result));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_validatorReturnsNull_noErrorsAdded() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator))).thenReturn(true);
        when(validator.walk(executionContext, node, rootNode, instancePath, false)).thenReturn(null);
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
        Set<ValidationMessage> result = schemaUnderTest.walk(executionContext, node, rootNode, instancePath, false);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(validator, times(1)).walk(executionContext, node, rootNode, instancePath, false);
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), eq(result));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_validatorReturnsEmptySet_noErrorsAdded() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator))).thenReturn(true);
        when(validator.walk(executionContext, node, rootNode, instancePath, true)).thenReturn(Collections.emptySet());
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
        Set<ValidationMessage> result = schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(validator, times(1)).walk(executionContext, node, rootNode, instancePath, true);
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), eq(result));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_multipleValidators_errorsAggregated() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator, validator2));
        // Both validators are allowed to run
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), any())).thenReturn(true);
        Set<ValidationMessage> set1 = new HashSet<>(Collections.singleton(validationMessage));
        Set<ValidationMessage> set2 = new HashSet<>(Collections.singleton(validationMessage2));
        when(validator.walk(executionContext, node, rootNode, instancePath, true)).thenReturn(set1);
        when(validator2.walk(executionContext, node, rootNode, instancePath, true)).thenReturn(set2);
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), any(), anySet());
        Set<ValidationMessage> result = schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(validationMessage));
        assertTrue(result.contains(validationMessage2));
        verify(validator, times(1)).walk(executionContext, node, rootNode, instancePath, true);
        verify(validator2, times(1)).walk(executionContext, node, rootNode, instancePath, true);
        verify(listenerRunner, times(2)).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), any(), anySet());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_validatorThrowsException_postWalkStillInvoked() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator))).thenReturn(true);
        when(validator.walk(executionContext, node, rootNode, instancePath, false)).thenThrow(new RuntimeException("validator failure"));
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schemaUnderTest.walk(executionContext, node, rootNode, instancePath, false));
        assertEquals("validator failure", ex.getMessage());
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_nullValidationContext_throwsNPE() throws Exception {
        JsonSchema schema = new JsonSchema(Collections.emptyList(), false, false, null, null, false, null, // <-- null ValidationContext
        null, null, null, null, null, null, null, null, Collections.emptyMap());
        // No need to stub anything – the NPE occurs before any interaction.
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, node, rootNode, instancePath, true));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_nullConfig_throwsNPE() {
        // config is null
        when(validationContext.getConfig()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_nullKeywordWalkListenerRunner_throwsNPE() {
        when(config.getKeywordWalkListenerRunner()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_preWalkListenerThrows_postWalkStillInvoked() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator))).thenThrow(new RuntimeException("pre‑walk failure"));
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true));
        assertEquals("pre‑walk failure", ex.getMessage());
        // Post‑walk must have been called despite the pre‑walk exception.
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_postWalkListenerThrows_exceptionPropagates() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator))).thenReturn(true);
        when(validator.walk(executionContext, node, rootNode, instancePath, true)).thenReturn(Collections.emptySet());
        doThrow(new RuntimeException("post‑walk failure")).when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true));
        assertEquals("post‑walk failure", ex.getMessage());
        verify(validator, times(1)).walk(executionContext, node, rootNode, instancePath, true);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_validatorNullEvaluationPath_throwsNPE() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        // null path
        when(validator.getEvaluationPath()).thenReturn(null);
        // The NPE occurs when walk() tries to call getName(-1) on a null reference.
        assertThrows(NullPointerException.class, () -> schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void walk_emptyEvaluationPath_throwsIndexOutOfBounds_andPostWalkInvoked() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        JsonNodePath emptyPath = mock(JsonNodePath.class);
        when(emptyPath.getName(-1)).thenThrow(new IndexOutOfBoundsException("no elements"));
        when(validator.getEvaluationPath()).thenReturn(emptyPath);
        // Pre‑walk listener will be called before the exception; we let it return true.
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator))).thenReturn(true);
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
        IndexOutOfBoundsException ex = assertThrows(IndexOutOfBoundsException.class, () -> schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true));
        assertEquals("no elements", ex.getMessage());
        // Even though the exception originates from evaluationPath.getName(-1),
        // the finally block must still invoke post‑walk.
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
    }
}
