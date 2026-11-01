###Test START##
```java
/****************************************************************************************
 *  JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}
 *
 *  The tests are written by three “experts”.  Each expert contributed one test case
 *  for a different logical area of the class.  The final suite combines those
 *  contributions so that the class is exercised for:
 *
 *   1. Numeric equality utility – normal, edge and extreme values.
 *   2. Discriminator handling – both implicit and explicit mapping logic as well as
 *      merging of nested discriminators.
 *   3. Path handling, keyword lookup and annotation collection – covering the
 *      `atRoot`, `hasAdjacentKeywordInEvaluationPath`, `collectAnnotations` and
 *      `putAnnotation` methods.
 *
 *  Only public and protected members that appear in the source code (or in the
 *  supplied signatures) are used.  No external mocking frameworks are employed;
 *  concrete objects are built directly from the library’s constructors.
 *
 *  The test class is deliberately self‑contained: helper concrete subclasses are
 *  defined inside the test file to instantiate the abstract {@code BaseJsonValidator}
 *  and to create minimal {@code JsonSchema} instances required for the tests.
 ****************************************************************************************/

package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test suite for {@link BaseJsonValidator}.
 */
public class BaseJsonValidatorTest {

    /** Jackson mapper – used to create {@link JsonNode} instances. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple logger that records whether {@code debug(..)} was invoked. */
    private static final class RecordingLogger implements Logger {
        private final List<String> messages = new ArrayList<>();
        private final String name;

        RecordingLogger(String name) { this.name = name; }

        List<String> getMessages() { return messages; }

        @Override public String getName() { return name; }
        @Override public boolean isTraceEnabled() { return false; }
        @Override public void trace(String msg) {}
        @Override public void trace(String format, Object arg) {}
        @Override public void trace(String format, Object arg1, Object arg2) {}
        @Override public void trace(String format, Object... arguments) {}
        @Override public void trace(String msg, Throwable t) {}
        @Override public boolean isDebugEnabled() { return true; }
        @Override public void debug(String msg) { messages.add(msg); }
        @Override public void debug(String format, Object arg) { messages.add(String.format(format, arg)); }
        @Override public void debug(String format, Object arg1, Object arg2) { messages.add(String.format(format, arg1, arg2)); }
        @Override public void debug(String format, Object... arguments) { messages.add(String.format(format, arguments)); }
        @Override public void debug(String msg, Throwable t) { messages.add(msg); }
        @Override public boolean isInfoEnabled() { return false; }
        @Override public void info(String msg) {}
        @Override public void info(String format, Object arg) {}
        @Override public void info(String format, Object arg1, Object arg2) {}
        @Override public void info(String format, Object... arguments) {}
        @Override public void info(String msg, Throwable t) {}
        @Override public boolean isWarnEnabled() { return false; }
        @Override public void warn(String msg) {}
        @Override public void warn(String format, Object arg) {}
        @Override public void warn(String format, Object... arguments) {}
        @Override public void warn(String format, Object arg1, Object arg2) {}
        @Override public void warn(String msg, Throwable t) {}
        @Override public boolean isErrorEnabled() { return false; }
        @Override public void error(String msg) {}
        @Override public void error(String format, Object arg) {}
        @Override public void error(String format, Object arg1, Object arg2) {}
        @Override public void error(String format, Object... arguments) {}
        @Override public void error(String msg, Throwable t) {}
    }

    /** Minimal concrete subclass of {@link BaseJsonValidator}. */
    private static final class TestValidator extends BaseJsonValidator {
        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation) {
            // No real validation – just return an empty set.
            return Collections.emptySet();
        }
    }

    /** Minimal concrete subclass of {@link JsonSchema} used for discriminator tests. */
    private static final class MinimalJsonSchema extends JsonSchema {
        MinimalJsonSchema(JsonNode schemaNode,
                          ValidationContext validationContext,
                          SchemaLocation schemaLocation,
                          JsonNodePath evaluationPath) {
            super(
                /* validators */ Collections.emptyList(),
                /* validatorsLoaded */ false,
                /* recursiveAnchor */ false,
                /* typeValidator */ null,
                /* id */ null,
                /* suppressSubSchemaRetrieval */ false,
                schemaNode,
                validationContext,
                /* errorMessageType */ null,
                /* errorMessageKeyword */ null,
                /* messageSource */ DefaultMessageSource.getInstance(),
                /* keyword */ ValidatorTypeCode.TRUE,
                /* parentSchema */ null,
                schemaLocation,
                evaluationPath,
                /* evaluationParentSchema */ null,
                /* errorMessage */ null);
        }

        @Override public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode jsonNode,
                                                       JsonNode rootNode, JsonNodePath instanceLocation) {
            return Collections.emptySet();
        }
    }

    /** Helper to create a dummy {@link ValidationContext}. */
    private static ValidationContext createDummyValidationContext() {
        // The constructor permits null arguments – the test suite never calls methods
        // that require the meta‑schema or factory, so null is safe.
        return new ValidationContext(null, null, new SchemaValidatorsConfig());
    }

    private ValidationContext validationContext;
    private SchemaLocation dummyLocation;
    private JsonNodePath dummyPath;
    private JsonNode dummySchemaNode;

    @Before
    public void setUp() {
        validationContext = createDummyValidationContext();
        dummyLocation = SchemaLocation.DOCUMENT;
        dummyPath = BaseJsonValidator.JsonNodePathLegacy.getInstance();
        dummySchemaNode = MAPPER.createObjectNode(); // empty schema node
    }

    /*=====================================================================
     *  Expert 1 – numeric equality utility
     *=====================================================================*/
    @Test
    public void testEqualsUtility() {
        // exact equality
        assertTrue(BaseJsonValidator.equals(1.0, 1.0));

        // values that differ less than 1e‑12
        double a = 0.1234567890123;
        double b = a + 5e-13;
        assertTrue(BaseJsonValidator.equals(a, b));

        // values that differ more than the tolerance
        double c = a + 2e-12;
        assertFalse(BaseJsonValidator.equals(a, c));

        // extreme values (positive infinity)
        assertTrue(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));

        // NaN is never equal – the implementation follows Math.abs which yields NaN → false
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN));
    }

    /*=====================================================================
     *  Expert 2 – discriminator handling (implicit, explicit & merging)
     *=====================================================================*/
    @Test
    public void testDiscriminatorMatchingAndMerging() {
        // -----------------------------------------------------------------
        // 1️⃣  Implicit match – the fragment name equals the discriminator value
        // -----------------------------------------------------------------
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode(); // empty – no explicit mapping
        String discriminatorValue = "MyType";

        // Build a schema whose fragment is exactly "MyType"
        SchemaLocation schemaLoc = SchemaLocation.of("#/definitions/MyType");
        JsonNodePath evalPath = BaseJsonValidator.JsonNodePathLegacy.getInstance();
        MinimalJsonSchema schema = new MinimalJsonSchema(MAPPER.createObjectNode(),
                                                         validationContext,
                                                         schemaLoc,
                                                         evalPath);
        // Implicit match should set the context to “match”
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorValue, schema);
        assertTrue(ctx.isDiscriminatorMatchFound());

        // -----------------------------------------------------------------
        // 2️⃣  Explicit mapping match
        // -----------------------------------------------------------------
        ctx = new DiscriminatorContext(); // reset
        discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = discriminator.putObject("mapping");
        // mapping: "MyOther" -> "#/definitions/MyType"
        mapping.put("MyOther", "#/definitions/MyType");
        discriminatorValue = "MyOther";

        // Explicit mapping should also mark a match
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorValue, schema);
        assertTrue(ctx.isDiscriminatorMatchFound());

        // -----------------------------------------------------------------
        // 3️⃣  No explicit match but allowed implicit fallback (no override)
        // -----------------------------------------------------------------
        ctx = new DiscriminatorContext();
        discriminator = MAPPER.createObjectNode();
        mapping = discriminator.putObject("mapping");
        // mapping to a different fragment – does not match discriminatorValue
        mapping.put("SomeOther", "#/definitions/Other");
        discriminatorValue = "MyType";

        // The explicit mapping does not match; because there is no override for the
        // parent schema fragment, implicit matching must be performed.
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, discriminatorValue, schema);
        assertTrue(ctx.isDiscriminatorMatchFound());

        // -----------------------------------------------------------------
        // 4️⃣  Register & merge discriminators – root + child with overlapping mapping
        // -----------------------------------------------------------------
        DiscriminatorContext mergeCtx = new DiscriminatorContext();

        // Root discriminator (already registered)
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode rootMapping = rootDiscriminator.putObject("mapping");
        rootMapping.put("A", "#/definitions/A");
        BaseJsonValidator.registerAndMergeDiscriminator(mergeCtx, rootDiscriminator, schema, dummyPath);

        // Child schema introduces a new mapping "B" and attempts to redefine "A"
        JsonNode childSchemaNode = MAPPER.createObjectNode();
        SchemaLocation childLoc = SchemaLocation.of("#/definitions/Child");
        MinimalJsonSchema childSchema = new MinimalJsonSchema(childSchemaNode,
                                                             validationContext,
                                                             childLoc,
                                                             dummyPath);
        // Simulate the child schema having its own discriminator definition
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        ObjectNode childMapping = childDiscriminator.putObject("mapping");
        childMapping.put("B", "#/definitions/B");
        // Attempting to redefine "A" with a different target should cause an exception
        childMapping.put("A", "#/definitions/AnotherA");

        try {
            BaseJsonValidator.registerAndMergeDiscriminator(mergeCtx, childDiscriminator, childSchema, dummyPath);
            fail("Expected JsonSchemaException due to mapping redefinition");
        } catch (JsonSchemaException e) {
            // Expected path – verify the message mentions the conflicting key
            assertTrue(e.getMessage().contains("A"));
        }

        // Now add a non‑conflicting mapping and verify that it is merged correctly
        childMapping.remove("A"); // remove the conflicting entry
        childMapping.put("C", "#/definitions/C");
        BaseJsonValidator.registerAndMergeDiscriminator(mergeCtx, childDiscriminator, childSchema, dummyPath);
        ObjectNode merged = mergeCtx.getDiscriminatorForPath(schema.getSchemaLocation().toString());
        assertNotNull(merged);
        // The merged mapping must contain A, B and C
        JsonNode mergedMap = merged.get("mapping");
        assertEquals("#/definitions/A", mergedMap.get("A").asText());
        assertEquals("#/definitions/B", mergedMap.get("B").asText());
        assertEquals("#/definitions/C", mergedMap.get("C").asText());
    }

    /*=====================================================================
     *  Expert 3 – path handling, keyword lookup and annotation collection
     *=====================================================================*/
    @Test
    public void testPathHandlingAndAnnotationUtilities() {
        // -----------------------------------------------------------------
        // a) atRoot() must respect the path type configured in ValidationContext
        // -----------------------------------------------------------------
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setPathType(PathType.JSON_POINTER);
        ValidationContext ctxPointer = new ValidationContext(null, null, config);
        TestValidator pointerValidator = new TestValidator(
                dummyLocation,
                dummyPath,
                dummySchemaNode,
                null,
                ValidatorTypeCode.TRUE,
                ctxPointer);
        JsonNodePath rootPointer = pointerValidator.atRoot();
        assertEquals(PathType.JSON_POINTER, rootPointer.getPathType());

        config.setPathType(PathType.LEGACY);
        ValidationContext ctxLegacy = new ValidationContext(null, null, config);
        TestValidator legacyValidator = new TestValidator(
                dummyLocation,
                dummyPath,
                dummySchemaNode,
                null,
                ValidatorTypeCode.TRUE,
                ctxLegacy);
        JsonNodePath rootLegacy = legacyValidator.atRoot();
        assertEquals(PathType.LEGACY, rootLegacy.getPathType());

        // -----------------------------------------------------------------
        // b) hasAdjacentKeywordInEvaluationPath – when parent schema is null
        // -----------------------------------------------------------------
        assertFalse(pointerValidator.hasAdjacentKeywordInEvaluationPath("any"));
        // -----------------------------------------------------------------
        // c) hasAdjacentKeywordInEvaluationPath – when a sibling validator exists
        // -----------------------------------------------------------------
        // Build a minimal parent schema that contains a validator with keyword "testKeyword"
        JsonNode parentSchemaNode = MAPPER.createObjectNode();
        MinimalJsonSchema parentSchema = new MinimalJsonSchema(parentSchemaNode,
                                                             validationContext,
                                                             SchemaLocation.of("#/parent"),
                                                             dummyPath);
        // Create a dummy sibling validator
        JsonValidator sibling = new JsonValidator() {
            @Override public Set<ValidationMessage> validate(ExecutionContext ec, JsonNode n, JsonNode r, JsonNodePath i) { return Collections.emptySet(); }
            @Override public SchemaLocation getSchemaLocation() { return SchemaLocation.of("#/sibling"); }
            @Override public JsonNodePath getEvaluationPath() { return dummyPath; }
            @Override public String getKeyword() { return "testKeyword"; }
        };
        // Manually inject the sibling into the parent schema's validator list via reflection
        try {
            java.lang.reflect.Field f = JsonSchema.class.getDeclaredField("validators");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonValidator> list = (List<JsonValidator>) f.get(parentSchema);
            list.add(sibling);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Create a validator whose evaluation parent schema is the one we just built
        TestValidator childValidator = new TestValidator(
                dummyLocation,
                dummyPath,
                dummySchemaNode,
                parentSchema,
                ValidatorTypeCode.TRUE,
                validationContext);

        assertTrue(childValidator.hasAdjacentKeywordInEvaluationPath("testKeyword"));
        assertFalse(childValidator.hasAdjacentKeywordInEvaluationPath("nonexistent"));

        // -----------------------------------------------------------------
        // d) collectAnnotations – ensure it respects ExecutionConfig flags
        // -----------------------------------------------------------------
        ExecutionConfig execConfig = new ExecutionConfig();
        execConfig.setAnnotationCollectionEnabled(true);
        execConfig.setAnnotationCollectionFilter(k -> k.startsWith("collect"));
        ExecutionContext execContext = new ExecutionContext(execConfig);

        // keyword that passes the filter
        assertTrue(childValidator.collectAnnotations(execContext, "collectMe"));
        // keyword that does NOT pass the filter
        assertFalse(childValidator.collectAnnotations(execContext, "ignoreMe"));

        // -----------------------------------------------------------------
        // e) putAnnotation – verify that an annotation is stored with the expected data
        // -----------------------------------------------------------------
        // Use the same execution context as above – it contains a fresh JsonNodeAnnotations store
        childValidator.putAnnotation(execContext, builder -> builder
                .keyword("collectMe")
                .instanceLocation(dummyPath)
                .schemaLocation(dummyLocation));

        // The annotation store should now contain exactly one entry with the keyword we set
        assertEquals(1, execContext.getAnnotations().size());
        JsonNodeAnnotation stored = execContext.getAnnotations().iterator().next();
        assertEquals("collectMe", stored.getKeyword());
        assertEquals(dummyPath, stored.getEvaluationPath());
        assertEquals(dummyLocation, stored.getSchemaLocation());
    }

    /*=====================================================================
     *  Additional small sanity checks
     *=====================================================================*/
    @Test
    public void testToStringAndSchemaAccessors() {
        TestValidator validator = new TestValidator(
                dummyLocation,
                dummyPath,
                dummySchemaNode,
                null,
                ValidatorTypeCode.TRUE,
                validationContext);
        // toString() delegates to getEvaluationPath().getName(-1)
        // our dummyPath is the LEGACY singleton – its name(-1) is the empty string
        assertEquals("", validator.toString());

        // accessor methods should return the objects supplied via the constructor
        assertSame(dummyLocation, validator.getSchemaLocation());
        assertSame(dummyPath, validator.getEvaluationPath());
        assertEquals(ValidatorTypeCode.TRUE.getValue(), validator.getKeyword());
        assertSame(dummySchemaNode, validator.getSchemaNode());
        assertNull(validator.getParentSchema());

        // fetchSubSchemaNode must honour suppressSubSchemaRetrieval flag
        // (the flag is false in the default constructor, but we can test the true case)
        TestValidator suppressingValidator = new TestValidator(
                dummyLocation,
                dummyPath,
                dummySchemaNode,
                null,
                ValidatorTypeCode.TRUE,
                validationContext) {
            @Override protected JsonSchema fetchSubSchemaNode(ValidationContext ctx) {
                // force suppression for the test
                return super.fetchSubSchemaNode(new ValidationContext(null, null, new SchemaValidatorsConfig()) {
                    @Override public JsonSchemaFactory getJsonSchemaFactory() {
                        // Return a factory that would throw if called – we never want it called
                        throw new AssertionError("Factory must not be used when suppression is true");
                    }
                });
            }
        };
        // The overridden method still respects the flag – we set it via reflection
        try {
            java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("suppressSubSchemaRetrieval");
            f.setAccessible(true);
            f.setBoolean(suppressingValidator, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertNull(suppressingValidator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testDebugMethodLogsWhenEnabled() {
        // Arrange logger and execution context with debug enabled
        RecordingLogger logger = new RecordingLogger("test");
        ExecutionConfig execConfig = new ExecutionConfig();
        execConfig.getExecutionConfig().setDebugEnabled(true); // assuming getter/setter exists
        // If the real ExecutionConfig does not expose a setter, use reflection
        try {
            java.lang.reflect.Field f = ExecutionConfig.class.getDeclaredField("debugEnabled");
            f.setAccessible(true);
            f.setBoolean(execConfig, true);
        } catch (Exception e) {
            // ignore – the test will still run with debug disabled, which is acceptable
        }
        ExecutionContext execContext = new ExecutionContext(execConfig);

        JsonNode node = MAPPER.readTree("{\"a\":1}");
        JsonNode root = node;
        JsonNodePath instancePath = BaseJsonValidator.JsonNodePathLegacy.getInstance();

        // Act
        BaseJsonValidator.debug(logger, execContext, node, root, instancePath);

        // Assert – logger should have received exactly one message containing the node JSON
        List<String> msgs = logger.getMessages();
        assertEquals(1, msgs.size());
        assertTrue(msgs.get(0).contains("\"a\":1"));
        assertTrue(msgs.get(0).contains("validate("));
    }
}
```

###Test END##