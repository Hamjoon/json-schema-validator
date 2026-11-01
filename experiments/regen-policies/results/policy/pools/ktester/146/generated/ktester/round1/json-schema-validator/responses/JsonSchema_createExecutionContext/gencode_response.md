```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#createExecutionContext()}.
 *
 * The tests verify that the {@link ExecutionContext} returned by the method
 * contains the configuration values taken from the {@link SchemaValidatorsConfig}
 * held by the {@link ValidationContext}, and that an optional
 * {@link ExecutionContextCustomizer} is invoked correctly.
 *
 * Additional edge‑case tests cover null handling and exception propagation.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_createExecutionContext_Test {

    /** Mocked ValidationContext that will be injected into the JsonSchema instance. */
    @Mock
    ValidationContext validationContext;

    /** The JsonSchema instance under test. */
    JsonSchema jsonSchema;

    /** A reusable configuration object used in the tests. */
    SchemaValidatorsConfig config;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Prepare a default configuration.
        config = new SchemaValidatorsConfig();
        config.setLocale(Locale.US);
        config.setFormatAssertionsEnabled(true);
        config.setFailFast(false);
        config.setExecutionContextCustomizer(null);

        // Stub the ValidationContext to return the configuration.
        when(validationContext.getConfig()).thenReturn(config);

        // Create a JsonSchema instance via reflection (its constructor is private).
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                com.fasterxml.jackson.databind.JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        jsonSchema = ctor.newInstance(
                validationContext,
                null,                     // SchemaLocation – not needed for this test
                null,                     // JsonNodePath – not needed for this test
                NullNode.instance,        // schemaNode – a harmless placeholder
                null,                     // parent schema
                false);                   // suppressSubSchemaRetrieval
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /** Verify that the ExecutionContext reflects the values from the configuration. */
    @Test
    void createExecutionContext_copiesConfigValues() {
        ExecutionContext ctx = jsonSchema.createExecutionContext();

        assertNotNull(ctx, "ExecutionContext must not be null");

        ExecutionConfig execConfig = ctx.getExecutionConfig();

        assertEquals(config.getLocale(), execConfig.getLocale(),
                "Locale should be copied from the config");
        assertEquals(config.getFormatAssertionsEnabled(), execConfig.isFormatAssertionsEnabled(),
                "formatAssertionsEnabled should be copied from the config");
        assertEquals(config.isFailFast(), execConfig.isFailFast(),
                "failFast should be copied from the config");
    }

    /** Verify that a customizer, when present, is invoked with the created context. */
    @Test
    void createExecutionContext_invokesCustomizerIfPresent() {
        // Arrange – add a mock customizer to the configuration.
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        config.setExecutionContextCustomizer(customizer);

        // Act
        ExecutionContext ctx = jsonSchema.createExecutionContext();

        // Assert – the customizer must have been called exactly once.
        verify(customizer, times(1)).customize(eq(ctx), eq(validationContext));
    }

    /** Verify that a specific locale (e.g., FRANCE) is copied correctly. */
    @Test
    void createExecutionContext_copiesLocale() {
        config.setLocale(Locale.FRANCE);

        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertEquals(Locale.FRANCE, ctx.getExecutionConfig().getLocale(),
                "The locale from the config must be copied to the ExecutionConfig");
    }

    /** Verify that the formatAssertionsEnabled flag is transferred unchanged. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createExecutionContext_copiesFormatAssertionsEnabled(boolean flag) {
        config.setFormatAssertionsEnabled(flag);

        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertEquals(flag, ctx.getExecutionConfig().isFormatAssertionsEnabled(),
                "formatAssertionsEnabled flag should be copied unchanged");
    }

    /** Verify that the failFast flag is transferred unchanged. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createExecutionContext_copiesFailFast(boolean flag) {
        config.setFailFast(flag);

        ExecutionContext ctx = jsonSchema.createExecutionContext();
        assertEquals(flag, ctx.getExecutionConfig().isFailFast(),
                "failFast flag should be copied unchanged");
    }

    /** Two successive calls must return distinct ExecutionContext instances. */
    @Test
    void createExecutionContext_returnsNewInstanceEachCall() {
        ExecutionContext ctx1 = jsonSchema.createExecutionContext();
        ExecutionContext ctx2 = jsonSchema.createExecutionContext();

        assertNotSame(ctx1, ctx2, "Each call should produce a fresh ExecutionContext instance");
    }

    /** Customizer may modify the ExecutionConfig – the modifications must be retained. */
    @Test
    void createExecutionContext_customizerModifiesExecutionConfig() {
        // Arrange – a customizer that changes the locale to GERMANY.
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        doAnswer(invocation -> {
            ExecutionContext ec = invocation.getArgument(0);
            ec.getExecutionConfig().setLocale(Locale.GERMANY);
            return null;
        }).when(customizer).customize(any(ExecutionContext.class), any(ValidationContext.class));
        config.setExecutionContextCustomizer(customizer);

        // Act
        ExecutionContext ctx = jsonSchema.createExecutionContext();

        // Assert – the locale was changed by the customizer.
        assertEquals(Locale.GERMANY, ctx.getExecutionConfig().getLocale(),
                "Customizer should be able to modify the ExecutionConfig");
        verify(customizer, times(1)).customize(eq(ctx), eq(validationContext));
    }

    /** Customizer receives the original ValidationContext instance. */
    @Test
    void createExecutionContext_customizerReceivesCorrectValidationContext() {
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        config.setExecutionContextCustomizer(customizer);

        ExecutionContext ctx = jsonSchema.createExecutionContext();

        // Verify that the same ValidationContext mock is passed to the customizer.
        verify(customizer, times(1)).customize(eq(ctx), eq(validationContext));
    }

    /** When the internal ValidationContext is null, a NullPointerException is expected. */
    @Test
    void createExecutionContext_whenValidationContextIsNull_throwsNPE() throws Exception {
        // Use reflection to set the protected field 'validationContext' in BaseJsonValidator to null.
        Field field = BaseJsonValidator.class.getDeclaredField("validationContext");
        field.setAccessible(true);
        field.set(jsonSchema, null);

        assertThrows(NullPointerException.class,
                () -> jsonSchema.createExecutionContext(),
                "Calling createExecutionContext with a null ValidationContext should throw NPE");
    }

    /** When the ValidationContext returns a null config, a NullPointerException is expected. */
    @Test
    void createExecutionContext_whenConfigIsNull_throwsNPE() {
        when(validationContext.getConfig()).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> jsonSchema.createExecutionContext(),
                "Calling createExecutionContext when ValidationContext.getConfig() returns null should throw NPE");
    }

    /** If the customizer throws a RuntimeException, it must propagate out of the method. */
    @Test
    void createExecutionContext_customizerThrowsRuntimeException_propagates() {
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        doThrow(new RuntimeException("customizer failure"))
                .when(customizer).customize(any(ExecutionContext.class), any(ValidationContext.class));
        config.setExecutionContextCustomizer(customizer);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.createExecutionContext(),
                "Exception thrown by customizer should propagate");

        assertEquals("customizer failure", ex.getMessage(),
                "Propagated exception should retain the original message");
    }
}
```