package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getNodeFieldType_Test {

    // -----------------------------------------------------------------------
    @Mock
    ValidationContext // not used directly in the test
    validationContext;

    @Mock
    SchemaLocation // can be a mock – constructor does not dereference it here
    schemaLocation;

    @Mock
    JsonNodePath // same as above
    evaluationPath;

    @Mock
    ValidatorTypeCode // not needed for the logic under test
    validatorType;

    @Mock
    ErrorMessageType // required by the constructor
    errorMessageType;

    @Mock
    Keyword // required by the constructor
    keyword;

    @Mock
    JsonSchema // the only collaborator we need to stub
    parentSchema;

    // concrete instance under test
    private TestableBaseJsonValidator validator;

    /**
     * Minimal concrete subclass that simply forwards the protected method so that
     * the test can call it.
     */
    private static class TestableBaseJsonValidator extends BaseJsonValidator {

        TestableBaseJsonValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ErrorMessageType errorMessageType, Keyword keyword, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, errorMessageType, keyword, validationContext, suppressSubSchemaRetrieval);
        }

        /**
         * Exposes the protected {@code getNodeFieldType()} for testing.
         */
        String exposeGetNodeFieldType() {
            return super.getNodeFieldType();
        }

        /**
         * Allows a test to replace the parent schema at runtime.
         */
        void setParentSchema(JsonSchema newParent) {
            // The field in the superclass is protected, so we can assign it directly.
            this.parentSchema = newParent;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The schema node that will be returned by the mocked parent schema.
        ObjectNode parentSchemaNode = JsonNodeFactory.instance.objectNode();
        // By default we put a "type" field – individual tests can modify this node.
        parentSchemaNode.put("type", "string");
        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
        // Create the concrete validator instance.
        validator = new TestableBaseJsonValidator(schemaLocation, evaluationPath, // the validator's own schemaNode is irrelevant for this test
        null, parentSchema, errorMessageType, keyword, validationContext, false);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(validationContext, schemaLocation, evaluationPath, validatorType, errorMessageType, keyword, parentSchema);
    }

    private static Stream<Arguments> nonTextualTypeProvider() {
        return Stream.of(// numeric node → empty string
        Arguments.of(JsonNodeFactory.instance.numberNode(5), ""), // array node → empty string
        Arguments.of(JsonNodeFactory.instance.arrayNode().add("object").add("string"), ""), // null node → literal "null"
        Arguments.of(JsonNodeFactory.instance.nullNode(), "null"));
    }

    // -----------------------------------------------------------------------
    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // Arrange – the default arrangement in setUp already puts "type":"string".
        // Act
        String type = validator.exposeGetNodeFieldType();
        // Assert
        assertEquals("string", type, "The method should return the value of the \"type\" field");
    }

    @Test
    void getNodeFieldType_returnsNullWhenAbsent() {
        // Arrange – remove the "type" field from the mocked parent schema node.
        // no "type"
        ObjectNode emptyNode = JsonNodeFactory.instance.objectNode();
        when(parentSchema.getSchemaNode()).thenReturn(emptyNode);
        // Act
        String type = validator.exposeGetNodeFieldType();
        // Assert
        assertNull(type, "The method should return null when the \"type\" field is missing");
    }

    /**
     * Parameterized test covering non‑textual {@code type} values.
     *
     * <ul>
     *   <li>Numeric node – {@code asText()} returns an empty string.</li>
     *   <li>Array node – {@code asText()} also returns an empty string.</li>
     *   <li>Null node – {@code asText()} returns the literal string {@code "null"}.</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: type node = {0}, expected = \"{1}\"")
    @MethodSource("nonTextualTypeProvider")
    void getNodeFieldType_handlesNonTextualTypeNode(JsonNode typeNode, String expected) {
        // Build a parent schema node that contains the supplied {@code typeNode}.
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode();
        parentNode.set("type", typeNode);
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);
        String actual = validator.exposeGetNodeFieldType();
        assertEquals(expected, actual, () -> "When \"type\" is a " + typeNode.getClass().getSimpleName() + " the method should return \"" + expected + "\"");
    }

    @Test
    void getNodeFieldType_returnsNullWhenParentSchemaNodeIsJsonNull() {
        // The parent schema's node is a Jackson NullNode (represents JSON null).
        when(parentSchema.getSchemaNode()).thenReturn(JsonNodeFactory.instance.nullNode());
        String result = validator.exposeGetNodeFieldType();
        assertNull(result, "When the parent schema node itself is a NullNode, get(\"type\") returns null and the method should return null");
    }

    @Test
    void getNodeFieldType_throwsNPEWhenParentSchemaIsNull() {
        // Construct a validator whose parent schema reference is null.
        TestableBaseJsonValidator validatorWithNullParent = new TestableBaseJsonValidator(schemaLocation, evaluationPath, null, // parentSchema = null
        null, errorMessageType, keyword, validationContext, false);
        // The method should throw NullPointerException when it attempts to call
        // getParentSchema().getSchemaNode().
        assertThrows(NullPointerException.class, validatorWithNullParent::exposeGetNodeFieldType, "Calling getNodeFieldType() with a null parent schema must throw NPE");
    }
}
