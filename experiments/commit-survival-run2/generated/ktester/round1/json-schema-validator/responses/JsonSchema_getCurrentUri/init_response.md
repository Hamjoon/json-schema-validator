```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchema#getCurrentUri()}.
 *
 * The {@code JsonSchema} constructor is private, but the package‑private static
 * factory method {@code JsonSchema.from(...)} can be used because this test
 * resides in the same package.  All collaborators required by {@code ValidationContext}
 * are mocked – the behaviour of {@code JsonSchema} itself is exercised, not the
 * collaborators.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getCurrentUri_Test {

    // -------------------------------------------------------------------------
    // Shared mocks – created once for the whole test suite
    // -------------------------------------------------------------------------
    @Mock
    static URIFactory mockUriFactory;
    @Mock
    static URNFactory mockUrnFactory;
    @Mock
    static JsonMetaSchema mockMetaSchema;
    @Mock
    static JsonSchemaFactory mockJsonSchemaFactory;
    @Mock
    static SchemaValidatorsConfig mockConfig;

    private static ValidationContext validationContext;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // The ValidationContext constructor validates that none of the arguments are null.
        // Supplying the mocked collaborators is sufficient for our tests.
        validationContext = new ValidationContext(
                mockUriFactory,
                mockUrnFactory,
                mockMetaSchema,
                mockJsonSchemaFactory,
                mockConfig
        );
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock interactions before each test to keep them independent.
        reset(mockUriFactory, mockUrnFactory, mockMetaSchema, mockJsonSchemaFactory, mockConfig);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up per‑test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    // -------------------------------------------------------------------------
    // Test cases
    // -------------------------------------------------------------------------

    @Test
    void getCurrentUri_withoutId_returnsProvidedUri() throws Exception {
        // GIVEN
        URI expectedUri = new URI("http://example.com/schema.json");
        JsonNode emptySchemaNode = MAPPER.readTree("{}"); // no "$id" property

        // WHEN
        JsonSchema schema = JsonSchema.from(
                validationContext,
                "#",                // schemaPath – not relevant for this test
                expectedUri,
                emptySchemaNode,
                null,               // no parent schema
                false               // suppressSubSchemaRetrieval
        );

        // THEN
        assertNotNull(schema, "JsonSchema instance should be created");
        assertEquals(expectedUri, schema.getCurrentUri(),
                "When the schema node does not contain an $id, getCurrentUri() must return the URI supplied to the constructor");
    }

    @Test
    void getCurrentUri_withId_combinesBaseAndId() throws Exception {
        // GIVEN
        URI baseUri = new URI("http://example.com/base/");
        // Schema node contains an $id that is relative to the base URI
        JsonNode schemaWithId = MAPPER.readTree("{\"$id\": \"sub/schema.json\"}");

        // WHEN
        JsonSchema schema = JsonSchema.from(
                validationContext,
                "#",
                baseUri,
                schemaWithId,
                null,
                false
        );

        // THEN
        assertNotNull(schema, "JsonSchema instance should be created");
        URI actual = schema.getCurrentUri();
        URI expected = baseUri.resolve("sub/schema.json"); // how combineCurrentUriWithIds should behave
        assertEquals(expected, actual,
                "When the schema node contains an $id, getCurrentUri() must reflect the combined URI");
    }
}
```