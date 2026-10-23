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

@ExtendWith(org.junit.jupiter.api.extension.ExtendWith.class) // placeholder to keep original annotation structure
class JsonSchemaFactory_create_Test {

    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    private static JsonSchemaFactory factory;

    private ValidationContext validationContext;

    private JsonNode emptyNode;

    private JsonSchema parentSchemaMock;

    private URI parentUri;

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
        @Override public void setCurrentUri(URI currentUri) { throw new UnsupportedOperationException(); }
        @Override public JsonSchema getParentSchema() { throw new UnsupportedOperationException(); }
        @Override public JsonNode getSchemaNode() { throw new UnsupportedOperationException(); }
        @Override public JsonMetaSchema getMetaSchema() { throw new UnsupportedOperationException(); }
        @Override public void setMetaSchema(JsonMetaSchema metaSchema) { throw new UnsupportedOperationException(); }
        @Override public void validate(JsonNode node, ValidationContext validationContext) { throw new UnsupportedOperationException(); }
        @Override public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory) { throw new UnsupportedOperationException(); }
        @Override public void validate(JsonNode node, ValidationContext validationContext, JsonValidatorFactory validatorFactory, SchemaValidatorsConfig config) { throw new UnsupportedOperationException(); }
        @Override public JsonSchemaFactory getFactory() { throw new UnsupportedOperationException(); }
        @Override public String getType() { throw new UnsupportedOperationException(); }
        @Override public JsonSchema createDeepCopy() { throw new UnsupportedOperationException(); }
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
        validationContext = new ValidationContext(factory.getUriFactory(),
                factory.urnFactory, metaSchema, factory, null);
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

    @Test
    void create_WithNullParentSchema_ThrowsNullPointerException() {
        String schemaPath = "/any";
        JsonSchema nullParent = null;
        assertThrows(NullPointerException.class,
                () -> factory.create(validationContext, schemaPath, emptyNode, nullParent),
                "Calling create with a null parentSchema must result in NPE because getCurrentUri() is accessed");
    }

    @Test
    void create_WithNullSchemaNode_ForwardsNull() {
        String schemaPath = "/path";
        JsonNode node = null;
        JsonSchema result = factory.create(validationContext, schemaPath, node, parentSchemaMock);
        assertNotNull(result, "Factory should still return a JsonSchema instance");
    }

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

    @Test
    void create_WithComplexSchemaNode_ForwardsSameInstance() throws Exception {
        String schemaPath = "/complex";
        String json = "{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"name\":{\"type\":\"string\"},"
                + "   \"age\":{\"type\":\"integer\"}"
                + "}"
                + "}";
        JsonNode complexNode = new ObjectMapper().readTree(json);
        JsonSchema result = factory.create(validationContext, schemaPath, complexNode, parentSchemaMock);
        assertNotNull(result, "Factory should return a JsonSchema");
    }
}
```