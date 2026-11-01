
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
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

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
        // Create real‑method mocks for JsonSchema so that the actual
        // implementation of findSchemaResourceRoot() is executed,
        // while we can stub the methods it depends on.
        root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.isSchemaResourceRoot()).thenReturn(true);
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
        reset(root, middle, leaf);
    }

    @Test
    void shouldReturnRootWhenCalledOnLeaf() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.isSchemaResourceRoot()).thenReturn(true);
        when(root.getParentSchema()).thenReturn(null);
        JsonSchema middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(middle.isSchemaResourceRoot()).thenReturn(false);
        when(middle.getParentSchema()).thenReturn(root);
        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        when(leaf.getParentSchema()).thenReturn(middle);
        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(root, result, "The root schema should be returned when starting from the leaf");
    }

    @Test
    void shouldReturnSelfWhenAlreadyRoot() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.isSchemaResourceRoot()).thenReturn(true);
        when(root.getParentSchema()).thenReturn(null);
        JsonSchema result = root.findSchemaResourceRoot();
        assertSame(root, result, "When the schema itself is the resource root, it should be returned unchanged");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Null parent causes NullPointerException")
    void shouldThrowNPEWhenParentIsNullAndNoRoot() {
        JsonSchema orphan = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(orphan.isSchemaResourceRoot()).thenReturn(false);
        when(orphan.getParentSchema()).thenReturn(null);
        assertThrows(NullPointerException.class, orphan::findSchemaResourceRoot, "A null parent while searching for a resource root should result in NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Stops at the nearest true ancestor even if higher ancestors are also true")
    void shouldReturnNearestTrueAncestor() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.isSchemaResourceRoot()).thenReturn(true);
        when(root.getParentSchema()).thenReturn(null);
        JsonSchema middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(middle.isSchemaResourceRoot()).thenReturn(true);
        when(middle.getParentSchema()).thenReturn(root);
        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        when(leaf.getParentSchema()).thenReturn(middle);
        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(middle, result, "The method should stop at the first true ancestor (the direct parent)");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Finds root in a deep hierarchy (depth 5)")
    void shouldFindRootInDeepHierarchy() {
        JsonSchema deepRoot = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(deepRoot.isSchemaResourceRoot()).thenReturn(true);
        when(deepRoot.getParentSchema()).thenReturn(null);
        JsonSchema l4 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(l4.isSchemaResourceRoot()).thenReturn(false);
        when(l4.getParentSchema()).thenReturn(deepRoot);
        JsonSchema l3 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(l3.isSchemaResourceRoot()).thenReturn(false);
        when(l3.getParentSchema()).thenReturn(l4);
        JsonSchema l2 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(l2.isSchemaResourceRoot()).thenReturn(false);
        when(l2.getParentSchema()).thenReturn(l3);
        JsonSchema l1 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(l1.isSchemaResourceRoot()).thenReturn(false);
        when(l1.getParentSchema()).thenReturn(l2);
        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        when(leaf.getParentSchema()).thenReturn(l1);
        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(deepRoot, result, "The method should correctly walk five levels and return the deep root");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Self‑referencing schema leads to StackOverflowError")
    void shouldOverflowWhenSelfReferencing() {
        JsonSchema self = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(self.isSchemaResourceRoot()).thenReturn(false);
        // self‑reference
        when(self.getParentSchema()).thenReturn(self);
        assertThrows(StackOverflowError.class, self::findSchemaResourceRoot, "A self‑referencing parent without a true root should eventually cause StackOverflowError");
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Exception thrown by isSchemaResourceRoot propagates")
    void shouldPropagateExceptionFromIsSchemaResourceRoot() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.isSchemaResourceRoot()).thenReturn(true);
        when(root.getParentSchema()).thenReturn(null);
        JsonSchema broken = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(broken.isSchemaResourceRoot()).thenThrow(new IllegalStateException("boom"));
        when(broken.getParentSchema()).thenReturn(root);
        IllegalStateException ex = assertThrows(IllegalStateException.class, broken::findSchemaResourceRoot, "The IllegalStateException from isSchemaResourceRoot should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Exception thrown by getParentSchema propagates")
    void shouldPropagateExceptionFromGetParentSchema() {
        JsonSchema broken = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(broken.isSchemaResourceRoot()).thenReturn(false);
        when(broken.getParentSchema()).thenThrow(new UnsupportedOperationException("no parent"));
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
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:7: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:10: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:37: error: no suitable constructor found for JsonSchema(no arguments)
        TestJsonSchema(JsonSchema parent, boolean root) {
                                                        ^
    constructor JsonSchema.JsonSchema(ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(List<JsonValidator>,boolean,boolean,TypeValidator,String,boolean,JsonNode,ValidationContext,ErrorMessageType,String,MessageSource,Keyword,JsonSchema,SchemaLocation,JsonNodePath,JsonSchema,Map<String,String>) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:65: error: cannot find symbol
        root = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                      ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:66: error: cannot find symbol
        when(root.isSchemaResourceRoot()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:67: error: cannot find symbol
        when(root.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:68: error: cannot find symbol
        middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                        ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:69: error: cannot find symbol
        when(middle.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:70: error: cannot find symbol
        when(middle.getParentSchema()).thenReturn(root);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:71: error: cannot find symbol
        leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                      ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:72: error: cannot find symbol
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:73: error: cannot find symbol
        when(leaf.getParentSchema()).thenReturn(middle);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:79: error: cannot find symbol
        reset(root, middle, leaf);
        ^
  symbol:   method reset(JsonSchema,JsonSchema,JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:84: error: cannot find symbol
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:85: error: cannot find symbol
        when(root.isSchemaResourceRoot()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:86: error: cannot find symbol
        when(root.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:87: error: cannot find symbol
        JsonSchema middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:88: error: cannot find symbol
        when(middle.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:89: error: cannot find symbol
        when(middle.getParentSchema()).thenReturn(root);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:90: error: cannot find symbol
        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:91: error: cannot find symbol
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:92: error: cannot find symbol
        when(leaf.getParentSchema()).thenReturn(middle);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:99: error: cannot find symbol
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:100: error: cannot find symbol
        when(root.isSchemaResourceRoot()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:101: error: cannot find symbol
        when(root.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:110: error: cannot find symbol
        JsonSchema orphan = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:111: error: cannot find symbol
        when(orphan.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:112: error: cannot find symbol
        when(orphan.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:120: error: cannot find symbol
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:121: error: cannot find symbol
        when(root.isSchemaResourceRoot()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:122: error: cannot find symbol
        when(root.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:123: error: cannot find symbol
        JsonSchema middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:124: error: cannot find symbol
        when(middle.isSchemaResourceRoot()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:125: error: cannot find symbol
        when(middle.getParentSchema()).thenReturn(root);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:126: error: cannot find symbol
        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:127: error: cannot find symbol
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:128: error: cannot find symbol
        when(leaf.getParentSchema()).thenReturn(middle);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:137: error: cannot find symbol
        JsonSchema deepRoot = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                     ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:138: error: cannot find symbol
        when(deepRoot.isSchemaResourceRoot()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:139: error: cannot find symbol
        when(deepRoot.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:140: error: cannot find symbol
        JsonSchema l4 = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                               ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:141: error: cannot find symbol
        when(l4.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:142: error: cannot find symbol
        when(l4.getParentSchema()).thenReturn(deepRoot);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:143: error: cannot find symbol
        JsonSchema l3 = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                               ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:144: error: cannot find symbol
        when(l3.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:145: error: cannot find symbol
        when(l3.getParentSchema()).thenReturn(l4);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:146: error: cannot find symbol
        JsonSchema l2 = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                               ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:147: error: cannot find symbol
        when(l2.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:148: error: cannot find symbol
        when(l2.getParentSchema()).thenReturn(l3);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:149: error: cannot find symbol
        JsonSchema l1 = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                               ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:150: error: cannot find symbol
        when(l1.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:151: error: cannot find symbol
        when(l1.getParentSchema()).thenReturn(l2);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:152: error: cannot find symbol
        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:153: error: cannot find symbol
        when(leaf.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:154: error: cannot find symbol
        when(leaf.getParentSchema()).thenReturn(l1);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:163: error: cannot find symbol
        JsonSchema self = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:164: error: cannot find symbol
        when(self.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:166: error: cannot find symbol
        when(self.getParentSchema()).thenReturn(self);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:174: error: cannot find symbol
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:175: error: cannot find symbol
        when(root.isSchemaResourceRoot()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:176: error: cannot find symbol
        when(root.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:177: error: cannot find symbol
        JsonSchema broken = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:178: error: cannot find symbol
        when(broken.isSchemaResourceRoot()).thenThrow(new IllegalStateException("boom"));
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:179: error: cannot find symbol
        when(broken.getParentSchema()).thenReturn(root);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:188: error: cannot find symbol
        JsonSchema broken = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:189: error: cannot find symbol
        when(broken.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:190: error: cannot find symbol
        when(broken.getParentSchema()).thenThrow(new UnsupportedOperationException("no parent"));
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findSchemaResourceRoot_Test
69 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method findSchemaResourceRoot()': '\n     * Finds the root of the schema resource.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return the root of the schema\n     ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\n\t\tmethod `public JsonSchema findSchemaResourceRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`, api document: \n     * Finds the root of the schema resource.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return the root of the schema\n     ;\n\t\tmethod `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`, api document: \n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`, api document: \n     * Gets the evaluation parent schema.\n     * <p>\n     * This is the dynamic parent schema when following references.\n     * \n     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)\n     * @return the evaluation parent schema\n     ;\n\t\tmethod `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\nclass `com.networknt.schema.ValidationMessageHandler`:\n\trelated functions:\n\t\tmethod `protected JsonNode getMessageNode(String, JsonNode, JsonSchema, String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\nclass `com.networknt.schema.utils.JsonNodeUtil`:\n\trelated functions:\n\t\tmethod `public static boolean equalsToSchemaType(JsonNode, JsonType, JsonSchema, ValidationContext)` returns `boolean`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


