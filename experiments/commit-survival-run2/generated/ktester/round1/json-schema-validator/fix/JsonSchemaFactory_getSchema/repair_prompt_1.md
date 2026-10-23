
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
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
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchemaFactory.getSchema(String, …) tests")
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

    private FakeObjectMapper fakeObjectMapper;

    // not used directly, but required by the constructor
    private YAMLMapper fakeYamlMapper;

    /**
     * Simple fake {@link ObjectMapper} that can be programmed to return a node or throw an exception.
     */
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
        fakeObjectMapper = new FakeObjectMapper();
        // real instance – behaviour is not exercised in these tests
        fakeYamlMapper = new YAMLMapper();
        // Minimal meta‑schema map – the real factory only needs the entry for the default URI
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema#";
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(defaultMetaSchemaURI, mock(JsonMetaSchema.class));
        // Empty URI map – acceptable for the private constructor
        Map<String, String> uriMap = Collections.emptyMap();
        // Use reflection to call the private constructor of JsonSchemaFactory
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        factory = ctor.newInstance(fakeObjectMapper, fakeYamlMapper, defaultMetaSchemaURI, // URISchemeFactory – not needed for these tests
        null, // URISchemeFetcher – not needed for these tests
        null, // URNFactory – not needed for these tests
        null, metaSchemas, uriMap, false, // CompositeURITranslator – not needed for these tests
        null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid interference between tests
        reset(mockObjectMapper, mockYamlMapper, mockUriFactory, mockUriFetcher, mockUrnFactory, mockCompositeTranslator);
    }

    @Test
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
        // The real ObjectMapper throws NPE for null; we simulate the same behaviour
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
        // Build a factory where the jsonMapper dependency is null
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema#";
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(defaultMetaSchemaURI, mock(JsonMetaSchema.class));
        Map<String, String> uriMap = Collections.emptyMap();
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        JsonSchemaFactory factoryWithNullMapper = ctor.newInstance(// null ObjectMapper
        null, fakeYamlMapper, defaultMetaSchemaURI, null, null, null, metaSchemas, uriMap, false, null);
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
            // Return default values for primitive return types, null otherwise
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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:17: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:18: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:21: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:26: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:29: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:32: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:35: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:38: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:41: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:70: error: readTree(String) in FakeObjectMapper cannot override readTree(String) in ObjectMapper
        public JsonNode readTree(String content) throws IOException {
                        ^
  overridden method does not throw IOException
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:96: error: cannot find symbol
        factory = spy(factory);
                  ^
  symbol:   method spy(JsonSchemaFactory)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:101: error: variable defaultMetaSchemaURI is already defined in method setupBeforeEach()
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema#";
               ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:102: error: variable metaSchemas is already defined in method setupBeforeEach()
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
                                    ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:105: error: variable uriMap is already defined in method setupBeforeEach()
        Map<String, String> uriMap = Collections.emptyMap();
                            ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:107: error: variable ctor is already defined in method setupBeforeEach()
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
                                       ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:119: error: cannot find symbol
        reset(mockObjectMapper, mockYamlMapper, mockUriFactory, mockUriFetcher, mockUrnFactory, mockCompositeTranslator);
        ^
  symbol:   method reset(ObjectMapper,YAMLMapper,URISchemeFactory,URISchemeFetcher,URNFactory,CompositeURITranslator)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:152: error: incompatible types: IllegalArgumentException cannot be converted to IOException
        fakeObjectMapper.whenReadTreeThrow(schema, illegalArg);
                                                   ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
17 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\tdependent fields:\n\t\tcom.fasterxml.jackson.databind.ObjectMapper com.networknt.schema.JsonSchemaFactory.jsonMapper;\n\t\torg.slf4j.Logger com.networknt.schema.JsonSchemaFactory.logger;\n\trelated functions:\n\t\tmethod `private boolean idMatchesSourceUri(JsonMetaSchema, JsonNode, URI)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)`;\n\t\tmethod `public JsonSchema getSchema(InputStream, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)`;\n\t\tmethod `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)`;\n\t\tmethod `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)`;\nclass `com.networknt.schema.JsonMetaSchema`:\n\trelated functions:\n\t\tmethod `public JsonValidator newValidator(ValidationContext, String, String, JsonNode, JsonSchema, String)` returns `com.networknt.schema.JsonValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


