**###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchema}.
 *
 * The tests are written with Mockito to isolate {@code JsonSchema} from the rest of the
 * library (which is large and contains many side‑effects).  Only the public API is
 * exercised – constructors, accessor methods, validation entry points and the
 * special handling of $recursiveAnchor, $id and $ref.
 *
 * The test class follows the steps requested:
 *   1. List all public methods (see the comment block at the top of the file).
 *   2. Provide a basic test for each method.
 *   3. Identify edge‑cases / exception scenarios.
 *   4. Add dedicated tests for those edge‑cases.
 *   5. Merge everything into a single JUnit‑4 test file.
 *
 * The test file is deliberately self‑contained – it does **not** rely on any
 * external files and can be run on its own.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * List of public methods in {@link JsonSchema} (as of the supplied source):
 *
 * <pre>
 * static JsonSchema from(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)
 * JsonSchema(ValidationContext, URI, JsonNode)                                     // deprecated
 * JsonSchema(ValidationContext, String, URI, JsonNode, JsonSchema)                // deprecated
 * JsonSchema(ValidationContext, URI, JsonNode, boolean)                           // deprecated
 * JsonSchema createChildSchema(String, JsonNode)
 * URI getCurrentUri()
 * JsonNode getRefSchemaNode(String)
 * JsonSchema findLexicalRoot()
 * JsonSchema findAncestor()
 * Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)
 * Set<ValidationMessage> validate(JsonNode)
 * ValidationResult validateAndCollect(ExecutionContext, JsonNode)
 * ValidationResult validateAndCollect(JsonNode)
 * ValidationResult walk(ExecutionContext, JsonNode, boolean)
 * ValidationResult walk(JsonNode, boolean)
 * ValidationResult walkAtNode(ExecutionContext, JsonNode, JsonNode, String, boolean)
 * String toString()
 * boolean hasRequiredValidator()
 * JsonValidator getRequiredValidator()
 * boolean hasTypeValidator()
 * TypeValidator getTypeValidator()
 * void initializeValidators()
 * boolean isDynamicAnchor()
 * ExecutionContext createExecutionContext()
 * </pre>
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    // -------------------------------------------------------------------------
    //  Mocks used across many tests
    // -------------------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private JsonMetaSchema metaSchema;
    @Mock private Config config;
    @Mock private URIFactory uriFactory;
    @Mock private JsonValidator genericValidator;          // implements both JsonValidator & JsonSchemaWalker
    @Mock private JsonValidator recursiveAnchorValidator;
    @Mock private JsonValidator refValidator;
    @Mock private JsonValidator requiredValidator;
    @Mock private TypeValidator typeValidator;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidationResult validationResult;
    @Mock private JsonSchemaFactory jsonSchemaFactory;     // used only when creating child schemas

    private final JsonNodeFactory nf = JsonNodeFactory.instance;

    // -------------------------------------------------------------------------
    //  Helper methods
    // -------------------------------------------------------------------------
    /**
     * Creates a very small schema node that contains a single keyword.
     *
     * @param keyword the keyword name (e.g. "type", "required")
     * @param value   the value node for the keyword
     * @return an {@link ObjectNode} representing the schema
     */
    private ObjectNode simpleSchema(String keyword, JsonNode value) {
        ObjectNode node = nf.objectNode();
        node.set(keyword, value);
        return node;
    }

    /**
     * Configures the common mock behaviour used by most tests:
     * <ul>
     *   <li>validationContext.getMetaSchema() → metaSchema</li>
     *   <li>validationContext.getConfig() → config</li>
     *   <li>validationContext.getURIFactory() → uriFactory</li>
     *   <li>validationContext.newValidator(...) → genericValidator (or the supplied one)</li>
     *   <li>config.isCustomMessageSupported() → false (simplifies getCustomMessage)</li>
     *   <li>executionContext.getCollectorContext() → collectorContext</li>
     *   <li>collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY) → null (forces state creation)</li>
     * </ul>
     *
     * @param validator the validator to return from newValidator (null means genericValidator)
     */
    private void stubCommonBehaviour(JsonValidator validator) {
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.newValidator(anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(validator != null ? validator : genericValidator);
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(null);
    }

    /**
     * Helper that builds a {@link JsonSchema} instance using the public
     * (deprecated) constructor that takes a base URI and a schema node.
     *
     * @param schemaNode the schema definition
     * @param baseUri    the base URI (may be {@code null})
     * @return a {@link JsonSchema} ready for testing
     */
    private JsonSchema buildSchema(ObjectNode schemaNode, URI baseUri) {
        stubCommonBehaviour(null);
        // The constructor under test will invoke combineCurrentUriWithIds().
        // For normal tests we let the URI factory simply return the supplied
        // baseUri (or a derived one) – no exception is thrown.
        try {
            when(uriFactory.create(any(URI.class), anyString())).thenAnswer(invocation -> {
                URI base = invocation.getArgument(0);
                String id = invocation.getArgument(1);
                return new URI(base == null ? id : base.toString() + id);
            });
        } catch (Exception e) {
            // won't happen in the stub
        }
        return new JsonSchema(validationContext, baseUri, schemaNode);
    }

    // -------------------------------------------------------------------------
    //  1.  Basic tests for each public method
    // -------------------------------------------------------------------------

    @Test
    public void testStaticFromFactoryMethod() {
        ObjectNode schema = simpleSchema("type", nf.textNode("string"));
        JsonSchema schemaObj = JsonSchema.from(validationContext, "#", URI.create("http://example.com/"), schema, null, false);
        assertNotNull(schemaObj);
        assertEquals(URI.create("http://example.com/"), schemaObj.getCurrentUri());
    }

    @Test
    public void testDeprecatedConstructorWithBaseUri() {
        ObjectNode schema = simpleSchema("type", nf.textNode("integer"));
        JsonSchema js = new JsonSchema(validationContext, URI.create("http://example.com/base"), schema);
        assertNotNull(js);
        assertEquals(URI.create("http://example.com/base"), js.getCurrentUri());
    }

    @Test
    public void testDeprecatedConstructorWithSchemaPath() {
        ObjectNode schema = simpleSchema("type", nf.textNode("boolean"));
        JsonSchema js = new JsonSchema(validationContext, "#/foo", URI.create("http://example.com/base"), schema, null);
        assertNotNull(js);
        assertEquals(URI.create("http://example.com/base"), js.getCurrentUri());
    }

    @Test
    public void testCreateChildSchema() {
        ObjectNode parentSchema = simpleSchema("type", nf.textNode("object"));
        JsonSchema parent = buildSchema(parentSchema, URI.create("http://example.com/parent"));
        // Stub the factory method that the parent uses to create a child.
        JsonSchema child = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parent))).thenReturn(child);
        JsonSchema result = parent.createChildSchema("#/child", nf.objectNode());
        assertSame(child, result);
    }

    @Test
    public void testGetCurrentUri() {
        ObjectNode schema = simpleSchema("type", nf.textNode("null"));
        JsonSchema js = buildSchema(schema, URI.create("http://example.com/"));
        assertEquals(URI.create("http://example.com/"), js.getCurrentUri());
    }

    @Test
    public void testGetRefSchemaNode_WithJsonPointer() {
        // Build a root schema that contains a nested property.
        ObjectNode nested = nf.objectNode();
        nested.put("inner", "value");
        ObjectNode rootSchema = nf.objectNode();
        rootSchema.set("properties", nested);

        JsonSchema js = buildSchema(rootSchema, URI.create("http://example.com/"));
        JsonNode result = js.getRefSchemaNode("#/properties/inner");
        assertNotNull(result);
        assertEquals("value", result.asText());
    }

    @Test
    public void testGetRefSchemaNode_WithMissingPointer_ReturnsNull() {
        ObjectNode rootSchema = nf.objectNode(); // empty schema
        JsonSchema js = buildSchema(rootSchema, URI.create("http://example.com/"));
        JsonNode result = js.getRefSchemaNode("#/nonexistent");
        assertNull(result);
    }

    @Test
    public void testFindLexicalRoot_NoIdPresent_ReturnsSelf() {
        ObjectNode schema = simpleSchema("type", nf.textNode("string"));
        JsonSchema js = buildSchema(schema, null);
        JsonSchema lexicalRoot = js.findLexicalRoot();
        assertSame(js, lexicalRoot);
    }

    @Test
    public void testFindAncestor_RootHasNoParent_ReturnsSelf() {
        ObjectNode schema = simpleSchema("type", nf.textNode("string"));
        JsonSchema js = buildSchema(schema, null);
        JsonSchema ancestor = js.findAncestor();
        assertSame(js, ancestor);
    }

    @Test
    public void testValidate_WithNoValidators_ReturnsEmptySet() {
        // Schema without any keywords – read() will create no validators.
        ObjectNode emptySchema = nf.objectNode();
        JsonSchema js = buildSchema(emptySchema, null);
        Set<ValidationMessage> errors = js.validate(nf.objectNode());
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateAndCollect_Basic() {
        ObjectNode schema = simpleSchema("type", nf.textNode("string"));
        JsonSchema js = buildSchema(schema, null);
        // Stub the validator to return an empty error set.
        when(genericValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        ValidationResult result = js.validateAndCollect(nf.objectNode());
        assertNotNull(result);
        // The result should contain no errors.
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void testWalk_Basic() {
        ObjectNode schema = simpleSchema("type", nf.textNode("string"));
        JsonSchema js = buildSchema(schema, null);
        when(genericValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        ValidationResult result = js.walk(nf.objectNode(), false);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void testWalkAtNode_Basic() {
        ObjectNode schema = simpleSchema("type", nf.textNode("string"));
        JsonSchema js = buildSchema(schema, null);
        when(genericValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        ValidationResult result = js.walkAtNode(executionContext, nf.objectNode(), nf.objectNode(),
                "/some/path", false);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    public void testToStringRepresentation() {
        ObjectNode schema = simpleSchema("type", nf.textNode("integer"));
        JsonSchema js = buildSchema(schema, null);
        String str = js.toString();
        assertTrue(str.contains("\"#/\""));
        assertTrue(str.contains("\"integer\""));
    }

    @Test
    public void testHasRequiredValidator_FalseWhenAbsent() {
        ObjectNode schema = simpleSchema("type", nf.textNode("string"));
        JsonSchema js = buildSchema(schema, null);
        assertFalse(js.hasRequiredValidator());
    }

    @Test
    public void testHasRequiredValidator_TrueWhenPresent() {
        // Build a schema that contains "required": [...]
        ObjectNode schema = nf.objectNode();
        schema.set("required", nf.arrayNode().add("name"));
        // Stub newValidator to return a dedicated requiredValidator for the "required" keyword.
        when(validationContext.newValidator(anyString(), eq("required"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(requiredValidator);
        JsonSchema js = buildSchema(schema, null);
        assertTrue(js.hasRequiredValidator());
        assertSame(requiredValidator, js.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidator_TrueWhenPresent() {
        ObjectNode schema = simpleSchema("type", nf.textNode("object"));
        // Stub newValidator to return a TypeValidator for keyword "type".
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(typeValidator);
        JsonSchema js = buildSchema(schema, null);
        assertTrue(js.hasTypeValidator());
        assertSame(typeValidator, js.getTypeValidator());
    }

    @Test
    public void testInitializeValidators_IsIdempotent() {
        ObjectNode schema = simpleSchema("type", nf.textNode("string"));
        JsonSchema js = buildSchema(schema, null);
        // The validator returned by newValidator is the genericValidator mock.
        js.initializeValidators(); // first call – should invoke preloadJsonSchema()
        js.initializeValidators(); // second call – must NOT invoke again
        verify(genericValidator, times(1)).preloadJsonSchema();
    }

    @Test
    public void testIsDynamicAnchor_WithRecursiveAnchorTrue() {
        // Schema containing "$recursiveAnchor": true
        ObjectNode schema = nf.objectNode();
        schema.put("$recursiveAnchor", true);
        // Stub newValidator to return a validator for "$recursiveAnchor"
        when(validationContext.newValidator(anyString(), eq("$recursiveAnchor"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(recursiveAnchorValidator);
        JsonSchema js = buildSchema(schema, null);
        assertTrue(js.isDynamicAnchor());
    }

    @Test
    public void testCreateExecutionContext_UsesSupplierIfConfigured() {
        // Prepare a supplier that returns a pre‑constructed ExecutionContext.
        ExecutionContext supplied = mock(ExecutionContext.class);
        when(config.getExecutionContextSupplier()).thenReturn(() -> supplied);
        ObjectNode schema = nf.objectNode();
        JsonSchema js = buildSchema(schema, null);
        ExecutionContext ctx = js.createExecutionContext();
        assertSame(supplied, ctx);
    }

    @Test
    public void testCreateExecutionContext_DefaultWhenNoSupplier() {
        when(config.getExecutionContextSupplier()).thenReturn(null);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);
        ObjectNode schema = nf.objectNode();
        JsonSchema js = buildSchema(schema, null);
        ExecutionContext ctx = js.createExecutionContext();
        assertNotNull(ctx);
        // The collector context inside the ExecutionContext must be non‑null.
        assertNotNull(ctx.getCollectorContext());
    }

    // -------------------------------------------------------------------------
    //  2.  Edge‑case / exception tests
    // -------------------------------------------------------------------------

    @Test(expected = JsonSchemaException.class)
    public void testCombineCurrentUriWithIds_IllegalId_ThrowsException() {
        // Schema containing an "$id" that the URI factory will reject.
        ObjectNode schema = nf.objectNode();
        schema.put("$id", "!!!invalid-uri!!!");
        // Make resolveSchemaId return the bad id.
        when(validationContext.resolveSchemaId(schema)).thenReturn("!!!invalid-uri!!!");
        // Make the factory throw IllegalArgumentException.
        when(uriFactory.create(any(), anyString())).thenThrow(new IllegalArgumentException("bad uri"));
        // The constructor will invoke combineCurrentUriWithIds and should throw.
        new JsonSchema(validationContext, URI.create("http://example.com/"), schema);
    }

    @Test
    public void testGetRefSchemaNode_WithFragmentReference_UsesMetaSchema() {
        // Schema with a fragment reference e.g. "#/definitions/foo"
        ObjectNode root = nf.objectNode();
        ObjectNode defs = nf.objectNode();
        ObjectNode foo = nf.objectNode();
        foo.put("type", "string");
        defs.set("foo", foo);
        root.set("definitions", defs);
        // Make metaSchema return the node when asked for a fragment.
        when(metaSchema.getNodeByFragmentRef(eq("#/definitions/foo"), any(JsonNode.class))).thenReturn(foo);
        JsonSchema js = buildSchema(root, URI.create("http://example.com/"));
        JsonNode result = js.getRefSchemaNode("#/definitions/foo");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNode_WithUrnReference_FallsBackToHandleNullNode() {
        // Simulate a schema where the URN cannot be resolved directly.
        ObjectNode root = nf.objectNode();
        JsonSchema js = buildSchema(root, URI.create("http://example.com/"));
        // Stub the internal fetchSubSchemaNode to return null (no sub‑schema)
        JsonSchema spy = spy(js);
        doReturn(null).when(spy).fetchSubSchemaNode(any(ValidationContext.class));
        JsonNode result = spy.getRefSchemaNode("urn:nonexistent");
        assertNull(result);
    }

    @Test
    public void testValidate_WithRecursiveAnchorAndInvalidValue_ThrowsJsonSchemaException() {
        // Schema where "$recursiveAnchor" is NOT a boolean – should cause exception.
        ObjectNode schema = nf.objectNode();
        schema.put("$recursiveAnchor", "notABoolean");
        // Stub newValidator to return a validator for "$recursiveAnchor" (the actual validator is irrelevant)
        when(validationContext.newValidator(anyString(), eq("$recursiveAnchor"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(refValidator);
        try {
            new JsonSchema(validationContext, URI.create("http://example.com/"), schema);
            fail("Expected JsonSchemaException because $recursiveAnchor is not boolean");
        } catch (JsonSchemaException ex) {
            assertTrue(ex.getMessage().contains("$recursiveAnchor"));
        }
    }

    @Test
    public void testRead_RefOnlyWhenOlderDialect_ClearsOtherValidators() {
        // Simulate an older draft (dialect version < 2019-09)
        when(validationContext.activeDialect()).thenReturn(Optional.of(VersionFlag.V4));
        // Schema containing "$ref" and another keyword.
        ObjectNode schema = nf.objectNode();
        schema.put("$ref", "#/definitions/bar");
        schema.put("type", "string");
        // $ref validator
        when(validationContext.newValidator(anyString(), eq("$ref"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(refValidator);
        // other validator (type)
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), any()))
                .thenReturn(typeValidator);
        JsonSchema js = buildSchema(schema, URI.create("http://example.com/"));
        // The internal validators map should contain ONLY the $ref entry.
        Map<String, JsonValidator> map = js.getValidators(); // private, accessed via reflection
        assertEquals(1, map.size());
        assertTrue(map.keySet().iterator().next().endsWith("/$ref"));
    }

    // -------------------------------------------------------------------------
    //  3.  Utility to access the private getValidators() for the above test
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private Map<String, JsonValidator> getValidators(JsonSchema schema) throws Exception {
        java.lang.reflect.Method m = JsonSchema.class.getDeclaredMethod("getValidators");
        m.setAccessible(true);
        return (Map<String, JsonValidator>) m.invoke(schema);
    }

    // -------------------------------------------------------------------------
    //  4.  End of test class
    // -------------------------------------------------------------------------
}
```
**###Test END##**