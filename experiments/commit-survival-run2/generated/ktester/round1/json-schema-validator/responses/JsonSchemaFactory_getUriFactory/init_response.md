```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getUriFactory_Test {

    /** Instance under test */
    private JsonSchemaFactory jsonSchemaFactory;

    /** The factory we expect to be returned by {@code getUriFactory()} */
    private URIFactory expectedUriFactory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for this test class.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----- Minimal but valid collaborators ---------------------------------
        ObjectMapper jsonMapper = new ObjectMapper();               // real mapper – no external I/O
        YAMLMapper yamlMapper = new YAMLMapper();                  // real mapper – no external I/O

        // The default meta‑schema URI used by the factory (must be present in the map)
        String defaultMetaSchemaURI = "http://example.com/meta/schema";

        // A dummy JsonMetaSchema – we only need a non‑null entry in the map
        JsonMetaSchema dummyMetaSchema = new JsonMetaSchema(defaultMetaSchemaURI, jsonMapper, yamlMapper,
                Collections.emptyMap(), null, null, null, null, false, null);

        Map<String, JsonMetaSchema> jsonMetaSchemas = new HashMap<>();
        // The key is the normalized form of the default URI; the factory uses the same method,
        // so we store it under the exact string for simplicity.
        jsonMetaSchemas.put(defaultMetaSchemaURI, dummyMetaSchema);

        // Empty URI‑to‑URI mapping (allowed by the constructor)
        Map<String, String> uriMap = Collections.emptyMap();

        // The objects we want to verify are returned unchanged
        expectedUriFactory = new URIFactory() {
            // No behaviour needed for this test
        };

        // The scheme‑factory and fetcher can be empty – they are not exercised here
        URISchemeFactory uriSchemeFactory = new URISchemeFactory(Collections.emptyMap());
        URISchemeFetcher uriSchemeFetcher = new URISchemeFetcher(Collections.emptyMap());

        // URNFactory has a public no‑arg constructor
        URNFactory urnFactory = new URNFactory();

        // Composite translator – we can use the default constructor
        URITranslator.CompositeURITranslator uriTranslators = new URITranslator.CompositeURITranslator();

        // ----- Instantiate JsonSchemaFactory via reflection --------------------
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class
                .getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class,
                        URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class,
                        Map.class, Map.class, boolean.class,
                        URITranslator.CompositeURITranslator.class);
        ctor.setAccessible(true);
        jsonSchemaFactory = ctor.newInstance(
                jsonMapper,
                yamlMapper,
                defaultMetaSchemaURI,
                uriSchemeFactory,
                uriSchemeFetcher,
                urnFactory,
                jsonMetaSchemas,
                uriMap,
                false,
                uriTranslators
        );

        // Replace the private field `uriFactory` with our expected instance using reflection.
        // This mimics the scenario where the builder would have injected it.
        java.lang.reflect.Field uriFactoryField = JsonSchemaFactory.class.getDeclaredField("uriFactory");
        uriFactoryField.setAccessible(true);
        uriFactoryField.set(jsonSchemaFactory, expectedUriFactory);
    }

    @AfterEach
    void teardownAfterEach() {
        jsonSchemaFactory = null;
        expectedUriFactory = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    @Test
    void getUriFactory_returnsConfiguredFactory() {
        // When
        URIFactory actual = jsonSchemaFactory.getUriFactory();

        // Then
        assertNotNull(actual, "The returned URIFactory should not be null");
        assertSame(expectedUriFactory, actual,
                "JsonSchemaFactory#getUriFactory should return the instance supplied at construction");
    }
}
```