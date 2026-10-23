```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OneOfValidator#preloadJsonSchema()}.
 *
 * <p>These tests avoid external libraries such as Mockito by using simple
 * hand‑crafted stub implementations of {@link JsonSchema} and
 * {@link ValidationContext}.  The private {@code schemas} field of
 * {@link OneOfValidator} is manipulated via reflection where necessary.</p>
 */
class OneOfValidator_preloadJsonSchema_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Instance under test. */
    private OneOfValidator validator;

    /** Simple stub implementation of {@link JsonSchema} that records calls to
     * {@code initializeValidators()} and can be configured to throw an
     * exception when that method is invoked. */
    static class TestJsonSchema implements JsonSchema {
        private int initCalls = 0;
        private RuntimeException runtimeExceptionToThrow;
        private AssertionError assertionErrorToThrow;
        private boolean throwNullPointer = false;
        private boolean modifyDuringInit = false;
        private List<JsonSchema> externalList; // used for concurrent modification test

        TestJsonSchema() {}

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
        @Override public void validate(JsonNode node, JsonNode rootNode, String at) { throw new UnsupportedOperationException(); }
        @Override public JsonNode getSchemaNode() { throw new UnsupportedOperationException(); }
        @Override public void setParentSchema(JsonSchema parent) { throw new UnsupportedOperationException(); }
        @Override public JsonSchema getParentSchema() { throw new UnsupportedOperationException(); }
        @Override public void setPath(String path) { throw new UnsupportedOperationException(); }
        @Override public String getPath() { throw new UnsupportedOperationException(); }
        @Override public void setValidator(Validator validator) { throw new UnsupportedOperationException(); }
        @Override public Validator getValidator() { throw new UnsupportedOperationException(); }
        @Override public void setSchema(JsonNode schema) { throw new UnsupportedOperationException(); }
        @Override public JsonNode getSchema() { throw new UnsupportedOperationException(); }
        @Override public void setSchemaPath(String schemaPath) { throw new UnsupportedOperationException(); }
        @Override public String getSchemaPath() { throw new UnsupportedOperationException(); }
        @Override public void setSchemaLocation(String schemaLocation) { throw new UnsupportedOperationException(); }
        @Override public String getSchemaLocation() { throw new UnsupportedOperationException(); }
        @Override public void setSchemaVersion(SpecVersion.VersionFlag version) { throw new UnsupportedOperationException(); }
        @Override public SpecVersion.VersionFlag getSchemaVersion() { throw new UnsupportedOperationException(); }
        @Override public void setValidatorType(ValidatorTypeCode type) { throw new UnsupportedOperationException(); }
        @Override public ValidatorTypeCode getValidatorType() { throw new UnsupportedOperationException(); }
        @Override public void setSchemaType(SchemaType type) { throw new UnsupportedOperationException(); }
        @Override public SchemaType getSchemaType() { throw new UnsupportedOperationException(); }
        @Override public void setReference(String ref) { throw new UnsupportedOperationException(); }
        @Override public String getReference() { throw new UnsupportedOperationException(); }
        @Override public void setReferenceSchema(JsonSchema referenceSchema) { throw new UnsupportedOperationException(); }
        @Override public JsonSchema getReferenceSchema() { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode) { throw new UnsupportedOperationException(); }
        @Override public JsonNode getSchemaNode(String path) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst, boolean isEnum) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst, boolean isEnum) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst, boolean isEnum, boolean isUniqueItems) { throw new UnsupportedOperationException(); }
        @Override public void setSchemaNode(JsonNode schemaNode, String path, JsonNode rootNode, boolean isRoot, boolean isRef, boolean isRecursive, boolean isCombined, boolean isAllOf, boolean isAnyOf, boolean isOneOf, boolean isNot, boolean isIf, boolean isThen, boolean isElse, boolean isDependencies, boolean isPatternProperties, boolean isAdditionalProperties, boolean isAdditionalItems, boolean isPropertyNames, boolean isContains, boolean isConst, boolean isEnum, boolean isUniqueItems, boolean isMaximum) { throw new UnsupportedOperationException(); }
        // ... many more methods omitted for brevity
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

    /**
     * Helper that creates a {@link OneOfValidator} from a JSON array string.
     *
     * @param jsonArray the JSON text representing an array of subschemas
     */
    private OneOfValidator createValidator(String jsonArray) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(jsonArray);
        // Pass {@code null} for ValidationContext because we will not rely on it
        return new OneOfValidator("testPath", schemaNode, null, null);
    }

    @BeforeEach
    void reset() {
        // No Mockito to reset.
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
        list.add(child1); // child1 appears twice
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
        list.add(child); // duplicate entry
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
        list.add(null); // null entry should cause NPE
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