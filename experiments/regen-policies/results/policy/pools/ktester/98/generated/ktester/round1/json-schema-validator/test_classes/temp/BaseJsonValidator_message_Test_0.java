package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_message_Test {

    private SchemaLocation schemaLocation;

    private JsonNodePath evaluationPath;

    private JsonSchema parentSchema;

    private ValidatorTypeCode validatorType;

    private ValidationContext validationContext;

    /**
     * Concrete subclass used only for testing the protected {@code message()} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /**
         * Public wrapper that simply delegates to the protected {@code message()} implementation.
         */
        public MessageSourceValidationMessage.Builder callMessage() {
            return super.message();
        }

        /* The {@code validate} method is abstract in the base class – a trivial implementation is enough
         * for the purpose BaseJsonValidator_message_Testthis test. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void initMocks() {
        schemaLocation = mock(SchemaLocation.class);
        evaluationPath = mock(JsonNodePath.class);
        parentSchema = mock(JsonSchema.class);
        validatorType = mock(ValidatorTypeCode.class);
        validationContext = mock(ValidationContext.class);
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
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        // ----- Act -------------------------------------------------------------
        MessageSourceValidationMessage.Builder builder = validator.callMessage();
        // ----- Assert -----------------------------------------------------------
        // The builder returned by BaseJsonValidator.message() must contain the same
        // schema node that was supplied to the validator via its constructor.
        // The builder API provides a getter for the stored schema node.
        assertNotNull(builder, "Builder must not be null");
        assertSame(schemaNode, builder.getSchemaNode(), "The builder's schema node should be the same instance supplied to the validator");
    }

    @Test
    void message_ShouldPopulateBuilderWithNullSchemaNode() {
        // Arrange: construct validator with a null schemaNode
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, null, parentSchema, validatorType, validationContext);
        // Act
        MessageSourceValidationMessage.Builder builder = validator.callMessage();
        // Assert
        assertNotNull(builder, "Builder must not be null even when schemaNode is null");
        assertNull(builder.getSchemaNode(), "Builder's schemaNode should be null when the validator was constructed with null");
    }

    @Test
    void message_ShouldReturnDistinctBuilderInstancesOnEachCall() {
        // Arrange
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode().put("type", "integer");
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        // Act
        MessageSourceValidationMessage.Builder first = validator.callMessage();
        MessageSourceValidationMessage.Builder second = validator.callMessage();
        // Assert
        assertNotSame(first, second, "Each invocation of message() should produce a new Builder instance");
    }

    @Test
    void message_ShouldPropagateJacksonNullNodeInstance() {
        // Arrange: use Jackson's NullNode singleton
        JsonNode nullNode = NullNode.getInstance();
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, nullNode, parentSchema, validatorType, validationContext);
        // Act
        MessageSourceValidationMessage.Builder builder = validator.callMessage();
        // Assert
        assertSame(nullNode, builder.getSchemaNode(), "Builder must contain the exact NullNode instance supplied to the validator");
    }

    @Test
    void message_ShouldPreserveReferenceToMutableArrayNode() {
        // Arrange: create a mutable ArrayNode and keep a reference to it
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3);
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, arrayNode, parentSchema, validatorType, validationContext);
        // Act: obtain builder and then mutate the original ArrayNode
        MessageSourceValidationMessage.Builder builder = validator.callMessage();
        assertSame(arrayNode, builder.getSchemaNode(), "Builder should hold the same ArrayNode reference supplied at construction");
        // Mutate the original node
        arrayNode.add(4);
        // Assert: the builder's schemaNode reflects the mutation (same reference, updated size)
        JsonNode nodeFromBuilder = builder.getSchemaNode();
        assertTrue(nodeFromBuilder.isArray(), "Builder's schemaNode should still be an array");
        assertEquals(4, ((ArrayNode) nodeFromBuilder).size(), "Builder's schemaNode must reflect mutations performed on the original ArrayNode");
    }

    @Test
    void message_ShouldUseDefaultMessageSourceWhenNotOverridden() {
        // Arrange: any schemaNode is fine; we use a simple ObjectNode
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode().put("type", "boolean");
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        // Act
        MessageSourceValidationMessage.Builder builder = validator.callMessage();
        // Assert
        assertNotNull(builder.getMessageSource(), "Builder must contain a MessageSource instance");
        assertTrue(builder.getMessageSource() instanceof DefaultMessageSource, "The default MessageSource implementation should be DefaultMessageSource");
    }
}
