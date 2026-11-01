```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.Collections;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(URI)}.
 *
 * The tests verify that:
 * <ul>
 *   <li>a valid {@link URI} resolves to a non‑null {@link JsonSchema}</li>
 *   <li>the same {@link URI} returns the same {@link JsonSchema} instance (cache behaviour)</li>
 *   <li>a {@code null} argument results in {@link NullPointerException}</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_6_Test {

    /** Factory under test. */
    private JsonSchemaFactory factory;

    /** URI of the test schema. */
    private URI schemaUri;

    /** Simple JSON schema content. */
    private static final String SCHEMA_JSON = "{\n"
            + "  \"$id\": \"http://example.com/schema\",\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"type\": \"object\"\n"
            + "}";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the URI that will be used in the tests.
        schemaUri = URI.create("http://example.com/schema");

        /*
         * Build a JsonSchemaFactory that knows about the above schema.
         * The builder API allows us to inject a custom SchemaLoaders instance
         * that maps the URI string to the raw JSON schema text.
         */
        factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(
                                Collections.singletonMap(schemaUri.toString(), SCHEMA_JSON))));
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getSchema_withValidUri_returnsNonNullSchema() {
        // given
        // (factory and schemaUri are prepared in @BeforeEach)

        // when
        JsonSchema schema = factory.getSchema(schemaUri);

        // then
        assertNotNull(schema, "Factory should return a non‑null JsonSchema for a known URI");
        // The schema node should contain the "type" property we defined.
        assertEquals("object",
                schema.getSchemaNode().get("type").asText(),
                "The loaded schema should contain the expected 'type' definition");
    }

    @Test
    void getSchema_sameUri_returnsSameInstance() {
        // given
        // (factory and schemaUri are prepared in @BeforeEach)

        // when
        JsonSchema first = factory.getSchema(schemaUri);
        JsonSchema second = factory.getSchema(schemaUri);

        // then
        assertSame(first, second, "Repeated calls with the same URI should return the cached JsonSchema instance");
    }

    @Test
    void getSchema_withNullUri_throwsNullPointerException() {
        // given
        URI nullUri = null;

        // when / then
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(nullUri),
                "Calling getSchema(null) must throw NullPointerException");
    }
}
```