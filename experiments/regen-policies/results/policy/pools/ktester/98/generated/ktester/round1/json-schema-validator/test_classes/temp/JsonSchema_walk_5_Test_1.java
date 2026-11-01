package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_5_Test {

    /**
     * Shared factory used to create the schema under test.
     */
    private static JsonSchemaFactory factory;

    /**
     * The schema instance used by all test methods.
     */
    private static JsonSchema schema;

    /**
     * Simple JSON schema used for the tests.
     */
    private static final String SIMPLE_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "properties": {
            "name": { "type": "string" }
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

    @AfterAll
    static void teardownAfterAll() {
        // Release static references – helps the GC in long test suites.
        factory = null;
        schema = null;
    }

    /**
     * Verify that {@code walk(..., true)} performs validation and returns a
     * {@link ValidationResult} containing the expected validation messages.
     */
    @Test
    void walk_ValidateTrue_ReturnsMessages() {
        // Input that violates the schema (name must be a string).
        String invalidJson = "{ \"name\": 123 }";
        ValidationResult result = schema.walk(invalidJson, InputFormat.JSON, true);
        assertNotNull(result, "Result must not be null");
        // The result should contain at least one validation message.
        List<ValidationMessage> messages = result.getValidationMessages().stream().collect(Collectors.toList());
        assertFalse(messages.isEmpty(), "Validation messages should be present for invalid input");
        // Optionally assert that the message mentions the property 'name'.
        boolean mentionsName = messages.stream().anyMatch(m -> m.getPath().contains("name"));
        assertTrue(mentionsName, "At least one message should refer to the 'name' property");
    }

    /**
     * Verify that {@code walk(..., false)} does **not** perform validation.
     * The returned {@link ValidationResult} should contain no messages.
     */
    @Test
    void walk_ValidateFalse_NoMessages() {
        // Same invalid JSON, but validation is disabled.
        String invalidJson = "{ \"name\": 123 }";
        ValidationResult result = schema.walk(invalidJson, InputFormat.JSON, false);
        assertNotNull(result, "Result must not be null");
        // When validation is disabled the result should be empty.
        List<ValidationMessage> messages = result.getValidationMessages().stream().collect(Collectors.toList());
        assertTrue(messages.isEmpty(), "No validation messages should be returned when validation is disabled");
    }

    /**
     * A basic concurrency test similar to the one in {@link JsonSchemaTest},
     * but exercising the {@code walk} method.
     */
    @Test
    void walk_concurrentExecution() throws Exception {
        String validJson = "{ \"name\": \"John\" }";
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
                    assertTrue(r.getValidationMessages().isEmpty(), "Valid JSON should produce no validation messages");
                } catch (Exception e) {
                    synchronized (exceptionHolder) {
                        exceptionHolder[0] = e;
                    }
                }
            });
            t.start();
            threads.add(t);
        }
        // release all threads simultaneously
        latch.countDown();
        for (Thread t : threads) {
            t.join();
        }
        if (exceptionHolder[0] != null) {
            throw exceptionHolder[0];
        }
    }
}
