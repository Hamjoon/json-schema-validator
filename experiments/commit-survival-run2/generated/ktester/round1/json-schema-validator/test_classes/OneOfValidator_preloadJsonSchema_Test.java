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
     * Minimal stub implementation of {@link JsonSchema}.  All abstract methods
     * throw {@link UnsupportedOperationException} except {@code initializeValidators()},
     * which can be overridden by subclasses.
     */
    static abstract class BaseStubJsonSchema extends JsonSchema {

        BaseStubJsonSchema() {
            // JsonSchema has a protected constructor that expects a ValidationContext,
            // a schema node and a parent schema.  For the purpose of these tests we can
            // safely pass {@code null} for all three arguments.
            super(null, null, null);
        }

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
    }

    /**
     * Minimal concrete {@link JsonSchema} that throws {@link UnsupportedOperationException}
     * for all methods except {@link #initializeValidators()} which can be customised.
     */
    static class TestJsonSchema extends JsonSchema {

        private int initCalls = 0;

        private RuntimeException runtimeExceptionToThrow;

        private AssertionError assertionErrorToThrow;

        private boolean throwNullPointer = false;

        private boolean modifyDuringInit = false;

        private List<JsonSchema> externalList;

        TestJsonSchema() {
            super(null, null, null);
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

        /* --------------------------------------------------------------------- */
        /* The remaining methods are required only because JsonSchema is an    */
        /* abstract class. They are not used by the tests, so they simply throw */
        /* UnsupportedOperationException.                                        */
        /* --------------------------------------------------------------------- */
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
    }

    /**
     * Helper that creates an {@link OneOfValidator} from a JSON array string.
     *
     * @param jsonArray the JSON text representing an array of subschemas
     */
    private OneOfValidator createValidator(String jsonArray) throws IOException {
        JsonNode schemaNode = MAPPER.readTree(jsonArray);
        // ValidationContext is not required for the tests that manipulate the
        // internal 'schemas' list directly.
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
