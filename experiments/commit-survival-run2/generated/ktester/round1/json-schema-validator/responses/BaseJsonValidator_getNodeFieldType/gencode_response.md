```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
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
 * Unit tests for {@link BaseJsonValidator#getNodeFieldType()}.
 *
 * <p>The abstract {@code BaseJsonValidator} is exercised through a minimal concrete
 * subclass that only implements the {@code validate} method. All interactions with
 * the JSON tree are mocked using Mockito, allowing us to focus on the behaviour of
 * {@code getNodeFieldType()} under different scenarios.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getNodeFieldType_Test {

    // ----- Mocks -------------------------------------------------------------
    @Mock
    JsonSchema parentSchema;

    @Mock
    JsonNode parentSchemaNode;

    @Mock
    JsonNode typeNode;

    // ----- Test subject ------------------------------------------------------
    private TestValidator validator;

    // ----- Helper concrete class --------------------------------------------
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

    // ----- Lifecycle ---------------------------------------------------------
    @BeforeEach
    void setupBeforeEach() {
        // By default the parent schema returns a non‑null schema node; individual
        // tests will customise the behaviour of the "type" field or the schema node
        // itself.
        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        reset(parentSchema, parentSchemaNode, typeNode);
    }

    // ----- Tests -------------------------------------------------------------
    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // Arrange: parent schema node contains a "type" field with textual value "string"
        when(parentSchemaNode.get("type")).thenReturn(typeNode);
        when(typeNode.asText()).thenReturn("string");

        validator = new TestValidator("dummyPath", null, parentSchema, null, null);

        // Act
        String result = validator.getNodeFieldType();

        // Assert
        assertEquals("string", result, "The type field value should be returned");
    }

    @Test
    void getNodeFieldType_returnsNullWhenAbsent() {
        // Arrange: parent schema node does NOT contain a "type" field
        when(parentSchemaNode.get("type")).thenReturn(null);

        validator = new TestValidator("dummyPath", null, parentSchema, null, null);

        // Act
        String result = validator.getNodeFieldType();

        // Assert
        assertNull(result, "When the type field is missing, null should be returned");
    }

    @ParameterizedTest(name = "[{index}] type node returns \"{1}\"")
    @MethodSource("scalarTypeNodeProvider")
    void getNodeFieldType_returnsScalarAsText(String description, JsonNode mockNode, String expected) {
        // Arrange
        when(parentSchemaNode.get("type")).thenReturn(mockNode);
        // The mockNode is already configured by the provider to return the expected asText()
        validator = new TestValidator("dummyPath", null, parentSchema, null, null);

        // Act
        String result = validator.getNodeFieldType();

        // Assert
        assertEquals(expected, result, description);
    }

    /**
     * Provides a stream of arguments representing different scalar JSON node types.
     * For container nodes (array, object) Jackson's {@code asText()} returns an empty string.
     */
    private static Stream<Arguments> scalarTypeNodeProvider() {
        JsonNode numericNode = mock(JsonNode.class);
        when(numericNode.asText()).thenReturn("123");

        JsonNode booleanNode = mock(JsonNode.class);
        when(booleanNode.asText()).thenReturn("true");

        JsonNode arrayNode = mock(JsonNode.class);
        when(arrayNode.asText()).thenReturn(""); // non‑textual container

        JsonNode objectNode = mock(JsonNode.class);
        when(objectNode.asText()).thenReturn(""); // non‑textual container

        JsonNode nullNode = mock(JsonNode.class);
        when(nullNode.asText()).thenReturn("null"); // Jackson's NullNode

        return Stream.of(
                Arguments.of("Numeric type node should return its textual representation",
                             numericNode, "123"),
                Arguments.of("Boolean type node should return its textual representation",
                             booleanNode, "true"),
                Arguments.of("Array type node should return empty string",
                             arrayNode, ""),
                Arguments.of("Object type node should return empty string",
                             objectNode, ""),
                Arguments.of("Explicit null node should return the string \"null\"",
                             nullNode, "null")
        );
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

    @Test
    void getNodeFieldType_throwsWhenParentSchemaNodeIsNull() {
        // Arrange: parent schema returns null for getSchemaNode()
        when(parentSchema.getSchemaNode()).thenReturn(null);
        validator = new TestValidator("dummyPath", null, parentSchema, null, null);

        // Act & Assert
        assertThrows(NullPointerException.class,
                     validator::getNodeFieldType,
                     "Calling getNodeFieldType() when the parent schema node is null should throw NPE");
    }

    // ----- Optional global lifecycle hooks (empty) -------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global setup required for these unit tests
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required for these unit tests
    }
}
```