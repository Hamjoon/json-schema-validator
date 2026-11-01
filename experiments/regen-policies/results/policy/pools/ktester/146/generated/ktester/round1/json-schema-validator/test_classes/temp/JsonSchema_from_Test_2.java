package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.net.URI;

@ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    /**
     * Shared ObjectMapper for creating JsonNode instances.
     */
    private static ObjectMapper mapper;

    /**
     * ValidationContext used by all test cases.
     */
    private ValidationContext validationContext;

    /**
     * Shared ObjectMapper for creating {@link JsonNode} instances.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Minimal dummy implementation of {@link SchemaLocation}.
     */
    private static final class DummySchemaLocation implements SchemaLocation {

        @Override
        public String toString() {
            return "mocked-schema-location";
        }

        @Override
        public URI getAbsoluteIri() {
            return null;
        }
    }

    /**
     * Minimal dummy implementation of {@link JsonNodePath}.
     */
    private static final class DummyJsonNodePath implements JsonNodePath {
        // No behaviour required for the current tests.
    }

    /**
     * Minimal dummy implementation of {@link JsonSchema} used only as a parent placeholder.
     */
    private static final class DummyParentSchema extends JsonSchema {

        DummyParentSchema() {
            super(null, null, null, null, null, false);
        }
    }

    /**
     * Simple {@link SchemaLocation} implementation used in tests.
     */
    private static final class SimpleSchemaLocation extends SchemaLocation {

        SimpleSchemaLocation() {
            super("dummy-location");
        }
    }

    /**
     * Simple {@link JsonNodePath} implementation used in tests (no behaviour needed).
     */
    private static final class SimpleJsonNodePath extends JsonNodePath {
        // No additional behaviour required for the current test scenarios.
    }

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        // Build a minimal but functional ValidationContext (no external resources needed).
        // library provides a static instance
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(metaSchema, factory, config, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(metaSchema, factory, config, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal but functional ValidationContext (no external resources needed).
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012Instance();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(metaSchema, factory, config, new java.util.concurrent.ConcurrentHashMap<>(), new java.util.concurrent.ConcurrentHashMap<>(), new java.util.concurrent.ConcurrentHashMap<>());
        // Re‑initialise the context before each test to avoid cross‑test contamination.
        initValidationContext();
    }

    /**
     * Creates a mocked {@link SchemaLocation} that safely returns a string.
     */
    private static SchemaLocation mockSchemaLocation() {
        SchemaLocation location = mock(SchemaLocation.class);
        when(location.toString()).thenReturn("mocked-schema-location");
        when(location.getAbsoluteIri()).thenReturn(null);
        return location;
    }

    /**
     * Creates a mocked {@link JsonNodePath}.
     */
    private static JsonNodePath mockJsonNodePath() {
        return mock(JsonNodePath.class);
    }

    /**
     * Creates a mocked parent {@link JsonSchema}.
     */
    private static JsonSchema mockParentSchema() {
        return mock(JsonSchema.class);
    }

    /**
     * Supplies the data for {@link #successfulCreation(...)}.
     */
    private static Stream<Arguments> successfulCreationProvider() {
        JsonNode emptyObject = new ObjectNode(JsonNodeFactory.instance);
        JsonNode emptyArray = new ArrayNode(JsonNodeFactory.instance);
        JsonNode trueBoolean = BooleanNode.TRUE;
        JsonNode textNode = new TextNode("example");
        JsonSchema parent = createParentSchema();
        return Stream.of(// all non‑null arguments, suppress = false
        Arguments.of("All arguments non‑null, suppress=false", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyObject, parent, false), // all non‑null arguments, suppress = true
        Arguments.of("All arguments non‑null, suppress=true", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyObject, parent, true), // optional nulls (location, path, parent) – still succeeds
        Arguments.of("Optional nulls (location, path, parent)", validationContext, null, null, emptyObject, null, false), // various JsonNode implementations
        Arguments.of("ArrayNode as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyArray, parent, false), Arguments.of("BooleanNode (true) as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), trueBoolean, null, false), Arguments.of("TextNode as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), textNode, parent, true));
    }

    private static Stream<Arguments> nullMandatoryProvider() {
        JsonNode dummyNode = new ObjectNode(JsonNodeFactory.instance);
        JsonSchema parent = createParentSchema();
        return Stream.of(Arguments.of("validationContext is null", null, new SimpleSchemaLocation(), new SimpleJsonNodePath(), dummyNode, parent, false), Arguments.of("schemaNode is null", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), null, parent, false));
    }

    /**
     * Helper to create a minimal parent {@link JsonSchema} instance.
     */
    private static JsonSchema createParentSchema() {
        // An empty object schema is sufficient as a parent placeholder.
        JsonNode emptyObject = new ObjectNode(JsonNodeFactory.instance);
        return JsonSchema.from(validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyObject, null, false);
    }

    /**
     * Verifies that {@code JsonSchema.from(...)} creates a usable {@link JsonSchema}
     * instance with the supplied arguments.
     */
    @Test
    void fromCreatesJsonSchemaInstance() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{}");
        SchemaLocation schemaLocation = new SimpleSchemaLocation();
        JsonNodePath evaluationPath = new SimpleJsonNodePath();
        JsonSchema result = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        assertNotNull(result, "JsonSchema.from should not return null");
        assertSame(validationContext, result.getValidationContext(), "The ValidationContext inside the created JsonSchema must be the one supplied");
        assertNotNull(result.getValidators(), "Validators list should be initialised (may be empty)");
        assertTrue(result.getValidators().isEmpty(), "An empty schema should result in an empty validators list");
    }

    /**
     * Parameterized test that verifies successful creation of {@link JsonSchema}
     * for a wide range of argument combinations where no exception is expected.
     *
     * The test data is supplied by {@link #successfulCreationProvider()}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulCreationProvider")
    void successfulCreation(String description, ValidationContext ctx, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parent, boolean suppressSubSchemaRetrieval) {
        JsonSchema result = JsonSchema.from(ctx, schemaLocation, evaluationPath, schemaNode, parent, suppressSubSchemaRetrieval);
        assertNotNull(result, description + " – result must not be null");
        assertSame(ctx, result.getValidationContext(), description + " – ValidationContext must be the one supplied");
        assertNotNull(result.getValidators(), description + " – validators list must be initialised");
    }

    /**
     * Tests that a {@link NullPointerException} is thrown when mandatory parameters
     * are {@code null}.  Two distinct scenarios are covered.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullMandatoryProvider")
    void nullMandatoryParameters(String description, ValidationContext ctx, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parent, boolean suppress) {
        assertThrows(NullPointerException.class, () -> JsonSchema.from(ctx, schemaLocation, evaluationPath, schemaNode, parent, suppress), description);
    }

    /**
     * Tests that a malformed {@code $ref} containing an illegal percent‑encoding
     * triggers an {@link UnsupportedEncodingException}.
     *
     * The schema node is built as {@code {"$ref":"http://example.com/invalid%ZZschema.json"}}.
     * The constructor of {@link JsonSchema} attempts to URL‑decode the reference,
     * which should raise the exception.
     */
    @Test
    void malformedRefThrowsUnsupportedEncodingException() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("$ref", "http://example.com/invalid%ZZschema.json");
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();
        assertThrows(UnsupportedEncodingException.class, () -> JsonSchema.from(validationContext, location, path, schemaNode, createParentSchema(), false), "Malformed $ref should cause UnsupportedEncodingException");
    }

    /**
     * Tests that a {@code $ref} pointing to a non‑existent external resource
     * results in an {@link IOException}.
     *
     * The actual IOException is thrown by the internal schema loading logic.
     */
    @Test
    void nonExistentRefThrowsIOException() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("$ref", "http://nonexistent.example.com/schema.json");
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();
        assertThrows(IOException.class, () -> JsonSchema.from(validationContext, location, path, schemaNode, createParentSchema(), false), "Non‑existent $ref should cause IOException");
    }

    /**
     * Tests that providing a primitive numeric {@link JsonNode} (e.g., {@code IntNode})
     * leads to an {@link IllegalArgumentException}.
     *
     * The constructor expects a schema node that can represent a JSON‑Schema
     * (object, array or boolean).  Supplying a scalar numeric node is illegal.
     */
    @Test
    void primitiveNumericNodeThrowsIllegalArgumentException() throws Exception {
        JsonNode numericNode = new IntNode(123);
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, location, path, numericNode, createParentSchema(), false), "Numeric schema node should cause IllegalArgumentException");
    }

    /**
     * Tests that a circular reference (self‑referencing {@code $ref}) results in a
     * {@link JsonSchemaException}.
     *
     * The schema node contains {@code {"$ref":"#"}} which points to itself.
     */
    @Test
    void circularReferenceThrowsJsonSchemaException() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("$ref", "#");
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();
        assertThrows(JsonSchemaException.class, () -> JsonSchema.from(validationContext, location, path, schemaNode, createParentSchema(), false), "Self‑referencing $ref should cause JsonSchemaException");
    }

    /**
     * Tests that when a {@code $ref} needs to be resolved against a parent schema
     * but the {@code parent} argument is {@code null}, an {@link IllegalStateException}
     * is thrown.
     *
     * The schema node contains a relative reference that requires a parent for resolution.
     */
    @Test
    void refRequiresParentButParentIsNullThrowsIllegalStateException() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("$ref", "#/definitions/foo");
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();
        assertThrows(IllegalStateException.class, () -> JsonSchema.from(validationContext, location, path, schemaNode, null, false), "Missing parent for $ref resolution should cause IllegalStateException");
    }
}
