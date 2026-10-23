/**
 * JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}.
 *
 * The class under test is abstract, therefore a minimal concrete subclass
 * {@code DummyValidator} is defined inside the test class.  All public and
 * protected members of {@code BaseJsonValidator} are exercised, including
 * the static helper methods that are used by the validator framework.
 *
 * The tests rely on Mockito for creating lightweight mocks of the many
 * collaborators ( {@link JsonNode}, {@link JsonSchema},
 * {@link ValidationContext}, {@link Logger}, etc.).  By configuring the
 * mocks we can drive each execution path inside the static methods
 * {@code checkDiscriminatorMatch}, {@code registerAndMergeDiscriminator},
 * {@code fetchSubSchemaNode} and the private {@code obtainSubSchemaNode}
 * helper.
 *
 * The test class is deliberately verbose – each logical branch is covered
 * by a separate test method and the assertions explain the expected
 * behaviour.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

/**
 * Concrete validator used only for testing the BaseJsonValidator behaviour.
 */
class DummyValidator extends BaseJsonValidator {

    DummyValidator(String schemaPath,
                    JsonNode schemaNode,
                    JsonSchema parentSchema,
                    ValidatorTypeCode validatorType,
                    ValidationContext validationContext,
                    boolean suppressSubSchemaRetrieval) {
        super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
    }

    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
        // For the purpose of the unit tests we simply return an empty set.
        return Collections.emptySet();
    }
}

/**
 * Test suite.
 */
public class BaseJsonValidatorTest {

    private JsonNode schemaNode;
    private JsonSchema parentSchema;
    private ValidationContext validationContext;
    private ValidationContext.Config config;
    private JsonSchemaFactory jsonSchemaFactory;
    private URIFactory uriFactory;
    private Logger logger;

    @Before
    public void setUp() {
        schemaNode = mock(JsonNode.class);
        parentSchema = mock(JsonSchema.class);
        validationContext = mock(ValidationContext.class);
        config = mock(ValidationContext.Config.class);
        jsonSchemaFactory = mock(JsonSchemaFactory.class);
        uriFactory = mock(URIFactory.class);
        logger = mock(Logger.class);

        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getJsonSchemaFactory()).thenReturn(jsonSchemaFactory);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        // Use the default PathType – we do not need a custom one.
        when(config.getPathType()).thenReturn(PathType.DEFAULT);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for the static helper methods
     * --------------------------------------------------------------------- */

