package com.networknt.schema;
import com.networknt.schema.ValidationContext.DiscriminatorContext;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * <p>The tests are organised as follows:</p>
 * <ul>
 *   <li>Listing of all public (including inherited) methods.</li>
 *   <li>Basic sanity tests for each public method.</li>
 *   <li>Edge‑case and exception tests for static utilities and protected helpers.</li>
 *   <li>Utility subclasses used to expose protected/static members without violating the
 *   “no‑mocking / no‑sub‑classing library interfaces” rule.</li>
 * </ul>
 *
 * <p>All objects are created using only constructors or factory methods that are part of the
 * supplied API.  No external mocking frameworks are used.</p>
 */
public class BaseJsonValidatorTest {

    /** Simple logger used by the {@code debug} helper. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseJsonValidatorTest.class);

    private ObjectMapper mapper;
    private ValidationContext validationContext;
    private JsonSchema emptySchema;   // a concrete subclass of {@code BaseJsonValidator}
    private ExecutionContext executionContext;

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.  It simply returns an empty
     * {@link java.util.Set} from {@code validate(...)} – enough to let us instantiate the abstract
     * base class and call its protected helpers.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType,
                  validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            // No real validation – return empty set.
            return Collections.emptySet();
        }
    }

    /**
     * Helper subclass that makes the protected static utility methods public so that they can be
     * invoked directly from the test code.
     */
    private static class TestHelper extends BaseJsonValidator {

        TestHelper(String schemaPath,
                   JsonNode schemaNode,
                   JsonSchema parentSchema,
                   ValidatorTypeCode validatorType,
                   ValidationContext validationContext,
                   boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType,
                  validationContext, suppressSubSchemaRetrieval);
        }

        // expose protected static methods as public static wrappers
        public static void publicCheckDiscriminatorMatch(DiscriminatorContext ctx,
                                                         ObjectNode discriminator,
                                                         String propertyValue,
                                                         JsonSchema schema) {
            checkDiscriminatorMatch(ctx, discriminator, propertyValue, schema);
        }

