
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findLexicalRoot_Test {

    /**
     * When the schema itself has a non‑null {@code id}, {@code findLexicalRoot()}
     * must return the same instance.
     */
    @Test
    void findLexicalRoot_returnsSelfWhenIdPresent() {
        // Create a mock that calls real methods for findLexicalRoot()
        JsonSchema schemaWithId = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schemaWithId.getId()).thenReturn("http://example.com/root");
        when(schemaWithId.getParentSchema()).thenReturn(null);
        JsonSchema result = schemaWithId.findLexicalRoot();
        assertSame(schemaWithId, result, "When the schema has an id, findLexicalRoot() should return the schema itself");
    }

    /**
     * When the current schema has no {@code id} but its direct parent does,
     * the method should return the parent.
     */
    @Test
    void findLexicalRoot_returnsParentWhenParentHasId() {
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getId()).thenReturn("http://example.com/parent");
        when(parent.getParentSchema()).thenReturn(null);
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(parent);
        JsonSchema result = child.findLexicalRoot();
        assertSame(parent, result, "When the child has no id but its parent does, findLexicalRoot() should return the parent");
    }

    /**
     * When both the current schema and its immediate parent lack an {@code id},
     * the method must walk up the chain until it finds an ancestor with a non‑null
     * {@code id}.
     */
    @Test
    void findLexicalRoot_walksUpToAncestorWithId() {
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getId()).thenReturn("http://example.com/root");
        when(root.getParentSchema()).thenReturn(null);
        JsonSchema intermediate = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(intermediate.getId()).thenReturn(null);
        when(intermediate.getParentSchema()).thenReturn(root);
        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.getId()).thenReturn(null);
        when(leaf.getParentSchema()).thenReturn(intermediate);
        JsonSchema result = leaf.findLexicalRoot();
        assertSame(root, result, "findLexicalRoot() should climb the parent chain and return the first ancestor with a non‑null id");
    }

    /**
     * When none of the ancestors (including the topmost one) have an {@code id},
     * the method should return the topmost schema (the one whose parent is {@code null}).
     */
    @Test
    void findLexicalRoot_returnsTopmostWhenNoIdFound() {
        JsonSchema top = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(top.getId()).thenReturn(null);
        when(top.getParentSchema()).thenReturn(null);
        JsonSchema middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(middle.getId()).thenReturn(null);
        when(middle.getParentSchema()).thenReturn(top);
        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(leaf.getId()).thenReturn(null);
        when(leaf.getParentSchema()).thenReturn(middle);
        JsonSchema result = leaf.findLexicalRoot();
        assertSame(top, result, "If no ancestor has an id, findLexicalRoot() should return the topmost schema");
    }

    /**
     * The schema has a null {@code id} and no parent. The method should return
     * the schema itself because the loop breaks when {@code getParentSchema()}
     * is {@code null}.
     */
    @Test
    void findLexicalRoot_returnsSelfWhenIdNullAndNoParent() {
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(null);
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "When id is null and there is no parent, findLexicalRoot() should return the original instance");
    }

    /**
     * An empty string is considered non‑null, therefore the method must return
     * the original instance without traversing the parent chain.
     */
    @Test
    void findLexicalRoot_treatsEmptyStringIdAsNonNull() {
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn("");
        // even if a parent exists, it should never be consulted
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getId()).thenReturn("parentId");
        when(schema.getParentSchema()).thenReturn(parent);
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "An empty string id should be treated as non‑null; the method must return the schema itself");
    }

    /**
     * A deep chain where the first encountered non‑null id is at level 2
     * (even though higher levels also have ids). The method must stop at the
     * first non‑null id encountered while walking upwards.
     */
    @Test
    void findLexicalRoot_returnsFirstEncounteredIdInDeepChain() {
        // root (level4) – id null
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getId()).thenReturn(null);
        when(root.getParentSchema()).thenReturn(null);
        // level3 – id "id3"
        JsonSchema level3 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(level3.getId()).thenReturn("id3");
        when(level3.getParentSchema()).thenReturn(root);
        // level2 – id "id2" (first non‑null when walking up from child)
        JsonSchema level2 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(level2.getId()).thenReturn("id2");
        when(level2.getParentSchema()).thenReturn(level3);
        // level1 – id null
        JsonSchema level1 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(level1.getId()).thenReturn(null);
        when(level1.getParentSchema()).thenReturn(level2);
        // child – id null
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(level1);
        JsonSchema result = child.findLexicalRoot();
        assertSame(level2, result, "The method should return the first ancestor with a non‑null id (level2) even though higher ancestors also have ids");
    }

    /**
     * Self‑referential parent with a non‑null id. The loop condition fails
     * immediately, so the method must return the original instance and must
     * not enter an infinite loop.
     */
    @Test
    void findLexicalRoot_selfReferentialParentWithIdReturnsSelf() {
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn("selfId");
        // self‑reference
        when(schema.getParentSchema()).thenReturn(schema);
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "When the schema has a non‑null id, findLexicalRoot() should return itself even if getParentSchema() returns itself");
    }

    /**
     * Chain where only the middle ancestor possesses a non‑null id.
     * Expected result is that middle ancestor.
     */
    @Test
    void findLexicalRoot_returnsMiddleAncestorWithId() {
        // top – id null
        JsonSchema top = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(top.getId()).thenReturn(null);
        when(top.getParentSchema()).thenReturn(null);
        // middle – id "mid"
        JsonSchema middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(middle.getId()).thenReturn("mid");
        when(middle.getParentSchema()).thenReturn(top);
        // child – id null
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(middle);
        JsonSchema result = child.findLexicalRoot();
        assertSame(middle, result, "The method should stop at the first ancestor (middle) that has a non‑null id");
    }

    /**
     * Chain of four schemas where the first non‑null id appears at level2,
     * while level3 also has a non‑null id. The method must stop at level2.
     */
    @Test
    void findLexicalRoot_stopsAtFirstNonNullIdEvenIfHigherAncestorsAlsoHaveIds() {
        // root – id null
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getId()).thenReturn(null);
        when(root.getParentSchema()).thenReturn(null);
        // level3 – id "id3"
        JsonSchema level3 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(level3.getId()).thenReturn("id3");
        when(level3.getParentSchema()).thenReturn(root);
        // level2 – id "id2" (first non‑null)
        JsonSchema level2 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(level2.getId()).thenReturn("id2");
        when(level2.getParentSchema()).thenReturn(level3);
        // level1 – id null
        JsonSchema level1 = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(level1.getId()).thenReturn(null);
        when(level1.getParentSchema()).thenReturn(level2);
        // child – id null
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(level1);
        JsonSchema result = child.findLexicalRoot();
        assertSame(level2, result, "The method should return the first encountered non‑null id (level2) even though level3 also has an id");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:7: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:9: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:6: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:11: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:21: error: cannot find symbol
        JsonSchema schemaWithId = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                         ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:22: error: cannot find symbol
        when(schemaWithId.getId()).thenReturn("http://example.com/root");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:23: error: cannot find symbol
        when(schemaWithId.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:34: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:35: error: cannot find symbol
        when(parent.getId()).thenReturn("http://example.com/parent");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:36: error: cannot find symbol
        when(parent.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:37: error: cannot find symbol
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                  ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:38: error: cannot find symbol
        when(child.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:39: error: cannot find symbol
        when(child.getParentSchema()).thenReturn(parent);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:51: error: cannot find symbol
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:52: error: cannot find symbol
        when(root.getId()).thenReturn("http://example.com/root");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:53: error: cannot find symbol
        when(root.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:54: error: cannot find symbol
        JsonSchema intermediate = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                         ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:55: error: cannot find symbol
        when(intermediate.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:56: error: cannot find symbol
        when(intermediate.getParentSchema()).thenReturn(root);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:57: error: cannot find symbol
        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:58: error: cannot find symbol
        when(leaf.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:59: error: cannot find symbol
        when(leaf.getParentSchema()).thenReturn(intermediate);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:70: error: cannot find symbol
        JsonSchema top = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:71: error: cannot find symbol
        when(top.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:72: error: cannot find symbol
        when(top.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:73: error: cannot find symbol
        JsonSchema middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:74: error: cannot find symbol
        when(middle.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:75: error: cannot find symbol
        when(middle.getParentSchema()).thenReturn(top);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:76: error: cannot find symbol
        JsonSchema leaf = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:77: error: cannot find symbol
        when(leaf.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:78: error: cannot find symbol
        when(leaf.getParentSchema()).thenReturn(middle);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:90: error: cannot find symbol
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:91: error: cannot find symbol
        when(schema.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:92: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:103: error: cannot find symbol
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:104: error: cannot find symbol
        when(schema.getId()).thenReturn("");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:106: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:107: error: cannot find symbol
        when(parent.getId()).thenReturn("parentId");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:108: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(parent);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:121: error: cannot find symbol
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:122: error: cannot find symbol
        when(root.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:123: error: cannot find symbol
        when(root.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:125: error: cannot find symbol
        JsonSchema level3 = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:126: error: cannot find symbol
        when(level3.getId()).thenReturn("id3");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:127: error: cannot find symbol
        when(level3.getParentSchema()).thenReturn(root);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:129: error: cannot find symbol
        JsonSchema level2 = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:130: error: cannot find symbol
        when(level2.getId()).thenReturn("id2");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:131: error: cannot find symbol
        when(level2.getParentSchema()).thenReturn(level3);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:133: error: cannot find symbol
        JsonSchema level1 = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:134: error: cannot find symbol
        when(level1.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:135: error: cannot find symbol
        when(level1.getParentSchema()).thenReturn(level2);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:137: error: cannot find symbol
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                  ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:138: error: cannot find symbol
        when(child.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:139: error: cannot find symbol
        when(child.getParentSchema()).thenReturn(level1);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:151: error: cannot find symbol
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:152: error: cannot find symbol
        when(schema.getId()).thenReturn("selfId");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:154: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(schema);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:166: error: cannot find symbol
        JsonSchema top = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:167: error: cannot find symbol
        when(top.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:168: error: cannot find symbol
        when(top.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:170: error: cannot find symbol
        JsonSchema middle = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:171: error: cannot find symbol
        when(middle.getId()).thenReturn("mid");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:172: error: cannot find symbol
        when(middle.getParentSchema()).thenReturn(top);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:174: error: cannot find symbol
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                  ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:175: error: cannot find symbol
        when(child.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:176: error: cannot find symbol
        when(child.getParentSchema()).thenReturn(middle);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:188: error: cannot find symbol
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:189: error: cannot find symbol
        when(root.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:190: error: cannot find symbol
        when(root.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:192: error: cannot find symbol
        JsonSchema level3 = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:193: error: cannot find symbol
        when(level3.getId()).thenReturn("id3");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:194: error: cannot find symbol
        when(level3.getParentSchema()).thenReturn(root);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:196: error: cannot find symbol
        JsonSchema level2 = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:197: error: cannot find symbol
        when(level2.getId()).thenReturn("id2");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:198: error: cannot find symbol
        when(level2.getParentSchema()).thenReturn(level3);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:200: error: cannot find symbol
        JsonSchema level1 = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:201: error: cannot find symbol
        when(level1.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:202: error: cannot find symbol
        when(level1.getParentSchema()).thenReturn(level2);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:204: error: cannot find symbol
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                  ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:205: error: cannot find symbol
        when(child.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:206: error: cannot find symbol
        when(child.getParentSchema()).thenReturn(level1);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
82 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema findSchemaResourceRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Finds the root of the schema resource.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return the root of the schema\n     ;\n\t\tmethod `public JsonNode getRefSchemaNode(String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Find the schema node for $ref attribute.\n     *\n     * @param ref String\n     * @return JsonNode\n     ;\n\t\tmethod `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\n\t\tmethod `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\n\t\tmethod `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Gets the evaluation parent schema.\n     * <p>\n     * This is the dynamic parent schema when following references.\n     * \n     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)\n     * @return the evaluation parent schema\n     ;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static String resolve(JsonSchema, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


