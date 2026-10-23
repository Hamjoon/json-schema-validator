```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All supported draft version flags – the factory must hold the correct default meta‑schema URI
 *       and must register exactly that meta‑schema.</li>
 *   <li>Behaviour on {@code null} version flag.</li>
 *   <li>Factory instance uniqueness (no internal caching).</li>
 *   <li>Verification that the Builder’s default configuration (cache enabled, no custom translators)
 *       is preserved.</li>
 * </ul>
 *
 * <p>Reflection is used to inspect the private state of {@link JsonSchemaFactory}
 * because the class purposefully does not expose getters for those fields.
 */
class JsonSchemaFactoryGetInstanceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Reads a private (or package‑private) field via reflection. */
    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(target);
            return type.cast(value);
        } catch (NoSuchFieldException e) {
            fail("Field '" + name + "' not found in class " + target.getClass(), e);
            throw new AssertionError(e); // unreachable
        } catch (IllegalAccessException e) {
            fail("Unable to access field '" + name + "' in class " + target.getClass(), e);
            throw new AssertionError(e);
        }
    }

    /** Normalises a meta‑schema URI using the factory's static helper (mirrors internal behaviour). */
    private static String normalise(String uri) {
        try {
            // The method is package‑private static; use reflection to invoke it.
            java.lang.reflect.Method m = JsonSchemaFactory.class
                    .getDeclaredMethod("normalizeMetaSchemaUri", String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Parameter sources                                                     */
    /* --------------------------------------------------------------------- */

    /** All supported version flags together with their expected default meta‑schema URI. */
    private static Stream<Arguments> supportedVersionFlags() {
        return Stream.of(
                Arguments.of(VersionFlag.V4,  "http://json-schema.org/draft-04/schema#"),
                Arguments.of(VersionFlag.V6,  "http://json-schema.org/draft-06/schema#"),
                Arguments.of(VersionFlag.V7,  "http://json-schema.org/draft-07/schema#"),
                Arguments.of(VersionFlag.V2019_09, "https://json-schema.org/draft/2019-09/schema#"),
                Arguments.of(VersionFlag.V2020_12, "https://json-schema.org/draft/2020-12/schema#")
        );
    }

    /** The same set as above but only for the meta‑schema‑registration assertions. */
    private static Stream<Arguments> metaSchemaRegistrationFlags() {
        return Stream.of(
                Arguments.of(VersionFlag.V6,  "http://json-schema.org/draft-06/schema#"),
                Arguments.of(VersionFlag.V2019_09, "https://json-schema.org/draft/2019-09/schema#")
        );
    }

    /* --------------------------------------------------------------------- */
    /* Test cases                                                            */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index} => flag={0}, expectedUri={1}")
    @MethodSource("supportedVersionFlags")
    @DisplayName("Valid version flags – defaultMetaSchemaURI and meta‑schema registration")
    void getInstance_WithSupportedFlag_ReturnsFactoryWithCorrectMetaSchema(
            VersionFlag flag, String expectedMetaSchemaUri) {

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(flag);
        assertNotNull(factory, "Factory must not be null");

        // ----- verify defaultMetaSchemaURI -----
        String actualDefaultUri = getField(factory, "defaultMetaSchemaURI", String.class);
        assertEquals(expectedMetaSchemaUri, actualDefaultUri,
                "defaultMetaSchemaURI must match the expected URI for flag " + flag);

        // ----- verify internal jsonMetaSchemas map -----
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, JsonMetaSchema> metaMap =
                getField(factory, "jsonMetaSchemas", ConcurrentMap.class);

        assertNotNull(metaMap, "jsonMetaSchemas map must be present");
        assertEquals(1, metaMap.size(),
                "Exactly one meta‑schema must be registered for flag " + flag);

        String normalizedKey = normalise(expectedMetaSchemaUri);
        assertTrue(metaMap.containsKey(normalizedKey),
                "Meta‑schema map must contain the normalized key '" + normalizedKey + "'");
    }

    @Test
    @DisplayName("Null version flag should cause IllegalArgumentException")
    void getInstance_NullVersionFlag_ShouldThrowException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> JsonSchemaFactory.getInstance(null));
        assertTrue(ex.getMessage().toLowerCase().contains("null"),
                "Exception message should mention null");
    }

    @Test
    @DisplayName("Two successive calls with the same flag produce distinct factories")
    void getInstance_TwiceWithSameFlag_ProducesDistinctInstances() {
        JsonSchemaFactory f1 = JsonSchemaFactory.getInstance(VersionFlag.V4);
        JsonSchemaFactory f2 = JsonSchemaFactory.getInstance(VersionFlag.V4);
        assertNotSame(f1, f2, "Factory instances must be different objects");
    }

    @Test
    @DisplayName("Two successive calls with the latest flag produce distinct factories")
    void getInstance_TwiceWithLatestFlag_ProducesDistinctInstances() {
        JsonSchemaFactory f1 = JsonSchemaFactory.getInstance(VersionFlag.V2020_12);
        JsonSchemaFactory f2 = JsonSchemaFactory.getInstance(VersionFlag.V2020_12);
        assertNotSame(f1, f2, "Factory instances must be different objects");
    }

    @ParameterizedTest(name = "{index} => flag={0}, expectedUri={1}")
    @MethodSource("metaSchemaRegistrationFlags")
    @DisplayName("Meta‑schema registration count & key normalisation")
    void getInstance_MetaSchemaMap_HasSingleNormalisedKey(
            VersionFlag flag, String expectedMetaSchemaUri) {

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(flag);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, JsonMetaSchema> metaMap =
                getField(factory, "jsonMetaSchemas", ConcurrentMap.class);

        assertEquals(1, metaMap.size(),
                "Factory should contain exactly one registered meta‑schema");

        String expectedKey = normalise(expectedMetaSchemaUri);
        assertTrue(metaMap.containsKey(expectedKey),
                "Registered key must be the normalised URI: " + expectedKey);
    }

    @Test
    @DisplayName("Builder defaults are untouched – cache enabled & no URI translators")
    void getInstance_BuilderDefaults_Unmodified() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);

        // enableUriSchemaCache defaults to true
        Boolean cacheEnabled = getField(factory, "enableUriSchemaCache", Boolean.class);
        assertTrue(cacheEnabled, "enableUriSchemaCache should be true by default");

        // uriTranslators is a CompositeURITranslator – its internal collection should be empty
        Object translator = getField(factory, "uriTranslators", Object.class);
        try {
            Field listField = translator.getClass().getDeclaredField("translators");
            listField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) listField.get(translator);
            assertTrue(list.isEmpty(), "CompositeURITranslator should contain no translators by default");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unable to inspect CompositeURITranslator internals", e);
        }
    }

    /**
     * Additional sanity check – the factory returned by {@code getInstance}
     * must be usable to compile a minimal schema and validate a trivial JSON
     * document. This mirrors the original test that already existed in the
     * scaffold but is kept here for completeness.
     */
    @Test
    @DisplayName("Factory can parse a minimal schema and validate an instance")
    void getInstance_CanParseAndValidateSimpleSchema() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        String schemaJson = "{}"; // any JSON value is valid
        JsonSchema schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Returned JsonSchema must not be null");

        JsonNode instance = MAPPER.readTree("{\"foo\":\"bar\"}");
        assertDoesNotThrow(() -> schema.validate(instance),
                "Validation of a conforming instance must not throw");
    }
}
```