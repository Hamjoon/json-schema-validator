```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the protected {@code getNodeFieldType()} method of {@link BaseJsonValidator}.
 *
 * The method simply looks for a {@code "type"} property in the parent schema node.
 * We therefore create a concrete subclass of {@code BaseJsonValidator} that
 * exposes the method for testing and use Mockito to stub the required collaborators.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getNodeFieldType_Test {

    /* --------------------------------------------------------------------- */
    /* Mocked collaborators – they are injected by MockitoExtension          */
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
    JsonSchema parentSchema;        // the only collaborator that matters

    private TestValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // The concrete subclass is instantiated for each test case with the same
        // mocked constructor arguments (the parent schema will be re‑stubbed per test).
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

    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // Arrange – parent schema node contains a "type" field
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode();
        parentNode.put("type", "object");
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);

        // Act
        String result = validator.invokeGetNodeFieldType();

        // Assert
        assertEquals("object", result, "The method should return the value of the \"type\" field");
    }

    @Test
    void getNodeFieldType_returnsNullWhenAbsent() {
        // Arrange – parent schema node does NOT contain a "type" field
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode(); // empty object
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);

        // Act
        String result = validator.invokeGetNodeFieldType();

        // Assert
        assertNull(result, "The method should return null when the \"type\" field is missing");
    }

    /* --------------------------------------------------------------------- */
    /* Helper concrete subclass – exposes the protected method for testing   */
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
        /* Minimal implementations of abstract members required by the base   */
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
}
```