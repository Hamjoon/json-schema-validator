```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#readTree(String, InputFormat)}.
 *
 * <p>The method has two distinct execution paths:
 *
 * <ul>
 *   <li>If the factory was created without a {@link JsonNodeReader} (the default
 *       case), the call is delegated to the {@link ObjectMapper} obtained from
 *       {@code getObjectMapper(inputFormat)}.
 *   <li>If a {@link JsonNodeReader} is present, the call is delegated to that
 *       reader.
 * </ul>
 *
 * <p>Both paths are exercised using a real {@link JsonSchemaFactory}
 * instance (created via {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag)})
 * and Mockito spies/stubs.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_readTree_Test {

    /** Mocked dependencies used across the test methods. */
    @Mock
    private JsonNodeReader mockReader;

    @Mock
    private ObjectMapper mockMapper;

    @Mock
    private JsonNode mockNode;

    @Mock
    private InputFormat mockInputFormat;

    /** The factory created without a {@link JsonNodeReader}. */
    private JsonSchemaFactory factoryWithoutReader;

    /** The factory created with a {@link JsonNodeReader}. */
    private JsonSchemaFactory factoryWithReader;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // --------------------------------------------------------------------
        // 1️⃣  Factory without a JsonNodeReader (default configuration)
        // --------------------------------------------------------------------
        factoryWithoutReader = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012); // any supported version works

        // --------------------------------------------------------------------
        // 2️⃣  Factory with a JsonNodeReader supplied via the builder customiser
        // --------------------------------------------------------------------
        factoryWithReader = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.jsonNodeReader(mockReader));
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * When the factory has **no** {@link JsonNodeReader}, {@code readTree}
     * must delegate to the appropriate {@link ObjectMapper}.
     */
    @Test
    void readTree_delegatesToObjectMapper_whenReaderIsNull() throws IOException {
        // Arrange ------------------------------------------------------------
        // Spy the factory so we can stub {@code getObjectMapper} without
        // touching its private state.
        JsonSchemaFactory spyFactory = Mockito.spy(factoryWithoutReader);

        // Stub the mapper returned for the supplied InputFormat.
        doReturn(mockMapper).when(spyFactory).getObjectMapper(mockInputFormat);
        // Stub the mapper's readTree method.
        when(mockMapper.readTree("some content")).thenReturn(mockNode);

        // Act ---------------------------------------------------------------
        JsonNode result = spyFactory.readTree("some content", mockInputFormat);

        // Assert -------------------------------------------------------------
        assertSame(mockNode, result, "The JsonNode returned by the ObjectMapper should be propagated.");
        verify(mockMapper, times(1)).readTree("some content");
        verifyNoInteractions(mockReader);
        // Ensure the factory never tried to obtain a JsonNodeReader‑based path.
        verify(spyFactory, never()).jsonNodeReader;
    }

    /**
     * When the factory has a {@link JsonNodeReader}, {@code readTree}
     * must delegate to that reader and must **not** call the ObjectMapper.
     */
    @Test
    void readTree_delegatesToJsonNodeReader_whenReaderIsPresent() throws IOException {
        // Arrange ------------------------------------------------------------
        // Spy the factory that already contains the mocked JsonNodeReader.
        JsonSchemaFactory spyFactory = Mockito.spy(factoryWithReader);

        // Stub the JsonNodeReader to return the mock JsonNode.
        when(mockReader.readTree("another content", mockInputFormat)).thenReturn(mockNode);

        // Act ---------------------------------------------------------------
        JsonNode result = spyFactory.readTree("another content", mockInputFormat);

        // Assert -------------------------------------------------------------
        assertSame(mockNode, result, "The JsonNode returned by the JsonNodeReader should be propagated.");
        verify(mockReader, times(1)).readTree("another content", mockInputFormat);
        // The ObjectMapper path must not be touched.
        verify(spyFactory, never()).getObjectMapper(any());
    }
}
```