package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_debug_Test {

    /**
     * Mocked logger used to capture the debug call.
     */
    @Mock
    private Logger logger;

    /**
     * Mocked JSON node representing the instance being validated.
     */
    @Mock
    private JsonNode node;

    /**
     * Mocked JSON node representing the root of the document.
     */
    @Mock
    private JsonNode rootNode;

    /**
     * Mocked path indicating the location of the instance within the document.
     */
    @Mock
    private JsonNodePath instanceLocation;

    /**
     * Supplies all test cases described in the specification.
     *
     * @return a stream of arguments for {@link #debug_shouldBehaveAccordingToParameters}
     */
    Stream<Arguments> debugTestCases() {
        // Common non‑null mocks that can be reused across several cases.
        JsonNode sampleNode = mock(JsonNode.class);
        JsonNode rootNode = mock(JsonNode.class);
        ObjectNode objectNodeWithFields = mock(ObjectNode.class);
        ObjectNode objectNodeRoot = mock(ObjectNode.class);
        JsonNode arrayNode = mock(JsonNode.class);
        return Stream.of(// -----------------------------------------------------------------
        // Group: BaseJsonValidator.debug method behavior
        // -----------------------------------------------------------------
        Arguments.of(logger, sampleNode, rootNode, JsonNodePath.of("/a/b"), false, "Standard case: all parameters are non‑null"), Arguments.of(logger, null, rootNode, JsonNodePath.of("/a"), false, "Node argument is null"), Arguments.of(logger, sampleNode, null, JsonNodePath.of("/b"), false, "RootNode argument is null"), Arguments.of(logger, sampleNode, rootNode, null, false, "InstanceLocation argument is null"), Arguments.of(logger, null, null, null, false, "All arguments are null"), // -----------------------------------------------------------------
        // Group: BaseJsonValidator.debug edge cases
        // -----------------------------------------------------------------
        Arguments.of(null, sampleNode, rootNode, JsonNodePath.of("/a/b"), true, "Logger argument is null – expect NullPointerException"), Arguments.of(logger, sampleNode, rootNode, JsonNodePath.of(""), false, "InstanceLocation is an empty path"), Arguments.of(logger, sampleNode, rootNode, JsonNodePath.of("/"), false, "InstanceLocation represents the document root"), Arguments.of(logger, objectNodeWithFields, objectNodeRoot, JsonNodePath.of("/obj"), false, "Both node and rootNode are ObjectNode instances with fields"), Arguments.of(logger, arrayNode, null, JsonNodePath.of("/arr"), false, "Node is an array node while rootNode is null"));
    }

    @Test
    void debug_shouldLogCorrectMessage() {
        // WHEN: the static debug method is invoked
        BaseJsonValidator.debug(logger, node, rootNode, instanceLocation);
        // THEN: the logger must receive a debug call with the exact format string
        // and the three supplied arguments in the same order.
        verify(logger, times(1)).debug(eq("validate( {}, {}, {})"), same(node), same(rootNode), same(instanceLocation));
    }

    /**
     * Parameterized test that executes the {@code debug} method with a variety of
     * argument combinations defined in {@link #debugTestCases()}.
     *
     * @param loggerParam        the {@link Logger} instance to pass (may be {@code null})
     * @param node               the instance node (may be {@code null})
     * @param rootNode           the root document node (may be {@code null})
     * @param instanceLocation   the location within the document (may be {@code null})
     * @param expectException    {@code true} if a {@link NullPointerException} is expected
     * @param description        short description used as the test name
     */
    @ParameterizedTest(name = "{index}: {5}")
    @MethodSource("debugTestCases")
    void debug_shouldBehaveAccordingToParameters(Logger loggerParam, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean expectException, String description) {
        if (expectException) {
            // When the logger is null we expect a NPE because the method tries to call logger.debug().
            assertThrows(NullPointerException.class, () -> BaseJsonValidator.debug(loggerParam, node, rootNode, instanceLocation), "Expected NullPointerException when logger is null");
            return;
        }
        // Normal execution – verify that logger.debug is called with the exact format
        // string and the supplied arguments (preserving nulls where appropriate).
        BaseJsonValidator.debug(loggerParam, node, rootNode, instanceLocation);
        // Build matchers that respect possible null values.
        Object nodeMatcher = node == null ? isNull() : same(node);
        Object rootMatcher = rootNode == null ? isNull() : same(rootNode);
        Object pathMatcher = instanceLocation == null ? isNull() : same(instanceLocation);
        verify(loggerParam, times(1)).debug(eq("validate( {}, {}, {})"), nodeMatcher, rootMatcher, pathMatcher);
        verifyNoMoreInteractions(loggerParam);
    }
}
