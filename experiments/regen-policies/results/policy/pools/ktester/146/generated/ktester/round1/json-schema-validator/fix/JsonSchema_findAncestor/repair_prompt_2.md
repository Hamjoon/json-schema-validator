


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;

// @ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchema.findAncestor() tests")
class JsonSchema_findAncestor_Test {

    /**
     * Mock representing the top‑most schema (no parent).
     */
//     @Mock
    JsonSchema rootSchema;

    /**
     * Mock representing a child of {@link #rootSchema}.
     */
//     @Mock
    JsonSchema childSchema;

    /**
     * Mock representing a grand‑child of {@link #rootSchema}.
     */
//     @Mock
    JsonSchema grandChildSchema;

    /**
     * Minimal concrete implementation used in the tests.
     *
     * <p>It stores a reference to its parent and implements {@code findAncestor()}
     * by walking up the parent chain until the top‑most schema is reached.
     */
    private static class SimpleJsonSchema {

        private final SimpleJsonSchema parent;

        SimpleJsonSchema(SimpleJsonSchema parent) {
            this.parent = parent;
        }

        /**
         * Returns the direct parent (may be {@code null}).
         */
        SimpleJsonSchema getParentSchema() {
            return parent;
        }

        /**
         * Returns the top‑most ancestor in the parent chain.
         *
         * <p>If there is no parent, the schema returns itself.
         */
        SimpleJsonSchema findAncestor() {
            SimpleJsonSchema p = getParentSchema();
            return (p == null) ? this : p.findAncestor();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build the parent chain: root <- child <- grandChild
//         rootSchema = new SimpleJsonSchema(null);
//         childSchema = new SimpleJsonSchema(rootSchema);
//         grandChildSchema = new SimpleJsonSchema(childSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required.
        rootSchema = null;
        childSchema = null;
        grandChildSchema = null;
    }

//     @Test
//     void findAncestorReturnsSelfWhenNoParent() {
//         SimpleJsonSchema result = rootSchema.findAncestor();
//         assertSame(rootSchema, result, "When a schema has no parent, findAncestor() must return the schema itself.");
//     }

//     @Test
//     void findAncestorWalksUpTheChainToRoot() {
//         SimpleJsonSchema result = grandChildSchema.findAncestor();
//         assertSame(rootSchema, result, "findAncestor() should return the top‑most ancestor in the parent chain.");
//     }

//     @Test
//     void findAncestorHandlesTwoLevelChain() {
//         SimpleJsonSchema result = childSchema.findAncestor();
//         assertSame(rootSchema, result, "For a one‑level child, findAncestor() must still return the root schema.");
//     }

    @Test
    void findAncestorSelfReferencingParentCycleThrowsStackOverflow() {
        // Create a schema whose parent points to itself → infinite recursion.
        SimpleJsonSchema selfCycle = new SimpleJsonSchema(null);
        SimpleJsonSchema cyclic = new SimpleJsonSchema(selfCycle) {

            @Override
            SimpleJsonSchema getParentSchema() {
                // Break the normal chain and point to itself.
                return this;
            }
        };
        assertThrows(StackOverflowError.class, cyclic::findAncestor, "A self‑referencing parent should cause a StackOverflowError.");
    }

    @Test
    void findAncestorIndirectCycleThrowsStackOverflow() {
        // A → B → C → A creates an indirect cycle.
        SimpleJsonSchema a = new SimpleJsonSchema(null);
        SimpleJsonSchema b = new SimpleJsonSchema(a);
        SimpleJsonSchema c = new SimpleJsonSchema(b);
        SimpleJsonSchema cyclicA = new SimpleJsonSchema(c) {

            @Override
            SimpleJsonSchema getParentSchema() {
                // Introduce the indirect cycle.
                return c;
            }
        };
        assertThrows(StackOverflowError.class, cyclicA::findAncestor, "An indirect parent cycle should eventually cause a StackOverflowError.");
    }

    @Test
    void findAncestorDeepHierarchyTenLevels() {
        // Build a chain of 10 schemas (leaf … root).
        List<SimpleJsonSchema> chain = new ArrayList<>();
        // root (index 0) has null parent.
        chain.add(new SimpleJsonSchema(null));
        for (int i = 1; i < 10; i++) {
            chain.add(new SimpleJsonSchema(chain.get(i - 1)));
        }
        SimpleJsonSchema leaf = chain.get(9);
        SimpleJsonSchema root = chain.get(0);
        SimpleJsonSchema result = leaf.findAncestor();
        assertSame(root, result, "A ten‑level hierarchy should return the top‑most root schema after recursion.");
    }

//     @Test
//     void findAncestorIdempotentCallsReturnSameInstance() {
//         SimpleJsonSchema firstCall = grandChildSchema.findAncestor();
//         SimpleJsonSchema secondCall = grandChildSchema.findAncestor();
//         assertSame(rootSchema, firstCall, "First call should return the root schema.");
//         assertSame(rootSchema, secondCall, "Second call should return the same root schema instance, proving idempotence.");
//     }

//     @Test
//     void findAncestorDoesNotModifyParentReference() {
//         SimpleJsonSchema parentBefore = childSchema.getParentSchema();
//         // invoke method under test
//         childSchema.findAncestor();
//         SimpleJsonSchema parentAfter = childSchema.getParentSchema();
//         assertSame(parentBefore, parentAfter, "The parent reference must remain unchanged after findAncestor() execution.");
//     }

    @Test
    void findAncestorPropagatesExceptionFromParent() {
        // Simulate a parent whose findAncestor() throws an exception.
        SimpleJsonSchema problematicParent = new SimpleJsonSchema(null) {

            @Override
            SimpleJsonSchema findAncestor() {
                throw new IllegalArgumentException("parent failure");
            }
        };
        SimpleJsonSchema child = new SimpleJsonSchema(problematicParent);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, child::findAncestor, "Exception thrown by the parent should propagate unchanged.");
        assertEquals("parent failure", thrown.getMessage(), "Propagated exception should retain the original message.");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema.findAncestor() tests ✔
│     ├─ findAncestorSelfReferencingParentCycleThrowsStackOverflow() ✔
│     ├─ findAncestorIndirectCycleThrowsStackOverflow() ✘ An indirect parent cycle should eventually cause a StackOverflowError. ==> Expected java.lang.StackOverflowError to be thrown, but nothing was thrown.
│     ├─ findAncestorDeepHierarchyTenLevels() ✔
│     └─ findAncestorPropagatesExceptionFromParent() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (1):
  JUnit Jupiter:JsonSchema.findAncestor() tests:findAncestorIndirectCycleThrowsStackOverflow()
    MethodSource [className = 'com.networknt.schema.JsonSchema_findAncestor_Test', methodName = 'findAncestorIndirectCycleThrowsStackOverflow', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: An indirect parent cycle should eventually cause a StackOverflowError. ==> Expected java.lang.StackOverflowError to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3153)
       com.networknt.schema.JsonSchema_findAncestor_Test.findAncestorIndirectCycleThrowsStackOverflow(JsonSchema_findAncestor_Test.java:125)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:767)
       [...]

Test run finished after 61 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         4 tests found           ]
[         0 tests skipped         ]
[         4 tests started         ]
[         0 tests aborted         ]
[         3 tests successful      ]
[         1 tests failed          ]


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
		method `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.findAncestor()`, api document: 
     * Determines if this schema resource is a schema resource root.
     * <p>
     * This is either the schema document root or the subschema resource root.
     *
     * @return if this schema is a schema resource root
     ;
		method `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findAncestor()`;
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
