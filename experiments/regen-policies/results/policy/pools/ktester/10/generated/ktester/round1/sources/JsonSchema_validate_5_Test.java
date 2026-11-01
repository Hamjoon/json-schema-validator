package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.OutputFormat;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.serialization.JsonMapperFactory;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_5_Test {

    /**
     * Shared objects that can be reused across test methods.
     */
    private static JsonSchemaFactory schemaFactory;

    private static JsonMapperFactory mapperFactory;

    /**
     * Instance under test.
     */
    private JsonSchema jsonSchema;

    /**
     * Sample JSON instance that conforms to the schema.
     */
    private JsonNode validInstance;

    /**
     * Simple OutputFormat that collects messages into a {@link Set}.
     */
    private static final OutputFormat<Set<ValidationMessage>> SET_FORMAT = messages -> {
        Set<ValidationMessage> set = new LinkedHashSet<>(messages);
        return set;
    };

    /**
     * OutputFormat that serialises the messages to a JSON string.
     */
    private static final OutputFormat<String> JSON_STRING_FORMAT = messages -> {
        ArrayNode array = mapperFactory.objectMapper().createArrayNode();
        for (ValidationMessage vm : messages) {
            ObjectNode node = mapperFactory.objectMapper().createObjectNode();
            node.put("message", vm.getMessage());
            node.put("path", vm.getPath());
            array.add(node);
        }
        try {
            return mapperFactory.objectMapper().writeValueAsString(array);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    /**
     * OutputFormat that deliberately throws a RuntimeException.
     */
    private static final OutputFormat<Object> THROWING_FORMAT = messages -> {
        throw new RuntimeException("formatter failure");
    };

    /**
     * OutputFormat whose apply method throws a checked IOException.
     */
    private static final OutputFormat<Object> IOEXCEPTION_FORMAT = messages -> {
        throw new IOException("simulated IO problem");
    };

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the factories once for the whole test suite.
        schemaFactory = JsonSchemaFactory.getInstance();
        mapperFactory = JsonMapperFactory.getInstance();
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // GIVEN: a simple JSON schema that requires an object with a single
        //        string property called "name".
        // --------------------------------------------------------------------
        String schemaJson = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }
            """;
        JsonNode schemaNode = mapperFactory.readTree(schemaJson);
        // JsonSchemaFactory builds a JsonSchema instance using the public API.
        jsonSchema = schemaFactory.getSchema(schemaNode);
        // --------------------------------------------------------------------
        // AND: a valid JSON document that satisfies the schema.
        // --------------------------------------------------------------------
        String validJson = """
            {
              "name": "John Doe"
            }
            """;
        validInstance = mapperFactory.readTree(validJson);
    }

    /**
     * Helper that builds a {@link JsonSchema} from a JSON schema string.
     */
    private static JsonSchema buildSchema(String schemaJson) throws IOException {
        JsonNode schemaNode = mapperFactory.readTree(schemaJson);
        return schemaFactory.getSchema(schemaNode);
    }

    // Example of how a parameterized test could be added in the future.
    static Stream<Arguments> normalValidationProvider() throws Exception {
        JsonNode valid = mapperFactory.readTree("{\"name\":\"John\",\"age\":30}");
        JsonNode missing = mapperFactory.readTree("{\"age\":30}");
        return Stream.of(Arguments.of(valid, OutputFormat.DEFAULT, Collections.emptyList()), Arguments.of(missing, OutputFormat.DEFAULT, List.of("required")));
    }

    @Test
    void shouldReturnEmptyResultWhenInstanceIsValid() {
        // --------------------------------------------------------------------
        // WHEN: the validate(JsonNode, OutputFormat) method is invoked with the
        //       default output format.
        // --------------------------------------------------------------------
        Set<ValidationMessage> result = jsonSchema.validate(validInstance, OutputFormat.DEFAULT);
        // --------------------------------------------------------------------
        // THEN: the validation result must be empty (i.e., no errors).
        // --------------------------------------------------------------------
        assertNotNull(result, "Result set should never be null");
        assertTrue(result.isEmpty(), "Expected no validation errors for a valid instance");
    }

    @Test
    void shouldReturnEmptyListWhenInstanceIsValid() {
        // GIVEN a default list‑based OutputFormat
        OutputFormat<List<ValidationMessage>> format = OutputFormat.DEFAULT;
        // WHEN validation is performed
        List<ValidationMessage> result = jsonSchema.validate(validInstance, format);
        // THEN no validation messages are returned
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Expected no validation errors for a valid instance");
    }

    @Test
    void shouldReturnMissingPropertyMessageWhenRequiredIsAbsent() {
        // GIVEN a JSON object missing the required "name" property
        JsonNode missingName = mapperFactory.readTree("{\"age\":30}");
        // AND a list‑based OutputFormat
        OutputFormat<List<ValidationMessage>> format = OutputFormat.DEFAULT;
        // WHEN validation is performed
        List<ValidationMessage> result = jsonSchema.validate(missingName, format);
        // THEN a single ValidationMessage about the missing property is present
        assertNotNull(result);
        assertEquals(1, result.size());
        ValidationMessage msg = result.get(0);
        assertEquals("required", msg.getType());
        assertTrue(msg.getMessage().contains("name"));
    }

    @Test
    void shouldThrowNPEWhenRootNodeIsNull() {
        OutputFormat<List<ValidationMessage>> format = OutputFormat.DEFAULT;
        assertThrows(NullPointerException.class, () -> jsonSchema.validate((JsonNode) null, format));
    }

    @Test
    void shouldThrowNPEWhenOutputFormatIsNull() {
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(validInstance, null));
    }

    @Test
    void shouldCollectMessagesIntoSetWhenUsingSetFormat() throws Exception {
        JsonNode missingName = mapperFactory.readTree("{\"age\":30}");
        Set<ValidationMessage> result = jsonSchema.validate(missingName, SET_FORMAT);
        assertEquals(1, result.size());
        ValidationMessage vm = result.iterator().next();
        assertEquals("required", vm.getType());
    }

    @Test
    void shouldReturnJsonStringWhenUsingStringFormat() throws Exception {
        // JSON where "age" is a string instead of integer → type mismatch
        JsonNode badAge = mapperFactory.readTree("{\"name\":\"John\",\"age\":\"thirty\"}");
        String json = jsonSchema.validate(badAge, JSON_STRING_FORMAT);
        // The string should be a JSON array with at least one element
        assertTrue(json.startsWith("["));
        assertTrue(json.contains("instance type is not integer"));
    }

    @Test
    void shouldPropagateRuntimeExceptionFromOutputFormat() {
        assertThrows(RuntimeException.class, () -> jsonSchema.validate(validInstance, THROWING_FORMAT));
    }

    @Test
    void shouldWrapCheckedExceptionFromOutputFormatInRuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.validate(validInstance, IOEXCEPTION_FORMAT));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("simulated IO problem", ex.getCause().getMessage());
    }

    @Test
    void shouldAcceptCorrectIntegerTypeForAge() {
        JsonNode good = mapperFactory.readTree("{\"age\":25}");
        List<ValidationMessage> result = jsonSchema.validate(good, OutputFormat.DEFAULT);
        // No required property "name", but we are only testing type validator;
        // the schema still requires "name", so we expect a required‑message.
        // To isolate the type test we add the required property.
        JsonNode goodWithName = mapperFactory.readTree("{\"name\":\"John\",\"age\":25}");
        List<ValidationMessage> result2 = jsonSchema.validate(goodWithName, OutputFormat.DEFAULT);
        assertTrue(result2.isEmpty(), "No messages expected when types match and required property present");
    }

    @Test
    void shouldReportTypeMismatchWhenAgeIsString() {
        JsonNode bad = mapperFactory.readTree("{\"name\":\"John\",\"age\":\"25\"}");
        List<ValidationMessage> result = jsonSchema.validate(bad, OutputFormat.DEFAULT);
        assertEquals(1, result.size());
        ValidationMessage vm = result.get(0);
        assertEquals("type", vm.getType());
        assertTrue(vm.getMessage().contains("integer"));
    }

    @Test
    void shouldValidateSuccessfullyWhenUsingInternalRef() throws Exception {
        // Schema with an internal $ref to "#/definitions/address"
        String schemaWithRef = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "address": { "$ref": "#/definitions/address" }
              },
              "definitions": {
                "address": {
                  "type": "object",
                  "properties": {
                    "city": { "type": "string" },
                    "zip":  { "type": "integer" }
                  },
                  "required": ["city","zip"]
                }
              },
              "required": ["address"]
            }
            """;
        JsonSchema refSchema = buildSchema(schemaWithRef);
        JsonNode validAddress = mapperFactory.readTree("""
            {
              "address": {
                "city": "NY",
                "zip": 10001
              }
            }
            """);
        List<ValidationMessage> result = refSchema.validate(validAddress, OutputFormat.DEFAULT);
        assertTrue(result.isEmpty(), "Valid address should produce no errors");
    }

    @Test
    void shouldReportMultipleErrorsInsideRef() throws Exception {
        String schemaWithRef = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "address": { "$ref": "#/definitions/address" }
              },
              "definitions": {
                "address": {
                  "type": "object",
                  "properties": {
                    "city": { "type": "string" },
                    "zip":  { "type": "integer" }
                  },
                  "required": ["city","zip"]
                }
              },
              "required": ["address"]
            }
            """;
        JsonSchema refSchema = buildSchema(schemaWithRef);
        JsonNode invalidAddress = mapperFactory.readTree("""
            {
              "address": {
                "city": 123,
                "zip": "10001"
              }
            }
            """);
        List<ValidationMessage> result = refSchema.validate(invalidAddress, OutputFormat.DEFAULT);
        assertEquals(2, result.size(), "Both city and zip have type mismatches");
    }

    @Test
    void shouldValidateFiniteRecursiveStructure() throws Exception {
        // Schema that uses a recursive anchor to allow nested "node" objects.
        String recursiveSchema = """
            {
              "$schema": "http://json-schema.org/draft-2019-09/schema#",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "node": { "$recursiveRef": "#" }
              }
            }
            """;
        JsonSchema schema = buildSchema(recursiveSchema);
        JsonNode instance = mapperFactory.readTree("""
            {
              "node": {
                "node": {
                  "node": null
                }
              }
            }
            """);
        List<ValidationMessage> result = schema.validate(instance, OutputFormat.DEFAULT);
        assertTrue(result.isEmpty(), "Finite recursion should validate without errors");
    }

    @Test
    void shouldPropagateJsonSchemaExceptionWhenRefIsInvalid() {
        // $ref points to a non‑existent external file.
        String badRefSchema = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "$ref": "nonexistent-schema.json"
            }
            """;
        JsonSchema schema = assertDoesNotThrow(() -> buildSchema(badRefSchema));
        JsonNode anyNode = mapperFactory.readTree("{\"dummy\":1}");
        // Validation triggers the resolution and should throw JsonSchemaException.
        assertThrows(JsonSchemaException.class, () -> schema.validate(anyNode, OutputFormat.DEFAULT));
    }

    @Test
    void shouldPropagateJsonSchemaExceptionForUnknownKeyword() {
        String unknownKeywordSchema = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "unknownKeyword": true
            }
            """;
        // Building the schema itself throws the exception.
        assertThrows(JsonSchemaException.class, () -> buildSchema(unknownKeywordSchema));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenSchemaIsNotInitialized() throws Exception {
        // Use reflection to instantiate JsonSchema without calling the factory.
        java.lang.reflect.Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        JsonSchema uninitialized = ctor.newInstance();
        // The internal validator list is null; calling validate should fail.
        assertThrows(IllegalStateException.class, () -> uninitialized.validate(validInstance, OutputFormat.DEFAULT));
    }

    @Test
    @Disabled("StackOverflowError is JVM‑dependent; this test demonstrates intent.")
    void shouldPropagateStackOverflowErrorWhenRecursionDepthIsExceeded() throws Exception {
        // Build a schema with a recursive anchor (same as earlier).
        String recursiveSchema = """
            {
              "$schema": "http://json-schema.org/draft-2019-09/schema#",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "node": { "$recursiveRef": "#" }
              }
            }
            """;
        JsonSchema schema = buildSchema(recursiveSchema);
        // Construct a deeply nested JSON (e.g., 5000 levels).
        ObjectNode root = mapperFactory.objectMapper().createObjectNode();
        ObjectNode current = root;
        for (int i = 0; i < 5000; i++) {
            ObjectNode child = mapperFactory.objectMapper().createObjectNode();
            current.set("node", child);
            current = child;
        }
        // Expect a StackOverflowError to bubble up.
        assertThrows(StackOverflowError.class, () -> schema.validate(root, OutputFormat.DEFAULT));
    }

    @ParameterizedTest
    @MethodSource("normalValidationProvider")
    void parameterizedNormalValidation(JsonNode node, OutputFormat<List<ValidationMessage>> format, List<String> expectedMessageTypes) {
        List<ValidationMessage> msgs = jsonSchema.validate(node, format);
        assertEquals(expectedMessageTypes.size(), msgs.size());
        for (int i = 0; i < expectedMessageTypes.size(); i++) {
            assertEquals(expectedMessageTypes.get(i), msgs.get(i).getType());
        }
    }
}
