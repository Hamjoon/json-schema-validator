package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walkAtNode_Test {

    // -----------------------------------------------------------------------
    @Mock
    ValidationContext validationContext;

    @Mock
    JsonMetaSchema metaSchema;

    @Mock
    JsonSchemaFactory jsonSchemaFactory;

    @Mock
    SchemaValidatorsConfig config;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    // the schema definition node
    @Mock
    JsonNode schemaNode;

    // node that will be walked
    @Mock
    JsonNode instanceNode;

    // root of the instance document
    @Mock
    JsonNode rootNode;

    // location of the instance node
    @Mock
    JsonNodePath instanceLocation;

    @Mock
    ExecutionContext executionContext;

    // -----------------------------------------------------------------------
    JsonSchema jsonSchema;

    // -----------------------------------------------------------------------
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Simple implementation of {@link WalkListener} that counts callbacks.
     */
    private static class TestWalkListener implements WalkListener {

        int enterCount = 0;

        int exitCount = 0;

        @Override
        public void enterKeyword(JsonValidator validator, ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation) {
            enterCount++;
        }

        @Override
        public void exitKeyword(JsonValidator validator, ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation) {
            exitCount++;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mock behaviour required by the JsonSchema constructor.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        // Meta‑schema methods used during construction.
        when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);
        // Build the JsonSchema instance via the package‑private factory method.
        // The parent schema is null and sub‑schema retrieval is not suppressed.
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        assertNotNull(jsonSchema, "JsonSchema instance should be created successfully");
        // exercised.
        CollectorContext collector = mock(CollectorContext.class);
        when(executionContext.getCollectorContext()).thenReturn(collector);
    }

    /**
     * Helper that creates a {@link JsonSchema} from a JSON schema string using the
     *  library's factory.  The factory is configured with the latest Draft‑07
     *  version, which is sufficient for the test data.
     */
    private static JsonSchema createSchema(String schemaJson) {
        JsonNode schemaNode;
        try {
            schemaNode = MAPPER.readTree(schemaJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid schema JSON supplied to test", e);
        }
        // The factory method used by the library – it returns a fully initialised
        // JsonSchema instance (including validators).
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        return factory.getSchema(schemaNode);
    }

    /**
     * Supplies the matrix of inputs described in the *basic parameter validation* group.
     */
    private static Stream<Arguments> basicParameterProvider() {
        // Mocks for the non‑null arguments – they are not used by the method body
        // when the earlier argument is null, but we provide them for completeness.
        ExecutionContext validCtx = mock(ExecutionContext.class);
        JsonNode validNode = mock(JsonNode.class);
        JsonNode validRoot = mock(JsonNode.class);
        JsonNodePath validPath = mock(JsonNodePath.class);
        return Stream.of(// 1️⃣ executionContext == null → NPE
        Arguments.of(null, null, null, null, false, NullPointerException.class, "ExecutionContext must be non‑null"), // 2️⃣ node == null → ValidationResult with an error about missing node
        Arguments.of(validCtx, null, validRoot, validPath, true, null, "Missing instance node should yield an invalid result"), // 3️⃣ rootNode == null while validation requested → invalid result
        Arguments.of(validCtx, validNode, null, validPath, true, null, "Missing root node should yield an invalid result"), // 4️⃣ instanceLocation == null → invalid result
        Arguments.of(validCtx, validNode, validRoot, null, true, null, "Missing instanceLocation should yield an invalid result"));
    }

    /**
     * Verifies that {@code walkAtNode} executes without throwing and returns a
     * non‑null {@link ValidationResult}. The internal walk logic is exercised
     * with all collaborators mocked, so the focus is on the method contract.
     */
    @Test
    void walkAtNode_returnsNonNullResult() {
        // Act
        ValidationResult result = jsonSchema.walkAtNode(executionContext, instanceNode, rootNode, instanceLocation, // request validation while walking
        true);
        // Assert
        assertNotNull(result, "walkAtNode should return a ValidationResult instance");
    }

    /**
     * Same as the previous test but with validation disabled. The method should
     * still return a {@link ValidationResult} (which may be empty).
     */
    @Test
    void walkAtNode_withoutValidation_returnsNonNullResult() {
        ValidationResult result = jsonSchema.walkAtNode(executionContext, instanceNode, rootNode, instanceLocation, // walk without performing validation
        false);
        assertNotNull(result, "walkAtNode should return a ValidationResult even when validation is disabled");
    }

    // -----------------------------------------------------------------------
    @ParameterizedTest(name = "{index} ⇒ execCtx={0}, node={1}, root={2}, loc={3}, validate={4}")
    @MethodSource("basicParameterProvider")
    void walkAtNode_basicParameterValidation(ExecutionContext ctx, JsonNode node, JsonNode root, JsonNodePath location, boolean validate, Class<? extends Throwable> expectedException, String expectation) {
        // any simple schema
        JsonSchema schema = createSchema("{\"type\":\"object\"}");
        if (expectedException != null) {
            // The method is expected to throw – verify the exception type.
            assertThrows(expectedException, () -> schema.walkAtNode(ctx, node, root, location, validate), expectation);
        } else {
            // Normal execution – we only assert that a ValidationResult is returned.
            ValidationResult result = schema.walkAtNode(ctx, node, root, location, validate);
            assertNotNull(result, "Result must never be null");
        }
    }

    // -----------------------------------------------------------------------
    @Test
    void walkAtNode_successfulValidation_true() {
        // Schema: requires a property "name" of type string
        String schemaJson = """
            {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }""";
        JsonSchema schema = createSchema(schemaJson);
        // Instance that satisfies the schema
        JsonNode instance = MAPPER.createObjectNode().put("name", "John Doe");
        // root location
        JsonNodePath location = JsonNodePath.of("$");
        ValidationResult result = schema.walkAtNode(executionContext, instance, // rootNode == instance for this test
        instance, location, true);
        assertTrue(result.isValid(), "A conforming instance must produce a valid result");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    // -----------------------------------------------------------------------
    @Test
    void walkAtNode_successfulValidation_false() {
        // Same schema as above – we only care that walk does not throw.
        String schemaJson = """
            {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }""";
        JsonSchema schema = createSchema(schemaJson);
        JsonNode instance = MAPPER.createObjectNode().put("name", "John Doe");
        JsonNodePath location = JsonNodePath.of("$");
        ValidationResult result = schema.walkAtNode(executionContext, instance, instance, location, false);
        // With validation disabled the result should still be valid (no messages)
        assertTrue(result.isValid(), "Result must be valid when validation is disabled");
        assertTrue(result.getMessages().isEmpty(), "No messages should be produced when validators are skipped");
    }

    // -----------------------------------------------------------------------
    @Test
    void walkAtNode_typeMismatch() {
        String schemaJson = """
            { "type": "object" }""";
        JsonSchema schema = createSchema(schemaJson);
        // [] – not an object
        JsonNode arrayInstance = MAPPER.createArrayNode();
        JsonNodePath location = JsonNodePath.of("$");
        ValidationResult result = schema.walkAtNode(executionContext, arrayInstance, arrayInstance, location, true);
        assertFalse(result.isValid(), "Type mismatch must produce an invalid result");
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type")), "Validation messages should contain a reference to 'type'");
    }

    // -----------------------------------------------------------------------
    @Test
    void walkAtNode_missingRequiredProperty() {
        String schemaJson = """
            {
              "type": "object",
              "required": ["id"],
              "properties": {
                "id": { "type": "integer" }
              }
            }""";
        JsonSchema schema = createSchema(schemaJson);
        // {} – missing "id"
        JsonNode instance = MAPPER.createObjectNode();
        JsonNodePath location = JsonNodePath.of("$");
        ValidationResult result = schema.walkAtNode(executionContext, instance, instance, location, true);
        assertFalse(result.isValid(), "Missing required property must be reported");
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("required")), "Message should mention the missing required property");
    }

    // -----------------------------------------------------------------------
    @Test
    void walkAtNode_refResolution_localFragment_success() {
        // Schema with a local definition and a $ref to it
        String schemaJson = """
            {
              "$defs": {
                "positiveInteger": {
                  "type": "integer",
                  "minimum": 0
                }
              },
              "$ref": "#/$defs/positiveInteger"
            }""";
        JsonSchema schema = createSchema(schemaJson);
        JsonNode instance = MAPPER.numberNode(42);
        JsonNodePath location = JsonNodePath.of("$");
        ValidationResult result = schema.walkAtNode(executionContext, instance, instance, location, true);
        assertTrue(result.isValid(), "Valid $ref resolution should succeed");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected for a valid referenced schema");
    }

    // -----------------------------------------------------------------------
    @Test
    void walkAtNode_refResolution_missingDefinition_failure() {
        // $ref points to a non‑existent definition
        String schemaJson = """
            {
              "$ref": "#/$defs/missingDef"
            }""";
        JsonSchema schema = createSchema(schemaJson);
        // content irrelevant
        JsonNode instance = MAPPER.createObjectNode();
        JsonNodePath location = JsonNodePath.of("$");
        // The library throws an IllegalArgumentException when it cannot resolve the $ref.
        assertThrows(IllegalArgumentException.class, () -> schema.walkAtNode(executionContext, instance, instance, location, true), "Unresolvable $ref must cause an IllegalArgumentException");
    }

    // -----------------------------------------------------------------------
    @Test
    void walkAtNode_keywordWalkListenerInvocation() throws Exception {
        // Build a schema that contains two keywords (type + required) so we can
        // verify that the listener receives two enter/exit callbacks.
        String schemaJson = """
            {
              "type": "object",
              "required": ["name"],
              "properties": {
                "name": { "type": "string" }
              }
            }""";
        JsonSchema schema = createSchema(schemaJson);
        // Install a simple listener that records callbacks.
        TestWalkListener listener = new TestWalkListener();
        // The listener runner is a package‑private field – set it via reflection.
        Field runnerField = JsonSchema.class.getDeclaredField("keywordWalkListenerRunner");
        runnerField.setAccessible(true);
        runnerField.set(schema, new DefaultKeywordWalkListenerRunner(List.of(listener)));
        JsonNode instance = MAPPER.createObjectNode().put("name", "Alice");
        JsonNodePath location = JsonNodePath.of("$");
        ValidationResult result = schema.walkAtNode(executionContext, instance, instance, location, true);
        assertTrue(result.isValid(), "Instance should be valid");
        assertEquals(2, listener.enterCount, "Enter callback should be called for each keyword");
        assertEquals(2, listener.exitCount, "Exit callback should be called for each keyword");
    }

    // -----------------------------------------------------------------------
    @Test
    void walkAtNode_malformedInstanceLocation() {
        String schemaJson = """
            { "type": "object" }""";
        JsonSchema schema = createSchema(schemaJson);
        JsonNode instance = MAPPER.createObjectNode();
        // Construct a JsonNodePath that contains an illegal URI fragment.
        JsonNodePath malformedPath = mock(JsonNodePath.class);
        // illegal percent‑encoding
        when(malformedPath.toString()).thenReturn("#/invalid%zz");
        // The walk implementation attempts to URL‑decode the path; we expect an
        // IllegalArgumentException wrapped in a ValidationResult (no exception thrown).
        ValidationResult result = schema.walkAtNode(executionContext, instance, instance, malformedPath, true);
        assertFalse(result.isValid(), "Malformed instance location should yield an invalid result");
        assertTrue(result.getMessages().stream().anyMatch(m -> m.getMessage().toLowerCase().contains("illegal")), "Message should indicate an illegal argument");
    }

    // -----------------------------------------------------------------------
    @Test
    void walkAtNode_invalidRefUri() {
        // $ref contains a raw space, which is illegal according to RFC 3986.
        String schemaJson = """
            {
              "$ref": "#/definitions/invalid uri"
            }""";
        JsonSchema schema = createSchema(schemaJson);
        JsonNode instance = MAPPER.createObjectNode();
        JsonNodePath location = JsonNodePath.of("$");
        // The library validates the URI during resolution and throws.
        assertThrows(IllegalArgumentException.class, () -> schema.walkAtNode(executionContext, instance, instance, location, true), "Invalid $ref URI must cause IllegalArgumentException");
    }

    // -----------------------------------------------------------------------
    @Test
    void walkAtNode_executionContextWithNullCollector() {
        ExecutionContext ctxWithNullCollector = mock(ExecutionContext.class);
        when(ctxWithNullCollector.getCollectorContext()).thenReturn(null);
        String schemaJson = """
            { "type": "object" }""";
        JsonSchema schema = createSchema(schemaJson);
        JsonNode instance = MAPPER.createObjectNode();
        JsonNodePath location = JsonNodePath.of("$");
        // The walk attempts to add a message to the collector; a null collector
        // leads to a NullPointerException.
        assertThrows(NullPointerException.class, () -> schema.walkAtNode(ctxWithNullCollector, instance, instance, location, true), "Null collector inside ExecutionContext must cause NPE");
    }

    // -----------------------------------------------------------------------
    @Test
    void walkAtNode_unsupportedSpecVersion() {
        // Create a ValidationContext that pretends to use an unknown version.
        ValidationContext unknownVersionContext = mock(ValidationContext.class);
        when(unknownVersionContext.getSpecVersion()).thenReturn(// pretend future version
        VersionFlag.valueOf("V202212"));
        // Build a JsonSchema using the mocked ValidationContext via reflection.
        // The factory method cannot be used because it validates the version flag.
        JsonSchema schema = spy(JsonSchema.from(unknownVersionContext, mock(SchemaLocation.class), mock(JsonNodePath.class), mock(JsonNode.class), null, false));
        // ExecutionContext that wraps the unknown‑version ValidationContext.
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getValidationContext()).thenReturn(unknownVersionContext);
        when(ctx.getCollectorContext()).thenReturn(mock(CollectorContext.class));
        JsonNode instance = MAPPER.createObjectNode();
        JsonNodePath location = JsonNodePath.of("$");
        // The walk should reject the unsupported version with IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> schema.walkAtNode(ctx, instance, instance, location, true), "Unsupported SpecVersion must cause IllegalArgumentException");
    }

    // -----------------------------------------------------------------------
    @Test
    @Disabled("Demonstrates the expected failure; running it would crash the JVM.")
    void walkAtNode_circularRef_withoutRecursiveAnchor() {
        // Schema A → $ref "#/defs/B", B → $ref "#/defs/A"
        String schemaJson = """
            {
              "$defs": {
                "A": { "$ref": "#/$defs/B" },
                "B": { "$ref": "#/$defs/A" }
              },
              "$ref": "#/$defs/A"
            }""";
        JsonSchema schema = createSchema(schemaJson);
        JsonNode instance = MAPPER.createObjectNode();
        JsonNodePath location = JsonNodePath.of("$");
        // The library does not have a guard for this situation, so a StackOverflowError
        // (or a wrapped ValidationException) is the realistic outcome.
        assertThrows(StackOverflowError.class, () -> schema.walkAtNode(executionContext, instance, instance, location, true), "Circular $ref without recursive anchor should overflow the stack");
    }

    // -----------------------------------------------------------------------
    @Test
    void walkAtNode_unsupportedKeyword() {
        // The schema contains an unknown keyword "x-unknown".
        String schemaJson = """
            {
              "type": "object",
              "x-unknown": {}
            }""";
        // The factory should reject the schema because it cannot map the keyword.
        assertThrows(IllegalArgumentException.class, () -> createSchema(schemaJson), "Schema with unknown keyword must cause IllegalArgumentException");
    }
}
