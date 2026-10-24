###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for {@link BaseJsonValidator}.
 *
 * The tests instantiate a concrete subclass of {@link BaseJsonValidator}
 * (named {@code TestValidator}) because the class itself is abstract.
 * No external mocking frameworks are used – all objects are created
 * via the public constructors that are part of the library.
 */
public class BaseJsonValidatorTest {

    /** Simple concrete implementation used for exercising the protected methods. */
    private static class TestValidator extends BaseJsonValidator {
        public TestValidator(String schemaPath,
                             JsonNode schemaNode,
                             JsonSchema parentSchema,
                             ValidatorTypeCode validatorType,
                             ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            // No real validation – tests only need the instance.
            return Collections.emptySet();
        }
    }

    /** Helper to create a minimal {@link JsonSchema} instance. */
    private static JsonSchema createSchema(String schemaPath, JsonNode schemaNode) {
        // ValidationContext is not used by the parts of JsonSchema that we need in the tests,
        // therefore we can safely pass {@code null}.
        return new JsonSchema(null, schemaPath, null, schemaNode, null);
    }

    /** Helper to create a {@link JsonSchema} that contains a discriminator definition. */
    private static JsonSchema createSchemaWithDiscriminator(String schemaPath,
                                                            ObjectNode discriminatorNode) {
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.set("discriminator", discriminatorNode);
        return createSchema(schemaPath, schemaNode);
    }

    /** --------------------------------------------------------------------- *
     *  Tests for the static utility methods
     *  --------------------------------------------------------------------- */

    @Test
    public void testEqualsCloseNumbers() {
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertTrue(BaseJsonValidator.equals(-123.456, -123.4560000000005));
    }

    @Test
    public void testEqualsDifferentNumbers() {
        assertFalse(BaseJsonValidator.equals(1.0, 1.1));
        assertFalse(BaseJsonValidator.equals(0.0, 1e-10)); // outside the 1e-12 tolerance
    }

