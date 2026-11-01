```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_debug_Test {

    @Mock
    Logger logger;

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionConfig executionConfig;

    // real JsonNode instances used in the tests
    private JsonNode node;
    private JsonNode rootNode;
    private JsonNodePath instanceLocation;

    @BeforeAll
    static void setupBeforeAll() {
        // no global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // create simple JSON nodes
        ObjectMapper mapper = new ObjectMapper();
        node = mapper.readTree("{\"field\":\"value\"}");
        rootNode = mapper.readTree("{\"root\":true}");

        // we can use a mock for JsonNodePath – only its toString() representation is used
        instanceLocation = mock(JsonNodePath.class);
        when(instanceLocation.toString()).thenReturn("/instance/path");

        // default stubbing for execution context
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
    }

    @AfterEach
    void teardownAfterEach() {
        // reset mocks to avoid interaction leakage between tests
        reset(logger, executionContext, executionConfig, instanceLocation);
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up
    }

    @Test
    void debug_ShouldLogWhenDebugEnabledAndLoggerDebugEnabled() {
        // arrange
        when(executionConfig.isDebugEnabled()).thenReturn(true);
        when(logger.isDebugEnabled()).thenReturn(true);

        // act
        BaseJsonValidator.debug(logger, executionContext, node, rootNode, instanceLocation);

        // assert – the logger should receive exactly one debug call with the expected message
        String expectedMessage = new StringBuilder()
                .append("validate( ")
                .append(node.toString())
                .append(", ")
                .append(rootNode.toString())
                .append(", ")
                .append(instanceLocation.toString())
                .append(")")
                .toString();

        verify(logger, times(1)).debug(expectedMessage);
    }

    @Test
    void debug_ShouldNotLogWhenDebugDisabled() {
        // arrange – both flags false (any combination should suppress logging)
        when(executionConfig.isDebugEnabled()).thenReturn(false);
        when(logger.isDebugEnabled()).thenReturn(true); // logger debug enabled but config disabled

        // act
        BaseJsonValidator.debug(logger, executionContext, node, rootNode, instanceLocation);

        // assert – logger.debug must never be called
        verify(logger, never()).debug(anyString());
    }

    @Test
    void debug_ShouldNotLogWhenLoggerDebugDisabled() {
        // arrange – config enabled but logger debug disabled
        when(executionConfig.isDebugEnabled()).thenReturn(true);
        when(logger.isDebugEnabled()).thenReturn(false);

        // act
        BaseJsonValidator.debug(logger, executionContext, node, rootNode, instanceLocation);

        // assert – logger.debug must never be called
        verify(logger, never()).debug(anyString());
    }
}
```