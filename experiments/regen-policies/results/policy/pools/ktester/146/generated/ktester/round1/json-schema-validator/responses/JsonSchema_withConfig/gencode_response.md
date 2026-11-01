```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#withConfig(SchemaValidatorsConfig)}.
 *
 * The tests cover the following groups (derived from the provided JSON test cases):
 *
 * <ul>
 *   <li><b>sameConfigReturnsOriginalInstance</b> – when the supplied
 *       {@link SchemaValidatorsConfig} is equal to the one already stored in the
 *       {@link ValidationContext}, the same {@link JsonSchema} instance must be
 *       returned.</li>
 *   <li><b>differentConfigCreatesNewInstance</b> – when the supplied configuration
 *       differs (and the meta‑schema does not force a change), a new
 *       {@link JsonSchema} instance must be created with the new configuration.</li>
 *   <li><b>discriminatorKeywordAutoEnabled</b> – when the meta‑schema contains the
 *       {@code discriminator} keyword but the supplied config disables it, the
 *       method must automatically enable {@code discriminator} and {@code nullable}
 *       and return a new schema instance.</li>
 *   <li><b>nullConfigThrowsNPE</b> – passing {@code null} as the configuration must
 *       result in a {@link NullPointerException}.</li>
 *   <li><b>nullValidationContextThrowsNPE</b> – if the schema’s
 *       {@link ValidationContext} is {@code null}, calling {@code withConfig}
 *       must throw a {@link NullPointerException}.</li>
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

    /* --------------------------------------------------------------------- */
    /* 1. sameConfigReturnsOriginalInstance                                    */
    /* --------------------------------------------------------------------- */

    /**
     * The {@code withConfig} method must return the original {@link JsonSchema}
     * instance when the supplied configuration is equal (by {@code equals}) to
     * the configuration stored in the current {@link ValidationContext}.
     */
    @Test
    void sameConfigReturnsOriginalInstance() {
        String schemaJson = "{"
                + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\": \"object\""
                + "}";

        // Configuration used to create the schema.
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .nullableKeywordEnabled(true)
                .build();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson, originalConfig);

        // Build a *different* instance that is equal to the original one.
        SchemaValidatorsConfig equalConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .nullableKeywordEnabled(true)
                .build();

        JsonSchema result = schema.withConfig(equalConfig);
        assertSame(schema, result,
                "Expected the same JsonSchema instance when the supplied config is equal to the current one");
    }

    /* --------------------------------------------------------------------- */
    /* 2. differentConfigCreatesNewInstance                                    */
    /* --------------------------------------------------------------------- */

    /**
     * When the supplied configuration differs from the one stored in the
     * {@link ValidationContext} and the meta‑schema does not force an automatic
     * change, {@code withConfig} must build a new {@link ValidationContext} and
     * return a freshly constructed {@link JsonSchema}.
     */
    @Test
    void differentConfigCreatesNewInstance() {
        String schemaJson = "{"
                + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\": \"object\""
                + "}";

        // Original configuration – discriminator enabled.
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .nullableKeywordEnabled(true)
                .build();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, originalConfig);

        // New configuration – discriminator disabled (different from original).
        SchemaValidatorsConfig newConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(true) // keep nullable the same to isolate the change
                .build();

        JsonSchema newSchema = originalSchema.withConfig(newConfig);

        // Must be a different instance.
        assertNotSame(originalSchema, newSchema,
                "Expected a new JsonSchema instance when the supplied config differs");

        // The new schema's ValidationContext must hold the *exact* configuration we passed.
        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertEquals(newConfig, resultingConfig,
                "The resulting ValidationContext should contain the supplied configuration");
    }

    /* --------------------------------------------------------------------- */
    /* 3. discriminatorKeywordAutoEnabled                                      */
    /* --------------------------------------------------------------------- */

    /**
     * If the meta‑schema contains the {@code discriminator} keyword but the
     * supplied configuration disables it, {@code withConfig} must automatically
     * enable both {@code discriminator} and {@code nullable} and return a new
     * {@link JsonSchema} instance.
     */
    @Test
    void discriminatorKeywordAutoEnabled() {
        // Schema that contains a discriminator keyword (supported by draft‑2020‑12).
        String schemaJson = "{"
                + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\": \"object\","
                + "\"properties\": {\"type\": {\"type\": \"string\"}},"
                + "\"discriminator\": {"
                + "    \"propertyName\": \"type\","
                + "    \"mapping\": {\"a\": \"#/definitions/A\"}"
                + "},"
                + "\"definitions\": {"
                + "    \"A\": {"
                + "        \"type\": \"object\","
                + "        \"properties\": {\"a\": {\"type\": \"string\"}}"
                + "    }"
                + "}"
                + "}";

        // Configuration that explicitly disables the discriminator (and nullable).
        SchemaValidatorsConfig disabledConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(false)
                .build();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, disabledConfig);

        // The call should trigger the auto‑enable logic.
        JsonSchema newSchema = originalSchema.withConfig(disabledConfig);

        assertNotSame(originalSchema, newSchema,
                "Expected a new JsonSchema instance when the supplied config disables a required keyword");

        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertTrue(resultingConfig.isDiscriminatorKeywordEnabled(),
                "Discriminator keyword should be enabled in the resulting configuration");
        assertTrue(resultingConfig.isNullableKeywordEnabled(),
                "Nullable keyword should be enabled in the resulting configuration");
    }

    /* --------------------------------------------------------------------- */
    /* 4. nullConfigThrowsNPE                                                  */
    /* --------------------------------------------------------------------- */

    /**
     * Passing {@code null} as the {@link SchemaValidatorsConfig} must result in a
     * {@link NullPointerException}.
     */
    @Test
    void nullConfigThrowsNPE() {
        String schemaJson = "{"
                + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\": \"object\""
                + "}";

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson);

        assertThrows(NullPointerException.class,
                () -> schema.withConfig(null),
                "Calling withConfig(null) should throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* 5. nullValidationContextThrowsNPE                                       */
    /* --------------------------------------------------------------------- */

    /**
     * If the {@link JsonSchema} instance has a {@code null} {@link ValidationContext},
     * calling {@code withConfig} must throw a {@link NullPointerException}.
     * This situation is simulated by creating a real schema and then using
     * reflection (or a Mockito spy) to make {@code getValidationContext()} return
     * {@code null}.
     */
    @Test
    void nullValidationContextThrowsNPE() throws Exception {
        String schemaJson = "{"
                + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\": \"object\""
                + "}";

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema realSchema = factory.getSchema(schemaJson);

        // Create a spy so we can stub getValidationContext() to return null.
        JsonSchema spySchema = Mockito.spy(realSchema);
        Mockito.doReturn(null).when(spySchema).getValidationContext();

        SchemaValidatorsConfig anyConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .build();

        assertThrows(NullPointerException.class,
                () -> spySchema.withConfig(anyConfig),
                "Calling withConfig when ValidationContext is null should throw NullPointerException");
    }
}
```