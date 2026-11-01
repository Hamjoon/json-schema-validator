```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.annotation.JsonNodeAnnotation;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for the protected {@code putAnnotation} method of {@link BaseJsonValidator}.
 *
 * <p>The tests cover two scenarios:
 *
 * <ul>
 *   <li>When the customiser does not modify the builder, the stored
 *       {@link JsonNodeAnnotation} must contain only the automatically
 *       populated fields ({@code evaluationPath}, {@code schemaLocation},
 *       {@code keyword}).</li>
 *   <li>When a {@code null} {@link ExecutionContext} is supplied, the method
 *       should throw a {@link NullPointerException}.</li>
 * </ul>
 *
 * <p>A minimal concrete subclass {@code TestValidator} is created because
 * {@code BaseJsonValidator} is abstract. All constructor arguments are mocked
 * because they are not used by the method under test.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_putAnnotation_Test {

    /** Concrete validator used for the tests. */
    private TestValidator validator;

    /** Mocked collaborators required by the {@code BaseJsonValidator} constructor. */
    @Mock private SchemaLocation mockSchemaLocation;
    @Mock private JsonNodePath mockEvaluationPath;
    @Mock private JsonNode mockSchemaNode;
    @Mock private JsonSchema mockParentSchema;
    @Mock private ValidatorTypeCode mockValidatorType;
    @Mock private ValidationContext mockValidationContext;

    /** Mocked execution context used in the tests. */
    @Mock private ExecutionContext mockExecutionContext;

    /** Mocked annotations container returned by {@code ExecutionContext#getAnnotations()}. */
    @Mock private Annotations mockAnnotations;

    @BeforeEach
    void setupBeforeEach() {
        // Build the concrete validator instance.
        validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext);

        // Stub the annotation container.
        when(mockExecutionContext.getAnnotations()).thenReturn(mockAnnotations);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid cross‑test interference.
        Mockito.reset(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                mockExecutionContext,
                mockAnnotations);
    }

    /**
     * Verifies that the customiser is invoked and the resulting annotation is stored.
     */
    @Test
    void putAnnotation_shouldInvokeCustomizerAndStoreAnnotation() {
        // GIVEN – a mocked customiser.
        @SuppressWarnings("unchecked")
        Consumer<JsonNodeAnnotation.Builder> customiser = mock(Consumer.class);

        // WHEN – the protected method is exercised via the concrete subclass.
        validator.putAnnotation(mockExecutionContext, customiser);

        // THEN – the customiser must have been called once with a builder.
        verify(customiser, times(1)).accept(any(JsonNodeAnnotation.Builder.class));

        // AND – the built annotation must have been stored in the context.
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
        Assertions.assertAll(
                () -> {
                    // evaluationPath
                    java.lang.reflect.Field f = JsonNodeAnnotation.class.getDeclaredField("evaluationPath");
                    f.setAccessible(true);
                    Object value = f.get(stored);
                    Assertions.assertSame(mockEvaluationPath, value,
                            "evaluationPath should be the validator's evaluationPath");
                },
                () -> {
                    // schemaLocation
                    java.lang.reflect.Field f = JsonNodeAnnotation.class.getDeclaredField("schemaLocation");
                    f.setAccessible(true);
                    Object value = f.get(stored);
                    Assertions.assertSame(mockSchemaLocation, value,
                            "schemaLocation should be the validator's schemaLocation");
                },
                () -> {
                    // keyword
                    java.lang.reflect.Field f = JsonNodeAnnotation.class.getDeclaredField("keyword");
                    f.setAccessible(true);
                    Object value = f.get(stored);
                    Assertions.assertEquals(validator.getKeyword(), value,
                            "keyword should be the validator's keyword");
                },
                () -> {
                    // optional fields – we expect them to be null (or default)
                    java.lang.reflect.Field[] fields = JsonNodeAnnotation.class.getDeclaredFields();
                    for (java.lang.reflect.Field field : fields) {
                        if (field.getName().equals("evaluationPath")
                                || field.getName().equals("schemaLocation")
                                || field.getName().equals("keyword")) {
                            continue; // already checked
                        }
                        field.setAccessible(true);
                        Assertions.assertNull(field.get(stored),
                                "Optional field '" + field.getName() + "' should be null by default");
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
        Consumer<JsonNodeAnnotation.Builder> noOpCustomizer = builder -> {};

        // WHEN / THEN – NPE is expected.
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.putAnnotation(null, noOpCustomizer),
                "Calling putAnnotation with a null ExecutionContext should throw NPE");
    }

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} that supplies
     * only the abstract members required for instantiation.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                  validatorType, validationContext);
        }

        /** Simple constant keyword for testing. */
        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        /** No validation logic is required for annotation tests. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }
}
```