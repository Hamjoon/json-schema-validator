package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Set;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaNode_Test {

//     @Mock
    private ValidationContext mockValidationContext;

//     @Mock
    private JsonSchema mockParentSchema;

//     @Mock
    private JsonNode mockSchemaNode;

//     @Mock
    private ValidatorTypeCode mockValidatorType;

    private TestValidator validator;

    /**
     * A concrete minimal implementation of the abstract {@link BaseJsonValidator}.
     */
    private static final class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /**
         * The {@link JsonValidator} contract requires a {@code validate} method with
         * four parameters.  For the purpose of these tests the method does nothing
         * and simply returns an empty set.
         */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation) {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The simplest possible supporting objects.
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        // All collaborators that are not needed for the getSchemaNode test are passed as null.
        validator = new TestValidator(schemaLocation, evaluationPath, // schemaNode – will be set per test
        null, // parentSchema
        null, // validatorType
        null, // validationContext
        null);
    }

    /**
     * Supplies a stream of concrete {@link JsonNode} instances together with a readable description.
     */
    private static Stream<Arguments> jsonNodeProvider() {
        JsonNodeFactory f = JsonNodeFactory.instance;
        ObjectNode objectNode = f.objectNode().put("type", "string");
        ArrayNode arrayNode = f.arrayNode().add(1).add(2).add(3);
        TextNode textNode = f.textNode("hello");
        NumericNode numericNode = f.numberNode(42);
        BooleanNode booleanNode = f.booleanNode(true);
        NullNode nullNode = f.nullNode();
        return Stream.of(Arguments.of(objectNode, "ObjectNode representing {\"type\":\"string\"}"), Arguments.of(arrayNode, "ArrayNode representing [1,2,3]"), Arguments.of(textNode, "TextNode representing \"hello\""), Arguments.of(numericNode, "NumericNode (IntNode) representing 42"), Arguments.of(booleanNode, "BooleanNode representing true"), Arguments.of(nullNode, "NullNode representing JSON null"));
    }

    @Test
    void getSchemaNode_returnsTheNodeProvidedInConstructor() {
        JsonNode customNode = JsonNodeFactory.instance.objectNode().put("custom", "value");
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        TestValidator customValidator = new TestValidator(schemaLocation, evaluationPath, customNode, null, null, null);
        JsonNode returned = customValidator.getSchemaNode();
        Assertions.assertSame(customNode, returned, "BaseJsonValidator#getSchemaNode should return the exact JsonNode instance supplied to the constructor");
    }

    @Test
    void getSchemaNode_isNotNullWhenConstructorArgumentIsNonNull() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("a", 1);
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        TestValidator v = new TestValidator(schemaLocation, evaluationPath, node, null, null, null);
        Assertions.assertNotNull(v.getSchemaNode(), "BaseJsonValidator#getSchemaNode must not return null when a non‑null schema node was given");
    }

    @Test
    void getSchemaNode_returnsNullWhenConstructorArgumentIsNull() {
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        TestValidator nullValidator = new TestValidator(schemaLocation, evaluationPath, null, null, null, null);
        Assertions.assertNull(nullValidator.getSchemaNode(), "BaseJsonValidator#getSchemaNode should return null when constructed with a null schemaNode");
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("jsonNodeProvider")
    void getSchemaNode_returnsExactInstanceForVariousNodeTypes(JsonNode node, String description) {
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        TestValidator concreteValidator = new TestValidator(schemaLocation, evaluationPath, node, null, null, null);
        Assertions.assertSame(node, concreteValidator.getSchemaNode(), "BaseJsonValidator#getSchemaNode should return the exact " + description + " instance");
    }

    @Test
    void getSchemaNode_reflectsMutationsOnOriginalObjectNode() {
        ObjectNode original = JsonNodeFactory.instance.objectNode().put("type", "string");
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        TestValidator mutableValidator = new TestValidator(schemaLocation, evaluationPath, original, null, null, null);
        // Mutate after construction.
        original.put("maxLength", 5);
        JsonNode returned = mutableValidator.getSchemaNode();
        Assertions.assertSame(original, returned, "The returned ObjectNode should be the same mutable instance supplied to the constructor");
        Assertions.assertEquals(5, returned.get("maxLength").asInt(), "Mutations performed on the original ObjectNode must be visible via getSchemaNode()");
    }

    @Test
    void getSchemaNode_reflectsMutationsOnOriginalArrayNode() {
        ArrayNode original = JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3);
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        TestValidator mutableValidator = new TestValidator(schemaLocation, evaluationPath, original, null, null, null);
        // Mutate after construction.
        original.add(4);
        JsonNode returned = mutableValidator.getSchemaNode();
        Assertions.assertSame(original, returned, "The returned ArrayNode should be the same mutable instance supplied to the constructor");
        Assertions.assertEquals(4, returned.size() - 1, "Mutations performed on the original ArrayNode must be visible via getSchemaNode()");
        Assertions.assertEquals(4, returned.get(3).asInt(), "Appended element should be present in the node returned by getSchemaNode()");
    }
}