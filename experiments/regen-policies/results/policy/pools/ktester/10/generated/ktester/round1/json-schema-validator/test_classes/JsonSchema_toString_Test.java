package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_toString_Test {

    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    @BeforeEach
    void setupBeforeEach() {
        // Stub the behaviour needed for the toString() method
        // evaluationPath is rendered inside quotes, we control its toString()
        when(evaluationPath.toString()).thenReturn("$");
        // schemaNode is rendered as its JSON representation
        when(schemaNode.toString()).thenReturn("{\"type\":\"string\"}");
    }

    /**
     * Creates a {@link JsonSchema} instance via the package‑private factory
     * and returns a Mockito spy so that {@code getEvaluationPath()} and
     * {@code getSchemaNode()} can be stubbed per test case.
     */
    private JsonSchema createSpy(JsonNodePath path, JsonNode node) {
        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, path, node, // no parent
        null, // suppressSubSchemaRetrieval
        false);
        return spy(schema);
    }

    @Test
    void toString_returnsExpectedRepresentation() {
        // JsonSchema has a package‑private static factory method `from`
        // which is accessible because this test resides in the same package.
        JsonSchema jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, // no parent schema
        null, // suppressSubSchemaRetrieval
        false);
        String expected = "\"$\" : {\"type\":\"string\"}";
        assertEquals(expected, jsonSchema.toString(), "JsonSchema.toString() should format the evaluation path and schema node correctly");
    }

    @Test
    @DisplayName("Normal operation – non‑null path and node")
    void toString_normalOperation() {
        // Arrange
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.toString()).thenReturn("/properties/name");
        JsonNode node = mock(JsonNode.class);
        when(node.toString()).thenReturn("{\"type\":\"string\"}");
        JsonSchema jsonSchema = createSpy(path, node);
        // Act
        String result = jsonSchema.toString();
        // Assert
        assertEquals("\"/properties/name\" : {\"type\":\"string\"}", result, "toString() should concatenate quoted path and node JSON correctly");
    }

    @Test
    @DisplayName("Null evaluation path – should render literal \"null\"")
    void toString_nullEvaluationPath() {
        // Arrange
        JsonNodePath path = mock(JsonNodePath.class);
        // simulate null path
        when(path.toString()).thenReturn(null);
        JsonNode node = mock(JsonNode.class);
        when(node.toString()).thenReturn("{\"type\":\"integer\"}");
        JsonSchema jsonSchema = createSpy(path, node);
        // Act
        String result = jsonSchema.toString();
        // Assert
        assertEquals("\"null\" : {\"type\":\"integer\"}", result, "When the evaluation path is null, the string \"null\" should appear in quotes");
    }

    @Test
    @DisplayName("Null schema node – should throw NullPointerException")
    void toString_nullSchemaNode() {
        // Arrange
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.toString()).thenReturn("/some/path");
        // schemaNode = null
        JsonSchema jsonSchema = createSpy(path, null);
        // Act & Assert
        assertThrows(NullPointerException.class, jsonSchema::toString, "Calling toString() with a null schema node must throw NPE");
    }

    @Test
    @DisplayName("Both evaluation path and schema node null – NPE from node")
    void toString_bothNull() {
        // Arrange
        // both null
        JsonSchema jsonSchema = createSpy(null, null);
        // Act & Assert
        assertThrows(NullPointerException.class, jsonSchema::toString, "When both values are null, a NullPointerException is expected");
    }

    @Test
    @DisplayName("Empty evaluation path – should produce empty quotes")
    void toString_emptyEvaluationPath() {
        // Arrange
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.toString()).thenReturn("");
        JsonNode node = mock(JsonNode.class);
        when(node.toString()).thenReturn("{\"type\":\"boolean\"}");
        JsonSchema jsonSchema = createSpy(path, node);
        // Act
        String result = jsonSchema.toString();
        // Assert
        assertEquals("\"\" : {\"type\":\"boolean\"}", result, "Empty evaluation path must be represented as empty quotes");
    }

    @Test
    @DisplayName("Whitespace evaluation path – spaces must be preserved")
    void toString_whitespaceEvaluationPath() {
        // Arrange
        JsonNodePath path = mock(JsonNodePath.class);
        // two spaces
        when(path.toString()).thenReturn("  ");
        JsonNode node = mock(JsonNode.class);
        when(node.toString()).thenReturn("{\"type\":\"string\"}");
        JsonSchema jsonSchema = createSpy(path, node);
        // Act
        String result = jsonSchema.toString();
        // Assert
        assertEquals("\"  \" : {\"type\":\"string\"}", result, "Whitespace inside the path should be kept unchanged");
    }

    @Test
    @DisplayName("Special characters in path – should appear unchanged")
    void toString_specialCharactersInPath() {
        // Arrange
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.toString()).thenReturn("/properties/name with space");
        JsonNode node = mock(JsonNode.class);
        when(node.toString()).thenReturn("{\"type\":\"string\"}");
        JsonSchema jsonSchema = createSpy(path, node);
        // Act
        String result = jsonSchema.toString();
        // Assert
        assertEquals("\"/properties/name with space\" : {\"type\":\"string\"}", result, "Special characters (including spaces) must be preserved");
    }

    @Test
    @DisplayName("Complex schema node – array representation")
    void toString_complexSchemaNode() {
        // Arrange
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.toString()).thenReturn("/items");
        JsonNode node = mock(JsonNode.class);
        when(node.toString()).thenReturn("[{\"type\":\"string\"},{\"type\":\"number\"}]");
        JsonSchema jsonSchema = createSpy(path, node);
        // Act
        String result = jsonSchema.toString();
        // Assert
        assertEquals("\"/items\" : [{\"type\":\"string\"},{\"type\":\"number\"}]", result, "Array schema node must be concatenated without alteration");
    }

    @Test
    @DisplayName("Repeated calls produce identical strings")
    void toString_repeatedCalls() {
        // Arrange
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.toString()).thenReturn("/repeat");
        JsonNode node = mock(JsonNode.class);
        when(node.toString()).thenReturn("{\"type\":\"string\"}");
        JsonSchema jsonSchema = createSpy(path, node);
        // Act
        String first = jsonSchema.toString();
        String second = jsonSchema.toString();
        // Assert
        assertEquals(first, second, "Multiple invocations must return the same string");
    }

    @Test
    @DisplayName("Schema node toString throws – exception propagates")
    void toString_schemaNodeToStringThrows() {
        // Arrange
        JsonNodePath path = mock(JsonNodePath.class);
        when(path.toString()).thenReturn("/bad");
        JsonNode node = mock(JsonNode.class);
        when(node.toString()).thenThrow(new IllegalStateException("bad node"));
        JsonSchema jsonSchema = createSpy(path, node);
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, jsonSchema::toString, "Exception from schemaNode.toString() must propagate");
        assertEquals("bad node", ex.getMessage());
    }
}
