package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getParentSchema_Test {

    // -----------------------------------------------------------------------
    @Mock
    private SchemaLocation mockSchemaLocation;

    @Mock
    private JsonNodePath mockEvaluationPath;

    @Mock
    private JsonNode mockSchemaNode;

    @Mock
    private ValidationContext mockValidationContext;

    @Mock
    private ValidatorTypeCode mockValidatorTypeCode;

    @Mock
    private JsonSchema mockParentSchema;

    private TestValidator validatorWithParent;

    private TestValidator validatorWithoutParent;

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} used only for
     * exercising the {@code getParentSchema()} method and a few auxiliary
     * operations required by the extended test suite.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        // -------------------------------------------------------------------
        // JsonValidator abstract methods – provide trivial implementations
        // -------------------------------------------------------------------
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public String getKeyword() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return null;
        }

        // -------------------------------------------------------------------
        // Helper methods for the new test scenarios
        // -------------------------------------------------------------------
        /**
         * Allows the test to change the protected {@code parentSchema} field
         * after construction (used by the “Null After Reset” test).
         */
        public void setParentSchema(JsonSchema schema) {
            this.parentSchema = schema;
        }

        /**
         * Public wrapper around the protected {@code fetchSubSchemaNode}
         * method.  The implementation in the base class may rely on the
         * provided {@link ValidationContext}; we simply delegate to it.
         */
        public JsonSchema invokeFetchSubSchemaNode() {
            return super.fetchSubSchemaNode(this.validationContext);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a validator where a non‑null parent schema is supplied
        validatorWithParent = new TestValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorTypeCode, mockValidationContext);
        // Create a validator where the parent schema is null
        validatorWithoutParent = new TestValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, null, mockValidatorTypeCode, mockValidationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks between test methods to avoid cross‑test interference
        reset(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockValidationContext, mockValidatorTypeCode, mockParentSchema);
    }

    // -----------------------------------------------------------------------
    @Test
    void getParentSchema_returnsSuppliedParentSchema() {
        // Act
        JsonSchema result = validatorWithParent.getParentSchema();
        // Assert
        assertNotNull(result, "Parent schema should not be null when supplied via constructor");
        assertSame(mockParentSchema, result, "The returned parent schema must be the exact instance supplied to the constructor");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentSupplied() {
        // Act
        JsonSchema result = validatorWithoutParent.getParentSchema();
        // Assert
        assertNull(result, "Parent schema should be null when the constructor receives a null argument");
    }

    @Test
    @DisplayName("Consistency Across Calls – repeated getParentSchema() returns same instance")
    void getParentSchema_isConsistentAcrossMultipleCalls() {
        JsonSchema first = validatorWithParent.getParentSchema();
        JsonSchema second = validatorWithParent.getParentSchema();
        assertSame(first, second, "Multiple invocations must return the identical parent schema instance");
    }

    @Test
    @DisplayName("Different JsonSchema Implementations – custom instance is returned unchanged")
    void getParentSchema_returnsCustomJsonSchemaImplementation() {
        // Create a custom (non‑mock) JsonSchema implementation.
        JsonSchema customSchema = new JsonSchema() {
        };
        TestValidator customValidator = new TestValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, customSchema, mockValidatorTypeCode, mockValidationContext);
        JsonSchema result = customValidator.getParentSchema();
        assertSame(customSchema, result, "The getter must return the exact custom JsonSchema instance supplied at construction");
    }

    @Test
    @DisplayName("After Sub‑Schema Fetch – parent schema remains unchanged")
    void getParentSchema_unchangedAfterFetchSubSchemaNode() {
        // The fetchSubSchemaNode method may return null or a mock; we are only
        // interested in confirming that the parent schema field is not altered.
        JsonSchema fetched = validatorWithParent.invokeFetchSubSchemaNode();
        // The fetched schema is not relevant for this test; it may be null.
        // Verify that the original parent schema is still the same instance.
        JsonSchema after = validatorWithParent.getParentSchema();
        assertSame(mockParentSchema, after, "Parent schema must stay unchanged after invoking fetchSubSchemaNode()");
    }

    @Test
    @DisplayName("Thread‑Safety – concurrent calls return the same instance")
    void getParentSchema_isThreadSafe() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<JsonSchema>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> validatorWithParent.getParentSchema());
        }
        List<Future<JsonSchema>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate in time");
        // Collect all returned instances
        List<JsonSchema> results = futures.stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        // All must be the same reference
        JsonSchema first = results.get(0);
        for (JsonSchema schema : results) {
            assertSame(first, schema, "All threads must receive the identical parent schema instance");
        }
    }

    @Test
    @DisplayName("Null After Reset – setter changes internal state to null")
    void getParentSchema_reflectsExplicitResetToNull() {
        // Verify initial state is non‑null
        assertNotNull(validatorWithParent.getParentSchema(), "Pre‑condition: parent schema should be non‑null before reset");
        // Reset the protected field to null via the helper setter
        validatorWithParent.setParentSchema(null);
        // Now the getter must return null
        assertNull(validatorWithParent.getParentSchema(), "After resetting the internal parentSchema to null, getter must return null");
    }
}
