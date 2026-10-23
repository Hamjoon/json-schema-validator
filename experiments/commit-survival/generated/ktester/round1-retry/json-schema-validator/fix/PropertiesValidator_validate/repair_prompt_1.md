
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
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

    /**
     * Simple {@link ValidationMessage} implementation.
     */
    private static final class SimpleValidationMessage implements ValidationMessage {

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

    /**
     * Stub for {@link JsonSchema}.  It records calls to {@code validate}.
     */
    private static final class StubJsonSchema implements JsonSchema {

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

        // ----------- methods required only for the tests --------------------
        @Override
        public boolean hasRequiredValidator() {
            return false;
        }

        @Override
        public RequiredValidator getRequiredValidator() {
            return null;
        }

        // All other JsonSchema methods are not needed for these tests.
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

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRefFragment) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRefFragmentRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRefFragmentRecursiveRef) {
        }

        // getters for verification
        int getCallCount() {
            return callCount;
        }

        List<Object[]> getCallArgs() {
            return callArgs;
        }
    }

    /**
     * Stub for {@link RequiredValidator}.
     */
    private static final class StubRequiredValidator implements RequiredValidator {

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
     * Stub for {@link ValidatorState}.  It records method calls.
     */
    private static final class StubValidatorState implements ValidatorState {

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

        // getters for verification
        List<String> getCallLog() {
            return callLog;
        }
    }

    /**
     * Stub for {@link CollectorContext}.
     */
    private static final class StubCollectorContext implements CollectorContext {

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

        // other methods are not needed for these tests
        @Override
        public void addEvaluatedProperty(String property) {
            evaluated.add(property);
        }

        @Override
        public void addEvaluatedProperties(Collection<String> properties) {
            evaluated.addAll(properties);
        }

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
        public void addEvaluatedItems(Collection<Integer> items) {
        }

        @Override
        public void addEvaluatedItem(Integer item) {
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
     * Stub for {@link ExecutionContext}.
     */
    private static final class StubExecutionContext implements ExecutionContext {

        private final CollectorContext collectorContext;

        StubExecutionContext(CollectorContext collector) {
            this.collectorContext = collector;
        }

        @Override
        public CollectorContext getCollectorContext() {
            return collectorContext;
        }

        // other methods are not required for these tests
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
     * Stub for {@link ValidationContext}.
     */
    private static final class StubValidationContext implements ValidationContext {

        private final JsonSchema schemaToReturn;

        StubValidationContext(JsonSchema schema) {
            this.schemaToReturn = schema;
        }

        @Override
        public JsonSchema newSchema(String name, JsonNode node, JsonSchema parent) {
            return schemaToReturn;
        }

        // other methods are not needed for these tests
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
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRefFragment) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRefFragmentRecursive) {
        }
    }

    /**
     * Stub for parent schema – only the required‑validator related methods are needed.
     */
    private static final class StubParentSchema implements JsonSchema {

        private boolean hasRequired;

        private RequiredValidator requiredValidator;

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

        // other methods are not used in the tests
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

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRef) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRefFragment) {
        }

        @Override
        public void setSchemaPath(String path, boolean absolute, boolean isReference, boolean isFragment, boolean isRecursive, boolean isRecursiveRef, boolean isRecursiveRefFragment, boolean isRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursive, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRef, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRefFragment, boolean isRecursiveRefFragmentRecursiveRefFragmentRecursiveRefFragmentRecursive) {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // 1️⃣  Minimal schema node – a single property called "prop"
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("prop", MAPPER.createObjectNode());
        // 2️⃣  Stubbed property schema – by default returns an empty set
        propertySchema = new StubJsonSchema(Collections.emptySet());
        // 3️⃣  ValidationContext stub – always returns the above property schema
        validationContext = new StubValidationContext(propertySchema);
        // 4️⃣  Parent schema – no required validator by default
        parentSchema = new StubParentSchema(false, null);
        // 5️⃣  Build the validator under test
        validator = new PropertiesValidator("test-schema", schemaNode, parentSchema, validationContext);
        // 6️⃣  Collector / Execution / ValidatorState chain
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
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("prop", "any-value");
        // child schema already returns empty set (configured in @BeforeEach)
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "");
        assertNotNull(result);
        assertTrue(result.isEmpty(), "No validation messages are expected when the property validates successfully");
        assertEquals(1, propertySchema.getCallCount(), "Child schema should be invoked exactly once");
        Object[] args = propertySchema.getCallArgs().get(0);
        assertSame(executionContext, args[0]);
        assertSame(dataNode.get("prop"), args[1]);
        assertSame(dataNode, args[2]);
        assertEquals("", args[3]);
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
        // recreate validator so it picks up the new parent schema
        validator = new PropertiesValidator("test-schema", MAPPER.createObjectNode().set("prop", MAPPER.createObjectNode()), parentSchema, validationContext);
        // re‑wire execution context (collector already contains validatorState)
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        assertNotNull(result);
        assertEquals(1, result.size(), "Exactly one validation message (the required error) should be returned");
        assertTrue(result.contains(requiredMessage), "The returned set must contain the required validator message");
        assertEquals(0, propertySchema.getCallCount(), "Child schema must not be called when property is absent");
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
        // make validatorState report that we are inside a complex validator
        validatorState = new StubValidatorState(true, false, true);
        collectorContext.set(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        assertNotNull(result);
        assertTrue(result.isEmpty(), "No errors expected when child validation succeeds");
        List<String> log = validatorState.getCallLog();
        // Expected order: isComplexValidator, setComplexValidator:false, setComplexValidator:true
        assertEquals(Arrays.asList("isComplexValidator", "setComplexValidator:false", "setComplexValidator:true"), log, "Complex flag should be cleared before recursion and restored afterwards");
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
        assertNotNull(result);
        // walkSchema returns an empty set in the real implementation; we only assert no direct call.
        assertTrue(result.isEmpty(), "When walk is enabled the validator does not call the child schema directly");
        assertEquals(0, propertySchema.getCallCount(), "Child schema should not be invoked when walk is enabled");
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
        assertNotNull(result);
        assertTrue(result.isEmpty(), "No errors should be returned when required validator yields an empty set");
        assertEquals(0, propertySchema.getCallCount(), "Child schema must not be called");
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
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(msg));
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
        // make the validator state report complex
        validatorState = new StubValidatorState(true, false, true);
        collectorContext.set(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        executionContext = new StubExecutionContext(collectorContext);
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Complex validator should return early with an empty set");
        // verify that setMatchedNode(false) was called
        assertTrue(validatorState.getCallLog().contains("setMatchedNode:false"));
    }

    /* --------------------------------------------------------------------- *
     *  No schemas defined – the internal map is empty.                     *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenNoSchemasDefined() throws Exception {
        // clear the internal `schemas` map via reflection
        Field schemasField = PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonSchema> map = (Map<String, JsonSchema>) schemasField.get(validator);
        map.clear();
        ObjectNode dataNode = MAPPER.createObjectNode();
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        assertNotNull(result);
        assertTrue(result.isEmpty(), "With no property schemas the validator must return an empty set");
        assertTrue(result.getClass().getName().contains("Unmodifiable"), "Result set should be immutable");
    }

    /* --------------------------------------------------------------------- *
     *  Parent schema without required validator, property absent.          *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateWhenParentHasNoRequiredValidatorAndPropertyAbsent() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        // parentSchema already has no required validator (default)
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Missing property should be ignored when parent has no required validator");
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
        when(validatorState.isWalkEnabled()).thenReturn(true);
        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        // Assert
        assertNotNull(result);
        // In the real implementation walkSchema would add errors; here we only ensure
        // that the validator does not call the child schema directly.
        assertTrue(result.isEmpty());
        verify(propertySchema, never()).validate(any(), any(), any(), anyString());
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
        // Re‑create validator so that its internal map contains both entries
        validator = new PropertiesValidator("test-schema", schemaNode, parentSchema, validationContext);
        // stub the child schema validation for "presentProp"
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenAnswer(invocation -> {
            String at = invocation.getArgument(3);
            if (at.endsWith("presentProp")) {
                ValidationMessage vm = mock(ValidationMessage.class);
                when(vm.getMessage()).thenReturn("presentError");
                return new HashSet<>(Collections.singleton(vm));
            }
            return Collections.emptySet();
        });
        // Required validator for the missing property
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        ValidationMessage requiredMsg = mock(ValidationMessage.class);
        when(requiredMsg.getMessage()).thenReturn("'missingProp' is required");
        Set<ValidationMessage> requiredErrors = new HashSet<>(Collections.singleton(requiredMsg));
        when(requiredValidator.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(requiredErrors);
        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);
        // Data node contains only the present property
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("presentProp", "value");
        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size(), "Both the child error and the required error should be present");
        assertTrue(result.stream().anyMatch(vm -> "presentError".equals(vm.getMessage())));
        assertTrue(result.stream().anyMatch(vm -> "'missingProp' is required".equals(vm.getMessage())));
        // Evaluated properties should contain only the present one
        Set<String> evaluated = collectorContext.getEvaluatedProperties();
        assertEquals(1, evaluated.size());
        assertTrue(evaluated.contains("#/presentProp"));
    }

    /* --------------------------------------------------------------------- *
     *  Property present – non‑complex validator, matchedNode unchanged.    *
     * --------------------------------------------------------------------- */
    @Test
    void testValidatePropertyPresentNonComplexMatchedNodeUnchanged() {
        // Arrange
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("simple", "value");
        when(validatorState.isComplexValidator()).thenReturn(false);
        when(propertySchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, dataNode, dataNode, "#");
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        // matchedNode should never be toggled for a non‑complex validator
        verify(validatorState, never()).setMatchedNode(anyBoolean());
    }

    /* --------------------------------------------------------------------- *
     *  Null ExecutionContext – expect NullPointerException.               *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullExecutionContext() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("any", "value");
        assertThrows(NullPointerException.class, () -> validator.validate(null, dataNode, dataNode, "#"));
    }

    /* --------------------------------------------------------------------- *
     *  Null node argument – expect NullPointerException.                  *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullNode() {
        assertThrows(NullPointerException.class, () -> validator.validate(executionContext, null, MAPPER.createObjectNode(), "#"));
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
        assertThrows(NullPointerException.class, () -> validator.validate(executionContext, dataNode, dataNode, "#"));
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
        assertThrows(NullPointerException.class, () -> validator.validate(executionContext, dataNode, dataNode, "#"));
    }

    /* --------------------------------------------------------------------- *
     *  Null "at" path – expect NullPointerException inside atPath.       *
     * --------------------------------------------------------------------- */
    @Test
    void testValidateNullAtPath() {
        ObjectNode dataNode = MAPPER.createObjectNode();
        dataNode.put("name", "value");
        assertThrows(NullPointerException.class, () -> validator.validate(executionContext, dataNode, dataNode, null));
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
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, propertySchema.getCallCount(), "Child schema must not be called when walk is enabled");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:13: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:14: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:55: error: interface expected here
    private static final class SimpleValidationMessage implements ValidationMessage {
                                                                  ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:102: error: interface expected here
    private static final class StubJsonSchema implements JsonSchema {
                                                         ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:257: error: interface expected here
    private static final class StubRequiredValidator implements RequiredValidator {
                                                                ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:274: error: interface expected here
    private static final class StubValidatorState implements ValidatorState {
                                                             ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:328: error: interface expected here
    private static final class StubCollectorContext implements CollectorContext {
                                                               ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:435: error: interface expected here
    private static final class StubExecutionContext implements ExecutionContext {
                                                               ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:561: error: interface expected here
    private static final class StubValidationContext implements ValidationContext {
                                                                ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:691: error: interface expected here
    private static final class StubParentSchema implements JsonSchema {
                                                           ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:17: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:28: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:31: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:35: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:38: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:41: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:44: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_validate_Test
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:63: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:68: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:73: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:78: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:83: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:88: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:114: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:122: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:127: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:133: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:138: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:143: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:148: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:153: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:158: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:163: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:168: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:172: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:176: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:180: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:184: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:188: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:192: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:196: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:200: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:204: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:208: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:212: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:216: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:220: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:224: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:228: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:232: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:236: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:240: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:265: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:290: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:296: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:302: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:308: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:314: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:334: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:339: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:346: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:352: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:357: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:362: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:366: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:370: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:374: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:378: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:382: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:386: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:390: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:394: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:398: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:402: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:406: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:411: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:416: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:421: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:426: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:443: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:449: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:454: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:458: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:463: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:467: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:472: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:476: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:481: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:485: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:490: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:494: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:499: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:503: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:508: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:512: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:517: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:521: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:526: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:530: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_validate_Test.java:535: error: method does not override or implement a method from a supertype
        @Override
        ^
100 errors
only showing the first 100 errors, of 273 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.PropertiesValidator`:\n\tdependent fields:\n\t\tjava.util.Map<java.lang.String, com.networknt.schema.JsonSchema> com.networknt.schema.PropertiesValidator.schemas;\n\t\torg.slf4j.Logger com.networknt.schema.PropertiesValidator.logger;\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\n\t\tmethod `private void walkSchema(ExecutionContext, Map.Entry<String, JsonSchema>, JsonNode, JsonNode, String, boolean, Set<ValidationMessage>, WalkListenerRunner)` returns `void`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.ValidatorState`:\n\tdependent fields:\n\t\tjava.lang.String com.networknt.schema.ValidatorState.VALIDATOR_STATE_KEY;\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `private static void resetValidatorState(CollectorContext)` returns `void`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.PatternPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.AdditionalPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.AnyOfValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


