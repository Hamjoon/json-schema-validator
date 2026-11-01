
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findLexicalRoot_Test {

    /**
     * Helper that creates a {@link JsonSchema} instance and injects the
     * {@code id} and {@code parentSchema} fields via reflection.
     *
     * @param id     the lexical identifier (may be {@code null})
     * @param parent the parent schema (may be {@code null})
     * @return a {@code JsonSchema} with the supplied state
     */
    private JsonSchema createSchema(String id, JsonSchema parent) {
        try {
            // JsonSchema has a public no‑arg constructor in the library.
            JsonSchema schema = JsonSchema.class.getDeclaredConstructor().newInstance();
            // The library stores the identifier in a field named "id".
            // The parent reference is stored in a field named "parentSchema".
            // Both fields are private, so we make them accessible.
            Field idField = JsonSchema.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(schema, id);
            Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
            parentField.setAccessible(true);
            parentField.set(schema, parent);
            return schema;
        } catch (Exception e) {
            // If reflection fails we wrap the exception in a RuntimeException,
            // causing the test to fail with a clear message.
            throw new RuntimeException("Unable to create test JsonSchema instance", e);
        }
    }

    /**
     * When the schema itself already has an {@code id}, {@code findLexicalRoot}
     * should return the same instance.
     */
    @Test
    void testFindLexicalRootWhenSelfHasId() {
        // Arrange
        JsonSchema schema = createSchema("rootId", null);
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
        JsonSchema parent = createSchema("parentId", null);
        JsonSchema child = createSchema(null, parent);
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
        JsonSchema root = createSchema(null, null);
        JsonSchema child = createSchema(null, root);
        JsonSchema grandChild = createSchema(null, child);
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
        JsonSchema root = createSchema(null, null);
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
        JsonSchema schema = createSchema(null, null);
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
        JsonSchema grandParent = createSchema("grandParentId", null);
        JsonSchema parent = createSchema(null, grandParent);
        JsonSchema child = createSchema(null, parent);
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
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:7: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:10: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\n\t\tmethod `public JsonSchema findSchemaResourceRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Finds the root of the schema resource.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return the root of the schema\n     ;\n\t\tmethod `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ;\n\t\tmethod `public JsonNode getRefSchemaNode(String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Find the schema node for $ref attribute.\n     *\n     * @param ref String\n     * @return JsonNode\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\n\t\tmethod `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Gets the evaluation parent schema.\n     * <p>\n     * This is the dynamic parent schema when following references.\n     * \n     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)\n     * @return the evaluation parent schema\n     ;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static String resolve(JsonSchema, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


