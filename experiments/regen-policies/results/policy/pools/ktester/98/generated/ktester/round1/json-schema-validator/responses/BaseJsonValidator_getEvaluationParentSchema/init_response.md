```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getEvaluationParentSchema_Test {

    // Mocks that are shared between tests
    @Mock ValidationContext validationContext;
    @Mock MessageSource messageSource;
    @Mock Keyword keyword;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;

    /** Concrete subclass of the abstract BaseJsonValidator used only for testing. */
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
                      JsonSchema evaluationParentSchema) {
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
                  Collections.emptyMap());
        }

        // Minimal implementations required by the JsonValidator interface
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public String getKeyword() {
            return "test-keyword";
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required beyond the @Mock fields
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
    void shouldReturnEvaluationParentSchemaWhenItIsSet() {
        // GIVEN an evaluationParentSchema is supplied
        JsonSchema evaluationParent = mock(JsonSchema.class);

        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,                     // ErrorMessageType – not needed for this test
                null,                     // errorMessageKeyword
                messageSource,
                keyword,
                mock(JsonSchema.class),   // parentSchema (won’t be used in this case)
                schemaLocation,
                evaluationPath,
                evaluationParent);

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN it returns the supplied evaluationParentSchema
        assertSame(evaluationParent, result,
                "When evaluationParentSchema is non‑null, getEvaluationParentSchema() must return it");
    }

    @Test
    void shouldReturnParentSchemaWhenEvaluationParentSchemaIsNull() {
        // GIVEN no evaluationParentSchema (null) and a parent schema
        JsonSchema parent = mock(JsonSchema.class);

        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                null,
                null,
                messageSource,
                keyword,
                parent,                   // parentSchema
                schemaLocation,
                evaluationPath,
                null);                    // evaluationParentSchema is null

        // WHEN the method is invoked
        JsonSchema result = validator.getEvaluationParentSchema();

        // THEN it falls back to the parent schema
        assertSame(parent, result,
                "When evaluationParentSchema is null, getEvaluationParentSchema() must return the parent schema");
    }
}
```