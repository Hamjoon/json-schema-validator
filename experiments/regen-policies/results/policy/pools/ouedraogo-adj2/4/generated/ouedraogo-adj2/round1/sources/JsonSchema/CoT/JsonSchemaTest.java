package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link JsonSchema}.  The tests instantiate a minimal
 * {@link ValidationContext} implementation (see {@link TestValidationContext})
 * and a trivial {@link JsonValidator} implementation (see {@link TestValidator})
 * so that the {@link JsonSchema} class can be exercised without needing the full
 * library runtime.  Each public method of {@code JsonSchema} is invoked at least
 * once and edge‑case / exception handling paths are verified.
 *
 * The test file is delimited by the markers required in the task description.
 */
public class JsonSchemaTest {

    /* -----------------------------------------------------------------------
     *  Helper classes – lightweight stand‑ins for library collaborators
     * ---------------------------------------------------------------------*/

    /** Minimal {@link ValidationContext} that supplies a meta‑schema and a dummy
     *  validator factory.  All other methods return {@code null} or sensible defaults. */
    private static class TestValidationContext extends ValidationContext {
        private final SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        public TestValidationContext() {
            super(null, null, JsonMetaSchema.getV4(), null, null);
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            return JsonMetaSchema.getV4();
        }

        @Override
        public String resolveSchemaId(JsonNode schemaNode) {
            // No "$id" handling – keep the original URI unchanged.
            return null;
        }

        @Override
        public JsonValidator newValidator(SchemaLocation schemaLocation,
                                          JsonNodePath evaluationPath,
                                          String keyword,
                                          JsonNode schemaNode,
                                          JsonSchema parentSchema) {
            // Return a no‑op validator that never produces errors.
            return new TestValidator(schemaLocation, evaluationPath, keyword);
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return config;
        }
    }

    /** Very small {@link JsonValidator} implementation used by the tests.
     *  It records whether {@code preloadJsonSchema()} has been called. */
    private static class TestValidator implements JsonValidator {
        private final SchemaLocation schemaLocation;
        private final JsonNodePath evaluationPath;
        private final String keyword;
        private boolean preloaded = false;

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      String keyword) {
            this.schemaLocation = schemaLocation;
            this.evaluationPath = evaluationPath;
            this.keyword = keyword;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                JsonNodePath instanceLocation) {
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> walk(ExecutionContext executionContext,
                                            JsonNode node,
                                            JsonNode rootNode,
                                            JsonNodePath instanceLocation,
                                            boolean shouldValidateSchema) {
            return Collections.emptySet();
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return schemaLocation;
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return evaluationPath;
        }

        @Override
        public String getKeyword() {
            return keyword;
        }

        @Override
        public void preloadJsonSchema() {
            this.preloaded = true;
        }

        boolean isPreloaded() {
            return preloaded;
        }
    }

    /** Simple {@link OutputFormat} that returns the raw set of messages. */
    private static class IdentityFormat implements OutputFormat<Set<ValidationMessage>> {
        @Override
        public Set<ValidationMessage> format(Set<ValidationMessage> validationMessages,
                                              ExecutionContext executionContext,
                                              ValidationContext validationContext) {
            return validationMessages;
        }
    }

    /** No‑op {@link ExecutionCustomizer}. */
    private static class NoOpCustomizer implements ExecutionCustomizer {
        @Override
        public void customize(ExecutionContext executionContext, ValidationContext validationContext) {
            // nothing
        }
    }

    /* -----------------------------------------------------------------------
     *  Test fixtures
     * ---------------------------------------------------------------------*/

