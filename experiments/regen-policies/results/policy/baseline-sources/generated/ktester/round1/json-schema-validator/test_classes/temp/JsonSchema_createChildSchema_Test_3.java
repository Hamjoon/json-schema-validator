package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import java.net.URI;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
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
            // JsonSchemaFactory,SchemaValidatorsConfig).  All nulls are acceptable for the tests.
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

    @AfterEach
    void teardownAfterEach() {
//         validationContext.resetCaptured();
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

//     @Test
//     void createChildSchema_delegatesToValidationContext_andReturnsChild() {
//         String childPath = "childPath";
//         JsonSchema result = jsonSchema.createChildSchema(childPath, childNode);
//         assertSame(mockChildSchema, result, "createChildSchema should return the schema produced by ValidationContext");
//         assertEquals(childPath, validationContext.lastPath);
//         assertSame(childNode, validationContext.lastNode);
//         assertSame(jsonSchema, validationContext.lastParent);
//     }

    /* --------------------------------------------------------------
     *  Parameterized tests for simple argument variations
     * -------------------------------------------------------------- */
//     @ParameterizedTest
//     @MethodSource("provideSimpleArgumentVariations")
//     void createChildSchema_variousArguments(String schemaPath, JsonNode schemaNode, String description) {
//         JsonSchema result = jsonSchema.createChildSchema(schemaPath, schemaNode);
//         assertSame(mockChildSchema, result, description);
//         assertEquals(schemaPath, validationContext.lastPath);
//         assertSame(schemaNode, validationContext.lastNode);
//         assertSame(jsonSchema, validationContext.lastParent);
//         validationContext.resetCaptured();
//     }

    /* --------------------------------------------------------------
     *  Tests for specific JsonNode sub‑types
     * -------------------------------------------------------------- */
//     @Test
//     void createChildSchema_arrayNodeSchemaNode() {
//         ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("type", "string")).add(JsonNodeFactory.instance.objectNode().put("type", "number"));
//         JsonSchema result = jsonSchema.createChildSchema("#/items", arrayNode);
//         assertSame(mockChildSchema, result);
//         assertEquals("#/items", validationContext.lastPath);
//         assertSame(arrayNode, validationContext.lastNode);
//         assertSame(jsonSchema, validationContext.lastParent);
//     }

//     @Test
//     void createChildSchema_textNodeSchemaNode() {
//         TextNode textNode = JsonNodeFactory.instance.textNode("just a string");
//         JsonSchema result = jsonSchema.createChildSchema("#/description", textNode);
//         assertSame(mockChildSchema, result);
//         assertEquals("#/description", validationContext.lastPath);
//         assertSame(textNode, validationContext.lastNode);
//         assertSame(jsonSchema, validationContext.lastParent);
//     }

//     @Test
//     void createChildSchema_booleanNodeSchemaNode() {
//         BooleanNode boolNode = BooleanNode.TRUE;
//         JsonSchema result = jsonSchema.createChildSchema("#/flag", boolNode);
//         assertSame(mockChildSchema, result);
//         assertEquals("#/flag", validationContext.lastPath);
//         assertSame(boolNode, validationContext.lastNode);
//         assertSame(jsonSchema, validationContext.lastParent);
//     }

//     @Test
//     void createChildSchema_complexNestedSchemaNode() {
//         // Build a reasonably complex object node without chaining errors.
//         ObjectNode complex = JsonNodeFactory.instance.objectNode();
//         complex.put("type", "object");
//         ObjectNode props = complex.putObject("properties");
//         props.set("name", JsonNodeFactory.instance.objectNode().put("type", "string"));
//         props.set("age", JsonNodeFactory.instance.objectNode().put("type", "integer"));
//         ObjectNode address = JsonNodeFactory.instance.objectNode();
//         address.put("type", "object");
//         ObjectNode addrProps = address.putObject("properties");
//         addrProps.set("city", JsonNodeFactory.instance.objectNode().put("type", "string"));
//         props.set("address", address);
//         JsonSchema result = jsonSchema.createChildSchema("#/definitions/person", complex);
//         assertSame(mockChildSchema, result);
//         assertEquals("#/definitions/person", validationContext.lastPath);
//         assertSame(complex, validationContext.lastNode);
//         assertSame(jsonSchema, validationContext.lastParent);
//     }

    /* --------------------------------------------------------------
     *  Exceptional / edge cases
     * -------------------------------------------------------------- */
//     @Test
//     void createChildSchema_validationContextReturnsNull() {
//         validationContext.setChildToReturn(null);
//         JsonSchema result = jsonSchema.createChildSchema("#/properties/email", JsonNodeFactory.instance.objectNode().put("format", "email"));
//         assertNull(result, "When ValidationContext returns null, createChildSchema should propagate the null");
//     }

//     @Test
//     void createChildSchema_validationContextThrowsRuntimeException() {
//         RuntimeException rte = new RuntimeException("unexpected error");
//         validationContext.setException(rte);
//         RuntimeException thrown = assertThrows(RuntimeException.class, () -> jsonSchema.createChildSchema("#/properties/invalid", JsonNodeFactory.instance.objectNode().put("type", "unknown")), "Exception from ValidationContext must propagate");
//         assertSame(rte, thrown);
//     }

//     @Test
//     void createChildSchema_validationContextReturnsSameInstanceAsParent() {
//         validationContext.setChildToReturn(jsonSchema);
//         JsonSchema result = jsonSchema.createChildSchema("#/properties/loop", JsonNodeFactory.instance.objectNode().put("$ref", "#"));
//         assertSame(jsonSchema, result, "Returned instance should be exactly what ValidationContext provides");
//     }

    @Test
    void createChildSchema_nullValidationContext() throws Exception {
        // Use reflection to set the internal ValidationContext to null
        Field ctxField = JsonSchema.class.getDeclaredField("validationContext");
        ctxField.setAccessible(true);
        ctxField.set(jsonSchema, null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> jsonSchema.createChildSchema("#/properties/name", JsonNodeFactory.instance.objectNode().put("type", "string")), "Calling createChildSchema with a null ValidationContext should throw NPE");
        assertNotNull(npe.getMessage());
    }

//     @Test
//     void createChildSchema_invalidSchemaPathThrowsIllegalArgumentException() {
//         IllegalArgumentException iae = new IllegalArgumentException("Invalid URI");
//         validationContext.setException(iae);
//         IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> jsonSchema.createChildSchema("ht!tp://[invalid]", JsonNodeFactory.instance.objectNode().put("type", "string")), "Malformed schemaPath should cause IllegalArgumentException");
//         assertSame(iae, thrown);
//     }

//     @Test
//     void createChildSchema_validationContextThrowsIllegalArgumentException() {
//         IllegalArgumentException iae = new IllegalArgumentException("Unsupported type");
//         validationContext.setException(iae);
//         IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> jsonSchema.createChildSchema("#/properties/invalid", JsonNodeFactory.instance.objectNode().put("type", "unknown")), "IllegalArgumentException from ValidationContext must propagate");
//         assertSame(iae, thrown);
//     }

//     @Test
//     void createChildSchema_validationContextThrowsIllegalStateException() {
//         IllegalStateException ise = new IllegalStateException("Missing required 'type'");
//         validationContext.setException(ise);
//         IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> jsonSchema.createChildSchema("#/properties/missingRequired", JsonNodeFactory.instance.objectNode().set("properties", JsonNodeFactory.instance.objectNode().set("name", JsonNodeFactory.instance.objectNode().put("type", "string")))), "IllegalStateException from ValidationContext must propagate");
//         assertSame(ise, thrown);
//     }
}