package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_toString_Test {

    /**
     * Mocks for the constructor parameters
     */
    @Mock
    SchemaLocation mockSchemaLocation;

    @Mock
    JsonNodePath mockEvaluationPath;

    @Mock
    JsonNode mockSchemaNode;

    @Mock
    JsonSchema mockParentSchema;

    @Mock
    ValidatorTypeCode mockValidatorType;

    @Mock
    ValidationContext mockValidationContext;

    /**
     * The concrete instance of the abstract BaseJsonValidator we are testing
     */
    BaseJsonValidator validatorUnderTest;

    @BeforeEach
    void setupBeforeEach() {
        // Stub the evaluation path to return a known name for the last segment.
        // The toString() implementation uses getEvaluationPath().getName(-1)
        when(mockEvaluationPath.getName(-1)).thenReturn("expectedPath");
        // Create a minimal concrete subclass of BaseJsonValidator.
        // Only the abstract methods of JsonValidator need to be supplied.
        validatorUnderTest = new BaseJsonValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext) {

            // JsonValidator requires a validate method – we provide a dummy implementation.
            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
                // no validation performed
                return Set.of();
            }

            @Override
            public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format) {
                return null;
            }

            @Override
            public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer) {
                return null;
            }

            // getKeyword is abstract in ValidationMessageHandler; return a dummy value.
            @Override
            public String getKeyword() {
                return "dummyKeyword";
            }
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        clearInvocations(mockEvaluationPath, mockSchemaLocation, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
    }

    private static Stream<Arguments> normalReturnValues() {
        return Stream.of(Arguments.of("leaf", "Normal case – several elements, last is 'leaf'"), Arguments.of("only", "Single‑element path, last is 'only'"), Arguments.of(null, "Empty path – getName(-1) returns null"), Arguments.of("", "Empty string as last element"), Arguments.of("   ", "Whitespace‑only last element"), Arguments.of("!@#$%^&*()_+-=[]{}|;':\",.<>/?", "Special characters in last element"), Arguments.of("finalNode", "Mixed null earlier element, last valid name"));
    }

    private static Stream<Arguments> nameMethodExceptions() {
        return Stream.of(Arguments.of(IndexOutOfBoundsException.class, "Path implementation throws IndexOutOfBoundsException for empty internal list"), Arguments.of(UnsupportedOperationException.class, "Custom JsonNodePath throws UnsupportedOperationException"), Arguments.of(IllegalArgumentException.class, "Custom JsonNodePath throws IllegalArgumentException for negative index"));
    }

    @Test
    void toString_returnsLastSegmentOfEvaluationPath() {
        // GIVEN the evaluation path is stubbed to return "expectedPath" for getName(-1)
        // WHEN toString() is invoked
        String result = validatorUnderTest.toString();
        // THEN the result should be the stubbed name
        assertEquals("expectedPath", result);
        // Verify that getName(-1) was indeed called.
        verify(mockEvaluationPath, times(1)).getName(-1);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("normalReturnValues")
    void toString_returnsExpectedValue(String stubbedResult, String description) {
        // GIVEN the evaluation path returns the stubbed result for getName(-1)
        when(mockEvaluationPath.getName(-1)).thenReturn(stubbedResult);
        // WHEN toString() is invoked
        String result = validatorUnderTest.toString();
        // THEN the result matches the stubbed value (including null)
        assertEquals(stubbedResult, result, "toString() should return the stubbed last segment");
        verify(mockEvaluationPath, times(1)).getName(-1);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("nameMethodExceptions")
    void toString_propagatesExceptionFromGetName(Class<? extends Throwable> exceptionClass, String description) {
        // GIVEN getName(-1) throws the specified unchecked exception
        when(mockEvaluationPath.getName(-1)).thenThrow(exceptionClass);
        // WHEN toString() is called
        // THEN the same exception is propagated
        assertThrows(exceptionClass, () -> validatorUnderTest.toString(), "toString() must propagate " + exceptionClass.getSimpleName());
        verify(mockEvaluationPath, times(1)).getName(-1);
    }

    @Test
    void toString_throws_NullPointerException_whenEvaluationPathIsNull() {
        // Create a validator that returns null from getEvaluationPath()
        BaseJsonValidator validatorWithNullPath = new BaseJsonValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext) {

            @Override
            public JsonNodePath getEvaluationPath() {
                // force null
                return null;
            }

            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
                return Set.of();
            }

            @Override
            public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format) {
                return null;
            }

            @Override
            public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer) {
                return null;
            }

            @Override
            public String getKeyword() {
                return "dummyKeyword";
            }
        };
        // THEN a NullPointerException is thrown
        assertThrows(NullPointerException.class, validatorWithNullPath::toString, "toString() should throw NPE when evaluation path is null");
    }

    @Test
    void toString_propagates_IllegalStateException_fromGetEvaluationPath() {
        BaseJsonValidator validator = new BaseJsonValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext) {

            @Override
            public JsonNodePath getEvaluationPath() {
                throw new IllegalStateException("forced");
            }

            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
                return Set.of();
            }

            @Override
            public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format) {
                return null;
            }

            @Override
            public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer) {
                return null;
            }

            @Override
            public String getKeyword() {
                return "dummyKeyword";
            }
        };
        assertThrows(IllegalStateException.class, validator::toString, "toString() must propagate IllegalStateException from getEvaluationPath()");
    }

    @Test
    void toString_propagates_RuntimeException_fromGetEvaluationPath() {
        BaseJsonValidator validator = new BaseJsonValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext) {

            @Override
            public JsonNodePath getEvaluationPath() {
                throw new RuntimeException("forced");
            }

            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
                return Set.of();
            }

            @Override
            public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format) {
                return null;
            }

            @Override
            public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer) {
                return null;
            }

            @Override
            public String getKeyword() {
                return "dummyKeyword";
            }
        };
        assertThrows(RuntimeException.class, validator::toString, "toString() must propagate RuntimeException from getEvaluationPath()");
    }

    @Test
    void toString_propagates_IllegalStateException_fromCustomJsonNodePath() {
        JsonNodePath throwingPath = mock(JsonNodePath.class);
        when(throwingPath.getName(-1)).thenThrow(new IllegalStateException("forced"));
        // Use a validator that returns the custom path
        BaseJsonValidator validator = new BaseJsonValidator(mockSchemaLocation, throwingPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext) {

            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
                return Set.of();
            }

            @Override
            public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format) {
                return null;
            }

            @Override
            public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer) {
                return null;
            }

            @Override
            public String getKeyword() {
                return "dummyKeyword";
            }
        };
        assertThrows(IllegalStateException.class, validator::toString, "toString() must propagate IllegalStateException from JsonNodePath#getName(-1)");
    }
}
