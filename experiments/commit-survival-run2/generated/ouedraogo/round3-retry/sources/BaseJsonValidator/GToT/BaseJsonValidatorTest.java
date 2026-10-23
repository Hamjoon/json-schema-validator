/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All public accessor methods.</li>
 *   <li>Protected helper methods (exposed through a concrete subclass).</li>
 *   <li>Static utility methods {@code equals} and {@code debug}.</li>
 *   <li>Edge‑case handling of discriminator logic.</li>
 *   <li>Behaviour of {@code fetchSubSchemaNode} with and without suppression.</li>
 *   <li>Path handling utilities.</li>
 * </ul>
 *
 * <p>External library classes are mocked with Mockito so the tests stay fast and focused on the
 * logic inside {@code BaseJsonValidator}.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

/**
 * Concrete subclass used only for testing the protected members of {@code BaseJsonValidator}.
 */
class TestValidator extends BaseJsonValidator {

    TestValidator(String schemaPath,
                  JsonNode schemaNode,
                  JsonSchema parentSchema,
                  ValidatorTypeCode validatorType,
                  ValidationContext validationContext,
                  boolean suppressSubSchemaRetrieval) {
        super(schemaPath, schemaNode, parentSchema, validatorType,
                validationContext, suppressSubSchemaRetrieval);
    }

    /** Simple implementation that returns an empty set – the real validation logic is not needed. */
    @Override
    protected Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
        return Collections.emptySet();
    }

    /* Expose protected methods for the test suite */
    @Override
    public JsonSchema fetchSubSchemaNode(ValidationContext validationContext) {
        return super.fetchSubSchemaNode(validationContext);
    }

    @Override
    public String getNodeFieldType() {
        return super.getNodeFieldType();
    }

    @Override
    public void preloadJsonSchemas(Collection<JsonSchema> schemas) {
        super.preloadJsonSchemas(schemas);
    }

    @Override
    public PathType getPathType() {
        return super.getPathType();
    }

    @Override
    public String atRoot() {
        return super.atRoot();
    }

    @Override
    public String atPath(String currentPath, String token) {
        return super.atPath(currentPath, token);
    }

    @Override
    public String atPath(String currentPath, int index) {
        return super.atPath(currentPath, index);
    }
}

