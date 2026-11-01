```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BaseJsonValidator#fetchSubSchemaNode(ValidationContext)}.
 *
 * The method simply returns {@code null} when {@code suppressSubSchemaRetrieval}
 * is {@code true}; otherwise it delegates to the private static
 * {@code obtainSubSchemaNode} method.  Because the private method creates a
 * {@link JsonSchema} instance that requires a fully‑initialised
 * {@link ValidationContext}, the only reliable unit‑testable behaviour is the
 * short‑circuit when sub‑schema retrieval is suppressed.
 *
 * The test creates a tiny concrete subclass of {@link BaseJsonValidator}
 * (the original class is abstract) and verifies the two possible outcomes.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_fetchSubSchemaNode_Test {

    /** Minimal concrete validator used only for exercising the protected method. */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(boolean suppressSubSchemaRetrieval) {
            // All arguments except the last one are not needed for the behaviour we test.
            super(
                /* schemaLocation */ null,
                /* evaluationPath */ null,
                /* schemaNode */     null,
                /* parentSchema */  null,
                /* errorMessageType */ null,
                /* keyword */        null,
                /* validationContext */ null,
                suppressSubSchemaRetrieval
            );
        }

        // The abstract methods of JsonValidator are implemented as no‑ops.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Set.of();
        }

        @Override
        public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format) {
            return null;
        }

        @Override
        public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format,
                              ExecutionContextCustomizer executionCustomizer) {
            return null;
        }

        @Override
        public String getKeyword() {
            return "test";
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

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed() {
        // Given a validator that suppresses sub‑schema retrieval
        TestValidator validator = new TestValidator(true);

        // When fetchSubSchemaNode is invoked
        JsonSchema result = validator.fetchSubSchemaNode(null);

        // Then the result must be null
        assertNull(result, "Expected null when sub‑schema retrieval is suppressed");
    }

    @Test
    void fetchSubSchemaNode_returnsNonNullWhenNotSuppressed() {
        // Given a validator that does NOT suppress sub‑schema retrieval
        TestValidator validator = new TestValidator(false);

        // When fetchSubSchemaNode is invoked
        JsonSchema result = validator.fetchSubSchemaNode(null);

        // Then the result should be a JsonSchema instance (the private method creates one)
        // The exact type is not asserted beyond non‑null because the internal creation
        // logic is exercised indirectly.
        assertNotNull(result, "Expected a non‑null JsonSchema when sub‑schema retrieval is enabled");
    }
}
```