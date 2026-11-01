package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonSchemaFactory_getSchema_8_Test {

    //     @Mock
    ObjectMapper jsonMapper;

    //     @Mock
    YAMLMapper yamlMapper;

    // Additional objects needed for construction
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema";

    private JsonSchemaFactory factorySpy;

    /**
     * The factory under test – we use the public default instance.
     */
    private JsonSchemaFactory factory;

    //     @BeforeEach
    void setupBeforeEach() {
        // Use the public factory instance – it is fully initialised and does not need
        // any Mockito stubbing.
        factory = JsonSchemaFactory.getInstance();
    }

    /* -----------------------------------------------------------------------
     * Helper to turn a JSON string into a JsonNode.  Using a real ObjectMapper
     * keeps the test independent of the mocked jsonMapper that is only needed
     * for the factory construction.
     * ----------------------------------------------------------------------- */
    private static JsonNode parseJson(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Invalid test JSON", e);
        }
    }

    /* -----------------------------------------------------------------------
     * SUCCESSFUL CASES – the factory should return a non‑null JsonSchema.
     * ----------------------------------------------------------------------- */
    static Stream<Arguments> successCases() {
        return Stream.of(Arguments.of("http JSON schema", "http://example.com/schema.json", "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"), Arguments.of("file URI with YAML‑compatible JSON node", "file:/tmp/local-schema.yaml", "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }"), Arguments.of("empty JSON object", "http://example.com/empty-schema.json", "{}"));
    }

    /* -----------------------------------------------------------------------
     * EXCEPTION CASES – the factory should propagate the exception thrown by
     * newJsonSchema (or by URI construction when the literal is malformed).
     * ----------------------------------------------------------------------- */
    static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null arguments
        Arguments.of("null URI", null, "{ \"type\": \"object\" }", NullPointerException.class), Arguments.of("null JsonNode", "http://example.com/any-schema.json", null, NullPointerException.class), Arguments.of("both null", null, null, NullPointerException.class), // Unsupported / malformed URIs
        Arguments.of("unsupported scheme (ftp)", "ftp://example.com/schema.json", "{ \"type\": \"object\" }", IllegalArgumentException.class), Arguments.of("relative URI", "schemas/schema.json", "{ \"type\": \"object\" }", IllegalArgumentException.class), Arguments.of("data URI scheme", "data:application/json,{\"type\":\"object\"}", "{ \"type\": \"object\" }", IllegalArgumentException.class), // Invalid JSON node shapes
        Arguments.of("JSON array node", "http://example.com/schema.json", "[ { \"type\": \"object\" } ]", IllegalArgumentException.class), Arguments.of("primitive JSON node", "http://example.com/schema.json", "\"string\"", IllegalArgumentException.class), // Invalid schema content (unknown type)
        Arguments.of("unknown type keyword", "http://example.com/schema.json", "{ \"type\": \"unknown\" }", IllegalArgumentException.class));
    }

    @Test
    void getSchema_withUriAndJsonNode_returnsJsonSchema() throws Exception {
        // GIVEN
        URI schemaUri = new URI("http://example.com/my-schema");
        JsonNode schemaNode = parseJson("{ \"type\": \"object\" }");
        // WHEN
        JsonSchema actual = factory.getSchema(schemaUri, schemaNode);
        // THEN
        assertNotNull(actual, "The returned JsonSchema should not be null");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successCases")
    void getSchema_successful(String caseName, String uriString, String json) throws Exception {
        // GIVEN
        URI schemaUri = new URI(uriString);
        JsonNode schemaNode = parseJson(json);
        // WHEN
        JsonSchema result = factory.getSchema(schemaUri, schemaNode);
        // THEN
        assertNotNull(result, "Factory should return a non‑null JsonSchema for case: " + caseName);
    }

//     @ParameterizedTest(name = "[{index}] {0}")
//     @MethodSource("exceptionCases")
//     void getSchema_exceptions(String caseName, String uriString, String json, Class<? extends Throwable> expectedException) throws Exception {
//         // -------------------------------------------------
//         // Build parameters – handling nulls and malformed URIs
//         // -------------------------------------------------
//         URI schemaUri = null;
//         JsonNode schemaNode = null;
//         if (uriString != null) {
//             try {
//                 schemaUri = new URI(uriString);
//             } catch (Exception e) {
//                 // If the URI literal itself is malformed we expect the test
//                 // data to map to IllegalArgumentException.
//                 assertTrue(IllegalArgumentException.class.isAssignableFrom(expectedException), "Test data inconsistency: malformed URI should map to IllegalArgumentException");
//                 assertThrows(expectedException, () -> factory.getSchema(new URI(uriString), parseJson("{\"type\":\"object\"}")));
//                 return;
//             }
//         }
//         if (json != null) {
//             schemaNode = parseJson(json);
//         }
//         // -------------------------------------------------
//         // Exercise the method under test and verify the exception.
//         // -------------------------------------------------
//         assertThrows(expectedException, () -> factory.getSchema(schemaUri, schemaNode), () -> "Expected " + expectedException.getSimpleName() + " for case: " + caseName);
//     }
}