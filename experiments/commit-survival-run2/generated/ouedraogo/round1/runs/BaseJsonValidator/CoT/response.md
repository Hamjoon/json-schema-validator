###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
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
 * Unit tests for {@link BaseJsonValidator}.  Because the class is abstract we
 * provide a tiny concrete subclass that simply forwards to the protected
 * methods we want to test.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    // -----------------------------------------------------------------------
    // Helper concrete implementation
    // -----------------------------------------------------------------------
    private static class TestableBaseJsonValidator extends BaseJsonValidator {

        TestableBaseJsonValidator(String schemaPath,
                                  JsonNode schemaNode,
                                  JsonSchema parentSchema,
                                  ValidatorTypeCode validatorType,
                                  ValidationContext validationContext,
                                  boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType,
                    validationContext, suppressSubSchemaRetrieval);
        }

        // the abstract contract of JsonValidator is already satisfied by the
        // implementation in BaseJsonValidator, therefore no extra methods are
        // required.
    }

    // -----------------------------------------------------------------------
    // Mocks & test data
    // -----------------------------------------------------------------------
    @Mock private JsonNode schemaNodeMock;
    @Mock private JsonSchema parentSchemaMock;
    @Mock private ValidationContext validationContextMock;
    @Mock private ValidationContext.Config configMock;
    @Mock private ValidationContext.DiscriminatorContext discriminatorContextMock;
    @Mock private JsonSchemaFactory jsonSchemaFactoryMock;
    @Mock private URIFactory uriFactoryMock;
    @Mock private JsonSchema subSchemaMock;
    @Mock private ExecutionContext executionContextMock;
    @Mock private Logger loggerMock;
    @Mock private ObjectNode discriminatorNodeMock;
    @Mock private JsonNode mappingNodeMock;
    @Mock private JsonNode idNodeMock;

    private static final String SCHEMA_PATH = "/my/path";
    private static final ValidatorTypeCode VALIDATOR_TYPE = ValidatorTypeCode.MAX_LENGTH; // any constant

    private TestableBaseJsonValidator validator;

    @Before
    public void setUp() {
        // Default behaviour for the validation context configuration
        when(validationContextMock.getConfig()).thenReturn(configMock);
        when(configMock.isFailFast()).thenReturn(false);
        when(configMock.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(configMock.getPathType()).thenReturn(PathType.DEFAULT);

        // Default behaviour for URI factory
        when(validationContextMock.getURIFactory()).thenReturn(uriFactoryMock);
        when(validationContextMock.getJsonSchemaFactory()).thenReturn(jsonSchemaFactoryMock);

        validator = new TestableBaseJsonValidator(
                SCHEMA_PATH,
                schemaNodeMock,
                parentSchemaMock,
                VALIDATOR_TYPE,
                validationContextMock,
                false);
    }

    // -----------------------------------------------------------------------
    // Public getter methods
    // -----------------------------------------------------------------------
    @Test
    public void testGetSchemaPath() {
        assertEquals(SCHEMA_PATH, validator.getSchemaPath());
    }

    @Test
    public void testGetSchemaNode() {
        assertSame(schemaNodeMock, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema() {
        assertSame(parentSchemaMock, validator.getParentSchema());
    }

    // -----------------------------------------------------------------------
    // fetchSubSchemaNode – normal, suppressed and edge‑cases
    // -----------------------------------------------------------------------
    @Test
    public void testFetchSubSchemaNode_SuppressedReturnsNull() {
        TestableBaseJsonValidator suppressed = new TestableBaseJsonValidator(
                SCHEMA_PATH, schemaNodeMock, parentSchemaMock,
                VALIDATOR_TYPE, validationContextMock, true);
        assertNull(suppressed.fetchSubSchemaNode(validationContextMock));
    }

    @Test
    public void testFetchSubSchemaNode_NoIdReturnsNull() {
        // schemaNode does not contain an "id" field
        when(schemaNodeMock.get("id")).thenReturn(null);
        assertNull(validator.fetchSubSchemaNode(validationContextMock));
    }

    @Test
    public void testFetchSubSchemaNode_InvalidIdReturnsNull() {
        when(schemaNodeMock.get("id")).thenReturn(idNodeMock);
        when(idNodeMock.textValue()).thenReturn("not a uri");
        when(uriFactoryMock.create("not a uri")).thenThrow(new IllegalArgumentException("bad uri"));
        assertNull(validator.fetchSubSchemaNode(validationContextMock));
    }

    @Test
    public void testFetchSubSchemaNode_ValidIdReturnsSchema() throws Exception {
        when(schemaNodeMock.get("id")).thenReturn(idNodeMock);
        when(idNodeMock.textValue()).thenReturn("http://example.com/schema");
        URI uri = new URI("http://example.com/schema");
        when(uriFactoryMock.create("http://example.com/schema")).thenReturn(uri);
        when(jsonSchemaFactoryMock.getSchema(uri, configMock)).thenReturn(subSchemaMock);

        JsonSchema result = validator.fetchSubSchemaNode(validationContextMock);
        assertSame(subSchemaMock, result);
    }

    // -----------------------------------------------------------------------
    // equals(double,double) – tolerance handling
    // -----------------------------------------------------------------------
    @Test
    public void testEquals_Tolerance() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.000000000002));
        assertFalse(BaseJsonValidator.equals(1.0, 1.1));
    }

    // -----------------------------------------------------------------------
    // checkDiscriminatorMatch – various branches
    // -----------------------------------------------------------------------
    @Test
    public void testCheckDiscriminatorMatch_NullPropertyValueMarksMatch() {
        // When property value is null we expect a direct call to markMatch()
        BaseJsonValidator.checkDiscriminatorMatch(
                discriminatorContextMock,
                discriminatorNodeMock,
                null,
                subSchemaMock);
        verify(discriminatorContextMock, times(1)).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitMappingMatch() {
        // Build a discriminator node with an explicit mapping that matches
        when(discriminatorNodeMock.get("mapping")).thenReturn(mappingNodeMock);
        // mapping: {"foo":"#/components/schemas/Bar"}
        JsonNode mappingValueNode = mock(JsonNode.class);
        when(mappingNodeMock.fields()).thenReturn(
                Collections.enumeration(
                        Collections.singletonMap("foo", mappingValueNode).entrySet()));
        when(mappingValueNode.asText()).thenReturn("#/components/schemas/Bar");

        // Schema that we are checking – its path must equal the mapping value
        JsonSchema matchingSchema = mock(JsonSchema.class);
        when(matchingSchema.schemaPath).thenReturn("#/components/schemas/Bar");

        BaseJsonValidator.checkDiscriminatorMatch(
                discriminatorContextMock,
                discriminatorNodeMock,
                "foo",
                matchingSchema);

        verify(discriminatorContextMock, times(1)).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitMappingNoMatch_ImplicitAllowed() {
        // mapping exists but does not contain the key we are looking for
        when(discriminatorNodeMock.get("mapping")).thenReturn(mappingNodeMock);
        when(mappingNodeMock.fields()).thenReturn(
                Collections.enumeration(Collections.emptyMap().entrySet()));

        // Parent schema path ends with the discriminator property value → implicit match
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/some/root/foo");

        // no explicit key overrides for the parent schema
        BaseJsonValidator.checkDiscriminatorMatch(
                discriminatorContextMock,
                discriminatorNodeMock,
                "foo",
                schema);

        verify(discriminatorContextMock, times(1)).markMatch();
    }

    // -----------------------------------------------------------------------
    // registerAndMergeDiscriminator – error handling and merging
    // -----------------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminator_RedefinePropertyNameThrows() {
        // Simulate a root discriminator already registered for the same path
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        when(ctx.getDiscriminatorForPath(anyString())).thenReturn(mock(ObjectNode.class));

        // The schema we are registering also contains a discriminator with a propertyName
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/base");
        ObjectNode schemaDiscriminator = mock(ObjectNode.class);
        JsonNode propertyNameNode = mock(JsonNode.class);
        when(propertyNameNode.asText()).thenReturn("type");
        when(schemaDiscriminator.get("propertyName")).thenReturn(propertyNameNode);
        when(schema.schemaNode).thenReturn(schemaDiscriminator);

        // Root discriminator (the one we pass in) – it does NOT have a mapping, just an empty object
        ObjectNode rootDiscriminator = mock(ObjectNode.class);
        when(rootDiscriminator.get("mapping")).thenReturn(null);

        // This should throw because the schema tries to redefine propertyName
        BaseJsonValidator.registerAndMergeDiscriminator(
                ctx,
                rootDiscriminator,
                schema,
                "at ");
    }

    @Test
    public void testRegisterAndMergeDiscriminator_MergeMappingsWithoutRedefinition() {
        // Context already has a mapping {"A":"#/schemas/A"}
        ObjectNode existingMapping = new com.fasterxml.jackson.databind.node.ObjectNode(
                new com.fasterxml.jackson.databind.node.JsonNodeFactory(false));
        existingMapping.set("A", new com.fasterxml.jackson.databind.node.TextNode("#/schemas/A"));

        // Context discriminator node with the mapping above
        ObjectNode contextDiscriminator = new com.fasterxml.jackson.databind.node.ObjectNode(
                new com.fasterxml.jackson.databind.node.JsonNodeFactory(false));
        contextDiscriminator.set("mapping", existingMapping);

        // Schema we are registering has a *different* mapping {"B":"#/schemas/B"}
        ObjectNode schemaDiscriminator = new com.fasterxml.jackson.databind.node.ObjectNode(
                new com.fasterxml.jackson.databind.node.JsonNodeFactory(false));
        ObjectNode schemaMapping = new com.fasterxml.jackson.databind.node.ObjectNode(
                new com.fasterxml.jackson.databind.node.JsonNodeFactory(false));
        schemaMapping.set("B", new com.fasterxml.jackson.databind.node.TextNode("#/schemas/B"));
        schemaDiscriminator.set("mapping", schemaMapping);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/schemas/B");
        when(schema.schemaNode).thenReturn(schemaDiscriminator);

        // Context that will return the existing discriminator for the same path
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        when(ctx.getDiscriminatorForPath(anyString())).thenReturn(contextDiscriminator);
        when(ctx.getDiscriminatorForPath("/schemas/B")).thenReturn(null); // not needed but safe

        // invoke – should not throw
        BaseJsonValidator.registerAndMergeDiscriminator(
                ctx,
                contextDiscriminator,
                schema,
                "at ");

        // Verify that the merged mapping now contains both keys
        JsonNode merged = contextDiscriminator.get("mapping");
        assertNotNull(merged);
        assertTrue(merged.has("A"));
        assertTrue(merged.has("B"));
        assertEquals("#/schemas/B", merged.get("B").asText());
    }

    // -----------------------------------------------------------------------
    // Path handling helpers
    // -----------------------------------------------------------------------
    @Test
    public void testAtRoot_NotNull() {
        String root = validator.atRoot();
        assertNotNull(root);
    }

    @Test
    public void testAtPath_WithToken() {
        String result = validator.atPath("/parent", "child");
        // The exact format depends on PathType implementation; we only assert that the
        // result contains both components.
        assertTrue(result.contains("/parent"));
        assertTrue(result.contains("child"));
    }

    @Test
    public void testAtPath_WithIndex() {
        String result = validator.atPath("/array", 3);
        assertTrue(result.contains("/array"));
        assertTrue(result.contains("[3]") || result.contains("3"));
    }

    // -----------------------------------------------------------------------
    // preloadJsonSchemas – ensure each schema is initialized
    // -----------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchemas() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(s1, s2);

        validator.preloadJsonSchemas(schemas);

        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
    }

    // -----------------------------------------------------------------------
    // getNodeFieldType – returns the "type" field from the parent schema node
    // -----------------------------------------------------------------------
    @Test
    public void testGetNodeFieldTypeWhenPresent() {
        JsonNode typeNode = mock(JsonNode.class);
        when(typeNode.asText()).thenReturn("object");
        JsonNode parentSchemaNode = mock(JsonNode.class);
        when(parentSchemaMock.getSchemaNode()).thenReturn(parentSchemaNode);
        when(parentSchemaNode.get("type")).thenReturn(typeNode);

        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenMissing() {
        JsonNode parentSchemaNode = mock(JsonNode.class);
        when(parentSchemaMock.getSchemaNode()).thenReturn(parentSchemaNode);
        when(parentSchemaNode.get("type")).thenReturn(null);

        assertNull(validator.getNodeFieldType());
    }

    // -----------------------------------------------------------------------
    // validate(ExecutionContext, JsonNode) – basic delegation test
    // -----------------------------------------------------------------------
    @Test
    public void testValidateDelegatesToThreeArgMethod() {
        JsonNode node = mock(JsonNode.class);
        Set<ValidationMessage> expected = Collections.emptySet();

        // Spy the validator to intercept the three‑argument overload
        TestableBaseJsonValidator spy = Mockito.spy(validator);
        doReturn(expected).when(spy).validate(any(ExecutionContext.class), any(JsonNode.class),
                any(JsonNode.class), anyString());

        Set<ValidationMessage> actual = spy.validate(executionContextMock, node);
        assertSame(expected, actual);
        verify(spy, times(1)).validate(eq(executionContextMock), eq(node), eq(node), anyString());
    }
}
```
###Test END##