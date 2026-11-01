package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import com.networknt.schema.annotation.JsonNodeAnnotation;
import com.networknt.schema.annotation.Annotations;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_putAnnotation_Test {

    /**
     * The concrete validator used for the test.
     */
    private TestValidator validator;

    /**
     * Mocked collaborators required by the {@code BaseJsonValidator} constructor.
     */
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

    /**
     * Mocked execution context used in the test.
     */
    @Mock
    private ExecutionContext mockExecutionContext;

    /**
     * Mocked annotations container returned by {@code ExecutionContext#getAnnotations()}.
     */
    @Mock
    private Annotations mockAnnotations;

    /**
     * Stub objects that represent the state of the validator.
     */
    private final SchemaLocation schemaLocation = null;

    private final JsonNodePath evaluationPath = null;

    private final JsonNode schemaNode = null;

    private final JsonSchema parentSchema = null;

    private final ValidatorTypeCode validatorType = null;

    private final ValidationContext validationContext = null;

    /**
     * ExecutionContext that delegates to a test‑specific {@link Annotations} implementation.
     */
    private ExecutionContext executionContext;

    /**
     * Test‑specific {@link Annotations} that records the annotation that is put.
     */
    private TestAnnotations testAnnotations;

    class RecordingConsumer implements Consumer<JsonNodeAnnotation.Builder> {

        boolean called = false;

        @Override
        public void accept(JsonNodeAnnotation.Builder builder) {
            called = true;
        }
    }

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} that supplies only the
     * abstract members required for instantiation.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        /* The current version of {@code BaseJsonValidator} expects the four‑argument
         * validate method.  The implementation is a no‑op for the purpose of these tests. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            return Collections.emptySet();
        }
    }

    /**
     * Simple {@link Annotations} implementation that records the last annotation
     * passed to {@code put}.
     */
    private static class TestAnnotations extends Annotations {

        private JsonNodeAnnotation stored;

        @Override
        public void put(JsonNodeAnnotation annotation) {
            this.stored = annotation;
        }

        JsonNodeAnnotation getStored() {
            return stored;
        }
    }

    class RecordingConsumer implements Consumer<JsonNodeAnnotation.Builder> {

        boolean called = false;

        @Override
        public void accept(JsonNodeAnnotation.Builder builder) {
            called = true;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build the concrete validator instance.
        validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        // Prepare a TestAnnotations instance and inject it into a real ExecutionContext
        // via reflection (ExecutionContext holds the Annotations instance in a private field).
        testAnnotations = new TestAnnotations();
        // ValidationContext not needed for this test
        executionContext = new ExecutionContext(null);
        Field annField = ExecutionContext.class.getDeclaredField("annotations");
        annField.setAccessible(true);
        annField.set(executionContext, testAnnotations);
    }

    @Test
    void putAnnotation_shouldInvokeCustomizerAndStoreAnnotation() {
        // ------------------------------------------------------------
        // GIVEN – a customiser that records that it has been called.
        // ------------------------------------------------------------
        class RecordingConsumer implements Consumer<JsonNodeAnnotation.Builder> {

            boolean called = false;

            @Override
            public void accept(JsonNodeAnnotation.Builder builder) {
                called = true;
            }
        }
        RecordingConsumer customiser = new RecordingConsumer();
        // ------------------------------------------------------------
        // WHEN – the protected method is exercised via the concrete subclass.
        // ------------------------------------------------------------
        validator.putAnnotation(executionContext, customiser);
        // ------------------------------------------------------------
        // THEN – the customiser must have been called once.
        // ------------------------------------------------------------
        assertTrue(customiser.called, "Customizer should have been invoked");
        // ------------------------------------------------------------
        // AND – the built annotation must have been stored in the context.
        // ------------------------------------------------------------
        assertNotNull(testAnnotations.getStored(), "Annotations container should have received an annotation");
    }

    /**
     * Default case: the customiser does nothing.
     *
     * <p>We capture the {@link JsonNodeAnnotation} that is put into the
     * {@link Annotations} container and assert that its automatically populated
     * fields match the validator's state.
     */
    @Test
    void putAnnotation_Default_noCustomizerChanges() {
        // GIVEN – a no‑op customiser.
        Consumer<JsonNodeAnnotation.Builder> noOpCustomizer = builder -> {
            /* no‑op */
        };
        // WHEN – invoke the method.
        validator.putAnnotation(executionContext, noOpCustomizer);
        // THEN – capture the annotation that was stored.
        JsonNodeAnnotation stored = testAnnotations.getStored();
        assertNotNull(stored, "An annotation should have been stored");
        // Verify the automatically populated fields using reflection (the
        // JsonNodeAnnotation API is not part of the public contract in this
        // context).
        Assertions.assertAll(() -> {
            Field f = JsonNodeAnnotation.class.getDeclaredField("evaluationPath");
            f.setAccessible(true);
            Object value = f.get(stored);
            assertSame(evaluationPath, value, "evaluationPath should be the validator's evaluationPath");
        }, () -> {
            Field f = JsonNodeAnnotation.class.getDeclaredField("schemaLocation");
            f.setAccessible(true);
            Object value = f.get(stored);
            assertSame(schemaLocation, value, "schemaLocation should be the validator's schemaLocation");
        }, () -> {
            Field f = JsonNodeAnnotation.class.getDeclaredField("keyword");
            f.setAccessible(true);
            Object value = f.get(stored);
            assertEquals(validator.getKeyword(), value, "keyword should be the validator's keyword");
        }, () -> {
            // optional fields – we expect them to be null (or default)
            Field[] fields = JsonNodeAnnotation.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getName().equals("evaluationPath") || field.getName().equals("schemaLocation") || field.getName().equals("keyword")) {
                    // already checked
                    continue;
                }
                field.setAccessible(true);
                assertNull(field.get(stored), "Optional field '" + field.getName() + "' should be null by default");
            }
        });
    }

    /**
     * When a {@code null} {@link ExecutionContext} is supplied the method should
     * throw a {@link NullPointerException}.
     */
    @Test
    void putAnnotation_NullExecutionContext_throwsNPE() {
        // GIVEN – a no‑op customiser.
        Consumer<JsonNodeAnnotation.Builder> noOpCustomizer = builder -> {
        };
        // WHEN / THEN – NPE is expected.
        assertThrows(NullPointerException.class, () -> validator.putAnnotation(null, noOpCustomizer), "Calling putAnnotation with a null ExecutionContext should throw NPE");
    }
}
