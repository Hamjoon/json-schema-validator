


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Proxy;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_findAncestor_Test {

//     @Mock
    private ValidationContext // shared mock for all schemas
    validationContext;

//     @Mock
    private JsonNode // dummy schema node (content not relevant)
    schemaNode;

    // top‑most schema (no parent)
    private JsonSchema rootSchema;

    // direct child of rootSchema
    private JsonSchema childSchema;

    // child of childSchema
    private JsonSchema grandChildSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Create a proxy ValidationContext that returns null for any method call.
        validationContext = (ValidationContext) Proxy.newProxyInstance(ValidationContext.class.getClassLoader(), new Class[] { ValidationContext.class }, (proxy, method, args) -> null);
        // The dummy JsonNode can also be a simple proxy; its behaviour is never used.
        schemaNode = (JsonNode) Proxy.newProxyInstance(JsonNode.class.getClassLoader(), new Class[] { JsonNode.class }, (proxy, method, args) -> null);
        // Build a simple parent chain: root -> child -> grandChild
        rootSchema = JsonSchema.from(validationContext, "rootPath", URI.create("http://example.com/root"), schemaNode, // no parent
        null, false);
        childSchema = JsonSchema.from(validationContext, "childPath", URI.create("http://example.com/child"), schemaNode, // parent is rootSchema
        rootSchema, false);
        grandChildSchema = JsonSchema.from(validationContext, "grandChildPath", URI.create("http://example.com/grandChild"), schemaNode, // parent is childSchema
        childSchema, false);
    }

    @Test
    void findAncestor_WhenSchemaHasNoParent_ReturnsItself() {
        JsonSchema result = rootSchema.findAncestor();
        assertSame(rootSchema, result, "Root schema should be its own ancestor");
    }

    @Test
    void findAncestor_WhenSchemaHasDirectParent_ReturnsParent() {
        JsonSchema result = childSchema.findAncestor();
        assertSame(rootSchema, result, "Child schema should resolve to the direct parent as ancestor");
    }

    @Test
    void findAncestor_WhenSchemaHasMultipleAncestors_ReturnsTopMostAncestor() {
        JsonSchema result = grandChildSchema.findAncestor();
        assertSame(rootSchema, result, "Grand‑child schema should resolve to the top‑most ancestor");
    }

    @Test
    void findAncestor_IdempotentOnRootSchema() {
        JsonSchema firstCall = rootSchema.findAncestor();
        JsonSchema secondCall = rootSchema.findAncestor();
        assertSame(rootSchema, firstCall, "First call should return the root itself");
        assertSame(rootSchema, secondCall, "Second call should also return the root itself");
        assertSame(firstCall, secondCall, "Both calls must return the identical instance");
    }

    @Test
    void findAncestor_DeepHierarchy_TenLevels() {
        List<JsonSchema> chain = new ArrayList<>();
        JsonSchema previous = null;
        for (int i = 0; i < 10; i++) {
            JsonSchema current = JsonSchema.from(validationContext, "path" + i, URI.create("http://example.com/schema" + i), schemaNode, // parent is the previous element (null for the first)
            previous, false);
            chain.add(current);
            previous = current;
        }
        JsonSchema leaf = chain.get(chain.size() - 1);
        JsonSchema topAncestor = chain.get(0);
        JsonSchema result = leaf.findAncestor();
        assertSame(topAncestor, result, "Leaf schema should resolve to the first schema in a ten‑level chain");
    }

    @Test
    void findAncestor_CyclicParentReference_ThrowsStackOverflowError() throws Exception {
        JsonSchema schemaA = JsonSchema.from(validationContext, "A", URI.create("http://example.com/A"), schemaNode, // temporary null parent
        null, false);
        JsonSchema schemaB = JsonSchema.from(validationContext, "B", URI.create("http://example.com/B"), schemaNode, // B's parent is A
        schemaA, false);
        // Introduce a cycle: A's parent becomes B.
        Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
        parentField.setAccessible(true);
        parentField.set(schemaA, schemaB);
        // The method lacks cycle detection; a StackOverflowError is expected.
        assertThrows(StackOverflowError.class, () -> schemaA.findAncestor(), "A cyclic parent chain should cause a StackOverflowError");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_findAncestor_Test ✔
│     ├─ findAncestor_WhenSchemaHasDirectParent_ReturnsParent() ✘ com.networknt.schema.ValidationContext is not an interface
│     ├─ findAncestor_CyclicParentReference_ThrowsStackOverflowError() ✘ com.networknt.schema.ValidationContext is not an interface
│     ├─ findAncestor_WhenSchemaHasNoParent_ReturnsItself() ✘ com.networknt.schema.ValidationContext is not an interface
│     ├─ findAncestor_IdempotentOnRootSchema() ✘ com.networknt.schema.ValidationContext is not an interface
│     ├─ findAncestor_DeepHierarchy_TenLevels() ✘ com.networknt.schema.ValidationContext is not an interface
│     └─ findAncestor_WhenSchemaHasMultipleAncestors_ReturnsTopMostAncestor() ✘ com.networknt.schema.ValidationContext is not an interface
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (6):
  JUnit Jupiter:JsonSchema_findAncestor_Test:findAncestor_WhenSchemaHasDirectParent_ReturnsParent()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestor_WhenSchemaHasDirectParent_ReturnsParent', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: com.networknt.schema.ValidationContext is not an interface
       java.base/java.lang.reflect.Proxy$ProxyBuilder.validateProxyInterfaces(Proxy.java:706)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:648)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:656)
       java.base/java.lang.reflect.Proxy.lambda$getProxyConstructor$0(Proxy.java:429)
       java.base/jdk.internal.loader.AbstractClassLoaderValue$Memoizer.get(AbstractClassLoaderValue.java:329)
       java.base/jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:205)
       java.base/java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:427)
       java.base/java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1037)
       com.networknt.schema.JsonSchema_findAncestor_Test.setupBeforeEach(JsonSchema_findAncestor_Test.java:35)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       [...]
  JUnit Jupiter:JsonSchema_findAncestor_Test:findAncestor_CyclicParentReference_ThrowsStackOverflowError()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestor_CyclicParentReference_ThrowsStackOverflowError', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: com.networknt.schema.ValidationContext is not an interface
       java.base/java.lang.reflect.Proxy$ProxyBuilder.validateProxyInterfaces(Proxy.java:706)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:648)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:656)
       java.base/java.lang.reflect.Proxy.lambda$getProxyConstructor$0(Proxy.java:429)
       java.base/jdk.internal.loader.AbstractClassLoaderValue$Memoizer.get(AbstractClassLoaderValue.java:329)
       java.base/jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:205)
       java.base/java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:427)
       java.base/java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1037)
       com.networknt.schema.JsonSchema_findAncestor_Test.setupBeforeEach(JsonSchema_findAncestor_Test.java:35)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       [...]
  JUnit Jupiter:JsonSchema_findAncestor_Test:findAncestor_WhenSchemaHasNoParent_ReturnsItself()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestor_WhenSchemaHasNoParent_ReturnsItself', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: com.networknt.schema.ValidationContext is not an interface
       java.base/java.lang.reflect.Proxy$ProxyBuilder.validateProxyInterfaces(Proxy.java:706)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:648)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:656)
       java.base/java.lang.reflect.Proxy.lambda$getProxyConstructor$0(Proxy.java:429)
       java.base/jdk.internal.loader.AbstractClassLoaderValue$Memoizer.get(AbstractClassLoaderValue.java:329)
       java.base/jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:205)
       java.base/java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:427)
       java.base/java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1037)
       com.networknt.schema.JsonSchema_findAncestor_Test.setupBeforeEach(JsonSchema_findAncestor_Test.java:35)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       [...]
  JUnit Jupiter:JsonSchema_findAncestor_Test:findAncestor_IdempotentOnRootSchema()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestor_IdempotentOnRootSchema', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: com.networknt.schema.ValidationContext is not an interface
       java.base/java.lang.reflect.Proxy$ProxyBuilder.validateProxyInterfaces(Proxy.java:706)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:648)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:656)
       java.base/java.lang.reflect.Proxy.lambda$getProxyConstructor$0(Proxy.java:429)
       java.base/jdk.internal.loader.AbstractClassLoaderValue$Memoizer.get(AbstractClassLoaderValue.java:329)
       java.base/jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:205)
       java.base/java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:427)
       java.base/java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1037)
       com.networknt.schema.JsonSchema_findAncestor_Test.setupBeforeEach(JsonSchema_findAncestor_Test.java:35)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       [...]
  JUnit Jupiter:JsonSchema_findAncestor_Test:findAncestor_DeepHierarchy_TenLevels()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestor_DeepHierarchy_TenLevels', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: com.networknt.schema.ValidationContext is not an interface
       java.base/java.lang.reflect.Proxy$ProxyBuilder.validateProxyInterfaces(Proxy.java:706)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:648)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:656)
       java.base/java.lang.reflect.Proxy.lambda$getProxyConstructor$0(Proxy.java:429)
       java.base/jdk.internal.loader.AbstractClassLoaderValue$Memoizer.get(AbstractClassLoaderValue.java:329)
       java.base/jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:205)
       java.base/java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:427)
       java.base/java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1037)
       com.networknt.schema.JsonSchema_findAncestor_Test.setupBeforeEach(JsonSchema_findAncestor_Test.java:35)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       [...]
  JUnit Jupiter:JsonSchema_findAncestor_Test:findAncestor_WhenSchemaHasMultipleAncestors_ReturnsTopMostAncestor()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestor_WhenSchemaHasMultipleAncestors_ReturnsTopMostAncestor', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: com.networknt.schema.ValidationContext is not an interface
       java.base/java.lang.reflect.Proxy$ProxyBuilder.validateProxyInterfaces(Proxy.java:706)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:648)
       java.base/java.lang.reflect.Proxy$ProxyBuilder.<init>(Proxy.java:656)
       java.base/java.lang.reflect.Proxy.lambda$getProxyConstructor$0(Proxy.java:429)
       java.base/jdk.internal.loader.AbstractClassLoaderValue$Memoizer.get(AbstractClassLoaderValue.java:329)
       java.base/jdk.internal.loader.AbstractClassLoaderValue.computeIfAbsent(AbstractClassLoaderValue.java:205)
       java.base/java.lang.reflect.Proxy.getProxyConstructor(Proxy.java:427)
       java.base/java.lang.reflect.Proxy.newProxyInstance(Proxy.java:1037)
       com.networknt.schema.JsonSchema_findAncestor_Test.setupBeforeEach(JsonSchema_findAncestor_Test.java:35)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       [...]

Test run finished after 38 ms
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

 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `private String getCustomMessage(JsonNode, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
		method `private JsonNode getMessageNode(JsonNode, JsonSchema, String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
		method `public JsonSchema findAncestor()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
		method ` JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
		method `public JsonNode getRefSchemaNode(String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findAncestor()`, api document: 
     * Find the schema node for $ref attribute.
     *
     * @param ref String
     * @return JsonNode
     ;
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
class `com.networknt.schema.utils.JsonNodeUtil`:
	related functions:
		method `private static boolean isEnumObjectSchema(JsonSchema)` returns `boolean`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
class `com.networknt.schema.ItemsValidator`:
	related functions:
		method `private void walkSchema(ExecutionContext, JsonSchema, JsonNode, JsonNode, String, boolean, Set<ValidationMessage>)` returns `void`, related with `com.networknt.schema.JsonSchema.findAncestor()`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
