```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getMetaSchema(String, SchemaValidatorsConfig)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Well‑known dialect handling (true branch of the Optional chain)</li>
 *   <li>Custom / unknown meta‑schema handling (fallback branch)</li>
 *   <li>Variations of the {@code config} argument (null / non‑null)</li>
 *   <li>String edge cases (null, empty, whitespace, case‑sensitivity)</li>
 *   <li>Caching behaviour for unknown ids</li>
 *   <li>Exception propagation from {@code SpecVersionDetector}</li>
 *   <li>Exception propagation from {@code loadMetaSchema}</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMetaSchema_Test {

    private static AutoCloseable mocks;

    private JsonSchemaFactory factory;               // the instance under test (a spy)
    private Map<String, JsonMetaSchema> metaSchemas; // internal map used by the factory

    @BeforeAll
    static void initAll() {
        // Initialise Mockito annotations (needed for @Mock if we add any later)
        mocks = MockitoAnnotations.openMocks(JsonSchemaFactory_getMetaSchema_Test.class);
    }

    @AfterAll
    static void closeAll() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @BeforeEach
    void setUp() {
        // --------------------------------------------------------------------
        // Minimal constructor arguments – the real factory has a protected
        // constructor that receives many collaborators.  We use the public
        // constructor that the library provides (the one used in the original
        // test framework) and then spy it so that we can verify calls to the
        // protected loadMetaSchema method.
        // --------------------------------------------------------------------
        ObjectMapper jsonMapper = mock(ObjectMapper.class);
        ObjectMapper yamlMapper = mock(ObjectMapper.class);
        SchemaLoaders.Builder loadersBuilder = new SchemaLoaders.Builder();
        SchemaMappers.Builder mappersBuilder = new SchemaMappers.Builder();

        // Pre‑populate the map with the well‑known Draft‑07 meta‑schema – this
        // mirrors the behaviour of the real factory.
        String defaultMetaSchemaURI = "http://json-schema.org/draft-07/schema#";
        JsonMetaSchema draft07 = mock(JsonMetaSchema.class);
        metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(defaultMetaSchemaURI), draft07);

        // Create a concrete factory instance.
        JsonSchemaFactory realFactory = new JsonSchemaFactory(
                jsonMapper,
                yamlMapper,
                defaultMetaSchemaURI,
                loadersBuilder,
                mappersBuilder,
                metaSchemas,
                false) {

            /**
             * Overridden only for test purposes – it records the call and returns
             * a distinct mock (or throws, depending on the test scenario).
             */
            @Override
            protected JsonMetaSchema loadMetaSchema(String id, SchemaValidatorsConfig config) {
                // Default behaviour: return a fresh mock and store it for caching.
                JsonMetaSchema custom = mock(JsonMetaSchema.class);
                metaSchemas.put(normalizeMetaSchemaUri(id), custom);
                return custom;
            }
        };

        // Spy the factory so that we can verify interactions with loadMetaSchema.
        factory = spy(realFactory);
    }

    /* --------------------------------------------------------------
     *  1. Well‑known dialect handling (true branch)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] id={0}")
    @CsvSource({
            "'http://json-schema.org/draft-07/schema#', DRAFT_07",
            "'https://json-schema.org/draft/2020-12/schema', DRAFT_2020_12",
            "'http://json-schema.org/draft-04/schema#', DRAFT_04"
    })
    void testGetMetaSchema_WellKnownDialect(String id, String versionEnum) {
        // GIVEN
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        // WHEN
        JsonMetaSchema result = factory.getMetaSchema(id, cfg);

        // THEN
        assertNotNull(result, "Result must not be null for a recognised dialect");

        // Verify that the result is the instance that belongs to the expected
        // JsonSchemaVersion enum.  The library ships with a singleton per version.
        JsonSchemaVersion expectedVersion = JsonSchemaVersion.valueOf(versionEnum);
        assertSame(expectedVersion.getInstance(), result,
                "The meta‑schema should be the singleton provided by the enum");

        // The fallback must never be invoked for a well‑known dialect.
        verify(factory, never()).loadMetaSchema(anyString(), any());
    }

    /* --------------------------------------------------------------
     *  2. Custom / unknown meta‑schema handling (fallback branch)
     * -------------------------------------------------------------- */
    @Test
    void testGetMetaSchema_CustomId_DelegatesToLoadMetaSchema() {
        // GIVEN
        String customId = "urn:custom:my-meta-schema";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        // WHEN
        JsonMetaSchema result = factory.getMetaSchema(customId, cfg);

        // THEN
        assertNotNull(result, "Custom meta‑schema must be returned");
        verify(factory, times(1)).loadMetaSchema(eq(customId), eq(cfg));
        // The factory caches the result – the map should contain the same instance.
        String normalized = JsonSchemaFactory.normalizeMetaSchemaUri(customId);
        assertSame(result, metaSchemas.get(normalized),
                "Returned instance must be stored in the internal cache");
    }

    @Test
    void testGetMetaSchema_NullId_UsesLoadMetaSchema() {
        // GIVEN
        String nullId = null;
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        // WHEN
        JsonMetaSchema result = factory.getMetaSchema(nullId, cfg);

        // THEN
        assertNotNull(result, "Result must not be null even when id is null");
        verify(factory, times(1)).loadMetaSchema(isNull(), eq(cfg));
    }

    @Test
    void testGetMetaSchema_EmptyStringId_UsesLoadMetaSchema() {
        // GIVEN
        String emptyId = "";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        // WHEN
        JsonMetaSchema result = factory.getMetaSchema(emptyId, cfg);

        // THEN
        assertNotNull(result, "Result must not be null for empty id");
        verify(factory, times(1)).loadMetaSchema(eq(emptyId), eq(cfg));
    }

    /* --------------------------------------------------------------
     *  3. Config parameter variations (null config)
     * -------------------------------------------------------------- */
    @Test
    void testGetMetaSchema_WellKnownDialect_NullConfig() {
        // GIVEN
        String id = "http://json-schema.org/draft-07/schema#";

        // WHEN
        JsonMetaSchema result = factory.getMetaSchema(id, null);

        // THEN
        assertNotNull(result);
        assertSame(JsonSchemaVersion.DRAFT_07.getInstance(), result);
        verify(factory, never()).loadMetaSchema(anyString(), any());
    }

    @Test
    void testGetMetaSchema_CustomId_NullConfig() {
        // GIVEN
        String customId = "urn:custom:my-meta-schema";

        // WHEN
        JsonMetaSchema result = factory.getMetaSchema(customId, null);

        // THEN
        assertNotNull(result);
        verify(factory, times(1)).loadMetaSchema(eq(customId), isNull());
    }

    /* --------------------------------------------------------------
     *  4. Id string variations (whitespace & case)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] id=''{0}''")
    @CsvSource({
            "'  http://json-schema.org/draft-07/schema#  '",
            "'HTTP://JSON-SCHEMA.ORG/DRAFT-07/SCHEMA#'"
    })
    void testGetMetaSchema_IdWithWhitespaceOrUpperCase_FallsBack(String id) {
        // GIVEN
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        // WHEN
        JsonMetaSchema result = factory.getMetaSchema(id, cfg);

        // THEN
        assertNotNull(result);
        verify(factory, times(1)).loadMetaSchema(eq(id), eq(cfg));
    }

    /* --------------------------------------------------------------
     *  5. Malformed or non‑URI ids (fallback)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] id=''{0}''")
    @CsvSource({
            "'not a uri'",
            "'http://[invalid-ip]/schema#'"
    })
    void testGetMetaSchema_MalformedId_FallsBack(String id) {
        // GIVEN
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        // WHEN
        JsonMetaSchema result = factory.getMetaSchema(id, cfg);

        // THEN
        assertNotNull(result);
        verify(factory, times(1)).loadMetaSchema(eq(id), eq(cfg));
    }

    /* --------------------------------------------------------------
     *  6. Repeated calls with same unknown id (caching)
     * -------------------------------------------------------------- */
    @Test
    void testGetMetaSchema_CachingBehaviourForUnknownId() {
        // GIVEN
        String unknownId = "urn:custom:cached-schema";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        // First call – should trigger loadMetaSchema
        JsonMetaSchema first = factory.getMetaSchema(unknownId, cfg);
        // Second call – should retrieve the cached instance
        JsonMetaSchema second = factory.getMetaSchema(unknownId, cfg);

        // THEN
        assertNotNull(first);
        assertSame(first, second, "Subsequent calls must return the cached instance");
        // loadMetaSchema must have been invoked exactly once
        verify(factory, times(1)).loadMetaSchema(eq(unknownId), eq(cfg));
    }

    /* --------------------------------------------------------------
     *  7. SpecVersionDetector parsing errors – exception propagation
     * -------------------------------------------------------------- */
    @Test
    void testGetMetaSchema_SpecVersionDetectorThrowsException() {
        // GIVEN
        String badId = "http://[invalid-host]/schema#";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        try (MockedStatic<SpecVersionDetector> mocked = mockStatic(SpecVersionDetector.class)) {
            // Simulate the detector throwing an IllegalArgumentException for this id
            mocked.when(() -> SpecVersionDetector.detectOptionalVersion(badId))
                    .thenThrow(new IllegalArgumentException("Invalid URI"));

            // WHEN / THEN
            assertThrows(IllegalArgumentException.class,
                    () -> factory.getMetaSchema(badId, cfg),
                    "The IllegalArgumentException from the detector must propagate");
        }

        // Verify that loadMetaSchema was never reached
        verify(factory, never()).loadMetaSchema(anyString(), any());
    }

    @Test
    void testGetMetaSchema_SpecVersionDetectorControlCharThrows() {
        // GIVEN
        String controlCharId = "\u0000";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        try (MockedStatic<SpecVersionDetector> mocked = mockStatic(SpecVersionDetector.class)) {
            mocked.when(() -> SpecVersionDetector.detectOptionalVersion(controlCharId))
                    .thenThrow(new IllegalArgumentException("Control character not allowed"));

            assertThrows(IllegalArgumentException.class,
                    () -> factory.getMetaSchema(controlCharId, cfg));
        }

        verify(factory, never()).loadMetaSchema(anyString(), any());
    }

    /* --------------------------------------------------------------
     *  8. loadMetaSchema throws – exception propagation
     * -------------------------------------------------------------- */
    @Test
    void testGetMetaSchema_LoadMetaSchemaResourceNotFound() {
        // GIVEN
        String missingResource = "classpath:/nonexistent-meta-schema.json";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        // Override loadMetaSchema for this specific id to throw
        doThrow(new IllegalArgumentException("Resource not found"))
                .when(factory).loadMetaSchema(eq(missingResource), any());

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class,
                () -> factory.getMetaSchema(missingResource, cfg));
    }

    @Test
    void testGetMetaSchema_LoadMetaSchemaIoError() {
        // GIVEN
        String ioErrorId = "http://example.com/unstable-schema.json";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        doThrow(new UncheckedIOException(new java.io.IOException("Network failure")))
                .when(factory).loadMetaSchema(eq(ioErrorId), any());

        // WHEN / THEN
        assertThrows(UncheckedIOException.class,
                () -> factory.getMetaSchema(ioErrorId, cfg));
    }
}
```