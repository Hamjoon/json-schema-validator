package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.slf4j.Logger;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BaseJsonValidator validator;

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ValidationContext.Config config;

    @Mock
    private ValidationContext.URIFactory uriFactory;

    @Mock
    private JsonSchemaFactory jsonSchemaFactory;

    @Mock
    private JsonSchema fetchedSchema;

    @Mock
    private Logger logger;

    @Mock
    private ValidationContext.DiscriminatorContext discriminatorContext;

    @Before
    public void setUp() {
        // minimal concrete subclass of BaseJsonValidator
        validator = new BaseJsonValidator("schemaPath", NullNode.getInstance(),
                mock(JsonSchema.class), ValidatorTypeCode.ADDITIONAL_PROPERTIES, validationContext) {
        };
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(config.getPathType()).thenReturn(PathType.DEFAULT);
    }

    /* ---------- equals(double,double) ---------- */
    @Test
    public void testEqualsWithinEpsilon() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.0));
        assertTrue(BaseJsonValidator.equals(-5.0, -5.0));
    }

    @Test
    public void testEqualsOutsideEpsilon() {
        assertFalse(BaseJsonValidator.equals(1.0, 1.0000001));
    }

    /* ---------- debug(Logger,JsonNode,JsonNode,String) ---------- */
    @Test
    public void testDebugCallsLogger() {
        JsonNode node = TextNode.valueOf("node");
        JsonNode root = TextNode.valueOf("root");
        BaseJsonValidator.debug(logger, node, root, "at");
        verify(logger, times(1)).debug("validate( {}, {}, {})", node, root, "at");
    }

    /* ---------- fetchSubSchemaNode ---------- */
    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        BaseJsonValidator suppressed = new BaseJsonValidator("path", NullNode.getInstance(),
                mock(JsonSchema.class), ValidatorTypeCode.ADDITIONAL_PROPERTIES, validationContext, true) {
        };
        assertNull(suppressed.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNodeObtainSuccess() {
        // prepare schemaNode with an "id"
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("id", "http://example.com/schema");
        validator.schemaNode = schemaNode;

        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(uriFactory.create("http://example.com/schema")).thenReturn(URI.create("http://example.com/schema"));
        when(validationContext.getJsonSchemaFactory()).thenReturn(jsonSchemaFactory);
        when(jsonSchemaFactory.getSchema(URI.create("http://example.com/schema"), config)).thenReturn(fetchedSchema);

        JsonSchema result = validator.fetchSubSchemaNode(validationContext);
        assertSame(fetchedSchema, result);
    }

    @Test
    public void testFetchSubSchemaNodeObtainFailsWhenIdMissing() {
        validator.schemaNode = MAPPER.createObjectNode(); // no "id"
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    /* ---------- getNodeFieldType ---------- */
    @Test
    public void testGetNodeFieldTypeFound() {
        JsonSchema parent = mock(JsonSchema.class);
        ObjectNode parentSchemaNode = MAPPER.createObjectNode();
        parentSchemaNode.put("type", "object");
        when(parent.getSchemaNode()).thenReturn(parentSchemaNode);
        validator.parentSchema = parent;

        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeMissing() {
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getSchemaNode()).thenReturn(MAPPER.createObjectNode()); // empty
        validator.parentSchema = parent;

        assertNull(validator.getNodeFieldType());
    }

    /* ---------- preloadJsonSchemas ---------- */
    @Test
    public void testPreloadJsonSchemasCallsInitialize() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        List<JsonSchema> list = Arrays.asList(s1, s2);
        validator.preloadJsonSchemas(list);
        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
    }

    /* ---------- Path handling ---------- */
    @Test
    public void testAtRootUsesPathTypeRoot() {
        assertEquals(PathType.DEFAULT.getRoot(), validator.atRoot());
    }

    @Test
    public void testAtPathStringToken() {
        String result = validator.atPath("/a", "b");
        assertEquals(PathType.DEFAULT.append("/a", "b"), result);
    }

    @Test
    public void testAtPathIntIndex() {
        String result = validator.atPath("/a", 2);
        assertEquals(PathType.DEFAULT.append("/a", 2), result);
    }

    /* ---------- Discriminator handling ---------- */

    // Helper to create a simple JsonSchema stub
    private static class SimpleJsonSchema implements JsonSchema {
        final String schemaPath;
        final ObjectNode schemaNode;

        SimpleJsonSchema(String schemaPath, ObjectNode schemaNode) {
            this.schemaPath = schemaPath;
            this.schemaNode = schemaNode;
        }

        @Override public String getSchemaPath() { return schemaPath; }
        @Override public JsonNode getSchemaNode() { return schemaNode; }
        @Override public void initializeValidators() {}
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node) { return null; }
        @Override public void initialize() {}
        @Override public JsonSchema getRefSchema() { return null; }
        @Override public void setParentSchema(JsonSchema parent) {}
        @Override public JsonSchema getRefSchema(String ref) { return null; }
        @Override public JsonSchema getRootSchema() { return null; }
        @Override public JsonSchema getSchemaById(URI id) { return null; }
        @Override public boolean isReadOnly() { return false; }
        @Override public void setReadOnly(boolean readOnly) {}
        @Override public boolean isWriteOnly() { return false; }
        @Override public void setWriteOnly(boolean writeOnly) {}
        @Override public boolean isDeprecated() { return false; }
        @Override public void setDeprecated(boolean deprecated) {}
        @Override public boolean isNullable() { return false; }
        @Override public void setNullable(boolean nullable) {}
        @Override public boolean isAdditionalPropertiesAllowed() { return true; }
        @Override public void setAdditionalPropertiesAllowed(boolean additionalPropertiesAllowed) {}
        @Override public JsonSchema getSchemaNodeByPath(String path) { return null; }
        @Override public Map<String, JsonSchema> getSchemaMap() { return null; }
        @Override public JsonSchema getSchema(String path) { return null; }
        @Override public JsonSchema getSchema(String path, boolean resolveRefs) { return null; }
        @Override public Set<String> getEnumValues() { return null; }
        @Override public void setEnumValues(Set<String> enumValues) {}
        @Override public String getId() { return null; }
        @Override public void setId(String id) {}
        @Override public void setSchemaPath(String schemaPath) {}
        @Override public JsonSchema getRefResolvedSchema() { return null; }
        @Override public boolean isRecursive() { return false; }
        @Override public void setRecursive(boolean recursive) {}
    }

    @Test
    public void testCheckDiscriminatorMatchNullValueMarksMatch() {
        // discriminatorPropertyValue == null => immediate match
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                MAPPER.createObjectNode(),
                null,
                mock(JsonSchema.class));

        verify(discriminatorContext, times(1)).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingMatch() {
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("typeA", "/schemas/typeA");
        discriminator.set("mapping", mapping);

        SimpleJsonSchema schema = new SimpleJsonSchema("/schemas/typeA", MAPPER.createObjectNode());

        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                discriminator,
                "typeA",
                schema);

        verify(discriminatorContext, times(1)).markMatch();
        verify(discriminatorContext, never()).isDiscriminatorMatchFound();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitNoMatchImplicitAllowed() {
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("typeB", "/schemas/typeB");
        discriminator.set("mapping", mapping);

        SimpleJsonSchema schema = new SimpleJsonSchema("/schemas/typeC", MAPPER.createObjectNode());

        // no explicit match, but implicit should succeed because path ends with value
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                discriminator,
                "typeC",
                schema);

        // markMatch should be called via implicit match
        verify(discriminatorContext, times(1)).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitNoMatchImplicitBlockedByOverride() {
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        // mapping key overrides the implicit path for the parent schema
        mapping.put("typeC", "/schemas/parent");
        discriminator.set("mapping", mapping);

        SimpleJsonSchema parentSchema = new SimpleJsonSchema("/schemas/parent", MAPPER.createObjectNode());

        // discriminatorPropertyValue is "typeC", but explicit mapping points to a different schemaPath
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext,
                discriminator,
                "typeC",
                parentSchema);

        // No match should be recorded
        verify(discriminatorContext, never()).markMatch();
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorThrowsWhenPropertyNameRedefined() {
        // root discriminator already registered
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        rootDiscriminator.put("propertyName", "type");
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);

        // schema that also defines a discriminator with a propertyName (illegal)
        ObjectNode schemaDiscriminatorNode = MAPPER.createObjectNode();
        schemaDiscriminatorNode.put("propertyName", "type");
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", schemaDiscriminatorNode);
        SimpleJsonSchema schema = new SimpleJsonSchema("/schemas/child", schemaNode);

        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                MAPPER.createObjectNode(),
                schema,
                "at ");
    }

    @Test
    public void testRegisterAndMergeDiscriminatorAddsMissingMapping() {
        // root has no mapping
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);
        when(discriminatorContext.getDiscriminatorForPath("/schemas/child")).thenReturn(null);

        // child schema defines a mapping
        ObjectNode childMapping = MAPPER.createObjectNode();
        childMapping.put("typeX", "/schemas/typeX");
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        childDiscriminator.set("mapping", childMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", childDiscriminator);
        SimpleJsonSchema childSchema = new SimpleJsonSchema("/schemas/child", schemaNode);

        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                MAPPER.createObjectNode(),
                childSchema,
                "at ");

        // after merging, root discriminator should now contain the mapping
        verify(discriminatorContext, times(1)).registerDiscriminator(eq("/schemas/child"), any(ObjectNode.class));
        assertNotNull(rootDiscriminator.get("mapping"));
        assertEquals(childMapping, rootDiscriminator.get("mapping"));
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesWithoutConflict() {
        // root discriminator already has a mapping
        ObjectNode rootMapping = MAPPER.createObjectNode();
        rootMapping.put("typeA", "/schemas/typeA");
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        rootDiscriminator.set("mapping", rootMapping);
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);
        when(discriminatorContext.getDiscriminatorForPath("/schemas/child")).thenReturn(null);

        // child adds a new mapping that does not conflict
        ObjectNode childMapping = MAPPER.createObjectNode();
        childMapping.put("typeB", "/schemas/typeB");
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        childDiscriminator.set("mapping", childMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", childDiscriminator);
        SimpleJsonSchema childSchema = new SimpleJsonSchema("/schemas/child", schemaNode);

        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                MAPPER.createObjectNode(),
                childSchema,
                "at ");

        // the root mapping should now contain both entries
        ObjectNode merged = (ObjectNode) rootDiscriminator.get("mapping");
        assertEquals("/schemas/typeA", merged.get("typeA").asText());
        assertEquals("/schemas/typeB", merged.get("typeB").asText());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorThrowsOnMappingRedefinition() {
        // root mapping contains typeA -> /schemas/typeA
        ObjectNode rootMapping = MAPPER.createObjectNode();
        rootMapping.put("typeA", "/schemas/typeA");
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        rootDiscriminator.set("mapping", rootMapping);
        when(discriminatorContext.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);
        when(discriminatorContext.getDiscriminatorForPath("/schemas/child")).thenReturn(null);

        // child attempts to redefine typeA to a different path
        ObjectNode childMapping = MAPPER.createObjectNode();
        childMapping.put("typeA", "/schemas/other");
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        childDiscriminator.set("mapping", childMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", childDiscriminator);
        SimpleJsonSchema childSchema = new SimpleJsonSchema("/schemas/child", schemaNode);

        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext,
                MAPPER.createObjectNode(),
                childSchema,
                "at ");
    }
}
