```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat, Consumer)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_14_Test {

    /** Instance of {@link JsonSchema} used in the tests. */
    private JsonSchema schema;

    /** Simple JSON schema that accepts any object. */
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    /** Simple JSON document that conforms to {@link #SIMPLE_SCHEMA}. */
    private static final String VALID_INPUT = "{}";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a JsonSchema instance using the public factory API.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(SIMPLE_SCHEMA);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for now.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Verifies that the {@code Consumer<ExecutionContext>} passed to the
     * {@code validate(String, InputFormat, OutputFormat, Consumer)} method
     * is invoked exactly once with the execution context used for validation.
     */
    @Test
    void validate_shouldInvokeConsumerWithExecutionContext() {
        // Arrange
        @SuppressWarnings("unchecked")
        Consumer<ExecutionContext> consumer = mock(Consumer.class);

        // Act
        JsonNode result = schema.validate(
                VALID_INPUT,
                InputFormat.JSON,
                OutputFormat.JSON,
                consumer);

        // Assert
        assertNotNull(result, "The validation should return a non‑null JsonNode result.");
        // The consumer must have been called once with any ExecutionContext instance.
        verify(consumer, times(1)).accept(any(ExecutionContext.class));
    }
}
```