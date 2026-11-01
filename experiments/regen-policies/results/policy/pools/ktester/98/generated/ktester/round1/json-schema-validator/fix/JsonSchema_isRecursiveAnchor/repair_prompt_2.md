


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_isRecursiveAnchor_Test {

    private static JsonSchemaFactory factory;

    private static SchemaValidatorsConfig config;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a factory for Draft‑2020‑12 which supports the $recursiveAnchor keyword
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Use the default validator configuration
        config = SchemaValidatorsConfig.builder().build();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources
        factory = null;
        config = null;
    }

    private static Stream<Arguments> basicBehaviourProvider() {
        return Stream.of(// freshly constructed schema – default should be false
        Arguments.of(false, "default"), // set true via reflection
        Arguments.of(true, "setTrue"), // set false via reflection
        Arguments.of(false, "setFalse"));
    }

    private static Stream<Arguments> recursiveAnchorTrueProvider() {
        return Stream.of(Arguments.of("{ \"$recursiveAnchor\": true }"), Arguments.of("{ \"type\": \"object\", \"$recursiveAnchor\": true, \"properties\": { \"id\": { \"type\": \"string\" } } }"));
    }

    private static Stream<Arguments> recursiveAnchorFalseProvider() {
        return Stream.of(Arguments.of("{ \"$recursiveAnchor\": false }"), Arguments.of("{ \"type\": \"array\", \"$recursiveAnchor\": false, \"items\": { \"type\": \"integer\" } }"));
    }

    private static Stream<Arguments> noKeywordProvider() {
        return Stream.of(Arguments.of("{ \"type\": \"string\" }"), Arguments.of("{ \"allOf\": [ { \"type\": \"number\" }, { \"type\": \"null\" } ] }"));
    }

    private static Stream<Arguments> inheritanceProvider() {
        return Stream.of(// Child does NOT declare $recursiveAnchor – should be false even though parent is true
        Arguments.of("{ \"$recursiveAnchor\": true, \"type\": \"object\", \"properties\": { \"child\": { \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } } } }", "/properties/child", false, "Child inherits false when it does not declare the keyword"), // Child declares $recursiveAnchor:true – should be true
        Arguments.of("{ \"$recursiveAnchor\": true, \"type\": \"object\", \"properties\": { \"child\": { \"$recursiveAnchor\": true, \"type\": \"object\" } } }", "/properties/child", true, "Child declares true and should report true regardless of parent"));
    }

    private static Stream<Arguments> malformedProvider() {
        return Stream.of(Arguments.of("{ \"$recursiveAnchor\": \"yes\" }"), Arguments.of("{ \"$recursiveAnchor\": null }"));
    }

    @Test
    void isRecursiveAnchor_true_whenKeywordPresent() throws Exception {
        // given: a schema that defines $recursiveAnchor:true
        String schemaData = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"$id\": \"http://example.org/recursive-schema.json\",\n" + "  \"$recursiveAnchor\": true,\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": { \"type\": \"string\" }\n" + "  }\n" + "}";
        JsonSchema schema = factory.getSchema(schemaData, config);
        // when
        boolean result = schema.isRecursiveAnchor();
        // then
        assertTrue(result, "Expected isRecursiveAnchor() to return true when $recursiveAnchor is present and true");
    }

    @Test
    void isRecursiveAnchor_false_whenKeywordAbsent() throws Exception {
        // given: a schema without the $recursiveAnchor keyword
        String schemaData = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"$id\": \"http://example.org/non-recursive-schema.json\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"name\": { \"type\": \"string\" }\n" + "  }\n" + "}";
        JsonSchema schema = factory.getSchema(schemaData, config);
        // when
        boolean result = schema.isRecursiveAnchor();
        // then
        assertFalse(result, "Expected isRecursiveAnchor() to return false when $recursiveAnchor is not defined");
    }

    @ParameterizedTest(name = "basic behaviour – {1}")
    @MethodSource("basicBehaviourProvider")
    void isRecursiveAnchor_basicBehaviour(boolean expected, String scenario) throws Exception {
        // Create a minimal schema (empty object) – the factory will give us a fresh instance.
        JsonSchema schema = factory.getSchema("{}", config);
        // Manipulate the private field only for the two non‑default scenarios.
        if (!"default".equals(scenario)) {
            Field f = JsonSchema.class.getDeclaredField("recursiveAnchor");
            f.setAccessible(true);
            f.set(schema, expected);
        }
        assertEquals(expected, schema.isRecursiveAnchor(), () -> "Scenario '" + scenario + "': isRecursiveAnchor() should return " + expected);
    }

    @ParameterizedTest(name = "keyword true – schema: {0}")
    @MethodSource("recursiveAnchorTrueProvider")
    void isRecursiveAnchor_keywordTrue(String schemaJson) {
        JsonSchema schema = factory.getSchema(schemaJson, config);
        assertTrue(schema.isRecursiveAnchor(), () -> "Schema " + schemaJson + " should report recursiveAnchor = true");
    }

    @ParameterizedTest(name = "keyword false – schema: {0}")
    @MethodSource("recursiveAnchorFalseProvider")
    void isRecursiveAnchor_keywordFalse(String schemaJson) {
        JsonSchema schema = factory.getSchema(schemaJson, config);
        assertFalse(schema.isRecursiveAnchor(), () -> "Schema " + schemaJson + " should report recursiveAnchor = false");
    }

    @ParameterizedTest(name = "no keyword – schema: {0}")
    @MethodSource("noKeywordProvider")
    void isRecursiveAnchor_noKeyword(String schemaJson) {
        JsonSchema schema = factory.getSchema(schemaJson, config);
        assertFalse(schema.isRecursiveAnchor(), () -> "Schema without $recursiveAnchor should report false");
    }

//     @ParameterizedTest(name = "inheritance – {3}")
//     @MethodSource("inheritanceProvider")
//     void isRecursiveAnchor_inheritance(String parentSchemaJson, String childPointer, boolean expected, String description) {
//         JsonSchema parent = factory.getSchema(parentSchemaJson, config);
//         // Build a JsonNodePath from the JSON‑Pointer string.
//         JsonNodePath path = JsonNodePath.ROOT;
//         // childPointer is of the form "/properties/child"
//         for (String token : childPointer.substring(1).split("/")) {
//             path = new JsonNodePath(path, token);
//         }
//         JsonSchema child = parent.getSubSchema(path);
//         assertNotNull(child, "Child schema should be resolved for pointer " + childPointer);
//         assertEquals(expected, child.isRecursiveAnchor(), () -> "Inheritance test failed: " + description);
//     }

    @ParameterizedTest(name = "malformed value – schema: {0}")
    @MethodSource("malformedProvider")
    void isRecursiveAnchor_malformedValue(String schemaJson) {
        // The factory must tolerate non‑boolean values and treat them as absent.
        JsonSchema schema = factory.getSchema(schemaJson, config);
        assertFalse(schema.isRecursiveAnchor(), () -> "Malformed $recursiveAnchor should result in false");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

20:01:36.891 [main] WARN com.networknt.schema.UnknownKeywordFactory -- Unknown keyword $recursiveAnchor - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword or if it should generate annotations AnnotationKeyword
╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_isRecursiveAnchor_Test ✔
│     ├─ isRecursiveAnchor_malformedValue(String) ✔
│     │  ├─ malformed value – schema: { "$recursiveAnchor": "yes" } ✘ /$recursiveAnchor: The value of a $recursiveAnchor must be a Boolean literal but is STRING
│     │  └─ malformed value – schema: { "$recursiveAnchor": null } ✘ /$recursiveAnchor: The value of a $recursiveAnchor must be a Boolean literal but is NULL
│     ├─ isRecursiveAnchor_keywordFalse(String) ✔
│     │  ├─ keyword false – schema: { "$recursiveAnchor": false } ✔
│     │  └─ keyword false – schema: { "type": "array", "$recursiveAnchor": false, "items": {... ✔
│     ├─ isRecursiveAnchor_false_whenKeywordAbsent() ✔
│     ├─ isRecursiveAnchor_basicBehaviour(boolean, String) ✔
│     │  ├─ basic behaviour – default ✔
│     │  ├─ basic behaviour – setTrue ✔
│     │  └─ basic behaviour – setFalse ✔
│     ├─ isRecursiveAnchor_keywordTrue(String) ✔
│     │  ├─ keyword true – schema: { "$recursiveAnchor": true } ✔
│     │  └─ keyword true – schema: { "type": "object", "$recursiveAnchor": true, "properties... ✔
│     ├─ isRecursiveAnchor_noKeyword(String) ✔
│     │  ├─ no keyword – schema: { "type": "string" } ✔
│     │  └─ no keyword – schema: { "allOf": [ { "type": "number" }, { "type": "null" } ] } ✔
│     └─ isRecursiveAnchor_true_whenKeywordPresent() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (2):
  JUnit Jupiter:JsonSchema_isRecursiveAnchor_Test:isRecursiveAnchor_malformedValue(String):malformed value – schema: { "$recursiveAnchor": "yes" }
    MethodSource [className = 'com.networknt.schema.JsonSchema_isRecursiveAnchor_Test', methodName = 'isRecursiveAnchor_malformedValue', methodParameterTypes = 'java.lang.String']
    => com.networknt.schema.JsonSchemaException: /$recursiveAnchor: The value of a $recursiveAnchor must be a Boolean literal but is STRING
       com.networknt.schema.JsonSchema.read(JsonSchema.java:540)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1325)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchemaFactory.doCreate(JsonSchemaFactory.java:369)
       com.networknt.schema.JsonSchemaFactory.newJsonSchema(JsonSchemaFactory.java:334)
       com.networknt.schema.JsonSchemaFactory.getSchema(JsonSchemaFactory.java:526)
       com.networknt.schema.JsonSchemaFactory.getSchema(JsonSchemaFactory.java:509)
       com.networknt.schema.JsonSchema_isRecursiveAnchor_Test.isRecursiveAnchor_malformedValue(JsonSchema_isRecursiveAnchor_Test.java:142)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       [...]
  JUnit Jupiter:JsonSchema_isRecursiveAnchor_Test:isRecursiveAnchor_malformedValue(String):malformed value – schema: { "$recursiveAnchor": null }
    MethodSource [className = 'com.networknt.schema.JsonSchema_isRecursiveAnchor_Test', methodName = 'isRecursiveAnchor_malformedValue', methodParameterTypes = 'java.lang.String']
    => com.networknt.schema.JsonSchemaException: /$recursiveAnchor: The value of a $recursiveAnchor must be a Boolean literal but is NULL
       com.networknt.schema.JsonSchema.read(JsonSchema.java:540)
       com.networknt.schema.JsonSchema.getValidators(JsonSchema.java:1325)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:151)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchemaFactory.doCreate(JsonSchemaFactory.java:369)
       com.networknt.schema.JsonSchemaFactory.newJsonSchema(JsonSchemaFactory.java:334)
       com.networknt.schema.JsonSchemaFactory.getSchema(JsonSchemaFactory.java:526)
       com.networknt.schema.JsonSchemaFactory.getSchema(JsonSchemaFactory.java:509)
       com.networknt.schema.JsonSchema_isRecursiveAnchor_Test.isRecursiveAnchor_malformedValue(JsonSchema_isRecursiveAnchor_Test.java:142)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       [...]

Test run finished after 529 ms
[         9 containers found      ]
[         0 containers skipped    ]
[         9 containers started    ]
[         0 containers aborted    ]
[         9 containers successful ]
[         0 containers failed     ]
[        13 tests found           ]
[         0 tests skipped         ]
[        13 tests started         ]
[         0 tests aborted         ]
[        11 tests successful      ]
[         2 tests failed          ]


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
	dependent fields:
		boolean com.networknt.schema.JsonSchema.recursiveAnchor;
	related functions:
		method `private List<JsonValidator> read(JsonNode)` returns `java.util.List<com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.isRecursiveAnchor()`, api document: 
     * Please note that the key in {@link #validators} map is the evaluation path.
     ;
		method `public JsonSchema fromRef(JsonSchema, JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isRecursiveAnchor()`, api document: 
     * Creates a schema using the current one as a template with the parent as the
     * ref.
     * <p>
     * This is typically used if this schema is a schema resource that can be
     * pointed to by various references.
     *
     * @param refEvaluationParentSchema the parent ref
     * @param refEvaluationPath the ref evaluation path
     * @return the schema
     ;
		method `public JsonSchema withConfig(SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isRecursiveAnchor()`;
		method `public boolean isRecursiveAnchor()` returns `boolean`, related with `com.networknt.schema.JsonSchema.isRecursiveAnchor()`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.isRecursiveAnchor()`, api document: 
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
