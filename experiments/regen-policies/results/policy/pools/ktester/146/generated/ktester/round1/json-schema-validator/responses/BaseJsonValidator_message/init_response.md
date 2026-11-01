```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

/**
 * Tests for the {@link BaseJsonValidator#message()} method.
 * The test verifies that the builder returned by {@code message()}
 * contains the {@code schemaNode} supplied to the validator via its constructor.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_message_Test {

    // ----- Mocks for constructor dependencies -----
    @Mock ValidationContext validationContext;
    @Mock JsonSchema parentSchema;
    @Mock ValidatorTypeCode validatorType;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;               // the schema node we expect to be set in the builder

    // ----- Instance under test -----
    private TestValidator validator;

    /** Concrete subclass of the abstract {@link BaseJsonValidator} used for testing. */
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

        /** Minimal implementation – not used by the {@code message()} test. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    // -----------------------------------------------------------------
    // Lifecycle methods
    // -----------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the concrete validator with the mocked dependencies.
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
        // Reset mocks to avoid interference between tests.
        Mockito.reset(validationContext, parentSchema, validatorType,
                schemaLocation, evaluationPath, schemaNode);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    // -----------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------
    @Test
    void message_ShouldReturnBuilderWithSchemaNodeSet() {
        // Act: obtain the builder from the method under test and build a ValidationMessage.
        MessageSourceValidationMessage.Builder builder = validator.message();
        Assertions.assertNotNull(builder, "The builder returned by message() must not be null");

        // The builder is expected to be fluent – calling schemaNode again returns the same instance.
        MessageSourceValidationMessage.Builder sameBuilder = builder.schemaNode(schemaNode);
        Assertions.assertSame(builder, sameBuilder,
                "The schemaNode(...) method should return the same builder instance for chaining");

        // Build the ValidationMessage and verify that the schemaNode inside it matches the one supplied
        // to the validator's constructor.
        ValidationMessage validationMessage = builder.build();
        Assertions.assertNotNull(validationMessage, "Built ValidationMessage must not be null");
        Assertions.assertSame(schemaNode, validationMessage.getSchemaNode(),
                "The ValidationMessage should contain the schemaNode provided to the validator");
    }
}
```