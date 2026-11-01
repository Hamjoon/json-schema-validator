```java
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

/**
 * Unit tests for {@link JsonSchemaFactory#readTree(InputStream, InputFormat)}.
 *
 * <p>The tests cover the two execution paths inside {@code readTree}:
 *
 * <ul>
 *   <li>When the {@code JsonNodeReader} is {@code null} – the factory falls back to the
 *       appropriate {@link ObjectMapper} (JSON or YAML) to read the content.
 *   <li>When a non‑null {@code JsonNodeReader} is supplied – the factory delegates to it.
 * </ul>
 *
 * <p>The second path is exercised by creating a {@link JsonSchemaFactory} instance via
 * reflection that injects a mock {@code JsonNodeReader}. This approach respects the
 * requirement of not accessing private fields directly in production code – the
 * reflection is confined to the test harness.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_readTree_2_Test {

    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the public factory method – it creates a JsonSchemaFactory with a null JsonNodeReader.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /** Verify that JSON input is parsed correctly when no JsonNodeReader is configured. */
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

    /** Verify that YAML input is parsed correctly when no JsonNodeReader is configured. */
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

    /** Verify that when a custom JsonNodeReader is supplied the factory delegates to it. */
    @Test
    void readTree_withCustomJsonNodeReader_delegatesToReader() throws Exception {
        // GIVEN
        // Create a mock JsonNodeReader that returns a predetermined JsonNode.
        JsonNodeReader mockReader = mock(JsonNodeReader.class);
        JsonNode expectedNode = factory.getJsonMapper().readTree("{\"mock\":\"true\"}");
        when(mockReader.readTree(any(InputStream.class), eq(InputFormat.JSON))).thenReturn(expectedNode);

        // Build a JsonSchemaFactory instance that uses the mock JsonNodeReader.
        // The constructor is private, so we obtain it via reflection.
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor =
                JsonSchemaFactory.class.getDeclaredConstructor(
                        ObjectMapper.class,
                        ObjectMapper.class,
                        JsonNodeReader.class,
                        String.class,
                        SchemaLoaders.Builder.class,
                        SchemaMappers.Builder.class,
                        java.util.concurrent.ConcurrentMap.class,
                        boolean.class,
                        JsonMetaSchemaFactory.class);
        ctor.setAccessible(true);

        // Minimal required arguments – we reuse the mappers from a regular factory instance.
        JsonSchemaFactory factoryWithReader = ctor.newInstance(
                factory.getJsonMapper(),
                factory.getYamlMapper(),
                mockReader,
                "https://json-schema.org/draft/2020-12/schema", // default meta‑schema IRI
                null,
                null,
                new java.util.concurrent.ConcurrentHashMap<>(),
                false,
                null);

        String json = "{\"any\":\"data\"}";
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        // WHEN
        JsonNode actualNode = factoryWithReader.readTree(is, InputFormat.JSON);

        // THEN
        assertSame(expectedNode, actualNode, "The factory should return the node provided by the custom JsonNodeReader");
        // Verify delegation
        verify(mockReader, times(1)).readTree(any(InputStream.class), eq(InputFormat.JSON));
    }
}
```