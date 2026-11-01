```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * The tests cover the behaviour of pre‑walk / post‑walk listeners, handling of
 * validator results (null, empty, non‑empty), exception propagation and null
 * configuration scenarios.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_10_Test {

    /* --------------------------------------------------------------------- */
    /*  Mocks shared by most test methods                                      */
    /* --------------------------------------------------------------------- */
    @Mock ExecutionContext executionContext;
    @Mock JsonNode node;
    @Mock JsonNode rootNode;
    @Mock JsonNodePath instancePath;
    @Mock ValidationContext validationContext;
    @Mock SchemaValidatorsConfig config;
    @Mock KeywordWalkListenerRunner listenerRunner;
    @Mock JsonValidator validator;
    @Mock JsonValidator validator2;
    @Mock ValidationMessage validationMessage;
    @Mock ValidationMessage validationMessage2;

    /* --------------------------------------------------------------------- */
    /*  The concrete JsonSchema under test.                                   */
    /* --------------------------------------------------------------------- */
    private JsonSchema schemaUnderTest;

    @BeforeEach
    void setupBeforeEach() {
        // Minimal concrete JsonSchema using the protected constructor.
        schemaUnderTest = new JsonSchema(
                Collections.emptyList(), // validators (stubbed per test)
                false,                   // validatorsLoaded
                false,                    // recursiveAnchor
                null,                     // typeValidator
                null,                     // id
                false,                    // suppressSubSchemaRetrieval
                null,                     // schemaNode
                validationContext,       // ValidationContext
                null,                     // errorMessageType
                null,                     // errorMessageKeyword
                null,                     // messageSource
                null,                     // keyword
                null,                     // parentSchema
                null,                     // schemaLocation
                null,                     // evaluationPath
                null,                     // evaluationParentSchema
                Collections.emptyMap()    // errorMessage map
        );

        // Stub the validationContext hierarchy used inside walk().
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getKeywordWalkListenerRunner()).thenReturn(listenerRunner);
    }

    /* --------------------------------------------------------------------- */
    /*  Helper to replace the private ValidationContext field via reflection. */
    /* --------------------------------------------------------------------- */
    private void setValidationContext(JsonSchema schema, ValidationContext ctx) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(schema, ctx);
    }

    /* --------------------------------------------------------------------- */
    /*  1. Pre‑walk listener returns true – validator executed                 */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_preWalkTrue_validatorExecuted_andListenersInvoked() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        Set<ValidationMessage> validatorResult = new HashSet<>(Collections.singleton(validationMessage));
        when(validator.walk(executionContext, node, rootNode, instancePath, true)).thenReturn(validatorResult);
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator)))
                .thenReturn(true);
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());

        Set<ValidationMessage> result = schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(validationMessage));

        verify(listenerRunner, times(1)).runPreWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator));
        verify(validator, times(1)).walk(executionContext, node, rootNode, instancePath, true);
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), eq(result));
    }

    /* --------------------------------------------------------------------- */
    /*  2. Pre‑walk listener returns false – validator skipped                */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_preWalkFalse_validatorSkipped_andPostListenerStillInvoked() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator)))
                .thenReturn(false);
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());

        Set<ValidationMessage> result = schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(validator, never()).walk(any(), any(), any(), any(), anyBoolean());
        verify(listenerRunner, times(1)).runPreWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator));
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), eq(result));
    }

    /* --------------------------------------------------------------------- */
    /*  3. Validator returns null – no errors added                             */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_validatorReturnsNull_noErrorsAdded() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator)))
                .thenReturn(true);
        when(validator.walk(executionContext, node, rootNode, instancePath, false)).thenReturn(null);
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());

        Set<ValidationMessage> result = schemaUnderTest.walk(executionContext, node, rootNode, instancePath, false);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(validator, times(1)).walk(executionContext, node, rootNode, instancePath, false);
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), eq(result));
    }

    /* --------------------------------------------------------------------- */
    /*  4. Validator returns empty Set – no errors added                       */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_validatorReturnsEmptySet_noErrorsAdded() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator)))
                .thenReturn(true);
        when(validator.walk(executionContext, node, rootNode, instancePath, true))
                .thenReturn(Collections.emptySet());
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());

        Set<ValidationMessage> result = schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(validator, times(1)).walk(executionContext, node, rootNode, instancePath, true);
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), eq(result));
    }

    /* --------------------------------------------------------------------- */
    /*  5. Multiple validators – errors aggregated                              */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_multipleValidators_errorsAggregated() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator, validator2));

        // Both validators are allowed to run
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), any()))
                .thenReturn(true);

        Set<ValidationMessage> set1 = new HashSet<>(Collections.singleton(validationMessage));
        Set<ValidationMessage> set2 = new HashSet<>(Collections.singleton(validationMessage2));

        when(validator.walk(executionContext, node, rootNode, instancePath, true)).thenReturn(set1);
        when(validator2.walk(executionContext, node, rootNode, instancePath, true)).thenReturn(set2);

        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), any(), anySet());

        Set<ValidationMessage> result = schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(validationMessage));
        assertTrue(result.contains(validationMessage2));

        verify(validator, times(1)).walk(executionContext, node, rootNode, instancePath, true);
        verify(validator2, times(1)).walk(executionContext, node, rootNode, instancePath, true);
        verify(listenerRunner, times(2)).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), any(), anySet());
    }

    /* --------------------------------------------------------------------- */
    /*  6. Validator throws exception – post‑walk still invoked               */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_validatorThrowsException_postWalkStillInvoked() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator)))
                .thenReturn(true);
        when(validator.walk(executionContext, node, rootNode, instancePath, false))
                .thenThrow(new RuntimeException("validator failure"));
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                schemaUnderTest.walk(executionContext, node, rootNode, instancePath, false));

        assertEquals("validator failure", ex.getMessage());

        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
    }

    /* --------------------------------------------------------------------- */
    /*  7. Null ValidationContext causes NullPointerException                  */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_nullValidationContext_throwsNPE() throws Exception {
        JsonSchema schema = new JsonSchema(
                Collections.emptyList(),
                false, false, null, null, false, null,
                null, // <-- null ValidationContext
                null, null, null, null, null, null, null, Collections.emptyMap());

        // No need to stub anything – the NPE occurs before any interaction.
        assertThrows(NullPointerException.class, () ->
                schema.walk(executionContext, node, rootNode, instancePath, true));
    }

    /* --------------------------------------------------------------------- */
    /*  8. Null Config in ValidationContext causes NullPointerException       */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_nullConfig_throwsNPE() {
        when(validationContext.getConfig()).thenReturn(null); // config is null
        assertThrows(NullPointerException.class, () ->
                schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true));
    }

    /* --------------------------------------------------------------------- */
    /*  9. Null KeywordWalkListenerRunner causes NullPointerException          */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_nullKeywordWalkListenerRunner_throwsNPE() {
        when(config.getKeywordWalkListenerRunner()).thenReturn(null);
        assertThrows(NullPointerException.class, () ->
                schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true));
    }

    /* --------------------------------------------------------------------- */
    /* 10. Pre‑walk listener throws RuntimeException – post‑walk still invoked */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_preWalkListenerThrows_postWalkStillInvoked() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator)))
                .thenThrow(new RuntimeException("pre‑walk failure"));
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true));

        assertEquals("pre‑walk failure", ex.getMessage());

        // Post‑walk must have been called despite the pre‑walk exception.
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
    }

    /* --------------------------------------------------------------------- */
    /* 11. Post‑walk listener throws RuntimeException – exception propagates   */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_postWalkListenerThrows_exceptionPropagates() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator)))
                .thenReturn(true);
        when(validator.walk(executionContext, node, rootNode, instancePath, true))
                .thenReturn(Collections.emptySet());
        doThrow(new RuntimeException("post‑walk failure")).when(listenerRunner)
                .runPostWalkListeners(eq(executionContext), anyString(),
                        eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true));

        assertEquals("post‑walk failure", ex.getMessage());

        verify(validator, times(1)).walk(executionContext, node, rootNode, instancePath, true);
    }

    /* --------------------------------------------------------------------- */
    /* 12. Validator returns null evaluationPath – NPE before pre‑walk          */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_validatorNullEvaluationPath_throwsNPE() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));
        when(validator.getEvaluationPath()).thenReturn(null); // null path

        // The NPE occurs when walk() tries to call getName(-1) on a null reference.
        assertThrows(NullPointerException.class, () ->
                schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true));
    }

    /* --------------------------------------------------------------------- */
    /* 13. Empty evaluationPath causes IndexOutOfBoundsException – post‑walk still invoked */
    /* --------------------------------------------------------------------- */
    @Test
    void walk_emptyEvaluationPath_throwsIndexOutOfBounds_andPostWalkInvoked() {
        when(schemaUnderTest.getValidators()).thenReturn(List.of(validator));

        JsonNodePath emptyPath = mock(JsonNodePath.class);
        when(emptyPath.getName(-1)).thenThrow(new IndexOutOfBoundsException("no elements"));
        when(validator.getEvaluationPath()).thenReturn(emptyPath);
        // Pre‑walk listener will be called before the exception; we let it return true.
        when(listenerRunner.runPreWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator)))
                .thenReturn(true);
        doNothing().when(listenerRunner).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());

        IndexOutOfBoundsException ex = assertThrows(IndexOutOfBoundsException.class, () ->
                schemaUnderTest.walk(executionContext, node, rootNode, instancePath, true));

        assertEquals("no elements", ex.getMessage());

        // Even though the exception originates from evaluationPath.getName(-1),
        // the finally block must still invoke post‑walk.
        verify(listenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(),
                eq(node), eq(rootNode), eq(instancePath), eq(schemaUnderTest), eq(validator), anySet());
    }
}
```