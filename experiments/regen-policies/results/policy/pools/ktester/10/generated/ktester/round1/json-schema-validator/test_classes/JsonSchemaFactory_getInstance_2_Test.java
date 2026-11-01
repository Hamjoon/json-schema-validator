package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getInstance_2_Test {

    /**
     * Common test data
     */
    private static final String DEFAULT_META_URI = "http://example.com/default-meta";

    private static final String EXTRA_META_URI = "http://example.com/extra-meta";

    /**
     * Mocks that are reused across test methods
     */
    private JsonSchemaVersion mockVersion;

    private JsonMetaSchema mockDefaultMeta;

    private JsonMetaSchema mockExtraMeta;

    /**
     * A dummy {@code VersionFlag} – the concrete value is irrelevant because we mock the static lookup.
     */
    private static final SpecVersion.VersionFlag DUMMY_FLAG = new SpecVersion.VersionFlag(1L, "DUMMY");

    private static final String CUSTOM_META_URI = "http://example.com/custom-meta";

    private JsonMetaSchema mockCustomMeta1;

    private JsonMetaSchema mockCustomMeta2;

    @BeforeEach
    void setupBeforeEach() {
        // Create fresh mocks for each test to avoid cross‑test interference.
        mockVersion = mock(JsonSchemaVersion.class);
        mockDefaultMeta = mock(JsonMetaSchema.class);
        mockExtraMeta = mock(JsonMetaSchema.class);
        // Default meta‑schema behaviour
        when(mockDefaultMeta.getUri()).thenReturn(DEFAULT_META_URI);
        when(mockVersion.getInstance()).thenReturn(mockDefaultMeta);
        mockVersion = mock(JsonSchemaVersion.class);
        mockDefaultMeta = mock(JsonMetaSchema.class);
        mockExtraMeta = mock(JsonMetaSchema.class);
        mockCustomMeta1 = mock(JsonMetaSchema.class);
        mockCustomMeta2 = mock(JsonMetaSchema.class);
        when(mockDefaultMeta.getUri()).thenReturn(DEFAULT_META_URI);
        when(mockVersion.getInstance()).thenReturn(mockDefaultMeta);
    }

    /**
     * Shortcut to create a fresh {@link SchemaValidatorsConfig}.
     */
    private static SchemaValidatorsConfig newConfig() {
        return new SchemaValidatorsConfig();
    }

    /**
     * Verify that {@code getInstance(flag, null)} registers the default meta‑schema
     * and that the resulting factory can retrieve it via {@code getMetaSchema}.
     */
    @Test
    void testGetInstanceWithoutCustomizer() {
        // Mock the static method that resolves a version flag to a JsonSchemaVersion.
        try (MockedStatic<JsonSchemaFactory> mockedFactory = Mockito.mockStatic(JsonSchemaFactory.class)) {
            mockedFactory.when(() -> JsonSchemaFactory.checkVersion(DUMMY_FLAG)).thenReturn(mockVersion);
            // The real builder() method is used – we do not mock it.
            mockedFactory.when(JsonSchemaFactory::builder).thenCallRealMethod();
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(DUMMY_FLAG, null);
            assertNotNull(factory, "Factory must not be null");
            // The factory should contain the default meta‑schema we supplied.
            JsonMetaSchema retrieved = factory.getMetaSchema(DEFAULT_META_URI, new SchemaValidatorsConfig());
            assertSame(mockDefaultMeta, retrieved, "Factory should return the default meta‑schema that was added by the method");
        }
    }

    /**
     * Verify that a non‑null customizer is invoked and can add additional meta‑schemas.
     */
    @Test
    void testGetInstanceWithCustomizer() {
        // Prepare the extra meta‑schema mock.
        when(mockExtraMeta.getUri()).thenReturn(EXTRA_META_URI);
        // Define a customizer that adds the extra meta‑schema to the builder.
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.addMetaSchema(mockExtraMeta);
        try (MockedStatic<JsonSchemaFactory> mockedFactory = Mockito.mockStatic(JsonSchemaFactory.class)) {
            mockedFactory.when(() -> JsonSchemaFactory.checkVersion(DUMMY_FLAG)).thenReturn(mockVersion);
            mockedFactory.when(JsonSchemaFactory::builder).thenCallRealMethod();
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(DUMMY_FLAG, customizer);
            assertNotNull(factory, "Factory must not be null");
            // Both the default and the extra meta‑schema must be present.
            JsonMetaSchema defaultRetrieved = factory.getMetaSchema(DEFAULT_META_URI, new SchemaValidatorsConfig());
            JsonMetaSchema extraRetrieved = factory.getMetaSchema(EXTRA_META_URI, new SchemaValidatorsConfig());
            assertSame(mockDefaultMeta, defaultRetrieved, "Default meta‑schema should be registered");
            assertSame(mockExtraMeta, extraRetrieved, "Extra meta‑schema added by the customizer should be registered");
        }
        // Verify that the customizer was actually executed.
        // (If it had not been called, the extra meta‑schema would be missing.)
        // interaction caused by the later lookup
        verify(mockExtraMeta).getUri();
    }

    /**
     * Retrieves a private field value via reflection.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new AssertionError("Unable to access field '" + fieldName + "'", e);
        }
    }

    @Test
    void customizerIsNull_registersDefaultMetaSchema() {
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class)) {
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4)).thenReturn(mockVersion);
            // Use the real builder() implementation.
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, null);
            assertNotNull(factory, "Factory must not be null");
            // The default meta‑schema must be retrievable via its URI.
            JsonMetaSchema retrieved = factory.getMetaSchema(DEFAULT_META_URI, newConfig());
            assertSame(mockDefaultMeta, retrieved, "Factory should return the default meta‑schema that was added internally");
        }
    }

    @Test
    void customizerModifiesBuilder_enableUriSchemaCache() {
        Consumer<JsonSchemaFactory.Builder> customizer = b -> b.enableUriSchemaCache(false);
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class)) {
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7)).thenReturn(mockVersion);
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7, customizer);
            assertNotNull(factory, "Factory must not be null");
            // Verify the flag via reflection.
            Boolean flag = getPrivateField(factory, "enableUriSchemaCache");
            assertFalse(flag, "enableUriSchemaCache should be false after customiser execution");
            // Default meta‑schema URI must still match the version’s meta‑schema.
            String defaultUri = getPrivateField(factory, "defaultMetaSchemaURI");
            assertEquals(JsonSchemaVersion.V7.getInstance().getUri(), defaultUri, "defaultMetaSchemaURI must match the built‑in V7 meta‑schema URI");
        }
    }

    @Test
    void nullVersionFlag_throwsNullPointerException() {
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class)) {
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(null)).thenThrow(new NullPointerException("versionFlag is null"));
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            assertThrows(NullPointerException.class, () -> JsonSchemaFactory.getInstance(null, null), "Calling getInstance with a null versionFlag should throw NPE");
        }
    }

    @Test
    void unsupportedVersionFlag_throwsIllegalArgumentException() {
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class)) {
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.UNKNOWN)).thenThrow(new IllegalArgumentException("Unsupported version"));
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.UNKNOWN, null), "Unsupported versionFlag should result in IllegalArgumentException");
        }
    }

    @Test
    void versionV6_withoutCustomizer_registersDefaultMetaSchema() {
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class)) {
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6)).thenReturn(mockVersion);
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V6, null);
            assertNotNull(factory, "Factory must not be null");
            String defaultUri = getPrivateField(factory, "defaultMetaSchemaURI");
            assertEquals(JsonSchemaVersion.V6.getInstance().getUri(), defaultUri, "defaultMetaSchemaURI must match V6 built‑in meta‑schema URI");
            JsonMetaSchema retrieved = factory.getMetaSchema(defaultUri, newConfig());
            assertSame(mockDefaultMeta, retrieved, "Built‑in V6 meta‑schema must be registered");
        }
    }

    @Test
    void customizerAddsAdditionalMetaSchema() {
        when(mockExtraMeta.getUri()).thenReturn(EXTRA_META_URI);
        Consumer<JsonSchemaFactory.Builder> customizer = b -> b.addMetaSchema(mockExtraMeta);
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class)) {
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6)).thenReturn(mockVersion);
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V6, customizer);
            assertNotNull(factory, "Factory must not be null");
            // default meta‑schema
            JsonMetaSchema defaultRetrieved = factory.getMetaSchema(DEFAULT_META_URI, newConfig());
            assertSame(mockDefaultMeta, defaultRetrieved, "Default meta‑schema should be present");
            // extra meta‑schema
            JsonMetaSchema extraRetrieved = factory.getMetaSchema(EXTRA_META_URI, newConfig());
            assertSame(mockExtraMeta, extraRetrieved, "Extra meta‑schema added by customiser must be present");
            // internal map size >= 2
            @SuppressWarnings("unchecked")
            ConcurrentMap<String, JsonMetaSchema> internalMap = getPrivateField(factory, "jsonMetaSchemas");
            assertTrue(internalMap.size() >= 2, "Factory's internal meta‑schema map should contain at least two entries");
        }
    }

    @Test
    void customizerOverridesDefaultMetaSchemaURI() {
        String customUri = "http://example.com/custom-meta";
        Consumer<JsonSchemaFactory.Builder> customizer = b -> b.defaultMetaSchemaURI(customUri);
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class)) {
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7)).thenReturn(mockVersion);
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7, customizer);
            assertNotNull(factory, "Factory must not be null");
            String defaultUri = getPrivateField(factory, "defaultMetaSchemaURI");
            assertEquals(customUri, defaultUri, "defaultMetaSchemaURI should be replaced by the custom value");
            // built‑in V7 meta‑schema must still be registered (added before customiser runs)
            JsonMetaSchema builtIn = factory.getMetaSchema(JsonSchemaVersion.V7.getInstance().getUri(), newConfig());
            assertNotNull(builtIn, "Built‑in V7 meta‑schema must still be registered");
        }
    }

    @Test
    void customizerTogglesEnableUriSchemaCacheTwice() {
        Consumer<JsonSchemaFactory.Builder> customizer = b -> b.enableUriSchemaCache(false).enableUriSchemaCache(true);
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class)) {
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7)).thenReturn(mockVersion);
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7, customizer);
            assertNotNull(factory, "Factory must not be null");
            Boolean flag = getPrivateField(factory, "enableUriSchemaCache");
            assertTrue(flag, "Final value of enableUriSchemaCache should be true (last call wins)");
            String defaultUri = getPrivateField(factory, "defaultMetaSchemaURI");
            assertEquals(JsonSchemaVersion.V7.getInstance().getUri(), defaultUri, "defaultMetaSchemaURI must still match V7 meta‑schema URI");
        }
    }

    @Test
    void customizerAddsCollectionOfMetaSchemas() {
        when(mockCustomMeta1.getUri()).thenReturn("http://example.com/custom1");
        when(mockCustomMeta2.getUri()).thenReturn("http://example.com/custom2");
        Collection<JsonMetaSchema> collection = Arrays.asList(mockCustomMeta1, mockCustomMeta2);
        Consumer<JsonSchemaFactory.Builder> customizer = b -> b.addMetaSchemas(collection);
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class)) {
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6)).thenReturn(mockVersion);
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V6, customizer);
            assertNotNull(factory, "Factory must not be null");
            // both custom meta‑schemas must be registered
            assertNotNull(factory.getMetaSchema(mockCustomMeta1.getUri(), newConfig()), "First custom meta‑schema should be registered");
            assertNotNull(factory.getMetaSchema(mockCustomMeta2.getUri(), newConfig()), "Second custom meta‑schema should be registered");
            // built‑in V6 meta‑schema must also be present
            assertNotNull(factory.getMetaSchema(JsonSchemaVersion.V6.getInstance().getUri(), newConfig()), "Built‑in V6 meta‑schema must be present");
            // internal map size >= 3
            @SuppressWarnings("unchecked")
            ConcurrentMap<String, JsonMetaSchema> internalMap = getPrivateField(factory, "jsonMetaSchemas");
            assertTrue(internalMap.size() >= 3, "Factory should contain at least three meta‑schemas (built‑in + two custom)");
        }
    }

    @Test
    void customiserConfiguresSchemaLoadersAndMappers() {
        // Mocks for custom components
        SchemaLoader customLoader = mock(SchemaLoader.class);
        SchemaMapper customMapper = mock(SchemaMapper.class);
        // Customiser that adds the components
        Consumer<JsonSchemaFactory.Builder> customizer = b -> b.schemaLoaders(slBuilder -> slBuilder.addSchemaLoader(customLoader)).schemaMappers(smBuilder -> smBuilder.addSchemaMapper(customMapper));
        // Mock static creation of SchemaLoaders.Builder and SchemaMappers.Builder
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class);
            MockedStatic<SchemaLoaders> slMock = Mockito.mockStatic(SchemaLoaders.class);
            MockedStatic<SchemaMappers> smMock = Mockito.mockStatic(SchemaMappers.class)) {
            // static checkVersion
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4)).thenReturn(mockVersion);
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            // Provide real builder instances for the nested builders
            SchemaLoaders.Builder slBuilder = SchemaLoaders.builder();
            SchemaMappers.Builder smBuilder = SchemaMappers.builder();
            slMock.when(SchemaLoaders::builder).thenReturn(slBuilder);
            smMock.when(SchemaMappers::builder).thenReturn(smBuilder);
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, customizer);
            assertNotNull(factory, "Factory must not be null");
            // Verify custom loader is present
            SchemaLoader loader = factory.getSchemaLoader();
            assertTrue(loader.getLoaders().contains(customLoader), "Custom SchemaLoader should be present in the factory's loader chain");
            // Verify custom mapper is present – the factory does not expose a getter,
            // so we use reflection to obtain the private field.
            Object schemaMappers = getPrivateField(factory, "schemaMappers");
            // schemaMappers is expected to have a method getMappers()
            try {
                @SuppressWarnings("unchecked")
                Collection<SchemaMapper> mappers = (Collection<SchemaMapper>) schemaMappers.getClass().getMethod("getMappers").invoke(schemaMappers);
                assertTrue(mappers.contains(customMapper), "Custom SchemaMapper should be present in the factory's mapper chain");
            } catch (Exception e) {
                fail("Unable to inspect schemaMappers via reflection", e);
            }
        }
    }

    @Test
    void customiserThrowsNullPointerException_propagates() {
        Consumer<JsonSchemaFactory.Builder> throwingCustomizer = b -> {
            throw new NullPointerException("customizer NPE");
        };
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class)) {
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4)).thenReturn(mockVersion);
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            assertThrows(NullPointerException.class, () -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4, throwingCustomizer), "Customiser NPE must propagate");
        }
    }

    @Test
    void customiserThrowsIllegalArgumentException_propagates() {
        Consumer<JsonSchemaFactory.Builder> throwingCustomizer = b -> {
            throw new IllegalArgumentException("customizer illegal arg");
        };
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class)) {
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6)).thenReturn(mockVersion);
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V6, throwingCustomizer), "Customiser IllegalArgumentException must propagate");
        }
    }

    @Test
    void customiserThrowsRuntimeException_propagates() {
        Consumer<JsonSchemaFactory.Builder> throwingCustomizer = b -> {
            throw new RuntimeException("customizer failure");
        };
        try (MockedStatic<JsonSchemaFactory> factoryMock = Mockito.mockStatic(JsonSchemaFactory.class)) {
            factoryMock.when(() -> JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7)).thenReturn(mockVersion);
            factoryMock.when(JsonSchemaFactory::builder).thenCallRealMethod();
            assertThrows(RuntimeException.class, () -> JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7, throwingCustomizer), "Customiser RuntimeException must propagate");
        }
    }
}
