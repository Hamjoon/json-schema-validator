```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#getNodeFieldType()}.
 *
 * <p>The protected method is exercised via a tiny concrete subclass
 * {@link TestValidator}.  All dependencies are mocked, and real Jackson
 * {@link JsonNode} implementations are used where possible (e.g. {@link TextNode},
 * {@link IntNode}, {@link BooleanNode}, {@link NullNode}, {@link ObjectNode},
 * {@link ArrayNode}).  This gives us confidence that the method correctly
 * forwards the call to {@code asText()} for every possible node type.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getNodeFieldType_Test {

    /** Simple concrete implementation used only for testing the protected method. */
    static class TestValidator extends BaseJsonValidator {
        TestValidator(JsonSchema parentSchema) {
            // The super‑constructor arguments that are not used by getNodeFieldType()
            // are passed as {@code null}.  The signature matches the one used in the
            // original test scaffold.
            super(null, null, null, parentSchema, null, null);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // Not needed for these tests.
            return null;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test setup required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- *
     *  Branch and value coverage (first group of test cases)
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] expected=''{0}'' – {1}")
    @MethodSource("provideBranchAndValueCases")
    void getNodeFieldType_branchAndValueCoverage(String expected, String description) {
        // Arrange
        JsonSchema parentSchema = mock(JsonSchema.class);
        JsonNode parentSchemaNode = mock(JsonNode.class);
        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);

        // Build the appropriate "type" node according to the expected outcome.
        JsonNode typeNode;
        if (expected == null) {
            // Simulate missing "type" field → get("type") returns null.
            when(parentSchemaNode.get("type")).thenReturn(null);
            typeNode = null;
        } else {
            // For all other cases we create a concrete JsonNode that returns the
            // expected string when asText() is called.
            switch (expected) {
                case "object":
                case "array":
                case "  object  ":
                case "π":
                    typeNode = TextNode.valueOf(expected);
                    break;
                case "5":
                    typeNode = IntNode.valueOf(5);
                    break;
                case "true":
                    typeNode = BooleanNode.TRUE;
                    break;
                case "false":
                    typeNode = BooleanNode.FALSE;
                    break;
                case "":
                    // Empty string can be produced by NullNode, ObjectNode or ArrayNode.
                    // The specific node type is decided in the test data provider.
                    typeNode = NullNode.getInstance(); // placeholder, will be overridden.
                    break;
                default:
                    typeNode = TextNode.valueOf(expected);
            }
            when(parentSchemaNode.get("type")).thenReturn(typeNode);
        }

        // For the special cases where the expected value is an empty string but the
        // underlying node is NOT a NullNode (ObjectNode / ArrayNode), the provider
        // supplies the concrete node via the third argument.
        // (Handled in the provider – see below.)

        TestValidator validator = new TestValidator(parentSchema);

        // Act
        String actual = validator.getNodeFieldType();

        // Assert
        if (expected == null) {
            assertNull(actual, description);
        } else {
            assertEquals(expected, actual, description);
        }
    }

    private static Stream<Arguments> provideBranchAndValueCases() {
        // The first three cases come from the original JSON group.
        return Stream.of(
                // 1. textual "object"
                Arguments.of("object", "Parent schema's schemaNode contains a \"type\" field with textual value \"object\"."),
                // 2. textual "array"
                Arguments.of("array", "Parent schema's schemaNode contains a \"type\" field with textual value \"array\"."),
                // 3. missing field → null
                Arguments.of(null, "Parent schema's schemaNode does NOT contain a \"type\" field; method should return null."),
                // 4. explicit JSON null → empty string
                Arguments.of("", "Parent schema's schemaNode contains a \"type\" field whose value is JSON null (NullNode)."),
                // 5. numeric value → \"5\"
                Arguments.of("5", "Parent schema's schemaNode contains a numeric \"type\" value (5).")
        );
    }

    /* --------------------------------------------------------------------- *
     *  Additional type variations (second group of test cases)
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] expected=''{0}'' – {1}")
    @MethodSource("provideAdditionalTypeVariations")
    void getNodeFieldType_additionalVariations(String expected, JsonNode typeNode, String description) {
        // Arrange
        JsonSchema parentSchema = mock(JsonSchema.class);
        JsonNode parentSchemaNode = mock(JsonNode.class);
        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
        when(parentSchemaNode.get("type")).thenReturn(typeNode);

        TestValidator validator = new TestValidator(parentSchema);

        // Act
        String actual = validator.getNodeFieldType();

        // Assert
        assertEquals(expected, actual, description);
    }

    private static Stream<Arguments> provideAdditionalTypeVariations() {
        return Stream.of(
                // boolean true → "true"
                Arguments.of("true", BooleanNode.TRUE,
                        "Parent schema's schemaNode contains a \"type\" field with boolean true."),
                // boolean false → "false"
                Arguments.of("false", BooleanNode.FALSE,
                        "Parent schema's schemaNode contains a \"type\" field with boolean false."),
                // object node → empty string
                Arguments.of("", new ObjectNode(JsonNodeFactory.instance),
                        "Parent schema's schemaNode contains a \"type\" field whose value is a JSON object; asText() returns empty string."),
                // array node → empty string
                Arguments.of("", new ArrayNode(JsonNodeFactory.instance),
                        "Parent schema's schemaNode contains a \"type\" field whose value is a JSON array; asText() returns empty string."),
                // whitespace‑preserving text
                Arguments.of("  object  ", TextNode.valueOf("  object  "),
                        "Parent schema's schemaNode contains a \"type\" field with leading/trailing whitespace; whitespace must be preserved."),
                // Unicode character
                Arguments.of("π", TextNode.valueOf("π"),
                        "Parent schema's schemaNode contains a \"type\" field with a Unicode character (Greek pi).")
        );
    }

    /* --------------------------------------------------------------------- *
     *  Exception scenarios (third group of test cases)
     * --------------------------------------------------------------------- */

    @Test
    void getNodeFieldType_throws_NullPointerException_whenParentSchemaIsNull() {
        // Arrange: validator with a null parent schema.
        TestValidator validator = new TestValidator(null);

        // Act & Assert
        assertThrows(NullPointerException.class,
                validator::getNodeFieldType,
                "When parent schema is null, a NullPointerException should be thrown.");
    }

    @Test
    void getNodeFieldType_throws_NullPointerException_whenParentSchemaNodeIsNull() {
        // Arrange
        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.getSchemaNode()).thenReturn(null); // schemaNode is null

        TestValidator validator = new TestValidator(parentSchema);

        // Act & Assert
        assertThrows(NullPointerException.class,
                validator::getNodeFieldType,
                "When parent schema's schemaNode is null, a NullPointerException should be thrown.");
    }

    @Test
    void getNodeFieldType_propagates_RuntimeException_fromSchemaNode() {
        // Arrange
        JsonSchema parentSchema = mock(JsonSchema.class);
        JsonNode parentSchemaNode = mock(JsonNode.class);
        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
        when(parentSchemaNode.get("type"))
                .thenThrow(new RuntimeException("Unexpected failure while accessing node"));

        TestValidator validator = new TestValidator(parentSchema);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                validator::getNodeFieldType,
                "A RuntimeException thrown by the schema node should propagate.");
        assertEquals("Unexpected failure while accessing node", ex.getMessage());
    }
}
```