package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * The tests focus on the **public** and **protected** behaviour that can be exercised
 * without touching the rest of the library.  A tiny concrete subclass
 * {@code TestValidator} is created inside the test file so that we can instantiate
 * the abstract {@code BaseJsonValidator}.  Wherever a {@link JsonSchema} instance
 * is required we use the public constructors of {@code JsonSchema} with
 * {@code null} for the {@link ValidationContext} (the constructor safely handles a
 * {@code null} context) and we set {@code suppressSubSchemaRetrieval=true} to avoid
 * any interaction with the optional sub‑schema loading logic.
 *
 * The test cases cover:
 *
 * <ul>
 *   <li>basic getters – {@code getSchemaPath()}, {@code getSchemaNode()},
 *       {@code getParentSchema()}</li>
 *   <li>the {@code fetchSubSchemaNode()} shortcut when sub‑schema retrieval is
 *       suppressed.</li>
 *   <li>the static {@code equals(double,double)} utility with values inside and
 *       outside the tolerance.</li>
 *   <li>path handling – {@code atRoot()}, {@code atPath(String,String)} and
 *       {@code atPath(String,int)} using the default {@link PathType}.</li>
 *   <li>the {@code getNodeFieldType()} helper that reads the parent schema’s
 *       {@code type} keyword.</li>
 *   <li>pre‑loading of child schemas via {@code preloadJsonSchemas()}.</li>
 *   <li>discriminator matching – both implicit (null property value) and explicit
 *       mapping scenarios.</li>
 *   <li>registration and merging of discriminators, ensuring that the context
 *       receives the expected mapping without throwing.</li>
 * </ul>
 *
 * All objects are built using only the constructors and methods listed in the
 * supplied source signatures – no additional mocking frameworks are used.
 */
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} that simply
     * returns an empty validation result.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema,
                    ValidatorTypeCode.TRUE,   // any enum value is acceptable for the test
                    validationContext,
                    suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at) {
            // No real validation – return an empty set.
            return Collections.emptySet();
        }
    }

    /**
     * Extension of {@link JsonSchema} that records whether {@code initializeValidators()}
     * has been invoked.  This allows us to verify {@code preloadJsonSchemas()}.
     */
    private static class TestJsonSchema extends JsonSchema {

        private boolean initialized = false;

        TestJsonSchema(ValidationContext ctx, URI uri, JsonNode schemaNode) {
            super(ctx, uri, schemaNode, true); // suppress sub‑schema retrieval
        }

        @Override
        public void initializeValidators() {
            initialized = true;
            super.initializeValidators();
        }

        boolean isInitialized() {
            return initialized;
        }
    }

    /* -------------------------------------------------
     *  Simple getter tests
     * ------------------------------------------------- */

    @Test
    public void testGetSchemaPath() {
        String path = "/properties/name";
        TestValidator validator = new TestValidator(path,
                MAPPER.createObjectNode(),
                null,
                null,
                true);
        Assert.assertEquals(path, validator.getSchemaPath());
    }

    @Test
    public void testGetSchemaNode() {
        ObjectNode node = MAPPER.createObjectNode().put("type", "string");
        TestValidator validator = new TestValidator("/type",
                node,
                null,
                null,
                true);
        Assert.assertSame(node, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema() {
        // parent schema with a simple node
        JsonNode parentNode = MAPPER.createObjectNode().put("type", "integer");
        JsonSchema parent = new JsonSchema(null, URI.create("urn:parent"), parentNode, true);
        TestValidator validator = new TestValidator("/child",
                MAPPER.createObjectNode(),
                parent,
                null,
                true);
        Assert.assertSame(parent, validator.getParentSchema());
    }

    /* -------------------------------------------------
     *  fetchSubSchemaNode – suppressed retrieval
     * ------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        TestValidator validator = new TestValidator("/any",
                MAPPER.createObjectNode(),
                null,
                null,
                true); // suppressSubSchemaRetrieval = true
        // The method must return null without touching the (null) ValidationContext.
        Assert.assertNull(validator.fetchSubSchemaNode(null));
    }

    /* -------------------------------------------------
     *  equals(double,double) – tolerance handling
     * ------------------------------------------------- */

    @Test
    public void testEqualsWithinTolerance() {
        double a = 1.000000000001;
        double b = 1.000000000002;
        Assert.assertTrue(BaseJsonValidator.equals(a, b));
    }

    @Test
    public void testEqualsOutsideTolerance() {
        double a = 1.0;
        double b = 1.0 + 1e-10; // larger than 1e-12
        Assert.assertFalse(BaseJsonValidator.equals(a, b));
    }

    /* -------------------------------------------------
     *  Path handling – atRoot() and atPath()
     * ------------------------------------------------- */

    @Test
    public void testAtRootUsesDefaultPathType() {
        TestValidator validator = new TestValidator("/root",
                MAPPER.createObjectNode(),
                null,
                null,
                true);
        // PathType.DEFAULT returns "$"
        Assert.assertEquals(PathType.DEFAULT.getRoot(), validator.atRoot());
    }

    @Test
    public void testAtPathWithStringToken() {
        TestValidator validator = new TestValidator("/root",
                MAPPER.createObjectNode(),
                null,
                null,
                true);
        String childPath = validator.atPath(validator.atRoot(), "properties");
        // For the default PathType the expected result is "$.properties"
        Assert.assertEquals(PathType.DEFAULT.append(PathType.DEFAULT.getRoot(), "properties"), childPath);
    }

    @Test
    public void testAtPathWithArrayIndex() {
        TestValidator validator = new TestValidator("/root",
                MAPPER.createObjectNode(),
                null,
                null,
                true);
        String indexedPath = validator.atPath(validator.atRoot(), 3);
        Assert.assertEquals(PathType.DEFAULT.append(PathType.DEFAULT.getRoot(), 3), indexedPath);
    }

    /* -------------------------------------------------
     *  getNodeFieldType – reads parent schema's "type"
     * ------------------------------------------------- */

    @Test
    public void testGetNodeFieldTypeWhenPresent() {
        // parent schema containing a "type" field
        ObjectNode parentNode = MAPPER.createObjectNode().put("type", "number");
        JsonSchema parent = new JsonSchema(null, URI.create("urn:parent"), parentNode, true);
        TestValidator validator = new TestValidator("/child",
                MAPPER.createObjectNode(),
                parent,
                null,
                true);
        Assert.assertEquals("number", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenAbsent() {
        JsonSchema parent = new JsonSchema(null, URI.create("urn:parent"), MAPPER.createObjectNode(), true);
        TestValidator validator = new TestValidator("/child",
                MAPPER.createObjectNode(),
                parent,
                null,
                true);
        Assert.assertNull(validator.getNodeFieldType());
    }

    /* -------------------------------------------------
     *  preloadJsonSchemas – ensure initializeValidators() is called
     * ------------------------------------------------- */

    @Test
    public void testPreloadJsonSchemasInvokesInitialize() {
        TestJsonSchema child1 = new TestJsonSchema(null,
                URI.create("urn:child1"),
                MAPPER.createObjectNode());
        TestJsonSchema child2 = new TestJsonSchema(null,
                URI.create("urn:child2"),
                MMapper.createObjectNode());

        TestValidator validator = new TestValidator("/root",
                MAPPER.createObjectNode(),
                null,
                null,
                true);
        validator.preloadJsonSchemas(Set.of(child1, child2));

        Assert.assertTrue(child1.isInitialized());
        Assert.assertTrue(child2.isInitialized());
    }

    /* -------------------------------------------------
     *  Discriminator handling – implicit match
     * ------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatchImplicitNullValue() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode(); // empty discriminator
        JsonSchema dummySchema = new JsonSchema(null, URI.create("urn:dummy"), MAPPER.createObjectNode(), true);

        // discriminatorPropertyValue == null => should mark a match
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, dummySchema);
        Assert.assertTrue(ctx.isDiscriminatorMatchFound());
    }

    /* -------------------------------------------------
     *  Discriminator handling – explicit mapping match
     * ------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatchExplicitMapping() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();

        // Build discriminator with an explicit mapping: "dog" -> "/pets/dog"
        ObjectNode mapping = MAPPER.createObjectNode().put("dog", "/pets/dog");
        ObjectNode discriminator = MAPPER.createObjectNode().set("mapping", mapping);

        // Schema whose path exactly matches the mapping value
        JsonSchema matchingSchema = new JsonSchema(null,
                URI.create("urn:match"),
                MAPPER.createObjectNode(),
                true) {
            @Override
            public String getSchemaPath() {
                return "/pets/dog";
            }
        };

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "dog", matchingSchema);
        Assert.assertTrue(ctx.isDiscriminatorMatchFound());
    }

    /* -------------------------------------------------
     *  registerAndMergeDiscriminator – basic registration
     * ------------------------------------------------- */

    @Test
    public void testRegisterAndMergeDiscriminatorSimple() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();

        // Root discriminator (empty mapping)
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();

        // Schema that contains its own discriminator definition
        ObjectNode schemaNode = MAPPER.createObjectNode()
                .set("discriminator", MAPPER.createObjectNode().put("propertyName", "type"));
        JsonSchema schema = new JsonSchema(null,
                URI.create("urn:schema"),
                schemaNode,
                true) {
            @Override
            public String getSchemaPath() {
                return "/components/schemas/Animal";
            }
        };

        // The method should register the discriminator without throwing.
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at /components/schemas/Animal");

        ObjectNode stored = ctx.getDiscriminatorForPath("/components/schemas/Animal");
        Assert.assertNotNull(stored);
        // The stored discriminator must contain the original propertyName.
        Assert.assertEquals("type", stored.get("propertyName").asText());
    }

    /* -------------------------------------------------
     *  registerAndMergeDiscriminator – merging mappings
     * ------------------------------------------------- */

    @Test
    public void testRegisterAndMergeDiscriminatorWithMappings() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();

        // Root discriminator already has a mapping for "cat"
        ObjectNode rootMapping = MAPPER.createObjectNode().put("cat", "/pets/cat");
        ObjectNode rootDiscriminator = MAPPER.createObjectNode().set("mapping", rootMapping);

        // Child schema adds a new mapping for "dog"
        ObjectNode childMapping = MAPPER.createObjectNode().put("dog", "/pets/dog");
        ObjectNode childDiscriminatorNode = MAPPER.createObjectNode()
                .put("propertyName", "type")
                .set("mapping", childMapping);
        JsonSchema childSchema = new JsonSchema(null,
                URI.create("urn:child"),
                childDiscriminatorNode,
                true) {
            @Override
            public String getSchemaPath() {
                return "/components/schemas/Pet";
            }
        };

        // Register & merge – should combine mappings without exception.
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema,
                "at /components/schemas/Pet");

        ObjectNode merged = ctx.getDiscriminatorForPath("/components/schemas/Pet");
        Assert.assertNotNull(merged);
        // Verify both entries are present.
        Assert.assertEquals("/pets/cat", merged.get("mapping").get("cat").asText());
        Assert.assertEquals("/pets/dog", merged.get("mapping").get("dog").asText());
    }

    /* -------------------------------------------------
     *  debug() – ensure it does not throw (logger is optional)
     * ------------------------------------------------- */

    @Test
    public void testDebugDoesNotThrow() {
        // Use a simple SLF4J logger implementation (no actual logging required)
        Logger logger = org.slf4j.LoggerFactory.getLogger(BaseJsonValidatorTest.class);
        JsonNode node = MAPPER.createObjectNode().put("sample", "value");
        JsonNode root = MAPPER.createObjectNode().set("root", node);
        // The method only logs; we just verify that no exception bubbles up.
        BaseJsonValidator.debug(logger, node, root, "at /sample");
    }
}
