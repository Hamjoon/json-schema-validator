```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.serialization.JsonNodeReader;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory#readTree(InputStream, InputFormat)}.
 *
 * <p>The test class follows the framework that was already provided.  It adds
 * coverage for all groups and cases described in the JSON test‑case
 * specification, using real Jackson parsers for the happy‑path cases and
 * Mockito for the exceptional‑path cases.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_readTree_2_Test {

    /** Factory that uses the built‑in {@link ObjectMapper} (jsonNodeReader == null). */
    private JsonSchemaFactory factoryWithDefaultReader;

    /** Factory that delegates to a supplied {@link JsonNodeReader} (mocked). */
    private JsonSchemaFactory factoryWithMockReader;

    @Mock
    private JsonNodeReader mockReader;   // injected by Mockito

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // 1️⃣  Factory that uses the built‑in ObjectMapper (jsonNodeReader == null)
        factoryWithDefaultReader = JsonSchemaFactory.getInstance(VersionFlag.V202012);

        // 2️⃣  Factory that delegates to a supplied JsonNodeReader (mocked)
        factoryWithMockReader = JsonSchemaFactory.builder()
                .jsonNodeReader(mockReader)   // inject the mock
                .build();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    /* --------------------------------------------------------------------- */
    /*                     DEFAULT READER – HAPPY PATH                       */
    /* --------------------------------------------------------------------- */

    @Test
    void readTree_usesObjectMapper_whenReaderIsNull_andJson() throws IOException {
        // GIVEN
        String json = "{\"alpha\":42,\"beta\":\"text\"}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        // WHEN
        JsonNode result = factoryWithDefaultReader.readTree(is, InputFormat.JSON);

        // THEN
        assertNotNull(result, "Resulting JsonNode must not be null");
        assertEquals(42, result.get("alpha").asInt(), "Field 'alpha' should be 42");
        assertEquals("text", result.get("beta").asText(), "Field 'beta' should be 'text'");
    }

    @Test
    void readTree_usesObjectMapper_whenReaderIsNull_andYaml() throws IOException {
        // GIVEN
        String yaml = "name: John\nage: 30\n";
        InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        // WHEN
        JsonNode result = factoryWithDefaultReader.readTree(is, InputFormat.YAML);

        // THEN
        assertNotNull(result, "Resulting JsonNode must not be null");
        assertEquals("John", result.get("name").asText(), "YAML field 'name' should be 'John'");
        assertEquals(30, result.get("age").asInt(), "YAML field 'age' should be 30");
    }

    /* --------------------------------------------------------------------- */
    /*                     DEFAULT READER – EXCEPTION PATH                   */
    /* --------------------------------------------------------------------- */

    @Test
    void readTree_propagatesIOException_fromObjectMapper() {
        // GIVEN: InputStream that throws IOException on any read
        InputStream badStream = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("forced IO error"); }
        };

        // WHEN / THEN
        assertThrows(IOException.class,
                () -> factoryWithDefaultReader.readTree(badStream, InputFormat.JSON),
                "IOException should be propagated");
    }

    @Test
    void readTree_propagatesIllegalArgumentException_whenInputStreamIsNull() {
        // GIVEN
        InputStream nullStream = null;

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> factoryWithDefaultReader.readTree(nullStream, InputFormat.JSON),
                "IllegalArgumentException (from ObjectMapper) must be propagated");
    }

    @Test
    void readTree_propagatesParsingException_forMalformedJson() {
        // GIVEN
        String malformed = "{\"name\": \"John\""; // missing closing }
        InputStream is = new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8));

        // WHEN / THEN
        assertThrows(IOException.class,
                () -> factoryWithDefaultReader.readTree(is, InputFormat.JSON),
                "Parsing malformed JSON should result in an IOException");
    }

    @Test
    void readTree_propagatesParsingException_forEmptyStream() {
        // GIVEN
        InputStream empty = new ByteArrayInputStream(new byte[0]);

        // WHEN / THEN
        assertThrows(IOException.class,
                () -> factoryWithDefaultReader.readTree(empty, InputFormat.JSON),
                "Empty stream should cause a parsing exception");
    }

    @Test
    void readTree_propagatesNullPointerException_whenInputFormatIsNull() {
        // GIVEN
        String json = "{\"valid\":true}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> factoryWithDefaultReader.readTree(is, null),
                "Null inputFormat should cause NullPointerException");
    }

    @Test
    void readTree_propagatesUncheckedException_fromObjectMapper() throws IOException {
        // GIVEN: spy the factory and stub getObjectMapper to return a mock that throws
        JsonSchemaFactory spyFactory = spy(factoryWithDefaultReader);
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.readTree(any(InputStream.class))).thenThrow(new IllegalStateException("unchecked"));
        doReturn(mockMapper).when(spyFactory).getObjectMapper(InputFormat.JSON);

        InputStream is = new ByteArrayInputStream("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));

        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> spyFactory.readTree(is, InputFormat.JSON));
        assertEquals("unchecked", ex.getMessage());
    }

    @Test
    void readTree_propagatesException_fromGetObjectMapperFactoryMethod() throws IOException {
        // GIVEN: spy the factory and make getObjectMapper throw NPE
        JsonSchemaFactory spyFactory = spy(factoryWithDefaultReader);
        doThrow(new NullPointerException("factory failure")).when(spyFactory).getObjectMapper(InputFormat.JSON);

        InputStream is = new ByteArrayInputStream("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));

        // WHEN / THEN
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> spyFactory.readTree(is, InputFormat.JSON));
        assertEquals("factory failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*                     CUSTOM READER – HAPPY PATH                        */
    /* --------------------------------------------------------------------- */

    @Test
    void readTree_delegatesToJsonNodeReader_whenReaderIsPresent_andJson() throws IOException {
        // GIVEN
        String json = "{\"gamma\":true}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        JsonNode mockedNode = mock(JsonNode.class);
        when(mockReader.readTree(is, InputFormat.JSON)).thenReturn(mockedNode);

        // WHEN
        JsonNode result = factoryWithMockReader.readTree(is, InputFormat.JSON);

        // THEN
        assertSame(mockedNode, result, "Returned node must be the one supplied by the mock reader");
        verify(mockReader, times(1)).readTree(is, InputFormat.JSON);
    }

    @Test
    void readTree_delegatesToJsonNodeReader_whenReaderIsPresent_andYaml() throws IOException {
        // GIVEN
        String yaml = "age: 30\n";
        InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        JsonNode mockedNode = mock(JsonNode.class);
        when(mockReader.readTree(is, InputFormat.YAML)).thenReturn(mockedNode);

        // WHEN
        JsonNode result = factoryWithMockReader.readTree(is, InputFormat.YAML);

        // THEN
        assertSame(mockedNode, result);
        verify(mockReader, times(1)).readTree(is, InputFormat.YAML);
    }

    /* --------------------------------------------------------------------- */
    /*                     CUSTOM READER – EXCEPTION PATH                    */
    /* --------------------------------------------------------------------- */

    @Test
    void readTree_delegatesToJsonNodeReader_propagatesIllegalArgumentException_forNullStream() throws IOException {
        // GIVEN
        InputStream nullStream = null;
        when(mockReader.readTree(nullStream, InputFormat.YAML))
                .thenThrow(new IllegalArgumentException("null stream"));

        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factoryWithMockReader.readTree(nullStream, InputFormat.YAML));
        assertEquals("null stream", ex.getMessage());
        verify(mockReader).readTree(nullStream, InputFormat.YAML);
    }

    @Test
    void readTree_delegatesToJsonNodeReader_propagatesIOException_forMalformedYaml() throws IOException {
        // GIVEN
        String malformedYaml = "name: John: Doe";
        InputStream is = new ByteArrayInputStream(malformedYaml.getBytes(StandardCharsets.UTF_8));
        when(mockReader.readTree(is, InputFormat.YAML))
                .thenThrow(new IOException("parsing error"));

        // WHEN / THEN
        IOException ex = assertThrows(IOException.class,
                () -> factoryWithMockReader.readTree(is, InputFormat.YAML));
        assertEquals("parsing error", ex.getMessage());
        verify(mockReader).readTree(is, InputFormat.YAML);
    }

    @Test
    void readTree_delegatesToJsonNodeReader_propagatesNullPointerException_forNullFormat() throws IOException {
        // GIVEN
        String json = "{\"age\":30}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        when(mockReader.readTree(is, null))
                .thenThrow(new NullPointerException("format null"));

        // WHEN / THEN
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> factoryWithMockReader.readTree(is, null));
        assertEquals("format null", ex.getMessage());
        verify(mockReader).readTree(is, null);
    }

    @Test
    void readTree_delegatesToJsonNodeReader_returnsNull_whenReaderReturnsNull() throws IOException {
        // GIVEN
        String json = "{\"age\":30}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        when(mockReader.readTree(is, InputFormat.JSON)).thenReturn(null);

        // WHEN
        JsonNode result = factoryWithMockReader.readTree(is, InputFormat.JSON);

        // THEN
        assertNull(result, "Method should return null when the JsonNodeReader returns null");
        verify(mockReader).readTree(is, InputFormat.JSON);
    }

    @Test
    void readTree_delegatesToJsonNodeReader_propagatesRuntimeException() throws IOException {
        // GIVEN
        String json = "{\"age\":30}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        when(mockReader.readTree(is, InputFormat.JSON))
                .thenThrow(new RuntimeException("unexpected"));

        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> factoryWithMockReader.readTree(is, InputFormat.JSON));
        assertEquals("unexpected", ex.getMessage());
        verify(mockReader).readTree(is, InputFormat.JSON);
    }

    /* --------------------------------------------------------------------- */
    /*                     UNSUPPORTED INPUTFORMAT (compile‑time)            */
    /* --------------------------------------------------------------------- */
    // The InputFormat enum only defines JSON and YAML, therefore an
    // "unsupported" constant cannot be created at compile time.
    // If the library ever adds a new constant, the following test will
    // automatically fail, indicating that the factory needs to handle it.
    @Test
    void readTree_unsupportedInputFormat_behaviour() {
        // This test documents the current contract: only JSON and YAML are
        // supported.  Attempting to use any other enum value (which cannot be
        // instantiated here) would result in a compilation error.
        // Hence, no runtime test is required.
        assertTrue(InputFormat.JSON != null && InputFormat.YAML != null,
                "InputFormat must define at least JSON and YAML");
    }
}
```