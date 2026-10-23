package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;

@DisplayName("JsonSchemaFactory.getSchema(String, …) tests")
class JsonSchemaFactory_getSchema_Test {

    //     @Mock
    ObjectMapper mockObjectMapper;

    //     @Mock
    YAMLMapper mockYamlMapper;

    //     @Mock
    URISchemeFactory mockUriFactory;

    //     @Mock
    URISchemeFetcher mockUriFetcher;

    //     @Mock
    URNFactory mockUrnFactory;

    //     @Mock
    CompositeURITranslator mockCompositeTranslator;

    // Instance under test (created via reflection)
    JsonSchemaFactory factory;

    private FakeObjectMapper fakeObjectMapper;

    // not used directly, but required by the constructor
    private YAMLMapper fakeYamlMapper;

    /**
     * Simple fake {@link ObjectMapper} that can be programmed to return a node
     * or throw a supplied exception when {@code readTree(String)} is called.
     */
    private static class FakeObjectMapper extends ObjectMapper {

        private final Map<String, JsonNode> nodes = new HashMap<>();

        private final Map<String, Throwable> exceptions = new HashMap<>();

        void whenReadTreeReturn(String json, JsonNode node) {
            nodes.put(json, node);
        }

        void whenReadTreeThrow(String json, Throwable ex) {
            exceptions.put(json, ex);
        }

        @Override
        public JsonNode readTree(String content) throws JsonProcessingException {
            if (exceptions.containsKey(content)) {
                Throwable t = exceptions.get(content);
                if (t instanceof RuntimeException) {
                    // IllegalArgumentException etc. – re‑throw as‑is
                    throw (RuntimeException) t;
                }
                // Checked exception – wrap into a JsonProcessingException so that the
                // factory sees an IOException‑type failure.
                throw new JsonProcessingException(t.getMessage(), t) {
                };
            }
            if (nodes.containsKey(content)) {
                return nodes.get(content);
            }
            try {
                return super.readTree(content);
            } catch (IOException e) {
                // Wrap any unexpected IOException
                throw new JsonProcessingException(e.getMessage(), e) {
                };
            }
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Minimal meta‑schema map required by the private constructor
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema#";
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        // Use the real default meta‑schema instance instead of a proxy
        metaSchemas.put(defaultMetaSchemaURI, JsonMetaSchema.getV7());
        // Empty URI map – acceptable for the constructor
        Map<String, String> uriMap = Collections.emptyMap();
        // Fake mappers
        fakeObjectMapper = new FakeObjectMapper();
        fakeYamlMapper = new YAMLMapper();
        // Create a dummy URISchemeFactory mock – the factory never calls its methods
        mockUriFactory = mock(URISchemeFactory.class);
        // Use reflection to invoke the private constructor
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        factory = ctor.newInstance(fakeObjectMapper, fakeYamlMapper, defaultMetaSchemaURI, // non‑null URISchemeFactory
        mockUriFactory, // URISchemeFetcher – not needed for these tests
        null, // URNFactory – not needed for these tests
        null, null, metaSchemas, uriMap, false, null);
    }

    @Test
    void testGetSchemaSuccess() throws Exception {
        // Arrange – a very simple valid JSON schema
        String json = "{\"type\":\"string\"}";
        JsonNode fakeNode = mock(JsonNode.class);
        fakeObjectMapper.whenReadTreeReturn(json, fakeNode);
        // Act
        JsonSchema result = factory.getSchema(json, new SchemaValidatorsConfig());
        // Assert
        assertNotNull(result, "Factory should return a JsonSchema for a valid schema string");
    }

