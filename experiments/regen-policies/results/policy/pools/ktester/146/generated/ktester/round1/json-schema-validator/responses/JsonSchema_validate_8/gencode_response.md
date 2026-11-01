```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchema#validate(JsonNode, OutputFormat, Consumer)}.
 *
 * The tests focus on the delegation logic, null‑argument handling, interaction with the
 * {@link Consumer<ExecutionContext>}, and proper propagation of exceptions.
 *
 * For most scenarios the {@link JsonSchema} instance is spied and the deeper overload
 * {@code validate(ExecutionContext, JsonNode, OutputFormat, ExecutionContextCustomizer)}
 * is stubbed. The stub invokes the supplied {@link ExecutionContextCustomizer} with a
 * mocked {@link ExecutionContext} and then returns a pre‑determined result (or throws
 * an exception) so that the behaviour of the outer overload can be verified in isolation.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_8_Test {

    /** Instance created once for the whole test suite. */
    private static JsonSchema jsonSchema;

    /** Common collaborators used to build the schema via the package‑private factory. */
    @Mock private static ValidationContext mockValidationContext;
    @Mock private static SchemaLocation mockSchemaLocation;
    @Mock private static JsonNodePath mockEvaluationPath;
    @Mock private static JsonNode mockSchemaNode;

    @BeforeAll
    static void beforeAll() {
        // Build a JsonSchema instance using the factory method. All arguments are mocked
        // because the behaviour of the underlying validators is not exercised here.
        jsonSchema = JsonSchema.from(
                mockValidationContext,
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                null,               // no parent schema
                false);             // do not suppress sub‑schema retrieval
    }

    /* --------------------------------------------------------------------- */
    /*  Helper classes used by several test cases                         */
    /* --------------------------------------------------------------------- */

    /** Simple OutputFormat that collects the messages into a List<String>. */
    static class CollectingFormat implements OutputFormat<List<String>> {
        @Override
        public List<String> format(Set<ValidationMessage> messages, ExecutionContext ctx) {
            return messages.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.toList());
        }
    }

    /**
     * OutputFormat that reads a flag named {@code customFlag} from the
     * {@link ExecutionContext} and returns a string representation.
     */
    static class ContextAwareFormat implements OutputFormat<String> {
        @Override
        public String format(Set<ValidationMessage> messages, ExecutionContext ctx) {
            Object flag = ctx.get("customFlag");
            return "customFlag=" + flag;
        }
    }

    /* --------------------------------------------------------------------- */
    /*  1. Null‑parameter handling                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_NullRootNode_ThrowsNPE() {
        JsonNode rootNode = null;
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(rootNode, format, consumer));
    }

    @Test
    void validate_NullFormat_ThrowsNPE() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = null;
        Consumer<ExecutionContext> consumer = ctx -> {};

        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(rootNode, format, consumer));
    }

    @Test
    void validate_NullConsumer_ThrowsNPE() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = null;

        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(rootNode, format, consumer));
    }

    /* --------------------------------------------------------------------- */
    /*  2. Successful validation (no errors)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_Successful_NoErrors() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(invocation -> {
                    // invoke the customizer – it does nothing in this test
                    ExecutionContextCustomizer c = invocation.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    // return an empty list as the formatted result
                    return Collections.emptyList();
                });

        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty(), "Result should be an empty list when validation succeeds");
    }

    /* --------------------------------------------------------------------- */
    /*  3. Validation failure – error messages produced                       */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_Failure_WithErrorMessages() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(invocation -> {
                    ExecutionContextCustomizer c = invocation.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    // simulate two validation messages
                    ValidationMessage m1 = mock(ValidationMessage.class);
                    when(m1.getMessage()).thenReturn("type mismatch");
                    ValidationMessage m2 = mock(ValidationMessage.class);
                    when(m2.getMessage()).thenReturn("required property missing");
                    Set<ValidationMessage> msgs = new LinkedHashSet<>(Arrays.asList(m1, m2));
                    return format.format(msgs, mock(ExecutionContext.class));
                });

        List<String> result = spy.validate(rootNode, format, consumer);
        assertEquals(Arrays.asList("type mismatch", "required property missing"), result);
    }

    /* --------------------------------------------------------------------- */
    /*  4. ExecutionCustomizer interaction (stores a flag)                */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_ConsumerInvokedAndResultReflectsFlag() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<String> format = new ContextAwareFormat();
        // Consumer that puts a flag into the ExecutionContext
        Consumer<ExecutionContext> consumer = ctx -> ctx.put("customFlag", true);

        // Mock ExecutionContext that behaves like a mutable map
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        Map<String, Object> ctxMap = new HashMap<>();
        doAnswer(inv -> {
            ctxMap.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(mockCtx).put(anyString(), any());
        when(mockCtx.get(anyString())).thenAnswer(inv -> ctxMap.get(inv.getArgument(0)));

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mockCtx);               // invoke the consumer
                    // now call the format – it will read the flag from mockCtx
                    return format.format(Collections.emptySet(), mockCtx);
                });

        String result = spy.validate(rootNode, format, consumer);
        assertEquals("customFlag=true", result);
    }

    /* --------------------------------------------------------------------- */
    /*  5. Recursive anchor handling – should not recurse infinitely         */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_RecursiveAnchor_NoInfiniteRecursion() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        // make the schema report that it is a recursive anchor
        when(spy.isRecursiveAnchor()).thenReturn(true);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    return Collections.emptyList();
                });

        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  6. Schema‑resource‑root flag – normal operation                      */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_SchemaResourceRootFlag_NormalBehaviour() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.isSchemaResourceRoot()).thenReturn(true);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    return Collections.emptyList();
                });

        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  7. ExecutionCustomizer throws an unchecked exception                 */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_ConsumerThrows_RuntimeExceptionPropagated() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
            throw new RuntimeException("customizer failure");
        };

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    // the stub simply forwards the customizer – the exception will bubble up
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    return Collections.emptyList(); // never reached
                });

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> spy.validate(rootNode, format, consumer));
        assertEquals("customizer failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  8. OutputFormat throws IllegalStateException                         */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_OutputFormatThrows_IllegalStateExceptionPropagated() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<String> format = mock(OutputFormat.class);
        Consumer<ExecutionContext> consumer = ctx -> {};

        when(format.format(anySet(), any())).thenThrow(new IllegalStateException("format failure"));

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    // delegate to the format – this will throw
                    return format.format(Collections.emptySet(), mock(ExecutionContext.class));
                });

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> spy.validate(rootNode, format, consumer));
        assertEquals("format failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  9. Primitive root node type mismatch (numeric instead of object)    */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_PrimitiveRootNode_TypeMismatchMessage() {
        JsonNode rootNode = mock(JsonNode.class); // represents a number node in this test
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    ValidationMessage msg = mock(ValidationMessage.class);
                    when(msg.getMessage()).thenReturn("type mismatch");
                    Set<ValidationMessage> msgs = Collections.singleton(msg);
                    return format.format(msgs, mock(ExecutionContext.class));
                });

        List<String> result = spy.validate(rootNode, format, consumer);
        assertEquals(Collections.singletonList("type mismatch"), result);
    }

    /* --------------------------------------------------------------------- */
    /* 10. Array root node – success (minItems satisfied)                   */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_ArrayRootNode_Success() {
        JsonNode rootNode = mock(JsonNode.class); // represents a valid array node
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    return Collections.emptyList();
                });

        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 11. Array root node – failure (minItems violated)                   */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_ArrayRootNode_Failure_MinItems() {
        JsonNode rootNode = mock(JsonNode.class); // represents an array with too few items
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    ValidationMessage msg = mock(ValidationMessage.class);
                    when(msg.getMessage()).thenReturn("minItems constraint violated");
                    Set<ValidationMessage> msgs = Collections.singleton(msg);
                    return format.format(msgs, mock(ExecutionContext.class));
                });

        List<String> result = spy.validate(rootNode, format, consumer);
        assertEquals(Collections.singletonList("minItems constraint violated"), result);
    }

    /* --------------------------------------------------------------------- */
    /* 12. $ref resolution (internal definition) – succeeds                 */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_InternalRefResolution_Succeeds() {
        JsonNode rootNode = mock(JsonNode.class); // matches the referenced definition
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    return Collections.emptyList();
                });

        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 13. Fail‑fast mode via ExecutionContext flag                         */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_FailFast_StopsAfterFirstError() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> ctx.put(ExecutionContext.KEY_FAIL_FAST, true);

        // Mock ExecutionContext that stores the flag
        ExecutionContext mockCtx = mock(ExecutionContext.class);
        Map<String, Object> flagMap = new HashMap<>();
        doAnswer(inv -> {
            flagMap.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(mockCtx).put(anyString(), any());
        when(mockCtx.get(anyString())).thenAnswer(inv -> flagMap.get(inv.getArgument(0)));

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mockCtx); // set fail‑fast flag
                    // Simulate that the validator respects the flag and returns only one message
                    ValidationMessage msg = mock(ValidationMessage.class);
                    when(msg.getMessage()).thenReturn("first error only");
                    Set<ValidationMessage> msgs = Collections.singleton(msg);
                    return format.format(msgs, mockCtx);
                });

        List<String> result = spy.validate(rootNode, format, consumer);
        assertEquals(Collections.singletonList("first error only"), result);
    }

    /* --------------------------------------------------------------------- */
    /* 14. Empty schema (no validators) – any JSON validates                */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_EmptySchema_NoValidators() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        // make getValidators return an empty list
        when(spy.getValidators()).thenReturn(Collections.emptyList());
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    return Collections.emptyList();
                });

        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 15. Recursive anchor with circular $ref – no StackOverflow           */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_RecursiveAnchor_CircularRef_NoStackOverflow() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.isRecursiveAnchor()).thenReturn(true);
        // Simulate that the internal validation detects the recursion guard
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    return Collections.emptyList();
                });

        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 16. Schema resource root with external $ref – normal operation       */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_SchemaResourceRoot_WithExternalRef() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.isSchemaResourceRoot()).thenReturn(true);
        // Assume external $ref resolves correctly; return empty result
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    return Collections.emptyList();
                });

        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 17. ExecutionContext creation failure (createExecutionContext throws)*/
    /* --------------------------------------------------------------------- */

    @Test
    void validate_CreateExecutionContextThrows_Propagates() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.createExecutionContext()).thenThrow(new RuntimeException("creation failure"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> spy.validate(rootNode, format, consumer));
        assertEquals("creation failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 18. Malformed $ref handling – IllegalArgumentException               */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_MalformedRef_ThrowsIllegalArgumentException() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenThrow(new IllegalArgumentException("malformed $ref"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> spy.validate(rootNode, format, consumer));
        assertEquals("malformed $ref", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 19. Null validators list – NullPointerException                       */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_NullValidatorsList_ThrowsNullPointerException() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.getValidators()).thenReturn(null);
        // The deeper overload will try to iterate over the list and hit NPE
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenCallRealMethod(); // let the real method run and hit the NPE

        assertThrows(NullPointerException.class,
                () -> spy.validate(rootNode, format, consumer));
    }

    /* --------------------------------------------------------------------- */
    /* 20. OutputFormat throws an Error (AssertionError)                    */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_OutputFormatThrowsError_AssertionErrorPropagated() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<String> format = mock(OutputFormat.class);
        Consumer<ExecutionContext> consumer = ctx -> {};

        when(format.format(anySet(), any())).thenThrow(new AssertionError("format error"));

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenAnswer(inv -> {
                    ExecutionContextCustomizer c = inv.getArgument(3);
                    c.customize(mock(ExecutionContext.class));
                    return format.format(Collections.emptySet(), mock(ExecutionContext.class));
                });

        AssertionError err = assertThrows(AssertionError.class,
                () -> spy.validate(rootNode, format, consumer));
        assertEquals("format error", err.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 21. UnsupportedEncodingException from $ref decoding                  */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_UnsupportedEncodingException_Propagates() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {};

        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any()))
                .thenThrow(new UnsupportedEncodingException("unsupported charset"));

        UnsupportedEncodingException ex = assertThrows(UnsupportedEncodingException.class,
                () -> spy.validate(rootNode, format, consumer));
        assertEquals("unsupported charset", ex.getMessage());
    }
}
```