    @Test
    public void testDebugDoesNotThrow() {
        Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);
        JsonNode node = JsonNodeFactory.instance.textNode("value");
        JsonNode root = JsonNodeFactory.instance.objectNode();
        BaseJsonValidator.debug(logger, node, root, "/test");
        // No assertion – we only verify that the call does not raise an exception.
    }

    /** --------------------------------------------------------------------- *
     *  Tests for discriminator handling
     *  --------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatchWhenPropertyValueIsNull() {
        DiscriminatorContext ctx = new DiscriminatorContext();
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode(); // empty – not used
        JsonSchema dummySchema = createSchema("/any", JsonNodeFactory.instance.objectNode());

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, dummySchema);
        assertTrue("When the discriminator property value is null a match must be recorded",
                ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingFound() {
        DiscriminatorContext ctx = new DiscriminatorContext();

        // discriminator with explicit mapping: "cat" -> "/schemas/cat"
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.put("cat", "/schemas/cat");
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        discriminator.set("mapping", mapping);

        JsonSchema catSchema = createSchema("/schemas/cat", JsonNodeFactory.instance.objectNode());

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "cat", catSchema);
        assertTrue("Explicit mapping should cause a match", ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingNotFoundFallsBackToImplicit() {
        DiscriminatorContext ctx = new DiscriminatorContext();

        // explicit mapping does NOT contain the key we will query
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.put("dog", "/schemas/dog");
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        discriminator.set("mapping", mapping);

        // schema path ends with the discriminator value -> implicit match
        JsonSchema schema = createSchema("/schemas/cat", JsonNodeFactory.instance.objectNode());

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "cat", schema);
        assertTrue("Implicit match should be used when explicit mapping does not match",
                ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchNoMatch() {
        DiscriminatorContext ctx = new DiscriminatorContext();

        // No mapping at all
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();

        JsonSchema schema = createSchema("/schemas/unknown", JsonNodeFactory.instance.objectNode());

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "cat", schema);
        assertFalse("No match should be recorded", ctx.isDiscriminatorMatchFound());
    }

    /** --------------------------------------------------------------------- *
     *  Tests for registerAndMergeDiscriminator
     *  --------------------------------------------------------------------- */

    @Test
    public void testRegisterAndMergeDiscriminatorAddsMappingWhenRootHasNone() {
        DiscriminatorContext ctx = new DiscriminatorContext();

        // root discriminator – initially empty
        ObjectNode rootDiscriminator = JsonNodeFactory.instance.objectNode();

        // child schema that defines a discriminator with a mapping for "dog"
        ObjectNode childMapping = JsonNodeFactory.instance.objectNode();
        childMapping.put("dog", "/schemas/dog");
        ObjectNode childDiscriminator = JsonNodeFactory.instance.objectNode();
        childDiscriminator.set("mapping", childMapping);
        JsonSchema childSchema = createSchemaWithDiscriminator("/schemas/child", childDiscriminator);

        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema, "at ");

        // After registration the root discriminator must now contain the child's mapping
        JsonNode merged = rootDiscriminator.get("mapping");
        assertNotNull("Root discriminator should now have a mapping object", merged);
        assertEquals("/schemas/dog", merged.get("dog").asText());
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesAdditionalMappings() {
        DiscriminatorContext ctx = new DiscriminatorContext();

        // root discriminator already has a mapping for "cat"
        ObjectNode rootMapping = JsonNodeFactory.instance.objectNode();
        rootMapping.put("cat", "/schemas/cat");
        ObjectNode rootDiscriminator = JsonNodeFactory.instance.objectNode();
        rootDiscriminator.set("mapping", rootMapping);

        // child schema adds a new mapping for "dog"
        ObjectNode childMapping = JsonNodeFactory.instance.objectNode();
        childMapping.put("dog", "/schemas/dog");
        ObjectNode childDiscriminator = JsonNodeFactory.instance.objectNode();
        childDiscriminator.set("mapping", childMapping);
        JsonSchema childSchema = createSchemaWithDiscriminator("/schemas/child", childDiscriminator);

        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema, "at ");

        JsonNode merged = rootDiscriminator.get("mapping");
        assertNotNull(merged);
        assertEquals("/schemas/cat", merged.get("cat").asText());
        assertEquals("/schemas/dog", merged.get("dog").asText());
    }

    @Test
    public void testRegisterAndMergeDiscriminatorThrowsOnMappingRedefinition() {
        DiscriminatorContext ctx = new DiscriminatorContext();

        // root mapping for "cat" -> /schemas/cat
        ObjectNode rootMapping = JsonNodeFactory.instance.objectNode();
        rootMapping.put("cat", "/schemas/cat");
        ObjectNode rootDiscriminator = JsonNodeFactory.instance.objectNode();
        rootDiscriminator.set("mapping", rootMapping);

        // child schema tries to redefine "cat" to a different value
        ObjectNode childMapping = JsonNodeFactory.instance.objectNode();
        childMapping.put("cat", "/schemas/anotherCat");
        ObjectNode childDiscriminator = JsonNodeFactory.instance.objectNode();
        childDiscriminator.set("mapping", childMapping);
        JsonSchema childSchema = createSchemaWithDiscriminator("/schemas/child", childDiscriminator);

        try {
            BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema, "at ");
            fail("Expected JsonSchemaException due to mapping redefinition");
        } catch (JsonSchemaException e) {
            // Expected exception – verify the message contains the offending key.
            assertTrue(e.getMessage().contains("cat"));
        }
    }

    /** --------------------------------------------------------------------- *
     *  Tests for instance methods that depend only on constructor arguments
     *  --------------------------------------------------------------------- */

    @Test
    public void testGettersReturnConstructorValues() {
        String schemaPath = "/my/schema";
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode().put("type", "string");
        JsonSchema parent = createSchema("/parent", JsonNodeFactory.instance.objectNode());

        TestValidator validator = new TestValidator(schemaPath, schemaNode,
                parent, ValidatorTypeCode.TYPE, null);

        assertEquals(schemaPath, validator.getSchemaPath());
        assertSame(schemaNode, validator.getSchemaNode());
        assertSame(parent, validator.getParentSchema());
    }

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        // suppressSubSchemaRetrieval = true => method must return null without touching the context
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode(); // no "id" field
        TestValidator validator = new TestValidator("/path", schemaNode,
                null, ValidatorTypeCode.TYPE,
                // use the 6‑arg constructor to set the flag
                new ValidationContext(null, null, null, null, null) {
                    // anonymous stub – all methods are unused for this test
                }) {
            {
                // force the protected field via reflection (or by using the 6‑arg ctor directly)
                // The 6‑arg constructor is protected; we therefore invoke it through a subclass.
            }
        };

        // Unfortunately the 6‑arg constructor is protected; we create a subclass that calls it.
        class SuppressedValidator extends BaseJsonValidator {
            SuppressedValidator() {
                super("/path", schemaNode, null, ValidatorTypeCode.TYPE,
                        null, true);
            }

            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                   JsonNode node,
                                                   JsonNode rootNode,
                                                   String at) {
                return Collections.emptySet();
            }
        }

        SuppressedValidator sv = new SuppressedValidator();
        assertNull("When suppression is true the sub‑schema must not be retrieved", sv.fetchSubSchemaNode(null));
    }

    @Test
    public void testFetchSubSchemaNodeWhenNoIdReturnsNull() {
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode(); // absent "id"
        TestValidator validator = new TestValidator("/path", schemaNode,
                null, ValidatorTypeCode.TYPE, null);
        assertNull("Without an \"id\" field fetchSubSchemaNode must return null", validator.fetchSubSchemaNode(null));
    }

    @Test
    public void testGetNodeFieldTypeReturnsTypeFromParentSchema() {
        // parent schema contains a "type" property
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode();
        parentNode.put("type", "integer");
        JsonSchema parent = createSchema("/parent", parentNode);

        TestValidator validator = new TestValidator("/path",
                JsonNodeFactory.instance.objectNode(),
                parent, ValidatorTypeCode.TYPE, null);

        assertEquals("integer", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeReturnsNullWhenAbsent() {
        JsonSchema parent = createSchema("/parent", JsonNodeFactory.instance.objectNode());

        TestValidator validator = new TestValidator("/path",
                JsonNodeFactory.instance.objectNode(),
                parent, ValidatorTypeCode.TYPE, null);

        assertNull(validator.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemasInvokesInitializeValidators() {
        // Two schemas to preload
        JsonSchema schema1 = createSchema("/s1", JsonNodeFactory.instance.objectNode());
        JsonSchema schema2 = createSchema("/s2", JsonNodeFactory.instance.objectNode());

        TestValidator validator = new TestValidator("/path",
                JsonNodeFactory.instance.objectNode(),
                null, ValidatorTypeCode.TYPE, null);

        // The method does not return a value – we simply ensure it runs without exception.
        validator.preloadJsonSchemas(java.util.Arrays.asList(schema1, schema2));
    }

    /** --------------------------------------------------------------------- *
     *  Tests for path handling utilities
     *  --------------------------------------------------------------------- */

    @Test
    public void testPathTypeIsDefault() {
        TestValidator validator = new TestValidator("/path",
                JsonNodeFactory.instance.objectNode(),
                null, ValidatorTypeCode.TYPE, null);
        assertEquals(PathType.DEFAULT, validator.getPathType());
    }

    @Test
    public void testAtRootUsesPathTypeRoot() {
        TestValidator validator = new TestValidator("/path",
                JsonNodeFactory.instance.objectNode(),
                null, ValidatorTypeCode.TYPE, null);
        assertEquals(PathType.DEFAULT.getRoot(), validator.atRoot());
    }

    @Test
    public void testAtPathWithStringToken() {
        TestValidator validator = new TestValidator("/path",
                JsonNodeFactory.instance.objectNode(),
                null, ValidatorTypeCode.TYPE, null);
        String result = validator.atPath("/a/b", "c");
        assertEquals(PathType.DEFAULT.append("/a/b", "c"), result);
    }

    @Test
    public void testAtPathWithIndexToken() {
        TestValidator validator = new TestValidator("/path",
                JsonNodeFactory.instance.objectNode(),
                null, ValidatorTypeCode.TYPE, null);
        String result = validator.atPath("/array", 2);
        assertEquals(PathType.DEFAULT.append("/array", 2), result);
    }
}
###Test END##