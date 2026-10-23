
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
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import java.util.List;
import java.util.ConcurrentModificationException;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_preloadJsonSchema_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    ValidationContext // mock for the ValidationContext dependency
    validationContext;

    @Mock
    JsonSchema // parent schema can be null or a mock – not used in the test
    parentSchema;

    @Mock
    JsonSchema // first child schema returned by ValidationContext.newSchema
    childSchema1;

    @Mock
    JsonSchema // second child schema returned by ValidationContext.newSchema
    childSchema2;

    // instance under test
    OneOfValidator validator;

    /**
     * Very small stub implementation of {@link JsonSchema}.  Only the
     * {@code initializeValidators()} method is required for the test.
     */
    static class StubJsonSchema implements JsonSchema {

        @Override
        public void initializeValidators() {
            // No‑op – the real validator would initialise its child validators.
        }
        // All other methods of the JsonSchema interface are left with the
        // default behaviour (throwing {@code UnsupportedOperationException})
        // because they are not needed for this particular test.
    }

    /**
     * Stub {@link ValidationContext} that always returns a new {@link StubJsonSchema}
     * instance for any call to {@code newSchema(...)}.
     */
    static class StubValidationContext extends ValidationContext {

        @Override
        public JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
            return new StubJsonSchema();
        }
    }

    /**
     * Simple stub implementation of {@link JsonSchema} that records calls to
     * {@code initializeValidators()} and can be configured to throw an
     * exception when that method is invoked.
     */
    static class TestJsonSchema implements JsonSchema {

        private int initCalls = 0;

        private RuntimeException runtimeExceptionToThrow;

        private AssertionError assertionErrorToThrow;

        private boolean throwNullPointer = false;

        private boolean modifyDuringInit = false;

        // used for concurrent modification test
        private List<JsonSchema> externalList;

        TestJsonSchema() {
        }

        void setRuntimeExceptionToThrow(RuntimeException ex) {
            this.runtimeExceptionToThrow = ex;
        }

        void setAssertionErrorToThrow(AssertionError err) {
            this.assertionErrorToThrow = err;
        }

        void setThrowNullPointer(boolean flag) {
            this.throwNullPointer = flag;
        }

        void setModifyDuringInit(boolean flag, List<JsonSchema> list) {
            this.modifyDuringInit = flag;
            this.externalList = list;
        }

        int getInitCalls() {
            return initCalls;
        }

        @Override
        public void initializeValidators() {
            if (throwNullPointer) {
                throw new NullPointerException();
            }
            if (runtimeExceptionToThrow != null) {
                throw runtimeExceptionToThrow;
            }
            if (assertionErrorToThrow != null) {
                throw assertionErrorToThrow;
            }
            if (modifyDuringInit && externalList != null) {
                externalList.add(new TestJsonSchema());
            }
            initCalls++;
        }

        // -----------------------------------------------------------------
        // The remaining methods of the JsonSchema interface are not needed for
        // these tests and therefore simply throw UnsupportedOperationException.
        // -----------------------------------------------------------------
        @Override
        public void validate(JsonNode node, JsonNode rootNode, String at) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonNode getSchemaNode() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setParentSchema(JsonSchema parent) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchema getParentSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPath(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getPath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setValidator(Validator validator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Validator getValidator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchema(JsonNode schema) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonNode getSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaPath(String schemaPath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSchemaPath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaLocation(String schemaLocation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSchemaLocation() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaVersion(SpecVersion.VersionFlag version) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SpecVersion.VersionFlag getSchemaVersion() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setValidatorType(ValidatorTypeCode type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ValidatorTypeCode getValidatorType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaType(SchemaType type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SchemaType getSchemaType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setReference(String ref) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getReference() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setReferenceSchema(JsonSchema referenceSchema) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonSchema getReferenceSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public JsonNode getSchemaNode(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst, boolean isEnum) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst, boolean isEnum) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst, boolean isEnum, boolean isUniqueItems) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst, boolean isEnum, boolean isUniqueItems, boolean isMaximum) {
            throw new UnsupportedOperationException();
        }
        // ... many more methods omitted for brevity
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mocks before each test to avoid cross‑test interference
        reset(validationContext, childSchema1, childSchema2);
    }

    /**
     * Helper that creates an {@link OneOfValidator} from a JSON array string.
     *
     * @param jsonArray the JSON text representing an array of subschemas
     */
    private OneOfValidator createValidator(String jsonArray) throws IOException {
        JsonNode schemaNode = MAPPER.readTree(jsonArray);
        // Pass {@code null} for ValidationContext because we will not rely on it
        return new OneOfValidator("testPath", schemaNode, null, null);
    }

    /**
     * Utility to replace the private {@code schemas} field via reflection.
     *
     * @param validator the validator instance whose field we want to set
     * @param list      the list (or null) to assign to the field
     */
    private void setSchemasField(OneOfValidator validator, List<JsonSchema> list) throws Exception {
        Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, list);
    }

    @Test
    void preloadJsonSchema_callsInitializeValidatorsOnAllChildSchemas() throws Exception {
        validator = createValidator("[{}, {}]");
        List<JsonSchema> list = new ArrayList<>();
        TestJsonSchema child1 = new TestJsonSchema();
        TestJsonSchema child2 = new TestJsonSchema();
        list.add(child1);
        list.add(child2);
        setSchemasField(validator, list);
        validator.preloadJsonSchema();
        assertEquals(1, child1.getInitCalls());
        assertEquals(1, child2.getInitCalls());
    }

    @Test
    void preloadJsonSchema_withEmptySchemaArray_doesNotInvokeInitializeValidators() throws Exception {
        validator = createValidator("[]");
        // schemas list will be empty after construction
        validator.preloadJsonSchema();
        // nothing to assert – just ensure no exception is thrown
    }

    @Test
    void preloadJsonSchema_emptySchemasList_noInteraction() throws Exception {
        validator = createValidator("[]");
        setSchemasField(validator, new ArrayList<>());
        validator.preloadJsonSchema();
        // No exception means success
    }

    @Test
    void preloadJsonSchema_singleSchema_initializeOnce() throws Exception {
        validator = createValidator("[{}]");
        TestJsonSchema child = new TestJsonSchema();
        List<JsonSchema> list = new ArrayList<>();
        list.add(child);
        setSchemasField(validator, list);
        validator.preloadJsonSchema();
        assertEquals(1, child.getInitCalls());
    }

    @Test
    void preloadJsonSchema_multipleSchemas_initializeOncePerSchema() throws Exception {
        validator = createValidator("[{}, {}, {}]");
        TestJsonSchema child1 = new TestJsonSchema();
        TestJsonSchema child2 = new TestJsonSchema();
        List<JsonSchema> list = new ArrayList<>();
        list.add(child1);
        list.add(child2);
        // child1 appears twice
        list.add(child1);
        setSchemasField(validator, list);
        validator.preloadJsonSchema();
        assertEquals(2, child1.getInitCalls());
        assertEquals(1, child2.getInitCalls());
    }

    @Test
    void preloadJsonSchema_exceptionPropagation_runtimeException() throws Exception {
        validator = createValidator("[{}]");
        TestJsonSchema child = new TestJsonSchema();
        child.setRuntimeExceptionToThrow(new RuntimeException("boom"));
        List<JsonSchema> list = new ArrayList<>();
        list.add(child);
        setSchemasField(validator, list);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.preloadJsonSchema());
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void preloadJsonSchema_duplicateSchemaInstances_callsTwice() throws Exception {
        validator = createValidator("[{}]");
        TestJsonSchema child = new TestJsonSchema();
        List<JsonSchema> list = new ArrayList<>();
        list.add(child);
        // duplicate entry
        list.add(child);
        setSchemasField(validator, list);
        validator.preloadJsonSchema();
        assertEquals(2, child.getInitCalls());
    }

    @Test
    void preloadJsonSchema_nullSchemaElement_throwsNullPointerException() throws Exception {
        validator = createValidator("[{}]");
        TestJsonSchema child = new TestJsonSchema();
        List<JsonSchema> list = new ArrayList<>();
        list.add(child);
        // null entry should cause NPE
        list.add(null);
        list.add(child);
        setSchemasField(validator, list);
        assertThrows(NullPointerException.class, () -> validator.preloadJsonSchema());
    }

    @Test
    void preloadJsonSchema_concurrentModificationDuringIteration_throwsException() throws Exception {
        validator = createValidator("[{}]");
        TestJsonSchema child = new TestJsonSchema();
        List<JsonSchema> list = new ArrayList<>();
        list.add(child);
        // configure child to modify the list during initialise
        child.setModifyDuringInit(true, list);
        setSchemasField(validator, list);
        assertThrows(ConcurrentModificationException.class, () -> validator.preloadJsonSchema());
    }

    @Test
    void preloadJsonSchema_idempotentCall_callsInitializeEachTime() throws Exception {
        validator = createValidator("[{}, {}]");
        TestJsonSchema child1 = new TestJsonSchema();
        TestJsonSchema child2 = new TestJsonSchema();
        List<JsonSchema> list = new ArrayList<>();
        list.add(child1);
        list.add(child2);
        setSchemasField(validator, list);
        // first call
        validator.preloadJsonSchema();
        // second call
        validator.preloadJsonSchema();
        assertEquals(2, child1.getInitCalls());
        assertEquals(2, child2.getInitCalls());
    }

    @Test
    void preloadJsonSchema_nullInternalSchemasList_throwsNullPointerException() throws Exception {
        validator = createValidator("[]");
        setSchemasField(validator, null);
        assertThrows(NullPointerException.class, () -> validator.preloadJsonSchema());
    }

    @Test
    void preloadJsonSchema_errorPropagation_assertionError() throws Exception {
        validator = createValidator("[{}]");
        TestJsonSchema child = new TestJsonSchema();
        child.setAssertionErrorToThrow(new AssertionError("assertion failed"));
        List<JsonSchema> list = new ArrayList<>();
        list.add(child);
        setSchemasField(validator, list);
        AssertionError err = assertThrows(AssertionError.class, () -> validator.preloadJsonSchema());
        assertEquals("assertion failed", err.getMessage());
    }

    @Test
    void preloadJsonSchema_executesWithoutException() throws Exception {
        // Using real StubValidationContext that returns simple schemas
        ValidationContext validationContext = new ValidationContext(null, null, null, null, null) {

            @Override
            public JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
                return new TestJsonSchema();
            }
        };
        JsonNode schemaNode = MAPPER.readTree("[{}, {}]");
        OneOfValidator realValidator = new OneOfValidator("testPath", schemaNode, null, validationContext);
        assertDoesNotThrow(realValidator::preloadJsonSchema);
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:13: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:14: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:45: error: interface expected here
    static class StubJsonSchema implements JsonSchema {
                                           ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:73: error: interface expected here
    static class TestJsonSchema implements JsonSchema {
                                           ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:164: error: cannot find symbol
        public void setValidator(Validator validator) {
                                 ^
  symbol:   class Validator
  location: class TestJsonSchema
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:169: error: cannot find symbol
        public Validator getValidator() {
               ^
  symbol:   class Validator
  location: class TestJsonSchema
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:224: error: cannot find symbol
        public void setSchemaType(SchemaType type) {
                                  ^
  symbol:   class SchemaType
  location: class TestJsonSchema
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:229: error: cannot find symbol
        public SchemaType getSchemaType() {
               ^
  symbol:   class SchemaType
  location: class TestJsonSchema
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:369: error: method setSchemaNode(JsonNode,String) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:374: error: method setSchemaNode(JsonNode) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:379: error: method setSchemaNode(JsonNode,String,JsonNode,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:384: error: method setSchemaNode(JsonNode,String,JsonNode) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:389: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:394: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:399: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:404: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:409: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:414: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:419: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:424: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:429: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:434: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:439: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:444: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:449: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:454: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:459: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:464: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:469: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:474: error: method setSchemaNode(JsonNode,String,JsonNode,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean,boolean) is already defined in class TestJsonSchema
        public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst, boolean isEnum) {
                    ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:17: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:22: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class OneOfValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:26: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class OneOfValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class OneOfValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:34: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class OneOfValidator_preloadJsonSchema_Test
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:47: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:60: error: constructor ValidationContext in class ValidationContext cannot be applied to given types;
    static class StubValidationContext extends ValidationContext {
           ^
  required: URIFactory,URNFactory,JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:64: error: incompatible types: StubJsonSchema cannot be converted to JsonSchema
            return new StubJsonSchema();
                   ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:112: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:124: error: incompatible types: TestJsonSchema cannot be converted to JsonSchema
                externalList.add(new TestJsonSchema());
                                 ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:133: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:138: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:143: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:148: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:153: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:158: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:163: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:168: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:173: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:178: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:183: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:188: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:193: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:198: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:203: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:208: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:213: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:218: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:223: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:228: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:233: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:238: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:243: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:248: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:253: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:258: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:263: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:268: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:273: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:278: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:283: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:288: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:293: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:298: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:303: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:308: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:313: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:318: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:323: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:328: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:333: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:338: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:343: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:348: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:353: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:358: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:363: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:368: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:373: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:378: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:383: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:388: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:393: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:398: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:403: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:408: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:413: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:418: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:423: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/OneOfValidator_preloadJsonSchema_Test.java:428: error: method does not override or implement a method from a supertype
        @Override
        ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
100 errors
only showing the first 100 errors, of 128 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'dependent classes': 'class `com.networknt.schema.OneOfValidator`:\n\tdependent fields:\n\t\tjava.util.List<com.networknt.schema.JsonSchema> com.networknt.schema.OneOfValidator.schemas;\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.OneOfValidator.preloadJsonSchema()`;\nclass `com.networknt.schema.IfValidator`:\n\trelated functions:\n\t\tmethod `public void preloadJsonSchema()` returns `void`, related with `com.networknt.schema.OneOfValidator.preloadJsonSchema()`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `public void preloadJsonSchema()` returns `void`, related with `com.networknt.schema.OneOfValidator.preloadJsonSchema()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


