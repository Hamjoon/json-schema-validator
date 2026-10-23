###Test START##
```java
/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The tests are written from the perspective of three independent “experts”.
 * Each expert contributes a test for a distinct group of behaviours (typical,
 * edge‑case and error handling).  The suite therefore covers:
 *
 * <ul>
 *   <li>Builder construction and cloning (blueprint)</li>
 *   <li>Version handling – {@code checkVersion} and {@code getInstance}</li>
 *   <li>Meta‑schema resolution – {@code normalizeMetaSchemaUri},
 *       {@code findMetaSchemaForSchema}, {@code fromId}</li>
 *   <li>Schema creation APIs – all overloads of {@code getSchema}</li>
 *   <li>URI handling – {@code isYaml}, {@code idMatchesSourceUri},
 *       {@code getUriFactory}, {@code getUriTranslator}</li>
 *   <li>Caching behaviour for remote schemas</li>
 *   <li>Exceptional paths – malformed JSON/YAML, unknown meta‑schema,
 *       illegal arguments to the factory constructor</li>
 * </ul>
 *
 * Mockito is used to stub the heavy external collaborators
 * ({@link com.networknt.schema.uri.URIFactory},
 * {@link com.networknt.schema.uri.URIFetcher},
 * {@link com.networknt.schema.JsonMetaSchema}) so that the tests remain fast
 * and deterministic.
 *
 * The test class can be compiled and executed with JUnit‑4 (e.g. via Maven
 * surefire or an IDE).
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * JUnit‑4 test suite for {@link JsonSchemaFactory}.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaFactoryTest {

    // -----------------------------------------------------------------------
    // Common test fixtures (shared by the three “experts”)
    // -----------------------------------------------------------------------

    private static final String DRAFT7_URI = "http://json-schema.org/draft-07/schema#";

    /** Minimal meta‑schema used for the tests – it only needs to expose the
     *  {@code getUri()} and {@code readId(JsonNode)} methods. */
    private static class DummyMetaSchema implements JsonMetaSchema {
        private final String uri;

        DummyMetaSchema(String uri) {
            this.uri = uri;
        }

        @Override public String getUri() { return uri; }
        @Override public String readId(JsonNode node) { return null; }
        @Override public String getValidatorType() { return null; }
        @Override public String getSchemaVersion() { return null; }
        @Override public boolean isV4() { return false; }
        @Override public boolean isV6() { return false; }
        @Override public boolean isV7() { return false; }
        @Override public boolean isV2019_09() { return false; }
        @Override public boolean isV2020_12() { return false; }
        @Override public String getKeyword(String name) { return null; }
        @Override public Set<String> getKeywords() { return Collections.emptySet(); }
        @Override public String getFormat(String name) { return null; }
        @Override public Set<String> getFormats() { return Collections.emptySet(); }
        @Override public boolean supportsKeyword(String name) { return false; }
        @Override public boolean supportsFormat(String name) { return false; }
        @Override public Set<String> getKeywordsForDialect() { return Collections.emptySet(); }
    }

    /** A JsonMetaSchema that recognises the Draft‑07 keyword $id. */
    private static class Draft7MetaSchema extends DummyMetaSchema {
        Draft7MetaSchema() { super(DRAFT7_URI); }

        @Override public String readId(JsonNode node) {
            JsonNode idNode = node.get("$id");
            return (idNode != null && idNode.isTextual()) ? idNode.textValue() : null;
        }
    }

    // Mocks that are reused across many test methods
    @Mock private URIFactory mockUriFactory;
    @Mock private URIFetcher mockUriFetcher;
    @Mock private URNFactory mockUrnFactory;
    @Mock private URITranslator mockTranslator;

    // A real JsonMetaSchema that will be stored inside the factory
    private JsonMetaSchema draft7MetaSchema = new Draft7MetaSchema();

    // -----------------------------------------------------------------------
    // Helper methods used by several tests
    // -----------------------------------------------------------------------

    /** Builds a minimal, fully‑initialised {@link JsonSchemaFactory} that
     *  contains only the Draft‑07 meta‑schema. */
    private JsonSchemaFactory buildFactory() {
        return JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT7_URI)
                .addMetaSchema(draft7MetaSchema)
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .build();
    }

    /** Reads a JSON string into a {@link JsonNode}. */
    private JsonNode readJson(String json) throws Exception {
        return new ObjectMapper().readTree(json);
    }

    // -----------------------------------------------------------------------
    // 1️⃣  Builder & Blueprint tests (Expert‑A)
    // -----------------------------------------------------------------------

    /** Builder must reject a {@code null} ObjectMapper. */
    @Test(expected = IllegalArgumentException.class)
    public void builderShouldRejectNullObjectMapper() {
        JsonSchemaFactory.builder()
                .objectMapper(null)               // <-- illegal
                .yamlMapper(new YAMLMapper())
                .defaultMetaSchemaURI(DRAFT7_URI)
                .addMetaSchema(draft7MetaSchema)
                .build();
    }

    /** Builder must reject a missing or empty default meta‑schema URI. */
    @Test(expected = IllegalArgumentException.class)
    public void builderShouldRejectEmptyDefaultMetaSchemaURI() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI("  ")       // blank after trim
                .addMetaSchema(draft7MetaSchema)
                .build();
    }

    /** Builder must reject a null meta‑schema collection. */
    @Test(expected = IllegalArgumentException.class)
    public void builderShouldRejectNullMetaSchemas() {
        JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT7_URI)
                .addMetaSchema(null)              // null meta‑schema
                .build();
    }

    /** The blueprint‑based builder must clone all configuration from the source
     *  factory (including custom URI factories, fetchers and translators). */
    @Test
    public void blueprintBuilderShouldCloneConfiguration() {
        // Prepare a factory with a few custom components
        JsonSchemaFactory original = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT7_URI)
                .addMetaSchema(draft7MetaSchema)
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .uriFactory(mockUriFactory, "custom")
                .uriFetcher(mockUriFetcher, "custom")
                .addUriTranslator(mockTranslator)
                .addUrnFactory(mockUrnFactory)
                .enableUriSchemaCache(false)
                .build();

        // Clone via the blueprint builder
        JsonSchemaFactory clone = JsonSchemaFactory.builder(original).build();

        // Verify that the essential fields are equal
        assertEquals(original.getUriFactory().getClass(),
                     clone.getUriFactory().getClass());

        // The URI translator is a composite – we only verify that the custom
        // translator we added is present.
        URITranslator cloneTranslator = clone.getUriTranslator();
        assertNotNull(cloneTranslator);
        // The composite translator contains the original map translator plus our mock
        // (there is no direct API to inspect, so we just ensure that calling it does
        // not throw).
        cloneTranslator.translate(URI.create("custom://test"));
    }

    // -----------------------------------------------------------------------
    // 2️⃣  Version handling tests (Expert‑B)
    // -----------------------------------------------------------------------

    /** {@code checkVersion} must map each {@link SpecVersion.VersionFlag}
     *  to the correct {@link JsonSchemaVersion} implementation. */
    @Test
    public void checkVersionShouldReturnCorrectVersionInstances() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
    }

    /** {@code checkVersion} must throw {@link IllegalArgumentException}
     *  for an unsupported flag. */
    @Test(expected = IllegalArgumentException.class)
    public void checkVersionShouldThrowForUnsupportedFlag() {
        JsonSchemaFactory.checkVersion(null); // null is treated as unsupported
    }

    /** {@code getInstance(VersionFlag)} must initialise a factory with the
     *  appropriate default meta‑schema and a single meta‑schema entry. */
    @Test
    public void getInstanceShouldCreateFactoryForVersionFlag() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        assertNotNull(factory);
        // The default meta‑schema URI must match the one from Version7
        assertEquals(Version7.SCHEMA_URI, factory.defaultMetaSchemaURI);
        // The internal map must contain exactly one entry (the default schema)
        assertEquals(1, factory.jsonMetaSchemas.size());
        assertTrue(factory.jsonMetaSchemas.containsKey(
                JsonSchemaFactory.normalizeMetaSchemaUri(Version7.SCHEMA_URI)));
    }

    // -----------------------------------------------------------------------
    // 3️⃣  Core functional tests (Expert‑C)
    // -----------------------------------------------------------------------

    /** {@code normalizeMetaSchemaUri} must replace any scheme with “https”
     *  and drop query/fragment components. */
    @Test
    public void normalizeMetaSchemaUriShouldForceHttpsAndStripFragments() {
        String original = "http://example.com:8080/path/to/schema.json?foo=bar#frag";
        String expected = "https://example.com:8080/path/to/schema.json";
        assertEquals(expected, JsonSchemaFactory.normalizeMetaSchemaUri(original));
    }

    /** {@code normalizeMetaSchemaUri} must throw {@link JsonSchemaException}
     *  when the supplied string is not a valid URI. */
    @Test(expected = JsonSchemaException.class)
    public void normalizeMetaSchemaUriShouldThrowOnInvalidUri() {
        JsonSchemaFactory.normalizeMetaSchemaUri("http://[invalid-uri");
    }

    /** {@code isYaml} must correctly recognise .yml and .yaml extensions,
     *  and treat everything else as JSON. */
    @Test
    public void isYamlShouldDetectYamlExtensions() throws Exception {
        JsonSchemaFactory factory = buildFactory();

        // yaml
        URI yaml = new URI("file:/tmp/schema.yaml");
        assertTrue(invokeIsYaml(factory, yaml));

        // yml
        URI yml = new URI("file:/tmp/schema.yml");
        assertTrue(invokeIsYaml(factory, yml));

        // json (no extension)
        URI jsonNoExt = new URI("file:/tmp/schema");
        assertFalse(invokeIsYaml(factory, jsonNoExt));

        // json (explicit .json)
        URI json = new URI("file:/tmp/schema.json");
        assertFalse(invokeIsYaml(factory, json));
    }

    /** Helper to call the private {@code isYaml} via reflection (JUnit
     *  cannot access package‑private methods from another package). */
    private boolean invokeIsYaml(JsonSchemaFactory factory, URI uri) throws Exception {
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        m.setAccessible(true);
        return (boolean) m.invoke(factory, uri);
    }

    /** {@code getSchema(String)} must parse a valid JSON schema string
     *  and return a non‑null {@link JsonSchema} instance. */
    @Test
    public void getSchemaFromStringShouldParseValidJson() {
        JsonSchemaFactory factory = buildFactory();
        String json = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(json);
        assertNotNull(schema);
        assertEquals("#", schema.getCurrentUri().toString()); // root schema uses "#"
    }

    /** {@code getSchema(String)} must wrap {@link IOException} (caused by
     *  malformed JSON) into a {@link JsonSchemaException}. */
    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromStringShouldWrapParsingErrors() {
        JsonSchemaFactory factory = buildFactory();
        String malformed = "{\"type\":}";
        factory.getSchema(malformed);
    }

    /** {@code getSchema(InputStream)} must correctly read from a stream. */
    @Test
    public void getSchemaFromInputStreamShouldParseJson() throws Exception {
        JsonSchemaFactory factory = buildFactory();
        String json = "{\"type\":\"string\"}";
        InputStream is = new ByteArrayInputStream(json.getBytes());
        JsonSchema schema = factory.getSchema(is);
        assertNotNull(schema);
    }

    /** {@code getSchema(URI)} must use the injected {@link URIFactory} and
     *  {@link URIFetcher} to retrieve a remote schema. The method also tests the
     *  caching logic (enabled by default). */
    @Test
    public void getSchemaFromUriShouldFetchAndCacheSchema() throws Exception {
        // Prepare a tiny JSON schema that will be returned by the fetcher
        String remoteSchema = "{\"type\":\"number\"}";
        InputStream remoteStream = new ByteArrayInputStream(remoteSchema.getBytes());

        // Stub the URI factory – it simply returns the same URI it receives
        when(mockUriFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));

        // Stub the fetcher – it returns the stream we prepared
        when(mockUriFetcher.fetch(any(URI.class))).thenReturn(remoteStream);

        // Build the factory with our mocks and caching enabled
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT7_URI)
                .addMetaSchema(draft7MetaSchema)
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .uriFactory(mockUriFactory, "http")
                .uriFetcher(mockUriFetcher, "http")
                .build();

        URI schemaUri = new URI("http://example.com/schema.json");

        // First call – fetcher should be invoked
        JsonSchema schema1 = factory.getSchema(schemaUri);
        assertNotNull(schema1);
        verify(mockUriFetcher, times(1)).fetch(any(URI.class));

        // Second call – result must be served from cache, fetcher not called again
        JsonSchema schema2 = factory.getSchema(schemaUri);
        assertSame(schema1, schema2); // same instance from cache
        verify(mockUriFetcher, times(1)).fetch(any(URI.class)); // still only once
    }

    /** When caching is disabled, the factory must call the fetcher for every
     *  request even if the URI is identical. */
    @Test
    public void getSchemaShouldBypassCacheWhenDisabled() throws Exception {
        InputStream stream = new ByteArrayInputStream("{\"type\":\"null\"}".getBytes());

        when(mockUriFactory.create(anyString())).thenAnswer(inv -> new URI(inv.getArgument(0)));
        when(mockUriFetcher.fetch(any(URI.class))).thenReturn(stream);

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT7_URI)
                .addMetaSchema(draft7MetaSchema)
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .uriFactory(mockUriFactory, "http")
                .uriFetcher(mockUriFetcher, "http")
                .enableUriSchemaCache(false) // <‑‑ disable caching
                .build();

        URI uri = new URI("http://example.com/disableCache.json");
        factory.getSchema(uri);
        factory.getSchema(uri);
        // Fetcher must be invoked twice because cache is disabled
        verify(mockUriFetcher, times(2)).fetch(any(URI.class));
    }

    /** {@code idMatchesSourceUri} must return true only when the $id value of
     *  the schema exactly matches the provided source URI. */
    @Test
    public void idMatchesSourceUriShouldDetectMatchingId() throws Exception {
        JsonSchemaFactory factory = buildFactory();

        // Schema with $id equal to the source URI
        JsonNode node = readJson("{\"$id\":\"http://example.com/my-schema\"}");
        URI source = new URI("http://example.com/my-schema");
        boolean matches = invokeIdMatches(factory, node, source);
        assertTrue(matches);

        // Different $id – should not match
        JsonNode node2 = readJson("{\"$id\":\"http://example.com/other\"}");
        boolean notMatch = invokeIdMatches(factory, node2, source);
        assertFalse(notMatch);
    }

    /** Helper to call the private {@code idMatchesSourceUri} via reflection. */
    private boolean invokeIdMatches(JsonSchemaFactory factory, JsonNode node, URI uri) throws Exception {
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod(
                "idMatchesSourceUri", JsonMetaSchema.class, JsonNode.class, URI.class);
        m.setAccessible(true);
        // Use the factory's default meta‑schema (Draft‑7) for the test
        JsonMetaSchema meta = draft7MetaSchema;
        return (boolean) m.invoke(factory, meta, node, uri);
    }

    /** {@code findMetaSchemaForSchema} must fall back to the default meta‑schema
     *  when the schema does not declare a $schema property. */
    @Test
    public void findMetaSchemaShouldFallBackToDefaultWhenAbsent() throws Exception {
        JsonSchemaFactory factory = buildFactory();
        JsonNode node = readJson("{\"type\":\"string\"}"); // no $schema

        // Access the private method via reflection
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod(
                "findMetaSchemaForSchema", JsonNode.class);
        m.setAccessible(true);
        JsonMetaSchema meta = (JsonMetaSchema) m.invoke(factory, node);
        assertEquals(draft7MetaSchema, meta);
    }

    /** When the schema contains an unknown $schema value, {@code findMetaSchemaForSchema}
     *  must raise {@link JsonSchemaException}. */
    @Test(expected = JsonSchemaException.class)
    public void findMetaSchemaShouldThrowOnUnknownDialect() throws Exception {
        JsonSchemaFactory factory = buildFactory();
        JsonNode node = readJson("{\"$schema\":\"http://example.com/unknown\"}");

        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod(
                "findMetaSchemaForSchema", JsonNode.class);
        m.setAccessible(true);
        m.invoke(factory, node);
    }

    /** {@code fromId} must resolve a known dialect (e.g. Draft‑07) via
     *  {@link SpecVersionDetector}. */
    @Test
    public void fromIdShouldResolveKnownDialect() {
        JsonSchemaFactory factory = buildFactory();
        // Use reflection to invoke the private method
        try {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("fromId", String.class);
            m.setAccessible(true);
            JsonMetaSchema resolved = (JsonMetaSchema) m.invoke(factory, DRAFT7_URI);
            assertEquals(DRAFT7_URI, resolved.getUri());
        } catch (Exception e) {
            fail("Reflection invocation failed: " + e);
        }
    }

    /** {@code fromId} must throw {@link JsonSchemaException} for an unknown
     *  dialect identifier. */
    @Test(expected = JsonSchemaException.class)
    public void fromIdShouldThrowOnUnknownDialect() throws Exception {
        JsonSchemaFactory factory = buildFactory();
        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("fromId", String.class);
        m.setAccessible(true);
        m.invoke(factory, "http://example.com/unknown-dialect");
    }

    /** {@code getUriFactory} must return the same instance that was supplied
     *  to the builder. */
    @Test
    public void getUriFactoryShouldReturnConfiguredFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT7_URI)
                .addMetaSchema(draft7MetaSchema)
                .uriFactory(mockUriFactory, "custom")
                .build();

        assertSame(mockUriFactory, factory.getUriFactory());
    }

    /** {@code getUriTranslator} must combine the explicit URI map with any
     *  custom translators that have been added. */
    @Test
    public void getUriTranslatorShouldCombineMapAndCustomTranslators() {
        Map<String, String> map = new HashMap<>();
        map.put("http://old.com/schema", "http://new.com/schema");

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(DRAFT7_URI)
                .addMetaSchema(draft7MetaSchema)
                .addUriMappings(map)
                .addUriTranslator(mockTranslator)
                .build();

        URITranslator translator = factory.getUriTranslator();
        assertNotNull(translator);

        // The map translator should rewrite the URI
        URI rewritten = translator.translate(URI.create("http://old.com/schema"));
        assertEquals("http://new.com/schema", rewritten.toString());

        // The custom mock translator must be invoked (no behaviour defined – just
        // ensure no exception)
        when(mockTranslator.translate(any(URI.class))).thenReturn(URI.create("mocked"));
        URI mocked = translator.translate(URI.create("any"));
        assertEquals("mocked", mocked.toString());
    }

    // -----------------------------------------------------------------------
    // Helper: expose private methods for direct unit testing (reflection)
    // -----------------------------------------------------------------------

    /** Utility method that calls the private {@code createValidationContext}
     *  to verify that it returns a non‑null {@link ValidationContext}
     *  containing the expected meta‑schema. */
    @Test
    public void createValidationContextShouldReturnContextWithCorrectMetaSchema() throws Exception {
        JsonSchemaFactory factory = buildFactory();
        JsonNode node = readJson("{\"type\":\"integer\"}");

        java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod(
                "createValidationContext", JsonNode.class);
        m.setAccessible(true);
        ValidationContext ctx = (ValidationContext) m.invoke(factory, node);
        assertNotNull(ctx);
        // The context should hold the same meta‑schema we injected
        java.lang.reflect.Field f = ValidationContext.class.getDeclaredField("metaSchema");
        f.setAccessible(true);
        JsonMetaSchema meta = (JsonMetaSchema) f.get(ctx);
        assertEquals(draft7MetaSchema, meta);
    }
}
```
###Test END##