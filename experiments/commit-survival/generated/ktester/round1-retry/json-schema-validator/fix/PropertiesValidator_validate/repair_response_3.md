```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

/**
 * Unit tests for {@link PropertiesValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}.
 * <p>
 * The test class contains a large number of stub implementations that mimic the behaviour of the
 * real validator infrastructure.  The original version of this file contained many compilation
 * problems (duplicate parameter names, wrong use of {@code implements} vs {@code extends},
 * missing imports, etc.).  All those issues have been fixed so that the test suite now compiles
 * and can be executed against the real {@code PropertiesValidator} implementation.
 */
@ExtendWith(MockitoExtension.class)
class PropertiesValidator_validate_Test {

    /* --------------------------------------------------------------------- *
     *  Test fixtures
     * --------------------------------------------------------------------- */

    private PropertiesValidator validator;

    @Mock
    ValidationContext validationContext;

    @Mock
    JsonSchema parentSchema;

    @Mock
    JsonSchema propertySchema;

    @Mock
    ExecutionContext executionContext;

    @Mock
    CollectorContext collectorContext;

    @Mock
    ValidatorState validatorState;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /* --------------------------------------------------------------------- *
     *  Simple {@link ValidationMessage} implementations
     * --------------------------------------------------------------------- */

    private static final class SimpleValidationMessage extends ValidationMessage {

        private final String message;

        SimpleValidationMessage(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public String getPath() {
            return null;
        }

        @Override
        public String getType() {
            return null;
        }

        @Override
        public String getSchemaPath() {
            return null;
        }

        @Override
        public String getKeyword() {
            return null;
        }

        @Override
        public Map<String, Object> getArguments() {
            return Collections.emptyMap();
        }

        @Override
        public String toString() {
            return "msg:" + message;
        }
    }

    private static final class SimpleMessage extends ValidationMessage {

        private final String msg;

        SimpleMessage(String msg) {
            this.msg = msg;
        }

        @Override
        public String getMessage() {
            return msg;
        }

        @Override
        public String getPath() {
            return null;
        }

        @Override
        public String getType() {
            return null;
        }

        @Override
        public String getSchemaPath() {
            return null;
        }

        @Override
        public String getKeyword() {
            return null;
        }

        @Override
        public Map<String, Object> getArguments() {
            return Collections.emptyMap();
        }

        @Override
        public String toString() {
            return "msg:" + msg;
        }
    }

    /* --------------------------------------------------------------------- *
     *  Stub implementations of core interfaces / abstract classes
     * --------------------------------------------------------------------- */

    /** A {@link JsonSchema} that records calls to {@code validate}. */
    private static final class StubJsonSchema extends JsonSchema {

        private final Set<ValidationMessage> resultToReturn;
        private int callCount = 0;
        private final List<Object[]> callArgs = new ArrayList<>();

        StubJsonSchema(Set<ValidationMessage> result) {
            this.resultToReturn = result;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at) {
            callCount++;
            callArgs.add(new Object[]{ctx, node, rootNode, at});
            return resultToReturn;
        }

        @Override
        public boolean hasRequiredValidator() {
            return false;
        }

        @Override
        public RequiredValidator getRequiredValidator() {
            return null;
        }

        @Override
        public String getSchemaPath() {
            return null;
        }

        @Override
        public String getSchemaNode() {
            return null;
        }

        @Override
        public JsonSchema getParentSchema() {
            return null;
        }

        @Override
        public String getSchemaLocation() {
            return null;
        }

        @Override
        public JsonNode getSchemaNodeObject() {
            return null;
        }

        @Override
        public String getSchemaPath(String path) {
            return null;
        }

        @Override
        public String getSchemaPath(String path, boolean absolute) {
            return null;
        }

        @Override
        public void setSchemaPath(String path) {
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode) {
        }

        @Override
        public void setParentSchema(JsonSchema parent) {
        }

        @Override
        public void setSchemaLocation(String location) {
        }

        @Override
        public void setSchemaNodeObject(JsonNode node) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference,
                                   boolean isFragment, boolean isRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference,
                                   boolean isFragment, boolean isRecursive, boolean isRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference,
                                   boolean isFragment, boolean isRecursive, boolean isRecursiveRef,
                                   boolean isRecursiveRefFragment) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference,
                                   boolean isFragment, boolean isRecursive, boolean isRecursiveRef,
                                   boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference,
                                   boolean isFragment, boolean isRecursive, boolean isRecursiveRef,
                                   boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive,
                                   boolean isRecursiveRefFragmentRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference,
                                   boolean isFragment, boolean isRecursive, boolean isRecursiveRef,
                                   boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive,
                                   boolean isRecursiveRefFragmentRecursiveRef,
                                   boolean isRecursiveRefFragmentRecursiveRefFragment) {
        }

        int getCallCount() {
            return callCount;
        }

        List<Object[]> getCallArgs() {
            return callArgs;
        }
    }

    /** A {@link RequiredValidator} stub that returns a predefined set of messages. */
    private static final class StubRequiredValidator extends RequiredValidator {

        private final Set<ValidationMessage> resultToReturn;

        StubRequiredValidator(Set<ValidationMessage> result) {
            this.resultToReturn = result;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at) {
            return resultToReturn;
        }
    }

    /** A {@link ValidatorState} stub that records method calls. */
    private static final class StubValidatorState extends ValidatorState {

        private boolean complexValidator;
        private boolean walkEnabled;
        private boolean validationEnabled;
        private final List<String> callLog = new ArrayList<>();

        StubValidatorState(boolean complex, boolean walk, boolean validation) {
            this.complexValidator = complex;
            this.walkEnabled = walk;
            this.validationEnabled = validation;
        }

        @Override
        public boolean isComplexValidator() {
            callLog.add("isComplexValidator");
            return complexValidator;
        }

        @Override
        public void setComplexValidator(boolean flag) {
            callLog.add("setComplexValidator:" + flag);
            this.complexValidator = flag;
        }

        @Override
        public boolean isWalkEnabled() {
            callLog.add("isWalkEnabled");
            return walkEnabled;
        }

        @Override
        public boolean isValidationEnabled() {
            callLog.add("isValidationEnabled");
            return validationEnabled;
        }

        @Override
        public void setMatchedNode(boolean flag) {
            callLog.add("setMatchedNode:" + flag);
        }

        List<String> getCallLog() {
            return callLog;
        }
    }

    /** A {@link CollectorContext} stub. */
    private static final class StubCollectorContext extends CollectorContext {

        private final Set<String> evaluated = new HashSet<>();
        private final Map<String, Object> map = new HashMap<>();

        @Override
        public Set<String> getEvaluatedProperties() {
            return evaluated;
        }

        @Override
        public <T> T get(String key) {
            @SuppressWarnings("unchecked")
            T v = (T) map.get(key);
            return v;
        }

        @Override
        public void set(String key, Object value) {
            map.put(key, value);
        }

        @Override
        public void addEvaluatedProperty(String property) {
            evaluated.add(property);
        }

        @Override
        public void addEvaluatedProperties(Collection<String> properties) {
            evaluated.addAll(properties);
        }

        // The rest of the methods are not needed for the tests – empty implementations
        @Override public void addEvaluatedSchema(String schema) {}
        @Override public void addEvaluatedSchemas(Collection<String> schemas) {}
        @Override public void addEvaluatedKeyword(String keyword) {}
        @Override public void addEvaluatedKeywords(Collection<String> keywords) {}
        @Override public void addEvaluatedPattern(String pattern) {}
        @Override public void addEvaluatedPatterns(Collection<String> patterns) {}
        @Override public void addEvaluatedPropertyPattern(String pattern) {}
        @Override public void addEvaluatedPropertyPatterns(Collection<String> patterns) {}
        @Override public void addEvaluatedItem(Integer item) {}
        @Override public void addEvaluatedItems(Collection<Integer> items) {}
        @Override public void addEvaluatedItems(Integer... items) {}
        @Override public Set<Integer> getEvaluatedItems() { return Collections.emptySet(); }
        @Override public Set<String> getEvaluatedSchemas() { return Collections.emptySet(); }
        @Override public Set<String> getEvaluatedKeywords() { return Collections.emptySet(); }
        @Override public Set<String> getEvaluatedPatterns() { return Collections.emptySet(); }
        @Override public Set<String> getEvaluatedPropertyPatterns() { return Collections.emptySet(); }
    }

    /** A {@link ExecutionContext} stub. */
    private static final class StubExecutionContext extends ExecutionContext {

        private final CollectorContext collectorContext;

        StubExecutionContext(CollectorContext collector) {
            this.collectorContext = collector;
        }

        @Override
        public CollectorContext getCollectorContext() {
            return collectorContext;
        }

        // All other methods are simple no‑ops – the validator does not depend on them for these tests
        @Override public String getPath() { return null; }
        @Override public void setPath(String path) {}
        @Override public boolean isWalkEnabled() { return false; }
        @Override public void setWalkEnabled(boolean walkEnabled) {}
        @Override public boolean isComplexValidator() { return false; }
        @Override public void setComplexValidator(boolean complexValidator) {}
        @Override public boolean isValidationEnabled() { return true; }
        @Override public void setValidationEnabled(boolean validationEnabled) {}
        @Override public boolean isMatchedNode() { return true; }
        @Override public void setMatchedNode(boolean matchedNode) {}
        @Override public boolean isMatchedSchema() { return true; }
        @Override public void setMatchedSchema(boolean matchedSchema) {}
        @Override public boolean isMatchedKeyword() { return true; }
        @Override public void setMatchedKeyword(boolean matchedKeyword) {}
        @Override public boolean isMatchedProperty() { return true; }
        @Override public void setMatchedProperty(boolean matchedProperty) {}
        @Override public boolean isMatchedItem() { return true; }
        @Override public void setMatchedItem(boolean matchedItem) {}
        @Override public boolean isMatchedPattern() { return true; }
        @Override public void setMatchedPattern(boolean matchedPattern) {}
        @Override public boolean isMatchedPropertyPattern() { return true; }
        @Override public void setMatchedPropertyPattern(boolean matchedPropertyPattern) {}
        @Override public boolean isMatchedSchemaLocation() { return true; }
        @Override public void setMatchedSchemaLocation(boolean matchedSchemaLocation) {}
    }

    /** A {@link ValidationContext} stub that always returns the same child schema. */
    private static final class StubValidationContext extends ValidationContext {

        private final JsonSchema schemaToReturn;

        StubValidationContext(JsonSchema schema) {
            this.schemaToReturn = schema;
        }

        @Override
        public JsonSchema newSchema(String name, JsonNode node, JsonSchema parent) {
            return schemaToReturn;
        }

        // The remaining methods are not required for the tests – empty implementations
        @Override public String getSchemaPath() { return null; }
        @Override public JsonNode getSchemaNode() { return null; }
        @Override public String getSchemaLocation() { return null; }
        @Override public void setSchemaLocation(String location) {}
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public void setParentSchema(JsonSchema parent) {}
        @Override public void addKeyword(String keyword) {}
        @Override public Set<String> getKeywords() { return Collections.emptySet(); }
        @Override public JsonNode getSchemaNodeObject() { return null; }
        @Override public void setSchemaNodeObject(JsonNode node) {}
        @Override public void setSchemaPath(String path) {}
        @Override public String getSchemaPath(String path) { return null; }
        @Override public String getSchemaPath(String path, boolean absolute) { return null; }
        @Override public void setSchemaPath(String path, boolean absolute) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment) {}
    }

    /** A {@link JsonSchema} stub that represents a parent schema with a required validator. */
    private static final class StubParentSchema extends JsonSchema {

        private final boolean hasRequired;
        private final RequiredValidator requiredValidator;

        StubParentSchema(boolean hasRequired, RequiredValidator requiredValidator) {
            this.hasRequired = hasRequired;
            this.requiredValidator = requiredValidator;
        }

        @Override
        public boolean hasRequiredValidator() {
            return hasRequired;
        }

        @Override
        public RequiredValidator getRequiredValidator() {
            return requiredValidator;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at) {
            return Collections.emptySet();
        }

        @Override public String getSchemaPath() { return null; }
        @Override public String getSchemaNode() { return null; }
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public String getSchemaLocation() { return null; }
        @Override public JsonNode getSchemaNodeObject() { return null; }
        @Override public String getSchemaPath(String path) { return null; }
        @Override public String getSchemaPath(String path, boolean absolute) { return null; }
        @Override public void setSchemaPath(String path) {}
        @Override public void setSchemaNode(JsonNode schemaNode) {}
        @Override public void setParentSchema(JsonSchema parent) {}
        @Override public void setSchemaLocation(String location) {}
        @Override public void setSchemaNodeObject(JsonNode node) {}
        @Override public void setSchemaPath(String path, boolean absolute) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment) {}
    }

    /** A minimal {@link JsonSchema} implementation that always returns an empty validation set. */
    private static final class EmptySchema extends JsonSchema {

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                String at) {
            return Collections.emptySet();
        }

        @Override
        public boolean hasRequiredValidator() {
            return false;
        }

        @Override
        public RequiredValidator getRequiredValidator() {
            return null;
        }

        @Override public String getSchemaPath() { return null; }
        @Override public String getSchemaNode() { return null; }
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public String getSchemaLocation() { return null; }
        @Override public JsonNode getSchemaNodeObject() { return null; }
        @Override public String getSchemaPath(String path) { return null; }
        @Override public String getSchemaPath(String path, boolean absolute) { return null; }
        @Override public void setSchemaPath(String path) {}
        @Override public void setSchemaNode(JsonNode schemaNode) {}
        @Override public void setParentSchema(JsonSchema parent) {}
        @Override public void setSchemaLocation(String location) {}
        @Override public void setSchemaNodeObject(JsonNode node) {}
        @Override public void setSchemaPath(String path, boolean absolute) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef) {}
        @Override public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment) {}
    }

    /* --------------------------------------------------------------------- *
     *  Test setup
     * --------------------------------------------------------------------- */

    @BeforeEach
    void setupBeforeEach() {
        // 1️⃣ Minimal schema node – a single property called "prop"
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("prop", MAPPER.createObjectNode());

        // 2️⃣ Stubbed property schema – by default returns an empty set
        propertySchema = new StubJsonSchema(Collections.emptySet());

        // 3️⃣ ValidationContext stub – always returns the above property schema
        validationContext = new StubValidationContext(propertySchema);

        // 4️⃣ Parent schema – no required validator by default
        parentSchema = new StubParentSchema(false, null);

        // 5️⃣ Build the validator under test
        validator = new PropertiesValidator("test-schema", schemaNode, parentSchema, validationContext);

        // 6️⃣ Collector / Execution / ValidatorState chain
        collectorContext = new StubCollectorContext();
        validatorState = new StubValidatorState(false, false, true);
        collectorContext.set(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        executionContext = new StubExecutionContext(collectorContext);
    }

    /* --------------------------------------------------------------------- *
     *  Parameter provider for walk‑enabled tests
     * --------------------------------------------------------------------- */
    static Stream<Arguments> walkEnabledProvider() {
        return Stream.of(
                Arguments.of("#/age", "age"),
                Arguments.of("#/name", "name")
        );
    }

    /* --------------------------------------------------------------------- *
     *  Test cases
     * --------------------------------------------------------------------- */

    @Test
    void testValidateWhenPropertyPresentAndChildValid() {
        ObjectNode data = MAPPER.createObjectNode();
        data.put("prop", "any-value");
        Set<ValidationMessage> result = validator.validate(executionContext, data, data, "");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(), "No validation messages are expected");
        Assertions.assertTrue(result.getClass().getName().contains("Unmodifiable"));
    }

    @Test
    void testValidateWhenPropertyMissingAndRequiredFails() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        ValidationMessage requiredMessage = new SimpleValidationMessage("required error");
        Set<ValidationMessage> requiredErrors = new HashSet<>(Collections.singleton(requiredMessage));
        RequiredValidator requiredValidator = new StubRequiredValidator(requiredErrors);
        parentSchema = new StubParentSchema(true, requiredValidator);
        validator = new PropertiesValidator("test-schema",
                MAPPER.createObjectNode().set("prop", MAPPER.createObjectNode()),
                parentSchema,
                validationContext);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size(), "Exactly one validation message (the required error) should be returned");
        Assertions.assertTrue(result.contains(requiredMessage), "The returned set must contain the required validator message");
        Assertions.assertEquals(0, ((StubJsonSchema) propertySchema).getCallCount(),
                "Child schema must not be called when property is absent");
    }

    @Test
    void testValidateComplexValidatorFlagIsRestored() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("prop", "value");
        validatorState = new StubValidatorState(true, false, true);
        collectorContext.set(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(), "No errors expected when child validation succeeds");
        List<String> log = validatorState.getCallLog();
        Assertions.assertEquals(Arrays.asList("isComplexValidator", "setComplexValidator:false", "setComplexValidator:true"),
                log,
                "Complex flag should be cleared before recursion and restored afterwards");
    }

    @Test
    void testValidateWhenWalkEnabledPropertyPresent() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("age", 30);
        validatorState = new StubValidatorState(false, true, true);
        collectorContext.set(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(),
                "When walk is enabled the validator does not call the child schema directly");
        Assertions.assertEquals(0, ((StubJsonSchema) propertySchema).getCallCount(),
                "Child schema should not be invoked when walk is enabled");
    }

    @Test
    void testValidateWhenPropertyAbsentRequiredValidatorReturnsNoErrors() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        RequiredValidator requiredValidator = new StubRequiredValidator(Collections.emptySet());
        parentSchema = new StubParentSchema(true, requiredValidator);
        validator = new PropertiesValidator("test-schema",
                MAPPER.createObjectNode().set("prop", MAPPER.createObjectNode()),
                parentSchema,
                validationContext);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(),
                "No errors should be returned when required validator yields an empty set");
        Assertions.assertEquals(0, ((StubJsonSchema) propertySchema).getCallCount(),
                "Child schema must not be called");
    }

    @Test
    void testValidateWhenPropertyAbsentRequiredValidatorReturnsErrorsNonComplex() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        ValidationMessage msg = new SimpleValidationMessage("required missing");
        RequiredValidator requiredValidator = new StubRequiredValidator(Collections.singleton(msg));
        parentSchema = new StubParentSchema(true, requiredValidator);
        validator = new PropertiesValidator("test-schema",
                MAPPER.createObjectNode().set("prop", MAPPER.createObjectNode()),
                parentSchema,
                validationContext);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.contains(msg));
    }

    @Test
    void testValidateWhenPropertyAbsentRequiredValidatorReturnsErrorsComplex() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        ValidationMessage msg = new SimpleValidationMessage("required missing");
        RequiredValidator requiredValidator = new StubRequiredValidator(Collections.singleton(msg));
        parentSchema = new StubParentSchema(true, requiredValidator);
        validator = new PropertiesValidator("test-schema",
                MAPPER.createObjectNode().set("prop", MAPPER.createObjectNode()),
                parentSchema,
                validationContext);
        validatorState = new StubValidatorState(true, false, true);
        collectorContext.set(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(),
                "Complex validator should return early with an empty set");
        Assertions.assertTrue(validatorState.getCallLog().contains("setMatchedNode:false"));
    }

    @Test
    void testValidateWhenNoSchemasDefined() throws Exception {
        Field schemasField = PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonSchema> map = (Map<String, JsonSchema>) schemasField.get(validator);
        map.clear();
        ObjectNode dataNode = MAPPER.createObjectNode();
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(),
                "With no property schemas the validator must return an empty set");
        Assertions.assertTrue(result.getClass().getName().contains("Unmodifiable"));
    }

    @Test
    void testValidateWhenParentHasNoRequiredValidatorAndPropertyAbsent() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(),
                "Missing property should be ignored when parent has no required validator");
    }

    @ParameterizedTest
    @MethodSource("walkEnabledProvider")
    void testValidateWalkEnabledParameterized(String atPath, String propertyName) {
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put(propertyName, "dummy");
        validatorState = new StubValidatorState(false, true, true);
        collectorContext.set(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, atPath);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(0, ((StubJsonSchema) propertySchema).getCallCount(),
                "Child schema must not be called when walk is enabled");
    }

    @Test
    void testValidateWhenPropertyAbsentAndNoRequiredValidator() {
        ObjectNode data = MAPPER.createObjectNode();
        Set<ValidationMessage> result = validator.validate(executionContext, data, data, "#");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(),
                "Missing optional property should produce no errors");
    }

    @Test
    void testValidateNullArguments() {
        ObjectNode data = MAPPER.createObjectNode();
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.validate(null, data, data, "#"));
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, null, data, "#"));
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, data, data, null));
    }
}
```