    @Test
    void testGetSchemaIOExceptionIsWrapped() throws Exception {
        // Arrange
        String json = "invalid json";
        IOException ioException = new IOException("boom");
        fakeObjectMapper.whenReadTreeThrow(json, ioException);
        // Act & Assert
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(json, null), "IOException should be wrapped in JsonSchemaException");
        assertEquals(ioException, thrown.getCause(), "Original IOException should be the cause of the thrown JsonSchemaException");
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaNullSchemaString_IllegalArgumentException() throws Exception {
        // Arrange
        String schema = null;
        IllegalArgumentException illegalArg = new IllegalArgumentException("Schema is null");
        fakeObjectMapper.whenReadTreeThrow(schema, illegalArg);
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schema, new SchemaValidatorsConfig()), "Passing null should propagate IllegalArgumentException");
        assertSame(illegalArg, thrown, "The same exception instance should be propagated");
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaEmptyString_JsonSchemaException() throws Exception {
        // Arrange
        String schema = "";
        IOException io = new IOException("Empty content");
        fakeObjectMapper.whenReadTreeThrow(schema, io);
        // Act & Assert
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schema, new SchemaValidatorsConfig()), "Empty string should be wrapped in JsonSchemaException");
        assertEquals(io, thrown.getCause(), "The original IOException should be the cause");
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaMalformedJson_JsonSchemaException() throws Exception {
        // Arrange
        String schema = "{type:object,}";
        IOException parseException = new IOException("Malformed JSON");
        fakeObjectMapper.whenReadTreeThrow(schema, parseException);
        // Act & Assert
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schema, new SchemaValidatorsConfig()), "Malformed JSON should be wrapped in JsonSchemaException");
        assertEquals(parseException, thrown.getCause(), "The original IOException should be the cause");
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaNullConfigReturnsSchema() throws Exception {
        // Arrange
        String json = "{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}";
        JsonNode fakeNode = mock(JsonNode.class);
        fakeObjectMapper.whenReadTreeReturn(json, fakeNode);
        // Act
        JsonSchema result = factory.getSchema(json, null);
        // Assert
        assertNotNull(result, "Factory should still return a JsonSchema when config is null");
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaComplexValidSchema() throws Exception {
        // Arrange – a fairly large but still syntactically correct schema
        String complexSchema = "{\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"id\": {\"type\": \"integer\"},\n" + "    \"name\": {\"type\": \"string\"},\n" + "    \"tags\": {\"type\": \"array\", \"items\": {\"type\": \"string\"}},\n" + "    \"address\": {\n" + "      \"type\": \"object\",\n" + "      \"properties\": {\n" + "        \"street\": {\"type\": \"string\"},\n" + "        \"zip\": {\"type\": \"string\"}\n" + "      },\n" + "      \"required\": [\"street\"]\n" + "    }\n" + "  },\n" + "  \"required\": [\"id\", \"name\"]\n" + "}";
        JsonNode fakeNode = mock(JsonNode.class);
        fakeObjectMapper.whenReadTreeReturn(complexSchema, fakeNode);
        // Act
        JsonSchema result = factory.getSchema(complexSchema, new SchemaValidatorsConfig());
        // Assert
        assertNotNull(result, "Factory should parse a complex valid schema");
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaWithInternalRef() throws Exception {
        // Arrange
        String schemaWithRef = "{\n" + "  \"$id\": \"http://example.com/root.json\",\n" + "  \"definitions\": {\n" + "    \"positiveInteger\": {\"type\": \"integer\", \"minimum\": 0}\n" + "  },\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"count\": {\"$ref\": \"#/definitions/positiveInteger\"}\n" + "  }\n" + "}";
        JsonNode fakeNode = mock(JsonNode.class);
        fakeObjectMapper.whenReadTreeReturn(schemaWithRef, fakeNode);
        // Act
        JsonSchema result = factory.getSchema(schemaWithRef, new SchemaValidatorsConfig());
        // Assert
        assertNotNull(result, "Factory should handle schemas with internal $ref");
    }

    // -----------------------------------------------------------------
    @Test
    void testGetSchemaNullJsonMapper_NullPointerException() throws Exception {
        // Build a factory where the jsonMapper dependency is null but the
        // URISchemeFactory is non‑null (required by the constructor).
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema#";
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(defaultMetaSchemaURI, JsonMetaSchema.getV7());
        Map<String, String> uriMap = Collections.emptyMap();
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        JsonSchemaFactory factoryWithNullMapper = ctor.newInstance(// null ObjectMapper
        null, fakeYamlMapper, defaultMetaSchemaURI, // still need a non‑null URIFactory
        mockUriFactory, null, null, metaSchemas, uriMap, false, null);
        // Act & Assert
        NullPointerException npe = assertThrows(NullPointerException.class, () -> factoryWithNullMapper.getSchema("{\"type\":\"object\"}", new SchemaValidatorsConfig()), "Calling getSchema with a factory that has a null ObjectMapper should throw NPE");
        assertNotNull(npe);
    }

    /* -----------------------------------------------------------------
       Helper method to create simple Mockito‑like mocks for classes that are
       only used as placeholders in the test (e.g., JsonMetaSchema).  The
       project does not contain Mockito, but we can rely on the fact that
       those classes are never interacted with in the test logic.
    ----------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static <T> T mock(Class<T> clazz) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, (proxy, method, args) -> {
            Class<?> ret = method.getReturnType();
            if (ret.isPrimitive()) {
                if (ret == boolean.class)
                    return false;
                if (ret == byte.class)
                    return (byte) 0;
                if (ret == short.class)
                    return (short) 0;
                if (ret == int.class)
                    return 0;
                if (ret == long.class)
                    return 0L;
                if (ret == float.class)
                    return 0f;
                if (ret == double.class)
                    return 0d;
                if (ret == char.class)
                    return '\0';
            }
            return null;
        });
    }
}
