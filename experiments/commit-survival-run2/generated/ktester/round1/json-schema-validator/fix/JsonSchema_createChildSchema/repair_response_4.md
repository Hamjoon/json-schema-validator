```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonSchema.createChildSchema tests")
class JsonSchema_createChildSchema_Test {

    private static final String ROOT_SCHEMA_PATH = "root";

    // @Mock
    private ValidationContext validationContext; // mock injected by Mockito

    // @Mock
    private JsonMetaSchema metaSchema; // required by ValidationContext#getMetaSchema()

    // @Mock
    private JsonSchema mockChildSchema; // result of newSchema(...)

    // @Mock
    private JsonNode childNode; // schemaNode supplied to createChildSchema

    // instance under test
    private JsonSchema jsonSchema;

    /**
     * Dummy ValidationContext that records the parameters passed to
     * {@link ValidationContext#newSchema(String, JsonNode, JsonSchema)}.
     */
    private static class DummyValidationContext extends ValidationContext {

        JsonSchema childToReturn;
        RuntimeException toThrow;
        String lastPath;
        JsonNode lastNode;
        JsonSchema lastParent;

        DummyValidationContext(JsonSchema childToReturn) {
            // ValidationContext constructor (URIFactory,URNFactory,JsonMetaSchema,
            // JsonSchemaFactory,SchemaValidatorsConfig). All nulls are acceptable for the tests.
            super(null, null, null, null, null);
            this.childToReturn = childToReturn;
        }

        void setChildToReturn(JsonSchema child) {
            this.childToReturn = child;
        }

        void setException(RuntimeException e) {
            this.toThrow = e;
        }

        void resetCaptured() {
            lastPath = null;
            lastNode = null;
            lastParent = null;
        }

        @Override
        public JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
            this.lastPath = path;
            this.lastNode = node;
            this.lastParent = parent;
            if (toThrow != null) {
                throw toThrow;
            }
            return childToReturn;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // a dummy child schema that will be returned by the stubbed ValidationContext
        mockChildSchema = instantiateJsonSchema(null);
        // any node works
        childNode = JsonNodeFactory.instance.objectNode();
        validationContext = new DummyValidationContext(mockChildSchema);
        jsonSchema = instantiateJsonSchema(validationContext);
    }

    private static Stream<Arguments> provideSimpleArgumentVariations() {
        JsonNodeFactory f = JsonNodeFactory.instance;
        return Stream.of(
                Arguments.of("#/properties/name", f.objectNode().put("type", "string"), "Valid ObjectNode schemaNode"),
                Arguments.of(null, f.objectNode().put("type", "integer"), "Null schemaPath should be forwarded unchanged"),
                Arguments.of("#/properties/age", null, "Null schemaNode should be forwarded unchanged"),
                Arguments.of(null, null, "Both arguments null should be forwarded unchanged"),
                Arguments.of("", f.objectNode().put("type", "string"), "Empty schemaPath should be forwarded unchanged"),
                Arguments.of("   ", f.objectNode().put("type", "integer"), "Whitespace schemaPath should be forwarded unchanged")
        );
    }

    /**
     * Utility that creates a {@link JsonSchema} instance via the private
     * constructor using reflection.
     */
    private static JsonSchema instantiateJsonSchema(ValidationContext ctx) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class, String.class, URI.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // Minimal arguments – the actual values are not used by the tests.
        ObjectNode dummyRootNode = new ObjectNode(JsonNodeFactory.instance);
        return ctor.newInstance(ctx, ROOT_SCHEMA_PATH, null, dummyRootNode, null, false);
    }

    @Test
    void createChildSchema_nullValidationContext() throws Exception {
        // Use reflection to set the internal ValidationContext to null
        Field ctxField = JsonSchema.class.getDeclaredField("validationContext");
        ctxField.setAccessible(true);
        ctxField.set(jsonSchema, null);

        NullPointerException npe = assertThrows(
                NullPointerException.class,
                () -> jsonSchema.createChildSchema(
                        "#/properties/name",
                        JsonNodeFactory.instance.objectNode().put("type", "string")),
                "Calling createChildSchema with a null ValidationContext should throw NPE"
        );
        assertNotNull(npe.getMessage());
    }
}
```