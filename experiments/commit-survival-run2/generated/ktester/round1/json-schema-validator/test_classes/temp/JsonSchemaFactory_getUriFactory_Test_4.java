package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import com.networknt.schema.uri.URIFactory;
import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getUriFactory_Test {

    /**
     * Instance under test
     */
    private JsonSchemaFactory jsonSchemaFactory;

    /**
     * The factory we expect to be returned by {@code getUriFactory()}
     */
    private URIFactory expectedUriFactory;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----- Minimal but valid collaborators ---------------------------------
        // real mapper – no external I/O
        ObjectMapper jsonMapper = new ObjectMapper();
        // real mapper – no external I/O
        YAMLMapper yamlMapper = new YAMLMapper();
        String defaultMetaSchemaURI = "http://example.com/meta/schema";
        // Empty meta‑schemas map – the constructor does not require any entries for
        // the behaviour we are testing (getUriFactory)
        Map<String, JsonMetaSchema> jsonMetaSchemas = Collections.emptyMap();
        // Empty URI‑to‑URI mapping (allowed by the constructor)
        Map<String, String> uriMap = Collections.emptyMap();
        // The objects we want to verify are returned unchanged
        expectedUriFactory = new URIFactory() {

            @Override
            public URI create(String uri) {
                return URI.create(uri);
            }

            @Override
            public URI create(URI base, String reference) {
                // Simple implementation that resolves against the base using URI.resolve
                return base.resolve(reference);
            }
        };
        // The scheme‑factory and fetcher can be empty – they are not exercised here
        URISchemeFactory uriSchemeFactory = new URISchemeFactory(Collections.emptyMap());
        URISchemeFetcher uriSchemeFetcher = new URISchemeFetcher(Collections.emptyMap());
        // Provide a minimal concrete URNFactory implementation
        URNFactory urnFactory = new URNFactory() {

            @Override
            public URI create(String urn) {
                return URI.create(urn);
            }
        };
        // Composite translator – we can use the default constructor
        URITranslator.CompositeURITranslator uriTranslators = new URITranslator.CompositeURITranslator();
        // ----- Instantiate JsonSchemaFactory via reflection --------------------
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, URITranslator.CompositeURITranslator.class);
        ctor.setAccessible(true);
        jsonSchemaFactory = ctor.newInstance(jsonMapper, yamlMapper, defaultMetaSchemaURI, uriSchemeFactory, uriSchemeFetcher, urnFactory, jsonMetaSchemas, uriMap, false, uriTranslators);
        // Replace the private field `uriFactory` with our expected instance using reflection.
        java.lang.reflect.Field uriFactoryField = JsonSchemaFactory.class.getDeclaredField("uriFactory");
        uriFactoryField.setAccessible(true);
        uriFactoryField.set(jsonSchemaFactory, expectedUriFactory);
    }

    @AfterEach
    void teardownAfterEach() {
        jsonSchemaFactory = null;
        expectedUriFactory = null;
    }

    @Test
    void getUriFactory_returnsConfiguredFactory() {
        // When
        URIFactory actual = jsonSchemaFactory.getUriFactory();
        // Then
        assertNotNull(actual, "The returned URIFactory should not be null");
        assertSame(expectedUriFactory, actual, "JsonSchemaFactory#getUriFactory should return the instance supplied at construction");
    }

    /* --------------------------------------------------------------
     *  1. Default builder creates a non‑null default URIFactory.
     * -------------------------------------------------------------- */
    @Test
    void defaultBuilder_returnsNonNullDefaultUriFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder().build();
        URIFactory uriFactory = factory.getUriFactory();
        assertNotNull(uriFactory, "Default builder must provide a non‑null URIFactory");
        // Repeated calls must give the same reference.
        assertSame(uriFactory, factory.getUriFactory(), "Repeated getUriFactory() calls should return the identical instance");
    }

    /* --------------------------------------------------------------
     *  2. Custom URIFactory supplied via builder is returned unchanged.
     * -------------------------------------------------------------- */
    @Test
    void customUriFactory_isReturnedUnmodified() {
        // Arrange – a lightweight custom implementation
        URIFactory customFactory = new URIFactory() {

            @Override
            public URI create(String uri) {
                return URI.create(uri);
            }

            @Override
            public URI create(URI base, String reference) {
                return base.resolve(reference);
            }
        };
        // Act
        JsonSchemaFactory factory = JsonSchemaFactory.builder().uriFactory(// builder API assumed to exist
        customFactory).build();
        // Assert
        assertSame(customFactory, factory.getUriFactory(), "The factory returned by getUriFactory() must be the exact instance supplied to the builder");
    }

    /* --------------------------------------------------------------
     *  3. Blueprint inheritance – custom factory.
     * -------------------------------------------------------------- */
    @Test
    void blueprintInheritance_customFactory_isPreserved() {
        // Arrange – build a blueprint with a custom factory
        URIFactory customFactory = new URIFactory() {

            @Override
            public URI create(String uri) {
                return URI.create(uri);
            }

            @Override
            public URI create(URI base, String reference) {
                return base.resolve(reference);
            }
        };
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().uriFactory(customFactory).build();
        // Act – create a new factory from the blueprint
        JsonSchemaFactory derived = JsonSchemaFactory.builder(blueprint).build();
        // Assert
        assertSame(customFactory, derived.getUriFactory(), "Derived factory must inherit the custom URIFactory from its blueprint");
    }

    /* --------------------------------------------------------------
     *  4. Blueprint inheritance – default factory.
     * -------------------------------------------------------------- */
    @Test
    void blueprintInheritance_defaultFactory_isShared() {
        // Arrange – blueprint created via the default builder
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().build();
        // Act – derive a second factory from the blueprint
        JsonSchemaFactory derived = JsonSchemaFactory.builder(blueprint).build();
        // Assert – both must expose the *same* URIFactory reference
        assertSame(blueprint.getUriFactory(), derived.getUriFactory(), "Derived factory should share the default URIFactory instance with its blueprint");
    }

    /* --------------------------------------------------------------
     *  5. Multiple calls consistency for various factory configurations.
     * -------------------------------------------------------------- */
    @Test
    void multipleCalls_consistencyAcrossDifferentFactories() {
        // Default factory
        JsonSchemaFactory defaultFactory = JsonSchemaFactory.builder().build();
        assertSame(defaultFactory.getUriFactory(), defaultFactory.getUriFactory(), "Default factory must return identical URIFactory on successive calls");
        // Custom factory
        URIFactory customFactory = new URIFactory() {

            @Override
            public URI create(String uri) {
                return URI.create(uri);
            }

            @Override
            public URI create(URI base, String reference) {
                return base.resolve(reference);
            }
        };
        JsonSchemaFactory custom = JsonSchemaFactory.builder().uriFactory(customFactory).build();
        assertSame(custom.getUriFactory(), custom.getUriFactory(), "Custom factory must return identical URIFactory on successive calls");
        // Blueprint‑derived factory
        JsonSchemaFactory derived = JsonSchemaFactory.builder(custom).build();
        assertSame(derived.getUriFactory(), derived.getUriFactory(), "Blueprint‑derived factory must return identical URIFactory on successive calls");
    }

    /* --------------------------------------------------------------
     *  6. Builder(null) must reject a null blueprint argument.
     * -------------------------------------------------------------- */
    @Test
    void builder_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder((JsonSchemaFactory) null), "Calling builder(null) should throw NullPointerException");
    }
}