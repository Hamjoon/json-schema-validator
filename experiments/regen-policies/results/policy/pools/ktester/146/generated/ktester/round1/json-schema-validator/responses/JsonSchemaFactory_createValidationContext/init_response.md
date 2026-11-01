```java
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

/**
 * Unit tests for {@link JsonSchemaFactory#createValidationContext(JsonNode, SchemaValidatorsConfig)}.
 *
 * The tests use reflection to invoke the protected method and Mockito to stub the
 * {@code getMetaSchemaOrDefault} call so that we can control whether the meta‑schema
 * contains the {@code "discriminator"} keyword.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createValidationContext_Test {

    private JsonSchemaFactory factory;          // real factory instance
    private JsonSchemaFactory spyFactory;       // spy that allows stubbing protected methods

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a standard factory (draft‑2020‑12) – it is thread‑safe and fully initialised.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Create a Mockito spy so we can stub the protected getMetaSchemaOrDefault method.
        spyFactory = Mockito.spy(factory);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for now
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required
    }

    /**
     * Helper that invokes the protected {@code createValidationContext} method via reflection.
     */
    private ValidationContext invokeCreateValidationContext(JsonSchemaFactory target,
                                                             JsonNode schemaNode,
                                                             SchemaValidatorsConfig config) throws Exception {
        Method method = JsonSchemaFactory.class
                .getDeclaredMethod("createValidationContext", JsonNode.class, SchemaValidatorsConfig.class);
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
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(false)
                .build();

        // AND – the meta‑schema does NOT contain the "discriminator" keyword
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getKeywords()).thenReturn(Collections.emptyMap());
        // Stub the protected method to return our mock meta‑schema
        doReturn(meta).when(spyFactory).getMetaSchemaOrDefault(any(JsonNode.class), any(SchemaValidatorsConfig.class));

        // WHEN
        ValidationContext ctx = invokeCreateValidationContext(spyFactory, schemaNode, originalConfig);
        SchemaValidatorsConfig resultingConfig = extractConfig(ctx);

        // THEN – the factory must return the *same* config instance (no auto‑enable)
        assertSame(originalConfig, resultingConfig,
                "When the meta‑schema lacks 'discriminator', the original config should be returned unchanged");
        assertFalse(resultingConfig.isDiscriminatorKeywordEnabled(),
                "Discriminator keyword should remain disabled");
        assertFalse(resultingConfig.isNullableKeywordEnabled(),
                "Nullable keyword should remain as provided");
    }

    @Test
    void createValidationContext_withDiscriminatorKeyword_enablesDiscriminatorAndNullable() throws Exception {
        // GIVEN – a schema node (the actual content is irrelevant)
        JsonNode schemaNode = factory.getObjectMapper(InputFormat.JSON).readTree("{\"type\":\"object\"}");

        // AND – a config where the discriminator keyword is disabled
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(false)
                .nullableKeywordEnabled(false)
                .build();

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
        assertNotSame(originalConfig, resultingConfig,
                "When the meta‑schema contains 'discriminator', a new config should be created");
        assertTrue(resultingConfig.isDiscriminatorKeywordEnabled(),
                "Discriminator keyword must be enabled automatically");
        assertTrue(resultingConfig.isNullableKeywordEnabled(),
                "Nullable keyword must be enabled automatically");
    }
}
```