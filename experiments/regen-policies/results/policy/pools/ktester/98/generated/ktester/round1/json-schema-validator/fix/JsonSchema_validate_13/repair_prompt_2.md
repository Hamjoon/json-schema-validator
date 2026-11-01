
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;

import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

/**
 * Test class for {@link JsonSchema#validate(ExecutionContext, JsonNode, OutputFormat, ExecutionContextCustomizer)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_13_Test {

    /**
     * Mocks used across test methods
     */
    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

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
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        JsonSchema realInstance = ctor.newInstance(
                validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                false);
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
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                .thenReturn("formatted-result");
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
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                .thenReturn(42);
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
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                .thenReturn("formatted-result");
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
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                .thenReturn(123);
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer);
        // assert
        assertEquals(123, result);
        InOrder inOrder = inOrder(outputFormat, executionCustomizer, schemaSpy);
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
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext)))
                .thenReturn("ff-result");
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
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext)))
                .thenReturn("ff-with-customizer");
        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer);
        // assert
        assertEquals("ff-with-customizer", result);
        InOrder inOrder = inOrder(outputFormat, executionCustomizer, schemaSpy);
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
        when(outputFormat.format(eq(schemaSpy), isNull(), eq(executionContext), eq(validationContext)))
                .thenReturn("null-msg-result");
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
        doThrow(new NullPointerException("executionContext is null"))
                .when(outputFormat).customize(null, validationContext);
        // act & assert
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(null, mock(JsonNode.class), outputFormat, null));
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
        assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), null, null));
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
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null));
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
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                .thenThrow(rte);
        // act & assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null));
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
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer));
        assertSame(rte, ex);
        verify(outputFormat).customize(executionContext, validationContext);
        verify(executionCustomizer).customize(executionContext, validationContext);
        // internal validation and format must NOT be called
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
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer));
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
        when(outputFormat.format(eq(schemaSpy), isNull(), eq(executionContext), eq(validationContext)))
                .thenReturn("null-set-result");
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
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(executionContext, null, outputFormat, null));
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
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null));
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
        AssertionError thrown = assertThrows(AssertionError.class,
                () -> schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null));
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
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:9: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:10: error: package org.mockito does not exist
import org.mockito.InOrder;
                  ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:20: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:21: error: package org.mockito does not exist
