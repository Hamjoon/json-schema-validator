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

/**
 * Unit tests for {@link BaseJsonValidator#debug(Logger, ExecutionContext, JsonNode, JsonNode, JsonNodePath)}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal logging when both debug flags are enabled</li>
 *   <li>no logging when either flag is disabled</li>
 *   <li>behaviour when any argument is {@code null}</li>
 *   <li>short‑circuiting when logger debug is disabled (even if other arguments are null)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_debug_Test {

    @Mock
    Logger logger;

    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionConfig executionConfig;

    // real JsonNode instances used in the tests (except when we deliberately use null)
    private JsonNode node;
    private JsonNode rootNode;
    private JsonNodePath instanceLocation;

    @BeforeAll
    static void setupBeforeAll() {
        // no global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // create simple JSON nodes – these are the default non‑null values
        ObjectMapper mapper = new ObjectMapper();
        node = mapper.readTree("{\"field\":\"value\"}");
        rootNode = mapper.readTree("{\"root\":true}");

        // mock JsonNodePath – only its toString() representation is used
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

    /* --------------------------------------------------------------------- */
    /* Existing basic scenarios (kept unchanged)                            */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /* New tests for null handling                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void debug_ShouldThrowNPE_WhenLoggerIsNull() {
        // arrange – logger is null, execution config debug enabled
        when(executionConfig.isDebugEnabled()).thenReturn(true);
        // act & assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.debug(null, executionContext, node, rootNode, instanceLocation));
    }

    @Test
    void debug_ShouldThrowNPE_WhenExecutionContextIsNull() {
        // arrange – executionContext is null, logger debug enabled
        when(logger.isDebugEnabled()).thenReturn(true);
        when(executionConfig.isDebugEnabled()).thenReturn(true);
        // act & assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.debug(logger, null, node, rootNode, instanceLocation));
    }

    @Test
    void debug_ShouldThrowNPE_WhenExecutionConfigIsNull() {
        // arrange – executionContext returns null ExecutionConfig
        when(executionContext.getExecutionConfig()).thenReturn(null);
        when(logger.isDebugEnabled()).thenReturn(true);
        // act & assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.debug(logger, executionContext, node, rootNode, instanceLocation));
    }

    @Test
    void debug_ShouldThrowNPE_WhenNodeIsNullAndDebugEnabled() {
        // arrange – both debug flags true, node is null
        when(executionConfig.isDebugEnabled()).thenReturn(true);
        when(logger.isDebugEnabled()).thenReturn(true);
        // act & assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.debug(logger, executionContext, null, rootNode, instanceLocation));
    }

    @Test
    void debug_ShouldThrowNPE_WhenRootNodeIsNullAndDebugEnabled() {
        // arrange – both debug flags true, rootNode is null
        when(executionConfig.isDebugEnabled()).thenReturn(true);
        when(logger.isDebugEnabled()).thenReturn(true);
        // act & assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.debug(logger, executionContext, node, null, instanceLocation));
    }

    @Test
    void debug_ShouldThrowNPE_WhenInstanceLocationIsNullAndDebugEnabled() {
        // arrange – both debug flags true, instanceLocation is null
        when(executionConfig.isDebugEnabled()).thenReturn(true);
        when(logger.isDebugEnabled()).thenReturn(true);
        // act & assert
        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.debug(logger, executionContext, node, rootNode, null));
    }

    @Test
    void debug_ShouldSilentlyReturn_WhenLoggerDebugDisabledEvenIfOtherArgsAreNull() {
        // arrange – logger debug disabled, execution config debug enabled,
        // but node, rootNode and instanceLocation are null
        when(logger.isDebugEnabled()).thenReturn(false);
        when(executionConfig.isDebugEnabled()).thenReturn(true);
        // act – should not throw
        assertDoesNotThrow(() ->
                BaseJsonValidator.debug(logger, executionContext, null, null, null));
        // verify that logger.debug was never called (short‑circuit)
        verify(logger, never()).debug(anyString());
    }
}
```