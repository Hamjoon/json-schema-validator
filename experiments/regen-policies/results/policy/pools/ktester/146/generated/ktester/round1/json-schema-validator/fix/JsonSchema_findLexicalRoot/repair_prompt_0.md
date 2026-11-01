
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
import static org.mockito.Answers.CALLS_REAL_METHODS;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findLexicalRoot_Test {

    /**
     * When the schema itself already has an {@code id}, {@code findLexicalRoot}
     * should return the same instance.
     */
    @Test
    void testFindLexicalRootWhenSelfHasId() {
        // Arrange
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn("rootId");
        when(schema.getParentSchema()).thenReturn(null);
        // Act
        JsonSchema result = schema.findLexicalRoot();
        // Assert
        assertSame(schema, result, "Lexical root should be the schema itself when it has a non‑null id");
    }

    /**
     * When the current schema has no {@code id} but its parent does,
     * {@code findLexicalRoot} should return the parent.
     */
    @Test
    void testFindLexicalRootFindsAncestorWithId() {
        // Arrange
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getId()).thenReturn("parentId");
        when(parent.getParentSchema()).thenReturn(null);
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(parent);
        // Act
        JsonSchema result = child.findLexicalRoot();
        // Assert
        assertSame(parent, result, "Lexical root should be the nearest ancestor with a non‑null id");
    }

    /**
     * When neither the schema nor any of its ancestors have an {@code id},
     * {@code findLexicalRoot} should return the top‑most schema in the chain.
     */
    @Test
    void testFindLexicalRootWhenNoAncestorHasId() {
        // Arrange
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getId()).thenReturn(null);
        when(root.getParentSchema()).thenReturn(null);
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(root);
        JsonSchema grandChild = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(grandChild.getId()).thenReturn(null);
        when(grandChild.getParentSchema()).thenReturn(child);
        // Act
        JsonSchema result = grandChild.findLexicalRoot();
        // Assert
        assertSame(root, result, "When no ancestors have an id, the lexical root should be the topmost schema");
    }

    /**
     * A root schema without an {@code id} should be its own lexical root.
     */
    @Test
    void testFindLexicalRootRootWithoutId() {
        // Arrange
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(root.getId()).thenReturn(null);
        when(root.getParentSchema()).thenReturn(null);
        // Act
        JsonSchema result = root.findLexicalRoot();
        // Assert
        assertSame(root, result, "A root schema without an id should be its own lexical root");
    }

    /**
     * When the schema has a {@code null} id and no parent, it should be its own
     * lexical root.
     */
    @Test
    void testFindLexicalRootSelfNoIdNoParent() {
        // Arrange
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schema.getId()).thenReturn(null);
        when(schema.getParentSchema()).thenReturn(null);
        // Act
        JsonSchema result = schema.findLexicalRoot();
        // Assert
        assertSame(schema, result, "A schema without id and without parent should be its own lexical root");
    }

    /**
     * When the immediate parent has no {@code id} but the grand‑parent does,
     * the method should climb two levels and return the grand‑parent.
     */
    @Test
    void testFindLexicalRootGrandParentHasId() {
        // Arrange
        JsonSchema grandParent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(grandParent.getId()).thenReturn("grandParentId");
        when(grandParent.getParentSchema()).thenReturn(null);
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(parent.getId()).thenReturn(null);
        when(parent.getParentSchema()).thenReturn(grandParent);
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(child.getId()).thenReturn(null);
        when(child.getParentSchema()).thenReturn(parent);
        // Act
        JsonSchema result = child.findLexicalRoot();
        // Assert
        assertSame(grandParent, result, "Lexical root should be the first ancestor with a non‑null id (grand‑parent in this case)");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:7: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:8: error: package org.mockito does not exist
import static org.mockito.Answers.CALLS_REAL_METHODS;
                         ^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:8: error: static import only from classes and interfaces
import static org.mockito.Answers.CALLS_REAL_METHODS;
^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:9: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:10: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:6: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:12: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:22: error: cannot find symbol
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:23: error: cannot find symbol
        when(schema.getId()).thenReturn("rootId");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:24: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:38: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:39: error: cannot find symbol
        when(parent.getId()).thenReturn("parentId");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:40: error: cannot find symbol
        when(parent.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:41: error: cannot find symbol
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                  ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:42: error: cannot find symbol
        when(child.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:43: error: cannot find symbol
        when(child.getParentSchema()).thenReturn(parent);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:57: error: cannot find symbol
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:58: error: cannot find symbol
        when(root.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:59: error: cannot find symbol
        when(root.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:60: error: cannot find symbol
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                  ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:61: error: cannot find symbol
        when(child.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:62: error: cannot find symbol
        when(child.getParentSchema()).thenReturn(root);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:63: error: cannot find symbol
        JsonSchema grandChild = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                       ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:64: error: cannot find symbol
        when(grandChild.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:65: error: cannot find symbol
        when(grandChild.getParentSchema()).thenReturn(child);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:78: error: cannot find symbol
        JsonSchema root = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                 ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:79: error: cannot find symbol
        when(root.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:80: error: cannot find symbol
        when(root.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:94: error: cannot find symbol
        JsonSchema schema = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:95: error: cannot find symbol
        when(schema.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:96: error: cannot find symbol
        when(schema.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:110: error: cannot find symbol
        JsonSchema grandParent = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                        ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:111: error: cannot find symbol
        when(grandParent.getId()).thenReturn("grandParentId");
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:112: error: cannot find symbol
        when(grandParent.getParentSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:113: error: cannot find symbol
        JsonSchema parent = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                   ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:114: error: cannot find symbol
        when(parent.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:115: error: cannot find symbol
        when(parent.getParentSchema()).thenReturn(grandParent);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:116: error: cannot find symbol
        JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
                                                  ^
  symbol:   variable CALLS_REAL_METHODS
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:117: error: cannot find symbol
        when(child.getId()).thenReturn(null);
        ^
  symbol:   method when(String)
  location: class JsonSchema_findLexicalRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:118: error: cannot find symbol
        when(child.getParentSchema()).thenReturn(parent);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_findLexicalRoot_Test
40 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\n\t\tmethod `public JsonSchema findSchemaResourceRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Finds the root of the schema resource.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return the root of the schema\n     ;\n\t\tmethod `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ;\n\t\tmethod `public JsonNode getRefSchemaNode(String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Find the schema node for $ref attribute.\n     *\n     * @param ref String\n     * @return JsonNode\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\n\t\tmethod `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Gets the evaluation parent schema.\n     * <p>\n     * This is the dynamic parent schema when following references.\n     * \n     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)\n     * @return the evaluation parent schema\n     ;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static String resolve(JsonSchema, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