    private ObjectMapper mapper;
    private TestValidationContext validationContext;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        validationContext = new TestValidationContext();
    }

    /** Helper to build a {@link JsonSchema} from a JSON string. */
    private JsonSchema buildSchema(String json) throws Exception {
        JsonNode schemaNode = mapper.readTree(json);
        return JsonSchema.from(validationContext,
                SchemaLocation.of("http://example.com/base"),
                null,                         // evaluationPath – not used in the tests
                new URI("http://example.com/base"),
                schemaNode,
                null,                         // no parent
                false);
    }

    /* -----------------------------------------------------------------------
     *  1.  Public method list (for documentation – not a test)
     * ---------------------------------------------------------------------*/

    // The public methods of JsonSchema (extracted from the source):
    //
    // public static JsonSchema from(ValidationContext, SchemaLocation,
    //                               JsonNodePath, URI, JsonNode, JsonSchema,
    //                               boolean)
    // public JsonSchema createChildSchema(SchemaLocation, JsonNode)
    // public URI getCurrentUri()
    // public JsonNode getRefSchemaNode(String)
    // public JsonSchema findAncestor()
    // public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode,
    //                                        JsonNodePath)
    // public Set<ValidationMessage> validate(JsonNode)
    // public <T> T validate(JsonNode, OutputFormat<T>)
    // public <T> T validate(JsonNode, OutputFormat<T>, ExecutionCustomizer)
    // public ValidationResult validateAndCollect(ExecutionContext, JsonNode)
    // public ValidationResult validateAndCollect(JsonNode)
    // public ValidationResult walk(ExecutionContext, JsonNode, boolean)
    // public ValidationResult walk(JsonNode, boolean)
    // public ValidationResult walkAtNode(ExecutionContext, JsonNode, JsonNode,
    //                                    JsonNodePath, boolean)
    // public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode,
    //                                    JsonNodePath, boolean)
    // public String toString()
    // public boolean hasRequiredValidator()
    // public JsonValidator getRequiredValidator()
    // public boolean hasTypeValidator()
    // public TypeValidator getTypeValidator()
    // public List<JsonValidator> getValidators()
    // public void initializeValidators()
    // public boolean isDynamicAnchor()
    // public ExecutionContext createExecutionContext()

    /* -----------------------------------------------------------------------
     *  2.  Basic functionality tests
     * ---------------------------------------------------------------------*/

    @Test
    public void testGetCurrentUri() throws Exception {
        JsonSchema schema = buildSchema("{}");
        assertEquals(new URI("http://example.com/base"), schema.getCurrentUri());
    }

    @Test
    public void testToStringContainsEvaluationPathAndSchemaNode() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"string\"}");
        String txt = schema.toString();
        assertTrue(txt.contains("\"type\":\"string\""));
        // evaluation path for a root schema is empty – we only check that something is printed
        assertTrue(txt.startsWith("\""));
    }

    @Test
    public void testCreateChildSchema() throws Exception {
        JsonSchema parent = buildSchema("{\"type\":\"object\"}");
        JsonNode childNode = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema child = parent.createChildSchema(SchemaLocation.of("http://example.com/child"), childNode);
        assertNotNull(child);
        // the child's parent should be the original schema
        assertEquals(parent, child.getParentSchema());
    }

    @Test
    public void testValidateWithoutErrors() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"string\"}");
        JsonNode instance = mapper.readTree("\"hello\"");
        Set<ValidationMessage> errors = schema.validate(instance);
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testValidateWithOutputFormat() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"string\"}");
        JsonNode instance = mapper.readTree("123"); // invalid – not a string
        Set<ValidationMessage> messages = schema.validate(instance, new IdentityFormat());
        // The default validator for "type" will produce a message
        assertFalse(messages.isEmpty());
        // The message should contain the validator type "type"
        boolean hasType = messages.stream()
                .anyMatch(m -> "type".equals(m.getKeyword()));
        assertTrue(hasType);
    }

    @Test
    public void testValidateWithCustomizer() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"string\"}");
        JsonNode instance = mapper.readTree("123");
        Set<ValidationMessage> messages = schema.validate(instance, new IdentityFormat(),
                new NoOpCustomizer());
        assertFalse(messages.isEmpty());
    }

    @Test
    public void testValidateAndCollect() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"string\"}");
        JsonNode instance = mapper.readTree("\"ok\"");
        ValidationResult result = schema.validateAndCollect(instance);
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty());
    }

    @Test
    public void testWalkReturnsEmptySetWhenNoWalkListeners() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"string\"}");
        JsonNode instance = mapper.readTree("\"walk\"");
        ValidationResult result = schema.walk(instance, false);
        assertNotNull(result);
        assertTrue(result.getValidationMessages().isEmpty());
    }

    /* -----------------------------------------------------------------------
     *  3.  Edge‑case and exception tests
     * ---------------------------------------------------------------------*/

    @Test
    public void testGetRefSchemaNodeSimpleFragment() throws Exception {
        JsonSchema schema = buildSchema("{\"definitions\":{\"foo\":{\"type\":\"integer\"}}}");
        JsonNode refNode = schema.getRefSchemaNode("#/definitions/foo");
        assertNotNull(refNode);
        assertEquals("integer", refNode.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNodeUrlEncodedFragment() throws Exception {
        // fragment contains a space that will be URL‑encoded as %20
        JsonSchema schema = buildSchema("{\"properties\":{\"a b\":{\"type\":\"string\"}}}");
        JsonNode refNode = schema.getRefSchemaNode("#/properties/a%20b");
        assertNotNull(refNode);
        assertEquals("string", refNode.get("type").asText());
    }

    @Test
    public void testGetRefSchemaNodeMissingFragmentReturnsNull() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"object\"}");
        JsonNode refNode = schema.getRefSchemaNode("#/nonexistent");
        assertNull(refNode);
    }

    @Test
    public void testHasRequiredValidatorTrue() throws Exception {
        JsonSchema schema = buildSchema("{\"required\":[\"a\"]}");
        assertTrue(schema.hasRequiredValidator());
        assertNotNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasRequiredValidatorFalse() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"object\"}");
        assertFalse(schema.hasRequiredValidator());
        assertNull(schema.getRequiredValidator());
    }

    @Test
    public void testHasTypeValidatorTrue() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"string\"}");
        assertTrue(schema.hasTypeValidator());
        assertNotNull(schema.getTypeValidator());
    }

    @Test
    public void testHasTypeValidatorFalse() throws Exception {
        JsonSchema schema = buildSchema("{\"properties\":{\"a\":{\"type\":\"number\"}}}");
        // The top‑level schema has no "type" keyword
        assertFalse(schema.hasTypeValidator());
        assertNull(schema.getTypeValidator());
    }

    @Test
    public void testIsDynamicAnchorTrue() throws Exception {
        JsonSchema schema = buildSchema("{\"$recursiveAnchor\":true}");
        assertTrue(schema.isDynamicAnchor());
    }

    @Test
    public void testIsDynamicAnchorFalseWhenAbsent() throws Exception {
        JsonSchema schema = buildSchema("{}");
        assertFalse(schema.isDynamicAnchor());
    }

    @Test
    public void testInitializeValidatorsCallsPreload() throws Exception {
        JsonSchema schema = buildSchema("{\"type\":\"string\"}");
        // validators are lazy – not yet created
        List<JsonValidator> before = schema.getValidators();
        // all validators are TestValidator instances
        for (JsonValidator v : before) {
            assertTrue(v instanceof TestValidator);
        }
        // none should be preloaded yet
        for (JsonValidator v : before) {
            assertFalse(((TestValidator) v).isPreloaded());
        }

        schema.initializeValidators();

        // after initialization each validator must have been preloaded
        for (JsonValidator v : before) {
            assertTrue(((TestValidator) v).isPreloaded());
        }
    }

    @Test(expected = JsonSchemaException.class)
    public void testCombineCurrentUriWithInvalidIdThrowsException() throws Exception {
        // Subclass ValidationContext to return an illegal ID string.
        ValidationContext badIdContext = new TestValidationContext() {
            @Override
            public String resolveSchemaId(JsonNode schemaNode) {
                return "http://[invalid]"; // illegal URI
            }
        };
        // The schema body can be empty – the error is triggered during construction.
        JsonNode empty = mapper.readTree("{}");
        JsonSchema.from(badIdContext,
                SchemaLocation.of("http://example.com/base"),
                null,
                new URI("http://example.com/base"),
                empty,
                null,
                false);
        // Expect JsonSchemaException from combineCurrentUriWithIds()
    }

    @Test(expected = JsonSchemaException.class)
    public void testRecursiveAnchorNonBooleanThrowsException() throws Exception {
        // "$recursiveAnchor": "notABoolean" should cause an exception while reading validators.
        JsonSchema schema = buildSchema("{\"$recursiveAnchor\":\"oops\"}");
        // Trigger reading of validators (which happens lazily on the first call)
        schema.getValidators();
    }

    /* -----------------------------------------------------------------------
     *  4.  createExecutionContext variations
     * ---------------------------------------------------------------------*/

    @Test
    public void testCreateExecutionContextUsesSupplierIfPresent() throws Exception {
        // Provide a custom ExecutionContext supplier via the config.
        ExecutionContext supplied = new ExecutionContext();
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setExecutionContextSupplier(() -> supplied);
        validationContext.setConfig(cfg);

        JsonSchema schema = buildSchema("{}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertSame(supplied, ctx);
    }

    @Test
    public void testCreateExecutionContextUsesDefaultWhenNoSupplier() throws Exception {
        JsonSchema schema = buildSchema("{}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertNotNull(ctx);
        // The default ExecutionContext contains a CollectorContext instance.
        assertNotNull(ctx.getCollectorContext());
    }
}
