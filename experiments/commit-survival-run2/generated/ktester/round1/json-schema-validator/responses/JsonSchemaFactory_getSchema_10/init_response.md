```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.CompositeURITranslator;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_10_Test {

    /*** shared test data ***/
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    /*** objects that are part of the JsonSchemaFactory constructor ***/
    private ObjectMapper jsonMapper;
    private YAMLMapper yamlMapper;
    private URISchemeFactory uriFactory;
    private URISchemeFetcher uriFetcher;
    private URNFactory urnFactory;
    private CompositeURITranslator uriTranslators;
    private Map<String, JsonMetaSchema> jsonMetaSchemas;
    private Map<String, String> uriMap;

    /*** objects used in the actual test ***/
    private JsonNode sampleNode;
    private SchemaValidatorsConfig config;
    private URI sampleUri;

    /*** the factory under test (spied so we can stub the protected newJsonSchema method) ***/
    private JsonSchemaFactory factorySpy;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* -------------------------------------------------
         * 1️⃣  Build the constructor arguments for JsonSchemaFactory
         * ------------------------------------------------- */
        jsonMapper = new ObjectMapper();
        yamlMapper = new YAMLMapper();

        // Minimal (empty) but non‑null maps for the scheme factories/fetchers
        uriFactory = new URISchemeFactory(Collections.emptyMap());
        uriFetcher = new URISchemeFetcher(Collections.emptyMap());

        // URNFactory has a public no‑arg constructor
        urnFactory = new URNFactory();

        // Composite translator – we do not need any real translators for the test
        uriTranslators = new CompositeURITranslator();

        // JsonMetaSchema map must contain an entry for the default meta‑schema URI.
        // We can mock JsonMetaSchema because the factory never inspects its content
        // during the execution of getSchema(...).
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
        jsonMetaSchemas = new HashMap<>();
        jsonMetaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMetaSchema);

        // Empty URI mapping and cache flag – not relevant for this unit test
        uriMap = Collections.emptyMap();
        boolean enableUriSchemaCache = false;

        /* -------------------------------------------------
         * 2️⃣  Create a real factory instance and then spy it
         * ------------------------------------------------- */
        JsonSchemaFactory realFactory = new JsonSchemaFactory(
                jsonMapper,
                yamlMapper,
                DEFAULT_META_SCHEMA_URI,
                uriFactory,
                uriFetcher,
                urnFactory,
                jsonMetaSchemas,
                uriMap,
                enableUriSchemaCache,
                uriTranslators
        );

        // Spy allows us to stub the protected newJsonSchema method without touching private fields
        factorySpy = Mockito.spy(realFactory);

        // Stub the protected method to return a mock JsonSchema – this isolates the test
        JsonSchema mockSchema = mock(JsonSchema.class);
        doReturn(mockSchema).when(factorySpy).newJsonSchema(any(URI.class), any(JsonNode.class), any(SchemaValidatorsConfig.class));

        /* -------------------------------------------------
         * 3️⃣  Prepare the parameters for the focal method
         * ------------------------------------------------- */
        sampleUri = new URI("http://example.com/my-schema.json");
        sampleNode = jsonMapper.readTree("{\"type\":\"object\"}");
        config = new SchemaValidatorsConfig();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void shouldReturnJsonSchemaFromGetSchema_WithUriNodeAndConfig() {
        // WHEN
        JsonSchema result = factorySpy.getSchema(sampleUri, sampleNode, config);

        // THEN
        assertNotNull(result, "The returned JsonSchema should not be null");
        // Verify that the protected factory method was invoked with the exact arguments we supplied
        verify(factorySpy, times(1)).newJsonSchema(eq(sampleUri), eq(sampleNode), eq(config));
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenSchemaUriIsNull() {
        // GIVEN a null URI
        URI nullUri = null;

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> factorySpy.getSchema(nullUri, sampleNode, config),
                "Calling getSchema with a null URI should raise IllegalArgumentException");
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenJsonNodeIsNull() {
        // GIVEN a null JsonNode
        JsonNode nullNode = null;

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> factorySpy.getSchema(sampleUri, nullNode, config),
                "Calling getSchema with a null JsonNode should raise IllegalArgumentException");
    }

    @Test
    void shouldThrowIllegalArgumentException_WhenConfigIsNull() {
        // GIVEN a null config
        SchemaValidatorsConfig nullConfig = null;

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> factorySpy.getSchema(sampleUri, sampleNode, nullConfig),
                "Calling getSchema with a null SchemaValidatorsConfig should raise IllegalArgumentException");
    }
}
```