```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchema#validate(String, InputFormat, ExecutionContextCustomizer)}.
 *
 * <p>The test suite covers:
 * <ul>
 *   <li>happy‑path validation (no errors)</li>
 *   <li>parsing errors (malformed JSON, empty document, invalid URL‑encoding)</li>
 *   <li>argument validation (null input / null {@link InputFormat})</li>
 *   <li>different {@link InputFormat}s (JSON, YAML, URL‑ENCODED)</li>
 *   <li>behaviour of {@link ExecutionContextCustomizer} – normal execution, modification of
 *       execution config, and exception propagation</li>
 * </ul>
 *
 * <p>All tests reuse a common {@link JsonSchema} instance built from a simple schema that
 * requires the properties {@code name} (string) and {@code age} (integer).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaValidateStringInputFormatCustomizerTest {

    /** Simple schema used for all tests. */
    private static final String SCHEMA_JSON = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              },
              "required": ["name", "age"]
            }
            """;

    private JsonSchema schema;
    private InputFormat jsonFormat;
    private InputFormat yamlFormat;
    private InputFormat urlEncodedFormat;

    @Mock
    private ExecutionContextCustomizer mockCustomizer; // used in a few tests

    @BeforeEach
    void setUp() {
        // Build a schema factory with the default configuration (no external refs)
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(s -> s.schemas(Collections.emptyMap())));

        schema = factory.getSchema(SCHEMA_JSON);
        jsonFormat = InputFormat.JSON;
        yamlFormat = InputFormat.YAML;
        urlEncodedFormat = InputFormat.URL_ENCODED;
    }

    @AfterEach
    void resetMocks() {
        reset(mockCustomizer);
    }

    /* --------------------------------------------------------------
     *  Helper methods
     * -------------------------------------------------------------- */

    /** Executes {@code schema.validate(...)} and either returns the validation messages
     *  or re‑throws the expected exception. */
    private Set<ValidationMessage> invokeValidate(String input,
                                                  InputFormat format,
                                                  ExecutionContextCustomizer customizer) {
        return schema.validate(input, format, customizer);
    }

    /** Creates a customizer that toggles {@code strictMode} on the underlying
     *  {@link ExecutionConfig}. */
    private ExecutionContextCustomizer strictModeCustomizer(boolean enable) {
        return ctx -> {
            try {
                // ExecutionContext -> ExecutionConfig -> setStrictMode(boolean)
                Object execConfig = ctx.getClass()
                        .getMethod("getExecutionConfig")
                        .invoke(ctx);
                execConfig.getClass()
                        .getMethod("setStrictMode", boolean.class)
                        .invoke(execConfig, enable);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /** Creates a customizer that toggles {@code failFast} on the underlying
     *  {@link ExecutionConfig}. */
    private ExecutionContextCustomizer failFastCustomizer(boolean enable) {
        return ctx -> {
            try {
                Object execConfig = ctx.getClass()
                        .getMethod("getExecutionConfig")
                        .invoke(ctx);
                execConfig.getClass()
                        .getMethod("setFailFast", boolean.class)
                        .invoke(execConfig, enable);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /* --------------------------------------------------------------
     *  Parameterised tests for the “happy‑path” and parsing‑error cases
     * -------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideBasicValidationScenarios")
    void validate_BasicScenarios(String description,
                                String input,
                                InputFormat format,
                                ExecutionContextCustomizer customizer,
                                Class<? extends Throwable> expectedException,
                                ValidationAssertion assertion) {

        if (expectedException != null) {
            Throwable thrown = assertThrows(expectedException,
                    () -> invokeValidate(input, format, customizer),
                    "Expected exception for scenario: " + description);
            // For IllegalArgumentException we also check the message content
            if (expectedException == IllegalArgumentException.class && input == null) {
                assertTrue(thrown.getMessage().toLowerCase().contains("input must not be null"));
            }
            if (expectedException == IllegalArgumentException.class && format == null) {
                assertTrue(thrown.getMessage().toLowerCase().contains("inputformat must not be null"));
            }
            return;
        }

        Set<ValidationMessage> messages = invokeValidate(input, format, customizer);
        assertion.assertOn(messages);
    }

    /** Functional interface used to assert on the returned {@link ValidationMessage}s. */
    @FunctionalInterface
    private interface ValidationAssertion {
        void assertOn(Set<ValidationMessage> messages);
    }

    /** Supplies the data for {@link #validate_BasicScenarios(String, String, InputFormat, ExecutionContextCustomizer, Class, ValidationAssertion)}. */
    private static Stream<Arguments> provideBasicValidationScenarios() {
        return Stream.of(
                // 1. Happy path – valid JSON, no customizer
                Arguments.of(
                        "Valid JSON, no customizer",
                        """{"name":"John","age":30}""",
                        InputFormat.JSON,
                        null,
                        null,
                        (ValidationAssertion) msgs -> {
                            assertNotNull(msgs);
                            assertTrue(msgs.isEmpty(), "No validation errors expected");
                        }),

                // 2. Malformed JSON – parsing error
                Arguments.of(
                        "Malformed JSON (syntax error)",
                        """{"name":"John","age":}""",
                        InputFormat.JSON,
                        null,
                        null,
                        (ValidationAssertion) msgs -> {
                            assertFalse(msgs.isEmpty(), "Parsing error messages expected");
                            String text = msgs.iterator().next().getMessage().toLowerCase();
                            assertTrue(text.contains("invalid") || text.contains("unexpected"),
                                    "Message should indicate a JSON syntax problem");
                        }),

                // 3. Null input – IllegalArgumentException
                Arguments.of(
                        "Null input string",
                        null,
                        InputFormat.JSON,
                        null,
                        IllegalArgumentException.class,
                        null),

                // 4. Empty string – parsing error
                Arguments.of(
                        "Empty input string",
                        "",
                        InputFormat.JSON,
                        null,
                        null,
                        (ValidationAssertion) msgs -> {
                            assertFalse(msgs.isEmpty(), "Empty document should yield a parsing error");
                            String text = msgs.iterator().next().getMessage().toLowerCase();
                            assertTrue(text.contains("invalid") || text.contains("empty"),
                                    "Message should mention empty/invalid JSON");
                        }),

                // 5. Null InputFormat – IllegalArgumentException
                Arguments.of(
                        "Null InputFormat",
                        """{"name":"John","age":30}""",
                        null,
                        null,
                        IllegalArgumentException.class,
                        null),

                // 6. URL‑encoded valid JSON – no errors
                Arguments.of(
                        "Valid URL‑encoded JSON",
                        "%7B%22name%22%3A%22John%22%2C%22age%22%3A30%7D",
                        InputFormat.URL_ENCODED,
                        null,
                        null,
                        (ValidationAssertion) msgs -> {
                            assertTrue(msgs.isEmpty(), "Decoded JSON should be valid");
                        }),

                // 7. URL‑encoded malformed percent‑encoding – UnsupportedEncodingException
                Arguments.of(
                        "Invalid URL‑encoded percent sequence",
                        "%E0%A4%A",
                        InputFormat.URL_ENCODED,
                        null,
                        UnsupportedEncodingException.class,
                        null),

                // 8. YAML input – valid
                Arguments.of(
                        "Valid YAML input",
                        """
                        name: John
                        age: 30
                        """,
                        InputFormat.YAML,
                        null,
                        null,
                        (ValidationAssertion) msgs -> {
                            assertTrue(msgs.isEmpty(), "Valid YAML should produce no errors");
                        }),

                // 9. URL‑encoded malformed JSON after decoding – parsing error
                Arguments.of(
                        "URL‑encoded malformed JSON",
                        "%7B%22name%22%3A%22John%22%2C%22age%22%3A%7D",
                        InputFormat.URL_ENCODED,
                        null,
                        null,
                        (ValidationAssertion) msgs -> {
                            assertFalse(msgs.isEmpty(), "Decoded malformed JSON should yield a parsing error");
                        }),

                // 10. JSON array instead of object – type error
                Arguments.of(
                        "JSON array where object expected",
                        """[{"name":"John","age":30}]""",
                        InputFormat.JSON,
                        null,
                        null,
                        (ValidationAssertion) msgs -> {
                            assertFalse(msgs.isEmpty(), "Array input should cause a type‑mismatch error");
                            String text = msgs.iterator().next().getMessage().toLowerCase();
                            assertTrue(text.contains("array") && text.contains("object"),
                                    "Message should indicate expected object but got array");
                        })
        );
    }

    /* --------------------------------------------------------------
     *  Tests that involve ExecutionContextCustomizer behaviour
     * -------------------------------------------------------------- */

    @Test
    void validate_WithStrictModeCustomizer_RequiredPropertyError() {
        // Input missing the required "name" property
        String input = """{"age":30}""";

        ExecutionContextCustomizer strictCustomizer = strictModeCustomizer(true);

        Set<ValidationMessage> messages = invokeValidate(input, jsonFormat, strictCustomizer);
        assertFalse(messages.isEmpty(), "Strict mode should enforce required properties");
        boolean hasRequired = messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("required"));
        assertTrue(hasRequired, "Expected a required‑property validation message");
    }

    @Test
    void validate_WithDisableStrictModeCustomizer_NoRequiredError() {
        // Input missing the required "name" property
        String input = """{"age":30}""";

        ExecutionContextCustomizer disableStrict = strictModeCustomizer(false);

        Set<ValidationMessage> messages = invokeValidate(input, jsonFormat, disableStrict);
        // When strict mode is disabled the required‑property validator should be turned off
        // (the library treats this as “no required‑property errors”)
        boolean hasRequired = messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("required"));
        assertFalse(hasRequired, "Required‑property error should be suppressed in non‑strict mode");
    }

    @Test
    void validate_WithFailFastCustomizer_StopsAfterFirstError() {
        // Input has two problems: missing required "name" and wrong type for "age"
        String input = """{"age":"thirty"}""";

        ExecutionContextCustomizer failFast = failFastCustomizer(true);

        Set<ValidationMessage> messages = invokeValidate(input, jsonFormat, failFast);
        assertEquals(1, messages.size(),
                "Fail‑fast mode should stop after the first validation error");
        ValidationMessage msg = messages.iterator().next();
        String lower = msg.getMessage().toLowerCase();
        // The first error reported by the library is usually the required‑property one
        assertTrue(lower.contains("required") || lower.contains("type"),
                "The single message should be either required‑property or type error");
    }

    @Test
    void validate_WithCustomizerThatThrowsRuntimeException_Propagates() {
        ExecutionContextCustomizer throwing = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> invokeValidate("""{"name":"John","age":30}""", jsonFormat, throwing));
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void validate_WithCustomizerThatThrowsAssertionError_Propagates() {
        ExecutionContextCustomizer throwing = ctx -> {
            throw new AssertionError("assertion from customizer");
        };
        AssertionError err = assertThrows(AssertionError.class,
                () -> invokeValidate("""{"name":"John","age":30}""", jsonFormat, throwing));
        assertEquals("assertion from customizer", err.getMessage());
    }

    @Test
    void validate_WithCustomizerThatThrowsIllegalStateException_Propagates() {
        ExecutionContextCustomizer throwing = ctx -> {
            throw new IllegalStateException("illegal state in customizer");
        };
        IllegalStateException ise = assertThrows(IllegalStateException.class,
                () -> invokeValidate("""{"name":"John","age":30}""", jsonFormat, throwing));
        assertEquals("illegal state in customizer", ise.getMessage());
    }

    @Test
    void validate_NullInput_ArgumentCheckPrecedesCustomizerInvocation() {
        ExecutionContextCustomizer throwing = ctx -> {
            throw new RuntimeException("should never be called");
        };
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> invokeValidate(null, jsonFormat, throwing));
        assertTrue(iae.getMessage().toLowerCase().contains("input must not be null"));
        // Verify that the customizer was never invoked
        verifyNoInteractions(throwing);
    }

    @Test
    void validate_NullInputFormat_ArgumentCheckPrecedesCustomizerInvocation() {
        ExecutionContextCustomizer throwing = ctx -> {
            throw new RuntimeException("should never be called");
        };
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> invokeValidate("""{"name":"John","age":30}""", null, throwing));
        assertTrue(iae.getMessage().toLowerCase().contains("inputformat must not be null"));
        verifyNoInteractions(throwing);
    }

    /* --------------------------------------------------------------
     *  Existing test from the original scaffold (kept unchanged)
     * -------------------------------------------------------------- */

    @Test
    void validate_WithExecutionContextCustomizer_ShouldInvokeCustomizerAndReturnMessages() {
        // Arrange – a customizer that simply records its invocation
        ExecutionContextCustomizer recording = mock(ExecutionContextCustomizer.class);

        // Act
        Set<ValidationMessage> messages = schema.validate(
                """
                {
                  "age": "not-an-integer"
                }
                """,
                jsonFormat,
                recording);

        // Assert – type‑mismatch error for the "age" property
        assertNotNull(messages);
        assertEquals(1, messages.size());
        ValidationMessage msg = messages.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("integer"),
                "Message should indicate an integer type problem");

        // Verify that the customizer was called exactly once
        verify(recording, times(1)).customize(any(ExecutionContext.class));
    }
}
```