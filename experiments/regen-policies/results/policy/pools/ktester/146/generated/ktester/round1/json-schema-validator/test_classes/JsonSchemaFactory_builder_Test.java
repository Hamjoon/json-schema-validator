package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_Test {

    /**
     * Supplies the arguments for {@link #builderBasicFunctionality}.
     */
    private static Stream<Arguments> builderBasicScenarios() {
        JsonSchemaFactory.Builder first = JsonSchemaFactory.builder();
        JsonSchemaFactory.Builder second = JsonSchemaFactory.builder();
        return Stream.of(Arguments.of("Calling JsonSchemaFactory.builder() should return a non‑null Builder object.", first, null), Arguments.of("Two consecutive calls to JsonSchemaFactory.builder() must return distinct Builder objects.", first, second));
    }

    /**
     * Verify that the no‑arg {@code builder()} method returns a fresh Builder
     * instance on each invocation.
     */
    @Test
    void builderReturnsNewInstanceEachCall() {
        JsonSchemaFactory.Builder first = JsonSchemaFactory.builder();
        JsonSchemaFactory.Builder second = JsonSchemaFactory.builder();
        assertNotNull(first, "First builder instance must not be null");
        assertNotNull(second, "Second builder instance must not be null");
        assertNotSame(first, second, "Each call to builder() should return a distinct Builder");
    }

    /**
     * Verify that the {@code builder(JsonSchemaFactory)} overload creates a Builder
     * that can be used to produce a factory with the same default meta‑schema IRI as the
     * blueprint. The test uses the public {@code getMetaSchema} API to compare the
     * resulting factories.
     */
    @Test
    void builderWithBlueprintCopiesConfiguration() {
        // Create a baseline factory using the public getInstance API.
        JsonSchemaFactory original = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, b -> b.defaultMetaSchemaIri("https://json-schema.org/draft/2020-12/schema"));
        // Obtain a builder based on the original factory.
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(original);
        assertNotNull(copyBuilder, "Builder created from blueprint must not be null");
        // Build a new factory from the copy builder.
        JsonSchemaFactory copy = copyBuilder.build();
        assertNotNull(copy, "Factory built from blueprint builder must not be null");
        // Both factories should resolve the same meta‑schema for the default IRI.
        SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();
        JsonMetaSchema originalMeta = original.getMetaSchema("https://json-schema.org/draft/2020-12/schema", cfg);
        JsonMetaSchema copyMeta = copy.getMetaSchema("https://json-schema.org/draft/2020-12/schema", cfg);
        assertSame(originalMeta, copyMeta, "MetaSchema obtained from the copied factory should be the same instance as the original");
    }

    /**
     * Verify that a Builder can successfully build a {@link JsonSchemaFactory}
     * instance and that the resulting factory is functional (i.e., it can read a
     * simple JSON schema).
     */
    @Test
    void builderCanBuildFunctionalFactory() throws Exception {
        // Build a factory with the default meta‑schema IRI.
        JsonSchemaFactory factory = JsonSchemaFactory.builder().defaultMetaSchemaIri("https://json-schema.org/draft/2020-12/schema").build();
        assertNotNull(factory, "Factory built by Builder must not be null");
        // A minimal valid JSON schema (the empty schema) should be accepted.
        String emptySchema = "{}";
        JsonSchema schema = factory.getSchema(emptySchema);
        assertNotNull(schema, "Factory should be able to produce a JsonSchema from a minimal schema");
    }

    /**
     * Parameterized test that verifies the basic contract of the no‑arg
     * {@code builder()} method:
     * <ul>
     *   <li>the returned Builder is non‑null</li>
     *   <li>consecutive calls yield distinct Builder objects</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("builderBasicScenarios")
    void builderBasicFunctionality(String description, JsonSchemaFactory.Builder builder, JsonSchemaFactory.Builder anotherBuilder) {
        assertNotNull(builder, description + " – Builder must not be null");
        if (anotherBuilder != null) {
            assertNotSame(builder, anotherBuilder, description + " – Subsequent call must return a different Builder instance");
        }
    }

    /**
     * Ensure that {@code builder(null)} behaves like the no‑arg overload:
     * it must not throw and must return a fresh Builder each time.
     */
    @Test
    void builderWithNullBlueprintReturnsFreshBuilders() {
        JsonSchemaFactory.Builder first = JsonSchemaFactory.builder((JsonSchemaFactory) null);
        JsonSchemaFactory.Builder second = JsonSchemaFactory.builder((JsonSchemaFactory) null);
        assertNotNull(first, "Builder from builder(null) must not be null");
        assertNotNull(second, "Second Builder from builder(null) must not be null");
        assertNotSame(first, second, "Each call with null blueprint should produce a distinct Builder");
    }

    /**
     * Verify that configurations applied to one Builder do not leak into a
     * subsequently created Builder.
     *
     * <p>The test configures a custom default meta‑schema IRI on the first
     * builder, builds a factory, and then builds a second factory without that
     * customisation. The two factories must resolve different meta‑schemas for
     * the custom IRI, proving that the configuration is isolated per Builder.
     */
    @Test
    void independentConfigurationsBetweenBuilders() {
        // First builder – custom default meta‑schema IRI.
        String customIri = "https://example.com/custom-meta-schema";
        JsonSchemaFactory factoryWithCustomIri = JsonSchemaFactory.builder().defaultMetaSchemaIri(customIri).build();
        // Second builder – no customisation (uses library defaults).
        JsonSchemaFactory defaultFactory = JsonSchemaFactory.builder().build();
        assertNotNull(factoryWithCustomIri, "Factory built from customised Builder must not be null");
        assertNotNull(defaultFactory, "Factory built from default Builder must not be null");
        SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();
        // The custom factory should resolve the custom IRI.
        JsonMetaSchema customMeta = factoryWithCustomIri.getMetaSchema(customIri, cfg);
        assertNotNull(customMeta, "Factory with custom IRI should be able to resolve its configured meta‑schema");
        // The default factory should *not* resolve the custom IRI (returns null or a different instance).
        JsonMetaSchema defaultMeta = defaultFactory.getMetaSchema(customIri, cfg);
        assertTrue(defaultMeta == null || defaultMeta != customMeta, "Default factory must not share the custom meta‑schema configuration");
    }
}
