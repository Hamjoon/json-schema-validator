```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link BaseJsonValidator#getNodeFieldType()}.
 *
 * The tests use real {@link JsonNode} instances created with Jackson's
 * {@link JsonNodeFactory} and concrete {@link JsonSchema} objects obtained
 * from {@link JsonSchemaFactory}. This removes the need for Mockito
 * (which is not available in the compilation class‑path) while still
 * exercising the same behaviour.
 */
class BaseJsonValidator_getNodeFieldType_Test {

    // ----- Test subject ------------------------------------------------------
    private TestValidator validator;

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}
     * required only to expose the protected {@code getNodeFieldType()} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for the tests of getNodeFieldType()
            return Collections.emptySet();
        }
    }

    /**
     * Helper that creates a {@link JsonSchema} from the supplied schema node.
     */
    private static JsonSchema schemaFromNode(JsonNode node) {
        return JsonSchemaFactory.getInstance(VersionFlag.V4).getSchema(node);
    }

    /**
     * Provides a stream of arguments representing different scalar JSON node types.
     * For container nodes (array, object) Jackson's {@code asText()} returns an empty string.
     */
    private static Stream<Arguments> scalarTypeNodeProvider() {
        JsonNode numericNode = new IntNode(123);          // asText() -> "123"
        JsonNode booleanNode = BooleanNode.TRUE;         // asText() -> "true"
        JsonNode arrayNode   = JsonNodeFactory.instance.arrayNode(); // asText() -> ""
        JsonNode objectNode  = JsonNodeFactory.instance.objectNode(); // asText() -> ""
        JsonNode nullNode    = NullNode.instance;        // asText() -> "null"

        return Stream.of(
                Arguments.of("Numeric type node should return its textual representation", numericNode, "123"),
                Arguments.of("Boolean type node should return its textual representation", booleanNode, "true"),
                Arguments.of("Array type node should return empty string", arrayNode, ""),
                Arguments.of("Object type node should return empty string", objectNode, ""),
                Arguments.of("Explicit null node should return the string \"null\"", nullNode, "null")
        );
    }

    // ----- Tests -------------------------------------------------------------

    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // Arrange: schema contains a "type" field with value "string"
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "string");
        JsonSchema parent = schemaFromNode(schemaNode);

        validator = new TestValidator("dummyPath", null, parent, null, null);

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

        validator = new TestValidator("dummyPath", null, parent, null, null);

        // Act
        String result = validator.getNodeFieldType();

        // Assert
        assertNull(result, "When the type field is missing, null should be returned");
    }

    @ParameterizedTest(name = "[{index}] type node returns \"{2}\"")
    @MethodSource("scalarTypeNodeProvider")
    void getNodeFieldType_returnsScalarAsText(String description, JsonNode typeNode, String expected) {
        // Arrange: schema with the given type node
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.set("type", typeNode);
        JsonSchema parent = schemaFromNode(schemaNode);

        validator = new TestValidator("dummyPath", null, parent, null, null);

        // Act
        String result = validator.getNodeFieldType();

        // Assert
        assertEquals(expected, result, description);
    }

    @Test
    void getNodeFieldType_throwsWhenParentSchemaIsNull() {
        // Arrange: validator is constructed with a null parent schema
        validator = new TestValidator("dummyPath", null, null, null, null);

        // Act & Assert
        assertThrows(NullPointerException.class,
                validator::getNodeFieldType,
                "Calling getNodeFieldType() with a null parent schema should throw NPE");
    }
}
```