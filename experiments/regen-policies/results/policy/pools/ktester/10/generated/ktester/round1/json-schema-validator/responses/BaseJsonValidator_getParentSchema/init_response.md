```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BaseJsonValidator#getParentSchema()}.
 *
 * <p>The tests verify that the method simply returns the {@code parentSchema}
 * instance that was supplied to the {@link BaseJsonValidator} constructor.
 * Two scenarios are covered:
 *
 * <ul>
 *   <li>a non‑null parent schema is returned unchanged</li>
 *   <li>a {@code null} parent schema is handled correctly (returns {@code null})</li>
 * </ul>
 *
 * <p>A minimal concrete subclass of {@link BaseJsonValidator} is defined inside
 * the test class because {@code BaseJsonValidator} is abstract. The subclass
 * provides only the abstract methods required for instantiation; their
 * implementations are irrelevant for the behaviour under test.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getParentSchema_Test {

    // -----------------------------------------------------------------------
    // Mocks required for the BaseJsonValidator constructor
    // -----------------------------------------------------------------------
    @Mock
    private SchemaLocation mockSchemaLocation;
    @Mock
    private JsonNodePath mockEvaluationPath;
    @Mock
    private JsonNode mockSchemaNode;
    @Mock
    private ValidationContext mockValidationContext;
    @Mock
    private ValidatorTypeCode mockValidatorTypeCode;

    // -----------------------------------------------------------------------
    // Helper concrete validator used in the tests
    // -----------------------------------------------------------------------
    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}. All abstract
     * methods are implemented with trivial stubs because the tests only need the
     * {@code getParentSchema()} behaviour.
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

        // -------------------------------------------------------------------
        // Abstract methods required by BaseJsonValidator – simple stubs
        // -------------------------------------------------------------------
        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            // Not needed for the current tests
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Test lifecycle – no special global state, so methods are empty
    // -----------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito @Mock fields are initialised automatically by the extension
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------
    @Test
    void getParentSchema_returnsProvidedParentSchema() {
        // Arrange: create a mock parent schema and a validator instance that
        // receives it via the constructor.
        JsonSchema mockParentSchema = mock(JsonSchema.class);
        TestValidator validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorTypeCode,
                mockValidationContext);

        // Act
        JsonSchema result = validator.getParentSchema();

        // Assert
        assertSame(mockParentSchema, result,
                "BaseJsonValidator#getParentSchema() should return the exact instance supplied to the constructor");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentProvided() {
        // Arrange: pass null as the parent schema.
        TestValidator validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                null,                       // <-- no parent schema
                mockValidatorTypeCode,
                mockValidationContext);

        // Act
        JsonSchema result = validator.getParentSchema();

        // Assert
        assertNull(result,
                "BaseJsonValidator#getParentSchema() should return null when the validator was constructed with a null parent");
    }
}
```