
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchema walk tests (stubbed listeners/validators)")
class JsonSchema_walk_10_Test {
    /**
     * The schema instance under test.
     */
    private JsonSchema schema;

    /**
     * Execution context passed to {@code walk}.
     */
    private ExecutionContext executionContext;

    /**
     * Mocked validation context used inside the schema.
     */
    @Mock
    private ValidationContext mockValidationContext;

    /**
     * Mocked configuration returned from the validation context.
     */
    @Mock
    private SchemaValidatorsConfig mockConfig;

    /**
     * Mocked listener runner handling pre‑ and post‑walk callbacks.
     */
    @Mock
    private KeywordWalkListenerRunner mockListenerRunner;

    /**
     * A single validator that will be returned from {@code getValidators()}.
     */
    @Mock
    private JsonValidator mockValidator;

    /**
     * A dummy validation message returned by the mock validator.
     */
    private ValidationMessage dummyMessage;

    /**
     * Sample JSON nodes used as parameters for the walk call.
     */
    private JsonNode node;

    private JsonNode rootNode;

    private JsonNodePath instancePath;

    /**
     * Stubbed listener runner handling pre‑ and post‑walk callbacks.
     */
    private StubKeywordWalkListenerRunner stubListenerRunner;

    /**
     * A single stub validator that will be returned from {@code getValidators()}.
     */
    private StubValidator stubValidator;

    static class Call {
        final ExecutionContext ctx;

        final String keyword;

        final JsonNode node;

        final JsonNode rootNode;

        final JsonNodePath path;

        final JsonSchema schema;

        final JsonValidator validator;

        // only for post‑walk
        final Set<ValidationMessage> messages;

        Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
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

    /**
     * Simple stub for {@link KeywordWalkListenerRunner}.
     */
    private static class StubKeywordWalkListenerRunner implements KeywordWalkListenerRunner {
        static class Call {
            final ExecutionContext ctx;

            final String keyword;

            final JsonNode node;

            final JsonNode rootNode;

            final JsonNodePath path;

            final JsonSchema schema;

            final JsonValidator validator;

            // only for post‑walk
            final Set<ValidationMessage> messages;

            Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
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

        /**
         * Global result for pre‑walk (can be overridden per‑keyword).
         */
        boolean preResult = true;

        /**
         * Exception to throw from pre‑walk (if non‑null).
         */
        RuntimeException preException = null;

        /**
         * Exception to throw from post‑walk (if non‑null).
         */
        RuntimeException postException = null;

        /**
         * Per‑keyword overrides for the pre‑walk result.
         */
        private final Map<String, Boolean> perKeywordResult = new HashMap<>();

        void setPreResultForKeyword(String keyword, boolean result) {
            perKeywordResult.put(keyword, result);
        }

        @Override
        public boolean runPreWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator) {
            preCalls.add(new Call(ctx, keyword, node, rootNode, instancePath, schema, validator, null));
            if (preException != null) {
                throw preException;
            }
            return perKeywordResult.getOrDefault(keyword, preResult);
        }

        @Override
        public void runPostWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
            postCalls.add(new Call(ctx, keyword, node, rootNode, instancePath, schema, validator, messages));
            if (postException != null) {
                throw postException;
            }
        }
    }

