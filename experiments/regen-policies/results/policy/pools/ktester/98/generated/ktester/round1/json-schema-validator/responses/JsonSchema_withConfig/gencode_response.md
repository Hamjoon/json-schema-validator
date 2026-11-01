```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#withConfig(SchemaValidatorsConfig)}.
 *
 * <p>The tests exercise every logical branch described in the specification
 * JSON.  Where the public API does not allow direct injection of a
 * {@link ValidationContext}, reflection is used to replace the internal
 * {@code validationContext} field with a Mockito mock.</p>
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
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /**
     * Replaces the {@code validationContext} field of the supplied {@link JsonSchema}
     * with the given mock.  The field lives in {@link BaseJsonValidator}, so we
     * have to reach into that superclass via reflection.
     */
    private static void injectValidationContext(JsonSchema schema,
                                                ValidationContext mockContext) throws Exception {
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
    private JsonSchema createSchemaWithMockContext(boolean metaContainsDiscriminator,
                                                   SchemaValidatorsConfig currentConfig) throws Exception {
        // Create a real schema first – we only need a concrete instance.
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);

        // Mock the ValidationContext hierarchy.
        ValidationContext mockContext = mock(ValidationContext.class);
        MetaSchema mockMeta = mock(MetaSchema.class);
        Map<String, Keyword> keywords = metaContainsDiscriminator
                ? Collections.singletonMap("discriminator", mock(Keyword.class))
                : Collections.emptyMap();

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

    /* --------------------------------------------------------------------- */
    /* Existing simple cases (already in the starter file)                  */
    /* --------------------------------------------------------------------- */

    @Test
    void withConfig_sameConfig_returnsSameInstance() {
        SchemaValidatorsConfig defaultConfig = SchemaValidatorsConfig.builder().build();
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA, defaultConfig);
        JsonSchema result = schema.withConfig(defaultConfig);
        assertSame(schema, result,
                "withConfig should return the same instance when the config is unchanged");
    }

    @Test
    void withConfig_differentConfig_returnsNewInstance() {
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().build();
        SchemaValidatorsConfig newConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .build();

        JsonSchema originalSchema = factory.getSchema(SIMPLE_SCHEMA, originalConfig);
        JsonSchema newSchema = originalSchema.withConfig(newConfig);

        assertNotSame(originalSchema, newSchema,
                "withConfig should create a new JsonSchema instance when the config differs");
        assertEquals(newConfig, newSchema.getValidationContext().getConfig(),
                "The new JsonSchema should contain the supplied configuration");
    }

    /* --------------------------------------------------------------------- */
    /* 1. First if condition true – discriminator present but disabled       */
    /* --------------------------------------------------------------------- */

    @Test
    void firstIf_discriminatorPresent_configDisabled_returnsNewInstanceWithRebuiltConfig()
            throws Exception {
        // Context contains "discriminator"
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(false)
                .build();

        // The current context config is the default (both false)
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().build();

        JsonSchema schema = createSchemaWithMockContext(true, current);

        JsonSchema result = schema.withConfig(supplied);

        assertNotSame(schema, result, "A new instance must be created because the rebuilt config differs");
        SchemaValidatorsConfig usedConfig = result.getValidationContext().getConfig();
        assertTrue(usedConfig.isDiscriminatorKeywordEnabled(),
                "Rebuilt config should have discriminatorKeywordEnabled = true");
        assertTrue(usedConfig.isNullableKeywordEnabled(),
                "Rebuilt config should have nullableKeywordEnabled = true");
    }

    /* --------------------------------------------------------------------- */
    /* 2. First if condition false – discriminator absent                    */
    /* --------------------------------------------------------------------- */

    @Test
    void firstIf_discriminatorAbsent_configDiffers_returnsNewInstance()
            throws Exception {
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .build();
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().build();

        JsonSchema schema = createSchemaWithMockContext(false, current);
        JsonSchema result = schema.withConfig(supplied);

        assertNotSame(schema, result, "New instance expected when config differs and no discriminator handling");
    }

    @Test
    void firstIf_discriminatorAbsent_configEquals_returnsSameInstance()
            throws Exception {
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .build();

        JsonSchema schema = createSchemaWithMockContext(false, supplied);
        JsonSchema result = schema.withConfig(supplied);
        assertSame(schema, result, "Same instance expected when config equals current and no discriminator");
    }

    /* --------------------------------------------------------------------- */
    /* 3. First if condition false – discriminator present & already enabled */
    /* --------------------------------------------------------------------- */

    @Test
    void firstIf_discriminatorPresent_enabledConfigDiffers_returnsNewInstance()
            throws Exception {
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .nullableKeywordEnabled(true)
                .build();

        // Current context config is default (both false)
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().build();

        JsonSchema schema = createSchemaWithMockContext(true, current);
        JsonSchema result = schema.withConfig(supplied);

        assertNotSame(schema, result,
                "When discriminator already enabled but supplied config differs, a new instance must be created");
    }

    @Test
    void firstIf_discriminatorPresent_enabledConfigEquals_returnsSameInstance()
            throws Exception {
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .nullableKeywordEnabled(true)
                .build();

        JsonSchema schema = createSchemaWithMockContext(true, supplied);
        JsonSchema result = schema.withConfig(supplied);
        assertSame(schema, result,
                "Same instance expected when discriminator present and supplied config equals current");
    }

    /* --------------------------------------------------------------------- */
    /* 4. Second if condition true – config changed after first if           */
    /* --------------------------------------------------------------------- */

    @Test
    void secondIf_configChangedAfterFirstIf_returnsNewInstance()
            throws Exception {
        // Context already has discriminator enabled, so first if will be skipped.
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .nullableKeywordEnabled(true)
                .build();

        // Supplied config does NOT have discriminator enabled.
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(false)
                .build();

        JsonSchema schema = createSchemaWithMockContext(true, current);
        JsonSchema result = schema.withConfig(supplied);

        assertNotSame(schema, result, "A new JsonSchema must be returned when supplied config differs");
        assertEquals(supplied, result.getValidationContext().getConfig(),
                "The new instance should contain the supplied configuration");
    }

    /* --------------------------------------------------------------------- */
    /* 5. Second if condition false – supplied config equals current         */
    /* --------------------------------------------------------------------- */

    @Test
    void secondIf_configEqualsCurrent_returnsSameInstance()
            throws Exception {
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder()
                .typeLoose(true)
                .build();

        JsonSchema schema = createSchemaWithMockContext(false, current);
        JsonSchema result = schema.withConfig(current);
        assertSame(schema, result, "When supplied config equals current, the same instance must be returned");
    }

    /* --------------------------------------------------------------------- */
    /* 6. First if true – rebuilt config equals current (no new instance)    */
    /* --------------------------------------------------------------------- */

    @Test
    void firstIf_trueRebuiltConfigEqualsCurrent_returnsSameInstance()
            throws Exception {
        // Current context already has discriminator & nullable enabled.
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .nullableKeywordEnabled(true)
                .build();

        // Supplied config has both flags false – triggers rebuilding.
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(false)
                .build();

        JsonSchema schema = createSchemaWithMockContext(true, current);
        JsonSchema result = schema.withConfig(supplied);
        assertSame(schema, result,
                "Rebuilt config equals existing config, therefore the original instance should be returned");
    }

    /* --------------------------------------------------------------------- */
    /* 7. Meta‑schema without discriminator – config enables discriminator   */
    /* --------------------------------------------------------------------- */

    @Test
    void metaSchemaWithoutDiscriminator_configEnablesDiscriminator_returnsNewInstance()
            throws Exception {
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().build();
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .build();

        JsonSchema schema = createSchemaWithMockContext(false, current);
        JsonSchema result = schema.withConfig(supplied);
        assertNotSame(schema, result,
                "When meta‑schema lacks discriminator but supplied config enables it, a new instance must be created");
    }

    @Test
    void metaSchemaWithoutDiscriminator_configEqualsCurrent_returnsSameInstance()
            throws Exception {
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true)
                .build();

        JsonSchema schema = createSchemaWithMockContext(false, current);
        JsonSchema result = schema.withConfig(current);
        assertSame(schema, result,
                "If meta‑schema lacks discriminator and config already matches, the same instance is returned");
    }

    /* --------------------------------------------------------------------- */
    /* 8. Config differs only in nullableKeywordEnabled flag                */
    /* --------------------------------------------------------------------- */

    @Test
    void configDiffersOnlyInNullableFlag_returnsNewInstance()
            throws Exception {
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder()
                .nullableKeywordEnabled(false)
                .build();

        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder()
                .nullableKeywordEnabled(true)
                .build();

        JsonSchema schema = createSchemaWithMockContext(false, current);
        JsonSchema result = schema.withConfig(supplied);
        assertNotSame(schema, result,
                "A change only in nullableKeywordEnabled must still cause a new instance");
    }

    /* --------------------------------------------------------------------- */
    /* 9. Config differs only in a non‑discriminator flag (typeLoose)       */
    /* --------------------------------------------------------------------- */

    @Test
    void configDiffersOnlyInNonDiscriminatorFlag_returnsNewInstance()
            throws Exception {
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder()
                .typeLoose(false)
                .build();

        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder()
                .typeLoose(true)
                .build();

        JsonSchema schema = createSchemaWithMockContext(false, current);
        JsonSchema result = schema.withConfig(supplied);
        assertNotSame(schema, result,
                "A change in any flag other than discriminator must produce a new instance");
    }

    /* --------------------------------------------------------------------- */
    /* 10. Null config argument – NullPointerException                      */
    /* --------------------------------------------------------------------- */

    @Test
    void withConfig_nullConfig_throwsNPE() throws Exception {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        assertThrows(NullPointerException.class, () -> schema.withConfig(null),
                "Calling withConfig(null) must throw NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* 11. Null ValidationContext – NullPointerException                    */
    /* --------------------------------------------------------------------- */

    @Test
    void withConfig_nullValidationContext_throwsNPE() throws Exception {
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        // Inject a null ValidationContext via reflection.
        Field f = BaseJsonValidator.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(schema, null);

        SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();
        assertThrows(NullPointerException.class, () -> schema.withConfig(cfg),
                "If the internal ValidationContext is null, withConfig should throw NPE");
    }

    /* --------------------------------------------------------------------- */
    /* 12. Null MetaSchema in ValidationContext – NullPointerException      */
    /* --------------------------------------------------------------------- */

    @Test
    void withConfig_nullMetaSchema_throwsNPE() throws Exception {
        ValidationContext mockContext = mock(ValidationContext.class);
        when(mockContext.getMetaSchema()).thenReturn(null);
        when(mockContext.getConfig()).thenReturn(SchemaValidatorsConfig.builder().build());

        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        injectValidationContext(schema, mockContext);

        SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();
        assertThrows(NullPointerException.class, () -> schema.withConfig(cfg),
                "Null MetaSchema should cause NPE inside withConfig");
    }

    /* --------------------------------------------------------------------- */
    /* 13. Null Keywords map in MetaSchema – NullPointerException            */
    /* --------------------------------------------------------------------- */

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
        assertThrows(NullPointerException.class, () -> schema.withConfig(cfg),
                "Null keywords map should cause NPE");
    }

    /* --------------------------------------------------------------------- */
    /* 14. Null Config in ValidationContext – NullPointerException          */
    /* --------------------------------------------------------------------- */

    @Test
    void withConfig_nullConfigInContext_throwsNPE() throws Exception {
        MetaSchema mockMeta = mock(MetaSchema.class);
        when(mockMeta.getKeywords()).thenReturn(Collections.emptyMap());

        ValidationContext mockContext = mock(ValidationContext.class);
        when(mockContext.getMetaSchema()).thenReturn(mockMeta);
        when(mockContext.getConfig()).thenReturn(null); // <-- null config

        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        injectValidationContext(schema, mockContext);

        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().build();
        assertThrows(NullPointerException.class, () -> schema.withConfig(supplied),
                "Null config inside ValidationContext should cause NPE when equals() is invoked");
    }

    /* --------------------------------------------------------------------- */
    /* 15. ValidationContext constructor receives null arguments – NPE        */
    /* --------------------------------------------------------------------- */

    @Test
    void withConfig_newValidationContextHasNullRequiredArgument_throwsNPE()
            throws Exception {
        // Context without discriminator, current config = default.
        ValidationContext mockContext = mock(ValidationContext.class);
        MetaSchema mockMeta = mock(MetaSchema.class);
        when(mockMeta.getKeywords()).thenReturn(Collections.emptyMap());
        when(mockContext.getMetaSchema()).thenReturn(mockMeta);
        when(mockContext.getConfig()).thenReturn(SchemaValidatorsConfig.builder().build());

        // Force the second if‑block to be true by supplying a different config.
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder()
                .typeLoose(true)
                .build();

        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        // Inject a context that will cause the ValidationContext constructor to receive
        // a null argument for one of its required fields (e.g., jsonSchemaFactory).
        // We achieve this by mocking getJsonSchemaFactory() to return null (already default).
        // The real constructor checks for null and throws NPE – we assert that.
        injectValidationContext(schema, mockContext);

        assertThrows(NullPointerException.class, () -> schema.withConfig(supplied),
                "If ValidationContext constructor receives null required arguments, NPE should be thrown");
    }
}
```