
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
import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import com.networknt.schema.urn.URNFactory;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

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

    /**
     * Collaborators of {@link JsonSchemaFactory}
     */
    private URISchemeFactory uriFactory;

    private URISchemeFetcher uriFetcher;

    private URNFactory urnFactory;

    private ObjectMapper objectMapper;

    private YAMLMapper yamlMapper;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Simple real instances for the collaborators – the factory does not use them
        // for the behaviours exercised in these tests.
        ObjectMapper objectMapper = new ObjectMapper();
        YAMLMapper yamlMapper = new YAMLMapper();
        // Empty maps – the factory will fall back to its built‑in meta‑schemas.
        Map<String, JsonMetaSchema> emptyMetaMap = Collections.emptyMap();
        Map<String, String> emptyUriMap = Collections.emptyMap();
        URITranslator.CompositeURITranslator compositeTranslator = new URITranslator.CompositeURITranslator();
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, URITranslator.CompositeURITranslator.class);
        ctor.setAccessible(true);
        // Factory used in the first test
        factory = ctor.newInstance(objectMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, null, null, null, emptyMetaMap, emptyUriMap, false, compositeTranslator);
        // Default factory (draft‑07) used by the majority of tests
        defaultFactory = ctor.newInstance(objectMapper, yamlMapper, DRAFT07_URI, null, null, null, emptyMetaMap, emptyUriMap, false, compositeTranslator);
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
        JsonNode emptySchemaNode = JsonNodeFactory.instance.objectNode();
        ValidationContext ctx = factory.createValidationContext(emptySchemaNode);
        assertNotNull(ctx, "createValidationContext should never return null");
        // The default meta‑schema should be associated with the context.
        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertEquals(DEFAULT_META_SCHEMA_URI, meta.getUri().toString(), "The ValidationContext should contain the meta‑schema associated with the default URI");
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
        assertEquals(DRAFT07_URI, meta.getUri().toString(), "The ValidationContext must contain the draft‑07 meta‑schema");
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
        assertEquals(DRAFT07_URI, meta.getUri().toString(), "Should use the default draft‑07 meta‑schema");
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
        String json = "{ \"$schema\": \"urn:jsonschema:draft-07\", \"type\": \"number\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        ValidationContext ctx = defaultFactory.createValidationContext(node);
        assertNotNull(ctx);
        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertEquals(DRAFT07_URI, meta.getUri().toString(), "URN should resolve to the same draft‑07 meta‑schema instance");
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
        assertEquals(DRAFT07_URI, meta.getUri().toString());
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
        assertEquals(DRAFT07_URI, meta.getUri().toString());
    }

    /* ----------------------------------------------------------------------
     * 9. Factory built for draft‑04 – missing $schema must resolve to draft‑04
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Factory created for draft‑04 uses draft‑04 default meta‑schema")
    void factoryDraft04_missingSchemaUsesDraft04() throws Exception {
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, URITranslator.CompositeURITranslator.class);
        ctor.setAccessible(true);
        JsonSchemaFactory draft04Factory = ctor.newInstance(new ObjectMapper(), new YAMLMapper(), DRAFT04_URI, null, null, null, Collections.emptyMap(), Collections.emptyMap(), false, new URITranslator.CompositeURITranslator());
        JsonNode node = JSON_PARSER.readTree("{ \"type\": \"boolean\" }");
        ValidationContext ctx = draft04Factory.createValidationContext(node);
        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertEquals(DRAFT04_URI, meta.getUri().toString(), "Should resolve to draft‑04 meta‑schema");
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
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:15: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:26: error: cannot find symbol
    private URISchemeFactory uriFactoryMock;
            ^
  symbol:   class URISchemeFactory
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:29: error: cannot find symbol
    private URISchemeFetcher uriFetcherMock;
            ^
  symbol:   class URISchemeFetcher
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:70: error: cannot find symbol
    private URISchemeFactory uriFactory;
            ^
  symbol:   class URISchemeFactory
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:72: error: cannot find symbol
    private URISchemeFetcher uriFetcher;
            ^
  symbol:   class URISchemeFetcher
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:16: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:19: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:25: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:28: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_createValidationContext_Test
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
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:90: error: cannot find symbol
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, URITranslator.CompositeURITranslator.class);
                                                                                                                                                 ^
  symbol:   class URISchemeFactory
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:90: error: cannot find symbol
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, URITranslator.CompositeURITranslator.class);
                                                                                                                                                                         ^
  symbol:   class URISchemeFetcher
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:223: error: cannot find symbol
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, URITranslator.CompositeURITranslator.class);
                                                                                                                                                 ^
  symbol:   class URISchemeFactory
  location: class JsonSchemaFactory_createValidationContext_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_createValidationContext_Test.java:223: error: cannot find symbol
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, URITranslator.CompositeURITranslator.class);
                                                                                                                                                                         ^
  symbol:   class URISchemeFetcher
  location: class JsonSchemaFactory_createValidationContext_Test
16 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.ValidationContext', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\tdependent fields:\n\t\tcom.networknt.schema.uri.URISchemeFactory com.networknt.schema.JsonSchemaFactory.uriFactory;\n\t\tcom.networknt.schema.urn.URNFactory com.networknt.schema.JsonSchemaFactory.urnFactory;\n\trelated functions:\n\t\tmethod `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.createValidationContext(JsonNode)`;\n\t\tmethod `public URIFactory getUriFactory()` returns `com.networknt.schema.uri.URIFactory`, related with `com.networknt.schema.JsonSchemaFactory.createValidationContext(JsonNode)`, api document: \n     * @return A shared {@link URI} factory that is used for creating the URI references in schemas.\n     ;\n\t\tmethod `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.createValidationContext(JsonNode)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.createValidationContext(JsonNode)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


