```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag, Consumer)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getInstance_2_Test {

    /** A mock {@link JsonMetaSchema} used in the customizer test. */
    @Mock
    JsonMetaSchema customMetaSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock before each test (MockitoExtension does this automatically,
        // but we keep the method for symmetry with the template).
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
     * Verify that {@code getInstance} works when the customizer is {@code null}.
     * The factory should contain the default meta‑schema associated with the
     * supplied {@link VersionFlag}.
     */
    @Test
    void getInstance_withoutCustomizer_returnsFactoryWithDefaultMetaSchema() {
        // Use a well‑known version flag.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, null);

        // The default meta‑schema IRI for Draft‑2020‑12 is defined by the spec.
        String expectedIri = "https://json-schema.org/draft/2020-12/schema";

        // The factory should be able to retrieve the meta‑schema for that IRI.
        JsonMetaSchema meta = factory.getMetaSchema(expectedIri, SchemaValidatorsConfig.builder().build());

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
        // Arrange – configure the mock meta‑schema.
        String customIri = "urn:custom:meta:schema";
        when(customMetaSchema.getIri()).thenReturn(customIri);

        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder
                .defaultMetaSchemaIri(customIri)
                .metaSchema(customMetaSchema);

        // Act – create the factory with the customizer.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, customizer);

        // Assert – the factory should return the exact mock instance for the custom IRI.
        JsonMetaSchema retrieved = factory.getMetaSchema(customIri, SchemaValidatorsConfig.builder().build());
        assertNotNull(retrieved, "Factory should contain the custom meta‑schema");
        assertSame(customMetaSchema, retrieved,
                "Factory should return the same JsonMetaSchema instance that was supplied via the customizer");
    }
}
```