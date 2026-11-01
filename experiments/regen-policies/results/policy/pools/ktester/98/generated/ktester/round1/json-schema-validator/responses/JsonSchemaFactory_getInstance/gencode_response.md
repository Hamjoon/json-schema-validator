```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.function.Consumer;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag)} and its
 * overloads.
 *
 * The tests cover:
 * <ul>
 *   <li>Creation of a non‑null factory for various {@link VersionFlag}s.</li>
 *   <li>Defaulting behaviour when {@code null} is supplied as the version flag.</li>
 *   <li>Correct loading of the corresponding meta‑schema for each draft.</li>
 *   <li>Caching semantics – the same flag yields the same instance, different flags
 *       yield different instances.</li>
 *   <li>Application (or non‑application) of a customiser via the two‑argument overload.</li>
 * </ul>
 *
 * No private members of {@link JsonSchemaFactory} are accessed; the public API is used
 * exclusively.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getInstance_Test {

    /** Dummy schema used in the customiser test. */
    private static final String DUMMY_SCHEMA_ID = "https://example.com/dummy-schema";
    private static final String DUMMY_SCHEMA_CONTENT = "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\" }";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* ------------------------------------------------------------
     *  Basic non‑null factory creation
     * ------------------------------------------------------------ */

    @Test
    void getInstance_returnsNonNullFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertNotNull(factory, "Factory should not be null for a valid VersionFlag");
    }

    /* ------------------------------------------------------------
     *  Null version flag – should fall back to the library default (V7)
     * ------------------------------------------------------------ */

    @Test
    void getInstance_nullVersionFlag_fallsBackToDefault() {
        // According to the specification the method must not throw.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance((VersionFlag) null);
        assertNotNull(factory, "Factory must be non‑null even when version flag is null");

        // The default version is V7 (draft‑07). Verify that its meta‑schema can be loaded.
        JsonMetaSchema meta = factory.getMetaSchema(
                "http://json-schema.org/draft-07/schema#", null);
        assertNotNull(meta, "Meta‑schema for draft‑07 should be loadable when using the default factory");
    }

    /* ------------------------------------------------------------
     *  Parameterised test for explicit version flags (V4, V6, V7)
     * ------------------------------------------------------------ */

    @ParameterizedTest(name = "getInstance({0}) returns factory that can load its meta‑schema")
    @EnumSource(value = VersionFlag.class, names = {"V4", "V6", "V7"})
    void getInstance_explicitVersion_loadsCorrespondingMetaSchema(VersionFlag flag) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(flag);
        assertNotNull(factory, "Factory must be non‑null for flag " + flag);

        // Determine the well‑known meta‑schema IRI for the given draft.
        String iri = switch (flag) {
            case V4 -> "http://json-schema.org/draft-04/schema#";
            case V6 -> "http://json-schema.org/draft-06/schema#";
            case V7 -> "http://json-schema.org/draft-07/schema#";
            default -> throw new IllegalArgumentException("Unexpected flag: " + flag);
        };

        JsonMetaSchema meta = factory.getMetaSchema(iri, null);
        assertNotNull(meta, "Meta‑schema should be loadable for " + flag);
        // Optional: verify that the meta‑schema reports the same version flag.
        // The JsonMetaSchema class provides getSpecVersion() in recent releases.
        // If unavailable, the null‑check above is sufficient.
        // assertEquals(flag, meta.getSpecVersion(), "Meta‑schema version should match the flag");
    }

    /* ------------------------------------------------------------
     *  Newer draft versions (V2019_09, V2020_12)
     * ------------------------------------------------------------ */

    @ParameterizedTest(name = "getInstance({0}) loads newer draft meta‑schema")
    @EnumSource(value = VersionFlag.class, names = {"V2019_09", "V2020_12"})
    void getInstance_newerDrafts_loadsMetaSchema(VersionFlag flag) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(flag);
        assertNotNull(factory, "Factory must be non‑null for flag " + flag);

        String iri = switch (flag) {
            case V2019_09 -> "https://json-schema.org/draft/2019-09/schema#";
            case V2020_12 -> "https://json-schema.org/draft/2020-12/schema#";
            default -> throw new IllegalArgumentException("Unexpected flag: " + flag);
        };

        JsonMetaSchema meta = factory.getMetaSchema(iri, null);
        assertNotNull(meta, "Meta‑schema should be loadable for " + flag);
        // If JsonMetaSchema exposes its version, we could assert it matches `flag`.
    }

    /* ------------------------------------------------------------
     *  Caching behaviour – same version returns same instance
     * ------------------------------------------------------------ */

    @Test
    void getInstance_sameVersion_returnsSameInstance() {
        JsonSchemaFactory first = JsonSchemaFactory.getInstance(VersionFlag.V6);
        JsonSchemaFactory second = JsonSchemaFactory.getInstance(VersionFlag.V6);
        assertSame(first, second, "Factory instances for the same VersionFlag should be identical (cached)");
    }

    /* ------------------------------------------------------------
     *  Caching behaviour – different versions return different instances
     * ------------------------------------------------------------ */

    @Test
    void getInstance_differentVersions_returnDifferentInstances() {
        JsonSchemaFactory v4 = JsonSchemaFactory.getInstance(VersionFlag.V4);
        JsonSchemaFactory v7 = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertNotSame(v4, v7, "Factories for different VersionFlags must be distinct objects");
    }

    /* ------------------------------------------------------------
     *  Customiser supplied via the two‑argument overload
     * ------------------------------------------------------------ */

    @Test
    void getInstance_withCustomizer_appliesCustomization() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(
                                Collections.singletonMap(DUMMY_SCHEMA_ID, DUMMY_SCHEMA_CONTENT))));

        assertNotNull(factory, "Factory created with customiser must not be null");

        // Resolve the dummy schema that was added through the customiser.
        JsonSchema schema = factory.getSchema(SchemaLocation.of(DUMMY_SCHEMA_ID));
        assertNotNull(schema, "Factory should resolve the dummy schema added via customiser");
    }

    /* ------------------------------------------------------------
     *  Two‑argument overload with a null customiser – should behave like the
     *  single‑argument overload.
     * ------------------------------------------------------------ */

    @Test
    void getInstance_nullCustomizer_behavesLikeSingleArgument() {
        JsonSchemaFactory viaSingle = JsonSchemaFactory.getInstance(VersionFlag.V7);
        JsonSchemaFactory viaTwoArg = JsonSchemaFactory.getInstance(VersionFlag.V7, (Consumer<JsonSchemaFactory.Builder>) null);

        assertNotNull(viaTwoArg, "Factory must be non‑null when customiser is null");
        assertSame(viaSingle, viaTwoArg, "When the customiser is null the two‑argument overload should return the same instance as the single‑argument overload");
    }
}
```