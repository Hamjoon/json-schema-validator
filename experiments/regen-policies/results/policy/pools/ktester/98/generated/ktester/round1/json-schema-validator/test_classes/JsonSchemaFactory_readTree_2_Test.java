package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.serialization.JsonNodeReader;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_readTree_2_Test {

    private JsonSchemaFactory factory;

    @BeforeEach
    void setupBeforeEach() {
        // Use the public factory method – it creates a JsonSchemaFactory with a null JsonNodeReader.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /* --------------------------------------------------------------------- *
     *  Helper to inject a mock JsonNodeReader into a factory instance.
     * --------------------------------------------------------------------- */
    private static void injectJsonNodeReader(JsonSchemaFactory target, JsonNodeReader reader) throws Exception {
        Field f = JsonSchemaFactory.class.getDeclaredField("jsonNodeReader");
        f.setAccessible(true);
        f.set(target, reader);
    }

    private static Stream<Arguments> nullReaderProvider() {
        return Stream.of(// valid JSON
        Arguments.of(new ByteArrayInputStream("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8)), InputFormat.JSON, "Valid JSON should be parsed", null), // valid YAML
        Arguments.of(new ByteArrayInputStream("key: value".getBytes(StandardCharsets.UTF_8)), InputFormat.YAML, "Valid YAML should be parsed", null), // empty/invalid JSON – ObjectMapper throws JsonProcessingException wrapped as IOException
        Arguments.of(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)), InputFormat.JSON, "Empty JSON should cause IOException", IOException.class), // null InputStream – ObjectMapper throws NullPointerException
        Arguments.of(null, InputFormat.JSON, "Null InputStream should cause NullPointerException", NullPointerException.class));
    }

    /**
     * Verify that JSON input is parsed correctly when no JsonNodeReader is configured.
     */
    @Test
    void readTree_jsonInput_returnsExpectedNode() throws IOException {
        // GIVEN
        String json = "{\"name\":\"test\",\"value\":42}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        // WHEN
        JsonNode node = factory.readTree(is, InputFormat.JSON);
        // THEN
        assertNotNull(node, "The returned JsonNode should not be null");
        assertEquals("test", node.get("name").asText(), "Field 'name' should match the JSON value");
        assertEquals(42, node.get("value").asInt(), "Field 'value' should match the JSON value");
    }

    /**
     * Verify that YAML input is parsed correctly when no JsonNodeReader is configured.
     */
    @Test
    void readTree_yamlInput_returnsExpectedNode() throws IOException {
        // GIVEN
        String yaml = "name: yamlTest\nvalue: 99";
        InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        // WHEN
        JsonNode node = factory.readTree(is, InputFormat.YAML);
        // THEN
        assertNotNull(node, "The returned JsonNode should not be null");
        assertEquals("yamlTest", node.get("name").asText(), "Field 'name' should match the YAML value");
        assertEquals(99, node.get("value").asInt(), "Field 'value' should match the YAML value");
    }

    /**
     * Verify that when a custom JsonNodeReader is supplied the factory delegates to it.
     */
    @Test
    void readTree_withCustomJsonNodeReader_delegatesToReader() throws Exception {
        // GIVEN
        // Create a mock JsonNodeReader that returns a predetermined JsonNode.
        JsonNodeReader mockReader = mock(JsonNodeReader.class);
        JsonNode expectedNode = factory.getJsonMapper().readTree("{\"mock\":\"true\"}");
        when(mockReader.readTree(any(InputStream.class), eq(InputFormat.JSON))).thenReturn(expectedNode);
        // Build a JsonSchemaFactory instance that uses the mock JsonNodeReader.
        // The constructor is private, so we obtain it via reflection.
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, ObjectMapper.class, JsonNodeReader.class, String.class, SchemaLoaders.Builder.class, SchemaMappers.Builder.class, java.util.concurrent.ConcurrentMap.class, boolean.class, JsonMetaSchemaFactory.class);
        ctor.setAccessible(true);
        // Minimal required arguments – we reuse the mappers from a regular factory instance.
        JsonSchemaFactory factoryWithReader = ctor.newInstance(factory.getJsonMapper(), factory.getYamlMapper(), mockReader, // default meta‑schema IRI
        "https://json-schema.org/draft/2020-12/schema", null, null, new java.util.concurrent.ConcurrentHashMap<>(), false, null);
        String json = "{\"any\":\"data\"}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        // WHEN
        JsonNode actualNode = factoryWithReader.readTree(is, InputFormat.JSON);
        // THEN
        assertSame(expectedNode, actualNode, "The factory should return the node provided by the custom JsonNodeReader");
        // Verify delegation
        verify(mockReader, times(1)).readTree(any(InputStream.class), eq(InputFormat.JSON));
    }

    /* --------------------------------------------------------------------- *
     *  Group: jsonNodeReader is null – delegate to ObjectMapper
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("nullReaderProvider")
    void readTree_nullReader_variousScenarios(InputStream content, InputFormat format, String description, Class<? extends Throwable> expectedException) throws IOException {
        if (expectedException != null) {
            // Expect an exception to be thrown
            assertThrows(expectedException, () -> factory.readTree(content, format), description);
        } else {
            // Normal parsing – verify node structure
            JsonNode node = factory.readTree(content, format);
            assertNotNull(node, description);
            assertEquals("value", node.get("key").asText(), "Parsed node should contain field 'key' with value 'value'");
        }
    }

    /* --------------------------------------------------------------------- *
     *  Group: jsonNodeReader is provided – delegate to JsonNodeReader
     * --------------------------------------------------------------------- */
    @Test
    void readTree_withReader_parsesJson() throws Exception {
        JsonNodeReader mockReader = mock(JsonNodeReader.class);
        JsonNode expected = factory.getJsonMapper().readTree("{\"a\":1}");
        when(mockReader.readTree(any(InputStream.class), eq(InputFormat.JSON))).thenReturn(expected);
        injectJsonNodeReader(factory, mockReader);
        InputStream is = new ByteArrayInputStream("{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        JsonNode actual = factory.readTree(is, InputFormat.JSON);
        assertSame(expected, actual, "Should return node from mocked JsonNodeReader");
        verify(mockReader).readTree(any(InputStream.class), eq(InputFormat.JSON));
    }

    @Test
    void readTree_withReader_parsesYaml() throws Exception {
        JsonNodeReader mockReader = mock(JsonNodeReader.class);
        JsonNode expected = factory.getJsonMapper().readTree("{\"b\":2}");
        when(mockReader.readTree(any(InputStream.class), eq(InputFormat.YAML))).thenReturn(expected);
        injectJsonNodeReader(factory, mockReader);
        InputStream is = new ByteArrayInputStream("b: 2".getBytes(StandardCharsets.UTF_8));
        JsonNode actual = factory.readTree(is, InputFormat.YAML);
        assertSame(expected, actual);
        verify(mockReader).readTree(any(InputStream.class), eq(InputFormat.YAML));
    }

    @Test
    void readTree_withReader_malformedContent_throwsIOException() throws Exception {
        JsonNodeReader mockReader = mock(JsonNodeReader.class);
        when(mockReader.readTree(any(InputStream.class), eq(InputFormat.JSON))).thenThrow(new IOException("malformed"));
        injectJsonNodeReader(factory, mockReader);
        InputStream is = new ByteArrayInputStream("invalid".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> factory.readTree(is, InputFormat.JSON), "Malformed content should propagate IOException from JsonNodeReader");
    }

    @Test
    void readTree_withReader_nullInputStream_propagatesException() throws Exception {
        JsonNodeReader mockReader = mock(JsonNodeReader.class);
        when(mockReader.readTree(isNull(), any())).thenThrow(new NullPointerException("null stream"));
        injectJsonNodeReader(factory, mockReader);
        assertThrows(NullPointerException.class, () -> factory.readTree(null, InputFormat.JSON), "Null InputStream should propagate exception from JsonNodeReader");
    }

    /* --------------------------------------------------------------------- *
     *  Group: inputFormat null handling (jsonNodeReader == null)
     * --------------------------------------------------------------------- */
    @Test
    void readTree_nullInputFormat_withoutReader_throwsNPE() {
        InputStream is = new ByteArrayInputStream("{\"x\":1}".getBytes(StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> factory.readTree(is, null), "Passing null InputFormat should cause NullPointerException from getObjectMapper");
    }

    /* --------------------------------------------------------------------- *
     *  Group: jsonNodeReader throws unchecked exception
     * --------------------------------------------------------------------- */
    @Test
    void readTree_withReader_uncheckedException_propagates() throws Exception {
        JsonNodeReader mockReader = mock(JsonNodeReader.class);
        when(mockReader.readTree(any(InputStream.class), any())).thenThrow(new IllegalStateException("unchecked"));
        injectJsonNodeReader(factory, mockReader);
        InputStream is = new ByteArrayInputStream("{\"a\":true}".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalStateException.class, () -> factory.readTree(is, InputFormat.JSON), "Unchecked exception from JsonNodeReader must propagate");
    }

    /* --------------------------------------------------------------------- *
     *  Group: jsonNodeReader returns null
     * --------------------------------------------------------------------- */
    @Test
    void readTree_withReader_returnsNull() throws Exception {
        JsonNodeReader mockReader = mock(JsonNodeReader.class);
        when(mockReader.readTree(any(InputStream.class), any())).thenReturn(null);
        injectJsonNodeReader(factory, mockReader);
        InputStream is = new ByteArrayInputStream("{\"nullNode\":null}".getBytes(StandardCharsets.UTF_8));
        JsonNode result = factory.readTree(is, InputFormat.JSON);
        assertNull(result, "Factory should return null when JsonNodeReader returns null");
    }

    /* --------------------------------------------------------------------- *
     *  Group: format/content mismatch
     * --------------------------------------------------------------------- */
    @Test
    void readTree_jsonFormat_withYamlContent_throwsIOException() {
        InputStream is = new ByteArrayInputStream("key: value".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> factory.readTree(is, InputFormat.JSON), "JSON mapper should fail on YAML content");
    }

    @Test
    void readTree_yamlFormat_withJsonContent_succeeds() throws IOException {
        // Jackson's YAML parser accepts JSON as a subset.
        InputStream is = new ByteArrayInputStream("{\"k\":\"v\"}".getBytes(StandardCharsets.UTF_8));
        JsonNode node = factory.readTree(is, InputFormat.YAML);
        assertNotNull(node);
        assertEquals("v", node.get("k").asText(), "YAML parser should correctly read JSON content");
    }

    /* --------------------------------------------------------------------- *
     *  Group: input stream read error (ObjectMapper path)
     * --------------------------------------------------------------------- */
    @Test
    void readTree_inputStreamReadError_propagatesIOException() {
        InputStream badStream = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("forced read error");
            }
        };
        assertThrows(IOException.class, () -> factory.readTree(badStream, InputFormat.JSON), "IOException from underlying stream must propagate");
    }

    /* --------------------------------------------------------------------- *
     *  Group: jsonNodeReader provided – null inputFormat handling
     * --------------------------------------------------------------------- */
    @Test
    void readTree_withReader_nullInputFormat_propagatesException() throws Exception {
        JsonNodeReader mockReader = mock(JsonNodeReader.class);
        when(mockReader.readTree(any(InputStream.class), isNull())).thenThrow(new NullPointerException("null format"));
        injectJsonNodeReader(factory, mockReader);
        InputStream is = new ByteArrayInputStream("{\"sample\":123}".getBytes(StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> factory.readTree(is, null), "Null InputFormat should cause JsonNodeReader to throw NPE");
    }

    /* --------------------------------------------------------------------- *
     *  Group: jsonNodeReader provided – input stream read error propagation
     * --------------------------------------------------------------------- */
    @Test
    void readTree_withReader_streamReadError_propagatesIOException() throws Exception {
        JsonNodeReader mockReader = mock(JsonNodeReader.class);
        when(mockReader.readTree(any(InputStream.class), any())).thenThrow(new IOException("stream failure"));
        injectJsonNodeReader(factory, mockReader);
        InputStream badStream = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("forced read error");
            }
        };
        assertThrows(IOException.class, () -> factory.readTree(badStream, InputFormat.JSON), "IOException from JsonNodeReader should propagate");
    }
}
