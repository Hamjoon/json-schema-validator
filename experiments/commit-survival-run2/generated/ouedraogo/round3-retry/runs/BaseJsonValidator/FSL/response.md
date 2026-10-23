###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

/**
 * Comprehensive unit tests for {@link BaseJsonValidator}.
 *
 * The test class defines a minimal concrete implementation of {@link BaseJsonValidator}
 * together with lightweight stub implementations of the various collaborators
 * ( {@link JsonSchema}, {@link ValidationContext}, {@link PathType}, etc.).
 *
 * The tests cover:
 * <ul>
 *   <li>Constructor initialisation (path type, defaults strategy, suppress flag)</li>
 *   <li>Static utility methods: {@code equals}, {@code debug}</li>
 *   <li>Sub‑schema retrieval logic via {@code fetchSubSchemaNode}</li>
 *   <li>Discriminator handling helpers</li>
 *   <li>Path handling helpers</li>
 *   <li>Getter methods and auxiliary behaviour</li>
 * </ul>
 */
public class BaseJsonValidatorTest {

    /** Simple concrete validator used for testing the protected members of BaseJsonValidator. */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema,
                      ValidatorTypeCode validatorType, ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                                JsonNode rootNode, String at) {
            return Collections.emptySet();
        }
    }

    /** Minimal stub for {@link JsonSchema}. */
    private static class StubJsonSchema implements JsonSchema {
        final String schemaPath;
        final JsonNode schemaNode;
        final JsonSchema parent;

        StubJsonSchema(String schemaPath, JsonNode schemaNode, JsonSchema parent) {
            this.schemaPath = schemaPath;
            this.schemaNode = schemaNode;
            this.parent = parent;
        }

        @Override public String getSchemaPath() { return schemaPath; }
        @Override public JsonNode getSchemaNode() { return schemaNode; }
        @Override public JsonSchema getParentSchema() { return parent; }

        // The following methods are not needed for the tests, they are no‑ops.
        @Override public void initializeValidators() {}
        @Override public Set<ValidationMessage> validate(JsonNode node) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, JsonNode rootNode) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, JsonNode rootNode, String at) { return null; }
        @Override public void walk(JsonNode node, ValidationContext context) {}
        @Override public void walk(JsonNode node, ValidationContext context, String at) {}
        @Override public JsonSchema getSchemaByURI(URI uri) { return null; }
        @Override public JsonSchema getSchemaByPath(String path) { return null; }
        @Override public JsonSchema getParentSchema(JsonNode node) { return null; }
    }

    /** Minimal stub for {@link ValidationContext}. */
    private static class StubValidationContext implements ValidationContext {
        private final Config config;
        private final URIFactory uriFactory = new StubURIFactory();
        private final JsonSchemaFactory jsonSchemaFactory = new StubJsonSchemaFactory();

        StubValidationContext(Config config) { this.config = config; }

        @Override public Config getConfig() { return config; }
        @Override public URIFactory getURIFactory() { return uriFactory; }
        @Override public JsonSchemaFactory getJsonSchemaFactory() { return jsonSchemaFactory; }
        @Override public DiscriminatorContext getDiscriminatorContext() { return null; }
        @Override public void setDiscriminatorContext(DiscriminatorContext ctx) {}
        @Override public void setCurrentUri(URI uri) {}
        @Override public URI getCurrentUri() { return null; }
        @Override public void setCurrentSchema(JsonSchema schema) {}
        @Override public JsonSchema getCurrentSchema() { return null; }
        @Override public void setCurrentNode(JsonNode node) {}
        @Override public JsonNode getCurrentNode() { return null; }
        @Override public void setDepth(int depth) {}
        @Override public int getDepth() { return 0; }
        @Override public void setPath(String path) {}
        @Override public String getPath() { return null; }
        @Override public void setRootNode(JsonNode rootNode) {}
        @Override public JsonNode getRootNode() { return null; }
        @Override public void setParentSchema(JsonSchema schema) {}
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public void setCurrentDiscriminatorContext(DiscriminatorContext ctx) {}
        @Override public DiscriminatorContext getCurrentDiscriminatorContext() { return null; }
    }

    /** Minimal stub for {@link ValidationContext.Config}. */
    private static class StubConfig implements ValidationContext.Config {
        private final boolean failFast;
        private final ApplyDefaultsStrategy applyDefaultsStrategy;
        private final PathType pathType;

        StubConfig(boolean failFast, ApplyDefaultsStrategy applyDefaultsStrategy, PathType pathType) {
            this.failFast = failFast;
            this.applyDefaultsStrategy = applyDefaultsStrategy;
            this.pathType = pathType;
        }

        @Override public boolean isFailFast() { return failFast; }
        @Override public ApplyDefaultsStrategy getApplyDefaultsStrategy() { return applyDefaultsStrategy; }
        @Override public PathType getPathType() { return pathType; }
        @Override public ResourceBundle getResourceBundle() { return null; }
        @Override public boolean isOpenAPI31() { return false; }
        @Override public boolean isOpenAPI30() { return false; }
        @Override public boolean isOpenAPI20() { return false; }
        @Override public boolean isOpenAPI() { return false; }
        @Override public boolean isJsonSchemaVersion202012() { return false; }
        @Override public boolean isJsonSchemaVersion201909() { return false; }
        @Override public boolean isJsonSchemaVersionDraft7() { return false; }
        @Override public boolean isJsonSchemaVersionDraft6() { return false; }
        @Override public boolean isJsonSchemaVersionDraft4() { return false; }
        @Override public boolean isJsonSchemaVersionDraft3() { return false; }
        @Override public boolean isJsonSchemaVersionDraft2() { return false; }
        @Override public boolean isJsonSchemaVersionDraft1() { return false; }
        @Override public boolean isJsonSchemaVersionDraft0() { return false; }
        @Override public boolean isEarlyOpenAPIVersion() { return false; }
    }

    /** Simple {@link URIFactory} implementation delegating to {@link URI#new URI(String)}. */
    private static class StubURIFactory implements URIFactory {
        @Override public URI create(String uri) { return URI.create(uri); }
    }

    /** Simple {@link JsonSchemaFactory} that creates a {@link StubJsonSchema} from a URI. */
    private static class StubJsonSchemaFactory implements JsonSchemaFactory {
        @Override public JsonSchema getSchema(URI uri, ValidationContext.Config config) {
            // The schema path will be the URI string for easy verification.
            return new StubJsonSchema(uri.toString(), JsonNodeFactory.instance.objectNode(), null);
        }
    }

    /** Very small {@link PathType} implementation used for path handling tests. */
    private static class SimplePathType implements PathType {
        @Override public String getRoot() { return "$"; }
        @Override public String append(String current, String token) { return current + "." + token; }
        @Override public String append(String current, int index) { return current + "[" + index + "]"; }
    }

    /** Stub {@link DiscriminatorContext} that records matches and registrations. */
    private static class StubDiscriminatorContext implements ValidationContext.DiscriminatorContext {
        private boolean matchFound = false;
        private final Map<String, ObjectNode> registry = new HashMap<>();

        @Override public void markMatch() { this.matchFound = true; }
        @Override public boolean isDiscriminatorMatchFound() { return matchFound; }
        @Override public ObjectNode getDiscriminatorForPath(String path) { return registry.get(path); }
        @Override public void registerDiscriminator(String path, ObjectNode discriminator) { registry.put(path, discriminator); }
    }

    private static final JsonNodeFactory NF = JsonNodeFactory.instance;
    private StubConfig defaultConfig;
    private StubValidationContext defaultContext;
    private SimplePathType simplePathType;

    @Before
    public void setUp() {
        simplePathType = new SimplePathType();
        defaultConfig = new StubConfig(false, ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY, simplePathType);
        defaultContext = new StubValidationContext(defaultConfig);
    }

    /* ---------------------------------------------------------------------- */
    /*  Static utility methods                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsWithinTolerance() {
        assertTrue(BaseJsonValidator.equals(1.0, 1.0 + 1e-13));
        assertFalse(BaseJsonValidator.equals(1.0, 1.0 + 1e-10));
    }

    @Test
    public void testDebugLogsCorrectMessage() {
        final StringBuilder sb = new StringBuilder();
        Logger logger = new Logger() {
            @Override public String getName() { return "test"; }
            @Override public boolean isTraceEnabled() { return false; }
            @Override public void trace(String msg) { }
            @Override public void trace(String format, Object arg) { }
            @Override public void trace(String format, Object arg1, Object arg2) { }
            @Override public void trace(String format, Object... arguments) { }
            @Override public void trace(String msg, Throwable t) { }
            @Override public boolean isDebugEnabled() { return true; }
            @Override public void debug(String msg) { sb.append(msg); }
            @Override public void debug(String format, Object arg) { sb.append(String.format(format, arg)); }
            @Override public void debug(String format, Object arg1, Object arg2) { sb.append(String.format(format, arg1, arg2)); }
            @Override public void debug(String format, Object... arguments) { sb.append(String.format(format, arguments)); }
            @Override public void debug(String msg, Throwable t) { }
            @Override public boolean isInfoEnabled() { return false; }
            @Override public void info(String msg) { }
            @Override public void info(String format, Object arg) { }
            @Override public void info(String format, Object arg1, Object arg2) { }
            @Override public void info(String format, Object... arguments) { }
            @Override public void info(String msg, Throwable t) { }
            @Override public boolean isWarnEnabled() { return false; }
            @Override public void warn(String msg) { }
            @Override public void warn(String format, Object arg) { }
            @Override public void warn(String format, Object... arguments) { }
            @Override public void warn(String msg, Throwable t) { }
            @Override public boolean isErrorEnabled() { return false; }
            @Override public void error(String msg) { }
            @Override public void error(String format, Object arg) { }
            @Override public void error(String format, Object arg1, Object arg2) { }
            @Override public void error(String format, Object... arguments) { }
            @Override public void error(String msg, Throwable t) { }
        };

        JsonNode node = NF.textNode("node");
        JsonNode root = NF.textNode("root");
        BaseJsonValidator.debug(logger, node, root, "/path");
        assertTrue(sb.toString().contains("validate"));
        assertTrue(sb.toString().contains("node"));
        assertTrue(sb.toString().contains("root"));
        assertTrue(sb.toString().contains("/path"));
    }

    /* ---------------------------------------------------------------------- */
    /*  fetchSubSchemaNode / obtainSubSchemaNode                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNode_NoId_ReturnsNull() {
        ObjectNode schemaNode = NF.objectNode(); // empty, no "id"
        TestValidator validator = new TestValidator("#", schemaNode, null,
                null, defaultContext, false);
        assertNull(validator.fetchSubSchemaNode(defaultContext));
    }

    @Test
    public void testFetchSubSchemaNode_IdEqualsDollarSchema_ReturnsNull() {
        ObjectNode schemaNode = NF.objectNode();
        schemaNode.set("$schema", NF.textNode("http://example.com/schema"));
        schemaNode.set("id", NF.textNode("http://example.com/schema"));
        TestValidator validator = new TestValidator("#", schemaNode, null,
                null, defaultContext, false);
        assertNull(validator.fetchSubSchemaNode(defaultContext));
    }

    @Test
    public void testFetchSubSchemaNode_InvalidUri_ReturnsNull() {
        ObjectNode schemaNode = NF.objectNode();
        schemaNode.set("id", NF.textNode(":// not a valid uri"));
        TestValidator validator = new TestValidator("#", schemaNode, null,
                null, defaultContext, false);
        assertNull(validator.fetchSubSchemaNode(defaultContext));
    }

    @Test
    public void testFetchSubSchemaNode_ValidUri_ReturnsSchema() {
        ObjectNode schemaNode = NF.objectNode();
        schemaNode.set("id", NF.textNode("http://example.com/valid"));
        TestValidator validator = new TestValidator("#", schemaNode, null,
                null, defaultContext, false);
        JsonSchema result = validator.fetchSubSchemaNode(defaultContext);
        assertNotNull(result);
        assertEquals("http://example.com/valid", result.getSchemaPath());
    }

    @Test
    public void testFetchSubSchemaNode_Suppressed_ReturnsNull() {
        ObjectNode schemaNode = NF.objectNode();
        schemaNode.set("id", NF.textNode("http://example.com/valid"));
        TestValidator validator = new TestValidator("#", schemaNode, null,
                null, defaultContext, true);
        assertNull(validator.fetchSubSchemaNode(defaultContext));
    }

    /* ---------------------------------------------------------------------- */
    /*  Getter methods                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetters() {
        ObjectNode schemaNode = NF.objectNode().put("type", "string");
        StubJsonSchema parent = new StubJsonSchema("parentPath", NF.objectNode(), null);
        TestValidator validator = new TestValidator("myPath", schemaNode, parent,
                null, defaultContext, false);
        assertEquals("myPath", validator.getSchemaPath());
        assertSame(schemaNode, validator.getSchemaNode());
        assertSame(parent, validator.getParentSchema());
    }

    @Test
    public void testGetNodeFieldType_WithAndWithoutType() {
        ObjectNode parentNode = NF.objectNode().put("type", "integer");
        StubJsonSchema parent = new StubJsonSchema("p", parentNode, null);
        TestValidator validator = new TestValidator("p", NF.objectNode(), parent,
                null, defaultContext, false);
        assertEquals("integer", validator.getNodeFieldType());

        // without a type field
        StubJsonSchema parentNoType = new StubJsonSchema("p2", NF.objectNode(), null);
        TestValidator validator2 = new TestValidator("p2", NF.objectNode(), parentNoType,
                null, defaultContext, false);
        assertNull(validator2.getNodeFieldType());
    }

    /* ---------------------------------------------------------------------- */
    /*  preloadJsonSchemas                                                    */
    /* ---------------------------------------------------------------------- */

    private static class InitCountingSchema implements JsonSchema {
        private int initCount = 0;
        @Override public void initializeValidators() { initCount++; }
        int getInitCount() { return initCount; }

        // Unused methods – stubbed
        @Override public String getSchemaPath() { return null; }
        @Override public JsonNode getSchemaNode() { return null; }
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, JsonNode rootNode) { return null; }
        @Override public Set<ValidationMessage> validate(JsonNode node, JsonNode rootNode, String at) { return null; }
        @Override public void walk(JsonNode node, ValidationContext context) {}
        @Override public void walk(JsonNode node, ValidationContext context, String at) {}
        @Override public JsonSchema getSchemaByURI(URI uri) { return null; }
        @Override public JsonSchema getSchemaByPath(String path) { return null; }
        @Override public JsonSchema getParentSchema(JsonNode node) { return null; }
    }

    @Test
    public void testPreloadJsonSchemas_InitializesAll() {
        InitCountingSchema s1 = new InitCountingSchema();
        InitCountingSchema s2 = new InitCountingSchema();
        List<JsonSchema> list = Arrays.asList(s1, s2);
        TestValidator validator = new TestValidator("#", NF.objectNode(), null,
                null, defaultContext, false);
        validator.preloadJsonSchemas(list);
        assertEquals(1, s1.getInitCount());
        assertEquals(1, s2.getInitCount());
    }

    /* ---------------------------------------------------------------------- */
    /*  Path handling (atRoot, atPath)                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPathHelpers() {
        TestValidator validator = new TestValidator("#", NF.objectNode(), null,
                null, defaultContext, false);
        assertEquals("$", validator.atRoot());
        assertEquals("$.name", validator.atPath("$", "name"));
        assertEquals("$.items[3]", validator.atPath("$", 3));
    }

    /* ---------------------------------------------------------------------- */
    /*  Discriminator handling helpers                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatch_NullPropertyValue_MarksMatch() {
        StubDiscriminatorContext ctx = new StubDiscriminatorContext();
        ObjectNode discriminator = NF.objectNode();
        JsonSchema anySchema = new StubJsonSchema("any", NF.objectNode(), null);
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, anySchema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_ImplicitMatch() {
        StubDiscriminatorContext ctx = new StubDiscriminatorContext();
        ObjectNode discriminator = NF.objectNode(); // no "mapping"
        JsonSchema schema = new StubJsonSchema("/definitions/Foo", NF.objectNode(), null);
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "Foo", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitMatchFound() {
        StubDiscriminatorContext ctx = new StubDiscriminatorContext();
        ObjectNode discriminator = NF.objectNode();
        ObjectNode mapping = NF.objectNode();
        mapping.put("dog", NF.textNode("/schemas/Dog"));
        discriminator.set("mapping", mapping);
        JsonSchema schema = new StubJsonSchema("/schemas/Dog", NF.objectNode(), null);
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "dog", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitNoMatch_FallbackImplicitWhenNoOverride() {
        StubDiscriminatorContext ctx = new StubDiscriminatorContext();
        ObjectNode discriminator = NF.objectNode();
        ObjectNode mapping = NF.objectNode();
        mapping.put("cat", NF.textNode("/schemas/Cat"));
        discriminator.set("mapping", mapping);
        // schema path does not match explicit mapping, but suffix matches "Dog"
        JsonSchema schema = new StubJsonSchema("/schemas/Dog", NF.objectNode(), null);
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "Dog", schema);
        // No explicit match, no override -> implicit match should fire
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatch_ExplicitNoMatch_NoFallbackIfOverrideExists() {
        StubDiscriminatorContext ctx = new StubDiscriminatorContext();
        ObjectNode discriminator = NF.objectNode();
        ObjectNode mapping = NF.objectNode();
        mapping.put("Dog", NF.textNode("/schemas/OtherDog"));
        discriminator.set("mapping", mapping);
        JsonSchema schema = new StubJsonSchema("/schemas/Dog", NF.objectNode(), null);
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, "Dog", schema);
        // explicit mapping exists but points elsewhere; override prevents implicit fallback
        assertFalse(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testRegisterAndMergeDiscriminator_AddsMappingWhenContextHasNone() {
        StubDiscriminatorContext ctx = new StubDiscriminatorContext();
        ObjectNode rootDiscriminator = NF.objectNode(); // empty root
        ctx.registerDiscriminator("rootPath", rootDiscriminator);

        // schema with its own discriminator containing a mapping
        ObjectNode schemaDiscriminator = NF.objectNode();
        ObjectNode schemaMapping = NF.objectNode();
        schemaMapping.put("A", NF.textNode("/schemas/A"));
        schemaDiscriminator.set("mapping", schemaMapping);
        schemaDiscriminator.set("propertyName", NF.textNode("type")); // propertyName present – should NOT trigger exception because it is on the schema's own discriminator, not the root

        StubJsonSchema schema = new StubJsonSchema("rootPath/child", schemaDiscriminator, null);

        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at/");
        ObjectNode after = ctx.getDiscriminatorForPath("rootPath");
        assertNotNull(after.get("mapping"));
        assertEquals(NF.textNode("/schemas/A"), after.get("mapping").get("A"));
    }

    @Test
    public void testRegisterAndMergeDiscriminator_MergesWithoutConflict() {
        StubDiscriminatorContext ctx = new StubDiscriminatorContext();
        ObjectNode rootDiscriminator = NF.objectNode();
        ObjectNode rootMapping = NF.objectNode();
        rootMapping.put("X", NF.textNode("/schemas/X"));
        rootDiscriminator.set("mapping", rootMapping);
        ctx.registerDiscriminator("rootPath", rootDiscriminator);

        // schema adds a new mapping "Y"
        ObjectNode schemaDiscriminator = NF.objectNode();
        ObjectNode schemaMapping = NF.objectNode();
        schemaMapping.put("Y", NF.textNode("/schemas/Y"));
        schemaDiscriminator.set("mapping", schemaMapping);
        StubJsonSchema schema = new StubJsonSchema("rootPath/child", schemaDiscriminator, null);

        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at/");
        ObjectNode merged = ctx.getDiscriminatorForPath("rootPath");
        assertEquals(NF.textNode("/schemas/X"), merged.get("mapping").get("X"));
        assertEquals(NF.textNode("/schemas/Y"), merged.get("mapping").get("Y"));
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminator_ConflictThrowsException() {
        StubDiscriminatorContext ctx = new StubDiscriminatorContext();
        ObjectNode rootDiscriminator = NF.objectNode();
        ObjectNode rootMapping = NF.objectNode();
        rootMapping.put("dup", NF.textNode("/schemas/Old"));
        rootDiscriminator.set("mapping", rootMapping);
        ctx.registerDiscriminator("rootPath", rootDiscriminator);

        // schema tries to redefine the same key with a different value
        ObjectNode schemaDiscriminator = NF.objectNode();
        ObjectNode schemaMapping = NF.objectNode();
        schemaMapping.put("dup", NF.textNode("/schemas/New"));
        schemaDiscriminator.set("mapping", schemaMapping);
        StubJsonSchema schema = new StubJsonSchema("rootPath/child", schemaDiscriminator, null);

        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at/");
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminator_PropertyNameRedefinitionThrowsException() {
        StubDiscriminatorContext ctx = new StubDiscriminatorContext();
        ObjectNode rootDiscriminator = NF.objectNode();
        ctx.registerDiscriminator("rootPath", rootDiscriminator);

        // schema discriminator defines a propertyName – illegal when a root discriminator already exists
        ObjectNode schemaDiscriminator = NF.objectNode();
        schemaDiscriminator.set("propertyName", NF.textNode("type"));
        StubJsonSchema schema = new StubJsonSchema("rootPath/child", schemaDiscriminator, null);

        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, "at/");
    }

    @Test
    public void testCheckForImplicitDiscriminatorMappingMatch_Match() {
        StubDiscriminatorContext ctx = new StubDiscriminatorContext();
        JsonSchema schema = new StubJsonSchema("/defs/Animal", NF.objectNode(), null);
        BaseJsonValidator.checkDiscriminatorMatch(ctx, NF.objectNode(), "Animal", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckForExplicitDiscriminatorMappingMatch_Match() {
        StubDiscriminatorContext ctx = new StubDiscriminatorContext();
        JsonNode mapping = NF.objectNode().set("bird", NF.textNode("/schemas/Bird"));
        JsonSchema schema = new StubJsonSchema("/schemas/Bird", NF.objectNode(), null);
        BaseJsonValidator.checkDiscriminatorMatch(ctx, NF.objectNode().set("mapping", mapping), "bird", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testNoExplicitDiscriminatorKeyOverride_ReturnsFalseWhenOverrideExists() {
        ObjectNode mapping = NF.objectNode();
        mapping.put("key1", NF.textNode("/parent/path"));
        StubJsonSchema parent = new StubJsonSchema("/parent/path", NF.objectNode(), null);
        boolean result = invokeNoExplicitDiscriminatorKeyOverride(mapping, parent);
        assertFalse(result);
    }

    @Test
    public void testNoExplicitDiscriminatorKeyOverride_ReturnsTrueWhenNoOverride() {
        ObjectNode mapping = NF.objectNode();
        mapping.put("key1", NF.textNode("/some/other"));
        StubJsonSchema parent = new StubJsonSchema("/parent/path", NF.objectNode(), null);
        boolean result = invokeNoExplicitDiscriminatorKeyOverride(mapping, parent);
        assertTrue(result);
    }

    /** Helper to invoke the private static method {@code noExplicitDiscriminatorKeyOverride}. */
    private boolean invokeNoExplicitDiscriminatorKeyOverride(JsonNode mapping, JsonSchema parentSchema) {
        try {
            java.lang.reflect.Method m = BaseJsonValidator.class.getDeclaredMethod(
                    "noExplicitDiscriminatorKeyOverride", JsonNode.class, JsonSchema.class);
            m.setAccessible(true);
            return (boolean) m.invoke(null, mapping, parentSchema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
###Test END##