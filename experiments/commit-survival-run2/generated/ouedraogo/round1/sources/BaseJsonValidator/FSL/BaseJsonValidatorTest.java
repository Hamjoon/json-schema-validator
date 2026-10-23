/*****************************************************************************************
 * JUnit‑4 test suite for {@link com.networknt.schema.BaseJsonValidator}
 *
 * The tests cover every public / protected method of the class, exercising all
 * logical branches (null handling, explicit/implicit discriminator mapping,
 * sub‑schema retrieval, path handling, etc.).  The suite uses only the public
 * API of the library together with Mockito for lightweight stubbing.  Where the
 * library does not expose a setter (e.g. final fields) reflection is used to
 * inject the required state.
 *
 * NOTE – The test class defines a minimal concrete implementation of the abstract
 * BaseJsonValidator (named {@code TestValidator}) and a handful of tiny stub
 * implementations for the surrounding types (JsonSchema, ValidationContext,
 * Config, URIFactory, JsonSchemaFactory, etc.).  These stubs provide just enough
 * behaviour for the validator logic to be exercised without pulling in the full
 * library implementation.
 *****************************************************************************************/

package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import java.net.URI;
import java.util.*;

public class BaseJsonValidatorTest {

    /* -------------------------------------------------------------------------
     *  Minimal stub implementations – only the members that the validator uses
     * ---------------------------------------------------------------------- */

    /** Simple stub for {@link JsonSchema}. */
    private static class StubJsonSchema implements JsonSchema {
        final String schemaPath;
        final JsonNode schemaNode;

        StubJsonSchema(String schemaPath, JsonNode schemaNode) {
            this.schemaPath = schemaPath;
            this.schemaNode = schemaNode;
        }

        @Override public JsonNode getSchemaNode() { return schemaNode; }

        /* The remaining methods of the JsonSchema interface are not required for
         * the tests and therefore are left with a no‑op implementation. */
        @Override public void initializeValidators() {}
        @Override public Set<ValidationMessage> validate(JsonNode node) { return Collections.emptySet(); }
        @Override public Set<ValidationMessage> validate(JsonNode node, String at) { return Collections.emptySet(); }
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public String getSchemaPath() { return schemaPath; }
        @Override public String getMessage() { return null; }
        @Override public void setMessage(String message) {}
        @Override public ValidatorTypeCode getValidatorType() { return null; }
        @Override public void setValidatorType(ValidatorTypeCode validatorType) {}
        @Override public JsonSchema getSubSchema(String pointer) { return null; }
        @Override public JsonSchema getSubSchema(JsonNode node) { return null; }
        @Override public ValidationContext getValidationContext() { return null; }
        @Override public void setValidationContext(ValidationContext validationContext) {}
        @Override public JsonSchemaFactory getSchemaFactory() { return null; }
        @Override public void setSchemaFactory(JsonSchemaFactory schemaFactory) {}
        @Override public Config getConfig() { return null; }
        @Override public void setConfig(Config config) {}
        @Override public URI getId() { return null; }
        @Override public void setId(URI id) {}
        @Override public boolean isReadOnly() { return false; }
        @Override public void setReadOnly(boolean readOnly) {}
        @Override public String getRef() { return null; }
        @Override public void setRef(String ref) {}
        @Override public JsonNode getSchemaNodeTree() { return null; }
        @Override public void setSchemaNodeTree(JsonNode schemaNodeTree) {}
        @Override public JsonNode getSchemaNodeForPath(String path) { return null; }
        @Override public void setSchemaNodeForPath(String path, JsonNode node) {}
        @Override public Set<String> getEvaluatedProperties() { return null; }
        @Override public void addEvaluatedProperty(String property) {}
        @Override public Set<String> getEvaluatedItems() { return null; }
        @Override public void addEvaluatedItem(String index) {}
        @Override public boolean isRecursive() { return false; }
        @Override public void setRecursive(boolean recursive) {}
    }

