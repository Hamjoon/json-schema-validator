package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.networknt.schema.annotation.JsonNodeAnnotation;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_putAnnotation_Test {

    /**
     * The concrete validator used for the test.
     */
    private TestValidator validator;

    /**
     * Mocked collaborators required by the {@code BaseJsonValidator} constructor.
     */
    @Mock
    private SchemaLocation mockSchemaLocation;

    @Mock
    private JsonNodePath mockEvaluationPath;

    @Mock
    private JsonNode mockSchemaNode;

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    @Mock
    private ValidationContext mockValidationContext;

    /**
     * Mocked execution context used in the test.
     */
    @Mock
    private ExecutionContext mockExecutionContext;

    /**
     * Mocked annotations container returned by {@code ExecutionContext#getAnnotations()}.
     */
    @Mock
    private Annotations mockAnnotations;

    /**
     * Minimal concrete implementation BaseJsonValidator_putAnnotation_Test{@link BaseJsonValidator} that supplies
     * only the abstract members required for instantiation.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        /**
         * The keyword used by the validator – a simple constant is sufficient.
         */
        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        /**
         * No validation logic is required for the annotation test.
         */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build the concrete validator instance.
        validator = new TestValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
        // The validator does not use the execution‑context during construction,
        // therefore we can safely stub the annotation container here.
        when(mockExecutionContext.getAnnotations()).thenReturn(mockAnnotations);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid cross‑test interference.
        Mockito.reset(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, mockExecutionContext, mockAnnotations);
    }

    @Test
    void putAnnotation_shouldInvokeCustomizerAndStoreAnnotation() {
        // ------------------------------------------------------------
        // GIVEN – a customiser that simply adds a dummy property.
        // ------------------------------------------------------------
        @SuppressWarnings("unchecked")
        Consumer<JsonNodeAnnotation.Builder> customiser = mock(Consumer.class);
        // ------------------------------------------------------------
        // WHEN – the protected method is exercised via the concrete subclass.
        // ------------------------------------------------------------
        validator.putAnnotation(mockExecutionContext, customiser);
        // ------------------------------------------------------------
        // THEN – the customiser must have been called once with a builder.
        // ------------------------------------------------------------
        verify(customiser, times(1)).accept(any(JsonNodeAnnotation.Builder.class));
        // ------------------------------------------------------------
        // AND – the built annotation must have been stored in the context.
        // ------------------------------------------------------------
        verify(mockAnnotations, times(1)).put(any(JsonNodeAnnotation.class));
    }

    /**
     * Default case: the customiser does nothing.
     *
     * <p>We capture the {@link JsonNodeAnnotation} that is put into the
     * {@link Annotations} container and assert that its automatically populated
     * fields match the validator's state.
     */
    @Test
    void putAnnotation_Default_noCustomizerChanges() {
        // GIVEN – a no‑op customiser.
        Consumer<JsonNodeAnnotation.Builder> noOpCustomizer = builder -> {
            // intentionally left blank
        };
        // WHEN – invoke the method.
        validator.putAnnotation(mockExecutionContext, noOpCustomizer);
        // THEN – capture the annotation that was stored.
        ArgumentCaptor<JsonNodeAnnotation> captor = ArgumentCaptor.forClass(JsonNodeAnnotation.class);
        verify(mockAnnotations, times(1)).put(captor.capture());
        JsonNodeAnnotation stored = captor.getValue();
        // Verify the automatically populated fields using reflection (the
        // JsonNodeAnnotation API is not part of the public contract in this
        // context).
        Assertions.assertAll(() -> {
            // evaluationPath
            java.lang.reflect.Field f = JsonNodeAnnotation.class.getDeclaredField("evaluationPath");
            f.setAccessible(true);
            Object value = f.get(stored);
            Assertions.assertSame(mockEvaluationPath, value, "evaluationPath should be the validator's evaluationPath");
        }, () -> {
            // schemaLocation
            java.lang.reflect.Field f = JsonNodeAnnotation.class.getDeclaredField("schemaLocation");
            f.setAccessible(true);
            Object value = f.get(stored);
            Assertions.assertSame(mockSchemaLocation, value, "schemaLocation should be the validator's schemaLocation");
        }, () -> {
            // keyword
            java.lang.reflect.Field f = JsonNodeAnnotation.class.getDeclaredField("keyword");
            f.setAccessible(true);
            Object value = f.get(stored);
            Assertions.assertEquals(validator.getKeyword(), value, "keyword should be the validator's keyword");
        }, () -> {
            // optional fields – we expect them to be null (or default)
            java.lang.reflect.Field[] fields = JsonNodeAnnotation.class.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                if (field.getName().equals("evaluationPath") || field.getName().equals("schemaLocation") || field.getName().equals("keyword")) {
                    // already checked
                    continue;
                }
                field.setAccessible(true);
                Assertions.assertNull(field.get(stored), "Optional field '" + field.getName() + "' should be null by default");
            }
        });
    }

    /**
     * When a {@code null} {@link ExecutionContext} is supplied the method should
     * throw a {@link NullPointerException}.
     */
    @Test
    void putAnnotation_NullExecutionContext_throwsNPE() {
        // GIVEN – a no‑op customiser.
        Consumer<JsonNodeAnnotation.Builder> noOpCustomizer = builder -> {
        };
        // WHEN / THEN – NPE is expected.
        Assertions.assertThrows(NullPointerException.class, () -> validator.putAnnotation(null, noOpCustomizer), "Calling putAnnotation with a null ExecutionContext should throw NPE");
    }
}
