```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the protected {@code getNodeFieldType()} method of {@link BaseJsonValidator}.
 *
 * <p>
 * A minimal concrete subclass {@code TestableBaseJsonValidator} is used to expose the
 * protected method. All collaborators are mocked – the only interaction exercised is
 * {@link JsonSchema#getSchemaNode()}.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getNodeFieldType_Test {

    // -----------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------
    @Mock
    ValidationContext validationContext;          // not used directly in the test

    @Mock
    SchemaLocation schemaLocation;               // not dereferenced in the focal method

    @Mock
    JsonNodePath evaluationPath;                 // same as above

    @Mock
    ErrorMessageType errorMessageType;           // required by the constructor

    @Mock
    Keyword keyword;                             // required by the constructor

    @Mock
    JsonSchema parentSchema;                     // the only collaborator we need to stub

    private TestableBaseJsonValidator validator; // concrete instance under test

    // -----------------------------------------------------------------------
    // Helper concrete class exposing the protected method
    // -----------------------------------------------------------------------
    /**
     * Minimal concrete subclass that simply forwards the protected method so that
     * the test can call it.
     */
    private static class TestableBaseJsonValidator extends BaseJsonValidator {
        TestableBaseJsonValidator(SchemaLocation schemaLocation,
                                  JsonNodePath evaluationPath,
                                  JsonNode schemaNode,
                                  JsonSchema parentSchema,
                                  ErrorMessageType errorMessageType,
                                  Keyword keyword,
                                  ValidationContext validationContext,
                                  boolean suppressSubSchemaRetrieval) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                  errorMessageType, keyword, validationContext, suppressSubSchemaRetrieval);
        }

        /** Exposes the protected {@code getNodeFieldType()} for testing. */
        String exposeGetNodeFieldType() {
            return super.getNodeFieldType();
        }

        /** Allows a test to replace the parent schema at runtime. */
        void setParentSchema(JsonSchema newParent) {
            // The field in the superclass is protected, so we can assign it directly.
            this.parentSchema = newParent;
        }
    }

    // -----------------------------------------------------------------------
    // Lifecycle methods
    // -----------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default the mocked parent schema returns an ObjectNode with a "type":"string".
        ObjectNode defaultParentNode = JsonNodeFactory.instance.objectNode();
        defaultParentNode.put("type", "string");
        when(parentSchema.getSchemaNode()).thenReturn(defaultParentNode);

        // Create the concrete validator instance.
        validator = new TestableBaseJsonValidator(
                schemaLocation,
                evaluationPath,
                null,               // the validator's own schemaNode is irrelevant for this test
                parentSchema,
                errorMessageType,
                keyword,
                validationContext,
                false);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(validationContext, schemaLocation, evaluationPath,
              errorMessageType, keyword, parentSchema);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    // -----------------------------------------------------------------------
    // Test cases
    // -----------------------------------------------------------------------

    @Test
    void getNodeFieldType_returnsTypeWhenPresent() {
        // The default arrangement in setUp already puts "type":"string".
        String type = validator.exposeGetNodeFieldType();
        assertEquals("string", type, "Should return the value of the \"type\" field");
    }

    @Test
    void getNodeFieldType_returnsNullWhenAbsent() {
        // Remove the "type" field from the mocked parent schema node.
        ObjectNode emptyNode = JsonNodeFactory.instance.objectNode(); // no "type"
        when(parentSchema.getSchemaNode()).thenReturn(emptyNode);
        String type = validator.exposeGetNodeFieldType();
        assertNull(type, "Should return null when the \"type\" field is missing");
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
        assertEquals(expected, actual,
                () -> "When \"type\" is a " + typeNode.getClass().getSimpleName()
                        + " the method should return \"" + expected + "\"");
    }

    private static Stream<Arguments> nonTextualTypeProvider() {
        return Stream.of(
                // numeric node → empty string
                Arguments.of(JsonNodeFactory.instance.numberNode(5), ""),
                // array node → empty string
                Arguments.of(JsonNodeFactory.instance.arrayNode()
                        .add("object")
                        .add("string"), ""),
                // null node → literal "null"
                Arguments.of(JsonNodeFactory.instance.nullNode(), "null")
        );
    }

    @Test
    void getNodeFieldType_returnsNullWhenParentSchemaNodeIsJsonNull() {
        // The parent schema's node is a Jackson NullNode (represents JSON null).
        when(parentSchema.getSchemaNode()).thenReturn(JsonNodeFactory.instance.nullNode());

        String result = validator.exposeGetNodeFieldType();
        assertNull(result,
                "When the parent schema node itself is a NullNode, get(\"type\") returns null and the method should return null");
    }

    @Test
    void getNodeFieldType_throwsNPEWhenParentSchemaIsNull() {
        // Construct a validator whose parent schema reference is null.
        TestableBaseJsonValidator validatorWithNullParent = new TestableBaseJsonValidator(
                schemaLocation,
                evaluationPath,
                null,
                null,                       // parentSchema = null
                errorMessageType,
                keyword,
                validationContext,
                false);

        // The method should throw NullPointerException when it attempts to call
        // getParentSchema().getSchemaNode().
        assertThrows(NullPointerException.class,
                validatorWithNullParent::exposeGetNodeFieldType,
                "Calling getNodeFieldType() with a null parent schema must throw NPE");
    }
}
```