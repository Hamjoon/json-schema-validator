
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findSchemaResourceRoot_Test {

    /**
     * The root schema – the one that reports {@code isSchemaResourceRoot()} as true.
     */
    private JsonSchema root;

    /**
     * An intermediate schema whose parent is {@code root}.
     */
    private JsonSchema middle;

    /**
     * The leaf schema whose parent is {@code middle}.
     */
    private JsonSchema leaf;

    /**
     * Simple concrete implementation used only for testing.
     */
    private static class TestJsonSchema extends JsonSchema {

        private JsonSchema parent;

        private final boolean root;

        TestJsonSchema(JsonSchema parent, boolean root) {
            this.parent = parent;
            this.root = root;
        }

        /**
         * Allows changing the parent after construction (used for self‑reference test).
         */
        void setParent(JsonSchema parent) {
            this.parent = parent;
        }

        @Override
        public JsonSchema getParentSchema() {
            return parent;
        }

        @Override
        public boolean isSchemaResourceRoot() {
            return root;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a real‑method mock for JsonSchema so that the actual
        // implementation of findSchemaResourceRoot() is executed,
        // while we can stub the methods it depends on.
        root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.isSchemaResourceRoot()).thenReturn(true);
        // root has no parent
        when(root.getParentSchema()).thenReturn(null);
        middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(middle.isSchemaResourceRoot()).thenReturn(false);
        when(middle.getParentSchema()).thenReturn(root);
        leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        when(leaf.getParentSchema()).thenReturn(middle);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        Mockito.reset(root, middle, leaf);
    }

    @Test
    void shouldReturnRootWhenCalledOnLeaf() {
        JsonSchema root = new TestJsonSchema(null, true);
        JsonSchema middle = new TestJsonSchema(root, false);
        JsonSchema leaf = new TestJsonSchema(middle, false);
        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(root, result, "The root schema should be returned when starting from the leaf");
    }

    @Test
    void shouldReturnSelfWhenAlreadyRoot() {
        JsonSchema root = new TestJsonSchema(null, true);
        JsonSchema result = root.findSchemaResourceRoot();
        assertSame(root, result, "When the schema itself is the resource root, it should be returned unchanged");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Null parent causes NullPointerException")
    void shouldThrowNPEWhenParentIsNullAndNoRoot() {
        JsonSchema orphan = new TestJsonSchema(null, false);
        assertThrows(NullPointerException.class, orphan::findSchemaResourceRoot, "A null parent while searching for a resource root should result in NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Stops at the nearest true ancestor even if higher ancestors are also true")
    void shouldReturnNearestTrueAncestor() {
        JsonSchema root = new TestJsonSchema(null, true);
        // also a root
        JsonSchema middle = new TestJsonSchema(root, true);
        JsonSchema leaf = new TestJsonSchema(middle, false);
        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(middle, result, "The method should stop at the first true ancestor (the direct parent)");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Finds root in a deep hierarchy (depth 5)")
    void shouldFindRootInDeepHierarchy() {
        JsonSchema deepRoot = new TestJsonSchema(null, true);
        JsonSchema l4 = new TestJsonSchema(deepRoot, false);
        JsonSchema l3 = new TestJsonSchema(l4, false);
        JsonSchema l2 = new TestJsonSchema(l3, false);
        JsonSchema l1 = new TestJsonSchema(l2, false);
        JsonSchema leaf = new TestJsonSchema(l1, false);
        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(deepRoot, result, "The method should correctly walk five levels and return the deep root");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Self‑referencing schema leads to StackOverflowError")
    void shouldOverflowWhenSelfReferencing() {
        TestJsonSchema self = new TestJsonSchema(null, false);
        // create a self‑reference
        self.setParent(self);
        assertThrows(StackOverflowError.class, self::findSchemaResourceRoot, "A self‑referencing parent without a true root should eventually cause StackOverflowError");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Exception thrown by isSchemaResourceRoot propagates")
    void shouldPropagateExceptionFromIsSchemaResourceRoot() {
        JsonSchema root = new TestJsonSchema(null, true);
        JsonSchema broken = new JsonSchema() {

            @Override
            public boolean isSchemaResourceRoot() {
                throw new IllegalStateException("boom");
            }

            @Override
            public JsonSchema getParentSchema() {
                return root;
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, broken::findSchemaResourceRoot, "The IllegalStateException from isSchemaResourceRoot should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Exception thrown by getParentSchema propagates")
    void shouldPropagateExceptionFromGetParentSchema() {
        JsonSchema broken = new JsonSchema() {

            @Override
            public boolean isSchemaResourceRoot() {
                return false;
            }

            @Override
            public JsonSchema getParentSchema() {
                throw new UnsupportedOperationException("no parent");
            }
        };
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, broken::findSchemaResourceRoot, "The UnsupportedOperationException from getParentSchema should propagate");
        assertEquals("no parent", ex.getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:5: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:8: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:35: error: no suitable constructor found for JsonSchema(no arguments)
        TestJsonSchema(JsonSchema parent, boolean root) {
                                                        ^
    constructor JsonSchema.JsonSchema(ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(List<JsonValidator>,boolean,boolean,TypeValidator,String,boolean,JsonNode,ValidationContext,ErrorMessageType,String,MessageSource,Keyword,JsonSchema,SchemaLocation,JsonNodePath,JsonSchema,Map<String,String>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:63: error: cannot find symbol
        root = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                      ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:64: error: cannot find symbol
        when(root.isSchemaResourceRoot()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:66: error: cannot find symbol
        when(root.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:67: error: cannot find symbol
        middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                        ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:68: error: cannot find symbol
        when(middle.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:69: error: cannot find symbol
        when(middle.getParentSchema()).thenReturn(root);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:70: error: cannot find symbol
        leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                      ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:71: error: cannot find symbol
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:72: error: cannot find symbol
        when(leaf.getParentSchema()).thenReturn(middle);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:78: error: cannot find symbol
        Mockito.reset(root, middle, leaf);
        ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:146: error: no suitable constructor found for JsonSchema(no arguments)
        JsonSchema broken = new JsonSchema() {
                            ^
    constructor JsonSchema.JsonSchema(ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(List<JsonValidator>,boolean,boolean,TypeValidator,String,boolean,JsonNode,ValidationContext,ErrorMessageType,String,MessageSource,Keyword,JsonSchema,SchemaLocation,JsonNodePath,JsonSchema,Map<String,String>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:166: error: no suitable constructor found for JsonSchema(no arguments)
        JsonSchema broken = new JsonSchema() {
                            ^
    constructor JsonSchema.JsonSchema(ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(List<JsonValidator>,boolean,boolean,TypeValidator,String,boolean,JsonNode,ValidationContext,ErrorMessageType,String,MessageSource,Keyword,JsonSchema,SchemaLocation,JsonNodePath,JsonSchema,Map<String,String>) is not applicable
      (actual and formal argument lists differ in length)
15 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method findSchemaResourceRoot()': '\n     * Finds the root of the schema resource.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return the root of the schema\n     ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\n\t\tmethod `public JsonSchema findSchemaResourceRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`, api document: \n     * Finds the root of the schema resource.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return the root of the schema\n     ;\n\t\tmethod `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`, api document: \n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`, api document: \n     * Gets the evaluation parent schema.\n     * <p>\n     * This is the dynamic parent schema when following references.\n     * \n     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)\n     * @return the evaluation parent schema\n     ;\n\t\tmethod `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\nclass `com.networknt.schema.ValidationMessageHandler`:\n\trelated functions:\n\t\tmethod `protected JsonNode getMessageNode(String, JsonNode, JsonSchema, String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\nclass `com.networknt.schema.utils.JsonNodeUtil`:\n\trelated functions:\n\t\tmethod `public static boolean equalsToSchemaType(JsonNode, JsonType, JsonSchema, ValidationContext)` returns `boolean`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


