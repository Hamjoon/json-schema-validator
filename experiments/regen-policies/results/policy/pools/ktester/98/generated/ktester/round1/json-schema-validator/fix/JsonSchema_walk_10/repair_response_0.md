```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Test suite for {@link JsonSchema#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * <p>This version does not depend on Mockito. Simple stub implementations are used
 * for {@link KeywordWalkListenerRunner} and {@link JsonValidator}.</p>
 */
@DisplayName("JsonSchema walk tests (stubbed listeners/validators)")
class JsonSchema_walk_10_Test {

    /** The schema instance under test. */
    private JsonSchema schema;

    /** Execution context passed to {@code walk}. */
    private ExecutionContext executionContext;

    /** Stubbed listener runner handling pre‑ and post‑walk callbacks. */
    private StubKeywordWalkListenerRunner stubListenerRunner;

    /** A single stub validator that will be returned from {@code getValidators()}. */
    private StubValidator stubValidator;

    /** A dummy validation message returned by the stub validator. */
    private ValidationMessage dummyMessage;

    /** Sample JSON nodes used as parameters for the walk call. */
    private JsonNode node;
    private JsonNode rootNode;
    private JsonNodePath instancePath;

    /** Simple enum used by a parameterised test. */
    private enum ValidatorResult { NON_EMPTY, EMPTY, NULL }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* -----------------------------------------------------------------
         * 1. Create a minimal real JsonSchema instance.
         * ----------------------------------------------------------------- */
        SchemaValidatorsConfig config = new SchemaValidatorsConfig(); // will be filled later
        ValidationContext realVc = new ValidationContext(
                JsonMetaSchema.getV202012(),
                JsonSchemaFactory.getInstance(VersionFlag.V202012),
                config);
        ObjectNode emptySchemaNode = JsonNodeFactory.instance.objectNode();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        schema = factory.getSchema(emptySchemaNode);               // empty schema

        /* -----------------------------------------------------------------
         * 2. Prepare stub listener runner.
         * ----------------------------------------------------------------- */
        stubListenerRunner = new StubKeywordWalkListenerRunner();
        config.setKeywordWalkListenerRunner(stubListenerRunner);   // plug into config

        /* -----------------------------------------------------------------
         * 3. Prepare a stub validator.
         * ----------------------------------------------------------------- */
        JsonNodePath validatorPath = new JsonNodePath(PathType.JSON_POINTER);
        // the stub will use the keyword name "dummyKeyword"
        whenValidatorPathName(validatorPath, -1, "dummyKeyword");
        stubValidator = new StubValidator(validatorPath);
        dummyMessage = ValidationMessage.of("dummy error");
        stubValidator.setResult(Set.of(dummyMessage));

        /* -----------------------------------------------------------------
         * 4. Inject the stub validator into the schema (via reflection).
         * ----------------------------------------------------------------- */
        setValidators(List.of(stubValidator));

        /* -----------------------------------------------------------------
         * 5. Common objects used in the test methods.
         * ----------------------------------------------------------------- */
        executionContext = new ExecutionContext();
        node = JsonNodeFactory.instance.objectNode().put("a", "value");
        rootNode = node.deepCopy();
        instancePath = new JsonNodePath(PathType.JSON_POINTER);
    }

    /** Helper to replace the private {@code validators} field. */
    private void setValidators(List<JsonValidator> list) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        f.set(schema, list);
    }

    /** Helper to set a name on a JsonNodePath mock (used only for the stub). */
    private static void whenValidatorPathName(JsonNodePath path, int index, String name) throws Exception {
        // JsonNodePath is a concrete class; we set its internal name list via reflection.
        // The class stores the names in a private List<String> called "names".
        Field namesField = JsonNodePath.class.getDeclaredField("names");
        namesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) namesField.get(path);
        // ensure capacity
        while (names.size() <= index) {
            names.add(null);
        }
        names.set(index, name);
    }

    /* --------------------------------------------------------------
     * 1. Simple happy‑path test.
     * -------------------------------------------------------------- */
    @Test
    void walkShouldDelegateToValidatorAndInvokeListeners() {
        // Act
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);

        // Assert result
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(dummyMessage));

        // Verify listener calls
        assertEquals(1, stubListenerRunner.preCalls.size());
        assertEquals(1, stubListenerRunner.postCalls.size());

        StubKeywordWalkListenerRunner.Call pre = stubListenerRunner.preCalls.get(0);
        assertEquals("dummyKeyword", pre.keyword);
        assertSame(executionContext, pre.ctx);
        assertSame(node, pre.node);
        assertSame(rootNode, pre.rootNode);
        assertSame(instancePath, pre.path);
        assertSame(schema, pre.schema);
        assertSame(stubValidator, pre.validator);

        StubKeywordWalkListenerRunner.Call post = stubListenerRunner.postCalls.get(0);
        assertEquals("dummyKeyword", post.keyword);
        assertSame(executionContext, post.ctx);
        assertSame(node, post.node);
        assertSame(rootNode, post.rootNode);
        assertSame(instancePath, post.path);
        assertSame(schema, post.schema);
        assertSame(stubValidator, post.validator);
        assertEquals(result, post.messages);
    }

    /* --------------------------------------------------------------
     * 2. Pre‑walk listener allows validation – different validator results.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "pre‑walk true, validator result = {0}")
    @EnumSource(ValidatorResult.class)
    void walk_preWalkAllowsVariousValidatorResults(ValidatorResult resultType) {
        // arrange
        switch (resultType) {
            case NON_EMPTY -> stubValidator.setResult(Set.of(ValidationMessage.of("dummy error")));
            case EMPTY -> stubValidator.setResult(Collections.emptySet());
            case NULL -> stubValidator.setResult(null);
        }
        stubListenerRunner.preResult = true; // ensure pre‑walk proceeds

        // act
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);

        // assert
        if (resultType == ValidatorResult.NON_EMPTY) {
            assertEquals(1, result.size());
            assertTrue(result.contains(ValidationMessage.of("dummy error")));
        } else {
            assertTrue(result.isEmpty());
        }

        // verify listener interaction
        assertEquals(1, stubListenerRunner.preCalls.size());
        assertEquals(1, stubListenerRunner.postCalls.size());
    }

    /* --------------------------------------------------------------
     * 3. Pre‑walk listener skips validation.
     * -------------------------------------------------------------- */
    @Test
    void walk_preWalkSkipsValidator() {
        stubListenerRunner.preResult = false; // skip

        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);

        assertTrue(result.isEmpty());
        // validator should not have been called – we check that its walk method was never invoked
        assertTrue(stubValidator.walkCalledCount == 0);
        assertEquals(1, stubListenerRunner.preCalls.size());
        assertEquals(1, stubListenerRunner.postCalls.size());
        // post‑walk must receive an empty set
        assertTrue(stubListenerRunner.postCalls.get(0).messages.isEmpty());
    }

    /* --------------------------------------------------------------
     * 4. Multiple validators with mixed outcomes.
     * -------------------------------------------------------------- */
    @Test
    void walk_multipleValidatorsMixedOutcomes() throws Exception {
        // validator A – returns non‑empty set
        StubValidator validatorA = new StubValidator(new JsonNodePath(PathType.JSON_POINTER));
        whenValidatorPathName(validatorA.getEvaluationPath(), -1, "keywordA");
        validatorA.setResult(Set.of(ValidationMessage.of("msgA")));
        stubListenerRunner.setPreResultForKeyword("keywordA", true);

        // validator B – pre‑walk returns false (skip)
        StubValidator validatorB = new StubValidator(new JsonNodePath(PathType.JSON_POINTER));
        whenValidatorPathName(validatorB.getEvaluationPath(), -1, "keywordB");
        stubListenerRunner.setPreResultForKeyword("keywordB", false);

        // validator C – returns non‑empty set
        StubValidator validatorC = new StubValidator(new JsonNodePath(PathType.JSON_POINTER));
        whenValidatorPathName(validatorC.getEvaluationPath(), -1, "keywordC");
        validatorC.setResult(Set.of(ValidationMessage.of("msgC")));
        stubListenerRunner.setPreResultForKeyword("keywordC", true);

        setValidators(List.of(validatorA, validatorB, validatorC));

        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(m -> "msgA".equals(m.getMessage())));
        assertTrue(result.stream().anyMatch(m -> "msgC".equals(m.getMessage())));

        // verify calls
        assertEquals(3, stubListenerRunner.preCalls.size());
        assertEquals(3, stubListenerRunner.postCalls.size());
        assertEquals(1, validatorA.walkCalledCount);
        assertEquals(0, validatorB.walkCalledCount);
        assertEquals(1, validatorC.walkCalledCount);
    }

    /* --------------------------------------------------------------
     * 5. Empty validators list.
     * -------------------------------------------------------------- */
    @Test
    void walk_emptyValidatorsList() throws Exception {
        setValidators(Collections.emptyList());

        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);

        assertTrue(result.isEmpty());
        assertTrue(stubListenerRunner.preCalls.isEmpty());
        assertTrue(stubListenerRunner.postCalls.isEmpty());
    }

    /* --------------------------------------------------------------
     * 6. Validator throws exception – post‑walk still executed.
     * -------------------------------------------------------------- */
    @Test
    void walk_validatorThrowsException() {
        RuntimeException boom = new RuntimeException("boom");
        stubValidator.setException(boom);
        stubListenerRunner.preResult = true;

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(executionContext, node, rootNode, instancePath, true));
        assertSame(boom, ex);

        // pre‑walk called, post‑walk called with empty set
        assertEquals(1, stubListenerRunner.preCalls.size());
        assertEquals(1, stubListenerRunner.postCalls.size());
        assertTrue(stubListenerRunner.postCalls.get(0).messages.isEmpty());
    }

    /* --------------------------------------------------------------
     * 7. Pre‑walk listener throws exception – validator not invoked.
     * -------------------------------------------------------------- */
    @Test
    void walk_preWalkListenerThrowsException() {
        RuntimeException boom = new RuntimeException("listener boom");
        stubListenerRunner.preException = boom;

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(executionContext, node, rootNode, instancePath, true));
        assertSame(boom, ex);

        // validator must not be called
        assertEquals(0, stubValidator.walkCalledCount);
        // post‑walk must still be called once (with empty set)
        assertEquals(1, stubListenerRunner.postCalls.size());
        assertTrue(stubListenerRunner.postCalls.get(0).messages.isEmpty());
    }

    /* --------------------------------------------------------------
     * 8. Duplicate ValidationMessage handling.
     * -------------------------------------------------------------- */
    @Test
    void walk_duplicateValidationMessages() throws Exception {
        ValidationMessage dup = ValidationMessage.of("duplicate");
        StubValidator validatorA = new StubValidator(new JsonNodePath(PathType.JSON_POINTER));
        whenValidatorPathName(validatorA.getEvaluationPath(), -1, "kwA");
        validatorA.setResult(Set.of(dup));
        stubListenerRunner.setPreResultForKeyword("kwA", true);

        StubValidator validatorB = new StubValidator(new JsonNodePath(PathType.JSON_POINTER));
        whenValidatorPathName(validatorB.getEvaluationPath(), -1, "kwB");
        validatorB.setResult(Set.of(dup));
        stubListenerRunner.setPreResultForKeyword("kwB", true);

        setValidators(List.of(validatorA, validatorB));

        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);

        assertEquals(1, result.size());
        assertTrue(result.contains(dup));
    }

    /* --------------------------------------------------------------
     * 9. Order preservation of error messages.
     * -------------------------------------------------------------- */
    @Test
    void walk_preservesErrorMessageOrder() throws Exception {
        StubValidator validatorA = new StubValidator(new JsonNodePath(PathType.JSON_POINTER));
        whenValidatorPathName(validatorA.getEvaluationPath(), -1, "kwA");
        ValidationMessage msgA = ValidationMessage.of("msgA");
        validatorA.setResult(Set.of(msgA));
        stubListenerRunner.setPreResultForKeyword("kwA", true);

        StubValidator validatorB = new StubValidator(new JsonNodePath(PathType.JSON_POINTER));
        whenValidatorPathName(validatorB.getEvaluationPath(), -1, "kwB");
        ValidationMessage msgB = ValidationMessage.of("msgB");
        validatorB.setResult(Set.of(msgB));
        stubListenerRunner.setPreResultForKeyword("kwB", true);

        StubValidator validatorC = new StubValidator(new JsonNodePath(PathType.JSON_POINTER));
        whenValidatorPathName(validatorC.getEvaluationPath(), -1, "kwC");
        ValidationMessage msgC = ValidationMessage.of("msgC");
        validatorC.setResult(Set.of(msgC));
        stubListenerRunner.setPreResultForKeyword("kwC", true);

        setValidators(List.of(validatorA, validatorB, validatorC));

        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);

        assertEquals(3, result.size());
        Iterator<ValidationMessage> it = result.iterator();
        assertEquals(msgA, it.next());
        assertEquals(msgB, it.next());
        assertEquals(msgC, it.next());
    }

    /* --------------------------------------------------------------
     * 10. shouldValidateSchema flag false but validator still executed.
     * -------------------------------------------------------------- */
    @Test
    void walk_shouldValidateSchemaFlagForwarded() {
        stubValidator.setResult(Set.of(ValidationMessage.of("dummy")));
        stubListenerRunner.preResult = true;

        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, false);

        assertEquals(1, result.size());
        // the stub validator does not inspect the flag, but we can verify it was called
        assertEquals(1, stubValidator.lastShouldValidateSchemaFlagCalls.size());
        assertFalse(stubValidator.lastShouldValidateSchemaFlagCalls.get(0));
    }

    /* --------------------------------------------------------------
     * 11. Null ValidationContext – expect NPE.
     * -------------------------------------------------------------- */
    @Test
    void walk_nullValidationContext_throwsNPE() throws Exception {
        // make internal field null via reflection
        Field vcField = BaseJsonValidator.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        vcField.set(schema, null);

        assertThrows(NullPointerException.class,
                () -> schema.walk(executionContext, node, rootNode, instancePath, true));
    }

    /* -----------------------------------------------------------------
     * Stub implementations used by the tests.
     * ----------------------------------------------------------------- */

    /** Simple stub for {@link KeywordWalkListenerRunner}. */
    private static class StubKeywordWalkListenerRunner implements KeywordWalkListenerRunner {
        static class Call {
            final ExecutionContext ctx;
            final String keyword;
            final JsonNode node;
            final JsonNode rootNode;
            final JsonNodePath path;
            final JsonSchema schema;
            final JsonValidator validator;
            final Set<ValidationMessage> messages; // only for post‑walk

            Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode,
                 JsonNodePath path, JsonSchema schema, JsonValidator validator,
                 Set<ValidationMessage> messages) {
                this.ctx = ctx;
                this.keyword = keyword;
                this.node = node;
                this.rootNode = rootNode;
                this.path = path;
                this.schema = schema;
                this.validator = validator;
                this.messages = messages;
            }
        }

        final List<Call> preCalls = new ArrayList<>();
        final List<Call> postCalls = new ArrayList<>();

        /** Global result for pre‑walk (can be overridden per‑keyword). */
        boolean preResult = true;

        /** Exception to throw from pre‑walk (if non‑null). */
        RuntimeException preException = null;

        /** Exception to throw from post‑walk (if non‑null). */
        RuntimeException postException = null;

        /** Per‑keyword overrides for the pre‑walk result. */
        private final Map<String, Boolean> perKeywordResult = new HashMap<>();

        void setPreResultForKeyword(String keyword, boolean result) {
            perKeywordResult.put(keyword, result);
        }

        @Override
        public boolean runPreWalkListeners(ExecutionContext ctx, String keyword, JsonNode node,
                                           JsonNode rootNode, JsonNodePath instancePath,
                                           JsonSchema schema, JsonValidator validator) {
            preCalls.add(new Call(ctx, keyword, node, rootNode, instancePath, schema, validator, null));
            if (preException != null) {
                throw preException;
            }
            return perKeywordResult.getOrDefault(keyword, preResult);
        }

        @Override
        public void runPostWalkListeners(ExecutionContext ctx, String keyword, JsonNode node,
                                         JsonNode rootNode, JsonNodePath instancePath,
                                         JsonSchema schema, JsonValidator validator,
                                         Set<ValidationMessage> messages) {
            postCalls.add(new Call(ctx, keyword, node, rootNode, instancePath, schema, validator, messages));
            if (postException != null) {
                throw postException;
            }
        }
    }

    /** Simple stub for {@link JsonValidator}. */
    private static class StubValidator implements JsonValidator {
        private final JsonNodePath evaluationPath;
        private Set<ValidationMessage> result;
        private RuntimeException exception;
        int walkCalledCount = 0;
        final List<Boolean> lastShouldValidateSchemaFlagCalls = new ArrayList<>();

        StubValidator(JsonNodePath evaluationPath) {
            this.evaluationPath = evaluationPath;
        }

        void setResult(Set<ValidationMessage> r) {
            this.result = r;
        }

        void setException(RuntimeException e) {
            this.exception = e;
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return evaluationPath;
        }

        @Override
        public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node,
                                           JsonNode rootNode, JsonNodePath instancePath,
                                           boolean shouldValidateSchema) {
            walkCalledCount++;
            lastShouldValidateSchemaFlagCalls.add(shouldValidateSchema);
            if (exception != null) {
                throw exception;
            }
            return result;
        }

        /* The following methods are part of the JsonValidator interface but are not
         * needed for the current tests. Minimal implementations are provided. */
        @Override public String getKeyword() { return evaluationPath.getName(-1); }
        @Override public JsonSchema getSchema() { return null; }
        @Override public JsonNode getSchemaNode() { return null; }
        @Override public void setParentSchema(JsonSchema parent) {}
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public void setSchemaLocation(SchemaLocation location) {}
        @Override public SchemaLocation getSchemaLocation() { return null; }
    }
}
```