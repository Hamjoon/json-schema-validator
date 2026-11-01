```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_14_Test {

    /** Shared factory used by all tests. */
    private static JsonSchemaFactory factory;

    /** ObjectMapper used to build {@link JsonNode} instances from JSON strings. */
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory for the 2020‑12 draft – this is the most common version used in the
        // library’s own test‑suite.  The factory is thread‑safe and does not require any special
        // configuration for the simple scenarios exercised here.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * Verifies that {@code getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)} returns a
     * non‑null {@link JsonSchema} instance when supplied with a valid location, a parsed schema
     * node and a default configuration.
     */
    @Test
    void getSchema_withValidLocationNodeAndConfig_returnsJsonSchema() throws Exception {
        // ----- given ---------------------------------------------------------
        // a minimal JSON schema (object type)
        String schemaJson = "{\"type\":\"object\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);

        // a concrete {@link SchemaLocation}
        AbsoluteIri absoluteIri = new AbsoluteIri("https://example.com/simple-schema.json");
        SchemaLocation location = new SchemaLocation(absoluteIri);

        // a default configuration – the builder is part of the public API
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ----- when ----------------------------------------------------------
        JsonSchema jsonSchema = factory.getSchema(location, schemaNode, config);

        // ----- then ----------------------------------------------------------
        assertNotNull(jsonSchema, "The factory should produce a non‑null JsonSchema instance");
        // The JsonSchema API does not expose the original SchemaLocation directly, but we can
        // at least verify that the underlying schema node is the one we supplied.
        assertEquals(schemaNode, jsonSchema.getSchemaNode(),
                "The JsonSchema should contain the same schema node that was passed to the factory");
    }

    /**
     * Ensures that invoking {@code getSchema} repeatedly with the same {@link SchemaLocation}
     * and configuration yields the same {@link JsonSchema} instance when schema caching is enabled
     * (the default behaviour of the factory).
     */
    @Test
    void getSchema_multipleCallsWithSameParameters_returnSameInstance() throws Exception {
        // ----- given ---------------------------------------------------------
        String schemaJson = "{\"type\":\"string\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        AbsoluteIri absoluteIri = new AbsoluteIri("https://example.com/string-schema.json");
        SchemaLocation location = new SchemaLocation(absoluteIri);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ----- when ----------------------------------------------------------
        JsonSchema first = factory.getSchema(location, schemaNode, config);
        JsonSchema second = factory.getSchema(location, schemaNode, config);

        // ----- then ----------------------------------------------------------
        assertSame(first, second, "When caching is enabled the factory should return the same JsonSchema instance");
    }
}
```