```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#withConfig(SchemaValidatorsConfig)}.
 *
 * The tests cover two main scenarios:
 * <ul>
 *   <li>When the supplied {@link SchemaValidatorsConfig} is equal to the
 *       configuration already used by the {@link JsonSchema} instance – the
 *       method should return the same {@link JsonSchema} object.</li>
 *   <li>When the supplied configuration disables the {@code discriminator}
 *       keyword while the meta‑schema contains this keyword – the method must
 *       create a new configuration that enables the keyword (and also the
 *       {@code nullable} keyword) and return a new {@link JsonSchema}
 *       instance.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_withConfig_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that {@code withConfig} returns the same {@link JsonSchema}
     * instance when the supplied configuration is equal to the one already
     * used by the schema.
     */
    @Test
    void withConfigReturnsSameInstanceWhenConfigUnchanged() {
        // a simple schema without a discriminator keyword
        String schemaJson = "{"
                + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\": \"object\""
                + "}";

        // configuration with discriminator enabled (the default for the meta‑schema)
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .build();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson, config);

        // invoke withConfig using an *equal* configuration (different instance)
        SchemaValidatorsConfig equalConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .build();

        JsonSchema result = schema.withConfig(equalConfig);

        // The method should return the original instance.
        assertSame(schema, result, "Expected the same JsonSchema instance when config is unchanged");
    }

    /**
     * Verify that {@code withConfig} creates a new {@link JsonSchema}
     * instance when the supplied configuration disables the {@code discriminator}
     * keyword while the meta‑schema contains this keyword. The method must
     * automatically enable the discriminator (and nullable) keyword.
     */
    @Test
    void withConfigEnablesDiscriminatorKeywordWhenDisabled() {
        // Schema that contains a discriminator keyword (supported by draft‑2020‑12)
        String schemaJson = "{"
                + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\": \"object\","
                + "\"properties\": {"
                + "    \"type\": {\"type\": \"string\"}"
                + "},"
                + "\"discriminator\": {"
                + "    \"propertyName\": \"type\","
                + "    \"mapping\": {"
                + "        \"a\": \"#/definitions/A\""
                + "    }"
                + "},"
                + "\"definitions\": {"
                + "    \"A\": {"
                + "        \"type\": \"object\","
                + "        \"properties\": {"
                + "            \"a\": {\"type\": \"string\"}"
                + "        }"
                + "    }"
                + "}"
                + "}";

        // Start with a configuration that explicitly disables the discriminator keyword
        SchemaValidatorsConfig disabledConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(false) // also disabled to verify it gets enabled automatically
                .build();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, disabledConfig);

        // Call withConfig using the same (disabled) configuration.
        // According to the implementation, the method should replace it with a new
        // configuration that enables both discriminator and nullable keywords.
        JsonSchema newSchema = originalSchema.withConfig(disabledConfig);

        // The returned schema must be a *different* instance.
        assertNotSame(originalSchema, newSchema,
                "Expected a new JsonSchema instance when the supplied config disables a required keyword");

        // Verify that the new schema's validation context now has the discriminator
        // keyword enabled (and nullable enabled as a side‑effect).
        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertTrue(resultingConfig.isDiscriminatorKeywordEnabled(),
                "Discriminator keyword should be enabled in the resulting configuration");
        assertTrue(resultingConfig.isNullableKeywordEnabled(),
                "Nullable keyword should be enabled in the resulting configuration");
    }
}
```