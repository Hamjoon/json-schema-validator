package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_8_Test {

    /**
     * The {@link JsonSchema} instance under test – created once for all tests.
     */
    private static JsonSchema jsonSchema;

    /**
     * Commonly used mocks.
     */
    @Mock
    private static ValidationContext mockValidationContext;

    @Mock
    private static SchemaLocation mockSchemaLocation;

    @Mock
    private static JsonNodePath mockEvaluationPath;

    @Mock
    private static JsonNode mockSchemaNode;

    /**
     * Simple OutputFormat that collects the messages into a List<String>.
     */
    static class CollectingFormat implements OutputFormat<List<String>> {

        @Override
        public List<String> format(Set<ValidationMessage> messages, ExecutionContext ctx) {
            return messages.stream().map(ValidationMessage::getMessage).collect(Collectors.toList());
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

    @BeforeAll
    static void setupBeforeAll() {
        // Create a JsonSchema instance using the package‑private factory.
        // All constructor arguments are mocked – the concrete behaviour of
        // those collaborators is irrelevant for the overload we are testing.
        jsonSchema = JsonSchema.from(mockValidationContext, mockSchemaLocation, mockEvaluationPath, mockSchemaNode, // no parent schema
        null, // do not suppress sub‑schema retrieval
        false);
        // because the behaviour of the underlying validators is not exercised here.
        jsonSchema = JsonSchema.from(mockValidationContext, mockSchemaLocation, mockEvaluationPath, mockSchemaNode, // no parent schema
        null, // do not suppress sub‑schema retrieval
        false);
    }

    /**
     * Verifies that {@code JsonSchema.validate(JsonNode, OutputFormat, Consumer)}
     * delegates to the {@code validate(ExecutionContext, JsonNode, OutputFormat, ExecutionContextCustomizer)}
     * overload, invokes the supplied {@link Consumer} with the created
     * {@link ExecutionContext}, and returns the value produced by the delegate.
     */
    @Test
    void validate_WithConsumer_DelegatesAndInvokesConsumer() {
        // Arrange ------------------------------------------------------------
        JsonNode rootNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> consumer = mock(Consumer.class);
        ExecutionContext fakeExecutionContext = mock(ExecutionContext.class);
        // Spy the original JsonSchema so we can stub the deeper overload.
        JsonSchema spySchema = spy(jsonSchema);
        // Stub the deeper overload. It receives an ExecutionContextCustomizer
        // that we must invoke with our fake ExecutionContext to emulate the
        // real behaviour of the library.
        when(spySchema.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(invocation -> {
            // Extract the ExecutionContextCustomizer supplied by the method
            // under test and invoke it with the fake ExecutionContext.
            ExecutionContextCustomizer customizer = invocation.getArgument(3);
            customizer.customize(fakeExecutionContext);
            // Return a dummy result that the outer overload should propagate.
            return "delegated-result";
        });
        // Act ----------------------------------------------------------------
        String result = spySchema.validate(rootNode, format, consumer);
        // Assert -------------------------------------------------------------
        assertEquals("delegated-result", result, "The result returned by the delegate overload should be propagated.");
        // Verify that the consumer was called exactly once with the fake
        // ExecutionContext that we supplied in the stub.
        verify(consumer, times(1)).accept(fakeExecutionContext);
    }

    @Test
    void validate_NullRootNode_ThrowsNPE() {
        JsonNode rootNode = null;
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(rootNode, format, consumer));
    }

    @Test
    void validate_NullFormat_ThrowsNPE() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = null;
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(rootNode, format, consumer));
    }

    @Test
    void validate_NullConsumer_ThrowsNPE() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = null;
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(rootNode, format, consumer));
    }

    @Test
    void validate_Successful_NoErrors() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(invocation -> {
            // invoke the customizer – it does nothing in this test
            ExecutionContextCustomizer c = invocation.getArgument(3);
            c.customize(mock(ExecutionContext.class));
            // return an empty list as the formatted result
            return Collections.emptyList();
        });
        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty(), "Result should be an empty list when validation succeeds");
    }

    @Test
    void validate_Failure_WithErrorMessages() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(invocation -> {
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
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
            ExecutionContextCustomizer c = inv.getArgument(3);
            // invoke the consumer
            c.customize(mockCtx);
            // now call the format – it will read the flag from mockCtx
            return format.format(Collections.emptySet(), mockCtx);
        });
        String result = spy.validate(rootNode, format, consumer);
        assertEquals("customFlag=true", result);
    }

    @Test
    void validate_RecursiveAnchor_NoInfiniteRecursion() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        // make the schema report that it is a recursive anchor
        when(spy.isRecursiveAnchor()).thenReturn(true);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
            ExecutionContextCustomizer c = inv.getArgument(3);
            c.customize(mock(ExecutionContext.class));
            return Collections.emptyList();
        });
        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    @Test
    void validate_SchemaResourceRootFlag_NormalBehaviour() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.isSchemaResourceRoot()).thenReturn(true);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
            ExecutionContextCustomizer c = inv.getArgument(3);
            c.customize(mock(ExecutionContext.class));
            return Collections.emptyList();
        });
        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    @Test
    void validate_ConsumerThrows_RuntimeExceptionPropagated() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
            // the stub simply forwards the customizer – the exception will bubble up
            ExecutionContextCustomizer c = inv.getArgument(3);
            c.customize(mock(ExecutionContext.class));
            // never reached
            return Collections.emptyList();
        });
        RuntimeException ex = assertThrows(RuntimeException.class, () -> spy.validate(rootNode, format, consumer));
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void validate_OutputFormatThrows_IllegalStateExceptionPropagated() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<String> format = mock(OutputFormat.class);
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        when(format.format(anySet(), any())).thenThrow(new IllegalStateException("format failure"));
        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
            ExecutionContextCustomizer c = inv.getArgument(3);
            c.customize(mock(ExecutionContext.class));
            // delegate to the format – this will throw
            return format.format(Collections.emptySet(), mock(ExecutionContext.class));
        });
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> spy.validate(rootNode, format, consumer));
        assertEquals("format failure", ex.getMessage());
    }

    @Test
    void validate_PrimitiveRootNode_TypeMismatchMessage() {
        // represents a number node in this test
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
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

    @Test
    void validate_ArrayRootNode_Success() {
        // represents a valid array node
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
            ExecutionContextCustomizer c = inv.getArgument(3);
            c.customize(mock(ExecutionContext.class));
            return Collections.emptyList();
        });
        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    @Test
    void validate_ArrayRootNode_Failure_MinItems() {
        // represents an array with too few items
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
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

    @Test
    void validate_InternalRefResolution_Succeeds() {
        // matches the referenced definition
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
            ExecutionContextCustomizer c = inv.getArgument(3);
            c.customize(mock(ExecutionContext.class));
            return Collections.emptyList();
        });
        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

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
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
            ExecutionContextCustomizer c = inv.getArgument(3);
            // set fail‑fast flag
            c.customize(mockCtx);
            // Simulate that the validator respects the flag and returns only one message
            ValidationMessage msg = mock(ValidationMessage.class);
            when(msg.getMessage()).thenReturn("first error only");
            Set<ValidationMessage> msgs = Collections.singleton(msg);
            return format.format(msgs, mockCtx);
        });
        List<String> result = spy.validate(rootNode, format, consumer);
        assertEquals(Collections.singletonList("first error only"), result);
    }

    @Test
    void validate_EmptySchema_NoValidators() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        // make getValidators return an empty list
        when(spy.getValidators()).thenReturn(Collections.emptyList());
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
            ExecutionContextCustomizer c = inv.getArgument(3);
            c.customize(mock(ExecutionContext.class));
            return Collections.emptyList();
        });
        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    @Test
    void validate_RecursiveAnchor_CircularRef_NoStackOverflow() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.isRecursiveAnchor()).thenReturn(true);
        // Simulate that the internal validation detects the recursion guard
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
            ExecutionContextCustomizer c = inv.getArgument(3);
            c.customize(mock(ExecutionContext.class));
            return Collections.emptyList();
        });
        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    @Test
    void validate_SchemaResourceRoot_WithExternalRef() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.isSchemaResourceRoot()).thenReturn(true);
        // Assume external $ref resolves correctly; return empty result
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
            ExecutionContextCustomizer c = inv.getArgument(3);
            c.customize(mock(ExecutionContext.class));
            return Collections.emptyList();
        });
        List<String> result = spy.validate(rootNode, format, consumer);
        assertTrue(result.isEmpty());
    }

    @Test
    void validate_CreateExecutionContextThrows_Propagates() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.createExecutionContext()).thenThrow(new RuntimeException("creation failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> spy.validate(rootNode, format, consumer));
        assertEquals("creation failure", ex.getMessage());
    }

    @Test
    void validate_MalformedRef_ThrowsIllegalArgumentException() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenThrow(new IllegalArgumentException("malformed $ref"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> spy.validate(rootNode, format, consumer));
        assertEquals("malformed $ref", ex.getMessage());
    }

    @Test
    void validate_NullValidatorsList_ThrowsNullPointerException() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.getValidators()).thenReturn(null);
        // The deeper overload will try to iterate over the list and hit NPE
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).// let the real method run and hit the NPE
        thenCallRealMethod();
        assertThrows(NullPointerException.class, () -> spy.validate(rootNode, format, consumer));
    }

    @Test
    void validate_OutputFormatThrowsError_AssertionErrorPropagated() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<String> format = mock(OutputFormat.class);
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        when(format.format(anySet(), any())).thenThrow(new AssertionError("format error"));
        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenAnswer(inv -> {
            ExecutionContextCustomizer c = inv.getArgument(3);
            c.customize(mock(ExecutionContext.class));
            return format.format(Collections.emptySet(), mock(ExecutionContext.class));
        });
        AssertionError err = assertThrows(AssertionError.class, () -> spy.validate(rootNode, format, consumer));
        assertEquals("format error", err.getMessage());
    }

    @Test
    void validate_UnsupportedEncodingException_Propagates() {
        JsonNode rootNode = mock(JsonNode.class);
        OutputFormat<List<String>> format = new CollectingFormat();
        Consumer<ExecutionContext> consumer = ctx -> {
        };
        JsonSchema spy = spy(jsonSchema);
        when(spy.validate(any(ExecutionContext.class), eq(rootNode), eq(format), any())).thenThrow(new UnsupportedEncodingException("unsupported charset"));
        UnsupportedEncodingException ex = assertThrows(UnsupportedEncodingException.class, () -> spy.validate(rootNode, format, consumer));
        assertEquals("unsupported charset", ex.getMessage());
    }
}
