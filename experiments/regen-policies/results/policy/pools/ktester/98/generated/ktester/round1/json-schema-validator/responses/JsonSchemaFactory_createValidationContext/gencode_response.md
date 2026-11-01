```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#createValidationContext(JsonNode, SchemaValidatorsConfig)}.
 *
 * The method is {@code protected}, therefore we invoke it via reflection.
 *
 * The test suite covers:
 * <ul>
 *   <li>Enabling the discriminator flag when the meta‑schema contains the keyword
 *       and the supplied {@link SchemaValidatorsConfig} disables it.</li>
 *   <li>Leaving the configuration untouched when the flag is already enabled.</li>
 *   <li>Leaving the configuration untouched when the meta‑schema does not declare the
 *       discriminator keyword (irrespective of the supplied flags).</li>
 *   <li>Null‑argument handling (NPE) for {@code config} and {@code schemaNode}.</li>
 *   <li>Defensive behaviour when {@code getMetaSchemaOrDefault} returns {@code null}
 *       or a {@link JsonMetaSchema} with a {@code null} keywords map.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createValidationContext_Test {

    /** Helper that invokes the protected {@code createValidationContext} method via reflection. */
    private ValidationContext invokeCreateValidationContext(JsonSchemaFactory factory,
                                                            JsonNode schemaNode,
                                                            SchemaValidatorsConfig config) throws Exception {
        var method = JsonSchemaFactory.class
                .getDeclaredMethod("createValidationContext", JsonNode.class, SchemaValidatorsConfig.class);
        method.setAccessible(true);
        return (ValidationContext) method.invoke(factory, schemaNode, config);
    }

    /* --------------------------------------------------------------------- */
    /*  Existing test cases (kept unchanged)                                 */
    /* --------------------------------------------------------------------- */

    /**
     * When the meta‑schema contains the {@code discriminator} keyword and the supplied
     * configuration has {@code discriminatorKeywordEnabled}=false, the factory must
     * create a new {@link SchemaValidatorsConfig} with the flag (and {@code nullableKeywordEnabled})
     * turned on.
     */
    @Test
    void shouldEnableDiscriminatorWhenMissingInConfig() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        ObjectMapper mapper = factory.getJsonMapper();
        JsonNode schemaNode = mapper.readTree("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"}");

        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(false)
                .build();

        ValidationContext ctx = invokeCreateValidationContext(factory, schemaNode, originalConfig);
        SchemaValidatorsConfig resultingConfig = ctx.getConfig();

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

        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .nullableKeywordEnabled(true)
                .build();

        ValidationContext ctx = invokeCreateValidationContext(factory, schemaNode, originalConfig);
        SchemaValidatorsConfig resultingConfig = ctx.getConfig();

        assertSame(originalConfig, resultingConfig,
                "When discriminator is already enabled the original config should be returned");
    }

    /* --------------------------------------------------------------------- */
    /*  Additional test cases                                               */
    /* --------------------------------------------------------------------- */

    /**
     * Parameterised test for meta‑schemas that do NOT contain the {@code discriminator}
     * keyword (e.g., Draft‑7). The supplied configuration must be returned unchanged,
     * regardless of the flag values.
     */
    @ParameterizedTest
    @MethodSource("configsWithoutDiscriminator")
    void shouldLeaveConfigUnchangedWhenMetaSchemaLacksDiscriminator(SchemaValidatorsConfig config) throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909); // Draft‑7 does not have discriminator
        ObjectMapper mapper = factory.getJsonMapper();
        JsonNode schemaNode = mapper.readTree("{\"$schema\":\"http://json-schema.org/draft-07/schema#\"}");

        ValidationContext ctx = invokeCreateValidationContext(factory, schemaNode, config);
        SchemaValidatorsConfig resultingConfig = ctx.getConfig();

        assertSame(config, resultingConfig,
                "Configuration should be unchanged when meta‑schema lacks discriminator");
    }

    private static Stream<Arguments> configsWithoutDiscriminator() {
        return Stream.of(
                Arguments.of(SchemaValidatorsConfig.builder()
                        .discriminatorKeywordEnabled(false)
                        .nullableKeywordEnabled(false)
                        .build()),
                Arguments.of(SchemaValidatorsConfig.builder()
                        .discriminatorKeywordEnabled(true)
                        .nullableKeywordEnabled(false)
                        .build()),
                Arguments.of(SchemaValidatorsConfig.builder()
                        .discriminatorKeywordEnabled(false)
                        .nullableKeywordEnabled(true)
                        .build()),
                Arguments.of(SchemaValidatorsConfig.builder()
                        .discriminatorKeywordEnabled(true)
                        .nullableKeywordEnabled(true)
                        .build())
        );
    }

    /**
     * Supplying {@code null} as the configuration while the meta‑schema contains the
     * {@code discriminator} keyword must result in a {@link NullPointerException}
     * because the method dereferences {@code config.isDiscriminatorKeywordEnabled()}.
     */
    @Test
    void shouldThrowNPEWhenConfigIsNullAndMetaSchemaHasDiscriminator() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        ObjectMapper mapper = factory.getJsonMapper();
        JsonNode schemaNode = mapper.readTree("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"}");

        assertThrows(NullPointerException.class,
                () -> invokeCreateValidationContext(factory, schemaNode, null),
                "Expected NPE when config is null and discriminator keyword is present");
    }

    /**
     * Supplying {@code null} as the configuration while the meta‑schema does NOT contain
     * the {@code discriminator} keyword also leads to a {@link NullPointerException},
     * but the cause is the {@link ValidationContext} constructor receiving a {@code null}
     * config. The test asserts that an NPE is thrown.
     */
    @Test
    void shouldThrowNPEWhenConfigIsNullAndMetaSchemaLacksDiscriminator() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        ObjectMapper mapper = factory.getJsonMapper();
        JsonNode schemaNode = mapper.readTree("{\"$schema\":\"http://json-schema.org/draft-07/schema#\"}");

        assertThrows(NullPointerException.class,
                () -> invokeCreateValidationContext(factory, schemaNode, null),
                "Expected NPE when config is null and meta‑schema lacks discriminator");
    }

    /**
     * Supplying {@code null} as the schema node must cause a {@link NullPointerException}
     * because {@code getMetaSchemaOrDefault} dereferences the argument.
     */
    @Test
    void shouldThrowNPEWhenSchemaNodeIsNull() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        assertThrows(NullPointerException.class,
                () -> invokeCreateValidationContext(factory, null, config),
                "Expected NPE when schemaNode is null");
    }

    /**
     * If {@code getMetaSchemaOrDefault} (which is protected) unexpectedly returns {@code null},
     * the subsequent call to {@code jsonMetaSchema.getKeywords()} triggers a {@link NullPointerException}.
     */
    @Test
    void shouldThrowNPEWhenMetaSchemaIsNull() throws Exception {
        // Factory that forces getMetaSchemaOrDefault to return null
        JsonSchemaFactory factory = new JsonSchemaFactory() {
            @Override
            protected JsonMetaSchema getMetaSchemaOrDefault(JsonNode schemaNode, SchemaValidatorsConfig config) {
                return null; // simulate unexpected null
            }
        };
        ObjectMapper mapper = factory.getJsonMapper();
        JsonNode schemaNode = mapper.readTree("{\"any\":\"value\"}");
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        assertThrows(NullPointerException.class,
                () -> invokeCreateValidationContext(factory, schemaNode, config),
                "Expected NPE when meta‑schema is null");
    }

    /**
     * If the resolved {@link JsonMetaSchema} returns a {@code null} keywords map,
     * the call to {@code containsKey("discriminator")} results in a {@link NullPointerException}.
     */
    @Test
    void shouldThrowNPEWhenMetaSchemaKeywordsIsNull() throws Exception {
        JsonMetaSchema metaWithNullKeywords = org.mockito.Mockito.mock(JsonMetaSchema.class);
        org.mockito.Mockito.when(metaWithNullKeywords.getKeywords()).thenReturn(null);

        JsonSchemaFactory factory = new JsonSchemaFactory() {
            @Override
            protected JsonMetaSchema getMetaSchemaOrDefault(JsonNode schemaNode, SchemaValidatorsConfig config) {
                return metaWithNullKeywords;
            }
        };
        ObjectMapper mapper = factory.getJsonMapper();
        JsonNode schemaNode = mapper.readTree("{\"any\":\"value\"}");
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        assertThrows(NullPointerException.class,
                () -> invokeCreateValidationContext(factory, schemaNode, config),
                "Expected NPE when meta‑schema keywords map is null");
    }
}
```