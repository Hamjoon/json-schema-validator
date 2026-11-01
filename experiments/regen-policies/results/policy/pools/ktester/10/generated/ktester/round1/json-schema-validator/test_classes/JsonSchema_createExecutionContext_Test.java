package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Locale;

@ExtendWith(MockitoExtension.class)
class JsonSchema_createExecutionContext_Test {

    // ----- collaborators ----------------------------------------------------
    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    // ----- object under test ------------------------------------------------
    JsonSchema jsonSchema;

    // ----- shared test data ------------------------------------------------
    SchemaValidatorsConfig config;

    @BeforeEach
    void setupBeforeEach() {
        // a real config instance – we can mutate it per test
        config = new SchemaValidatorsConfig();
        // stub the validationContext to return our config
        when(validationContext.getConfig()).thenReturn(config);
        // The method is accessible because the test resides in the same package.
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, // no parent
        null, // do not suppress sub‑schema retrieval
        false);
        // Build a JsonSchema instance using the package‑private factory method.
        // a fresh config for each test
        config = new SchemaValidatorsConfig();
        when(validationContext.getConfig()).thenReturn(config);
        // use the package‑private factory – the test resides in the same package
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        reset(validationContext, schemaLocation, evaluationPath, schemaNode);
        reset(validationContext, schemaLocation, evaluationPath, schemaNode);
    }

    @Test
    void createExecutionContext_withoutCustomizer_returnsContextWithConfig() {
        // Act
        ExecutionContext ec = jsonSchema.createExecutionContext();
        // Assert
        assertNotNull(ec, "ExecutionContext should not be null");
        assertSame(config, ec.getExecutionConfig(), "The ExecutionConfig inside the ExecutionContext must be the one from ValidationContext");
        // No customizer – ensure nothing was invoked
        verify(validationContext, times(1)).getConfig();
    }

    @Test
    void createExecutionContext_withCustomizer_invokesCustomizer() {
        // Arrange
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        config.setExecutionContextCustomizer(customizer);
        // Act
        ExecutionContext ec = jsonSchema.createExecutionContext();
        // Assert
        assertNotNull(ec, "ExecutionContext should not be null");
        // Verify that the customizer was called exactly once with the correct arguments
        verify(customizer, times(1)).customize(ec, validationContext);
    }

    @Test
    void createExecutionContext_customizerCanMutateContext() {
        // Arrange: a customizer that flips a flag inside the ExecutionConfig
        ExecutionContextCustomizer customizer = (executionContext, ctx) -> {
            executionContext.getExecutionConfig().setFormatAssertionsEnabled(!executionContext.getExecutionConfig().isFormatAssertionsEnabled());
        };
        config.setExecutionContextCustomizer(customizer);
        boolean original = config.isFormatAssertionsEnabled();
        // Act
        ExecutionContext ec = jsonSchema.createExecutionContext();
        // Assert
        assertNotNull(ec);
        assertEquals(!original, ec.getExecutionConfig().isFormatAssertionsEnabled(), "Customizer should have toggled the formatAssertionsEnabled flag");
    }

    // -----------------------------------------------------------------------
    @Test
    void createExecutionContext_additional_localeAndFormatFlag() {
        // Arrange: non‑default locale and disabled format assertions
        config.setLocale(Locale.GERMAN);
        config.setFormatAssertionsEnabled(false);
        // Act
        ExecutionContext ec = jsonSchema.createExecutionContext();
        // Assert
        assertNotNull(ec, "ExecutionContext must be created");
        ExecutionConfig execCfg = ec.getExecutionConfig();
        assertEquals(Locale.GERMAN, execCfg.getLocale(), "ExecutionConfig.locale should reflect the config value");
        assertFalse(execCfg.isFormatAssertionsEnabled(), "ExecutionConfig.formatAssertionsEnabled should reflect the config value");
        // No customizer – verify that none was set
        assertNull(config.getExecutionContextCustomizer(), "Customizer should be null for this test");
    }

    // -----------------------------------------------------------------------
    @Test
    void createExecutionContext_additional_unevaluatedFlags() {
        // Arrange: enable both disable‑flags
        config.setUnevaluatedItemsAnalysisDisabled(true);
        config.setUnevaluatedPropertiesAnalysisDisabled(true);
        // Act
        ExecutionContext ec = jsonSchema.createExecutionContext();
        // Assert
        assertNotNull(ec, "ExecutionContext must be created");
        CollectorContext collector = ec.getCollectorContext();
        // The CollectorContext API does not expose getters in the public source,
        // so we use reflection to verify the internal fields.
        try {
            java.lang.reflect.Field itemsField = CollectorContext.class.getDeclaredField("unevaluatedItemsAnalysisDisabled");
            java.lang.reflect.Field propsField = CollectorContext.class.getDeclaredField("unevaluatedPropertiesAnalysisDisabled");
            itemsField.setAccessible(true);
            propsField.setAccessible(true);
            boolean itemsDisabled = (boolean) itemsField.get(collector);
            boolean propsDisabled = (boolean) propsField.get(collector);
            assertTrue(itemsDisabled, "CollectorContext should have unevaluatedItemsAnalysisDisabled = true");
            assertTrue(propsDisabled, "CollectorContext should have unevaluatedPropertiesAnalysisDisabled = true");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unable to reflect CollectorContext fields: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    @Test
    void createExecutionContext_additional_customizerModifiesLocale() {
        // Arrange: customizer that forces French locale
        ExecutionContextCustomizer customizer = (executionContext, ctx) -> executionContext.getExecutionConfig().setLocale(Locale.FRENCH);
        config.setExecutionContextCustomizer(customizer);
        // Act
        ExecutionContext ec = jsonSchema.createExecutionContext();
        // Assert
        assertNotNull(ec);
        assertEquals(Locale.FRENCH, ec.getExecutionConfig().getLocale(), "Customizer should have overridden the locale to French");
        // Verify that the customizer was indeed invoked
        // (the lambda itself cannot be verified, so we rely on the side‑effect)
    }

    // -----------------------------------------------------------------------
    @Test
    void createExecutionContext_additional_customizerThrowsException() {
        // Arrange: customizer that throws
        ExecutionContextCustomizer exploding = (executionContext, ctx) -> {
            throw new RuntimeException("boom");
        };
        config.setExecutionContextCustomizer(exploding);
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.createExecutionContext(), "Exception from customizer must propagate");
        assertEquals("boom", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    @Test
    void createExecutionContext_nullValidationContext() throws Exception {
        // Arrange: create a JsonSchema instance and then null out its validationContext
        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        // Use reflection to set the protected field in BaseJsonValidator
        java.lang.reflect.Field vcField = BaseJsonValidator.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        vcField.set(schema, null);
        // Act & Assert
        assertThrows(NullPointerException.class, schema::createExecutionContext, "When validationContext is null a NullPointerException should be thrown");
    }

    // -----------------------------------------------------------------------
    @Test
    void createExecutionContext_nullConfig() {
        // Arrange: stub getConfig() to return null
        when(validationContext.getConfig()).thenReturn(null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> jsonSchema.createExecutionContext(), "When config is null a NullPointerException should be thrown");
    }

    // -----------------------------------------------------------------------
    @ParameterizedTest
    @ValueSource(strings = { "ENGLISH", "FRENCH", "GERMAN" })
    void createExecutionContext_variousLocales(String localeName) {
        Locale locale = Locale.valueOf(localeName);
        config.setLocale(locale);
        ExecutionContext ec = jsonSchema.createExecutionContext();
        assertEquals(locale, ec.getExecutionConfig().getLocale(), "Locale should be propagated correctly for " + localeName);
    }
}
