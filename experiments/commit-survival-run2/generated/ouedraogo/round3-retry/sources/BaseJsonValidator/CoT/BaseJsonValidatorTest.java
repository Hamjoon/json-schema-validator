/***************************************************************************************************
 * JUnit‑4 test suite for {@code com.networknt.schema.BaseJsonValidator}
 *
 * The test class creates a tiny concrete subclass of the abstract {@code BaseJsonValidator}
 * so that protected members can be exercised.  All external collaborators are mocked with
 * Mockito – this keeps the tests fast, deterministic and focused on the behaviour of the
 * methods under test.
 *
 * The suite is organised as follows:
 *
 * 1️⃣  Public API – simple “happy‑path” tests for the public getters and utility methods.  
 * 2️⃣  Edge‑case / exception tests – covering the static helper methods that handle discriminators,
 *     URI parsing, double comparison, etc.  
 * 3️⃣  Behaviour of the protected helper methods such as {@code fetchSubSchemaNode},
 *     {@code getNodeFieldType}, {@code preloadJsonSchemas} and the path‑building helpers.
 *
 * All tests run under JUnit‑4 and use the Mockito runner for automatic mock initialisation.
 ***************************************************************************************************/

package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

/* -------------------------------------------------------------------------- *
 * 1️⃣  A tiny concrete implementation of the abstract BaseJsonValidator.
 * -------------------------------------------------------------------------- */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    /* ---------------------------------------------------------------------- *
     * 2️⃣  Mocks required by the BaseJsonValidator constructor and helpers.
     * ---------------------------------------------------------------------- */
    @Mock private ValidationContext mockValidationContext;
    @Mock private ValidationContext.Config mockConfig;
    @Mock private ValidationContext.DiscriminatorContext mockDiscriminatorContext;
    @Mock private JsonSchema mockParentSchema;
    @Mock private JsonSchemaFactory mockFactory;
    @Mock private URIFactory mockURIFactory;
    @Mock private Logger mockLogger;

    private ObjectNode dummySchemaNode;
    private TestValidator validator;          // concrete subclass used in the tests
    private static final String SCHEMA_PATH = "/my/schema";

    /* ---------------------------------------------------------------------- *
     * 3️⃣  Set‑up common test data.
     * ---------------------------------------------------------------------- */
    @Before
    public void setUp() {
        // ----- basic schema node (empty – individual tests will customise it) -----
        dummySchemaNode = JsonNodeFactory.instance.objectNode();

        // ----- ValidationContext configuration (mostly defaults) -----------------
        Mockito.when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        Mockito.when(mockConfig.isFailFast()).thenReturn(false);
        Mockito.when(mockConfig.getApplyDefaultsStrategy())
               .thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        Mockito.when(mockConfig.getPathType()).thenReturn(PathType.DEFAULT);
        Mockito.when(mockValidationContext.getURIFactory()).thenReturn(mockURIFactory);
        Mockito.when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockFactory);

        // ----- parent schema – used by getNodeFieldType() ------------------------
        ObjectNode parentSchemaNode = JsonNodeFactory.instance.objectNode();
        parentSchemaNode.put("type", "string");
        Mockito.when(mockParentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
        Mockito.when(mockParentSchema.getSchemaPath()).thenReturn("/parent");

        // ----- instantiate the concrete validator --------------------------------
        validator = new TestValidator(
                SCHEMA_PATH,
                dummySchemaNode,
                mockParentSchema,
                null,                     // validator type – not needed for the tests
                mockValidationContext,
                false                     // suppressSubSchemaRetrieval = false
        );
    }

    /* -------------------------------------------------------------------------- *
     * 4️⃣  Concrete subclass – only forwards the constructor to the super‑class.
     * -------------------------------------------------------------------------- */
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

        /* The abstract class does not declare any abstract methods, therefore the
         * subclass does not need to implement anything else.  The overridden
         * validate(ExecutionContext, JsonNode) method from the super‑class is
         * sufficient for the tests that call it. */
    }

    /* -------------------------------------------------------------------------- *
     * 5️⃣  PUBLIC GETTER TESTS – happy path
     * -------------------------------------------------------------------------- */
    @Test
    public void testGetSchemaPath() {
        assertEquals(SCHEMA_PATH, validator.getSchemaPath());
    }

    @Test
    public void testGetSchemaNode() {
        assertSame(dummySchemaNode, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema() {
        assertSame(mockParentSchema, validator.getParentSchema());
    }

    /* -------------------------------------------------------------------------- *
     * 6️⃣  PATH HELPERS
     * -------------------------------------------------------------------------- */
    @Test
    public void testAtRootReturnsNonNull() {
        String root = validator.atRoot();
        assertNotNull(root);
        // The default implementation of PathType.DEFAULT returns "$"
        assertEquals(PathType.DEFAULT.getRoot(), root);
    }

    @Test
    public void testAtPathWithStringToken() {
        String result = validator.atPath("$", "name");
        assertEquals(PathType.DEFAULT.append("$", "name"), result);
    }

    @Test
    public void testAtPathWithArrayIndex() {
        String result = validator.atPath("$", 2);
        assertEquals(PathType.DEFAULT.append("$", 2), result);
    }

    @Test
    public void testGetPathType() {
        assertSame(PathType.DEFAULT, validator.getPathType());
    }

    /* -------------------------------------------------------------------------- *
     * 7️⃣  getNodeFieldType() – normal and missing‑type cases
     * -------------------------------------------------------------------------- */
    @Test
    public void testGetNodeFieldTypeWhenTypeExists() {
        // parent schema already contains {"type":"string"} from setUp()
        assertEquals("string", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenNoType() {
        // make the parent schema node empty
        Mockito.when(mockParentSchema.getSchemaNode())
               .thenReturn(JsonNodeFactory.instance.objectNode());

        assertNull(validator.getNodeFieldType());
    }

    /* -------------------------------------------------------------------------- *
     * 8️⃣  preloadJsonSchemas – each supplied schema must receive initialise()
     * -------------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemasInvokesInitialize() {
        JsonSchema schemaA = Mockito.mock(JsonSchema.class);
        JsonSchema schemaB = Mockito.mock(JsonSchema.class);
        Collection<JsonSchema> collection = Arrays.asList(schemaA, schemaB);

        validator.preloadJsonSchemas(collection);

        Mockito.verify(schemaA).initializeValidators();
        Mockito.verify(schemaB).initializeValidators();
    }

    /* -------------------------------------------------------------------------- *
     * 9️⃣  fetchSubSchemaNode – various edge cases
     * -------------------------------------------------------------------------- */
    @Test
    public void testFetchSubSchemaNodeSuppressedReturnsNull() {
        TestValidator suppressed = new TestValidator(
                SCHEMA_PATH,
                dummySchemaNode,
                mockParentSchema,
                null,
                mockValidationContext,
                true);                     // suppressSubSchemaRetrieval = true

        assertNull(suppressed.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void testFetchSubSchemaNodeNoIdFieldReturnsNull() {
        // dummySchemaNode does NOT contain an "id" field
        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void testFetchSubSchemaNodeInvalidUriReturnsNull() {
        dummySchemaNode.put("id", ":// not a valid uri");
        Mockito.doThrow(new IllegalArgumentException()).when(mockURIFactory).create(Mockito.anyString());

        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void testFetchSubSchemaNodeValidUriReturnsSchema() {
        // Prepare a valid id and a mock schema to be returned by the factory
        String id = "http://example.com/schema.json";
        dummySchemaNode.put("id", id);
        JsonSchema returnedSchema = Mockito.mock(JsonSchema.class);
        Mockito.when(mockURIFactory.create(id)).thenReturn(URI.create(id));
        Mockito.when(mockFactory.getSchema(URI.create(id), mockConfig)).thenReturn(returnedSchema);

        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertSame(returnedSchema, result);
    }

    /* -------------------------------------------------------------------------- *
     * 10️⃣  equals(double,double) – tolerance handling
     * -------------------------------------------------------------------------- */
    @Test
    public void testEqualsWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.000000000002));
    }

    @Test
    public void testEqualsOutsideTolerance() {
        assertFalse(BaseJsonValidator.equals(1.0, 1.0000001));
    }

    /* -------------------------------------------------------------------------- *
     * 11️⃣  debug(Logger, JsonNode, JsonNode, String) – should never throw
     * -------------------------------------------------------------------------- */
    @Test
    public void testDebugNeverThrows() {
        JsonNode node = JsonNodeFactory.instance.objectNode().put("a", 1);
        JsonNode root = JsonNodeFactory.instance.objectNode().put("root", true);
        BaseJsonValidator.debug(mockLogger, node, root, "path");
        // No exception ⇒ test passes.  Optionally verify that logger.debug was called.
        Mockito.verify(mockLogger).debug(Mockito.eq("validate( {}, {}, {})"),
                Mockito.same(node), Mockito.same(root), Mockito.eq("path"));
    }

    /* -------------------------------------------------------------------------- *
     * 12️⃣  Discriminator handling – a collection of focused scenarios.
     * -------------------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatchWhenPropertyValueIsNull() {
        // When the discriminator property value is null the context must be marked as a match.
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                null,                     // discriminator object not used in this case
                null,                     // discriminatorPropertyValue == null
                null);                    // jsonSchema not used

        Mockito.verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitMappingMatch() {
        // discriminatorMapping == null ⇒ implicit mapping is used.
        JsonSchema schema = Mockito.mock(JsonSchema.class);
        Mockito.when(schema.getSchemaPath()).thenReturn("/base/value");

        BaseJsonValidator.checkDiscriminatorMatch(
                mockDiscriminatorContext,
                null,                     // discriminator (no mapping)
                "value",                  // property value
                schema);

        Mockito.verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingMatch() {
        // Build a discriminator object with an explicit mapping that matches the schema path.
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode mapping = discriminator.putObject("mapping");
        mapping.put("myKey", "/schemas/mySchema");

        JsonSchema schema = Mockito.mock(JsonSchema.class);
        Mockito.when(schema.getSchemaPath()).thenReturn("/schemas/mySchema");

        BaseJsonValidator.checkDiscriminatorMatch(
                mockDiscriminatorContext,
                discriminator,
                "myKey",
                schema);

        Mockito.verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingNoMatchFallsBackToImplicit() {
        // Discriminator has an explicit mapping that does NOT match the schema.
        // The implicit mapping should be tried because the explicit key does NOT
        // override the parent schema (noExplicitDiscriminatorKeyOverride == true).
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode mapping = discriminator.putObject("mapping");
        mapping.put("otherKey", "/schemas/other");

        JsonSchema schema = Mockito.mock(JsonSchema.class);
        Mockito.when(schema.getSchemaPath()).thenReturn("/base/value");

        // make the discriminator context report that no match has been found yet
        Mockito.when(mockDiscriminatorContext.isDiscriminatorMatchFound()).thenReturn(false);
        // no explicit key overrides the parent (the method inspects the mapping values)
        // – we do not need to stub anything else because the static helper inspects the
        //   mapping directly.

        BaseJsonValidator.checkDiscriminatorMatch(
                mockDiscriminatorContext,
                discriminator,
                "value",          // property value not present in explicit mapping
                schema);

        // The implicit rule should have been applied → markMatch()
        Mockito.verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingNoMatchAndOverridePreventsImplicit() {
        // Explicit mapping exists that points to the SAME schema path as the parent,
        // therefore noImplicit fallback must be suppressed.
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode mapping = discriminator.putObject("mapping");
        // mapping key points to the parent schema path – this forces the “override” logic.
        mapping.put("value", "/parent");

        JsonSchema schema = Mockito.mock(JsonSchema.class);
        Mockito.when(schema.getSchemaPath()).thenReturn("/parent");

        // Simulate that the context has NOT yet found a match.
        Mockito.when(mockDiscriminatorContext.isDiscriminatorMatchFound()).thenReturn(false);

        BaseJsonValidator.checkDiscriminatorMatch(
                mockDiscriminatorContext,
                discriminator,
                "value",
                schema);

        // No implicit match should be performed, therefore markMatch() must NOT be called.
        Mockito.verify(mockDiscriminatorContext, Mockito.never()).markMatch();
    }

    /* -------------------------------------------------------------------------- *
     * 13️⃣  registerAndMergeDiscriminator – merge scenarios and error paths.
     * -------------------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorRedefinesPropertyNameThrows() {
        // Build a parent discriminator that already defines a propertyName.
        ObjectNode rootDiscriminator = JsonNodeFactory.instance.objectNode();
        rootDiscriminator.putObject("mapping").put("a", "/a");
        rootDiscriminator.put("propertyName", "type");   // illegal re‑definition

        // Context will return the root discriminator for the given path.
        Mockito.when(mockDiscriminatorContext.getDiscriminatorForPath("/parent"))
               .thenReturn(rootDiscriminator);

        // Schema that also contains a discriminator node.
        JsonSchema schema = Mockito.mock(JsonSchema.class);
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.set("discriminator", JsonNodeFactory.instance.objectNode()
                .put("propertyName", "type"));
        Mockito.when(schema.schemaNode).thenReturn(schemaNode);
        Mockito.when(schema.schemaPath).thenReturn("/parent");

        // The call must raise JsonSchemaException because propertyName is being redefined.
        BaseJsonValidator.registerAndMergeDiscriminator(
                mockDiscriminatorContext,
                rootDiscriminator,
                schema,
                "AT");
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesMappingsWithoutCollision() {
        // Root discriminator with an existing mapping.
        ObjectNode rootDiscriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode rootMapping = rootDiscriminator.putObject("mapping");
        rootMapping.put("a", "/a");

        // Context returns the root discriminator for the schema path.
        Mockito.when(mockDiscriminatorContext.getDiscriminatorForPath("/parent"))
               .thenReturn(rootDiscriminator);

        // Child schema with a *different* mapping.
        JsonSchema childSchema = Mockito.mock(JsonSchema.class);
        ObjectNode childDiscriminatorNode = JsonNodeFactory.instance.objectNode();
        ObjectNode childMapping = childDiscriminatorNode.putObject("mapping");
        childMapping.put("b", "/b");
        Mockito.when(childSchema.schemaNode).thenReturn(childDiscriminatorNode);
        Mockito.when(childSchema.schemaPath).thenReturn("/parent");

        BaseJsonValidator.registerAndMergeDiscriminator(
                mockDiscriminatorContext,
                rootDiscriminator,
                childSchema,
                "AT");

        // After merging, the root discriminator must contain BOTH entries.
        assertEquals("/a", rootMapping.get("a").asText());
        assertEquals("/b", rootMapping.get("b").asText());

        // Verify that the discriminator was registered for the schema path.
        Mockito.verify(mockDiscriminatorContext).registerDiscriminator("/parent", rootDiscriminator);
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorMappingCollisionThrows() {
        // Root discriminator with a mapping for key "x".
        ObjectNode rootDiscriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode rootMapping = rootDiscriminator.putObject("mapping");
        rootMapping.put("x", "/old");

        // Context returns the root discriminator.
        Mockito.when(mockDiscriminatorContext.getDiscriminatorForPath("/parent"))
               .thenReturn(rootDiscriminator);

        // Child schema tries to redefine the same key with a *different* value.
        JsonSchema childSchema = Mockito.mock(JsonSchema.class);
        ObjectNode childDiscriminatorNode = JsonNodeFactory.instance.objectNode();
        ObjectNode childMapping = childDiscriminatorNode.putObject("mapping");
        childMapping.put("x", "/new");   // different target → illegal redefinition
        Mockito.when(childSchema.schemaNode).thenReturn(childDiscriminatorNode);
        Mockito.when(childSchema.schemaPath).thenReturn("/parent");

        BaseJsonValidator.registerAndMergeDiscriminator(
                mockDiscriminatorContext,
                rootDiscriminator,
                childSchema,
                "AT");
    }

    /* -------------------------------------------------------------------------- *
     * 14️⃣  Helper method – obtainSubSchemaNode is private, but its behaviour is
     *     exercised indirectly via fetchSubSchemaNode (tested above).  No further
     *     direct tests are required.
     * -------------------------------------------------------------------------- */
}
