
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Collections;

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
        // The real constructor checks for null and throws NPE – we assert that.
        injectValidationContext(schema, mockContext);
        assertThrows(NullPointerException.class, () -> schema.withConfig(supplied), "If ValidationContext constructor receives null required arguments, NPE should be thrown");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:5: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:14: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:58: error: cannot find symbol
        ValidationContext mockContext = mock(ValidationContext.class);
                                        ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:59: error: cannot find symbol
        MetaSchema mockMeta = mock(MetaSchema.class);
        ^
  symbol:   class MetaSchema
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:59: error: cannot find symbol
        MetaSchema mockMeta = mock(MetaSchema.class);
                                   ^
  symbol:   class MetaSchema
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:60: error: cannot find symbol
        Map<String, Keyword> keywords = metaContainsDiscriminator ? Collections.singletonMap("discriminator", mock(Keyword.class)) : Collections.emptyMap();
                                                                                                              ^
  symbol:   method mock(Class<Keyword>)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:61: error: cannot find symbol
        when(mockContext.getMetaSchema()).thenReturn(mockMeta);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:63: error: cannot find symbol
        when(mockContext.getConfig()).thenReturn(currentConfig);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:66: error: cannot find symbol
        when(mockContext.getJsonSchemaFactory()).thenReturn(null);
        ^
  symbol:   method when(JsonSchemaFactory)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:67: error: cannot find symbol
        when(mockContext.getSchemaReferences()).thenReturn(null);
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:68: error: cannot find symbol
        when(mockContext.getSchemaResources()).thenReturn(null);
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:69: error: cannot find symbol
        when(mockContext.getDynamicAnchors()).thenReturn(null);
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:247: error: cannot find symbol
        ValidationContext mockContext = mock(ValidationContext.class);
                                        ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:248: error: cannot find symbol
        when(mockContext.getMetaSchema()).thenReturn(null);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:249: error: cannot find symbol
        when(mockContext.getConfig()).thenReturn(SchemaValidatorsConfig.builder().build());
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:258: error: cannot find symbol
        MetaSchema mockMeta = mock(MetaSchema.class);
        ^
  symbol:   class MetaSchema
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:258: error: cannot find symbol
        MetaSchema mockMeta = mock(MetaSchema.class);
                                   ^
  symbol:   class MetaSchema
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:260: error: cannot find symbol
        ValidationContext mockContext = mock(ValidationContext.class);
                                        ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:261: error: cannot find symbol
        when(mockContext.getMetaSchema()).thenReturn(mockMeta);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:262: error: cannot find symbol
        when(mockContext.getConfig()).thenReturn(SchemaValidatorsConfig.builder().build());
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:271: error: cannot find symbol
        MetaSchema mockMeta = mock(MetaSchema.class);
        ^
  symbol:   class MetaSchema
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:271: error: cannot find symbol
        MetaSchema mockMeta = mock(MetaSchema.class);
                                   ^
  symbol:   class MetaSchema
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:273: error: cannot find symbol
        ValidationContext mockContext = mock(ValidationContext.class);
                                        ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:274: error: cannot find symbol
        when(mockContext.getMetaSchema()).thenReturn(mockMeta);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:276: error: cannot find symbol
        when(mockContext.getConfig()).thenReturn(null);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:286: error: cannot find symbol
        ValidationContext mockContext = mock(ValidationContext.class);
                                        ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:287: error: cannot find symbol
        MetaSchema mockMeta = mock(MetaSchema.class);
        ^
  symbol:   class MetaSchema
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:287: error: cannot find symbol
        MetaSchema mockMeta = mock(MetaSchema.class);
                                   ^
  symbol:   class MetaSchema
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:289: error: cannot find symbol
        when(mockContext.getMetaSchema()).thenReturn(mockMeta);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_withConfig_Test
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:290: error: cannot find symbol
        when(mockContext.getConfig()).thenReturn(SchemaValidatorsConfig.builder().build());
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_withConfig_Test
30 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.SchemaValidatorsConfig`:\n\t\nclass `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.parentSchema;\n\t\tcom.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.evaluationParentSchema;\n\t\tcom.networknt.schema.Keyword com.networknt.schema.ValidationMessageHandler.keyword;\n\t\tcom.networknt.schema.ErrorMessageType com.networknt.schema.ValidationMessageHandler.errorMessageType;\n\t\tcom.networknt.schema.i18n.MessageSource com.networknt.schema.ValidationMessageHandler.messageSource;\n\t\tcom.networknt.schema.JsonNodePath com.networknt.schema.ValidationMessageHandler.evaluationPath;\n\t\tcom.networknt.schema.SchemaLocation com.networknt.schema.ValidationMessageHandler.schemaLocation;\n\t\tjava.lang.String com.networknt.schema.ValidationMessageHandler.errorMessageKeyword;\n\t\tjava.util.Map<java.lang.String, java.lang.String> com.networknt.schema.ValidationMessageHandler.errorMessage;\n\trelated functions:\n\t\tmethod `protected MessageSourceValidationMessage.Builder message()` returns `com.networknt.schema.MessageSourceValidationMessage.Builder`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;\nclass `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tboolean com.networknt.schema.JsonSchema.recursiveAnchor;\n\t\tjava.lang.String com.networknt.schema.JsonSchema.id;\n\trelated functions:\n\t\tmethod `public JsonSchema getRefSchema(JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;\n\t\tmethod `public JsonSchema fromRef(JsonSchema, JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`, api document: \n     * Creates a schema using the current one as a template with the parent as the\n     * ref.\n     * <p>\n     * This is typically used if this schema is a schema resource that can be\n     * pointed to by various references.\n     *\n     * @param refEvaluationParentSchema the parent ref\n     * @param refEvaluationPath the ref evaluation path\n     * @return the schema\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tboolean com.networknt.schema.BaseJsonValidator.suppressSubSchemaRetrieval;\n\t\tcom.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;\n\t\nclass `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `private ValidationContext withMetaSchema(ValidationContext, JsonNode)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`, api document: \n     * Determines the validation context to use for the schema given the parent\n     * validation context.\n     * <p>\n     * This is typically the same validation context unless the schema has a\n     * different $schema from the parent.\n     * <p>\n     * If the schema does not define a $schema, the parent should be used.\n     * \n     * @param validationContext the parent validation context\n     * @param schemaNode        the schema node\n     * @return the validation context to use\n     ;\n\t\tmethod `protected ValidationContext createValidationContext(JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static void copySchemaResources(ValidationContext, JsonSchema)` returns `void`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;\nclass `com.networknt.schema.UnevaluatedPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


