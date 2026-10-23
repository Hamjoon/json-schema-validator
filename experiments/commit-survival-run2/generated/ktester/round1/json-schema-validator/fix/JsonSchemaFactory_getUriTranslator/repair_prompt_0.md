
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;
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

        private final Map<String, String> map = Map.of("HTTP://EXAMPLE.COM/SCHEMA", "classpath:/caps-schema.json");

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

        private final Map<String, String> map = Map.of("http://example.com/schema#section", "classpath:/schema-section.json", "http://example.com/schema?version=1", "classpath:/schema-v1.json");

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
            var ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
            ctor.setAccessible(true);
            factory = (JsonSchemaFactory) ctor.newInstance(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, urnFactory, jsonMetaSchemas, uriMap, // enableUriSchemaCache – not relevant for this test
            false, uriTranslators);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // ---- mock the static URITranslator.map method ---------------------------------
        uriTranslatorStaticMock = mockStatic(URITranslator.class);
        // start a fresh static mock before each test
        uriTranslatorStaticMock = mockStatic(URITranslator.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // release the static mock; otherwise it leaks into other tests
        if (uriTranslatorStaticMock != null) {
            uriTranslatorStaticMock.close();
        }
        if (uriTranslatorStaticMock != null) {
            uriTranslatorStaticMock.close();
        }
    }

    /**
     * Helper to build a factory instance with the given map and composite translator
     */
    private JsonSchemaFactory buildFactory(Map<String, String> map, CompositeURITranslator composite) {
        try {
            var ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, CompositeURITranslator.class);
            ctor.setAccessible(true);
            // dummy meta‑schema map – not used by getUriTranslator()
            Map<String, JsonMetaSchema> dummyMeta = new HashMap<>();
            dummyMeta.put(JsonSchemaFactory.normalizeMetaSchemaUri("http://example.com/meta"), mock(JsonMetaSchema.class));
            return (JsonSchemaFactory) ctor.newInstance(jsonMapper, yamlMapper, "http://example.com/meta", schemeFactory, schemeFetcher, urnFactory, dummyMeta, map, false, composite);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a simple {@link URITranslator} that uses the supplied map for translation.
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

    @Test
    void getUriTranslator_returnsCompositeWithMapTranslator() {
        // -------------------------------------------------------------------------
        // Arrange – mock the static map() call and the CompositeURITranslator#with()
        // -------------------------------------------------------------------------
        URITranslator mappedTranslator = mock(URITranslator.class);
        URITranslator combinedTranslator = mock(URITranslator.class);
        // static call: URITranslator.map(uriMap) → mappedTranslator
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mappedTranslator);
        // instance call: uriTranslators.with(mappedTranslator) → combinedTranslator
        when(uriTranslators.with(mappedTranslator)).thenReturn(combinedTranslator);
        // -------------------------------------------------------------------------
        // Act
        // -------------------------------------------------------------------------
        URITranslator result = factory.getUriTranslator();
        // -------------------------------------------------------------------------
        // Assert
        // -------------------------------------------------------------------------
        assertSame(combinedTranslator, result, "The factory should return the translator produced by uriTranslators.with(mapper)");
        // verify that the interactions happened exactly as expected
        uriTranslatorStaticMock.verify(() -> URITranslator.map(uriMap));
        verify(uriTranslators).with(mappedTranslator);
    }

    @Test
    void getUriTranslator_worksWhenUriMapIsEmpty() {
        // -------------------------------------------------------------------------
        // Arrange – replace the uriMap with an empty map and repeat the stubbing
        // -------------------------------------------------------------------------
        // make it empty
        uriMap.clear();
        URITranslator mappedTranslator = mock(URITranslator.class);
        URITranslator combinedTranslator = mock(URITranslator.class);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mappedTranslator);
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

    /**
     * Default factory – no custom map, default composite (mocked).
     */
    @Test
    void DefaultFactory_identityTranslation() {
        uriMap = Collections.emptyMap();
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        // map() returns an identity translator
        URITranslator identity = mock(URITranslator.class);
        when(identity.translate(any(URI.class))).thenAnswer(i -> i.getArgument(0));
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(identity);
        // composite.with(identity) returns the same identity instance
        when(composite.with(identity)).thenReturn(identity);
        factory = buildFactory(uriMap, composite);
        URITranslator result = factory.getUriTranslator();
        assertNotNull(result, "Translator must never be null");
        URI sample = URI.create("http://any.org/resource");
        assertEquals(sample, result.translate(sample), "Default translator should perform identity translation");
        // verify interactions
        uriTranslatorStaticMock.verify(() -> URITranslator.map(uriMap));
        verify(composite).with(identity);
    }

    /**
     * Single entry map – verify mapping works.
     */
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
        assertEquals(expected, translator.translate(source), "The source URI should be mapped to the target URI");
    }

    /**
     * Multiple entries – ensure correct entry is selected.
     */
    @Test
    void CustomUriMap_WithMultipleEntries_correctEntrySelected() {
        uriMap = Map.of("http://a.com", "classpath:/a.json", "http://b.com", "classpath:/b.json");
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

    /**
     * Empty map – identity translation.
     */
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

    /**
     * Null uriMap – expect NPE from static map call.
     */
    @Test
    void NullUriMap_throwsNullPointerException() {
        uriMap = null;
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        // static map(null) should throw NPE – we let the real static method be called
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
        // deliberately null
        CompositeURITranslator composite = null;
        factory = buildFactory(uriMap, composite);
        // static map returns a dummy translator (identity)
        URITranslator identity = mock(URITranslator.class);
        when(identity.translate(any(URI.class))).thenAnswer(i -> i.getArgument(0));
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(identity);
        assertThrows(NullPointerException.class, factory::getUriTranslator);
    }

    /**
     * Composite translator precedence – existing composite mapping wins over map.
     */
    @Test
    void CompositeTranslatorPrecedence_originalCompositeWins() {
        uriMap = Map.of("http://example.com/override", "classpath:/override-from-map.json");
        // a composite translator that already maps the source URI
        CompositeURITranslator originalComposite = mock(CompositeURITranslator.class);
        URITranslator preExisting = mock(URITranslator.class);
        when(preExisting.translate(URI.create("http://example.com/override"))).thenReturn(URI.create("classpath:/override-from-composite.json"));
        // when with() is called we return a translator that delegates first to preExisting,
        // then to the map‑based translator (which would return the map value)
        when(originalComposite.with(any())).thenAnswer(inv -> {
            URITranslator mapBased = inv.getArgument(0);
            return (URI uri) -> {
                URI fromPre = preExisting.translate(uri);
                return fromPre.equals(uri) ? mapBased.translate(uri) : fromPre;
            };
        });
        // static map returns a translator that would return the map value
        URITranslator mapBased = createMapBasedTranslator(uriMap);
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        factory = buildFactory(uriMap, originalComposite);
        URI result = factory.getUriTranslator().translate(URI.create("http://example.com/override"));
        assertEquals(URI.create("classpath:/override-from-composite.json"), result, "Composite translator mapping should take precedence over the map entry");
    }

    /**
     * Null key in map – expect NPE when map() is invoked.
     */
    @Test
    void NullKeyOrValueInMap_nullKey_throwsException() {
        uriMap = new HashMap<>();
        uriMap.put(null, "classpath:/null-key.json");
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        // Simulate the static method throwing NPE for a null key
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
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        // map() returns a translator that will NPE on translate because value is null
        URITranslator mapBased = mock(URITranslator.class);
        when(mapBased.translate(any(URI.class))).thenAnswer(inv -> {
            URI in = inv.getArgument(0);
            String target = uriMap.get(in.toString());
            // target is null → NPE
            return URI.create(target);
        });
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        when(composite.with(mapBased)).thenReturn(mapBased);
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

    /**
     * Invalid URI in map – static map should throw IllegalArgumentException.
     */
    @Test
    void InvalidUriInMap_badKey_throwsException() {
        uriMap = Map.of("ht!tp://bad uri", "classpath:/valid-schema.json");
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
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
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        // map() returns a translator that will attempt to create a URI from the bad value
        URITranslator mapBased = mock(URITranslator.class);
        when(mapBased.translate(any(URI.class))).thenAnswer(inv -> {
            URI in = inv.getArgument(0);
            String target = uriMap.get(in.toString());
            // This line will throw IllegalArgumentException due to malformed URI
            return URI.create(target);
        });
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        when(composite.with(mapBased)).thenReturn(mapBased);
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
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
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
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
        URITranslator mapBased = mock(URITranslator.class);
        when(mapBased.translate(any(URI.class))).thenAnswer(inv -> {
            URI in = inv.getArgument(0);
            String target = uriMap.get(in.toString());
            // Empty string leads to IllegalArgumentException when creating URI
            return URI.create(target);
        });
        uriTranslatorStaticMock.when(() -> URITranslator.map(uriMap)).thenReturn(mapBased);
        when(composite.with(mapBased)).thenReturn(mapBased);
        factory = buildFactory(uriMap, composite);
        URITranslator translator = factory.getUriTranslator();
        assertThrows(IllegalArgumentException.class, () -> translator.translate(URI.create("http://example.com/schema")));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:14: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:15: error: package org.mockito does not exist
import org.mockito.MockedStatic;
                  ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:16: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:18: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:25: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:67: error: cannot find symbol
    private MockedStatic<URITranslator> uriTranslatorStaticMock;
            ^
  symbol:   class MockedStatic
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:26: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:28: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:34: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:37: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:40: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:43: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:46: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:50: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:69: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:72: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:90: error: cannot find symbol
            composite = mock(CompositeURITranslator.class);
                        ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test.TrailingSlashMapping
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:93: error: cannot find symbol
            when(composite.with(mapBased)).thenReturn(mapBased);
            ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test.TrailingSlashMapping
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:127: error: cannot find symbol
            composite = mock(CompositeURITranslator.class);
                        ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test.CaseSensitivityInMapping
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:130: error: cannot find symbol
            when(composite.with(mapBased)).thenReturn(mapBased);
            ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test.CaseSensitivityInMapping
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:164: error: cannot find symbol
            composite = mock(CompositeURITranslator.class);
                        ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test.FragmentAndQueryMapping
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:167: error: cannot find symbol
            when(composite.with(mapBased)).thenReturn(mapBased);
            ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test.FragmentAndQueryMapping
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:196: error: cannot find symbol
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
                                         ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:212: error: cannot find symbol
        uriTranslatorStaticMock = mockStatic(URITranslator.class);
                                  ^
  symbol:   method mockStatic(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:214: error: cannot find symbol
        uriTranslatorStaticMock = mockStatic(URITranslator.class);
                                  ^
  symbol:   method mockStatic(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:237: error: cannot find symbol
            dummyMeta.put(JsonSchemaFactory.normalizeMetaSchemaUri("http://example.com/meta"), mock(JsonMetaSchema.class));
                                                                                               ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:248: error: cannot find symbol
        URITranslator translator = mock(URITranslator.class);
                                   ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:249: error: cannot find symbol
        when(translator.translate(any(URI.class))).thenAnswer(invocation -> {
                                  ^
  symbol:   method any(Class<URI>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:262: error: cannot find symbol
        URITranslator mappedTranslator = mock(URITranslator.class);
                                         ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:263: error: cannot find symbol
        URITranslator combinedTranslator = mock(URITranslator.class);
                                           ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:267: error: cannot find symbol
        when(uriTranslators.with(mappedTranslator)).thenReturn(combinedTranslator);
        ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:278: error: cannot find symbol
        verify(uriTranslators).with(mappedTranslator);
        ^
  symbol:   method verify(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:288: error: cannot find symbol
        URITranslator mappedTranslator = mock(URITranslator.class);
                                         ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:289: error: cannot find symbol
        URITranslator combinedTranslator = mock(URITranslator.class);
                                           ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:291: error: cannot find symbol
        when(uriTranslators.with(mappedTranslator)).thenReturn(combinedTranslator);
        ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:301: error: cannot find symbol
        verify(uriTranslators).with(mappedTranslator);
        ^
  symbol:   method verify(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:310: error: cannot find symbol
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
                                           ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:312: error: cannot find symbol
        URITranslator identity = mock(URITranslator.class);
                                 ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:313: error: cannot find symbol
        when(identity.translate(any(URI.class))).thenAnswer(i -> i.getArgument(0));
                                ^
  symbol:   method any(Class<URI>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:316: error: cannot find symbol
        when(composite.with(identity)).thenReturn(identity);
        ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:324: error: cannot find symbol
        verify(composite).with(identity);
        ^
  symbol:   method verify(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:334: error: cannot find symbol
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
                                           ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:337: error: cannot find symbol
        when(composite.with(mapBased)).thenReturn(mapBased);
        ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:351: error: cannot find symbol
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
                                           ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:354: error: cannot find symbol
        when(composite.with(mapBased)).thenReturn(mapBased);
        ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:368: error: cannot find symbol
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
                                           ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:369: error: cannot find symbol
        URITranslator identity = mock(URITranslator.class);
                                 ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:370: error: cannot find symbol
        when(identity.translate(any(URI.class))).thenAnswer(i -> i.getArgument(0));
                                ^
  symbol:   method any(Class<URI>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:372: error: cannot find symbol
        when(composite.with(identity)).thenReturn(identity);
        ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:385: error: cannot find symbol
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
                                           ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:402: error: cannot find symbol
        URITranslator identity = mock(URITranslator.class);
                                 ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:403: error: cannot find symbol
        when(identity.translate(any(URI.class))).thenAnswer(i -> i.getArgument(0));
                                ^
  symbol:   method any(Class<URI>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:415: error: cannot find symbol
        CompositeURITranslator originalComposite = mock(CompositeURITranslator.class);
                                                   ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:416: error: cannot find symbol
        URITranslator preExisting = mock(URITranslator.class);
                                    ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:417: error: cannot find symbol
        when(preExisting.translate(URI.create("http://example.com/override"))).thenReturn(URI.create("classpath:/override-from-composite.json"));
        ^
  symbol:   method when(URI)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:420: error: cannot find symbol
        when(originalComposite.with(any())).thenAnswer(inv -> {
                                    ^
  symbol:   method any()
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:442: error: cannot find symbol
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
                                           ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:456: error: cannot find symbol
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
                                           ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:458: error: cannot find symbol
        URITranslator mapBased = mock(URITranslator.class);
                                 ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:459: error: cannot find symbol
        when(mapBased.translate(any(URI.class))).thenAnswer(inv -> {
                                ^
  symbol:   method any(Class<URI>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:466: error: cannot find symbol
        when(composite.with(mapBased)).thenReturn(mapBased);
        ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:478: error: cannot find symbol
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
                                           ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:479: error: cannot find symbol
        URITranslator identity = mock(URITranslator.class);
                                 ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:480: error: cannot find symbol
        when(identity.translate(any(URI.class))).thenAnswer(i -> i.getArgument(0));
                                ^
  symbol:   method any(Class<URI>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:482: error: cannot find symbol
        when(composite.with(identity)).thenReturn(identity);
        ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:495: error: cannot find symbol
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
                                           ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:507: error: cannot find symbol
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
                                           ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:509: error: cannot find symbol
        URITranslator mapBased = mock(URITranslator.class);
                                 ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:510: error: cannot find symbol
        when(mapBased.translate(any(URI.class))).thenAnswer(inv -> {
                                ^
  symbol:   method any(Class<URI>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:517: error: cannot find symbol
        when(composite.with(mapBased)).thenReturn(mapBased);
        ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:529: error: cannot find symbol
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
                                           ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:541: error: cannot find symbol
        CompositeURITranslator composite = mock(CompositeURITranslator.class);
                                           ^
  symbol:   method mock(Class<CompositeURITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:542: error: cannot find symbol
        URITranslator mapBased = mock(URITranslator.class);
                                 ^
  symbol:   method mock(Class<URITranslator>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:543: error: cannot find symbol
        when(mapBased.translate(any(URI.class))).thenAnswer(inv -> {
                                ^
  symbol:   method any(Class<URI>)
  location: class JsonSchemaFactory_getUriTranslator_Test
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriTranslator_Test.java:550: error: cannot find symbol
        when(composite.with(mapBased)).thenReturn(mapBased);
        ^
  symbol:   method when(CompositeURITranslator)
  location: class JsonSchemaFactory_getUriTranslator_Test
75 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'return type': 'com.networknt.schema.uri.URITranslator', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\tdependent fields:\n\t\tcom.networknt.schema.uri.URITranslator.CompositeURITranslator com.networknt.schema.JsonSchemaFactory.uriTranslators;\n\t\tjava.util.Map<java.lang.String, java.lang.String> com.networknt.schema.JsonSchemaFactory.uriMap;\n\trelated functions:\n\t\tmethod `public JsonSchema getSchema(URI, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getUriTranslator()`;\n\t\tmethod `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getUriTranslator()`;\nclass `com.networknt.schema.SchemaValidatorsConfig`:\n\trelated functions:\n\t\tmethod `public CompositeURITranslator getUriTranslator()` returns `com.networknt.schema.uri.URITranslator.CompositeURITranslator`, related with `com.networknt.schema.JsonSchemaFactory.getUriTranslator()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getUriTranslator()`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


