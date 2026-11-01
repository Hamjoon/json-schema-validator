package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_message_Test {

    // ----- Mocks for constructor dependencies -----
    @Mock
    ValidationContext validationContext;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidatorTypeCode validatorType;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    // the schema node we expect to be set in the builder
    @Mock
    JsonNode schemaNode;

    // ----- Instance under test -----
    private TestValidator validator;

    /**
     * Concrete subclass of the abstract {@link BaseJsonValidator} used for testing.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /**
         * Minimal implementation – not used by the {@code message()} tests.
         */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the concrete validator with the mocked dependencies.
        validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid interference between tests.
        Mockito.reset(validationContext, parentSchema, validatorType, schemaLocation, evaluationPath, schemaNode);
        Mockito.reset(validationContext, parentSchema, validatorType, schemaLocation, evaluationPath);
    }

    /**
     * Supplies a stream of arguments: a description and the {@link JsonNode}
     * that will be passed to the validator constructor.
     */
    private static Stream<Arguments> schemaNodeProvider() {
        JsonNodeFactory f = JsonNodeFactory.instance;
        // 1. non‑null ObjectNode with a field
        ObjectNode objectNode = f.objectNode().put("field", "value");
        // 2. null schemaNode
        JsonNode nullNode = null;
        // 3. TextNode (non‑ObjectNode)
        TextNode textNode = f.textNode("example");
        // 4. Empty ObjectNode
        ObjectNode emptyObject = f.objectNode();
        // 5. Complex nested ObjectNode
        ObjectNode complex = f.objectNode();
        complex.put("name", "root");
        ObjectNode child = f.objectNode();
        child.put("id", 123);
        ArrayNode array = f.arrayNode();
        array.add("a").add("b");
        child.set("list", array);
        complex.set("child", child);
        return Stream.of(Arguments.of("non‑null ObjectNode", objectNode), Arguments.of("null schemaNode", nullNode), Arguments.of("TextNode", textNode), Arguments.of("empty ObjectNode", emptyObject), Arguments.of("complex nested ObjectNode", complex));
    }

    // -----------------------------------------------------------------
    @Test
    void message_ShouldReturnBuilderWithSchemaNodeSet() {
        // Act: obtain the builder from the method under test and build a ValidationMessage.
        MessageSourceValidationMessage.Builder builder = validator.message();
        Assertions.assertNotNull(builder, "The builder returned by message() must not be null");
        // The builder is expected to be fluent – calling schemaNode again returns the same instance.
        MessageSourceValidationMessage.Builder sameBuilder = builder.schemaNode(schemaNode);
        Assertions.assertSame(builder, sameBuilder, "The schemaNode(...) method should return the same builder instance for chaining");
        // Build the ValidationMessage and verify that the schemaNode inside it matches the one supplied
        // to the validator's constructor.
        ValidationMessage validationMessage = builder.build();
        Assertions.assertNotNull(validationMessage, "Built ValidationMessage must not be null");
        Assertions.assertSame(schemaNode, validationMessage.getSchemaNode(), "The ValidationMessage should contain the schemaNode provided to the validator");
    }

    @ParameterizedTest(name = "message() should propagate {0} to the builder")
    @MethodSource("schemaNodeProvider")
    void message_ShouldPropagateSchemaNodeToBuilder(String description, JsonNode schemaNode) {
        // Arrange – create a validator with the supplied schemaNode
        validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        // Act
        MessageSourceValidationMessage.Builder builder = validator.message();
        // Assert – builder must not be null
        assertNotNull(builder, "Builder returned by message() must not be null");
        // The builder should retain a reference to the exact node supplied.
        ValidationMessage vm = builder.build();
        assertSame(schemaNode, vm.getSchemaNode(), "The ValidationMessage must contain the same JsonNode instance that was given to the validator");
    }

    @Test
    void message_ShouldReflectMutationsPerformedOnOriginalObjectNode() {
        // Arrange – create a mutable ObjectNode, construct the validator,
        // then mutate the node before calling message().
        JsonNodeFactory f = JsonNodeFactory.instance;
        ObjectNode mutable = f.objectNode().put("initial", "value");
        validator = new TestValidator(schemaLocation, evaluationPath, mutable, parentSchema, validatorType, validationContext);
        // Mutate after construction
        mutable.put("addedLater", 42);
        // Act
        MessageSourceValidationMessage.Builder builder = validator.message();
        ValidationMessage vm = builder.build();
        // Assert – the built message must see the mutation
        JsonNode schemaNode = vm.getSchemaNode();
        assertTrue(schemaNode.isObject(), "Schema node should still be an ObjectNode");
        assertEquals(42, schemaNode.get("addedLater").asInt(), "The builder must hold a reference to the original ObjectNode, reflecting later mutations");
    }

    @Test
    void message_TwoSuccessiveCallsReturnDistinctBuilders() {
        // Arrange – any non‑null node works
        validator = new TestValidator(schemaLocation, evaluationPath, JsonNodeFactory.instance.objectNode(), parentSchema, validatorType, validationContext);
        // Act
        MessageSourceValidationMessage.Builder first = validator.message();
        MessageSourceValidationMessage.Builder second = validator.message();
        // Assert – each call yields a fresh builder instance
        assertNotSame(first, second, "Each invocation of message() should return a new Builder, not reuse the previous one");
    }

    @Disabled("Cannot force ValidationMessageHandler.message() to return null without a test‑specific subclass.")
    @Test
    void message_WhenSuperMessageReturnsNull_ShouldThrowNullPointerException() {
        // This test is intentionally left as a placeholder.
    }

    @Disabled("Builder type‑validation is internal; cannot reliably provoke IllegalArgumentException.")
    @Test
    void message_WithUnsupportedNodeType_ShouldThrowIllegalArgumentException() {
        // Placeholder for scenario where the builder rejects a BinaryNode, etc.
    }

    @Disabled("Builder lifecycle enforcement cannot be triggered without a custom Builder implementation.")
    @Test
    void message_WithAlreadyBuiltBuilder_ShouldThrowIllegalStateException() {
        // Placeholder for scenario where the builder is already built before schemaNode() is called.
    }
}
