```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link JsonSchema#walkAtNode(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * <p>The tests cover successful validation, failure cases, flag handling, defensive
 * null‑checks, path tracking, $ref resolution, recursive anchors, fail‑fast
 * configuration and error propagation for malformed schemas.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaWalkAtNodeTest {

    /** Shared objects for all tests. */
    private static JsonSchemaFactory factory;
    private static ObjectMapper mapper;

    /** Instance specific objects. */
    private ExecutionContext executionContext;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the most recent draft supported by the library.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // a fresh execution context for each test – the default constructor is sufficient.
        executionContext = new ExecutionContext();
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods
     * --------------------------------------------------------------------- */

    /** Parses a JSON string into a {@link JsonNode}. */
    private static JsonNode node(String json) throws IOException {
        return mapper.readTree(json);
    }

    /** Creates a {@link JsonSchema} from a JSON schema string. */
    private static JsonSchema schema(String schemaJson) {
        return factory.getSchema(schemaJson);
    }

    /** Returns a root {@link JsonNodePath}. */
    private static JsonNodePath rootPath() {
        return new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
    }

    /* --------------------------------------------------------------------- *
     *  Existing simple object schema tests (kept from the original framework)
     * --------------------------------------------------------------------- */

    @Test
    void walkAtNode_invalidInstance_returnsValidationMessages() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "name": { "type": "string" } },
                  "required": [ "name" ],
                  "additionalProperties": false
                }
                """;
        JsonSchema schema = schema(schemaJson);
        JsonNode node = node("{\"name\":123}");
        ValidationResult result = schema.walkAtNode(executionContext, node, node, rootPath(), true);
        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty(),
                "Expected validation errors because the instance does not conform to the schema");
    }

    @Test
    void walkAtNode_validInstance_returnsNoMessages() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "name": { "type": "string" } },
                  "required": [ "name" ],
                  "additionalProperties": false
                }
                """;
        JsonSchema schema = schema(schemaJson);
        JsonNode node = node("{\"name\":\"John Doe\"}");
        ValidationResult result = schema.walkAtNode(executionContext, node, node, rootPath(), true);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "A valid instance should produce no validation messages");
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised tests covering the JSON test‑case matrix
     * --------------------------------------------------------------------- */

    /** Parameter source for simple object validation (valid / invalid). */
    private static Stream<Arguments> simpleObjectCases() {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "name": { "type": "string" } },
                  "required": [ "name" ],
                  "additionalProperties": false
                }
                """;
        JsonSchema schema = schema(schemaJson);
        return Stream.of(
                // successful validation when flag is true
                Arguments.of("Successful validation when flag is true",
                        schema,
                        new ExecutionContext(),
                        node("{\"name\":\"John\"}"),
                        node("{\"name\":\"John\"}"),
                        rootPath(),
                        true,
                        true,   // expect success
                        null),

                // failed validation when flag is true (missing required)
                Arguments.of("Failed validation when flag is true",
                        schema,
                        new ExecutionContext(),
                        node("{}"),
                        node("{}"),
                        rootPath(),
                        true,
                        false,
                        "required"),

                // validation skipped when flag is false
                Arguments.of("Validation skipped when flag is false",
                        schema,
                        new ExecutionContext(),
                        node("{}"),
                        node("{}"),
                        rootPath(),
                        false,
                        true,
                        null)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("simpleObjectCases")
    void simpleObjectValidation(String group,
                                JsonSchema schema,
                                ExecutionContext ctx,
                                JsonNode node,
                                JsonNode rootNode,
                                JsonNodePath location,
                                boolean validateFlag,
                                boolean expectSuccess,
                                String expectedMessageFragment) {
        ValidationResult result = schema.walkAtNode(ctx, node, rootNode, location, validateFlag);
        assertNotNull(result);
        if (expectSuccess) {
            assertTrue(result.getMessages().isEmpty(),
                    () -> "Group '" + group + "' expected no validation messages");
        } else {
            assertFalse(result.getMessages().isEmpty(),
                    () -> "Group '" + group + "' expected validation messages");
            if (expectedMessageFragment != null) {
                boolean anyMatch = result.getMessages().stream()
                        .anyMatch(m -> m.getMessage().contains(expectedMessageFragment));
                assertTrue(anyMatch,
                        () -> "Group '" + group + "' expected a message containing '" + expectedMessageFragment + "'");
            }
        }
    }

    /** Parameter source for array type validation. */
    private static Stream<Arguments> arrayCases() {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "array",
                  "items": { "type": "integer" }
                }
                """;
        JsonSchema schema = schema(schemaJson);
        return Stream.of(
                Arguments.of("Successful validation for array type",
                        schema,
                        new ExecutionContext(),
                        node("[1,2,3]"),
                        node("[1,2,3]"),
                        rootPath(),
                        true,
                        true,
                        null),

                Arguments.of("Failed validation for array type with wrong element type",
                        schema,
                        new ExecutionContext(),
                        node("[\"a\",\"b\",3]"),
                        node("[\"a\",\"b\",3]"),
                        rootPath(),
                        true,
                        false,
                        "type")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("arrayCases")
    void arrayValidation(String group,
                         JsonSchema schema,
                         ExecutionContext ctx,
                         JsonNode node,
                         JsonNode rootNode,
                         JsonNodePath location,
                         boolean validateFlag,
                         boolean expectSuccess,
                         String expectedFragment) {
        ValidationResult result = schema.walkAtNode(ctx, node, rootNode, location, validateFlag);
        assertNotNull(result);
        if (expectSuccess) {
            assertTrue(result.getMessages().isEmpty(),
                    () -> "Group '" + group + "' should be successful");
        } else {
            assertFalse(result.getMessages().isEmpty(),
                    () -> "Group '" + group + "' should contain errors");
            if (expectedFragment != null) {
                assertTrue(result.getMessages().stream()
                                .anyMatch(m -> m.getMessage().contains(expectedFragment)),
                        () -> "Group '" + group + "' expected a message containing '" + expectedFragment + "'");
            }
        }
    }

    /** Parameter source for enum constraint validation. */
    private static Stream<Arguments> enumCases() {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "string",
                  "enum": ["red","green","yellow"]
                }
                """;
        JsonSchema schema = schema(schemaJson);
        return Stream.of(
                Arguments.of("Enum constraint violation – validate true",
                        schema,
                        new ExecutionContext(),
                        node("\"blue\""),
                        node("\"blue\""),
                        rootPath(),
                        true,
                        false,
                        "enum"),

                Arguments.of("Enum constraint violation – validate false (skip)",
                        schema,
                        new ExecutionContext(),
                        node("\"blue\""),
                        node("\"blue\""),
                        rootPath(),
                        false,
                        true,
                        null)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("enumCases")
    void enumValidation(String group,
                        JsonSchema schema,
                        ExecutionContext ctx,
                        JsonNode node,
                        JsonNode rootNode,
                        JsonNodePath location,
                        boolean validateFlag,
                        boolean expectSuccess,
                        String expectedFragment) {
        ValidationResult result = schema.walkAtNode(ctx, node, rootNode, location, validateFlag);
        assertNotNull(result);
        if (expectSuccess) {
            assertTrue(result.getMessages().isEmpty(),
                    () -> "Group '" + group + "' expected success");
        } else {
            assertFalse(result.getMessages().isEmpty(),
                    () -> "Group '" + group + "' expected errors");
            if (expectedFragment != null) {
                assertTrue(result.getMessages().stream()
                                .anyMatch(m -> m.getMessage().contains(expectedFragment)),
                        () -> "Group '" + group + "' expected a message containing '" + expectedFragment + "'");
            }
        }
    }

    /** Parameter source for additionalProperties = false. */
    private static Stream<Arguments> additionalPropsCases() {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "allowed": { "type": "string" } },
                  "additionalProperties": false,
                  "required": [ "allowed" ]
                }
                """;
        JsonSchema schema = schema(schemaJson);
        return Stream.of(
                Arguments.of("AdditionalProperties false – extra field present",
                        schema,
                        new ExecutionContext(),
                        node("{\"allowed\":\"yes\",\"extra\":123}"),
                        node("{\"allowed\":\"yes\",\"extra\":123}"),
                        rootPath(),
                        true,
                        false,
                        "additionalProperties")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("additionalPropsCases")
    void additionalPropertiesValidation(String group,
                                         JsonSchema schema,
                                         ExecutionContext ctx,
                                         JsonNode node,
                                         JsonNode rootNode,
                                         JsonNodePath location,
                                         boolean validateFlag,
                                         boolean expectSuccess,
                                         String expectedFragment) {
        ValidationResult result = schema.walkAtNode(ctx, node, rootNode, location, validateFlag);
        assertNotNull(result);
        if (expectSuccess) {
            assertTrue(result.getMessages().isEmpty());
        } else {
            assertFalse(result.getMessages().isEmpty());
            assertTrue(result.getMessages().stream()
                            .anyMatch(m -> m.getMessage().contains(expectedFragment)));
        }
    }

    /** Test for non‑root instanceLocation tracking. */
    @Test
    void nonRootInstanceLocationTracking() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "address": {
                      "type": "object",
                      "properties": {
                        "city": { "type": "string" }
                      },
                      "required": ["city"]
                    }
                  },
                  "required": ["address"]
                }
                """;
        JsonSchema schema = schema(schemaJson);
        JsonNode full = node("{\"address\":{\"city\":123}}");
        JsonNode subNode = full.at("/address"); // {"city":123}
        JsonNodePath location = JsonNodePath.of("address"); // walk starts at address
        ValidationResult result = schema.walkAtNode(executionContext, subNode, full, location, true);
        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty(),
                "Expected validation error because city is not a string");
        // The error path must be "/address/city"
        boolean pathMatches = result.getMessages().stream()
                .anyMatch(m -> m.getPath().equals("/address/city"));
        assertTrue(pathMatches, "Error path should be '/address/city'");
    }

    /** Test for deep instanceLocation error path tracking. */
    @Test
    void deepInstanceLocationErrorPathTracking() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "integer"
                }
                """;
        JsonSchema schema = schema(schemaJson);
        JsonNode root = node("{\"level1\":{\"level2\":{\"value\":\"not-a-number\"}}}");
        JsonNode node = root.at("/level1/level2/value"); // the string node
        JsonNodePath location = JsonNodePath.of("level1", "level2", "value");
        ValidationResult result = schema.walkAtNode(executionContext, node, root, location, true);
        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty(),
                "Expected type error because the node is a string, not integer");
        boolean pathMatches = result.getMessages().stream()
                .anyMatch(m -> m.getPath().equals("/level1/level2/value"));
        assertTrue(pathMatches, "Error path should be '/level1/level2/value'");
    }

    /** Test $ref resolution with an external definition (in‑memory). */
    @Test
    void refResolutionWithExternalDefinition() throws Exception {
        // Main schema referencing #/definitions/person
        String mainSchema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "person": { "$ref": "#/definitions/personDef" }
                  },
                  "definitions": {
                    "personDef": {
                      "type": "object",
                      "properties": { "name": { "type": "string" } },
                      "required": ["name"]
                    }
                  }
                }
                """;
        JsonSchema schema = schema(mainSchema);
        JsonNode node = node("{\"person\":{}}");
        ValidationResult result = schema.walkAtNode(executionContext, node, node, rootPath(), true);
        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty(),
                "Missing required property 'name' inside $ref schema should be reported");
        boolean hasNameMsg = result.getMessages().stream()
                .anyMatch(m -> m.getMessage().contains("required") && m.getPath().contains("person/name"));
        assertTrue(hasNameMsg, "Message should refer to missing 'person/name'");
    }

    /** Test recursive anchor handling – valid recursive structure. */
    @Test
    void recursiveAnchorValidStructure() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$recursiveAnchor": true,
                  "type": "object",
                  "properties": {
                    "node": {
                      "$recursiveRef": "#"
                    }
                  },
                  "required": []
                }
                """;
        JsonSchema schema = schema(schemaJson);
        JsonNode instance = node("{\"node\":{\"node\":{\"value\":1}}}");
        ValidationResult result = schema.walkAtNode(executionContext, instance, instance, rootPath(), true);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "Recursive structure that conforms to the schema should succeed");
    }

    /** Test fail‑fast disabled – multiple errors aggregated. */
    @Test
    void failFastDisabledAggregatesErrors() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "age": { "type": "integer", "minimum": 0 },
                    "email": { "type": "string", "format": "email" }
                  },
                  "required": ["age","email"]
                }
                """;
        // configure the factory to disable fail‑fast
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setFailFast(false);
        JsonSchemaFactory customFactory = JsonSchemaFactory.getInstance(VersionFlag.V202012, cfg -> cfg.set(config));
        JsonSchema schema = customFactory.getSchema(schemaJson);

        JsonNode instance = node("{\"age\":-5,\"email\":\"not-an-email\"}");
        ValidationResult result = schema.walkAtNode(executionContext, instance, instance, rootPath(), true);
        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty(),
                "Both violations should be reported when failFast is disabled");
        assertEquals(2, result.getMessages().size(),
                "Expected exactly two validation messages");
    }

    /** Test that a null ExecutionContext results in internal creation and succeeds. */
    @Test
    void nullExecutionContextCreatesDefaultAndValidates() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "name": { "type": "string" } },
                  "required": ["name"]
                }
                """;
        JsonSchema schema = schema(schemaJson);
        JsonNode node = node("{\"name\":\"Alice\"}");
        ValidationResult result = schema.walkAtNode(null, node, node, rootPath(), true);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "Validation should succeed even when ExecutionContext is null");
    }

    /** Test that passing a null node leads to NullPointerException. */
    @Test
    void nullNodeThrowsNullPointerException() {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object"
                }
                """;
        JsonSchema schema = schema(schemaJson);
        assertThrows(NullPointerException.class, () ->
                schema.walkAtNode(executionContext, null, null, rootPath(), true));
    }

    /** Test that a null JsonNodePath argument triggers NPE. */
    @Test
    void nullInstanceLocationThrowsNullPointerException() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": { "name": { "type": "string" } }
                }
                """;
        JsonSchema schema = schema(schemaJson);
        JsonNode node = node("{\"name\":\"John\"}");
        assertThrows(NullPointerException.class, () ->
                schema.walkAtNode(executionContext, node, node, null, true));
    }

    /** Test that an illegal JsonNodePath segment causes IllegalArgumentException. */
    @Test
    void illegalJsonNodePathSegmentThrowsIllegalArgumentException() throws Exception {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object"
                }
                """;
        JsonSchema schema = schema(schemaJson);
        JsonNode node = node("{\"name\":\"John\"}");
        // JsonNodePath.of(null) is illegal – we construct it via reflection to avoid compile‑time error
        JsonNodePath illegalPath = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
        // add a null segment using the protected API (simulated by reflection)
        java.lang.reflect.Field segs = JsonNodePath.class.getDeclaredField("segments");
        segs.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) segs.get(illegalPath);
        list.add(null);
        assertThrows(IllegalArgumentException.class, () ->
                schema.walkAtNode(executionContext, node, node, illegalPath, true));
    }

    /** Test that a malformed $ref URI propagates UnsupportedEncodingException. */
    @Test
    void malformedRefUriPropagatesUnsupportedEncodingException() {
        String badRefSchema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$ref": "#/components/schemas/%ZZ"
                }
                """;
        // The exception is thrown during schema parsing/validation, not during walkAtNode.
        assertThrows(UnsupportedEncodingException.class, () -> {
            JsonSchema schema = factory.getSchema(badRefSchema);
            JsonNode node = node("{}");
            schema.walkAtNode(executionContext, node, node, rootPath(), true);
        });
    }

    /** Test that an unresolvable $ref leads to IllegalArgumentException. */
    @Test
    void unresolvableRefThrowsIllegalArgumentException() {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$ref": "#/components/schemas/Unknown"
                }
                """;
        assertThrows(IllegalArgumentException.class, () -> {
            JsonSchema schema = factory.getSchema(schemaJson);
            JsonNode node = node("{}");
            schema.walkAtNode(executionContext, node, node, rootPath(), true);
        });
    }

    /** Test external schema loading failure – IOException. */
    @Test
    void externalSchemaLoadingFailureThrowsIOException() {
        // Reference a non‑existent file via file URI
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "$ref": "file:///nonexistent/schema.json"
                }
                """;
        assertThrows(IOException.class, () -> {
            JsonSchema schema = factory.getSchema(schemaJson);
            JsonNode node = node("{}");
            schema.walkAtNode(executionContext, node, node, rootPath(), true);
        });
    }

    /** Test sub‑node walk with full document as rootNode where $ref resolves relative to root. */
    @Test
    void subNodeWithRootNodeForRefResolution() throws Exception {
        String mainSchema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "wrapper": {
                      "$ref": "#/definitions/inner"
                    }
                  },
                  "definitions": {
                    "inner": {
                      "type": "object",
                      "properties": { "id": { "type": "integer" } },
                      "required": ["id"]
                    }
                  }
                }
                """;
        JsonSchema schema = schema(mainSchema);
        JsonNode fullDoc = node("{\"wrapper\":{\"id\":10}}");
        JsonNode subNode = fullDoc.at("/wrapper"); // {"id":10}
        JsonNodePath location = JsonNodePath.of("wrapper");
        ValidationResult result = schema.walkAtNode(executionContext, subNode, fullDoc, location, true);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(),
                "The $ref should resolve against the root document and validation should succeed");
    }
}
```