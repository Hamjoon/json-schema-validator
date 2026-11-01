```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#createValidationContext(JsonNode, SchemaValidatorsConfig)}.
 *
 * The method is {@code protected}, therefore we invoke it via reflection.
 * The tests verify the two branches inside the method:
 *
 * <ul>
 *   <li>If the meta‑schema contains the {@code discriminator} keyword and the supplied
 *       {@link SchemaValidatorsConfig} does not have {@code discriminatorKeywordEnabled}
 *       set to {@code true}, a new configuration instance is created with the flag turned on
 *       (and {@code nullableKeywordEnabled} also turned on).</li>
 *   <li>If the flag is already enabled, the original configuration object is returned unchanged.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createValidationContext_Test {

    /**
     * Helper that invokes the protected {@code createValidationContext} method via reflection.
     */
    private ValidationContext invokeCreateValidationContext(JsonSchemaFactory factory,
                                                            JsonNode schemaNode,
                                                            SchemaValidatorsConfig config) throws Exception {
        Method method = JsonSchemaFactory.class
                .getDeclaredMethod("createValidationContext", JsonNode.class, SchemaValidatorsConfig.class);
        method.setAccessible(true);
        return (ValidationContext) method.invoke(factory, schemaNode, config);
    }

    /**
     * When the meta‑schema contains the {@code discriminator} keyword and the supplied
     * configuration has {@code discriminatorKeywordEnabled}=false, the factory must create a
     * new {@link SchemaValidatorsConfig} with the flag (and {@code nullableKeywordEnabled})
     * turned on.
     */
    @Test
    void shouldEnableDiscriminatorWhenMissingInConfig() throws Exception {
        // Use the 2020‑12 draft which defines the "discriminator" keyword.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

        // Minimal schema that points to the 2020‑12 meta‑schema.
        ObjectMapper mapper = factory.getJsonMapper();
        JsonNode schemaNode = mapper.readTree("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"}");

        // Configuration with the discriminator flag disabled.
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(false)
                .build();

        ValidationContext ctx = invokeCreateValidationContext(factory, schemaNode, originalConfig);

        // The ValidationContext stores the (possibly new) configuration.
        SchemaValidatorsConfig resultingConfig = ctx.getConfig();

        // A new instance must have been created (different reference) and the flag must be true.
        assertNotSame(originalConfig, resultingConfig,
                "A new SchemaValidatorsConfig should be created when discriminator is disabled");
        assertTrue(resultingConfig.isDiscriminatorKeywordEnabled(),
                "discriminatorKeywordEnabled must be true after creation");
        assertTrue(resultingConfig.isNullableKeywordEnabled(),
                "nullableKeywordEnabled must be true when discriminator is auto‑enabled");
    }

    /**
     * When the supplied configuration already has {@code discriminatorKeywordEnabled}=true,
     * the factory must return the same configuration instance (no wrapping).
     */
    @Test
    void shouldReturnSameConfigWhenDiscriminatorAlreadyEnabled() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        ObjectMapper mapper = factory.getJsonMapper();
        JsonNode schemaNode = mapper.readTree("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"}");

        // Configuration already enables discriminator (and nullable, matching the branch logic).
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .nullableKeywordEnabled(true)
                .build();

        ValidationContext ctx = invokeCreateValidationContext(factory, schemaNode, originalConfig);
        SchemaValidatorsConfig resultingConfig = ctx.getConfig();

        // The same instance must be returned.
        assertSame(originalConfig, resultingConfig,
                "When discriminator is already enabled the original config should be returned");
    }
}
```