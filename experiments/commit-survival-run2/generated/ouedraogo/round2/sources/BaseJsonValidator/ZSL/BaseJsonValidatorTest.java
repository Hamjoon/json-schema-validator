package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

/**
 * Unit tests for {@link BaseJsonValidator}. The class under test is abstract,
 * therefore a simple concrete subclass {@link TestValidator} is provided that
 * implements the {@code validate} method with a no‑op implementation.
 *
 * The tests cover:
 * <ul>
 *   <li>Utility static methods {@code equals} and {@code debug}</li>
 *   <li>All public/protected getters</li>
 *   <li>Behaviour of {@code fetchSubSchemaNode} when sub‑schema retrieval is suppressed</li>
 *   <li>Node type extraction via {@code getNodeFieldType}</li>
 *   <li>Pre‑loading of schemas with {@code preloadJsonSchemas}</li>
 *   <li>Path handling helpers {@code atRoot}, {@code atPath(String, String)} and {@code atPath(String, int)}</li>
 * </ul>
 */
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple concrete implementation for testing */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, null, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation – return empty set
            return Collections.emptySet();
        }
    }

    private JsonSchema parentSchemaMock;
    private ValidationContext validationContextMock;
    private ObjectNode simpleSchemaNode;
    private TestValidator validator;

    @Before
    public void setUp() {
        // parent schema mock – used only for getNodeFieldType()
        parentSchemaMock = mock(JsonSchema.class);
        // By default the parent schema has no "type" field
        when(parentSchemaMock.getSchemaNode()).thenReturn(MAPPER.createObjectNode());

        // validation context mock – not used for most tests (null is acceptable)
        validationContextMock = mock(ValidationContext.class);
        when(validationContextMock.getConfig()).thenReturn(null); // config is optional for our tests

        simpleSchemaNode = MAPPER.createObjectNode();
        simpleSchemaNode.put("title", "Test Schema");

        validator = new TestValidator("testPath", simpleSchemaNode, parentSchemaMock,
                validationContextMock, false);
    }

    /** --------------------------------------------------------------------- **/
    /**                     Tests for static utility methods                 **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void testEquals_CloseNumbers_ShouldBeTrue() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.000000000002));
    }

    @Test
    public void testEquals_FarNumbers_ShouldBeFalse() {
        assertFalse(BaseJsonValidator.equals(1.0, 2.0));
    }

    @Test
    public void testDebug_LogsCorrectMessage() {
        Logger logger = mock(Logger.class);
        JsonNode node = MAPPER.createObjectNode().put("field", "value");
        JsonNode root = MAPPER.createObjectNode().put("root", true);
        String at = "$.path";

        BaseJsonValidator.debug(logger, node, root, at);

        // Verify that logger.debug was called with the exact format string and arguments
        verify(logger).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq(at));
    }

    /** --------------------------------------------------------------------- **/
    /**                         Getter / simple behaviours                     **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void testGetSchemaPath_ReturnsConstructorValue() {
        assertEquals("testPath", validator.getSchemaPath());
    }

    @Test
    public void testGetSchemaNode_ReturnsSuppliedNode() {
        assertSame(simpleSchemaNode, validator.getSchemaNode());
    }

    @Test
    public void testGetParentSchema_ReturnsSuppliedParent() {
        assertSame(parentSchemaMock, validator.getParentSchema());
    }

    @Test
    public void testFetchSubSchemaNode_WhenSuppressed_ShouldReturnNull() {
        // Create a validator with suppressSubSchemaRetrieval = true
        TestValidator suppressed = new TestValidator("path", simpleSchemaNode,
                parentSchemaMock, validationContextMock, true);
        assertNull(suppressed.fetchSubSchemaNode(validationContextMock));
    }

    /** --------------------------------------------------------------------- **/
    /**                 getNodeFieldType – with and without "type"            **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void testGetNodeFieldType_WithType_ShouldReturnValue() {
        // parent schema contains a "type" field
        ObjectNode parentNode = MAPPER.createObjectNode().put("type", "string");
        when(parentSchemaMock.getSchemaNode()).thenReturn(parentNode);

        assertEquals("string", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldType_WithoutType_ShouldReturnNull() {
        // parent schema already returns an empty object (no "type")
        when(parentSchemaMock.getSchemaNode()).thenReturn(MAPPER.createObjectNode());

        assertNull(validator.getNodeFieldType());
    }

    /** --------------------------------------------------------------------- **/
    /**               preloadJsonSchemas – ensure initializeValidators called  **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void testPreloadJsonSchemas_InvokesInitializeValidators() {
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        Set<JsonSchema> schemas = new HashSet<>();
        schemas.add(schemaA);
        schemas.add(schemaB);

        validator.preloadJsonSchemas(schemas);

        verify(schemaA).initializeValidators();
        verify(schemaB).initializeValidators();
    }

    /** --------------------------------------------------------------------- **/
    /**                     Path handling helpers (atRoot, atPath)           **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void testAtRoot_ReturnsNonNullRootPath() {
        String root = validator.atRoot();
        assertNotNull(root);
        // The default PathType implementation uses "/" as root – verify that at least it contains a slash
        assertTrue(root.contains("/"));
    }

    @Test
    public void testAtPath_WithStringToken_AppendsCorrectly() {
        String root = validator.atRoot(); // e.g. "/"
        String child = "property";

        String result = validator.atPath(root, child);
        assertNotNull(result);
        // The result should contain the child token
        assertTrue(result.endsWith(child));
        // And it should start with the root path
        assertTrue(result.startsWith(root));
    }

    @Test
    public void testAtPath_WithIndex_AppendsCorrectly() {
        String root = validator.atRoot();
        int index = 3;

        String result = validator.atPath(root, index);
        assertNotNull(result);
        // The result should end with the index value
        assertTrue(result.endsWith(String.valueOf(index)));
        // And start with the root path
        assertTrue(result.startsWith(root));
    }

    /** --------------------------------------------------------------------- **/
    /**                 getPathType – ensure the default is used               **/
    /** --------------------------------------------------------------------- **/

    @Test
    public void testGetPathType_ReturnsDefaultWhenNoConfigProvided() {
        // ValidationContext with null config -> BaseJsonValidator should fallback to PathType.DEFAULT
        PathType pathType = validator.getPathType();
        assertNotNull(pathType);
        // The default implementation returns a non‑empty root path
        assertEquals("/", pathType.getRoot());
    }
}
