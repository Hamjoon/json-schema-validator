package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link BaseJsonValidator}.
 *
 * <p>
 * The tests focus on the concrete, non‑abstract behaviour of {@code BaseJsonValidator}
 * (static helpers, protected helpers, and simple instance methods).  A small concrete
 * subclass {@code TestValidator} is defined inside the test class so that we can
 * instantiate the abstract base class without needing the full schema validation
 * machinery.
 * </p>
 *
 * <p>
 * The test suite deliberately avoids any external mocking frameworks – all objects
 * are created through public constructors or factory methods that are part of the
 * library API.
 * </p>
 */
public class BaseJsonValidatorTest {

    /** Simple concrete implementation of {@link BaseJsonValidator}. */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                  ValidatorTypeCode.TRUE, // any validator type – not used in the tests
                  validationContext);
            // overwrite the flag set by the constructor above
            this.suppressSubSchemaRetrieval = suppressSubSchemaRetrieval;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation) {
            // For the purpose of the tests we simply return an empty set.
            return Collections.emptySet();
        }
    }

    /** Minimal {@link ValidationContext} that returns a stub {@link SchemaValidatorsConfig}. */
    private static class StubValidationContext extends ValidationContext {
        private final StubConfig config = new StubConfig();

        StubValidationContext() {
            super(null, null, null);
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return config;
        }

        /** Stub configuration exposing only the path‑type needed by {@code atRoot()}. */
        private static class StubConfig extends SchemaValidatorsConfig {
            private PathType pathType = PathType.LEGACY;

            @Override
            public PathType getPathType() {
                return pathType;
            }

            public void setPathType(PathType pathType) {
                this.pathType = pathType;
            }
        }
    }

    /** Minimal {@link ExecutionContext} whose {@link ExecutionConfig} can be tweaked. */
    private static class StubExecutionContext extends ExecutionContext {
        private final StubExecutionConfig config = new StubExecutionConfig();

        StubExecutionContext() {
            super();
        }

        @Override
        public ExecutionConfig getExecutionConfig() {
            return config;
        }

        /** Stub execution configuration used only for the {@code debug} and
         *  annotation‑collection tests. */
        private static class StubExecutionConfig extends ExecutionConfig {
            private boolean debugEnabled = false;
            private boolean annotationEnabled = false;
            private java.util.function.Predicate<String> annotationFilter = k -> true;

            @Override
            public boolean isDebugEnabled() {
                return debugEnabled;
            }

            @Override
            public boolean isAnnotationCollectionEnabled() {
                return annotationEnabled;
            }

            @Override
            public java.util.function.Predicate<String> getAnnotationCollectionFilter() {
                return annotationFilter;
            }

            public void setDebugEnabled(boolean debugEnabled) {
                this.debugEnabled = debugEnabled;
            }

            public void setAnnotationEnabled(boolean annotationEnabled) {
                this.annotationEnabled = annotationEnabled;
            }

            public void setAnnotationFilter(java.util.function.Predicate<String> filter) {
                this.annotationFilter = filter;
            }
        }
    }

    /** Helper to create a very small {@link JsonSchema} instance used as a parent
     *  schema in a few tests.  The constructor of {@link JsonSchema} is {@code protected},
     *  therefore the test class (being in the same package) can invoke it directly. */
    private static JsonSchema createSimpleJsonSchema(JsonNode schemaNode) {
        // Most arguments are not needed for the behaviours we test, therefore we
        // pass simple defaults (empty collections, nulls, etc.).
        return new JsonSchema(
                Collections.emptyList(),   // validators
                false,                      // validatorsLoaded
                false,                      // recursiveAnchor
                null,                       // typeValidator
                null,                       // id
                false,                      // suppressSubSchemaRetrieval
                schemaNode,                 // schemaNode
                new StubValidationContext(), // validationContext
                null,                       // errorMessageType
                null,                       // errorMessageKeyword
                DefaultMessageSource.getInstance(), // messageSource
                null,                       // keyword
                null,                       // parentSchema
                SchemaLocation.DOCUMENT,    // schemaLocation
                new JsonNodePath(PathType.LEGACY), // evaluationPath
                null,                       // evaluationParentSchema
                new HashMap<>());           // errorMessage map
    }

    @Test
    public void testEqualsDoublePrecision() {
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-10));
        assertTrue(BaseJsonValidator.equals(-5.123456789, -5.123456789000001));
    }

    @Test
    public void testDebugDoesNotThrowWhenDebugDisabled() {
        Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);
        StubExecutionContext execCtx = new StubExecutionContext(); // debug disabled by default
        JsonNode node = JsonNodeFactory.instance.objectNode();
        JsonNode root = JsonNodeFactory.instance.objectNode();
        JsonNodePath instanceLocation = new JsonNodePath(PathType.LEGACY);

        // Should simply return without any exception even though logger.isDebugEnabled()
        // may be false.
        BaseJsonValidator.debug(logger, execCtx, node, root, instanceLocation);
    }

    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode();
        StubValidationContext ctx = new StubValidationContext();

        TestValidator validator = new TestValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.LEGACY),
                schemaNode,
                null,
                ctx,
                true   // suppressSubSchemaRetrieval
        );

        // Because suppression flag is true, the method must return null without
        // trying to resolve a sub‑schema.
        assertNull(validator.fetchSubSchemaNode(ctx));
    }

    @Test
    public void testFetchSubSchemaNodeWithoutIdReturnsNull() {
        // Schema node without an "id" property – obtainSubSchemaNode will return null.
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode();
        StubValidationContext ctx = new StubValidationContext();

        TestValidator validator = new TestValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.LEGACY),
                schemaNode,
                null,
                ctx,
                false   // do not suppress
        );

        assertNull(validator.fetchSubSchemaNode(ctx));
    }

    @Test
    public void testAtRootReturnsCorrectPathInstance() {
        StubValidationContext ctx = new StubValidationContext();

        // Verify LEGACY
        ctx.getConfig().setPathType(PathType.LEGACY);
        TestValidator validatorLegacy = new TestValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.LEGACY),
                JsonNodeFactory.instance.objectNode(),
                null,
                ctx,
                false);
        JsonNodePath rootLegacy = validatorLegacy.atRoot();
        assertEquals(PathType.LEGACY, rootLegacy.getPathType());

        // Verify JSON_POINTER
        ctx.getConfig().setPathType(PathType.JSON_POINTER);
        TestValidator validatorPointer = new TestValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.JSON_POINTER),
                JsonNodeFactory.instance.objectNode(),
                null,
                ctx,
                false);
        JsonNodePath rootPointer = validatorPointer.atRoot();
        assertEquals(PathType.JSON_POINTER, rootPointer.getPathType());

        // Verify JSON_PATH
        ctx.getConfig().setPathType(PathType.JSON_PATH);
        TestValidator validatorPath = new TestValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.JSON_PATH),
                JsonNodeFactory.instance.objectNode(),
                null,
                ctx,
                false);
        JsonNodePath rootPath = validatorPath.atRoot();
        assertEquals(PathType.JSON_PATH, rootPath.getPathType());
    }

    @Test
    public void testGetNodeFieldTypeWhenParentHasType() {
        // Build a parent schema that contains a "type" field.
        ObjectNode parentSchemaNode = JsonNodeFactory.instance.objectNode();
        parentSchemaNode.put("type", "object");
        JsonSchema parentSchema = createSimpleJsonSchema(parentSchemaNode);

        TestValidator validator = new TestValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.LEGACY),
                JsonNodeFactory.instance.objectNode(),
                parentSchema,
                new StubValidationContext(),
                false);

        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenParentHasNoType() {
        JsonSchema parentSchema = createSimpleJsonSchema(JsonNodeFactory.instance.objectNode());

        TestValidator validator = new TestValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.LEGACY),
                JsonNodeFactory.instance.objectNode(),
                parentSchema,
                new StubValidationContext(),
                false);

        assertNull(validator.getNodeFieldType());
    }

    @Test
    public void testHasAdjacentKeywordInEvaluationPathWhenAbsent() {
        // No parent schema – the method should safely return false.
        TestValidator validator = new TestValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.LEGACY),
                JsonNodeFactory.instance.objectNode(),
                null,
                new StubValidationContext(),
                false);

        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("anyKeyword"));
    }

    @Test
    public void testCheckDiscriminatorMatchWithNullPropertyValue() {
        // Create a discriminator object (content does not matter for this path).
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        DiscriminatorContext ctx = new DiscriminatorContext();

        // The method should mark the context as ignored when the property value is null.
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, null);
        assertTrue(ctx.isDiscriminatorIgnore());
    }

    @Test
    public void testCheckForImplicitDiscriminatorMappingMatch() {
        // Build a dummy schema whose fragment name matches the discriminator property value.
        SchemaLocation location = SchemaLocation.of("#/components/schemas/Animal");
        JsonSchema dummySchema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                JsonNodeFactory.instance.objectNode(),
                new StubValidationContext(),
                null,
                null,
                DefaultMessageSource.getInstance(),
                null,
                null,
                location,
                new JsonNodePath(PathType.LEGACY),
                null,
                Collections.emptyMap());

        DiscriminatorContext ctx = new DiscriminatorContext();

        // The fragment name for the location above is "Animal".
        BaseJsonValidator.checkForImplicitDiscriminatorMappingMatch(ctx, "Animal", dummySchema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckForExplicitDiscriminatorMappingMatch() {
        // Prepare a discriminator mapping: {"typeA": "#/components/schemas/TypeA"}
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.put("typeA", "#/components/schemas/TypeA");

        // Build a schema whose fragment is "/components/schemas/TypeA"
        SchemaLocation location = SchemaLocation.of("#/components/schemas/TypeA");
        JsonSchema dummySchema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                JsonNodeFactory.instance.objectNode(),
                new StubValidationContext(),
                null,
                null,
                DefaultMessageSource.getInstance(),
                null,
                null,
                location,
                new JsonNodePath(PathType.LEGACY),
                null,
                Collections.emptyMap());

        DiscriminatorContext ctx = new DiscriminatorContext();

        BaseJsonValidator.checkForExplicitDiscriminatorMappingMatch(ctx, "typeA", mapping, dummySchema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testNoExplicitDiscriminatorKeyOverrideReturnsTrueWhenNoOverlap() {
        // Mapping that does NOT reference the parent schema fragment.
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.put("typeB", "#/components/schemas/TypeB");

        // Parent schema with fragment "/components/schemas/Parent"
        SchemaLocation parentLocation = SchemaLocation.of("#/components/schemas/Parent");
        JsonSchema parentSchema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                JsonNodeFactory.instance.objectNode(),
                new StubValidationContext(),
                null,
                null,
                DefaultMessageSource.getInstance(),
                null,
                null,
                parentLocation,
                new JsonNodePath(PathType.LEGACY),
                null,
                Collections.emptyMap());

        boolean result = BaseJsonValidator.noExplicitDiscriminatorKeyOverride(mapping, parentSchema);
        assertTrue(result);
    }

    @Test
    public void testNoExplicitDiscriminatorKeyOverrideReturnsFalseWhenOverlap() {
        // Mapping that references the parent schema fragment.
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.put("typeParent", "#/components/schemas/Parent");

        // Parent schema with fragment "/components/schemas/Parent"
        SchemaLocation parentLocation = SchemaLocation.of("#/components/schemas/Parent");
        JsonSchema parentSchema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                JsonNodeFactory.instance.objectNode(),
                new StubValidationContext(),
                null,
                null,
                DefaultMessageSource.getInstance(),
                null,
                null,
                parentLocation,
                new JsonNodePath(PathType.LEGACY),
                null,
                Collections.emptyMap());

        boolean result = BaseJsonValidator.noExplicitDiscriminatorKeyOverride(mapping, parentSchema);
        assertFalse(result);
    }

    @Test
    public void testCollectAnnotationsDisabledByDefault() {
        StubExecutionContext execCtx = new StubExecutionContext(); // annotation collection disabled
        TestValidator validator = new TestValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.LEGACY),
                JsonNodeFactory.instance.objectNode(),
                null,
                new StubValidationContext(),
                false);

        assertFalse(validator.collectAnnotations(execCtx));
    }

    @Test
    public void testCollectAnnotationsEnabledAndAcceptedKeyword() {
        StubExecutionContext execCtx = new StubExecutionContext();
        // Enable annotation collection and accept any keyword.
        execCtx.config.setAnnotationEnabled(true);
        execCtx.config.setAnnotationFilter(k -> true);

        TestValidator validator = new TestValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.LEGACY),
                JsonNodeFactory.instance.objectNode(),
                null,
                new StubValidationContext(),
                false);

        assertTrue(validator.collectAnnotations(execCtx));
    }

    @Test
    public void testPutAnnotationAddsEntryToExecutionContext() {
        // Create a minimal execution context with a real annotations holder.
        ExecutionContext execCtx = new ExecutionContext();
        // Ensure the annotation collection filter will accept the keyword.
        execCtx.getExecutionConfig().setAnnotationCollectionEnabled(true);
        execCtx.getExecutionConfig().setAnnotationCollectionFilter(k -> true);

        TestValidator validator = new TestValidator(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.LEGACY),
                JsonNodeFactory.instance.objectNode(),
                null,
                new StubValidationContext(),
                false);

        // Customizer that sets a simple property on the annotation.
        Consumer<JsonNodeAnnotation.Builder> customizer = builder -> builder.property("testProp");

        // Invoke the method under test.
        validator.putAnnotation(execCtx, customizer);

        // Verify that an annotation with the expected property has been stored.
        boolean found = execCtx.getAnnotations().stream()
                .anyMatch(a -> "testProp".equals(a.getProperty()));
        assertTrue("Annotation should have been added to the execution context", found);
    }

    @Test
    public void testToStringReflectsEvaluationPathName() {
        JsonNodePath path = new JsonNodePath(PathType.LEGACY).append("myKeyword");
        TestValidator validator = new TestValidator(
                SchemaLocation.DOCUMENT,
                path,
                JsonNodeFactory.instance.objectNode(),
                null,
                new StubValidationContext(),
                false);

        // The toString implementation delegates to evaluationPath.getName(-1)
        assertEquals("myKeyword", validator.toString());
    }
}
