/***************************************************************************************************
 * JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}
 *
 * The test class creates a concrete subclass (TestValidator) that exposes the protected members
 * of BaseJsonValidator so they can be exercised.  Mockito is used to mock the complex collaborators
 * (ValidationContext, JsonSchema, JsonSchemaFactory, URIFactory, Config etc.).
 *
 * The suite follows the requested steps:
 *   1. List of public (and relevant protected) methods with signatures.
 *   2. Basic unit tests for the normal behaviour of each method.
 *   3. Identification of edge‑cases / exception handling.
 *   4. Additional tests that cover those edge‑cases.
 *   5. All tests are merged into a single JUnit‑4 test file.
 ***************************************************************************************************/

package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Concrete implementation of {@link BaseJsonValidator} used only for testing.
 * It implements the abstract {@code validate} method with a trivial implementation
 * that returns an empty {@link Set}.
 */
class TestValidator extends BaseJsonValidator {

    TestValidator(String schemaPath,
                  JsonNode schemaNode,
                  JsonSchema parentSchema,
                  ValidatorTypeCode validatorType,
                  ValidationContext validationContext) {
        super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
    }

    TestValidator(String schemaPath,
                  JsonNode schemaNode,
                  JsonSchema parentSchema,
                  ValidatorTypeCode validatorType,
                  ValidationContext validationContext,
                  boolean suppressSubSchemaRetrieval) {
        super(schemaPath, schemaNode, parentSchema, validatorType,
                validationContext, suppressSubSchemaRetrieval);
    }

    /** Trivial implementation – the real validators are tested elsewhere. */
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
        return Collections.emptySet();
    }

    /** Expose protected helpers for testing */
    String exposeAtRoot() { return atRoot(); }
    String exposeAtPath(String cur, String token) { return atPath(cur, token); }
    String exposeAtPath(String cur, int index) { return atPath(cur, index); }
    JsonSchema exposeFetchSubSchemaNode(ValidationContext vc) { return fetchSubSchemaNode(vc); }
    boolean exposeEquals(double n1, double n2) { return equals(n1, n2); }
    void exposePreloadJsonSchemas(Collection<JsonSchema> schemas) { preloadJsonSchemas(schemas); }
}

/**
 * Test suite.
 */
