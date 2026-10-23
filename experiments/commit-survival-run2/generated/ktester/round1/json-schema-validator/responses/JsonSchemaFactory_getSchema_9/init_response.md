```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_9_Test {

    /*** Mocks for all collaborators ***/
    @Mock
    private ObjectMapper jsonMapper;
    @Mock
    private YAMLMapper yamlMapper;
    @Mock
    private URISchemeFactory uriFactory;
    @Mock
    private URISchemeFetcher uriFetcher;
    @Mock
    private URNFactory urnFactory;
    @Mock
    private CompositeURITranslator uriTranslators;
    @Mock
    private URIFetcher dummyFetcher; // needed to fill the map for URISchemeFetcher

    /*** System under test ***/
    private JsonSchemaFactory factory;

    /*** Helper objects ***/
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Initialise the mocks (handled by MockitoExtension) and create a real instance
        // of JsonSchemaFactory via reflection because its constructor is package‑private.
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        // The factory expects the default meta‑schema to be present in the map.
        JsonMetaSchema dummyMeta = mock(JsonMetaSchema.class);
        when(dummyMeta.getUri()).thenReturn(DEFAULT_META_SCHEMA_URI);
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMeta);

        Map<String, String> uriMap = Collections.emptyMap();

        // Build the JsonSchemaFactory instance using reflection.
        java.lang.reflect.Constructor<JsonSchemaFactory> ctor =
                JsonSchemaFactory.class.getDeclaredConstructor(
                        ObjectMapper.class,
                        YAMLMapper.class,
                        String.class,
                        URISchemeFactory.class,
                        URISchemeFetcher.class,
                        URNFactory.class,
                        Map.class,
                        Map.class,
                        boolean.class,
                        CompositeURITranslator.class);
        ctor.setAccessible(true);
        factory = ctor.newInstance(
                jsonMapper,
                yamlMapper,
                DEFAULT_META_SCHEMA_URI,
                uriFactory,
                uriFetcher,
                urnFactory,
                metaSchemas,
                uriMap,
                false,                 // enableUriSchemaCache
                uriTranslators);
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
     * Verify that {@code getSchema(URI, SchemaValidatorsConfig)} converts an {@link IllegalArgumentException}
     * thrown by {@link URISchemeFactory#create(URI)} into a {@link JsonSchemaException}.
     */
    @Test
    void getSchema_throwsJsonSchemaException_whenUriFactoryCreateFails() throws Exception {
        // Arrange
        URI bogusUri = new URI("http://example.com/bogus-schema.json");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // Simulate the factory throwing an IllegalArgumentException when translating the URI.
        when(uriFactory.create(anyString())).thenThrow(new IllegalArgumentException("bad URI"));

        // Act & Assert
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(bogusUri, config));

        // The cause of the JsonSchemaException should be the original IllegalArgumentException.
        assertTrue(thrown.getCause() instanceof IllegalArgumentException);
        assertEquals("bad URI", thrown.getCause().getMessage());

        // Verify that the logger was invoked (optional – we only verify interaction with the mock).
        verify(uriFactory, times(1)).create(anyString());
    }

    /**
     * Verify that when the URI cache is disabled and the schema is not cached,
     * the method attempts to fetch the schema via {@link URISchemeFetcher}.
     * This test does not go through the full parsing pipeline – it only checks the
     * interaction with the fetcher.
     */
    @Test
    void getSchema_callsFetcher_whenCacheDisabled() throws Exception {
        // Arrange
        URI schemaUri = new URI("http://example.com/schema.json");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // Mock the URI translation and creation to return the same URI.
        when(uriFactory.create(anyString())).thenAnswer(invocation -> new URI((String) invocation.getArgument(0)));

        // Provide a dummy InputStream that represents a minimal JSON schema.
        InputStream dummyStream = new java.io.ByteArrayInputStream("{\"type\":\"object\"}".getBytes());
        when(uriFetcher.fetch(any(URI.class))).thenReturn(dummyStream);

        // Mock ObjectMapper to return a dummy JsonNode.
        JsonNode dummyNode = mock(JsonNode.class);
        when(jsonMapper.readTree(dummyStream)).thenReturn(dummyNode);

        // Mock the private helper methods indirectly by stubbing the public behaviour they affect.
        // The factory will call findMetaSchemaForSchema – we let it return the dummy meta‑schema we
        // placed in the constructor map (already satisfied).
        // The idMatchesSourceUri check will return false by default (no $id in our dummy schema).

        // Act
        JsonSchema result = factory.getSchema(schemaUri, config);

        // Assert
        assertNotNull(result, "Resulting JsonSchema should not be null");
        verify(uriFetcher, times(1)).fetch(any(URI.class));
        verify(jsonMapper, times(1)).readTree(any(InputStream.class));
    }
}
```