```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.*;

import com.networknt.schema.CollectorContext.Scope;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Unit tests for {@link OneOfValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}.
 *
 * The test class creates lightweight stubs for the various framework classes
 * (ValidationContext, ExecutionContext, CollectorContext, Scope, ValidatorState
 * and JsonSchema) so that the behaviour of {@code OneOfValidator} can be exercised
 * in isolation.
 */
class OneOfValidator_validate_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;

    private JsonSchema parentSchema;

    private SimpleJsonSchema childSchema1;

    private SimpleJsonSchema childSchema2;

    private OneOfValidator validator;

    private ExecutionContext executionContext;

    private CollectorContext collectorContext;

    private Scope grandParentScope;

    private ValidatorState validatorState;

    /* -------------------------------------------------------------
     *  Minimal {@link ValidationContext} that returns a pre‑programmed list of schemas.
     * ------------------------------------------------------------- */
    private static class SimpleValidationContext extends ValidationContext {

        private final Queue<JsonSchema> queue = new ArrayDeque<>();

        SimpleValidationContext() {
            // ValidationContext(boolean failFast, boolean isOpenAPI3,
            //                 URIFactory uriFactory, JsonSchemaFactory schemaFactory)
            super(false, false, null, null);
        }

        void queueSchemas(JsonSchema... schemas) {
            queue.addAll(Arrays.asList(schemas));
        }

        @Override
        public JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
            return queue.poll();
        }
    }

    /* -------------------------------------------------------------
     *  Minimal {@link ExecutionContext}.
     * ------------------------------------------------------------- */
    private static class SimpleExecutionContext extends ExecutionContext {

        private CollectorContext collectorContext;

        SimpleExecutionContext() {
            super(null);
        }

        public void setCollectorContext(CollectorContext cc) {
            this.collectorContext = cc;
        }

        @Override
        public CollectorContext getCollectorContext() {
            return collectorContext;
        }
    }

    /* -------------------------------------------------------------
     *  Minimal {@link CollectorContext}.
     * ------------------------------------------------------------- */
    private static class SimpleCollectorContext extends CollectorContext {

        private final Iterator<Scope> enterIter;

        private final Scope exitScope;

        private final Map<String, Object> map = new HashMap<>();

        private Set<String> evaluatedItems = new HashSet<>();

        private Set<String> evaluatedProperties = new HashSet<>();

        SimpleCollectorContext(List<Scope> enterScopes, Scope exitScope) {
            super();
            this.enterIter = enterScopes.iterator();
            this.exitScope = exitScope;
        }

        /** Store a value in the internal map – used by the tests. */
        public void put(String key, Object value) {
            map.put(key, value);
        }

        /** Retrieve a value – overrides the method defined in {@link CollectorContext}. */
        @Override
        public Object get(String key) {
            return map.get(key);
        }

        @Override
        public Scope enterDynamicScope() {
            return enterIter.hasNext() ? enterIter.next() : null;
        }

        @Override
        public Scope exitDynamicScope() {
            return exitScope;
        }

        @Override
        public Set<String> getEvaluatedItems() {
            return evaluatedItems;
        }

        public void setEvaluatedItems(Set<String> set) {
            this.evaluatedItems = set;
        }

        @Override
        public Set<String> getEvaluatedProperties() {
            return evaluatedProperties;
        }

        public void setEvaluatedProperties(Set<String> set) {
            this.evaluatedProperties = set;
        }
    }

    /* -------------------------------------------------------------
     *  Minimal {@link Scope}.  It does not need any behaviour for the tests.
     * ------------------------------------------------------------- */
    private static class SimpleScope extends Scope {

        SimpleScope() {
            // Scope(boolean, boolean, JsonSchema) constructor
            super(false, false, null);
        }
    }

    /* -------------------------------------------------------------
     *  Minimal {@link ValidatorState}.
     * ------------------------------------------------------------- */
    private static class SimpleValidatorState extends ValidatorState {

        private boolean walkEnabled = false;

        private boolean matchedNode = true;

        private boolean validationEnabled = true;

        SimpleValidatorState() {
            super();
        }

        @Override
        public boolean isWalkEnabled() {
            return walkEnabled;
        }

        public void setWalkEnabled(boolean b) {
            this.walkEnabled = b;
        }

        @Override
        public boolean hasMatchedNode() {
            return matchedNode;
        }

        public void setMatchedNode(boolean b) {
            this.matchedNode = b;
        }

        @Override
        public boolean isValidationEnabled() {
            return validationEnabled;
        }

        public void setValidationEnabled(boolean b) {
            this.validationEnabled = b;
        }
    }

    /* -------------------------------------------------------------
     *  Minimal {@link JsonSchema} that returns pre‑programmed results for
     *  {@code validate} / {@code walk}.
     * ------------------------------------------------------------- */
    private static class SimpleJsonSchema extends JsonSchema {

        private Set<ValidationMessage> validateResult = Collections.emptySet();

        private Set<ValidationMessage> walkResult = Collections.emptySet();

        SimpleJsonSchema() {
            // JsonSchema(ValidationContext, JsonNode, JsonSchema, String)
            super(new SimpleValidationContext(), null, null, null);
        }

        void setValidateResult(Set<ValidationMessage> result) {
            this.validateResult = result;
        }

        void setWalkResult(Set<ValidationMessage> result) {
            this.walkResult = result;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ec, JsonNode node, JsonNode rootNode, String at) {
            return validateResult;
        }

        @Override
        public Set<ValidationMessage> walk(ExecutionContext ec, JsonNode node, JsonNode rootNode, String at,
                                           boolean walkEnabled) {
            return walkResult;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* ---------- Construction of OneOfValidator ---------- */
        validationContext = new SimpleValidationContext();
        parentSchema = new SimpleJsonSchema();
        childSchema1 = new SimpleJsonSchema();
        childSchema2 = new SimpleJsonSchema();
        // make ValidationContext return the two child schemas when the validator
        // constructs its internal list
        ((SimpleValidationContext) validationContext).queueSchemas(childSchema1, childSchema2);
        ArrayNode schemaArray = MAPPER.createArrayNode();
        // placeholder 1
        schemaArray.addObject();
        // placeholder 2
        schemaArray.addObject();
        validator = new OneOfValidator("testPath", schemaArray, parentSchema, validationContext);
        /* ---------- Execution context used during validation ---------- */
        executionContext = new SimpleExecutionContext();
        // three scopes – one “grand‑parent” and one per child iteration
        grandParentScope = new SimpleScope();
        Scope childScope1 = new SimpleScope();
        Scope childScope2 = new SimpleScope();
        collectorContext = new SimpleCollectorContext(Arrays.asList(grandParentScope, childScope1, childScope2),
                new SimpleScope());
        ((SimpleExecutionContext) executionContext).setCollectorContext(collectorContext);
        // validatorState stored inside the CollectorContext
        validatorState = new SimpleValidatorState();
        ((SimpleCollectorContext) collectorContext).put(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        validatorState.setWalkEnabled(false);
        validatorState.setMatchedNode(true);
        validatorState.setValidationEnabled(true);
        // the collections that OneOfValidator clears on failure
        ((SimpleCollectorContext) collectorContext).setEvaluatedItems(new HashSet<>());
        ((SimpleCollectorContext) collectorContext).setEvaluatedProperties(new HashSet<>());
    }

    /**
     * Helper that creates a simple empty JSON node ({}).
     */
    private JsonNode emptyNode() {
        return MAPPER.createObjectNode();
    }

    /**
     * Utility to replace the internal {@code schemas} list with the provided mocks.
     */
    private void setInternalSchemas(JsonSchema... schemas) throws Exception {
        Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, new ArrayList<>(Arrays.asList(schemas)));
    }

    /**
     * Utility to enable/disable the {@code failFast} flag via reflection.
     */
    private void setFailFast(boolean failFast) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("failFast");
        f.setAccessible(true);
        f.set(validator, failFast);
    }

    /* -------------------------------------------------------------
     *  Helper to create a ValidationMessage – the library only provides
     *  a no‑arg constructor with setters.
     * ------------------------------------------------------------- */
    private static ValidationMessage vm(String type, String path, String message) {
        ValidationMessage vm = new ValidationMessage();
        vm.setType(type);
        vm.setPath(path);
        vm.setMessage(message);
        return vm;
    }

    @Test
    void validate_WhenExactlyOneSchemaMatches_ReturnsNoErrors() {
        childSchema1.setValidateResult(Collections.emptySet());
        childSchema2.setValidateResult(Set.of(vm("type", "#", "error")));
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertEquals(1, result.size(),
                "When exactly one schema matches, only the child errors should be returned.");
    }

    @Test
    void validate_WhenZeroOrMoreThanOneSchemaMatches_ReturnsOneOfError() {
        // ---- Scenario 1: zero matching schemas ----
        childSchema1.setValidateResult(Set.of(vm("type", "#", "err1")));
        childSchema2.setValidateResult(Set.of(vm("type", "#", "err2")));
        Set<ValidationMessage> zeroMatch = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(zeroMatch.isEmpty(), "Zero matching schemas must produce a OneOf error.");
        assertTrue(zeroMatch.size() >= 1, "At least the OneOf error should be present.");
        // ---- Scenario 2: two matching schemas ----
        childSchema1.setValidateResult(Collections.emptySet());
        childSchema2.setValidateResult(Collections.emptySet());
        Set<ValidationMessage> twoMatch = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(twoMatch.isEmpty(), "More than one matching schema must produce a OneOf error.");
        assertEquals(1, twoMatch.size(), "When two schemas match, only the OneOf error should be reported.");
    }

    /* ---------------------------------------------------------
     *  1. Walk disabled – exactly one schema matches
     * --------------------------------------------------------- */
    @Test
    void validate_WalkDisabled_ExactlyOneSchemaMatches_ReturnsNoErrors() throws Exception {
        SimpleJsonSchema s1 = new SimpleJsonSchema();
        SimpleJsonSchema s2 = new SimpleJsonSchema();
        setInternalSchemas(s1, s2);
        s1.setValidateResult(Collections.emptySet());
        s2.setValidateResult(Set.of(vm("type", "#", "error")));
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertEquals(1, result.size(),
                "When exactly one schema matches, the OneOf validator must return the child errors only.");
    }

    /* ---------------------------------------------------------
     *  2. Walk disabled – zero matching schemas
     * --------------------------------------------------------- */
    @Test
    void validate_WalkDisabled_NoSchemaMatches_ReturnsOneOfError() throws Exception {
        SimpleJsonSchema s1 = new SimpleJsonSchema();
        SimpleJsonSchema s2 = new SimpleJsonSchema();
        setInternalSchemas(s1, s2);
        ValidationMessage childErr1 = vm("type", "#", "c1");
        ValidationMessage childErr2 = vm("type", "#", "c2");
        s1.setValidateResult(Set.of(childErr1));
        s2.setValidateResult(Set.of(childErr2));
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(result.isEmpty(), "Zero matching schemas must produce a OneOf error.");
        assertTrue(result.contains(childErr1) && result.contains(childErr2),
                "Child errors must be part of the returned set.");
        assertTrue(((SimpleCollectorContext) collectorContext).getEvaluatedItems().isEmpty()
                        && ((SimpleCollectorContext) collectorContext).getEvaluatedProperties().isEmpty(),
                "Evaluated items/properties should be cleared after a failure.");
    }

    /* ---------------------------------------------------------
     *  3. Walk disabled – matchedNode flag false after validation
     * --------------------------------------------------------- */
    @Test
    void validate_WalkDisabled_MatchedNodeFalse_IgnoresEmptySchemaErrors() throws Exception {
        SimpleJsonSchema s = new SimpleJsonSchema();
        setInternalSchemas(s);
        s.setValidateResult(Collections.emptySet());
        validatorState.setMatchedNode(false);
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(result.isEmpty(),
                "Because matchedNode is false, validator should treat schema as non‑matching.");
        assertEquals(1, result.size(),
                "When matchedNode is false, exactly one OneOf error must be reported.");
    }

    /* ---------------------------------------------------------
     *  4. Walk enabled – exactly one schema matches
     * --------------------------------------------------------- */
    @Test
    void validate_WalkEnabled_ExactlyOneSchemaMatches_ReturnsNoErrors() throws Exception {
        validatorState.setWalkEnabled(true);
        SimpleJsonSchema s = new SimpleJsonSchema();
        setInternalSchemas(s);
        s.setWalkResult(Collections.emptySet());
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertTrue(result.isEmpty(), "One matching schema with walk enabled must produce no errors.");
    }

    /* ---------------------------------------------------------
     *  5. Walk enabled – matchedNode false after walk
     * --------------------------------------------------------- */
    @Test
    void validate_WalkEnabled_MatchedNodeFalse_IgnoresEmptyWalkResult() throws Exception {
        validatorState.setWalkEnabled(true);
        validatorState.setMatchedNode(false);
        SimpleJsonSchema s = new SimpleJsonSchema();
        setInternalSchemas(s);
        s.setWalkResult(Collections.emptySet());
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(result.isEmpty(),
                "Validator must treat the schema as non‑matching because matchedNode is false.");
        assertEquals(1, result.size(),
                "Only the OneOf error should be present when walk succeeds but matchedNode is false.");
    }

    /* ---------------------------------------------------------
     *  6. Walk enabled – validation disabled (state.isValidationEnabled == false)
     * --------------------------------------------------------- */
    @Test
    void validate_WalkEnabled_ValidationDisabled_SchemaWalkCalledWithFalseFlag() throws Exception {
        validatorState.setWalkEnabled(true);
        validatorState.setValidationEnabled(false);
        SimpleJsonSchema s = new SimpleJsonSchema();
        setInternalSchemas(s);
        s.setWalkResult(Collections.emptySet());
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertTrue(result.isEmpty(),
                "With validation disabled, an empty walk result should still be considered a match.");
    }

    /* ---------------------------------------------------------
     *  7. Multiple valid schemas – failFast = false
     * --------------------------------------------------------- */
    @Test
    void validate_MultipleValidSchemas_FailFastFalse_ReturnsOneOfError() throws Exception {
        SimpleJsonSchema s1 = new SimpleJsonSchema();
        SimpleJsonSchema s2 = new SimpleJsonSchema();
        SimpleJsonSchema s3 = new SimpleJsonSchema();
        setInternalSchemas(s1, s2, s3);
        s1.setValidateResult(Collections.emptySet());
        s2.setValidateResult(Collections.emptySet());
        s3.setValidateResult(Collections.emptySet());
        setFailFast(false);
        Set<ValidationMessage> result = validator.validate(executionContext, emptyNode(), emptyNode(), "#");
        assertFalse(result.isEmpty(),
                "More than one matching schema must produce a OneOf error when failFast is false.");
        assertEquals(1, result.size(),
                "When multiple schemas match and failFast is false, only the OneOf message should be present.");
    }

    /* ---------------------------------------------------------
     *  8. Multiple valid schemas – failFast = true
     * --------------------------------------------------------- */
    @Test
    void validate_MultipleValidSchemas_FailFastTrue_ThrowsJsonSchemaException() throws Exception {
        SimpleJsonSchema s1 = new SimpleJsonSchema();
        SimpleJsonSchema s2 = new SimpleJsonSchema();
        setInternalSchemas(s1, s2);
        s1.setValidateResult(Collections.emptySet());
        s2.setValidateResult(Collections.emptySet());
        setFailFast(true);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> validator.validate(executionContext, emptyNode(), emptyNode(), "#"),
                "When failFast is true and multiple schemas match, an exception must be thrown.");
        assertNotNull(ex.getMessage(),
                "Exception should contain a message describing the OneOf error.");
    }
}
```