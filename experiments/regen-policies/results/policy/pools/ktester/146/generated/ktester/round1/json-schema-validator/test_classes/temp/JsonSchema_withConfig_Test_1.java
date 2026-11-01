package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class JsonSchema_withConfig_Test {

    /**
     * Verify that {@code withConfig} returns the same {@link JsonSchema}
     * instance when the supplied configuration is equal to the one already
     * used by the schema.
     */
    @Test
    void withConfigReturnsSameInstanceWhenConfigUnchanged() {
        // a simple schema without a discriminator keyword
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"" + "}";
        // configuration with discriminator enabled (the default for the meta‑schema)
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson, config);
        // invoke withConfig using an *equal* configuration (different instance)
        SchemaValidatorsConfig equalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
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
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"," + "\"properties\": {" + "    \"type\": {\"type\": \"string\"}" + "}," + "\"discriminator\": {" + "    \"propertyName\": \"type\"," + "    \"mapping\": {" + "        \"a\": \"#/definitions/A\"" + "    }" + "}," + "\"definitions\": {" + "    \"A\": {" + "        \"type\": \"object\"," + "        \"properties\": {" + "            \"a\": {\"type\": \"string\"}" + "        }" + "    }" + "}" + "}";
        // Start with a configuration that explicitly disables the discriminator keyword
        SchemaValidatorsConfig disabledConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).// also disabled to verify it gets enabled automatically
        nullableKeywordEnabled(false).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, disabledConfig);
        // Call withConfig using the same (disabled) configuration.
        // According to the implementation, the method should replace it with a new
        // configuration that enables both discriminator and nullable keywords.
        JsonSchema newSchema = originalSchema.withConfig(disabledConfig);
        // The returned schema must be a *different* instance.
        assertNotSame(originalSchema, newSchema, "Expected a new JsonSchema instance when the supplied config disables a required keyword");
        // Verify that the new schema's validation context now has the discriminator
        // keyword enabled (and nullable enabled as a side‑effect).
        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertTrue(resultingConfig.isDiscriminatorKeywordEnabled(), "Discriminator keyword should be enabled in the resulting configuration");
        assertTrue(resultingConfig.isNullableKeywordEnabled(), "Nullable keyword should be enabled in the resulting configuration");
    }

    /**
     * The {@code withConfig} method must return the original {@link JsonSchema}
     * instance when the supplied configuration is equal (by {@code equals}) to
     * the configuration stored in the current {@link ValidationContext}.
     */
    @Test
    void sameConfigReturnsOriginalInstance() {
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"" + "}";
        // Configuration used to create the schema.
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson, originalConfig);
        // Build a *different* instance that is equal to the original one.
        SchemaValidatorsConfig equalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
        JsonSchema result = schema.withConfig(equalConfig);
        assertSame(schema, result, "Expected the same JsonSchema instance when the supplied config is equal to the current one");
    }

    /**
     * When the supplied configuration differs from the one stored in the
     * {@link ValidationContext} and the meta‑schema does not force an automatic
     * change, {@code withConfig} must build a new {@link ValidationContext} and
     * return a freshly constructed {@link JsonSchema}.
     */
    @Test
    void differentConfigCreatesNewInstance() {
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"" + "}";
        // Original configuration – discriminator enabled.
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, originalConfig);
        // New configuration – discriminator disabled (different from original).
        SchemaValidatorsConfig newConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).// keep nullable the same to isolate the change
        nullableKeywordEnabled(true).build();
        JsonSchema newSchema = originalSchema.withConfig(newConfig);
        // Must be a different instance.
        assertNotSame(originalSchema, newSchema, "Expected a new JsonSchema instance when the supplied config differs");
        // The new schema's ValidationContext must hold the *exact* configuration we passed.
        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertEquals(newConfig, resultingConfig, "The resulting ValidationContext should contain the supplied configuration");
    }

    /**
     * If the meta‑schema contains the {@code discriminator} keyword but the
     * supplied configuration disables it, {@code withConfig} must automatically
     * enable both {@code discriminator} and {@code nullable} and return a new
     * {@link JsonSchema} instance.
     */
    @Test
    void discriminatorKeywordAutoEnabled() {
        // Schema that contains a discriminator keyword (supported by draft‑2020‑12).
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"," + "\"properties\": {\"type\": {\"type\": \"string\"}}," + "\"discriminator\": {" + "    \"propertyName\": \"type\"," + "    \"mapping\": {\"a\": \"#/definitions/A\"}" + "}," + "\"definitions\": {" + "    \"A\": {" + "        \"type\": \"object\"," + "        \"properties\": {\"a\": {\"type\": \"string\"}}" + "    }" + "}" + "}";
        // Configuration that explicitly disables the discriminator (and nullable).
        SchemaValidatorsConfig disabledConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, disabledConfig);
        // The call should trigger the auto‑enable logic.
        JsonSchema newSchema = originalSchema.withConfig(disabledConfig);
        assertNotSame(originalSchema, newSchema, "Expected a new JsonSchema instance when the supplied config disables a required keyword");
        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertTrue(resultingConfig.isDiscriminatorKeywordEnabled(), "Discriminator keyword should be enabled in the resulting configuration");
        assertTrue(resultingConfig.isNullableKeywordEnabled(), "Nullable keyword should be enabled in the resulting configuration");
    }

    /**
     * Passing {@code null} as the {@link SchemaValidatorsConfig} must result in a
     * {@link NullPointerException}.
     */
    @Test
    void nullConfigThrowsNPE() {
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"" + "}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(NullPointerException.class, () -> schema.withConfig(null), "Calling withConfig(null) should throw NullPointerException");
    }

    /**
     * If the {@link JsonSchema} instance has a {@code null} {@link ValidationContext},
     * calling {@code withConfig} must throw a {@link NullPointerException}.
     * This situation is simulated by creating a real schema and then using
     * reflection (or a Mockito spy) to make {@code getValidationContext()} return
     * {@code null}.
     */
    @Test
    void nullValidationContextThrowsNPE() throws Exception {
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"" + "}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema realSchema = factory.getSchema(schemaJson);
        // Use reflection to nullify the internal validationContext.
        Field vcField = JsonSchema.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        vcField.set(realSchema, null);
        SchemaValidatorsConfig anyConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
        assertThrows(NullPointerException.class, () -> realSchema.withConfig(anyConfig), "Calling withConfig when ValidationContext is null should throw NullPointerException");
    }
}
