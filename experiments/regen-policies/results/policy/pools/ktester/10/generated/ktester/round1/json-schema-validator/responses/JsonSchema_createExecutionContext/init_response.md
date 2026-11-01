```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;

/**
 * Tests for {@link JsonSchema#createExecutionContext()}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_createExecutionContext_Test {

    // ----- collaborators ----------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;

    // ----- object under test ------------------------------------------------
    JsonSchema jsonSchema;

    // ----- shared test data ------------------------------------------------
    SchemaValidatorsConfig config;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // a real config instance – we can mutate it per test
        config = new SchemaValidatorsConfig();

        // stub the validationContext to return our config
        when(validationContext.getConfig()).thenReturn(config);

        // Build a JsonSchema instance using the package‑private factory method.
        // The method is accessible because the test resides in the same package.
        jsonSchema = JsonSchema.from(validationContext,
                                    schemaLocation,
                                    evaluationPath,
                                    schemaNode,
                                    null,                 // no parent
                                    false);               // do not suppress sub‑schema retrieval
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        reset(validationContext, schemaLocation, evaluationPath, schemaNode);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }

    @Test
    void createExecutionContext_withoutCustomizer_returnsContextWithConfig() {
        // Act
        ExecutionContext ec = jsonSchema.createExecutionContext();

        // Assert
        assertNotNull(ec, "ExecutionContext should not be null");
        assertSame(config, ec.getExecutionConfig(),
                "The ExecutionConfig inside the ExecutionContext must be the one from ValidationContext");
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
        assertEquals(!original, ec.getExecutionConfig().isFormatAssertionsEnabled(),
                "Customizer should have toggled the formatAssertionsEnabled flag");
    }
}
```