```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_readTree_2_Test {

    /** Factory whose {@code jsonNodeReader} is **null** – the default behaviour. */
    private JsonSchemaFactory factoryWithDefaultReader;

    /** Factory whose {@code jsonNodeReader} is a Mockito mock – we can verify delegation. */
    private JsonSchemaFactory factoryWithMockReader;

    @Mock
    private JsonNodeReader mockReader;   // injected by Mockito

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // 1️⃣  Factory that uses the built‑in ObjectMapper (jsonNodeReader == null)
        factoryWithDefaultReader = JsonSchemaFactory.getInstance(VersionFlag.V202012);

        // 2️⃣  Factory that delegates to a supplied JsonNodeReader (mocked)
        // The builder API of JsonSchemaFactory allows us to plug in a custom JsonNodeReader.
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

    /** 
     * When {@code jsonNodeReader} is {@code null} the method should read the
     * content via the appropriate {@link ObjectMapper}.
     */
    @Test
    void readTree_usesObjectMapper_whenReaderIsNull() throws IOException {
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

    /**
     * When a custom {@link JsonNodeReader} is supplied the method must delegate
     * to it and return whatever node the reader provides.
     */
    @Test
    void readTree_delegatesToJsonNodeReader_whenReaderIsPresent() throws IOException {
        // GIVEN
        String json = "{\"gamma\":true}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        JsonNode mockedNode = mock(JsonNode.class);
        when(mockReader.readTree(is, InputFormat.JSON)).thenReturn(mockedNode);

        // WHEN
        JsonNode result = factoryWithMockReader.readTree(is, InputFormat.JSON);

        // THEN
        assertSame(mockedNode, result, "The returned node should be the one supplied by the mock reader");
        verify(mockReader, times(1)).readTree(is, InputFormat.JSON);
    }
}
```