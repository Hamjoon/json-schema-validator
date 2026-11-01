package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class BaseJsonValidatorTest {

    private static final Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);

    private ObjectNode emptyDiscriminator;
    private ObjectNode explicitDiscriminator;
    private ObjectNode implicitDiscriminator;

    private ValidationContext.DiscriminatorContext discriminatorContext;

    private MinimalJsonSchema parentSchemaWithType;
    private MinimalJsonSchema childSchema;
    private MinimalJsonSchema schemaForImplicit;
    private MinimalJsonSchema schemaForExplicit;

    @Before
    public void setUp() {
        // ---------- Discriminator nodes ----------
        // empty discriminator (no mapping)
        emptyDiscriminator = JsonNodeFactory.instance.objectNode()
                .put("propertyName", "type");

        // explicit discriminator with a mapping entry
        explicitDiscriminator = JsonNodeFactory.instance.objectNode()
                .put("propertyName", "type")
                .set("mapping", JsonNodeFactory.instance.objectNode()
                        .put("key1", "#/myFragment")
                        .put("key2", "#/otherFragment"));

        // implicit discriminator (no mapping, only propertyName)
        implicitDiscriminator = JsonNodeFactory.instance.objectNode()
                .put("propertyName", "type");

        discriminatorContext = new ValidationContext.DiscriminatorContext();

        // ---------- Schemas ----------
        // Parent schema containing a "type" field
        ObjectNode parentSchemaNode = JsonNodeFactory.instance.objectNode()
                .put("type", "string");
        parentSchemaWithType = new MinimalJsonSchema(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.DEFAULT),
                parentSchemaNode,
                null,
                ValidatorTypeCode.TRUE,
                null);

        // Child schema that references the parent (used for getNodeFieldType tests)
        childSchema = new MinimalJsonSchema(
                SchemaLocation.DOCUMENT,
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(),
                parentSchemaWithType,
                ValidatorTypeCode.TRUE,
                null);

        // Schema whose fragment name equals the discriminator property value (implicit match)
        schemaForImplicit = new MinimalJsonSchema(
                SchemaLocation.of("http://example.com#/myFragment"),
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(),
                null,
                ValidatorTypeCode.TRUE,
                null);

        // Schema used for explicit mapping match
        schemaForExplicit = new MinimalJsonSchema(
                SchemaLocation.of("http://example.com#/myFragment"),
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(),
                null,
                ValidatorTypeCode.TRUE,
                null);
    }

    // -------------------------------------------------------------------------
    // Static utility methods
    // -------------------------------------------------------------------------

    @Test
    public void testEqualsMethod() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.0));
        assertFalse(BaseJsonValidator.equals(1.0001, 1.0));
    }

    @Test
    public void testDebugMethodDoesNotThrow() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("a", 1);
        JsonNode root = JsonNodeFactory.instance.objectNode().put("root", true);
        JsonNodePath path = new JsonNodePath(PathType.DEFAULT).append("a");
        // Should simply log; no exception expected
        BaseJsonValidator.debug(logger, node, root, path);
    }

    // -------------------------------------------------------------------------
    // Instance methods (via a concrete subclass)
    // -------------------------------------------------------------------------

    @Test
    public void testGettersAndToString() {
        MinimalJsonSchema validator = new MinimalJsonSchema(
                SchemaLocation.of("http://example.com#/test"),
                new JsonNodePath(PathType.DEFAULT).append("myKeyword"),
                JsonNodeFactory.instance.objectNode(),
                null,
                ValidatorTypeCode.TRUE,
                null);

        assertEquals(SchemaLocation.of("http://example.com#/test"), validator.getSchemaLocation());
        assertEquals("myKeyword", validator.getEvaluationPath().getName(-1));
        assertEquals("true", validator.getKeyword()); // ValidatorTypeCode.TRUE value is "true"
        assertNotNull(validator.getSchemaNode());
        assertNull(validator.getParentSchema());

        // toString() should return the last token of the evaluation path
        assertEquals("myKeyword", validator.toString());
    }

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        MinimalJsonValidator suppressingValidator = new MinimalJsonValidator(
                SchemaLocation.of("http://example.com#/test"),
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(),
                null,
                ValidatorTypeCode.TRUE,
                null,
                null,
                true); // suppressSubSchemaRetrieval = true

        assertNull(suppressingValidator.fetchSubSchemaNode(null));
    }

    @Test
    public void testValidateSimple() {
        MinimalJsonValidator validator = new MinimalJsonValidator(
                SchemaLocation.of("http://example.com#/test"),
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(),
                null,
                ValidatorTypeCode.TRUE,
                null,
                null,
                false);

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        Set<ValidationMessage> messages = validator.validate(ctx, node);
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testGetNodeFieldType() {
        // childSchema has parentSchemaWithType whose schemaNode contains "type":"string"
        assertEquals("string", childSchema.getNodeFieldType());

        // validator without a parent (or without "type") should return null
        MinimalJsonValidator validator = new MinimalJsonValidator(
                SchemaLocation.of("http://example.com#/noParent"),
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(),
                null,
                ValidatorTypeCode.TRUE,
                null,
                null,
                false);
        assertNull(validator.getNodeFieldType());
    }

    @Test
    public void testAtRootCreatesPathWithDefaultType() {
        MinimalJsonValidator validator = new MinimalJsonValidator(
                SchemaLocation.of("http://example.com#/test"),
                new JsonNodePath(PathType.DEFAULT),
                JsonNodeFactory.instance.objectNode(),
                null,
                ValidatorTypeCode.TRUE,
                null,
                null,
                false);
        JsonNodePath rootPath = validator.atRoot();
        assertNotNull(rootPath);
        assertEquals(PathType.DEFAULT, rootPath.getPathType());
    }

    // -------------------------------------------------------------------------
    // Discriminator related static methods
    // -------------------------------------------------------------------------

    @Test
    public void testCheckDiscriminatorMatchWhenPropertyValueIsNull() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        BaseJsonValidator.checkDiscriminatorMatch(ctx, emptyDiscriminator, null, schemaForImplicit);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitMapping() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        BaseJsonValidator.checkDiscriminatorMatch(ctx, implicitDiscriminator, "myFragment", schemaForImplicit);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingSuccess() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        BaseJsonValidator.checkDiscriminatorMatch(ctx, explicitDiscriminator, "key1", schemaForExplicit);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingFailureFallsBackToImplicit() {
        // Mapping exists but does not point to the schema; also no explicit override,
        // so implicit mapping should be considered.
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        BaseJsonValidator.checkDiscriminatorMatch(ctx, explicitDiscriminator, "key2", schemaForImplicit);
        // key2 maps to "#/otherFragment", which does NOT match schemaForImplicit fragment.
        // No implicit match either, so should be false.
        assertFalse(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testNoExplicitDiscriminatorKeyOverride() {
        // mapping that points to a different fragment -> should return true (no override)
        ObjectNode mapping = JsonNodeFactory.instance.objectNode()
                .put("otherKey", "#/someOtherFragment");
        assertTrue(BaseJsonValidator.noExplicitDiscriminatorKeyOverride(mapping, schemaForImplicit));

        // mapping that points to the parent schema's fragment -> should return false
        ObjectNode overridingMapping = JsonNodeFactory.instance.objectNode()
                .put("override", "#/myFragment"); // matches schemaForImplicit fragment
        assertFalse(BaseJsonValidator.noExplicitDiscriminatorKeyOverride(overridingMapping, schemaForImplicit));
    }

    @Test
    public void testRegisterAndMergeDiscriminatorAddsMissingMapping() {
        // Root discriminator without mapping
        ObjectNode rootDiscriminator = JsonNodeFactory.instance.objectNode()
                .put("propertyName", "type");

        // Child schema that defines its own discriminator with a mapping
        ObjectNode childMapping = JsonNodeFactory.instance.objectNode()
                .put("newKey", "#/newFragment");
        ObjectNode childDiscriminator = JsonNodeFactory.instance.objectNode()
                .put("propertyName", "type")
                .set("mapping", childMapping);

        // Simulate a child schema whose schemaNode contains the discriminator we just built
        MinimalJsonSchema childSchema = new MinimalJsonSchema(
                SchemaLocation.of("http://example.com#/child"),
                new JsonNodePath(PathType.DEFAULT),
                childDiscriminator,
                null,
                ValidatorTypeCode.TRUE,
                null);

        // Register & merge
        BaseJsonValidator.registerAndMergeDiscriminator(
                discriminatorContext,
                rootDiscriminator,
                childSchema,
                new JsonNodePath(PathType.DEFAULT));

        // After merging, rootDiscriminator should now have the mapping from childDiscriminator
        JsonNode resultingMapping = rootDiscriminator.get("mapping");
        assertNotNull(resultingMapping);
        assertEquals("#/newFragment", resultingMapping.get("newKey").asText());
    }

    // -------------------------------------------------------------------------
    // Helper concrete class used for testing
    // -------------------------------------------------------------------------

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} that simply
     * returns an empty set of {@link ValidationMessage}s.
     */
    private static class MinimalJsonValidator extends BaseJsonValidator {

        MinimalJsonValidator(SchemaLocation schemaLocation,
                             JsonNodePath evaluationPath,
                             JsonNode schemaNode,
                             JsonSchema parentSchema,
                             ValidatorTypeCode validatorType,
                             ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                    validatorType, validatorType, validationContext, false);
        }

        // Additional constructor that allows us to control the suppressSubSchemaRetrieval flag
        MinimalJsonValidator(SchemaLocation schemaLocation,
                             JsonNodePath evaluationPath,
                             JsonNode schemaNode,
                             JsonSchema parentSchema,
                             ValidatorTypeCode validatorType,
                             ValidationContext validationContext,
                             Keyword keyword,
                             boolean suppressSubSchemaRetrieval) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                    validatorType, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                               JsonNode rootNode, JsonNodePath instanceLocation) {
            // Simple implementation that always succeeds
            return Collections.emptySet();
        }
    }

    /**
     * A thin wrapper around {@link MinimalJsonValidator} that simply forwards
     * the constructor arguments to the super class. This class exists to make
     * the test code a little more readable when we need a concrete
     * {@link JsonSchema}‑like object.
     */
    private static class MinimalJsonSchema extends MinimalJsonValidator {
        MinimalJsonSchema(SchemaLocation schemaLocation,
                          JsonNodePath evaluationPath,
                          JsonNode schemaNode,
                          JsonSchema parentSchema,
                          ValidatorTypeCode validatorType,
                          ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                    validatorType, validationContext);
        }
    }
}
