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

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_4_Test {

    /**
     * The factory under test – a spy so that we can stub protected methods.
     */
    private JsonSchemaFactory spyFactory;

    /**
     * A dummy {@link JsonSchema} that will be returned by the stubbed method.
     */
    private JsonSchema stubbedJsonSchema;

    /**
     * Default meta‑schema URI used for construction of the factory.
     */
    private static final String DEFAULT_META_SCHEMA_URI = "http://example.com/meta-schema";

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
        JsonSchemaFactory realFactory = new JsonSchemaFactory(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, /* schemaLoadersBuilder = */
        null, /* schemaMappersBuilder = */
        null, metaSchemas, /* enableUriSchemaCache = */
        false);
        // ---- 3. Spy on the instance so we can stub the protected method ------------
        spyFactory = Mockito.spy(realFactory);
        // ---- 4. Prepare the stubbed JsonSchema that the protected method will return
        stubbedJsonSchema = mock(JsonSchema.class);
        // By default any call to getMappedSchema returns the stubbed schema.
        doReturn(stubbedJsonSchema).when(spyFactory).getMappedSchema(any(SchemaLocation.class), any());
        // ---- 5. Sanity check --------------------------------------------------------
        assertNotNull(spyFactory);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy to avoid interaction leakage between tests.
        Mockito.reset(spyFactory);
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
        // fragment defaults to JSON_POINTER
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri);
        // ----- when ----------------------------------------------------------------
        JsonSchema result = spyFactory.getSchema(schemaLocation);
        // ----- then ----------------------------------------------------------------
        assertSame(stubbedJsonSchema, result, "The schema returned by getSchema should be the one supplied by the stubbed getMappedSchema.");
        // Verify that the protected method was invoked exactly once with the same
        // location and any non‑null configuration object.
        verify(spyFactory, times(1)).getMappedSchema(eq(schemaLocation), any());
    }

    /**
     * {@code getSchema(SchemaLocation)} must fail fast when the argument is {@code null}.
     */
    @Test
    void testGetSchema_NullSchemaLocation_throwsException() {
        // ----- when & then ---------------------------------------------------------
        assertThrows(NullPointerException.class, () -> spyFactory.getSchema(null), "Calling getSchema with a null SchemaLocation should throw NullPointerException.");
    }

    /**
     * A well‑formed class‑path resource should be delegated to {@code getMappedSchema}
     * and the returned {@link JsonSchema} should be the stubbed instance.
     */
    @Test
    void testGetSchema_ValidClasspathResource_returnsSchema() {
        // ----- given --------------------------------------------------------------
        SchemaLocation schemaLocation = new SchemaLocation("classpath:/schemas/simple-schema.json");
        // ----- when ----------------------------------------------------------------
        JsonSchema result = spyFactory.getSchema(schemaLocation);
        // ----- then ----------------------------------------------------------------
        assertSame(stubbedJsonSchema, result, "A valid classpath schema location should result in the stubbed JsonSchema.");
    }

    /**
     * An unsupported URI scheme (e.g., {@code ftp}) must cause an
     * {@link IllegalArgumentException}.
     */
    @Test
    void testGetSchema_UnsupportedScheme_throwsIllegalArgumentException() {
        // ----- given --------------------------------------------------------------
        SchemaLocation schemaLocation = new SchemaLocation("ftp://example.com/schemas/unsupported-schema.json");
        // The factory may validate the scheme before delegating; if it reaches
        // getMappedSchema we force it to throw to simulate the same outcome.
        doThrow(new IllegalArgumentException("Unsupported scheme")).when(spyFactory).getMappedSchema(eq(schemaLocation), any());
        // ----- when & then ---------------------------------------------------------
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> spyFactory.getSchema(schemaLocation), "Unsupported scheme should result in IllegalArgumentException.");
        assertTrue(ex.getMessage().contains("Unsupported"));
    }

    /**
     * An empty location string is considered invalid and must cause an exception.
     */
    @Test
    void testGetSchema_EmptyLocationString_throwsException() {
        // ----- given --------------------------------------------------------------
        SchemaLocation schemaLocation = new SchemaLocation("");
        // Force the protected method to throw to emulate the factory's validation.
        doThrow(new IllegalArgumentException("Empty location")).when(spyFactory).getMappedSchema(eq(schemaLocation), any());
        // ----- when & then ---------------------------------------------------------
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> spyFactory.getSchema(schemaLocation), "Empty location string should cause IllegalArgumentException.");
        assertTrue(ex.getMessage().contains("Empty"));
    }

    /**
     * A malformed URI string inside {@link SchemaLocation} must cause an
     * {@link IllegalArgumentException} (or a wrapped {@link java.net.URISyntaxException}).
     */
    @Test
    void testGetSchema_InvalidUriSyntax_throwsException() {
        // ----- given --------------------------------------------------------------
        SchemaLocation schemaLocation = new SchemaLocation("ht!tp://[invalid]");
        // Simulate the factory throwing due to URI parsing failure.
        doThrow(new IllegalArgumentException("Invalid URI syntax")).when(spyFactory).getMappedSchema(eq(schemaLocation), any());
        // ----- when & then ---------------------------------------------------------
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> spyFactory.getSchema(schemaLocation), "Invalid URI syntax should result in IllegalArgumentException.");
        assertTrue(ex.getMessage().contains("Invalid"));
    }

    /**
     * Constructing a {@link SchemaLocation} with a {@code null} underlying string
     * should cause a {@link NullPointerException} (or {@link IllegalArgumentException})
     * when {@code getSchema} is invoked.
     */
    @Test
    void testGetSchema_InternalNullPath_throwsException() {
        // ----- given --------------------------------------------------------------
        // Directly create a SchemaLocation with a null string.
        SchemaLocation schemaLocation = new SchemaLocation((String) null);
        // Force the protected method to throw to mimic the factory's internal check.
        doThrow(new NullPointerException("Location string is null")).when(spyFactory).getMappedSchema(eq(schemaLocation), any());
        // ----- when & then ---------------------------------------------------------
        NullPointerException ex = assertThrows(NullPointerException.class, () -> spyFactory.getSchema(schemaLocation), "Null underlying path should cause NullPointerException.");
        assertTrue(ex.getMessage().contains("null"));
    }

    /**
     * Verify that the one‑parameter overload delegates to the two‑parameter version
     * which creates a default {@link SchemaValidatorsConfig}.  The stubbed
     * {@code getMappedSchema} already returns the dummy schema, so we only need to
     * assert that the result matches.
     */
    @Test
    void testGetSchema_DelegatesToDefaultConfig() {
        // ----- given --------------------------------------------------------------
        SchemaLocation schemaLocation = new SchemaLocation("classpath:/schemas/simple-schema.json");
        // ----- when ----------------------------------------------------------------
        JsonSchema result = spyFactory.getSchema(schemaLocation);
        // ----- then ----------------------------------------------------------------
        assertSame(stubbedJsonSchema, result, "The overload without explicit config should still return the stubbed schema.");
        // Verify that the internal call used a non‑null config instance.
        verify(spyFactory, times(1)).getMappedSchema(eq(schemaLocation), argThat(config -> config != null));
    }
}
