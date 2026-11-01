package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Marker;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Minimal {@link Logger} implementation that records debug messages.
     */
    private static class SimpleLogger implements Logger {

        private final List<String> messages = new ArrayList<>();

        private boolean debugEnabled;

        SimpleLogger(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
        }

        void setDebugEnabled(boolean enabled) {
            this.debugEnabled = enabled;
        }

        List<String> getMessages() {
            return messages;
        }

        @Override
        public String getName() {
            return "SimpleLogger";
        }

        @Override
        public boolean isTraceEnabled() {
            return false;
        }

        @Override
        public void trace(String msg) {
        }

        @Override
        public void trace(String format, Object arg) {
        }

        @Override
        public void trace(String format, Object arg1, Object arg2) {
        }

        @Override
        public void trace(String format, Object... arguments) {
        }

        @Override
        public void trace(String msg, Throwable t) {
        }

        @Override
        public boolean isTraceEnabled(Marker marker) {
            return false;
        }

        @Override
        public void trace(Marker marker, String msg) {
        }

        @Override
        public void trace(Marker marker, String format, Object arg) {
        }

        @Override
        public void trace(Marker marker, String format, Object arg1, Object arg2) {
        }

        @Override
        public void trace(Marker marker, String format, Object... argArray) {
        }

        @Override
        public void trace(Marker marker, String msg, Throwable t) {
        }

        @Override
        public boolean isDebugEnabled() {
            return debugEnabled;
        }

        @Override
        public void debug(String msg) {
            if (debugEnabled)
                messages.add(msg);
        }

        @Override
        public void debug(String format, Object arg) {
            debug(String.format(format, arg));
        }

        @Override
        public void debug(String format, Object arg1, Object arg2) {
            debug(String.format(format, arg1, arg2));
        }

        @Override
        public void debug(String format, Object... arguments) {
            debug(String.format(format, arguments));
        }

        @Override
        public void debug(String msg, Throwable t) {
            debug(msg);
        }

        @Override
        public boolean isDebugEnabled(Marker marker) {
            return debugEnabled;
        }

        @Override
        public void debug(Marker marker, String msg) {
            debug(msg);
        }

        @Override
        public void debug(Marker marker, String format, Object arg) {
            debug(format, arg);
        }

        @Override
        public void debug(Marker marker, String format, Object arg1, Object arg2) {
            debug(format, arg1, arg2);
        }

        @Override
        public void debug(Marker marker, String format, Object... arguments) {
            debug(format, arguments);
        }

        @Override
        public void debug(Marker marker, String msg, Throwable t) {
            debug(msg);
        }

        @Override
        public boolean isInfoEnabled() {
            return false;
        }

        @Override
        public void info(String msg) {
        }

        @Override
        public void info(String format, Object arg) {
        }

        @Override
        public void info(String format, Object arg1, Object arg2) {
        }

        @Override
        public void info(String format, Object... arguments) {
        }

        @Override
        public void info(String msg, Throwable t) {
        }

        @Override
        public boolean isInfoEnabled(Marker marker) {
            return false;
        }

        @Override
        public void info(Marker marker, String msg) {
        }

        @Override
        public void info(Marker marker, String format, Object arg) {
        }

        @Override
        public void info(Marker marker, String format, Object arg1, Object arg2) {
        }

        @Override
        public void info(Marker marker, String format, Object... arguments) {
        }

        @Override
        public void info(Marker marker, String msg, Throwable t) {
        }

        @Override
        public boolean isWarnEnabled() {
            return false;
        }

        @Override
        public void warn(String msg) {
        }

        @Override
        public void warn(String format, Object arg) {
        }

        @Override
        public void warn(String format, Object... arguments) {
        }

        @Override
        public void warn(String format, Object arg1, Object arg2) {
        }

        @Override
        public void warn(String msg, Throwable t) {
        }

        @Override
        public boolean isWarnEnabled(Marker marker) {
            return false;
        }

        @Override
        public void warn(Marker marker, String msg) {
        }

        @Override
        public void warn(Marker marker, String format, Object arg) {
        }

        @Override
        public void warn(Marker marker, String format, Object arg1, Object arg2) {
        }

        @Override
        public void warn(Marker marker, String format, Object... arguments) {
        }

        @Override
        public void warn(Marker marker, String msg, Throwable t) {
        }

        @Override
        public boolean isErrorEnabled() {
            return false;
        }

        @Override
        public void error(String msg) {
        }

        @Override
        public void error(String format, Object arg) {
        }

        @Override
        public void error(String format, Object arg1, Object arg2) {
        }

        @Override
        public void error(String format, Object... arguments) {
        }

        @Override
        public void error(String msg, Throwable t) {
        }

        @Override
        public boolean isErrorEnabled(Marker marker) {
            return false;
        }

        @Override
        public void error(Marker marker, String msg) {
        }

        @Override
        public void error(Marker marker, String format, Object arg) {
        }

        @Override
        public void error(Marker marker, String format, Object arg1, Object arg2) {
        }

        @Override
        public void error(Marker marker, String format, Object... arguments) {
        }

        @Override
        public void error(Marker marker, String msg, Throwable t) {
        }
    }

    /**
     * Simple {@link ExecutionConfig} implementation.
     */
    private static class SimpleExecutionConfig implements ExecutionConfig {

        private final boolean debugEnabled;

        SimpleExecutionConfig(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
        }

        @Override
        public boolean isDebugEnabled() {
            return debugEnabled;
        }
    }

    /**
     * Simple {@link ExecutionContext} implementation.
     */
    private static class SimpleExecutionContext implements ExecutionContext {

        private final ExecutionConfig cfg;

        SimpleExecutionContext(ExecutionConfig cfg) {
            this.cfg = cfg;
        }

        @Override
        public ExecutionConfig getExecutionConfig() {
            return cfg;
        }
    }

    /**
     * Minimal {@link JsonNode} that returns a predefined string representation.
     */
    private static class SimpleJsonNode extends JsonNode {

        private final String representation;

        SimpleJsonNode(String representation) {
            this.representation = representation;
        }

        @Override
        public String toString() {
            return representation;
        }

        // The following methods are required to satisfy the abstract contract.
        @Override
        public JsonNodeType getNodeType() {
            return JsonNodeType.POJO;
        }

        @Override
        public boolean isValueNode() {
            return true;
        }

        @Override
        public boolean asBoolean() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int asInt() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long asLong() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double asDouble() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String asText() {
            return representation;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
        // All other abstract methods are left with default (unsupported) implementations.
    }

    /**
     * Simple {@link JsonNodePath} implementation that returns a predefined string.
     */
    private static class SimpleJsonNodePath extends JsonNodePath {

        private final String path;

        SimpleJsonNodePath(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return path;
        }
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

    private static Stream<Arguments> nullArgumentProvider() {
        SimpleLogger goodLogger = logger(true);
        SimpleExecutionContext goodCtx = executionContext(true);
        SimpleJsonNode goodNode = jsonNode("node");
        SimpleJsonNode goodRoot = jsonNode("root");
        SimpleJsonNodePath goodPath = jsonNodePath("/path");
        return Stream.of(Arguments.of("Logger is null", null, goodCtx, goodNode, goodRoot, goodPath), Arguments.of("ExecutionContext is null", goodLogger, null, goodNode, goodRoot, goodPath), Arguments.of("Node is null", goodLogger, goodCtx, null, goodRoot, goodPath), Arguments.of("RootNode is null", goodLogger, goodCtx, goodNode, null, goodPath), Arguments.of("InstanceLocation is null", goodLogger, goodCtx, goodNode, goodRoot, null));
    }

    private static SimpleLogger logger(boolean debugEnabled) {
        return new SimpleLogger(debugEnabled);
    }

    private static SimpleExecutionContext executionContext(boolean cfgDebugEnabled) {
        return new SimpleExecutionContext(new SimpleExecutionConfig(cfgDebugEnabled));
    }

    private static SimpleJsonNode jsonNode(String representation) {
        return new SimpleJsonNode(representation);
    }

    private static SimpleJsonNodePath jsonNodePath(String path) {
        return new SimpleJsonNodePath(path);
    }

    @Test
    void debug_ShouldLogWhenDebugEnabled() {
        SimpleLogger log = logger(true);
        SimpleExecutionContext ctx = executionContext(true);
        SimpleJsonNode node = jsonNode("node");
        SimpleJsonNode root = jsonNode("root");
        SimpleJsonNodePath path = jsonNodePath("path");
        BaseJsonValidator.debug(log, ctx, node, root, path);
        assertEquals(1, log.getMessages().size());
        assertEquals("validate( node, root, path)", log.getMessages().get(0));
    }

    @Test
    void debug_ShouldNotLogWhenExecutionConfigDebugDisabled() {
        SimpleLogger log = logger(true);
        // config disables debug
        SimpleExecutionContext ctx = executionContext(false);
        SimpleJsonNode node = jsonNode("node");
        SimpleJsonNode root = jsonNode("root");
        SimpleJsonNodePath path = jsonNodePath("path");
        BaseJsonValidator.debug(log, ctx, node, root, path);
        assertTrue(log.getMessages().isEmpty(), "No debug messages should be emitted");
    }

    @Test
    void debug_ShouldNotLogWhenLoggerDebugDisabled() {
        // logger disabled
        SimpleLogger log = logger(false);
        SimpleExecutionContext ctx = executionContext(true);
        SimpleJsonNode node = jsonNode("node");
        SimpleJsonNode root = jsonNode("root");
        SimpleJsonNodePath path = jsonNodePath("path");
        BaseJsonValidator.debug(log, ctx, node, root, path);
        assertTrue(log.getMessages().isEmpty(), "Logger is disabled, so no messages");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullArgumentProvider")
    void debug_ShouldThrowNPEWhenAnyArgumentIsNull(String description, Logger loggerArg, ExecutionContext ctxArg, JsonNode nodeArg, JsonNode rootNodeArg, JsonNodePath pathArg) {
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.debug(loggerArg, ctxArg, nodeArg, rootNodeArg, pathArg), description);
    }

    @Test
    void debug_ShouldLogCorrectlyWhenValuesContainSpecialCharacters() {
        SimpleLogger log = logger(true);
        SimpleExecutionContext ctx = executionContext(true);
        SimpleJsonNode node = jsonNode("{\"key\":\"value, with, commas\"}");
        SimpleJsonNode root = jsonNode("{\"root\":\"line\nbreak\"}");
        SimpleJsonNodePath path = jsonNodePath("/path/with space");
        BaseJsonValidator.debug(log, ctx, node, root, path);
        assertEquals(1, log.getMessages().size());
        assertEquals("validate( {\"key\":\"value, with, commas\"}, {\"root\":\"line\nbreak\"}, /path/with space)", log.getMessages().get(0));
    }

    @Test
    void debug_ShouldLogPlaceholdersWhenAllStringsAreEmpty() {
        SimpleLogger log = logger(true);
        SimpleExecutionContext ctx = executionContext(true);
        SimpleJsonNode node = jsonNode("");
        SimpleJsonNode root = jsonNode("");
        SimpleJsonNodePath path = jsonNodePath("");
        BaseJsonValidator.debug(log, ctx, node, root, path);
        assertEquals(1, log.getMessages().size());
        assertEquals("validate( , , )", log.getMessages().get(0));
    }

    @Test
    void debug_ShouldThrowNPEWhenExecutionConfigIsNull() {
        // ExecutionContext that returns null for getExecutionConfig()
        ExecutionContext ctx = new ExecutionContext() {

            @Override
            public ExecutionConfig getExecutionConfig() {
                return null;
            }
        };
        SimpleLogger log = logger(true);
        SimpleJsonNode node = jsonNode("{}");
        SimpleJsonNode root = jsonNode("{}");
        SimpleJsonNodePath path = jsonNodePath("/path");
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.debug(log, ctx, node, root, path), "ExecutionConfig is null – should raise NPE");
    }
}
