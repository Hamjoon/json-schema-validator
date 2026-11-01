package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_7_Test {

    /**
     * Shared objects for all tests
     */
    private static JsonSchema schema;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A simple schema that requires a string property {@code name}. Used by a few tests.
     */
    private static JsonSchema simpleNameSchema;

    /**
     * Collects {@link ValidationMessage}s into a {@link List}.
     */
    private static final OutputFormat<List<ValidationMessage>> COLLECTING_FORMAT = messages -> new ArrayList<>(messages);

    /**
     * Returns {@code true} when there are no validation messages, {@code false} otherwise.
     */
    private static final OutputFormat<Boolean> BOOLEAN_FORMAT = messages -> messages.isEmpty();

    @BeforeAll
    static void setupBeforeAll() {
        // A tiny schema that requires a string property "name"
        String schemaData = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }""";
        // Use the factory to obtain a JsonSchema instance – this is the public way
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        schema = factory.getSchema(schemaData);
    }

    private static Stream<Arguments> nullCheckProvider() {
        JsonNode validNode = MAPPER.createObjectNode().put("name", "John");
        return Stream.of(// executionContext == null
        Arguments.of(null, validNode, COLLECTING_FORMAT, "executionContext must not be null"), // node == null
        Arguments.of(new ExecutionContext(), null, COLLECTING_FORMAT, "node must not be null"), // format == null
        Arguments.of(new ExecutionContext(), validNode, null, "format must not be null"));
    }

    /**
     * Verify that a valid JSON node produces an empty list of validation messages
     * when the {@code validate(ExecutionContext, JsonNode, OutputFormat)} method
     * is used.
     */
    @Test
    void testValidateValidNode() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":\"John\"}");
        // OutputFormat that simply collects the messages into a List
        OutputFormat<List<ValidationMessage>> format = messages -> new ArrayList<>(messages);
        List<ValidationMessage> result = schema.validate(ctx, node, format);
        assertTrue(result.isEmpty(), "Expected no validation messages for a valid node");
    }

    /**
     * Verify that an invalid JSON node (wrong type for {@code name}) yields a
     * non‑empty list of validation messages.
     */
    @Test
    void testValidateInvalidNode() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":1}");
        OutputFormat<List<ValidationMessage>> format = messages -> new ArrayList<>(messages);
        List<ValidationMessage> result = schema.validate(ctx, node, format);
        assertFalse(result.isEmpty(), "Expected validation messages for an invalid node");
        assertEquals(1, result.size(), "Exactly one validation error should be reported");
    }

    @ParameterizedTest
    @MethodSource("nullCheckProvider")
    @DisplayName("Argument validation – null checks")
    void testValidateArgumentNullChecks(ExecutionContext ctx, JsonNode node, OutputFormat<?> format, String expectedMessage) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> simpleNameSchema.validate(ctx, node, format));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains(expectedMessage.toLowerCase(Locale.ROOT)), () -> "Exception message should contain '" + expectedMessage + "'");
    }

    @Test
    @DisplayName("Invalid node (type mismatch) yields a validation message")
    void testValidateInvalidType() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":1}");
        List<ValidationMessage> result = simpleNameSchema.validate(ctx, node, COLLECTING_FORMAT);
        assertFalse(result.isEmpty(), "Expected validation messages for an invalid node");
        assertEquals(1, result.size(), "Exactly one validation error should be reported");
        assertTrue(result.get(0).getMessage().toLowerCase(Locale.ROOT).contains("type"), "Message should indicate a type mismatch");
    }

    @Test
    @DisplayName("Missing required property produces a validation message")
    void testValidateMissingRequired() throws Exception {
        // Schema requiring a string property {@code id}
        String schemaStr = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": { "id": { "type": "string" } },
              "required": ["id"]
            }""";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaStr);
        ExecutionContext ctx = new ExecutionContext();
        // missing {@code id}
        JsonNode node = MAPPER.readTree("{}");
        List<ValidationMessage> result = schema.validate(ctx, node, COLLECTING_FORMAT);
        assertFalse(result.isEmpty(), "Expected a validation message for missing required property");
        assertTrue(result.stream().anyMatch(m -> m.getMessage().toLowerCase(Locale.ROOT).contains("required")), "Message should mention 'required'");
    }

    @Test
    @DisplayName("BooleanOutputFormat returns true for a valid instance")
    void testBooleanOutputFormatSuccess() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":\"Alice\"}");
        Boolean result = simpleNameSchema.validate(ctx, node, BOOLEAN_FORMAT);
        assertTrue(result, "BooleanOutputFormat should return true when validation succeeds");
    }

    @Test
    @DisplayName("BooleanOutputFormat returns false for an invalid instance")
    void testBooleanOutputFormatFailure() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":123}");
        Boolean result = simpleNameSchema.validate(ctx, node, BOOLEAN_FORMAT);
        assertFalse(result, "BooleanOutputFormat should return false when validation fails");
    }

    @Test
    @DisplayName("Recursive anchor – valid self‑referencing structure")
    void testRecursiveAnchorValid() throws Exception {
        String schemaStr = """
            {
              "$id": "http://example.com/linked-list",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "value": { "type": "integer" },
                "next": { "$ref": "#" }
              },
              "required": ["value"]
            }""";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaStr);
        ExecutionContext ctx = new ExecutionContext();
        // {"value":1,"next":{"value":2}}
        JsonNode node = MAPPER.readTree("{\"value\":1,\"next\":{\"value\":2}}");
        List<ValidationMessage> result = schema.validate(ctx, node, COLLECTING_FORMAT);
        assertTrue(result.isEmpty(), "Recursive schema should validate without infinite recursion");
    }

    @Test
    @DisplayName("Recursive anchor – nested violation is reported")
    void testRecursiveAnchorInvalidNested() throws Exception {
        String schemaStr = """
            {
              "$id": "http://example.com/linked-list",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "value": { "type": "integer" },
                "next": { "$ref": "#" }
              },
              "required": ["value"]
            }""";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaStr);
        ExecutionContext ctx = new ExecutionContext();
        // {"value":1,"next":{"value":"bad"}} – inner value is a string
        JsonNode node = MAPPER.readTree("{\"value\":1,\"next\":{\"value\":\"bad\"}}");
        List<ValidationMessage> result = schema.validate(ctx, node, COLLECTING_FORMAT);
        assertFalse(result.isEmpty(), "Expected a validation message for the nested type error");
        assertTrue(result.stream().anyMatch(m -> m.getMessage().toLowerCase(Locale.ROOT).contains("type")), "Message should indicate a type mismatch in the nested node");
    }

    @Test
    @DisplayName("Internal $ref resolves correctly")
    void testInternalRefResolution() throws Exception {
        String schemaStr = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "$ref": "#/definitions/person",
              "definitions": {
                "person": {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }
              }
            }""";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaStr);
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":\"Bob\"}");
        List<ValidationMessage> result = schema.validate(ctx, node, COLLECTING_FORMAT);
        assertTrue(result.isEmpty(), "Internal $ref should be resolved and validation succeed");
    }

    @Test
    @DisplayName("Unknown keyword triggers JsonSchemaException")
    void testUnknownKeywordParsingError() {
        String schemaStr = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "unknownKeyword": true
            }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaStr));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("unknown"), "Exception message should mention 'unknown keyword'");
    }

    @Test
    @DisplayName("Malformed $ref URI causes IllegalArgumentException")
    void testMalformedRefUri() {
        String schemaStr = """
            {
              "$ref": "ht!tp://[invalid]"
            }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaStr));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("uri"), "Exception message should mention 'URI' or 'invalid $ref'");
    }

    @Test
    @DisplayName("Invalid percent‑encoding in $id throws UnsupportedEncodingException")
    void testUnsupportedEncodingInId() {
        String schemaStr = """
            {
              "$id": "http://example.com/schema%ZZ"
            }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertThrows(UnsupportedEncodingException.class, () -> factory.getSchema(schemaStr));
    }

    @Test
    @DisplayName("ExecutionContext without ValidationContext causes IllegalStateException")
    void testExecutionContextWithoutValidationContext() throws Exception {
        // Use reflection to invoke the (package‑private) constructor that accepts a ValidationContext.
        Constructor<ExecutionContext> ctor = ExecutionContext.class.getDeclaredConstructor(ValidationContext.class);
        ctor.setAccessible(true);
        // pass null ValidationContext
        ExecutionContext ctx = ctor.newInstance((Object) null);
        // Simple schema that expects a string.
        String schemaStr = """
            { "type": "string" }""";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaStr);
        JsonNode node = MAPPER.readTree("\"hello\"");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> schema.validate(ctx, node, COLLECTING_FORMAT));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("validationcontext"), "Exception message should mention missing ValidationContext");
    }

    @Test
    @DisplayName("Circular $ref without $recursiveAnchor throws JsonSchemaException")
    void testCircularRefWithoutRecursiveAnchor() {
        String schemaStr = """
            {
              "$id": "#",
              "$ref": "#"
            }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaStr));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("circular"), "Exception message should contain 'circular reference'");
    }

    @Test
    @Disabled("External $ref resolution requires network/file system access which is out of scope for unit tests.")
    void testExternalRefResolution() {
        // Placeholder for an external $ref test. In a real integration test,
        // a mock URI resolver would be supplied to the JsonSchemaFactory.
    }
}
