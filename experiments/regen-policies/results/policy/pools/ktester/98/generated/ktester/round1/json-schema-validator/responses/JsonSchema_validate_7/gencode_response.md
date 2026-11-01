```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link JsonSchema#validate(ExecutionContext, JsonNode, OutputFormat)}.
 *
 * <p>The test suite covers:
 * <ul>
 *   <li>Argument validation (null checks)</li>
 *   <li>Successful validation (no errors)</li>
 *   <li>Failed validation (error messages)</li>
 *   <li>Custom {@link OutputFormat} behaviour</li>
 *   <li>Recursive anchor handling</li>
 *   <li>$ref resolution (internal and external)</li>
 *   <li>Schema parsing errors</li>
 *   <li>Unsupported $id encoding</li>
 *   <li>Invalid {@link ExecutionContext} state</li>
 *   <li>Circular $ref detection</li>
 * </ul>
 *
 * <p>All tests use the public factory methods – no private members are accessed
 * directly (reflection is only used to create an {@code ExecutionContext} with a
 * {@code null} {@code ValidationContext} for the “ExecutionContext state validation” case).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_7_Test {

    /** Shared objects for all tests */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** A simple schema that requires a string property {@code name}. Used by a few tests. */
    private static JsonSchema simpleNameSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a reusable simple schema for the “valid/invalid node” tests.
        String schemaData = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        simpleNameSchema = factory.getSchema(schemaData);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally
    }

    /* --------------------------------------------------------------
     *  Helper OutputFormat implementations
     * -------------------------------------------------------------- */

    /** Collects {@link ValidationMessage}s into a {@link List}. */
    private static final OutputFormat<List<ValidationMessage>> COLLECTING_FORMAT =
            messages -> new ArrayList<>(messages);

    /** Returns {@code true} when there are no validation messages, {@code false} otherwise. */
    private static final OutputFormat<Boolean> BOOLEAN_FORMAT =
            messages -> messages.isEmpty();

    /* --------------------------------------------------------------
     *  1. Null checks and argument validation
     * -------------------------------------------------------------- */

    private static Stream<Arguments> nullCheckProvider() {
        JsonNode validNode = MAPPER.createObjectNode().put("name", "John");
        return Stream.of(
                // executionContext == null
                Arguments.of(null, validNode, COLLECTING_FORMAT,
                        "executionContext must not be null"),
                // node == null
                Arguments.of(new ExecutionContext(), null, COLLECTING_FORMAT,
                        "node must not be null"),
                // format == null
                Arguments.of(new ExecutionContext(), validNode, null,
                        "format must not be null")
        );
    }

    @ParameterizedTest
    @MethodSource("nullCheckProvider")
    @DisplayName("Argument validation – null checks")
    void testValidateArgumentNullChecks(ExecutionContext ctx, JsonNode node,
                                        OutputFormat<?> format, String expectedMessage) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> simpleNameSchema.validate(ctx, node, format));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains(expectedMessage.toLowerCase(Locale.ROOT)),
                () -> "Exception message should contain '" + expectedMessage + "'");
    }

    /* --------------------------------------------------------------
     *  2. Successful validation (no errors)
     * -------------------------------------------------------------- */

    @Test
    @DisplayName("Valid node produces empty collection")
    void testValidateValidNode() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":\"John\"}");

        List<ValidationMessage> result = simpleNameSchema.validate(ctx, node, COLLECTING_FORMAT);
        assertTrue(result.isEmpty(), "Expected no validation messages for a valid node");
    }

    /* --------------------------------------------------------------
     *  3. Failed validation (error messages produced)
     * -------------------------------------------------------------- */

    @Test
    @DisplayName("Invalid node (type mismatch) yields a validation message")
    void testValidateInvalidType() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = MAPPER.readTree("{\"name\":1}");

        List<ValidationMessage> result = simpleNameSchema.validate(ctx, node, COLLECTING_FORMAT);
        assertFalse(result.isEmpty(), "Expected validation messages for an invalid node");
        assertEquals(1, result.size(), "Exactly one validation error should be reported");
        assertTrue(result.get(0).getMessage().toLowerCase(Locale.ROOT).contains("type"),
                "Message should indicate a type mismatch");
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
        JsonNode node = MAPPER.readTree("{}"); // missing {@code id}

        List<ValidationMessage> result = schema.validate(ctx, node, COLLECTING_FORMAT);
        assertFalse(result.isEmpty(), "Expected a validation message for missing required property");
        assertTrue(result.stream()
                .anyMatch(m -> m.getMessage().toLowerCase(Locale.ROOT).contains("required")),
                "Message should mention 'required'");
    }

    /* --------------------------------------------------------------
     *  4. Custom OutputFormat behavior
     * -------------------------------------------------------------- */

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

    /* --------------------------------------------------------------
     *  5. Recursive anchor handling
     * -------------------------------------------------------------- */

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
        assertTrue(result.stream()
                .anyMatch(m -> m.getMessage().toLowerCase(Locale.ROOT).contains("type")),
                "Message should indicate a type mismatch in the nested node");
    }

    /* --------------------------------------------------------------
     *  6. $ref resolution handling (internal $ref)
     * -------------------------------------------------------------- */

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

    /* --------------------------------------------------------------
     *  7. Schema parsing errors
     * -------------------------------------------------------------- */

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
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(schemaStr));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("unknown"),
                "Exception message should mention 'unknown keyword'");
    }

    @Test
    @DisplayName("Malformed $ref URI causes IllegalArgumentException")
    void testMalformedRefUri() {
        String schemaStr = """
                {
                  "$ref": "ht!tp://[invalid]"
                }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(schemaStr));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("uri"),
                "Exception message should mention 'URI' or 'invalid $ref'");
    }

    /* --------------------------------------------------------------
     *  8. Unsupported encoding in $id
     * -------------------------------------------------------------- */

    @Test
    @DisplayName("Invalid percent‑encoding in $id throws UnsupportedEncodingException")
    void testUnsupportedEncodingInId() {
        String schemaStr = """
                {
                  "$id": "http://example.com/schema%ZZ"
                }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertThrows(UnsupportedEncodingException.class,
                () -> factory.getSchema(schemaStr));
    }

    /* --------------------------------------------------------------
     *  9. ExecutionContext state validation
     * -------------------------------------------------------------- */

    @Test
    @DisplayName("ExecutionContext without ValidationContext causes IllegalStateException")
    void testExecutionContextWithoutValidationContext() throws Exception {
        // Use reflection to invoke the (package‑private) constructor that accepts a ValidationContext.
        Constructor<ExecutionContext> ctor = ExecutionContext.class.getDeclaredConstructor(ValidationContext.class);
        ctor.setAccessible(true);
        ExecutionContext ctx = ctor.newInstance((Object) null); // pass null ValidationContext

        // Simple schema that expects a string.
        String schemaStr = """
                { "type": "string" }""";
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V7).getSchema(schemaStr);
        JsonNode node = MAPPER.readTree("\"hello\"");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> schema.validate(ctx, node, COLLECTING_FORMAT));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("validationcontext"),
                "Exception message should mention missing ValidationContext");
    }

    /* --------------------------------------------------------------
     * 10. Circular $ref detection without $recursiveAnchor
     * -------------------------------------------------------------- */

    @Test
    @DisplayName("Circular $ref without $recursiveAnchor throws JsonSchemaException")
    void testCircularRefWithoutRecursiveAnchor() {
        String schemaStr = """
                {
                  "$id": "#",
                  "$ref": "#"
                }""";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(schemaStr));
        assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("circular"),
                "Exception message should contain 'circular reference'");
    }

    /* --------------------------------------------------------------
     * 11. (Optional) External $ref handling – disabled due to network dependency
     * -------------------------------------------------------------- */

    @Test
    @Disabled("External $ref resolution requires network/file system access which is out of scope for unit tests.")
    void testExternalRefResolution() {
        // Placeholder for an external $ref test. In a real integration test,
        // a mock URI resolver would be supplied to the JsonSchemaFactory.
    }
}
```