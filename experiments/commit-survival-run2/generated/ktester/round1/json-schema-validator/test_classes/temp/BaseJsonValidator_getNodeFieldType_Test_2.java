package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.*;
import com.networknt.schema.SpecVersion.VersionFlag;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getNodeFieldType_Test {

    // ----- Mocks -------------------------------------------------------------
    @Mock
    JsonSchema parentSchema;

    @Mock
    JsonNode parentSchemaNode;

    @Mock
    JsonNode typeNode;

    // ----- Test subject ------------------------------------------------------
    private TestValidator validator;

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}
     * required only to expose the protected {@code getNodeFieldType()} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        // The current version of {@link BaseJsonValidator} declares this method
        // as abstract. For the purpose of these tests we simply return an empty
        // set – the validation logic itself is irrelevant.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            return Collections.emptySet();
        }
    }

    // ----- Lifecycle ---------------------------------------------------------
    @BeforeEach
    void setupBeforeEach() {
        // By default the parent schema returns a non‑null schema node; individual
        // tests will customise the behaviour of the "type" field or the schema node
        // itself.
        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        reset(parentSchema, parentSchemaNode, typeNode);
    }

    /**
     * Provides a stream of arguments representing different scalar JSON node types.
     * For container nodes (array, object) Jackson's {@code asText()} returns an empty string.
     */
    private static Stream<Arguments> scalarTypeNodeProvider() {
        // asText() -> "123"
        JsonNode numericNode = new IntNode(123);
        // asText() -> "true"
        JsonNode booleanNode = BooleanNode.TRUE;
        // asText() -> ""
        JsonNode arrayNode = JsonNodeFactory.instance.arrayNode();
        // asText() -> ""
        JsonNode objectNode = JsonNodeFactory.instance.objectNode();
        // asText() -> "null"
        JsonNode nullNode = NullNode.instance;
        return Stream.of(Arguments.of("Numeric type node should return its textual representation", numericNode, "123"), Arguments.of("Boolean type node should return its textual representation", booleanNode, "true"), Arguments.of("Array type node should return empty string", arrayNode, ""), Arguments.of("Object type node should return empty string", objectNode, ""), Arguments.of("Explicit null node should return the string \"null\"", nullNode, "null"));
    }

    /**
     * Helper that creates a {@link JsonSchema} from the supplied schema node.
     */
    private static JsonSchema schemaFromNode(JsonNode node) {
        return JsonSchemaFactory.getInstance(VersionFlag.V4).getSchema(node);
    }

    // ----- Tests -------------------------------------------------------------
    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // Arrange: schema contains a "type" field with value "string"
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", "string");
        JsonSchema parent = schemaFromNode(schemaNode);
        TestValidator validator = new TestValidator("dummyPath", null, parent, null, null);
        // Act
        String result = validator.getNodeFieldType();
        // Assert
        assertEquals("string", result, "The type field value should be returned");
    }

    @Test
    void getNodeFieldType_returnsNullWhenAbsent() {
        // Arrange: schema does NOT contain a "type" field
        // empty object
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        JsonSchema parent = schemaFromNode(schemaNode);
        TestValidator validator = new TestValidator("dummyPath", null, parent, null, null);
        // Act
        String result = validator.getNodeFieldType();
        // Assert
        assertNull(result, "When the type field is missing, null should be returned");
    }

    @ParameterizedTest(name = "[{index}] type node returns \"{1}\"")
    @MethodSource("scalarTypeNodeProvider")
    void getNodeFieldType_returnsScalarAsText(String description, JsonNode mockNode, String expected) {
        // Arrange: schema with the given type node
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.set("type", typeNode);
        JsonSchema parent = schemaFromNode(schemaNode);
        TestValidator validator = new TestValidator("dummyPath", null, parent, null, null);
        // Act
        String result = validator.getNodeFieldType();
        // Assert
        assertEquals(expected, result, description);
    }

    @Test
    void getNodeFieldType_throwsWhenParentSchemaIsNull() {
        // Arrange: validator is constructed with a null parent schema
        TestValidator validator = new TestValidator("dummyPath", null, null, null, null);
        // Act & Assert
        assertThrows(NullPointerException.class, validator::getNodeFieldType, "Calling getNodeFieldType() with a null parent schema should throw NPE");
    }

    @Test
    void getNodeFieldType_throwsWhenParentSchemaNodeIsNull() {
        // Arrange: create a JsonSchema subclass that returns null for getSchemaNode()
        JsonSchema parent = new JsonSchema(null, null, null) {

            @Override
            public JsonNode getSchemaNode() {
                return null;
            }
        };
        TestValidator validator = new TestValidator("dummyPath", null, parent, null, null);
        // Act & Assert
        assertThrows(NullPointerException.class, validator::getNodeFieldType, "Calling getNodeFieldType() when the parent schema node is null should throw NPE");
    }
}
