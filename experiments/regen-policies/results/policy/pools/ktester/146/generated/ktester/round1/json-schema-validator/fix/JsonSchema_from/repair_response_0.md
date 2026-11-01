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
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link JsonSchema#from(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema, boolean)}.
 *
 * The original test used Mockito for creating dummy objects, but the project does not
 * include the Mockito dependency.  Simple hand‑crafted dummy implementations are
 * provided instead.
 */
class JsonSchema_from_Test {

    /** Shared ObjectMapper for creating {@link JsonNode} instances. */
    private static ObjectMapper mapper;

    /** ValidationContext used by all test cases. */
    private static ValidationContext validationContext;

    /** Minimal dummy implementation of {@link SchemaLocation}. */
    private static final class DummySchemaLocation implements SchemaLocation {
        @Override public String toString() { return "mocked-schema-location"; }
        @Override public URI getAbsoluteIri() { return null; }
    }

    /** Minimal dummy implementation of {@link JsonNodePath}. */
    private static final class DummyJsonNodePath implements JsonNodePath {
        // No behaviour required for the current tests.
    }

    /** Minimal dummy implementation of {@link JsonSchema} used only as a parent placeholder. */
    private static final class DummyParentSchema extends JsonSchema {
        DummyParentSchema() { super(null, null, null, null, null, false); }
    }

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();

        // Build a minimal but functional ValidationContext (no external resources needed).
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();               // library provides a static instance
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

    /** Supplies data for successful creation tests. */
    private static Stream<Arguments> successfulCreationProvider() {
        JsonNode emptyObject = new ObjectNode(JsonNodeFactory.instance);
        JsonNode emptyArray  = new ArrayNode(JsonNodeFactory.instance);
        JsonNode trueBoolean = BooleanNode.TRUE;
        JsonNode textNode    = new TextNode("example");

        // a few representative combinations – enough to exercise the constructor
        return Stream.of(
                // all non‑null arguments, suppress flag false
                Arguments.of("All arguments non‑null, suppress=false",
                        validationContext,
                        new DummySchemaLocation(),
                        new DummyJsonNodePath(),
                        emptyObject,
                        new DummyParentSchema(),
                        false),

                // all non‑null arguments, suppress flag true
                Arguments.of("All arguments non‑null, suppress=true",
                        validationContext,
                        new DummySchemaLocation(),
                        new DummyJsonNodePath(),
                        emptyObject,
                        new DummyParentSchema(),
                        true),

                // optional nulls (location, path, parent) – still succeeds
                Arguments.of("Optional nulls (location, path, parent)",
                        validationContext,
                        null,
                        null,
                        emptyObject,
                        null,
                        false),

                // various JsonNode implementations
                Arguments.of("ArrayNode as schemaNode",
                        validationContext,
                        new DummySchemaLocation(),
                        new DummyJsonNodePath(),
                        emptyArray,
                        new DummyParentSchema(),
                        false),

                Arguments.of("BooleanNode (true) as schemaNode",
                        validationContext,
                        new DummySchemaLocation(),
                        new DummyJsonNodePath(),
                        trueBoolean,
                        null,
                        false),

                Arguments.of("TextNode as schemaNode",
                        validationContext,
                        new DummySchemaLocation(),
                        new DummyJsonNodePath(),
                        textNode,
                        new DummyParentSchema(),
                        true)
        );
    }

    /** Supplies data for mandatory‑parameter‑null tests. */
    private static Stream<Arguments> nullMandatoryProvider() {
        JsonNode dummyNode = new ObjectNode(JsonNodeFactory.instance);
        return Stream.of(
                Arguments.of("validationContext is null",
                        null,
                        new DummySchemaLocation(),
                        new DummyJsonNodePath(),
                        dummyNode,
                        new DummyParentSchema(),
                        false),

                Arguments.of("schemaNode is null",
                        validationContext,
                        new DummySchemaLocation(),
                        new DummyJsonNodePath(),
                        null,
                        new DummyParentSchema(),
                        false)
        );
    }

    @Test
    void fromCreatesJsonSchemaInstance() throws Exception {
        // ----- given ---------------------------------------------------------
        JsonNode schemaNode = mapper.readTree("{}");
        SchemaLocation schemaLocation = new DummySchemaLocation();
        JsonNodePath evaluationPath = new DummyJsonNodePath();
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
        assertNotNull(result.getValidators(), "Validators list should be initialised (may be empty)");
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
                () -> JsonSchema.from(ctx,
                                      schemaLocation,
                                      evaluationPath,
                                      schemaNode,
                                      parent,
                                      suppress),
                description);
    }

    @Test
    void malformedRefThrowsUnsupportedEncodingException() throws Exception {
        ObjectNode schemaNode = mapper.createObjectNode()
                .put("$ref", "http://example.com/invalid%ZZschema.json");
        SchemaLocation location = new DummySchemaLocation();
        JsonNodePath path = new DummyJsonNodePath();

        assertThrows(UnsupportedEncodingException.class,
                () -> JsonSchema.from(validationContext,
                                      location,
                                      path,
                                      schemaNode,
                                      new DummyParentSchema(),
                                      false),
                "Malformed $ref should cause UnsupportedEncodingException");
    }

    @Test
    void nonExistentRefThrowsIOException() throws Exception {
        ObjectNode schemaNode = mapper.createObjectNode()
                .put("$ref", "http://nonexistent.example.com/schema.json");
        SchemaLocation location = new DummySchemaLocation();
        JsonNodePath path = new DummyJsonNodePath();

        assertThrows(IOException.class,
                () -> JsonSchema.from(validationContext,
                                      location,
                                      path,
                                      schemaNode,
                                      new DummyParentSchema(),
                                      false),
                "Non‑existent $ref should cause IOException");
    }

    @Test
    void primitiveNumericNodeThrowsIllegalArgumentException() throws Exception {
        JsonNode numericNode = new IntNode(123);
        SchemaLocation location = new DummySchemaLocation();
        JsonNodePath path = new DummyJsonNodePath();

        assertThrows(IllegalArgumentException.class,
                () -> JsonSchema.from(validationContext,
                                      location,
                                      path,
                                      numericNode,
                                      new DummyParentSchema(),
                                      false),
                "Numeric schema node should cause IllegalArgumentException");
    }

    @Test
    void circularReferenceThrowsJsonSchemaException() throws Exception {
        ObjectNode schemaNode = mapper.createObjectNode().put("$ref", "#");
        SchemaLocation location = new DummySchemaLocation();
        JsonNodePath path = new DummyJsonNodePath();

        assertThrows(JsonSchemaException.class,
                () -> JsonSchema.from(validationContext,
                                      location,
                                      path,
                                      schemaNode,
                                      new DummyParentSchema(),
                                      false),
                "Self‑referencing $ref should cause JsonSchemaException");
    }

    @Test
    void refRequiresParentButParentIsNullThrowsIllegalStateException() throws Exception {
        ObjectNode schemaNode = mapper.createObjectNode().put("$ref", "#/definitions/foo");
        SchemaLocation location = new DummySchemaLocation();
        JsonNodePath path = new DummyJsonNodePath();

        assertThrows(IllegalStateException.class,
                () -> JsonSchema.from(validationContext,
                                      location,
                                      path,
                                      schemaNode,
                                      null,
                                      false),
                "Missing parent for $ref resolution should cause IllegalStateException");
    }
}
```