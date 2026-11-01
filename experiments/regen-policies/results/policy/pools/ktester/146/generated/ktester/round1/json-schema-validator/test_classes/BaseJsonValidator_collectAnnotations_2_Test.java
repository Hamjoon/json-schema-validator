package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Collections;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_collectAnnotations_2_Test {

    // ----- Mocks for the constructor dependencies -----
    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidatorTypeCode validatorTypeCode;

    @Mock
    ValidationContext validationContext;

    // ----- Mocks for the method parameters -----
    @Mock
    ExecutionContext executionContext;

    @Mock
    ExecutionConfig executionConfig;

    // The concrete validator under test
    TestValidator validator;

    /* --------------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            // The real BaseJsonValidator constructor has many parameters; the
            // test harness supplies dummy values that compile.
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validatorType, validationContext, false);
        }

        /**
         * Expose the protected method for testing.
         */
        boolean callCollectAnnotations(ExecutionContext ec, String keyword) {
            return collectAnnotations(ec, keyword);
        }

        // Minimal implementation – not used in these tests.
        @Override
        public java.util.Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default the execution context returns the (mocked) execution config.
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        // Create a concrete validator. The constructor arguments are not relevant for
        // the protected method we are testing, therefore we pass the mocked values.
        validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorTypeCode, validationContext);
    }

    private static Stream<Arguments> provideSpecialKeywordScenarios() {
        return Stream.of(// empty string
        Arguments.of(true, "", true, true), Arguments.of(true, "", false, false), // whitespace string
        Arguments.of(true, "   ", true, true), // Unicode string
        Arguments.of(true, "µñïçødë", true, true), // disabled collection – filter would throw if invoked, but must not be called
        // filter result irrelevant
        Arguments.of(false, "anyKeyword", false, false));
    }

    @Test
    void shouldReturnFalseWhenAnnotationCollectionDisabled() {
        // Arrange
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(false);
        // The filter should never be consulted, but we stub it anyway.
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(keyword -> true);
        // Act
        boolean result = validator.callCollectAnnotations(executionContext, "anyKeyword");
        // Assert
        assertFalse(result, "collectAnnotations must return false when collection is disabled");
        // Verify that the filter was not invoked
        verify(executionConfig, never()).getAnnotationCollectionFilter();
    }

    @Test
    void shouldReturnFalseWhenFilterRejectsKeyword() {
        // Arrange
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        // Filter returns false for the supplied keyword
        Predicate<String> filter = kw -> false;
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);
        // Act
        boolean result = validator.callCollectAnnotations(executionContext, "rejectedKeyword");
        // Assert
        assertFalse(result, "collectAnnotations must return false when filter rejects the keyword");
        verify(executionConfig).getAnnotationCollectionFilter();
    }

    @Test
    void shouldReturnTrueWhenEnabledAndFilterAcceptsKeyword() {
        // Arrange
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        // Filter returns true only for the expected keyword
        Predicate<String> filter = kw -> "acceptedKeyword".equals(kw);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);
        // Act
        boolean result = validator.callCollectAnnotations(executionContext, "acceptedKeyword");
        // Assert
        assertTrue(result, "collectAnnotations must return true when enabled and filter accepts the keyword");
        verify(executionConfig).getAnnotationCollectionFilter();
    }

    @Test
    void shouldPropagateExceptionFromFilter() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> exploding = kw -> {
            throw new RuntimeException("filter exploded");
        };
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(exploding);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.callCollectAnnotations(executionContext, "badKeyword"));
        assertEquals("filter exploded", ex.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenFilterIsNull() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> validator.callCollectAnnotations(executionContext, "anyKeyword"));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenExecutionContextIsNull() {
        assertThrows(NullPointerException.class, () -> validator.callCollectAnnotations(null, "anyKeyword"));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenExecutionConfigIsNull() {
        when(executionContext.getExecutionConfig()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> validator.callCollectAnnotations(executionContext, "anyKeyword"));
    }

    @Test
    void shouldPropagateExceptionFromIsAnnotationCollectionEnabled() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenThrow(new IllegalStateException("config failure"));
        // The filter is irrelevant because the exception occurs first.
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(kw -> true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.callCollectAnnotations(executionContext, "anyKeyword"));
        assertEquals("config failure", ex.getMessage());
    }

    @Test
    void shouldPropagateNullPointerExceptionFromFilterWhenKeywordIsNull() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = kw -> {
            if (kw == null) {
                throw new NullPointerException("keyword is null");
            }
            return true;
        };
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> validator.callCollectAnnotations(executionContext, null));
        assertEquals("keyword is null", ex.getMessage());
    }

    @Test
    void shouldPropagateIllegalArgumentExceptionFromFilter() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = kw -> {
            if ("illegalKeyword".equals(kw)) {
                throw new IllegalArgumentException("illegal keyword");
            }
            return true;
        };
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.callCollectAnnotations(executionContext, "illegalKeyword"));
        assertEquals("illegal keyword", ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] enabled={0}, keyword=''{1}'', filterAccepts={2} => expected={3}")
    @MethodSource("provideSpecialKeywordScenarios")
    void shouldHandleSpecialKeywordValues(boolean enabled, String keyword, boolean filterAccepts, boolean expected) {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(enabled);
        Predicate<String> filter = kw -> filterAccepts;
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);
        boolean result = validator.callCollectAnnotations(executionContext, keyword);
        assertEquals(expected, result);
    }

    @Test
    void shouldNotInvokeFilterWhenCollectionDisabledEvenIfFilterThrows() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(false);
        // Configure a filter that would throw when called.
        Predicate<String> exploding = kw -> {
            throw new RuntimeException("should not be called");
        };
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(exploding);
        boolean result = validator.callCollectAnnotations(executionContext, "anyKeyword");
        assertFalse(result, "When collection is disabled the method must return false without invoking the filter");
        // Verify that the filter's test method was never called.
        verify(executionConfig, never()).getAnnotationCollectionFilter();
    }
}
