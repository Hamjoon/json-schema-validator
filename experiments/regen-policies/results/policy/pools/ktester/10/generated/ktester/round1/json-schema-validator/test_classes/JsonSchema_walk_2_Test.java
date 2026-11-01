package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Set;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_2_Test {

    /**
     * The schema under test – a very simple object schema.
     */
    private JsonSchema jsonSchema;

    /**
     * Jackson mapper used to build JsonNode instances from JSON literals.
     */
    private ObjectMapper mapper;

    /* --------------------------------------------------------------
     *  Functional interface used to encapsulate per‑scenario assertions.
     * -------------------------------------------------------------- */
    @FunctionalInterface
    private interface Assertion {

        void assertResult(ValidationResult result) throws Exception;
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Initialise the mapper.
        mapper = new ObjectMapper();
        // A minimal schema: {"type":"object"}
        JsonNode schemaNode = mapper.readTree("{\"type\":\"object\"}");
        // Build a JsonSchema instance using the public factory – this gives us a fully‑initialised object
        // without having to touch any private constructors.
        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaNode);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release references – helps the GC in long test suites.
        jsonSchema = null;
        mapper = null;
    }

    /**
     * Creates a {@link JsonSchema} from a JSON schema string using Draft‑07
     * (the version used by the library in the original test suite).
     */
    private JsonSchema createSchema(String schemaJson) throws Exception {
        JsonNode schemaNode = mapper.readTree(schemaJson);
        return JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaNode);
    }

    /**
     * Parses a JSON instance literal into a {@link JsonNode}.
     */
    private JsonNode parseInstance(String json) throws Exception {
        return mapper.readTree(json);
    }

    /**
     * Parameter source for the basic validation scenarios.
     *
     * Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the schema definition (as JSON string)</li>
     *   <li>the instance to validate (as JSON string, or {@code null})</li>
     *   <li>the {@code validate} flag</li>
     *   <li>a description of the expected outcome (used only for readability)</li>
     *   <li>a lambda that asserts the expected {@link ValidationResult}</li>
     * </ul>
     */
    private static Stream<Arguments> basicScenarios() {
        return Stream.of(// 1. valid object, schema only defines type=object
        Arguments.of("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}", "{\"name\":\"John\",\"age\":30}", true, "valid instance – should succeed", (Assertion) (result) -> {
            assertTrue(result.getValidationMessages().isEmpty(), "Expected no validation messages for a valid instance");
        }), // 2. type mismatches for both properties
        Arguments.of("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}", "{\"name\":123,\"age\":\"thirty\"}", true, "type mismatches – expect two messages", (Assertion) (result) -> {
            Set<ValidationMessage> msgs = result.getValidationMessages();
            assertEquals(2, msgs.size(), "Two type‑mismatch messages expected");
            assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("name")), "Message about 'name' type mismatch expected");
            assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("age")), "Message about 'age' type mismatch expected");
        }), // 3. additionalProperties not allowed
        Arguments.of("{\"type\":\"object\",\"additionalProperties\":false}", "{\"unexpected\":\"field\"}", true, "extra property with additionalProperties:false – expect error", (Assertion) (result) -> {
            assertFalse(result.getValidationMessages().isEmpty(), "Expected at least one validation message for disallowed property");
            assertTrue(result.getValidationMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("additional")), "Message should mention 'additionalProperties'");
        }), // 4. validation disabled – any content should succeed
        Arguments.of("{\"type\":\"object\"}", "{\"any\":\"value\"}", false, "validation disabled – should return success regardless of content", (Assertion) (result) -> {
            assertTrue(result.getValidationMessages().isEmpty(), "No messages expected when validation is disabled");
        }), // 5. null instance with validation enabled – should fail
        Arguments.of("{\"type\":\"object\"}", null, true, "null instance with validation enabled – expect failure", (Assertion) (result) -> {
            assertFalse(result.getValidationMessages().isEmpty(), "Null instance should produce a validation error");
            assertTrue(result.getValidationMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("null")), "Message should indicate that the instance is null or missing");
        }), // 6. schema without required validator – missing property is ok
        Arguments.of("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}", "{\"name\":\"Alice\"}", true, "schema without required validator – missing optional fields are allowed", (Assertion) (result) -> {
            assertTrue(result.getValidationMessages().isEmpty(), "No validation messages expected when required validator is absent");
        }), // 7. type validator present and passes (object type)
        Arguments.of("{\"type\":\"object\"}", "{\"type\":\"object\"}", true, "type validator present and instance type matches", (Assertion) (result) -> {
            assertTrue(result.getValidationMessages().isEmpty(), "Valid type should produce no messages");
        }), // 8. type validator present but mismatched type
        Arguments.of("{\"type\":\"object\"}", "{\"type\":123}", true, "type validator present but instance type mismatches", (Assertion) (result) -> {
            assertFalse(result.getValidationMessages().isEmpty(), "Type mismatch should generate a validation message");
            assertTrue(result.getValidationMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")), "Message should mention a type error");
        }));
    }

    private static Stream<Arguments> requiredScenarios() {
        return Stream.of(// 1. missing required property "age"
        Arguments.of("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}},\"required\":[\"age\"]}", "{\"name\":\"Bob\"}", true, "missing required property 'age' – expect failure", (Assertion) (result) -> {
            assertFalse(result.getValidationMessages().isEmpty(), "Missing required property should produce a message");
            assertTrue(result.getValidationMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("age")), "Message should mention the missing 'age' property");
        }), // 2. all required properties present with correct types
        Arguments.of("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}},\"required\":[\"name\",\"age\"]}", "{\"name\":\"Bob\",\"age\":25}", true, "all required properties satisfied – expect success", (Assertion) (result) -> {
            assertTrue(result.getValidationMessages().isEmpty(), "No messages expected when all required fields are valid");
        }), // 3. required property present but wrong type
        Arguments.of("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}},\"required\":[\"age\"]}", "{\"name\":\"Bob\",\"age\":\"twenty\"}", true, "required property 'age' present with wrong type – expect type error only", (Assertion) (result) -> {
            Set<ValidationMessage> msgs = result.getValidationMessages();
            assertEquals(1, msgs.size(), "Only the type‑mismatch message should be present");
            assertTrue(msgs.iterator().next().getMessage().toLowerCase().contains("age"), "Message should refer to 'age' type mismatch");
        }), // 4. null instance with validation disabled – should succeed
        Arguments.of("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}},\"required\":[\"age\"]}", null, false, "validation disabled with null node – expect success", (Assertion) (result) -> {
            assertTrue(result.getValidationMessages().isEmpty(), "No messages expected when validation is disabled");
        }));
    }

    @Test
    void walk_shouldReturnValidationResult_whenValidateIsTrue() throws Exception {
        // GIVEN – a JSON instance that conforms to the schema
        JsonNode instance = mapper.readTree("{\"name\":\"John\",\"age\":30}");
        // WHEN – the walk method is invoked with validation enabled
        ValidationResult result = jsonSchema.walk(instance, true);
        // THEN – a non‑null result is returned and, for a valid instance, no messages are present
        assertNotNull(result, "walk should never return null");
        assertTrue(result.getValidationMessages().isEmpty(), "A valid instance should produce an empty set of validation messages");
    }

    @Test
    void walk_shouldReturnValidationResult_whenValidateIsFalse() throws Exception {
        // GIVEN – a JSON instance that does NOT conform to the schema (array instead of object)
        JsonNode invalidInstance = mapper.readTree("[1,2,3]");
        // WHEN – walk is called with validation disabled
        ValidationResult result = jsonSchema.walk(invalidInstance, false);
        // THEN – walk still returns a ValidationResult, but because validation was skipped
        // it should contain no messages.
        assertNotNull(result, "walk should never return null even when validation is disabled");
        assertTrue(result.getValidationMessages().isEmpty(), "When validation is disabled, walk should not report any messages");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("basicScenarios")
    void walk_basicValidationScenarios(String schemaJson, String instanceJson, boolean validate, String description, Assertion assertion) throws Exception {
        // GIVEN – a schema built from the supplied JSON definition
        JsonSchema schema = createSchema(schemaJson);
        // AND – the instance node (may be null)
        JsonNode instance = instanceJson == null ? null : parseInstance(instanceJson);
        // WHEN – walk is invoked
        ValidationResult result = schema.walk(instance, validate);
        // THEN – delegate to the scenario‑specific assertion
        assertion.assertResult(result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("requiredScenarios")
    void walk_requiredValidatorScenarios(String schemaJson, String instanceJson, boolean validate, String description, Assertion assertion) throws Exception {
        JsonSchema schema = createSchema(schemaJson);
        JsonNode instance = instanceJson == null ? null : parseInstance(instanceJson);
        ValidationResult result = schema.walk(instance, validate);
        assertion.assertResult(result);
    }
}
