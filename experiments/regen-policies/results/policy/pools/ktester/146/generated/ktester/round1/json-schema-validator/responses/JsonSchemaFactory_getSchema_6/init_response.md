```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(URI)}.
 *
 * <p>The test creates a {@link JsonSchemaFactory} via the public {@code getInstance}
 * builder API, registers a simple JSON‑Schema under a known URI and verifies that
 * {@code getSchema(URI)} returns a non‑null {@link JsonSchema} instance that can be
 * used for validation.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_6_Test {

    /** Factory used in all test cases. */
    private static JsonSchemaFactory factory;

    /** Simple schema used for the tests – a trivial object schema. */
    private static final String SIMPLE_SCHEMA = "{\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"$id\": \"https://example.com/simple-schema\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"name\": { \"type\": \"string\" }\n"
            + "  },\n"
            + "  \"required\": [\"name\"]\n"
            + "}";

    /** The URI under which the schema is registered. */
    private static final URI SIMPLE_SCHEMA_URI = URI.create("https://example.com/simple-schema");

    @BeforeAll
    static void setupBeforeAll() {
        /*
         * Build a factory that knows about the simple schema.  The builder API
         * allows us to plug a custom map of URI → schema content without touching
         * any private members of JsonSchemaFactory.
         */
        factory = JsonSchemaFactory.getInstance(
                VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(
                                Collections.singletonMap(
                                        SIMPLE_SCHEMA_URI.toString(),
                                        SIMPLE_SCHEMA))));
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
        // Nothing to release; the factory does not hold external resources.
        factory = null;
    }

    /**
     * Verifies that {@code getSchema(URI)} returns a {@link JsonSchema} instance
     * for a URI that has been registered via the {@link SchemaLoaders.Builder}.
     */
    @Test
    void shouldReturnJsonSchemaWhenUriIsRegistered() {
        // given – the URI is already registered in the factory (see @BeforeAll)

        // when
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA_URI);

        // then
        assertNotNull(schema, "Factory should return a non‑null JsonSchema for a known URI");
        // The JsonSchema API provides {@code getSchemaNode()} which returns the parsed
        // JsonNode representation of the schema.  We can safely call it to ensure the
        // underlying schema was correctly loaded.
        assertNotNull(schema.getSchemaNode(), "The loaded JsonSchema should contain a parsed schema node");
    }

    /**
     * Verifies that requesting a schema for an unknown URI results in a
     * {@link JsonSchemaException} (the concrete exception type used by the library
     * when a schema cannot be resolved).  The exact exception type may differ
     * between versions, so we only assert that an exception is thrown.
     */
    @Test
    void shouldThrowWhenUriIsNotRegistered() {
        // given
        URI unknownUri = URI.create("https://example.com/unknown-schema");

        // when / then
        assertThrows(RuntimeException.class, () -> factory.getSchema(unknownUri),
                "Requesting an unknown schema URI should result in an exception");
    }
}
```