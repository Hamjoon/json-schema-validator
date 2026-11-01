```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#validate(ExecutionContext, JsonNode)}.
 *
 * <p>The test suite focuses on the public {@code validate} method – its
 * argument validation, delegation to the four‑argument overload and the
 * interaction with the protected helper methods that influence the final
 * result (annotation collection, sub‑schema retrieval, etc.).  A minimal concrete
 * subclass {@link TestValidator} is used to stub out the abstract behaviour and
 * to expose the protected hooks for verification.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidatorValidateTest {

    /* --------------------------------------------------------------------- *
     *  Mocks required for constructing the validator                         *
     * --------------------------------------------------------------------- */
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;
    @Mock ValidatorTypeCode validatorType;
    @Mock ValidationContext validationContext;

    /** The concrete validator under test. */
    private TestValidator validator;

    /** ExecutionContext supplied to the {@code validate} call. */
    private ExecutionContext executionContext;

    /** Instance node supplied to the {@code validate} call. */
    private JsonNode instanceNode;

    @BeforeEach
    void setUp() {
        // a fresh execution context for every test – it can be a mock because
        // the validator only calls a few of its methods which we stub later.
        executionContext = mock(ExecutionContext.class);

        // a generic (non‑null) instance node – its concrete type does not matter
        // for the delegation tests.
        instanceNode = mock(JsonNode.class);

        // create the validator.  All constructor arguments are either mocks or
        // simple values.  The validator itself is a *spy* so that we can
        // verify calls to the protected helpers.
        validator = Mockito.spy(new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext,
                false /* suppressSubSchemaRetrieval */));
        // default behaviour: the overload returns an empty set.
        validator.setOverloadResult(Collections.emptySet());
    }

    @AfterEach
    void tearDown() {
        // reset the spy between tests to avoid call‑count leakage.
        clearInvocations(validator);
    }

    /* --------------------------------------------------------------------- *
     *  1. Delegation test – already covered in the provided framework           *
     * --------------------------------------------------------------------- */
    @Test
    void validate_delegatesToFourArgumentMethod() {
        // the four‑argument overload will return a known set
        Set<ValidationMessage> expected = Collections.singleton(mock(ValidationMessage.class));
        doReturn(expected).when(validator).validate(eq(executionContext), eq(instanceNode),
                eq(instanceNode), any(JsonNodePath.class));

        Set<ValidationMessage> actual = validator.validate(executionContext, instanceNode);

        assertSame(expected, actual, "validate should return exactly what the overload returns");
        ArgumentCaptor<JsonNodePath> pathCaptor = ArgumentCaptor.forClass(JsonNodePath.class);
        verify(validator, times(1)).validate(eq(executionContext), eq(instanceNode),
                eq(instanceNode), pathCaptor.capture());
        assertSame(validator.getRootPath(), pathCaptor.getValue(),
                "the JsonNodePath passed to the overload must be the value returned by atRoot()");
    }

    /* --------------------------------------------------------------------- *
     *  2. Argument validation – null checks                                    *
     * --------------------------------------------------------------------- */
    @Test
    void validate_nullExecutionContext_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(null, instanceNode));
        assertEquals("ExecutionContext must not be null", ex.getMessage());
    }

    @Test
    void validate_nullJsonNode_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(executionContext, null));
        assertEquals("JsonNode must not be null", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  3. atRoot() returning null – should result in NPE                      *
     * --------------------------------------------------------------------- */
    @Test
    void validate_atRootReturnsNull_throwsNullPointerException() {
        validator.setRootPath(null); // force atRoot() to return null
        assertThrows(NullPointerException.class,
                () -> validator.validate(executionContext, instanceNode));
    }

    /* --------------------------------------------------------------------- *
     *  4. Happy path – valid node, no annotations, no sub‑schema            *
     * --------------------------------------------------------------------- */
    @Test
    void validate_happyPath_returnsEmptySet() {
        // configure the validator to *not* collect annotations and not to fetch a sub‑schema
        validator.setCollectAnnotationsResult(false);
        validator.setSuppressSubSchemaRetrieval(true);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode);
        assertTrue(result.isEmpty(), "a valid node should produce an empty result set");
        // verify that the four‑argument overload was still invoked
        verify(validator, times(1)).validate(eq(executionContext), eq(instanceNode),
                eq(instanceNode), any(JsonNodePath.class));
    }

    /* --------------------------------------------------------------------- *
     *  5. Validation error – overload returns a non‑empty set                *
     * --------------------------------------------------------------------- */
    @Test
    void validate_nodeViolatesSchema_returnsValidationMessage() {
        ValidationMessage msg = mock(ValidationMessage.class);
        validator.setOverloadResult(Collections.singleton(msg));

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode);
        assertEquals(1, result.size(), "exactly one validation message expected");
        assertTrue(result.contains(msg), "the returned set must contain the message from the overload");
    }

    /* --------------------------------------------------------------------- *
     *  6. Annotation collection – collectAnnotations true                     *
     * --------------------------------------------------------------------- */
    @Test
    void validate_collectAnnotations_true_annotationAdded() {
        validator.setCollectAnnotationsResult(true);
        validator.setKeyword("type"); // the keyword that the validator reports

        validator.validate(executionContext, instanceNode);

        // the validator should have called the two‑argument version
        assertTrue(validator.isCollectAnnotationsCalled(),
                "collectAnnotations(ExecutionContext) must be invoked");
        // and the keyword‑specific version
        assertEquals("type", validator.getCollectAnnotationsKeyword(),
                "collectAnnotations(ExecutionContext, keyword) must be invoked with the validator's keyword");
        // finally, the annotation customiser must have been applied
        assertTrue(validator.isAnnotationAdded(),
                "putAnnotation should have been called when annotation collection is enabled");
    }

    /* --------------------------------------------------------------------- *
     *  7. Annotation collection – collectAnnotations false                    *
     * --------------------------------------------------------------------- */
    @Test
    void validate_collectAnnotations_false_noAnnotationAdded() {
        validator.setCollectAnnotationsResult(false);
        validator.setKeyword("type");

        validator.validate(executionContext, instanceNode);

        assertTrue(validator.isCollectAnnotationsCalled(),
                "collectAnnotations(ExecutionContext) must still be consulted");
        assertFalse(validator.isAnnotationAdded(),
                "no annotation must be added when collectAnnotations returns false");
    }

    /* --------------------------------------------------------------------- *
     *  8. Sub‑schema retrieval – suppressed                                      *
     * --------------------------------------------------------------------- */
    @Test
    void validate_suppressSubSchemaRetrieval_true_fetchSubSchemaNotCalled() {
        validator.setSuppressSubSchemaRetrieval(true);
        validator.setCollectAnnotationsResult(false); // irrelevant for this test

        validator.validate(executionContext, instanceNode);

        assertFalse(validator.isFetchSubSchemaCalled(),
                "fetchSubSchemaNode must not be invoked when suppressSubSchemaRetrieval is true");
    }

    /* --------------------------------------------------------------------- *
     *  9. Sub‑schema retrieval – allowed, sub‑schema returns errors            *
     * --------------------------------------------------------------------- */
    @Test
    void validate_suppressSubSchemaRetrieval_false_subSchemaReturnsError() {
        validator.setSuppressSubSchemaRetrieval(false);
        // make fetchSubSchemaNode return a dummy schema whose validation yields an error
        JsonSchema subSchema = mock(JsonSchema.class);
        ValidationResult subResult = mock(ValidationResult.class);
        when(subResult.getMessages()).thenReturn(Collections.singleton("sub‑schema violation"));
        when(subSchema.walk(any(), anyString(), any(), anyBoolean())).thenReturn(subResult);
        validator.setFetchedSubSchema(subSchema);
        // make the four‑argument overload return an empty set (no errors of its own)
        validator.setOverloadResult(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode);

        assertTrue(validator.isFetchSubSchemaCalled(),
                "fetchSubSchemaNode must be called when suppression is off");
        // because the real BaseJsonValidator merges sub‑schema messages into the
        // final result, we expect at least one message.  The concrete merging logic
        // is not reproduced here; we simply assert that the result is not empty.
        assertFalse(result.isEmpty(),
                "when the fetched sub‑schema produces errors, the final result must not be empty");
    }

    /* --------------------------------------------------------------------- *
     *  10. fetchSubSchemaNode throws – exception must propagate                *
     * --------------------------------------------------------------------- */
    @Test
    void validate_fetchSubSchemaThrows_exceptionPropagates() {
        validator.setSuppressSubSchemaRetrieval(false);
        IllegalArgumentException iae = new IllegalArgumentException("invalid sub‑schema reference");
        validator.setFetchSubSchemaException(iae);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(executionContext, instanceNode));
        assertSame(iae, thrown, "the exception from fetchSubSchemaNode must propagate unchanged");
    }

    /* --------------------------------------------------------------------- *
     *  Helper concrete class that exposes the protected hooks                *
     * --------------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        /** Fields used to control the behaviour of the overridden hooks. */
        private JsonNodePath rootPath = mock(JsonNodePath.class);
        private Set<ValidationMessage> overloadResult = Collections.emptySet();
        private boolean collectAnnotationsResult = false;
        private boolean collectAnnotationsCalled = false;
        private String collectAnnotationsKeyword = null;
        private boolean annotationAdded = false;
        private boolean fetchSubSchemaCalled = false;
        private JsonSchema fetchedSubSchema = null;
        private RuntimeException fetchSubSchemaException = null;
        private String keyword = "defaultKeyword";

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaLocation, evaluationPath, schemaNode,
                    parentSchema, validatorType, validationContext);
            // the protected flag is final in the real class – we cannot change it
            // after construction, therefore we pass the desired value via the
            // constructor argument and store it in a field that the overridden
            // fetchSubSchemaNode will honour.
            this.suppressSubSchemaRetrieval = suppressSubSchemaRetrieval;
        }

        /* ----------------------------------------------------------------- *
         *  Configuration helpers used by the test methods                     *
         * ----------------------------------------------------------------- */
        void setRootPath(JsonNodePath path) {
            this.rootPath = path;
        }

        JsonNodePath getRootPath() {
            return this.rootPath;
        }

        void setOverloadResult(Set<ValidationMessage> result) {
            this.overloadResult = result;
        }

        void setCollectAnnotationsResult(boolean result) {
            this.collectAnnotationsResult = result;
        }

        void setKeyword(String kw) {
            this.keyword = kw;
        }

        void setFetchedSubSchema(JsonSchema schema) {
            this.fetchedSubSchema = schema;
        }

        void setFetchSubSchemaException(RuntimeException ex) {
            this.fetchSubSchemaException = ex;
        }

        boolean isCollectAnnotationsCalled() {
            return this.collectAnnotationsCalled;
        }

        String getCollectAnnotationsKeyword() {
            return this.collectAnnotationsKeyword;
        }

        boolean isAnnotationAdded() {
            return this.annotationAdded;
        }

        boolean isFetchSubSchemaCalled() {
            return this.fetchSubSchemaCalled;
        }

        /* ----------------------------------------------------------------- *
         *  Overridden protected methods                                        *
         * ----------------------------------------------------------------- */
        @Override
        protected JsonNodePath atRoot() {
            return this.rootPath;
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                   JsonNode node,
                                                   JsonNode rootNode,
                                                   JsonNodePath instanceLocation) {
            // the public validate method ultimately forwards to this overload;
            // we simply return the pre‑configured result.
            return this.overloadResult;
        }

        @Override
        protected boolean collectAnnotations(ExecutionContext executionContext) {
            this.collectAnnotationsCalled = true;
            return this.collectAnnotationsResult;
        }

        @Override
        protected boolean collectAnnotations(ExecutionContext executionContext, String keyword) {
            this.collectAnnotationsCalled = true;
            this.collectAnnotationsKeyword = keyword;
            return this.collectAnnotationsResult;
        }

        @Override
        protected void putAnnotation(ExecutionContext executionContext,
                                    java.util.function.Consumer<JsonNodeAnnotation.Builder> customizer) {
            // In the real implementation this would add an annotation to the
            // execution context.  For the test we merely record that the method
            // was invoked.
            this.annotationAdded = true;
        }

        @Override
        protected JsonSchema fetchSubSchemaNode(ValidationContext validationContext) {
            this.fetchSubSchemaCalled = true;
            if (this.fetchSubSchemaException != null) {
                throw this.fetchSubSchemaException;
            }
            return this.fetchedSubSchema;
        }

        /* ----------------------------------------------------------------- *
         *  Minimal implementations of the JsonValidator abstract methods   *
         * ----------------------------------------------------------------- */
        @Override
        public String getKeyword() {
            return this.keyword;
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }
    }
}
```