```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#getParentSchema()}.
 *
 * <p>The abstract {@code BaseJsonValidator} cannot be instantiated directly,
 * therefore a minimal concrete subclass {@code TestValidator} is used.
 * The tests cover:
 *
 * <ul>
 *   <li>normal construction with a parent schema</li>
 *   <li>construction without a parent schema</li>
 *   <li>changing the {@code parentSchema} field after construction (via reflection)</li>
 *   <li>resetting the field to {@code null}</li>
 *   <li>idempotent behaviour of repeated calls</li>
 *   <li>handling of a mock that throws on any method call (ensuring the getter does not invoke it)</li>
 * </ul>
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
    private JsonSchema constructorParentSchema;   // used for the “constructor initialization” test
    @Mock
    private JsonSchema secondParentSchema;       // used for the “field changed after construction” test
    @Mock
    private JsonSchema exceptionMock;             // mock that throws on any method call
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
        // Instance with a non‑null parent (used by most tests)
        validatorWithParent = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                constructorParentSchema,   // parent supplied at construction time
                validatorType,
                validationContext);

        // Instance without a parent (used by the “null parent” test)
        validatorWithoutParent = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                null,
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
    // Helper to modify the protected {@code parentSchema} field via reflection
    // -------------------------------------------------------------------------
    private void setParentSchema(TestValidator validator, JsonSchema newParent) throws Exception {
        Field field = ValidationMessageHandler.class.getDeclaredField("parentSchema");
        field.setAccessible(true);
        field.set(validator, newParent);
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
        assertSame(constructorParentSchema, result,
                "Returned parent schema must be the exact instance passed to the constructor");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentSupplied() {
        // Act
        JsonSchema result = validatorWithoutParent.getParentSchema();

        // Assert
        assertNull(result, "Parent schema should be null when the constructor receives a null parent");
    }

    @Test
    void getParentSchema_reflectivelyChangedAfterConstruction() throws Exception {
        // Change the protected field to a different mock
        setParentSchema(validatorWithParent, secondParentSchema);

        // Act
        JsonSchema result = validatorWithParent.getParentSchema();

        // Assert
        assertSame(secondParentSchema, result,
                "After reflective change, getParentSchema() must return the newly set instance");
    }

    @Test
    void getParentSchema_resetToNullAfterBeingSet() throws Exception {
        // Initially the field holds a non‑null mock (constructorParentSchema)
        assertNotNull(validatorWithParent.getParentSchema(),
                "Pre‑condition: parent schema should be non‑null before reset");

        // Reset to null via reflection
        setParentSchema(validatorWithParent, null);

        // Act
        JsonSchema result = validatorWithParent.getParentSchema();

        // Assert
        assertNull(result, "After resetting to null, getParentSchema() must return null");
    }

    @Test
    void getParentSchema_isIdempotent() {
        // First call
        JsonSchema first = validatorWithParent.getParentSchema();
        // Second call
        JsonSchema second = validatorWithParent.getParentSchema();
        // Third call
        JsonSchema third = validatorWithParent.getParentSchema();

        // All references must be identical
        assertSame(first, second, "Repeated calls must return the same instance (first vs second)");
        assertSame(second, third, "Repeated calls must return the same instance (second vs third)");
    }

    @Test
    void getParentSchema_returnsExceptionThrowingMockWithoutInvokingIt() throws Exception {
        // Configure the mock to throw on any method invocation
        JsonSchema throwingMock = mock(JsonSchema.class, invocation -> {
            throw new RuntimeException("Method should not be called");
        });

        // Inject the throwing mock via reflection
        setParentSchema(validatorWithParent, throwingMock);

        // Act – the getter must simply return the reference; no method on the mock is called
        JsonSchema result = validatorWithParent.getParentSchema();

        // Assert
        assertSame(throwingMock, result,
                "Getter should return the reference even if the mock throws on method calls");
    }
}
```