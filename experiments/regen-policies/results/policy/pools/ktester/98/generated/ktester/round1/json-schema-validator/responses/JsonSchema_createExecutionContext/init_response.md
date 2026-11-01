```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests for {@link JsonSchema#createExecutionContext()}.
 *
 * The tests verify that:
 * <ul>
 *   <li>The {@link ExecutionConfig} inside the created {@link ExecutionContext}
 *       copies the locale, format‑assertions flag and fail‑fast flag from the
 *       {@link SchemaValidatorsConfig} held by the {@link ValidationContext}.</li>
 *   <li>If an {@link ExecutionContextCustomizer} is configured, it is invoked
 *       with the newly created {@link ExecutionContext} and the current
 *       {@link ValidationContext}.</li>
 * </ul>
 *
 * The test class lives in the same package as {@link JsonSchema} so that the
 * package‑private static factory method {@code JsonSchema.from(...)} can be used
 * to create an instance without resorting to reflection.
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
    ObjectNode schemaNode;                        // a minimal schema node (empty object)

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
}
```