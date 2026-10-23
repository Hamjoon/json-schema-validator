


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    /**
     * Shared mapper for building {@link JsonNode} objects from JSON strings.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A simple schema containing a {@code type} keyword – this triggers creation of a {@link TypeValidator}.
     */
    private static final String SCHEMA_WITH_TYPE = "{ \"type\": \"string\" }";

    /**
     * Mocked {@link ValidationContext} used for all test instances.
     */
//     @Mock
    private ValidationContext validationContext;

    /**
     * The {@link JsonSchema} instance under test.
     */
    private JsonSchema jsonSchema;

    /**
     * Base URI used for {@link JsonSchema#from(...)} calls.
     */
    private static final URI BASE_URI = URI.create("http://example.com/schema");

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Build a JsonNode representing a schema that contains a "type"
        JsonNode schemaNode = MAPPER.readTree(SCHEMA_WITH_TYPE);
        // Create the JsonSchema instance via the public factory.
        jsonSchema = JsonSchemaFactory.getInstance().getSchema(schemaNode);
        // At this point the schema has not yet run its validator initialisation.
        // The typeValidator field should therefore be null.
    }

    /**
     * Helper that builds a {@link JsonSchema} from the given JSON string and schema path.
     */
    private JsonSchema buildSchema(String schemaJson, String schemaPath) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        // The factory creates a JsonSchema instance; it does not initialise validators automatically.
        return JsonSchemaFactory.getInstance().getSchema(schemaNode);
    }

    /**
     * Cases where a TypeValidator must be present (non‑null).
     */
    private static Stream<Arguments> presentTypeValidatorProvider() {
        return Stream.of(Arguments.of("{ \"type\": \"string\" }", "#", "string"), Arguments.of("{ \"type\": \"number\" }", "#/properties/price", "number"), Arguments.of("{ \"type\": \"integer\" }", "#/properties/count", "integer"), Arguments.of("{ \"type\": \"boolean\" }", "#/properties/active", "boolean"), Arguments.of("{ \"type\": \"object\" }", "#/properties/data", "object"), Arguments.of("{ \"type\": \"array\" }", "#/properties/list", "array"), Arguments.of("{ \"type\": \"null\" }", "#/properties/nullable", "null"), // composite type (array of types)
        Arguments.of("{ \"type\": [\"string\", \"null\"] }", "#/properties/name", "composite"), // primitive with extra constraints
        Arguments.of("{ \"type\": \"string\", \"minLength\": 3, \"enum\": [\"abc\", \"def\"] }", "#/properties/code", "string+constraints"));
    }

    /**
     * Cases where no TypeValidator should be created (null).
     */
    private static Stream<Arguments> absentTypeValidatorProvider() {
        return Stream.of(Arguments.of("{ \"minimum\": 5 }", "#/properties/age"), Arguments.of("{}", "#"), // empty type array – not a valid type definition
        Arguments.of("{ \"type\": [] }", "#/properties/invalid"));
    }

    /**
     * Cases that require an explicit {@code initializeValidators()} call.
     */
    private static Stream<Arguments> explicitInitProvider() {
        return Stream.of(Arguments.of("{ \"type\": \"integer\" }", "#"));
    }

    /**
     * Cases for repeated initialisation – the same instance must survive.
     */
    private static Stream<Arguments> repeatedInitProvider() {
        return Stream.of(Arguments.of("{ \"type\": \"string\" }", "#/properties/name"));
    }

    /**
     * Cases for checking reference equality across multiple getter calls.
     */
    private static Stream<Arguments> sameInstanceProvider() {
        return Stream.of(Arguments.of("{ \"type\": \"integer\" }", "#/properties/id"));
    }

    /**
     * Case for $ref resolution.
     */
    private static Stream<Arguments> refResolutionProvider() {
        String schema = "{\n" + "  \"$ref\": \"#/definitions/positiveInteger\",\n" + "  \"definitions\": {\n" + "    \"positiveInteger\": { \"type\": \"integer\", \"minimum\": 1 }\n" + "  }\n" + "}";
        return Stream.of(Arguments.of(schema, "#/properties/count"));
    }

    @Test
    @DisplayName("getTypeValidator() returns null before validators are initialised")
    void testGetTypeValidatorInitiallyNull() {
        // Pre‑condition – the schema has not been initialised yet.
        assertFalse(jsonSchema.hasTypeValidator(), "hasTypeValidator() should be false before initialiseValidators()");
        // The getter should return null.
        assertNull(jsonSchema.getTypeValidator(), "getTypeValidator() should return null before initialiseValidators()");
    }

    @Test
    @DisplayName("getTypeValidator() returns a non‑null TypeValidator after initialiseValidators()")
    void testGetTypeValidatorAfterInitialize() {
        jsonSchema.initializeValidators();
        assertTrue(jsonSchema.hasTypeValidator(), "hasTypeValidator() should be true after initialiseValidators()");
        TypeValidator typeValidator = jsonSchema.getTypeValidator();
        assertNotNull(typeValidator, "getTypeValidator() should return a non‑null instance after initialiseValidators()");
    }

    @ParameterizedTest(name = "[{index}] typeValidator present – schemaPath={1}")
    @MethodSource("presentTypeValidatorProvider")
    @DisplayName("getTypeValidator() returns non‑null when a valid 'type' keyword is present")
    void testTypeValidatorPresent(String schemaJson, String schemaPath, String _description) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        schema.initializeValidators();
        assertTrue(schema.hasTypeValidator(), "hasTypeValidator() should be true when schema defines a type");
        assertNotNull(schema.getTypeValidator(), "getTypeValidator() must return a non‑null instance");
    }

    @ParameterizedTest(name = "[{index}] typeValidator absent – schemaPath={1}")
    @MethodSource("absentTypeValidatorProvider")
    @DisplayName("getTypeValidator() returns null when no valid 'type' keyword is present")
    void testTypeValidatorAbsent(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        schema.initializeValidators();
        assertFalse(schema.hasTypeValidator(), "hasTypeValidator() should be false when schema does not define a valid type");
        assertNull(schema.getTypeValidator(), "getTypeValidator() must be null");
    }

    @ParameterizedTest(name = "[{index}] explicit initialiseValidators()")
    @MethodSource("explicitInitProvider")
    @DisplayName("getTypeValidator() remains non‑null after explicit initialiseValidators()")
    void testExplicitInitialization(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        schema.initializeValidators();
        assertTrue(schema.hasTypeValidator(), "hasTypeValidator() should be true after initialiseValidators()");
        assertNotNull(schema.getTypeValidator(), "getTypeValidator() must be non‑null after initialiseValidators()");
    }

    @ParameterizedTest(name = "[{index}] repeated initialiseValidators()")
    @MethodSource("repeatedInitProvider")
    @DisplayName("Repeated initialiseValidators() calls do not replace the TypeValidator")
    void testRepeatedInitialization(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        schema.initializeValidators();
        TypeValidator first = schema.getTypeValidator();
        assertNotNull(first, "First call to getTypeValidator() must be non‑null");
        // Second initialisation – should be a no‑op regarding the type validator
        schema.initializeValidators();
        TypeValidator second = schema.getTypeValidator();
        assertSame(first, second, "TypeValidator instance must be identical after repeated initialiseValidators()");
    }

    @ParameterizedTest(name = "[{index}] consistent instance across calls")
    @MethodSource("sameInstanceProvider")
    @DisplayName("Multiple getTypeValidator() calls return the same instance")
    void testConsistentInstance(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        schema.initializeValidators();
        TypeValidator first = schema.getTypeValidator();
        TypeValidator second = schema.getTypeValidator();
        TypeValidator third = schema.getTypeValidator();
        assertAll("reference equality checks", () -> assertSame(first, second, "First and second calls should return the same instance"), () -> assertSame(second, third, "Second and third calls should return the same instance"));
    }

    @ParameterizedTest(name = "[{index}] $ref resolution")
    @MethodSource("refResolutionProvider")
    @DisplayName("$ref resolves to a schema containing a 'type' keyword")
    void testRefResolution(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        // $ref resolution happens during initialiseValidators()
        schema.initializeValidators();
        assertTrue(schema.hasTypeValidator(), "hasTypeValidator() should be true after $ref resolution");
        assertNotNull(schema.getTypeValidator(), "getTypeValidator() must be non‑null after $ref resolution");
    }

    @Test
    @DisplayName("getTypeValidator() returns null before any initialisation")
    void testGetterBeforeInitialisation() throws Exception {
        JsonSchema schema = buildSchema("{ \"type\": \"string\" }", "#");
        // No initialiseValidators() invoked.
        assertFalse(schema.hasTypeValidator(), "hasTypeValidator() should be false before initialiseValidators()");
        assertNull(schema.getTypeValidator(), "getTypeValidator() should be null before initialiseValidators()");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_getTypeValidator_Test ✔
│     ├─ getTypeValidator() returns non‑null when a valid 'type' keyword is present ✔
│     │  ├─ [1] typeValidator present – schemaPath=# ✔
│     │  ├─ [2] typeValidator present – schemaPath=#/properties/price ✔
│     │  ├─ [3] typeValidator present – schemaPath=#/properties/count ✔
│     │  ├─ [4] typeValidator present – schemaPath=#/properties/active ✔
│     │  ├─ [5] typeValidator present – schemaPath=#/properties/data ✔
│     │  ├─ [6] typeValidator present – schemaPath=#/properties/list ✔
│     │  ├─ [7] typeValidator present – schemaPath=#/properties/nullable ✔
│     │  ├─ [8] typeValidator present – schemaPath=#/properties/name ✔
│     │  └─ [9] typeValidator present – schemaPath=#/properties/code ✔
│     ├─ Repeated initialiseValidators() calls do not replace the TypeValidator ✔
│     │  └─ [1] repeated initialiseValidators() ✔
│     ├─ getTypeValidator() returns null before validators are initialised ✔
│     ├─ $ref resolves to a schema containing a 'type' keyword ✔
│     │  └─ [1] $ref resolution ✘ hasTypeValidator() should be true after $ref resolution ==> expected: <true> but was: <false>
│     ├─ getTypeValidator() returns null before any initialisation ✔
│     ├─ Multiple getTypeValidator() calls return the same instance ✔
│     │  └─ [1] consistent instance across calls ✔
│     ├─ getTypeValidator() returns a non‑null TypeValidator after initialiseValidators() ✔
│     ├─ getTypeValidator() remains non‑null after explicit initialiseValidators() ✔
│     │  └─ [1] explicit initialiseValidators() ✔
│     └─ getTypeValidator() returns null when no valid 'type' keyword is present ✔
│        ├─ [1] typeValidator absent – schemaPath=#/properties/age ✔
│        ├─ [2] typeValidator absent – schemaPath=# ✔
│        └─ [3] typeValidator absent – schemaPath=#/properties/invalid ✘ hasTypeValidator() should be false when schema does not define a valid type ==> expected: <false> but was: <true>
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:JsonSchema_getTypeValidator_Test:$ref resolves to a schema containing a 'type' keyword:[1] $ref resolution
    MethodSource [className = 'com.networknt.schema.JsonSchema_getTypeValidator_Test', methodName = 'testRefResolution', methodParameterTypes = 'java.lang.String, java.lang.String']
    => org.opentest4j.AssertionFailedError: hasTypeValidator() should be true after $ref resolution ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:211)
       com.networknt.schema.JsonSchema_getTypeValidator_Test.testRefResolution(JsonSchema_getTypeValidator_Test.java:188)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_getTypeValidator_Test:getTypeValidator() returns null when no valid 'type' keyword is present:[3] typeValidator absent – schemaPath=#/properties/invalid
    MethodSource [className = 'com.networknt.schema.JsonSchema_getTypeValidator_Test', methodName = 'testTypeValidatorAbsent', methodParameterTypes = 'java.lang.String, java.lang.String']
    => org.opentest4j.AssertionFailedError: hasTypeValidator() should be false when schema does not define a valid type ==> expected: <false> but was: <true>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertFalse.failNotFalse(AssertFalse.java:63)
       org.junit.jupiter.api.AssertFalse.assertFalse(AssertFalse.java:36)
       org.junit.jupiter.api.Assertions.assertFalse(Assertions.java:236)
       com.networknt.schema.JsonSchema_getTypeValidator_Test.testTypeValidatorAbsent(JsonSchema_getTypeValidator_Test.java:141)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 173 ms
[        10 containers found      ]
[         0 containers skipped    ]
[        10 containers started    ]
[         0 containers aborted    ]
[        10 containers successful ]
[         0 containers failed     ]
[        19 tests found           ]
[         0 tests skipped         ]
[        19 tests started         ]
[         0 tests aborted         ]
[        17 tests successful      ]
[         2 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 

return type: 
com.networknt.schema.TypeValidator

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	dependent fields:
		com.networknt.schema.TypeValidator com.networknt.schema.JsonSchema.typeValidator;
	related functions:
		method `private Map<String, JsonValidator> read(JsonNode)` returns `java.util.Map<java.lang.String, com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.getTypeValidator()`, api document: 
     * Please note that the key in {@link #validators} map is a schema path. It is
     * used in {@link com.networknt.schema.walk.DefaultKeywordWalkListenerRunner} to derive the keyword.
     ;
		method `public boolean hasTypeValidator()` returns `boolean`, related with `com.networknt.schema.JsonSchema.getTypeValidator()`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.getTypeValidator()`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
