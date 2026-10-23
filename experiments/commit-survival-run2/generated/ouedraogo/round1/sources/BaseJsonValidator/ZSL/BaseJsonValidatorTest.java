package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link BaseJsonValidator}.
 *
 * The class under test is abstract, therefore a minimal concrete implementation
 * {@link TestValidator} is provided for exercising the protected methods.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    // -------------------------------------------------------------------------
    // Helper concrete implementation of the abstract BaseJsonValidator
    // -------------------------------------------------------------------------
    private static class TestValidator extends BaseJsonValidator {
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
            // simple no‑op validator – always succeeds
            return Collections.emptySet();
        }
    }

    // -------------------------------------------------------------------------
    // Common test fixtures
    // -------------------------------------------------------------------------
    private JsonNodeFactory factory = JsonNodeFactory.instance;

    @Mock
    private Logger mockLogger;

    @Mock
    private ValidationContext mockContext;

    @Mock
    private ValidationContext.Config mockConfig;

    @Mock
    private ValidationContext.DiscriminatorContext mockDiscriminatorContext;

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private JsonSchema mockChildSchema;

    @Before
    public void setUp() {
        // Default behaviour for the ValidationContext configuration
        when(mockContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.isFailFast()).thenReturn(false);
        when(mockConfig.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(mockConfig.getPathType()).thenReturn(PathType.DEFAULT);
        when(mockContext.getDiscriminatorContext()).thenReturn(mockDiscriminatorContext);
    }

    // -------------------------------------------------------------------------
    // Tests for the static utility methods
    // -------------------------------------------------------------------------

    @Test
    public void testEqualsWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-10));
    }

    @Test
    public void testDebugLogsCorrectly() {
        JsonNode node = factory.textNode("value");
        JsonNode root = factory.objectNode();
        BaseJsonValidator.debug(mockLogger, node, root, "/path");

        verify(mockLogger).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq("/path"));
    }

    // -------------------------------------------------------------------------
    // Tests for fetchSubSchemaNode (which indirectly tests obtainSubSchemaNode)
    // -------------------------------------------------------------------------

    @Test
    public void testFetchSubSchemaNodeSuppressedReturnsNull() {
        TestValidator validator = new TestValidator("#/suppress",
                factory.objectNode(),
                mockParentSchema,
                ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockContext,
                true); // suppressSubSchemaRetrieval = true

        assertNull(validator.fetchSubSchemaNode(mockContext));
    }

    @Test
    public void testFetchSubSchemaNodeWithoutIdReturnsNull() {
        TestValidator validator = new TestValidator("#/noId",
                factory.objectNode(), // no "id" property
                mockParentSchema,
                ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockContext,
                false);

        // No need to stub the factory – the method should return null before any lookup
        assertNull(validator.fetchSubSchemaNode(mockContext));
    }

    // -------------------------------------------------------------------------
    // Tests for discriminator handling
    // -------------------------------------------------------------------------

    @Test
    public void testCheckDiscriminatorMatchWhenPropertyValueIsNullMarksMatch() {
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                factory.objectNode(),
                null,
                mockChildSchema);
        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingSuccess() {
        // Build discriminator JSON: { "mapping": { "typeA": "/schemas/typeA" } }
        ObjectNode mapping = factory.objectNode();
        mapping.put("typeA", "/schemas/typeA");
        ObjectNode discriminator = factory.objectNode();
        discriminator.set("mapping", mapping);

        when(mockChildSchema.schemaPath).thenReturn("/schemas/typeA");

        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator,
                "typeA",
                mockChildSchema);

        verify(mockDiscriminatorContext).markMatch();
        verify(mockDiscriminatorContext, never()).isDiscriminatorMatchFound();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitNoMatchFallsBackToImplicit() {
        // Mapping does NOT contain the key, but implicit match is possible
        ObjectNode mapping = factory.objectNode();
        mapping.put("other", "/schemas/other");
        ObjectNode discriminator = factory.objectNode();
        discriminator.set("mapping", mapping);

        when(mockChildSchema.schemaPath).thenReturn("/schemas/typeB");

        // parent schema path that is *not* present in the explicit mapping values
        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.schemaPath).thenReturn("/schemas/parent");

        // The checkDiscriminatorMatch method internally uses the static helper
        // noExplicitDiscriminatorKeyOverride – we need a parent schema with a
        // different path to allow the implicit fallback.
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator,
                "typeB",
                mockChildSchema);

        // Implicit match should have been recorded
        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitNoMatchNoImplicitWhenOverrideExists() {
        // Mapping contains a key that points to the *parent* schema path,
        // which should suppress the implicit match.
        ObjectNode mapping = factory.objectNode();
        mapping.put("typeC", "/schemas/parent"); // points to parent
        ObjectNode discriminator = factory.objectNode();
        discriminator.set("mapping", mapping);

        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.schemaPath).thenReturn("/schemas/parent");

        when(mockChildSchema.schemaPath).thenReturn("/schemas/typeC");

        // The method should *not* call markMatch because the explicit mapping
        // overrides the implicit one.
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator,
                "typeC",
                mockChildSchema);

        verify(mockDiscriminatorContext, never()).markMatch();
    }

    @Test
    public void testRegisterAndMergeDiscriminatorAddsRootWhenNoExisting() {
        // Context with no discriminator for the given path
        when(mockDiscriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(null);
        when(mockDiscriminatorContext.registerDiscriminator(anyString(), any(ObjectNode.class)))
                .thenAnswer(invocation -> null);

        // Root discriminator (empty)
        ObjectNode rootDiscriminator = factory.objectNode();

        // Child schema without its own discriminator
        JsonSchema childSchema = mock(JsonSchema.class);
        when(childSchema.schemaNode).thenReturn(factory.objectNode());
        when(childSchema.schemaPath).thenReturn("#/child");

        BaseJsonValidator.registerAndMergeDiscriminator(mockDiscriminatorContext,
                rootDiscriminator,
                childSchema,
                "at");

        // The context should have been asked to register the (still empty) discriminator
        verify(mockDiscriminatorContext).registerDiscriminator(eq("#/child"), eq(rootDiscriminator));
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorRedefinePropertyNameThrows() {
        // Existing discriminator for the same path (simulates inheritance)
        when(mockDiscriminatorContext.getDiscriminatorForPath("#/child"))
                .thenReturn(factory.objectNode());

        // Child schema with a discriminator containing a propertyName – illegal redefinition
        ObjectNode childDiscriminator = factory.objectNode();
        childDiscriminator.set("propertyName", factory.textNode("type"));
        JsonSchema childSchema = mock(JsonSchema.class);
        when(childSchema.schemaNode).thenReturn(factory.objectNode().set("discriminator", childDiscriminator));
        when(childSchema.schemaPath).thenReturn("#/child");

        BaseJsonValidator.registerAndMergeDiscriminator(mockDiscriminatorContext,
                factory.objectNode(),
                childSchema,
                "at");
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorMappingConflictThrows() {
        // Existing root discriminator with a mapping entry "A" -> "/old"
        ObjectNode rootMapping = factory.objectNode();
        rootMapping.put("A", "/old");
        ObjectNode rootDiscriminator = factory.objectNode();
        rootDiscriminator.set("mapping", rootMapping);

        when(mockDiscriminatorContext.getDiscriminatorForPath("#/child"))
                .thenReturn(rootDiscriminator);

        // Child schema with a discriminator that also defines mapping "A" -> "/new"
        ObjectNode childMapping = factory.objectNode();
        childMapping.put("A", "/new");
        ObjectNode childDiscriminator = factory.objectNode();
        childDiscriminator.set("mapping", childMapping);
        JsonSchema childSchema = mock(JsonSchema.class);
        when(childSchema.schemaNode).thenReturn(factory.objectNode().set("discriminator", childDiscriminator));
        when(childSchema.schemaPath).thenReturn("#/child");

        BaseJsonValidator.registerAndMergeDiscriminator(mockDiscriminatorContext,
                rootDiscriminator,
                childSchema,
                "at");
    }

    // -------------------------------------------------------------------------
    // Tests for accessor / helper methods
    // -------------------------------------------------------------------------

    @Test
    public void testGetNodeFieldTypeReturnsDefinedType() {
        // Parent schema node with a "type" field
        ObjectNode parentNode = factory.objectNode();
        parentNode.put("type", "string");
        when(mockParentSchema.getSchemaNode()).thenReturn(parentNode);
        when(mockParentSchema.schemaPath).thenReturn("#/parent");

        TestValidator validator = new TestValidator("#/test",
                factory.objectNode(),
                mockParentSchema,
                ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockContext,
                false);

        assertEquals("string", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeReturnsNullWhenMissing() {
        // Parent schema without a "type" field
        when(mockParentSchema.getSchemaNode()).thenReturn(factory.objectNode());

        TestValidator validator = new TestValidator("#/test",
                factory.objectNode(),
                mockParentSchema,
                ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockContext,
                false);

        assertNull(validator.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemasInitializesAllSchemas() {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(schema1, schema2);

        TestValidator validator = new TestValidator("#/test",
                factory.objectNode(),
                mockParentSchema,
                ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockContext,
                false);

        validator.preloadJsonSchemas(schemas);

        verify(schema1).initializeValidators();
        verify(schema2).initializeValidators();
    }

    @Test
    public void testPathGenerationUsesConfiguredPathType() {
        // The validator will pick up PathType.DEFAULT from the mocked config
        TestValidator validator = new TestValidator("#/test",
                factory.objectNode(),
                mockParentSchema,
                ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockContext,
                false);

        assertEquals(PathType.DEFAULT.getRoot(), validator.atRoot());

        String root = validator.atRoot();
        String tokenPath = validator.atPath(root, "name");
        assertEquals(PathType.DEFAULT.append(root, "name"), tokenPath);

        String indexedPath = validator.atPath(root, 3);
        assertEquals(PathType.DEFAULT.append(root, 3), indexedPath);
    }

    // -------------------------------------------------------------------------
    // Boilerplate classes for the library types that are not needed in depth
    // -------------------------------------------------------------------------

    /**
     * Minimal stub for {@link ExecutionContext}. The validator under test does not
     * use any of its methods, so an empty implementation suffices.
     */
    private static class ExecutionContext {
    }
}
