###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.i18n.DefaultMessageSource;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link BaseJsonValidator}.  The concrete class {@link TestValidator}
 * is used to instantiate the abstract {@code BaseJsonValidator}.
 */
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;
    private ObjectNode schemaNode;
    private JsonSchema parentSchema;
    private ExecutionContext executionContext;
    private TestLogger logger;

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} used only for testing.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                  validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation) {
            // simple validator – always returns an empty set
            return Collections.emptySet();
        }
    }

    /**
     * Simple {@link Logger} implementation that records whether {@code debug(String)} was called.
     */
    private static class TestLogger implements Logger {
        private final List<String> messages = new ArrayList<>();
        private boolean debugEnabled = false;

        void setDebugEnabled(boolean enabled) {
            this.debugEnabled = enabled;
        }

        boolean isDebugEnabled() {
            return debugEnabled;
        }

        List<String> getMessages() {
            return messages;
        }

        @Override public String getName() { return "TestLogger"; }
        @Override public boolean isTraceEnabled() { return false; }
        @Override public void trace(String msg) {}
        @Override public void trace(String format, Object arg) {}
        @Override public void trace(String format, Object arg1, Object arg2) {}
        @Override public void trace(String format, Object... arguments) {}
        @Override public void trace(String msg, Throwable t) {}
        @Override public boolean isTraceEnabled(Marker marker) { return false; }
        @Override public void trace(Marker marker, String msg) {}
        @Override public void trace(Marker marker, String format, Object arg) {}
        @Override public void trace(Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void trace(Marker marker, String format, Object... argArray) {}
        @Override public void trace(Marker marker, String msg, Throwable t) {}

        @Override public boolean isDebugEnabled() { return debugEnabled; }
        @Override public void debug(String msg) { messages.add(msg); }
        @Override public void debug(String format, Object arg) { messages.add(String.format(format, arg)); }
        @Override public void debug(String format, Object arg1, Object arg2) { messages.add(String.format(format, arg1, arg2)); }
        @Override public void debug(String format, Object... arguments) { messages.add(String.format(format, arguments)); }
        @Override public void debug(String msg, Throwable t) { messages.add(msg + " " + t); }
        @Override public boolean isDebugEnabled(Marker marker) { return debugEnabled; }
        @Override public void debug(Marker marker, String msg) {}
        @Override public void debug(Marker marker, String format, Object arg) {}
        @Override public void debug(Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void debug(Marker marker, String format, Object... arguments) {}
        @Override public void debug(Marker marker, String msg, Throwable t) {}

        @Override public boolean isInfoEnabled() { return false; }
        @Override public void info(String msg) {}
        @Override public void info(String format, Object arg) {}
        @Override public void info(String format, Object arg1, Object arg2) {}
        @Override public void info(String format, Object... arguments) {}
        @Override public void info(String msg, Throwable t) {}
        @Override public boolean isInfoEnabled(Marker marker) { return false; }
        @Override public void info(Marker marker, String msg) {}
        @Override public void info(Marker marker, String format, Object arg) {}
        @Override public void info(Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void info(Marker marker, String format, Object... arguments) {}
        @Override public void info(Marker marker, String msg, Throwable t) {}

        @Override public boolean isWarnEnabled() { return false; }
        @Override public void warn(String msg) {}
        @Override public void warn(String format, Object arg) {}
        @Override public void warn(String format, Object... arguments) {}
        @Override public void warn(String format, Object arg1, Object arg2) {}
        @Override public void warn(String msg, Throwable t) {}
        @Override public boolean isWarnEnabled(Marker marker) { return false; }
        @Override public void warn(Marker marker, String msg) {}
        @Override public void warn(Marker marker, String format, Object arg) {}
        @Override public void warn(Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void warn(Marker marker, String format, Object... arguments) {}
        @Override public void warn(Marker marker, String msg, Throwable t) {}

        @Override public boolean isErrorEnabled() { return false; }
        @Override public void error(String msg) {}
        @Override public void error(String format, Object arg) {}
        @Override public void error(String format, Object arg1, Object arg2) {}
        @Override public void error(String format, Object... arguments) {}
        @Override public void error(String msg, Throwable t) {}
        @Override public boolean isErrorEnabled(Marker marker) { return false; }
        @Override public void error(Marker marker, String msg) {}
        @Override public void error(Marker marker, String format, Object arg) {}
        @Override public void error(Marker marker, String format, Object arg1, Object arg2) {}
        @Override public void error(Marker marker, String format, Object... arguments) {}
        @Override public void error(Marker marker, String msg, Throwable t) {}
    }

    @Before
    public void setUp() {
        // Build a simple ValidationContext – use the default V7 meta‑schema and factory.
        JsonMetaSchema meta = JsonMetaSchema.getV7();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(meta, factory, config);

        schemaLocation = SchemaLocation.of("http://example.com/schema");
        evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        schemaNode = MAPPER.createObjectNode();
        parentSchema = null; // not needed for the majority of tests

        executionContext = new ExecutionContext();
        logger = new TestLogger();
    }

    /* ----------------------------------------------------------------------
     *  Public method signatures (for reference)
     * ---------------------------------------------------------------------- */
    // - BaseJsonValidator(SchemaLocation, JsonNodePath, JsonNode, JsonSchema,
    //   ValidatorTypeCode, ValidationContext)
    // - BaseJsonValidator(SchemaLocation, JsonNodePath, JsonNode, JsonSchema,
    //   ErrorMessageType, Keyword, ValidationContext, boolean)
    // - protected BaseJsonValidator(boolean, JsonNode, ValidationContext,
    //   ErrorMessageType, String, MessageSource, Keyword, JsonSchema,
    //   SchemaLocation, JsonNodePath, JsonSchema, Map<String,String>)
    // - public static void debug(Logger, ExecutionContext, JsonNode, JsonNode,
    //   JsonNodePath)
    // - public static boolean equals(double, double)
    // - public SchemaLocation getSchemaLocation()
    // - public JsonNodePath getEvaluationPath()
    // - public String getKeyword()
    // - public JsonNode getSchemaNode()
    // - public JsonSchema getParentSchema()
    // - public JsonSchema getEvaluationParentSchema()
    // - protected JsonSchema fetchSubSchemaNode(ValidationContext)
    // - public Set<ValidationMessage> validate(ExecutionContext, JsonNode)
    // - protected String getNodeFieldType()
    // - protected void preloadJsonSchemas(Collection<JsonSchema>)
    // - protected JsonNodePath atRoot()
    // - public String toString()
    // - protected boolean hasAdjacentKeywordInEvaluationPath(String)
    // - protected MessageSourceValidationMessage.Builder message()
    // - protected boolean collectAnnotations(ExecutionContext)
    // - protected boolean collectAnnotations(ExecutionContext, String)
    // - protected void putAnnotation(ExecutionContext, Consumer<JsonNodeAnnotation.Builder>)

    /* ----------------------------------------------------------------------
     *  Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testGettersAndToString() {
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                schemaNode, parentSchema, ValidatorTypeCode.TRUE, validationContext);

        assertEquals(schemaLocation, validator.getSchemaLocation());
        assertEquals(evaluationPath, validator.getEvaluationPath());
        assertEquals(ValidatorTypeCode.TRUE.getValue(), validator.getKeyword());
        assertEquals(schemaNode, validator.getSchemaNode());
        assertNull(validator.getParentSchema());
        // toString() returns the last element of the evaluation path – which is empty for root
        assertEquals("", validator.toString());
    }

    @Test
    public void testEqualsUtility() {
        // values within tolerance
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.0));
        // values outside tolerance
        assertFalse(BaseJsonValidator.equals(1.1, 1.0));
        // exact equality
        assertTrue(BaseJsonValidator.equals(5.0, 5.0));
    }

    @Test
    public void testDebugWhenEnabled() {
        // enable debug on both execution config and logger
        ExecutionConfig execConfig = new ExecutionConfig();
        execConfig.setDebugEnabled(true);
        executionContext.setExecutionConfig(execConfig);
        logger.setDebugEnabled(true);

        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        JsonNode root = MAPPER.createObjectNode().set("root", node);
        JsonNodePath path = new JsonNodePath(PathType.JSON_POINTER).append("/a");

        BaseJsonValidator.debug(logger, executionContext, node, root, path);

        assertFalse(logger.getMessages().isEmpty());
        String logged = logger.getMessages().get(0);
        assertTrue(logged.contains("validate("));
        assertTrue(logged.contains(node.toString()));
        assertTrue(logged.contains(root.toString()));
        assertTrue(logged.contains(path.toString()));
    }

    @Test
    public void testDebugWhenDisabled() {
        // both sides disabled – no message should be recorded
        ExecutionConfig execConfig = new ExecutionConfig();
        execConfig.setDebugEnabled(false);
        executionContext.setExecutionConfig(execConfig);
        logger.setDebugEnabled(false);

        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        JsonNode root = MAPPER.createObjectNode().set("root", node);
        JsonNodePath path = new JsonNodePath(PathType.JSON_POINTER).append("/a");

        BaseJsonValidator.debug(logger, executionContext, node, root, path);
        assertTrue(logger.getMessages().isEmpty());
    }

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        // suppressSubSchemaRetrieval = true => always null
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                schemaNode, parentSchema, ValidatorTypeCode.TRUE, validationContext) {
            @Override
            protected JsonSchema fetchSubSchemaNode(ValidationContext ctx) {
                // force the protected field to true for this test
                return super.fetchSubSchemaNode(ctx);
            }
        };
        // use reflection to set the protected flag (allowed in tests)
        try {
            java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("suppressSubSchemaRetrieval");
            f.setAccessible(true);
            f.setBoolean(validator, true);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNodeWhenNoId() {
        // schemaNode without an "id" field – should return null
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                schemaNode, parentSchema, ValidatorTypeCode.TRUE, validationContext);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testGetEvaluationParentSchemaFallback() {
        // evaluationParentSchema is null – should fallback to parentSchema
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                schemaNode, parentSchema, ValidatorTypeCode.TRUE, validationContext);
        assertNull(validator.getEvaluationParentSchema()); // both are null
    }

    @Test
    public void testAtRootRespectsConfigPathType() {
        // configure JSON_POINTER path type
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setPathType(PathType.JSON_POINTER);
        ValidationContext ctx = new ValidationContext(JsonMetaSchema.getV7(),
                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7), cfg);
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                schemaNode, parentSchema, ValidatorTypeCode.TRUE, ctx);

        JsonNodePath rootPath = validator.atRoot();
        assertEquals(PathType.JSON_POINTER, rootPath.getPathType());
    }

    /* ----------------------------------------------------------------------
     *  Edge case / exception handling tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testRegisterAndMergeDiscriminator_NoRedefinition() {
        // Build a minimal discriminator object with a mapping
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.set("typeA", MAPPER.textNode("#/definitions/typeA"));
        rootDiscriminator.set("mapping", mapping);

        // Create a dummy JsonSchema whose schemaLocation fragment equals "typeA"
        SchemaLocation childLoc = SchemaLocation.of("http://example.com/schema#typeA");
        JsonSchema childSchema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                MAPPER.createObjectNode(),
                validationContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                childLoc,
                new JsonNodePath(PathType.JSON_POINTER),
                null,
                Collections.emptyMap()
        );

        // Discriminator context
        DiscriminatorContext ctx = new DiscriminatorContext();

        // Call the protected static method via reflection (since it is protected)
        try {
            java.lang.reflect.Method m = BaseJsonValidator.class
                    .getDeclaredMethod("registerAndMergeDiscriminator",
                            DiscriminatorContext.class, ObjectNode.class, JsonSchema.class, JsonNodePath.class);
            m.setAccessible(true);
            m.invoke(null, ctx, rootDiscriminator, childSchema,
                    new JsonNodePath(PathType.JSON_POINTER));
        } catch (Exception e) {
            fail("Invocation failed: " + e);
        }

        // After registration the context should contain the discriminator for the child location
        ObjectNode stored = ctx.getDiscriminatorForPath(childLoc);
        assertNotNull(stored);
        assertEquals(rootDiscriminator, stored);
    }

    @Test
    public void testCheckDiscriminatorMatch_IgnoreWhenPropertyValueNull() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        JsonSchema dummySchema = null; // not used when property value is null

        try {
            java.lang.reflect.Method m = BaseJsonValidator.class
                    .getDeclaredMethod("checkDiscriminatorMatch",
                            DiscriminatorContext.class, ObjectNode.class, String.class, JsonSchema.class);
            m.setAccessible(true);
            m.invoke(null, ctx, discriminator, null, dummySchema);
        } catch (Exception e) {
            fail("Invocation failed: " + e);
        }

        assertTrue(ctx.isDiscriminatorIgnore());
    }

    /* ----------------------------------------------------------------------
     *  Helper methods for building ExecutionConfig (not part of public API)
     * ---------------------------------------------------------------------- */

    // The ExecutionConfig class is part of the library but not listed in the
    // supplied API.  It provides setters for the flags used in the tests.
    private static class ExecutionConfig {
        private boolean debugEnabled = false;
        private boolean annotationCollectionEnabled = false;
        private java.util.function.Predicate<String> annotationFilter = s -> true;

        public boolean isDebugEnabled() {
            return debugEnabled;
        }

        public void setDebugEnabled(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
        }

        public boolean isAnnotationCollectionEnabled() {
            return annotationCollectionEnabled;
        }

        public void setAnnotationCollectionEnabled(boolean annotationCollectionEnabled) {
            this.annotationCollectionEnabled = annotationCollectionEnabled;
        }

        public java.util.function.Predicate<String> getAnnotationCollectionFilter() {
            return annotationFilter;
        }

        public void setAnnotationCollectionFilter(java.util.function.Predicate<String> filter) {
            this.annotationFilter = filter;
        }
    }
}
```
###Test END##