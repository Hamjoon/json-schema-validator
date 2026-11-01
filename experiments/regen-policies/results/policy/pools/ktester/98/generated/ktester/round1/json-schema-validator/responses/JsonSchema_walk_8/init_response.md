```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_8_Test {

    private static final String SIMPLE_SCHEMA = "{\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"name\": { \"type\": \"string\" }\n"
            + "  },\n"
            + "  \"required\": [\"name\"]\n"
            + "}";

    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a JsonSchema instance that will be reused by all tests
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder // default configuration – no special loaders required
        );
        // Use the default config (pre‑load is true by default, which is fine for this simple schema)
        schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema, "Schema should have been created");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing specific to clean up for each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    /**
     * Walk the schema with {@code validate = false}. The method should still return a
     * {@link ValidationResult} but without performing any validation – i.e. the
     * result must contain no validation messages.
     */
    @Test
    void walkWithoutValidationReturnsEmptyResult() {
        String validJson = "{\"name\":\"John\"}";
        ValidationResult result = schema.walk(validJson, InputFormat.JSON, false, executionContext -> {
            // The customiser should be invoked even when validation is disabled.
            assertNotNull(executionContext, "ExecutionContext supplied to customiser must not be null");
        });

        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getValidationMessages();
        assertTrue(messages.isEmpty(), "When validation is disabled there should be no messages");
    }

    /**
     * Walk the schema with {@code validate = true} on an invalid instance.
     * The supplied {@link java.util.function.Consumer} must be invoked and the
     * resulting {@link ValidationResult} must contain the expected validation
     * messages.
     */
    @Test
    void walkWithValidationAndCustomizerDetectsErrors() {
        String invalidJson = "{\"name\":1}";
        AtomicBoolean customiserInvoked = new AtomicBoolean(false);

        ValidationResult result = schema.walk(invalidJson, InputFormat.JSON, true, executionContext -> {
            // verify that the consumer is called and that we receive a non‑null context
            assertNotNull(executionContext, "ExecutionContext supplied to customiser must not be null");
            customiserInvoked.set(true);
        });

        assertTrue(customiserInvoked.get(), "The execution customiser should have been invoked");

        assertNotNull(result, "Result must not be null");
        Set<ValidationMessage> messages = result.getValidationMessages();

        // The schema requires \"name\" to be a string, therefore we expect at least one message
        assertFalse(messages.isEmpty(), "Invalid input should produce validation messages");
        // Simple sanity check – the message should mention the property \"name\"
        boolean mentionsName = messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("name"));
        assertTrue(mentionsName, "At least one validation message should refer to the 'name' property");
    }
}
```