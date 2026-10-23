### Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The test class follows the requested steps:
 *
 * 1. List of all public methods (including signatures) extracted from the source.
 * 2. Basic test cases that verify the normal behaviour of each method.
 * 3. Identification of edge‑cases / exception scenarios.
 * 4. Additional test cases that target those edge‑cases.
 * 5. All test methods are merged into one file that can be compiled and run
 *    with JUnit‑4 (and Mockito for the required stubbing).
 *
 * NOTE:
 * - The library classes such as {@link JsonSchema}, {@link ValidationContext},
 *   {@link JsonMetaSchema}, {@link URIFactory} etc. are part of the *networknt*
 *   JSON‑Schema validator project.  In order to keep the tests self‑contained,
 *   the heavy dependencies are mocked with Mockito where possible.
 * - Real parsing (Jackson/YAML) is used for the few tests that need to
 *   deserialize a schema string or a YAML file.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Public methods of {@code JsonSchemaFactory} (extracted from the source):
 *
 * <pre>
 * public static Builder builder()
 * public static JsonSchemaFactory getInstance()
 * public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag versionFlag)
 * public static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag versionFlag)
 * public static Builder builder(JsonSchemaFactory blueprint)
 *
 * public JsonSchema create(ValidationContext validationContext,
 *                          String schemaPath,
 *                          JsonNode schemaNode,
 *                          JsonSchema parentSchema)
 *
 * public URIFactory getUriFactory()
 * public URITranslator getUriTranslator()
 *
 * public JsonSchema getSchema(String schema, SchemaValidatorsConfig config)
 * public JsonSchema getSchema(String schema)
 *
 * public JsonSchema getSchema(InputStream schemaStream,
 *                            SchemaValidatorsConfig config)
 * public JsonSchema getSchema(InputStream schemaStream)
 *
 * public JsonSchema getSchema(URI schemaUri,
 *                             SchemaValidatorsConfig config)
 * public JsonSchema getSchema(URI schemaUri)
 *
 * public JsonSchema getSchema(URI schemaUri,
 *                             JsonNode jsonNode,
 *                             SchemaValidatorsConfig config)
 *
 * public JsonSchema getSchema(JsonNode jsonNode,
 *                             SchemaValidatorsConfig config)
 *
 * public JsonSchema getSchema(URI schemaUri,
 *                             JsonNode jsonNode)
 *
 * public JsonSchema getSchema(JsonNode jsonNode)
 *
 * // (private / protected helpers are not part of the public API)
 * </pre>
 *
 * The test suite below covers the normal flow of each public method and also
 * the identified edge‑cases.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaFactoryTest {

    /* --------------------------------------------------------------------- *
     *  Test fixtures
     * --------------------------------------------------------------------- */

    @Mock
    private JsonMetaSchema mockMetaSchema;               // required by the builder
    @Mock
    private URIFactory mockUriFactory;                    // custom factory for URI tests
    @Mock
    private URIFetcher mockUriFetcher;                    // custom fetcher for URI tests

    private JsonSchemaFactory factory;                  // instance under test
    private static final String DRAFT_07_URI = "https://json-schema.org/draft-07/schema";

    @Before
    public void setUp() throws Exception {
        // -----------------------------------------------------------------
        // Minimal, yet functional, factory configuration
        // -----------------------------------------------------------------
        when(mockMetaSchema.getUri()).thenReturn(DRAFT_07_URI);
        // The meta‑schema must be able to read an "$id" – for the tests we simply
        // return null (no id) which is sufficient for the default flow.
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn(null);

        factory = JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .build();
    }

    /* --------------------------------------------------------------------- *
     *  1. BASIC FUNCTIONALITY TESTS
     * --------------------------------------------------------------------- */

    @Test
    public void testBuilderCreatesFactory() {
        assertNotNull("Factory built with builder must not be null", factory);
        assertEquals("Default meta‑schema URI must be preserved",
                DRAFT_07_URI, factory.defaultMetaSchemaURI);
    }

    @Test
    public void testStaticBuilderMethod() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder();
        assertNotNull(b);
    }

    @Test
    public void testGetInstanceWithDefaultVersion() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance();
        assertNotNull("Factory obtained via getInstance() must not be null", f);
    }

    @Test
    public void testGetInstanceWithExplicitVersion() {
        JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull(f);
    }

    @Test
    public void testCheckVersionValidValues() {
        for (SpecVersion.VersionFlag flag : SpecVersion.VersionFlag.values()) {
            JsonSchemaVersion version = JsonSchemaFactory.checkVersion(flag);
            assertNotNull("checkVersion should return a non‑null value for " + flag, version);
        }
    }

    @Test
    public void testCheckVersionNullFlag() {
        assertNull("checkVersion(null) must return null", JsonSchemaFactory.checkVersion(null));
    }

    @Test
    public void testBuilderFromBlueprint() {
        JsonSchemaFactory.Builder b = JsonSchemaFactory.builder(factory);
        assertNotNull(b);
        JsonSchemaFactory rebuilt = b.build();
        assertNotNull(rebuilt);
        // the rebuilt factory must contain the same default meta‑schema URI
        assertEquals(factory.defaultMetaSchemaURI, rebuilt.defaultMetaSchemaURI);
    }

    @Test
    public void testGetUriFactory() {
        URIFactory uf = factory.getUriFactory();
        assertNotNull(uf);
    }

    @Test
    public void testGetUriTranslator() {
        URITranslator tr = factory.getUriTranslator();
        assertNotNull(tr);
    }

    @Test
    public void testGetSchemaFromStringValid() {
        String simpleSchema = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(simpleSchema);
        assertNotNull("A well‑formed JSON schema string should produce a JsonSchema object", schema);
    }

    @Test
    public void testGetSchemaFromStringWithConfig() {
        String simpleSchema = "{\"type\":\"object\"}";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema schema = factory.getSchema(simpleSchema, cfg);
        assertNotNull(schema);
        // the config is attached to the ValidationContext inside the schema.
        // We verify that it was stored (indirectly) by checking that the
        // schema’s ValidationContext holds the same instance.
        assertSame(cfg, schema.getValidationContext().getConfig());
    }

    @Test
    public void testGetSchemaFromInputStreamValid() throws IOException {
        String json = "{\"type\":\"string\"}";
        try (InputStream is = new ByteArrayInputStream(json.getBytes())) {
            JsonSchema schema = factory.getSchema(is);
            assertNotNull(schema);
        }
    }

    @Test
    public void testGetSchemaFromUriWithJsonExtension() throws Exception {
        // -----------------------------------------------------------------
        // Prepare a mock URI scheme "mock" that returns a JSON schema.
        // -----------------------------------------------------------------
        URI testUri = new URI("mock://test.json");
        String json = "{\"type\":\"integer\"}";
        InputStream jsonStream = new ByteArrayInputStream(json.getBytes());

        when(mockUriFactory.create(anyString())).thenAnswer(invocation -> {
            // the factory simply returns the URI it received
            return new URI(invocation.getArgument(0));
        });
        when(mockUriFetcher.fetch(eq(testUri))).thenReturn(jsonStream);

        JsonSchemaFactory customFactory = JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockUriFactory, "mock")
                .uriFetcher(mockUriFetcher, "mock")
                .build();

        JsonSchema schema = customFactory.getSchema(testUri);
        assertNotNull("Schema fetched via custom mock URI must be created", schema);
        // verify that the fetcher was indeed invoked
        verify(mockUriFetcher, times(1)).fetch(eq(testUri));
    }

    @Test
    public void testGetSchemaFromUriWithYamlExtension() throws Exception {
        // -----------------------------------------------------------------
        // Same as the previous test but with a ".yaml" file to trigger the
        // YAML parsing branch.
        // -----------------------------------------------------------------
        URI yamlUri = new URI("mock://test.yaml");
        String yaml = "type: object";
        InputStream yamlStream = new ByteArrayInputStream(yaml.getBytes());

        when(mockUriFactory.create(anyString())).thenAnswer(invocation -> new URI(invocation.getArgument(0)));
        when(mockUriFetcher.fetch(eq(yamlUri))).thenReturn(yamlStream);

        JsonSchemaFactory customFactory = JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockUriFactory, "mock")
                .uriFetcher(mockUriFetcher, "mock")
                .build();

        JsonSchema schema = customFactory.getSchema(yamlUri);
        assertNotNull("YAML schema must be parsed and returned", schema);
        verify(mockUriFetcher, times(1)).fetch(eq(yamlUri));
    }

    /* --------------------------------------------------------------------- *
     *  2. EDGE‑CASE / EXCEPTION TESTS
     * --------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromStringInvalidJson() {
        factory.getSchema("this is not json");
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromInputStreamInvalidJson() throws IOException {
        try (InputStream is = new ByteArrayInputStream("not { json".getBytes())) {
            factory.getSchema(is);
        }
    }

    @Test(expected = JsonSchemaException.class)
    public void testGetSchemaFromUriUnsupportedScheme() throws Exception {
        // Use a scheme that the default factory does NOT know about.
        URI badUri = new URI("unknown://schema.json");
        factory.getSchema(badUri);
    }

    @Test(expected = JsonSchemaException.class)
    public void testNormalizeMetaSchemaUriWithBadInput() throws Exception {
        // The method is protected static – we reach it via reflection.
        java.lang.reflect.Method m = JsonSchemaFactory.class
                .getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        m.setAccessible(true);
        m.invoke(null, "http://[invalid-uri]");
    }

    @Test
    public void testIdMatchesSourceUriReturnsFalseWhenNoId() throws Exception {
        // The method is private – we use reflection to invoke it.
        java.lang.reflect.Method m = JsonSchemaFactory.class
                .getDeclaredMethod("idMatchesSourceUri",
                        JsonMetaSchema.class, JsonNode.class, URI.class);
        m.setAccessible(true);

        // Create an empty schema node (no "$id")
        JsonNode emptyNode = new ObjectMapper().readTree("{}");
        URI sourceUri = new URI("http://example.com/schema.json");

        boolean result = (boolean) m.invoke(factory, mockMetaSchema, emptyNode, sourceUri);
        assertFalse("When schema has no $id, idMatchesSourceUri must return false", result);
    }

    @Test
    public void testIsYamlDetectionLogic() throws Exception {
        // Private method – accessed via reflection.
        java.lang.reflect.Method m = JsonSchemaFactory.class
                .getDeclaredMethod("isYaml", URI.class);
        m.setAccessible(true);

        URI jsonUri = new URI("file://some/path/schema.json");
        URI yamlUri = new URI("file://some/path/schema.yaml");
        URI ymlUri = new URI("file://some/path/schema.yml");
        URI noExtUri = new URI("file://some/path/schema");

        assertFalse((boolean) m.invoke(factory, jsonUri));
        assertTrue((boolean) m.invoke(factory, yamlUri));
        assertTrue((boolean) m.invoke(factory, ymlUri));
        assertFalse((boolean) m.invoke(factory, noExtUri));
    }

    @Test
    public void testCreateMethodDelegatesToDoCreate() {
        // create() is public and delegates to doCreate().  We spy on the factory
        // to verify that doCreate() is called with the expected arguments.
        JsonSchemaFactory spyFactory = Mockito.spy(factory);

        ValidationContext ctx = mock(ValidationContext.class);
        JsonNode node = new ObjectMapper().createObjectNode();
        JsonSchema parent = mock(JsonSchema.class);

        // invoke the method under test
        spyFactory.create(ctx, "/myPath", node, parent);

        // verify delegation
        verify(spyFactory, times(1))
                .doCreate(eq(ctx), eq("/myPath"), any(URI.class), eq(node), eq(parent), eq(false));
    }

    /* --------------------------------------------------------------------- *
     *  3. HELPERS
     * --------------------------------------------------------------------- */

    // The following helper methods are not test cases; they simply make the
    // test code a little cleaner.

    private JsonSchemaFactory buildFactoryWithCustomUriComponents() {
        return JsonSchemaFactory.builder()
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(DRAFT_07_URI)
                .addMetaSchema(mockMetaSchema)
                .uriFactory(mockUriFactory, "mock")
                .uriFetcher(mockUriFetcher, "mock")
                .build();
    }
}
```
### Test END##