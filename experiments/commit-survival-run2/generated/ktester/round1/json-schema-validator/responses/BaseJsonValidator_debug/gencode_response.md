```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

/**
 * Unit tests for {@link BaseJsonValidator#debug(Logger, JsonNode, JsonNode, String)}.
 *
 * <p>The tests are organised in three groups:
 *
 * <ul>
 *   <li>Normal logging – verifies that the logger receives the exact message pattern and arguments,
 *       even when some arguments are {@code null} or contain special characters.</li>
 *   <li>Exception propagation – ensures that the method does not swallow
 *       {@link RuntimeException}, {@link AssertionError} or any other {@link Error} coming from the
 *       logger or from the {@link JsonNode} arguments.</li>
 *   <li>Invalid usage – a {@code null} logger should result in a {@link NullPointerException}.</li>
 * </ul>
 *
 * <p>All mocks are created by Mockito.  For the cases that require a logger to evaluate the supplied
 * arguments (e.g., when a {@link JsonNode#toString()} throws), the {@code debug} method of the mocked
 * logger is stubbed to call {@code toString()} on each argument, thus reproducing the behaviour of a
 * real SLF4J implementation.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_debug_Test {

    /* --------------------------------------------------------------------- */
    /*  Normal logging cases – the logger should be invoked exactly once.   */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: at=''{3}''")
    @MethodSource("normalLoggingCases")
    void debug_normalLogging(Logger logger,
                             JsonNode node,
                             JsonNode rootNode,
                             String at) {
        // When the static debug method is invoked
        BaseJsonValidator.debug(logger, node, rootNode, at);

        // Then the logger receives exactly one call with the expected pattern.
        verify(logger).debug("validate( {}, {}, {})", node, rootNode, at);
    }

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
        JsonNode emptyArrayNode  = mock(JsonNode.class);

        return Stream.of(
                // group: debugMethodTests
                Arguments.of(mock(Logger.class), nodeA, rootA, "/path/to/element"),
                Arguments.of(mock(Logger.class), null, rootB, "/another/path"),
                Arguments.of(mock(Logger.class), nodeC, null, "/yet/another/path"),
                Arguments.of(mock(Logger.class), nodeD, rootD, ""), // empty string
                Arguments.of(mock(Logger.class), nodeE, rootE,
                        "/path/with/special/chars/!@#$%^&*()"),

                // group: debugMethodAdditionalTests
                Arguments.of(mock(Logger.class), null, null, "/both/null"),
                // "at" is null – we pass a literal null value
                Arguments.of(mock(Logger.class), nodeF, rootF, null),
                Arguments.of(mock(Logger.class), emptyObjectNode, emptyArrayNode, "/empty/nodes"),
                Arguments.of(mock(Logger.class), nodeG, rootG, "   "), // whitespace only
                // logger null case is handled separately (see below)
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
                Arguments.empty() // placeholder to keep stream non‑empty if needed
        ).filter(args -> args.get() != null); // drop the placeholder if not needed
    }

    /* --------------------------------------------------------------------- */
    /*  Exception propagation – logger or JsonNode throws.                  */
    /* --------------------------------------------------------------------- */

    @Test
    void debug_loggerIsNull_throwsNullPointerException() {
        Logger nullLogger = null;
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        String at = "/null/logger";

        assertThrows(NullPointerException.class,
                () -> BaseJsonValidator.debug(nullLogger, node, root, at));
    }

    @Test
    void debug_loggerThrowsRuntimeException_propagates() {
        Logger logger = mock(Logger.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        String at = "/exception/case";

        doThrow(new RuntimeException("logger failure"))
                .when(logger).debug(anyString(), any(), any(), any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> BaseJsonValidator.debug(logger, node, root, at));
        assertEquals("logger failure", ex.getMessage());
    }

    @Test
    void debug_loggerThrowsAssertionError_propagates() {
        Logger logger = mock(Logger.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        String at = "/error/logger";

        doThrow(new AssertionError("assertion error"))
                .when(logger).debug(anyString(), any(), any(), any());

        AssertionError err = assertThrows(AssertionError.class,
                () -> BaseJsonValidator.debug(logger, node, root, at));
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
            arg1.toString(); // this will throw
            return null;
        }).when(logger).debug(anyString(), any(), any(), any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> BaseJsonValidator.debug(logger, badNode, root, at));
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
            arg2.toString(); // throws
            return null;
        }).when(logger).debug(anyString(), any(), any(), any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> BaseJsonValidator.debug(logger, node, badRoot, at));
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
            arg1.toString(); // throws "node failure"
            // arg2 would never be evaluated
            return null;
        }).when(logger).debug(anyString(), any(), any(), any());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> BaseJsonValidator.debug(logger, badNode, badRoot, at));
        assertEquals("node failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  Lifecycle hooks – untouched (already provided in the skeleton).    */
    /* --------------------------------------------------------------------- */

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates a fresh mock for each test method automatically
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that no unexpected interactions occurred with the mock logger
        // (Only the mocks used inside a test are verified there.)
        // This call is safe – if a test didn't create a mock called `logger`, Mockito will ignore it.
        // The explicit verification for each logger mock is performed inside the test bodies.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }
}
```