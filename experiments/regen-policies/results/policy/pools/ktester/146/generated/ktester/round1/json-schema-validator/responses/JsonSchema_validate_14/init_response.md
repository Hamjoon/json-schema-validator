```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat, Consumer)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_14_Test {

    /** Schema used for the tests – a simple object schema that always validates. */
    private JsonSchema schema;

    /** Input JSON that conforms to the above schema. */
    private static final String VALID_INPUT = "{\"name\":\"John\"}";

    /** Simple schema definition – an object with a required string property "name". */
    private static final String SCHEMA_JSON = "{\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"name\": { \"type\": \"string\" }\n"
            + "  },\n"
            + "  \"required\": [\"name\"]\n"
            + "}";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a JsonSchemaFactory for the 2020‑12 draft and obtain a JsonSchema instance.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Use the default configuration (no special preload settings required).
        this.schema = factory.getSchema(SCHEMA_JSON);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global tear‑down needed.
    }

    /**
     * Verifies that the {@code validate(String, InputFormat, OutputFormat, Consumer)} method
     * returns the value produced by the supplied {@link OutputFormat} and that the
     * {@link Consumer} receives the {@link ExecutionContext} created for the validation.
     */
    @Test
    void validateInvokesConsumerAndReturnsOutputFormatResult() {
        // Flag that will be set by the consumer to prove it was invoked.
        AtomicBoolean consumerInvoked = new AtomicBoolean(false);

        // Consumer that records the execution context (simply flips the flag).
        java.util.function.Consumer<ExecutionContext> executionCustomizer = ctx -> {
            assertNotNull(ctx, "ExecutionContext supplied to consumer must not be null");
            consumerInvoked.set(true);
        };

        // OutputFormat that returns a constant string – this is what the method under test should return.
        OutputFormat<String> outputFormat = (executionContext, validationMessages) -> "validation‑ok";

        // Perform the validation.
        String result = schema.validate(
                VALID_INPUT,
                InputFormat.JSON,
                outputFormat,
                executionCustomizer);

        // Verify that the OutputFormat's result is returned.
        assertEquals("validation‑ok", result, "The result from OutputFormat should be returned");

        // Verify that the consumer was indeed called.
        assertTrue(consumerInvoked.get(), "The executionCustomizer consumer should have been invoked");
    }
}
```