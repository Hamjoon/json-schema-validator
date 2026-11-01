```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_preloadJsonSchemas_Test {

    /** Mocks that are reused for creating the concrete validator instance */
    @Mock SchemaLocation mockSchemaLocation;
    @Mock JsonNodePath mockEvaluationPath;
    @Mock JsonNode mockSchemaNode;
    @Mock ValidationContext mockValidationContext;
    @Mock ValidatorTypeCode mockValidatorType;

    /** Concrete subclass exposing the protected method */
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

    private TestValidator validator;

    @BeforeEach
    void setupBeforeEach() {
        // The constructor of BaseJsonValidator only stores the arguments;
        // the mocks are sufficient – no behaviour is required.
        validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                null,                 // parentSchema – not needed for this test
                mockValidatorType,
                mockValidationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to release for this simple test
    }

    @Test
    void preloadJsonSchemas_callsInitializeValidatorsOnEachSchema() {
        // GIVEN – a collection of mocked JsonSchema objects
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(schema1, schema2);

        // WHEN – the protected method is invoked
        validator.preloadJsonSchemas(schemas);

        // THEN – each schema must have its initialise method called exactly once
        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global initialisation required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global clean‑up required
    }
}
```