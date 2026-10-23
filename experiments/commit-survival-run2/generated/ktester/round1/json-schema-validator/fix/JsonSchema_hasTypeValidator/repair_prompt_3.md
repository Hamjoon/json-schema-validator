


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
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

// @ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchema.hasTypeValidator() tests")
class JsonSchema_hasTypeValidator_Test {

    private static ObjectMapper mapper;

//     @Mock
    ValidationContext // mocked validation context
    validationContext;

//     @Mock
    JsonMetaSchema // mocked meta‑schema (returned by the context)
    metaSchema;

    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        // Use the default draft‑07 meta‑schema; the exact version is not important for these tests
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    @BeforeEach
    void setupBeforeEach() {
        // The JsonSchema constructor accesses validationContext.getMetaSchema()
        // and validationContext.getConfig(). We stub the needed calls here.
//         when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // config is optional for this test
//         when(validationContext.getConfig()).thenReturn(null);
    }

    @Test
    void hasTypeValidator_returnsTrueWhenSchemaDefinesType() throws Exception {
        // GIVEN – a schema node that contains a top‑level "type" keyword
        JsonNode schemaNode = mapper.readTree("{\"type\":\"string\"}");
        // WHEN – a JsonSchema instance is created and validators are initialised
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        jsonSchema.initializeValidators();
        // THEN – the type validator should be present
        assertTrue(jsonSchema.hasTypeValidator(), "hasTypeValidator() should return true when a 'type' keyword is defined");
    }

    @Test
    void hasTypeValidator_returnsFalseWhenSchemaDoesNotDefineType() throws Exception {
        // GIVEN – a schema node without a top‑level "type" keyword
        JsonNode schemaNode = mapper.readTree("{\"properties\":{\"name\":{\"type\":\"string\"}}}");
        // WHEN – a JsonSchema instance is created and validators are initialised
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        jsonSchema.initializeValidators();
        // THEN – the type validator should be absent
        assertFalse(jsonSchema.hasTypeValidator(), "hasTypeValidator() should return false when no top‑level 'type' keyword exists");
    }

    /**
     * Group: hasTypeValidator - typeValidator is null
     * Scenario: a freshly created JsonSchema (no initialiseValidators call) should
     * report false because the private field {@code typeValidator} is still null.
     */
    @Test
    void hasTypeValidator_isFalseWhenFieldNeverInitialised() throws Exception {
        // GIVEN – an empty schema (no "type") and no call to initializeValidators()
        JsonNode emptySchema = mapper.readTree("{}");
        JsonSchema jsonSchema = factory.getSchema(emptySchema);
        // DO NOT call jsonSchema.initializeValidators();
        // THEN – the method must report false
        assertFalse(jsonSchema.hasTypeValidator(), "hasTypeValidator() should be false when the internal typeValidator field has never been set");
    }

    /**
     * Group: hasTypeValidator - typeValidator becomes null after reflection reset
     * Scenario: after a schema with a "type" keyword is fully initialised,
     * we manually nullify the private field via reflection and verify that
     * {@code hasTypeValidator()} reflects the new state.
     */
    @Test
    void hasTypeValidator_isFalseAfterReflectiveReset() throws Exception {
        // GIVEN – a schema that defines a type and is fully initialised
        JsonNode schemaNode = mapper.readTree("{\"type\":\"integer\"}");
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        jsonSchema.initializeValidators();
        assertTrue(jsonSchema.hasTypeValidator(), "pre‑condition: typeValidator must be non‑null after init");
        // WHEN – we set the private field 'typeValidator' to null using reflection
        Field typeValidatorField = JsonSchema.class.getDeclaredField("typeValidator");
        typeValidatorField.setAccessible(true);
        typeValidatorField.set(jsonSchema, null);
        // THEN – hasTypeValidator must now return false
        assertFalse(jsonSchema.hasTypeValidator(), "hasTypeValidator() should return false after the internal typeValidator field is cleared via reflection");
    }