import static org.mockito.ArgumentMatchers.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:22: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:23: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:28: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:34: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:37: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:40: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:43: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:47: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:50: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:53: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:56: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:92: error: cannot find symbol
        schemaSpy = Mockito.spy(realInstance);
                    ^
  symbol:   variable Mockito
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:97: error: cannot find symbol
        doReturn(dummyMessages).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));
        ^
  symbol:   method doReturn(Set<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:97: error: cannot find symbol
        doReturn(dummyMessages).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));
                                                         ^
  symbol:   method any(Class<ExecutionContext>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:97: error: cannot find symbol
        doReturn(dummyMessages).when(schemaSpy).validate(any(ExecutionContext.class), any(JsonNode.class));
                                                                                      ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:103: error: cannot find symbol
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        ^
  symbol:   method doNothing()
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:104: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                 ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:104: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                                ^
  symbol:   method eq(Set<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:104: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                                                   ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:104: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                                                                         ^
  symbol:   method eq(ValidationContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:107: error: cannot find symbol
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer);
                                                             ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:111: error: cannot find symbol
        verify(outputFormat, times(1)).customize(executionContext, validationContext);
                             ^
  symbol:   method times(int)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:112: error: cannot find symbol
        verify(executionCustomizer, times(1)).customize(executionContext, validationContext);
                                    ^
  symbol:   method times(int)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:113: error: cannot find symbol
        verify(schemaSpy, times(1)).validate(executionContext, any(JsonNode.class));
                          ^
  symbol:   method times(int)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:113: error: cannot find symbol
        verify(schemaSpy, times(1)).validate(executionContext, any(JsonNode.class));
                                                               ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:114: error: cannot find symbol
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
                             ^
  symbol:   method times(int)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:114: error: cannot find symbol
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
                                              ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:114: error: cannot find symbol
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
                                                             ^
  symbol:   method eq(Set<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:114: error: cannot find symbol
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
                                                                                ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:114: error: cannot find symbol
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
                                                                                                      ^
  symbol:   method eq(ValidationContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:120: error: cannot find symbol
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        ^
  symbol:   method doNothing()
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:121: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                 ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:121: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                                ^
  symbol:   method eq(Set<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:121: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                                                   ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:121: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                                                                         ^
  symbol:   method eq(ValidationContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:124: error: cannot find symbol
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
                                                             ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:128: error: cannot find symbol
        verifyNoInteractions(executionCustomizer);
        ^
  symbol:   method verifyNoInteractions(ExecutionContextCustomizer)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:130: error: cannot find symbol
        verify(outputFormat, times(1)).customize(executionContext, validationContext);
                             ^
  symbol:   method times(int)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:131: error: cannot find symbol
        verify(schemaSpy, times(1)).validate(executionContext, any(JsonNode.class));
                          ^
  symbol:   method times(int)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:131: error: cannot find symbol
        verify(schemaSpy, times(1)).validate(executionContext, any(JsonNode.class));
                                                               ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:132: error: cannot find symbol
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
                             ^
  symbol:   method times(int)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:132: error: cannot find symbol
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
                                              ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:132: error: cannot find symbol
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
                                                             ^
  symbol:   method eq(Set<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:132: error: cannot find symbol
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
                                                                                ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:132: error: cannot find symbol
        verify(outputFormat, times(1)).format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
                                                                                                      ^
  symbol:   method eq(ValidationContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:142: error: cannot find symbol
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        ^
  symbol:   method doNothing()
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:143: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                 ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:143: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                                ^
  symbol:   method eq(Set<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:143: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                                                   ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:143: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                                                                         ^
  symbol:   method eq(ValidationContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:146: error: cannot find symbol
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
                                                             ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:149: error: cannot find symbol
        verify(outputFormat).customize(executionContext, validationContext);
        ^
  symbol:   method verify(OutputFormat<Object>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:150: error: cannot find symbol
        verifyNoInteractions(executionCustomizer);
        ^
  symbol:   method verifyNoInteractions(ExecutionContextCustomizer)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:151: error: cannot find symbol
        verify(schemaSpy).validate(executionContext, any(JsonNode.class));
        ^
  symbol:   method verify(JsonSchema)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:151: error: cannot find symbol
        verify(schemaSpy).validate(executionContext, any(JsonNode.class));
                                                     ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:152: error: cannot find symbol
        verify(outputFormat).format(schemaSpy, dummyMessages, executionContext, validationContext);
        ^
  symbol:   method verify(OutputFormat<Object>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:162: error: cannot find symbol
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        ^
  symbol:   method doNothing()
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:163: error: cannot find symbol
        doNothing().when(executionCustomizer).customize(executionContext, validationContext);
        ^
  symbol:   method doNothing()
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:164: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                 ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:164: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                                ^
  symbol:   method eq(Set<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:164: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                                                   ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:164: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                                                                                         ^
  symbol:   method eq(ValidationContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:167: error: cannot find symbol
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer);
                                                             ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:170: error: cannot find symbol
        InOrder inOrder = inOrder(outputFormat, executionCustomizer, schemaSpy);
        ^
  symbol:   class InOrder
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:170: error: cannot find symbol
        InOrder inOrder = inOrder(outputFormat, executionCustomizer, schemaSpy);
                          ^
  symbol:   method inOrder(OutputFormat<Object>,ExecutionContextCustomizer,JsonSchema)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:173: error: cannot find symbol
        inOrder.verify(schemaSpy).validate(executionContext, any(JsonNode.class));
                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:184: error: cannot find symbol
        ValidationMessage vm = mock(ValidationMessage.class);
                               ^
  symbol:   method mock(Class<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:186: error: cannot find symbol
        FailFastAssertionException ffa = mock(FailFastAssertionException.class);
                                         ^
  symbol:   method mock(Class<FailFastAssertionException>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:187: error: cannot find symbol
        when(ffa.getValidationMessages()).thenReturn(failFastMessages);
        ^
  symbol:   method when(Set<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:188: error: cannot find symbol
        doThrow(ffa).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        ^
  symbol:   method doThrow(FailFastAssertionException)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:188: error: cannot find symbol
        doThrow(ffa).when(schemaSpy).validate(executionContext, any(JsonNode.class));
                                                                ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:189: error: cannot find symbol
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        ^
  symbol:   method doNothing()
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:190: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext)))
                                 ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:190: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext)))
                                                ^
  symbol:   method eq(Set<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:190: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext)))
                                                                      ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:190: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext)))
                                                                                            ^
  symbol:   method eq(ValidationContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:193: error: cannot find symbol
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);
                                                             ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:196: error: cannot find symbol
        verify(outputFormat).customize(executionContext, validationContext);
        ^
  symbol:   method verify(OutputFormat<Object>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:197: error: cannot find symbol
        verifyNoInteractions(executionCustomizer);
        ^
  symbol:   method verifyNoInteractions(ExecutionContextCustomizer)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:198: error: cannot find symbol
        verify(outputFormat).format(schemaSpy, failFastMessages, executionContext, validationContext);
        ^
  symbol:   method verify(OutputFormat<Object>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:208: error: cannot find symbol
        ValidationMessage vm = mock(ValidationMessage.class);
                               ^
  symbol:   method mock(Class<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:210: error: cannot find symbol
        FailFastAssertionException ffa = mock(FailFastAssertionException.class);
                                         ^
  symbol:   method mock(Class<FailFastAssertionException>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:211: error: cannot find symbol
        when(ffa.getValidationMessages()).thenReturn(failFastMessages);
        ^
  symbol:   method when(Set<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:212: error: cannot find symbol
        doThrow(ffa).when(schemaSpy).validate(executionContext, any(JsonNode.class));
        ^
  symbol:   method doThrow(FailFastAssertionException)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:212: error: cannot find symbol
        doThrow(ffa).when(schemaSpy).validate(executionContext, any(JsonNode.class));
                                                                ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:213: error: cannot find symbol
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        ^
  symbol:   method doNothing()
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:214: error: cannot find symbol
        doNothing().when(executionCustomizer).customize(executionContext, validationContext);
        ^
  symbol:   method doNothing()
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:215: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext)))
                                 ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:215: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext)))
                                                ^
  symbol:   method eq(Set<ValidationMessage>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:215: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext)))
                                                                      ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:215: error: cannot find symbol
        when(outputFormat.format(eq(schemaSpy), eq(failFastMessages), eq(executionContext), eq(validationContext)))
                                                                                            ^
  symbol:   method eq(ValidationContext)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:218: error: cannot find symbol
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, executionCustomizer);
                                                             ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:221: error: cannot find symbol
        InOrder inOrder = inOrder(outputFormat, executionCustomizer, schemaSpy);
        ^
  symbol:   class InOrder
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:221: error: cannot find symbol
        InOrder inOrder = inOrder(outputFormat, executionCustomizer, schemaSpy);
                          ^
  symbol:   method inOrder(OutputFormat<Object>,ExecutionContextCustomizer,JsonSchema)
  location: class JsonSchema_validate_13_Test
src/test/java/com/networknt/schema/JsonSchema_validate_13_Test.java:224: error: cannot find symbol
        inOrder.verify(schemaSpy).validate(executionContext, any(JsonNode.class));
                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_validate_13_Test
100 errors
only showing the first 100 errors, of 198 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)': '\n     * Validates to a format.\n     * \n     * @param <T>                 the result type\n     * @param executionContext    the execution context\n     * @param node                the node\n     * @param format              the format\n     * @param executionCustomizer the customizer\n     * @return the result\n     ', 'return type': 'T', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.ExecutionContextCustomizer`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `private T walkAtNodeInternal(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`;\n\t\tmethod `public ExecutionContext createExecutionContext()` returns `com.networknt.schema.ExecutionContext`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Creates an execution context.\n     * \n     * @return the execution context\n     ;\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;\nclass `com.networknt.schema.FormatValidator`:\n\trelated functions:\n\t\tmethod `protected boolean isStrict(ExecutionContext)` returns `boolean`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, OutputFormat<T>, ExecutionContextCustomizer)`, api document: \r\n     * Determines if strict handling.\r\n     * <p>\r\n     * Note that this defaults to false.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @return whether to perform strict handling\r\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


