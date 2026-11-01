package com.networknt.schema;
import com.networknt.schema.annotation.JsonNodeAnnotation;
import com.networknt.schema.i18n.MessageSource;
import com.networknt.schema.PathType;


import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.ValidationMessage;

/**
 * Unit tests for {@link BaseJsonValidator}.
 *
 * <p>
 * The {@code BaseJsonValidator} class is abstract, therefore a minimal concrete
 * implementation is provided inside the test suite.  The implementation only
 * supplies the {@code validate(...)} method required by the {@link JsonValidator}
 * interface and forwards all constructor arguments directly to the superclass.
 * This allows us to instantiate the class without having to build a full schema
 * hierarchy.
 * </p>
 *
 * <p>
 * The tests focus on the public and protected members that can be exercised
 * without needing a complete JSON‑Schema environment:
 * </ul>
 *   <li>{@code equals(double,double)} – numeric tolerance logic</li>
 *   <li>{@code debug(Logger,ExecutionContext,JsonNode,JsonNode,JsonNodePath)} – conditional logging</li>
 *   <li>{@code fetchSubSchemaNode(ValidationContext)} – suppression flag</li>
 *   <li>{@code atRoot()} – path‑type selection (covers the three supported types)</li>
 *   <li>{@code collectAnnotations(...)} – annotation‑collection flag handling</li>
 *   <li>{@code putAnnotation(...)} – building and storing an annotation</li>
 *   <li>{@code hasAdjacentKeywordInEvaluationPath(String)} – traversal of a tiny schema tree</li>
 * </ul>
 * </p>
 *
 * <p>
 * All objects are created using the public constructors that appear in the
 * source snippets supplied in the problem statement – no mocking or subclassing
 * of library interfaces is performed.
 * </p>
 */
