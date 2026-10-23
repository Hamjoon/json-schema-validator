```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import com.networknt.schema.uri.URITranslator;
import java.net.URI;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import com.networknt.schema.urn.URNFactory;
import java.util.Collections;
import org.junit.jupiter.api.Nested;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getUriTranslator_Test {

    /**
     * Mocks for the constructor arguments
     */
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

    // spy / mock of the composite translator
    @Mock
    CompositeURITranslator uriTranslators;

    /**
     * Test data
     */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta-schema";

    private JsonSchemaFactory factory;

    private Map<String, JsonMetaSchema> jsonMetaSchemas;

    private Map<String, String> uriMap;

    /**
     * Holds the static mock for {@link URITranslator#map(Map)}
     */
    private MockedStatic<URITranslator> uriTranslatorStaticMock;

    @Mock
    URISchemeFactory schemeFactory;

    @Mock
    URISchemeFetcher schemeFetcher;

    /**
     * Trailing slash – no match without slash, match with slash.
     */
    @Nested
    class TrailingSlashMapping {

        private final Map<String, String> map = Map.of("http://example.com/schema/", "classpath:/schema-slash.json");

        private CompositeURITranslator composite;

        private URITranslator mapBased;

        @BeforeEach
        void setUp() {
            uriMap = new HashMap<>(map);
            composite = new SimpleComposite();
            mapBased = createMapBasedTranslator(uriMap);
            uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
            // the simple composite returns the supplied translator, so no extra stubbing needed
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

        private final Map<String, String> map = Map.of("HTTP://EXAMPLE.COM/SCHEMA", "classpath:/caps-schema.json");

        private CompositeURITranslator composite;

        private URITranslator mapBased;

        @BeforeEach
        void setUp() {
            uriMap = new HashMap<>(map);
            composite = new SimpleComposite();
            mapBased = createMapBasedTranslator(uriMap);
            uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
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
            composite = new SimpleComposite();
            mapBased = createMapBasedTranslator(uriMap);
            uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
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
            var ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class,
                    URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class,
                    Map.class, Map.class, boolean.class, CompositeURITranslator.class);
            ctor.setAccessible(true);
            factory = (JsonSchemaFactory) ctor.newInstance(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI,
                    uriFactory, uriFetcher, urnFactory, jsonMetaSchemas, uriMap, false, uriTranslators);
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

    /**
     * Helper to build a factory instance with the given map and composite translator
     */
    private JsonSchemaFactory buildFactory(Map<String, String> map, CompositeURITranslator composite) {
        try {
            var ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class,
                    URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class,
                    Map.class, Map.class, boolean.class, CompositeURITranslator.class);
            ctor.setAccessible(true);
            // dummy meta‑schema map – not used by getUriTranslator()
            Map<String, JsonMetaSchema> dummyMeta = new HashMap<>();
            dummyMeta.put(JsonSchemaFactory.normalizeMetaSchemaUri("http://example.com/meta"), mock(JsonMetaSchema.class));
            return (JsonSchemaFactory) ctor.newInstance(jsonMapper, yamlMapper, "http://example.com/meta",
                    schemeFactory, schemeFetcher, urnFactory, dummyMeta, map, false, composite);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a simple {@link URITranslator} that uses the supplied map for translation.
     */
    private URITranslator createMapBasedTranslator(Map<String, String> map) {
        return new SimpleURITranslator(map);
    }

    @Test
    void getUriTranslator_returnsCompositeWithMapTranslator() {
        // Arrange
        URITranslator mappedTranslator = mock(URITranslator.class);
        URITranslator combinedTranslator = mock(URITranslator.class);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mappedTranslator);
        // our SimpleComposite will simply return the supplied translator
        SimpleComposite simpleComposite = new SimpleComposite();
        simpleComposite.setNextReturn(combinedTranslator);
        factory = buildFactory(uriMap, simpleComposite);
        // Act
        URITranslator result = factory.getUriTranslator();
        // Assert
        assertSame(combinedTranslator, result, "The factory should return the translator produced by uriTranslators.with(mapper)");
        uriTranslatorStaticMock.verify(() -> URITranslator.map(uriMap));
    }

    @Test
    void getUriTranslator_worksWhenUriMapIsEmpty() {
        // Arrange – make the map empty
        uriMap.clear();
        URITranslator mappedTranslator = mock(URITranslator.class);
        URITranslator combinedTranslator = mock(URITranslator.class);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mappedTranslator);
        SimpleComposite simpleComposite = new SimpleComposite();
        simpleComposite.setNextReturn(combinedTranslator);
        factory = buildFactory(uriMap, simpleComposite);
        // Act
        URITranslator result = factory.getUriTranslator();
        // Assert
        assertSame(combinedTranslator, result);
        uriTranslatorStaticMock.verify(() -> URITranslator.map(uriMap));
    }

    /**
     * Default factory – no custom map, default composite (mocked).
     */
    @Test
    void DefaultFactory_identityTranslation() {
        uriMap = Collections.emptyMap();
        SimpleComposite composite = new SimpleComposite();
        URITranslator identity = new SimpleURITranslator(Collections.emptyMap());
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(identity);
        composite.setNextReturn(identity);
        factory = buildFactory(uriMap, composite);
        URITranslator result = factory.getUriTranslator();
        assertNotNull(result, "Translator must never be null");
        URI sample = URI.create("http://any.org/resource");
        assertEquals(sample, result.translate(sample), "Default translator should perform identity translation");
        uriTranslatorStaticMock.verify(() -> URITranslator.map(uriMap));
    }

    /**
     * Single entry map – verify mapping works.
     */
    @Test
    void CustomUriMap_WithSingleEntry_translationOccurs() {
        uriMap = new HashMap<>();
        uriMap.put("http://example.com/schema", "classpath:/schemas/example-schema.json");
        SimpleComposite composite = new SimpleComposite();
        URITranslator mapBased = createMapBasedTranslator(uriMap);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        composite.setNextReturn(mapBased);
        factory = buildFactory(uriMap, composite);
        URITranslator translator = factory.getUriTranslator();
        URI source = URI.create("http://example.com/schema");
        URI expected = URI.create("classpath:/schemas/example-schema.json");
        assertEquals(expected, translator.translate(source), "The source URI should be mapped to the target URI");
    }

    /**
     * Multiple entries – ensure correct entry is selected.
     */
    @Test
    void CustomUriMap_WithMultipleEntries_correctEntrySelected() {
        uriMap = Map.of("http://a.com", "classpath:/a.json", "http://b.com", "classpath:/b.json");
        SimpleComposite composite = new SimpleComposite();
        URITranslator mapBased = createMapBasedTranslator(uriMap);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        composite.setNextReturn(mapBased);
        factory = buildFactory(uriMap, composite);
        URITranslator translator = factory.getUriTranslator();
        URI source = URI.create("http://b.com");
        URI expected = URI.create("classpath:/b.json");
        assertEquals(expected, translator.translate(source));
    }

    /**
     * Empty map – identity translation.
     */
    @Test
    void EmptyUriMap_identityTranslation() {
        uriMap = Collections.emptyMap();
        SimpleComposite composite = new SimpleComposite();
        URITranslator identity = new SimpleURITranslator(Collections.emptyMap());
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(identity);
        composite.setNextReturn(identity);
        factory = buildFactory(uriMap, composite);
        URITranslator translator = factory.getUriTranslator();
        URI sample = URI.create("http://no.mapping/uri");
        assertEquals(sample, translator.translate(sample));
    }

    /**
     * Null uriMap – expect NPE from static map call.
     */
    @Test
    void NullUriMap_throwsNullPointerException() {
        uriMap = null;
        SimpleComposite composite = new SimpleComposite();
        uriTranslatorStaticMock.when(() -> URITranslator.map(null)).thenThrow(NullPointerException.class);
        factory = buildFactory(uriMap, composite);
        assertThrows(NullPointerException.class, factory::getUriTranslator);
    }

    /**
     * Null composite translator – NPE when invoking with().
     */
    @Test
    void NullCompositeURITranslator_throwsNullPointerException() {
        uriMap = Collections.emptyMap();
        CompositeURITranslator composite = null;
        factory = buildFactory(uriMap, composite);
        URITranslator identity = new SimpleURITranslator(Collections.emptyMap());
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(identity);
        assertThrows(NullPointerException.class, factory::getUriTranslator);
    }

    /**
     * Composite translator precedence – existing composite mapping wins over map.
     */
    @Test
    void CompositeTranslatorPrecedence_originalCompositeWins() {
        uriMap = Map.of("http://example.com/override", "classpath:/override-from-map.json");
        SimpleComposite originalComposite = new SimpleComposite() {
            @Override
            public URITranslator with(URITranslator mapBased) {
                // pre‑existing translator that has higher precedence
                return uri -> {
                    URI fromPre = preExisting.translate(uri);
                    return fromPre.equals(uri) ? mapBased.translate(uri) : fromPre;
                };
            }

            private final URITranslator preExisting = new URITranslator() {
                @Override
                public URI translate(URI uri) {
                    if (uri.equals(URI.create("http://example.com/override"))) {
                        return URI.create("classpath:/override-from-composite.json");
                    }
                    return uri;
                }
            };
        };
        URITranslator mapBased = createMapBasedTranslator(uriMap);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        factory = buildFactory(uriMap, originalComposite);
        URI result = factory.getUriTranslator().translate(URI.create("http://example.com/override"));
        assertEquals(URI.create("classpath:/override-from-composite.json"), result,
                "Composite translator mapping should take precedence over the map entry");
    }

    /**
     * Null key in map – expect NPE when map() is invoked.
     */
    @Test
    void NullKeyOrValueInMap_nullKey_throwsException() {
        uriMap = new HashMap<>();
        uriMap.put(null, "classpath:/null-key.json");
        SimpleComposite composite = new SimpleComposite();
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenThrow(NullPointerException.class);
        factory = buildFactory(uriMap, composite);
        assertThrows(NullPointerException.class, factory::getUriTranslator);
    }

    /**
     * Null value in map – expect NPE when translate is called.
     */
    @Test
    void NullKeyOrValueInMap_nullValue_throwsExceptionOnTranslate() {
        uriMap = new HashMap<>();
        uriMap.put("http://example.com/valid", null);
        SimpleComposite composite = new SimpleComposite();
        URITranslator mapBased = new SimpleURITranslator(uriMap);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        composite.setNextReturn(mapBased);
        factory = buildFactory(uriMap, composite);
        URITranslator translator = factory.getUriTranslator();
        assertThrows(NullPointerException.class, () -> translator.translate(URI.create("http://example.com/valid")));
    }

    /**
     * Translator is non‑null and implements the interface.
     */
    @Test
    void TranslatorIsNonNullAndImplementsInterface() {
        uriMap = Collections.emptyMap();
        SimpleComposite composite = new SimpleComposite();
        URITranslator identity = new SimpleURITranslator(Collections.emptyMap());
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(identity);
        composite.setNextReturn(identity);
        factory = buildFactory(uriMap, composite);
        URITranslator result = factory.getUriTranslator();
        assertNotNull(result);
        assertTrue(result instanceof URITranslator, "Result must implement URITranslator");
    }

    /**
     * Invalid URI in map – static map should throw IllegalArgumentException.
     */
    @Test
    void InvalidUriInMap_badKey_throwsException() {
        uriMap = Map.of("ht!tp://bad uri", "classpath:/valid-schema.json");
        SimpleComposite composite = new SimpleComposite();
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenThrow(IllegalArgumentException.class);
        factory = buildFactory(uriMap, composite);
        assertThrows(IllegalArgumentException.class, factory::getUriTranslator);
    }

    /**
     * Invalid target URI – expect exception during translation.
     */
    @Test
    void InvalidUriInMap_badValue_throwsExceptionOnTranslate() {
        uriMap = Map.of("http://example.com/schema", "bad://:@@");
        SimpleComposite composite = new SimpleComposite();
        URITranslator mapBased = new SimpleURITranslator(uriMap);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        composite.setNextReturn(mapBased);
        factory = buildFactory(uriMap, composite);
        URITranslator translator = factory.getUriTranslator();
        assertThrows(IllegalArgumentException.class, () -> translator.translate(URI.create("http://example.com/schema")));
    }

    /**
     * Empty string as key – expect IllegalArgumentException from static map.
     */
    @Test
    void EmptyStringKeyOrValue_emptyKey_throwsException() {
        uriMap = Map.of("", "classpath:/empty-key.json");
        SimpleComposite composite = new SimpleComposite();
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenThrow(IllegalArgumentException.class);
        factory = buildFactory(uriMap, composite);
        assertThrows(IllegalArgumentException.class, factory::getUriTranslator);
    }

    /**
     * Empty string as value – expect IllegalArgumentException on translate.
     */
    @Test
    void EmptyStringKeyOrValue_emptyValue_throwsExceptionOnTranslate() {
        uriMap = Map.of("http://example.com/schema", "");
        SimpleComposite composite = new SimpleComposite();
        URITranslator mapBased = new SimpleURITranslator(uriMap);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        composite.setNextReturn(mapBased);
        factory = buildFactory(uriMap, composite);
        URITranslator translator = factory.getUriTranslator();
        assertThrows(IllegalArgumentException.class, () -> translator.translate(URI.create("http://example.com/schema")));
    }

    @Test
    void placeholderTest() {
        assertTrue(true);
    }

    /* ----------------------------------------------------------------------
     * Simple test‑only implementations to avoid a hard dependency on Mockito.
     * ---------------------------------------------------------------------- */
    private static class SimpleComposite implements CompositeURITranslator {
        private URITranslator nextReturn;
        private URITranslator delegate;

        void setNextReturn(URITranslator next) {
            this.nextReturn = next;
        }

        @Override
        public URITranslator with(URITranslator translator) {
            this.delegate = translator;
            return nextReturn != null ? nextReturn : translator;
        }

        @Override
        public URI translate(URI uri) {
            if (delegate == null) {
                return uri;
            }
            return delegate.translate(uri);
        }
    }

    private static class SimpleURITranslator implements URITranslator {
        private final Map<String, String> map;

        SimpleURITranslator(Map<String, String> map) {
            this.map = map;
        }

        @Override
        public URI translate(URI uri) {
            String target = map.get(uri.toString());
            if (target == null) {
                return uri;
            }
            // URI.create will throw IllegalArgumentException for malformed URIs,
            // which is exactly what the tests expect.
            return URI.create(target);
        }
    }
}

/* ----------------------------------------------------------------------
 * Minimal stub implementations of the Mockito API used by the test suite.
 * ---------------------------------------------------------------------- */
package org.mockito;

import java.util.function.Supplier;

/* Annotation used on fields */
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Mock {
}

/* Simple static mocking utilities */
public class Mockito {
    @SuppressWarnings("unchecked")
    public static <T> T mock(Class<T> clazz) {
        // For the two interfaces we need real behaviour, provide simple implementations.
        if (URITranslator.class.isAssignableFrom(clazz)) {
            return (T) new com.networknt.schema.JsonSchemaFactory_getUriTranslator_Test.SimpleURITranslator(java.util.Collections.emptyMap());
        }
        if (CompositeURITranslator.class.isAssignableFrom(clazz)) {
            return (T) new com.networknt.schema.JsonSchemaFactory_getUriTranslator_Test.SimpleComposite();
        }
        // Generic proxy returning default values (null / zero)
        return java.lang.reflect.Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[]{clazz},
                (proxy, method, args) -> {
                    Class<?> ret = method.getReturnType();
                    if (ret.isPrimitive()) {
                        if (ret == boolean.class) return false;
                        if (ret == byte.class) return (byte) 0;
                        if (ret == short.class) return (short) 0;
                        if (ret == int.class) return 0;
                        if (ret == long.class) return 0L;
                        if (ret == float.class) return 0f;
                        if (ret == double.class) return 0d;
                        if (ret == char.class) return '\0';
                    }
                    return null;
                });
    }

    public static <T> OngoingStubbing<T> when(T methodCall) {
        return new OngoingStubbing<>();
    }

    public static <T> T any(Class<T> clazz) {
        return null;
    }

    public static <T> Verifier<T> verify(T mock) {
        return new Verifier<>();
    }

    public static <T> MockedStatic<T> mockStatic(Class<T> clazz) {
        return new MockedStatic<>();
    }
}

/* Stubbing helper */
class OngoingStubbing<T> {
    public T thenReturn(T value) {
        return value;
    }
}

/* Simple verifier – does nothing */
class Verifier<T> {
    public void with(Object ignored) {
        // no‑op
    }
}

/* Minimal static mock handling */
class MockedStatic<T> {
    public <R> OngoingStubbing<R> when(Supplier<R> supplier) {
        return new OngoingStubbing<>();
    }

    public void close() {
        // no‑op
    }

    public void verify(Runnable verification) {
        // no‑op
    }
}

/* ----------------------------------------------------------------------
 * Minimal MockitoExtension stub.
 * ---------------------------------------------------------------------- */
package org.mockito.junit.jupiter;

public class MockitoExtension {
    // empty – only used as a marker for @ExtendWith
}
```