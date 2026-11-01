package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.*;
import java.util.Collections;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_withConfig_Test {

    /**
     * Simple JSON schema used for all tests.
     */
    private static final String SIMPLE_SCHEMA = "{\n" + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" + "  \"type\": \"object\"\n" + "}";

    /**
     * Factory that creates {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory for the 2020‑12 draft – the concrete draft does not
        // matter for the withConfig logic, only that the factory can build a schema.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    /**
     * Replaces the {@code validationContext} field of the supplied {@link JsonSchema}
     * with the given mock.  The field lives in {@link BaseJsonValidator}, so we
     * have to reach into that superclass via reflection.
     */
    private static void injectValidationContext(JsonSchema schema, ValidationContext mockContext) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(schema, mockContext);
    }

    /**
     * Creates a {@link JsonSchema} instance whose internal {@link ValidationContext}
     * is a Mockito mock configured with the supplied parameters.
     *
     * @param metaContainsDiscriminator whether the meta‑schema's keyword map
     *                                  contains the key {@code "discriminator"}
     * @param currentConfig the configuration stored in the mocked context
     * @return a {@link JsonSchema} ready for the {@code withConfig} call
     */
    private JsonSchema createSchemaWithMockContext(boolean metaContainsDiscriminator, SchemaValidatorsConfig currentConfig) throws Exception {
        // Create a real schema first – we only need a concrete instance.
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        // Mock the ValidationContext hierarchy.
        ValidationContext mockContext = mock(ValidationContext.class);
        MetaSchema mockMeta = mock(MetaSchema.class);
        Map<String, Keyword> keywords = metaContainsDiscriminator ? Collections.singletonMap("discriminator", mock(Keyword.class)) : Collections.emptyMap();
        when(mockContext.getMetaSchema()).thenReturn(mockMeta);
        when(mockMeta.getKeywords()).thenReturn(keywords);
        when(mockContext.getConfig()).thenReturn(currentConfig);
        // The other getters used in the constructor are not needed for the
        // branches we test, so they can return null safely.
        when(mockContext.getJsonSchemaFactory()).thenReturn(null);
        when(mockContext.getSchemaReferences()).thenReturn(null);
        when(mockContext.getSchemaResources()).thenReturn(null);
        when(mockContext.getDynamicAnchors()).thenReturn(null);
        injectValidationContext(schema, mockContext);
        return schema;
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
        SchemaValidatorsConfig newConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
        // Obtain a schema with the original configuration.
        JsonSchema originalSchema = factory.getSchema(SIMPLE_SCHEMA, originalConfig);
        // Invoke withConfig using a different configuration.
        JsonSchema newSchema = originalSchema.withConfig(newConfig);
        // The returned schema must be a *different* object.
        assertNotSame(originalSchema, newSchema, "withConfig should create a new JsonSchema instance when the config differs");
        // The new schema's validation context must hold the new configuration.
        assertEquals(newConfig, newSchema.getValidationContext().getConfig(), "The new JsonSchema should contain the supplied configuration");
    }

    @Test
    void firstIf_discriminatorPresent_configDisabled_returnsNewInstanceWithRebuiltConfig() throws Exception {
        // Context contains "discriminator"
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build();
        // The current context config is the default (both false)
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().build();
        JsonSchema schema = createSchemaWithMockContext(true, current);
        JsonSchema result = schema.withConfig(supplied);
        assertNotSame(schema, result, "A new instance must be created because the rebuilt config differs");
        SchemaValidatorsConfig usedConfig = result.getValidationContext().getConfig();
        assertTrue(usedConfig.isDiscriminatorKeywordEnabled(), "Rebuilt config should have discriminatorKeywordEnabled = true");
        assertTrue(usedConfig.isNullableKeywordEnabled(), "Rebuilt config should have nullableKeywordEnabled = true");
    }

    @Test
    void firstIf_discriminatorAbsent_configDiffers_returnsNewInstance() throws Exception {
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).build();
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().build();
        JsonSchema schema = createSchemaWithMockContext(false, current);
        JsonSchema result = schema.withConfig(supplied);
        assertNotSame(schema, result, "New instance expected when config differs and no discriminator handling");
    }

    @Test
    void firstIf_discriminatorAbsent_configEquals_returnsSameInstance() throws Exception {
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).build();
        JsonSchema schema = createSchemaWithMockContext(false, supplied);
        JsonSchema result = schema.withConfig(supplied);
        assertSame(schema, result, "Same instance expected when config equals current and no discriminator");
    }

    @Test
    void firstIf_discriminatorPresent_enabledConfigDiffers_returnsNewInstance() throws Exception {
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
        // Current context config is default (both false)
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().build();
        JsonSchema schema = createSchemaWithMockContext(true, current);
        JsonSchema result = schema.withConfig(supplied);
        assertNotSame(schema, result, "When discriminator already enabled but supplied config differs, a new instance must be created");
    }

    @Test
    void firstIf_discriminatorPresent_enabledConfigEquals_returnsSameInstance() throws Exception {
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
        JsonSchema schema = createSchemaWithMockContext(true, supplied);
        JsonSchema result = schema.withConfig(supplied);
        assertSame(schema, result, "Same instance expected when discriminator present and supplied config equals current");
    }

    @Test
    void secondIf_configChangedAfterFirstIf_returnsNewInstance() throws Exception {
        // Context already has discriminator enabled, so first if will be skipped.
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
        // Supplied config does NOT have discriminator enabled.
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build();
        JsonSchema schema = createSchemaWithMockContext(true, current);
        JsonSchema result = schema.withConfig(supplied);
        assertNotSame(schema, result, "A new JsonSchema must be returned when supplied config differs");
        assertEquals(supplied, result.getValidationContext().getConfig(), "The new instance should contain the supplied configuration");
    }

    @Test
    void secondIf_configEqualsCurrent_returnsSameInstance() throws Exception {
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().typeLoose(true).build();
        JsonSchema schema = createSchemaWithMockContext(false, current);
        JsonSchema result = schema.withConfig(current);
        assertSame(schema, result, "When supplied config equals current, the same instance must be returned");
    }

    @Test
    void firstIf_trueRebuiltConfigEqualsCurrent_returnsSameInstance() throws Exception {
        // Current context already has discriminator & nullable enabled.
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
        // Supplied config has both flags false – triggers rebuilding.
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build();
        JsonSchema schema = createSchemaWithMockContext(true, current);
        JsonSchema result = schema.withConfig(supplied);
        assertSame(schema, result, "Rebuilt config equals existing config, therefore the original instance should be returned");
    }

    @Test
    void metaSchemaWithoutDiscriminator_configEnablesDiscriminator_returnsNewInstance() throws Exception {
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().build();
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
        JsonSchema schema = createSchemaWithMockContext(false, current);
        JsonSchema result = schema.withConfig(supplied);
        assertNotSame(schema, result, "When meta‑schema lacks discriminator but supplied config enables it, a new instance must be created");
    }

    @Test
    void metaSchemaWithoutDiscriminator_configEqualsCurrent_returnsSameInstance() throws Exception {
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
        JsonSchema schema = createSchemaWithMockContext(false, current);
        JsonSchema result = schema.withConfig(current);
        assertSame(schema, result, "If meta‑schema lacks discriminator and config already matches, the same instance is returned");
    }

    @Test
    void configDiffersOnlyInNullableFlag_returnsNewInstance() throws Exception {
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().nullableKeywordEnabled(false).build();
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().nullableKeywordEnabled(true).build();
        JsonSchema schema = createSchemaWithMockContext(false, current);
        JsonSchema result = schema.withConfig(supplied);
        assertNotSame(schema, result, "A change only in nullableKeywordEnabled must still cause a new instance");
    }

    @Test
    void configDiffersOnlyInNonDiscriminatorFlag_returnsNewInstance() throws Exception {
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().typeLoose(false).build();
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().typeLoose(true).build();
        JsonSchema schema = createSchemaWithMockContext(false, current);
        JsonSchema result = schema.withConfig(supplied);
        assertNotSame(schema, result, "A change in any flag other than discriminator must produce a new instance");
    }

    @Test
    void withConfig_nullConfig_throwsNPE() throws Exception {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        assertThrows(NullPointerException.class, () -> schema.withConfig(null), "Calling withConfig(null) must throw NullPointerException");
    }

    @Test
    void withConfig_nullValidationContext_throwsNPE() throws Exception {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        // Inject a null ValidationContext via reflection.
        Field f = BaseJsonValidator.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(schema, null);
        SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();
        assertThrows(NullPointerException.class, () -> schema.withConfig(cfg), "If the internal ValidationContext is null, withConfig should throw NPE");
    }

    @Test
    void withConfig_nullMetaSchema_throwsNPE() throws Exception {
        ValidationContext mockContext = mock(ValidationContext.class);
        when(mockContext.getMetaSchema()).thenReturn(null);
        when(mockContext.getConfig()).thenReturn(SchemaValidatorsConfig.builder().build());
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        injectValidationContext(schema, mockContext);
        SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();
        assertThrows(NullPointerException.class, () -> schema.withConfig(cfg), "Null MetaSchema should cause NPE inside withConfig");
    }

    @Test
    void withConfig_nullKeywordsMap_throwsNPE() throws Exception {
        MetaSchema mockMeta = mock(MetaSchema.class);
        when(mockMeta.getKeywords()).thenReturn(null);
        ValidationContext mockContext = mock(ValidationContext.class);
        when(mockContext.getMetaSchema()).thenReturn(mockMeta);
        when(mockContext.getConfig()).thenReturn(SchemaValidatorsConfig.builder().build());
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        injectValidationContext(schema, mockContext);
        SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();
        assertThrows(NullPointerException.class, () -> schema.withConfig(cfg), "Null keywords map should cause NPE");
    }

    @Test
    void withConfig_nullConfigInContext_throwsNPE() throws Exception {
        MetaSchema mockMeta = mock(MetaSchema.class);
        when(mockMeta.getKeywords()).thenReturn(Collections.emptyMap());
        ValidationContext mockContext = mock(ValidationContext.class);
        when(mockContext.getMetaSchema()).thenReturn(mockMeta);
        // <-- null config
        when(mockContext.getConfig()).thenReturn(null);
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        injectValidationContext(schema, mockContext);
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().build();
        assertThrows(NullPointerException.class, () -> schema.withConfig(supplied), "Null config inside ValidationContext should cause NPE when equals() is invoked");
    }

    @Test
    void withConfig_newValidationContextHasNullRequiredArgument_throwsNPE() throws Exception {
        // Context without discriminator, current config = default.
        ValidationContext mockContext = mock(ValidationContext.class);
        MetaSchema mockMeta = mock(MetaSchema.class);
        when(mockMeta.getKeywords()).thenReturn(Collections.emptyMap());
        when(mockContext.getMetaSchema()).thenReturn(mockMeta);
        when(mockContext.getConfig()).thenReturn(SchemaValidatorsConfig.builder().build());
        // Force the second if‑block to be true by supplying a different config.
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().typeLoose(true).build();
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        // Inject a context that will cause the ValidationContext constructor to receive
        // a null argument for one of its required fields (e.g., jsonSchemaFactory).
        // We achieve this by mocking getJsonSchemaFactory() to return null (already default).
        injectValidationContext(schema, mockContext);
        assertThrows(NullPointerException.class, () -> schema.withConfig(supplied), "If ValidationContext constructor receives null required arguments, NPE should be thrown");
    }
}
