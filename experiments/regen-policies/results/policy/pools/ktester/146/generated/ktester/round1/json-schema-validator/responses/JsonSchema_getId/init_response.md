```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#getId()}.
 *
 * <p>The tests cover three scenarios that are exercised by the
 * {@code getId()} implementation in {@link JsonSchema}:
 *
 * <ul>
 *   <li>a schema that defines a plain {@code $id} – the method should return that value;</li>
 *   <li>a schema that defines an {@code $id} containing a fragment – the method should return {@code null}
 *       because the fragment makes the identifier an anchor rather than a document identifier;</li>
 *   <li>a schema without an {@code $id} – the method should fall back to the absolute IRI of the
 *       {@link SchemaLocation} (the factory supplies a generated URI, which we verify is non‑null
 *       and non‑empty).</li>
 * </ul>
 *
 * <p>The {@link JsonSchemaFactory} is used to create {@link JsonSchema} instances via its public API,
 * avoiding any direct access to private constructors or fields.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getId_Test {

    /** Shared factory for all tests – created once per test class. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft that supports $id (draft‑07 or later).
        factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    /**
     * When the schema defines a plain {@code $id}, {@code getId()} must return that exact value.
     */
    @Test
    void getId_returnsPlainId() {
        // language=JSON
        String schema = """
                {
                  "$schema": "https://json-schema.org/draft/2019-09/schema",
                  "$id": "http://example.org/my-schema.json",
                  "type": "object"
                }
                """;

        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema instance");
        assertEquals("http://example.org/my-schema.json", jsonSchema.getId(),
                "getId() should return the plain $id value");
    }

    /**
     * When the {@code $id} contains a fragment, the identifier is considered an anchor and
     * {@code getId()} must return {@code null}.
     */
    @Test
    void getId_returnsNullWhenIdHasFragment() {
        // language=JSON
        String schema = """
                {
                  "$schema": "https://json-schema.org/draft/2019-09/schema",
                  "$id": "http://example.org/my-schema.json#myFragment",
                  "type": "object"
                }
                """;

        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema instance");
        assertNull(jsonSchema.getId(),
                "getId() must be null when $id contains a fragment (anchor)");
    }

    /**
     * When a schema does not declare {@code $id}, the implementation falls back to the
     * absolute IRI of the {@link SchemaLocation}. The factory supplies a generated URI
     * (e.g. {@code urn:jsonschema:...}); we verify that {@code getId()} is non‑null and
     * non‑empty in this case.
     */
    @Test
    void getId_fallsBackToSchemaLocationWhenNoId() {
        // language=JSON
        String schema = """
                {
                  "$schema": "https://json-schema.org/draft/2019-09/schema",
                  "type": "object"
                }
                """;

        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema instance");

        String id = jsonSchema.getId();
        assertNotNull(id, "When $id is absent, getId() should fall back to a generated IRI");
        assertFalse(id.isEmpty(), "The fallback IRI should not be an empty string");
    }
}
```