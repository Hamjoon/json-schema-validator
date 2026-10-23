```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.networknt.schema.utils.JsonNodeUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TypeValidator#equalsToSchemaType(JsonNode)}.
 *
 * <p>The method is a thin delegator to
 * {@link JsonNodeUtil#equalsToSchemaType(JsonNode, JsonType, JsonSchema, ValidationContext)}.
 * Each test case configures a static mock of {@code JsonNodeUtil} so that the
 * {@code TypeValidator} can be exercised in isolation.  The tests verify that
 * the correct arguments are forwarded and that the returned value (or exception)
 * is propagated unchanged.</p>
 */
@ExtendWith(MockitoExtension.class)
class TypeValidator_equalsToSchemaType_Test {

    private static final String SCHEMA_PATH = "testPath";

    // -------------------------------------------------------------------------
    // Mocks required for constructing a TypeValidator instance.
    // -------------------------------------------------------------------------
    @Mock
    private JsonNode schemaNode;               // supplied to the constructor
    @Mock
    private JsonSchema parentSchema;           // can be replaced per‑test
    @Mock
    private ValidationContext validationContext; // can be replaced per‑test
    @Mock
    private JsonNode instanceNode;             // generic placeholder, not used directly

    /**
     * Helper that creates a {@link TypeValidator} with a forced {@code schemaType}
     * (the value returned by {@link TypeFactory#getSchemaNodeType(JsonNode)}).
     *
     * @param schemaType          the type that the validator should hold (may be {@code null})
     * @param parentSchema        the parent schema to be stored in the validator (may be {@code null})
     * @param validationContext   the validation context to be stored in the validator (may be {@code null})
     * @return a fully constructed {@link TypeValidator}
     */
    private TypeValidator createValidator(JsonType schemaType,
                                          JsonSchema parentSchema,
                                          ValidationContext validationContext) {
        // Mock the static TypeFactory call that the constructor uses.
        try (MockedStatic<TypeFactory> typeFactory = mockStatic(TypeFactory.class)) {
            typeFactory.when(() -> TypeFactory.getSchemaNodeType(any(JsonNode.class)))
                       .thenReturn(schemaType);
            // Build the validator under the static mock.
            return new TypeValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);
        }
    }

    // -------------------------------------------------------------------------
    // Parameterised test covering all scenarios described in the JSON spec.
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "{5}")
    @MethodSource("equalsToSchemaTypeProvider")
    void testEqualsToSchemaType(JsonNode node,
                               JsonType schemaType,
                               JsonSchema parent,
                               ValidationContext vc,
                               Object expected,               // Boolean or Class<? extends Throwable>
                               String description) {

        TypeValidator validator = createValidator(schemaType, parent, vc);

        try (MockedStatic<JsonNodeUtil> jsonNodeUtil = mockStatic(JsonNodeUtil.class)) {
            if (expected instanceof Boolean) {
                boolean exp = (Boolean) expected;
                jsonNodeUtil.when(() -> JsonNodeUtil.equalsToSchemaType(node, schemaType, parent, vc))
                           .thenReturn(exp);

                boolean result = validator.equalsToSchemaType(node);
                assertEquals(exp, result, description);
                jsonNodeUtil.verify(() ->
                        JsonNodeUtil.equalsToSchemaType(node, schemaType, parent, vc), times(1));
            } else if (expected instanceof Class) {
                @SuppressWarnings("unchecked")
                Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
                jsonNodeUtil.when(() -> JsonNodeUtil.equalsToSchemaType(node, schemaType, parent, vc))
                           .thenThrow(exClass);

                assertThrows(exClass, () -> validator.equalsToSchemaType(node), description);
                jsonNodeUtil.verify(() ->
                        JsonNodeUtil.equalsToSchemaType(node, schemaType, parent, vc), times(1));
            } else {
                fail("Test data error – expected value must be Boolean or Exception class");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Data provider – each Arguments entry mirrors one JSON test case.
    // -------------------------------------------------------------------------

    static Stream<Arguments> equalsToSchemaTypeProvider() {
        // JSON node instances
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

        // Validation contexts (plain mocks – behaviour is not needed)
        ValidationContext defaultVC = mock(ValidationContext.class);
        ValidationContext looseVC = mock(ValidationContext.class);
        // parent schemas (plain mocks – only identity matters)
        JsonSchema parentStringSchema = mock(JsonSchema.class);
        JsonSchema parentStringFormatDate = mock(JsonSchema.class);
        JsonSchema parentInvalidType = mock(JsonSchema.class);
        JsonSchema parentNoType = mock(JsonSchema.class);

        return Stream.of(
                // 1. Matching schema type
                Arguments.of(intNode, JsonType.INTEGER, null, defaultVC, true,
                        "Matching schema type – integer node matches INTEGER"),
                // 2. Mismatching schema type
                Arguments.of(stringNode, JsonType.INTEGER, null, defaultVC, false