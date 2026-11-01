```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Tests for the protected {@code preloadJsonSchemas} method of {@link BaseJsonValidator}.
 * The method simply iterates over the supplied {@link JsonSchema} collection and
 * invokes {@code initializeValidators()} on each element.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_preloadJsonSchemas_Test {

    // --------------------------------------------------------------------- //
    // Test fixture
    // --------------------------------------------------------------------- //

    @Mock
    ValidationContext validationContext;      // used only to satisfy the constructor
    @Mock
    SchemaLocation schemaLocation;           // used only to satisfy the constructor
    @Mock
    JsonNodePath evaluationPath;             // used only to satisfy the constructor
    @Mock
    JsonNode schemaNode;                     // used only to satisfy the constructor
    @Mock
    JsonSchema parentSchema;                 // used only to satisfy the constructor
    @Mock
    ValidatorTypeCode validatorType;       // used only to satisfy the constructor

    /** Concrete subclass exposing the protected method for testing. */
    static class TestValidator extends BaseJsonValidator {
        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode,
                  parentSchema, validatorType, validationContext);
        }

        /** Helper that forwards to the protected method under test. */
        void callPreloadJsonSchemas(Collection<JsonSchema> schemas) {
            preloadJsonSchemas(schemas);
        }

        // ----------------------------------------------------------------- //
        // Minimal implementations required by the JsonValidator interface.
        // ----------------------------------------------------------------- //

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Set.of(); // no validation needed for these tests
        }

        @Override
        public String getKeyword() {
            return null;
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }
    }

    private TestValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh validator instance for each test.
        validator = new TestValidator(schemaLocation, evaluationPath, schemaNode,
                                      parentSchema, validatorType, validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    // --------------------------------------------------------------------- //
    // Tests
    // --------------------------------------------------------------------- //

    @Test
    void preloadJsonSchemas_callsInitializeValidatorsOnEachSchema() {
        // GIVEN a collection of two mock JsonSchema objects
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = List.of(schema1, schema2);

        // WHEN the protected method is invoked via the public wrapper
        validator.callPreloadJsonSchemas(schemas);

        // THEN each schema's initialise method must have been called exactly once
        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
    }

    @Test
    void preloadJsonSchemas_withEmptyCollectionDoesNotThrow() {
        // GIVEN an empty collection
        Collection<JsonSchema> empty = List.of();

        // WHEN the method is called
        Assertions.assertDoesNotThrow(() -> validator.callPreloadJsonSchemas(empty));

        // THEN no interactions occur – verification is implicit as there are no mocks
    }
}
```