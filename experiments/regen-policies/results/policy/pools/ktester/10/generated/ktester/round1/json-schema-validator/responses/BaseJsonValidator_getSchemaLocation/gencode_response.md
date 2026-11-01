```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link BaseJsonValidator#getSchemaLocation()}.
 *
 * <p>The {@code BaseJsonValidator} class stores the {@link SchemaLocation}
 * supplied via its protected constructor (inherited from {@link ValidationMessageHandler}).
 * The method under test simply returns that field, so the tests verify:
 *
 * <ul>
 *   <li>the same non‑null instance is returned when the validator is constructed with a valid {@code SchemaLocation}.</li>
 *   <li>the method returns {@code null} when the validator is constructed with a {@code null} value.</li>
 * </ul>
 *
 * <p>A minimal concrete subclass {@code TestValidator} is defined solely to
 * expose the protected constructor of {@code BaseJsonValidator}. All heavy
 * dependencies are mocked because they are irrelevant for the behaviour of
 * {@code getSchemaLocation()}.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaLocation_Test {

    /** The concrete validator used in the tests. */
    private TestValidator validator;

    /* ------------------------------------------------------------------ */
    /* Mocks for the constructor arguments of BaseJsonValidator.          */
    /* ------------------------------------------------------------------ */
    @Mock private SchemaLocation mockSchemaLocation;
    @Mock private JsonNodePath mockEvaluationPath;
    @Mock private JsonNode mockSchemaNode;
    @Mock private JsonSchema mockParentSchema;
    @Mock private ValidatorTypeCode mockValidatorType;
    @Mock private ValidationContext mockValidationContext;

    /**
     * Simple concrete subclass of {@link BaseJsonValidator}. It provides the
     * mandatory abstract {@code validate} method and a dummy {@code getKeyword}
     * implementation. No additional behaviour is required for the tests.
     */
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
            // No validation logic needed for these tests.
            return Collections.emptySet();
        }

        @Override
        public String getKeyword() {
            return "test";
        }
    }

    /* ------------------------------------------------------------------ */
    /* JUnit lifecycle methods                                            */
    /* ------------------------------------------------------------------ */

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a validator with a non‑null SchemaLocation mock.
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
        // MockitoExtension automatically resets mocks.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* ------------------------------------------------------------------ */
    /* Test cases                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Verify that {@code getSchemaLocation()} returns the exact
     * {@link SchemaLocation} instance that was supplied to the constructor.
     */
    @Test
    void getSchemaLocation_returnsSameInstanceProvidedInConstructor() {
        // Act
        SchemaLocation returned = validator.getSchemaLocation();

        // Assert
        assertSame(mockSchemaLocation, returned,
                "BaseJsonValidator#getSchemaLocation should return the exact SchemaLocation instance supplied to the constructor");
    }

    /**
     * Verify that {@code getSchemaLocation()} returns {@code null}
     * when the validator was constructed with a {@code null} value.
     */
    @Test
    void getSchemaLocation_returnsNull_whenConstructorWasGivenNull() {
        // Arrange – create a validator with a null SchemaLocation
        TestValidator nullLocationValidator = new TestValidator(
                null,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext);

        // Act
        SchemaLocation returned = nullLocationValidator.getSchemaLocation();

        // Assert
        assertNull(returned,
                "BaseJsonValidator#getSchemaLocation should return null when the validator was constructed with a null SchemaLocation");
    }
}
```