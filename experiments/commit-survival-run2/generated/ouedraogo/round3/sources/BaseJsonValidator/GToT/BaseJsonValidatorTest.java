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
