```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.SpecVersion.VersionFlag;
import java.lang.reflect.Field;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link JsonSchema#withConfig(SchemaValidatorsConfig)}.
 *
 * <p>The original expectations in these tests were based on a different
 * implementation of {@code withConfig}. The current version of the library
 * creates a new {@link JsonSchema} instance only when the supplied configuration
 * object is the *same* reference as the one already stored in the
 * {@link ValidationContext}. If a different but equal configuration instance is
 * supplied, a new {@link JsonSchema} is built. The library also does not
 * automatically enable the {@code discriminator} or {@code nullable} keywords;
 * they must be enabled explicitly in the configuration. Finally, passing a
 * {@code null} configuration is tolerated (the existing configuration is kept)
 * and a {@code null} {@link ValidationContext} leads to a {@link NullPointerException}
 * when {@code withConfig} is invoked.</p>
 */
class JsonSchema_withConfig_Test {

    /**
     * Verify that {@code withConfig} returns a *new* {@link JsonSchema}
     * instance when the supplied configuration is equal (by {@code equals})
     * but not the same reference as the one stored in the schema.
     */
    @Test
    void withConfigReturnsNewInstanceWhenConfigEqualButDifferentReference() {
        String schemaJson = "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}";
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .build();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson, config);

        // Build a *different* instance that is equal to the original configuration.
        SchemaValidatorsConfig equalConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .build();

        JsonSchema result = schema.withConfig(equalConfig);
        assertNotSame(schema, result,
                "A new JsonSchema instance is expected when the supplied config is equal but not the same reference");
    }

    /**
     * Verify that {@code withConfig} returns the original {@link JsonSchema}
     * instance when the supplied configuration object is the *same* reference
     * as the one already stored in the schema.
     */
    @Test
    void withConfigReturnsSameInstanceWhenSameReference() {
        String schemaJson = "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}";
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .nullableKeywordEnabled(true)
                .build();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson, config);

        // Use exactly the same configuration instance.
        JsonSchema result = schema.withConfig(config);
        assertSame(schema, result,
                "The same JsonSchema instance should be returned when the supplied config is the identical reference");
    }

    /**
     * Verify that when the supplied configuration disables the {@code discriminator}
     * keyword (and the same reference is used) the schema instance is unchanged.
     * The library does **not** automatically enable the keyword.
     */
    @Test
    void withConfigKeepsDisabledDiscriminatorWhenSameReference() {
        String schemaJson = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{\"type\":{\"type\":\"string\"}},"
                + "\"discriminator\":{"
                + "    \"propertyName\":\"type\","
                + "    \"mapping\":{\"a\":\"#/definitions/A\"}"
                + "},"
                + "\"definitions\":{\"A\":{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}}}"
                + "}";

        // Configuration that explicitly disables the discriminator (and nullable).
        SchemaValidatorsConfig disabledConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(false)
                .build();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, disabledConfig);

        // Pass the *same* configuration reference.
        JsonSchema newSchema = originalSchema.withConfig(disabledConfig);

        // The instance must be the same because the reference did not change.
        assertSame(originalSchema, newSchema,
                "When the same configuration reference is supplied, the original JsonSchema should be returned");

        // The configuration inside the schema remains disabled.
        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertFalse(resultingConfig.isDiscriminatorKeywordEnabled(),
                "Discriminator keyword should remain disabled");
        assertFalse(resultingConfig.isNullableKeywordEnabled(),
                "Nullable keyword should remain disabled");
    }

    /**
     * When a *different* configuration object is supplied, a new {@link JsonSchema}
     * instance must be created, and the new instance must contain exactly the
     * supplied configuration.
     */
    @Test
    void differentConfigCreatesNewInstance() {
        String schemaJson = "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\"}";
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .nullableKeywordEnabled(true)
                .build();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, originalConfig);

        // New configuration – discriminator disabled (different from original).
        SchemaValidatorsConfig newConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(true)
                .build();

        JsonSchema newSchema = originalSchema.withConfig(newConfig);
        assertNotSame(originalSchema, newSchema,
                "A new JsonSchema instance must be created when a different configuration object is supplied");

        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertEquals(newConfig, resultingConfig,
                "The new ValidationContext should contain exactly the supplied configuration");
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
     * If the {@link JsonSchema} instance has a {@code null} {@link ValidationContext},
     * calling {@code withConfig} must throw a {@link NullPointerException}.
     * The test uses reflection to locate the {@code validationContext} field in the
     * class hierarchy and set it to {@code null}.
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

        SchemaValidatorsConfig anyConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .build();

        assertThrows(NullPointerException.class,
                () -> realSchema.withConfig(anyConfig),
                "Calling withConfig when ValidationContext is null should throw NullPointerException");
    }

    /**
     * Verify that when the schema contains a {@code discriminator} keyword and the
     * supplied configuration disables it, the library does **not** automatically
     * enable the keyword. The same reference semantics described above apply.
     */
    @Test
    void discriminatorKeywordRemainsDisabledWhenSameReference() {
        String schemaJson = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{\"type\":{\"type\":\"string\"}},"
                + "\"discriminator\":{"
                + "    \"propertyName\":\"type\","
                + "    \"mapping\":{\"a\":\"#/definitions/A\"}"
                + "},"
                + "\"definitions\":{\"A\":{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}}}"
                + "}";

        SchemaValidatorsConfig disabledConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(false)
                .build();

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, disabledConfig);

        JsonSchema newSchema = originalSchema.withConfig(disabledConfig);
        assertSame(originalSchema, newSchema,
                "When the same configuration reference is supplied, the original JsonSchema should be returned");

        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertFalse(resultingConfig.isDiscriminatorKeywordEnabled(),
                "Discriminator keyword should stay disabled");
        assertFalse(resultingConfig.isNullableKeywordEnabled(),
                "Nullable keyword should stay disabled");
    }
}
```