###Test START##
```java
/****************************************************************************************
 * JUnit 4 test suite for {@link com.networknt.schema.BaseJsonValidator}
 *
 * The tests cover:
 *  - Constructor initialization and getters
 *  - Static utility methods (equals, debug, discriminator handling)
 *  - Path handling (atRoot, atPath)
 *  - Helper methods (fetchSubSchemaNode, preloadJsonSchemas, getNodeFieldType)
 *
 * Mockito is used to mock the heavy dependencies (ValidationContext, JsonSchema,
 * ExecutionContext, DiscriminatorContext, etc.).
 *
 * Author: Automated Test Generator
 ****************************************************************************************/
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
 * Concrete implementation of {@link BaseJsonValidator} used only for testing.
 * The {@code validate} method simply returns an empty set – the real validation
 * logic is not the focus of this test class.
 */
class TestValidator extends BaseJsonValidator {

    TestValidator(String schemaPath,
                  JsonNode schemaNode,
                  JsonSchema parentSchema,
                  ValidatorTypeCode validatorType,
                  ValidationContext validationContext,
                  boolean suppressSubSchemaRetrieval) {
        super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
    }

    @Override
    protected Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
        // No actual validation – return empty set.
        return Collections.emptySet();
    }
}

/**
 * Test suite for {@link BaseJsonValidator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    // -------------------------------------------------------------------------
    // Common test fixtures
    // -------------------------------------------------------------------------
    @Mock
    private ValidationContext validationContext;

    @Mock
    private ValidationContext.Config config;

    @Mock
    private JsonSchema parentSchema;

    @Mock
    private JsonSchema subSchema; // used for fetchSubSchemaNode tests

    @Mock
    private JsonSchemaFactory jsonSchemaFactory;

    @Mock
    private URIFactory uriFactory;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Logger logger;

    private ObjectMapper mapper = new ObjectMapper();

    private ObjectNode simpleSchemaNode;
    private TestValidator validator;

    @Before
    public void setUp() {
        // Basic schema node used by many tests
        simpleSchemaNode = mapper.createObjectNode();
        simpleSchemaNode.put("title", "test schema");

        // Default mock behaviour for validation context
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isFailFast()).thenReturn(false);
        when(config.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(config.getPathType()).thenReturn(PathType.DEFAULT);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.getJsonSchemaFactory()).thenReturn(jsonSchemaFactory);
        when(uriFactory.create(anyString())).thenReturn(URI.create("http://example.com/schema"));
        when(jsonSchemaFactory.getSchema(any(URI.class), any()))
                .thenReturn(subSchema);

        // Create a validator instance (suppressSubSchemaRetrieval = false)
        validator = new TestValidator(
                "#/properties/name",
                simpleSchemaNode,
                parentSchema,
                ValidatorTypeCode.TYPE, // arbitrary, not used by tests
                validationContext,
                false);
    }

    // -------------------------------------------------------------------------
    // Constructor / getters
    // -------------------------------------------------------------------------
    @Test
    public void testConstructorInitialisesFields() {
        assertEquals("#/properties/name", validator.getSchemaPath());
        assertSame(simpleSchemaNode, validator.getSchemaNode());
        assertSame(parentSchema, validator.getParentSchema());
        // PathType defaults to PathType.DEFAULT as set up in the mock config
        assertEquals(PathType.DEFAULT, validator.getPathType());
    }

    // -------------------------------------------------------------------------
    // Static utility methods
    // -------------------------------------------------------------------------
    @Test
    public void testEqualsWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertTrue(BaseJsonValidator.equals(-5.123456, -5.123456 + 5e-13));
    }

    @Test
    public void testEqualsOutsideTolerance() {
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-10));
        assertFalse(BaseJsonValidator.equals(100.0, 101.0));
    }

    @Test
    public void testDebugLogsCorrectly() {
        JsonNode node = mapper.createObjectNode().put("a", 1);
        JsonNode root = mapper.createObjectNode().put("root", true);
        BaseJsonValidator.debug(logger, node, root, "path");
        verify(logger, times(1)).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq("path"));
    }

    // -------------------------------------------------------------------------
    // Path handling (atRoot / atPath)
    // -------------------------------------------------------------------------
    @Test
    public void testAtRootUsesPathTypeRoot() {
        assertEquals(PathType.DEFAULT.getRoot(), validator.atRoot());
    }

    @Test
    public void testAtPathWithStringToken() {
        String result = validator.atPath("/properties", "name");
        // PathType.DEFAULT appends using '/' separator
        assertEquals("/properties/name", result);
    }

    @Test
    public void testAtPathWithIndex() {
        String result = validator.atPath("/items", 2);
        assertEquals("/items/2", result);
    }

    // -------------------------------------------------------------------------
    // getNodeFieldType()
    // -------------------------------------------------------------------------
    @Test
    public void testGetNodeFieldTypeWhenPresent() {
        // parent schema node contains a "type" field
        ObjectNode parentNode = mapper.createObjectNode();
        parentNode.put("type", "object");
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);

        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeWhenAbsent() {
        when(parentSchema.getSchemaNode()).thenReturn(mapper.createObjectNode()); // empty node
        assertNull(validator.getNodeFieldType());
    }

    // -------------------------------------------------------------------------
    // fetchSubSchemaNode()
    // -------------------------------------------------------------------------
    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        TestValidator suppressed = new TestValidator(
                "#/any",
                simpleSchemaNode,
                parentSchema,
                ValidatorTypeCode.TYPE,
                validationContext,
                true); // suppress = true

        assertNull(suppressed.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNodeWhenNotSuppressedAndIdPresent() {
        // Add an "id" field that points to a resolvable URI
        simpleSchemaNode.put("id", "http://example.com/schema");
        JsonSchema result = validator.fetchSubSchemaNode(validationContext);
        assertSame(subSchema, result);
    }

    @Test
    public void testFetchSubSchemaNodeReturnsNullWhenIdMissing() {
        // No "id" field – obtainSubSchemaNode returns null internally
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    // -------------------------------------------------------------------------
    // preloadJsonSchemas()
    // -------------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchemasInvokesInitializeValidators() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        Collection<JsonSchema> collection = Arrays.asList(s1, s2);

        validator.preloadJsonSchemas(collection);

        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
    }

    // -------------------------------------------------------------------------
    // Discriminator handling (private static methods exercised via reflection)
    // -------------------------------------------------------------------------
    @Test
    public void testCheckDiscriminatorMatchWhenPropertyValueIsNullMarksMatch() {
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        ObjectNode discriminator = mapper.createObjectNode();
        JsonSchema schema = mock(JsonSchema.class);

        // invoke the private static method via reflection
        invokeStaticCheckDiscriminatorMatch(ctx, discriminator, null, schema);

        verify(ctx, times(1)).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingFound() {
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        ObjectNode discriminator = mapper.createObjectNode();
        ObjectNode mapping = mapper.createObjectNode();
        mapping.put("dog", "#/definitions/Dog");
        discriminator.set("mapping", mapping);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.getSchemaPath()).thenReturn("#/definitions/Dog");

        invokeStaticCheckDiscriminatorMatch(ctx, discriminator, "dog", schema);

        verify(ctx, times(1)).markMatch();
        verify(ctx, never()).isDiscriminatorMatchFound(); // not needed for this path
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitWhenNoExplicitMapping() {
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        ObjectNode discriminator = mapper.createObjectNode(); // no mapping node

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.getSchemaPath()).thenReturn("#/definitions/cat");

        // discriminator property value matches the tail of the schema path
        invokeStaticCheckDiscriminatorMatch(ctx, discriminator, "cat", schema);

        verify(ctx, times(1)).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatchNoMatchLeavesContextUntouched() {
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        ObjectNode discriminator = mapper.createObjectNode();
        ObjectNode mapping = mapper.createObjectNode();
        mapping.put("bird", "#/definitions/Bird");
        discriminator.set("mapping", mapping);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.getSchemaPath()).thenReturn("#/definitions/Fish");

        invokeStaticCheckDiscriminatorMatch(ctx, discriminator, "dog", schema);

        verify(ctx, never()).markMatch();
    }

    @Test
    public void testRegisterAndMergeDiscriminatorAddsRootWhenNoExisting() {
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        // No discriminator already registered for the schema path
        when(ctx.getDiscriminatorForPath(anyString())).thenReturn(null);

        ObjectNode rootDiscriminator = mapper.createObjectNode();
        rootDiscriminator.put("propertyName", "type");

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("#/components/schemas/Base");
        // schemaNode without its own discriminator
        ObjectNode schemaNode = mapper.createObjectNode();
        when(schema.schemaNode).thenReturn(schemaNode);

        // Call method
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at ");

        // Verify that the discriminator was simply registered
        verify(ctx, times(1)).registerDiscriminator(eq("#/components/schemas/Base"), same(rootDiscriminator));
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorThrowsWhenPropertyNameRedefined() {
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        // Simulate that a root discriminator already exists for the same path
        when(ctx.getDiscriminatorForPath(anyString())).thenReturn(mock(ObjectNode.class));

        // Schema contains its own discriminator with a propertyName (illegal redefinition)
        ObjectNode schemaDiscriminator = mapper.createObjectNode();
        schemaDiscriminator.put("propertyName", "newProp");
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.set("discriminator", schemaDiscriminator);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("#/components/schemas/Derived");
        when(schema.schemaNode).thenReturn(schemaNode);

        ObjectNode rootDiscriminator = mapper.createObjectNode(); // empty root discriminator

        // Should throw JsonSchemaException
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at ");
    }

    @Test
    public void testRegisterAndMergeDiscriminatorMergesMappingsWithoutConflict() {
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        // Pretend a root discriminator already exists for the path
        ObjectNode rootDiscriminator = mapper.createObjectNode();
        ObjectNode rootMapping = mapper.createObjectNode();
        rootMapping.put("cat", "#/definitions/Cat");
        rootDiscriminator.set("mapping", rootMapping);
        when(ctx.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);

        // Schema provides an additional mapping entry
        ObjectNode schemaDiscriminator = mapper.createObjectNode();
        ObjectNode schemaMapping = mapper.createObjectNode();
        schemaMapping.put("dog", "#/definitions/Dog");
        schemaDiscriminator.set("mapping", schemaMapping);
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.set("discriminator", schemaDiscriminator);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("#/components/schemas/Animal");
        when(schema.schemaNode).thenReturn(schemaNode);

        // Execute merge
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at ");

        // After merge, root mapping must contain both entries
        JsonNode merged = rootDiscriminator.get("mapping");
        assertNotNull(merged);
        assertEquals("#/definitions/Cat", merged.get("cat").asText());
        assertEquals("#/definitions/Dog", merged.get("dog").asText());

        verify(ctx, times(1)).registerDiscriminator(eq("#/components/schemas/Animal"), same(rootDiscriminator));
    }

    @Test
    public void testNoExplicitDiscriminatorKeyOverrideReturnsTrueWhenNoOverride() {
        ObjectNode mapping = mapper.createObjectNode();
        mapping.put("car", "#/definitions/Vehicle");
        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.schemaPath).thenReturn("#/definitions/Parent");

        boolean result = invokeStaticNoExplicitDiscriminatorKeyOverride(mapping, parentSchema);
        assertTrue(result);
    }

    @Test
    public void testNoExplicitDiscriminatorKeyOverrideReturnsFalseWhenOverrideExists() {
        ObjectNode mapping = mapper.createObjectNode();
        mapping.put("car", "#/definitions/Parent"); // points to parent schema path
        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.schemaPath).thenReturn("#/definitions/Parent");

        boolean result = invokeStaticNoExplicitDiscriminatorKeyOverride(mapping, parentSchema);
        assertFalse(result);
    }

    // -------------------------------------------------------------------------
    // Helper methods to invoke private static methods via reflection
    // -------------------------------------------------------------------------
    private void invokeStaticCheckDiscriminatorMatch(ValidationContext.DiscriminatorContext ctx,
                                                     ObjectNode discriminator,
                                                     String discriminatorPropertyValue,
                                                     JsonSchema schema) {
        try {
            java.lang.reflect.Method m = BaseJsonValidator.class
                    .getDeclaredMethod("checkDiscriminatorMatch",
                            ValidationContext.DiscriminatorContext.class,
                            ObjectNode.class,
                            String.class,
                            JsonSchema.class);
            m.setAccessible(true);
            m.invoke(null, ctx, discriminator, discriminatorPropertyValue, schema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean invokeStaticNoExplicitDiscriminatorKeyOverride(JsonNode mapping,
                                                                   JsonSchema parentSchema) {
        try {
            java.lang.reflect.Method m = BaseJsonValidator.class
                    .getDeclaredMethod("noExplicitDiscriminatorKeyOverride",
                            JsonNode.class,
                            JsonSchema.class);
            m.setAccessible(true);
            return (boolean) m.invoke(null, mapping, parentSchema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```
###Test END##