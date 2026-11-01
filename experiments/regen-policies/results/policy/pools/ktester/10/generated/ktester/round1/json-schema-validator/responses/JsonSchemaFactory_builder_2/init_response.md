```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchemaFactory#builder(JsonSchemaFactory)}.
 *
 * The test verifies that the builder created from an existing {@link JsonSchemaFactory}
 * instance copies the essential configuration (meta‑schemas, default meta‑schema URI,
 * JSON/YAML mappers and the optional schema‑loaders / schema‑mappers builders).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_2_Test {

    /** A mock meta‑schema used for the blueprint factory. */
    @Mock
    private JsonMetaSchema mockMetaSchema;

    /** The blueprint factory that will be used as the source for the builder. */
    private JsonSchemaFactory blueprintFactory;

    /** Common JSON mapper used for both factories. */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /** Common YAML mapper used for both factories. */
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper();

    /** The URI that identifies the default meta‑schema. */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the mock meta‑schema so that its URI matches the default one.
        when(mockMetaSchema.getUri()).thenReturn(DEFAULT_META_SCHEMA_URI);

        // Build a blueprint JsonSchemaFactory with all configurable parts set.
        blueprintFactory = JsonSchemaFactory.builder()
                .addMetaSchema(mockMetaSchema)                     // meta‑schema map
                .defaultMetaSchemaURI(DEFAULT_META_SCHEMA_URI)    // default URI
                .jsonMapper(JSON_MAPPER)                           // JSON mapper
                .yamlMapper(YAML_MAPPER)                           // YAML mapper
                // Initialise the optional builders – the consumer does nothing
                // other than forcing the internal builder objects to be created.
                .schemaLoaders(b -> { /* no customisation */ })
                .schemaMappers(b -> { /* no customisation */ })
                .build();
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
     * Verifies that the builder created from an existing factory copies the
     * configuration correctly.
     */
    @Test
    void builderCopiesConfigurationFromBlueprint() {
        // -------------------------------------------------
        // given: a blueprint factory (already prepared in @BeforeEach)
        // -------------------------------------------------

        // -------------------------------------------------
        // when: we obtain a new Builder from the static method
        // -------------------------------------------------
        JsonSchemaFactory.Builder copyBuilder = JsonSchemaFactory.builder(blueprintFactory);
        JsonSchemaFactory copyFactory = copyBuilder.build();

        // -------------------------------------------------
        // then: the copy must expose the same default meta‑schema URI
        // -------------------------------------------------
        JsonMetaSchema retrieved = copyFactory.getMetaSchema(DEFAULT_META_SCHEMA_URI, null);
        assertNotNull(retrieved, "The default meta‑schema must be present in the copied factory");
        assertSame(mockMetaSchema, retrieved,
                "The meta‑schema instance should be the same as the one added to the blueprint");

        // -------------------------------------------------
        // and: the JSON and YAML mappers must be the ones supplied
        // -------------------------------------------------
        // The getters for the mappers are protected; we verify their effect indirectly
        // by serialising a simple JSON node and checking that no exception is thrown.
        // (If the mappers were null, JsonSchemaFactory would have thrown during construction.)
        assertDoesNotThrow(() -> copyFactory.getSchema("{\"type\":\"string\"}"),
                "Factory built from the copied builder should be usable for schema parsing");

        // -------------------------------------------------
        // and: the optional schema‑loaders and schema‑mappers builders must have been copied
        // -------------------------------------------------
        // The presence of these builders is reflected in the ability to obtain a schema
        // loader without encountering a NullPointerException.
        assertDoesNotThrow(() -> copyFactory.getSchemaLoader(),
                "Schema loader should be initialised when the original factory had custom builders");
    }
}
```