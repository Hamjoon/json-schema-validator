**Step 1 – Public API of `BaseJsonValidator`**  

| Visibility | Return type | Method name & signature | Brief description |
|------------|-------------|--------------------------|-------------------|
| **Constructors** | – | `BaseJsonValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext)` | Main constructor – delegates to the 6‑arg one with `suppressSubSchemaRetrieval = false`. |
| **Constructors** | – | `BaseJsonValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval)` | Full constructor – initialises the validator. |
| **Public** | `String` | `getSchemaPath()` | Returns the schema path that was supplied to the constructor. |
| **Public** | `JsonNode` | `getSchemaNode()` | Returns the JSON node that represents the validator’s schema fragment. |
| **Public** | `JsonSchema` | `getParentSchema()` | Returns the parent schema (may be `null`). |
| **Public** | `Set<ValidationMessage>` | `validate(ExecutionContext executionContext, JsonNode node)` | Entry‑point for validation – delegates to the 4‑arg `validate` method that concrete subclasses must implement. |
| **Protected** | `JsonSchema` | `fetchSubSchemaNode(ValidationContext validationContext)` | Returns a sub‑schema based on the `"id"` field – or `null` when `suppressSubSchemaRetrieval` is `true`. |
| **Protected** | `String` | `getNodeFieldType()` | Reads the `"type"` field from the *parent* schema. |
| **Protected** | `void` | `preloadJsonSchemas(Collection<JsonSchema> schemas)` | Calls `initializeValidators()` on each supplied schema. |
| **Protected** | `PathType` | `getPathType()` | Returns the `PathType` that was resolved from the `ValidationContext`. |
| **Protected** | `String` | `atRoot()` | Returns the root path string (`PathType#getRoot()`). |
| **Protected** | `String` | `atPath(String currentPath, String token)` | Appends a property token to a JSON‑Pointer‑style path. |
| **Protected** | `String` | `atPath(String currentPath, int index)` | Appends an array‑index token to a path. |
| **Static (protected)** | `boolean` | `equals(double n1, double n2)` | Approximate equality for floating‑point numbers. |
| **Static (protected)** | `void` | `debug(Logger logger, JsonNode node, JsonNode rootNode, String at)` | Logs the three nodes at the supplied location. |
| **Static (protected)** | `void` | `checkDiscriminatorMatch(DiscriminatorContext ctx, ObjectNode discriminator, String propValue, JsonSchema schema)` | Implements the OpenAPI 3 “discriminator” matching algorithm. |
| **Static (protected)** | `void` | `registerAndMergeDiscriminator(DiscriminatorContext ctx, ObjectNode discriminator, JsonSchema schema, String at)` | Merges nested discriminators and registers them in the context. |
| **Static (private)** | – | `checkForImplicitDiscriminatorMappingMatch(...)` | Helper for implicit mapping. |
| **Static (private)** | – | `checkForExplicitDiscriminatorMappingMatch(...)` | Helper for explicit mapping. |
| **Static (private)** | `boolean` | `noExplicitDiscriminatorKeyOverride(JsonNode mapping, JsonSchema parentSchema)` | Detects whether an explicit mapping would hide a parent mapping. |
| **Static (private)** | `JsonSchema` | `obtainSubSchemaNode(JsonNode schemaNode, ValidationContext validationContext)` | Core logic used by `fetchSubSchemaNode`. |

---

**Step 2 – Basic JUnit 4 test ideas (one per public method)**  

