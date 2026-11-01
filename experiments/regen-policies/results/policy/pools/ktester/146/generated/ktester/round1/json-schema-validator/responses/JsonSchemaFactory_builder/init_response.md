```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#builder()} and {@link JsonSchemaFactory#builder(JsonSchemaFactory)}.
 *
 * The tests focus on the contract of the static builder factory methods:
 * <ul>
 *   <li>Each call to {@code builder()} must return a new, non‑null {@code Builder} instance.</li>
 *   <li>The {@code builder(JsonSchemaFactory)} overload must return a distinct {@code Builder}
 *       that can be used to create a factory with the same configuration as the blueprint.</li>
 *   <li>The {@code Builder} should be able to produce a {@link JsonSchemaFactory} instance via {@code build()}.</li>
 * </ul>
 *
 * No private members of {@link JsonSchemaFactory} or its {@code Builder} are accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
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
        JsonSchemaFactory original = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                b -> b.defaultMetaSchemaIri("https://json-schema.org/draft/2020-12/schema"));

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

        assertSame(originalMeta, copyMeta,
                "MetaSchema obtained from the copied factory should be the same instance as the original");
    }

    /**
     * Verify that a Builder can successfully build a {@link JsonSchemaFactory}
     * instance and that the resulting factory is functional (i.e., it can read a
     * simple JSON schema).
     */
    @Test
    void builderCanBuildFunctionalFactory() throws Exception {
        // Build a factory with the default meta‑schema IRI.
        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .defaultMetaSchemaIri("https://json-schema.org/draft/2020-12/schema")
                .build();

        assertNotNull(factory, "Factory built by Builder must not be null");

        // A minimal valid JSON schema (the empty schema) should be accepted.
        String emptySchema = "{}";
        JsonSchema schema = factory.getSchema(emptySchema);
        assertNotNull(schema, "Factory should be able to produce a JsonSchema from a minimal schema");
    }
}
```