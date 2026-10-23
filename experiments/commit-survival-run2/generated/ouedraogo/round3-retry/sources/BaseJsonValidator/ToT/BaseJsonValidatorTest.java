/**
 * JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}.
 *
 * The tests are written from the viewpoint of three “experts” that each
 * contribute a focused test case for a specific behaviour.  The final suite
 * therefore contains:
 *
 * 1.  Typical usage / happy‑path tests.
 * 2.  Edge‑case tests (null values, empty collections, extreme numbers …).
 * 3.  Error‑scenario tests (invalid URIs, redefinition of discriminators …).
 *
 * Because {@code BaseJsonValidator} is abstract we provide a tiny concrete
 * subclass that implements the abstract {@code validate(...)} method with a
 * no‑op implementation.  The subclass also exposes the protected static
 * utility methods so that they can be invoked directly from the test code.
 *
 * The suite uses Mockito for mocking collaborators (e.g. {@code Logger},
 * {@code ValidationContext}, {@code JsonSchema}) and Jackson for constructing
 * JSON nodes.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

/**
 * Concrete subclass used only for testing the protected members of
 * {@link BaseJsonValidator}.
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

    /** Simple no‑op implementation – the real validation logic is irrelevant for these tests. */
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
        return Collections.emptySet();
    }

    /** Expose the protected static methods for the test suite. */
    static void publicCheckDiscriminatorMatch(ValidationContext.DiscriminatorContext ctx,
                                              ObjectNode discriminator,
                                              String propertyValue,
                                              JsonSchema schema) {
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, propertyValue, schema);
    }

    static void publicRegisterAndMergeDiscriminator(ValidationContext.DiscriminatorContext ctx,
                                                     ObjectNode discriminator,
                                                     JsonSchema schema,
                                                     String at) {
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, discriminator, schema, at);
    }

    /** Helper that delegates to the private static method – used via reflection. */
    static boolean publicEquals(double a, double b) {
        return BaseJsonValidator.equals(a, b);
    }

    /** Helper that delegates to the private static debug method – used via reflection. */
    static void publicDebug(Logger logger, JsonNode node, JsonNode root, String at) {
        BaseJsonValidator.debug(logger, node, root, at);
    }
}

