
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
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

    @ParameterizedTest(name = "inheritance – {3}")
    @MethodSource("inheritanceProvider")
    void isRecursiveAnchor_inheritance(String parentSchemaJson, String childPointer, boolean expected, String description) {
        JsonSchema parent = factory.getSchema(parentSchemaJson, config);
        JsonSchema child = parent.getSubSchema(new JsonNodePath(childPointer, PathType.JSON_POINTER));
        assertNotNull(child, "Child schema should be resolved for pointer " + childPointer);
        assertEquals(expected, child.isRecursiveAnchor(), () -> "Inheritance test failed: " + description);
    }

    @ParameterizedTest(name = "malformed value – schema: {0}")
    @MethodSource("malformedProvider")
    void isRecursiveAnchor_malformedValue(String schemaJson) {
        // The factory must tolerate non‑boolean values and treat them as absent.
        JsonSchema schema = factory.getSchema(schemaJson, config);
        assertFalse(schema.isRecursiveAnchor(), () -> "Malformed $recursiveAnchor should result in false");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_isRecursiveAnchor_Test.java:12: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_isRecursiveAnchor_Test.java:15: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_isRecursiveAnchor_Test.java:129: error: no suitable constructor found for JsonNodePath(String,PathType)
        JsonSchema child = parent.getSubSchema(new JsonNodePath(childPointer, PathType.JSON_POINTER));
                                               ^
    constructor JsonNodePath.JsonNodePath(JsonNodePath,String) is not applicable
      (argument mismatch; String cannot be converted to JsonNodePath)
    constructor JsonNodePath.JsonNodePath(JsonNodePath,int) is not applicable
      (argument mismatch; String cannot be converted to JsonNodePath)
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
3 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'boolean', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tboolean com.networknt.schema.JsonSchema.recursiveAnchor;\n\trelated functions:\n\t\tmethod `private List<JsonValidator> read(JsonNode)` returns `java.util.List<com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.isRecursiveAnchor()`, api document: \n     * Please note that the key in {@link #validators} map is the evaluation path.\n     ;\n\t\tmethod `public JsonSchema fromRef(JsonSchema, JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isRecursiveAnchor()`, api document: \n     * Creates a schema using the current one as a template with the parent as the\n     * ref.\n     * <p>\n     * This is typically used if this schema is a schema resource that can be\n     * pointed to by various references.\n     *\n     * @param refEvaluationParentSchema the parent ref\n     * @param refEvaluationPath the ref evaluation path\n     * @return the schema\n     ;\n\t\tmethod `public JsonSchema withConfig(SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isRecursiveAnchor()`;\n\t\tmethod `public boolean isRecursiveAnchor()` returns `boolean`, related with `com.networknt.schema.JsonSchema.isRecursiveAnchor()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.isRecursiveAnchor()`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


