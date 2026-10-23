


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
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.*;

// @ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getNodeFieldType_Test {

    // ----- Mocks -------------------------------------------------------------
//     @Mock
    JsonSchema parentSchema;

//     @Mock
    JsonNode parentSchemaNode;

//     @Mock
    JsonNode typeNode;

    // ----- Test subject ------------------------------------------------------
    private TestValidator validator;

    // ----- Test subject ------------------------------------------------------
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        // The abstract {@code validate} method is not needed for these tests.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            return Collections.emptySet();
        }
    }

    // ----- Lifecycle ---------------------------------------------------------
    @BeforeEach
    void setupBeforeEach() {
        // By default the parent schema returns a non‑null schema node; individual
        // tests will customise the behaviour of the "type" field or the schema node
        // itself.
//         when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
//         reset(parentSchema, parentSchemaNode, typeNode);
    }

    /**
     * Provides a stream of arguments representing different scalar JSON node types.
     * For container nodes (array, object) Jackson's {@code asText()} returns an empty string.
     */
    private static Stream<Arguments> scalarTypeNodeProvider() {
        // asText() -> "123"
        JsonNode numericNode = new IntNode(123);
        // asText() -> "true"
        JsonNode booleanNode = BooleanNode.TRUE;
        // asText() -> ""
        JsonNode arrayNode = JsonNodeFactory.instance.arrayNode();
        // asText() -> ""
        JsonNode objectNode = JsonNodeFactory.instance.objectNode();
        // asText() -> "null"
        JsonNode nullNode = NullNode.instance;
        return Stream.of(Arguments.of("Numeric type node should return its textual representation", numericNode, "123"), Arguments.of("Boolean type node should return its textual representation", booleanNode, "true"), Arguments.of("Array type node should return empty string", arrayNode, ""), Arguments.of("Object type node should return empty string", objectNode, ""), Arguments.of("Explicit null node should return the string \"null\"", nullNode, "null"));
    }

    /**
     * Helper that creates a {@link JsonSchema} from the supplied schema node.
     */
    private static JsonSchema schemaFromNode(JsonNode node) {
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(node);
    }

    // ----- Tests -------------------------------------------------------------
    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // Arrange: schema contains a "type" field with value "string"
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "string");
        JsonSchema parent = schemaFromNode(schemaNode);
        TestValidator validator = new TestValidator("dummyPath", null, parent, null, null);
        // Act
        String result = validator.getNodeFieldType();
        // Assert
        assertEquals("string", result, "The type field value should be returned");
    }

    @Test
    void getNodeFieldType_returnsNullWhenAbsent() {
        // Arrange: schema does NOT contain a "type" field (empty object)
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        JsonSchema parent = schemaFromNode(schemaNode);
        TestValidator validator = new TestValidator("dummyPath", null, parent, null, null);
        // Act
        String result = validator.getNodeFieldType();
        // Assert
        assertNull(result, "When the type field is missing, null should be returned");
    }

    @ParameterizedTest(name = "[{index}] type node returns \"{1}\"")
    @MethodSource("scalarTypeNodeProvider")
    void getNodeFieldType_returnsScalarAsText(String description, JsonNode mockNode, String expected) {
        // Arrange: schema with the given type node
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.set("type", typeNode);
        JsonSchema parent = schemaFromNode(schemaNode);
        TestValidator validator = new TestValidator("dummyPath", null, parent, null, null);
        // Act
        String result = validator.getNodeFieldType();
        // Assert
        assertEquals(expected, result, description);
    }

    @Test
    void getNodeFieldType_throwsWhenParentSchemaIsNull() {
        // Arrange: validator is constructed with a null parent schema
        TestValidator validator = new TestValidator("dummyPath", null, null, null, null);
        // Act & Assert
        assertThrows(NullPointerException.class, validator::getNodeFieldType, "Calling getNodeFieldType() with a null parent schema should throw NPE");
    }

    @Test
    void getNodeFieldType_throwsWhenParentSchemaNodeIsNull() {
        // Arrange: create a JsonSchema subclass that returns null for getSchemaNode()
        JsonSchema parent = new JsonSchema(null, null, null) {

            @Override
            public JsonNode getSchemaNode() {
                return null;
            }
        };
        TestValidator validator = new TestValidator("dummyPath", null, parent, null, null);
        // Act & Assert
        assertThrows(NullPointerException.class, validator::getNodeFieldType, "Calling getNodeFieldType() when the parent schema node is null should throw NPE");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ BaseJsonValidator_getNodeFieldType_Test ✔
│     ├─ getNodeFieldType_returnsNullWhenAbsent() ✔
│     ├─ getNodeFieldType_throwsWhenParentSchemaIsNull() ✔
│     ├─ getNodeFieldType_returnsTypeWhenPresent() ✔
│     ├─ getNodeFieldType_throwsWhenParentSchemaNodeIsNull() ✘ Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
│     └─ getNodeFieldType_returnsScalarAsText(String, JsonNode, String) ✔
│        ├─ [1] type node returns "123" ✘ Numeric type node should return its textual representation ==> expected: <123> but was: <null>
│        ├─ [2] type node returns "true" ✘ Boolean type node should return its textual representation ==> expected: <true> but was: <null>
│        ├─ [3] type node returns "[]" ✘ Array type node should return empty string ==> expected: <> but was: <null>
│        ├─ [4] type node returns "{}" ✘ Object type node should return empty string ==> expected: <> but was: <null>
│        └─ [5] type node returns "null" ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (5):
  JUnit Jupiter:BaseJsonValidator_getNodeFieldType_Test:getNodeFieldType_throwsWhenParentSchemaNodeIsNull()
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_getNodeFieldType_Test', methodName = 'getNodeFieldType_throwsWhenParentSchemaNodeIsNull', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:91)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:77)
       com.networknt.schema.BaseJsonValidator_getNodeFieldType_Test$1.<init>(BaseJsonValidator_getNodeFieldType_Test.java:135)
       com.networknt.schema.BaseJsonValidator_getNodeFieldType_Test.getNodeFieldType_throwsWhenParentSchemaNodeIsNull(BaseJsonValidator_getNodeFieldType_Test.java:135)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       [...]
  JUnit Jupiter:BaseJsonValidator_getNodeFieldType_Test:getNodeFieldType_returnsScalarAsText(String, JsonNode, String):[1] type node returns "123"
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_getNodeFieldType_Test', methodName = 'getNodeFieldType_returnsScalarAsText', methodParameterTypes = 'java.lang.String, com.fasterxml.jackson.databind.JsonNode, java.lang.String']
    => org.opentest4j.AssertionFailedError: Numeric type node should return its textual representation ==> expected: <123> but was: <null>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1153)
       com.networknt.schema.BaseJsonValidator_getNodeFieldType_Test.getNodeFieldType_returnsScalarAsText(BaseJsonValidator_getNodeFieldType_Test.java:121)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:BaseJsonValidator_getNodeFieldType_Test:getNodeFieldType_returnsScalarAsText(String, JsonNode, String):[2] type node returns "true"
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_getNodeFieldType_Test', methodName = 'getNodeFieldType_returnsScalarAsText', methodParameterTypes = 'java.lang.String, com.fasterxml.jackson.databind.JsonNode, java.lang.String']
    => org.opentest4j.AssertionFailedError: Boolean type node should return its textual representation ==> expected: <true> but was: <null>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1153)
       com.networknt.schema.BaseJsonValidator_getNodeFieldType_Test.getNodeFieldType_returnsScalarAsText(BaseJsonValidator_getNodeFieldType_Test.java:121)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:BaseJsonValidator_getNodeFieldType_Test:getNodeFieldType_returnsScalarAsText(String, JsonNode, String):[3] type node returns "[]"
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_getNodeFieldType_Test', methodName = 'getNodeFieldType_returnsScalarAsText', methodParameterTypes = 'java.lang.String, com.fasterxml.jackson.databind.JsonNode, java.lang.String']
    => org.opentest4j.AssertionFailedError: Array type node should return empty string ==> expected: <> but was: <null>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1153)
       com.networknt.schema.BaseJsonValidator_getNodeFieldType_Test.getNodeFieldType_returnsScalarAsText(BaseJsonValidator_getNodeFieldType_Test.java:121)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:BaseJsonValidator_getNodeFieldType_Test:getNodeFieldType_returnsScalarAsText(String, JsonNode, String):[4] type node returns "{}"
    MethodSource [className = 'com.networknt.schema.BaseJsonValidator_getNodeFieldType_Test', methodName = 'getNodeFieldType_returnsScalarAsText', methodParameterTypes = 'java.lang.String, com.fasterxml.jackson.databind.JsonNode, java.lang.String']
    => org.opentest4j.AssertionFailedError: Object type node should return empty string ==> expected: <> but was: <null>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1153)
       com.networknt.schema.BaseJsonValidator_getNodeFieldType_Test.getNodeFieldType_returnsScalarAsText(BaseJsonValidator_getNodeFieldType_Test.java:121)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 162 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[         9 tests found           ]
