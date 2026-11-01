```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
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
 * non‑null; otherwise it must fall back to the {@code parentSchema}.  The
 * tests also verify that the fallback is dynamic – a change to the parent
 * schema after the first call is reflected on subsequent calls.</p>
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

    /* --------------------------------------------------------------------- *
     *  Group: EvaluationParentSchema Retrieval
     * --------------------------------------------------------------------- */

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

    @Test
    void getEvaluationParentSchema_returnsNullWhenBothSchemasAreNull() {
        // ----- Arrange -------------------------------------------------------
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
                null,               // parentSchema is null
                schemaLocation,
                evaluationPath,
                null,               // evaluationParentSchema is null
                errorMap);

        // ----- Act -----------------------------------------------------------
        JsonSchema result = validator.getEvaluationParentSchema();

        // ----- Assert ---------------------------------------------------------
        assertNull(result,
                "When both evaluationParentSchema and parentSchema are null the method should return null");
    }

    /* --------------------------------------------------------------------- *
     *  Group: Dynamic Parent Schema Retrieval
     * --------------------------------------------------------------------- */

    @Test
    void getEvaluationParentSchema_reflectsUpdatedParentSchema() throws Exception {
        // ----- Arrange -------------------------------------------------------
        JsonSchema parentSchemaA = mock(JsonSchema.class);
        JsonSchema parentSchemaB = mock(JsonSchema.class);

        JsonNode schemaNode = mock(JsonNode.class);
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        ValidationContext validationContext = mock(ValidationContext.class);
        ErrorMessageType errorMessageType = mock(ErrorMessageType.class);
        MessageSource messageSource = mock(MessageSource.class);
        Keyword keyword = mock(Keyword.class);
        Map<String, String> errorMap = new HashMap<>();

        // Start with parentSchemaA and no evaluationParentSchema
        TestValidator validator = new TestValidator(
                false,
                schemaNode,
                validationContext,
                errorMessageType,
                null,
                messageSource,
                keyword,
                parentSchemaA,
                schemaLocation,
                evaluationPath,
                null,
                errorMap);

        // ----- Act & Assert (first call) ------------------------------------
        JsonSchema firstResult = validator.getEvaluationParentSchema();
        assertSame(parentSchemaA, firstResult,
                "First call should return the initially supplied parent schema (A)");

        // ----- Change the internal parent schema to B using reflection -------
        Field parentField = BaseJsonValidator.class.getDeclaredField("parentSchema");
        parentField.setAccessible(true);
        parentField.set(validator, parentSchemaB);

        // ----- Act & Assert (second call) ------------------------------------
        JsonSchema secondResult = validator.getEvaluationParentSchema();
        assertSame(parentSchemaB, secondResult,
                "After mutating the internal parent schema the method should return the new instance (B)");
    }
}
```