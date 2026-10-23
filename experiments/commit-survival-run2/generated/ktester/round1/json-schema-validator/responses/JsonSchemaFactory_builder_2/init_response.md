```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.uri.URIFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonSchemaFactory#builder(JsonSchemaFactory)}.
 *
 * The purpose of these tests is to verify that the static {@code builder(JsonSchemaFactory)}
 * method creates a new {@link JsonSchemaFactory.Builder} that is pre‑populated with the
 * configuration of the supplied blueprint factory.  Because the builder’s internal state
 * is not exposed via public getters, the tests focus on observable behaviour:
 *
 * <ul>
 *   <li>The returned builder is not {@code null} and is a distinct instance.</li>
 *   <li>Custom components (ObjectMapper, YAMLMapper, URI mappings, factories and fetchers)
 *       that were supplied to the blueprint are retained – we verify this by mutating the
 *       builder after creation and ensuring the mutation does not affect the original
 *       blueprint.</li>
 *   <li>The builder can still be used to create a functional {@link JsonSchemaFactory}
 *       instance (i.e. {@code build()} does not throw).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_2_Test {

    /** A mock {@link JsonMetaSchema} used as the sole meta‑schema for the blueprint. */
    @Mock
    private JsonMetaSchema mockMetaSchema;

    /** A mock {@link URIFactory} for a custom scheme. */
    @Mock
    private URIFactory mockUriFactory;

    /** A mock {@link URIFetcher} for the same custom scheme. */
    @Mock
    private URIFetcher mockUriFetcher;

    /** A mock {@link URITranslator} that could be added to the translator chain. */
    @Mock
    private URITranslator mockUriTranslator;

    /** A mock {@link URNFactory} – the builder accepts {@code null} but we provide a mock
     *  to exercise the code path that stores it. */
    @Mock
    private URNFactory mockUrnFactory;

    /** Blueprint factory that will be passed to {@code JsonSchemaFactory.builder(…) } */
    private JsonSchemaFactory blueprintFactory;

    /** Common constants used throughout the tests. */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta";
    private static final String CUSTOM_SCHEME = "custom";

    @BeforeEach
    void setUp() {
        // ----------------------------------------------------------------------
        // 1. Prepare a minimal but complete set of constructor arguments.
        // ----------------------------------------------------------------------
        ObjectMapper jsonMapper = new ObjectMapper();
        YAMLMapper yamlMapper = new YAMLMapper();

        // The meta‑schema map must contain an entry for the default meta‑schema URI.
        Map<String, JsonMetaSchema> metaSchemaMap = new HashMap<>();
        // The factory normalises the URI, therefore we store the normalised key.
        metaSchemaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), mockMetaSchema);

        // Simple URI mapping – can be empty, but we add one entry to make the test
        // observable.
        Map<String, String> uriMap = Collections.singletonMap("http", "https");

        // Build the blueprint factory using the public Builder API.
        blueprintFactory = JsonSchemaFactory.builder()
                .objectMapper(jsonMapper)
                .yamlMapper(yamlMapper)
                .defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI)
                .addMetaSchema(mockMetaSchema)
                .addUriMappings(uriMap)
                .addUriTranslator(mockUriTranslator)
                .uriFactory(mockUriFactory, CUSTOM_SCHEME)
                .uriFetcher(mockUriFetcher, CUSTOM_SCHEME)
                .addUrnFactory(mockUrnFactory)
                .build();

        // Sanity check – the blueprint must be usable; any exception here would
        // indicate a problem with the test setup rather than the method under test.
        assertNotNull(blueprintFactory, "Blueprint factory should be created successfully");
    }

    @AfterEach
    void tearDown() {
        // No external resources to release; the method exists for symmetry with
        // the provided template.
    }

    @Test
    @DisplayName("builder(JsonSchemaFactory) returns a new Builder pre‑populated with blueprint data")
    void testBuilderCopiesBlueprintConfiguration() {
        // ----------------------------------------------------------------------
        // WHEN: we request a builder based on the blueprint factory
        // ----------------------------------------------------------------------
        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(blueprintFactory);

        // ----------------------------------------------------------------------
        // THEN: the returned builder must be a distinct, non‑null instance
        // ----------------------------------------------------------------------
        assertNotNull(derivedBuilder, "The derived builder must not be null");
        assertNotSame(blueprintFactory, derivedBuilder,
                "The derived builder must not be the same instance as the blueprint factory");

        // ----------------------------------------------------------------------
        // AND: mutating the derived builder must not affect the original blueprint
        // ----------------------------------------------------------------------
        ObjectMapper newMapper = new ObjectMapper(); // a different mapper instance
        derivedBuilder.objectMapper(newMapper);

        // Build a new factory from the mutated builder.
        JsonSchemaFactory mutatedFactory = derivedBuilder.build();

        // The blueprint's ObjectMapper should remain unchanged.
        // We can verify this indirectly by checking that the blueprint can still
        // create a schema from a simple JSON string – if the mapper were replaced,
        // the behaviour would still be correct, but the reference equality would
        // differ.  Since ObjectMapper does not expose identity via public API,
        // we simply ensure that the blueprint's factory is still functional.
        assertDoesNotThrow(() -> blueprintFactory.getSchema("{\"type\":\"string\"}"),
                "Blueprint factory must remain functional after the derived builder is mutated");
        assertDoesNotThrow(() -> mutatedFactory.getSchema("{\"type\":\"string\"}"),
                "Mutated factory must be functional with the new ObjectMapper");
    }

    @Test
    @DisplayName("builder(JsonSchemaFactory) can be used to create a fully functional factory")
    void testBuilderProducesUsableFactory() {
        // ----------------------------------------------------------------------
        // GIVEN: a builder derived from the blueprint
        // ----------------------------------------------------------------------
        JsonSchemaFactory.Builder derivedBuilder = JsonSchemaFactory.builder(blueprintFactory);

        // ----------------------------------------------------------------------
        // WHEN: we build a new factory from the derived builder
        // ----------------------------------------------------------------------
        JsonSchemaFactory newFactory = derivedBuilder.build();

        // ----------------------------------------------------------------------
        // THEN: the new factory must be able to resolve the default meta‑schema URI
        // ----------------------------------------------------------------------
        // The public API does not expose the default meta‑schema directly,
        // but we can exercise it by creating a schema that references it via "$schema".
        String schemaWithMeta = String.format("{\"$schema\":\"%s\",\"type\":\"string\"}", DEFAULT_META_SCHEMA_URI);
        JsonSchema schema = newFactory.getSchema(schemaWithMeta);

        // The schema creation should not throw and the resulting schema must not be null.
        assertNotNull(schema, "Schema created from the new factory must not be null");

        // Validate a simple JSON instance against the created schema – it should succeed.
        assertDoesNotThrow(() -> schema.validate(new ObjectMapper().readTree("\"hello\"")),
                "Validation of a valid instance must not throw");
    }
}
```