package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * <p>The tests are organised in three logical groups:
 *
 * <ul>
 *   <li><b>Public instance methods</b> – basic happy‑path behaviour.</li>
 *   <li><b>Static utility methods</b> – correctness of the helper logic.</li>
 *   <li><b>Edge‑case / exception scenarios</b> – inputs that trigger the special
 *       handling paths inside the class.</li>
 * </ul>
 *
 * <p>Because {@code BaseJsonValidator} is abstract a tiny concrete subclass
 * {@code TestValidator} is defined inside the test class.  The implementation
 * of {@code validate(...)} simply returns an empty {@link Set} – this is enough
 * for exercising the non‑abstract methods without pulling in the whole schema
 * validation machinery.
 *
 * <p>All objects are created using the public constructors that appear in the
 * supplied source code; no external mocking frameworks are used.
 */
public class BaseJsonValidatorTest {

    /** Simple concrete validator used for testing the non‑abstract behaviour. */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval,
                      Keyword keyword) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                  ValidatorTypeCode.TRUE,                 // errorMessageType – any enum works
                  keyword,
                  validationContext,
                  suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation) {
            // No real validation – just an empty set.
            return Collections.emptySet();
        }
    }

    /** Minimal {@link JsonSchema} implementation that only supplies a schema node. */
    private static class MinimalJsonSchema extends JsonSchema {
        MinimalJsonSchema(JsonNode schemaNode,
                          ValidationContext validationContext,
                          Keyword keyword,
                          JsonSchema parentSchema,
                          SchemaLocation schemaLocation,
                          JsonNodePath evaluationPath) {
            super(Collections.emptyList(),          // validators
                  false,                            // validatorsLoaded
                  false,                            // recursiveAnchor
                  null,                             // typeValidator
                  null,                             // id
                  false,                            // suppressSubSchemaRetrieval
                  schemaNode,
                  validationContext,
                  ValidatorTypeCode.TRUE,          // errorMessageType
                  null,                             // errorMessageKeyword
                  DefaultMessageSource.getInstance(),
                  keyword,
                  parentSchema,
                  schemaLocation,
                  evaluationPath,
                  null,                             // evaluationParentSchema
                  null);                            // errorMessage map
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private JsonNodePath evaluationPath;
    private SchemaLocation schemaLocation;
    private Keyword dummyKeyword;
    private Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);

    @Before
    public void setUp() {
        // --------------------------------------------------------------------
        // A very small ValidationContext – only the configuration part is used.
        // --------------------------------------------------------------------
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        // Use LEGACY as the default; individual tests can change it.
        config.setPathType(PathType.LEGACY);
        validationContext = new ValidationContext(null, null, config);

        // --------------------------------------------------------------------
        // ExecutionContext with a default ExecutionConfig (all flags false).
        // --------------------------------------------------------------------
        ExecutionConfig execConfig = new ExecutionConfig();
        executionContext = new ExecutionContext(execConfig);

        // --------------------------------------------------------------------
        // Common objects used by many tests.
        // --------------------------------------------------------------------
        evaluationPath = new JsonNodePath(PathType.LEGACY);
        schemaLocation = SchemaLocation.DOCUMENT;

        dummyKeyword = new Keyword() {
            @Override public String getValue() { return "dummy"; }
            @Override
            public JsonValidator newValidator(SchemaLocation sl, JsonNodePath enp,
                                              JsonNode sn, JsonSchema ps) {
                return null;
            }
        };
    }

    // ------------------------------------------------------------------------
    // 1.  Public instance methods – basic behaviour
    // ------------------------------------------------------------------------

    @Test
    public void testGetSchemaLocation() {
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false, dummyKeyword);
        assertSame(schemaLocation, validator.getSchemaLocation());
    }

    @Test
    public void testGetEvaluationPath() {
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false, dummyKeyword);
        assertSame(evaluationPath, validator.getEvaluationPath());
    }

    @Test
    public void testGetKeyword() {
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false, dummyKeyword);
        assertEquals("dummy", validator.getKeyword());
    }

    @Test
    public void testGetSchemaNode() {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("type", "object");
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                schemaNode, null, validationContext, false, dummyKeyword);
        assertSame(schemaNode, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema() {
        MinimalJsonSchema parent = new MinimalJsonSchema(MAPPER.createObjectNode(),
                validationContext, dummyKeyword, null,
                SchemaLocation.DOCUMENT, new JsonNodePath(PathType.LEGACY));
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), parent, validationContext, false, dummyKeyword);
        assertSame(parent, validator.getParentSchema());
    }

    @Test
    public void testGetEvaluationParentSchemaWhenNull() {
        MinimalJsonSchema parent = new MinimalJsonSchema(MAPPER.createObjectNode(),
                validationContext, dummyKeyword, null,
                SchemaLocation.DOCUMENT, new JsonNodePath(PathType.LEGACY));
        // evaluationParentSchema is null – the method should fall back to parentSchema
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), parent, validationContext, false, dummyKeyword);
        assertSame(parent, validator.getEvaluationParentSchema());
    }

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext,
                true,                         // suppressSubSchemaRetrieval = true
                dummyKeyword);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testValidateConvenienceMethod() {
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false, dummyKeyword);
        JsonNode data = MAPPER.createObjectNode().put("a", 1);
        Set<ValidationMessage> result = validator.validate(executionContext, data);
        // Our stub validator returns an empty set.
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetNodeFieldTypeWhenPresent() {
        ObjectNode parentSchemaNode = MAPPER.createObjectNode().put("type", "string");
        MinimalJsonSchema parent = new MinimalJsonSchema(parentSchemaNode,
                validationContext, dummyKeyword, null,
                SchemaLocation.DOCUMENT, new JsonNodePath(PathType.LEGACY));
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), parent, validationContext, false, dummyKeyword);
        assertEquals("string", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenAbsent() {
        MinimalJsonSchema parent = new MinimalJsonSchema(MAPPER.createObjectNode(),
                validationContext, dummyKeyword, null,
                SchemaLocation.DOCUMENT, new JsonNodePath(PathType.LEGACY));
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), parent, validationContext, false, dummyKeyword);
        assertNull(validator.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemasInvokesInitializeValidators() {
        // A JsonSchema that records whether initializeValidators() was called.
        class SpySchema extends JsonSchema {
            boolean initialized = false;
            SpySchema(JsonNode schemaNode) {
                super(Collections.emptyList(), false, false, null, null,
                      false, schemaNode, validationContext,
                      ValidatorTypeCode.TRUE, null,
                      DefaultMessageSource.getInstance(),
                      dummyKeyword, null,
                      SchemaLocation.DOCUMENT,
                      new JsonNodePath(PathType.LEGACY),
                      null, null);
            }
            @Override public void initializeValidators() { initialized = true; }
        }

        SpySchema schema1 = new SpySchema(MAPPER.createObjectNode());
        SpySchema schema2 = new SpySchema(MAPPER.createObjectNode());

        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false, dummyKeyword);
        validator.preloadJsonSchemas(java.util.Arrays.asList(schema1, schema2));

        assertTrue(schema1.initialized);
        assertTrue(schema2.initialized);
    }

    @Test
    public void testAtRootRespectsPathTypeFromConfig() {
        // JSON_POINTER
        validationContext.getConfig().setPathType(PathType.JSON_POINTER);
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false, dummyKeyword);
        JsonNodePath rootPath = validator.atRoot();
        assertEquals(PathType.JSON_POINTER, rootPath.getPathType());

        // LEGACY
        validationContext.getConfig().setPathType(PathType.LEGACY);
        rootPath = validator.atRoot();
        assertEquals(PathType.LEGACY, rootPath.getPathType());

        // JSON_PATH
        validationContext.getConfig().setPathType(PathType.JSON_PATH);
        rootPath = validator.atRoot();
        assertEquals(PathType.JSON_PATH, rootPath.getPathType());
    }

    @Test
    public void testToStringReturnsLastElementName() {
        JsonNodePath path = new JsonNodePath(PathType.LEGACY).append("properties")
                .append("address");
        TestValidator validator = new TestValidator(schemaLocation, path,
                MAPPER.createObjectNode(), null, validationContext, false, dummyKeyword);
        assertEquals("address", validator.toString());
    }

    @Test
    public void testHasAdjacentKeywordInEvaluationPathFound() {
        // Build a parent schema that contains a validator whose keyword equals "testKey".
        class DummyValidator extends BaseJsonValidator {
            DummyValidator(JsonNodePath evaluationPath, JsonSchema parent) {
                super(SchemaLocation.DOCUMENT, evaluationPath,
                      MAPPER.createObjectNode(), parent,
                      ValidatorTypeCode.TRUE, dummyKeyword,
                      validationContext, false);
            }
            @Override public Set<ValidationMessage> validate(ExecutionContext ec,
                                                             JsonNode n,
                                                             JsonNode rn,
                                                             JsonNodePath il) { return Collections.emptySet(); }
            @Override public String getKeyword() { return "testKey"; }
        }

        MinimalJsonSchema parent = new MinimalJsonSchema(MAPPER.createObjectNode(),
                validationContext, dummyKeyword, null,
                SchemaLocation.DOCUMENT, new JsonNodePath(PathType.LEGACY));
        DummyValidator sibling = new DummyValidator(new JsonNodePath(PathType.LEGACY), parent);
        // Inject the sibling validator into the parent schema's validator list.
        java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        f.set(parent, java.util.Collections.singletonList(sibling));

        TestValidator validator = new TestValidator(schemaLocation,
                new JsonNodePath(PathType.LEGACY).append("properties").append("person"),
                MAPPER.createObjectNode(),
                parent,
                validationContext,
                false,
                dummyKeyword);

        assertTrue(validator.hasAdjacentKeywordInEvaluationPath("testKey"));
    }

    @Test
    public void testHasAdjacentKeywordInEvaluationPathNotFound() {
        MinimalJsonSchema parent = new MinimalJsonSchema(MAPPER.createObjectNode(),
                validationContext, dummyKeyword, null,
                SchemaLocation.DOCUMENT, new JsonNodePath(PathType.LEGACY));

        TestValidator validator = new TestValidator(schemaLocation,
                new JsonNodePath(PathType.LEGACY).append("properties").append("person"),
                MAPPER.createObjectNode(),
                parent,
                validationContext,
                false,
                dummyKeyword);

        assertFalse(validator.hasAdjacentKeywordInEvaluationPath("nonexistent"));
    }

    // ------------------------------------------------------------------------
    // 2.  Static utility methods
    // ------------------------------------------------------------------------

    @Test
    public void testStaticEqualsWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.000000000002));
        assertTrue(BaseJsonValidator.equals(-0.0, 0.0));
    }

    @Test
    public void testStaticEqualsOutsideTolerance() {
        assertFalse(BaseJsonValidator.equals(1.0, 1.1));
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN)); // NaN never equals
    }

    @Test
    public void testDebugDoesNotThrowWhenDebugDisabled() {
        // ExecutionConfig with debug disabled (default)
        ExecutionConfig execConfig = new ExecutionConfig();
        execConfig.setDebugEnabled(false);
        ExecutionContext ctx = new ExecutionContext(execConfig);

        // No exception should be thrown even though logger may be null.
        BaseJsonValidator.debug(logger, ctx,
                MAPPER.createObjectNode(),
                MAPPER.createObjectNode(),
                new JsonNodePath(PathType.LEGACY));
    }

    // ------------------------------------------------------------------------
    // 3.  Edge‑case / error handling tests
    // ------------------------------------------------------------------------

    @Test
    public void testFetchSubSchemaNodeReturnsNullWhenIdMissing() {
        ObjectNode schemaNode = MAPPER.createObjectNode(); // no "id"
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                schemaNode, null, validationContext, false, dummyKeyword);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNodeReturnsNullWhenIdEquals$Schema() {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("id", "http://example.com/schema");
        schemaNode.put("$schema", "http://example.com/schema");
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                schemaNode, null, validationContext, false, dummyKeyword);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testCollectAnnotationsEnabledAndAccepted() {
        ExecutionConfig execConfig = new ExecutionConfig();
        execConfig.setAnnotationCollectionEnabled(true);
        // Accept everything
        execConfig.setAnnotationCollectionFilter(k -> true);
        ExecutionContext ctx = new ExecutionContext(execConfig);

        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false, dummyKeyword);
        assertTrue(validator.collectAnnotations(ctx));
        assertTrue(validator.collectAnnotations(ctx, "anyKeyword"));
    }

    @Test
    public void testCollectAnnotationsDisabled() {
        ExecutionConfig execConfig = new ExecutionConfig();
        execConfig.setAnnotationCollectionEnabled(false);
        ExecutionContext ctx = new ExecutionContext(execConfig);

        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false, dummyKeyword);
        assertFalse(validator.collectAnnotations(ctx));
    }

    @Test
    public void testPutAnnotationAddsToExecutionContext() {
        ExecutionConfig execConfig = new ExecutionConfig();
        execConfig.setAnnotationCollectionEnabled(true);
        execConfig.setAnnotationCollectionFilter(k -> true);
        ExecutionContext ctx = new ExecutionContext(execConfig);

        TestValidator validator = new TestValidator(schemaLocation, evaluationPath,
                MAPPER.createObjectNode(), null, validationContext, false, dummyKeyword);

        Consumer<JsonNodeAnnotation.Builder> customizer = b -> b
                .instanceLocation(evaluationPath)
                .annotationValue(MAPPER.createObjectNode().put("note", "test"));

        validator.putAnnotation(ctx, customizer);

        // The annotation map should now contain exactly one entry.
        assertEquals(1, ctx.getAnnotations().size());
    }
}
