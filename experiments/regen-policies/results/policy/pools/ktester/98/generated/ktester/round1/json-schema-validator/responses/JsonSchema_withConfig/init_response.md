```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#withConfig(SchemaValidatorsConfig)}.
 *
 * The tests cover the two logical paths inside the method:
 * <ul>
 *   <li>when the supplied {@code SchemaValidatorsConfig} is equal to the
 *       current configuration – the same {@link JsonSchema} instance is
 *       returned.</li>
 *   <li>when the supplied configuration differs – a new {@link JsonSchema}
 *       instance is created with the new configuration.</li>
 * </ul>
 *
 * The {@link JsonSchema} objects are created via the public
 * {@link JsonSchemaFactory} API; no reflection or private‑member access is
 * required.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_withConfig_Test {

    /** Simple JSON schema used for all tests. */
    private static final String SIMPLE_SCHEMA = "{\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"type\": \"object\"\n"
            + "}";

    /** Factory that creates {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory for the 2020‑12 draft – the concrete draft does not
        // matter for the withConfig logic, only that the factory can build a schema.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No static resources to release.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise per test.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per test.
    }

    /**
     * Verify that {@code withConfig} returns the same instance when the supplied
     * configuration is equal to the current one.
     */
    @Test
    void withConfig_sameConfig_returnsSameInstance() {
        // Build a default configuration (all options are at their defaults).
        SchemaValidatorsConfig defaultConfig = SchemaValidatorsConfig.builder().build();

        // Obtain a JsonSchema instance using that configuration.
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA, defaultConfig);

        // Call withConfig with an *equal* configuration.
        JsonSchema result = schema.withConfig(defaultConfig);

        // The method should return the original instance.
        assertSame(schema, result, "withConfig should return the same instance when the config is unchanged");
    }

    /**
     * Verify that {@code withConfig} creates a new {@link JsonSchema} when the
     * supplied configuration differs from the current one.
     *
     * The test changes a single flag (discriminatorKeywordEnabled) to force the
     * creation of a new validation context.
     */
    @Test
    void withConfig_differentConfig_returnsNewInstance() {
        // Original (default) configuration.
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().build();

        // A different configuration – enable the discriminator keyword.
        SchemaValidatorsConfig newConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .build();

        // Obtain a schema with the original configuration.
        JsonSchema originalSchema = factory.getSchema(SIMPLE_SCHEMA, originalConfig);

        // Invoke withConfig using a different configuration.
        JsonSchema newSchema = originalSchema.withConfig(newConfig);

        // The returned schema must be a *different* object.
        assertNotSame(originalSchema, newSchema,
                "withConfig should create a new JsonSchema instance when the config differs");

        // The new schema's validation context must hold the new configuration.
        assertEquals(newConfig,
                newSchema.getValidationContext().getConfig(),
                "The new JsonSchema should contain the supplied configuration");
    }
}
```