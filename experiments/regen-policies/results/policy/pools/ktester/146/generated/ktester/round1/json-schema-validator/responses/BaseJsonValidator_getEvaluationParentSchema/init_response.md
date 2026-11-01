```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link BaseJsonValidator#getEvaluationParentSchema()}.
 *
 * <p>The method should return the {@code evaluationParentSchema} when it is
 * non‑null; otherwise it must fall back to the {@code parentSchema}.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getEvaluationParentSchema_Test {

    /** Concrete subclass used only for testing the protected method. */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(boolean suppressSubSchemaRetrieval,
                     JsonNode schemaNode,
                     ValidationContext validationContext,
                     ErrorMessageType errorMessageType,
                     String errorMessageKeyword,
                     MessageSource messageSource,
                     Keyword keyword,
                     JsonSchema parentSchema,
                     SchemaLocation schemaLocation,
                     JsonNodePath evaluationPath,
                     JsonSchema evaluationParentSchema,
                     Map<String, String> errorMessage) {
            super(suppressSubSchemaRetrieval,
                  schemaNode,
                  validationContext,
                  errorMessageType,
                  errorMessageKeyword,
                  messageSource,
                  keyword,
                  parentSchema,
                  schemaLocation,
                  evaluationPath,
                  evaluationParentSchema,
                  errorMessage);
        }

        /* JsonValidator contract – not relevant for these tests */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
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
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void getEvaluationParentSchema_returnsEvaluationParentWhenSet() {
        // ----- Arrange -------------------------------------------------------
        JsonSchema parentSchema = mock(JsonSchema.class);
        JsonSchema evaluationParentSchema = mock(JsonSchema.class);

        JsonNode schemaNode = mock(JsonNode.class);
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        ValidationContext validationContext = mock(ValidationContext.class);
        ErrorMessageType errorMessageType = mock(ErrorMessageType.class);
        MessageSource messageSource = mock(MessageSource.class);
        Keyword keyword = mock(Keyword.class);
        Map<String, String> errorMap = new HashMap<>();

        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                errorMessageType,
                null,
                messageSource,
                keyword,
                parentSchema,
                schemaLocation,
                evaluationPath,
                evaluationParentSchema,
                errorMap);

        // ----- Act -----------------------------------------------------------
        JsonSchema result = validator.getEvaluationParentSchema();

        // ----- Assert ---------------------------------------------------------
        assertSame(evaluationParentSchema, result,
                "When evaluationParentSchema is provided it should be returned");
    }

    @Test
    void getEvaluationParentSchema_returnsParentWhenEvaluationParentIsNull() {
        // ----- Arrange -------------------------------------------------------
        JsonSchema parentSchema = mock(JsonSchema.class);

        JsonNode schemaNode = mock(JsonNode.class);
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        ValidationContext validationContext = mock(ValidationContext.class);
        ErrorMessageType errorMessageType = mock(ErrorMessageType.class);
        MessageSource messageSource = mock(MessageSource.class);
        Keyword keyword = mock(Keyword.class);
        Map<String, String> errorMap = new HashMap<>();

        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                errorMessageType,
                null,
                messageSource,
                keyword,
                parentSchema,
                schemaLocation,
                evaluationPath,
                null,                // evaluationParentSchema is null
                errorMap);

        // ----- Act -----------------------------------------------------------
        JsonSchema result = validator.getEvaluationParentSchema();

        // ----- Assert ---------------------------------------------------------
        assertSame(parentSchema, result,
                "When evaluationParentSchema is null the method should fall back to parentSchema");
    }
}
```