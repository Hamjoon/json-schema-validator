```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(String, SchemaValidatorsConfig)}.
 *
 * <p>The original test suite relied on Mockito for mocking.  The execution
 * environment for this kata does not include Mockito, therefore the tests
 * have been rewritten to use simple hand‑crafted fakes.</p>
 */
@DisplayName("JsonSchemaFactory.getSchema(String, …) tests")
class JsonSchemaFactory_getSchema_Test {

    /** Simple fake {@link ObjectMapper} that can be programmed to return a node or throw an exception. */
    private static class FakeObjectMapper extends ObjectMapper {
        private final Map<String, JsonNode> nodes = new HashMap<>();
        private final Map<String, IOException> exceptions = new HashMap<>();

        void whenReadTreeReturn(String json, JsonNode node) {
            nodes.put(json, node);
        }

        void whenReadTreeThrow(String json, IOException ex) {
            exceptions.put(json, ex);
        }

        @Override
        public JsonNode readTree(String content) throws IOException {
            if (exceptions.containsKey(content)) {
                throw exceptions.get(content);
            }
            if (nodes.containsKey(content)) {
                return nodes.get(content);
            }
            // Fallback – let the real ObjectMapper try (will likely fail, which is fine for the tests)
            return super.readTree(content);
        }
    }

    private FakeObjectMapper fakeObjectMapper;
    private YAMLMapper fakeYamlMapper; // not used directly, but required by the constructor
    private JsonSchemaFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        fakeObjectMapper = new FakeObjectMapper();
        fakeYamlMapper = new YAMLMapper(); // real instance – behaviour is not exercised in these tests

        // Minimal meta‑schema map – the real factory only needs the entry for the default URI
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema#";
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(defaultMetaSchemaURI, mock(JsonMetaSchema.class));

        // Empty URI map – acceptable for the private constructor
        Map<String, String> uriMap = Collections.emptyMap();

        // Use reflection to call the private constructor of JsonSchemaFactory
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(
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
                fakeObjectMapper,
                fakeYamlMapper,
                defaultMetaSchemaURI,
                null,          // URISchemeFactory – not needed for these tests
                null,          // URISchemeFetcher – not needed for these tests
                null,          // URNFactory – not needed for these tests
                metaSchemas,
                uriMap,
                false,
                null           // CompositeURITranslator – not needed for these tests
        );
    }

    @AfterEach
    void tearDown() {
        // No special cleanup required
    }

    @Test
    @DisplayName("Successful schema parsing returns a JsonSchema instance")
    void testGetSchemaSuccess() throws Exception {
        // Arrange – a very simple valid JSON schema
        String json = "{\"type\":\"string\"}";
        JsonNode fakeNode = mock(JsonNode.class);
        fakeObjectMapper.whenReadTreeReturn(json, fakeNode);

        // Act
        JsonSchema result = factory.getSchema(json, new SchemaValidatorsConfig());

        // Assert – the factory should have produced a non‑null JsonSchema
        assertNotNull(result, "Factory should return a JsonSchema for a valid schema string");
    }

    @Test
    @DisplayName("IOException from ObjectMapper is wrapped in JsonSchemaException")
    void testGetSchemaIOExceptionIsWrapped() {
        // Arrange
        String json = "invalid json";
        IOException ioException = new IOException("boom");
        fakeObjectMapper.whenReadTreeThrow(json, ioException);

        // Act & Assert
        JsonSchemaException thrown = assertThrows(
                JsonSchemaException.class,
                () -> factory.getSchema(json, null),
                "IOException should be wrapped in JsonSchemaException");
        assertEquals(ioException, thrown.getCause(),
                "Original IOException should be the cause of the thrown JsonSchemaException");
    }

    @Test
    @DisplayName("Null schema string propagates IllegalArgumentException")
    void testGetSchemaNullSchemaString_IllegalArgumentException() {
        // Arrange
        String schema = null;
        IllegalArgumentException illegalArg = new IllegalArgumentException("Schema is null");
        // The real ObjectMapper throws NPE for null; we simulate the same behaviour
        fakeObjectMapper.whenReadTreeThrow(schema, illegalArg);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> factory.getSchema(schema, new SchemaValidatorsConfig()),
                "Passing null should propagate IllegalArgumentException");
        assertSame(illegalArg, thrown,
                "The same exception instance should be propagated");
    }

    @Test
    @DisplayName("Empty string leads to JsonSchemaException")
    void testGetSchemaEmptyString_JsonSchemaException() {
        // Arrange
        String schema = "";
        IOException io = new IOException("Empty content");
        fakeObjectMapper.whenReadTreeThrow(schema, io);

        // Act & Assert
        JsonSchemaException thrown = assertThrows(
                JsonSchemaException.class,
                () -> factory.getSchema(schema, new SchemaValidatorsConfig()),
                "Empty string should be wrapped in JsonSchemaException");
        assertEquals(io, thrown.getCause(),
                "The original IOException should be the cause");
    }

    @Test
    @DisplayName("Malformed JSON leads to JsonSchemaException")
    void testGetSchemaMalformedJson_JsonSchemaException() {
        // Arrange
        String schema = "{type:object,}";
        IOException parseException = new IOException("Malformed JSON");
        fakeObjectMapper.whenReadTreeThrow(schema, parseException);

        // Act & Assert
        JsonSchemaException thrown = assertThrows(
                JsonSchemaException.class,
                () -> factory.getSchema(schema, new SchemaValidatorsConfig()),
                "Malformed JSON should be wrapped in JsonSchemaException");
        assertEquals(parseException, thrown.getCause(),
                "The original IOException should be the cause");
    }

    @Test
    @DisplayName("Null config is accepted and a JsonSchema is still produced")
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

    @Test
    @DisplayName("Complex valid schema is parsed without error")
    void testGetSchemaComplexValidSchema() throws Exception {
        // Arrange – a fairly large but still syntactically correct schema
        String complexSchema = "{\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"id\": {\"type\": \"integer\"},\n" +
                "    \"name\": {\"type\": \"string\"},\n" +
                "    \"tags\": {\"type\": \"array\", \"items\": {\"type\": \"string\"}},\n" +
                "    \"address\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"properties\": {\n" +
                "        \"street\": {\"type\": \"string\"},\n" +
                "        \"zip\": {\"type\": \"string\"}\n" +
                "      },\n" +
                "      \"required\": [\"street\"]\n" +
                "    }\n" +
                "  },\n" +
                "  \"required\": [\"id\", \"name\"]\n" +
                "}";
        JsonNode fakeNode = mock(JsonNode.class);
        fakeObjectMapper.whenReadTreeReturn(complexSchema, fakeNode);

        // Act
        JsonSchema result = factory.getSchema(complexSchema, new SchemaValidatorsConfig());

        // Assert
        assertNotNull(result, "Factory should parse a complex valid schema");
    }

    @Test
    @DisplayName("Schema containing internal $ref is parsed")
    void testGetSchemaWithInternalRef() throws Exception {
        // Arrange
        String schemaWithRef = "{\n" +
                "  \"$id\": \"http://example.com/root.json\",\n" +
                "  \"definitions\": {\n" +
                "    \"positiveInteger\": {\"type\": \"integer\", \"minimum\": 0}\n" +
                "  },\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"count\": {\"$ref\": \"#/definitions/positiveInteger\"}\n" +
                "  }\n" +
                "}";
        JsonNode fakeNode = mock(JsonNode.class);
        fakeObjectMapper.whenReadTreeReturn(schemaWithRef, fakeNode);

        // Act
        JsonSchema result = factory.getSchema(schemaWithRef, new SchemaValidatorsConfig());

        // Assert
        assertNotNull(result, "Factory should handle schemas with internal $ref");
    }

    @Test
    @DisplayName("Factory built with a null ObjectMapper throws NPE on getSchema")
    void testGetSchemaNullJsonMapper_NullPointerException() throws Exception {
        // Build a factory where the jsonMapper dependency is null
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema#";
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(defaultMetaSchemaURI, mock(JsonMetaSchema.class));
        Map<String, String> uriMap = Collections.emptyMap();

        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(
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
        JsonSchemaFactory factoryWithNullMapper = ctor.newInstance(
                null,                     // null ObjectMapper
                fakeYamlMapper,
                defaultMetaSchemaURI,
                null,
                null,
                null,
                metaSchemas,
                uriMap,
                false,
                null);

        // Act & Assert
        NullPointerException npe = assertThrows(
                NullPointerException.class,
                () -> factoryWithNullMapper.getSchema("{\"type\":\"object\"}", new SchemaValidatorsConfig()),
                "Calling getSchema with a factory that has a null ObjectMapper should throw NPE");
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
        return (T) java.lang.reflect.Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class<?>[]{clazz},
                (proxy, method, args) -> {
                    // Return default values for primitive return types, null otherwise
                    Class<?> ret = method.getReturnType();
                    if (ret.isPrimitive()) {
                        if (ret == boolean.class) return false;
                        if (ret == byte.class) return (byte) 0;
                        if (ret == short.class) return (short) 0;
                        if (ret == int.class) return 0;
                        if (ret == long.class) return 0L;
                        if (ret == float.class) return 0f;
                        if (ret == double.class) return 0d;
                        if (ret == char.class) return '\0';
                    }
                    return null;
                });
    }
}
```