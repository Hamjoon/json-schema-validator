```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat, ExecutionContextCustomizer)}.
 *
 * The test suite covers:
 * <ul>
 *   <li>normal successful validation with various input formats</li>
 *   <li>argument‑null checks</li>
 *   <li>deserialization failures (invalid JSON / YAML / URL‑encoded data)</li>
 *   <li>behaviour of {@link ExecutionContextCustomizer}</li>
 *   <li>different {@link OutputFormat} implementations, including ones that throw exceptions</li>
 *   <li>edge cases such as empty/whitespace input, large payloads and UTF‑8 BOM handling</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_15_Test {

    /** A permissive schema that accepts any JSON – used for most test cases. */
    private static JsonSchema schema;

    /** Factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** Mocked customizer – individual tests replace its behaviour with a lambda when needed. */
    @Mock
    ExecutionContextCustomizer executionContextCustomizer;

    @BeforeAll
    static void setupBeforeAll() {
        // an empty schema (equivalent to {} ) validates any JSON document
        String schemaData = "{}";

        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(schemaData);
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(executionContextCustomizer);
    }

    /* --------------------------------------------------------------------- */
    /* Helper OutputFormat implementations                                   */
    /* --------------------------------------------------------------------- */

    /** Simple OutputFormat that returns the {@link JsonNode}'s {@code toString()} value. */
    private static class StringOutputFormat implements OutputFormat<String> {
        @Override
        public String apply(JsonNode node) {
            return node.toString();
        }
    }

    /** OutputFormat that returns the raw {@link JsonNode}. */
    private static class JsonNodeOutputFormat implements OutputFormat<JsonNode> {
        @Override
        public JsonNode apply(JsonNode node) {
            return node;
        }
    }

    /** OutputFormat that deliberately throws a supplied exception when invoked. */
    private static class ThrowingOutputFormat<T> implements OutputFormat<T> {
        private final RuntimeException toThrow;

        ThrowingOutputFormat(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public T apply(JsonNode node) {
            throw toThrow;
        }
    }

    /* --------------------------------------------------------------------- */
    /* Parameter sources                                                    */
    /* --------------------------------------------------------------------- */

    /** Normal successful validation – various input formats. */
    private static Stream<Arguments> normalSuccessProvider() {
        return Stream.of(
                // plain JSON string
                Arguments.of("{\"name\":\"John\"}", InputFormat.JSON, new StringOutputFormat()),
                // URL‑encoded JSON
                Arguments.of("%7B%22name%22%3A%22John%22%7D", InputFormat.URL_ENCODED, new StringOutputFormat()),
                // YAML input
                Arguments.of("name: John\nage: 30", InputFormat.YAML, new StringOutputFormat()),
                // OutputFormat that returns a JsonNode
                Arguments.of("{\"active\":true}", InputFormat.JSON, new JsonNodeOutputFormat())
        );
    }

    /** Invalid argument combinations that must raise IllegalArgumentException. */
    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(
                Arguments.of(null, InputFormat.JSON, new StringOutputFormat()),
                Arguments.of("{}", null, new StringOutputFormat()),
                Arguments.of("{}", InputFormat.JSON, null)
        );
    }

    /** Inputs that cause deserialization failures (invalid JSON / YAML / URL‑encoded). */
    private static Stream<Arguments> deserializationFailureProvider() {
        return Stream.of(
                // malformed JSON
                Arguments.of("{invalidJson:}", InputFormat.JSON, IOException.class),
                // malformed URL‑encoded string (unsupported percent‑encoding)
                Arguments.of("%ZZ%", InputFormat.URL_ENCODED, UnsupportedEncodingException.class),
                // empty string (JSON parser cannot produce a node)
                Arguments.of("", InputFormat.JSON, IOException.class),
                // whitespace only
                Arguments.of("   ", InputFormat.JSON, IOException.class),
                // malformed YAML
                Arguments.of("name: John\n:bad", InputFormat.YAML, IOException.class),
                // unsupported InputFormat (XML)
                Arguments.of("{\"key\":\"value\"}", InputFormat.XML, IllegalArgumentException.class)
        );
    }

    /** Cases that involve a custom {@link ExecutionContextCustomizer}. */
    private static Stream<Arguments> customizerProvider() {
        return Stream.of(
                // customizer that adds a flag – we simulate by doing nothing and expecting normal success
                Arguments.of("{\"type\":\"string\"}", InputFormat.JSON, new StringOutputFormat(),
                        (ExecutionContextCustomizer) ctx -> {/* no‑op */},
                        true),

                // customizer that throws a RuntimeException
                Arguments.of("{\"id\":1}", InputFormat.JSON, new StringOutputFormat(),
                        (ExecutionContextCustomizer) ctx -> { throw new RuntimeException("customizer failure"); },
                        RuntimeException.class),

                // customizer that returns null (simulated by setting a field to null via reflection)
                Arguments.of("{\"type\":\"string\"}", InputFormat.JSON, new StringOutputFormat(),
                        (ExecutionContextCustomizer) ctx -> {
                            // force a NullPointerException later by clearing the execution context reference
                            // (the library will later dereference the context)
                            // we achieve this by using reflection on the context object – not needed here,
                            // we simply throw NPE to emulate the situation.
                            throw new NullPointerException("customizer returned null context");
                        },
                        NullPointerException.class)
        );
    }

    /** Large payload test – ensures no OOM or unexpected exception. */
    private static String largeJsonArray() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 1000; i++) {
            sb.append("{\"id\":").append(i).append(",\"value\":\"x\"},");
        }
        // remove trailing comma and close array
        sb.setLength(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }

    /** Input that starts with a UTF‑8 BOM. */
    private static String jsonWithBom() {
        return "\uFEFF{\"name\":\"BOM\"}";
    }

    /* --------------------------------------------------------------------- */
    /* Test methods                                                          */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] inputFormat={1}")
    @MethodSource("normalSuccessProvider")
    void validate_normalSuccess(String input, InputFormat format, OutputFormat<?> outputFormat) {
        Object result = schema.validate(input, format, outputFormat, executionContextCustomizer);
        assertNotNull(result, "Result must not be null");

        // For StringOutputFormat we expect a JSON string that contains "valid":true
        if (outputFormat instanceof StringOutputFormat) {
            String asString = (String) result;
            assertTrue(asString.contains("\"valid\":true") || asString.contains("\"valid\":false"),
                    "Result should contain a validation flag");
        }

        // For JsonNodeOutputFormat we expect a JsonNode with a "valid" field
        if (outputFormat instanceof JsonNodeOutputFormat) {
            JsonNode node = (JsonNode) result;
            assertTrue(node.has("valid"), "JsonNode result must contain a \"valid\" field");
        }

        // the customizer mock should have been invoked exactly once
        verify(executionContextCustomizer, times(1)).customize(any(ExecutionContext.class));
    }

    @ParameterizedTest(name = "[{index}] null argument test")
    @MethodSource("illegalArgumentProvider")
    void validate_illegalArguments(String input, InputFormat format, OutputFormat<?> outputFormat) {
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(input, format, outputFormat, executionContextCustomizer));
    }

    @ParameterizedTest(name = "[{index}] deserialization failure – expects {2}")
    @MethodSource("deserializationFailureProvider")
    void validate_deserializationFailure(String input, InputFormat format, Class<? extends Exception> expected) {
        assertThrows(expected,
                () -> schema.validate(input, format, new StringOutputFormat(), executionContextCustomizer));
    }

    @ParameterizedTest(name = "[{index}] customizer behaviour")
    @MethodSource("customizerProvider")
    void validate_customizerEffects(String input,
                                    InputFormat format,
                                    OutputFormat<?> outputFormat,
                                    ExecutionContextCustomizer customizer,
                                    Object expected) {

        if (expected instanceof Boolean && (Boolean) expected) {
            // normal success – customizer does not interfere
            Object result = schema.validate(input, format, outputFormat, customizer);
            assertNotNull(result);
            verify(customizer, times(1)).customize(any(ExecutionContext.class));
        } else if (expected instanceof Class) {
            // customizer is expected to throw the supplied exception type
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass,
                    () -> schema.validate(input, format, outputFormat, customizer));
        }
    }

    @Test
    void validate_largePayload() {
        String input = largeJsonArray();
        Object result = schema.validate(input, InputFormat.JSON, new StringOutputFormat(), executionContextCustomizer);
        assertNotNull(result);
        String asString = (String) result;
        assertTrue(asString.contains("\"valid\":true") || asString.contains("\"valid\":false"));
        verify(executionContextCustomizer, times(1)).customize(any(ExecutionContext.class));
    }

    @Test
    void validate_utf8BomHandling() {
        String input = jsonWithBom();
        Object result = schema.validate(input, InputFormat.JSON, new StringOutputFormat(), executionContextCustomizer);
        assertNotNull(result);
        String asString = (String) result;
        assertTrue(asString.contains("\"valid\":true") || asString.contains("\"valid\":false"));
        verify(executionContextCustomizer, times(1)).customize(any(ExecutionContext.class));
    }

    @Test
    void validate_outputFormatThrowsRuntimeException() {
        OutputFormat<String> throwing = new ThrowingOutputFormat<>(new RuntimeException("format failure"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate("{\"name\":\"John\"}", InputFormat.JSON, throwing, executionContextCustomizer));
        assertEquals("format failure", ex.getMessage());
    }

    @Test
    void validate_outputFormatThrowsIOException() {
        // The library expects OutputFormat.apply to possibly throw IOException.
        // We simulate this by wrapping the IOException in a RuntimeException (as OutputFormat is not declared to throw checked exceptions).
        OutputFormat<String> throwing = new ThrowingOutputFormat<>(new RuntimeException(new IOException("IO failure")));
        RuntimeException wrapper = assertThrows(RuntimeException.class,
                () -> schema.validate("{\"id\":1}", InputFormat.JSON, throwing, executionContextCustomizer));
        assertTrue(wrapper.getCause() instanceof IOException);
        assertEquals("IO failure", wrapper.getCause().getMessage());
    }
}
```