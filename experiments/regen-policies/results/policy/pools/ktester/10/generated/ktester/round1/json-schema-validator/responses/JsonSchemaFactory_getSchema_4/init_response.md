```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(SchemaLocation)}.
 *
 * The test creates a real {@link JsonSchemaFactory} instance using its
 * package‑private constructor and then spies on it so that the protected
 * {@code getMappedSchema} method can be stubbed. This isolates the test from
 * any external resources (files, network, etc.) while still exercising the
 * public {@code getSchema(SchemaLocation)} workflow.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_4_Test {

    /** The factory under test – a spy so that we can stub protected methods. */
    private JsonSchemaFactory spyFactory;

    /** A dummy {@link JsonSchema} that will be returned by the stubbed method. */
    private JsonSchema stubbedJsonSchema;

    /** Default meta‑schema URI used for construction of the factory. */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta-schema";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // ---- 1. Mock the collaborators required by the constructor -----------------
        ObjectMapper jsonMapper = mock(ObjectMapper.class);
        ObjectMapper yamlMapper = mock(ObjectMapper.class);
        JsonMetaSchema metaSchema = mock(JsonMetaSchema.class);

        // The factory validates that the map contains the default meta‑schema URI.
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(DEFAULT_META_SCHEMA_URI, metaSchema);

        // ---- 2. Create a real factory instance --------------------------------------
        JsonSchemaFactory realFactory = new JsonSchemaFactory(
                jsonMapper,
                yamlMapper,
                DEFAULT_META_SCHEMA_URI,
                /* schemaLoadersBuilder = */ null,
                /* schemaMappersBuilder = */ null,
                metaSchemas,
                /* enableUriSchemaCache = */ false);

        // ---- 3. Spy on the instance so we can stub the protected method ------------
        spyFactory = Mockito.spy(realFactory);

        // ---- 4. Prepare the stubbed JsonSchema that the protected method will return
        stubbedJsonSchema = mock(JsonSchema.class);
        doReturn(stubbedJsonSchema)
                .when(spyFactory)
                .getMappedSchema(any(SchemaLocation.class), any());

        // ---- 5. Verify that the factory was constructed correctly (sanity check) ---
        assertNotNull(spyFactory);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy to avoid interaction leakage between tests.
        Mockito.reset(spyFactory);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verifies that {@code JsonSchemaFactory#getSchema(SchemaLocation)} delegates to
     * the protected {@code getMappedSchema} method and returns the value produced
     * by that method.
     */
    @Test
    void testGetSchemaReturnsMappedSchema() {
        // ----- given --------------------------------------------------------------
        // Build a minimal SchemaLocation.  AbsoluteIri has a public constructor that
        // accepts a String representation of the IRI.
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/schema");
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri); // fragment defaults to JSON_POINTER

        // ----- when ----------------------------------------------------------------
        JsonSchema result = spyFactory.getSchema(schemaLocation);

        // ----- then ----------------------------------------------------------------
        assertSame(stubbedJsonSchema, result,
                "The schema returned by getSchema should be the one supplied by the stubbed getMappedSchema.");

        // Verify that the protected method was invoked exactly once with the same
        // location and any non‑null configuration object.
        verify(spyFactory, times(1))
                .getMappedSchema(eq(schemaLocation), any());
    }
}
```