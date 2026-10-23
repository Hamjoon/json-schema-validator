```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JsonSchema#toString()}.
 *
 * The {@code toString()} implementation simply concatenates the schema path
 * (quoted) with the JSON representation of the underlying schema node.
 *
 * The test creates a {@link JsonSchema} instance via the public static factory
 * method {@code JsonSchema.from(...)} using mocked dependencies. No private
 * members of {@code JsonSchema} or its collaborators are accessed directly.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_toString_Test {

    @Mock
    ValidationContext validationContext;   // required by JsonSchema.from(...)
    @Mock
    JsonMetaSchema metaSchema;            // returned by validationContext.getMetaSchema()
    @Mock
    JsonNode schemaNode;                  // the node whose toString() we control

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Minimal behaviour required by the JsonSchema constructor.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // Returning null for config avoids the creation of KeywordWalkListenerRunner.
        when(validationContext.getConfig()).thenReturn(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /** -----------------------------------------------------------------------
     *  Standard / Edge / Special / Whitespace / Empty / Primitive / Long / Literal
     *  cases – all expected to return a string.
     *  --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] path=''{0}'', node=''{1}''")
    @MethodSource("provideSuccessfulToStringCases")
    void toString_returnsExpectedRepresentation(String schemaPath,
                                                String nodeString,
                                                String expected) {
        // GIVEN – mock the node's toString() if the node is not null
        JsonNode node = schemaNode;
        if (nodeString != null) {
            when(node.toString()).thenReturn(nodeString);
        } else {
            node = null; // simulate a null node (used only for exception test)
        }

        // WHEN – create the JsonSchema instance via the public factory method.
        JsonSchema jsonSchema = JsonSchema.from(
                validationContext,
                schemaPath,
                URI.create("http://example.com/schema.json"),
                node,
                null,               // no parent schema
                false               // do not suppress sub‑schema retrieval
        );

        // THEN – verify the string representation matches the contract.
        assertEquals(expected, jsonSchema.toString(),
                () -> "Unexpected toString() result for path: " + schemaPath);
    }

    /** -----------------------------------------------------------------------
     *  Exception case – null schema node should cause NPE.
     *  --------------------------------------------------------------------- */
    @Test
    void toString_nullSchemaNode_throwsNullPointerException() {
        // GIVEN
        String schemaPath = "/nullNode";
        JsonNode node = null; // explicitly null

        JsonSchema jsonSchema = JsonSchema.from(
                validationContext,
                schemaPath,
                URI.create("http://example.com/schema.json"),
                node,
                null,
                false);

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                jsonSchema::toString,
                "Calling toString() with a null schema node should throw NPE");
    }

    /** -----------------------------------------------------------------------
     *  Method source providing all successful test cases.
     *  --------------------------------------------------------------------- */
    private static Stream<Arguments> provideSuccessfulToStringCases() {
        return Stream.of(
                // ---------- Standard Cases ----------
                Arguments.of("/properties/name",
                        "{\"type\":\"string\"}",
                        "\"/properties/name\" : {\"type\":\"string\"}"),
                Arguments.of("/items",
                        "[{\"type\":\"string\"},{\"type\":\"number\"}]",
                        "\"/items\" : [{\"type\":\"string\"},{\"type\":\"number\"}]"),

                // ---------- Edge Cases ----------
                Arguments.of("", // empty path
                        "{\"type\":\"integer\"}",
                        "\"\" : {\"type\":\"integer\"}"),
                Arguments.of("prop\"Name", // internal double‑quote
                        "{\"type\":\"boolean\"}",
                        "\"prop\"Name\" : {\"type\":\"boolean\"}"),
                Arguments.of(null, // null path
                        "{\"type\":\"string\"}",
                        "\"null\" : {\"type\":\"string\"}"),

                // ---------- Special Characters ----------
                Arguments.of("/unicode/π",
                        "{\"type\":\"number\"}",
                        "\"/unicode/π\" : {\"type\":\"number\"}"),
                Arguments.of("/backslash\\test",
                        "{\"type\":\"null\"}",
                        "\"/backslash\\test\" : {\"type\":\"null\"}"),
                Arguments.of("prop'Name",
                        "{\"type\":\"boolean\"}",
                        "\"prop'Name\" : {\"type\":\"boolean\"}"),

                // ---------- Whitespace and Control Characters ----------
                Arguments.of("   /whitespace   ",
                        "{\"type\":\"string\"}",
                        "\"   /whitespace   \" : {\"type\":\"string\"}"),
                Arguments.of("/new\nline",
                        "{\"type\":\"boolean\"}",
                        "\"/new\nline\" : {\"type\":\"boolean\"}"),
                Arguments.of("/tab\tchar",
                        "{\"type\":\"integer\"}",
                        "\"/tab\tchar\" : {\"type\":\"integer\"}"),

                // ---------- Empty Nodes ----------
                Arguments.of("/emptyObject",
                        "{}",
                        "\"/emptyObject\" : {}"),
                Arguments.of("/emptyArray",
                        "[]",
                        "\"/emptyArray\" : []"),

                // ---------- Primitive Nodes ----------
                Arguments.of("/textNode",
                        "\"simple text\"",
                        "\"/textNode\" : \"simple text\""),
                Arguments.of("/numericNode",
                        "123",
                        "\"/numericNode\" : 123"),
                Arguments.of("/booleanNode",
                        "true",
                        "\"/booleanNode\" : true"),
                Arguments.of("/nullNodeString",
                        "null",
                        "\"/nullNodeString\" : null"),

                // ---------- Long Path ----------
                Arguments.of(generateLongPath(),
                        "{\"type\":\"string\"}",
                        "\"" + generateLongPath() + "\" : {\"type\":\"string\"}"),

                // ---------- Literal \"null\" Path ----------
                Arguments.of("null",
                        "{\"type\":\"string\"}",
                        "\"null\" : {\"type\":\"string\"}")
        );
    }

    /** Helper to generate a path longer than 100 characters. */
    private static String generateLongPath() {
        // 110 characters (including the leading slash)
        return "/" + "a".repeat(109);
    }
}
```