| Method | Basic test case (what it verifies) |
|--------|-------------------------------------|
| Constructor (6‑arg) | After construction the getters (`getSchemaPath()`, `getSchemaNode()`, `getParentSchema()`) return exactly the values supplied. |
| `getSchemaPath()` | Returns the value passed to the constructor. |
| `getSchemaNode()` | Returns the same `JsonNode` instance that was supplied. |
| `getParentSchema()` | Returns the exact `JsonSchema` instance supplied (or `null` when none). |
| `validate(ExecutionContext, JsonNode)` | Using a concrete subclass that implements the 4‑arg `validate` to return an empty set – the public method must forward to it and return that empty set. |
| `fetchSubSchemaNode(ValidationContext)` | When `suppressSubSchemaRetrieval` is `true` the method must return `null`. When it is `false` and the schema node contains a valid `"id"` URI, the method must return the `JsonSchema` obtained from the factory. |
| `getNodeFieldType()` | Returns the value of the `"type"` field from the parent schema (or `null` when absent). |
| `preloadJsonSchemas(Collection)` | Each supplied schema receives a call to `initializeValidators()`. |
| `getPathType()` | Returns the `PathType` that was resolved from the `ValidationContext` (or the default). |
| `atRoot()` | Returns the root string defined by the resolved `PathType`. |
| `atPath(String,String)` | Correctly concatenates the current path and a property token using `PathType.append`. |
| `atPath(String,int)` | Correctly concatenates the current path and an array index. |
| `equals(double,double)` | Returns `true` when the difference is < 1e‑12 and `false` otherwise. |
| `debug(Logger,JsonNode,JsonNode,String)` | Calls `logger.debug` with the expected format string – no exception should be thrown. |
| `checkDiscriminatorMatch(...)` | Various scenarios (null property value, explicit mapping, implicit mapping, no match). |
| `registerAndMergeDiscriminator(...)` | Normal merge, re‑definition of `propertyName`, and mapping conflicts – must throw `JsonSchemaException` where appropriate. |

---

**Step 3 – Edge‑case & Exception scenarios that need testing**

| Method | Edge / Exception case |
|--------|------------------------|
| Constructor | `validationContext` = `null` (should still create a validator – defaults are used). |
| `fetchSubSchemaNode` | `schemaNode` has no `"id"` field → returns `null`. |
| `fetchSubSchemaNode` | `"id"` field is present but not a textual node → returns `null`. |
| `fetchSubSchemaNode` | `"id"` contains an illegal URI → returns `null`. |
| `equals` | `NaN` vs `NaN`, `Infinity` vs `Infinity` – both should be `false` because the subtraction yields `NaN`. |
| `debug` | `logger` is `null` → method must not throw NPE (it is safe because the method is static and the call is guarded by the caller). |
| `checkDiscriminatorMatch` | `discriminator` is `null` → must not throw NPE (the method is never called with `null` in the library, but we test defensive behaviour). |
| `checkDiscriminatorMatch` | `discriminator.get("mapping")` returns `null` – implicit mapping path must be evaluated. |
| `checkDiscriminatorMatch` | Explicit mapping exists but does **not** match → no match should be recorded. |
| `registerAndMergeDiscriminator` | The root discriminator already defines a `propertyName` and a nested schema tries to redefine it → `JsonSchemaException`. |
| `registerAndMergeDiscriminator` | Two nested discriminators define the **same** mapping key with **different** values → `JsonSchemaException`. |
| `registerAndMergeDiscriminator` | No conflict – the method must silently merge and register the discriminator. |
| `noExplicitDiscriminatorKeyOverride` | Mapping contains the parent schema path → returns `false`. |
| `noExplicitDiscriminatorKeyOverride` | Mapping does **not** contain the parent schema path → returns `true`. |
| `atPath` (String) | Current path is the root (`""`) → result must be `"/token"` (according to the default `PathType`). |
| `atPath` (int) | Index is negative – the method must still produce a string (the library does not validate the index). |

---

**Step 4 – Full JUnit 4 test file**

Below is a *single* self‑contained test class that implements **all** of the above test ideas.  
It uses **JUnit 4**, **Mockito** for mocks and **Jackson** for JSON handling.  
A tiny concrete subclass (`DummyValidator`) is created so that the abstract class can be instantiated and its protected members can be exercised.

