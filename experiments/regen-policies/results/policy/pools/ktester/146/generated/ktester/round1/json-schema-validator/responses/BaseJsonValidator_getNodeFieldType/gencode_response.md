```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the protected {@code getNodeFieldType()} method of {@link BaseJsonValidator}.
 *
 * <p>The method simply looks for a {@code "type"} property in the parent schema node.
 * We therefore create a concrete subclass {@code TestValidator} that exposes the method for testing.
 * All collaborators are mocked with Mockito.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getNodeFieldType_Test {

    /* --------------------------------------------------------------------- */
    /* Mocked collaborators – injected by MockitoExtension                    */
    /* --------------------------------------------------------------------- */
    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode validatorSchemaNode;   // not used by the method under test

    @Mock
    ValidationContext validationContext;

    @Mock
    ValidatorTypeCode validatorType;

    @Mock
    JsonSchema parentSchema;        // the only collaborator that matters for most tests

    private TestValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh validator instance for each test.
        validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                validatorSchemaNode,
                parentSchema,
                validatorType,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------------- */
    /* Positive case – the parent schema contains a textual "type" field   */
    /* --------------------------------------------------------------------- */
    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // Arrange – parent schema node contains a "type" field with value "object"
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode();
        parentNode.put("type", "object");
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);

        // Act
        String result = validator.invokeGetNodeFieldType();

        // Assert
        assertEquals("object", result,
                "The method should return the text value of the \"type\" property");
    }

    /* --------------------------------------------------------------------- */
    /* Negative case – the parent schema does NOT contain a "type" field   */
    /* --------------------------------------------------------------------- */
    @Test
    void getNodeFieldType_returnsNullWhenAbsent() {
        // Arrange – empty parent node (no "type")
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode();
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);

        // Act
        String result = validator.invokeGetNodeFieldType();

        // Assert
        assertNull(result, "The method should return null when the \"type\" field is missing");
    }

    /* --------------------------------------------------------------------- */
    /* Parameterized tests for non‑textual \"type\" values (array, number, null) */
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
        assertEquals("", result,
                "JsonNode.asText() returns an empty string for non‑textual nodes, so the method should return \"\"");
    }

    private static Stream<Arguments> nonTextualTypeProvider() {
        // Array node: ["string","null"]
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode()
                .add("string")
                .add("null");
        // Numeric node: 42
        NumericNode numericNode = JsonNodeFactory.instance.numberNode(42);
        // Null literal node
        NullNode nullNode = JsonNodeFactory.instance.nullNode();

        return Stream.of(
                Arguments.of(arrayNode),
                Arguments.of(numericNode),
                Arguments.of(nullNode)
        );
    }

    /* --------------------------------------------------------------------- */
    /* Failure mode – getParentSchema() returns null                        */
    /* --------------------------------------------------------------------- */
    @Test
    void getNodeFieldType_throwsNPE_whenParentSchemaIsNull() {
        // Arrange – create a validator with a null parent schema
        TestValidator validatorWithNullParent = new TestValidator(
                schemaLocation,
                evaluationPath,
                validatorSchemaNode,
                null,               // parentSchema = null
                validatorType,
                validationContext);

        // Act & Assert
        assertThrows(NullPointerException.class,
                validatorWithNullParent::invokeGetNodeFieldType,
                "Calling getNodeFieldType() with a null parent schema should throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* Failure mode – parent schema exists but its schema node is null     */
    /* --------------------------------------------------------------------- */
    @Test
    void getNodeFieldType_throwsNPE_whenParentSchemaNodeIsNull() {
        // Arrange – parent schema returns null for getSchemaNode()
        when(parentSchema.getSchemaNode()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class,
                validator::invokeGetNodeFieldType,
                "If getSchemaNode() returns null, invoking get(\"type\") should throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* Helper concrete subclass – exposes the protected method for testing */
    /* --------------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /** Expose the protected method so the test can call it. */
        String invokeGetNodeFieldType() {
            return getNodeFieldType();
        }

        /* ----------------------------------------------------------------- */
        /* Minimal implementations of abstract members – not exercised here */
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
}
```