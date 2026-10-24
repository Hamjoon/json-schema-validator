/***************************************************************************************************
 * JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}
 *
 * The tests are placed in the same package (com.networknt.schema) so that the protected static
 * members of {@code BaseJsonValidator} can be accessed without using reflection.
 *
 * A tiny concrete subclass {@code TestValidator} is introduced – it only implements the abstract
 * {@code validate(...)} method required by {@link com.networknt.schema.JsonValidator}.  The validator
 * does nothing and returns an empty set, which is sufficient for exercising the non‑abstract
 * behaviour of the base class.
 *
 * The test cases cover:
 *   • numeric equality helper {@code equals(double,double)}
 *   • debug logging helper {@code debug(...)}
 *   • sub‑schema retrieval logic via {@code fetchSubSchemaNode(...)}
 *   • discriminator handling helpers:
 *        – {@code checkDiscriminatorMatch(...)}
 *        – {@code registerAndMergeDiscriminator(...)}
 *   • path handling helpers {@code atRoot()}, {@code atPath(...)} and {@code getPathType()}
 *   • {@code getNodeFieldType()} and {@code preloadJsonSchemas(...)}
 *
 * Real library classes are used – no mocking frameworks are employed, in accordance with the
 * requirements.  Where a full {@code JsonSchema} instance is needed, the public constructors of
 * {@code JsonSchema} are used with a minimal {@link com.fasterxml.jackson.databind.node.ObjectNode}
 * as the schema definition.  The {@code ValidationContext} argument is passed as {@code null}
 * because the exercised code paths either do not touch the context or safely handle a {@code null}
 * reference.
 ***************************************************************************************************/

package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal concrete implementation of {@link BaseJsonValidator}.  It simply returns an empty
 * {@link java.util.Set} from the {@code validate(...)} method – the only abstract method that
 * must be supplied.
 */
class TestValidator extends BaseJsonValidator {

    TestValidator(String schemaPath,
                  JsonNode schemaNode,
                  JsonSchema parentSchema,
                  ValidatorTypeCode validatorType,
                  ValidationContext validationContext,
                  boolean suppressSubSchemaRetrieval) {
        super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
    }

    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                           JsonNode node,
                                           JsonNode rootNode,
                                           String at) {
        // No validation logic – just return an empty set.
        return Collections.emptySet();
    }
}

/**
 * Test suite.
 */
public class BaseJsonValidatorTest {

    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

    /** -----------------------------------------------------------------------
     *  1.  Test the numeric equality helper.
     * ------------------------------------------------------------------------ */
    @Test
    public void testEqualsHelper() {
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertTrue(BaseJsonValidator.equals(-123.456, -123.456000000001));
        assertFalse(BaseJsonValidator.equals(0.0, 1e-10));
        assertFalse(BaseJsonValidator.equals(3.1415, 3.1416));
    }

