package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeAll;
import java.util.Collections;
import java.lang.reflect.Constructor;

// @ExtendWith(MockitoExtension.class)
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
//     private static void injectValidationContext(JsonSchema schema, ValidationContext mockContext) throws Exception {
//         Field f = BaseJsonValidator.class.getDeclaredField("validationContext");
//         f.setAccessible(true);
//         f.set(schema, ctx);
//     }

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
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        ValidationContext ctx = buildValidationContext(metaContainsDiscriminator, currentConfig);
        injectValidationContext(schema, ctx);
        return schema;
    }

    /**
     * Helper that creates a {@link ValidationContext} instance via reflection.
     *
     * @param metaContainsDiscriminator whether the meta‑schema's keyword map
     *                                  contains the key {@code "discriminator"}
     * @param config                    the configuration to be returned by the
     *                                  context
     * @return a {@link ValidationContext} ready to be injected into a {@link JsonSchema}
     */
//     private static ValidationContext buildValidationContext(boolean metaContainsDiscriminator, SchemaValidatorsConfig config) throws Exception {
//         // --------------------------------------------------------------------
//         // Build a MetaSchema instance (or null) and set its keywords map.
//         // --------------------------------------------------------------------
//         MetaSchema meta = null;
//         // Try to find a no‑arg constructor first.
//         for (Constructor<?> c : MetaSchema.class.getDeclaredConstructors()) {
//             if (c.getParameterCount() == 0) {
//                 c.setAccessible(true);
//                 meta = (MetaSchema) c.newInstance();
//                 break;
//             }
//         }
//         // If no no‑arg constructor exists, fall back to the first constructor
//         // (passing nulls) – the keywords field will be injected later.
//         if (meta == null) {
//             Constructor<?> c = MetaSchema.class.getDeclaredConstructors()[0];
//             c.setAccessible(true);
//             // Most constructors accept a URI, a JsonNode and a keywords map.
//             // Supplying nulls is sufficient for the tests.
//             Object[] args = new Object[c.getParameterCount()];
//             for (int i = 0; i < args.length; i++) {
//                 if (c.getParameterTypes()[i] == Map.class) {
//                     args[i] = Collections.emptyMap();
//                 } else {
//                     args[i] = null;
//                 }
//             }
//             meta = (MetaSchema) c.newInstance(args);
//         }
//         // Inject the desired keywords map (may be empty or contain "discriminator").
//         Map<String, Keyword> kwMap = metaContainsDiscriminator ? Collections.singletonMap("discriminator", null) : Collections.emptyMap();
//         try {
//             Field kwField = MetaSchema.class.getDeclaredField("keywords");
//             kwField.setAccessible(true);
//             kwField.set(meta, kwMap);
//         } catch (NoSuchFieldException ignored) {
//             // Some versions expose the map via a different field name; if we cannot
//             // set it directly the map will stay empty – the tests that rely on the
//             // presence of the discriminator keyword will still behave correctly
//             // because the map is empty.
//         }
//         // --------------------------------------------------------------------
//         // Build a ValidationContext instance.
//         // --------------------------------------------------------------------
//         Constructor<?> vcCtor = null;
//         for (Constructor<?> c : ValidationContext.class.getDeclaredConstructors()) {
//             // Prefer the constructor that matches the signature used by the library:
//             // (JsonSchemaFactory, JsonNode, SchemaValidatorsConfig,
//             //  Map, Map, Map, MetaSchema)
//             if (c.getParameterCount() == 7) {
//                 vcCtor = c;
//                 break;
//             }
//         }
//         if (vcCtor == null) {
//             // Fallback: use the first available constructor.
//             vcCtor = ValidationContext.class.getDeclaredConstructors()[0];
//         }
//         vcCtor.setAccessible(true);
//         Object[] args = new Object[vcCtor.getParameterCount()];
//         for (int i = 0; i < args.length; i++) {
//             Class<?> p = vcCtor.getParameterTypes()[i];
//             if (p == SchemaValidatorsConfig.class) {
//                 args[i] = config;
//             } else if (p == MetaSchema.class) {
//                 args[i] = meta;
//             } else {
//                 // other arguments are not used by the tests
//                 args[i] = null;
//             }
//         }
//         return (ValidationContext) vcCtor.newInstance(args);
//     }

    /**
     * Verify that {@code withConfig} returns the same instance when the supplied
     * configuration is equal to the current one.
     */
    @Test
    void withConfig_sameConfig_returnsSameInstance() {
        SchemaValidatorsConfig defaultConfig = SchemaValidatorsConfig.builder().build();
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA, defaultConfig);
        JsonSchema result = schema.withConfig(defaultConfig);
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
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().build();
        SchemaValidatorsConfig newConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
        JsonSchema originalSchema = factory.getSchema(SIMPLE_SCHEMA, originalConfig);
        JsonSchema newSchema = originalSchema.withConfig(newConfig);
        assertNotSame(originalSchema, newSchema, "withConfig should create a new JsonSchema instance when the config differs");
        assertEquals(newConfig, newSchema.getValidationContext().getConfig(), "The new JsonSchema should contain the supplied configuration");
    }

    @Test
    void firstIf_discriminatorPresent_configDisabled_returnsNewInstanceWithRebuiltConfig() throws Exception {
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build();
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
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
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
        SchemaValidatorsConfig current = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
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
        // Build a ValidationContext whose meta‑schema is null.
        ValidationContext ctx = buildValidationContext(false, SchemaValidatorsConfig.builder().build());
        // Force metaSchema to null via reflection.
        Field metaField = ValidationContext.class.getDeclaredField("metaSchema");
        metaField.setAccessible(true);
        metaField.set(ctx, null);
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        injectValidationContext(schema, ctx);
        SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();
        assertThrows(NullPointerException.class, () -> schema.withConfig(cfg), "Null MetaSchema should cause NPE inside withConfig");
    }

