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

/**
 * Tests for {@link BaseJsonValidator#getParentSchema()}.
 *
 * The abstract {@code BaseJsonValidator} cannot be instantiated directly,
 * therefore a minimal concrete subclass is created for the purpose of the test.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getParentSchema_Test {

    // -------------------------------------------------------------------------
    // Mocks for constructor dependencies
    // -------------------------------------------------------------------------
    @Mock
    private SchemaLocation schemaLocation;
    @Mock
    private JsonNodePath evaluationPath;
    @Mock
    private JsonNode schemaNode;
    @Mock
    private JsonSchema parentSchema;
    @Mock
    private ValidatorTypeCode validatorType;
    @Mock
    private ValidationContext validationContext;

    // -------------------------------------------------------------------------
    // Test subject
    // -------------------------------------------------------------------------
    private TestValidator validatorWithParent;
    private TestValidator validatorWithoutParent;

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.
     * All abstract methods are stubbed because the tests focus only on
     * {@code getParentSchema()}.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode,
                    parentSchema, validatorType, validationContext);
        }

        // -----------------------------------------------------------------
        // JsonValidator contract – stub implementations
        // -----------------------------------------------------------------
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public String getKeyword() {
            return null;
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
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle methods
    // -------------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create two validator instances: one with a parent schema, one without.
        validatorWithParent = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,          // <-- the parent we want to retrieve
                validatorType,
                validationContext);

        validatorWithoutParent = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                null,                  // no parent schema
                validatorType,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void getParentSchema_returnsProvidedParent() {
        // Act
        JsonSchema result = validatorWithParent.getParentSchema();

        // Assert
        assertNotNull(result, "Parent schema should not be null when supplied to the constructor");
        assertSame(parentSchema, result, "Returned parent schema must be the exact instance passed to the constructor");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentSupplied() {
        // Act
        JsonSchema result = validatorWithoutParent.getParentSchema();

        // Assert
        assertNull(result, "Parent schema should be null when the constructor receives a null parent");
    }
}
```