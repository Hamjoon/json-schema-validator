###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

/**
 * Unit tests for {@link BaseJsonValidator}.
 *
 * The tests are placed in the same package as the class under test so that
 * protected members can be accessed directly.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    // ------------------------------------------------------------------------
    // Helper concrete implementation of the abstract BaseJsonValidator
    // ------------------------------------------------------------------------
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
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // simple no‑op validator – always returns an empty set
            return Collections.emptySet();
        }
    }

    // ------------------------------------------------------------------------
    // Mocks and shared objects
    // ------------------------------------------------------------------------
    @Mock
    private ValidationContext mockValidationContext;

    @Mock
    private ValidationContext.Config mockConfig;

    @Mock
    private ValidationContext.DiscriminatorContext mockDiscriminatorContext;

    @Mock
    private JsonSchemaFactory mockSchemaFactory;

    @Mock
    private URIFactory mockURIFactory;

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private JsonSchema mockChildSchema;

    @Mock
    private Logger mockLogger;

    private ObjectNode emptyObjectNode;
    private ObjectNode discriminatorNode;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        emptyObjectNode = JsonNodeFactory.instance.objectNode();

        // default validation context behaviour
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockSchemaFactory);
        when(mockValidationContext.getURIFactory()).thenReturn(mockURIFactory);
        when(mockConfig.isFailFast()).thenReturn(false);
        when(mockConfig.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(mockConfig.getPathType()).thenReturn(PathType.JSON_POINTER);
    }

    // ------------------------------------------------------------------------
    // Tests for the static utility method equals(double,double)
    // ------------------------------------------------------------------------
    @Test
    public void testEqualsWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-11));
        assertTrue(BaseJsonValidator.equals(-5.123456789, -5.123456789 + 5e-13));
        assertFalse(BaseJsonValidator.equals(-5.123456789, -5.123456789 + 5e-11));
    }

    // ------------------------------------------------------------------------
    // Tests for the debug(...) helper
    // ------------------------------------------------------------------------
    @Test
    public void testDebugLogsMessage() {
        JsonNode node = JsonNodeFactory.instance.textNode("value");
        JsonNode root = JsonNodeFactory.instance.objectNode();
        String at = "$.path";

        BaseJsonValidator.debug(mockLogger, node, root, at);

        // Verify that logger.debug was called with the expected format string and arguments
        verify(mockLogger).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq(at));
    }

    // ------------------------------------------------------------------------
    // Tests for fetchSubSchemaNode()
    // ------------------------------------------------------------------------
    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode();
        TestValidator validator = new TestValidator("#/test", schemaNode, mockParentSchema,
                ValidatorTypeCode.MAXIMUM, mockValidationContext, true);

        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNull(result);
    }

    @Test
    public void testFetchSubSchemaNodeNoId() {
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode(); // no "id" property
        TestValidator validator = new TestValidator("#/test", schemaNode, mockParentSchema,
                ValidatorTypeCode.MAXIMUM, mockValidationContext, false);

        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNull(result);
    }

    @Test
    public void testFetchSubSchemaNodeInvalidId() throws Exception {
        // schemaNode contains an "id" that cannot be turned into a valid URI
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("id", ":// not a uri");

        TestValidator validator = new TestValidator("#/test", schemaNode, mockParentSchema,
                ValidatorTypeCode.MAXIMUM, mockValidationContext, false);

        // URIFactory.create will throw IllegalArgumentException – we let the mock behave that way
        when(mockURIFactory.create(anyString())).thenThrow(new IllegalArgumentException());

        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNull(result);
    }

    // ------------------------------------------------------------------------
    // Tests for discriminator handling
    // ------------------------------------------------------------------------
    @Test
    public void testCheckDiscriminatorMatchNullValue() {
        // When discriminatorPropertyValue is null we expect a match to be marked immediately
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext, emptyObjectNode, null, mockChildSchema);
        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitMapping() {
        // No explicit mapping, implicit mapping based on schemaPath
        when(mockChildSchema.schemaPath).thenReturn("/myType");
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext, emptyObjectNode,
                "myType", mockChildSchema);
        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingSuccess() {
        // explicit mapping present and matches
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.put("typeA", "#/defs/typeA");
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        discriminator.set("mapping", mapping);

        when(mockChildSchema.schemaPath).thenReturn("#/defs/typeA");

        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext, discriminator,
                "typeA", mockChildSchema);
        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingFailsButImplicitAllowed() {
        // explicit mapping does NOT match, but there is no explicit key override,
        // and the implicit schemaPath matches – therefore a match should be recorded.
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.put("typeB", "#/defs/typeB");
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        discriminator.set("mapping", mapping);

        when(mockChildSchema.schemaPath).thenReturn("/typeC");
        // no explicit key overrides for parent schema (parent path is "/parent")
        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.schemaPath).thenReturn("/parent");

        // Mock the noExplicitDiscriminatorKeyOverride static call via reflection is not possible,
        // but the method uses only the mapping and parentSchema – we can rely on its logic.
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext, discriminator,
                "typeC", mockChildSchema);
        verify(mockDiscriminatorContext).markMatch();
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorRejectsPropertyNameRedefinition() {
        // Context already has a discriminator for the given schema path
        when(mockDiscriminatorContext.getDiscriminatorForPath(anyString()))
                .thenReturn(emptyObjectNode);

        // Schema contains a discriminator with a propertyName – this must cause an exception
        ObjectNode schemaDiscriminator = JsonNodeFactory.instance.objectNode();
        schemaDiscriminator.set("propertyName", new TextNode("type"));
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.set("discriminator", schemaDiscriminator);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaNode).thenReturn(schemaNode);
        when(schema.schemaPath).thenReturn("/root");

        // Invoke – should throw JsonSchemaException
        BaseJsonValidator.registerAndMergeDiscriminator(mockDiscriminatorContext,
                emptyObjectNode, schema, "$");
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesMappings() {
        // Context already holds a discriminator without a mapping
        ObjectNode contextDiscriminator = JsonNodeFactory.instance.objectNode();
        when(mockDiscriminatorContext.getDiscriminatorForPath(anyString()))
                .thenReturn(contextDiscriminator);

        // Schema has a discriminator with a mapping {"b":"#/b"}
        ObjectNode schemaMapping = JsonNodeFactory.instance.objectNode();
        schemaMapping.put("b", "#/b");
        ObjectNode schemaDiscriminator = JsonNodeFactory.instance.objectNode();
        schemaDiscriminator.set("mapping", schemaMapping);
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.set("discriminator", schemaDiscriminator);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaNode).thenReturn(schemaNode);
        when(schema.schemaPath).thenReturn("/root");

        // Perform the merge
        BaseJsonValidator.registerAndMergeDiscriminator(mockDiscriminatorContext,
                emptyObjectNode, schema, "$");

        // Verify that the context discriminator now contains the merged mapping entry
        JsonNode mergedMapping = contextDiscriminator.get("mapping");
        assertNotNull(mergedMapping);
        assertTrue(mergedMapping.has("b"));
        assertEquals("#/b", mergedMapping.get("b").asText());
    }

    // ------------------------------------------------------------------------
    // Tests for getters and path handling
    // ------------------------------------------------------------------------
    @Test
    public void testGettersReturnConstructorValues() {
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        TestValidator validator = new TestValidator("#/path", schemaNode, mockParentSchema,
                ValidatorTypeCode.MAXIMUM, mockValidationContext, false);

        assertEquals("#/path", validator.getSchemaPath());
        assertEquals(schemaNode, validator.getSchemaNode());
        assertEquals(mockParentSchema, validator.getParentSchema());
    }

    @Test
    public void testGetNodeFieldType() {
        // parent schema node contains a "type" field
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode();
        parentNode.put("type", "object");
        when(mockParentSchema.getSchemaNode()).thenReturn(parentNode);

        TestValidator validator = new TestValidator("#/path", emptyObjectNode, mockParentSchema,
                ValidatorTypeCode.MAXIMUM, mockValidationContext, false);

        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenAbsent() {
        // parent schema node does NOT contain a "type" field
        ObjectNode parentNode = JsonNodeFactory.instance.objectNode();
        when(mockParentSchema.getSchemaNode()).thenReturn(parentNode);

        TestValidator validator = new TestValidator("#/path", emptyObjectNode, mockParentSchema,
                ValidatorTypeCode.MAXIMUM, mockValidationContext, false);

        assertNull(validator.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemasInitializesAllSchemas() {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        Set<JsonSchema> schemas = new HashSet<>();
        schemas.add(schema1);
        schemas.add(schema2);

        TestValidator validator = new TestValidator("#/path", emptyObjectNode, mockParentSchema,
                ValidatorTypeCode.MAXIMUM, mockValidationContext, false);
        validator.preloadJsonSchemas(schemas);

        verify(schema1).initializeValidators();
        verify(schema2).initializeValidators();
    }

    @Test
    public void testPathHelpers() {
        TestValidator validator = new TestValidator("#/path", emptyObjectNode, mockParentSchema,
                ValidatorTypeCode.MAXIMUM, mockValidationContext, false);

        // PathType is JSON_POINTER by default in the mocked config
        assertEquals("$", validator.atRoot());

        String childPath = validator.atPath("$", "name");
        assertEquals("$.name", childPath);

        String indexedPath = validator.atPath("$", 3);
        assertEquals("$[3]", indexedPath);
    }
}
###Test END##