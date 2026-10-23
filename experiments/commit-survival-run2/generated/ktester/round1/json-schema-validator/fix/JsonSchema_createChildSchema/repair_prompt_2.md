
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
import java.lang.reflect.Constructor;
import java.net.URI;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchema.createChildSchema tests")
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
        validationContext.resetCaptured();
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

    @Test
    void createChildSchema_delegatesToValidationContext_andReturnsChild() {
        String childPath = "childPath";
        JsonSchema result = jsonSchema.createChildSchema(childPath, childNode);
        assertSame(mockChildSchema, result, "createChildSchema should return the schema produced by ValidationContext");
        assertEquals(childPath, validationContext.lastPath);
        assertSame(childNode, validationContext.lastNode);
        assertSame(jsonSchema, validationContext.lastParent);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for simple argument variations
     * -------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("provideSimpleArgumentVariations")
    void createChildSchema_variousArguments(String schemaPath, JsonNode schemaNode, String description) {
        JsonSchema result = jsonSchema.createChildSchema(schemaPath, schemaNode);
        assertSame(mockChildSchema, result, description);
        assertEquals(schemaPath, validationContext.lastPath);
        assertSame(schemaNode, validationContext.lastNode);
        assertSame(jsonSchema, validationContext.lastParent);
        validationContext.resetCaptured();
    }

    /* --------------------------------------------------------------
     *  Tests for specific JsonNode sub‑types
     * -------------------------------------------------------------- */
    @Test
    void createChildSchema_arrayNodeSchemaNode() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode().add(JsonNodeFactory.instance.objectNode().put("type", "string")).add(JsonNodeFactory.instance.objectNode().put("type", "number"));
        JsonSchema result = jsonSchema.createChildSchema("#/items", arrayNode);
        assertSame(mockChildSchema, result);
        assertEquals("#/items", validationContext.lastPath);
        assertSame(arrayNode, validationContext.lastNode);
        assertSame(jsonSchema, validationContext.lastParent);
    }

    @Test
    void createChildSchema_textNodeSchemaNode() {
        TextNode textNode = JsonNodeFactory.instance.textNode("just a string");
        JsonSchema result = jsonSchema.createChildSchema("#/description", textNode);
        assertSame(mockChildSchema, result);
        assertEquals("#/description", validationContext.lastPath);
        assertSame(textNode, validationContext.lastNode);
        assertSame(jsonSchema, validationContext.lastParent);
    }

    @Test
    void createChildSchema_booleanNodeSchemaNode() {
        BooleanNode boolNode = BooleanNode.TRUE;
        JsonSchema result = jsonSchema.createChildSchema("#/flag", boolNode);
        assertSame(mockChildSchema, result);
        assertEquals("#/flag", validationContext.lastPath);
        assertSame(boolNode, validationContext.lastNode);
        assertSame(jsonSchema, validationContext.lastParent);
    }

    @Test
    void createChildSchema_complexNestedSchemaNode() {
        // Build a reasonably complex object node without chaining errors.
        ObjectNode complex = JsonNodeFactory.instance.objectNode();
        complex.put("type", "object");
        ObjectNode props = complex.putObject("properties");
        props.set("name", JsonNodeFactory.instance.objectNode().put("type", "string"));
        props.set("age", JsonNodeFactory.instance.objectNode().put("type", "integer"));
        ObjectNode address = JsonNodeFactory.instance.objectNode();
        address.put("type", "object");
        ObjectNode addrProps = address.putObject("properties");
        addrProps.set("city", JsonNodeFactory.instance.objectNode().put("type", "string"));
        props.set("address", address);
        JsonSchema result = jsonSchema.createChildSchema("#/definitions/person", complex);
        assertSame(mockChildSchema, result);
        assertEquals("#/definitions/person", validationContext.lastPath);
        assertSame(complex, validationContext.lastNode);
        assertSame(jsonSchema, validationContext.lastParent);
    }

    /* --------------------------------------------------------------
     *  Exceptional / edge cases
     * -------------------------------------------------------------- */
    @Test
    void createChildSchema_validationContextReturnsNull() {
        validationContext.setChildToReturn(null);
        JsonSchema result = jsonSchema.createChildSchema("#/properties/email", JsonNodeFactory.instance.objectNode().put("format", "email"));
        assertNull(result, "When ValidationContext returns null, createChildSchema should propagate the null");
    }

    @Test
    void createChildSchema_validationContextThrowsRuntimeException() {
        RuntimeException rte = new RuntimeException("unexpected error");
        validationContext.setException(rte);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> jsonSchema.createChildSchema("#/properties/invalid", JsonNodeFactory.instance.objectNode().put("type", "unknown")), "Exception from ValidationContext must propagate");
        assertSame(rte, thrown);
    }

    @Test
    void createChildSchema_validationContextReturnsSameInstanceAsParent() {
        validationContext.setChildToReturn(jsonSchema);
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
        validationContext.setException(iae);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> jsonSchema.createChildSchema("ht!tp://[invalid]", JsonNodeFactory.instance.objectNode().put("type", "string")), "Malformed schemaPath should cause IllegalArgumentException");
        assertSame(iae, thrown);
    }

    @Test
    void createChildSchema_validationContextThrowsIllegalArgumentException() {
        IllegalArgumentException iae = new IllegalArgumentException("Unsupported type");
        validationContext.setException(iae);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> jsonSchema.createChildSchema("#/properties/invalid", JsonNodeFactory.instance.objectNode().put("type", "unknown")), "IllegalArgumentException from ValidationContext must propagate");
        assertSame(iae, thrown);
    }

    @Test
    void createChildSchema_validationContextThrowsIllegalStateException() {
        IllegalStateException ise = new IllegalStateException("Missing required 'type'");
        validationContext.setException(ise);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> jsonSchema.createChildSchema("#/properties/missingRequired", JsonNodeFactory.instance.objectNode().set("properties", JsonNodeFactory.instance.objectNode().set("name", JsonNodeFactory.instance.objectNode().put("type", "string")))), "IllegalStateException from ValidationContext must propagate");
        assertSame(ise, thrown);
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:14: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:15: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:18: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:24: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:28: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:32: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:36: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_createChildSchema_Test
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:104: error: cannot find symbol
        validationContext.resetCaptured();
                         ^
  symbol:   method resetCaptured()
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:129: error: cannot find symbol
        assertEquals(childPath, validationContext.lastPath);
                                                 ^
  symbol:   variable lastPath
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:130: error: cannot find symbol
        assertSame(childNode, validationContext.lastNode);
                                               ^
  symbol:   variable lastNode
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:131: error: cannot find symbol
        assertSame(jsonSchema, validationContext.lastParent);
                                                ^
  symbol:   variable lastParent
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:142: error: cannot find symbol
        assertEquals(schemaPath, validationContext.lastPath);
                                                  ^
  symbol:   variable lastPath
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:143: error: cannot find symbol
        assertSame(schemaNode, validationContext.lastNode);
                                                ^
  symbol:   variable lastNode
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:144: error: cannot find symbol
        assertSame(jsonSchema, validationContext.lastParent);
                                                ^
  symbol:   variable lastParent
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:145: error: cannot find symbol
        validationContext.resetCaptured();
                         ^
  symbol:   method resetCaptured()
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:156: error: cannot find symbol
        assertEquals("#/items", validationContext.lastPath);
                                                 ^
  symbol:   variable lastPath
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:157: error: cannot find symbol
        assertSame(arrayNode, validationContext.lastNode);
                                               ^
  symbol:   variable lastNode
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:158: error: cannot find symbol
        assertSame(jsonSchema, validationContext.lastParent);
                                                ^
  symbol:   variable lastParent
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:166: error: cannot find symbol
        assertEquals("#/description", validationContext.lastPath);
                                                       ^
  symbol:   variable lastPath
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:167: error: cannot find symbol
        assertSame(textNode, validationContext.lastNode);
                                              ^
  symbol:   variable lastNode
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:168: error: cannot find symbol
        assertSame(jsonSchema, validationContext.lastParent);
                                                ^
  symbol:   variable lastParent
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:176: error: cannot find symbol
        assertEquals("#/flag", validationContext.lastPath);
                                                ^
  symbol:   variable lastPath
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:177: error: cannot find symbol
        assertSame(boolNode, validationContext.lastNode);
                                              ^
  symbol:   variable lastNode
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:178: error: cannot find symbol
        assertSame(jsonSchema, validationContext.lastParent);
                                                ^
  symbol:   variable lastParent
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:196: error: cannot find symbol
        assertEquals("#/definitions/person", validationContext.lastPath);
                                                              ^
  symbol:   variable lastPath
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:197: error: cannot find symbol
        assertSame(complex, validationContext.lastNode);
                                             ^
  symbol:   variable lastNode
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:198: error: cannot find symbol
        assertSame(jsonSchema, validationContext.lastParent);
                                                ^
  symbol:   variable lastParent
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:206: error: cannot find symbol
        validationContext.setChildToReturn(null);
                         ^
  symbol:   method setChildToReturn(<null>)
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:214: error: cannot find symbol
        validationContext.setException(rte);
                         ^
  symbol:   method setException(RuntimeException)
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:221: error: cannot find symbol
        validationContext.setChildToReturn(jsonSchema);
                         ^
  symbol:   method setChildToReturn(JsonSchema)
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:239: error: cannot find symbol
        validationContext.setException(iae);
                         ^
  symbol:   method setException(IllegalArgumentException)
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:247: error: cannot find symbol
        validationContext.setException(iae);
                         ^
  symbol:   method setException(IllegalArgumentException)
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/JsonSchema_createChildSchema_Test.java:255: error: cannot find symbol
        validationContext.setException(ise);
                         ^
  symbol:   method setException(IllegalStateException)
  location: variable validationContext of type ValidationContext
33 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema createChildSchema(String, JsonNode)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.createChildSchema(String, JsonNode)`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static JsonSchemaRef getJsonSchemaRef(JsonSchema, ValidationContext, String, String)` returns `com.networknt.schema.JsonSchemaRef`, related with `com.networknt.schema.JsonSchema.createChildSchema(String, JsonNode)`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.createChildSchema(String, JsonNode)`;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.createChildSchema(String, JsonNode)`;\nclass `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.createChildSchema(String, JsonNode)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.createChildSchema(String, JsonNode)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


