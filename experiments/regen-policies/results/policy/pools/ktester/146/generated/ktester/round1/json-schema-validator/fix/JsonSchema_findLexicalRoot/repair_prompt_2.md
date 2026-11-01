


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;

// @ExtendWith(MockitoExtension.class)
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
            // JsonSchema may not have a public no‑arg constructor, so we obtain
            // it via reflection and make it accessible.
            Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            JsonSchema schema = ctor.newInstance();
            // Set the private fields "id" and "parentSchema".
            Field idField = JsonSchema.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(schema, id);
            Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
            parentField.setAccessible(true);
            parentField.set(schema, parent);
            return schema;
        } catch (Exception e) {
            // Wrap any reflection exception so the test fails with a clear cause.
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

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_findLexicalRoot_Test ✔
│     ├─ testFindLexicalRootSelfNoIdNoParent() ✘ Unable to create test JsonSchema instance
│     ├─ testFindLexicalRootFindsAncestorWithId() ✘ Unable to create test JsonSchema instance
│     ├─ testFindLexicalRootRootWithoutId() ✘ Unable to create test JsonSchema instance
│     ├─ testFindLexicalRootWhenNoAncestorHasId() ✘ Unable to create test JsonSchema instance
│     ├─ testFindLexicalRootGrandParentHasId() ✘ Unable to create test JsonSchema instance
│     └─ testFindLexicalRootWhenSelfHasId() ✘ Unable to create test JsonSchema instance
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (6):
  JUnit Jupiter:JsonSchema_findLexicalRoot_Test:testFindLexicalRootSelfNoIdNoParent()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findLexicalRoot_Test', methodName = 'testFindLexicalRootSelfNoIdNoParent', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to create test JsonSchema instance
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.createSchema(JsonSchema_findLexicalRoot_Test.java:36)
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.testFindLexicalRootSelfNoIdNoParent(JsonSchema_findLexicalRoot_Test.java:105)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.JsonSchema.<init>()
       java.base/java.lang.Class.getConstructor0(Class.java:3587)
       java.base/java.lang.Class.getDeclaredConstructor(Class.java:2756)
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.createSchema(JsonSchema_findLexicalRoot_Test.java:23)
       [...]
  JUnit Jupiter:JsonSchema_findLexicalRoot_Test:testFindLexicalRootFindsAncestorWithId()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findLexicalRoot_Test', methodName = 'testFindLexicalRootFindsAncestorWithId', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to create test JsonSchema instance
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.createSchema(JsonSchema_findLexicalRoot_Test.java:36)
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.testFindLexicalRootFindsAncestorWithId(JsonSchema_findLexicalRoot_Test.java:61)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.JsonSchema.<init>()
       java.base/java.lang.Class.getConstructor0(Class.java:3587)
       java.base/java.lang.Class.getDeclaredConstructor(Class.java:2756)
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.createSchema(JsonSchema_findLexicalRoot_Test.java:23)
       [...]
  JUnit Jupiter:JsonSchema_findLexicalRoot_Test:testFindLexicalRootRootWithoutId()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findLexicalRoot_Test', methodName = 'testFindLexicalRootRootWithoutId', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to create test JsonSchema instance
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.createSchema(JsonSchema_findLexicalRoot_Test.java:36)
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.testFindLexicalRootRootWithoutId(JsonSchema_findLexicalRoot_Test.java:91)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.JsonSchema.<init>()
       java.base/java.lang.Class.getConstructor0(Class.java:3587)
       java.base/java.lang.Class.getDeclaredConstructor(Class.java:2756)
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.createSchema(JsonSchema_findLexicalRoot_Test.java:23)
       [...]
  JUnit Jupiter:JsonSchema_findLexicalRoot_Test:testFindLexicalRootWhenNoAncestorHasId()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findLexicalRoot_Test', methodName = 'testFindLexicalRootWhenNoAncestorHasId', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to create test JsonSchema instance
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.createSchema(JsonSchema_findLexicalRoot_Test.java:36)
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.testFindLexicalRootWhenNoAncestorHasId(JsonSchema_findLexicalRoot_Test.java:76)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.JsonSchema.<init>()
       java.base/java.lang.Class.getConstructor0(Class.java:3587)
       java.base/java.lang.Class.getDeclaredConstructor(Class.java:2756)
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.createSchema(JsonSchema_findLexicalRoot_Test.java:23)
       [...]
  JUnit Jupiter:JsonSchema_findLexicalRoot_Test:testFindLexicalRootGrandParentHasId()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findLexicalRoot_Test', methodName = 'testFindLexicalRootGrandParentHasId', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to create test JsonSchema instance
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.createSchema(JsonSchema_findLexicalRoot_Test.java:36)
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.testFindLexicalRootGrandParentHasId(JsonSchema_findLexicalRoot_Test.java:119)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.JsonSchema.<init>()
       java.base/java.lang.Class.getConstructor0(Class.java:3587)
       java.base/java.lang.Class.getDeclaredConstructor(Class.java:2756)
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.createSchema(JsonSchema_findLexicalRoot_Test.java:23)
       [...]
  JUnit Jupiter:JsonSchema_findLexicalRoot_Test:testFindLexicalRootWhenSelfHasId()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findLexicalRoot_Test', methodName = 'testFindLexicalRootWhenSelfHasId', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to create test JsonSchema instance
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.createSchema(JsonSchema_findLexicalRoot_Test.java:36)
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.testFindLexicalRootWhenSelfHasId(JsonSchema_findLexicalRoot_Test.java:47)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.JsonSchema.<init>()
       java.base/java.lang.Class.getConstructor0(Class.java:3587)
       java.base/java.lang.Class.getDeclaredConstructor(Class.java:2756)
       com.networknt.schema.JsonSchema_findLexicalRoot_Test.createSchema(JsonSchema_findLexicalRoot_Test.java:23)
       [...]

Test run finished after 48 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         6 tests found           ]
[         0 tests skipped         ]
[         6 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[         6 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * Used for creating a schema with validators for validating inputs. This is
 * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}
 * and should be cached for performance.
 * <p>
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 * <p>
 * JsonSchema instances are thread-safe provided its configuration is not
 * modified.
 

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;
		method `public JsonSchema findSchemaResourceRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: 
     * Finds the root of the schema resource.
     * <p>
     * This is either the schema document root or the subschema resource root.
     *
     * @return the root of the schema
     ;
		method `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: 
     * Determines if this schema resource is a schema resource root.
     * <p>
     * This is either the schema document root or the subschema resource root.
     *
     * @return if this schema is a schema resource root
     ;
		method `public JsonNode getRefSchemaNode(String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: 
     * Find the schema node for $ref attribute.
     *
     * @param ref String
     * @return JsonNode
     ;
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;
		method `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`, api document: 
     * Gets the evaluation parent schema.
     * <p>
     * This is the dynamic parent schema when following references.
     * 
     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)
     * @return the evaluation parent schema
     ;
class `com.networknt.schema.RefValidator`:
	related functions:
		method `private static String resolve(JsonSchema, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findLexicalRoot()`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