/**
 * Test suite.
 */
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema parentSchema;
    private ValidationContext validationContext;
    private Logger logger;

    @Before
    public void setUp() {
        parentSchema = mock(JsonSchema.class);
        validationContext = mock(ValidationContext.class);
        logger = mock(Logger.class);
    }

    /* --------------------------------------------------------------------- */
    /* 1.  equals(double,double) – typical, edge and precision cases          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testEquals_TypicalEquality() {
        assertTrue(TestValidator.publicEquals(1.0, 1.0));
        assertTrue(TestValidator.publicEquals(0.0, -0.0));
    }

    @Test
    public void testEquals_TypicalInequality() {
        assertFalse(TestValidator.publicEquals(1.0, 2.0));
    }

    @Test
    public void testEquals_EpsilonBoundary() {
        double a = 1.000000000001;
        double b = 1.000000000002;
        // Difference is 1e-12 which is the threshold – should be considered equal
        assertTrue(TestValidator.publicEquals(a, b));

        double c = 1.000000000001;
        double d = 1.000000000013; // diff = 1.2e-11  > 1e-12
        assertFalse(TestValidator.publicEquals(c, d));
    }

    /* --------------------------------------------------------------------- */
    /* 2.  debug(Logger,JsonNode,JsonNode,String) – logger interaction       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDebug_LogsCorrectMessage() {
        JsonNode node = MAPPER.createObjectNode().put("foo", "bar");
        JsonNode root = MAPPER.createObjectNode().put("root", true);
        String at = "$.data";

        TestValidator.publicDebug(logger, node, root, at);

        // Verify that logger.debug was called with the expected template and arguments.
        verify(logger, times(1)).debug(eq("validate( {}, {}, {})"),
                eq(node), eq(root), eq(at));
    }

    /* --------------------------------------------------------------------- */
    /* 3.  getNodeFieldType() – reads the “type” field from parent schema   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetNodeFieldType_TypePresent() {
        ObjectNode parentNode = MAPPER.createObjectNode().put("type", "object");
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);

        TestValidator validator = new TestValidator("#", MAPPER.createObjectNode(),
                parentSchema, null, validationContext, false);

        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldType_TypeAbsent() {
        when(parentSchema.getSchemaNode()).thenReturn(MAPPER.createObjectNode()); // empty object

        TestValidator validator = new TestValidator("#", MAPPER.createObjectNode(),
                parentSchema, null, validationContext, false);

        assertNull(validator.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- */
    /* 4.  fetchSubSchemaNode – suppress flag and missing “id” handling      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testFetchSubSchemaNode_SuppressedReturnsNull() {
        TestValidator validator = new TestValidator("#", MAPPER.createObjectNode(),
                parentSchema, null, validationContext, true); // suppress = true

        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNode_NoIdReturnsNull() {
        TestValidator validator = new TestValidator("#", MAPPER.createObjectNode(),
                parentSchema, null, validationContext, false); // suppress = false

        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNode_InvalidUriReturnsNull() {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("id", ":// invalid uri");
        TestValidator validator = new TestValidator("#", schemaNode,
                parentSchema, null, validationContext, false);

        // Mock the parts of ValidationContext that are accessed inside obtainSubSchemaNode
        ValidationContext mockCtx = mock(ValidationContext.class);
        URIFactory mockFactory = mock(URIFactory.class);
        when(mockCtx.getURIFactory()).thenReturn(mockFactory);
        when(mockFactory.create(anyString())).thenThrow(new IllegalArgumentException());

        assertNull(validator.fetchSubSchemaNode(mockCtx));
    }

    /* --------------------------------------------------------------------- */
    /* 5.  preloadJsonSchemas – ensure initializeValidators() is called      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemas_InvokesInitialize() {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        TestValidator validator = new TestValidator("#", MAPPER.createObjectNode(),
                parentSchema, null, validationContext, false);

        validator.preloadJsonSchemas(java.util.Arrays.asList(schema1, schema2));

        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /* 6.  Path handling – atRoot(), atPath(String, String) and atPath(...,int) */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPathRoot_DefaultImplementation() {
        TestValidator validator = new TestValidator("#", MAPPER.createObjectNode(),
                parentSchema, null, validationContext, false);

        // The default PathType (as used by the library) defines "$" as the root.
        assertEquals("$", validator.atRoot());
    }

    @Test
    public void testAtPath_StringToken_AppendsCorrectly() {
        TestValidator validator = new TestValidator("#", MAPPER.createObjectNode(),
                parentSchema, null, validationContext, false);

        String result = validator.atPath("$", "name");
        // For the default PathType the expected format is "$.name"
        assertEquals("$.name", result);
    }

    @Test
    public void testAtPath_IntIndex_AppendsCorrectly() {
        TestValidator validator = new TestValidator("#", MAPPER.createObjectNode(),
                parentSchema, null, validationContext, false);

        String result = validator.atPath("$", 3);
        // Expected format for array indices is "$[3]"
        assertEquals("$[3]", result);
    }

    /* --------------------------------------------------------------------- */
    /* 7.  checkDiscriminatorMatch – various discriminator configurations   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatch_NullPropertyValue_MarksMatch() {
        // Build a minimal DiscriminatorContext that records whether a match was made.
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext() {
            private boolean matched = false;
            @Override public void markMatch() { matched = true; }
            @Override public boolean isDiscriminatorMatchFound() { return matched; }
            // other methods are not needed for this test
            @Override public JsonSchema getDiscriminatorForPath(String path) { return null; }
            @Override public void registerDiscriminator(String path, ObjectNode discriminator) {}
        };

        ObjectNode discriminator = MAPPER.createObjectNode();
        JsonSchema anySchema = mock(JsonSchema.class);

        TestValidator.publicCheckDiscriminatorMatch(ctx, discriminator, null, anySchema);
        assertTrue("When property value is null the context must be marked as a match",
                ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitMappingMatch() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext() {
            private boolean matched = false;
            @Override public void markMatch() { matched = true; }
            @Override public boolean isDiscriminatorMatchFound() { return matched; }
            @Override public JsonSchema getDiscriminatorForPath(String path) { return null; }
            @Override public void registerDiscriminator(String path, ObjectNode discriminator) {}
        };

        // Discriminator with explicit mapping {"cat": "/schemas/cat"}
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode().put("cat", "#/definitions/cat");
        discriminator.set("mapping", mapping);

        // Schema whose path matches the mapping value
        JsonSchema catSchema = mock(JsonSchema.class);
        when(catSchema.schemaPath).thenReturn("#/definitions/cat");

        TestValidator.publicCheckDiscriminatorMatch(ctx, discriminator, "cat", catSchema);
        assertTrue("Explicit mapping should result in a match", ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitMappingNoMatch_ImplicitMatch() {
        // Context that records matches
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext() {
            private boolean matched = false;
            @Override public void markMatch() { matched = true; }
            @Override public boolean isDiscriminatorMatchFound() { return matched; }
            @Override public JsonSchema getDiscriminatorForPath(String path) { return null; }
            @Override public void registerDiscriminator(String path, ObjectNode discriminator) {}
        };

        // Discriminator with a mapping that does NOT contain the property value
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode().put("dog", "#/definitions/dog");
        discriminator.set("mapping", mapping);

        // Schema with a path that ends with the property value – implicit match should fire
        JsonSchema catSchema = mock(JsonSchema.class);
        when(catSchema.schemaPath).thenReturn("#/definitions/cat");

        TestValidator.publicCheckDiscriminatorMatch(ctx, discriminator, "cat", catSchema);
        assertTrue("When explicit mapping fails, implicit mapping (path suffix) should match", ctx.isDiscriminatorMatchFound());
    }

    /* --------------------------------------------------------------------- */
    /* 8.  registerAndMergeDiscriminator – merging and conflict detection      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testRegisterAndMergeDiscriminator_AddsRootMappingWhenMissing() {
        // Prepare a root discriminator without a mapping
        ObjectNode rootDiscriminator = MAPPER.createObjectNode().put("propertyName", "type");
        // Child schema that defines its own mapping
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        ObjectNode childMapping = MAPPER.createObjectNode().put("car", "#/definitions/car");
        childDiscriminator.set("mapping", childMapping);

        JsonSchema childSchema = mock(JsonSchema.class);
        when(childSchema.schemaPath).thenReturn("#/definitions/car");
        // The child schema node contains its own discriminator definition
        ObjectNode childSchemaNode = MAPPER.createObjectNode().set("discriminator", childDiscriminator);
        when(childSchema.schemaNode).thenReturn(childSchemaNode);

        // Minimal discriminator context that only records registrations
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext() {
            private final java.util.Map<String, ObjectNode> map = new java.util.HashMap<>();
            @Override public void markMatch() {}
            @Override public boolean isDiscriminatorMatchFound() { return false; }
            @Override public JsonSchema getDiscriminatorForPath(String path) { return null; }
            @Override public void registerDiscriminator(String path, ObjectNode discriminator) { map.put(path, discriminator); }
            public ObjectNode getRegistered(String path) { return map.get(path); }
        };

        // Register the root discriminator first
        ctx.registerDiscriminator("rootPath", rootDiscriminator);

        // Now invoke the merge logic – the child mapping should be copied to the root
        TestValidator.publicRegisterAndMergeDiscriminator(ctx, rootDiscriminator, childSchema, "at");

        // Verify that the root discriminator now contains the child's mapping entry
        ObjectNode resultingMapping = (ObjectNode) rootDiscriminator.get("mapping");
        assertNotNull("Mapping should have been added to the root discriminator", resultingMapping);
        assertEquals("#/definitions/car", resultingMapping.get("car").asText());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminator_ReDefinitionThrowsException() {
        // Root discriminator already defines a mapping for "cat"
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode rootMapping = MAPPER.createObjectNode().put("cat", "#/definitions/oldCat");
        rootDiscriminator.set("mapping", rootMapping);

        // Child schema tries to redefine the same key with a different value
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        ObjectNode childMapping = MAPPER.createObjectNode().put("cat", "#/definitions/newCat");
        childDiscriminator.set("mapping", childMapping);

        JsonSchema childSchema = mock(JsonSchema.class);
        when(childSchema.schemaPath).thenReturn("#/definitions/newCat");
        ObjectNode childSchemaNode = MAPPER.createObjectNode().set("discriminator", childDiscriminator);
        when(childSchema.schemaNode).thenReturn(childSchemaNode);

        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext() {
            @Override public void markMatch() {}
            @Override public boolean isDiscriminatorMatchFound() { return false; }
            @Override public JsonSchema getDiscriminatorForPath(String path) { return null; }
            @Override public void registerDiscriminator(String path, ObjectNode discriminator) {}
        };

        // The call must raise JsonSchemaException because the mapping key "cat"
        // is being redefined with a different target.
        TestValidator.publicRegisterAndMergeDiscriminator(ctx, rootDiscriminator, childSchema, "at");
    }

    /* --------------------------------------------------------------------- */
    /* 9.  getSchemaPath / getSchemaNode / getParentSchema – simple getters   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetters_ReturnProvidedValues() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("title", "TestSchema");
        TestValidator validator = new TestValidator("#/properties/foo",
                schemaNode,
                parentSchema,
                null,
                validationContext,
                false);

        assertEquals("#/properties/foo", validator.getSchemaPath());
        assertSame(schemaNode, validator.getSchemaNode());
        assertSame(parentSchema, validator.getParentSchema());
    }
}
