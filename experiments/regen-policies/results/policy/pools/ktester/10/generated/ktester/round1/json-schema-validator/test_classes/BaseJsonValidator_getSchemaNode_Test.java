package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaNode_Test {

    // -------------------------------------------------------------------------
    @Mock
    private SchemaLocation mockSchemaLocation;

    @Mock
    private JsonNodePath mockEvaluationPath;

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    @Mock
    private ValidationContext mockValidationContext;

    // -------------------------------------------------------------------------
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        // The abstract {@code validate} method from {@link JsonValidator}
        // is implemented with a no‑op that returns an empty set.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        // The keyword is required by {@link ValidationMessageHandler};
        // a dummy value is sufficient for the test.
        @Override
        public String getKeyword() {
            return "test";
        }
    }

    /**
     * Provides a stream of different {@link JsonNode} implementations together with
     * a short description used for the display name of the parameterized test.
     */
    private static Stream<Arguments> nodeVariantsProvider() {
        JsonNodeFactory f = JsonNodeFactory.instance;
        // ObjectNode with several fields
        ObjectNode objectNode = f.objectNode().put("type", "integer").put("minimum", 0);
        // ArrayNode representing an "anyOf" style schema
        ArrayNode arrayNode = f.arrayNode().add(f.objectNode().put("type", "string")).add(f.objectNode().put("type", "number"));
        // Primitive nodes
        BooleanNode booleanNode = f.booleanNode(true);
        NumericNode numericNode = f.numberNode(42);
        TextNode textNode = f.textNode("example");
        return Stream.of(Arguments.of(objectNode, "ObjectNode (complex object)"), Arguments.of(arrayNode, "ArrayNode (list)"), Arguments.of(booleanNode, "BooleanNode (true)"), Arguments.of(numericNode, "NumericNode (42)"), Arguments.of(textNode, "TextNode (\"example\")"));
    }

    @Test
    void getSchemaNode_returnsProvidedNode() {
        // Arrange: a non‑null JsonNode that will be passed to the constructor
        JsonNode expected = JsonNodeFactory.instance.objectNode().put("type", "string");
        BaseJsonValidator validator = new TestValidator(mockSchemaLocation, mockEvaluationPath, expected, mockParentSchema, mockValidatorType, mockValidationContext);
        // Act
        JsonNode actual = validator.getSchemaNode();
        // Assert
        assertSame(expected, actual, "getSchemaNode() should return the exact instance supplied to the constructor");
    }

    @Test
    void getSchemaNode_returnsNullWhenSchemaNodeIsNull() {
        // Arrange: pass a null schemaNode
        BaseJsonValidator validator = new TestValidator(mockSchemaLocation, mockEvaluationPath, null, mockParentSchema, mockValidatorType, mockValidationContext);
        // Act & Assert
        assertNull(validator.getSchemaNode(), "getSchemaNode() should return null when the validator was constructed with a null schemaNode");
    }

    @ParameterizedTest(name = "[{index}] getSchemaNode returns same instance for {1}")
    @MethodSource("nodeVariantsProvider")
    void getSchemaNode_returnsSameInstanceForVariousNodeTypes(JsonNode suppliedNode, String description) {
        // Arrange
        BaseJsonValidator validator = new TestValidator(mockSchemaLocation, mockEvaluationPath, suppliedNode, mockParentSchema, mockValidatorType, mockValidationContext);
        // Act
        JsonNode result = validator.getSchemaNode();
        // Assert: identity equality and runtime type preservation
        assertSame(suppliedNode, result, "The returned node must be the exact same instance that was supplied (" + description + ")");
        assertEquals(suppliedNode.getClass(), result.getClass(), "Runtime type must be preserved for " + description);
    }

    @Test
    void getSchemaNode_reflectsExternalMutations() {
        // Arrange: create a mutable ObjectNode and pass it to the validator
        ObjectNode mutableNode = JsonNodeFactory.instance.objectNode().put("initial", "value");
        BaseJsonValidator validator = new TestValidator(mockSchemaLocation, mockEvaluationPath, mutableNode, mockParentSchema, mockValidatorType, mockValidationContext);
        // Mutate the node *after* construction
        mutableNode.put("addedLater", 123);
        // Act
        JsonNode result = validator.getSchemaNode();
        // Assert: the mutation is visible, proving no defensive copy was made
        assertTrue(result.has("addedLater"), "The returned JsonNode should reflect mutations performed on the original instance");
        assertEquals(123, result.get("addedLater").asInt(), "The mutated field must retain the value assigned after construction");
    }
}
