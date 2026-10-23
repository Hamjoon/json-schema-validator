```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit‑tests for {@link BaseJsonValidator#validate(ExecutionContext, JsonNode)}.
 *
 * <p>These tests focus on the logic that lives in the 2‑argument {@code validate}
 * method of {@code BaseJsonValidator}.  All concrete validators extend this class
 * and implement the 4‑argument {@code validate} method – the tests therefore
 * use a tiny concrete subclass ({@link TestValidator}) that simply returns a
 * pre‑configured {@link Set} of {@link ValidationMessage}s.</p>
 *
 * <p>The test suite covers:</p>
 * <ul>
 *   <li>delegation to the 4‑argument method;</li>
 *   <li>fail‑fast behavior when either argument is {@code null};</li>
 *   <li>handling of a {@code null} {@code schemaNode} field (should return an empty set);</li>
 *   <li>rejection of an invalid {@code schemaNode} type (array or scalar) with {@link IllegalArgumentException}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_validate_Test {

    /* --------------------------------------------------- *
     *  Mocks and shared test data                         *
     * --------------------------------------------------- */
    @Mock JsonSchema mockParentSchema;
    @Mock ValidatorTypeCode mockValidatorType;
    @Mock ValidationContext mockValidationContext;

    /** The dummy set that our concrete validator will return. */
    @Mock Set<ValidationMessage> dummyReturnSet;

    /** Simple JSON nodes used in a few tests. */
    JsonNode schemaNode;   // will be replaced via reflection in some tests
    JsonNode testNode;

    /** Real {@link ExecutionContext}.  It has no heavyweight dependencies. */
    ExecutionContext executionContext;

    /** Concrete validator used for the delegation test. */
    TestValidator validator;

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator}.  It stores the
     * {@code Set<ValidationMessage>} that should be returned by the 4‑argument
     * {@code validate} method.
     */
    private static class TestValidator extends BaseJsonValidator {
        private final Set<ValidationMessage> toReturn;

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval,
                      Set<ValidationMessage> toReturn) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext,
                    suppressSubSchemaRetrieval);
            this.toReturn = toReturn;
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                   JsonNode node,
                                                   JsonNode rootNode,
                                                   String at) {
            // In a real validator this would contain the validation logic.
            return toReturn;
        }
    }

    /* --------------------------------------------------- *
     *  Test lifecycle                                      *
     * --------------------------------------------------- */
    @BeforeEach
    void setUp() {
        schemaNode = JsonNodeFactory.instance.objectNode();   // a valid empty schema object
        testNode   = JsonNodeFactory.instance.objectNode();   // a generic JSON object

        executionContext = new ExecutionContext();            // simple, no‑arg ctor

        validator = new TestValidator(
                "testSchemaPath",
                schemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false,                 // suppressSubSchemaRetrieval = false
                dummyReturnSet);
    }

    @AfterEach
    void tearDown() {
        // Ensure no stray interactions leak between tests.
        clearInvocations(mockParentSchema, mockValidatorType, mockValidationContext, dummyReturnSet);
    }

    /* --------------------------------------------------- *
     *  Helper methods                                      *
     * --------------------------------------------------- */
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

    /* --------------------------------------------------- *
     *  Test cases                                          *
     * --------------------------------------------------- */

    /** Delegation test – already provided in the original template. */
    @Test
    void testValidateDelegatesToFourArgMethod() {
        TestValidator spyValidator = spy(validator);

        Set<ValidationMessage> result = spyValidator.validate(executionContext, testNode);

        verify(spyValidator, times(1)).validate(
                same(executionContext),
                same(testNode),
                same(testNode),
                eq(spyValidator.atRoot()));

        assertSame(dummyReturnSet, result,
                "The 2‑arg validate method must return exactly what the 4‑arg version returns");
    }

    /* ---------- Null‑argument tests ---------- */
    private static Stream<Arguments> nullArgumentProvider() {
        return Stream.of(
                Arguments.of(null, null, "Both arguments null should NPE"),
                Arguments.of(new ExecutionContext(), null, "Null node should NPE"),
                Arguments.of(null, JsonNodeFactory.instance.objectNode(), "Null executionContext should NPE")
        );
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("nullArgumentProvider")
    void testValidateNullArguments(ExecutionContext ec, JsonNode node, String _description) {
        assertThrows(NullPointerException.class, () -> validator.validate(ec, node),
                "BaseJsonValidator.validate must fail fast with NPE when any argument is null");
    }

    /* ---------- Schema‑node null handling ---------- */
    @Test
    void testValidateReturnsEmptyWhenSchemaNodeIsNull() throws Exception {
        setValidatorSchemaNode(null);               // simulate a validator without a schema

        Set<ValidationMessage> result = validator.validate(executionContext, testNode);

        assertNotNull(result, "Result set should never be null");
        assertTrue(result.isEmpty(), "When schemaNode is null the validator should return an empty set");
    }

    /* ---------- Invalid schema node type handling ---------- */
    @Test
    void testValidateThrowsWhenSchemaNodeIsArray() throws Exception {
        ArrayNode arraySchema = JsonNodeFactory.instance.arrayNode()
                .add("type")
                .add("string");
        setValidatorSchemaNode(arraySchema);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(executionContext, testNode),
                "Array schema nodes are illegal – an IllegalArgumentException must be thrown");
        assertTrue(ex.getMessage().contains("schemaNode"), "Exception message should mention 'schemaNode'");
    }

    @Test
    void testValidateThrowsWhenSchemaNodeIsScalar() throws Exception {
        TextNode scalarSchema = TextNode.valueOf("string");
        setValidatorSchemaNode(scalarSchema);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(executionContext, testNode),
                "Scalar (non‑object) schema nodes are illegal – an IllegalArgumentException must be thrown");
        assertTrue(ex.getMessage().contains("schemaNode"));
    }

    /* ---------- suppressSubSchemaRetrieval flag ---------- */
    @Test
    void testValidateDoesNotCallFetchSubSchemaWhenSuppressed() throws Exception {
        // Build a validator where suppressSubSchemaRetrieval = true.
        TestValidator suppressingValidator = new TestValidator(
                "path",
                schemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                true,                         // <- suppress sub‑schema retrieval
                dummyReturnSet) {
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
        TestValidator normalValidator = spy(new TestValidator(
                "path",
                schemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false,                        // suppression disabled
                dummyReturnSet) {
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
```