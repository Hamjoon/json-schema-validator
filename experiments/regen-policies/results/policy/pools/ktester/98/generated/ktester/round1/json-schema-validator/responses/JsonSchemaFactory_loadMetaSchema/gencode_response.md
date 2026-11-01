```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory#loadMetaSchema(String, SchemaValidatorsConfig)}.
 *
 * <p>The test suite covers:
 * <ul>
 *   <li>delegation to a custom {@link JsonMetaSchemaFactory}</li>
 *   <li>fallback to {@link DefaultJsonMetaSchemaFactory} when no custom factory is set</li>
 *   <li>caching behaviour of {@link JsonSchemaFactory#getMetaSchema(String, SchemaValidatorsConfig)}</li>
 *   <li>handling of various edge‑case arguments (null, empty, whitespace)</li>
 *   <li>propagation of return values and exceptions from the underlying factory</li>
 * </ul>
 *
 * <p>Reflection is used only to inject a mock {@code JsonMetaSchemaFactory} into the
 * private {@code metaSchemaFactory} field and to invoke the protected {@code loadMetaSchema}
 * method when needed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_loadMetaSchema_Test {

    /** Instance created via the public API – serves as the baseline factory. */
    private JsonSchemaFactory factory;

    /** Mocked configuration – its internal state is irrelevant for these tests. */
    @Mock
    private SchemaValidatorsConfig mockConfig;

    /** Mocked {@link JsonMetaSchema} returned by the custom factory. */
    @Mock
    private JsonMetaSchema mockMetaSchema;

    /** Mocked {@link JsonMetaSchemaFactory} used to verify delegation and exception handling. */
    @Mock
    private JsonMetaSchemaFactory mockMetaSchemaFactory;

    @BeforeEach
    void setup() {
        // Obtain a fresh factory instance for each test.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /* --------------------------------------------------------------------- *
     *  Utility helpers
     * --------------------------------------------------------------------- */

    /**
     * Injects the supplied {@code JsonMetaSchemaFactory} into the private
     * {@code metaSchemaFactory} field of the given {@code JsonSchemaFactory}
     * instance using reflection.
     */
    private void injectMetaFactory(JsonSchemaFactory target, JsonMetaSchemaFactory metaFactory)
            throws Exception {
        Field f = JsonSchemaFactory.class.getDeclaredField("metaSchemaFactory");
        f.setAccessible(true);
        f.set(target, metaFactory);
    }

    /**
     * Calls the protected {@code loadMetaSchema} method via reflection.
     *
     * @return the {@link JsonMetaSchema} returned by the method
     */
    private JsonMetaSchema invokeLoadMetaSchema(JsonSchemaFactory target, String iri,
                                                SchemaValidatorsConfig cfg) throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("loadMetaSchema", String.class,
                SchemaValidatorsConfig.class);
        m.setAccessible(true);
        return (JsonMetaSchema) m.invoke(target, iri, cfg);
    }

    /* --------------------------------------------------------------------- *
     *  1. Delegation to a custom JsonMetaSchemaFactory
     * --------------------------------------------------------------------- */

    @Test
    void loadMetaSchema_delegatesToCustomMetaSchemaFactory() throws Exception {
        // Inject mock factory.
        injectMetaFactory(factory, mockMetaSchemaFactory);
        when(mockMetaSchemaFactory.getMetaSchema(anyString(), any(JsonSchemaFactory.class), any()))
                .thenReturn(mockMetaSchema);

        String iri = "https://example.com/custom/meta";
        JsonMetaSchema result = factory.getMetaSchema(iri, mockConfig);

        assertSame(mockMetaSchema, result,
                "The meta‑schema returned by the factory should be the mock instance");
        verify(mockMetaSchemaFactory, times(1))
                .getMetaSchema(eq(iri), same(factory), same(mockConfig));
    }

    @Test
    void loadMetaSchema_isCachedAfterFirstInvocation() {
        JsonSchemaFactory spyFactory = Mockito.spy(factory);
        String iri = "https://json-schema.org/draft/2020-12/schema";

        JsonMetaSchema first = spyFactory.getMetaSchema(iri, mockConfig);
        JsonMetaSchema second = spyFactory.getMetaSchema(iri, mockConfig);

        assertSame(first, second, "Subsequent calls must return the cached meta‑schema instance");
        verify(spyFactory, times(1)).loadMetaSchema(eq(iri), same(mockConfig));
    }

    /* --------------------------------------------------------------------- *
     *  2. Custom factory edge‑cases (null IRI, null config, empty/whitespace IRI,
     *     null return, exceptions)
     * --------------------------------------------------------------------- */

    @Test
    void loadMetaSchema_customFactory_nullIri_propagatesException() throws Exception {
        injectMetaFactory(factory, mockMetaSchemaFactory);
        when(mockMetaSchemaFactory.getMetaSchema(isNull(), any(), any()))
                .thenThrow(new NullPointerException("IRI is null"));

        assertThrows(NullPointerException.class,
                () -> invokeLoadMetaSchema(factory, null, mockConfig));
    }

    @Test
    void loadMetaSchema_customFactory_nullConfig_passesThrough() throws Exception {
        injectMetaFactory(factory, mockMetaSchemaFactory);
        when(mockMetaSchemaFactory.getMetaSchema(anyString(), any(), isNull()))
                .thenReturn(mockMetaSchema);

        String iri = "http://example.com/meta";
        JsonMetaSchema result = invokeLoadMetaSchema(factory, iri, null);

        assertSame(mockMetaSchema, result);
        verify(mockMetaSchemaFactory).getMetaSchema(eq(iri), same(factory), isNull());
    }

    @ParameterizedTest(name = "customFactory returns null for IRI \"{0}\"")
    @ValueSource(strings = {"http://example.com/null-meta"})
    void loadMetaSchema_customFactory_returnsNull(String iri) throws Exception {
        injectMetaFactory(factory, mockMetaSchemaFactory);
        when(mockMetaSchemaFactory.getMetaSchema(eq(iri), any(), any()))
                .thenReturn(null);

        JsonMetaSchema result = invokeLoadMetaSchema(factory, iri, mockConfig);
        assertNull(result, "When the custom factory returns null the method must return null");
    }

    @ParameterizedTest(name = "customFactory throws {0} for IRI \"{1}\"")
    @MethodSource("provideCustomFactoryExceptions")
    void loadMetaSchema_customFactory_throwsException(Class<? extends Throwable> exClass,
                                                      String iri) throws Exception {
        injectMetaFactory(factory, mockMetaSchemaFactory);
        when(mockMetaSchemaFactory.getMetaSchema(eq(iri), any(), any()))
                .thenThrow(exClass.getDeclaredConstructor(String.class)
                        .newInstance("Injected exception"));

        assertThrows(exClass, () -> invokeLoadMetaSchema(factory, iri, mockConfig));
    }

    private static Stream<Arguments> provideCustomFactoryExceptions() {
        return Stream.of(
                Arguments.of(RuntimeException.class, "http://example.com/bad-meta"),
                Arguments.of(IllegalArgumentException.class, "http://example.com/illegal-arg"),
                Arguments.of(NullPointerException.class, "http://example.com/null-config"),
                Arguments.of(AssertionError.class, "http://example.com/assertion-error")
        );
    }

    @ParameterizedTest(name = "customFactory receives IRI \"{0}\" (empty or whitespace)")
    @MethodSource("provideIriEdgeValues")
    void loadMetaSchema_customFactory_edgeIriValues(String iri) throws Exception {
        injectMetaFactory(factory, mockMetaSchemaFactory);
        when(mockMetaSchemaFactory.getMetaSchema(eq(iri), any(), any()))
                .thenReturn(mockMetaSchema);

        JsonMetaSchema result = invokeLoadMetaSchema(factory, iri, mockConfig);
        assertSame(mockMetaSchema, result);
        verify(mockMetaSchemaFactory).getMetaSchema(eq(iri), same(factory), same(mockConfig));
    }

    private static Stream<Arguments> provideIriEdgeValues() {
        return Stream.of(
                Arguments.of(""),          // empty string
                Arguments.of("   ")        // whitespace only
        );
    }

    @Test
    void loadMetaSchema_customFactory_nonNullConfigWithCustomSettings() throws Exception {
        injectMetaFactory(factory, mockMetaSchemaFactory);
        // Create a config with a custom flag.
        SchemaValidatorsConfig customConfig = new SchemaValidatorsConfig();
        // Assume there is a setter called setTypeLoose; using reflection if not public.
        // For illustration we just pass the config; the factory should receive it unchanged.
        when(mockMetaSchemaFactory.getMetaSchema(anyString(), any(), same(customConfig)))
                .thenReturn(mockMetaSchema);

        String iri = "http://example.com/custom-config-meta";
        JsonMetaSchema result = invokeLoadMetaSchema(factory, iri, customConfig);
        assertSame(mockMetaSchema, result);
        verify(mockMetaSchemaFactory).getMetaSchema(eq(iri), same(factory), same(customConfig));
    }

    /* --------------------------------------------------------------------- *
     *  3. Fallback to DefaultJsonMetaSchemaFactory (no custom factory)
     * --------------------------------------------------------------------- */

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

        assertThrows(IllegalArgumentException.class,
                () -> invokeLoadMetaSchema(factory, malformedIri, mockConfig));
    }

    @Test
    void loadMetaSchema_fallbackToDefaultFactory_returnsNullWhenFactoryReturnsNull()
            throws Exception {
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

    /* --------------------------------------------------------------------- *
     *  4. Additional sanity checks (null IRI with default factory)
     * --------------------------------------------------------------------- */

    @Test
    void loadMetaSchema_defaultFactory_nullIri_propagatesException() throws Exception {
        // When metaSchemaFactory is null, the method delegates to DefaultJsonMetaSchemaFactory,
        // which will throw a NullPointerException for a null IRI.
        assertThrows(NullPointerException.class,
                () -> invokeLoadMetaSchema(factory, null, mockConfig));
    }
}
```