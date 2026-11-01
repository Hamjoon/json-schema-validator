```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tests for {@link BaseJsonValidator#toString()}.
 *
 * The {@code toString()} implementation simply delegates to
 * {@code getEvaluationPath().getName(-1)}.  The test therefore verifies that the
 * returned string matches the value provided by a mocked {@link JsonNodePath}
 * instance.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_toString_Test {

    // -----------------------------------------------------------------------
    // Mocks for the constructor parameters of BaseJsonValidator
    // -----------------------------------------------------------------------
    @Mock
    private SchemaLocation schemaLocation;

    @Mock
    private JsonNodePath evaluationPath;   // will be stubbed to return a name

    @Mock
    private JsonNode schemaNode;

    @Mock
    private JsonSchema parentSchema;

    @Mock
    private ValidatorTypeCode validatorType;

    @Mock
    private ValidationContext validationContext;

    // -----------------------------------------------------------------------
    // The object under test – a real BaseJsonValidator instance that calls real
    // methods (including toString) but whose constructor arguments are mocked.
    // -----------------------------------------------------------------------
    private BaseJsonValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the evaluationPath to return a predictable name for the last segment.
        when(evaluationPath.getName(-1)).thenReturn("expectedPathSegment");

        // Create a partial mock that calls real methods (including toString) while
        // using the mocked constructor arguments.
        validator = mock(BaseJsonValidator.class,
                withSettings()
                        .useConstructor(schemaLocation, evaluationPath, schemaNode,
                                parentSchema, validatorType, validationContext)
                        .defaultAnswer(CALLS_REAL_METHODS));
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void toString_returnsLastEvaluationPathName() {
        // GIVEN – the evaluationPath mock is already configured in @BeforeEach

        // WHEN
        String result = validator.toString();

        // THEN
        assertEquals("expectedPathSegment", result,
                "BaseJsonValidator.toString() should return the name of the last segment of the evaluation path");
    }
}
```