@RunWith(MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // -----------------------------------------------------------------------
    // 1.  Public / protected methods to be covered (signatures)
    // -----------------------------------------------------------------------
    // public String getSchemaPath()
    // public JsonNode getSchemaNode()
    // public JsonSchema getParentSchema()
    // public Set<ValidationMessage> validate(ExecutionContext, JsonNode)
    // protected JsonSchema fetchSubSchemaNode(ValidationContext)
    // protected static boolean equals(double, double)
    // protected static void debug(Logger, JsonNode, JsonNode, String)
    // protected String getNodeFieldType()
    // protected void preloadJsonSchemas(Collection<JsonSchema>)
    // protected PathType getPathType()
    // protected String atRoot()
    // protected String atPath(String, String)
    // protected String atPath(String, int)

    // -----------------------------------------------------------------------
    // Mocks used across many tests
    // -----------------------------------------------------------------------
    @Mock private ValidationContext mockContext;
    @Mock private ValidationContext.Config mockConfig;
    @Mock private JsonSchemaFactory mockFactory;
    @Mock private URIFactory mockURIFactory;
    @Mock private JsonSchema mockParentSchema;
    @Mock private JsonSchema mockSubSchema;
    @Mock private Logger mockLogger;
    @Mock private ExecutionContext mockExecutionContext;

    /** Simple {@link PathType} implementation that mimics the default behaviour
     *  (root = "$", dot‑separated for objects, bracket for arrays). */
    private static class SimplePathType implements PathType {
        @Override public String getRoot() { return "$"; }
        @Override public String append(String cur, String token) { return cur + "." + token; }
        @Override public String append(String cur, int index) { return cur + "[" + index + "]"; }
    }

    @Before
    public void setUp() {
        // Default behaviour for the ValidationContext hierarchy
        Mockito.when(mockContext.getConfig()).thenReturn(mockConfig);
        Mockito.when(mockConfig.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        Mockito.when(mockConfig.getPathType()).thenReturn(new SimplePathType());
        Mockito.when(mockContext.getURIFactory()).thenReturn(mockURIFactory);
        Mockito.when(mockContext.getJsonSchemaFactory()).thenReturn(mockFactory);
    }

    // -----------------------------------------------------------------------
    // 2.  Basic tests for normal behaviour
    // -----------------------------------------------------------------------

    @Test
    public void testGetSchemaPath() {
        TestValidator validator = new TestValidator("myPath", NullNode.getInstance(),
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        assertEquals("myPath", validator.getSchemaPath());
    }

    @Test
    public void testGetSchemaNode() {
        ObjectNode node = MAPPER.createObjectNode().put("type", "object");
        TestValidator validator = new TestValidator("p", node, mockParentSchema,
                ValidatorTypeCode.TYPE, mockContext);
        assertSame(node, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema() {
        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        assertSame(mockParentSchema, validator.getParentSchema());
    }

    @Test
    public void testValidateReturnsEmptySet() {
        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        Set<ValidationMessage> result = validator.validate(mockExecutionContext, NullNode.getInstance());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetNodeFieldTypeFound() {
        ObjectNode parentSchemaNode = MAPPER.createObjectNode().put("type", "string");
        JsonSchema parentSchema = Mockito.mock(JsonSchema.class);
        Mockito.when(parentSchema.getSchemaNode()).thenReturn(parentSchemaNode);

        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                parentSchema, ValidatorTypeCode.TYPE, mockContext);
        assertEquals("string", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeMissing() {
        JsonSchema parentSchema = Mockito.mock(JsonSchema.class);
        Mockito.when(parentSchema.getSchemaNode()).thenReturn(NullNode.getInstance());

        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                parentSchema, ValidatorTypeCode.TYPE, mockContext);
        assertNull(validator.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemasInvokesInitialize() {
        JsonSchema schema1 = Mockito.mock(JsonSchema.class);
        JsonSchema schema2 = Mockito.mock(JsonSchema.class);
        Set<JsonSchema> schemas = new HashSet<>();
        schemas.add(schema1);
        schemas.add(schema2);

        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        validator.exposePreloadJsonSchemas(schemas);

        Mockito.verify(schema1).initializeValidators();
        Mockito.verify(schema2).initializeValidators();
    }

    @Test
    public void testAtRootAndAtPath() {
        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);

        assertEquals("$", validator.exposeAtRoot());
        assertEquals("$.name", validator.exposeAtPath("$", "name"));
        assertEquals("$.items[3]", validator.exposeAtPath("$", 3));
    }

    @Test
    public void testStaticEqualsCloseValues() {
        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        assertTrue(validator.exposeEquals(1.000000000001, 1.0));
        assertTrue(validator.exposeEquals(-0.0, 0.0));
    }

    @Test
    public void testStaticEqualsDifferentValues() {
        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        assertFalse(validator.exposeEquals(1.0, 1.1));
    }

    // -----------------------------------------------------------------------
    // 3.  Edge‑case / exception handling tests
    // -----------------------------------------------------------------------

    /** When suppressSubSchemaRetrieval is true, fetchSubSchemaNode must return null irrespective of the schema. */
    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext, true);
        assertNull(validator.exposeFetchSubSchemaNode(mockContext));
    }

    /** No "id" property → obtainSubSchemaNode returns null */
    @Test
    public void testFetchSubSchemaNodeNoId() {
        ObjectNode schemaNode = MAPPER.createObjectNode(); // empty – no "id"
        TestValidator validator = new TestValidator("p", schemaNode,
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        assertNull(validator.exposeFetchSubSchemaNode(mockContext));
    }

    /** "id" present but not a valid URI → obtainSubSchemaNode returns null */
    @Test
    public void testFetchSubSchemaNodeInvalidUri() {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("id", ":// not a uri");
        // URIFactory.create will throw IllegalArgumentException – we let the mock throw it.
        Mockito.when(mockURIFactory.create(Mockito.anyString()))
               .thenThrow(new IllegalArgumentException("bad uri"));

        TestValidator validator = new TestValidator("p", schemaNode,
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        assertNull(validator.exposeFetchSubSchemaNode(mockContext));
    }

    /** Valid "id" that resolves to a schema – fetchSubSchemaNode returns that schema. */
    @Test
    public void testFetchSubSchemaNodeValid() throws Exception {
        final String idValue = "http://example.com/schema.json";
        ObjectNode schemaNode = MAPPER.createObjectNode().put("id", idValue);

        // Mock the URI creation
        URI uri = new URI(idValue);
        Mockito.when(mockURIFactory.create(idValue)).thenReturn(uri);

        // Mock the factory to return a concrete JsonSchema
        Mockito.when(mockFactory.getSchema(uri, mockConfig)).thenReturn(mockSubSchema);

        TestValidator validator = new TestValidator("p", schemaNode,
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        JsonSchema result = validator.exposeFetchSubSchemaNode(mockContext);
        assertSame(mockSubSchema, result);
    }

    /** The static debug method should not throw even when logger is a mock. */
    @Test
    public void testDebugDoesNotThrow() {
        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        JsonNode root = MAPPER.createObjectNode().put("root", true);
        // No need to verify the logger call – just ensure no exception is raised.
        BaseJsonValidator.debug(mockLogger, node, root, "testAt");
    }

    // -----------------------------------------------------------------------
    // 4.  Additional edge cases for atPath (null / empty token, negative index)
    // -----------------------------------------------------------------------

    @Test
    public void testAtPathWithNullToken() {
        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        // The implementation simply concatenates; passing null results in "null" string.
        assertEquals("$.null", validator.exposeAtPath("$", (String) null));
    }

    @Test
    public void testAtPathWithEmptyToken() {
        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        assertEquals("$.", validator.exposeAtPath("$", ""));
    }

    @Test
    public void testAtPathWithNegativeIndex() {
        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        assertEquals("$[-5]", validator.exposeAtPath("$", -5));
    }

    // -----------------------------------------------------------------------
    // 5.  Coverage for getPathType (protected) via subclass exposure
    // -----------------------------------------------------------------------
    @Test
    public void testGetPathTypeUsesConfigOrDefault() {
        // Config provides a custom PathType – we already stubbed it in setUp()
        TestValidator validator = new TestValidator("p", NullNode.getInstance(),
                mockParentSchema, ValidatorTypeCode.TYPE, mockContext);
        assertTrue(validator.getPathType() instanceof SimplePathType);
    }
}
