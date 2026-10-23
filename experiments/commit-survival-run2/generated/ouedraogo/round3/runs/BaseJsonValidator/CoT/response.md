###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Extraction of all public methods (including constructors).</li>
 *   <li>Basic functional verification of each public method.</li>
 *   <li>Edge‑case and exception handling scenarios (e.g. missing “id”, suppress flag, null type field,
 *       preload of schemas, path handling).</li>
 * </ul>
 *
 * <p>Because {@code BaseJsonValidator} is abstract, a lightweight concrete subclass is defined
 * inside the test file.  All external collaborators are mocked with Mockito so that the tests
 * remain fast, deterministic and isolated from the rest of the library.
 */

package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Concrete subclass used only for testing the protected members of {@link BaseJsonValidator}.
 */
class TestJsonValidator extends BaseJsonValidator {

    TestJsonValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
        super(schemaPath, schemaNode, parentSchema, null, validationContext, suppressSubSchemaRetrieval);
    }

    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
        // Minimal implementation – the real validation logic is out of scope for these unit tests.
        return Collections.emptySet();
    }
}

/**
 * Unit tests for {@link BaseJsonValidator}.
 */
public class BaseJsonValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema mockParentSchema;
    private JsonNode parentSchemaNode;
    private JsonNode childSchemaNode;
    private ValidationContext mockValidationContext;

    @Before
    public void setUp() {
        // ----- parent schema -------------------------------------------------
        mockParentSchema = mock(JsonSchema.class);
        parentSchemaNode = MAPPER.createObjectNode(); // empty by default
        when(mockParentSchema.getSchemaNode()).thenReturn(parentSchemaNode);
        when(mockParentSchema.schemaPath).thenReturn("#/parent");

        // ----- child schema --------------------------------------------------
        childSchemaNode = MAPPER.createObjectNode(); // empty by default

        // ----- validation context (can be null for many tests) ----------------
        mockValidationContext = mock(ValidationContext.class);
        // The config is optional – we let the mock return null so that defaults are used.
        when(mockValidationContext.getConfig()).thenReturn(null);
    }

    /* --------------------------------------------------------------------- *
     *  Public method list (extracted from the source)
     * --------------------------------------------------------------------- *
     *
     * 1. BaseJsonValidator(String schemaPath, JsonNode schemaNode,
     *                      JsonSchema parentSchema, ValidatorTypeCode validatorType,
     *                      ValidationContext validationContext)
     *
     * 2. BaseJsonValidator(String schemaPath, JsonNode schemaNode,
     *                      JsonSchema parentSchema, ValidatorTypeCode validatorType,
     *                      ValidationContext validationContext, boolean suppressSubSchemaRetrieval)
     *
     * 3. String getSchemaPath()
     * 4. JsonNode getSchemaNode()
     * 5. JsonSchema getParentSchema()
     * 6. JsonSchema fetchSubSchemaNode(ValidationContext validationContext)
     * 7. Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node)
     * 8. String getNodeFieldType()
     * 9. void preloadJsonSchemas(Collection<JsonSchema> schemas)
     *10. PathType getPathType()
     *11. String atRoot()
     *12. String atPath(String currentPath, String token)
     *13. String atPath(String currentPath, int index)
     *
     * (static protected helpers are also present but are exercised indirectly.)
     */

    /* --------------------------------------------------------------------- *
     *  1‑2. Constructors – basic instantiation test
     * --------------------------------------------------------------------- */
    @Test
    public void constructor_shouldInitialiseFields() {
        TestJsonValidator validator = new TestJsonValidator(
                "#/test",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                false);

        assertEquals("#/test", validator.getSchemaPath());
        assertSame(childSchemaNode, validator.getSchemaNode());
        assertSame(mockParentSchema, validator.getParentSchema());
    }

    /* --------------------------------------------------------------------- *
     *  3. getSchemaPath()
     * --------------------------------------------------------------------- */
    @Test
    public void getSchemaPath_returnsConstructorValue() {
        TestJsonValidator validator = new TestJsonValidator(
                "#/myPath",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                false);
        assertEquals("#/myPath", validator.getSchemaPath());
    }

    /* --------------------------------------------------------------------- *
     *  4. getSchemaNode()
     * --------------------------------------------------------------------- */
    @Test
    public void getSchemaNode_returnsProvidedNode() {
        ObjectNode node = MAPPER.createObjectNode().put("foo", "bar");
        TestJsonValidator validator = new TestJsonValidator(
                "#/node",
                node,
                mockParentSchema,
                mockValidationContext,
                false);
        assertSame(node, validator.getSchemaNode());
    }

    /* --------------------------------------------------------------------- *
     *  5. getParentSchema()
     * --------------------------------------------------------------------- */
    @Test
    public void getParentSchema_returnsParentSchemaReference() {
        TestJsonValidator validator = new TestJsonValidator(
                "#/child",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                false);
        assertSame(mockParentSchema, validator.getParentSchema());
    }

    /* --------------------------------------------------------------------- *
     *  6. fetchSubSchemaNode()
     * --------------------------------------------------------------------- */
    @Test
    public void fetchSubSchemaNode_returnsNullWhenSuppressed() {
        TestJsonValidator validator = new TestJsonValidator(
                "#/suppress",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                true); // suppress flag = true
        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void fetchSubSchemaNode_returnsNullWhenNoIdField() {
        // schemaNode does NOT contain an "id" property
        TestJsonValidator validator = new TestJsonValidator(
                "#/noId",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                false);
        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
    }

    @Test
    public void fetchSubSchemaNode_returnsNullWhenIdIsInvalidUri() {
        // Create a schema node with an invalid URI in the "id" field.
        ObjectNode nodeWithBadId = MAPPER.createObjectNode().put("id", ":::not a uri:::");
        TestJsonValidator validator = new TestJsonValidator(
                "#/badId",
                nodeWithBadId,
                mockParentSchema,
                mockValidationContext,
                false);

        // The internal obtainSubSchemaNode catches IllegalArgumentException and returns null.
        assertNull(validator.fetchSubSchemaNode(mockValidationContext));
    }

    /* --------------------------------------------------------------------- *
     *  7. validate()
     * --------------------------------------------------------------------- */
    @Test
    public void validate_returnsEmptySetFromConcreteImplementation() {
        TestJsonValidator validator = new TestJsonValidator(
                "#/validate",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                false);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        JsonNode dummyNode = MAPPER.createObjectNode();
        Set<ValidationMessage> result = validator.validate(execCtx, dummyNode);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  8. getNodeFieldType()
     * --------------------------------------------------------------------- */
    @Test
    public void getNodeFieldType_returnsParentTypeWhenPresent() {
        ((ObjectNode) parentSchemaNode).put("type", "object");
        TestJsonValidator validator = new TestJsonValidator(
                "#/type",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                false);
        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void getNodeFieldType_returnsNullWhenParentHasNoType() {
        // parentSchemaNode is empty (no "type")
        TestJsonValidator validator = new TestJsonValidator(
                "#/noType",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                false);
        assertNull(validator.getNodeFieldType());
    }

    /* --------------------------------------------------------------------- *
     *  9. preloadJsonSchemas()
     * --------------------------------------------------------------------- */
    @Test
    public void preloadJsonSchemas_callsInitializeValidatorsOnEachSchema() {
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        TestJsonValidator validator = new TestJsonValidator(
                "#/preload",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                false);

        validator.preloadJsonSchemas(Set.of(schemaA, schemaB));

        verify(schemaA, times(1)).initializeValidators();
        verify(schemaB, times(1)).initializeValidators();
    }

    /* --------------------------------------------------------------------- *
     * 10. getPathType()
     * --------------------------------------------------------------------- */
    @Test
    public void getPathType_returnsDefaultWhenContextHasNoPathType() {
        TestJsonValidator validator = new TestJsonValidator(
                "#/pathType",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                false);
        // The default is PathType.DEFAULT (a constant defined in the library).
        assertSame(PathType.DEFAULT, validator.getPathType());
    }

    /* --------------------------------------------------------------------- *
     * 11. atRoot()
     * --------------------------------------------------------------------- */
    @Test
    public void atRoot_returnsRootFromPathType() {
        TestJsonValidator validator = new TestJsonValidator(
                "#/root",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                false);
        String root = validator.atRoot();
        assertEquals(PathType.DEFAULT.getRoot(), root);
    }

    /* --------------------------------------------------------------------- *
     * 12‑13. atPath() overloads
     * --------------------------------------------------------------------- */
    @Test
    public void atPath_withStringToken_appendsCorrectly() {
        TestJsonValidator validator = new TestJsonValidator(
                "#/path",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                false);
        String result = validator.atPath("#/parent", "child");
        // PathType.DEFAULT appends with '/' – we only assert that the token appears.
        assertTrue(result.endsWith("/child"));
        assertTrue(result.startsWith("#/parent"));
    }

    @Test
    public void atPath_withIndex_appendsCorrectly() {
        TestJsonValidator validator = new TestJsonValidator(
                "#/pathIdx",
                childSchemaNode,
                mockParentSchema,
                mockValidationContext,
                false);
        String result = validator.atPath("#/array", 2);
        assertTrue(result.endsWith("[2]") || result.endsWith("/2")); // depending on PathType implementation
        assertTrue(result.startsWith("#/array"));
    }

    /* --------------------------------------------------------------------- *
     *  Protected static helper – equals(double,double)
     * --------------------------------------------------------------------- */
    @Test
    public void equals_doubleToleranceWorks() throws Exception {
        // Use reflection because the method is protected.
        java.lang.reflect.Method m = BaseJsonValidator.class.getDeclaredMethod("equals", double.class, double.class);
        m.setAccessible(true);
        boolean close = (Boolean) m.invoke(null, 1.0, 1.0 + 1e-13);
        boolean far = (Boolean) m.invoke(null, 1.0, 1.0 + 1e-5);
        assertTrue("Numbers within tolerance should be equal", close);
        assertFalse("Numbers outside tolerance should not be equal", far);
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – debug(Logger,JsonNode,JsonNode,String) does not throw
     * --------------------------------------------------------------------- */
    @Test
    public void debug_doesNotThrowWhenLoggerIsNull() throws Exception {
        // The method is protected static; invoke via reflection.
        java.lang.reflect.Method debug = BaseJsonValidator.class.getDeclaredMethod(
                "debug", Logger.class, JsonNode.class, JsonNode.class, String.class);
        debug.setAccessible(true);
        // Pass null logger – it should simply do nothing (no NPE).
        debug.invoke(null, null, MAPPER.createObjectNode(), MAPPER.createObjectNode(), "test");
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – checkDiscriminatorMatch with null discriminatorPropertyValue
     * --------------------------------------------------------------------- */
    @Test
    public void checkDiscriminatorMatch_nullPropertyValue_marksMatch() throws Exception {
        // Prepare a mock DiscriminatorContext that records the call to markMatch().
        ValidationContext.DiscriminatorContext mockCtx = mock(ValidationContext.DiscriminatorContext.class);
        ObjectNode discriminator = MAPPER.createObjectNode(); // content irrelevant for this case
        JsonSchema dummySchema = mock(JsonSchema.class);

        java.lang.reflect.Method method = BaseJsonValidator.class.getDeclaredMethod(
                "checkDiscriminatorMatch",
                ValidationContext.DiscriminatorContext.class,
                ObjectNode.class,
                String.class,
                JsonSchema.class);
        method.setAccessible(true);
        method.invoke(null, mockCtx, discriminator, null, dummySchema);
        verify(mockCtx, times(1)).markMatch();
    }
}
```
###Test END##