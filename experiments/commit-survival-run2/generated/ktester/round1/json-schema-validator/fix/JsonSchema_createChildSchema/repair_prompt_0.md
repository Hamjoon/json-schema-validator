
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.params.provider.Arguments;

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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:6: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:7: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:11: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:17: error: package org.mockito does not exist
import static org.mockito.ArgumentMatchers.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:18: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:19: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:22: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:31: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:35: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:39: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:49: error: cannot find symbol
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:51: error: cannot find symbol
        when(validationContext.getConfig()).thenReturn(null);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:63: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockChildSchema);
                                         ^
  symbol:   method anyString()
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:63: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockChildSchema);
                                                      ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:63: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockChildSchema);
                                                                           ^
  symbol:   method any(Class<JsonSchema>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:69: error: cannot find symbol
        reset(validationContext, mockChildSchema, childNode);
        ^
  symbol:   method reset(ValidationContext,JsonSchema,JsonNode)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:85: error: cannot find symbol
        verify(validationContext, times(1)).newSchema(eq(childPath), eq(childNode), eq(jsonSchema));
                                  ^
  symbol:   method times(int)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:85: error: cannot find symbol
        verify(validationContext, times(1)).newSchema(eq(childPath), eq(childNode), eq(jsonSchema));
                                                      ^
  symbol:   method eq(String)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:85: error: cannot find symbol
        verify(validationContext, times(1)).newSchema(eq(childPath), eq(childNode), eq(jsonSchema));
                                                                     ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:85: error: cannot find symbol
        verify(validationContext, times(1)).newSchema(eq(childPath), eq(childNode), eq(jsonSchema));
                                                                                    ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:86: error: cannot find symbol
        verifyNoMoreInteractions(validationContext);
        ^
  symbol:   method verifyNoMoreInteractions(ValidationContext)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:99: error: cannot find symbol
        verify(validationContext, times(1)).newSchema(eq(schemaPath), eq(schemaNode), eq(jsonSchema));
                                  ^
  symbol:   method times(int)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:99: error: cannot find symbol
        verify(validationContext, times(1)).newSchema(eq(schemaPath), eq(schemaNode), eq(jsonSchema));
                                                      ^
  symbol:   method eq(String)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:99: error: cannot find symbol
        verify(validationContext, times(1)).newSchema(eq(schemaPath), eq(schemaNode), eq(jsonSchema));
                                                                      ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:99: error: cannot find symbol
        verify(validationContext, times(1)).newSchema(eq(schemaPath), eq(schemaNode), eq(jsonSchema));
                                                                                      ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:100: error: cannot find symbol
        verifyNoMoreInteractions(validationContext);
        ^
  symbol:   method verifyNoMoreInteractions(ValidationContext)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:102: error: cannot find symbol
        reset(validationContext);
        ^
  symbol:   method reset(ValidationContext)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:104: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockChildSchema);
                                         ^
  symbol:   method anyString()
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:104: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockChildSchema);
                                                      ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:104: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockChildSchema);
                                                                           ^
  symbol:   method any(Class<JsonSchema>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:115: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/items"), eq(arrayNode), eq(jsonSchema));
        ^
  symbol:   method verify(ValidationContext)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:115: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/items"), eq(arrayNode), eq(jsonSchema));
                                            ^
  symbol:   method eq(String)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:115: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/items"), eq(arrayNode), eq(jsonSchema));
                                                           ^
  symbol:   method eq(ArrayNode)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:115: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/items"), eq(arrayNode), eq(jsonSchema));
                                                                          ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:123: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/description"), eq(textNode), eq(jsonSchema));
        ^
  symbol:   method verify(ValidationContext)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:123: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/description"), eq(textNode), eq(jsonSchema));
                                            ^
  symbol:   method eq(String)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:123: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/description"), eq(textNode), eq(jsonSchema));
                                                                 ^
  symbol:   method eq(TextNode)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:123: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/description"), eq(textNode), eq(jsonSchema));
                                                                               ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:131: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/flag"), eq(boolNode), eq(jsonSchema));
        ^
  symbol:   method verify(ValidationContext)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:131: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/flag"), eq(boolNode), eq(jsonSchema));
                                            ^
  symbol:   method eq(String)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:131: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/flag"), eq(boolNode), eq(jsonSchema));
                                                          ^
  symbol:   method eq(BooleanNode)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:131: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/flag"), eq(boolNode), eq(jsonSchema));
                                                                        ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:136: error: cannot find symbol
        ObjectNode complex = JsonNodeFactory.instance.objectNode().put("type", "object").set("properties", JsonNodeFactory.instance.objectNode().set("name", JsonNodeFactory.instance.objectNode().put("type", "string")).set("age", JsonNodeFactory.instance.objectNode().put("type", "integer")).set("address", JsonNodeFactory.instance.objectNode().put("type", "object").set("properties", JsonNodeFactory.instance.objectNode().set("city", JsonNodeFactory.instance.objectNode().put("type", "string")))));
                                                                                                                                                                                                                         ^
  symbol:   method set(String,ObjectNode)
  location: class JsonNode
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:139: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/definitions/person"), eq(complex), eq(jsonSchema));
        ^
  symbol:   method verify(ValidationContext)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:139: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/definitions/person"), eq(complex), eq(jsonSchema));
                                            ^
  symbol:   method eq(String)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:139: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/definitions/person"), eq(complex), eq(jsonSchema));
                                                                        ^
  symbol:   method eq(ObjectNode)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:139: error: cannot find symbol
        verify(validationContext).newSchema(eq("#/definitions/person"), eq(complex), eq(jsonSchema));
                                                                                     ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:147: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(null);
                                         ^
  symbol:   method anyString()
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:147: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(null);
                                                      ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:147: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(null);
                                                                           ^
  symbol:   method any(Class<JsonSchema>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:155: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenThrow(rte);
                                         ^
  symbol:   method anyString()
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:155: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenThrow(rte);
                                                      ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:155: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenThrow(rte);
                                                                           ^
  symbol:   method any(Class<JsonSchema>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:162: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(// return the parent itself
                                         ^
  symbol:   method anyString()
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:162: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(// return the parent itself
                                                      ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:162: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(// return the parent itself
                                                                           ^
  symbol:   method any(Class<JsonSchema>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:181: error: cannot find symbol
        when(validationContext.newSchema(eq("ht!tp://[invalid]"), any(JsonNode.class), any(JsonSchema.class))).thenThrow(iae);
                                         ^
  symbol:   method eq(String)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:181: error: cannot find symbol
        when(validationContext.newSchema(eq("ht!tp://[invalid]"), any(JsonNode.class), any(JsonSchema.class))).thenThrow(iae);
                                                                  ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:181: error: cannot find symbol
        when(validationContext.newSchema(eq("ht!tp://[invalid]"), any(JsonNode.class), any(JsonSchema.class))).thenThrow(iae);
                                                                                       ^
  symbol:   method any(Class<JsonSchema>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:189: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenThrow(iae);
                                         ^
  symbol:   method anyString()
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:189: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenThrow(iae);
                                                      ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:189: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenThrow(iae);
                                                                           ^
  symbol:   method any(Class<JsonSchema>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:197: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenThrow(ise);
                                         ^
  symbol:   method anyString()
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:197: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenThrow(ise);
                                                      ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:197: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenThrow(ise);
                                                                           ^
  symbol:   method any(Class<JsonSchema>)
  location: class JsonSchema_createChildSchema_Test
66 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema createChildSchema(String, JsonNode)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.createChildSchema(String, JsonNode)`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static JsonSchemaRef getJsonSchemaRef(JsonSchema, ValidationContext, String, String)` returns `com.networknt.schema.JsonSchemaRef`, related with `com.networknt.schema.JsonSchema.createChildSchema(String, JsonNode)`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.createChildSchema(String, JsonNode)`;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.createChildSchema(String, JsonNode)`;\nclass `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.createChildSchema(String, JsonNode)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.createChildSchema(String, JsonNode)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


