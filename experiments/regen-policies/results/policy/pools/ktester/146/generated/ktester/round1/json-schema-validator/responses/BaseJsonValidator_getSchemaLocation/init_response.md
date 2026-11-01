```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaLocation_Test {

    /** Simple concrete implementation of the abstract {@link BaseJsonValidator}
     *  used only for exercising {@code getSchemaLocation()}.
     */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                  validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for this test
            return Collections.emptySet();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test setup required
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
    void getSchemaLocation_returnsConstructorProvidedInstance() {
        // Arrange – create mocks for all constructor dependencies
        SchemaLocation mockLocation = mock(SchemaLocation.class);
        JsonNodePath mockEvalPath = mock(JsonNodePath.class);
        JsonNode mockSchemaNode = mock(JsonNode.class);
        JsonSchema mockParentSchema = mock(JsonSchema.class);
        ValidatorTypeCode mockValidatorType = mock(ValidatorTypeCode.class);
        ValidationContext mockContext = mock(ValidationContext.class);

        // Act – instantiate the concrete test validator
        BaseJsonValidator validator = new TestValidator(
                mockLocation,
                mockEvalPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockContext
        );

        // Assert – the getter must return the same instance supplied to the constructor
        assertSame(mockLocation, validator.getSchemaLocation(),
                "BaseJsonValidator#getSchemaLocation should return the SchemaLocation passed to the constructor");
    }
}
```