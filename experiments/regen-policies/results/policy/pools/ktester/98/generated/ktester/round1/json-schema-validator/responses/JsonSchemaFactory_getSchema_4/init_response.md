```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_4_Test {

    /** Factory instance used by the tests */
    private JsonSchemaFactory factory;

    /** Simple JSON schema used for the tests */
    private static final String SIMPLE_SCHEMA = """
            {
              "$id": "https://example.com/simple-schema",
              "type": "string"
            }
            """;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Build a JsonSchemaFactory that knows how to load the SIMPLE_SCHEMA
         * from the URI https://example.com/simple-schema.
         * The factory is created via the public static getInstance method,
         * which internally builds the required JsonNodeReader, SchemaLoaders,
         * SchemaMappers, etc.  No mocking of internal collaborators is needed.
         */
        factory = JsonSchemaFactory.getInstance(
                VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(
                                Collections.singletonMap(
                                        "https://example.com/simple-schema",
                                        SIMPLE_SCHEMA
                                )
                        )
                )
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this factory.
        factory = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verifies that {@code JsonSchemaFactory#getSchema(SchemaLocation)} returns a
     * non‑null {@link JsonSchema} instance for a known schema location and that
     * repeated calls with the same location return the same cached instance.
     */
    @Test
    void getSchema_givenValidSchemaLocation_returnsJsonSchema_andCachesResult() {
        // --------------------------------------------------------------------
        // given: a SchemaLocation that points to the simple schema defined above
        // --------------------------------------------------------------------
        AbsoluteIri absoluteIri = new AbsoluteIri("https://example.com/simple-schema");
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri);

        // --------------------------------------------------------------------
        // when: the factory is asked for a JsonSchema for that location
        // --------------------------------------------------------------------
        JsonSchema firstSchema = factory.getSchema(schemaLocation);
        JsonSchema secondSchema = factory.getSchema(schemaLocation);

        // --------------------------------------------------------------------
        // then: a JsonSchema is returned and the two calls yield the same instance
        // --------------------------------------------------------------------
        assertNotNull(firstSchema, "The factory should return a non‑null JsonSchema");
        assertNotNull(secondSchema, "A second call should also return a non‑null JsonSchema");
        assertSame(firstSchema, secondSchema,
                "JsonSchemaFactory should cache schemas; repeated calls must return the same instance");
    }
}
```