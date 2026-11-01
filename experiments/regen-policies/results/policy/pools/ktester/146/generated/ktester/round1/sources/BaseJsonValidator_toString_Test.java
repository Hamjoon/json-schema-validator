package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_toString_Test {

    // ----- Mocks for constructor dependencies ---------------------------------
    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidatorTypeCode validatorType;

    @Mock
    ValidationContext validationContext;

    // ----- Instance under test ------------------------------------------------
    private TestValidator validator;

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} required for instantiation.
     * Only {@link #validate(ExecutionContext, JsonNode)} is abstract; we return an
     * empty set because the validation logic is irrelevant for the {@code toString()}
     * test.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    // -------------------------------------------------------------------------
    private static class Scenario {

        final String description;

        // value returned by getName(-1)
        final String getNameReturn;

        // exception thrown by getName(-1)
        final Class<? extends Throwable> getNameException;

        // true → set internal field to null
        final boolean evaluationPathNull;

        // exception thrown by getEvaluationPath()
        final Class<? extends Throwable> evaluationPathException;

        // expected String result (null allowed)
        final String expectedResult;

        // expected exception from toString()
        final Class<? extends Throwable> expectedException;

        Scenario(String description, String getNameReturn, Class<? extends Throwable> getNameException, boolean evaluationPathNull, Class<? extends Throwable> evaluationPathException, String expectedResult, Class<? extends Throwable> expectedException) {
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

    @BeforeEach
    void setupBeforeEach() {
        // Stub the evaluationPath to return a known name for index -1.
        when(evaluationPath.getName(-1)).thenReturn("expectedPath");
        // Instantiate the concrete validator with the mocked collaborators.
        validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        // By default the evaluationPath mock returns a deterministic value.
        when(evaluationPath.getName(-1)).thenReturn("default");
        validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        // Reset all mocks to avoid cross‑test contamination.
        reset(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
    }

    // -------------------------------------------------------------------------
    static Stream<Scenario> scenarios() {
        return Stream.of(// ----- evaluation path handling ------------------------------------------------
        new Scenario("last segment of a multi‑segment path", "leaf", null, false, null, "leaf", null), new Scenario("single‑segment path", "only", null, false, null, "only", null), new Scenario("empty path – returns empty string", "", null, false, null, "", null), new Scenario("null evaluationPath → NullPointerException", null, null, true, null, null, NullPointerException.class), // ----- edge and special cases -------------------------------------------------
        new Scenario("last segment is null → toString() returns null", null, null, false, null, null, null), new Scenario("getName(-1) throws IndexOutOfBoundsException", null, IndexOutOfBoundsException.class, false, null, null, IndexOutOfBoundsException.class), new Scenario("segment contains whitespace", "leaf with spaces", null, false, null, "leaf with spaces", null), new Scenario("unicode characters in segment", "叶子", null, false, null, "叶子", null), new Scenario("large number of segments (100) – last is seg99", "seg99", null, false, null, "seg99", null), // ----- getEvaluationPath() exception propagation -------------------------------
        new Scenario("getEvaluationPath() throws IllegalStateException", null, null, false, IllegalStateException.class, null, IllegalStateException.class), new Scenario("getEvaluationPath() throws RuntimeException", null, null, false, RuntimeException.class, null, RuntimeException.class));
    }

    // ----- Actual test ---------------------------------------------------------
    @Test
    void toString_returnsEvaluationPathName() {
        // Act
        String result = validator.toString();
        // Assert
        assertEquals("expectedPath", result, "BaseJsonValidator.toString() should delegate to getEvaluationPath().getName(-1)");
        // Verify that the delegation actually happened.
        verify(evaluationPath).getName(-1);
    }

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
        // default
        BaseJsonValidator underTest = validator;
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
            Throwable thrown = assertThrows(s.expectedException, underTest::toString, "Expected exception not thrown for scenario: " + s.description);
            // For completeness, ensure the exception type matches exactly.
            assertEquals(s.expectedException, thrown.getClass(), "Wrong exception type for scenario: " + s.description);
        } else {
            String result = underTest.toString();
            assertEquals(s.expectedResult, result, "Unexpected toString() result for scenario: " + s.description);
            // Verify delegation when we have a non‑null evaluationPath.
            if (!s.evaluationPathNull && s.evaluationPathException == null) {
                verify(evaluationPath, times(1)).getName(-1);
            }
        }
    }
}
