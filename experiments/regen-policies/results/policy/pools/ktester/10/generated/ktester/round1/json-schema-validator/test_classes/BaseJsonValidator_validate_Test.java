package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_validate_Test {

    // ----- Mocks for constructor dependencies -----
    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode // the schema definition node
    schemaNode;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidatorTypeCode validatorType;

    @Mock
    ValidationContext validationContext;

    // ----- Object under test -----
    TestValidator validator;

    // ----- Helper factories -----
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Minimal concrete subclass that simply returns an empty validation result.
     */
    static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /**
         * The core validation logic for the abstract class.
         * For the purpose of this unit‑test we return an empty set,
         * allowing us to verify that the public {@code validate(ExecutionContext, JsonNode)}
         * method correctly delegates to this implementation.
         */
        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            return Collections.emptySet();
        }
    }

    /**
     * Concrete subclass that records the atRoot() call and returns a preset set.
     */
    static class DelegatingValidator extends BaseJsonValidator {

        final Set<ValidationMessage> toReturn;

        boolean atRootCalled = false;

        DelegatingValidator(Set<ValidationMessage> toReturn) {
            super(null, null, null, null, null, null);
            this.toReturn = toReturn;
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            // simply return the preset set
            return toReturn;
        }

        @Override
        protected JsonNodePath atRoot() {
            atRootCalled = true;
            return mock(JsonNodePath.class);
        }
    }

    static class AtRootTrackingValidator extends BaseJsonValidator {

        int atRootInvocations = 0;

        JsonNodePath capturedPath;

        JsonNodePath pathPassedToValidate;

        AtRootTrackingValidator() {
            super(null, null, null, null, null, null);
        }

        @Override
        protected JsonNodePath atRoot() {
            atRootInvocations++;
            capturedPath = mock(JsonNodePath.class);
            return capturedPath;
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            pathPassedToValidate = at;
            return Collections.emptySet();
        }
    }

    static class FixedResultValidator extends BaseJsonValidator {

        private final Set<ValidationMessage> result;

        FixedResultValidator(Set<ValidationMessage> result) {
            super(null, null, null, null, null, null);
            this.result = result;
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            return result;
        }
    }

    static class OriginalNodeCaptureValidator extends BaseJsonValidator {

        JsonNode capturedOriginalNode;

        OriginalNodeCaptureValidator() {
            super(null, null, null, null, null, null);
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            capturedOriginalNode = rootNode;
            return Collections.emptySet();
        }
    }

    static class ExceptionThrowingValidator extends BaseJsonValidator {

        ExceptionThrowingValidator() {
            super(null, null, null, null, null, null);
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            throw new RuntimeException("forced error");
        }
    }

    /**
     * Subclass that creates a ValidationMessage containing the node's class name.
     */
    static class NodeClassMessageValidator extends BaseJsonValidator {

        NodeClassMessageValidator() {
            super(null, null, null, null, null, null);
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            // Build a simple ValidationMessage – we use a mock to avoid dependence on its ctor.
            ValidationMessage vm = mock(ValidationMessage.class);
            when(vm.getMessage()).thenReturn("Node type: " + node.getNodeType().name());
            return Collections.singleton(vm);
        }
    }

    static class CountingValidator extends BaseJsonValidator {

        int invocationCount = 0;

        private final Set<ValidationMessage> result = Collections.singleton(mock(ValidationMessage.class));

        CountingValidator() {
            super(null, null, null, null, null, null);
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            invocationCount++;
            return result;
        }
    }

    static class NullAtRootValidator extends BaseJsonValidator {

        NullAtRootValidator() {
            super(null, null, null, null, null, null);
        }

        @Override
        protected JsonNodePath atRoot() {
            // deliberately return null
            return null;
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            // force a dereference of the path – this will NPE because at is null
            at.toString();
            return Collections.emptySet();
        }
    }

    static class AtRootExceptionValidator extends BaseJsonValidator {

        AtRootExceptionValidator() {
            super(null, null, null, null, null, null);
        }

        @Override
        protected JsonNodePath atRoot() {
            throw new IllegalStateException("atRoot failed");
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            // never reached
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the concrete validator with the mocked collaborators.
        validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
    }

    @Test
    void testValidateReturnsEmptySet() {
        // Arrange
        ExecutionContext executionContext = new ExecutionContext();
        // the instance to be validated
        JsonNode dataNode = mock(JsonNode.class);
        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode);
        // Assert
        assertNotNull(result, "Resulting set should never be null");
        assertTrue(result.isEmpty(), "Expected an empty validation result");
    }

    /* --------------------------------------------------------------
     *  1. Delegation – the public method must forward to the 4‑arg overload
     * -------------------------------------------------------------- */
    @Test
    void testValidateDelegatesAndReturnsResultFromOverload() {
        // a deterministic set that the overload will return
        ValidationMessage vm = mock(ValidationMessage.class);
        Set<ValidationMessage> expected = Collections.singleton(vm);
        validator = new DelegatingValidator(expected);
        ExecutionContext ctx = mock(ExecutionContext.class);
        ObjectNode data = MAPPER.createObjectNode().put("name", "value");
        Set<ValidationMessage> actual = validator.validate(ctx, data);
        assertSame(expected, actual, "The result must be exactly the set returned by the overload");
        assertTrue(((DelegatingValidator) validator).atRootCalled, "atRoot() should have been invoked");
    }

    /* --------------------------------------------------------------
     *  2. Null argument handling – NPE must be thrown before delegation
     * -------------------------------------------------------------- */
    @Test
    void testValidateThrowsWhenExecutionContextIsNull() {
        // any concrete subclass works
        validator = new TestValidator();
        ObjectNode data = MAPPER.createObjectNode();
        assertThrows(NullPointerException.class, () -> validator.validate(null, data), "A null ExecutionContext should cause NPE");
    }

    @Test
    void testValidateThrowsWhenNodeIsNull() {
        validator = new TestValidator();
        ExecutionContext ctx = mock(ExecutionContext.class);
        assertThrows(NullPointerException.class, () -> validator.validate(ctx, null), "A null JsonNode should cause NPE");
    }

    /* --------------------------------------------------------------
     *  3. Root path handling – atRoot() must be called exactly once and
     *     its result forwarded as the fourth argument.
     * -------------------------------------------------------------- */
    @Test
    void testAtRootIsInvokedOnceAndPassedToOverload() {
        validator = new AtRootTrackingValidator();
        ExecutionContext ctx = mock(ExecutionContext.class);
        ObjectNode data = MAPPER.createObjectNode();
        validator.validate(ctx, data);
        AtRootTrackingValidator v = (AtRootTrackingValidator) validator;
        assertEquals(1, v.atRootInvocations, "atRoot() must be called exactly once");
        assertSame(v.capturedPath, v.pathPassedToValidate, "The JsonNodePath returned by atRoot() must be the one passed to the overload");
    }

    /* --------------------------------------------------------------
     *  4. Integration – concrete subclass returns a non‑empty set
     * -------------------------------------------------------------- */
    @Test
    void testSubclassReturnsNonEmptySet() {
        ValidationMessage vm = mock(ValidationMessage.class);
        Set<ValidationMessage> expected = Collections.singleton(vm);
        validator = new FixedResultValidator(expected);
        ExecutionContext ctx = mock(ExecutionContext.class);
        ObjectNode data = MAPPER.createObjectNode().put("age", 30);
        Set<ValidationMessage> actual = validator.validate(ctx, data);
        assertEquals(expected, actual, "Validator should return the messages produced by the subclass");
    }

    /* --------------------------------------------------------------
     *  5. Propagation of original node reference (third argument)
     * -------------------------------------------------------------- */
    @Test
    void testOriginalNodeReferenceIsPreserved() {
        validator = new OriginalNodeCaptureValidator();
        ExecutionContext ctx = mock(ExecutionContext.class);
        ObjectNode data = MAPPER.createObjectNode().put("key", "value");
        validator.validate(ctx, data);
        OriginalNodeCaptureValidator v = (OriginalNodeCaptureValidator) validator;
        assertSame(data, v.capturedOriginalNode, "The third argument (originalNode) must be the same instance supplied to validate");
    }

    /* --------------------------------------------------------------
     *  6. Exception propagation from the overload
     * -------------------------------------------------------------- */
    @Test
    void testExceptionFromOverloadIsPropagated() {
        validator = new ExceptionThrowingValidator();
        ExecutionContext ctx = mock(ExecutionContext.class);
        ObjectNode data = MAPPER.createObjectNode();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.validate(ctx, data), "RuntimeException from overload must propagate");
        assertEquals("forced error", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  7. Handling of non‑object JsonNode (TextNode)
     * -------------------------------------------------------------- */
    @Test
    void testTextNodeIsPassedThroughAndMessageContainsClassName() {
        validator = new NodeClassMessageValidator();
        ExecutionContext ctx = mock(ExecutionContext.class);
        TextNode textNode = new TextNode("sample");
        Set<ValidationMessage> result = validator.validate(ctx, textNode);
        assertEquals(1, result.size(), "Exactly one validation message expected");
        ValidationMessage vm = result.iterator().next();
        // we expect the message to contain the word "TEXT"
        assertTrue(vm.getMessage().contains("TEXT"), () -> "Message should contain node type \"TEXT\", got: " + vm.getMessage());
    }

    /* --------------------------------------------------------------
     *  8. Idempotent delegation – two calls produce equal results and invoke overload twice
     * -------------------------------------------------------------- */
    @Test
    void testRepeatedCallsDelegateEachTimeAndReturnEqualSets() {
        validator = new CountingValidator();
        ExecutionContext ctx = mock(ExecutionContext.class);
        ObjectNode data = MAPPER.createObjectNode().put("id", 1);
        Set<ValidationMessage> first = validator.validate(ctx, data);
        Set<ValidationMessage> second = validator.validate(ctx, data);
        CountingValidator v = (CountingValidator) validator;
        assertEquals(2, v.invocationCount, "Overload should be called once per validate invocation");
        assertEquals(first, second, "Both calls must return equal Set instances");
    }

    /* --------------------------------------------------------------
     *  9. atRoot() returns null – overload dereferences it and throws NPE
     * -------------------------------------------------------------- */
    @Test
    void testAtRootReturnsNullLeadsToNPE() {
        validator = new NullAtRootValidator();
        ExecutionContext ctx = mock(ExecutionContext.class);
        ObjectNode data = MAPPER.createObjectNode().put("field", "value");
        assertThrows(NullPointerException.class, () -> validator.validate(ctx, data), "When atRoot() returns null, the overload should throw NPE");
    }

    /* --------------------------------------------------------------
     * 10. atRoot() throws an exception – must propagate unchanged
     * -------------------------------------------------------------- */
    @Test
    void testAtRootExceptionIsPropagated() {
        validator = new AtRootExceptionValidator();
        ExecutionContext ctx = mock(ExecutionContext.class);
        ObjectNode data = MAPPER.createObjectNode().put("foo", 123);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(ctx, data), "Exception from atRoot() must propagate");
        assertEquals("atRoot failed", ex.getMessage());
    }
}
