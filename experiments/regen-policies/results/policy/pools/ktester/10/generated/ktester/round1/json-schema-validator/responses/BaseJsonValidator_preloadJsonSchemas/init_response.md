```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_preloadJsonSchemas_Test {

    // Mocks required for the BaseJsonValidator constructor
    @Mock SchemaLocation mockSchemaLocation;
    @Mock JsonNodePath mockEvaluationPath;
    @Mock JsonNode mockSchemaNode;
    @Mock JsonSchema mockParentSchema;
    @Mock ValidatorTypeCode mockValidatorType;
    @Mock ValidationContext mockValidationContext;

    // The concrete test subclass
    private TestValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the concrete validator with mocked dependencies.
        validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference
        Mockito.reset(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void shouldInvokeInitializeValidatorsOnEachSchema() {
        // GIVEN a collection of JsonSchema mocks
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(schema1, schema2);

        // WHEN the protected method is exercised via the public wrapper
        validator.callPreloadJsonSchemas(schemas);

        // THEN each schema's initializeValidators() must be called exactly once
        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
        verifyNoMoreInteractions(schema1, schema2);
    }

    @Test
    void shouldDoNothingWhenCollectionIsEmpty() {
        // GIVEN an empty collection
        Collection<JsonSchema> empty = Collections.emptyList();

        // WHEN the method is invoked
        validator.callPreloadJsonSchemas(empty);

        // THEN no interactions with any JsonSchema occur (nothing to verify)
        // The test passes if no exception is thrown.
    }

    /**
     * Minimal concrete subclass exposing the protected {@code preloadJsonSchemas}
     * method for testing.
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

        /**
         * Public wrapper that delegates to the protected method under test.
         */
        void callPreloadJsonSchemas(Collection<JsonSchema> schemas) {
            super.preloadJsonSchemas(schemas);
        }
    }
}
```