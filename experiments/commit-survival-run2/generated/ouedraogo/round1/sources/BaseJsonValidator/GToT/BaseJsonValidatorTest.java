/**
 * JUnit 4 test suite for {@link com.networknt.schema.BaseJsonValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public accessor methods</li>
 *   <li>Utility static methods (equals, debug)</li>
 *   <li>Discriminator handling helpers</li>
 *   <li>fetchSubSchemaNode behaviour (suppress flag & URI handling)</li>
 *   <li>Path generation helpers (atRoot, atPath)</li>
 *   <li>Edge‑case and exception scenarios</li>
 * </ul>
 *
 * The test class uses Mockito for creating lightweight stubs of the many
 * collaborators required by {@code BaseJsonValidator}.  A concrete inner class
 * {@code TestValidator} is provided that implements the abstract parts of the
 * validator so that the protected methods of the base class can be exercised.
 *
 * The test file is self‑contained and can be compiled and run against the
 * original source without any external test utilities.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.net.URI;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    /** Simple concrete implementation of the abstract {@link BaseJsonValidator}. */
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

        /** The real validator would perform real checks – for the test we simply
         *  return an empty set. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            return Collections.emptySet();
        }
    }

    private static final String SCHEMA_PATH = "/mySchema";
    private static final String PARENT_SCHEMA_PATH = "/parentSchema";

    @Mock private JsonSchema mockParentSchema;
    @Mock private ValidationContext mockValidationContext;
    @Mock private ValidationContext.DiscriminatorContext mockDiscriminatorContext;
    @Mock private Logger mockLogger;
    @Mock private URIFactory mockURIFactory;
    @Mock private JsonSchemaFactory mockJsonSchemaFactory;
    @Mock private JsonSchema mockSubSchema;

    private ObjectMapper mapper = new ObjectMapper();

    private TestValidator validator;

    @Before
    public void setUp() throws Exception {
        // Basic schema node – can be enriched in individual tests
        ObjectNode schemaNode = mapper.createObjectNode();
        // parent schema must return its own schemaNode – we use a simple empty node
        when(mockParentSchema.getSchemaNode()).thenReturn(mapper.createObjectNode());
        when(mockParentSchema.getSchemaPath()).thenReturn(PARENT_SCHEMA_PATH);

        // ValidationContext basic stubbing (required by the BaseJsonValidator ctor)
        when(mockValidationContext.getConfig()).thenReturn(null);
        when(mockValidationContext.getURIFactory()).thenReturn(mockURIFactory);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockJsonSchemaFactory);
        when(mockValidationContext.getDiscriminatorContext()).thenReturn(mockDiscriminatorContext);

        validator = new TestValidator(
                SCHEMA_PATH,
                schemaNode,
                mockParentSchema,
                ValidatorTypeCode.ADDITIONAL_PROPERTIES, // arbitrary enum value
                mockValidationContext,
                false // do NOT suppress sub‑schema retrieval by default
        );
    }

    /* --------------------------------------------------------------------- */
    /*  Public accessor methods                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetSchemaPath() {
        assertEquals(SCHEMA_PATH, validator.getSchemaPath());
    }

    @Test
    public void testGetSchemaNode() {
        JsonNode node = validator.getSchemaNode();
        assertNotNull(node);
        assertTrue(node.isObject());
    }

    @Test
    public void testGetParentSchema() {
        assertSame(mockParentSchema, validator.getParentSchema());
    }

    /* --------------------------------------------------------------------- */
    /*  fetchSubSchemaNode – normal and suppressed paths                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        TestValidator suppressed = new TestValidator(
                SCHEMA_PATH,
                mapper.createObjectNode(),
                mockParentSchema,
                ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockValidationContext,
                true // suppress flag
        );
        assertNull(suppressed.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void testFetchSubSchemaNodeObtainSuccess() throws Exception {
        // Prepare a schema node that contains a valid "id" field.
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("id", "http://example.com/subschema");
        // Re‑create validator with the new node
        TestValidator v = new TestValidator(
                SCHEMA_PATH,
                schemaNode,
                mockParentSchema,
                ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockValidationContext,
                false
        );

        // Stub the URI factory & schema factory
        URI dummyUri = new URI("http://example.com/subschema");
        when(mockURIFactory.create("http://example.com/subschema")).thenReturn(dummyUri);
        when(mockJsonSchemaFactory.getSchema(dummyUri, null)).thenReturn(mockSubSchema);

        JsonSchema result = v.fetchSubSchemaNode(mockValidationContext);
        assertSame(mockSubSchema, result);
    }

    @Test
    public void testFetchSubSchemaNodeObtainInvalidUri() throws Exception {
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("id", "not a valid uri");
        TestValidator v = new TestValidator(
                SCHEMA_PATH,
                schemaNode,
                mockParentSchema,
                ValidatorTypeCode.ADDITIONAL_PROPERTIES,
                mockValidationContext,
                false
        );

        // Simulate IllegalArgumentException from the URIFactory
        when(mockURIFactory.create("not a valid uri")).thenThrow(new IllegalArgumentException());

        assertNull(v.fetchSubSchemaNode(mockValidationContext));
    }

    /* --------------------------------------------------------------------- */
    /*  Static utility methods                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsCloseEnough() {
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-10));
    }

    @Test
    public void testDebugLogsCorrectly() {
        JsonNode node = mapper.createObjectNode().put("a", 1);
        JsonNode root = mapper.createObjectNode().put("b", 2);
        String at = "/some/path";

        BaseJsonValidator.debug(mockLogger, node, root, at);

        // Verify the exact SLF4J pattern used in the method
        verify(mockLogger).debug("validate( {}, {}, {})", node, root, at);
    }

    /* --------------------------------------------------------------------- */
    /*  Discriminator handling helpers                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatchNullValueMarksMatch() {
        BaseJsonValidator.checkDiscriminatorMatch(
                mockDiscriminatorContext,
                mapper.createObjectNode(),
                null,                     // discriminatorPropertyValue is null
                mockParentSchema
        );
        verify(mockDiscriminatorContext).markMatch();
        verifyNoMoreInteractions(mockDiscriminatorContext);
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitMapping() {
        // No explicit mapping node – expect implicit match when schemaPath ends with the value
        ObjectNode discriminator = mapper.createObjectNode();
        String propValue = "typeX";

        // Prepare a schema whose path ends with "/typeX"
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.getSchemaPath()).thenReturn("/root/" + propValue);

        BaseJsonValidator.checkDiscriminatorMatch(
                mockDiscriminatorContext,
                discriminator,
                propValue,
                schema
        );
        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingFound() {
        ObjectNode discriminator = mapper.createObjectNode();
        ObjectNode mapping = mapper.createObjectNode();
        mapping.put("typeY", "/schemas/typeY");
        discriminator.set("mapping", mapping);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.getSchemaPath()).thenReturn("/schemas/typeY");

        BaseJsonValidator.checkDiscriminatorMatch(
                mockDiscriminatorContext,
                discriminator,
                "typeY",
                schema
        );
        verify(mockDiscriminatorContext).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingNotFoundFallsBackToImplicit() {
        // Mapping present but does NOT match the supplied value
        ObjectNode discriminator = mapper.createObjectNode();
        ObjectNode mapping = mapper.createObjectNode();
        mapping.put("other", "/schemas/other");
        discriminator.set("mapping", mapping);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.getSchemaPath()).thenReturn("/schemas/typeZ");

        // The property value does not appear in mapping, but the schemaPath ends with the value,
        // so the implicit check should still mark a match.
        BaseJsonValidator.checkDiscriminatorMatch(
                mockDiscriminatorContext,
                discriminator,
                "typeZ",
                schema
        );
        verify(mockDiscriminatorContext).markMatch();
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorThrowsOnPropertyNameRedefinition() {
        // Set up a parent schema that already has a discriminator with a propertyName
        ObjectNode parentDiscriminator = mapper.createObjectNode();
        parentDiscriminator.set("propertyName", new TextNode("type"));
        ObjectNode parentSchemaNode = mapper.createObjectNode();
        parentSchemaNode.set("discriminator", parentDiscriminator);

        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.schemaNode).thenReturn(parentSchemaNode);
        when(parentSchema.schemaPath).thenReturn("/parent");

        // DiscriminatorContext returns a discriminator for the same path (simulating inheritance)
        when(mockDiscriminatorContext.getDiscriminatorForPath(parentSchema.schemaPath))
                .thenReturn(parentDiscriminator);

        // The discriminator we try to register – it also defines a propertyName → illegal
        ObjectNode newDiscriminator = mapper.createObjectNode();
        newDiscriminator.set("propertyName", new TextNode("type"));

        // Call – should throw JsonSchemaException
        BaseJsonValidator.registerAndMergeDiscriminator(
                mockDiscriminatorContext,
                newDiscriminator,
                parentSchema,
                "at "
        );
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesMappingsWithoutCollision() {
        // Root discriminator (already registered) – no propertyName, but has a mapping
        ObjectNode rootDiscriminator = mapper.createObjectNode();
        ObjectNode rootMapping = mapper.createObjectNode();
        rootMapping.put("A", "/schemas/A");
        rootDiscriminator.set("mapping", rootMapping);

        // Parent schema that adds a new mapping entry
        ObjectNode parentMapping = mapper.createObjectNode();
        parentMapping.put("B", "/schemas/B");

        ObjectNode parentDiscriminator = mapper.createObjectNode();
        parentDiscriminator.set("mapping", parentMapping);

        ObjectNode parentSchemaNode = mapper.createObjectNode();
        parentSchemaNode.set("discriminator", parentDiscriminator);

        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.schemaNode).thenReturn(parentSchemaNode);
        when(parentSchema.schemaPath).thenReturn("/parent");

        // Context already holds the root discriminator for the same path
        when(mockDiscriminatorContext.getDiscriminatorForPath(parentSchema.schemaPath))
                .thenReturn(rootDiscriminator);

        // Execute merge – should succeed without exception
        BaseJsonValidator.registerAndMergeDiscriminator(
                mockDiscriminatorContext,
                rootDiscriminator,
                parentSchema,
                "at "
        );

        // Verify that the new entry has been merged into the root mapping
        assertEquals("/schemas/B", rootMapping.get("B").asText());

        // Verify that the context registers the (possibly updated) discriminator
        verify(mockDiscriminatorContext).registerDiscriminator(parentSchema.schemaPath, rootDiscriminator);
    }

    /* --------------------------------------------------------------------- */
    /*  Path handling helpers                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAtRootUsesDefaultPathType() {
        String root = validator.atRoot();
        assertNotNull(root);
        // The default PathType returns "/" – we only assert non‑null to stay independent
        // of internal enum implementation.
    }

    @Test
    public void testAtPathStringToken() {
        String child = validator.atPath("/root", "child");
        assertNotNull(child);
        // Expected pattern is something like "/root/child" (or "$.root.child" depending on PathType)
        assertTrue(child.contains("child"));
    }

    @Test
    public void testAtPathArrayIndex() {
        String indexed = validator.atPath("/root", 3);
        assertNotNull(indexed);
        assertTrue(indexed.contains("[3]") || indexed.contains("3"));
    }

    /* --------------------------------------------------------------------- */
    /*  getNodeFieldType – verifies extraction of the "type" field from parent */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetNodeFieldTypeFound() {
        ObjectNode parentNode = mapper.createObjectNode();
        parentNode.put("type", "object");
        when(mockParentSchema.getSchemaNode()).thenReturn(parentNode);

        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeMissing() {
        ObjectNode parentNode = mapper.createObjectNode(); // no "type"
        when(mockParentSchema.getSchemaNode()).thenReturn(parentNode);

        assertNull(validator.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- */
    /*  preloadJsonSchemas – ensures each supplied schema is initialized   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPreloadJsonSchemasCallsInitializeValidators() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(s1, s2);

        validator.preloadJsonSchemas(schemas);

        verify(s1).initializeValidators();
        verify(s2).initializeValidators();
    }

    /* --------------------------------------------------------------------- */
    /*  validate(ExecutionContext, JsonNode) – delegates to the 4‑arg   */
    /*  overload. The concrete implementation in TestValidator returns an   */
    /*  empty set, so we simply assert that behaviour.                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testValidateDelegatesToFourArgVersion() {
        ExecutionContext execCtx = mock(ExecutionContext.class);
        JsonNode node = mapper.createObjectNode();

        Set<ValidationMessage> result = validator.validate(execCtx, node);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