    /**
     * Simple stub for {@link JsonValidator}.
     */
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
        public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, boolean shouldValidateSchema) {
            walkCalledCount++;
            lastShouldValidateSchemaFlagCalls.add(shouldValidateSchema);
            if (exception != null) {
                throw exception;
            }
            return result;
        }

        @Override
        public String getKeyword() {
            return evaluationPath.getName(-1);
        }

        @Override
        public JsonSchema getSchema() {
            return null;
        }

        @Override
        public JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public void setParentSchema(JsonSchema parent) {
        }

        @Override
        public JsonSchema getParentSchema() {
            return null;
        }

        @Override
        public void setSchemaLocation(SchemaLocation location) {
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }
    }

    // only for post‑walk
    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* -----------------------------------------------------------------
        1. Create a minimal real JsonSchema instance.
        -----------------------------------------------------------------
         */
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        ValidationContext realVc = new ValidationContext(JsonMetaSchema.getV202012(), JsonSchemaFactory.getInstance(VersionFlag.V202012), config);
        ObjectNode emptySchemaNode = JsonNodeFactory.instance.objectNode();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // empty schema
        schema = factory.getSchema(emptySchemaNode);
        /* -----------------------------------------------------------------
        2. Prepare stub listener runner.
        -----------------------------------------------------------------
         */
        stubListenerRunner = new StubKeywordWalkListenerRunner();
        // plug into config
        config.setKeywordWalkListenerRunner(stubListenerRunner);
        /* -----------------------------------------------------------------
        3. Prepare a stub validator.
        -----------------------------------------------------------------
         */
        JsonNodePath validatorPath = new JsonNodePath(PathType.JSON_POINTER);
        // the stub will use the keyword name "dummyKeyword"
        whenValidatorPathName(validatorPath, -1, "dummyKeyword");
        stubValidator = new StubValidator(validatorPath);
        dummyMessage = ValidationMessage.of("dummy error");
        stubValidator.setResult(Set.of(dummyMessage));
        /* -----------------------------------------------------------------
        4. Inject the stub validator into the schema (via reflection).
        -----------------------------------------------------------------
         */
        setValidators(List.of(stubValidator));
        /* -----------------------------------------------------------------
        5. Common objects used in the test methods.
        -----------------------------------------------------------------
         */
        executionContext = new ExecutionContext();
        node = JsonNodeFactory.instance.objectNode().put("a", "value");
        rootNode = node.deepCopy();
        instancePath = new JsonNodePath(PathType.JSON_POINTER);
    }

    /**
     * Helper to replace the private {@code validators} field.
     */
    private void setValidators(List<JsonValidator> list) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        f.set(schema, list);
    }

    /**
     * Helper to replace the private {@code validationContext} field.
     */
    private void setValidationContext(ValidationContext vc) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(schema, vc);
    }

    /**
     * Helper to set a name on a JsonNodePath mock (used only for the stub).
     */
    private static void whenValidatorPathName(JsonNodePath path, int index, String name) throws Exception {
        // JsonNodePath is a concrete class; we set its internal name list via reflection.
        // The class stores the names in a private List<String> called "names".
        Field namesField = JsonNodePath.class.getDeclaredField("names");
        namesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> names = ((List<String>) (namesField.get(path)));
        // ensure capacity
        while (names.size() <= index) {
            names.add(null);
        } 
        names.set(index, name);
    }

    /**
     * Verifies that {@link JsonSchema#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}
     * correctly delegates to validators, respects the pre‑walk listener result,
     * aggregates validation messages and invokes the post‑walk listener.
     */
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
    1. Pre‑walk listener allows validation – different validator results.
    --------------------------------------------------------------
     */
    @ParameterizedTest(name = "pre‑walk true, validator result = {0}")
    @EnumSource(ValidatorResult.class)
    void walk_preWalkAllowsVariousValidatorResults(ValidatorResult resultType) throws Exception {
        // arrange
        switch (resultType) {
            case NON_EMPTY ->
                stubValidator.setResult(Set.of(ValidationMessage.of("dummy error")));
            case EMPTY ->
                stubValidator.setResult(Collections.emptySet());
            case NULL ->
                stubValidator.setResult(null);
        }
        // ensure pre‑walk proceeds
        stubListenerRunner.preResult = true;
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
    2. Pre‑walk listener skips validation.
    --------------------------------------------------------------
     */
    @Test
    void walk_preWalkSkipsValidator() throws Exception {
        // skip
        stubListenerRunner.preResult = false;
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);
        assertTrue(result.isEmpty());
        // validator should not have been called – we check that its walk method was never invoked
        assertEquals(0, stubValidator.walkCalledCount);
        assertEquals(1, stubListenerRunner.preCalls.size());
        assertEquals(1, stubListenerRunner.postCalls.size());
        // post‑walk must receive an empty set
        assertTrue(stubListenerRunner.postCalls.get(0).messages.isEmpty());
    }

    /* --------------------------------------------------------------
    3. Multiple validators with mixed outcomes.
    --------------------------------------------------------------
     */
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
    4. Empty validators list.
    --------------------------------------------------------------
     */
    @Test
    void walk_emptyValidatorsList() throws Exception {
        setValidators(Collections.emptyList());
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);
        assertTrue(result.isEmpty());
        assertTrue(stubListenerRunner.preCalls.isEmpty());
        assertTrue(stubListenerRunner.postCalls.isEmpty());
    }

    /* --------------------------------------------------------------
    5. Validator throws exception – post‑walk still executed.
    --------------------------------------------------------------
     */
    @Test
    void walk_validatorThrowsException() throws Exception {
        RuntimeException boom = new RuntimeException("boom");
        stubValidator.setException(boom);
        stubListenerRunner.preResult = true;
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(executionContext, node, rootNode, instancePath, true));
        assertSame(boom, ex);
        // pre‑walk called, post‑walk called with empty set
        assertEquals(1, stubListenerRunner.preCalls.size());
        assertEquals(1, stubListenerRunner.postCalls.size());
        assertTrue(stubListenerRunner.postCalls.get(0).messages.isEmpty());
    }

    /* --------------------------------------------------------------
    6. Pre‑walk listener throws exception – validator not invoked.
    --------------------------------------------------------------
     */
    @Test
    void walk_preWalkListenerThrowsException() throws Exception {
        RuntimeException boom = new RuntimeException("listener boom");
        stubListenerRunner.preException = boom;
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(executionContext, node, rootNode, instancePath, true));
        assertSame(boom, ex);
        // validator must not be called
        assertEquals(0, stubValidator.walkCalledCount);
        // post‑walk must still be called once (with empty set)
        assertEquals(1, stubListenerRunner.postCalls.size());
        assertTrue(stubListenerRunner.postCalls.get(0).messages.isEmpty());
    }

    /* --------------------------------------------------------------
    7. Duplicate ValidationMessage handling.
    --------------------------------------------------------------
     */
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
    8. Order preservation of error messages.
    --------------------------------------------------------------
     */
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
    9. shouldValidateSchema flag false but validator still executed.
    --------------------------------------------------------------
     */
    @Test
    void walk_shouldValidateSchemaFlagForwarded() throws Exception {
        stubValidator.setResult(Set.of(ValidationMessage.of("dummy")));
        stubListenerRunner.preResult = true;
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, false);
        assertEquals(1, result.size());
        // the stub validator does not inspect the flag, but we can verify it was called
        assertEquals(1, stubValidator.lastShouldValidateSchemaFlagCalls.size());
        assertFalse(stubValidator.lastShouldValidateSchemaFlagCalls.get(0));
    }

    /* --------------------------------------------------------------
    10. Null ValidationContext – expect NPE.
    --------------------------------------------------------------
     */
    @Test
    void walk_nullValidationContext_throwsNPE() throws Exception {
        // make internal field null via reflection
        Field vcField = BaseJsonValidator.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        vcField.set(schema, null);
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, node, rootNode, instancePath, true));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:1: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
 ^
  symbol: class ExtendWith
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:2: error: cannot find symbol
@DisplayName("JsonSchema walk tests (stubbed listeners/validators)")
 ^
  symbol: class DisplayName
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:7: error: cannot find symbol
    private JsonSchema schema;
            ^
  symbol:   class JsonSchema
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:12: error: cannot find symbol
    private ExecutionContext executionContext;
            ^
  symbol:   class ExecutionContext
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:18: error: cannot find symbol
    private ValidationContext mockValidationContext;
            ^
  symbol:   class ValidationContext
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:24: error: cannot find symbol
    private SchemaValidatorsConfig mockConfig;
            ^
  symbol:   class SchemaValidatorsConfig
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:30: error: cannot find symbol
    private KeywordWalkListenerRunner mockListenerRunner;
            ^
  symbol:   class KeywordWalkListenerRunner
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:36: error: cannot find symbol
    private JsonValidator mockValidator;
            ^
  symbol:   class JsonValidator
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:41: error: cannot find symbol
    private ValidationMessage dummyMessage;
            ^
  symbol:   class ValidationMessage
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:46: error: cannot find symbol
    private JsonNode node;
            ^
  symbol:   class JsonNode
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:48: error: cannot find symbol
    private JsonNode rootNode;
            ^
  symbol:   class JsonNode
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:50: error: cannot find symbol
    private JsonNodePath instancePath;
            ^
  symbol:   class JsonNodePath
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:95: error: cannot find symbol
    private static class StubKeywordWalkListenerRunner implements KeywordWalkListenerRunner {
                                                                  ^
  symbol:   class KeywordWalkListenerRunner
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:175: error: cannot find symbol
    private static class StubValidator implements JsonValidator {
                                                  ^
  symbol:   class JsonValidator
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:295: error: cannot find symbol
    private void setValidators(List<JsonValidator> list) throws Exception {
                               ^
  symbol:   class List
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:295: error: cannot find symbol
    private void setValidators(List<JsonValidator> list) throws Exception {
                                    ^
  symbol:   class JsonValidator
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:304: error: cannot find symbol
    private void setValidationContext(ValidationContext vc) throws Exception {
                                      ^
  symbol:   class ValidationContext
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:313: error: cannot find symbol
    private static void whenValidatorPathName(JsonNodePath path, int index, String name) throws Exception {
                                              ^
  symbol:   class JsonNodePath
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:368: error: cannot find symbol
    void walk_preWalkAllowsVariousValidatorResults(ValidatorResult resultType) throws Exception {
                                                   ^
  symbol:   class ValidatorResult
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:176: error: cannot find symbol
        private final JsonNodePath evaluationPath;
                      ^
  symbol:   class JsonNodePath
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:178: error: cannot find symbol
        private Set<ValidationMessage> result;
                ^
  symbol:   class Set
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:178: error: cannot find symbol
        private Set<ValidationMessage> result;
                    ^
  symbol:   class ValidationMessage
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:184: error: cannot find symbol
        final List<Boolean> lastShouldValidateSchemaFlagCalls = new ArrayList<>();
              ^
  symbol:   class List
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:186: error: cannot find symbol
        StubValidator(JsonNodePath evaluationPath) {
                      ^
  symbol:   class JsonNodePath
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:190: error: cannot find symbol
        void setResult(Set<ValidationMessage> r) {
                       ^
  symbol:   class Set
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:190: error: cannot find symbol
        void setResult(Set<ValidationMessage> r) {
                           ^
  symbol:   class ValidationMessage
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:199: error: cannot find symbol
        public JsonNodePath getEvaluationPath() {
               ^
  symbol:   class JsonNodePath
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:204: error: cannot find symbol
        public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, boolean shouldValidateSchema) {
                                           ^
  symbol:   class ExecutionContext
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:204: error: cannot find symbol
        public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, boolean shouldValidateSchema) {
                                                                 ^
  symbol:   class JsonNode
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:204: error: cannot find symbol
        public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, boolean shouldValidateSchema) {
                                                                                ^
  symbol:   class JsonNode
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:204: error: cannot find symbol
        public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, boolean shouldValidateSchema) {
                                                                                                   ^
  symbol:   class JsonNodePath
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:204: error: cannot find symbol
        public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, boolean shouldValidateSchema) {
               ^
  symbol:   class Set
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:204: error: cannot find symbol
        public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, boolean shouldValidateSchema) {
                   ^
  symbol:   class ValidationMessage
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:219: error: cannot find symbol
        public JsonSchema getSchema() {
               ^
  symbol:   class JsonSchema
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:224: error: cannot find symbol
        public JsonNode getSchemaNode() {
               ^
  symbol:   class JsonNode
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:229: error: cannot find symbol
        public void setParentSchema(JsonSchema parent) {
                                    ^
  symbol:   class JsonSchema
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:233: error: cannot find symbol
        public JsonSchema getParentSchema() {
               ^
  symbol:   class JsonSchema
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:238: error: cannot find symbol
        public void setSchemaLocation(SchemaLocation location) {
                                      ^
  symbol:   class SchemaLocation
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:242: error: cannot find symbol
        public SchemaLocation getSchemaLocation() {
               ^
  symbol:   class SchemaLocation
  location: class StubValidator
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:126: error: cannot find symbol
        final List<Call> preCalls = new ArrayList<>();
              ^
  symbol:   class List
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:128: error: cannot find symbol
        final List<Call> postCalls = new ArrayList<>();
              ^
  symbol:   class List
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:148: error: cannot find symbol
        private final Map<String, Boolean> perKeywordResult = new HashMap<>();
                      ^
  symbol:   class Map
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:155: error: cannot find symbol
        public boolean runPreWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator) {
                                           ^
  symbol:   class ExecutionContext
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:155: error: cannot find symbol
        public boolean runPreWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator) {
                                                                                 ^
  symbol:   class JsonNode
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:155: error: cannot find symbol
        public boolean runPreWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator) {
                                                                                                ^
  symbol:   class JsonNode
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:155: error: cannot find symbol
        public boolean runPreWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator) {
                                                                                                                   ^
  symbol:   class JsonNodePath
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:155: error: cannot find symbol
        public boolean runPreWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator) {
                                                                                                                                              ^
  symbol:   class JsonSchema
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:155: error: cannot find symbol
        public boolean runPreWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator) {
                                                                                                                                                                 ^
  symbol:   class JsonValidator
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:164: error: cannot find symbol
        public void runPostWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                         ^
  symbol:   class ExecutionContext
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:164: error: cannot find symbol
        public void runPostWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                               ^
  symbol:   class JsonNode
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:164: error: cannot find symbol
        public void runPostWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                              ^
  symbol:   class JsonNode
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:164: error: cannot find symbol
        public void runPostWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                                 ^
  symbol:   class JsonNodePath
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:164: error: cannot find symbol
        public void runPostWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                                                            ^
  symbol:   class JsonSchema
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:164: error: cannot find symbol
        public void runPostWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                                                                               ^
  symbol:   class JsonValidator
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:164: error: cannot find symbol
        public void runPostWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                                                                                                        ^
  symbol:   class Set
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:164: error: cannot find symbol
        public void runPostWalkListeners(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath instancePath, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                                                                                                            ^
  symbol:   class ValidationMessage
  location: class StubKeywordWalkListenerRunner
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:97: error: cannot find symbol
            final ExecutionContext ctx;
                  ^
  symbol:   class ExecutionContext
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:101: error: cannot find symbol
            final JsonNode node;
                  ^
  symbol:   class JsonNode
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:103: error: cannot find symbol
            final JsonNode rootNode;
                  ^
  symbol:   class JsonNode
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:105: error: cannot find symbol
            final JsonNodePath path;
                  ^
  symbol:   class JsonNodePath
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:107: error: cannot find symbol
            final JsonSchema schema;
                  ^
  symbol:   class JsonSchema
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:109: error: cannot find symbol
            final JsonValidator validator;
                  ^
  symbol:   class JsonValidator
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:112: error: cannot find symbol
            final Set<ValidationMessage> messages;
                  ^
  symbol:   class Set
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:112: error: cannot find symbol
            final Set<ValidationMessage> messages;
                      ^
  symbol:   class ValidationMessage
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:114: error: cannot find symbol
            Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                 ^
  symbol:   class ExecutionContext
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:114: error: cannot find symbol
            Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                       ^
  symbol:   class JsonNode
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:114: error: cannot find symbol
            Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                      ^
  symbol:   class JsonNode
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:114: error: cannot find symbol
            Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                         ^
  symbol:   class JsonNodePath
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:114: error: cannot find symbol
            Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                            ^
  symbol:   class JsonSchema
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:114: error: cannot find symbol
            Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                                               ^
  symbol:   class JsonValidator
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:114: error: cannot find symbol
            Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                                                                        ^
  symbol:   class Set
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:114: error: cannot find symbol
            Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                                                                            ^
  symbol:   class ValidationMessage
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:63: error: cannot find symbol
        final ExecutionContext ctx;
              ^
  symbol:   class ExecutionContext
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:67: error: cannot find symbol
        final JsonNode node;
              ^
  symbol:   class JsonNode
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:69: error: cannot find symbol
        final JsonNode rootNode;
              ^
  symbol:   class JsonNode
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:71: error: cannot find symbol
        final JsonNodePath path;
              ^
  symbol:   class JsonNodePath
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:73: error: cannot find symbol
        final JsonSchema schema;
              ^
  symbol:   class JsonSchema
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:75: error: cannot find symbol
        final JsonValidator validator;
              ^
  symbol:   class JsonValidator
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:78: error: cannot find symbol
        final Set<ValidationMessage> messages;
              ^
  symbol:   class Set
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:78: error: cannot find symbol
        final Set<ValidationMessage> messages;
                  ^
  symbol:   class ValidationMessage
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:80: error: cannot find symbol
        Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
             ^
  symbol:   class ExecutionContext
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:80: error: cannot find symbol
        Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                   ^
  symbol:   class JsonNode
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:80: error: cannot find symbol
        Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                  ^
  symbol:   class JsonNode
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:80: error: cannot find symbol
        Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                     ^
  symbol:   class JsonNodePath
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:80: error: cannot find symbol
        Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                        ^
  symbol:   class JsonSchema
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:80: error: cannot find symbol
        Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                                           ^
  symbol:   class JsonValidator
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:80: error: cannot find symbol
        Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                                                                    ^
  symbol:   class Set
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:80: error: cannot find symbol
        Call(ExecutionContext ctx, String keyword, JsonNode node, JsonNode rootNode, JsonNodePath path, JsonSchema schema, JsonValidator validator, Set<ValidationMessage> messages) {
                                                                                                                                                        ^
  symbol:   class ValidationMessage
  location: class Call
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:1: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:17: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:23: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:29: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:35: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:248: error: cannot find symbol
    @BeforeEach
     ^
  symbol:   class BeforeEach
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:332: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:366: error: cannot find symbol
    @ParameterizedTest(name = "pre‑walk true, validator result = {0}")
     ^
  symbol:   class ParameterizedTest
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:367: error: cannot find symbol
    @EnumSource(ValidatorResult.class)
     ^
  symbol:   class EnumSource
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:367: error: cannot find symbol
    @EnumSource(ValidatorResult.class)
                ^
  symbol:   class ValidatorResult
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:398: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:416: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class JsonSchema_walk_10_Test
100 errors
only showing the first 100 errors, of 228 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, JsonSchema, JsonNode, JsonNode, JsonNodePath, boolean, Set<ValidationMessage>, String)` returns `void`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public ValidationContext getValidationContext()` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;\n\t\tmethod `public ExecutionContext createExecutionContext()` returns `com.networknt.schema.ExecutionContext`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`, api document: \n     * Creates an execution context.\n     * \n     * @return the execution context\n     ;\n\t\tmethod `private T walkAtNodeInternal(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;\nclass `com.networknt.schema.PrefixItemsValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, JsonSchema, JsonNode, JsonNode, JsonNodePath, boolean, Set<ValidationMessage>)` returns `void`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


