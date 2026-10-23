
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;

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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:18: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:19: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:11: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:20: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:23: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:33: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:36: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:39: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:42: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:54: error: cannot find symbol
        metaSchemas.put(defaultMetaSchemaURI, mock(JsonMetaSchema.class));
                                              ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:62: error: cannot find symbol
        factory = spy(factory);
                  ^
  symbol:   method spy(JsonSchemaFactory)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:68: error: cannot find symbol
        reset(mockObjectMapper, mockYamlMapper, mockUriFactory, mockUriFetcher, mockUrnFactory, mockCompositeTranslator);
        ^
  symbol:   method reset(ObjectMapper,YAMLMapper,URISchemeFactory,URISchemeFetcher,URNFactory,CompositeURITranslator)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:75: error: cannot find symbol
        JsonNode mockNode = mock(JsonNode.class);
                            ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:76: error: cannot find symbol
        JsonSchema mockSchema = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:79: error: cannot find symbol
        when(mockObjectMapper.readTree(json)).thenReturn(mockNode);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:81: error: cannot find symbol
        doReturn(mockSchema).when(factory).newJsonSchema(null, mockNode, config);
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:86: error: cannot find symbol
        verify(mockObjectMapper, times(1)).readTree(json);
                                 ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:87: error: cannot find symbol
        verify(factory, times(1)).newJsonSchema(null, mockNode, config);
                        ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:95: error: cannot find symbol
        when(mockObjectMapper.readTree(json)).thenThrow(ioException);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:99: error: cannot find symbol
        verify(mockObjectMapper, times(1)).readTree(json);
                                 ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:108: error: cannot find symbol
        when(mockObjectMapper.readTree(schema)).thenThrow(illegalArg);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:112: error: cannot find symbol
        verify(mockObjectMapper, times(1)).readTree(schema);
                                 ^
  symbol:   method times(int)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:121: error: cannot find symbol
        when(mockObjectMapper.readTree(schema)).thenThrow(io);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:125: error: cannot find symbol
        verify(mockObjectMapper).readTree(schema);
        ^
  symbol:   method verify(ObjectMapper)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:134: error: cannot find symbol
        when(mockObjectMapper.readTree(schema)).thenThrow(parseException);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:138: error: cannot find symbol
        verify(mockObjectMapper).readTree(schema);
        ^
  symbol:   method verify(ObjectMapper)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:146: error: cannot find symbol
        JsonNode mockNode = mock(JsonNode.class);
                            ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:147: error: cannot find symbol
        JsonSchema mockSchema = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:149: error: cannot find symbol
        when(mockObjectMapper.readTree(json)).thenReturn(mockNode);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:150: error: cannot find symbol
        doReturn(mockSchema).when(factory).newJsonSchema(null, mockNode, null);
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:155: error: cannot find symbol
        verify(mockObjectMapper).readTree(json);
        ^
  symbol:   method verify(ObjectMapper)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:156: error: cannot find symbol
        verify(factory).newJsonSchema(null, mockNode, null);
        ^
  symbol:   method verify(JsonSchemaFactory)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:164: error: cannot find symbol
        JsonNode mockNode = mock(JsonNode.class);
                            ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:165: error: cannot find symbol
        JsonSchema mockSchema = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:167: error: cannot find symbol
        when(mockObjectMapper.readTree(complexSchema)).thenReturn(mockNode);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:168: error: cannot find symbol
        doReturn(mockSchema).when(factory).newJsonSchema(null, mockNode, cfg);
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:173: error: cannot find symbol
        verify(mockObjectMapper).readTree(complexSchema);
        ^
  symbol:   method verify(ObjectMapper)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:174: error: cannot find symbol
        verify(factory).newJsonSchema(null, mockNode, cfg);
        ^
  symbol:   method verify(JsonSchemaFactory)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:182: error: cannot find symbol
        JsonNode mockNode = mock(JsonNode.class);
                            ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:183: error: cannot find symbol
        JsonSchema mockSchema = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:185: error: cannot find symbol
        when(mockObjectMapper.readTree(schemaWithRef)).thenReturn(mockNode);
        ^
  symbol:   method when(JsonNode)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:186: error: cannot find symbol
        doReturn(mockSchema).when(factory).newJsonSchema(null, mockNode, cfg);
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:191: error: cannot find symbol
        verify(mockObjectMapper).readTree(schemaWithRef);
        ^
  symbol:   method verify(ObjectMapper)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:192: error: cannot find symbol
        verify(factory).newJsonSchema(null, mockNode, cfg);
        ^
  symbol:   method verify(JsonSchemaFactory)
  location: class JsonSchemaFactory_getSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getSchema_Test.java:201: error: cannot find symbol
        metaSchemas.put(defaultMetaSchemaURI, mock(JsonMetaSchema.class));
                                              ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_getSchema_Test
47 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\tdependent fields:\n\t\tcom.fasterxml.jackson.databind.ObjectMapper com.networknt.schema.JsonSchemaFactory.jsonMapper;\n\t\torg.slf4j.Logger com.networknt.schema.JsonSchemaFactory.logger;\n\trelated functions:\n\t\tmethod `private boolean idMatchesSourceUri(JsonMetaSchema, JsonNode, URI)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)`;\n\t\tmethod `public JsonSchema getSchema(InputStream, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)`;\n\t\tmethod `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)`;\n\t\tmethod `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)`;\nclass `com.networknt.schema.JsonMetaSchema`:\n\trelated functions:\n\t\tmethod `public JsonValidator newValidator(ValidationContext, String, String, JsonNode, JsonSchema, String)` returns `com.networknt.schema.JsonValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(String, SchemaValidatorsConfig)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


