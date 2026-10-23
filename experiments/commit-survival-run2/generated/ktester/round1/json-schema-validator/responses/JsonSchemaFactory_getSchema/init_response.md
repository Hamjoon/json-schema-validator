```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.CompositeURITranslator;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_Test {

    // Mocks for constructor dependencies
    @Mock ObjectMapper mockObjectMapper;
    @Mock YAMLMapper mockYamlMapper;
    @Mock URISchemeFactory mockUriFactory;
    @Mock URISchemeFetcher mockUriFetcher;
    @Mock URNFactory mockUrnFactory;
    @Mock CompositeURITranslator mockCompositeTranslator;

    // Instance under test (created via reflection)
    JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Prepare a minimal valid meta‑schema map required by the private constructor
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema#";
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        // The constructor uses normalizeMetaSchemaUri – for the default draft‑07 it returns the same value
        metaSchemas.put(defaultMetaSchemaURI, mock(JsonMetaSchema.class));

        // Empty URI map – allowed by the constructor
        Map<String, String> uriMap = Collections.emptyMap();

        // Use reflection to invoke the private constructor
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class
                .getDeclaredConstructor(ObjectMapper.class,
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
        factory = ctor.newInstance(mockObjectMapper,
                                   mockYamlMapper,
                                   defaultMetaSchemaURI,
                                   mockUriFactory,
                                   mockUriFetcher,
                                   mockUrnFactory,
                                   metaSchemas,
                                   uriMap,
                                   false,
                                   mockCompositeTranslator);

        // Spy the factory so we can stub the protected newJsonSchema method
        factory = spy(factory);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        reset(mockObjectMapper, mockYamlMapper, mockUriFactory,
              mockUriFetcher, mockUrnFactory, mockCompositeTranslator);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void testGetSchemaSuccess() throws Exception {
        // Arrange
        String json = "{\"type\":\"string\"}";
        JsonNode mockNode = mock(JsonNode.class);
        JsonSchema mockSchema = mock(JsonSchema.class);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // ObjectMapper.readTree should return the mocked JsonNode
        when(mockObjectMapper.readTree(json)).thenReturn(mockNode);
        // newJsonSchema should return the mocked JsonSchema
        doReturn(mockSchema).when(factory).newJsonSchema(null, mockNode, config);

        // Act
        JsonSchema result = factory.getSchema(json, config);

        // Assert
        assertSame(mockSchema, result, "Factory should return the JsonSchema produced by newJsonSchema");
        verify(mockObjectMapper, times(1)).readTree(json);
        verify(factory, times(1)).newJsonSchema(null, mockNode, config);
    }

    @Test
    void testGetSchemaIOExceptionIsWrapped() throws Exception {
        // Arrange
        String json = "invalid json";
        IOException ioException = new IOException("boom");
        when(mockObjectMapper.readTree(json)).thenThrow(ioException);

        // Act & Assert
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(json, null),
                "IOException should be wrapped in JsonSchemaException");
        assertEquals(ioException, thrown.getCause(),
                "Original IOException should be the cause of the thrown JsonSchemaException");
        verify(mockObjectMapper, times(1)).readTree(json);
    }
}
```