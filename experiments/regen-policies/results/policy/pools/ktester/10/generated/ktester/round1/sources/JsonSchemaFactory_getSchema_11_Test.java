package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Method;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_11_Test {

    /* ----------------------------------------------------------------------
     *  Helper to obtain a factory instance.  The library ships with a static
     *  factory method that returns a factory pre‑configured for the given
     *  draft version.  All tests use the V4 meta‑schema – it is the most
     *  widely supported version and works for the generic JSON schemas used
     *  in the test data.
     * ---------------------------------------------------------------------- */
    private JsonSchemaFactory factory() {
        return JsonSchemaFactory.getInstance(VersionFlag.V4);
    }

    /* ----------------------------------------------------------------------
     *  Parameter source for the “happy‑path” cases – i.e. those that are
     *  expected to return a non‑null {@link JsonSchema}.  Each argument
     *  consists of:
     *
     *  1. The URI to pass to the method.
     *  2. The JSON node representing the schema.
     *  3. The expected string representation of the {@link SchemaLocation}
     *     stored inside the resulting {@link JsonSchema}.
     * ---------------------------------------------------------------------- */
    private static Stream<Arguments> happyPathProvider() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return Stream.of(// basic functionality – simple object schema
        Arguments.of(URI.create("http://example.com/schema"), mapper.readTree("{\"type\":\"object\"}"), "http://example.com/schema"), // file‑scheme URI
        Arguments.of(URI.create("file:/tmp/my-schema.json"), mapper.readTree("{\"$id\":\"my-schema\",\"type\":\"string\"}"), "file:/tmp/my-schema.json"), // empty URI string
        Arguments.of(URI.create(""), mapper.readTree("{\"type\":\"null\"}"), ""), // encoded fragment
        Arguments.of(URI.create("http://example.com/schema.json#%2Fdefinitions%2Ffoo"), mapper.readTree("{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}"), "http://example.com/schema.json#/definitions/foo"), // relative URI
        Arguments.of(URI.create("schemas/myschema.json"), mapper.readTree("{\"type\":\"string\"}"), "schemas/myschema.json"), // URI with query parameters
        Arguments.of(URI.create("http://example.com/schema.json?v=2&debug=true"), mapper.readTree("{\"type\":\"null\"}"), "http://example.com/schema.json?v=2&debug=true"), // FTP URI
        Arguments.of(URI.create("ftp://example.com/remote-schema.json"), mapper.readTree("{\"type\":\"integer\"}"), "ftp://example.com/remote-schema.json"), // empty object node
        Arguments.of(URI.create("http://example.com/empty-object"), mapper.readTree("{}"), "http://example.com/empty-object"), // array schema
        Arguments.of(URI.create("http://example.com/array-schema"), mapper.readTree("{\"type\":\"array\",\"items\":{\"type\":\"string\"}}"), "http://example.com/array-schema"), // complex schema with $ref
        Arguments.of(URI.create("http://example.com/complex-schema"), mapper.readTree("{\"$id\":\"#/components/schemas/Person\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"address\":{\"$ref\":\"#/components/schemas/Address\"}}}"), "http://example.com/complex-schema"), // primitive JSON node (text node)
        Arguments.of(URI.create("http://example.com/primitive-node"), mapper.readTree("\"just a string\""), "http://example.com/primitive-node"), // relative URI without scheme
        Arguments.of(URI.create("schemas/myschema.json"), mapper.readTree("{\"type\":\"string\"}"), "schemas/myschema.json"));
    }

    /* ----------------------------------------------------------------------
     *  Provider for the cases that are expected to throw a {@link
     *  NullPointerException}.  The first argument is the URI (which may be
     *  {@code null}), the second argument is the JSON node (which may also be
     *  {@code null}) and the third argument is a short description used only
     *  for the test name.
     * ---------------------------------------------------------------------- */
    private static Stream<Arguments> nullPointerProvider() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return Stream.of(Arguments.of(null, mapper.readTree("{\"type\":\"boolean\"}"), "null URI should cause NPE"), Arguments.of(URI.create("http://example.com/invalid"), null, "null JsonNode should cause NPE"));
    }

    /**
     * ----------------------------------------------------------------------
     *  Test case for JsonSchemaFactory#getSchema(URI, JsonNode)
     *
     *  The test creates a minimal JSON schema (a simple “type : string”
     *  definition), obtains a JsonSchemaFactory instance via the public
     *  static factory method, invokes the focal method and asserts that a
     *  non‑null JsonSchema is returned.  The returned schema is expected to
     *  be a concrete implementation of JsonSchema; further behavioural
     *  verification would require knowledge of JsonSchema’s public API,
     *  which is outside the scope of this generic test.
     * ---------------------------------------------------------------------- **
     */
    @Test
    void shouldReturnJsonSchemaWhenGivenValidUriAndJsonNode() throws Exception {
        // given: a simple JSON schema node and a dummy URI
        String schemaString = "{\"type\":\"string\"}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree(schemaString);
        URI schemaUri = URI.create("http://example.com/simple-schema.json");
        // and: a JsonSchemaFactory instance (using the default V4 meta‑schema)
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
        // when: the focal method is invoked
        JsonSchema result = factory.getSchema(schemaUri, schemaNode);
        // then: a non‑null JsonSchema instance is produced
        assertNotNull(result, "The factory should return a non‑null JsonSchema instance");
        // Additional sanity check – the result should be an instance of JsonSchema
        assertTrue(result instanceof JsonSchema, "Result must be a JsonSchema");
    }

    /* ----------------------------------------------------------------------
     *  Happy‑path test – verifies that a non‑null {@link JsonSchema} is
     *  returned, that its stored {@link SchemaLocation} matches the supplied
     *  URI string and that the underlying schema node is identical to the
     *  one supplied to the factory.
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] URI={0}")
    @MethodSource("happyPathProvider")
    void shouldCreateSchemaWithCorrectLocationAndNode(URI schemaUri, JsonNode schemaNode, String expectedLocationString) {
        // given
        JsonSchemaFactory factory = factory();
        // when
        JsonSchema result = factory.getSchema(schemaUri, schemaNode);
        // then
        assertNotNull(result, "Factory must return a non‑null JsonSchema");
        // The public API of JsonSchema provides getSchemaLocation() and getSchemaNode()
        // – use them to verify the internal state.
        assertEquals(expectedLocationString, result.getSchemaLocation().toString(), "SchemaLocation string should reflect the supplied URI");
        assertSame(schemaNode, result.getSchemaNode(), "The JsonSchema should retain the exact JsonNode instance supplied");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullPointerProvider")
    void shouldThrowNPEWhenArgumentsInvalid(URI schemaUri, JsonNode schemaNode, String description) {
        JsonSchemaFactory factory = factory();
        assertThrows(NullPointerException.class, () -> factory.getSchema(schemaUri, schemaNode), description);
    }

    /* ----------------------------------------------------------------------
     *  Immutability test – the factory must not modify the supplied
     *  {@link JsonNode}.  We capture the JSON string representation before the
     *  call and compare it afterwards.
     * ---------------------------------------------------------------------- */
    @Test
    void shouldNotMutateSuppliedJsonNode() throws Exception {
        // given: a mutable ObjectNode
        ObjectMapper mapper = new ObjectMapper();
        JsonNode originalNode = mapper.createObjectNode().set("type", mapper.getNodeFactory().textNode("string"));
        // keep a deep copy of its JSON representation
        String before = originalNode.toString();
        JsonSchemaFactory factory = factory();
        URI uri = URI.create("http://example.com/immutable-test");
        // when
        JsonSchema schema = factory.getSchema(uri, originalNode);
        // then
        assertNotNull(schema, "Result must be non‑null");
        // The original node must be unchanged
        assertEquals(before, originalNode.toString(), "The JsonNode supplied to getSchema must remain unchanged");
    }
}
