package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Utility methods {@code equals}, {@code debug}, path handling and node‑type lookup.</li>
 *   <li>Behaviour of {@code fetchSubSchemaNode} with sub‑schema retrieval suppression.</li>
 *   <li>Pre‑loading of child schemas.</li>
 *   <li>Private static discriminator handling logic via reflection.</li>
 *   <li>Edge‑case handling (null values, missing fields, etc.).</li>
 * </ul>
 *
 * <p>Only the public API listed in the prompt is used – no external mocking frameworks.
 */
@RunWith(JUnit4.class)
public class BaseJsonValidatorTest {

    /** Simple concrete implementation of the abstract {@link BaseJsonValidator}. */
    private static class MinimalValidator extends BaseJsonValidator {

        MinimalValidator(String schemaPath, JsonNode schemaNode,
                         JsonSchema parentSchema, ValidatorTypeCode validatorType,
                         ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /** Constructor that forces {@code suppressSubSchemaRetrieval} to {@code true}. */
        MinimalValidator(String schemaPath, JsonNode schemaNode,
                         JsonSchema parentSchema, ValidatorTypeCode validatorType,
                         ValidationContext validationContext, boolean suppress) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppress);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            // Minimal validator does no validation – always returns an empty set.
            return Collections.emptySet();
        }
    }

    /** Test‑specific {@link JsonSchema} that records whether {@code initializeValidators} was invoked. */
    private static class TestJsonSchema extends JsonSchema {
        private boolean initialized = false;

        TestJsonSchema(ValidationContext ctx, String schemaPath, JsonNode schemaNode) {
            super(ctx, schemaPath, null, schemaNode, null);
        }

        @Override
        public void initializeValidators() {
            initialized = true;
        }

        boolean isInitialized() {
            return initialized;
        }
    }

    /** Very small SLF4J {@link Logger} implementation that records the last formatted message. */
    private static class RecordingLogger implements Logger {
        private String lastMessage;

        String getLastMessage() {
            return lastMessage;
        }

        // Only the {@code debug(String, Object...)} method is needed for the tests.
        @Override
        public void debug(String format, Object... arguments) {
            // Very naive {}‑placeholder replacement – sufficient for the test.
            String msg = format;
            for (Object arg : arguments) {
                msg = msg.replaceFirst("\\{\\}", arg == null ? "null" : arg.toString());
            }
            lastMessage = msg;
        }

        // -----------------------------------------------------------------
        // All other Logger methods are no‑ops – they are required to satisfy
        // the interface but are not used by the class under test.
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

    private JsonNodeFactory nf;
    private JsonNode simpleNode;
    private ObjectNode emptyObjectNode;

    @Before
    public void setUp() {
        nf = JsonNodeFactory.instance;
        simpleNode = nf.textNode("sample");
        emptyObjectNode = nf.objectNode();
    }

    /** -----------------------------------------------------------------
     *  Test the numeric equality helper with typical, edge and error cases.
     *  ----------------------------------------------------------------- */
    @Test
    public void testEqualsHelper() {
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertTrue(BaseJsonValidator.equals(-123.456, -123.456 - 1e-13));
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-5));
        assertTrue(BaseJsonValidator.equals(0.0, 0.0));
        assertTrue(BaseJsonValidator.equals(Double.NaN, Double.NaN)); // NaN - NaN yields NaN, abs(NaN) is NaN < 1e-12 is false → we treat as false
        // The method does not special‑case NaN, so the result should be false.
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN));
    }

    /** -----------------------------------------------------------------
     *  Verify that the {@code debug} method forwards the formatted message
     *  to the supplied logger.
     *  ----------------------------------------------------------------- */
    @Test
    public void testDebugLogging() {
        RecordingLogger logger = new RecordingLogger();
        JsonNode root = nf.objectNode();
        BaseJsonValidator.debug(logger, simpleNode, root, "/myPath");
        String expected = "validate( sample, {}, /myPath)";
        assertEquals(expected, logger.getLastMessage());
    }

    /** -----------------------------------------------------------------
     *  Ensure {@code fetchSubSchemaNode} respects the suppression flag.
     *  ----------------------------------------------------------------- */
    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        MinimalValidator validator = new MinimalValidator(
                "/root",
                emptyObjectNode,
                null,
                ValidatorTypeCode.TRUE,
                null,
                true); // suppressSubSchemaRetrieval = true

        // Even though the validationContext is null, the method must simply return null.
        assertNull(validator.fetchSubSchemaNode(null));
    }

    /** -----------------------------------------------------------------
     *  Test {@code getNodeFieldType} when the parent schema contains a "type"
     *  property and when it does not.
     *  ----------------------------------------------------------------- */
    @Test
    public void testGetNodeFieldType() {
        // Parent schema with a "type" field
        ObjectNode parentNode = nf.objectNode().put("type", "string");
        JsonSchema parentSchema = new JsonSchema(null, null, parentNode);
        MinimalValidator validator = new MinimalValidator(
                "/child",
                emptyObjectNode,
                parentSchema,
                ValidatorTypeCode.TYPE,
                null);

        assertEquals("string", validator.getNodeFieldType());

        // Parent without a "type" field
        JsonSchema parentWithoutType = new JsonSchema(null, null, nf.objectNode());
        MinimalValidator validator2 = new MinimalValidator(
                "/child2",
                emptyObjectNode,
                parentWithoutType,
                ValidatorTypeCode.TYPE,
                null);
        assertNull(validator2.getNodeFieldType());
    }

    /** -----------------------------------------------------------------
     *  Verify that {@code preloadJsonSchemas} invokes {@code initializeValidators}
     *  on each supplied schema.
     *  ----------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemas() {
        TestJsonSchema child = new TestJsonSchema(null, "/child", nf.objectNode());
        MinimalValidator validator = new MinimalValidator(
                "/root",
                emptyObjectNode,
                null,
                ValidatorTypeCode.TRUE,
                null);
        validator.preloadJsonSchemas(Collections.singleton(child));
        assertTrue(child.isInitialized());
    }

    /** -----------------------------------------------------------------
     *  Simple path handling tests – root, token, and indexed token.
     *  ----------------------------------------------------------------- */
    @Test
    public void testPathUtilities() {
        MinimalValidator validator = new MinimalValidator(
                "/root",
                emptyObjectNode,
                null,
                ValidatorTypeCode.TRUE,
                null);

        String root = validator.atRoot();
        assertEquals(PathType.DEFAULT.getRoot(), root);

        String childPath = validator.atPath(root, "foo");
        assertEquals(PathType.DEFAULT.append(root, "foo"), childPath);

        String indexedPath = validator.atPath(root, 3);
        assertEquals(PathType.DEFAULT.append(root, 3), indexedPath);
    }

    /** -----------------------------------------------------------------
     *  Private static method {@code checkDiscriminatorMatch} – exercised via
     *  reflection. Various scenarios are covered:
     *  <ul>
     *      <li>Null discriminator value – always a match.</li>
     *      <li>Explicit mapping match.</li>
     *      <li>Implicit mapping match (schemaPath ends with value).</li>
     *      <li>No match.</li>
     *  </ul>
     *  ----------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatch() throws Exception {
        Method method = BaseJsonValidator.class.getDeclaredMethod(
                "checkDiscriminatorMatch",
                ValidationContext.DiscriminatorContext.class,
                ObjectNode.class,
                String.class,
                JsonSchema.class);
        method.setAccessible(true);

        // 1. discriminatorPropertyValue == null  -> match
        ValidationContext.DiscriminatorContext ctx1 = new ValidationContext.DiscriminatorContext();
        ObjectNode disc1 = nf.objectNode(); // empty discriminator
        JsonSchema dummy = new JsonSchema(null, "/any", nf.objectNode());
        method.invoke(null, ctx1, disc1, null, dummy);
        assertTrue(ctx1.isDiscriminatorMatchFound());

        // 2. explicit mapping match
        ValidationContext.DiscriminatorContext ctx2 = new ValidationContext.DiscriminatorContext();
        ObjectNode disc2 = nf.objectNode();
        ObjectNode mapping = nf.objectNode().put("cat", "/schemas/cat");
        disc2.set("mapping", mapping);
        JsonSchema catSchema = new JsonSchema(null, "/schemas/cat", nf.objectNode());
        method.invoke(null, ctx2, disc2, "cat", catSchema);
        assertTrue(ctx2.isDiscriminatorMatchFound());

        // 3. implicit mapping match (no mapping, but schemaPath ends with value)
        ValidationContext.DiscriminatorContext ctx3 = new ValidationContext.DiscriminatorContext();
        ObjectNode disc3 = nf.objectNode(); // no mapping
        JsonSchema dogSchema = new JsonSchema(null, "/animals/dog", nf.objectNode());
        method.invoke(null, ctx3, disc3, "dog", dogSchema);
        assertTrue(ctx3.isDiscriminatorMatchFound());

        // 4. no match at all
        ValidationContext.DiscriminatorContext ctx4 = new ValidationContext.DiscriminatorContext();
        ObjectNode disc4 = nf.objectNode();
        disc4.set("mapping", nf.objectNode().put("cat", "/schemas/cat"));
        JsonSchema fishSchema = new JsonSchema(null, "/animals/fish", nf.objectNode());
        method.invoke(null, ctx4, disc4, "bird", fishSchema);
        assertFalse(ctx4.isDiscriminatorMatchFound());
    }

    /** -----------------------------------------------------------------
     *  Private static method {@code noExplicitDiscriminatorKeyOverride} – also
     *  accessed via reflection.
     *  ----------------------------------------------------------------- */
    @Test
    public void testNoExplicitDiscriminatorKeyOverride() throws Exception {
        Method method = BaseJsonValidator.class.getDeclaredMethod(
                "noExplicitDiscriminatorKeyOverride",
                JsonNode.class,
                JsonSchema.class);
        method.setAccessible(true);

        // Mapping that does NOT reference the parent schema path -> should return true
        ObjectNode mapping1 = nf.objectNode().put("cat", "/schemas/cat");
        JsonSchema parent1 = new JsonSchema(null, "/schemas/dog", nf.objectNode());
        boolean result1 = (boolean) method.invoke(null, mapping1, parent1);
        assertTrue(result1);

        // Mapping that references the parent schema path -> should return false
        ObjectNode mapping2 = nf.objectNode().put("dog", "/schemas/dog");
        JsonSchema parent2 = new JsonSchema(null, "/schemas/dog", nf.objectNode());
        boolean result2 = (boolean) method.invoke(null, mapping2, parent2);
        assertFalse(result2);
    }

    /** -----------------------------------------------------------------
     *  Private static method {@code registerAndMergeDiscriminator} – exercised
     *  via reflection. The test verifies:
     *  <ul>
     *      <li>Registration of a root discriminator.</li>
     *      <li>Merging of mappings without redefinition.</li>
     *      <li>Exception on illegal redefinition.</li>
     *  </ul>
     *  ----------------------------------------------------------------- */
    @Test
    public void testRegisterAndMergeDiscriminator() throws Exception {
        Method method = BaseJsonValidator.class.getDeclaredMethod(
                "registerAndMergeDiscriminator",
                ValidationContext.DiscriminatorContext.class,
                ObjectNode.class,
                JsonSchema.class,
                String.class);
        method.setAccessible(true);

        // Prepare a root discriminator with a mapping for "cat"
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode rootDisc = nf.objectNode();
        ObjectNode rootMapping = nf.objectNode().put("cat", "/schemas/cat");
        rootDisc.set("mapping", rootMapping);
        ctx.registerDiscriminator("/root", rootDisc);

        // Child schema that adds a new mapping "dog"
        ObjectNode childDisc = nf.objectNode(); // empty mapping initially
        JsonSchema childSchema = new JsonSchema(null, "/schemas/dog", nf.objectNode());

        // Simulate a nested discriminator on the child schema with its own mapping
        ObjectNode nestedDisc = nf.objectNode();
        ObjectNode nestedMapping = nf.objectNode().put("dog", "/schemas/dog");
        nestedDisc.set("mapping", nestedMapping);
        // Merge – should succeed and result in both "cat" and "dog" mappings present
        method.invoke(null, ctx, rootDisc, childSchema, "testAt");

        ObjectNode merged = ctx.getDiscriminatorForPath("/root");
        assertNotNull(merged);
        JsonNode mergedMappingNode = merged.get("mapping");
        assertNotNull(mergedMappingNode);
        assertEquals("/schemas/cat", mergedMappingNode.get("cat").asText());
        assertEquals("/schemas/dog", mergedMappingNode.get("dog").asText());

        // Now attempt an illegal redefinition: child tries to map "cat" to a different path
        ObjectNode illegalDisc = nf.objectNode();
        ObjectNode illegalMapping = nf.objectNode().put("cat", "/schemas/anotherCat");
        illegalDisc.set("mapping", illegalMapping);
        JsonSchema illegalSchema = new JsonSchema(null, "/schemas/illegalCat", nf.objectNode());

        try {
            method.invoke(null, ctx, rootDisc, illegalSchema, "illegalAt");
            fail("Expected JsonSchemaException due to mapping redefinition");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertTrue(e.getCause() instanceof JsonSchemaException);
        }
    }

    /** -----------------------------------------------------------------
     *  Verify the getters for schemaPath, schemaNode and parentSchema.
     *  ----------------------------------------------------------------- */
    @Test
    public void testGetters() {
        ObjectNode schemaNode = nf.objectNode().put("title", "TestSchema");
        JsonSchema parent = new JsonSchema(null, "/parent", schemaNode);
        MinimalValidator validator = new MinimalValidator(
                "/child",
                schemaNode,
                parent,
                ValidatorTypeCode.TRUE,
                null);

        assertEquals("/child", validator.getSchemaPath());
        assertSame(schemaNode, validator.getSchemaNode());
        assertSame(parent, validator.getParentSchema());
    }

    /** -----------------------------------------------------------------
     *  The {@code validate(ExecutionContext, JsonNode)} overload should delegate
     *  to the three‑argument version with the root path.
     *  ----------------------------------------------------------------- */
    @Test
    public void testValidateOverload() {
        MinimalValidator validator = new MinimalValidator(
                "/root",
                emptyObjectNode,
                null,
                ValidatorTypeCode.TRUE,
                null);
        ExecutionContext execCtx = new ExecutionContext();
        JsonNode data = nf.objectNode();

        Set<ValidationMessage> result = validator.validate(execCtx, data);
        // MinimalValidator returns an empty set for every call.
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
