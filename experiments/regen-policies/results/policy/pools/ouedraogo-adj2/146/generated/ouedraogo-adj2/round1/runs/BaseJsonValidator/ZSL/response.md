###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link BaseJsonValidator}.
 *
 * <p>The tests focus on the public and protected behaviour that can be exercised
 * without needing to rely on the full JSON‑Schema engine.  A concrete subclass
 * {@code TestValidator} is defined inside the test file to make the abstract
 * class instantiable.</p>
 */
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple {@link Logger} implementation that records the last message passed to {@code debug}. */
    private static class RecordingLogger implements Logger {
        private boolean debugEnabled = false;
        private String lastMessage = null;

        void setDebugEnabled(boolean enabled) {
            this.debugEnabled = enabled;
        }

        String getLastMessage() {
            return lastMessage;
        }

        @Override public String getName() { return "recording"; }
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
        @Override public boolean isDebugEnabled() { return debugEnabled; }
        @Override public void debug(String msg) { this.lastMessage = msg; }
        @Override public void debug(String format, Object arg) { this.lastMessage = String.format(format, arg); }
        @Override public void debug(String format, Object arg1, Object arg2) { this.lastMessage = String.format(format, arg1, arg2); }
        @Override public void debug(String format, Object... arguments) {
            this.lastMessage = String.format(format.replace("%s", "%s"), arguments);
        }
        @Override public void debug(String msg, Throwable t) { this.lastMessage = msg; }
        @Override public boolean isDebugEnabled(org.slf4j.Marker marker) { return debugEnabled; }
        @Override public void debug(org.slf4j.Marker marker, String msg) { this.lastMessage = msg; }
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

    /** Minimal concrete implementation of {@link BaseJsonValidator}. */
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
                  ValidatorTypeCode.TRUE,          // errorMessageType
                  ValidatorTypeCode.TRUE,          // keyword (also implements ErrorMessageType)
                  validationContext,
                  suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation) {
            // For the purpose of these unit tests we simply return an empty set.
            return Collections.emptySet();
        }
    }

    private ValidationContext minimalContext;
    private SchemaLocation dummyLocation;
    private JsonNodePath dummyPath;
    private JsonNode dummySchemaNode;
    private TestValidator validator;

    @Before
    public void setUp() {
        // Build a very small ValidationContext – we only need a non‑null instance.
        JsonMetaSchema meta = JsonMetaSchema.getV202012(); // the default meta‑schema
        JsonSchemaFactory factory = new JsonSchemaFactory(); // uses the default constructor
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        minimalContext = new ValidationContext(meta, factory, config);

        dummyLocation = SchemaLocation.DOCUMENT;
        dummyPath = new JsonNodePath(PathType.JSON_POINTER);
        dummySchemaNode = MAPPER.createObjectNode();

        validator = new TestValidator(dummyLocation, dummyPath, dummySchemaNode, null,
                minimalContext, false);
    }

    /* --------------------------------------------------------------------- */
    /* 1. Test the numeric equality helper                                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testEqualsCloseNumbers() {
        assertTrue(BaseJsonValidator.equals(0.1 + 0.2, 0.3));
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-10)); // outside tolerance
    }

    /* --------------------------------------------------------------------- */
    /* 2. Test the debug helper – only logs when both flags are true          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDebugWhenEnabled() {
        RecordingLogger logger = new RecordingLogger();
        logger.setDebugEnabled(true);

        ExecutionContext exec = new ExecutionContext();
        // enable debug in the execution config
        exec.getExecutionConfig().setDebugEnabled(true);

        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        JsonNode root = MAPPER.createObjectNode().set("root", node);
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER).append("a");

        BaseJsonValidator.debug(logger, exec, node, root, instanceLocation);

        String expected = "validate( " + node.toString() + ", " + root.toString() + ", " + instanceLocation.toString() + ")";
        assertEquals(expected, logger.getLastMessage());
    }

    @Test
    public void testDebugWhenDisabled() {
        RecordingLogger logger = new RecordingLogger();
        logger.setDebugEnabled(false); // logger disabled

        ExecutionContext exec = new ExecutionContext();
        exec.getExecutionConfig().setDebugEnabled(false); // execution config disabled

        JsonNode node = MAPPER.createObjectNode().put("b", 2);
        JsonNode root = MAPPER.createObjectNode().set("root", node);
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER).append("b");

        BaseJsonValidator.debug(logger, exec, node, root, instanceLocation);

        // No message should have been recorded.
        assertNull(logger.getLastMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 3. Test discriminator matching – various branches                      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDiscriminatorMatchWhenPropertyValueIsNull() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, validator);
        assertTrue(ctx.isDiscriminatorIgnore());
    }

    @Test
    public void testImplicitDiscriminatorMatch() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();

        // Create a schema whose fragment name equals the discriminator property value.
        SchemaLocation fragmentLocation = SchemaLocation.of("#/myFragment");
        JsonSchema schema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                MAPPER.createObjectNode(),
                minimalContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                fragmentLocation,
                new JsonNodePath(PathType.JSON_POINTER),
                null,
                null);

        // The fragment name is "myFragment"
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "myFragment", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testExplicitDiscriminatorMappingMatch() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();

        // Build explicit mapping: "typeA" -> "#/schemas/typeA"
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("typeA", "#/schemas/typeA");
        discriminator.set("mapping", mapping);

        // Build a schema whose location fragment is "/schemas/typeA"
        SchemaLocation schLoc = SchemaLocation.of("#/schemas/typeA");
        JsonSchema schema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                MAPPER.createObjectNode(),
                minimalContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                schLoc,
                new JsonNodePath(PathType.JSON_POINTER),
                null,
                null);

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "typeA", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testExplicitMappingNotFoundFallsBackToImplicitWhenAllowed() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();

        // explicit mapping does NOT contain "typeB"
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("typeA", "#/schemas/typeA");
        discriminator.set("mapping", mapping);

        // schema fragment is "typeB" – implicit match should be used because
        // the explicit mapping does not reference the same fragment.
        SchemaLocation schLoc = SchemaLocation.of("#/typeB");
        JsonSchema schema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                MAPPER.createObjectNode(),
                minimalContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                schLoc,
                new JsonNodePath(PathType.JSON_POINTER),
                null,
                null);

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "typeB", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testExplicitMappingOverridesImplicitWhenNoOverrideAllowed() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();

        // explicit mapping that points to the SAME fragment as the parent schema
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("typeA", "#/schemas/parent");
        discriminator.set("mapping", mapping);

        // parent schema (used only for the noExplicitDiscriminatorKeyOverride check)
        SchemaLocation parentLoc = SchemaLocation.of("#/schemas/parent");
        JsonSchema parentSchema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                MAPPER.createObjectNode(),
                minimalContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                parentLoc,
                new JsonNodePath(PathType.JSON_POINTER),
                null,
                null);

        // schema we are checking – its fragment is "/schemas/child"
        SchemaLocation childLoc = SchemaLocation.of("#/schemas/child");
        JsonSchema childSchema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                MAPPER.createObjectNode(),
                minimalContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                childLoc,
                new JsonNodePath(PathType.JSON_POINTER),
                null,
                null);

        // The explicit mapping does not match, and because the explicit mapping
        // references the same fragment as the parent, the implicit fallback must
        // NOT be performed.
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "typeA", childSchema);
        assertFalse(ctx.isDiscriminatorMatchFound());
    }

    /* --------------------------------------------------------------------- */
    /* 4. Test registerAndMergeDiscriminator – successful merge and error   */
    /* --------------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorThrowsWhenPropertyNameRedefined() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        rootDiscriminator.put("propertyName", "type"); // root already defines propertyName

        // Register the root discriminator first.
        ctx.registerDiscriminator(dummyLocation, rootDiscriminator);

        // Child schema also defines a discriminator with a propertyName – this should trigger an exception.
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        childDiscriminator.put("propertyName", "kind");

        SchemaLocation childLoc = SchemaLocation.of("#/child");
        JsonSchema childSchema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                MAPPER.createObjectNode(),
                minimalContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                childLoc,
                new JsonNodePath(PathType.JSON_POINTER),
                null,
                null);

        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema,
                new JsonNodePath(PathType.JSON_POINTER));
        // Expect JsonSchemaException
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesMappings() {
        DiscriminatorContext ctx = new DiscriminatorContext();

        // Root discriminator has a mapping for "A"
        ObjectNode rootDisc = MAPPER.createObjectNode();
        ObjectNode rootMapping = MAPPER.createObjectNode();
        rootMapping.put("A", "#/schemas/A");
        rootDisc.set("mapping", rootMapping);

        // Register root discriminator
        ctx.registerDiscriminator(dummyLocation, rootDisc);

        // Child discriminator adds a new mapping "B"
        ObjectNode childDisc = MAPPER.createObjectNode();
        ObjectNode childMapping = MAPPER.createObjectNode();
        childMapping.put("B", "#/schemas/B");
        childDisc.set("mapping", childMapping);

        SchemaLocation childLoc = SchemaLocation.of("#/child");
        JsonSchema childSchema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                MAPPER.createObjectNode(),
                minimalContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                childLoc,
                new JsonNodePath(PathType.JSON_POINTER),
                null,
                null);

        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDisc, childSchema,
                new JsonNodePath(PathType.JSON_POINTER));

        // After merging, root discriminator should contain both "A" and "B"
        ObjectNode merged = ctx.getDiscriminatorForPath(dummyLocation);
        assertNotNull(merged);
        JsonNode mergedMapping = merged.get("mapping");
        assertNotNull(mergedMapping);
        assertEquals("#/schemas/A", mergedMapping.get("A").asText());
        assertEquals("#/schemas/B", mergedMapping.get("B").asText());
    }

    /* --------------------------------------------------------------------- */
    /* 5. Test fetchSubSchemaNode – suppression and missing id               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testFetchSubSchemaNodeSuppressedReturnsNull() {
        TestValidator suppressed = new TestValidator(dummyLocation, dummyPath,
                MAPPER.createObjectNode().put("id", "http://example.com/schema"),
                null, minimalContext, true);
        assertNull(suppressed.fetchSubSchemaNode(minimalContext));
    }

    @Test
    public void testFetchSubSchemaNodeWithoutIdReturnsNull() {
        TestValidator noId = new TestValidator(dummyLocation, dummyPath,
                MAPPER.createObjectNode(), // no "id" field
                null, minimalContext, false);
        assertNull(noId.fetchSubSchemaNode(minimalContext));
    }

    /* --------------------------------------------------------------------- */
    /* 6. Test getNodeFieldType – parent schema contains a type field         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetNodeFieldTypeWhenParentHasType() {
        // Build a parent schema node containing "type":"object"
        ObjectNode parentNode = MAPPER.createObjectNode();
        parentNode.put("type", "object");

        // Minimal JsonSchema parent – we only need the schemaNode field.
        JsonSchema parentSchema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                parentNode,
                minimalContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                dummyLocation,
                dummyPath,
                null,
                null);

        TestValidator child = new TestValidator(dummyLocation, dummyPath,
                MAPPER.createObjectNode(),
                parentSchema,
                minimalContext,
                false);

        assertEquals("object", child.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenParentHasNoType() {
        ObjectNode parentNode = MAPPER.createObjectNode(); // empty
        JsonSchema parentSchema = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                parentNode,
                minimalContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                dummyLocation,
                dummyPath,
                null,
                null);

        TestValidator child = new TestValidator(dummyLocation, dummyPath,
                MAPPER.createObjectNode(),
                parentSchema,
                minimalContext,
                false);

        assertNull(child.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- */
    /* 7. Test preloadJsonSchemas – initializeValidators is invoked          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemasInvokesInitializeValidators() {
        // A test schema that flips a flag when initializeValidators() is called.
        class FlagSchema extends JsonSchema {
            boolean initialized = false;

            FlagSchema() {
                super(Collections.emptyList(),
                      false,
                      false,
                      null,
                      null,
                      false,
                      MAPPER.createObjectNode(),
                      minimalContext,
                      ValidatorTypeCode.TRUE,
                      null,
                      DefaultMessageSource.getInstance(),
                      ValidatorTypeCode.TRUE,
                      null,
                      dummyLocation,
                      dummyPath,
                      null,
                      null);
            }

            @Override
            public void initializeValidators() {
                initialized = true;
            }
        }

        FlagSchema schema1 = new FlagSchema();
        FlagSchema schema2 = new FlagSchema();

        validator.preloadJsonSchemas(Arrays.asList(schema1, schema2));

        assertTrue(schema1.initialized);
        assertTrue(schema2.initialized);
    }

    /* --------------------------------------------------------------------- */
    /* 8. Test atRoot – behaviour depends on the configured PathType         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testAtRootReturnsConfiguredPathInstance() {
        // Configure the ValidationContext to use LEGACY path type.
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setPathType(PathType.LEGACY);
        ValidationContext ctx = new ValidationContext(JsonMetaSchema.getV202012(),
                new JsonSchemaFactory(),
                cfg);

        TestValidator v = new TestValidator(dummyLocation, dummyPath,
                MAPPER.createObjectNode(),
                null,
                ctx,
                false);

        JsonNodePath rootPath = v.atRoot();
        assertEquals(PathType.LEGACY, rootPath.getPathType());
    }

    @Test
    public void testAtRootFallsBackToNewInstanceWhenPathTypeUnknown() {
        // Use a config that returns a custom (non‑standard) PathType.
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig() {
            @Override
            public PathType getPathType() {
                return PathType.UNKNOWN; // assume UNKNOWN is not one of the three handled cases
            }
        };
        ValidationContext ctx = new ValidationContext(JsonMetaSchema.getV202012(),
                new JsonSchemaFactory(),
                cfg);

        TestValidator v = new TestValidator(dummyLocation, dummyPath,
                MAPPER.createObjectNode(),
                null,
                ctx,
                false);

        JsonNodePath rootPath = v.atRoot();
        // The fallback creates a new JsonNodePath with the same type as the config.
        assertEquals(PathType.UNKNOWN, rootPath.getPathType());
    }

    /* --------------------------------------------------------------------- */
    /* 9. Test hasAdjacentKeywordInEvaluationPath – positive and negative cases */
    /* --------------------------------------------------------------------- */
    @Test
    public void testHasAdjacentKeywordFoundInParent() {
        // Build a parent validator that reports keyword "minLength"
        JsonValidator dummyValidator = new JsonValidator() {
            @Override public Set<ValidationMessage> validate(ExecutionContext ec, JsonNode n, JsonNode r, JsonNodePath i) { return null; }
            @Override public SchemaLocation getSchemaLocation() { return dummyLocation; }
            @Override public JsonNodePath getEvaluationPath() { return dummyPath; }
            @Override public String getKeyword() { return "minLength"; }
        };

        // Parent schema containing the dummy validator.
        JsonSchema parent = new JsonSchema(
                Collections.singletonList(dummyValidator),
                false,
                false,
                null,
                null,
                false,
                MAPPER.createObjectNode(),
                minimalContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                dummyLocation,
                dummyPath,
                null,
                null);

        TestValidator child = new TestValidator(dummyLocation, dummyPath,
                MAPPER.createObjectNode(),
                parent,
                minimalContext,
                false);

        assertTrue(child.hasAdjacentKeywordInEvaluationPath("minLength"));
    }

    @Test
    public void testHasAdjacentKeywordStopsAtPropertiesOrItems() {
        // Build a chain: child -> intermediate (element "properties") -> root
        JsonSchema root = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                MAPPER.createObjectNode(),
                minimalContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                dummyLocation,
                new JsonNodePath(PathType.JSON_POINTER).append("root"),
                null,
                null);

        // Intermediate schema with evaluationPath ending with "properties"
        JsonSchema intermediate = new JsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                null,
                false,
                MAPPER.createObjectNode(),
                minimalContext,
                ValidatorTypeCode.TRUE,
                null,
                DefaultMessageSource.getInstance(),
                ValidatorTypeCode.TRUE,
                null,
                dummyLocation,
                new JsonNodePath(PathType.JSON_POINTER).append("properties"),
                root,
                null);

        // Child validator whose parent is intermediate
        TestValidator child = new TestValidator(dummyLocation,
                new JsonNodePath(PathType.JSON_POINTER).append("child"),
                MAPPER.createObjectNode(),
                intermediate,
                minimalContext,
                false);

        // Even though root does not contain the keyword, the search should stop at "properties"
        assertFalse(child.hasAdjacentKeywordInEvaluationPath("anyKeyword"));
    }

    /* --------------------------------------------------------------------- */
    /* 10. Test collectAnnotations – both default and custom keyword handling */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCollectAnnotationsEnabledAndKeywordAccepted() {
        ExecutionContext exec = new ExecutionContext();
        exec.getExecutionConfig().setAnnotationCollectionEnabled(true);
        // accept everything
        exec.getExecutionConfig().setAnnotationCollectionFilter(k -> true);

        assertTrue(validator.collectAnnotations(exec));
    }

    @Test
    public void testCollectAnnotationsDisabled() {
        ExecutionContext exec = new ExecutionContext();
        exec.getExecutionConfig().setAnnotationCollectionEnabled(false);
        exec.getExecutionConfig().setAnnotationCollectionFilter(k -> true);

        assertFalse(validator.collectAnnotations(exec));
    }

    @Test
    public void testCollectAnnotationsKeywordFilteredOut() {
        ExecutionContext exec = new ExecutionContext();
        exec.getExecutionConfig().setAnnotationCollectionEnabled(true);
        // reject the validator's own keyword
        exec.getExecutionConfig().setAnnotationCollectionFilter(k -> !k.equals(validator.getKeyword()));

        assertFalse(validator.collectAnnotations(exec));
    }
}
```
###Test END##