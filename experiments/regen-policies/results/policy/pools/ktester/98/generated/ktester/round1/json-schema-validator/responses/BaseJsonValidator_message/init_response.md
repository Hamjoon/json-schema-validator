```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BaseJsonValidator#message()}.
 *
 * <p>The test creates a concrete subclass of {@code BaseJsonValidator}
 * that exposes the protected {@code message()} method via a public wrapper.
 * The constructor arguments are mocked where possible – only the {@code schemaNode}
 * is a real object because the behaviour under test is the propagation of this
 * node into the {@code MessageSourceValidationMessage.Builder} returned by
 * {@code message()}.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_message_Test {

    /** Concrete subclass used only for testing the protected {@code message()} method. */
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

        /** Public wrapper that simply delegates to the protected {@code message()} implementation. */
        public MessageSourceValidationMessage.Builder callMessage() {
            return super.message();
        }

        /* The {@code validate} method is abstract in the base class – a trivial implementation is enough
         * for the purpose of this test. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void message_ShouldPopulateBuilderWithSchemaNode() {
        // ----- Arrange ---------------------------------------------------------
        // a real schema node – this is the value we expect to be set on the builder
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode().put("type", "string");

        // the remaining constructor arguments are not relevant for the behaviour under test,
        // therefore they are mocked.
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        JsonSchema parentSchema = mock(JsonSchema.class);
        ValidatorTypeCode validatorType = mock(ValidatorTypeCode.class);
        ValidationContext validationContext = mock(ValidationContext.class);

        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext
        );

        // ----- Act -------------------------------------------------------------
        MessageSourceValidationMessage.Builder builder = validator.callMessage();

        // ----- Assert -----------------------------------------------------------
        // The builder returned by BaseJsonValidator.message() must contain the same
        // schema node that was supplied to the validator via its constructor.
        // The builder API provides a getter for the stored schema node.
        assertNotNull(builder, "Builder must not be null");
        assertSame(schemaNode, builder.getSchemaNode(),
                "The builder's schema node should be the same instance supplied to the validator");
    }
}
```