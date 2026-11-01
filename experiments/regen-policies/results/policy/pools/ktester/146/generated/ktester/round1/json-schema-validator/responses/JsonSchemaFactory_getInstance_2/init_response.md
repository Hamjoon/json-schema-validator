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

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed
    }

    /**
     * Verify that calling {@code getInstance} with a {@code null} customizer returns a
     * factory that contains the default meta‑schema for the supplied version flag.
     */
    @Test
    void getInstance_WithNullCustomizer_ReturnsFactoryWithDefaultMetaSchema() {
        // Act
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, null);

        // The default meta‑schema IRI for V202012 is defined inside the library.
        // We obtain it via the version enum to avoid hard‑coding the string.
        JsonSchemaVersion version = JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012);
        String defaultIri = version.getInstance().getIri();

        // Assert
        JsonMetaSchema meta = factory.getMetaSchema(defaultIri, SchemaValidatorsConfig.builder().build());
        assertNotNull(meta, "Factory should contain the default meta‑schema for the version flag");
        assertEquals(defaultIri, meta.getIri(), "Returned meta‑schema IRI should match the default");
    }

    /**
     * Verify that a customizer can replace the default meta‑schema IRI and supply a
     * custom {@link JsonMetaSchema}. The factory should then return the custom
     * meta‑schema when queried with the new IRI.
     */
    @Test
    void getInstance_WithCustomizer_ReplacesDefaultMetaSchema(@Mock JsonMetaSchema customMeta) {
        // Arrange: stub the IRI of the custom meta‑schema
        String customIri = "urn:custom:meta:schema";
        when(customMeta.getIri()).thenReturn(customIri);

        Consumer<JsonSchemaFactory.Builder> customizer = builder -> builder
                .defaultMetaSchemaIri(customIri)   // set the new default IRI
                .metaSchema(customMeta);           // register the custom meta‑schema

        // Act
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, customizer);

        // Assert: the factory should return the mocked meta‑schema for the custom IRI
        JsonMetaSchema retrieved = factory.getMetaSchema(customIri, SchemaValidatorsConfig.builder().build());
        assertSame(customMeta, retrieved, "Factory should return the custom meta‑schema supplied via the customizer");
    }
}
```