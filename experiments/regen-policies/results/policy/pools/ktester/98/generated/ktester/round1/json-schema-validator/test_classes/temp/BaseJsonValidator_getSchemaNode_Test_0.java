package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.i18n.DefaultMessageSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaNode_Test {

    @Mock
    private ValidationContext mockValidationContext;

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private JsonNode mockSchemaNode;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    private TestValidator validator;

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}.
     */
    private static final class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /**
         * The {@link JsonValidator} contract requires a {@code validate} method.
         */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for the getSchemaNode test.
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create the simplest possible supporting objects.
        // fragment defaults to JSON_POINTER
        SchemaLocation schemaLocation = new SchemaLocation(null);
        // PathType is an enum inside the library
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON);
        // Instantiate the concrete validator under test.
        validator = new TestValidator(schemaLocation, evaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
    }

    /**
     * Supplies a stream of concrete {@link JsonNode} instances together with a readable description.
     */
    private static Stream<Arguments> jsonNodeProvider() {
        JsonNodeFactory f = JsonNodeFactory.instance;
        ObjectNode objectNode = f.objectNode().put("type", "string");
        ArrayNode arrayNode = f.arrayNode().add(1).add(2).add(3);
        TextNode textNode = f.textNode("hello");
        // creates an IntNode
        NumericNode numericNode = f.numberNode(42);
        BooleanNode booleanNode = f.booleanNode(true);
        NullNode nullNode = f.nullNode();
        return Stream.of(Arguments.of(objectNode, "ObjectNode representing {\"type\":\"string\"}"), Arguments.of(arrayNode, "ArrayNode representing [1,2,3]"), Arguments.of(textNode, "TextNode representing \"hello\""), Arguments.of(numericNode, "NumericNode (IntNode) representing 42"), Arguments.of(booleanNode, "BooleanNode representing true"), Arguments.of(nullNode, "NullNode representing JSON null"));
    }

    @Test
    void getSchemaNode_returnsTheNodeProvidedInConstructor() {
        JsonNode returned = validator.getSchemaNode();
        assertSame(mockSchemaNode, returned, "BaseJsonValidator#getSchemaNode should return the exact JsonNode instance supplied to the constructor");
    }

    @Test
    void getSchemaNode_isNotNullWhenConstructorArgumentIsNonNull() {
        assertNotNull(validator.getSchemaNode(), "BaseJsonValidator#getSchemaNode must not return null when a non‑null schema node was given");
    }

    @Test
    void getSchemaNode_returnsNullWhenConstructorArgumentIsNull() {
        // Build a validator whose schemaNode argument is null.
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON);
        TestValidator nullValidator = new TestValidator(schemaLocation, evaluationPath, // <-- null schema node
        null, mockParentSchema, mockValidatorType, mockValidationContext);
        assertNull(nullValidator.getSchemaNode(), "BaseJsonValidator#getSchemaNode should return null when constructed with a null schemaNode");
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("jsonNodeProvider")
    void getSchemaNode_returnsExactInstanceForVariousNodeTypes(JsonNode node, String description) {
        // Construct a validator with the supplied concrete JsonNode.
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON);
        TestValidator concreteValidator = new TestValidator(schemaLocation, evaluationPath, node, mockParentSchema, mockValidatorType, mockValidationContext);
        // The getter must return the *same* reference, not a copy.
        assertSame(node, concreteValidator.getSchemaNode(), "BaseJsonValidator#getSchemaNode should return the exact " + description + " instance");
    }

    @Test
    void getSchemaNode_reflectsMutationsOnOriginalObjectNode() {
        // Create a mutable ObjectNode and build the validator with it.
        ObjectNode original = JsonNodeFactory.instance.objectNode().put("type", "string");
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON);
        TestValidator mutableValidator = new TestValidator(schemaLocation, evaluationPath, original, mockParentSchema, mockValidatorType, mockValidationContext);
        // Mutate the original node after construction.
        original.put("maxLength", 5);
        // The returned node should reflect the mutation (same reference).
        JsonNode returned = mutableValidator.getSchemaNode();
        assertSame(original, returned, "The returned ObjectNode should be the same mutable instance supplied to the constructor");
        assertEquals(5, returned.get("maxLength").asInt(), "Mutations performed on the original ObjectNode must be visible via getSchemaNode()");
    }

    @Test
    void getSchemaNode_reflectsMutationsOnOriginalArrayNode() {
        // Create a mutable ArrayNode and build the validator with it.
        ArrayNode original = JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3);
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON);
        TestValidator mutableValidator = new TestValidator(schemaLocation, evaluationPath, original, mockParentSchema, mockValidatorType, mockValidationContext);
        // Mutate the original array after construction.
        original.add(4);
        // The returned node should reflect the mutation (same reference).
        JsonNode returned = mutableValidator.getSchemaNode();
        assertSame(original, returned, "The returned ArrayNode should be the same mutable instance supplied to the constructor");
        assertEquals(4, returned.size() - 1, "Mutations performed on the original ArrayNode must be visible via getSchemaNode()");
        assertEquals(4, returned.get(3).asInt(), "Appended element should be present in the node returned by getSchemaNode()");
    }
}