    /** Simple stub for {@link ValidationContext}. */
    private static class StubValidationContext implements ValidationContext {
        private final Config config;
        private final URIFactory uriFactory;
        private final JsonSchemaFactory schemaFactory;
        private final Map<String, DiscriminatorContext> discriminatorMap = new HashMap<>();

        StubValidationContext(Config config, URIFactory uriFactory, JsonSchemaFactory schemaFactory) {
            this.config = config;
            this.uriFactory = uriFactory;
            this.schemaFactory = schemaFactory;
        }

        @Override public Config getConfig() { return config; }
        @Override public URIFactory getURIFactory() { return uriFactory; }
        @Override public JsonSchemaFactory getJsonSchemaFactory() { return schemaFactory; }

        /* Only the discriminator related API is needed for the tests. */
        @Override public DiscriminatorContext getDiscriminatorContext() {
            return new DiscriminatorContext() {
                private final Set<String> matched = new HashSet<>();
                private final Map<String, ObjectNode> registry = new HashMap<>();

                @Override public void registerDiscriminator(String path, ObjectNode disc) {
                    registry.put(path, disc);
                }
                @Override public ObjectNode getDiscriminatorForPath(String path) {
                    return registry.get(path);
                }
                @Override public void markMatch() { matched.add("matched"); }
                @Override public boolean isDiscriminatorMatchFound() { return !matched.isEmpty(); }
                @Override public void clearMatch() { matched.clear(); }
            };
        }

        /* Unused methods – return null / empty values. */
        @Override public String getCurrentPath() { return null; }
        @Override public void setCurrentPath(String path) {}
        @Override public String getSchemaPath() { return null; }
        @Override public void setSchemaPath(String path) {}
        @Override public JsonNode getRootNode() { return null; }
        @Override public void setRootNode(JsonNode node) {}
        @Override public boolean isFailFast() { return false; }
        @Override public void setFailFast(boolean failFast) {}
        @Override public void setSchemaPathStack(Deque<String> stack) {}
        @Override public Deque<String> getSchemaPathStack() { return null; }
        @Override public void setCurrentNode(JsonNode node) {}
        @Override public JsonNode getCurrentNode() { return null; }
        @Override public void setParentSchema(JsonSchema schema) {}
        @Override public JsonSchema getParentSchema() { return null; }
    }

    /** Very small {@link Config} implementation – only the flags that BaseJsonValidator reads. */
    private static class StubConfig implements Config {
        private final ApplyDefaultsStrategy applyDefaultsStrategy;
        private final PathType pathType;
        private final boolean failFast;

        StubConfig(ApplyDefaultsStrategy applyDefaultsStrategy, PathType pathType, boolean failFast) {
            this.applyDefaultsStrategy = applyDefaultsStrategy;
            this.pathType = pathType;
            this.failFast = failFast;
        }

        @Override public ApplyDefaultsStrategy getApplyDefaultsStrategy() { return applyDefaultsStrategy; }
        @Override public PathType getPathType() { return pathType; }
        @Override public boolean isFailFast() { return failFast; }

