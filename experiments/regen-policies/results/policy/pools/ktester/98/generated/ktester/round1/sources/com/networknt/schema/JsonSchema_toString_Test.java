package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_toString_Test {

    private JsonSchema schema;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // a real instance used as the spy base
    private JsonSchema dummySchema;

    /**
     * Simple {@link JsonNode} that throws a supplied exception from {@code toString()}.
     */
//     private static class ExplodingNode extends JsonNode {

        private final RuntimeException exception;

        ExplodingNode(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public JsonToken asToken() {
            return JsonToken.VALUE_STRING;
        }

        @Override
        public JsonNodeType getNodeType() {
            return JsonNodeType.STRING;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Iterator<JsonNode> elements() {
            return Collections.emptyIterator();
        }

        @Override
        public Iterator<String> fieldNames() {
            return Collections.emptyIterator();
        }

        @Override
        public JsonNode get(int index) {
            return null;
        }

        @Override
        public JsonNode get(String fieldName) {
            return null;
        }

        @Override
        public JsonNode path(String fieldName) {
            return MissingNode.getInstance();
        }

        @Override
        public JsonNode path(int index) {
            return MissingNode.getInstance();
        }

        @Override
        public JsonNode deepCopy() {
            return this;
        }

        @Override
        public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString("exploding");
        }

        @Override
        public String toString() {
            throw exception;
        }

        /* JsonNode defines equals() and hashCode() as abstract, we provide trivial implementations. */
        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        /* Additional abstract methods introduced in newer Jackson versions. */
        @Override
        public JsonNode findValue(String fieldName) {
            return null;
        }

        @Override
        public List<JsonNode> findValues(String fieldName, List<JsonNode> found) {
            return found;
        }

        @Override
        public List<String> findValuesAsText(String fieldName, List<String> found) {
            return found;
        }

        @Override
        public JsonNode findPath(String fieldName) {
            return MissingNode.getInstance();
        }

        @Override
        public JsonNode findParent(String fieldName) {
            return null;
        }

        @Override
        public List<JsonNode> findParents(String fieldName, List<JsonNode> found) {
            return found;
        }

//         @Override
//         public JsonNode findValueDescendant(String fieldName) {
//             return null;
//         }

//         @Override
//         public List<JsonNode> findValuesDescendant(String fieldName, List<JsonNode> found) {
//             return found;
//         }

//         @Override
//         public List<String> findValuesDescendantAsText(String fieldName, List<String> found) {
//             return found;
//         }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // will overwrite the fields that produce the output.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.dummySchema = factory.getSchema("{\"type\":\"object\"}");
        // Create a minimal, valid schema – its content is irrelevant because we
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        this.schema = createSchema("$", node);
    }

    /* --------------------------------------------------------------------- */
//     private JsonSchema createSpy(String evaluationPath, JsonNode schemaNode) {
//         JsonSchema spy = spy(dummySchema);
//         // Stub the protected methods used by toString()
//         doReturn(evaluationPath).when(spy).getEvaluationPath();
//         doReturn(schemaNode).when(spy).getSchemaNode();
//         return spy;
//     }

//     private JsonSchema createSpyWithNullPath(JsonNode schemaNode) {
//         JsonSchema spy = spy(dummySchema);
//         doReturn(null).when(spy).getEvaluationPath();
//         doReturn(schemaNode).when(spy).getSchemaNode();
//         return spy;
//     }

//     private JsonSchema createSpyWithException(JsonNode exceptionNode) {
//         JsonSchema spy = spy(dummySchema);
//         doReturn("$").when(spy).getEvaluationPath();
//         doReturn(exceptionNode).when(spy).getSchemaNode();
//         return spy;
//     }

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

    /**
     * Creates a fresh {@link JsonSchema} instance and forces the internal
     * {@code evaluationPath} and {@code schemaNode} fields to the supplied
     * values using reflection.
     */
    private JsonSchema createSchema(String evaluationPath, JsonNode schemaNode) throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema newSchema = factory.getSchema("{\"type\":\"object\"}");
        // set the protected fields via reflection
        Field pathField = JsonSchema.class.getDeclaredField("evaluationPath");
        pathField.setAccessible(true);
        pathField.set(newSchema, evaluationPath);
        Field nodeField = JsonSchema.class.getDeclaredField("schemaNode");
        nodeField.setAccessible(true);
        nodeField.set(newSchema, schemaNode);
        return newSchema;
    }

    private JsonSchema createSchemaWithNullPath(JsonNode schemaNode) throws Exception {
        return createSchema(null, schemaNode);
    }

    private JsonSchema createSchemaWithException(JsonNode exceptionNode) throws Exception {
        return createSchema("$", exceptionNode);
    }

    @Test
    void toStringProducesExpectedRepresentation() throws Exception {
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        String expectedNodeString = node.toString();
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
        JsonSchema schema = createSchema(path, node);
        assertEquals(expected, schema.toString(), "toString() should correctly concatenate evaluation path and node");
    }

    @Test
    void basicConcatenation_NullSchemaNode_throwsNPE() {
        JsonSchema schema = createSchema("$", null);
        assertThrows(NullPointerException.class, schema::toString, "When the underlying schema node is null, toString() must propagate NPE");
    }

    @ParameterizedTest
    @MethodSource("diverseDataTypeProvider")
    void diverseDataTypeHandling(String path, JsonNode node, String expected) {
        JsonSchema schema;
        if (path == null) {
            schema = createSchemaWithNullPath(node);
        } else {
            schema = createSchema(path, node);
        }
        assertEquals(expected, schema.toString(), "toString() must correctly render various JSON node types and path edge‑cases");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void toStringPropagatesIllegalStateExceptionFromNode() {
        JsonNode explodingNode = new ExplodingNode(new IllegalStateException("custom toString failure"));
        JsonSchema schema = createSchemaWithException(explodingNode);
        IllegalStateException ex = assertThrows(IllegalStateException.class, schema::toString, "IllegalStateException from node.toString() must propagate");
        assertEquals("custom toString failure", ex.getMessage());
    }

    @Test
    void toStringPropagatesRuntimeExceptionFromNode() {
        JsonNode explodingNode = new ExplodingNode(new RuntimeException("unexpected error"));
        JsonSchema schema = createSchemaWithException(explodingNode);
        RuntimeException ex = assertThrows(RuntimeException.class, schema::toString, "RuntimeException from node.toString() must propagate");
        assertEquals("unexpected error", ex.getMessage());
    }
}