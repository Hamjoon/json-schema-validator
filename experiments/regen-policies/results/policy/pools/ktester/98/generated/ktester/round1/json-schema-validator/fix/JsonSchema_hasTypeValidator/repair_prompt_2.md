


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
import com.networknt.schema.SpecVersion.VersionFlag;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Constructor;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_hasTypeValidator_Test {

    /**
     * Factory used to create {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default draft‑2020‑12 meta‑schema; no customisation needed for these tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {
        });
    }

    /**
     * A schema that contains a top‑level {@code "type"} keyword should report
     * that it has a {@link TypeValidator}.
     */
    @Test
    void hasTypeValidator_whenTypeIsPresent_returnsTrue() {
        // GIVEN a schema JSON string with a top‑level type definition
        String schemaWithType = "{ \"type\": \"object\" }";
        // WHEN the schema is parsed
        JsonSchema jsonSchema = factory.getSchema(schemaWithType);
        // THEN hasTypeValidator() must return true
        assertTrue(jsonSchema.hasTypeValidator(), "hasTypeValidator() should be true when a top‑level \"type\" keyword is present");
    }

    /**
     * A schema that does not contain a top‑level {@code "type"} keyword should
     * report that it does not have a {@link TypeValidator}.
     */
    @Test
    void hasTypeValidator_whenTypeIsAbsent_returnsFalse() {
        // GIVEN a schema JSON string without a top‑level type definition
        String schemaWithoutType = "{ \"properties\": { \"name\": { \"type\": \"string\" } } }";
        // WHEN the schema is parsed
        JsonSchema jsonSchema = factory.getSchema(schemaWithoutType);
        // THEN hasTypeValidator() must return false
        assertFalse(jsonSchema.hasTypeValidator(), "hasTypeValidator() should be false when no top‑level \"type\" keyword is present");
    }

    /**
     * A schema whose {@code "type"} keyword is an array (union type) should also
     * report that it has a {@link TypeValidator}.
     */
    @Test
    void hasTypeValidator_whenTypeIsUnion_returnsTrue() {
        // GIVEN a schema with a union type definition
        String schemaWithUnionType = "{ \"type\": [\"string\", \"null\"] }";
        // WHEN the schema is parsed
        JsonSchema jsonSchema = factory.getSchema(schemaWithUnionType);
        // THEN hasTypeValidator() must return true
        assertTrue(jsonSchema.hasTypeValidator(), "hasTypeValidator() should be true when \"type\" is a union array");
    }

    /**
     * When the protected {@code getTypeValidator()} returns a non‑null mock,
     * {@code hasTypeValidator()} must return {@code true}.
     */
//     @Test
//     void hasTypeValidator_spyWithNonNullTypeValidator_returnsTrue() {
//         // Create a real instance (using a minimal schema) and spy it.
//         JsonSchema real = factory.getSchema("{\"type\":\"string\"}");
//         JsonSchema spy = spy(real);
//         // Stub the protected method to return a dummy (non‑null) TypeValidator.
//         TypeValidator dummy = mock(TypeValidator.class);
//         doReturn(dummy).when(spy).getTypeValidator();
//         // The public method should now see the stubbed value.
//         assertTrue(spy.hasTypeValidator(), "hasTypeValidator() should be true when getTypeValidator() is stubbed to a non‑null value");
//     }

    /**
     * When the protected {@code getTypeValidator()} returns {@code null},
     * {@code hasTypeValidator()} must return {@code false}.
     */
//     @Test
//     void hasTypeValidator_spyWithNullTypeValidator_returnsFalse() {
//         JsonSchema real = factory.getSchema("{\"properties\":{\"name\":{\"type\":\"string\"}}}");
//         JsonSchema spy = spy(real);
//         // Stub to return null.
//         doReturn(null).when(spy).getTypeValidator();
//         assertFalse(spy.hasTypeValidator(), "hasTypeValidator() should be false when getTypeValidator() is stubbed to null");
//     }

    @Test
    void hasTypeValidator_refPointsToSchemaWithType_returnsTrue() {
        // referenced schema (definition) contains a type
        String rootSchemaJson = "{\n" + "  \"$ref\": \"#/definitions/ref\",\n" + "  \"definitions\": {\n" + "    \"ref\": { \"type\": \"integer\" }\n" + "  }\n" + "}";
        JsonSchema schema = factory.getSchema(rootSchemaJson);
        assertTrue(schema.hasTypeValidator(), "hasTypeValidator() should be true when $ref resolves to a schema containing a top‑level \"type\"");
    }

    @Test
    void hasTypeValidator_refPointsToSchemaWithoutType_returnsFalse() {
        String rootSchemaJson = "{\n" + "  \"$ref\": \"#/definitions/ref\",\n" + "  \"definitions\": {\n" + "    \"ref\": { \"properties\": { \"age\": { \"type\": \"number\" } } }\n" + "  }\n" + "}";
        JsonSchema schema = factory.getSchema(rootSchemaJson);
        assertFalse(schema.hasTypeValidator(), "hasTypeValidator() should be false when $ref resolves to a schema that lacks a top‑level \"type\"");
    }

    @Test
    void hasTypeValidator_emptyTypeArray_returnsFalse() {
        String schemaJson = "{ \"type\": [] }";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertFalse(schema.hasTypeValidator(), "An empty \"type\" array should be treated as no usable TypeValidator");
    }

    @Test
    void hasTypeValidator_invalidTypeValue_throwsJsonSchemaException() {
        String schemaJson = "{ \"type\": 123 }";
        // The factory validates the schema during creation and should throw.
        assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaJson), "A non‑string/non‑array \"type\" value must cause JsonSchemaException");
    }

    @Test
    void hasTypeValidator_nullJsonSchemaReference_throwsNullPointerException() {
        JsonSchema schema = null;
        assertThrows(NullPointerException.class, schema::hasTypeValidator, "Calling hasTypeValidator() on a null reference must throw NullPointerException");
    }

    @Test
    void hasTypeValidator_circularRef_throwsStackOverflowError() {
        String circularSchema = "{\n" + "  \"$ref\": \"#/definitions/circle\",\n" + "  \"definitions\": {\n" + "    \"circle\": { \"$ref\": \"#/definitions/circle\" }\n" + "  }\n" + "}";
        // The recursion occurs during schema construction / validation.
        assertThrows(StackOverflowError.class, () -> {
            JsonSchema schema = factory.getSchema(circularSchema);
            // Trigger the lazy creation of the TypeValidator.
            schema.hasTypeValidator();
        }, "A self‑referencing $ref should eventually cause StackOverflowError");
    }

    @Test
    void hasTypeValidator_schemaCreatedWithNullNode_throwsNullPointerException() throws Exception {
        // Obtain the (package‑private) constructor that the factory uses internally.
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // Pass null for the JsonNode argument – all other arguments are also null
        // because they are not needed for this particular test.
        JsonSchema schema = ctor.newInstance(null, null, null, null, null, false);
        // The internal logic expects a non‑null schema node; calling the public
        // method should therefore raise NPE.
        assertThrows(NullPointerException.class, schema::hasTypeValidator, "Creating a JsonSchema with a null JsonNode should cause NullPointerException when hasTypeValidator() is invoked");
    }

    @ParameterizedTest(name = "type array [{0}] -> expected {1}")
    @CsvSource({ "'[\"string\",\"null\"]', true", "'[]', false" })
    void hasTypeValidator_typeArrayVariants(String typeArrayJson, boolean expected) {
        String schemaJson = "{ \"type\": " + typeArrayJson + " }";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertEquals(expected, schema.hasTypeValidator(), "Unexpected result for schema with type array: " + typeArrayJson);
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_hasTypeValidator_Test ✔
│     ├─ hasTypeValidator_whenTypeIsUnion_returnsTrue() ✔
│     ├─ hasTypeValidator_refPointsToSchemaWithoutType_returnsFalse() ✔
│     ├─ hasTypeValidator_whenTypeIsPresent_returnsTrue() ✔
│     ├─ hasTypeValidator_circularRef_throwsStackOverflowError() ✘ A self‑referencing $ref should eventually cause StackOverflowError ==> Expected java.lang.StackOverflowError to be thrown, but nothing was thrown.
│     ├─ hasTypeValidator_nullJsonSchemaReference_throwsNullPointerException() ✘ java.lang.NullPointerException
│     ├─ hasTypeValidator_schemaCreatedWithNullNode_throwsNullPointerException() ✘ Cannot invoke "com.networknt.schema.ValidationContext.resolveSchemaId(com.fasterxml.jackson.databind.JsonNode)" because "validationContext" is null
│     ├─ hasTypeValidator_typeArrayVariants(String, boolean) ✔
│     │  ├─ type array [["string","null"]] -> expected true ✔
│     │  └─ type array [[]] -> expected false ✘ Unexpected result for schema with type array: [] ==> expected: <false> but was: <true>
│     ├─ hasTypeValidator_whenTypeIsAbsent_returnsFalse() ✔
│     ├─ hasTypeValidator_invalidTypeValue_throwsJsonSchemaException() ✘ A non‑string/non‑array "type" value must cause JsonSchemaException ==> Expected com.networknt.schema.JsonSchemaException to be thrown, but nothing was thrown.
│     ├─ hasTypeValidator_refPointsToSchemaWithType_returnsTrue() ✘ hasTypeValidator() should be true when $ref resolves to a schema containing a top‑level "type" ==> expected: <true> but was: <false>
│     └─ hasTypeValidator_emptyTypeArray_returnsFalse() ✘ An empty "type" array should be treated as no usable TypeValidator ==> expected: <false> but was: <true>
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (7):
  JUnit Jupiter:JsonSchema_hasTypeValidator_Test:hasTypeValidator_circularRef_throwsStackOverflowError()
    MethodSource [className = 'com.networknt.schema.JsonSchema_hasTypeValidator_Test', methodName = 'hasTypeValidator_circularRef_throwsStackOverflowError', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: A self‑referencing $ref should eventually cause StackOverflowError ==> Expected java.lang.StackOverflowError to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_hasTypeValidator_Test.hasTypeValidator_circularRef_throwsStackOverflowError(JsonSchema_hasTypeValidator_Test.java:137)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_hasTypeValidator_Test:hasTypeValidator_nullJsonSchemaReference_throwsNullPointerException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_hasTypeValidator_Test', methodName = 'hasTypeValidator_nullJsonSchemaReference_throwsNullPointerException', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.JsonSchema_hasTypeValidator_Test.hasTypeValidator_nullJsonSchemaReference_throwsNullPointerException(JsonSchema_hasTypeValidator_Test.java:130)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       [...]
  JUnit Jupiter:JsonSchema_hasTypeValidator_Test:hasTypeValidator_schemaCreatedWithNullNode_throwsNullPointerException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_hasTypeValidator_Test', methodName = 'hasTypeValidator_schemaCreatedWithNullNode_throwsNullPointerException', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.resolveSchemaId(com.fasterxml.jackson.databind.JsonNode)" because "validationContext" is null
       com.networknt.schema.JsonSchema.resolve(JsonSchema.java:76)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchema_hasTypeValidator_Test.hasTypeValidator_schemaCreatedWithNullNode_throwsNullPointerException(JsonSchema_hasTypeValidator_Test.java:151)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
  JUnit Jupiter:JsonSchema_hasTypeValidator_Test:hasTypeValidator_typeArrayVariants(String, boolean):type array [[]] -> expected false
    MethodSource [className = 'com.networknt.schema.JsonSchema_hasTypeValidator_Test', methodName = 'hasTypeValidator_typeArrayVariants', methodParameterTypes = 'java.lang.String, boolean']
    => org.opentest4j.AssertionFailedError: Unexpected result for schema with type array: [] ==> expected: <false> but was: <true>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1156)
       com.networknt.schema.JsonSchema_hasTypeValidator_Test.hasTypeValidator_typeArrayVariants(JsonSchema_hasTypeValidator_Test.java:162)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_hasTypeValidator_Test:hasTypeValidator_invalidTypeValue_throwsJsonSchemaException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_hasTypeValidator_Test', methodName = 'hasTypeValidator_invalidTypeValue_throwsJsonSchemaException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: A non‑string/non‑array "type" value must cause JsonSchemaException ==> Expected com.networknt.schema.JsonSchemaException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_hasTypeValidator_Test.hasTypeValidator_invalidTypeValue_throwsJsonSchemaException(JsonSchema_hasTypeValidator_Test.java:124)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_hasTypeValidator_Test:hasTypeValidator_refPointsToSchemaWithType_returnsTrue()
    MethodSource [className = 'com.networknt.schema.JsonSchema_hasTypeValidator_Test', methodName = 'hasTypeValidator_refPointsToSchemaWithType_returnsTrue', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: hasTypeValidator() should be true when $ref resolves to a schema containing a top‑level "type" ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:214)
       com.networknt.schema.JsonSchema_hasTypeValidator_Test.hasTypeValidator_refPointsToSchemaWithType_returnsTrue(JsonSchema_hasTypeValidator_Test.java:103)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_hasTypeValidator_Test:hasTypeValidator_emptyTypeArray_returnsFalse()
    MethodSource [className = 'com.networknt.schema.JsonSchema_hasTypeValidator_Test', methodName = 'hasTypeValidator_emptyTypeArray_returnsFalse', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: An empty "type" array should be treated as no usable TypeValidator ==> expected: <false> but was: <true>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertFalse.failNotFalse(AssertFalse.java:63)
       org.junit.jupiter.api.AssertFalse.assertFalse(AssertFalse.java:36)
       org.junit.jupiter.api.Assertions.assertFalse(Assertions.java:239)
       com.networknt.schema.JsonSchema_hasTypeValidator_Test.hasTypeValidator_emptyTypeArray_returnsFalse(JsonSchema_hasTypeValidator_Test.java:117)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 429 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        12 tests found           ]
[         0 tests skipped         ]
[        12 tests started         ]
[         0 tests aborted         ]
[         5 tests successful      ]
[         7 tests failed          ]


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
boolean

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public boolean hasTypeValidator()` returns `boolean`, related with `com.networknt.schema.JsonSchema.hasTypeValidator()`;
class `com.networknt.schema.AnyOfValidator`:
	related functions:
		method `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.hasTypeValidator()`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.hasTypeValidator()`, api document: 
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
