```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for the package‑private static {@code JsonSchema.from(...)} factory method.
 *
 * The test suite covers:
 * <ul>
 *   <li>Successful creation with various combinations of arguments (including optional nulls).</li>
 *   <li>Creation with different {@link JsonNode} implementations.</li>
 *   <li>Failure scenarios that are expected to throw specific exceptions.</li>
 * </ul>
 *
 * The {@code @BeforeEach} method builds a real {@link ValidationContext} that can be reused for the
 * “happy‑path” tests. For the failure tests that require a {@code null} validationContext we use a
 * Mockito mock (or simply pass {@code null}) as appropriate.
 *
 * All tests interact with the public API only; no private fields or methods are accessed
 * directly.  Where the factory method is expected to throw, {@link Assertions#assertThrows}
 * is used to verify the exact exception type.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    /** Shared ObjectMapper for creating {@link JsonNode} instances. */
    private static ObjectMapper mapper;

    /** ValidationContext used by the successful‑creation tests. */
    private ValidationContext validationContext;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal but functional ValidationContext (no external resources needed).
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012Instance();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(metaSchema, factory, config,
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>(),
                new java.util.concurrent.ConcurrentHashMap<>());
    }

    /* -------------------------------------------------------------
     *  Helper methods for building arguments used by the parameterized
     *  tests below.
     * ------------------------------------------------------------- */

    /** Creates a mocked {@link SchemaLocation} that safely returns a string. */
    private static SchemaLocation mockSchemaLocation() {
        SchemaLocation location = mock(SchemaLocation.class);
        when(location.toString()).thenReturn("mocked-schema-location");
        when(location.getAbsoluteIri()).thenReturn(null);
        return location;
    }

    /** Creates a mocked {@link JsonNodePath}. */
    private static JsonNodePath mockJsonNodePath() {
        return mock(JsonNodePath.class);
    }

    /** Creates a mocked parent {@link JsonSchema}. */
    private static JsonSchema mockParentSchema() {
        return mock(JsonSchema.class);
    }

    /* -------------------------------------------------------------
     *  Successful creation test groups
     * ------------------------------------------------------------- */

    /**
     * Parameterized test that verifies successful creation of {@link JsonSchema}
     * for a wide range of argument combinations where no exception is expected.
     *
     * The test data is supplied by {@link #successfulCreationProvider()}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulCreationProvider")
    void successfulCreation(String description,
                           ValidationContext ctx,
                           SchemaLocation schemaLocation,
                           JsonNodePath evaluationPath,
                           JsonNode schemaNode,
                           JsonSchema parent,
                           boolean suppressSubSchemaRetrieval) {
        // Act
        JsonSchema result = JsonSchema.from(ctx, schemaLocation, evaluationPath,
                schemaNode, parent, suppressSubSchemaRetrieval);

        // Assert
        assertNotNull(result, description + " – result must not be null");
        assertSame(ctx, result.getValidationContext(),
                description + " – ValidationContext must be the one supplied");
        assertNotNull(result.getValidators(),
                description + " – validators list must be initialised");
        // The exact validator count depends on the supplied schema; we only assert non‑null.
    }

    /** Supplies the data for {@link #successfulCreation(...)}. */
    private static Stream<Arguments> successfulCreationProvider() {
        JsonNode emptyObject = new ObjectNode(JsonNodeFactory.instance);
        JsonNode emptyArray = new ArrayNode(JsonNodeFactory.instance);
        JsonNode trueBoolean = BooleanNode.TRUE;
        JsonNode textNode = new TextNode("example");
        JsonNode intNode = new IntNode(123); // used only for failure case, not here

        return Stream.of(
                // All non‑null arguments, suppress flag false
                Arguments.of("All arguments non‑null, suppress=false",
                        new ValidationContext(JsonMetaSchema.getV202012Instance(),
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012),
                                SchemaValidatorsConfig.builder().build(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>()),
                        mockSchemaLocation(),
                        mockJsonNodePath(),
                        emptyObject,
                        mockParentSchema(),
                        false),

                // All non‑null arguments, suppress flag true
                Arguments.of("All arguments non‑null, suppress=true",
                        new ValidationContext(JsonMetaSchema.getV202012Instance(),
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012),
                                SchemaValidatorsConfig.builder().build(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                        new java.util.concurrent.ConcurrentHashMap<>(),
                        new java.util.concurrent.ConcurrentHashMap<>()),
                        mockSchemaLocation(),
                        mockJsonNodePath(),
                        emptyObject,
                        mockParentSchema(),
                        true),

                // Optional nulls (schemaLocation, evaluationPath, parent) – still succeeds
                Arguments.of("Optional nulls (location, path, parent)",
                        new ValidationContext(JsonMetaSchema.getV202012Instance(),
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012),
                                SchemaValidatorsConfig.builder().build(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>()),
                        null,
                        null,
                        emptyObject,
                        null,
                        false),

                // Mixed optional nulls with suppress=true
                Arguments.of("Mixed optional nulls, suppress=true (location null)",
                        new ValidationContext(JsonMetaSchema.getV202012Instance(),
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012),
                                SchemaValidatorsConfig.builder().build(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>()),
                        null,
                        mockJsonNodePath(),
                        emptyObject,
                        null,
                        true),

                Arguments.of("Mixed optional nulls, suppress=true (path null)",
                        new ValidationContext(JsonMetaSchema.getV202012Instance(),
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012),
                                SchemaValidatorsConfig.builder().build(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>()),
                        mockSchemaLocation(),
                        null,
                        emptyObject,
                        mockParentSchema(),
                        true),

                Arguments.of("Mixed optional nulls, suppress=true (both location & path null)",
                        new ValidationContext(JsonMetaSchema.getV202012Instance(),
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012),
                                SchemaValidatorsConfig.builder().build(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>()),
                        null,
                        null,
                        emptyObject,
                        mockParentSchema(),
                        true),

                // Various JsonNode implementations (ArrayNode, BooleanNode, TextNode)
                Arguments.of("ArrayNode as schemaNode",
                        new ValidationContext(JsonMetaSchema.getV202012Instance(),
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012),
                                SchemaValidatorsConfig.builder().build(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>()),
                        mockSchemaLocation(),
                        mockJsonNodePath(),
                        emptyArray,
                        mockParentSchema(),
                        false),

                Arguments.of("BooleanNode (true) as schemaNode",
                        new ValidationContext(JsonMetaSchema.getV202012Instance(),
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012),
                                SchemaValidatorsConfig.builder().build(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>()),
                        mockSchemaLocation(),
                        mockJsonNodePath(),
                        trueBoolean,
                        null,
                        false),

                Arguments.of("TextNode as schemaNode",
                        new ValidationContext(JsonMetaSchema.getV202012Instance(),
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012),
                                SchemaValidatorsConfig.builder().build(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>()),
                        mockSchemaLocation(),
                        mockJsonNodePath(),
                        textNode,
                        mockParentSchema(),
                        true),

                // Parent null with both flag variations
                Arguments.of("Parent null, suppress=false",
                        new ValidationContext(JsonMetaSchema.getV202012Instance(),
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012),
                                SchemaValidatorsConfig.builder().build(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>()),
                        mockSchemaLocation(),
                        mockJsonNodePath(),
                        emptyObject,
                        null,
                        false),

                Arguments.of("Parent null, suppress=true",
                        new ValidationContext(JsonMetaSchema.getV202012Instance(),
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012),
                                SchemaValidatorsConfig.builder().build(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>()),
                        mockSchemaLocation(),
                        mockJsonNodePath(),
                        emptyObject,
                        null,
                        true)
        );
    }

    /* -------------------------------------------------------------
     *  Failure test groups – expecting specific exceptions
     * ------------------------------------------------------------- */

    /**
     * Tests that a {@link NullPointerException} is thrown when mandatory parameters
     * are {@code null}.  Two distinct scenarios are covered.
     */
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
                () -> JsonSchema.from(ctx, schemaLocation, evaluationPath,
                        schemaNode, parent, suppress),
                description);
    }

    private static Stream<Arguments> nullMandatoryProvider() {
        JsonNode dummyNode = new ObjectNode(JsonNodeFactory.instance);
        return Stream.of(
                Arguments.of("validationContext is null",
                        null,
                        mockSchemaLocation(),
                        mockJsonNodePath(),
                        dummyNode,
                        mockParentSchema(),
                        false),

                Arguments.of("schemaNode is null",
                        new ValidationContext(JsonMetaSchema.getV202012Instance(),
                                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012),
                                SchemaValidatorsConfig.builder().build(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>(),
                                new java.util.concurrent.ConcurrentHashMap<>()),
                        mockSchemaLocation(),
                        mockJsonNodePath(),
                        null,
                        mockParentSchema(),
                        false)
        );
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
        // ----- given ---------------------------------------------------------
        ObjectNode schemaNode = mapper.createObjectNode()
                .put("$ref", "http://example.com/invalid%ZZschema.json");

        SchemaLocation location = mockSchemaLocation();
        JsonNodePath path = mockJsonNodePath();

        // ----- when & then --------------------------------------------------
        assertThrows(UnsupportedEncodingException.class,
                () -> JsonSchema.from(validationContext, location, path,
                        schemaNode, mockParentSchema(), false),
                "Malformed $ref should cause UnsupportedEncodingException");
    }

    /**
     * Tests that a {@code $ref} pointing to a non‑existent external resource
     * results in an {@link IOException}.
     *
     * The actual IOException is thrown by the internal schema loading logic.
     */
    @Test
    void nonExistentRefThrowsIOException() throws Exception {
        // ----- given ---------------------------------------------------------
        ObjectNode schemaNode = mapper.createObjectNode()
                .put("$ref", "http://nonexistent.example.com/schema.json");

        SchemaLocation location = mockSchemaLocation();
        JsonNodePath path = mockJsonNodePath();

        // ----- when & then --------------------------------------------------
        assertThrows(IOException.class,
                () -> JsonSchema.from(validationContext, location, path,
                        schemaNode, mockParentSchema(), false),
                "Non‑existent $ref should cause IOException");
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
        // ----- given ---------------------------------------------------------
        JsonNode numericNode = new IntNode(123);
        SchemaLocation location = mockSchemaLocation();
        JsonNodePath path = mockJsonNodePath();

        // ----- when & then --------------------------------------------------
        assertThrows(IllegalArgumentException.class,
                () -> JsonSchema.from(validationContext, location, path,
                        numericNode, mockParentSchema(), false),
                "Numeric schema node should cause IllegalArgumentException");
    }

    /**
     * Tests that a circular reference (self‑referencing {@code $ref}) results in a
     * {@link JsonSchemaException}.
     *
     * The schema node contains {@code {"$ref":"#"}} which points to itself.
     */
    @Test
    void circularReferenceThrowsJsonSchemaException() throws Exception {
        // ----- given ---------------------------------------------------------
        ObjectNode schemaNode = mapper.createObjectNode()
                .put("$ref", "#"); // self‑reference

        SchemaLocation location = mockSchemaLocation();
        JsonNodePath path = mockJsonNodePath();

        // ----- when & then --------------------------------------------------
        assertThrows(JsonSchemaException.class,
                () -> JsonSchema.from(validationContext, location, path,
                        schemaNode, mockParentSchema(), false),
                "Self‑referencing $ref should cause JsonSchemaException");
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
        // ----- given ---------------------------------------------------------
        ObjectNode schemaNode = mapper.createObjectNode()
                .put("$ref", "#/definitions/foo"); // requires parent for resolution

        SchemaLocation location = mockSchemaLocation();
        JsonNodePath path = mockJsonNodePath();

        // ----- when & then --------------------------------------------------
        assertThrows(IllegalStateException.class,
                () -> JsonSchema.from(validationContext, location, path,
                        schemaNode, null, false),
                "Missing parent for $ref resolution should cause IllegalStateException");
    }

    /* -------------------------------------------------------------
     *  Original single test retained for completeness
     * ------------------------------------------------------------- */

    /**
     * Verifies that {@code JsonSchema.from(...)} creates a usable {@link JsonSchema}
     * instance with the supplied arguments (basic happy‑path test).
     */
    @Test
    void fromCreatesJsonSchemaInstance() throws Exception {
        // ----- given ---------------------------------------------------------
        JsonNode schemaNode = mapper.readTree("{}");

        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        when(schemaLocation.toString()).thenReturn("mocked-schema-location");
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);

        JsonNodePath evaluationPath = mock(JsonNodePath.class);

        JsonSchema parent = null;
        boolean suppressSubSchemaRetrieval = false;

        // ----- when ----------------------------------------------------------
        JsonSchema result = JsonSchema.from(validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                parent,
                suppressSubSchemaRetrieval);

        // ----- then ----------------------------------------------------------
        assertNotNull(result, "JsonSchema.from should not return null");
        assertSame(validationContext, result.getValidationContext(),
                "The ValidationContext inside the created JsonSchema must be the one supplied");
        assertNotNull(result.getValidators(),
                "Validators list should be initialised (may be empty)");
        assertTrue(result.getValidators().isEmpty(),
                "An empty schema should result in an empty validators list");
    }
}
```