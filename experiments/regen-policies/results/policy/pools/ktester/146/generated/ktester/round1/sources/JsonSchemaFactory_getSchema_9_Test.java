package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_9_Test {

    /**
     * Factory used in all test cases.
     */
    private static JsonSchemaFactory factory;

    /**
     * ObjectMapper for building {@link JsonNode} instances from JSON strings.
     */
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft version – the concrete version does not matter for the
        // behaviour we are testing, it only needs to be a valid one.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        mapper = new ObjectMapper();
    }

    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(// malformed URI
        Arguments.of("ht!tp://[invalid]", JsonNodeFactory.instance.objectNode().put("type", "null"), IllegalArgumentException.class, "Malformed URI"), // unsupported scheme mailto
        Arguments.of("mailto:user@example.com", JsonNodeFactory.instance.objectNode().put("type", "string"), IllegalArgumentException.class, "Unsupported mailto scheme"), // unsupported scheme ftp
        Arguments.of("ftp://example.com/schema.json", JsonNodeFactory.instance.objectNode().put("type", "string"), IllegalArgumentException.class, "Unsupported ftp scheme"), // array node
        Arguments.of("http://example.com/array-schema.json", JsonNodeFactory.instance.arrayNode().add("string"), IllegalArgumentException.class, "Array node as schema"), // primitive text node
        Arguments.of("http://example.com/primitive-schema.json", JsonNodeFactory.instance.textNode("just a string"), IllegalArgumentException.class, "Primitive text node"), // numeric node
        Arguments.of("http://example.com/numeric-schema.json", JsonNodeFactory.instance.numberNode(42), IllegalArgumentException.class, "Numeric primitive node"));
    }

    /**
     * Creates a {@link SchemaLocation} from a string. The constructor used in the
     * library accepts a raw string, so this helper simply forwards the argument.
     */
    private static SchemaLocation newSchemaLocation(String uri) {
        return new SchemaLocation(uri);
    }

    /**
     * Verify that {@code getSchema(SchemaLocation, JsonNode)} returns a {@link JsonSchema}
     * that wraps the supplied schema node.
     */
    @Test
    void getSchema_withValidLocationAndNode_returnsSchemaContainingNode() throws Exception {
        // ---------------------------------------------------------------------
        // given: a simple JSON Schema and a corresponding SchemaLocation
        // ---------------------------------------------------------------------
        String schemaJson = """
            {
              "$id": "http://example.com/simple-schema",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "string"
            }
            """;
        JsonNode schemaNode = mapper.readTree(schemaJson);
        // AbsoluteIri is a public class in the same package; we construct it with the
        // identifier used in the schema above.
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/simple-schema");
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri);
        // ---------------------------------------------------------------------
        // when: the factory creates a JsonSchema from the location and node
        // ---------------------------------------------------------------------
        JsonSchema jsonSchema = factory.getSchema(schemaLocation, schemaNode);
        // ---------------------------------------------------------------------
        // then: the result is non‑null and contains the original schema node
        // ---------------------------------------------------------------------
        assertNotNull(jsonSchema, "The factory must return a JsonSchema instance");
        // JsonSchema exposes its underlying node via getSchemaNode()
        JsonNode returnedNode = jsonSchema.getSchemaNode();
        assertNotNull(returnedNode, "Returned JsonSchema should contain a schema node");
        assertEquals(schemaNode, returnedNode, "The schema node inside JsonSchema must be identical to the node supplied to the factory");
    }

    /**
     * Verify that providing {@code null} as the {@link JsonNode} argument results in an
     * {@link IllegalArgumentException}. The method contract does not allow a null
     * schema node because a schema cannot be created without a document.
     */
    @Test
    void getSchema_withNullNode_throwsIllegalArgumentException() {
        // -----------------------------------------------------------------
        // given: a valid SchemaLocation but a null JsonNode
        // -----------------------------------------------------------------
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/invalid-schema");
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri);
        JsonNode nullNode = null;
        // -----------------------------------------------------------------
        // when / then: the call must fail with IllegalArgumentException
        // -----------------------------------------------------------------
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(schemaLocation, nullNode), "Calling getSchema with a null JsonNode should raise IllegalArgumentException");
    }

    @Test
    void getSchema_withRelativeLocationAndNode_returnsNonNullSchema() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("type", "integer");
        SchemaLocation location = new SchemaLocation("/schemas/local-schema.json");
        JsonSchema schema = factory.getSchema(location, node);
        assertNotNull(schema, "Factory should handle relative URIs and return a JsonSchema");
    }

    @Test
    void getSchema_withEncodedSpaceInUri_returnsNonNullSchema() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("type", "number");
        SchemaLocation location = new SchemaLocation("http://example.com/my%20schema.json");
        JsonSchema schema = factory.getSchema(location, node);
        assertNotNull(schema, "Encoded spaces must be accepted");
    }

    @Test
    void getSchema_withQueryParametersInUri_returnsNonNullSchema() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("type", "boolean");
        SchemaLocation location = new SchemaLocation("http://example.com/schema.json?version=1");
        JsonSchema schema = factory.getSchema(location, node);
        assertNotNull(schema, "Query parameters must be ignored for loading");
    }

    @Test
    void getSchema_withEmptyObjectNode_returnsNonNullSchema() {
        // {}
        JsonNode emptyNode = JsonNodeFactory.instance.objectNode();
        SchemaLocation location = new SchemaLocation("http://example.com/empty.json");
        JsonSchema schema = factory.getSchema(location, emptyNode);
        assertNotNull(schema, "Even an empty object node should produce a JsonSchema");
        assertEquals(emptyNode, schema.getSchemaNode(), "The underlying node must be the empty object we supplied");
    }

    @Test
    void getSchema_withNullSchemaLocation_throwsNullPointerException() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("type", "boolean");
        assertThrows(NullPointerException.class, () -> factory.getSchema(null, node), "A null SchemaLocation must cause NPE");
    }

    @Test
    void getSchema_withNullJsonNode_throwsIllegalArgumentException() {
        SchemaLocation location = new SchemaLocation("http://example.com/invalid-schema");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, null), "null JsonNode should be rejected with IllegalArgumentException");
    }

    @Test
    void getSchema_withBothParametersNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> factory.getSchema(null, null), "Both arguments null must raise NPE before any processing");
    }

    @Test
    void getSchema_withMalformedUri_throwsIllegalArgumentException() {
        SchemaLocation location = new SchemaLocation("ht!tp://[invalid]");
        JsonNode node = JsonNodeFactory.instance.objectNode().put("type", "null");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, node), "Malformed URI should be rejected");
    }

    @Test
    void getSchema_withUnsupportedScheme_mailto_throwsIllegalArgumentException() {
        SchemaLocation location = new SchemaLocation("mailto:user@example.com");
        JsonNode node = JsonNodeFactory.instance.objectNode().put("type", "string");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, node), "Unsupported scheme (mailto) must cause IllegalArgumentException");
    }

    @Test
    void getSchema_withUnsupportedScheme_ftp_throwsIllegalArgumentException() {
        SchemaLocation location = new SchemaLocation("ftp://example.com/schema.json");
        JsonNode node = JsonNodeFactory.instance.objectNode().put("type", "string");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, node), "Unsupported scheme (ftp) must cause IllegalArgumentException");
    }

    @Test
    void getSchema_withArrayNode_throwsIllegalArgumentException() {
        JsonNode arrayNode = JsonNodeFactory.instance.arrayNode().add("string");
        SchemaLocation location = new SchemaLocation("http://example.com/array-schema.json");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, arrayNode), "Array node cannot be used as a schema definition");
    }

    @Test
    void getSchema_withPrimitiveTextNode_throwsIllegalArgumentException() {
        JsonNode textNode = JsonNodeFactory.instance.textNode("just a string");
        SchemaLocation location = new SchemaLocation("http://example.com/primitive-schema.json");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, textNode), "Primitive text node must be rejected");
    }

    @Test
    void getSchema_withNumericNode_throwsIllegalArgumentException() {
        JsonNode numberNode = JsonNodeFactory.instance.numberNode(42);
        SchemaLocation location = new SchemaLocation("http://example.com/numeric-schema.json");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, numberNode), "Numeric node cannot be used as a schema definition");
    }

    @Test
    void getSchema_withNullJsonNodeAndExistingFile_returnsLoadedSchema() throws IOException {
        // Create a temporary file that contains a minimal valid schema.
        String content = """
            {
              "$id": "http://example.com/temp-schema",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "string"
            }
            """;
        Path tempFile = Files.createTempFile("temp-schema", ".json");
        Files.writeString(tempFile, content);
        tempFile.toFile().deleteOnExit();
        // Build a file‑scheme URI that the factory can understand.
        URI fileUri = tempFile.toUri();
        SchemaLocation location = new SchemaLocation(fileUri.toString());
        // jsonNode is null – the factory must read the file.
        JsonSchema schema = factory.getSchema(location, null);
        assertNotNull(schema, "Factory should load schema from file when JsonNode is null");
        assertEquals("string", schema.getSchemaNode().get("type").asText(), "Loaded schema must contain the expected content");
    }

    @Test
    void getSchema_withNonExistentClasspathResource_throwsIllegalArgumentException() {
        // classpath scheme – the resource does not exist.
        SchemaLocation location = new SchemaLocation("classpath:/schemas/missing-schema.json");
        assertThrows(IllegalArgumentException.class, () -> factory.getSchema(location, null), "Missing classpath resource must result in IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("illegalArgumentProvider")
    void getSchema_illegalArguments_throwExpectedException(String uri, JsonNode node, Class<? extends Exception> expected, String description) {
        SchemaLocation location = new SchemaLocation(uri);
        assertThrows(expected, () -> factory.getSchema(location, node));
    }
}
