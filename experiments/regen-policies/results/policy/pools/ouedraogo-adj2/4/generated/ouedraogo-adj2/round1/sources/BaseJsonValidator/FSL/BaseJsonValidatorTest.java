package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Unit tests for {@link BaseJsonValidator}.
 *
 * The tests create a concrete subclass of {@code BaseJsonValidator}
 * (named {@code TestValidator}) that provides a minimal implementation of the
 * abstract {@code validate} method.  All public and protected members of
 * {@code BaseJsonValidator} are exercised, including static utility methods.
 *
 * No external mocking frameworks are used; a tiny {@link Logger} implementation
 * captures the call made by {@code debug(...)}.
 */
public class BaseJsonValidatorTest {

    /** Simple {@link Logger} implementation that records the last debug call. */
    private static class RecordingLogger implements Logger {
        String lastMessage;
        Object[] lastArgs;

        @Override
        public void debug(String format, Object... arguments) {
            this.lastMessage = format;
            this.lastArgs = arguments;
        }

        // -----------------------------------------------------------------
        // The remaining Logger methods are not needed for the tests and are
        // implemented as no‑ops.
        // -----------------------------------------------------------------
        @Override public String getName() { return "RecordingLogger"; }
        @Override public boolean isTraceEnabled() { return false; }
        @Override public void trace(String msg) {}
        @Override public void trace(String format, Object arg) {}
        @Override public void trace(String format, Object arg1, Object arg2) {}
        @Override public void trace(String format, Object... arguments) {}
        @Override public void trace(String msg, Throwable t) {}
        @Override public boolean isTraceEnabled(org.slf4j.Marker marker) { return false; }
        @Override public void trace(org.slf4j.Marker marker, String msg) {}
        @Override public void trace(org.slf4j.Marker marker, String format, Object arg) {}
        @Override public void trace(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void trace(org.slf4j.Marker marker, String format, Object... argArray) {}
        @Override public void trace(org.slf4j.Marker marker, String msg, Throwable t) {}
        @Override public boolean isDebugEnabled() { return true; }
        @Override public boolean isDebugEnabled(org.slf4j.Marker marker) { return true; }
        @Override public void debug(String msg) {}
        @Override public void debug(String format, Object arg) {}
        @Override public void debug(String format, Object arg1, Object arg2) {}
        @Override public void debug(org.slf4j.Marker marker, String msg) {}
        @Override public void debug(org.slf4j.Marker marker, String format, Object arg) {}
        @Override public void debug(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void debug(org.slf4j.Marker marker, String format, Object... arguments) {}
        @Override public void debug(org.slf4j.Marker marker, String msg, Throwable t) {}
        @Override public boolean isInfoEnabled() { return false; }
        @Override public void info(String msg) {}
        @Override public void info(String format, Object arg) {}
        @Override public void info(String format, Object arg1, Object arg2) {}
        @Override public void info(String format, Object... arguments) {}
        @Override public void info(String msg, Throwable t) {}
        @Override public boolean isInfoEnabled(org.slf4j.Marker marker) { return false; }
        @Override public void info(org.slf4j.Marker marker, String msg) {}
        @Override public void info(org.slf4j.Marker marker, String format, Object arg) {}
        @Override public void info(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void info(org.slf4j.Marker marker, String format, Object... arguments) {}
        @Override public void info(org.slf4j.Marker marker, String msg, Throwable t) {}
        @Override public boolean isWarnEnabled() { return false; }
        @Override public void warn(String msg) {}
        @Override public void warn(String format, Object arg) {}
        @Override public void warn(String format, Object... arguments) {}
        @Override public void warn(String format, Object arg1, Object arg2) {}
        @Override public void warn(String msg, Throwable t) {}
        @Override public boolean isWarnEnabled(org.slf4j.Marker marker) { return false; }
        @Override public void warn(org.slf4j.Marker marker, String msg) {}
        @Override public void warn(org.slf4j.Marker marker, String format, Object arg) {}
        @Override public void warn(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void warn(org.slf4j.Marker marker, String format, Object... arguments) {}
        @Override public void warn(org.slf4j.Marker marker, String msg, Throwable t) {}
        @Override public boolean isErrorEnabled() { return false; }
        @Override public void error(String msg) {}
        @Override public void error(String format, Object arg) {}
        @Override public void error(String format, Object arg1, Object arg2) {}
        @Override public void error(String format, Object... arguments) {}
        @Override public void error(String msg, Throwable t) {}
        @Override public boolean isErrorEnabled(org.slf4j.Marker marker) { return false; }
        @Override public void error(org.slf4j.Marker marker, String msg) {}
        @Override public void error(org.slf4j.Marker marker, String format, Object arg) {}
        @Override public void error(org.slf4j.Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void error(org.slf4j.Marker marker, String format, Object... arguments) {}
        @Override public void error(org.slf4j.Marker marker, String msg, Throwable t) {}
    }

    /** Minimal {@link OutputFormat} implementation used by the tests. */
    private static class SimpleOutputFormat implements OutputFormat<Set<ValidationMessage>> {
        @Override
        public void customize(ExecutionContext executionContext, ValidationContext validationContext) {
            // no customisation required for the test
        }

        @Override
        public Set<ValidationMessage> format(Set<ValidationMessage> messages,
                                                ExecutionContext executionContext,
                                                ValidationContext validationContext) {
            return messages;
        }
    }

    /** Minimal {@link ExecutionCustomizer} implementation used by the tests. */
    private static class SimpleExecutionCustomizer implements ExecutionCustomizer {
        @Override
        public void customize(ExecutionContext executionContext, ValidationContext validationContext) {
            // nothing to customise for the test
        }
    }

    /**
     * Concrete subclass of {@link BaseJsonValidator} providing a trivial
     * implementation of the abstract {@code validate} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                       JsonNodePath evaluationPath,
                       JsonNode schemaNode,
                       JsonSchema parentSchema,
                       ValidatorTypeCode validatorType,
                       ValidationContext validationContext,
                       boolean suppressSubSchemaRetrieval) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                  validatorType, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                JsonNodePath instanceLocation) {
            // For testing we simply return an empty set – no validation errors.
            return Collections.emptySet();
        }
    }

    /** Helper to build a dummy {@link JsonSchema} that can act as a parent. */
    private JsonSchema buildDummyParentSchema(String typeValue) {
        // Build a simple schema node containing a "type" property.
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("type", typeValue);

        // The JsonSchema constructor used here matches the public API of the
        // library (location, path, node, parent, validationContext).
        // Passing {@code null} for the parent and validationContext is acceptable
        // for the limited interactions performed in the tests.
        return new JsonSchema(
                SchemaLocation.of("http://example.com/parent"),
                new JsonNodePath(PathType.DEFAULT),
                schemaNode,
                null,
                null);
    }

    @Test
    public void testEqualsMethod() {
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-10));
        assertTrue(BaseJsonValidator.equals(-123.456, -123.456));
    }

    @Test
    public void testDebugLogsCorrectly() {
        RecordingLogger logger = new RecordingLogger();
        JsonNode node = JsonNodeFactory.instance.objectNode().put("a", 1);
        JsonNode root = JsonNodeFactory.instance.objectNode();
        JsonNodePath path = new JsonNodePath(PathType.DEFAULT).append("foo");

        BaseJsonValidator.debug(logger, node, root, path);

        assertEquals("validate( {}, {}, {})", logger.lastMessage);
        assertArrayEquals(new Object[]{node, root, path}, logger.lastArgs);
    }

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        TestValidator validator = new TestValidator(
                SchemaLocation.of("http://example.com/schema"),
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(),
                null,
                ValidatorTypeCode.TYPE,
                null,
                true); // suppressSubSchemaRetrieval = true

        assertNull("When suppression flag is true, fetchSubSchemaNode must return null",
                validator.fetchSubSchemaNode(null));
    }

    @Test
    public void testFetchSubSchemaNodeWhenNoId() {
        TestValidator validator = new TestValidator(
                SchemaLocation.of("http://example.com/schema"),
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(), // empty schema node – no "id"
                null,
                ValidatorTypeCode.TYPE,
                null,
                false); // suppression off

        // No "id" field → obtainSubSchemaNode returns null → fetchSubSchemaNode returns null
        assertNull("Without an 'id' property, fetchSubSchemaNode must return null",
                validator.fetchSubSchemaNode(null));
    }

    @Test
    public void testGetterMethods() {
        SchemaLocation location = SchemaLocation.of("http://example.com/schema");
        JsonNodePath evalPath = new JsonNodePath(PathType.DEFAULT).append("myToken");
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode().put("x", 42);
        JsonSchema parent = buildDummyParentSchema("string");

        TestValidator validator = new TestValidator(
                location,
                evalPath,
                schemaNode,
                parent,
                ValidatorTypeCode.TYPE,
                null,
                false);

        assertEquals(location, validator.getSchemaLocation());
        assertEquals(evalPath, validator.getEvaluationPath());
        assertEquals(ValidatorTypeCode.TYPE.getValue(), validator.getKeyword());
        assertEquals(schemaNode, validator.getSchemaNode());
        assertEquals(parent, validator.getParentSchema());
    }

    @Test
    public void testGetNodeFieldType() {
        JsonSchema parent = buildDummyParentSchema("integer");

        TestValidator validator = new TestValidator(
                SchemaLocation.of("http://example.com/schema"),
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(),
                parent,
                ValidatorTypeCode.TYPE,
                null,
                false);

        assertEquals("integer", validator.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemasInvokesInitialize() {
        // Build two dummy schemas that record whether initializeValidators() was called.
        class DummySchema extends JsonSchema {
            boolean initialized = false;
            DummySchema(SchemaLocation loc) {
                super(loc,
                      new JsonNodePath(PathType.DEFAULT),
                      JsonNodeFactory.instance.objectNode(),
                      null,
                      null);
            }
            @Override
            public void initializeValidators() {
                initialized = true;
            }
        }

        DummySchema s1 = new DummySchema(SchemaLocation.of("http://example.com/1"));
        DummySchema s2 = new DummySchema(SchemaLocation.of("http://example.com/2"));

        TestValidator validator = new TestValidator(
                SchemaLocation.of("http://example.com/schema"),
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(),
                null,
                ValidatorTypeCode.TYPE,
                null,
                false);

        validator.preloadJsonSchemas(java.util.Arrays.asList(s1, s2));

        assertTrue("First dummy schema should have been initialized", s1.initialized);
        assertTrue("Second dummy schema should have been initialized", s2.initialized);
    }

    @Test
    public void testAtRootCreatesRootPath() {
        TestValidator validator = new TestValidator(
                SchemaLocation.of("http://example.com/schema"),
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(),
                null,
                ValidatorTypeCode.TYPE,
                null,
                false);

        JsonNodePath rootPath = validator.atRoot();
        // The root path for the default PathType has no name elements.
        assertEquals(0, rootPath.getNameCount());
        assertEquals("", rootPath.toString());
    }

    @Test
    public void testToStringReturnsLastPathElement() {
        JsonNodePath path = new JsonNodePath(PathType.DEFAULT)
                .append("first")
                .append("second")
                .append("lastToken");

        TestValidator validator = new TestValidator(
                SchemaLocation.of("http://example.com/schema"),
                path,
                JsonNodeFactory.instance.objectNode(),
                null,
                ValidatorTypeCode.TYPE,
                null,
                false);

        // getName(-1) returns the last token; toString should therefore be "lastToken".
        assertEquals("lastToken", validator.toString());
    }

    @Test
    public void testValidateOverloadsReturnEmptySet() {
        TestValidator validator = new TestValidator(
                SchemaLocation.of("http://example.com/schema"),
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(),
                null,
                ValidatorTypeCode.TYPE,
                null,
                false);

        ExecutionContext execCtx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode().put("field", "value");

        // 1. validate(ExecutionContext, JsonNode)
        Set<ValidationMessage> result1 = validator.validate(execCtx, node);
        assertNotNull(result1);
        assertTrue(result1.isEmpty());

        // 2. validate(ExecutionContext, JsonNode, OutputFormat)
        SimpleOutputFormat format = new SimpleOutputFormat();
        Set<ValidationMessage> result2 = validator.validate(execCtx, node, format);
        assertNotNull(result2);
        assertTrue(result2.isEmpty());

        // 3. validate(ExecutionContext, JsonNode, OutputFormat, ExecutionCustomizer)
        SimpleExecutionCustomizer customizer = new SimpleExecutionCustomizer();
        Set<ValidationMessage> result3 = validator.validate(execCtx, node, format, customizer);
        assertNotNull(result3);
        assertTrue(result3.isEmpty());
    }
}
