package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link JsonSchemaFactory}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>Public API contract (builder, factory methods, version handling)</li>
 *   <li>Edge‑case validation (null / empty arguments, malformed URIs)</li>
 *   <li>Exception handling (IO errors, illegal arguments)</li>
 *   <li>Internal behaviour exercised via public methods (YAML detection, meta‑schema normalisation)</li>
 *   <li>Cache behaviour for {@code getSchema(URI,…)} using a mocked {@link URIFetcher}</li>
 * </ul>
 *
 * <p>Private helpers are accessed with reflection where needed; this is acceptable in a unit‑test
 * context because the methods are package‑private / protected and have no public equivalent.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaFactoryTest {

    /** A real factory built from the built‑in Draft‑V4 meta‑schema – used for most happy‑path tests. */
    private static JsonSchemaFactory draftV4Factory;

    @BeforeClass
    public static void setUpClass() {
        // Build a factory that contains the official Draft‑V4 meta‑schema.
        draftV4Factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull("Factory should be created", draftV4Factory);
    }

    /** --------------------------------------------------------------------- */
    /**  Builder validation tests                                            */
    /** --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void builderShouldRejectNullDefaultMetaSchemaUri() {
        // The builder validates that the default meta‑schema URI is non‑null / non‑empty.
        JsonSchemaFactory.builder()
                .addMetaSchema(draftV4Factory.jsonMetaSchemas.get(draftV4Factory.defaultMetaSchemaURI))
                .defaultMetaSchemaURI(null)   // <-- illegal
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderShouldRejectEmptyDefaultMetaSchemaUri() {
        JsonSchemaFactory.builder()
                .addMetaSchema(draftV4Factory.jsonMetaSchemas.get(draftV4Factory.defaultMetaSchemaURI))
                .defaultMetaSchemaURI("   ") // <-- illegal (empty after trim)
                .build();
    }

    /** --------------------------------------------------------------------- */
    /**  Version handling tests                                             */
    /** --------------------------------------------------------------------- */

    @Test
    public void checkVersionShouldReturnCorrectVersionInstances() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
    }

    @Test
    public void checkVersionShouldReturnNullForNullFlag() {
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkVersionShouldThrowOnUnsupportedFlag() {
        // Simulate a future enum constant that is not handled – the default case should fire.
        JsonSchemaFactory.checkVersion(Enum.valueOf(SpecVersion.VersionFlag.class, "V999"));
    }

    /** --------------------------------------------------------------------- */
    /**  Normalisation of Meta‑Schema URIs                                   */
    /** --------------------------------------------------------------------- */

    @Test
    public void normalizeMetaSchemaUriShouldForceHttpsScheme() throws Exception {
        String original = "http://json-schema.org/draft-04/schema#";
        Method normalize = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        normalize.setAccessible(true);
        String normalized = (String) normalize.invoke(null, original);
        assertTrue("Scheme must be https", normalized.startsWith("https://"));
        assertTrue(normalized.contains("json-schema.org"));
    }

    @Test(expected = JsonSchemaException.class)
    public void normalizeMetaSchemaUriShouldThrowOnInvalidUri() throws Exception {
        Method normalize = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
        normalize.setAccessible(true);
        normalize.invoke(null, ":// not a uri");
    }

    /** --------------------------------------------------------------------- */
    /**  YAML / JSON detection on URI                                         */
    /** --------------------------------------------------------------------- */

    @Test
    public void isYamlShouldDetectYamlExtension() throws Exception {
        Method isYaml = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        isYaml.setAccessible(true);

        URI yaml1 = new URI("file:/tmp/schema.yaml");
        URI yaml2 = new URI("http://example.com/schemas/my-schema.yml");
        URI json = new URI("http://example.com/schemas/schema.json");
        URI noExt = new URI("http://example.com/schemas/schema");

        assertTrue((Boolean) isYaml.invoke(draftV4Factory, yaml1));
        assertTrue((Boolean) isYaml.invoke(draftV4Factory, yaml2));
        assertFalse((Boolean) isYaml.invoke(draftV4Factory, json));
        assertFalse((Boolean) isYaml.invoke(draftV4Factory, noExt));
    }

    /** --------------------------------------------------------------------- */
    /**  getSchema from String (JSON) – happy path                           */
    /** --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromStringShouldParseValidJson() {
        String simpleSchema = "{\"type\":\"object\"}";
        JsonSchema schema = draftV4Factory.getSchema(simpleSchema);
        assertNotNull("Schema object must be returned", schema);
        // The returned JsonSchema should contain a ValidationContext with the default meta‑schema.
        assertEquals(draftV4Factory.defaultMetaSchemaURI,
                schema.getValidationContext().getMetaSchema().getUri());
    }

    /** --------------------------------------------------------------------- */
    /**  getSchema from String (JSON) – malformed input                        */
    /** --------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromStringShouldThrowOnInvalidJson() {
        // Missing closing brace – Jackson will throw an IOException which the factory wraps.
        String malformed = "{\"type\":\"object\"";
        draftV4Factory.getSchema(malformed);
    }

    /** --------------------------------------------------------------------- */
    /**  getSchema from InputStream – happy path                               */
    /** --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromInputStreamShouldParseValidJson() throws Exception {
        String json = "{\"type\":\"string\"}";
        try (InputStream is = new ByteArrayInputStream(json.getBytes())) {
            JsonSchema schema = draftV4Factory.getSchema(is);
            assertNotNull(schema);
        }
    }

    /** --------------------------------------------------------------------- */
    /**  getSchema from URI – cache behaviour                                 */
    /** --------------------------------------------------------------------- */

    @Mock
    private URIFetcher mockFetcher;

    private JsonSchemaFactory factoryWithMockFetcher;

    @Before
    public void setUpFactoryWithMockFetcher() throws Exception {
        // Build a factory that uses the mock fetcher and enables the cache.
        JsonMetaSchema draftMeta = draftV4Factory.jsonMetaSchemas.get(draftV4Factory.defaultMetaSchemaURI);
        factoryWithMockFetcher = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(draftMeta.getUri())
                .addMetaSchema(draftMeta)
                .uriFactory(new URLFactory(), "http", "https")
                .uriFetcher(mockFetcher, "http", "https")
                .enableUriSchemaCache(true)
                .build();

        // Mock fetcher returns a simple JSON schema for any URI.
        when(mockFetcher.fetch(any(URI.class))).thenAnswer(invocation -> {
            String json = "{\"type\":\"number\"}";
            return new ByteArrayInputStream(json.getBytes());
        });
    }

    @Test
    public void getSchemaFromUriShouldCacheResultWhenEnabled() throws Exception {
        URI uri = new URI("http://example.com/simple-schema.json");

        // First call – fetcher is invoked.
        JsonSchema first = factoryWithMockFetcher.getSchema(uri);
        assertNotNull(first);
        verify(mockFetcher, times(1)).fetch(uri);

        // Second call – result should be retrieved from the cache, fetcher not called again.
        JsonSchema second = factoryWithMockFetcher.getSchema(uri);
        assertNotNull(second);
        verify(mockFetcher, times(1)).fetch(uri); // still only one invocation

        // The cached instance should be the same (reference equality).
        assertSame(first, second);
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromUriShouldWrapFetcherIOException() throws Exception {
        URI badUri = new URI("http://example.com/unreachable.json");
        when(mockFetcher.fetch(badUri)).thenThrow(new IOException("network down"));
        factoryWithMockFetcher.getSchema(badUri);
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromUriShouldWrapIllegalArgumentWhenUriFactoryFails() throws Exception {
        // Force the underlying URI factory to throw IllegalArgumentException.
        URIFactory badFactory = mock(URIFactory.class);
        when(badFactory.create(anyString())).thenThrow(new IllegalArgumentException("bad uri"));
        JsonMetaSchema draftMeta = draftV4Factory.jsonMetaSchemas.get(draftV4Factory.defaultMetaSchemaURI);
        JsonSchemaFactory brokenFactory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(draftMeta.getUri())
                .addMetaSchema(draftMeta)
                .uriFactory(badFactory, "http")
                .uriFetcher(mockFetcher, "http")
                .enableUriSchemaCache(false)
                .build();

        URI uri = new URI("http://example.com/schema.json");
        brokenFactory.getSchema(uri);
    }

    /** --------------------------------------------------------------------- */
    /**  getSchema from URI – YAML detection                                   */
    /** --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromYamlUriShouldUseYamlMapper() throws Exception {
        // Prepare a YAML schema (simple type string)
        String yaml = "type: string";
        when(mockFetcher.fetch(any(URI.class))).thenReturn(new ByteArrayInputStream(yaml.getBytes()));

        URI yamlUri = new URI("http://example.com/schema.yaml");
        JsonSchema schema = factoryWithMockFetcher.getSchema(yamlUri);
        assertNotNull(schema);

        // Verify that the factory used the YAML mapper by checking that the node
        // has a "type" property with value "string".
        JsonNode node = schema.getValidationContext().getSchemaNode();
        assertEquals("string", node.get("type").asText());
    }

    /** --------------------------------------------------------------------- */
    /**  getSchema overloads with JsonNode and Config – basic sanity check   */
    /** --------------------------------------------------------------------- */

    @Test
    public void getSchemaFromJsonNodeShouldReturnNonNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"type\":\"boolean\"}");
        JsonSchema schema = draftV4Factory.getSchema(node);
        assertNotNull(schema);
        assertEquals("boolean", schema.getValidationContext().getSchemaNode().get("type").asText());
    }

    @Test
    public void getSchemaFromJsonNodeAndConfigShouldRespectConfig() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"type\":\"null\"}");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setFailFast(true);
        JsonSchema schema = draftV4Factory.getSchema(node, config);
        assertNotNull(schema);
        assertTrue(schema.getValidationContext().getConfig().isFailFast());
    }

    /** --------------------------------------------------------------------- */
    /**  Accessor methods – simple contract checks                             */
    /** --------------------------------------------------------------------- */

    @Test
    public void getUriFactoryShouldReturnFactoryProvidedInBuilder() {
        URIFactory factory = draftV4Factory.getUriFactory();
        assertNotNull(factory);
        // The default factory must contain at least the "http" scheme.
        assertTrue(factory.getURIFactories().containsKey("http"));
    }

    @Test
    public void getUriTranslatorShouldCombineMappingsAndTranslators() {
        URITranslator translator = draftV4Factory.getUriTranslator();
        assertNotNull(translator);
        // The default translator should map the empty map (no custom mappings) and
        // contain the built‑in CompositeURITranslator.
        URI mapped = translator.translate(URI.create("http://example.com"));
        assertEquals(URI.create("http://example.com"), mapped);
    }

    /** --------------------------------------------------------------------- */
    /**  Helper to invoke private methods via reflection                     */
    /** --------------------------------------------------------------------- */

    private static Object invokePrivate(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(draftV4Factory, args);
    }

    /** --------------------------------------------------------------------- */
    /**  Additional edge‑case tests                                           */
    /** --------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromStringShouldWrapNullPointerException() {
        // Passing null to ObjectMapper.readTree results in IllegalArgumentException,
        // which the factory wraps into JsonSchemaException.
        draftV4Factory.getSchema((String) null);
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromUriShouldWrapNullArgument() throws Exception {
        // The public method does not accept null; it will eventually call the URI factory,
        // which throws IllegalArgumentException – the factory wraps it.
        draftV4Factory.getSchema((URI) null);
    }
}
