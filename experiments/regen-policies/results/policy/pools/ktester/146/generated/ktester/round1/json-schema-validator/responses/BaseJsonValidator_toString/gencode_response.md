```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for {@link BaseJsonValidator#toString()}.
 *
 * <p>The method is a thin delegation:
 * <pre>
 *     return getEvaluationPath().getName(-1);
 * </pre>
 * Therefore the tests focus on the contract of {@link JsonNodePath#getName(int)} and
 * on the behaviour when {@code getEvaluationPath()} itself is {@code null} or throws.
 *
 * <p>Each test case from the supplied JSON is represented by a {@link Scenario}
 * instance and exercised by a single parameterised test method.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_toString_Test {

    // ----- Mocks for constructor dependencies ---------------------------------
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;          // mock for the normal path
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;
    @Mock ValidatorTypeCode validatorType;
    @Mock ValidationContext validationContext;

    // ----- Instance under test -------------------------------------------------
    private TestValidator validator;   // concrete subclass used for testing

    // ----- Minimal concrete subclass -----------------------------------------
    /**
     * Minimal concrete subclass of {@link BaseJsonValidator}. The {@code validate}
     * method is irrelevant for {@code toString()} and therefore returns an empty set.
     */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(SchemaLocation schemaLocation,
                     JsonNodePath evaluationPath,
                     JsonNode schemaNode,
                     JsonSchema parentSchema,
                     ValidatorTypeCode validatorType,
                     ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode,
                  parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    // ----- Test lifecycle -----------------------------------------------------
    @BeforeEach
    void setUp() {
        // By default the evaluationPath mock returns a deterministic value.
        when(evaluationPath.getName(-1)).thenReturn("default");

        validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext);
    }

    @AfterEach
    void tearDown() {
        // Reset all mocks to avoid cross‑test contamination.
        reset(schemaLocation, evaluationPath, schemaNode,
                parentSchema, validatorType, validationContext);
    }

    // -------------------------------------------------------------------------
    //  Scenario definition used by the parameterised test
    // -------------------------------------------------------------------------
    private static class Scenario {
        final String description;
        final String getNameReturn;                // value returned by getName(-1)
        final Class<? extends Throwable> getNameException; // exception thrown by getName(-1)
        final boolean evaluationPathNull;           // true → set internal field to null
        final Class<? extends Throwable> evaluationPathException; // exception thrown by getEvaluationPath()
        final String expectedResult;               // expected String result (null allowed)
        final Class<? extends Throwable> expectedException; // expected exception from toString()

        Scenario(String description,
                 String getNameReturn,
                 Class<? extends Throwable> getNameException,
                 boolean evaluationPathNull,
                 Class<? extends Throwable> evaluationPathException,
                 String expectedResult,
                 Class<? extends Throwable> expectedException) {
            this.description = description;
            this.getNameReturn = getNameReturn;
            this.getNameException = getNameException;
            this.evaluationPathNull = evaluationPathNull;
            this.evaluationPathException = evaluationPathException;
            this.expectedResult = expectedResult;
            this.expectedException = expectedException;
        }

        @Override
        public String toString() {
            // JUnit displays this in the test name – keep it short but informative.
            return description;
        }
    }

    // -------------------------------------------------------------------------
    //  Provide all scenarios extracted from the JSON specification
    // -------------------------------------------------------------------------
    static Stream<Scenario> scenarios() {
        return Stream.of(
                // ----- evaluation path handling ------------------------------------------------
                new Scenario(
                        "last segment of a multi‑segment path",
                        "leaf", null, false, null,
                        "leaf", null),
                new Scenario(
                        "single‑segment path",
                        "only", null, false, null,
                        "only", null),
                new Scenario(
                        "empty path – returns empty string",
                        "", null, false, null,
                        "", null),
                new Scenario(
                        "null evaluationPath → NullPointerException",
                        null, null, true, null,
                        null, NullPointerException.class),

                // ----- edge and special cases -------------------------------------------------
                new Scenario(
                        "last segment is null → toString() returns null",
                        null, null, false, null,
                        null, null),
                new Scenario(
                        "getName(-1) throws IndexOutOfBoundsException",
                        null, IndexOutOfBoundsException.class, false, null,
                        null, IndexOutOfBoundsException.class),
                new Scenario(
                        "segment contains whitespace",
                        "leaf with spaces", null, false, null,
                        "leaf with spaces", null),
                new Scenario(
                        "unicode characters in segment",
                        "叶子", null, false, null,
                        "叶子", null),
                new Scenario(
                        "large number of segments (100) – last is seg99",
                        "seg99", null, false, null,
                        "seg99", null),

                // ----- getEvaluationPath() exception propagation -------------------------------
                new Scenario(
                        "getEvaluationPath() throws IllegalStateException",
                        null, null, false, IllegalStateException.class,
                        null, IllegalStateException.class),
                new Scenario(
                        "getEvaluationPath() throws RuntimeException",
                        null, null, false, RuntimeException.class,
                        null, RuntimeException.class)
        );
    }

    // -------------------------------------------------------------------------
    //  Parameterised test that executes every scenario
    // -------------------------------------------------------------------------
    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void toString_behaviour(Scenario s) throws Exception {
        // -----------------------------------------------------------------
        //  1. Configure the mock for getName(-1) if required
        // -----------------------------------------------------------------
        if (s.getNameException != null) {
            when(evaluationPath.getName(-1)).thenThrow(s.getNameException);
        } else if (s.getNameReturn != null || s.getNameReturn == null) {
            // The explicit null case is important for the "null segment" test.
            when(evaluationPath.getName(-1)).thenReturn(s.getNameReturn);
        }

        // -----------------------------------------------------------------
        //  2. Prepare the validator instance according to the scenario
        // -----------------------------------------------------------------
        BaseJsonValidator underTest = validator; // default

        if (s.evaluationPathNull) {
            // Use reflection to set the internal evaluationPath field to null.
            Field field = BaseJsonValidator.class.getDeclaredField("evaluationPath");
            field.setAccessible(true);
            field.set(underTest, null);
        }

        if (s.evaluationPathException != null) {
            // Spy the validator and force getEvaluationPath() to throw.
            underTest = spy(validator);
            doThrow(s.evaluationPathException).when(underTest).getEvaluationPath();
        }

        // -----------------------------------------------------------------
        //  3. Execute and verify
        // -----------------------------------------------------------------
        if (s.expectedException != null) {
            Throwable thrown = assertThrows(s.expectedException,
                    underTest::toString,
                    "Expected exception not thrown for scenario: " + s.description);
            // For completeness, ensure the exception type matches exactly.
            assertEquals(s.expectedException, thrown.getClass(),
                    "Wrong exception type for scenario: " + s.description);
        } else {
            String result = underTest.toString();
            assertEquals(s.expectedResult, result,
                    "Unexpected toString() result for scenario: " + s.description);
            // Verify delegation when we have a non‑null evaluationPath.
            if (!s.evaluationPathNull && s.evaluationPathException == null) {
                verify(evaluationPath, times(1)).getName(-1);
            }
        }
    }
}
```