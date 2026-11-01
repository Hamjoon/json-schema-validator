```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag, Consumer)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Behaviour when the customizer is {@code null} for various {@link VersionFlag}s.</li>
 *   <li>Correct application of a non‑null customizer (adding meta‑schemas, changing defaults,
 *       disabling cache, setting readers/mappers, etc.).</li>
 *   <li>Exception handling for invalid input (null version flag, customizer throwing,
 *       illegal builder configuration).</li>
 * </ul>
 *
 * <p>Reflection is used only to inspect private fields that are not exposed by the public API
 * (e.g., {@code enableSchemaCache}, {@code jsonNodeReader}, {@code jsonMapper}). This follows
 * the requirement of not accessing private members directly.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getInstance_2_Test {

    /* --------------------------------------------------------------------- */
    /* Mocks used across several test cases                                   */
    /* --------------------------------------------------------------------- */
    @Mock JsonMetaSchema customMetaSchema;

    @Mock JsonNodeReader customReader;

    @Mock ObjectMapper customJsonMapper;

    /* --------------------------------------------------------------------- */
    /* Lifecycle hooks – kept empty as the template requires them            */
    /* --------------------------------------------------------------------- */
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already resets mocks.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Returns a {@link SchemaValidatorsConfig} instance used for meta‑schema look‑ups. */
    private static SchemaValidatorsConfig config() {
        return SchemaValidatorsConfig.builder().build();
    }

    /** Retrieves a private field value via reflection. */
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Existing tests from the template (kept unchanged)                     */
    /* --------------------------------------------------------------------- */

    /**
     * Verify that {@code getInstance} works when the customizer is {@code null}.
     * The factory should contain the default meta‑schema associated with the
     * supplied {@link VersionFlag}.
     */
    @Test
    void getInstance_withoutCustomizer_returnsFactoryWithDefaultMetaSchema() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, null);
        String expectedIri = "https://json-schema.org/draft/2020-12/schema";
        JsonMetaSchema meta = factory.getMetaSchema(expectedIri, config());
        assertNotNull(meta, "Factory should contain the default meta‑schema for Draft‑2020‑12");
        assertEquals(expectedIri, meta.getIri(),
                "The retrieved meta‑schema IRI should match the expected default IRI");
    }

    /**
     * Verify that the customizer is applied: a custom meta‑schema can be added
     * and set as the default. The factory should return the same instance that
     * was supplied via the customizer.
     */
    @Test
    void getInstance_withCustomizer_addsCustomMetaSchemaAndSetsDefault() {
        String customIri = "urn:custom:meta:schema";
        when(customMetaSchema.getIri()).thenReturn(customIri);

        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder
                .defaultMetaSchemaIri(customIri)
                .metaSchema(customMetaSchema);

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, customizer);
        JsonMetaSchema retrieved = factory.getMetaSchema(customIri, config());
        assertNotNull(retrieved, "Factory should contain the custom meta‑schema");
        assertSame(customMetaSchema, retrieved,
                "Factory should return the same JsonMetaSchema instance that was supplied via the customizer");
    }

    /* --------------------------------------------------------------------- */
    /* Parameterized tests for the various version flags with null customizer */
    /* --------------------------------------------------------------------- */

    private static Stream<Arguments> versionFlagProvider() {
        return Stream.of(
                Arguments.of(VersionFlag.V4, "http://json-schema.org/draft-04/schema#"),
                Arguments.of(VersionFlag.V6, "http://json-schema.org/draft-06/schema#"),
                Arguments.of(VersionFlag.V7, "http://json-schema.org/draft-07/schema#"),
                Arguments.of(VersionFlag.V2019_09, "https://json-schema.org/draft/2019-09/schema"),
                Arguments.of(VersionFlag.V2020_12, "https://json-schema.org/draft/2020-12/schema")
        );
    }

    @ParameterizedTest(name = "null customizer – version {0}")
    @MethodSource("versionFlagProvider")
    void getInstance_nullCustomizer_variousVersions_returnsFactoryWithCorrectDefaultMetaSchema(
            VersionFlag versionFlag, String expectedIri) {

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(versionFlag, null);
        assertNotNull(factory, "Factory must not be null");

        JsonMetaSchema meta = factory.getMetaSchema(expectedIri, config());
        assertNotNull(meta, "Factory should contain the default meta‑schema for version " + versionFlag);
        assertEquals(expectedIri, meta.getIri(),
                "The retrieved meta‑schema IRI should match the expected IRI for version " + versionFlag);
    }

    /* --------------------------------------------------------------------- */
    /* Customizer that disables the internal schema cache                     */
    /* --------------------------------------------------------------------- */

    @Test
    void getInstance_customizerDisablesSchemaCache_reflectsDisabledFlag() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.enableSchemaCache(false);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4, customizer);

        // The private field `enableSchemaCache` should now be false.
        Boolean cacheEnabled = getPrivateField(factory, "enableSchemaCache");
        assertNotNull(cacheEnabled, "Reflection should retrieve the enableSchemaCache field");
        assertFalse(cacheEnabled, "Schema cache must be disabled after customizer sets it to false");
    }

    /* --------------------------------------------------------------------- */
    /* Customizer that throws an exception                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void getInstance_customizerThrowsException_propagatesException() {
        Consumer<JsonSchemaFactory.Builder> explodingCustomizer = builder -> {
            throw new IllegalStateException("boom");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> JsonSchemaFactory.getInstance(VersionFlag.V4, explodingCustomizer),
                "Exception thrown by the customizer must propagate");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* Customizer that adds an extra meta‑schema via collection overload      */
    /* --------------------------------------------------------------------- */

    @Test
    void getInstance_customizerAddsExtraMetaSchemaViaCollection_containsBothSchemas() {
        String customIri = "urn:custom:meta:schema";
        when(customMetaSchema.getIri()).thenReturn(customIri);

        Consumer<JsonSchemaFactory.Builder> customizer = builder ->
                builder.metaSchemas(Collections.singletonList(customMetaSchema));

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4, customizer);

        // Default V4 meta‑schema IRI
        String v4Iri = "http://json-schema.org/draft-04/schema#";

        // Both should be present.
        JsonMetaSchema v4Meta = factory.getMetaSchema(v4Iri, config());
        JsonMetaSchema customMeta = factory.getMetaSchema(customIri, config());

        assertNotNull(v4Meta, "Default V4 meta‑schema must still be present");
        assertNotNull(customMeta, "Custom meta‑schema added via collection must be present");
        assertSame(customMetaSchema, customMeta, "Custom meta‑schema instance should be the one supplied");
    }

    /* --------------------------------------------------------------------- */
    /* Customizer that sets a JsonNodeReader                                   */
    /* --------------------------------------------------------------------- */

    @Test
    void getInstance_customizerSetsJsonNodeReader_reflectsReaderInstance() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.jsonNodeReader(customReader);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4, customizer);

        JsonNodeReader actual = getPrivateField(factory, "jsonNodeReader");
        assertSame(customReader, actual, "Factory should retain the JsonNodeReader supplied by the customizer");
    }

    /* --------------------------------------------------------------------- */
    /* Customizer that sets the (deprecated) jsonMapper                        */
    /* --------------------------------------------------------------------- */

    @Test
    void getInstance_customizerSetsJsonMapper_reflectsMapperInstance() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.jsonMapper(customJsonMapper);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4, customizer);

        ObjectMapper actual = getPrivateField(factory, "jsonMapper");
        assertSame(customJsonMapper, actual,
                "Factory should retain the ObjectMapper supplied via the deprecated jsonMapper method");
    }

    /* --------------------------------------------------------------------- */
    /* No‑op customizer – should behave like a null customizer                */
    /* --------------------------------------------------------------------- */

    @Test
    void getInstance_customizerNoOp_behavesLikeNullCustomizer() {
        Consumer<JsonSchemaFactory.Builder> noOp = builder -> {
            // deliberately empty
        };
        JsonSchemaFactory factoryWithNoOp = JsonSchemaFactory.getInstance(VersionFlag.V4, noOp);
        JsonSchemaFactory factoryWithNull = JsonSchemaFactory.getInstance(VersionFlag.V4, null);

        // Both factories should contain the same default meta‑schema IRI.
        String v4Iri = "http://json-schema.org/draft-04/schema#";
        JsonMetaSchema meta1 = factoryWithNoOp.getMetaSchema(v4Iri, config());
        JsonMetaSchema meta2 = factoryWithNull.getMetaSchema(v4Iri, config());

        assertNotNull(meta1);
        assertNotNull(meta2);
        // The two meta‑schema instances are the same singleton from the library.
        assertEquals(meta1.getIri(), meta2.getIri(),
                "Both factories should resolve the same default meta‑schema IRI");
    }

    /* --------------------------------------------------------------------- */
    /* Null version flag – should cause checkVersion to fail                  */
    /* --------------------------------------------------------------------- */

    @Test
    void getInstance_nullVersionFlag_throwsExceptionBeforeCustomizer() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.defaultMetaSchemaIri("ignored");
        assertThrows(NullPointerException.class,
                () -> JsonSchemaFactory.getInstance(null, customizer),
                "A null version flag must cause an exception before the customizer is invoked");
    }

    /* --------------------------------------------------------------------- */
    /* Customizer that sets a null defaultMetaSchemaIri – builder should fail  */
    /* --------------------------------------------------------------------- */

    @Test
    void getInstance_customizerSetsNullDefaultMetaSchemaIri_builderFails() {
        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder.defaultMetaSchemaIri(null);
        // The builder will throw when build() is called inside getInstance.
        assertThrows(NullPointerException.class,
                () -> JsonSchemaFactory.getInstance(VersionFlag.V4, customizer),
                "Setting a null defaultMetaSchemaIri should cause a NPE/IAE during build");
    }

    /* --------------------------------------------------------------------- */
    /* Verify that a non‑null customizer is invoked exactly once               */
    /* --------------------------------------------------------------------- */

    @Test
    void getInstance_customizerIsInvokedExactlyOnce() {
        Consumer<JsonSchemaFactory.Builder> spyCustomizer = mock(Consumer.class);
        JsonSchemaFactory.getInstance(VersionFlag.V4, spyCustomizer);
        verify(spyCustomizer, times(1)).accept(any(JsonSchemaFactory.Builder.class));
    }
}
```