package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createValidationContext_Test {

    // real factory instance
    private JsonSchemaFactory factory;

    // spy that allows stubbing protected methods
    private JsonSchemaFactory spyFactory;

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a standard factory (draft‑2020‑12) – it is thread‑safe and fully initialised.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Create a Mockito spy so we can stub the protected getMetaSchemaOrDefault method.
        spyFactory = Mockito.spy(factory);
        // Use a concrete factory (draft‑2020‑12). It is fully initialised and thread‑safe.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        spyFactory = Mockito.spy(factory);
    }

    /**
     * Helper that invokes the protected {@code createValidationContext} method via reflection.
     */
    private ValidationContext invokeCreateValidationContext(JsonSchemaFactory target, JsonNode schemaNode, SchemaValidatorsConfig config) throws Exception {
        Method method = JsonSchemaFactory.class.getDeclaredMethod("createValidationContext", JsonNode.class, SchemaValidatorsConfig.class);
        method.setAccessible(true);
        return (ValidationContext) method.invoke(target, schemaNode, config);
    }

    /**
     * Retrieves the {@code SchemaValidatorsConfig} stored inside a {@link ValidationContext}
     * using reflection (the field is package‑private in the library).
     */
    private SchemaValidatorsConfig extractConfig(ValidationContext ctx) throws Exception {
        Field cfgField = ValidationContext.class.getDeclaredField("config");
        cfgField.setAccessible(true);
        return (SchemaValidatorsConfig) cfgField.get(ctx);
    }

    @Test
    void createValidationContext_withoutDiscriminatorKeyword_returnsOriginalConfig() throws Exception {
        // GIVEN – a simple schema (content does not matter for this test)
        JsonNode schemaNode = factory.getObjectMapper(InputFormat.JSON).readTree("{\"type\":\"object\"}");
        // AND – a config where the discriminator keyword is explicitly disabled
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build();
        // AND – the meta‑schema does NOT contain the "discriminator" keyword
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getKeywords()).thenReturn(Collections.emptyMap());
        // Stub the protected method to return our mock meta‑schema
        doReturn(meta).when(spyFactory).getMetaSchemaOrDefault(any(JsonNode.class), any(SchemaValidatorsConfig.class));
        // WHEN
        ValidationContext ctx = invokeCreateValidationContext(spyFactory, schemaNode, originalConfig);
        SchemaValidatorsConfig resultingConfig = extractConfig(ctx);
        // THEN – the factory must return the *same* config instance (no auto‑enable)
        assertSame(originalConfig, resultingConfig, "When the meta‑schema lacks 'discriminator', the original config should be returned unchanged");
        assertFalse(resultingConfig.isDiscriminatorKeywordEnabled(), "Discriminator keyword should remain disabled");
        assertFalse(resultingConfig.isNullableKeywordEnabled(), "Nullable keyword should remain as provided");
    }

    @Test
    void createValidationContext_withDiscriminatorKeyword_enablesDiscriminatorAndNullable() throws Exception {
        // GIVEN – a schema node (the actual content is irrelevant)
        JsonNode schemaNode = factory.getObjectMapper(InputFormat.JSON).readTree("{\"type\":\"object\"}");
        // AND – a config where the discriminator keyword is disabled
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build();
        // AND – a meta‑schema whose keywords map contains the "discriminator" entry
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        Map<String, Object> keywordMap = Collections.singletonMap("discriminator", new Object());
        when(meta.getKeywords()).thenReturn(keywordMap);
        // Stub the protected method to return our mock meta‑schema
        doReturn(meta).when(spyFactory).getMetaSchemaOrDefault(any(JsonNode.class), any(SchemaValidatorsConfig.class));
        // WHEN
        ValidationContext ctx = invokeCreateValidationContext(spyFactory, schemaNode, originalConfig);
        SchemaValidatorsConfig resultingConfig = extractConfig(ctx);
        // THEN – a *new* config instance must be created with both flags enabled
        assertNotSame(originalConfig, resultingConfig, "When the meta‑schema contains 'discriminator', a new config should be created");
        assertTrue(resultingConfig.isDiscriminatorKeywordEnabled(), "Discriminator keyword must be enabled automatically");
        assertTrue(resultingConfig.isNullableKeywordEnabled(), "Nullable keyword must be enabled automatically");
    }

    @Test
    void discriminatorPresent_configAlreadyEnabled_returnsSameConfig() throws Exception {
        JsonNode schemaNode = factory.getObjectMapper(InputFormat.JSON).readTree("{\"type\":\"object\"}");
        // Config already has discriminator enabled
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(// nullable flag does not matter here
        false).build();
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getKeywords()).thenReturn(Collections.singletonMap("discriminator", new Object()));
        doReturn(meta).when(spyFactory).getMetaSchemaOrDefault(any(JsonNode.class), any(SchemaValidatorsConfig.class));
        ValidationContext ctx = invokeCreateValidationContext(spyFactory, schemaNode, originalConfig);
        SchemaValidatorsConfig resultingConfig = extractConfig(ctx);
        // Should be the exact same instance – no new config is built.
        assertSame(originalConfig, resultingConfig, "When discriminator is already enabled, the factory must keep the original config");
    }

    @Test
    void discriminatorPresent_configDisabled_nullableAlreadyTrue_preservesNullable() throws Exception {
        JsonNode schemaNode = factory.getObjectMapper(InputFormat.JSON).readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(// already true
        true).typeLoose(// default value, kept for later verification
        false).build();
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getKeywords()).thenReturn(Collections.singletonMap("discriminator", new Object()));
        doReturn(meta).when(spyFactory).getMetaSchemaOrDefault(any(JsonNode.class), any(SchemaValidatorsConfig.class));
        ValidationContext ctx = invokeCreateValidationContext(spyFactory, schemaNode, originalConfig);
        SchemaValidatorsConfig resultingConfig = extractConfig(ctx);
        assertNotSame(originalConfig, resultingConfig, "A new config instance must be created");
        assertTrue(resultingConfig.isDiscriminatorKeywordEnabled(), "Discriminator must be enabled");
        assertTrue(resultingConfig.isNullableKeywordEnabled(), "Nullable flag must stay true");
        // other flags must stay unchanged
        assertEquals(originalConfig.isTypeLoose(), resultingConfig.isTypeLoose(), "Other configuration flags must be preserved");
    }

    @Test
    void discriminatorPresent_configDisabled_otherFlagsPreserved() throws Exception {
        JsonNode schemaNode = factory.getObjectMapper(InputFormat.JSON).readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).typeLoose(true).formatKeywordEnabled(false).additionalProperties(false).build();
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getKeywords()).thenReturn(Collections.singletonMap("discriminator", new Object()));
        doReturn(meta).when(spyFactory).getMetaSchemaOrDefault(any(JsonNode.class), any(SchemaValidatorsConfig.class));
        ValidationContext ctx = invokeCreateValidationContext(spyFactory, schemaNode, originalConfig);
        SchemaValidatorsConfig resultingConfig = extractConfig(ctx);
        assertNotSame(originalConfig, resultingConfig, "A new config instance must be created");
        assertTrue(resultingConfig.isDiscriminatorKeywordEnabled(), "Discriminator must be enabled");
        assertTrue(resultingConfig.isNullableKeywordEnabled(), "Nullable must be enabled automatically");
        // Verify that the rest of the flags are unchanged
        assertEquals(originalConfig.isTypeLoose(), resultingConfig.isTypeLoose(), "typeLoose flag must be preserved");
        assertEquals(originalConfig.isFormatKeywordEnabled(), resultingConfig.isFormatKeywordEnabled(), "formatKeywordEnabled flag must be preserved");
        assertEquals(originalConfig.isAdditionalPropertiesAllowed(), resultingConfig.isAdditionalPropertiesAllowed(), "additionalProperties flag must be preserved");
    }

    @Test
    void discriminatorAbsent_configDiscriminatorEnabled_keepsOriginalConfig() throws Exception {
        JsonNode schemaNode = factory.getObjectMapper(InputFormat.JSON).readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(// user explicitly enabled
        true).nullableKeywordEnabled(false).build();
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        // meta‑schema without the discriminator keyword
        when(meta.getKeywords()).thenReturn(Collections.emptyMap());
        doReturn(meta).when(spyFactory).getMetaSchemaOrDefault(any(JsonNode.class), any(SchemaValidatorsConfig.class));
        ValidationContext ctx = invokeCreateValidationContext(spyFactory, schemaNode, originalConfig);
        SchemaValidatorsConfig resultingConfig = extractConfig(ctx);
        assertSame(originalConfig, resultingConfig, "When the meta‑schema lacks 'discriminator', the supplied config must be returned unchanged even if it enables the flag");
    }

    @Test
    void nullConfig_throwsNullPointerException() {
        JsonNode schemaNode = factory.getObjectMapper(InputFormat.JSON).readTree("{\"type\":\"object\"}");
        // No stubbing of getMetaSchemaOrDefault – the NPE occurs before its use.
        assertThrows(NullPointerException.class, () -> invokeCreateValidationContext(spyFactory, schemaNode, null), "Passing a null SchemaValidatorsConfig must result in NPE");
    }

    @Test
    void nullSchemaNode_throwsNullPointerException() {
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // Stub getMetaSchemaOrDefault to throw NPE when called with null (the real method would do that)
        doThrow(NullPointerException.class).when(spyFactory).getMetaSchemaOrDefault(isNull(), any(SchemaValidatorsConfig.class));
        assertThrows(NullPointerException.class, () -> invokeCreateValidationContext(spyFactory, null, config), "Passing a null JsonNode must result in NPE");
    }

    @Test
    void nonObjectSchemaNode_throwsIllegalArgumentException() {
        // Create an ArrayNode (non‑object)
        JsonNode arrayNode = factory.getObjectMapper(InputFormat.JSON).readTree("[1,2,3]");
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        // The stub will throw IllegalArgumentException when the node is not an object
        doAnswer(invocation -> {
            JsonNode node = invocation.getArgument(0);
            if (!node.isObject()) {
                throw new IllegalArgumentException("Schema node must be a JSON object");
            }
            return meta;
        }).when(spyFactory).getMetaSchemaOrDefault(any(JsonNode.class), any(SchemaValidatorsConfig.class));
        assertThrows(IllegalArgumentException.class, () -> invokeCreateValidationContext(spyFactory, arrayNode, config), "A non‑object schema node must cause IllegalArgumentException");
    }

    @Test
    void metaSchemaKeywordsMapNull_throwsNullPointerException() {
        JsonNode schemaNode = factory.getObjectMapper(InputFormat.JSON).readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build();
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        // Simulate a meta‑schema whose getKeywords() returns null
        when(meta.getKeywords()).thenReturn(null);
        doReturn(meta).when(spyFactory).getMetaSchemaOrDefault(any(JsonNode.class), any(SchemaValidatorsConfig.class));
        assertThrows(NullPointerException.class, () -> invokeCreateValidationContext(spyFactory, schemaNode, config), "If meta‑schema.getKeywords() returns null, a NPE is expected");
    }

    @Test
    void discriminatorPresent_configDisabled_verifyNewConfigInstance() throws Exception {
        JsonNode schemaNode = factory.getObjectMapper(InputFormat.JSON).readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).build();
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getKeywords()).thenReturn(Collections.singletonMap("discriminator", new Object()));
        doReturn(meta).when(spyFactory).getMetaSchemaOrDefault(any(JsonNode.class), any(SchemaValidatorsConfig.class));
        ValidationContext ctx = invokeCreateValidationContext(spyFactory, schemaNode, originalConfig);
        SchemaValidatorsConfig resultingConfig = extractConfig(ctx);
        assertNotSame(originalConfig, resultingConfig, "When discriminator needs to be enabled, a brand‑new config instance must be created");
        assertTrue(resultingConfig.isDiscriminatorKeywordEnabled(), "The new config must have discriminatorKeywordEnabled == true");
    }

    @Test
    void metaSchemaAbsent_returnsNullPointerException() {
        JsonNode schemaNode = factory.getObjectMapper(InputFormat.JSON).readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // Stub getMetaSchemaOrDefault to return null (simulating a missing meta‑schema)
        doReturn(null).when(spyFactory).getMetaSchemaOrDefault(any(JsonNode.class), any(SchemaValidatorsConfig.class));
        assertThrows(NullPointerException.class, () -> invokeCreateValidationContext(spyFactory, schemaNode, config), "When getMetaSchemaOrDefault returns null, a NPE should be thrown");
    }
}
