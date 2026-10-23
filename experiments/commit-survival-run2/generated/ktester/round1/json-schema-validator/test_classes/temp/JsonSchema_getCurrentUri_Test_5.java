package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getCurrentUri_Test {

    // -------------------------------------------------------------------------
    @Mock
    static URIFactory mockUriFactory;

    @Mock
    static URNFactory mockUrnFactory;

    @Mock
    static JsonMetaSchema mockMetaSchema;

    @Mock
    static JsonSchemaFactory mockJsonSchemaFactory;

    @Mock
    static SchemaValidatorsConfig mockConfig;

    private static ValidationContext validationContext;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // implementations that are part of the json-schema-validator library.
    private static URIFactory uriFactory;

    private static URNFactory urnFactory;

    private static JsonMetaSchema metaSchema;

    private static JsonSchemaFactory jsonSchemaFactory;

    private static SchemaValidatorsConfig config;

    /**
     * Simple {@link URIFactory} implementation used in the tests.
     */
    private static class DummyURIFactory implements URIFactory {

        /**
         * Creates a URI from a plain string.
         */
        @Override
        public URI create(String uri) {
            return URI.create(uri);
        }

        /**
         * Resolves a relative URI against a base URI.
         */
        @Override
        public URI create(URI baseUri, String uri) {
            return baseUri.resolve(uri);
        }
    }

    /**
     * Simple {@link URNFactory} implementation used in the tests.
     */
    private static class DummyURNFactory implements URNFactory {

        @Override
        public URI create(String urn) {
            return URI.create(urn);
        }
    }

    /**
     * Simple holder for resolution test data.
     */
    private static final class ResolutionCase {

        private final String description;

        private final String baseUri;

        private final String schemaPath;

        private final String expected;

        ResolutionCase(String description, String baseUri, String schemaPath, String expected) {
            this.description = description;
            this.baseUri = baseUri;
            this.schemaPath = schemaPath;
            this.expected = expected;
        }

        String description() {
            return description;
        }

        String baseUri() {
            return baseUri;
        }

        String schemaPath() {
            return schemaPath;
        }

        String expected() {
            return expected;
        }
    }

    /**
     * Simple holder for encoding test data.
     */
    private static final class EncodingCase {

        private final String description;

        private final String baseUri;

        private final String schemaPath;

        private final String expected;

        EncodingCase(String description, String baseUri, String schemaPath, String expected) {
            this.description = description;
            this.baseUri = baseUri;
            this.schemaPath = schemaPath;
            this.expected = expected;
        }

        String description() {
            return description;
        }

        String baseUri() {
            return baseUri;
        }

        String schemaPath() {
            return schemaPath;
        }

        String expected() {
            return expected;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the collaborators with simple dummy implementations.
        URIFactory uriFactory = new DummyURIFactory();
        URNFactory urnFactory = new DummyURNFactory();
        // any non‑null meta‑schema works
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();
        // default factory
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance();
        // default configuration
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // The ValidationContext constructor validates that none of the arguments are null.
        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema, jsonSchemaFactory, config);
    }

    private static Stream<ResolutionCase> resolutionCases() {
        return Stream.of(new ResolutionCase("relative schemaPath resolves against base URI", "http://example.com/base/", "sub/schema.json", "http://example.com/base/sub/schema.json"), new ResolutionCase("absolute schemaPath replaces the base URI", "http://example.com/base/", "http://example.com/absolute/schema.json", "http://example.com/absolute/schema.json"), new ResolutionCase("schemaPath with fragment is preserved", "http://example.com/base/", "schema.json#fragment", "http://example.com/base/schema.json#fragment"), new ResolutionCase("schemaPath with query string is preserved", "http://example.com/base/", "schema.json?query=1", "http://example.com/base/schema.json?query=1"), new ResolutionCase("empty schemaPath leaves base URI unchanged", "http://example.com/base/", "", "http://example.com/base/"));
    }

    private static Stream<EncodingCase> encodingCases() {
        return Stream.of(new EncodingCase("space in path must be percent‑encoded", "http://example.com/base/", "space in name.json", "http://example.com/base/space%20in%20name.json"), new EncodingCase("Unicode check‑mark must be percent‑encoded", "http://example.com/base/", "✓.json", "http://example.com/base/%E2%9C%93.json"), new EncodingCase("leading hash is treated as fragment", "http://example.com/base/", "#hash.json", "http://example.com/base/#hash.json"));
    }

    @Test
    void getCurrentUri_withoutId_returnsProvidedUri() throws Exception {
        // GIVEN
        URI expectedUri = new URI("http://example.com/schema.json");
        JsonNode emptySchemaNode = MAPPER.readTree("{}");
        // WHEN
        JsonSchema schema = JsonSchema.from(validationContext, "#", expectedUri, emptySchemaNode, null, false);
        // THEN
        Assertions.assertNotNull(schema, "JsonSchema instance should be created");
        Assertions.assertEquals(expectedUri, schema.getCurrentUri(), "When the schema node does not contain an $id, getCurrentUri() must return the URI supplied to the constructor");
    }

    @Test
    void getCurrentUri_withId_combinesBaseAndId() throws Exception {
        // GIVEN
        URI baseUri = new URI("http://example.com/base/");
        JsonNode schemaWithId = MAPPER.readTree("{\"$id\": \"sub/schema.json\"}");
        // WHEN
        JsonSchema schema = JsonSchema.from(validationContext, "#", baseUri, schemaWithId, null, false);
        // THEN
        Assertions.assertNotNull(schema, "JsonSchema instance should be created");
        URI actual = schema.getCurrentUri();
        URI expected = baseUri.resolve("sub/schema.json");
        Assertions.assertEquals(expected, actual, "When the schema node contains an $id, getCurrentUri() must reflect the combined URI");
    }

    @Test
    void getCurrentUri_whenInternalFieldIsNull_returnsNull() throws Exception {
        // GIVEN – a normal schema instance
        URI uri = new URI("http://example.com/any.json");
        JsonSchema schema = JsonSchema.from(validationContext, "#", uri, MAPPER.readTree("{}"), null, false);
        // Use reflection to set the private field `currentUri` to null.
        Field field = JsonSchema.class.getDeclaredField("currentUri");
        field.setAccessible(true);
        field.set(schema, null);
        // WHEN / THEN
        Assertions.assertNull(schema.getCurrentUri(), "If the internal `currentUri` field is null, the getter must return null");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("resolutionCases")
    void getCurrentUri_uriResolutionBehavior(ResolutionCase rc) throws Exception {
        // GIVEN
        URI base = new URI(rc.baseUri());
        JsonNode emptyNode = MAPPER.readTree("{}");
        // WHEN
        JsonSchema schema = JsonSchema.from(validationContext, rc.schemaPath(), base, emptyNode, null, false);
        // THEN
        Assertions.assertNotNull(schema, "JsonSchema instance should be created");
        Assertions.assertEquals(new URI(rc.expected()), schema.getCurrentUri(), rc.description());
    }

    @Test
    void getCurrentUri_returnsSameInstance_whenSuppliedNonNullUri() throws Exception {
        // GIVEN
        URI supplied = new URI("http://example.com/identical.json");
        JsonSchema schema = JsonSchema.from(validationContext, "#", supplied, MAPPER.readTree("{}"), null, false);
        // WHEN
        URI result = schema.getCurrentUri();
        // THEN
        Assertions.assertSame(supplied, result, "The getter must return the exact same URI object (reference equality)");
    }

    @Test
    void getCurrentUri_returnsBaseInstance_whenSchemaPathResolvesToSameUri() throws Exception {
        // GIVEN
        URI base = new URI("http://example.com/dir/");
        JsonSchema schema = JsonSchema.from(validationContext, ".", base, MAPPER.readTree("{}"), null, false);
        // WHEN
        URI result = schema.getCurrentUri();
        // THEN
        Assertions.assertSame(base, result, "When the resolved URI is identical to the base, the original URI instance must be returned");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("encodingCases")
    void getCurrentUri_encodesSpecialCharactersCorrectly(EncodingCase ec) throws Exception {
        // GIVEN
        URI base = new URI(ec.baseUri());
        JsonSchema schema = JsonSchema.from(validationContext, ec.schemaPath(), base, MAPPER.readTree("{}"), null, false);
        // WHEN
        URI actual = schema.getCurrentUri();
        // THEN
        Assertions.assertEquals(new URI(ec.expected()), actual, ec.description());
    }
}