/**
 * Test class.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    /* --------------------------------------------------------------------- */
    /*  Mocks for the external dependencies                                   */
    /* --------------------------------------------------------------------- */
    @Mock private ValidationContext mockValidationContext;
    @Mock private ValidationContext.Config mockConfig;
    @Mock private ValidationContext.URIFactory mockUriFactory;
    @Mock private ValidationContext.JsonSchemaFactory mockJsonSchemaFactory;
    @Mock private JsonSchema mockParentSchema;
    @Mock private JsonSchema mockChildSchema;
    @Mock private ExecutionContext mockExecutionContext;
    @Mock private Logger mockLogger;
    @Mock private ValidationContext.DiscriminatorContext mockDiscriminatorContext;
    @Mock private PathType mockPathType;

    /* --------------------------------------------------------------------- */
    /*  Test data                                                             */
    /* --------------------------------------------------------------------- */
    private static final String SCHEMA_PATH = "/mySchema";
    private static final String PARENT_SCHEMA_PATH = "/parent";
    private static final String CHILD_SCHEMA_PATH = "/parent/child";

    private ObjectNode schemaNode;
    private ObjectNode parentSchemaNode;
    private ObjectNode childSchemaNode;

    @Before
    public void setUp() {
        // ---- basic schema nodes -------------------------------------------------
        schemaNode = JsonNodeFactory.instance.objectNode();
        parentSchemaNode = JsonNodeFactory.instance.objectNode();
        childSchemaNode = JsonNodeFactory.instance.objectNode();

        // ---- parent schema mock -------------------------------------------------
        when(mockParentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
        when(mockParentSchema.getSchemaPath()).thenReturn(PARENT_SCHEMA_PATH);
        // child schema (used for discriminator tests)
        when(mockChildSchema.getSchemaNode()).thenReturn(childSchemaNode);
        when(mockChildSchema.getSchemaPath()).thenReturn(CHILD_SCHEMA_PATH);

        // ---- validation context mock configuration -------------------------------
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.isFailFast()).thenReturn(false);
        when(mockConfig.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(mockConfig.getPathType()).thenReturn(mockPathType);
        when(mockValidationContext.getURIFactory()).thenReturn(mockUriFactory);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockJsonSchemaFactory);
        when(mockPathType.getRoot()).thenReturn("$");
        when(mockPathType.append(anyString(), anyString())).thenAnswer(invocation ->
                invocation.getArgument(0) + "/" + invocation.getArgument(1));
        when(mockPathType.append(anyString(), anyInt())).thenAnswer(invocation ->
                invocation.getArgument(0) + "[" + invocation.getArgument(1) + "]");
    }

    /* --------------------------------------------------------------------- */
    /*  1. Public accessor methods                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaPath() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, false);
        assertEquals(SCHEMA_PATH, validator.getSchemaPath());
    }

    @Test
    public void testGetSchemaNode() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, false);
        assertSame(schemaNode, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, false);
        assertSame(mockParentSchema, validator.getParentSchema());
    }

    /* --------------------------------------------------------------------- */
    /*  2. fetchSubSchemaNode – suppression & normal flow                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, true);
        assertNull("When suppression flag is true, fetchSubSchemaNode must return null",
                validator.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void testFetchSubSchemaNodeNoId() {
        // schemaNode has no "id" field – obtainSubSchemaNode should return null
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, false);
        assertNull("No 'id' attribute => obtainSubSchemaNode returns null",
                validator.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void testFetchSubSchemaNodeValidId() throws Exception {
        // add a valid id that can be turned into a URI
        schemaNode.put("id", "http://example.com/schema");
        URI fakeUri = new URI("http://example.com/schema");
        JsonSchema fakeSchema = mock(JsonSchema.class);
        when(mockUriFactory.create("http://example.com/schema")).thenReturn(fakeUri);
        when(mockJsonSchemaFactory.getSchema(fakeUri, mockConfig)).thenReturn(fakeSchema);

        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, false);
        assertSame("Should retrieve the schema returned by the factory",
                fakeSchema, validator.fetchSubSchemaNode(mockValidationContext));
    }

    /* --------------------------------------------------------------------- */
    /*  3. Static utility methods                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsCloseNumbers() {
        assertTrue("Numbers that differ less than 1e-12 must be considered equal",
                BaseJsonValidator.equals(0.1 + 0.2, 0.3));
    }

    @Test
    public void testEqualsNotCloseNumbers() {
        assertFalse("Numbers that differ more than 1e-12 must not be considered equal",
                BaseJsonValidator.equals(1.0, 1.000000000001));
    }

    @Test
    public void testDebugLogs() {
        JsonNode node = JsonNodeFactory.instance.textNode("value");
        JsonNode root = JsonNodeFactory.instance.objectNode();
        BaseJsonValidator.debug(mockLogger, node, root, "path");
        verify(mockLogger).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq("path"));
    }

    /* --------------------------------------------------------------------- */
    /*  4. getNodeFieldType – present & absent                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetNodeFieldTypePresent() {
        parentSchemaNode.put("type", "string");
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, false);
        assertEquals("string", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeAbsent() {
        // parent schema node does NOT contain a "type" field
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, false);
        assertNull("When no type field is present, null must be returned",
                validator.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- */
    /*  5. preloadJsonSchemas – ensure initializeValidators is invoked    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPreloadJsonSchemasInvokesInitialize() {
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        List<JsonSchema> schemas = Arrays.asList(schemaA, schemaB);

        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, false);
        validator.preloadJsonSchemas(schemas);

        verify(schemaA).initializeValidators();
        verify(schemaB).initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /*  6. Path handling (atRoot, atPath)                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAtRoot() {
        when(mockPathType.getRoot()).thenReturn("$");
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, false);
        assertEquals("$", validator.atRoot());
    }

    @Test
    public void testAtPathWithToken() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, false);
        String result = validator.atPath("$.properties", "name");
        assertEquals("$.properties/name", result);
    }

    @Test
    public void testAtPathWithIndex() {
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, false);
        String result = validator.atPath("$.items", 2);
        assertEquals("$.items[2]", result);
    }

    /* --------------------------------------------------------------------- */
    /*  7. Discriminator handling – static protected methods                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatchNullValueMarksMatch() {
        // discriminatorPropertyValue == null => immediate match
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                null, null, mockChildSchema);
        verify(mockDiscriminatorContext).markMatch();
        verifyNoMoreInteractions(mockDiscriminatorContext);
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingMatch() {
        // Build discriminator with explicit mapping { "dog": "/parent/child" }
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.put("dog", CHILD_SCHEMA_PATH);
        discriminator.set("mapping", mapping);

        // Context initially reports no match
        when(mockDiscriminatorContext.isDiscriminatorMatchFound()).thenReturn(false);
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator, "dog", mockChildSchema);

        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingNoMatchFallsBackToImplicit() {
        // Explicit mapping does NOT contain the value, but implicit match should work
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.put("cat", "/some/other/path");
        discriminator.set("mapping", mapping);

        // No explicit match; also no override (no entry maps to parent schema path)
        when(mockDiscriminatorContext.isDiscriminatorMatchFound()).thenReturn(false);
        // parent schema path = "/parent"
        when(mockDiscriminatorContext.isDiscriminatorMatchFound()).thenReturn(false);
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator, "child", mockChildSchema);

        // Implicit match: schemaPath ends with "/child"
        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitOnly() {
        // No mapping at all – should rely on implicit rule
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();

        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator, "child", mockChildSchema);

        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testRegisterAndMergeDiscriminatorAddsNewMapping() {
        // Context that already has a root discriminator with an empty mapping
        ObjectNode rootDiscriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode rootMapping = JsonNodeFactory.instance.objectNode();
        rootDiscriminator.set("mapping", rootMapping);
        when(mockDiscriminatorContext.getDiscriminatorForPath(PARENT_SCHEMA_PATH)).thenReturn(rootDiscriminator);
        when(mockDiscriminatorContext.getDiscriminatorForPath(CHILD_SCHEMA_PATH)).thenReturn(null);

        // Child schema defines its own mapping { "dog": "/parent/child" }
        ObjectNode childDiscriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode childMapping = JsonNodeFactory.instance.objectNode();
        childMapping.put("dog", CHILD_SCHEMA_PATH);
        childDiscriminator.set("mapping", childMapping);

        // The method should merge the child's mapping into the root mapping
        BaseJsonValidator.registerAndMergeDiscriminator(mockDiscriminatorContext,
                rootDiscriminator, mockChildSchema, "testAt:");

        // Verify that the key "dog" has been added to the root mapping
        assertEquals(CHILD_SCHEMA_PATH, rootMapping.get("dog").asText());

        // Verify that the discriminator was registered for the child schema path
        verify(mockDiscriminatorContext).registerDiscriminator(CHILD_SCHEMA_PATH, rootDiscriminator);
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorDetectsPropertyNameRedefinition() {
        // Parent schema already defines a discriminator with a propertyName
        ObjectNode parentDiscriminator = JsonNodeFactory.instance.objectNode();
        parentDiscriminator.set("propertyName", JsonNodeFactory.instance.textNode("type"));
        when(mockDiscriminatorContext.getDiscriminatorForPath(PARENT_SCHEMA_PATH)).thenReturn(parentDiscriminator);
        when(mockDiscriminatorContext.getDiscriminatorForPath(CHILD_SCHEMA_PATH)).thenReturn(null);

        // Attempt to register a new discriminator on child – should throw
        BaseJsonValidator.registerAndMergeDiscriminator(mockDiscriminatorContext,
                JsonNodeFactory.instance.objectNode(), mockChildSchema, "testAt:");
    }

    /* --------------------------------------------------------------------- */
    /*  8. validate(ExecutionContext, JsonNode) – basic delegation test      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testValidateDelegatesToThreeArgumentVersion() {
        // The three‑argument version is overridden in TestValidator to return an empty set.
        TestValidator validator = new TestValidator(SCHEMA_PATH, schemaNode,
                mockParentSchema, null, mockValidationContext, false);
        Set<ValidationMessage> result = validator.validate(mockExecutionContext, JsonNodeFactory.instance.objectNode());
        assertTrue("The overridden three‑argument method returns an empty set", result.isEmpty());
    }
}
