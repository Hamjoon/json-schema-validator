**Test class for `BaseJsonValidator#fetchSubSchemaNode(ValidationContext)`**

```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit‑tests for {@link BaseJsonValidator#fetchSubSchemaNode(ValidationContext)}.
 *
 * <p>The method has only two observable behaviours that can be verified without
 * relying on the private static {@code obtainSubSchemaNode} implementation:
 *
 * <ul>
 *   <li>If {@code suppressSubSchemaRetrieval} is {@code true} the method must
 *       short‑circuit and return {@code null}.</li>
 *   <li>If the flag is {@code false} the method delegates to the private static
 *       helper.  In the real library this helper creates a {@link JsonSchema}
 *       from {@code schemaNode} and the supplied {@link ValidationContext}.
 *       For the purpose of these unit‑tests we verify the delegation contract
 *       by exercising a few realistic scenarios – a non‑null {@code schemaNode},
 *       a {@code null} {@code schemaNode}, and the propagation of unchecked
 *       exceptions that the helper might throw.</li>
 * </ul>
 *
 * <p>Because {@code obtainSubSchemaNode} is private static we cannot mock it
 * directly with plain Mockito.  Instead we rely on reflection to inject the
 * desired state into the validator (e.g. {@code schemaNode = null}) and we
 * allow the real helper to run for the happy‑path scenarios.  For the
 * exception‑propagation scenarios we use Mockito’s {@code MockedStatic} in
 * combination with reflection to replace the private static method with a
 * stub that throws the required exception.  This technique keeps the test
 * class self‑contained and does not require any external power‑mocking
 * libraries.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_fetchSubSchemaNode_Test {

    /* -------------------------------------------------------------
     * Minimal concrete implementation of the abstract BaseJsonValidator.
     * ------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema,
                  validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // Validation is out of scope for these tests.
            return Collections.emptySet();
        }
    }

    /* -------------------------------------------------------------
     * Mocks used by every test case.
     * ------------------------------------------------------------- */
    @Mock private JsonNode mockSchemaNode;               // can be an ObjectNode in some tests
    @Mock private JsonSchema mockParentSchema;
    @Mock private ValidatorTypeCode mockValidatorType;
    @Mock private ValidationContext mockValidationContext;

    /* -------------------------------------------------------------
     * Helper to set the protected field {@code schemaNode} via reflection.
     * ------------------------------------------------------------- */
    private static void setSchemaNode(BaseJsonValidator validator, JsonNode node) {
        try {
            Field f = BaseJsonValidator.class.getDeclaredField("schemaNode");
            f.setAccessible(true);
            f.set(validator, node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* -------------------------------------------------------------
     * Helper to set the protected field {@code suppressSubSchemaRetrieval}
     * via reflection (used only for sanity‑checking that the constructor
     * really stores the value we pass).
     * ------------------------------------------------------------- */
    private static void setSuppressFlag(BaseJsonValidator validator, boolean flag) {
        try {
            Field f = BaseJsonValidator.class.getDeclaredField("suppressSubSchemaRetrieval");
            f.setAccessible(true);
            f.setBoolean(validator, flag);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* -------------------------------------------------------------
     * 1. suppressSubSchemaRetrieval = true – always returns null.
     * ------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed_andNonNullContext() {
        TestValidator validator = new TestValidator(
                "unusedPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                true);                                 // suppress flag = true

        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNull(result, "With suppressSubSchemaRetrieval=true the method must return null");
    }

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed_andNullContext() {
        TestValidator validator = new TestValidator(
                "unusedPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                true);                                 // suppress flag = true

        JsonSchema result = validator.fetchSubSchemaNode(null);
        assertNull(result, "Even with a null ValidationContext the method must return null");
    }

    /* -------------------------------------------------------------
     * 2. suppressSubSchemaRetrieval = false – normal delegation.
     * ------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_delegatesWhenNotSuppressed_andValidSchemaNode() {
        // Arrange – a validator that does NOT suppress retrieval and has a
        // non‑null schema node.  The real obtainSubSchemaNode will create a
        // JsonSchema instance; we only assert that the result is non‑null.
        TestValidator validator = new TestValidator(
                "unusedPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);                                // suppress flag = false

        // Minimal stubbing of ValidationContext – the private helper only needs
        // the factories to be non‑null.  Returning null works for the current
        // implementation because the helper guards against NPEs.
        when(mockValidationContext.getConfig()).thenReturn(null);
        when(mockValidationContext.getMetaSchema()).thenReturn(null);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(null);
        when(mockValidationContext.getUriFactory()).thenReturn(null);
        when(mockValidationContext.getUrnFactory()).thenReturn(null);

        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNotNull(result, "When not suppressed a JsonSchema instance should be returned");
    }

    /* -------------------------------------------------------------
     * 3. schemaNode is null – the private helper returns null, therefore
     *    fetchSubSchemaNode must also return null.
     * ------------------------------------------------------------- */
    @Test
    void fetchSubSchemaNode_returnsNullWhenSchemaNodeIsNull() throws Exception {
        TestValidator validator = new TestValidator(
                "unusedPath",
                null,                               // <-- null schemaNode at construction time
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);                             // suppress flag = false

        // Additional safety – ensure the field really is null (reflection)
        setSchemaNode(validator, null);

        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);
        assertNull(result, "With a null schemaNode the helper returns null, so fetchSubSchemaNode must return null");
    }

    /* -------------------------------------------------------------
     * 4. Propagation of unchecked exceptions thrown by obtainSubSchemaNode.
     *    We replace the private static method with a stub that throws the
     *    required exception using Mockito's MockedStatic and reflection.
     * ------------------------------------------------------------- */
    private static void mockObtainSubSchemaNodeToThrow(Class<? extends Throwable> toThrow) {
        try {
            // Obtain a Method reference to the private static method.
            var method = BaseJsonValidator.class.getDeclaredMethod(
                    "obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            method.setAccessible(true);

            // Use MockedStatic to replace the *class* and intercept the call.
            // Inside the lambda we invoke the method reflectively – this forces
            // Mockito to route the call through the mock, where we can then
            // throw the desired exception.
            try (var mocked = mockStatic(BaseJsonValidator.class)) {
                mocked.when(() -> {
                    // The following reflective call mirrors the real invocation.
                    return (JsonSchema) method.invoke(null, any(JsonNode.class), any(ValidationContext.class));
                }).thenAnswer(invocation -> {
                    throw toThrow.getDeclaredConstructor(String.class)
                                 .newInstance("forced by test");
                });

                // Store the mocked static in a thread‑local so the test method
                // can use it later.
                MockedObtainSubSchemaHolder.mocked = mocked;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Holder for the currently active MockedStatic – used to keep the scope
     *  of the mock alive for the duration of a single test. */
    private static class MockedObtainSubSchemaHolder {
        static MockedStatic<BaseJsonValidator> mocked;
    }

    @ParameterizedTest(name = "{index} => exception={0}")
    @MethodSource("exceptionProvider")
    void fetchSubSchemaNode_propagatesUncheckedExceptions(Class<? extends Throwable> exceptionClass) throws Exception {
        // Arrange – validator with suppress flag = false.
        TestValidator validator = new TestValidator(
                "unusedPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);

        // Mock the private static method to throw the requested exception.
        try (var mocked = mockStatic(BaseJsonValidator.class)) {
            // Use reflection to obtain the private method.
            var privateMethod = BaseJsonValidator.class.getDeclaredMethod(
                    "obtainSubSchemaNode", JsonNode.class, ValidationContext.class);
            privateMethod.setAccessible(true);

            // Configure the static mock.
            mocked.when(() -> {
                // The lambda mirrors the exact call made inside fetchSubSchemaNode.
                return (JsonSchema) privateMethod.invoke(null, any(JsonNode.class), any(ValidationContext.class));
            }).thenAnswer(invocation -> {
               