```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Additional unit tests for {@link JsonSchema#createExecutionContext()}.
 *
 * <p>These tests complement the ones already provided in the template and cover:
 *
 * <ul>
 *   <li>Customizer mutating the {@link ExecutionConfig} (e.g., toggling {@code failFast}).</li>
 *   <li>Customizer throwing an exception – the exception must propagate.</li>
 *   <li>Behaviour when the {@link ValidationContext} stored inside {@link JsonSchema}
 *       is {@code null}.</li>
 *   <li>Behaviour when {@link ValidationContext#getConfig()} returns {@code null}.</li>
 * </ul>
 *
 * The test class keeps the original {@code @BeforeEach}/{@code @AfterEach} methods
 * untouched and re‑uses the {@code jsonSchema} instance created there.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_createExecutionContext_Test {

    /** Mocks and test fixtures shared by all test methods. */
    @Mock
    ValidationContext validationContext;          // injected into JsonSchema
    @Mock
    JsonMetaSchema metaSchema;                    // required by ValidationContext
    @Mock
    SchemaLocation schemaLocation;                // passed to JsonSchema.from(...)
    @Mock
    JsonNodePath evaluationPath;                  // passed to JsonSchema.from(...)

    SchemaValidatorsConfig config;                // real config – we set values on it
    ObjectNode schemaNode;                         // a minimal schema node (empty object)

    /** Instance of {@link JsonSchema} under test. */
    JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() {
        // ---- create a very small schema node (empty object) -----------------
        schemaNode = new ObjectMapper().createObjectNode();

        // ---- configure a real SchemaValidatorsConfig ------------------------
        config = new SchemaValidatorsConfig();
        config.setLocale(Locale.GERMAN);
        config.setFormatAssertionsEnabled(true);
        config.setFailFast(true);
        // no customizer yet – individual tests will set it if needed

        // ---- stub the ValidationContext to return the objects we need -------
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        // resolveSchemaId is called during JsonSchema construction – we do not need
        // any special handling for this test, returning null is sufficient.
        when(validationContext.resolveSchemaId(any())).thenReturn(null);

        // ---- create the JsonSchema instance using the package‑private factory --
        jsonSchema = JsonSchema.from(validationContext,
                                    schemaLocation,
                                    evaluationPath,
                                    schemaNode,
                                    null,               // no parent
                                    false);             // suppressSubSchemaRetrieval
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for these unit tests
    }

    /** Verify that the ExecutionConfig values are copied from the ValidationContext. */
    @Test
    void createExecutionContext_copiesConfigValues() {
        // Act
        ExecutionContext executionContext = jsonSchema.createExecutionContext();

        // Assert – the ExecutionConfig inside the ExecutionContext must reflect the
        // values we set on the SchemaValidatorsConfig.
        ExecutionConfig execConfig = executionContext.getExecutionConfig();
        assertNotNull(execConfig, "ExecutionConfig should not be null");

        assertEquals(config.getLocale(), execConfig.getLocale(),
                "Locale should be copied from the config");
        assertEquals(config.getFormatAssertionsEnabled(), execConfig.isFormatAssertionsEnabled(),
                "formatAssertionsEnabled should be copied from the config");
        assertEquals(config.isFailFast(), execConfig.isFailFast(),
                "failFast flag should be copied from the config");
    }

    /** Verify that a configured ExecutionContextCustomizer is invoked. */
    @Test
    void createExecutionContext_invokesCustomizer_whenPresent() {
        // Arrange – create a mock customizer and register it in the config
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        config.setExecutionContextCustomizer(customizer);

        // Act
        ExecutionContext executionContext = jsonSchema.createExecutionContext();

        // Assert – the customizer must have been called exactly once with the
        // newly created ExecutionContext and the ValidationContext used by the schema.
        verify(customizer, times(1)).customize(executionContext, validationContext);
    }

    /** Customizer mutates the ExecutionConfig (toggles failFast). */
    @Test
    void createExecutionContext_customizerCanMutateConfig() {
        // Arrange – a customizer that flips the failFast flag
        ExecutionContextCustomizer customizer = (execCtx, vCtx) -> {
            ExecutionConfig ec = execCtx.getExecutionConfig();
            ec.setFailFast(!ec.isFailFast());
        };
        config.setExecutionContextCustomizer(customizer);

        // Act
        ExecutionContext executionContext = jsonSchema.createExecutionContext();

        // Assert – the flag has been toggled compared to the original config
        boolean expected = !config.isFailFast(); // original was true, we expect false
        assertEquals(expected, executionContext.getExecutionConfig().isFailFast(),
                "Customizer should have toggled the failFast flag");
    }

    /** If the customizer throws, the exception propagates out of createExecutionContext(). */
    @Test
    void createExecutionContext_customizerThrows_exceptionPropagates() {
        // Arrange – a customizer that always throws
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        RuntimeException boom = new RuntimeException("boom");
        doThrow(boom).when(customizer).customize(any(), any());
        config.setExecutionContextCustomizer(customizer);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.createExecutionContext(),
                "Exception from customizer should propagate");
        assertSame(boom, ex, "Propagated exception should be the same instance thrown by the customizer");
    }

    /** Null ValidationContext inside JsonSchema leads to NPE. */
    @Test
    void createExecutionContext_nullValidationContext_throwsNPE() throws Exception {
        // Use reflection to set the protected field BaseJsonValidator.validationContext to null
        Field field = BaseJsonValidator.class.getDeclaredField("validationContext");
        field.setAccessible(true);
        field.set(jsonSchema, null);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> jsonSchema.createExecutionContext(),
                "When validationContext is null, createExecutionContext should throw NPE");
    }

    /** Null SchemaValidatorsConfig returned from ValidationContext leads to NPE. */
    @Test
    void createExecutionContext_nullConfig_throwsNPE() {
        // Stub getConfig() to return null
        when(validationContext.getConfig()).thenReturn(null);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> jsonSchema.createExecutionContext(),
                "When validationContext.getConfig() returns null, createExecutionContext should throw NPE");
    }

    /** Verify that a non‑default locale is correctly transferred to the ExecutionContext. */
    @Test
    void createExecutionContext_localePropagation() {
        // Change locale to a non‑default value
        config.setLocale(Locale.FRENCH);

        ExecutionContext executionContext = jsonSchema.createExecutionContext();

        assertEquals(Locale.FRENCH, executionContext.getExecutionConfig().getLocale(),
                "Locale set in SchemaValidatorsConfig must be propagated to ExecutionContext");
    }
}
```