package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_12_Test {

    /**
     * Factory under test – created once per test method.
     */
    private JsonSchemaFactory factory;

    /**
     * Jackson mapper used to create a {@link JsonNode} representing a simple schema.
     */
    private ObjectMapper objectMapper;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a mapper that will be used to build the JsonNode argument.
        objectMapper = new ObjectMapper();
        // Obtain a factory instance for the 2020‑12 draft.  The builder lambda is empty
        // because the default configuration (including the default meta‑schema) is sufficient
        // for the simple schema used in the tests.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> {
            // No customisation required for this test suite.
        });
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test – the factory and mapper are lightweight.
        factory = null;
        objectMapper = null;
    }

    /**
     * Provides a stream of valid URI / JSON schema pairs.
     * Each entry corresponds to one of the groups described in the JSON test
     * specification (normal flow, relative URI, fragment/query, etc.).
     */
    private static Stream<Arguments> validInputsProvider() {
        return Stream.of(// Normal absolute URI
        Arguments.of("http://example.com/schema.json", "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"), // File scheme URI
        Arguments.of("file:///tmp/my-schema.yaml", "{}"), // Relative URI (no scheme)
        Arguments.of("schemas/myschema.json", "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}"), // URI with query and fragment
        Arguments.of("http://example.com/schema.json?version=1#section2", "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}"), // Complex nested schema
        Arguments.of("https://example.org/complex-schema.json", "{\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"address\": {\n" + "      \"type\": \"object\",\n" + "      \"properties\": {\n" + "        \"street\": {\"type\": \"string\"},\n" + "        \"city\": {\"type\": \"string\"}\n" + "      },\n" + "      \"required\": [\"street\", \"city\"]\n" + "    },\n" + "    \"phones\": {\n" + "      \"type\": \"array\",\n" + "      \"items\": {\"type\": \"string\", \"pattern\": \"^\\\\+?[0-9]+$\"}\n" + "    }\n" + "  },\n" + "  \"required\": [\"address\"]\n" + "}"), // Array schema
        Arguments.of("http://example.com/array-schema.json", "{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}"), // Primitive string schema
        Arguments.of("http://example.com/string-schema.json", "{\"type\":\"string\",\"maxLength\":10}"), // Unsupported scheme (classpath)
        Arguments.of("classpath:/schemas/internal-schema.json", "{\"type\":\"object\",\"properties\":{\"flag\":{\"type\":\"boolean\"}}}"), // Empty string URI
        Arguments.of("", "{\"type\":\"object\"}"), // Percent‑encoded characters
        Arguments.of("http://example.com/schemas/%E2%9C%93-schema.json", "{\"type\":\"object\"}"));
    }

    /**
     * Verifies that {@code getSchema(URI, JsonNode)} returns a {@link JsonSchema}
     * that contains the supplied schema node and that its location matches the
     * supplied URI.
     */
    @Test
    void getSchema_withUriAndNode_returnsExpectedJsonSchema() throws Exception {
        // GIVEN – a simple JSON schema and a URI identifying it
        String schemaJson = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonNode schemaNode = objectMapper.readTree(schemaJson);
        URI schemaUri = new URI("http://example.com/schemas/person");
        // WHEN – the factory creates a JsonSchema from the URI and node
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode);
        // THEN – the result must be non‑null and reflect the inputs
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema instance");
        // The schema node stored inside the JsonSchema should be equal to the one we supplied.
        // JsonSchema provides a public accessor {@code getSchemaNode()}.
        JsonNode returnedNode = jsonSchema.getSchemaNode();
        assertNotNull(returnedNode, "JsonSchema#getSchemaNode() must not return null");
        assertEquals(schemaNode, returnedNode, "The schema node inside JsonSchema must match the supplied JsonNode");
        // The location stored in the JsonSchema must correspond to the supplied URI.
        // JsonSchema provides a public accessor {@code getSchemaLocation()}.
        SchemaLocation location = jsonSchema.getSchemaLocation();
        assertNotNull(location, "JsonSchema#getSchemaLocation() must not return null");
        assertEquals(schemaUri.toString(), location.getLocation(), "SchemaLocation should match the supplied URI string");
    }

    /**
     * Ensures that calling {@code getSchema} with a {@code null} URI results in a
     * {@link NullPointerException}.  The method does not perform an explicit null
     * check; the exception is expected to be thrown when the URI is dereferenced
     * inside the implementation.
     */
    @Test
    void getSchema_withNullUri_throwsNullPointerException() throws Exception {
        // GIVEN – a valid schema node but a null URI
        JsonNode schemaNode = objectMapper.readTree("{\"type\":\"string\"}");
        URI nullUri = null;
        // WHEN / THEN – a NullPointerException is expected
        assertThrows(NullPointerException.class, () -> factory.getSchema(nullUri, schemaNode), "Calling getSchema with a null URI should throw NullPointerException");
    }

    /**
     * Ensures that calling {@code getSchema} with a {@code null} JsonNode results in a
     * {@link NullPointerException}.  The implementation will attempt to read the node,
     * which leads to an NPE.
     */
    @Test
    void getSchema_withNullNode_throwsNullPointerException() {
        // This test reflects the historic behaviour where a null node caused NPE.
        // It is kept for backward compatibility; the newer test `getSchema_withNullNode_returnsSchemaWithNullRoot`
        // documents the intended contract.
        URI schemaUri = URI.create("http://example.com/schema");
        assertThrows(NullPointerException.class, () -> factory.getSchema(schemaUri, null), "Calling getSchema with a null JsonNode should throw NullPointerException (legacy expectation)");
    }

    @ParameterizedTest(name = "[{index}] URI=''{0}''")
    @MethodSource("validInputsProvider")
    void getSchema_withVariousValidUris_returnsExpectedJsonSchema(String uriString, String jsonString) throws IOException {
        // GIVEN
        JsonNode schemaNode = objectMapper.readTree(jsonString);
        // URI.create throws IllegalArgumentException for malformed URIs,
        URI schemaUri = URI.create(uriString);
        // which is not the case for the values supplied here.
        // WHEN
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode);
        // THEN
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema");
        // Verify the node stored inside the JsonSchema equals the one we supplied.
        JsonNode returnedNode = jsonSchema.getSchemaNode();
        assertNotNull(returnedNode, "JsonSchema#getSchemaNode() must not be null");
        assertEquals(schemaNode, returnedNode, "The schema node inside JsonSchema must match the supplied JsonNode");
        // Verify the location string matches exactly the supplied URI string.
        SchemaLocation location = jsonSchema.getSchemaLocation();
        assertNotNull(location, "JsonSchema#getSchemaLocation() must not be null");
        assertEquals(uriString, location.getLocation(), "SchemaLocation should exactly match the supplied URI string");
    }

    @Test
    void getSchema_withNullNode_returnsSchemaWithNullRoot() throws IOException {
        URI schemaUri = URI.create("http://example.com/schema");
        // According to the specification, a null JsonNode is allowed.
        JsonSchema jsonSchema = factory.getSchema(schemaUri, null);
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema even when the node is null");
        assertNull(jsonSchema.getSchemaNode(), "JsonSchema#getSchemaNode() should return null when a null JsonNode was supplied");
        SchemaLocation location = jsonSchema.getSchemaLocation();
        assertNotNull(location);
        assertEquals(schemaUri.toString(), location.getLocation(), "SchemaLocation must still reflect the supplied URI");
    }

    @Test
    void getSchema_withMalformedUri_propagatesException() throws IOException {
        // The string is not a valid URI – URI.create will throw IllegalArgumentException.
        String badUri = "ht!tp://[invalid]";
        JsonNode schemaNode = objectMapper.readTree("{\"type\":\"integer\"}");
        assertThrows(IllegalArgumentException.class, () -> {
            URI uri = URI.create(badUri);
            factory.getSchema(uri, schemaNode);
        }, "A malformed URI should cause an IllegalArgumentException (or URISyntaxException) to be thrown");
    }

    @Test
    void getSchema_withMissingNode_returnsSchemaContainingMissingNode() throws IOException {
        URI schemaUri = URI.create("http://example.com/missing-node.json");
        // Jackson's representation of a missing node
        JsonNode missingNode = MissingNode.getInstance();
        JsonSchema jsonSchema = factory.getSchema(schemaUri, missingNode);
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema");
        assertTrue(jsonSchema.getSchemaNode() instanceof MissingNode, "The schema node inside JsonSchema should be a MissingNode");
        SchemaLocation location = jsonSchema.getSchemaLocation();
        assertEquals(schemaUri.toString(), location.getLocation(), "SchemaLocation must match the supplied URI");
    }

    @Test
    void getSchema_usesDefaultSchemaValidatorsConfig() throws IOException {
        // The default config does NOT enable the "email" format validator.
        // We create a schema that uses the "email" format and verify that validation
        // does NOT automatically succeed (i.e., the format validator is absent).
        String schemaJson = "{ \"type\": \"string\", \"format\": \"email\" }";
        JsonNode schemaNode = objectMapper.readTree(schemaJson);
        URI schemaUri = URI.create("http://example.com/email-schema.json");
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode);
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema");
        // Validate a non‑email value – it should fail because the default config
        // does not have a custom email validator that would otherwise accept it.
        JsonNode instance = objectMapper.readTree("\"not-an-email\"");
        Set<ValidationMessage> messages = jsonSchema.validate(instance);
        assertFalse(messages.isEmpty(), "With the default SchemaValidatorsConfig the email format should be validated and thus reject a non‑email string");
    }
}
