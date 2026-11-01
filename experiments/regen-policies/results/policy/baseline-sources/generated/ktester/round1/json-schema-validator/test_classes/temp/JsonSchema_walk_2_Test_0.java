package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_2_Test {

    /**
     * Mocks required for constructing a JsonSchema instance
     */
    @Mock
    ValidationContext mockValidationContext;

    @Mock
    ExecutionContext mockExecutionContext;

    @Mock
    ValidationResult mockValidationResult;

    /**
     * Real objects used as arguments
     */
    private JsonNode dummySchemaNode;

    private JsonNode dummyInputNode;

    /**
     * The JsonSchema under test – spied so we can stub internal methods
     */
    private JsonSchema jsonSchemaSpy;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("Required property & type validation")
    class RequiredAndTypeTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\"," + "  \"properties\":{" + "    \"name\":{\"type\":\"string\"}," + "    \"age\":{\"type\":\"integer\"}" + "  }," + "  \"required\":[\"name\",\"age\"]" + "}");

        @ParameterizedTest(name = "[{index}] shouldValidate={0} expectedSuccess={1}")
        @MethodSource("validAndInvalidProvider")
        void walk_requiredAndType(boolean shouldValidate, boolean expectedSuccess, String instanceJson, String... expectedMessages) {
            JsonNode instance = nodeFromString(instanceJson);
            ValidationResult result = schema.walk(instance, shouldValidate);
            assertEquals(expectedSuccess, result.isSuccess(), () -> "Expected success=" + expectedSuccess + " but got messages: " + result.getMessages());
            // When we expect failures, make sure at least one message contains the expected text.
            if (!expectedSuccess && expectedMessages.length > 0) {
                for (String exp : expectedMessages) {
                    assertTrue(result.getMessages().stream().anyMatch(msg -> msg.getMessage().contains(exp)), "Expected a message containing: " + exp);
                }
            }
        }

        static Stream<Arguments> validAndInvalidProvider() {
            return Stream.of(// valid object – both required properties present and correct types
            Arguments.of(true, true, "{\"name\":\"John\",\"age\":30}"), // type mismatch for both properties
            Arguments.of(true, false, "{\"name\":123,\"age\":\"thirty\"}", "type", "type"), // missing required property "name"
            Arguments.of(true, false, "{\"age\":25}", "required"), // shouldValidate = false – validation is bypassed, always success
            Arguments.of(false, true, "{\"unexpected\":\"value\"}"));
        }
    }

    @Nested
    @DisplayName("Enum validation")
    class EnumTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"string\", \"enum\":[\"RED\",\"GREEN\",\"BLUE\"] }");

        @Test
        void walk_enumSuccess() {
            ValidationResult result = schema.walk(nodeFromString("\"RED\""), true);
            assertTrue(result.isSuccess(), "Enum value RED should be valid");
        }

        @Test
        void walk_enumFailure() {
            ValidationResult result = schema.walk(nodeFromString("\"YELLOW\""), true);
            assertFalse(result.isSuccess(), "Enum value YELLOW should be invalid");
            assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().contains("enum")), "Expected an enum‑validation message");
        }
    }

    @Nested
    @DisplayName("PatternProperties and format validation")
    class PatternAndFormatTests {

        private final JsonSchema emailPatternSchema = schemaFromString("{ \"type\":\"object\"," + "  \"patternProperties\":{" + "    \"^email$\":{ \"type\":\"string\", \"format\":\"email\" }" + "  }" + "}");

        private final JsonSchema dateTimeSchema = schemaFromString("{ \"type\":\"string\", \"format\":\"date-time\" }");

        @Test
        void walk_emailPatternSuccess() {
            ValidationResult result = emailPatternSchema.walk(nodeFromString("{\"email\":\"john.doe@example.com\"}"), true);
            assertTrue(result.isSuccess(), "Valid email should pass pattern/format validation");
        }

        @Test
        void walk_emailPatternFailure() {
            ValidationResult result = emailPatternSchema.walk(nodeFromString("{\"email\":\"not-an-email\"}"), true);
            assertFalse(result.isSuccess(), "Invalid email should fail format validation");
            assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().contains("format")), "Expected a format‑validation message for email");
        }

        @Test
        void walk_dateTimeSuccess() {
            ValidationResult result = dateTimeSchema.walk(nodeFromString("\"2023-08-15T13:45:30Z\""), true);
            assertTrue(result.isSuccess(), "Valid RFC‑3339 date‑time should pass");
        }

        @Test
        void walk_dateTimeFailure() {
            ValidationResult result = dateTimeSchema.walk(nodeFromString("\"15/08/2023 13:45\""), true);
            assertFalse(result.isSuccess(), "Invalid date‑time should fail");
            assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().contains("format")), "Expected a format‑validation message for date-time");
        }
    }

    @Nested
    @DisplayName("$ref resolution")
    class RefTests {

        private final JsonSchema schemaWithRef = schemaFromString("{ \"$defs\": {" + "     \"vehicle\": {" + "        \"type\":\"object\"," + "        \"properties\":{ \"type\":{\"type\":\"string\"}, \"wheels\":{\"type\":\"integer\"} }," + "        \"required\":[\"type\",\"wheels\"]" + "     }" + "  }," + "  \"$ref\":\"#/$defs/vehicle\"" + "}");

        @Test
        void walk_refResolvesSuccessfully() {
            ValidationResult result = schemaWithRef.walk(nodeFromString("{\"type\":\"car\",\"wheels\":4}"), true);
            assertTrue(result.isSuccess(), "Instance should satisfy the referenced vehicle definition");
        }

        @Test
        void walk_refUnresolvedFails() {
            JsonSchema badRefSchema = schemaFromString("{ \"$ref\":\"#/nonexistent\" }");
            ValidationResult result = badRefSchema.walk(nodeFromString("{\"any\":\"value\"}"), true);
            assertFalse(result.isSuccess(), "Unresolved $ref must produce a validation error");
            assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().contains("$ref")), "Expected a $ref‑resolution error message");
        }
    }

    @Nested
    @DisplayName("Boolean schemas")
    class BooleanSchemaTests {

        private final JsonSchema schemaTrue = schemaFromString("true");

        private final JsonSchema schemaFalse = schemaFromString("false");

        @Test
        void walk_booleanTrueAlwaysValid() {
            ValidationResult result = schemaTrue.walk(nodeFromString("null"), true);
            assertTrue(result.isSuccess(), "Boolean true schema must accept any instance");
        }

        @Test
        void walk_booleanFalseAlwaysInvalid() {
            ValidationResult result = schemaFalse.walk(nodeFromString("\"anything\""), true);
            assertFalse(result.isSuccess(), "Boolean false schema must reject all instances");
            assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().contains("false")), "Expected a message indicating failure against a false schema");
        }
    }

    @Nested
    @DisplayName("additionalProperties handling")
    class AdditionalPropertiesTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\"," + "  \"properties\":{ \"id\":{\"type\":\"integer\"} }," + "  \"required\":[\"id\"]," + "  \"additionalProperties\":false" + "}");

        @Test
        void walk_additionalPropAllowedFails() {
            ValidationResult result = schema.walk(nodeFromString("{\"id\":1,\"extra\":\"not allowed\"}"), true);
            assertFalse(result.isSuccess(), "Extra property should trigger additionalProperties validation error");
            assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().contains("additionalProperties")), "Expected an additionalProperties validation message");
        }
    }

    @Nested
    @DisplayName("anyOf validation")
    class AnyOfTests {

        private final JsonSchema schema = schemaFromString("{ \"anyOf\":[" + "   { \"type\":\"string\" }," + "   { \"type\":\"integer\" }" + "]}");

        @Test
        void walk_anyOfMatchesInteger() {
            ValidationResult result = schema.walk(nodeFromString("42"), true);
            assertTrue(result.isSuccess(), "Integer instance should satisfy anyOf");
        }

        @Test
        void walk_anyOfNoMatch() {
            ValidationResult result = schema.walk(nodeFromString("true"), true);
            assertFalse(result.isSuccess(), "Boolean instance should not match any anyOf subschema");
            assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().contains("anyOf")), "Expected an anyOf‑validation error message");
        }
    }

    @Nested
    @DisplayName("Recursive (self‑referencing) schema")
    class RecursiveSchemaTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\"," + "  \"properties\":{" + "    \"value\":{\"type\":\"integer\"}," + "    \"next\":{\"anyOf\":[{\"type\":\"null\"},{\"$ref\":\"#\"}]}" + "  },\"required\":[\"value\",\"next\"]}");

        @Test
        void walk_recursiveValid() {
            ValidationResult result = schema.walk(nodeFromString("{\"value\":1,\"next\":{\"value\":2,\"next\":null}}"), true);
            assertTrue(result.isSuccess(), "Valid recursive structure should succeed");
        }

        @Test
        void walk_recursiveInvalid() {
            ValidationResult result = schema.walk(nodeFromString("{\"value\":\"not an int\",\"next\":{}}"), true);
            assertFalse(result.isSuccess(), "Invalid recursive instance should fail");
            // expect a type‑mismatch for "value" and a missing‑required for nested "next.value"
            assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().contains("type") && m.getPath().contains("value")), "Expected a type‑mismatch message for root 'value'");
            assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().contains("required") && m.getPath().contains("next.value")), "Expected a required‑property message for 'next.value'");
        }
    }

    @Nested
    @DisplayName("Null instance node")
    class NullInstanceTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\" }");

        @Test
        void walk_nullNodeWhenValidateTrue() {
            ValidationResult result = schema.walk(null, true);
            assertFalse(result.isSuccess(), "Null instance should be reported as invalid");
            assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("null")), "Expected a message indicating the instance node is null");
        }

        @Test
        void walk_nullNodeWhenValidateFalse() {
            // When validation is disabled the implementation still returns a
            // ValidationResult – by contract it should be successful.
            ValidationResult result = schema.walk(null, false);
            assertTrue(result.isSuccess(), "When validation is disabled, null instance is considered successful");
        }
    }

    @Nested
    @DisplayName("Unsupported schema node type")
    class UnsupportedSchemaNodeTests {

        @Test
        void walk_schemaNodeIsNumberThrows() {
            // Build a JsonSchema with a numeric literal as the schema node.
            JsonNode numericSchemaNode = MAPPER.getNodeFactory().numberNode(123);
            JsonSchema malformedSchema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(numericSchemaNode);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> malformedSchema.walk(nodeFromString("\"any\""), true));
            assertTrue(ex.getMessage().contains("Schema node must be an object or boolean"), "Exception message should mention the unsupported schema node type");
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create simple JSON nodes (empty objects are sufficient for the delegation test)
        ObjectMapper mapper = new ObjectMapper();
        dummySchemaNode = mapper.createObjectNode();
        dummyInputNode = mapper.createObjectNode();
        // Build a JsonSchema instance via the public factory method.
        // The constructor is private, but `from` is the intended entry point.
        JsonSchema realSchema = JsonSchema.from(mockValidationContext, // arbitrary schema path
        "schemaPath", // base URI – not used in this test
        URI.create("http://example.com"), dummySchemaNode, // no parent schema
        null, // do not suppress sub‑schema retrieval
        false);
        // Spy the real instance so we can control the behaviour of the protected methods.
        jsonSchemaSpy = Mockito.spy(realSchema);
        // Stub the creation of the ExecutionContext – the public walk method delegates to it.
        doReturn(mockExecutionContext).when(jsonSchemaSpy).createExecutionContext();
        // Stub the overloaded walk method that receives the ExecutionContext.
        // This is the method we *actually* want to verify is called.
        doReturn(mockValidationResult).when(jsonSchemaSpy).walk(eq(mockExecutionContext), any(JsonNode.class), anyBoolean());
    }

    /**
     * Helper to create a JsonSchema from a JSON schema string.
     */
    private static JsonSchema schemaFromString(String schemaJson) {
        try {
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            // JsonSchemaFactory creates a fully‑initialised JsonSchema instance.
            return JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaNode);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Helper to parse an instance JSON string into a JsonNode.
     */
    private static JsonNode nodeFromString(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void walk_delegatesToExecutionContextVersion_andReturnsResult_whenShouldValidateTrue() {
        // Act
        ValidationResult result = jsonSchemaSpy.walk(dummyInputNode, true);
        // Assert – the result must be the stubbed ValidationResult
        assertSame(mockValidationResult, result, "walk(JsonNode, true) should return the delegated ValidationResult");
        // Verify that the internal overload was invoked with the correct parameters
        verify(jsonSchemaSpy, times(1)).walk(eq(mockExecutionContext), eq(dummyInputNode), eq(true));
    }

    @Test
    void walk_delegatesToExecutionContextVersion_andReturnsResult_whenShouldValidateFalse() {
        // Act
        ValidationResult result = jsonSchemaSpy.walk(dummyInputNode, false);
        // Assert – the result must be the stubbed ValidationResult
        assertSame(mockValidationResult, result, "walk(JsonNode, false) should return the delegated ValidationResult");
        // Verify that the internal overload was invoked with the correct parameters
        verify(jsonSchemaSpy, times(1)).walk(eq(mockExecutionContext), eq(dummyInputNode), eq(false));
    }
}
