package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

/**
 * Comprehensive JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Utility methods (equals, debug, atPath, atRoot).</li>
 *   <li>Discriminator handling (checkDiscriminatorMatch, implicit/explicit mapping, overrides).</li>
 *   <li>Schema node fetching (fetchSubSchemaNode with/without suppression).</li>
 *   <li>Helper methods (getNodeFieldType, preloadJsonSchemas, getPathType).</li>
 * </ul>
 *
 * <p>Because {@code BaseJsonValidator} is abstract, a minimal concrete subclass
 * {@code DummyValidator} is defined inside the test class.  The subclass only
 * implements the {@code validate} method required by {@link JsonValidator}.
 *
 * <p>All interactions with external collaborators (e.g. {@link ValidationContext},
 * {@link JsonSchema}, {@link Logger}) are performed through Mockito mocks,
 * allowing us to verify side‑effects and to simulate error conditions.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Helper concrete implementation of the abstract BaseJsonValidator
    // -----------------------------------------------------------------------
    private static class DummyValidator extends BaseJsonValidator {

        DummyValidator(String schemaPath,
                       JsonNode schemaNode,
                       JsonSchema parentSchema,
                       ValidatorTypeCode validatorType,
                       ValidationContext validationContext,
                       boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext,
                  suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No real validation – return an empty set.
            return Collections.emptySet();
        }
    }

    // -----------------------------------------------------------------------
    // Common test fixtures
    // -----------------------------------------------------------------------
    @Mock private ValidationContext mockContext;
    @Mock private ValidationContext.Config mockConfig;
    @Mock private JsonSchemaFactory mockFactory;
    @Mock private URIFactory mockUriFactory;
    @Mock private JsonSchema mockSubSchema;
    @Mock private JsonSchema mockParentSchema;
    @Mock private Logger mockLogger;
    @Mock private ValidationContext.DiscriminatorContext mockDiscriminatorContext;
    @Mock private ExecutionContext mockExecutionContext;

    private ObjectNode schemaNode;
    private DummyValidator validator;

    @Before
    public void setUp() throws Exception {
        // Basic schema node used by many tests
        schemaNode = MAPPER.createObjectNode();
        schemaNode.put("type", "string");

        // Default configuration needed by the BaseJsonValidator constructor
        when(mockContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.isFailFast()).thenReturn(false);
        when(mockConfig.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(mockConfig.getPathType()).thenReturn(PathType.DEFAULT);

        // Build a validator that does NOT suppress sub‑schema retrieval (default case)
        validator = new DummyValidator("#/properties/name",
                schemaNode,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockContext,
                false);
    }

    // -----------------------------------------------------------------------
    // 1. equals(double,double) – tolerance handling
    // -----------------------------------------------------------------------
    @Test
    public void testEqualsWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.000000000002));
        assertTrue(BaseJsonValidator.equals(-0.0, 0.0));
    }

    @Test
    public void testEqualsOutsideTolerance() {
        assertFalse(BaseJsonValidator.equals(1.0, 1.000001));
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN)); // NaN is never equal
    }

    // -----------------------------------------------------------------------
    // 2. debug(Logger,JsonNode,JsonNode,String) – logger interaction
    // -----------------------------------------------------------------------
    @Test
    public void testDebugLogsCorrectMessage() {
        JsonNode node = MAPPER.createObjectNode().put("field", "value");
        JsonNode root = MAPPER.createObjectNode().put("root", true);
        String at = "$.data";

        BaseJsonValidator.debug(mockLogger, node, root, at);

        verify(mockLogger).debug(eq("validate( {}, {}, {})"),
                eq(node), eq(root), eq(at));
    }

    // -----------------------------------------------------------------------
    // 3. fetchSubSchemaNode – suppressed vs. normal retrieval
    // -----------------------------------------------------------------------
    @Test
    public void testFetchSubSchemaNodeSuppressedReturnsNull() {
        DummyValidator suppressedValidator = new DummyValidator("#/s",
                schemaNode,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockContext,
                true); // suppress = true

        assertNull(suppressedValidator.fetchSubSchemaNode(mockContext));
    }

    @Test
    public void testFetchSubSchemaNodeObtainsSchemaWhenIdPresent() throws Exception {
        // schemaNode with an "id" that can be resolved
        ObjectNode nodeWithId = MAPPER.createObjectNode();
        nodeWithId.put("id", "http://example.com/schema.json");

        // Mock the URI creation and factory lookup
        URI fakeUri = new URI("http://example.com/schema.json");
        when(mockContext.getURIFactory()).thenReturn(mockUriFactory);
        when(mockUriFactory.create("http://example.com/schema.json")).thenReturn(fakeUri);
        when(mockContext.getJsonSchemaFactory()).thenReturn(mockFactory);
        when(mockFactory.getSchema(fakeUri, mockConfig)).thenReturn(mockSubSchema);

        DummyValidator v = new DummyValidator("#/s",
                nodeWithId,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockContext,
                false);

        JsonSchema result = v.fetchSubSchemaNode(mockContext);
        assertSame(mockSubSchema, result);
    }

    @Test
    public void testFetchSubSchemaNodeReturnsNullIfIdMissingOrInvalid() {
        // No "id" field – should return null
        DummyValidator v = new DummyValidator("#/s",
                MAPPER.createObjectNode(),
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockContext,
                false);
        assertNull(v.fetchSubSchemaNode(mockContext));

        // Invalid URI – simulate IllegalArgumentException from URIFactory
        ObjectNode nodeBadId = MAPPER.createObjectNode();
        nodeBadId.put("id", "::::");
        when(mockContext.getURIFactory()).thenReturn(mockUriFactory);
        when(mockUriFactory.create("::::")).thenThrow(new IllegalArgumentException());

        DummyValidator vBad = new DummyValidator("#/s",
                nodeBadId,
                mockParentSchema,
                ValidatorTypeCode.TYPE,
                mockContext,
                false);
        assertNull(vBad.fetchSubSchemaNode(mockContext));
    }

    // -----------------------------------------------------------------------
    // 4. getNodeFieldType – pulls "type" from parent schema node
    // -----------------------------------------------------------------------
    @Test
    public void testGetNodeFieldTypeFound() {
        // parent schema node contains a "type" field
        ObjectNode parentNode = MAPPER.createObjectNode().put("type", "integer");
        when(mockParentSchema.getSchemaNode()).thenReturn(parentNode);

        assertEquals("integer", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeMissing() {
        // parent schema node does NOT contain a "type" field
        when(mockParentSchema.getSchemaNode()).thenReturn(MAPPER.createObjectNode());
        assertNull(validator.getNodeFieldType());
    }

    // -----------------------------------------------------------------------
    // 5. preloadJsonSchemas – ensure initializeValidators() is invoked
    // -----------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchemasInvokesInitialize() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        Set<JsonSchema> set = new HashSet<>();
        set.add(s1);
        set.add(s2);

        validator.preloadJsonSchemas(set);

        verify(s1).initializeValidators();
        verify(s2).initializeValidators();
    }

    // -----------------------------------------------------------------------
    // 6. Path handling – atRoot, atPath(String), atPath(int)
    // -----------------------------------------------------------------------
    @Test
    public void testAtRootUsesDefaultPathType() {
        // PathType.DEFAULT's root is "/"
        assertEquals("/", validator.atRoot());
    }

    @Test
    public void testAtPathWithStringToken() {
        String result = validator.atPath("/", "name");
        assertEquals("/name", result);
    }

    @Test
    public void testAtPathWithIntIndex() {
        String result = validator.atPath("/", 0);
        assertEquals("/0", result);
    }

    // -----------------------------------------------------------------------
    // 7. checkDiscriminatorMatch – various branches
    // -----------------------------------------------------------------------
    @Test
    public void testCheckDiscriminatorMatchNullPropertyValueMarksMatch() {
        // When discriminator property value is null, it should mark a match immediately.
        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                MAPPER.createObjectNode(),
                null,
                mockSubSchema);
        verify(mockDiscriminatorContext).markMatch();
        verifyNoMoreInteractions(mockDiscriminatorContext);
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingMatch() {
        // Build discriminator with explicit mapping {"cat": "#/definitions/Cat"}
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("cat", "#/definitions/Cat");
        discriminator.set("mapping", mapping);

        // Schema whose path matches the mapping value
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("#/definitions/Cat");

        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator,
                "cat",
                schema);

        verify(mockDiscriminatorContext).markMatch();
        verifyNoMoreInteractions(mockDiscriminatorContext);
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingNoMatchFallsBackToImplicit() {
        // Mapping does NOT contain the discriminator value; implicit match should be tried.
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("dog", "#/definitions/Dog");
        discriminator.set("mapping", mapping);

        // Schema path ends with the discriminator property value -> implicit match
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("#/definitions/cat"); // ends with "cat"

        // Mock the "no explicit key override" check to return true
        // (the private static method is exercised via the public method; no extra mocking needed)

        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator,
                "cat",
                schema);

        // Expect markMatch because of implicit match
        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingNoMatchAndNoImplicit() {
        // Mapping present, but neither explicit nor implicit matches.
        ObjectNode discriminator = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("dog", "#/definitions/Dog");
        discriminator.set("mapping", mapping);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("#/definitions/Cat"); // does NOT end with "cat"

        BaseJsonValidator.checkDiscriminatorMatch(mockDiscriminatorContext,
                discriminator,
                "cat",
                schema);

        // No match should be recorded
        verify(mockDiscriminatorContext, never()).markMatch();
    }

    // -----------------------------------------------------------------------
    // 8. noExplicitDiscriminatorKeyOverride – true/false scenarios
    // -----------------------------------------------------------------------
    @Test
    public void testNoExplicitDiscriminatorKeyOverrideReturnsTrueWhenNoOverride() {
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("cat", "#/definitions/Cat");
        mapping.put("dog", "#/definitions/Dog");

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.schemaPath).thenReturn("#/definitions/Animal");

        boolean result = invokeNoExplicitDiscriminatorKeyOverride(mapping, parent);
        assertTrue(result);
    }

    @Test
    public void testNoExplicitDiscriminatorKeyOverrideReturnsFalseWhenOverride() {
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("cat", "#/definitions/Animal"); // points to parent schema path

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.schemaPath).thenReturn("#/definitions/Animal");

        boolean result = invokeNoExplicitDiscriminatorKeyOverride(mapping, parent);
        assertFalse(result);
    }

    // Helper to call the private static method via reflection
    private boolean invokeNoExplicitDiscriminatorKeyOverride(JsonNode mapping, JsonSchema parent) {
        try {
            java.lang.reflect.Method m = BaseJsonValidator.class
                    .getDeclaredMethod("noExplicitDiscriminatorKeyOverride", JsonNode.class, JsonSchema.class);
            m.setAccessible(true);
            return (boolean) m.invoke(null, mapping, parent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -----------------------------------------------------------------------
    // 9. registerAndMergeDiscriminator – merging logic and error paths
    // -----------------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorThrowsWhenPropertyNameRedefined() {
        // Set up a parent schema that already has a discriminator with a propertyName
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        rootDiscriminator.put("propertyName", "type");
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.schemaNode).thenReturn(rootDiscriminator);
        when(rootSchema.schemaPath).thenReturn("#/root");

        // Context returns a previous discriminator for the same path
        when(mockDiscriminatorContext.getDiscriminatorForPath(anyString()))
                .thenReturn(rootDiscriminator);

        // Current schema also defines a discriminator with a propertyName (illegal)
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        childDiscriminator.put("propertyName", "category");
        JsonSchema childSchema = mock(JsonSchema.class);
        when(childSchema.schemaNode).thenReturn(childDiscriminator);
        when(childSchema.schemaPath).thenReturn("#/root/child");

        // Attempt to register – should throw JsonSchemaException
        BaseJsonValidator.registerAndMergeDiscriminator(mockDiscriminatorContext,
                rootDiscriminator,
                childSchema,
                "at ");
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesMappings() {
        // Root discriminator with mapping {"cat":"#/defs/Cat"}
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode rootMapping = MAPPER.createObjectNode();
        rootMapping.put("cat", "#/defs/Cat");
        rootDiscriminator.set("mapping", rootMapping);
        when(mockDiscriminatorContext.getDiscriminatorForPath("#/root")).thenReturn(rootDiscriminator);

        // Child discriminator with additional mapping {"dog":"#/defs/Dog"}
        ObjectNode childDiscriminator = MAPPER.createObjectNode();
        ObjectNode childMapping = MAPPER.createObjectNode();
        childMapping.put("dog", "#/defs/Dog");
        childDiscriminator.set("mapping", childMapping);
        JsonSchema childSchema = mock(JsonSchema.class);
        when(childSchema.schemaNode).thenReturn(childDiscriminator);
        when(childSchema.schemaPath).thenReturn("#/root/child");

        // Register – should merge without exception
        BaseJsonValidator.registerAndMergeDiscriminator(mockDiscriminatorContext,
                rootDiscriminator,
                childSchema,
                "at ");

        // Verify that the merged discriminator now contains both entries
        assertEquals("#/defs/Cat", rootMapping.get("cat").asText());
        assertEquals("#/defs/Dog", rootMapping.get("dog").asText());
        // Ensure the context registers the final discriminator
        verify(mockDiscriminatorContext).registerDiscriminator(eq(childSchema.schemaPath), eq(rootDiscriminator));
    }

    // -----------------------------------------------------------------------
    // 10. getSchemaPath / getSchemaNode / getParentSchema – simple getters
    // -----------------------------------------------------------------------
    @Test
    public void testGettersReturnConstructorValues() {
        assertEquals("#/properties/name", validator.getSchemaPath());
        assertSame(schemaNode, validator.getSchemaNode());
        assertSame(mockParentSchema, validator.getParentSchema());
    }
}