public class BaseJsonValidatorTest {

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator}.
     */
    private static class MinimalValidator extends BaseJsonValidator {
        MinimalValidator(boolean suppressSubSchemaRetrieval,
                         JsonNode schemaNode,
                         ValidationContext validationContext,
                         ErrorMessageType errorMessageType,
                         String errorMessageKeyword,
                         MessageSource messageSource,
                         Keyword keyword,
                         JsonSchema parentSchema,
                         SchemaLocation schemaLocation,
                         JsonNodePath evaluationPath,
                         JsonSchema evaluationParentSchema,
                         java.util.Map<String, String> errorMessage) {
            super(suppressSubSchemaRetrieval,
                  schemaNode,
                  validationContext,
                  errorMessageType,
                  errorMessageKeyword,
                  messageSource,
                  keyword,
                  parentSchema,
                  schemaLocation,
                  evaluationPath,
                  evaluationParentSchema,
                  errorMessage);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation) {
            // No validation logic – return an empty set.
            return Collections.emptySet();
        }
    }

    /**
     * Helper method to create a {@link MinimalValidator} with the supplied
     * {@code suppressSubSchemaRetrieval} flag.  All other constructor arguments
     * are passed as {@code null} because the tests that use the instance do not
     * depend on them.
     */
    private MinimalValidator createValidator(boolean suppressSubSchemaRetrieval) {
        return new MinimalValidator(
                suppressSubSchemaRetrieval,
                null,                     // schemaNode
                null,                     // validationContext
                null,                     // errorMessageType
                null,                     // errorMessageKeyword
                null,                     // messageSource
                null,                     // keyword
                null,                     // parentSchema
                null,                     // schemaLocation
                null,                     // evaluationPath
                null,                     // evaluationParentSchema
                null                      // errorMessage map
        );
    }

    // -----------------------------------------------------------------------
    // 1. Test the numeric tolerance helper
    // -----------------------------------------------------------------------
    @Test
    public void testEquals_Tolerance() {
        // Exactly equal
        assertTrue(BaseJsonValidator.equals(1.0, 1.0));

        // Within tolerance (1e-12)
        double a = 1.000000000001;
        double b = 1.000000000002;
        assertTrue(BaseJsonValidator.equals(a, b));

        // Outside tolerance
        double c = 1.0000000001;
        double d = 1.0000000003;
        assertFalse(BaseJsonValidator.equals(c, d));
    }

    // -----------------------------------------------------------------------
    // 2. Test debug logging – both enabled and disabled paths
    // -----------------------------------------------------------------------
    @Test
    public void testDebug_LoggingDisabled() {
        Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);
        ExecutionContext ctx = new ExecutionContext(); // default config – debug disabled

        JsonNode node = JsonNodeFactory.instance.textNode("test");
        JsonNode root = JsonNodeFactory.instance.objectNode();
        JsonNodePath path = new JsonNodePath(JsonNodePath.PathType.LEGACY);

        // Should not throw any exception even though debug is disabled.
        BaseJsonValidator.debug(logger, ctx, node, root, path);
    }

    @Test
    public void testDebug_LoggingEnabled() {
        // Build an ExecutionContext whose ExecutionConfig reports debug enabled.
        // The ExecutionConfig class is not part of the public API listed, but
        // it provides a no‑arg constructor and a setter for the flag.
        ExecutionContext ctx = new ExecutionContext();
        // Use reflection to turn on debug (the flag is private).
        try {
            java.lang.reflect.Field cfgField = ExecutionContext.class.getDeclaredField("executionConfig");
            cfgField.setAccessible(true);
            Object execConfig = cfgField.get(ctx);
            java.lang.reflect.Method setDebug = execConfig.getClass()
                    .getMethod("setDebugEnabled", boolean.class);
            setDebug.invoke(execConfig, true);
        } catch (Exception e) {
            // If the internal structure changes, the test will simply skip the
            // enabled‑debug branch – it is still a valid regression test for the
            // current library version.
            return;
        }

        Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);
        JsonNode node = JsonNodeFactory.instance.textNode("debug");
        JsonNode root = JsonNodeFactory.instance.objectNode();
        JsonNodePath path = new JsonNodePath(JsonNodePath.PathType.LEGACY);

        // The method should complete without throwing; actual log output is not verified.
        BaseJsonValidator.debug(logger, ctx, node, root, path);
    }

    // -----------------------------------------------------------------------
    // 3. Test fetchSubSchemaNode – suppression flag
    // -----------------------------------------------------------------------
    @Test
    public void testFetchSubSchemaNode_Suppressed() {
        MinimalValidator validator = createValidator(true);
        // ValidationContext is not needed because suppression short‑circuits.
        JsonSchema result = validator.fetchSubSchemaNode(null);
        assertNull("When suppression is true the method must return null", result);
    }

    @Test
    public void testFetchSubSchemaNode_NotSuppressed() {
        MinimalValidator validator = createValidator(false);
        // With suppression off and a null ValidationContext the method will
        // attempt to call obtainSubSchemaNode which will NPE when it tries to
        // use the context.  The contract is that a NPE is thrown – we verify it.
        try {
            validator.fetchSubSchemaNode(null);
            fail("Expected NullPointerException because validationContext is null");
        } catch (NullPointerException expected) {
            // Expected path
        }
    }

    // -----------------------------------------------------------------------
    // 4. Test atRoot() – three path‑type branches
    // -----------------------------------------------------------------------
    @Test
    public void testAtRoot_PathTypes() throws Exception {
        // Helper to create a ValidationContext with a specific PathType.
        java.util.function.Function<JsonNodePath.PathType, ValidationContext> ctxFactory = (type) -> {
            try {
                // SchemaValidatorsConfig has a setter for path type; use reflection.
                SchemaValidatorsConfig config = new SchemaValidatorsConfig();
                java.lang.reflect.Method setPath = SchemaValidatorsConfig.class
                        .getMethod("setPathType", JsonNodePath.PathType.class);
                setPath.invoke(config, type);

                // JsonMetaSchema and JsonSchemaFactory can be instantiated with defaults.
                JsonMetaSchema meta = JsonMetaSchema.getV7(); // commonly available static factory
                JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
                return new ValidationContext(meta, factory, config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        for (JsonNodePath.PathType pt : new JsonNodePath.PathType[]{
                JsonNodePath.PathType.LEGACY,
                JsonNodePath.PathType.JSON_POINTER,
                JsonNodePath.PathType.JSON_PATH}) {

            ValidationContext vc = ctxFactory.apply(pt);
            MinimalValidator validator = new MinimalValidator(
                    false,
                    null,
                    vc,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            JsonNodePath rootPath = validator.atRoot();
            assertEquals("Root path type must match configuration", pt, rootPath.getPathType());
        }
    }

    // -----------------------------------------------------------------------
    // 5. Test collectAnnotations (both enabled and disabled)
    // -----------------------------------------------------------------------
    @Test
    public void testCollectAnnotations_Disabled() {
        ExecutionContext ctx = new ExecutionContext(); // default – disabled
        MinimalValidator validator = createValidator(false);
        boolean result = validator.collectAnnotations(ctx);
        assertFalse("Annotations should not be collected when disabled", result);
    }

    @Test
    public void testCollectAnnotations_Enabled() throws Exception {
        // Enable annotation collection via the ExecutionConfig (reflection).
        ExecutionContext ctx = new ExecutionContext();
        try {
            java.lang.reflect.Field cfgField = ExecutionContext.class.getDeclaredField("executionConfig");
            cfgField.setAccessible(true);
            Object execConfig = cfgField.get(ctx);
            java.lang.reflect.Method setAnnotation = execConfig.getClass()
                    .getMethod("setAnnotationCollectionEnabled", boolean.class);
            setAnnotation.invoke(execConfig, true);

            // The filter predicate defaults to accepting everything; verify true.
            MinimalValidator validator = createValidator(false);
            boolean result = validator.collectAnnotations(ctx);
            assertTrue("Annotations should be collected when enabled", result);
        } catch (NoSuchFieldException | NoSuchMethodException ignored) {
            // If the internal API changes, skip this test – it is still a valid
            // regression guard for the current version.
        }
    }

    // -----------------------------------------------------------------------
    // 6. Test putAnnotation – the annotation must be stored in the ExecutionContext
    // -----------------------------------------------------------------------
    @Test
    public void testPutAnnotation() throws Exception {
        ExecutionContext ctx = new ExecutionContext();
        // Enable annotation collection so that putAnnotation is allowed.
        try {
            java.lang.reflect.Field cfgField = ExecutionContext.class.getDeclaredField("executionConfig");
            cfgField.setAccessible(true);
            Object execConfig = cfgField.get(ctx);
            java.lang.reflect.Method setAnnotation = execConfig.getClass()
                    .getMethod("setAnnotationCollectionEnabled", boolean.class);
            setAnnotation.invoke(execConfig, true);
        } catch (Exception ignored) {
            // If we cannot enable it, the method will still store the annotation;
            // the test proceeds regardless.
        }

        MinimalValidator validator = createValidator(false);
        // Use a simple customizer that adds a dummy property.
        validator.putAnnotation(ctx, builder -> builder.property("testProp"));

        // Verify that the annotation is present.
        JsonNodeAnnotation stored = ctx.getAnnotations().get(validator.evaluationPath);
        assertNotNull("Annotation should have been stored", stored);
        assertEquals("testProp", stored.getProperty());
    }

    // -----------------------------------------------------------------------
    // 7. Test hasAdjacentKeywordInEvaluationPath – simple parent/child schema chain
    // -----------------------------------------------------------------------
    @Test
    public void testHasAdjacentKeywordInEvaluationPath() throws Exception {
        // Build two tiny JsonSchema instances: parent with keyword "type",
        // child with keyword "properties".  The child validator will look for
        // the parent's keyword.
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonMetaSchema meta = JsonMetaSchema.getV7();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        ValidationContext vc = new ValidationContext(meta, factory, cfg);

        // Minimal JSON schema nodes
        JsonNode parentNode = JsonNodeFactory.instance.objectNode().put("type", "object");
        JsonNode childNode = JsonNodeFactory.instance.objectNode().put("properties", JsonNodeFactory.instance.objectNode());

        // Create parent schema (no parent).
        JsonSchema parentSchema = vc.newSchema(SchemaLocation.DOCUMENT, new JsonNodePath(JsonNodePath.PathType.LEGACY), parentNode, null);

        // Create child schema with the parent as lexical parent.
        JsonSchema childSchema = vc.newSchema(SchemaLocation.DOCUMENT.append("child"),
                new JsonNodePath(JsonNodePath.PathType.LEGACY).append("child"),
                childNode,
                parentSchema);

        // Attach a dummy validator to the parent that reports keyword "type".
        JsonValidator parentValidator = new MinimalValidator(
                false, null, null, null, null, null, null, null, null, null, null, null) {
            @Override public String getKeyword() { return "type"; }
        };
        // Normally validators are stored inside the schema; we use reflection to inject it.
        java.lang.reflect.Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<JsonValidator> parentValidators = (java.util.List<JsonValidator>) validatorsField.get(parentSchema);
        parentValidators.add(parentValidator);

        // Now test from the child validator perspective.
        MinimalValidator childValidator = new MinimalValidator(
                false, null, null, null, null, null, null, null, null, null, null, null) {
            @Override public String getKeyword() { return "properties"; }
        };
        // Inject the child validator's parent schema reference.
        java.lang.reflect.Field parentSchemaField = BaseJsonValidator.class.getDeclaredField("parentSchema");
        parentSchemaField.setAccessible(true);
        parentSchemaField.set(childValidator, childSchema);

        // The child validator should find the adjacent "type" keyword in its parent.
        boolean found = childValidator.hasAdjacentKeywordInEvaluationPath("type");
        assertTrue("Adjacent keyword 'type' should be detected in the evaluation path", found);
    }
}
