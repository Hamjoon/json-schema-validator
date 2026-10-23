**1.  Public methods of `JsonSchema` (including signatures)**  

| # | Visibility | Return type | Method signature |
|---|------------|------------|------------------|
| 1 | `public static` | `JsonSchema` | `from(ValidationContext validationContext, String schemaPath, URI currentUri, JsonNode schemaNode, JsonSchema parent, boolean suppressSubSchemaRetrieval)` |
| 2 | `public` (deprecated) | `JsonSchema` | `JsonSchema(ValidationContext validationContext, URI baseUri, JsonNode schemaNode)` |
| 3 | `public` (deprecated) | `JsonSchema` | `JsonSchema(ValidationContext validationContext, String schemaPath, URI currentUri, JsonNode schemaNode, JsonSchema parent)` |
| 4 | `public` (deprecated) | `JsonSchema` | `JsonSchema(ValidationContext validationContext, URI baseUri, JsonNode schemaNode, boolean suppressSubSchemaRetrieval)` |
| 5 | `public` | `JsonSchema` | `createChildSchema(String schemaPath, JsonNode schemaNode)` |
| 6 | `public` | `URI` | `getCurrentUri()` |
| 7 | `public` | `JsonNode` | `getRefSchemaNode(String ref)` |
| 8 | `public` | `JsonSchema` | `findAncestor()` |
| 9 | `public` | `Set<ValidationMessage>` | `validate(ExecutionContext executionContext, JsonNode jsonNode, JsonNode rootNode, String at)` |
|10 | `public` | `Set<ValidationMessage>` | `validate(JsonNode rootNode)` |
|11 | `public` | `ValidationResult` | `validateAndCollect(ExecutionContext executionContext, JsonNode node)` |
|12 | `public` | `ValidationResult` | `validateAndCollect(JsonNode node)` |
|13 | `public` | `ValidationResult` | `walk(ExecutionContext executionContext, JsonNode node, boolean shouldValidateSchema)` |
|14 | `public` | `ValidationResult` | `walk(JsonNode node, boolean shouldValidateSchema)` |
|15 | `public` | `ValidationResult` | `walkAtNode(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema)` |
|16 | `public` | `Set<ValidationMessage>` | `walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema)` |
|17 | `public` | `String` | `toString()` |
|18 | `public` | `boolean` | `hasRequiredValidator()` |
|19 | `public` | `JsonValidator` | `getRequiredValidator()` |
|20 | `public` | `boolean` | `hasTypeValidator()` |
|21 | `public` | `TypeValidator` | `getTypeValidator()` |
|22 | `public` | `void` | `initializeValidators()` |
|23 | `public` | `boolean` | `isDynamicAnchor()` |
|24 | `public` | `ExecutionContext` | `createExecutionContext()` |

---

**2.  Potential edge‑cases / exceptional situations**

| Method | Edge case / Exception | Reason |
|--------|-----------------------|--------|
| `combineCurrentUriWithIds` (private, exercised via constructor) | `id == null` | Should leave the original `currentUri` unchanged |
| `combineCurrentUriWithIds` | `id` is a fragment (`#something`) **and** `currentUri == null` | Returns `null` (no context) |
| `combineCurrentUriWithIds` | `validationContext.getURIFactory().create(..)` throws `IllegalArgumentException` | Should be wrapped in `JsonSchemaException` |
| `getRefSchemaNode` | `ref` is a JSON‑Pointer (`#/a/b`) that points to an existing node | Returns that node |
| `getRefSchemaNode` | `ref` is a JSON‑Pointer that points to a *missing* node | Falls back to `handleNullNode` → may return `null` |
| `getRefSchemaNode` | `ref` is a fragment (`#myFragment`) | Delegates to `metaSchema.getNodeByFragmentRef` |
| `initializeValidators` | Called twice – `preloadJsonSchema` must be executed only once | `validatorsLoaded` flag prevents double loading |
| `isDynamicAnchor` | Schema contains `$recursiveAnchor : true` | Returns `true` |
| `createExecutionContext` | `validationContext.getConfig().getExecutionContextSupplier()` returns a custom supplier | The supplied `ExecutionContext` must be used |
| `toString` | Schema path or node contains special characters | Should be correctly escaped by `JsonNode.toString()` (tested indirectly) |
| `findAncestor` | Schema has no parent (root) | Returns itself |
| `createChildSchema` | ValidationContext creates a new schema – we verify delegation | Ensure `validationContext.newSchema` is called with the right arguments |

