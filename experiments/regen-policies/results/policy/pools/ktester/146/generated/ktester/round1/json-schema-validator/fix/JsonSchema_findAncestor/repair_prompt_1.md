
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
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchema.findAncestor() tests")
class JsonSchema_findAncestor_Test {

    /**
     * Mock representing the top‑most schema (no parent).
     */
    @Mock
    JsonSchema rootSchema;

    /**
     * Mock representing a child of {@link #rootSchema}.
     */
    @Mock
    JsonSchema childSchema;

    /**
     * Mock representing a grand‑child of {@link #rootSchema}.
     */
    @Mock
    JsonSchema grandChildSchema;

    /**
     * Minimal concrete implementation of {@link JsonSchema} used in the tests.
     * It stores a reference to its parent and delegates {@code findAncestor()}
     * to the default implementation defined in the interface.
     */
    private static class SimpleJsonSchema implements JsonSchema {

        private final JsonSchema parent;

        SimpleJsonSchema(JsonSchema parent) {
            this.parent = parent;
        }

        @Override
        public JsonSchema getParentSchema() {
            return parent;
        }

        /* -----------------------------------------------------------------
         * The remaining methods of the {@link JsonSchema} interface are not
         * required for the behaviour under test.  They are implemented with
         * harmless defaults so that the class can be instantiated.
         * ----------------------------------------------------------------- */
        @Override
        public JsonSchema findLexicalRoot() {
            return this;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return null;
        }

        @Override
        public com.fasterxml.jackson.databind.JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public JsonSchemaFactory getFactory() {
            return null;
        }

        @Override
        public String getSchemaPath() {
            return "";
        }

        @Override
        public String getId() {
            return "";
        }

        @Override
        public boolean isSchemaResourceRoot() {
            return false;
        }

        @Override
        public boolean isRef() {
            return false;
        }

        @Override
        public boolean isConditional() {
            return false;
        }

        @Override
        public boolean isSchemaNode(com.fasterxml.jackson.databind.JsonNode node) {
            return false;
        }

        @Override
        public JsonSchema getRefSchema() {
            return null;
        }

