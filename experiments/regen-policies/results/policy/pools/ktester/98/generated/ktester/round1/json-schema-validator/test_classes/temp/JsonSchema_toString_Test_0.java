package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchema_toString_Test {

    private JsonSchema schema;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // a real instance used as the spy base
    private JsonSchema dummySchema;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create a minimal, valid schema – its content is irrelevant because we
        // will stub the methods that produce the output.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.dummySchema = factory.getSchema("{\"type\":\"object\"}");
    }

    /* --------------------------------------------------------------------- */
    private JsonSchema createSpy(String evaluationPath, JsonNode schemaNode) {
        JsonSchema spy = spy(dummySchema);
        // Stub the protected methods used by toString()
        doReturn(evaluationPath).when(spy).getEvaluationPath();
        doReturn(schemaNode).when(spy).getSchemaNode();
        return spy;
    }

    private JsonSchema createSpyWithNullPath(JsonNode schemaNode) {
        JsonSchema spy = spy(dummySchema);
        doReturn(null).when(spy).getEvaluationPath();
        doReturn(schemaNode).when(spy).getSchemaNode();
        return spy;
    }

    private JsonSchema createSpyWithException(JsonNode exceptionNode) {
        JsonSchema spy = spy(dummySchema);
        doReturn("$").when(spy).getEvaluationPath();
        doReturn(exceptionNode).when(spy).getSchemaNode();
        return spy;
    }

    /* --------------------------------------------------------------------- */
    static Stream<Arguments> basicConcatenationProvider() {
        return Stream.of(// typical object node
        Arguments.of("/properties/name", MAPPER.createObjectNode().put("type", "string"), "\"/properties/name\" : {\"type\":\"string\"}"), // empty path with boolean node
        Arguments.of("", BooleanNode.TRUE, "\"\" : true"), // special characters in path with array node
        Arguments.of("/items[0]/$ref", MAPPER.createArrayNode().add(1).add(2).add(3), "\"/items[0]/$ref\" : [1,2,3]"));
    }

    /* --------------------------------------------------------------------- */
    static Stream<Arguments> diverseDataTypeProvider() {
        return Stream.of(// TextNode with escaped quotes
        Arguments.of("/description", new TextNode("A \"quoted\" description"), "\"/description\" : \"A \\\"quoted\\\" description\""), // Decimal numeric node
        Arguments.of("/price", new DoubleNode(19.99), "\"/price\" : 19.99"), // NullNode
        Arguments.of("/nullNode", NullNode.instance, "\"/nullNode\" : null"), // Unicode character in path
        Arguments.of("/unicode/π", MAPPER.createObjectNode().put("type", "number"), "\"/unicode/π\" : {\"type\":\"number\"}"), // Path with spaces and BooleanNode
        Arguments.of("/space path/with spaces", BooleanNode.TRUE, "\"/space path/with spaces\" : true"), // Path is null (Java null) with numeric node
        Arguments.of(null, new IntNode(42), "\"null\" : 42"), // Backslash in path
        Arguments.of("/escaped\\backslash", BooleanNode.FALSE, "\"/escaped\\\\backslash\" : false"));
    }

    @Test
    void toStringProducesExpectedRepresentation() throws Exception {
        // Expected node string (Jackson's default compact representation)
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        String expectedNodeString = node.toString();
        // The evaluation path for a root schema is "$"
        String expected = "\"$\" : " + expectedNodeString;
        String actual = schema.toString();
        assertEquals(expected, actual, "JsonSchema.toString() should combine the evaluation path and the schema node");
    }

    @Test
    void toStringContainsSchemaNodeWhenSchemaIsComplex() throws Exception {
        String complexSchemaJson = "{" + "\"type\":\"object\"," + "\"properties\":{" + "   \"name\":{\"type\":\"string\"}," + "   \"age\":{\"type\":\"integer\"}" + "}," + "\"required\":[\"name\"]" + "}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema complexSchema = factory.getSchema(complexSchemaJson);
        JsonNode node = MAPPER.readTree(complexSchemaJson);
        String expected = "\"$\" : " + node.toString();
        assertEquals(expected, complexSchema.toString(), "toString() must reflect the full JSON representation of the schema node");
    }

    @ParameterizedTest
    @MethodSource("basicConcatenationProvider")
    void basicConcatenationScenarios(String path, JsonNode node, String expected) {
        JsonSchema schema = createSpy(path, node);
        assertEquals(expected, schema.toString(), "toString() should correctly concatenate evaluation path and node");
    }

    @Test
    void basicConcatenation_NullSchemaNode_throwsNPE() {
        JsonSchema schema = createSpy("$", null);
        assertThrows(NullPointerException.class, schema::toString, "When the underlying schema node is null, toString() must propagate NPE");
    }

    @ParameterizedTest
    @MethodSource("diverseDataTypeProvider")
    void diverseDataTypeHandling(String path, JsonNode node, String expected) {
        JsonSchema schema;
        if (path == null) {
            schema = createSpyWithNullPath(node);
        } else {
            schema = createSpy(path, node);
        }
        assertEquals(expected, schema.toString(), "toString() must correctly render various JSON node types and path edge‑cases");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toStringPropagatesIllegalStateExceptionFromNode() {
        JsonNode explodingNode = mock(JsonNode.class);
        when(explodingNode.toString()).thenThrow(new IllegalStateException("custom toString failure"));
        JsonSchema schema = createSpyWithException(explodingNode);
        IllegalStateException ex = assertThrows(IllegalStateException.class, schema::toString, "IllegalStateException from node.toString() must propagate");
        assertEquals("custom toString failure", ex.getMessage());
    }

    @Test
    void toStringPropagatesRuntimeExceptionFromNode() {
        JsonNode explodingNode = mock(JsonNode.class);
        when(explodingNode.toString()).thenThrow(new RuntimeException("unexpected error"));
        JsonSchema schema = createSpyWithException(explodingNode);
        RuntimeException ex = assertThrows(RuntimeException.class, schema::toString, "RuntimeException from node.toString() must propagate");
        assertEquals("unexpected error", ex.getMessage());
    }
}
