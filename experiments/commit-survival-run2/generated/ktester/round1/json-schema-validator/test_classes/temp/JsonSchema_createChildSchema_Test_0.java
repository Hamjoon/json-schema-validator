package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_createChildSchema_Test {

    private static final String ROOT_SCHEMA_PATH = "root";

    @Mock
    private ValidationContext // mock injected by Mockito
    validationContext;

    @Mock
    private JsonMetaSchema // required by ValidationContext#getMetaSchema()
    metaSchema;

    @Mock
    private JsonSchema // result of newSchema(...)
    mockChildSchema;

    @Mock
    private JsonNode // schemaNode supplied to createChildSchema
    childNode;

    // instance under test
    private JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Stub the ValidationContext behaviour used in the JsonSchema constructor.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // config can be null – the constructor checks for null before using it.
        when(validationContext.getConfig()).thenReturn(null);
        // Prepare a minimal schema node required by the private constructor.
        ObjectNode dummyRootNode = new ObjectNode(JsonNodeFactory.instance);
        // Use reflection to invoke the private JsonSchema constructor.
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, String.class, URI.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        jsonSchema = ctor.newInstance(validationContext, ROOT_SCHEMA_PATH, // currentUri
        null, // schemaNode
        dummyRootNode, // parent
        null, // suppressSubSchemaRetrieval
        false);
        // Stub the `newSchema` method that createChildSchema delegates to.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockChildSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions for a clean next test.
        reset(validationContext, mockChildSchema, childNode);
    }

    private static Stream<Arguments> provideSimpleArgumentVariations() {
        JsonNodeFactory f = JsonNodeFactory.instance;
        return Stream.of(Arguments.of("#/properties/name", f.objectNode().set("type", f.textNode("string")), "Valid ObjectNode schemaNode"), Arguments.of(null, f.objectNode().set("type", f.textNode("integer")), "Null schemaPath should be forwarded unchanged"), Arguments.of("#/properties/age", null, "Null schemaNode should be forwarded unchanged"), Arguments.of(null, null, "Both arguments null should be forwarded unchanged"), Arguments.of("", f.objectNode().set("type", f.textNode("string")), "Empty schemaPath should be forwarded unchanged"), Arguments.of("   ", f.objectNode().set("type", f.textNode("integer")), "Whitespace schemaPath should be forwarded unchanged"));
    }

    @Test
    void createChildSchema_delegatesToValidationContext_andReturnsChild() {
        // GIVEN – the parameters for the call
        String childPath = "childPath";
        // WHEN
        JsonSchema result = jsonSchema.createChildSchema(childPath, childNode);
        // THEN
        assertSame(mockChildSchema, result, "createChildSchema should return the schema produced by ValidationContext");
        verify(validationContext, times(1)).newSchema(eq(childPath), eq(childNode), eq(jsonSchema));
        verifyNoMoreInteractions(validationContext);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for simple argument variations
     * -------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("provideSimpleArgumentVariations")
    void createChildSchema_variousArguments(String schemaPath, JsonNode schemaNode, String description) {
        // Act
        JsonSchema result = jsonSchema.createChildSchema(schemaPath, schemaNode);
        // Assert
        assertSame(mockChildSchema, result, description);
        verify(validationContext, times(1)).newSchema(eq(schemaPath), eq(schemaNode), eq(jsonSchema));
        verifyNoMoreInteractions(validationContext);
        // reset for next iteration
        reset(validationContext);
        // Re‑stub default behaviour for subsequent calls
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockChildSchema);
    }

    /* --------------------------------------------------------------
     *  Tests for specific JsonNode sub‑types
     * -------------------------------------------------------------- */
    @Test
    void createChildSchema_arrayNodeSchemaNode() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().set("type", JsonNodeFactory.instance.textNode("string"))).add(JsonNodeFactory.instance.objectNode().set("type", JsonNodeFactory.instance.textNode("number")));
        JsonSchema result = jsonSchema.createChildSchema("#/items", arrayNode);
        assertSame(mockChildSchema, result);
        verify(validationContext).newSchema(eq("#/items"), eq(arrayNode), eq(jsonSchema));
    }

    @Test
    void createChildSchema_textNodeSchemaNode() {
        TextNode textNode = JsonNodeFactory.instance.textNode("just a string");
        JsonSchema result = jsonSchema.createChildSchema("#/description", textNode);
        assertSame(mockChildSchema, result);
        verify(validationContext).newSchema(eq("#/description"), eq(textNode), eq(jsonSchema));
    }

    @Test
    void createChildSchema_booleanNodeSchemaNode() {
        BooleanNode boolNode = BooleanNode.TRUE;
        JsonSchema result = jsonSchema.createChildSchema("#/flag", boolNode);
        assertSame(mockChildSchema, result);
        verify(validationContext).newSchema(eq("#/flag"), eq(boolNode), eq(jsonSchema));
    }

    @Test
    void createChildSchema_complexNestedSchemaNode() {
        ObjectNode complex = JsonNodeFactory.instance.objectNode().put("type", "object").set("properties", JsonNodeFactory.instance.objectNode().set("name", JsonNodeFactory.instance.objectNode().put("type", "string")).set("age", JsonNodeFactory.instance.objectNode().put("type", "integer")).set("address", JsonNodeFactory.instance.objectNode().put("type", "object").set("properties", JsonNodeFactory.instance.objectNode().set("city", JsonNodeFactory.instance.objectNode().put("type", "string")))));
        JsonSchema result = jsonSchema.createChildSchema("#/definitions/person", complex);
        assertSame(mockChildSchema, result);
        verify(validationContext).newSchema(eq("#/definitions/person"), eq(complex), eq(jsonSchema));
    }

    /* --------------------------------------------------------------
     *  Exceptional / edge cases
     * -------------------------------------------------------------- */
    @Test
    void createChildSchema_validationContextReturnsNull() {
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(null);
        JsonSchema result = jsonSchema.createChildSchema("#/properties/email", JsonNodeFactory.instance.objectNode().put("format", "email"));
        assertNull(result, "When ValidationContext returns null, createChildSchema should propagate the null");
    }

    @Test
    void createChildSchema_validationContextThrowsRuntimeException() {
        RuntimeException rte = new RuntimeException("unexpected error");
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenThrow(rte);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> jsonSchema.createChildSchema("#/properties/invalid", JsonNodeFactory.instance.objectNode().put("type", "unknown")), "Exception from ValidationContext must propagate");
        assertSame(rte, thrown);
    }

    @Test
    void createChildSchema_validationContextReturnsSameInstanceAsParent() {
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(// return the parent itself
        jsonSchema);
        JsonSchema result = jsonSchema.createChildSchema("#/properties/loop", JsonNodeFactory.instance.objectNode().put("$ref", "#"));
        assertSame(jsonSchema, result, "Returned instance should be exactly what ValidationContext provides");
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

    @Test
    void createChildSchema_invalidSchemaPathThrowsIllegalArgumentException() {
        IllegalArgumentException iae = new IllegalArgumentException("Invalid URI");
        when(validationContext.newSchema(eq("ht!tp://[invalid]"), any(JsonNode.class), any(JsonSchema.class))).thenThrow(iae);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> jsonSchema.createChildSchema("ht!tp://[invalid]", JsonNodeFactory.instance.objectNode().put("type", "string")), "Malformed schemaPath should cause IllegalArgumentException");
        assertSame(iae, thrown);
    }

    @Test
    void createChildSchema_validationContextThrowsIllegalArgumentException() {
        IllegalArgumentException iae = new IllegalArgumentException("Unsupported type");
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenThrow(iae);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> jsonSchema.createChildSchema("#/properties/invalid", JsonNodeFactory.instance.objectNode().put("type", "unknown")), "IllegalArgumentException from ValidationContext must propagate");
        assertSame(iae, thrown);
    }

    @Test
    void createChildSchema_validationContextThrowsIllegalStateException() {
        IllegalStateException ise = new IllegalStateException("Missing required 'type'");
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenThrow(ise);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> jsonSchema.createChildSchema("#/properties/missingRequired", JsonNodeFactory.instance.objectNode().set("properties", JsonNodeFactory.instance.objectNode().set("name", JsonNodeFactory.instance.objectNode().put("type", "string")))), "IllegalStateException from ValidationContext must propagate");
        assertSame(ise, thrown);
    }
}
