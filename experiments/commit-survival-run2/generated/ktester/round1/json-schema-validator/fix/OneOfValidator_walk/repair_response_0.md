```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OneOfValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}.
 *
 * <p>The original test suite used Mockito for mocking and verification.
 * Mockito is not available in the execution environment, therefore the
 * tests have been rewritten to use lightweight hand‑crafted test doubles
 * and reflection.</p>
 */
@DisplayName("OneOfValidator walk() behaviour")
class OneOfValidatorWalkTest {

    /** shared mapper for building JSON nodes */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** the validator instance under test */
    private OneOfValidator validator;

    /** execution context passed to the validator */
    private ExecutionContext execContext;

    /** data node (the instance being validated) */
    private JsonNode dataNode;

    /** root node of the document */
    private JsonNode rootNode;

    /** JSON pointer location */
    private String at;

    /** two child schemas that will be injected into the validator */
    private RecordingJsonSchema childSchema0;
    private RecordingJsonSchema childSchema1;

    /** a simple ValidationContext stub – it is never consulted because we
     * inject the child schemas directly via reflection. */
    private ValidationContext dummyContext;

    @BeforeEach
    void setUp() throws Exception {
        // ---- simple objects -------------------------------------------------
        execContext = new ExecutionContext();
        dataNode = MAPPER.createObjectNode();
        rootNode = MAPPER.createObjectNode();
        at = "#";

        // ---- build a schema node that contains two child schemas ------------
        ArrayNode arrayNode = MAPPER.createArrayNode();

        ObjectNode child0Node = MAPPER.createObjectNode();
        child0Node.put("type", "string");
        arrayNode.add(child0Node);

        ObjectNode child1Node = MAPPER.createObjectNode();
        child1Node.put("type", "integer");
        arrayNode.add(child1Node);

        // ---- create a dummy ValidationContext (not used) --------------------
        dummyContext = new ValidationContext(null, null, null, null) {
            // no behaviour needed for these tests
        };

        // ---- construct the validator (it will create an empty internal list) ---
        validator = new OneOfValidator("testPath", arrayNode, null, dummyContext);

        // ---- create simple child schemas that record walk invocations ----------
        childSchema0 = new RecordingJsonSchema();
        childSchema1 = new RecordingJsonSchema();

        // ---- inject the child schemas into the private 'schemas' field ----------
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(validator, Arrays.asList(childSchema0, childSchema1));
    }

    /**
     * Helper validator that allows us to stub the {@code validate} method.
     */
    private static class StubValidateOneOfValidator extends OneOfValidator {
        private Set<ValidationMessage> stubResult = Collections.emptySet();

        StubValidateOneOfValidator(String path, JsonNode schemaNode, JsonSchema parent,
                                   ValidationContext ctx) {
            super(path, schemaNode, parent, ctx);
        }

        void setStubResult(Set<ValidationMessage> result) {
            this.stubResult = result;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at) {
            return stubResult;
        }
    }

    /**
     * Minimal JsonSchema implementation that records whether {@code walk}
     * was called.  All other behaviour is inert.
     */
    private static class RecordingJsonSchema extends JsonSchema {
        private boolean walked = false;

        RecordingJsonSchema() {
            // the protected constructor of JsonSchema expects a ValidationContext,
            // a schema node and a parent schema – we can safely pass nulls for the
            // purpose of these tests because the overridden methods do not use them.
            super(null, null, null);
        }

        @Override
        public Set<ValidationMessage> walk(ExecutionContext ctx,
                                           JsonNode node,
                                           JsonNode rootNode,
                                           String at,
                                           boolean shouldValidateSchema) {
            walked = true;
            // OneOfValidator does not aggregate child messages, so we return empty.
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at) {
            // not used in the tests that involve this class
            return Collections.emptySet();
        }

        boolean wasWalked() {
            return walked;
        }

        void reset() {
            walked = false;
        }
    }

    /* --------------------------------------------------------------------- *
     *  Tests
     * --------------------------------------------------------------------- */

    @Test
    @DisplayName("walk should delegate to validate when shouldValidateSchema == true")
    void walk_delegatesToValidateWhenFlagTrue() {
        // arrange – stub validator to return a known set
        StubValidateOneOfValidator stubValidator =
                new StubValidateOneOfValidator("testPath", MAPPER.createArrayNode(), null, dummyContext);
        Set<ValidationMessage> expected = new HashSet<>();
        expected.add(new ValidationMessage("dummy"));
        stubValidator.setStubResult(expected);

        // act
        Set<ValidationMessage> actual = stubValidator.walk(execContext, dataNode, rootNode, at, true);

        // assert
        assertSame(expected, actual, "walk must return exactly what validate() returns");
    }

    @Test
    @DisplayName("walk should return empty set when shouldValidateSchema == true and validate returns empty")
    void walk_trueFlagEmptyResult() {
        StubValidateOneOfValidator stubValidator =
                new StubValidateOneOfValidator("testPath", MAPPER.createArrayNode(), null, dummyContext);
        stubValidator.setStubResult(Collections.emptySet());

        Set<ValidationMessage> result = stubValidator.walk(execContext, dataNode, rootNode, at, true);
        assertTrue(result.isEmpty(), "Result should be empty when validate returns empty");
    }

    @Test
    @DisplayName("walk should invoke each child schema when shouldValidateSchema == false")
    void walk_falseFlagInvokesChildren() {
        // pre‑condition – children have not been walked yet
        assertFalse(childSchema0.wasWalked(), "child 0 not walked yet");
        assertFalse(childSchema1.wasWalked(), "child 1 not walked yet");

        // act
        Set<ValidationMessage> result = validator.walk(execContext, dataNode, rootNode, at, false);

        // assert – result must be empty and each child must have been walked once
        assertTrue(result.isEmpty(), "Result set must be empty when shouldValidateSchema is false");
        assertTrue(childSchema0.wasWalked(), "child 0 walk should have been called");
        assertTrue(childSchema1.wasWalked(), "child 1 walk should have been called");
    }

    @Test
    @DisplayName("walk with no child schemas returns empty set")
    void walk_falseFlagNoChildren() throws Exception {
        // construct a validator with an empty array node (no children)
        OneOfValidator emptyValidator = new OneOfValidator("emptyPath",
                MAPPER.createArrayNode(),
                null,
                dummyContext);

        Set<ValidationMessage> result = emptyValidator.walk(execContext, dataNode, rootNode, at, false);
        assertTrue(result.isEmpty(), "Result must be empty when there are no child schemas");
    }

    @Test
    @DisplayName("walk propagates RuntimeException thrown by a child schema")
    void walk_propagatesExceptionFromChild() throws Exception {
        // child that throws
        RecordingJsonSchema throwingChild = new RecordingJsonSchema() {
            @Override
            public Set<ValidationMessage> walk(ExecutionContext ctx,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at,
                                                boolean shouldValidateSchema) {
                throw new RuntimeException("child failure");
            }
        };

        // inject the throwing child as the sole schema
        OneOfValidator validatorWithThrow = new OneOfValidator("path",
                MAPPER.createArrayNode().add(MAPPER.createObjectNode().put("type", "string")),
                null,
                dummyContext);
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(validatorWithThrow, Collections.singletonList(throwingChild));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validatorWithThrow.walk(execContext, dataNode, rootNode, at, false));
        assertEquals("child failure", ex.getMessage());
    }

    @Test
    @DisplayName("walk forwards NullPointerException when node is null and flag true")
    void walk_nullNodePropagatesException() {
        StubValidateOneOfValidator stubValidator =
                new StubValidateOneOfValidator("testPath", MAPPER.createArrayNode(), null, dummyContext) {
                    @Override
                    public Set<ValidationMessage> validate(ExecutionContext ctx,
                                                            JsonNode node,
                                                            JsonNode rootNode,
                                                            String at) {
                        if (node == null) {
                            throw new NullPointerException("node is null");
                        }
                        return super.validate(ctx, node, rootNode, at);
                    }
                };

        assertThrows(NullPointerException.class,
                () -> stubValidator.walk(execContext, null, rootNode, at, true));
    }

    @Test
    @DisplayName("walk forwards NullPointerException when ExecutionContext is null and flag true")
    void walk_nullExecutionContextPropagatesException() {
        StubValidateOneOfValidator stubValidator =
                new StubValidateOneOfValidator("testPath", MAPPER.createArrayNode(), null, dummyContext) {
                    @Override
                    public Set<ValidationMessage> validate(ExecutionContext ctx,
                                                            JsonNode node,
                                                            JsonNode rootNode,
                                                            String at) {
                        if (ctx == null) {
                            throw new NullPointerException("executionContext is null");
                        }
                        return super.validate(ctx, node, rootNode, at);
                    }
                };

        assertThrows(NullPointerException.class,
                () -> stubValidator.walk(null, dataNode, rootNode, at, true));
    }

    @ParameterizedTest
    @MethodSource("provideValidateResults")
    @DisplayName("walk returns exactly what validate() returns (parameterized)")
    void walk_parameterizedValidateResult(Set<ValidationMessage> stubResult) {
        StubValidateOneOfValidator stubValidator =
                new StubValidateOneOfValidator("testPath", MAPPER.createArrayNode(), null, dummyContext);
        stubValidator.setStubResult(stubResult);

        Set<ValidationMessage> actual = stubValidator.walk(execContext, dataNode, rootNode, at, true);
        assertEquals(stubResult, actual);
    }

    private static Stream<Set<ValidationMessage>> provideValidateResults() {
        ValidationMessage dummyMsg = new ValidationMessage("dummy");
        return Stream.of(
                Collections.singleton(dummyMsg), // non‑empty
                Collections.emptySet()          // empty
        );
    }
}
```