        /* All other config methods are irrelevant for the current test suite. */
        @Override public void setFailFast(boolean failFast) {}
        @Override public void setApplyDefaultsStrategy(ApplyDefaultsStrategy strategy) {}
        @Override public void setPathType(PathType pathType) {}
        @Override public boolean isHandleNullableField() { return false; }
        @Override public void setHandleNullableField(boolean handleNullableField) {}
        @Override public boolean isOpenApiMode() { return false; }
        @Override public void setOpenApiMode(boolean openApiMode) {}
        @Override public boolean isTypeLoose() { return false; }
        @Override public void setTypeLoose(boolean typeLoose) {}
        @Override public boolean isFailOnUnknownProperties() { return false; }
        @Override public void setFailOnUnknownProperties(boolean failOnUnknownProperties) {}
        @Override public boolean isTypeLooseWhenNotSpecified() { return false; }
        @Override public void setTypeLooseWhenNotSpecified(boolean typeLooseWhenNotSpecified) {}
        @Override public boolean isFailFastOnFirstError() { return false; }
        @Override public void setFailFastOnFirstError(boolean failFastOnFirstError) {}
        @Override public boolean isHideIgnoredProperties() { return false; }
        @Override public void setHideIgnoredProperties(boolean hideIgnoredProperties) {}
        @Override public boolean isIgnoreUndeclaredProperties() { return false; }
        @Override public void setIgnoreUndeclaredProperties(boolean ignoreUndeclaredProperties) {}
        @Override public boolean isValidateObjectAsArray() { return false; }
        @Override public void setValidateObjectAsArray(boolean validateObjectAsArray) {}
        @Override public boolean isValidatePlainObject() { return false; }
        @Override public void setValidatePlainObject(boolean validatePlainObject) {}
        @Override public boolean isSkipDefaults() { return false; }
        @Override public void setSkipDefaults(boolean skipDefaults) {}
        @Override public boolean isReadOnly() { return false; }
        @Override public void setReadOnly(boolean readOnly) {}
        @Override public boolean isFailFastOnInvalidSchema() { return false; }
        @Override public void setFailFastOnInvalidSchema(boolean failFastOnInvalidSchema) {}
        @Override public boolean isValidateSchema() { return false; }
        @Override public void setValidateSchema(boolean validateSchema) {}
        @Override public boolean isValidateSchemaUnconditionally() { return false; }
        @Override public void setValidateSchemaUnconditionally(boolean validateSchemaUnconditionally) {}
        @Override public boolean isDisplayMessageKey() { return false; }
        @Override public void setDisplayMessageKey(boolean displayMessageKey) {}
        @Override public boolean isLogError() { return false; }
        @Override public void setLogError(boolean logError) {}
        @Override public boolean isSetDefaults() { return false; }
        @Override public void setSetDefaults(boolean setDefaults) {}
        @Override public boolean isCollectAllDefaults() { return false; }
        @Override public void setCollectAllDefaults(boolean collectAllDefaults) {}
        @Override public boolean isAddToAdditionalProperties() { return false; }
        @Override public void setAddToAdditionalProperties(boolean addToAdditionalProperties) {}
        @Override public boolean isValidateAdditionalProperties() { return false; }
        @Override public void setValidateAdditionalProperties(boolean validateAdditionalProperties) {}
        @Override public boolean isValidateItems() { return false; }
        @Override public void setValidateItems(boolean validateItems) {}
        @Override public boolean isValidateFormat() { return false; }
        @Override public void setValidateFormat(boolean validateFormat) {}
        @Override public boolean isShouldValidate() { return true; }
        @Override public void setShouldValidate(boolean shouldValidate) {}
        @Override public String getMessageKey() { return null; }
        @Override public void setMessageKey(String messageKey) {}
        @Override public String getMessage() { return null; }
        @Override public void setMessage(String message) {}
        @Override public Locale getLocale() { return Locale.ENGLISH; }
        @Override public void setLocale(Locale locale) {}
        @Override public MessageSource getMessageSource() { return null; }
        @Override public void setMessageSource(MessageSource messageSource) {}
        @Override public ResourceBundle getResourceBundle() { return I18nSupport.DEFAULT_RESOURCE_BUNDLE; }
        @Override public void setResourceBundle(ResourceBundle resourceBundle) {}
        @Override public boolean isReadOnlyNode() { return false; }
        @Override public void setReadOnlyNode(boolean readOnlyNode) {}
        @Override public Map<String, Object> asMap() { return Collections.emptyMap(); }
    }

    /** Simple {@link PathType} implementation used for deterministic path handling. */
    private static class SimplePathType implements PathType {
        @Override public String getRoot() { return "$"; }
        @Override public String append(String currentPath, String token) {
            return currentPath + "." + token;
        }
        @Override public String append(String currentPath, int index) {
            return currentPath + "[" + index + "]";
        }
    }

    /** Minimal {@link URIFactory} that creates a URI or throws on malformed input. */
    private static class SimpleURIFactory implements URIFactory {
        @Override public URI create(String uri) { return URI.create(uri); }
    }

