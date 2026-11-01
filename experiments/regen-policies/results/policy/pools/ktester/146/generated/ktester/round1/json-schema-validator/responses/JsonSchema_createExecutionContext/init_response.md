```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;

/**
 * Unit tests for {@link JsonSchema#createExecutionContext()}.
 *
 * The tests verify that the {@link ExecutionContext} returned by the method
 * contains the configuration values taken from the {@link SchemaValidatorsConfig}
 * held by the {@link ValidationContext}, and that an optional
 * {@link ExecutionContextCustomizer} is invoked when present.
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
        config.setLocale(java.util.Locale.US);
        config.setFormatAssertionsEnabled(true);
        config.setFailFast(false);
        // By default no customizer.
        config.setExecutionContextCustomizer(null);

        // Stub the ValidationContext to return the configuration.
        when(validationContext.getConfig()).thenReturn(config);

        // Create a JsonSchema instance via reflection (its constructor is private).
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
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
}
```