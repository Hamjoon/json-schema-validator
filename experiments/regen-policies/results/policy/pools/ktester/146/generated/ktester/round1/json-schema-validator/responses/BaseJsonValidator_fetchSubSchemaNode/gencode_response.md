```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link BaseJsonValidator#fetchSubSchemaNode(ValidationContext)}.
 *
 * <p>The test suite covers:</p>
 * <ul>
 *   <li>Suppressed retrieval (should always return {@code null}).</li>
 *   <li>Normal retrieval (delegates to the private static {@code obtainSubSchemaNode} method).</li>
 *   <li>Behaviour when {@code ValidationContext} is {@code null}.</li>
 *   <li>Repeated invocations returning the same instance.</li>
 *   <li>Propagation of exceptions thrown by {@code obtainSubSchemaNode}.</li>
 *   <li>Verification that the private static helper receives the correct arguments.</li>
 * </ul>
 *
 * <p>Because {@code obtainSubSchemaNode} is a private static method we use Mockito's
 * {@code mockStatic} together with reflection to stub or verify its behaviour.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_fetchSubSchemaNode_Test {

    /** Shared objects used by several tests. */
    private static SchemaLocation schemaLocation;
    private static JsonNodePath evaluationPath;
    private static JsonNode emptySchemaNode;

    @Mock
    private ValidationContext mockValidationContext;   // generic mock for most cases

    @Mock
    private ValidationContext emptyValidationContext; // represents a freshly‑constructed context

    @Mock
    private ValidatorTypeCode mockValidatorType;       // not used by the method under test

    @BeforeAll
    static void setupBeforeAll() {
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/schema");
        schemaLocation = new SchemaLocation(absoluteIri);
        evaluationPath = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
        emptySchemaNode = new ObjectNode(JsonNodeFactory.instance);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment
    }

    /* --------------------------------------------------------------------- *
     *  Helper concrete subclass – provides the minimal implementation
     *  required to instantiate the abstract BaseJsonValidator.
     * --------------------------------------------------------------------- */
    static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaLocation,
                  evaluationPath,
                  schemaNode,
                  parentSchema,
                  validatorType,
                  validatorType,
                  validationContext,
                  suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        protected String getNodeFieldType() {
            return "test";
        }
    }

    /* --------------------------------------------------------------------- *
     *  1. Suppressed retrieval – always returns null
     * --------------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_shouldReturnNullWhenSuppressed() {
        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,
                mockValidatorType,
                mockValidationContext,
                true // suppressSubSchemaRetrieval = true
        );

        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNull(result, "When suppression is enabled the method must return null");
    }

    @Test
    void fetchSubSchemaNode_shouldReturnNullWhenSuppressedEvenIfContextIsNull() {
        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,
                mockValidatorType,
                null,
                true // suppressSubSchemaRetrieval = true
        );

        JsonSchema result = validator.fetchSubSchemaNode(null);
        assertNull(result, "Suppression overrides a null ValidationContext – result must be null");
    }

    /* --------------------------------------------------------------------- *
     *  2. Normal retrieval – delegate to obtainSubSchemaNode
     * --------------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_shouldReturnSchemaWhenNotSuppressed() {
        // a real ValidationContext is required for the private helper to work
        JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V201909);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        ValidationContext realContext = new ValidationContext(meta, factory, new SchemaValidatorsConfig());

        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,
                mockValidatorType,
                realContext,
                false // suppressSubSchemaRetrieval = false
        );

        JsonSchema result = validator.fetchSubSchemaNode(realContext);
        assertNotNull(result, "When suppression is disabled a JsonSchema instance must be returned");
    }

    @Test
    void fetchSubSchemaNode_withNullValidationContext_returnsSchema() {
        // Use a real (but empty) ValidationContext – the method must still call obtainSubSchemaNode
        JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V201909);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        ValidationContext realContext = new ValidationContext(meta, factory, new SchemaValidatorsConfig());

        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,
                mockValidatorType,
                realContext,
                false
        );

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
        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                customNode,
                null,
                mockValidatorType,
                mockValidationContext,
                false
        );

        // Mock the private static method via reflection + Mockito's mockStatic
        try (MockedStatic<BaseJsonValidator> mocked = mockStatic(BaseJsonValidator.class)) {
            // Use reflection to obtain a MethodHandle that we can call inside the stub
            Method privateMethod = BaseJsonValidator.class.getDeclaredMethod(
                    "obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            privateMethod.setAccessible(true);

            // Stub: whenever the private method is called, return the mock schema
            mocked.when(() -> privateMethod.invoke(null, any(JsonNode.class), any(ValidationContext.class)))
                  .thenAnswer(invocation -> {
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

        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,
                mockValidatorType,
                mockValidationContext,
                false
        );

        try (MockedStatic<BaseJsonValidator> mocked = mockStatic(BaseJsonValidator.class)) {
            Method m = BaseJsonValidator.class.getDeclaredMethod(
                    "obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            m.setAccessible(true);
            mocked.when(() -> m.invoke(null, any(JsonNode.class), any(ValidationContext.class)))
                  .thenReturn(mockSchema);

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
        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,
                mockValidatorType,
                mockValidationContext,
                false
        );

        try (MockedStatic<BaseJsonValidator> mocked = mockStatic(BaseJsonValidator.class)) {
            Method m = BaseJsonValidator.class.getDeclaredMethod(
                    "obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            m.setAccessible(true);
            mocked.when(() -> m.invoke(null, any(JsonNode.class), any(ValidationContext.class)))
                  .thenThrow(new NullPointerException("simulated NPE"));

            assertThrows(NullPointerException.class,
                    () -> validator.fetchSubSchemaNode(mockValidationContext),
                    "NullPointerException must propagate unchanged");
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
    }

    @Test
    void fetchSubSchemaNode_propagatesIllegalArgumentExceptionFromObtainSubSchemaNode() {
        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,
                mockValidatorType,
                mockValidationContext,
                false
        );

        try (MockedStatic<BaseJsonValidator> mocked = mockStatic(BaseJsonValidator.class)) {
            Method m = BaseJsonValidator.class.getDeclaredMethod(
                    "obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            m.setAccessible(true);
            mocked.when(() -> m.invoke(null, any(JsonNode.class), any(ValidationContext.class)))
                  .thenThrow(new IllegalArgumentException("invalid schema node"));

            assertThrows(IllegalArgumentException.class,
                    () -> validator.fetchSubSchemaNode(mockValidationContext),
                    "IllegalArgumentException must be propagated");
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
    }

    @Test
    void fetchSubSchemaNode_propagatesRuntimeExceptionFromObtainSubSchemaNode() {
        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,
                mockValidatorType,
                mockValidationContext,
                false
        );

        try (MockedStatic<BaseJsonValidator> mocked = mockStatic(BaseJsonValidator.class)) {
            Method m = BaseJsonValidator.class.getDeclaredMethod(
                    "obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            m.setAccessible(true);
            mocked.when(() -> m.invoke(null, any(JsonNode.class), any(ValidationContext.class)))
                  .thenThrow(new RuntimeException("unexpected failure"));

            assertThrows(RuntimeException.class,
                    () -> validator.fetchSubSchemaNode(mockValidationContext),
                    "RuntimeException must be propagated");
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
    }

    /* --------------------------------------------------------------------- *
     *  6. Suppression flag prevents any exception from being thrown
     * --------------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_suppressionPreventsExceptionFromObtainSubSchemaNode() {
        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,
                mockValidatorType,
                mockValidationContext,
                true // suppression = true
        );

        // Even though we would like obtainSubSchemaNode to throw, the method must never be called.
        // We verify this by mocking the static method and making it throw – the test should still pass.
        try (MockedStatic<BaseJsonValidator> mocked = mockStatic(BaseJsonValidator.class)) {
            Method m = BaseJsonValidator.class.getDeclaredMethod(
                    "obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            m.setAccessible(true);
            mocked.when(() -> m.invoke(null, any(JsonNode.class), any(ValidationContext.class)))
                  .thenThrow(new IllegalStateException("should not be called"));

            // No exception should be thrown because the flag short‑circuits the call.
            assertDoesNotThrow(() -> validator.fetchSubSchemaNode(mockValidationContext),
                    "When suppression is true, fetchSubSchemaNode must not invoke obtainSubSchemaNode");
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

        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                nonEmptyNode,
                null,
                mockValidatorType,
                mockValidationContext,
                true // suppressSubSchemaRetrieval = true
        );

        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNull(result, "Suppression must win over a valid schemaNode – result must be null");
    }
}
```