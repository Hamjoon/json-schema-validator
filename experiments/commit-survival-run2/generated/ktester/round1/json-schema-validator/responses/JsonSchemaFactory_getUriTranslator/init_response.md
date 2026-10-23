**Generated test class**

```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchemaFactory#getUriTranslator()}.
 *
 * The test creates a {@link JsonSchemaFactory} instance via reflection
 * (the constructor is private) and supplies all required collaborators
 * as Mockito mocks.  The static method {@code URITranslator.map(..)} is
 * mocked with {@code MockedStatic} so that we can verify that the
 * {@code CompositeURITranslator#with(..)} method is invoked with the
 * mapper produced from the {@code uriMap}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getUriTranslator_Test {

    /** Mocks for the constructor arguments */
    @Mock ObjectMapper jsonMapper;
    @Mock YAMLMapper yamlMapper;
    @Mock URISchemeFactory uriFactory;
    @Mock URISchemeFetcher uriFetcher;
    @Mock URNFactory urnFactory;
    @Mock CompositeURITranslator uriTranslators;   // spy / mock of the composite translator

    /** Test data */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta-schema";

    private JsonSchemaFactory factory;
    private Map<String, JsonMetaSchema> jsonMetaSchemas;
    private Map<String, String> uriMap;

    /** Holds the static mock for {@link URITranslator#map(Map)} */
    private MockedStatic<URITranslator> uriTranslatorStaticMock;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // ---- initialise simple maps -------------------------------------------------
        // a minimal jsonMetaSchemas map containing the default meta‑schema entry
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
        jsonMetaSchemas = new HashMap<>();
        jsonMetaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMetaSchema);

        // an example uriMap (can be empty – the logic under test does not depend on its content)
        uriMap = new HashMap<>();
        uriMap.put("http://old.com", "http://new.com");

        // ---- create the JsonSchemaFactory instance via reflection --------------------
        try {
            var ctor = JsonSchemaFactory.class.getDeclaredConstructor(
                    ObjectMapper.class,
                    YAMLMapper.class,
                    String.class,
                    URISchemeFactory.class,
                    URISchemeFetcher.class,
                    URNFactory.class,
                    Map.class,
                    Map.class,
                    boolean.class,
                    CompositeURITranslator.class);
            ctor.setAccessible(true);
            factory = (JsonSchemaFactory) ctor.newInstance(
                    jsonMapper,
                    yamlMapper,
                    DEFAULT_META_SCHEMA_URI,
                    uriFactory,
                    uriFetcher,
                    urnFactory,
                    jsonMetaSchemas,
                    uriMap,
                    false,                     // enableUriSchemaCache – not relevant for this test
                    uriTranslators);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // ---- mock the static URITranslator.map method ---------------------------------
        uriTranslatorStaticMock = mockStatic(URITranslator.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // release the static mock; otherwise it leaks into other tests
        if (uriTranslatorStaticMock != null) {
            uriTranslatorStaticMock.close();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean.
    }

    @Test
    void getUriTranslator_returnsCompositeWithMapTranslator() {
        // -------------------------------------------------------------------------
        // Arrange – mock the static map() call and the CompositeURITranslator#with()
        // -------------------------------------------------------------------------
        URITranslator mappedTranslator = mock(URITranslator.class);
        URITranslator combinedTranslator = mock(URITranslator.class);

        // static call: URITranslator.map(uriMap) → mappedTranslator
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap))
                               .thenReturn(mappedTranslator);

        // instance call: uriTranslators.with(mappedTranslator) → combinedTranslator
        when(uriTranslators.with(mappedTranslator)).thenReturn(combinedTranslator);

        // -------------------------------------------------------------------------
        // Act
        // -------------------------------------------------------------------------
        URITranslator result = factory.getUriTranslator();

        // -------------------------------------------------------------------------
        // Assert
        // -------------------------------------------------------------------------
        assertSame(combinedTranslator, result,
                "The factory should return the translator produced by uriTranslators.with(mapper)");

        // verify that the interactions happened exactly as expected
        uriTranslatorStaticMock.verify(() -> URITranslator.map(uriMap));
        verify(uriTranslators).with(mappedTranslator);
    }

    @Test
    void getUriTranslator_worksWhenUriMapIsEmpty() {
        // -------------------------------------------------------------------------
        // Arrange – replace the uriMap with an empty map and repeat the stubbing
        // -------------------------------------------------------------------------
        uriMap.clear(); // make it empty

        URITranslator mappedTranslator = mock(URITranslator.class);
        URITranslator combinedTranslator = mock(URITranslator.class);

        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap))
                               .thenReturn(mappedTranslator);
        when(uriTranslators.with(mappedTranslator)).thenReturn(combinedTranslator);

        // -------------------------------------------------------------------------
        // Act
        // -------------------------------------------------------------------------
        URITranslator result = factory.getUriTranslator();

        // -------------------------------------------------------------------------
        // Assert
        // -------------------------------------------------------------------------
        assertSame(combinedTranslator, result);
        uriTranslatorStaticMock.verify(() -> URITranslator.map(uriMap));
        verify(uriTranslators).with(mappedTranslator);
    }
}
```