package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getNodeFieldType_Test {

    /**
     * Simple concrete implementation used only for testing the protected method.
     */
    static class TestValidator extends BaseJsonValidator {

        TestValidator(JsonSchema parentSchema) {
            // The super‑constructor arguments that are not used by getNodeFieldType()
            // are passed as {@code null}.  The signature matches the one used in the
            // original test scaffold.
            super(null, null, null, parentSchema, null, null);
        }

        // The abstract method signature in the current version of BaseJsonValidator
        // includes additional parameters.  They are not needed for the tests,
        // so we provide a no‑op implementation.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode schemaNode, JsonNodePath path) {
            return null;
        }
    }

    private static Stream<Arguments> provideBranchAndValueCases() {
        // 1. textual "object"
        // 2. textual "array"
        // 3. missing field → null
        // 4. explicit JSON null → empty string
        // 5. numeric value → "5"
        return Stream.of(Arguments.of("object", "Parent schema's schemaNode contains a \"type\" field with textual value \"object\"."), Arguments.of("array", "Parent schema's schemaNode contains a \"type\" field with textual value \"array\"."), Arguments.of(null, "Parent schema's schemaNode does NOT contain a \"type\" field; method should return null."), Arguments.of("", "Parent schema's schemaNode contains a \"type\" field whose value is JSON null (NullNode)."), Arguments.of("5", "Parent schema's schemaNode contains a numeric \"type\" value (5)."));
    }

    private static Stream<Arguments> provideAdditionalTypeVariations() {
        return Stream.of(// boolean true → "true"
        Arguments.of("true", BooleanNode.TRUE, "Parent schema's schemaNode contains a \"type\" field with boolean true."), // boolean false → "false"
        Arguments.of("false", BooleanNode.FALSE, "Parent schema's schemaNode contains a \"type\" field with boolean false."), // object node → empty string
        Arguments.of("", new ObjectNode(JsonNodeFactory.instance), "Parent schema's schemaNode contains a \"type\" field whose value is a JSON object; asText() returns empty string."), // array node → empty string
        Arguments.of("", new ArrayNode(JsonNodeFactory.instance), "Parent schema's schemaNode contains a \"type\" field whose value is a JSON array; asText() returns empty string."), // whitespace‑preserving text
        Arguments.of("  object  ", TextNode.valueOf("  object  "), "Parent schema's schemaNode contains a \"type\" field with leading/trailing whitespace; whitespace must be preserved."), // Unicode character
        Arguments.of("π", TextNode.valueOf("π"), "Parent schema's schemaNode contains a \"type\" field with a Unicode character (Greek pi)."));
    }

    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // given
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "string");
        JsonSchema parentSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaNode);
        TestValidator validator = new TestValidator(parentSchema);
        // when
        String result = validator.getNodeFieldType();
        // then
        assertEquals("string", result);
    }

    @Test
    void getNodeFieldType_returnsNullWhenNoTypeField() {
        // given – schema without a "type" field
        // empty
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        JsonSchema parentSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaNode);
        TestValidator validator = new TestValidator(parentSchema);
        // when
        String result = validator.getNodeFieldType();
        // then
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] expected=''{0}'' – {1}")
    @MethodSource("provideBranchAndValueCases")
    void getNodeFieldType_branchAndValueCoverage(String expected, String description) {
        // Arrange: build a schema node according to the expected outcome
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        if (expected != null) {
            // create the appropriate JsonNode for the "type" field
            JsonNode typeNode;
            switch(expected) {
                case "object":
                case "array":
                case "  object  ":
                case "π":
                    typeNode = TextNode.valueOf(expected);
                    break;
                case "5":
                    typeNode = IntNode.valueOf(5);
                    break;
                case "true":
                    typeNode = BooleanNode.TRUE;
                    break;
                case "false":
                    typeNode = BooleanNode.FALSE;
                    break;
                case "":
                    // empty string – use NullNode (covers the explicit null case)
                    typeNode = NullNode.getInstance();
                    break;
                default:
                    typeNode = TextNode.valueOf(expected);
            }
            schemaNode.set("type", typeNode);
        }
        // If expected is null we simply leave the "type" field absent.
        JsonSchema parentSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaNode);
        TestValidator validator = new TestValidator(parentSchema);
        // Act
        String actual = validator.getNodeFieldType();
        // Assert
        if (expected == null) {
            assertNull(actual, description);
        } else {
            assertEquals(expected, actual, description);
        }
    }

    @ParameterizedTest(name = "[{index}] expected=''{0}'' – {1}")
    @MethodSource("provideAdditionalTypeVariations")
    void getNodeFieldType_additionalVariations(String expected, JsonNode typeNode, String description) {
        // Arrange
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.set("type", typeNode);
        JsonSchema parentSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaNode);
        TestValidator validator = new TestValidator(parentSchema);
        // Act
        String actual = validator.getNodeFieldType();
        // Assert
        assertEquals(expected, actual, description);
    }

    @Test
    void getNodeFieldType_throws_NullPointerException_whenParentSchemaIsNull() {
        // Arrange: validator with a null parent schema.
        TestValidator validator = new TestValidator(null);
        // Act & Assert
        assertThrows(NullPointerException.class, validator::getNodeFieldType, "When parent schema is null, a NullPointerException should be thrown.");
    }

    @Test
    void getNodeFieldType_throws_NullPointerException_whenParentSchemaNodeIsNull() {
        // Arrange: create a JsonSchema whose schema node is null.
        JsonSchema parentSchema = new JsonSchema(null, null) {

            @Override
            public JsonNode getSchemaNode() {
                // force null
                return null;
            }
        };
        TestValidator validator = new TestValidator(parentSchema);
        // Act & Assert
        assertThrows(NullPointerException.class, validator::getNodeFieldType, "When parent schema's schemaNode is null, a NullPointerException should be thrown.");
    }

    @Test
    void getNodeFieldType_propagates_RuntimeException_fromSchemaNode() {
        // Arrange: a JsonSchema whose getSchemaNode() returns a node that throws
        // when "type" is accessed.
        JsonNode badNode = new ObjectNode(JsonNodeFactory.instance) {

            @Override
            public JsonNode get(String fieldName) {
                throw new RuntimeException("Unexpected failure while accessing node");
            }
        };
        JsonSchema parentSchema = new JsonSchema(null, null) {

            @Override
            public JsonNode getSchemaNode() {
                return badNode;
            }
        };
        TestValidator validator = new TestValidator(parentSchema);
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, validator::getNodeFieldType, "A RuntimeException thrown by the schema node should propagate.");
        assertEquals("Unexpected failure while accessing node", ex.getMessage());
    }
}
