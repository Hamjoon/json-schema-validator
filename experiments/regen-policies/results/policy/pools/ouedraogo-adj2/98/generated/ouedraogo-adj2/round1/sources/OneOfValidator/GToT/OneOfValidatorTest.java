package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * <p>The tests are written without any external mocking framework – all
 * collaborators are concrete test‑doubles that extend the library classes
 * using only the public / protected APIs that appear in the supplied source
 * snippets.</p>
 *
 * <p>Each test focuses on a particular behaviour of {@code OneOfValidator}:
 *
 * <ul>
 *   <li>basic validation where exactly one subschema matches;</li>
 *   <li>no subschema matches;</li>
 *   <li>more than one subschema matches (including short‑circuit logic);</li>
 *   <li>the {@code canShortCircuit()} flag when the parent schema contains an
 *       {@code unevaluatedProperties} validator;</li>
 *   <li>the {@code reportChildErrors()} decision based on the fail‑fast flag.</li>
 * </ul>
 *
 * The helper classes {@code TestJsonSchema}, {@code TestValidationContext},
 * {@code DummyValidator} and {@code TestableOneOfValidator} are defined at the
 * bottom of this file.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ExecutionContext executionContext;

    @Before
    public void setUp() {
        // a fresh execution context for every test
        this.executionContext = new ExecutionContext();
    }

    /**
     * Helper that creates a {@code OneOfValidator} from an array of schema
     * objects.  Each child object may contain a boolean field {@code "valid"}
     * that tells the test‑double {@code TestJsonSchema} whether the subschema
     * should succeed.
     */
    private OneOfValidator createValidator(ArrayNode schemaArray,
                                            JsonSchema parentSchema,
                                            TestValidationContext testContext) throws Exception {
        SchemaLocation location = SchemaLocation.DOCUMENT;
        JsonNodePath path = new JsonNodePath(JsonNodePath.PathType.ROOT);
        // The constructor will invoke {@code testContext.newSchema(...)} for each
        // element of {@code schemaArray} and store the returned {@code TestJsonSchema}
        // instances in the private {@code schemas} list.
        return new TestableOneOfValidator(location, path, schemaArray, parentSchema, testContext);
    }

    @Test
    public void testValidate_ExactlyOneValid() throws Exception {
        // child[0] → invalid, child[1] → valid
        ArrayNode array = MAPPER.createArrayNode();
        array.add(MAPPER.createObjectNode().put("valid", false));
        array.add(MAPPER.createObjectNode().put("valid", true));

        JsonSchema parent = new TestJsonSchema(null, Collections.emptyList()); // no parent validators
        TestValidationContext ctx = new TestValidationContext();

        OneOfValidator validator = createValidator(array, parent, ctx);

        JsonNode instance = MAPPER.readTree("\"any string\"");
        Set<ValidationMessage> result = validator.validate(executionContext, instance,
                                                          instance, new JsonNodePath(JsonNodePath.PathType.ROOT));

        // Exactly one subschema matches → no error
        assertTrue("Expected no validation errors", result.isEmpty());
    }

    @Test
    public void testValidate_NoValidSchema() throws Exception {
        // both children invalid
        ArrayNode array = MAPPER.createArrayNode();
        array.add(MAPPER.createObjectNode().put("valid", false));
        array.add(MAPPER.createObjectNode().put("valid", false));

        JsonSchema parent = new TestJsonSchema(null, Collections.emptyList());
        TestValidationContext ctx = new TestValidationContext();

        OneOfValidator validator = createValidator(array, parent, ctx);

        JsonNode instance = MAPPER.readTree("123");
        Set<ValidationMessage> result = validator.validate(executionContext, instance,
                                                          instance, new JsonNodePath(JsonNodePath.PathType.ROOT));

        // No subschema matches → a single "oneOf" error
        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertEquals("oneOf", msg.getMessageKey());
        assertEquals("0", msg.getArguments()[0]); // number of valid schemas
    }

    @Test
    public void testValidate_MultipleValid_WithShortCircuit() throws Exception {
        // both children valid – short‑circuit should stop after the second match
        ArrayNode array = MAPPER.createArrayNode();
        array.add(MAPPER.createObjectNode().put("valid", true));
        array.add(MAPPER.createObjectNode().put("valid", true));

        JsonSchema parent = new TestJsonSchema(null, Collections.emptyList());
        TestValidationContext ctx = new TestValidationContext();

        // use the testable subclass to expose the protected {@code canShortCircuit()}
        TestableOneOfValidator validator = (TestableOneOfValidator) createValidator(array, parent, ctx);
        assertTrue("canShortCircuit should be true by default", validator.exposedCanShortCircuit());

        JsonNode instance = MAPPER.readTree("null");
        Set<ValidationMessage> result = validator.validate(executionContext, instance,
                                                          instance, new JsonNodePath(JsonNodePath.PathType.ROOT));

        // More than one valid schema → an error with indexes listed
        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertEquals("oneOf.indexes", msg.getMessageKey());
        assertEquals("2", msg.getArguments()[0]);               // number of valid schemas
        assertEquals("0, 1", msg.getArguments()[1]);           // the indexes
    }

    @Test
    public void testCanShortCircuit_FalseWhenParentHasUnevaluatedValidator() throws Exception {
        // both children valid – but parent contains an "unevaluatedProperties" validator,
        // therefore short‑circuit must be disabled.
        ArrayNode array = MAPPER.createArrayNode();
        array.add(MAPPER.createObjectNode().put("valid", true));
        array.add(MAPPER.createObjectNode().put("valid", true));

        // parent with a dummy validator that reports the keyword "unevaluatedProperties"
        DummyValidator unevaluated = new DummyValidator("unevaluatedProperties");
        JsonSchema parent = new TestJsonSchema(null, Collections.singletonList(unevaluated));

        TestValidationContext ctx = new TestValidationContext();

        TestableOneOfValidator validator = (TestableOneOfValidator) createValidator(array, parent, ctx);
        assertFalse("canShortCircuit must be false when parent has unevaluated validator",
                    validator.exposedCanShortCircuit());

        JsonNode instance = MAPPER.readTree("{}");
        Set<ValidationMessage> result = validator.validate(executionContext, instance,
                                                          instance, new JsonNodePath(JsonNodePath.PathType.ROOT));

        // Because short‑circuit is disabled we still end up with a "oneOf.indexes" error
        // (two schemas matched) – the behaviour of the validator after the flag is
        // exercised is the same as the previous test.
        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertEquals("oneOf.indexes", msg.getMessageKey());
    }

    @Test
    public void testReportChildErrors_RespectsFailFastFlag() throws Exception {
        // child[0] invalid, child[1] valid – we only need to check the decision made
        // by the protected {@code reportChildErrors()} method.
        ArrayNode array = MAPPER.createArrayNode();
        array.add(MAPPER.createObjectNode().put("valid", false));
        array.add(MAPPER.createObjectNode().put("valid", true));

        JsonSchema parent = new TestJsonSchema(null, Collections.emptyList());
        TestValidationContext ctx = new TestValidationContext();

        TestableOneOfValidator validator = (TestableOneOfValidator) createValidator(array, parent, ctx);

        // fail‑fast disabled (default) → child errors should be reported
        executionContext.setFailFast(false);
        assertTrue("With failFast = false child errors must be reported",
                   validator.exposedReportChildErrors(executionContext));

        // fail‑fast enabled → child errors must NOT be reported
        executionContext.setFailFast(true);
        assertFalse("With failFast = true child errors must NOT be reported",
                    validator.exposedReportChildErrors(executionContext));
    }

    /* --------------------------------------------------------------------- */
    /* Helper test‑double classes                                            */
    /* --------------------------------------------------------------------- */

    /**
     * Simple {@link JsonSchema} implementation that can be configured to be
     * either "valid" (returns an empty error set) or "invalid" (returns a
     * single dummy {@link ValidationMessage}).
     */
    private static class TestJsonSchema extends JsonSchema {
        private final boolean valid;
        private final List<JsonValidator> validators; // for parent schema only

        public TestJsonSchema(Boolean valid, List<JsonValidator> validators) {
            // all arguments required by the protected super‑constructor are set to null
            // because they are not used by the overridden {@code validate()} method.
            super(null, false, false, null, null, false, null, null,
                  null, null, null, null, null, null, null, null, null);
            this.valid = Boolean.TRUE.equals(valid);
            this.validators = validators != null ? validators : Collections.emptyList();
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode jsonNode, JsonNode rootNode,
                                                JsonNodePath instanceLocation) {
            if (valid) {
                return Collections.emptySet();
            }
            // create a minimal ValidationMessage – the content is not inspected
            ValidationMessage vm = ValidationMessage.builder()
                    .code("test.invalid")
                    .message("invalid according to test schema")
                    .build();
            return Collections.singleton(vm);
        }

        @Override
        public List<JsonValidator> getValidators() {
            return validators;
        }
    }

    /**
     * {@link ValidationContext} subclass that creates {@link TestJsonSchema}
     * instances for the child schemas of a {@code OneOfValidator}.  The child
     * node must contain a boolean field {@code "valid"} that determines the
     * behaviour of the returned schema.
     */
    private static class TestValidationContext extends ValidationContext {
        public TestValidationContext() {
            // all constructor arguments are set to {@code null} because the
            // overridden {@code newSchema} does not delegate to the original logic.
            super(null, null, null);
        }

        @Override
        public JsonSchema newSchema(SchemaLocation schemaLocation,
                                   JsonNodePath evaluationPath,
                                   JsonNode schemaNode,
                                   JsonSchema parentSchema) {
            boolean isValid = schemaNode.path("valid").asBoolean(false);
            // No validators are attached to child schemas.
            return new TestJsonSchema(isValid, Collections.emptyList());
        }
    }

    /**
     * Minimal {@link JsonValidator} implementation that only supplies a
     * {@code keyword}.  Used to simulate the presence of an
     * {@code unevaluatedProperties} (or {@code unevaluatedItems}) validator
     * in the parent schema.
     */
    private static class DummyValidator implements JsonValidator {
        private final String keyword;

        DummyValidator(String keyword) {
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
            return SchemaLocation.DOCUMENT;
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return new JsonNodePath(JsonNodePath.PathType.ROOT);
        }

        @Override
        public String getKeyword() {
            return keyword;
        }
    }

    /**
     * Exposes the protected methods of {@link OneOfValidator} that are needed
     * for the unit tests.
     */
    private static class TestableOneOfValidator extends OneOfValidator {
        public TestableOneOfValidator(SchemaLocation schemaLocation,
                                     JsonNodePath evaluationPath,
                                     JsonNode schemaNode,
                                     JsonSchema parentSchema,
                                     ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        }

        /** expose {@code reportChildErrors} */
        public boolean exposedReportChildErrors(ExecutionContext ctx) {
            return super.reportChildErrors(ctx);
        }

        /** expose {@code canShortCircuit} */
        public boolean exposedCanShortCircuit() {
            return super.canShortCircuit();
        }
    }
}
