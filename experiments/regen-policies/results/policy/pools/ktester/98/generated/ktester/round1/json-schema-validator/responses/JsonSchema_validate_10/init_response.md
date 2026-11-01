```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_10_Test {

    private static JsonSchemaFactory factory;
    private JsonSchema schema;

    /**
     * Initialise the {@link JsonSchemaFactory} once for all tests.
     * We use the 2020‑12 draft (the most recent stable draft at the time of writing).
     */
    @BeforeAll
    static void setupBeforeAll() {
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * Build a very small schema before each test.
     * The schema requires the root JSON value to be a string.
     */
    @BeforeEach
    void setupBeforeEach() {
        String schemaData = "{\n" +
                "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
                "  \"type\": \"string\"\n" +
                "}";
        // No special configuration – the default is fine for these unit tests.
        schema = factory.getSchema(schemaData);
    }

    /**
     * Verify that the {@code validate(String, InputFormat, Consumer<ExecutionContext>)}
     * overload correctly forwards the {@link ExecutionContext} to the supplied consumer.
     * The test supplies a valid JSON string, expects an empty validation result set,
     * and checks that the consumer was invoked.
     */
    @Test
    void validate_withExecutionContextConsumer_invokedAndNoErrors() {
        // given
        String validJson = "\"hello world\""; // a JSON string literal – matches the schema
        AtomicBoolean consumerInvoked = new AtomicBoolean(false);
        Consumer<ExecutionContext> executionCustomizer = ctx -> consumerInvoked.set(true);

        // when
        Set<ValidationMessage> result = schema.validate(validJson, InputFormat.JSON, executionCustomizer);

        // then
        assertTrue(result.isEmpty(), "A valid JSON string should produce no validation messages");
        assertTrue(consumerInvoked.get(), "The execution customizer must have been invoked");
    }

    /**
     * Verify that the same overload still works when validation fails.
     * The consumer should still be called even if the payload does not satisfy the schema.
     */
    @Test
    void validate_withExecutionContextConsumer_invalidInput() {
        // given
        String invalidJson = "123"; // a JSON number – does NOT satisfy the \"type\":\"string\" schema
        AtomicBoolean consumerInvoked = new AtomicBoolean(false);
        Consumer<ExecutionContext> executionCustomizer = ctx -> consumerInvoked.set(true);

        // when
        Set<ValidationMessage> result = schema.validate(invalidJson, InputFormat.JSON, executionCustomizer);

        // then
        assertFalse(result.isEmpty(), "Invalid JSON should produce at least one validation message");
        assertTrue(consumerInvoked.get(), "The execution customizer must still be invoked on failure");
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test resources to release.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }
}
```