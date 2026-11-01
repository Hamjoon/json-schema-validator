```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BaseJsonValidator#getSchemaNode()}.
 *
 * The {@code BaseJsonValidator} class is abstract, therefore a minimal concrete
 * subclass is created inside the test suite. All collaborators are mocked
 * because the method under test simply returns the {@code schemaNode}
 * supplied via the constructor.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaNode_Test {

    // -------------------------------------------------------------------------
    // Mocks for the constructor parameters that are not needed for the test
    // -------------------------------------------------------------------------
    @Mock
    private SchemaLocation mockSchemaLocation;

    @Mock
    private JsonNodePath mockEvaluationPath;

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    @Mock
    private ValidationContext mockValidationContext;

    // -------------------------------------------------------------------------
    // Test lifecycle (no special global resources are required)
    // -------------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global setup required
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already initialises the @Mock fields
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    // -------------------------------------------------------------------------
    // Concrete subclass used only for exercising the protected constructor
    // -------------------------------------------------------------------------
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        // The abstract {@code validate} method from {@link JsonValidator}
        // is implemented with a no‑op that returns an empty set.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        // The keyword is required by {@link ValidationMessageHandler};
        // a dummy value is sufficient for the test.
        @Override
        public String getKeyword() {
            return "test";
        }
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void getSchemaNode_returnsProvidedNode() {
        // Arrange: a non‑null JsonNode that will be passed to the constructor
        JsonNode expected = JsonNodeFactory.instance.objectNode().put("type", "string");

        BaseJsonValidator validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                expected,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext);

        // Act
        JsonNode actual = validator.getSchemaNode();

        // Assert
        assertSame(expected, actual, "getSchemaNode() should return the exact instance supplied to the constructor");
    }

    @Test
    void getSchemaNode_returnsNullWhenSchemaNodeIsNull() {
        // Arrange: pass a null schemaNode
        BaseJsonValidator validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                null,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext);

        // Act & Assert
        assertNull(validator.getSchemaNode(),
                "getSchemaNode() should return null when the validator was constructed with a null schemaNode");
    }
}
```