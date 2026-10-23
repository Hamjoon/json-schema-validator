
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
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

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

    /**
     * Simple concrete {@link JsonSchema} implementation used as a parent schema stub.
     */
    private static class SimpleParentSchema extends JsonSchema {

        private final URI currentUri;

        SimpleParentSchema(URI currentUri) {
            super(null, null, null, currentUri, null);
            this.currentUri = currentUri;
        }

        @Override
        public URI getCurrentUri() {
            return currentUri;
        }

        // All other abstract methods are not needed for these tests.
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
        // Use the public factory method provided by the library.
        factory = JsonSchemaFactory.getInstance();
    }

    @BeforeEach
    void setupBeforeEach() {
        parentUri = URI.create("http://example.com/parent");
        parentSchemaMock = new SimpleParentSchema(parentUri);
        emptyNode = new ObjectMapper().createObjectNode();
        // Obtain a meta‑schema instance from the factory for the ValidationContext.
        JsonMetaSchema metaSchema = factory.getMetaSchemaByUri(DEFAULT_META_SCHEMA_URI);
        validationContext = new ValidationContext(factory.getUriFactory(), factory.urnFactory, metaSchema, factory, null);
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
        JsonSchema result = factory.create(validationContext, null, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema instance");
    }

    @Test
    void create_WithExplicitSchemaPath_PassesItThrough() {
        String schemaPath = "/my/custom/path";
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema instance");
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
        JsonNode node = null;
        JsonSchema result = factory.create(validationContext, schemaPath, node, parentSchemaMock);
        assertNotNull(result, "Factory should still return a JsonSchema instance");
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
    }

    @Test
    void create_WithNonNullValidationContext_ForwardsSameInstance() {
        String schemaPath = "/valid/path";
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema");
    }

    /* --------------------------------------------------------------------- *
     *  5. Empty schemaPath handling
     * --------------------------------------------------------------------- */
    @Test
    void create_WithEmptySchemaPath_ForwardsUnchanged() {
        String schemaPath = "";
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema instance");
    }

    @Test
    void create_WithParentSchemaHavingNullCurrentUri_ForwardsNullUri() {
        String schemaPath = "/any";
        JsonSchema parentWithNullUri = new SimpleParentSchema(null);
        JsonSchema result = factory.create(validationContext, schemaPath, emptyNode, parentWithNullUri);
        assertNotNull(result, "Factory should still create a JsonSchema");
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
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:17: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:86: error: interface expected here
    private static class DummyParentSchemaWithNullUri implements JsonSchema {
                                                                 ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:125: error: cannot find symbol
        public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory) {
                                                                                 ^
  symbol:   class JsonValidatorFactory
  location: class DummyParentSchemaWithNullUri
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:130: error: cannot find symbol
        public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory, SchemaValidatorsConfig config) {
                                                                                 ^
  symbol:   class JsonValidatorFactory
  location: class DummyParentSchemaWithNullUri
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:199: error: cannot find symbol
        public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory) {
                                                                                 ^
  symbol:   class JsonValidatorFactory
  location: class SimpleParentSchema
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:204: error: cannot find symbol
        public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory, SchemaValidatorsConfig config) {
                                                                                 ^
  symbol:   class JsonValidatorFactory
  location: class SimpleParentSchema
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:20: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:55: error: JsonSchemaFactory(ObjectMapper,YAMLMapper,String,URISchemeFactory,URISchemeFetcher,URNFactory,Map<String,JsonMetaSchema>,Map<String,String>,boolean,CompositeURITranslator) has private access in JsonSchemaFactory
            super(jsonMapper, yamlMapper, defaultMetaSchemaURI, uriFactory, uriFetcher, urnFactory, jsonMetaSchemas, uriMap, enableUriSchemaCache, uriTranslators);
            ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:65: error: cannot find symbol
            return mock(JsonSchema.class);
                   ^
  symbol:   method mock(Class<JsonSchema>)
  location: class TestableJsonSchemaFactory
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:88: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:94: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:99: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:104: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:109: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:114: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:119: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:124: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:129: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:134: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:139: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:144: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:158: error: incompatible types: URI cannot be converted to JsonNode
            super(null, null, null, currentUri, null);
                                    ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:168: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:183: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:188: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:193: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:198: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:203: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:208: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:213: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:218: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:227: error: incompatible types: JsonSchemaFactory cannot be converted to TestableJsonSchemaFactory
        factory = JsonSchemaFactory.getInstance();
                                               ^
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:236: error: cannot find symbol
        JsonMetaSchema metaSchema = factory.getMetaSchemaByUri(DEFAULT_META_SCHEMA_URI);
                                           ^
  symbol:   method getMetaSchemaByUri(String)
  location: variable factory of type TestableJsonSchemaFactory
src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java:237: error: urnFactory has private access in JsonSchemaFactory
        validationContext = new ValidationContext(factory.getUriFactory(), factory.urnFactory, metaSchema, factory, null);
                                                                                  ^
Note: src/test/java/com/networknt/schema/JsonSchemaFactory_create_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
34 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\t\nclass `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `public JsonSchema create(ValidationContext, String, JsonNode, JsonSchema)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.create(ValidationContext, String, JsonNode, JsonSchema)`;\n\t\tmethod `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.create(ValidationContext, String, JsonNode, JsonSchema)`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static URI determineSchemaUri(URIFactory, JsonSchema, String)` returns `java.net.URI`, related with `com.networknt.schema.JsonSchemaFactory.create(ValidationContext, String, JsonNode, JsonSchema)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.create(ValidationContext, String, JsonNode, JsonSchema)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


