package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getNodeFieldType_Test {

    /* --------------------------------------------------------------------- */
    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode // not used by the method under test
    validatorSchemaNode;

    @Mock
    ValidationContext validationContext;

    @Mock
    ValidatorTypeCode validatorType;

    @Mock
    JsonSchema // the only collaborator that matters
    parentSchema;

    private TestValidator validator;

    /* --------------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /**
         * Expose the protected method so the test can call it.
         */
        String invokeGetNodeFieldType() {
            return getNodeFieldType();
        }

        /* ----------------------------------------------------------------- */
        /* Minimal implementations BaseJsonValidator_getNodeFieldType_Testabstract members required by the base   */
        /* class – they are not exercised by the tests.                       */
        /* ----------------------------------------------------------------- */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public String getKeyword() {
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The concrete subclass is instantiated for each test case with the same
        // mocked constructor arguments (the parent schema will be re‑stubbed per test).
        validator = new TestValidator(schemaLocation, evaluationPath, validatorSchemaNode, parentSchema, validatorType, validationContext);
    }

    private static Stream<Arguments> nonTextualTypeProvider() {
        // Array node: ["string","null"]
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode().add("string").add("null");
        // Numeric node: 42
        NumericNode numericNode = JsonNodeFactory.instance.numberNode(42);
        // Null literal node
        NullNode nullNode = JsonNodeFactory.instance.nullNode();
        return Stream.of(Arguments.of(arrayNode), Arguments.of(numericNode), Arguments.of(nullNode));
    }

    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // Arrange – parent schema node contains a "type" field
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode();
        parentNode.put("type", "object");
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);
        // Act
        String result = validator.invokeGetNodeFieldType();
        // Assert
        assertEquals("object", result, "The method should return the value BaseJsonValidator_getNodeFieldType_Testthe \"type\" field");
    }

    @Test
    void getNodeFieldType_returnsNullWhenAbsent() {
        // Arrange – parent schema node does NOT contain a "type" field
        // empty object
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode();
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);
        // Act
        String result = validator.invokeGetNodeFieldType();
        // Assert
        assertNull(result, "The method should return null when the \"type\" field is missing");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] type node = {0}")
    @MethodSource("nonTextualTypeProvider")
    void getNodeFieldType_returnsEmptyStringForNonTextualTypes(JsonNode typeNode) {
        // Arrange – parent node with a non‑textual "type" value
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode();
        parentNode.set("type", typeNode);
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);
        // Act
        String result = validator.invokeGetNodeFieldType();
        // Assert
        assertEquals("", result, "JsonNode.asText() returns an empty string for non‑textual nodes, so the method should return \"\"");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getNodeFieldType_throwsNPE_whenParentSchemaIsNull() {
        // Arrange – create a validator with a null parent schema
        TestValidator validatorWithNullParent = new TestValidator(schemaLocation, evaluationPath, validatorSchemaNode, // parentSchema = null
        null, validatorType, validationContext);
        // Act & Assert
        assertThrows(NullPointerException.class, validatorWithNullParent::invokeGetNodeFieldType, "Calling getNodeFieldType() with a null parent schema should throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getNodeFieldType_throwsNPE_whenParentSchemaNodeIsNull() {
        // Arrange – parent schema returns null for getSchemaNode()
        when(parentSchema.getSchemaNode()).thenReturn(null);
        // Act & Assert
        assertThrows(NullPointerException.class, validator::invokeGetNodeFieldType, "If getSchemaNode() returns null, invoking get(\"type\") should throw NullPointerException");
    }
}
