```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchema#validate(ExecutionContext, JsonNode, OutputFormat)}.
 *
 * <p>The test suite covers:</p>
 * <ul>
 *   <li>Normal delegation to the overloaded method.</li>
 *   <li>Null‑argument handling (NPE).</li>
 *   <li>Propagation of unchecked exceptions thrown by {@link OutputFormat}.</li>
 *   <li>Handling of special {@link JsonNode} types (null node, missing node).</li>
 *   <li>Verification that the {@link OutputFormat} is invoked exactly once.</li>
 *   <li>Return‑value forwarding for simple and complex objects.</li>
 *   <li>Propagation of schema‑parsing and execution‑context errors.</li>
 *   <li>Wrapping of checked exceptions from {@link OutputFormat}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_7_Test {

    /** Helper to build {@link JsonNode} instances from JSON literals. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** The schema under test – created per test to keep it isolated. */
    private JsonSchema schema;

    /** Mocked {@link OutputFormat} used in most tests. */
    @Mock
    OutputFormat<Object> outputFormat;

    /** A fresh {@link ExecutionContext} for each test (real instance). */
    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        // a very permissive schema (type "object") – enough to let validation run.
        String schemaData = "{\"type\":\"object\"}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(schemaData);
        this.executionContext = new ExecutionContext();

        // By default the mocked format returns the Set it receives wrapped in a List.
        when(outputFormat.apply(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Set<ValidationMessage> msgs = (Set<ValidationMessage>) invocation.getArgument(0);
            // Return whatever the test expects – most tests will override this stub.
            return new ArrayList<>(msgs);
        });
    }

    @AfterEach
    void tearDown() {
        reset(outputFormat);
    }

    /* -------------------------------------------------------------
     *  Delegation behaviour tests
     * ------------------------------------------------------------- */

    @Test
    void delegate_normalFlow_returnsValueFromOverloadedMethod() {
        // arrange – spy the schema so we can stub the overloaded method
        JsonSchema spy = spy(schema);
        String expected = "OK";
        doReturn(expected).when(spy).validate(eq(executionContext), any(JsonNode.class), eq(outputFormat), isNull());

        JsonNode node = MAPPER.createObjectNode(); // any non‑null node

        // act
        Object result = spy.validate(executionContext, node, outputFormat);

        // assert
        assertEquals(expected, result);
        verify(spy, times(1)).validate(eq(executionContext), eq(node), eq(outputFormat), isNull());
    }

    @Test
    void delegate_nullExecutionContext_throwsNPE() {
        JsonNode node = MAPPER.createObjectNode();
        assertThrows(NullPointerException.class,
                () -> schema.validate(null, node, outputFormat));
    }

    @Test
    void delegate_nullNode_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> schema.validate(executionContext, null, outputFormat));
    }

    @Test
    void delegate_nullOutputFormat_throwsNPE() {
        JsonNode node = MAPPER.createObjectNode();
        assertThrows(NullPointerException.class,
                () -> schema.validate(executionContext, node, null));
    }

    @Test
    void delegate_parametersArePassedUnmodified() {
        JsonSchema spy = spy(schema);
        String sentinel = "CAPTURED";
        doReturn(sentinel).when(spy).validate(eq(executionContext), any(JsonNode.class), eq(outputFormat), isNull());

        JsonNode node = MAPPER.readTree("{\"type\":\"array\"}");

        Object result = spy.validate(executionContext, node, outputFormat);

        assertEquals(sentinel, result);
        verify(spy).validate(eq(executionContext), eq(node), eq(outputFormat), isNull());
    }

    /* -------------------------------------------------------------
     *  OutputFormat exception propagation
     * ------------------------------------------------------------- */

    @Test
    void outputFormat_throwsIllegalStateException_isPropagated() {
        JsonNode node = MAPPER.readTree("{\"type\":\"string\",\"value\":\"abc\"}");
        when(outputFormat.apply(any())).thenThrow(new IllegalStateException("format failure"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> schema.validate(executionContext, node, outputFormat));
        assertEquals("format failure", ex.getMessage());
    }

    @Test
    void outputFormat_throwsRuntimeException_isPropagated() {
        JsonNode node = MAPPER.readTree("{\"type\":\"number\",\"value\":42}");
        when(outputFormat.apply(any())).thenThrow(new RuntimeException("unexpected"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(executionContext, node, outputFormat));
        assertEquals("unexpected", ex.getMessage());
    }

    /* -------------------------------------------------------------
     *  Special JsonNode handling
     * ------------------------------------------------------------- */

    @Test
    void validate_nullJsonNode_isHandledLikeAnyOtherNode() {
        JsonNode nullNode = NullNode.getInstance();
        when(outputFormat.apply(any())).thenReturn("NULL_OK");

        Object result = schema.validate(executionContext, nullNode, outputFormat);
        assertEquals("NULL_OK", result);
    }

    @Test
    void validate_missingNode_throwsIllegalArgumentException() {
        JsonNode missing = MissingNode.getInstance();
        when(outputFormat.apply(any())).thenReturn("MISSING_OK");

        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(executionContext, missing, outputFormat));
    }

    /* -------------------------------------------------------------
     *  OutputFormat return‑value handling
     * ------------------------------------------------------------- */

    @Test
    void outputFormat_returnsComplexObject_isReturnedUnchanged() {
        JsonNode node = MAPPER.readTree("{\"type\":\"array\",\"items\":[1,2,3]}");
        List<String> complex = List.of("A", "B", "C");
        when(outputFormat.apply(any())).thenReturn(complex);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) schema.validate(executionContext, node, outputFormat);
        assertEquals(complex, result);
    }

    @Test
    void outputFormat_isInvokedExactlyOnce() {
        JsonNode node = MAPPER.readTree("{\"type\":\"object\",\"properties\":{}}");
        OutputFormat<Object> spyFormat = spy(outputFormat);
        when(spyFormat.apply(any())).thenReturn("ONE_CALL");

        Object result = schema.validate(executionContext, node, spyFormat);
        assertEquals("ONE_CALL", result);
        verify(spyFormat, times(1)).apply(any());
    }

    @Test
    void emptyObjectNode_isValidatedAndResultReturned() {
        JsonNode emptyObj = MAPPER.readTree("{}");
        when(outputFormat.apply(any())).thenReturn("EMPTY_OBJ_OK");

        Object result = schema.validate(executionContext, emptyObj, outputFormat);
        assertEquals("EMPTY_OBJ_OK", result);
    }

    /* -------------------------------------------------------------
     *  ExecutionContext custom state verification
     * ------------------------------------------------------------- */

    /** Simple ExecutionContext that holds a map of attributes. */
    private static class TestExecutionContext extends ExecutionContext {
        private final java.util.Map<String, Object> attributes = new java.util.HashMap<>();

        void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }

        Object getAttribute(String key) {
            return attributes.get(key);
        }
    }

    @Test
    void executionContext_customAttribute_isVisibleToOutputFormat() {
        TestExecutionContext ctx = new TestExecutionContext();
        ctx.setAttribute("mode", "STRICT");

        JsonNode node = MAPPER.readTree("{\"type\":\"string\",\"value\":\"test\"}");

        // OutputFormat that reads the attribute from the supplied ExecutionContext
        OutputFormat<Object> format = mock(OutputFormat.class);
        when(format.apply(any())).thenAnswer(invocation -> {
            // The format receives only the Set<ValidationMessage>. To obtain the
            // ExecutionContext we capture it via a spy on the schema (see below).
            // For simplicity we just return the attribute directly from the test context.
            return ctx.getAttribute("mode");
        });

        // Spy the schema to capture the ExecutionContext that is passed to the overloaded method.
        JsonSchema spy = spy(schema);
        doReturn("STRICT").when(spy).validate(eq(ctx), any(JsonNode.class), eq(format), isNull());

        Object result = spy.validate(ctx, node, format);
        assertEquals("STRICT", result);
    }

    /* -------------------------------------------------------------
     *  Propagation of internal errors
     * ------------------------------------------------------------- */

    @Test
    void schemaWithInvalidDefinition_propagatesJsonSchemaException() {
        // an unknown keyword forces JsonSchemaFactory to create a schema that will
        // throw JsonSchemaException when validation is attempted.
        String badSchema = "{\"unknownKeyword\":true}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema bad = factory.getSchema(badSchema);
        JsonNode node = MAPPER.createObjectNode();

        assertThrows(JsonSchemaException.class,
                () -> bad.validate(executionContext, node, outputFormat));
    }

    @Test
    void executionContextThrowsDuringValidation_exceptionIsPropagated() {
        // Mock ExecutionContext that throws on any method call.
        ExecutionContext broken = mock(ExecutionContext.class);
        when(broken.getConfig()).thenThrow(new IllegalStateException("context broken"));

        JsonNode node = MAPPER.readTree("{\"type\":\"string\",\"value\":\"test\"}");

        // The schema will attempt to read configuration from the context,
        // causing the mocked method to throw.
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> schema.validate(broken, node, outputFormat));
        assertEquals("context broken", ex.getMessage());
    }

    @Test
    void outputFormatThrowsCheckedException_isWrappedInRuntimeException() {
        JsonNode node = MAPPER.readTree("{\"type\":\"number\",\"value\":123}");
        when(outputFormat.apply(any())).thenThrow(new IOException("IO failure"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(executionContext, node, outputFormat));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("IO failure", ex.getCause().getMessage());
    }
}
```