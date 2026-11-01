```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat)}.
 *
 * <p>The tests focus on the public {@code validate} overload that accepts a
 * {@link InputFormat} and an {@link OutputFormat}.  An {@link OutputFormat}
 * implementation is mocked so that we can verify the interaction without
 * depending on the concrete implementation details of the library.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_12_Test {

    /** Factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** Schema under test – created per‑test so that each test gets a fresh instance. */
    private JsonSchema schema;

    /** Simple JSON schema used for the happy‑path test (valid object). */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    /** Schema that requires a {@code name} property of type {@code string}. */
    private static final String REQUIRED_STRING_SCHEMA = "{\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": { \"name\": { \"type\": \"string\" } },\n"
            + "  \"required\": [\"name\"]\n"
            + "}";

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the factory once for the whole test class.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default create a schema that validates any JSON object.
        schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release globally.
    }

    /**
     * Verify that the {@code validate(String, InputFormat, OutputFormat)} method
     * forwards the {@link ValidationResult} to the supplied {@link OutputFormat}
     * and returns the value produced by that format.
     */
    @Test
    void validate_returnsOutputFromOutputFormat() {
        // GIVEN – a valid JSON input and a mocked OutputFormat that simply returns the
        // ValidationResult it receives.
        String validJson = "{}";
        @SuppressWarnings("unchecked")
        OutputFormat<ValidationResult> outputFormat = mock(OutputFormat.class);
        when(outputFormat.apply(any(ValidationResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN – the schema is validated using the mocked OutputFormat.
        ValidationResult result = schema.validate(validJson, InputFormat.JSON, outputFormat);

        // THEN – the OutputFormat should have been invoked exactly once with a non‑null
        // ValidationResult, and the method should return the same object that the
        // OutputFormat returned.
        verify(outputFormat, times(1)).apply(any(ValidationResult.class));
        assertNotNull(result, "The returned ValidationResult must not be null");
        assertTrue(result.getMessages().isEmpty(), "There should be no validation messages for a valid input");
    }

    /**
     * Verify that validation failures are correctly propagated to the {@link OutputFormat}
     * and that the {@link ValidationResult} contains the expected messages.
     */
    @Test
    void validate_failureIsPropagatedToOutputFormat() {
        // GIVEN – a schema that requires a string property {@code name} and an input that
        // violates this rule (provides a number instead).
        schema = factory.getSchema(REQUIRED_STRING_SCHEMA);
        String invalidJson = "{\"name\": 123}";
        @SuppressWarnings("unchecked")
        OutputFormat<ValidationResult> outputFormat = mock(OutputFormat.class);
        when(outputFormat.apply(any(ValidationResult.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        ValidationResult result = schema.validate(invalidJson, InputFormat.JSON, outputFormat);

        // THEN
        verify(outputFormat, times(1)).apply(any(ValidationResult.class));
        assertNotNull(result, "Result must not be null even when validation fails");
        assertFalse(result.getMessages().isEmpty(), "There should be at least one validation message");
        // The library reports a single message for the type mismatch.
        assertEquals(1, result.getMessages().size(),
                "Exactly one validation message is expected for the type error");
    }

    /**
     * Demonstrates that the method works with a custom {@link OutputFormat}
     * that transforms the {@link ValidationResult} into a simple {@code boolean}
     * indicating success.
     */
    @Test
    void validate_withCustomBooleanOutputFormat() {
        // GIVEN – a custom OutputFormat that returns {@code true} when there are no
        // validation messages and {@code false} otherwise.
        OutputFormat<Boolean> booleanFormat = ValidationResult::isSuccess;
        String json = "{}";

        // WHEN
        Boolean success = schema.validate(json, InputFormat.JSON, booleanFormat);

        // THEN
        assertTrue(success, "The validation should succeed and the custom OutputFormat should return true");
    }
}
```