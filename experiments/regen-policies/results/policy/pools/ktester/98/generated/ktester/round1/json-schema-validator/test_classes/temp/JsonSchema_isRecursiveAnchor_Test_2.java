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