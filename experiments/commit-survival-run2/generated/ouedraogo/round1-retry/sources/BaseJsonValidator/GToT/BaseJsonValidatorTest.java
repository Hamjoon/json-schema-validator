package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
 * Comprehensive JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public getters.</li>
 *   <li>Behaviour of {@code fetchSubSchemaNode} under normal and edge conditions.</li>
 *   <li>Utility static methods {@code equals} and {@code debug}.</li>
 *   <li>Discriminator handling helpers.</li>
 *   <li>Path building helpers.</li>
 *   <li>Pre‑loading of schemas.</li>
 * </ul>
 *
 * A concrete subclass {@code TestValidator} is used to instantiate the abstract
 * {@code BaseJsonValidator}.  Simple stub implementations and Mockito mocks
 * replace the complex collaborators required by the original library.
 */
public class BaseJsonValidatorTest {

    /* ----------------------------------------------------------------------
     * Helper classes & stubs
     * ---------------------------------------------------------------------- */

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} used for
     * testing.  The overloaded {@code validate} method simply returns an empty
     * {@link Set} – the real validation logic lives in concrete validators in
     * the library and is out of scope for these unit tests.
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
            // No real validation – just return an empty set.
            return Collections.emptySet();
        }
    }

    /**
     * Very small implementation of {@link ValidationContext.DiscriminatorContext}
     * that records whether a match was marked and stores registered discriminators.
     */
    private static class SimpleDiscriminatorContext implements ValidationContext.DiscriminatorContext {

        private boolean matchMarked = false;
        private final Map<String, ObjectNode> registrations = new HashMap<>();

        @Override
        public void markMatch() {
            matchMarked = true;
        }

        @Override
        public boolean isDiscriminatorMatchFound() {
            return matchMarked;
        }

        @Override
        public void registerDiscriminator(String schemaPath, ObjectNode discriminator) {
            registrations.put(schemaPath, discriminator);
        }

        @Override
        public ObjectNode getDiscriminatorForPath(String schemaPath) {
            return registrations.get(schemaPath);
        }

        // The remaining methods are not needed for these tests.
        @Override public void reset() {}
        @Override public void setCurrentDiscriminator(ObjectNode discriminator) {}
        @Override public ObjectNode getCurrentDiscriminator() { return null; }
        @Override public String getCurrentDiscriminatorPath() { return null; }
    }

    /* ----------------------------------------------------------------------
     * Mocks & shared fixtures
     * ---------------------------------------------------------------------- */

    @Mock private JsonNode mockSchemaNode;
    @Mock private JsonSchema mockParentSchema;
    @Mock private ValidationContext mockValidationContext;
    @Mock private ValidationContext.Config mockConfig;
    @Mock private ValidationContext.URIFactory mockURIFactory;
    @Mock private JsonSchemaFactory mockJsonSchemaFactory;
    @Mock private Logger mockLogger;
    @Mock private ExecutionContext mockExecutionContext;

    @InjectMocks private TestValidator validator;

    private static final String SCHEMA_PATH = "/mySchema";
    private static final String SCHEMA_ID = "http://example.com/schema.json";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // Basic schema node with an "id" field
        when(mockSchemaNode.get("id")).thenReturn(new TextNode(SCHEMA_ID));
        when(mockSchemaNode.get("$schema")).thenReturn(null);
        when(mockSchemaNode.get("type")).thenReturn(new TextNode("object"));

        // ValidationContext configuration
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.isFailFast()).thenReturn(false);
        when(mockConfig.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(mockConfig.getPathType()).thenReturn(PathType.DEFAULT);

        // URI factory & JSON schema factory stubs
        when(mockValidationContext.getURIFactory()).thenReturn(mockURIFactory);
        when(mockURIFactory.create(SCHEMA_ID)).thenReturn(URI.create(SCHEMA_ID));
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockJsonSchemaFactory);
        when(mockJsonSchemaFactory.getSchema(any(URI.class), any())).thenReturn(mock(JsonSchema.class));

        // Build the concrete validator instance
        validator = new TestValidator(SCHEMA_PATH, mockSchemaNode,
                mockParentSchema, ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockValidationContext, false);
    }

    /* ----------------------------------------------------------------------
     * Public getter tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetSchemaPath() {
        assertEquals(SCHEMA_PATH, validator.getSchemaPath());
    }

    @Test
    public void testGetSchemaNode() {
        assertSame(mockSchemaNode, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema() {
        assertSame(mockParentSchema, validator.getParentSchema());
    }

    /* ----------------------------------------------------------------------
     * fetchSubSchemaNode tests (including edge cases)
     * ---------------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNode_NotSuppressed_IdPresent_ValidUri() {
        JsonSchema sub = validator.fetchSubSchemaNode(mockValidationContext);
        assertNotNull(sub);
        verify(mockJsonSchemaFactory).getSchema(eq(URI.create(SCHEMA_ID)), any());
    }

    @Test
    public void testFetchSubSchemaNode_SuppressedReturnsNull() {
        TestValidator suppressed = new TestValidator(SCHEMA_PATH, mockSchemaNode,
                mockParentSchema, ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockValidationContext, true);
        assertNull(suppressed.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void testFetchSubSchemaNode_NoIdFieldReturnsNull() {
        when(mockSchemaNode.get("id")).thenReturn(null);
        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void testFetchSubSchemaNode_IdNotUriReturnsNull() {
        when(mockURIFactory.create(SCHEMA_ID)).thenThrow(new IllegalArgumentException("bad uri"));
        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
    }

    /* ----------------------------------------------------------------------
     * Static utility method tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testEquals_Tolerance() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.0));
        assertFalse(BaseJsonValidator.equals(1.0, 1.1));
    }

    @Test
    public void testDebug_LogsCorrectMessage() {
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        BaseJsonValidator.debug(mockLogger, node, root, "path");
        verify(mockLogger).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq("path"));
    }

    /* ----------------------------------------------------------------------
     * Discriminator handling tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatch_NullPropertyValue_MarksMatch() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = mock(ObjectNode.class);
        JsonSchema schema = mock(JsonSchema.class);
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitMappingMatch() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();

        // Build a discriminator with an explicit mapping {"pet":"#/definitions/dog"}
        ObjectNode discriminator = mock(ObjectNode.class);
        ObjectNode mapping = mock(ObjectNode.class);
        when(discriminator.get("mapping")).thenReturn(mapping);
        when(mapping.fields()).thenReturn(
                Collections.singletonMap("pet", new TextNode("#/definitions/dog")).entrySet().iterator());

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("#/definitions/dog");

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "pet", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_ImplicitMappingMatch() {
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode discriminator = mock(ObjectNode.class);
        when(discriminator.get("mapping")).thenReturn(null);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/animals/cat");
        // The discriminator value matches the last token of the schema path.
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "cat", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testRegisterAndMergeDiscriminator_MergeWithoutConflict() {
        // Prepare a root discriminator without a mapping.
        ObjectNode rootDiscriminator = mock(ObjectNode.class);
        when(rootDiscriminator.get("mapping")).thenReturn(null);

        // Context already has the root discriminator registered for "/root".
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ctx.registerDiscriminator("/root", rootDiscriminator);

        // Child schema contains a discriminator with a mapping.
        ObjectNode childDiscriminator = mock(ObjectNode.class);
        ObjectNode childMapping = mock(ObjectNode.class);
        when(childDiscriminator.get("mapping")).thenReturn(childMapping);
        when(childMapping.fields()).thenReturn(
                Collections.singletonMap("type", new TextNode("#/typeA")).entrySet().iterator());

        JsonSchema childSchema = mock(JsonSchema.class);
        when(childSchema.schemaNode).thenReturn(mock(JsonNode.class));
        when(childSchema.schemaPath).thenReturn("/child");
        when(childSchema.schemaNode.get("discriminator")).thenReturn(childDiscriminator);

        // No conflict – merging should succeed without exception.
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, childSchema, "/child");
        // After merge the root should now contain the child's mapping.
        verify(rootDiscriminator).set(eq("mapping"), same(childDiscriminator));
    }

    /* ----------------------------------------------------------------------
     * Path handling tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testAtRoot_UsesDefaultPathType() {
        // PathType.DEFAULT's root is "$"
        assertEquals("$", validator.atRoot());
    }

    @Test
    public void testAtPath_StringToken() {
        String result = validator.atPath("$", "name");
        assertEquals("$.name", result);
    }

    @Test
    public void testAtPath_ArrayIndex() {
        String result = validator.atPath("$.items", 2);
        assertEquals("$.items[2]", result);
    }

    /* ----------------------------------------------------------------------
     * preloadJsonSchemas tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testPreloadJsonSchemas_InitializesAllSchemas() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        List<JsonSchema> list = Arrays.asList(s1, s2);

        validator.preloadJsonSchemas(list);
        verify(s1).initializeValidators();
        verify(s2).initializeValidators();
    }

    /* ----------------------------------------------------------------------
     * getNodeFieldType edge case
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetNodeFieldType_TypeMissingReturnsNull() {
        when(mockParentSchema.getSchemaNode()).thenReturn(mock(JsonNode.class));
        when(mockParentSchema.getSchemaNode().get("type")).thenReturn(null);
        assertNull(validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldType_TypePresent() {
        when(mockParentSchema.getSchemaNode()).thenReturn(mock(JsonNode.class));
        when(mockParentSchema.getSchemaNode().get("type")).thenReturn(new TextNode("string"));
        assertEquals("string", validator.getNodeFieldType());
    }
}
