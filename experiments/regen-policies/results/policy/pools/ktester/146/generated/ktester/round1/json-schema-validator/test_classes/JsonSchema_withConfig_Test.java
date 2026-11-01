package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.lang.reflect.Field;
import org.junit.jupiter.api.*;

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
        SchemaValidatorsConfig disabledConfig = // also disabled to verify it gets enabled automatically
        SchemaValidatorsConfig.builder().// also disabled to verify it gets enabled automatically
        discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build();
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
        String schemaJson = "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}";
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, originalConfig);
        // New configuration – discriminator disabled (different from original).
        SchemaValidatorsConfig newConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(true).build();
        JsonSchema newSchema = originalSchema.withConfig(newConfig);
        assertNotSame(originalSchema, newSchema, "A new JsonSchema instance must be created when a different configuration object is supplied");
        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertEquals(newConfig, resultingConfig, "The new ValidationContext should contain exactly the supplied configuration");
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
        String schemaJson = "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema realSchema = factory.getSchema(schemaJson);
        // Find the field named "validationContext" in the class hierarchy.
        Field vcField = null;
        Class<?> cls = realSchema.getClass();
        while (cls != null && vcField == null) {
            try {
                vcField = cls.getDeclaredField("validationContext");
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        assertNotNull(vcField, "Field 'validationContext' should exist in JsonSchema hierarchy");
        vcField.setAccessible(true);
        vcField.set(realSchema, null);
        SchemaValidatorsConfig anyConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
        assertThrows(NullPointerException.class, () -> realSchema.withConfig(anyConfig), "Calling withConfig when ValidationContext is null should throw NullPointerException");
    }

    /**
     * Verify that {@code withConfig} returns a *new* {@link JsonSchema}
     * instance when the supplied configuration is equal (by {@code equals})
     * but not the same reference as the one stored in the schema.
     */
    @Test
    void withConfigReturnsNewInstanceWhenConfigEqualButDifferentReference() {
        String schemaJson = "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}";
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson, config);
        // Build a *different* instance that is equal to the original configuration.
        SchemaValidatorsConfig equalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
        JsonSchema result = schema.withConfig(equalConfig);
        assertNotSame(schema, result, "A new JsonSchema instance is expected when the supplied config is equal but not the same reference");
    }

    /**
     * Verify that {@code withConfig} returns the original {@link JsonSchema}
     * instance when the supplied configuration object is the *same* reference
     * as the one already stored in the schema.
     */
    @Test
    void withConfigReturnsSameInstanceWhenSameReference() {
        String schemaJson = "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}";
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson, config);
        // Use exactly the same configuration instance.
        JsonSchema result = schema.withConfig(config);
        assertSame(schema, result, "The same JsonSchema instance should be returned when the supplied config is the identical reference");
    }

    /**
     * Verify that when the supplied configuration disables the {@code discriminator}
     * keyword (and the same reference is used) the schema instance is unchanged.
     * The library does **not** automatically enable the keyword.
     */
    @Test
    void withConfigKeepsDisabledDiscriminatorWhenSameReference() {
        String schemaJson = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"type\":{\"type\":\"string\"}}," + "\"discriminator\":{" + "    \"propertyName\":\"type\"," + "    \"mapping\":{\"a\":\"#/definitions/A\"}" + "}," + "\"definitions\":{\"A\":{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}}}" + "}";
        // Configuration that explicitly disables the discriminator (and nullable).
        SchemaValidatorsConfig disabledConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, disabledConfig);
        // Pass the *same* configuration reference.
        JsonSchema newSchema = originalSchema.withConfig(disabledConfig);
        // The instance must be the same because the reference did not change.
        assertSame(originalSchema, newSchema, "When the same configuration reference is supplied, the original JsonSchema should be returned");
        // The configuration inside the schema remains disabled.
        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertFalse(resultingConfig.isDiscriminatorKeywordEnabled(), "Discriminator keyword should remain disabled");
        assertFalse(resultingConfig.isNullableKeywordEnabled(), "Nullable keyword should remain disabled");
    }

    /**
     * Passing {@code null} as the {@link SchemaValidatorsConfig} does **not**
     * throw an exception in the current implementation – the existing configuration
     * is retained. The test verifies that no exception is thrown and that the
     * original schema instance is returned.
     */
    @Test
    void nullConfigIsIgnored() {
        String schemaJson = "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson);
        // No exception should be thrown.
        assertDoesNotThrow(() -> {
            JsonSchema result = schema.withConfig(null);
            // The implementation returns the original instance when null is supplied.
            assertSame(schema, result, "When null is supplied, the original JsonSchema instance should be returned");
        });
    }

    /**
     * Verify that when the schema contains a {@code discriminator} keyword and the
     * supplied configuration disables it, the library does **not** automatically
     * enable the keyword. The same reference semantics described above apply.
     */
    @Test
    void discriminatorKeywordRemainsDisabledWhenSameReference() {
        String schemaJson = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{\"type\":{\"type\":\"string\"}}," + "\"discriminator\":{" + "    \"propertyName\":\"type\"," + "    \"mapping\":{\"a\":\"#/definitions/A\"}" + "}," + "\"definitions\":{\"A\":{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}}}" + "}";
        SchemaValidatorsConfig disabledConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, disabledConfig);
        JsonSchema newSchema = originalSchema.withConfig(disabledConfig);
        assertSame(originalSchema, newSchema, "When the same configuration reference is supplied, the original JsonSchema should be returned");
        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertFalse(resultingConfig.isDiscriminatorKeywordEnabled(), "Discriminator keyword should stay disabled");
        assertFalse(resultingConfig.isNullableKeywordEnabled(), "Nullable keyword should stay disabled");
    }
}
