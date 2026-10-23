
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class PropertiesValidator_validate_Test {

    /**
     * The validator under test.
     */
    private PropertiesValidator validator;

    /**
     * Mocked collaborators.
     */
    @Mock
    ValidationContext validationContext;

    @Mock
    JsonSchema parentSchema;

    // schema for the single property defined in the test schema
    @Mock
    JsonSchema propertySchema;

    @Mock
    ExecutionContext executionContext;

    @Mock
    CollectorContext collectorContext;

    @Mock
    ValidatorState validatorState;

    /**
     * Helper objects used in several tests.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    /**
     * A {@link JsonSchema} that records calls to {@code validate}.
     */
    private static final class StubJsonSchema extends JsonSchema {

        private final Set<ValidationMessage> resultToReturn;

        private int callCount = 0;

        private final List<Object[]> callArgs = new ArrayList<>();

        StubJsonSchema(Set<ValidationMessage> result) {
            this.resultToReturn = result;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at) {
            callCount++;
            callArgs.add(new Object[] { ctx, node, rootNode, at });
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
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment) {
        }

        int getCallCount() {
            return callCount;
        }

        List<Object[]> getCallArgs() {
            return callArgs;
        }
    }

    /**
     * A {@link RequiredValidator} stub that returns a predefined set of messages.
     */
    private static final class StubRequiredValidator extends RequiredValidator {

        private final Set<ValidationMessage> resultToReturn;

        StubRequiredValidator(Set<ValidationMessage> result) {
            this.resultToReturn = result;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at) {
            return resultToReturn;
        }
    }

    /**
     * A {@link ValidatorState} stub that records method calls.
     */
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

    /**
     * A {@link CollectorContext} stub.
     */
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
        @Override
        public void addEvaluatedSchema(String schema) {
        }

        @Override
        public void addEvaluatedSchemas(Collection<String> schemas) {
        }

        @Override
        public void addEvaluatedKeyword(String keyword) {
        }

        @Override
        public void addEvaluatedKeywords(Collection<String> keywords) {
        }

        @Override
        public void addEvaluatedPattern(String pattern) {
        }

        @Override
        public void addEvaluatedPatterns(Collection<String> patterns) {
        }

        @Override
        public void addEvaluatedPropertyPattern(String pattern) {
        }

        @Override
        public void addEvaluatedPropertyPatterns(Collection<String> patterns) {
        }

        @Override
        public void addEvaluatedItem(Integer item) {
        }

        @Override
        public void addEvaluatedItems(Collection<Integer> items) {
        }

        @Override
        public void addEvaluatedItems(Integer... items) {
        }

        @Override
        public Set<Integer> getEvaluatedItems() {
            return Collections.emptySet();
        }

        @Override
        public Set<String> getEvaluatedSchemas() {
            return Collections.emptySet();
        }

        @Override
        public Set<String> getEvaluatedKeywords() {
            return Collections.emptySet();
        }

        @Override
        public Set<String> getEvaluatedPatterns() {
            return Collections.emptySet();
        }

        @Override
        public Set<String> getEvaluatedPropertyPatterns() {
            return Collections.emptySet();
        }
    }

    /**
     * A {@link ExecutionContext} stub.
     */
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
        @Override
        public String getPath() {
            return null;
        }

        @Override
        public void setPath(String path) {
        }

        @Override
        public boolean isWalkEnabled() {
            return false;
        }

        @Override
        public void setWalkEnabled(boolean walkEnabled) {
        }

        @Override
        public boolean isComplexValidator() {
            return false;
        }

        @Override
        public void setComplexValidator(boolean complexValidator) {
        }

        @Override
        public boolean isValidationEnabled() {
            return true;
        }

        @Override
        public void setValidationEnabled(boolean validationEnabled) {
        }

        @Override
        public boolean isMatchedNode() {
            return true;
        }

        @Override
        public void setMatchedNode(boolean matchedNode) {
        }

        @Override
        public boolean isMatchedSchema() {
            return true;
        }

        @Override
        public void setMatchedSchema(boolean matchedSchema) {
        }

        @Override
        public boolean isMatchedKeyword() {
            return true;
        }

        @Override
        public void setMatchedKeyword(boolean matchedKeyword) {
        }

        @Override
        public boolean isMatchedProperty() {
            return true;
        }

        @Override
        public void setMatchedProperty(boolean matchedProperty) {
        }

        @Override
        public boolean isMatchedItem() {
            return true;
        }

        @Override
        public void setMatchedItem(boolean matchedItem) {
        }

        @Override
        public boolean isMatchedPattern() {
            return true;
        }

        @Override
        public void setMatchedPattern(boolean matchedPattern) {
        }

        @Override
        public boolean isMatchedPropertyPattern() {
            return true;
        }

        @Override
        public void setMatchedPropertyPattern(boolean matchedPropertyPattern) {
        }

        @Override
        public boolean isMatchedSchemaLocation() {
            return true;
        }

        @Override
        public void setMatchedSchemaLocation(boolean matchedSchemaLocation) {
        }
    }

    /**
     * A {@link ValidationContext} stub that always returns the same child schema.
     */
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
        @Override
        public String getSchemaPath() {
            return null;
        }

        @Override
        public JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public String getSchemaLocation() {
            return null;
        }

        @Override
        public void setSchemaLocation(String location) {
        }

        @Override
        public JsonSchema getParentSchema() {
            return null;
        }

        @Override
        public void setParentSchema(JsonSchema parent) {
        }

        @Override
        public void addKeyword(String keyword) {
        }

        @Override
        public Set<String> getKeywords() {
            return Collections.emptySet();
        }

        @Override
        public JsonNode getSchemaNodeObject() {
            return null;
        }

        @Override
        public void setSchemaNodeObject(JsonNode node) {
        }

        @Override
        public void setSchemaPath(String path) {
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
        public void setSchemaPath(String path, boolean absolute) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment) {
        }
    }

    /**
     * A {@link JsonSchema} stub that represents a parent schema with a required validator.
     */
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
        public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at) {
            return Collections.emptySet();
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
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment) {
        }
    }

    /**
     * A minimal {@link JsonSchema} implementation that always returns an empty validation set.
     */
    private static final class EmptySchema extends JsonSchema {

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at) {
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
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment) {
        }
    }

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
     *  Parameterized test source for walk‑enabled property present case.  *
     * --------------------------------------------------------------------- */
    static Stream<Arguments> walkEnabledProvider() {
        return Stream.of(Arguments.of("#/age", "age"), Arguments.of("#/name", "name"));
    }

    /* --------------------------------------------------------------------- *
     *  Test 1 – property is present, validation of the child schema succeeds *
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

    /* --------------------------------------------------------------------- *
     *  Test 2 – property is missing and the parent schema has a required   *
     *           validator that reports an error.                           *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenPropertyMissingAndRequiredFails() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        ValidationMessage requiredMessage = new SimpleValidationMessage("required error");
        Set<ValidationMessage> requiredErrors = new HashSet<>(Collections.singleton(requiredMessage));
        RequiredValidator requiredValidator = new StubRequiredValidator(requiredErrors);
        parentSchema = new StubParentSchema(true, requiredValidator);
        validator = new PropertiesValidator("test-schema", MAPPER.createObjectNode().set("prop", MAPPER.createObjectNode()), parentSchema, validationContext);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size(), "Exactly one validation message (the required error) should be returned");
        Assertions.assertTrue(result.contains(requiredMessage), "The returned set must contain the required validator message");
        Assertions.assertEquals(0, ((StubJsonSchema) propertySchema).getCallCount(), "Child schema must not be called when property is absent");
    }

    /* --------------------------------------------------------------------- *
     *  Test 3 – complex validator scenario (e.g. oneOf) where the state is *
     *           marked as complex. In this simplified test we only verify *
     *           that the state flag is restored after the recursive call. *
     * --------------------------------------------------------------------- */
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
        Assertions.assertEquals(Arrays.asList("isComplexValidator", "setComplexValidator:false", "setComplexValidator:true"), log, "Complex flag should be cleared before recursion and restored afterwards");
    }

    /* --------------------------------------------------------------------- *
     *  Walk enabled – property present, validator should delegate to       *
     *  walkSchema (child schema must NOT be called).                       *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenWalkEnabledPropertyPresent() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("age", 30);
        validatorState = new StubValidatorState(false, true, true);
        collectorContext.set(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(), "When walk is enabled the validator does not call the child schema directly");
        Assertions.assertEquals(0, ((StubJsonSchema) propertySchema).getCallCount(), "Child schema should not be invoked when walk is enabled");
    }

    /* --------------------------------------------------------------------- *
     *  Required validator returns no errors (property absent).             *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenPropertyAbsentRequiredValidatorReturnsNoErrors() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        RequiredValidator requiredValidator = new StubRequiredValidator(Collections.emptySet());
        parentSchema = new StubParentSchema(true, requiredValidator);
        validator = new PropertiesValidator("test-schema", MAPPER.createObjectNode().set("prop", MAPPER.createObjectNode()), parentSchema, validationContext);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(), "No errors should be returned when required validator yields an empty set");
        Assertions.assertEquals(0, ((StubJsonSchema) propertySchema).getCallCount(), "Child schema must not be called");
    }

    /* --------------------------------------------------------------------- *
     *  Required validator returns errors, validator is NOT complex.       *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenPropertyAbsentRequiredValidatorReturnsErrorsNonComplex() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        ValidationMessage msg = new SimpleValidationMessage("required missing");
        RequiredValidator requiredValidator = new StubRequiredValidator(Collections.singleton(msg));
        parentSchema = new StubParentSchema(true, requiredValidator);
        validator = new PropertiesValidator("test-schema", MAPPER.createObjectNode().set("prop", MAPPER.createObjectNode()), parentSchema, validationContext);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.contains(msg));
    }

    /* --------------------------------------------------------------------- *
     *  Required validator returns errors, validator IS complex – early  *
     *  exit and matchedNode set to false.                                   *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenPropertyAbsentRequiredValidatorReturnsErrorsComplex() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        ValidationMessage msg = new SimpleValidationMessage("required missing");
        RequiredValidator requiredValidator = new StubRequiredValidator(Collections.singleton(msg));
        parentSchema = new StubParentSchema(true, requiredValidator);
        validator = new PropertiesValidator("test-schema", MAPPER.createObjectNode().set("prop", MAPPER.createObjectNode()), parentSchema, validationContext);
        validatorState = new StubValidatorState(true, false, true);
        collectorContext.set(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(), "Complex validator should return early with an empty set");
        Assertions.assertTrue(validatorState.getCallLog().contains("setMatchedNode:false"));
    }

    /* --------------------------------------------------------------------- *
     *  No schemas defined – the internal map is empty.                     *
     * --------------------------------------------------------------------- */
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
        Assertions.assertTrue(result.isEmpty(), "With no property schemas the validator must return an empty set");
        Assertions.assertTrue(result.getClass().getName().contains("Unmodifiable"));
    }

    /* --------------------------------------------------------------------- *
     *  Parent schema without required validator, property absent.          *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenParentHasNoRequiredValidatorAndPropertyAbsent() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(), "Missing property should be ignored when parent has no required validator");
    }

    /* --------------------------------------------------------------------- *
     *  Walk enabled – simulate errors returned from walkSchema.            *
     *  Since walkSchema is private we cannot stub it directly; we verify *
     *  that the child schema is *not* invoked and the result is empty.   *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWalkEnabledWithErrorsFromWalkSchema() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("age", 30);
        // Enable walk via the validator state
        validatorState = new StubValidatorState(false, true, true);
        collectorContext.set(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        executionContext = new StubExecutionContext(collectorContext);
        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
        Assertions.assertEquals(0, ((StubJsonSchema) propertySchema).getCallCount(), "Child schema must not be called when walk is enabled");
    }

    /* --------------------------------------------------------------------- *
     *  Multiple properties – one present, one missing with required error. *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateMultiplePropertiesMixedPresence() throws Exception {
        // Arrange – build a schema node with two properties
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("presentProp", MAPPER.createObjectNode());
        schemaNode.set("missingProp", MAPPER.createObjectNode());
        // Stub for the "presentProp" schema – returns a validation message
        JsonSchema presentSchema = new JsonSchema() {

            @Override
            public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at) {
                ValidationMessage vm = new SimpleValidationMessage("presentError");
                return Collections.singleton(vm);
            }

            // other methods – empty implementations
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

            // all overloaded setters – empty
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
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursive) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursive, boolean isRecursiveRef) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment) {
            }
        };
        // ValidationContext that returns the appropriate schema based on property name
        validationContext = new ValidationContext() {

            @Override
            public JsonSchema newSchema(String name, JsonNode node, JsonSchema parent) {
                if ("presentProp".equals(name)) {
                    return presentSchema;
                }
                // missingProp – we do not need a child schema because it is absent
                return null;
            }

            // other methods – empty implementations
            @Override
            public String getSchemaPath() {
                return null;
            }

            @Override
            public JsonNode getSchemaNode() {
                return null;
            }

            @Override
            public String getSchemaLocation() {
                return null;
            }

            @Override
            public void setSchemaLocation(String location) {
            }

            @Override
            public JsonSchema getParentSchema() {
                return null;
            }

            @Override
            public void setParentSchema(JsonSchema parent) {
            }

            @Override
            public void addKeyword(String keyword) {
            }

            @Override
            public Set<String> getKeywords() {
                return Collections.emptySet();
            }

            @Override
            public JsonNode getSchemaNodeObject() {
                return null;
            }

            @Override
            public void setSchemaNodeObject(JsonNode node) {
            }

            @Override
            public void setSchemaPath(String path) {
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
            public void setSchemaPath(String path, boolean absolute) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursive) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursive, boolean isRecursiveRef) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef) {
            }

            @Override
            public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment) {
            }
        };
        // Required validator for the missing property
        ValidationMessage requiredMsg = new SimpleValidationMessage("'missingProp' is required");
        RequiredValidator requiredValidator = new StubRequiredValidator(Collections.singleton(requiredMsg));
        parentSchema = new StubParentSchema(true, requiredValidator);
        // Re‑create the validator with the new context
        validator = new PropertiesValidator("test-schema", schemaNode, parentSchema, validationContext);
        // Data node contains only the present property
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("presentProp", "value");
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size(), "Both the child error and the required error should be present");
        Assertions.assertTrue(result.stream().anyMatch(vm -> "presentError".equals(vm.getMessage())));
        Assertions.assertTrue(result.stream().anyMatch(vm -> "'missingProp' is required".equals(vm.getMessage())));
        // Evaluated properties should contain only the present one
        Set<String> evaluated = collectorContext.getEvaluatedProperties();
        Assertions.assertEquals(1, evaluated.size());
        Assertions.assertTrue(evaluated.contains("#/presentProp"));
    }

    /* --------------------------------------------------------------------- *
     *  Property present – non‑complex validator, matchedNode unchanged.    *
     * --------------------------------------------------------------------- */
    @Test
    void testValidatePropertyPresentNonComplexMatchedNodeUnchanged() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("simple", "value");
        validatorState = new StubValidatorState(false, false, true);
        collectorContext.set(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        executionContext = new StubExecutionContext(collectorContext);
        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
        // matchedNode should never be toggled for a non‑complex validator
        Assertions.assertFalse(validatorState.getCallLog().contains("setMatchedNode:true"));
        Assertions.assertFalse(validatorState.getCallLog().contains("setMatchedNode:false"));
    }

    /* --------------------------------------------------------------------- *
     *  Null ExecutionContext – expect NullPointerException.               *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullExecutionContext() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("any", "value");
        Assertions.assertThrows(NullPointerException.class, () -> validator.validate(null, dataNode, dataNode, "#"));
    }

    /* --------------------------------------------------------------------- *
     *  Null node argument – expect NullPointerException.                  *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullNode() {
        Assertions.assertThrows(NullPointerException.class, () -> validator.validate(executionContext, null, MAPPER.createObjectNode(), "#"));
    }

    /* --------------------------------------------------------------------- *
     *  CollectorContext missing ValidatorState – expect NPE or CCE.       *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateCollectorContextMissingValidatorState() {
        // remove the validator state from the collector
        collectorContext.set(ValidatorState.VALIDATOR_STATE_KEY, null);
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("prop", "value");
        Assertions.assertThrows(NullPointerException.class, () -> validator.validate(executionContext, dataNode, dataNode, "#"));
    }

    /* --------------------------------------------------------------------- *
     *  Null parent schema – expect NullPointerException when required    *
     *  validator is accessed.                                             *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullParentSchema() throws Exception {
        // construct a validator with a null parent schema
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("anyProp", MAPPER.createObjectNode());
        validator = new PropertiesValidator("test-schema", schemaNode, null, validationContext);
        ObjectNode dataNode = MAPPER.createObjectNode();
        Assertions.assertThrows(NullPointerException.class, () -> validator.validate(executionContext, dataNode, dataNode, "#"));
    }

    /* --------------------------------------------------------------------- *
     *  Null "at" path – expect NullPointerException inside atPath.       *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullAtPath() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("name", "value");
        Assertions.assertThrows(NullPointerException.class, () -> validator.validate(executionContext, dataNode, dataNode, null));
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
        Assertions.assertEquals(0, ((StubJsonSchema) propertySchema).getCallCount(), "Child schema must not be called when walk is enabled");
    }

    @Test
    void testValidateWhenPropertyAbsentAndNoRequiredValidator() {
        ObjectNode data = MAPPER.createObjectNode();
        Set<ValidationMessage> result = validator.validate(executionContext, data, data, "#");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(), "Missing optional property should produce no errors");
    }

    @Test
    void testValidateNullArguments() {
        ObjectNode data = MAPPER.createObjectNode();
        Assertions.assertThrows(NullPointerException.class, () -> validator.validate(null, data, data, "#"));
        Assertions.assertThrows(NullPointerException.class, () -> validator.validate(executionContext, null, data, "#"));
        Assertions.assertThrows(NullPointerException.class, () -> validator.validate(executionContext, data, data, null));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:11: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:12: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:15: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:16: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:19: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:33: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:37: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:40: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:43: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:46: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:82: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:88: error: getArguments() in SimpleValidationMessage cannot override getArguments() in ValidationMessage
        public Map<String, Object> getArguments() {
                                   ^
  return type Map<String,Object> is not compatible with String[]
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:87: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:126: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:132: error: getArguments() in SimpleMessage cannot override getArguments() in ValidationMessage
        public Map<String, Object> getArguments() {
                                   ^
  return type Map<String,Object> is not compatible with String[]
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:131: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:153: error: no suitable constructor found for JsonSchema(no arguments)
        StubJsonSchema(Set<ValidationMessage> result) {
                                                      ^
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:180: error: getSchemaNode() in StubJsonSchema cannot override getSchemaNode() in BaseJsonValidator
        public String getSchemaNode() {
                      ^
  return type String is not compatible with JsonNode
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:179: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:189: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:194: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:199: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:204: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:209: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:213: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:217: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:221: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:225: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:229: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:233: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:237: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:241: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:245: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:249: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:253: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:257: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:261: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:281: error: constructor RequiredValidator in class RequiredValidator cannot be applied to given types;
        StubRequiredValidator(Set<ValidationMessage> result) {
                                                             ^
  required: String,JsonNode,JsonSchema,ValidationContext
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:359: error: name clash: get(String) in StubCollectorContext and get(String) in CollectorContext have the same erasure, yet neither overrides the other
        public <T> T get(String key) {
                     ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:358: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:365: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:370: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:375: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:381: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:385: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:389: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:393: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:397: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:401: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:405: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:409: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:413: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:417: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:421: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:426: error: getEvaluatedItems() in StubCollectorContext cannot override getEvaluatedItems() in CollectorContext
        public Set<Integer> getEvaluatedItems() {
                            ^
  return type Set<Integer> is not compatible with Collection<String>
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:425: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:430: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:435: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:440: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:445: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:468: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:473: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:477: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:482: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:486: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:491: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:495: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:500: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:504: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:509: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:513: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:518: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:522: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:527: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:531: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:536: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:540: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:545: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:549: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:554: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:558: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:563: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:567: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:572: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:584: error: constructor ValidationContext in class ValidationContext cannot be applied to given types;
        StubValidationContext(JsonSchema schema) {
                                                 ^
  required: URIFactory,URNFactory,JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:594: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:599: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:604: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:609: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:613: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:618: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:622: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:626: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:631: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:636: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:640: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:644: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:649: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:654: error: method does not override or implement a method from a supertype
        @Override
        ^
100 errors
only showing the first 100 errors, of 261 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.PropertiesValidator`:\n\tdependent fields:\n\t\tjava.util.Map<java.lang.String, com.networknt.schema.JsonSchema> com.networknt.schema.PropertiesValidator.schemas;\n\t\torg.slf4j.Logger com.networknt.schema.PropertiesValidator.logger;\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\n\t\tmethod `private void walkSchema(ExecutionContext, Map.Entry<String, JsonSchema>, JsonNode, JsonNode, String, boolean, Set<ValidationMessage>, WalkListenerRunner)` returns `void`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.ValidatorState`:\n\tdependent fields:\n\t\tjava.lang.String com.networknt.schema.ValidatorState.VALIDATOR_STATE_KEY;\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `private static void resetValidatorState(CollectorContext)` returns `void`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.PatternPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.AdditionalPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.AnyOfValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


