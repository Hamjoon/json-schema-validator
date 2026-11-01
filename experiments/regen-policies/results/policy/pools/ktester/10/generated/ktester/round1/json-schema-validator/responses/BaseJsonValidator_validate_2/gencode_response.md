```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#validate(ExecutionContext, JsonNode, OutputFormat)}.
 *
 * <p>The test class focuses on the three‑argument overload and on the
 * behaviour that is guaranteed by the abstract base class (null‑argument checks,
 * delegation to the four‑argument version, handling of an {@code ExecutionContextCustomizer},
 * and the protected static {@code equals(double,double)} helper).</p>
 *
 * <p>Only the parts of the validator that are required for the tests are
 * implemented; all other behaviour is stubbed with Mockito.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_validate_2_Test {

    /* --------------------------------------------------------------------- */
    /*  Mocks for the constructor dependencies of BaseJsonValidator          */
    /* --------------------------------------------------------------------- */
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;
    @Mock ValidatorTypeCode validatorType;
    @Mock ValidationContext validationContext;

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.  All abstract
     * methods are supplied with trivial implementations because the tests
     * exercise only the logic present in {@code BaseJsonValidator.validate(...)}.
     */
    private static class MinimalValidator extends BaseJsonValidator {

        MinimalValidator(SchemaLocation schemaLocation,
                         JsonNodePath evaluationPath,
                         JsonNode schemaNode,
                         JsonSchema parentSchema,
                         ValidatorTypeCode validatorType,
                         ValidationContext validationContext) {
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
    /*  Helper to create a validator instance                                 */
    /* --------------------------------------------------------------------- */
    private MinimalValidator createValidator() {
        return new MinimalValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext);
    }

    /* --------------------------------------------------------------------- */
    /*  1. Null‑argument checks                                               */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_nullExecutionContext_throwsNPE() {
        MinimalValidator validator = createValidator();

        JsonNode node = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> validator.validate(null, node, format));
        assertTrue(ex.getMessage().contains("executionContext"),
                "Exception message should mention executionContext");
    }

    @Test
    void validate_nullNode_throwsNPE() {
        MinimalValidator validator = createValidator();

        ExecutionContext ctx = new ExecutionContext();
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> validator.validate(ctx, null, format));
        assertTrue(ex.getMessage().contains("node"),
                "Exception message should mention node");
    }

    @Test
    void validate_nullFormat_throwsNPE() {
        MinimalValidator validator = createValidator();

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mock(JsonNode.class);

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> validator.validate(ctx, node, null));
        assertTrue(ex.getMessage().contains("format"),
                "Exception message should mention format");
    }

    /* --------------------------------------------------------------------- */
    /*  2. Delegation to the four‑argument overload (already covered)       */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_threeArgOverload_delegatesToFourArgVersion_withNullCustomizer() {
        // Arrange
        ExecutionContext execContext = new ExecutionContext();
        JsonNode instanceNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);

        // Use the TestValidator from the original scaffold – it overrides the
        // four‑argument method and records the received arguments.
        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext);

        // Act
        String result = validator.validate(execContext, instanceNode, format);

        // Assert
        assertEquals("delegatedResult", result);
        assertSame(execContext, validator.capturedExecutionContext);
        assertSame(instanceNode, validator.capturedNode);
        assertSame(format, validator.capturedFormat);
        assertNull(validator.capturedCustomizer,
                "The three‑arg overload must pass a null customizer to the four‑arg method");
    }

    /* --------------------------------------------------------------------- */
    /*  3. ExecutionContextCustomizer exception propagation                  */
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
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.validate(ctx, node, format, throwingCustomizer));

        assertEquals("customizer failure", ex.getMessage(),
                "Exception thrown by the customizer must be propagated unchanged");
    }

    /* --------------------------------------------------------------------- */
    /*  4. Protected static equals(double,double) helper                      */
    /* --------------------------------------------------------------------- */
    @Test
    void equals_nanValues_returnsFalse() throws Exception {
        Method equalsMethod = BaseJsonValidator.class
                .getDeclaredMethod("equals", double.class, double.class);
        equalsMethod.setAccessible(true);

        boolean result = (boolean) equalsMethod.invoke(null, Double.NaN, Double.NaN);
        assertFalse(result, "NaN should not be considered equal to NaN");
    }

    @Test
    void equals_positiveZeroAndNegativeZero_returnsTrue() throws Exception {
        Method equalsMethod = BaseJsonValidator.class
                .getDeclaredMethod("equals", double.class, double.class);
        equalsMethod.setAccessible(true);

        boolean result = (boolean) equalsMethod.invoke(null, 0.0d, -0.0d);
        assertTrue(result, "+0.0 and -0.0 must be treated as equal");
    }

    /* --------------------------------------------------------------------- */
    /*  5. OutputFormat exception propagation                                 */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_outputFormatThrows_exceptionPropagates() {
        MinimalValidator validator = createValidator();

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mock(JsonNode.class);

        @SuppressWarnings("unchecked")
        OutputFormat<String> explodingFormat = mock(OutputFormat.class);
        when(explodingFormat.format(anySet())).thenThrow(new RuntimeException("format failure"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.validate(ctx, node, explodingFormat));

        assertEquals("format failure", ex.getMessage(),
                "Exceptions from OutputFormat must propagate to the caller");
    }

    /* --------------------------------------------------------------------- */
    /*  6. Simple successful validation path (no messages)                    */
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
        doReturn(Collections.emptySet()).when(validator).validate(any(ExecutionContext.class),
                any(JsonNode.class));

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        when(format.format(Collections.emptySet())).thenReturn("OK");

        String result = validator.validate(ctx, node, format);
        assertEquals("OK", result, "When there are no validation messages the formatter's result should be returned");
    }

    /* --------------------------------------------------------------------- */
    /*  7. Validation failure path (messages present)                        */
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

    /* --------------------------------------------------------------------- */
    /*  Helper subclass used in the original three‑arg overload test        */
    /* --------------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        ExecutionContext capturedExecutionContext;
        JsonNode capturedNode;
        OutputFormat<?> capturedFormat;
        ExecutionContextCustomizer capturedCustomizer;

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

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

        @Override
        public <T> T validate(ExecutionContext executionContext,
                              JsonNode node,
                              OutputFormat<T> format,
                              ExecutionContextCustomizer executionCustomizer) {
            this.capturedExecutionContext = executionContext;
            this.capturedNode = node;
            this.capturedFormat = format;
            this.capturedCustomizer = executionCustomizer;
            @SuppressWarnings("unchecked")
            T result = (T) "delegatedResult";
            return result;
        }
    }
}
```