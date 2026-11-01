```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#readTree(String, InputFormat)}.
 *
 * <p>Two scenarios are covered:
 *
 * <ul>
 *   <li>When the factory has no {@link JsonNodeReader} (the default configuration). In this
 *       case the method should delegate to the appropriate {@link ObjectMapper} obtained via
 *       {@code getObjectMapper(inputFormat)}.
 *   <li>When a custom {@link JsonNodeReader} is supplied. The method must delegate to the
 *       reader and return its result.
 * </ul>
 *
 * The tests use the public {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag)} factory
 * method and, for the second scenario, inject a mock {@code JsonNodeReader} via reflection
 * (the field is package‑private and cannot be set through the public API).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_readTree_Test {

    private JsonSchemaFactory factory;

    @Mock
    private JsonNodeReader mockReader; // injected only for the custom‑reader test

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory with the default configuration (jsonNodeReader == null)
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verify that, when no {@link JsonNodeReader} is configured, {@code readTree}
     * uses the internal {@link ObjectMapper} for the supplied {@link InputFormat}.
     */
    @Test
    void readTree_usesObjectMapperWhenReaderIsNull() throws IOException {
        // GIVEN
        String json = "{\"name\":\"John\",\"age\":30}";
        InputFormat format = InputFormat.JSON; // typical enum value

        // WHEN
        JsonNode result = factory.readTree(json, format);

        // THEN
        JsonNode expected = OBJECT_MAPPER.readTree(json);
        assertEquals(expected, result, "The JsonNode produced by readTree should match ObjectMapper output");
    }

    /**
     * Verify that, when a custom {@link JsonNodeReader} is present, {@code readTree}
     * delegates to it and returns the node supplied by the reader.
     */
    @Test
    void readTree_delegatesToCustomJsonNodeReader() throws Exception {
        // GIVEN
        String yaml = "name: Jane\nage: 25";
        InputFormat format = InputFormat.YAML; // typical enum value

        // Prepare a JsonNode that the mock reader will return.
        JsonNode mockedNode = OBJECT_MAPPER.readTree("{\"name\":\"Jane\",\"age\":25}");
        when(mockReader.readTree(yaml, format)).thenReturn(mockedNode);

        // Inject the mock into the factory (the field is package‑private).
        Field readerField = JsonSchemaFactory.class.getDeclaredField("jsonNodeReader");
        readerField.setAccessible(true);
        readerField.set(factory, mockReader);

        // WHEN
        JsonNode result = factory.readTree(yaml, format);

        // THEN
        assertSame(mockedNode, result, "readTree should return the node supplied by the custom JsonNodeReader");
        verify(mockReader, times(1)).readTree(yaml, format);
    }
}
```