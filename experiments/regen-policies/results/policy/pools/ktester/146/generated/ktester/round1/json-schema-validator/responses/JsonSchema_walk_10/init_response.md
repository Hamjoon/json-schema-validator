```java
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

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * The tests verify that:
 * <ul>
 *   <li>Pre‑walk listeners are consulted.</li>
 *   <li>When a pre‑walk listener returns {@code true}, the validator’s {@code walk}
 *       method is invoked and any returned {@link ValidationMessage}s are aggregated.</li>
 *   <li>Post‑walk listeners are always invoked, regardless of the pre‑walk result.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_10_Test {

    /* --------------------------------------------------------------------- */
    /*  Mocks shared by all test methods                                      */
    /* --------------------------------------------------------------------- */
    @Mock ExecutionContext executionContext;
    @Mock JsonNode node;
    @Mock JsonNode rootNode;
    @Mock JsonNodePath instancePath;
    @Mock ValidationContext validationContext;
    @Mock SchemaValidatorsConfig config;
    @Mock KeywordWalkListenerRunner listenerRunner;
    @Mock JsonValidator validator;
    @Mock ValidationMessage validationMessage;

    /* --------------------------------------------------------------------- */
    /*  The concrete JsonSchema under test.                                   */
    /* --------------------------------------------------------------------- */
    private JsonSchema schemaUnderTest;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // Build a minimal concrete subclass of JsonSchema using the protected
        // constructor. All arguments that are not required for the walk()
        // logic are passed as null or empty collections.
        // -----------------------------------------------------------------
        schemaUnderTest = new JsonSchema(
                Collections.emptyList(),          // validators (will be stubbed)
                false,                            // validatorsLoaded
                false,                            // recursiveAnchor
                null,                             // typeValidator
                null,                             // id
                false,                            // suppressSubSchemaRetrieval
                null,                             // schemaNode
                validationContext,                // ValidationContext
                null,                             // errorMessageType
                null,                             // errorMessageKeyword
                null,                             // messageSource
                null,                             // keyword
                null,                             // parentSchema
                null,                             // schemaLocation
                null,                             // evaluationPath
                null,                             // evaluationParentSchema
                Collections.emptyMap()            // errorMessage map
        );

        // -----------------------------------------------------------------
        // Stub the validationContext hierarchy used inside walk().
        // -----------------------------------------------------------------
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getKeywordWalkListenerRunner()).thenReturn(listenerRunner);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
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
        when(validator.walk(executionContext, node, rootNode, instancePath, true))
                .thenReturn(validatorResult);
        // Pre‑walk listener permits execution.
        when(listenerRunner.runPreWalkListeners(
                eq(executionContext),
                anyString(),               // keyword name – not relevant for this test
                eq(node), eq(rootNode), eq(instancePath),
                eq(schemaUnderTest), eq(validator)))
                .thenReturn(true);
        // Post‑walk listener does nothing (void method); we only verify the call.
        doNothing().when(listenerRunner).runPostWalkListeners(
                eq(executionContext),
                anyString(),
                eq(node), eq(rootNode), eq(instancePath),
                eq(schemaUnderTest), eq(validator), anySet());

        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = schemaUnderTest.walk(
                executionContext, node, rootNode, instancePath, true);

        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        assertNotNull(result, "Resulting set must not be null");
        assertEquals(1, result.size(), "Exactly one ValidationMessage should be returned");
        assertTrue(result.contains(validationMessage), "Returned set must contain the validator's message");

        // Verify interactions
        verify(listenerRunner, times(1)).runPreWalkListeners(
                eq(executionContext), anyString(), eq(node), eq(rootNode),
                eq(instancePath), eq(schemaUnderTest), eq(validator));
        verify(validator, times(1)).walk(
                executionContext, node, rootNode, instancePath, true);
        verify(listenerRunner, times(1)).runPostWalkListeners(
                eq(executionContext), anyString(), eq(node), eq(rootNode),
                eq(instancePath), eq(schemaUnderTest), eq(validator), eq(result));
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
        when(listenerRunner.runPreWalkListeners(
                eq(executionContext),
                anyString(),
                eq(node), eq(rootNode), eq(instancePath),
                eq(schemaUnderTest), eq(validator)))
                .thenReturn(false);
        // Post‑walk listener – again just verify the call.
        doNothing().when(listenerRunner).runPostWalkListeners(
                eq(executionContext),
                anyString(),
                eq(node), eq(rootNode), eq(instancePath),
                eq(schemaUnderTest), eq(validator), anySet());

        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = schemaUnderTest.walk(
                executionContext, node, rootNode, instancePath, true);

        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        assertNotNull(result, "Result set must never be null");
        assertTrue(result.isEmpty(), "No messages should be collected when pre‑walk is false");

        // Verify that validator.walk was never called.
        verify(validator, never()).walk(any(), any(), any(), any(), anyBoolean());

        // Verify that listeners were still invoked.
        verify(listenerRunner, times(1)).runPreWalkListeners(
                eq(executionContext), anyString(), eq(node), eq(rootNode),
                eq(instancePath), eq(schemaUnderTest), eq(validator));
        verify(listenerRunner, times(1)).runPostWalkListeners(
                eq(executionContext), anyString(), eq(node), eq(rootNode),
                eq(instancePath), eq(schemaUnderTest), eq(validator), eq(result));
    }
}
```