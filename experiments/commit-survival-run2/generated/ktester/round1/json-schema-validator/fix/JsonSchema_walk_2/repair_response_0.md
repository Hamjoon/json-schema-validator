```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.stream.Stream;

/**
 * Unit tests for {@link JsonSchema#walk(JsonNode, boolean)} covering a wide range of
 * validation scenarios. The tests use the public {@link JsonSchemaFactory} to obtain
 * fully‑initialised schema instances and then invoke {@code walk} directly.
 *
 * <p>Mockito‑based delegation tests have been removed because the project does not
 * include the Mockito dependency. The remaining tests exercise the real validation
 * logic provided by the library.</p>
 */
class JsonSchema_walk_2_Test {

    /**
     * Real objects used as arguments
     */
    private JsonNode dummySchemaNode;
    private JsonNode dummyInputNode;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("Required property & type validation")
    class RequiredAndTypeTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\"," +
                "  \"properties\":{" +
                "    \"name\":{\"type\":\"string\"}," +
                "    \"age\":{\"type\":\"integer\"}" +
                "  }," +
                "  \"required\":[\"name\",\"age\"]" +
                "}");

        @ParameterizedTest(name = "[{index}] shouldValidate={0} expectedSuccess={1}")
        @MethodSource("validAndInvalidProvider")
        void walk_requiredAndType(boolean shouldValidate, boolean expectedSuccess,
                                  String instanceJson, String... expectedMessages) {
            JsonNode instance = nodeFromString(instanceJson);
            ValidationResult result = schema.walk(instance, shouldValidate);
            Assertions.assertEquals(expectedSuccess, result.isSuccess(),
                    () -> "Expected success=" + expectedSuccess + " but got messages: " + result.getMessages());

            if (!expectedSuccess && expectedMessages.length > 0) {
                for (String exp : expectedMessages) {
                    Assertions.assertTrue(
                            result.getMessages().stream()
                                    .anyMatch(msg -> msg.getMessage().contains(exp)),
                            "Expected a message containing: " + exp);
                }
            }
        }

        static Stream<Arguments> validAndInvalidProvider() {
            return Stream.of(
                    // valid object – both required properties present and correct types
                    Arguments.of(true, true, "{\"name\":\"John\",\"age\":30}"),
                    // type mismatch for both properties
                    Arguments.of(true, false, "{\"name\":123,\"age\":\"thirty\"}", "type", "type"),
                    // missing required property "name"
                    Arguments.of(true, false, "{\"age\":25}", "required"),
                    // shouldValidate = false – validation is bypassed, always success
                    Arguments.of(false, true, "{\"unexpected\":\"value\"}")
            );
        }
    }

    @Nested
    @DisplayName("Enum validation")
    class EnumTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"string\", \"enum\":[\"RED\",\"GREEN\",\"BLUE\"] }");

        @Test
        void walk_enumSuccess() {
            ValidationResult result = schema.walk(nodeFromString("\"RED\""), true);
            Assertions.assertTrue(result.isSuccess(), "Enum value RED should be valid");
        }

        @Test
        void walk_enumFailure() {
            ValidationResult result = schema.walk(nodeFromString("\"YELLOW\""), true);
            Assertions.assertFalse(result.isSuccess(), "Enum value YELLOW should be invalid");
            Assertions.assertTrue(
                    result.getMessages().stream().anyMatch(m -> m.getMessage().contains("enum")),
                    "Expected an enum‑validation message");
        }
    }

    @Nested
    @DisplayName("PatternProperties and format validation")
    class PatternAndFormatTests {

        private final JsonSchema emailPatternSchema = schemaFromString("{ \"type\":\"object\"," +
                "  \"patternProperties\":{" +
                "    \"^email$\":{ \"type\":\"string\", \"format\":\"email\" }" +
                "  }" +
                "}");

        private final JsonSchema dateTimeSchema = schemaFromString("{ \"type\":\"string\", \"format\":\"date-time\" }");

        @Test
        void walk_emailPatternSuccess() {
            ValidationResult result = emailPatternSchema.walk(nodeFromString("{\"email\":\"john.doe@example.com\"}"), true);
            Assertions.assertTrue(result.isSuccess(), "Valid email should pass pattern/format validation");
        }

        @Test
        void walk_emailPatternFailure() {
            ValidationResult result = emailPatternSchema.walk(nodeFromString("{\"email\":\"not-an-email\"}"), true);
            Assertions.assertFalse(result.isSuccess(), "Invalid email should fail format validation");
            Assertions.assertTrue(
                    result.getMessages().stream().anyMatch(m -> m.getMessage().contains("format")),
                    "Expected a format‑validation message for email");
        }

        @Test
        void walk_dateTimeSuccess() {
            ValidationResult result = dateTimeSchema.walk(nodeFromString("\"2023-08-15T13:45:30Z\""), true);
            Assertions.assertTrue(result.isSuccess(), "Valid RFC‑3339 date‑time should pass");
        }

        @Test
        void walk_dateTimeFailure() {
            ValidationResult result = dateTimeSchema.walk(nodeFromString("\"15/08/2023 13:45\""), true);
            Assertions.assertFalse(result.isSuccess(), "Invalid date‑time should fail");
            Assertions.assertTrue(
                    result.getMessages().stream().anyMatch(m -> m.getMessage().contains("format")),
                    "Expected a format‑validation message for date-time");
        }
    }

    @Nested
    @DisplayName("$ref resolution")
    class RefTests {

        private final JsonSchema schemaWithRef = schemaFromString("{ \"$defs\": {" +
                "     \"vehicle\": {" +
                "        \"type\":\"object\"," +
                "        \"properties\":{ \"type\":{\"type\":\"string\"}, \"wheels\":{\"type\":\"integer\"} }," +
                "        \"required\":[\"type\",\"wheels\"]" +
                "     }" +
                "  }," +
                "  \"$ref\":\"#/$defs/vehicle\"" +
                "}");

        @Test
        void walk_refResolvesSuccessfully() {
            ValidationResult result = schemaWithRef.walk(nodeFromString("{\"type\":\"car\",\"wheels\":4}"), true);
            Assertions.assertTrue(result.isSuccess(), "Instance should satisfy the referenced vehicle definition");
        }

        @Test
        void walk_refUnresolvedFails() {
            JsonSchema badRefSchema = schemaFromString("{ \"$ref\":\"#/nonexistent\" }");
            ValidationResult result = badRefSchema.walk(nodeFromString("{\"any\":\"value\"}"), true);
            Assertions.assertFalse(result.isSuccess(), "Unresolved $ref must produce a validation error");
            Assertions.assertTrue(
                    result.getMessages().stream().anyMatch(m -> m.getMessage().contains("$ref")),
                    "Expected a $ref‑resolution error message");
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
            Assertions.assertTrue(result.isSuccess(), "Boolean true schema must accept any instance");
        }

        @Test
        void walk_booleanFalseAlwaysInvalid() {
            ValidationResult result = schemaFalse.walk(nodeFromString("\"anything\""), true);
            Assertions.assertFalse(result.isSuccess(), "Boolean false schema must reject all instances");
            Assertions.assertTrue(
                    result.getMessages().stream().anyMatch(m -> m.getMessage().contains("false")),
                    "Expected a message indicating failure against a false schema");
        }
    }

    @Nested
    @DisplayName("additionalProperties handling")
    class AdditionalPropertiesTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\"," +
                "  \"properties\":{ \"id\":{\"type\":\"integer\"} }," +
                "  \"required\":[\"id\"]," +
                "  \"additionalProperties\":false" +
                "}");

        @Test
        void walk_additionalPropAllowedFails() {
            ValidationResult result = schema.walk(nodeFromString("{\"id\":1,\"extra\":\"not allowed\"}"), true);
            Assertions.assertFalse(result.isSuccess(), "Extra property should trigger additionalProperties validation error");
            Assertions.assertTrue(
                    result.getMessages().stream().anyMatch(m -> m.getMessage().contains("additionalProperties")),
                    "Expected an additionalProperties validation message");
        }
    }

    @Nested
    @DisplayName("anyOf validation")
    class AnyOfTests {

        private final JsonSchema schema = schemaFromString("{ \"anyOf\":[" +
                "   { \"type\":\"string\" }," +
                "   { \"type\":\"integer\" }" +
                "]}");

        @Test
        void walk_anyOfMatchesInteger() {
            ValidationResult result = schema.walk(nodeFromString("42"), true);
            Assertions.assertTrue(result.isSuccess(), "Integer instance should satisfy anyOf");
        }

        @Test
        void walk_anyOfNoMatch() {
            ValidationResult result = schema.walk(nodeFromString("true"), true);
            Assertions.assertFalse(result.isSuccess(), "Boolean instance should not match any anyOf subschema");
            Assertions.assertTrue(
                    result.getMessages().stream().anyMatch(m -> m.getMessage().contains("anyOf")),
                    "Expected an anyOf‑validation error message");
        }
    }

    @Nested
    @DisplayName("Recursive (self‑referencing) schema")
    class RecursiveSchemaTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\"," +
                "  \"properties\":{" +
                "    \"value\":{\"type\":\"integer\"}," +
                "    \"next\":{\"anyOf\":[{\"type\":\"null\"},{\"$ref\":\"#\"}]}" +
                "  },\"required\":[\"value\",\"next\"]}");

        @Test
        void walk_recursiveValid() {
            ValidationResult result = schema.walk(nodeFromString("{\"value\":1,\"next\":{\"value\":2,\"next\":null}}"), true);
            Assertions.assertTrue(result.isSuccess(), "Valid recursive structure should succeed");
        }

        @Test
        void walk_recursiveInvalid() {
            ValidationResult result = schema.walk(nodeFromString("{\"value\":\"not an int\",\"next\":{}}"), true);
            Assertions.assertFalse(result.isSuccess(), "Invalid recursive instance should fail");
            Assertions.assertTrue(
                    result.getMessages().stream().anyMatch(m -> m.getMessage().contains("type") && m.getPath().contains("value")),
                    "Expected a type‑mismatch message for root 'value'");
            Assertions.assertTrue(
                    result.getMessages().stream().anyMatch(m -> m.getMessage().contains("required") && m.getPath().contains("next.value")),
                    "Expected a required‑property message for 'next.value'");
        }
    }

    @Nested
    @DisplayName("Null instance node")
    class NullInstanceTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\" }");

        @Test
        void walk_nullNodeWhenValidateTrue() {
            ValidationResult result = schema.walk(null, true);
            Assertions.assertFalse(result.isSuccess(), "Null instance should be reported as invalid");
            Assertions.assertTrue(
                    result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("null")),
                    "Expected a message indicating the instance node is null");
        }

        @Test
        void walk_nullNodeWhenValidateFalse() {
            // When validation is disabled the implementation still returns a
            // ValidationResult – by contract it should be successful.
            ValidationResult result = schema.walk(null, false);
            Assertions.assertTrue(result.isSuccess(), "When validation is disabled, null instance is considered successful");
        }
    }

    @Nested
    @DisplayName("Unsupported schema node type")
    class UnsupportedSchemaNodeTests {

        @Test
        void walk_schemaNodeIsNumberThrows() {
            // Build a JsonSchema with a numeric literal as the schema node.
            JsonNode numericSchemaNode = MAPPER.getNodeFactory().numberNode(123);
            JsonSchema malformedSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(numericSchemaNode);
            IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                    () -> malformedSchema.walk(nodeFromString("\"any\""), true));
            Assertions.assertTrue(ex.getMessage().contains("Schema node must be an object or boolean"),
                    "Exception message should mention the unsupported schema node type");
        }
    }

    /**
     * Helper to create a JsonSchema from a JSON schema string.
     */
    private static JsonSchema schemaFromString(String schemaJson) {
        try {
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(schemaNode);
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
}
```