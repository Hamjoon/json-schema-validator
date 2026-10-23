package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.utils.JsonNodeUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TypeValidator_equalsToSchemaType_Test {

    private static final String SCHEMA_PATH = "testPath";

    @Mock
    private JsonNode // the node that describes the schema (used only to obtain the JsonType)
    schemaNode;

    @Mock
    private JsonSchema // parent schema passed to the validator
    parentSchema;

    @Mock
    private ValidationContext // validation context passed to the validator
    validationContext;

    @Mock
    private JsonNode // the JSON instance that will be validated
    instanceNode;

    /**
     * Helper that creates a {@link TypeValidator} whose {@code schemaType}
     * is forced to {@link JsonType#STRING} by mocking {@link TypeFactory#getSchemaNodeType(JsonNode)}.
     */
    private TypeValidator createValidator() {
        // Mock the static TypeFactory call that the constructor uses to decide the schema type.
        try (MockedStatic<TypeFactory> typeFactory = mockStatic(TypeFactory.class)) {
            typeFactory.when(() -> TypeFactory.getSchemaNodeType(schemaNode)).thenReturn(JsonType.STRING);
            // Construct the validator under the static mock.
            return new TypeValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);
        }
    }

    static Stream<Arguments> equalsToSchemaTypeProvider() {
        JsonNode intNode = new IntNode(42);
        JsonNode stringNode = new TextNode("hello");
        JsonNode nullNode = NullNode.getInstance();
        JsonNode floatNode = new DoubleNode(3.14);
        JsonNode floatNode2 = new DoubleNode(2.718);
        JsonNode boolTrue = BooleanNode.TRUE;
        JsonNode boolFalse = BooleanNode.FALSE;
        JsonNode emptyArray = JsonNodeFactory.instance.arrayNode();
        JsonNode mixedArray = JsonNodeFactory.instance.arrayNode().add(1).add("two").add(true);
        JsonNode intArray = JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3);
        JsonNode objectNode = JsonNodeFactory.instance.objectNode().put("a", 1).put("b", "x");
        JsonNode simpleObject = JsonNodeFactory.instance.objectNode().put("key", "value");
        ValidationContext defaultVC = mock(ValidationContext.class);
        ValidationContext looseVC = mock(ValidationContext.class);
        JsonSchema parentStringSchema = mock(JsonSchema.class);
        JsonSchema parentStringFormatDate = mock(JsonSchema.class);
        JsonSchema parentInvalidType = mock(JsonSchema.class);
        JsonSchema parentNoType = mock(JsonSchema.class);
        ???;
    }

    @Test
    void equalsToSchemaType_returnsTrueWhenJsonNodeUtilSaysTrue() {
        // Arrange
        TypeValidator validator = createValidator();
        try (MockedStatic<JsonNodeUtil> jsonNodeUtil = mockStatic(JsonNodeUtil.class)) {
            jsonNodeUtil.when(() -> JsonNodeUtil.equalsToSchemaType(instanceNode, JsonType.STRING, parentSchema, validationContext)).thenReturn(true);
            // Act
            boolean result = validator.equalsToSchemaType(instanceNode);
            // Assert
            assertTrue(result, "Method should return true when JsonNodeUtil reports true");
            jsonNodeUtil.verify(() -> JsonNodeUtil.equalsToSchemaType(instanceNode, JsonType.STRING, parentSchema, validationContext), times(1));
        }
    }

    @Test
    void equalsToSchemaType_returnsFalseWhenJsonNodeUtilSaysFalse() {
        // Arrange
        TypeValidator validator = createValidator();
        try (MockedStatic<JsonNodeUtil> jsonNodeUtil = mockStatic(JsonNodeUtil.class)) {
            jsonNodeUtil.when(() -> JsonNodeUtil.equalsToSchemaType(instanceNode, JsonType.STRING, parentSchema, validationContext)).thenReturn(false);
            // Act
            boolean result = validator.equalsToSchemaType(instanceNode);
            // Assert
            assertFalse(result, "Method should return false when JsonNodeUtil reports false");
            jsonNodeUtil.verify(() -> JsonNodeUtil.equalsToSchemaType(instanceNode, JsonType.STRING, parentSchema, validationContext), times(1));
        }
    }

    @ParameterizedTest(name = "{5}")
    @MethodSource("equalsToSchemaTypeProvider")
    void testEqualsToSchemaType(JsonNode node, JsonType schemaType, JsonSchema parent, ValidationContext vc, Object expected, String description) {
        TypeValidator validator = createValidator(schemaType, parent, vc);
        try (MockedStatic<JsonNodeUtil> jsonNodeUtil = mockStatic(JsonNodeUtil.class)) {
            if (expected instanceof Boolean) {
                boolean exp = (Boolean) expected;
                jsonNodeUtil.when(() -> JsonNodeUtil.equalsToSchemaType(node, schemaType, parent, vc)).thenReturn(exp);
                boolean result = validator.equalsToSchemaType(node);
                assertEquals(exp, result, description);
                jsonNodeUtil.verify(() -> JsonNodeUtil.equalsToSchemaType(node, schemaType, parent, vc), times(1));
            } else if (expected instanceof Class) {
                @SuppressWarnings("unchecked")
                Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
                jsonNodeUtil.when(() -> JsonNodeUtil.equalsToSchemaType(node, schemaType, parent, vc)).thenThrow(exClass);
                assertThrows(exClass, () -> validator.equalsToSchemaType(node), description);
                jsonNodeUtil.verify(() -> JsonNodeUtil.equalsToSchemaType(node, schemaType, parent, vc), times(1));
            } else {
                fail("Test data error – expected value must be Boolean or Exception class");
            }
        }
    }
}
