/**
 * JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}.
 *
 * The class under test is abstract, therefore a minimal concrete subclass
 * {@code TestValidator} is provided that implements the only abstract method
 * required by {@link com.networknt.schema.JsonValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Utility static methods: {@code equals}, {@code debug},
 *       {@code checkDiscriminatorMatch}, {@code registerAndMergeDiscriminator},
 *       and the private helper methods used by them.</li>
 *   <li>Instance methods that depend on the constructor arguments:
 *       {@code getSchemaPath}, {@code getSchemaNode},
 *       {@code getParentSchema}, {@code fetchSubSchemaNode},
 *       {@code getNodeFieldType}, {@code preloadJsonSchemas},
 *       {@code atRoot}, {@code atPath(String, String)} and {@code atPath(String, int)}.</li>
 * </ul>
 *
 * Mockito is used to mock the heavy weight collaborators
 * ({@link com.networknt.schema.ValidationContext},
 * {@link com.networknt.schema.ValidationContext.Config},
 * {@link com.networknt.schema.ValidationContext.DiscriminatorContext},
 * {@link com.networknt.schema.JsonSchemaFactory},
 * {@link com.networknt.schema.URIFactory},
 * {@link com.networknt.schema.ExecutionContext},
 * {@link org.slf4j.Logger} etc.).
 *
 * The tests purposefully avoid any external files – all JSON nodes are created
 * programmatically with Jackson's {@link com.fasterxml.jackson.databind.ObjectMapper}.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Minimal concrete implementation of the abstract class. */
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
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at) {
            // For the purpose of the unit tests we never reach this method.
            return Collections.emptySet();
        }
    }

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ValidationContext.Config config;

    @Mock
    private ValidationContext.DiscriminatorContext discriminatorContext;

    @Mock
    private JsonSchema parentSchema;

    @Mock
    private JsonSchemaFactory jsonSchemaFactory;

    @Mock
    private URIFactory uriFactory;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private Logger logger;

    private PathType dummyPathType;

    @Before
    public void setUp() throws Exception {
        // ----- common mock behaviour -------------------------------------------------
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isFailFast()).thenReturn(false);
        when(config.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        when(config.getPathType()).thenReturn(dummyPathType = mock(PathType.class));
        when(dummyPathType.getRoot()).thenReturn("$");
        when(dummyPathType.append(anyString(), anyString())).thenAnswer(inv -> inv.getArgument(0) + "/" + inv.getArgument(1));
        when(dummyPathType.append(anyString(), anyInt())).thenAnswer(inv -> inv.getArgument(0) + "/" + inv.getArgument(1));

        when(validationContext.getJsonSchemaFactory()).thenReturn(jsonSchemaFactory);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
    }

    /* -------------------------------------------------------------------------
     * Helper to inject a public field 'schemaPath' into a mocked JsonSchema.
     * ----------------------------------------------------------------------- */
    private static void setSchemaPath(JsonSchema schema, String path) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("schemaPath");
        f.setAccessible(true);
        f.set(schema, path);
    }

    /* -------------------------------------------------------------------------
     * 1.  equals(double,double) – epsilon based comparison
     * ----------------------------------------------------------------------- */
    @Test
    public void testEqualsWithinEpsilon() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.000000000002));
    }

    @Test
    public void testEqualsOutsideEpsilon() {
        assertFalse(BaseJsonValidator.equals(1.0, 1.1));
    }

    /* -------------------------------------------------------------------------
     * 2.  debug(Logger,JsonNode,JsonNode,String) – verify logger call
     * ----------------------------------------------------------------------- */
    @Test
    public void testDebugLogsMessage() {
        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        JsonNode root = MAPPER.createObjectNode().put("root", true);
        BaseJsonValidator.debug(logger, node, root, "/path");
        verify(logger).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq("/path"));
    }

    /* -------------------------------------------------------------------------
     * 3.  fetchSubSchemaNode – suppressed retrieval
     * ----------------------------------------------------------------------- */
    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        TestValidator validator = new TestValidator(
                "/schema", MAPPER.createObjectNode(), parentSchema,
                ValidatorTypeCode.MAXIMUM, validationContext, true);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    /* -------------------------------------------------------------------------
     * 4.  fetchSubSchemaNode – no "id" field → null
     * ----------------------------------------------------------------------- */
    @Test
    public void testFetchSubSchemaNodeNoId() {
        JsonNode schemaNode = MAPPER.createObjectNode(); // empty node, no "id"
        TestValidator validator = new TestValidator(
                "/schema", schemaNode, parentSchema,
                ValidatorTypeCode.MAXIMUM, validationContext, false);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    /* -------------------------------------------------------------------------
     * 5.  fetchSubSchemaNode – malformed id → null
     * ----------------------------------------------------------------------- */
    @Test
    public void testFetchSubSchemaNodeInvalidId() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("id", ":// bad uri");

        when(uriFactory.create(anyString())).thenThrow(new IllegalArgumentException());

        TestValidator validator = new TestValidator(
                "/schema", schemaNode, parentSchema,
                ValidatorTypeCode.MAXIMUM, validationContext, false);
        assertNull(validator.fetchSubSchemaNode(validationContext));
    }

    /* -------------------------------------------------------------------------
     * 6.  fetchSubSchemaNode – valid id → schema returned
     * ----------------------------------------------------------------------- */
    @Test
    public void testFetchSubSchemaNodeValidId() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.put("id", "http://example.com/my-schema");

        URI uri = new URI("http://example.com/my-schema");
        JsonSchema fetched = mock(JsonSchema.class);

        when(uriFactory.create("http://example.com/my-schema")).thenReturn(uri);
        when(jsonSchemaFactory.getSchema(eq(uri), any())).thenReturn(fetched);

        TestValidator validator = new TestValidator(
                "/schema", schemaNode, parentSchema,
                ValidatorTypeCode.MAXIMUM, validationContext, false);
        assertSame(fetched, validator.fetchSubSchemaNode(validationContext));
    }

    /* -------------------------------------------------------------------------
     * 7.  getNodeFieldType – parent schema contains "type"
     * ----------------------------------------------------------------------- */
    @Test
    public void testGetNodeFieldTypePresent() {
        ObjectNode parentNode = MAPPER.createObjectNode();
        parentNode.put("type", "object");
        when(parentSchema.getSchemaNode()).thenReturn(parentNode);

        TestValidator validator = new TestValidator(
                "/schema", MAPPER.createObjectNode(), parentSchema,
                ValidatorTypeCode.MAXIMUM, validationContext, false);
        assertEquals("object", validator.getNodeFieldType());
    }

    /* -------------------------------------------------------------------------
     * 8.  getNodeFieldType – absent "type"
     * ----------------------------------------------------------------------- */
    @Test
    public void testGetNodeFieldTypeAbsent() {
        when(parentSchema.getSchemaNode()).thenReturn(MAPPER.createObjectNode()); // no type
        TestValidator validator = new TestValidator(
                "/schema", MAPPER.createObjectNode(), parentSchema,
                ValidatorTypeCode.MAXIMUM, validationContext, false);
        assertNull(validator.getNodeFieldType());
    }

    /* -------------------------------------------------------------------------
     * 9.  preloadJsonSchemas – ensure initializeValidators called
     * ----------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemasCallsInitialize() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        Collection<JsonSchema> coll = Arrays.asList(s1, s2);

        TestValidator validator = new TestValidator(
                "/schema", MAPPER.createObjectNode(), parentSchema,
                ValidatorTypeCode.MAXIMUM, validationContext, false);
        validator.preloadJsonSchemas(coll);

        verify(s1).initializeValidators();
        verify(s2).initializeValidators();
    }

    /* -------------------------------------------------------------------------
     * 10.  atRoot / atPath (String) / atPath (int)
     * ----------------------------------------------------------------------- */
    @Test
    public void testPathGeneration() {
        TestValidator validator = new TestValidator(
                "/schema", MAPPER.createObjectNode(), parentSchema,
                ValidatorTypeCode.MAXIMUM, validationContext, false);
        assertEquals("$", validator.atRoot());
        assertEquals("$" + "/foo", validator.atPath("$", "foo"));
        assertEquals("$" + "/5", validator.atPath("$", 5));
    }

    /* -------------------------------------------------------------------------
     * 11.  checkDiscriminatorMatch – null discriminator value → always match
     * ----------------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatchNullValue() {
        ObjectNode discriminator = MAPPER.createObjectNode();
        JsonSchema schema = mock(JsonSchema.class);
        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext, discriminator, null, schema);
        verify(discriminatorContext).markMatch();
    }

    /* -------------------------------------------------------------------------
     * 12.  checkDiscriminatorMatch – implicit match (path ends with value)
     * ----------------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatchImplicit() throws Exception {
        ObjectNode discriminator = MAPPER.createObjectNode(); // no mapping
        JsonSchema schema = mock(JsonSchema.class);
        setSchemaPath(schema, "/something/ValueX");

        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext, discriminator,
                "ValueX", schema);

        verify(discriminatorContext).markMatch();
    }

    /* -------------------------------------------------------------------------
     * 13.  checkDiscriminatorMatch – explicit mapping match
     * ----------------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatchExplicit() throws Exception {
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("Dog", "/pet/dog");
        ObjectNode discriminator = MAPPER.createObjectNode();
        discriminator.set("mapping", mapping);

        JsonSchema schema = mock(JsonSchema.class);
        setSchemaPath(schema, "/pet/dog");

        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext, discriminator,
                "Dog", schema);

        verify(discriminatorContext).markMatch();
    }

    /* -------------------------------------------------------------------------
     * 14.  checkDiscriminatorMatch – explicit mapping not matching, but implicit allowed
     * ----------------------------------------------------------------------- */
    @Test
    public void testCheckDiscriminatorMatchExplicitNoMatchImplicitAllowed() throws Exception {
        // explicit mapping for a different schema
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("Cat", "/pet/cat");
        ObjectNode discriminator = MAPPER.createObjectNode();
        discriminator.set("mapping", mapping);

        // parent schema that does NOT appear in the explicit mapping
        JsonSchema parent = mock(JsonSchema.class);
        setSchemaPath(parent, "/pet/dog");

        // schema under test – its path ends with the discriminator value → implicit match
        JsonSchema schema = mock(JsonSchema.class);
        setSchemaPath(schema, "/pet/Dog");

        // current discriminator context must report "no explicit key override"
        when(discriminatorContext.isDiscriminatorMatchFound()).thenReturn(false);
        // no explicit mapping overrides the parent schema path → true
        // (the static method inspects the mapping directly, no need to stub)

        BaseJsonValidator.checkDiscriminatorMatch(discriminatorContext, discriminator,
                "Dog", schema);

        verify(discriminatorContext).markMatch(); // implicit match triggered
    }

    /* -------------------------------------------------------------------------
     * 15.  registerAndMergeDiscriminator – first registration (no previous discriminator)
     * ----------------------------------------------------------------------- */
    @Test
    public void testRegisterAndMergeDiscriminatorFirstRegistration() throws Exception {
        // root discriminator (empty mapping)
        ObjectNode rootDisc = MAPPER.createObjectNode();

        // schema that contains a discriminator with a mapping
        ObjectNode schemaDisc = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("A", "/type/A");
        schemaDisc.set("mapping", mapping);
        schemaDisc.put("propertyName", "type"); // will cause exception if redefined – we will not set it

        JsonSchema schema = mock(JsonSchema.class);
        setSchemaPath(schema, "/type/A");
        // schemaNode contains the discriminator object
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", schemaDisc);
        when(schema.schemaNode).thenReturn(schemaNode); // direct field access via mock

        // No previous discriminator registered for the path, so merge should just copy mapping
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDisc, schema,
                "/type/A");

        // Verify that the root discriminator now contains the mapping from the schema
        JsonNode resultingMapping = rootDisc.get("mapping");
        assertNotNull(resultingMapping);
        assertEquals("/type/A", resultingMapping.get("A").asText());

        // And the discriminator is registered in the context
        verify(discriminatorContext).registerDiscriminator(eq("/type/A"), eq(rootDisc));
    }

    /* -------------------------------------------------------------------------
     * 16.  registerAndMergeDiscriminator – redefinition of propertyName throws
     * ----------------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorRedefinePropertyNameThrows() throws Exception {
        // existing root discriminator already registered for the same path
        ObjectNode rootDisc = MAPPER.createObjectNode();
        when(discriminatorContext.getDiscriminatorForPath("/type/A")).thenReturn(rootDisc);

        // schema with its own discriminator that defines propertyName (illegal)
        ObjectNode schemaDisc = MAPPER.createObjectNode();
        schemaDisc.put("propertyName", "type");
        JsonSchema schema = mock(JsonSchema.class);
        setSchemaPath(schema, "/type/A");
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", schemaDisc);
        when(schema.schemaNode).thenReturn(schemaNode);

        // This call must throw JsonSchemaException
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDisc, schema,
                "/type/A");
    }

    /* -------------------------------------------------------------------------
     * 17.  registerAndMergeDiscriminator – mapping conflict throws
     * ----------------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorMappingConflictThrows() throws Exception {
        // existing root discriminator with a mapping for key "X"
        ObjectNode rootDisc = MAPPER.createObjectNode();
        ObjectNode rootMapping = MAPPER.createObjectNode();
        rootMapping.put("X", "/type/X1");
        rootDisc.set("mapping", rootMapping);
        when(discriminatorContext.getDiscriminatorForPath("/type/X")).thenReturn(rootDisc);

        // schema with a discriminator that tries to redefine the same key with a different value
        ObjectNode schemaDisc = MAPPER.createObjectNode();
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.put("X", "/type/X2"); // different target → conflict
        schemaDisc.set("mapping", schemaMapping);
        JsonSchema schema = mock(JsonSchema.class);
        setSchemaPath(schema, "/type/X");
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", schemaDisc);
        when(schema.schemaNode).thenReturn(schemaNode);

        // This call must throw JsonSchemaException because of the conflict
        BaseJsonValidator.registerAndMergeDiscriminator(discriminatorContext, rootDisc, schema,
                "/type/X");
    }

    /* -------------------------------------------------------------------------
     * 18.  getSchemaPath / getSchemaNode / getParentSchema – simple getters
     * ----------------------------------------------------------------------- */
    @Test
    public void testGetters() {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("foo", "bar");
        TestValidator validator = new TestValidator(
                "/myPath", schemaNode, parentSchema,
                ValidatorTypeCode.MAXIMUM, validationContext, false);
        assertEquals("/myPath", validator.getSchemaPath());
        assertSame(schemaNode, validator.getSchemaNode());
        assertSame(parentSchema, validator.getParentSchema());
    }
}
