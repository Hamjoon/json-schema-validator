```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_14_Test {

    /** Factory used for all tests in this class. */
    private static JsonSchemaFactory factory;

    /** Simple JSON mapper used to create {@link JsonNode} instances. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it creates a fully‑initialised instance
        // with the default meta‑schema for the 2020‑12 draft.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state is required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verifies that {@code getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)}
     * returns a non‑null {@link JsonSchema} when supplied with a valid location,
     * a parsed schema node and a default configuration.
     */
    @Test
    void getSchema_withValidArguments_returnsJsonSchema() throws Exception {
        // ---- given -----------------------------------------------------------
        String schemaContent = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        JsonNode schemaNode = MAPPER.readTree(schemaContent);

        // AbsoluteIri and JsonNodePath are part of the public API; they have a
        // public constructor that accepts a string and a static constant for the
        // root JSON‑pointer respectively.
        AbsoluteIri absoluteIri = new AbsoluteIri("https://example.com/test-schema.json");
        SchemaLocation location = new SchemaLocation(absoluteIri); // fragment defaults to JSON_POINTER

        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ---- when ------------------------------------------------------------
        JsonSchema jsonSchema = factory.getSchema(location, schemaNode, config);

        // ---- then ------------------------------------------------------------
        assertNotNull(jsonSchema, "The factory should produce a non‑null JsonSchema instance");
        // The created JsonSchema should expose the original schema node (the public
        // API provides getSchemaNode() for this purpose).
        assertEquals(schemaNode, jsonSchema.getSchemaNode(),
                "The schema node stored inside JsonSchema must match the one supplied to the factory");
    }

    /**
     * Ensures that repeated calls with the same {@link SchemaLocation} and
     * {@link SchemaValidatorsConfig} return the *same* {@link JsonSchema}
     * instance – the factory caches schemas for performance.
     */
    @Test
    void getSchema_calledTwiceWithSameParameters_returnsSameInstance() throws Exception {
        // ---- given -----------------------------------------------------------
        String schemaContent = "{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}";
        JsonNode schemaNode = MAPPER.readTree(schemaContent);
        AbsoluteIri absoluteIri = new AbsoluteIri("https://example.com/array-schema.json");
        SchemaLocation location = new SchemaLocation(absoluteIri);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ---- when ------------------------------------------------------------
        JsonSchema first = factory.getSchema(location, schemaNode, config);
        JsonSchema second = factory.getSchema(location, schemaNode, config);

        // ---- then ------------------------------------------------------------
        assertNotNull(first);
        assertNotNull(second);
        assertSame(first, second,
                "Repeated calls with identical arguments should return the cached JsonSchema instance");
    }

    /**
     * Checks that providing a {@code null} {@link SchemaValidatorsConfig}
     * results in an {@link IllegalArgumentException}. The factory validates its
     * inputs before delegating to internal helpers.
     */
    @Test
    void getSchema_withNullConfig_throwsException() throws Exception {
        // ---- given -----------------------------------------------------------
        String schemaContent = "{\"type\":\"null\"}";
        JsonNode schemaNode = MAPPER.readTree(schemaContent);
        AbsoluteIri absoluteIri = new AbsoluteIri("https://example.com/null-schema.json");
        SchemaLocation location = new SchemaLocation(absoluteIri);

        // ---- when / then ----------------------------------------------------
        assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(location, schemaNode, null),
                "Passing a null SchemaValidatorsConfig should raise an IllegalArgumentException");
    }
}
```