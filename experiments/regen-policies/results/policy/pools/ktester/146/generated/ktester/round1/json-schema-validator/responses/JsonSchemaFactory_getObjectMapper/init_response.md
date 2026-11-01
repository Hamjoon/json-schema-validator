```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getObjectMapper(InputFormat)}.
 *
 * The tests verify that the correct {@link ObjectMapper} is returned for
 * supported {@link InputFormat}s and that an {@link IllegalArgumentException}
 * is thrown for unsupported formats.
 *
 * The test class resides in the same package as {@link JsonSchemaFactory},
 * therefore it can access the package‑private {@code getObjectMapper},
 * {@code getJsonMapper} and {@code getYamlMapper} methods.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getObjectMapper_Test {

    /** Factory instance used for all tests. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a standard factory instance – the builder creates a fully‑initialised object.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        factory = null;
    }

    @Test
    void getObjectMapper_returnsJsonMapper_forJsonInputFormat() throws IOException {
        // GIVEN
        InputFormat format = InputFormat.JSON;

        // WHEN
        ObjectMapper mapper = factory.getObjectMapper(format);

        // THEN
        assertNotNull(mapper, "Mapper should not be null for JSON format");
        // The mapper returned by getObjectMapper should be the same instance as the
        // factory's internal JSON mapper.
        assertSame(factory.getJsonMapper(), mapper,
                "Factory should return its internal JSON ObjectMapper");

        // Additionally verify that the mapper can parse a JSON string.
        String json = "{\"name\":\"John\",\"age\":30}";
        JsonNode node = mapper.readTree(json);
        assertEquals("John", node.get("name").asText());
        assertEquals(30, node.get("age").asInt());
    }

    @Test
    void getObjectMapper_returnsYamlMapper_forYamlInputFormat() throws IOException {
        // GIVEN
        InputFormat format = InputFormat.YAML;

        // WHEN
        ObjectMapper mapper = factory.getObjectMapper(format);

        // THEN
        assertNotNull(mapper, "Mapper should not be null for YAML format");
        // The mapper returned by getObjectMapper should be the same instance as the
        // factory's internal YAML mapper.
        assertSame(factory.getYamlMapper(), mapper,
                "Factory should return its internal YAML ObjectMapper");

        // Verify that the mapper can parse a simple YAML document.
        String yaml = "name: Jane\nage: 25";
        JsonNode node = mapper.readTree(yaml);
        assertEquals("Jane", node.get("name").asText());
        assertEquals(25, node.get("age").asInt());
    }

    @Test
    void getObjectMapper_throwsException_forUnsupportedInputFormat() {
        // GIVEN
        // An input format that is not JSON nor YAML – we use null to simulate an
        // unsupported value (the method checks equality against the two constants).
        InputFormat unsupported = null;

        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getObjectMapper(unsupported),
                "Expected IllegalArgumentException for null/unsupported InputFormat");
        assertTrue(ex.getMessage().contains("Unsupported input format"),
                "Exception message should mention unsupported format");
    }
}
```