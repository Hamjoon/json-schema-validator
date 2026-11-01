


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_findAncestor_Test {

    /**
     * Helper to create a very simple schema (empty object).
     */
    private static JsonSchema createSchema() {
        // The library provides a factory that can build a schema from a JSON string.
        // An empty object is sufficient for our purposes – it creates a schema with
        // no validation rules and, crucially, a {@code null} parent.
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909).getSchema("{}");
    }

    /**
     * Helper that sets the {@code parentSchema} field via reflection.
     */
    private static void setParent(JsonSchema child, JsonSchema parent) {
        try {
            Field f = JsonSchema.class.getDeclaredField("parentSchema");
            f.setAccessible(true);
            f.set(child, parent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A schema without a parent should return itself as the ancestor.
     */
    @Test
    void findAncestorWhenRoot() {
        // parent == null
        JsonSchema root = createSchema();
        JsonSchema result = root.findAncestor();
        assertSame(root, result, "Root schema should be its own ancestor");
    }

    /**
     * A child schema should resolve its ancestor to the root schema.
     */
    @Test
    void findAncestorWhenChild() {
        JsonSchema root = createSchema();
        JsonSchema child = createSchema();
        setParent(child, root);
        JsonSchema result = child.findAncestor();
        assertSame(root, result, "Ancestor of a direct child should be the root schema");
    }

    /**
     * A deeper hierarchy (grand‑child → child → root) should still resolve to the root.
     */
    @Test
    void findAncestorWhenGrandChild() {
        JsonSchema root = createSchema();
        JsonSchema child = createSchema();
        JsonSchema grandChild = createSchema();
        setParent(child, root);
        setParent(grandChild, child);
        JsonSchema result = grandChild.findAncestor();
        assertSame(root, result, "Ancestor of a grand‑child should be the root schema");
    }

    /**
     * Two schemas that reference each other as parents create a cycle.
     * The current implementation does not guard against cycles, so a
     * {@link StackOverflowError} is expected.
     */
    @Test
    void findAncestorWithCyclicParentReference() {
        JsonSchema a = createSchema();
        JsonSchema b = createSchema();
        setParent(a, b);
        setParent(b, a);
        assertThrows(StackOverflowError.class, a::findAncestor, "Cyclic parent chain should cause StackOverflowError");
    }

    /**
     * A schema whose parent is itself also creates an immediate cycle.
     * Expect a {@link StackOverflowError}.
     */
    @Test
    void findAncestorWithSelfReferentialParent() {
        JsonSchema self = createSchema();
        setParent(self, self);
        assertThrows(StackOverflowError.class, self::findAncestor, "Self‑referential parent should cause StackOverflowError");
    }

    /**
     * Idempotent root retrieval – calling {@code findAncestor()} multiple
     * times on a root schema should always return the same instance.
     */
    @Test
    void findAncestorIdempotentRoot() {
        JsonSchema root = createSchema();
        JsonSchema first = root.findAncestor();
        JsonSchema second = root.findAncestor();
        assertSame(root, first, "First call should return the root itself");
        assertSame(root, second, "Second call should also return the root itself");
        assertSame(first, second, "Both calls should return the identical instance");
    }

    /**
     * Build a non‑cyclic chain of 50 schemas and verify that the deepest
     * schema resolves to the top‑most root without stack overflow.
     */
    @Test
    void findAncestorDeepNonCyclicChain() {
        final int depth = 50;
        List<JsonSchema> chain = new ArrayList<>(depth);
        // create the schemas
        for (int i = 0; i < depth; i++) {
            chain.add(createSchema());
        }
        // wire the chain: element i's parent is element i‑1 (i > 0)
        for (int i = depth - 1; i > 0; i--) {
            setParent(chain.get(i), chain.get(i - 1));
        }
        JsonSchema deepest = chain.get(depth - 1);
        JsonSchema ancestor = deepest.findAncestor();
        assertSame(chain.get(0), ancestor, "Deep (50‑level) chain should resolve to the first element as ancestor");
    }

    /**
     * Construct a very deep chain (e.g., 2000 levels) that exceeds typical
     * JVM stack depth. The recursive implementation is expected to throw
     * {@link StackOverflowError}.
     */
    @Test
    void findAncestorDeepChainExceedingStack() {
        final int depth = 2000;
        List<JsonSchema> chain = new ArrayList<>(depth);
        for (int i = 0; i < depth; i++) {
            chain.add(createSchema());
        }
        for (int i = depth - 1; i > 0; i--) {
            setParent(chain.get(i), chain.get(i - 1));
        }
        JsonSchema deepest = chain.get(depth - 1);
        assertThrows(StackOverflowError.class, deepest::findAncestor, "A chain deeper than the JVM stack limit should cause StackOverflowError");
    }

    /**
     * Verify that exceptions thrown by {@code getParentSchema()} are
     * propagated unchanged by {@code findAncestor()}.
     */
//     @Test
//     void findAncestorParentThrowsException() {
//         JsonSchema faultyParent = mock(JsonSchema.class, CALLS_REAL_METHODS);
//         when(faultyParent.getParentSchema()).thenThrow(new RuntimeException("parent failure"));
//         JsonSchema child = mock(JsonSchema.class, CALLS_REAL_METHODS);
//         when(child.getParentSchema()).thenReturn(faultyParent);
//         RuntimeException ex = assertThrows(RuntimeException.class, child::findAncestor, "Exception from getParentSchema() should propagate");
//         assertEquals("parent failure", ex.getMessage(), "Propagated exception should retain original message");
//     }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_findAncestor_Test ✔
│     ├─ findAncestorWithSelfReferentialParent() ✘ java.lang.NoSuchFieldException: parentSchema
│     ├─ findAncestorWhenChild() ✘ java.lang.NoSuchFieldException: parentSchema
│     ├─ findAncestorWithCyclicParentReference() ✘ java.lang.NoSuchFieldException: parentSchema
│     ├─ findAncestorIdempotentRoot() ✔
│     ├─ findAncestorDeepNonCyclicChain() ✘ java.lang.NoSuchFieldException: parentSchema
│     ├─ findAncestorDeepChainExceedingStack() ✘ java.lang.NoSuchFieldException: parentSchema
│     ├─ findAncestorWhenRoot() ✔
│     └─ findAncestorWhenGrandChild() ✘ java.lang.NoSuchFieldException: parentSchema
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (6):
  JUnit Jupiter:JsonSchema_findAncestor_Test:findAncestorWithSelfReferentialParent()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestorWithSelfReferentialParent', methodParameterTypes = '']
    => java.lang.RuntimeException: java.lang.NoSuchFieldException: parentSchema
       com.networknt.schema.JsonSchema_findAncestor_Test.setParent(JsonSchema_findAncestor_Test.java:32)
       com.networknt.schema.JsonSchema_findAncestor_Test.findAncestorWithSelfReferentialParent(JsonSchema_findAncestor_Test.java:94)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchFieldException: parentSchema
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       com.networknt.schema.JsonSchema_findAncestor_Test.setParent(JsonSchema_findAncestor_Test.java:28)
       [...]
  JUnit Jupiter:JsonSchema_findAncestor_Test:findAncestorWhenChild()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestorWhenChild', methodParameterTypes = '']
    => java.lang.RuntimeException: java.lang.NoSuchFieldException: parentSchema
       com.networknt.schema.JsonSchema_findAncestor_Test.setParent(JsonSchema_findAncestor_Test.java:32)
       com.networknt.schema.JsonSchema_findAncestor_Test.findAncestorWhenChild(JsonSchema_findAncestor_Test.java:54)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchFieldException: parentSchema
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       com.networknt.schema.JsonSchema_findAncestor_Test.setParent(JsonSchema_findAncestor_Test.java:28)
       [...]
  JUnit Jupiter:JsonSchema_findAncestor_Test:findAncestorWithCyclicParentReference()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestorWithCyclicParentReference', methodParameterTypes = '']
    => java.lang.RuntimeException: java.lang.NoSuchFieldException: parentSchema
       com.networknt.schema.JsonSchema_findAncestor_Test.setParent(JsonSchema_findAncestor_Test.java:32)
       com.networknt.schema.JsonSchema_findAncestor_Test.findAncestorWithCyclicParentReference(JsonSchema_findAncestor_Test.java:82)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchFieldException: parentSchema
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       com.networknt.schema.JsonSchema_findAncestor_Test.setParent(JsonSchema_findAncestor_Test.java:28)
       [...]
  JUnit Jupiter:JsonSchema_findAncestor_Test:findAncestorDeepNonCyclicChain()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestorDeepNonCyclicChain', methodParameterTypes = '']
    => java.lang.RuntimeException: java.lang.NoSuchFieldException: parentSchema
       com.networknt.schema.JsonSchema_findAncestor_Test.setParent(JsonSchema_findAncestor_Test.java:32)
       com.networknt.schema.JsonSchema_findAncestor_Test.findAncestorDeepNonCyclicChain(JsonSchema_findAncestor_Test.java:126)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchFieldException: parentSchema
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       com.networknt.schema.JsonSchema_findAncestor_Test.setParent(JsonSchema_findAncestor_Test.java:28)
       [...]
  JUnit Jupiter:JsonSchema_findAncestor_Test:findAncestorDeepChainExceedingStack()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestorDeepChainExceedingStack', methodParameterTypes = '']
    => java.lang.RuntimeException: java.lang.NoSuchFieldException: parentSchema
       com.networknt.schema.JsonSchema_findAncestor_Test.setParent(JsonSchema_findAncestor_Test.java:32)
       com.networknt.schema.JsonSchema_findAncestor_Test.findAncestorDeepChainExceedingStack(JsonSchema_findAncestor_Test.java:146)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchFieldException: parentSchema
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       com.networknt.schema.JsonSchema_findAncestor_Test.setParent(JsonSchema_findAncestor_Test.java:28)
       [...]
  JUnit Jupiter:JsonSchema_findAncestor_Test:findAncestorWhenGrandChild()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestorWhenGrandChild', methodParameterTypes = '']
    => java.lang.RuntimeException: java.lang.NoSuchFieldException: parentSchema
       com.networknt.schema.JsonSchema_findAncestor_Test.setParent(JsonSchema_findAncestor_Test.java:32)
       com.networknt.schema.JsonSchema_findAncestor_Test.findAncestorWhenGrandChild(JsonSchema_findAncestor_Test.java:67)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchFieldException: parentSchema
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       com.networknt.schema.JsonSchema_findAncestor_Test.setParent(JsonSchema_findAncestor_Test.java:28)
       [...]

Test run finished after 335 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         8 tests found           ]
[         0 tests skipped         ]
[         8 tests started         ]
[         0 tests aborted         ]
[         2 tests successful      ]
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
		method `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
		method `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.findAncestor()`, api document: 
     * Determines if this schema resource is a schema resource root.
     * <p>
     * This is either the schema document root or the subschema resource root.
     *
     * @return if this schema is a schema resource root
     ;
		method `public JsonSchema findAncestor()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
		method `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findAncestor()`, api document: 
     * Gets the evaluation parent schema.
     * <p>
     * This is the dynamic parent schema when following references.
     * 
     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)
     * @return the evaluation parent schema
     ;
class `com.networknt.schema.ValidationMessageHandler`:
	related functions:
		method `protected JsonNode getMessageNode(String, JsonNode, JsonSchema, String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
class `com.networknt.schema.utils.JsonNodeUtil`:
	related functions:
		method `public static boolean equalsToSchemaType(JsonNode, JsonType, JsonSchema, ValidationContext)` returns `boolean`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
class `com.networknt.schema.OneOfValidator`:
	related functions:
		method `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.findAncestor()`, api document: 
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
