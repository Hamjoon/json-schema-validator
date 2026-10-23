```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode)}.
 *
 * <p>Each {@code @ParameterizedTest} covers a group of related test cases
 * described in the supplied JSON test‑case document.  The tests build a
 * {@link JsonSchema} from a small inline JSON schema that expresses the
 * constraint under test, then validate a JSON instance and assert on the
 * returned {@link ValidationMessage}s.</p>
 *
 * <p>The {@code @BeforeEach} method creates a default (empty) schema – this
 * is overridden inside the individual tests where a different schema is
 * required.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_2_Test {

    /** The schema under test – recreated in each test method as needed. */
    private JsonSchema jsonSchema;

    /** Jackson mapper used to build JSON payloads for the tests. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // default empty schema – will be replaced in tests that need a specific schema
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        jsonSchema = factory.getSchema("{}");
        assertNotNull(jsonSchema, "JsonSchema instance must be created");
    }

    @AfterEach
    void teardownAfterEach() {
        jsonSchema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /*---------------------------------------------------------------------*/
    /* Helper utilities                                                   */
    /*---------------------------------------------------------------------*/

    /** Parses a JSON string into a {@link JsonNode}. */
    private static JsonNode node(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON supplied to test", e);
        }
    }

    /** Creates a {@link JsonSchema} from the supplied schema JSON. */
    private static JsonSchema schema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        return factory.getSchema(schemaJson);
    }

    /** Returns the textual representation of the first validation message, or null if none. */
    private static String firstMessage(Set<ValidationMessage> messages) {
        return messages.isEmpty() ? null : messages.iterator().next().getMessage();
    }

    /*---------------------------------------------------------------------*/
    /* 1. Null root node – should be treated as empty document (empty set) */
    /*---------------------------------------------------------------------*/

    @Test
    void nullRootNodeShouldYieldEmptySet() {
        // The current implementation throws NPE, but the specification for this
        // test suite expects the validator to treat null as an empty JSON document.
        // We therefore guard the call and assert the empty‑set contract.
        Set<ValidationMessage> messages = assertDoesNotThrow(() -> jsonSchema.validate(null));
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "Null root node should produce no validation messages");
    }

    /*---------------------------------------------------------------------*/
    /* 2. Required property validation                                    */
    /*---------------------------------------------------------------------*/

    @ParameterizedTest(name = "[{index}] required‑property: {0}")
    @MethodSource("requiredPropertyProvider")
    void requiredPropertyValidation(String description, String schemaJson,
                                    String instanceJson, boolean expectEmpty,
                                    String expectedFragment) {
        jsonSchema = schema(schemaJson);
        Set<ValidationMessage> messages = jsonSchema.validate(node(instanceJson));

        if (expectEmpty) {
            assertTrue(messages.isEmpty(), description + " – expected no messages");
        } else {
            assertFalse(messages.isEmpty(), description + " – expected at least one message");
            assertTrue(firstMessage(messages).contains(expectedFragment),
                    description + " – message should contain '" + expectedFragment + "'");
        }
    }

    private static Stream<Arguments> requiredPropertyProvider() {
        String schema = """
                {
                  "type":"object",
                  "required":["name"]
                }
                """;
        return Stream.of(
                Arguments.of("required property present",
                        schema,
                        """{ "name": "John" }""",
                        true,
                        null),
                Arguments.of("required property missing",
                        schema,
                        """{ }""",
                        false,
                        "required property 'name'")
        );
    }

    /*---------------------------------------------------------------------*/
    /* 3. Type validation                                                 */
    /*---------------------------------------------------------------------*/

    @ParameterizedTest(name = "[{index}] type‑validation: {0}")
    @MethodSource("typeValidationProvider")
    void typeValidation(String description, String schemaJson,
                        String instanceJson, boolean expectEmpty,
                        String expectedFragment) {
        jsonSchema = schema(schemaJson);
        Set<ValidationMessage> messages = jsonSchema.validate(node(instanceJson));

        if (expectEmpty) {
            assertTrue(messages.isEmpty(), description + " – expected no messages");
        } else {
            assertFalse(messages.isEmpty(), description + " – expected validation error");
            assertTrue(firstMessage(messages).contains(expectedFragment),
                    description + " – message should contain '" + expectedFragment + "'");
        }
    }

    private static Stream<Arguments> typeValidationProvider() {
        String schema = """
                {
                  "type":"object",
                  "properties":{
                    "age":{"type":"integer"}
                  }
                }
                """;
        return Stream.of(
                Arguments.of("integer matches type",
                        schema,
                        """{ "age": 30 }""",
                        true,
                        null),
                Arguments.of("string violates integer type",
                        schema,
                        """{ "age": "thirty" }""",
                        false,
                        "type mismatch")
        );
    }

    /*---------------------------------------------------------------------*/
    /* 4. Combined required + type validation                              */
    /*---------------------------------------------------------------------*/

    @ParameterizedTest(name = "[{index}] combined required+type: {0}")
    @MethodSource("combinedRequiredTypeProvider")
    void combinedRequiredAndType(String description, String schemaJson,
                                 String instanceJson, int expectedMessageCount,
                                 List<String> expectedFragments) {
        jsonSchema = schema(schemaJson);
        Set<ValidationMessage> messages = jsonSchema.validate(node(instanceJson));

        assertEquals(expectedMessageCount, messages.size(),
                description + " – unexpected number of messages");

        // Verify each expected fragment appears in at least one message
        for (String fragment : expectedFragments) {
            boolean found = messages.stream()
                    .anyMatch(m -> m.getMessage().contains(fragment));
            assertTrue(found, description + " – expected fragment '" + fragment + "' not found");
        }
    }

    private static Stream<Arguments> combinedRequiredTypeProvider() {
        String schema = """
                {
                  "type":"object",
                  "required":["name"],
                  "properties":{
                    "name":{"type":"string"},
                    "age":{"type":"integer"}
                  }
                }
                """;
        return Stream.of(
                Arguments.of("both constraints satisfied",
                        schema,
                        """{ "name": "Alice", "age": 25 }""",
                        0,
                        Collections.emptyList()),
                Arguments.of("missing required and wrong type",
                        schema,
                        """{ "age": "twenty" }""",
                        2,
                        List.of("required property 'name'", "type mismatch"))
        );
    }

    /*---------------------------------------------------------------------*/
    /* 5. $ref resolution                                                  */
    /*---------------------------------------------------------------------*/

    @ParameterizedTest(name = "[{index}] $ref resolution: {0}")
    @MethodSource("refResolutionProvider")
    void refResolution(String description, String schemaJson,
                       String instanceJson, boolean expectEmpty,
                       String expectedFragment) {
        jsonSchema = schema(schemaJson);
        Set<ValidationMessage> messages = jsonSchema.validate(node(instanceJson));

        if (expectEmpty) {
            assertTrue(messages.isEmpty(), description + " – expected no messages");
        } else {
            assertFalse(messages.isEmpty(), description + " – expected validation error");
            assertTrue(firstMessage(messages).contains(expectedFragment),
                    description + " – message should contain '" + expectedFragment + "'");
        }
    }

    private static Stream<Arguments> refResolutionProvider() {
        String schema = """
                {
                  "$ref":"#/definitions/address",
                  "definitions":{
                    "address":{
                      "type":"object",
                      "required":["city"],
                      "properties":{
                        "city":{"type":"string"},
                        "zip":{"type":"integer"}
                      }
                    }
                  }
                }
                """;
        return Stream.of(
                Arguments.of("valid address object",
                        schema,
                        """{ "address": { "city": "NY" } }""",
                        true,
                        null),
                Arguments.of("missing required property in referenced schema",
                        schema,
                        """{ "address": { "zip": 12345 } }""",
                        false,
                        "required property 'city'")
        );
    }

    /*---------------------------------------------------------------------*/
    /* 6. AdditionalProperties = false                                      */
    /*---------------------------------------------------------------------*/

    @ParameterizedTest(name = "[{index}] additionalProperties false: {0}")
    @MethodSource("additionalPropertiesProvider")
    void additionalPropertiesFalse(String description, String schemaJson,
                                   String instanceJson, boolean expectEmpty,
                                   String expectedFragment) {
        jsonSchema = schema(schemaJson);
        Set<ValidationMessage> messages = jsonSchema.validate(node(instanceJson));

        if (expectEmpty) {
            assertTrue(messages.isEmpty(), description + " – no validation messages expected");
        } else {
            assertFalse(messages.isEmpty(), description + " – validation messages expected");
            assertTrue(firstMessage(messages).contains(expectedFragment),
                    description + " – message should contain '" + expectedFragment + "'");
        }
    }

    private static Stream<Arguments> additionalPropertiesProvider() {
        String schema = """
                {
                  "type":"object",
                  "properties":{"known":{"type":"integer"}},
                  "additionalProperties":false
                }
                """;
        return Stream.of(
                Arguments.of("only defined property",
                        schema,
                        """{ "known": 1 }""",
                        true,
                        null),
                Arguments.of("extra property present",
                        schema,
                        """{ "known": 1, "extra": "value" }""",
                        false,
                        "additional property 'extra'")
        );
    }

    /*---------------------------------------------------------------------*/
    /* 7. Pattern validation                                                */
    /*---------------------------------------------------------------------*/

    @ParameterizedTest(name = "[{index}] pattern validation: {0}")
    @MethodSource("patternProvider")
    void patternValidation(String description, String schemaJson,
                           String instanceJson, boolean expectEmpty,
                           String expectedFragment) {
        jsonSchema = schema(schemaJson);
        Set<ValidationMessage> messages = jsonSchema.validate(node(instanceJson));

        if (expectEmpty) {
            assertTrue(messages.isEmpty(), description + " – expected valid pattern");
        } else {
            assertFalse(messages.isEmpty(), description + " – expected pattern mismatch");
            assertTrue(firstMessage(messages).contains(expectedFragment),
                    description + " – message should contain '" + expectedFragment + "'");
        }
    }

    private static Stream<Arguments> patternProvider() {
        String schema = """
                {
                  "type":"object",
                  "properties":{
                    "code":{"type":"string","pattern":"^[A-Z]{2}\\d{4}$"}
                  }
                }
                """;
        return Stream.of(
                Arguments.of("matching pattern",
                        schema,
                        """{ "code": "AB1234" }""",
                        true,
                        null),
                Arguments.of("non‑matching pattern",
                        schema,
                        """{ "code": "ab12" }""",
                        false,
                        "pattern mismatch")
        );
    }

    /*---------------------------------------------------------------------*/
    /* 8. Array items and size validation                                   */
    /*---------------------------------------------------------------------*/

    @ParameterizedTest(name = "[{index}] array validation: {0}")
    @MethodSource("arrayProvider")
    void arrayValidation(String description, String schemaJson,
                         String instanceJson, int expectedMessageCount,
                         List<String> expectedFragments) {
        jsonSchema = schema(schemaJson);
        Set<ValidationMessage> messages = jsonSchema.validate(node(instanceJson));

        assertEquals(expectedMessageCount, messages.size(),
                description + " – unexpected number of messages");

        for (String fragment : expectedFragments) {
            boolean found = messages.stream()
                    .anyMatch(m -> m.getMessage().contains(fragment));
            assertTrue(found, description + " – expected fragment '" + fragment + "' not found");
        }
    }

    private static Stream<Arguments> arrayProvider() {
        String schema = """
                {
                  "type":"array",
                  "minItems":1,
                  "maxItems":5,
                  "items":{"type":"integer"}
                }
                """;
        return Stream.of(
                Arguments.of("valid integer array",
                        schema,
                        "[1,2,3]",
                        0,
                        Collections.emptyList()),
                Arguments.of("type and size violations",
                        schema,
                        "[1,\"two\",3,4,5,6]",
