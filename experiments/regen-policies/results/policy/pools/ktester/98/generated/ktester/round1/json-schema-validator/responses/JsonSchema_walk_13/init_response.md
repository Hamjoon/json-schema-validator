```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, OutputFormat, boolean)}.
 *
 * The tests focus on the public {@code walk} method and verify that the supplied
 * {@link OutputFormat} is invoked with the {@link ValidationResult} produced by
 * the schema evaluation.
 *
 * No private members of {@link JsonSchema} or its dependencies are accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_13_Test {

    /** Factory used to create {@link JsonSchema} instances. */
    private JsonSchemaFactory factory;

    /** Simple schema that accepts any JSON object. */
    private JsonSchema schema;

    /** Execution context used for the walk operation. */
    private ExecutionContext executionContext;

    /** Input format for JSON strings. */
    private static final InputFormat INPUT_FORMAT = InputFormat.JSON;

    /** Sample JSON input that conforms to the simple schema. */
    private static final String VALID_JSON = "{\"name\":\"John\"}";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a factory for the 2020‑12 draft (the most recent at the time of writing).
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);

        // Very small schema – just declares that the root must be an object.
        String schemaData = "{\"type\":\"object\"}";
        schema = factory.getSchema(schemaData);

        // Fresh execution context for each test.
        executionContext = new ExecutionContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the objects are garbage‑collected.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Verifies that {@code walk} forwards the {@link ValidationResult} to the
     * supplied {@link OutputFormat} and returns the value produced by the format.
     */
    @Test
    void walkShouldDelegateToOutputFormat_andReturnItsResult() {
        // -------------------------------------------------------
        // given: a mock OutputFormat that simply returns the ValidationResult it receives
        // -------------------------------------------------------
        @SuppressWarnings("unchecked")
        OutputFormat<ValidationResult> outputFormat = mock(OutputFormat.class);
        when(outputFormat.apply(any(ValidationResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // -------------------------------------------------------
        // when: the walk method is invoked with validation enabled
        // -------------------------------------------------------
        ValidationResult result = schema.walk(executionContext,
                                              VALID_JSON,
                                              INPUT_FORMAT,
                                              outputFormat,
                                              true);

        // -------------------------------------------------------
        // then: the result is not null, contains no validation messages,
        //       and the OutputFormat was called exactly once
        // -------------------------------------------------------
        assertNotNull(result, "walk should return a non‑null ValidationResult");
        assertTrue(result.getMessages().isEmpty(),
                   "The simple schema should produce no validation messages for valid JSON");
        verify(outputFormat, times(1)).apply(any(ValidationResult.class));
    }

    /**
     * Checks that when validation is disabled, the schema still walks the tree
     * and the OutputFormat receives a {@link ValidationResult} with an empty
     * message set.
     */
    @Test
    void walkWithoutValidationShouldStillReturnEmptyResult() {
        @SuppressWarnings("unchecked")
        OutputFormat<ValidationResult> outputFormat = mock(OutputFormat.class);
        when(outputFormat.apply(any(ValidationResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ValidationResult result = schema.walk(executionContext,
                                              VALID_JSON,
                                              INPUT_FORMAT,
                                              outputFormat,
                                              false);

        assertNotNull(result, "Result must never be null");
        assertTrue(result.getMessages().isEmpty(),
                   "When validation is disabled the result should contain no messages");
        verify(outputFormat, times(1)).apply(any(ValidationResult.class));
    }
}
```