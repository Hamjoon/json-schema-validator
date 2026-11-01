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
    @Mock
    ObjectMapper mockObjectMapper;

    @Mock
    YAMLMapper mockYamlMapper;

    @Mock
    URISchemeFactory mockUriFactory;

    @Mock
    URISchemeFetcher mockUriFetcher;

    @Mock
    URNFactory mockUrnFactory;

    @Mock
    CompositeURITranslator mockCompositeTranslator;

    // Instance under test (created via reflection)
    JsonSchemaFactory factory;

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
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        factory = ctor.newInstance(mockObjectMapper, mockYamlMapper, defaultMetaSchemaURI, mockUriFactory, mockUriFetcher, mockUrnFactory, metaSchemas, uriMap, false, mockCompositeTranslator);
        // Spy the factory so we can stub the protected newJsonSchema method
        factory = spy(factory);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid interference between tests
        reset(mockObjectMapper, mockYamlMapper, mockUriFactory, mockUriFetcher, mockUrnFactory, mockCompositeTranslator);
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
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(json, null), "IOException should be wrapped in JsonSchemaException");
        assertEquals(ioException, thrown.getCause(), "Original IOException should be the cause of the thrown JsonSchemaException");
        verify(mockObjectMapper, times(1)).readTree(json);
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaNullSchemaString_IllegalArgumentException() throws Exception {
        // Arrange
        String schema = null;
        IllegalArgumentException illegalArg = new IllegalArgumentException("Schema is null");
        when(mockObjectMapper.readTree(schema)).thenThrow(illegalArg);
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schema, new SchemaValidatorsConfig()), "Passing null should propagate IllegalArgumentException");
        assertSame(illegalArg, thrown, "The same exception instance should be propagated");
        verify(mockObjectMapper, times(1)).readTree(schema);
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaEmptyString_JsonSchemaException() throws Exception {
        // Arrange
        String schema = "";
        IOException io = new IOException("Empty content");
        when(mockObjectMapper.readTree(schema)).thenThrow(io);
        // Act & Assert
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schema, new SchemaValidatorsConfig()), "Empty string should be wrapped in JsonSchemaException");
        assertEquals(io, thrown.getCause());
        verify(mockObjectMapper).readTree(schema);
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaMalformedJson_JsonSchemaException() throws Exception {
        // Arrange
        String schema = "{type:object,}";
        IOException parseException = new IOException("Malformed JSON");
        when(mockObjectMapper.readTree(schema)).thenThrow(parseException);
        // Act & Assert
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schema, new SchemaValidatorsConfig()), "Malformed JSON should be wrapped in JsonSchemaException");
        assertEquals(parseException, thrown.getCause());
        verify(mockObjectMapper).readTree(schema);
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaNullConfigReturnsSchema() throws Exception {
        // Arrange
        String json = "{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}";
        JsonNode mockNode = mock(JsonNode.class);
        JsonSchema mockSchema = mock(JsonSchema.class);
        // config is null
        when(mockObjectMapper.readTree(json)).thenReturn(mockNode);
        doReturn(mockSchema).when(factory).newJsonSchema(null, mockNode, null);
        // Act
        JsonSchema result = factory.getSchema(json, null);
        // Assert
        assertSame(mockSchema, result);
        verify(mockObjectMapper).readTree(json);
        verify(factory).newJsonSchema(null, mockNode, null);
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaComplexValidSchema() throws Exception {
        // Arrange
        String complexSchema = "{\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"id\": {\"type\": \"integer\"},\n" + "    \"name\": {\"type\": \"string\"},\n" + "    \"tags\": {\"type\": \"array\", \"items\": {\"type\": \"string\"}},\n" + "    \"address\": {\n" + "      \"type\": \"object\",\n" + "      \"properties\": {\n" + "        \"street\": {\"type\": \"string\"},\n" + "        \"zip\": {\"type\": \"string\"}\n" + "      },\n" + "      \"required\": [\"street\"]\n" + "    }\n" + "  },\n" + "  \"required\": [\"id\", \"name\"]\n" + "}";
        JsonNode mockNode = mock(JsonNode.class);
        JsonSchema mockSchema = mock(JsonSchema.class);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        when(mockObjectMapper.readTree(complexSchema)).thenReturn(mockNode);
        doReturn(mockSchema).when(factory).newJsonSchema(null, mockNode, cfg);
        // Act
        JsonSchema result = factory.getSchema(complexSchema, cfg);
        // Assert
        assertSame(mockSchema, result);
        verify(mockObjectMapper).readTree(complexSchema);
        verify(factory).newJsonSchema(null, mockNode, cfg);
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaWithInternalRef() throws Exception {
        // Arrange
        String schemaWithRef = "{\n" + "  \"$id\": \"http://example.com/root.json\",\n" + "  \"definitions\": {\n" + "    \"positiveInteger\": {\"type\": \"integer\", \"minimum\": 0}\n" + "  },\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"count\": {\"$ref\": \"#/definitions/positiveInteger\"}\n" + "  }\n" + "}";
        JsonNode mockNode = mock(JsonNode.class);
        JsonSchema mockSchema = mock(JsonSchema.class);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        when(mockObjectMapper.readTree(schemaWithRef)).thenReturn(mockNode);
        doReturn(mockSchema).when(factory).newJsonSchema(null, mockNode, cfg);
        // Act
        JsonSchema result = factory.getSchema(schemaWithRef, cfg);
        // Assert
        assertSame(mockSchema, result);
        verify(mockObjectMapper).readTree(schemaWithRef);
        verify(factory).newJsonSchema(null, mockNode, cfg);
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaNullJsonMapper_NullPointerException() throws Exception {
        // Build a factory where the jsonMapper dependency is null
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema#";
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(defaultMetaSchemaURI, mock(JsonMetaSchema.class));
        Map<String, String> uriMap = Collections.emptyMap();
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        JsonSchemaFactory factoryWithNullMapper = // <-- null ObjectMapper
        ctor.// <-- null ObjectMapper
        newInstance(// <-- null ObjectMapper
        null, mockYamlMapper, defaultMetaSchemaURI, mockUriFactory, mockUriFetcher, mockUrnFactory, metaSchemas, uriMap, false, mockCompositeTranslator);
        // Act & Assert
        NullPointerException npe = assertThrows(NullPointerException.class, () -> factoryWithNullMapper.getSchema("{\"type\":\"object\"}", new SchemaValidatorsConfig()), "Calling getSchema with a factory that has a null ObjectMapper should throw NPE");
        // Optionally verify the message, but it's not guaranteed by the implementation
        assertNotNull(npe);
    }
}
