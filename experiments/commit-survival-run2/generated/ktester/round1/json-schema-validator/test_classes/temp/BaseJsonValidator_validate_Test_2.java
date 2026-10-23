package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import java.util.Set;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import java.util.HashSet;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_validate_Test {

    /**
     * Mocks and test data
     */
    @Mock
    JsonSchema mockParentSchema;

    @Mock
    ValidatorTypeCode mockValidatorType;

    @Mock
    ValidationContext mockValidationContext;

    @Mock
    Set<ValidationMessage> // the set that the 4‑arg validate will return
    expectedSet;

    // dummy schema node
    JsonNode schemaNode;

    // real execution context (no heavy dependencies)
    ExecutionContext executionContext;

    // node that will be validated
    JsonNode testNode;

    /**
     * Concrete validator used for the test
     */
    TestValidator validator;

    /**
     * The dummy set that our concrete validator will return.
     */
    @Mock
    Set<ValidationMessage> dummyReturnSet;

    /**
     * Simple concrete validator that records the arguments passed to the
     * 4‑argument {@code validate} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        private final Set<ValidationMessage> toReturn;

        // captured arguments for assertions
        private ExecutionContext capturedEc;

        private JsonNode capturedNode;

        private JsonNode capturedRoot;

        private String capturedAt;

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval, Set<ValidationMessage> toReturn) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
            this.toReturn = toReturn;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            this.capturedEc = executionContext;
            this.capturedNode = node;
            this.capturedRoot = rootNode;
            this.capturedAt = at;
            return toReturn;
        }

        // getters for the captured values
        ExecutionContext getCapturedEc() {
            return capturedEc;
        }

        JsonNode getCapturedNode() {
            return capturedNode;
        }

        JsonNode getCapturedRoot() {
            return capturedRoot;
        }

        String getCapturedAt() {
            return capturedAt;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // initialise simple JSON nodes
        schemaNode = JsonNodeFactory.instance.objectNode();
        testNode = JsonNodeFactory.instance.objectNode();
        // a fresh execution context – no heavy dependencies
        executionContext = new ExecutionContext();
        // the set that the validator will return
        dummyReturnSet = new HashSet<>();
        // create the validator instance (suppressSubSchemaRetrieval = false)
        validator = new TestValidator("testSchemaPath", schemaNode, // parentSchema – not needed for these tests
        null, // validatorType – not needed
        null, // validationContext – not needed
        null, false, dummyReturnSet);
    }

    /**
     * Uses reflection to set the {@code schemaNode} field of the validator.
     * This is required for the “null schema” and “invalid schema” test cases
     * because {@code schemaNode} is {@code protected} in the base class.
     */
    private void setValidatorSchemaNode(JsonNode newNode) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("schemaNode");
        f.setAccessible(true);
        f.set(validator, newNode);
    }

    /* ---------- Null‑argument tests ---------- */
    private static Stream<Arguments> nullArgumentProvider() {
        return Stream.of(Arguments.of(null, null, "Both arguments null should NPE"), Arguments.of(new ExecutionContext(), null, "Null node should NPE"), Arguments.of(null, JsonNodeFactory.instance.objectNode(), "Null executionContext should NPE"));
    }

    /**
     * Verify that the 2‑argument {@code validate} method correctly delegates to the
     * 4‑argument version, passing the same node for both {@code node} and {@code rootNode},
     * and using the path returned by {@code atRoot()}.
     */
    @Test
    void testValidateDelegatesToFourArgMethod() {
        Set<ValidationMessage> result = validator.validate(executionContext, testNode);
        Assertions.assertSame(dummyReturnSet, result, "Returned set must be the one supplied by the 4‑arg method");
        Assertions.assertSame(executionContext, validator.getCapturedEc(), "ExecutionContext should be passed through");
        Assertions.assertSame(testNode, validator.getCapturedNode(), "Node argument should be passed through");
        Assertions.assertSame(testNode, validator.getCapturedRoot(), "RootNode argument should be the same as node");
        Assertions.assertEquals(validator.atRoot(), validator.getCapturedAt(), "Path argument should be the value returned by atRoot()");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("nullArgumentProvider")
    void testValidateNullArguments(ExecutionContext ec, JsonNode node, String _description) {
        Assertions.assertThrows(NullPointerException.class, () -> validator.validate(ec, node), "BaseJsonValidator.validate must fail fast with NPE when any argument is null");
    }

    /* ---------- Schema‑node null handling ---------- */
    @Test
    void testValidateReturnsEmptyWhenSchemaNodeIsNull() throws Exception {
        setValidatorSchemaNode(null);
        Set<ValidationMessage> result = validator.validate(executionContext, testNode);
        Assertions.assertNotNull(result, "Result set should never be null");
        Assertions.assertTrue(result.isEmpty(), "When schemaNode is null the validator should return an empty set");
    }

    /* ---------- Invalid schema node type handling ---------- */
    @Test
    void testValidateThrowsWhenSchemaNodeIsArray() throws Exception {
        ArrayNode arraySchema = JsonNodeFactory.instance.arrayNode().add("type").add("string");
        setValidatorSchemaNode(arraySchema);
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> validator.validate(executionContext, testNode), "Array schema nodes are illegal – an IllegalArgumentException must be thrown");
        Assertions.assertTrue(ex.getMessage().contains("schemaNode"), "Exception message should mention 'schemaNode'");
    }

    @Test
    void testValidateThrowsWhenSchemaNodeIsScalar() throws Exception {
        TextNode scalarSchema = TextNode.valueOf("string");
        setValidatorSchemaNode(scalarSchema);
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> validator.validate(executionContext, testNode), "Scalar (non‑object) schema nodes are illegal – an IllegalArgumentException must be thrown");
        Assertions.assertTrue(ex.getMessage().contains("schemaNode"));
    }

    /* ---------- suppressSubSchemaRetrieval flag ---------- */
    @Test
    void testValidateDoesNotCallFetchSubSchemaWhenSuppressed() throws Exception {
        // validator with suppression enabled; override fetchSubSchemaNode to fail if called
        TestValidator suppressingValidator = new TestValidator("path", schemaNode, null, null, null, true, dummyReturnSet) {

            @Override
            protected JsonSchema fetchSubSchemaNode(ValidationContext vc) {
                Assertions.fail("fetchSubSchemaNode must NOT be called when suppression is enabled");
                // unreachable
                return null;
            }
        };
        Set<ValidationMessage> result = suppressingValidator.validate(executionContext, testNode);
        Assertions.assertSame(dummyReturnSet, result, "Result should still be the predefined dummy set");
    }

    @Test
    void testValidateCallsFetchSubSchemaWhenNotSuppressed() throws Exception {
        // validator with suppression disabled; override fetchSubSchemaNode to record invocation
        final boolean[] called = { false };
        TestValidator normalValidator = new TestValidator("path", schemaNode, null, null, null, false, dummyReturnSet) {

            @Override
            protected JsonSchema fetchSubSchemaNode(ValidationContext vc) {
                called[0] = true;
                // returning null is fine for the test
                return null;
            }
        };
        Set<ValidationMessage> result = normalValidator.validate(executionContext, testNode);
        Assertions.assertSame(dummyReturnSet, result, "Result should be the predefined dummy set");
        Assertions.assertTrue(called[0], "fetchSubSchemaNode should have been invoked when suppression is disabled");
    }
}
