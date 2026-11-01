**Generated test class**

```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Set;

/**
 * Tests for {@link BaseJsonValidator#getSchemaLocation()}.
 *
 * <p>The concrete {@code TestValidator} subclass is created only for the purpose of
 * exercising the protected constructor of {@code BaseJsonValidator}. All heavy
 * dependencies are mocked, because the method under test merely returns the
 * {@code SchemaLocation} instance that was supplied to the constructor.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaLocation_Test {

    /** The concrete validator used in the tests. */
    private TestValidator validator;

    /** Mocks for the constructor arguments. */
    @Mock private SchemaLocation mockSchemaLocation;
    @Mock private JsonNodePath mockEvaluationPath;
    @Mock private JsonNode mockSchemaNode;
    @Mock private JsonSchema mockParentSchema;
    @Mock private ValidatorTypeCode mockValidatorType;
    @Mock private ValidationContext mockValidationContext;

    /** Simple concrete subclass of {@link BaseJsonValidator}. */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /** Minimal implementation – the validator does nothing. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        /** The keyword required by {@link JsonValidator}. */
        @Override
        public String getKeyword() {
            return "test";
        }
    }

    /* ---------------------------------------------------------------------- */
    /*                     JUnit lifecycle methods                           */
    /* ---------------------------------------------------------------------- */

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // The mock ValidationContext is used only for the constructor; its
        // methods are never invoked because the constructor guards against null
        // config objects.
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
        // Resetting mocks is handled automatically by MockitoExtension.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* ---------------------------------------------------------------------- */
    /*                               Tests                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    void getSchemaLocation_returnsSameInstanceProvidedInConstructor() {
        // Act
        SchemaLocation returned = validator.getSchemaLocation();

        // Assert
        assertSame(mockSchemaLocation, returned,
                "BaseJsonValidator#getSchemaLocation should return the exact SchemaLocation instance supplied to the constructor");
    }

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