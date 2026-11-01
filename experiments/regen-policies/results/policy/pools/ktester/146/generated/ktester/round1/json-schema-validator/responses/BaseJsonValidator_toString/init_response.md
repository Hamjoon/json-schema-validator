```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link BaseJsonValidator#toString()}.
 *
 * The method simply delegates to {@code getEvaluationPath().getName(-1)}.
 * We therefore verify that the returned string matches the value supplied by
 * the {@link JsonNodePath} mock.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_toString_Test {

    // ----- Mocks for constructor dependencies ---------------------------------
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

    // ----- Instance under test ------------------------------------------------
    private TestValidator validator;

    // ----- Helper concrete implementation ------------------------------------
    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} required for instantiation.
     * Only {@link #validate(ExecutionContext, JsonNode)} is abstract; we return an
     * empty set because the validation logic is irrelevant for the {@code toString()}
     * test.
     */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode,
                  parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    // ----- Test lifecycle -----------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the evaluationPath to return a known name for index -1.
        when(evaluationPath.getName(-1)).thenReturn("expectedPath");

        // Instantiate the concrete validator with the mocked collaborators.
        validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    // ----- Actual test ---------------------------------------------------------
    @Test
    void toString_returnsEvaluationPathName() {
        // Act
        String result = validator.toString();

        // Assert
        assertEquals("expectedPath", result,
                "BaseJsonValidator.toString() should delegate to getEvaluationPath().getName(-1)");
        // Verify that the delegation actually happened.
        verify(evaluationPath).getName(-1);
    }
}
```