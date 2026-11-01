###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public and protected methods of {@code BaseJsonValidator}</li>
 *   <li>Static utility methods (equals, debug, discriminator handling, …)</li>
 *   <li>Edge‑case and exception scenarios (null values, suppressed sub‑schema retrieval,
 *       discriminator re‑definition, etc.)</li>
 * </ul>
 *
 * The test class creates a concrete subclass {@code TestValidator} that supplies a
 * minimal implementation of the abstract {@code validate(...)} method.  Simple
 * stub implementations of {@code ValidationContext}, {@code ExecutionContext},
 * {@code DiscriminatorContext} and {@code Logger} are provided as inner classes so
 * that the tests do not depend on any external mocking framework.
 *
 * The library classes are used exactly as they appear in the source code – no
 * additional APIs or newer versions are referenced.
 */
package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * Test suite for {@link BaseJsonValidator}.
 */
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple concrete validator used for testing. */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaLocation,
                  evaluationPath,
                  schemaNode,
                  parentSchema,
                  ValidatorTypeCode.TYPE,               // any keyword – not used in the tests
                  validationContext,
                  suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                JsonNodePath instanceLocation) {
            // a no‑op validator – always returns an empty set
            return Collections.emptySet();
        }
    }

    /** Minimal {@link ValidationContext} – only the {@code getConfig()} method is required. */
    private static class MinimalValidationContext extends ValidationContext {
        MinimalValidationContext(SchemaValidatorsConfig config) {
            super(null, null, config);
        }
    }

    /** Minimal {@link ExecutionContext} – only the {@code getExecutionConfig()} method is required. */
    private static class MinimalExecutionContext extends ExecutionContext {
        private final ExecutionConfig execConfig;

        MinimalExecutionContext(ExecutionConfig execConfig) {
            this.execConfig = execConfig;
        }

        @Override
        public ExecutionConfig getExecutionConfig() {
            return execConfig;
        }

        @Override
        public DiscriminatorContext getCurrentDiscriminatorContext() {
            return new SimpleDiscriminatorContext();
        }

        @Override
        public JsonNodeAnnotations getAnnotations() {
            return new JsonNodeAnnotations();
        }
    }

    /** Very small {@link ExecutionConfig} implementation – only the two boolean flags are needed. */
    private static class SimpleExecutionConfig extends ExecutionConfig {
        private boolean debugEnabled = false;
        private boolean annotationCollectionEnabled = false;
        private java.util.function.Predicate<String> annotationFilter = s -> true;

        SimpleExecutionConfig setDebugEnabled(boolean enabled) {
            this.debugEnabled = enabled;
            return this;
        }

        SimpleExecutionConfig setAnnotationCollectionEnabled(boolean enabled) {
            this.annotationCollectionEnabled = enabled;
            return this;
        }

        SimpleExecutionConfig setAnnotationFilter(java.util.function.Predicate<String> filter) {
            this.annotationFilter = filter;
            return this;
        }

        @Override
        public boolean isDebugEnabled() {
            return debugEnabled;
        }

        @Override
        public boolean isAnnotationCollectionEnabled() {
            return annotationCollectionEnabled;
        }

        @Override
        public java.util.function.Predicate<String> getAnnotationCollectionFilter() {
            return annotationFilter;
        }
    }

    /** Very small {@link DiscriminatorContext} that records what happened. */
    private static class SimpleDiscriminatorContext extends DiscriminatorContext {
        private final Map<SchemaLocation, ObjectNode> registry = new HashMap<>();
        private boolean matchFound = false;
        private boolean ignore = false;

        @Override
        public void registerDiscriminator(SchemaLocation schemaLocation, ObjectNode discriminator) {
            registry.put(schemaLocation, discriminator);
        }

        @Override
        public ObjectNode getDiscriminatorForPath(SchemaLocation schemaLocation) {
            return registry.get(schemaLocation);
        }

        @Override
        public void markMatch() {
            this.matchFound = true;
        }

        @Override
        public void markIgnore() {
            this.ignore = true;
        }

        @Override
        public boolean isDiscriminatorMatchFound() {
            return matchFound;
        }

        @Override
        public boolean isDiscriminatorIgnore() {
            return ignore;
        }
    }

    /** Simple {@link Logger} implementation that captures the last debug message. */
    private static class CapturingLogger implements Logger {
        String lastDebugMessage = null;
        boolean debugEnabled = false;

        void setDebugEnabled(boolean enabled) {
            this.debugEnabled = enabled;
        }

        @Override public String getName() { return "capturing"; }
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
        @Override public void debug(String msg) { lastDebugMessage = msg; }
        @Override public void debug(String format, Object arg) { lastDebugMessage = String.format(format, arg); }
        @Override public void debug(String format, Object arg1, Object arg2) { lastDebugMessage = String.format(format, arg1, arg2); }
        @Override public void debug(String format, Object... arguments) { lastDebugMessage = String.format(format, arguments); }
        @Override public void debug(String msg, Throwable t) { lastDebugMessage = msg; }
        @Override public boolean isDebugEnabled(Marker marker) { return debugEnabled; }
        @Override public void debug(Marker marker, String msg) { lastDebugMessage = msg; }
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

    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;
    private ObjectNode simpleSchemaNode;
    private TestValidator validator;
    private SimpleExecutionConfig execConfig;
    private MinimalValidationContext validationContext;

    @Before
    public void setUp() {
        schemaLocation = SchemaLocation.of("http://example.com/schema");
        evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        simpleSchemaNode = MAPPER.createObjectNode();
        simpleSchemaNode.put("type", "object");

        // configuration that uses JSON_POINTER as the path type (required for atRoot())
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setPathType(PathType.JSON_POINTER);

        validationContext = new MinimalValidationContext(cfg);
        execConfig = new SimpleExecutionConfig()
                .setDebugEnabled(false)
                .setAnnotationCollectionEnabled(false);

        // parent schema is null for most tests – we will create a parent where needed
        validator = new TestValidator(schemaLocation, evaluationPath, simpleSchemaNode, null,
                validationContext, false);
    }

    /** -----------------------------------------------------------------------
     *  Public / protected instance methods
     * ----------------------------------------------------------------------- */

    @Test
    public void testGetSchemaLocation() {
        assertEquals(schemaLocation, validator.getSchemaLocation());
    }

    @Test
    public void testGetEvaluationPath() {
        assertEquals(evaluationPath, validator.getEvaluationPath());
    }

    @Test
    public void testGetKeyword() {
        // The keyword for the validator is the value of the supplied ValidatorTypeCode (TYPE)
        assertEquals(ValidatorTypeCode.TYPE.getValue(), validator.getKeyword());
    }

    @Test
    public void testGetSchemaNode() {
        assertSame(simpleSchemaNode, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchemaWhenNull() {
        assertNull(validator.getParentSchema());
    }

    @Test
    public void testGetEvaluationParentSchemaFallsBackToParent() {
        assertNull(validator.getEvaluationParentSchema());
    }

    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        TestValidator suppressed = new TestValidator(schemaLocation, evaluationPath,
                simpleSchemaNode, null, validationContext, true);
        assertNull(suppressed.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNodeNoId() {
        // schema node does not contain an "id" field – obtainSubSchemaNode should return null
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testValidateConvenienceMethodCallsRootValidate() {
        // The convenience overload should delegate to the 4‑argument version.
        // Because our validator returns an empty set we just verify that no exception is thrown.
        ExecutionContext ctx = new MinimalExecutionContext(execConfig);
        Set<ValidationMessage> result = validator.validate(ctx, simpleSchemaNode);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetNodeFieldTypeFoundInParent() {
        // Build a parent schema that contains a "type" field
        ObjectNode parentNode = MAPPER.createObjectNode();
        parentNode.put("type", "array");

        TestValidator parent = new TestValidator(schemaLocation, evaluationPath, parentNode,
                null, validationContext, false);
        // child validator that uses the parent we just built
        TestValidator child = new TestValidator(schemaLocation, evaluationPath, simpleSchemaNode,
                parent, validationContext, false);

        assertEquals("array", child.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeMissing() {
        // parent schema without a "type" field
        ObjectNode parentNode = MAPPER.createObjectNode();
        TestValidator parent = new TestValidator(schemaLocation, evaluationPath, parentNode,
                null, validationContext, false);
        TestValidator child = new TestValidator(schemaLocation, evaluationPath, simpleSchemaNode,
                parent, validationContext, false);
        assertNull(child.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemasInvokesInitializeValidators() {
        // Create two dummy schemas that record whether initializeValidators() was called
        class DummySchema extends JsonSchema {
            boolean initialized = false;
            DummySchema() {
                super(Collections.emptyList(), true, false, null, null, false,
                        MAPPER.createObjectNode(),
                        validationContext,
                        ValidatorTypeCode.TYPE,
                        null,
                        DefaultMessageSource.getInstance(),
                        ValidatorTypeCode.TYPE,
                        null,
                        schemaLocation,
                        evaluationPath,
                        null,
                        null);
            }
            @Override public void initializeValidators() { initialized = true; }
        }

        DummySchema s1 = new DummySchema();
        DummySchema s2 = new DummySchema();

        validator.preloadJsonSchemas(Arrays.asList(s1, s2));

        assertTrue(s1.initialized);
        assertTrue(s2.initialized);
    }

    @Test
    public void testAtRootRespectsPathTypeFromConfig() {
        JsonNodePath root = validator.atRoot();
        // The config we supplied uses JSON_POINTER, therefore the returned instance must be the singleton
        assertSame(BaseJsonValidator.JsonNodePathJsonPointer.getInstance(), root);
    }

    @Test
    public void testToStringUsesEvaluationPathName() {
        // evaluationPath is a JsonNodePath; its name(-1) for an empty path is an empty string
        assertEquals("", validator.toString());
    }

    @Test
    public void testHasAdjacentKeywordInEvaluationPathFound() {
        // Create a parent schema that contains a validator with keyword "type"
        TestValidator parent = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false) {
            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                    JsonNode node,
                                                    JsonNode rootNode,
                                                    JsonNodePath instanceLocation) {
                return Collections.emptySet();
            }

            @Override
            public List<JsonValidator> getValidators() {
                // Return a single validator whose keyword matches the searched one
                JsonValidator dummy = new TestValidator(schemaLocation, evaluationPath,
                        MAPPER.createObjectNode(), null, validationContext, false) {
                    @Override public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode n,
                                                                    JsonNode r, JsonNodePath p) {
                        return Collections.emptySet();
                    }

                    @Override public String getKeyword() { return "type"; }
                };
                return Collections.singletonList(dummy);
            }
        };

        // child validator uses the above parent as its evaluation parent
        TestValidator child = new TestValidator(schemaLocation, evaluationPath,
                simpleSchemaNode, parent, validationContext, false);

        assertTrue(child.hasAdjacentKeywordInEvaluationPath("type"));
    }

    @Test
    public void testHasAdjacentKeywordInEvaluationPathNotFound() {
        // Parent schema without any validators
        TestValidator parent = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false) {
            @Override public List<JsonValidator> getValidators() { return Collections.emptyList(); }
        };
        TestValidator child = new TestValidator(schemaLocation, evaluationPath,
                simpleSchemaNode, parent, validationContext, false);
        assertFalse(child.hasAdjacentKeywordInEvaluationPath("unknown"));
    }

    /** -----------------------------------------------------------------------
     *  Static utility methods
     * ----------------------------------------------------------------------- */

    @Test
    public void testEqualsCloseNumbers() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.000000000002));
    }

    @Test
    public void testEqualsDifferentNumbers() {
        assertFalse(BaseJsonValidator.equals(1.0, 2.0));
    }

    @Test
    public void testDebugWhenEnabledAndDebugFlagOn() {
        CapturingLogger logger = new CapturingLogger();
        logger.setDebugEnabled(true);
        execConfig.setDebugEnabled(true);

        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        JsonNode root = MAPPER.createObjectNode().put("b", 2);
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        BaseJsonValidator.debug(logger, new MinimalExecutionContext(execConfig), node, root, instanceLocation);
        assertNotNull(logger.lastDebugMessage);
        assertTrue(logger.lastDebugMessage.contains("validate("));
    }

    @Test
    public void testDebugWhenDisabledDoesNotLog() {
        CapturingLogger logger = new CapturingLogger();
        logger.setDebugEnabled(true);
        execConfig.setDebugEnabled(false); // execution config disables debug

        JsonNode node = MAPPER.createObjectNode();
        JsonNode root = MAPPER.createObjectNode();
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);

        BaseJsonValidator.debug(logger, new MinimalExecutionContext(execConfig), node, root, instanceLocation);
        assertNull(logger.lastDebugMessage);
    }

    /** -----------------------------------------------------------------------
     *  Discriminator handling – edge cases & exception scenarios
     * ----------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatchNullPropertyValueMarksIgnore() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, null);
        assertTrue(ctx.isDiscriminatorIgnore());
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitMappingMatch() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();

        // parent schema whose fragment name matches the discriminator property value
        SchemaLocation childLoc = SchemaLocation.of("http://example.com/schema#MyType");
        JsonNodePath childPath = new JsonNodePath(PathType.JSON_POINTER);
        ObjectNode childSchemaNode = MAPPER.createObjectNode();
        JsonSchema childSchema = new TestValidator(childLoc, childPath, childSchemaNode,
                null, validationContext, false);

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "MyType", childSchema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingMatch() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        // mapping "dog" -> "#/DogSchema"
        mapping.put("dog", "#/DogSchema");
        discriminator.set("mapping", mapping);

        // child schema with fragment "DogSchema"
        SchemaLocation childLoc = SchemaLocation.of("http://example.com/schema#DogSchema");
        JsonSchema childSchema = new TestValidator(childLoc, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false);

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "dog", childSchema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testRegisterAndMergeDiscriminatorRedefinePropertyNameThrows() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();

        // root discriminator (already registered)
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        rootDiscriminator.put("propertyName", "type");
        SchemaLocation rootLoc = SchemaLocation.of("http://example.com/root");
        ctx.registerDiscriminator(rootLoc, rootDiscriminator);

        // child schema that also defines a propertyName – this should cause an exception
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        childDiscriminator.put("propertyName", "type");
        SchemaLocation childLoc = SchemaLocation.of("http://example.com/child");

        JsonSchema childSchema = new TestValidator(childLoc, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false);

        try {
            BaseJsonValidator.registerAndMergeDiscriminator(ctx, childDiscriminator, childSchema,
                    new JsonNodePath(PathType.JSON_POINTER));
            fail("Expected JsonSchemaException due to propertyName redefinition");
        } catch (JsonSchemaException e) {
            // expected
        }
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesMappingsWithoutConflict() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();

        // root discriminator with an initial mapping
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode rootMapping = MAPPER.createObjectNode();
        rootMapping.put("cat", "#/CatSchema");
        rootDiscriminator.set("mapping", rootMapping);
        SchemaLocation rootLoc = SchemaLocation.of("http://example.com/root");
        ctx.registerDiscriminator(rootLoc, rootDiscriminator);

        // child schema adds a new mapping "dog"
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        ObjectNode childMapping = MAPPER.createObjectNode();
        childMapping.put("dog", "#/DogSchema");
        childDiscriminator.set("mapping", childMapping);
        SchemaLocation childLoc = SchemaLocation.of("http://example.com/child");

        JsonSchema childSchema = new TestValidator(childLoc, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false);

        // This should merge the two mappings without throwing
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, childDiscriminator,
                childSchema, new JsonNodePath(PathType.JSON_POINTER));

        ObjectNode merged = ctx.getDiscriminatorForPath(rootLoc);
        assertNotNull(merged);
        JsonNode mergedMapping = merged.get("mapping");
        assertNotNull(mergedMapping);
        assertEquals("#/CatSchema", mergedMapping.get("cat").asText());
        assertEquals("#/DogSchema", mergedMapping.get("dog").asText());
    }

    /** -----------------------------------------------------------------------
     *  Annotation collection helpers – edge cases
     * ----------------------------------------------------------------------- */

    @Test
    public void testCollectAnnotationsEnabledAndKeywordAccepted() {
        execConfig.setAnnotationCollectionEnabled(true)
                  .setAnnotationFilter(k -> k.equals("type"));
        MinimalExecutionContext ctx = new MinimalExecutionContext(execConfig);
        assertTrue(validator.collectAnnotations(ctx, "type"));
    }

    @Test
    public void testCollectAnnotationsDisabled() {
        execConfig.setAnnotationCollectionEnabled(false);
        MinimalExecutionContext ctx = new MinimalExecutionContext(execConfig);
        assertFalse(validator.collectAnnotations(ctx, "type"));
    }

    @Test
    public void testCollectAnnotationsKeywordFilteredOut() {
        execConfig.setAnnotationCollectionEnabled(true)
                  .setAnnotationFilter(k -> false);
        MinimalExecutionContext ctx = new MinimalExecutionContext(execConfig);
        assertFalse(validator.collectAnnotations(ctx, "type"));
    }

    /** -----------------------------------------------------------------------
     *  putAnnotation – ensures the builder is populated correctly
     * ----------------------------------------------------------------------- */

    @Test
    public void testPutAnnotationCreatesAnnotationWithCorrectMetadata() {
        // Use a simple execution context that records the annotation
        MinimalExecutionContext ctx = new MinimalExecutionContext(execConfig);
        validator.putAnnotation(ctx, b -> b.value(MAPPER.createObjectNode().put("x", 1)));

        JsonNodeAnnotation ann = ctx.getAnnotations().get(validator.getEvaluationPath());
        assertNotNull(ann);
        assertEquals(validator.getEvaluationPath(), ann.getEvaluationPath());
        assertEquals(validator.getSchemaLocation(), ann.getSchemaLocation());
        assertEquals(validator.getKeyword(), ann.getKeyword());
        // the custom value we added
        assertEquals(1, ann.getValue().get("x").asInt());
    }
}
```
###Test END##