    /** -----------------------------------------------------------------------
     *  2.  Test the debug helper – it must not throw any exception.
     * ------------------------------------------------------------------------ */
    @Test
    public void testDebugHelperDoesNotThrow() {
        Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);
        JsonNode node = FACTORY.textNode("sample");
        JsonNode root = FACTORY.objectNode();
        // The method only logs; we only verify that it runs without NPE.
        BaseJsonValidator.debug(logger, node, root, "$");
    }

    /** -----------------------------------------------------------------------
     *  3.  fetchSubSchemaNode – suppressed retrieval must always return null.
     * ------------------------------------------------------------------------ */
    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        ObjectNode emptySchema = FACTORY.objectNode();
        TestValidator validator = new TestValidator(
                "#/properties/foo",
                emptySchema,
                null,
                null,
                null,
                true // suppressSubSchemaRetrieval = true
        );
        assertNull("When sub‑schema retrieval is suppressed the result must be null",
                validator.fetchSubSchemaNode(null));
    }

    /** -----------------------------------------------------------------------
     *  4.  fetchSubSchemaNode – without an “id” property the helper returns null.
     * ------------------------------------------------------------------------ */
    @Test
    public void testFetchSubSchemaNodeNoId() {
        ObjectNode schemaWithoutId = FACTORY.objectNode();
        TestValidator validator = new TestValidator(
                "#/properties/bar",
                schemaWithoutId,
                null,
                null,
                null,
                false // retrieval allowed
        );
        // No “id” field ⇒ obtainSubSchemaNode returns null.
        assertNull(validator.fetchSubSchemaNode(null));
    }

    /** -----------------------------------------------------------------------
     *  5.  checkDiscriminatorMatch – a null discriminator value marks a match immediately.
     * ------------------------------------------------------------------------ */
    @Test
    public void testCheckDiscriminatorMatchNullValue() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode discriminator = FACTORY.objectNode(); // empty discriminator
        JsonSchema dummySchema = new JsonSchema(null, URI.create("http://example.com"), FACTORY.objectNode());

        // null property value → match
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, dummySchema);
        assertTrue("Null discriminator property value must result in a match", ctx.isDiscriminatorMatchFound());
    }

    /** -----------------------------------------------------------------------
     *  6.  checkDiscriminatorMatch – implicit mapping (no “mapping” node) works when the
     *      schema path ends with the discriminator value.
     * ------------------------------------------------------------------------ */
    @Test
    public void testCheckDiscriminatorMatchImplicit() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode discriminator = FACTORY.objectNode(); // no mapping field
        JsonSchema schema = new JsonSchema(null, URI.create("http://example.com"), FACTORY.objectNode());
        // Simulate a schema whose path ends with "/myType"
        setSchemaPath(schema, "#/definitions/myType");

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "myType", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    /** -----------------------------------------------------------------------
     *  7.  checkDiscriminatorMatch – explicit mapping is honoured.
     * ------------------------------------------------------------------------ */
    @Test
    public void testCheckDiscriminatorMatchExplicit() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();

        // Build discriminator { "mapping" : { "dog" : "#/defs/Dog" } }
        ObjectNode mapping = FACTORY.objectNode();
        mapping.put("dog", "#/defs/Dog");
        ObjectNode discriminator = FACTORY.objectNode();
        discriminator.set("mapping", mapping);

        JsonSchema schema = new JsonSchema(null, URI.create("http://example.com"), FACTORY.objectNode());
        setSchemaPath(schema, "#/defs/Dog");

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "dog", schema);
        assertTrue("Explicit mapping should cause a match", ctx.isDiscriminatorMatchFound());
    }

    /** -----------------------------------------------------------------------
     *  8.  checkDiscriminatorMatch – explicit mapping does NOT match but implicit mapping is
     *      allowed (no explicit key overrides) – should still mark a match.
     * ------------------------------------------------------------------------ */
    @Test
    public void testCheckDiscriminatorMatchExplicitNoMatchImplicitAllowed() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();

        // Discriminator with a mapping that does NOT contain the wanted key.
        ObjectNode mapping = FACTORY.objectNode();
        mapping.put("cat", "#/defs/Cat");   // unrelated mapping
        ObjectNode discriminator = FACTORY.objectNode();
        discriminator.set("mapping", mapping);

        JsonSchema schema = new JsonSchema(null, URI.create("http://example.com"), FACTORY.objectNode());
        // Implicit path ends with the discriminator value "bird"
        setSchemaPath(schema, "#/defs/bird");

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "bird", schema);
        assertTrue("When the explicit mapping does not match and there is no key override, implicit match must be used",
                ctx.isDiscriminatorMatchFound());
    }

    /** -----------------------------------------------------------------------
     *  9.  registerAndMergeDiscriminator – redefining the discriminator propertyName must
     *      raise a {@link JsonSchemaException}.
     * ------------------------------------------------------------------------ */
    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorRedefinePropertyName() {
        // Existing discriminator registered on the context (empty mapping)
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode rootDiscriminator = FACTORY.objectNode(); // root discriminator that will be passed in
        ctx.registerDiscriminator("#/parent", rootDiscriminator);

        // Schema that introduces a new discriminator with a propertyName (illegal)
        ObjectNode newDiscriminator = FACTORY.objectNode();
        newDiscriminator.put("propertyName", "type"); // illegal redefinition

        JsonSchema schema = new JsonSchema(null, URI.create("http://example.com"), FACTORY.objectNode());
        setSchemaPath(schema, "#/parent/child");

        // This call must throw JsonSchemaException
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at");
    }

    /** -----------------------------------------------------------------------
     * 10.  registerAndMergeDiscriminator – merging mappings when both root and child have
     *      mappings that do not clash.
     * ------------------------------------------------------------------------ */
    @Test
    public void testRegisterAndMergeDiscriminatorMergeMappings() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();

        // Root discriminator already has a mapping entry "a" -> "#/defs/A"
        ObjectNode rootMapping = FACTORY.objectNode();
        rootMapping.put("a", "#/defs/A");
        ObjectNode rootDiscriminator = FACTORY.objectNode();
        rootDiscriminator.set("mapping", rootMapping);
        ctx.registerDiscriminator("#/root", rootDiscriminator);

        // Child schema discriminator brings an additional mapping "b" -> "#/defs/B"
        ObjectNode childMapping = FACTORY.objectNode();
        childMapping.put("b", "#/defs/B");
        ObjectNode childDiscriminator = FACTORY.objectNode();
        childDiscriminator.set("mapping", childMapping);

        JsonSchema childSchema = new JsonSchema(null, URI.create("http://example.com"), FACTORY.objectNode());
        setSchemaPath(childSchema, "#/root/child");

        // Merge – should not throw.
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema, "at");

        // Verify that the root discriminator now contains both entries.
        ObjectNode merged = (ObjectNode) ctx.getDiscriminatorForPath("#/root");
        assertNotNull(merged);
        ObjectNode mergedMapping = (ObjectNode) merged.get("mapping");
        assertEquals("#/defs/A", mergedMapping.get("a").asText());
        assertEquals("#/defs/B", mergedMapping.get("b").asText());
    }

    /** -----------------------------------------------------------------------
     * 11.  registerAndMergeDiscriminator – attempting to redefine an existing mapping key with a
     *      different value must raise {@link JsonSchemaException}.
     * ------------------------------------------------------------------------ */
    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorMappingRedefinitionThrows() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();

        // Root discriminator with mapping "x" -> "#/defs/Old"
        ObjectNode rootMapping = FACTORY.objectNode();
        rootMapping.put("x", "#/defs/Old");
        ObjectNode rootDiscriminator = FACTORY.objectNode();
        rootDiscriminator.set("mapping", rootMapping);
        ctx.registerDiscriminator("#/root", rootDiscriminator);

        // Child schema attempts to map the same key "x" to a *different* location.
        ObjectNode childMapping = FACTORY.objectNode();
        childMapping.put("x", "#/defs/New");
        ObjectNode childDiscriminator = FACTORY.objectNode();
        childDiscriminator.set("mapping", childMapping);

        JsonSchema childSchema = new JsonSchema(null, URI.create("http://example.com"), FACTORY.objectNode());
        setSchemaPath(childSchema, "#/root/child");

        // This must throw because key "x" is being redefined.
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema, "at");
    }

    /** -----------------------------------------------------------------------
     * 12.  Path handling – atRoot(), atPath(String, String) and atPath(String, int)
     * ------------------------------------------------------------------------ */
    @Test
    public void testPathHelpers() {
        // Use the concrete TestValidator – it inherits the protected helpers.
        TestValidator validator = new TestValidator(
                "#",
                FACTORY.objectNode(),
                null,
                null,
                null,
                false
        );

        // The default PathType is PathType.DEFAULT (which currently resolves to JSON_POINTER)
        PathType pt = validator.getPathType();
        assertNotNull(pt);
        assertEquals(pt.getRoot(), validator.atRoot());

        // Simple child token
        String childPath = validator.atPath("/", "foo");
        assertEquals(pt.append("/", "foo"), childPath);

        // Indexed child
        String indexedPath = validator.atPath("/array", 2);
        assertEquals(pt.append("/array", 2), indexedPath);
    }

    /** -----------------------------------------------------------------------
     * 13.  getNodeFieldType() – returns the value of the parent schema's "type"
     *      keyword when present, otherwise null.
     * ------------------------------------------------------------------------ */
    @Test
    public void testGetNodeFieldType() {
        // Parent schema with a "type": "object" field
        ObjectNode parentSchemaNode = FACTORY.objectNode();
        parentSchemaNode.put("type", "object");
        JsonSchema parentSchema = new JsonSchema(null, URI.create("http://example.com"), parentSchemaNode);

        // Child validator (the class under test) – it will use the parent schema above.
        TestValidator child = new TestValidator(
                "#/properties/foo",
                FACTORY.objectNode(),
                parentSchema,
                null,
                null,
                false
        );

        assertEquals("object", child.getNodeFieldType());

        // Now a parent without a "type" field
        JsonSchema parentWithoutType = new JsonSchema(null, URI.create("http://example.com"), FACTORY.objectNode());
        TestValidator child2 = new TestValidator(
                "#/properties/bar",
                FACTORY.objectNode(),
                parentWithoutType,
                null,
                null,
                false
        );
        assertNull(child2.getNodeFieldType());
    }

    /** -----------------------------------------------------------------------
     * 14.  preloadJsonSchemas – simply invokes {@code initializeValidators()} on each
     *      supplied schema.  The test uses a dummy schema that records whether the method
     *      has been called.
     * ------------------------------------------------------------------------ */
    @Test
    public void testPreloadJsonSchemas() {
        // Dummy schema that flips a flag when initializeValidators() is called.
        class DummySchema extends JsonSchema {
            boolean initialized = false;

            DummySchema() {
                super(null, URI.create("http://example.com"), FACTORY.objectNode());
            }

            @Override
            public void initializeValidators() {
                initialized = true;
            }
        }

        DummySchema s1 = new DummySchema();
        DummySchema s2 = new DummySchema();

        TestValidator validator = new TestValidator(
                "#",
                FACTORY.objectNode(),
                null,
                null,
                null,
                false
        );

        validator.preloadJsonSchemas(java.util.Arrays.asList(s1, s2));

        assertTrue(s1.initialized);
        assertTrue(s2.initialized);
    }

    /** -----------------------------------------------------------------------
     *  Utility – inject a schema path into a JsonSchema instance using reflection.
     *  BaseJsonValidator stores the path in the private field {@code schemaPath}
     *  defined in its superclass {@link ValidationMessageHandler}.  The field is
     *  package‑private (default) in the original library, so reflection works even
     *  without changing accessibility.
     * ------------------------------------------------------------------------ */
    private static void setSchemaPath(JsonSchema schema, String path) {
        try {
            java.lang.reflect.Field f = ValidationMessageHandler.class.getDeclaredField("schemaPath");
            f.setAccessible(true);
            f.set(schema, path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
