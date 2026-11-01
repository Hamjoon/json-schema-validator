package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.stream.Stream;

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
     * Concrete subclass that implements the 4‑arg validate method
     */
    private static class TestValidator extends BaseJsonValidator {

        private final Set<ValidationMessage> toReturn;

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval, Set<ValidationMessage> toReturn) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
            this.toReturn = toReturn;
        }

        /**
         * The 4‑argument version that the 2‑argument {@code validate} method delegates to.
         * For the purpose of the test we simply return a pre‑configured set.
         */
        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            // In a real validator this would contain the validation logic.
            return toReturn;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise simple JSON nodes.
        schemaNode = JsonNodeFactory.instance.objectNode();
        testNode = JsonNodeFactory.instance.objectNode();
        // ExecutionContext can be instantiated directly – it does not hit external resources.
        executionContext = new ExecutionContext();
        // Create the concrete validator instance.
        validator = new TestValidator("testSchemaPath", schemaNode, mockParentSchema, mockValidatorType, mockValidationContext, false, expectedSet);
        // a valid empty schema object
        schemaNode = JsonNodeFactory.instance.objectNode();
        // a generic JSON object
        testNode = JsonNodeFactory.instance.objectNode();
        // simple, no‑arg ctor
        executionContext = new ExecutionContext();
        validator = new TestValidator("testSchemaPath", schemaNode, mockParentSchema, mockValidatorType, mockValidationContext, // suppressSubSchemaRetrieval = false
        false, dummyReturnSet);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        clearInvocations(mockParentSchema, mockValidatorType, mockValidationContext, expectedSet);
        // Ensure no stray interactions leak between tests.
        clearInvocations(mockParentSchema, mockValidatorType, mockValidationContext, dummyReturnSet);
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
        // Spy on the concrete validator so that we can verify internal method calls.
        TestValidator spyValidator = spy(validator);
        // Call the method under test.
        Set<ValidationMessage> result = spyValidator.validate(executionContext, testNode);
        // Verify delegation: the 4‑arg method should have been invoked with the expected arguments.
        verify(spyValidator, times(1)).validate(same(executionContext), same(testNode), same(testNode), eq(spyValidator.atRoot()));
        // The result should be exactly the set we configured the 4‑arg method to return.
        assertSame(expectedSet, result, "The returned set should be the one supplied by the 4‑arg validate method");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("nullArgumentProvider")
    void testValidateNullArguments(ExecutionContext ec, JsonNode node, String _description) {
        assertThrows(NullPointerException.class, () -> validator.validate(ec, node), "BaseJsonValidator.validate must fail fast with NPE when any argument is null");
    }

    /* ---------- Schema‑node null handling ---------- */
    @Test
    void testValidateReturnsEmptyWhenSchemaNodeIsNull() throws Exception {
        // simulate a validator without a schema
        setValidatorSchemaNode(null);
        Set<ValidationMessage> result = validator.validate(executionContext, testNode);
        assertNotNull(result, "Result set should never be null");
        assertTrue(result.isEmpty(), "When schemaNode is null the validator should return an empty set");
    }

    /* ---------- Invalid schema node type handling ---------- */
    @Test
    void testValidateThrowsWhenSchemaNodeIsArray() throws Exception {
        ArrayNode arraySchema = JsonNodeFactory.instance.arrayNode().add("type").add("string");
        setValidatorSchemaNode(arraySchema);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(executionContext, testNode), "Array schema nodes are illegal – an IllegalArgumentException must be thrown");
        assertTrue(ex.getMessage().contains("schemaNode"), "Exception message should mention 'schemaNode'");
    }

    @Test
    void testValidateThrowsWhenSchemaNodeIsScalar() throws Exception {
        TextNode scalarSchema = TextNode.valueOf("string");
        setValidatorSchemaNode(scalarSchema);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(executionContext, testNode), "Scalar (non‑object) schema nodes are illegal – an IllegalArgumentException must be thrown");
        assertTrue(ex.getMessage().contains("schemaNode"));
    }

    /* ---------- suppressSubSchemaRetrieval flag ---------- */
    @Test
    void testValidateDoesNotCallFetchSubSchemaWhenSuppressed() throws Exception {
        // Build a validator where suppressSubSchemaRetrieval = true.
        TestValidator suppressingValidator = new TestValidator("path", schemaNode, mockParentSchema, mockValidatorType, mockValidationContext, // <- suppress sub‑schema retrieval
        true, dummyReturnSet) {

            @Override
            protected JsonSchema fetchSubSchemaNode(ValidationContext vc) {
                // If this method is invoked the test should fail.
                fail("fetchSubSchemaNode must NOT be called when suppression is enabled");
                return null;
            }
        };
        Set<ValidationMessage> result = suppressingValidator.validate(executionContext, testNode);
        assertSame(dummyReturnSet, result, "Result should still be the predefined dummy set");
    }

    @Test
    void testValidateCallsFetchSubSchemaWhenNotSuppressed() throws Exception {
        // Spy on a validator that overrides fetchSubSchemaNode to record the call.
        TestValidator normalValidator = spy(new TestValidator("path", schemaNode, mockParentSchema, mockValidatorType, mockValidationContext, // suppression disabled
        false, dummyReturnSet) {

            @Override
            protected JsonSchema fetchSubSchemaNode(ValidationContext vc) {
                // Return null – the real validator would resolve a $ref if present.
                return null;
            }
        });
        Set<ValidationMessage> result = normalValidator.validate(executionContext, testNode);
        assertSame(dummyReturnSet, result);
        // Verify that the protected method was indeed invoked once.
        verify(normalValidator, times(1)).fetchSubSchemaNode(any());
    }
}
