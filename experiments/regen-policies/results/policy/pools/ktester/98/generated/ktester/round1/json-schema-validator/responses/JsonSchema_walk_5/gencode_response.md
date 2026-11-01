```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#walk(String, InputFormat, boolean)}.
 *
 * <p>
 * The test suite covers a wide range of input scenarios described in the
 * supplied JSON test matrix: valid/invalid JSON, YAML, empty or whitespace
 * inputs, null values, unsupported {@link InputFormat}s and malformed payloads.
 * Each case asserts that a {@link ValidationResult} is returned and that its
 * success/failure state matches the expectation.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_5_Test {

    /** Shared factory used to create the schema under test. */
    private static JsonSchemaFactory factory;

    /** The schema instance used by all test methods. */
    private static JsonSchema schema;

    /** Simple JSON schema used for the tests. */
    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              },
              "required": [ "name" ]
            }
            """;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the factory once for the whole test class.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Build a schema without any special configuration.
        schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema, "Schema should be created successfully");
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references – helps the GC in long test suites.
        factory = null;
        schema = null;
    }

    /* --------------------------------------------------------------------- */
    /* Existing concrete tests (kept unchanged)                           */
    /* --------------------------------------------------------------------- */

    @Test
    void walk_ValidateTrue_ReturnsMessages() {
        // Input that violates the schema (name must be a string).
        String invalidJson = """{ "name": 123 }""";

        ValidationResult result = schema.walk(invalidJson, InputFormat.JSON, true);
        assertNotNull(result, "Result must not be null");

        // The result should contain at least one validation message.
        List<ValidationMessage> messages = result.getValidationMessages()
                .stream()
                .collect(Collectors.toList());

        assertFalse(messages.isEmpty(), "Validation messages should be present for invalid input");
        // Optionally assert that the message mentions the property 'name'.
        boolean mentionsName = messages.stream()
                .anyMatch(m -> m.getPath().contains("name"));
        assertTrue(mentionsName, "At least one message should refer to the 'name' property");
    }

    @Test
    void walk_ValidateFalse_NoMessages() {
        // Same invalid JSON, but validation is disabled.
        String invalidJson = """{ "name": 123 }""";

        ValidationResult result = schema.walk(invalidJson, InputFormat.JSON, false);
        assertNotNull(result, "Result must not be null");

        // When validation is disabled the result should be empty.
        List<ValidationMessage> messages = result.getValidationMessages()
                .stream()
                .collect(Collectors.toList());

        assertTrue(messages.isEmpty(), "No validation messages should be returned when validation is disabled");
    }

    @Test
    void walk_concurrentExecution() throws Exception {
        String validJson = """{ "name": "John" }""";

        int threadCount = 30;
        var latch = new java.util.concurrent.CountDownLatch(1);
        var threads = new java.util.ArrayList<Thread>();
        var exceptionHolder = new Exception[1];

        for (int i = 0; i < threadCount; ++i) {
            Thread t = new Thread(() -> {
                try {
                    latch.await();
                    ValidationResult r = schema.walk(validJson, InputFormat.JSON, true);
                    assertNotNull(r);
                    assertTrue(r.getValidationMessages().isEmpty(),
                            "Valid JSON should produce no validation messages");
                } catch (Exception e) {
                    synchronized (exceptionHolder) {
                        exceptionHolder[0] = e;
                    }
                }
            });
            t.start();
            threads.add(t);
        }

        latch.countDown(); // release all threads simultaneously
        for (Thread t : threads) {
            t.join();
        }

        if (exceptionHolder[0] != null) {
            throw exceptionHolder[0];
        }
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised tests for the exhaustive matrix described in JSON      */
    /* --------------------------------------------------------------------- */

    /**
     * Data holder for a single test case.
     */
    private static record WalkCase(
            String description,
            String input,
            InputFormat format,
            boolean validate,
            boolean expectSuccess,
            String expectedMessageFragment) {}

    /**
     * Provides all test cases required by the specification.
     */
    private static Stream<Arguments> walkCasesProvider() {
        return Stream.of(
                // 1. Valid JSON input – validation enabled
                Arguments.of(new WalkCase(
                        "Valid JSON input – validation enabled",
                        "{\"name\":\"John\",\"age\":30}",
                        InputFormat.JSON,
                        true,
                        true,
                        null)),

                // 2. Valid JSON input – validation disabled
                Arguments.of(new WalkCase(
                        "Valid JSON input – validation disabled",
                        "{\"name\":\"John\",\"age\":30}",
                        InputFormat.JSON,
                        false,
                        true,
                        null)),

                // 3. Invalid JSON syntax
                Arguments.of(new WalkCase(
                        "Invalid JSON syntax",
                        "{\"name\":\"John\", \"age\":}",
                        InputFormat.JSON,
                        true,
                        false,
                        "Unexpected token"}),

                // 4. Unsupported InputFormat (null)
                Arguments.of(new WalkCase(
                        "Unsupported InputFormat (null)",
                        "{\"name\":\"John\"}",
                        null,
                        true,
                        false,
                        "InputFormat is null")),

                // 5. Empty input string
                Arguments.of(new WalkCase(
                        "Empty input string",
                        "",
                        InputFormat.JSON,
                        true,
                        false,
                        "Empty input")),

                // 6. Null input string
                Arguments.of(new WalkCase(
                        "Null input string",
                        null,
                        InputFormat.JSON,
                        true,
                        false,
                        "Null input")),

                // 7. Whitespace‑only input string
                Arguments.of(new WalkCase(
                        "Whitespace‑only input string",
                        "   \n\t  ",
                        InputFormat.JSON,
                        true,
                        false,
                        "No content to map")),

                // 8. JSON array as root node (schema does not forbid arrays)
                Arguments.of(new WalkCase(
                        "JSON array as root node",
                        "[{\"id\":1},{\"id\":2}]",
                        InputFormat.JSON,
                        true,
                        true,
                        null)),

                // 9. JSON primitive number as root node
                Arguments.of(new WalkCase(
                        "JSON primitive number as root node",
                        "42",
                        InputFormat.JSON,
                        true,
                        true,
                        null)),

                // 10. JSON primitive boolean as root node
                Arguments.of(new WalkCase(
                        "JSON primitive boolean as root node",
                        "true",
                        InputFormat.JSON,
                        true,
                        true,
                        null)),

                // 11. Null InputFormat with validation disabled
                Arguments.of(new WalkCase(
                        "Null InputFormat with validation disabled",
                        "{\"name\":\"John\"}",
                        null,
                        false,
                        false,
                        "InputFormat is null")),

                // 12. Large JSON payload
                Arguments.of(new WalkCase(
                        "Large JSON payload",
                        generateLargeJson(),
                        InputFormat.JSON,
                        true,
                        true,
                        null)),

                // 13. Unicode characters in JSON string
                Arguments.of(new WalkCase(
                        "Unicode characters in JSON string",
                        "{\"message\":\"Hello, 世界 🌍\"}",
                        InputFormat.JSON,
                        true,
                        true,
                        null)),

                // 14. Malformed YAML input – validation enabled
                Arguments.of(new WalkCase(
                        "Malformed YAML input – validation enabled",
                        "name: John\nage: 30:\n",
                        InputFormat.YAML,
                        true,
                        false,
                        "while scanning for the next token")),

                // 15. Invalid percent‑encoding in input string
                Arguments.of(new WalkCase(
                        "Invalid percent‑encoding in input string",
                        "%GG%ZZ",
                        InputFormat.JSON,
                        true,
                        false,
                        "IllegalArgumentException")),

                // 16. Invalid JSON escape sequence
                Arguments.of(new WalkCase(
                        "Invalid JSON escape sequence",
                        "{\"path\":\"C:\\\\temp\\\\file\\x\"}",
                        InputFormat.JSON,
                        true,
                        false,
                        "Invalid escape")),

                // 17. Valid YAML input – validation enabled (if YAML supported)
                Arguments.of(new WalkCase(
                        "Valid YAML input – validation enabled",
                        "name: John\nage: 30",
                        InputFormat.YAML,
                        true,
                        true,
                        null)),

                // 18. Valid YAML input – validation disabled
                Arguments.of(new WalkCase(
                        "Valid YAML input – validation disabled",
                        "name: John\nage: 30",
                        InputFormat.YAML,
                        false,
                        true,
                        null))

                // Additional cases (e.g., unsupported XML) are omitted because the
                // enum constant may not exist in the library version used.
        );
    }

    /**
     * Helper that builds a relatively large JSON payload (≈ 50 items).
     */
    private static String generateLargeJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"items\":[");
        for (int i = 0; i <= 50; i++) {
            sb.append("{\"id\":").append(i).append('}');
            if (i < 50) sb.append(',');
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * Executes the {@code walk} method for each matrix entry and asserts the
     * expected success/failure semantics.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("walkCasesProvider")
    void walk_matrixTests(WalkCase tc) {
        // -----------------------------------------------------------------
        // Arrange – nothing special to set up per case.
        // -----------------------------------------------------------------
        String input = tc.input();
        InputFormat format = tc.format();
        boolean validate = tc.validate();

        // -----------------------------------------------------------------
        // Act – invoke the method under test.
        // -----------------------------------------------------------------
        ValidationResult result = schema.walk(input, format, validate);

        // -----------------------------------------------------------------
        // Assert – verify the result matches the expectation.
        // -----------------------------------------------------------------
        assertNotNull(result, tc.description() + ": result must not be null");

        boolean isSuccess = result.isSuccess()
                || result.getValidationMessages().isEmpty(); // fallback for older versions

        if (tc.expectSuccess()) {
            assertTrue(isSuccess,
                    tc.description() + ": expected a successful ValidationResult but got failures: "
                            + result.getValidationMessages());
        } else {
            assertFalse(isSuccess,
                    tc.description() + ": expected a failure ValidationResult but it succeeded");
            // When a specific fragment of the error message is supplied, ensure it appears.
            if (tc.expectedMessageFragment() != null) {
                String allMessages = result.getValidationMessages()
                        .stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.joining("; "));
                assertTrue(allMessages.toLowerCase().contains(tc.expectedMessageFragment().toLowerCase()),
                        tc.description() + ": expected message fragment not found. Messages: " + allMessages);
            }
        }
    }
}
```