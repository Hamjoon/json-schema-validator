package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All public methods (including constructors)</li>
 *   <li>Protected helper methods (exposed via a concrete test subclass)</li>
 *   <li>Static utility methods</li>
 *   <li>Edge‑case handling such as missing “id”, invalid URI, suppress‑sub‑schema flag,
 *       discriminator mapping logic and path generation.</li>
 * </ul>
 *
 * <p>Mockito is used to stub the complex collaborators {@link ValidationContext},
 * {@link ValidationContext.Config}, {@link URIFactory},
 * {@link JsonSchemaFactory} and {@link JsonSchema}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    /* ---------------------------------------------------------------------- */
    /* Helper concrete implementation – the class under test is abstract.   */
    /* ---------------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext,
                    suppressSubSchemaRetrieval);
        }

        /* The abstract {@link JsonValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}
         * is not implemented by {@code BaseJsonValidator}; the concrete subclass must provide it.
         * For the purpose of these tests we simply return an empty set – the behaviour of the
         * other helper methods does not depend on validation results.
         */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            return Collections.emptySet();
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Common test fixtures                                                  */
    /* ---------------------------------------------------------------------- */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock private ValidationContext mockValidationContext;
    @Mock private ValidationContext.Config mockConfig;
    @Mock private URIFactory mockUriFactory;
    @Mock private JsonSchemaFactory mockSchemaFactory;
    @Mock private JsonSchema mockParentSchema;
    @Mock private JsonSchema mockSubSchema;
    @Mock private ExecutionContext mockExecutionContext;
    @Mock private Logger mockLogger;

    private ObjectNode simpleSchemaNode;

    @Before
    public void setUp() throws Exception {
        // ----- a minimal schema node used by many tests -----
        simpleSchemaNode = MAPPER.createObjectNode();
        simpleSchemaNode.put("type", "object");

        // ----- default stubbing for ValidationContext hierarchy -----
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.isFailFast()).thenReturn(false);
        when(mockConfig.getApplyDefaultsStrategy())
                .thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(mockConfig.getPathType()).thenReturn(PathType.DEFAULT);
        when(mockValidationContext.getURIFactory()).thenReturn(mockUriFactory);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockSchemaFactory);
        // the factory will simply return the mocked sub‑schema for any URI
        when(mockSchemaFactory.getSchema(any(URI.class), any()))
                .thenReturn(mockSubSchema);
    }

    /* ---------------------------------------------------------------------- */
    /* 1.  Public API – getters & basic behaviour                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetters() {
        TestValidator validator = new TestValidator(
                "/mySchema",
                simpleSchemaNode,
                mockParentSchema,
                ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockValidationContext,
                false);

        assertEquals("/mySchema", validator.getSchemaPath());
        assertSame(simpleSchemaNode, validator.getSchemaNode());
        assertSame(mockParentSchema, validator.getParentSchema());
    }

    @Test
    public void testValidateDelegatesToProtectedMethod() {
        TestValidator validator = new TestValidator(
                "/schema",
                simpleSchemaNode,
                mockParentSchema,
                ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockValidationContext,
                false);

        Set<ValidationMessage> result = validator.validate(mockExecutionContext, MAPPER.createObjectNode());
        // The concrete implementation returns an empty set
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetNodeFieldType() {
        // parent schema contains a "type" field
        ObjectNode parentSchemaNode = MAPPER.createObjectNode();
        parentSchemaNode.put("type", "string");
        when(mockParentSchema.getSchemaNode()).thenReturn(parentSchemaNode);

        TestValidator validator = new TestValidator(
                "/schema",
                simpleSchemaNode,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockValidationContext,
                false);

        assertEquals("string", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenMissing() {
        when(mockParentSchema.getSchemaNode()).thenReturn(MAPPER.createObjectNode()); // no "type"
        TestValidator validator = new TestValidator(
                "/schema",
                simpleSchemaNode,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockValidationContext,
                false);
        assertNull(validator.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemas() {
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        List<JsonSchema> schemas = Arrays.asList(schemaA, schemaB);

        TestValidator validator = new TestValidator(
                "/schema",
                simpleSchemaNode,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockValidationContext,
                false);

        validator.preloadJsonSchemas(schemas);
        // each schema should receive a call to initializeValidators()
        verify(schemaA).initializeValidators();
        verify(schemaB).initializeValidators();
    }

    @Test
    public void testPathHelpers() {
        TestValidator validator = new TestValidator(
                "/schema",
                simpleSchemaNode,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockValidationContext,
                false);

        // default PathType uses '/' as separator and root is "$"
        assertEquals("$", validator.atRoot());
        assertEquals("$.foo", validator.atPath("$", "foo"));
        assertEquals("$.items[3]", validator.atPath("$.items", 3));
    }

    /* ---------------------------------------------------------------------- */
    /* 2.  fetchSubSchemaNode – normal and suppressed cases                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNodeReturnsNullWhenSuppressed() {
        TestValidator validator = new TestValidator(
                "/schema",
                simpleSchemaNode,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockValidationContext,
                true); // suppress flag = true

        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void testFetchSubSchemaNodeWhenIdMissingReturnsNull() {
        // schemaNode has no "id" property
        TestValidator validator = new TestValidator(
                "/schema",
                simpleSchemaNode,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockValidationContext,
                false);

        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
        verifyNoInteractions(mockUriFactory, mockSchemaFactory);
    }

    @Test
    public void testFetchSubSchemaNodeWithValidId() {
        // add a proper "id" that differs from "$schema"
        simpleSchemaNode.put("id", "http://example.com/my-schema.json");
        simpleSchemaNode.put("$schema", "http://json-schema.org/draft-07/schema#");

        TestValidator validator = new TestValidator(
                "/schema",
                simpleSchemaNode,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockValidationContext,
                false);

        // prepare a concrete URI that the factory will accept
        URI fakeUri = URI.create("http://example.com/my-schema.json");
        when(mockUriFactory.create("http://example.com/my-schema.json"))
                .thenReturn(fakeUri);

        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertSame(mockSubSchema, result);
        verify(mockUriFactory).create("http://example.com/my-schema.json");
        verify(mockSchemaFactory).getSchema(fakeUri, mockConfig);
    }

    @Test
    public void testFetchSubSchemaNodeWithInvalidUriReturnsNull() {
        simpleSchemaNode.put("id", "::::invalid-uri::::");
        TestValidator validator = new TestValidator(
                "/schema",
                simpleSchemaNode,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockValidationContext,
                false);

        // simulate IllegalArgumentException from URIFactory
        when(mockUriFactory.create("::::invalid-uri::::"))
                .thenThrow(new IllegalArgumentException("bad uri"));

        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
        verify(mockUriFactory).create("::::invalid-uri::::");
        verifyNoMoreInteractions(mockSchemaFactory);
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  Static utility methods                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.000000000002));
        assertFalse(BaseJsonValidator.equals(1.0, 1.1));
    }

    @Test
    public void testDebugLogsCorrectly() {
        JsonNode node = simpleSchemaNode;
        JsonNode root = simpleSchemaNode.deepCopy();
        BaseJsonValidator.debug(mockLogger, node, root, "/at");
        verify(mockLogger).debug("validate( {}, {}, {})", node, root, "/at");
    }

    /* ---------------------------------------------------------------------- */
    /* 4.  Discriminator handling – implicit & explicit mapping logic       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatchNullPropertyMarksMatch() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        JsonSchema dummySchema = mock(JsonSchema.class);

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, dummySchema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitMappingMatch() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode(); // no "mapping"
        // schema path that ends with the discriminator value
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/base/xyz");

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "xyz", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingMatch() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("cat", "#/definitions/catSchema");
        discriminator.set("mapping", mapping);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("#/definitions/catSchema");

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "cat", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingNoMatchFallsBackToImplicit() {
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        // explicit mapping points to a different schema path
        mapping.put("dog", "#/definitions/dogSchema");
        discriminator.set("mapping", mapping);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/animals/dog");

        // No explicit match, no overriding key – should fall back to implicit match
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "dog", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorDetectsRedefinitionOfPropertyName() {
        // Build a parent schema that already defines a discriminator with a propertyName
        ObjectNode parentDiscriminator = MAPPER.createObjectNode();
        parentDiscriminator.put("propertyName", "type");
        JsonSchema parentSchema = mock(JsonSchema.class);
        ObjectNode parentSchemaNode = MAPPER.createObjectNode();
        parentSchemaNode.set("discriminator", parentDiscriminator);
        when(parentSchema.schemaNode).thenReturn(parentSchemaNode);
        when(parentSchema.schemaPath).thenReturn("/parent");

        // Context already knows the discriminator for the parent path
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ctx.registerDiscriminator("/parent", MAPPER.createObjectNode());

        // Child schema with its own discriminator (no propertyName)
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        ObjectNode childMapping = MAPPER.createObjectNode();
        childMapping.put("dog", "#/definitions/dog");
        childDiscriminator.set("mapping", childMapping);
        JsonSchema childSchema = mock(JsonSchema.class);
        ObjectNode childSchemaNode = MAPPER.createObjectNode();
        childSchemaNode.set("discriminator", childDiscriminator);
        when(childSchema.schemaNode).thenReturn(childSchemaNode);
        when(childSchema.schemaPath).thenReturn("/parent/child");

        // The call should throw because the root discriminator already has a propertyName
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, childDiscriminator, childSchema, "/");
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesMappingsWithoutCollision() {
        // Root discriminator without mapping
        ValidationContext.DiscriminatorContext ctx = new ValidationContext.DiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ctx.registerDiscriminator("/root", rootDiscriminator);

        // Child schema with a mapping
        ObjectNode childMapping = MAPPER.createObjectNode();
        childMapping.put("cat", "#/defs/cat");
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        childDiscriminator.set("mapping", childMapping);
        JsonSchema childSchema = mock(JsonSchema.class);
        ObjectNode childSchemaNode = MAPPER.createObjectNode();
        childSchemaNode.set("discriminator", childDiscriminator);
        when(childSchema.schemaNode).thenReturn(childSchemaNode);
        when(childSchema.schemaPath).thenReturn("/root/animal");

        // Should merge mapping into the root discriminator without exception
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, childDiscriminator, childSchema, "/");

        // Verify that the root discriminator now contains the merged entry
        JsonNode mergedMapping = ctx.getDiscriminatorForPath("/root").get("mapping");
        assertNotNull(mergedMapping);
        assertEquals("#/defs/cat", mergedMapping.get("cat").asText());
    }

    /* ---------------------------------------------------------------------- */
    /* 5.  Constructor edge cases – null arguments                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConstructorHandlesNullValidationContext() {
        // All nullable arguments are set to null – the constructor must not throw
        TestValidator validator = new TestValidator(
                "/path",
                simpleSchemaNode,
                null,
                null,
                null,
                false);

        // The internal defaults should be applied
        assertEquals(PathType.DEFAULT, validator.getPathType());
        // suppress flag default is false
        assertFalse(validator.suppressSubSchemaRetrieval);
    }

    @Test
    public void testConstructorAppliesDefaultApplyDefaultsStrategy() {
        when(mockConfig.getApplyDefaultsStrategy()).thenReturn(null);
        TestValidator validator = new TestValidator(
                "/path",
                simpleSchemaNode,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockValidationContext,
                false);

        // The strategy should be the EMPTY one defined in ApplyDefaultsStrategy
        assertSame(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY,
                validator.applyDefaultsStrategy);
    }
}