    @Test
    public void testEqualsWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.0));
        assertTrue(BaseJsonValidator.equals(-0.0, 0.0));
    }

    @Test
    public void testEqualsOutsideTolerance() {
        assertFalse(BaseJsonValidator.equals(1.0, 1.1));
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN)); // NaN is never equal
    }

    @Test
    public void testDebugLogsCorrectMessage() {
        JsonNode node = mock(JsonNode.class);
        JsonNode root = mock(JsonNode.class);
        BaseJsonValidator.debug(logger, node, root, "path");

        ArgumentCaptor<String> fmt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> arg1 = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> arg2 = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> arg3 = ArgumentCaptor.forClass(Object.class);

        verify(logger).debug(fmt.capture(), arg1.capture(), arg2.capture(), arg3.capture());

        assertEquals("validate( {}, {}, {})", fmt.getValue());
        assertSame(node, arg1.getValue());
        assertSame(root, arg2.getValue());
        assertEquals("path", arg3.getValue());
    }

    /* --------------------------------------------------------------------- *
     *  fetchSubSchemaNode / obtainSubSchemaNode
     * --------------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        DummyValidator validator = new DummyValidator("#", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE, validationContext, true);
        assertNull(validator.fetchSubSchemaNode(validationContext));
        // no interaction with the ValidationContext should happen
        verifyNoInteractions(validationContext);
    }

    @Test
    public void testFetchSubSchemaNode_NoId() {
        when(schemaNode.get("id")).thenReturn(null);
        DummyValidator validator = new DummyValidator("#", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE, validationContext, false);
        assertNull(validator.fetchSubSchemaNode(validationContext));
        verify(schemaNode).get("id");
    }

    @Test
    public void testFetchSubSchemaNode_IdEqualsSchema() {
        JsonNode idNode = mock(JsonNode.class);
        JsonNode schemaNodeNode = mock(JsonNode.class);
        when(idNode.equals(schemaNodeNode)).thenReturn(true);
        when(schemaNode.get("id")).thenReturn(idNode);
        when(schemaNode.get("$schema")).thenReturn(schemaNodeNode);
        DummyValidator validator = new DummyValidator("#", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE, validationContext, false);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNode_InvalidUri() {
        JsonNode idNode = mock(JsonNode.class);
        when(idNode.textValue()).thenReturn("ht!tp://bad_uri");
        when(schemaNode.get("id")).thenReturn(idNode);
        when(uriFactory.create(anyString())).thenThrow(new IllegalArgumentException("bad"));
        DummyValidator validator = new DummyValidator("#", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE, validationContext, false);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNode_ValidUri() {
        JsonNode idNode = mock(JsonNode.class);
        when(idNode.textValue()).thenReturn("http://example.com/schema.json");
        when(schemaNode.get("id")).thenReturn(idNode);
        when(uriFactory.create("http://example.com/schema.json")).thenReturn(URI.create("http://example.com/schema.json"));
        JsonSchema expected = mock(JsonSchema.class);
        when(jsonSchemaFactory.getSchema(URI.create("http://example.com/schema.json"), config))
                .thenReturn(expected);

        DummyValidator validator = new DummyValidator("#", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE, validationContext, false);
        assertSame(expected, validator.fetchSubSchemaNode(validationContext));
    }

    /* --------------------------------------------------------------------- *
     *  checkDiscriminatorMatch – various branches
     * --------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatch_NullPropertyValueMarksMatch() {
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        // No exception – just ensure markMatch() is called.
        BaseJsonValidator.checkDiscriminatorMatch(ctx, mock(ObjectNode.class), null, mock(JsonSchema.class));
        verify(ctx).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatch_NoMapping_ImplicitMatch() {
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        ObjectNode discriminator = mock(ObjectNode.class);
        when(discriminator.get("mapping")).thenReturn(null);
        JsonSchema schema = mock(JsonSchema.class);
        // schema path ends with the discriminator value → implicit match
        when(schema.schemaPath).thenReturn("#/components/schemas/pet");
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "pet", schema);
        verify(ctx).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitMappingMatch() {
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        ObjectNode discriminator = mock(ObjectNode.class);
        ObjectNode mapping = mock(ObjectNode.class);
        when(discriminator.get("mapping")).thenReturn(mapping);
        // mapping entry "dog" -> "#/components/schemas/Dog"
        JsonNode mappingValue = mock(JsonNode.class);
        when(mappingValue.asText()).thenReturn("#/components/schemas/Dog");
        when(mapping.fields()).thenReturn(Collections.singletonMap("dog", mappingValue).entrySet().iterator());

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("#/components/schemas/Dog");

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "dog", schema);
        verify(ctx).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitNoMatch_ImplicitAllowed() {
        // mapping does not contain the value, but there is no overriding key → implicit match
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        when(ctx.isDiscriminatorMatchFound()).thenReturn(false);
        ObjectNode discriminator = mock(ObjectNode.class);
        ObjectNode mapping = mock(ObjectNode.class);
        when(discriminator.get("mapping")).thenReturn(mapping);
        // mapping contains a different key
        JsonNode otherMappingValue = mock(JsonNode.class);
        when(otherMappingValue.asText()).thenReturn("#/components/schemas/Cat");
        when(mapping.fields()).thenReturn(Collections.singletonMap("cat", otherMappingValue).entrySet().iterator());

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("#/components/schemas/dog");

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "dog", schema);
        // because no explicit match and no override, implicit match should be performed
        verify(ctx).markMatch();
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitNoMatch_OverridePreventsImplicit() {
        // mapping contains a key that points to the parent schema → override, implicit match must NOT happen
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        when(ctx.isDiscriminatorMatchFound()).thenReturn(false);
        ObjectNode discriminator = mock(ObjectNode.class);
        ObjectNode mapping = mock(ObjectNode.class);
        when(discriminator.get("mapping")).thenReturn(mapping);
        // mapping has entry that points to the parent schema path "#/parent"
        JsonNode overridingValue = mock(JsonNode.class);
        when(overridingValue.asText()).thenReturn("#/parent");
        when(mapping.fields()).thenReturn(Collections.singletonMap("dog", overridingValue).entrySet().iterator());

        JsonSchema parentSchema = mock(JsonSchema.class);
        when(parentSchema.schemaPath).thenReturn("#/parent");
        JsonSchema childSchema = mock(JsonSchema.class);
        when(childSchema.schemaPath).thenReturn("#/components/schemas/Dog");

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "dog", childSchema);
        // markMatch must NOT be called because the override blocks implicit match
        verify(ctx, never()).markMatch();
    }

    /* --------------------------------------------------------------------- *
     *  registerAndMergeDiscriminator – merge and exception handling
     * --------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminator_RedefinePropertyNameThrows() {
        // Setup a root discriminator already registered with a propertyName
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        when(ctx.getDiscriminatorForPath(anyString())).thenReturn(mock(ObjectNode.class));

        // schema that contains a discriminator with a propertyName (redefinition)
        JsonNode discNode = mock(JsonNode.class);
        JsonNode propNameNode = mock(JsonNode.class);
        when(discNode.get("propertyName")).thenReturn(propNameNode);
        when(discNode.get("mapping")).thenReturn(null);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaNode).thenReturn(discNode);
        when(schema.schemaPath).thenReturn("#/schemas/Child");

        ObjectNode rootDiscriminator = mock(ObjectNode.class);
        when(rootDiscriminator.get("mapping")).thenReturn(null);

        // The call must throw JsonSchemaException
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at ");
    }

    @Test
    public void testRegisterAndMergeDiscriminator_AddNewMapping() {
        // Context with no existing discriminator (first registration)
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        when(ctx.getDiscriminatorForPath(anyString())).thenReturn(null);

        // Schema with a discriminator that contains a mapping
        ObjectNode schemaMapping = mock(ObjectNode.class);
        when(schemaMapping.fields()).thenReturn(Collections.emptyIterator());

        JsonNode discNode = mock(JsonNode.class);
        when(discNode.get("propertyName")).thenReturn(null);
        when(discNode.get("mapping")).thenReturn(schemaMapping);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaNode).thenReturn(discNode);
        when(schema.schemaPath).thenReturn("#/schemas/Child");

        // Root discriminator initially without a mapping
        ObjectNode rootDiscriminator = mock(ObjectNode.class);
        when(rootDiscriminator.get("mapping")).thenReturn(null);

        // Perform merge – should set the root's "mapping" to the schema's discriminator object
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at ");

        verify(rootDiscriminator).set(eq("mapping"), eq(discNode));
        verify(ctx).registerDiscriminator(eq("#/schemas/Child"), eq(rootDiscriminator));
    }

    @Test
    public void testRegisterAndMergeDiscriminator_MergeExistingMappings() {
        // Context already has a discriminator with a mapping containing key "cat"
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        ObjectNode existingMapping = mock(ObjectNode.class);
        when(existingMapping.fields()).thenReturn(Collections.singletonMap("cat", mock(JsonNode.class)).entrySet().iterator());

        ObjectNode rootDiscriminator = mock(ObjectNode.class);
        when(rootDiscriminator.get("mapping")).thenReturn(existingMapping);
        when(ctx.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);

        // Schema provides a new mapping entry "dog" -> "#/schemas/Dog"
        ObjectNode schemaMapping = mock(ObjectNode.class);
        JsonNode dogNode = mock(JsonNode.class);
        when(dogNode.asText()).thenReturn("#/schemas/Dog");
        when(schemaMapping.fields()).thenReturn(Collections.singletonMap("dog", dogNode).entrySet().iterator());

        JsonNode discNode = mock(JsonNode.class);
        when(discNode.get("propertyName")).thenReturn(null);
        when(discNode.get("mapping")).thenReturn(schemaMapping);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaNode).thenReturn(discNode);
        when(schema.schemaPath).thenReturn("#/schemas/Child");

        // Merge – should add the new key "dog" without touching existing "cat"
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at ");

        verify(existingMapping).set(eq("dog"), eq(dogNode));
        verify(ctx).registerDiscriminator(eq("#/schemas/Child"), eq(rootDiscriminator));
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminator_ConflictingMappingThrows() {
        // Existing mapping contains key "dog" with a different value
        ValidationContext.DiscriminatorContext ctx = mock(ValidationContext.DiscriminatorContext.class);
        ObjectNode existingMapping = mock(ObjectNode.class);
        JsonNode existingDogNode = mock(JsonNode.class);
        when(existingDogNode.asText()).thenReturn("#/schemas/OldDog");
        when(existingMapping.fields()).thenReturn(Collections.singletonMap("dog", existingDogNode).entrySet().iterator());

        ObjectNode rootDiscriminator = mock(ObjectNode.class);
        when(rootDiscriminator.get("mapping")).thenReturn(existingMapping);
        when(ctx.getDiscriminatorForPath(anyString())).thenReturn(rootDiscriminator);

        // New schema tries to map "dog" to a different schema path
        ObjectNode schemaMapping = mock(ObjectNode.class);
        JsonNode newDogNode = mock(JsonNode.class);
        when(newDogNode.asText()).thenReturn("#/schemas/NewDog");
        when(schemaMapping.fields()).thenReturn(Collections.singletonMap("dog", newDogNode).entrySet().iterator());

        JsonNode discNode = mock(JsonNode.class);
        when(discNode.get("propertyName")).thenReturn(null);
        when(discNode.get("mapping")).thenReturn(schemaMapping);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaNode).thenReturn(discNode);
        when(schema.schemaPath).thenReturn("#/schemas/Child");

        // This call must throw because the mapping already exists with a different value
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at ");
    }

    /* --------------------------------------------------------------------- *
     *  Simple getters – sanity checks
     * --------------------------------------------------------------------- */

    @Test
    public void testGettersReturnSuppliedValues() {
        DummyValidator validator = new DummyValidator("#/path", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE, validationContext, false);
        assertEquals("#/path", validator.getSchemaPath());
        assertSame(schemaNode, validator.getSchemaNode());
        assertSame(parentSchema, validator.getParentSchema());
    }

    /* --------------------------------------------------------------------- *
     *  getNodeFieldType – reads the "type" field from the parent schema
     * --------------------------------------------------------------------- */

    @Test
    public void testGetNodeFieldType_Exists() {
        JsonNode typeNode = mock(JsonNode.class);
        when(typeNode.asText()).thenReturn("object");
        JsonNode parentSchemaNode = mock(JsonNode.class);
        when(parentSchemaNode.get("type")).thenReturn(typeNode);
        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);

        DummyValidator validator = new DummyValidator("#", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE, validationContext, false);
        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldType_NotPresent() {
        JsonNode parentSchemaNode = mock(JsonNode.class);
        when(parentSchemaNode.get("type")).thenReturn(null);
        when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);

        DummyValidator validator = new DummyValidator("#", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE, validationContext, false);
        assertNull(validator.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- *
     *  preloadJsonSchemas – verify initializeValidators() is called on each
     * --------------------------------------------------------------------- */

    @Test
    public void testPreloadJsonSchemasInitializesAll() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        List<JsonSchema> list = Arrays.asList(s1, s2);

        DummyValidator validator = new DummyValidator("#", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE, validationContext, false);
        validator.preloadJsonSchemas(list);

        verify(s1).initializeValidators();
        verify(s2).initializeValidators();
    }

    /* --------------------------------------------------------------------- *
     *  Path handling – atRoot, atPath(String, String) and atPath(String, int)
     * --------------------------------------------------------------------- */

    @Test
    public void testPathHelpersUseDefaultPathType() {
        // With a null ValidationContext the constructor falls back to PathType.DEFAULT
        DummyValidator validator = new DummyValidator("#", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE, null, false);

        String root = validator.atRoot();
        assertNotNull(root);
        // The default PathType uses "$" as root (as defined by the library)
        assertEquals("$", root);

        String child = validator.atPath(root, "foo");
        // For the default PathType the expected format is "$.foo"
        assertEquals("$.foo", child);

        String indexed = validator.atPath(root, 3);
        assertEquals("$[3]", indexed);
    }

    /* --------------------------------------------------------------------- *
     *  validate(ExecutionContext, JsonNode) – simple delegation test
     * --------------------------------------------------------------------- */

    @Test
    public void testValidateDelegatesToThreeArgumentVersion() {
        ExecutionContext execCtx = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        DummyValidator validator = spy(new DummyValidator("#", schemaNode, parentSchema,
                ValidatorTypeCode.TYPE, validationContext, false));

        // Stub the three‑argument version to return a known set
        Set<ValidationMessage> expected = Collections.singleton(new ValidationMessage("code", "path", "msg"));
        doReturn(expected).when(validator).validate(execCtx, node, node, validator.atRoot());

        Set<ValidationMessage> actual = validator.validate(execCtx, node);
        assertSame(expected, actual);
        verify(validator).validate(execCtx, node, node, validator.atRoot());
    }
}
