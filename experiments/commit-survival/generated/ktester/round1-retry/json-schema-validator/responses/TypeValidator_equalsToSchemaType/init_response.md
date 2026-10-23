```java
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

/**
 * Unit tests for {@link TypeValidator#equalsToSchemaType(JsonNode)}.
 *
 * The method is a thin delegator to {@link JsonNodeUtil#equalsToSchemaType(JsonNode, JsonType, JsonSchema, ValidationContext)}.
 * The tests verify that the delegating logic forwards the correct arguments and returns the value
 * supplied by the static utility method.
 */
@ExtendWith(MockitoExtension.class)
class TypeValidator_equalsToSchemaType_Test {

    private static final String SCHEMA_PATH = "testPath";

    @Mock
    private JsonNode schemaNode;          // the node that describes the schema (used only to obtain the JsonType)
    @Mock
    private JsonSchema parentSchema;      // parent schema passed to the validator
    @Mock
    private ValidationContext validationContext; // validation context passed to the validator
    @Mock
    private JsonNode instanceNode;        // the JSON instance that will be validated

    /**
     * Helper that creates a {@link TypeValidator} whose {@code schemaType}
     * is forced to {@link JsonType#STRING} by mocking {@link TypeFactory#getSchemaNodeType(JsonNode)}.
     */
    private TypeValidator createValidator() {
        // Mock the static TypeFactory call that the constructor uses to decide the schema type.
        try (MockedStatic<TypeFactory> typeFactory = mockStatic(TypeFactory.class)) {
            typeFactory.when(() -> TypeFactory.getSchemaNodeType(schemaNode))
                       .thenReturn(JsonType.STRING);
            // Construct the validator under the static mock.
            return new TypeValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);
        }
    }

    @Test
    void equalsToSchemaType_returnsTrueWhenJsonNodeUtilSaysTrue() {
        // Arrange
        TypeValidator validator = createValidator();

        try (MockedStatic<JsonNodeUtil> jsonNodeUtil = mockStatic(JsonNodeUtil.class)) {
            jsonNodeUtil.when(() -> JsonNodeUtil.equalsToSchemaType(
                    instanceNode, JsonType.STRING, parentSchema, validationContext))
                    .thenReturn(true);

            // Act
            boolean result = validator.equalsToSchemaType(instanceNode);

            // Assert
            assertTrue(result, "Method should return true when JsonNodeUtil reports true");
            jsonNodeUtil.verify(() -> JsonNodeUtil.equalsToSchemaType(
                    instanceNode, JsonType.STRING, parentSchema, validationContext), times(1));
        }
    }

    @Test
    void equalsToSchemaType_returnsFalseWhenJsonNodeUtilSaysFalse() {
        // Arrange
        TypeValidator validator = createValidator();

        try (MockedStatic<JsonNodeUtil> jsonNodeUtil = mockStatic(JsonNodeUtil.class)) {
            jsonNodeUtil.when(() -> JsonNodeUtil.equalsToSchemaType(
                    instanceNode, JsonType.STRING, parentSchema, validationContext))
                    .thenReturn(false);

            // Act
            boolean result = validator.equalsToSchemaType(instanceNode);

            // Assert
            assertFalse(result, "Method should return false when JsonNodeUtil reports false");
            jsonNodeUtil.verify(() -> JsonNodeUtil.equalsToSchemaType(
                    instanceNode, JsonType.STRING, parentSchema, validationContext), times(1));
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle hooks – kept empty because the test does not need global setup
    // or teardown, but they are present to satisfy the requested template.
    // -------------------------------------------------------------------------

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mocks are injected by MockitoExtension.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }
}
```