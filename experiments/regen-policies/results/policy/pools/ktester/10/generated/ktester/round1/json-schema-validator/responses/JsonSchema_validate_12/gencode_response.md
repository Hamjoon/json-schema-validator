```java
package com.networknt.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat, Consumer)}.
 *
 * The tests cover the scenarios described in the JSON test‑case specification.
 *
 * <p>Implementation notes</p>
 * <ul>
 *   <li>A real {@link JsonSchema} instance is created via reflection because its constructor is package‑private.</li>
 *   <li>The instance is spied so that internal methods {@code createExecutionContext()},
 *       {@code deserialize(...)} and the final {@code validate(JsonNode, OutputFormat, Consumer)} overload
 *       can be stubbed according to each test case.</li>
 *   <li>For cases that require a {@link ValidationMessage} we use a simple mock – the content of the
 *       message is not important for the assertions.</li>
 *   <li>When an {@link ExecutionContext} customizer is exercised we verify that the consumer receives the
 *       context and that the expected attribute is set.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_12_Test {

    /* ----------------------------------------------------------------------
     *  Mocks and shared fixtures
     * ---------------------------------------------------------------------- */
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock InputFormat inputFormat;               // generic mock – concrete type not required
    @Mock InputFormat yamlInputFormat;           // separate mock for YAML tests
    @Mock ExecutionContext executionContext;     // the context passed to the consumer
    @Mock Consumer<ExecutionContext> executionCustomizer; // consumer supplied by caller

    /** The JsonSchema instance under test (spied) */
    JsonSchema schemaSpy;

    /** A dummy JSON node used as the deserialized input */
    private static final JsonNode DUMMY_ROOT_NODE = JsonMapperFactory.getMapper()
            .createObjectNode()
            .put("dummy", "value");

    /** Simple OutputFormat that returns the node unchanged (used for Set<ValidationMessage> cases) */
    static class IdentityOutputFormat<T> implements OutputFormat<T> {
        @SuppressWarnings("unchecked")
        @Override
        public T apply(JsonNode node) {
            return (T) node; // caller will cast to the expected type
        }
    }

    /** OutputFormat that returns a constant value (used for string result test) */
    static class StringOutputFormat implements OutputFormat<String> {
        @Override
        public String apply(JsonNode node) {
            return node.toString();
        }
    }

    /** OutputFormat that throws a RuntimeException – used to test exception propagation */
    static class ThrowingOutputFormat<T> implements OutputFormat<T> {
        @Override
        public T apply(JsonNode node) {
            throw new RuntimeException("OutputFormat failure");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----------------------------------------------------------------------
        // 1. Build a real JsonSchema instance via reflection (the constructor is private)
        // ----------------------------------------------------------------------
        ObjectNode emptySchemaNode = JsonMapperFactory.getMapper().createObjectNode(); // "{}"
        java.lang.reflect.Constructor<JsonSchema> ctor =
                JsonSchema.class.getDeclaredConstructor(
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
                emptySchemaNode,
                null,               // no parent schema
                false);             // suppressSubSchemaRetrieval

        // ----------------------------------------------------------------------
        // 2. Spy on the real instance so we can stub internal methods
        // ----------------------------------------------------------------------
        schemaSpy = Mockito.spy(realInstance);

        // ----------------------------------------------------------------------
        // 3. Default stubbing – happy‑path behaviour
        // ----------------------------------------------------------------------
        // createExecutionContext() returns the mock executionContext
        doReturn(executionContext).when(schemaSpy).createExecutionContext();

        // deserialize(..) returns a known JsonNode (DUMMY_ROOT_NODE) for the normal cases
        doReturn(DUMMY_ROOT_NODE).when(schemaSpy).deserialize(anyString(), any(InputFormat.class));

        // The final validate(JsonNode, OutputFormat, Consumer) overload simply applies the format
        // to DUMMY_ROOT_NODE (ignoring the consumer – it is verified separately)
        doAnswer(invocation -> {
            OutputFormat<?> fmt = invocation.getArgument(1);
            return fmt.apply(DUMMY_ROOT_NODE);
        }).when(schemaSpy).validate(any(JsonNode.class), any(OutputFormat.class), any(Consumer.class));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions so each test starts clean
        reset(validationContext, schemaLocation, evaluationPath,
                inputFormat, yamlInputFormat, executionContext, executionCustomizer);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup necessary
    }

    /* ----------------------------------------------------------------------
     *  Test cases – one method per group (as defined in the JSON spec)
     * ---------------------------------------------------------------------- */

    @Test
    void validate_validJson_noCustomizer_returnsEmptySet() {
        // GIVEN
        String json = "{\"name\":\"John\",\"age\":30}";
        OutputFormat<Set<ValidationMessage>> identity = new IdentityOutputFormat<>();

        // WHEN
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = schemaSpy.validate(json, inputFormat, identity, executionCustomizer);

        // THEN
        assertTrue(result.isEmpty(), "A valid JSON document should produce an empty validation set");
        verify(executionCustomizer, times(1)).accept(executionContext);
        verify(schemaSpy).deserialize(eq(json), eq(inputFormat));
    }

    @Test
    void validate_invalidJson_syntaxError_propagatesJsonProcessingException() throws Exception {
        // GIVEN
        String malformedJson = "{\"name\":\"John\", \"age\":}";
        JsonProcessingException jpe = new JsonProcessingException("Invalid JSON") {};
        doThrow(jpe).when(schemaSpy).deserialize(eq(malformedJson), any(InputFormat.class));

        // WHEN / THEN
        JsonProcessingException thrown = assertThrows(JsonProcessingException.class,
                () -> schemaSpy.validate(malformedJson, inputFormat,
                        new IdentityOutputFormat<>(), executionCustomizer),
                "Malformed JSON should cause JsonProcessingException to be thrown");
        assertEquals(jpe, thrown);
        verify(executionCustomizer, never()).accept(any());
    }

    @Test
    void validate_validYaml_noCustomizer_returnsEmptySet() {
        // GIVEN
        String yaml = "name: John\nage: 30";
        OutputFormat<Set<ValidationMessage>> identity = new IdentityOutputFormat<>();

        // WHEN
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = schemaSpy.validate(yaml, yamlInputFormat, identity, executionCustomizer);

        // THEN
        assertTrue(result.isEmpty(), "A valid YAML document should produce an empty validation set");
        verify(executionCustomizer, times(1)).accept(executionContext);
        verify(schemaSpy).deserialize(eq(yaml), eq(yamlInputFormat));
    }

    @Test
    void validate_schemaViolation_requiredProperty_returnsSetWithRequiredMessage() {
        // GIVEN
        String jsonMissingName = "{\"age\":30}";
        ValidationMessage requiredMsg = mock(ValidationMessage.class);
        when(requiredMsg.getType()).thenReturn("required");
        when(requiredMsg.getMessage()).thenReturn("required property 'name' is missing");
        Set<ValidationMessage> messages = new HashSet<>();
        messages.add(requiredMsg);

        // Stub the final validate overload to return the prepared set
        doReturn(messages).when(schemaSpy).validate(any(JsonNode.class), any(OutputFormat.class), any(Consumer.class));

        OutputFormat<Set<ValidationMessage>> identity = new IdentityOutputFormat<>();

        // WHEN
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = schemaSpy.validate(jsonMissingName, inputFormat, identity, executionCustomizer);

        // THEN
        assertEquals(1, result.size(), "Exactly one validation message should be returned");
        ValidationMessage vm = result.iterator().next();
        assertEquals("required", vm.getType());
        assertTrue(vm.getMessage().contains("name"));
        verify(executionCustomizer, times(1)).accept(executionContext);
    }

    @Test
    void validate_nullInput_throwsNullPointerException() {
        // GIVEN
        String nullInput = null;

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(nullInput, inputFormat,
                        new IdentityOutputFormat<>(), executionCustomizer),
                "Passing a null input string should result in NPE");
        verify(executionCustomizer, never()).accept(any());
    }

    @Test
    void validate_withExecutionCustomizer_modifiesContext() {
        // GIVEN
        String json = "{\"name\":\"John\",\"age\":30}";
        // Use a real ExecutionContext implementation if available; otherwise a mock works.
        // Here we keep the mock and verify that the consumer invokes setAttribute on it.
        Consumer<ExecutionContext> customizer = ctx -> ctx.setAttribute("customFlag", Boolean.TRUE);
        // Stub createExecutionContext to return the same mock that the customizer will receive
        doReturn(executionContext).when(schemaSpy).createExecutionContext();

        // WHEN
        schemaSpy.validate(json, inputFormat, new IdentityOutputFormat<>(), customizer);

        // THEN
        // Verify that the customizer was indeed called with the executionContext
        verify(executionContext, times(1)).setAttribute(eq("customFlag"), eq(Boolean.TRUE));
        verify(executionCustomizer, never()).accept(any()); // we used a different consumer
    }

    @Test
    void validate_outputFormatThrowsException_propagatesRuntimeException() {
        // GIVEN
        String json = "{\"name\":\"John\",\"age\":30}";
        OutputFormat<String> throwingFormat = new ThrowingOutputFormat<>();

        // WHEN / THEN
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> schemaSpy.validate(json, inputFormat, throwingFormat, executionCustomizer),
                "Exception from OutputFormat should propagate");
        assertEquals("OutputFormat failure", thrown.getMessage());
        verify(executionCustomizer, times(1)).accept(executionContext);
    }

    @Test
    void validate_nullInputFormat_throwsNullPointerException() {
        // GIVEN
        String json = "{\"name\":\"John\",\"age\":30}";
        InputFormat nullFormat = null;

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(json, nullFormat,
                        new IdentityOutputFormat<>(), executionCustomizer),
                "Null InputFormat should cause NPE");
        verify(executionCustomizer, never()).accept(any());
    }

    @Test
    void validate_nullOutputFormat_throwsNullPointerException() {
        // GIVEN
        String json = "{\"name\":\"John\",\"age\":30}";
        OutputFormat<Set<ValidationMessage>> nullFormat = null;

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(json, inputFormat, nullFormat, executionCustomizer),
                "Null OutputFormat should cause NPE");
        verify(executionCustomizer, never()).accept(any());
    }

    @Test
    void validate_nullExecutionCustomizer_throwsNullPointerException() {
        // GIVEN
        String json = "{\"name\":\"John\",\"age\":30}";
        Consumer<ExecutionContext> nullCustomizer = null;

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> schemaSpy.validate(json, inputFormat,
                        new IdentityOutputFormat<>(), nullCustomizer),
                "Null executionCustomizer should cause NPE");
        // No interaction with executionContext because the lambda is never invoked
        verify(executionContext, never()).setAttribute(anyString(), any());
    }

    @Test
    void validate_mismatchedFormat_throwsJsonProcessingException() throws Exception {
        // GIVEN
        String json = "{\"name\":\"John\",\"age\":30}";
        // Simulate that the YAML deserializer receives JSON and fails
        JsonProcessingException jpe = new JsonProcessingException("Mismatched format") {};
        doThrow(jpe).when(schemaSpy).deserialize(eq(json), eq(yamlInputFormat));

        // WHEN / THEN
        JsonProcessingException thrown = assertThrows(JsonProcessingException.class,
                () -> schemaSpy.validate(json, yamlInputFormat,
                        new IdentityOutputFormat<>(), executionCustomizer),
                "Mismatched input format should cause JsonProcessingException");
        assertEquals(jpe, thrown);
        verify(executionCustomizer, never()).accept(any());
    }

    @Test
    void validate_executionCustomizerThrowsRuntimeException_propagatesException() {
        // GIVEN
        String json = "{\"name\":\"John\",\"age\":30}";
        Consumer<ExecutionContext> explodingCustomizer = ctx -> {
            throw new IllegalStateException("Customizer failure");
        };

        // WHEN / THEN
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> schemaSpy.validate(json, inputFormat,
                        new IdentityOutputFormat<>(), explodingCustomizer),
                "Exception from executionCustomizer must propagate");
        assertEquals("Customizer failure", thrown.getMessage());
        // The internal validate overload should never be reached
        verify(schemaSpy, never()).validate(any(JsonNode.class), any(OutputFormat.class), any(Consumer.class));
    }
}
```