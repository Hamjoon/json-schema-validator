package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import java.lang.reflect.Method;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_fetchSubSchemaNode_Test {

    /**
     * Common objects used by several tests.
     */
    private static SchemaLocation schemaLocation;

    private static JsonNodePath evaluationPath;

    private static JsonNode emptySchemaNode;

    @Mock
    private ValidationContext // used for the “suppressed” case
    mockValidationContext;

    @Mock
    private ValidatorTypeCode // can be any mock – its behaviour is not relevant
    mockValidatorType;

    @Mock
    private ValidationContext // represents a freshly‑constructed context
    emptyValidationContext;

    /**
     * Minimal concrete implementation BaseJsonValidator_fetchSubSchemaNode_Test{@link BaseJsonValidator}.
     * Only the abstract {@code validate} and {@code getNodeFieldType} methods are
     * implemented with trivial behaviour because they are not exercised by the
     * tests for {@code fetchSubSchemaNode}.
     */
    static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for these tests
            return Collections.emptySet();
        }

        @Override
        protected String getNodeFieldType() {
            // Arbitrary non‑null value – the method is required by the abstract class
            return "test";
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // a very small, valid schema location – the absolute IRI can be any string
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/schema");
        schemaLocation = new SchemaLocation(absoluteIri);
        // an empty evaluation path (the root BaseJsonValidator_fetchSubSchemaNode_Testthe schema)
        evaluationPath = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
        // an empty JSON object used as the schema node
        emptySchemaNode = new ObjectNode(JsonNodeFactory.instance);
    }

    /**
     * When the validator is constructed with {@code suppressSubSchemaRetrieval = true},
     * {@code fetchSubSchemaNode} must return {@code null} regardless BaseJsonValidator_fetchSubSchemaNode_Testthe validation context.
     */
    @Test
    void fetchSubSchemaNode_shouldReturnNullWhenSuppressed() {
        // ----------------------------------------------------------------------
        // GIVEN – a concrete validator with suppression enabled
        // ----------------------------------------------------------------------
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, emptySchemaNode, // parent schema – not needed for this test
        null, mockValidatorType, mockValidationContext, // suppressSubSchemaRetrieval = true
        true);
        // ----------------------------------------------------------------------
        // WHEN
        // ----------------------------------------------------------------------
        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        // ----------------------------------------------------------------------
        // THEN
        // ----------------------------------------------------------------------
        assertNull(result, "fetchSubSchemaNode should return null when suppression is enabled");
    }

    /**
     * When suppression is disabled the method should delegate to {@code obtainSubSchemaNode}
     * and return a non‑null {@link JsonSchema}.  A real {@link ValidationContext} is used
     * so that the private static helper can create a genuine {@link JsonSchema} instance.
     */
    @Test
    void fetchSubSchemaNode_shouldReturnSchemaWhenNotSuppressed() {
        // ----------------------------------------------------------------------
        // GIVEN – a real ValidationContext (required for the private helper)
        // ----------------------------------------------------------------------
        JsonMetaSchema metaSchema = JsonMetaSchema.getInstance(VersionFlag.V201909);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        ValidationContext realContext = new ValidationContext(metaSchema, factory, new SchemaValidatorsConfig());
        // ----------------------------------------------------------------------
        // AND – a concrete validator with suppression disabled
        // ----------------------------------------------------------------------
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, emptySchemaNode, // parent schema – not needed for this test
        null, // the validator type is irrelevant for this path
        mockValidatorType, realContext, // suppressSubSchemaRetrieval = false
        false);
        // ----------------------------------------------------------------------
        // WHEN
        // ----------------------------------------------------------------------
        JsonSchema result = validator.fetchSubSchemaNode(realContext);
        // ----------------------------------------------------------------------
        // THEN
        // ----------------------------------------------------------------------
        assertNotNull(result, "fetchSubSchemaNode should return a JsonSchema when suppression is disabled");
    }

    @Test
    void fetchSubSchemaNode_shouldReturnNullWhenSuppressedEvenIfContextIsNull() {
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, emptySchemaNode, null, mockValidatorType, null, // suppressSubSchemaRetrieval = true
        true);
        JsonSchema result = validator.fetchSubSchemaNode(null);
        assertNull(result, "Suppression overrides a null ValidationContext – result must be null");
    }

    @Test
    void fetchSubSchemaNode_withNullValidationContext_returnsSchema() {
        // Use a real (but empty) ValidationContext – the method must still call obtainSubSchemaNode
        JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V201909);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        ValidationContext realContext = new ValidationContext(meta, factory, new SchemaValidatorsConfig());
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, emptySchemaNode, null, mockValidatorType, realContext, false);
        // Pass null explicitly – the private helper should still be invoked
        JsonSchema result = validator.fetchSubSchemaNode(null);
        assertNotNull(result, "Even with a null ValidationContext the method must return a JsonSchema");
    }

    /* --------------------------------------------------------------------- *
     *  3. Verify that obtainSubSchemaNode is invoked with the exact arguments
     * --------------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_invokesObtainSubSchemaNodeWithCorrectArguments() throws Exception {
        JsonSchema mockSchema = mock(JsonSchema.class);
        // Prepare a validator with a distinct schemaNode so we can compare it later
        ObjectNode customNode = new ObjectNode(JsonNodeFactory.instance);
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, customNode, null, mockValidatorType, mockValidationContext, false);
        // Mock the private static method via reflection + Mockito's mockStatic
        try (MockedStatic<BaseJsonValidator> mocked = mockStatic(BaseJsonValidator.class)) {
            // Use reflection to obtain a MethodHandle that we can call inside the stub
            Method privateMethod = BaseJsonValidator.class.getDeclaredMethod("obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            privateMethod.setAccessible(true);
            // Stub: whenever the private method is called, return the mock schema
            mocked.when(() -> privateMethod.invoke(null, any(JsonNode.class), any(ValidationContext.class))).thenAnswer(invocation -> {
                JsonNode passedNode = (JsonNode) invocation.getArgument(0);
                ValidationContext passedCtx = (ValidationContext) invocation.getArgument(1);
                // Verify that the arguments are exactly what we expect
                assertSame(customNode, passedNode, "SchemaNode passed to obtainSubSchemaNode must be the validator's node");
                assertSame(mockValidationContext, passedCtx, "ValidationContext passed must be the one supplied to fetchSubSchemaNode");
                return mockSchema;
            });
            JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
            assertSame(mockSchema, result, "The result must be the stubbed JsonSchema instance");
        }
    }

    /* --------------------------------------------------------------------- *
     *  4. Repeated calls must return the same instance (no hidden state)
     * --------------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_multipleCallsReturnSameInstance() {
        JsonSchema mockSchema = mock(JsonSchema.class);
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, emptySchemaNode, null, mockValidatorType, mockValidationContext, false);
        try (MockedStatic<BaseJsonValidator> mocked = mockStatic(BaseJsonValidator.class)) {
            Method m = BaseJsonValidator.class.getDeclaredMethod("obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            m.setAccessible(true);
            mocked.when(() -> m.invoke(null, any(JsonNode.class), any(ValidationContext.class))).thenReturn(mockSchema);
            JsonSchema first = validator.fetchSubSchemaNode(mockValidationContext);
            JsonSchema second = validator.fetchSubSchemaNode(mockValidationContext);
            assertSame(first, second, "Both calls must return the identical JsonSchema instance");
        } catch (Exception e) {
            fail("Reflection failure: " + e);
        }
    }

    /* --------------------------------------------------------------------- *
     *  5. Propagation of exceptions thrown by obtainSubSchemaNode
     * --------------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_propagatesNullPointerExceptionFromObtainSubSchemaNode() {
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, emptySchemaNode, null, mockValidatorType, mockValidationContext, false);
        try (MockedStatic<BaseJsonValidator> mocked = mockStatic(BaseJsonValidator.class)) {
            Method m = BaseJsonValidator.class.getDeclaredMethod("obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            m.setAccessible(true);
            mocked.when(() -> m.invoke(null, any(JsonNode.class), any(ValidationContext.class))).thenThrow(new NullPointerException("simulated NPE"));
            assertThrows(NullPointerException.class, () -> validator.fetchSubSchemaNode(mockValidationContext), "NullPointerException must propagate unchanged");
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
    }

    @Test
    void fetchSubSchemaNode_propagatesIllegalArgumentExceptionFromObtainSubSchemaNode() {
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, emptySchemaNode, null, mockValidatorType, mockValidationContext, false);
        try (MockedStatic<BaseJsonValidator> mocked = mockStatic(BaseJsonValidator.class)) {
            Method m = BaseJsonValidator.class.getDeclaredMethod("obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            m.setAccessible(true);
            mocked.when(() -> m.invoke(null, any(JsonNode.class), any(ValidationContext.class))).thenThrow(new IllegalArgumentException("invalid schema node"));
            assertThrows(IllegalArgumentException.class, () -> validator.fetchSubSchemaNode(mockValidationContext), "IllegalArgumentException must be propagated");
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
    }

    @Test
    void fetchSubSchemaNode_propagatesRuntimeExceptionFromObtainSubSchemaNode() {
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, emptySchemaNode, null, mockValidatorType, mockValidationContext, false);
        try (MockedStatic<BaseJsonValidator> mocked = mockStatic(BaseJsonValidator.class)) {
            Method m = BaseJsonValidator.class.getDeclaredMethod("obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            m.setAccessible(true);
            mocked.when(() -> m.invoke(null, any(JsonNode.class), any(ValidationContext.class))).thenThrow(new RuntimeException("unexpected failure"));
            assertThrows(RuntimeException.class, () -> validator.fetchSubSchemaNode(mockValidationContext), "RuntimeException must be propagated");
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
    }

    /* --------------------------------------------------------------------- *
     *  6. Suppression flag prevents any exception from being thrown
     * --------------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_suppressionPreventsExceptionFromObtainSubSchemaNode() {
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, emptySchemaNode, null, mockValidatorType, mockValidationContext, // suppression = true
        true);
        // Even though we would like obtainSubSchemaNode to throw, the method must never be called.
        // We verify this by mocking the static method and making it throw – the test should still pass.
        try (MockedStatic<BaseJsonValidator> mocked = mockStatic(BaseJsonValidator.class)) {
            Method m = BaseJsonValidator.class.getDeclaredMethod("obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            m.setAccessible(true);
            mocked.when(() -> m.invoke(null, any(JsonNode.class), any(ValidationContext.class))).thenThrow(new IllegalStateException("should not be called"));
            // No exception should be thrown because the flag short‑circuits the call.
            assertDoesNotThrow(() -> validator.fetchSubSchemaNode(mockValidationContext), "When suppression is true, fetchSubSchemaNode must not invoke obtainSubSchemaNode");
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
    }

    /* --------------------------------------------------------------------- *
     *  7. Suppression flag overrides a non‑null schemaNode
     * --------------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_suppressedEvenWithValidSchemaNodeReturnsNull() {
        ObjectNode nonEmptyNode = new ObjectNode(JsonNodeFactory.instance);
        nonEmptyNode.put("type", "object");
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, nonEmptyNode, null, mockValidatorType, mockValidationContext, // suppressSubSchemaRetrieval = true
        true);
        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNull(result, "Suppression must win over a valid schemaNode – result must be null");
    }
}
