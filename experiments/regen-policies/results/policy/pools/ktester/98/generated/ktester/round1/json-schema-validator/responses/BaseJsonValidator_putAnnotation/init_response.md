```java
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

/**
 * Tests for the protected {@code putAnnotation} method of {@link BaseJsonValidator}.
 *
 * <p>The test creates a minimal concrete subclass of {@code BaseJsonValidator}
 * because the class itself is abstract. All required constructor arguments are
 * mocked – they are not used by the method under test. The test verifies that
 *
 * <ul>
 *   <li>the {@code Consumer} supplied as a customiser is invoked with a
 *       {@link JsonNodeAnnotation.Builder} instance, and</li>
 *   <li>the resulting {@link JsonNodeAnnotation} is stored in the
 *       {@link ExecutionContext}'s annotation container.</li>
 * </ul>
 *
 * The real {@code ExecutionContext#getAnnotations()} method returns an
 * {@code Annotations} object that provides a {@code put(JsonNodeAnnotation)}
 * method.  This type is mocked, and the interaction is verified.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_putAnnotation_Test {

    /** The concrete validator used for the test. */
    private TestValidator validator;

    /** Mocked collaborators required by the {@code BaseJsonValidator} constructor. */
    @Mock private SchemaLocation mockSchemaLocation;
    @Mock private JsonNodePath mockEvaluationPath;
    @Mock private JsonNode mockSchemaNode;
    @Mock private JsonSchema mockParentSchema;
    @Mock private ValidatorTypeCode mockValidatorType;
    @Mock private ValidationContext mockValidationContext;

    /** Mocked execution context used in the test. */
    @Mock private ExecutionContext mockExecutionContext;

    /** Mocked annotations container returned by {@code ExecutionContext#getAnnotations()}. */
    @Mock private Annotations mockAnnotations;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

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

        // The validator does not use the execution‑context during construction,
        // therefore we can safely stub the annotation container here.
        when(mockExecutionContext.getAnnotations()).thenReturn(mockAnnotations);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
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

    @AfterAll
    static void teardownAfterAll() {
        // No global clean‑up required.
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

        /** The keyword used by the validator – a simple constant is sufficient. */
        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        /** No validation logic is required for the annotation test. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }
}
```