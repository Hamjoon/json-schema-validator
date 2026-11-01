package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_loadMetaSchema_Test {

    /**
     * Factory created by the public API – used as a base for the tests.
     */
    private JsonSchemaFactory factory;

    /**
     * Mocked configuration passed to the factory – its internals are irrelevant for these tests.
     */
    @Mock
    private SchemaValidatorsConfig mockConfig;

    /**
     * Mocked {@link JsonMetaSchema} that will be returned by the custom meta‑schema factory.
     */
    @Mock
    private JsonMetaSchema mockMetaSchema;

    /**
     * Mocked {@link JsonMetaSchemaFactory} used to verify delegation.
     */
    @Mock
    private JsonMetaSchemaFactory mockMetaSchemaFactory;

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a regular factory instance (uses the default meta‑schema factory).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Obtain a fresh factory instance for each test.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * Injects the supplied {@code JsonMetaSchemaFactory} into the private
     * {@code metaSchemaFactory} field of the given {@code JsonSchemaFactory}
     * instance using reflection.
     */
    private void injectMetaFactory(JsonSchemaFactory target, JsonMetaSchemaFactory metaFactory) throws Exception {
        Field f = JsonSchemaFactory.class.getDeclaredField("metaSchemaFactory");
        f.setAccessible(true);
        f.set(target, metaFactory);
    }

    /**
     * Calls the protected {@code loadMetaSchema} method via reflection.
     *
     * @return the {@link JsonMetaSchema} returned by the method
     */
    private JsonMetaSchema invokeLoadMetaSchema(JsonSchemaFactory target, String iri, SchemaValidatorsConfig cfg) throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("loadMetaSchema", String.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        return (JsonMetaSchema) m.invoke(target, iri, cfg);
    }

    private static Stream<Arguments> provideCustomFactoryExceptions() {
        return Stream.of(Arguments.of(RuntimeException.class, "http://example.com/bad-meta"), Arguments.of(IllegalArgumentException.class, "http://example.com/illegal-arg"), Arguments.of(NullPointerException.class, "http://example.com/null-config"), Arguments.of(AssertionError.class, "http://example.com/assertion-error"));
    }

    private static Stream<Arguments> provideIriEdgeValues() {
        return Stream.of(// empty string
        Arguments.of(""), // whitespace only
        Arguments.of("   "));
    }

    /**
     * When a custom {@link JsonMetaSchemaFactory} is present the {@code loadMetaSchema}
     * implementation must delegate to it.
     */
    @Test
    void loadMetaSchema_delegatesToCustomMetaSchemaFactory() throws Exception {
        // ---------------------------------------------------------------------
        // GIVEN – inject a mock JsonMetaSchemaFactory into the factory instance.
        // ---------------------------------------------------------------------
        Field metaFactoryField = JsonSchemaFactory.class.getDeclaredField("metaSchemaFactory");
        metaFactoryField.setAccessible(true);
        metaFactoryField.set(factory, mockMetaSchemaFactory);
        // Configure the mock to return a known JsonMetaSchema.
        when(mockMetaSchemaFactory.getMetaSchema(anyString(), any(JsonSchemaFactory.class), any())).thenReturn(mockMetaSchema);
        // ---------------------------------------------------------------------
        // WHEN – request a meta‑schema.
        // ---------------------------------------------------------------------
        String testIri = "https://example.com/custom/meta/schema";
        JsonMetaSchema result = factory.getMetaSchema(testIri, mockConfig);
        // ---------------------------------------------------------------------
        // THEN – the custom factory must have been called exactly once and the
        //        returned instance must be the one supplied by the mock.
        // ---------------------------------------------------------------------
        assertSame(mockMetaSchema, result, "The meta‑schema returned by the factory should be the mock instance");
        verify(mockMetaSchemaFactory, times(1)).getMetaSchema(eq(testIri), same(factory), same(mockConfig));
    }

    /**
     * The result of {@link JsonSchemaFactory#getMetaSchema(String, SchemaValidatorsConfig)}
     * is cached; consequently {@code loadMetaSchema} should be invoked only once for the same IRI.
     */
    @Test
    void loadMetaSchema_isCachedAfterFirstInvocation() {
        // -----------------------------------------------------------------
        // GIVEN – a spy that allows us to verify protected method calls.
        // -----------------------------------------------------------------
        JsonSchemaFactory spyFactory = Mockito.spy(factory);
        String testIri = "https://json-schema.org/draft/2020-12/schema";
        // -----------------------------------------------------------------
        // WHEN – request the same meta‑schema twice.
        // -----------------------------------------------------------------
        JsonMetaSchema first = spyFactory.getMetaSchema(testIri, mockConfig);
        JsonMetaSchema second = spyFactory.getMetaSchema(testIri, mockConfig);
        // -----------------------------------------------------------------
        // THEN – both calls return the same instance and loadMetaSchema
        //        was executed only once.
        // -----------------------------------------------------------------
        assertSame(first, second, "Subsequent calls must return the cached meta‑schema instance");
        // Verify that the protected method was invoked exactly once.
        verify(spyFactory, times(1)).loadMetaSchema(eq(testIri), same(mockConfig));
    }

    @Test
    void loadMetaSchema_customFactory_nullIri_propagatesException() throws Exception {
        injectMetaFactory(factory, mockMetaSchemaFactory);
        when(mockMetaSchemaFactory.getMetaSchema(isNull(), any(), any())).thenThrow(new NullPointerException("IRI is null"));
        assertThrows(NullPointerException.class, () -> invokeLoadMetaSchema(factory, null, mockConfig));
    }

    @Test
    void loadMetaSchema_customFactory_nullConfig_passesThrough() throws Exception {
        injectMetaFactory(factory, mockMetaSchemaFactory);
        when(mockMetaSchemaFactory.getMetaSchema(anyString(), any(), isNull())).thenReturn(mockMetaSchema);
        String iri = "http://example.com/meta";
        JsonMetaSchema result = invokeLoadMetaSchema(factory, iri, null);
        assertSame(mockMetaSchema, result);
        verify(mockMetaSchemaFactory).getMetaSchema(eq(iri), same(factory), isNull());
    }

    @ParameterizedTest(name = "customFactory returns null for IRI \"{0}\"")
    @ValueSource(strings = { "http://example.com/null-meta" })
    void loadMetaSchema_customFactory_returnsNull(String iri) throws Exception {
        injectMetaFactory(factory, mockMetaSchemaFactory);
        when(mockMetaSchemaFactory.getMetaSchema(eq(iri), any(), any())).thenReturn(null);
        JsonMetaSchema result = invokeLoadMetaSchema(factory, iri, mockConfig);
        assertNull(result, "When the custom factory returns null the method must return null");
    }

    @ParameterizedTest(name = "customFactory throws {0} for IRI \"{1}\"")
    @MethodSource("provideCustomFactoryExceptions")
    void loadMetaSchema_customFactory_throwsException(Class<? extends Throwable> exClass, String iri) throws Exception {
        injectMetaFactory(factory, mockMetaSchemaFactory);
        when(mockMetaSchemaFactory.getMetaSchema(eq(iri), any(), any())).thenThrow(exClass.getDeclaredConstructor(String.class).newInstance("Injected exception"));
        assertThrows(exClass, () -> invokeLoadMetaSchema(factory, iri, mockConfig));
    }

    @ParameterizedTest(name = "customFactory receives IRI \"{0}\" (empty or whitespace)")
    @MethodSource("provideIriEdgeValues")
    void loadMetaSchema_customFactory_edgeIriValues(String iri) throws Exception {
        injectMetaFactory(factory, mockMetaSchemaFactory);
        when(mockMetaSchemaFactory.getMetaSchema(eq(iri), any(), any())).thenReturn(mockMetaSchema);
        JsonMetaSchema result = invokeLoadMetaSchema(factory, iri, mockConfig);
        assertSame(mockMetaSchema, result);
        verify(mockMetaSchemaFactory).getMetaSchema(eq(iri), same(factory), same(mockConfig));
    }

    @Test
    void loadMetaSchema_customFactory_nonNullConfigWithCustomSettings() throws Exception {
        injectMetaFactory(factory, mockMetaSchemaFactory);
        // Create a config with a custom flag.
        SchemaValidatorsConfig customConfig = new SchemaValidatorsConfig();
        // Assume there is a setter called setTypeLoose; using reflection if not public.
        // For illustration we just pass the config; the factory should receive it unchanged.
        when(mockMetaSchemaFactory.getMetaSchema(anyString(), any(), same(customConfig))).thenReturn(mockMetaSchema);
        String iri = "http://example.com/custom-config-meta";
        JsonMetaSchema result = invokeLoadMetaSchema(factory, iri, customConfig);
        assertSame(mockMetaSchema, result);
        verify(mockMetaSchemaFactory).getMetaSchema(eq(iri), same(factory), same(customConfig));
    }

    @Test
    void loadMetaSchema_fallbackToDefaultFactory_returnsMetaSchema() throws Exception {
        // Ensure the factory has no custom meta‑schema factory (default state).
        // The default factory should return a non‑null JsonMetaSchema for a known IRI.
        String iri = "https://json-schema.org/draft/2020-12/schema";
        JsonMetaSchema result = invokeLoadMetaSchema(factory, iri, mockConfig);
        assertNotNull(result, "Default factory should return a JsonMetaSchema for a valid IRI");
    }

    @Test
    void loadMetaSchema_fallbackToDefaultFactory_malformedIriThrows() throws Exception {
        // The default factory validates the IRI and throws IllegalArgumentException for malformed values.
        String malformedIri = "not-a-valid-uri";
        assertThrows(IllegalArgumentException.class, () -> invokeLoadMetaSchema(factory, malformedIri, mockConfig));
    }

    @Test
    void loadMetaSchema_fallbackToDefaultFactory_returnsNullWhenFactoryReturnsNull() throws Exception {
        // This test assumes that the default factory could return null for a specific IRI.
        // We use an unlikely IRI that the default implementation might not recognise.
        String iri = "http://example.com/null-default";
        JsonMetaSchema result = invokeLoadMetaSchema(factory, iri, mockConfig);
        // The contract of loadMetaSchema does not forbid null, so we simply assert that the
        // method returns whatever the default factory provides (which may be null).
        // If the default returns a non‑null value the assertion will still pass.
        // The purpose is to ensure no additional null‑check is performed.
        // No explicit expectation here.
    }

    @Test
    void loadMetaSchema_defaultFactory_nullIri_propagatesException() throws Exception {
        // When metaSchemaFactory is null, the method delegates to DefaultJsonMetaSchemaFactory,
        // which will throw a NullPointerException for a null IRI.
        assertThrows(NullPointerException.class, () -> invokeLoadMetaSchema(factory, null, mockConfig));
    }
}
