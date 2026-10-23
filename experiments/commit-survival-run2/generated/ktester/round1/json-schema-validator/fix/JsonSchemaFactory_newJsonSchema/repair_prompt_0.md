
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
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_newJsonSchema_Test {

    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    private JsonSchemaFactory factory;

    private ObjectMapper jsonMapper;

    private YAMLMapper yamlMapper;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // real mappers – they are lightweight and needed for JsonNode creation
        jsonMapper = new ObjectMapper();
        yamlMapper = new YAMLMapper();
        // minimal but valid constructor arguments
        URISchemeFactory uriFactory = new URISchemeFactory(Collections.emptyMap());
        URISchemeFetcher uriFetcher = new URISchemeFetcher(Collections.emptyMap());
        // default ctor (does nothing)
        URNFactory urnFactory = new URNFactory();
        CompositeURITranslator uriTranslators = new CompositeURITranslator();
        // a dummy meta‑schema entry – the factory only checks presence, not content
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> jsonMetaSchemas = new HashMap<>();
        jsonMetaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMetaSchema);
        Map<String, String> uriMap = new HashMap<>();
        // use reflection because the constructor is private
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        factory = ctor.newInstance(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, urnFactory, jsonMetaSchemas, uriMap, false, uriTranslators);
        jsonMapper = new ObjectMapper();
        yamlMapper = new YAMLMapper();
        URISchemeFactory uriFactory = new URISchemeFactory(Collections.emptyMap());
        URISchemeFetcher uriFetcher = new URISchemeFetcher(Collections.emptyMap());
        URNFactory urnFactory = new URNFactory();
        CompositeURITranslator uriTranslators = new CompositeURITranslator();
        // The factory keeps a map of meta‑schema URIs → JsonMetaSchema objects.
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMetaSchema);
        Map<String, String> uriMap = new HashMap<>();
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        factory = ctor.newInstance(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, urnFactory, metaSchemas, uriMap, false, uriTranslators);
    }

    private JsonSchema invokeNewJsonSchema(URI uri, JsonNode node, SchemaValidatorsConfig cfg) throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("newJsonSchema", URI.class, JsonNode.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        return (JsonSchema) m.invoke(factory, uri, node, cfg);
    }

    private static Stream<Arguments> validSchemaProvider() {
        return Stream.of(// group: Valid inputs – typical JSON schema
        Arguments.of(URI.create("http://example.com/schema.json"), "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}", new SchemaValidatorsConfig(), "typical"), // group: Empty schema node – minimal object
        Arguments.of(URI.create("http://example.com/empty-schema.json"), "{}", new SchemaValidatorsConfig(), "empty"), // group: Boolean schema – true literal
        Arguments.of(URI.create("file:/tmp/boolean-schema.json"), "true", new SchemaValidatorsConfig(), "boolean"), // group: Complex schema with nested subschemas – branch coverage in doCreate
        Arguments.of(URI.create("http://example.com/complex.json"), "{\"type\":\"object\",\"properties\":{\"address\":{\"type\":\"object\",\"properties\":{\"street\":{\"type\":\"string\"}}}}}", new SchemaValidatorsConfig(), "complex"), // group: $id and $schema meta‑schema handling
        Arguments.of(URI.create("http://example.com/meta.json"), "{\"$id\":\"http://example.com/meta.json\",\"$schema\":\"http://json-schema.org/draft-06/schema#\",\"type\":\"object\"}", new SchemaValidatorsConfig(), "metaId"), // group: Unsupported keyword – should still create schema
        Arguments.of(URI.create("http://example.com/unknown-key.json"), "{\"type\":\"object\",\"unknownKeyword\":123}", new SchemaValidatorsConfig(), "unknownKeyword"), // group: Array schema with items subschema
        Arguments.of(URI.create("http://example.com/array-schema.json"), "{\"type\":\"array\",\"items\":{\"type\":\"string\"}}", new SchemaValidatorsConfig(), "arrayItems"), // group: Schema node missing explicit type – still creates
        Arguments.of(URI.create("http://example.com/no-type.json"), "{\"properties\":{\"age\":{\"type\":\"integer\"}}}", new SchemaValidatorsConfig(), "noTopLevelType"), // group: Schema URI containing a fragment
        Arguments.of(new URI("http://example.com/schema.json#/definitions/foo"), "{\"$ref\":\"#/definitions/bar\"}", new SchemaValidatorsConfig(), "fragmentUri"), // group: Schema URI with query parameters
        Arguments.of(new URI("http://example.com/schema.json?version=1"), "{\"type\":\"object\"}", new SchemaValidatorsConfig(), "queryUri"), // group: Custom configuration – typeLoose enabled
        Arguments.of(URI.create("http://example.com/type-loose.json"), "{\"type\":\"integer\",\"minimum\":5}", new SchemaValidatorsConfig() {

            {
                setTypeLoose(true);
            }
        }, "typeLoose"));
    }

    private static Stream<Arguments> nullConfigProvider() {
        return Stream.of(// group: Null configuration – default behaviour
        Arguments.of(URI.create("file:/tmp/meta-schema.json"), "{\"$schema\":\"http://json-schema.org/draft-07/schema#\"}", null, "nullConfig"), // group: Both schemaUri and config null
        Arguments.of(null, "{\"type\":\"string\"}", null, "nullUriAndConfig"));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// group: Null schema node – error handling
        Arguments.of(URI.create("http://example.com/empty.json"), null, new SchemaValidatorsConfig(), NullPointerException.class, "nullNode"), // group: Invalid schema node – numeric literal
        Arguments.of(URI.create("http://example.com/numeric.json"), new com.fasterxml.jackson.databind.node.IntNode(42), new SchemaValidatorsConfig(), IllegalArgumentException.class, "numericNode"), // group: Invalid schema node – MissingNode
        Arguments.of(URI.create("http://example.com/missing.json"), com.fasterxml.jackson.databind.node.MissingNode.getInstance(), new SchemaValidatorsConfig(), IllegalArgumentException.class, "missingNode"), // group: Unsupported URI scheme – should raise exception
        Arguments.of(new URI("unknown://example.com/schema.json"), "{\"type\":\"object\"}", new SchemaValidatorsConfig(), IllegalArgumentException.class, "unknownScheme"));
    }

    /**
     * Retrieves a field value via reflection, making the field accessible.
     *
     * @param target the object (or class) containing the field
     * @param name   the field name
     * @return the field value
     */
    private static Object getField(Object target, String name) throws Exception {
        Class<?> clazz = (target instanceof Class) ? (Class<?>) target : target.getClass();
        Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    @Test
    void newJsonSchema_WithAllParameters_ShouldReturnJsonSchema() throws Exception {
        // given
        URI schemaUri = URI.create("http://example.com/schema");
        JsonNode schemaNode = jsonMapper.readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // when
        Method m = JsonSchemaFactory.class.getDeclaredMethod("newJsonSchema", URI.class, JsonNode.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        JsonSchema result = (JsonSchema) m.invoke(factory, schemaUri, schemaNode, config);
        // then
        assertNotNull(result, "newJsonSchema should return a non‑null JsonSchema instance");
    }

    @Test
    void newJsonSchema_WithNullUriAndConfig_ShouldReturnJsonSchema() throws Exception {
        // given
        JsonNode schemaNode = jsonMapper.readTree("{\"type\":\"string\"}");
        // when
        Method m = JsonSchemaFactory.class.getDeclaredMethod("newJsonSchema", URI.class, JsonNode.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        JsonSchema result = (JsonSchema) m.invoke(factory, null, schemaNode, null);
        // then
        assertNotNull(result, "newJsonSchema should handle null URI and null config and still return a JsonSchema");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validSchemaProvider")
    void newJsonSchema_ValidSchemas_ShouldCreateCorrectJsonSchema(URI uri, String json, SchemaValidatorsConfig config, String description) throws Exception {
        JsonNode node = jsonMapper.readTree(json);
        JsonSchema schema = invokeNewJsonSchema(uri, node, config);
        assertNotNull(schema, "Schema must not be null for: " + description);
        // Verify that the original node is preserved (or equivalent) inside the JsonSchema.
        JsonNode storedNode = (JsonNode) getField(schema, "schemaNode");
        assertEquals(node, storedNode, "Stored schema node must equal the supplied node");
        // Verify that the current URI is stored correctly (may be null).
        URI storedUri = (URI) getField(schema, "currentUri");
        assertEquals(uri, storedUri, "Current URI should match the supplied URI");
        // Additional branching checks for selected groups
        switch(description) {
            case "complex":
                // the address property must have its own "properties" node
                JsonNode addressProps = storedNode.path("properties").path("address").path("properties");
                assertTrue(addressProps.has("street"), "Complex schema should contain nested 'street' property");
                break;
            case "boolean":
                assertTrue(storedNode.isBoolean() && storedNode.booleanValue(), "Boolean schema node should be the literal true");
                break;
            case "fragmentUri":
                assertNotNull(storedUri);
                assertTrue(storedUri.getFragment().equals("definitions/foo"), "Fragment part must be retained in the current URI");
                break;
            case "queryUri":
                assertNotNull(storedUri);
                assertEquals("version=1", storedUri.getQuery(), "Query string must be preserved");
                break;
            case "typeLoose":
                // The ValidationContext inside the created JsonSchema should carry the custom config.
                Object validationContext = getField(schema, "validationContext");
                SchemaValidatorsConfig effectiveCfg = (SchemaValidatorsConfig) getField(validationContext, "config");
                assertTrue(effectiveCfg.isTypeLoose(), "Custom configuration flag ‘typeLoose’ must be true inside the context");
                break;
            default:
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullConfigProvider")
    void newJsonSchema_NullConfigOrUri_ShouldFallBackToDefaults(URI uri, String json, SchemaValidatorsConfig config, String description) throws Exception {
        JsonNode node = jsonMapper.readTree(json);
        JsonSchema schema = invokeNewJsonSchema(uri, node, config);
        assertNotNull(schema, "Schema must be created even when config/uri is null: " + description);
        // Default config must be non‑null inside the ValidationContext.
        Object validationContext = getField(schema, "validationContext");
        SchemaValidatorsConfig effectiveCfg = (SchemaValidatorsConfig) getField(validationContext, "config");
        assertNotNull(effectiveCfg, "A default SchemaValidatorsConfig should be created when null is supplied");
        // When URI is null the factory should keep it null (or a fallback).  We only assert that no NPE occurs.
        URI storedUri = (URI) getField(schema, "currentUri");
        if (uri == null) {
            assertNull(storedUri, "Current URI should be null when null was supplied");
        } else {
            assertEquals(uri, storedUri);
        }
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("exceptionProvider")
    void newJsonSchema_InvalidInputs_ShouldThrowException(URI uri, JsonNode node, SchemaValidatorsConfig config, Class<? extends Throwable> expected, String description) {
        Throwable thrown = assertThrows(expected, () -> invokeNewJsonSchema(uri, node, config), "Expected " + expected.getSimpleName() + " for case: " + description);
        // optionally log the message for debugging
        assertNotNull(thrown.getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:19: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:20: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:23: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:43: error: URNFactory is abstract; cannot be instantiated
        URNFactory urnFactory = new URNFactory();
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:46: error: cannot find symbol
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
                                         ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_newJsonSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:56: error: variable uriFactory is already defined in method setupBeforeEach()
        URISchemeFactory uriFactory = new URISchemeFactory(Collections.emptyMap());
                         ^
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:57: error: variable uriFetcher is already defined in method setupBeforeEach()
        URISchemeFetcher uriFetcher = new URISchemeFetcher(Collections.emptyMap());
                         ^
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:58: error: variable urnFactory is already defined in method setupBeforeEach()
        URNFactory urnFactory = new URNFactory();
                   ^
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:58: error: URNFactory is abstract; cannot be instantiated
        URNFactory urnFactory = new URNFactory();
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:59: error: variable uriTranslators is already defined in method setupBeforeEach()
        CompositeURITranslator uriTranslators = new CompositeURITranslator();
                               ^
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:61: error: variable dummyMetaSchema is already defined in method setupBeforeEach()
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
                       ^
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:61: error: cannot find symbol
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
                                         ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_newJsonSchema_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:64: error: variable uriMap is already defined in method setupBeforeEach()
        Map<String, String> uriMap = new HashMap<>();
                            ^
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:65: error: variable ctor is already defined in method setupBeforeEach()
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
                                       ^
15 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`;\n\t\tmethod `public JsonSchema create(ValidationContext, String, JsonNode, JsonSchema)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`;\n\t\tmethod `protected JsonSchema newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


