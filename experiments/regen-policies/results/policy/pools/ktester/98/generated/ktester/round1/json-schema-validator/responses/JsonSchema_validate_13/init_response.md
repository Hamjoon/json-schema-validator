```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(ExecutionContext, JsonNode, OutputFormat, ExecutionContextCustomizer)}.
 *
 * The tests focus on the orchestration performed by the method:
 * <ul>
 *   <li>the {@link OutputFormat#customize(ExecutionContext, ValidationContext)} call</li>
 *   <li>the optional {@link ExecutionContextCustomizer#customize(ExecutionContext, ValidationContext)} call</li>
 *   <li>delegation to the internal {@code validate(ExecutionContext, JsonNode)} method</li>
 *   <li>the {@link OutputFormat#format(JsonSchema, Set, ExecutionContext, ValidationContext)} call</li>
 * </ul>
 *
 * All heavy‑weight collaborators are mocked; a real {@link JsonSchema} instance is created
 * via reflection using its private constructor.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_13_Test {

    /** Mocks used across test methods */
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;               // may be null – we use a mock for safety
    @Mock ExecutionContext executionContext;
    @Mock OutputFormat<Object> outputFormat;
    @Mock ExecutionContextCustomizer executionCustomizer;

    /** The JsonSchema instance under test (a spy so we can stub the internal validate call). */
    JsonSchema schemaSpy;

    /** A deterministic set of validation messages returned by the stubbed internal validation. */
    Set<ValidationMessage> dummyMessages = Collections.emptySet();

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* ----------------------------------------------------------------------
         * Create a real JsonSchema instance via its private constructor.
         * ---------------------------------------------------------------------- */
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

        /* ----------------------------------------------------------------------
         * Wrap it in a Mockito spy so we can stub the internal validate(...) call.
         * ---------------------------------------------------------------------- */
        schemaSpy = Mockito.spy(realInstance);

        /* ----------------------------------------------------------------------
         * Stub the internal validation to return a known (empty) set.
         * ---------------------------------------------------------------------- */
        doReturn(dummyMessages)
                .when(schemaSpy)
                .validate(any(ExecutionContext.class), any(JsonNode.class));
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to release for this lightweight test.
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
        verify(outputFormat, times(1))
                .format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
    }

    @Test
    void validate_withoutCustomizer_skipsCustomizer_andStillFormatsResult() {
        // arrange – null customizer, stub OutputFormat
        doNothing().when(outputFormat).customize(executionContext, validationContext);
        when(outputFormat.format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext)))
                .thenReturn(42); // any object to prove the flow works

        // act
        Object result = schemaSpy.validate(executionContext, mock(JsonNode.class), outputFormat, null);

        // assert
        assertEquals(42, result);

        // verify that the customizer was NOT invoked
        verifyNoInteractions(executionCustomizer);
        // other interactions remain the same
        verify(outputFormat, times(1)).customize(executionContext, validationContext);
        verify(schemaSpy, times(1)).validate(executionContext, any(JsonNode.class));
        verify(outputFormat, times(1))
                .format(eq(schemaSpy), eq(dummyMessages), eq(executionContext), eq(validationContext));
    }
}
```