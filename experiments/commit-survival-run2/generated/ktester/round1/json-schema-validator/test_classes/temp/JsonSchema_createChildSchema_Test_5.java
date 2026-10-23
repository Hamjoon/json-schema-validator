package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonSchema.createChildSchema tests")
class JsonSchema_createChildSchema_Test {

    private static final String ROOT_SCHEMA_PATH = "root";

    //     @Mock
    private ValidationContext // mock injected by Mockito
    validationContext;

    //     @Mock
    private JsonMetaSchema // required by ValidationContext#getMetaSchema()
    metaSchema;

    //     @Mock
    private JsonSchema // result of newSchema(...)
    mockChildSchema;

    //     @Mock
    private JsonNode // schemaNode supplied to createChildSchema
    childNode;

    // instance under test
    private JsonSchema jsonSchema;

    /**
     * Path of this schema relative to the root schema.
     */
    private final String schemaPath;

    /**
     * URI of the schema (may be {@code null}).
     */
    private final URI uri;

    /**
     * The JSON node that represents this schema.
     */
    private final JsonNode schemaNode;

    /**
     * Parent schema – may be {@code null} for the root schema.
     */
    private final JsonSchema parentSchema;

    /**
     * Flag used by the original library (kept for compatibility).
     */
    private final boolean someFlag;

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
        return Stream.of(Arguments.of("#/properties/name", f.objectNode().put("type", "string"), "Valid ObjectNode schemaNode"), Arguments.of(null, f.objectNode().put("type", "integer"), "Null schemaPath should be forwarded unchanged"), Arguments.of("#/properties/age", null, "Null schemaNode should be forwarded unchanged"), Arguments.of(null, null, "Both arguments null should be forwarded unchanged"), Arguments.of("", f.objectNode().put("type", "string"), "Empty schemaPath should be forwarded unchanged"), Arguments.of("   ", f.objectNode().put("type", "integer"), "Whitespace schemaPath should be forwarded unchanged"));
    }

    /**
     * Utility that creates a {@link JsonSchema} instance via the private
     * constructor using reflection.
     */
    private static JsonSchema instantiateJsonSchema(ValidationContext ctx) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, String.class, URI.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // Minimal arguments – the actual values are not used by the tests.
        ObjectNode dummyRootNode = new ObjectNode(JsonNodeFactory.instance);
        return ctor.newInstance(ctx, ROOT_SCHEMA_PATH, null, dummyRootNode, null, false);
    }

    /**
     * Creates a child {@link JsonSchema} using the internal
     * {@link ValidationContext}.  The method forwards the supplied
     * {@code schemaPath} and {@code schemaNode} unchanged to the context.
     *
     * @param schemaPath the JSON‑Pointer path for the child schema (may be
     *                   {@code null} or empty).
     * @param schemaNode the {@link JsonNode} that defines the child schema.
     * @return the child {@code JsonSchema} produced by the {@code ValidationContext}.
     * @throws NullPointerException if the internal {@code validationContext}
     *                              is {@code null}.
     */
    public JsonSchema createChildSchema(String schemaPath, JsonNode schemaNode) {
        if (validationContext == null) {
            // The test expects a NPE with a non‑null message.
            throw new NullPointerException("validationContext is null");
        }
        // Delegate to the ValidationContext – it records the parameters for the
        // test's dummy implementation.
        return validationContext.newSchema(schemaPath, schemaNode, this);
    }

    /**
     * Helper used by the test class to instantiate a {@code JsonSchema}
     * via the private constructor.
     *
     * @param ctx the {@link ValidationContext} (may be {@code null}).
     * @return a new {@code JsonSchema} instance.
     */
    static JsonSchema instantiate(ValidationContext ctx) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, String.class, URI.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // Minimal dummy node – the actual content is irrelevant for the tests.
        JsonNode dummyRootNode = new com.fasterxml.jackson.databind.node.ObjectNode(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance);
        return ctor.newInstance(ctx, JsonSchema_createChildSchema_Test.ROOT_SCHEMA_PATH, null, dummyRootNode, null, false);
    }

    public ValidationContext getValidationContext() {
        return validationContext;
    }

    public String getSchemaPath() {
        return schemaPath;
    }

    public URI getUri() {
        return uri;
    }

    public JsonNode getSchemaNode() {
        return schemaNode;
    }

    public JsonSchema getParentSchema() {
        return parentSchema;
    }

    public boolean isSomeFlag() {
        return someFlag;
    }

    @Test
    void createChildSchema_nullValidationContext() throws Exception {
        // Use reflection to set the internal ValidationContext to null
        Field ctxField = JsonSchema.class.getDeclaredField("validationContext");
        ctxField.setAccessible(true);
        ctxField.set(jsonSchema, null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> jsonSchema.createChildSchema("#/properties/name", JsonNodeFactory.instance.objectNode().put("type", "string")), "Calling createChildSchema with a null ValidationContext should throw NPE");
        assertNotNull(npe.getMessage());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof JsonSchema))
            return false;
        JsonSchema that = (JsonSchema) o;
        return someFlag == that.someFlag && Objects.equals(validationContext, that.validationContext) && Objects.equals(schemaPath, that.schemaPath) && Objects.equals(uri, that.uri) && Objects.equals(schemaNode, that.schemaNode) && Objects.equals(parentSchema, that.parentSchema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validationContext, schemaPath, uri, schemaNode, parentSchema, someFlag);
    }
}
