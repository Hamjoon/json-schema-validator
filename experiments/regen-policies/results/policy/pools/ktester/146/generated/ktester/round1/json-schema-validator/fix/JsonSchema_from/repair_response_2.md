```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link JsonSchema#from(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema, boolean)}.
 */
@ExtendWith({})
class JsonSchema_from_Test {

    /** Shared ObjectMapper for creating JsonNode instances. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** ValidationContext used by all test cases (static so it can be used in @BeforeAll). */
    private static ValidationContext validationContext;

    /** Minimal dummy implementation of {@link SchemaLocation}. */
    private static final class SimpleSchemaLocation extends SchemaLocation {
        SimpleSchemaLocation() {
            // The constructor of SchemaLocation expects an AbsoluteIri; we pass null for the test.
            super((AbsoluteIri) null);
        }
    }

    /** Minimal dummy implementation of {@link JsonNodePath}. */
    private static final class SimpleJsonNodePath extends JsonNodePath {
        SimpleJsonNodePath() {
            // Use the ROOT path type – sufficient for our tests.
            super(PathType.ROOT);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Build a minimal but functional ValidationContext (no external resources needed).
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(
                metaSchema,
                factory,
                config,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Re‑initialise the context before each test to avoid cross‑test contamination.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(
                metaSchema,
                factory,
                config,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());
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

        return Stream.of(
                // all non‑null arguments, suppress = false
                Arguments.of("All arguments non‑null, suppress=false", validationContext,
                        new SimpleSchemaLocation(), new SimpleJsonNodePath(),
                        emptyObject, parent, false),

                // all non‑null arguments, suppress = true
                Arguments.of("All arguments non‑null, suppress=true", validationContext,
                        new SimpleSchemaLocation(), new SimpleJsonNodePath(),
                        emptyObject, parent, true),

                // optional nulls (location, path, parent) – still succeeds
                Arguments.of("Optional nulls (location, path, parent)", validationContext,
                        null, null, emptyObject, null, false),

                // various JsonNode implementations
                Arguments.of("ArrayNode as schemaNode", validationContext,
                        new SimpleSchemaLocation(), new SimpleJsonNodePath(),
                        emptyArray, parent, false),

                Arguments.of("BooleanNode (true) as schemaNode", validationContext,
                        new SimpleSchemaLocation(), new SimpleJsonNodePath(),
                        trueBoolean, null, false),

                Arguments.of("TextNode as schemaNode", validationContext,
                        new SimpleSchemaLocation(), new SimpleJsonNodePath(),
                        textNode, parent, true)
        );
    }

    private static Stream<Arguments> nullMandatoryProvider() {
        JsonNode dummyNode = new ObjectNode(JsonNodeFactory.instance);
        JsonSchema parent = createParentSchema();

        return Stream.of(
                Arguments.of("validationContext is null", null,
                        new SimpleSchemaLocation(), new SimpleJsonNodePath(),
                        dummyNode, parent, false),

                Arguments.of("schemaNode is null", validationContext,
                        new SimpleSchemaLocation(), new SimpleJsonNodePath(),
                        null, parent, false)
        );
    }

    /**
     * Helper to create a minimal parent {@link JsonSchema} instance.
     */
    private static JsonSchema createParentSchema() {
        JsonNode emptyObject = new ObjectNode(JsonNodeFactory.instance);
        return JsonSchema.from(validationContext,
                new SimpleSchemaLocation(),
                new SimpleJsonNodePath(),
                emptyObject,
                null,
                false);
    }

    @Test
    void fromCreatesJsonSchemaInstance() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{}");
        SchemaLocation schemaLocation = new SimpleSchemaLocation();
        JsonNodePath evaluationPath = new SimpleJsonNodePath();

        JsonSchema result = JsonSchema.from(validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                null,
                false);

        assertNotNull(result, "JsonSchema.from should not return null");
        assertSame(validationContext, result.getValidationContext(),
                "The ValidationContext inside the created JsonSchema must be the one supplied");
        assertNotNull(result.getValidators(),
                "Validators list should be initialised (may be empty)");
        assertTrue(result.getValidators().isEmpty(),
                "An empty schema should result in an empty validators list");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulCreationProvider")
    void successfulCreation(String description,
                            ValidationContext ctx,
                            SchemaLocation schemaLocation,
                            JsonNodePath evaluationPath,
                            JsonNode schemaNode,
                            JsonSchema parent,
                            boolean suppressSubSchemaRetrieval) {

        JsonSchema result = JsonSchema.from(ctx,
                schemaLocation,
                evaluationPath,
                schemaNode,
                parent,
                suppressSubSchemaRetrieval);

        assertNotNull(result, description + " – result must not be null");
        assertSame(ctx, result.getValidationContext(),
                description + " – ValidationContext must be the one supplied");
        assertNotNull(result.getValidators(),
                description + " – validators list must be initialised");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullMandatoryProvider")
    void nullMandatoryParameters(String description,
                                 ValidationContext ctx,
                                 SchemaLocation schemaLocation,
                                 JsonNodePath evaluationPath,
                                 JsonNode schemaNode,
                                 JsonSchema parent,
                                 boolean suppress) {

        assertThrows(NullPointerException.class,
                () -> JsonSchema.from(ctx, schemaLocation, evaluationPath, schemaNode, parent, suppress),
                description);
    }

    @Test
    void malformedRefThrowsUnsupportedEncodingException() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode()
                .put("$ref", "http://example.com/invalid%ZZschema.json");
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();

        assertThrows(UnsupportedEncodingException.class,
                () -> JsonSchema.from(validationContext, location, path, schemaNode,
                        createParentSchema(), false),
                "Malformed $ref should cause UnsupportedEncodingException");
    }

    @Test
    void nonExistentRefThrowsIOException() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode()
                .put("$ref", "http://nonexistent.example.com/schema.json");
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();

        assertThrows(IOException.class,
                () -> JsonSchema.from(validationContext, location, path, schemaNode,
                        createParentSchema(), false),
                "Non‑existent $ref should cause IOException");
    }

    @Test
    void primitiveNumericNodeThrowsIllegalArgumentException() throws Exception {
        JsonNode numericNode = new IntNode(123);
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();

        assertThrows(IllegalArgumentException.class,
                () -> JsonSchema.from(validationContext, location, path, numericNode,
                        createParentSchema(), false),
                "Numeric schema node should cause IllegalArgumentException");
    }

    @Test
    void circularReferenceThrowsJsonSchemaException() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("$ref", "#");
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();

        assertThrows(JsonSchemaException.class,
                () -> JsonSchema.from(validationContext, location, path, schemaNode,
                        createParentSchema(), false),
                "Self‑referencing $ref should cause JsonSchemaException");
    }

    @Test
    void refRequiresParentButParentIsNullThrowsIllegalStateException() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("$ref", "#/definitions/foo");
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();

        assertThrows(IllegalStateException.class,
                () -> JsonSchema.from(validationContext, location, path, schemaNode,
                        null, false),
                "Missing parent for $ref resolution should cause IllegalStateException");
    }
}
```