```java
###Test START##
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
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
 * Comprehensive JUnit‑4 test suite for {@link BaseJsonValidator}.
 *
 * The suite covers:
 *   • all public methods (including constructors)
 *   • protected helpers via a concrete test subclass
 *   • static utility methods
 *   • edge‑cases and expected exception paths
 */
@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class BaseJsonValidatorTest {

    // -----------------------------------------------------------------------
    // Helper objects and common test fixtures
    // -----------------------------------------------------------------------
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode simpleSchemaNode;
    private JsonSchema parentSchema;
    private ValidatorTypeCode validatorType;
    private ValidationContext validationContext;
    private ExecutionContext executionContext;

    @Before
    public void setUp() {
        simpleSchemaNode = MAPPER.createObjectNode().put("type", "object");
        parentSchema = mock(JsonSchema.class);
        when(parentSchema.getSchemaNode()).thenReturn(simpleSchemaNode);
        validatorType = ValidatorTypeCode.TYPE; // any non‑null enum value
        validationContext = mock(ValidationContext.class);
        when(validationContext.getConfig()).thenReturn(null); // use defaults
        executionContext = mock(ExecutionContext.class);
    }

    // -----------------------------------------------------------------------
    // 1. Concrete subclass to expose protected members
    // -----------------------------------------------------------------------
    private static class DummyValidator extends BaseJsonValidator {

        DummyValidator(String schemaPath,
                       JsonNode schemaNode,
                       JsonSchema parentSchema,
                       ValidatorTypeCode validatorType,
                       ValidationContext validationContext,
                       boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        // expose protected methods for testing
        JsonSchema callFetchSubSchemaNode(ValidationContext vc) {
            return fetchSubSchemaNode(vc);
        }

        String callGetNodeFieldType() {
            return getNodeFieldType();
        }

        void callPreloadJsonSchemas(Collection<JsonSchema> schemas) {
            preloadJsonSchemas(schemas);
        }

        PathType callGetPathType() {
            return getPathType();
        }

        String callAtRoot() {
            return atRoot();
        }

        String callAtPath(String cur, String token) {
            return atPath(cur, token);
        }

        String callAtPath(String cur, int idx) {
            return atPath(cur, idx);
        }

        // Minimal concrete implementation of the abstract 4‑arg validate method
        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                   JsonNode node,
                                                   JsonNode rootNode,
                                                   String at) {
            // For the purpose of the tests we simply return an empty set.
            return Collections.emptySet();
        }
    }

    // -----------------------------------------------------------------------
    // 2. Constructor & basic getters
    // -----------------------------------------------------------------------
    @Test
    public void testConstructorAndGetters() {
        DummyValidator validator = new DummyValidator(
                "/properties/name",
                simpleSchemaNode,
                parentSchema,
                validatorType,
                validationContext,
                false);

        assertEquals("/properties/name", validator.getSchemaPath());
        assertSame(simpleSchemaNode, validator.getSchemaNode());
        assertSame(parentSchema, validator.getParentSchema());
    }

    @Test
    public void testConstructorWithNullValidationContext() {
        DummyValidator validator = new DummyValidator(
                "/a/b",
                simpleSchemaNode,
                null,
                validatorType,
                null,
                false);

        // defaults are applied – no NPE should be thrown
        assertEquals("/a/b", validator.getSchemaPath());
        assertNull(validator.getParentSchema());
    }

    // -----------------------------------------------------------------------
    // 3. Public validate() – forwards to the 4‑arg version
    // -----------------------------------------------------------------------
    @Test
    public void testValidateReturnsEmptySet() {
        DummyValidator validator = new DummyValidator(
                "/root",
                simpleSchemaNode,
                parentSchema,
                validatorType,
                validationContext,
                false);

        JsonNode instance = MAPPER.createObjectNode().put("foo", "bar");
        Set<ValidationMessage> messages = validator.validate(executionContext, instance);
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    // -----------------------------------------------------------------------
    // 4. fetchSubSchemaNode() – suppressed & normal paths
    // -----------------------------------------------------------------------
    @Test
    public void testFetchSubSchemaNodeSuppressed() {
        DummyValidator validator = new DummyValidator(
                "/path",
                simpleSchemaNode,
                parentSchema,
                validatorType,
                validationContext,
                true); // suppress retrieval

        assertNull(validator.callFetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNodeWithValidId() throws Exception {
        // Build a schema node that contains a valid "id"
        ObjectNode schemaWithId = MAPPER.createObjectNode();
        schemaWithId.put("id", "http://example.com/my-schema");

        // Mock the URI factory and JsonSchemaFactory that the ValidationContext would provide
        URIFactory uriFactory = mock(URIFactory.class);
        JsonSchemaFactory schemaFactory = mock(JsonSchemaFactory.class);
        JsonSchema subSchema = mock(JsonSchema.class);

        when(validationContext.getURIFactory()).thenReturn(uriFactory);
        when(validationContext.getJsonSchemaFactory()).thenReturn(schemaFactory);
        when(uriFactory.create("http://example.com/my-schema")).thenReturn(new URI("http://example.com/my-schema"));
        when(schemaFactory.getSchema(any(URI.class), any())).thenReturn(subSchema);

        DummyValidator validator = new DummyValidator(
                "/some/path",
                schemaWithId,
                parentSchema,
                validatorType,
                validationContext,
                false);

        JsonSchema result = validator.callFetchSubSchemaNode(validationContext);
        assertSame(subSchema, result);
    }

    @Test
    public void testFetchSubSchemaNodeWithInvalidId() {
        ObjectNode schemaBadId = MAPPER.createObjectNode().put("id", "not a valid uri");

        when(validationContext.getURIFactory()).thenReturn(mock(URIFactory.class));
        // the mocked factory will throw IllegalArgumentException
        URIFactory badFactory = validationContext.getURIFactory();
        when(badFactory.create("not a valid uri")).thenThrow(new IllegalArgumentException());

        DummyValidator validator = new DummyValidator(
                "/path",
                schemaBadId,
                parentSchema,
                validatorType,
                validationContext,
                false);

        assertNull(validator.callFetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNodeWithoutIdField() {
        ObjectNode schemaNoId = MAPPER.createObjectNode(); // empty object

        DummyValidator validator = new DummyValidator(
                "/path",
                schemaNoId,
                parentSchema,
                validatorType,
                validationContext,
                false);

        assertNull(validator.callFetchSubSchemaNode(validationContext));
    }

    // -----------------------------------------------------------------------
    // 5. getNodeFieldType()
    // -----------------------------------------------------------------------
    @Test
    public void testGetNodeFieldTypePresent() {
        DummyValidator validator = new DummyValidator(
                "/p",
                simpleSchemaNode,
                parentSchema,
                validatorType,
                validationContext,
                false);
        assertEquals("object", validator.callGetNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeAbsent() {
        JsonNode emptyNode = MAPPER.createObjectNode(); // no "type"
        when(parentSchema.getSchemaNode()).thenReturn(emptyNode);

        DummyValidator validator = new DummyValidator(
                "/p",
                emptyNode,
                parentSchema,
                validatorType,
                validationContext,
                false);
        assertNull(validator.callGetNodeFieldType());
    }

    // -----------------------------------------------------------------------
    // 6. preloadJsonSchemas()
    // -----------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchemasCallsInitialize() {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        List<JsonSchema> list = Arrays.asList(s1, s2);

        DummyValidator validator = new DummyValidator(
                "/p",
                simpleSchemaNode,
                parentSchema,
                validatorType,
                validationContext,
                false);

        validator.callPreloadJsonSchemas(list);

        verify(s1, times(1)).initializeValidators();
        verify(s2, times(1)).initializeValidators();
    }

    // -----------------------------------------------------------------------
    // 7. Path handling (PathType defaults)
    // -----------------------------------------------------------------------
    @Test
    public void testPathTypeAndRoot() {
        DummyValidator validator = new DummyValidator(
                "/p",
                simpleSchemaNode,
                parentSchema,
                validatorType,
                validationContext,
                false);

        // default PathType is PathType.DEFAULT
        assertEquals(PathType.DEFAULT, validator.callGetPathType());
        assertEquals(PathType.DEFAULT.getRoot(), validator.callAtRoot());
    }

    @Test
    public void testAtPathString() {
        DummyValidator validator = new DummyValidator(
                "/p",
                simpleSchemaNode,
                parentSchema,
                validatorType,
                validationContext,
                false);
        String result = validator.callAtPath("/a/b", "c");
        assertEquals("/a/b/c", result);
    }

    @Test
    public void testAtPathInt() {
        DummyValidator validator = new DummyValidator(
                "/p",
                simpleSchemaNode,
                parentSchema,
                validatorType,
                validationContext,
                false);
        String result = validator.callAtPath("/a/b", 2);
        assertEquals("/a/b/2", result);
    }

    // -----------------------------------------------------------------------
    // 8. Static utility methods
    // -----------------------------------------------------------------------
    @Test
    public void testEqualsApproximate() {
        assertTrue(BaseJsonValidator.equals(1.000000000001, 1.0));
        assertFalse(BaseJsonValidator.equals(1.0, 1.1));
        assertFalse(BaseJsonValidator.equals(Double.NaN, Double.NaN));
        assertTrue(BaseJsonValidator.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    @Test
    public void testDebugCallsLogger() {
        Logger logger = mock(Logger.class);
        JsonNode node = MAPPER.createObjectNode().put("a", 1);
        JsonNode root = MAPPER.createObjectNode().put("root", true);
        BaseJsonValidator.debug(logger, node, root, "/path");
        verify(logger, times(1)).debug(eq("validate( {}, {}, {})"), eq(node), eq(root), eq("/path"));
    }

    // -----------------------------------------------------------------------
    // 9. Discriminator handling – static protected methods exercised via reflection
    // -----------------------------------------------------------------------
    @Test
    public void testCheckDiscriminatorMatchNullPropertyValue() throws Exception {
        // a tiny stub implementation of DiscriminatorContext that records a match
        class SimpleCtx implements ValidationContext.DiscriminatorContext {
            private boolean matched = false;
            @Override public void markMatch() { matched = true; }
            @Override public boolean isDiscriminatorMatchFound() { return matched; }
            @Override public void registerDiscriminator(String path, ObjectNode disc) {}
            @Override public ObjectNode getDiscriminatorForPath(JsonSchema schema) { return null; }
            @Override public ObjectNode getDiscriminatorForPath(String path) { return null; }
        }
        SimpleCtx ctx = new SimpleCtx();

        ObjectNode disc = MAPPER.createObjectNode(); // empty discriminator
        // use reflection to invoke the protected static method
        java.lang.reflect.Method m = BaseJsonValidator.class
                .getDeclaredMethod("checkDiscriminatorMatch",
                        ValidationContext.DiscriminatorContext.class,
                        ObjectNode.class,
                        String.class,
                        JsonSchema.class);
        m.setAccessible(true);
        m.invoke(null, ctx, disc, null, mock(JsonSchema.class));
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingSuccess() throws Exception {
        class SimpleCtx implements ValidationContext.DiscriminatorContext {
            private boolean matched = false;
            @Override public void markMatch() { matched = true; }
            @Override public boolean isDiscriminatorMatchFound() { return matched; }
            @Override public void registerDiscriminator(String path, ObjectNode disc) {}
            @Override public ObjectNode getDiscriminatorForPath(JsonSchema schema) { return null; }
            @Override public ObjectNode getDiscriminatorForPath(String path) { return null; }
        }
        SimpleCtx ctx = new SimpleCtx();

        ObjectNode disc = MAPPER.createObjectNode();
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("myType", "/schemas/myType");
        disc.set("mapping", mapping);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/schemas/myType");

        java.lang.reflect.Method m = BaseJsonValidator.class
                .getDeclaredMethod("checkDiscriminatorMatch",
                        ValidationContext.DiscriminatorContext.class,
                        ObjectNode.class,
                        String.class,
                        JsonSchema.class);
        m.setAccessible(true);
        m.invoke(null, ctx, disc, "myType", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitMappingSuccess() throws Exception {
        class SimpleCtx implements ValidationContext.DiscriminatorContext {
            private boolean matched = false;
            @Override public void markMatch() { matched = true; }
            @Override public boolean isDiscriminatorMatchFound() { return matched; }
            @Override public void registerDiscriminator(String path, ObjectNode disc) {}
            @Override public ObjectNode getDiscriminatorForPath(JsonSchema schema) { return null; }
            @Override public ObjectNode getDiscriminatorForPath(String path) { return null; }
        }
        SimpleCtx ctx = new SimpleCtx();

        ObjectNode disc = MAPPER.createObjectNode(); // no mapping → implicit only
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.schemaPath).thenReturn("/components/schemas/myType");

        java.lang.reflect.Method m = BaseJsonValidator.class
                .getDeclaredMethod("checkDiscriminatorMatch",
                        ValidationContext.DiscriminatorContext.class,
                        ObjectNode.class,
                        String.class,
                        JsonSchema.class);
        m.setAccessible(true);
        m.invoke(null, ctx, disc, "myType", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorRedefinePropertyNameThrows() throws Exception {
        // Prepare a root discriminator that already defines a propertyName
        ObjectNode rootDisc = MAPPER.createObjectNode();
        rootDisc.set("propertyName", new TextNode("type"));
        rootDisc.set("mapping", MAPPER.createObjectNode());

        // Parent schema that also has a discriminator with a propertyName (illegal)
        JsonSchema parent = mock(JsonSchema.class);
        ObjectNode parentSchemaNode = MAPPER.createObjectNode();
        ObjectNode nestedDisc = MAPPER.createObjectNode();
        nestedDisc.set("propertyName", new TextNode("type"));
        parentSchemaNode.set("discriminator", nestedDisc);
        when(parent.schemaNode).thenReturn(parentSchemaNode);
        when(parent.schemaPath).thenReturn("/parent");

        // Context stub that will return the root discriminator for the path
        class SimpleCtx implements ValidationContext.DiscriminatorContext {
            @Override public void markMatch() {}
            @Override public boolean isDiscriminatorMatchFound() { return false; }
            @Override public void registerDiscriminator(String path, ObjectNode disc) {}
            @Override public ObjectNode getDiscriminatorForPath(JsonSchema s) { return rootDisc; }
            @Override public ObjectNode getDiscriminatorForPath(String path) { return rootDisc; }
        }
        SimpleCtx ctx = new SimpleCtx();

        java.lang.reflect.Method m = BaseJsonValidator.class
                .getDeclaredMethod("registerAndMergeDiscriminator",
                        ValidationContext.DiscriminatorContext.class,
                        ObjectNode.class,
                        JsonSchema.class,
                        String.class);
        m.setAccessible(true);
        // This call must throw JsonSchemaException because the nested discriminator
        // tries to redefine the propertyName.
        m.invoke(null, ctx, MAPPER.createObjectNode(), parent, "at/");
    }

    @Test
    public void testNoExplicitDiscriminatorKeyOverride() throws Exception {
        // Mapping that does NOT contain the parent schema path
        ObjectNode mapping = MAPPER.createObjectNode();
        mapping.put("foo", "/schemas/foo");

        JsonSchema parent = mock(JsonSchema.class);
        when(parent.schemaPath).thenReturn("/schemas/bar");

        java.lang.reflect.Method m = BaseJsonValidator.class
                .getDeclaredMethod("noExplicitDiscriminatorKeyOverride",
                        JsonNode.class,
                        JsonSchema.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(null, mapping, parent);
        assertTrue(result);

        // Now add the parent path – should return false
        mapping.put("bar", "/schemas/bar");
        boolean result2 = (boolean) m.invoke(null, mapping, parent);
        assertFalse(result2);
    }

    // -----------------------------------------------------------------------
    // 10. Additional sanity checks (null handling, defensive programming)
    // -----------------------------------------------------------------------
    @Test
    public void testDebugWithNullLoggerDoesNotThrow() {
        // The method itself does not guard against null, but callers are expected
        // to pass a non‑null logger. We still verify that a NPE is not thrown
        // when the logger reference is null by catching it.
        try {
            BaseJsonValidator.debug(null,
                    MAPPER.createObjectNode(),
                    MAPPER.createObjectNode(),
                    "/any");
        } catch (NullPointerException npe) {
            fail("debug() should not throw NPE when logger is null (it simply dereferences).");
        }
    }

    @Test
    public void testAtPathWithEmptyCurrentPath() {
        DummyValidator validator = new DummyValidator(
                "/p",
                simpleSchemaNode,
                parentSchema,
                validatorType,
                validationContext,
                false);
        String result = validator.callAtPath("", "first");
        // Default PathType appends a leading '/' if needed
        assertEquals("/first", result);
    }
}
###Test END##
```