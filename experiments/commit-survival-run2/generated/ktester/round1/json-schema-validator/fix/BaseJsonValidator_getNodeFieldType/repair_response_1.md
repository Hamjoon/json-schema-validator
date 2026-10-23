```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BaseJsonValidator#getNodeFieldType()}.
 *
 * <p>The tests use a minimal concrete subclass of {@link BaseJsonValidator}
 * that implements the required abstract methods. No external mocking framework
 * is needed – real {@link JsonSchema} instances are created via
 * {@link JsonSchemaFactory}.</p>
 */
class BaseJsonValidator_getNodeFieldType_Test {

    /** Minimal concrete implementation of the abstract {@link BaseJsonValidator}
     * required only to expose the protected {@code getNodeFieldType()} method. */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        // The current version of {@link BaseJsonValidator} declares this method
        // as abstract. For the purpose of these tests we simply return an empty
        // set – the validation logic itself is irrelevant.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            return Collections.emptySet();
        }
    }

    /**
     * Provides a stream of arguments representing different scalar JSON node types.
     * For container nodes (array, object) Jackson's {@code asText()} returns an empty string.
     */
    private static Stream<Arguments> scalarTypeNodeProvider() {
        JsonNode numericNode = new IntNode(123);                 // asText() -> "123"
        JsonNode booleanNode = BooleanNode.TRUE;                // asText() -> "true"
        JsonNode arrayNode   = JsonNodeFactory.instance.arrayNode(); // asText() -> ""
        JsonNode objectNode  = JsonNodeFactory.instance.objectNode(); // asText() -> ""
        JsonNode nullNode    = NullNode.instance;               // asText() -> "null"

        return Stream.of(
                Arguments.of("Numeric type node should return its textual representation", numericNode, "123"),
                Arguments.of("Boolean type node should return its textual representation", booleanNode, "true"),
                Arguments.of("Array type node should return empty string", arrayNode, ""),
                Arguments.of("Object type node should return empty string", objectNode, ""),
                Arguments.of("Explicit null node should return the string \"null\"", nullNode, "null")
        );
    }

    /** Helper that creates a {@link JsonSchema} from the supplied schema node. */
    private static JsonSchema schemaFromNode(JsonNode node) {
        return JsonSchemaFactory.getInstance(VersionFlag.V4).getSchema(node);
    }

    // --------------------------------------------------------------------- Tests

    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // Arrange: schema contains a "type" field with value "string"
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "string");
        JsonSchema parent = schemaFromNode(schemaNode);
        TestValidator validator = new TestValidator("dummyPath", null, parent, null, null);

        // Act
        String result = validator.getNodeFieldType();

        // Assert
        assertEquals("string", result, "The type field value should be returned");
    }

    @Test
    void getNodeFieldType_returnsNullWhenAbsent() {
        // Arrange: schema does NOT contain a "type" field
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode(); // empty object
        JsonSchema parent = schemaFromNode(schemaNode);
        TestValidator validator = new TestValidator("dummyPath", null, parent, null, null);

        // Act
        String result = validator.getNodeFieldType();

        // Assert
        assertNull(result, "When the type field is missing, null should be returned");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("scalarTypeNodeProvider")
    void getNodeFieldType_returnsScalarAsText(String description, JsonNode typeNode, String expected) {
        // Arrange: schema with the given type node
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.set("type", typeNode);
        JsonSchema parent = schemaFromNode(schemaNode);
        TestValidator validator = new TestValidator("dummyPath", null, parent, null, null);

        // Act
        String result = validator.getNodeFieldType();

        // Assert
        assertEquals(expected, result, description);
    }

    @Test
    void getNodeFieldType_throwsWhenParentSchemaIsNull() {
        // Arrange: validator is constructed with a null parent schema
        TestValidator validator = new TestValidator("dummyPath", null, null, null, null);

        // Act & Assert
        assertThrows(NullPointerException.class,
                validator::getNodeFieldType,
                "Calling getNodeFieldType() with a null parent schema should throw NPE");
    }

    @Test
    void getNodeFieldType_throwsWhenParentSchemaNodeIsNull() {
        // Arrange: create a JsonSchema subclass that returns null for getSchemaNode()
        JsonSchema parent = new JsonSchema(null, null, null) {
            @Override
            public JsonNode getSchemaNode() {
                return null;
            }
        };
        TestValidator validator = new TestValidator("dummyPath", null, parent, null, null);

        // Act & Assert
        assertThrows(NullPointerException.class,
                validator::getNodeFieldType,
                "Calling getNodeFieldType() when the parent schema node is null should throw NPE");
    }
}
```