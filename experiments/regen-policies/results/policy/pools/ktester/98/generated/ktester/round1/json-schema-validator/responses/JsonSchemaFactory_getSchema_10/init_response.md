```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(SchemaLocation, SchemaValidatorsConfig)}.
 *
 * The tests focus on the public contract of the method:
 * <ul>
 *   <li>It should return a non‑null {@link JsonSchema} instance for a valid schema location.</li>
 *   <li>The returned schema should be usable for validation (basic sanity check).</li>
 *   <li>The method must not throw unexpected exceptions when invoked with a default configuration.</li>
 * </ul>
 *
 * The factory is created via {@link JsonSchemaFactory#getInstance(VersionFlag)} which
 * provides a fully‑initialised instance with all required internal dependencies.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_10_Test {

    /** The factory under test – a real instance (no mocking of internal collaborators). */
    private static JsonSchemaFactory factory;

    /** A simple JSON schema used in the tests. */
    private static final String SIMPLE_SCHEMA = """
            {
              "$id": "urn:test:simple",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "string"
            }
            """;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the latest draft (2020‑12) – the factory will internally create all needed
        // JsonNodeReader, SchemaLoaders, SchemaMappers, etc.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No explicit resources to release; the factory does not hold external handles.
        factory = null;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise per‑test at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    /**
     * Verify that {@code getSchema(SchemaLocation, SchemaValidatorsConfig)} returns a
     * non‑null {@link JsonSchema} for a valid schema location and that the schema can
     * successfully validate a conforming instance.
     */
    @Test
    void getSchema_returnsValidJsonSchema() {
        // -------------------------------------------------------
        // given: a schema location pointing to an in‑memory schema and a default config
        // -------------------------------------------------------
        // Register the simple schema under a known URI using the factory's customizer.
        // This mimics the way callers would make a schema available to the loader.
        JsonSchemaFactory customFactory = JsonSchemaFactory.getInstance(
                VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        loaders -> loaders.schemas(
                                java.util.Collections.singletonMap(
                                        "urn:test:simple", SIMPLE_SCHEMA))));

        SchemaLocation location = SchemaLocation.of(URI.create("urn:test:simple"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // -------------------------------------------------------
        // when: the focal method is invoked
        // -------------------------------------------------------
        JsonSchema schema = customFactory.getSchema(location, config);

        // -------------------------------------------------------
        // then: a non‑null schema is returned and it validates a correct instance
        // -------------------------------------------------------
        assertNotNull(schema, "The factory should return a non‑null JsonSchema instance");

        // A simple valid JSON instance that matches the schema ("type": "string")
        JsonNode validNode = factory.getJsonMapper().readTree("\"hello\"");

        // Validation should succeed without errors
        assertTrue(schema.validate(validNode).isEmpty(),
                "A valid instance should produce no validation errors");
    }

    /**
     * Ensure that calling the method with {@code null} arguments results in the expected
     * {@link NullPointerException}. The factory itself validates its public API contracts.
     */
    @Test
    void getSchema_nullArguments_throwNPE() {
        SchemaLocation location = SchemaLocation.of(URI.create("urn:test:simple"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // Null location
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(null, config),
                "Passing a null SchemaLocation should throw NPE");

        // Null config
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(location, null),
                "Passing a null SchemaValidatorsConfig should throw NPE");
    }
}
```