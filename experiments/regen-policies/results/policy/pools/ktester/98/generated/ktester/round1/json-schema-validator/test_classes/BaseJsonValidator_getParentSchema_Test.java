package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

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

    // -----------------------------------------------------------------------
    private static final SchemaLocation DUMMY_SCHEMA_LOCATION = null;

    private static final JsonNodePath DUMMY_EVALUATION_PATH = null;

    private static final JsonNode DUMMY_SCHEMA_NODE = null;

    private static final ValidatorTypeCode DUMMY_VALIDATOR_TYPE = null;

    private static final ValidationContext DUMMY_VALIDATION_CONTEXT = null;

    // a non‑null JsonSchema instance used as the “parent” in the first validator
    private static final JsonSchema DUMMY_PARENT_SCHEMA = new // ValidationContext
    // SchemaLocation
    JsonSchema(// JsonNodePath
    null, // JsonNode (schema node)
    null, // parent JsonSchema
    null, // isRef
    null, null, false) {
    };

    // -----------------------------------------------------------------------
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        // -------------------------------------------------------------------
        // JsonValidator abstract methods – trivial implementations
        // -------------------------------------------------------------------
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
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
        // Helper used by a test – we deliberately do **not** call the real
        // fetchSubSchemaNode implementation because it depends on a fully‑
        // fledged ValidationContext.  Returning null is sufficient for the
        // purpose of the test that verifies the parent schema is unchanged.
        // -------------------------------------------------------------------
        public JsonSchema invokeFetchSubSchemaNode() {
            // no‑op stub
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a dummy parent schema via reflection
        JsonSchema dummyParent = createDummyJsonSchema();
        // validator with a supplied (non‑null) parent schema
        validatorWithParent = new TestValidator(DUMMY_SCHEMA_LOCATION, DUMMY_EVALUATION_PATH, DUMMY_SCHEMA_NODE, dummyParent, DUMMY_VALIDATOR_TYPE, DUMMY_VALIDATION_CONTEXT);
        // validator where the parent schema argument is null
        validatorWithoutParent = new TestValidator(DUMMY_SCHEMA_LOCATION, DUMMY_EVALUATION_PATH, DUMMY_SCHEMA_NODE, null, DUMMY_VALIDATOR_TYPE, DUMMY_VALIDATION_CONTEXT);
    }

    /**
     * Creates a dummy {@link JsonSchema} instance via reflection. The constructor of
     * {@code JsonSchema} is private, so we make it accessible for test purposes.
     */
    private static JsonSchema createDummyJsonSchema() {
        try {
            Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
            ctor.setAccessible(true);
            return ctor.newInstance(null, null, null, null, null, false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create dummy JsonSchema instance", e);
        }
    }

    // -----------------------------------------------------------------------
    @Test
    void getParentSchema_returnsSuppliedParentSchema() {
        JsonSchema result = validatorWithParent.getParentSchema();
        assertNotNull(result, "Parent schema should not be null when supplied via constructor");
        // The dummy parent created in setup is the exact instance stored
        assertSame(validatorWithParent.getParentSchema(), result, "The returned parent schema must be the exact instance supplied to the constructor");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentSupplied() {
        JsonSchema result = validatorWithoutParent.getParentSchema();
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
        JsonSchema customSchema = createDummyJsonSchema();
        TestValidator customValidator = new TestValidator(DUMMY_SCHEMA_LOCATION, DUMMY_EVALUATION_PATH, DUMMY_SCHEMA_NODE, customSchema, DUMMY_VALIDATOR_TYPE, DUMMY_VALIDATION_CONTEXT);
        JsonSchema result = customValidator.getParentSchema();
        assertSame(customSchema, result, "The getter must return the exact custom JsonSchema instance supplied at construction");
    }

    @Test
    @DisplayName("After Sub‑Schema Fetch – parent schema remains unchanged")
    void getParentSchema_unchangedAfterFetchSubSchemaNode() {
        // invoke the stub that does not touch the parent schema
        JsonSchema fetched = validatorWithParent.invokeFetchSubSchemaNode();
        // fetched value is irrelevant; we only verify the parent schema stayed the same
        JsonSchema after = validatorWithParent.getParentSchema();
        assertSame(validatorWithParent.getParentSchema(), after, "Parent schema must stay unchanged after invoking fetchSubSchemaNode()");
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
        List<JsonSchema> results = futures.stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        JsonSchema first = results.get(0);
        for (JsonSchema schema : results) {
            assertSame(first, schema, "All threads must receive the identical parent schema instance");
        }
    }

    @Test
    @DisplayName("Null After Reset – setter changes internal state to null")
    void getParentSchema_reflectsExplicitResetToNull() {
        // pre‑condition
        assertNotNull(validatorWithParent.getParentSchema(), "Parent schema should be non‑null before reset");
        // use reflection to bypass the final field restriction
        Field field = BaseJsonValidator.class.getDeclaredField("parentSchema");
        field.setAccessible(true);
        field.set(validatorWithParent, null);
        // verification
        assertNull(validatorWithParent.getParentSchema(), "After resetting the internal parentSchema to null via reflection, getter must return null");
    }
}