    /**
     * Group: hasTypeValidator - typeValidator remains null for schema without type
     * Scenario: a schema that does not contain a top‑level "type" keyword should
     * never create a {@code TypeValidator}, even after {@code initializeValidators()}.
     */
    @Test
    void hasTypeValidator_isFalseWhenSchemaLacksTypeKeyword() throws Exception {
        // GIVEN – a schema that only defines properties, no top‑level "type"
        JsonNode schemaNode = mapper.readTree("{\"properties\":{\"age\":{\"type\":\"integer\"}}}");
        JsonSchema jsonSchema = factory.getSchema(schemaNode);
        jsonSchema.initializeValidators();
        // THEN – the type validator remains absent
        assertFalse(jsonSchema.hasTypeValidator(), "hasTypeValidator() must be false when the schema does not declare a top‑level 'type' keyword");
    }

    /**
     * Group: hasTypeValidator - typeValidator true after child schema inherits type
     * Scenario: a child schema created from a parent that defines a "type" should
     * inherit the {@code TypeValidator}. We verify this by creating a child with an
     * empty schema node and checking {@code hasTypeValidator()} on the child.
     */
    @Test
    void hasTypeValidator_isTrueForChildSchemaInheritingParentType() throws Exception {
        // GIVEN – parent schema with a top‑level type
        JsonNode parentNode = mapper.readTree("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema parent = factory.getSchema(parentNode);
        parent.initializeValidators();
        assertTrue(parent.hasTypeValidator(), "pre‑condition: parent must have a type validator");
        // WHEN – create a child schema that does not declare its own type (empty object)
        JsonNode childNode = mapper.readTree("{}");
        JsonSchema child = parent.createChildSchema("#/child", childNode);
        child.initializeValidators();
        // THEN – the child should also report a non‑null type validator (inherited)
        assertTrue(child.hasTypeValidator(), "Child schema should inherit the parent's type validator and thus return true");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema.hasTypeValidator() tests ✔
│     ├─ hasTypeValidator_returnsTrueWhenSchemaDefinesType() ✔
│     ├─ hasTypeValidator_isTrueForChildSchemaInheritingParentType() ✘ Child schema should inherit the parent's type validator and thus return true ==> expected: <true> but was: <false>
│     ├─ hasTypeValidator_isFalseWhenFieldNeverInitialised() ✔
│     ├─ hasTypeValidator_returnsFalseWhenSchemaDoesNotDefineType() ✔
│     ├─ hasTypeValidator_isFalseWhenSchemaLacksTypeKeyword() ✔
│     └─ hasTypeValidator_isFalseAfterReflectiveReset() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (1):
  JUnit Jupiter:JsonSchema.hasTypeValidator() tests:hasTypeValidator_isTrueForChildSchemaInheritingParentType()
    MethodSource [className = 'com.networknt.schema.JsonSchema_hasTypeValidator_Test', methodName = 'hasTypeValidator_isTrueForChildSchemaInheritingParentType', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Child schema should inherit the parent's type validator and thus return true ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:211)
       com.networknt.schema.JsonSchema_hasTypeValidator_Test.hasTypeValidator_isTrueForChildSchemaInheritingParentType(JsonSchema_hasTypeValidator_Test.java:132)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 147 ms
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
[         5 tests successful      ]
[         1 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 

return type: 
boolean

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	dependent fields:
		com.networknt.schema.TypeValidator com.networknt.schema.JsonSchema.typeValidator;
	related functions:
		method `public boolean hasTypeValidator()` returns `boolean`, related with `com.networknt.schema.JsonSchema.hasTypeValidator()`;
		method `private Map<String, JsonValidator> read(JsonNode)` returns `java.util.Map<java.lang.String, com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.hasTypeValidator()`, api document: 
     * Please note that the key in {@link #validators} map is a schema path. It is
     * used in {@link com.networknt.schema.walk.DefaultKeywordWalkListenerRunner} to derive the keyword.
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.hasTypeValidator()`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
