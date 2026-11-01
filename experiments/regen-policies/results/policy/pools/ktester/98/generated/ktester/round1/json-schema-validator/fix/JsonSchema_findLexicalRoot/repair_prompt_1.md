
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findLexicalRoot_Test {

    /**
     * Minimal concrete implementation that reproduces the parts of {@link JsonSchema}
     * needed for these tests.
     */
    private static class TestSchema extends JsonSchema {

        private final String id;

        private final TestSchema parent;

        TestSchema(String id, TestSchema parent) {
            // The real {@code JsonSchema} does not have a zero‑arg constructor.
            // To satisfy the compiler we call the protected no‑arg constructor
            // via reflection.  The constructor is only used so that the instance
            // can exist; the overridden methods provide the required behaviour.
            super(null, null, null, null);
            this.id = id;
            this.parent = parent;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public JsonSchema getParentSchema() {
            return parent;
        }

        /**
         * Re‑implementation of {@link JsonSchema#findLexicalRoot()} based on the
         * public contract of the library.  This mirrors the algorithm used in the
         * actual class and is sufficient for the unit‑test scenarios.
         */
        @Override
        public JsonSchema findLexicalRoot() {
            JsonSchema current = this;
            while (current.getId() == null) {
                JsonSchema p = current.getParentSchema();
                if (p == null) {
                    break;
                }
                current = p;
            }
            return current;
        }
    }

    /**
     * When the schema itself has a non‑null {@code id}, {@code findLexicalRoot()}
     * must return the same instance.
     */
    @Test
    void findLexicalRoot_returnsSelfWhenIdPresent() {
        TestSchema schemaWithId = new TestSchema("http://example.com/root", null);
        JsonSchema result = schemaWithId.findLexicalRoot();
        assertSame(schemaWithId, result, "When the schema has an id, findLexicalRoot() should return the schema itself");
    }

    /**
     * When the current schema has no {@code id} but its direct parent does,
     * the method should return the parent.
     */
    @Test
    void findLexicalRoot_returnsParentWhenParentHasId() {
        TestSchema parent = new TestSchema("http://example.com/parent", null);
        TestSchema child = new TestSchema(null, parent);
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
        TestSchema root = new TestSchema("http://example.com/root", null);
        TestSchema intermediate = new TestSchema(null, root);
        TestSchema leaf = new TestSchema(null, intermediate);
        JsonSchema result = leaf.findLexicalRoot();
        assertSame(root, result, "findLexicalRoot() should climb the parent chain and return the first ancestor with a non‑null id");
    }

    /**
     * When none of the ancestors (including the topmost one) have an {@code id},
     * the method should return the topmost schema (the one whose parent is {@code null}).
     */
    @Test
    void findLexicalRoot_returnsTopmostWhenNoIdFound() {
        TestSchema top = new TestSchema(null, null);
        TestSchema middle = new TestSchema(null, top);
        TestSchema leaf = new TestSchema(null, middle);
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
        TestSchema schema = new TestSchema(null, null);
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "When id is null and there is no parent, findLexicalRoot() should return the original instance");
    }

    /**
     * An empty string is considered non‑null, therefore the method must return
     * the original instance without traversing the parent chain.
     */
    @Test
    void findLexicalRoot_treatsEmptyStringIdAsNonNull() {
        TestSchema parent = new TestSchema("parentId", null);
        TestSchema schema = new TestSchema("", parent);
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
        TestSchema root = new TestSchema(null, null);
        // level3 – id "id3"
        TestSchema level3 = new TestSchema("id3", root);
        // level2 – id "id2" (first non‑null when walking up from child)
        TestSchema level2 = new TestSchema("id2", level3);
        // level1 – id null
        TestSchema level1 = new TestSchema(null, level2);
        // child – id null
        TestSchema child = new TestSchema(null, level1);
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
        TestSchema schema = new TestSchema("selfId", null);
        // create a self‑reference after construction
        // (reflection is used only to set the parent field for the test)
        try {
            java.lang.reflect.Field f = TestSchema.class.getDeclaredField("parent");
            f.setAccessible(true);
            f.set(schema, schema);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        JsonSchema result = schema.findLexicalRoot();
        assertSame(schema, result, "When the schema has a non‑null id, findLexicalRoot() should return itself even if getParentSchema() returns itself");
    }

    /**
     * Chain where only the middle ancestor possesses a non‑null id.
     * Expected result is that middle ancestor.
     */
    @Test
    void findLexicalRoot_returnsMiddleAncestorWithId() {
        TestSchema top = new TestSchema(null, null);
        TestSchema middle = new TestSchema("mid", top);
        TestSchema child = new TestSchema(null, middle);
        JsonSchema result = child.findLexicalRoot();
        assertSame(middle, result, "The method should stop at the first ancestor (middle) that has a non‑null id");
    }

    /**
     * Chain of four schemas where the first non‑null id appears at level2,
     * while level3 also has a non‑null id. The method must stop at level2.
     */
    @Test
    void findLexicalRoot_stopsAtFirstNonNullIdEvenIfHigherAncestorsAlsoHaveIds() {
        TestSchema root = new TestSchema(null, null);
        TestSchema level3 = new TestSchema("id3", root);
        TestSchema level2 = new TestSchema("id2", level3);
        TestSchema level1 = new TestSchema(null, level2);
        TestSchema child = new TestSchema(null, level1);
        JsonSchema result = child.findLexicalRoot();
        assertSame(level2, result, "The method should return the first encountered non‑null id (level2) even though level3 also has an id");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:4: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:9: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_findLexicalRoot_Test.java:27: error: no suitable constructor found for JsonSchema(<null>,<null>,<null>,<null>)
            super(null, null, null, null);
            ^
    constructor JsonSchema.JsonSchema(ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(List<JsonValidator>,boolean,boolean,TypeValidator,String,boolean,JsonNode,ValidationContext,ErrorMessageType,String,MessageSource,Keyword,JsonSchema,SchemaLocation,JsonNodePath,JsonSchema,Map<String,String>) is not applicable
      (actual and formal argument lists differ in length)
3 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema findSchemaResourceRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Finds the root of the schema resource.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return the root of the schema\n     ;\n\t\tmethod `public JsonNode getRefSchemaNode(String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Find the schema node for $ref attribute.\n     *\n     * @param ref String\n     * @return JsonNode\n     ;\n\t\tmethod `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\n\t\tmethod `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;\n\t\tmethod `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: \n     * Gets the evaluation parent schema.\n     * <p>\n     * This is the dynamic parent schema when following references.\n     * \n     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)\n     * @return the evaluation parent schema\n     ;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static String resolve(JsonSchema, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


