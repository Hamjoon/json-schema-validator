
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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createValidationContext_Test {

    /**
     * Mocks for the collaborators of {@link JsonSchemaFactory} **
     */
    @Mock
    private URISchemeFactory uriFactoryMock;

    @Mock
    private URISchemeFetcher uriFetcherMock;

    @Mock
    private URNFactory urnFactoryMock;

    @Mock
    private ObjectMapper objectMapperMock;

    @Mock
    private YAMLMapper yamlMapperMock;

    /**
     * Instance under test **
     */
    private JsonSchemaFactory factory;

    /**
     * Constants used for construction **
     */
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    /**
     * Instance under test – the default factory (draft‑07) **
     */
    private JsonSchemaFactory defaultFactory;

    /**
     * Helper ObjectMapper for parsing test JSON literals **
     */
    private static final ObjectMapper JSON_PARSER = new ObjectMapper();

    /**
     * Constants for meta‑schema URIs **
     */
    private static final String DRAFT07_URI = "http://json-schema.org/draft-07/schema#";

    private static final String DRAFT04_URI = "http://json-schema.org/draft-04/schema#";

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /*
         * The constructor of JsonSchemaFactory is package‑private (actually private) and expects a
         * fairly large set of arguments.  We create a minimal but valid set of arguments and use
         * reflection to instantiate the factory.
         *
         * The only argument that is consulted by {@code createValidationContext} is the map of
         * {@link JsonMetaSchema}s – it is used by the private {@code findMetaSchemaForSchema}
         * method when the supplied schema does not declare its own $schema property.  Therefore we
         * provide a map that contains an entry for the default meta‑schema URI and associate it
         * with a mock {@link JsonMetaSchema}.
         */
        JsonMetaSchema metaSchemaMock = mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> jsonMetaSchemas = new HashMap<>();
        jsonMetaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), metaSchemaMock);
        // Empty URI‑map – not needed for this test.
        Map<String, String> uriMap = Collections.emptyMap();
        // The CompositeURITranslator can be instantiated directly – it has a no‑arg constructor.
        URITranslator.CompositeURITranslator compositeTranslator = new URITranslator.CompositeURITranslator();
        // Obtain the (private) constructor via reflection.
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, URITranslator.CompositeURITranslator.class);
        ctor.setAccessible(true);
        factory = ctor.newInstance(objectMapperMock, yamlMapperMock, DEFAULT_META_SCHEMA_URI, uriFactoryMock, uriFetcherMock, urnFactoryMock, jsonMetaSchemas, uriMap, // enableUriSchemaCache
        false, compositeTranslator);
    }

    /* ----------------------------------------------------------------------
     * Utility to extract the JsonMetaSchema object from a ValidationContext
     * via reflection (the field is package‑private in the original library).
     * ---------------------------------------------------------------------- */
    private JsonMetaSchema extractMetaSchema(ValidationContext ctx) throws Exception {
        Field f = ValidationContext.class.getDeclaredField("jsonMetaSchema");
        f.setAccessible(true);
        return (JsonMetaSchema) f.get(ctx);
    }

    @Test
    void createValidationContext_returnsNonNullContext() {
        // GIVEN – a simple (empty) JSON schema node.  An empty node triggers the factory to fall back
        // to the default meta‑schema supplied during construction.
        JsonNode emptySchemaNode = JsonNodeFactory.instance.objectNode();
        // WHEN – the method under test is invoked.
        ValidationContext ctx = factory.createValidationContext(emptySchemaNode);
        // THEN – a non‑null ValidationContext is returned.
        assertNotNull(ctx, "createValidationContext should never return null");
        // Additional sanity checks (optional, depending on the public API of ValidationContext).
        // We verify that the context holds the same JsonMetaSchema instance we injected.
        // ValidationContext provides a getter for the meta‑schema in the actual library.
        // If the getter name differs, adjust accordingly.
        assertSame(factory.getJsonMetaSchema(DEFAULT_META_SCHEMA_URI), ctx.getJsonMetaSchema(), "The ValidationContext should contain the meta‑schema associated with the default URI");
    }

    /* ----------------------------------------------------------------------
     * 1. Explicit $schema that matches a known meta‑schema (draft‑07)
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Explicit known $schema resolves to the correct meta‑schema")
    void explicitKnownSchema_resolvesMetaSchema() throws Exception {
        String json = "{ \"$schema\": \"" + DRAFT07_URI + "\", \"type\": \"object\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        ValidationContext ctx = defaultFactory.createValidationContext(node);
        assertNotNull(ctx, "Context must not be null");
        JsonMetaSchema meta = extractMetaSchema(ctx);
        // The meta‑schema instance should be the one we put into the factory map.
        assertSame(defaultFactory.getJsonMetaSchema(DRAFT07_URI), meta, "The ValidationContext must contain the draft‑07 meta‑schema");
        assertSame(uriFactoryMock, ctx.getUriFactory(), "uriFactory should be propagated");
        assertSame(urnFactoryMock, ctx.getUrnFactory(), "urnFactory should be propagated");
    }

    /* ----------------------------------------------------------------------
     * 2. No $schema – fallback to default meta‑schema (draft‑07)
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Missing $schema falls back to the factory default")
    void missingSchema_usesDefaultMetaSchema() throws Exception {
        JsonNode node = JSON_PARSER.readTree("{ \"type\": \"string\" }");
        ValidationContext ctx = defaultFactory.createValidationContext(node);
        assertNotNull(ctx);
        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertSame(defaultFactory.getJsonMetaSchema(DRAFT07_URI), meta, "Should use the default draft‑07 meta‑schema");
    }

    /* ----------------------------------------------------------------------
     * 3. Null schema node – expect NPE
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Null schema node causes NullPointerException")
    void nullSchemaNode_throwsNPE() {
        assertThrows(NullPointerException.class, () -> defaultFactory.createValidationContext(null));
    }

    /* ----------------------------------------------------------------------
     * 4. Unknown $schema URI – expect IllegalArgumentException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Unknown $schema URI results in IllegalArgumentException")
    void unknownSchemaUri_throwsException() {
        String json = "{ \"$schema\": \"http://example.com/unknown/schema#\", \"type\": \"array\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class, () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 5. URN based $schema – the internal URITranslator should normalise it
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("URN $schema is translated to the canonical HTTP URI")
    void urnSchema_isTranslated() throws Exception {
        // The library ships with a built‑in URN translator, so we rely on it.
        String json = "{ \"$schema\": \"urn:jsonschema:draft-07\", \"type\": \"number\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        ValidationContext ctx = defaultFactory.createValidationContext(node);
        assertNotNull(ctx);
        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertSame(defaultFactory.getJsonMetaSchema(DRAFT07_URI), meta, "URN should resolve to the same draft‑07 meta‑schema instance");
    }

    /* ----------------------------------------------------------------------
     * 6. Non‑string $schema value (number) – expect IllegalArgumentException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema with non‑string numeric value throws")
    void nonStringSchemaNumber_throwsException() {
        String json = "{ \"$schema\": 123, \"type\": \"object\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class, () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 7. $schema with surrounding whitespace – should be trimmed
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema containing whitespace is trimmed and resolved")
    void schemaWithWhitespace_isTrimmed() throws Exception {
        String json = "{ \"$schema\": \"  " + DRAFT07_URI + "  \", \"type\": \"null\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        ValidationContext ctx = defaultFactory.createValidationContext(node);
        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertSame(defaultFactory.getJsonMetaSchema(DRAFT07_URI), meta);
    }

    /* ----------------------------------------------------------------------
     * 8. $schema without fragment identifier – factory should add '#'
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema without trailing fragment is normalised")
    void schemaWithoutFragment_isNormalised() throws Exception {
        String withoutFragment = "http://json-schema.org/draft-07/schema";
        String json = "{ \"$schema\": \"" + withoutFragment + "\", \"type\": \"integer\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        ValidationContext ctx = defaultFactory.createValidationContext(node);
        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertSame(defaultFactory.getJsonMetaSchema(DRAFT07_URI), meta);
    }

    /* ----------------------------------------------------------------------
     * 9. Factory built for draft‑04 – missing $schema must resolve to draft‑04
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Factory created for draft‑04 uses draft‑04 default meta‑schema")
    void factoryDraft04_missingSchemaUsesDraft04() throws Exception {
        // ------------------------------------------------------------------
        // Build a second factory that knows draft‑04.  The construction logic
        // mirrors the one in @BeforeEach but with different URI and map entry.
        // ------------------------------------------------------------------
        JsonMetaSchema draft04Meta = mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> metaMap = new HashMap<>();
        metaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DRAFT04_URI), draft04Meta);
        URITranslator.CompositeURITranslator translator = new URITranslator.CompositeURITranslator();
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, com.fasterxml.jackson.dataformat.yaml.YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, URITranslator.CompositeURITranslator.class);
        ctor.setAccessible(true);
        JsonSchemaFactory draft04Factory = ctor.newInstance(objectMapperMock, yamlMapperMock, DRAFT04_URI, uriFactoryMock, uriFetcherMock, urnFactoryMock, metaMap, Collections.emptyMap(), false, translator);
        JsonNode node = JSON_PARSER.readTree("{ \"type\": \"boolean\" }");
        ValidationContext ctx = draft04Factory.createValidationContext(node);
        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertSame(draft04Meta, meta, "Should resolve to draft‑04 meta‑schema");
    }

    /* ----------------------------------------------------------------------
     * 10. Two successive calls return distinct ValidationContext objects
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Multiple invocations produce independent ValidationContext objects")
    void multipleInvocations_areIndependent() {
        JsonNode node = JSON_PARSER.readTree("{ \"type\": \"string\" }");
        ValidationContext ctx1 = defaultFactory.createValidationContext(node);
        ValidationContext ctx2 = defaultFactory.createValidationContext(node);
        assertNotSame(ctx1, ctx2, "Each call must produce a new ValidationContext instance");
    }

    /* ----------------------------------------------------------------------
     * 11. $schema as JSON array – expect IllegalArgumentException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema specified as an array is rejected")
    void schemaArrayValue_throwsException() {
        String json = "{ \"$schema\": [\"http://json-schema.org/draft-07/schema#\"], \"type\": \"object\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class, () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 12. $schema as boolean – expect IllegalArgumentException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema specified as a boolean is rejected")
    void schemaBooleanValue_throwsException() {
        String json = "{ \"$schema\": true, \"type\": \"number\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class, () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 13. $schema empty string – reject
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema empty string throws")
    void schemaEmptyString_throwsException() {
        String json = "{ \"$schema\": \"\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class, () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 14. $schema whitespace‑only – reject
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema whitespace‑only string throws")
    void schemaWhitespaceOnly_throwsException() {
        String json = "{ \"$schema\": \"   \" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class, () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 15. $schema malformed URI – reject
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema with malformed URI throws")
    void schemaMalformedUri_throwsException() {
        String json = "{ \"$schema\": \"http://[invalid]\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class, () -> defaultFactory.createValidationContext(node));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:13: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:14: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:21: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:22: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:23: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:25: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:31: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:34: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:37: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:40: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:43: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:86: error: cannot find symbol
        JsonMetaSchema metaSchemaMock = mock(JsonMetaSchema.class);
                                        ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:123: error: cannot find symbol
        assertSame(factory.getJsonMetaSchema(DEFAULT_META_SCHEMA_URI), ctx.getJsonMetaSchema(), "The ValidationContext should contain the meta‑schema associated with the default URI");
                          ^
  symbol:   method getJsonMetaSchema(String)
  location: variable factory of type JsonSchemaFactory
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:123: error: cannot find symbol
        assertSame(factory.getJsonMetaSchema(DEFAULT_META_SCHEMA_URI), ctx.getJsonMetaSchema(), "The ValidationContext should contain the meta‑schema associated with the default URI");
                                                                          ^
  symbol:   method getJsonMetaSchema()
  location: variable ctx of type ValidationContext
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:138: error: cannot find symbol
        assertSame(defaultFactory.getJsonMetaSchema(DRAFT07_URI), meta, "The ValidationContext must contain the draft‑07 meta‑schema");
                                 ^
  symbol:   method getJsonMetaSchema(String)
  location: variable defaultFactory of type JsonSchemaFactory
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:139: error: cannot find symbol
        assertSame(uriFactoryMock, ctx.getUriFactory(), "uriFactory should be propagated");
                                      ^
  symbol:   method getUriFactory()
  location: variable ctx of type ValidationContext
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:140: error: cannot find symbol
        assertSame(urnFactoryMock, ctx.getUrnFactory(), "urnFactory should be propagated");
                                      ^
  symbol:   method getUrnFactory()
  location: variable ctx of type ValidationContext
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:153: error: cannot find symbol
        assertSame(defaultFactory.getJsonMetaSchema(DRAFT07_URI), meta, "Should use the default draft‑07 meta‑schema");
                                 ^
  symbol:   method getJsonMetaSchema(String)
  location: variable defaultFactory of type JsonSchemaFactory
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:188: error: cannot find symbol
        assertSame(defaultFactory.getJsonMetaSchema(DRAFT07_URI), meta, "URN should resolve to the same draft‑07 meta‑schema instance");
                                 ^
  symbol:   method getJsonMetaSchema(String)
  location: variable defaultFactory of type JsonSchemaFactory
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:212: error: cannot find symbol
        assertSame(defaultFactory.getJsonMetaSchema(DRAFT07_URI), meta);
                                 ^
  symbol:   method getJsonMetaSchema(String)
  location: variable defaultFactory of type JsonSchemaFactory
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:226: error: cannot find symbol
        assertSame(defaultFactory.getJsonMetaSchema(DRAFT07_URI), meta);
                                 ^
  symbol:   method getJsonMetaSchema(String)
  location: variable defaultFactory of type JsonSchemaFactory
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:239: error: cannot find symbol
        JsonMetaSchema draft04Meta = mock(JsonMetaSchema.class);
                                     ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_createValidationContext_Test
22 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.ValidationContext', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\tdependent fields:\n\t\tcom.networknt.schema.uri.URISchemeFactory com.networknt.schema.JsonSchemaFactory.uriFactory;\n\t\tcom.networknt.schema.urn.URNFactory com.networknt.schema.JsonSchemaFactory.urnFactory;\n\trelated functions:\n\t\tmethod `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.createValidationContext(JsonNode)`;\n\t\tmethod `public URIFactory getUriFactory()` returns `com.networknt.schema.uri.URIFactory`, related with `com.networknt.schema.JsonSchemaFactory.createValidationContext(JsonNode)`, api document: \n     * @return A shared {@link URI} factory that is used for creating the URI references in schemas.\n     ;\n\t\tmethod `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.createValidationContext(JsonNode)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.createValidationContext(JsonNode)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


