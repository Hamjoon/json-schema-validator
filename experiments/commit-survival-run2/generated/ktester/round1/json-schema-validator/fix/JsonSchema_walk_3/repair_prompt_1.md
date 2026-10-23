
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.networknt.schema.walk.JsonSchemaWalker;
import java.util.Set;
import com.networknt.schema.walk.WalkListenerRunner;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
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

    // partial mock of JsonSchema
    private JsonSchema spySchema;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        // Use a recent draft version – V7 works for the simple tests below
        schemaFactory = JsonSchemaFactory.getInstance(VersionFlag.V7);
    }

    /**
     * Prepare a fresh {@link JsonSchema} spy before each test.
     */
    @BeforeEach
    void init() throws Exception {
        // Create a spy that calls real methods (we need the real walk implementation)
        spySchema = Mockito.spy(JsonSchema.class);
        // Inject the listener runner and a minimal validation context
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
     * Verify that walking a schema that does not define any keywords
     * (e.g. an empty object) returns an empty validation‑message set.
     */
    @Test
    void walkReturnsEmptyWhenNoValidators() throws Exception {
        // an empty schema – no validators will be generated
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
        // schema requiring a string value
        JsonNode typeSchemaNode = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema schema = schemaFactory.getSchema(typeSchemaNode);
        ExecutionContext execCtx = new ExecutionContext();
        // ---- valid instance (string) ----
        JsonNode validInstance = mapper.readTree("\"hello\"");
        Set<ValidationMessage> validMessages = schema.walk(execCtx, validInstance, validInstance, "", true);
        assertTrue(validMessages.isEmpty(), "A valid string instance should produce no validation messages");
        // ---- invalid instance (integer) ----
        JsonNode invalidInstance = mapper.readTree("123");
        Set<ValidationMessage> invalidMessages = schema.walk(execCtx, invalidInstance, invalidInstance, "", true);
        assertFalse(invalidMessages.isEmpty(), "An integer instance should produce at least one validation message for the type keyword");
        // Optional: assert that the message mentions the expected keyword
        boolean containsTypeKeyword = invalidMessages.stream().anyMatch(msg -> msg.getMessage().toLowerCase().contains("type"));
        assertTrue(containsTypeKeyword, "Validation messages should contain information about the 'type' keyword");
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
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean())).thenReturn(walkerResult);
        // one validator entry
        Map<String, JsonSchemaWalker> validators = Map.of("path/kw", walker);
        doReturn(validators).when(spySchema).getValidators();
        // ----- act -----------------------------------------------------------
        Set<ValidationMessage> messages = spySchema.walk(mockExecutionContext, mockNode, mockRootNode, "/myPath", true);
        // ----- assert ---------------------------------------------------------
        assertEquals(walkerResult, messages, "Messages from walker must be returned");
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any());
        verify(walker, times(1)).walk(eq(mockExecutionContext), eq(mockNode), eq(mockRootNode), eq("/myPath"), eq(true));
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
    }

    /* --------------------------------------------------------------------- *
     *  2. Pre‑walk listener returns false – walker is skipped
     * --------------------------------------------------------------------- */
    @Test
    void preWalkFalseWalkerSkipped() throws Exception {
        WalkListenerRunner runner = mockListenerRunner;
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(false);
        Map<String, JsonSchemaWalker> validators = Map.of("path/kw", walker);
        doReturn(validators).when(spySchema).getValidators();
        Set<ValidationMessage> messages = spySchema.walk(mockExecutionContext, mockNode, mockRootNode, "/myPath", true);
        assertTrue(messages.isEmpty(), "No messages expected when walker is skipped");
        verify(walker, never()).walk(any(), any(), any(), anyString(), anyBoolean());
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
    }

    /* --------------------------------------------------------------------- *
     *  3. Post‑walk listener is always executed
     * --------------------------------------------------------------------- */
    @Test
    void postWalkAlwaysExecuted() throws Exception {
        WalkListenerRunner runner = mockListenerRunner;
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        // walker will be skipped
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).// walker will be skipped
        thenReturn(false);
        Map<String, JsonSchemaWalker> validators = Map.of("kw1", walker);
        doReturn(validators).when(spySchema).getValidators();
        spySchema.walk(mockExecutionContext, mockNode, mockRootNode, "/myPath", true);
        verify(runner, times(1)).runPostWalkListeners(eq(mockExecutionContext), eq("kw1"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any(), anySet());
    }

    /* --------------------------------------------------------------------- *
     *  4. Exception thrown by walker – post‑walk still runs
     * --------------------------------------------------------------------- */
    @Test
    void walkerExceptionKeepsPostWalk() throws Exception {
        WalkListenerRunner runner = mockListenerRunner;
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean())).thenThrow(new RuntimeException("walker failure"));
        Map<String, JsonSchemaWalker> validators = Map.of("kw", walker);
        doReturn(validators).when(spySchema).getValidators();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> spySchema.walk(mockExecutionContext, mockNode, mockRootNode, "/exception", true));
        assertEquals("walker failure", thrown.getMessage());
        // even though walker threw, post‑walk must have been invoked
        verify(runner, times(1)).runPostWalkListeners(eq(mockExecutionContext), eq("kw"), eq(mockNode), eq(mockRootNode), eq("/exception"), anyString(), any(), any(), any(), any(), anySet());
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
        // both true
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).// both true
        thenReturn(true);
        when(walkerA.walk(any(), any(), any(), anyString(), anyBoolean())).thenReturn(Set.of(vmA));
        when(walkerB.walk(any(), any(), any(), anyString(), anyBoolean())).thenReturn(Set.of(vmB));
        Map<String, JsonSchemaWalker> validators = Map.of("kwA", walkerA, "kwB", walkerB);
        doReturn(validators).when(spySchema).getValidators();
        Set<ValidationMessage> result = spySchema.walk(mockExecutionContext, mockNode, mockRootNode, "/multi", true);
        assertEquals(2, result.size(), "Messages from both walkers must be present");
        assertTrue(result.containsAll(Set.of(vmA, vmB)));
        verify(runner, times(2)).runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any());
        verify(runner, times(2)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(result));
    }

    /* --------------------------------------------------------------------- *
     *  6. Empty validators map – returns empty set
     * --------------------------------------------------------------------- */
    @Test
    void emptyValidatorsReturnsEmptySet() throws Exception {
        doReturn(Collections.emptyMap()).when(spySchema).getValidators();
        Set<ValidationMessage> result = spySchema.walk(mockExecutionContext, mockNode, mockRootNode, "/", false);
        assertTrue(result.isEmpty(), "Result should be empty when no validators exist");
        verify(mockListenerRunner, never()).runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any());
        verify(mockListenerRunner, never()).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), anySet());
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
        when(runner.runPreWalkListeners(any(), eq("kwTrue"), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
        when(runner.runPreWalkListeners(any(), eq("kwFalse"), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(false);
        when(walkerTrue.walk(any(), any(), any(), anyString(), anyBoolean())).thenReturn(Set.of(vm));
        Map<String, JsonSchemaWalker> validators = Map.of("kwTrue", walkerTrue, "kwFalse", walkerFalse);
        doReturn(validators).when(spySchema).getValidators();
        Set<ValidationMessage> result = spySchema.walk(mockExecutionContext, mockNode, mockRootNode, "/mixed", true);
        assertEquals(Set.of(vm), result, "Only messages from the true pre‑walk entry expected");
        verify(walkerTrue, times(1)).walk(any(), any(), any(), anyString(), anyBoolean());
        verify(walkerFalse, never()).walk(any(), any(), any(), anyString(), anyBoolean());
        verify(runner, times(2)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), anySet());
    }

    /* --------------------------------------------------------------------- *
     *  8. Pre‑walk listener throws RuntimeException – post‑walk still executed
     * --------------------------------------------------------------------- */
    @Test
    void preWalkThrowsExceptionPostWalkRuns() throws Exception {
        WalkListenerRunner runner = mockListenerRunner;
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenThrow(new RuntimeException("pre‑walk failure"));
        Map<String, JsonSchemaWalker> validators = Map.of("kw", walker);
        doReturn(validators).when(spySchema).getValidators();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> spySchema.walk(mockExecutionContext, mockNode, mockRootNode, "/exception", true));
        assertEquals("pre‑walk failure", thrown.getMessage());
        // post‑walk must still be called despite the exception
        verify(runner, times(1)).runPostWalkListeners(eq(mockExecutionContext), eq("kw"), eq(mockNode), eq(mockRootNode), eq("/exception"), anyString(), any(), any(), any(), any(), anySet());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:14: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:17: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:33: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:36: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:40: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:43: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:62: error: cannot find symbol
        spySchema = Mockito.spy(JsonSchema.class);
                    ^
  symbol:   variable Mockito
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:66: error: cannot find symbol
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockFactory);
        ^
  symbol:   method when(JsonSchemaFactory)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:124: error: cannot find symbol
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
                                  ^
  symbol:   method mock(Class<JsonSchemaWalker>)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:125: error: cannot find symbol
        ValidationMessage vm = mock(ValidationMessage.class);
                               ^
  symbol:   method mock(Class<ValidationMessage>)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:127: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
                                        ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:127: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
                                               ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:127: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
                                                            ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:127: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
                                                                   ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:127: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
                                                                          ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:127: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
                                                                                       ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:127: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
                                                                                                    ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:127: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
                                                                                                           ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:127: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
                                                                                                                  ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:127: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(true);
                                                                                                                         ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:128: error: cannot find symbol
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean())).thenReturn(walkerResult);
                         ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:128: error: cannot find symbol
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean())).thenReturn(walkerResult);
                                ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:128: error: cannot find symbol
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean())).thenReturn(walkerResult);
                                       ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:128: error: cannot find symbol
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean())).thenReturn(walkerResult);
                                              ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:128: error: cannot find symbol
        when(walker.walk(any(), any(), any(), anyString(), anyBoolean())).thenReturn(walkerResult);
                                                           ^
  symbol:   method anyBoolean()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:131: error: cannot find symbol
        doReturn(validators).when(spySchema).getValidators();
        ^
  symbol:   method doReturn(Map<String,JsonSchemaWalker>)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:136: error: cannot find symbol
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any());
                       ^
  symbol:   method times(int)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:136: error: cannot find symbol
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any());
                                                     ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:136: error: cannot find symbol
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any());
                                                                               ^
  symbol:   method eq(String)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:136: error: cannot find symbol
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any());
                                                                                              ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:136: error: cannot find symbol
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any());
                                                                                                            ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:136: error: cannot find symbol
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any());
                                                                                                                              ^
  symbol:   method eq(String)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:136: error: cannot find symbol
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any());
                                                                                                                                             ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:136: error: cannot find symbol
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any());
                                                                                                                                                          ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:136: error: cannot find symbol
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any());
                                                                                                                                                                 ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:136: error: cannot find symbol
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any());
                                                                                                                                                                        ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:136: error: cannot find symbol
        verify(runner, times(1)).runPreWalkListeners(eq(mockExecutionContext), eq("path/kw"), eq(mockNode), eq(mockRootNode), eq("/myPath"), anyString(), any(), any(), any(), any());
                                                                                                                                                                               ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:137: error: cannot find symbol
        verify(walker, times(1)).walk(eq(mockExecutionContext), eq(mockNode), eq(mockRootNode), eq("/myPath"), eq(true));
                       ^
  symbol:   method times(int)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:137: error: cannot find symbol
        verify(walker, times(1)).walk(eq(mockExecutionContext), eq(mockNode), eq(mockRootNode), eq("/myPath"), eq(true));
                                      ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:137: error: cannot find symbol
        verify(walker, times(1)).walk(eq(mockExecutionContext), eq(mockNode), eq(mockRootNode), eq("/myPath"), eq(true));
                                                                ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:137: error: cannot find symbol
        verify(walker, times(1)).walk(eq(mockExecutionContext), eq(mockNode), eq(mockRootNode), eq("/myPath"), eq(true));
                                                                              ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:137: error: cannot find symbol
        verify(walker, times(1)).walk(eq(mockExecutionContext), eq(mockNode), eq(mockRootNode), eq("/myPath"), eq(true));
                                                                                                ^
  symbol:   method eq(String)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:137: error: cannot find symbol
        verify(walker, times(1)).walk(eq(mockExecutionContext), eq(mockNode), eq(mockRootNode), eq("/myPath"), eq(true));
                                                                                                               ^
  symbol:   method eq(boolean)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:138: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                       ^
  symbol:   method times(int)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:138: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                      ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:138: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                             ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:138: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                          ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:138: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                 ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:138: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                        ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:138: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                                     ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:138: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                                                  ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:138: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                                                         ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:138: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                                                                ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:138: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                                                                       ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:138: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                                                                              ^
  symbol:   method eq(Set<ValidationMessage>)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:147: error: cannot find symbol
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
                                  ^
  symbol:   method mock(Class<JsonSchemaWalker>)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:148: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(false);
                                        ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:148: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(false);
                                               ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:148: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(false);
                                                            ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:148: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(false);
                                                                   ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:148: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(false);
                                                                          ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:148: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(false);
                                                                                       ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:148: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(false);
                                                                                                    ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:148: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(false);
                                                                                                           ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:148: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(false);
                                                                                                                  ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:148: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).thenReturn(false);
                                                                                                                         ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:150: error: cannot find symbol
        doReturn(validators).when(spySchema).getValidators();
        ^
  symbol:   method doReturn(Map<String,JsonSchemaWalker>)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:153: error: cannot find symbol
        verify(walker, never()).walk(any(), any(), any(), anyString(), anyBoolean());
                       ^
  symbol:   method never()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:153: error: cannot find symbol
        verify(walker, never()).walk(any(), any(), any(), anyString(), anyBoolean());
                                     ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:153: error: cannot find symbol
        verify(walker, never()).walk(any(), any(), any(), anyString(), anyBoolean());
                                            ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:153: error: cannot find symbol
        verify(walker, never()).walk(any(), any(), any(), anyString(), anyBoolean());
                                                   ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:153: error: cannot find symbol
        verify(walker, never()).walk(any(), any(), any(), anyString(), anyBoolean());
                                                          ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:153: error: cannot find symbol
        verify(walker, never()).walk(any(), any(), any(), anyString(), anyBoolean());
                                                                       ^
  symbol:   method anyBoolean()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:154: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                       ^
  symbol:   method times(int)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:154: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                      ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:154: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                             ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:154: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                          ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:154: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                 ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:154: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                        ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:154: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                                     ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:154: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                                                  ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:154: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                                                         ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:154: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                                                                ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:154: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                                                                       ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:154: error: cannot find symbol
        verify(runner, times(1)).runPostWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any(), eq(messages));
                                                                                                                                              ^
  symbol:   method eq(Set<ValidationMessage>)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:163: error: cannot find symbol
        JsonSchemaWalker walker = mock(JsonSchemaWalker.class);
                                  ^
  symbol:   method mock(Class<JsonSchemaWalker>)
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:165: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).// walker will be skipped
                                        ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:165: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).// walker will be skipped
                                               ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:165: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).// walker will be skipped
                                                            ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:165: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).// walker will be skipped
                                                                   ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:165: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).// walker will be skipped
                                                                          ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:165: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).// walker will be skipped
                                                                                       ^
  symbol:   method anyString()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:165: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).// walker will be skipped
                                                                                                    ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:165: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).// walker will be skipped
                                                                                                           ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:165: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).// walker will be skipped
                                                                                                                  ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:165: error: cannot find symbol
        when(runner.runPreWalkListeners(any(), anyString(), any(), any(), anyString(), anyString(), any(), any(), any(), any())).// walker will be skipped
                                                                                                                         ^
  symbol:   method any()
  location: class JsonSchema_walk_3_Test
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:168: error: cannot find symbol
        doReturn(validators).when(spySchema).getValidators();
        ^
  symbol:   method doReturn(Map<String,JsonSchemaWalker>)
  location: class JsonSchema_walk_3_Test