[         0 tests skipped         ]
[         9 tests started         ]
[         0 tests aborted         ]
[         4 tests successful      ]
[         5 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

dependent classes: 
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `protected String getNodeFieldType()` returns `java.lang.String`, related with `com.networknt.schema.BaseJsonValidator.getNodeFieldType()`;
class `com.networknt.schema.utils.JsonNodeUtil`:
	related functions:
		method `private static boolean isEnumObjectSchema(JsonSchema)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.getNodeFieldType()`;
		method `public static boolean isNodeNullable(JsonNode)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.getNodeFieldType()`;
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `private String getCustomMessage(JsonNode, String)` returns `java.lang.String`, related with `com.networknt.schema.BaseJsonValidator.getNodeFieldType()`;
		method ` JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.BaseJsonValidator.getNodeFieldType()`;
class `com.networknt.schema.PropertiesValidator`:
	related functions:
		method `private void walkSchema(ExecutionContext, Map.Entry<String, JsonSchema>, JsonNode, JsonNode, String, boolean, Set<ValidationMessage>, WalkListenerRunner)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.getNodeFieldType()`;
		method `private static JsonNode getDefaultNode(Map.Entry<String, JsonSchema>)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.BaseJsonValidator.getNodeFieldType()`;
class `com.networknt.schema.ItemsValidator`:
	related functions:
		method `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.getNodeFieldType()`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
