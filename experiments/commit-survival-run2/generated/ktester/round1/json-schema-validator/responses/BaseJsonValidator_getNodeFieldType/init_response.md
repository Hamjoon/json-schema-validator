```java
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
        // By default the parent schema returns a schema node; individual tests
        // will customise the behaviour of the "type" field.
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
        // Arrange: parent schema node contains a "type" field
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