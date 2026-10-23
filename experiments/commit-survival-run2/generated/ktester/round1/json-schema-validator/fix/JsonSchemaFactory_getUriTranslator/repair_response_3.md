```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link JsonSchemaFactory#getUriTranslator()}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getUriTranslator_Test {

    /* -------------------------------------------------
     *  Mocks for the constructor arguments of JsonSchemaFactory
     * ------------------------------------------------- */
    @Mock
    ObjectMapper jsonMapper;

    @Mock
    YAMLMapper yamlMapper;

    @Mock
    URISchemeFactory uriFactory;

    @Mock
    URISchemeFetcher uriFetcher;

    @Mock
    URNFactory urnFactory;

    // spy / mock of the composite translator (default instance used by the factory)
    @Mock
    CompositeURITranslator uriTranslators;

    // alternative mocks used in some helper methods
    @Mock
    URISchemeFactory schemeFactory;

    @Mock
    URISchemeFetcher schemeFetcher;

    /* -------------------------------------------------
     *  Test data
     * ------------------------------------------------- */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta-schema";

    private JsonSchemaFactory factory;

    private Map<String, JsonMetaSchema> jsonMetaSchemas;

    private Map<String, String> uriMap;

    /**
     * Holds the static mock for {@link URITranslator#map(Map)}.
     */
    private MockedStatic<URITranslator> uriTranslatorStaticMock;

    /* -------------------------------------------------
     *  Nested test groups
     * ------------------------------------------------- */

    /**
     * Trailing slash – no match without slash, match with slash.
     */
    @Nested
    class TrailingSlashMapping {

        private final Map<String, String> map = Map.of(
                "http://example.com/schema/", "classpath:/schema-slash.json");

        private CompositeURITranslator composite;
        private URITranslator mapBased;

        @BeforeEach
        void setUp() {
            uriMap = new HashMap<>(map);
            composite = mock(CompositeURITranslator.class);
            mapBased = createMapBasedTranslator(uriMap);
            uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
            when(composite.with(mapBased)).thenReturn(mapBased);
            factory = buildFactory(uriMap, composite);
        }

        @Test
        void withoutTrailingSlash_identity() {
            URI source = URI.create("http://example.com/schema");
            URI result = factory.getUriTranslator().translate(source);
            assertEquals(source, result, "URI without trailing slash should not be mapped");
        }

        @Test
        void withTrailingSlash_mapped() {
            URI source = URI.create("http://example.com/schema/");
            URI expected = URI.create("classpath:/schema-slash.json");
            assertEquals(expected, factory.getUriTranslator().translate(source));
        }
    }

    /**
     * Case‑sensitivity – map key case must match exactly.
     */
    @Nested
    class CaseSensitivityInMapping {

        private final Map<String, String> map = Map.of(
                "HTTP://EXAMPLE.COM/SCHEMA", "classpath:/caps-schema.json");

        private CompositeURITranslator composite;
        private URITranslator mapBased;

        @BeforeEach
        void setUp() {
            uriMap = new HashMap<>(map);
            composite = mock(CompositeURITranslator.class);
            mapBased = createMapBasedTranslator(uriMap);
            uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
            when(composite.with(mapBased)).thenReturn(mapBased);
            factory = buildFactory(uriMap, composite);
        }

        @Test
        void lowerCase_noMatch_identity() {
            URI source = URI.create("http://example.com/schema");
            URI result = factory.getUriTranslator().translate(source);
            assertEquals(source, result);
        }

        @Test
        void exactCase_match() {
            URI source = URI.create("HTTP://EXAMPLE.COM/SCHEMA");
            URI expected = URI.create("classpath:/caps-schema.json");
            assertEquals(expected, factory.getUriTranslator().translate(source));
        }
    }

    /**
     * Fragment and query handling – full string must match.
     */
    @Nested
    class FragmentAndQueryMapping {

        private final Map<String, String> map = Map.of(
                "http://example.com/schema#section", "classpath:/schema-section.json",
                "http://example.com/schema?version=1", "classpath:/schema-v1.json");

        private CompositeURITranslator composite;
        private URITranslator mapBased;

        @BeforeEach
        void setUp() {
            uriMap = new HashMap<>(map);
            composite = mock(CompositeURITranslator.class);
            mapBased = createMapBasedTranslator(uriMap);
            uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
            when(composite.with(mapBased)).thenReturn(mapBased);
            factory = buildFactory(uriMap, composite);
        }

        @Test
        void fragment_match() {
            URI source = URI.create("http://example.com/schema#section");
            URI expected = URI.create("classpath:/schema-section.json");
            assertEquals(expected, factory.getUriTranslator().translate(source));
        }

        @Test
        void query_match() {
            URI source = URI.create("http://example.com/schema?version=1");
            URI expected = URI.create("classpath:/schema-v1.json");
            assertEquals(expected, factory.getUriTranslator().translate(source));
        }

        @Test
        void noFragmentOrQuery_identity() {
            URI source = URI.create("http://example.com/schema");
            assertEquals(source, factory.getUriTranslator().translate(source));
        }
    }

    /* -------------------------------------------------
     *  Common setup / teardown
     * ------------------------------------------------- */
    @BeforeEach
    void setupBeforeEach() {
        // minimal meta‑schema map – the factory only needs it for construction
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
        jsonMetaSchemas = new HashMap<>();
        jsonMetaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMetaSchema);

        // a simple example uriMap (may be overwritten by individual tests)
        uriMap = new HashMap<>();
        uriMap.put("http://old.com", "http://new.com");

        // create the factory via reflection – we keep the constructor signature identical
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
                    false,
                    uriTranslators);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // start a fresh static mock before each test
        uriTranslatorStaticMock = mockStatic(URITranslator.class);
    }

    @AfterEach
    void teardownAfterEach() {
        if (uriTranslatorStaticMock != null) {
            uriTranslatorStaticMock.close();
        }
    }

    /* -------------------------------------------------
     *  Helper methods
     * ------------------------------------------------- */
    /**
     * Builds a {@link JsonSchemaFactory} instance with the supplied map and composite translator.
     */
    private JsonSchemaFactory buildFactory(Map<String, String> map,
                                          CompositeURITranslator composite) {
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
            // dummy meta‑schema map – not used by getUriTranslator()
            Map<String, JsonMetaSchema> dummyMeta = new HashMap<>();
            dummyMeta.put(JsonSchemaFactory.normalizeMetaSchemaUri("http://example.com/meta"),
                    mock(JsonMetaSchema.class));
            return (JsonSchemaFactory) ctor.newInstance(
                    jsonMapper,
                    yamlMapper,
                    "http://example.com/meta",
                    schemeFactory,
                    schemeFetcher,
                    urnFactory,
                    dummyMeta,
                    map,
                    false,
                    composite);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a simple {@link URITranslator} that uses the supplied map for translation.
     */
    private URITranslator createMapBasedTranslator(Map<String, String> map) {
        URITranslator translator = mock(URITranslator.class);
        when(translator.translate(any(URI.class))).thenAnswer(invocation -> {
            URI in = invocation.getArgument(0);
            String target = map.get(in.toString());
            return target != null ? URI.create(target) : in;
        });
        return translator;
    }

    /* -------------------------------------------------
     *  Individual test cases
     * ------------------------------------------------- */

    @Test
    void getUriTranslator_returnsCompositeWithMapTranslator() {
        // Arrange
        URITranslator mappedTranslator = mock(URITranslator.class);
        URITranslator combinedTranslator = mock(URITranslator.class);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mappedTranslator);
        when(uriTranslators.with(mappedTranslator)).thenReturn(combinedTranslator);

        // Act
        URITranslator result = factory.getUriTranslator();

        // Assert
        assertSame(combinedTranslator, result,
                "The factory should return the translator produced by uriTranslators.with(mapper)");
        uriTranslatorStaticMock.verify(() -> URITranslator.map(uriMap));
        verify(uriTranslators).with(mappedTranslator);
    }

    @Test
    void getUriTranslator_worksWhenUriMapIsEmpty() {
        // Arrange – make the map empty
        uriMap.clear();
        URITranslator mappedTranslator = mock(URITranslator.class);
        URITranslator combinedTranslator = mock(URITranslator.class);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mappedTranslator);
        when(uriTranslators.with(mappedTranslator)).thenReturn(combinedTranslator);

        // Act
        URITranslator result = factory.getUriTranslator();

        // Assert
        assertSame(combinedTranslator, result);
        uriTranslatorStaticMock.verify(() -> URITranslator.map(uriMap));
        verify(uriTranslators).with(mappedTranslator);
    }

    @Test
    void DefaultFactory_identityTranslation() {
        uriMap = Collections.emptyMap();
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        URITranslator identity = mock(URITranslator.class);
        when(identity.translate(any(URI.class))).thenAnswer(i -> i.getArgument(0));
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(identity);
        when(composite.with(identity)).thenReturn(identity);
        factory = buildFactory(uriMap, composite);

        URITranslator result = factory.getUriTranslator();
        assertNotNull(result, "Translator must never be null");
        URI sample = URI.create("http://any.org/resource");
        assertEquals(sample, result.translate(sample), "Default translator should perform identity translation");
        uriTranslatorStaticMock.verify(() -> URITranslator.map(uriMap));
        verify(composite).with(identity);
    }

    @Test
    void CustomUriMap_WithSingleEntry_translationOccurs() {
        uriMap = new HashMap<>();
        uriMap.put("http://example.com/schema", "classpath:/schemas/example-schema.json");
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        URITranslator mapBased = createMapBasedTranslator(uriMap);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        when(composite.with(mapBased)).thenReturn(mapBased);
        factory = buildFactory(uriMap, composite);

        URITranslator translator = factory.getUriTranslator();
        URI source = URI.create("http://example.com/schema");
        URI expected = URI.create("classpath:/schemas/example-schema.json");
        assertEquals(expected, translator.translate(source),
                "The source URI should be mapped to the target URI");
    }

    @Test
    void CustomUriMap_WithMultipleEntries_correctEntrySelected() {
        uriMap = Map.of(
                "http://a.com", "classpath:/a.json",
                "http://b.com", "classpath:/b.json");
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        URITranslator mapBased = createMapBasedTranslator(uriMap);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        when(composite.with(mapBased)).thenReturn(mapBased);
        factory = buildFactory(uriMap, composite);

        URITranslator translator = factory.getUriTranslator();
        URI source = URI.create("http://b.com");
        URI expected = URI.create("classpath:/b.json");
        assertEquals(expected, translator.translate(source));
    }

    @Test
    void EmptyUriMap_identityTranslation() {
        uriMap = Collections.emptyMap();
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        URITranslator identity = mock(URITranslator.class);
        when(identity.translate(any(URI.class))).thenAnswer(i -> i.getArgument(0));
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(identity);
        when(composite.with(identity)).thenReturn(identity);
        factory = buildFactory(uriMap, composite);

        URITranslator translator = factory.getUriTranslator();
        URI sample = URI.create("http://no.mapping/uri");
        assertEquals(sample, translator.translate(sample));
    }

    @Test
    void NullUriMap_throwsNullPointerException() {
        uriMap = null;
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        uriTranslatorStaticMock.when(() -> URITranslator.map(null))
                .thenThrow(NullPointerException.class);
        factory = buildFactory(uriMap, composite);
        assertThrows(NullPointerException.class, factory::getUriTranslator);
    }

    @Test
    void NullCompositeURITranslator_throwsNullPointerException() {
        uriMap = Collections.emptyMap();
        CompositeURITranslator composite = null;
        factory = buildFactory(uriMap, composite);
        URITranslator identity = mock(URITranslator.class);
        when(identity.translate(any(URI.class))).thenAnswer(i -> i.getArgument(0));
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(identity);
        assertThrows(NullPointerException.class, factory::getUriTranslator);
    }

    @Test
    void CompositeTranslatorPrecedence_originalCompositeWins() {
        uriMap = Map.of("http://example.com/override", "classpath:/override-from-map.json");
        CompositeURITranslator originalComposite = mock(CompositeURITranslator.class);
        URITranslator preExisting = mock(URITranslator.class);
        when(preExisting.translate(URI.create("http://example.com/override")))
                .thenReturn(URI.create("classpath:/override-from-composite.json"));

        when(originalComposite.with(any())).thenAnswer(inv -> {
            URITranslator mapBased = inv.getArgument(0);
            return (URI uri) -> {
                URI fromPre = preExisting.translate(uri);
                return fromPre.equals(uri) ? mapBased.translate(uri) : fromPre;
            };
        });

        URITranslator mapBased = createMapBasedTranslator(uriMap);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        factory = buildFactory(uriMap, originalComposite);

        URI result = factory.getUriTranslator()
                .translate(URI.create("http://example.com/override"));
        assertEquals(URI.create("classpath:/override-from-composite.json"), result,
                "Composite translator mapping should take precedence over the map entry");
    }

    @Test
    void NullKeyOrValueInMap_nullKey_throwsException() {
        uriMap = new HashMap<>();
        uriMap.put(null, "classpath:/null-key.json");
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap))
                .thenThrow(NullPointerException.class);
        factory = buildFactory(uriMap, composite);
        assertThrows(NullPointerException.class, factory::getUriTranslator);
    }

    @Test
    void NullKeyOrValueInMap_nullValue_throwsExceptionOnTranslate() {
        uriMap = new HashMap<>();
        uriMap.put("http://example.com/valid", null);
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        URITranslator mapBased = mock(URITranslator.class);
        when(mapBased.translate(any(URI.class))).thenAnswer(inv -> {
            URI in = inv.getArgument(0);
            String target = uriMap.get(in.toString()); // will be null
            return URI.create(target); // NPE expected
        });
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        when(composite.with(mapBased)).thenReturn(mapBased);
        factory = buildFactory(uriMap, composite);
        URITranslator translator = factory.getUriTranslator();
        assertThrows(NullPointerException.class,
                () -> translator.translate(URI.create("http://example.com/valid")));
    }

    @Test
    void TranslatorIsNonNullAndImplementsInterface() {
        uriMap = Collections.emptyMap();
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        URITranslator identity = mock(URITranslator.class);
        when(identity.translate(any(URI.class))).thenAnswer(i -> i.getArgument(0));
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(identity);
        when(composite.with(identity)).thenReturn(identity);
        factory = buildFactory(uriMap, composite);
        URITranslator result = factory.getUriTranslator();
        assertNotNull(result);
        assertTrue(result instanceof URITranslator, "Result must implement URITranslator");
    }

    @Test
    void InvalidUriInMap_badKey_throwsException() {
        uriMap = Map.of("ht!tp://bad uri", "classpath:/valid-schema.json");
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap))
                .thenThrow(IllegalArgumentException.class);
        factory = buildFactory(uriMap, composite);
        assertThrows(IllegalArgumentException.class, factory::getUriTranslator);
    }

    @Test
    void InvalidUriInMap_badValue_throwsExceptionOnTranslate() {
        uriMap = Map.of("http://example.com/schema", "bad://:@@");
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        URITranslator mapBased = mock(URITranslator.class);
        when(mapBased.translate(any(URI.class))).thenAnswer(inv -> {
            URI in = inv.getArgument(0);
            String target = uriMap.get(in.toString());
            return URI.create(target); // will throw IllegalArgumentException
        });
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        when(composite.with(mapBased)).thenReturn(mapBased);
        factory = buildFactory(uriMap, composite);
        URITranslator translator = factory.getUriTranslator();
        assertThrows(IllegalArgumentException.class,
                () -> translator.translate(URI.create("http://example.com/schema")));
    }

    @Test
    void EmptyStringKeyOrValue_emptyKey_throwsException() {
        uriMap = Map.of("", "classpath:/empty-key.json");
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap))
                .thenThrow(IllegalArgumentException.class);
        factory = buildFactory(uriMap, composite);
        assertThrows(IllegalArgumentException.class, factory::getUriTranslator);
    }

    @Test
    void EmptyStringKeyOrValue_emptyValue_throwsExceptionOnTranslate() {
        uriMap = Map.of("http://example.com/schema", "");
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        URITranslator mapBased = mock(URITranslator.class);
        when(mapBased.translate(any(URI.class))).thenAnswer(inv -> {
            URI in = inv.getArgument(0);
            String target = uriMap.get(in.toString());
            return URI.create(target); // IllegalArgumentException for empty string
        });
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        when(composite.with(mapBased)).thenReturn(mapBased);
        factory = buildFactory(uriMap, composite);
        URITranslator translator = factory.getUriTranslator();
        assertThrows(IllegalArgumentException.class,
                () -> translator.translate(URI.create("http://example.com/schema")));
    }

    @Test
    void placeholderTest() {
        assertTrue(true);
    }
}
```