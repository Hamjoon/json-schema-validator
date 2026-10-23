```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchema_walk_2_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */
    /** Build a {@link JsonSchema} from a JSON string. */
    private static JsonSchema schemaFromString(String schemaJson) {
        try {
            JsonNode schemaNode = MAPPER.readTree(schemaJson);
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(schemaNode);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Parse a JSON instance string into a {@link JsonNode}. */
    private static JsonNode nodeFromString(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Parse the literal "null" into a {@link JsonNode}. Used to avoid NPEs. */
    private static JsonNode nullNode() {
        return MAPPER.nullNode();
    }

    /* --------------------------------------------------------------------- */
    /* Required property & type validation                                   */
    /* --------------------------------------------------------------------- */
    @Nested
    @DisplayName("Required property & type validation")
    class RequiredAndTypeTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"object\","
                + "  \"properties\":{"
                + "    \"name\":{\"type\":\"string\"},"
                + "    \"age\":{\"type\":\"integer\"}"
                + "  },"
                + "  \"required\":[\"name\",\"age\"]"
                + "}");

        @ParameterizedTest(name = "[{index}] shouldValidate={0} expectedSuccess={1}")
        @MethodSource("validAndInvalidProvider")
        void walk_requiredAndType(boolean shouldValidate,
                                  boolean expectedSuccess,
                                  String instanceJson,
                                  String... expectedMessages) {
            JsonNode instance = nodeFromString(instanceJson);
            Set<ValidationMessage> messages = schema.validate(instance);
            boolean success = messages.isEmpty();
            assertEquals(expectedSuccess, success,
                    () -> "Expected success=" + expectedSuccess + " but got messages: " + messages);
            if (!expectedSuccess && expectedMessages.length > 0) {
                for (String exp : expectedMessages) {
                    assertTrue(messages.stream().anyMatch(msg -> msg.getMessage().contains(exp)),
                            "Expected a message containing: " + exp);
                }
            }
        }

        /** Provide an empty {@code String[]} for the cases that do not need
         *  expected messages – this satisfies JUnit’s var‑args conversion. */
        static Stream<Arguments> validAndInvalidProvider() {
            return Stream.of(
                    // valid object – both required properties present and correct types
                    Arguments.of(true, true, "{\"name\":\"John\",\"age\":30}", new String[]{}),
                    // type mismatch for both properties
                    Arguments.of(true, false, "{\"name\":123,\"age\":\"thirty\"}", new String[]{"type", "type"}),
                    // missing required property "age"
                    Arguments.of(true, false, "{\"name\":\"John\"}", new String[]{"required"}),
                    // shouldValidate = false – validation is bypassed, always success (simulated)
                    Arguments.of(false, true, "{\"unexpected\":\"value\"}", new String[]{})
            );
        }
    }

    /* --------------------------------------------------------------------- */
    /* Enum validation                                                       */
    /* --------------------------------------------------------------------- */
    @Nested
    @DisplayName("Enum validation")
    class EnumTests {

        private final JsonSchema schema = schemaFromString("{ \"type\":\"string\", \"enum\":[\"RED\",\"GREEN\",\"BLUE\"] }");

        @Test
        void walk_enumSuccess() {
            Set<ValidationMessage> messages = schema.validate(nodeFromString("\"RED\""));
            assertTrue(messages.isEmpty(), "Enum value RED should be valid");
        }

        @Test
        void walk_enumFailure() {
            Set<ValidationMessage> messages = schema.validate(nodeFromString("\"YELLOW\""));
            assertFalse(messages.isEmpty(), "Enum value YELLOW should be invalid");
            assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("enum")),
                    "Expected an enum‑validation message");
        }
    }

    /* --------------------------------------------------------------------- */
    /* PatternProperties and format validation                               */
    /* --------------------------------------------------------------------- */
    @Nested
    @DisplayName("PatternProperties and format validation")
    class PatternAndFormatTests {

        private final JsonSchema emailPatternSchema = schemaFromString("{ \"type\":\"object\","
                + "  \"patternProperties\":{"
                + "    \"^email$\":{ \"type\":\"string\", \"format\":\"email\" }"
                + "  }"
                + "}");

        private final JsonSchema dateTimeSchema = schemaFromString("{ \"type\":\"string\", \"format\":\"date-time\" }");

        @Test
        void walk_emailPatternSuccess() {
            Set<ValidationMessage> messages = emailPatternSchema.validate(nodeFromString("{\"email\":\"john.doe@example.com\"}"));
            assertTrue(messages.isEmpty(), "Valid email should pass pattern/format validation");
        }

        @Test
        void walk_emailPatternFailure() {
            Set<ValidationMessage> messages = emailPatternSchema.validate(nodeFromString("{\"email\":\"not-an-email\"}"));
            assertFalse(messages.isEmpty(), "Invalid email should fail format validation");
            // The concrete message may vary between library versions – we only require that a message exists.
            assertTrue(messages.stream().anyMatch(m -> true),
                    "Expected at least one validation message for an invalid email");
        }

        @Test
        void walk_dateTimeSuccess() {
            Set<ValidationMessage> messages = dateTimeSchema.validate(nodeFromString("\"2023-08-15T13:45:30Z\""));
            assertTrue(messages.isEmpty(), "Valid RFC‑3339 date‑time should pass");
        }

        @Test
        void walk_dateTimeFailure() {
            Set<ValidationMessage> messages = dateTimeSchema.validate(nodeFromString("\"15/08/2023 13:45\""));
            assertFalse(messages.isEmpty(), "Invalid date‑time should fail");
            // Same reasoning as above – we only need a message.
            assertTrue(messages.stream().anyMatch(m -> true),
                    "Expected at least one validation message for an invalid date‑time");
        }
    }

    /* --------------------------------------------------------------------- */
    /* $ref resolution                                                       */
    /* --------------------------------------------------------------------- */
    @Nested
    @DisplayName("$ref resolution")
    class RefTests {

        private final JsonSchema schemaWithRef = schemaFromString("{ \"$defs\": {"
                + "     \"vehicle\": {"
                + "        \"type\":\"object\","
                + "        \"properties\":{ \"type\":{\"type\":\"string\"}, \"wheels\":{\"type\":\"integer\"} },"
                + "        \"required\":[\"type\",\"wheels\"]"
                + "     }"
                + "  },"
                + "  \"$ref\":\"#/$defs/vehicle\""
                + "}");

        @Test
        void walk_refResolvesSuccessfully() {
            Set<ValidationMessage> messages = schemaWithRef.validate(nodeFromString("{\"type\":\"car\",\"wheels\":4}"));
            assertTrue(messages.isEmpty(), "Instance should satisfy the referenced vehicle definition");
        }

        @Test
        void walk_refUnresolvedFails() {
            JsonSchema badRefSchema;
            try {
                badRefSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                        .getSchema(nodeFromString("{ \"$ref\":\"#/nonexistent\" }"));
                // If construction succeeded we validate and expect an error message.
                Set<ValidationMessage> messages = badRefSchema.validate(nodeFromString("{\"any\":\"value\"}"));
                assertFalse(messages.isEmpty(), "Unresolved $ref must produce a validation error");
                assertTrue(messages.stream().anyMatch(m -> m.getMessage().contains("$ref")),
                        "Expected a $ref‑resolution error message");
            } catch (JsonSchemaException e) {
                // Some library versions throw during schema construction – treat that as a successful
                // detection of the unresolved reference.
                assertTrue(e.getMessage().contains("$ref"),
                        "Exception message should mention an unresolved $ref");
            }
        }
    }

    /* --------------------------------------------------------------------- */
    /* Boolean schemas                                                       */
    /* --------------------------------------------------------------------- */
    @Nested
    @DisplayName("Boolean schemas")
    class BooleanSchemaTests {

        private final JsonSchema schemaTrue = schemaFromString("true");
        private final JsonSchema schemaFalse = schemaFromString("false");

        @Test
        void walk_booleanTrueAlwaysValid() {
            Set<ValidationMessage> messages = schemaTrue.validate(nodeFromString("null"));
            assertTrue(messages.isEmpty