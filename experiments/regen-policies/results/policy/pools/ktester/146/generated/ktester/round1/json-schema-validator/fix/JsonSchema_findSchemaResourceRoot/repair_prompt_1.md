
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.List;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findSchemaResourceRoot_Test {

    /**
     * Factory used to create {@link JsonSchema} instances for the tests.
     */
    private static JsonSchemaFactory factory;

    /**
     * Root schema used in the tests (no parent).
     */
    private JsonSchema rootSchema;

    /**
     * Child schema obtained from the root via a $ref.
     */
    private JsonSchema childSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 meta‑schema – it is the most common version and works
        // for the simple test schemas below.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Root schema – a simple object with an ID.  The "$ref" points to the
         * definition "inner" that lives in the same document.  Loading the
         * schema creates a hierarchy where the schema that represents the
         * "$ref" target has a non‑null parent (the root schema).
         */
        String rootSchemaJson = """
            {
              "$id": "http://example.com/root.json",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "inner": { "$ref": "#/definitions/inner" }
              },
              "definitions": {
                "inner": {
                  "type": "string"
                }
              }
            }
            """;
        // Default configuration is sufficient for these tests.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // Create the root schema instance.
        rootSchema = factory.getSchema(rootSchemaJson, config);
        /*
         * Obtain the subschema that corresponds to the "$ref" target.
         *
         * The {@code fromRef} method builds a new {@link JsonSchema} whose parent
         * is the original schema.
         */
        JsonNodePath refPath = createPath("definitions", "inner");
        childSchema = rootSchema.fromRef(rootSchema, refPath);
    }

    @AfterEach
    void teardownAfterEach() {
        rootSchema = null;
        childSchema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    /**
     * Helper that builds a {@link JsonNodePath} representing a JSON‑Pointer
     * consisting of the supplied path segments.
     *
     * <p>Because {@code JsonNodePath} does not expose a public constructor that
     * accepts a var‑args list, we create the path by starting with an empty
     * root path and chaining the protected constructors via reflection.  This
     * keeps the test self‑contained and does not rely on any internal fields
     * of {@code JsonSchema}.
     *
     * @param segments the path segments (in order)
     * @return a {@code JsonNodePath} representing the concatenated segments
     */
    private static JsonNodePath createPath(String... segments) {
        try {
            // Start with the public empty‑path constructor.
            JsonNodePath path = new JsonNodePath(PathType.OBJECT);
            // Use reflection to invoke the private constructor that adds a segment.
            Constructor<JsonNodePath> ctor = JsonNodePath.class.getDeclaredConstructor(JsonNodePath.class, String.class);
            ctor.setAccessible(true);
            for (String seg : segments) {
                path = ctor.newInstance(path, seg);
            }
            return path;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build JsonNodePath for test", e);
        }
    }

    /**
     * Utility to create a Mockito spy (CALLS_REAL_METHODS) of {@link JsonSchema}
     * with the desired {@code isSchemaResourceRoot} flag and parent reference.
     */
    private JsonSchema mockSchema(boolean isRoot, JsonSchema parent) {
        JsonSchema mock = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(mock.isSchemaResourceRoot()).thenReturn(isRoot);
        Mockito.when(mock.getParentSchema()).thenReturn(parent);
        return mock;
    }

    /**
     * Builds a chain of {@link JsonSchema} mocks of the given length.
     *
     * @param length            number of schemas in the chain (≥1)
     * @param trueAtPositions   indices (0‑based) where {@code isSchemaResourceRoot}
     *                          should return {@code true}
     * @return the leaf (first element) of the chain
     */
    private JsonSchema buildChain(int length, List<Integer> trueAtPositions) {
        // will become the root (no parent)
        JsonSchema next = null;
        for (int i = length - 1; i >= 0; i--) {
            boolean isRoot = trueAtPositions.contains(i);
            JsonSchema current = mockSchema(isRoot, next);
            next = current;
        }
        // leaf
        return next;
    }

    @Test
    void rootSchema_isItsOwnResourceRoot() {
        // The root schema should be a resource root; the method must return itself.
        JsonSchema result = rootSchema.findSchemaResourceRoot();
        assertSame(rootSchema, result, "A schema that is already a resource root must return itself");
        assertTrue(result.isSchemaResourceRoot(), "The returned schema must report that it is a resource root");
    }

    @Test
    void childSchema_findsRootAsResourceRoot() {
        // The child schema (created via fromRef) has a non‑null parent.
        assertNotNull(childSchema.getParentSchema(), "The child schema created via fromRef should have a parent");
        // The method must walk up the parent chain and return the original root.
        JsonSchema result = childSchema.findSchemaResourceRoot();
        assertSame(rootSchema, result, "findSchemaResourceRoot should return the top‑most document schema");
        assertTrue(result.isSchemaResourceRoot(), "The returned schema must be a resource root");
    }

    @Test
    void threeLevelHierarchy_onlyRootIsResourceRoot() {
        // leaf → middle → root, only root returns true.
        JsonSchema root = mockSchema(true, null);
        JsonSchema middle = mockSchema(false, root);
        JsonSchema leaf = mockSchema(false, middle);
        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(root, result, "Should return the top‑most root that is a resource root");
    }

    @Test
    void threeLevelHierarchy_middleAndRootAreResourceRoot() {
        // leaf → middle(true) → root(true) – should stop at middle.
        JsonSchema root = mockSchema(true, null);
        JsonSchema middle = mockSchema(true, root);
        JsonSchema leaf = mockSchema(false, middle);
        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(middle, result, "Should stop at the first ancestor that is a resource root (middle)");
    }

    @Test
    void leafWithNullParentAndNotRoot_throwsNullPointerException() {
        // parent is null, not a root
        JsonSchema leaf = mockSchema(false, null);
        assertThrows(NullPointerException.class, leaf::findSchemaResourceRoot, "When no ancestor reports true and parent is null, a NPE is expected");
    }

    @Test
    void fiveLevelHierarchy_onlyLevel5IsResourceRoot() {
        // level1 → level2 → level3 → level4 → level5(true)
        JsonSchema level5 = mockSchema(true, null);
        JsonSchema level4 = mockSchema(false, level5);
        JsonSchema level3 = mockSchema(false, level4);
        JsonSchema level2 = mockSchema(false, level3);
        JsonSchema level1 = mockSchema(false, level2);
        JsonSchema result = level1.findSchemaResourceRoot();
        assertSame(level5, result, "Should return the first (and only) ancestor that is a resource root (level5)");
    }

    @Test
    void twoLevelHierarchy_parentIsResourceRoot() {
        // child(false) → parent(true)
        JsonSchema parent = mockSchema(true, null);
        JsonSchema child = mockSchema(false, parent);
        JsonSchema result = child.findSchemaResourceRoot();
        assertSame(parent, result, "Should return the immediate parent when it is the first resource root");
    }

    @Test
    void fourLevelHierarchy_middleIsFirstResourceRoot() {
        // leaf(false) → level2(false) → level3(true) → level4(true)
        JsonSchema level4 = mockSchema(true, null);
        JsonSchema level3 = mockSchema(true, level4);
        JsonSchema level2 = mockSchema(false, level3);
        JsonSchema leaf = mockSchema(false, level2);
        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(level3, result, "Should stop at level3, the first ancestor reporting true");
    }

    @Test
    void cyclicParentReference_causesStackOverflowError() {
        // schema whose parent is itself; isSchemaResourceRoot = false
        JsonSchema cyclic = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(cyclic.isSchemaResourceRoot()).thenReturn(false);
        Mockito.when(cyclic.getParentSchema()).thenReturn(cyclic);
        assertThrows(StackOverflowError.class, cyclic::findSchemaResourceRoot, "A cyclic parent chain should eventually overflow the stack");
    }

    @Test
    void isSchemaResourceRootThrowsIllegalStateException_propagates() {
        JsonSchema problematic = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(problematic.isSchemaResourceRoot()).thenThrow(new IllegalStateException("boom"));
        // parent can be anything; it will never be consulted because the exception occurs first
        Mockito.when(problematic.getParentSchema()).thenReturn(null);
        IllegalStateException ex = assertThrows(IllegalStateException.class, problematic::findSchemaResourceRoot, "Exception from isSchemaResourceRoot() must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void getParentSchemaThrowsIllegalArgumentException_propagates() {
        JsonSchema problematic = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(problematic.isSchemaResourceRoot()).thenReturn(false);
        Mockito.when(problematic.getParentSchema()).thenThrow(new IllegalArgumentException("parent error"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, problematic::findSchemaResourceRoot, "Exception from getParentSchema() must propagate");
        assertEquals("parent error", ex.getMessage());
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:13: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:102: error: cannot find symbol
            JsonNodePath path = new JsonNodePath(PathType.OBJECT);
                                                         ^
  symbol:   variable OBJECT
  location: class PathType
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:120: error: cannot find symbol
        JsonSchema mock = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
                                                         ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:120: error: cannot find symbol
        JsonSchema mock = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
                          ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:121: error: cannot find symbol
        Mockito.when(mock.isSchemaResourceRoot()).thenReturn(isRoot);
        ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:122: error: cannot find symbol
        Mockito.when(mock.getParentSchema()).thenReturn(parent);
        ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:226: error: cannot find symbol
        JsonSchema cyclic = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
                                                           ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:226: error: cannot find symbol
        JsonSchema cyclic = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
                            ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:227: error: cannot find symbol
        Mockito.when(cyclic.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:228: error: cannot find symbol
        Mockito.when(cyclic.getParentSchema()).thenReturn(cyclic);
        ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:234: error: cannot find symbol
        JsonSchema problematic = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
                                                                ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:234: error: cannot find symbol
        JsonSchema problematic = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
                                 ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:235: error: cannot find symbol
        Mockito.when(problematic.isSchemaResourceRoot()).thenThrow(new IllegalStateException("boom"));
        ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:237: error: cannot find symbol
        Mockito.when(problematic.getParentSchema()).thenReturn(null);
        ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:244: error: cannot find symbol
        JsonSchema problematic = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
                                                                ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:244: error: cannot find symbol
        JsonSchema problematic = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
                                 ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:245: error: cannot find symbol
        Mockito.when(problematic.isSchemaResourceRoot()).thenReturn(false);
        ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
src/test/java/com/networknt/schema/JsonSchema_findSchemaResourceRoot_Test.java:246: error: cannot find symbol
        Mockito.when(problematic.getParentSchema()).thenThrow(new IllegalArgumentException("parent error"));
        ^
  symbol:   variable Mockito
  location: class JsonSchema_findSchemaResourceRoot_Test
19 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method findSchemaResourceRoot()': '\n     * Finds the root of the schema resource.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return the root of the schema\n     ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\n\t\tmethod `public JsonSchema findSchemaResourceRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`, api document: \n     * Finds the root of the schema resource.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return the root of the schema\n     ;\n\t\tmethod `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`, api document: \n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`, api document: \n     * Gets the evaluation parent schema.\n     * <p>\n     * This is the dynamic parent schema when following references.\n     * \n     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)\n     * @return the evaluation parent schema\n     ;\n\t\tmethod `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\nclass `com.networknt.schema.ValidationMessageHandler`:\n\trelated functions:\n\t\tmethod `protected JsonNode getMessageNode(String, JsonNode, JsonSchema, String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\nclass `com.networknt.schema.utils.JsonNodeUtil`:\n\trelated functions:\n\t\tmethod `public static boolean equalsToSchemaType(JsonNode, JsonType, JsonSchema, ValidationContext)` returns `boolean`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.findSchemaResourceRoot()`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


