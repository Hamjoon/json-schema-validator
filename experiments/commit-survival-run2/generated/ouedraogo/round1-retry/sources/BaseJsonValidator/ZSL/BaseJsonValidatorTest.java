package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.slf4j.Logger;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link BaseJsonValidator}.
 *
 * <p>Because {@code BaseJsonValidator} is abstract we provide a minimal concrete
 * implementation {@code TestValidator} that simply returns an empty {@link Set}
 * of {@link ValidationMessage}s.  All static and protected behaviours are exercised
 * via this concrete class and a set of lightweight test doubles for the
 * surrounding schema infrastructure.</p>
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Minimal concrete implementation used for testing. */
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
            // For the purpose of the tests we simply return an empty set.
            return Collections.emptySet();
        }
    }

    /** Simple {@link DiscriminatorContext} implementation that records matches. */
    private static class SimpleDiscriminatorContext implements ValidationContext.DiscriminatorContext {
        private boolean matchFound = false;
        private final Map<String, ObjectNode> registry = new HashMap<>();

        @Override
        public void markMatch() {
            this.matchFound = true;
        }

        @Override
        public boolean isDiscriminatorMatchFound() {
            return matchFound;
        }

        @Override
        public void registerDiscriminator(String schemaPath, ObjectNode discriminator) {
            registry.put(schemaPath, discriminator);
        }

        @Override
        public ObjectNode getDiscriminatorForPath(String schemaPath) {
            return registry.get(schemaPath);
        }
    }

    @Mock
    private Logger mockLogger;

    @Mock
    private ValidationContext mockValidationContext;

    @Mock
    private ValidationContext.Config mockConfig;

    @Mock
    private URIFactory mockUriFactory;

    @Mock
    private JsonSchemaFactory mockJsonSchemaFactory;

    @Mock
    private JsonSchema mockFetchedSchema;

    @Mock
    private JsonSchema mockParentSchema;

    @Before
    public void setUp() {
        // Default configuration returns null for optional values -> PathType.DEFAULT will be used.
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.getApplyDefaultsStrategy()).thenReturn(null);
        when(mockConfig.getPathType()).thenReturn(null);
        when(mockConfig.isFailFast()).thenReturn(false);
        when(mockConfig.getResourceBundle()).thenReturn(null);
        when(mockConfig.getApplyDefaultsStrategy()).thenReturn(null);
        // URIFactory and JsonSchemaFactory are required only for fetchSubSchemaNode tests.
        when(mockValidationContext.getURIFactory()).thenReturn(mockUriFactory);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockJsonSchemaFactory);
    }

    /* --------------------------------------------------------------------- */
    /*  Static utility methods                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsCloseNumbers() {
        assertTrue(BaseJsonValidator.equals(0.1 + 0.2, 0.3));
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.0));
        assertFalse(BaseJsonValidator.equals(1.0, 1.1));
    }

    @Test
    public void testDebugLogsCorrectly() {
        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        JsonNode root = MAPPER.createObjectNode().put("root", true);
        BaseJsonValidator.debug(mockLogger, node, root, "path");
        verify(mockLogger, times(1)).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq("path"));
    }

    /* --------------------------------------------------------------------- */
    /*  fetchSubSchemaNode                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNode_SuppressedReturnsNull() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("id", "http://example.com/schema");
        TestValidator validator = new TestValidator("#", schemaNode, mockParentSchema,
                null, mockValidationContext, true);
        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void testFetchSubSchemaNode_NoIdReturnsNull() {
        JsonNode schemaNode = MAPPER.createObjectNode(); // no "id"
        TestValidator validator = new TestValidator("#", schemaNode, mockParentSchema,
                null, mockValidationContext, false);
        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void testFetchSubSchemaNode_InvalidUriReturnsNull() throws Exception {
        JsonNode schemaNode = MAPPER.createObjectNode().put("id", ":// bad uri");
        when(mockUriFactory.create(":// bad uri")).thenThrow(new IllegalArgumentException());
        TestValidator validator = new TestValidator("#", schemaNode, mockParentSchema,
                null, mockValidationContext, false);
        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void testFetchSubSchemaNode_ValidIdReturnsSchema() throws Exception {
        JsonNode schemaNode = MAPPER.createObjectNode().put("id", "http://example.com/schema");
        URI uri = new URI("http://example.com/schema");
        when(mockUriFactory.create("http://example.com/schema")).thenReturn(uri);
        when(mockJsonSchemaFactory.getSchema(uri, mockConfig)).thenReturn(mockFetchedSchema);

        TestValidator validator = new TestValidator("#", schemaNode, mockParentSchema,
                null, mockValidationContext, false);
        JsonSchema fetched = validator.fetchSubSchemaNode(mockValidationContext);
        assertSame(mockFetchedSchema, fetched);
    }

    /* --------------------------------------------------------------------- */
    /*  getNodeFieldType                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetNodeFieldType_ReturnsParentType() {
        ObjectNode parentSchemaNode = MAPPER.createObjectNode().put("type", "object");
        when(mockParentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
        TestValidator validator = new TestValidator("#", MAPPER.createObjectNode(),
                mockParentSchema, null, mockValidationContext, false);
        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldType_NoTypeReturnsNull() {
        ObjectNode parentSchemaNode = MAPPER.createObjectNode(); // no type
        when(mockParentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
        TestValidator validator = new TestValidator("#", MAPPER.createObjectNode(),
                mockParentSchema, null, mockValidationContext, false);
        assertNull(validator.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- */
    /*  preloadJsonSchemas                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPreloadJsonSchemas_InitializesAllSchemas() {
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(schema1, schema2);
        TestValidator validator = new TestValidator("#", MAPPER.createObjectNode(),
                mockParentSchema, null, mockValidationContext, false);
        validator.preloadJsonSchemas(schemas);
        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /*  Path handling (atRoot, atPath)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPathMethodsUseDefaultPathType() {
        TestValidator validator = new TestValidator("#", MAPPER.createObjectNode(),
                mockParentSchema, null, mockValidationContext, false);
        // Default PathType is expected to be non‑null and to return a root string.
        String root = validator.atRoot();
        assertNotNull(root);
        // Simple concatenation checks – exact format depends on PathType implementation.
        String child = validator.atPath(root, "child");
        assertTrue(child.contains("child"));
        String indexed = validator.atPath(root, 3);
        assertTrue(indexed.contains("[3]") || indexed.contains("3"));
    }

    /* --------------------------------------------------------------------- */
    /*  Discriminator handling                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatch_NullPropertyMarksMatch() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        JsonSchema anySchema = mock(JsonSchema.class);
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, anySchema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_ImplicitMappingMatch() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode(); // no mapping
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/defs/TypeA");
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "TypeA", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitMappingMatch() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("TypeB", "/defs/TypeB");
        discriminator.set("mapping", mapping);
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/defs/TypeB");
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "TypeB", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_NoExplicitMatchFallsBackToImplicit() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("Other", "/defs/Other");
        discriminator.set("mapping", mapping);
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/defs/Implicit");
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "Implicit", schema);
        // No explicit match, but implicit path matches, so match should be true.
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_NoMatchWhenExplicitOverridesImplicit() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        // Mapping that points to a *different* schema path than the implicit one.
        mapping.put("Implicit", "/defs/OtherSchema");
        discriminator.set("mapping", mapping);
        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.schemaPath).thenReturn("/defs/Parent");
        // The parent schema path is NOT present in the mapping, so noOverride = true.
        JsonSchema targetSchema = mock(JsonSchema.class);
        when(targetSchema.schemaPath).thenReturn("/defs/Implicit");
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "Implicit", targetSchema);
        // Since explicit mapping does NOT match, and no override, implicit match is performed.
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testRegisterAndMergeDiscriminator_MergesMappings() {
        // Setup root discriminator context with an existing mapping.
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode rootMapping = MAPPER.createObjectNode();
        rootMapping.put("A", "/defs/A");
        rootDiscriminator.set("mapping", rootMapping);
        ctx.registerDiscriminator("/root", rootDiscriminator);

        // Schema that adds a new mapping entry "B".
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        ObjectNode childMapping = MAPPER.createObjectNode();
        childMapping.put("B", "/defs/B");
        childDiscriminator.set("mapping", childMapping);
        JsonSchema childSchema = mock(JsonSchema.class);
        when(childSchema.schemaPath).thenReturn("/root/child");
        when(childSchema.schemaNode).thenReturn(childDiscriminator);

        // Invoke registerAndMergeDiscriminator – it should merge "B" into the root mapping.
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema, "at ");
        ObjectNode merged = (ObjectNode) ctx.getDiscriminatorForPath("/root").get("mapping");
        assertNotNull(merged.get("A"));
        assertNotNull(merged.get("B"));
        assertEquals("/defs/B", merged.get("B").asText());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminator_ConflictingMappingThrows() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode rootMapping = MAPPER.createObjectNode();
        rootMapping.put("X", "/defs/Old");
        rootDiscriminator.set("mapping", rootMapping);
        ctx.registerDiscriminator("/root", rootDiscriminator);

        // Child schema tries to redefine the same key "X" with a different value.
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        ObjectNode childMapping = MAPPER.createObjectNode();
        childMapping.put("X", "/defs/New");
        childDiscriminator.set("mapping", childMapping);
        JsonSchema childSchema = mock(JsonSchema.class);
        when(childSchema.schemaPath).thenReturn("/root/child");
        when(childSchema.schemaNode).thenReturn(childDiscriminator);

        // Expect JsonSchemaException due to mapping redefinition.
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema, "at ");
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods (none needed)                                         */
    /* --------------------------------------------------------------------- */

}