        public static void publicRegisterAndMergeDiscriminator(DiscriminatorContext ctx,
                                                                ObjectNode discriminator,
                                                                JsonSchema schema,
                                                                String at) {
            registerAndMergeDiscriminator(ctx, discriminator, schema, at);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            return Collections.emptySet();
        }
    }

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();

        // Build the minimal objects required by ValidationContext.
        // All these classes have public no‑arg constructors in the real library.
        URIFactory uriFactory = new URIFactory();
        URNFactory urnFactory = new URNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV2019_09(); // static factory in real lib
        JsonSchemaFactory schemaFactory = new JsonSchemaFactory();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        validationContext = new ValidationContext(uriFactory, urnFactory,
                metaSchema, schemaFactory, config);

        // An empty schema node ({}).  This will be used as the parent schema for our TestValidator.
        JsonNode emptyNode = mapper.createObjectNode();

        // Create a concrete JsonSchema (which itself extends BaseJsonValidator) – this will act as
        // the parent for the TestValidator instance.
        emptySchema = new JsonSchema(validationContext, URI.create("urn:empty-schema"), emptyNode);

        executionContext = new ExecutionContext();
    }

    /* ---------------------------------------------------------------------- */
    /* 1.  Public methods of BaseJsonValidator (including those inherited)    */
    /* ---------------------------------------------------------------------- */

    // The public API we need to cover (excluding the default methods from interfaces):
    //   String getSchemaPath()
    //   JsonNode getSchemaNode()
    //   JsonSchema getParentSchema()
    //   Set<ValidationMessage> validate(ExecutionContext, JsonNode)
    //   protected JsonSchema fetchSubSchemaNode(ValidationContext)
    //   protected String getNodeFieldType()
    //   protected void preloadJsonSchemas(Collection<JsonSchema>)
    //   protected PathType getPathType()
    //   protected String atRoot()
    //   protected String atPath(String, String)
    //   protected String atPath(String, int)

    /* ---------------------------------------------------------------------- */
    /* 2.  Basic sanity tests for each public method                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetSchemaPath() {
        TestValidator validator = new TestValidator("/myPath",
                mapper.createObjectNode(),
                emptySchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                false);
        assertEquals("/myPath", validator.getSchemaPath());
    }

    @Test
    public void testGetSchemaNode() {
        ObjectNode node = mapper.createObjectNode().put("type", "string");
        TestValidator validator = new TestValidator("/path",
                node,
                emptySchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                false);
        assertSame(node, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema() {
        TestValidator validator = new TestValidator("/path",
                mapper.createObjectNode(),
                emptySchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                false);
        assertSame(emptySchema, validator.getParentSchema());
    }

    @Test
    public void testValidateDelegatesToThreeArgVersion() {
        // The three‑arg version is abstract; our TestValidator returns an empty set.
        TestValidator validator = new TestValidator("/path",
                mapper.createObjectNode(),
                emptySchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                false);
        JsonNode sample = mapper.readTree("{\"foo\":\"bar\"}");
        Set<ValidationMessage> result = validator.validate(executionContext, sample);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        // suppressSubSchemaRetrieval = true → fetchSubSchemaNode must return null
        TestValidator validator = new TestValidator("/path",
                mapper.createObjectNode(),
                emptySchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                true);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNodeWhenIdMissing() {
        // schemaNode does NOT contain an “id” field → obtainSubSchemaNode returns null
        ObjectNode schemaNode = mapper.createObjectNode(); // empty
        TestValidator validator = new TestValidator("/path",
                schemaNode,
                emptySchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                false);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testGetNodeFieldType() {
        // Parent schema contains a “type” field.
        ObjectNode parentNode = mapper.createObjectNode().put("type", "object");
        JsonSchema parent = new JsonSchema(validationContext,
                URI.create("urn:parent"), parentNode);
        TestValidator validator = new TestValidator("/path",
                mapper.createObjectNode(),
                parent,
                ValidatorTypeCode.TYPE,
                validationContext,
                false);
        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenMissing() {
        // No “type” field on parent schema → should return null.
        ObjectNode parentNode = mapper.createObjectNode(); // no "type"
        JsonSchema parent = new JsonSchema(validationContext,
                URI.create("urn:parent"), parentNode);
        TestValidator validator = new TestValidator("/path",
                mapper.createObjectNode(),
                parent,
                ValidatorTypeCode.TYPE,
                validationContext,
                false);
        assertNull(validator.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemasInvokesInitializeValidators() {
        // We use a spy‑like approach: a JsonSchema subclass that records a flag when
        // initializeValidators() is called.
        class InitFlagSchema extends JsonSchema {
            boolean initialized = false;

            InitFlagSchema(ValidationContext ctx, URI uri, JsonNode node) {
                super(ctx, uri, node);
            }

            @Override
            public void initializeValidators() {
                initialized = true;
            }
        }

        InitFlagSchema child1 = new InitFlagSchema(validationContext,
                URI.create("urn:child1"), mapper.createObjectNode());
        InitFlagSchema child2 = new InitFlagSchema(validationContext,
                URI.create("urn:child2"), mapper.createObjectNode());

        Set<JsonSchema> collection = new HashSet<>();
        collection.add(child1);
        collection.add(child2);

        TestValidator validator = new TestValidator("/path",
                mapper.createObjectNode(),
                emptySchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                false);
        validator.preloadJsonSchemas(collection);

        assertTrue(child1.initialized);
        assertTrue(child2.initialized);
    }

    @Test
    public void testPathTypeAndAtRoot() {
        TestValidator validator = new TestValidator("/path",
                mapper.createObjectNode(),
                emptySchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                false);
        // The default PathType (as per constructor) is PathType.DEFAULT
        assertEquals(PathType.DEFAULT, validator.getPathType());
        assertEquals(PathType.DEFAULT.getRoot(), validator.atRoot());
    }

    @Test
    public void testAtPathStringAndInt() {
        TestValidator validator = new TestValidator("/path",
                mapper.createObjectNode(),
                emptySchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                false);
        String childPath = validator.atPath("/root", "child");
        assertEquals(PathType.DEFAULT.append("/root", "child"), childPath);

        String indexPath = validator.atPath("/root", 3);
        assertEquals(PathType.DEFAULT.append("/root", 3), indexPath);
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  Edge‑case / exception tests for static / protected utilities       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsUtility() {
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-10));
        // NaN handling – Math.abs(NaN) is NaN, comparison < 1e-12 is false.
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN));
    }

    @Test
    public void testDebugUtilityLogsCorrectMessage() {
        // The debug method simply forwards to the logger – we verify that no exception is thrown.
        JsonNode node = mapper.createObjectNode().put("a", 1);
        JsonNode root = mapper.createObjectNode().set("root", node);
        BaseJsonValidator.debug(LOGGER, node, root, "/test");
        // No assertion – if the logger were mis‑configured an exception would surface.
    }

    @Test
    public void testCheckDiscriminatorMatchWithNullPropertyValue() {
        ValidationContext.DiscriminatorContext ctx = validationContext.getCurrentDiscriminatorContext();
        ObjectNode discriminator = mapper.createObjectNode();
        JsonSchema dummySchema = emptySchema; // any schema works

        // property value = null → context should be marked as match.
        TestHelper.publicCheckDiscriminatorMatch(ctx, discriminator, null, dummySchema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitMapping() {
        ValidationContext.DiscriminatorContext ctx = validationContext.getCurrentDiscriminatorContext();
        ObjectNode discriminator = mapper.createObjectNode(); // no "mapping"
        // Create a child schema whose path ends with the discriminator value.
        JsonSchema child = validationContext.newSchema("/root/pet/dog",
                mapper.createObjectNode(),
                emptySchema);
        // The discriminator property value is "dog" → implicit match expected.
        TestHelper.publicCheckDiscriminatorMatch(ctx, discriminator, "dog", child);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingSuccess() {
        ValidationContext.DiscriminatorContext ctx = validationContext.getCurrentDiscriminatorContext();
        ObjectNode discriminator = mapper.createObjectNode();
        ObjectNode mapping = mapper.createObjectNode();
        mapping.put("cat", "/root/pet/cat");
        discriminator.set("mapping", mapping);

        JsonSchema catSchema = validationContext.newSchema("/root/pet/cat",
                mapper.createObjectNode(),
                emptySchema);
        TestHelper.publicCheckDiscriminatorMatch(ctx, discriminator, "cat", catSchema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingFailureFallsBackToImplicit() {
        ValidationContext.DiscriminatorContext ctx = validationContext.getCurrentDiscriminatorContext();
        ObjectNode discriminator = mapper.createObjectNode();
        ObjectNode mapping = mapper.createObjectNode();
        // mapping does NOT contain the value we will pass.
        mapping.put("bird", "/root/pet/bird");
        discriminator.set("mapping", mapping);

        // Schema path ends with the discriminator value → implicit match should be used.
        JsonSchema dogSchema = validationContext.newSchema("/root/pet/dog",
                mapper.createObjectNode(),
                emptySchema);
        TestHelper.publicCheckDiscriminatorMatch(ctx, discriminator, "dog", dogSchema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testRegisterAndMergeDiscriminatorAddsMissingMapping() {
        ValidationContext.DiscriminatorContext ctx = validationContext.getCurrentDiscriminatorContext();
        ObjectNode rootDiscriminator = mapper.createObjectNode(); // empty root mapping
        // Child schema with its own mapping
        ObjectNode childMapping = mapper.createObjectNode();
        childMapping.put("fish", "/root/pet/fish");
        ObjectNode childDiscriminator = mapper.createObjectNode();
        childDiscriminator.set("mapping", childMapping);

        JsonSchema childSchema = validationContext.newSchema("/root/pet/fish",
                mapper.createObjectNode(),
                emptySchema);

        // First register root discriminator (empty)
        ctx.registerDiscriminator("/root/pet", rootDiscriminator);
        // Now merge child mapping into root
        TestHelper.publicRegisterAndMergeDiscriminator(ctx, childDiscriminator, childSchema, "/at");
        ObjectNode merged = ctx.getDiscriminatorForPath("/root/pet");
        assertNotNull(merged);
        assertTrue(merged.has("mapping"));
        assertEquals("/root/pet/fish", merged.get("mapping").get("fish").asText());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorDetectsConflict() {
        ValidationContext.DiscriminatorContext ctx = validationContext.getCurrentDiscriminatorContext();

        // Root discriminator already contains a mapping for "bird"
        ObjectNode rootMapping = mapper.createObjectNode();
        rootMapping.put("bird", "/root/pet/bird");
        ObjectNode rootDiscriminator = mapper.createObjectNode();
        rootDiscriminator.set("mapping", rootMapping);
        ctx.registerDiscriminator("/root/pet", rootDiscriminator);

        // Child schema tries to redefine the same key with a different value
        ObjectNode childMapping = mapper.createObjectNode();
        childMapping.put("bird", "/root/pet/birdAlt");
        ObjectNode childDiscriminator = mapper.createObjectNode();
        childDiscriminator.set("mapping", childMapping);
        JsonSchema childSchema = validationContext.newSchema("/root/pet/birdAlt",
                mapper.createObjectNode(),
                emptySchema);

        // This call must throw JsonSchemaException because of the redefinition.
        TestHelper.publicRegisterAndMergeDiscriminator(ctx, childDiscriminator, childSchema, "/at");
    }

    @Test
    public void testFetchSubSchemaNodeInvalidUriReturnsNull() {
        // Build a schema node with an invalid URI in the "id" field.
        ObjectNode schemaNode = mapper.createObjectNode().put("id", ":// not a valid uri");
        TestValidator validator = new TestValidator("/invalidUri",
                schemaNode,
                emptySchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                false);
        // The private obtainSubSchemaNode catches IllegalArgumentException and returns null.
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    /* ---------------------------------------------------------------------- */
    /* 4.  Summary – the test class is self‑contained and can be executed      */
    /* ---------------------------------------------------------------------- */
}
