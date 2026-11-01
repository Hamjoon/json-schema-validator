```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#collectAnnotations(ExecutionContext, String)}.
 *
 * <p>
 * The method under test is {@code protected}, therefore a minimal concrete subclass
 * {@link TestValidator} is created that simply forwards the call. All collaborators are
 * mocked so that the tests focus exclusively on the decision logic inside the method.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_collectAnnotations_2_Test {

    /* ----- Mocks required for the BaseJsonValidator constructor ----- */
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;
    @Mock ValidatorTypeCode validatorTypeCode;
    @Mock ValidationContext validationContext;

    /* ----- Mocks for the method parameters ----- */
    @Mock ExecutionContext executionContext;
    @Mock ExecutionConfig executionConfig;

    /* ----- Instance under test ----- */
    TestValidator validator;

    @BeforeEach
    void setupBeforeEach() {
        // By default the execution context returns the (mocked) execution config.
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);

        // Create a concrete validator. The constructor arguments are not relevant for
        // the protected method we are testing, therefore we pass the mocked values.
        validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorTypeCode,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Mockito cleans up automatically.
    }

    /* --------------------------------------------------------------------- */
    /* Simple boolean decision branches (already covered in the template)   */
    /* --------------------------------------------------------------------- */

    @Test
    void shouldReturnFalseWhenAnnotationCollectionDisabled() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(false);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(kw -> true); // never used

        boolean result = validator.callCollectAnnotations(executionContext, "anyKeyword");

        assertFalse(result, "collectAnnotations must return false when collection is disabled");
        verify(executionConfig, never()).getAnnotationCollectionFilter();
    }

    @Test
    void shouldReturnFalseWhenFilterRejectsKeyword() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = kw -> false;
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        boolean result = validator.callCollectAnnotations(executionContext, "rejectedKeyword");

        assertFalse(result, "collectAnnotations must return false when filter rejects the keyword");
        verify(executionConfig).getAnnotationCollectionFilter();
    }

    @Test
    void shouldReturnTrueWhenEnabledAndFilterAcceptsKeyword() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> filter = kw -> "acceptedKeyword".equals(kw);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        boolean result = validator.callCollectAnnotations(executionContext, "acceptedKeyword");

        assertTrue(result, "collectAnnotations must return true when enabled and filter accepts the keyword");
        verify(executionConfig).getAnnotationCollectionFilter();
    }

    /* --------------------------------------------------------------------- */
    /* Exception handling                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    void shouldPropagateExceptionFromFilter() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        Predicate<String> exploding = kw -> {
            throw new RuntimeException("filter exploded");
        };
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(exploding);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.callCollectAnnotations(executionContext, "badKeyword"));
        assertEquals("filter exploded", ex.getMessage());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenFilterIsNull() {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> validator.callCollectAnnotations(executionContext, "anyKeyword"));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenExecutionContextIsNull() {
        assertThrows(NullPointerException.class,
                () -> validator.callCollectAnnotations(null, "anyKeyword"));
    }

    @Test
    void shouldThrowNullPointerExceptionWhenExecutionConfigIsNull() {
        when(executionContext.getExecutionConfig()).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> validator.callCollectAnnotations(executionContext, "anyKeyword"));
    }

    @Test
    void shouldPropagateExceptionFromIsAnnotationCollectionEnabled() {
        when(executionConfig.isAnnotationCollectionEnabled())
                .thenThrow(new IllegalStateException("config failure"));
        // The filter is irrelevant because the exception occurs first.
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(kw -> true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.callCollectAnnotations(executionContext, "anyKeyword"));
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

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> validator.callCollectAnnotations(executionContext, null));
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

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.callCollectAnnotations(executionContext, "illegalKeyword"));
        assertEquals("illegal keyword", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* Special keyword values – parameterized test                         */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] enabled={0}, keyword=''{1}'', filterAccepts={2} => expected={3}")
    @MethodSource("provideSpecialKeywordScenarios")
    void shouldHandleSpecialKeywordValues(boolean enabled,
                                          String keyword,
                                          boolean filterAccepts,
                                          boolean expected) {
        when(executionConfig.isAnnotationCollectionEnabled()).thenReturn(enabled);
        Predicate<String> filter = kw -> filterAccepts;
        when(executionConfig.getAnnotationCollectionFilter()).thenReturn(filter);

        boolean result = validator.callCollectAnnotations(executionContext, keyword);
        assertEquals(expected, result);
    }

    private static Stream<Arguments> provideSpecialKeywordScenarios() {
        return Stream.of(
                // empty string
                Arguments.of(true, "", true, true),
                Arguments.of(true, "", false, false),

                // whitespace string
                Arguments.of(true, "   ", true, true),

                // Unicode string
                Arguments.of(true, "µñïçødë", true, true),

                // disabled collection – filter would throw if invoked, but must not be called
                Arguments.of(false, "anyKeyword", false, false) // filter result irrelevant
        );
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

    /* --------------------------------------------------------------------- */
    /* Helper concrete class – only implements the abstract validate method */
    /* --------------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            // The real BaseJsonValidator constructor has many parameters; the
            // test harness supplies dummy values that compile.
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                    validatorType, validatorType, validationContext, false);
        }

        /** Expose the protected method for testing. */
        boolean callCollectAnnotations(ExecutionContext ec, String keyword) {
            return collectAnnotations(ec, keyword);
        }

        // Minimal implementation – not used in these tests.
        @Override
        public java.util.Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }
}
```