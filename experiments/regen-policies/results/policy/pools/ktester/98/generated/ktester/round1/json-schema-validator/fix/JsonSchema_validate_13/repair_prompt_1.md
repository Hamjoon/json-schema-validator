
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_13_Test {

    /**
     * Mocks used across test methods
     */
    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.fasterxml.jackson.databind.JsonNode;
import org.mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Set;
import java.util.Collections;
    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    // may be null – we use a mock for safety
    @Mock
    JsonSchema parentSchema;

    @Mock
    ExecutionContext executionContext;

    @Mock
    OutputFormat<Object> outputFormat;

    @Mock
    ExecutionContextCustomizer executionCustomizer;

    /**
     * The JsonSchema instance under test (a spy so we can stub the internal validate call).
     */
    JsonSchema schemaSpy;

    /**
     * A deterministic set of validation messages returned by the stubbed internal validation.
     */
    Set<ValidationMessage> dummyMessages = Collections.emptySet();

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* ------------------------------------------------------------------
         * Create a real JsonSchema instance via its private constructor.
         * ------------------------------------------------------------------ */
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        JsonSchema realInstance = ctor.newInstance(validationContext, schemaLocation, evaluationPath, schemaNode, parentSchema, false);
        /* ------------------------------------------------------------------
         * Wrap it in a Mockito spy so we can stub the internal validate(...) call.
         * ------------------------------------------------------------------ */
        schemaSpy = Mockito.spy(realInstance);
        /* ------------------------------------------------------------------
         * By default, the internal validation returns an empty set.
         * Individual tests override this behaviour as needed.
         * ------------------------------------------------------------------ */
        doReturn(dummyMessages).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));
    }

    @Test
    void validate_withCustomizer_callsAllCollaborators_andReturnsFormattedResult() {
        // arrange – stub the OutputFormat behaviour
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext))).thenReturn("formatted-result");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer);
        // assert – the result comes from the OutputFormat
        assertEquals("formatted-result", result);
        // verify orchestration
        verify(outputFormat, times(1)).customize(executionContext, validationContext);
        verify(executionCustomizer, times(1)).customize(executionContext, validationContext);
        verify(schemaSpy, times(1)).validate(executionContext, any(JsonNode.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
    }

    @Test
    void validate_withoutCustomizer_skipsCustomizer_andStillFormatsResult() {
        // arrange – null customizer, stub OutputFormat
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        // any object to prove the flow works
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext))).// any object to prove the flow works
        thenReturn(42);
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
        // assert
        assertEquals(42, result);
        // verify that the customizer was NOT invoked
        verifyNoInteractions(executionCustomizer);
        // other interactions remain the same
        verify(outputFormat, times(1)).customize(executionContext, validationContext);
        verify(schemaSpy, times(1)).validate(executionContext, any(JsonNode.class));
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
    }

    /* ----------------------------------------------------------------------
     * 1. Normal flow – no customizer
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – normal flow (no customizer, no exception)")
    void validate_normalFlow_noCustomizer() {
        // arrange
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext))).thenReturn("formatted-result");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
        // assert
        assertEquals("formatted-result", result);
        verify(outputFormat).customize(executionContext, validationContext);
        verifyNoInteractions(executionCustomizer);
        verify(schemaSpy).validate(executionContext, any(JsonNode.class));
        verify(outputFormat).format(schemaSpy, dummyMessages, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 2. Normal flow – with ExecutionContextCustomizer
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – with ExecutionContextCustomizer")
    void validate_normalFlow_withCustomizer() {
        // arrange
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        doNothing().when(executionCustomizer).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext))).thenReturn(123);
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer);
        // assert
        assertEquals(123, result);
        InOrder inOrder = inOrder(outputFormat, executionCustomizer, schemaSpy, outputFormat);
        inOrder.verify(outputFormat).customize(executionContext, validationContext);
        inOrder.verify(executionCustomizer).customize(executionContext, validationContext);
        inOrder.verify(schemaSpy).validate(executionContext, any(JsonNode.class));
        inOrder.verify(outputFormat).format(schemaSpy, dummyMessages, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 3. FailFastAssertionException handling – normal customizer absent
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – FailFastAssertionException handling (no customizer)")
    void validate_failFast_noCustomizer() {
        // arrange
        ValidationMessage vm = mock(ValidationMessage.class);
        Set<ValidationMessage> failFastMessages = Collections.singleton(vm);
        FailFastAssertionException ffa = mock(FailFastAssertionException.class);
        when(ffa.getValidationMessages()).thenReturn(failFastMessages);
        doThrow(ffa).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext))).thenReturn("ff-result");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
        // assert
        assertEquals("ff-result", result);
        verify(outputFormat).customize(executionContext, validationContext);
        verifyNoInteractions(executionCustomizer);
        verify(outputFormat).format(schemaSpy, failFastMessages, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 4. FailFastAssertionException handling – with customizer
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – FailFastAssertionException handling (with customizer)")
    void validate_failFast_withCustomizer() {
        // arrange
        ValidationMessage vm = mock(ValidationMessage.class);
        Set<ValidationMessage> failFastMessages = Collections.singleton(vm);
        FailFastAssertionException ffa = mock(FailFastAssertionException.class);
        when(ffa.getValidationMessages()).thenReturn(failFastMessages);
        doThrow(ffa).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        doNothing().when(executionCustomizer).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext))).thenReturn("ff-with-customizer");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer);
        // assert
        assertEquals("ff-with-customizer", result);
        InOrder inOrder = inOrder(outputFormat, executionCustomizer, schemaSpy, outputFormat);
        inOrder.verify(outputFormat).customize(executionContext, validationContext);
        inOrder.verify(executionCustomizer).customize(executionContext, validationContext);
        inOrder.verify(schemaSpy).validate(executionContext, any(JsonNode.class));
        inOrder.verify(outputFormat).format(schemaSpy, failFastMessages, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 5. FailFastAssertionException with null messages
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – FailFastAssertionException with null messages")
    void validate_failFast_nullMessages() {
        // arrange
        FailFastAssertionException ffa = mock(FailFastAssertionException.class);
        when(ffa.getValidationMessages()).thenReturn(null);
        doThrow(ffa).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), isNull(), eq(executionContext), eq(validationContext))).thenReturn("null-msg-result");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
        // assert
        assertEquals("null-msg-result", result);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(outputFormat).format(schemaSpy, null, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 6. Null ExecutionContext – should propagate NPE from format.customize
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – null ExecutionContext")
    void validate_nullExecutionContext() {
        // arrange: make format.customize throw NPE when executionContext is null
        doThrow(new NullPointerException("executionContext is null")).when(outputFormat).customize(null, validationContext);
        // act & assert
        NullPointerException ex = assertThrows(NullPointerException.class, () -> schemaSpy.validate(null, mock(JsonNode.class), outputFormat, null));
        assertEquals("executionContext is null", ex.getMessage());
        // ensure no further interactions
        verify(outputFormat).customize(null, validationContext);
        verifyNoMoreInteractions(outputFormat, executionCustomizer, schemaSpy);
    }

    /* ----------------------------------------------------------------------
     * 7. Null OutputFormat – NPE should be thrown before any interaction
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – null OutputFormat")
    void validate_nullOutputFormat() {
        // act & assert
        assertThrows(NullPointerException.class, () -> schemaSpy.validate(executionContext, mock(JsonNode.class), null, null));
    }

    /* ----------------------------------------------------------------------
     * 8. OutputFormat.customize throws RuntimeException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – OutputFormat.customize throws RuntimeException")
    void validate_outputFormatCustomizeThrows() {
        // arrange
        RuntimeException rte = new RuntimeException("customize failed");
        doThrow(rte).when(outputFormat).customize(executionContext, validationContext);
        // act & assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null));
        assertSame(rte, ex);
        verify(outputFormat).customize(executionContext, validationContext);
        verifyNoInteractions(executionCustomizer);
        verifyNoMoreInteractions(outputFormat);
    }

    /* ----------------------------------------------------------------------
     * 9. OutputFormat.format throws RuntimeException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – OutputFormat.format throws RuntimeException")
    void validate_outputFormatFormatThrows() {
        // arrange
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        RuntimeException rte = new RuntimeException("format failed");
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext))).thenThrow(rte);
        // act & assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null));
        assertSame(rte, ex);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(outputFormat).format(schemaSpy, dummyMessages, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 10. ExecutionContextCustomizer throws RuntimeException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – ExecutionContextCustomizer throws RuntimeException")
    void validate_customizerThrows() {
        // arrange
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        RuntimeException rte = new RuntimeException("customizer failed");
        doThrow(rte).when(executionCustomizer).customize(executionContext, validationContext);
        // act & assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer));
        assertSame(rte, ex);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(executionCustomizer).customize(executionContext, validationContext);
        // internal validation and format.format must NOT be called
        verify(schemaSpy, never()).validate(any(), any());
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    /* ----------------------------------------------------------------------
     * 11. Both customizer and format.customize throw – customizer should not run
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – both customizer and format.customize throw")
    void validate_bothThrow_formatFirst() {
        // arrange
        RuntimeException rte = new RuntimeException("format customize failed");
        doThrow(rte).when(outputFormat).customize(executionContext, validationContext);
        // customizer is a mock; we verify it is never invoked
        // act & assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer));
        assertSame(rte, ex);
        verify(outputFormat).customize(executionContext, validationContext);
        verifyNoInteractions(executionCustomizer);
        verify(schemaSpy, never()).validate(any(), any());
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    /* ----------------------------------------------------------------------
     * 12. Internal validate returns null
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – internal validate returns null")
    void validate_internalValidateReturnsNull() {
        // arrange
        doReturn(null).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), isNull(), eq(executionContext), eq(validationContext))).thenReturn("null-set-result");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
        // assert
        assertEquals("null-set-result", result);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(outputFormat).format(schemaSpy, null, executionContext, validationContext);
    }

    /* ----------------------------------------------------------------------
     * 13. Null JsonNode – internal validation should receive null and we simulate NPE
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – null JsonNode")
    void validate_nullJsonNode() {
        // arrange: internal validate throws NPE when node is null
        doThrow(new NullPointerException("node is null")).when(schemaSpy).validate(executionContext, isNull());
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        // act & assert
        NullPointerException ex = assertThrows(NullPointerException.class, () -> schemaSpy.validate(executionContext, null, outputFormat, null));
        assertEquals("node is null", ex.getMessage());
        verify(outputFormat).customize(executionContext, validationContext);
        // format.format must never be called
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    /* ----------------------------------------------------------------------
     * 14. Internal validate throws generic RuntimeException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – internal validate throws RuntimeException")
    void validate_internalValidateThrowsRuntime() {
        // arrange
        IllegalStateException ise = new IllegalStateException("boom");
        doThrow(ise).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        // act & assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null));
        assertSame(ise, ex);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(schemaSpy).validate(executionContext, any(JsonNode.class));
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    /* ----------------------------------------------------------------------
     * 15. Internal validate throws Error (e.g., AssertionError)
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("validate – internal validate throws Error")
    void validate_internalValidateThrowsError() {
        // arrange
        AssertionError error = new AssertionError("assertion failed");
        doThrow(error).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        // act & assert
        AssertionError thrown = assertThrows(AssertionError.class, () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null));
        assertSame(error, thrown);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(schemaSpy).validate(executionContext, any(JsonNode.class));
        verify(outputFormat, never()).format(any(), any(), any(), any());
    }

    @Test
    void placeholder() {
        // This test does nothing but guarantees that the test class compiles.
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:15: error: illegal start of type
import org.mockito.junit.jupiter.MockitoExtension;
^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:15: error: <identifier> expected
import org.mockito.junit.jupiter.MockitoExtension;
                                                 ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:16: error: illegal start of type
import org.junit.jupiter.api.Test;
^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:16: error: <identifier> expected
import org.junit.jupiter.api.Test;
                                 ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:17: error: illegal start of type
import org.junit.jupiter.api.DisplayName;
^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:17: error: <identifier> expected
import org.junit.jupiter.api.DisplayName;
                                        ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:18: error: illegal start of type
import com.fasterxml.jackson.databind.JsonNode;
^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:18: error: <identifier> expected
import com.fasterxml.jackson.databind.JsonNode;
                                              ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:19: error: illegal start of type
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:19: error: <identifier> expected
import org.mockito.*;
                   ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:20: error: illegal start of type
import org.junit.jupiter.api.BeforeEach;
^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:20: error: <identifier> expected
import org.junit.jupiter.api.BeforeEach;
                                       ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:21: error: illegal start of type
import org.junit.jupiter.api.*;
^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:21: error: <identifier> expected
import org.junit.jupiter.api.*;
                             ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:22: error: illegal start of type
import org.junit.jupiter.api.extension.ExtendWith;
^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:22: error: <identifier> expected
import org.junit.jupiter.api.extension.ExtendWith;
                                                 ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:23: error: illegal start of type
import java.util.Set;
^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:23: error: <identifier> expected
import java.util.Set;
                    ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:24: error: illegal start of type
import java.util.Collections;
^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:24: error: <identifier> expected
import java.util.Collections;
                            ^
20 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)': '\n     * Validates to a format.\n     * \n     * @param <T>                 the result type\n     * @param executionContext    the execution context\n     * @param node                the node\n     * @param format              the format\n     * @param executionCustomizer the customizer\n     * @return the result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.ExecutionContextCustomizer`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `private T walkAtNodeInternal(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`;\n\t\tmethod `public ExecutionContext createExecutionContext()` returns `com.networknt.schema.ExecutionContext`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Creates an execution context.\n     * \n     * @return the execution context\n     ;\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;\nclass `com.networknt.schema.FormatValidator`:\n\trelated functions:\n\t\tmethod `protected boolean isStrict(ExecutionContext)` returns `boolean`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \r\n     * Determines if strict handling.\r\n     * <p>\r\n     * Note that this defaults to false.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @return whether to perform strict handling\r\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


