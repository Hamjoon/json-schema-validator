package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_validate_2_Test {

    /* --------------------------------------------------------------------- */
    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidatorTypeCode validatorType;

    @Mock
    ValidationContext validationContext;

    /**
     * Concrete subclass of {@link BaseJsonValidator} that overrides the
     * four‑argument {@code validate} method so we can capture the parameters
     * it receives. This allows us to verify that the three‑argument overload
     * correctly delegates to it with a {@code null} {@link ExecutionContextCustomizer}.
     */
    private static class TestValidator extends BaseJsonValidator {

        // Captured arguments for verification
        ExecutionContext capturedExecutionContext;

        JsonNode capturedNode;

        OutputFormat<?> capturedFormat;

        ExecutionContextCustomizer capturedCustomizer;

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        // Minimal implementations of abstract members required by JsonValidator
        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }

        /**
         * The method under test ultimately calls this implementation.
         */
        @Override
        public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer) {
            this.capturedExecutionContext = executionContext;
            this.capturedNode = node;
            this.capturedFormat = format;
            this.capturedCustomizer = executionCustomizer;
            // Return a deterministic value so the test can assert on it.
            @SuppressWarnings("unchecked")
            T result = (T) "delegatedResult";
            return result;
        }
    }

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.  All abstract
     * methods are supplied with trivial implementations because the tests
     * exercise only the logic present in {@code BaseJsonValidator.validate(...)}.
     */
    private static class MinimalValidator extends BaseJsonValidator {

        MinimalValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public String getKeyword() {
            return "minimal";
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

    /* --------------------------------------------------------------------- */
    private MinimalValidator createValidator() {
        return new MinimalValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
    }

    @Test
    void validate_threeArgOverload_delegatesToFourArgVersion_withNullCustomizer() {
        // Arrange ------------------------------------------------------------
        // real instance – no heavy setup required
        ExecutionContext execContext = new ExecutionContext();
        JsonNode instanceNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        // Act ----------------------------------------------------------------
        String result = validator.validate(execContext, instanceNode, format);
        // Assert -------------------------------------------------------------
        assertEquals("delegatedResult", result, "The three‑arg overload should return the value from the four‑arg method.");
        assertSame(execContext, validator.capturedExecutionContext, "ExecutionContext should be passed through unchanged.");
        assertSame(instanceNode, validator.capturedNode, "JsonNode should be passed through unchanged.");
        assertSame(format, validator.capturedFormat, "OutputFormat should be passed through unchanged.");
        assertNull(validator.capturedCustomizer, "ExecutionContextCustomizer must be null for the three‑arg overload.");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_nullExecutionContext_throwsNPE() {
        MinimalValidator validator = createValidator();
        JsonNode node = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> validator.validate(null, node, format));
        assertTrue(ex.getMessage().contains("executionContext"), "Exception message should mention executionContext");
    }

    @Test
    void validate_nullNode_throwsNPE() {
        MinimalValidator validator = createValidator();
        ExecutionContext ctx = new ExecutionContext();
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> validator.validate(ctx, null, format));
        assertTrue(ex.getMessage().contains("node"), "Exception message should mention node");
    }

    @Test
    void validate_nullFormat_throwsNPE() {
        MinimalValidator validator = createValidator();
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mock(JsonNode.class);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> validator.validate(ctx, node, null));
        assertTrue(ex.getMessage().contains("format"), "Exception message should mention format");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_withCustomizerThatThrows_exceptionPropagates() throws Exception {
        MinimalValidator validator = createValidator();
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        // A customizer that throws a RuntimeException when invoked.
        Consumer<ExecutionContext> throwingCustomizer = mock(Consumer.class);
        doThrow(new RuntimeException("customizer failure")).when(throwingCustomizer).accept(any());
        // The four‑argument method is public, so we can call it directly.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.validate(ctx, node, format, throwingCustomizer));
        assertEquals("customizer failure", ex.getMessage(), "Exception thrown by the customizer must be propagated unchanged");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equals_nanValues_returnsFalse() throws Exception {
        Method equalsMethod = BaseJsonValidator.class.getDeclaredMethod("equals", double.class, double.class);
        equalsMethod.setAccessible(true);
        boolean result = (boolean) equalsMethod.invoke(null, Double.NaN, Double.NaN);
        assertFalse(result, "NaN should not be considered equal to NaN");
    }

    @Test
    void equals_positiveZeroAndNegativeZero_returnsTrue() throws Exception {
        Method equalsMethod = BaseJsonValidator.class.getDeclaredMethod("equals", double.class, double.class);
        equalsMethod.setAccessible(true);
        boolean result = (boolean) equalsMethod.invoke(null, 0.0d, -0.0d);
        assertTrue(result, "+0.0 and -0.0 must be treated as equal");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_outputFormatThrows_exceptionPropagates() {
        MinimalValidator validator = createValidator();
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> explodingFormat = mock(OutputFormat.class);
        when(explodingFormat.format(anySet())).thenThrow(new RuntimeException("format failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.validate(ctx, node, explodingFormat));
        assertEquals("format failure", ex.getMessage(), "Exceptions from OutputFormat must propagate to the caller");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_successfulValidation_returnsFormattedResult() {
        // This test uses a spy to stub the internal validation logic so that
        // it returns an empty set of ValidationMessage objects.
        MinimalValidator realValidator = createValidator();
        MinimalValidator validator = spy(realValidator);
        // Stub the protected method that actually performs validation.
        // In the real implementation it would collect messages; we force an
        // empty set here.
        doReturn(Collections.emptySet()).when(validator).validate(any(ExecutionContext.class), any(JsonNode.class));
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        when(format.format(Collections.emptySet())).thenReturn("OK");
        String result = validator.validate(ctx, node, format);
        assertEquals("OK", result, "When there are no validation messages the formatter's result should be returned");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_failureValidation_returnsFormattedResultWithMessages() {
        MinimalValidator realValidator = createValidator();
        MinimalValidator validator = spy(realValidator);
        ValidationMessage msg = mock(ValidationMessage.class);
        Set<ValidationMessage> messages = Collections.singleton(msg);
        doReturn(messages).when(validator).validate(any(ExecutionContext.class), any(JsonNode.class));
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        when(format.format(messages)).thenReturn("FAIL");
        String result = validator.validate(ctx, node, format);
        assertEquals("FAIL", result, "Formatter should receive the validation messages");
    }
}
