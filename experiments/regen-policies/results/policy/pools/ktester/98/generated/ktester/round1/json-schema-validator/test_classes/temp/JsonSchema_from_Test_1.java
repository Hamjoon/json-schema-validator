package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;

    private JsonNode schemaNode;

    @Mock
    private SchemaLocation mockSchemaLocation;

    @Mock
    private JsonNodePath mockEvaluationPath;

    @Mock
    private JsonSchema mockParentSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal ValidationContext – we use the default meta‑schema
        // (draft‑2020‑12) and a default JsonSchemaFactory.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(metaSchema, factory, config);
        // A tiny schema node – an empty object is sufficient for construction.
        schemaNode = MAPPER.createObjectNode();
    }

    private static Stream<JsonNode> validNodeProvider() {
        JsonNodeFactory f = JsonNodeFactory.instance;
        return Stream.of(f.objectNode(), f.arrayNode(), f.booleanNode(true));
    }

    @Test
    void from_WithParent_ShouldCreateSchemaWithSameValidationContext() {
        // Act
        JsonSchema result = JsonSchema.from(validationContext, // SchemaLocation – not needed for this test
        null, // EvaluationPath – not needed for this test
        null, schemaNode, // parent schema – none
        null, false);
        // Assert
        assertNotNull(result, "JsonSchema.from should not return null");
        assertSame(validationContext, result.getValidationContext(), "The created JsonSchema must expose the same ValidationContext that was supplied");
    }

    @Test
    void from_WithoutParent_ShouldCreateSchemaWithSameValidationContext() {
        // Act
        JsonSchema result = JsonSchema.from(validationContext, // SchemaLocation
        null, // EvaluationPath
        null, schemaNode, // no parent schema
        null, // suppressSubSchemaRetrieval flag true
        true);
        // Assert
        assertNotNull(result, "JsonSchema.from should not return null when parent is null");
        assertSame(validationContext, result.getValidationContext(), "The created JsonSchema must expose the same ValidationContext that was supplied");
    }

    @Test
    void from_NullValidationContext_ShouldCreateSchemaWithNullContext() {
        JsonSchema result = JsonSchema.from(null, null, null, schemaNode, null, false);
        assertNotNull(result, "Result must not be null even when ValidationContext is null");
        assertNull(result.getValidationContext(), "getValidationContext() should return null when a null context was supplied");
    }

    @Test
    void from_NullSchemaLocation_ShouldCreateSchemaWithNullId() {
        JsonSchema result = JsonSchema.from(validationContext, null, null, schemaNode, null, false);
        assertNotNull(result, "Result must not be null when SchemaLocation is null");
        assertNull(result.getId(), "getId() should be null because no SchemaLocation was supplied");
    }

    @Test
    void from_NullEvaluationPath_ShouldCreateSchemaWithoutException() {
        JsonSchema result = JsonSchema.from(validationContext, null, null, schemaNode, null, false);
        assertNotNull(result, "Result must not be null when evaluationPath is null");
    }

    @Test
    void from_NullSchemaNode_ShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> JsonSchema.from(validationContext, null, null, null, null, false), "Passing a null schemaNode must result in NPE");
    }

    @ParameterizedTest
    @MethodSource("validNodeProvider")
    void from_ValidSchemaNodeTypes_ShouldCreateInstance(JsonNode node) {
        JsonSchema result = JsonSchema.from(validationContext, null, null, node, null, false);
        assertNotNull(result, "Factory must return a non‑null JsonSchema for node: " + node.getNodeType());
        // Basic sanity: the instance should be usable for validation without throwing.
        assertDoesNotThrow(() -> result.validate(node), "Calling validate on the created schema should not throw");
    }

    @Test
    void from_NumericSchemaNode_ShouldThrowIllegalArgumentException() {
        JsonNode numericNode = JsonNodeFactory.instance.numberNode(42);
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, null, null, numericNode, null, false), "Numeric node is not a valid JSON Schema representation");
    }

    @Test
    void from_TextSchemaNode_ShouldThrowIllegalArgumentException() {
        JsonNode textNode = JsonNodeFactory.instance.textNode("not a schema");
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, null, null, textNode, null, false), "Text node is not a valid JSON Schema representation");
    }

    @Test
    void from_MissingSchemaNode_ShouldThrowIllegalArgumentException() {
        JsonNode missingNode = JsonNodeFactory.instance.missingNode();
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, null, null, missingNode, null, false), "Missing node must be rejected");
    }

    @Test
    void from_SuppressTrue_NoParent_SubSchemaRetrievalIsSuppressed() {
        JsonSchema schema = JsonSchema.from(validationContext, null, null, schemaNode, null, true);
        // Any attempt to resolve a sub‑schema must return null.
        assertNull(schema.getSubSchema(null), "When suppressSubSchemaRetrieval is true, getSubSchema must return null");
    }

    @Test
    void from_WithParent_SuppressFalse_FindAncestorReturnsParent() {
        // Create a parent schema first.
        JsonSchema parent = JsonSchema.from(validationContext, null, null, JsonNodeFactory.instance.objectNode(), null, false);
        JsonSchema child = JsonSchema.from(validationContext, null, null, JsonNodeFactory.instance.objectNode(), parent, false);
        assertSame(parent, child.findAncestor(), "findAncestor() on child should return the supplied parent schema");
    }

    @Test
    void from_WithParent_SuppressTrue_SubSchemaRetrievalIsSuppressedEvenWithParent() {
        JsonSchema parent = JsonSchema.from(validationContext, null, null, JsonNodeFactory.instance.objectNode(), null, false);
        JsonSchema child = JsonSchema.from(validationContext, null, null, JsonNodeFactory.instance.objectNode(), parent, true);
        assertNull(child.getSubSchema(null), "Suppress flag true must prevent sub‑schema resolution even when a parent exists");
    }

    @Test
    void from_InvalidSchemaLocationUri_ShouldThrowIllegalArgumentException() {
        // The SchemaLocation constructor validates the URI and throws IllegalArgumentException.
        // In the current library version the constructor expects an AbsoluteIri, so we
        // simply pass a malformed URI string wrapped in a try‑catch to verify the exception.
        assertThrows(IllegalArgumentException.class, () -> {
            // This will fail because the URI is not valid.
            new SchemaLocation(new AbsoluteIri("ht!tp://[invalid-uri]"));
        }, "Malformed URI must cause IllegalArgumentException");
    }

    @Test
    void from_DefaultIsRecursiveAnchor_ShouldBeFalse() {
        JsonSchema schema = JsonSchema.from(validationContext, null, null, schemaNode, null, false);
        assertFalse(schema.isRecursiveAnchor(), "A newly created schema should not be a recursive anchor by default");
    }
}
