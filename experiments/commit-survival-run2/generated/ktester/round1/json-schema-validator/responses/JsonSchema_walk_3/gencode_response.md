```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.walk.WalkListenerRunner;
import com.networknt.schema.walk.JsonSchemaWalker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}.
 *
 * <p>The tests focus on the orchestration performed by {@code walk}:
 * <ul>
 *   <li>interaction with {@link WalkListenerRunner}</li>
 *   <li>execution (or skipping) of {@link JsonSchemaWalker}</li>
 *   <li>correct handling of exceptions and null values</li>
 *   <li>processing of multiple validators</li>
 * </ul>
 *
 * <p>All collaborators are heavily mocked – the real schema parsing logic is not exercised.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_3_Test {

    /* --------------------------------------------------------------------- *
     *  Common test fixtures
     * --------------------------------------------------------------------- */
    @Mock ExecutionContext mockExecutionContext;
    @Mock JsonNode mockNode;
    @Mock JsonNode mockRootNode;
    @Mock ValidationContext mockValidationContext;
    @Mock JsonSchemaFactory mockFactory;          // returned by validationContext.getJsonSchemaFactory()
    @Mock WalkListenerRunner mockListenerRunner;

    private JsonSchema spySchema;                  // partial mock of JsonSchema

    /** Helper to inject private fields via reflection. */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /** Prepare a fresh {@link JsonSchema} spy before each test. */
    @BeforeEach
    void init() throws Exception {
        // Create a spy that calls real methods (we need the real walk implementation)
        spySchema = Mockito.spy(JsonSchema.class);

        // Inject the listener runner and a minimal validation context
        setField(spySchema, "keywordWalkListenerRunner", mockListenerRunner);
        setField(spySchema, "validationContext", mockValidationContext);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockFactory);
    }

    /* --------------------------------------------------------------------- *
     *  1. Pre‑walk listener returns true – walker is executed
     * --------------------------------------------------------------------- */
    @Test
    void preWalkTrueWalkerExecuted() throws Exception {
        // ----- arrange -------------------------------------------------------
        WalkListenerRunner runner = mockListenerRunner;
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        ValidationMessage vm = mock(ValidationMessage.class);
        Set<ValidationMessage> walkerResult = Set.of(vm);

        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(),
                anyString(), any(), any(), any(), any()))
                .thenReturn(true);
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(walkerResult);
        // one validator entry
        Map<String, JsonSchemaWalker> validators = Map.of("path/kw", walker);
        doReturn(validators).when(spySchema).getValidators();

        // ----- act -----------------------------------------------------------
        Set<ValidationMessage> messages = spySchema.walk(mockExecutionContext,
                mockNode, mockRootNode, "/myPath", true);

        // ----- assert ---------------------------------------------------------
        assertEquals(walkerResult, messages, "Messages from walker must be returned");
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext),
                eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"),
                anyString(), any(), any(), any(), any());
        verify(walker, times(1)).walk(eq(mockExecutionContext), eq(mockNode),
                eq(mockRootNode), eq("/myPath"), eq(true));
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(),
                any(), any(), anyString(), anyString(), any(), any(),
                any(), any(), eq(messages));
    }

    /* --------------------------------------------------------------------- *
     *  2. Pre‑walk listener returns false – walker is skipped
     * --------------------------------------------------------------------- */
    @Test
    void preWalkFalseWalkerSkipped() throws Exception {
        WalkListenerRunner runner = mockListenerRunner;
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(false);
        Map<String, JsonSchemaWalker> validators = Map.of("path/kw", walker);
        doReturn(validators).when(spySchema).getValidators();

        Set<ValidationMessage> messages = spySchema.walk(mockExecutionContext,
                mockNode, mockRootNode, "/myPath", true);

        assertTrue(messages.isEmpty(), "No messages expected when walker is skipped");
        verify(walker, never()).walk(any(), any(), any(), anyString(), anyBoolean());
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(),
                any(), any(), anyString(), anyString(), any(), any(),
                any(), any(), eq(messages));
    }

    /* --------------------------------------------------------------------- *
     *  3. Post‑walk listener is always executed
     * --------------------------------------------------------------------- */
    @Test
    void postWalkAlwaysExecuted() throws Exception {
        WalkListenerRunner runner = mockListenerRunner;
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(false);      // walker will be skipped
        Map<String, JsonSchemaWalker> validators = Map.of("kw1", walker);
        doReturn(validators).when(spySchema).getValidators();

        spySchema.walk(mockExecutionContext, mockNode, mockRootNode, "/myPath", true);

        verify(runner, times(1)).runPostWalkListeners(eq(mockExecutionContext),
                eq("kw1"), eq(mockNode), eq(mockRootNode), eq("/myPath"),
                anyString(), any(), any(), any(), any(), anySet());
    }

    /* --------------------------------------------------------------------- *
     *  4. Exception thrown by walker – post‑walk still runs
     * --------------------------------------------------------------------- */
    @Test
    void walkerExceptionKeepsPostWalk() throws Exception {
        WalkListenerRunner runner = mockListenerRunner;
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(true);
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("walker failure"));
        Map<String, JsonSchemaWalker> validators = Map.of("kw", walker);
        doReturn(validators).when(spySchema).getValidators();

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                spySchema.walk(mockExecutionContext, mockNode, mockRootNode, "/exception", true));

        assertEquals("walker failure", thrown.getMessage());
        // even though walker threw, post‑walk must have been invoked
        verify(runner, times(1)).runPostWalkListeners(eq(mockExecutionContext),
                eq("kw"), eq(mockNode), eq(mockRootNode), eq("/exception"),
                anyString(), any(), any(), any(), any(), anySet());
    }

    /* --------------------------------------------------------------------- *
     *  5. Multiple validators – each processed independently
     * --------------------------------------------------------------------- */
    @Test
    void multipleValidatorsAllProcessed() throws Exception {
        WalkListenerRunner runner = mockListenerRunner;
        JsonSchemaWalker walkerA = mock(JsonSchemaWalker.class);
        JsonSchemaWalker walkerB = mock(JsonSchemaWalker.class);
        ValidationMessage vmA = mock(ValidationMessage.class);
        ValidationMessage vmB = mock(ValidationMessage.class);
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(true); // both true
        when(walkerA.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Set.of(vmA));
        when(walkerB.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Set.of(vmB));
        Map<String, JsonSchemaWalker> validators = Map.of(
                "kwA", walkerA,
                "kwB", walkerB);
        doReturn(validators).when(spySchema).getValidators();

        Set<ValidationMessage> result = spySchema.walk(mockExecutionContext,
                mockNode, mockRootNode, "/multi", true);

        assertEquals(2, result.size(), "Messages from both walkers must be present");
        assertTrue(result.containsAll(Set.of(vmA, vmB)));
        verify(runner, times(2)).runPreWalkListeners(any(), anyString(),
                any(), any(), anyString(), anyString(), any(), any(), any(), any());
        verify(runner, times(2)).runPostWalkListeners(any(), anyString(),
                any(), any(), anyString(), anyString(), any(), any(),
                any(), any(), eq(result));
    }

    /* --------------------------------------------------------------------- *
     *  6. Empty validators map – returns empty set
     * --------------------------------------------------------------------- */
    @Test
    void emptyValidatorsReturnsEmptySet() throws Exception {
        doReturn(Collections.emptyMap()).when(spySchema).getValidators();

        Set<ValidationMessage> result = spySchema.walk(mockExecutionContext,
                mockNode, mockRootNode, "/", false);

        assertTrue(result.isEmpty(), "Result should be empty when no validators exist");
        verify(mockListenerRunner, never()).runPreWalkListeners(any(), anyString(),
                any(), any(), anyString(), anyString(), any(), any(), any(), any());
        verify(mockListenerRunner, never()).runPostWalkListeners(any(), anyString(),
                any(), any(), anyString(), anyString(), any(), any(),
                any(), any(), anySet());
    }

    /* --------------------------------------------------------------------- *
     *  7. Mixed pre‑walk outcomes – only true entries contribute messages
     * --------------------------------------------------------------------- */
    @Test
    void mixedPreWalkOutcomes() throws Exception {
        WalkListenerRunner runner = mockListenerRunner;
        JsonSchemaWalker walkerTrue = mock(JsonSchemaWalker.class);
        JsonSchemaWalker walkerFalse = mock(JsonSchemaWalker.class);
        ValidationMessage vm = mock(ValidationMessage.class);
        when(runner.runPreWalkListeners(any(), eq("kwTrue"), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(true);
        when(runner.runPreWalkListeners(any(), eq("kwFalse"), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(false);
        when(walkerTrue.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Set.of(vm));

        Map<String, JsonSchemaWalker> validators = Map.of(
                "kwTrue", walkerTrue,
                "kwFalse", walkerFalse);
        doReturn(validators).when(spySchema).getValidators();

        Set<ValidationMessage> result = spySchema.walk(mockExecutionContext,
                mockNode, mockRootNode, "/mixed", true);

        assertEquals(Set.of(vm), result, "Only messages from the true pre‑walk entry expected");
        verify(walkerTrue, times(1)).walk(any(), any(), any(), anyString(), anyBoolean());
        verify(walkerFalse, never()).walk(any(), any(), any(), anyString(), anyBoolean());
        verify(runner, times(2)).runPostWalkListeners(any(), anyString(), any(),
                any(), anyString(), anyString(), any(), any(), any(), any(),
                anySet());
    }

    /* --------------------------------------------------------------------- *
     *  8. Pre‑walk listener throws RuntimeException – post‑walk still executed
     * --------------------------------------------------------------------- */
    @Test
    void preWalkThrowsExceptionPostWalkRuns() throws Exception {
        WalkListenerRunner runner = mockListenerRunner;
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("pre‑walk failure"));
        Map<String, JsonSchemaWalker> validators = Map.of("kw", walker);
        doReturn(validators).when(spySchema).getValidators();

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                spySchema.walk(mockExecutionContext, mockNode, mockRootNode, "/exception", true));

        assertEquals("pre‑walk failure", thrown.getMessage());
        // post‑walk must still be called despite the exception
        verify(runner, times(1)).runPostWalkListeners(eq(mockExecutionContext),
                eq("kw"), eq(mockNode), eq(mockRootNode), eq("/exception"),
                anyString(), any(), any(), any(), any(), anySet());
    }

    /* --------------------------------------------------------------------- *
     *  9. Post‑walk listener throws RuntimeException – exception propagates
     * --------------------------------------------------------------------- */
    @Test
    void postWalkThrowsExceptionPropagates() throws Exception {
        WalkListenerRunner runner = mockListenerRunner;
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        ValidationMessage vm = mock(ValidationMessage.class);
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(true);
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Set.of(vm));
        doThrow(new RuntimeException