100 errors
only showing the first 100 errors, of 290 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tcom.networknt.schema.walk.WalkListenerRunner com.networknt.schema.JsonSchema.keywordWalkListenerRunner;\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;\n\t\tmethod `private void updateThisAsSubschema(URI)` returns `void`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`, api document: \n     * Creates a new parent schema from the current state and updates this object to refer to the subschema instead.\n     ;\n\t\tmethod `private URI combineCurrentUriWithIds(URI, JsonNode)` returns `java.net.URI`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\trelated functions:\n\t\tmethod `protected static void registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, String)` returns `void`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`, api document: \n     * Rolls up all nested and compatible discriminators to the root discriminator of the type. Detects attempts to redefine\n     * the <code>propertyName</code> or mappings.\n     *\n     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}\n     * @param discriminator               the discriminator to use for the check\n     * @param schema                      the value of the <code>discriminator/propertyName</code> field\n     * @param at                          the logging prefix\n     ;\nclass `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tjava.lang.String com.networknt.schema.ValidationMessageHandler.schemaPath;\n\t\tcom.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.parentSchema;\n\t\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, JsonSchema, JsonNode, JsonNode, String, boolean, Set<ValidationMessage>)` returns `void`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;\nclass `com.networknt.schema.PropertiesValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, Map.Entry<String, JsonSchema>, JsonNode, JsonNode, String, boolean, Set<ValidationMessage>, WalkListenerRunner)` returns `void`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


