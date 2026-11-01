```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaNode_Test {

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidatorTypeCode validatorType;

    @Mock
    ValidationContext validationContext;

    private TestValidator validator;

    /** Concrete subclass used only for testing the protected constructor and the
     *  {@code getSchemaNode()} method. */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for this test
            return Collections.emptySet();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialize
    }

    @BeforeEach
    void setupBeforeEach() {
        // ValidationContext is used inside the super‑constructor; it must return a non‑null config
        when(validationContext.getConfig()).thenReturn(new SchemaValidatorsConfig());

        validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test contamination
        Mockito.reset(schemaLocation, evaluationPath, schemaNode, parentSchema,
                validatorType, validationContext);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }

    @Test
    @DisplayName("getSchemaNode() should return the JsonNode supplied to the constructor")
    void testGetSchemaNodeReturnsSuppliedNode() {
        JsonNode result = validator.getSchemaNode();
        assertSame(schemaNode, result, "The returned schema node must be the same instance supplied to the constructor");
    }
}
```