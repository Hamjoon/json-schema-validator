###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link BaseJsonValidator}.  Because {@code BaseJsonValidator} is abstract we
 * provide a minimal concrete implementation ( {@code TestValidator} ) that simply returns an
 * empty {@link java.util.Set} from the required {@code validate(...)} method.
 *
 * The tests cover:
 * <ul>
 *   <li>Public getters – {@code getSchemaLocation()}, {@code getEvaluationPath()},
 *       {@code getKeyword()}, {@code getSchemaNode()}, {@code getParentSchema()},
 *       {@code toString()}.</li>
 *   <li>Utility methods – {@code equals(double,double)}, {@code atRoot()},
 *       {@code getNodeFieldType()}, {@code preloadJsonSchemas()}, {@code fetchSubSchemaNode()}.</li>
 *   <li>Behaviour of the overloaded {@code validate(...)} methods that delegate to the
 *       core {@code validate(...)} implementation.</li>
 *   <li>Edge‑case handling such as {@code fetchSubSchemaNode()} when sub‑schema retrieval is
 *       suppressed or when the schema node does not contain an {@code id} field.</li>
 * </ul>
 */
public class BaseJsonValidatorTest {

    /** Simple concrete validator used for the tests. */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ErrorMessageType errorMessageType,
                      Keyword keyword,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                    errorMessageType, keyword, validationContext, suppressSubSchemaRetrieval);
        }

        /** The abstract method from {@link JsonValidator}.  Returns an empty set – the validator does no work. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath instanceLocation) {
            return Collections.emptySet();
        }
    }

    /** Minimal {@link ErrorMessageType} implementation for test construction. */
    private static class SimpleErrorMessageType implements ErrorMessageType {
        private final String code;

        SimpleErrorMessageType(String code) {
            this.code = code;
        }

        @Override
        public String getErrorCode() {
            return code;
        }
    }

    /** Minimal {@link Keyword} implementation for test construction. */
    private static class SimpleKeyword implements Keyword {
        private final String value;

        SimpleKeyword(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public JsonValidator newValidator(SchemaLocation schemaLocation,
                                          JsonNodePath evaluationPath,
                                          JsonNode schemaNode,
                                          JsonSchema parentSchema,
                                          ValidationContext validationContext) {
            // Not used in the tests.
            return null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(BaseJsonValidatorTest.class);
    private ObjectMapper mapper;
    private ObjectNode emptySchemaNode;
    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;
    private SimpleKeyword keyword;
    private SimpleErrorMessageType errorMessageType;
    private ExecutionContext executionContext;

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        emptySchemaNode = mapper.createObjectNode();
        schemaLocation = SchemaLocation.of("http://example.com");
        evaluationPath = new JsonNodePath(PathType.DEFAULT);
        keyword = new SimpleKeyword("testKeyword");
        errorMessageType = new SimpleErrorMessageType("E001");
        executionContext = new ExecutionContext();
    }

    /** Helper to create a {@link TestValidator} with default arguments. */
    private TestValidator createValidator(boolean suppressSubSchemaRetrieval) {
        return new TestValidator(schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,                      // parentSchema
                errorMessageType,
                keyword,
                null,                      // ValidationContext – not needed for the majority of tests
                suppressSubSchemaRetrieval);
    }

    @Test
    public void testEqualsUtility() {
        assertTrue("Numbers within tolerance should be equal", BaseJsonValidator.equals(1.000000000001, 1.0));
        assertFalse("Numbers outside tolerance should not be equal", BaseJsonValidator.equals(1.01, 1.0));
    }

    @Test
    public void testGettersAndToString() {
        TestValidator validator = createValidator(false);

        assertEquals("SchemaLocation getter must return the instance supplied in the constructor",
                schemaLocation, validator.getSchemaLocation());

        assertEquals("EvaluationPath getter must return the instance supplied in the constructor",
                evaluationPath, validator.getEvaluationPath());

        assertEquals("Keyword getter must delegate to the Keyword implementation",
                keyword.getValue(), validator.getKeyword());

        assertSame("SchemaNode getter must return the exact node passed to the constructor",
                emptySchemaNode, validator.getSchemaNode());

        assertNull("Parent schema is null in this test setup", validator.getParentSchema());

        // toString() delegates to evaluationPath.getName(-1).  For a freshly created path this returns null.
        // The test only verifies that the method does not throw.
        assertNull("toString() should return null when evaluationPath has no elements", validator.toString());
    }

    @Test
    public void testAtRootProducesPathWithCorrectType() {
        TestValidator validator = createValidator(false);
        JsonNodePath rootPath = validator.atRoot();
        assertNotNull("Root path must not be null", rootPath);
        assertEquals("Root path must use the default PathType", PathType.DEFAULT, rootPath.getPathType());
    }

    @Test
    public void testGetNodeFieldTypeWithNoParent() {
        TestValidator validator = createValidator(false);
        // parentSchema is null, therefore getNodeFieldType() must return null.
        assertNull("When parent schema is null, getNodeFieldType() should return null", validator.getNodeFieldType());
    }

    @Test
    public void testPreloadJsonSchemasWithEmptyCollection() {
        TestValidator validator = createValidator(false);
        // No exception should be thrown when the collection is empty.
        validator.preloadJsonSchemas(Collections.emptyList());
    }

    @Test
    public void testFetchSubSchemaNodeWhenSuppressed() {
        TestValidator validator = createValidator(true); // suppress = true
        // Even though validationContext is null, the method must return null without throwing NPE.
        assertNull("When sub‑schema retrieval is suppressed, fetchSubSchemaNode must return null",
                validator.fetchSubSchemaNode(null));
    }

    @Test
    public void testFetchSubSchemaNodeWhenNoIdPresent() {
        TestValidator validator = createValidator(false);
        // The schema node does not contain an "id" field, therefore obtainSubSchemaNode returns null.
        assertNull("When schema node lacks an \"id\" field, fetchSubSchemaNode must return null",
                validator.fetchSubSchemaNode(null));
    }

    @Test
    public void testValidateSimple() {
        TestValidator validator = createValidator(false);
        JsonNode instance = mapper.createObjectNode(); // any JSON node – validator does nothing.
        Set<ValidationMessage> result = validator.validate(executionContext, instance);
        assertNotNull("Result set must never be null", result);
        assertTrue("Our test validator always returns an empty set", result.isEmpty());
    }

    @Test
    public void testDebugMethodDoesNotThrow() {
        // The debug method only logs; we verify that it does not throw any exception.
        BaseJsonValidator.debug(logger, emptySchemaNode, emptySchemaNode, evaluationPath);
    }

    /**
     * A very small {@link OutputFormat} implementation used to test the overload
     * {@code validate(ExecutionContext, JsonNode, OutputFormat<T>)}.
     *
     * @param <T> the type returned by the format
     */
    private static class SimpleOutputFormat<T> implements OutputFormat<T> {
        private final T value;

        SimpleOutputFormat(T value) {
            this.value = value;
        }

        @Override
        public void customize(ExecutionContext executionContext, ValidationContext validationContext) {
            // No customisation needed for the test.
        }

        @Override
        public T format(Set<ValidationMessage> validationMessages,
                        ExecutionContext executionContext,
                        ValidationContext validationContext) {
            // Simply return the predetermined value irrespective of the input.
            return value;
        }
    }

    @Test
    public void testValidateWithOutputFormat() {
        TestValidator validator = createValidator(false);
        JsonNode instance = mapper.createObjectNode();

        SimpleOutputFormat<String> format = new SimpleOutputFormat<>("formatted-result");
        String result = validator.validate(executionContext, instance, format);
        assertEquals("The format's format() method should be invoked and its result returned",
                "formatted-result", result);
    }

    @Test
    public void testValidateWithOutputFormatAndCustomizer() {
        TestValidator validator = createValidator(false);
        JsonNode instance = mapper.createObjectNode();

        SimpleOutputFormat<Integer> format = new SimpleOutputFormat<>(42);
        ExecutionCustomizer customizer = (ctx, vCtx) -> {
            // No operation – just ensure the lambda can be called without error.
        };
        Integer result = validator.validate(executionContext, instance, format, customizer);
        assertEquals(Integer.valueOf(42), result);
    }

    /**
     * Minimal {@link OutputFormat} definition required for the tests above.
     * The real library defines this interface; we replicate only the methods we need.
     */
    public interface OutputFormat<T> {
        void customize(ExecutionContext executionContext, ValidationContext validationContext);
        T format(Set<ValidationMessage> validationMessages,
                 ExecutionContext executionContext,
                 ValidationContext validationContext);
    }

    /**
     * Minimal {@link ExecutionCustomizer} definition required for the tests above.
     * The real library defines this interface; we replicate only the method we need.
     */
    public interface ExecutionCustomizer {
        void customize(ExecutionContext executionContext, ValidationContext validationContext);
    }
}
```
###Test END##