package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_debug_Test {

    @Mock
    Logger // mock logger injected by Mockito
    logger;

    private static Stream<Arguments> normalLoggingCases() {
        // Helper to create a generic mock JsonNode (no special behaviour required)
        JsonNode nodeA = mock(JsonNode.class);
        JsonNode nodeC = mock(JsonNode.class);
        JsonNode nodeD = mock(JsonNode.class);
        JsonNode nodeE = mock(JsonNode.class);
        JsonNode nodeF = mock(JsonNode.class);
        JsonNode nodeG = mock(JsonNode.class);
        JsonNode nodeH = mock(JsonNode.class);
        JsonNode nodeI = mock(JsonNode.class);
        JsonNode nodeJ = mock(JsonNode.class);
        JsonNode nodeK = mock(JsonNode.class);
        JsonNode nodeL = mock(JsonNode.class);
        JsonNode rootA = mock(JsonNode.class);
        JsonNode rootB = mock(JsonNode.class);
        JsonNode rootC = mock(JsonNode.class);
        JsonNode rootD = mock(JsonNode.class);
        JsonNode rootE = mock(JsonNode.class);
        JsonNode rootF = mock(JsonNode.class);
        JsonNode rootG = mock(JsonNode.class);
        JsonNode rootH = mock(JsonNode.class);
        JsonNode rootI = mock(JsonNode.class);
        JsonNode rootJ = mock(JsonNode.class);
        JsonNode rootK = mock(JsonNode.class);
        JsonNode rootL = mock(JsonNode.class);
        // Empty structures used in additional tests
        JsonNode emptyObjectNode = mock(JsonNode.class);
        JsonNode emptyArrayNode = mock(JsonNode.class);
        return Stream.of(// group: debugMethodTests
        Arguments.of(mock(Logger.class), nodeA, rootA, "/path/to/element"), Arguments.of(mock(Logger.class), null, rootB, "/another/path"), Arguments.of(mock(Logger.class), nodeC, null, "/yet/another/path"), // empty string
        Arguments.of(mock(Logger.class), nodeD, rootD, ""), Arguments.of(mock(Logger.class), nodeE, rootE, "/path/with/special/chars/!@#$%^&*()"), // group: debugMethodAdditionalTests
        Arguments.of(mock(Logger.class), null, null, "/both/null"), // "at" is null – we pass a literal null value
        Arguments.of(mock(Logger.class), nodeF, rootF, null), Arguments.of(mock(Logger.class), emptyObjectNode, emptyArrayNode, "/empty/nodes"), // whitespace only
        Arguments.of(mock(Logger.class), nodeG, rootG, "   "), // logger null case is handled separately (see below)
        // logger throwing case is also handled separately
        // Remaining cases are covered in the exception‑propagation section
        // (they use different logger stubs)
        // No further normal cases needed here.
        // The above arguments cover every “expected normal logging” scenario.
        // The concrete mock instances are not important; only their identity matters.
        // The test method verifies that the same references are forwarded to the logger.
        // -----------------------------------------------------------------
        // End of normal cases.
        // -----------------------------------------------------------------
        // The following arguments are *not* part of normal logging and are therefore
        // excluded from this provider.
        // -----------------------------------------------------------------
        // group: debugMethodExceptionScenarios
        //   – those are covered by dedicated tests below.
        // -----------------------------------------------------------------
        // group: debugMethodAdditionalTests (exception‑throwing logger)
        //   – handled in separate tests.
        // -----------------------------------------------------------------
        // End of provider.
        // -----------------------------------------------------------------
        // placeholder to keep stream non‑empty if needed
        Arguments.empty()).filter(// drop the placeholder if not needed
        args -> args.get() != null);
    }

    @Test
    void debug_shouldLogMessageWithCorrectPatternAndArguments() {
        // given: a mock logger and dummy JsonNode instances
        JsonNode node = mock(JsonNode.class);
        JsonNode rootNode = mock(JsonNode.class);
        String at = "/some/path";
        // when: the static debug method is invoked
        BaseJsonValidator.debug(logger, node, rootNode, at);
        // then: the logger should receive a debug call with the expected format and arguments
        verify(logger).debug("validate( {}, {}, {})", node, rootNode, at);
    }

    @ParameterizedTest(name = "{index}: at=''{3}''")
    @MethodSource("normalLoggingCases")
    void debug_normalLogging(Logger logger, JsonNode node, JsonNode rootNode, String at) {
        // When the static debug method is invoked
        BaseJsonValidator.debug(logger, node, rootNode, at);
        // Then the logger receives exactly one call with the expected pattern.
        verify(logger).debug("validate( {}, {}, {})", node, rootNode, at);
    }

    @Test
    void debug_loggerIsNull_throwsNullPointerException() {
        Logger nullLogger = null;
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        String at = "/null/logger";
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.debug(nullLogger, node, root, at));
    }

    @Test
    void debug_loggerThrowsRuntimeException_propagates() {
        Logger logger = mock(Logger.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        String at = "/exception/case";
        doThrow(new RuntimeException("logger failure")).when(logger).debug(anyString(), any(), any(), any());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> BaseJsonValidator.debug(logger, node, root, at));
        assertEquals("logger failure", ex.getMessage());
    }

    @Test
    void debug_loggerThrowsAssertionError_propagates() {
        Logger logger = mock(Logger.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        String at = "/error/logger";
        doThrow(new AssertionError("assertion error")).when(logger).debug(anyString(), any(), any(), any());
        AssertionError err = assertThrows(AssertionError.class, () -> BaseJsonValidator.debug(logger, node, root, at));
        assertEquals("assertion error", err.getMessage());
    }

    @Test
    void debug_nodeToStringThrowsRuntimeException_propagates() {
        Logger logger = mock(Logger.class);
        JsonNode badNode = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        String at = "/exception/node";
        // Simulate a toString() that blows up
        when(badNode.toString()).thenThrow(new RuntimeException("node toString failure"));
        // The mock logger will evaluate the arguments (as a real SLF4J impl would)
        doAnswer(invocation -> {
            // Force evaluation of arguments to trigger the exception
            Object arg1 = invocation.getArgument(1);
            // this will throw
            arg1.toString();
            return null;
        }).when(logger).debug(anyString(), any(), any(), any());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> BaseJsonValidator.debug(logger, badNode, root, at));
        assertEquals("node toString failure", ex.getMessage());
    }

    @Test
    void debug_rootNodeToStringThrowsRuntimeException_propagates() {
        Logger logger = mock(Logger.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode badRoot = mock(JsonNode.class);
        String at = "/exception/root";
        when(badRoot.toString()).thenThrow(new RuntimeException("rootNode toString failure"));
        doAnswer(invocation -> {
            // Evaluate the second argument (rootNode)
            Object arg2 = invocation.getArgument(2);
            // throws
            arg2.toString();
            return null;
        }).when(logger).debug(anyString(), any(), any(), any());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> BaseJsonValidator.debug(logger, node, badRoot, at));
        assertEquals("rootNode toString failure", ex.getMessage());
    }

    @Test
    void debug_bothNodeAndRootThrowRuntimeException_propagatesFirst() {
        Logger logger = mock(Logger.class);
        JsonNode badNode = mock(JsonNode.class);
        JsonNode badRoot = mock(JsonNode.class);
        String at = "/both/exception";
        when(badNode.toString()).thenThrow(new RuntimeException("node failure"));
        when(badRoot.toString()).thenThrow(new RuntimeException("root failure"));
        doAnswer(invocation -> {
            // The logger evaluates arguments from left to right; we mimic that.
            Object arg1 = invocation.getArgument(1);
            // throws "node failure"
            arg1.toString();
            // arg2 would never be evaluated
            return null;
        }).when(logger).debug(anyString(), any(), any(), any());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> BaseJsonValidator.debug(logger, badNode, badRoot, at));
        assertEquals("node failure", ex.getMessage());
    }
}
