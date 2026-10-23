```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.networknt.schema.walk.JsonSchemaWalker;
import com.networknt.schema.walk.WalkListenerRunner;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}.
 *
 * The test class uses Mockito for mocking collaborators.  The Mockito JUnit‑Jupiter
 * extension is not used because it is not available in the test runtime; instead
 * {@link MockitoAnnotations#openMocks(Object)} is called manually.
 */
class JsonSchema_walk_3_Test {

    private static ObjectMapper mapper;
    private static JsonSchemaFactory schemaFactory;

    /* --------------------------------------------------------------------- *
     *  Common test fixtures
     * --------------------------------------------------------------------- */
    @Mock
    ExecutionContext mockExecutionContext;

    @Mock
    JsonNode mockNode;

    @Mock
    JsonNode mockRootNode;

    @Mock
    ValidationContext mockValidationContext;

    // returned by validationContext.getJsonSchemaFactory()
    @Mock
    JsonSchemaFactory mockFactory;

    @Mock
    WalkListenerRunner mockListenerRunner;

    // spy of a real JsonSchema instance
    private JsonSchema spySchema;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        // Use a recent draft version – V7 works for the simple tests below
        schemaFactory = JsonSchemaFactory.getInstance(VersionFlag.V7);
    }

    /**
     * Initialise Mockito mocks and prepare a fresh {@link JsonSchema} spy before each test.
     */
    @BeforeEach
    void init() throws Exception {
        MockitoAnnotations.openMocks(this);

        // create a real (empty) schema and spy it – this gives us a real walk implementation
        JsonSchema realSchema = schemaFactory.getSchema(mapper.readTree("{}"));
        spySchema = Mockito.spy(realSchema);

        // inject the listener runner and a minimal validation context
        setField(spySchema, "keywordWalkListenerRunner", mockListenerRunner);
        setField(spySchema, "validationContext", mockValidationContext);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockFactory);
    }

    /**
     * Helper to inject private fields via reflection.
     */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Helper to replace the private {@code validators} map inside the spied schema.
     */
    private static void setValidators(JsonSchema schema, Map<String, JsonSchemaWalker> validators) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        f.set(schema, validators);
    }

    /**
     * Verify that walking a schema that does not define any keywords
     * (e.g. an empty object) returns an empty validation‑message set.
     */
    @Test
    void walkReturnsEmptyWhenNoValidators() throws Exception {
        JsonNode emptySchemaNode = mapper.readTree("{}");
        JsonSchema schema = schemaFactory.getSchema(emptySchemaNode);
        ExecutionContext execCtx = new ExecutionContext();
        JsonNode instance = mapper.readTree("\"any value\"");
        Set<ValidationMessage> messages = schema.walk(execCtx, instance, instance, "", true);
        assertTrue(messages.isEmpty(), "Expected no validation messages for an empty schema");
    }

    /**
     * Walk a simple schema that contains a {@code type} keyword.
     * The walk should succeed (no messages) for a matching instance
     * and produce a validation message for a non‑matching instance.
     */
    @Test
    void walkValidatesTypeKeyword() throws Exception {
        JsonNode typeSchemaNode = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema schema = schemaFactory.getSchema(typeSchemaNode);
        ExecutionContext execCtx = new ExecutionContext();

        JsonNode validInstance = mapper.readTree("\"hello\"");
        Set<ValidationMessage> validMessages = schema.walk(execCtx, validInstance, validInstance, "", true);
        assertTrue(validMessages.isEmpty(),
                "A valid string instance should produce no validation messages");

        JsonNode invalidInstance = mapper.readTree("123");
        Set<ValidationMessage> invalidMessages = schema.walk(execCtx, invalidInstance, invalidInstance, "", true);
        assertFalse(invalidMessages.isEmpty(),
                "An integer instance should produce at least one validation message for the type keyword");

        boolean containsTypeKeyword = invalidMessages.stream()
                .anyMatch(msg -> msg.getMessage().toLowerCase().contains("type"));
        assertTrue(containsTypeKeyword,
                "Validation messages should contain information about the 'type' keyword");
    }

    /* --------------------------------------------------------------------- *
     *  1. Pre‑walk listener returns true – walker is executed
     * --------------------------------------------------------------------- */
    @Test
    void preWalkTrueWalkerExecuted() throws Exception {
        // arrange
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        ValidationMessage vm = mock(ValidationMessage.class);
        Set<ValidationMessage> walkerResult = Set.of(vm);

        when(mockListenerRunner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(true);
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(walkerResult);

        Map<String, JsonSchemaWalker> validators = Map.of("path/kw", walker);
        setValidators(spySchema, validators);

        // act
        Set<ValidationMessage> messages = spySchema.walk(mockExecutionContext, mockNode,
                mockRootNode, "/myPath", true);

        // assert
        assertEquals(walkerResult, messages, "Messages from walker must be returned");
        verify(mockListenerRunner, times(1))
                .runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"),
                        eq(mockNode), eq(mockRootNode), eq("/myPath"),
                        anyString(), any(), any(), any(), any());
        verify(walker, times(1))
                .walk(eq(mockExecutionContext), eq(mockNode), eq(mockRootNode),
                        eq("/myPath"), eq(true));
        verify(mockListenerRunner, times(1))
                .runPostWalkListeners(any(), anyString(), any(), any(),
                        anyString(), anyString(), any(), any(),
                        any(), any(), eq(messages));
    }

    /* --------------------------------------------------------------------- *
     *  2. Pre‑walk listener returns false – walker is skipped
     * --------------------------------------------------------------------- */
    @Test
    void preWalkFalseWalkerSkipped() throws Exception {
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        when(mockListenerRunner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(false);
        Map<String, JsonSchemaWalker> validators = Map.of("path/kw", walker);
        setValidators(spySchema, validators);

        Set<ValidationMessage> messages = spySchema.walk(mockExecutionContext, mockNode,
                mockRootNode, "/myPath", true);

        assertTrue(messages.isEmpty(), "No messages expected when walker is skipped");
        verify(walker, never()).walk(any(), any(), any(), anyString(), anyBoolean());
        verify(mockListenerRunner, times(1))
                .runPostWalkListeners(any(), anyString(), any(), any(),
                        anyString(), anyString(), any(), any(),
                        any(), any(), eq(messages));
    }

    /* --------------------------------------------------------------------- *
     *  3. Post‑walk listener is always executed
     * --------------------------------------------------------------------- */
    @Test
    void postWalkAlwaysExecuted() throws Exception {
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        when(mockListenerRunner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(false); // walker will be skipped
        Map<String, JsonSchemaWalker> validators = Map.of("kw1", walker);
        setValidators(spySchema, validators);

        spySchema.walk(mockExecutionContext, mockNode, mockRootNode, "/myPath", true);

        verify(mockListenerRunner, times(1))
                .runPostWalkListeners(eq(mockExecutionContext), eq("kw1"),
                        eq(mockNode), eq(mockRootNode), eq("/myPath"),
                        anyString(), any(), any(),
                        any(), any(), anySet());
    }

    /* --------------------------------------------------------------------- *
     *  4. Exception thrown by walker – post‑walk still runs
     * --------------------------------------------------------------------- */
    @Test
    void walkerExceptionKeepsPostWalk() throws Exception {
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        when(mockListenerRunner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(true);
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("walker failure"));
        Map<String, JsonSchemaWalker> validators = Map.of("kw", walker);
        setValidators(spySchema, validators);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> spySchema.walk(mockExecutionContext, mockNode,
                        mockRootNode, "/exception", true));
        assertEquals("walker failure", thrown.getMessage());

        verify(mockListenerRunner, times(1))
                .runPostWalkListeners(eq(mockExecutionContext), eq("kw"),
                        eq(mockNode), eq(mockRootNode), eq("/exception"),
                        anyString(), any(), any(),
                        any(), any(), anySet());
    }

    /* --------------------------------------------------------------------- *
     *  5. Multiple validators – each processed independently
     * --------------------------------------------------------------------- */
    @Test
    void multipleValidatorsAllProcessed() throws Exception {
        JsonSchemaWalker walkerA = mock(JsonSchemaWalker.class);
        JsonSchemaWalker walkerB = mock(JsonSchemaWalker.class);
        ValidationMessage vmA = mock(ValidationMessage.class);
        ValidationMessage vmB = mock(ValidationMessage.class);

        when(mockListenerRunner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(true);
        when(walkerA.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Set.of(vmA));
        when(walkerB.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Set.of(vmB));

        Map<String, JsonSchemaWalker> validators = Map.of("kwA", walkerA, "kwB", walkerB);
        setValidators(spySchema, validators);

        Set<ValidationMessage> result = spySchema.walk(mockExecutionContext,
                mockNode, mockRootNode, "/multi", true);

        assertEquals(2, result.size(),
                "Messages from both walkers must be present");
        assertTrue(result.containsAll(Set.of(vmA, vmB)));
        verify(mockListenerRunner, times(2))
                .runPreWalkListeners(any(), anyString(), any(), any(),
                        anyString(), anyString(), any(), any(),
                        any(), any());
        verify(mockListenerRunner, times(2))
                .runPostWalkListeners(any(), anyString(), any(), any(),
                        anyString(), anyString(), any(), any(),
                        any(), any(), eq(result));
    }

    /* --------------------------------------------------------------------- *
     *  6. Empty validators map – returns empty set
     * --------------------------------------------------------------------- */
    @Test
    void emptyValidatorsReturnsEmptySet() throws Exception {
        setValidators(spySchema, Collections.emptyMap());

        Set<ValidationMessage> result = spySchema.walk(mockExecutionContext,
                mockNode, mockRootNode, "/", false);
        assertTrue(result.isEmpty(),
                "Result should be empty when no validators exist");
        verify(mockListenerRunner, never())
                .runPreWalkListeners(any(), anyString(), any(), any(),
                        anyString(), anyString(), any(), any(),
                        any(), any());
        verify(mockListenerRunner, never())
                .runPostWalkListeners(any(), anyString(), any(), any(),
                        anyString(), anyString(), any(), any(),
                        any(), any(), anySet());
    }

    /* --------------------------------------------------------------------- *
     *  7. Mixed pre‑walk outcomes – only true entries contribute messages
     * --------------------------------------------------------------------- */
    @Test
    void mixedPreWalkOutcomes() throws Exception {
        JsonSchemaWalker walkerTrue = mock(JsonSchemaWalker.class);
        JsonSchemaWalker walkerFalse = mock(JsonSchemaWalker.class);
        ValidationMessage vm = mock(ValidationMessage.class);

        when(mockListenerRunner.runPreWalkListeners(any(), eq("kwTrue"), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(true);
        when(mockListenerRunner.runPreWalkListeners(any(), eq("kwFalse"), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(false);
        when(walkerTrue.walk(any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(Set.of(vm));

        Map<String, JsonSchemaWalker> validators = Map.of(
                "kwTrue", walkerTrue,
                "kwFalse", walkerFalse);
        setValidators(spySchema, validators);

        Set<ValidationMessage> result = spySchema.walk(mockExecutionContext,
                mockNode, mockRootNode, "/mixed", true);
        assertEquals(Set.of(vm), result,
                "Only messages from the true pre‑walk entry expected");
        verify(walkerTrue, times(1))
                .walk(any(), any(), any(), anyString(), anyBoolean());
        verify(walkerFalse, never())
                .walk(any(), any(), any(), anyString(), anyBoolean());
        verify(mockListenerRunner, times(2))
                .runPostWalkListeners(any(), anyString(), any(), any(),
                        anyString(), anyString(), any(), any(),
                        any(), any(), anySet());
    }

    /* --------------------------------------------------------------------- *
     *  8. Pre‑walk listener throws RuntimeException – post‑walk still executed
     * --------------------------------------------------------------------- */
    @Test
    void preWalkThrowsExceptionPostWalkRuns() throws Exception {
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        when(mockListenerRunner.runPreWalkListeners(any(), anyString(), any(), any(),
                anyString(), anyString(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("pre‑walk failure"));
        Map<String, JsonSchemaWalker> validators = Map.of("kw", walker);
        setValidators(spySchema, validators);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> spySchema.walk(mockExecutionContext, mockNode,
                        mockRootNode, "/exception", true));
        assertEquals("pre‑walk failure", thrown.getMessage());

        verify(mockListenerRunner, times(1))
                .runPostWalkListeners(eq(mockExecutionContext), eq("kw"),
                        eq(mockNode), eq(mockRootNode), eq("/exception"),
                        anyString(), any(), any(),
                        any(), any(), anySet());
    }
}
```