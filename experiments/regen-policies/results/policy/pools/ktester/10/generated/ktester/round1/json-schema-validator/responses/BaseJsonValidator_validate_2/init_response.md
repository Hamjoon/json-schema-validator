```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

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

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mocks are injected automatically by MockitoExtension.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up between tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

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

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
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
        public <T> T validate(ExecutionContext executionContext,
                              JsonNode node,
                              OutputFormat<T> format,
                              ExecutionContextCustomizer executionCustomizer) {
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

    @Test
    void validate_threeArgOverload_delegatesToFourArgVersion_withNullCustomizer() {
        // Arrange ------------------------------------------------------------
        ExecutionContext execContext = new ExecutionContext();               // real instance – no heavy setup required
        JsonNode instanceNode = mock(JsonNode.class);
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);

        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext);

        // Act ----------------------------------------------------------------
        String result = validator.validate(execContext, instanceNode, format);

        // Assert -------------------------------------------------------------
        assertEquals("delegatedResult", result, "The three‑arg overload should return the value from the four‑arg method.");
        assertSame(execContext, validator.capturedExecutionContext, "ExecutionContext should be passed through unchanged.");
        assertSame(instanceNode, validator.capturedNode, "JsonNode should be passed through unchanged.");
        assertSame(format, validator.capturedFormat, "OutputFormat should be passed through unchanged.");
        assertNull(validator.capturedCustomizer, "ExecutionContextCustomizer must be null for the three‑arg overload.");
    }
}
```