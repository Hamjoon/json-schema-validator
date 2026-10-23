
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
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_create_Test {

    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    private static TestableJsonSchemaFactory factory;

    private static JsonMetaSchema metaSchemaMock;

    private ValidationContext validationContext;

    private JsonNode emptyNode;

    private JsonSchema parentSchemaMock;

    private URI parentUri;

    /* --------------------------------------------------------------------- *
     *  Testable factory that records the arguments received by newJsonSchema *
     * --------------------------------------------------------------------- */
    private static class TestableJsonSchemaFactory extends JsonSchemaFactory {

        // value passed to doCreate (before any transformation)
        String capturedRawSchemaPath;

        // parentSchema.getCurrentUri()
        URI capturedCurrentUri;

        // node forwarded to newJsonSchema
        JsonNode capturedSchemaNode;

        // not used directly but kept for completeness
        ValidationContext capturedValidationContext;

        TestableJsonSchemaFactory(ObjectMapper jsonMapper, YAMLMapper yamlMapper, String defaultMetaSchemaURI, URISchemeFactory uriFactory, URISchemeFetcher uriFetcher, URNFactory urnFactory, Map<String, JsonMetaSchema> jsonMetaSchemas, Map<String, String> uriMap, boolean enableUriSchemaCache, CompositeURITranslator uriTranslators) {
            super(jsonMapper, yamlMapper, defaultMetaSchemaURI, uriFactory, uriFetcher, urnFactory, jsonMetaSchemas, uriMap, enableUriSchemaCache, uriTranslators);
        }

        @Override
        protected JsonSchema newJsonSchema(URI schemaUri, JsonNode schemaNode, SchemaValidatorsConfig config) {
            // Capture the arguments that flow from doCreate → newJsonSchema.
            this.capturedCurrentUri = schemaUri;
            this.capturedSchemaNode = schemaNode;
            // The raw schemaPath (the first String argument of doCreate) is captured
            // earlier via {@link #setCapturedRawSchemaPath(String)}.
            return mock(JsonSchema.class);
        }

        /**
         * Accessor used by the test to read the raw schemaPath that reach doCreate.
         */
        String getCapturedRawSchemaPath() {
            return capturedRawSchemaPath;
        }

        /**
         * Called via reflection from the test before invoking the private {@code doCreate}.
         */
        void setCapturedRawSchemaPath(String path) {
            this.capturedRawSchemaPath = path;
        }
    }

    /* --------------------------------------------------------------------- *
     *  6. ParentSchema with null currentUri
     * --------------------------------------------------------------------- */
    private static class DummyParentSchemaWithNullUri implements JsonSchema {

        @Override
        public URI getCurrentUri() {
            return null;
        }

        // All other methods are irrelevant for this test; they can return defaults or throw UnsupportedOperationException.
        @Override
        public void setCurrentUri(URI currentUri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchema getParentSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonNode getSchemaNode() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setMetaSchema(JsonMetaSchema metaSchema) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void validate(JsonNode node, ValidationContext validationContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory, SchemaValidatorsConfig config) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchemaFactory getFactory() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchema createDeepCopy() {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // ----- core objects required by the JsonSchemaFactory constructor -----
        ObjectMapper jsonMapper = new ObjectMapper();
        YAMLMapper yamlMapper = new YAMLMapper();
        // minimal but valid meta‑schema map
        metaSchemaMock = mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> metaSchemaMap = new HashMap<>();
        metaSchemaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), metaSchemaMock);
        // empty URI‑map and empty scheme factories/fetchers (they are not used in this test)
        URISchemeFactory uriFactory = new URISchemeFactory(Collections.emptyMap());
        URISchemeFetcher uriFetcher = new URISchemeFetcher(Collections.emptyMap());
        // default constructor
        URNFactory urnFactory = new URNFactory();
        CompositeURITranslator translators = new CompositeURITranslator();
        factory = new TestableJsonSchemaFactory(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, urnFactory, metaSchemaMap, Collections.emptyMap(), false, translators);
    }

    @BeforeEach
    void setupBeforeEach() {
        // ----- objects that change per test case -----
        parentUri = URI.create("http://example.com/parent");
        parentSchemaMock = mock(JsonSchema.class);
        when(parentSchemaMock.getCurrentUri()).thenReturn(parentUri);
        // a very small JSON node – an empty object is sufficient
        emptyNode = new ObjectMapper().createObjectNode();
        // ValidationContext requires a non‑null meta‑schema, factory and config (config may be null)
        validationContext = new ValidationContext(factory.getUriFactory(), factory.urnFactory, metaSchemaMock, factory, null);
    }

    /**
     * Helper that uses reflection to invoke the private {@code doCreate} method
     * and capture the raw {@code schemaPath} argument that {@code create} forwards.
     * The factory instance stores the captured value in a dedicated field.
     */
    private String invokeDoCreateAndCaptureRawPath(String schemaPath) {
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("doCreate", ValidationContext.class, String.class, URI.class, JsonNode.class, JsonSchema.class, boolean.class);
            m.setAccessible(true);
            // store the raw path so that the overridden newJsonSchema can see it
            factory.setCapturedRawSchemaPath(schemaPath == null ? "#" : schemaPath);
            // invoke the private method – it will eventually call our overridden newJsonSchema
            m.invoke(factory, validationContext, schemaPath == null ? "#" : schemaPath, parentUri, emptyNode, parentSchemaMock, false);
            // retrieve what was stored
            return factory.getCapturedRawSchemaPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void create_WithNullSchemaPath_UsesHashAsDefault() {
        // arrange
        // trigger the default "#"
        String schemaPath = null;
        // act
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        // assert
        assertNotNull(result, "Factory should return a JsonSchema instance");
        // The raw schemaPath that reaches doCreate is captured via reflection hack:
        // we cannot directly intercept the private method, but we can verify that the
        // parent URI was passed correctly to newJsonSchema (which we do intercept).
        assertEquals(parentUri, factory.capturedCurrentUri, "Parent URI should be forwarded to newJsonSchema");
        // The only way to check the default "#" is to ensure that the factory did not
        // receive a custom path – we verify that the captured raw path is "#".
        // This is achieved by invoking the private doCreate via reflection to
        // expose the argument (see the helper below).
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath);
        assertEquals("#", rawPath, "When schemaPath is null the default should be \"#\"");
    }

    @Test
    void create_WithExplicitSchemaPath_PassesItThrough() {
        String schemaPath = "/my/custom/path";
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema instance");
        assertEquals(parentUri, factory.capturedCurrentUri, "Parent URI must be forwarded to newJsonSchema");
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath, validationContext, emptyNode, parentSchemaMock);
        assertEquals(schemaPath, rawPath, "Provided schemaPath should be used unchanged");
    }

    /* --------------------------------------------------------------------- *
     *  2. Null parent schema handling
     * --------------------------------------------------------------------- */
    @Test
    void create_WithNullParentSchema_ThrowsNullPointerException() {
        String schemaPath = "/any";
        JsonSchema nullParent = null;
        assertThrows(NullPointerException.class, () -> factory.create(validationContext, schemaPath, emptyNode, nullParent), "Calling create with a null parentSchema must result in NPE because getCurrentUri() is accessed");
    }

    /* --------------------------------------------------------------------- *
     *  3. Null schemaNode forwarding
     * --------------------------------------------------------------------- */
    @Test
    void create_WithNullSchemaNode_ForwardsNull() {
        String schemaPath = "/path";
        // explicit null
        JsonNode node = null;
        JsonSchema result = factory.create(validationContext, schemaPath, node, parentSchemaMock);
        assertNotNull(result, "Factory should still return a JsonSchema instance");
        assertSame(node, factory.capturedSchemaNode, "The null JsonNode must be forwarded unchanged to newJsonSchema");
    }

    /* --------------------------------------------------------------------- *
     *  4. ValidationContext null handling
     * --------------------------------------------------------------------- */
    @Test
    void create_WithNullValidationContext_ForwardsNull() {
        String schemaPath = "/valid/path";
        ValidationContext nullVc = null;
        JsonSchema result = factory.create(nullVc, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema even when ValidationContext is null");
        // The test focus is that no NPE is thrown; we also verify that the parent URI is still correct.
        assertEquals(parentUri, factory.capturedCurrentUri, "Parent URI must still be forwarded correctly");
    }

    @Test
    void create_WithNonNullValidationContext_ForwardsSameInstance() {
        String schemaPath = "/valid/path";
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema");
        // We cannot capture validationContext inside newJsonSchema, but the fact that no exception occurs
        // proves that the same instance was passed through.
        // As an additional sanity check we invoke the private method via reflection with the same context.
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath, validationContext, emptyNode, parentSchemaMock);
        assertEquals(schemaPath, rawPath);
    }

    /* --------------------------------------------------------------------- *
     *  5. Empty schemaPath handling
     * --------------------------------------------------------------------- */
    @Test
    void create_WithEmptySchemaPath_ForwardsUnchanged() {
        // empty string is a legal path
        String schemaPath = "";
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema instance");
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath, validationContext, emptyNode, parentSchemaMock);
        assertEquals(schemaPath, rawPath, "Empty string schemaPath must be passed unchanged");
    }

    @Test
    void create_WithParentSchemaHavingNullCurrentUri_ForwardsNullUri() {
        String schemaPath = "/any";
        JsonSchema parentWithNullUri = new DummyParentSchemaWithNullUri();
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentWithNullUri);
        assertNotNull(result, "Factory should still create a JsonSchema");
        assertNull(factory.capturedCurrentUri, "When parentSchema.getCurrentUri() returns null it must be forwarded as null");
    }

    /* --------------------------------------------------------------------- *
     *  7. Complex schemaNode forwarding
     * --------------------------------------------------------------------- */
    @Test
    void create_WithComplexSchemaNode_ForwardsSameInstance() throws Exception {
        String schemaPath = "/complex";
        String json = "{" + "\"type\":\"object\"," + "\"properties\":{" + "   \"name\":{\"type\":\"string\"}," + "   \"age\":{\"type\":\"integer\"}" + "}" + "}";
        JsonNode complexNode = new ObjectMapper().readTree(json);
        JsonSchema result = factory.create(validationContext, schemaPath, complexNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema");
        assertSame(complexNode, factory.capturedSchemaNode, "The complex JsonNode must be forwarded unchanged to newJsonSchema");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:11: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:12: error: package org.mockito does not exist
import org.mockito.MockitoAnnotations;
                  ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:18: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:20: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:21: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:90: error: interface expected here
    private static class DummyParentSchemaWithNullUri implements JsonSchema {
                                                                 ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:129: error: cannot find symbol
        public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory) {
                                                                                 ^
  symbol:   class JsonValidatorFactory
  location: class DummyParentSchemaWithNullUri
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:134: error: cannot find symbol
        public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory, SchemaValidatorsConfig config) {
                                                                                 ^
  symbol:   class JsonValidatorFactory
  location: class DummyParentSchemaWithNullUri
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:24: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:59: error: JsonSchemaFactory(ObjectMapper,YAMLMapper,String,URISchemeFactory,URISchemeFetcher,URNFactory,Map<String,JsonMetaSchema>,Map<String,String>,boolean,CompositeURITranslator) has private access in JsonSchemaFactory
            super(jsonMapper, yamlMapper, defaultMetaSchemaURI, uriFactory, uriFetcher, urnFactory, jsonMetaSchemas, uriMap, enableUriSchemaCache, uriTranslators);
            ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:69: error: cannot find symbol
            return mock(JsonSchema.class);
                   ^
  symbol:   method mock(Class<JsonSchema>)
  location: class TestableJsonSchemaFactory
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:92: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:98: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:103: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:108: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:113: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:118: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:123: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:128: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:133: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:138: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:143: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:148: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:160: error: cannot find symbol
        metaSchemaMock = mock(JsonMetaSchema.class);
                         ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_create_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:167: error: URNFactory is abstract; cannot be instantiated
        URNFactory urnFactory = new URNFactory();
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:176: error: cannot find symbol
        parentSchemaMock = mock(JsonSchema.class);
                           ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchemaFactory_create_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:177: error: cannot find symbol
        when(parentSchemaMock.getCurrentUri()).thenReturn(parentUri);
        ^
  symbol:   method when(URI)
  location: class JsonSchemaFactory_create_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:181: error: urnFactory has private access in JsonSchemaFactory
        validationContext = new ValidationContext(factory.getUriFactory(), factory.urnFactory, metaSchemaMock, factory, null);
                                                                                  ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:231: error: method invokeDoCreateAndCaptureRawPath in class JsonSchemaFactory_create_Test cannot be applied to given types;
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath, validationContext, emptyNode, parentSchemaMock);
                         ^
  required: String
  found:    String,ValidationContext,JsonNode,JsonSchema
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:279: error: method invokeDoCreateAndCaptureRawPath in class JsonSchemaFactory_create_Test cannot be applied to given types;
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath, validationContext, emptyNode, parentSchemaMock);
                         ^
  required: String
  found:    String,ValidationContext,JsonNode,JsonSchema
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:292: error: method invokeDoCreateAndCaptureRawPath in class JsonSchemaFactory_create_Test cannot be applied to given types;
        String rawPath = invokeDoCreateAndCaptureRawPath(schemaPath, validationContext, emptyNode, parentSchemaMock);
                         ^
  required: String
  found:    String,ValidationContext,JsonNode,JsonSchema
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:299: error: incompatible types: DummyParentSchemaWithNullUri cannot be converted to JsonSchema
        JsonSchema parentWithNullUri = new DummyParentSchemaWithNullUri();
                                       ^
32 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\t\nclass `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `public JsonSchema create(ValidationContext, String, JsonNode, JsonSchema)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.create(ValidationContext, String, JsonNode, JsonSchema)`;\n\t\tmethod `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.create(ValidationContext, String, JsonNode, JsonSchema)`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static URI determineSchemaUri(URIFactory, JsonSchema, String)` returns `java.net.URI`, related with `com.networknt.schema.JsonSchemaFactory.create(ValidationContext, String, JsonNode, JsonSchema)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.create(ValidationContext, String, JsonNode, JsonSchema)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


