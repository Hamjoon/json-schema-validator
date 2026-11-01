```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit‑tests for the protected {@code BaseJsonValidator.message()} method.
 *
 * <p>The original test failed with a {@code NullPointerException} because the
 * {@link BaseJsonValidator} constructor requires a non‑null {@link ValidationContext}
 * (it is used to create a {@link ValidationMessageHandler}).  The test supplied
 * {@code null} for that argument which caused the NPE during construction.
 *
 * <p>To fix the problem we create a minimal {@link ValidationContext} instance
 * using the library's default {@link DefaultMessageSource} and a fresh
 * {@link SchemaValidatorsConfig}.  This satisfies the constructor contract while
 * keeping the tests focused on the behaviour of {@code message()}.
 */
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

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /** Public wrapper that simply delegates to the protected {@code message()} implementation. */
        public MessageSourceValidationMessage.Builder callMessage() {
            return super.message();
        }

        /** Minimal implementation of the abstract {@code validate} method. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                JsonNodePath instanceLocation) {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void initMocks() {
        // The tests do not depend on these objects; they can safely be null.
        schemaLocation = null;
        evaluationPath = null;
        parentSchema = null;
        validatorType = null;

        // A non‑null ValidationContext is required by BaseJsonValidator.
        // Using the library's default MessageSource and a fresh config is sufficient.
        validationContext = new ValidationContext(new DefaultMessageSource(),
                                                  new SchemaValidatorsConfig());
    }

    /**
     * Helper that extracts a private field value via reflection.
     */
    private static Object getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new AssertionError("Unable to access field '" + fieldName + "' via reflection", e);
        }
    }

    @Test
    void message_ShouldPopulateBuilderWithSchemaNode() {
        // ----- Arrange ---------------------------------------------------------
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode().put("type", "string");
        TestValidator validator = new TestValidator(null, null, schemaNode, null, null, validationContext);
        // ----- Act -------------------------------------------------------------
        MessageSourceValidationMessage.Builder builder = validator.callMessage();
        // ----- Assert -----------------------------------------------------------
        assertNotNull(builder, "Builder must not be null");
        JsonNode nodeFromBuilder = (JsonNode) getPrivateField(builder, "schemaNode");
        assertSame(schemaNode, nodeFromBuilder,
                "The builder's schema node should be the same instance supplied to the validator");
    }

    @Test
    void message_ShouldPopulateBuilderWithNullSchemaNode() {
        // Arrange: construct validator with a null schemaNode
        TestValidator validator = new TestValidator(null, null, null, null, null, validationContext);
        // Act
        MessageSourceValidationMessage.Builder builder = validator.callMessage();
        // Assert
        assertNotNull(builder, "Builder must not be null even when schemaNode is null");
        JsonNode nodeFromBuilder = (JsonNode) getPrivateField(builder, "schemaNode");
        assertNull(nodeFromBuilder,
                "Builder's schemaNode should be null when the validator was constructed with null");
    }

    @Test
    void message_ShouldReturnDistinctBuilderInstancesOnEachCall() {
        // Arrange
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode().put("type", "integer");
        TestValidator validator = new TestValidator(null, null, schemaNode, null, null, validationContext);
        // Act
        MessageSourceValidationMessage.Builder first = validator.callMessage();
        MessageSourceValidationMessage.Builder second = validator.callMessage();
        // Assert
        assertNotSame(first, second,
                "Each invocation of message() should produce a new Builder instance");
    }

    @Test
    void message_ShouldPropagateJacksonNullNodeInstance() {
        // Arrange: use Jackson's NullNode singleton
        JsonNode nullNode = NullNode.getInstance();
        TestValidator validator = new TestValidator(null, null, nullNode, null, null, validationContext);
        // Act
        MessageSourceValidationMessage.Builder builder = validator.callMessage();
        // Assert
        JsonNode nodeFromBuilder = (JsonNode) getPrivateField(builder, "schemaNode");
        assertSame(nullNode, nodeFromBuilder,
                "Builder must contain the exact NullNode instance supplied to the validator");
    }

    @Test
    void message_ShouldPreserveReferenceToMutableArrayNode() {
        // Arrange: create a mutable ArrayNode and keep a reference to it
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode().add(1).add(2).add(3);
        TestValidator validator = new TestValidator(null, null, arrayNode, null, null, validationContext);
        // Act: obtain builder and then mutate the original ArrayNode
        MessageSourceValidationMessage.Builder builder = validator.callMessage();
        JsonNode nodeFromBuilder = (JsonNode) getPrivateField(builder, "schemaNode");
        assertSame(arrayNode, nodeFromBuilder,
                "Builder should hold the same ArrayNode reference supplied at construction");
        // Mutate the original node
        arrayNode.add(4);
        // Assert: the builder's schemaNode reflects the mutation (same reference, updated size)
        assertTrue(nodeFromBuilder.isArray(),
                "Builder's schemaNode should still be an array");
        assertEquals(4, ((ArrayNode) nodeFromBuilder).size(),
                "Builder's schemaNode must reflect mutations performed on the original ArrayNode");
    }
}
```