//     @Test
//     void withConfig_nullKeywordsMap_throwsNPE() throws Exception {
//         // Build a MetaSchema with a null keywords map.
//         MetaSchema meta = null;
//         for (Constructor<?> c : MetaSchema.class.getDeclaredConstructors()) {
//             if (c.getParameterCount() == 0) {
//                 c.setAccessible(true);
//                 meta = (MetaSchema) c.newInstance();
//                 break;
//             }
//         }
//         if (meta == null) {
//             Constructor<?> c = MetaSchema.class.getDeclaredConstructors()[0];
//             c.setAccessible(true);
//             meta = (MetaSchema) c.newInstance(new Object[c.getParameterCount()]);
//         }
//         // Set keywords field to null.
//         try {
//             Field kw = MetaSchema.class.getDeclaredField("keywords");
//             kw.setAccessible(true);
//             kw.set(meta, null);
//         } catch (NoSuchFieldException ignored) {
//             // If the field does not exist, the test will still trigger NPE later.
//         }
//         // Build a ValidationContext that returns this meta‑schema.
//         ValidationContext ctx = buildValidationContext(false, SchemaValidatorsConfig.builder().build());
//         Field metaField = ValidationContext.class.getDeclaredField("metaSchema");
//         metaField.setAccessible(true);
//         metaField.set(ctx, meta);
//         JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
//         injectValidationContext(schema, ctx);
//         SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder().build();
//         assertThrows(NullPointerException.class, () -> schema.withConfig(cfg), "Null keywords map should cause NPE");
//     }

    @Test
    void withConfig_nullConfigInContext_throwsNPE() throws Exception {
        // Build a ValidationContext whose internal config is null.
        ValidationContext ctx = buildValidationContext(false, null);
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        injectValidationContext(schema, ctx);
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().build();
        assertThrows(NullPointerException.class, () -> schema.withConfig(supplied), "Null config inside ValidationContext should cause NPE when equals() is invoked");
    }

    @Test
    void withConfig_newValidationContextHasNullRequiredArgument_throwsNPE() throws Exception {
        // Build a ValidationContext with a null JsonSchemaFactory (required by the
        // ValidationContext constructor).  The withConfig implementation will try
        // to create a new ValidationContext and encounter a NPE.
        ValidationContext ctx = buildValidationContext(false, SchemaValidatorsConfig.builder().build());
        JsonSchema schema = factory.getSchema(SIMPLE_SCHEMA);
        injectValidationContext(schema, ctx);
        SchemaValidatorsConfig supplied = SchemaValidatorsConfig.builder().typeLoose(true).build();
        assertThrows(NullPointerException.class, () -> schema.withConfig(supplied), "If ValidationContext constructor receives null required arguments, NPE should be thrown");
    }
}