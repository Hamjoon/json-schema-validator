```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getNode_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema objectSchema; // schema node is an ObjectNode
    private JsonSchema arraySchema;  // schema node is an ArrayNode

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* -------------------------------------------------
         * Mock the ValidationContext that JsonSchema needs.
         * Only the methods that are invoked in the constructor
         * are stubbed.
         * ------------------------------------------------- */
        ValidationContext validationContext = mock(ValidationContext.class);
        JsonMetaSchema metaSchema = mock(JsonMetaSchema.class);
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());

        // Minimal SchemaLocation – absolute IRI is not required for these tests
        SchemaLocation schemaLocation = new SchemaLocation(null);

        // Evaluation path – any non‑null value works
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);

        /* -------------------------------------------------
         * Prepare the JSON nodes that act as the “schema”
         * for the getNode method.
         *
         * The object node contains every field that the
         * parameterised tests will look up.
         * ------------------------------------------------- */
        String objectJson = """
                {
                  "simple": "value",
                  "a~b": 1,
                  "c/d": 2,
                  "encoded space": 4,
                  "a/b": 5,
                  "a~1b": 6,
                  "a/b/c": 7,
                  "a/b~c": 8,
                  "no~tilde": 9,
                  "no~tilde space": 10,
                  "x/y/z": 11,
                  "true": 12,
                  "": 13
                }
                """;
        JsonNode objectNode = MAPPER.readTree(objectJson);

        String arrayJson = """
                ["arrayValue0","arrayValue1","arrayValue2"]
                """;
        JsonNode arrayNode = MAPPER.readTree(arrayJson);

        /* -------------------------------------------------
         * Create JsonSchema instances via the package‑private
         * factory method.  Because the test class lives in the
         * same package (com.networknt.schema) we can call it
         * directly.
         * ------------------------------------------------- */
        objectSchema = JsonSchema.from(validationContext,
                schemaLocation,
                evaluationPath,
                objectNode,
                null,
                false);

        arraySchema = JsonSchema.from(validationContext,
                schemaLocation,
                evaluationPath,
                arrayNode,
                null,
                false);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for now
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required
    }

    /* -------------------------------------------------
     *  Parameterised tests for string based look‑ups.
     *  Each entry contains:
     *   - the raw argument passed to getNode
     *   - the expected JSON value (as a Java Object for comparison)
     *   - a description (used only for readability)
     * ------------------------------------------------- */
    private static Stream<Arguments> stringLookupProvider() {
        return Stream.of(
                // Simple property – no special characters
                Arguments.of("simple", "value", "Simple string property"),
                // Escaped tilde (~1 → /)
                Arguments.of("c~1d", 2, "Escaped ~1 becomes '/'"),
                // Escaped tilde zero (~0 → ~)
                Arguments.of("a~0b", 1, "Escaped ~0 becomes '~'"),
                // Percent‑encoded space
                Arguments.of("encoded%20space", 4, "Percent‑encoded space decoded"),
                // Percent‑encoded slash (-> a/b)
                Arguments.of("a%2Fb", 5, "Percent‑encoded '/' decoded"),
                // Combined escaped tilde and percent‑encoding
                Arguments.of("a~1b%2Fc", 7, "First ~1 → '/', then %2F → '/'"),
                // Tilde present but no replacement tokens
                Arguments.of("no~tilde", 9, "Tilde present but nothing to replace"),
                // Empty string property
                Arguments.of("", 13, "Empty string key"),
                // Multiple tilde replacements in one string
                Arguments.of("a~1b~0c", 8, "Both ~1 and ~0 replacements"),
                // Percent decoding produces tilde after decoding (order test)
                Arguments.of("a%7E1b", 6, "Decoded %7E becomes '~' – no further ~ replacement"),
                // Tilde + percent encoding together
                Arguments.of("no~tilde%20space", 10, "Tilde check runs (no change) then %20 → space"),
                // Object with custom toString returning escaped pointer
                Arguments.of(new Object() {
                    @Override
                    public String toString() {
                        return "x~1y%2Fz";
                    }
                }, 11, "Custom object toString with both ~ and %")
        );
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("stringLookupProvider")
    void getNode_stringLookup(Object input, Object expectedValue, String _description) {
        JsonNode result = objectSchema.getNode(input);
        assertNotNull(result, "Result should not be null for key: " + input);
        // Expected values are either String or Number – compare via asText()/asInt()
        if (expectedValue instanceof String) {
            assertEquals(expectedValue, result.asText(),
                    "Unexpected text value for key: " + input);
        } else if (expectedValue instanceof Number) {
            assertEquals(((Number) expectedValue).intValue(), result.asInt(),
                    "Unexpected numeric value for key: " + input);
        } else {
            fail("Unsupported expected value type");
        }
    }

    /* -------------------------------------------------
     *  Numeric index tests (including truncation, negative,
     *  out‑of‑bounds, and double handling)
     * ------------------------------------------------- */
    @Test
    void getNode_numericIndex_zero() {
        JsonNode result = arraySchema.getNode(0);
        assertNotNull(result);
        assertEquals("arrayValue0", result.asText());
    }

    @Test
    void getNode_numericIndex_one() {
        JsonNode result = arraySchema.getNode(1);
        assertNotNull(result);
        assertEquals("arrayValue1", result.asText());
    }

    @Test
    void getNode_numericIndex_doubleTruncation() {
        JsonNode result = arraySchema.getNode(2.9); // intValue() truncates to 2
        assertNotNull(result);
        assertEquals("arrayValue2", result.asText());
    }

    @Test
    void getNode_numericIndex_negative() {
        JsonNode result = arraySchema.getNode(-1);
        assertNull(result, "Negative index should yield null");
    }

    @Test
    void getNode_numericIndex_outOfBounds() {
        JsonNode result = arraySchema.getNode(999);
        assertNull(result, "Out‑of‑bounds index should yield null");
    }

    /* -------------------------------------------------
     *  Non‑Number object whose toString yields a normal key
     * ------------------------------------------------- */
    @Test
    void getNode_nonStringObjectWithToString() {
        JsonNode result = objectSchema.getNode(Boolean.TRUE);
        assertNotNull(result);
        assertEquals(12, result.asInt(), "The key \"true\" should be resolved");
    }

    /* -------------------------------------------------
     *  Null argument – expect NPE because toString() is invoked
     * ------------------------------------------------- */
    @Test
    void getNode_nullArgument_throwsNPE() {
        assertThrows(NullPointerException.class, () -> objectSchema.getNode(null));
    }

    /* -------------------------------------------------
     *  Number implementation that throws from intValue()
     * ------------------------------------------------- */
    @Test
    void getNode_faultyNumber_intValueThrows() {
        Number badNumber = new Number() {
            @Override public int intValue() { throw new IllegalArgumentException("bad int"); }
            @Override public long longValue() { return 0L; }
            @Override public float floatValue() { return 0f; }
            @Override public double doubleValue() { return 0.0; }
        };
        assertThrows(IllegalArgumentException.class, () -> arraySchema.getNode(badNumber));
    }

    /* -------------------------------------------------
     *  Object whose toString() throws an exception
     * ------------------------------------------------- */
    @Test
    void getNode_faultyToString_throwsException() {
        Object badObject = new Object() {
            @Override public String toString() {
                throw new IllegalStateException("toString failed");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> objectSchema.getNode(badObject));
        assertEquals("toString failed", ex.getMessage());
    }

    /* -------------------------------------------------
     *  Null schema node – force getSchemaNode() to return null
     * ------------------------------------------------- */
    @Test
    void getNode_nullSchemaNode_throwsNPE() {
        // Spy the existing schema and stub getSchemaNode() to return null
        JsonSchema spySchema = spy(objectSchema);
        doReturn(null).when(spySchema).getSchemaNode();

        assertThrows(NullPointerException.class, () -> spySchema.getNode("simple"));
    }
}
```