---

**3.  JUnit 4 test file (`JsonSchemaTest.java`)**

```java
###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link JsonSchema}.  The tests use Mockito to isolate the
 * class from the rest of the library and focus on the public API, edge cases
 * and exception handling.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaTest {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private SchemaValidatorsConfig config;

    @Mock
    private JsonValidator validator1, validator2;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    private ObjectMapper mapper = new ObjectMapper();

    private JsonNode simpleSchemaNode;

    @Before
    public void setUp() throws Exception {
        // a minimal schema node: {"type":"object"}
        simpleSchemaNode = mapper.readTree("{\"type\":\"object\"}");

        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isCustomMessageSupported()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(config.doLoadCollectors()).thenReturn(false);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
    }

    /* --------------------------------------------------------------------- */
    /* 1.  createChildSchema – delegating to ValidationContext                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCreateChildSchemaDelegatesToValidationContext() {
        JsonSchema parent = JsonSchema.from(validationContext, "#", null, simpleSchemaNode, null, false);
        JsonSchema child = parent.createChildSchema("#/properties/name", simpleSchemaNode);

        // ValidationContext is expected to be used to create the child schema.
        // The static factory method `newSchema` is called inside ValidationContext;
        // we verify that the method was invoked (using a spy on the mock).
        verify(validationContext, times(1)).newSchema(eq("#/properties/name"), eq(simpleSchemaNode), eq(parent));
        assertNotNull(child);
    }

    /* --------------------------------------------------------------------- */
    /* 2.  getCurrentUri – normal combination of base URI and $id               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetCurrentUriCombinedWithId() throws Exception {
        // schema node contains an id keyword (default is "$id")
        ObjectNode schemaWithId = mapper.createObjectNode();
        schemaWithId.put("$id", "http://example.com/base");
        JsonSchema schema = JsonSchema.from(validationContext, "#", new URI("http://example.com/parent"), schemaWithId, null, false);
        assertEquals(new URI("http://example.com/base"), schema.getCurrentUri());
    }

    /* --------------------------------------------------------------------- */
    /* 3.  combineCurrentUriWithIds – fragment without context (returns null) */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCombineCurrentUriWithIdsFragmentNoContextReturnsNull() throws Exception {
        // Build a schema node with a fragment id "#frag"
        ObjectNode schema = mapper.createObjectNode();
        schema.put("$id", "#frag");

        // currentUri is null – this triggers the special case.
        JsonSchema schemaObj = JsonSchema.from(validationContext, "#", null, schema, null, false);

        // Use reflection to call the private method.
        Method m = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
        m.setAccessible(true);
        URI result = (URI) m.invoke(schemaObj, null, schema);
        assertNull(result);
    }

    /* --------------------------------------------------------------------- */
    /* 4.  combineCurrentUriWithIds – illegal URI throws JsonSchemaException   */
    /* --------------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void testCombineCurrentUriWithIdsIllegalUriThrowsException() throws Exception {
        // Mock the URIFactory to throw IllegalArgumentException
        URIFactory uriFactory = mock(URIFactory.class);
        when(uriFactory.create(any(URI.class), anyString())).thenThrow(new IllegalArgumentException("bad uri"));
        when(validationContext.getURIFactory()).thenReturn(uriFactory);

        // schema with a normal id
        ObjectNode schema = mapper.createObjectNode();
        schema.put("$id", "bad uri");

        JsonSchema schemaObj = JsonSchema.from(validationContext, "#", new URI("http://example.com/base"), schema, null, false);

        // invoke private method via reflection – it will throw JsonSchemaException
        Method m = JsonSchema.class.getDeclaredMethod("combineCurrentUriWithIds", URI.class, JsonNode.class);
        m.setAccessible(true);
        m.invoke(schemaObj, new URI("http://example.com/base"), schema);
    }

    /* --------------------------------------------------------------------- */
    /* 5.  getRefSchemaNode – JSON Pointer that resolves to a node            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetRefSchemaNodeJsonPointerFound() throws Exception {
        // root schema node with a nested property
        JsonNode root = mapper.readTree("{\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, root, null, false);

        JsonNode result = schema.getRefSchemaNode("#/properties/name");
        assertNotNull(result);
        assertEquals("string", result.get("type").asText());
    }

    /* --------------------------------------------------------------------- */
    /* 6.  getRefSchemaNode – JSON Pointer that points to a missing node       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetRefSchemaNodeJsonPointerMissingReturnsNull() throws Exception {
        JsonNode root = mapper.readTree("{\"properties\":{\"age\":{\"type\":\"integer\"}}}");
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, root, null, false);

        JsonNode result = schema.getRefSchemaNode("#/properties/name"); // name does NOT exist
        assertNull(result);
    }

    /* --------------------------------------------------------------------- */
    /* 7.  getRefSchemaNode – fragment reference delegated to metaSchema      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetRefSchemaNodeFragmentDelegatesToMetaSchema() {
        // Mock metaSchema to return a dummy node for fragment "#myFragment"
        JsonNode fragmentNode = mapper.createObjectNode().put("type", "boolean");
        when(metaSchema.getNodeByFragmentRef(eq("#myFragment"), any(JsonNode.class))).thenReturn(fragmentNode);
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, simpleSchemaNode, null, false);
        JsonNode result = schema.getRefSchemaNode("#myFragment");
        assertSame(fragmentNode, result);
    }

    /* --------------------------------------------------------------------- */
    /* 8.  findAncestor – root schema returns itself                           */
    /* --------------------------------------------------------------------- */
    @Test
    public void testFindAncestorReturnsSelfWhenRoot() {
        JsonSchema root = JsonSchema.from(validationContext, "#", null, simpleSchemaNode, null, false);
        assertSame(root, root.findAncestor());
    }

    /* --------------------------------------------------------------------- */
    /* 9.  initializeValidators – preloadJsonSchema called only once         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testInitializeValidatorsCallsPreloadOnce() throws Exception {
        // Prepare a schema that will produce two validators (type + required)
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("type", "object");
        schemaNode.putArray("required").add("name");

        // Mock ValidationContext to return two different validators
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(validator1);
        when(validationContext.newValidator(anyString(), eq("required"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(validator2);

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        // first call – validators are loaded
        schema.initializeValidators();
        // second call – should be a no‑op
        schema.initializeValidators();

        // each validator's preloadJsonSchema must have been called exactly once
        verify(validator1, times(1)).preloadJsonSchema();
        verify(validator2, times(1)).preloadJsonSchema();
    }

    /* --------------------------------------------------------------------- */
    /* 10. isDynamicAnchor – $recursiveAnchor true makes it true               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testIsDynamicAnchorTrueWhenRecursiveAnchorTrue() {
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("$recursiveAnchor", true);

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        assertTrue(schema.isDynamicAnchor());
    }

    /* --------------------------------------------------------------------- */
    /* 11. toString – verifies format                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToStringFormatting() {
        JsonSchema schema = JsonSchema.from(validationContext, "#/properties/name", null, simpleSchemaNode, null, false);
        String s = schema.toString();
        assertTrue(s.startsWith("\"#/properties/name\""));
        assertTrue(s.endsWith(simpleSchemaNode.toString()));
    }

    /* --------------------------------------------------------------------- */
    /* 12. hasRequiredValidator / getRequiredValidator                           */
    /* --------------------------------------------------------------------- */
    @Test
    public void testHasRequiredValidatorAndGetter() {
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.putArray("required").add("id");

        when(validationContext.newValidator(anyString(), eq("required"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(validator1);

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        assertTrue(schema.hasRequiredValidator());
        assertSame(validator1, schema.getRequiredValidator());
    }

    /* --------------------------------------------------------------------- */
    /* 13. hasTypeValidator / getTypeValidator                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testHasTypeValidatorAndGetter() {
        ObjectNode schemaNode = mapper.createObjectNode();
        schemaNode.put("type", "string");

        TypeValidator typeValidator = mock(TypeValidator.class);
        when(validationContext.newValidator(anyString(), eq("type"), any(JsonNode.class), any(JsonSchema.class), isNull()))
                .thenReturn(typeValidator);

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, schemaNode, null, false);
        assertTrue(schema.hasTypeValidator());
        assertSame(typeValidator, schema.getTypeValidator());
    }

    /* --------------------------------------------------------------------- */
    /* 14. createExecutionContext – uses supplier when configured               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCreateExecutionContextUsesSupplierIfProvided() {
        ExecutionContext supplied = mock(ExecutionContext.class);
        when(config.getExecutionContextSupplier()).thenReturn(() -> supplied);

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, simpleSchemaNode, null, false);
        ExecutionContext ctx = schema.createExecutionContext();
        assertSame(supplied, ctx);
    }

    /* --------------------------------------------------------------------- */
    /* 15. createExecutionContext – default supplier creates CollectorContext   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCreateExecutionContextDefaultCreatesCollectorContext() {
        when(config.getExecutionContextSupplier()).thenReturn(null);
        when(config.isUnevaluatedItemsAnalysisDisabled()).thenReturn(false);
        when(config.isUnevaluatedPropertiesAnalysisDisabled()).thenReturn(false);

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, simpleSchemaNode, null, false);
        ExecutionContext ctx = schema.createExecutionContext();

        assertNotNull(ctx);
        assertNotNull(ctx.getCollectorContext());
    }

    /* --------------------------------------------------------------------- */
    /* 16. validate(JsonNode) – basic delegation to validate(Execution…)    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testValidateRootNodeDelegatesToValidateWithExecutionContext() {
        // Stub the internal validate(ExecutionContext,…) to return an empty set
        JsonSchema schema = spy(JsonSchema.from(validationContext, "#", null, simpleSchemaNode, null, false));
        doReturn(Collections.emptySet()).when(schema).validate(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), anyString());

        JsonNode root = mapper.createObjectNode();
        Set<ValidationMessage> result = schema.validate(root);
        assertTrue(result.isEmpty());

        verify(schema, times(1)).validate(any(ExecutionContext.class), eq(root), eq(root), eq(""));
    }

    /* --------------------------------------------------------------------- */
    /* 17. walk(ExecutionContext, JsonNode, boolean) – no walkers, returns empty */
    /* --------------------------------------------------------------------- */
    @Test
    public void testWalkReturnsEmptyResultWhenNoWalkers() {
        JsonSchema schema = JsonSchema.from(validationContext, "#", null, simpleSchemaNode, null, false);
        ValidationResult result = schema.walk(executionContext, simpleSchemaNode, true);
        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 18. getRefSchemaNode – URN reference (treated like fragment)            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetRefSchemaNodeUrnReferenceDelegatesToMetaSchema() {
        JsonNode fragmentNode = mapper.createObjectNode().put("type", "number");
        when(metaSchema.getNodeByFragmentRef(eq("urn:example:myFragment"), any(JsonNode.class))).thenReturn(fragmentNode);
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);

        JsonSchema schema = JsonSchema.from(validationContext, "#", null, simpleSchemaNode, null, false);
        JsonNode result = schema.getRefSchemaNode("urn:example:myFragment");
        assertSame(fragmentNode, result);
    }

}
###Test END##
```