        @Override
        public Set<ValidationMessage> validate(com.fasterxml.jackson.databind.JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> validate(com.fasterxml.jackson.databind.JsonNode node, com.networknt.schema.ExecutionContext ctx) {
            return Collections.emptySet();
        }

        @Override
        public void initialize() {
        }

        @Override
        public void setEvaluationParentSchema(JsonSchema parent) {
        }

        @Override
        public void setParentSchema(JsonSchema parent) {
        }

        @Override
        public void setSchemaPath(String path) {
        }

        @Override
        public void setId(String id) {
        }

        @Override
        public void setSchemaNode(com.fasterxml.jackson.databind.JsonNode schemaNode) {
        }

        @Override
        public void setFactory(JsonSchemaFactory factory) {
        }

        @Override
        public void setSchemaResourceRoot(boolean root) {
        }

        @Override
        public void setRefSchema(JsonSchema refSchema) {
        }

        @Override
        public void setEvaluationParentSchema(JsonSchema evaluationParentSchema) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build the parent chain: root <- child <- grandChild
        rootSchema = new SimpleJsonSchema(null);
        childSchema = new SimpleJsonSchema(rootSchema);
        grandChildSchema = new SimpleJsonSchema(childSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Null out references to help the garbage collector; not strictly needed.
        rootSchema = null;
        childSchema = null;
        grandChildSchema = null;
    }

    @Test
    void findAncestorReturnsSelfWhenNoParent() {
        JsonSchema result = rootSchema.findAncestor();
        assertSame(rootSchema, result, "When a schema has no parent, findAncestor() must return the schema itself.");
    }

    @Test
    void findAncestorWalksUpTheChainToRoot() {
        JsonSchema result = grandChildSchema.findAncestor();
        assertSame(rootSchema, result, "findAncestor() should return the top‑most ancestor in the parent chain.");
    }

    @Test
    void findAncestorHandlesTwoLevelChain() {
        JsonSchema result = childSchema.findAncestor();
        assertSame(rootSchema, result, "For a one‑level child, findAncestor() must still return the root schema.");
    }

    @Test
    void findAncestorSelfReferencingParentCycleThrowsStackOverflow() {
        // Create a schema whose parent points to itself → infinite recursion.
        SimpleJsonSchema selfCycle = new SimpleJsonSchema(null);
        // Manually create the cycle.
        // (We cannot change the final field, so we use a subclass that overrides getParentSchema.)
        SimpleJsonSchema cyclic = new SimpleJsonSchema(selfCycle) {

            @Override
            public JsonSchema getParentSchema() {
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
        // Introduce the cycle by overriding getParentSchema on 'a'.
        SimpleJsonSchema cyclicA = new SimpleJsonSchema(c) {

            @Override
            public JsonSchema getParentSchema() {
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
        JsonSchema result = leaf.findAncestor();
        assertSame(root, result, "A ten‑level hierarchy should return the top‑most root schema after recursion.");
    }

    @Test
    void findAncestorIdempotentCallsReturnSameInstance() {
        JsonSchema firstCall = grandChildSchema.findAncestor();
        JsonSchema secondCall = grandChildSchema.findAncestor();
        assertSame(rootSchema, firstCall, "First call should return the root schema.");
        assertSame(rootSchema, secondCall, "Second call should return the same root schema instance, proving idempotence.");
    }

    @Test
    void findAncestorDoesNotModifyParentReference() {
        JsonSchema parentBefore = childSchema.getParentSchema();
        // invoke method under test
        childSchema.findAncestor();
        JsonSchema parentAfter = childSchema.getParentSchema();
        assertSame(parentBefore, parentAfter, "The parent reference must remain unchanged after findAncestor() execution.");
    }

    @Test
    void findAncestorPropagatesExceptionFromParent() {
        // Simulate a parent whose findAncestor() throws an exception.
        JsonSchema problematicParent = new SimpleJsonSchema(null) {

            @Override
            public JsonSchema findAncestor() {
                throw new IllegalArgumentException("parent failure");
            }
        };
        JsonSchema child = new SimpleJsonSchema(problematicParent);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, child::findAncestor, "Exception thrown by the parent should propagate unchanged.");
        assertEquals("parent failure", thrown.getMessage(), "Propagated exception should retain the original message.");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:10: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:40: error: interface expected here
    private static class SimpleJsonSchema implements JsonSchema {
                                                     ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:160: error: method setEvaluationParentSchema(JsonSchema) is already defined in class SimpleJsonSchema
        public void setEvaluationParentSchema(JsonSchema evaluationParentSchema) {
                    ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:13: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:20: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_findAncestor_Test
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:26: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_findAncestor_Test
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:32: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_findAncestor_Test
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:48: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:58: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:60: error: incompatible types: SimpleJsonSchema cannot be converted to JsonSchema
            return this;
                   ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:63: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:68: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:73: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:78: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:83: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:88: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:93: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:98: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:103: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:108: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:113: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:118: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:123: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:127: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:131: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:135: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:139: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:143: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:147: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:151: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:155: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:159: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:167: error: incompatible types: SimpleJsonSchema cannot be converted to JsonSchema
        rootSchema = new SimpleJsonSchema(null);
                     ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:168: error: incompatible types: SimpleJsonSchema cannot be converted to JsonSchema
        childSchema = new SimpleJsonSchema(rootSchema);
                      ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:169: error: incompatible types: SimpleJsonSchema cannot be converted to JsonSchema
        grandChildSchema = new SimpleJsonSchema(childSchema);
                           ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:204: error: incompatible types: SimpleJsonSchema cannot be converted to JsonSchema
        SimpleJsonSchema cyclic = new SimpleJsonSchema(selfCycle) {
                                                       ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:208: error: incompatible types: <anonymous SimpleJsonSchema> cannot be converted to JsonSchema
                return this;
                       ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:204: error: cannot find symbol
        SimpleJsonSchema cyclic = new SimpleJsonSchema(selfCycle) {
                                  ^
  symbol: constructor (SimpleJsonSchema)
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:211: error: invalid method reference
        assertThrows(StackOverflowError.class, cyclic::findAncestor, "A self‑referencing parent should cause a StackOverflowError.");
                                               ^
  cannot find symbol
    symbol:   method findAncestor()
    location: class SimpleJsonSchema
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:218: error: incompatible types: SimpleJsonSchema cannot be converted to JsonSchema
        SimpleJsonSchema b = new SimpleJsonSchema(a);
                                                  ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:219: error: incompatible types: SimpleJsonSchema cannot be converted to JsonSchema
        SimpleJsonSchema c = new SimpleJsonSchema(b);
                                                  ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:221: error: incompatible types: SimpleJsonSchema cannot be converted to JsonSchema
        SimpleJsonSchema cyclicA = new SimpleJsonSchema(c) {
                                                        ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:225: error: incompatible types: SimpleJsonSchema cannot be converted to JsonSchema
                return c;
                       ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:221: error: cannot find symbol
        SimpleJsonSchema cyclicA = new SimpleJsonSchema(c) {
                                   ^
  symbol: constructor (SimpleJsonSchema)
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:228: error: invalid method reference
        assertThrows(StackOverflowError.class, cyclicA::findAncestor, "An indirect parent cycle should eventually cause a StackOverflowError.");
                                               ^
  cannot find symbol
    symbol:   method findAncestor()
    location: class SimpleJsonSchema
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:238: error: incompatible types: SimpleJsonSchema cannot be converted to JsonSchema
            chain.add(new SimpleJsonSchema(chain.get(i - 1)));
                                                    ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:242: error: cannot find symbol
        JsonSchema result = leaf.findAncestor();
                                ^
  symbol:   method findAncestor()
  location: variable leaf of type SimpleJsonSchema
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:268: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:266: error: incompatible types: <anonymous SimpleJsonSchema> cannot be converted to JsonSchema
        JsonSchema problematicParent = new SimpleJsonSchema(null) {
                                       ^
src/test/java/com/networknt/schema/JsonSchema_findAncestor_Test.java:273: error: incompatible types: SimpleJsonSchema cannot be converted to JsonSchema
        JsonSchema child = new SimpleJsonSchema(problematicParent);
                           ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
51 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.findAncestor()`, api document: \n     * Determines if this schema resource is a schema resource root.\n     * <p>\n     * This is either the schema document root or the subschema resource root.\n     *\n     * @return if this schema is a schema resource root\n     ;\n\t\tmethod `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findAncestor()`;\n\t\tmethod `public JsonSchema findAncestor()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findAncestor()`;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.findAncestor()`;\n\t\tmethod `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.findAncestor()`, api document: \n     * Gets the evaluation parent schema.\n     * <p>\n     * This is the dynamic parent schema when following references.\n     * \n     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)\n     * @return the evaluation parent schema\n     ;\nclass `com.networknt.schema.ValidationMessageHandler`:\n\trelated functions:\n\t\tmethod `protected JsonNode getMessageNode(String, JsonNode, JsonSchema, String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.findAncestor()`;\nclass `com.networknt.schema.utils.JsonNodeUtil`:\n\trelated functions:\n\t\tmethod `public static boolean equalsToSchemaType(JsonNode, JsonType, JsonSchema, ValidationContext)` returns `boolean`, related with `com.networknt.schema.JsonSchema.findAncestor()`;\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.findAncestor()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.findAncestor()`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


