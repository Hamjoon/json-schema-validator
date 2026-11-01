```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.annotation.JsonNodeAnnotation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#putAnnotation(ExecutionContext, java.util.function.Consumer)}.
 *
 * The test creates a minimal concrete subclass of {@link BaseJsonValidator} that
 * exposes the protected {@code putAnnotation} method.  A real {@link ExecutionContext}
 * is used and the resulting annotations map is inspected to verify that the
 * annotation added by the customiser is stored correctly.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_putAnnotation_Test {

    /** Minimal concrete validator used only for exercising {@code putAnnotation}. */
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

        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for the putAnnotation test.
            return Collections.emptySet();
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            // The protected field {@code evaluationPath} is already set by the super‑constructor.
            return super.getEvaluationPath();
        }
    }

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

    private TestValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a concrete validator instance with mocked collaborators.
        validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Verifies that {@code putAnnotation} stores a {@link JsonNodeAnnotation}
     * built by the supplied customiser into the {@link ExecutionContext}'s
     * annotation map.
     */
    @Test
    void putAnnotationAddsAnnotationToExecutionContext() {
        // GIVEN – a fresh execution context
        ExecutionContext executionContext = new ExecutionContext();

        // The annotations map is initially empty
        Map<String, JsonNodeAnnotation> before = executionContext.getAnnotations();
        assertTrue(before.isEmpty(), "Annotations map should be empty before putAnnotation");

        // WHEN – we add a custom annotation
        validator.putAnnotation(executionContext, builder ->
                builder.annotation("myKey", "myValue"));

        // THEN – the annotation map contains exactly one entry
        Map<String, JsonNodeAnnotation> after = executionContext.getAnnotations();
        assertEquals(1, after.size(), "Exactly one annotation should have been added");

        JsonNodeAnnotation annotation = after.values().iterator().next();
        assertNotNull(annotation, "Stored annotation must not be null");
        // The builder copies the supplied key/value into the annotation.
        // The concrete JsonNodeAnnotation API provides {@code getKey} and {@code getValue}
        // – verify that they match the customiser's input.
        assertEquals("myKey", annotation.getKey(), "Annotation key should match the customiser");
        assertEquals("myValue", annotation.getValue(), "Annotation value should match the customiser");

        // Also verify that the annotation carries the evaluation path, schema location
        // and keyword supplied by the validator.
        assertEquals(mockEvaluationPath, annotation.getEvaluationPath(),
                "Evaluation path should be the one from the validator");
        assertEquals(mockSchemaLocation, annotation.getSchemaLocation(),
                "Schema location should be the one from the validator");
        assertEquals("testKeyword", annotation.getKeyword(),
                "Keyword should be the one returned by getKeyword()");
    }
}
```