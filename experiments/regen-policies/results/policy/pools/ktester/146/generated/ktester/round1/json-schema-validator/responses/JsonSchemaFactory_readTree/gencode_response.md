```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.serialization.JsonNodeReader;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory#readTree(String, InputFormat)}.
 *
 * <p>The test suite is organised in groups that mirror the JSON test‑case description
 * supplied with the task.  Each group is exercised by a dedicated test method (or a
 * parameterised test when several inputs share the same verification logic).</p>
 *
 * <p>Two scenarios are covered for every test:</p>
 * <ul>
 *   <li>When {@code jsonNodeReader} is {@code null} – the method must delegate to the
 *       appropriate {@link ObjectMapper} obtained via {@code getObjectMapper(InputFormat)}.</li>
 *   <li>When a custom {@link JsonNodeReader} is injected – the method must delegate to
 *       that reader and return exactly the {@link JsonNode} (or exception) produced by
 *       the reader.</li>
 * </ul>
 *
 * <p>For the custom‑reader scenario the {@code jsonNodeReader} field (package‑private)
 * is injected via reflection because the public API does not expose a setter.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_readTree_Test {

    private JsonSchemaFactory factory;

    @Mock
    private JsonNodeReader mockReader; // used for the "jsonNodeReader is set" tests

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default factory – jsonNodeReader == null
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------
     *  Helper utilities
     * -------------------------------------------------------------- */

    /** Injects {@code mockReader} into the current {@code factory} instance. */
    private void injectMockReader() throws Exception {
        Field field = JsonSchemaFactory.class.getDeclaredField("jsonNodeReader");
        field.setAccessible(true);
        field.set(factory, mockReader);
    }

    /* --------------------------------------------------------------
     *  Group 1 – jsonNodeReader is null – delegate to ObjectMapper
     * -------------------------------------------------------------- */

    @Test
    void readTree_usesObjectMapperWhenReaderIsNull_json() throws IOException {
        String json = "{\"name\":\"John\"}";
        InputFormat format = InputFormat.JSON;

        JsonNode result = factory.readTree(json, format);
        JsonNode expected = OBJECT_MAPPER.readTree(json);
        assertEquals(expected, result, "Result must match ObjectMapper output for JSON");
    }

    @Test
    void readTree_usesObjectMapperWhenReaderIsNull_yaml() throws IOException {
        String yaml = "name: John\nage: 30";
        InputFormat format = InputFormat.YAML;

        JsonNode result = factory.readTree(yaml, format);
        JsonNode expected = new ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory())
                .readTree(yaml);
        assertEquals(expected, result, "Result must match YAML ObjectMapper output");
    }

    @Test
    void readTree_usesObjectMapperWhenReaderIsNull_invalidJson_throwsIOException() {
        String invalid = "{invalid json}";
        InputFormat format = InputFormat.JSON;

        assertThrows(IOException.class,
                () -> factory.readTree(invalid, format),
                "Invalid JSON should cause ObjectMapper.readTree to throw IOException");
    }

    @Test
    void readTree_usesObjectMapperWhenReaderIsNull_nullContent_throwsIOException() {
        String content = null;
        InputFormat format = InputFormat.JSON;

        assertThrows(IOException.class,
                () -> factory.readTree(content, format),
                "Null content should cause ObjectMapper.readTree to throw an IOException");
    }

    @Test
    void readTree_usesObjectMapperWhenReaderIsNull_emptyString_throwsIOException() {
        String content = "";
        InputFormat format = InputFormat.JSON;

        assertThrows(IOException.class,
                () -> factory.readTree(content, format),
                "Empty string should cause ObjectMapper.readTree to throw an IOException");
    }

    @Test
    void readTree_usesObjectMapperWhenReaderIsNull_whitespaceOnlyYaml_throwsIOException() {
        String content = "   ";
        InputFormat format = InputFormat.YAML;

        assertThrows(IOException.class,
                () -> factory.readTree(content, format),
                "Whitespace‑only YAML should cause ObjectMapper.readTree to throw an IOException");
    }

    @Test
    void readTree_usesObjectMapperWhenReaderIsNull_nullInputFormat_throwsNullPointerException() {
        String json = "{\"valid\":true}";
        InputFormat format = null;

        assertThrows(NullPointerException.class,
                () -> factory.readTree(json, format),
                "Null InputFormat should cause a NullPointerException before delegating");
    }

    @Test
    void readTree_usesObjectMapperWhenReaderIsNull_unsupportedFormat_throwsIllegalArgumentException() {
        String json = "{\"key\":\"value\"}";
        // Assuming the enum has a value named XML that is not supported by the factory.
        InputFormat unsupported = InputFormat.XML;

        assertThrows(IllegalArgumentException.class,
                () -> factory.readTree(json, unsupported),
                "Unsupported InputFormat should cause IllegalArgumentException from getObjectMapper");
    }

    /* --------------------------------------------------------------
     *  Group 2 – jsonNodeReader is set – use JsonNodeReader implementation
     * -------------------------------------------------------------- */

    @Test
    void readTree_delegatesToCustomReader_json() throws Exception {
        injectMockReader();

        String json = "{\"id\":123}";
        InputFormat format = InputFormat.JSON;
        JsonNode mockedNode = OBJECT_MAPPER.readTree("{\"id\":123}");
        when(mockReader.readTree(json, format)).thenReturn(mockedNode);

        JsonNode result = factory.readTree(json, format);
        assertSame(mockedNode, result, "Factory must return the node supplied by JsonNodeReader");
        verify(mockReader, times(1)).readTree(json, format);
    }

    @Test
    void readTree_delegatesToCustomReader_yaml() throws Exception {
        injectMockReader();

        String yaml = "id: 123\nactive: true";
        InputFormat format = InputFormat.YAML;
        JsonNode mockedNode = OBJECT_MAPPER.readTree("{\"id\":123,\"active\":true}");
        when(mockReader.readTree(yaml, format)).thenReturn(mockedNode);

        JsonNode result = factory.readTree(yaml, format);
        assertSame(mockedNode, result);
        verify(mockReader).readTree(yaml, format);
    }

    @Test
    void readTree_customReader_throwsIOException_propagates() throws Exception {
        injectMockReader();

        String json = "{\"id\":1}";
        InputFormat format = InputFormat.JSON;
        when(mockReader.readTree(json, format)).thenThrow(new IOException("simulated I/O error"));

        IOException ex = assertThrows(IOException.class,
                () -> factory.readTree(json, format),
                "IOException from JsonNodeReader must propagate unchanged");
        assertEquals("simulated I/O error", ex.getMessage());
        verify(mockReader).readTree(json, format);
    }

    @Test
    void readTree_customReader_nullContent_throwsIOException() throws Exception {
        injectMockReader();

        String content = null;
        InputFormat format = InputFormat.YAML;
        when(mockReader.readTree(content, format)).thenThrow(new IOException("null content"));

        IOException ex = assertThrows(IOException.class,
                () -> factory.readTree(content, format));
        assertEquals("null content", ex.getMessage());
        verify(mockReader).readTree(content, format);
    }

    @Test
    void readTree_customReader_emptyString_returnsConfiguredNode() throws Exception {
        injectMockReader();

        String content = "";
        InputFormat format = InputFormat.JSON;
        JsonNode emptyNode = OBJECT_MAPPER.readTree("null"); // could be NullNode
        when(mockReader.readTree(content, format)).thenReturn(emptyNode);

        JsonNode result = factory.readTree(content, format);
        assertSame(emptyNode, result);
        verify(mockReader).readTree(content, format);
    }

    @Test
    void readTree_customReader_nullInputFormat_throwsNullPointerException() throws Exception {
        injectMockReader();

        String json = "{\"key\":\"value\"}";
        InputFormat format = null;
        // The factory should fail before invoking the reader.
        assertThrows(NullPointerException.class,
                () -> factory.readTree(json, format),
                "Null InputFormat must cause NPE before delegating to JsonNodeReader");
        verifyNoInteractions(mockReader);
    }

    @Test
    void readTree_customReader_complexNestedStructure() throws Exception {
        injectMockReader();

        String json = "{\"nested\":{\"a\":1,\"b\":2}}";
        InputFormat format = InputFormat.JSON;
        JsonNode complexNode = OBJECT_MAPPER.readTree(json);
        when(mockReader.readTree(json, format)).thenReturn(complexNode);

        JsonNode result = factory.readTree(json, format);
        assertSame(complexNode, result);
        verify(mockReader).readTree(json, format);
    }

    @Test
    void readTree_customReader_returnsNull_propagatesNull() throws Exception {
        injectMockReader();

        String yaml = "any: string";
        InputFormat format = InputFormat.YAML;
        when(mockReader.readTree(yaml, format)).thenReturn(null);

        JsonNode result = factory.readTree(yaml, format);
        assertNull(result, "Factory must propagate a null JsonNode from the custom reader");
        verify(mockReader).readTree(yaml, format);
    }

    @Test
    void readTree_customReader_throwsRuntimeException_propagates() throws Exception {
        injectMockReader();

        String json = "{\"id\":1}";
        InputFormat format = InputFormat.JSON;
        when(mockReader.readTree(json, format)).thenThrow(new IllegalStateException("boom"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> factory.readTree(json, format));
        assertEquals("boom", ex.getMessage());
        verify(mockReader).readTree(json, format);
    }

    @Test
    void readTree_customReader_nullContent_throwsNullPointerException_propagates() throws Exception {
        injectMockReader();

        String content = null;
        InputFormat format = InputFormat.YAML;
        when(mockReader.readTree(content, format)).thenThrow(new NullPointerException("null content"));

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> factory.readTree(content, format));
        assertEquals("null content", ex.getMessage());
        verify(mockReader).readTree(content, format);
    }

    /* --------------------------------------------------------------
     *  Parameterised tests for repetitive null/empty handling (reader null)
     * -------------------------------------------------------------- */

    private static Stream<Arguments> nullOrEmptyContentProvider() {
        return Stream.of(
                Arguments.of(null, InputFormat.JSON, IOException.class),
                Arguments.of("", InputFormat.JSON, IOException.class),
                Arguments.of("   ", InputFormat.YAML, IOException.class)
        );
    }

    @ParameterizedTest(name = "null/empty content [{0}] with format {1} → expect {2}")
    @MethodSource("nullOrEmptyContentProvider")
    void readTree_nullOrEmptyContent_whenReaderIsNull(String content,
                                                      InputFormat format,
                                                      Class<? extends Exception> expectedException) {
        assertThrows(expectedException,
                () -> factory.readTree(content, format));
    }

    /* --------------------------------------------------------------
     *  Parameterised tests for custom reader returning various nodes
     * -------------------------------------------------------------- */

    private static Stream<Arguments> customReaderReturnProvider() {
        return Stream.of(
                Arguments.of("{\"id\":123}", InputFormat.JSON, "{\"id\":123}"),
                Arguments.of("id: 123\nactive: true", InputFormat.YAML, "{\"id\":123,\"active\":true}")
        );
    }

    @ParameterizedTest(name = "customReader returns node for content \"{0}\" with format {1}")
    @MethodSource("customReaderReturnProvider")
    void readTree_customReader_returnsParsedNode(String content,
                                                  InputFormat format,
                                                  String expectedJson) throws Exception {
        injectMockReader();

        JsonNode expectedNode = OBJECT_MAPPER.readTree(expectedJson);
        when(mockReader.readTree(content, format)).thenReturn(expectedNode);

        JsonNode result = factory.readTree(content, format);
        assertSame(expectedNode, result);
        verify(mockReader).readTree(content, format);
    }
}
```