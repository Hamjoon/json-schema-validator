
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
import org.junit.jupiter.api.extension.ExtendWith;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeEach;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import org.mockito.junit.jupiter.MockitoExtension;

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
        // Obtain a factory instance via the public static method.
        // The default instance is sufficient because the tests replace the mappers via reflection.
        factory = JsonSchemaFactory.getInstance();
        // Ensure the factory uses the mappers we created (the field names are jsonMapper and yamlMapper)
        try {
            Field jsonMapperField = JsonSchemaFactory.class.getDeclaredField("jsonMapper");
            jsonMapperField.setAccessible(true);
            jsonMapperField.set(factory, jsonMapper);
        } catch (NoSuchFieldException ignored) {
            // If the implementation changes, the test will still work with the default mappers.
        }
        try {
            Field yamlMapperField = JsonSchemaFactory.class.getDeclaredField("yamlMapper");
            yamlMapperField.setAccessible(true);
            yamlMapperField.set(factory, yamlMapper);
        } catch (NoSuchFieldException ignored) {
        }
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
        Arguments.of(URI.create("unknown://example.com/schema.json"), "{\"type\":\"object\"}", new SchemaValidatorsConfig(), IllegalArgumentException.class, "unknownScheme"));
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
                JsonNode addressProps = storedNode.path("properties").path("address").path("properties");
                assertTrue(addressProps.has("street"), "Complex schema should contain nested 'street' property");
                break;
            case "boolean":
                assertTrue(storedNode.isBoolean() && storedNode.booleanValue(), "Boolean schema node should be the literal true");
                break;
            case "fragmentUri":
                assertNotNull(storedUri);
                assertEquals("definitions/foo", storedUri.getFragment(), "Fragment part must be retained in the current URI");
                break;
            case "queryUri":
                assertNotNull(storedUri);
                assertEquals("version=1", storedUri.getQuery(), "Query string must be preserved");
                break;
            case "typeLoose":
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
        assertNotNull(thrown.getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:19: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java:21: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
Note: src/test/java/com/networknt/schema/JsonSchemaFactory_newJsonSchema_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`;\n\t\tmethod `public JsonSchema create(ValidationContext, String, JsonNode, JsonSchema)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`;\n\t\tmethod `protected JsonSchema newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