    /** Minimal {@link JsonSchemaFactory} that always returns a new stub schema for a URI. */
    private static class SimpleJsonSchemaFactory implements JsonSchemaFactory {
        @Override public JsonSchema getSchema(URI uri, Config config) {
            // the returned schema simply stores the URI string as its path – enough for the
            // validator logic that compares schemaPath values.
            return new StubJsonSchema(uri.toString(), MissingNode.getInstance());
        }

        /* All other overloads are not required for the test suite. */
        @Override public JsonSchema getSchema(String schema) { return null; }
        @Override public JsonSchema getSchema(JsonNode schemaNode) { return null; }
        @Override public JsonSchema getSchema(JsonNode schemaNode, Config config) { return null; }
        @Override public JsonSchema getSchema(String schema, Config config) { return null; }
        @Override public JsonSchema getSchema(String schema, ValidationContext context) { return null; }
        @Override public JsonSchema getSchema(JsonNode schemaNode, ValidationContext context) { return null; }
        @Override public JsonSchema getSchema(URI uri) { return null; }
        @Override public JsonSchema getSchema(URI uri, ValidationContext context) { return null; }
        @Override public JsonSchema getSchema(URI uri, Config config, ValidationContext context) { return null; }
    }

    /** Concrete validator used only for exercising the protected methods. */
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
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No real validation – the test suite never calls the public validate()
            return Collections.emptySet();
        }
    }

    /* -------------------------------------------------------------------------
     *  Common test fixtures
     * ---------------------------------------------------------------------- */

    private Logger logger;
    private JsonNode dummyNode;
    private JsonNode rootNode;
    private ValidationContext validationContext;
    private StubConfig config;
    private TestValidator validator;

    @Before
    public void setUp() {
        logger = mock(Logger.class);
        dummyNode = mock(JsonNode.class);
        rootNode = mock(JsonNode.class);

        // config with a deterministic PathType and no special flags
        config = new StubConfig(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY,
                                new SimplePathType(),
                                false);
        validationContext = new StubValidationContext(config,
                                                      new SimpleURIFactory(),
                                                      new SimpleJsonSchemaFactory());

        // parent schema – only the "type" field is used by getNodeFieldType()
        ObjectNode parentSchemaNode = JsonNodeFactory.instance.objectNode();
        parentSchemaNode.put("type", "object");
        JsonSchema parentSchema = new StubJsonSchema("#/parent", parentSchemaNode);

        // the validator itself – we start with an empty schema node
        ObjectNode validatorSchemaNode = JsonNodeFactory.instance.objectNode();
        validator = new TestValidator("#/test", validatorSchemaNode,
                                      parentSchema,
                                      mock(ValidatorTypeCode.class),
                                      validationContext,
                                      false);
    }

    /* -------------------------------------------------------------------------
     *  Tests for the static utility methods
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsWithVeryCloseDoubles() {
        assertTrue(BaseJsonValidator.equals(0.1 + 0.2, 0.3));
        assertFalse(BaseJsonValidator.equals(0.1, 0.2));
    }

    @Test
    public void testDebugWritesExpectedMessage() {
        BaseJsonValidator.debug(logger, dummyNode, rootNode, "myPath");
        ArgumentCaptor<String> formatCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> arg1 = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> arg2 = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> arg3 = ArgumentCaptor.forClass(Object.class);
        verify(logger).debug(formatCaptor.capture(),
                             arg1.capture(),
                             arg2.capture(),
                             arg3.capture());

        assertEquals("validate( {}, {}, {})", formatCaptor.getValue());
        assertSame(dummyNode, arg1.getValue());
        assertSame(rootNode, arg2.getValue());
        assertEquals("myPath", arg3.getValue());
    }

    /* -------------------------------------------------------------------------
     *  Tests for discriminator handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testCheckDiscriminatorMatchWhenPropertyValueIsNull() {
        DiscriminatorContext ctx = validationContext.getDiscriminatorContext();
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode(); // empty, mapping not required
        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator, null, null);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitMappingMatch() {
        DiscriminatorContext ctx = validationContext.getDiscriminatorContext();
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode(); // no "mapping"
        // Schema whose path ends with the discriminator value – should be a match
        StubJsonSchema schema = new StubJsonSchema("#/components/schemas/Animal/dog", MissingNode.getInstance());

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator,
                "dog", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchImplicitMappingNoMatch() {
        DiscriminatorContext ctx = validationContext.getDiscriminatorContext();
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode(); // no "mapping"
        StubJsonSchema schema = new StubJsonSchema("#/components/schemas/Animal/cat", MissingNode.getInstance());

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator,
                "dog", schema);
        assertFalse(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingMatch() {
        DiscriminatorContext ctx = validationContext.getDiscriminatorContext();
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        mapping.put("dog", "#/components/schemas/Animal/dog");
        discriminator.set("mapping", mapping);

        StubJsonSchema schema = new StubJsonSchema("#/components/schemas/Animal/dog", MissingNode.getInstance());

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator,
                "dog", schema);
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingNoMatchFallsBackToImplicitWhenAllowed() {
        DiscriminatorContext ctx = validationContext.getDiscriminatorContext();
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        // Mapping that does NOT contain the value we will test
        mapping.put("cat", "#/components/schemas/Animal/cat");
        discriminator.set("mapping", mapping);

        // The schema path ends with the discriminator value – implicit match is possible
        StubJsonSchema schema = new StubJsonSchema("#/components/schemas/Animal/dog", MissingNode.getInstance());

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator,
                "dog", schema);
        // because the explicit mapping does not match AND no explicit key overrides the parent,
        // the implicit rule should fire.
        assertTrue(ctx.isDiscriminatorMatchFound());
    }

    @Test
    public void testCheckDiscriminatorMatchExplicitMappingNoMatchDoesNotFallBackWhenOverrideExists() {
        DiscriminatorContext ctx = validationContext.getDiscriminatorContext();
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        ObjectNode mapping = JsonNodeFactory.instance.objectNode();
        // Explicit mapping that points to the *parent* schema path – this blocks the fallback
        mapping.put("dog", "#/components/schemas/Animal/parent");
        discriminator.set("mapping", mapping);

        // Parent schema that the mapping points to
        StubJsonSchema parentSchema = new StubJsonSchema("#/components/schemas/Animal/parent", MissingNode.getInstance());

        // The schema we are testing – its path does end with "dog" but fallback must be blocked
        StubJsonSchema schema = new StubJsonSchema("#/components/schemas/Animal/dog", MissingNode.getInstance());

        BaseJsonValidator.checkDiscriminatorMatch(ctx, discriminator,
                "dog", schema);
        // No match because the explicit mapping overrides the implicit rule
        assertFalse(ctx.isDiscriminatorMatchFound());
    }

    /* -------------------------------------------------------------------------
     *  Tests for registerAndMergeDiscriminator
     * ---------------------------------------------------------------------- */

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorThrowsWhenPropertyNameRedefined() {
        // Build a parent schema that already contains a discriminator with a propertyName
        ObjectNode parentDiscriminator = JsonNodeFactory.instance.objectNode();
        parentDiscriminator.put("propertyName", "type");
        ObjectNode parentSchemaNode = JsonNodeFactory.instance.objectNode();
        parentSchemaNode.set("discriminator", parentDiscriminator);
        StubJsonSchema parentSchema = new StubJsonSchema("#/parent", parentSchemaNode);

        // The validator under test will hold a *different* discriminator (without propertyName)
        ObjectNode childDiscriminator = JsonNodeFactory.instance.objectNode();
        TestValidator childValidator = new TestValidator("#/child", JsonNodeFactory.instance.objectNode(),
                                                         parentSchema,
                                                         mock(ValidatorTypeCode.class),
                                                         validationContext,
                                                         false);

        // Trigger the merge – the code should detect the propertyName on the already‑registered
        // discriminator and throw JsonSchemaException.
        BaseJsonValidator.registerAndMergeDiscriminator(
                validationContext.getDiscriminatorContext(),
                childDiscriminator,
                childValidator,
                "atPath"
        );
    }

    @Test
    public void testRegisterAndMergeDiscriminatorAddsMissingMapping() {
        // Parent schema with a discriminator that has a mapping entry
        ObjectNode parentMapping = JsonNodeFactory.instance.objectNode();
        parentMapping.put("cat", "#/components/schemas/Animal/cat");
        ObjectNode parentDiscriminator = JsonNodeFactory.instance.objectNode();
        parentDiscriminator.set("mapping", parentMapping);
        ObjectNode parentSchemaNode = JsonNodeFactory.instance.objectNode();
        parentSchemaNode.set("discriminator", parentDiscriminator);
        StubJsonSchema parentSchema = new StubJsonSchema("#/parent", parentSchemaNode);

        // Child schema that also defines a *different* mapping entry
        ObjectNode childMapping = JsonNodeFactory.instance.objectNode();
        childMapping.put("dog", "#/components/schemas/Animal/dog");
        ObjectNode childDiscriminator = JsonNodeFactory.instance.objectNode();
        childDiscriminator.set("mapping", childMapping);
        TestValidator childValidator = new TestValidator("#/child", JsonNodeFactory.instance.objectNode(),
                                                         parentSchema,
                                                         mock(ValidatorTypeCode.class),
                                                         validationContext,
                                                         false);

        // Register the parent discriminator first (simulating earlier processing)
        validationContext.getDiscriminatorContext().registerDiscriminator("#/parent", parentDiscriminator);

        // Now merge the child discriminator – the mapping for "dog" should be added,
        // while the existing "cat" mapping must stay untouched.
        BaseJsonValidator.registerAndMergeDiscriminator(
                validationContext.getDiscriminatorContext(),
                childDiscriminator,
                childValidator,
                "atPath"
        );

        ObjectNode merged = (ObjectNode) validationContext.getDiscriminatorContext()
                .getDiscriminatorForPath("#/child");
        assertNotNull(merged);
        ObjectNode mergedMapping = (ObjectNode) merged.get("mapping");
        assertNotNull(mergedMapping);
        assertEquals("#/components/schemas/Animal/cat", mergedMapping.get("cat").asText());
        assertEquals("#/components/schemas/Animal/dog", mergedMapping.get("dog").asText());
    }

    @Test(expected = JsonSchemaException.class)
    public void testRegisterAndMergeDiscriminatorDetectsMappingRedefinition() {
        // Parent discriminator with a mapping for "cat"
        ObjectNode parentMapping = JsonNodeFactory.instance.objectNode();
        parentMapping.put("cat", "#/components/schemas/Animal/cat");
        ObjectNode parentDiscriminator = JsonNodeFactory.instance.objectNode();
        parentDiscriminator.set("mapping", parentMapping);
        ObjectNode parentSchemaNode = JsonNodeFactory.instance.objectNode();
        parentSchemaNode.set("discriminator", parentDiscriminator);
        StubJsonSchema parentSchema = new StubJsonSchema("#/parent", parentSchemaNode);

        // Child discriminator attempts to redefine the same key with a *different* value
        ObjectNode childMapping = JsonNodeFactory.instance.objectNode();
        childMapping.put("cat", "#/components/schemas/Animal/otherCat");
        ObjectNode childDiscriminator = JsonNodeFactory.instance.objectNode();
        childDiscriminator.set("mapping", childMapping);
        TestValidator childValidator = new TestValidator("#/child", JsonNodeFactory.instance.objectNode(),
                                                         parentSchema,
                                                         mock(ValidatorTypeCode.class),
                                                         validationContext,
                                                         false);

        // Register parent first
        validationContext.getDiscriminatorContext().registerDiscriminator("#/parent", parentDiscriminator);

        // Merge – should throw because "cat" is being redefined
        BaseJsonValidator.registerAndMergeDiscriminator(
                validationContext.getDiscriminatorContext(),
                childDiscriminator,
                childValidator,
                "atPath"
        );
    }

    /* -------------------------------------------------------------------------
     *  Tests for fetchSubSchemaNode (including obtainSubSchemaNode logic)
     * ---------------------------------------------------------------------- */

    @Test
    public void testFetchSubSchemaNodeSuppressedReturnsNull() {
        TestValidator suppressed = new TestValidator("#/test", JsonNodeFactory.instance.objectNode(),
                                                     new StubJsonSchema("#/parent", MissingNode.getInstance()),
                                                     mock(ValidatorTypeCode.class),
                                                     validationContext,
                                                     true);
        assertNull(suppressed.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNodeWhenIdMissingReturnsNull() {
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode(); // no "id"
        TestValidator v = new TestValidator("#/test", schemaNode,
                                            new StubJsonSchema("#/parent", MissingNode.getInstance()),
                                            mock(ValidatorTypeCode.class),
                                            validationContext,
                                            false);
        assertNull(v.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNodeWhenIdEqualsDollarSchemaReturnsNull() {
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("id", "http://example.com/schema");
        schemaNode.put("$schema", "http://example.com/schema"); // same as id
        TestValidator v = new TestValidator("#/test", schemaNode,
                                            new StubJsonSchema("#/parent", MissingNode.getInstance()),
                                            mock(ValidatorTypeCode.class),
                                            validationContext,
                                            false);
        assertNull(v.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNodeWhenIdIsInvalidUriReturnsNull() {
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("id", ":::: not a uri :::");
        TestValidator v = new TestValidator("#/test", schemaNode,
                                            new StubJsonSchema("#/parent", MissingNode.getInstance()),
                                            mock(ValidatorTypeCode.class),
                                            validationContext,
                                            false);
        assertNull(v.fetchSubSchemaNode(validationContext));
    }

    @Test
    public void testFetchSubSchemaNodeReturnsSchemaWhenIdIsValid() {
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        schemaNode.put("id", "http://example.com/other-schema");
        TestValidator v = new TestValidator("#/test", schemaNode,
                                            new StubJsonSchema("#/parent", MissingNode.getInstance()),
                                            mock(ValidatorTypeCode.class),
                                            validationContext,
                                            false);
        JsonSchema result = v.fetchSubSchemaNode(validationContext);
        assertNotNull(result);
        // The stub factory creates a schema whose path equals the URI string.
        assertEquals("http://example.com/other-schema", result.getSchemaPath());
    }

    /* -------------------------------------------------------------------------
     *  Tests for the simple accessor / path helper methods
     * ---------------------------------------------------------------------- */

    @Test
    public void testGettersReturnSuppliedValues() {
        assertEquals("#/test", validator.getSchemaPath());
        assertNotNull(validator.getSchemaNode());
        assertNotNull(validator.getParentSchema());
    }

    @Test
    public void testGetNodeFieldTypeReturnsParentTypeIfPresent() {
        // The parent schema node (created in setUp) contains "type":"object"
        assertEquals("object", validator.getNodeFieldType());
    }

    @Test
    public void testGetNodeFieldTypeReturnsNullWhenParentHasNoType() {
        JsonSchema parentWithoutType = new StubJsonSchema("#/parent", JsonNodeFactory.instance.objectNode());
        TestValidator v = new TestValidator("#/test", JsonNodeFactory.instance.objectNode(),
                                            parentWithoutType,
                                            mock(ValidatorTypeCode.class),
                                            validationContext,
                                            false);
        assertNull(v.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemasInvokesInitializeValidators() {
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        Collection<JsonSchema> collection = Arrays.asList(schemaA, schemaB);
        validator.preloadJsonSchemas(collection);
        verify(schemaA).initializeValidators();
        verify(schemaB).initializeValidators();
    }

    @Test
    public void testPathHelpersUseConfiguredPathType() {
        // The stub config supplies SimplePathType, whose root is "$"
        assertEquals("$", validator.atRoot());

        String child = validator.atPath("$", "foo");
        assertEquals("$.foo", child);

        String indexed = validator.atPath("$", 3);
        assertEquals("$[3]", indexed);
    }
}
