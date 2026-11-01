```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.slf4j.Logger;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_debug_Test {

    @Mock
    Logger logger;

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionConfig executionConfig;

    @Mock
    JsonNode node;

    @Mock
    JsonNode rootNode;

    @Mock
    JsonNodePath instanceLocation;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default stubbing common to all tests
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference
        clearInvocations(logger, executionContext, executionConfig, node, rootNode, instanceLocation);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void debug_ShouldLogWhenDebugEnabled() {
        // Arrange
        when(executionConfig.isDebugEnabled()).thenReturn(true);
        when(logger.isDebugEnabled()).thenReturn(true);
        when(node.toString()).thenReturn("node");
        when(rootNode.toString()).thenReturn("root");
        when(instanceLocation.toString()).thenReturn("path");

        // Act
        BaseJsonValidator.debug(logger, executionContext, node, rootNode, instanceLocation);

        // Assert
        verify(logger, times(1)).debug("validate( node, root, path)");
    }

    @Test
    void debug_ShouldNotLogWhenExecutionConfigDebugDisabled() {
        // Arrange
        when(executionConfig.isDebugEnabled()).thenReturn(false);
        when(logger.isDebugEnabled()).thenReturn(true); // logger would be ready, but config disables it

        // Act
        BaseJsonValidator.debug(logger, executionContext, node, rootNode, instanceLocation);

        // Assert
        verify(logger, never()).debug(anyString());
    }

    @Test
    void debug_ShouldNotLogWhenLoggerDebugDisabled() {
        // Arrange
        when(executionConfig.isDebugEnabled()).thenReturn(true);
        when(logger.isDebugEnabled()).thenReturn(false); // logger itself disabled

        // Act
        BaseJsonValidator.debug(logger, executionContext, node, rootNode, instanceLocation);

        // Assert
        verify(logger, never()).debug(anyString());
    }
}
```