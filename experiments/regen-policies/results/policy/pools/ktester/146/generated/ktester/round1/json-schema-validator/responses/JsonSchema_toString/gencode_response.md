```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import org.mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link JsonSchema#toString()} covering normal, edge‑case and
 * exceptional behaviours.
 *
 * The tests use a real {@link JsonSchemaFactory} for normal scenarios and
 * Mockito spies/mocks for cases where we need to force {@code getEvaluationPath()}
 * or {@code getSchemaNode()} to return {@code null} or throw.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_toString_Test {

    private static JsonSchemaFactory factory;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft – the exact version does not affect toString().
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /* --------------------------------------------------------------
     *  Normal / standard behaviour
     * -------------------------------------------------------------- */

    @ParameterizedTest(name = "{index} => path=''{0}'', node=''{1}'', expected=''{2}''")
    @MethodSource("standardBehaviourProvider")
    void toString_standardBehaviour(String evaluationPath, String nodeJson, String expected) throws Exception {
        // GIVEN – a JsonNode representing the schema fragment
        JsonNode schemaNode = MAPPER.readTree(nodeJson);

        // Create a real JsonSchema (root) and spy it so we can stub the
        // evaluation path without affecting the internal state.
        JsonSchema real = factory.getSchema(schemaNode);
        JsonSchema spy = Mockito.spy(real);

        // Stub getEvaluationPath() to return the required path (may be empty)
        doReturn(evaluationPath).when(spy).getEvaluationPath();

        // WHEN – call toString()
        String result = spy.toString();

        // THEN – the format must be exactly "<path>" : <nodeString>
        assertEquals(expected, result);
    }

    private static Stream<Arguments> standardBehaviourProvider() {
        return Stream.of(
                // root – empty path
                Arguments.of("", "{\"type\":\"string\"}",
                        "\"\" : {\"type\":\"string\"}"),
                // nested property
                Arguments.of("/properties/name",
                        "{\"type\":\"string\",\"minLength\":1}",
                        "\"/properties/name\" : {\"type\":\"string\",\"minLength\":1}"),
                // array index syntax
                Arguments.of("/items[0]",
                        "[1,2,3]",
                        "\"/items[0]\" : [1,2,3]"),
                // numeric node
                Arguments.of("/array[10]/item",
                        "42",
                        "\"/array[10]/item\" : 42"),
                // unicode character in path
                Arguments.of("/unicode/☃",
                        "true",
                        "\"/unicode/☃\" : true"),
                // empty object node
                Arguments.of("", "{}", "\"\" : {}"),
                // empty array node
                Arguments.of("/emptyArray", "[]", "\"/emptyArray\" : []"),
                // nested object node
                Arguments.of("/nested/object",
                        "{\"inner\":{\"type\":\"integer\"}}",
                        "\"/nested/object\" : {\"inner\":{\"type\":\"integer\"}}"),
                // path containing a double‑quote (needs escaping in Java string)
                Arguments.of("/quote\"test",
                        "\"value\"",
                        "\"/quote\\\"test\" : \"value\""),
                // path with a space and a null node
                Arguments.of("/space test",
                        "null",
                        "\"/space test\" : null"),
                // very long path
                Arguments.of("/largePath/segment1/segment2/segment3/segment4",
                        "{\"type\":\"object\"}",
                        "\"/largePath/segment1/segment2/segment3/segment4\" : {\"type\":\"object\"}")
        );
    }

    /* --------------------------------------------------------------
     *  Edge cases where evaluation path or schema node is null
     * -------------------------------------------------------------- */

    @Test
    void toString_nullEvaluationPath() throws Exception {
        JsonNode node = MAPPER.readTree("{\"type\":\"null\"}");
        JsonSchema real = factory.getSchema(node);
        JsonSchema spy = Mockito.spy(real);
        doReturn(null).when(spy).getEvaluationPath(); // path is null

        // Expected: the literal string "null" (because of string concatenation)
        String expected = "\"null\" : " + node.toString();
        assertEquals(expected, spy.toString());
    }

    @Test
    void toString_nullSchemaNode_throwsNPE() throws Exception {
        JsonSchema real = factory.getSchema(MAPPER.readTree("{\"type\":\"string\"}"));
        JsonSchema spy = Mockito.spy(real);
        doReturn("/some/path").when(spy).getEvaluationPath();
        doReturn(null).when(spy).getSchemaNode(); // schema node is null

        assertThrows(NullPointerException.class, spy::toString);
    }

    @Test
    void toString_bothNull_throwsNPE() throws Exception {
        JsonSchema real = factory.getSchema(MAPPER.readTree("{\"type\":\"string\"}"));
        JsonSchema spy = Mockito.spy(real);
        doReturn(null).when(spy).getEvaluationPath();
        doReturn(null).when(spy).getSchemaNode();

        assertThrows(NullPointerException.class, spy::toString);
    }

    /* --------------------------------------------------------------
     *  Exceptions coming from the getters themselves
     * -------------------------------------------------------------- */

    @Test
    void toString_evaluationPathThrowsIllegalStateException() throws Exception {
        JsonSchema real = factory.getSchema(MAPPER.readTree("{\"type\":\"string\"}"));
        JsonSchema spy = Mockito.spy(real);
        doThrow(new IllegalStateException("forced")).when(spy).getEvaluationPath();

        assertThrows(IllegalStateException.class, spy::toString);
    }

    @Test
    void toString_evaluationPathThrowsNullPointerException() throws Exception {
        JsonSchema real = factory.getSchema(MAPPER.readTree("{\"type\":\"string\"}"));
        JsonSchema spy = Mockito.spy(real);
        doThrow(new NullPointerException("forced")).when(spy).getEvaluationPath();

        assertThrows(NullPointerException.class, spy::toString);
    }

    @Test
    void toString_schemaNodeThrowsIllegalArgumentException() throws Exception {
        JsonSchema real = factory.getSchema(MAPPER.readTree("{\"type\":\"string\"}"));
        JsonSchema spy = Mockito.spy(real);
        doThrow(new IllegalArgumentException("forced")).when(spy).getSchemaNode();

        assertThrows(IllegalArgumentException.class, spy::toString);
    }

    /* --------------------------------------------------------------
     *  Exceptions coming from JsonNode.toString()
     * -------------------------------------------------------------- */

    @Test
    void toString_schemaNodeToStringThrowsRuntimeException() throws Exception {
        JsonNode badNode = mock(JsonNode.class);
        when(badNode.toString()).thenThrow(new RuntimeException("node toString failure"));

        JsonSchema real = factory.getSchema(MAPPER.readTree("{\"type\":\"string\"}"));
        JsonSchema spy = Mockito.spy(real);
        doReturn("/badNode").when(spy).getEvaluationPath();
        doReturn(badNode).when(spy).getSchemaNode();

        assertThrows(RuntimeException.class, spy::toString);
    }

    @Test
    void toString_schemaNodeToStringThrowsUnsupportedOperationException() throws Exception {
        JsonNode badNode = mock(JsonNode.class);
        when(badNode.toString()).thenThrow(new UnsupportedOperationException("unsupported"));

        JsonSchema real = factory.getSchema(MAPPER.readTree("{\"type\":\"string\"}"));
        JsonSchema spy = Mockito.spy(real);
        doReturn("/unsupported").when(spy).getEvaluationPath();
        doReturn(badNode).when(spy).getSchemaNode();

        assertThrows(UnsupportedOperationException.class, spy::toString);
    }

    /* --------------------------------------------------------------
     *  Original simple test kept for completeness
     * -------------------------------------------------------------- */

    @Test
    void toString_returnsEvaluationPathAndSchemaNode() throws Exception {
        String schemaJson = "{\"type\":\"string\"}";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        JsonSchema schema = factory.getSchema(schemaNode);

        String result = schema.toString();
        String expected = "\"\" : " + schemaNode.toString();
        assertEquals(expected, result);
    }
}
```